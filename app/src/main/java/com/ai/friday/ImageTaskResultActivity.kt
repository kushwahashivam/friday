package com.ai.friday

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ai.friday.config.Config
import com.ai.friday.task.Task
import com.ai.friday.utils.PersonData
import com.ai.friday.utils.ServerRequestLoader
import com.ai.friday.utils.Utils
import kotlinx.android.synthetic.main.activity_image_task_result.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException

class ImageTaskResultActivity: AppCompatActivity(){

    private lateinit var accessToken: String
    private var task: Task? = null
    private lateinit var taskResult: JSONObject
    private var faceViewsList = ArrayList<View>()

    private var handleFaceTap = true

    private val personDataRequestLoader = ServerRequestLoader(Config.URL_GET_PERSON_DATA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_task_result)

        /* Check for access token */
        val temp = Utils.getAccessToken(filesDir)
        if (temp == null) {
            Utils.gotoLogin(this)
        } else {
            accessToken = temp
        }
        /* ------------------------------------------------------------------------------------------ */

        loadTask(intent.getStringExtra("token")!!)
    }

    private fun loadTask(token: String){
        CoroutineScope(Dispatchers.IO).launch {
            val tasksJson = JSONObject(File(filesDir, Config.TASKS_FILE_NAME).readText())
            val tasksArray = tasksJson.getJSONArray("tasks")
            for (i in 0 until tasksArray.length()) {
                val taskJson = tasksArray.getJSONObject(i)
                val taskType = taskJson.getInt("task_type")
                val t = taskJson.getString("token")
                val datetime = taskJson.getString("datetime")
                val thumbnail = Utils.base64ToBitmap(taskJson.getString("thumbnail"))
                if(t == token) {
                    task = Task(taskType, token, datetime, thumbnail)
                    break
                }
            }
            loadTaskResult()
        }
    }

    private suspend fun loadTaskResult(){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val responseJsonObject = task?.getResult(accessToken)
                if(responseJsonObject == null){
                    Utils.displayToast(this@ImageTaskResultActivity, "Server connection error.")
                    goBack()
                    return@launch
                }
                taskResult = responseJsonObject
                when(taskResult.getInt("status_code")){
                    Config.CODE_SUCCESS -> {
                        val jsonArray = taskResult.getJSONArray("result")
                        faceViewsList = ArrayList()
                        for(i in 0 until jsonArray.length()){
                            val result = jsonArray.getJSONObject(i)
                            val bbox = result.getJSONArray("bbox")
                            val x1 = bbox.getInt(0)
                            val y1 = bbox.getInt(1)
                            val x2 = bbox.getInt(2)
                            val y2 = bbox.getInt(3)
                            val w = x2 - x1
                            val h = y2 - y1
                            val view = layoutInflater.inflate(R.layout.face_image, ll_investigation_faces, false) as ImageButton
                            view.apply {
                                view.setImageBitmap(Bitmap.createBitmap(task?.thumbnail!!, x1, y1, w, h))
                                tag = result.getJSONArray("matched_faces")
                                setOnClickListener { loadMatchedFaces(this) }
                            }
                            faceViewsList.add(view)
                        }
                        updateInvestigationFaces()
                    }
                    Config.CODE_TASK_NOT_COMPLETED -> {
                        Utils.displayToast(this@ImageTaskResultActivity, "Task not completed")
                        goBack()
                        return@launch
                    }
                    Config.CODE_TASK_NOT_FOUND -> {
                        Utils.displayToast(this@ImageTaskResultActivity, "Task not found")
                        goBack()
                        return@launch
                    }
                    in Config.CODES_AUTH_ERROR -> {
                        Utils.gotoLogin(this@ImageTaskResultActivity)
                        goBack()
                        return@launch
                    }
                    else -> {
                        Utils.displayToast(this@ImageTaskResultActivity, taskResult.getString("message"))
                        goBack()
                        return@launch
                    }
                }
            } catch (e: SocketTimeoutException) {
                Utils.displayToast(this@ImageTaskResultActivity, "Socket timeout error.")
            } catch (e: ConnectException) {
                Utils.displayToast(this@ImageTaskResultActivity, "Server may be offline.")
            }
        }
    }

    private fun updateInvestigationFaces(){
        CoroutineScope(Dispatchers.Main).launch {
            for(view in faceViewsList)
                ll_investigation_faces.addView(view)
        }
    }

    private fun loadMatchedFaces(view: View){
        if(!handleFaceTap)
            return
        handleFaceTap = false
        resetFacesBackground()
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        displayPBMatchedFaces()
        CoroutineScope(Dispatchers.IO).launch {
            val faceViewsList = ArrayList<View>()
            val viewTag = view.tag as JSONArray
            for(i in 0 until viewTag.length()){
                val requestJsonObject = JSONObject("{'access_token': '$accessToken', 'id': '${viewTag.getString(i)}'}")
                try {
                    val responseJsonObject = personDataRequestLoader.makeRequest(requestJsonObject)
                    if(responseJsonObject == null){
                        Utils.displayToast(this@ImageTaskResultActivity, "Server connection error.")
                        return@launch
                    }
                    when(responseJsonObject.getInt("status_code")){
                        Config.CODE_SUCCESS -> {
                            val data = responseJsonObject.getJSONObject("data")
                            val id = data.getString("id")
                            val name = data.getJSONObject("data").getString("name")
                            val image = Utils.base64ToBitmap(data.getString("image"))
                            val star = data.getInt("star")
                            var remData = ""
                            val iterator: Iterator<String> = data.getJSONObject("data").keys()
                            for (key in iterator) {
                                if (key == "name")
                                    continue
                                remData += "$key: "
                                remData += data.getJSONObject("data").getString(key)
                                remData += "\n\n"
                            }
                            val faceView = layoutInflater.inflate(R.layout.face_image_with_name, gl_matched_faced, false)
                            faceView.apply {
                                tag = PersonData(id, name, image, remData, star)
                                findViewById<ImageView>(R.id.iv_face_image).setImageBitmap(image)
                                findViewById<TextView>(R.id.tv_person_name).text = name
                                setOnClickListener { showPersonData(this) }
                            }
                            faceViewsList.add(faceView)
                        }
                        in Config.CODES_AUTH_ERROR -> {
                            Utils.gotoLogin(this@ImageTaskResultActivity)
                            return@launch
                        }
                        else -> Utils.displayToast(this@ImageTaskResultActivity, responseJsonObject.getString("message"))
                    }
                } catch (e: SocketTimeoutException) {
                    Utils.displayToast(this@ImageTaskResultActivity, "Socket timeout error.")
                } catch (e: ConnectException) {
                    Utils.displayToast(this@ImageTaskResultActivity, "Server may be offline.")
                }
                updateProgress(100*(i+1)/viewTag.length())
            }
            displayMatchedFaces(faceViewsList)
        }
    }

    private fun displayMatchedFaces(faceViewsList: ArrayList<View>){
        CoroutineScope(Dispatchers.Main).launch {
            gl_matched_faced.removeAllViews()
            for(view in faceViewsList)
                gl_matched_faced.addView(view)
            tv_no_face_match.visibility = if(faceViewsList.size > 0) View.INVISIBLE else View.VISIBLE
            handleFaceTap = true
            hidePBMatchedFaces()
        }
    }

    private fun showPersonData(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
        }
    }

    private fun resetFacesBackground(){
        for(view in faceViewsList)
            view.setBackgroundResource(R.drawable.yellow_green_gradient_rectangle)
    }

    private fun goBack(){
        CoroutineScope(Dispatchers.Main).launch {
            finish()
        }
    }

    private fun updateProgress(progress: Int){
        CoroutineScope(Dispatchers.Main).launch {
            pb_loading_matched_faces.progress = progress
        }
    }

    private fun displayPBMatchedFaces(){
        CoroutineScope(Dispatchers.Main).launch {
            tv_tap_face.visibility = View.INVISIBLE
            sv_matched_faces.visibility = View.INVISIBLE
            pb_loading_matched_faces.visibility = View.VISIBLE
        }
    }

    private fun hidePBMatchedFaces(){
        CoroutineScope(Dispatchers.Main).launch {
            pb_loading_matched_faces.visibility = View.INVISIBLE
            sv_matched_faces.visibility = View.VISIBLE
        }
    }
}