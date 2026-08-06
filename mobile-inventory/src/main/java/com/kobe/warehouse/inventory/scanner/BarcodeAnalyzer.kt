package com.kobe.warehouse.inventory.scanner

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Analyseur d'images CameraX adossé à ML Kit.
 *
 * ML Kit remplace ZXing pour le DataMatrix : son décodeur tolère les codes petits,
 * imprimés en creux ou légèrement déformés sur lesquels ZXing échouait, et le
 * modèle est embarqué dans l'APK (aucun téléchargement Play Services à la première
 * utilisation, l'inventaire devant fonctionner sur un réseau isolé).
 *
 * Les formats sont restreints à ceux du conditionnement pharmaceutique : chaque
 * format supplémentaire coûte du temps de décodage par image.
 */
class BarcodeAnalyzer(
    private val onBarcode: (barcode: String, format: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        scanner.process(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { barcode ->
                    barcode.rawValue?.takeIf { it.isNotBlank() }?.to(formatName(barcode.format))
                }?.let { (value, format) -> onBarcode(value, format) }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Échec de décodage", e) }
            // L'image doit être libérée quel que soit le résultat, sinon l'analyse
            // se bloque dès que le tampon CameraX est plein
            .addOnCompleteListener { image.close() }
    }

    /** Libère le détecteur ML Kit (à appeler quand l'écran n'analyse plus) */
    fun close() = scanner.close()

    private fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        else -> "UNKNOWN"
    }

    companion object {
        private const val TAG = "BarcodeAnalyzer"
    }
}
