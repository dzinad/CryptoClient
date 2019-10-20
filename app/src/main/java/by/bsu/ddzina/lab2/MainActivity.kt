package by.bsu.ddzina.lab2

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.security.Key
import java.security.interfaces.RSAPrivateKey
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var serverCommunicator: ServerCommunicator
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var pTextView: EditText
    private lateinit var qTextView: EditText
    private lateinit var fileTextView: EditText
    private var sessionKey: Key? = null
    private var privateKey: RSAPrivateKey? = null
    private lateinit var keyStorageHelper: KeyStorageHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pTextView = findViewById(R.id.pEditText)
        qTextView = findViewById(R.id.qEditText)
        fileTextView = findViewById(R.id.fileEditText)
        findViewById<Button>(R.id.requestTextBtn).setOnClickListener(this)
        findViewById<Button>(R.id.generateBtn).setOnClickListener(this)
        findViewById<Button>(R.id.requestSessionKeyBtn).setOnClickListener(this)
        keyStorageHelper = KeyStorageHelper(applicationContext)
        serverCommunicator = ServerCommunicator(applicationContext) // todo move initialization to constructor
        executor.execute { loadPrivateKey() }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.generateBtn -> clickedGenerate()
                R.id.requestTextBtn -> clickedRequestText()
                R.id.requestSessionKeyBtn -> clickedRequestSessionKey()
            }
        }
    }

    private fun clickedGenerate() {
        try {
            executor.execute {
                val keyPair = CryptoHelper.generateKeyPair()
                privateKey = keyPair.private
                keyStorageHelper.savePrivateKey(privateKey)
                serverCommunicator.sendPublicRsaKey(keyPair.public)
            }
        } catch (ex: Throwable) {
            Toast.makeText(this, "Prime numbers are of wrong format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clickedRequestText() {
        val fileName = fileTextView.text.toString()
        if (!TextUtils.isEmpty(fileName)) { // todo remove !
            Toast.makeText(this, "Enter file name", Toast.LENGTH_SHORT).show()
        } else {
            executor.execute {
                serverCommunicator.sendRequestForFile(fileName) { json ->
                    sessionKey?.let {
                        try {
                            val text = json.get(jsonKeyEncyptedKey) as ByteArray
                            val iv = json.get(jsonKeyIv) as ByteArray
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
}
