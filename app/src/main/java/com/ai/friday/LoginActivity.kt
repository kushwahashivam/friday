package com.ai.friday

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ai.friday.config.Config
import com.ai.friday.utils.ServerRequestLoader
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException

class LoginActivity : AppCompatActivity() {

    private val serverRequestLoader = ServerRequestLoader(Config.URL_LOGIN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun authenticate(view: View) {
        displayProgressBar()
        val username = et_username.text.trim()
        val password = et_password.text.trim()
        if (username.isEmpty() || password.isEmpty()) {
            hideProgressBar()
            showErrorMessage("username or password can not be empty")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val requestData = "{'username': '$username', 'password': '$password'}"
            val requestJsonObject = JSONObject(requestData)
            try {
                val responseJsonObject = serverRequestLoader.makeRequest(requestJsonObject)
                if (responseJsonObject == null) {
                    displayToast("Server connection error.")
                    hideProgressBar()
                    return@launch
                }
                val statusCode = responseJsonObject.getInt("status_code")
                if (statusCode == Config.CODE_SUCCESS) {
                    displayToast("Login successful.")
                    val file = File(filesDir, Config.TOKEN_FILE_NAME)
                    file.writeText(responseJsonObject.getString("access_token"))
                    hideProgressBar()
                    gotoDashboard()
                } else {
                    showErrorMessage(responseJsonObject.getString("message"))
                    hideProgressBar()
                }
            } catch (e: SocketTimeoutException) {
                displayToast("Socket timeout error.")
                hideProgressBar()
            } catch (e: ConnectException) {
                displayToast("Server may be offline.")
                hideProgressBar()
            }
        }
    }

    private fun showErrorMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            tv_auth_error_message.text = message
        }
    }

    private fun displayToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val toast = Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG)
            toast.show()
        }
    }

    private fun displayProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            btn_authenticate.visibility = View.INVISIBLE
            pb_authenticate.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            pb_authenticate.visibility = View.INVISIBLE
            btn_authenticate.visibility = View.VISIBLE
        }
    }

    private fun gotoDashboard() {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }
}