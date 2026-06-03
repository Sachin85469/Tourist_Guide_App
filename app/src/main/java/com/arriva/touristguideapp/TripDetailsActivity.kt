package com.arriva.touristguideapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.notifications.NotificationModel
import com.arriva.touristguideapp.data.notifications.NotificationRepository
import com.arriva.touristguideapp.data.trips.Trip
import com.arriva.touristguideapp.data.trips.TripRepository
import com.arriva.touristguideapp.profile.ProfileActivityTracker
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class TripDetailsActivity : BaseActivity() {

    private lateinit var tripRepository: TripRepository
    private lateinit var notificationRepository: NotificationRepository

    private lateinit var scrollTripDetails: NestedScrollView
    private lateinit var pbLoading: ProgressBar
    private lateinit var ivDetailImage: ImageView
    private lateinit var tvDetailDestination: TextView
    private lateinit var tvDetailStatus: TextView
    private lateinit var tvDetailLocation: TextView
    private lateinit var tvDetailDates: TextView
    private lateinit var tvDetailDuration: TextView
    private lateinit var tvDetailNotes: TextView
    private lateinit var tvDetailActivities: TextView
    private lateinit var tvDetailBudget: TextView
    private lateinit var tvDetailSavedPlaces: TextView
    private lateinit var btnDetailEdit: Button
    private lateinit var btnDetailDelete: Button

    private var tripId: String = ""
    private var currentTrip: Trip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_details)

        tripRepository = TripRepository()
        notificationRepository = NotificationRepository(this)
        tripId = intent.getStringExtra(EXTRA_TRIP_ID) ?: ""

        initViews()
        if (tripId.isBlank()) {
            Toast.makeText(this, "Trip details are unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadTrip()
    }

    private fun initViews() {
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        scrollTripDetails = findViewById(R.id.scrollTripDetails)
        pbLoading = findViewById(R.id.pbLoading)
        ivDetailImage = findViewById(R.id.ivDetailImage)
        tvDetailDestination = findViewById(R.id.tvDetailDestination)
        tvDetailStatus = findViewById(R.id.tvDetailStatus)
        tvDetailLocation = findViewById(R.id.tvDetailLocation)
        tvDetailDates = findViewById(R.id.tvDetailDates)
        tvDetailDuration = findViewById(R.id.tvDetailDuration)
        tvDetailNotes = findViewById(R.id.tvDetailNotes)
        tvDetailActivities = findViewById(R.id.tvDetailActivities)
        tvDetailBudget = findViewById(R.id.tvDetailBudget)
        tvDetailSavedPlaces = findViewById(R.id.tvDetailSavedPlaces)
        btnDetailEdit = findViewById(R.id.btnDetailEdit)
        btnDetailDelete = findViewById(R.id.btnDetailDelete)

        btnDetailEdit.setOnClickListener {
            currentTrip?.let { showEditDialog(it) }
        }
        btnDetailDelete.setOnClickListener {
            currentTrip?.let { confirmDelete(it) }
        }
    }

    private fun loadTrip() {
        pbLoading.visibility = View.VISIBLE
        scrollTripDetails.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val trip = tripRepository.getTrip(tripId)
                if (trip == null) {
                    Toast.makeText(this@TripDetailsActivity, "Trip not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                currentTrip = trip
                bindTrip(trip)
                scrollTripDetails.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@TripDetailsActivity, "Failed to load trip: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                pbLoading.visibility = View.GONE
            }
        }
    }

    private fun bindTrip(trip: Trip) {
        tvDetailDestination.text = trip.destinationLabel()
        tvDetailStatus.text = trip.displayStatus()
        tvDetailLocation.text = trip.locationLabel()
        tvDetailDates.text = trip.dateRangeLabel()
        tvDetailDuration.text = trip.durationLabel()
        tvDetailNotes.text = trip.notes.ifBlank { "No notes yet" }
        tvDetailActivities.text = trip.activitiesLabel()
        tvDetailBudget.text = trip.budget.ifBlank { "Budget not set" }
        tvDetailSavedPlaces.text = trip.savedPlacesLabel()

        val imageUrl = trip.heroImageUrl()
        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_trip)
                .centerCrop()
                .into(ivDetailImage)
        } else {
            ivDetailImage.setImageResource(R.drawable.ic_trip)
        }
    }

    private fun showEditDialog(trip: Trip) {
        TripEditDialog.show(this, trip) { updatedTrip, onSuccess, onError ->
            lifecycleScope.launch {
                try {
                    tripRepository.saveTrip(updatedTrip)
                    ProfileActivityTracker.log(
                        this@TripDetailsActivity,
                        ProfileActivityTracker.Action.TRIP_UPDATED,
                        updatedTrip.destinationLabel()
                    )
                    notificationRepository.addNotification(
                        "Trip Updated",
                        "Your trip '${updatedTrip.destinationLabel()}' was updated successfully.",
                        NotificationModel.TYPE_TRIP
                    )
                    Toast.makeText(this@TripDetailsActivity, "Trip updated successfully", Toast.LENGTH_SHORT).show()
                    onSuccess()
                    loadTrip()
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }

    private fun confirmDelete(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete ${trip.destinationLabel()}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        tripRepository.deleteTrip(trip.id)
                        ProfileActivityTracker.log(
                            this@TripDetailsActivity,
                            ProfileActivityTracker.Action.TRIP_DELETED,
                            trip.destinationLabel()
                        )
                        notificationRepository.addNotification(
                            "Trip Deleted",
                            "Your trip '${trip.destinationLabel()}' was deleted successfully.",
                            NotificationModel.TYPE_TRIP
                        )
                        Toast.makeText(this@TripDetailsActivity, "Trip deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@TripDetailsActivity, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        const val EXTRA_TRIP_ID = "trip_id"
    }
}
