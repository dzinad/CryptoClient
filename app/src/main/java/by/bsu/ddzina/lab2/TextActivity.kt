package by.bsu.ddzina.lab2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors

class TextActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)
        textView = findViewById(R.id.textTextView)
        executor.execute {
            val content = String(Utils.readFile(applicationContext, textFileName), Charsets.UTF_8)
            runOnUiThread {
                textView.text = content
            }
        }
    }
}
