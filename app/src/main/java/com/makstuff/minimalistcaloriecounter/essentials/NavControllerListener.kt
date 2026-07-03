package com.makstuff.minimalistcaloriecounter.essentials

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.makstuff.minimalistcaloriecounter.R

@Composable
fun NavControllerListener(
    nameFoodDayAdd: String,
    nameFoodDayEdit: String,
    navController: NavHostController,
    setNav: (String, NavButton) -> Unit,
    context: Context
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute, nameFoodDayAdd, nameFoodDayEdit) {
        when (currentRoute) {
            "day_home" ->
                setNav(context.getString(R.string.title_add_food_to_day), NAV_DAY)

            "day_content" ->
                setNav(context.getString(R.string.title_edit_food_in_day), NAV_DAY)

            "quick_import" ->
                setNav("Add Meal", NAV_DAY)

            "day_add_food" ->
                setNav(context.getString(R.string.title_pick_food), NAV_DAY)

            "day_add_weight/{index}" ->
                setNav(context.getString(R.string.title_enter_weight_of) + " " + nameFoodDayAdd, NAV_DAY)

            "day_edit_weight/{index}" ->
                setNav(context.getString(R.string.title_edit_weight_of) + " " + nameFoodDayEdit, NAV_DAY)

            "archive_home" ->
                setNav(context.getString(R.string.archive), NAV_ARCHIVE)

            "health_connect_nutrition" ->
                setNav("Meals", NAV_ARCHIVE)

            "goals_home" ->
                setNav("Goals", NAV_GOALS)

            "settings_home" ->
                setNav(context.getString(R.string.options), NAV_DAY)

            "archive_create_entry_manually" ->
                setNav(context.getString(R.string.title_create_new_archive_entry), NAV_ARCHIVE)

            "archive_create_entry_from_day" ->
                setNav(context.getString(R.string.title_enter_body_weight), NAV_ARCHIVE)

            "archive_edit_entry/{index}" ->
                setNav(context.getString(R.string.title_edit_archive_entry), NAV_ARCHIVE)

            "database_home" ->
                setNav(context.getString(R.string.database), NAV_DATABASE)

            "database_edit_entry/{index}" ->
                setNav(context.getString(R.string.title_edit_database_entry), NAV_DATABASE)

            "create_home" ->
                setNav(context.getString(R.string.title_create_new_database_entry), NAV_CREATE)

        }
    }
}
