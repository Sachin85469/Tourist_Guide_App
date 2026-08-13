package com.arriva.touristguideapp.sos.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.BaseActivity
import com.arriva.touristguideapp.R
import com.arriva.touristguideapp.data.sos.SOSContact
import com.arriva.touristguideapp.data.sos.SOSRepository
import com.arriva.touristguideapp.sos.manager.SOSMessageBuilder
import com.arriva.touristguideapp.sos.manager.SOSPreferences
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.UUID

class SOSSettingsActivity : BaseActivity() {
    private lateinit var preferences: SOSPreferences
    private lateinit var repository: SOSRepository
    private lateinit var adapter: EmergencyContactAdapter
    private var contactsList = mutableListOf<SOSContact>()
    private lateinit var layoutEmpty: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_settings)

        preferences = SOSPreferences(this)
        repository = SOSRepository(this)

        // Setup Toolbar
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        layoutEmpty = findViewById(R.id.layout_empty_contacts)

        // Load current contacts list from preferences
        contactsList = preferences.contacts.toMutableList()

        // Setup RecyclerView
        val rvContacts = findViewById<RecyclerView>(R.id.rv_emergency_contacts)
        rvContacts.layoutManager = LinearLayoutManager(this)
        adapter = EmergencyContactAdapter(
            contactsList,
            onEditClick = { contact -> showAddEditContactDialog(contact) },
            onDeleteClick = { contact -> deleteContact(contact) }
        )
        rvContacts.adapter = adapter

        updateEmptyState()

        setupEmergencyMessageSection()

        // Bind preference switches
        val swSendSms = findViewById<SwitchCompat>(R.id.sw_send_sms)
        val swShareLocation = findViewById<SwitchCompat>(R.id.sw_share_location)
        val swCallAfterSms = findViewById<SwitchCompat>(R.id.sw_call_after_sms)
        val swRequireConfirmation = findViewById<SwitchCompat>(R.id.sw_require_confirmation)
        val swVolume = findViewById<SwitchCompat>(R.id.sw_volume_trigger)
        val swFloating = findViewById<SwitchCompat>(R.id.sw_floating_button)

        swSendSms.isChecked = preferences.isSendSMSAutomatically
        swShareLocation.isChecked = preferences.isShareLiveLocation
        swCallAfterSms.isChecked = preferences.isCallAfterSMS
        swRequireConfirmation.isChecked = preferences.isRequireConfirmation
        swVolume.isChecked = preferences.isVolumeTriggerEnabled
        swFloating.isChecked = preferences.isFloatingButtonEnabled

        // Set listeners to save switch preferences on click
        swSendSms.setOnCheckedChangeListener { _, isChecked ->
            preferences.isSendSMSAutomatically = isChecked
        }
        swShareLocation.setOnCheckedChangeListener { _, isChecked ->
            preferences.isShareLiveLocation = isChecked
        }
        swCallAfterSms.setOnCheckedChangeListener { _, isChecked ->
            preferences.isCallAfterSMS = isChecked
        }
        swRequireConfirmation.setOnCheckedChangeListener { _, isChecked ->
            preferences.isRequireConfirmation = isChecked
        }
        swVolume.setOnCheckedChangeListener { _, isChecked ->
            preferences.isVolumeTriggerEnabled = isChecked
        }
        swFloating.setOnCheckedChangeListener { _, isChecked ->
            preferences.isFloatingButtonEnabled = isChecked
            toggleFloatingService(isChecked)
        }

        // Add contact listener
        findViewById<View>(R.id.btn_add_contact).setOnClickListener {
            if (contactsList.size >= 5) {
                Toast.makeText(this, "Maximum of 5 emergency contacts allowed", Toast.LENGTH_LONG).show()
            } else {
                showAddEditContactDialog()
            }
        }

        // Save & Sync Config button
        findViewById<View>(R.id.btn_save_settings).setOnClickListener {
            // In local-first pattern, contacts are saved immediately on add/edit/delete.
            // But we sync the switches and perform a force update to ensure preferences are flushed.
            preferences.isSendSMSAutomatically = swSendSms.isChecked
            preferences.isShareLiveLocation = swShareLocation.isChecked
            preferences.isCallAfterSMS = swCallAfterSms.isChecked
            preferences.isRequireConfirmation = swRequireConfirmation.isChecked
            preferences.isVolumeTriggerEnabled = swVolume.isChecked
            preferences.isFloatingButtonEnabled = swFloating.isChecked
            
            toggleFloatingService(swFloating.isChecked)

            // Force push local contacts list to Firestore
            lifecycleScope.launch {
                for (contact in contactsList) {
                    repository.updateContact(contact)
                }
            }

            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        }

        // Fetch contacts from cloud Firestore for sync on startup
        lifecycleScope.launch {
            try {
                val remoteContacts = repository.getContacts()
                if (remoteContacts.isNotEmpty()) {
                    // If local is empty, populate remote. If mismatch exists, merge.
                    if (contactsList.isEmpty()) {
                        contactsList.clear()
                        contactsList.addAll(remoteContacts)
                        preferences.contacts = contactsList
                        adapter.updateContacts(contactsList)
                        updateEmptyState()
                    }
                }
            } catch (e: Exception) {
                // Ignore background sync errors
            }
        }
    }

    private fun setupEmergencyMessageSection() {
        val tilMessage = findViewById<TextInputLayout>(R.id.til_sos_message)
        val etMessage = findViewById<TextInputEditText>(R.id.et_sos_message)

        etMessage.setText(preferences.sosMessageTemplate)

        findViewById<View>(R.id.btn_save_sos_message).setOnClickListener {
            tilMessage.error = null
            val text = etMessage.text?.toString()?.trim().orEmpty()

            if (SOSMessageBuilder.isTemplateEmpty(text)) {
                val defaultMessage = preferences.defaultSosMessageTemplate
                preferences.clearCustomSosMessageTemplate()
                etMessage.setText(defaultMessage)
                Toast.makeText(this, R.string.sos_message_empty_error, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!SOSMessageBuilder.isTemplateValidForSave(this, text)) {
                tilMessage.error = getString(
                    R.string.sos_message_too_long,
                    SOSMessageBuilder.MAX_SMS_PARTS
                )
                return@setOnClickListener
            }

            preferences.customSosMessageTemplate = text
            Toast.makeText(this, R.string.sos_message_saved, Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_restore_sos_message).setOnClickListener {
            tilMessage.error = null
            val defaultMessage = preferences.defaultSosMessageTemplate
            preferences.clearCustomSosMessageTemplate()
            etMessage.setText(defaultMessage)
            Toast.makeText(this, R.string.sos_message_restored, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmptyState() {
        if (contactsList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun toggleFloatingService(enable: Boolean) {
        if (enable) {
            try {
                val serviceIntent = Intent(this, com.arriva.touristguideapp.sos.service.FloatingSOSService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                // Ignore service starts errors (e.g. permission omitted)
            }
        } else {
            try {
                stopService(Intent(this, com.arriva.touristguideapp.sos.service.FloatingSOSService::class.java))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun deleteContact(contact: SOSContact) {
        if (contactsList.size <= 1) {
            Toast.makeText(this, "You must have at least one emergency contact", Toast.LENGTH_LONG).show()
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                contactsList.remove(contact)
                preferences.contacts = contactsList
                adapter.updateContacts(contactsList)
                updateEmptyState()

                lifecycleScope.launch {
                    try {
                        repository.deleteContact(contact.id)
                    } catch (e: Exception) {
                        // Sync failure ignored for offline functionality
                    }
                }
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddEditContactDialog(contactToEdit: SOSContact? = null) {
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_contact, null)
        builder.setView(dialogView)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tietName = dialogView.findViewById<TextInputEditText>(R.id.tiet_name)
        val tietRelationship = dialogView.findViewById<TextInputEditText>(R.id.tiet_relationship)
        val tietPhone = dialogView.findViewById<TextInputEditText>(R.id.tiet_phone)

        val tilName = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_name)
        val tilPhone = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_phone)

        if (contactToEdit != null) {
            tvTitle.text = "Edit Emergency Contact"
            tietName.setText(contactToEdit.name)
            tietRelationship.setText(contactToEdit.relationship)
            tietPhone.setText(contactToEdit.phone)
        } else {
            tvTitle.text = "Add Emergency Contact"
        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val name = tietName.text.toString().trim()
            val relationship = tietRelationship.text.toString().trim()
            val phone = tietPhone.text.toString().trim()

            tilName.error = null
            tilPhone.error = null

            val error = validateContactInput(name, phone, contactToEdit?.id, contactsList)
            if (error != null) {
                if (error.contains("Name")) {
                    tilName.error = error
                } else {
                    tilPhone.error = error
                }
                return@setOnClickListener
            }

            if (contactToEdit != null) {
                contactToEdit.name = name
                contactToEdit.relationship = relationship
                contactToEdit.phone = phone

                preferences.contacts = contactsList
                adapter.updateContacts(contactsList)

                lifecycleScope.launch {
                    try {
                        repository.updateContact(contactToEdit)
                    } catch (e: Exception) {
                        // Fail silently for offline robustness
                    }
                }
            } else {
                val newContact = SOSContact(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    relationship = relationship,
                    phone = phone
                )
                contactsList.add(newContact)

                preferences.contacts = contactsList
                adapter.updateContacts(contactsList)

                lifecycleScope.launch {
                    try {
                        repository.addContact(newContact)
                    } catch (e: Exception) {
                        // Fail silently for offline robustness
                    }
                }
            }

            updateEmptyState()
            dialog.dismiss()
            Toast.makeText(this, "Contact saved successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateContactInput(
        name: String,
        phone: String,
        currentId: String?,
        contacts: List<SOSContact>
    ): String? {
        if (name.isEmpty()) {
            return "Name cannot be empty"
        }

        val digits = phone.filter { it.isDigit() }
        if (digits.length < 8) {
            return "Phone number must have at least 8 digits"
        }

        val phoneRegex = """^\+?[0-9\-\s\(\)]+$""".toRegex()
        if (!phoneRegex.matches(phone)) {
            return "Invalid phone number characters"
        }

        // Exclude duplicate validation for the contact currently being edited
        val hasDuplicate = contacts.any {
            it.phone.filter { c -> c.isDigit() } == digits && it.id != currentId
        }
        if (hasDuplicate) {
            return "A contact with this phone number already exists"
        }

        return null
    }
}
