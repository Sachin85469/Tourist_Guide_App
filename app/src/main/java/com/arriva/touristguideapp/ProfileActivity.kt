package com.arriva.touristguideapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.repository.ProfileRepository
import com.arriva.touristguideapp.sos.ui.SOSSettingsActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: ProfileRepository
    
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var cardEditName: MaterialCardView
    private lateinit var etEditName: TextInputEditText
    
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        repository = ProfileRepository(this)

        initViews()
        loadUserData()
        observeStats()
        applyAnimations()
    }

    private fun initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        cardEditName = findViewById(R.id.cardEditName)
        etEditName = findViewById(R.id.etEditName)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // Setup settings rows
        setupRow(findViewById(R.id.btnEditProfile), R.drawable.ic_account, "Edit Profile", "Update your name and photo") {
            cardEditName.visibility = if (cardEditName.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (cardEditName.visibility == View.VISIBLE) {
                etEditName.setText(tvProfileName.text)
            }
        }

        setupRow(findViewById(R.id.btnMyReviews), R.drawable.ic_star, "My Reviews", "View and manage your feedback") {
            Toast.makeText(this, "My Reviews feature coming soon", Toast.LENGTH_SHORT).show()
        }

        setupRow(findViewById(R.id.btnTripHistory), R.drawable.ic_trip, "Trip History", "View your past and upcoming trips") {
            Toast.makeText(this, "Trip History feature coming soon", Toast.LENGTH_SHORT).show()
        }

        setupRow(findViewById(R.id.btnSecurity), R.drawable.ic_security, "Security", "Manage your account privacy") {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        setupRow(findViewById(R.id.btnNotificationSettings), R.drawable.ic_notification, "Notifications", "Control alerts and sounds") {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        setupRow(findViewById(R.id.btnLanguage), R.drawable.ic_language, "Language", "Choose your preferred language") {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        setupRow(findViewById(R.id.btnSOSSettings), R.drawable.ic_sos, "SOS Settings", "Manage emergency triggers") {
            startActivity(Intent(this, SOSSettingsActivity::class.java))
        }

        setupRow(findViewById(R.id.btnLogoutRow), R.drawable.ic_logout, "Sign Out", "Safely log out of your account") {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.btnSaveName).setOnClickListener {
            val newName = etEditName.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateProfile(mapOf("name" to newName))
            }
        }

        ivProfileImage.setOnClickListener {
            openGallery()
        }
    }

    private fun setupRow(row: View, icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
        row.findViewById<ImageView>(R.id.settingIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.settingTitle).text = title
        row.findViewById<TextView>(R.id.settingSubtitle).text = subtitle
        row.setOnClickListener { onClick() }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        tvProfileName.text = user.displayName ?: "User"
        tvProfileEmail.text = user.email

        lifecycleScope.launch {
            val profile = repository.getUserProfile()
            profile?.let {
                tvProfileName.text = it.name
                if (!it.profileImage.isNullOrEmpty()) {
                    Glide.with(this@ProfileActivity)
                        .load(it.profileImage)
                        .circleCrop()
                        .into(ivProfileImage)
                } else {
                    ivProfileImage.setImageDrawable(ProfileUtils.generateLetterAvatar(this@ProfileActivity, it.name))
                }
            }
        }
    }

    private fun observeStats() {
        lifecycleScope.launch {
            repository.getStats().collect { stats ->
                updateStat(R.id.statTrips, stats["trips"] ?: 0, "Trips")
                updateStat(R.id.statFavs, stats["favorites"] ?: 0, "Saved")
                updateStat(R.id.statReviews, stats["reviews"] ?: 0, "Reviews")
            }
        }
    }

    private fun updateStat(rowId: Int, value: Long, label: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.tvStatValue).text = value.toString()
        row.findViewById<TextView>(R.id.tvStatLabel).text = label
    }

    private fun updateProfile(updates: Map<String, Any>) {
        lifecycleScope.launch {
            try {
                repository.updateProfile(updates)
                Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                cardEditName.visibility = View.GONE
                loadUserData()
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri = data.data!!
            uploadProfileImage(imageUri)
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val downloadUrl = repository.uploadProfileImage(uri)
                updateProfile(mapOf("profileImage" to downloadUrl))
                Glide.with(this@ProfileActivity).load(uri).circleCrop().into(ivProfileImage)
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyAnimations() {
        findViewById<View>(R.id.cardProfileImage).apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()
        }
        tvProfileName.apply {
            translationY = 50f
            alpha = 0f
            animate().translationY(0f).alpha(1f).setDuration(600).setStartDelay(200).start()
        }
    }
}
