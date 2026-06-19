package com.arriva.touristguideapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class PlanTripActivity extends BaseActivity {

    private static final int PLAN_PRIMARY = Color.rgb(108, 76, 241);
    private static final int PLAN_TEXT_PRIMARY = Color.rgb(31, 41, 55);
    private static final int WHITE = Color.WHITE;
    private static final long STATE_ANIM_MS = 180L;

    private TextView chipDay1, chipDay2, chipDay3, chipDay4;
    private int selectedDays = 1;

    private View chipVibeHistorical;
    private View chipVibeNature;
    private View chipVibeReligious;
    private View chipVibeFood;
    private View chipVibeCulture;
    private View chipVibeAdventure;
    private View chipVibeScenic;
    private View chipVibeShopping;
    private final List<View> vibeChips = new ArrayList<>();

    private View chipBudgetBudget, chipBudgetMid, chipBudgetPremium;
    private String selectedBudget = "Mid-range";

    private View planTripHeader;
    private AppBarLayout planTripAppBar;
    private View headerTravelRow;
    private View headerContent;
    private View headerGlowLarge;
    private View headerGlowStart;
    private View headerGlowSmall;
    private View planTripSheet;
    private View cardVibes;
    private TextView btnGeneratePlan;
    private ProgressBar progressGeneratePlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_trip);

        configureStatusBar();

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        bindViews();
        setupDurationChips();
        setupVibeChips();
        setupBudgetChips();
        setupCta();
        setupCollapsingHeader();

        selectDurationChip(chipDay1, 1);
        selectBudgetChip(chipBudgetMid, "Mid-range");
        runEntranceAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setGeneratingState(false);
    }

    private void configureStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.rgb(91, 63, 214));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(
                    decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }
    }

    private void bindViews() {
        planTripAppBar = findViewById(R.id.planTripAppBar);
        planTripHeader = findViewById(R.id.planTripHeader);
        headerTravelRow = findViewById(R.id.headerTravelRow);
        headerContent = findViewById(R.id.headerContent);
        headerGlowLarge = findViewById(R.id.headerGlowLarge);
        headerGlowStart = findViewById(R.id.headerGlowStart);
        headerGlowSmall = findViewById(R.id.headerGlowSmall);
        planTripSheet = findViewById(R.id.planTripSheet);

        chipDay1 = findViewById(R.id.chipDay1);
        chipDay2 = findViewById(R.id.chipDay2);
        chipDay3 = findViewById(R.id.chipDay3);
        chipDay4 = findViewById(R.id.chipDay4);

        chipVibeHistorical = findViewById(R.id.chipVibeHistorical);
        chipVibeNature = findViewById(R.id.chipVibeNature);
        chipVibeReligious = findViewById(R.id.chipVibeReligious);
        chipVibeFood = findViewById(R.id.chipVibeFood);
        chipVibeCulture = findViewById(R.id.chipVibeCulture);
        chipVibeAdventure = findViewById(R.id.chipVibeAdventure);
        chipVibeScenic = findViewById(R.id.chipVibeScenic);
        chipVibeShopping = findViewById(R.id.chipVibeShopping);

        chipBudgetBudget = findViewById(R.id.chipBudgetBudget);
        chipBudgetMid = findViewById(R.id.chipBudgetMid);
        chipBudgetPremium = findViewById(R.id.chipBudgetPremium);

        cardVibes = findViewById(R.id.cardVibes);
        btnGeneratePlan = findViewById(R.id.btnGeneratePlan);
        progressGeneratePlan = findViewById(R.id.progressGeneratePlan);
    }

    private void setupCollapsingHeader() {
        if (planTripAppBar == null) return;
        planTripAppBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalRange = appBarLayout.getTotalScrollRange();
            if (totalRange <= 0) return;

            float collapse = Math.min(1f, Math.abs(verticalOffset) / (float) totalRange);
            float heroAlpha = 1f - collapse;
            float decorAlpha = Math.max(0f, 1f - collapse * 1.35f);

            if (headerContent != null) {
                headerContent.setAlpha(heroAlpha);
                headerContent.setTranslationY(-dp(18f) * collapse);
            }
            if (headerGlowLarge != null) headerGlowLarge.setAlpha(0.65f * decorAlpha);
            if (headerGlowStart != null) headerGlowStart.setAlpha(0.38f * decorAlpha);
            if (headerGlowSmall != null) headerGlowSmall.setAlpha(0.30f * decorAlpha);
        });
    }

    private void setupDurationChips() {
        if (chipDay1 != null) chipDay1.setOnClickListener(v -> selectDurationChip(chipDay1, 1));
        if (chipDay2 != null) chipDay2.setOnClickListener(v -> selectDurationChip(chipDay2, 2));
        if (chipDay3 != null) chipDay3.setOnClickListener(v -> selectDurationChip(chipDay3, 3));
        if (chipDay4 != null) chipDay4.setOnClickListener(v -> selectDurationChip(chipDay4, 4));
    }

    private void selectDurationChip(TextView selected, int days) {
        selectedDays = days;
        applySegmentState(chipDay1, selected == chipDay1);
        applySegmentState(chipDay2, selected == chipDay2);
        applySegmentState(chipDay3, selected == chipDay3);
        applySegmentState(chipDay4, selected == chipDay4);
    }

    private void setupVibeChips() {
        registerVibeChip(chipVibeHistorical, "Historical");
        registerVibeChip(chipVibeNature, "Nature");
        registerVibeChip(chipVibeReligious, "Religious");
        registerVibeChip(chipVibeFood, "Food");
        registerVibeChip(chipVibeCulture, "Culture");
        registerVibeChip(chipVibeAdventure, "Adventure");
        registerVibeChip(chipVibeScenic, "Scenic");
        registerVibeChip(chipVibeShopping, "Shopping");
    }

    private void registerVibeChip(View chip, String categoryLabel) {
        if (chip == null) return;
        chip.setTag(categoryLabel);
        chip.setOnClickListener(v -> toggleVibeChip(chip));
    }

    private void toggleVibeChip(View chip) {
        boolean isNowSelected = !vibeChips.contains(chip);
        if (isNowSelected) {
            vibeChips.add(chip);
        } else {
            vibeChips.remove(chip);
        }
        applyChoiceState(chip, isNowSelected, false);
    }

    private ArrayList<String> getSelectedVibes() {
        ArrayList<String> labels = new ArrayList<>();
        for (View chip : vibeChips) {
            Object tag = chip.getTag();
            if (tag instanceof String) {
                labels.add((String) tag);
            }
        }
        return labels;
    }

    private void setupBudgetChips() {
        if (chipBudgetBudget != null) {
            chipBudgetBudget.setOnClickListener(v -> selectBudgetChip(chipBudgetBudget, "Budget"));
        }
        if (chipBudgetMid != null) {
            chipBudgetMid.setOnClickListener(v -> selectBudgetChip(chipBudgetMid, "Mid-range"));
        }
        if (chipBudgetPremium != null) {
            chipBudgetPremium.setOnClickListener(v -> selectBudgetChip(chipBudgetPremium, "Luxury"));
        }
    }

    private void selectBudgetChip(View selected, String budget) {
        selectedBudget = budget;
        applyChoiceState(chipBudgetBudget, selected == chipBudgetBudget, true);
        applyChoiceState(chipBudgetMid, selected == chipBudgetMid, true);
        applyChoiceState(chipBudgetPremium, selected == chipBudgetPremium, true);
    }

    private void applySegmentState(TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setSelected(selected);
        chip.setBackgroundResource(selected
                ? R.drawable.bg_plan_segment_selected
                : R.drawable.bg_plan_segment_unselected);
        chip.setTextColor(selected ? WHITE : PLAN_PRIMARY);
        animateSelectable(chip, selected ? 1.02f : 1.0f, selected ? 5f : 0f);
    }

    private void applyChoiceState(View chip, boolean selected, boolean budgetCard) {
        if (chip == null) return;

        chip.setSelected(selected);
        chip.setBackgroundResource(selected
                ? (budgetCard ? R.drawable.bg_plan_budget_selected : R.drawable.bg_plan_choice_selected)
                : (budgetCard ? R.drawable.bg_plan_budget_unselected : R.drawable.bg_plan_choice_unselected));

        TextView label = chip.findViewById(R.id.choiceLabel);
        if (label != null) {
            label.setTextColor(selected ? WHITE : PLAN_TEXT_PRIMARY);
        }

        ImageView icon = chip.findViewById(R.id.choiceIcon);
        if (icon != null) {
            icon.setImageTintList(ColorStateList.valueOf(selected ? WHITE : PLAN_PRIMARY));
        }

        View iconShell = chip.findViewById(R.id.choiceIconShell);
        if (iconShell != null) {
            iconShell.setBackgroundResource(selected
                    ? R.drawable.bg_plan_choice_icon_selected
                    : R.drawable.bg_plan_choice_icon_unselected);
        }

        View badge = chip.findViewById(R.id.choiceBadge);
        if (badge != null) {
            badge.setVisibility(selected ? View.VISIBLE : View.GONE);
            if (selected) {
                badge.setScaleX(0.6f);
                badge.setScaleY(0.6f);
                badge.setAlpha(0f);
                badge.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(STATE_ANIM_MS)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }

        animateSelectable(chip, selected ? (budgetCard ? 1.03f : 1.05f) : 1.0f, selected ? 8f : 2f);
    }

    private void animateSelectable(View view, float targetScale, float elevationDp) {
        view.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(STATE_ANIM_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        ObjectAnimator elevation = ObjectAnimator.ofFloat(
                view,
                "elevation",
                view.getElevation(),
                dp(elevationDp)
        );
        elevation.setDuration(STATE_ANIM_MS);
        elevation.setInterpolator(new AccelerateDecelerateInterpolator());
        elevation.start();
    }

    private void setupCta() {
        if (btnGeneratePlan == null) return;
        btnGeneratePlan.setOnClickListener(v -> onCreateTripClicked());
    }

    private void onCreateTripClicked() {
        ArrayList<String> selectedVibes = getSelectedVibes();

        if (selectedVibes.isEmpty()) {
            shakeView(cardVibes);
            Snackbar.make(
                    btnGeneratePlan,
                    "Please select at least one type of spot",
                    Snackbar.LENGTH_SHORT
            ).setBackgroundTint(PLAN_PRIMARY)
                    .setTextColor(WHITE)
                    .show();
            return;
        }

        setGeneratingState(true);

        String typeString = selectedVibes.size() == 1
                ? selectedVibes.get(0)
                : String.join(", ", selectedVibes);

        Intent intent = new Intent(PlanTripActivity.this, ItineraryActivity.class);
        intent.putExtra("days", selectedDays);
        intent.putExtra("type", typeString);
        intent.putExtra("budget", selectedBudget);
        intent.putStringArrayListExtra("vibes", selectedVibes);
        startActivity(intent);
    }

    private void shakeView(View view) {
        if (view == null) return;
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX",
                0f, -16f, 16f, -12f, 12f, -8f, 8f, 0f);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(1f));
        shake.start();
    }

    private void setGeneratingState(boolean isGenerating) {
        if (btnGeneratePlan != null) {
            btnGeneratePlan.setEnabled(!isGenerating);
            btnGeneratePlan.setText(isGenerating ? "Generating your trip..." : "Generate My Trip");
            btnGeneratePlan.setAlpha(isGenerating ? 0.82f : 1.0f);
            btnGeneratePlan.animate()
                    .scaleX(isGenerating ? 0.98f : 1f)
                    .scaleY(isGenerating ? 0.98f : 1f)
                    .setDuration(STATE_ANIM_MS)
                    .start();
        }
        if (progressGeneratePlan != null) {
            progressGeneratePlan.setVisibility(isGenerating ? View.VISIBLE : View.GONE);
        }
        setChipsEnabled(!isGenerating);
    }

    private void setChipsEnabled(boolean enabled) {
        View[] allChips = {
                chipDay1, chipDay2, chipDay3, chipDay4,
                chipVibeHistorical, chipVibeNature, chipVibeReligious, chipVibeFood,
                chipVibeCulture, chipVibeAdventure, chipVibeScenic, chipVibeShopping,
                chipBudgetBudget, chipBudgetMid, chipBudgetPremium
        };
        for (View chip : allChips) {
            if (chip != null) {
                chip.setEnabled(enabled);
                chip.setAlpha(enabled ? 1.0f : 0.62f);
            }
        }
    }

    private void runEntranceAnimation() {
        if (planTripHeader != null) {
            planTripHeader.setAlpha(0f);
            planTripHeader.animate()
                    .alpha(1f)
                    .setDuration(360L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        if (planTripSheet != null) {
            planTripSheet.setAlpha(0f);
            planTripSheet.setTranslationY(dp(22f));
            planTripSheet.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(80L)
                    .setDuration(460L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        if (headerTravelRow != null) {
            headerTravelRow.setAlpha(0f);
            headerTravelRow.setTranslationY(dp(8f));
            headerTravelRow.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(180L)
                    .setDuration(360L)
                    .withEndAction(this::startTravelRowMotion)
                    .start();
        }
    }

    private void startTravelRowMotion() {
        if (headerTravelRow == null) return;
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(headerTravelRow, "translationY", 0f, -dp(3f), 0f);
        floatAnim.setDuration(1800L);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
