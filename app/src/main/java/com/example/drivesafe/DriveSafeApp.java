package com.example.drivesafe;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class DriveSafeApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 🔒 強制淺色模式
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
