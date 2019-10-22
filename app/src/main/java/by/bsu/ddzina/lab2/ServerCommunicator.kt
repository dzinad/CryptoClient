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
import java.io.DataOutputStream


@TargetApi(Build.VERSION_CODES.N)
class ServerCommunicator {

    enum class LoginResult(val stringValue: String) {
        OK("OK"), WRONG_USERNAME("WRONG_USERNAME"), WRONG_PASSWORD("WRONG_PASSWORD"), ERROR("ERROR");
    }

    private val baseUrl = "http://10.0.2.2:8000/"
    private var sessionKey: Key? = null
    private var publicKey: Key? = null

    fun requestLogin(username: String, password: String, completion: (LoginResult) -> Unit) {
        val url = URL("${baseUrl}login")
        val json = JSONObject().put("username", username).put("password", password)
        val body = json.toString().toByteArray()
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            setRequestProperty("Content-Length", "${body.size}")
            DataOutputStream(this.outputStream).use { wr -> wr.write(body) }
            println("Login. Response code: $responseCode")
            if (responseCode == 200) {
                val responseString = String(inputStream.readBytes())
                var loginResult = LoginResult.ERROR
                for (value in LoginResult.values()) {
                    if (value.stringValue == responseString) {
                        loginResult = value
                        break
                    }
                }
                completion(loginResult)
            } else {
                completion(LoginResult.ERROR)
            }
        }
    }

    fun sendPublicRsaKey(key: RSAPublicKey) {
        publicKey = key
        val body = JSONObject()
            .put(jsonKeyExponent, key.publicExponent.toString())
            .put(jsonKeyModulus, key.modulus.toString())
            .toString()
            .toByteArray(Charsets.UTF_8)

        try {
            val url = URL("${baseUrl}public_rsa/")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("Content-Length", "${body.size}")
                DataOutputStream(this.outputStream).use { wr -> wr.write(body) }
                println("Send Public RSA key. Response code: $responseCode")
            }
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while sending public RSA key.")
            ex.printStackTrace()
        }
    }

    fun sendRequestForFile(fileName: String, completion: (JSONObject?) -> Unit) {
        val text = "The future is as certain as life will come to an end, when time feels like a burden we struggle with our certain death"
        val url = URL("${baseUrl}get_file?file=$fileName")
        //completion(CryptoHelper.encryptAes(sessionKey!!, text.toByteArray(Charsets.UTF_8)))
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            println("Request text file. Response code: $responseCode")
            if (responseCode == 200) {
                try {
                    val responseJson = JSONObject(String(inputStream.readBytes()))
                    completion(responseJson)
                } catch (ex: Throwable) {
                    Log.e("[ServerCommunicator]", "Could not parse server response")
                    ex.printStackTrace()
                    completion(null)
                }
            } else {
                completion(null)
            }
        }
    }

    fun sendRequestForSessionKey(completion: (ByteArray) -> Unit) {
        val url = URL("${baseUrl}session_key")
        val r = SecureRandom()
        val aesKey = ByteArray(16)
        r.nextBytes(aesKey)
        sessionKey = SecretKeySpec(aesKey, "AES")
        completion(CryptoHelper.encryptRsa(publicKey!!, aesKey))
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            println("Request text file. Response code: $responseCode")
            if (responseCode == 200) {
                // todo check handle response
                //completion(CryptoHelper.encryptRsa(publicKey!!, inputStream.readBytes()))
            }
        }

    }

}