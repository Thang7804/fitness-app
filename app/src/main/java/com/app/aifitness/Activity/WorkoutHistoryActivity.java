package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.aifitness.Activity.Adapter.WorkoutHistoryAdapter;
import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.R;
import com.app.aifitness.workout.WorkoutSessionResult;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private RecyclerView rvWorkoutHistory;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private WorkoutHistoryAdapter adapter;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        rvWorkoutHistory = findViewById(R.id.rvWorkoutHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        rvWorkoutHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutHistoryAdapter(this);
        rvWorkoutHistory.setAdapter(adapter);

        setupBottomNavigation();
        loadWorkoutHistory();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_history);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                // Already on history page
                return true;
            } else if (itemId == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadWorkoutHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvWorkoutHistory.setVisibility(View.GONE);

        FirebaseHelper.getInstance().getWorkoutHistory(new DataCallBack<List<WorkoutSessionResult>>() {
            @Override
            public void onSuccess(List<WorkoutSessionResult> history) {
                progressBar.setVisibility(View.GONE);
                
                if (history == null || history.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvWorkoutHistory.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvWorkoutHistory.setVisibility(View.VISIBLE);
                    adapter.updateHistory(history);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                rvWorkoutHistory.setVisibility(View.GONE);
                Toast.makeText(WorkoutHistoryActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

