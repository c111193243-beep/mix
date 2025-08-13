package com.example.drivesafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private PreviewView previewView;
    private TextView tvFatigueScore, tvFatigueLevel, tvBlinks, tvYawns, tvEyesClosed;

    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;

    private int blinkCount = 0;
    private int yawnCount = 0;
    private float eyeClosedDuration = 0f;

    private BottomNavigationView bottomNav; // ↓ 讓 onResume 能重選當前 tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView      = findViewById(R.id.previewView);
        tvFatigueScore   = findViewById(R.id.tvFatigueScore);
        tvFatigueLevel   = findViewById(R.id.tvFatigueLevel);
        tvBlinks         = findViewById(R.id.tvBlinks);
        tvYawns          = findViewById(R.id.tvYawns);
        tvEyesClosed     = findViewById(R.id.tvEyesClosed);

        // BottomNavigation
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_detect); // 預設選偵測畫面
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_detect) {
                // 已在本頁，不跳轉
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_chart) {
                startActivity(new Intent(this, ChartActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
        bottomNav.setOnItemReselectedListener(item -> {
            // 點到同一個 tab 不重複動作，留空即可
        });

        // 權限 → 相機
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        } else {
            startCamera();
        }

        // ML Kit 臉部偵測設定
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 從其他頁回來時，底部保持選中「偵測」
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_detect);
    }

    /* ===== CameraX 維持原本流程 ===== */

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        androidx.camera.core.Preview preview =
                new androidx.camera.core.Preview.Builder().build();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        updateUIWithFace(face);
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e("MLKit", "Face detection failed", e);
                    imageProxy.close();
                });
    }

    private void updateUIWithFace(Face face) {
        float leftEyeOpen = face.getLeftEyeOpenProbability() != null ?
                face.getLeftEyeOpenProbability() : -1;
        float rightEyeOpen = face.getRightEyeOpenProbability() != null ?
                face.getRightEyeOpenProbability() : -1;
        float smileProb = face.getSmilingProbability() != null ?
                face.getSmilingProbability() : -1;

        float eyeOpenAvg = (leftEyeOpen + rightEyeOpen) / 2;

        runOnUiThread(() -> {
            if (eyeOpenAvg < 0.4) blinkCount++;
            if (smileProb > 0.7) yawnCount++;
            if (eyeOpenAvg < 0.3) eyeClosedDuration += 0.1f;

            int fatigueScore = 1 + blinkCount / 60 + yawnCount + (eyeClosedDuration > 1.5 ? 2 : 0);
            fatigueScore = Math.min(fatigueScore, 10);

            String fatigueLevel = "正常";
            if (fatigueScore >= 8) fatigueLevel = "高度疲勞";
            else if (fatigueScore >= 5) fatigueLevel = "中度疲勞";
            else if (fatigueScore >= 3) fatigueLevel = "輕度疲勞";

            tvFatigueScore.setText(String.valueOf(fatigueScore));
            tvFatigueLevel.setText(fatigueLevel);
            tvBlinks.setText(blinkCount + "次");
            tvYawns.setText(yawnCount + "次");
            tvEyesClosed.setText(String.format("%.1f秒", eyeClosedDuration));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "相機權限被拒絕", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
