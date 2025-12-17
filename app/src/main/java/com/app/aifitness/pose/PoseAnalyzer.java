package com.app.aifitness.pose;

import com.app.aifitness.exercise.ExerciseType;
import com.app.aifitness.exercise.Strictness;
import com.app.aifitness.util.AngleUtils;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

/**
 * PoseAnalyzer:
 * - Nhận PoseLandmarkerResult mỗi frame
 * - Tính:
 *   + reps / hold time
 *   + score (mượt, ít nhảy)
 *   + error message "dễ hiểu"
 *
 * YÊU CẦU CỦA BẠN:
 * - switch-case: SQUAT giữ riêng
 * - PLANK bổ sung riêng như squat
 * - Các bài còn lại: GenericRepRule (đủ dùng, không quá chi tiết)
 */
public class PoseAnalyzer {

    // ====== Config đầu vào ======
    private final ExerciseType exerciseType;
    private final boolean isHold;
    private final Strictness strictness;
    private final int targetReps;
    private final int targetHoldSeconds;

    // ====== State chung ======
    private int reps = 0;
    private long holdMillis = 0L;

    // Score mượt để tránh nhảy loạn khi chưa vào form
    private float scoreEma = 0f;
    private boolean emaInit = false;

    // ====== SQUAT state ======
    private enum SquatState { UP, DOWN }
    private SquatState squatState = SquatState.UP;
    private long lastRepMs = 0L;

    // ====== PLANK state ======
    private long lastGoodHoldMs = -1L;     // thời điểm frame "đạt chuẩn" gần nhất
    private long lastFrameTs = -1L;

    // ====== Generic rule instance ======
    private final GenericRepRule genericRepRule = new GenericRepRule();

    public PoseAnalyzer(
            ExerciseType exerciseType,
            boolean isHoldExercise,
            Strictness strictness,
            int targetReps,
            int targetHoldSeconds
    ) {
        this.exerciseType = exerciseType;
        this.isHold = isHoldExercise;
        this.strictness = strictness;
        this.targetReps = targetReps;
        this.targetHoldSeconds = targetHoldSeconds;
    }

    /**
     * Hàm chính: gọi mỗi khi PoseLandmarker trả result.
     * timestampMs: thời gian (SystemClock.uptimeMillis()).
     */
    public PoseFeedback process(PoseLandmarkerResult result, long timestampMs) {
        if (result == null || result.landmarks() == null || result.landmarks().isEmpty()) {
            return smooth(new PoseFeedback(0, "No pose", reps, false, holdMillis, false));
        }

        // Switch theo bài tập
        switch (exerciseType) {
            case SQUAT:
                return smooth(processSquat(result, timestampMs));

            case PLANK:
                return smooth(processPlank(result, timestampMs));

            // HOLD khác (SIDE_PLANK, WALL_SIT) có thể dùng chung logic "hold generic"
            // để bạn đỡ viết nhiều.
            case SIDE_PLANK:
            case WALL_SIT:
                return smooth(processHoldGeneric(result, timestampMs, exerciseType));

            // Còn lại coi là REPS generic
            default:
                return smooth(processGenericReps(result, timestampMs, exerciseType));
        }
    }

