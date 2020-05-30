package com.ai.friday.utils

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.friday.LoginActivity
import com.ai.friday.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.nio.charset.StandardCharsets

class Utils {
    companion object {
        fun getAccessToken(filesDir: File?): String? {
            var accessToken: String? = null
            val tokenFile = File(filesDir, Config.TOKEN_FILE_NAME)
            if (!tokenFile.exists()) {
                try {
                    val writer = FileWriter(tokenFile)
                    writer.write("")
                    writer.flush()
                    writer.close()
                } catch (e: IOException) { }
            }
            try {
                val inputStream = FileInputStream(tokenFile)
                val inputStreamReader =
                    InputStreamReader(inputStream, StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)
                accessToken = bufferedReader.readLine()
            } catch (e: FileNotFoundException) {
            } catch (e: IOException) { }
            return accessToken
        }

        fun displayToast(context: AppCompatActivity, message: String){
            CoroutineScope(Dispatchers.Main).launch {
                val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
                toast.show()
            }
        }

        fun gotoLogin(context: AppCompatActivity){
            CoroutineScope(Dispatchers.Main).launch {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            }
        }

        fun bitmapToBase64(bitmap: Bitmap): String{
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            var imgBytes = byteArrayOutputStream.toByteArray()
            imgBytes = Base64.encode(imgBytes, Base64.DEFAULT)
            return imgBytes.toString(StandardCharsets.UTF_8)
        }

        fun base64ToBitmap(str: String): Bitmap{
            var imgBytes = str.toByteArray(StandardCharsets.UTF_8)
            imgBytes = Base64.decode(imgBytes, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
        }
    }
}