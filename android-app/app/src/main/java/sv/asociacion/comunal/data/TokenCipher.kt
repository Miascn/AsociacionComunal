package sv.asociacion.comunal.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TokenCipher {
    private val alias = "asociacion_session_tokens"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key(): SecretKey = (keyStore.getKey(alias, null) as? SecretKey) ?: KeyGenerator
        .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        .apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size).put(cipher.iv).put(encrypted).array()
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        val packed = ByteBuffer.wrap(Base64.decode(value, Base64.NO_WRAP))
        val iv = ByteArray(packed.int).also(packed::get)
        val encrypted = ByteArray(packed.remaining()).also(packed::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }
}
