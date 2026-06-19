package com.arriva.touristguideapp;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.data.chat.AnthropicChatClient;
import com.arriva.touristguideapp.data.chat.ChatClient;
import com.arriva.touristguideapp.data.chat.ChatMessage;
import com.arriva.touristguideapp.data.chat.ChatRepository;
import com.arriva.touristguideapp.data.chat.GeminiChatClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiChatActivity extends BaseActivity {

    private static final String TAG = "AiChatActivity";

    private RecyclerView recyclerViewChat;
    private ChatAdapter chatAdapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private LinearProgressIndicator progressIndicator;
    private View emptyStateView;
    private View suggestedPromptsScroll;
    private TextView offlineBanner;
    private boolean isOffline;

    // The canonical conversation is persisted and sent to the selected provider.
    private final ArrayList<ChatMessage> conversation = new ArrayList<>();
    // UI-only typing and error rows live here and are never persisted or sent.
    private final ArrayList<ChatMessage> displayMessages = new ArrayList<>();

    private ChatRepository chatRepository;
    /** Primary chat client (proxy → Anthropic on the server). */
    private ChatClient primaryClient;
    /** Fallback chat client (Gemini directly) — used only when the proxy is unreachable. */
    private ChatClient fallbackClient;
    /** Alias for the currently active client (primary or fallback). */
    private ChatClient chatClient;
    private String userId;
    private boolean isThinking;
    private boolean isLoadingHistory;
    private int requestGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PerformanceTracker.startTimer("AI_CHAT_INIT");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        initViews();
        setupRecyclerView();
        setupListeners();
        chatRepository = new ChatRepository();
        initChatClient();
        loadChatHistory();

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
        
        // Create offline banner programmatically
        offlineBanner = new TextView(this);
        offlineBanner.setId(View.generateViewId());
        offlineBanner.setText("⚠️ AI assistant offline — connect to internet to chat");
        offlineBanner.setBackgroundColor(0xFFFFA500);
        offlineBanner.setTextColor(0xFFFFFFFF);
        offlineBanner.setPadding(16, 12, 16, 12);
        offlineBanner.setTextSize(14);
        offlineBanner.setVisibility(View.GONE);
        
        // Add banner to the ConstraintLayout
        androidx.constraintlayout.widget.ConstraintLayout constraintLayout = 
            (androidx.constraintlayout.widget.ConstraintLayout) findViewById(R.id.recyclerViewChat).getParent();
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = 
            new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
        params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        offlineBanner.setLayoutParams(params);
        constraintLayout.addView(offlineBanner, 0); // Add at index 0 to be on top
        
        // Adjust recyclerViewChat constraint to be below banner
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams recyclerViewParams = 
            (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) recyclerViewChat.getLayoutParams();
        recyclerViewParams.topToBottom = offlineBanner.getId();
        recyclerViewChat.setLayoutParams(recyclerViewParams);
        
        updateEmptyState();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(displayMessages, this::retryLastRequest);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage(etMessage.getText().toString().trim()));

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateComposerState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.chipPlanTrip).setOnClickListener(
                v -> onSuggestedPromptClick(((Chip) v).getText().toString())
        );
        findViewById(R.id.chipFood).setOnClickListener(
                v -> onSuggestedPromptClick(((Chip) v).getText().toString())
        );
        findViewById(R.id.chipAttractions).setOnClickListener(
                v -> onSuggestedPromptClick(((Chip) v).getText().toString())
        );
        findViewById(R.id.chipBudget).setOnClickListener(
                v -> onSuggestedPromptClick(((Chip) v).getText().toString())
        );
    }

    private void initChatClient() {
        // ─── 1. Build the proxy client (always primary if a backend URL is configured) ───
        String backendUrl = BuildConfig.ARRIVA_BACKEND_URL.trim();
        String appToken   = BuildConfig.ARRIVA_APP_TOKEN.trim();
        if (isConfiguredUrl(backendUrl)) {
            String chatUrl = backendUrl.endsWith("/")
                    ? backendUrl + "api/chat"
                    : backendUrl + "/api/chat";
            android.util.Log.d(TAG, "Configuring primary proxy client -> " + chatUrl);
            primaryClient = new AnthropicChatClient(this, chatUrl, appToken);
            chatClient    = primaryClient;
        } else {
            android.util.Log.d(TAG, "No Arriva backend configured.");
        }

        // ─── 2. Build the Gemini fallback ───
        String geminiKey = BuildConfig.GEMINI_API_KEY.trim();
        if (!isConfiguredKey(geminiKey)) {
            geminiKey = getString(R.string.gemini_api_key).trim();
            android.util.Log.d(TAG, "Using gemini key from strings.xml (length=" + geminiKey.length() + ")");
        } else {
            android.util.Log.d(TAG, "Using gemini key from BuildConfig (length=" + geminiKey.length() + ")");
        }
        if (isConfiguredKey(geminiKey)) {
            try {
                fallbackClient = new GeminiChatClient(
                        this,
                        geminiKey,
                        getString(R.string.ai_system_instruction)
                );
                android.util.Log.d(TAG, "Gemini fallback initialized.");
                // If we have no primary, promote Gemini to primary.
                if (chatClient == null) {
                    chatClient = fallbackClient;
                    android.util.Log.d(TAG, "Promoted Gemini to primary chat client.");
                }
            } catch (RuntimeException e) {
                android.util.Log.e(TAG, "Failed to initialize Gemini fallback", e);
            }
        } else {
            android.util.Log.w(TAG, "No Gemini API key configured.");
        }

        // ─── 3. No client at all — show offline state ───
        if (chatClient == null) {
            isOffline = true;
            showOfflineState();
        }
    }

    private void showOfflineState() {
        // Add system message with helpful info
        String offlineMessage = "I'm currently offline or not configured. Here are some things I can help you with once connected:\n" +
                "• Plan a trip itinerary\n" +
                "• Find the best food spots in Pune\n" +
                "• Get tips for visiting temples and forts\n" +
                "• Learn about budget and crowd levels at any place";
        
        ChatMessage systemMessage = new ChatMessage(
                ChatMessage.ROLE_ASSISTANT,
                offlineMessage,
                System.currentTimeMillis()
        );
        addDisplayMessage(systemMessage);
        
        // Show offline banner
        offlineBanner.setVisibility(View.VISIBLE);
        
        // Disable send button and typing field
        btnSend.setEnabled(false);
        etMessage.setEnabled(false);
        etMessage.setHint("AI assistant offline");
    }

    private void hideOfflineState() {
        offlineBanner.setVisibility(View.GONE);
        btnSend.setEnabled(true);
        etMessage.setEnabled(true);
        etMessage.setHint(getString(R.string.ai_input_hint));
    }

    private boolean isConfiguredKey(String apiKey) {
        return !apiKey.isEmpty()
                && !apiKey.equalsIgnoreCase("YOUR_KEY")
                && !apiKey.startsWith("YOUR_");
    }

    private boolean isConfiguredUrl(String url) {
        return url != null
                && !url.isEmpty()
                && !url.equalsIgnoreCase("YOUR_BACKEND_URL")
                && !url.startsWith("YOUR_")
                && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void loadChatHistory() {
        Log.d(TAG, "CHAT_LOAD_START");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "CHAT_LOAD_FAILED: Firebase user is null");
            Toast.makeText(this, R.string.ai_login_required, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        userId = user.getUid();
        Log.d(TAG, "USER_ID: " + userId);

        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "CHAT_LOAD_FAILED: USER_ID is null or empty");
            Toast.makeText(this, R.string.ai_login_required, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        isLoadingHistory = true;
        updateLoadingState();

        chatRepository.loadMessages(userId)
                .addOnSuccessListener(messages -> {
                    conversation.clear();
                    displayMessages.clear();
                    conversation.addAll(messages);
                    displayMessages.addAll(messages);

                    isLoadingHistory = false;
                    chatAdapter.notifyDataSetChanged();
                    updateLoadingState();
                    updateEmptyState();
                    scrollToLatestMessage();
                    Log.d(TAG, "CHAT_LOAD_SUCCESS: " + messages.size() + " messages");
                })
                .addOnFailureListener(exception -> {
                    isLoadingHistory = false;
                    updateLoadingState();
                    updateEmptyState();
                    Log.e(
                            TAG,
                            "CHAT_LOAD_FAILED: " + exception.getMessage(),
                            exception
                    );
                    Toast.makeText(
                            this,
                            getString(R.string.ai_history_load_failed),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void onSuggestedPromptClick(String prompt) {
        sendMessage(prompt);
    }

    private void sendMessage(String text) {
        if (text.isEmpty() || isThinking || isLoadingHistory) {
            return;
        }
        if (chatClient == null) {
            Toast.makeText(this, R.string.ai_assistant_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");
        removeErrorMessages();

        ChatMessage userMessage = new ChatMessage(
                ChatMessage.ROLE_USER,
                text,
                System.currentTimeMillis()
        );
        addConversationMessage(userMessage, true);
        requestAssistantResponse();
    }

    private void requestAssistantResponse() {
        if (chatClient == null || conversation.isEmpty()) {
            return;
        }

        removeErrorMessages();
        setThinkingState(true);
        int generation = ++requestGeneration;
        ArrayList<ChatMessage> historySnapshot = new ArrayList<>(conversation);

        // Determines whether this call is already using the fallback.
        final boolean usingFallback = (chatClient == fallbackClient);

        chatClient.sendConversation(historySnapshot, new ChatClient.Callback() {
            @Override
            public void onSuccess(@NonNull String response) {
                if (generation != requestGeneration || isFinishing() || isDestroyed()) {
                    return;
                }

                setThinkingState(false);
                ChatMessage assistantMessage = new ChatMessage(
                        ChatMessage.ROLE_ASSISTANT,
                        response,
                        System.currentTimeMillis()
                );
                addConversationMessage(assistantMessage, true);
            }

            @Override
            public void onError(@NonNull String message) {
                if (generation != requestGeneration || isFinishing() || isDestroyed()) {
                    return;
                }

                // ─── Fallback: if primary (proxy) failed and we have a Gemini client, try it ───
                if (!usingFallback && fallbackClient != null) {
                    android.util.Log.w(TAG, "Proxy failed, falling back to Gemini: " + message);
                    chatClient = fallbackClient;
                    // Retry silently using Gemini — still within the same generation/spinner
                    fallbackClient.sendConversation(historySnapshot, new ChatClient.Callback() {
                        @Override
                        public void onSuccess(@NonNull String response) {
                            if (generation != requestGeneration || isFinishing() || isDestroyed()) {
                                return;
                            }
                            setThinkingState(false);
                            ChatMessage assistantMessage = new ChatMessage(
                                    ChatMessage.ROLE_ASSISTANT,
                                    response,
                                    System.currentTimeMillis()
                            );
                            addConversationMessage(assistantMessage, true);
                        }

                        @Override
                        public void onError(@NonNull String fallbackMessage) {
                            if (generation != requestGeneration || isFinishing() || isDestroyed()) {
                                return;
                            }
                            setThinkingState(false);
                            addDisplayMessage(ChatMessage.error(
                                    "Could not reach the assistant. " + fallbackMessage));
                        }
                    });
                    return;
                }

                // Both primary and fallback failed (or we were already on fallback).
                setThinkingState(false);
                addDisplayMessage(ChatMessage.error(message));
            }
        });
    }

    private void retryLastRequest() {
        if (!isThinking && !isLoadingHistory && !conversation.isEmpty()) {
            requestAssistantResponse();
        }
    }

    private void addConversationMessage(ChatMessage message, boolean persist) {
        conversation.add(message);
        addDisplayMessage(message);

        if (persist && userId != null) {
            chatRepository.saveMessage(userId, message)
                    .addOnFailureListener(error -> Toast.makeText(
                            this,
                            R.string.ai_message_save_failed,
                            Toast.LENGTH_SHORT
                    ).show());
        }
    }

    private void addDisplayMessage(ChatMessage message) {
        displayMessages.add(message);
        chatAdapter.notifyItemInserted(displayMessages.size() - 1);
        updateEmptyState();
        scrollToLatestMessage();
    }

    private void setThinkingState(boolean thinking) {
        isThinking = thinking;
        if (thinking) {
            addDisplayMessage(ChatMessage.typingIndicator());
        } else {
            removeTypingIndicator();
        }
        updateLoadingState();
    }

    private void removeTypingIndicator() {
        for (int i = displayMessages.size() - 1; i >= 0; i--) {
            if (displayMessages.get(i).isTyping()) {
                displayMessages.remove(i);
                chatAdapter.notifyItemRemoved(i);
                return;
            }
        }
    }

    private void removeErrorMessages() {
        for (int i = displayMessages.size() - 1; i >= 0; i--) {
            if (displayMessages.get(i).isError()) {
                displayMessages.remove(i);
                chatAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void updateLoadingState() {
        progressIndicator.setVisibility(
                isThinking || isLoadingHistory ? View.VISIBLE : View.GONE
        );
        suggestedPromptsScroll.setVisibility(
                isThinking || isLoadingHistory ? View.GONE : View.VISIBLE
        );
        updateComposerState();
    }

    private void updateComposerState() {
        boolean hasText = !etMessage.getText().toString().trim().isEmpty();
        btnSend.setEnabled(hasText && !isThinking && !isLoadingHistory);
        etMessage.setEnabled(!isLoadingHistory);
    }

    private void updateEmptyState() {
        boolean isEmpty = displayMessages.isEmpty();
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerViewChat.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void scrollToLatestMessage() {
        if (!displayMessages.isEmpty()) {
            recyclerViewChat.scrollToPosition(displayMessages.size() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ai_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_clear_chat) {
            confirmClearChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmClearChat() {
        if (userId == null || conversation.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.ai_clear_chat)
                .setMessage(R.string.ai_clear_chat_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.clear, (dialog, which) -> clearChat())
                .show();
    }

    private void clearChat() {
        requestGeneration++;
        if (chatClient != null) {
            chatClient.cancelRequests();
        }
        isThinking = false;
        removeTypingIndicator();
        removeErrorMessages();
        isLoadingHistory = true;
        updateLoadingState();

        chatRepository.clearMessages(userId)
                .addOnSuccessListener(unused -> {
                    conversation.clear();
                    displayMessages.clear();
                    chatAdapter.notifyDataSetChanged();
                    isLoadingHistory = false;
                    updateLoadingState();
                    updateEmptyState();
                    Toast.makeText(this, R.string.ai_chat_cleared, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> {
                    isLoadingHistory = false;
                    updateLoadingState();
                    Toast.makeText(this, R.string.ai_clear_chat_failed, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Re-check network if we were previously offline
        if (isOffline && isNetworkAvailable()) {
            isOffline = false;
            hideOfflineState();
            initChatClient();
        }
    }

    @Override
    protected void onDestroy() {
        requestGeneration++;
        if (primaryClient != null) primaryClient.cancelRequests();
        if (fallbackClient != null) fallbackClient.cancelRequests();
        super.onDestroy();
    }

    static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        interface RetryListener {
            void onRetry();
        }

        private static final int VIEW_TYPE_USER = 1;
        private static final int VIEW_TYPE_AI = 2;
        private static final int VIEW_TYPE_TYPING = 3;

        private final List<ChatMessage> messages;
        private final RetryListener retryListener;
        private final SimpleDateFormat timeFormat =
                new SimpleDateFormat("HH:mm", Locale.getDefault());

        ChatAdapter(List<ChatMessage> messages, RetryListener retryListener) {
            this.messages = messages;
            this.retryListener = retryListener;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage message = messages.get(position);
            if (message.isTyping()) {
                return VIEW_TYPE_TYPING;
            }
            return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_USER) {
                return new UserViewHolder(
                        inflater.inflate(R.layout.item_chat_message_user, parent, false)
                );
            }
            if (viewType == VIEW_TYPE_TYPING) {
                return new TypingViewHolder(
                        inflater.inflate(R.layout.item_chat_typing, parent, false)
                );
            }
            return new AiViewHolder(
                    inflater.inflate(R.layout.item_chat_message_ai, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            if (holder instanceof UserViewHolder) {
                ((UserViewHolder) holder).bind(message, formatTime(message));
            } else if (holder instanceof AiViewHolder) {
                ((AiViewHolder) holder).bind(
                        message,
                        formatTime(message),
                        retryListener
                );
            } else if (holder instanceof TypingViewHolder) {
                ((TypingViewHolder) holder).startAnimation();
            }
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            if (holder instanceof TypingViewHolder) {
                ((TypingViewHolder) holder).stopAnimation();
            }
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        private String formatTime(ChatMessage message) {
            return timeFormat.format(new Date(message.getTimestamp()));
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvMessage;
            private final TextView tvTime;

            UserViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
            }

            void bind(ChatMessage message, String time) {
                tvMessage.setText(message.getContent());
                tvTime.setText(time);
            }
        }

        static class AiViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvMessage;
            private final TextView tvTime;
            private final MaterialButton btnRetry;

            AiViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
                btnRetry = itemView.findViewById(R.id.btnRetryChat);
            }

            void bind(ChatMessage message, String time, RetryListener retryListener) {
                tvMessage.setText(message.getContent());
                tvTime.setText(time);
                btnRetry.setVisibility(message.isError() ? View.VISIBLE : View.GONE);
                btnRetry.setOnClickListener(
                        message.isError() ? v -> retryListener.onRetry() : null
                );
            }
        }

        static class TypingViewHolder extends RecyclerView.ViewHolder {
            private final View[] dots;
            private final List<ObjectAnimator> animators = new ArrayList<>();

            TypingViewHolder(View itemView) {
                super(itemView);
                dots = new View[]{
                        itemView.findViewById(R.id.dotOne),
                        itemView.findViewById(R.id.dotTwo),
                        itemView.findViewById(R.id.dotThree)
                };
            }

            void startAnimation() {
                stopAnimation();
                for (int i = 0; i < dots.length; i++) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(
                            dots[i],
                            View.ALPHA,
                            0.25f,
                            1f
                    );
                    animator.setDuration(500);
                    animator.setStartDelay(i * 150L);
                    animator.setRepeatCount(ObjectAnimator.INFINITE);
                    animator.setRepeatMode(ObjectAnimator.REVERSE);
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    animator.start();
                    animators.add(animator);
                }
            }

            void stopAnimation() {
                for (ObjectAnimator animator : animators) {
                    animator.cancel();
                }
                animators.clear();
            }
        }
    }
}
