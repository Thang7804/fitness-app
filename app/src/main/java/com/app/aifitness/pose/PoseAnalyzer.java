package com.app.aifitness.pose;

import com.app.aifitness.exercise.ExerciseType;
import com.app.aifitness.exercise.Strictness;
import com.app.aifitness.util.AngleUtils;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;

/**
 * Phân tích pose từ MediaPipe và trả về PoseFeedback.
 *
 * - Hỗ trợ chi tiết:
 *   + Squat (REPS): state machine DOWN -> UP, dùng góc gối + hông.
 *   + Plank (HOLD): dùng góc thân tại hông.
 *
 * - Hỗ trợ generic cho các bài còn lại:
 *   + REPS khác Squat: generic up-down reps dựa trên biên độ di chuyển hông.
 *   + HOLD khác Plank: generic hold dựa trên độ “đứng yên” của hông/đầu.
 */
public class PoseAnalyzer {

    private final ExerciseType exerciseType;
    private final boolean isHoldExercise;
    private final Strictness strictness;
    private final StrictnessConfig config;
    private final ErrorDetector errorDetector;

    // Squat state
    private boolean squatIsDown = false;

    // Generic REPS state
    private boolean genericDown = false;
    private double genericBaselineHipY = -1.0;

    // Reps & hold
    private int totalReps = 0;

    // Hold chung (Plank + các bài HOLD khác)
    private long currentHoldMillis = 0L;
    private long holdStartTimestamp = -1L;

    // Baseline cho generic hold (giữ tư thế)
    private double baselineHipYHold = -1.0;
    private double baselineHeadYHold = -1.0;

    public PoseAnalyzer(ExerciseType exerciseType,
                        boolean isHoldExercise,
                        Strictness strictness,
                        int targetReps,
                        int targetHoldSeconds) {
        this.exerciseType = exerciseType;
        this.isHoldExercise = isHoldExercise;
        this.strictness = strictness;
        this.config = StrictnessConfig.forExercise(exerciseType, strictness, targetReps, targetHoldSeconds);
        this.errorDetector = new ErrorDetector();
    }

    /**
     * Hàm chính: nhận PoseLandmarkerResult + timestamp, trả về PoseFeedback.
     *
     * @param result      kết quả pose từ MediaPipe
     * @param timestampMs thời gian (ms) của frame (SystemClock.uptimeMillis())
     */
    public PoseFeedback process(PoseLandmarkerResult result, long timestampMs) {
        if (result == null || result.landmarks().isEmpty()) {
            resetHold();
            return new PoseFeedback(
                    0,
                    "Không thấy người trong khung hình",
                    totalReps,
                    false,
                    currentHoldMillis,
                    false
            );
        }

        List<NormalizedLandmark> landmarks = result.landmarks().get(0);
        if (landmarks == null || landmarks.size() < 33) {
            resetHold();
            return new PoseFeedback(
                    0,
                    "Di chuyển ra giữa khung hình",
                    totalReps,
                    false,
                    currentHoldMillis,
                    false
            );
        }

        // Routing logic:
        //  - Squat (REPS) -> processSquat
        //  - Plank (HOLD) -> processPlank
        //  - Các bài REPS còn lại -> processGenericReps
        //  - Các bài HOLD còn lại -> processGenericHold
        if (!isHoldExercise) {
            if (exerciseType == ExerciseType.SQUAT) {
                return processSquat(landmarks, timestampMs);
            } else {
                return processGenericReps(landmarks, timestampMs);
            }
        } else {
            if (exerciseType == ExerciseType.PLANK) {
                return processPlank(landmarks, timestampMs);
            } else {
                return processGenericHold(landmarks, timestampMs);
            }
        }
    }

    // ============================================================
    //                     SQUAT (REPS)
    // ============================================================

