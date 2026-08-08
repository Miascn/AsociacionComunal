package sv.asociacion.comunal.data

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sv.asociacion.comunal.BuildConfig

class AppContainer(context: Context) {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE })
        .build()
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create())).build().create(AuthApi::class.java)
    val authRepository = AuthRepository(api, SessionStore(context))
}
