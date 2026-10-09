package com.aliaygor.taptoflip
import org.junit.Assert.*
import org.junit.Test
class ReminderLanguageTest {
    @Test fun turkeyRegionOrTurkishLanguageUsesTurkish() {
        assertTrue(ReminderPolicy.useTurkish("en", "TR"))
        assertTrue(ReminderPolicy.useTurkish("tr", "DE"))
        assertFalse(ReminderPolicy.useTurkish("en", "US"))
        assertFalse(ReminderPolicy.useTurkish("de", "DE"))
    }
}
