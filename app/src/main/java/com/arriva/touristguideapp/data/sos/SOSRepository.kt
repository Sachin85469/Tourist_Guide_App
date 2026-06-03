package com.arriva.touristguideapp.data.sos

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SOSRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun getContacts(): List<SOSContact> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("emergency_contacts").get().await()
            snapshot.toObjects(SOSContact::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addContact(contact: SOSContact) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid).collection("emergency_contacts").document()
        contact.id = ref.id
        ref.set(contact).await()
    }

    suspend fun updateContact(contact: SOSContact) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("emergency_contacts").document(contact.id).set(contact).await()
    }

    suspend fun deleteContact(contactId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("emergency_contacts").document(contactId).delete().await()
    }
}
