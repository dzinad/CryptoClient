package by.bsu.ddzina.lab2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var userNameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var progressBar: ProgressBar
    private val serverCommunicator = ServerCommunicator()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val username = preferences.getString(keyUsername, null)
        if (username != null) {
            loginSuccessful()
        }

        userNameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        progressBar = findViewById(R.id.loadingProgressBar)
        findViewById<Button>(R.id.loginBtn).setOnClickListener(this)
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
                    if (it == ServerCommunicator.LoginResult.OK) {
                        preferences.edit().putString(keyUsername, username).commit()
                    }
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        when (it) {
                            ServerCommunicator.LoginResult.OK -> {
                                loginSuccessful()
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
                        }
                    }
                }
            }
        }
    }

    private fun loginSuccessful() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }
}
