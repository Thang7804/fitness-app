package com.app.aifitness.Firebase;

import com.app.aifitness.Model.Exercise;
import com.app.aifitness.Model.User;
import com.app.aifitness.BuildExLogic.Rules;
import com.app.aifitness.BuildExLogic.ScheduleBuild;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logic cập nhật lại bài tập trong schedule
 * 
 * Có 2 trường hợp:
 * 1. Cập nhật dựa trên feedback (too easy/hard) - chỉ điều chỉnh reps/time
 * 2. Cập nhật dựa trên thay đổi body parameters - rebuild schedule từ đầu
 */
public class ScheduleUpdateLogic {

    /**
     * Cập nhật bài tập dựa trên feedback
     * - Too Easy: tăng reps/time
     * - Too Hard: giảm reps/time
     * - Chỉ điều chỉnh các ngày CHƯA TẬP (từ currentDay+1 trở đi)
     * 
     * @param user User hiện tại
     * @param exerciseType Loại bài tập cần điều chỉnh (ví dụ: "SQUAT")
     * @param difficulty "too_easy" hoặc "too_hard"
     * @return true nếu đã điều chỉnh, false nếu không tìm thấy
     */
    public static boolean updateExerciseByFeedback(User user, String exerciseType, String difficulty) {
        if (user == null || user.schedule == null || user.schedule.isEmpty()) {
            return false;
        }

        int currentDay = user.currentDay != null ? user.currentDay : 0;
        boolean adjusted = false;

        // Duyệt qua tất cả các ngày trong schedule
        for (Map.Entry<String, Map<String, Object>> dayEntry : user.schedule.entrySet()) {
            String dayKey = dayEntry.getKey();
            
            // Lấy số ngày từ key (ví dụ: "day5" -> 5)
            int dayNumber;
            try {
                dayNumber = Integer.parseInt(dayKey.replaceAll("\\D+", ""));
            } catch (NumberFormatException e) {
                continue; // Bỏ qua nếu không parse được
            }

            // CHỈ điều chỉnh các ngày CHƯA TẬP (từ currentDay+1 trở đi)
            if (dayNumber <= currentDay) {
                continue;
            }

            Map<String, Object> dayExercises = dayEntry.getValue();
            if (dayExercises == null) continue;

            // Duyệt qua các bài tập trong ngày
            for (Map.Entry<String, Object> exEntry : dayExercises.entrySet()) {
                if (exEntry.getKey().equals("rest")) continue;

                Object exObj = exEntry.getValue();
                if (!(exObj instanceof Map)) continue;

                Map<String, Object> exDetail = (Map<String, Object>) exObj;
                String exId = (String) exDetail.get("id");
                
                // Kiểm tra nếu là bài tập cần điều chỉnh
                if (exId == null || !exId.contains(exerciseType.toLowerCase())) {
                    continue;
                }

                // Lấy giá trị hiện tại
                Object valueObj = exDetail.get("value");
                if (!(valueObj instanceof Number)) continue;

                int currentValue = ((Number) valueObj).intValue();
                int newValue = currentValue;

                // Điều chỉnh dựa trên feedback
                if ("too_easy".equals(difficulty)) {
                    // Tăng reps/time: +10-15% hoặc +2 (tùy loại)
                    String type = (String) exDetail.get("type");
                    if ("time".equals(type)) {
                        // Time: tăng 15%
                        newValue = (int)(currentValue * 1.15f);
                    } else {
                        // Reps: tăng 2 reps
                        newValue = currentValue + 2;
                    }
                } else if ("too_hard".equals(difficulty)) {
                    // Giảm reps/time: -10% hoặc -2 (tùy loại)
                    String type = (String) exDetail.get("type");
                    if ("time".equals(type)) {
                        // Time: giảm 10%
                        newValue = Math.max(10, (int)(currentValue * 0.9f));
                    } else {
                        // Reps: giảm 2 reps, tối thiểu 5
                        newValue = Math.max(5, currentValue - 2);
                    }
                }

                // Cập nhật giá trị mới
                if (newValue != currentValue) {
                    exDetail.put("value", newValue);
                    adjusted = true;
                }
            }
        }

        return adjusted;
    }

