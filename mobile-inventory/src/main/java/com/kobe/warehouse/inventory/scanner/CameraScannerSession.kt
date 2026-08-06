package com.kobe.warehouse.inventory.scanner

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Branche la caméra arrière sur un [PreviewView] et pousse chaque image dans
 * [BarcodeAnalyzer]. Mutualisé entre le scan continu de l'écran d'inventaire et
 * l'écran de scan plein écran.
 */
class CameraScannerSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onBarcode: (barcode: String, format: String) -> Unit
) {

    private var analysisExecutor: ExecutorService? = null
    private var analyzer: BarcodeAnalyzer? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var torchOn = false

    fun start() {
        if (cameraProvider != null) return
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = try {
                providerFuture.get()
            } catch (e: Exception) {
                Log.e(TAG, "Caméra indisponible", e)
                return@addListener
            }
            bind(provider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bind(provider: ProcessCameraProvider) {
        val executor = Executors.newSingleThreadExecutor()
        val barcodeAnalyzer = BarcodeAnalyzer(onBarcode)

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            // Résolution : le défaut CameraX (640×480) suffit à un code linéaire mais
            // laisse moins de 2 pixels par module sur un DataMatrix pharma de 6 mm
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            ANALYSIS_RESOLUTION,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            // Les images accumulées pendant un décodage sont jetées : mieux vaut
            // analyser l'image courante que rattraper un retard de plusieurs images
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, barcodeAnalyzer) }

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            enableTapToFocus()
        } catch (e: Exception) {
            Log.e(TAG, "Échec du démarrage de la caméra", e)
            executor.shutdown()
            barcodeAnalyzer.close()
            return
        }

        cameraProvider = provider
        analysisExecutor = executor
        analyzer = barcodeAnalyzer
    }

    /**
     * Bascule la torche. Les rayons bas sont mal éclairés et c'est précisément là
     * que la lecture des DataMatrix échoue.
     */
    fun toggleTorch() {
        val control = camera?.cameraControl ?: return
        if (camera?.cameraInfo?.hasFlashUnit() != true) return
        torchOn = !torchOn
        control.enableTorch(torchOn)
    }

    /**
     * Mise au point sur le point touché et zoom par pincement.
     *
     * L'autofocus continu vise le centre et se cale volontiers sur l'étagère
     * derrière la boîte tenue en main ; pouvoir désigner le code règle les cas
     * récalcitrants. Le zoom, lui, agrandit le code dans l'image analysée sans
     * approcher la caméra — la mise au point rapprochée est justement ce que ces
     * capteurs réussissent le moins bien.
     */
    private fun enableTapToFocus() {
        val pinchDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val info = camera?.cameraInfo ?: return true
                    val current = info.zoomState.value?.zoomRatio ?: 1f
                    camera?.cameraControl?.setZoomRatio(current * detector.scaleFactor)
                    return true
                }
            }
        )

        previewView.setOnTouchListener { view, event ->
            pinchDetector.onTouchEvent(event)
            // Un pincement en cours ne doit pas être confondu avec une visée
            if (event.action == MotionEvent.ACTION_UP && !pinchDetector.isInProgress) {
                val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                camera?.cameraControl?.startFocusAndMetering(
                    FocusMeteringAction.Builder(point).build()
                )
                view.performClick()
            }
            true
        }
    }

    fun stop() {
        previewView.setOnTouchListener(null)
        camera = null
        torchOn = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        analyzer?.close()
        analyzer = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
    }

    companion object {
        private const val TAG = "CameraScannerSession"
        private val ANALYSIS_RESOLUTION = Size(1280, 720)
    }
}
