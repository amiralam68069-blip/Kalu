package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AssistantState
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun NovaScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val config by viewModel.config.collectAsStateWithLifecycle()
    val state by viewModel.assistantState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val brightnessPercent by viewModel.brightnessPercent.collectAsStateWithLifecycle()
    val youtubeValidationStatus by viewModel.youtubeValidationStatus.collectAsStateWithLifecycle()

    var showSettingsScreen by remember { mutableStateOf(false) }
    var showVisionModal by remember { mutableStateOf(false) }
    var showHistoryModal by remember { mutableStateOf(false) }
    var historyInitialTab by remember { mutableStateOf(0) }

    // Apply live window brightness changes triggered by voice or Mobile Control commands
    LaunchedEffect(brightnessPercent) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = (brightnessPercent / 100f).coerceIn(0.05f, 1.0f)
            window.attributes = params
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for voice commands.")
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.externalIntent.collectLatest { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Unable to launch external application.")
                }
            }
        }
    }

    fun handleMicClick() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            if (state == AssistantState.LISTENING) {
                viewModel.stopListening()
            } else {
                viewModel.startListening()
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ObsidianDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianDark,
                            DeepSpace,
                            Color(0xFF03080B)
                        )
                    )
                )
                .drawBehind {
                    // Cybernetic coordinate grid matching Screenshot 4
                    val step = 30.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = CyberGridLine,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = CyberGridLine,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top HUD Bar matching Screenshot 4
                HudHeader(
                    assistantName = config.assistantName,
                    state = state,
                    isOnline = config.isOnline,
                    onOpenSettings = { showSettingsScreen = true },
                    onClearChat = { viewModel.clearChat() }
                )

                // 2. Centerpiece Cybernetic Radar/Reactor Orb matching Screenshot 4
                NovaOrbCanvas(
                    state = state,
                    audioAmplitude = amplitude,
                    glowThemeIndex = config.glowThemeIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(275.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // 3. Framed CONVERSATION Box matching Screenshot 4
                TranscriptView(
                    messages = messages,
                    assistantName = config.assistantName,
                    onConfirmWhatsApp = { phone, msg ->
                        viewModel.confirmWhatsApp(phone, msg)
                    },
                    onConfirmCall = { phone, viaWhatsApp ->
                        viewModel.confirmCall(phone, viaWhatsApp)
                    },
                    onOpenUrl = { url ->
                        viewModel.openUrl(url)
                    },
                    onToggleNote = { noteId ->
                        viewModel.toggleNote(noteId)
                    },
                    modifier = Modifier.weight(1f)
                )

                // 4. Bottom 4-Control Dock: [ONLINE] [VISION] (MIC) [HISTORY]
                BottomControlBar(
                    state = state,
                    isOnline = config.isOnline,
                    onToggleOnline = { viewModel.toggleOnline() },
                    onVisionClick = { showVisionModal = true },
                    onHistoryClick = {
                        historyInitialTab = 0
                        showHistoryModal = true
                    },
                    onMicClick = { handleMicClick() },
                    onInterruptClick = { viewModel.interrupt() },
                    onSendText = { text -> viewModel.submitQuery(text) },
                    glowThemeIndex = config.glowThemeIndex
                )
            }

            // Full-Screen Settings Overlay matching Screenshots 1, 2, 3
            if (showSettingsScreen) {
                SettingsDialog(
                    config = config,
                    contacts = contacts,
                    youtubeKeyValidationStatus = youtubeValidationStatus,
                    onValidateYouTubeKey = { key -> viewModel.validateYouTubeKey(key) },
                    onClearMemories = {
                        viewModel.clearAllMemories()
                        viewModel.clearChat()
                        scope.launch {
                            snackbarHostState.showSnackbar("Memory Core & conversation buffer cleared.")
                        }
                    },
                    onViewMemories = {
                        historyInitialTab = 1
                        showHistoryModal = true
                    },
                    onOpenAccessibilitySettings = {
                        viewModel.openAccessibilitySettings()
                    },
                    onExecuteQuickCommand = { cmd ->
                        viewModel.submitQuery(cmd)
                    },
                    onSaveConfig = { updated ->
                        viewModel.saveConfig(updated)
                    },
                    onAddContact = { name, phone, rel ->
                        viewModel.addContact(name, phone, rel)
                    },
                    onDeleteContact = { id ->
                        viewModel.deleteContact(id)
                    },
                    onDismiss = { showSettingsScreen = false }
                )
            }

            // Vision & Screen Inspection Modal
            if (showVisionModal) {
                VisionInspectorModal(
                    onAnalyzeVision = { bmp, prompt ->
                        viewModel.analyzeVision(bmp, prompt)
                    },
                    onInspectScreenCommand = {
                        viewModel.submitQuery("Inspect screen")
                    },
                    onOpenAccessibilitySettings = {
                        viewModel.openAccessibilitySettings()
                    },
                    onDismiss = { showVisionModal = false }
                )
            }

            // History, Command Input & Memory Core Modal
            if (showHistoryModal) {
                HistoryAndMemoryModal(
                    messages = messages,
                    memories = memories,
                    initialTab = historyInitialTab,
                    onSendCommand = { cmd -> viewModel.submitQuery(cmd) },
                    onClearHistory = { viewModel.clearChat() },
                    onAddMemory = { cat, text -> viewModel.addMemory(cat, text) },
                    onDeleteMemory = { id -> viewModel.deleteMemory(id) },
                    onClearMemories = { viewModel.clearAllMemories() },
                    onDismiss = { showHistoryModal = false }
                )
            }
        }
    }
}
