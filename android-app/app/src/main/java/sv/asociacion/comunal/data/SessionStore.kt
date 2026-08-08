package sv.asociacion.comunal.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore("secure_session")

class SessionStore(private val context: Context, private val cipher: TokenCipher = TokenCipher()) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    suspend fun tokens(): Pair<String?, String?> {
        val values = context.sessionDataStore.data.first()
        return try {
            values[accessKey]?.let(cipher::decrypt) to values[refreshKey]?.let(cipher::decrypt)
        } catch (_: Exception) {
            clear()
            null to null
        }
    }
    suspend fun save(access: String, refresh: String) = context.sessionDataStore.edit {
        it[accessKey] = cipher.encrypt(access)
        it[refreshKey] = cipher.encrypt(refresh)
    }
    suspend fun clear() = context.sessionDataStore.edit { it.clear() }
}
