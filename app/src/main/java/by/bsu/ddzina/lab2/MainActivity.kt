package by.bsu.ddzina.lab2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.security.interfaces.RSAPrivateKey
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val serverCommunicator = ServerCommunicator()
    private val executor = Executors.newSingleThreadExecutor()
    private var privateKey: RSAPrivateKey? = null
    private lateinit var keyStorageHelper: KeyStorageHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.generateBtn).setOnClickListener(this)
        findViewById<Button>(R.id.requestSessionKeyBtn).setOnClickListener(this)
        findViewById<Button>(R.id.loginBtn).setOnClickListener(this)
        keyStorageHelper = KeyStorageHelper(applicationContext)
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
            val keyPair = CryptoHelper.generateKeyPair()
            privateKey = keyPair.private
            keyStorageHelper.savePrivateKey(privateKey)
            serverCommunicator.sendPublicRsaKey(keyPair.public)
        }
    }

    private fun clickedRequestSessionKey() {
        executor.execute {
            serverCommunicator.sendRequestForSessionKey { encryptredSessionKey ->
                privateKey?.let {
                    sessionKey = CryptoHelper.decryptSessionKey(it, encryptredSessionKey)
                }
            }
        }
    }

    private fun loadPrivateKey() {
        privateKey = keyStorageHelper.loadPrivateKey()
    }

    private fun clickedLogin() {
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
    }
}
