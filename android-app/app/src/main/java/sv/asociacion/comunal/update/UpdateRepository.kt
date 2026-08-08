package sv.asociacion.comunal.update

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import sv.asociacion.comunal.BuildConfig
import java.io.File

class UpdateRepository(private val context: Context, private val client: OkHttpClient = OkHttpClient()) {
    suspend fun findUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        val base = BuildConfig.API_BASE_URL.trimEnd('/') + "/"
        val request = Request.Builder().url(base + "api/mobile/updates/android/manifest")
            .header("ngrok-skip-browser-warning", "asociacion-android").build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) error("Servidor respondió ${response.code}")
            val manifest = JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
            val version = manifest["version"].asString
            if (!VersionComparator.isNewer(version, BuildConfig.VERSION_NAME)) return@withContext null
            AppUpdate(version, base.trimEnd('/') + manifest["downloadPath"].asString, manifest["notes"]?.asString.orEmpty())
        }
    }

    suspend fun download(update: AppUpdate, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(update.downloadUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("No fue posible descargar la actualización")
            val body = response.body ?: error("La actualización llegó vacía")
            val destination = File(context.cacheDir, "updates/asociacion-${update.version}.apk")
            destination.parentFile?.mkdirs()
            body.byteStream().use { input -> destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE); var copied = 0L; var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    output.write(buffer, 0, read); copied += read
                    if (body.contentLength() > 0) onProgress(((copied * 100) / body.contentLength()).toInt().coerceIn(0, 100))
                }
            } }
            destination
        }
    }

}
