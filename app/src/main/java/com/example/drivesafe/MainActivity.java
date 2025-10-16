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
import com.example.drivesafe.db.FatigueRecord;


import com.example.drivesafe.db.AppDatabase;
import com.example.drivesafe.sync.UploadWorker;
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

    private static final String TAG = "DriveSafe";

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    // 後端（模擬器連本機）
    private static final String BASE_URL = "http://10.0.2.2:8005/";
    private static final String USER_ID  = "00000000-0000-0000-0000-000000000001";

    // —— 疲勞紀錄策略（可依需求調整）——
    private static final float FATIGUE_THRESHOLD = 5f;      // 幾分以上算「要記錄」
    private static final long  SAVE_MIN_INTERVAL = 30_000L; // 每 30 秒至少存一筆
    private static final long  TRIGGER_COOLDOWN  = 10_000L; // 達門檻後 10 秒內不再觸發

    private long lastSavedMs = 0L;
    private long lastTriggeredMs = 0L;

    private PreviewView previewView;
    private TextView tvFatigueScore, tvFatigueLevel, tvBlinks, tvYawns, tvEyesClosed;
    private BottomNavigationView bottomNav;

    private ExecutorService cameraExecutor;
    private ExecutorService dbExecutor;
    private FaceDetector faceDetector;
    private AppDatabase db;

    // 這三個只是示意（你之後可以換成真正的特徵/模型）
    private int   blinkCount = 0;
    private int   yawnCount = 0;
    private float eyesClosedSec = 0f;

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
        bottomNav        = findViewById(R.id.bottomNavigation);

        // BottomNav
        bottomNav.setSelectedItemId(R.id.nav_detect);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_detect) return true;
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }
            if (id == R.id.nav_chart) {
                startActivity(new Intent(this, ChartActivity.class));
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
        bottomNav.setOnItemReselectedListener(item -> {});

        // 權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        // ML Kit 臉部偵測（先用最簡配置）
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector   = FaceDetection.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Room
        db          = AppDatabase.getInstance(getApplicationContext());
        dbExecutor  = Executors.newSingleThreadExecutor();

        Log.i(TAG, "Main ready. BASE_URL=" + BASE_URL + " USER_ID=" + USER_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_detect);
    }

    /* ================= CameraX ================= */

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);

        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = providerFuture.get();
                bindCameraUseCases(provider);
            } catch (Exception e) {
                Log.e(TAG, "startCamera() error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider provider) {
        androidx.camera.core.Preview preview =
                new androidx.camera.core.Preview.Builder().build();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        provider.unbindAll();
        provider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
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
                        updateUIWithFace(faces.get(0));
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed", e);
                    imageProxy.close();
                });
    }

    /* ================ 分數/畫面/寫檔 ================ */

    private void updateUIWithFace(Face face) {
        Float l = face.getLeftEyeOpenProbability();
        Float r = face.getRightEyeOpenProbability();
        Float s = face.getSmilingProbability();

        // MLKit 有時會回 null，避免拿 null 去算造成誤判
        boolean leftOk  = (l != null && l >= 0f);
        boolean rightOk = (r != null && r >= 0f);
        boolean smileOk = (s != null && s >= 0f);

        float eyeOpenAvg = 1f; // 預設當作睜眼，避免 null 被當閉眼
        if (leftOk && rightOk) {
            eyeOpenAvg = (l + r) / 2f;
        }

        // —— 超簡化版規則（之後可換你的真正特徵/模型）——
        if (leftOk && rightOk && eyeOpenAvg < 0.4f) blinkCount++;
        if (smileOk && s > 0.7f) yawnCount++;      // 先用微笑當「張口」替代
        if (leftOk && rightOk && eyeOpenAvg < 0.3f) eyesClosedSec += 0.1f; // 估 0.1 秒

        // 疲勞分數（0~10），先用暫存變數，最後轉成 final 給 lambda 用
        int tmpScore = 1 + blinkCount / 60 + yawnCount + (eyesClosedSec > 1.5f ? 2 : 0);
        if (tmpScore > 10) tmpScore = 10;
        final int   fatigueScoreInt = tmpScore;
        final float fatigueScore    = (float) fatigueScoreInt;

        final String fatigueLevel;
        if (fatigueScore >= 8f)      fatigueLevel = "高度疲勞";
        else if (fatigueScore >= 5f) fatigueLevel = "中度疲勞";
        else if (fatigueScore >= 3f) fatigueLevel = "輕度疲勞";
        else                         fatigueLevel = "正常";

        // 更新 UI（使用 final 變數，避免 lambda 編譯錯）
        runOnUiThread(() -> {
            tvFatigueScore.setText(String.valueOf(fatigueScoreInt));
            tvFatigueLevel.setText(fatigueLevel);
            tvBlinks.setText(blinkCount + "次");
            tvYawns.setText(yawnCount + "次");
            tvEyesClosed.setText(String.format("%.1f秒", eyesClosedSec));
        });

        // 達門檻 → 觸發一次（有冷卻）
        if (fatigueScore >= FATIGUE_THRESHOLD) {
            maybeTriggerAndSave(fatigueScore);
        }

        // 保險：每隔一段時間至少存一筆
        maybeSavePeriodic(fatigueScore);
    }

    /** 達門檻的單次觸發（冷卻內不重複）+ 立即嘗試存檔與排上傳 */
    private void maybeTriggerAndSave(float score) {
        long now = System.currentTimeMillis();
        if (now - lastTriggeredMs < TRIGGER_COOLDOWN) return;
        lastTriggeredMs = now;

        Log.i(TAG, "FATIGUE TRIGGERED: score=" + score);
        Toast.makeText(this, "偵測到疲勞，已嘗試保存並上傳", Toast.LENGTH_SHORT).show();

        saveToDbAndEnqueueUpload(score, now);
    }

    /** 週期性保險存檔（避免整天 0 筆） */
    private void maybeSavePeriodic(float score) {
        long now = System.currentTimeMillis();
        if (now - lastSavedMs >= SAVE_MIN_INTERVAL) {
            saveToDbAndEnqueueUpload(score, now);
        }
    }

    /** 真正寫入 Room + 呼叫 WorkManager 上傳（背景） */
    private void saveToDbAndEnqueueUpload(float score, long ts) {
        dbExecutor.execute(() -> {
            try {
                long rowId = db.fatigueDao().insert(new FatigueRecord(ts, score));
                lastSavedMs = ts;
                Log.i(TAG, "[SAVE] local insert ok. rowId=" + rowId);

                UploadWorker.enqueue(getApplicationContext(), BASE_URL, USER_ID);
                Log.i(TAG, "[UPLOAD] enqueued");
            } catch (Exception e) {
                Log.e(TAG, "[SAVE] failed: " + e.getMessage(), e);
            }
        });
    }

    /* ================= 收尾/權限 ================= */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (dbExecutor != null) dbExecutor.shutdown();
        if (faceDetector != null) faceDetector.close();
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
