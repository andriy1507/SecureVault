package dev.spaceapps.secvault.feature.generator.data

import dev.spaceapps.secvault.feature.generator.domain.model.GeneratedValue
import dev.spaceapps.secvault.feature.generator.domain.repository.GeneratorHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.dsl.module

/** Generated values live only in memory and are cleared when the vault locks. */
internal class InMemoryGeneratorHistory : GeneratorHistoryRepository {
    private val _history = MutableStateFlow<List<GeneratedValue>>(emptyList())
    override val history: StateFlow<List<GeneratedValue>> = _history.asStateFlow()

    override fun add(value: GeneratedValue) {
        _history.update { (listOf(value) + it.filterNot { old -> old.value == value.value }).take(MAX_ENTRIES) }
    }

    override fun clear() {
        _history.value = emptyList()
    }

    private companion object {
        const val MAX_ENTRIES = 20
    }
}

val generatorDataModule = module {
    single<GeneratorHistoryRepository> { InMemoryGeneratorHistory() }
}
