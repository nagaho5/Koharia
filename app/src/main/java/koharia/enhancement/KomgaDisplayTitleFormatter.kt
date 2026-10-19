package koharia.enhancement

private val leadingRoundBlock = Regex("""^\s*(?:\([^)]*\)|（[^）]*）)\s*""")
private val leadingSquareBlock = Regex("""^\s*(?:\[[^]]*]|［[^］]*］)\s*""")

/**
 * Cleans only the string rendered by Koharia. The original title, Komga metadata,
 * file name and path remain untouched.
 */
fun simplifyKomgaDisplayTitle(title: String): String {
    var simplified = title
    while (true) {
        val next = simplified.replaceFirst(leadingRoundBlock, "")
        if (next == simplified) break
        simplified = next
    }
    simplified = simplified.replaceFirst(leadingSquareBlock, "")
    return simplified.trim().takeIf(String::isNotBlank) ?: title
}
