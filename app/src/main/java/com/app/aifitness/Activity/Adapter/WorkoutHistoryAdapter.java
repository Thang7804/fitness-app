package com.app.aifitness.Activity.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.app.aifitness.R;
import com.app.aifitness.workout.WorkoutSessionResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.HistoryViewHolder> {

    private List<WorkoutSessionResult> history;
    private Context context;
    private SimpleDateFormat dateFormat;

    public WorkoutHistoryAdapter(Context context) {
        this.context = context;
        this.history = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    public void updateHistory(List<WorkoutSessionResult> newHistory) {
        this.history = newHistory != null ? newHistory : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_workout_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        WorkoutSessionResult session = history.get(position);
        
        holder.tvExerciseName.setText(session.getExerciseName());
        holder.tvMode.setText(session.getModeLabel());
        holder.tvStrictness.setText("Strictness: " + session.getStrictness());
        holder.tvScore.setText("Score: " + session.getAvgScore());
        
        // Format date
        if (session.getEndTime() > 0) {
            Date date = new Date(session.getEndTime());
            holder.tvDate.setText(dateFormat.format(date));
        } else {
            holder.tvDate.setText("N/A");
        }

        // Display reps or hold time
        if (session.isHoldExercise()) {
            long seconds = session.getHoldSeconds();
            holder.tvStats.setText("Hold: " + seconds + "s");
        } else {
            holder.tvStats.setText("Reps: " + session.getTotalReps());
        }

        // Set score color
        int score = session.getAvgScore();
        int color;
        if (score <= 60) {
            color = Color.parseColor("#F44336"); // Red
        } else if (score <= 80) {
            color = Color.parseColor("#FF9800"); // Orange
        } else {
            color = Color.parseColor("#4CAF50"); // Green
        }
        holder.tvScore.setTextColor(color);

        // Card background based on score
        int bgColor;
        if (score <= 60) {
            bgColor = Color.parseColor("#1A1A1A");
        } else if (score <= 80) {
            bgColor = Color.parseColor("#1F1F1F");
        } else {
            bgColor = Color.parseColor("#1A2E1A");
        }
        holder.cardView.setCardBackgroundColor(bgColor);
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvExerciseName;
        TextView tvMode;
        TextView tvStrictness;
        TextView tvScore;
        TextView tvStats;
        TextView tvDate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvExerciseName = itemView.findViewById(R.id.tvExerciseName);
            tvMode = itemView.findViewById(R.id.tvMode);
            tvStrictness = itemView.findViewById(R.id.tvStrictness);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvStats = itemView.findViewById(R.id.tvStats);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}

