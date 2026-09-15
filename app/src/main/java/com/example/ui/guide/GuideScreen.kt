package com.example.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TabloRepository
import com.example.model.TabloChannel
import com.example.model.TabloProgram
import com.example.ui.components.tvFocusable
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ActiveAudioPill
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceRaised
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GuideScreen(
    repository: TabloRepository,
    channels: List<TabloChannel>,
    favorites: Set<String>,
    onPlayChannelInSlot: (TabloChannel, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableStateOf(channels.firstOrNull()) }
    var channelAirings by remember { mutableStateOf<List<TabloProgram>>(emptyList()) }
    var selectedProgram by remember { mutableStateOf<TabloProgram?>(null) }
    var targetSlot by remember { mutableStateOf(0) }

    LaunchedEffect(channels) {
        if (selectedChannel == null && channels.isNotEmpty()) {
            selectedChannel = channels.first()
        }
    }

    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { ch ->
            channelAirings = repository.getGuideAiringsForChannel(ch)
            selectedProgram = channelAirings.firstOrNull()
        }
    }

    val timeSlots = remember {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        (0..4).map { i ->
            sdf.format(Date(now + (i * 30 * 60 * 1000L)))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(16.dp)
            .testTag("guide_screen")
    ) {
        // Top Info & Slot Target Bar
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedProgram?.title ?: selectedChannel?.currentProgram?.title ?: "Select a Broadcast",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = selectedProgram?.description ?: selectedChannel?.currentProgram?.description ?: "Full EPG guide synced from Tablo 4th Gen tuner.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Slot Target Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Watch in:", fontSize = 12.sp, color = TextSecondary)
                    (0..3).forEach { slotIdx ->
                        val isSelected = targetSlot == slotIdx
                        Button(
                            onClick = {
                                targetSlot = slotIdx
                                selectedChannel?.let { onPlayChannelInSlot(it, slotIdx) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) ActiveAudioPill else SurfaceRaised,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Slot ${slotIdx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Time slots header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 190.dp, bottom = 8.dp)
        ) {
            timeSlots.forEach { timeStr ->
                Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    modifier = Modifier.width(200.dp)
                )
            }
        }

        // Guide Grid Body
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(channels, key = { it.identifier }) { ch ->
                val isSelectedChannel = selectedChannel?.identifier == ch.identifier
                var isRowFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Channel info cell
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelectedChannel) SurfaceRaised else SurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelectedChannel) ActiveAudioPill else SurfaceBorder
                        ),
                        modifier = Modifier
                            .width(180.dp)
                            .height(68.dp)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    selectedChannel = ch
                                }
                            }
                            .focusable()
                            .clickable { selectedChannel = ch }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = ch.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = ch.network.ifEmpty { if (ch.kind == "ota") "OTA" else "FAST" },
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { repository.toggleFavorite(ch.identifier) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (favorites.contains(ch.identifier)) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (favorites.contains(ch.identifier)) AccentAmber else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Programs row for this channel
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val airings = repository.getFallbackProgramsForChannel(ch)
                        items(airings) { prog ->
                            var isProgFocused by remember { mutableStateOf(false) }
                            val progShape = RoundedCornerShape(8.dp)

                            Card(
                                shape = progShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isProgFocused) SurfaceRaised else Color(0xFF131826)
                                ),
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(68.dp)
                                    .tvFocusable(isFocused = isProgFocused, shape = progShape)
                                    .onFocusChanged {
                                        isProgFocused = it.isFocused
                                        if (it.isFocused) {
                                            selectedChannel = ch
                                            selectedProgram = prog
                                        }
                                    }
                                    .focusable()
                                    .clickable {
                                        selectedChannel = ch
                                        selectedProgram = prog
                                        onPlayChannelInSlot(ch, targetSlot)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = prog.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (prog.genres.isNotEmpty()) prog.genres.first() else "Broadcast",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1
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
