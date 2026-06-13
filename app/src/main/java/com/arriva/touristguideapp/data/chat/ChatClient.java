package com.arriva.touristguideapp.data.chat;

import androidx.annotation.NonNull;

import java.util.List;

public interface ChatClient {

    interface Callback {
        void onSuccess(@NonNull String response);

        void onError(@NonNull String message);
    }

    void sendConversation(@NonNull List<ChatMessage> conversation,
                          @NonNull Callback callback);

    void cancelRequests();
}
