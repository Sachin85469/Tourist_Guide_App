package com.arriva.touristguideapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.data.notifications.NotificationModel
import com.arriva.touristguideapp.data.notifications.NotificationRepository
import com.arriva.touristguideapp.data.trips.Trip
import com.arriva.touristguideapp.data.trips.TripRepository
import com.arriva.touristguideapp.profile.ProfileActivityTracker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TripHistoryActivity : BaseActivity() {

    private lateinit var tripRepository: TripRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var adapter: TripHistoryAdapter

    private lateinit var dashboardContent: View
    private lateinit var rvTrips: RecyclerView
    private lateinit var llEmptyState: View
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnPlanNewTrip: Button
    private lateinit var etTripSearch: TextInputEditText
    private lateinit var filterGroup: MaterialButtonToggleGroup
    private lateinit var tvTotalTrips: TextView
    private lateinit var tvCompletedTrips: TextView
    private lateinit var tvUpcomingTrips: TextView
    private lateinit var tvFavoriteDestination: TextView
    private lateinit var tvTripResultSummary: TextView
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyMessage: TextView

    private val allTrips = mutableListOf<Trip>()
    private val visibleTrips = mutableListOf<Trip>()
    private var currentFilter = FILTER_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)

        tripRepository = TripRepository()
        notificationRepository = NotificationRepository(this)

        initViews()
    }

    override fun onResume() {
        super.onResume()
        loadUserTrips()
    }

    private fun initViews() {
        dashboardContent = findViewById(R.id.llDashboardContent)
        rvTrips = findViewById(R.id.rvTrips)
        llEmptyState = findViewById(R.id.llEmptyState)
        pbLoading = findViewById(R.id.pbLoading)
        btnPlanNewTrip = findViewById(R.id.btnPlanNewTrip)
        etTripSearch = findViewById(R.id.etTripSearch)
        filterGroup = findViewById(R.id.filterGroup)
        tvTotalTrips = findViewById(R.id.tvTotalTrips)
        tvCompletedTrips = findViewById(R.id.tvCompletedTrips)
        tvUpcomingTrips = findViewById(R.id.tvUpcomingTrips)
        tvFavoriteDestination = findViewById(R.id.tvFavoriteDestination)
        tvTripResultSummary = findViewById(R.id.tvTripResultSummary)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        rvTrips.layoutManager = LinearLayoutManager(this)
        adapter = TripHistoryAdapter(
            trips = visibleTrips,
            onTripClick = { openTripDetails(it) },
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { confirmDelete(it) }
        )
        rvTrips.adapter = adapter

        btnPlanNewTrip.setOnClickListener {
            startActivity(Intent(this, PlanTripActivity::class.java))
            finish()
        }

        etTripSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentFilter = when (checkedId) {
                R.id.btnFilterUpcoming -> FILTER_UPCOMING
                R.id.btnFilterCompleted -> FILTER_COMPLETED
                R.id.btnFilterCancelled -> FILTER_CANCELLED
                else -> FILTER_ALL
            }
            applyFilters()
        }
    }

    private fun loadUserTrips() {
        pbLoading.visibility = View.VISIBLE
        dashboardContent.visibility = View.GONE
        llEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val list = tripRepository.getTrips()
                allTrips.clear()
                allTrips.addAll(list.sortedByDescending { it.startDate ?: Date(0) })
                generateStartingSoonNotifications(allTrips)
                updateSummary()
                applyFilters()
            } catch (e: Exception) {
                Toast.makeText(this@TripHistoryActivity, "Failed to load trips: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState(hasAnyTrips = false)
            } finally {
                pbLoading.visibility = View.GONE
            }
        }
    }

    private fun applyFilters() {
        val query = etTripSearch.text?.toString() ?: ""
        visibleTrips.clear()
        visibleTrips.addAll(
            allTrips.filter { trip ->
                trip.matchesTripQuery(query) && matchesCurrentFilter(trip)
            }
        )
        adapter.notifyDataSetChanged()

        if (allTrips.isEmpty()) {
            showEmptyState(hasAnyTrips = false)
            return
        }

        dashboardContent.visibility = View.VISIBLE
        llEmptyState.visibility = View.GONE
        rvTrips.visibility = View.VISIBLE
        tvTripResultSummary.text = when {
            visibleTrips.isEmpty() -> "No trips match this view"
            visibleTrips.size == 1 -> "Showing 1 trip"
            else -> String.format(Locale.getDefault(), "Showing %d trips", visibleTrips.size)
        }
    }

    private fun matchesCurrentFilter(trip: Trip): Boolean {
        return when (currentFilter) {
            FILTER_UPCOMING -> trip.displayStatus() == "Upcoming"
            FILTER_COMPLETED -> trip.displayStatus() == "Completed"
            FILTER_CANCELLED -> trip.displayStatus() == "Cancelled"
            else -> true
        }
    }

    private fun updateSummary() {
        tvTotalTrips.text = allTrips.size.toString()
        tvCompletedTrips.text = allTrips.count { it.displayStatus() == "Completed" }.toString()
        tvUpcomingTrips.text = allTrips.count { it.displayStatus() == "Upcoming" }.toString()

        val favoriteDestination = allTrips
            .groupingBy { it.destinationLabel() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: "None yet"
        tvFavoriteDestination.text = "Favorite destination: $favoriteDestination"
    }

    private fun showEmptyState(hasAnyTrips: Boolean) {
        dashboardContent.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvEmptyTitle.text = if (hasAnyTrips) "No trips match this view" else "No trips yet"
        tvEmptyMessage.text = if (hasAnyTrips) {
            "Try a different search or filter."
        } else {
            "Start with the trip planner and your saved plans will appear here."
        }
        btnPlanNewTrip.visibility = if (hasAnyTrips) View.GONE else View.VISIBLE
    }

    private fun openTripDetails(trip: Trip) {
        if (trip.id.isBlank()) {
            Toast.makeText(this, "Trip details are unavailable for this item", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, TripDetailsActivity::class.java)
        intent.putExtra(TripDetailsActivity.EXTRA_TRIP_ID, trip.id)
        startActivity(intent)
    }

    private fun showEditDialog(trip: Trip) {
        TripEditDialog.show(this, trip) { updatedTrip, onSuccess, onError ->
            lifecycleScope.launch {
                try {
                    tripRepository.saveTrip(updatedTrip)
                    ProfileActivityTracker.log(
                        this@TripHistoryActivity,
                        ProfileActivityTracker.Action.TRIP_UPDATED,
                        updatedTrip.destinationLabel()
                    )
                    notificationRepository.addNotification(
                        "Trip Updated",
                        "Your trip '${updatedTrip.destinationLabel()}' was updated successfully.",
                        NotificationModel.TYPE_TRIP
                    )
                    Toast.makeText(this@TripHistoryActivity, "Trip updated successfully", Toast.LENGTH_SHORT).show()
                    onSuccess()
                    loadUserTrips()
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
                pbLoading.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        tripRepository.deleteTrip(trip.id)
                        ProfileActivityTracker.log(
                            this@TripHistoryActivity,
                            ProfileActivityTracker.Action.TRIP_DELETED,
                            trip.destinationLabel()
                        )
                        notificationRepository.addNotification(
                            "Trip Deleted",
                            "Your trip '${trip.destinationLabel()}' was deleted successfully.",
                            NotificationModel.TYPE_TRIP
                        )
                        Toast.makeText(this@TripHistoryActivity, "Trip deleted successfully", Toast.LENGTH_SHORT).show()
                        loadUserTrips()
                    } catch (e: Exception) {
                        Toast.makeText(this@TripHistoryActivity, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        pbLoading.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateStartingSoonNotifications(trips: List<Trip>) {
        val now = System.currentTimeMillis()
        val alertWindow = now + TimeUnit.HOURS.toMillis(24)
        val prefs = getSharedPreferences("trip_start_alerts", MODE_PRIVATE)

        trips.forEach { trip ->
            val startTime = trip.startDate?.time ?: return@forEach
            if (trip.displayStatus() != "Upcoming") return@forEach
            if (startTime !in (now + 1)..alertWindow) return@forEach

            val key = "starting_soon_${trip.userId}_${trip.id}_$startTime"
            if (prefs.getBoolean(key, false)) return@forEach

            notificationRepository.addNotification(
                "Trip Starting Soon",
                "${trip.destinationLabel()} starts within the next 24 hours.",
                NotificationModel.TYPE_TRIP
            )
            prefs.edit().putBoolean(key, true).apply()
        }
    }

    companion object {
        private const val FILTER_ALL = "all"
        private const val FILTER_UPCOMING = "upcoming"
        private const val FILTER_COMPLETED = "completed"
        private const val FILTER_CANCELLED = "cancelled"
    }
}
