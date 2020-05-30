package com.ai.friday

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.ai.friday.config.Config
import com.ai.friday.utils.ServerRequestLoader
import com.ai.friday.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var accessToken: String = ""
    private var systemInfoUpdaterJob: Job = Job().apply { cancel() }
    private val REQUEST_IMAGE_GET = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Create tasks file if not present */
        val file = File(filesDir, Config.TASKS_FILE_NAME)
        if(!file.exists()){
            file.writeText("{'tasks': []}")
        }
        /* ------------------------------------------------------------------------------------------ */

        /* Check for access token */
        val temp = Utils.getAccessToken(filesDir)
        if (temp == null) {
            Utils.gotoLogin(this)
            return
        } else {
            accessToken = temp
        }
        /* ------------------------------------------------------------------------------------------ */

        /* Start system info updater */
        startSystemInfoUpdater()
        /* ------------------------------------------------------------------------------------------ */
    }

    private fun startSystemInfoUpdater() {
        if (!systemInfoUpdaterJob.isActive) {
            systemInfoUpdaterJob = CoroutineScope(Dispatchers.IO).launch {
                val serverRequestLoader = ServerRequestLoader(Config.URL_HOME)
                val requestData = "{'access_token': '$accessToken'}"
                val requestJsonObject = JSONObject(requestData)
                while (isActive) {
                    try {
                        val responseJsonObject = serverRequestLoader.makeRequest(requestJsonObject)
                        if (responseJsonObject == null) {
                            Utils.displayToast(this@MainActivity, "Server connection error.")
                            return@launch
                        }
                        when (responseJsonObject.getInt("status_code")) {
                            Config.CODE_SUCCESS -> updateSystemInfo(responseJsonObject)
                            in Config.CODES_AUTH_ERROR -> Utils.gotoLogin(this@MainActivity)
                            else -> Utils.displayToast(
                                this@MainActivity,
                                responseJsonObject.getString("message")
                            )
                        }
                        delay(Config.SYSTEM_INFO_UPDATE_DURATION)
                    } catch (e: SocketTimeoutException) {
                        Utils.displayToast(this@MainActivity, "Socket timeout error.")
                    } catch (e: ConnectException) {
                        Utils.displayToast(this@MainActivity, "Server may be offline.")
                    }
                }
            }
        }
    }

    private fun updateSystemInfo(jsonObject: JSONObject) {
        CoroutineScope(Dispatchers.Main).launch {
            val cpuCount = jsonObject.getInt("cpu_count")
            val cpuPercent = jsonObject.getDouble("cpu_percent").roundToInt()
            val cpuFrequency = jsonObject.getDouble("cpu_frequency").roundToInt()
            val totalMemory = jsonObject.getDouble("total_memory").roundToInt()
            val availableMemory = jsonObject.getDouble("available_memory").roundToInt()
            val totalSwap = jsonObject.getDouble("total_swap").roundToInt()
            val availableSwap = jsonObject.getDouble("available_swap").roundToInt()
            val totalDisk = jsonObject.getDouble("total_disk").roundToInt()
            val availableDisk = jsonObject.getDouble("available_disk").roundToInt()
            val totalTasks = jsonObject.getInt("total_tasks")
            val completedTasks = jsonObject.getInt("completed_tasks")

            val cpuStr = "$cpuCount CPUs / $cpuPercent% / $cpuFrequency MHz"
            updateProgressAnimate(pb_cpu, cpuPercent*100)
            tv_cpu.text = cpuStr

            val memoryPercent: Int = (totalMemory - availableMemory) * 100 / totalMemory
            val memoryStr = "${totalMemory - availableMemory} / $totalMemory MB"
            updateProgressAnimate(pb_memory, memoryPercent*100)
            tv_memory.text = memoryStr

            val swapPercent: Int = (totalSwap - availableSwap) * 100 / totalSwap
            val swapStr = "${totalSwap - availableSwap} / $totalSwap MB"
            updateProgressAnimate(pb_swap, swapPercent*100)
            tv_swap.text = swapStr

            val diskPercent: Int = (totalDisk - availableDisk) * 100 / totalDisk
            val diskStr = "${totalDisk - availableDisk} / $totalDisk GB"
            updateProgressAnimate(pb_disk, diskPercent*100)
            tv_disk.text = diskStr

            val taskPercent: Int = if (totalTasks > 0) completedTasks * 100 / totalTasks else 100
            val taskStr = "$completedTasks / $totalTasks"
            updateProgressAnimate(pb_task, taskPercent*100)
            tv_task.text = taskStr
        }
    }

    private fun updateProgressAnimate(pb: ProgressBar, progress: Int){
        val animator = ObjectAnimator.ofInt(pb, "progress", pb.progress, progress)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    override fun onPause() {
        systemInfoUpdaterJob.cancel()
        super.onPause()
    }

    override fun onStop() {
        systemInfoUpdaterJob.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        systemInfoUpdaterJob.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        if(accessToken.isNotEmpty())
            startSystemInfoUpdater()
        super.onResume()
    }

    override fun onStart() {
        if(accessToken.isNotEmpty())
            startSystemInfoUpdater()
        super.onStart()
    }

    override fun onRestart() {
        if(accessToken.isNotEmpty())
            startSystemInfoUpdater()
        super.onRestart()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_IMAGE_GET && resultCode == Activity.RESULT_OK){
            val photoUri: Uri? = data?.data
            val intent = Intent(this, InvestigateImageActivity::class.java).apply {
                putExtra("photoUri", photoUri)
            }
            startActivity(intent)
        }
    }

    fun launchTasksActivity(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(this@MainActivity, TasksActivity::class.java)
            startActivity(intent)
        }
    }

    fun launchSearchPersonActivity(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(this@MainActivity, SearchPersonActivity::class.java)
            startActivity(intent)
        }
    }

    fun launchInvestigateImageActivity(view: View) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        if(intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }
    }

    fun launchVideoAnalysisActivity(view: View) {
    }

    fun launchStarredPersonsActivity(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(this@MainActivity, StarredPersonsActivity::class.java)
            startActivity(intent)
        }
    }

    fun launchLiveFeedActivity(view: View) {
    }

    fun launchAddIdentityActivity(view: View) {
    }

    fun launchAboutActivity(view: View){
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(this@MainActivity, AboutActivity::class.java)
            startActivity(intent)
        }
    }
}
