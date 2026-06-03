package com.arriva.touristguideapp.data.trips

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
                .toObjects(Trip::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveTrip(trip: Trip) {
        val uid = auth.currentUser?.uid ?: return
        trip.userId = uid
        if (trip.id.isEmpty()) {
            val ref = db.collection("trips").document()
            trip.id = ref.id
            ref.set(trip).await()
        } else {
            db.collection("trips").document(trip.id).set(trip).await()
        }
    }

    suspend fun deleteTrip(tripId: String) {
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
