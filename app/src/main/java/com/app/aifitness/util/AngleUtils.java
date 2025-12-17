package com.app.aifitness.util;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;

/**
 * Tiện ích tính toán hình học cơ bản cho pose.
 * - Tính góc 3 điểm (A-B-C) theo độ.
 * - Tính khoảng cách chuẩn hoá.
 * - Lấy landmark an toàn + kiểm tra visibility/presence.
 */
public class AngleUtils {

    // MediaPipe PoseLandmarker landmark indices (BlazePose)
    public static final int NOSE = 0;

    public static final int LEFT_SHOULDER = 11;
    public static final int RIGHT_SHOULDER = 12;

    public static final int LEFT_ELBOW = 13;
    public static final int RIGHT_ELBOW = 14;

    public static final int LEFT_WRIST = 15;
    public static final int RIGHT_WRIST = 16;

    public static final int LEFT_HIP = 23;
    public static final int RIGHT_HIP = 24;

    public static final int LEFT_KNEE = 25;
    public static final int RIGHT_KNEE = 26;

    public static final int LEFT_ANKLE = 27;
    public static final int RIGHT_ANKLE = 28;

    private AngleUtils() {}

    public static NormalizedLandmark lm(PoseLandmarkerResult r, int idx) {
        if (r == null) return null;
        List<List<NormalizedLandmark>> all = r.landmarks();
        if (all == null || all.isEmpty()) return null;
        List<NormalizedLandmark> one = all.get(0);
        if (one == null || idx < 0 || idx >= one.size()) return null;
        return one.get(idx);
    }

    /**
     * Landmark usable nếu visibility/presence đủ.
     * MediaPipe Tasks mới: visibility()/presence() trả về Optional<Float>.
     */
    public static boolean ok(NormalizedLandmark l, float minVis) {
        if (l == null) return false;

        // Nếu API bạn đang dùng có visibility/presence Optional:
        float vis = 1.0f;
        float pres = 1.0f;

        // Optional<Float> -> float
        try {
            vis = l.visibility().orElse(1.0f);
        } catch (Throwable ignored) {
            // nếu bản API khác không có visibility() hoặc signature khác -> giữ default 1.0
        }
        try {
            pres = l.presence().orElse(1.0f);
        } catch (Throwable ignored) {
            // nếu bản API khác không có presence() hoặc signature khác -> giữ default 1.0
        }

        return (vis >= minVis) && (pres >= minVis);
    }

    public static float x(NormalizedLandmark l) { return l == null ? 0f : l.x(); }
    public static float y(NormalizedLandmark l) { return l == null ? 0f : l.y(); }

    /** Khoảng cách Euclid trong toạ độ normalized (0..1). */
    public static float dist(NormalizedLandmark a, NormalizedLandmark b) {
        if (a == null || b == null) return 0f;
        float dx = x(a) - x(b);
        float dy = y(a) - y(b);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** Góc A-B-C (tại B) theo độ. */
    public static float angleDeg(NormalizedLandmark a, NormalizedLandmark b, NormalizedLandmark c) {
        if (a == null || b == null || c == null) return 0f;

        float bax = x(a) - x(b);
        float bay = y(a) - y(b);
        float bcx = x(c) - x(b);
        float bcy = y(c) - y(b);

        float dot = bax * bcx + bay * bcy;
        float mag1 = (float) Math.sqrt(bax * bax + bay * bay);
        float mag2 = (float) Math.sqrt(bcx * bcx + bcy * bcy);
        if (mag1 < 1e-6f || mag2 < 1e-6f) return 0f;

        float cos = dot / (mag1 * mag2);
        cos = clamp(cos, -1f, 1f);
        return (float) Math.toDegrees(Math.acos(cos));
    }

    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}