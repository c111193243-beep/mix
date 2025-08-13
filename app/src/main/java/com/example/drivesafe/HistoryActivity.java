package com.example.drivesafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmptyHint;
    private HistoryAdapter adapter;
    private ArrayList<HistoryItem> fakeList;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Toolbar：設為 ActionBar + 返回
        MaterialToolbar toolbar = findViewById(R.id.historyToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // BottomNavigation：此頁預設選中「紀錄」
        bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_detect) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                } else if (id == R.id.nav_history) {
                    // 已在本頁
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
            bottomNav.setOnItemReselectedListener(item -> { /* 同頁重選不動作 */ });
        }

        rvHistory = findViewById(R.id.rvHistory);
        tvEmptyHint = findViewById(R.id.tvEmptyHint);

        // TODO: 改成你的資料來源（這裡先用假資料）
        fakeList = new ArrayList<>();
        fakeList.add(new HistoryItem("2025-07-15 08:32", 7, "中度疲勞"));
        fakeList.add(new HistoryItem("2025-07-14 21:45", 3, "輕度疲勞"));
        fakeList.add(new HistoryItem("2025-07-13 19:10", 9, "高度疲勞"));

        adapter = new HistoryAdapter(fakeList);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        tvEmptyHint.setVisibility(fakeList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 從其他頁回來時，保持底部選中「紀錄」
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_history);
    }
}
