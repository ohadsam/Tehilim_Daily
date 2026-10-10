package com.ohadsam.tehilim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ohadsam.tehilim.ui.nav.AppRoot
import com.ohadsam.tehilim.ui.theme.TehilimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TehilimApp).container
        setContent {
            // The app is Hebrew-only: always lay out right-to-left regardless of device locale.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TehilimTheme {
                    AppRoot(container)
                }
            }
        }
    }
}
