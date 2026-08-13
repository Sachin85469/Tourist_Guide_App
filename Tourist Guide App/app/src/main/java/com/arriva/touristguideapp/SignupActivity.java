package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends BaseActivity {

    private static final String TAG = "SignupActivity";
    private static final String VERIFICATION_MESSAGE =
            "Verification email sent. Please verify before logging in.";

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvLoginLink;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etSignupName);
        etEmail = findViewById(R.id.etSignupEmail);
        etPassword = findViewById(R.id.etSignupPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.signupProgressBar);

        btnSignup.setOnClickListener(v -> createAccount());

        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 1. Update Profile in Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);

                            // 2. Save User to Firestore
                            saveUserToFirestore(user, name, email);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            mAuth.signOut();
                            Toast.makeText(
                                    SignupActivity.this,
                                    "Signup failed. Please try again.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";
                        Toast.makeText(SignupActivity.this, "Signup Failed: " + message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        User user = new User(firebaseUser.getUid(), name, email, "");

        db.collection("users").document(firebaseUser.getUid()).set(user)
                .addOnSuccessListener(aVoid -> sendVerificationAndSignOut(firebaseUser))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user profile during signup", e);
                    sendVerificationAndSignOut(firebaseUser);
                });
    }

    private void sendVerificationAndSignOut(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            mAuth.signOut();

            if (task.isSuccessful()) {
                Toast.makeText(
                        SignupActivity.this,
                        VERIFICATION_MESSAGE,
                        Toast.LENGTH_LONG
                ).show();
                finish();
            } else {
                String message = task.getException() != null
                        ? task.getException().getMessage()
                        : "Unable to send verification email";
                Log.e(TAG, "Email verification could not be sent", task.getException());
                Toast.makeText(
                        SignupActivity.this,
                        "Could not send verification email: " + message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}
