package com.arriva.touristguideapp;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView ivProfileImage;
    private TextView tvProfileName, tvProfileEmail;
    private Chip chipLoginProvider;
    private MaterialCardView cardEditName;
    private EditText etEditName;
    private Button btnSaveName, btnEditProfile, btnLogout;
    private View btnBack, btnEditImage;

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

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
                saveNameLocally(newName);
                tvProfileName.setText(newName);
                cardEditName.setVisibility(View.GONE);
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                // Refresh avatar in case name changed (for fallback letter)
                loadAvatar();
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String name = user.getDisplayName();
            String provider = "Email Account";

            for (UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) {
                    provider = "Google Account";
                    if (TextUtils.isEmpty(name)) name = profile.getDisplayName();
                }
            }

            // Load local manual override if exists
            String savedName = prefs.getString("user_display_name", null);
            if (!TextUtils.isEmpty(savedName)) {
                name = savedName;
            }

            tvProfileEmail.setText(email);
            chipLoginProvider.setText(provider);
            tvProfileName.setText(!TextUtils.isEmpty(name) ? name : "Tourist");

            loadAvatar();
        }
    }

    private void loadAvatar() {
        // Check for locally saved image first
        String localImageUri = prefs.getString("local_profile_image", null);
        if (localImageUri != null) {
            Glide.with(this)
                    .load(Uri.parse(localImageUri))
                    .circleCrop()
                    .into(ivProfileImage);
        } else {
            // Fallback to dynamic avatar logic
            ProfileUtils.loadAvatar(this, ivProfileImage);
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
            saveImageLocally(imageUri);
            
            // Show instantly
            Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(ivProfileImage);
            
            Toast.makeText(this, "Photo updated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageLocally(Uri uri) {
        prefs.edit().putString("local_profile_image", uri.toString()).apply();
    }

    private void saveNameLocally(String name) {
        prefs.edit().putString("user_display_name", name).apply();
    }
}