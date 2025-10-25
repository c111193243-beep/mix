package com.patrick.ui.alert

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import com.patrick.core.alert.AlertChannel   // 介面在 shared-core

class SystemAlertChannel(private val context: Context) : AlertChannel {

    override fun showFatigueWarning(probability: Float) {
        Toast.makeText(context, "偵測到疲勞 (p=$probability)", Toast.LENGTH_SHORT).show()
    }

    override fun playBeep() {
        // TODO: 需要聲音再補 MediaPlayer/SoundPool
    }

    override fun vibrate(durationMs: Long) {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
