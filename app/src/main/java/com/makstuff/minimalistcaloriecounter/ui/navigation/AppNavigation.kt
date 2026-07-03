package com.makstuff.minimalistcaloriecounter.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

fun NavHostController.navigateApp(route: String) {
    if (AppRoutes.isTopLevelRoute(route)) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navigate(route)
    }
}
