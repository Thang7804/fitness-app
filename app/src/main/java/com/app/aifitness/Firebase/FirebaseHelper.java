package com.app.aifitness.Firebase;

import com.app.aifitness.Model.Exercise;
import com.app.aifitness.Model.User;
import com.app.aifitness.workout.WorkoutFeedback;
import com.app.aifitness.workout.WorkoutSessionResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.app.aifitness.Firebase.ScheduleUpdateLogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public void registerUser(String email, String password, Callback authCallback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        User newUser = new User(email);
                        db.collection("Users").document(uid).set(newUser)
                                .addOnSuccessListener(aVoid -> authCallback.onSuccess())
                                .addOnFailureListener(e -> authCallback.onError("Save user error: " + e.getMessage()));
                    } else {
                        authCallback.onError("Register error: " + task.getException().getMessage());
                    }
                });
    }

    public void loginUser(String email, String password, Callback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Sign in error: " + task.getException().getMessage());
                    }
                });
    }

    public void updateUser(User user, Callback updateCallback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            updateCallback.onError("User has not logged in");
            return;
        }

        if (user == null) {
            updateCallback.onError("User data is null");
            return;
        }

        DocumentReference userRef = db.collection("Users").document(uid);
        Map<String, Object> updates = new HashMap<>();

        if (user.goal != null && !user.goal.isEmpty()) updates.put("goal", user.goal);
        if (user.availableTime != null && user.availableTime > 0) updates.put("availableTime", user.availableTime);
        if (user.experience != null && !user.experience.isEmpty()) updates.put("experience", user.experience);
        if (user.hasEquipment != null) updates.put("hasEquipment", user.hasEquipment);
        if (user.focusArea != null && !user.focusArea.isEmpty()) updates.put("focusArea", user.focusArea);
        if (user.Dob != null && !user.Dob.isEmpty()) updates.put("Dob", user.Dob);
        if (user.gender != null && !user.gender.isEmpty()) updates.put("gender", user.gender);
        if (user.height != null ) updates.put("height", user.height);
        if (user.weight != null ) updates.put("weight", user.weight);
        if (user.goalWeight != null ) updates.put("goalWeight", user.goalWeight);
        if (user.level != null ) updates.put("level", user.level);
        if (user.currentDay != null ) updates.put("currentDay", user.currentDay);
        if (user.dayPerWeek != null ) updates.put("dayPerWeek", user.dayPerWeek);
        if (user.healthIssue != null && !user.healthIssue.isEmpty()) updates.put("healthIssue", user.healthIssue);
        if (user.schedule != null && !user.schedule.isEmpty()) updates.put("schedule", user.schedule);

        if (updates.isEmpty()) {
            updateCallback.onError("No updates to apply");
            return;
        }

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> updateCallback.onSuccess())
                .addOnFailureListener(e -> updateCallback.onError(e.getMessage()));
    }

    public void getCurrentUser(String userId, DataCallBack<User> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError("Invalid user ID");
            return;
        }

        db.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllExercises(DataCallBack<List<Exercise>> callback) {
        db.collection("exercises")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Exercise> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            list.add(doc.toObject(Exercise.class));
                        }
                        callback.onSuccess(list);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public void getExerciseById(String exerciseId, DataCallBack<Exercise> callback) {
        db.collection("exercises").document(exerciseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot.toObject(Exercise.class));
                    } else {
                        callback.onError("Exercise not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    public void updateIsNewStatus(String uid,boolean isNewStatus, Callback updateCallback) {
        if (uid == null) {
            updateCallback.onError("User has not logged in");
            return;
        }

        DocumentReference userRef = db.collection("Users").document(uid);
        Map<String, Object> update = new HashMap<>();

        update.put("isNew", isNewStatus);

        userRef.update(update)
                .addOnSuccessListener(aVoid -> updateCallback.onSuccess())
                .addOnFailureListener(e -> updateCallback.onError( e.getMessage()));
    }

    public String getCurrentUserId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    public String getCurrentUserMail() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : null;
    }

    /**
     * Đăng xuất người dùng
     */
    public void signOut() {
        mAuth.signOut();
    }

    // ===== Workout Feedback Methods =====

    /**
     * Lưu feedback của người dùng về bài tập
     */
    public void saveWorkoutFeedback(WorkoutFeedback feedback, Callback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("User has not logged in");
            return;
        }

        if (feedback == null) {
            callback.onError("Feedback is null");
            return;
        }

        db.collection("Users")
                .document(uid)
                .collection("workoutFeedback")
                .add(feedback)
                .addOnSuccessListener(documentReference -> {
                    feedback.setId(documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Failed to save feedback: " + e.getMessage()));
    }

    // ===== Workout History Methods =====

    /**
     * Lưu workout session vào Firestore
     */
    public void saveWorkoutSession(WorkoutSessionResult session, Callback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("User has not logged in");
            return;
        }

        if (session == null) {
            callback.onError("Workout session is null");
            return;
        }

        db.collection("Users")
                .document(uid)
                .collection("workoutHistory")
                .add(session)
                .addOnSuccessListener(documentReference -> {
                    session.setId(documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Failed to save workout: " + e.getMessage()));
    }

    /**
     * Lấy toàn bộ lịch sử tập luyện của user, sắp xếp theo thời gian giảm dần
     */
    public void getWorkoutHistory(DataCallBack<List<WorkoutSessionResult>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("User has not logged in");
            return;
        }

        db.collection("Users")
                .document(uid)
                .collection("workoutHistory")
                .orderBy("endTime", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<WorkoutSessionResult> history = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            WorkoutSessionResult session = doc.toObject(WorkoutSessionResult.class);
                            session.setId(doc.getId());
                            history.add(session);
                        }
                        callback.onSuccess(history);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    /**
     * Lấy lịch sử tập luyện trong khoảng thời gian (để vẽ biểu đồ)
     */
    public void getWorkoutHistoryByDateRange(long startTime, long endTime, DataCallBack<List<WorkoutSessionResult>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("User has not logged in");
            return;
        }

        db.collection("Users")
                .document(uid)
                .collection("workoutHistory")
                .whereGreaterThanOrEqualTo("endTime", startTime)
                .whereLessThanOrEqualTo("endTime", endTime)
                .orderBy("endTime", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<WorkoutSessionResult> history = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            WorkoutSessionResult session = doc.toObject(WorkoutSessionResult.class);
                            session.setId(doc.getId());
                            history.add(session);
                        }
                        callback.onSuccess(history);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // ===== Workout Feedback Methods =====

    /**
     * Lưu feedback của người dùng về bài tập
     */

    public void adjustWorkoutBasedOnFeedback(String exerciseType, String difficulty, Callback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("User has not logged in");
            return;
        }

        // Lấy user hiện tại
        getCurrentUser(uid, new DataCallBack<User>() {
            @Override
            public void onSuccess(User user) {
                if (user == null || user.schedule == null || user.schedule.isEmpty()) {
                    callback.onError("No schedule found");
                    return;
                }

                // Sử dụng ScheduleUpdateLogic để cập nhật
                boolean adjusted = ScheduleUpdateLogic.updateExerciseByFeedback(user, exerciseType, difficulty);

                if (adjusted) {
                    // Cập nhật user với schedule đã điều chỉnh
                    updateUser(user, callback);
                } else {
                    callback.onSuccess(); // Không tìm thấy bài tập để điều chỉnh
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Rebuild schedule khi thông số cơ thể thay đổi đáng kể
     * 
     * @param resetToDay1 true = reset về ngày 1 và rebuild toàn bộ, false = chỉ rebuild các ngày chưa tập
     */
    public void rebuildScheduleAfterBodyChange(boolean resetToDay1, Callback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("User has not logged in");
            return;
        }

        getCurrentUser(uid, new DataCallBack<User>() {
            @Override
            public void onSuccess(User user) {
                getAllExercises(new DataCallBack<List<Exercise>>() {
                    @Override
                    public void onSuccess(List<Exercise> exercises) {
                        // Sử dụng ScheduleUpdateLogic để rebuild
                        boolean success = ScheduleUpdateLogic.rebuildScheduleByBodyChange(user, exercises, resetToDay1);
                        
                        if (success) {
                            // Update user
                            updateUser(user, callback);
                        } else {
                            callback.onError("Failed to rebuild schedule");
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Điều chỉnh tất cả bài tập trong schedule dựa trên body parameters mới
     * Tính lại reps/time cho tất cả bài tập (kể cả đã tập)
     */
    public void adjustAllExercisesByBodyChange(Callback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("User has not logged in");
            return;
        }

        getCurrentUser(uid, new DataCallBack<User>() {
            @Override
            public void onSuccess(User user) {
                getAllExercises(new DataCallBack<List<Exercise>>() {
                    @Override
                    public void onSuccess(List<Exercise> exercises) {
                        // Sử dụng ScheduleUpdateLogic để điều chỉnh
                        boolean adjusted = ScheduleUpdateLogic.adjustAllExercisesByBodyChange(user, exercises);
                        
                        if (adjusted) {
                            updateUser(user, callback);
                        } else {
                            callback.onSuccess(); // Không có gì để điều chỉnh
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}
