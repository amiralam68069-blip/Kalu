package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AssistantState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HudHeader(
    assistantName: String,
    state: AssistantState,
    onOpenSettings: () -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Assistant status tag
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = when (state) {
                                AssistantState.LISTENING -> NeonCyan
                                AssistantState.THINKING -> ElectricPurple
                                AssistantState.SPEAKING -> NeonPink
                                AssistantState.IDLE -> EmeraldGlow
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$assistantName CORE // ${state.name}",
                    color = TextIce,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "SYS TIME: $currentTime",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }

        // Right action buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClearChat,
                modifier = Modifier
                    .size(38.dp)
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .background(GlassSurface, RoundedCornerShape(10.dp))
                    .testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear transcript",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .background(GlassSurface, RoundedCornerShape(10.dp))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
