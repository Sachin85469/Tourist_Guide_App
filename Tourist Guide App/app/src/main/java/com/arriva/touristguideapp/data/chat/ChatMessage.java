package com.arriva.touristguideapp.data.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@IgnoreExtraProperties
public class ChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private String id;
    private String role;
    private String content;
    private long timestamp;

    private transient boolean typing;
    private transient boolean error;

    public ChatMessage() {
        // Required by Firestore.
    }

    public ChatMessage(@NonNull String role, @NonNull String content, long timestamp) {
        this(UUID.randomUUID().toString(), role, content, timestamp);
    }

    public ChatMessage(@NonNull String id,
                       @NonNull String role,
                       @NonNull String content,
                       long timestamp) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    @NonNull
    public static ChatMessage typingIndicator() {
        ChatMessage message = new ChatMessage("typing", ROLE_ASSISTANT, "", System.currentTimeMillis());
        message.typing = true;
        return message;
    }

    @NonNull
    public static ChatMessage error(@NonNull String content) {
        ChatMessage message = new ChatMessage("error", ROLE_ASSISTANT, content, System.currentTimeMillis());
        message.error = true;
        return message;
    }

    @Nullable
    public static ChatMessage fromDocument(@NonNull DocumentSnapshot document) {
        String role = document.getString("role");
        String content = document.getString("content");
        Long timestamp = document.getLong("timestamp");
        if (role == null || content == null) {
            return null;
        }
        return new ChatMessage(
                document.getId(),
                role,
                content,
                timestamp != null ? timestamp : 0L
        );
    }

    @NonNull
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        data.put("content", content);
        data.put("timestamp", timestamp);
        return data;
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isTyping() {
        return typing;
    }

    public boolean isError() {
        return error;
    }
}
