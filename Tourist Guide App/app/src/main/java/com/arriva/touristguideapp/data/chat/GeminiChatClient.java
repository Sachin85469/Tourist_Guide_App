package com.arriva.touristguideapp.data.chat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class GeminiChatClient implements ChatClient {

    private static final String MODEL = "gemini-2.5-flash";

    private final GenerativeModelFutures model;
    private final Executor callbackExecutor;

    @Nullable
    private ListenableFuture<GenerateContentResponse> activeRequest;

    public GeminiChatClient(@NonNull Context context,
                            @NonNull String apiKey,
                            @NonNull String systemInstruction) {
        Content instruction = new Content.Builder()
                .addText(systemInstruction)
                .build();
        GenerativeModel generativeModel = new GenerativeModel(
                MODEL,
                apiKey,
                null,
                null,
                new RequestOptions(),
                null,
                null,
                instruction
        );
        model = GenerativeModelFutures.from(generativeModel);
        callbackExecutor = ContextCompat.getMainExecutor(context.getApplicationContext());
    }

    private static final String TAG = "GeminiChatClient";

    @Override
    public void sendConversation(@NonNull List<ChatMessage> conversation,
                                 @NonNull Callback callback) {
        List<Content> history = new ArrayList<>();
        for (ChatMessage message : conversation) {
            if (message.isTyping() || message.isError()) {
                continue;
            }

            Content.Builder content = new Content.Builder();
            content.setRole(message.isUser() ? "user" : "model");
            content.addText(message.getContent());
            history.add(content.build());
        }

        if (history.isEmpty()) {
            callback.onError("There are no messages to send.");
            return;
        }

        // Log request details (avoid accessing Content internals to remain compatible)
        android.util.Log.d(TAG, "Sending to Gemini model=" + MODEL + " messagesCount=" + history.size());

        activeRequest = model.generateContent(history.toArray(new Content[0]));
        Futures.addCallback(activeRequest, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                activeRequest = null;
                String response = result != null ? result.getText() : null;
                android.util.Log.d(TAG, "Gemini success response=" + response);
                if (response == null || response.trim().isEmpty()) {
                    callback.onError("The assistant returned an empty response.");
                } else {
                    callback.onSuccess(response);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable error) {
                activeRequest = null;
                android.util.Log.e(TAG, "Gemini request failed", error);
                String details = error.getMessage();
                if (details != null
                        && details.toLowerCase(Locale.US).contains("quota")) {
                    callback.onError("The Gemini quota has been reached. Please try again later.");
                } else if (details != null && details.toLowerCase(Locale.US).contains("401")) {
                    callback.onError("Gemini authentication failed (invalid API key).");
                } else {
                    callback.onError("Could not connect to the assistant.");
                }
            }
        }, callbackExecutor);
    }

    @Override
    public void cancelRequests() {
        if (activeRequest != null) {
            activeRequest.cancel(true);
            activeRequest = null;
        }
    }
}
