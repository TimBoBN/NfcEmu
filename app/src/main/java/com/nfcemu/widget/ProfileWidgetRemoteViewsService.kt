package com.nfcemu.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileWidgetRemoteViewsService : RemoteViewsService() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ProfileWidgetRemoteViewsFactory(packageName) { profileRepository }
}

/**
 * Same "pinned first, then recently used" ordering as [com.nfcemu.ui.home.HomeViewModel]'s
 * quick-select row, so the widget mirrors what the app itself considers quick access.
 * Reads [ProfileRepository]'s hot StateFlow `.value` directly (no suspend calls) since
 * [RemoteViewsFactory] methods run synchronously on a widget binder thread - safe here
 * only because the repository is instantiated eagerly at process start (see
 * [com.nfcemu.NfcEmuApplication]), so its DataStore-backed state is already loaded by
 * the time any widget could possibly be visible to tap.
 */
private class ProfileWidgetRemoteViewsFactory(
    private val packageName: String,
    private val repositoryProvider: () -> ProfileRepository,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Profile> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val repository = repositoryProvider()
        val profiles = repository.profiles.value
        val activeId = repository.activeProfileId.value
        val pinned = profiles.filter { it.pinned }
        val recent = profiles
            .filter { !it.pinned && it.id != activeId }
            .sortedByDescending { it.lastUsedAt ?: it.createdAt }
            .take(MAX_RECENT)
        items = (pinned + recent).distinctBy { it.id }
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val profile = items[position]
        val views = RemoteViews(packageName, R.layout.widget_profile_item)
        views.setTextViewText(R.id.widget_item_name, profile.name)
        views.setImageViewResource(R.id.widget_item_icon, R.drawable.ic_widget_profile)
        views.setOnClickFillInIntent(
            R.id.widget_item_root,
            Intent().putExtra(ProfileWidgetClickReceiver.EXTRA_PROFILE_ID, profile.id),
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items[position].id.hashCode().toLong()
    override fun hasStableIds(): Boolean = true

    private companion object {
        const val MAX_RECENT = 8
    }
}
