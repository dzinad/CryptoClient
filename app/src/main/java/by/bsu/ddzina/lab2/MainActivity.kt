package by.bsu.ddzina.lab2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.security.Key
import java.security.interfaces.RSAPrivateKey
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val serverCommunicator = ServerCommunicator()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var fileTextView: EditText
    private var sessionKey: Key? = null
    private var privateKey: RSAPrivateKey? = null
    private lateinit var keyStorageHelper: KeyStorageHelper
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fileTextView = findViewById(R.id.fileEditText)
        findViewById<Button>(R.id.requestTextBtn).setOnClickListener(this)
        findViewById<Button>(R.id.generateBtn).setOnClickListener(this)
        findViewById<Button>(R.id.requestSessionKeyBtn).setOnClickListener(this)
        findViewById<Button>(R.id.logoutBtn).setOnClickListener(this)
        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        keyStorageHelper = KeyStorageHelper(applicationContext)
        executor.execute { loadPrivateKey() }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.generateBtn -> clickedGenerate()
                R.id.requestTextBtn -> clickedRequestText()
                R.id.requestSessionKeyBtn -> clickedRequestSessionKey()
                R.id.logoutBtn -> clickedLogout()
            }
        }
    }

    private fun clickedGenerate() {
        executor.execute {
            val keyPair = CryptoHelper.generateKeyPair()
            privateKey = keyPair.private
            keyStorageHelper.savePrivateKey(privateKey)
            serverCommunicator.sendPublicRsaKey(keyPair.public)
        }
    }

    private fun clickedRequestText() {
        val fileName = fileTextView.text.toString()
        if (TextUtils.isEmpty(fileName)) {
            Utils.showToast(applicationContext, "Enter file name.")
        } else {
            executor.execute {
                serverCommunicator.sendRequestForFile(fileName) { json ->
                    val error = if (json != null) json.optString(jsonKeyError) else ""
                    if (error.isEmpty() == false) {
                        when (error) {
                            jsonValueNoFile -> handleNoFile()
                            jsonValueSessionKeyExpired -> handleSessionKeyExpired()
                        }
                    } else {
                        val content = json?.optJSONObject(jsonKeyContent)
                        sessionKey?.let {
                            try {
                                val text = content?.get(jsonKeyEncyptedKey) as ByteArray
                                val iv = content?.get(jsonKeyIv) as ByteArray
                                val decryptedText = CryptoHelper.decryptText(it, text, iv)
                                Utils.saveFile(applicationContext, textFileName, decryptedText)
                                val intent = Intent(this, TextActivity::class.java)
                                startActivity(intent)
                            } catch (ex: Throwable) {
                                Log.e("[MainActivity]", "Something went wrong while processing requested text.")
                                ex.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleNoFile() {
        runOnUiThread {
            Utils.showToast(applicationContext, "No such file, try another name.")
        }
    }

    private fun handleSessionKeyExpired() {
        runOnUiThread {
            Utils.showToast(applicationContext, "Session key expired. Request a new one.")
        }
    }

    private fun clickedRequestSessionKey() {
        executor.execute {
            serverCommunicator.sendRequestForSessionKey { decryptedSessionKey ->
                privateKey?.let {
                    sessionKey = CryptoHelper.decryptSessionKey(it, decryptedSessionKey)
                }
            }
        }
    }

    private fun loadPrivateKey() {
        privateKey = keyStorageHelper.loadPrivateKey()
    }

    private fun clickedLogout() {
        executor.execute {
            preferences.edit().remove(keyUsername).commit()
            runOnUiThread {
                finish()
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }
}
