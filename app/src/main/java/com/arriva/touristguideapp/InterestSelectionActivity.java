package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class InterestSelectionActivity extends BaseActivity {
    private static final String TAG = "InterestSelectionActivity";
    public static final String EXTRA_PROFILE_EDIT_MODE = "com.arriva.touristguideapp.PROFILE_EDIT_MODE";

    private ChipGroup chipGroupInterests;
    private MaterialButton btnContinue;
    private TextView tvSkipForNow;
    private ProgressBar progressBar;
    private boolean profileEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interest_selection);

        profileEditMode = getIntent().getBooleanExtra(EXTRA_PROFILE_EDIT_MODE, false);

        chipGroupInterests = findViewById(R.id.chipGroupInterests);
        btnContinue = findViewById(R.id.btnContinueInterests);
        tvSkipForNow = findViewById(R.id.tvSkipForNow);
        progressBar = findViewById(R.id.interestProgressBar);

        if (profileEditMode) {
            btnContinue.setText("Save");
            tvSkipForNow.setVisibility(View.GONE);
            loadExistingInterests();
        }

        btnContinue.setOnClickListener(v -> saveInterests(collectSelectedInterests()));
        tvSkipForNow.setOnClickListener(v -> saveInterests(new ArrayList<>()));
    }

    private List<String> collectSelectedInterests() {
        List<String> selectedInterests = new ArrayList<>();
        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
            View child = chipGroupInterests.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    selectedInterests.add(chip.getText().toString());
                }
            }
        }
        return selectedInterests;
    }

    private void loadExistingInterests() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            redirectToLogin();
            return;
        }

        setSaving(true);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Object savedInterests = documentSnapshot.get(InterestSelectionNavigator.FIELD_TRAVEL_INTERESTS);
                    if (savedInterests instanceof List<?>) {
                        preselectInterests((List<?>) savedInterests);
                    }
                    setSaving(false);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Could not load travel interests", error);
                    setSaving(false);
                    Toast.makeText(
                            this,
                            "Could not load your current choices.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void preselectInterests(List<?> savedInterests) {
        Set<String> selected = new HashSet<>();
        for (Object interest : savedInterests) {
            if (interest instanceof String) {
                selected.add(normalizeInterest((String) interest));
            }
        }

        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
            View child = chipGroupInterests.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(selected.contains(normalizeInterest(chip.getText().toString())));
            }
        }
    }

    private String normalizeInterest(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        if (normalized.equals("history") || normalized.equals("historic")) {
            return "historical";
        }
        if (normalized.equals("spiritual") || normalized.equals("temple") || normalized.equals("temples")) {
            return "religious";
        }
        return normalized;
    }

    private void saveInterests(List<String> selectedInterests) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            redirectToLogin();
            return;
        }

        setSaving(true);
        User user = new User();
        user.setTravelInterests(selectedInterests);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .update(InterestSelectionNavigator.FIELD_TRAVEL_INTERESTS, user.getTravelInterests())
                .addOnSuccessListener(unused -> {
                    if (profileEditMode) {
                        openProfile();
                    } else {
                        InterestSelectionNavigator.openMain(this);
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Could not save travel interests", error);
                    setSaving(false);
                    Toast.makeText(
                            this,
                            "Could not save your choices. Please try again.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void openProfile() {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setSaving(boolean saving) {
        progressBar.setVisibility(saving ? View.VISIBLE : View.GONE);
        btnContinue.setEnabled(!saving);
        tvSkipForNow.setEnabled(!saving);
        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
            chipGroupInterests.getChildAt(i).setEnabled(!saving);
        }
    }
}
