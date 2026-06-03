package com.arriva.touristguideapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.data.notifications.NotificationRepository
import com.arriva.touristguideapp.data.notifications.NotificationModel
import com.arriva.touristguideapp.data.trips.Trip
import com.arriva.touristguideapp.data.trips.TripRepository
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TripHistoryActivity : AppCompatActivity() {

    private lateinit var tripRepository: TripRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var adapter: TripHistoryAdapter

    private lateinit var rvTrips: RecyclerView
    private lateinit var llEmptyState: View
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnPlanNewTrip: Button

    private val tripsList = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)

        tripRepository = TripRepository()
        notificationRepository = NotificationRepository(this)

        initViews()
        loadUserTrips()
    }

    private fun initViews() {
        rvTrips = findViewById(R.id.rvTrips)
        llEmptyState = findViewById(R.id.llEmptyState)
        pbLoading = findViewById(R.id.pbLoading)
        btnPlanNewTrip = findViewById(R.id.btnPlanNewTrip)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        rvTrips.layoutManager = LinearLayoutManager(this)
        adapter = TripHistoryAdapter(
            trips = tripsList,
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { confirmDelete(it) }
        )
        rvTrips.adapter = adapter

        btnPlanNewTrip.setOnClickListener {
            startActivity(Intent(this, PlanTripActivity::class.java))
            finish()
        }
    }

    private fun loadUserTrips() {
        pbLoading.visibility = View.VISIBLE
        rvTrips.visibility = View.GONE
        llEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val list = tripRepository.getTrips()
                tripsList.clear()
                tripsList.addAll(list.sortedByDescending { it.startDate ?: Date(0) })
                adapter.notifyDataSetChanged()
                
                pbLoading.visibility = View.GONE
                toggleEmptyState(tripsList.isEmpty())
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                Toast.makeText(this@TripHistoryActivity, "Failed to load trips: ${e.message}", Toast.LENGTH_SHORT).show()
                toggleEmptyState(true)
            }
        }
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            llEmptyState.visibility = View.VISIBLE
            rvTrips.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvTrips.visibility = View.VISIBLE
        }
    }

    private fun showEditDialog(trip: Trip) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_review) // Reuse dialog layout for editing title/comment
        dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle: TextView = dialog.findViewById(R.id.tvEditReviewTitle)
        val rbStars: RatingBar = dialog.findViewById(R.id.rbEditReviewStars)
        val etComment: EditText = dialog.findViewById(R.id.etEditReviewComment)
        val btnCancel: Button = dialog.findViewById(R.id.btnEditReviewCancel)
        val btnSave: Button = dialog.findViewById(R.id.btnEditReviewSave)
        val pbSaving: ProgressBar = dialog.findViewById(R.id.pbEditReviewSaving)

        tvTitle.text = "Edit Trip Title & Status"
        rbStars.visibility = View.GONE // Not rating a trip
        etComment.hint = "Enter new Trip Title"
        etComment.setText(trip.title)

        // Let's add a Spinner dynamically to edit status (planned, active, completed)
        val parentLayout = etComment.parent as LinearLayout
        val spinnerStatus = Spinner(this)
        val statusOptions = arrayOf("planned", "active", "completed")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = spinnerAdapter
        spinnerStatus.setSelection(statusOptions.indexOf(trip.status))
        
        // Insert spinner below the edit text
        val index = parentLayout.indexOfChild(etComment)
        parentLayout.addView(spinnerStatus, index + 1)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newTitle = etComment.text.toString().trim()
            val newStatus = spinnerStatus.selectedItem.toString()

            if (newTitle.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnCancel.isEnabled = false
            pbSaving.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    trip.title = newTitle
                    trip.status = newStatus
                    tripRepository.saveTrip(trip)
                    
                    // Trigger notification
                    notificationRepository.addNotification(
                        "Trip Updated",
                        "Your trip '$newTitle' was updated successfully to status: $newStatus.",
                        NotificationModel.TYPE_TRIP
                    )

                    Toast.makeText(this@TripHistoryActivity, "Trip updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadUserTrips()
                } catch (e: Exception) {
                    pbSaving.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnCancel.isEnabled = true
                    Toast.makeText(this@TripHistoryActivity, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun confirmDelete(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete this trip?")
            .setPositiveButton("Delete") { _, _ ->
                pbLoading.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        tripRepository.deleteTrip(trip.id)
                        
                        // Trigger notification
                        notificationRepository.addNotification(
                            "Trip Deleted",
                            "Your trip '${trip.title}' was deleted successfully.",
                            NotificationModel.TYPE_TRIP
                        )

                        Toast.makeText(this@TripHistoryActivity, "Trip deleted successfully", Toast.LENGTH_SHORT).show()
                        loadUserTrips()
                    } catch (e: Exception) {
                        pbLoading.visibility = View.GONE
                        Toast.makeText(this@TripHistoryActivity, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
