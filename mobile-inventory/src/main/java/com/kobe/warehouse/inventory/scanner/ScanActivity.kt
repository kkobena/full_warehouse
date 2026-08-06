package com.kobe.warehouse.inventory.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kobe.warehouse.inventory.databinding.ActivityScanBinding
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scan ponctuel plein écran : CameraX + ML Kit, se ferme sur le premier code lu.
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private var session: CameraScannerSession? = null
    private val feedback by lazy { ScanFeedback(this) }

    /**
     * L'analyse tourne sur un thread dédié et plusieurs images peuvent contenir le
     * même code avant que l'activité ne se ferme : seul le premier résultat compte.
     */
    private val delivered = AtomicBoolean(false)

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startScanning()
        } else {
            finishWith(RESULT_CANCELED, null, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanning() {
        session = CameraScannerSession(this, this, binding.previewView) { barcode, format ->
            if (delivered.compareAndSet(false, true)) {
                runOnUiThread {
                    feedback.success()
                    finishWith(RESULT_OK, barcode, format)
                }
            }
        }.also { it.start() }
    }

    private fun finishWith(resultCode: Int, barcode: String?, format: String?) {
        val data = barcode?.let {
            Intent()
                .putExtra(EXTRA_BARCODE, it)
                .putExtra(EXTRA_FORMAT, format.orEmpty())
        }
        setResult(resultCode, data)
        finish()
    }

    override fun onDestroy() {
        session?.stop()
        session = null
        feedback.release()
        super.onDestroy()
    }

    /** Contrat de résultat : aucune entrée, un [ScanResult] en sortie */
    class Contract : ActivityResultContract<Unit, ScanResult>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(context, ScanActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): ScanResult {
            if (resultCode != Activity.RESULT_OK) return ScanResult.Cancelled
            val barcode = intent?.getStringExtra(EXTRA_BARCODE)
            return if (barcode.isNullOrBlank()) {
                ScanResult.Cancelled
            } else {
                ScanResult.Success(barcode, intent.getStringExtra(EXTRA_FORMAT).orEmpty())
            }
        }
    }

    companion object {
        private const val EXTRA_BARCODE = "extra_barcode"
        private const val EXTRA_FORMAT = "extra_format"
    }
}
