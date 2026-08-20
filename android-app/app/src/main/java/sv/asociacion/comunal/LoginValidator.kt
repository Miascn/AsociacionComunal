package sv.asociacion.comunal

object LoginValidator {
    fun isValid(username: String, password: String): Boolean = username.isNotBlank() && password.isNotBlank()
}
