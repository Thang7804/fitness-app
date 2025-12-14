package com.app.aifitness.pose;

/**
 * Phát hiện lỗi kỹ thuật chính cho từng bài tập.
 *
 * Mục tiêu:
 *  - Chỉ trả về 1 lỗi quan trọng nhất (chuỗi ngắn).
 *  - Nếu form ổn -> trả về chuỗi rỗng "".
 */
public class ErrorDetector {

    public ErrorDetector() {
    }

    /**
     * Lỗi Squat dựa trên:
     *  - avgKneeAngle: góc gối trung bình
     *  - hipAngle: góc tại hông (knee-hip-shoulder)
     */
    public String detectSquatError(double avgKneeAngle,
                                   double hipAngle,
                                   StrictnessConfig config) {
        // Góc gối quá lớn -> chưa hạ đủ sâu
        if (avgKneeAngle > config.squatUpAngle - 10f) {
            return "Xuống thấp hơn một chút";
        }

        // Lưng gập quá nhiều
        double hipDeviation = Math.abs(180.0 - hipAngle);
        if (hipDeviation > 35.0) {
            return "Giữ lưng thẳng hơn";
        }

        // Nếu sâu quá (gối quá gập) có thể cảnh báo nhẹ
        if (avgKneeAngle < config.squatDownAngle - 20f) {
            return "Không cần xuống quá sâu";
        }

        return "";
    }

    /**
     * Lỗi Plank dựa trên:
     *  - bodyAngle: góc tại hông (shoulder-hip-ankle)
     */
    public String detectPlankError(double bodyAngle,
                                   StrictnessConfig config) {
        double deviation = Math.abs(180.0 - bodyAngle);

        // Hông xệ (thân cong xuống)
        if (bodyAngle < 180.0 - config.plankMaxHipDeviationDeg) {
            return "Hông bị xệ, siết bụng lên";
        }

        // Hông dựng cao (kiểu V ngược)
        if (bodyAngle > 180.0 + config.plankMaxHipDeviationDeg) {
            return "Hạ hông xuống cho thẳng thân";
        }

        // Nếu hơi lệch nhưng không quá nghiêm trọng
        if (deviation > config.plankMaxHipDeviationDeg / 2.0) {
            return "Giữ thân thành một đường thẳng";
        }

        return "";
    }

    /**
     * Nếu bài tập chưa hỗ trợ AI.
     */
    public String unsupportedExercise() {
        return "Bài này chưa hỗ trợ AI, hãy chọn Squat hoặc Plank";
    }
}
