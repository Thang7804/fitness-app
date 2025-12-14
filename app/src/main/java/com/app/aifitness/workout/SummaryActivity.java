package com.app.aifitness.workout;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Activity.WorkoutHistoryActivity;
import com.app.aifitness.databinding.ActivitySummaryBinding;

/**
 * SummaryActivity (lite):
 * - Hiển thị thống kê buổi tập
 * - Chưa có Home/History (để sau)
 */
public class SummaryActivity extends AppCompatActivity {

    private ActivitySummaryBinding binding;

    private String exerciseName;
    private String exerciseType;
    private boolean isHold;
    private String strictness;
    private int totalReps;
    private long totalHoldMillis;
    private int score;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        readIntentData();
        setupUI();
        setupButtons();
    }

    private void readIntentData() {
        Intent intent = getIntent();
        exerciseName = intent.getStringExtra("exercise_display_name");
        exerciseType = intent.getStringExtra("exercise_type");
        isHold = intent.getBooleanExtra("is_hold", false);
        strictness = intent.getStringExtra("strictness");
        totalReps = intent.getIntExtra("total_reps", 0);
        totalHoldMillis = intent.getLongExtra("total_hold_millis", 0L);
        score = intent.getIntExtra("score", 0);

        if (TextUtils.isEmpty(exerciseName)) exerciseName = "Workout";
        if (TextUtils.isEmpty(exerciseType)) exerciseType = "UNKNOWN";
        if (TextUtils.isEmpty(strictness)) strictness = "NORMAL";
    }

    private void setupUI() {
        binding.tvSummaryTitle.setText(exerciseName + " Complete!");

        String modeLabel = isHold ? "HOLD" : "REPS";
        String strictLabel = "Strictness: " + strictness;

        String stats;
        if (isHold) {
            long seconds = totalHoldMillis / 1000L;
            stats = "Mode: " + modeLabel + "\n"
                    + strictLabel + "\n"
                    + "Hold time: " + seconds + " s";
        } else {
            stats = "Mode: " + modeLabel + "\n"
                    + strictLabel + "\n"
                    + "Total reps: " + totalReps;
        }

        binding.tvSummaryStats.setText(stats);
        binding.tvSummaryScore.setText(String.valueOf(score));

        // Set score color
        int color;
        if (score <= 60) {
            color = Color.parseColor("#F44336"); // Red
        } else if (score <= 80) {
            color = Color.parseColor("#FF9800"); // Orange
        } else {
            color = Color.parseColor("#4CAF50"); // Green
        }
        binding.tvSummaryScore.setTextColor(color);
    }

    private void setupButtons() {
        binding.btnGoHome.setOnClickListener(v -> finish());
        
        binding.btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(SummaryActivity.this, WorkoutHistoryActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
