package com.aliaygor.taptoflip

import org.junit.Assert.*
import org.junit.Test

class ReminderPolicyTest {
    private val now = 30 * ReminderPolicy.DAY
    @Test fun newSchedulesAndUnknownStartAreNotReminded() {
        assertFalse(ReminderPolicy.shouldSend(now, now - ReminderPolicy.DAY, 0, 12))
        assertFalse(ReminderPolicy.shouldSend(now, now, 0, 12))
        assertFalse(ReminderPolicy.shouldSend(now, 0, 0, 12))
    }
    @Test fun firstReminderRequiresExactlyTwoDays() {
        assertFalse(ReminderPolicy.shouldSend(now, now - ReminderPolicy.INTERVAL + 1, 0, 12))
        assertTrue(ReminderPolicy.shouldSend(now, now - ReminderPolicy.INTERVAL, 0, 12))
    }
    @Test fun remindersAreOnlySentDuringDaytime() {
        val lastVisit = now - 3 * ReminderPolicy.DAY
        assertFalse(ReminderPolicy.shouldSend(now, lastVisit, 0, 9))
        assertTrue(ReminderPolicy.shouldSend(now, lastVisit, 0, 10))
        assertTrue(ReminderPolicy.shouldSend(now, lastVisit, 0, 19))
        assertFalse(ReminderPolicy.shouldSend(now, lastVisit, 0, 20))
    }
    @Test fun subsequentRemindersRequire48HoursWithoutDuplicates() {
        val lastVisit = now - 20 * ReminderPolicy.DAY
        assertFalse(ReminderPolicy.shouldSend(now, lastVisit, now, 12))
        assertFalse(ReminderPolicy.shouldSend(now, lastVisit, now - ReminderPolicy.INTERVAL + 1, 12))
        assertTrue(ReminderPolicy.shouldSend(now, lastVisit, now - ReminderPolicy.INTERVAL, 12))
    }
    @Test fun visitsDoNotResetCadenceAndFutureClockValuesDoNotSend() {
        assertTrue(ReminderPolicy.shouldSend(now, now - ReminderPolicy.DAY, now - 5 * ReminderPolicy.DAY, 12))
        assertFalse(ReminderPolicy.shouldSend(now, now + ReminderPolicy.DAY, 0, 12))
        assertFalse(ReminderPolicy.shouldSend(now, now - 5 * ReminderPolicy.DAY, now + ReminderPolicy.DAY, 12))
    }
}
