package com.app.aifitness.exercise;

public enum ExerciseType {
    PUSH_UP,
    SQUAT,
    PLANK,
    JUMPING_JACK,
    LUNGE,
    MOUNTAIN_CLIMBER,
    BICEP_CURL,
    BURPEE,
    SHOULDER_PRESS,
    HIGH_KNEES,
    SIDE_PLANK,
    DEADLIFT,
    BICYCLE_CRUNCH,
    WALL_SIT,
    TRICEP_DIP;

    /**
     * Map từ tên bài tập trong Firebase (chuẩn của bạn) -> enum để code dùng được.
     * Ví dụ Firebase: "Push-ups" => PUSH_UP
     */
    public static ExerciseType fromFirebaseName(String firebaseName) {
        if (firebaseName == null) return SQUAT;

        String s = firebaseName.trim().toLowerCase();

        switch (s) {
            case "push-ups": return PUSH_UP;
            case "squats": return SQUAT;
            case "plank": return PLANK;
            case "jumping jacks": return JUMPING_JACK;
            case "lunges": return LUNGE;
            case "mountain climbers": return MOUNTAIN_CLIMBER;
            case "bicep curls": return BICEP_CURL;
            case "burpees": return BURPEE;
            case "shoulder press": return SHOULDER_PRESS;
            case "high knees": return HIGH_KNEES;
            case "side plank": return SIDE_PLANK;
            case "deadlift": return DEADLIFT;
            case "bicycle crunches": return BICYCLE_CRUNCH;
            case "wall sit": return WALL_SIT;
            case "tricep dips": return TRICEP_DIP;
            default:
                return SQUAT; // fallback an toàn
        }
    }
}