    // =============================================================================================
    //  SQUAT (giữ riêng) - rule tương đối “đủ dùng” + ổn định + ít nhảy score
    // =============================================================================================
    private PoseFeedback processSquat(PoseLandmarkerResult r, long tMs) {
        // Landmarks cần
        NormalizedLandmark lh = AngleUtils.lm(r, AngleUtils.LEFT_HIP);
        NormalizedLandmark lk = AngleUtils.lm(r, AngleUtils.LEFT_KNEE);
        NormalizedLandmark la = AngleUtils.lm(r, AngleUtils.LEFT_ANKLE);

        NormalizedLandmark rh = AngleUtils.lm(r, AngleUtils.RIGHT_HIP);
        NormalizedLandmark rk = AngleUtils.lm(r, AngleUtils.RIGHT_KNEE);
        NormalizedLandmark ra = AngleUtils.lm(r, AngleUtils.RIGHT_ANKLE);

        NormalizedLandmark ls = AngleUtils.lm(r, AngleUtils.LEFT_SHOULDER);
        NormalizedLandmark rs = AngleUtils.lm(r, AngleUtils.RIGHT_SHOULDER);

        float minVis = 0.5f;

        boolean okLegs =
                AngleUtils.ok(lh, minVis) && AngleUtils.ok(lk, minVis) && AngleUtils.ok(la, minVis) &&
                        AngleUtils.ok(rh, minVis) && AngleUtils.ok(rk, minVis) && AngleUtils.ok(ra, minVis);

        if (!okLegs) {
            return new PoseFeedback(0, "Step back / show full legs", reps, false, holdMillis, false);
        }

        // Góc gối trái/phải (hip-knee-ankle)
        float leftKnee = AngleUtils.angleDeg(lh, lk, la);
        float rightKnee = AngleUtils.angleDeg(rh, rk, ra);
        float knee = (leftKnee + rightKnee) * 0.5f;

        // Ngưỡng theo strictness (có thể chỉnh thêm sau)
        float downTh; // xuống đủ sâu
        float upTh;   // lên đủ thẳng
        long debounceMs;

        switch (strictness) {
            case EASY:
                downTh = 115f;
                upTh = 165f;
                debounceMs = 350;
                break;
            case HARD:
                downTh = 105f;
                upTh = 172f;
                debounceMs = 500;
                break;
            case NORMAL:
            default:
                downTh = 110f;
                upTh = 168f;
                debounceMs = 420;
                break;
        }

        boolean repCompleted = false;

        // State machine: UP -> DOWN khi gối gập đủ (knee <= downTh)
        if (squatState == SquatState.UP) {
            if (knee <= downTh) {
                squatState = SquatState.DOWN;
            }
        } else { // DOWN -> UP khi duỗi lại (knee >= upTh) => +1 rep
            if (knee >= upTh) {
                if (tMs - lastRepMs >= debounceMs) {
                    reps += 1;
                    repCompleted = true;
                    lastRepMs = tMs;
                }
                squatState = SquatState.UP;
            }
        }

        // Score: dựa độ sâu + “thẳng người” đơn giản
        // - depthScore: gối càng nhỏ (đến ~90-100) càng tốt nhưng không bắt quá sâu.
        float depthScore = 0f;
        // knee 180: đứng thẳng; knee 90: rất sâu
        // ta map knee từ [180..95] -> [0..100]
        depthScore = (180f - knee) / (180f - 95f) * 100f;
        depthScore = AngleUtils.clamp(depthScore, 0f, 100f);

        // “lưng thẳng” rất đơn giản: vai-hip-ankle gần 180
        float torsoLeft = 0f, torsoRight = 0f;
        if (AngleUtils.ok(ls, minVis) && AngleUtils.ok(lh, minVis) && AngleUtils.ok(la, minVis)) {
            torsoLeft = AngleUtils.angleDeg(ls, lh, la);
        }
        if (AngleUtils.ok(rs, minVis) && AngleUtils.ok(rh, minVis) && AngleUtils.ok(ra, minVis)) {
            torsoRight = AngleUtils.angleDeg(rs, rh, ra);
        }
        float torso = (torsoLeft > 0 && torsoRight > 0) ? (torsoLeft + torsoRight) * 0.5f
                : (torsoLeft > 0 ? torsoLeft : torsoRight);

        float torsoPenalty = 0f;
        String msg = "Good";
        if (torso > 0f) {
            float diff = Math.abs(180f - torso);
            // diff 0..30 => penalty 0..40
            torsoPenalty = AngleUtils.clamp(diff / 30f * 40f, 0f, 40f);
            if (diff > 20f) msg = "Keep chest up";
        }

        int score = Math.round(0.7f * depthScore + 0.3f * 100f - torsoPenalty);
        score = AngleUtils.clampInt(score, 0, 100);

        // Nếu chưa vào form (đứng thẳng lâu), score đỡ nhảy:
        // Khi đang UP và knee gần 180 thì hiển thị score nền 60~70.
        if (squatState == SquatState.UP && knee > 165f) {
            score = Math.max(score, 65);
            if ("Good".equals(msg)) msg = "Ready";
        }

        boolean holdCompleted = false; // squat là reps
        return new PoseFeedback(score, msg, reps, repCompleted, holdMillis, holdCompleted);
    }

