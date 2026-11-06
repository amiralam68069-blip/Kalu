package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    config: NovaConfig,
    contacts: List<Contact>,
    onSaveConfig: (NovaConfig) -> Unit,
    onAddContact: (String, String, String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var assistantName by remember { mutableStateOf(config.assistantName) }
    var userName by remember { mutableStateOf(config.userName) }
    var wakeWordEnabled by remember { mutableStateOf(config.wakeWordEnabled) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var glowThemeIndex by remember { mutableStateOf(config.glowThemeIndex) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = General, 1 = Contacts

    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .testTag("settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = ObsidianDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOVA // SYSTEM CONFIG",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurface)
                        .padding(4.dp)
                ) {
                    TabPill(
                        title = "General & AI",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        title = "Contacts (${contacts.size})",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        GeneralSettingsTab(
                            assistantName = assistantName,
                            onAssistantNameChange = { assistantName = it },
                            userName = userName,
                            onUserNameChange = { userName = it },
                            wakeWordEnabled = wakeWordEnabled,
                            onWakeWordChange = { wakeWordEnabled = it },
                            apiKey = apiKey,
                            onApiKeyChange = { apiKey = it },
                            showApiKey = showApiKey,
                            onToggleShowApiKey = { showApiKey = !showApiKey },
                            glowThemeIndex = glowThemeIndex,
                            onGlowThemeChange = { glowThemeIndex = it }
                        )
                    } else {
                        ContactsTab(
                            contacts = contacts,
                            newContactName = newContactName,
                            onNewNameChange = { newContactName = it },
                            newContactPhone = newContactPhone,
                            onNewPhoneChange = { newContactPhone = it },
                            onAdd = {
                                if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                                    onAddContact(newContactName.trim(), newContactPhone.trim(), "Friend")
                                    newContactName = ""
                                    newContactPhone = ""
                                }
                            },
                            onDelete = onDeleteContact
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save button
                Button(
                    onClick = {
                        onSaveConfig(
                            config.copy(
                                assistantName = assistantName.ifBlank { "NOVA" },
                                userName = userName.ifBlank { "User" },
                                wakeWordEnabled = wakeWordEnabled,
                                apiKey = apiKey.trim(),
                                glowThemeIndex = glowThemeIndex
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Save & Apply Config", color = ObsidianDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) ObsidianDark else TextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun GeneralSettingsTab(
    assistantName: String,
    onAssistantNameChange: (String) -> Unit,
    userName: String,
    onUserNameChange: (String) -> Unit,
    wakeWordEnabled: Boolean,
    onWakeWordChange: (Boolean) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit,
    glowThemeIndex: Int,
    onGlowThemeChange: (Int) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("ASSISTANT IDENTITY", color = TextCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = assistantName,
                onValueChange = onAssistantNameChange,
                label = { Text("Assistant Name (e.g. NOVA, JARVIS)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assistant_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        item {
            Text("USER IDENTITY", color = TextCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = userName,
                onValueChange = onUserNameChange,
                label = { Text("Your Name (e.g. Aman)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        item {
            Text("CORE THEME GLOW", color = TextCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThemeGlowColorOption("Cyan", NeonCyan, glowThemeIndex == 0) { onGlowThemeChange(0) }
                ThemeGlowColorOption("Purple", ElectricPurple, glowThemeIndex == 1) { onGlowThemeChange(1) }
                ThemeGlowColorOption("Pink", NeonPink, glowThemeIndex == 2) { onGlowThemeChange(2) }
                ThemeGlowColorOption("Emerald", EmeraldGlow, glowThemeIndex == 3) { onGlowThemeChange(3) }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hands-Free 'Hey Nova' Wake Word", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Listen in background for wake command", color = TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = wakeWordEnabled,
                    onCheckedChange = onWakeWordChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                )
            }
        }

        item {
            Text("GEMINI API KEY (OPTIONAL)", color = TextCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Enter Gemini API Key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_api_key_input"),
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleShowApiKey) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = TextMuted
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "NOVA already works out-of-the-box with built-in voice intelligence. Add a key to unlock extended deep conversational reasoning.",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ThemeGlowColorOption(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name, color = if (isSelected) Color.White else TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ContactsTab(
    contacts: List<Contact>,
    newContactName: String,
    onNewNameChange: (String) -> Unit,
    newContactPhone: String,
    onNewPhoneChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("ADD NEW CONTACT", color = TextCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newContactName,
                    onValueChange = onNewNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = newContactPhone,
                    onValueChange = onNewPhoneChange,
                    label = { Text("Phone") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceLight)
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Contact for WhatsApp/Calls", color = TextIce, fontSize = 12.sp)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("SAVED CONTACTS", color = TextCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }

        items(contacts, key = { it.id }) { c ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = c.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(text = c.phone, color = TextMuted, fontSize = 11.sp)
                }
                IconButton(onClick = { onDelete(c.id) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = NeonPink.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
