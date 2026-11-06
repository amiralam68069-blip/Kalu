package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ASSISTANT_NAME = "assistant_name_v2"
        private const val KEY_USER_NAME = "user_name_v2"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_IS_ONLINE = "is_online"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val KEY_PERSONALITY_MODE = "personality_mode"
        private const val KEY_TRAIT_FRIENDLY = "trait_friendly"
        private const val KEY_TRAIT_HELPFUL = "trait_helpful"
        private const val KEY_TRAIT_HINGLISH = "trait_hinglish"
        private const val KEY_YOUTUBE_ENABLED = "youtube_enabled"
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_GLOW_THEME = "glow_theme"
        private const val KEY_CONTACTS_JSON = "contacts_json"
        private const val KEY_NOTES_JSON = "notes_json"
    }

    fun getConfig(): NovaConfig {
        return NovaConfig(
            assistantName = prefs.getString(KEY_ASSISTANT_NAME, "Amancipher.Ai") ?: "Amancipher.Ai",
            userName = prefs.getString(KEY_USER_NAME, "Amancipher") ?: "Amancipher",
            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true),
            isOnline = prefs.getBoolean(KEY_IS_ONLINE, true),
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            aiModel = prefs.getString(KEY_AI_MODEL, AVAILABLE_AI_MODELS.first()) ?: AVAILABLE_AI_MODELS.first(),
            selectedVoice = prefs.getString(KEY_SELECTED_VOICE, "Puck") ?: "Puck",
            personalityMode = prefs.getString(KEY_PERSONALITY_MODE, "Assistant Mode") ?: "Assistant Mode",
            traitFriendly = prefs.getBoolean(KEY_TRAIT_FRIENDLY, true),
            traitHelpful = prefs.getBoolean(KEY_TRAIT_HELPFUL, true),
            traitHinglishOrEnglish = prefs.getBoolean(KEY_TRAIT_HINGLISH, true),
            youtubeAutomationEnabled = prefs.getBoolean(KEY_YOUTUBE_ENABLED, true),
            youtubeApiKey = prefs.getString(KEY_YOUTUBE_API_KEY, "") ?: "",
            glowThemeIndex = prefs.getInt(KEY_GLOW_THEME, 0)
        )
    }

    fun saveConfig(config: NovaConfig) {
        prefs.edit()
            .putString(KEY_ASSISTANT_NAME, config.assistantName)
            .putString(KEY_USER_NAME, config.userName)
            .putBoolean(KEY_WAKE_WORD_ENABLED, config.wakeWordEnabled)
            .putBoolean(KEY_IS_ONLINE, config.isOnline)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_AI_MODEL, config.aiModel)
            .putString(KEY_SELECTED_VOICE, config.selectedVoice)
            .putString(KEY_PERSONALITY_MODE, config.personalityMode)
            .putBoolean(KEY_TRAIT_FRIENDLY, config.traitFriendly)
            .putBoolean(KEY_TRAIT_HELPFUL, config.traitHelpful)
            .putBoolean(KEY_TRAIT_HINGLISH, config.traitHinglishOrEnglish)
            .putBoolean(KEY_YOUTUBE_ENABLED, config.youtubeAutomationEnabled)
            .putString(KEY_YOUTUBE_API_KEY, config.youtubeApiKey)
            .putInt(KEY_GLOW_THEME, config.glowThemeIndex)
            .apply()
    }

    fun getContacts(): List<Contact> {
        val json = prefs.getString(KEY_CONTACTS_JSON, null)
        if (json.isNullOrEmpty()) {
            val defaults = listOf(
                Contact(name = "Rahul", phone = "+919876543210", relation = "Best Friend"),
                Contact(name = "Aman", phone = "+919876543211", relation = "Friend"),
                Contact(name = "Mom", phone = "+919988776655", relation = "Family"),
                Contact(name = "Rohan", phone = "+919812345678", relation = "Colleague"),
                Contact(name = "Priya", phone = "+919898989898", relation = "Sister")
            )
            saveContacts(defaults)
            return defaults
        }
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Contact>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Contact(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        phone = obj.getString("phone"),
                        relation = obj.optString("relation", "Friend")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveContacts(contacts: List<Contact>) {
        val array = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("relation", c.relation)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CONTACTS_JSON, array.toString()).apply()
    }

    fun getNotes(): List<NoteItem> {
        val json = prefs.getString(KEY_NOTES_JSON, null)
        if (json.isNullOrEmpty()) {
            val defaults = listOf(
                NoteItem(text = "Gym starts at 6 PM", isDone = false),
                NoteItem(text = "Review project deliverables for tomorrow", isDone = false),
                NoteItem(text = "Call Rahul regarding weekend plans", isDone = true)
            )
            saveNotes(defaults)
            return defaults
        }
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<NoteItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NoteItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        text = obj.getString("text"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isDone = obj.optBoolean("isDone", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNotes(notes: List<NoteItem>) {
        val array = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("text", n.text)
            obj.put("timestamp", n.timestamp)
            obj.put("isDone", n.isDone)
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOTES_JSON, array.toString()).apply()
    }
}
