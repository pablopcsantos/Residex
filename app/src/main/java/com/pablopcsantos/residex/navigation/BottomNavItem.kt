package com.pablopcsantos.residex.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {

    object Calendar : BottomNavItem(
        route = Destinations.CALENDAR,
        label = "Calendário",
        icon = Icons.Filled.CalendarMonth
    )

    object MySelections : BottomNavItem(
        route = Destinations.MY_SELECTIONS,
        label = "Seleções",
        icon = Icons.Filled.Star
    )

    object Administration : BottomNavItem(
        route = Destinations.ADMINISTRATION,
        label = "Admin",
        icon = Icons.Filled.AdminPanelSettings
    )

    object Settings : BottomNavItem(
        route = Destinations.SETTINGS,
        label = "Ajustes",
        icon = Icons.Filled.Settings
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Calendar,
    BottomNavItem.MySelections,
    BottomNavItem.Administration,
    BottomNavItem.Settings
)
