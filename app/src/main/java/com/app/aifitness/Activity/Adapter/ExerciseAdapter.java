package com.app.aifitness.Activity.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.aifitness.Activity.ExerciseDetail;
import com.app.aifitness.R;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    private Context context;
    private List<Map<String, Object>> exercises;

    public ExerciseAdapter(Context context, List<Map<String, Object>> exercises) {
        this.context = context;
        this.exercises = exercises;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.exerciseitems, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Map<String, Object> item = exercises.get(position);
        String name = (String) item.get("name");
        String type = (String) item.get("type");
        Object valueObj = item.get("value");
        int value = (valueObj instanceof Number) ? ((Number) valueObj).intValue() : 0;

        holder.tvExerciseName.setText(name != null ? name : "Unnamed Exercise");

        if (type != null) {
            holder.tvExerciseDetail.setText(type + ": " + value);
        } else {
            holder.tvExerciseDetail.setText("Unknown type");
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ExerciseDetail.class);
            intent.putExtra("exerciseData", (Serializable) item);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return exercises != null ? exercises.size() : 0;
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView tvExerciseName, tvExerciseDetail;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExerciseName = itemView.findViewById(R.id.tvExerciseName);
            tvExerciseDetail = itemView.findViewById(R.id.tvExerciseDetail);
        }
    }
}
