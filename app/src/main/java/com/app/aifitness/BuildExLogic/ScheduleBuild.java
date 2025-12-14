package com.app.aifitness.BuildExLogic;

import com.app.aifitness.Model.Exercise;
import com.app.aifitness.Model.User;

import java.util.*;

public class ScheduleBuild {

    private final Random random = new Random();
    
    /**
     * Tính tổng số ngày workout dựa trên dayPerWeek và availableTime
     * Logic: 
     * - Dựa trên dayPerWeek (số ngày tập mỗi tuần)
     * - availableTime có thể ảnh hưởng đến số ngày (nếu thời gian ngắn thì có thể tập nhiều ngày hơn)
     * - Mặc định: 30 ngày schedule, tính số ngày workout
     */
    public static int calculateTotalWorkoutDays(User user) {
        if (user == null || user.dayPerWeek == null || user.dayPerWeek <= 0) {
            return 0;
        }
        
        int dayPerWeek = user.dayPerWeek;
        int scheduleDays = 30; // Tổng số ngày trong schedule
        
        // Tính số tuần: 30 ngày / 7 ≈ 4.3 tuần
        double weeks = scheduleDays / 7.0;
        
        // Số ngày workout = số tuần * số ngày tập mỗi tuần
        int totalWorkoutDays = (int) Math.round(weeks * dayPerWeek);
        
        // Điều chỉnh dựa trên availableTime (nếu có)
        if (user.availableTime != null) {
            // Nếu thời gian ngắn (< 20 phút), có thể tập nhiều ngày hơn một chút
            if (user.availableTime < 20) {
                totalWorkoutDays = Math.min(totalWorkoutDays + 1, scheduleDays);
            }
            // Nếu thời gian dài (> 60 phút), có thể giảm số ngày một chút
            else if (user.availableTime > 60) {
                totalWorkoutDays = Math.max(totalWorkoutDays - 1, dayPerWeek);
            }
        }
        
        // Đảm bảo không vượt quá số ngày trong schedule
        return Math.min(totalWorkoutDays, scheduleDays);
    }
    
    /**
     * Build ngày tiếp theo trong schedule
     * LƯU Ý: Không cập nhật currentDay ở đây!
     * currentDay chỉ nên được cập nhật khi user thực sự hoàn thành workout
     * 
     * @param user User object
     * @param allExercises Danh sách tất cả exercises
     * @return Số ngày đã được build (nextDay), hoặc -1 nếu lỗi
     */
    public int buildNextDay(User user, List<Exercise> allExercises) {
        if (user == null || allExercises == null || allExercises.isEmpty()) {
            return -1;
        }

        if (user.schedule == null) {
            user.schedule = new HashMap<>();
        }
        if (user.currentDay == null) {
            user.currentDay = 0;
        }

        // Tính totalWorkoutDays nếu chưa có
        if (user.totalWorkoutDays == null) {
            user.totalWorkoutDays = calculateTotalWorkoutDays(user);
        }

        // Tìm ngày tiếp theo cần build (có thể là currentDay + 1 hoặc ngày tiếp theo chưa có trong schedule)
        int nextDay = findNextDayToBuild(user);
        String dayKey = "day" + nextDay;

        // Kiểm tra xem ngày này đã tồn tại chưa
        if (user.schedule.containsKey(dayKey)) {
            // Nếu đã tồn tại, tìm ngày tiếp theo
            nextDay = findNextDayToBuild(user);
            dayKey = "day" + nextDay;
        }

        Map<String, Object> exMap = new HashMap<>();
        if (nextDay % 4 == 0) {
            Map<String, Object> restMap = new HashMap<>();
            restMap.put("id", "rest_" + nextDay);
            restMap.put("type", "rest");
            restMap.put("value", 1);
            restMap.put("isDynamic", false);
            restMap.put("name", "Rest Day");
            exMap.put("rest", restMap);
        } else {
            List<Exercise> dayExercises = pickRandomExercises(allExercises, 4);

            for (Exercise ex : dayExercises) {
                Map<String, Object> detail = new HashMap<>();
                int value = generateRepsOrTime(user, ex);

                detail.put("id", ex.id);
                detail.put("name", ex.name);
                detail.put("isDynamic", ex.isDynamic);
                detail.put("type", ex.isDynamic ? "reps" : "time");
                detail.put("value", value);

                exMap.put(ex.id, detail);
            }
        }
        user.schedule.put(dayKey, exMap);
        
        // KHÔNG cập nhật currentDay ở đây!
        // currentDay chỉ nên được cập nhật khi user hoàn thành workout
        return nextDay;
    }
    
    /**
     * Tìm ngày tiếp theo cần build
     * Logic: Tìm ngày lớn nhất trong schedule, sau đó +1
     */
    private int findNextDayToBuild(User user) {
        if (user.schedule == null || user.schedule.isEmpty()) {
            // Nếu schedule rỗng, bắt đầu từ day1
            return 1;
        }
        
        int maxDay = 0;
        for (String dayKey : user.schedule.keySet()) {
            try {
                int dayNumber = Integer.parseInt(dayKey.replaceAll("\\D+", ""));
                if (dayNumber > maxDay) {
                    maxDay = dayNumber;
                }
            } catch (NumberFormatException e) {
                // Bỏ qua nếu không parse được
            }
        }
        
        return maxDay + 1;
    }

    private List<Exercise> pickRandomExercises(List<Exercise> allExercises, int count) {
        List<Exercise> result = new ArrayList<>();
        if (allExercises == null || allExercises.isEmpty()) return result;

        List<Exercise> copy = new ArrayList<>(allExercises);
        Collections.shuffle(copy, random);

        for (int i = 0; i < Math.min(count, copy.size()); i++) {
            result.add(copy.get(i));
        }
        return result;
    }

    private int generateRepsOrTime(User user, Exercise ex) {
        int level = (user.level != null) ? user.level : 1;
        int base;

        switch (level) {
            case 1:
                base = 10;
                break;
            case 2:
                base = 15;
                break;
            default:
                base = 20;
                break;
        }

        base += ex.difficulty * 2;

        if (user.availableTime != null) {
            if (user.availableTime < 20) base -= 3;
            else if (user.availableTime > 40) base += 3;
        }
        if (ex.isDynamic) {
            return Math.max(10, base);
        } else {
            return Math.max(8, base * 2);
        }
    }
}
