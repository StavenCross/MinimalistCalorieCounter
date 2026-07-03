package com.makstuff.minimalistcaloriecounter.ui.reused

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

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
    NavigationBarItem(
        label = { Text(name) },
        icon = { Icon(painterResource(id = iconId), contentDescription = name) },
        selected = isSelected,
        onClick = { onClick() },
        colors = NavigationBarItemDefaults.colors(
            unselectedIconColor = resolvedIconColor.copy(alpha = 0.78f),
            unselectedTextColor = MaterialTheme.colorScheme.secondary,
            selectedIconColor = resolvedIconColor,
            selectedTextColor = MaterialTheme.colorScheme.secondary,
            indicatorColor = resolvedIconColor.copy(alpha = 0.18f)
        )
    )
}
