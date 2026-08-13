package com.arriva.touristguideapp.data.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatClient that connects to the NVIDIA NIM OpenAI-compatible API.
 */
public class NvidiaChatClient implements ChatClient {

    private static final String TAG = "NvidiaChatClient";
    private static final String REQUEST_TAG = "nvidia_chat";
    private static final String ENDPOINT_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final int TIMEOUT_MS = 30_000;

    private final RequestQueue requestQueue;
    private final String apiKey;
    private final String model;
    private final String systemInstruction;

    public NvidiaChatClient(@NonNull Context context,
                            @NonNull String apiKey,
                            @NonNull String model,
                            @NonNull String systemInstruction) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.apiKey = apiKey;
        this.model = model;
        this.systemInstruction = systemInstruction;
    }

    @Override
    public void sendConversation(@NonNull List<ChatMessage> conversation,
                                 @NonNull Callback callback) {
        JSONArray messagesJson = new JSONArray();
        
        // Add system instruction first
        try {
            if (systemInstruction != null && !systemInstruction.isEmpty()) {
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemInstruction);
                messagesJson.put(systemMsg);
            }
        } catch (JSONException ignored) {}

        for (ChatMessage msg : conversation) {
            if (msg.isTyping() || msg.isError()) continue;
            try {
                JSONObject m = new JSONObject();
                m.put("role", msg.isUser() ? "user" : "assistant");
                m.put("content", msg.getContent());
                messagesJson.put(m);
            } catch (JSONException ignored) {
            }
        }

        if (messagesJson.length() == 0) {
            callback.onError("There are no messages to send.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("messages", messagesJson);
            body.put("max_tokens", 1024);
        } catch (JSONException ignored) {}

        final String token = apiKey;

        try {
            android.util.Log.d(TAG, "Sending chat request to NVIDIA. Model: " + model);
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to log request payload", e);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ENDPOINT_URL,
                body,
                response -> {
                    try {
                        JSONArray choices = response.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject firstChoice = choices.getJSONObject(0);
                            JSONObject messageObj = firstChoice.optJSONObject("message");
                            if (messageObj != null) {
                                String content = messageObj.optString("content", "").trim();
                                if (content.isEmpty()) {
                                    callback.onError("The assistant returned an empty response.");
                                } else {
                                    callback.onSuccess(content);
                                }
                                return;
                            }
                        }
                        callback.onError("Failed to parse the assistant response.");
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error parsing success response", e);
                        callback.onError("The assistant returned a malformed response.");
                    }
                },
                error -> {
                    String errorMessage = "Could not connect to the NVIDIA assistant.";
                    try {
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            android.util.Log.e(TAG, "Server returned status: " + statusCode);
                            if (statusCode == 429) {
                                errorMessage = "NVIDIA rate limit reached. Please wait a moment and try again.";
                            } else if (statusCode == 401) {
                                errorMessage = "NVIDIA authentication failed. Please check your API key.";
                            } else if (statusCode >= 500) {
                                errorMessage = "The NVIDIA assistant is temporarily unavailable.";
                            }
                        } else if (error.getMessage() != null && error.getMessage().contains("timed out")) {
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
                    headers.put("Authorization", "Bearer " + token);
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
        if (requestQueue != null) {
            requestQueue.cancelAll(REQUEST_TAG);
        }
    }
}
