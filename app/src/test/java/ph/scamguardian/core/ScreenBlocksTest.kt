package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenBlocksTest {
    @Test
    fun chat_keepsOnlyIncomingMessages() {
        val nodes =
            listOf(
                ScreenNode("Pahiram muna ng 5,000 urgent lang", centerX = 300, containerId = 1),
                ScreenNode("Sige, magkano ba kailangan mo?", centerX = 800, containerId = 2),
                ScreenNode("Send mo sa ibang number ko", centerX = 599, containerId = 3),
                ScreenNode("Nasa gitna lang ito ng screen", centerX = 600, containerId = 4),
            )

        assertEquals(
            listOf("Pahiram muna ng 5,000 urgent lang", "Send mo sa ibang number ko"),
            ScreenBlocks.chat(nodes, windowLeft = 0, windowWidth = 1000).map { it.text },
        )
    }

    @Test
    fun chat_measuresAgainstTheWindowNotTheScreen() {
        // A 1000 px screen split in two: the chat is in the right-hand 500 px pane.
        val nodes =
            listOf(
                ScreenNode("Incoming message in the right pane", centerX = 650, containerId = 1),
                ScreenNode("Outgoing message in the right pane", centerX = 900, containerId = 2),
                ScreenNode("Text from the other app on the left", centerX = 799, containerId = 3),
            )

        assertEquals(
            listOf("Incoming message in the right pane", "Text from the other app on the left"),
            ScreenBlocks.chat(nodes, windowLeft = 500, windowWidth = 500).map { it.text },
        )
        // The same chat in the left-hand pane: its outgoing messages are at 300 to 500 px.
        val leftPane =
            listOf(
                ScreenNode("Incoming message in the left pane", centerX = 150, containerId = 1),
                ScreenNode("Outgoing message in the left pane", centerX = 400, containerId = 2),
            )
        assertEquals(
            listOf("Incoming message in the left pane"),
            ScreenBlocks.chat(leftPane, windowLeft = 0, windowWidth = 500).map { it.text },
        )
    }

    @Test
    fun chat_skipsShortAndUiTextAndRepeats() {
        val nodes =
            listOf(
                "Seen",
                "Active now",
                "3:05 PM",
                "ok",
                "Juan dela Cruz",
                "  Bes emergency lang talaga  ",
                "Bes emergency lang talaga",
            ).mapIndexed { index, text -> ScreenNode(text, centerX = 100, containerId = index) }

        assertEquals(
            listOf("Juan dela Cruz", "Bes emergency lang talaga"),
            ScreenBlocks.chat(nodes, windowLeft = 0, windowWidth = 1000).map { it.text },
        )
    }

    @Test
    fun page_joinsTextInTheSameContainer() {
        val nodes =
            listOf(
                ScreenNode("Congratulations! You won a prize.", centerX = 500, containerId = 7),
                ScreenNode("Like", centerX = 100, containerId = 9),
                ScreenNode("Claim here: https://bit.ly/abc", centerX = 900, containerId = 7),
                ScreenNode("Comment", centerX = 500, containerId = 9),
                ScreenNode("Share", centerX = 900, containerId = 9),
                ScreenNode("Another post about the weather today", centerX = 500, containerId = 12),
            )

        assertEquals(
            listOf(
                "Congratulations! You won a prize.\nClaim here: https://bit.ly/abc",
                "Another post about the weather today",
            ),
            ScreenBlocks.page(nodes).map { it.text },
        )
    }

    @Test
    fun page_splitsAContainerIntoBlocksOfAboutAThousandCharacters() {
        val line = "word ".repeat(60).trim()
        val nodes = List(8) { ScreenNode(line, centerX = 500, containerId = 1) }

        val blocks = ScreenBlocks.page(List(8) { index -> nodes[index].copy(text = "$index $line") }).map { it.text }

        assertEquals(3, blocks.size)
        assertTrue(blocks.all { it.length <= 1000 })
    }

    @Test
    fun page_keepsOneLongTextWhole() {
        val long = "word ".repeat(400).trim()

        assertEquals(
            listOf(ScreenBlock(long)),
            ScreenBlocks.page(listOf(ScreenNode(long, centerX = 500, containerId = 1))),
        )
    }

    @Test
    fun blocks_knowWhereTheyAreOnScreen() {
        val message = ScreenNode("Pahiram muna ng 5,000", 300, 1, ScreenRect(40, 900, 560, 1000))
        val post = ScreenNode("Congratulations! You won a prize.", 500, 7, ScreenRect(20, 300, 1000, 380))
        val link = ScreenNode("Claim here: https://bit.ly/abc", 500, 7, ScreenRect(20, 400, 700, 460))

        assertEquals(
            listOf(ScreenBlock("Pahiram muna ng 5,000", ScreenRect(40, 900, 560, 1000))),
            ScreenBlocks.chat(listOf(message), windowLeft = 0, windowWidth = 1000),
        )
        // A joined block covers all of its parts.
        assertEquals(ScreenRect(20, 300, 1000, 460), ScreenBlocks.page(listOf(post, link)).single().bounds)
    }

    @Test
    fun chat_joinsAnAmountSentOnItsOwnToTheMessageBeforeIt() {
        val nodes =
            listOf(
                ScreenNode("Pahiram ako please now na", 300, 1, ScreenRect(40, 800, 560, 880)),
                ScreenNode("Magkano ba?", 800, 2, ScreenRect(600, 900, 960, 980)),
                ScreenNode("10k", 150, 3, ScreenRect(40, 1000, 200, 1080)),
                ScreenNode("Kumusta ka na pala", 300, 4, ScreenRect(40, 1100, 560, 1180)),
            )

        assertEquals(
            listOf(
                ScreenBlock("Pahiram ako please now na\n10k", ScreenRect(40, 800, 560, 1080)),
                ScreenBlock("Kumusta ka na pala", ScreenRect(40, 1100, 560, 1180)),
            ),
            ScreenBlocks.chat(nodes, windowLeft = 0, windowWidth = 1000),
        )
    }

    @Test
    fun chat_anAmountWithNothingBeforeIt_orALongerMessage_isNotJoined() {
        fun incoming(vararg texts: String) = texts.mapIndexed { index, text -> ScreenNode(text, 100, index) }

        assertEquals(
            emptyList<String>(),
            ScreenBlocks.chat(incoming("10k"), windowLeft = 0, windowWidth = 1000).map { it.text },
        )
        assertEquals(
            listOf("Kain na tayo mamaya", "Bayad ko yung 500 kahapon ha"),
            ScreenBlocks.chat(incoming("Kain na tayo mamaya", "Bayad ko yung 500 kahapon ha"), 0, 1000).map { it.text },
        )
    }

    @Test
    fun chat_dropsMessengersScreenReaderHints() {
        val hint = ", double tap to see sent/receive date and time, double tap and hold to react on message"
        val nodes =
            listOf(
                ScreenNode("Danward, Pahiram ako please now na$hint", centerX = 300, containerId = 1),
                ScreenNode("Danward, 10k$hint", centerX = 150, containerId = 2),
            )

        assertEquals(
            listOf("Danward, Pahiram ako please now na\nDanward, 10k"),
            ScreenBlocks.chat(nodes, windowLeft = 0, windowWidth = 1000).map { it.text },
        )
    }

    @Test
    fun isWorthChecking_skipsUiTextAndSingleWordsButNotShortMessagesOrLinks() {
        listOf(
            "Seen",
            "ACTIVE NOW",
            "Like",
            "Comment",
            "Share",
            "Reply",
            "3:05 PM",
            "12:30",
            "9:41am",
            "hello",
            "ok.",
            "",
        ).forEach { assertFalse(it, ScreenBlocks.isWorthChecking(it)) }
        listOf(
            "Send OTP",
            "GCash https://gcash-login.xyz",
            "gcash-win.cc",
            "https://bit.ly/abc",
            "bdo-bd0.cc/ph",
            "hello po kuya",
            "Meet at 3:05 PM today",
        ).forEach { assertTrue(it, ScreenBlocks.isWorthChecking(it)) }
    }
}
