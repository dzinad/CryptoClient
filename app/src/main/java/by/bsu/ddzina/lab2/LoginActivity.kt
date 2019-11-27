package by.bsu.ddzina.lab2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var userNameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var verCodeEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var serverCommunicator: ServerCommunicator
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var preferences: SharedPreferences
    private val verCodeOnClickListener = View.OnClickListener { handleVerCodeClick() }
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        serverCommunicator = ServerCommunicator(applicationContext)
        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val username = preferences.getString(keyUsername, null)
        if (sessionId == null) {
            sessionId = preferences.getString(keySessionId, null)
        }
        if (username != null && sessionId != null) {
            println("read session id: $sessionId")
            loginSuccessful()
        }

        userNameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        verCodeEditText = findViewById(R.id.verCodeEditText)
        progressBar = findViewById(R.id.loadingProgressBar)
        findViewById<Button>(R.id.loginBtn).setOnClickListener(this)
        findViewById<Button>(R.id.verCodeBtn).setOnClickListener(verCodeOnClickListener)
    }

    override fun onClick(v: View?) {
        val username = userNameEditText.text.toString()
        val password = passwordEditText.text.toString()
        if (username.isBlank() || password.isBlank()) {
            Utils.showToast(applicationContext, "Enter username and password")
        } else {
            progressBar.visibility = View.VISIBLE
            executor.execute {
                serverCommunicator.requestLogin(username, password) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        when (it) {
                            ServerCommunicator.LoginResult.OK -> {
                                val verCodeBtn = findViewById<Button>(R.id.verCodeBtn)
                                verCodeEditText.visibility = View.VISIBLE
                                verCodeBtn.visibility = View.VISIBLE
                                this.username = username
                            }
                            ServerCommunicator.LoginResult.ERROR -> {
                                Utils.showToast(applicationContext, "Some error occurred. Try again.")
                            }
                            ServerCommunicator.LoginResult.WRONG_USERNAME -> {
                                Utils.showToast(applicationContext, "Wrong username.")
                            }
                            ServerCommunicator.LoginResult.WRONG_PASSWORD -> {
                                Utils.showToast(applicationContext, "Wrong password.")
                            }
                            ServerCommunicator.LoginResult.NO_SESSION_KEY -> {
                                Utils.showToast(applicationContext, "Request session key first.")
                            }
                            ServerCommunicator.LoginResult.BAD_TOKEN -> {
                                Utils.showToast(applicationContext, "Establish connection with server first: generate RSA.")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleVerCodeClick() {
        executor.execute {
            try {
                val code = verCodeEditText.text.toString().toInt()
                serverCommunicator.sendVerification(code) { result, sessionIdFromServer ->
                    if (result == ServerCommunicator.LoginResult.OK) {
                        sessionId = sessionIdFromServer
                        preferences.edit()
                            .putString(keyUsername, username)
                            .putString(keySessionId, sessionId)
                            .commit()
                    }
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        when (result) {
                            ServerCommunicator.LoginResult.OK -> {
                                loginSuccessful()
                            }
                            ServerCommunicator.LoginResult.WRONG_VERIFICATION_CODE -> {
                                Utils.showToast(applicationContext, "Wrong verification code.")
                            }
                            else -> {
                                Utils.showToast(applicationContext, "Some error. Try again")
                            }
                        }
                    }
                }
            } catch (ex: Throwable) {
                Log.e("[LoginActivity]", "Something went wrong while sending verification code", ex)
            }
        }
    }

    private fun loginSuccessful() {
        finish()
        startActivity(Intent(this, TextActivity::class.java))
    }
}
