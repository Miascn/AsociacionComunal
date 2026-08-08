package sv.asociacion.comunal.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login") suspend fun login(@Body request: LoginRequest): LoginResponse
    @POST("api/auth/refresh") suspend fun refresh(@Body request: TokenRequest): TokenResponse
    @POST("api/auth/logout") suspend fun logout(@Body request: TokenRequest)
    @GET("api/me") suspend fun me(@Header("Authorization") authorization: String): MeResponse
}