    // =============================================================================================
    //  PLANK (bổ sung riêng) - HOLD: chỉ cộng thời gian khi form >= ngưỡng ổn định
    // =============================================================================================
    private PoseFeedback processPlank(PoseLandmarkerResult r, long tMs) {
        NormalizedLandmark ls = AngleUtils.lm(r, AngleUtils.LEFT_SHOULDER);
        NormalizedLandmark rs = AngleUtils.lm(r, AngleUtils.RIGHT_SHOULDER);
        NormalizedLandmark lh = AngleUtils.lm(r, AngleUtils.LEFT_HIP);
        NormalizedLandmark rh = AngleUtils.lm(r, AngleUtils.RIGHT_HIP);
        NormalizedLandmark la = AngleUtils.lm(r, AngleUtils.LEFT_ANKLE);
        NormalizedLandmark ra = AngleUtils.lm(r, AngleUtils.RIGHT_ANKLE);

        float minVis = 0.5f;
        boolean okBody =
                AngleUtils.ok(ls, minVis) && AngleUtils.ok(lh, minVis) && AngleUtils.ok(la, minVis) &&
                        AngleUtils.ok(rs, minVis) && AngleUtils.ok(rh, minVis) && AngleUtils.ok(ra, minVis);

        if (!okBody) {
            lastFrameTs = tMs;
            lastGoodHoldMs = -1L;
            return new PoseFeedback(0, "Show full body", reps, false, holdMillis, false);
        }

        // Góc thân người: shoulder-hip-ankle ~ 180 (càng thẳng càng tốt)
        float leftLine = AngleUtils.angleDeg(ls, lh, la);
        float rightLine = AngleUtils.angleDeg(rs, rh, ra);
        float line = (leftLine + rightLine) * 0.5f;

        float diff = Math.abs(180f - line);

        // Strictness config
        float maxDiffOk;
        int minScoreToCount;
        switch (strictness) {
            case EASY:
                maxDiffOk = 25f;
                minScoreToCount = 70;
                break;
            case HARD:
                maxDiffOk = 15f;
                minScoreToCount = 85;
                break;
            case NORMAL:
            default:
                maxDiffOk = 20f;
                minScoreToCount = 78;
                break;
        }

        // Score từ độ thẳng: diff 0 => 100; diff maxDiffOk => ~70; diff lớn => tụt
        float scoreF = 100f - (diff / 45f) * 100f; // diff 45 => 0
        scoreF = AngleUtils.clamp(scoreF, 0f, 100f);

        String msg = "Good plank";
        if (diff > maxDiffOk) {
            msg = "Keep body straight";
        }

        int score = Math.round(scoreF);

        // Hold time chỉ tăng khi score >= minScoreToCount liên tục
        if (lastFrameTs < 0) lastFrameTs = tMs;
        long dt = Math.max(0L, tMs - lastFrameTs);
        lastFrameTs = tMs;

        if (score >= minScoreToCount) {
            if (lastGoodHoldMs < 0) lastGoodHoldMs = tMs;
            holdMillis += dt;
        } else {
            // rớt form => reset “chuỗi tốt” để tránh ăn gian bằng rung nhẹ
            lastGoodHoldMs = -1L;
        }

        boolean holdCompleted = false;
        if (targetHoldSeconds > 0 && holdMillis >= targetHoldSeconds * 1000L) {
            holdCompleted = true;
            msg = "Completed!";
        }

        return new PoseFeedback(score, msg, reps, false, holdMillis, holdCompleted);
    }

