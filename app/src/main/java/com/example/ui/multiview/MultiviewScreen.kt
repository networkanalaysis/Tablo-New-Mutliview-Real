package com.example.ui.multiview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.MultiviewLayoutType
import com.example.playback.MultiviewPlaybackManager
import com.example.ui.theme.CanvasDark

@Composable
fun MultiviewScreen(
    playbackManager: MultiviewPlaybackManager,
    onOpenChannelPickerForSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAudioSlot by playbackManager.activeAudioSlot.collectAsState()
    val soloSlotIndex by playbackManager.soloSlotIndex.collectAsState()
    val layoutType by playbackManager.layoutType.collectAsState()

    if (soloSlotIndex != null) {
        val activeSoloSlot = playbackManager.slots[soloSlotIndex!!]
        SoloPlayerScreen(
            slot = activeSoloSlot,
            onBackToMultiview = { playbackManager.exitSoloMode() },
            onOpenPicker = { onOpenChannelPickerForSlot(soloSlotIndex!!) },
            onNextChannel = {
                // cycle to next slot in solo
                val nextSlot = (soloSlotIndex!! + 1) % 4
                playbackManager.enterSoloMode(nextSlot)
            },
            onPrevChannel = {
                val prevSlot = (soloSlotIndex!! + 3) % 4
                playbackManager.enterSoloMode(prevSlot)
            }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(12.dp)
            .testTag("multiview_grid_container")
    ) {
        when (layoutType) {
            MultiviewLayoutType.QUAD_2X2 -> {
                // 2x2 Grid (4 tiles)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Slot 0 & Slot 1
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MultiviewSlotTile(
                            slot = playbackManager.slots[0],
                            isAudible = activeAudioSlot == 0,
                            onFocused = { playbackManager.setActiveAudioSlot(0) },
                            onEnterSolo = { playbackManager.enterSoloMode(0) },
                            onOpenPicker = { onOpenChannelPickerForSlot(0) },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                        MultiviewSlotTile(
                            slot = playbackManager.slots[1],
                            isAudible = activeAudioSlot == 1,
                            onFocused = { playbackManager.setActiveAudioSlot(1) },
                            onEnterSolo = { playbackManager.enterSoloMode(1) },
                            onOpenPicker = { onOpenChannelPickerForSlot(1) },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    }

                    // Bottom Row: Slot 2 & Slot 3
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MultiviewSlotTile(
                            slot = playbackManager.slots[2],
                            isAudible = activeAudioSlot == 2,
                            onFocused = { playbackManager.setActiveAudioSlot(2) },
                            onEnterSolo = { playbackManager.enterSoloMode(2) },
                            onOpenPicker = { onOpenChannelPickerForSlot(2) },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                        MultiviewSlotTile(
                            slot = playbackManager.slots[3],
                            isAudible = activeAudioSlot == 3,
                            onFocused = { playbackManager.setActiveAudioSlot(3) },
                            onEnterSolo = { playbackManager.enterSoloMode(3) },
                            onOpenPicker = { onOpenChannelPickerForSlot(3) },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    }
                }
            }

            MultiviewLayoutType.DUAL_2UP -> {
                // 2-Up Side by Side (Slot 0 & Slot 1)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MultiviewSlotTile(
                        slot = playbackManager.slots[0],
                        isAudible = activeAudioSlot == 0,
                        onFocused = { playbackManager.setActiveAudioSlot(0) },
                        onEnterSolo = { playbackManager.enterSoloMode(0) },
                        onOpenPicker = { onOpenChannelPickerForSlot(0) },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                    MultiviewSlotTile(
                        slot = playbackManager.slots[1],
                        isAudible = activeAudioSlot == 1,
                        onFocused = { playbackManager.setActiveAudioSlot(1) },
                        onEnterSolo = { playbackManager.enterSoloMode(1) },
                        onOpenPicker = { onOpenChannelPickerForSlot(1) },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }
            }

            MultiviewLayoutType.SOLO_1UP -> {
                // Single 1-Up Slot
                val soloSlot = playbackManager.slots[activeAudioSlot]
                MultiviewSlotTile(
                    slot = soloSlot,
                    isAudible = true,
                    onFocused = { },
                    onEnterSolo = { playbackManager.enterSoloMode(activeAudioSlot) },
                    onOpenPicker = { onOpenChannelPickerForSlot(activeAudioSlot) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
