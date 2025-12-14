package com.app.aifitness.pose;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Vẽ skeleton từ PoseLandmarkerResult.
 *
 * - Chỉ dùng một số điểm quan trọng:
 *   head(0), shoulders(11,12), elbows(13,14), wrists(15,16),
 *   hips(23,24), knees(25,26), ankles(27,28)
 * - Vẽ connections thân–tay–chân tối giản như app fitness.
 * - Hỗ trợ xoay theo rotationDegrees từ CameraX.
 */
public class PoseOverlayView extends View {

    private final Paint pointPaint = new Paint();
    private final Paint linePaint = new Paint();

    private final List<NormalizedLandmark> currentLandmarks = new ArrayList<>();
    private PoseLandmarkerResult currentResult;

    // Các landmark quan trọng (index MediaPipe Pose)
    private static final int[] IMPORTANT_LANDMARKS = new int[]{
            0,      // head (nose)
            11, 12, // shoulders
            13, 14, // elbows
            15, 16, // wrists
            23, 24, // hips
            25, 26, // knees
            27, 28  // ankles
    };

    private static final Set<Integer> IMPORTANT_SET = new HashSet<>();

    static {
        for (int idx : IMPORTANT_LANDMARKS) {
            IMPORTANT_SET.add(idx);
        }
    }

    // Các đường nối thân–tay–chân
    private static final int[][] POSE_CONNECTIONS = new int[][]{
            // thân trên
            {11, 12},     // hai vai
            {11, 23},     // vai trái -> hông trái
            {12, 24},     // vai phải -> hông phải
            {23, 24},     // hai hông

            // tay trái
            {11, 13},
            {13, 15},

            // tay phải
            {12, 14},
            {14, 16},

            // chân trái
            {23, 25},
            {25, 27},

            // chân phải
            {24, 26},
            {26, 28},

            // đầu -> vai
            {0, 11},
            {0, 12}
    };

    // Xoay skeleton theo rotation của ảnh (0 / 90 / 180 / 270)
    private int imageRotationDegrees = 0;

    // Mirror ngang (nếu dùng camera trước, có thể bật)
    private boolean mirrorHorizontally = false;

    public PoseOverlayView(Context context) {
        super(context);
        init();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        pointPaint.setColor(Color.GREEN);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeWidth(10f);
        pointPaint.setAntiAlias(true);

        linePaint.setColor(Color.GREEN);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(8f);
        linePaint.setAntiAlias(true);
    }

    /**
     * Gọi từ Activity để set rotation từ CameraX (imageInfo.getRotationDegrees()).
     */
    public void setImageRotationDegrees(int degrees) {
        int d = degrees % 360;
        if (d < 0) {
            d += 360;
        }
        this.imageRotationDegrees = d;
        invalidate();
    }

    /**
     * Bật/tắt mirror ngang (nếu dùng camera trước / selfie).
     */
    public void setMirrorHorizontally(boolean mirror) {
        this.mirrorHorizontally = mirror;
        invalidate();
    }

    /**
     * Cập nhật kết quả pose mới nhất để vẽ.
     */
    public void setResults(@Nullable PoseLandmarkerResult result) {
        currentResult = result;
        currentLandmarks.clear();

        if (result != null && !result.landmarks().isEmpty()) {
            List<NormalizedLandmark> firstPose = result.landmarks().get(0);
            currentLandmarks.addAll(firstPose);
        }

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentLandmarks.isEmpty()) {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Vẽ line connections
        float[] p = new float[2];
        float[] q = new float[2];

        for (int[] connection : POSE_CONNECTIONS) {
            int startIndex = connection[0];
            int endIndex = connection[1];

            if (!isValidIndex(startIndex) || !isValidIndex(endIndex)) {
                continue;
            }

            NormalizedLandmark start = currentLandmarks.get(startIndex);
            NormalizedLandmark end = currentLandmarks.get(endIndex);

            transformNormalizedPoint(start.x(), start.y(), p);
            transformNormalizedPoint(end.x(), end.y(), q);

            float startX = p[0] * viewWidth;
            float startY = p[1] * viewHeight;
            float endX = q[0] * viewWidth;
            float endY = q[1] * viewHeight;

            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }

        // Vẽ điểm (chỉ landmark quan trọng)
        for (int i = 0; i < currentLandmarks.size(); i++) {
            if (!IMPORTANT_SET.contains(i)) {
                continue;
            }
            NormalizedLandmark lm = currentLandmarks.get(i);

            transformNormalizedPoint(lm.x(), lm.y(), p);
            float cx = p[0] * viewWidth;
            float cy = p[1] * viewHeight;

            canvas.drawCircle(cx, cy, 8f, pointPaint);
        }
    }

    private boolean isValidIndex(int idx) {
        return idx >= 0 && idx < currentLandmarks.size();
    }

    /**
     * Biến đổi (x, y) normalized trong hệ trục ảnh -> hệ trục màn hình,
     * xét đến rotationDegrees và mirror ngang.
     */
    private void transformNormalizedPoint(float x, float y, float[] out) {
        float tx;
        float ty;

        switch (imageRotationDegrees) {
            case 90:
                // Ảnh cần xoay 90 độ CW để thẳng đứng
                tx = 1f - y;
                ty = x;
                break;
            case 180:
                tx = 1f - x;
                ty = 1f - y;
                break;
            case 270:
                tx = y;
                ty = 1f - x;
                break;
            case 0:
            default:
                tx = x;
                ty = y;
                break;
        }

        if (mirrorHorizontally) {
            tx = 1f - tx;
        }

        out[0] = tx;
        out[1] = ty;
    }
}
