package com.example.ai

import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NovaBrain(
    private val preferencesManager: PreferencesManager,
    private val weatherRepository: WeatherRepository
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class BrainResponse(
        val spokenText: String,
        val actionCard: ActionCard? = null,
        val autoActionIntent: String? = null // e.g. open web url
    )

    suspend fun processQuery(userInput: String): BrainResponse = withContext(Dispatchers.Default) {
        val config = preferencesManager.getConfig()
        val query = userInput.trim()
        val lower = query.lowercase(Locale.ROOT)
        val contacts = preferencesManager.getContacts()

        // 1. YouTube / Music playback
        if (lower.contains("youtube") || lower.contains("gaana") || lower.contains("song") ||
            lower.contains("music") || lower.contains("play") || lower.contains("video bajao") || lower.contains("sunao") && (lower.contains("song") || lower.contains("music"))
        ) {
            val songName = extractSongQuery(query)
            val ytUrl = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(songName, "UTF-8")
            val replies = listOf(
                "Arre wah! Yeh lo mast gaana YouTube pe play kar rahi hu.",
                "Sahi choice hai yaar! Abhi YouTube pe '$songName' chalate hain.",
                "Hmm, music mode on! YouTube pe play kar rahi hu, enjoy karo!"
            )
            return@withContext BrainResponse(
                spokenText = replies.random(),
                actionCard = ActionCard.YouTubePlayerCard(
                    query = songName,
                    videoUrl = ytUrl,
                    title = songName
                ),
                autoActionIntent = ytUrl
            )
        }

        // 2. WhatsApp Message (Confirmation required!)
        if (lower.contains("whatsapp") || lower.contains("message") || lower.contains("msg") || lower.contains("bhejo") || lower.contains("send")) {
            val targetContact = findContactInQuery(query, contacts)
            val messageBody = extractMessageBody(query)

            if (targetContact != null) {
                val confirmPrompt = "Acha suno, ${targetContact.name} ko '$messageBody' bhej du WhatsApp pe?"
                return@withContext BrainResponse(
                    spokenText = confirmPrompt,
                    actionCard = ActionCard.WhatsAppConfirm(
                        contactName = targetContact.name,
                        phoneNumber = targetContact.phone,
                        message = messageBody
                    )
                )
            } else if (lower.contains("aman")) {
                val amanContact = Contact(name = "Aman", phone = "+919876543210")
                return@withContext BrainResponse(
                    spokenText = "Aman ko '$messageBody' WhatsApp pe bhej du?",
                    actionCard = ActionCard.WhatsAppConfirm(
                        contactName = "Aman",
                        phoneNumber = amanContact.phone,
                        message = messageBody
                    )
                )
            } else {
                return@withContext BrainResponse(
                    spokenText = "Kisko message bhejna hai yaar? Aapne contact ka naam nahi bataya. Ek baar naam bata do!",
                    actionCard = null
                )
            }
        }

        // 3. Call Intent (Confirmation required!)
        if (lower.contains("call") || lower.contains("phone milao") || lower.contains("dial") || lower.contains("phone lagao")) {
            val targetContact = findContactInQuery(query, contacts) ?: contacts.firstOrNull { lower.contains(it.name.lowercase()) }
            if (targetContact != null) {
                return@withContext BrainResponse(
                    spokenText = "${targetContact.name} ko call lagau? Ek baar confirm kar do.",
                    actionCard = ActionCard.CallConfirm(
                        contactName = targetContact.name,
                        phoneNumber = targetContact.phone
                    )
                )
            } else {
                return@withContext BrainResponse(
                    spokenText = "Acha, kisko phone lagana hai? Aap saved contacts me se naam bata sakte ho.",
                    actionCard = null
                )
            }
        }

        // 4. Weather Query (Real-time Open-Meteo)
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("temperature") || lower.contains("baarish") || lower.contains("rain")) {
            val city = extractCity(query)
            val weatherResult = weatherRepository.getWeather(city)
            if (weatherResult.isSuccess) {
                val w = weatherResult.getOrThrow()
                val weatherSpeech = "Acha suno! ${w.city} me temperature abhi ${w.tempC.toInt()} degree Celsius hai aur ${w.condition}. Hawa ki raftaar ${w.windSpeed.toInt()} km/h hai."
                return@withContext BrainResponse(
                    spokenText = weatherSpeech,
                    actionCard = ActionCard.WeatherCard(
                        location = w.city,
                        tempC = w.tempC,
                        weatherCode = w.weatherCode,
                        description = w.condition,
                        humidity = w.humidity,
                        windSpeed = w.windSpeed
                    )
                )
            }
        }

        // 5. Alarm and Timers
        if (lower.contains("alarm") || lower.contains("timer") || lower.contains("jaga dena")) {
            val time = extractTime(query)
            val replies = listOf(
                "Done yaar! Maine $time ka alarm set kar diya hai. Mast aaram karo!",
                "Theek hai, $time baje alarm baj jaayega. Late mat hona!",
                "Bilkul! $time ka alarm scheduled hai."
            )
            return@withContext BrainResponse(
                spokenText = replies.random(),
                actionCard = ActionCard.AlarmCard(
                    time = time,
                    label = "NOVA Alarm"
                )
            )
        }

        // 6. Notes & To-Do List
        if (lower.contains("note") || lower.contains("todo") || lower.contains("to-do") || lower.contains("yaad rakhna") || lower.contains("list me add")) {
            val noteContent = query.replace(Regex("(?i)(note|add|todo|to-do|yaad rakhna|list me|karo|likho)"), "").trim()
                .ifEmpty { "Important task from voice note" }

            val currentNotes = preferencesManager.getNotes().toMutableList()
            val newNote = NoteItem(text = noteContent)
            currentNotes.add(0, newNote)
            preferencesManager.saveNotes(currentNotes)

            return@withContext BrainResponse(
                spokenText = "Haanji, note add ho gaya hai: '$noteContent'. Aapki to-do list me safe hai!",
                actionCard = ActionCard.NoteCard(
                    id = newNote.id,
                    text = newNote.text,
                    isCompleted = false
                )
            )
        }

        // 7. Morning Briefing
        if (lower.contains("briefing") || lower.contains("subah") || lower.contains("morning") || lower.contains("aaj ka din") || lower.contains("update")) {
            val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH)
            val dateStr = sdf.format(Date())
            val notes = preferencesManager.getNotes().filter { !it.isDone }
            val noteSummary = if (notes.isNotEmpty()) "Aapke paas ${notes.size} pending tasks hain." else "Aapka schedule aaj bilkul free hai!"

            val briefingText = "Good morning ${config.userName}! Aaj $dateStr hai. Mausam kaafi badiya hai. $noteSummary Main ready hu, batayein kya karna hai?"
            return@withContext BrainResponse(
                spokenText = briefingText,
                actionCard = ActionCard.AlarmCard(
                    time = "Today",
                    label = "Morning Briefing Ready"
                )
            )
        }

        // 8. Shayari, Jokes & Light Humor
        if (lower.contains("shayari") || lower.contains("kavita")) {
            val shayaris = listOf(
                "Arre wah, suniye:\n'Sitaaron se aage jahaan aur bhi hain,\nAbhi ishq ke imtihaan aur bhi hain!'\nKaisi lagi?",
                "Zindagi ki raahon me dosti ka sahara ho,\nDil me khushi aur aankhon me nazara ho!\nNova hamesha aapke saath hai!",
                "Khushbu ki tarah meri har saans me,\nPyaar apna basaane ka vaada karo,\nRang jitne tumhari mohabbat ke hain,\nMere dil me sajaane ka vaada karo!"
            )
            return@withContext BrainResponse(spokenText = shayaris.random())
        }

        if (lower.contains("joke") || lower.contains("chutkula") || lower.contains("hansa do") || lower.contains("funny")) {
            val jokes = listOf(
                "Teacher: 1 se 10 tak ginti sunao.\nPappu: 1, 2, 3, 4, 5, 7, 8, 9, 10.\nTeacher: 6 kahan gaya?\nPappu: Arre ma'am, 6 toh kal news me corona positive nikla tha!",
                "Pappu: Yaar doctor ne subah daudne ko bola tha.\nFriend: Toh dauda kar na!\nPappu: Lekin raste me kutta piche pad gaya, ab pata nahi cardio ho raha hai ya jaan bacha raha hu!",
                "Ek dost doosre se: Bhai tera phone silent pe rehta hai toh sunta kaise hai?\nDoosra: Vibration me dil ke paas rakhta hu, dil ki dhadkan tez ho jati hai!"
            )
            return@withContext BrainResponse(spokenText = jokes.random())
        }

        // 9. Web Search / Open Websites
        if (lower.contains("google") || lower.contains("search") || lower.contains("open") || lower.contains("kholo")) {
            val searchTopic = query.replace(Regex("(?i)(google|search|open|kholo|pe|karo)"), "").trim()
            val url = if (searchTopic.contains(".com") || searchTopic.contains(".org") || searchTopic.contains(".in")) {
                if (searchTopic.startsWith("http")) searchTopic else "https://$searchTopic"
            } else {
                "https://www.google.com/search?q=" + java.net.URLEncoder.encode(searchTopic, "UTF-8")
            }
            return@withContext BrainResponse(
                spokenText = "Theek hai, $searchTopic search kar ke browser khol rahi hu.",
                autoActionIntent = url
            )
        }

        // 10. Math Calculation
        if (lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/") ||
            lower.contains("plus") || lower.contains("minus") || lower.contains("multiply") || lower.contains("divide") || lower.contains("kitna hota hai")
        ) {
            val result = evaluateMath(query)
            if (result != null) {
                return@withContext BrainResponse(
                    spokenText = "Iska answer $result hai yaar. Simple calculation thi!"
                )
            }
        }

        // 11. Custom Gemini API call if key configured
        if (config.apiKey.isNotEmpty()) {
            val geminiReply = callGeminiApi(config.apiKey, query, config.userName, config.assistantName)
            if (geminiReply != null) {
                return@withContext BrainResponse(spokenText = geminiReply)
            }
        }

        // 12. Smart conversational fallback in natural Hinglish
        val conversationalReplies = listOf(
            "Hmm, sun rahi hu ${config.userName}! Kuch mast batao ya koi kaam karna ho toh bolo.",
            "Acha! Main samajh rahi hu. Aap bolo YouTube gaana chalana hai, WhatsApp message bhejna hai ya mausam dekhna hai?",
            "Arre wah! NOVA ready hai. Batayein aage kya plan hai?",
            "Theek hai yaar, aur batao aaj ka din kaisa chal raha hai?",
            "Suno, agar bore ho rahe ho toh bolna, koi badiya shayari ya gaana sunati hu!"
        )
        BrainResponse(spokenText = conversationalReplies.random())
    }

    private fun extractSongQuery(query: String): String {
        return query.replace(Regex("(?i)(youtube|pe|badiya|sa|song|gaana|chalao|bajao|play|karo|sunao|video|music)"), "").trim()
            .ifEmpty { "Arijit Singh Best Songs" }
    }

    private fun findContactInQuery(query: String, contacts: List<Contact>): Contact? {
        val lower = query.lowercase(Locale.ROOT)
        for (c in contacts) {
            if (lower.contains(c.name.lowercase(Locale.ROOT))) {
                return c
            }
        }
        return null
    }

    private fun extractMessageBody(query: String): String {
        val lower = query.lowercase(Locale.ROOT)
        val koIndex = lower.indexOf(" ko ")
        if (koIndex != -1) {
            val afterKo = query.substring(koIndex + 4)
            val cleaned = afterKo.replace(Regex("(?i)(whatsapp|pe|message|msg|bhejo|send|karo|bol do|kah do)"), "").trim()
            if (cleaned.isNotEmpty()) return cleaned
        }
        val clean = query.replace(Regex("(?i)(whatsapp|pe|message|bhejo|send|karo|aman|priya|rohan|mom)"), "").trim()
        return if (clean.isNotEmpty()) clean else "Hi, kaise ho?"
    }

    private fun extractCity(query: String): String {
        val words = query.split(" ")
        val keywords = listOf("weather", "mausam", "ka", "me", "in", "today", "aaj", "kaisa", "hai", "batao")
        val candidate = words.filterNot { keywords.contains(it.lowercase()) }.joinToString(" ").trim()
        return if (candidate.isNotEmpty() && candidate.length > 2) candidate else "Delhi"
    }

    private fun extractTime(query: String): String {
        val match = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|baje)?)", RegexOption.IGNORE_CASE).find(query)
        return match?.value?.trim() ?: "7:00 AM"
    }

    private fun evaluateMath(query: String): String? {
        return try {
            val sanitized = query.replace(Regex("[^0-9+\\-*/.]"), " ").trim()
            val tokens = sanitized.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (tokens.size >= 3) {
                val a = tokens[0].toDouble()
                val op = tokens[1]
                val b = tokens[2].toDouble()
                val ans = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else return "infinite"
                    else -> return null
                }
                if (ans % 1.0 == 0.0) ans.toInt().toString() else "%.2f".format(ans)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun callGeminiApi(
        apiKey: String,
        userQuery: String,
        userName: String,
        assistantName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val systemPrompt = "You are $assistantName, a futuristic JARVIS-style personal voice AI assistant and friend for $userName. Speak in natural, casual Hinglish (Hindi + English mix). Use short, lively sentences and small natural reactions like 'hmm', 'acha', 'arre wah', 'suno yaar', 'theek hai'. Use light humor and match the user's mood. NEVER say 'As an AI' or 'I am an AI model'. Keep your response under 3 sentences for snappy voice delivery."

            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemPrompt\n\nUser said: $userQuery"))
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: return@withContext null
            val root = JSONObject(respBody)
            val candidates = root.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
