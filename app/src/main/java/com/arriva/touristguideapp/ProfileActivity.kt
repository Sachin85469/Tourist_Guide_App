package com.arriva.touristguideapp

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: ProfileRepository
    private lateinit var notificationRepository: com.arriva.touristguideapp.data.notifications.NotificationRepository
    
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        repository = ProfileRepository(this)

        notificationRepository = com.arriva.touristguideapp.data.notifications.NotificationRepository(this)
        initViews()
        applyAnimations()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        observeStats()
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val row = findViewById<View>(R.id.btnNotificationSettings)
        val badge = row.findViewById<TextView>(R.id.tvRowBadge)
        val unreadCount = notificationRepository.getUnreadCount()
        if (unreadCount > 0) {
            badge.text = unreadCount.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // Setup settings rows
        setupRow(findViewById(R.id.btnEditProfile), R.drawable.ic_account, "Edit Profile", "Update your name and photo") {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        setupRow(findViewById(R.id.btnMyReviews), R.drawable.ic_star, "My Reviews", "View and manage your feedback") {
            startActivity(Intent(this, MyReviewsActivity::class.java))
        }

        setupRow(findViewById(R.id.btnTripHistory), R.drawable.ic_trip, "Trip History", "View your past and upcoming trips") {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }

        setupRow(findViewById(R.id.btnSecurity), R.drawable.ic_security, "Security", "Manage your account privacy") {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        setupRow(findViewById(R.id.btnNotificationSettings), R.drawable.ic_notification, "Notifications", "View your alerts and updates") {
            startActivity(Intent(this, NotificationHistoryActivity::class.java))
        }

        setupRow(findViewById(R.id.btnLanguage), R.drawable.ic_language, "Language", "Choose your preferred language") {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        setupRow(findViewById(R.id.btnSOSSettings), R.drawable.ic_sos, "SOS Settings", "Manage emergency triggers") {
            startActivity(Intent(this, SOSSettingsActivity::class.java))
        }

        setupRow(findViewById(R.id.btnLogoutRow), R.drawable.ic_logout, "Sign Out", "Safely log out of your account") {
            auth.signOut()
            // Reset local cache
            repository.cacheUserProfile(User())
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        ivProfileImage.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
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
        
        // Load cached details first
        val cached = repository.getCachedUserProfile()
        val displayName = cached?.fullName ?: cached?.name ?: user.displayName ?: "Complete Your Profile"
        val username = if (cached?.username.isNullOrEmpty()) "Complete Your Profile" else cached?.username
        val email = user.email ?: "Complete Your Profile"
        
        tvProfileName.text = displayName
        findViewById<TextView>(R.id.tvProfileUsername).text = username
        tvProfileEmail.text = email

        if (!cached?.profileImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(cached?.profileImage)
                .circleCrop()
                .placeholder(R.drawable.ic_account)
                .into(ivProfileImage)
        } else {
            ivProfileImage.setImageDrawable(ProfileUtils.generateLetterAvatar(this, displayName))
        }

        // Fetch fresh from Firestore
        lifecycleScope.launch {
            val profile = repository.getUserProfile()
            profile?.let {
                val updatedName = it.fullName ?: it.name ?: "Complete Your Profile"
                tvProfileName.text = updatedName
                findViewById<TextView>(R.id.tvProfileUsername).text = if (it.username.isNullOrEmpty()) "Complete Your Profile" else it.username
                tvProfileEmail.text = it.email ?: "Complete Your Profile"
                
                if (!it.profileImage.isNullOrEmpty()) {
                    Glide.with(this@ProfileActivity)
                        .load(it.profileImage)
                        .circleCrop()
                        .placeholder(R.drawable.ic_account)
                        .into(ivProfileImage)
                } else {
                    ivProfileImage.setImageDrawable(ProfileUtils.generateLetterAvatar(this@ProfileActivity, updatedName))
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