    // =============================================================================================
    //  HOLD generic cho SIDE_PLANK / WALL_SIT (đỡ phải viết nhiều)
    // =============================================================================================
    private PoseFeedback processHoldGeneric(PoseLandmarkerResult r, long tMs, ExerciseType type) {
        // Generic hold: chỉ dựa “đứng yên đủ rõ” + visibility
        // (Bạn có thể thay bằng rule riêng sau)

        float minVis = 0.5f;
        NormalizedLandmark lh = AngleUtils.lm(r, AngleUtils.LEFT_HIP);
        NormalizedLandmark rh = AngleUtils.lm(r, AngleUtils.RIGHT_HIP);
        NormalizedLandmark ls = AngleUtils.lm(r, AngleUtils.LEFT_SHOULDER);
        NormalizedLandmark rs = AngleUtils.lm(r, AngleUtils.RIGHT_SHOULDER);

        if (!(AngleUtils.ok(lh, minVis) && AngleUtils.ok(rh, minVis) && AngleUtils.ok(ls, minVis) && AngleUtils.ok(rs, minVis))) {
            lastFrameTs = tMs;
            return new PoseFeedback(0, "Show upper body", reps, false, holdMillis, false);
        }

        // Score “nhẹ”: nếu visible tốt => 80..95
        int score = 88;
        String msg = type == ExerciseType.WALL_SIT ? "Hold wall-sit" : "Hold position";

        // cộng thời gian nếu visible tốt (đơn giản)
        if (lastFrameTs < 0) lastFrameTs = tMs;
        long dt = Math.max(0L, tMs - lastFrameTs);
        lastFrameTs = tMs;

        holdMillis += dt;

        boolean holdCompleted = false;
        if (targetHoldSeconds > 0 && holdMillis >= targetHoldSeconds * 1000L) {
            holdCompleted = true;
            msg = "Completed!";
        }

        return new PoseFeedback(score, msg, reps, false, holdMillis, holdCompleted);
    }

    // =============================================================================================
    //  Generic REP rule cho nhiều bài: PUSH_UP, JUMPING_JACK, LUNGE, ...
    // =============================================================================================
    private PoseFeedback processGenericReps(PoseLandmarkerResult r, long tMs, ExerciseType type) {
        GenericRepRule.Signal signal = GenericRepRule.Signal.HIP_HEIGHT; // default

        // Chọn “tín hiệu chuyển động” đơn giản theo bài
        switch (type) {
            case PUSH_UP:
            case TRICEP_DIP:
                signal = GenericRepRule.Signal.ELBOW_ANGLE;
                break;

            case BICEP_CURL:
            case SHOULDER_PRESS:
                signal = GenericRepRule.Signal.WRIST_SHOULDER_DIST;
                break;

            case HIGH_KNEES:
            case MOUNTAIN_CLIMBER:
            case BICYCLE_CRUNCH:
                signal = GenericRepRule.Signal.KNEE_LIFT;
                break;

            case JUMPING_JACK:
                signal = GenericRepRule.Signal.LEG_SPREAD;
                break;

            case BURPEE:
            case DEADLIFT:
            case LUNGE:
            default:
                signal = GenericRepRule.Signal.HIP_HEIGHT;
                break;
        }

        GenericRepRule.Result gr = genericRepRule.update(r, tMs, signal, strictness);

        // đồng bộ reps
        if (gr.repCompleted) reps += 1;

        // Nếu bạn đang chạy mode HOLD mà lại gọi generic reps => vẫn trả reps,
        // nhưng CameraWorkoutActivity của bạn sẽ check theo isHoldExercise.
        // Nên bài reps/hold phải set đúng từ ExerciseDetail.
        boolean holdCompleted = false;

        return new PoseFeedback(gr.score, gr.message, reps, gr.repCompleted, holdMillis, holdCompleted);
    }

    // =============================================================================================
    //  Smooth score để HUD không nhảy loạn (rất quan trọng khi “chưa vào form”)
    // =============================================================================================
    private PoseFeedback smooth(PoseFeedback raw) {
        // EMA cho score
        float alpha;
        switch (strictness) {
            case HARD: alpha = 0.18f; break;
            case EASY: alpha = 0.25f; break;
            case NORMAL:
            default: alpha = 0.22f; break;
        }

        if (!emaInit) {
            scoreEma = raw.getScore();
            emaInit = true;
        } else {
            scoreEma = AngleUtils.lerp(scoreEma, raw.getScore(), alpha);
        }

        int smoothScore = AngleUtils.clampInt(Math.round(scoreEma), 0, 100);

        return new PoseFeedback(
                smoothScore,
                raw.getErrorMessage(),
                raw.getReps(),
                raw.isRepCompleted(),
                raw.getHoldMillis(),
                raw.isHoldCompleted()
        );
    }

    // =============================================================================================
    //  GenericRepRule: 1 class dùng chung cho nhiều bài REP (đủ dùng)
    // =============================================================================================
    private static class GenericRepRule {

        enum Phase { UP, DOWN }