    /**
     * Rebuild schedule khi body parameters thay đổi đáng kể
     * - Tính lại reps/time dựa trên weight/height mới
     * - Có thể reset về ngày 1 hoặc chỉ rebuild các ngày chưa tập
     * 
     * @param user User với body parameters mới
     * @param allExercises Danh sách tất cả bài tập
     * @param resetToDay1 true = reset về ngày 1, false = chỉ rebuild các ngày chưa tập
     * @return true nếu đã rebuild
     */
    public static boolean rebuildScheduleByBodyChange(User user, List<Exercise> allExercises, boolean resetToDay1) {
        if (user == null || allExercises == null || allExercises.isEmpty()) {
            return false;
        }

        Rules rules = new Rules();
        ScheduleBuild scheduler = new ScheduleBuild();

        // Filter recommended exercises dựa trên body parameters mới
        List<Exercise> recommended = rules.filterRecommended(user, allExercises);

        int currentDay = user.currentDay != null ? user.currentDay : 0;

        // Tính totalWorkoutDays nếu chưa có
        if (user.totalWorkoutDays == null || user.totalWorkoutDays <= 0) {
            user.totalWorkoutDays = ScheduleBuild.calculateTotalWorkoutDays(user);
        }
        
        // Tính số ngày cần gen: totalWorkoutDays + rest days
        int totalDaysToGenerate = Math.max(30, user.totalWorkoutDays + (user.totalWorkoutDays / 3));

        if (resetToDay1) {
            // Reset về ngày 1: xóa toàn bộ schedule và rebuild
            user.schedule = new HashMap<>();
            user.currentDay = 0;
            
            // Build schedule dựa trên totalWorkoutDays
            for (int i = 0; i < totalDaysToGenerate; i++) {
                scheduler.buildNextDay(user, recommended);
            }
        } else {
            // Chỉ rebuild các ngày chưa tập (từ currentDay+1 trở đi)
            // Xóa các ngày chưa tập
            Map<String, Map<String, Object>> scheduleCopy = new HashMap<>(user.schedule);
            for (Map.Entry<String, Map<String, Object>> dayEntry : scheduleCopy.entrySet()) {
                String dayKey = dayEntry.getKey();
                try {
                    int dayNumber = Integer.parseInt(dayKey.replaceAll("\\D+", ""));
                    if (dayNumber > currentDay) {
                        user.schedule.remove(dayKey);
                    }
                } catch (NumberFormatException e) {
                    // Giữ nguyên nếu không parse được
                }
            }

            // Rebuild các ngày còn lại dựa trên totalWorkoutDays
            int daysToRebuild = totalDaysToGenerate - currentDay;
            for (int i = 0; i < daysToRebuild; i++) {
                scheduler.buildNextDay(user, recommended);
            }
        }

        return true;
    }

    /**
     * Tính toán reps/time dựa trên user và exercise
     * (Copy logic từ ScheduleBuild.generateRepsOrTime)
     */
    private static int calculateRepsOrTime(User user, Exercise ex) {
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

    /**
     * Điều chỉnh tất cả bài tập trong schedule dựa trên body parameters
     * - Tính lại reps/time cho tất cả bài tập (kể cả đã tập)
     * - Sử dụng công thức mới dựa trên weight/height
     * 
     * @param user User với body parameters mới
     * @param allExercises Danh sách tất cả bài tập
     * @return true nếu đã điều chỉnh
     */
    public static boolean adjustAllExercisesByBodyChange(User user, List<Exercise> allExercises) {
        if (user == null || user.schedule == null || user.schedule.isEmpty() || allExercises == null) {
            return false;
        }

        boolean adjusted = false;

        // Tính BMI mới
        float bmi = 0f;
        if (user.weight != null && user.height != null && user.height > 0) {
            bmi = user.weight / (user.height * user.height / 10000f);
        }

        // Duyệt qua tất cả các ngày
        for (Map.Entry<String, Map<String, Object>> dayEntry : user.schedule.entrySet()) {
            Map<String, Object> dayExercises = dayEntry.getValue();
            if (dayExercises == null) continue;

            for (Map.Entry<String, Object> exEntry : dayExercises.entrySet()) {
                if (exEntry.getKey().equals("rest")) continue;

                Object exObj = exEntry.getValue();
                if (!(exObj instanceof Map)) continue;

                Map<String, Object> exDetail = (Map<String, Object>) exObj;
                String exId = (String) exDetail.get("id");
                
                // Tìm exercise trong allExercises
                Exercise exercise = null;
                for (Exercise ex : allExercises) {
                    if (ex.id != null && ex.id.equals(exId)) {
                        exercise = ex;
                        break;
                    }
                }

                if (exercise == null) continue;

                // Tính lại giá trị dựa trên body parameters mới
                int newValue = calculateRepsOrTime(user, exercise);

                // Điều chỉnh dựa trên BMI
                if (bmi > 30 && exercise.intensity >= 3) {
                    // BMI cao + intensity cao -> giảm 15%
                    newValue = (int)(newValue * 0.85f);
                } else if (bmi < 18.5 && exercise.intensity >= 3) {
                    // BMI thấp + intensity cao -> giảm 10%
                    newValue = (int)(newValue * 0.9f);
                }

                Object currentValueObj = exDetail.get("value");
                int currentValue = currentValueObj instanceof Number ? ((Number) currentValueObj).intValue() : 0;

                if (newValue != currentValue) {
                    exDetail.put("value", newValue);
                    adjusted = true;
                }
            }
        }

        return adjusted;
    }
}

