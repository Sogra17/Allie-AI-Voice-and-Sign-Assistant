package com.example.handsignrecognizer

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.MappedByteBuffer
import java.nio.charset.Charset

class HandSignClassifier(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        val modelBuffer = loadModelFile("hand_model.tflite")
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels() {
        labels = context.assets.open("labels.txt").bufferedReader(Charset.defaultCharset()).readLines()
    }

    fun classify(input: FloatArray): String {
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(arrayOf(input), output)
        val probabilities = output[0]
        val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        return if (maxIdx != -1) labels[maxIdx] else "Unknown"
    }
}