    private PoseFeedback processSquat(List<NormalizedLandmark> lm, long timestampMs) {
        int LEFT_HIP = 23;
        int LEFT_KNEE = 25;
        int LEFT_ANKLE = 27;
        int RIGHT_HIP = 24;
        int RIGHT_KNEE = 26;
        int RIGHT_ANKLE = 28;
        int LEFT_SHOULDER = 11;
        int RIGHT_SHOULDER = 12;

        NormalizedLandmark lHip = lm.get(LEFT_HIP);
        NormalizedLandmark lKnee = lm.get(LEFT_KNEE);
        NormalizedLandmark lAnkle = lm.get(LEFT_ANKLE);

        NormalizedLandmark rHip = lm.get(RIGHT_HIP);
        NormalizedLandmark rKnee = lm.get(RIGHT_KNEE);
        NormalizedLandmark rAnkle = lm.get(RIGHT_ANKLE);

        NormalizedLandmark lShoulder = lm.get(LEFT_SHOULDER);
        NormalizedLandmark rShoulder = lm.get(RIGHT_SHOULDER);

        // Góc gối trái/phải
        double leftKneeAngle = AngleUtils.angleFromLandmarks(lHip, lKnee, lAnkle);
        double rightKneeAngle = AngleUtils.angleFromLandmarks(rHip, rKnee, rAnkle);
        double avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2.0;

        // Góc hông trái/phải (knee-hip-shoulder)
        double leftHipAngle = AngleUtils.angleFromLandmarks(lKnee, lHip, lShoulder);
        double rightHipAngle = AngleUtils.angleFromLandmarks(rKnee, rHip, rShoulder);
        double avgHipAngle = (leftHipAngle + rightHipAngle) / 2.0;

        boolean repCompleted = false;

        // State machine DOWN -> UP với hysteresis
        if (!squatIsDown && avgKneeAngle <= config.squatDownAngle) {
            squatIsDown = true;
        } else if (squatIsDown && avgKneeAngle >= config.squatUpAngle) {
            squatIsDown = false;
            totalReps += 1;
            repCompleted = true;
        }

        // Tính score theo độ sâu + lưng
        double depthScore = computeSquatDepthScore(avgKneeAngle);
        double postureScore = computeSquatPostureScore(avgHipAngle);
        double finalScore = 0.7 * depthScore + 0.3 * postureScore;

        // Lỗi chính
        String errorMsg = errorDetector.detectSquatError(avgKneeAngle, avgHipAngle, config);

        // Với Squat, không dùng hold time
        resetHold();

        return new PoseFeedback(
                (int) Math.round(finalScore),
                errorMsg,
                totalReps,
                repCompleted,
                currentHoldMillis,
                false
        );
    }

    private double computeSquatDepthScore(double avgKneeAngle) {
        if (avgKneeAngle <= config.squatDownAngle) {
            return 100.0;
        }
        if (avgKneeAngle >= config.squatUpAngle) {
            return 30.0;
        }

        double range = config.squatUpAngle - config.squatDownAngle;
        if (range <= 0.0) {
            return 0.0;
        }

        double t = (avgKneeAngle - config.squatDownAngle) / range;
        double score = 100.0 - t * 70.0; // từ 100 giảm dần về 30
        return AngleUtils.clamp(score, 0.0, 100.0);
    }

    private double computeSquatPostureScore(double avgHipAngle) {
        // 180 độ = thân + đùi gần thẳng, lệch quá nhiều -> lưng gập
        double deviation = Math.abs(180.0 - avgHipAngle);

        if (deviation <= 10.0) {
            return 100.0;
        }
        if (deviation >= 35.0) {
            return 40.0;
        }

        double t = (deviation - 10.0) / (35.0 - 10.0);
        double score = 100.0 - t * 60.0; // từ 100 giảm dần về 40
        return AngleUtils.clamp(score, 0.0, 100.0);
    }

    // ============================================================
    //                     PLANK (HOLD)
    // ============================================================

    private PoseFeedback processPlank(List<NormalizedLandmark> lm, long timestampMs) {
        int LEFT_SHOULDER = 11;
        int LEFT_HIP = 23;
        int LEFT_ANKLE = 27;

        NormalizedLandmark lShoulder = lm.get(LEFT_SHOULDER);
        NormalizedLandmark lHip = lm.get(LEFT_HIP);
        NormalizedLandmark lAnkle = lm.get(LEFT_ANKLE);

        // Góc tại hông (shoulder-hip-ankle)
        double bodyAngle = AngleUtils.angleFromLandmarks(lShoulder, lHip, lAnkle);

        // Score dựa trên độ lệch khỏi 180
        double deviation = Math.abs(180.0 - bodyAngle);
        double score;

        if (deviation <= config.plankMaxHipDeviationDeg) {
            score = 100.0;
        } else if (deviation >= config.plankMaxHipDeviationDeg * 2.0) {
            score = 40.0;
        } else {
            double t = (deviation - config.plankMaxHipDeviationDeg) /
                    (config.plankMaxHipDeviationDeg);
            score = 100.0 - t * 60.0;
        }
        score = AngleUtils.clamp(score, 0.0, 100.0);

        // Lỗi chính
        String errorMsg = errorDetector.detectPlankError(bodyAngle, config);

        // Cập nhật thời gian hold nếu điểm >= minScore
        boolean goodEnough = score >= config.minScore;
        boolean holdCompleted = false;

        if (goodEnough) {
            if (holdStartTimestamp < 0L) {
                holdStartTimestamp = timestampMs;
            }
            currentHoldMillis = timestampMs - holdStartTimestamp;

            if (config.targetHoldSeconds > 0 &&
                    currentHoldMillis >= config.targetHoldSeconds * 1000L) {
                holdCompleted = true;
            }
        } else {
            resetHold();
        }

        return new PoseFeedback(
                (int) Math.round(score),
                errorMsg,
                totalReps,
                false,
                currentHoldMillis,
                holdCompleted
        );
    }

    // ============================================================
    //             GENERIC REPS cho các bài còn lại
    // ============================================================

