package com.nokia.launcher.ui.home

const val MULTI_TAP_TIMEOUT_MS = 800L

private val keyChars = mapOf(
    '1' to ".,?!-@1", '2' to "abc2", '3' to "def3", '4' to "ghi4", '5' to "jkl5",
    '6' to "mno6", '7' to "pqrs7", '8' to "tuv8", '9' to "wxyz9", '0' to " 0",
    '*' to "*+-=/()%&\$:;_'\"",
)

/** Cycled with `#`, like on a classic Nokia. */
enum class InputMode(val label: String) { Lower("abc"), Upper("ABC"), Number("123") }

data class Tap(val key: Char, val timeMs: Long)

/** What `*` cycles through while dialing: itself, `+`, pause and wait. */
private const val DIAL_STAR = "*+pw"

fun dialTap(text: String, previous: Tap?, tap: Tap): String {
    val cycling = tap.key == '*' && previous?.key == '*' &&
        tap.timeMs - previous.timeMs < MULTI_TAP_TIMEOUT_MS &&
        text.isNotEmpty() && text.last() in DIAL_STAR
    return if (cycling) {
        text.dropLast(1) + DIAL_STAR[(DIAL_STAR.indexOf(text.last()) + 1) % DIAL_STAR.length]
    } else {
        text + tap.key
    }
}

/** Android dial strings write pause as `,` and wait as `;`. */
fun dialString(text: String) = text.replace('p', ',').replace('w', ';')

/** Returns the text after pressing [tap]; the same key within the timeout cycles the last letter. */
fun multiTap(text: String, previous: Tap?, tap: Tap, mode: InputMode = InputMode.Lower): String {
    val base = keyChars[tap.key] ?: return text
    if (mode == InputMode.Number) return text + tap.key
    val chars = if (mode == InputMode.Upper) base.uppercase() else base
    val cycling = previous?.key == tap.key &&
        tap.timeMs - previous.timeMs < MULTI_TAP_TIMEOUT_MS &&
        text.isNotEmpty() && text.last() in chars
    return if (cycling) {
        text.dropLast(1) + chars[(chars.indexOf(text.last()) + 1) % chars.length]
    } else {
        text + chars.first()
    }
}
