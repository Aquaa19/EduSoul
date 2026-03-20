// File: EduSoul/app/src/main/java/com/aquaa/edusoul/activities/manager/ScanQrActivity.kt
package com.aquaa.edusoul.activities.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage

@OptIn(ExperimentalGetImage::class)
class ScanQrActivity : BaseActivity() {

    private val TAG = "ScanQrActivity"
    private lateinit var previewView: PreviewView
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var barcodeScanner: BarcodeScanner

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Camera permission granted.")
            startCamera()
        } else {
            Log.e(TAG, "Camera permission denied.")
            showToast("Camera permission is required to scan QR codes.")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)

        previewView = findViewById(R.id.previewView)

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        cameraProvider?.unbindAll()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // This reference should be resolved with CameraX 1.3.3
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider?.bindToLifecycle(
                this,
                cameraSelector as CameraSelector,
                preview as Preview,
                imageAnalysis as ImageAnalysis
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            showToast("Failed to start camera for scanning.")
            finish()
        }
    }

    private fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            Log.d(TAG, "QR Code scanned: $rawValue")
                            val resultIntent = Intent().apply {
                                putExtra("SCANNED_QR_CODE", rawValue)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                            return@addOnSuccessListener
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}