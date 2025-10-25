package com.patrick.core.alert

interface AlertChannel {
    fun showFatigueWarning(probability: Float)
    fun playBeep()
    fun vibrate(durationMs: Long = 500)
}
