package com.arriva.touristguideapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.repository.ProfileRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PrivacySettingsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileRepository: ProfileRepository

    private lateinit var swPublicProfile: SwitchCompat
    private lateinit var swRecommendations: SwitchCompat
    private lateinit var swAnalytics: SwitchCompat
    private lateinit var swPromoNotifications: SwitchCompat

    private var isInitializing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_settings)

        auth = FirebaseAuth.getInstance()
        profileRepository = ProfileRepository(this)

        swPublicProfile = findViewById(R.id.swPublicProfile)
        swRecommendations = findViewById(R.id.swRecommendations)
        swAnalytics = findViewById(R.id.swAnalytics)
        swPromoNotifications = findViewById(R.id.swPromoNotifications)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        setupToggles()
        loadPrivacyPreferences()
    }

    private fun loadPrivacyPreferences() {
        lifecycleScope.launch {
            try {
                val profile = profileRepository.getUserProfile()
                if (profile != null) {
                    swPublicProfile.isChecked = profile.isShowProfilePublicly
                    swRecommendations.isChecked = profile.isAllowRecommendations
                    swAnalytics.isChecked = profile.isShareAnalytics
                    swPromoNotifications.isChecked = profile.isReceiveNotifications
                }
            } catch (e: Exception) {
                Toast.makeText(this@PrivacySettingsActivity, "Failed to load preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isInitializing = false
            }
        }
    }

    private fun setupToggles() {
        swPublicProfile.setOnCheckedChangeListener { _, isChecked ->
            savePreference("showProfilePublicly", isChecked, "Public Profile Visibility updated")
        }

        swRecommendations.setOnCheckedChangeListener { _, isChecked ->
            savePreference("allowRecommendations", isChecked, "Personalized Recommendations updated")
        }

        swAnalytics.setOnCheckedChangeListener { _, isChecked ->
            savePreference("shareAnalytics", isChecked, "Anonymous Analytics sharing updated")
        }

        swPromoNotifications.setOnCheckedChangeListener { _, isChecked ->
            savePreference("receiveNotifications", isChecked, "Promotional Notifications preference updated")
        }
    }

    private fun savePreference(key: String, value: Boolean, successMsg: String) {
        if (isInitializing) return
        
        lifecycleScope.launch {
            try {
                // Update Firestore profile
                profileRepository.updateProfile(mapOf(key to value))
                Toast.makeText(this@PrivacySettingsActivity, successMsg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@PrivacySettingsActivity, "Error saving preference: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
