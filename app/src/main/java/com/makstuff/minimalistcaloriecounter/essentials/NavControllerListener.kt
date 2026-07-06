package com.makstuff.minimalistcaloriecounter.essentials

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes

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
            AppRoutes.DAY_HOME ->
                setNav(context.getString(R.string.title_add_food_to_day), NAV_DAY)

            AppRoutes.DAY_CONTENT ->
                setNav(context.getString(R.string.title_edit_food_in_day), NAV_DAY)

            AppRoutes.DAY_ADD_FOOD ->
                setNav(context.getString(R.string.title_pick_food), NAV_DAY)

            AppRoutes.DAY_ADD_WEIGHT ->
                setNav(context.getString(R.string.title_enter_weight_of) + " " + nameFoodDayAdd, NAV_DAY)

            AppRoutes.DAY_EDIT_WEIGHT ->
                setNav(context.getString(R.string.title_edit_weight_of) + " " + nameFoodDayEdit, NAV_DAY)

            AppRoutes.ARCHIVE_HOME ->
                setNav(context.getString(R.string.archive), NAV_ARCHIVE)

            AppRoutes.HEALTH_CONNECT_NUTRITION ->
                setNav("Meals", NAV_ARCHIVE)

            AppRoutes.GOALS_HOME ->
                setNav("Goals", NAV_GOALS)

            AppRoutes.SETTINGS_HOME ->
                setNav(context.getString(R.string.options), NAV_DAY)

            AppRoutes.ARCHIVE_CREATE_ENTRY_MANUALLY ->
                setNav(context.getString(R.string.title_create_new_archive_entry), NAV_ARCHIVE)

            AppRoutes.ARCHIVE_CREATE_ENTRY_FROM_DAY ->
                setNav(context.getString(R.string.title_enter_body_weight), NAV_ARCHIVE)

            AppRoutes.ARCHIVE_EDIT_ENTRY ->
                setNav(context.getString(R.string.title_edit_archive_entry), NAV_ARCHIVE)

            AppRoutes.DATABASE_HOME ->
                setNav(context.getString(R.string.database), NAV_DATABASE)

            AppRoutes.DATABASE_EDIT_ENTRY ->
                setNav(context.getString(R.string.title_edit_database_entry), NAV_DATABASE)

            AppRoutes.CREATE_HOME ->
                setNav(context.getString(R.string.title_create_new_database_entry), NAV_CREATE)

        }
    }
}
