package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.ChatMessage
import com.example.data.MemoryEntity
import com.example.service.AmancipherAccessibilityService
import com.example.service.SwipeDirection
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VisionInspectorModal(
    onAnalyzeVision: (Bitmap?, String) -> Unit,
    onInspectScreenCommand: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var visionPrompt by remember { mutableStateOf("Describe what you see and highlight key details") }
    val isA11yConnected by AmancipherAccessibilityService.isConnected.collectAsState()
    val inspectedElements by AmancipherAccessibilityService.lastInspectedElements.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            capturedBitmap = bmp
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                capturedBitmap = bmp
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .testTag("vision_modal"),
            colors = CardDefaults.cardColors(containerColor = ObsidianDark),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "Vision",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AMANCIPHER.AI // VISION & SCREEN",
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Camera & Photo Picker Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val hasCam = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera Snap", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Photo", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = capturedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Captured preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusWeak,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Capture a camera frame or pick a photo for Gemini Vision",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prompt Field + Analyze Button
                OutlinedTextField(
                    value = visionPrompt,
                    onValueChange = { visionPrompt = it },
                    label = { Text("Vision Question / Instruction") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onAnalyzeVision(capturedBitmap, visionPrompt)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze with Amancipher.Ai Vision", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Accessibility Screen Inspector Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCESSIBILITY SCREEN INSPECTOR",
                        color = TextCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isA11yConnected) "● Active" else "○ Inactive",
                        color = if (isA11yConnected) EmeraldGlow else AmberGlow,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    if (!isA11yConnected) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Enable Amancipher.Ai Accessibility Service to inspect screen nodes, click buttons, type text, and scroll automatically.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onOpenAccessibilitySettings,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("Open Accessibility Settings", color = NeonCyan, fontSize = 12.sp)
                            }
                        }
                    } else {
                        val lastGesture by AmancipherAccessibilityService.lastGestureStatus.collectAsState()
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nodes: ${inspectedElements.size} · $lastGesture",
                                    color = TextIce,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    AmancipherAccessibilityService.instance?.inspectScreen()
                                    onInspectScreenCommand()
                                    onDismiss()
                                }) {
                                    Text("Read Screen", color = NeonCyan, fontSize = 12.sp)
                                }
                            }

                            // Quick UI Gesture Control Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "↑ Up" to { AmancipherAccessibilityService.instance?.performSwipe(SwipeDirection.UP) },
                                    "↓ Down" to { AmancipherAccessibilityService.instance?.performSwipe(SwipeDirection.DOWN) },
                                    "← Left" to { AmancipherAccessibilityService.instance?.performSwipe(SwipeDirection.LEFT) },
                                    "→ Right" to { AmancipherAccessibilityService.instance?.performSwipe(SwipeDirection.RIGHT) },
                                    "◁ Back" to { AmancipherAccessibilityService.instance?.navigateBack() }
                                ).forEach { (label, action) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0E222B))
                                            .border(1.dp, CyberFieldBorder, RoundedCornerShape(8.dp))
                                            .clickable { action() }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = label, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(inspectedElements.take(20)) { node ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                val a11y = AmancipherAccessibilityService.instance
                                                if (node.centerX > 0 && node.centerY > 0) {
                                                    a11y?.performTap(node.centerX.toFloat(), node.centerY.toFloat())
                                                } else {
                                                    a11y?.clickByText(node.text)
                                                }
                                            }
                                            .padding(vertical = 2.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• [${node.className}] ${node.text}",
                                            color = if (node.isClickable) TextIce else TextMuted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (node.centerX > 0 || node.centerY > 0) {
                                            Text(
                                                text = "(${node.centerX},${node.centerY})",
                                                color = NeonCyan.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryAndMemoryModal(
    messages: List<ChatMessage>,
    memories: List<MemoryEntity>,
    initialTab: Int = 0,
    onSendCommand: (String) -> Unit,
    onClearHistory: () -> Unit,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (Int) -> Unit,
    onClearMemories: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var commandText by remember { mutableStateOf("") }
    var newMemoryText by remember { mutableStateOf("") }
    var newMemoryCategory by remember { mutableStateOf("Routine") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .testTag("history_modal"),
            colors = CardDefaults.cardColors(containerColor = ObsidianDark),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "CONVERSATION & COMMANDS" else "MEMORY CORE (ROOM DB)",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) NeonCyan else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "History (${messages.size})",
                            color = if (selectedTab == 0) Color.Black else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) NeonCyan else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Memory Core (${memories.size})",
                            color = if (selectedTab == 1) Color.Black else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Quick Text Command Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            placeholder = { Text("Type command (e.g. Turn on flashlight)...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_command_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commandText.isNotBlank()) {
                                    onSendCommand(commandText.trim())
                                    commandText = ""
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonCyan)
                                .testTag("send_command_button")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages.reversed(), key = { it.id }) { msg ->
                            val time = remember(msg.timestamp) {
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.timestamp))
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberCardBg)
                                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${msg.sender.name} · $time",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(text = msg.text, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonBorder)
                    ) {
                        Text("Clear Short-Term Conversation Buffer", color = CrimsonText, fontSize = 12.sp)
                    }
                } else {
                    // Memory Core Tab (Room DB)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Routine", "Preference", "Note", "Fact").forEach { cat ->
                            val active = newMemoryCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) NeonCyan else CyberCardBg)
                                    .border(1.dp, if (active) NeonCyan else CyberCardBorder, RoundedCornerShape(14.dp))
                                    .clickable { newMemoryCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (active) Color.Black else TextIce,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newMemoryText,
                            onValueChange = { newMemoryText = it },
                            placeholder = { Text("Add memory (e.g. Gym starts at 6 PM)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newMemoryText.isNotBlank()) {
                                    onAddMemory(newMemoryCategory, newMemoryText.trim())
                                    newMemoryText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonCyan)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Memory", tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (memories.isEmpty()) {
                            item {
                                Text(
                                    text = "No long-term memories stored yet. Say 'Remember my gym starts at 6 PM' or add one above.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            items(memories, key = { it.id }) { mem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyberCardBg)
                                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mem.category.uppercase(),
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = mem.content, color = TextPrimary, fontSize = 13.sp)
                                    }
                                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Memory",
                                            tint = CrimsonText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onClearMemories,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonBorder)
                    ) {
                        Text("CLEAR ALL LONG-TERM MEMORIES", color = CrimsonText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
