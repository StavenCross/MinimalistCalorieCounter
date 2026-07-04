package com.makstuff.minimalistcaloriecounter.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AccentMenu = Color(0xFF90CAF9)
private val AccentSettings = Color(0xFFFFD166)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    currentRoute: String?,
    onOpenMenu: () -> Unit,
    onOpenQuickImportSettings: () -> Unit,
    onOpenGoalsSettings: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open navigation menu",
                    tint = AccentMenu,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        actions = {
            if (currentRoute == AppRoutes.QUICK_IMPORT) {
                IconButton(onClick = onOpenQuickImportSettings) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Add Meal settings",
                        tint = AccentSettings,
                    )
                }
            }
            if (currentRoute == AppRoutes.GOALS_HOME) {
                IconButton(onClick = onOpenGoalsSettings) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Goal settings",
                        tint = AccentSettings,
                    )
                }
            }
        },
    )
}
