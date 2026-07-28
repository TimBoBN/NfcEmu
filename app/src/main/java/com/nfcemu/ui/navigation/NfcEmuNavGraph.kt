package com.nfcemu.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nfcemu.ui.home.ManageQuickSelectScreen
import com.nfcemu.ui.library.LibraryScreen
import com.nfcemu.ui.myprofile.MyProfileScreen
import com.nfcemu.ui.onboarding.OnboardingScreen
import com.nfcemu.ui.onboarding.OnboardingViewModel
import com.nfcemu.ui.profileform.ProfileFormScreen
import com.nfcemu.ui.profileform.TypePickerScreen
import com.nfcemu.ui.profilelist.ProfileListScreen
import com.nfcemu.ui.receivecontact.ReceiveContactScreen
import com.nfcemu.ui.scantag.ScanTagScreen
import com.nfcemu.ui.scantag.ScannedPayloadCodec
import com.nfcemu.ui.settings.SettingsScreen
import com.nfcemu.ui.share.SharePreviewScreen
import com.nfcemu.ui.share.SharedTextCodec
import com.nfcemu.ui.theme.Motion
import com.nfcemu.ui.transmit.TransmitScreen
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
    const val TRANSMIT = "transmit"
    const val MY_PROFILE = "myProfile"
    const val RECEIVE_CONTACT = "receiveContact"
    const val SHARE_PREVIEW = "sharePreview/{sharedText}"
    const val MANAGE_QUICK_SELECT = "manageQuickSelect"
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
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedDecelerate),
    ) + fadeIn(tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedDecelerate))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedAccelerate),
    ) + fadeOut(tween(Motion.DURATION_SHORT, easing = Motion.emphasizedAccelerate))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedDecelerate),
    ) + fadeIn(tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedDecelerate))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.emphasizedAccelerate),
    ) + fadeOut(tween(Motion.DURATION_SHORT, easing = Motion.emphasizedAccelerate))

/**
 * [pendingSharedText] carries text handed to the app via the Android share sheet
 * (`ACTION_SEND`, see [com.nfcemu.ui.MainActivity]) - a one-shot event, not nav state, so it's
 * consumed via [onSharedTextConsumed] immediately after triggering navigation to keep it from
 * refiring on recomposition or process restoration.
 */
@Composable
fun NfcEmuNavGraph(
    pendingSharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingSharedText) {
        val text = pendingSharedText ?: return@LaunchedEffect
        navController.navigate("sharePreview/${SharedTextCodec.encode(text)}")
        onSharedTextConsumed()
    }

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
                    onNavigateToTransmit = { navController.navigate(Routes.TRANSMIT) },
                    onNavigateToScanTag = { navController.navigate(Routes.SCAN_TAG) },
                    onNavigateToMyProfile = { navController.navigate(Routes.MY_PROFILE) },
                    onNavigateToReceiveContact = { navController.navigate(Routes.RECEIVE_CONTACT) },
                    onNavigateToManageQuickSelect = { navController.navigate(Routes.MANAGE_QUICK_SELECT) },
                )
            }
        }
        composable(Routes.PROFILE_LIST) {
            ProfileListScreen(
                onBack = { navController.popBackStack() },
                onNewProfile = { navController.navigate(Routes.TYPE_PICKER) },
                onEditProfile = { profile -> navController.navigate("profileForm/edit/${profile.id}") },
                onWriteToTag = { profile -> navController.navigate("writeTag/${profile.id}") },
                onNavigateToTransmit = { navController.navigate(Routes.TRANSMIT) },
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
        composable(Routes.TRANSMIT) {
            TransmitScreen(onClose = { navController.popBackStack() })
        }
        composable(Routes.MY_PROFILE) {
            MyProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTransmit = { navController.navigate(Routes.TRANSMIT) },
            )
        }
        composable(Routes.RECEIVE_CONTACT) {
            ReceiveContactScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MANAGE_QUICK_SELECT) {
            ManageQuickSelectScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.SHARE_PREVIEW,
            arguments = listOf(navArgument("sharedText") { type = NavType.StringType }),
        ) {
            SharePreviewScreen(
                onBack = { navController.popBackStack() },
                onActivated = {
                    navController.navigate(Routes.TRANSMIT) {
                        popUpTo(Routes.SHARE_PREVIEW) { inclusive = true }
                    }
                },
            )
        }
    }
}
