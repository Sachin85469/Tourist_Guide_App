package com.arriva.touristguideapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileUtils {

    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(Exception e);
    }

    public interface AdminCallback {
        void onResult(boolean isAdmin);
    }

    /**
     * Fetches user data from Firestore.
     */
    public static void fetchUserData(String uid, UserCallback callback) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        callback.onUserLoaded(user);
                    } else {
                        callback.onError(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Loads the current user's profile image or a letter avatar fallback into an ImageView using Firestore data.
     * Priority: SharedPreferences (Local) > Firestore (Remote) > Auth (Google) > Letter Avatar
     */
    public static void loadAvatar(Context context, ImageView imageView) {
        // 1. Check SharedPreferences for local image persistence (Requirement 1 & 3)
        android.content.SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String localImageUri = prefs.getString("local_profile_image", null);

        if (localImageUri != null) {
            Glide.with(context)
                    .load(android.net.Uri.parse(localImageUri))
                    .circleCrop()
                    .into(imageView);
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        fetchUserData(currentUser.getUid(), new UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                if (!TextUtils.isEmpty(user.getProfileImage())) {
                    Glide.with(context)
                            .load(user.getProfileImage())
                            .circleCrop()
                            .placeholder(generateLetterAvatar(context, user.getName()))
                            .into(imageView);
                } else {
                    imageView.setImageDrawable(generateLetterAvatar(context, user.getName()));
                }
            }

            @Override
            public void onError(Exception e) {
                // Fallback to Auth display name if Firestore fails
                imageView.setImageDrawable(generateLetterAvatar(context, currentUser.getDisplayName()));
            }
        });
    }

    /**
     * Overloaded loadAvatar to use provided User object directly (efficiency).
     * Still checks SharedPreferences first for immediate local updates.
     */
    public static void loadAvatar(Context context, ImageView imageView, User user) {
        // 1. Check SharedPreferences for local image persistence
        android.content.SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String localImageUri = prefs.getString("local_profile_image", null);

        if (localImageUri != null) {
            Glide.with(context)
                    .load(android.net.Uri.parse(localImageUri))
                    .circleCrop()
                    .into(imageView);
            return;
        }

        if (user == null) return;
        
        if (!TextUtils.isEmpty(user.getProfileImage())) {
            Glide.with(context)
                    .load(user.getProfileImage())
                    .circleCrop()
                    .placeholder(generateLetterAvatar(context, user.getName()))
                    .into(imageView);
        } else {
            imageView.setImageDrawable(generateLetterAvatar(context, user.getName()));
        }
    }

    /**
     * Generates a circular Drawable with a purple gradient background and the first letter of the name.
     */
    public static void checkAdminStatus(String uid, AdminCallback callback) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    callback.onResult("admin".equals(role) || "moderator".equals(role));
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public static Drawable generateLetterAvatar(Context context, String name) {
        String letter = "U"; // Default to U for User
        if (!TextUtils.isEmpty(name)) {
            letter = name.substring(0, 1).toUpperCase();
        }

        int size = 120; // Size in pixels
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 1. Draw Gradient Circle
        int startColor = ContextCompat.getColor(context, R.color.purple_gradient_start);
        int endColor = ContextCompat.getColor(context, R.color.purple_gradient_end);
        
        LinearGradient gradient = new LinearGradient(0, 0, size, size, 
                startColor, endColor, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // 2. Draw Text
        paint.setShader(null);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size * 0.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        paint.getTextBounds(letter, 0, 1, bounds);
        float y = (size / 2f) - bounds.centerY();
        
        canvas.drawText(letter, size / 2f, y, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
