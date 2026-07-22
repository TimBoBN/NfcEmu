package com.nfcemu.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.ui.home.HomeScreen
import com.nfcemu.ui.library.LibraryScreen
import com.nfcemu.ui.onboarding.OnboardingScreen
import com.nfcemu.ui.onboarding.OnboardingViewModel
import com.nfcemu.ui.profileform.ProfileFormScreen
import com.nfcemu.ui.profileform.TypePickerScreen
import com.nfcemu.ui.profilelist.ProfileListScreen
import com.nfcemu.ui.scantag.ScanTagScreen
import com.nfcemu.ui.scantag.ScannedPayloadCodec
import com.nfcemu.ui.settings.SettingsScreen
import com.nfcemu.ui.theme.Motion
import com.nfcemu.ui.writetag.WriteTagScreen

private object Routes {
    const val HOME = "home"
    const val PROFILE_LIST = "profileList"
    const val TYPE_PICKER = "typePicker"
    const val PROFILE_FORM_NEW = "profileForm/new/{template}"
    const val PROFILE_FORM_EDIT = "profileForm/edit/{profileId}"
    const val PROFILE_FORM_SCAN_REVIEW = "profileForm/scanReview/{scannedTag}"
    const val SCAN_TAG = "scanTag"
    const val WRITE_TAG = "writeTag/{profileId}"
    const val SETTINGS = "settings"
    const val LIBRARY = "library"
}

/**
 * Shared-axis-style screen transitions (forward = slide in from the end + fade,
 * back = slide in from the start + fade), applied once here as [NavHost] defaults
 * rather than repeated on every `composable()` call. Distances are relative to the
 * container (`AnimatedContentTransitionScope`), so this looks right on any screen
 * size instead of using a fixed pixel offset.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedEasing),
    ) + fadeIn(tween(Motion.DURATION_MEDIUM))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedEasing),
    ) + fadeOut(tween(Motion.DURATION_SHORT))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedEasing),
    ) + fadeIn(tween(Motion.DURATION_MEDIUM))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedEasing),
    ) + fadeOut(tween(Motion.DURATION_SHORT))

@Composable
fun NfcEmuNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {
        composable(Routes.HOME) {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsState()

            when (onboardingCompleted) {
                null -> Unit // still loading persisted flag, avoid flashing either screen
                false -> OnboardingScreen(onDone = { /* state flips reactively, no nav needed */ })
                true -> HomeScreen(
                    onNavigateToProfiles = { navController.navigate(Routes.PROFILE_LIST) },
                    onNavigateToLibrary = { navController.navigate(Routes.LIBRARY) },
                    onNavigateToNewProfile = { navController.navigate(Routes.TYPE_PICKER) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
        }
        composable(Routes.PROFILE_LIST) {
            ProfileListScreen(
                onBack = { navController.popBackStack() },
                onNewProfile = { navController.navigate(Routes.TYPE_PICKER) },
                onEditProfile = { profile -> navController.navigate("profileForm/edit/${profile.id}") },
                onWriteToTag = { profile -> navController.navigate("writeTag/${profile.id}") },
            )
        }
        composable(Routes.TYPE_PICKER) {
            TypePickerScreen(
                onBack = { navController.popBackStack() },
                onTypeSelected = { template ->
                    navController.navigate("profileForm/new/${template.name}") {
                        popUpTo(Routes.TYPE_PICKER) { inclusive = true }
                    }
                },
                onScanTag = { navController.navigate(Routes.SCAN_TAG) },
            )
        }
        composable(Routes.SCAN_TAG) {
            ScanTagScreen(
                onBack = { navController.popBackStack() },
                onScanned = { scannedTag ->
                    val encoded = ScannedPayloadCodec.encode(scannedTag)
                    navController.navigate("profileForm/scanReview/$encoded") {
                        popUpTo(Routes.TYPE_PICKER) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.PROFILE_FORM_NEW,
            arguments = listOf(navArgument("template") { type = NavType.StringType }),
        ) {
            ProfileFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(
            route = Routes.PROFILE_FORM_EDIT,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            ProfileFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PROFILE_FORM_SCAN_REVIEW,
            arguments = listOf(navArgument("scannedTag") { type = NavType.StringType }),
        ) {
            ProfileFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(
            route = Routes.WRITE_TAG,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            WriteTagScreen(
                onBack = { navController.popBackStack() },
                onWritten = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(onBack = { navController.popBackStack() })
        }
    }
}
