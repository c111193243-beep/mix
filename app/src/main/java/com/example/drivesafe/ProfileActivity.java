package com.example.drivesafe;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvAge, tvPhone;
    private MaterialButton btnEditProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 綁定元件
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvAge = findViewById(R.id.tvAge);
        tvPhone = findViewById(R.id.tvPhone);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        // 假資料
        String name = "王小明";
        String email = "xiaoming@example.com";
        int age = 30;
        String phone = "0912-345678";

        // 顯示資料
        tvName.setText("姓名：" + name);
        tvEmail.setText("Email：" + email);
        tvAge.setText("年齡：" + age);
        tvPhone.setText("電話：" + phone);

        // 編輯按鈕
        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(ProfileActivity.this, "未來可編輯資料", Toast.LENGTH_SHORT).show()
        );
    }
}
