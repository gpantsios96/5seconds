package app.techdox.pause

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** A gentle tick when the pause ends. No-ops quietly on devices without a vibrator. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private val usable get() = vibrator?.hasVibrator() == true

    /** A single soft tick. */
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
