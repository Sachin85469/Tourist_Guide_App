package com.arriva.touristguideapp

import android.app.Dialog
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arriva.touristguideapp.data.trips.Trip
import com.google.android.material.textfield.TextInputEditText

object TripEditDialog {
    private val statusOptions = arrayOf("Upcoming", "Ongoing", "Completed", "Cancelled")

    fun show(
        activity: AppCompatActivity,
        trip: Trip,
        onSave: (Trip, () -> Unit, (Exception) -> Unit) -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_trip)
        dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle: TextView = dialog.findViewById(R.id.tvEditTripTitle)
        val spinnerStatus: Spinner = dialog.findViewById(R.id.spinnerTripStatus)
        val etStartDate: TextInputEditText = dialog.findViewById(R.id.etTripStartDate)
        val etEndDate: TextInputEditText = dialog.findViewById(R.id.etTripEndDate)
        val etNotes: TextInputEditText = dialog.findViewById(R.id.etTripNotes)
        val etActivities: TextInputEditText = dialog.findViewById(R.id.etTripActivities)
        val etBudget: TextInputEditText = dialog.findViewById(R.id.etTripBudget)
        val btnCancel: Button = dialog.findViewById(R.id.btnEditTripCancel)
        val btnSave: Button = dialog.findViewById(R.id.btnEditTripSave)
        val pbSaving: ProgressBar = dialog.findViewById(R.id.pbEditTripSaving)

        tvTitle.text = "Edit ${trip.destinationLabel()}"
        etStartDate.setText(trip.inputStartDateLabel())
        etEndDate.setText(trip.inputEndDateLabel())
        etNotes.setText(trip.notes)
        etActivities.setText(trip.activities.joinToString("\n"))
        etBudget.setText(trip.budget)

        val spinnerAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, statusOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = spinnerAdapter
        spinnerStatus.setSelection(statusOptions.indexOf(trip.displayStatus()).coerceAtLeast(0))

        fun setSaving(isSaving: Boolean) {
            pbSaving.visibility = if (isSaving) View.VISIBLE else View.GONE
            btnSave.isEnabled = !isSaving
            btnCancel.isEnabled = !isSaving
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val startDate = try {
                parseTripInputDate(etStartDate.text?.toString() ?: "")
            } catch (e: Exception) {
                Toast.makeText(activity, "Use yyyy-MM-dd for start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val endDate = try {
                parseTripInputDate(etEndDate.text?.toString() ?: "")
            } catch (e: Exception) {
                Toast.makeText(activity, "Use yyyy-MM-dd for end date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (startDate != null && endDate != null && endDate.before(startDate)) {
                Toast.makeText(activity, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val activities = (etActivities.text?.toString() ?: "")
                .split('\n', ',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val selectedStatus = spinnerStatus.selectedItem?.toString() ?: trip.displayStatus()
            val updatedTrip = trip.copy(
                startDate = startDate,
                endDate = endDate,
                notes = etNotes.text?.toString()?.trim() ?: "",
                activities = activities,
                budget = etBudget.text?.toString()?.trim() ?: "",
                status = deriveStoredStatus(startDate, endDate, selectedStatus)
            )

            setSaving(true)
            onSave(
                updatedTrip,
                {
                    setSaving(false)
                    dialog.dismiss()
                },
                { error ->
                    setSaving(false)
                    Toast.makeText(activity, "Failed to update trip: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }
}
