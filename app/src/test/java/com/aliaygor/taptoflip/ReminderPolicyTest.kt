package com.aliaygor.taptoflip

import org.junit.Assert.*
import org.junit.Test

class ReminderPolicyTest {
    private val now = 30 * ReminderPolicy.DAY
    @Test fun neverRemindsAnActivePlayer() {
        assertFalse(ReminderPolicy.shouldSend(now, now - 3 * ReminderPolicy.DAY, 0, 0, 12))
        assertFalse(ReminderPolicy.shouldSend(now, 0, 0, 0, 12))
    }
    @Test fun firstReminderNeedsFourDaysAndDaytime() {
        assertTrue(ReminderPolicy.shouldSend(now, now - 4 * ReminderPolicy.DAY, 0, 0, 10))
        assertFalse(ReminderPolicy.shouldSend(now, now - 4 * ReminderPolicy.DAY, 0, 0, 9))
        assertFalse(ReminderPolicy.shouldSend(now, now - 4 * ReminderPolicy.DAY, 0, 0, 20))
    }
    @Test fun secondReminderWaitsSevenDaysAndThenStops() {
        assertFalse(ReminderPolicy.shouldSend(now, now - 20 * ReminderPolicy.DAY,
            now - 6 * ReminderPolicy.DAY, 1, 12))
        assertTrue(ReminderPolicy.shouldSend(now, now - 20 * ReminderPolicy.DAY,
            now - 7 * ReminderPolicy.DAY, 1, 12))
        assertFalse(ReminderPolicy.shouldSend(now, now - 20 * ReminderPolicy.DAY, 0, 2, 12))
    }
    @Test fun returningDoesNotCauseAnImmediateRepeat() {
        assertFalse(ReminderPolicy.shouldSend(now, now - 4 * ReminderPolicy.DAY,
            now - 2 * ReminderPolicy.DAY, 0, 12))
        assertFalse(ReminderPolicy.shouldSend(now, now + ReminderPolicy.DAY, 0, 0, 12))
    }
}
