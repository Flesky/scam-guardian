package ph.scamguardian

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionLevelTest {
    @Test
    fun of_isOffUnlessTheButtonAndTheServiceAreBothOn() {
        EngineState.entries.forEach { engine ->
            assertEquals(ProtectionLevel.OFF, ProtectionLevel.of(isOn = false, serviceEnabled = true, engine))
            assertEquals(ProtectionLevel.OFF, ProtectionLevel.of(isOn = true, serviceEnabled = false, engine))
        }
    }

    @Test
    fun of_whenOn_followsTheEngine() {
        val expected =
            mapOf(
                EngineState.IDLE to ProtectionLevel.STARTING,
                EngineState.LOADING to ProtectionLevel.STARTING,
                EngineState.READY to ProtectionLevel.FULL,
                EngineState.LIMITED to ProtectionLevel.LIMITED,
                EngineState.FAILED to ProtectionLevel.UNAVAILABLE,
            )

        expected.forEach { (engine, level) ->
            assertEquals(level, ProtectionLevel.of(isOn = true, serviceEnabled = true, engine))
        }
    }
}
