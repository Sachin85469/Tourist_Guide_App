package com.arriva.touristguideapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.repository.ProfileRepository
import com.arriva.touristguideapp.profile.ProfileCompletionHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountInfoActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_info)

        auth = FirebaseAuth.getInstance()
        profileRepository = ProfileRepository(this)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        loadUserData()
    }

    private fun loadUserData() {
        val firebaseUser = auth.currentUser ?: return

        // 1. Set dates from auth metadata
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val creationTime = firebaseUser.metadata?.creationTimestamp
        val lastSignInTime = firebaseUser.metadata?.lastSignInTimestamp

        findViewById<TextView>(R.id.tvCreatedDateValue).text =
            if (creationTime != null && creationTime > 0) sdf.format(Date(creationTime)) else "N/A"
        findViewById<TextView>(R.id.tvLastLoginValue).text =
            if (lastSignInTime != null && lastSignInTime > 0) sdf.format(Date(lastSignInTime)) else "N/A"

        // 2. Set profile fields from Firestore
        lifecycleScope.launch {
            val userProfile = profileRepository.getUserProfile()
            if (userProfile != null) {
                findViewById<TextView>(R.id.tvFullNameValue).text =
                    userProfile.fullName ?: userProfile.name ?: "N/A"
                findViewById<TextView>(R.id.tvUsernameValue).text =
                    if (userProfile.username.isNullOrEmpty()) "N/A" else userProfile.username
                findViewById<TextView>(R.id.tvEmailValue).text =
                    if (userProfile.email.isNullOrEmpty()) firebaseUser.email ?: "N/A" else userProfile.email
                findViewById<TextView>(R.id.tvPhoneValue).text =
                    if (userProfile.phoneNumber.isNullOrEmpty()) "N/A" else userProfile.phoneNumber

                // Update Profile Completion Bar
                val completion = ProfileCompletionHelper.calculateCompletion(userProfile)
                findViewById<LinearProgressIndicator>(R.id.pbAccountCompletion).progress = completion
                findViewById<TextView>(R.id.tvAccountCompletionText).text =
                    getString(R.string.profile_completion_format, completion)
            } else {
                findViewById<TextView>(R.id.tvFullNameValue).text = firebaseUser.displayName ?: "N/A"
                findViewById<TextView>(R.id.tvUsernameValue).text = "N/A"
                findViewById<TextView>(R.id.tvEmailValue).text = firebaseUser.email ?: "N/A"
                findViewById<TextView>(R.id.tvPhoneValue).text = firebaseUser.phoneNumber ?: "N/A"

                val fallbackUser = User().apply {
                    email = firebaseUser.email
                    name = firebaseUser.displayName
                    fullName = firebaseUser.displayName
                }
                val completion = ProfileCompletionHelper.calculateCompletion(fallbackUser)
                findViewById<LinearProgressIndicator>(R.id.pbAccountCompletion).progress = completion
                findViewById<TextView>(R.id.tvAccountCompletionText).text =
                    getString(R.string.profile_completion_format, completion)
            }
        }
    }

}