    /**
     * Generic REPS:
     *  - Dùng hông trung bình (center hip) làm mốc.
     *  - Khi hông hạ xuống đủ sâu rồi trở về vị trí ban đầu -> tính 1 rep.
     *  - Score dựa trên biên độ di chuyển hông.
     *
     * Áp dụng cho: Jumping Jack, Knee Push-up, Bridge, Mountain Climber,
     * các bài Dumbbell dạng lên–xuống…
     */
    private PoseFeedback processGenericReps(List<NormalizedLandmark> lm, long timestampMs) {
        int LEFT_HIP = 23;
        int RIGHT_HIP = 24;

        NormalizedLandmark lHip = lm.get(LEFT_HIP);
        NormalizedLandmark rHip = lm.get(RIGHT_HIP);

        double hipCenterY = (lHip.y() + rHip.y()) / 2.0; // 0 = top, 1 = bottom

        // Khởi tạo baseline đứng thẳng (trung bình nhiều frame)
        if (genericBaselineHipY < 0.0) {
            genericBaselineHipY = hipCenterY;
        } else {
            // EMA nhẹ để theo kịp nếu user đổi vị trí đứng
            double alpha = 0.02;
            genericBaselineHipY = (1 - alpha) * genericBaselineHipY + alpha * hipCenterY;
        }

        double diff = hipCenterY - genericBaselineHipY; // >0: hạ thấp người

        // Ngưỡng: user phải hạ xuống ít nhất ~7% chiều cao khung hình
        double downThreshold = 0.07;
        double upThreshold = downThreshold / 2.0;

        boolean repCompleted = false;

        if (!genericDown && diff > downThreshold) {
            genericDown = true;
        } else if (genericDown && diff < upThreshold) {
            genericDown = false;
            totalReps += 1;
            repCompleted = true;
        }

        // Score theo biên độ hiện tại
        double amplitude = Math.abs(diff);
        double targetAmplitude = downThreshold * 1.5; // di chuyển ~10% chiều cao là đẹp
        double amplitudeScore = 100.0 * Math.min(1.0, amplitude / targetAmplitude);
        amplitudeScore = AngleUtils.clamp(amplitudeScore, 30.0, 100.0);

        String errorMsg = "";
        if (amplitudeScore < config.minScore) {
            errorMsg = "Tăng biên độ chuyển động";
        }

        resetHold(); // không dùng hold cho bài REPS

        return new PoseFeedback(
                (int) Math.round(amplitudeScore),
                errorMsg,
                totalReps,
                repCompleted,
                currentHoldMillis,
                false
        );
    }

    // ============================================================
    //            GENERIC HOLD cho các bài còn lại
    // ============================================================

    /**
     * Generic HOLD:
     *  - Đo độ “yên” của hông và đầu so với tư thế ban đầu.
     *  - Ít di chuyển -> score cao, tiếp tục tính hold.
     *
     * Áp dụng cho: Downward Dog, Cobra, Seated Straddle,...
     */
    private PoseFeedback processGenericHold(List<NormalizedLandmark> lm, long timestampMs) {
        int HEAD = 0;
        int LEFT_HIP = 23;
        int RIGHT_HIP = 24;

        NormalizedLandmark head = lm.get(HEAD);
        NormalizedLandmark lHip = lm.get(LEFT_HIP);
        NormalizedLandmark rHip = lm.get(RIGHT_HIP);

        double hipCenterY = (lHip.y() + rHip.y()) / 2.0;
        double headY = head.y();

        // Khởi tạo baseline tư thế ban đầu
        if (baselineHipYHold < 0.0 || baselineHeadYHold < 0.0) {
            baselineHipYHold = hipCenterY;
            baselineHeadYHold = headY;
        }

        double hipDiff = Math.abs(hipCenterY - baselineHipYHold);
        double headDiff = Math.abs(headY - baselineHeadYHold);

        // movement ~ tổng độ lệch, càng lớn càng xấu
        double movement = hipDiff * 0.7 + headDiff * 0.3;

        // Map movement [0..0.12] -> score [100..40]
        double maxMovement = 0.12; // lệch khoảng 12% chiều cao là rất nhiều
        double t = Math.min(1.0, movement / maxMovement);
        double score = 100.0 - t * 60.0; // 100 -> 40
        score = AngleUtils.clamp(score, 30.0, 100.0);

        boolean goodEnough = score >= config.minScore;
        boolean holdCompleted = false;
        String errorMsg = "";

        if (!goodEnough) {
            errorMsg = "Giữ cơ thể ít di chuyển hơn";
            resetHold();
        } else {
            if (holdStartTimestamp < 0L) {
                holdStartTimestamp = timestampMs;
            }
            currentHoldMillis = timestampMs - holdStartTimestamp;

            if (config.targetHoldSeconds > 0 &&
                    currentHoldMillis >= config.targetHoldSeconds * 1000L) {
                holdCompleted = true;
            }
        }

        return new PoseFeedback(
                (int) Math.round(score),
                errorMsg,
                totalReps,
                false,
                currentHoldMillis,
                holdCompleted
        );
    }

    // ============================================================
    //                       Helpers
    // ============================================================

    private void resetHold() {
        holdStartTimestamp = -1L;
        currentHoldMillis = 0L;
        baselineHipYHold = -1.0;
        baselineHeadYHold = -1.0;
    }
}
