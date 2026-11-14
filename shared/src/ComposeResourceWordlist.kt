package dev.spaceapps.secvault

import dev.spaceapps.secvault.core.data.crypto.WordlistSource
import dev.spaceapps.secvault.feature.generator.domain.repository.GeneratorWordsSource
import dev.spaceapps.secvault.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val WORDLIST_PATH = "files/bip39_english.txt"

/** Official BIP39 English list, bundled at composeResources/files. Also feeds passphrases and usernames. */
internal class ComposeResourceWordlist :
    WordlistSource,
    GeneratorWordsSource {
    private val mutex = Mutex()
    private var cached: List<String>? = null

    override suspend fun load(): List<String> =
        mutex.withLock {
            cached ?: Res
                .readBytes(WORDLIST_PATH)
                .decodeToString()
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .also { cached = it }
        }

    override suspend fun words(): List<String> = load()
}
