package com.example.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import com.example.model.TabloChannel
import com.example.model.TabloStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerSlot(
    val slotIndex: Int,
    private val context: Context
) {
    companion object {
        private const val TAG = "PlayerSlot"
    }

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = true
        volume = 0f // default muted until focused
    }

    private val _channel = MutableStateFlow<TabloChannel?>(null)
    val channel: StateFlow<TabloChannel?> = _channel.asStateFlow()

    private val _stream = MutableStateFlow<TabloStream?>(null)
    val stream: StateFlow<TabloStream?> = _stream.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isMuted = MutableStateFlow(true)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                        _isPlaying.value = false
                    }
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        _isPlaying.value = player.isPlaying
                        _errorMessage.value = null
                    }
                    Player.STATE_ENDED -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Slot $slotIndex error: ${error.message}", error)
                _isBuffering.value = false
                _isPlaying.value = false
                _errorMessage.value = "Playback error: ${error.localizedMessage ?: "Stream failed"}"
            }
        })
    }

    fun load(ch: TabloChannel, st: TabloStream, muted: Boolean = true) {
        _channel.value = ch
        _stream.value = st
        _errorMessage.value = null
        _isBuffering.value = true

        mute(muted)

        try {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Tablo-Multiview/1.0 (Android TV; ExoPlayer)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(15000)

            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(st.playlistUrl))

            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting media source on slot $slotIndex: ${e.message}", e)
            _errorMessage.value = "Failed starting stream: ${e.message}"
            _isBuffering.value = false
        }
    }

    fun mute(muted: Boolean) {
        _isMuted.value = muted
        player.volume = if (muted) 0f else 1f
    }

    fun retry() {
        val currentChannel = _channel.value
        val currentStream = _stream.value
        if (currentChannel != null && currentStream != null) {
            load(currentChannel, currentStream, _isMuted.value)
        }
    }

    fun clear() {
        player.stop()
        player.clearMediaItems()
        _channel.value = null
        _stream.value = null
        _errorMessage.value = null
        _isBuffering.value = false
        _isPlaying.value = false
    }

    fun release() {
        player.release()
    }
}
