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
    private Chip chipLoginProvider;
    private MaterialCardView cardEditName;
    private EditText etEditName;
    private Button btnSaveName, btnEditProfile, btnLogout, btnAdminDashboard;
    private View btnBack, btnEditImage, dividerAdmin;

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

        btnBack.setOnClickListener(v -> finish());
        
        btnEditProfile.setOnClickListener(v -> {
            if (cardEditName.getVisibility() == View.VISIBLE) {
                cardEditName.setVisibility(View.GONE);
            } else {
                cardEditName.setVisibility(View.VISIBLE);
                etEditName.setText(tvProfileName.getText().toString());
            }
        });

        btnSaveName.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            if (!TextUtils.isEmpty(newName)) {
                updateNameInFirestore(newName);
            }
        });

        btnEditImage.setOnClickListener(v -> openGallery());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
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
        findViewById(R.id.avatarContainer).setAlpha(0f);
        findViewById(R.id.avatarContainer).setScaleX(0.5f);
        findViewById(R.id.avatarContainer).setScaleY(0.5f);
        findViewById(R.id.avatarContainer).animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start();

        findViewById(R.id.userInfo).setTranslationY(100f);
        findViewById(R.id.userInfo).setAlpha(0f);
        findViewById(R.id.userInfo).animate().translationY(0f).alpha(1f).setDuration(600).setStartDelay(200).start();

        findViewById(R.id.statsRow).setTranslationY(100f);
        findViewById(R.id.statsRow).setAlpha(0f);
        findViewById(R.id.statsRow).animate().translationY(0f).alpha(1f).setDuration(600).setStartDelay(400).start();

        findViewById(R.id.cardActions).setTranslationY(100f);
        findViewById(R.id.cardActions).setAlpha(0f);
        findViewById(R.id.cardActions).animate().translationY(0f).alpha(1f).setDuration(600).setStartDelay(600).start();
    }

    private void initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnEditImage = findViewById(R.id.btnEditImage);
        btnAdminDashboard = findViewById(R.id.btnAdminDashboard);
        dividerAdmin = findViewById(R.id.dividerAdmin);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        chipLoginProvider = findViewById(R.id.chipLoginProvider);
        cardEditName = findViewById(R.id.cardEditName);
        etEditName = findViewById(R.id.etEditName);
        btnSaveName = findViewById(R.id.btnSaveName);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadUserData() {
        // Load local avatar immediately for best UX
        ProfileUtils.loadAvatar(this, ivProfileImage);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Check Admin Status
            ProfileUtils.checkAdminStatus(user.getUid(), isAdmin -> {
                if (isAdmin) {
                    btnAdminDashboard.setVisibility(View.VISIBLE);
                    dividerAdmin.setVisibility(View.VISIBLE);
                    btnAdminDashboard.setOnClickListener(v -> {
                        Intent intent = new Intent(this, AdminDashboardActivity.class);
                        startActivity(intent);
                    });
                }
            });

            ProfileUtils.fetchUserData(user.getUid(), new ProfileUtils.UserCallback() {
                @Override
                public void onUserLoaded(User userModel) {
                    tvProfileName.setText(userModel.getName());
                    tvProfileEmail.setText(userModel.getEmail());

                    String provider = "Email Account";
                    for (UserInfo profile : user.getProviderData()) {
                        if (profile.getProviderId().equals("google.com")) {
                            provider = "Google Account";
                        }
                    }
                    chipLoginProvider.setText(provider);

                    ProfileUtils.loadAvatar(ProfileActivity.this, ivProfileImage, userModel);
                }

                @Override
                public void onError(Exception e) {
                    // Fallback to basic Auth info
                    tvProfileName.setText(user.getDisplayName());
                    tvProfileEmail.setText(user.getEmail());
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