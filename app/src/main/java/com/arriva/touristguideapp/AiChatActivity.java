package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

public class AiChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private ChatAdapter chatAdapter;
    private EditText etMessage;
    private View btnSend;
    private LinearProgressIndicator progressIndicator;
    private List<ChatMessage> messages = new ArrayList<>();
    private GenerativeModelFutures model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressIndicator = findViewById(R.id.progressIndicator);

        chatAdapter = new ChatAdapter(messages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);

        initGemini();

        btnSend.setOnClickListener(v -> sendMessage());

        // Initial greeting
        addMessage(new ChatMessage(getString(R.string.ai_initial_message), false));
    }

    private void initGemini() {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        android.util.Log.d("GEMINI_DEBUG", "Key exists: " + (!apiKey.isEmpty() && !apiKey.equals("YOUR_ACTUAL_GEMINI_KEY")));

        if (apiKey.isEmpty() || apiKey.equals("YOUR_ACTUAL_GEMINI_KEY")) {
            android.util.Log.e("AiChatActivity", "Gemini API key is not configured correctly in local.properties.");
            Toast.makeText(this, "Gemini API key is not configured.", Toast.LENGTH_LONG).show();
        }

        // Define strict system instructions to limit the bot to tourism only
        Content systemInstruction = new Content.Builder()
                .addText("You are a specialized Tourism AI Assistant. " +
                        "Your ONLY purpose is to answer queries related to tourism, travel, attractions, and local guide information. " +
                        "Strictly refuse to answer ANY questions that are not related to tourism. " +
                        "Do not perform basic math, solve equations, or answer general knowledge questions outside of travel. " +
                        "If a user asks a non-tourism question, politely respond: 'I am sorry, but I can only assist with tourism-related queries.'")
                .build();

        try {
            // Using 'gemini-1.5-flash-latest' as requested
            String modelName = "gemini-1.5-flash-latest";
            android.util.Log.d("AiChatActivity", "Using model: " + modelName);

            GenerativeModel gm = new GenerativeModel(
                    modelName,
                    apiKey,
                    null, // generationConfig
                    null, // safetySettings
                    new RequestOptions(), // requestOptions
                    null, // tools
                    null, // toolConfig
                    systemInstruction // systemInstruction
            );
            model = GenerativeModelFutures.from(gm);
            android.util.Log.d("AiChatActivity", "Gemini model initialized successfully from BuildConfig.");
        } catch (Exception e) {
            android.util.Log.e("AiChatActivity", "Failed to initialize Gemini", e);
            Toast.makeText(this, "AI initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        if (model == null) {
            android.util.Log.e("AiChatActivity", "SendMessage called but model is null");
            Toast.makeText(this, "AI Assistant not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");
        addMessage(new ChatMessage(text, true));
        
        // Show loading state
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        android.util.Log.d("AiChatActivity", "Sending request to Gemini: " + text);

        Content content = new Content.Builder()
                .addText(text)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    android.util.Log.d("AiChatActivity", "Gemini response received successfully.");
                    if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    String resultText = Objects.requireNonNullElse(result.getText(), "I received an empty response. Please try again.");
                    addMessage(new ChatMessage(resultText, false));
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                runOnUiThread(() -> {
                    android.util.Log.e("AiChatActivity", "Gemini request failed", t);
                    if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    
                    String userFriendlyError = "Sorry, I'm having trouble connecting right now.";
                    String errorMessage = t.getMessage() != null ? t.getMessage() : "";
                    
                    if (errorMessage.contains("API key not valid") || errorMessage.contains("API_KEY_INVALID")) {
                        userFriendlyError = "Technical error: Invalid API configuration.";
                    } else if (errorMessage.contains("quota") || errorMessage.contains("429")) {
                        userFriendlyError = "I've reached my daily limit. Please try again later.";
                    } else if (errorMessage.contains("network") || errorMessage.contains("Unable to resolve host") || errorMessage.contains("timeout")) {
                        userFriendlyError = "Please check your internet connection and try again.";
                    }

                    addMessage(new ChatMessage(userFriendlyError, false));
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        recyclerViewChat.scrollToPosition(messages.size() - 1);
    }

    static class ChatMessage {
        String text;
        boolean isUser;

        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.tvMessage.setText(message.text);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardMessage.getLayoutParams();
            if (message.isUser) {
                params.gravity = Gravity.END;
                holder.cardMessage.setCardBackgroundColor(ContextCompat.getColor(AiChatActivity.this, R.color.primary));
                holder.tvMessage.setTextColor(ContextCompat.getColor(AiChatActivity.this, R.color.white));
            } else {
                params.gravity = Gravity.START;
                holder.cardMessage.setCardBackgroundColor(ContextCompat.getColor(AiChatActivity.this, R.color.gray_light));
                holder.tvMessage.setTextColor(ContextCompat.getColor(AiChatActivity.this, R.color.black));
            }
            holder.cardMessage.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;
            MaterialCardView cardMessage;

            ViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                cardMessage = itemView.findViewById(R.id.cardMessage);
            }
        }
    }
}