        enum Signal {
            HIP_HEIGHT,            // lên/xuống theo hip (burpee/deadlift/lunge…)
            ELBOW_ANGLE,           // pushup/dip
            KNEE_LIFT,             // high knees / mountain climber / bicycle crunch
            LEG_SPREAD,            // jumping jack
            WRIST_SHOULDER_DIST    // curl/press: cổ tay lên gần vai/qua vai
        }

        static class Result {
            int score;
            String message;
            boolean repCompleted;
        }

        private Phase phase = Phase.UP;
        private long lastSwitchMs = 0L;

        // EMA cho signal để chống rung
        private float sigEma = 0f;
        private boolean sigInit = false;

        Result update(PoseLandmarkerResult r, long tMs, Signal signal, Strictness strictness) {
            Result out = new Result();
            out.repCompleted = false;
            out.message = "Good";

            // thresholds theo strictness
            long debounceMs;
            float downTh;
            float upTh;

            switch (strictness) {
                case EASY:
                    debounceMs = 260;
                    downTh = 0.60f;
                    upTh = 0.40f;
                    break;
                case HARD:
                    debounceMs = 420;
                    downTh = 0.68f;
                    upTh = 0.32f;
                    break;
                case NORMAL:
                default:
                    debounceMs = 330;
                    downTh = 0.64f;
                    upTh = 0.36f;
                    break;
            }

            // signalValue chuẩn hoá về 0..1 (mỗi signal có cách tính riêng)
            float v = computeSignal01(r, signal);

            // EMA làm mượt signal => giảm nhảy số rep sai
            float alpha = 0.25f;
            if (!sigInit) {
                sigEma = v;
                sigInit = true;
            } else {
                sigEma = AngleUtils.lerp(sigEma, v, alpha);
            }
            v = sigEma;

            // Rep state machine:
            // - UP -> DOWN khi v >= downTh
            // - DOWN -> UP khi v <= upTh => +1 rep
            if (phase == Phase.UP) {
                if (v >= downTh && (tMs - lastSwitchMs) >= debounceMs) {
                    phase = Phase.DOWN;
                    lastSwitchMs = tMs;
                }
            } else {
                if (v <= upTh && (tMs - lastSwitchMs) >= debounceMs) {
                    phase = Phase.UP;
                    lastSwitchMs = tMs;
                    out.repCompleted = true;
                }
            }

            // Score đơn giản + ổn định:
            // - Visible tốt => nền ~80
            // - Nếu signal dao động hợp lý => 80..95
            // - Nếu không thấy landmark => tụt và báo message
            int base = 82;
            int bonus = Math.round(15f * (1f - Math.abs(0.5f - v) * 2f)); // gần 0.5 => bonus cao
            out.score = AngleUtils.clampInt(base + bonus, 0, 100);

            // Nếu v quá “bất thường” (đứng im, chưa vào form) => báo Ready và giữ score nền
            if (Math.abs(v - 0.5f) < 0.05f) {
                out.message = "Ready";
                out.score = Math.max(out.score, 70);
            }

            return out;
        }

