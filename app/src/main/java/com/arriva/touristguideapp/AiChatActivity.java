package com.arriva.touristguideapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AiChatActivity extends BaseActivity {

    private RecyclerView recyclerViewChat;
    private ChatAdapter chatAdapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private LinearProgressIndicator progressIndicator;
    private View emptyStateView;
    private View suggestedPromptsScroll;
    private final List<ChatMessage> messages = new ArrayList<>();
    private GenerativeModelFutures model;
    private boolean isThinking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PerformanceTracker.startTimer("AI_CHAT_INIT");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        initViews();
        setupRecyclerView();
        setupListeners();
        initGemini();

        PerformanceTracker.endTimer("AI_CHAT_INIT");
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressIndicator = findViewById(R.id.progressIndicator);
        emptyStateView = findViewById(R.id.emptyStateView);
        suggestedPromptsScroll = findViewById(R.id.suggestedPromptsScroll);

        updateEmptyState();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage(etMessage.getText().toString().trim()));
        
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setEnabled(!s.toString().trim().isEmpty() && !isThinking);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.chipPlanTrip).setOnClickListener(v -> onSuggestedPromptClick(((Chip)v).getText().toString()));
        findViewById(R.id.chipFood).setOnClickListener(v -> onSuggestedPromptClick(((Chip)v).getText().toString()));
        findViewById(R.id.chipAttractions).setOnClickListener(v -> onSuggestedPromptClick(((Chip)v).getText().toString()));
        findViewById(R.id.chipBudget).setOnClickListener(v -> onSuggestedPromptClick(((Chip)v).getText().toString()));
    }

    private void onSuggestedPromptClick(String prompt) {
        sendMessage(prompt);
    }

    private void initGemini() {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey.isEmpty() || apiKey.equals("YOUR_ACTUAL_GEMINI_KEY")) {
            apiKey = getString(R.string.gemini_api_key);
        }

        if (apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
            Toast.makeText(this, R.string.ai_config_missing, Toast.LENGTH_LONG).show();
            return;
        }

        Content systemInstruction = new Content.Builder()
                .addText(getString(R.string.ai_system_instruction))
                .build();

        try {
            GenerativeModel gm = new GenerativeModel(
                    "gemini-2.5-flash",
                    apiKey,
                    null, null, new RequestOptions(), null, null,
                    systemInstruction
            );
            model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            android.util.Log.e("AiChatActivity", "Failed to initialize Gemini", e);
        }
    }

    private void sendMessage(String text) {
        if (text.isEmpty() || isThinking) return;

        if (model == null) {
            Toast.makeText(this, R.string.ai_assistant_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");
        addMessage(new ChatMessage(text, true));
        setThinkingState(true);

        Content content = new Content.Builder().addText(text).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    setThinkingState(false);
                    String resultText = result.getText();
                    if (resultText == null || resultText.isEmpty()) {
                        resultText = getString(R.string.ai_generic_error);
                    }
                    addMessage(new ChatMessage(resultText, false));
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    setThinkingState(false);
                    String error = getString(R.string.ai_connection_error);
                    if (t.getMessage() != null && t.getMessage().contains("quota")) {
                        error = getString(R.string.ai_quota_error);
                    }
                    addMessage(new ChatMessage(error, false));
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setThinkingState(boolean thinking) {
        isThinking = thinking;
        progressIndicator.setVisibility(thinking ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!thinking && !etMessage.getText().toString().trim().isEmpty());
        suggestedPromptsScroll.setVisibility(thinking ? View.GONE : View.VISIBLE);
        
        if (thinking) {
            addMessage(new ChatMessage(null, false, true));
        } else {
            removeTypingIndicator();
        }
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        recyclerViewChat.scrollToPosition(messages.size() - 1);
        updateEmptyState();
    }

    private void removeTypingIndicator() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).isTyping) {
                messages.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = messages.isEmpty();
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerViewChat.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    static class ChatMessage {
        String text;
        boolean isUser;
        boolean isTyping;

        ChatMessage(String text, boolean isUser) {
            this(text, isUser, false);
        }

        ChatMessage(String text, boolean isUser, boolean isTyping) {
            this.text = text;
            this.isUser = isUser;
            this.isTyping = isTyping;
        }
    }

    static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_USER = 1;
        private static final int VIEW_TYPE_AI = 2;
        private static final int VIEW_TYPE_TYPING = 3;

        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage msg = messages.get(position);
            if (msg.isTyping) return VIEW_TYPE_TYPING;
            return msg.isUser ? VIEW_TYPE_USER : VIEW_TYPE_AI;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_USER) {
                return new UserViewHolder(inflater.inflate(R.layout.item_chat_message_user, parent, false));
            } else if (viewType == VIEW_TYPE_TYPING) {
                return new TypingViewHolder(inflater.inflate(R.layout.item_chat_typing, parent, false));
            } else {
                return new AiViewHolder(inflater.inflate(R.layout.item_chat_message_ai, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            
            // Premium Entrance Animation
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(20f);
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(position % 5 * 50L)
                .start();

            if (holder instanceof UserViewHolder) {
                ((UserViewHolder) holder).tvMessage.setText(message.text);
            } else if (holder instanceof AiViewHolder) {
                ((AiViewHolder) holder).tvMessage.setText(message.text);
            } else if (holder instanceof TypingViewHolder) {
                View card = holder.itemView.findViewById(R.id.cardTyping);
                if (card != null) {
                    card.startAnimation(android.view.animation.AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.pulse));
                }
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;
            UserViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
            }
        }

        static class AiViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;
            AiViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
            }
        }

        static class TypingViewHolder extends RecyclerView.ViewHolder {
            TypingViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}