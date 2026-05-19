package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private ChatAdapter chatAdapter;
    private EditText etMessage;
    private View btnSend;
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

        chatAdapter = new ChatAdapter(messages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);

        initGemini();

        btnSend.setOnClickListener(v -> sendMessage());

        // Initial greeting
        addMessage(new ChatMessage(getString(R.string.ai_initial_message), false));
    }

    private void initGemini() {
        String apiKey = getString(R.string.gemini_api_key);
        if (apiKey.equals("YOUR_GEMINI_API_KEY")) {
            Toast.makeText(this, "Please set your Gemini API key in strings.xml", Toast.LENGTH_LONG).show();
        }

        // Define strict system instructions to limit the bot to tourism only
        Content systemInstruction = new Content.Builder()
                .addText("You are a specialized Tourism AI Assistant. " +
                        "Your ONLY purpose is to answer queries related to tourism, travel, attractions, and local guide information. " +
                        "Strictly refuse to answer ANY questions that are not related to tourism. " +
                        "Do not perform basic math, solve equations, or answer general knowledge questions outside of travel. " +
                        "If a user asks a non-tourism question, politely respond: 'I am sorry, but I can only assist with tourism-related queries.'")
                .build();

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                apiKey,
                null, // generationConfig (3rd)
                null, // safetySettings (4th)
                new RequestOptions(), // requestOptions (5th)
                null, // tools (6th)
                null, // toolConfig (7th)
                systemInstruction // systemInstruction (8th)
        );
        model = GenerativeModelFutures.from(gm);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        etMessage.setText("");
        addMessage(new ChatMessage(text, true));

        Content content = new Content.Builder()
                .addText(text)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                runOnUiThread(() -> addMessage(new ChatMessage(resultText, false)));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                runOnUiThread(() -> {
                    addMessage(new ChatMessage("Error: " + t.getMessage(), false));
                    Toast.makeText(AiChatActivity.this, "Failed to get response", Toast.LENGTH_SHORT).show();
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