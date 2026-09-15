package com.example.ui.livetv

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
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
import com.example.model.TabloChannel
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

@Composable
fun LiveTvScreen(
    channels: List<TabloChannel>,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onWatchInSlot: (TabloChannel, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableStateOf(channels.firstOrNull()) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(16.dp)
            .testTag("live_tv_screen"),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Channels List (55% width)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.1f)
        ) {
            Text(
                text = "Live Channels (${channels.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.identifier }) { ch ->
                    val isSelected = selectedChannel?.identifier == ch.identifier
                    var isFocused by remember { mutableStateOf(false) }
                    val shape = RoundedCornerShape(10.dp)

                    Card(
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected || isFocused) SurfaceRaised else SurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) ActiveAudioPill else SurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(isFocused = isFocused, shape = shape)
                            .onFocusChanged {
                                isFocused = it.isFocused
                                if (it.isFocused) selectedChannel = ch
                            }
                            .focusable()
                            .clickable { selectedChannel = ch }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (ch.kind == "ota") AccentGreen else Color(0xFF3B82F6),
                                    modifier = Modifier.padding(end = 10.dp)
                                ) {
                                    Text(
                                        text = if (ch.kind == "ota") "OTA" else "FAST",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = ch.displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = ch.currentProgram?.title ?: "Live Broadcast",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(onClick = { onToggleFavorite(ch.identifier) }) {
                                Icon(
                                    imageVector = if (favorites.contains(ch.identifier)) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (favorites.contains(ch.identifier)) AccentAmber else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Channel Detail & Slot Action Dispatcher (45% width)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.9f)
        ) {
            val ch = selectedChannel
            if (ch != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (ch.kind == "ota") AccentGreen else Color(0xFF3B82F6)
                            ) {
                                Text(
                                    text = if (ch.kind == "ota") "OTA ANTENNA" else "FAST STREAM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (ch.network.isNotEmpty()) {
                                Text(ch.network, fontSize = 13.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = ch.displayName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "CURRENT AIRING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = AccentGreen
                        )

                        Text(
                            text = ch.currentProgram?.title ?: "Live Television Transmission",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = ch.currentProgram?.description ?: "Tune in live through Tablo 4th Gen high-definition digital tuner.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Multiview Slot Assignment Actions
                    Column {
                        Text(
                            text = "ASSIGN TO MULTIVIEW TILE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (0..3).forEach { slotIndex ->
                                Button(
                                    onClick = { onWatchInSlot(ch, slotIndex) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Slot ${slotIndex + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onWatchInSlot(ch, 0) },
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveAudioPill),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Watch in Multiview Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a channel to preview", color = TextSecondary)
                }
            }
        }
    }
}
