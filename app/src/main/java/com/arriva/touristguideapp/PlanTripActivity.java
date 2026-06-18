package com.arriva.touristguideapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class PlanTripActivity extends BaseActivity {

    // ─── Duration chips ───────────────────────────────────────────────────────
    private TextView chipDay1, chipDay2, chipDay3, chipDay4;
    private int selectedDays = 1;

    // ─── Vibe / spot-type chips (multi-select) ────────────────────────────────
    private TextView chipVibeHistorical;
    private TextView chipVibeNature;
    private TextView chipVibeReligious;
    private TextView chipVibeFood;
    private TextView chipVibeCulture;
    private TextView chipVibeAdventure;
    private TextView chipVibeScenic;
    private TextView chipVibeShopping;
    /** Mutable set of currently selected vibe labels (display labels, trimmed). */
    private final List<TextView> vibeChips = new ArrayList<>();

    // ─── Budget chips ─────────────────────────────────────────────────────────
    private TextView chipBudgetBudget, chipBudgetMid, chipBudgetPremium;
    private String selectedBudget = "Mid-range";

    // ─── CTA ──────────────────────────────────────────────────────────────────
    private View   cardVibes;
    private TextView btnGeneratePlan;
    private ProgressBar progressGeneratePlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_trip);

        // Back button in the custom header
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        bindViews();
        setupDurationChips();
        setupVibeChips();
        setupBudgetChips();
        setupCta();

        // Default selections on first load
        selectDurationChip(chipDay1, 1);
        selectBudgetChip(chipBudgetMid, "Mid-range");
    }

    @Override
    protected void onResume() {
        super.onResume();
        setGeneratingState(false);
    }

    // ─── View binding ─────────────────────────────────────────────────────────

    private void bindViews() {
        chipDay1 = findViewById(R.id.chipDay1);
        chipDay2 = findViewById(R.id.chipDay2);
        chipDay3 = findViewById(R.id.chipDay3);
        chipDay4 = findViewById(R.id.chipDay4);

        chipVibeHistorical = findViewById(R.id.chipVibeHistorical);
        chipVibeNature     = findViewById(R.id.chipVibeNature);
        chipVibeReligious  = findViewById(R.id.chipVibeReligious);
        chipVibeFood       = findViewById(R.id.chipVibeFood);
        chipVibeCulture    = findViewById(R.id.chipVibeCulture);
        chipVibeAdventure  = findViewById(R.id.chipVibeAdventure);
        chipVibeScenic     = findViewById(R.id.chipVibeScenic);
        chipVibeShopping   = findViewById(R.id.chipVibeShopping);

        chipBudgetBudget  = findViewById(R.id.chipBudgetBudget);
        chipBudgetMid     = findViewById(R.id.chipBudgetMid);
        chipBudgetPremium = findViewById(R.id.chipBudgetPremium);

        cardVibes         = findViewById(R.id.cardVibes);
        btnGeneratePlan   = findViewById(R.id.btnGeneratePlan);
        progressGeneratePlan = findViewById(R.id.progressGeneratePlan);
    }

    // ─── Duration chips: single-select ────────────────────────────────────────

    private void setupDurationChips() {
        chipDay1.setOnClickListener(v -> selectDurationChip(chipDay1, 1));
        chipDay2.setOnClickListener(v -> selectDurationChip(chipDay2, 2));
        chipDay3.setOnClickListener(v -> selectDurationChip(chipDay3, 3));
        chipDay4.setOnClickListener(v -> selectDurationChip(chipDay4, 4));
    }

    private void selectDurationChip(TextView selected, int days) {
        selectedDays = days;
        applyChipSelected(chipDay1, selected == chipDay1);
        applyChipSelected(chipDay2, selected == chipDay2);
        applyChipSelected(chipDay3, selected == chipDay3);
        applyChipSelected(chipDay4, selected == chipDay4);
    }

    // ─── Vibe chips: multi-select ──────────────────────────────────────────────

    private void setupVibeChips() {
        // Register chips with their backend category label (matches ItineraryActivity/server)
        registerVibeChip(chipVibeHistorical, "Historical");
        registerVibeChip(chipVibeNature,     "Nature");
        registerVibeChip(chipVibeReligious,  "Religious");
        registerVibeChip(chipVibeFood,       "Food");
        registerVibeChip(chipVibeCulture,    "Culture");
        registerVibeChip(chipVibeAdventure,  "Adventure");
        registerVibeChip(chipVibeScenic,     "Scenic");
        registerVibeChip(chipVibeShopping,   "Shopping");
    }

    /**
     * Registers a vibe chip. The chip stores its category label in the tag,
     * and its selected state is tracked by the {@code vibeChips} list.
     */
    private void registerVibeChip(TextView chip, String categoryLabel) {
        chip.setTag(categoryLabel);
        chip.setOnClickListener(v -> toggleVibeChip(chip));
    }

    private void toggleVibeChip(TextView chip) {
        boolean isNowSelected = !vibeChips.contains(chip);
        if (isNowSelected) {
            vibeChips.add(chip);
        } else {
            vibeChips.remove(chip);
        }
        applyChipSelected(chip, isNowSelected);
    }

    /** Returns a list of the backend category strings for all selected vibe chips. */
    private ArrayList<String> getSelectedVibes() {
        ArrayList<String> labels = new ArrayList<>();
        for (TextView chip : vibeChips) {
            Object tag = chip.getTag();
            if (tag instanceof String) {
                labels.add((String) tag);
            }
        }
        return labels;
    }

    // ─── Budget chips: single-select ──────────────────────────────────────────

    private void setupBudgetChips() {
        chipBudgetBudget.setOnClickListener(v  -> selectBudgetChip(chipBudgetBudget, "Budget"));
        chipBudgetMid.setOnClickListener(v     -> selectBudgetChip(chipBudgetMid, "Mid-range"));
        chipBudgetPremium.setOnClickListener(v -> selectBudgetChip(chipBudgetPremium, "Luxury"));
    }

    private void selectBudgetChip(TextView selected, String budget) {
        selectedBudget = budget;
        applyChipSelected(chipBudgetBudget,  selected == chipBudgetBudget);
        applyChipSelected(chipBudgetMid,     selected == chipBudgetMid);
        applyChipSelected(chipBudgetPremium, selected == chipBudgetPremium);
    }

    // ─── Chip visual helper ────────────────────────────────────────────────────

    /**
     * Switches a chip between selected (solid purple, white text) and
     * unselected (white fill, purple text/outline) states by toggling the
     * drawable's checked state via the view's background state list.
     */
    private void applyChipSelected(TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setSelected(selected);
        chip.getBackground().setState(selected
                ? new int[]{android.R.attr.state_checked}
                : new int[]{});
        chip.setTextColor(selected
                ? 0xFFFFFFFF   // white on filled purple
                : 0xFF7C4DFF); // purple on white
    }

    // ─── CTA ──────────────────────────────────────────────────────────────────

    private void setupCta() {
        if (btnGeneratePlan == null) return;
        btnGeneratePlan.setOnClickListener(v -> onCreateTripClicked());
    }

    private void onCreateTripClicked() {
        ArrayList<String> selectedVibes = getSelectedVibes();

        // Validate: at least one vibe must be selected
        if (selectedVibes.isEmpty()) {
            shakeView(cardVibes);
            Snackbar.make(
                    btnGeneratePlan,
                    "Please select at least one type of spot",
                    Snackbar.LENGTH_SHORT
            ).setBackgroundTint(0xFF7C4DFF)
             .setTextColor(0xFFFFFFFF)
             .show();
            return;
        }

        setGeneratingState(true);

        // Build a comma-joined type string for the backend (ItineraryActivity passes
        // it to generateAiPlan and the server handles comma-separated multi-vibe).
        String typeString = selectedVibes.size() == 1
                ? selectedVibes.get(0)
                : String.join(", ", selectedVibes);

        Intent intent = new Intent(PlanTripActivity.this, ItineraryActivity.class);
        intent.putExtra("days",   selectedDays);
        intent.putExtra("type",   typeString);
        intent.putExtra("budget", selectedBudget);
        intent.putStringArrayListExtra("vibes", selectedVibes);
        startActivity(intent);
    }

    // ─── Shake animation for empty vibe validation ────────────────────────────

    private void shakeView(View view) {
        if (view == null) return;
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX",
                0f, -16f, 16f, -12f, 12f, -8f, 8f, 0f);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(1f));
        shake.start();
    }

    // ─── Loading / generating state ───────────────────────────────────────────

    private void setGeneratingState(boolean isGenerating) {
        if (btnGeneratePlan != null) {
            btnGeneratePlan.setEnabled(!isGenerating);
            btnGeneratePlan.setText(isGenerating ? "Creating your AI trip…" : "✨  Create My Trip");
            btnGeneratePlan.setAlpha(isGenerating ? 0.75f : 1.0f);
        }
        if (progressGeneratePlan != null) {
            progressGeneratePlan.setVisibility(isGenerating ? View.VISIBLE : View.GONE);
        }
        // Disable all chips during generation
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
                chip.setAlpha(enabled ? 1.0f : 0.6f);
            }
        }
    }
}
