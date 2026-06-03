package com.arriva.touristguideapp.sos.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.R
import com.arriva.touristguideapp.data.sos.SOSContact
import com.arriva.touristguideapp.data.sos.SOSRepository
import com.arriva.touristguideapp.sos.manager.SOSPreferences
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SOSSettingsActivity : AppCompatActivity() {
    private lateinit var preferences: SOSPreferences
    private lateinit var repository: SOSRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_settings)

        preferences = SOSPreferences(this)
        repository = SOSRepository(this)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val etContact = findViewById<TextInputEditText>(R.id.et_emergency_contact)
        val swVolume = findViewById<SwitchCompat>(R.id.sw_volume_trigger)
        val swFloating = findViewById<SwitchCompat>(R.id.sw_floating_button)
        val swCall = findViewById<SwitchCompat>(R.id.sw_call_after_sms)

        etContact.setText(preferences.contactNumber)
        swVolume.isChecked = preferences.isVolumeTriggerEnabled
        swFloating.isChecked = preferences.isFloatingButtonEnabled
        swCall.isChecked = preferences.isCallAfterSMS

        findViewById<View>(R.id.btn_save_settings).setOnClickListener {
            val contactNumber = etContact.text.toString()
            preferences.contactNumber = contactNumber
            preferences.isVolumeTriggerEnabled = swVolume.isChecked
            preferences.isFloatingButtonEnabled = swFloating.isChecked
            preferences.isCallAfterSMS = swCall.isChecked

            // Sync to Firestore
            lifecycleScope.launch {
                repository.addContact(SOSContact(name = "Primary", phone = contactNumber))
            }

            if (swFloating.isChecked) {
                try {
                    val serviceIntent = Intent(this, com.arriva.touristguideapp.sos.service.FloatingSOSService::class.java)
                    ContextCompat.startForegroundService(this, serviceIntent)
                } catch (e: Exception) {
                    // Handle exception
                }
            } else {
                stopService(Intent(this, com.arriva.touristguideapp.sos.service.FloatingSOSService::class.java))
            }

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        }
    }
}
