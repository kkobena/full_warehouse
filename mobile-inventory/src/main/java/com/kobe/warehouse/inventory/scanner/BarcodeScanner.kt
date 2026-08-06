package com.kobe.warehouse.inventory.scanner

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

/**
 * Point d'entrée du scan ponctuel : ouvre [ScanActivity] (CameraX + ML Kit) et
 * remonte le résultat.
 *
 * Le launcher doit être enregistré pendant `onCreate` (avant STARTED).
 */
class BarcodeScanner(
    private val activity: ComponentActivity,
    private val onResult: (ScanResult) -> Unit
) {

    private val launcher: ActivityResultLauncher<Unit> =
        activity.registerForActivityResult(ScanActivity.Contract()) { result -> onResult(result) }

    /** Ouvre la caméra pour un scan unique */
    fun startScan() = launcher.launch(Unit)

    /** Check if device has camera */
    fun hasCamera(): Boolean =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
}

/**
 * Scan result sealed class
 */
sealed class ScanResult {
    data class Success(val barcode: String, val format: String) : ScanResult()
    object Cancelled : ScanResult()
    data class Error(val message: String) : ScanResult()
}
