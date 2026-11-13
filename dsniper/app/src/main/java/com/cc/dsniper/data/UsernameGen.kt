// com.cc.dsniper.data.UsernameGen — generates username candidates
package com.cc.dsniper.data

object UsernameGen {
    private const val LETTERS = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS  = "0123456789"
    private const val ALPHANUM = "$LETTERS$DIGITS"

    // Generates a lazy sequence of all usernames of [length] chars
    // from the given charset, in random order (Fisher-Yates on indices).
    fun generate(config: SniperConfig): Sequence<String> {
        val charset = when {
            config.onlyNumbers  -> DIGITS
            config.onlyLetters  -> LETTERS
            else                -> ALPHANUM
        }
        val len = config.digits
        val total = Math.pow(charset.length.toDouble(), len.toDouble()).toLong()

        // Build shuffled index list — for small charsets/lengths this is fine
        // For 36^4 = ~1.6M entries, allocate list of longs
        return sequence {
            val indices = (0L until total).toMutableList()
            indices.shuffle()
            for (idx in indices) {
                yield(indexToString(idx, charset, len))
            }
        }
    }

    private fun indexToString(idx: Long, charset: String, len: Int): String {
        val sb = StringBuilder(len)
        var n = idx
        val base = charset.length.toLong()
        repeat(len) {
            sb.append(charset[(n % base).toInt()])
            n /= base
        }
        return sb.reverse().toString()
    }
}
