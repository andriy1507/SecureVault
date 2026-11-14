package dev.spaceapps.secvault.feature.vault.domain.model

/**
 * Card networks that change how a number is grouped and checked. Everything that isn't American
 * Express uses 4-digit groups and a 3-digit security code.
 */
enum class CardBrand(
    /** Digit groups shown with a space between them, e.g. 4-6-5 for Amex. */
    val groups: List<Int>,
    val validLengths: IntRange,
    /** Most cards of the brand have this many digits; the editor starts checking the number from here. */
    val typicalLength: Int,
    val cvvLength: Int,
) {
    Amex(groups = listOf(4, 6, 5), validLengths = 15..15, typicalLength = 15, cvvLength = 4),
    Other(groups = listOf(4, 4, 4, 4, 3), validLengths = 12..19, typicalLength = 16, cvvLength = 3),
    ;

    val maxLength: Int get() = validLengths.last
}

/** Month and year of a card's expiry (year as 4 digits). */
data class YearMonth(
    val year: Int,
    val month: Int,
) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth) = compareValuesBy(this, other, { it.year }, { it.month })
}

/** Problems with a card that block saving. */
enum class CardError { InvalidNumber, InvalidExpiry, InvalidCvv }

/** Result of [PaymentCard.validate]. [expired] is only a warning: old cards can still be stored. */
data class CardValidation(
    val number: CardError? = null,
    val expiry: CardError? = null,
    val cvv: CardError? = null,
    val expired: Boolean = false,
) {
    val isValid: Boolean get() = number == null && expiry == null && cvv == null
}

/**
 * Input rules for payment cards.
 * While editing, the draft holds digits only (number, "MMYY" expiry, CVV); separators are drawn by the UI.
 * Saved items store the number as digits and the expiry as "MM/YY".
 */
object PaymentCard {
    const val EXPIRY_DIGITS = 4
    const val EXPIRY_SEPARATOR = '/'
    private const val MONTH_DIGITS = 2
    private const val MONTHS = 12
    private const val CENTURY = 2000
    private const val LUHN_BASE = 10
    private const val LUHN_DOUBLE_OVER = 9

    fun brandOf(digits: String): CardBrand =
        if (digits.startsWith("34") || digits.startsWith("37")) CardBrand.Amex else CardBrand.Other

    /** Digits only, cut to the longest number the brand allows. Pasted spaces and dashes are dropped. */
    fun sanitizeNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return digits.take(brandOf(digits).maxLength)
    }

    /** Splits [digits] into the brand's groups, e.g. "4417 1234 5678 9010". */
    fun groupNumber(digits: String): List<String> {
        val groups = mutableListOf<String>()
        var start = 0
        for (size in brandOf(digits).groups) {
            if (start >= digits.length) break
            groups += digits.substring(start, minOf(start + size, digits.length))
            start += size
        }
        if (start < digits.length) groups += digits.substring(start)
        return groups
    }

    /**
     * Digits of "MMYY" as the user types them, the way banking apps do:
     * a first digit of 2–9 becomes "0X", and a month above 12 or "00" can't be typed.
     */
    fun sanitizeExpiry(input: String): String {
        val out = StringBuilder()
        for (c in input) {
            if (!c.isDigit() || out.length >= EXPIRY_DIGITS) continue
            when (out.length) {
                0 -> {
                    if (c > '1') out.append('0').append(c) else out.append(c)
                }

                1 -> {
                    val first = out[0]
                    val impossible = (first == '0' && c == '0') || (first == '1' && c > '2')
                    if (!impossible) out.append(c)
                }

                else -> {
                    out.append(c)
                }
            }
        }
        return out.toString()
    }

    /** "0528" → "05/28"; partial input keeps the slash once the month is complete ("05" → "05/"). */
    fun formatExpiry(digits: String): String =
        if (digits.length < MONTH_DIGITS) {
            digits
        } else {
            digits.take(MONTH_DIGITS) + EXPIRY_SEPARATOR + digits.drop(MONTH_DIGITS)
        }

    fun sanitizeCvv(
        input: String,
        cardDigits: String,
    ): String = input.filter { it.isDigit() }.take(brandOf(cardDigits).cvvLength)

    /** Luhn checksum, used by every major card network to catch typos. */
    fun isLuhnValid(digits: String): Boolean {
        if (digits.isEmpty() || digits.any { !it.isDigit() }) return false
        var sum = 0
        digits.reversed().forEachIndexed { index, c ->
            var d = c.digitToInt()
            if (index % 2 == 1) {
                d *= 2
                if (d > LUHN_DOUBLE_OVER) d -= LUHN_DOUBLE_OVER
            }
            sum += d
        }
        return sum % LUHN_BASE == 0
    }

    /** "MMYY" → month and year; null when incomplete or the month isn't 01–12. */
    fun parseExpiry(digits: String): YearMonth? {
        if (digits.length != EXPIRY_DIGITS || digits.any { !it.isDigit() }) return null
        val month = digits.take(MONTH_DIGITS).toInt()
        if (month !in 1..MONTHS) return null
        return YearMonth(year = CENTURY + digits.drop(MONTH_DIGITS).toInt(), month = month)
    }

    /** Empty fields are allowed (a card can be saved with only a name); filled ones must be complete and valid. */
    fun validate(
        number: String,
        expiry: String,
        cvv: String,
        today: YearMonth,
    ): CardValidation {
        val brand = brandOf(number)
        val numberError = when {
            number.isEmpty() -> null
            number.length !in brand.validLengths || !isLuhnValid(number) -> CardError.InvalidNumber
            else -> null
        }
        val parsedExpiry = parseExpiry(expiry)
        val expiryError = if (expiry.isNotEmpty() && parsedExpiry == null) CardError.InvalidExpiry else null
        val cvvError = if (cvv.isNotEmpty() && cvv.length != brand.cvvLength) CardError.InvalidCvv else null
        return CardValidation(
            number = numberError,
            expiry = expiryError,
            cvv = cvvError,
            expired = parsedExpiry != null && parsedExpiry < today,
        )
    }

    /** Year and month (UTC) of [epochMillis]; month precision is all card expiry needs. */
    fun yearMonthOf(epochMillis: Long): YearMonth {
        // Days-to-civil conversion (Howard Hinnant), avoiding a date-time dependency.
        val days = epochMillis.floorDiv(MILLIS_PER_DAY) + DAYS_FROM_0000_TO_1970
        val era = days.floorDiv(DAYS_PER_ERA)
        val dayOfEra = days - era * DAYS_PER_ERA
        val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val mp = (5 * dayOfYear + 2) / 153
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
        return YearMonth(year, month)
    }

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val DAYS_FROM_0000_TO_1970 = 719_468L
    private const val DAYS_PER_ERA = 146_097L
}
