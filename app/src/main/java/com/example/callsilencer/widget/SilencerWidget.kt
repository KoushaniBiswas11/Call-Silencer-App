package com.example.callsilencer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.callsilencer.R
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.service.DndManager

class SilencerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.callsilencer.WIDGET_TOGGLE"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SilencerWidget::class.java)
            )
            val intent = Intent(context, SilencerWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val repository = CallSilencerRepository(context)
            val newState = !repository.isSilencerActive()
            repository.setSilencerActive(newState)

            // Update DND
            DndManager(context).setDnd(newState)

            // Refresh widget
            updateAllWidgets(context)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val repository = CallSilencerRepository(context)
        val isActive = repository.isSilencerActive()

        val views = RemoteViews(context.packageName, R.layout.widget_silencer)

        // Update status text
        views.setTextViewText(
            R.id.widget_status,
            if (isActive) "🔕 Active" else "🔔 Inactive"
        )

        // Update button text
        views.setTextViewText(
            R.id.widget_toggle_btn,
            if (isActive) "Turn OFF" else "Turn ON"
        )

        // Update background color
        views.setInt(
            R.id.widget_container,
            "setBackgroundColor",
            if (isActive)
                android.graphics.Color.parseColor("#1A1F3C")
            else
                android.graphics.Color.parseColor("#252B4A")
        )

        // Toggle button click
        val toggleIntent = Intent(context, SilencerWidget::class.java).apply {
            action = ACTION_TOGGLE
        }
        val togglePending = PendingIntent.getBroadcast(
            context, 0, toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_toggle_btn, togglePending)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}