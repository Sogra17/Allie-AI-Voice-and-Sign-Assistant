package com.example.handsignrecognizer


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HandAnalyzer(
    private val context: android.content.Context,
    private val classifier: HandSignClassifier,
    private val predictionText: TextView
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        setupLandmarker()
    }


    private fun setupLandmarker() {
        val assetManager = context.assets

        val inputStream = assetManager.open("hand_landmarker.task")
        val modelBytes = inputStream.readBytes()
        val modelBuffer = java.nio.ByteBuffer.allocateDirect(modelBytes.size).apply {
            put(modelBytes)
            rewind()
        }

        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(modelBuffer)
            .build()

        val options = HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(1)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }



    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage: Image = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // Convert to bitmap
        val bitmap = BitmapUtils.imageToBitmap(mediaImage, rotationDegrees)

        // Flip for selfie view
        val flipped = flipBitmap(bitmap)

        executor.execute {
            val mpImage = BitmapImageBuilder(flipped).build()

            val result: HandLandmarkerResult = handLandmarker!!.detect(mpImage)

            if (result.handedness().isNotEmpty()) {
                val landmarks = result.landmarks()[0]

                val inputArray = FloatArray(63) // 21 landmarks * 3 (x,y,z)

                for ((i, lm) in landmarks.withIndex()) {
                    inputArray[i * 3] = lm.x()
                    inputArray[i * 3 + 1] = lm.y()
                    inputArray[i * 3 + 2] = lm.z()
                }

                val prediction = classifier.classify(inputArray)

                predictionText.post {
                    val text = context.getString(R.string.gesture_text, prediction)
                    predictionText.text = text
                    if (prediction == "open_youtube") {
                        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                        intent?.let { context.startActivity(it) }
                    }
                    if (prediction == "open_flashlight") {
                        val camManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                        val cameraId = camManager.cameraIdList[0]
                        camManager.setTorchMode(cameraId, true)
                    }
                }
            }

            imageProxy.close()
        }
    }

    private fun flipBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}