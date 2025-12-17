package com.app.aifitness.pose;

/**
 * PoseFeedback:
 * - reps / holdMillis: tiến độ bài tập
 * - liveScore: điểm realtime (đã làm mượt) để hiển thị trực quan
 * - sessionScore: điểm ổn định (dùng làm "score chính")
 * - formLabel/message: feedback ngắn gọn cho user
 * - progress01: 0..1 (tiến độ)
 * - scoringActive: true khi đã qua warm-up và đủ điều kiện chấm
 *
 * NOTE: File này có thêm constructor + method "alias" để tương thích code cũ
 * (PoseAnalyzer / CameraWorkoutActivity phiên bản cũ hay gọi getScore(), getErrorMessage(), isRepCompleted()).
 */
public class PoseFeedback {
    private final int reps;
    private final long holdMillis;
    private final boolean holdCompleted;

    // Điểm realtime (đã làm mượt) để hiển thị “trực quan”
    private final int liveScore;

    // Điểm ổn định theo rep (hoặc theo hold) -> dùng làm “Score chính”
    private final int sessionScore;

    // Nhãn form: GOOD / OK / BAD / GET_IN_FRAME / ...
    private final String formLabel;

    // Thông báo lỗi ngắn gọn (VD: "Back straight", "Go deeper", ...)
    private final String message;

    // Tiến độ bài (0..1): reps/target hoặc holdTime/target
    private final float progress01;

    // true nếu frame đủ điều kiện chấm điểm (đã qua warm-up)
    private final boolean scoringActive;

    // ===== Extra compat flags (for old code) =====
    // Một số code cũ dùng flag "repCompleted" (vừa hoàn thành 1 rep).
    // Với kiến trúc mới, nếu bạn chưa dùng thì cứ để false.
    private final boolean repCompleted;

    public PoseFeedback(
            int reps,
            long holdMillis,
            boolean holdCompleted,
            int liveScore,
            int sessionScore,
            String formLabel,
            String message,
            float progress01,
            boolean scoringActive
    ) {
        this(reps, holdMillis, holdCompleted, liveScore, sessionScore, formLabel, message, progress01, scoringActive, false);
    }

    // Full constructor nội bộ (có repCompleted)
    public PoseFeedback(
            int reps,
            long holdMillis,
            boolean holdCompleted,
            int liveScore,
            int sessionScore,
            String formLabel,
            String message,
            float progress01,
            boolean scoringActive,
            boolean repCompleted
    ) {
        this.reps = reps;
        this.holdMillis = holdMillis;
        this.holdCompleted = holdCompleted;
        this.liveScore = liveScore;
        this.sessionScore = sessionScore;
        this.formLabel = formLabel;
        this.message = message;
        this.progress01 = progress01;
        this.scoringActive = scoringActive;
        this.repCompleted = repCompleted;
    }

    // =========================================================
    // ✅ Backward-compatible constructor (OLD PoseAnalyzer 6 params)
    // required: 9 params (new) but old code passes 6 -> provide overload.
    // Old order thường là: reps, holdMillis, score, holdCompleted, formLabel, message
    // =========================================================
    public PoseFeedback(
            int reps,
            long holdMillis,
            int score,
            boolean holdCompleted,
            String formLabel,
            String message
    ) {
        this(
                reps,
                holdMillis,
                holdCompleted,
                /*liveScore*/ score,
                /*sessionScore*/ score,
                formLabel,
                message,
                /*progress01*/ 0f,
                /*scoringActive*/ true,
                /*repCompleted*/ false
        );
    }
    // =========================================================
// ✅ Backward-compatible constructor (ALT old order)
// Some old code calls: (int reps, String message, int score, boolean repCompleted, long holdMillis, boolean holdCompleted)
// =========================================================
    public PoseFeedback(
            int reps,
            String message,
            int score,
            boolean repCompleted,
            long holdMillis,
            boolean holdCompleted
    ) {
        this(
                reps,
                holdMillis,
                holdCompleted,
                /*liveScore*/ score,
                /*sessionScore*/ score,
                /*formLabel*/ repCompleted ? "REP_OK" : "OK",
                /*message*/ message,
                /*progress01*/ 0f,
                /*scoringActive*/ true,
                /*repCompleted*/ repCompleted
        );
    }
    public int getReps() { return reps; }
    public long getHoldMillis() { return holdMillis; }
    public boolean isHoldCompleted() { return holdCompleted; }

    public int getLiveScore() { return liveScore; }
    public int getSessionScore() { return sessionScore; }

    /**
     * Alias để tương thích code cũ (CameraWorkoutActivity / HUD đang gọi getScore()).
     * "Score chính" theo định nghĩa hiện tại là sessionScore.
     */
    public int getScore() { return sessionScore; }

    public String getFormLabel() { return formLabel; }
    public String getMessage() { return message; }

    public float getProgress01() { return progress01; }
    public boolean isScoringActive() { return scoringActive; }

    // =========================================================
    // ✅ Backward-compatible methods
    // =========================================================

    /**
     * Code cũ hay gọi isRepCompleted() để biết vừa xong 1 rep.
     * Nếu hệ thống mới chưa dùng, cứ trả repCompleted.
     */
    public boolean isRepCompleted() {
        return repCompleted;
    }

    /**
     * Code cũ hay gọi getErrorMessage() -> map sang message/formLabel.
     */
    public String getErrorMessage() {
        if (message != null && !message.trim().isEmpty()) return message;
        if (formLabel != null && !formLabel.trim().isEmpty()) return formLabel;
        return "";
    }
}