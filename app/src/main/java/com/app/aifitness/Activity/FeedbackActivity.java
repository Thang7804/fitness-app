package com.app.aifitness.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;

import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.R;
import com.app.aifitness.workout.WorkoutFeedback;
import com.google.android.material.button.MaterialButton;

public class FeedbackActivity extends AppCompatActivity {

    private TextView tvExerciseName;
    private RadioGroup rgDifficulty;
    private RadioButton rbTooEasy;
    private RadioButton rbOk;
    private RadioButton rbTooHard;
    private RatingBar ratingBar;
    private EditText edtComment;
    private MaterialButton btnSubmit;

    private String exerciseName;
    private String exerciseType;
    private String workoutSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        initViews();
        readIntentData();
        setupSubmitButton();
    }

    private void initViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName);
        rgDifficulty = findViewById(R.id.rgDifficulty);
        rbTooEasy = findViewById(R.id.rbTooEasy);
        rbOk = findViewById(R.id.rbOk);
        rbTooHard = findViewById(R.id.rbTooHard);
        ratingBar = findViewById(R.id.ratingBar);
        edtComment = findViewById(R.id.edtComment);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void readIntentData() {
        exerciseName = getIntent().getStringExtra("exercise_name");
        exerciseType = getIntent().getStringExtra("exercise_type");
        workoutSessionId = getIntent().getStringExtra("workout_session_id");

        if (TextUtils.isEmpty(exerciseName)) {
            exerciseName = "Workout";
        }
        if (TextUtils.isEmpty(exerciseType)) {
            exerciseType = "UNKNOWN";
        }

        tvExerciseName.setText("How was " + exerciseName + "?");
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        // Validate difficulty selection
        int selectedId = rgDifficulty.getCheckedRadioButtonId();
        final String difficulty;
        final boolean needsAdjustment;

        if (selectedId == R.id.rbTooEasy) {
            difficulty = "too_easy";
            needsAdjustment = true;
        } else if (selectedId == R.id.rbOk) {
            difficulty = "ok";
            needsAdjustment = false;
        } else if (selectedId == R.id.rbTooHard) {
            difficulty = "too_hard";
            needsAdjustment = true;
        } else {
            Toast.makeText(this, "Please select difficulty level", Toast.LENGTH_SHORT).show();
            return;
        }

        int rating = (int) ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Please rate this exercise", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = edtComment.getText().toString().trim();

        // Create final variable for exerciseType (it's a class field, but we make it final for inner class)
        final String finalExerciseType = exerciseType;

        // Create feedback
        WorkoutFeedback feedback = new WorkoutFeedback(
                null, // id will be set by Firebase
                workoutSessionId,
                exerciseName,
                exerciseType,
                difficulty,
                rating,
                comment,
                System.currentTimeMillis(),
                needsAdjustment
        );

        // Save feedback
        btnSubmit.setEnabled(false);
        FirebaseHelper.getInstance().saveWorkoutFeedback(feedback, new Callback() {
            @Override
            public void onSuccess() {
                Toast.makeText(FeedbackActivity.this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                
                // Adjust workout if needed
                if (needsAdjustment) {
                    FirebaseHelper.getInstance().adjustWorkoutBasedOnFeedback(
                            finalExerciseType, difficulty, new Callback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(FeedbackActivity.this, 
                                            "Workout adjusted based on your feedback", 
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // Silent fail for adjustment
                                }
                            });
                }

                // Go back to DayDetailActivity hoặc MainActivity
                String dayName = getIntent().getStringExtra("dayName");
                if (dayName != null && !dayName.isEmpty()) {
                    Intent intent = new Intent(FeedbackActivity.this, DayDetailActivity.class);
                    intent.putExtra("dayName", dayName);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(FeedbackActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                btnSubmit.setEnabled(true);
                Toast.makeText(FeedbackActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

