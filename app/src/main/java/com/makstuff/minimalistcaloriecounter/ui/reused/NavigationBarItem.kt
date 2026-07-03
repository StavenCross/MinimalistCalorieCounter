package com.makstuff.minimalistcaloriecounter.ui.reused

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

data class NavigationBarItemData(
    val name: String,
    val iconId: Int,
    val isSelected: Boolean,
    val iconColor: Color? = null,
    val onClick: () -> Unit,
)

@Composable
fun RowScope.NavigationBarItem(
    name: String,
    iconId: Int,
    isSelected: Boolean,
    iconColor: Color? = null,
    onClick: () -> Unit,
) {
    val resolvedIconColor = iconColor ?: MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        modifier = Modifier
            .weight(1f)
            .height(72.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(resolvedIconColor.copy(alpha = 0.92f)),
            )
        }
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = name,
            tint = if (isSelected) resolvedIconColor else resolvedIconColor.copy(alpha = 0.72f),
            modifier = Modifier.size(28.dp),
        )
    }
}
