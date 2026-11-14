package dev.spaceapps.secvault.feature.generator.domain.repository

import dev.spaceapps.secvault.feature.generator.domain.model.GeneratedValue
import kotlinx.coroutines.flow.StateFlow

/** Word list used for passphrases and usernames. */
fun interface GeneratorWordsSource {
    suspend fun words(): List<String>
}

/** What was generated during this unlocked session. Kept only in memory. */
interface GeneratorHistoryRepository {
    val history: StateFlow<List<GeneratedValue>>
    fun add(value: GeneratedValue)
    fun clear()
}
