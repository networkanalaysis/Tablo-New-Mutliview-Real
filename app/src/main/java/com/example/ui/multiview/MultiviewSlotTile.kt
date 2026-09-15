package com.example.ui.multiview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.PlayerSlot
import com.example.ui.components.VideoSurfaceView
import com.example.ui.components.tvFocusable
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.ActiveAudioPill
import com.example.ui.theme.MutedPill
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceRaised
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MultiviewSlotTile(
    slot: PlayerSlot,
    isAudible: Boolean,
    onFocused: () -> Unit,
    onEnterSolo: () -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channel by slot.channel.collectAsState()
    val isBuffering by slot.isBuffering.collectAsState()
    val errorMessage by slot.errorMessage.collectAsState()
    var isTileFocused by remember { mutableStateOf(false) }

    val tileShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(tileShape)
            .background(SurfaceDark)
            .tvFocusable(
                isFocused = isTileFocused,
                shape = tileShape
            )
            .onFocusChanged { focusState ->
                isTileFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocused()
                }
            }
            .focusable()
            .clickable {
                if (channel != null) {
                    onEnterSolo()
                } else {
                    onOpenPicker()
                }
            }
            .testTag("slot_tile_${slot.slotIndex}")
    ) {
        if (channel != null) {
            // Live Video Surface
            VideoSurfaceView(
                player = slot.player,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle top and bottom gradient overlays for readability of TV badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xCC000000), Color.Transparent),
                            startY = 0f,
                            endY = 120f
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Channel Badge (Major.Minor / Call Sign)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (channel?.kind == "ota") AccentGreen else Color(0xFF3B82F6)
                        ) {
                            Text(
                                text = if (channel?.kind == "ota") "OTA" else "FAST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = channel?.displayName ?: "",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (!channel?.network.isNullOrEmpty()) {
                            Text(
                                text = "• ${channel?.network}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Audio Focus Pill Badge
                    Surface(
                        shape = CircleShape,
                        color = if (isAudible) ActiveAudioPill else MutedPill
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isAudible) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = if (isAudible) "Audio Active" else "Muted",
                                tint = if (isAudible) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAudible) "AUDIO" else "MUTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAudible) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            // Bottom overlay: Current airing title & Change Channel action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xE6000000)),
                            startY = 0f,
                            endY = 140f
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel?.currentProgram?.title ?: "Live Broadcast",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        channel?.currentProgram?.description?.let { desc ->
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

                    // Switch Channel button when focused
                    if (isTileFocused) {
                        Button(
                            onClick = onOpenPicker,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceRaised,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Switch",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Buffering Indicator
            AnimatedVisibility(
                visible = isBuffering,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x99000000),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = AccentGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Error Overlay with Retry
            if (errorMessage != null) {
                Surface(
                    color = Color(0xEE121620),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Stream Interrupted",
                            color = AccentRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = errorMessage ?: "Unable to connect to live stream",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { slot.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onOpenPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                            ) {
                                Text("Pick Channel", fontSize = 12.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }

        } else {
            // Empty Slot State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isTileFocused) ActiveAudioPill else SurfaceRaised,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Channel",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Slot ${slot.slotIndex + 1} • Add Channel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTileFocused) TextPrimary else TextSecondary
                )
                Text(
                    text = "Press OK / Select to choose live channel",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
