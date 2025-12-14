package com.app.aifitness.pose;

import com.app.aifitness.exercise.ExerciseType;
import com.app.aifitness.exercise.Strictness;

/**
 * Cấu hình theo độ strictness cho từng bài tập.
 *
 *  - Squat: ngưỡng góc gối DOWN / UP (để đếm rep)
 *  - Plank: độ lệch tối đa cho phép ở hông (so với thân thẳng)
 *  - minScore: điểm tối thiểu coi là "đạt" (EASY/NORMAL/HARD)
 */
public class StrictnessConfig {

    // Squat
    public final float squatDownAngle;   // ví dụ: 90 độ (càng nhỏ càng sâu)
    public final float squatUpAngle;     // ví dụ: 160 độ (đứng thẳng)

    // Plank
    public final float plankMaxHipDeviationDeg; // độ lệch tối đa cho phép (|180 - angleHip|)

    // Điểm tối thiểu coi là đạt theo strictness
    public final int minScore;

    // Mục tiêu tham khảo
    public final int targetReps;
    public final int targetHoldSeconds;

    public StrictnessConfig(float squatDownAngle,
                            float squatUpAngle,
                            float plankMaxHipDeviationDeg,
                            int minScore,
                            int targetReps,
                            int targetHoldSeconds) {
        this.squatDownAngle = squatDownAngle;
        this.squatUpAngle = squatUpAngle;
        this.plankMaxHipDeviationDeg = plankMaxHipDeviationDeg;
        this.minScore = minScore;
        this.targetReps = targetReps;
        this.targetHoldSeconds = targetHoldSeconds;
    }

    /**
     * Tạo StrictnessConfig cho một bài tập cụ thể.
     * Ở Bước 6 ta tập trung cho SQUAT và PLANK.
     */
    public static StrictnessConfig forExercise(ExerciseType type,
                                               Strictness strictness,
                                               int targetReps,
                                               int targetHoldSeconds) {
        int minScore = strictness.getMinScore();

        // Giá trị mặc định
        float squatDown = 90f;
        float squatUp = 160f;
        float plankDeviation = 15f;

        // Điều chỉnh theo Strictness
        switch (strictness) {
            case EASY:
                squatDown = 100f;
                squatUp = 150f;
                plankDeviation = 20f;
                break;
            case NORMAL:
                squatDown = 90f;
                squatUp = 160f;
                plankDeviation = 15f;
                break;
            case HARD:
                squatDown = 80f;
                squatUp = 170f;
                plankDeviation = 10f;
                break;
        }

        // Cho các bài khác, vẫn dùng chung config cơ bản
        return new StrictnessConfig(
                squatDown,
                squatUp,
                plankDeviation,
                minScore,
                targetReps,
                targetHoldSeconds
        );
    }
}
