package com.example.drivesafe.net;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenStore {
    private final SharedPreferences sp;
    public TokenStore(Context ctx) {
        sp = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }
    public void save(String token) { sp.edit().putString("access_token", token).apply(); }
    public String get() { return sp.getString("access_token", null); }
    public void clear() { sp.edit().remove("access_token").apply(); }
}
