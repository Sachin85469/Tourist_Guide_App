package com.arriva.touristguideapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.repository.ProfileRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SecurityActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)

        auth = FirebaseAuth.getInstance()
        profileRepository = ProfileRepository(this)

        initViews()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        reloadUser()
    }

    private fun reloadUser() {
        val user = auth.currentUser ?: return
        lifecycleScope.launch {
            try {
                user.reload().await()
                updateUI()
            } catch (e: Exception) {
                // Ignore reload/network failures
            }
        }
    }

    private fun initViews() {
        findViewById<View>(R.id.toolbar).setOnClickListener { finish() }

        // Account Details Row
        val btnAccountInfo = findViewById<View>(R.id.btnAccountInfo)
        setupRow(btnAccountInfo, R.drawable.ic_account, "Account Information", "View full name, username, email & metadata")
        btnAccountInfo.setOnClickListener {
            startActivity(Intent(this, AccountInfoActivity::class.java))
        }

        // Change Password Row
        val btnChangePassword = findViewById<View>(R.id.btnChangePassword)
        setupRow(btnChangePassword, R.drawable.ic_lock, "Change Password", "Update your login credentials")
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        // Email Verification Row
        val btnVerifyEmail = findViewById<View>(R.id.btnVerifyEmail)
        setupRow(btnVerifyEmail, R.drawable.ic_check_circle, "Verify Email", "Ensure your email is confirmed")
        btnVerifyEmail.setOnClickListener { verifyEmail() }

        // Phone Verification Row
        val btnVerifyPhone = findViewById<View>(R.id.btnVerifyPhone)
        setupRow(btnVerifyPhone, R.drawable.ic_account, "Phone Verification", "Confirm your mobile number")
        btnVerifyPhone.setOnClickListener { showPhoneVerificationDialog() }

        // Active Device Management Row
        val btnDeviceManagement = findViewById<View>(R.id.btnDeviceManagement)
        setupRow(btnDeviceManagement, R.drawable.ic_account, "Active Devices", "Manage sessions and sign out other devices")
        btnDeviceManagement.setOnClickListener {
            startActivity(Intent(this, DeviceManagementActivity::class.java))
        }

        // Login History Row
        val btnLoginHistory = findViewById<View>(R.id.btnLoginHistory)
        setupRow(btnLoginHistory, R.drawable.ic_history, "Login History", "Review date, time and device of recent logins")
        btnLoginHistory.setOnClickListener {
            startActivity(Intent(this, LoginHistoryActivity::class.java))
        }

        // Privacy Settings Row
        val btnPrivacySettings = findViewById<View>(R.id.btnPrivacySettings)
        setupRow(btnPrivacySettings, R.drawable.ic_security, "Privacy Settings", "Configure sharing, ads, and profile visibility")
        btnPrivacySettings.setOnClickListener {
            startActivity(Intent(this, PrivacySettingsActivity::class.java))
        }

        // Delete Account Row
        val btnDeleteAccount = findViewById<View>(R.id.btnDeleteAccount)
        setupRow(btnDeleteAccount, R.drawable.ic_delete, "Delete Account", "Permanently remove your data")
        btnDeleteAccount.setOnClickListener { showDeleteAccountConfirmation() }
    }

    private fun setupRow(row: View, icon: Int, title: String, subtitle: String) {
        row.findViewById<ImageView>(R.id.settingIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.settingTitle).text = title
        row.findViewById<TextView>(R.id.settingSubtitle).text = subtitle
    }

    private fun updateUI() {
        val user = auth.currentUser
        val isVerified = user?.isEmailVerified == true
        findViewById<TextView>(R.id.tvEmailStatus).text = if (isVerified) "Email verified" else "Email not verified"
        findViewById<ImageView>(R.id.ivSecurityStatus).apply {
            setImageResource(if (isVerified) R.drawable.ic_check_circle else R.drawable.ic_security)
            setColorFilter(getColor(if (isVerified) R.color.indicator_green else R.color.indicator_red))
        }

        val btnVerifyEmail = findViewById<View>(R.id.btnVerifyEmail)
        btnVerifyEmail.findViewById<TextView>(R.id.settingSubtitle).text =
            if (isVerified) "Verified" else "Not Verified - Tap to verify"

        lifecycleScope.launch {
            try {
                val profile = profileRepository.getUserProfile()
                val phoneVerified = profile?.isPhoneVerified == true
                val btnVerifyPhone = findViewById<View>(R.id.btnVerifyPhone)
                btnVerifyPhone.findViewById<TextView>(R.id.settingSubtitle).text =
                    if (phoneVerified) "Verified" else "Not Verified - Tap to verify"
                btnVerifyPhone.findViewById<ImageView>(R.id.settingIcon).apply {
                    setImageResource(if (phoneVerified) R.drawable.ic_check_circle else R.drawable.ic_account)
                    setColorFilter(getColor(if (phoneVerified) R.color.indicator_green else R.color.adaptive_text_muted))
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun verifyEmail() {
        val user = auth.currentUser
        if (user == null) return
        if (user.isEmailVerified) {
            Toast.makeText(this, "Email is already verified", Toast.LENGTH_SHORT).show()
            return
        }

        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrent = view.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNew = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirm = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val tvStrength = view.findViewById<TextView>(R.id.tvPasswordStrength)

        etNew.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s?.toString() ?: ""
                val strength = checkPasswordStrength(password)
                tvStrength.text = "Password Strength: $strength"
                when (strength) {
                    "Weak" -> tvStrength.setTextColor(getColor(R.color.indicator_red))
                    "Medium" -> tvStrength.setTextColor(getColor(R.color.premium_gold))
                    "Strong" -> tvStrength.setTextColor(getColor(R.color.indicator_green))
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val current = etCurrent.text.toString()
                val new = etNew.text.toString()
                val confirm = etConfirm.text.toString()

                if (current.isEmpty()) {
                    Toast.makeText(this, "Current password is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (new.length < 8) {
                    Toast.makeText(this, "New password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (new != confirm) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                reauthenticateAndChangePassword(current, new)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkPasswordStrength(password: String): String {
        if (password.isEmpty()) return "Weak"
        
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?~`"
        if (password.any { specialChars.contains(it) }) score++
        
        return when {
            score <= 1 -> "Weak"
            score == 2 || score == 3 -> "Medium"
            else -> "Strong"
        }
    }

    private fun reauthenticateAndChangePassword(current: String, new: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, current)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(new).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Update failed: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPhoneVerificationDialog() {
        lifecycleScope.launch {
            try {
                val profile = profileRepository.getUserProfile()
                val phoneVerified = profile?.isPhoneVerified == true
                
                if (phoneVerified) {
                    MaterialAlertDialogBuilder(this@SecurityActivity)
                        .setTitle("Phone Verification")
                        .setMessage("Your phone number (${profile.phoneNumber ?: "registered phone"}) is already verified.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }
                
                val view = layoutInflater.inflate(R.layout.dialog_reauth, null)
                val etPhoneInput = view.findViewById<TextInputEditText>(R.id.etPassword)
                etPhoneInput.hint = "Phone Number"
                etPhoneInput.inputType = android.text.InputType.TYPE_CLASS_PHONE
                
                if (!profile?.phoneNumber.isNullOrEmpty()) {
                    etPhoneInput.setText(profile?.phoneNumber)
                }

                MaterialAlertDialogBuilder(this@SecurityActivity)
                    .setTitle("Phone Verification")
                    .setMessage("Enter your phone number to receive a verification OTP code.")
                    .setView(view)
                    .setPositiveButton("Send OTP") { _, _ ->
                        val phoneNumber = etPhoneInput.text.toString().trim()
                        if (phoneNumber.length >= 7) {
                            simulateOtpFlow(phoneNumber)
                        } else {
                            Toast.makeText(this@SecurityActivity, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@SecurityActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun simulateOtpFlow(phoneNumber: String) {
        val simulatedCode = (100000 + (Math.random() * 900000).toInt()).toString()
        
        Toast.makeText(this, "SMS Sent! Code: $simulatedCode", Toast.LENGTH_LONG).show()

        val view = layoutInflater.inflate(R.layout.dialog_reauth, null)
        val etCodeInput = view.findViewById<TextInputEditText>(R.id.etPassword)
        etCodeInput.hint = "6-Digit OTP Code"
        etCodeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm OTP")
            .setMessage("We simulated sending an SMS to $phoneNumber. Please enter the OTP code displayed in the popup toast.")
            .setView(view)
            .setPositiveButton("Verify") { _, _ ->
                val code = etCodeInput.text.toString().trim()
                if (code == simulatedCode) {
                    saveVerifiedPhone(phoneNumber)
                } else {
                    Toast.makeText(this, "Incorrect OTP code. Verification failed.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveVerifiedPhone(phoneNumber: String) {
        lifecycleScope.launch {
            try {
                val profile = profileRepository.getUserProfile() ?: User()
                profile.phoneNumber = phoneNumber
                profile.isPhoneVerified = true
                profileRepository.saveUserProfile(profile)
                
                Toast.makeText(this@SecurityActivity, "Phone verified successfully!", Toast.LENGTH_SHORT).show()
                updateUI()
            } catch (e: Exception) {
                Toast.makeText(this@SecurityActivity, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteAccountConfirmation() {
        val view = layoutInflater.inflate(R.layout.dialog_reauth, null)
        val etInput = view.findViewById<TextInputEditText>(R.id.etPassword)
        etInput.hint = "Type DELETE to confirm"
        etInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account?")
            .setMessage("This will permanently delete your profile, reviews, trips, and saved places. This action cannot be undone. To proceed, please type 'DELETE' below.")
            .setView(view)
            .setPositiveButton("Delete Permanently", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        val btnDelete = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnDelete.setOnClickListener {
            val typed = etInput.text.toString().trim()
            if (typed == "DELETE") {
                dialog.dismiss()
                showReauthForDelete()
            } else {
                Toast.makeText(this, "Please type 'DELETE' exactly as written to confirm.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReauthForDelete() {
        val view = layoutInflater.inflate(R.layout.dialog_reauth, null)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Password")
            .setMessage("Please enter your password to confirm.")
            .setView(view)
            .setPositiveButton("Confirm") { _, _ ->
                deleteAccount(etPassword.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount(password: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        lifecycleScope.launch {
            try {
                user.reauthenticate(credential).await()
                profileRepository.deleteAccount()
                Toast.makeText(this@SecurityActivity, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                
                val intent = Intent(this@SecurityActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@SecurityActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
