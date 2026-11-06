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
        val message: String
    ) : ActionCard()

    data class CallConfirm(
        val contactName: String,
        val phoneNumber: String
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
        val title: String
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

data class NovaConfig(
    val assistantName: String = "NOVA",
    val userName: String = "Aman",
    val wakeWordEnabled: Boolean = true,
    val apiKey: String = "",
    val glowThemeIndex: Int = 0 // 0 = Cyan, 1 = Purple, 2 = Rose, 3 = Emerald
)
