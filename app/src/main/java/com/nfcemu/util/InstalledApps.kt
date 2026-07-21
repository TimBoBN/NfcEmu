package com.nfcemu.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A launchable app, for the AAR "bind to a specific app" package picker. */
data class InstalledApp(
    val label: String,
    val packageName: String,
)

/** Narrow, mockable contract so ViewModels don't depend on Android's PackageManager directly. */
interface InstalledAppsSource {
    suspend fun queryLaunchableApps(): List<InstalledApp>
}

@Singleton
class PackageManagerInstalledAppsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : InstalledAppsSource {

    override suspend fun queryLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)

        resolved
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString() ?: packageName
                InstalledApp(label = label, packageName = packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
