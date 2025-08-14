package com.example.drivesafe;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthStore {
    private static final String PREFS = "auth_prefs";
    private static final String K_USER = "user";
    private static final String K_PASS = "pass";

    // 儲存帳密
    public static void save(Context ctx, String user, String pass) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putString(K_USER, user)
                .putString(K_PASS, pass)
                .apply();
    }

    // 取得帳號（預設 test@example.com）
    public static String getUser(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(K_USER, "test@example.com");
    }

    // 取得密碼（預設 1234）
    public static String getPass(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(K_PASS, "1234");
    }

    // 檢查帳密是否正確
    public static boolean check(Context ctx, String user, String pass) {
        return getUser(ctx).equals(user) && getPass(ctx).equals(pass);
    }
}
