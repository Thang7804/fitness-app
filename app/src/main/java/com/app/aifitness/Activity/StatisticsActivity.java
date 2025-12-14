package com.app.aifitness.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.R;
import com.app.aifitness.workout.WorkoutSessionResult;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private LineChart chartScore;
    private BarChart chartReps;
    private TextView tvTotalWorkouts;
    private TextView tvAvgScore;
    private TextView tvTotalReps;
    private TextView tvBestScore;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        chartScore = findViewById(R.id.chartScore);
        chartReps = findViewById(R.id.chartReps);
        tvTotalWorkouts = findViewById(R.id.tvTotalWorkouts);
        tvAvgScore = findViewById(R.id.tvAvgScore);
        tvTotalReps = findViewById(R.id.tvTotalReps);
        tvBestScore = findViewById(R.id.tvBestScore);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupBottomNavigation();
        setupCharts();
        loadStatistics();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_statistics);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, WorkoutHistoryActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_statistics) {
                // Already on statistics page
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupCharts() {
        // Setup Score Chart
        chartScore.getDescription().setEnabled(false);
        chartScore.setTouchEnabled(true);
        chartScore.setDragEnabled(true);
        chartScore.setScaleEnabled(true);
        chartScore.setPinchZoom(true);
        chartScore.setBackgroundColor(Color.parseColor("#1A1A1A"));
        chartScore.getLegend().setTextColor(Color.WHITE);

        XAxis xAxisScore = chartScore.getXAxis();
        xAxisScore.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisScore.setTextColor(Color.WHITE);
        xAxisScore.setGridColor(Color.parseColor("#333333"));
        xAxisScore.setAxisLineColor(Color.parseColor("#666666"));

        YAxis leftAxisScore = chartScore.getAxisLeft();
        leftAxisScore.setTextColor(Color.WHITE);
        leftAxisScore.setGridColor(Color.parseColor("#333333"));
        leftAxisScore.setAxisLineColor(Color.parseColor("#666666"));
        leftAxisScore.setAxisMinimum(0f);
        leftAxisScore.setAxisMaximum(100f);

        YAxis rightAxisScore = chartScore.getAxisRight();
        rightAxisScore.setEnabled(false);

        // Setup Reps Chart
        chartReps.getDescription().setEnabled(false);
        chartReps.setTouchEnabled(true);
        chartReps.setDragEnabled(true);
        chartReps.setScaleEnabled(true);
        chartReps.setPinchZoom(true);
        chartReps.setBackgroundColor(Color.parseColor("#1A1A1A"));
        chartReps.getLegend().setTextColor(Color.WHITE);

        XAxis xAxisReps = chartReps.getXAxis();
        xAxisReps.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisReps.setTextColor(Color.WHITE);
        xAxisReps.setGridColor(Color.parseColor("#333333"));
        xAxisReps.setAxisLineColor(Color.parseColor("#666666"));

        YAxis leftAxisReps = chartReps.getAxisLeft();
        leftAxisReps.setTextColor(Color.WHITE);
        leftAxisReps.setGridColor(Color.parseColor("#333333"));
        leftAxisReps.setAxisLineColor(Color.parseColor("#666666"));
        leftAxisReps.setAxisMinimum(0f);

        YAxis rightAxisReps = chartReps.getAxisRight();
        rightAxisReps.setEnabled(false);
    }

    private void loadStatistics() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getInstance().getWorkoutHistory(new DataCallBack<List<WorkoutSessionResult>>() {
            @Override
            public void onSuccess(List<WorkoutSessionResult> history) {
                progressBar.setVisibility(View.GONE);
                
                if (history == null || history.isEmpty()) {
                    showEmptyState();
                    return;
                }

                calculateAndDisplayStats(history);
                setupScoreChart(history);
                setupRepsChart(history);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StatisticsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateAndDisplayStats(List<WorkoutSessionResult> history) {
        int totalWorkouts = history.size();
        int totalReps = 0;
        int totalScore = 0;
        int bestScore = 0;
        int countWithScore = 0;

        for (WorkoutSessionResult session : history) {
            if (!session.isHoldExercise()) {
                totalReps += session.getTotalReps();
            }
            if (session.getAvgScore() > 0) {
                totalScore += session.getAvgScore();
                countWithScore++;
                if (session.getAvgScore() > bestScore) {
                    bestScore = session.getAvgScore();
                }
            }
        }

        tvTotalWorkouts.setText(String.valueOf(totalWorkouts));
        tvTotalReps.setText(String.valueOf(totalReps));
        tvBestScore.setText(String.valueOf(bestScore));

        if (countWithScore > 0) {
            int avgScore = totalScore / countWithScore;
            tvAvgScore.setText(String.valueOf(avgScore));
        } else {
            tvAvgScore.setText("N/A");
        }
    }

    private void setupScoreChart(List<WorkoutSessionResult> history) {
        // Get last 7 workouts for score chart
        int count = Math.min(7, history.size());
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        for (int i = 0; i < count; i++) {
            WorkoutSessionResult session = history.get(i);
            entries.add(new Entry(i, session.getAvgScore()));
            Date date = new Date(session.getEndTime());
            labels.add(dateFormat.format(date));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Score");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#FF9800"));
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#33FF9800"));

        LineData lineData = new LineData(dataSet);
        chartScore.setData(lineData);
        chartScore.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartScore.invalidate();
    }

    private void setupRepsChart(List<WorkoutSessionResult> history) {
        // Group by exercise type and sum reps
        List<WorkoutSessionResult> repsWorkouts = new ArrayList<>();
        for (WorkoutSessionResult session : history) {
            if (!session.isHoldExercise()) {
                repsWorkouts.add(session);
            }
        }

        if (repsWorkouts.isEmpty()) {
            chartReps.setVisibility(View.GONE);
            return;
        }

        // Get last 7 workouts
        int count = Math.min(7, repsWorkouts.size());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        for (int i = 0; i < count; i++) {
            WorkoutSessionResult session = repsWorkouts.get(i);
            entries.add(new BarEntry(i, session.getTotalReps()));
            Date date = new Date(session.getEndTime());
            labels.add(dateFormat.format(date));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Reps");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        chartReps.setData(barData);
        chartReps.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartReps.invalidate();
    }

    private void showEmptyState() {
        chartScore.setVisibility(View.GONE);
        chartReps.setVisibility(View.GONE);
    }
}

