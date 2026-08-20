package sv.asociacion.comunal.data

data class LoginRequest(val username: String, val password: String)
data class TokenRequest(val refreshToken: String)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
data class UserDto(val id: Int, val username: String, val role: String, val passwordChangeRequired: Boolean = false)
data class MemberDto(val id: Int, val names: String, val lastNames: String, val status: String)
data class LoginResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val user: UserDto)
data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long)
data class MeResponse(val user: UserDto, val member: MemberDto?)
