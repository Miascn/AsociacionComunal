package sv.asociacion.comunal.data

class AuthRepository(private val api: AuthApi, private val sessions: SessionStore) {
    suspend fun login(username: String, password: String): MeResponse {
        val login = api.login(LoginRequest(username.trim(), password))
        sessions.save(login.accessToken, login.refreshToken)
        return api.me("Bearer ${login.accessToken}")
    }

    suspend fun restore(): MeResponse? {
        val (access, refresh) = sessions.tokens()
        if (access == null || refresh == null) return null
        return runCatching { api.me("Bearer $access") }.getOrElse {
            runCatching {
                val renewed = api.refresh(TokenRequest(refresh))
                sessions.save(renewed.accessToken, renewed.refreshToken)
                api.me("Bearer ${renewed.accessToken}")
            }.getOrElse { sessions.clear(); null }
        }
    }

    suspend fun logout() {
        val refresh = sessions.tokens().second
        if (refresh != null) runCatching { api.logout(TokenRequest(refresh)) }
        sessions.clear()
    }
}
