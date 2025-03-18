package com.gsi.mrzandnfc.smartscanner

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@SuppressLint("ContextCastToActivity")
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    context: Activity,
    startScan: Boolean,
    onClosedActivity: () -> Unit,
    onResult: (bitmap: Bitmap,imageUri:Uri) -> Unit= { _,_ -> }
) {
   // val startScan = startScan

   // val context = LocalContext.current as Activity

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
           // startScan = false

            if (it.resultCode == Activity.RESULT_OK) {
                val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                result?.pages?.let { pages ->
                    for (page in pages) {
                        val imageUri = page.imageUri


                        //convert imageUri to bitmap

                        val bitmap = imageUri?.let { uri ->
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(context.contentResolver, uri)
                            )
                        }
                        if (bitmap != null) {
                            onResult(bitmap, imageUri)
                        }


                       // Storage.saveDoc(context, imageUri)
                        //viewModel.readDocs()
                    }
                }

            } else if (it.resultCode == Activity.RESULT_CANCELED) {
                onClosedActivity()
            }else
            {
                //Handle error case

                onClosedActivity()
            }
        //Handle cancelled case

        },

    )
    if (startScan) {
        Scan(
            activity = context,
            start = {
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(it)


                    .build())
            }
        )
    }

}

