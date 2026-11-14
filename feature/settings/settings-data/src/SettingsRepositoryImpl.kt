package dev.spaceapps.secvault.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.spaceapps.secvault.core.data.storage.SETTINGS_DATASTORE
import dev.spaceapps.secvault.feature.settings.domain.AppSettings
import dev.spaceapps.secvault.feature.settings.domain.AppTheme
import dev.spaceapps.secvault.feature.settings.domain.AutoLock
import dev.spaceapps.secvault.feature.settings.domain.ClipboardClear
import dev.spaceapps.secvault.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[THEME] = next.theme.name
            prefs[DYNAMIC_COLOR] = next.dynamicColor
            prefs[AUTO_LOCK] = next.autoLock.name
            prefs[CLIPBOARD_CLEAR] = next.clipboardClear.name
            prefs[BLOCK_SCREENSHOTS] = next.blockScreenshots
        }
    }

    override suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = enumOr(this[THEME], defaults.theme),
            dynamicColor = this[DYNAMIC_COLOR] ?: defaults.dynamicColor,
            autoLock = enumOr(this[AUTO_LOCK], defaults.autoLock),
            clipboardClear = enumOr(this[CLIPBOARD_CLEAR], defaults.clipboardClear),
            blockScreenshots = this[BLOCK_SCREENSHOTS] ?: defaults.blockScreenshots,
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(
        name: String?,
        default: E,
    ): E = name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AUTO_LOCK = stringPreferencesKey("auto_lock")
        val CLIPBOARD_CLEAR = stringPreferencesKey("clipboard_clear")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
    }
}

val settingsDataModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get(named(SETTINGS_DATASTORE))) }
}
