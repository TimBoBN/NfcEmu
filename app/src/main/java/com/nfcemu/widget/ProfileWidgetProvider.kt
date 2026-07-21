package com.nfcemu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.nfcemu.R
import com.nfcemu.data.ProfileRepository
import com.nfcemu.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val activeName = profileRepository.profiles.value
            .find { it.id == profileRepository.activeProfileId.value }
            ?.name
        for (widgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId, activeName))
        }
    }

    companion object {
        /** Called by [ProfileWidgetUpdater] whenever the active profile or the profile list changes. */
        fun refreshAll(context: Context, activeProfileName: String?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ProfileWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (widgetId in ids) {
                manager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId, activeProfileName))
            }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun buildRemoteViews(context: Context, widgetId: Int, activeProfileName: String?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_profile_list)

            views.setTextViewText(
                R.id.widget_active_label,
                activeProfileName?.let { context.getString(R.string.widget_active_prefix, it) }
                    ?: context.getString(R.string.widget_no_active),
            )

            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_active_label, openAppIntent)

            val serviceIntent = Intent(context, ProfileWidgetRemoteViewsService::class.java).apply {
                data = android.net.Uri.parse("nfcemu://widget/$widgetId")
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)
            views.setTextViewText(R.id.widget_empty, context.getString(R.string.widget_empty))
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)

            val clickTemplateIntent = Intent(context, ProfileWidgetClickReceiver::class.java)
            val clickTemplatePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                clickTemplateIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setPendingIntentTemplate(R.id.widget_list, clickTemplatePendingIntent)

            return views
        }
    }
}
