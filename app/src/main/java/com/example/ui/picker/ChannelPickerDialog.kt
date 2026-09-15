package com.example.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun ChannelPickerDialog(
    targetSlotIndex: Int,
    channels: List<TabloChannel>,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onChannelSelected: (TabloChannel) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredChannels = remember(channels, favorites, selectedFilter) {
        when (selectedFilter) {
            "OTA" -> channels.filter { it.kind == "ota" }
            "FAST" -> channels.filter { it.kind != "ota" }
            "Favorites" -> channels.filter { favorites.contains(it.identifier) }
            else -> channels
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .widthIn(min = 600.dp, max = 800.dp)
                .fillMaxHeight(0.88f)
                .padding(24.dp)
                .testTag("channel_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Channel for Slot ${targetSlotIndex + 1}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose a live OTA broadcast or FAST stream to assign to this tile",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(SurfaceRaised, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("All", "OTA", "FAST", "Favorites").forEach { filterName ->
                        val isSelected = selectedFilter == filterName
                        Button(
                            onClick = { selectedFilter = filterName },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) ActiveAudioPill else SurfaceRaised,
                                contentColor = if (isSelected) Color.White else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = filterName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Channels List
                if (filteredChannels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No channels in this category",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredChannels, key = { it.identifier }) { ch ->
                            ChannelRowItem(
                                channel = ch,
                                isFavorite = favorites.contains(ch.identifier),
                                onToggleFavorite = { onToggleFavorite(ch.identifier) },
                                onSelect = { onChannelSelected(ch) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelRowItem(
    channel: TabloChannel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) SurfaceRaised else Color(0xFF141926)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(isFocused = isFocused, shape = shape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onSelect() }
            .testTag("picker_channel_${channel.identifier}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge (OTA / FAST)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (channel.kind == "ota") AccentGreen else Color(0xFF3B82F6),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = if (channel.kind == "ota") "OTA" else "FAST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Channel name & network
            Column(modifier = Modifier.width(130.dp)) {
                Text(
                    text = channel.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (channel.network.isNotEmpty()) {
                    Text(
                        text = channel.network,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Airing program title & synopsis
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.currentProgram?.title ?: "Live Transmission",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                channel.currentProgram?.description?.let { desc ->
                    if (desc.isNotEmpty()) {
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Favorite toggle
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Favorite" else "Add to favorites",
                    tint = if (isFavorite) AccentAmber else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
