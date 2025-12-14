package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.Exercise;
import com.app.aifitness.R;
import com.app.aifitness.workout.CameraWorkoutActivity;
import com.google.android.material.button.MaterialButton;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExerciseDetail extends AppCompatActivity {

    private TextView tvExerciseName, tvExerciseId, tvDescription, tvCameraAngle, tvCalories, tvDuration;
    private WebView webViewYoutube;
    private MaterialButton btnStartExercise;
    private TextView btnBack;

    // scheduleType từ Firestore (time/reps) -> KHÔNG dùng để quyết định mode nữa
    private String scheduleType;
    private int exerciseValue;
    private String exerciseId;

    private String dayName; // nếu bạn có truyền ngày qua intent thì giữ lại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        initViews();

        dayName = getIntent().getStringExtra("dayName");

        Serializable data = getIntent().getSerializableExtra("exerciseData");
        if (data == null) {
            Toast.makeText(this, "Missing exerciseData", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Map<String, Object> exerciseMap = (Map<String, Object>) data;
        exerciseId = (String) exerciseMap.get("id");
        scheduleType = (String) exerciseMap.get("type"); // time / reps (có thể sai)
        Object value = exerciseMap.get("value");

        if (value instanceof Long) {
            exerciseValue = ((Long) value).intValue();
        } else if (value instanceof Integer) {
            exerciseValue = (Integer) value;
        } else {
            exerciseValue = 0;
        }

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseDetail.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        setupWebView();

        loadExerciseDetails();
    }

    private void initViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName);
        tvExerciseId = findViewById(R.id.tvExerciseId);
        tvDescription = findViewById(R.id.tvDescription);
        tvCameraAngle = findViewById(R.id.tvCameraAngle);
        tvCalories = findViewById(R.id.tvCalories);
        tvDuration = findViewById(R.id.tvDuration);
        webViewYoutube = findViewById(R.id.webViewYoutube);
        btnStartExercise = findViewById(R.id.btnStartExercise);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupWebView() {
        WebSettings settings = webViewYoutube.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webViewYoutube.setWebViewClient(new WebViewClient());
    }

    private void loadExerciseDetails() {
        FirebaseHelper.getInstance().getExerciseById(exerciseId, new DataCallBack<Exercise>() {
            @Override
            public void onSuccess(Exercise exercise) {
                if (exercise == null) {
                    Toast.makeText(ExerciseDetail.this, "Exercise not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                tvExerciseName.setText(exercise.name);
                tvExerciseId.setText("ID: " + exercise.id);
                tvDescription.setText(exercise.description);
                tvCameraAngle.setText("Camera: " + exercise.cameraSide);
                tvCalories.setText("Calories/min: " + exercise.caloriesPerMin);

                // ====== QUYẾT ĐỊNH MODE THEO LOẠI BÀI (KHÔNG THEO scheduleType time/reps) ======
                String exerciseTypeEnum = mapToExerciseTypeEnum(exercise.id, exercise.name);
                boolean isHoldMode = isHoldByExerciseType(exerciseTypeEnum);

                // Hiển thị đúng label
                String targetText = isHoldMode
                        ? "Time: " + exerciseValue + " sec"
                        : "Reps: " + exerciseValue;
                tvDuration.setText(targetText);

                // Load video youtube
                if (exercise.videoUrl != null && !exercise.videoUrl.isEmpty()) {
                    String videoId = extractYoutubeId(exercise.videoUrl);
                    if (!videoId.isEmpty()) {
                        String html = "<html><body style='margin:0;padding:0;'>" +
                                "<iframe width='100%' height='100%' " +
                                "src='https://www.youtube.com/embed/" + videoId + "?autoplay=0&modestbranding=1&rel=0' " +
                                "frameborder='0' allowfullscreen></iframe>" +
                                "</body></html>";
                        webViewYoutube.loadData(html, "text/html", "utf-8");
                    }
                }

                // ====== START CAMERA AI ======
                btnStartExercise.setOnClickListener(v -> {
                    Intent intent = new Intent(ExerciseDetail.this, CameraWorkoutActivity.class);

                    intent.putExtra("exercise_display_name", exercise.name != null ? exercise.name : "Workout");
                    intent.putExtra("exercise_type", exerciseTypeEnum);

                    // Bạn muốn bỏ Home/History -> strictness cứ fix cứng hoặc lấy theo UI sau này
                    intent.putExtra("strictness", "NORMAL");

                    if (dayName != null) intent.putExtra("dayName", dayName);

                    // mode
                    intent.putExtra("exercise_mode", isHoldMode ? "HOLD" : "REPS");

                    // target
                    if (isHoldMode) {
                        intent.putExtra("target_hold_seconds", Math.max(exerciseValue, 0));
                        intent.putExtra("target_reps", 0);
                    } else {
                        // Squat đang bị "12 sec" ở firestore -> vẫn chạy 12 REPS theo ý bạn
                        intent.putExtra("target_reps", Math.max(exerciseValue, 0));
                        intent.putExtra("target_hold_seconds", 0);
                    }

                    // (tuỳ bạn) gửi scheduleType để debug
                    intent.putExtra("schedule_type_raw", scheduleType);

                    startActivity(intent);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ExerciseDetail.this, "Failed to load exercise: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // ================= HELPERS =================

    /**
     * Quyết định loại bài để AI xử lý (enum string).
     * - Ưu tiên theo name (vì firestore scheduleType hay bị sai)
     * - Fallback theo id nếu name null
     */
    private String mapToExerciseTypeEnum(String id, String name) {
        String s = "";
        if (name != null) s = name.toLowerCase(Locale.ROOT);
        else if (id != null) s = id.toLowerCase(Locale.ROOT);

        // HOLD
        if (s.contains("plank")) return "PLANK";
        if (s.contains("side plank")) return "PLANK"; // nếu bạn muốn chung PLANK

        // REPS
        if (s.contains("squat")) return "SQUAT";
        if (s.contains("push") || s.contains("push-up") || s.contains("push up")) return "PUSH_UP";
        if (s.contains("lunge")) return "LUNGE";
        if (s.contains("situp") || s.contains("sit-up") || s.contains("sit up")) return "SIT_UP";
        if (s.contains("burpee")) return "BURPEE";
        if (s.contains("jumping jack")) return "JUMPING_JACK";

        // default: cứ coi là bài reps
        return "SQUAT";
    }

    /**
     * Chỉ PLANK là HOLD (time). Bạn muốn thêm bài HOLD khác thì thêm vào đây.
     */
    private boolean isHoldByExerciseType(String exerciseTypeEnum) {
        if (exerciseTypeEnum == null) return false;
        return "PLANK".equalsIgnoreCase(exerciseTypeEnum);
    }

    private String extractYoutubeId(String url) {
        if (url == null || url.isEmpty()) return "";
        String pattern = "(?<=v=|/videos/|embed/|youtu.be/)[^#&?]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        return matcher.find() ? matcher.group() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webViewYoutube != null) {
            webViewYoutube.destroy();
        }
    }
}
