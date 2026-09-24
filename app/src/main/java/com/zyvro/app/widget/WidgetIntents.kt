package com.zyvro.app.widget

import android.content.Context
import android.content.Intent
import com.zyvro.app.ui.MainActivity

object WidgetIntents {
    const val EXTRA_TAB = "zyvro_tab"

    fun openTab(context: Context, tab: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TAB, tab)
        }

    fun openApp(context: Context): Intent = openTab(context, "home")
}
