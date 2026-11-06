package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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

    var showSettingsDialog by remember { mutableStateOf(false) }

    // Audio recording runtime permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for voice interaction.")
            }
        }
    }

    // Handle external intents (WhatsApp, Phone call, YouTube, Web search)
    LaunchedEffect(viewModel) {
        viewModel.externalIntent.collectLatest { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Could not open application for this action.")
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
                            Color(0xFF060912)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top HUD telemetry header
                HudHeader(
                    assistantName = config.assistantName,
                    state = state,
                    onOpenSettings = { showSettingsDialog = true },
                    onClearChat = { viewModel.clearChat() }
                )

                // Quick Action Chips Row
                QuickActionChips(
                    onActionSelected = { command ->
                        viewModel.submitQuery(command)
                    }
                )

                // Dynamic 3D Reactive Orb Centerpiece
                NovaOrbCanvas(
                    state = state,
                    audioAmplitude = amplitude,
                    glowThemeIndex = config.glowThemeIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Live Transcripts & Action Cards
                TranscriptView(
                    messages = messages,
                    assistantName = config.assistantName,
                    onConfirmWhatsApp = { phone, msg ->
                        viewModel.confirmWhatsApp(phone, msg)
                    },
                    onConfirmCall = { phone ->
                        viewModel.confirmCall(phone)
                    },
                    onOpenUrl = { url ->
                        viewModel.openUrl(url)
                    },
                    onToggleNote = { noteId ->
                        viewModel.toggleNote(noteId)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Bottom Tap-to-Talk Mic & Keyboard Command Bar
                BottomControlBar(
                    state = state,
                    onMicClick = { handleMicClick() },
                    onInterruptClick = { viewModel.interrupt() },
                    onSendText = { text -> viewModel.submitQuery(text) },
                    glowThemeIndex = config.glowThemeIndex
                )
            }

            // Settings Sheet / Modal
            if (showSettingsDialog) {
                SettingsDialog(
                    config = config,
                    contacts = contacts,
                    onSaveConfig = { updated ->
                        viewModel.saveConfig(updated)
                    },
                    onAddContact = { name, phone, rel ->
                        viewModel.addContact(name, phone, rel)
                    },
                    onDeleteContact = { id ->
                        viewModel.deleteContact(id)
                    },
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}
