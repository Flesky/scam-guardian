package ph.scamguardian.core

import org.junit.Test

/** Prints how many real scams the rules catch and how many safe messages they flag. */
class RuleCoverageTest {
    @Test
    fun printRuleCoverageAndFalseAlarms() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val scams = Fixtures.realScams()
        val safe = Fixtures.safeMessages()

        val caught = scams.mapNotNull { pipeline.check(it.text)?.type?.key }
        val falseAlarms = safe.filter { pipeline.check(it) != null }

        println("Rule coverage on real scams: ${caught.size}/${scams.size}")
        caught.groupingBy { it }.eachCount().forEach { (type, count) -> println("  $type: $count") }
        println("False alarms on safe messages: ${falseAlarms.size}/${safe.size}")
        falseAlarms.forEach { println("  $it") }
    }
}
