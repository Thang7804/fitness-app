package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.User;
import com.app.aifitness.R;
import com.app.aifitness.workout.WorkoutSessionResult;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import com.app.aifitness.Activity.Adapter.ScheduleAdapter;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvScheduleDays;
    private BottomNavigationView bottomNavigation;
    private TextView tvTotalWorkouts;
    private TextView tvTodayWorkouts;
    private View dashboardCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvScheduleDays = findViewById(R.id.rvScheduleDays);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvTotalWorkouts = findViewById(R.id.tvTotalWorkouts);
        tvTodayWorkouts = findViewById(R.id.tvTodayWorkouts);
        dashboardCard = findViewById(R.id.dashboardCard);

        rvScheduleDays.setLayoutManager(new LinearLayoutManager(this));

        setupBottomNavigation();
        loadScheduleDays();
        loadDashboardStats();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_schedule) {
                // Already on schedule page
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, WorkoutHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadScheduleDays() {
        FirebaseHelper.getInstance().getCurrentUser(FirebaseHelper.getInstance().getCurrentUserId(), new DataCallBack<User>() {
            @Override
            public void onSuccess(User user) {
                if (user == null || user.schedule == null || user.schedule.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No schedule days found", Toast.LENGTH_SHORT).show();
                    return;
                }

                ScheduleAdapter adapter = new ScheduleAdapter(MainActivity.this, user.schedule);
                rvScheduleDays.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDashboardStats() {
        FirebaseHelper.getInstance().getWorkoutHistory(new DataCallBack<List<WorkoutSessionResult>>() {
            @Override
            public void onSuccess(List<WorkoutSessionResult> history) {
                if (history != null) {
                    int total = history.size();
                    tvTotalWorkouts.setText(String.valueOf(total));

                    // Count today's workouts
                    long todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                    int todayCount = 0;
                    for (WorkoutSessionResult session : history) {
                        if (session.getEndTime() >= todayStart) {
                            todayCount++;
                        }
                    }
                    tvTodayWorkouts.setText(String.valueOf(todayCount));
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Silent fail for dashboard
            }
        });
    }
}