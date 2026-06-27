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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "GoogleSignIn";
    private static final String VERIFICATION_MESSAGE =
            "Please verify your email before logging in.";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private View btnGoogleSignIn;
    private TextView tvCreateAccount, tvForgotPassword;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.loginProgressBar);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Email Login
        btnLogin.setOnClickListener(v -> loginUser());

        // Google Sign In
        btnGoogleSignIn.setOnClickListener(v -> signIn());

        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());

        // Redirect to Signup
        tvCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email to reset password");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Password reset email sent.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Could not send reset email.";
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signIn() {
        // Sign out from Google to force account chooser if needed
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                // Google Sign In failed
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                checkUserInFirestore(user);
                            } else {
                                Toast.makeText(
                                        LoginActivity.this,
                                        "Authentication failed.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.e(TAG, "Firebase auth with google failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && !document.exists()) {
                            // Automatically create Firestore document if it does not exist
                            User newUser = new User(
                                    user.getUid(),
                                    user.getDisplayName(),
                                    user.getEmail(),
                                    "" // profileImage initially empty
                            );
                            db.collection("users").document(user.getUid()).set(newUser)
                                    .addOnCompleteListener(createTask -> {
                                        if (!createTask.isSuccessful()) {
                                            Log.e(TAG, "Failed to create Google user document", createTask.getException());
                                        }
                                        Toast.makeText(LoginActivity.this, "Welcome " + (user.getDisplayName() != null ? user.getDisplayName() : ""), Toast.LENGTH_SHORT).show();
                                        navigateToHome();
                                    });
                            return;
                        }
                        Toast.makeText(LoginActivity.this, "Welcome " + (user.getDisplayName() != null ? user.getDisplayName() : ""), Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        Log.e(TAG, "Firestore check failed", task.getException());
                        navigateToHome(); // Still navigate even if check fails, better than blocking user
                    }
                });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            progressBar.setVisibility(View.GONE);
                            mAuth.signOut();
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login failed. Please try again.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        user.reload().addOnCompleteListener(reloadTask -> {
                            progressBar.setVisibility(View.GONE);
                            FirebaseUser refreshedUser = mAuth.getCurrentUser();

                            if (!reloadTask.isSuccessful() || refreshedUser == null) {
                                mAuth.signOut();
                                Toast.makeText(
                                        LoginActivity.this,
                                        "Could not verify your email status. Please try again.",
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            if (!refreshedUser.isEmailVerified()) {
                                mAuth.signOut();
                                Toast.makeText(
                                        LoginActivity.this,
                                        VERIFICATION_MESSAGE,
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            navigateToHome();
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Login Failed: " + message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recordLoginSession() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String deviceId = BaseActivity.getDeviceId(this);
        String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        long timestamp = System.currentTimeMillis();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Record device
        java.util.Map<String, Object> deviceData = new java.util.HashMap<>();
        deviceData.put("deviceId", deviceId);
        deviceData.put("deviceName", deviceName);
        deviceData.put("lastActiveTime", timestamp);

        db.collection("users")
                .document(uid)
                .collection("devices")
                .document(deviceId)
                .set(deviceData);

        // 2. Log login history
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        java.util.Date now = new java.util.Date(timestamp);

        java.util.Map<String, Object> historyData = new java.util.HashMap<>();
        historyData.put("date", dateFormat.format(now));
        historyData.put("time", timeFormat.format(now));
        historyData.put("device", deviceName);
        historyData.put("timestamp", timestamp);

        db.collection("users")
                .document(uid)
                .collection("login_history")
                .add(historyData);
    }

    private void navigateToHome() {
        recordLoginSession();
        InterestSelectionNavigator.openNext(this);
    }
}
