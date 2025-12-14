package com.app.aifitness.BuildExLogic;

import com.app.aifitness.Model.Exercise;
import com.app.aifitness.Model.User;

import java.util.*;

public class ScheduleBuild {

    private final Random random = new Random();
    public void buildNextDay(User user, List<Exercise> allExercises) {
        if (user == null || allExercises == null || allExercises.isEmpty()) {
            return;
        }

        if (user.schedule == null) {
            user.schedule = new HashMap<>();
        }
        if (user.currentDay == null) {
            user.currentDay = 0;
        }

        int nextDay = user.currentDay + 1;
        String dayKey = "day" + nextDay;

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
                detail.put("type", ex.isDynamic ? "time" : "reps");
                detail.put("value", value);

                exMap.put(ex.id, detail);
            }
        }
        user.schedule.put(dayKey, exMap);
        user.currentDay = nextDay;
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
