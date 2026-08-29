package mir.oslav.adrevenue.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class PersistentAdMobCache @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val snapshotPreferenceKey = stringPreferencesKey(CacheConstants.SnapshotKey)

    suspend fun read(): CachedAdMobSnapshot =
        dataStore.data
            .map { preferences ->
                preferences[snapshotPreferenceKey]
                    ?.let { encodedSnapshot ->
                        runCatching {
                            json.decodeFromString<CachedAdMobSnapshot>(encodedSnapshot)
                        }.getOrNull()
                    }
                    ?: CachedAdMobSnapshot()
            }
            .first()

    suspend fun write(snapshot: CachedAdMobSnapshot) {
        dataStore.edit { preferences ->
            preferences[snapshotPreferenceKey] = json.encodeToString(snapshot)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(snapshotPreferenceKey)
        }
    }
}
