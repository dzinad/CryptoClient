package by.bsu.ddzina.lab2

import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.*
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

    // todo remove: server stub
    @Throws(Exception::class)
    fun encryptRsa(key: Key, content: ByteArray): ByteArray {
        cipherRSA.init(Cipher.ENCRYPT_MODE, key)
        return cipherRSA.doFinal(content)
    }

    @Throws(Exception::class)
    fun encryptAes(key: Key, content: ByteArray): JSONObject {
        val r = SecureRandom()
        val iv = ByteArray(16)
        r.nextBytes(iv)
        cipherAES.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        return JSONObject().put(jsonKeyEncyptedKey, cipherAES.doFinal(content)).put(jsonKeyIv, iv)
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