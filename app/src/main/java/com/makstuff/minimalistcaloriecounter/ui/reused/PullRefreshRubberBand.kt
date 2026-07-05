package com.makstuff.minimalistcaloriecounter.ui.reused

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.pullRefreshRubberBand(isRefreshing: Boolean): Modifier {
    val maxOffsetPx = with(LocalDensity.current) { 96.dp.toPx() }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetPx,
        animationSpec = if (offsetPx == 0f) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "pullRefreshRubberBandOffset",
    )
    val connection = remember(maxOffsetPx, isRefreshing) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || isRefreshing) return Offset.Zero
                offsetPx = when {
                    available.y > 0f -> (offsetPx + available.y * 0.34f).coerceIn(0f, maxOffsetPx)
                    available.y < 0f -> (offsetPx + available.y * 0.72f).coerceAtLeast(0f)
                    else -> offsetPx
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                offsetPx = 0f
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) offsetPx = 0f
    }

    return nestedScroll(connection)
        .graphicsLayer { translationY = animatedOffset }
}
