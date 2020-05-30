package com.ai.friday.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ai.friday.R

class TasksAdapter(val itemClickListener: ItemClickListener): RecyclerView.Adapter<TasksAdapter.TaskViewHolder>(){

    var data: List<Task>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksAdapter.TaskViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.task_card, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun onBindViewHolder(holder: TasksAdapter.TaskViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class TaskViewHolder(private val view: View): RecyclerView.ViewHolder(view){
        fun bind(position: Int){
            if(data == null)
                return
            view.apply {
                findViewById<ImageView>(R.id.iv_task_thumbnail).setImageBitmap(data?.get(position)?.thumbnail)
                findViewById<TextView>(R.id.tv_datetime).text = data?.get(position)?.datetime
                findViewById<ProgressBar>(R.id.pb_task_progress).progress = data?.get(position)?.progress ?: 0
                findViewById<ImageView>(R.id.iv_task_done).visibility = if(data?.get(position)?.progress == 100) View.VISIBLE else View.INVISIBLE
                setOnClickListener{itemClickListener.onItemClick(position)}
            }
        }
    }

    interface ItemClickListener{
        fun onItemClick(position: Int)
    }
}