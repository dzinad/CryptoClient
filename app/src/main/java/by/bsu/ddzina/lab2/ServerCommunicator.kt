package by.bsu.ddzina.lab2

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.io.DataOutputStream


@TargetApi(Build.VERSION_CODES.N)
class ServerCommunicator(private val context: Context) {

    enum class LoginResult(val stringValue: String) {
        OK("OK"),
        WRONG_USERNAME("WRONG_USERNAME"),
        WRONG_PASSWORD("WRONG_PASSWORD"),
        ERROR("ERROR"),
        NO_SESSION_KEY("NO_SESSION_KEY"),
        BAD_TOKEN("BAD_TOKEN"),
        WRONG_VERIFICATION_CODE("WRONG_VERIFICATION_CODE");
    }

    private val baseUrl = "http://10.0.2.2:8000/"

    fun sendVerification(code: Int, completion: (LoginResult, String?) -> Unit) {
        if (sessionKey == null) {
            completion(LoginResult.NO_SESSION_KEY, null)
            return
        }
        if (token == null) {
            completion(LoginResult.BAD_TOKEN, null)
        }
        try {
            val url = URL("${baseUrl}verification/?$tokenParam=${token!!}")
            val encryptedCode = CryptoHelper.encryptAes(sessionKey!!, code.toString())
            val json = JSONObject()
                .put(jsonKeyVerificationCode, encryptedCode)
            val body = json.toString().toByteArray()
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("Content-Length", "${body.size}")
                DataOutputStream(this.outputStream).use { wr -> wr.write(body) }
                println("Verification. Response code: $responseCode")
                if (responseCode == 200) {
                    val responseJson = JSONObject(String(inputStream.readBytes()))
                    val resultString = responseJson.optString(keyResult)
                    var loginResult = LoginResult.ERROR
                    for (value in LoginResult.values()) {
                        if (value.stringValue == resultString) {
                            loginResult = value
                            break
                        }
                    }
                    if (loginResult == LoginResult.OK) {
                        val sessionIdEncrypted = responseJson.optJSONObject(keySessionId)
                        val sessionIdDecrypted = CryptoHelper.decryptAes(sessionKey!!, sessionIdEncrypted)
                        completion(loginResult, sessionIdDecrypted)
                    } else {
                        completion(loginResult, null)
                    }
                } else {
                    completion(LoginResult.ERROR, null)
                }
            }
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while sending verification code.")
            ex.printStackTrace()
        }
    }

    fun sendLogout(completion: (Boolean) -> Unit) {
        if (sessionKey == null) {
            completion(false)
            return
        }
        if (token == null) {
            completion(false)
            return
        }
        try {
            val url = URL("${baseUrl}logout/?$tokenParam=${token!!}")
            val body = CryptoHelper.encryptAes(
                sessionKey!!,
                JSONObject()
                    .put(keySessionId, sessionId)
                    .toString()
            ).toString().toByteArray(Charsets.UTF_8)
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("Content-Length", "${body.size}")
                DataOutputStream(this.outputStream).use { wr -> wr.write(body) }

                if (responseCode == 200) {
                    completion(true)
                } else {
                    completion(false)
                }
            }
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while logging out.", ex)
            completion(false)
        }
    }

    fun requestLogin(username: String, password: String, completion: (LoginResult) -> Unit) {
        if (sessionKey == null) {
            completion(LoginResult.NO_SESSION_KEY)
            return
        }
        if (token == null) {
            completion(LoginResult.BAD_TOKEN)
            return
        }
        try {
            val url = URL("${baseUrl}login/?$tokenParam=${token!!}")
            println("encrypt password $password with session key: ${String(Base64.encode(sessionKey!!.encoded, Base64.DEFAULT))}")
            val encryptedJson = CryptoHelper.encryptAes(sessionKey!!, password)
            val json = JSONObject()
                .put("username", username)
                .put("password", encryptedJson.toString())
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
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while requesting login.")
            ex.printStackTrace()
        }
    }

    fun sendPublicRsaKey(key: RSAPublicKey) {
        val body = JSONObject()
            .put(jsonKeyExponent, key.publicExponent.toString())
            .put(jsonKeyModulus, key.modulus.toString())
            .toString()
            .toByteArray(Charsets.UTF_8)

        try {
            val url = URL("${baseUrl}public_rsa")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("Content-Length", "${body.size}")
                DataOutputStream(this.outputStream).use { wr -> wr.write(body) }
                println("Send Public RSA key. Response code: $responseCode")
                if (responseCode == 200) {
                    try {
                        token = String(inputStream.readBytes())
                        Utils.saveFile(context, tokenFileName, token!!.toByteArray())
                    } catch (ex: Throwable) {
                        Log.e("[ServerCommunicator]", "Could not parse server response")
                        ex.printStackTrace()
                    }
                }
            }
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while sending public RSA key.")
            ex.printStackTrace()
        }
    }

    fun sendRequestForFile(fileName: String, completion: (JSONObject?) -> Unit) {
        if (token == null || sessionKey == null) {
            Utils.showToast(context, "Get token and session key first")
            return
        }
        try {
            val url = URL("${baseUrl}get_file?file=$fileName&$tokenParam=${token!!}")
            val body = CryptoHelper.encryptAes(
                sessionKey!!,
                JSONObject()
                    .put(keySessionId, sessionId)
                    .toString()
            ).toString().toByteArray(Charsets.UTF_8)
            println("body: ${String(body)}")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                println("session key: $sessionKey")
                setRequestProperty("Content-Length", "${body.size}")
                DataOutputStream(this.outputStream).use { wr -> wr.write(body) }

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
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while requesting file.")
            ex.printStackTrace()
        }
    }

    fun sendRequestForSessionKey(completion: (ByteArray) -> Unit) {
        if (token == null) {
            Utils.showToast(context, "Get token first")
            return
        }
        try {
            val url = URL("${baseUrl}session_key/?$tokenParam=${token!!}")
            val r = SecureRandom()
            val aesKey = ByteArray(16)
            r.nextBytes(aesKey)
            //sessionKey = SecretKeySpec(aesKey, "AES")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                println("Request session key. Response code: $responseCode")
                if (responseCode == 200) {
                    completion(inputStream.readBytes())
                }
            }
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while requesting session key.")
            ex.printStackTrace()
        }

    }

    fun sendFile(fileName: String, fileContent: String, completion: (String) -> Unit) {
        if (token == null || sessionKey == null) {
            completion("Get token and session key first")
            return
        }
        try {
            val url = URL("${baseUrl}send_file/?$tokenParam=${token!!}")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                val body = CryptoHelper.encryptAes(
                    sessionKey!!,
                    JSONObject()
                        .put(jsonKeyFileName, fileName)
                        .put(jsonKeyFileContent, fileContent)
                        .put(jsonKeyRequestCode, secretRequestCode ?: "")
                        .put(keySessionId, sessionId)
                        .toString()
                ).toString().toByteArray(Charsets.UTF_8)
                setRequestProperty("Content-Length", "${body.size}")
                DataOutputStream(this.outputStream).use { wr -> wr.write(body) }
                println("Send file. Response code: $responseCode")
                if (responseCode == 200) {
                    val encryptedNewRequestCode = JSONObject(String(inputStream.readBytes()))
                    val newRequestCode = String(
                        CryptoHelper.decryptText(
                            sessionKey!!,
                            Base64.decode(encryptedNewRequestCode.getString(jsonKeyContent).toByteArray(), Base64.DEFAULT),
                            Base64.decode(encryptedNewRequestCode.getString(jsonKeyIv).toByteArray(), Base64.DEFAULT),
                            encryptedNewRequestCode.optInt(jsonKeyExtraSymbols)
                        )
                    )
                    if (TextUtils.isEmpty(newRequestCode) == false) {
                        secretRequestCode = newRequestCode
                        println("new request code: $secretRequestCode")
                    }
                    completion("File $fileName sent")
                } else {
                    completion("File not sent. Response code: $responseCode")
                }
            }
        } catch (ex: Throwable) {
            Log.e("[ServerCommunicator]", "Something went wrong while sending file.")
            ex.printStackTrace()
            completion("Some error while sending file")
        }
    }
}