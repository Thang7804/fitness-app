package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.aifitness.Activity.Adapter.ExerciseAdapter;
import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.Exercise;
import com.app.aifitness.Model.User;
import com.app.aifitness.R;
import com.app.aifitness.workout.WorkoutSessionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DayDetailActivity extends AppCompatActivity {

    private TextView tvDayTitle, btnBack;
    private RecyclerView rvExercises;
    private String dayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        tvDayTitle = findViewById(R.id.tvDayTitle);
        rvExercises = findViewById(R.id.rvExercises);
        rvExercises.setLayoutManager(new LinearLayoutManager(this));
        btnBack = findViewById(R.id.btnBack);
        dayName = getIntent().getStringExtra("dayName");
        String displayName;
        if(dayName!=null){
         displayName = "Day " + dayName.replaceAll("\\D+", "");}
        else{
            displayName="Unknown Day";
        }
        tvDayTitle.setText(displayName);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(DayDetailActivity.this, MainActivity.class);
            finish();
        });
        loadExercises();
        
        // Kiểm tra và cập nhật currentDay nếu cần (khi quay lại từ FeedbackActivity)
        checkAndUpdateCurrentDay();
    }
    
    /**
     * Kiểm tra và cập nhật currentDay nếu TẤT CẢ bài tập trong ngày đã hoàn thành
     * Tiêu chí hoàn thành: completed=true trong WorkoutSessionResult (dựa trên AI detect)
     */
    private void checkAndUpdateCurrentDay() {
        if (dayName == null || dayName.isEmpty()) {
            return;
        }
        
        // Parse dayNumber từ dayName (ví dụ: "day1" -> 1)
        int dayNumber;
        try {
            dayNumber = Integer.parseInt(dayName.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return;
        }
        
        // Lấy schedule để biết có bao nhiêu bài tập trong ngày
        FirebaseHelper.getInstance().getCurrentUser(
            FirebaseHelper.getInstance().getCurrentUserId(),
            new DataCallBack<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user == null || user.schedule == null) {
                        return;
                    }
                    
                    Map<String, Object> dayMap = user.schedule.get(dayName);
                    if (dayMap == null) {
                        return;
                    }
                    
                    // Đếm số bài tập trong ngày (không tính rest)
                    Set<String> exerciseIds = new HashSet<>();
                    for (Map.Entry<String, Object> entry : dayMap.entrySet()) {
                        String key = entry.getKey();
                        if (!key.equals("rest") && entry.getValue() instanceof Map) {
                            exerciseIds.add(key); // Exercise ID
                        }
                    }
                    
                    // Nếu là rest day, không cần check
                    if (exerciseIds.isEmpty()) {
                        // Rest day - cập nhật currentDay luôn
                        FirebaseHelper.getInstance().updateCurrentDayIfNeeded(dayNumber, new com.app.aifitness.Firebase.Callback() {
                            @Override
                            public void onSuccess() {
                                // Silent success
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                // Silent fail
                            }
                        });
                        return;
                    }
                    
                    // Lấy workout history để check bài tập nào đã hoàn thành
                    FirebaseHelper.getInstance().getWorkoutHistory(new DataCallBack<List<WorkoutSessionResult>>() {
                        @Override
                        public void onSuccess(List<WorkoutSessionResult> history) {
                            if (history == null) {
                                return;
                            }
                            
                            // Đếm số bài tập đã hoàn thành (completed=true và exerciseId match)
                            Set<String> completedExerciseIds = new HashSet<>();
                            for (WorkoutSessionResult session : history) {
                                if (session.isCompleted() && 
                                    dayName.equals(session.getDayName()) &&
                                    session.getExerciseId() != null &&
                                    exerciseIds.contains(session.getExerciseId())) {
                                    completedExerciseIds.add(session.getExerciseId());
                                }
                            }
                            
                            // Chỉ cập nhật currentDay khi TẤT CẢ bài tập đã hoàn thành
                            if (completedExerciseIds.size() >= exerciseIds.size()) {
                                FirebaseHelper.getInstance().updateCurrentDayIfNeeded(dayNumber, new com.app.aifitness.Firebase.Callback() {
                                    @Override
                                    public void onSuccess() {
                                        // Silent success
                                    }
                                    
                                    @Override
                                    public void onError(String errorMessage) {
                                        // Silent fail
                                    }
                                });
                            }
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            // Silent fail
                        }
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    // Silent fail
                }
            });
    }

    private void loadExercises() {
        FirebaseHelper.getInstance().getCurrentUser(FirebaseHelper.getInstance().getCurrentUserId(),
                new DataCallBack<User>() {
                    @Override
                    public void onSuccess(User user) {
                        if (user == null || user.schedule == null) {
                            Toast.makeText(DayDetailActivity.this, "No schedule found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> dayMap = user.schedule.get(dayName);
                        if (dayMap == null || dayMap.isEmpty()) {
                            Toast.makeText(DayDetailActivity.this, "No exercises for this day", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FirebaseHelper.getInstance().getAllExercises(new DataCallBack<List<Exercise>>() {
                            @Override
                            public void onSuccess(List<Exercise> allExercises) {

                                Map<String, String> idToName = new HashMap<>();
                                for (Exercise ex : allExercises) {
                                    idToName.put(ex.id, ex.name);
                                }

                                // Lấy workout history để check bài tập nào đã hoàn thành
                                FirebaseHelper.getInstance().getWorkoutHistory(new DataCallBack<List<WorkoutSessionResult>>() {
                                    @Override
                                    public void onSuccess(List<WorkoutSessionResult> history) {
                                        // Tạo set các exercise ID đã hoàn thành (completed=true)
                                        Set<String> completedExerciseIds = new HashSet<>();
                                        if (history != null) {
                                            for (WorkoutSessionResult session : history) {
                                                if (session.isCompleted() && 
                                                    dayName.equals(session.getDayName()) &&
                                                    session.getExerciseId() != null) {
                                                    completedExerciseIds.add(session.getExerciseId());
                                                }
                                            }
                                }

                                List<Map<String, Object>> exerciseList = new ArrayList<>();
                                for (Map.Entry<String, Object> entry : dayMap.entrySet()) {
                                    if (entry.getValue() instanceof Map) {
                                        String exerciseId = entry.getKey();
                                        Map<String, Object> detail = (Map<String, Object>) entry.getValue();
                                        detail.put("name", idToName.getOrDefault(exerciseId, exerciseId));
                                        detail.put("id", exerciseId); // Đảm bảo có id
                                        detail.put("completed", completedExerciseIds.contains(exerciseId)); // Đánh dấu đã hoàn thành
                                        exerciseList.add(detail);
                                    }
                                }

                                ExerciseAdapter adapter = new ExerciseAdapter(DayDetailActivity.this, exerciseList, dayName);
                                rvExercises.setAdapter(adapter);
                                    }
                                    
                                    @Override
                                    public void onError(String errorMessage) {
                                        // Nếu không load được history, vẫn hiển thị exercises nhưng không highlight
                                        List<Map<String, Object>> exerciseList = new ArrayList<>();
                                        for (Map.Entry<String, Object> entry : dayMap.entrySet()) {
                                            if (entry.getValue() instanceof Map) {
                                                String exerciseId = entry.getKey();
                                                Map<String, Object> detail = (Map<String, Object>) entry.getValue();
                                                detail.put("name", idToName.getOrDefault(exerciseId, exerciseId));
                                                detail.put("id", exerciseId);
                                                detail.put("completed", false);
                                                exerciseList.add(detail);
                                            }
                                        }
                                        ExerciseAdapter adapter = new ExerciseAdapter(DayDetailActivity.this, exerciseList, dayName);
                                        rvExercises.setAdapter(adapter);
                                    }
                                });
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(DayDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(DayDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}