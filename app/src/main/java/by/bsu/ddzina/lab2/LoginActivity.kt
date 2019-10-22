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
    private lateinit var serverCommunicator: ServerCommunicator
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val username = preferences.getString(keyUsername, null)
        println("[ddlog] 2 ${username}")
        if (username != null) {
            loginSuccessful()
        }

        userNameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        progressBar = findViewById(R.id.loadingProgressBar)
        serverCommunicator = ServerCommunicator(applicationContext)
        findViewById<Button>(R.id.loginBtn).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val username = userNameEditText.text.toString()
        val password = passwordEditText.text.toString()
        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(applicationContext, "Enter username and password", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(applicationContext, "Some error occurred. Try again.", Toast.LENGTH_SHORT).show()
                            }
                            ServerCommunicator.LoginResult.WRONG_USERNAME -> {
                                Toast.makeText(applicationContext, "Wrong username", Toast.LENGTH_SHORT).show()
                            }
                            ServerCommunicator.LoginResult.WRONG_PASSWORD -> {
                                Toast.makeText(applicationContext, "Wrong password", Toast.LENGTH_SHORT).show()
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
