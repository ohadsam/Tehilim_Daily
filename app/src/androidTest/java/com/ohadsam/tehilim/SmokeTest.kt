package com.ohadsam.tehilim

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        rule.onAllNodesWithText(text(R.string.rem_new)).onFirst().assertExists()
    }
}
