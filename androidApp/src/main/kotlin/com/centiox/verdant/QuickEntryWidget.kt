package com.centiox.verdant

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb

class QuickEntryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.quick_entry_widget)
            views.setTextColor(
                R.id.widget_title,
                themeAccent(
                        context
                            .getSharedPreferences("verdant-native", Context.MODE_PRIVATE)
                            .getString("accent", DEFAULT_BACKGROUND) ?: DEFAULT_BACKGROUND
                    )
                    .toArgb(),
            )
            listOf("expense" to R.id.widget_expense, "income" to R.id.widget_income)
                .forEachIndexed { index, (type, view) ->
                    val intent =
                        Intent(context, MainActivity::class.java)
                            .setAction("com.centiox.verdant.QUICK_$type")
                            .putExtra("quickAdd", type)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                    views.setOnClickPendingIntent(
                        view,
                        PendingIntent.getActivity(
                            context,
                            index + 50,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QuickEntryWidget::class.java))
            if (ids.isNotEmpty()) QuickEntryWidget().onUpdate(context, manager, ids)
        }

        fun pin(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported)
                manager.requestPinAppWidget(
                    ComponentName(context, QuickEntryWidget::class.java),
                    null,
                    null,
                )
            else
                Toast.makeText(
                        context,
                        "Add Verdant from your launcher's widgets list.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
        }
    }
}
