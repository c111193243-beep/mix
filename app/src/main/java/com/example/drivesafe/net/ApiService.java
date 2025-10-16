package com.example.drivesafe.net;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    /* ========== 登入（視你的後端哪個有實作，就用哪個） ========== */

    // A. /members/login（JSON）
    @POST("members/login")
    Call<LoginResp> loginMembers(@Body LoginReq req);

    // B. /login（JSON）
    @POST("login")
    Call<LoginResp> loginJson(@Body LoginReq req);

    // C. /token（form，常見於 OAuth2PasswordRequestForm）
    @FormUrlEncoded
    @POST("token")
    Call<LoginResp> loginForm(
            @Field("username") String username,
            @Field("password") String password
    );

    /* ========== 舊 schema：FatigueDto（HistoryActivity.fetchFromServerAndCache / UploadWorker 會用到） ========== */

    // 查詢一段時間內的舊版疲勞紀錄
    @GET("driving_records")
    Call<List<FatigueDto>> getRecords(
            @Header("Authorization") String bearerToken,   // e.g. "Bearer xxx"；若無就傳 null
            @Query("user_id") String userId,
            @Query("start_ms") Long startMs,
            @Query("end_ms") Long endMs
    );

    // 單筆上傳（舊 schema）
    @POST("driving_records")
    Call<FatigueDto> postRecord(
            @Header("Authorization") String bearerToken,
            @Body FatigueDto body
    );

    // ✅ 批次上傳（舊 schema）— 給 UploadWorker 用
    @POST("driving_records")
    Call<List<FatigueDto>> postRecords(
            @Header("Authorization") String bearerToken,
            @Body List<FatigueDto> payload
    );

    /* ========== 新 schema：DrivingRecordDto（HistoryActivity.cloudFetchAllThenShow / testUploadOneRecord 會用到） ========== */

    // 依 member_id 取得雲端駕駛紀錄（清單）
    @GET("driving_records")
    Call<List<DrivingRecordDto>> getDrivingRecords(@Query("member_id") int memberId);

    // 新 schema：單筆上傳
    @POST("driving_records")
    Call<DrivingRecordDto> postDrivingRecord(@Body DrivingRecordDto dto);

    /* ========== 成員查詢（可用來把 email 對應成整數 member_id） ========== */

    @GET("members")
    Call<List<MemberDto>> getMembers();

    /* ========== 健康檢查 / 根目錄回應（可選） ========== */

    @GET("/")
    Call<RootResp> root();
}
