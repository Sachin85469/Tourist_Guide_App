package com.arriva.touristguideapp.sos.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.R
import com.arriva.touristguideapp.sos.model.SOSEvent

class SOSHistoryAdapter(
    private val events: List<SOSEvent>
) : RecyclerView.Adapter<SOSHistoryAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_sos_event,
            parent,
            false
        )
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tv_event_time)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_event_date)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_event_location)
        private val tvContacts: TextView = itemView.findViewById(R.id.tv_event_contacts)

        fun bind(event: SOSEvent) {
            tvTime.text = event.time
            tvDate.text = event.date
            tvLocation.text = event.location
            tvContacts.text = event.contactsNotified

            // Setup click on maps link to open browser/maps
            if (event.location != null && event.location.startsWith("http")) {
                tvLocation.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.location))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        itemView.context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } else {
                tvLocation.setOnClickListener(null)
            }
        }
    }
}
