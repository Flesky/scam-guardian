package ph.scamguardian.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentBlocksTest {
    @Test
    fun add_isTrueOnlyTheFirstTime() {
        val sent = SentBlocks()

        assertTrue(sent.add("com.whatsapp", "Pahiram muna ng 5,000"))
        assertFalse(sent.add("com.whatsapp", "Pahiram muna ng 5,000"))
        assertTrue(sent.add("com.viber.voip", "Pahiram muna ng 5,000"))
    }

    @Test
    fun add_keepsApartTextsWithTheSameHashCode() {
        val greeting = "Hello kuya kumusta ka"
        val otpRequest = "Please send your OTP to me br32eED"
        val sent = SentBlocks()

        assertEquals("com.whatsapp\n$greeting".hashCode(), "com.whatsapp\n$otpRequest".hashCode())
        assertTrue(sent.add("com.whatsapp", greeting))
        assertTrue(sent.add("com.whatsapp", otpRequest))
    }

    @Test
    fun forget_makesTheBlockNewAgain() {
        val sent = SentBlocks()
        sent.add("com.whatsapp", "Pahiram muna ng 5,000")

        sent.forget("com.whatsapp", "Pahiram muna ng 5,000")

        assertTrue(sent.add("com.whatsapp", "Pahiram muna ng 5,000"))
    }

    @Test
    fun add_forgetsTheOldestBlockPastTheCapacity() {
        val sent = SentBlocks(capacity = 2)
        sent.add("app", "first message here")
        sent.add("app", "second message here")
        sent.add("app", "third message here")

        assertTrue(sent.add("app", "first message here"))
        assertFalse(sent.add("app", "third message here"))
    }

    @Test
    fun clear_makesEveryBlockNewAgain() {
        val sent = SentBlocks()
        sent.add("com.whatsapp", "first message here")
        sent.add("com.viber.voip", "second message here")

        sent.clear()

        assertTrue(sent.add("com.whatsapp", "first message here"))
        assertTrue(sent.add("com.viber.voip", "second message here"))
    }
}
