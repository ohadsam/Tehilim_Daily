package com.ohadsam.tehilim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ohadsam.tehilim.share.IncomingShare
import com.ohadsam.tehilim.ui.nav.AppRoot
import com.ohadsam.tehilim.ui.theme.TehilimTheme

class MainActivity : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
        handleIncomingShare(intent)
    }

    /** A `.tehilim` file opened from WhatsApp / mail, or a message forwarded to the app: read it off the main thread and hand it to the sharing screen. */
    private fun handleIncomingShare(intent: Intent?) {
        if (!IncomingShare.isCandidate(intent) || intent == null) return
        val container = (application as TehilimApp).container
        kotlin.concurrent.thread(name = "incoming-share") {
            val text = IncomingShare.readText(applicationContext, intent)
            if (!text.isNullOrBlank()) runOnUiThread { container.shareInbox.value = text }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = intent?.getStringExtra(EXTRA_ROUTE)
        if (savedInstanceState == null) handleIncomingShare(intent)
        enableEdgeToEdge()
        val container = (application as TehilimApp).container
        setContent {
            // The app is Hebrew-only: always lay out right-to-left regardless of device locale.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val themeSettings by container.themePrefs.settings.collectAsState()
                TehilimTheme(themeSettings) {
                    AppRoot(container, pendingRoute.value, onRouteHandled = { pendingRoute.value = null })
                }
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}
