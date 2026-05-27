package com.arriva.touristguideapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView ivProfileImage;
    private TextView tvProfileName, tvProfileEmail;
    private MaterialCardView cardEditName;
    private EditText etEditName;
    private Button btnSaveName;
    private View btnEditProfileRow, btnLogoutRow, btnSOSSettingsRow, btnNotificationSettingsRow, btnSecurityRow, btnLanguageRow;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadUserData();
        setupStats();
        applyAnimations();

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            if (cardEditName != null) {
                if (cardEditName.getVisibility() == View.VISIBLE) {
                    cardEditName.setVisibility(View.GONE);
                } else {
                    cardEditName.setVisibility(View.VISIBLE);
                    etEditName.setText(tvProfileName.getText().toString());
                }
            }
        });

        if (btnSOSSettingsRow != null) {
            btnSOSSettingsRow.setOnClickListener(v -> {
                startActivity(new Intent(this, com.arriva.touristguideapp.sos.ui.SOSSettingsActivity.class));
            });
        }

        if (btnNotificationSettingsRow != null) {
            btnNotificationSettingsRow.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationSettingsActivity.class));
            });
        }

        if (btnLogoutRow != null) {
            btnLogoutRow.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (ivProfileImage != null) {
            ivProfileImage.setOnClickListener(v -> openGallery());
        }

        if (btnSaveName != null) {
            btnSaveName.setOnClickListener(v -> {
                String newName = etEditName.getText().toString().trim();
                if (!TextUtils.isEmpty(newName)) {
                    updateNameInFirestore(newName);
                }
            });
        }
    }

    private void updateNameInFirestore(String newName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .update("name", newName)
                    .addOnSuccessListener(aVoid -> {
                        tvProfileName.setText(newName);
                        cardEditName.setVisibility(View.GONE);
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        loadUserData();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupStats() {
        // Set mock stats for premium look
        View statTrips = findViewById(R.id.statTrips);
        ((TextView) statTrips.findViewById(R.id.tvStatValue)).setText("12");
        ((TextView) statTrips.findViewById(R.id.tvStatLabel)).setText("Trips");

        View statFavs = findViewById(R.id.statFavs);
        ((TextView) statFavs.findViewById(R.id.tvStatValue)).setText("45");
        ((TextView) statFavs.findViewById(R.id.tvStatLabel)).setText("Saved");

        View statReviews = findViewById(R.id.statReviews);
        ((TextView) statReviews.findViewById(R.id.tvStatValue)).setText("8");
        ((TextView) statReviews.findViewById(R.id.tvStatLabel)).setText("Reviews");
    }

    private void applyAnimations() {
        if (findViewById(R.id.cardProfileImage) != null) {
            findViewById(R.id.cardProfileImage).setAlpha(0f);
            findViewById(R.id.cardProfileImage).setScaleX(0.5f);
            findViewById(R.id.cardProfileImage).setScaleY(0.5f);
            findViewById(R.id.cardProfileImage).animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start();
        }

        if (tvProfileName != null) {
            tvProfileName.setTranslationY(100f);
            tvProfileName.setAlpha(0f);
            tvProfileName.animate().translationY(0f).alpha(1f).setDuration(600).setStartDelay(200).start();
        }

        if (findViewById(R.id.statsRow) != null) {
            findViewById(R.id.statsRow).setTranslationY(100f);
            findViewById(R.id.statsRow).setAlpha(0f);
            findViewById(R.id.statsRow).animate().translationY(0f).alpha(1f).setDuration(600).setStartDelay(400).start();
        }
    }

    private void initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        cardEditName = findViewById(R.id.cardEditName);
        etEditName = findViewById(R.id.etEditName);
        btnSaveName = findViewById(R.id.btnSaveName);

        btnEditProfileRow = findViewById(R.id.btnEditProfile);
        btnSecurityRow = findViewById(R.id.btnSecurity);
        btnNotificationSettingsRow = findViewById(R.id.btnNotificationSettings);
        btnLanguageRow = findViewById(R.id.btnLanguage);
        btnSOSSettingsRow = findViewById(R.id.btnSOSSettings);
        btnLogoutRow = findViewById(R.id.btnLogoutRow);

        setupSettingRow(btnEditProfileRow, R.drawable.ic_account, "Edit Profile", "Update your name and photo");
        setupSettingRow(btnSecurityRow, R.drawable.ic_security, "Security", "Manage your account privacy");
        setupSettingRow(btnNotificationSettingsRow, R.drawable.ic_notification, "Notifications", "Control alerts and sounds");
        setupSettingRow(btnLanguageRow, R.drawable.ic_language, "Language", "Choose your preferred language");
        setupSettingRow(btnSOSSettingsRow, R.drawable.ic_sos, "SOS Settings", "Manage emergency triggers");
        setupSettingRow(btnLogoutRow, R.drawable.ic_logout, "Sign Out", "Safely log out of your account");

        if (btnLanguageRow != null) {
            btnLanguageRow.findViewById(R.id.settingActionIcon).setVisibility(View.GONE);
            btnLanguageRow.findViewById(R.id.settingSwitch).setVisibility(View.GONE);
        }

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupSettingRow(View row, int iconRes, String title, String subtitle) {
        if (row == null) return;
        ImageView icon = row.findViewById(R.id.settingIcon);
        TextView tvTitle = row.findViewById(R.id.settingTitle);
        TextView tvSubtitle = row.findViewById(R.id.settingSubtitle);

        if (icon != null) icon.setImageResource(iconRes);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }

    private void loadUserData() {
        // Load local avatar immediately for best UX
        ProfileUtils.loadAvatar(this, ivProfileImage);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            ProfileUtils.fetchUserData(user.getUid(), new ProfileUtils.UserCallback() {
                @Override
                public void onUserLoaded(User userModel) {
                    if (tvProfileName != null) tvProfileName.setText(userModel.getName());
                    if (tvProfileEmail != null) tvProfileEmail.setText(userModel.getEmail());
                    ProfileUtils.loadAvatar(ProfileActivity.this, ivProfileImage, userModel);
                }

                @Override
                public void onError(Exception e) {
                    // Fallback to basic Auth info
                    if (tvProfileName != null) tvProfileName.setText(user.getDisplayName());
                    if (tvProfileEmail != null) tvProfileEmail.setText(user.getEmail());
                    ProfileUtils.loadAvatar(ProfileActivity.this, ivProfileImage);
                }
            });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            
            // 1. Save to SharedPreferences for local persistence (Requirement 1 & 2)
            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("local_profile_image", imageUri.toString())
                    .apply();
            
            // 2. Show instantly in UI
            Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(ivProfileImage);
            
            Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
        }
    }
}