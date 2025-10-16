package com.example.drivesafe.net;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // 模擬器連你電腦 → 10.0.2.2；埠號請對齊你後端（例 8005）
    private static final String BASE_URL = "http://10.0.2.2:8005/";
    private static ApiService API;

    public static ApiService api() {
        if (API == null) {
            OkHttpClient ok = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            Retrofit r = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(ok)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            API = r.create(ApiService.class);
        }
        return API;
    }
}
