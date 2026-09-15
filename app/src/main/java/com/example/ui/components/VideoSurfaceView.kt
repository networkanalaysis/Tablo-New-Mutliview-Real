package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun VideoSurfaceView(
    player: Player?,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false
                this.resizeMode = resizeMode
                this.player = player
            }
        },
        update = { view ->
            if (view.player != player) {
                view.player = player
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
        },
        modifier = modifier
    )

    DisposableEffect(player) {
        onDispose {
            // Keep player instance lifecycle managed by PlayerSlot
        }
    }
}
