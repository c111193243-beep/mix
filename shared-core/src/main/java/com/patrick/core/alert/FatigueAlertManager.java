package com.patrick.core.alert

import android.content.Context
import android.util.Log
import com.patrick.core.FatigueDetectionResult
import com.patrick.core.FatigueDialogCallback
import com.patrick.core.FatigueUiCallback

/**
 * 最小可用版的告警管理器：目前只做 logging，不顯示任何 UI 或音效。
 * 之後你要加對話框/聲音，只要在對應方法裡補上即可。
 */
class FatigueAlertManager(private val context: Context) {

    private var dialogCallback: FatigueDialogCallback? = null
    private var uiCallback: FatigueUiCallback? = null

    fun setDialogCallback(callback: FatigueDialogCallback) {
        this.dialogCallback = callback
    }

    fun setUiCallback(callback: FatigueUiCallback) {
        this.uiCallback = callback
    }

    /**
     * 接到疲勞事件時的處理（目前只 log；保留與 FatigueDetectionManager 相容的介面）
     */
    fun handleFatigueDetection(result: FatigueDetectionResult) {
        Log.d("FatigueAlertManager", "handleFatigueDetection: level=${result.fatigueLevel}, events=${result.events.size}")
        // 這裡未彈窗；若要顯示對話框或播聲音，可在這裡呼叫 uiCallback.* 或 dialogCallback.*
        // 例如：uiCallback?.setWarningDialogActive(true)
    }

    /**
     * 停止所有告警（聲音/震動/對話框等）；目前只 log。
     */
    fun stopAllAlerts() {
        Log.d("FatigueAlertManager", "stopAllAlerts")
        // 若未來有聲音或對話框，在這裡關閉它們
        // uiCallback?.setWarningDialogActive(false)
    }
}
