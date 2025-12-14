package com.app.aifitness.Activity.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.aifitness.Activity.RestDayActivity;
import com.app.aifitness.R;
import com.app.aifitness.Activity.DayDetailActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.DayViewHolder> {

    private Map<String, Map<String, Object>> schedule;
    private List<String> dayList;
    private Context context;

    public ScheduleAdapter(Context context, Map<String, Map<String, Object>> schedule) {
        this.context = context;
        this.schedule = schedule;
        this.dayList = new ArrayList<>(schedule.keySet());
        Collections.sort(dayList, (d1, d2) -> {
            int n1 = Integer.parseInt(d1.replaceAll("\\D+", ""));
            int n2 = Integer.parseInt(d2.replaceAll("\\D+", ""));
            return Integer.compare(n1, n2);
        });
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.scheduleday, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String day = dayList.get(position);
        String displayName = "Day " + day.replaceAll("\\D+", "");
        holder.tvDayName.setText(displayName);

        holder.itemView.setOnClickListener(v -> {
            boolean isRestDay = false;
            Map<String, Object> dayMap = schedule.get(day);
            if (dayMap != null && dayMap.containsKey("rest")) {
                Object restObj = dayMap.get("rest");
                if (restObj instanceof Map) {
                    Map<String, Object> restData = (Map<String, Object>) restObj;
                    Object typeObj = restData.get("type");
                    isRestDay = "rest".equals(typeObj);
                }
            }

            Intent intent;
            if (isRestDay) {
                intent = new Intent(context, RestDayActivity.class);
            } else {
                intent = new Intent(context, DayDetailActivity.class);
            }

            intent.putExtra("dayName", day);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
        }
    }
}