        private float computeSignal01(PoseLandmarkerResult r, Signal s) {
            float minVis = 0.5f;

            NormalizedLandmark ls = AngleUtils.lm(r, AngleUtils.LEFT_SHOULDER);
            NormalizedLandmark rs = AngleUtils.lm(r, AngleUtils.RIGHT_SHOULDER);
            NormalizedLandmark lh = AngleUtils.lm(r, AngleUtils.LEFT_HIP);
            NormalizedLandmark rh = AngleUtils.lm(r, AngleUtils.RIGHT_HIP);
            NormalizedLandmark lk = AngleUtils.lm(r, AngleUtils.LEFT_KNEE);
            NormalizedLandmark rk = AngleUtils.lm(r, AngleUtils.RIGHT_KNEE);
            NormalizedLandmark la = AngleUtils.lm(r, AngleUtils.LEFT_ANKLE);
            NormalizedLandmark ra = AngleUtils.lm(r, AngleUtils.RIGHT_ANKLE);
            NormalizedLandmark le = AngleUtils.lm(r, AngleUtils.LEFT_ELBOW);
            NormalizedLandmark re = AngleUtils.lm(r, AngleUtils.RIGHT_ELBOW);
            NormalizedLandmark lw = AngleUtils.lm(r, AngleUtils.LEFT_WRIST);
            NormalizedLandmark rw = AngleUtils.lm(r, AngleUtils.RIGHT_WRIST);

            // fallback an toàn
            if (s == Signal.HIP_HEIGHT) {
                if (!(AngleUtils.ok(lh, minVis) && AngleUtils.ok(rh, minVis) && AngleUtils.ok(la, minVis) && AngleUtils.ok(ra, minVis)))
                    return 0.5f;

                // hipY càng lớn => người càng “thấp” (do y normalized: 0 top, 1 bottom)
                float hipY = (AngleUtils.y(lh) + AngleUtils.y(rh)) * 0.5f;
                float ankleY = (AngleUtils.y(la) + AngleUtils.y(ra)) * 0.5f;

                // normalize: (hipY gần ankleY) => thấp => v cao
                float raw = (hipY - 0.2f) / (ankleY - 0.2f + 1e-6f);
                return AngleUtils.clamp(raw, 0f, 1f);
            }

            if (s == Signal.ELBOW_ANGLE) {
                // dùng góc elbow trung bình (shoulder-elbow-wrist), map về 0..1:
                // 180 (thẳng) => 0, 60 (gập) => 1
                if (!(AngleUtils.ok(ls, minVis) && AngleUtils.ok(le, minVis) && AngleUtils.ok(lw, minVis) &&
                        AngleUtils.ok(rs, minVis) && AngleUtils.ok(re, minVis) && AngleUtils.ok(rw, minVis)))
                    return 0.5f;

                float aL = AngleUtils.angleDeg(ls, le, lw);
                float aR = AngleUtils.angleDeg(rs, re, rw);
                float a = (aL + aR) * 0.5f;

                float v = (180f - a) / (180f - 60f);
                return AngleUtils.clamp(v, 0f, 1f);
            }

            if (s == Signal.KNEE_LIFT) {
                // knee càng cao (y nhỏ) => v cao
                if (!(AngleUtils.ok(lk, minVis) && AngleUtils.ok(rk, minVis) && AngleUtils.ok(lh, minVis) && AngleUtils.ok(rh, minVis)))
                    return 0.5f;

                float kneeY = (AngleUtils.y(lk) + AngleUtils.y(rk)) * 0.5f;
                float hipY = (AngleUtils.y(lh) + AngleUtils.y(rh)) * 0.5f;

                // nếu kneeY < hipY nhiều => nâng cao => v cao
                float diff = (hipY - kneeY); // càng lớn càng tốt
                // map diff khoảng [0..0.35] => [0..1]
                float v = diff / 0.35f;
                return AngleUtils.clamp(v, 0f, 1f);
            }

            if (s == Signal.LEG_SPREAD) {
                // khoảng cách 2 ankle càng xa => v cao
                if (!(AngleUtils.ok(la, minVis) && AngleUtils.ok(ra, minVis) && AngleUtils.ok(lh, minVis) && AngleUtils.ok(rh, minVis)))
                    return 0.5f;

                float ankleDx = Math.abs(AngleUtils.x(la) - AngleUtils.x(ra));
                float hipDx = Math.abs(AngleUtils.x(lh) - AngleUtils.x(rh)) + 1e-6f;

                float ratio = ankleDx / hipDx; // >1 là chân mở rộng
                // map ratio [0.8..2.2] => [0..1]
                float v = (ratio - 0.8f) / (2.2f - 0.8f);
                return AngleUtils.clamp(v, 0f, 1f);
            }

            if (s == Signal.WRIST_SHOULDER_DIST) {
                // cổ tay lên gần vai => v cao (curl/press)
                if (!(AngleUtils.ok(lw, minVis) && AngleUtils.ok(rw, minVis) && AngleUtils.ok(ls, minVis) && AngleUtils.ok(rs, minVis)))
                    return 0.5f;

                float dL = AngleUtils.dist(lw, ls);
                float dR = AngleUtils.dist(rw, rs);
                float d = (dL + dR) * 0.5f;

                // d nhỏ => v cao. map d [0.35..0.05] => [0..1]
                float v = (0.35f - d) / (0.35f - 0.05f);
                return AngleUtils.clamp(v, 0f, 1f);
            }

            return 0.5f;
        }
    }
}
