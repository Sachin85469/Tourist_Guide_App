package com.arriva.touristguideapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceManagementActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var container: LinearLayout
    private lateinit var btnSignOutOthers: Button
    private lateinit var currentDeviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_management)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentDeviceId = getDeviceId(this)

        container = findViewById(R.id.deviceContainer)
        btnSignOutOthers = findViewById(R.id.btnSignOutOthers)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        btnSignOutOthers.setOnClickListener {
            showSignOutOthersConfirmation()
        }

        loadDevices()
    }

    private fun loadDevices() {
        val uid = auth.currentUser?.uid ?: return
        container.removeAllViews()

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("users")
                    .document(uid)
                    .collection("devices")
                    .get()
                    .await()

                val inflater = LayoutInflater.from(this@DeviceManagementActivity)
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

                var otherDevicesCount = 0

                for (doc in snapshot.documents) {
                    val deviceId = doc.getString("deviceId") ?: continue
                    val name = doc.getString("deviceName") ?: "Unknown Device"
                    val lastActive = doc.getLong("lastActiveTime") ?: 0L

                    val isCurrent = deviceId == currentDeviceId
                    if (!isCurrent) {
                        otherDevicesCount++
                    }

                    // Inflate custom card row
                    val card = inflater.inflate(R.layout.item_device_card, container, false) as MaterialCardView
                    val tvName = card.findViewById<TextView>(R.id.tvDeviceName)
                    val tvActive = card.findViewById<TextView>(R.id.tvDeviceActive)
                    val badge = card.findViewById<TextView>(R.id.tvDeviceBadge)

                    tvName.text = name
                    tvActive.text = "Last active: ${sdf.format(Date(lastActive))}"
                    
                    if (isCurrent) {
                        badge.visibility = View.VISIBLE
                        badge.text = "This Device"
                        badge.setBackgroundResource(R.drawable.badge_background)
                    } else {
                        badge.visibility = View.GONE
                    }

                    container.addView(card)
                }

                btnSignOutOthers.isEnabled = otherDevicesCount > 0

            } catch (e: Exception) {
                Toast.makeText(this@DeviceManagementActivity, "Error loading devices: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSignOutOthersConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign Out Other Devices?")
            .setMessage("Are you sure you want to sign out of all other active devices? You will remain signed in on this device.")
            .setPositiveButton("Sign Out Others") { _, _ ->
                signOutOthers()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signOutOthers() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("users")
                    .document(uid)
                    .collection("devices")
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val deviceId = doc.getString("deviceId") ?: continue
                    if (deviceId != currentDeviceId) {
                        doc.reference.delete().await()
                    }
                }

                Toast.makeText(this@DeviceManagementActivity, "Successfully signed out other devices.", Toast.LENGTH_SHORT).show()
                loadDevices()

            } catch (e: Exception) {
                Toast.makeText(this@DeviceManagementActivity, "Failed to sign out other devices: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
