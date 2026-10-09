package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyDetectorTest {
    @Test
    fun find_detectsEachMoneyFormat() {
        val cases =
            mapOf(
                "You have received P4700.00 from SSS" to listOf("p4700.00"),
                "regalong handog na P888: www" to listOf("p888"),
                "kuhanin ang P157+128 na bonus" to listOf("p157"),
                "PHP 1,688 Play or withdraw" to listOf("php 1,688"),
                "Earn up to ₱10,000. Join now" to listOf("₱10,000"),
                "redeemed for 3,000 pesos." to listOf("3,000 pesos"),
                "Big winner of 865.3K on Pinata" to listOf("865.3k"),
                "our 30K top up gift" to listOf("30k"),
                "padalhan mo ako 5kyaw" to listOf("5kyaw"),
                "pahiram 5 kyaw" to listOf("5 kyaw"),
                "get 18P bonos" to listOf("18p"),
                "your 6,225 points will expire" to listOf("6,225 points"),
                "6,552 pts will clear" to listOf("6,552 pts"),
                "reward points (5,980) will expire" to listOf("points (5,980"),
                "win P50K vouchers" to listOf("p50k"),
            )

        cases.forEach { (text, expected) -> assertEquals(text, expected, MoneyDetector.find(text)) }
    }

    @Test
    fun find_ignoresTextWithoutMoney() {
        val cases =
            listOf(
                "Meeting moved to 3pm tomorrow",
                "smart kid si Juan, top 1 sa klase",
                "p4dala na po agad",
                "arriving in 3 minutes",
                "Your OTP is 482913",
                "filename 5kyawphoto",
                "filename abc5kyaw",
            )

        cases.forEach { text -> assertEquals(text, emptyList<String>(), MoneyDetector.find(text)) }
    }

    @Test
    fun hasAmount_alsoAcceptsPlainNumbersThatReadAsAmounts() {
        listOf("10k", "Pahiram muna 5,000 urgent lang", "send 500 please", "P1,000 lang", "kahit 10000")
            .forEach { assertTrue(it, MoneyDetector.hasAmount(it)) }
        listOf("Pahiram ng charger", "3 pm tayo", "10:30 na", "10-05-26", "0917 kasi", "top 1 sa klase", "v2.5")
            .forEach { assertFalse(it, MoneyDetector.hasAmount(it)) }
    }
}
