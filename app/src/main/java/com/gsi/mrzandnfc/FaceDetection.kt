package com.gsi.mrzandnfc

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FaceDetectionHelper {

    companion object {
        private const val TAG = "FaceDetectionHelper"
        private const val SCALING_FACTOR = 10
    }

    private val faceDetector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
        )
    }

    suspend fun detectFace(bitmap: Bitmap?, context: Context): Bitmap? {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null")
            return null
        }

        return try {
            analyzeImage(bitmap, context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in face detection: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error in face detection: ${e.message}", Toast.LENGTH_LONG).show()
            }
            null
        }
    }

    private suspend fun analyzeImage(bitmap: Bitmap, context: Context): Bitmap? {
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmap.width / SCALING_FACTOR,
            bitmap.height / SCALING_FACTOR,
            false
        )

        val inputImage = InputImage.fromBitmap(scaledBitmap, 0)

        return try {
            val faces = faceDetector.process(inputImage).await()
            if (faces.isNotEmpty()) {
                for (face in faces) {
                    val rect =face.boundingBox
                    rect.set(
                        rect.left * SCALING_FACTOR,
                        rect.top * (SCALING_FACTOR-1),
                        rect.right * (SCALING_FACTOR),
                        rect.bottom * SCALING_FACTOR*90
                    )
                }
                val result = cropFaceDetected(faces, bitmap)
                scaledBitmap.recycle() // Free memory
                result
            } else {
                Log.w(TAG, "No faces detected")
                scaledBitmap.recycle()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Face detection failed: ${e.message}", e)
            scaledBitmap.recycle()
            null
        }
    }

    private fun cropFaceDetected(faces: List<Face>, bitmap: Bitmap): Bitmap? {
        val face = faces.firstOrNull() ?: return null
        val rect = face.boundingBox

        val x = Math.max(rect.left, 0)
        val y = Math.max(rect.top , 0)
        val width = rect.width()
        val height = rect.height()
        val croppedBitmap = Bitmap.createBitmap(
            bitmap, x, y - SCALING_FACTOR * 9,
            if (x + width > bitmap.width) bitmap.width - x else width,
            if (y + height > bitmap.height) bitmap.height - y else height
        )

       // bitmap.recycle() // Free memory after processing
        return croppedBitmap
    }

    private fun enhanceBitmap(bitmap: Bitmap): Bitmap {
        val enhancedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
        val canvas = android.graphics.Canvas(enhancedBitmap)
        val paint = android.graphics.Paint()

        // تحسين التباين والسطوع
        val colorMatrix = android.graphics.ColorMatrix().apply {
            setScale(1.2f, 1.2f, 1.2f, 1f) // زيادة السطوع بنسبة 20%
        }
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return enhancedBitmap
    }
}
