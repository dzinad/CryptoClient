package by.bsu.ddzina.lab2

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAPrivateKeySpec
import java.util.*
import javax.crypto.spec.SecretKeySpec

class KeyStorageHelper(private val context: Context) {

    private val privateKeyPassword = Utils.getMD5Hash(context.packageName)

    fun loadPrivateKey(): RSAPrivateKey? {
        try {
            val privateKeyMetaJson = JSONObject(String(Utils.readFile(context, privateKeyMetaFileName)))
            val iv = Base64.decode(privateKeyMetaJson.getString(jsonKeyIv).toByteArray(Charsets.UTF_8), Base64.DEFAULT)
            val decryptedString = String(
                CryptoHelper.decryptText(
                    SecretKeySpec(privateKeyPassword, "AES"),
                    Base64.decode(Utils.readFile(context, privateKeyFileName), Base64.DEFAULT),
                    iv,
                    privateKeyMetaJson.optInt(jsonKeyExtraSymbols)
                ),
                Charsets.UTF_8)
            val json  = JSONObject(decryptedString)

            val keySpec = RSAPrivateKeySpec(
                json.getString(jsonKeyModulus).toBigInteger(),
                json.getString(jsonKeyExponent).toBigInteger()
            )
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey
        } catch (ex: Throwable) {
            Log.e("[KeyStorageHelper]", "Something went wrong while loading private key")
            ex.printStackTrace()
        }
        return null
    }

    fun savePrivateKey(privateKey: RSAPrivateKey?) {
        try {
            val json = JSONObject()
            json.put(jsonKeyModulus, privateKey?.modulus.toString())
            json.put(jsonKeyExponent, privateKey?.privateExponent.toString())
            val encryptedJson = CryptoHelper.encryptAes(
                SecretKeySpec(privateKeyPassword, "AES"),
                json.toString()
            )
            Utils.saveFile(
                context,
                privateKeyFileName,
                encryptedJson.getString(jsonKeyContent).toByteArray(Charsets.UTF_8)
            )
            val metaJson = JSONObject()
                .put(jsonKeyIv, encryptedJson.getString(jsonKeyIv))
                .put(jsonKeyExtraSymbols, encryptedJson.getInt(jsonKeyExtraSymbols))
            Utils.saveFile(
                context,
                privateKeyMetaFileName,
                metaJson.toString().toByteArray(Charsets.UTF_8)
            )
        } catch (ex: Throwable) {
            Log.e("[KeyStorageHelper]", "Something went wrong while saving private key")
            ex.printStackTrace()
        }
    }

}