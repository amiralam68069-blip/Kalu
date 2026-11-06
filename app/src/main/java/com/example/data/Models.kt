package com.example.data

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class MessageSender {
    USER,
    NOVA
}

sealed class ActionCard {
    data class WhatsAppConfirm(
        val contactName: String,
        val phoneNumber: String,
        val message: String,
        val isVideoCall: Boolean = false
    ) : ActionCard()

    data class CallConfirm(
        val contactName: String,
        val phoneNumber: String,
        val viaWhatsApp: Boolean = false
    ) : ActionCard()

    data class WeatherCard(
        val location: String,
        val tempC: Double,
        val weatherCode: Int,
        val description: String,
        val humidity: Int,
        val windSpeed: Double
    ) : ActionCard()

    data class YouTubePlayerCard(
        val query: String,
        val videoUrl: String,
        val title: String,
        val videoId: String = ""
    ) : ActionCard()

    data class AlarmCard(
        val time: String,
        val label: String
    ) : ActionCard()

    data class NoteCard(
        val id: String,
        val text: String,
        val isCompleted: Boolean
    ) : ActionCard()

    data class DeviceActionCard(
        val actionTitle: String,
        val detail: String,
        val statusText: String
    ) : ActionCard()

    data class ScreenInspectCard(
        val activePackage: String,
        val elementCount: Int,
        val summary: List<String>
    ) : ActionCard()
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionCard: ActionCard? = null
)

data class Contact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val relation: String = "Friend"
)

data class NoteItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDone: Boolean = false
)

val AVAILABLE_VOICES = listOf(
    "Puck", "Charon", "Kore", "Fenrir",
    "Aoede", "Leda", "Orus", "Zephyr"
)

val AVAILABLE_AI_MODELS = listOf(
    "Gemini 3.1 Flash Live (Ultra-Low Latency - Default)",
    "Gemini 2.5 Flash (Balanced Speed & Reasoning)",
    "Gemini 2.5 Pro (Deep Intelligence & Coding)"
)

val AVAILABLE_PERSONALITY_MODES = listOf(
    "Assistant Mode",
    "Witty Cybernetic",
    "Tactical JARVIS",
    "Friendly Companion"
)

data class NovaConfig(
    val assistantName: String = "Amancipher.Ai",
    val userName: String = "Amancipher",
    val wakeWordEnabled: Boolean = true,
    val isOnline: Boolean = true,
    val apiKey: String = "",
    val aiModel: String = AVAILABLE_AI_MODELS.first(),
    val selectedVoice: String = "Puck",
    val personalityMode: String = "Assistant Mode",
    val traitFriendly: Boolean = true,
    val traitHelpful: Boolean = true,
    val traitHinglishOrEnglish: Boolean = true,
    val youtubeAutomationEnabled: Boolean = true,
    val youtubeApiKey: String = "",
    val glowThemeIndex: Int = 0 // 0 = Cyan, 1 = Purple, 2 = Rose, 3 = Emerald
)
