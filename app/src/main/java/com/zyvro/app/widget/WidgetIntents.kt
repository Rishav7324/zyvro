package com.zyvro.app.widget

import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import com.zyvro.app.ui.MainActivity

/**
 * Shared widget navigation. Tab is delivered to MainActivity as an intent
 * extra named "zyvro_tab" (Glance maps action parameters to extras), which
 * MainActivity already reads via EXTRA_WIDGET_TAB.
 */
object WidgetIntents {
    const val EXTRA_TAB = "zyvro_tab"
    private val TabKey = ActionParameters.Key<String>(EXTRA_TAB)

    fun openTabAction(tab: String): Action =
        actionStartActivity<MainActivity>(
            parameters = actionParametersOf(TabKey to tab)
        )

    fun openAppAction(): Action = openTabAction("home")
}
