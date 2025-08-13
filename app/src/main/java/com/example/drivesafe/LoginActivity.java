package com.example.drivesafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister, tvForgot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG", "LoginActivity onCreate() 開始");
        setContentView(R.layout.activity_login);

        Log.d("DEBUG", "LoginActivity 畫面已設定成功");

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgot = findViewById(R.id.tvForgot);

        Log.d("DEBUG", "LoginActivity UI 元件綁定完成");

        btnLogin.setOnClickListener(v -> {
            Log.d("DEBUG", "點擊登入按鈕");
            handleLogin();
        });

        tvRegister.setOnClickListener(v -> {
            Log.d("DEBUG", "點擊註冊連結");
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgot.setOnClickListener(v -> {
            Log.d("DEBUG", "點擊忘記密碼連結");
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        Log.d("DEBUG", "LoginActivity onCreate() 結束");
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        Log.d("DEBUG", "輸入 Email: " + email);
        Log.d("DEBUG", "輸入 Password: " + password);

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("請輸入 Email");
            Log.w("DEBUG", "Email 為空");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email 格式錯誤");
            Log.w("DEBUG", "Email 格式錯誤");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("請輸入密碼");
            Log.w("DEBUG", "密碼為空");
            return;
        }

        // ✅ 測試帳號（可換成 Firebase 或 Room）
        if (email.equals("test@example.com") && password.equals("123456")) {
            Log.i("DEBUG", "登入成功，跳轉至 MainActivity");
            Toast.makeText(this, "登入成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Log.w("DEBUG", "登入失敗：帳號或密碼錯誤");
            Toast.makeText(this, "帳號或密碼錯誤", Toast.LENGTH_SHORT).show();
        }
    }
}
