package com.arriva.touristguideapp.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
            .inflate(R.layout.item_activity_timeline_premium, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivActivityIcon)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvActivityDescription)
        private val tvTime: TextView = itemView.findViewById(R.id.tvActivityTime)

        fun bind(item: ProfileActivityItem) {
            // Set appropriate icon based on activity type
            ivIcon.setImageResource(getActivityIcon(item.type))
            
            tvDescription.text = item.description
            val relative = RelativeTimeFormatter.format(item.timestamp)
            if (relative.isNotEmpty()) {
                tvTime.text = relative
                tvTime.visibility = View.VISIBLE
            } else {
                tvTime.visibility = View.GONE
            }
        }
        
        private fun getActivityIcon(type: ProfileActivityItem.Type): Int {
            return when (type) {
                ProfileActivityItem.Type.TRIP -> R.drawable.ic_trip
                ProfileActivityItem.Type.FAVORITE -> R.drawable.ic_favorite
                ProfileActivityItem.Type.REVIEW -> R.drawable.ic_star
                ProfileActivityItem.Type.SOS -> R.drawable.ic_notification
                ProfileActivityItem.Type.OTHER -> R.drawable.ic_account
            }
        }
    }
}
