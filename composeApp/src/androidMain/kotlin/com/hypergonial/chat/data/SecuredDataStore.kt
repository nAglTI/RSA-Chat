package com.hypergonial.chat.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

interface SecuredDataStore {

    suspend fun keys(): Set<String>
    suspend fun size(): Int
    suspend fun <T> hasKey(key: Preferences.Key<T>): Boolean

    fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T>
    suspend fun <T> putPreference(key: Preferences.Key<T>, value: T)

    suspend fun <T> putSecurePreference(key: Preferences.Key<String>, value: T, serializer: KSerializer<T>)
    fun <T> getSecurePreference(key: Preferences.Key<String>, defaultValue: T, serializer: KSerializer<T>): Flow<T>

    suspend fun <T> removePreference(key: Preferences.Key<T>)
    suspend fun clearAllPreference()
}

// Secured datastore
internal class SecuredDataStoreImpl(
    name: String,
    private val context: Context,
    private val securityUtil: SecurityDataUtils,
    private val json: Json
) : SecuredDataStore {

    private val Context.dataStore by preferencesDataStore(name = name)
    private val bytesToStringSeparator = "|"
    private val ivToStringSeparator = ":iv:"
    private val keyAlias = MasterKey.DEFAULT_MASTER_KEY_ALIAS

    override suspend fun keys(): Set<String> = context.dataStore.data.first().asMap().keys.map { it.name }.toSet()

    override suspend fun size(): Int = context.dataStore.data.first().asMap().size

    override suspend fun <T> hasKey(key: Preferences.Key<T>): Boolean {
        return context.dataStore.data.firstOrNull()?.contains(key) ?: false
    }

    // Получение обычного (незащищённого) значения
    override fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[key] ?: defaultValue
        }

    // Сохранение обычного (незащищённого) значения
    override suspend fun <T> putPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Put pref in secured state.
     *
     * @param key ключ для DataStore (используется тип String для зашифрованного представления).
     * @param value значение, которое нужно сохранить.
     * @param serializer сериализатор для типа T.
     */
    override suspend fun <T> putSecurePreference(
        key: Preferences.Key<String>,
        value: T,
        serializer: KSerializer<T>
    ) {
        context.dataStore.edit { preferences ->
            val serializedInput = json.encodeToString(serializer, value)
            val (iv, secureByteArray) = securityUtil.encryptData(keyAlias, serializedInput)
            val secureString = iv.joinToString(separator = bytesToStringSeparator) +
                    ivToStringSeparator +
                    secureByteArray.joinToString(separator = bytesToStringSeparator)
            preferences[key] = secureString
        }
    }

    /**
     * Читает зашифрованное значение, расшифровывает и десериализует его.
     *
     * @param key ключ для DataStore.
     * @param defaultValue значение по умолчанию, если зашифрованная строка отсутствует или имеет неверный формат.
     * @param serializer сериализатор для типа T.
     * @return Flow с объектом типа T.
     */
    override fun <T> getSecurePreference(
        key: Preferences.Key<String>,
        defaultValue: T,
        serializer: KSerializer<T>
    ): Flow<T> =
        context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val secureString = preferences[key] ?: return@map defaultValue
            val parts = secureString.split(ivToStringSeparator, limit = 2)
            if (parts.size < 2) return@map defaultValue
            val (ivString, encryptedString) = parts
            val iv = ivString.split(bytesToStringSeparator)
                .mapNotNull { it.toByteOrNull() }
                .toByteArray()
            val encryptedData = encryptedString.split(bytesToStringSeparator)
                .mapNotNull { it.toByteOrNull() }
                .toByteArray()
            val decryptedValue = securityUtil.decryptData(keyAlias, iv, encryptedData)
            json.decodeFromString(serializer, decryptedValue)
        }

    // Удаление значения по ключу
    override suspend fun <T> removePreference(key: Preferences.Key<T>) {
        context.dataStore.edit {
            it.remove(key)
        }
    }

    // Полная очистка всех сохранённых значений
    override suspend fun clearAllPreference() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
