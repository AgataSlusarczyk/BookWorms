package com.bookworms.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.CloseTitle
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanIsbnScreen(
    // The code the camera read, or null while it's still looking for one.
    recognizedIsbn: String?,
    onIsbnRecognized: (String) -> Unit = {},
    onEnterIsbnManuallyClick: () -> Unit,
    onAddWithoutIsbnClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    var hasCameraPermission by remember {
        mutableStateOf(
            isPreview || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    // Ask for camera permission as soon as the screen opens.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .mistedBackground()
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        CloseTitle(title = stringResource(R.string.scan_isbn_title), onCloseClick = onCloseClick)

        Spacer(Modifier.height(16.dp))

        // weight(1f) = take the space that's left, so the buttons stay at the bottom.
        CameraViewport(
            hasPermission = hasCameraPermission,
            isScanningEnabled = recognizedIsbn == null,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onIsbnDetected = onIsbnRecognized,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        if (recognizedIsbn != null) {
            Spacer(Modifier.height(12.dp))
            RecognizedCode(recognizedIsbn)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEnterIsbnManuallyClick,
            shape = FieldShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            // null, because the button text already says what it does.
            Icon(
                painter = painterResource(R.drawable.ic_barcode_scan),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.enter_isbn_manually))
        }

        TextButton(
            onClick = onAddWithoutIsbnClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.add_book_without_isbn))
        }
    }
}

@Composable
private fun CameraViewport(
    hasPermission: Boolean,
    isScanningEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onIsbnDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.radialGradient(listOf(CameraGray, CameraBlack)),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        when {
            !hasPermission -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRequestPermission,
                        shape = FieldShape,
                    ) {
                        Text(stringResource(R.string.grant_camera_permission))
                    }
                }
            }

            !isPreview -> {
                CameraPreviewWithAnalysis(
                    isScanningEnabled = isScanningEnabled,
                    onIsbnDetected = onIsbnDetected,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (hasPermission) {
            // Aiming frame and red laser line drawn on top of the camera feed.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.73f)
                    .aspectRatio(2f)
                    .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(2.dp)
                        .background(ScanLineRed),
                )
            }

            Text(
                text = stringResource(R.string.scan_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun CameraPreviewWithAnalysis(
    isScanningEnabled: Boolean,
    onIsbnDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processFrameForIsbn(
                                imageProxy = imageProxy,
                                isScanningEnabled = isScanningEnabled,
                                scanner = scanner,
                                onIsbnDetected = onIsbnDetected,
                            )
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (_: Exception) {
                    // Camera binding failed.
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processFrameForIsbn(
    imageProxy: ImageProxy,
    isScanningEnabled: Boolean,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onIsbnDetected: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null || !isScanningEnabled) {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                val rawValue = barcode.rawValue ?: continue
                val digitsOnly = rawValue.filter { it.isDigit() }
                // Valid ISBN-13 barcodes are 13 digits and start with 978 or 979.
                if (digitsOnly.length == 13 && (digitsOnly.startsWith("978") || digitsOnly.startsWith("979"))) {
                    onIsbnDetected(digitsOnly)
                    break
                }
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

// Light blue box with the code the camera read.
@Composable
private fun RecognizedCode(isbn: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), FieldShape)
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.recognized_code),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = isbn,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.fetching_book_details),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val CameraBlack = Color(0xFF0C1115)
private val CameraGray = Color(0xFF262B30)
private val ScanLineRed = Color(0xFFC03B36)

@Preview(device = "id:pixel_8")
@Composable
private fun ScanIsbnScreenPreview() {
    BookWormsTheme {
        ScanIsbnScreen(
            recognizedIsbn = "978-83-08-04815-3",
            onEnterIsbnManuallyClick = {},
            onAddWithoutIsbnClick = {},
            onCloseClick = {},
        )
    }
}