package com.makstuff.minimalistcaloriecounter.ui.reused

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
// Central panel shell for elevated screen sections; callers own intent-specific color and spacing choices.
fun SurfacePanel(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentPadding: Int = 8,
    verticalSpacing: Int = 6,
    tonalElevation: Int = 0,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = tonalElevation.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding.dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp),
            content = content,
        )
    }
}
