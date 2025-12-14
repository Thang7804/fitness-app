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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

                                List<Map<String, Object>> exerciseList = new ArrayList<>();
                                for (Map.Entry<String, Object> entry : dayMap.entrySet()) {
                                    if (entry.getValue() instanceof Map) {
                                        Map<String, Object> detail = (Map<String, Object>) entry.getValue();
                                        detail.put("name", idToName.getOrDefault(entry.getKey(), entry.getKey()));
                                        exerciseList.add(detail);
                                    }
                                }

                                ExerciseAdapter adapter = new ExerciseAdapter(DayDetailActivity.this, exerciseList);
                                rvExercises.setAdapter(adapter);
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