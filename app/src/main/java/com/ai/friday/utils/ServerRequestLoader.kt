package com.ai.friday.utils

import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

class ServerRequestLoader(urlString: String){
    private var url: URL? = URL(urlString)

    fun makeRequest(requestJsonObject: JSONObject): JSONObject? {
        var responseJsonObject: JSONObject?
        val requestData: String = requestJsonObject.toString()
        with(url?.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            doInput = true
            setChunkedStreamingMode(0)
            connectTimeout = 30000
            readTimeout = 30000
            with(outputStream as OutputStream){
                val writer = BufferedWriter(OutputStreamWriter(this, StandardCharsets.UTF_8))
                writer.write(requestData)
                writer.flush()
            }
            with(inputStream as InputStream){
                val reader = BufferedReader(InputStreamReader(this, StandardCharsets.UTF_8))
                val responseData: String = reader.lines().collect(Collectors.joining())
                responseJsonObject = JSONObject(responseData)
            }
        }
        return responseJsonObject
    }
}