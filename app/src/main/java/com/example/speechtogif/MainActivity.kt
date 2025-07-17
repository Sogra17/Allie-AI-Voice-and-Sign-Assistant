package com.example.speechtogif

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_SPEECH = 1
    private lateinit var gifImageView: ImageView
    private lateinit var recognizedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val speakButton = findViewById<Button>(R.id.speakButton)
        recognizedText = findViewById(R.id.recognizedText)
        gifImageView = findViewById(R.id.gifImageView)

        speakButton.setOnClickListener {
            startSpeechRecognition()
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)?.lowercase(Locale.getDefault()) ?: ""
            recognizedText.text = spokenText
            showGifForText(spokenText)
        }
    }

    private fun showGifForText(text: String) {
        val gifMap = mapOf(
            "hello" to "hello.gif",
            "how are you" to "how_are_you.gif",
            "i am fine" to "im_fine.gif",
            "goodbye" to "goodbye.gif",
            "i don't know" to "i_don't_know.gif"
        )

        val gifName = gifMap[text.trim()]
        if (gifName != null) {
            Glide.with(this)
                .asGif()
                .load("file:///android_asset/$gifName")
                .into(gifImageView)
        } else {
            Toast.makeText(this, "No matching GIF found", Toast.LENGTH_SHORT).show()
        }
    }
}
