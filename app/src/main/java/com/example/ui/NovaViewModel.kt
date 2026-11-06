package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.NovaBrain
import com.example.data.*
import com.example.voice.SpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder

class NovaViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val weatherRepository = WeatherRepository()
    private val brain = NovaBrain(preferencesManager, weatherRepository)

    private val _config = MutableStateFlow(preferencesManager.getConfig())
    val config: StateFlow<NovaConfig> = _config.asStateFlow()

    private val _contacts = MutableStateFlow(preferencesManager.getContacts())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _notes = MutableStateFlow(preferencesManager.getNotes())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    // Single-event navigation / external intent launcher
    private val _externalIntent = MutableSharedFlow<Intent>()
    val externalIntent: SharedFlow<Intent> = _externalIntent.asSharedFlow()

    private var speechManager: SpeechManager? = null

    init {
        speechManager = SpeechManager(
            context = application,
            onResultReceived = { spokenResult ->
                onUserSpoke(spokenResult)
            },
            onStateChanged = { isSpeaking ->
                if (isSpeaking) {
                    _assistantState.value = AssistantState.SPEAKING
                } else if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        )

        viewModelScope.launch {
            speechManager?.amplitude?.collect { amp ->
                _amplitude.value = amp
            }
        }

        viewModelScope.launch {
            speechManager?.isListening?.collect { listening ->
                if (listening) {
                    _assistantState.value = AssistantState.LISTENING
                } else if (_assistantState.value == AssistantState.LISTENING) {
                    // Speech finished, moving to thinking
                    _assistantState.value = AssistantState.THINKING
                }
            }
        }

        // Add welcome message from NOVA
        val welcome = ChatMessage(
            sender = MessageSender.NOVA,
            text = "Namaste ${_config.value.userName}! Main hu ${_config.value.assistantName}. Aaj main aapki kya madad kar sakti hu? YouTube gaana, WhatsApp message, weather ya alarms?",
            actionCard = null
        )
        _messages.value = listOf(welcome)
    }

    fun startListening() {
        speechManager?.startListening()
    }

    fun stopListening() {
        speechManager?.stopListening()
    }

    fun interrupt() {
        speechManager?.interrupt()
        _assistantState.value = AssistantState.IDLE
    }

    fun onUserSpoke(spokenText: String) {
        val clean = spokenText.trim()
        if (clean.isBlank()) {
            _assistantState.value = AssistantState.IDLE
            return
        }

        val assistantNameLower = _config.value.assistantName.lowercase()
        // Check for wake word filter
        val sanitized = clean.replace(Regex("(?i)^(hey\\s+${assistantNameLower}|suno\\s+${assistantNameLower}|${assistantNameLower})[,\\s]*"), "").trim()
        val query = sanitized.ifEmpty { clean }

        submitQuery(query)
    }

    fun submitQuery(userQuery: String) {
        if (userQuery.isBlank()) return

        // 1. Append user message
        val userMsg = ChatMessage(sender = MessageSender.USER, text = userQuery)
        _messages.value = _messages.value + userMsg

        // 2. Set Thinking state
        _assistantState.value = AssistantState.THINKING

        viewModelScope.launch {
            val response = brain.processQuery(userQuery)

            // 3. Append NOVA's reply
            val novaMsg = ChatMessage(
                sender = MessageSender.NOVA,
                text = response.spokenText,
                actionCard = response.actionCard
            )
            _messages.value = _messages.value + novaMsg

            // 4. Speak response
            _assistantState.value = AssistantState.SPEAKING
            speechManager?.speak(response.spokenText)

            // 5. If auto action is present (like YouTube playback search)
            response.autoActionIntent?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                _externalIntent.emit(intent)
            }
        }
    }

    fun confirmWhatsApp(phoneNumber: String, message: String) {
        viewModelScope.launch {
            val cleanPhone = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val waUrl = "https://wa.me/$cleanPhone?text=$encodedMsg"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            _externalIntent.emit(intent)

            speechManager?.speak("WhatsApp khol rahi hu, message pre-filled hai!")
        }
    }

    fun confirmCall(phoneNumber: String) {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            _externalIntent.emit(intent)
            speechManager?.speak("Phone dialer open kar rahi hu.")
        }
    }

    fun openUrl(url: String) {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            _externalIntent.emit(intent)
        }
    }

    fun toggleNote(noteId: String) {
        val currentNotes = _notes.value.map {
            if (it.id == noteId) it.copy(isDone = !it.isDone) else it
        }
        _notes.value = currentNotes
        preferencesManager.saveNotes(currentNotes)
    }

    fun saveConfig(newConfig: NovaConfig) {
        _config.value = newConfig
        preferencesManager.saveConfig(newConfig)
    }

    fun addContact(name: String, phone: String, relation: String) {
        val updated = _contacts.value + Contact(name = name, phone = phone, relation = relation)
        _contacts.value = updated
        preferencesManager.saveContacts(updated)
    }

    fun deleteContact(contactId: String) {
        val updated = _contacts.value.filterNot { it.id == contactId }
        _contacts.value = updated
        preferencesManager.saveContacts(updated)
    }

    fun clearChat() {
        _messages.value = emptyList()
        speechManager?.stopSpeaking()
        _assistantState.value = AssistantState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
    }
}
