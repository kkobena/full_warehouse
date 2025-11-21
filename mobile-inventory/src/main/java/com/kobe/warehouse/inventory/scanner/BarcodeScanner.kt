package com.kobe.warehouse.inventory.scanner

import android.app.Activity
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

/**
 * Barcode Scanner Utility
 * Wrapper for ZXing barcode scanner
 * Supports various warehouse scanner devices (Sunmi, Honeywell, Zebra)
 */
class BarcodeScanner(private val activity: Activity) {

    companion object {
        const val REQUEST_CODE_SCAN = 1001
    }

    /**
     * Start barcode scanner
     * Opens camera for barcode scanning
     */
    fun startScan() {
        val integrator = IntentIntegrator(activity)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scanner un code-barres")
        integrator.setCameraId(0)  // Use back camera
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(true)  // Lock to landscape for tablets
        integrator.initiateScan()
    }

    /**
     * Parse scan result from activity result
     * Call this in onActivityResult()
     */
    fun parseScanResult(requestCode: Int, resultCode: Int, data: Intent?): ScanResult {
        if (requestCode == REQUEST_CODE_SCAN) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            return when {
                result == null || result.contents == null -> {
                    ScanResult.Cancelled
                }
                else -> {
                    ScanResult.Success(result.contents, result.formatName)
                }
            }
        }
        return ScanResult.Error("Invalid request code")
    }

    /**
     * Check if device has camera
     */
    fun hasCamera(): Boolean {
        return activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }
}

/**
 * Scan result sealed class
 */
sealed class ScanResult {
    data class Success(val barcode: String, val format: String) : ScanResult()
    object Cancelled : ScanResult()
    data class Error(val message: String) : ScanResult()
}
