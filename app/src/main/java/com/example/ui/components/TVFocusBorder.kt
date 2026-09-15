package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FocusHighlight
import com.example.ui.theme.SurfaceBorder

@Composable
fun Modifier.tvFocusable(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color = FocusHighlight,
    unfocusedBorderColor: Color = SurfaceBorder,
    focusedBorderWidth: Dp = 2.5.dp,
    unfocusedBorderWidth: Dp = 1.dp,
    scaleWhenFocused: Float = 1.02f
): Modifier {
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else unfocusedBorderColor,
        animationSpec = tween(durationMillis = 150),
        label = "borderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
        animationSpec = tween(durationMillis = 150),
        label = "borderWidth"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleWhenFocused else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )

    return this
        .scale(scale)
        .border(
            border = BorderStroke(borderWidth, borderColor),
            shape = shape
        )
}
