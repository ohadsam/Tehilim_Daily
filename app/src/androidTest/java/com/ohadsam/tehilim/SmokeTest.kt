package com.ohadsam.tehilim

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/** End-to-end smoke test on a real emulator: the app starts, shows the five tabs, and the main screens open. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(res: Int) = rule.activity.getString(res)

    @Test
    fun bottomNavigationShowsAllTabs() {
        listOf(R.string.tab_today, R.string.tab_browse, R.string.tab_favorites, R.string.tab_reminders, R.string.tab_more).forEach {
            rule.onAllNodesWithText(text(it)).onFirst().assertExists()
        }
    }

    @Test
    fun settingsOpensFromMore() {
        rule.onAllNodesWithText(text(R.string.tab_more)).onFirst().performClick()
        rule.onAllNodesWithText(text(R.string.set_title)).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText(text(R.string.set_appearance)).assertCountEquals(1)
    }

    @Test
    fun favoritesAndRemindersTabsOpen() {
        rule.onAllNodesWithText(text(R.string.tab_favorites)).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText(text(R.string.fav_tab_refuah)).onFirst().assertExists()
        rule.onAllNodesWithText(text(R.string.tab_reminders)).onFirst().performClick()
        rule.waitForIdle()
        try {
            rule.waitUntil(5_000) { rule.onAllNodesWithText(text(R.string.rem_new), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            rule.onRoot().printToLog("TehilimTest") // dump the semantics tree to logcat for diagnosis
            throw e
        }
        rule.onAllNodesWithText(text(R.string.rem_new), useUnmergedTree = true).onFirst().assertExists()
    }

    /** Not an assertion: captures each main screen (half size, JPEG, base64 in logcat) so the UI can be reviewed from CI logs. */
    @Test
    fun captureScreens() {
        fun shot(name: String) {
            rule.waitForIdle()
            val full = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val small = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
            val out = ByteArrayOutputStream()
            small.compress(Bitmap.CompressFormat.JPEG, 75, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            b64.chunked(3000).forEachIndexed { i, chunk -> Log.i("TehilimShot", "$name|$i|$chunk") }
            Log.i("TehilimShot", "$name|END|")
        }

        runCatching { rule.waitUntil(20_000) { rule.onAllNodesWithText("פרק", substring = true).fetchSemanticsNodes().size > 1 } }
        shot("01_today")
        listOf(
            R.string.tab_browse to "02_browse",
            R.string.tab_favorites to "03_favorites",
            R.string.tab_reminders to "04_reminders",
            R.string.tab_more to "05_more",
        ).forEach { (res, name) ->
            rule.onAllNodesWithText(text(res)).onFirst().performClick()
            if (res == R.string.tab_more) {
                shot("05a_more_whatsnew")
                runCatching { rule.onAllNodesWithText(text(R.string.whats_new_close)).onFirst().performClick() }
            }
            runCatching { rule.waitUntil(8_000) { rule.onAllNodesWithText("פרק", substring = true).fetchSemanticsNodes().isNotEmpty() || res != R.string.tab_browse } }
            shot(name)
        }
        rule.onAllNodesWithText(text(R.string.set_title)).onFirst().performClick()
        shot("06_settings")
    }

    private fun shot2(name: String) {
        rule.waitForIdle()
        val full = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val small = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
        val out = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, 75, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP).chunked(3000).forEachIndexed { i, chunk -> Log.i("TehilimShot", "$name|$i|$chunk") }
    }

    /** Creates a favorite with a chapter range, then a reminder for it — exercises both editor sheets end to end. */
    @Test
    fun createFavoriteThenReminder() {
        // Dismiss the What's New dialog if it is showing (first launch of a version).
        rule.onAllNodesWithText(text(R.string.tab_more)).onFirst().performClick()
        runCatching { rule.onAllNodesWithText(text(R.string.whats_new_close)).onFirst().performClick() }

        rule.onAllNodesWithText(text(R.string.tab_favorites)).onFirst().performClick()
        rule.onAllNodesWithText(text(R.string.action_add), useUnmergedTree = true).onFirst().performClick()
        rule.onAllNodesWithText(text(R.string.fav_new_favorite)).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("ישועה")
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("20-24")
        shot2("07_favorite_editor")
        rule.onAllNodesWithText(text(R.string.action_save)).onFirst().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("ישועה").fetchSemanticsNodes().isNotEmpty() }
        shot2("08_favorite_saved")

        rule.onAllNodesWithText(text(R.string.tab_reminders)).onFirst().performClick()
        rule.onAllNodesWithText(text(R.string.rem_new), useUnmergedTree = true).onFirst().performClick()
        rule.waitForIdle()
        shot2("09_reminder_editor")
        rule.onAllNodesWithText(text(R.string.rem_section_target)).onFirst().assertExists()
        rule.onAllNodesWithText(text(R.string.action_save)).onFirst().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text(R.string.rem_off)).fetchSemanticsNodes().isEmpty() && rule.onAllNodesWithText("תהילים", substring = true).fetchSemanticsNodes().isNotEmpty() }
        shot2("10_reminder_saved")
    }
}
