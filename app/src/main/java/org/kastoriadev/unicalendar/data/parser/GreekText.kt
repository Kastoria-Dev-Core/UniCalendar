package org.kastoriadev.unicalendar.data.parser

import java.text.Normalizer
import java.time.DayOfWeek

/** Text helpers for matching Greek (and mixed Greek/Latin) university documents. */
internal object GreekText {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    /** Apostrophe-like characters used after Greek numerals (Α΄, Β’, ΣΤ' ...). */
    val PRIME_CHARS = Regex("[’'΄´ʹ`‘ʼ′]")

    private val LATIN_LOOKALIKES = mapOf(
        'A' to 'Α', 'B' to 'Β', 'E' to 'Ε', 'Z' to 'Ζ', 'H' to 'Η', 'I' to 'Ι', 'K' to 'Κ',
        'M' to 'Μ', 'N' to 'Ν', 'O' to 'Ο', 'P' to 'Ρ', 'T' to 'Τ', 'X' to 'Χ', 'Y' to 'Υ'
    )

    /** Upper-case [text] and strip accents/diacritics ("Δευτέρα" -> "ΔΕΥΤΕΡΑ"). */
    fun fold(text: String): String =
        COMBINING_MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").uppercase()

    /** Replace Latin capitals that look like Greek ones (documents mix them, e.g. "(B’)" or "ΧE"). */
    fun toGreekLookalikes(text: String): String {
        val builder = StringBuilder(text.length)
        for (ch in text) builder.append(LATIN_LOOKALIKES[ch] ?: ch)
        return builder.toString()
    }

    /** [fold] + [toGreekLookalikes]: the canonical form used for keyword matching. */
    fun canonical(text: String): String = toGreekLookalikes(fold(text))

    /**
     * Semester number for a Greek ordinal numeral ("Α", "ΣΤ", "Η΄") or an ordinal number
     * ("5", "5ο"), or null when [token] is not one.
     */
    fun semesterFromNumeral(token: String): String? {
        val cleaned = canonical(token).replace(PRIME_CHARS, "").filterNot { it.isWhitespace() }
        DIGIT_ORDINAL.matchEntire(cleaned)?.let { match ->
            return match.groupValues[1].takeIf { it.toInt() in 1..8 }
        }
        return when (cleaned) {
            "Α" -> "1"
            "Β" -> "2"
            "Γ" -> "3"
            "Δ" -> "4"
            "Ε" -> "5"
            "ΣΤ", "Ϛ" -> "6"
            "Ζ" -> "7"
            "Η" -> "8"
            else -> null
        }
    }

    private val DIGIT_ORDINAL = Regex("([1-8])(?:Ο|ΟΥ|Η|ΗΣ)?")

    private val DAYS = mapOf(
        "ΔΕΥΤΕΡΑ" to DayOfWeek.MONDAY,
        "ΤΡΙΤΗ" to DayOfWeek.TUESDAY,
        "ΤΕΤΑΡΤΗ" to DayOfWeek.WEDNESDAY,
        "ΠΕΜΠΤΗ" to DayOfWeek.THURSDAY,
        "ΠΑΡΑΣΚΕΥΗ" to DayOfWeek.FRIDAY,
        "ΣΑΒΒΑΤΟ" to DayOfWeek.SATURDAY,
        "ΚΥΡΙΑΚΗ" to DayOfWeek.SUNDAY,
        "MONDAY" to DayOfWeek.MONDAY,
        "TUESDAY" to DayOfWeek.TUESDAY,
        "WEDNESDAY" to DayOfWeek.WEDNESDAY,
        "THURSDAY" to DayOfWeek.THURSDAY,
        "FRIDAY" to DayOfWeek.FRIDAY,
        "SATURDAY" to DayOfWeek.SATURDAY,
        "SUNDAY" to DayOfWeek.SUNDAY
    )

    // Whole words only (plus Greek genitive endings), so "Τρίτο" is not mistaken for "Τρίτη".
    private val DAY_WORD = Regex(
        "(?<![\\p{L}])(" + DAYS.keys.joinToString("|") + ")(?:Σ|Υ)?(?![\\p{L}])"
    )

    /** First day of week named in [text] (Greek or English, any case/accents), or null. */
    fun dayOfWeek(text: String): DayOfWeek? {
        // Fold without the lookalike mapping so English day names stay intact.
        val match = DAY_WORD.find(fold(text)) ?: return null
        return DAYS[match.groupValues[1]]
    }
}
