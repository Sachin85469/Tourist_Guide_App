package com.arriva.touristguideapp.data.chat;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnthropicChatClient implements ChatClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";
    private static final String REQUEST_TAG = "anthropic_chat";

    private final RequestQueue requestQueue;
    private final String apiKey;
    private final String systemInstruction;

    public AnthropicChatClient(@NonNull Context context,
                               @NonNull String apiKey,
                               @NonNull String systemInstruction) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.apiKey = apiKey;
        this.systemInstruction = systemInstruction;
    }

    @Override
    public void sendConversation(@NonNull List<ChatMessage> conversation,
                                 @NonNull Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("max_tokens", 1200);
            body.put("system", systemInstruction);

            JSONArray apiMessages = new JSONArray();
            for (ChatMessage message : conversation) {
                if (message.isTyping() || message.isError()) {
                    continue;
                }

                JSONObject apiMessage = new JSONObject();
                apiMessage.put("role", message.getRole());
                apiMessage.put("content", message.getContent());
                apiMessages.put(apiMessage);
            }
            body.put("messages", apiMessages);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    body,
                    response -> parseResponse(response, callback),
                    error -> {
                        String message = "Could not connect to the assistant.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String responseBody = new String(
                                    error.networkResponse.data,
                                    StandardCharsets.UTF_8
                            );
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                JSONObject errorObject = errorJson.optJSONObject("error");
                                if (errorObject != null) {
                                    message = errorObject.optString("message", message);
                                }
                            } catch (JSONException ignored) {
                                // Keep the user-friendly fallback message.
                            }
                        }
                        callback.onError(message);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-api-key", apiKey);
                    headers.put("anthropic-version", "2023-06-01");
                    headers.put("content-type", "application/json");
                    return headers;
                }
            };

            request.setTag(REQUEST_TAG);
            requestQueue.add(request);
        } catch (JSONException e) {
            callback.onError("Could not create the assistant request.");
        }
    }

    @Override
    public void cancelRequests() {
        requestQueue.cancelAll(REQUEST_TAG);
    }

    private void parseResponse(@NonNull JSONObject response, @NonNull Callback callback) {
        JSONArray content = response.optJSONArray("content");
        if (content == null) {
            callback.onError("The assistant returned an empty response.");
            return;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.optJSONObject(i);
            if (block != null && "text".equals(block.optString("type"))) {
                String text = block.optString("text");
                if (!text.isEmpty()) {
                    if (result.length() > 0) {
                        result.append('\n');
                    }
                    result.append(text);
                }
            }
        }

        if (result.length() == 0) {
            callback.onError("The assistant returned an empty response.");
        } else {
            callback.onSuccess(result.toString());
        }
    }
}
