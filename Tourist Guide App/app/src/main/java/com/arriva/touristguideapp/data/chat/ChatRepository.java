package com.arriva.touristguideapp.data.chat;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRepository {

    private static final String TAG = "ChatRepository";
    private static final String COLLECTION_CHATS = "chats";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final int DELETE_BATCH_SIZE = 450;

    private final FirebaseFirestore db;

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    public Task<List<ChatMessage>> loadMessages(@NonNull String userId) {
        Log.d(TAG, "CHAT_LOAD_START");
        Log.d(TAG, "USER_ID: " + userId);
        Log.d(
                TAG,
                "CHAT_PATH: " + COLLECTION_CHATS + "/" + userId + "/" + COLLECTION_MESSAGES
        );

        if (userId == null || userId.trim().isEmpty()) {
            IllegalArgumentException exception =
                    new IllegalArgumentException("Cannot load chat without a userId");
            Log.e(TAG, "CHAT_LOAD_FAILED: " + exception.getMessage(), exception);
            return Tasks.forException(exception);
        }

        return messagesRef(userId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        if (exception == null) {
                            exception = new Exception("Unknown Firestore chat load error");
                        }
                        Log.e(
                                TAG,
                                "CHAT_LOAD_FAILED: " + exception.getMessage(),
                                exception
                        );
                        throw exception;
                    }

                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.d(TAG, "CHAT_LOAD_SUCCESS: 0 messages");
                        return Collections.emptyList();
                    }

                    List<ChatMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ChatMessage message = ChatMessage.fromDocument(document);
                        if (message != null) {
                            messages.add(message);
                        }
                    }

                    Log.d(TAG, "CHAT_LOAD_SUCCESS: " + messages.size() + " messages");
                    return messages;
                });
    }

    @NonNull
    public Task<Void> saveMessage(@NonNull String userId, @NonNull ChatMessage message) {
        return messagesRef(userId)
                .document(message.getId())
                .set(message.toFirestoreMap());
    }

    @NonNull
    public Task<Void> clearMessages(@NonNull String userId) {
        return deleteNextBatch(messagesRef(userId));
    }

    @NonNull
    private Task<Void> deleteNextBatch(@NonNull CollectionReference messages) {
        return messages.limit(DELETE_BATCH_SIZE).get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception error = task.getException();
                return Tasks.forException(error != null ? error : new Exception("Could not load chat messages"));
            }

            QuerySnapshot snapshot = task.getResult();
            if (snapshot == null || snapshot.isEmpty()) {
                return Tasks.forResult(null);
            }

            WriteBatch batch = db.batch();
            snapshot.getDocuments().forEach(document -> batch.delete(document.getReference()));
            return batch.commit().continueWithTask(deleteTask -> {
                if (!deleteTask.isSuccessful()) {
                    Exception error = deleteTask.getException();
                    return Tasks.forException(error != null ? error : new Exception("Could not clear chat"));
                }
                return deleteNextBatch(messages);
            });
        });
    }

    @NonNull
    private CollectionReference messagesRef(@NonNull String userId) {
        return db.collection(COLLECTION_CHATS)
                .document(userId)
                .collection(COLLECTION_MESSAGES);
    }
}
