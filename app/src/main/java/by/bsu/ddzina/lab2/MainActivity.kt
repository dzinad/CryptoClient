package by.bsu.ddzina.lab2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.security.interfaces.RSAPrivateKey
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var serverCommunicator: ServerCommunicator
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var keyStorageHelper: KeyStorageHelper
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        serverCommunicator = ServerCommunicator(applicationContext)
        findViewById<Button>(R.id.generateBtn).setOnClickListener(this)
        findViewById<Button>(R.id.requestSessionKeyBtn).setOnClickListener(this)
        findViewById<Button>(R.id.loginBtn).setOnClickListener(this)
        keyStorageHelper = KeyStorageHelper(applicationContext)
        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

        executor.execute { loadPrivateKey() }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.generateBtn -> clickedGenerate()
                R.id.requestSessionKeyBtn -> clickedRequestSessionKey()
                R.id.loginBtn -> clickedLogin()
            }
        }
    }

    private fun clickedGenerate() {
        executor.execute {
            if (preferences.getString(keyUsername, null) != null) {
                preferences.edit().remove(keyUsername).remove(keySessionId).commit()
            }
            val keyPair = CryptoHelper.generateKeyPair()
            privateKey = keyPair.private
            publicKey = keyPair.public
            keyStorageHelper.savePrivateKey(privateKey)
            serverCommunicator.sendPublicRsaKey(keyPair.public)
        }
    }

    private fun clickedRequestSessionKey() {
        executor.execute {
            serverCommunicator.sendRequestForSessionKey { encryptedSessionKey ->
                privateKey?.let {
                    println("[ddlog] encrypted session key: ${String(Base64.encode(encryptedSessionKey, Base64.DEFAULT))}")
                    sessionKey = CryptoHelper.decryptSessionKey(it, encryptedSessionKey)
                }
            }
        }
    }

    private fun loadPrivateKey() {
        privateKey = keyStorageHelper.loadPrivateKey()
        try {
            token = String(Utils.readFile(applicationContext, tokenFileName))
        } catch (ex: Throwable) {

        }
    }

    private fun clickedLogin() {
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
    }
}
