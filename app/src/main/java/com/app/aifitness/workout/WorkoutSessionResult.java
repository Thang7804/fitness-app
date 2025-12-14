package com.app.aifitness.workout;

/**
 * Model lưu 1 buổi tập để đưa lên Firestore.
 *
 * Firestore yêu cầu:
 *  - Có constructor rỗng (no-arg)
 *  - Có getter/setter public cho từng field
 */
public class WorkoutSessionResult {

    private String id;                   // Firestore document id (set sau khi load)
    private String exerciseName;         // Ví dụ: "Squat"
    private String exerciseType;         // Enum name: "SQUAT"
    private boolean holdExercise;        // true = HOLD, false = REPS
    private String strictness;           // "EASY" / "NORMAL" / "HARD"

    private int totalReps;               // cho REPS
    private long totalHoldMillis;        // cho HOLD (ms)
    private int avgScore;                // Điểm cuối (hoặc trung bình)

    private long startTime;              // timestamp ms (approx)
    private long endTime;                // timestamp ms

    // ===== BẮT BUỘC cho Firestore =====
    public WorkoutSessionResult() {
    }

    public WorkoutSessionResult(String id,
                                String exerciseName,
                                String exerciseType,
                                boolean holdExercise,
                                String strictness,
                                int totalReps,
                                long totalHoldMillis,
                                int avgScore,
                                long startTime,
                                long endTime) {
        this.id = id;
        this.exerciseName = exerciseName;
        this.exerciseType = exerciseType;
        this.holdExercise = holdExercise;
        this.strictness = strictness;
        this.totalReps = totalReps;
        this.totalHoldMillis = totalHoldMillis;
        this.avgScore = avgScore;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // ===== Getter / Setter =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public boolean isHoldExercise() {
        return holdExercise;
    }

    public void setHoldExercise(boolean holdExercise) {
        this.holdExercise = holdExercise;
    }

    public String getStrictness() {
        return strictness;
    }

    public void setStrictness(String strictness) {
        this.strictness = strictness;
    }

    public int getTotalReps() {
        return totalReps;
    }

    public void setTotalReps(int totalReps) {
        this.totalReps = totalReps;
    }

    public long getTotalHoldMillis() {
        return totalHoldMillis;
    }

    public void setTotalHoldMillis(long totalHoldMillis) {
        this.totalHoldMillis = totalHoldMillis;
    }

    public int getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(int avgScore) {
        this.avgScore = avgScore;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    // ===== Helper cho UI =====

    public String getModeLabel() {
        return holdExercise ? "HOLD" : "REPS";
    }

    public long getDurationSeconds() {
        if (startTime <= 0 || endTime <= 0 || endTime < startTime) {
            return 0;
        }
        return (endTime - startTime) / 1000L;
    }

    public long getHoldSeconds() {
        return totalHoldMillis / 1000L;
    }
}
