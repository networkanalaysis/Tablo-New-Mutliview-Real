package com.example.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MultiviewLayoutType
import com.example.model.NavigationDestination
import com.example.model.TabloDevice
import com.example.ui.components.tvFocusable
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ActiveAudioPill
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceRaised
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvNavHeader(
    currentDestination: NavigationDestination,
    onNavigate: (NavigationDestination) -> Unit,
    activeDevice: TabloDevice?,
    layoutType: MultiviewLayoutType,
    onLayoutChange: (MultiviewLayoutType) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(30_000)
        }
    }

    Surface(
        color = SurfaceDark,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("tv_nav_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: App Brand & Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tablo_multiview_icon_1789502942553),
                    contentDescription = "Tablo Multiview Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Text(
                    text = "TABLO MULTIVIEW",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
            }

            // Center: Navigation Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationDestination.values().forEach { dest ->
                    val isSelected = currentDestination == dest
                    var isFocused by remember { mutableStateOf(false) }
                    val shape = RoundedCornerShape(8.dp)

                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(
                                if (isSelected) ActiveAudioPill
                                else if (isFocused) SurfaceRaised
                                else Color.Transparent
                            )
                            .tvFocusable(isFocused = isFocused, shape = shape, scaleWhenFocused = 1.05f)
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable { onNavigate(dest) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("nav_tab_${dest.name}")
                    ) {
                        Text(
                            text = dest.title.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else if (isFocused) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            // Right: Device status, Layout selector (when in Multiview), and Clock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Layout selector pills (when on Multiview)
                if (currentDestination == NavigationDestination.MULTIVIEW) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(SurfaceRaised, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        listOf(
                            MultiviewLayoutType.QUAD_2X2 to "2x2",
                            MultiviewLayoutType.DUAL_2UP to "2-Up",
                            MultiviewLayoutType.SOLO_1UP to "1-Up"
                        ).forEach { (lType, label) ->
                            val isCurrent = layoutType == lType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) ActiveAudioPill else Color.Transparent)
                                    .clickable { onLayoutChange(lType) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Device connection status chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeDevice != null) "Tablo 4th Gen" else "Local Ready",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }

                // Clock
                Text(
                    text = currentTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}
