package by.bsu.ddzina.lab2

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Utils {

    fun readFile(context: Context, fileName: String): ByteArray {
        val file = File(context.filesDir, fileName)
        if (file.exists() == false) {
            throw RuntimeException("File $fileName does not exist.")
        } else {
            return file.readBytes()
        }
    }

    fun saveFile(context: Context, fileName: String, content: ByteArray) {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists() == false) {
                file.createNewFile()
            }
            val buffer = BufferedOutputStream(FileOutputStream(file))
            buffer.write(content)
            buffer.close()
        } catch (ex: Throwable) {
            Log.e("[Utils]", "Something went wrong while writing to file $fileName")
            ex.printStackTrace()
        }
    }

    @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
    fun getMD5Hash(input: String): ByteArray {
        val messageDigest = MessageDigest.getInstance("MD5")
        messageDigest.reset()
        messageDigest.update(input.toByteArray(Charsets.UTF_8))
        return messageDigest.digest()
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}