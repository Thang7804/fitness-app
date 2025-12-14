package com.app.aifitness.workout;

/**
 * Model lưu đánh giá của người dùng về bài tập
 */
public class WorkoutFeedback {
    private String id;
    private String workoutSessionId;  // ID của workout session liên quan
    private String exerciseName;
    private String exerciseType;
    private String difficulty;  // "too_easy", "ok", "too_hard"
    private int rating;  // 1-5 stars
    private String comment;  // Ghi chú của người dùng
    private long timestamp;
    private boolean needsAdjustment;  // Có cần điều chỉnh bài tập không

    // Constructor rỗng cho Firestore
    public WorkoutFeedback() {
    }

    public WorkoutFeedback(String id, String workoutSessionId, String exerciseName, 
                          String exerciseType, String difficulty, int rating, 
                          String comment, long timestamp, boolean needsAdjustment) {
        this.id = id;
        this.workoutSessionId = workoutSessionId;
        this.exerciseName = exerciseName;
        this.exerciseType = exerciseType;
        this.difficulty = difficulty;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
        this.needsAdjustment = needsAdjustment;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkoutSessionId() {
        return workoutSessionId;
    }

    public void setWorkoutSessionId(String workoutSessionId) {
        this.workoutSessionId = workoutSessionId;
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isNeedsAdjustment() {
        return needsAdjustment;
    }

    public void setNeedsAdjustment(boolean needsAdjustment) {
        this.needsAdjustment = needsAdjustment;
    }
}

