package com.ohadsam.tehilim.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionSummaryTest {
    private fun item(id: PermissionId, ok: Boolean, req: Boolean) = PermissionItem(id, ok, req)

    @Test
    fun separatesRequiredFromOptional() {
        val s = PermissionSummary(
            listOf(
                item(PermissionId.NOTIFICATIONS, false, true),
                item(PermissionId.EXACT_ALARMS, true, true),
                item(PermissionId.BATTERY, false, false),
                item(PermissionId.LOCATION, true, false),
            ),
        )
        assertEquals(listOf(PermissionId.NOTIFICATIONS), s.missingRequired.map { it.id })
        assertEquals(listOf(PermissionId.BATTERY), s.missingOptional.map { it.id })
        assertFalse(s.allGood)
    }

    @Test
    fun allGoodWhenEverythingGranted() {
        assertTrue(PermissionSummary(listOf(item(PermissionId.NOTIFICATIONS, true, true))).allGood)
    }
}
