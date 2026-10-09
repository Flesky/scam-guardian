package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerLimiterTest {
    @Test
    fun tryShow_allowsOneBannerPer30SecondsPerApp() {
        val limiter = BannerLimiter()

        assertTrue(limiter.tryShow("com.whatsapp", 1_000))
        assertFalse(limiter.tryShow("com.whatsapp", 1_001))
        assertFalse(limiter.tryShow("com.whatsapp", 30_999))
        assertTrue(limiter.tryShow("com.whatsapp", 31_000))
        assertFalse(limiter.tryShow("com.whatsapp", 60_999))
    }

    @Test
    fun tryShow_countsEachAppOnItsOwn() {
        val limiter = BannerLimiter()

        assertTrue(limiter.tryShow("com.whatsapp", 1_000))
        assertTrue(limiter.tryShow("com.viber.voip", 1_001))
        assertFalse(limiter.tryShow("com.viber.voip", 2_000))
    }

    @Test
    fun tryShow_aRefusedBannerDoesNotRestartTheWait() {
        val limiter = BannerLimiter(intervalMs = 100)

        assertTrue(limiter.tryShow("app", 0))
        assertFalse(limiter.tryShow("app", 99))
        assertTrue(limiter.tryShow("app", 100))
    }

    @Test
    fun isOpen_doesNotUseUpTheTurn() {
        val limiter = BannerLimiter(intervalMs = 100)

        assertTrue(limiter.isOpen("app", 0))
        assertTrue(limiter.tryShow("app", 0))
        assertFalse(limiter.isOpen("app", 50))
        assertTrue(limiter.isOpen("app", 100))
    }

    @Test
    fun unlimited_alwaysAllowsABanner() {
        val limiter = BannerLimiter(intervalMs = 30_000)

        assertTrue(limiter.tryShow("com.whatsapp", nowMs = 1_000, unlimited = true))
        assertTrue(limiter.tryShow("com.whatsapp", nowMs = 1_001, unlimited = true))
        assertTrue(limiter.isOpen("com.whatsapp", nowMs = 1_002, unlimited = true))
        // Back in normal mode the last demo banner still counts.
        assertFalse(limiter.tryShow("com.whatsapp", nowMs = 2_000))
    }

    @Test
    fun remainingMs_countsDownToTheNextAllowedBanner() {
        val limiter = BannerLimiter(intervalMs = 30_000)

        assertEquals(0, limiter.remainingMs("com.whatsapp", nowMs = 1_000))
        limiter.tryShow("com.whatsapp", nowMs = 1_000)

        assertEquals(30_000, limiter.remainingMs("com.whatsapp", nowMs = 1_000))
        assertEquals(10_000, limiter.remainingMs("com.whatsapp", nowMs = 21_000))
        assertEquals(0, limiter.remainingMs("com.whatsapp", nowMs = 40_000))
        assertEquals(0, limiter.remainingMs("com.viber.voip", nowMs = 2_000))
        assertEquals(0, limiter.remainingMs("com.whatsapp", nowMs = 2_000, unlimited = true))
    }
}
