package com.example.drivesafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Toolbar 返回鍵
        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 背景音效開關
        switchSound = findViewById(R.id.switchSound);
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(
                    this,
                    isChecked ? "背景音效開啟" : "背景音效關閉",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // 個人資料
        MaterialCardView cardProfile = findViewById(R.id.cardProfile);
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class))
            );
        }

        // 帳號管理
        MaterialCardView cardAccountManage = findViewById(R.id.cardAccountManage);
        if (cardAccountManage != null) {
            cardAccountManage.setOnClickListener(v ->
                    startActivity(new Intent(this, AccountManageActivity.class))
            );
        }

        // 登出
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
