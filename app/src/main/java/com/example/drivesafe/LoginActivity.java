package com.example.drivesafe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private CheckBox checkboxRemember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        checkboxRemember = findViewById(R.id.checkboxRemember);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);

        // 如果記住帳號，就顯示上次登入帳號
        if (checkboxRemember.isChecked()) {
            etEmail.setText(AuthStore.getUser(this));
        }

        btnLogin.setOnClickListener(v -> {
            String user = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty()) {
                Toast.makeText(this, "請輸入帳號", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pass.isEmpty()) {
                Toast.makeText(this, "請輸入密碼", Toast.LENGTH_SHORT).show();
                return;
            }

            if (AuthStore.check(this, user, pass)) {
                Toast.makeText(this, "登入成功", Toast.LENGTH_SHORT).show();
                // 記住帳號
                if (checkboxRemember.isChecked()) {
                    AuthStore.save(this, user, pass);
                }
                // 跳轉主畫面
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "帳號或密碼錯誤", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
