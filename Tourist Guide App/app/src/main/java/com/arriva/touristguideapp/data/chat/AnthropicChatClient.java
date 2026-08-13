package com.arriva.touristguideapp.data.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatClient that proxies conversations through the Arriva backend (/api/chat).
 *
 * <p>The Anthropic API key never leaves the server. The device authenticates
 * with a lightweight shared secret in the {@code X-App-Token} header.
 *
 * <p>Body sent to the backend:
 * <pre>
 * {
 *   "messages": [ { "role": "user"|"assistant", "content": "…" }, … ]
 * }
 * </pre>
 * Response expected:
 * <pre>
 * { "content": "AI response text" }
 * </pre>
 */
public class AnthropicChatClient implements ChatClient {

    private static final String TAG = "AnthropicChatClient";
    private static final String REQUEST_TAG = "arriva_chat";
    /** Timeout: 30 s, 1 attempt (no retry for chat — user can retry manually). */
    private static final int TIMEOUT_MS = 30_000;

    private final RequestQueue requestQueue;
    private final String chatEndpointUrl;
    private final String appToken;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * @param context          Application context used to create the Volley queue.
     * @param chatEndpointUrl  Full URL of the backend chat endpoint, e.g.
     *                         {@code https://arriva-backend.onrender.com/api/chat}.
     * @param appToken         Shared secret to send in {@code X-App-Token}.
     */
    public AnthropicChatClient(@NonNull Context context,
                               @NonNull String chatEndpointUrl,
                               @NonNull String appToken) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.chatEndpointUrl = chatEndpointUrl;
        this.appToken = appToken;
    }

    @Override
    public void sendConversation(@NonNull List<ChatMessage> conversation,
                                 @NonNull Callback callback) {
        JSONArray messagesJson = new JSONArray();
        for (ChatMessage msg : conversation) {
            if (msg.isTyping() || msg.isError()) continue;
            try {
                JSONObject m = new JSONObject();
                m.put("role", msg.isUser() ? "user" : "assistant");
                m.put("content", msg.getContent());
                messagesJson.put(m);
            } catch (JSONException ignored) {
                // Shouldn't happen with literal string keys
            }
        }

        if (messagesJson.length() == 0) {
            callback.onError("There are no messages to send.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("messages", messagesJson);
        } catch (JSONException ignored) {}

        final String token = appToken;

        // Detailed debug logging
        try {
            android.util.Log.d(TAG, "Sending chat request to: " + chatEndpointUrl + " payload: " + body.toString());
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to log request payload", e);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                chatEndpointUrl,
                body,
                response -> {
                    // Success path
                    try {
                        String content = response.optString("content", "").trim();
                        android.util.Log.d(TAG, "Received success response: " + response.toString());
                        if (content.isEmpty()) {
                            callback.onError("The assistant returned an empty response.");
                        } else {
                            callback.onSuccess(content);
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error parsing success response", e);
                        callback.onError("The assistant returned an empty response.");
                    }
                },
                error -> {
                    // Error path — decode the body if the server sent a JSON error
                    String errorMessage = "Could not connect to the Arriva assistant.";
                    try {
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            android.util.Log.e(TAG, "Server returned status: " + statusCode);
                            if (statusCode == 429) {
                                errorMessage = "Too many requests. Please wait a moment and try again.";
                            } else if (statusCode == 403) {
                                errorMessage = "Authentication error. Please reinstall the app.";
                            } else if (statusCode >= 500) {
                                errorMessage = "The Arriva assistant is temporarily unavailable.";
                            }
                            try {
                                String body2 = new String(error.networkResponse.data, "UTF-8");
                                android.util.Log.e(TAG, "Server error body: " + body2);
                                JSONObject errJson = new JSONObject(body2);
                                String serverMsg = errJson.optString("error", "");
                                if (!serverMsg.isEmpty()) {
                                    errorMessage = serverMsg;
                                }
                            } catch (Exception ignored2) {
                                android.util.Log.w(TAG, "Failed to parse server error body", ignored2);
                            }
                        } else if (error.getMessage() != null
                                && error.getMessage().contains("timed out")) {
                            errorMessage = "Request timed out. Please try again.";
                        }
                        android.util.Log.e(TAG, "Chat request failed: " + error.toString(), error);
                    } catch (Exception logEx) {
                        android.util.Log.e(TAG, "Unexpected error handling chat failure", logEx);
                    }
                    callback.onError(errorMessage);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                if (token != null && !token.isEmpty()) {
                    headers.put("X-App-Token", token);
                }
                return headers;
            }
        };

        request.setTag(REQUEST_TAG);
        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                0, // no automatic retries — chat is interactive
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }

    @Override
    public void cancelRequests() {
        requestQueue.cancelAll(REQUEST_TAG);
    }
}
