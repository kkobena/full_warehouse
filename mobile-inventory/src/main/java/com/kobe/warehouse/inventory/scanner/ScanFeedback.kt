package com.kobe.warehouse.inventory.scanner

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Bip + vibration à chaque code reconnu. Remplace le `BeepManager` de ZXing, retiré
 * avec la bibliothèque : en entrepôt le retour sonore est le seul signal fiable,
 * l'opérateur ne regarde pas l'écran entre deux scans.
 */
class ScanFeedback(context: Context) {

    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME)

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun success() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS)
        vibrate(VIBRATE_MS)
    }

    /**
     * Signal d'échec nettement distinct du succès — double bip grave et vibration
     * longue. En scan continu l'opérateur a les yeux sur les boîtes, pas sur
     * l'écran : un retour identique lui ferait croire le produit compté.
     */
    fun failure() {
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, ERROR_BEEP_MS)
        vibrate(ERROR_VIBRATE_MS)
    }

    private fun vibrate(durationMs: Long) {
        vibrator?.takeIf { it.hasVibrator() }?.vibrate(
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /** Le ToneGenerator détient une piste audio native : à libérer avec l'écran */
    fun release() = tone.release()

    private companion object {
        const val TONE_VOLUME = 80
        const val BEEP_MS = 150
        const val VIBRATE_MS = 60L
        const val ERROR_BEEP_MS = 500
        const val ERROR_VIBRATE_MS = 300L
    }
}
