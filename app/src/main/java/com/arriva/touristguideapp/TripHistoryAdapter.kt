package com.arriva.touristguideapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.data.trips.Trip
import java.text.SimpleDateFormat
import java.util.*

class TripHistoryAdapter(
    private val trips: List<Trip>,
    private val onEditClick: (Trip) -> Unit,
    private val onDeleteClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.bind(trip, onEditClick, onDeleteClick)
    }

    override fun getItemCount(): Int = trips.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTripTitle: TextView = itemView.findViewById(R.id.tvTripTitle)
        private val tvTripStatus: TextView = itemView.findViewById(R.id.tvTripStatus)
        private val tvTripDates: TextView = itemView.findViewById(R.id.tvTripDates)
        private val tvTripPlaces: TextView = itemView.findViewById(R.id.tvTripPlaces)
        private val btnEditTrip: Button = itemView.findViewById(R.id.btnEditTrip)
        private val btnDeleteTrip: Button = itemView.findViewById(R.id.btnDeleteTrip)

        fun bind(trip: Trip, onEditClick: (Trip) -> Unit, onDeleteClick: (Trip) -> Unit) {
            tvTripTitle.text = trip.title
            tvTripStatus.text = trip.status.uppercase()

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val startStr = trip.startDate?.let { dateFormat.format(it) } ?: "N/A"
            val endStr = trip.endDate?.let { dateFormat.format(it) } ?: "N/A"
            tvTripDates.text = "$startStr - $endStr"

            if (trip.places.isEmpty()) {
                tvTripPlaces.text = "No places selected."
            } else {
                val sb = StringBuilder()
                for (place in trip.places) {
                    sb.append("• ").append(place.name).append(" (").append(place.category).append(")\n")
                }
                tvTripPlaces.text = sb.toString().trim()
            }

            btnEditTrip.setOnClickListener { onEditClick(trip) }
            btnDeleteTrip.setOnClickListener { onDeleteClick(trip) }
        }
    }
}
