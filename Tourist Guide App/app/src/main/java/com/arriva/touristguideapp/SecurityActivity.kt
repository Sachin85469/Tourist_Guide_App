package com.arriva.touristguideapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.repository.ProfileRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class SecurityActivity : BaseActivity() {

    companion object {
        private const val TAG = "SecurityActivity"
        private const val PHONE_AUTH_TIMEOUT_SECONDS = 60L
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var profileRepository: ProfileRepository
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var pendingPhoneNumber: String? = null
    private var otpDialog: AlertDialog? = null
    private var otpCodeInput: TextInputEditText? = null
    private var isCompletingPhoneVerification = false

    private val phoneVerificationCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onCodeSent(
                newVerificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                super.onCodeSent(newVerificationId, token)
                verificationId = newVerificationId
                resendToken = token
                isCompletingPhoneVerification = false

                val phoneNumber = pendingPhoneNumber ?: return
                Log.d(TAG, "Phone OTP sent")
                Toast.makeText(
                    this@SecurityActivity,
                    "OTP sent to $phoneNumber",
                    Toast.LENGTH_SHORT
                ).show()
                showOtpEntryDialog(phoneNumber)
            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Phone verification completed automatically")
                verifyPhoneCredential(credential)
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                isCompletingPhoneVerification = false
                Log.e(TAG, "Phone verification failed", exception)
                Toast.makeText(
                    this@SecurityActivity,
                    "Verification failed: ${exception.message ?: "Unable to send OTP"}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onCodeAutoRetrievalTimeOut(expiredVerificationId: String) {
                verificationId = expiredVerificationId
                isCompletingPhoneVerification = false
                Log.d(TAG, "Phone OTP auto-retrieval timed out")
                Toast.makeText(
                    this@SecurityActivity,
                    "Automatic OTP detection timed out. Enter the code or tap Resend OTP.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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
                val inputLayout = view.findViewById<TextInputLayout>(R.id.textInputLayout)
                inputLayout.hint = "Phone Number with country code"
                inputLayout.endIconMode = TextInputLayout.END_ICON_NONE
                etPhoneInput.inputType = InputType.TYPE_CLASS_PHONE
                
                if (!profile?.phoneNumber.isNullOrEmpty()) {
                    etPhoneInput.setText(profile?.phoneNumber)
                }

                val dialog = MaterialAlertDialogBuilder(this@SecurityActivity)
                    .setTitle("Phone Verification")
                    .setMessage("Enter your phone number with country code, for example +919876543210.")
                    .setView(view)
                    .setPositiveButton("Send OTP", null)
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val phoneNumber = etPhoneInput.text.toString().trim()
                        if (isValidPhoneNumber(phoneNumber)) {
                            dialog.dismiss()
                            sendOtp(phoneNumber)
                        } else {
                            inputLayout.error = "Use international format, for example +919876543210"
                        }
                    }
                }
                dialog.show()
            } catch (e: Exception) {
                Toast.makeText(this@SecurityActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^\\+[1-9]\\d{7,14}$"))
    }

    private fun sendOtp(
        phoneNumber: String,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        pendingPhoneNumber = phoneNumber
        isCompletingPhoneVerification = false

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(PHONE_AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(phoneVerificationCallbacks)

        if (forceResendingToken != null) {
            optionsBuilder.setForceResendingToken(forceResendingToken)
        }

        try {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
            Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show()
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Could not start phone verification", exception)
            Toast.makeText(
                this,
                "Could not send OTP: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showOtpEntryDialog(phoneNumber: String) {
        otpDialog?.dismiss()
        val view = layoutInflater.inflate(R.layout.dialog_reauth, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val etCodeInput = view.findViewById<TextInputEditText>(R.id.etPassword)
        inputLayout.hint = "6-digit OTP code"
        inputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        etCodeInput.inputType = InputType.TYPE_CLASS_NUMBER
        otpCodeInput = etCodeInput

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirm OTP")
            .setMessage("Enter the verification code sent to $phoneNumber.")
            .setView(view)
            .setPositiveButton("Verify", null)
            .setNeutralButton("Resend OTP", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = etCodeInput.text.toString().trim()
                val currentVerificationId = verificationId
                if (code.length != 6) {
                    inputLayout.error = "Enter the 6-digit OTP"
                    return@setOnClickListener
                }
                if (currentVerificationId == null) {
                    Toast.makeText(this, "OTP session expired. Please resend.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                inputLayout.error = null
                val credential = PhoneAuthProvider.getCredential(currentVerificationId, code)
                verifyPhoneCredential(credential)
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val token = resendToken
                if (token == null) {
                    Toast.makeText(this, "Please wait before resending.", Toast.LENGTH_SHORT).show()
                } else {
                    sendOtp(phoneNumber, token)
                }
            }
        }

        dialog.setOnDismissListener {
            if (otpDialog === dialog) {
                otpDialog = null
                otpCodeInput = null
            }
        }
        otpDialog = dialog
        dialog.show()
    }

    private fun verifyPhoneCredential(credential: PhoneAuthCredential) {
        if (isCompletingPhoneVerification) return

        val user = auth.currentUser
        val phoneNumber = pendingPhoneNumber
        if (user == null || phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Your verification session expired.", Toast.LENGTH_SHORT).show()
            return
        }

        isCompletingPhoneVerification = true
        otpDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        otpDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = false

        user.updatePhoneNumber(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Firebase phone verification succeeded")
                otpDialog?.dismiss()
                clearPhoneVerificationState()
                saveVerifiedPhone(phoneNumber)
            } else {
                isCompletingPhoneVerification = false
                otpDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                otpDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = true
                Log.e(TAG, "Firebase phone verification failed", task.exception)
                Toast.makeText(
                    this,
                    "Incorrect or expired OTP: ${task.exception?.message ?: "Verification failed"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun clearPhoneVerificationState() {
        verificationId = null
        resendToken = null
        pendingPhoneNumber = null
        otpCodeInput = null
        otpDialog = null
        isCompletingPhoneVerification = false
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

    override fun onDestroy() {
        otpDialog?.dismiss()
        otpDialog = null
        super.onDestroy()
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
