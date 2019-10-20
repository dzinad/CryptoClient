package by.bsu.ddzina.lab2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAPrivateKeySpec
import javax.crypto.spec.SecretKeySpec

class KeyStorageHelper(private val context: Context) {

    private val privateKeyPassword = Utils.getMD5Hash(context.packageName)

    fun loadPrivateKey(): RSAPrivateKey? {
        try {
            val json  =  JSONObject(String(
                CryptoHelper.decryptText(
                    SecretKeySpec(privateKeyPassword, "AES"),
                    Utils.readFile(context, privateKeyFileName),
                    Utils.readFile(context, privateKeyIVFileName)
                ),
                Charsets.UTF_8)
            )

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
                json.toString().toByteArray(Charsets.UTF_8)
            )
            Utils.saveFile(
                context,
                privateKeyFileName,
                encryptedJson.get(jsonKeyEncyptedKey) as ByteArray
            )
            Utils.saveFile(
                context,
                privateKeyIVFileName,
                encryptedJson.get(jsonKeyIv) as ByteArray
            )
        } catch (ex: Throwable) {
            Log.e("[KeyStorageHelper]", "Something went wrong while saving private key")
            ex.printStackTrace()
        }
    }

}