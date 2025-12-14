package com.app.aifitness.Activity.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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
    private Integer currentDay; // currentDay từ User (ngày đã hoàn thành)

    public ScheduleAdapter(Context context, Map<String, Map<String, Object>> schedule, Integer currentDay) {
        this.context = context;
        this.schedule = schedule;
        this.currentDay = currentDay != null ? currentDay : 0;
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

        // Lấy số ngày từ key (ví dụ: "day5" -> 5)
        int dayNumber;
        try {
            dayNumber = Integer.parseInt(day.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            dayNumber = 0;
        }

        // Ngày hiện tại = currentDay + 1 (vì currentDay là ngày đã hoàn thành)
        // Nếu currentDay = 0 (chưa hoàn thành ngày nào), thì ngày hiện tại là day1
        int todayDay = currentDay + 1;
        boolean isToday = (dayNumber == todayDay);
        boolean isPast = (dayNumber < todayDay);
        boolean isFuture = (dayNumber > todayDay);

        // Highlight ngày hiện tại với màu xanh lá
        if (isToday) {
            holder.cardView.setCardBackgroundColor(0xFF2E7D32); // Green highlight (#2E7D32)
            holder.tvStatus.setText("Start Today");
            holder.tvStatus.setTextColor(0xFFFFFFFF); // White text
            holder.tvDayName.setTextColor(0xFFFFFFFF); // White text
        } else if (isPast) {
            holder.cardView.setCardBackgroundColor(0xFF1E1E1E); // Default dark
            holder.tvStatus.setText("Completed");
            holder.tvStatus.setTextColor(0xFF888888); // Gray text
            holder.tvDayName.setTextColor(0xFFFFFFFF); // White text
        } else { // isFuture
            holder.cardView.setCardBackgroundColor(0xFF1E1E1E); // Default dark
            holder.tvStatus.setText("Start");
            holder.tvStatus.setTextColor(0xFFFF9800); // Orange text
            holder.tvDayName.setTextColor(0xFFFFFFFF); // White text
        }

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

    /**
     * Kiểm tra xem day có tồn tại trong schedule không
     */
    public boolean hasDay(String dayKey) {
        return dayList.contains(dayKey);
    }

    /**
     * Lấy position của một day trong list
     */
    public int getPositionForDay(String dayKey) {
        return dayList.indexOf(dayKey);
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName;
        TextView tvStatus;
        CardView cardView;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
