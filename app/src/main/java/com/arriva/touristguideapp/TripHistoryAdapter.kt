package com.arriva.touristguideapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.data.trips.Trip
import com.bumptech.glide.Glide

class TripHistoryAdapter(
    private val trips: List<Trip>,
    private val onTripClick: (Trip) -> Unit,
    private val onEditClick: (Trip) -> Unit,
    private val onDeleteClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.bind(trip, onTripClick, onEditClick, onDeleteClick)
    }

    override fun getItemCount(): Int = trips.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivTripImage: ImageView = itemView.findViewById(R.id.ivTripImage)
        private val tvTripTitle: TextView = itemView.findViewById(R.id.tvTripTitle)
        private val tvTripStatus: TextView = itemView.findViewById(R.id.tvTripStatus)
        private val tvTripDates: TextView = itemView.findViewById(R.id.tvTripDates)
        private val tvTripDuration: TextView = itemView.findViewById(R.id.tvTripDuration)
        private val tvTripLocation: TextView = itemView.findViewById(R.id.tvTripLocation)
        private val tvTripPlaces: TextView = itemView.findViewById(R.id.tvTripPlaces)
        private val btnEditTrip: Button = itemView.findViewById(R.id.btnEditTrip)
        private val btnDeleteTrip: Button = itemView.findViewById(R.id.btnDeleteTrip)

        fun bind(
            trip: Trip,
            onTripClick: (Trip) -> Unit,
            onEditClick: (Trip) -> Unit,
            onDeleteClick: (Trip) -> Unit
        ) {
            tvTripTitle.text = trip.destinationLabel()
            tvTripStatus.text = trip.displayStatus()
            tvTripDates.text = trip.dateRangeLabel()
            tvTripDuration.text = trip.durationLabel()
            tvTripLocation.text = trip.locationLabel()

            val placesPreview = trip.places.take(3).joinToString("\n") { "- ${it.name}" }
            tvTripPlaces.text = placesPreview.ifBlank { "No saved places" }

            val imageUrl = trip.heroImageUrl()
            if (imageUrl.isNotBlank()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_trip)
                    .centerCrop()
                    .into(ivTripImage)
            } else {
                ivTripImage.setImageResource(R.drawable.ic_trip)
            }

            itemView.setOnClickListener { onTripClick(trip) }
            btnEditTrip.setOnClickListener { onEditClick(trip) }
            btnDeleteTrip.setOnClickListener { onDeleteClick(trip) }
        }
    }
}
