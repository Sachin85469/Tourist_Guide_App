package com.arriva.touristguideapp.data.notifications

import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.R
import java.util.*

class NotificationAdapter(
    private val notifications: MutableList<NotificationModel>,
    private val onItemClick: (NotificationModel) -> Unit,
    private val onDeleteClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification, onItemClick, onDeleteClick)
    }

    override fun getItemCount(): Int = notifications.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        private val viewUnreadDot: View = itemView.findViewById(R.id.viewNotificationUnreadDot)
        private val btnDelete: View = itemView.findViewById(R.id.btnDeleteNotification)
        private val layoutContainer: View = itemView.findViewById(R.id.layoutNotificationContainer)

        fun bind(
            notification: NotificationModel,
            onItemClick: (NotificationModel) -> Unit,
            onDeleteClick: (NotificationModel) -> Unit
        ) {
            tvTitle.text = notification.title
            tvMessage.text = notification.message

            // Relative time
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                notification.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            tvTime.text = relativeTime

            // Dynamic Icons based on notification type
            val iconRes = when (notification.type) {
                NotificationModel.TYPE_TRIP -> R.drawable.ic_trip
                NotificationModel.TYPE_REVIEW -> R.drawable.ic_star
                NotificationModel.TYPE_FAVORITE -> R.drawable.ic_favorite
                NotificationModel.TYPE_SOS -> R.drawable.ic_sos
                else -> R.drawable.ic_notification // Default SYSTEM
            }
            ivIcon.setImageResource(iconRes)

            // Dynamic Tint/Color based on type
            val tintColor = when (notification.type) {
                NotificationModel.TYPE_TRIP -> R.color.primary
                NotificationModel.TYPE_REVIEW -> android.R.color.holo_orange_dark
                NotificationModel.TYPE_FAVORITE -> android.R.color.holo_red_dark
                NotificationModel.TYPE_SOS -> android.R.color.holo_red_light
                else -> R.color.primary
            }
            ivIcon.imageTintList = ContextCompat.getColorStateList(itemView.context, tintColor)

            // Read/Unread styling
            if (notification.isRead) {
                tvTitle.setTypeface(null, Typeface.NORMAL)
                viewUnreadDot.visibility = View.GONE
                layoutContainer.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            } else {
                tvTitle.setTypeface(null, Typeface.BOLD)
                viewUnreadDot.visibility = View.VISIBLE
                // Light purple/blue tint for unread notifications
                layoutContainer.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.unread_bg))
            }

            itemView.setOnClickListener { onItemClick(notification) }
            btnDelete.setOnClickListener { onDeleteClick(notification) }
        }
    }
}
