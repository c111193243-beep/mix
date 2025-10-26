package com.patrick.core.alert;

import android.content.Context;
import android.util.Log;

import com.patrick.core.FatigueDetectionResult;
import com.patrick.core.FatigueDialogCallback;
import com.patrick.core.FatigueUiCallback;

public class FatigueAlertManager {
    private static final String TAG = "FatigueAlertManager";

    private final Context appContext;
    private FatigueDialogCallback dialogCallback;
    private FatigueUiCallback uiCallback;

    public FatigueAlertManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void setDialogCallback(FatigueDialogCallback callback) {
        this.dialogCallback = callback;
    }

    public void setUiCallback(FatigueUiCallback callback) {
        this.uiCallback = callback;
    }

    /**
     * 接到疲勞事件時的處理（目前只 log；介面與 FatigueDetectionManager 相容）
     */
    public void handleFatigueDetection(FatigueDetectionResult result) {
        String levelStr;
        try {
            levelStr = String.valueOf(result.getFatigueLevel());
        } catch (Throwable t) {
            levelStr = "UNKNOWN";
        }
        int eventCount = 0;
        try {
            eventCount = (result.getEvents() == null) ? 0 : result.getEvents().size();
        } catch (Throwable ignore) {}

        Log.d(TAG, "handleFatigueDetection: level=" + levelStr + ", events=" + eventCount);

        // 需要 UI/聲音時，再在這裡呼叫 callback（依你的介面決定）
        // if (uiCallback != null) uiCallback.setWarningDialogActive(true);
    }

    /**
     * 停止所有告警（聲音/震動/對話框等）；目前只 log。
     */
    public void stopAllAlerts() {
        Log.d(TAG, "stopAllAlerts");
        // if (uiCallback != null) uiCallback.setWarningDialogActive(false);
    }
}
