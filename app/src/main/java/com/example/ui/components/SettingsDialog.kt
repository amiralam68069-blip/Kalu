package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.service.AmancipherAccessibilityService
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    config: NovaConfig,
    contacts: List<Contact>,
    youtubeKeyValidationStatus: String = "",
    onValidateYouTubeKey: (String) -> Unit = {},
    onClearMemories: () -> Unit = {},
    onViewMemories: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onExecuteQuickCommand: (String) -> Unit = {},
    onSaveConfig: (NovaConfig) -> Unit,
    onAddContact: (String, String, String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var assistantName by remember { mutableStateOf(config.assistantName) }
    var userName by remember { mutableStateOf(config.userName) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    var aiModel by remember { mutableStateOf(config.aiModel) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var selectedVoice by remember { mutableStateOf(config.selectedVoice) }

    var personalityMode by remember { mutableStateOf(config.personalityMode) }
    var modeDropdownExpanded by remember { mutableStateOf(false) }
    var traitFriendly by remember { mutableStateOf(config.traitFriendly) }
    var traitHelpful by remember { mutableStateOf(config.traitHelpful) }
    var traitHinglish by remember { mutableStateOf(config.traitHinglishOrEnglish) }

    var youtubeEnabled by remember { mutableStateOf(config.youtubeAutomationEnabled) }
    var youtubeApiKey by remember { mutableStateOf(config.youtubeApiKey) }
    var showYoutubeKey by remember { mutableStateOf(false) }

    var managePermissionsExpanded by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }

    val isA11yConnected by AmancipherAccessibilityService.isConnected.collectAsState()

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        micGranted = results[Manifest.permission.RECORD_AUDIO] ?: micGranted
        cameraGranted = results[Manifest.permission.CAMERA] ?: cameraGranted
    }

    val grantedCount = 4 + (if (micGranted) 1 else 0) + (if (cameraGranted) 1 else 0)

    fun saveAndClose() {
        onSaveConfig(
            config.copy(
                assistantName = assistantName.ifBlank { "Amancipher.Ai" },
                userName = userName.ifBlank { "Amancipher" },
                apiKey = apiKey.trim(),
                aiModel = aiModel,
                selectedVoice = selectedVoice,
                personalityMode = personalityMode,
                traitFriendly = traitFriendly,
                traitHelpful = traitHelpful,
                traitHinglishOrEnglish = traitHinglish,
                youtubeAutomationEnabled = youtubeEnabled,
                youtubeApiKey = youtubeApiKey.trim()
            )
        )
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("settings_dialog")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Header Bar matching Screenshots 1, 2, 3
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050B0F))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "S e t t i n g s",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "SAVE",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .clickable { saveAndClose() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("save_settings_button")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CyberCardBorder)
            )

            // Scrollable Settings Sections
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 1. API KEY SECTION
                item {
                    SectionHeader(icon = Icons.Outlined.VpnKey, title = "API Key")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        Text(
                            text = "GEMINI API KEY",
                            color = TextMuted,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = { Text("Paste Gemini API Key...", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input"),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF060E13),
                                unfocusedContainerColor = Color(0xFF060E13),
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberFieldBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste from clipboard",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    clipboardManager.getText()?.text?.let { apiKey = it.trim() }
                                }
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (apiKey.isNotBlank()) EmeraldGlow else EmeraldGlow,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (apiKey.isNotBlank()) "Key Configured" else "Key Configured (Built-in Ready)",
                                    color = EmeraldGlow,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 2. AI MODEL SECTION
                item {
                    SectionHeader(icon = Icons.Outlined.SmartToy, title = "AI Model")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF060E13))
                                    .border(1.dp, CyberFieldBorder, RoundedCornerShape(12.dp))
                                    .clickable { modelDropdownExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 15.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = aiModel,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Model",
                                    tint = NeonCyan
                                )
                            }

                            DropdownMenu(
                                expanded = modelDropdownExpanded,
                                onDismissRequest = { modelDropdownExpanded = false },
                                modifier = Modifier.background(CyberCardSurface)
                            ) {
                                AVAILABLE_AI_MODELS.forEach { modelOption ->
                                    DropdownMenuItem(
                                        text = { Text(modelOption, color = TextPrimary, fontSize = 13.sp) },
                                        onClick = {
                                            aiModel = modelOption
                                            modelDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. VOICE SECTION (2x4 Grid matching Screenshot 1)
                item {
                    SectionHeader(icon = Icons.Outlined.MicNone, title = "Voice")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        val row1 = AVAILABLE_VOICES.take(4)
                        val row2 = AVAILABLE_VOICES.drop(4).take(4)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row1.forEach { voice ->
                                VoicePill(
                                    voice = voice,
                                    isSelected = selectedVoice == voice,
                                    onClick = { selectedVoice = voice },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row2.forEach { voice ->
                                VoicePill(
                                    voice = voice,
                                    isSelected = selectedVoice == voice,
                                    onClick = { selectedVoice = voice },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 4. PERSONALITY SECTION (matching Screenshot 1 & 3)
                item {
                    SectionHeader(icon = Icons.Outlined.PersonOutline, title = "Personality")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        // Assistant Mode Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF060E13))
                                    .border(1.dp, CyberFieldBorder, RoundedCornerShape(12.dp))
                                    .clickable { modeDropdownExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = personalityMode, color = TextPrimary, fontSize = 15.sp)
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Mode",
                                    tint = NeonCyan
                                )
                            }
                            DropdownMenu(
                                expanded = modeDropdownExpanded,
                                onDismissRequest = { modeDropdownExpanded = false },
                                modifier = Modifier.background(CyberCardSurface)
                            ) {
                                AVAILABLE_PERSONALITY_MODES.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode, color = TextPrimary) },
                                        onClick = {
                                            personalityMode = mode
                                            modeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Trait Chips: Friendly, Helpful, Hinglish or English
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TraitChip(
                                label = "Friendly",
                                isActive = traitFriendly,
                                onClick = { traitFriendly = !traitFriendly }
                            )
                            TraitChip(
                                label = "Helpful",
                                isActive = traitHelpful,
                                onClick = { traitHelpful = !traitHelpful }
                            )
                            TraitChip(
                                label = "Hinglish or English",
                                isActive = traitHinglish,
                                onClick = { traitHinglish = !traitHinglish }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "USER NAME",
                            color = TextMuted,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF060E13),
                                unfocusedContainerColor = Color(0xFF060E13),
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberFieldBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ASSISTANT NAME",
                            color = TextMuted,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = assistantName,
                            onValueChange = { assistantName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("assistant_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF060E13),
                                unfocusedContainerColor = Color(0xFF060E13),
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberFieldBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                // 5. PERMISSIONS SECTION (matching Screenshot 3)
                item {
                    SectionHeader(icon = Icons.Outlined.Lock, title = "Permissions")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                                        append("$grantedCount/6 ")
                                    }
                                    withStyle(SpanStyle(color = TextMuted)) {
                                        append("granted")
                                    }
                                },
                                fontSize = 15.sp
                            )

                            Button(
                                onClick = {
                                    multiPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECORD_AUDIO,
                                            Manifest.permission.CAMERA
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5CE1C6)),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "ALLOW ALL",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CyberCardBorder)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { managePermissionsExpanded = !managePermissionsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Manage Permissions & Contacts",
                                color = NeonCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (managePermissionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = NeonCyan
                            )
                        }

                        AnimatedVisibility(visible = managePermissionsExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PermissionRowItem(
                                    name = "Accessibility Service (Screen Control)",
                                    granted = isA11yConnected,
                                    actionLabel = if (isA11yConnected) "Connected" else "Enable",
                                    onClick = onOpenAccessibilitySettings
                                )
                                PermissionRowItem(
                                    name = "Microphone (Voice Recognition)",
                                    granted = micGranted,
                                    actionLabel = if (micGranted) "Granted" else "Grant",
                                    onClick = {
                                        multiPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    }
                                )
                                PermissionRowItem(
                                    name = "Camera & Torch (Vision & Flashlight)",
                                    granted = cameraGranted,
                                    actionLabel = if (cameraGranted) "Granted" else "Grant",
                                    onClick = {
                                        multiPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                    }
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "SAVED WHATSAPP CONTACTS (${contacts.size})",
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = newContactName,
                                        onValueChange = { newContactName = it },
                                        placeholder = { Text("Name", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = newContactPhone,
                                        onValueChange = { newContactPhone = it },
                                        placeholder = { Text("+91...", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    IconButton(
                                        onClick = {
                                            if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                                                onAddContact(newContactName.trim(), newContactPhone.trim(), "Friend")
                                                newContactName = ""
                                                newContactPhone = ""
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add", tint = NeonCyan)
                                    }
                                }
                                contacts.forEach { c ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${c.name} (${c.phone})", color = TextIce, fontSize = 12.sp)
                                        IconButton(onClick = { onDeleteContact(c.id) }, modifier = Modifier.size(26.dp)) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = CrimsonText, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. MEMORY CORE SECTION (matching Screenshot 3)
                item {
                    SectionHeader(icon = Icons.Outlined.Description, title = "Memory Core")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        Text(
                            text = "Short-term conversation buffer + Room Database long-term knowledge (Routines, Preferences, Notes, Facts).",
                            color = TextMuted,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClearMemories,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CrimsonBorder),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF140B0E))
                            ) {
                                Text(
                                    text = "CLEAR",
                                    color = CrimsonText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onViewMemories()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF1D4D5C)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF081B23))
                            ) {
                                Text(
                                    text = "VIEW MEMORIES",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // 7. YOUTUBE AUTOMATION SECTION (matching Screenshot 2 & 3)
                item {
                    SectionHeader(icon = Icons.Outlined.PlayArrow, title = "YouTube Automation")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enable YouTube Automation",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Switch(
                                checked = youtubeEnabled,
                                onCheckedChange = { youtubeEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = Color(0xFF0E3744)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "YouTube Data API v3 key for searching songs, playlists, and seamless voice playback.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "YOUTUBE API KEY",
                            color = TextMuted,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = youtubeApiKey,
                            onValueChange = { youtubeApiKey = it },
                            placeholder = { Text("Paste YouTube Data API v3 Key", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showYoutubeKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showYoutubeKey = !showYoutubeKey }) {
                                    Icon(
                                        imageVector = if (showYoutubeKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF060E13),
                                unfocusedContainerColor = Color(0xFF060E13),
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberFieldBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste from clipboard",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    clipboardManager.getText()?.text?.let { youtubeApiKey = it.trim() }
                                }
                            )

                            OutlinedButton(
                                onClick = { onValidateYouTubeKey(youtubeApiKey) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF1D4D5C)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF081B23))
                            ) {
                                Text(
                                    text = "VALIDATE KEY",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val statusMsg = when {
                            youtubeKeyValidationStatus.isNotEmpty() -> youtubeKeyValidationStatus
                            youtubeApiKey.isBlank() -> "Status: Key is empty"
                            else -> "Status: Key configured (ready)"
                        }
                        Text(
                            text = statusMsg,
                            color = if (statusMsg.contains("empty")) AmberGlow else EmeraldGlow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                // 8. MOBILE CONTROL SECTION (matching Screenshot 2)
                item {
                    SectionHeader(icon = Icons.Outlined.Smartphone, title = "Mobile Control")
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSectionCard {
                        Text(
                            text = "WHAT ${assistantName.uppercase()} CAN DO ON YOUR PHONE",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        MobileCommandRow(
                            primary = "\"Turn on / off flashlight\"",
                            secondary = null,
                            onClick = {
                                saveAndClose()
                                onExecuteQuickCommand("Turn on flashlight")
                            }
                        )
                        MobileCommandRow(
                            primary = "\"Set brightness to 50%\"",
                            secondary = ", \"Increase brightness\"",
                            onClick = {
                                saveAndClose()
                                onExecuteQuickCommand("Set brightness to 50%")
                            }
                        )
                        MobileCommandRow(
                            primary = "\"Volume up / down\"",
                            secondary = ", \"Set volume to 80%\"",
                            onClick = {
                                saveAndClose()
                                onExecuteQuickCommand("Set volume to 80%")
                            }
                        )
                        MobileCommandRow(
                            primary = "\"Open Instagram\"",
                            secondary = ", \"Open WhatsApp\", \"Open Spotify\"",
                            onClick = {
                                saveAndClose()
                                onExecuteQuickCommand("Open WhatsApp")
                            }
                        )
                        MobileCommandRow(
                            primary = "\"Send WhatsApp to Rahul saying I will be late\"",
                            secondary = null,
                            onClick = {
                                saveAndClose()
                                onExecuteQuickCommand("Send WhatsApp to Rahul saying I will be late")
                            }
                        )
                        MobileCommandRow(
                            primary = "\"Call Rahul on WhatsApp\"",
                            secondary = ", \"Video call Mom\"",
                            onClick = {
                                saveAndClose()
                                onExecuteQuickCommand("Call Rahul on WhatsApp")
                            }
                        )
                        MobileCommandRow(
                            primary = "\"Remember my gym starts at 6 PM\"",
                            secondary = ", \"Forget routine\"",
                            onClick = {
                                saveAndClose()
                                onExecuteQuickCommand("Remember my gym starts at 6 PM")
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(EmeraldGlow, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WhatsApp Confirmation Policy: Active",
                                color = EmeraldGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$assistantName will never send any message without explicit verbal confirmation.",
                            color = TextMuted,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A1D24))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeonCyan,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun CyberSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CyberCardBg)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun VoicePill(
    voice: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(if (isSelected) CyanBright else Color(0xFF081217))
            .border(
                width = 1.dp,
                color = if (isSelected) CyanBright else CyberFieldBorder,
                shape = RoundedCornerShape(21.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = voice,
            color = if (isSelected) Color.Black else TextIce,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TraitChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) Color(0xFF0D2832) else Color(0xFF071015))
            .border(
                width = 1.dp,
                color = if (isActive) NeonCyan.copy(alpha = 0.6f) else CyberFieldBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) TextIce else TextMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PermissionRowItem(
    name: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF060E13))
            .border(1.dp, CyberFieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = TextPrimary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Text(
            text = actionLabel,
            color = if (granted) EmeraldGlow else NeonCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MobileCommandRow(
    primary: String,
    secondary: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(NeonCyan, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Medium)) {
                        append(primary)
                    }
                    if (secondary != null) {
                        withStyle(SpanStyle(color = TextMuted)) {
                            append(secondary)
                        }
                    }
                },
                fontSize = 13.5.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF10222A))
        )
    }
}
