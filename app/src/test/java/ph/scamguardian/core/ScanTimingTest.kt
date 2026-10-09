package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanTimingTest {
    private val timing = ScanTiming(pauseAfterChangeMs = 1_500, pauseAfterScrollMs = 3_000)

    @Test
    fun delayAfterEvent_contentChange_waitsTheShortPause() {
        assertEquals(1_500, timing.delayAfterEvent(nowMs = 10_000, isScroll = false))
    }

    @Test
    fun delayAfterEvent_scroll_waitsTheLongPause() {
        assertEquals(3_000, timing.delayAfterEvent(nowMs = 10_000, isScroll = true))
    }

    @Test
    fun delayAfterEvent_contentChangeRightAfterAScroll_stillWaitsOutTheScrollPause() {
        timing.delayAfterEvent(nowMs = 10_000, isScroll = true)

        // Lists report content changes while they scroll; these must not shorten the wait.
        assertEquals(2_800, timing.delayAfterEvent(nowMs = 10_200, isScroll = false))
        assertEquals(1_500, timing.delayAfterEvent(nowMs = 11_500, isScroll = false))
    }

    @Test
    fun delayAfterEvent_longAfterAScroll_isBackToTheShortPause() {
        timing.delayAfterEvent(nowMs = 10_000, isScroll = true)

        assertEquals(1_500, timing.delayAfterEvent(nowMs = 20_000, isScroll = false))
    }

    @Test
    fun delayAfterEvent_everyScrollRestartsTheLongPause() {
        timing.delayAfterEvent(nowMs = 10_000, isScroll = true)

        assertEquals(3_000, timing.delayAfterEvent(nowMs = 12_900, isScroll = true))
    }
}
