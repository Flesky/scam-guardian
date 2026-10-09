package ph.scamguardian.core

import com.ibm.icu.text.SpoofChecker

/** Skeletons of one word, used to compare look-alike spellings. */
data class ConfusableForms(
    val exact: String,
    val upper: String,
    val lower: Set<String>,
) {
    /** True when this (possibly spoofed) word looks like the genuine [target] word. */
    fun looksLike(target: ConfusableForms): Boolean =
        exact == target.exact || exact == target.upper || lower.any { it in target.lower }
}

/** Detects look-alike characters ("BPl" for "BPI", "bd0" for "bdo", Cyrillic letters) with ICU confusable skeletons. */
class Confusables {
    private val checker = SpoofChecker.Builder().build()

    fun skeleton(text: String): String = checker.getSkeleton(text)

    /** Skeletons of the original-case text and of the lowercase text. */
    fun forms(text: String): ConfusableForms {
        val exact = skeleton(text)
        return ConfusableForms(
            exact = exact,
            upper = skeleton(text.uppercase()),
            lower = setOf(exact.lowercase(), skeleton(text.lowercase())),
        )
    }

    fun matches(
        text: String,
        target: String,
    ): Boolean = forms(text).looksLike(forms(target))
}
