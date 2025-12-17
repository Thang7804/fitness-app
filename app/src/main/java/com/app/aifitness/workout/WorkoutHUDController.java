package com.app.aifitness.workout;

import android.widget.TextView;

import com.app.aifitness.pose.PoseFeedback;

/**
 * HUD hiển thị "ổn định":
 * - Reps/Time
 * - Score: ưu tiên sessionScore (ổn định theo rep)
 * - FormLabel + message ngắn gọn
 */
public class WorkoutHUDController {

    private final TextView tvExerciseName;
    private final TextView tvRepsOrTime;
    private final TextView tvScore;
    private final TextView tvError;
    private final boolean isHold;

    public WorkoutHUDController(
            TextView tvExerciseName,
            TextView tvRepsOrTime,
            TextView tvScore,
            TextView tvError,
            boolean isHold
    ) {
        this.tvExerciseName = tvExerciseName;
        this.tvRepsOrTime = tvRepsOrTime;
        this.tvScore = tvScore;
        this.tvError = tvError;
        this.isHold = isHold;
    }

    public void update(PoseFeedback fb) {
        if (fb == null) return;

        if (isHold) {
            long sec = fb.getHoldMillis() / 1000L;
            tvRepsOrTime.setText("Time: " + sec + " s");
        } else {
            tvRepsOrTime.setText("Reps: " + fb.getReps());
        }

        // Score ổn định: sessionScore chỉ có sau khi hoàn thành >= 1 rep
        int sessionScore = fb.getSessionScore();
        int liveScore = fb.getLiveScore();

        if (!fb.isScoringActive()) {
            tvScore.setText("Score: --");
        } else {
            if (sessionScore > 0) tvScore.setText("Score: " + sessionScore);
            else tvScore.setText("Score: " + liveScore); // chưa có rep nào thì tạm show live
        }

        String label = fb.getFormLabel();
        String msg = fb.getMessage();

        if (msg == null) msg = "";
        if (label == null) label = "";

        // Text error: ưu tiên dạng "LABEL - message"
        if (!msg.isEmpty() && !"Good".equalsIgnoreCase(msg)) {
            tvError.setText(label + " - " + msg);
        } else {
            tvError.setText(label);
        }
    }
}
