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
//    private val cipherRSA = Cipher.getInstance("RSA")
    private val cipherAES = Cipher.getInstance("AES/OFB/NoPadding")

    fun generateKeyPair(): RSAKeyPair {
        generator.initialize(512, SecureRandom())
        val pair = generator.genKeyPair()
        return RSAKeyPair(pair.public as RSAPublicKey, pair.private as RSAPrivateKey)
    }

    @Throws(Exception::class)
    fun encryptAes(key: Key, content: String): JSONObject {
        val r = SecureRandom()
        val iv = ByteArray(16)
        r.nextBytes(iv)
        cipherAES.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        val extraSymbols = 16 - contentBytes.size % 16
        val content16 = contentBytes + ByteArray(extraSymbols) { 0 }
        val encryptedBytes = cipherAES.doFinal(content16)
        val encrypted = getStringForBytes(encryptedBytes)
        return JSONObject()
            .put(jsonKeyContent, encrypted)
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
        val cipherRSA = Cipher.getInstance("RSA")
        cipherRSA.init(Cipher.DECRYPT_MODE, key)
        val decryptedBytes = cipherRSA.doFinal(encryptedSessionKey)
        println("[ddlog] decrypted session key: ${Base64.encodeToString(decryptedBytes, Base64.DEFAULT)}")
        println("{ddlog} private exponent: ${privateKey!!.privateExponent}")
        println("{ddlog} modulus: ${privateKey!!.modulus}")


//        encryptRsa()


        return SecretKeySpec(decryptedBytes, "AES")
    }

    fun encryptRsa() {
        val cipherRSA = Cipher.getInstance("RSA")
        cipherRSA.init(Cipher.ENCRYPT_MODE, publicKey)
        val sessionKey = Base64.decode("+iyVqBR+nIJg8+0moF+fNIbGXgyCsSWW0Ub3yYNo62g=", Base64.DEFAULT)
        println("[ddlog] sessionKey.size = ${sessionKey.size}")
        val encrypted = cipherRSA.doFinal(sessionKey)
        println("[ddlog] encrypted by client: ${String(Base64.encode(encrypted, Base64.DEFAULT))}")
    }

    fun decryptAes(key: Key, json: JSONObject?): String {
        val text = Base64.decode(json?.getString(jsonKeyContent)?.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val iv = Base64.decode(json?.getString(jsonKeyIv)?.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val extraSymbols = json?.optInt(jsonKeyExtraSymbols, 0) ?: 0

        if (text != null && iv != null) {
            return String(decryptText(key, text, iv, extraSymbols))
        }
        return ""
    }

    @Throws(Exception::class)
    fun decryptText(key: Key, content: ByteArray, iv: ByteArray, extraSymbols: Int): ByteArray {
        cipherAES.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return cipherAES.doFinal(content).dropLast(extraSymbols).toByteArray()
    }
}