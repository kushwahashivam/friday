package com.ai.friday

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ai.friday.config.Config
import com.ai.friday.utils.ServerRequestLoader
import com.ai.friday.utils.Utils
import kotlinx.android.synthetic.main.activity_investigate_image.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class InvestigateImageActivity : AppCompatActivity() {

    private lateinit var accessToken: String
    private var photoUri: Uri? = null
    private lateinit var investigationImage: Bitmap

    private val detectFacesRequestLoader = ServerRequestLoader(Config.URL_DETECT_FACES)
    private val recognizeFacesRequestLoader = ServerRequestLoader(Config.URL_RECOGNIZE_FACES)

    private val FACE_UNSELECTED = 0
    private val FACE_SELECTED = 1

    private lateinit var detectedFaces: ArrayList<ImageButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investigate_image)

        /* Check for access token */
        val temp = Utils.getAccessToken(filesDir)
        if (temp == null) {
            Utils.gotoLogin(this)
        } else {
            accessToken = temp
        }
        /* ------------------------------------------------------------------------------------------ */

        val bundle = intent.extras
        if(bundle != null){
            photoUri = bundle.getParcelable("photoUri")
        }else{
            Utils.displayToast(this, "No data found in intent")
            finish()
        }
        if(photoUri == null){
            finish()
        }
        CoroutineScope(Dispatchers.IO).launch {
            openImage()
            displayImage()
        }
    }

    private fun goBack(){
        CoroutineScope(Dispatchers.Main).launch {
            finish()
        }
    }

    fun goBack(view: View){
        finish()
    }

    private fun openImage(){
        val source = ImageDecoder.createSource(contentResolver, photoUri!!)
        investigationImage = ImageDecoder.decodeBitmap(source)
        if(investigationImage.width > Config.MAX_IMAGE_SIZE || investigationImage.height > Config.MAX_IMAGE_SIZE){
            val newWidth: Int
            val newHeight: Int
            if(investigationImage.width > investigationImage.height){
                newWidth = Config.MAX_IMAGE_SIZE
                newHeight = ((investigationImage.height * 1.0 /investigationImage.width) * newWidth).toInt()
            } else {
                newHeight = Config.MAX_IMAGE_SIZE
                newWidth = ((investigationImage.width * 1.0 /investigationImage.height) * newHeight).toInt()
            }
            investigationImage = Bitmap.createScaledBitmap(investigationImage, newWidth, newHeight, true)
        }
    }

    private fun displayImage(){
        CoroutineScope(Dispatchers.Main).launch {
            tv_photo_missing.visibility = View.INVISIBLE
            ll_recognize_faces.visibility = View.INVISIBLE
            ll_detect_faces.visibility = View.VISIBLE
            iv_investigation_image.setImageBitmap(investigationImage)
        }
    }

    fun detectFaces(view: View){
        CoroutineScope(Dispatchers.IO).launch {
            displayPBDetectFaces()

            val imgName: String = UUID.randomUUID().toString() + ".jpeg"
            val imgData = Utils.bitmapToBase64(investigationImage)
            var requestData = "{'access_token': '$accessToken', "
            requestData += "'image_name': '$imgName', "
            requestData += "'image_data': '$imgData'}"
            val requestJsonObject = JSONObject(requestData)
            try{
                val responseJsonObject = detectFacesRequestLoader.makeRequest(requestJsonObject)
                if (responseJsonObject == null) {
                    Utils.displayToast(this@InvestigateImageActivity, "Server connection error.")
                    hidePBDetectFaces()
                    return@launch
                }
                when (responseJsonObject.getInt("status_code")) {
                    Config.CODE_SUCCESS -> {
                        val bboxes = responseJsonObject.getJSONArray("bboxes")
                        detectedFaces = ArrayList()
                        for(i in 0 until  bboxes.length()){
                            val bbox = bboxes.getJSONArray(i)
                            val imageButton = layoutInflater.inflate(R.layout.face_image, gl_faces, false) as ImageButton
                            val x1 = bbox.getInt(0)
                            val y1 = bbox.getInt(1)
                            val x2 = bbox.getInt(2)
                            val y2 = bbox.getInt(3)
                            val w = x2 - x1
                            val h = y2 - y1
                            val face = Bitmap.createBitmap(investigationImage, x1, y1, w, h)
                            imageButton.setImageBitmap(face)
                            imageButton.tag = mutableListOf(x1, y1, x2, y2, FACE_UNSELECTED)
                            imageButton.setOnClickListener {
                                val t = it.tag as MutableList<Int>
                                if(t[4] == FACE_UNSELECTED){
                                    t[4] = FACE_SELECTED
                                    it.setBackgroundColor(ContextCompat.getColor(this@InvestigateImageActivity, R.color.red))
                                } else {
                                    t[4] = FACE_UNSELECTED
                                    it.setBackgroundResource(R.drawable.yellow_green_gradient_rectangle)
                                }
                                it.tag = t
                            }
                            detectedFaces.add(imageButton)
                        }
                        displayFaceGrid()
                    }
                    in Config.CODES_AUTH_ERROR -> Utils.gotoLogin(this@InvestigateImageActivity)
                    else -> Utils.displayToast(this@InvestigateImageActivity, responseJsonObject.getString("message"))
                }
            } catch (e: SocketTimeoutException) {
                Utils.displayToast(this@InvestigateImageActivity, "Socket timeout error.")
            } catch (e: ConnectException) {
                Utils.displayToast(this@InvestigateImageActivity, "Server may be offline.")
            }
        }
    }

    fun recognizeFaces(view: View){
        displayPBRecognizeFaces()
        CoroutineScope(Dispatchers.IO).launch {
            val imgName: String = UUID.randomUUID().toString() + ".jpeg"
            val imgData = Utils.bitmapToBase64(investigationImage)
            var requestData = "{'access_token': '$accessToken', "
            requestData += "'image_name': '$imgName', "
            requestData += "'image_data': '$imgData', "
            requestData += "'bboxes': ["
            var first = true
            for(i in 0 until detectedFaces.size){
                val tag = detectedFaces[i].tag as MutableList<Int>
                if(tag[4] == FACE_SELECTED){
                    if(!first){
                        requestData += ", "
                    } else {
                        first = false
                    }
                    requestData += "[${tag[0]}, ${tag[1]}, ${tag[2]}, ${tag[3]}]"
                }
            }
            requestData += "]}"
            val requestJsonObject = JSONObject(requestData)
            try {
                val responseJsonObject = recognizeFacesRequestLoader.makeRequest(requestJsonObject)
                if (responseJsonObject == null) {
                    Utils.displayToast(this@InvestigateImageActivity, "Server connection error.")
                    hidePBRecognizeFaces()
                    return@launch
                }
                when(responseJsonObject["status_code"]){
                    Config.CODE_SUCCESS -> {
                        val datetime = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val token = responseJsonObject.getString("token")
                        var newTaskData = "{'token': '$token', "
                        newTaskData += "'task_type': ${Config.TASK_INVESTIGATE_IMAGE}, "
                        newTaskData += "'datetime': '${formatter.format(datetime)}', "
                        newTaskData += "'thumbnail': '$imgData'}"
                        val tasksJsonObject = JSONObject(File(filesDir, Config.TASKS_FILE_NAME).readText())
                        tasksJsonObject.getJSONArray("tasks").put(JSONObject(newTaskData))
                        File(filesDir, Config.TASKS_FILE_NAME).writeText(tasksJsonObject.toString())
                        Utils.displayToast(this@InvestigateImageActivity, "Face matching task created.")
                        goBack()
                        return@launch
                    }
                    in Config.CODES_AUTH_ERROR -> Utils.gotoLogin(this@InvestigateImageActivity)
                    else -> Utils.displayToast(this@InvestigateImageActivity, responseJsonObject.getString("message"))
                }
            }catch (e: SocketTimeoutException) {
                Utils.displayToast(this@InvestigateImageActivity, "Socket timeout error.")
            } catch (e: ConnectException) {
                Utils.displayToast(this@InvestigateImageActivity, "Server may be offline.")
            }
        }
    }

    private fun displayFaceGrid(){
        CoroutineScope(Dispatchers.Main).launch {
            tv_photo_missing.visibility = View.INVISIBLE
            ll_detect_faces.visibility = View.INVISIBLE
            ll_recognize_faces.visibility = View.VISIBLE
            for(imageButton in detectedFaces){
                gl_faces.addView(imageButton)
            }
        }
    }

    private fun displayPBDetectFaces(){
        CoroutineScope(Dispatchers.Main).launch {
            ib_detect_faces.visibility = View.INVISIBLE
            ib_finish_df.visibility = View.INVISIBLE
            pb_detect_faces.visibility = View.VISIBLE
        }
    }

    private fun hidePBDetectFaces(){
        CoroutineScope(Dispatchers.Main).launch {
            pb_detect_faces.visibility = View.INVISIBLE
            ib_detect_faces.visibility = View.VISIBLE
            ib_finish_df.visibility = View.VISIBLE
        }
    }

    private fun displayPBRecognizeFaces(){
        CoroutineScope(Dispatchers.Main).launch {
            ib_recognize_faces.visibility = View.INVISIBLE
            ib_finish_rf.visibility = View.INVISIBLE
            pb_recognize_faces.visibility = View.VISIBLE
        }
    }

    private fun hidePBRecognizeFaces(){
        CoroutineScope(Dispatchers.Main).launch {
            pb_recognize_faces.visibility = View.INVISIBLE
            ib_recognize_faces.visibility = View.VISIBLE
            ib_finish_rf.visibility = View.VISIBLE
        }
    }
}