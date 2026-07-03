package com.makstuff.minimalistcaloriecounter.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.ui.reused.NavigationBar
import com.makstuff.minimalistcaloriecounter.ui.reused.NavigationBarItem
import com.makstuff.minimalistcaloriecounter.ui.reused.NavigationBarItemData

@Composable
fun AppBottomBar(
    navigationBarHighlight: NavButton,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        items = AppDestinations.bottomBar.map { destination ->
            NavigationBarItemData(
                destination.label,
                destination.iconId,
                navigationBarHighlight == destination.navButton,
                Color(destination.accentArgb),
            ) { onNavigate(destination.route) }
        }.map {
            {
                NavigationBarItem(
                    name = it.name,
                    iconId = it.iconId,
                    isSelected = it.isSelected,
                    iconColor = it.iconColor,
                    onClick = it.onClick,
                )
            }
        }
    )
}

@Composable
fun AppMainDrawer(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable { onDismiss() },
    )
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(304.dp)
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { }
            .padding(top = 28.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
        AppDestinations.drawer.forEachIndexed { index, destination ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DrawerNavItem(destination.label) {
                onDismiss()
                onNavigate(destination.route)
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}
