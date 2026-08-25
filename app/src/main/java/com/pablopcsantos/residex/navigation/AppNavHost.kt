package com.pablopcsantos.residex.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pablopcsantos.residex.residency.ui.AdministrationScreen
import com.pablopcsantos.residex.residency.ui.AboutProjectScreen
import com.pablopcsantos.residex.residency.ui.AdministrationViewModel
import com.pablopcsantos.residex.residency.ui.CalendarScreen
import com.pablopcsantos.residex.residency.ui.CalendarViewModel
import com.pablopcsantos.residex.residency.ui.ManageSelectionsScreen
import com.pablopcsantos.residex.residency.ui.SelectionDetailsScreen
import com.pablopcsantos.residex.residency.ui.SelectionDetailsViewModel
import com.pablopcsantos.residex.residency.ui.SettingsScreen
import com.pablopcsantos.residex.residency.ui.SettingsViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.CALENDAR,
        modifier = modifier
    ) {
        composable(Destinations.CALENDAR) { CalendarScreen(hiltViewModel()) { id -> navController.navigate("${Destinations.DETAILS}/$id") } }
        composable(Destinations.MY_SELECTIONS) { ManageSelectionsScreen(hiltViewModel()) { id -> navController.navigate("${Destinations.DETAILS}/$id") } }
        composable(Destinations.ADMINISTRATION) { AdministrationScreen(hiltViewModel<AdministrationViewModel>()) }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = hiltViewModel<SettingsViewModel>(),
                onOpenAbout = { navController.navigate(Destinations.ABOUT_PROJECT) }
            )
        }
        composable(Destinations.ABOUT_PROJECT) {
            AboutProjectScreen(onBack = navController::popBackStack)
        }
        composable("${Destinations.DETAILS}/{selectionId}") {
            SelectionDetailsScreen(it.arguments?.getString("selectionId").orEmpty(), hiltViewModel(), navController::popBackStack)
        }
    }
}
