package com.aliaygor.taptoflip
import org.junit.Assert.*
import org.junit.Test
class ReviewPolicyTest {
    @Test fun waitsForExperienceAndDoesNotRepeatFrequently() {
        val now = 200 * ReviewPolicy.DAY
        assertFalse(ReviewPolicy.eligible(4, now, now - 4*ReviewPolicy.DAY, 0))
        assertFalse(ReviewPolicy.eligible(5, now, now - 2*ReviewPolicy.DAY, 0))
        assertTrue(ReviewPolicy.eligible(5, now, now - 3*ReviewPolicy.DAY, 0))
        assertFalse(ReviewPolicy.eligible(5, now, now - 100*ReviewPolicy.DAY, now - 89*ReviewPolicy.DAY))
        assertTrue(ReviewPolicy.eligible(5, now, now - 100*ReviewPolicy.DAY, now - 90*ReviewPolicy.DAY))
    }
}
