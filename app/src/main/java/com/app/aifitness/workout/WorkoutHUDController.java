package com.app.aifitness.workout;

import android.graphics.Color;
import android.text.TextUtils;
import android.widget.TextView;

import com.app.aifitness.pose.PoseFeedback;

/**
 * Quản lý HUD trong lúc tập:
 *  - Update text Reps / Time
 *  - Đổi màu Score theo ngưỡng:
 *      đỏ   <= 60
 *      vàng  60–80
 *      xanh  > 80
 *  - Hiển thị lỗi ngắn gọn (1 lỗi quan trọng nhất)
 */
public class WorkoutHUDController {

    private final TextView tvExerciseName;
    private final TextView tvRepsOrTime;
    private final TextView tvScore;
    private final TextView tvError;

    private final boolean isHoldExercise;

    public WorkoutHUDController(TextView tvExerciseName,
                                TextView tvRepsOrTime,
                                TextView tvScore,
                                TextView tvError,
                                boolean isHoldExercise) {
        this.tvExerciseName = tvExerciseName;
        this.tvRepsOrTime = tvRepsOrTime;
        this.tvScore = tvScore;
        this.tvError = tvError;
        this.isHoldExercise = isHoldExercise;
    }

    public void update(PoseFeedback feedback) {
        if (feedback == null) return;

        // ===== Score + màu =====
        int score = feedback.getScore();
        tvScore.setText("Score: " + score);

        int color;
        if (score <= 60) {
            color = Color.RED;
        } else if (score <= 80) {
            color = Color.YELLOW;
        } else {
            color = Color.GREEN;
        }
        tvScore.setTextColor(color);

        // ===== Reps / Time =====
        if (isHoldExercise) {
            long seconds = feedback.getHoldMillis() / 1000L;
            tvRepsOrTime.setText("Time: " + seconds + " s");
        } else {
            int reps = feedback.getReps();
            tvRepsOrTime.setText("Reps: " + reps);
        }

        // ===== Error message ngắn gọn =====
        String msg = feedback.getErrorMessage();
        if (TextUtils.isEmpty(msg)) {
            tvError.setText("Good form!");
        } else {
            tvError.setText(msg);
        }
    }
}
