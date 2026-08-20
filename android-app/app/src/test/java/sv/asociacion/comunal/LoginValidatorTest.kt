package sv.asociacion.comunal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidatorTest {
    @Test fun acceptsCompleteCredentials() = assertTrue(LoginValidator.isValid("jperez", "clave-segura"))
    @Test fun rejectsEmptyUsername() = assertFalse(LoginValidator.isValid(" ", "clave-segura"))
    @Test fun rejectsEmptyPassword() = assertFalse(LoginValidator.isValid("jperez", ""))
}
