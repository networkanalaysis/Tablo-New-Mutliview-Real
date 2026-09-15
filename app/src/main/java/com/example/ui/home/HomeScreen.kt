package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloChannel
import com.example.model.TabloRecording
import com.example.ui.components.tvFocusable
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ActiveAudioPill
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceRaised
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    channels: List<TabloChannel>,
    recordings: List<TabloRecording>,
    favorites: Set<String>,
    onOpenMultiview: () -> Unit,
    onPlayChannel: (TabloChannel, Int) -> Unit,
    onPlayRecording: (TabloRecording) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteChannels = remember(channels, favorites) {
        channels.filter { favorites.contains(it.identifier) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Card: Live Multiview Experience
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMultiview() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .padding(28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentGreen
                            ) {
                                Text(
                                    text = "TABLO 4TH GEN • MULTIVIEW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Simultaneous 4-Channel Live Multiview",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Experience sports and live broadcast news simultaneously in a 2x2 grid. D-pad moves instant visual focus and audio focus with zero video hitching.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onOpenMultiview,
                                colors = ButtonDefaults.buttonColors(containerColor = ActiveAudioPill),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Launch Multiview 2x2", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Row 1: Favorite Channels (if any)
        if (favoriteChannels.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Favorite Channels",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteChannels, key = { it.identifier }) { ch ->
                            HomeChannelCard(
                                channel = ch,
                                onSelect = { onPlayChannel(ch, 0) }
                            )
                        }
                    }
                }
            }
        }

        // Row 2: Live Channels
        item {
            Column {
                Text(
                    text = "Live OTA Broadcasts & FAST Streams",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(channels, key = { it.identifier }) { ch ->
                        HomeChannelCard(
                            channel = ch,
                            onSelect = { onPlayChannel(ch, 0) }
                        )
                    }
                }
            }
        }

        // Row 3: Tablo Recordings Library
        if (recordings.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Tablo DVR Recordings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recordings, key = { it.identifier }) { rec ->
                            HomeRecordingCard(
                                recording = rec,
                                onSelect = { onPlayRecording(rec) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeChannelCard(
    channel: TabloChannel,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) SurfaceRaised else SurfaceDark
        ),
        modifier = Modifier
            .width(220.dp)
            .height(115.dp)
            .tvFocusable(isFocused = isFocused, shape = shape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (channel.kind == "ota") AccentGreen else Color(0xFF3B82F6)
                ) {
                    Text(
                        text = if (channel.kind == "ota") "OTA" else "FAST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = channel.network,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Column {
                Text(
                    text = channel.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = channel.currentProgram?.title ?: "Live Airing",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun HomeRecordingCard(
    recording: TabloRecording,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) SurfaceRaised else SurfaceDark
        ),
        modifier = Modifier
            .width(240.dp)
            .height(115.dp)
            .tvFocusable(isFocused = isFocused, shape = shape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF8B5CF6)
                ) {
                    Text(
                        text = "DVR RECORDING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (recording.durationSec > 0) {
                    Text(
                        text = "${recording.durationSec / 60}m",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Column {
                Text(
                    text = recording.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
