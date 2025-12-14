package com.app.aifitness.workout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.app.aifitness.Activity.DayDetailActivity;
import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.databinding.ActivityCameraWorkoutBinding;
import com.app.aifitness.exercise.ExerciseType;
import com.app.aifitness.exercise.Strictness;
import com.app.aifitness.pose.PoseAnalyzer;
import com.app.aifitness.pose.PoseFeedback;
import com.app.aifitness.pose.PoseOverlayView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.OutputHandler;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.app.aifitness.workout.WorkoutSessionResult;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraWorkoutActivity extends AppCompatActivity {

    private static final String TAG = "CameraWorkoutActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private ActivityCameraWorkoutBinding binding;
    private PoseOverlayView poseOverlayView;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private PoseLandmarker poseLandmarker;

    private TextView tvExerciseName;
    private TextView tvRepsOrTime;
    private TextView tvScore;
    private TextView tvError;
    private ImageButton btnClose;

    private boolean isHoldExercise = false;

    private ExerciseType exerciseType = ExerciseType.SQUAT;
    private Strictness strictness = Strictness.NORMAL;
    private int targetReps = 0;
    private int targetHoldSeconds = 0;

    private PoseAnalyzer poseAnalyzer;
    private WorkoutHUDController hudController;

    // Info session
    private String exerciseDisplayName = "Workout";
    private String exerciseId; // Exercise ID từ schedule
    private int latestScore = 0;
    private int latestReps = 0;
    private long latestHoldMillis = 0L;
    private boolean latestHoldCompleted = false;

    private volatile int latestRotationDegrees = 0;

    private boolean sessionFinished = false;

    // để quay về DayDetailActivity
    private String dayName;

    // Track session time
    private long sessionStartTime = 0L;
    private String savedWorkoutSessionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraWorkoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        poseOverlayView = binding.poseOverlay;
        tvExerciseName = binding.tvExerciseName;
        tvRepsOrTime = binding.tvRepsOrTime;
        tvScore = binding.tvScore;
        tvError = binding.tvError;
        btnClose = binding.btnClose;

        // ==== GET INTENT DATA ====
        exerciseDisplayName = getIntent().getStringExtra("exercise_display_name");
        exerciseId = getIntent().getStringExtra("exercise_id"); // Exercise ID từ schedule
        String modeStr = getIntent().getStringExtra("exercise_mode");
        String typeStr = getIntent().getStringExtra("exercise_type");
        String strictStr = getIntent().getStringExtra("strictness");

        targetReps = getIntent().getIntExtra("target_reps", 0);
        targetHoldSeconds = getIntent().getIntExtra("target_hold_seconds", 0);

        dayName = getIntent().getStringExtra("dayName");

        if (exerciseDisplayName == null || exerciseDisplayName.trim().isEmpty()) {
            exerciseDisplayName = "Workout";
        }
        tvExerciseName.setText(exerciseDisplayName);

        isHoldExercise = "HOLD".equalsIgnoreCase(modeStr);

        if (typeStr != null) {
            try {
                exerciseType = ExerciseType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid exercise_type: " + typeStr, e);
                exerciseType = ExerciseType.SQUAT;
            }
        }

        if (strictStr != null) {
            try {
                strictness = Strictness.valueOf(strictStr);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid strictness: " + strictStr, e);
                strictness = Strictness.NORMAL;
            }
        }

        if (isHoldExercise) {
            tvRepsOrTime.setText("Time: 0 s");
        } else {
            tvRepsOrTime.setText("Reps: 0");
        }
        tvScore.setText("Score: --");
        tvError.setText("Ready");

        hudController = new WorkoutHUDController(
                tvExerciseName,
                tvRepsOrTime,
                tvScore,
                tvError,
                isHoldExercise
        );

        poseAnalyzer = new PoseAnalyzer(
                exerciseType,
                isHoldExercise,
                strictness,
                targetReps,
                targetHoldSeconds
        );

        // Close -> quay về danh sách bài tập trong ngày
        btnClose.setOnClickListener(v -> openDayDetailAndFinish(false));

        cameraExecutor = Executors.newSingleThreadExecutor();
        setupPoseLandmarker();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }

        // Track session start time
        sessionStartTime = System.currentTimeMillis();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void setupPoseLandmarker() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_full.task")
                    .build();

            PoseLandmarker.PoseLandmarkerOptions options =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.LIVE_STREAM)
                            .setMinPoseDetectionConfidence(0.5f)
                            .setMinPosePresenceConfidence(0.5f)
                            .setMinTrackingConfidence(0.5f)
                            .setNumPoses(1)
                            .setResultListener(
                                    new OutputHandler.ResultListener<PoseLandmarkerResult, MPImage>() {
                                        @Override
                                        public void run(PoseLandmarkerResult result, MPImage input) {
                                            long now = SystemClock.uptimeMillis();
                                            PoseFeedback feedback = null;

                                            try {
                                                if (poseAnalyzer != null) {
                                                    feedback = poseAnalyzer.process(result, now);
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "PoseAnalyzer error", e);
                                            }

                                            PoseFeedback finalFeedback = feedback;
                                            int rotation = latestRotationDegrees;

                                            runOnUiThread(() -> {
                                                poseOverlayView.setImageRotationDegrees(rotation);
                                                poseOverlayView.setResults(result);

                                                if (finalFeedback != null) {
                                                    if (hudController != null) {
                                                        hudController.update(finalFeedback);
                                                    }

                                                    latestScore = finalFeedback.getScore();
                                                    latestReps = finalFeedback.getReps();
                                                    latestHoldMillis = finalFeedback.getHoldMillis();
                                                    latestHoldCompleted = finalFeedback.isHoldCompleted();

                                                    checkAutoFinish();
                                                }
                                            });
                                        }
                                    }
                            )
                            .build();

            poseLandmarker = PoseLandmarker.createFromOptions(this, options);
            Log.d(TAG, "PoseLandmarker initialized successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing PoseLandmarker", e);
            Toast.makeText(this, "Failed to init pose model", Toast.LENGTH_LONG).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting camera provider", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (poseLandmarker == null || sessionFinished) {
            imageProxy.close();
            return;
        }

        try {
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();

            latestRotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

            ImageProxy.PlaneProxy plane = imageProxy.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            buffer.rewind();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            MPImage mpImage = new BitmapImageBuilder(bitmap).build();

            ImageProcessingOptions imageProcessingOptions =
                    ImageProcessingOptions.builder()
                            .setRotationDegrees(0)
                            .build();

            long timestampMs = SystemClock.uptimeMillis();
            poseLandmarker.detectAsync(mpImage, imageProcessingOptions, timestampMs);

        } catch (Exception e) {
            Log.e(TAG, "Error during pose detection", e);
        } finally {
            imageProxy.close();
        }
    }

    /**
     * Khi đủ target -> dừng camera -> quay về DayDetailActivity hoặc FeedbackActivity.
     * @param completed true nếu đủ target, false nếu user bấm close.
     */
    private void openDayDetailAndFinish(boolean completed) {
        if (sessionFinished) return;
        sessionFinished = true;

        // Lưu workout session vào Firebase nếu đã hoàn thành
        if (completed && sessionStartTime > 0) {
            saveWorkoutSession();
        }

        if (completed) {
            // Kiểm tra xem tất cả bài tập trong ngày đã hoàn thành chưa
            checkAllExercisesCompletedAndNavigate();
        } else {
            // Nếu không hoàn thành, quay về DayDetailActivity
            Intent intent = new Intent(CameraWorkoutActivity.this, DayDetailActivity.class);
            intent.putExtra("dayName", dayName);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }
    
    /**
     * Kiểm tra xem tất cả bài tập trong ngày đã hoàn thành chưa
     * Nếu đã hoàn thành tất cả -> chuyển đến FeedbackActivity để đánh giá
     * Nếu chưa -> quay về DayDetailActivity
     */
    private void checkAllExercisesCompletedAndNavigate() {
        if (dayName == null || dayName.isEmpty()) {
            // Không có dayName, quay về DayDetailActivity
            Intent intent = new Intent(CameraWorkoutActivity.this, DayDetailActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        // Lấy schedule để biết có bao nhiêu bài tập trong ngày
        FirebaseHelper.getInstance().getCurrentUser(
            FirebaseHelper.getInstance().getCurrentUserId(),
            new com.app.aifitness.Firebase.DataCallBack<com.app.aifitness.Model.User>() {
                @Override
                public void onSuccess(com.app.aifitness.Model.User user) {
                    if (user == null || user.schedule == null) {
                        // Không có schedule, quay về DayDetailActivity
                        navigateToDayDetail();
                        return;
                    }
                    
                    Map<String, Object> dayMap = user.schedule.get(dayName);
                    if (dayMap == null) {
                        navigateToDayDetail();
                        return;
                    }
                    
                    // Đếm số bài tập trong ngày (không tính rest)
                    Set<String> exerciseIds = new HashSet<>();
                    for (Map.Entry<String, Object> entry : dayMap.entrySet()) {
                        String key = entry.getKey();
                        if (!key.equals("rest") && entry.getValue() instanceof Map) {
                            exerciseIds.add(key);
                        }
                    }
                    
                    // Nếu là rest day hoặc không có bài tập, quay về DayDetailActivity
                    if (exerciseIds.isEmpty()) {
                        navigateToDayDetail();
                        return;
                    }
                    
                    // Lấy workout history để check bài tập nào đã hoàn thành
                    FirebaseHelper.getInstance().getWorkoutHistory(
                        new com.app.aifitness.Firebase.DataCallBack<List<WorkoutSessionResult>>() {
                            @Override
                            public void onSuccess(List<WorkoutSessionResult> history) {
                                if (history == null) {
                                    navigateToDayDetail();
                                    return;
                                }
                                
                                // Đếm số bài tập đã hoàn thành (completed=true)
                                Set<String> completedExerciseIds = new HashSet<>();
                                for (WorkoutSessionResult session : history) {
                                    if (session.isCompleted() && 
                                        dayName.equals(session.getDayName()) &&
                                        session.getExerciseId() != null &&
                                        exerciseIds.contains(session.getExerciseId())) {
                                        completedExerciseIds.add(session.getExerciseId());
                                    }
                                }
                                
                                // Chỉ chuyển đến FeedbackActivity khi TẤT CẢ bài tập đã hoàn thành
                                if (completedExerciseIds.size() >= exerciseIds.size()) {
                                    // Tất cả bài tập đã hoàn thành -> cho đánh giá
                                    Intent intent = new Intent(CameraWorkoutActivity.this, com.app.aifitness.Activity.FeedbackActivity.class);
                                    intent.putExtra("exercise_name", exerciseDisplayName);
                                    intent.putExtra("exercise_type", exerciseType.name());
                                    intent.putExtra("workout_session_id", savedWorkoutSessionId);
                                    intent.putExtra("dayName", dayName);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Chưa hoàn thành tất cả -> quay về DayDetailActivity
                                    navigateToDayDetail();
                                }
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                // Lỗi khi load history -> quay về DayDetailActivity
                                navigateToDayDetail();
                            }
                        });
                }
                
                @Override
                public void onError(String errorMessage) {
                    // Lỗi khi load user -> quay về DayDetailActivity
                    navigateToDayDetail();
                }
            });
    }
    
    private void navigateToDayDetail() {
        Intent intent = new Intent(CameraWorkoutActivity.this, DayDetailActivity.class);
        if (dayName != null) {
        intent.putExtra("dayName", dayName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Lưu workout session vào Firebase
     */
    private void saveWorkoutSession() {
        long endTime = System.currentTimeMillis();
        
        // Xác định completed dựa trên AI detect: đạt target reps hoặc hold time
        boolean completed = false;
        if (!isHoldExercise) {
            completed = (targetReps > 0 && latestReps >= targetReps);
        } else {
            completed = (targetHoldSeconds > 0 && latestHoldMillis >= targetHoldSeconds * 1000L);
        }
        
        WorkoutSessionResult session = new WorkoutSessionResult(
                null, // id sẽ được set bởi Firebase
                exerciseDisplayName,
                exerciseType.name(),
                exerciseId != null ? exerciseId : "", // Exercise ID từ schedule
                isHoldExercise,
                strictness.name(),
                dayName != null ? dayName : "", // Day name
                latestReps,
                latestHoldMillis,
                latestScore,
                completed, // true nếu đã hoàn thành target (dựa trên AI detect)
                sessionStartTime,
                endTime
        );

        FirebaseHelper.getInstance().saveWorkoutSession(session, new Callback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Workout session saved successfully");
                // Lưu session ID để dùng cho feedback (id đã được set trong saveWorkoutSession)
                savedWorkoutSessionId = session.getId();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to save workout session: " + errorMessage);
            }
        });
    }

    private void checkAutoFinish() {
        if (sessionFinished) return;

        if (!isHoldExercise) {
            if (targetReps > 0 && latestReps >= targetReps) {
                openDayDetailAndFinish(true);
            }
        } else {
            if (targetHoldSeconds > 0 && latestHoldMillis >= targetHoldSeconds * 1000L) {
                openDayDetailAndFinish(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
    }
}
