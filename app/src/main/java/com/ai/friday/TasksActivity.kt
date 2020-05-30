package com.ai.friday

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.friday.config.Config
import com.ai.friday.task.Task
import com.ai.friday.task.TasksAdapter
import com.ai.friday.utils.Utils
import kotlinx.android.synthetic.main.activity_tasks.*
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException

class TasksActivity: AppCompatActivity(), TasksAdapter.ItemClickListener{

    private lateinit var accessToken: String

    private lateinit var viewManager: LinearLayoutManager
    private lateinit var viewAdapter: TasksAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tasksData: ArrayList<Task>

    private var taskProgressUpdater: Job = Job().apply { cancel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        /* Check for access token */
        val temp = Utils.getAccessToken(filesDir)
        if (temp == null) {
            Utils.gotoLogin(this)
        } else {
            accessToken = temp
        }
        /* ------------------------------------------------------------------------------------------ */

        /* Initiate recycler view */
        viewManager = LinearLayoutManager(this)
        viewAdapter = TasksAdapter(this)
        recyclerView = rv_tasks.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
        /* ------------------------------------------------------------------------------------------ */

        loadTasks()
    }

    private fun loadTasks(){
        CoroutineScope(Dispatchers.IO).launch {
            val newTasks = ArrayList<Task>()
            val tasksJson = JSONObject(File(filesDir, Config.TASKS_FILE_NAME).readText())
            val tasksArray = tasksJson.getJSONArray("tasks")
            for (i in 0 until tasksArray.length()) {
                val taskJson = tasksArray.getJSONObject(i)
                val taskType = taskJson.getInt("task_type")
                val token = taskJson.getString("token")
                val datetime = taskJson.getString("datetime")
                val thumbnail = Utils.base64ToBitmap(taskJson.getString("thumbnail"))
                val task = Task(taskType, token, datetime, thumbnail)
                newTasks.add(task)
            }
            updateTasksData(newTasks)
        }
    }

    private fun updateTasksData(newData: ArrayList<Task>){
        CoroutineScope(Dispatchers.Main).launch {
            tasksData = newData
            viewAdapter.data = newData
            viewAdapter.notifyDataSetChanged()
            startTaskProgressUpdater()
            if(tasksData.size > 0)
                tv_no_tasks.visibility = View.INVISIBLE
        }
    }

    private fun startTaskProgressUpdater(){
        if(!taskProgressUpdater.isActive){
            taskProgressUpdater = CoroutineScope(Dispatchers.IO).launch {
                while(isActive){
                    for(task in tasksData){
                        try {
                            val responseJSONObject = task.getProgress(accessToken)
                            if(responseJSONObject == null){
                                Utils.displayToast(this@TasksActivity, "Server connection error.")
                                break
                            }
                            when(responseJSONObject.getInt("status_code")){
                                Config.CODE_SUCCESS -> {
                                    task.progress = (responseJSONObject.getDouble("progress") * 100).toInt()
                                }
                                Config.CODE_TASK_NOT_FOUND -> Utils.displayToast(this@TasksActivity, "Task token: ${task.token} not found")
                                in Config.CODES_AUTH_ERROR -> Utils.gotoLogin(this@TasksActivity)
                                else -> Utils.displayToast(this@TasksActivity, responseJSONObject.getString("message"))
                            }
                        } catch (e: SocketTimeoutException) {
                            Utils.displayToast(this@TasksActivity, "Socket timeout error.")
                            break
                        } catch (e: ConnectException) {
                            Utils.displayToast(this@TasksActivity, "Server may be offline.")
                            break
                        }
                    }
                    updateTasksData(tasksData)
                    delay(Config.TASK_PROGRESS_UPDATE_DURATION)
                }
            }
        }
    }

    override fun onItemClick(position: Int) {
        if(tasksData[position].progress != 100){
            Utils.displayToast(this, "Task not yet completed.")
            return
        }
        if(tasksData[position].taskType == Config.TASK_INVESTIGATE_IMAGE) {
            val intent = Intent(this, ImageTaskResultActivity::class.java)
            intent.putExtra("token", tasksData[position].token)
            startActivity(intent)
        }
    }

    override fun onPause() {
        taskProgressUpdater.cancel()
        super.onPause()
    }

    override fun onStop() {
        taskProgressUpdater.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        taskProgressUpdater.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        if(accessToken.isNotEmpty())
            loadTasks()
        super.onResume()
    }

    override fun onStart() {
        if(accessToken.isNotEmpty())
            loadTasks()
        super.onStart()
    }

    override fun onRestart() {
        if(accessToken.isNotEmpty())
            loadTasks()
        super.onRestart()
    }
}