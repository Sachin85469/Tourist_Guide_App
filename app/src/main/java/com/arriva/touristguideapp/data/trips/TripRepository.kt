package com.arriva.touristguideapp.data.trips

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TripRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getTrips(): List<Trip> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            db.collection("trips")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Trip::class.java)?.apply {
                        if (id.isEmpty()) id = document.id
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Java-friendly main-thread callback for Home's lightweight trip preview. */
    fun getTripsAsync(callback: Consumer<List<Trip>>) {
        CoroutineScope(Dispatchers.Main).launch {
            callback.accept(getTrips())
        }
    }

    suspend fun getTrip(tripId: String): Trip? {
        val uid = auth.currentUser?.uid ?: return null
        if (tripId.isBlank()) return null

        return try {
            val document = db.collection("trips").document(tripId).get().await()
            val trip = document.toObject(Trip::class.java) ?: return null
            if (trip.userId != uid) return null
            if (trip.id.isEmpty()) trip.id = document.id
            trip
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveTrip(trip: Trip) {
        val uid = auth.currentUser?.uid ?: return
        trip.userId = uid
        val now = Date()
        trip.updatedAt = now

        if (trip.destinationName.isBlank()) {
            trip.destinationName = trip.title
        }
        if (trip.location.isBlank()) {
            trip.location = trip.places.mapNotNull { it.city }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
        }

        if (trip.id.isEmpty()) {
            val ref = db.collection("trips").document()
            trip.id = ref.id
            trip.createdAt = now
            ref.set(trip).await()
        } else {
            db.collection("trips").document(trip.id).set(trip).await()
        }
    }

    suspend fun deleteTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (tripId.isBlank()) return

        val document = db.collection("trips").document(tripId).get().await()
        val ownerId = document.getString("userId") ?: return
        if (ownerId != uid) return

        db.collection("trips").document(tripId).delete().await()
    }

    fun saveTripAsync(trip: Trip): com.google.android.gms.tasks.Task<Void> {
        val tcs = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                saveTrip(trip)
                tcs.setResult(null)
            } catch (e: Exception) {
                tcs.setException(e)
            }
        }
        return tcs.task
    }
}
