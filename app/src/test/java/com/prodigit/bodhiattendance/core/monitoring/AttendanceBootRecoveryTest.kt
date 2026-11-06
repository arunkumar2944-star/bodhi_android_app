package com.prodigit.bodhiattendance.core.monitoring

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceBootRecoveryTest {

    @Test
    fun validBootAndPackageUpdateActionsAreAccepted() {
        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
        for (action in validActions) {
            assertTrue(
                "Action $action should be recognized for boot recovery",
                action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    action == "android.intent.action.QUICKBOOT_POWERON"
            )
        }
    }

    @Test
    fun unrelatedActionsAreRejected() {
        val invalidActions = listOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_BATTERY_LOW,
            "com.example.CUSTOM_ACTION"
        )
        for (action in invalidActions) {
            assertFalse(
                "Action $action should not trigger boot recovery",
                action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    action == "android.intent.action.QUICKBOOT_POWERON"
            )
        }
    }
}
