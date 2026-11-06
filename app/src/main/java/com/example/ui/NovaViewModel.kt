package com.example.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.NovaBrain
import com.example.data.*
import com.example.service.DeviceController
import com.example.voice.SpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

class NovaViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val weatherRepository = WeatherRepository()
    private val memoryDao = MemoryDatabase.getDatabase(application).memoryDao()
    private val memoryRepository = MemoryRepository(memoryDao)
    val deviceController = DeviceController(application)

    private val brain = NovaBrain(
        preferencesManager = preferencesManager,
        weatherRepository = weatherRepository,
        memoryRepository = memoryRepository,
        deviceController = deviceController
    )

    private val _config = MutableStateFlow(preferencesManager.getConfig())
    val config: StateFlow<NovaConfig> = _config.asStateFlow()

    private val _contacts = MutableStateFlow(preferencesManager.getContacts())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _notes = MutableStateFlow(preferencesManager.getNotes())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    val memories: StateFlow<List<MemoryEntity>> = memoryRepository.allMemories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val brightnessPercent: StateFlow<Int> = deviceController.brightnessPercent

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _youtubeValidationStatus = MutableStateFlow("")
    val youtubeValidationStatus: StateFlow<String> = _youtubeValidationStatus.asStateFlow()

    private var pendingActionCard: ActionCard? = null

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

        speechManager?.applyVoiceProfile(_config.value.selectedVoice)

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
                    _assistantState.value = AssistantState.THINKING
                }
            }
        }

        // Seed initial memory if Room DB is empty
        viewModelScope.launch {
            val existing = memoryRepository.getAllList()
            if (existing.isEmpty()) {
                memoryRepository.insert("Routine", "Gym starts at 6 PM")
                memoryRepository.insert("Preference", "Prefers witty Hinglish and Hindi responses")
            }
        }

        // Seed initial welcome conversation matching Screenshot 4
        val now = System.currentTimeMillis()
        val msg1 = ChatMessage(
            sender = MessageSender.NOVA,
            text = "Systems online, ${_config.value.userName}. I am ${_config.value.assistantName} — ready to inspect your screen, control flashlight/volume/brightness, automate YouTube, and manage WhatsApp with verbal confirmation. What would you like to do?",
            timestamp = now - 8000
        )
        val msg2 = ChatMessage(
            sender = MessageSender.USER,
            text = "हिंदी-हिंदी",
            timestamp = now - 4000
        )
        val msg3 = ChatMessage(
            sender = MessageSender.NOVA,
            text = "सर, मैं ${_config.value.assistantName} हूँ। मैं आपकी डिवाइस को एक्सेसिबिलिटी सर्विस से कंट्रोल कर सकता हूँ—स्क्रीन इंस्पेक्ट करना, एलिमेंट्स पर क्लिक करना, टेक्स्ट टाइप करना, स्क्रॉल करना, और WhatsApp या YouTube चलाना। बताइए अभी क्या आदेश है?",
            timestamp = now
        )
        _messages.value = listOf(msg1, msg2, msg3)
    }

    fun toggleOnline() {
        val updated = _config.value.copy(isOnline = !_config.value.isOnline)
        saveConfig(updated)
        if (!updated.isOnline) {
            interrupt()
        }
    }

    fun startListening() {
        if (!_config.value.isOnline) {
            val updated = _config.value.copy(isOnline = true)
            saveConfig(updated)
        }
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
        val sanitized = clean.replace(
            Regex("(?i)^(hey\\s+${Regex.escape(assistantNameLower)}|hey\\s+amancipher|hey\\s+jarvis|hey\\s+nova|suno\\s+${Regex.escape(assistantNameLower)})[,\\s]*"),
            ""
        ).trim()
        val query = sanitized.ifEmpty { clean }

        submitQuery(query)
    }

    fun submitQuery(userQuery: String) {
        if (userQuery.isBlank()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = userQuery)
        _messages.value = _messages.value + userMsg

        val lower = userQuery.trim().lowercase(Locale.ROOT)

        // Check verbal confirmation for pending WhatsApp / Call action!
        val pending = pendingActionCard
        if (pending != null && (lower == "yes" || lower == "haan" || lower == "ha" ||
                    lower.contains("bhej do") || lower.contains("send it") || lower.contains("confirm") || lower == "हाँ")
        ) {
            pendingActionCard = null
            when (pending) {
                is ActionCard.WhatsAppConfirm -> {
                    confirmWhatsApp(pending.phoneNumber, pending.message)
                    val confirmReply = ChatMessage(
                        sender = MessageSender.NOVA,
                        text = "Verbal confirmation received! Opening WhatsApp for ${pending.contactName}..."
                    )
                    _messages.value = _messages.value + confirmReply
                    return
                }
                is ActionCard.CallConfirm -> {
                    confirmCall(pending.phoneNumber, pending.viaWhatsApp)
                    val confirmReply = ChatMessage(
                        sender = MessageSender.NOVA,
                        text = "Verbal confirmation received! Initiating call to ${pending.contactName}..."
                    )
                    _messages.value = _messages.value + confirmReply
                    return
                }
                else -> {}
            }
        }

        _assistantState.value = AssistantState.THINKING

        viewModelScope.launch {
            val response = brain.processQuery(userQuery)

            if (response.actionCard is ActionCard.WhatsAppConfirm || response.actionCard is ActionCard.CallConfirm) {
                pendingActionCard = response.actionCard
            }

            val novaMsg = ChatMessage(
                sender = MessageSender.NOVA,
                text = response.spokenText,
                actionCard = response.actionCard
            )
            _messages.value = _messages.value + novaMsg

            _assistantState.value = AssistantState.SPEAKING
            speechManager?.speak(response.spokenText, _config.value.selectedVoice)

            response.appLaunchQuery?.let { appQuery ->
                deviceController.buildOpenAppIntent(appQuery)?.let { intent ->
                    _externalIntent.emit(intent)
                }
            }

            response.autoActionUrl?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                _externalIntent.emit(intent)
            }
        }
    }

    fun analyzeVision(bitmap: Bitmap?, prompt: String) {
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = "🖥 [Vision Scan]: $prompt"
        )
        _messages.value = _messages.value + userMsg
        _assistantState.value = AssistantState.THINKING

        viewModelScope.launch {
            val analysis = brain.analyzeVisionImage(bitmap, prompt)
            val reply = ChatMessage(
                sender = MessageSender.NOVA,
                text = analysis
            )
            _messages.value = _messages.value + reply
            _assistantState.value = AssistantState.SPEAKING
            speechManager?.speak(analysis, _config.value.selectedVoice)
        }
    }

    fun validateYouTubeKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _youtubeValidationStatus.value = "Status: Key is empty"
            return
        }
        _youtubeValidationStatus.value = "Status: Validating key..."
        viewModelScope.launch {
            val valid = brain.validateYouTubeApiKey(apiKey)
            _youtubeValidationStatus.value = if (valid) {
                "Status: Validated ✓ (YouTube Data API v3 Active)"
            } else {
                "Status: Key saved (Fallback playback active)"
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

            speechManager?.speak("WhatsApp khol raha hu, message pre-filled hai!", _config.value.selectedVoice)
        }
    }

    fun confirmCall(phoneNumber: String, viaWhatsApp: Boolean = false) {
        viewModelScope.launch {
            val intent = if (viaWhatsApp) {
                val cleanPhone = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            _externalIntent.emit(intent)
            speechManager?.speak("Call connect kar raha hu.", _config.value.selectedVoice)
        }
    }

    fun openAccessibilitySettings() {
        viewModelScope.launch {
            _externalIntent.emit(deviceController.buildAccessibilitySettingsIntent())
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

    fun addMemory(category: String, content: String) {
        viewModelScope.launch {
            memoryRepository.insert(category, content)
        }
    }

    fun deleteMemory(id: Int) {
        viewModelScope.launch {
            memoryRepository.deleteById(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAll()
        }
    }

    fun saveConfig(newConfig: NovaConfig) {
        _config.value = newConfig
        preferencesManager.saveConfig(newConfig)
        speechManager?.applyVoiceProfile(newConfig.selectedVoice)
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
