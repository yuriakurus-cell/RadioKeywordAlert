package com.example.radiokeywordalert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var keywordsEditText: EditText
    private lateinit var thresholdSeekBar: SeekBar
    private lateinit var thresholdValue: TextView
    private lateinit var audioSourceSpinner: Spinner
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) startServiceListening()
            else Toast.makeText(this, R.string.mic_required, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keywordsEditText = findViewById(R.id.keywordsEditText)
        thresholdSeekBar = findViewById(R.id.thresholdSeekBar)
        thresholdValue = findViewById(R.id.thresholdValue)
        audioSourceSpinner = findViewById(R.id.audioSourceSpinner)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        keywordsEditText.setText(prefs.getString("keywords", "тривога, допомога, пожежа, скасування тривоги"))
        thresholdSeekBar.progress = prefs.getInt("threshold", 60)
        thresholdValue.text = "${thresholdSeekBar.progress}%"

        // Setup spinner options
        val options = listOf("Авто (система)", "Вбудований мікрофон", "AUX / Гарнітура", "USB Line-In")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        audioSourceSpinner.adapter = adapter

        val savedIndex = prefs.getInt("audioSourceIndex", 0)
        audioSourceSpinner.setSelection(savedIndex)

        thresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                thresholdValue.text = "${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        startButton.setOnClickListener {
            prefs.edit()
                .putString("keywords", keywordsEditText.text.toString())
                .putInt("threshold", thresholdSeekBar.progress)
                .putInt("audioSourceIndex", audioSourceSpinner.selectedItemPosition)
                .apply()
            ensureMicPermissionAndStart()
        }

        stopButton.setOnClickListener {
            stopService(android.content.Intent(this, KeywordListenerService::class.java))
            Toast.makeText(this, R.string.stopped, Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureMicPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                startServiceListening()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(this, R.string.mic_required, Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startServiceListening() {
        val i = android.content.Intent(this, KeywordListenerService::class.java)
        ContextCompat.startForegroundService(this, i)
        Toast.makeText(this, R.string.started, Toast.LENGTH_SHORT).show()
    }
}
