package com.example.radiokeywordalert

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class KeywordListenerService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        startForeground(1, NotificationHelper.buildForeground(this).build())
        startRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopRecognizer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("KeywordListenerService", "Speech recognition not available")
            stopSelf()
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { restart() }
                override fun onResults(results: Bundle) {
                    handleResults(results)
                    restart()
                }
                override fun onPartialResults(partialResults: Bundle) {
                    handleResults(partialResults, partial = true)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevice = getPreferredAudioDevice()
            if (audioDevice != null) {
                try {
                    recognizer?.setPreferAudioDevice(audioDevice)
                    Log.i("KeywordListenerService", "Використовується пристрій: ${audioDevice.type}")
                } catch (e: Exception) {
                    Log.w("KeywordListenerService", "Не вдалося вибрати пристрій: ${e.message}")
                }
            }
        }

        startListening()
    }

    private fun getPreferredAudioDevice(): AudioDeviceInfo? {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val choice = prefs.getInt("audioSourceIndex", 0)

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val inputs = am.getDevices(AudioManager.GET_DEVICES_INPUTS)

        return when (choice) {
            1 -> inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
            2 -> inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
            3 -> inputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                it.type == AudioDeviceInfo.TYPE_LINE_DIGITAL
            }
            else -> null
        }
    }

    private fun startListening() {
        if (isListening) return
        val intent = RecognizerIntent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
        }
        recognizer?.startListening(intent)
        isListening = true
    }

    private fun stopRecognizer() {
        isListening = false
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun restart() {
        isListening = false
        startListening()
    }

    private fun handleResults(bundle: Bundle, partial: Boolean = false) {
        val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        val text = results.joinToString(" ").lowercase()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val keywords = prefs.getString("keywords", "") ?: ""
        val threshold = prefs.getInt("threshold", 60)

        val match = KeywordMatcher.bestMatch(text, keywords) ?: return
        if (match.score >= threshold) {
            NotificationHelper.notifyAlert(
                this,
                "Виявлено: ${match.keyword} (${match.score}%)",
                "Текст: ${text.take(80)}"
            )
        }
    }
}
