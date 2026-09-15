package com.example.playback

import android.content.Context
import com.example.model.MultiviewLayoutType
import com.example.model.TabloChannel
import com.example.model.TabloStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MultiviewPlaybackManager(context: Context) {
    val slots: List<PlayerSlot> = listOf(
        PlayerSlot(0, context),
        PlayerSlot(1, context),
        PlayerSlot(2, context),
        PlayerSlot(3, context)
    )

    private val _activeAudioSlot = MutableStateFlow(0)
    val activeAudioSlot: StateFlow<Int> = _activeAudioSlot.asStateFlow()

    private val _soloSlotIndex = MutableStateFlow<Int?>(null)
    val soloSlotIndex: StateFlow<Int?> = _soloSlotIndex.asStateFlow()

    private val _layoutType = MutableStateFlow(MultiviewLayoutType.QUAD_2X2)
    val layoutType: StateFlow<MultiviewLayoutType> = _layoutType.asStateFlow()

    init {
        // Slot 0 starts unmuted, others muted
        setActiveAudioSlot(0)
    }

    fun setActiveAudioSlot(index: Int) {
        if (index !in 0..3) return
        _activeAudioSlot.value = index
        slots.forEachIndexed { i, slot ->
            slot.mute(i != index)
        }
    }

    fun setChannelForSlot(slotIndex: Int, channel: TabloChannel, stream: TabloStream) {
        if (slotIndex !in 0..3) return
        val isAudible = (_activeAudioSlot.value == slotIndex)
        slots[slotIndex].load(channel, stream, muted = !isAudible)
    }

    fun clearSlot(slotIndex: Int) {
        if (slotIndex in 0..3) {
            slots[slotIndex].clear()
        }
    }

    fun setLayoutType(layout: MultiviewLayoutType) {
        _layoutType.value = layout
        if (layout == MultiviewLayoutType.SOLO_1UP) {
            enterSoloMode(_activeAudioSlot.value)
        } else if (_soloSlotIndex.value != null && layout != MultiviewLayoutType.SOLO_1UP) {
            exitSoloMode()
        }
    }

    fun enterSoloMode(slotIndex: Int) {
        if (slotIndex !in 0..3) return
        _soloSlotIndex.value = slotIndex
        setActiveAudioSlot(slotIndex)
    }

    fun exitSoloMode() {
        _soloSlotIndex.value = null
    }

    fun release() {
        slots.forEach { it.release() }
    }
}
