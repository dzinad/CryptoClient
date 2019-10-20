package by.bsu.ddzina.lab2

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.Key
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import javax.crypto.spec.SecretKeySpec

@TargetApi(Build.VERSION_CODES.N)
class ServerCommunicator(private val context: Context) {

    private val baseUrl = "http://10.0.2.2:8000/"
    private var sessionKey: Key? = null
    private var publicKey: Key? = null

    init { // todo remove
        try {
            val json = JSONObject(String(Utils.readFile(context, publicKeyFileName), Charsets.UTF_8))
            val keySpec = RSAPublicKeySpec(
                json.getString("modulus").toBigInteger(),
                json.getString("exp").toBigInteger()
            )
            publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    fun sendPublicRsaKey(key: RSAPublicKey) {
        publicKey = key
        // todo remove
        Utils.saveFile(
            context,
            publicKeyFileName,
            JSONObject()
                .put("exp", key.publicExponent.toString())
                .put("modulus", key.modulus.toString())
                .toString()
                .toByteArray(Charsets.UTF_8)
        )
        try {
            val url = URL("${baseUrl}public_rsa?exp=${key.publicExponent}&mod=${key.modulus}")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                println("Send Public RSA key. Response code: $responseCode")
            }
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while sending public RSA key.")
            ex.printStackTrace()
        }
    }

    fun sendRequestForFile(fileName: String, completion: (JSONObject) -> Unit) {
        val text = "The future is as certain as life will come to an end, when time feels like a burden we struggle with our certain death"
        val url = URL("${baseUrl}get_file?file=$fileName")
        completion(CryptoHelper.encryptAes(sessionKey!!, text.toByteArray(Charsets.UTF_8)))
//        with(url.openConnection() as HttpURLConnection) {
//            requestMethod = "GET"
//            println("Request text file. Response code: $responseCode")
//            inputStream.bufferedReader().use {
//                it.lines().forEach { line ->
//                    println(line)
//                }
//            }
//        }
    }

    fun sendRequestForSessionKey(completion: (ByteArray) -> Unit) {
        val url = URL("${baseUrl}session_key")
        val r = SecureRandom()
        val aesKey = ByteArray(16)
        r.nextBytes(aesKey)
        sessionKey = SecretKeySpec(aesKey, "AES")
        completion(CryptoHelper.encryptRsa(publicKey!!, aesKey))
//        with(url.openConnection() as HttpURLConnection) {
//            requestMethod = "GET"
//            println("Request text file. Response code: $responseCode")
//            completion(String(inputStream.readBytes()))
//        }

    }

}