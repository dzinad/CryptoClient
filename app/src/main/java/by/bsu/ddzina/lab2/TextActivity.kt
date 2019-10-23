package by.bsu.ddzina.lab2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.security.Key
import java.util.concurrent.Executors
import kotlin.random.Random

class TextActivity : AppCompatActivity(), View.OnClickListener {

    private val serverCommunicator = ServerCommunicator()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var preferences: SharedPreferences
    private lateinit var textTextView: TextView
    private lateinit var fileTextView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)
        textTextView = findViewById(R.id.textTextView)
        fileTextView = findViewById(R.id.fileEditText)
        findViewById<Button>(R.id.requestTextBtn).setOnClickListener(this)
        findViewById<Button>(R.id.logoutBtn).setOnClickListener(this)
        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.requestTextBtn -> clickedRequestText()
                R.id.logoutBtn -> clickedLogout()
            }
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun clickedRequestText() {
        val fileName = fileTextView.text.toString()
        if (TextUtils.isEmpty(fileName)) {
            Utils.showToast(applicationContext, "Enter file name.")
        } else if (sessionKey == null) {
            Utils.showToast(applicationContext, "Request session key first.")
        } else {
            executor.execute {
                serverCommunicator.sendRequestForFile(fileName) { json ->
                    executor.execute {
                        println("json response: $json")
                        val error = if (json != null) json.optString(jsonKeyError) else ""
                        if (error.isEmpty() == false) {
                            when (error) {
                                jsonValueNoFile -> handleNoFile()
                                jsonValueSessionKeyExpired -> handleSessionKeyExpired()
                            }
                        } else {
                            try {
                                val text = Base64.decode(json?.getString(jsonKeyContent)?.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
                                val iv = Base64.decode(json?.getString(jsonKeyIv)?.toByteArray(Charsets.UTF_8), Base64.DEFAULT)

                                val extraSymbols = json?.optInt(jsonKeyExtraSymbols, 0) ?: 0
                                if (text != null && iv != null) {
                                    val decryptedText = CryptoHelper.decryptText(sessionKey!!, text, iv)
                                    val decryptedTextString = String(decryptedText, Charsets.UTF_8).dropLast(extraSymbols)
                                    runOnUiThread {
                                        textTextView.text = decryptedTextString
                                    }
                                }
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

    private fun clickedLogout() {
        executor.execute {
            preferences.edit().remove(keyUsername).commit()
            runOnUiThread {
                finish()
                startActivity(Intent(this, LoginActivity::class.java))
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
}
