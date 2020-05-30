package com.ai.friday.searchperson

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ai.friday.R
import com.ai.friday.utils.PersonData

class SearchPersonAdapter(private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<SearchPersonAdapter.PersonViewHolder>() {

    var data: List<PersonData>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.person_card, parent, false)
        return PersonViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class PersonViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(position: Int) {
            if (data.isNullOrEmpty())
                return
            view.apply {
                findViewById<ImageView>(R.id.iv_person_image).setImageBitmap(data?.get(position)?.image)
                findViewById<TextView>(R.id.tv_person_name).text = data?.get(position)?.name
                val star = data?.get(position)?.star ?: 0
                if (star == 1)
                    findViewById<ImageView>(R.id.iv_star_person).setImageResource(R.drawable.star_red)
                else
                    findViewById<ImageView>(R.id.iv_star_person).setImageResource(R.drawable.star_green)
                findViewById<ImageView>(R.id.iv_star_person).setOnClickListener {
                    itemClickListener.toggleStar(adapterPosition)
                }
                setOnClickListener { itemClickListener.onItemClick(adapterPosition) }
            }
        }
    }

    interface ItemClickListener {
        fun onItemClick(position: Int)
        fun toggleStar(position: Int)
    }
}