package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.view.KeyEvent
import com.example.data.*
import com.example.service.AmancipherAccessibilityService
import com.example.service.DeviceController
import com.example.service.SwipeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NovaBrain(
    private val preferencesManager: PreferencesManager,
    private val weatherRepository: WeatherRepository,
    private val memoryRepository: MemoryRepository,
    private val deviceController: DeviceController
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    data class BrainResponse(
        val spokenText: String,
        val actionCard: ActionCard? = null,
        val autoActionUrl: String? = null,
        val appLaunchQuery: String? = null
    )

    suspend fun processQuery(userInput: String): BrainResponse = withContext(Dispatchers.Default) {
        val config = preferencesManager.getConfig()
        val query = userInput.trim()
        val lower = query.lowercase(Locale.ROOT)
        val contacts = preferencesManager.getContacts()

        // 1. Memory Core: "Remember..." / "Yaad rakhna..."
        if (lower.startsWith("remember") || lower.contains("remember my") || lower.contains("remember that") ||
            lower.contains("yaad rakhna") || lower.contains("save to memory")
        ) {
            val fact = query.replace(
                Regex("(?i)^(remember\\s+that|remember\\s+my|remember|yaad\\s+rakhna\\s+ki|yaad\\s+rakhna|save\\s+to\\s+memory)[:\\s]*"),
                ""
            ).trim().ifEmpty { query }

            val category = when {
                lower.contains("gym") || lower.contains("am") || lower.contains("pm") || lower.contains("routine") || lower.contains("daily") -> "Routine"
                lower.contains("like") || lower.contains("prefer") || lower.contains("favorite") || lower.contains("pasand") -> "Preference"
                else -> "Fact"
            }
            memoryRepository.insert(category, fact)
            return@withContext BrainResponse(
                spokenText = "Got it, ${config.userName}! Maine Memory Core ($category) me save kar liya hai: \"$fact\". Main hamesha yaad rakhunga.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "MEMORY CORE UPDATED",
                    detail = "[$category] $fact",
                    statusText = "Saved to Room Database"
                )
            )
        }

        // 2. Memory Core: "Forget routine" / "Forget..."
        if (lower.startsWith("forget") || lower.contains("forget routine") || lower.contains("bhool jao")) {
            val target = query.replace(Regex("(?i)(forget|bhool jao|routine)"), "").trim()
            val deletedCount = if (target.isEmpty()) {
                memoryRepository.deleteMatching("Routine")
            } else {
                memoryRepository.deleteMatching(target)
            }
            return@withContext BrainResponse(
                spokenText = "Done ${config.userName}! Memory Core se ${if (deletedCount > 0) "$deletedCount record" else "routine"} clear kar diya gaya hai.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "MEMORY PURGED",
                    detail = "Cleared matching memory entries",
                    statusText = "Room DB Synced"
                )
            )
        }

        // 3. Accessibility Service: Screen Reading & UI Gestures
        if (lower.contains("inspect screen") || lower.contains("what is on my screen") ||
            lower.contains("screen par kya") || lower.contains("read screen")
        ) {
            val a11y = AmancipherAccessibilityService.instance
            if (a11y != null) {
                val elements = a11y.inspectScreen()
                val pkg = AmancipherAccessibilityService.lastActivePackage.value
                val labels = elements.map { el ->
                    if (el.isClickable) "${el.text} [Button @ ${el.centerX},${el.centerY}]" else el.text
                }.filter { it.isNotBlank() }.distinct().take(10)
                val summarySpeech = if (labels.isNotEmpty()) {
                    "Screen content read complete! Active window me ${elements.size} UI elements mile hain, jaise: ${elements.take(4).joinToString(", ") { it.text }}."
                } else {
                    "Accessibility service connected hai, par active window me koi readable text node nahi mila."
                }
                return@withContext BrainResponse(
                    spokenText = summarySpeech,
                    actionCard = ActionCard.ScreenInspectCard(
                        activePackage = pkg,
                        elementCount = elements.size,
                        summary = labels.ifEmpty { listOf("Active Window Monitored") }
                    )
                )
            } else {
                return@withContext BrainResponse(
                    spokenText = "Screen content read karne ke liye Settings me jaakar Amancipher.Ai Accessibility Service enable kar dijiye, ${config.userName}.",
                    actionCard = ActionCard.DeviceActionCard(
                        actionTitle = "ACCESSIBILITY SERVICE",
                        detail = "Enable Accessibility Service in Settings -> Permissions to read screen content & perform UI gestures.",
                        statusText = "Permission Needed"
                    )
                )
            }
        }

        if (lower.startsWith("double tap") || lower.contains("double click")) {
            val a11y = AmancipherAccessibilityService.instance
            val coords = Regex("(\\d+)\\s*,?\\s*(\\d+)").find(lower)
            val x = coords?.groupValues?.get(1)?.toFloatOrNull() ?: 540f
            val y = coords?.groupValues?.get(2)?.toFloatOrNull() ?: 960f
            val ok = a11y?.performDoubleTap(x, y) ?: false
            return@withContext BrainResponse(
                spokenText = if (ok) "Screen par (${x.toInt()}, ${y.toInt()}) coordinates par double-tap gesture perform kar diya hai." else "Double-tap gesture ke liye Accessibility Service active honi chahiye.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "UI GESTURE // DOUBLE TAP",
                    detail = "Coordinates: (${x.toInt()}, ${y.toInt()})",
                    statusText = if (ok) "Dispatched" else "Service Inactive"
                )
            )
        }

        if (lower.startsWith("long press") || lower.startsWith("long click") || lower.contains("hold on ")) {
            val a11y = AmancipherAccessibilityService.instance
            val coords = Regex("(\\d+)\\s*,?\\s*(\\d+)").find(lower)
            if (coords != null) {
                val x = coords.groupValues[1].toFloat()
                val y = coords.groupValues[2].toFloat()
                val ok = a11y?.performLongPress(x, y) ?: false
                return@withContext BrainResponse(
                    spokenText = if (ok) "Coordinates (${x.toInt()}, ${y.toInt()}) par long-press kar diya hai." else "Long-press gesture ke liye Accessibility Service on karein.",
                    actionCard = ActionCard.DeviceActionCard(
                        actionTitle = "UI GESTURE // LONG PRESS",
                        detail = "Coordinates: (${x.toInt()}, ${y.toInt()})",
                        statusText = if (ok) "Dispatched" else "Service Inactive"
                    )
                )
            } else {
                val target = query.replace(Regex("(?i)(long press on|long press|long click on|long click|hold on)"), "").trim()
                val ok = a11y?.longClickByText(target) ?: false
                return@withContext BrainResponse(
                    spokenText = if (ok) "'$target' par long-press kar diya hai." else "Screen par '$target' element nahi mila.",
                    actionCard = ActionCard.DeviceActionCard(
                        actionTitle = "UI GESTURE // LONG PRESS",
                        detail = "Target: \"$target\"",
                        statusText = if (ok) "Long-Pressed" else "Not Found"
                    )
                )
            }
        }

        if (lower.startsWith("click ") || lower.startsWith("tap ") || lower.contains(" par click karo")) {
            val a11y = AmancipherAccessibilityService.instance
            val coords = Regex("(\\d+)\\s*,?\\s*(\\d+)").find(lower)
            if (coords != null && (lower.contains("at ") || lower.startsWith("tap "))) {
                val x = coords.groupValues[1].toFloat()
                val y = coords.groupValues[2].toFloat()
                val ok = a11y?.performTap(x, y) ?: false
                return@withContext BrainResponse(
                    spokenText = if (ok) "Screen coordinates (${x.toInt()}, ${y.toInt()}) par tap gesture perform kar diya." else "Tap gesture ke liye Accessibility Service enable karein.",
                    actionCard = ActionCard.DeviceActionCard(
                        actionTitle = "UI GESTURE // COORDINATE TAP",
                        detail = "Tapped at (${x.toInt()}, ${y.toInt()})",
                        statusText = if (ok) "Dispatched" else "Service Inactive"
                    )
                )
            }

            val target = query.replace(Regex("(?i)(click on|click|tap on|tap|par click karo|button)"), "").trim()
            if (a11y != null && target.isNotEmpty()) {
                val clicked = a11y.clickByText(target)
                return@withContext BrainResponse(
                    spokenText = if (clicked) "Done! '$target' element par click kar diya." else "Screen par '$target' naam ka clickable element nahi mila.",
                    actionCard = ActionCard.DeviceActionCard(
                        actionTitle = "ACCESSIBILITY CLICK",
                        detail = "Target: \"$target\"",
                        statusText = if (clicked) "Clicked Successfully" else "Element Not Found"
                    )
                )
            }
        }

        if (lower.startsWith("type ") || lower.contains("likho field me")) {
            val textToType = query.replace(Regex("(?i)^(type|write|likho field me)\\s+"), "").trim()
            val a11y = AmancipherAccessibilityService.instance
            if (a11y != null && textToType.isNotEmpty()) {
                val typed = a11y.inputText(textToType)
                return@withContext BrainResponse(
                    spokenText = if (typed) "Focused input field me '$textToType' type kar diya hai." else "Koi active editable text field nahi mili.",
                    actionCard = ActionCard.DeviceActionCard(
                        actionTitle = "ACCESSIBILITY TYPE",
                        detail = "Text: \"$textToType\"",
                        statusText = if (typed) "Typed Into Field" else "No Focused Input"
                    )
                )
            }
        }

        if (lower.contains("swipe ") || lower.contains("scroll down") || lower.contains("scroll up") ||
            lower.contains("niche scroll") || lower.contains("upar scroll")
        ) {
            val a11y = AmancipherAccessibilityService.instance
            val direction = when {
                lower.contains("left") || lower.contains("baaye") -> SwipeDirection.LEFT
                lower.contains("right") || lower.contains("daaye") -> SwipeDirection.RIGHT
                lower.contains("swipe down") || lower.contains("scroll up") || lower.contains("upar") -> SwipeDirection.DOWN
                else -> SwipeDirection.UP
            }
            val executed = if (lower.contains("scroll")) {
                a11y?.scrollScreen(forward = direction == SwipeDirection.UP) ?: false
            } else {
                a11y?.performSwipe(direction) ?: false
            }
            return@withContext BrainResponse(
                spokenText = if (executed) "UI gesture ${direction.name} execute kar diya gaya hai." else "Gesture execute karne ke liye Accessibility Service active karein.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "UI GESTURE // SWIPE ${direction.name}",
                    detail = "Direction: ${direction.name}",
                    statusText = if (executed) "Gesture Dispatched" else "Simulated / Ready"
                )
            )
        }

        if (lower.contains("zoom in") || lower.contains("zoom out") || lower.contains("pinch")) {
            val zoomIn = !lower.contains("out")
            val ok = AmancipherAccessibilityService.instance?.performPinchZoom(zoomIn) ?: false
            return@withContext BrainResponse(
                spokenText = if (ok) "Pinch ${if (zoomIn) "zoom in" else "zoom out"} gesture perform kar diya hai." else "Pinch zoom ke liye Accessibility Service active karein.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "UI GESTURE // PINCH ZOOM",
                    detail = if (zoomIn) "Mode: Zoom In (2-finger spread)" else "Mode: Zoom Out (2-finger pinch)",
                    statusText = if (ok) "Dispatched" else "Service Inactive"
                )
            )
        }

        if (lower == "go back" || lower == "navigate back" || lower == "piche jao" ||
            lower == "go home" || lower == "home screen" || lower == "open recents" ||
            lower == "open notifications" || lower == "open quick settings" || lower.contains("take screenshot")
        ) {
            val a11y = AmancipherAccessibilityService.instance
            val (label, ok) = when {
                lower.contains("home") -> "Navigate Home" to (a11y?.navigateHome() ?: false)
                lower.contains("recent") -> "Open Recents" to (a11y?.openRecents() ?: false)
                lower.contains("notification") -> "Open Notifications" to (a11y?.openNotifications() ?: false)
                lower.contains("quick setting") -> "Open Quick Settings" to (a11y?.openQuickSettings() ?: false)
                lower.contains("screenshot") -> "Take Screenshot" to (a11y?.takeScreenSnapshot() ?: false)
                else -> "Navigate Back" to (a11y?.navigateBack() ?: false)
            }
            return@withContext BrainResponse(
                spokenText = if (ok) "$label action execute kar diya hai." else "$label ke liye Accessibility Service on honi chahiye.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "GLOBAL SYSTEM ACTION",
                    detail = label,
                    statusText = if (ok) "Executed" else "Service Inactive"
                )
            )
        }

        // 4. Mobile Hardware Control: Flashlight
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            val ok = deviceController.setFlashlight(turnOn)
            val stateWord = if (turnOn) "ON" else "OFF"
            return@withContext BrainResponse(
                spokenText = "Flashlight $stateWord kar di hai, ${config.userName}!",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "FLASHLIGHT CONTROL",
                    detail = "Torch State: $stateWord",
                    statusText = if (ok) "Hardware Torch $stateWord" else "Torch $stateWord"
                )
            )
        }

        // 5. Mobile Hardware Control: Brightness
        if (lower.contains("brightness") || lower.contains("roshni")) {
            val numberMatch = Regex("(\\d{1,3})").find(lower)
            val newBrightness = when {
                numberMatch != null -> deviceController.setBrightnessPercent(numberMatch.value.toInt())
                lower.contains("increase") || lower.contains("up") || lower.contains("badhao") -> deviceController.adjustBrightness(true)
                lower.contains("decrease") || lower.contains("down") || lower.contains("kam") -> deviceController.adjustBrightness(false)
                else -> deviceController.setBrightnessPercent(60)
            }
            return@withContext BrainResponse(
                spokenText = "Screen brightness $newBrightness percent par set kar di hai.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "DISPLAY BRIGHTNESS",
                    detail = "Brightness Level: $newBrightness%",
                    statusText = "Applied to Display"
                )
            )
        }

        // 6. Mobile Hardware Control: Volume & Media Playback
        if (lower.contains("volume") || lower.contains("awaaz")) {
            val numberMatch = Regex("(\\d{1,3})").find(lower)
            val newVol = when {
                numberMatch != null -> deviceController.setVolumePercent(numberMatch.value.toInt())
                lower.contains("up") || lower.contains("increase") || lower.contains("badhao") -> deviceController.adjustVolume(true)
                lower.contains("down") || lower.contains("decrease") || lower.contains("kam") -> deviceController.adjustVolume(false)
                else -> deviceController.setVolumePercent(75)
            }
            return@withContext BrainResponse(
                spokenText = "Media volume $newVol percent par set kar diya hai, सर.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "AUDIO STREAM VOLUME",
                    detail = "Music Volume: $newVol%",
                    statusText = "AudioManager Updated"
                )
            )
        }

        if (lower.contains("pause music") || lower.contains("resume music") || lower.contains("next song") || lower.contains("next track") || lower.contains("previous track")) {
            val keyCode = when {
                lower.contains("next") -> KeyEvent.KEYCODE_MEDIA_NEXT
                lower.contains("prev") -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }
            deviceController.dispatchMediaKey(keyCode)
            return@withContext BrainResponse(
                spokenText = "Media playback command dispatch kar diya gaya hai.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "MEDIA TRANSPORT CONTROL",
                    detail = query,
                    statusText = "Dispatched to Active Player"
                )
            )
        }

        // 7. Mobile App Launchers: "Open Instagram", "Open WhatsApp", "Open Spotify"
        if ((lower.startsWith("open ") || lower.contains("kholo")) &&
            (lower.contains("instagram") || lower.contains("whatsapp") || lower.contains("spotify") || lower.contains("settings"))
        ) {
            val appName = when {
                lower.contains("instagram") -> "Instagram"
                lower.contains("whatsapp") -> "WhatsApp"
                lower.contains("spotify") -> "Spotify"
                else -> "Settings"
            }
            return@withContext BrainResponse(
                spokenText = "Abhi $appName open kar raha hu, ${config.userName}.",
                actionCard = ActionCard.DeviceActionCard(
                    actionTitle = "APP LAUNCHER",
                    detail = "Launching $appName",
                    statusText = "Intent Dispatched"
                ),
                appLaunchQuery = appName
            )
        }

        // 8. WhatsApp Call / Video Call (Confirmation Policy Active)
        if (lower.contains("video call") || (lower.contains("call") && lower.contains("whatsapp"))) {
            val targetContact = findContactInQuery(query, contacts)
                ?: Contact(name = extractPersonName(query).ifEmpty { "Rahul" }, phone = "+919876543210")
            val isVideo = lower.contains("video")
            val callType = if (isVideo) "WhatsApp Video Call" else "WhatsApp Call"
            return@withContext BrainResponse(
                spokenText = "${targetContact.name} ko $callType lagana hai? WhatsApp Confirmation Policy active hai—please 'Haan' bolkar ya button tap karke confirm karein.",
                actionCard = ActionCard.CallConfirm(
                    contactName = targetContact.name,
                    phoneNumber = targetContact.phone,
                    viaWhatsApp = true
                )
            )
        }

        // 9. WhatsApp Message (Confirmation Policy Active)
        if (lower.contains("whatsapp") || lower.contains("message") || lower.contains("msg") || lower.contains("bhejo") || lower.contains("saying")) {
            val targetContact = findContactInQuery(query, contacts)
                ?: Contact(name = extractPersonName(query).ifEmpty { "Rahul" }, phone = "+919876543210")
            val messageBody = extractMessageBody(query)

            val confirmPrompt = "WhatsApp Confirmation Policy: Kya main ${targetContact.name} ko yeh message bhej du — \"$messageBody\"? Confirm karne ke liye 'Yes' ya 'Haan' bolein."
            return@withContext BrainResponse(
                spokenText = confirmPrompt,
                actionCard = ActionCard.WhatsAppConfirm(
                    contactName = targetContact.name,
                    phoneNumber = targetContact.phone,
                    message = messageBody
                )
            )
        }

        // 10. Standard Voice Call (Confirmation Policy Active)
        if (lower.contains("call") || lower.contains("phone milao") || lower.contains("dial") || lower.contains("phone lagao")) {
            val targetContact = findContactInQuery(query, contacts)
                ?: Contact(name = extractPersonName(query).ifEmpty { "Rahul" }, phone = "+919876543210")
            return@withContext BrainResponse(
                spokenText = "${targetContact.name} ko phone call lagau? Ek baar confirm kar dijiye.",
                actionCard = ActionCard.CallConfirm(
                    contactName = targetContact.name,
                    phoneNumber = targetContact.phone,
                    viaWhatsApp = false
                )
            )
        }

        // 11. YouTube Search & Automation (Data API v3 + Direct Fallback)
        if (lower.contains("youtube") || lower.contains("gaana") || lower.contains("song") ||
            lower.contains("music") || lower.contains("play") || lower.contains("bajao")
        ) {
            val songName = extractSongQuery(query)
            var ytUrl = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(songName, "UTF-8")
            var resolvedTitle = songName
            var resolvedVideoId = ""

            if (config.youtubeAutomationEnabled && config.youtubeApiKey.isNotBlank()) {
                val apiResult = searchYouTubeDataApi(songName, config.youtubeApiKey)
                if (apiResult != null) {
                    resolvedVideoId = apiResult.first
                    resolvedTitle = apiResult.second
                    ytUrl = "https://www.youtube.com/watch?v=$resolvedVideoId"
                }
            }

            val reply = "Arre wah ${config.userName}! YouTube par '$resolvedTitle' search karke play kar raha hu."
            return@withContext BrainResponse(
                spokenText = reply,
                actionCard = ActionCard.YouTubePlayerCard(
                    query = songName,
                    videoUrl = ytUrl,
                    title = resolvedTitle,
                    videoId = resolvedVideoId
                ),
                autoActionUrl = ytUrl
            )
        }

        // 12. Weather Query (Open-Meteo)
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("temperature") || lower.contains("baarish")) {
            val city = extractCity(query)
            val weatherResult = weatherRepository.getWeather(city)
            if (weatherResult.isSuccess) {
                val w = weatherResult.getOrThrow()
                val weatherSpeech = "${w.city} me abhi temperature ${w.tempC.toInt()}°C hai aur ${w.condition}. Humidity ${w.humidity}% aur wind speed ${w.windSpeed.toInt()} km/h hai."
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

        // 13. Alarms & Timers
        if (lower.contains("alarm") || lower.contains("timer") || lower.contains("jaga dena")) {
            val time = extractTime(query)
            return@withContext BrainResponse(
                spokenText = "Done ${config.userName}! Maine $time ka alarm set kar diya hai.",
                actionCard = ActionCard.AlarmCard(
                    time = time,
                    label = "${config.assistantName} Alarm"
                )
            )
        }

        // 14. Notes & To-Do List
        if (lower.contains("note") || lower.contains("todo") || lower.contains("to-do") || lower.contains("list me add")) {
            val noteContent = query.replace(Regex("(?i)(note|add|todo|to-do|list me|karo|likho)"), "").trim()
                .ifEmpty { "Important task from voice note" }

            val currentNotes = preferencesManager.getNotes().toMutableList()
            val newNote = NoteItem(text = noteContent)
            currentNotes.add(0, newNote)
            preferencesManager.saveNotes(currentNotes)
            memoryRepository.insert("Note", noteContent)

            return@withContext BrainResponse(
                spokenText = "Note aur Memory Core dono me add ho gaya hai: '$noteContent'.",
                actionCard = ActionCard.NoteCard(
                    id = newNote.id,
                    text = newNote.text,
                    isCompleted = false
                )
            )
        }

        // 15. Web Search
        if (lower.contains("google") || lower.contains("search") || (lower.contains("open") && lower.contains(".com"))) {
            val searchTopic = query.replace(Regex("(?i)(google|search|open|kholo|pe|karo)"), "").trim()
            val url = if (searchTopic.contains(".com") || searchTopic.contains(".org") || searchTopic.contains(".in")) {
                if (searchTopic.startsWith("http")) searchTopic else "https://$searchTopic"
            } else {
                "https://www.google.com/search?q=" + java.net.URLEncoder.encode(searchTopic, "UTF-8")
            }
            return@withContext BrainResponse(
                spokenText = "Web par '$searchTopic' search karke open kar raha hu.",
                autoActionUrl = url
            )
        }

        // 16. Math Calculation
        if (lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/") ||
            lower.contains("plus") || lower.contains("minus") || lower.contains("multiply") || lower.contains("divide")
        ) {
            val result = evaluateMath(query)
            if (result != null) {
                return@withContext BrainResponse(
                    spokenText = "Iska exact calculation result $result hai, ${config.userName}."
                )
            }
        }

        // 17. Gemini API with Memory Core & Persona Context
        val memories = memoryRepository.getAllList().take(10)
        if (config.apiKey.isNotEmpty()) {
            val geminiReply = callGeminiApi(
                apiKey = config.apiKey,
                modelName = config.aiModel,
                userQuery = query,
                config = config,
                memories = memories
            )
            if (geminiReply != null) {
                return@withContext BrainResponse(spokenText = geminiReply)
            }
        }

        // 18. Witty, Helpful Persona Fallback (with Hindi/Devanagari & Hinglish awareness)
        val hasDevanagari = query.any { it in '\u0900'..'\u097F' } || lower.contains("हिंदी") || lower.contains("hindi")
        if (hasDevanagari) {
            return@withContext BrainResponse(
                spokenText = "सर, मैं ${config.assistantName} हूँ—आपका एडवांस्ड AI असिस्टेंट। मैं आपकी स्क्रीन इंस्पेक्ट कर सकता हूँ, फ्लैशलाइट, वॉल्यूम और ब्राइटनेस कंट्रोल कर सकता हूँ, और WhatsApp या YouTube भी चला सकता हूँ। बताइए अभी क्या करना है?"
            )
        }

        val memoryHint = if (memories.isNotEmpty()) {
            " Mujhe yaad hai: ${memories.first().content}."
        } else ""

        val wittyReplies = listOf(
            "All systems online, ${config.userName}!$memoryHint Bolo kya karna hai — flashlight on karu, WhatsApp bheju, ya YouTube pe koi badiya track chalau?",
            "Main sun raha hu ${config.userName}! Screen inspect karni ho, volume/brightness set karna ho, ya Rahul ko message bhejna ho, bas hukm karo.",
            "Amancipher.Ai ready for action!$memoryHint Aaj ka kya plan hai yaar?"
        )
        BrainResponse(spokenText = wittyReplies.random())
    }

    suspend fun analyzeVisionImage(bitmap: Bitmap?, prompt: String): String = withContext(Dispatchers.IO) {
        val config = preferencesManager.getConfig()
        val a11yElements = AmancipherAccessibilityService.lastInspectedElements.value
        val screenContext = if (a11yElements.isNotEmpty()) {
            "Active screen nodes: " + a11yElements.take(8).joinToString(", ") { it.text }
        } else {
            "Screen monitor standby."
        }

        if (bitmap != null && config.apiKey.isNotBlank()) {
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${config.apiKey}"
                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "You are ${config.assistantName}, a witty cybernetic AI assistant for ${config.userName}. Analyze this image and answer concisely in Hinglish/English: $prompt. ($screenContext)"))
                                put(JSONObject().apply {
                                    put("inline_data", JSONObject().apply {
                                        put("mime_type", "image/jpeg")
                                        put("data", base64Image)
                                    })
                                })
                            })
                        })
                    })
                }
                val request = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val root = JSONObject(body)
                    val text = root.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) return@withContext text.trim()
                }
            } catch (_: Exception) {}
        }

        if (bitmap != null) {
            val w = bitmap.width
            val h = bitmap.height
            val centerPixel = bitmap.getPixel(w / 2, h / 2)
            val r = (centerPixel shr 16) and 0xFF
            val g = (centerPixel shr 8) and 0xFF
            val b = centerPixel and 0xFF
            val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            val lighting = when {
                luma > 180 -> "brightly lit"
                luma < 60 -> "low-light / dark"
                else -> "well-balanced indoor lighting"
            }
            return@withContext "Vision scan complete (${w}x${h}px, $lighting, RGB [$r,$g,$b]). $screenContext Add your Gemini API key in Settings for deep neural object recognition!"
        }

        return@withContext "Screen Vision Inspection: $screenContext"
    }

    suspend fun validateYouTubeApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        try {
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=music&type=video&maxResults=1&key=${apiKey.trim()}"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun searchYouTubeDataApi(query: String, apiKey: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encoded&type=video&maxResults=1&key=${apiKey.trim()}"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val items = json.optJSONArray("items")
            if (items != null && items.length() > 0) {
                val first = items.getJSONObject(0)
                val videoId = first.getJSONObject("id").getString("videoId")
                val title = first.getJSONObject("snippet").getString("title")
                return@withContext videoId to title
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractSongQuery(query: String): String {
        return query.replace(Regex("(?i)(youtube|pe|par|badiya|sa|song|gaana|chalao|bajao|play|karo|sunao|video|music|on)"), "").trim()
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

    private fun extractPersonName(query: String): String {
        val toMatch = Regex("(?i)(?:to|call)\\s+([A-Za-z]+)").find(query)
        if (toMatch != null) return toMatch.groupValues[1]
        val koMatch = Regex("(?i)([A-Za-z]+)\\s+ko").find(query)
        if (koMatch != null) return koMatch.groupValues[1]
        return "Rahul"
    }

    private fun extractMessageBody(query: String): String {
        val sayingIndex = query.lowercase().indexOf("saying ")
        if (sayingIndex != -1) {
            return query.substring(sayingIndex + 7).trim().ifEmpty { "Hi, I will be there soon!" }
        }
        val koIndex = query.lowercase().indexOf(" ko ")
        if (koIndex != -1) {
            val afterKo = query.substring(koIndex + 4)
            val cleaned = afterKo.replace(Regex("(?i)(whatsapp|pe|message|msg|bhejo|send|karo|bol do|kah do)"), "").trim()
            if (cleaned.isNotEmpty()) return cleaned
        }
        return "I will be late"
    }

    private fun extractCity(query: String): String {
        val words = query.split(" ")
        val keywords = listOf("weather", "mausam", "ka", "me", "in", "today", "aaj", "kaisa", "hai", "batao")
        val candidate = words.filterNot { keywords.contains(it.lowercase()) }.joinToString(" ").trim()
        return if (candidate.isNotEmpty() && candidate.length > 2) candidate else "Delhi"
    }

    private fun extractTime(query: String): String {
        val match = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|baje)?)", RegexOption.IGNORE_CASE).find(query)
        return match?.value?.trim() ?: "6:00 PM"
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
        modelName: String,
        userQuery: String,
        config: NovaConfig,
        memories: List<MemoryEntity>
    ): String? = withContext(Dispatchers.IO) {
        try {
            val resolvedModel = when {
                modelName.contains("2.5 Pro", ignoreCase = true) -> "gemini-1.5-pro"
                else -> "gemini-1.5-flash"
            }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent?key=$apiKey"

            val memoryText = if (memories.isNotEmpty()) {
                "Long-term Memory Core facts about user: " + memories.joinToString("; ") { "[${it.category}] ${it.content}" }
            } else {
                "No stored memories yet."
            }

            val traits = buildList {
                if (config.traitFriendly) add("Friendly")
                if (config.traitHelpful) add("Helpful")
                add("Witty")
                if (config.traitHinglishOrEnglish) add("Fluent in conversational Hinglish, Hindi, and English")
            }.joinToString(", ")

            val systemPrompt = """
                You are ${config.assistantName}, an advanced AI assistant capable of controlling the Android device using accessibility services, hardware controls (flashlight, brightness, volume), WhatsApp messaging/calls, YouTube automation, and web searches.
                User Name: ${config.userName}
                Mode: ${config.personalityMode}
                Personality Traits: $traits
                $memoryText
                Match the user's language (if the user speaks Hindi in Devanagari, reply in Hindi Devanagari; if Hinglish or English, reply in witty Hinglish/English). Keep responses concise (2-3 sentences) for voice synthesis. Never say 'As an AI'.
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemPrompt\n\nUser: $userQuery"))
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
