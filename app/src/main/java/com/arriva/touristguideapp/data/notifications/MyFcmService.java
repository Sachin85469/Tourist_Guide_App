package com.arriva.touristguideapp.data.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.arriva.touristguideapp.MainActivity;
import com.arriva.touristguideapp.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFcmService extends FirebaseMessagingService {
    private static final String TAG = "MyFcmService";
    private static final String CHANNEL_ID = "tourist_guide_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "NOTIFICATION_RECEIVED from: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        String type = NotificationRepository.TYPE_ADMIN;

        // Handle data payload
        if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            type = remoteMessage.getData().get("type");
        }

        // Handle notification payload fallback
        if (remoteMessage.getNotification() != null) {
            if (title == null) title = remoteMessage.getNotification().getTitle();
            if (body == null) body = remoteMessage.getNotification().getBody();
        }

        if (title != null && body != null) {
            NotificationRepository repository = new NotificationRepository(this);
            
            // Check user preference
            if (repository.isEnabled(type)) {
                showNotification(title, body, type);
                
                // Save to history
                NotificationModel model = new NotificationModel(
                    java.util.UUID.randomUUID().toString(),
                    title, body, type, System.currentTimeMillis()
                );
                repository.saveToHistory(model);
            } else {
                Log.d(TAG, "NOTIFICATION_MUTED: type=" + type);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "FCM_TOKEN_REGISTERED: " + token);
        new NotificationRepository(this).registerToken(token);
    }

    private void showNotification(String title, String message, String type) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Tourism Updates", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("notification_type", type);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        Log.d(TAG, "NOTIFICATION_SENT: " + title);
    }
}
