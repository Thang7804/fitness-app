package com.app.aifitness.util;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

/**
 * Các hàm tiện ích để tính góc (độ) giữa 3 điểm.
 *
 * Tất cả góc trả về trong khoảng [0, 180].
 */
public final class AngleUtils {

    private AngleUtils() {
        // No instance
    }

    /**
     * Tính góc (độ) tại điểm B trong tam giác A-B-C.
     *
     * @param a landmark A
     * @param b landmark B (đỉnh góc)
     * @param c landmark C
     * @return góc ABC tính bằng độ [0, 180]
     */
    public static double angleFromLandmarks(NormalizedLandmark a,
                                            NormalizedLandmark b,
                                            NormalizedLandmark c) {
        if (a == null || b == null || c == null) {
            return 0.0;
        }

        double bax = a.x() - b.x();
        double bay = a.y() - b.y();
        double baz = a.z() - b.z();

        double bcx = c.x() - b.x();
        double bcy = c.y() - b.y();
        double bcz = c.z() - b.z();

        double dot = bax * bcx + bay * bcy + baz * bcz;
        double magBA = Math.sqrt(bax * bax + bay * bay + baz * baz);
        double magBC = Math.sqrt(bcx * bcx + bcy * bcy + bcz * bcz);

        if (magBA == 0.0 || magBC == 0.0) {
            return 0.0;
        }

        double cosTheta = dot / (magBA * magBC);
        cosTheta = clamp(cosTheta, -1.0, 1.0);

        return Math.toDegrees(Math.acos(cosTheta));
    }

    /**
     * Clamp giá trị vào đoạn [min, max].
     */
    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
