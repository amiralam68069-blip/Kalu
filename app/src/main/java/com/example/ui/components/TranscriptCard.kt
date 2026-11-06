package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TranscriptView(
    messages: List<ChatMessage>,
    assistantName: String,
    onConfirmWhatsApp: (String, String) -> Unit,
    onConfirmCall: (String, Boolean) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        // Section Title matching Screenshot 4
        Text(
            text = "CONVERSATION",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )

        // Framed Cyber Container with Corner L-Brackets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xD9081319))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(18.dp))
                .drawWithContent {
                    drawContent()
                    val bracketLen = 18.dp.toPx()
                    val strokeW = 1.8.dp.toPx()
                    val inset = 8.dp.toPx()
                    val bracketColor = NeonCyan.copy(alpha = 0.65f)

                    // Top-left L-bracket
                    drawLine(
                        color = bracketColor,
                        start = Offset(inset, inset),
                        end = Offset(inset + bracketLen, inset),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = bracketColor,
                        start = Offset(inset, inset),
                        end = Offset(inset, inset + bracketLen),
                        strokeWidth = strokeW
                    )

                    // Top-right L-bracket
                    drawLine(
                        color = bracketColor,
                        start = Offset(size.width - inset - bracketLen, inset),
                        end = Offset(size.width - inset, inset),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = bracketColor,
                        start = Offset(size.width - inset, inset),
                        end = Offset(size.width - inset, inset + bracketLen),
                        strokeWidth = strokeW
                    )
                }
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SYSTEM ONLINE // AWAITING COMMAND",
                            color = TextCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the glowing mic below or use VISION / HISTORY",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    assistantName: String,
    onConfirmWhatsApp: (String, String) -> Unit,
    onConfirmCall: (String, Boolean) -> Unit,
    onOpenUrl: (String) -> Unit,
    onToggleNote: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender + Timestamp Header matching Screenshot 4 ("You · 20:10:47" / "Amancipher.Ai · 20:10:47")
        Text(
            text = if (isUser) "You  ·  $timeStr" else "$assistantName  ·  $timeStr",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(
                bottom = 5.dp,
                start = if (isUser) 0.dp else 40.dp,
                end = if (isUser) 40.dp else 0.dp
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                SmallAssistantBadge(modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Bubble Box
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUser) Color(0xFF0E323D) else Color(0xFF101C24)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) NeonCyan.copy(alpha = 0.55f) else Color(0xFF1D333E),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp
                    )

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
                                    onConfirm = { onConfirmCall(card.phoneNumber, card.viaWhatsApp) }
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
                            is ActionCard.DeviceActionCard -> {
                                DeviceControlStatusCard(card = card)
                            }
                            is ActionCard.ScreenInspectCard -> {
                                ScreenInspectionResultCard(card = card)
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF102A34))
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "User",
                        tint = NeonCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallAssistantBadge(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF0D222B))
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val dotR = 1.4.dp.toPx()
        drawCircle(color = NeonCyan, radius = dotR, center = Offset(cx, cy))
        val ringR = size.minDimension * 0.25f
        for (i in 0 until 6) {
            val rad = Math.toRadians(i * 60.0)
            drawCircle(
                color = NeonCyan,
                radius = dotR,
                center = Offset(
                    cx + (ringR * cos(rad)).toFloat(),
                    cy + (ringR * sin(rad)).toFloat()
                )
            )
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
                    text = "WHATSAPP CONFIRMATION POLICY",
                    color = EmeraldGlow,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
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
                Text("Confirm & Send WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    contentDescription = "Call",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (card.viaWhatsApp) "CONFIRM WHATSAPP CALL" else "CONFIRM VOICE CALL",
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
                Text(
                    text = if (card.viaWhatsApp) "Start WhatsApp Call" else "Call Now",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun WeatherInfoCard(card: ActionCard.WeatherCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
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
                Text(text = "💧 Humidity: ${card.humidity}%", color = TextMuted, fontSize = 11.sp)
                Text(text = "💨 Wind: ${card.windSpeed.toInt()} km/h", color = TextMuted, fontSize = 11.sp)
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
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D212A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "YouTube",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YOUTUBE AUTOMATION",
                    color = NeonCyan,
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
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play in YouTube", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                Text(text = card.time, color = AmberGlow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
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

@Composable
private fun DeviceControlStatusCard(card: ActionCard.DeviceActionCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2129)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.actionTitle,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "● ${card.statusText}",
                    color = EmeraldGlow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.detail,
                color = TextPrimary,
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
private fun ScreenInspectionResultCard(card: ActionCard.ScreenInspectCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1E26)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "SCREEN INSPECTOR // ${card.elementCount} NODES",
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Text(
                text = "Package: ${card.activePackage}",
                color = TextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            card.summary.forEach { item ->
                Text(
                    text = "• $item",
                    color = TextIce,
                    fontSize = 12.sp
                )
            }
        }
    }
}
