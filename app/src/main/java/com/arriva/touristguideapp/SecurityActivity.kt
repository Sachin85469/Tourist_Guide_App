package com.arriva.touristguideapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
        updateUI()
    }

    private fun initViews() {
        findViewById<View>(R.id.toolbar).setOnClickListener { finish() }

        val btnChangePassword = findViewById<View>(R.id.btnChangePassword)
        setupRow(btnChangePassword, R.drawable.ic_lock, "Change Password", "Update your login credentials")
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        val btnVerifyEmail = findViewById<View>(R.id.btnVerifyEmail)
        setupRow(btnVerifyEmail, R.drawable.ic_check_circle, "Verify Email", "Ensure your email is confirmed")
        btnVerifyEmail.setOnClickListener { verifyEmail() }

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

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val current = etCurrent.text.toString()
                val new = etNew.text.toString()
                if (current.isNotEmpty() && new.length >= 6) {
                    reauthenticateAndChangePassword(current, new)
                } else {
                    Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reauthenticateAndChangePassword(current: String, new: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, current)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(new).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(this, "Password updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Update failed: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteAccountConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account?")
            .setMessage("This will permanently delete your profile, reviews, and saved places. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> showReauthForDelete() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReauthForDelete() {
        val view = layoutInflater.inflate(R.layout.dialog_reauth, null)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Deletion")
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
                Toast.makeText(this@SecurityActivity, "Account deleted", Toast.LENGTH_SHORT).show()
                
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
