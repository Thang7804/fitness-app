package com.app.aifitness.pose;

/**
 * Gói thông tin feedback cho mỗi frame:
 *  - score: điểm form (0..100)
 *  - errorMessage: lỗi quan trọng nhất (nếu có)
 *  - reps: tổng số rep đã hoàn thành (cho bài REPS)
 *  - repCompleted: frame này vừa hoàn thành 1 rep mới hay không
 *  - holdMillis: tổng thời gian hold (ms) cho bài HOLD
 *  - holdCompleted: đã đạt target hold time hay chưa
 *
 * Class này được dùng bởi:
 *  - PoseAnalyzer (tạo ra PoseFeedback)
 *  - WorkoutHUDController (cập nhật HUD)
 *  - CameraWorkoutActivity (lưu latest score / reps / holdMillis để gửi sang Summary)
 */
public class PoseFeedback {

    private final int score;
    private final String errorMessage;

    private final int reps;
    private final boolean repCompleted;

    private final long holdMillis;
    private final boolean holdCompleted;

    public PoseFeedback(int score,
                        String errorMessage,
                        int reps,
                        boolean repCompleted,
                        long holdMillis,
                        boolean holdCompleted) {
        this.score = score;
        this.errorMessage = errorMessage;
        this.reps = reps;
        this.repCompleted = repCompleted;
        this.holdMillis = holdMillis;
        this.holdCompleted = holdCompleted;
    }

    // ===== Getter chuẩn dùng trong toàn app =====

    public int getScore() {
        return score;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Tổng reps đã hoàn thành từ đầu buổi đến frame hiện tại (cho bài REPS). */
    public int getReps() {
        return reps;
    }

    /** Frame này có vừa tick thêm 1 rep mới không. */
    public boolean isRepCompleted() {
        return repCompleted;
    }

    /** Tổng thời gian hold (ms) từ đầu buổi (cho bài HOLD). */
    public long getHoldMillis() {
        return holdMillis;
    }

    /** Đã đạt target hold time (config.targetHoldSeconds) hay chưa. */
    public boolean isHoldCompleted() {
        return holdCompleted;
    }

    // ===== Alias phòng khi chỗ nào đó dùng tên khác =====

    /** Alias để tránh lỗi nếu chỗ khác gọi getMessage() thay vì getErrorMessage(). */
    public String getMessage() {
        return errorMessage;
    }

    /** Alias cho hold seconds tính sẵn, tiện dùng ở UI. */
    public long getHoldSeconds() {
        return holdMillis / 1000L;
    }
}
