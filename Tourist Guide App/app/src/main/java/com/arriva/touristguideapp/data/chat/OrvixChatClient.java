package com.arriva.touristguideapp.data.chat;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ChatClient} backed by the ORVIX FastAPI server.
 *
 * <p>Endpoint: {@code POST /v1/chat}
 * <br>Request body:  {@code {"message": "user message"}}
 * <br>Required header: {@code Authorization: Bearer <apiKey>}
 * <br>Response body:  {@code {"response": "AI response"}}
 *
 * <p>Networking is performed off the main thread by Volley; callbacks
 * are delivered on the main thread by default.
 */
public class OrvixChatClient implements ChatClient {

    private static final String TAG = "OrvixChatClient";
    private static final String REQUEST_TAG = "orvix_chat";

    /** Timeout: 30 s, no automatic retries (chat is interactive). */
    private static final int TIMEOUT_MS = 30_000;

    private final RequestQueue requestQueue;
    private final String chatEndpointUrl;
    private final String apiKey;

    /**
     * @param context  Application context used to create the Volley queue.
     * @param baseUrl  Base URL of the ORVIX server, e.g.
     *                 {@code http://192.168.1.X:8000}.
     *                 The {@code /v1/chat} path is appended automatically.
     * @param apiKey   Value sent in the {@code Authorization: Bearer} header
     *                 (e.g. {@code "orvix-test-key"}).
     */
    public OrvixChatClient(@NonNull Context context,
                           @NonNull String baseUrl,
                           @NonNull String apiKey) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        // Normalise: strip trailing slash before appending path
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.chatEndpointUrl = base + "/v1/chat";
        this.apiKey = apiKey;
    }

    @Override
    public void sendConversation(@NonNull List<ChatMessage> conversation,
                                 @NonNull Callback callback) {

        // Find the last user message (skip typing/error placeholders).
        String lastUserMessage = null;
        for (int i = conversation.size() - 1; i >= 0; i--) {
            ChatMessage msg = conversation.get(i);
            if (msg.isTyping() || msg.isError()) continue;
            if (msg.isUser()) {
                lastUserMessage = msg.getContent();
                break;
            }
        }

        if (lastUserMessage == null || lastUserMessage.trim().isEmpty()) {
            callback.onError("There are no messages to send.");
            return;
        }

        // Build JSON body: {"message": "..."}
        JSONObject body = new JSONObject();
        try {
            body.put("message", lastUserMessage);
        } catch (JSONException ignored) {
            // Impossible with a literal key.
        }

        android.util.Log.d(TAG, "Sending to ORVIX: " + chatEndpointUrl
                + " payload: " + body.toString());

        final String key = apiKey;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                chatEndpointUrl,
                body,
                response -> {
                    // ── Success path ──
                    try {
                        String content = response.optString("response", "").trim();
                        android.util.Log.d(TAG, "ORVIX response: " + response.toString());
                        if (content.isEmpty()) {
                            callback.onError("The assistant returned an empty response.");
                        } else {
                            callback.onSuccess(content);
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error parsing ORVIX response", e);
                        callback.onError("Failed to parse the assistant's response.");
                    }
                },
                error -> {
                    // ── Error path ──
                    String errorMessage = "Could not connect to the ORVIX assistant.";
                    try {
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            android.util.Log.e(TAG, "ORVIX returned status: " + statusCode);

                            if (statusCode == 401 || statusCode == 403) {
                                errorMessage = "ORVIX authentication failed (invalid API key).";
                            } else if (statusCode == 429) {
                                errorMessage = "Too many requests. Please wait a moment.";
                            } else if (statusCode >= 500) {
                                errorMessage = "The ORVIX server encountered an error.";
                            }

                            // Try to extract detail from the response body
                            try {
                                String responseBody = new String(
                                        error.networkResponse.data, "UTF-8");
                                android.util.Log.e(TAG, "ORVIX error body: " + responseBody);
                                JSONObject errJson = new JSONObject(responseBody);
                                String detail = errJson.optString("detail", "");
                                if (!detail.isEmpty()) {
                                    errorMessage = detail;
                                }
                            } catch (Exception ignored) {
                                android.util.Log.w(TAG,
                                        "Could not parse ORVIX error body", ignored);
                            }
                        } else if (error.getMessage() != null
                                && error.getMessage().contains("timed out")) {
                            errorMessage = "Request timed out. Is the ORVIX server running?";
                        }
                        android.util.Log.e(TAG, "ORVIX request failed: " + error.toString(), error);
                    } catch (Exception logEx) {
                        android.util.Log.e(TAG,
                                "Unexpected error handling ORVIX failure", logEx);
                    }
                    callback.onError(errorMessage);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + key);
                return headers;
            }
        };

        request.setTag(REQUEST_TAG);
        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                0,   // no automatic retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }

    @Override
    public void cancelRequests() {
        requestQueue.cancelAll(REQUEST_TAG);
    }
}
