package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onResultReceived: (String) -> Unit,
    private val onStateChanged: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var currentVoiceName: String = "Puck"

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var waveSimulationJob: Job? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val hiLocale = Locale("hi", "IN")
                val langResult = tts?.setLanguage(hiLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.ENGLISH
                }
                applyVoiceProfile(currentVoiceName)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        onStateChanged(true)
                        startSpeakingWaveAnimation()
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopSpeakingWaveAnimation()
                        _amplitude.value = 0f
                        onStateChanged(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopSpeakingWaveAnimation()
                        _amplitude.value = 0f
                        onStateChanged(false)
                    }
                })
            }
        }
    }

    fun applyVoiceProfile(voiceName: String) {
        currentVoiceName = voiceName
        if (!isTtsInitialized) return
        val (pitch, rate) = when (voiceName) {
            "Puck" -> 1.08f to 1.05f
            "Charon" -> 0.85f to 0.96f
            "Kore" -> 1.18f to 1.02f
            "Fenrir" -> 0.78f to 1.0f
            "Aoede" -> 1.25f to 1.03f
            "Leda" -> 1.15f to 0.98f
            "Orus" -> 0.92f to 1.04f
            "Zephyr" -> 1.20f to 1.06f
            else -> 1.10f to 1.02f
        }
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
    }

    fun startListening() {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                        _amplitude.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _amplitude.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _amplitude.value = 0f
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _amplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResultReceived(matches[0])
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            _amplitude.value = 0f
        } catch (_: Exception) {}
    }

    fun speak(text: String, voiceName: String = currentVoiceName) {
        if (!isTtsInitialized || tts == null) return
        stopListening()
        stopSpeaking()
        applyVoiceProfile(voiceName)

        val cleanText = text.replace(Regex("[*#_`~]"), "")
        val utteranceId = "AMANCIPHER_${System.currentTimeMillis()}"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        stopSpeakingWaveAnimation()
        _amplitude.value = 0f
    }

    fun interrupt() {
        stopListening()
        stopSpeaking()
    }

    private fun startSpeakingWaveAnimation() {
        stopSpeakingWaveAnimation()
        waveSimulationJob = coroutineScope.launch {
            while (isActive && _isSpeaking.value) {
                val base = 0.45f + (Math.sin(System.currentTimeMillis() / 120.0).toFloat() * 0.35f)
                val noise = ((Math.random() - 0.5f) * 0.2f).toFloat()
                _amplitude.value = (base + noise).coerceIn(0.15f, 1.0f)
                delay(60)
            }
        }
    }

    private fun stopSpeakingWaveAnimation() {
        waveSimulationJob?.cancel()
        waveSimulationJob = null
    }

    fun destroy() {
        coroutineScope.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
