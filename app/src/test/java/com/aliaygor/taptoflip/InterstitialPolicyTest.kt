package com.aliaygor.taptoflip

import org.junit.Assert.*
import org.junit.Test

class InterstitialPolicyTest {
    @Test fun firstAdDoesNotRequireTwoMinuteWait() {
        val policy = InterstitialPolicy()
        repeat(2) { policy.roundCompleted() }
        assertFalse(policy.canShow(10_000))
        policy.roundCompleted()
        assertTrue(policy.canShow(12_000))
    }
    @Test fun missingAdDoesNotLoseEligibleRound() {
        val policy = InterstitialPolicy()
        repeat(3) { policy.roundCompleted() }
        assertTrue(policy.canShow(10_000))
        policy.roundCompleted()
        assertTrue(policy.canShow(15_000))
    }
    @Test fun actualImpressionResetsRoundsAndEnforcesCooldown() {
        val policy = InterstitialPolicy()
        repeat(3) { policy.roundCompleted() }
        policy.shown(10_000)
        assertFalse(policy.canShow(60_000))
        repeat(3) { policy.roundCompleted() }
        assertFalse(policy.canShow(54_999))
        assertTrue(policy.canShow(55_000))
    }
}
