package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null) {
                openLogin();
                return;
            }

            if (BaseActivity.isGoogleUser(currentUser)) {
                openMain();
                return;
            }

            if (!BaseActivity.isEmailPasswordUser(currentUser)) {
                auth.signOut();
                openLogin();
                return;
            }

            currentUser.reload().addOnCompleteListener(task -> {
                FirebaseUser refreshedUser = auth.getCurrentUser();
                if (task.isSuccessful()
                        && refreshedUser != null
                        && refreshedUser.isEmailVerified()) {
                    openMain();
                } else {
                    auth.signOut();
                    openLogin();
                }
            });
        }, 2000);
    }

    private void openMain() {
        InterestSelectionNavigator.openNext(this);
    }

    private void openLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
