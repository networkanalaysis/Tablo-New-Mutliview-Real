package com.example.ui.multiview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.PlayerSlot
import com.example.ui.components.VideoSurfaceView
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.ActiveAudioPill
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceRaised
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SoloPlayerScreen(
    slot: PlayerSlot,
    onBackToMultiview: () -> Unit,
    onOpenPicker: () -> Unit,
    onNextChannel: () -> Unit,
    onPrevChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToMultiview()
    }

    val channel by slot.channel.collectAsState()
    val isBuffering by slot.isBuffering.collectAsState()
    val errorMessage by slot.errorMessage.collectAsState()

    var showControls by remember { mutableStateOf(true) }

    // Auto hide controls after 6 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(6000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("solo_player_screen")
    ) {
        // Video Surface
        VideoSurfaceView(
            player = slot.player,
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        AnimatedVisibility(
            visible = isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                strokeWidth = 3.dp,
                color = AccentGreen,
                modifier = Modifier.size(48.dp)
            )
        }

        // Top HUD Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xE6000000), Color.Transparent),
                            startY = 0f,
                            endY = 160f
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back to Multiview button
                    Button(
                        onClick = onBackToMultiview,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Return to Multiview (2x2)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Channel & Audio Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ActiveAudioPill
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AUDIO LIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Button(
                            onClick = onOpenPicker,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Channel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Bottom HUD Bar (Airings & Synopsis)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xF2000000)),
                            startY = 0f,
                            endY = 220f
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (channel?.kind == "ota") AccentGreen else Color(0xFF3B82F6)
                            ) {
                                Text(
                                    text = if (channel?.kind == "ota") "OTA BROADCAST" else "FAST STREAM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = channel?.displayName ?: "",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )

                            if (!channel?.network.isNullOrEmpty()) {
                                Text(
                                    text = "• ${channel?.network}",
                                    fontSize = 16.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Next / Prev channel buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onPrevChannel,
                                modifier = Modifier.background(SurfaceRaised, CircleShape)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Channel", tint = TextPrimary)
                            }
                            IconButton(
                                onClick = onNextChannel,
                                modifier = Modifier.background(SurfaceRaised, CircleShape)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Channel", tint = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        text = channel?.currentProgram?.title ?: "Live Program Airing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    channel?.currentProgram?.description?.let { desc ->
                        if (desc.isNotEmpty()) {
                            Text(
                                text = desc,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Stream Error state in Solo view
        if (errorMessage != null) {
            Surface(
                color = Color(0xEE121620),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Stream Disconnected", color = AccentRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(errorMessage ?: "", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = { slot.retry() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Connection")
                        }
                        Button(onClick = onBackToMultiview) {
                            Text("Return to Grid")
                        }
                    }
                }
            }
        }
    }
}
