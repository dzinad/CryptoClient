package by.bsu.ddzina.lab2

import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.Key
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {

    private val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
    private val cipherRSA = Cipher.getInstance("RSA")
    private val cipherAES = Cipher.getInstance("AES/OFB/NoPadding")

    fun generateKeyPair(): RSAKeyPair {
        generator.initialize(256, SecureRandom())
        val pair = generator.genKeyPair()
        return RSAKeyPair(pair.public as RSAPublicKey, pair.private as RSAPrivateKey)
    }

    @Throws(Exception::class)
    fun encryptAes(key: Key, content: String): JSONObject {
        val r = SecureRandom()
        val iv = ByteArray(16)
        r.nextBytes(iv)
        cipherAES.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val extraSymbols = 16 - content.length % 16
        val content16 = content + (1..extraSymbols).map { ' ' }.joinToString("")
        return JSONObject()
            .put(jsonKeyContent, getStringForBytes(cipherAES.doFinal(content16.toByteArray(Charsets.UTF_8))))
            .put(jsonKeyIv, getStringForBytes(iv))
            .put(jsonKeyExtraSymbols, extraSymbols)
    }

    private fun getStringForBytes(bytes: ByteArray): String? {
        try {
            return String(Base64.encode(bytes, Base64.DEFAULT), Charsets.UTF_8)
        } catch (ex: Throwable) {
            Log.e("[CryptoHelper]", "Could not convert bytes to string.")
            ex.printStackTrace()
        }
        return null
    }

    @Throws(Exception::class)
    fun decryptSessionKey(key: Key, encryptedSessionKey: ByteArray): Key {
        cipherRSA.init(Cipher.DECRYPT_MODE, key)
        val decryptedBytes = cipherRSA.doFinal(encryptedSessionKey)
        return SecretKeySpec(decryptedBytes, "AES")
    }

    @Throws(Exception::class)
    fun decryptText(key: Key, content: ByteArray, iv: ByteArray): ByteArray {
        cipherAES.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return cipherAES.doFinal(content)
    }
}