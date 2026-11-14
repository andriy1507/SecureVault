package dev.spaceapps.secvault.core.data.crypto

/** Provides the official BIP39 English wordlist (2048 words). */
fun interface WordlistSource {
    suspend fun load(): List<String>
}

/** BIP39 encoding: 256-bit entropy <-> 24 words (8-bit SHA-256 checksum). */
internal class Bip39(
    private val wordlist: List<String>,
) {
    init {
        require(wordlist.size == 2048) { "BIP39 wordlist must have 2048 words, got ${wordlist.size}" }
        require(wordlist.first() == "abandon" && wordlist.last() == "zoo") { "Unexpected BIP39 wordlist" }
    }

    private val index: Map<String, Int> = wordlist.withIndex().associate { it.value to it.index }

    suspend fun entropyToWords(entropy: ByteArray): List<String> {
        require(entropy.size == 32)
        val checksum = Crypto.sha256(entropy)[0]
        val bits = (entropy + checksum).toBits() // 264 bits = 24 * 11
        return bits.chunked(11).map { chunk -> wordlist[chunk.fold(0) { acc, b -> (acc shl 1) or b }] }
    }

    suspend fun wordsToEntropy(words: List<String>): ByteArray {
        require(words.size == 24) { "Need 24 words" }
        val bits = words.flatMap { w ->
            val i = index[w.trim().lowercase()] ?: throw IllegalArgumentException("Unknown word: $w")
            (10 downTo 0).map { (i shr it) and 1 }
        }
        val bytes = bits.chunked(8).map { byte -> byte.fold(0) { acc, b -> (acc shl 1) or b }.toByte() }.toByteArray()
        val entropy = bytes.copyOfRange(0, 32)
        require(Crypto.sha256(entropy)[0] == bytes[32]) { "Invalid recovery phrase" }
        return entropy
    }

    fun isValidWord(word: String): Boolean = word.trim().lowercase() in index

    private fun ByteArray.toBits(): List<Int> =
        flatMap { b -> (7 downTo 0).map { ((b.toInt() and 0xFF) shr it) and 1 } }
}
