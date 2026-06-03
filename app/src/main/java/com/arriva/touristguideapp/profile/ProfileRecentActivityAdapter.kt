package com.arriva.touristguideapp.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.R

class ProfileRecentActivityAdapter(
    private var items: List<ProfileActivityItem> = emptyList()
) : RecyclerView.Adapter<ProfileRecentActivityAdapter.ViewHolder>() {

    fun submitList(newItems: List<ProfileActivityItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_activity_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDescription: TextView = itemView.findViewById(R.id.tvActivityDescription)
        private val tvTime: TextView = itemView.findViewById(R.id.tvActivityTime)

        fun bind(item: ProfileActivityItem) {
            tvDescription.text = item.description
            val relative = RelativeTimeFormatter.format(item.timestamp)
            if (relative.isNotEmpty()) {
                tvTime.text = relative
                tvTime.visibility = View.VISIBLE
            } else {
                tvTime.visibility = View.GONE
            }
        }
    }
}
