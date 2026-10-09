package ph.scamguardian.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotMotionTest {
    private fun assertInPlace(pose: PlatePose) {
        assertEquals(0f, pose.dx, 0f)
        assertEquals(0f, pose.dy, 0f)
        assertEquals(0f, pose.degrees, 0f)
        assertEquals(1f, pose.alpha, 0f)
    }

    @Test
    fun assemble_endsWithEveryPlateInPlaceAndTheEyesOn() {
        val move = MascotMove.ASSEMBLE

        Plate.entries.forEach { assertInPlace(move.plate(it, move.durationMs)) }
        assertEquals(EyeLook(alpha = 1f, flash = 0f), move.eye(move.durationMs))
        assertEquals(0f, move.jolt(move.durationMs))
        assertEquals(0f, move.ring(move.durationMs).alpha)
    }

    @Test
    fun assemble_startsFromTheBareShell() {
        val move = MascotMove.ASSEMBLE

        Plate.entries.forEach { assertEquals(0f, move.plate(it, 0f).alpha) }
        assertEquals(0f, move.eye(0f).alpha)
    }

    @Test
    fun assemble_platesComeFromTheirSideAndTheEyesWaitForThem() {
        val move = MascotMove.ASSEMBLE
        val lastLockMs = Plate.entries.maxOf { it.lockAtMs }

        // Partway along its rail the cheek is still to the left of its place, and the crown above it.
        assertTrue(move.plate(Plate.CHEEK, Plate.CHEEK.lockAtMs + 30f).dx < 0f)
        assertTrue(move.plate(Plate.CROWN, Plate.CROWN.lockAtMs + 30f).dy < 0f)
        assertEquals(0f, move.eye(lastLockMs).alpha)
    }

    @Test
    fun disassemble_switchesTheEyesOffFirstAndEndsBare() {
        val move = MascotMove.DISASSEMBLE

        assertEquals(0f, move.eye(0f).alpha)
        Plate.entries.forEach { assertInPlace(move.plate(it, 0f)) }
        Plate.entries.forEach { assertEquals(0f, move.plate(it, move.durationMs).alpha) }
    }

    @Test
    fun disassemble_platesFallDownAndTumble() {
        val falling = MascotMove.DISASSEMBLE.plate(Plate.JAW, Plate.JAW.fallAtMs + 200f)

        assertTrue(falling.dy > 0f)
        assertTrue(falling.degrees != 0f)
    }

    @Test
    fun scanLine_sweepsDownTheEyesThenHidesUntilTheNextPeriod() {
        val early = scanLine(0.1f)
        val later = scanLine(0.2f)

        assertEquals(0f, scanLine(0f).alpha, 0f)
        assertTrue(early.alpha > 0f)
        assertTrue(later.y > early.y)
        assertEquals(0f, scanLine(0.5f).alpha, 0f)
        assertEquals(0f, scanLine(1f).alpha, 0f)
    }
}
