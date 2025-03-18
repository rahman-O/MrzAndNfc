package com.gsi.mrzandnfc.smartscanner

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_BASE
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning


@Composable
fun Scan(
    activity: Activity,
    start: (IntentSender) -> Unit
) {
    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1)

        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .setScannerMode(SCANNER_MODE_BASE)

        .build()

    val scanner = remember {
        GmsDocumentScanning.getClient(options)

    }

    scanner.getStartScanIntent(activity)
        .addOnSuccessListener { intentSender ->
            start(intentSender)
        }
        .addOnFailureListener {
            Log.e("Scanner", "Failed to start scanning", it)
           Toast.makeText(activity, "Failed to start scanning", Toast.LENGTH_SHORT).show()
        }
}
