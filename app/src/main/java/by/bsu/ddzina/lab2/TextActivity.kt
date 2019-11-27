package by.bsu.ddzina.lab2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executors

class TextActivity : AppCompatActivity(), View.OnClickListener {

    private val READ_REQUEST_CODE: Int = 42
    private lateinit var serverCommunicator: ServerCommunicator
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var preferences: SharedPreferences
    private lateinit var textTextView: TextView
    private lateinit var requestFileTextView: EditText
    private lateinit var filenameTextView: TextView
    private var chosenFile: Uri? = null
    private var chosenFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)
        serverCommunicator = ServerCommunicator(applicationContext)
        textTextView = findViewById(R.id.textTextView)
        requestFileTextView = findViewById(R.id.requestFileEditText)
        filenameTextView = findViewById(R.id.filenameTextView)
        findViewById<Button>(R.id.requestTextBtn).setOnClickListener(this)
        findViewById<Button>(R.id.chooseFileBtn).setOnClickListener(this)
        findViewById<Button>(R.id.sendFileBtn).setOnClickListener(this)
        findViewById<Button>(R.id.logoutBtn).setOnClickListener(this)
        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.requestTextBtn -> clickedRequestText()
                R.id.chooseFileBtn -> clickedChooseFile()
                R.id.logoutBtn -> clickedLogout()
                R.id.sendFileBtn -> clickedSendFile()
            }
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun clickedRequestText() {
        val fileName = requestFileTextView.text.toString()
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
                                jsonValueAuthorizationError -> handleAuthorizationError()
                            }
                        } else {
                            try {
                                println("parse file response: ${String(Base64.encode(sessionKey!!.encoded, Base64.DEFAULT))}")
                                val content = json?.optJSONObject(jsonKeyFileContent)
                                val decryptedTextString = CryptoHelper.decryptAes(sessionKey!!, content)
                                runOnUiThread {
                                    textTextView.text = decryptedTextString
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

    private fun clickedChooseFile() {
        executor.execute {
            performFileSearch()
        }
    }

    private fun performFileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            executor.execute {
                resultData?.data?.also { uri ->
                    val fileName = readFileName(uri)
                    runOnUiThread {
                        filenameTextView.text = fileName
                    }
                    chosenFile = uri
                    chosenFileName = fileName
                }
            }
        }
    }

    private fun clickedLogout() {
        executor.execute {
            serverCommunicator.sendLogout {
                if (it) {
                    sessionId = null
                    preferences.edit().remove(keyUsername).commit()
                    runOnUiThread {
                        finish()
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                } else {
                    runOnUiThread {
                        Utils.showToast(applicationContext, "Could not log out")
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

    private fun handleAuthorizationError() {
        runOnUiThread {
            Utils.showToast(applicationContext, "Authorization error.")
        }
    }

    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun readFileName(uri: Uri): String {
        val cursor: Cursor? = contentResolver.query( uri, null, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return "unknown"
    }

    private fun clickedSendFile() {
        chosenFile?.let {
            executor.execute {
                val fileContent = readTextFromUri(it)
                val fileName = chosenFileName!!
                chosenFile = null
                chosenFileName = null
                serverCommunicator.sendFile(fileName, fileContent) { message ->
                    runOnUiThread {
                        filenameTextView.text = ""
                        Utils.showToast(applicationContext, message)
                    }
                }
            }
        }
    }
}
