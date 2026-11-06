package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun TranscriptView(
    messages: List<ChatMessage>,
    assistantName: String,
    onConfirmWhatsApp: (String, String) -> Unit,
    onConfirmCall: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onToggleNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AWAITING AUDIO INPUT",
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap the mic below or say 'Hey Nova' to speak",
                    color = TextIce.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                assistantName = assistantName,
                onConfirmWhatsApp = onConfirmWhatsApp,
                onConfirmCall = onConfirmCall,
                onOpenUrl = onOpenUrl,
                onToggleNote = onToggleNote
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    assistantName: String,
    onConfirmWhatsApp: (String, String) -> Unit,
    onConfirmCall: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onToggleNote: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Tag label
        Text(
            text = if (isUser) "YOU" else assistantName,
            color = if (isUser) TextViolet else TextCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp)
        )

        // Glass bubble
        Box(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) GlassBorderPurple else GlassBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) Color(0x70201438) else GlassSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Interactive Action Cards if attached
                message.actionCard?.let { card ->
                    Spacer(modifier = Modifier.height(10.dp))
                    when (card) {
                        is ActionCard.WhatsAppConfirm -> {
                            WhatsAppConfirmationCard(
                                card = card,
                                onConfirm = { onConfirmWhatsApp(card.phoneNumber, card.message) }
                            )
                        }
                        is ActionCard.CallConfirm -> {
                            CallConfirmationCard(
                                card = card,
                                onConfirm = { onConfirmCall(card.phoneNumber) }
                            )
                        }
                        is ActionCard.WeatherCard -> {
                            WeatherInfoCard(card = card)
                        }
                        is ActionCard.YouTubePlayerCard -> {
                            YouTubeActionCard(
                                card = card,
                                onOpen = { onOpenUrl(card.videoUrl) }
                            )
                        }
                        is ActionCard.AlarmCard -> {
                            AlarmInfoCard(card = card)
                        }
                        is ActionCard.NoteCard -> {
                            NoteItemCard(
                                card = card,
                                onToggle = { onToggleNote(card.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsAppConfirmationCard(
    card: ActionCard.WhatsAppConfirm,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x60064E3B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "WhatsApp",
                    tint = EmeraldGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CONFIRM WHATSAPP MESSAGE",
                    color = EmeraldGlow,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "To: ${card.contactName} (${card.phoneNumber})",
                color = TextIce,
                fontSize = 12.sp
            )
            Text(
                text = "\"${card.message}\"",
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_whatsapp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Haan, Bhej Do (WhatsApp)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CallConfirmationCard(
    card: ActionCard.CallConfirm,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x600C4A6E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Phone Call",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CONFIRM VOICE CALL",
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Call ${card.contactName} (${card.phoneNumber})?",
                color = TextIce,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_call_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Milao", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WeatherInfoCard(card: ActionCard.WeatherCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurfaceLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.location.uppercase(),
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "${card.tempC.toInt()}°C",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = card.description, color = TextIce, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💧 Humidity: ${card.humidity}%",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "💨 Wind: ${card.windSpeed.toInt()} km/h",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun YouTubeActionCard(
    card: ActionCard.YouTubePlayerCard,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x604C0519)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "YouTube",
                    tint = NeonPink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YOUTUBE PLAYER",
                    color = NeonPink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = card.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_youtube_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play in YouTube", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AlarmInfoCard(card: ActionCard.AlarmCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmberGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x60451A03)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Alarm",
                tint = AmberGlow,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = card.time,
                    color = AmberGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(text = card.label, color = TextIce, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NoteItemCard(
    card: ActionCard.NoteCard,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurfaceLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = card.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = card.text,
                color = if (card.isCompleted) TextMuted else TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
