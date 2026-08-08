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
        val request = Request.Builder()
            .url("https://api.github.com/repos/Miascn/AsociacionComunal/releases?per_page=20")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GitHub respondió ${response.code}")
            val releases = JsonParser.parseString(response.body?.string().orEmpty()).asJsonArray
            val release = releases.firstOrNull { it.asJsonObject["tag_name"].asString.startsWith("android-v") }?.asJsonObject ?: return@withContext null
            val version = release["tag_name"].asString.removePrefix("android-v")
            if (!VersionComparator.isNewer(version, BuildConfig.VERSION_NAME)) return@withContext null
            val asset = release["assets"].asJsonArray.firstOrNull {
                it.asJsonObject["name"].asString.endsWith(".apk")
            }?.asJsonObject ?: return@withContext null
            AppUpdate(version, asset["browser_download_url"].asString, release["body"]?.asString.orEmpty())
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
