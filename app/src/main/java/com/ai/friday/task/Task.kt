package com.ai.friday.task

import android.graphics.Bitmap
import com.ai.friday.config.Config
import com.ai.friday.utils.ServerRequestLoader
import org.json.JSONObject
import java.io.Serializable

class Task(val taskType: Int,
           val token: String,
           val datetime: String,
           val thumbnail: Bitmap){

    var progress: Int = 0

    private val progressRequestLoader = ServerRequestLoader(Config.URL_GET_TASK_PROGRESS)
    private val resultRequestLoader = ServerRequestLoader(Config.URL_GET_TASK_RESULT)

    suspend fun getProgress(accessToken: String): JSONObject? {
        val requestData = "{'access_token': '$accessToken', 'token': '$token'}"
        val requestJsonObject = JSONObject(requestData)
        return progressRequestLoader.makeRequest(requestJsonObject)
    }

    suspend fun getResult(accessToken: String): JSONObject? {
        val requestData = "{'access_token': '$accessToken', 'token': '$token'}"
        val requestJsonObject = JSONObject(requestData)
        return resultRequestLoader.makeRequest(requestJsonObject)
    }
}