package dev.cazimir.floatnote.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechState {
    object Idle : SpeechState()
    object Initializing : SpeechState()
    object Listening : SpeechState()
    object Stopping : SpeechState()
    data class Error(val code: Int, val message: String) : SpeechState()
}

class SpeechRecognitionManager(private val context: Context) {

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state = _state.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText = _recognizedText.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    
    // To prevent rapid toggling
    private var lastToggleTime = 0L
    private val TOGGLE_DEBOUNCE_MS = 500L

    // Scope for internal operations like debouncing or retries
    private val scope = CoroutineScope(Dispatchers.Main)
    private var retryJob: Job? = null

    fun initialize(language: String) {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        }
        
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Add silence detection extras
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
    }

    fun startListening() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToggleTime < TOGGLE_DEBOUNCE_MS) return
        lastToggleTime = currentTime

        if (_state.value is SpeechState.Listening || _state.value is SpeechState.Initializing) return

        _state.value = SpeechState.Initializing
        
        try {
            if (speechRecognizer == null) {
                // Should have been initialized, but just in case
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(recognitionListener)
                }
            }
            speechRecognizer?.startListening(recognitionIntent)
            // State will be updated to Listening in onBeginningOfSpeech or manually if needed
            // But usually onBeginningOfSpeech is a bit late, so we can set it here or wait.
            // Let's wait for onReadyForSpeech or just set it to Listening to update UI immediately
            _state.value = SpeechState.Listening
        } catch (e: Exception) {
            _state.value = SpeechState.Error(0, "Failed to start: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToggleTime < TOGGLE_DEBOUNCE_MS) return
        lastToggleTime = currentTime

        if (_state.value !is SpeechState.Listening) return

        _state.value = SpeechState.Stopping
        speechRecognizer?.stopListening()
    }

    fun reset() {
        stopListening()
        _state.value = SpeechState.Idle
        _recognizedText.value = ""
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    fun updateText(newText: String) {
        _recognizedText.value = newText
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            // Ready to accept speech
        }

        override fun onBeginningOfSpeech() {
            _state.value = SpeechState.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            // User stopped speaking, waiting for results
            // We can transition to Stopping or just wait for results
        }

        override fun onError(error: Int) {
            val errorMessage = getErrorText(error)
            
            // Handle "No Match" or "Timeout" as soft errors (end of session)
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                 // Just go back to idle, maybe retry if we want continuous listening
                 // For now, let's go to Idle
                 _state.value = SpeechState.Idle
            } else if (error == SpeechRecognizer.ERROR_CLIENT && _state.value == SpeechState.Stopping) {
                // Ignore client errors during stopping
                _state.value = SpeechState.Idle
            } else {
                _state.value = SpeechState.Error(error, errorMessage)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                appendText(text)
            }
            _state.value = SpeechState.Idle
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // We could show partial results if we want
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun appendText(newText: String) {
        val current = _recognizedText.value
        _recognizedText.value = if (current.isBlank() || current.endsWith(" ")) {
            current + newText
        } else {
            "$current $newText"
        }
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Error $errorCode"
        }
    }
}
