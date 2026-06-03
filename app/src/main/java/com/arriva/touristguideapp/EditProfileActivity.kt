package com.arriva.touristguideapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.repository.ProfileRepository
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

class EditProfileActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: ProfileRepository

    private lateinit var ivProfileImage: ImageView
    private lateinit var btnUploadPhoto: Button
    private lateinit var btnRemovePhoto: Button

    private lateinit var tilFullName: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var tilUsername: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText

    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var rbOther: RadioButton

    private lateinit var tilDob: TextInputLayout
    private lateinit var etDob: TextInputEditText

    private lateinit var tilCountry: TextInputLayout
    private lateinit var etCountry: TextInputEditText
    private lateinit var tilState: TextInputLayout
    private lateinit var etState: TextInputEditText
    private lateinit var tilCity: TextInputLayout
    private lateinit var etCity: TextInputEditText

    private lateinit var spnTravelCategory: Spinner
    private lateinit var tilBio: TextInputLayout
    private lateinit var etBio: TextInputEditText

    private lateinit var btnSaveProfile: Button

    private var selectedImageUri: Uri? = null
    private var isImageRemoved = false
    private var currentProfileImageUrl: String? = null

    private val PICK_IMAGE_REQUEST = 200
    private val travelCategories = arrayOf("Temples", "Museums", "Adventure", "Nature", "Historical", "Food", "Shopping")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        repository = ProfileRepository(this)

        initViews()
        setupListeners()
        loadUserProfile()
    }

    private fun initViews() {
        ivProfileImage = findViewById(R.id.ivEditProfileImage)
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)

        tilFullName = findViewById(R.id.tilFullName)
        etFullName = findViewById(R.id.etFullName)
        tilUsername = findViewById(R.id.tilUsername)
        etUsername = findViewById(R.id.etUsername)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPhone = findViewById(R.id.tilPhone)
        etPhone = findViewById(R.id.etPhone)

        rgGender = findViewById(R.id.rgGender)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)
        rbOther = findViewById(R.id.rbOther)

        tilDob = findViewById(R.id.tilDob)
        etDob = findViewById(R.id.etDob)

        tilCountry = findViewById(R.id.tilCountry)
        etCountry = findViewById(R.id.etCountry)
        tilState = findViewById(R.id.tilState)
        etState = findViewById(R.id.etState)
        tilCity = findViewById(R.id.tilCity)
        etCity = findViewById(R.id.etCity)

        spnTravelCategory = findViewById(R.id.spnTravelCategory)
        tilBio = findViewById(R.id.tilBio)
        etBio = findViewById(R.id.etBio)

        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // Setup Travel Category Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, travelCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnTravelCategory.adapter = adapter
    }

    private fun setupListeners() {
        btnUploadPhoto.setOnClickListener { openGallery() }
        btnRemovePhoto.setOnClickListener { removePhoto() }

        etDob.setOnClickListener { showDatePicker() }
        tilDob.setStartIconOnClickListener { showDatePicker() }

        btnSaveProfile.setOnClickListener { saveProfile() }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        
        // 1. First load cached profile for immediate values
        val cached = repository.getCachedUserProfile()
        cached?.let { populateUI(it) }

        // 2. Fetch fresh profile from Firestore
        lifecycleScope.launch {
            try {
                val profile = repository.getUserProfile()
                profile?.let { populateUI(it) }
            } catch (e: Exception) {
                // Keep showing cached data
            }
        }
    }

    private fun populateUI(user: User) {
        etFullName.setText(user.fullName ?: user.name ?: "")
        etUsername.setText(user.username ?: "")
        etEmail.setText(user.email ?: auth.currentUser?.email ?: "")
        etPhone.setText(user.phoneNumber ?: "")

        when (user.gender?.lowercase(Locale.getDefault())) {
            "male" -> rbMale.isChecked = true
            "female" -> rbFemale.isChecked = true
            "other" -> rbOther.isChecked = true
            else -> rgGender.clearCheck()
        }

        etDob.setText(user.dateOfBirth ?: "")
        etCountry.setText(user.country ?: "")
        etState.setText(user.state ?: "")
        etCity.setText(user.city ?: "")

        val categoryIndex = travelCategories.indexOf(user.favoriteTravelCategory)
        if (categoryIndex >= 0) {
            spnTravelCategory.setSelection(categoryIndex)
        }

        etBio.setText(user.bio ?: "")

        currentProfileImageUrl = user.profileImage

        // Load local file image if available in UserPrefs, otherwise load URL
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val localImageUri = prefs.getString("local_profile_image", null)

        if (!isImageRemoved) {
            if (localImageUri != null) {
                Glide.with(this)
                    .load(Uri.parse(localImageUri))
                    .circleCrop()
                    .placeholder(R.drawable.ic_account)
                    .into(ivProfileImage)
            } else if (!user.profileImage.isNullOrEmpty()) {
                Glide.with(this)
                    .load(user.profileImage)
                    .circleCrop()
                    .placeholder(R.drawable.ic_account)
                    .into(ivProfileImage)
            } else {
                ivProfileImage.setImageDrawable(ProfileUtils.generateLetterAvatar(this, user.fullName ?: user.name ?: "U"))
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun removePhoto() {
        isImageRemoved = true
        selectedImageUri = null
        ivProfileImage.setImageDrawable(ProfileUtils.generateLetterAvatar(this, etFullName.text.toString().trim().ifEmpty { "U" }))
        Toast.makeText(this, "Profile photo removed from edit session", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri = data.data!!
            selectedImageUri = imageUri
            isImageRemoved = false
            Glide.with(this)
                .load(imageUri)
                .circleCrop()
                .placeholder(R.drawable.ic_account)
                .into(ivProfileImage)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dobString = etDob.text.toString().trim()
        if (dobString.isNotEmpty()) {
            val parts = dobString.split("/")
            if (parts.size == 3) {
                try {
                    calendar.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                    calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                    calendar.set(Calendar.YEAR, parts[2].toInt())
                } catch (e: Exception) {
                    // Ignore, use current date
                }
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val dob = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                etDob.setText(dob)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun saveProfile() {
        // Reset errors
        tilFullName.error = null
        tilUsername.error = null
        tilEmail.error = null
        tilPhone.error = null

        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        var isValid = true

        if (fullName.isEmpty()) {
            tilFullName.error = "Name cannot be empty"
            isValid = false
        }

        if (username.isEmpty()) {
            tilUsername.error = "Username cannot be empty"
            isValid = false
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            isValid = false
        }

        if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches() || phone.length < 7) {
            tilPhone.error = "Enter a valid phone number"
            isValid = false
        }

        if (!isValid) return

        // Show progress overlay / disable save button
        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Saving..."

        lifecycleScope.launch {
            try {
                var finalImageUrl = currentProfileImageUrl ?: ""

                // Handle image removal
                if (isImageRemoved) {
                    finalImageUrl = ""
                    val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("local_profile_image").apply()
                }

                // Handle new photo upload
                val pickedUri = selectedImageUri
                if (pickedUri != null) {
                    // Upload to Firebase Storage
                    val downloadUrl = repository.uploadProfileImage(pickedUri)
                    finalImageUrl = downloadUrl

                    // Cache photo locally to internal storage for instant offline reload
                    val localSavedUri = saveImageToInternalStorage(pickedUri)
                    if (localSavedUri != null) {
                        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("local_profile_image", localSavedUri.toString()).apply()
                    }
                }

                // Gather properties into User object
                val user = User().apply {
                    uid = auth.currentUser?.uid ?: ""
                    name = fullName
                    setFullName(fullName)
                    setEmail(email)
                    setUsername(username)
                    setPhoneNumber(phone)
                    profileImage = finalImageUrl
                    profilePhoto = finalImageUrl
                    
                    val selectedGender = when (rgGender.checkedRadioButtonId) {
                        R.id.rbMale -> "Male"
                        R.id.rbFemale -> "Female"
                        R.id.rbOther -> "Other"
                        else -> ""
                    }
                    gender = selectedGender
                    dateOfBirth = etDob.text.toString().trim()
                    country = etCountry.text.toString().trim()
                    state = etState.text.toString().trim()
                    city = etCity.text.toString().trim()
                    favoriteTravelCategory = spnTravelCategory.selectedItem.toString()
                    bio = etBio.text.toString().trim()
                }

                // Save user profile
                repository.saveUserProfile(user)

                // Generate profile update notification
                try {
                    val notif = com.arriva.touristguideapp.data.notifications.NotificationModel(
                        java.util.UUID.randomUUID().toString(),
                        "Profile Updated",
                        "Your profile information was updated successfully.",
                        com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_SYSTEM,
                        System.currentTimeMillis()
                    ).apply {
                        userId = auth.currentUser?.uid ?: ""
                        isRead = false
                    }
                    com.arriva.touristguideapp.data.notifications.NotificationRepository(this@EditProfileActivity).saveToHistory(notif)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Toast.makeText(this@EditProfileActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "Save Profile Changes"
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(filesDir, "profile_avatar.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
