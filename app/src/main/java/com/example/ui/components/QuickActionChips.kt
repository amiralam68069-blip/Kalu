package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class QuickAction(
    val icon: String,
    val title: String,
    val command: String
)

@Composable
fun QuickActionChips(
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickAction("🎵", "Music Play", "YouTube pe badiya sa song play karo"),
        QuickAction("💬", "WhatsApp Aman", "Aman ko Hi message bhejo"),
        QuickAction("📞", "Call Aman", "Aman ko call milao"),
        QuickAction("⛅", "Weather Today", "Aaj ka mausam kaisa hai?"),
        QuickAction("⏰", "7:00 AM Alarm", "Subah 7 baje ka alarm laga do"),
        QuickAction("🌅", "Morning Briefing", "Aaj ki morning briefing do"),
        QuickAction("📝", "Add Note", "Note likho: Project launch review at 5 PM"),
        QuickAction("🎭", "Shayari Sunao", "Ek mast romantic shayari sunao"),
        QuickAction("😄", "Ek Joke", "Mujhe ek funny chutkula sunao"),
        QuickAction("🔍", "Search News", "Google search latest technology news")
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { item ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .background(GlassSurface)
                    .clickable { onActionSelected(item.command) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("quick_action_${item.title.replace(" ", "_").lowercase()}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.title,
                    color = TextIce,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}
