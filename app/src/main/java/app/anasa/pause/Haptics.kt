package app.anasa.pause

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Gentle haptics synced with the breathing animation. No-ops quietly on devices without a vibrator. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private val usable get() = vibrator?.hasVibrator() == true

    /** A slowly rising buzz across the inhale, or a single tick if amplitude control is missing. */
    fun inhale(durationMillis: Long) {
        val v = vibrator ?: return
        if (!usable) return
        if (v.hasAmplitudeControl()) {
            val steps = 10
            val stepMs = durationMillis / steps
            val timings = LongArray(steps) { stepMs }
            val amplitudes = IntArray(steps) { i -> 10 + (i * 60 / steps) } // 10..~64 of 255: soft
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            tick()
        }
    }

    /** A single soft tick, used at the turn of each breath. */
    fun tick() {
        val v = vibrator ?: return
        if (!usable) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            v.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }
}
