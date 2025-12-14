package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.BuildExLogic.ScheduleBuild;
import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.User;
import com.app.aifitness.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvEmail;
    private EditText edtHeight;
    private EditText edtWeight;
    private EditText edtGoalWeight;
    private EditText edtDOB;
    private RadioGroup rgGender;
    private RadioButton rbMale;
    private RadioButton rbFemale;
    
    // Fitness Goals - Editable
    private RadioGroup rgGoal;
    private RadioButton rbLoseWeight;
    private RadioButton rbGainMuscle;
    private RadioButton rbMaintainFitness;
    private RadioButton rbIncreaseStamina;
    
    private RadioGroup rgExperience;
    private RadioButton rbBeginner;
    private RadioButton rbIntermediate;
    private RadioButton rbAdvanced;
    
    private RadioGroup rgFocusArea;
    private RadioButton rbFullBody;
    private RadioButton rbUpperBody;
    private RadioButton rbLowerBody;
    private RadioButton rbCore;
    
    private RadioGroup rgHealthIssue;
    private RadioButton rbNoIssues;
    private RadioButton rbJointPain;
    private RadioButton rbBackPain;
    private RadioButton rbCardioIssues;
    
    private RadioGroup rgAvailableTime;
    private RadioButton rb15Min;
    private RadioButton rb30Min;
    private RadioButton rb45Min;
    private RadioButton rb60MinPlus;
    
    private RadioGroup rgDayPerWeek;
    private RadioButton rb2Days;
    private RadioButton rb3Days;
    private RadioButton rb4Days;
    private RadioButton rb5Days;
    private RadioButton rb6Days;
    
    private MaterialButton btnSave;
    private MaterialButton btnEdit;
    private MaterialButton btnLogout;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;

    // View Mode TextViews
    private TextView tvHeight;
    private TextView tvWeight;
    private TextView tvGoalWeight;
    private TextView tvDOB;
    private TextView tvGender;
    private TextView tvGoal;
    private TextView tvExperience;
    private TextView tvFocusArea;
    private TextView tvHealthIssue;
    private TextView tvAvailableTime;
    private TextView tvDayPerWeek;

    // View/Edit Mode Containers
    private View llHeightView, llWeightView, llGoalWeightView, llDOBView, llGenderView;
    private View llFitnessGoalsView;
    private View tilHeight, tilWeight, tilGoalWeight, tilDOB, llGenderEdit;
    private View llFitnessGoalsEdit;

    private boolean isEditMode = false;
    private User currentUser;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseHelper = FirebaseHelper.getInstance();
        initViews();
        setupBottomNavigation();
        loadUserProfile();
        setupEditButton();
        setupSaveButton();
        setupLogoutButton();
        setViewMode(); // Start in view mode
    }

    private void initViews() {
        tvEmail = findViewById(R.id.tvEmail);
        edtHeight = findViewById(R.id.edtHeight);
        edtWeight = findViewById(R.id.edtWeight);
        edtGoalWeight = findViewById(R.id.edtGoalWeight);
        edtDOB = findViewById(R.id.edtDOB);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        
        // Fitness Goals
        rgGoal = findViewById(R.id.rgGoal);
        rbLoseWeight = findViewById(R.id.rbLoseWeight);
        rbGainMuscle = findViewById(R.id.rbGainMuscle);
        rbMaintainFitness = findViewById(R.id.rbMaintainFitness);
        rbIncreaseStamina = findViewById(R.id.rbIncreaseStamina);
        
        rgExperience = findViewById(R.id.rgExperience);
        rbBeginner = findViewById(R.id.rbBeginner);
        rbIntermediate = findViewById(R.id.rbIntermediate);
        rbAdvanced = findViewById(R.id.rbAdvanced);
        
        rgFocusArea = findViewById(R.id.rgFocusArea);
        rbFullBody = findViewById(R.id.rbFullBody);
        rbUpperBody = findViewById(R.id.rbUpperBody);
        rbLowerBody = findViewById(R.id.rbLowerBody);
        rbCore = findViewById(R.id.rbCore);
        
        rgHealthIssue = findViewById(R.id.rgHealthIssue);
        rbNoIssues = findViewById(R.id.rbNoIssues);
        rbJointPain = findViewById(R.id.rbJointPain);
        rbBackPain = findViewById(R.id.rbBackPain);
        rbCardioIssues = findViewById(R.id.rbCardioIssues);
        
        rgAvailableTime = findViewById(R.id.rgAvailableTime);
        rb15Min = findViewById(R.id.rb15Min);
        rb30Min = findViewById(R.id.rb30Min);
        rb45Min = findViewById(R.id.rb45Min);
        rb60MinPlus = findViewById(R.id.rb60MinPlus);
        
        rgDayPerWeek = findViewById(R.id.rgDayPerWeek);
        rb2Days = findViewById(R.id.rb2Days);
        rb3Days = findViewById(R.id.rb3Days);
        rb4Days = findViewById(R.id.rb4Days);
        rb5Days = findViewById(R.id.rb5Days);
        rb6Days = findViewById(R.id.rb6Days);
        
        btnSave = findViewById(R.id.btnSave);
        btnEdit = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // View Mode TextViews
        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);
        tvGoalWeight = findViewById(R.id.tvGoalWeight);
        tvDOB = findViewById(R.id.tvDOB);
        tvGender = findViewById(R.id.tvGender);
        tvGoal = findViewById(R.id.tvGoal);
        tvExperience = findViewById(R.id.tvExperience);
        tvFocusArea = findViewById(R.id.tvFocusArea);
        tvHealthIssue = findViewById(R.id.tvHealthIssue);
        tvAvailableTime = findViewById(R.id.tvAvailableTime);
        tvDayPerWeek = findViewById(R.id.tvDayPerWeek);

        // View/Edit Mode Containers
        llHeightView = findViewById(R.id.llHeightView);
        llWeightView = findViewById(R.id.llWeightView);
        llGoalWeightView = findViewById(R.id.llGoalWeightView);
        llDOBView = findViewById(R.id.llDOBView);
        llGenderView = findViewById(R.id.llGenderView);
        llFitnessGoalsView = findViewById(R.id.llFitnessGoalsView);

        tilHeight = findViewById(R.id.tilHeight);
        tilWeight = findViewById(R.id.tilWeight);
        tilGoalWeight = findViewById(R.id.tilGoalWeight);
        tilDOB = findViewById(R.id.tilDOB);
        llGenderEdit = findViewById(R.id.llGenderEdit);
        llFitnessGoalsEdit = findViewById(R.id.llFitnessGoalsEdit);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
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
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Already on profile page
                return true;
            }
            return false;
        });
    }

    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = firebaseHelper.getCurrentUserId();
        
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseHelper.getCurrentUser(userId, new DataCallBack<User>() {
            @Override
            public void onSuccess(User user) {
                progressBar.setVisibility(View.GONE);
                currentUser = user;
                displayUserInfo(user);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserInfo(User user) {
        if (user == null) return;

        // Email
        String email = firebaseHelper.getCurrentUserMail();
        if (email != null) {
            tvEmail.setText(email);
        }

        // Height - View & Edit
        if (user.height != null) {
            tvHeight.setText(user.height + " cm");
            edtHeight.setText(String.valueOf(user.height));
        } else {
            tvHeight.setText("-- cm");
        }

        // Weight - View & Edit
        if (user.weight != null) {
            tvWeight.setText(user.weight + " kg");
            edtWeight.setText(String.valueOf(user.weight));
        } else {
            tvWeight.setText("-- kg");
        }

        // Goal Weight - View & Edit
        if (user.goalWeight != null) {
            tvGoalWeight.setText(user.goalWeight + " kg");
            edtGoalWeight.setText(String.valueOf(user.goalWeight));
        } else {
            tvGoalWeight.setText("-- kg");
        }

        // DOB - View & Edit
        if (user.Dob != null && !user.Dob.isEmpty()) {
            tvDOB.setText(user.Dob);
            edtDOB.setText(user.Dob);
        } else {
            tvDOB.setText("--");
        }

        // Gender - View & Edit
        if (user.gender != null) {
            tvGender.setText(user.gender);
            if ("Male".equalsIgnoreCase(user.gender)) {
                rbMale.setChecked(true);
            } else if ("Female".equalsIgnoreCase(user.gender)) {
                rbFemale.setChecked(true);
            }
        } else {
            tvGender.setText("--");
        }

        // Goal - View & Edit
        if (user.goal != null && !user.goal.isEmpty()) {
            tvGoal.setText(user.goal);
            if ("Lose Weight".equals(user.goal)) {
                rbLoseWeight.setChecked(true);
            } else if ("Gain Muscle".equals(user.goal)) {
                rbGainMuscle.setChecked(true);
            } else if ("Maintain Fitness".equals(user.goal)) {
                rbMaintainFitness.setChecked(true);
            } else if ("Increase Stamina".equals(user.goal)) {
                rbIncreaseStamina.setChecked(true);
            }
        } else {
            tvGoal.setText("--");
        }

        // Experience (level) - View & Edit
        String expText = "--";
        if (user.level != null) {
            if (user.level == 1) {
                expText = "Beginner";
                rbBeginner.setChecked(true);
            } else if (user.level == 2) {
                expText = "Intermediate";
                rbIntermediate.setChecked(true);
            } else if (user.level == 3) {
                expText = "Advanced";
                rbAdvanced.setChecked(true);
            }
        }
        tvExperience.setText(expText);

        // Focus Area - View & Edit
        if (user.focusArea != null && !user.focusArea.isEmpty()) {
            tvFocusArea.setText(user.focusArea);
            if ("Full Body".equals(user.focusArea)) {
                rbFullBody.setChecked(true);
            } else if ("Upper Body".equals(user.focusArea)) {
                rbUpperBody.setChecked(true);
            } else if ("Lower Body".equals(user.focusArea)) {
                rbLowerBody.setChecked(true);
            } else if ("Core/Abs".equals(user.focusArea)) {
                rbCore.setChecked(true);
            }
        } else {
            tvFocusArea.setText("--");
        }

        // Health Issue - View & Edit
        if (user.healthIssue != null && !user.healthIssue.isEmpty()) {
            String healthText = user.healthIssue;
            if (healthText.contains("CardioIsssues")) {
                healthText = "Heart or Cardio Issues";
            }
            tvHealthIssue.setText(healthText);
            if ("No Issues".equals(user.healthIssue)) {
                rbNoIssues.setChecked(true);
            } else if ("Joint, Knee Pain".equals(user.healthIssue)) {
                rbJointPain.setChecked(true);
            } else if ("Back Pain".equals(user.healthIssue)) {
                rbBackPain.setChecked(true);
            } else if ("Heart or CardioIsssues".equals(user.healthIssue) || "Heart or CardioIssues".equals(user.healthIssue)) {
                rbCardioIssues.setChecked(true);
            }
        } else {
            tvHealthIssue.setText("--");
        }

        // Available Time - View & Edit
        String timeText = "--";
        if (user.availableTime != null) {
            if (user.availableTime == 15) {
                timeText = "15 minutes";
                rb15Min.setChecked(true);
            } else if (user.availableTime == 30) {
                timeText = "30 minutes";
                rb30Min.setChecked(true);
            } else if (user.availableTime == 45) {
                timeText = "45 minutes";
                rb45Min.setChecked(true);
            } else if (user.availableTime >= 60) {
                timeText = "60+ minutes";
                rb60MinPlus.setChecked(true);
            }
        }
        tvAvailableTime.setText(timeText);

        // Days per week - View & Edit
        if (user.dayPerWeek != null) {
            tvDayPerWeek.setText(user.dayPerWeek + " days/week");
            if (user.dayPerWeek == 2) {
                rb2Days.setChecked(true);
            } else if (user.dayPerWeek == 3) {
                rb3Days.setChecked(true);
            } else if (user.dayPerWeek == 4) {
                rb4Days.setChecked(true);
            } else if (user.dayPerWeek == 5) {
                rb5Days.setChecked(true);
            } else if (user.dayPerWeek == 6) {
                rb6Days.setChecked(true);
            }
        } else {
            tvDayPerWeek.setText("--");
        }
    }

    private void setupEditButton() {
        btnEdit.setOnClickListener(v -> {
            if (isEditMode) {
                setViewMode();
            } else {
                setEditMode();
            }
        });
    }

    private void setViewMode() {
        isEditMode = false;
        btnEdit.setText("Edit Profile");
        btnSave.setVisibility(View.GONE);

        // Show view mode, hide edit mode
        llHeightView.setVisibility(View.VISIBLE);
        llWeightView.setVisibility(View.VISIBLE);
        llGoalWeightView.setVisibility(View.VISIBLE);
        llDOBView.setVisibility(View.VISIBLE);
        llGenderView.setVisibility(View.VISIBLE);
        llFitnessGoalsView.setVisibility(View.VISIBLE);

        tilHeight.setVisibility(View.GONE);
        tilWeight.setVisibility(View.GONE);
        tilGoalWeight.setVisibility(View.GONE);
        tilDOB.setVisibility(View.GONE);
        llGenderEdit.setVisibility(View.GONE);
        llFitnessGoalsEdit.setVisibility(View.GONE);
    }

    private void setEditMode() {
        isEditMode = true;
        btnEdit.setText("Cancel");
        btnSave.setVisibility(View.VISIBLE);

        // Hide view mode, show edit mode
        llHeightView.setVisibility(View.GONE);
        llWeightView.setVisibility(View.GONE);
        llGoalWeightView.setVisibility(View.GONE);
        llDOBView.setVisibility(View.GONE);
        llGenderView.setVisibility(View.GONE);
        llFitnessGoalsView.setVisibility(View.GONE);

        tilHeight.setVisibility(View.VISIBLE);
        tilWeight.setVisibility(View.VISIBLE);
        tilGoalWeight.setVisibility(View.VISIBLE);
        tilDOB.setVisibility(View.VISIBLE);
        llGenderEdit.setVisibility(View.VISIBLE);
        llFitnessGoalsEdit.setVisibility(View.VISIBLE);
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            saveProfile();
            setViewMode(); // Return to view mode after saving
        });
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v -> {
            // Đăng xuất
            firebaseHelper.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Chuyển về màn hình đăng nhập
            Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveProfile() {
        if (currentUser == null) {
            Toast.makeText(this, "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate and update height
        String heightStr = edtHeight.getText().toString().trim().replace(',', '.');
        if (!heightStr.isEmpty()) {
            try {
                float height = Float.parseFloat(heightStr);
                if (height > 0) {
                    currentUser.height = height;
                } else {
                    Toast.makeText(this, "Height must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid height value", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate and update weight
        String weightStr = edtWeight.getText().toString().trim().replace(',', '.');
        if (!weightStr.isEmpty()) {
            try {
                float weight = Float.parseFloat(weightStr);
                if (weight > 0) {
                    currentUser.weight = weight;
                } else {
                    Toast.makeText(this, "Weight must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid weight value", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate and update goal weight (như trong GoalInfor)
        String goalWeightStr = edtGoalWeight.getText().toString().trim().replace(',', '.');
        if (!goalWeightStr.isEmpty()) {
            try {
                float goalWeight = Float.parseFloat(goalWeightStr);
                if (goalWeight <= 0) {
                    Toast.makeText(this, "Goal weight must be greater than 0!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Validate: Nếu goal là "Lose Weight" thì goalWeight phải < weight
                int selectedGoalId = rgGoal.getCheckedRadioButtonId();
                if (selectedGoalId == R.id.rbLoseWeight && currentUser.weight != null) {
                    if (goalWeight >= currentUser.weight) {
                        Toast.makeText(this, "Goal weight must be lower than your current weight!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                currentUser.goalWeight = goalWeight;
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Update DOB
        String dob = edtDOB.getText().toString().trim();
        if (!dob.isEmpty()) {
            currentUser.Dob = dob;
        }

        // Update gender
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rbMale) {
            currentUser.gender = "Male";
        } else if (selectedId == R.id.rbFemale) {
            currentUser.gender = "Female";
        }

        // Lưu giá trị cũ để so sánh
        String oldGoal = currentUser.goal;
        String oldHealthIssue = currentUser.healthIssue;
        Float oldWeight = currentUser.weight;
        Float oldHeight = currentUser.height;

        // Update Goal
        int selectedGoalId = rgGoal.getCheckedRadioButtonId();
        if (selectedGoalId == R.id.rbLoseWeight) {
            currentUser.goal = "Lose Weight";
        } else if (selectedGoalId == R.id.rbGainMuscle) {
            currentUser.goal = "Gain Muscle";
        } else if (selectedGoalId == R.id.rbMaintainFitness) {
            currentUser.goal = "Maintain Fitness";
        } else if (selectedGoalId == R.id.rbIncreaseStamina) {
            currentUser.goal = "Increase Stamina";
        }

        // Update Experience (level)
        int selectedExpId = rgExperience.getCheckedRadioButtonId();
        if (selectedExpId == R.id.rbBeginner) {
            currentUser.level = 1;
        } else if (selectedExpId == R.id.rbIntermediate) {
            currentUser.level = 2;
        } else if (selectedExpId == R.id.rbAdvanced) {
            currentUser.level = 3;
        }

        // Update Focus Area
        int selectedFocusId = rgFocusArea.getCheckedRadioButtonId();
        if (selectedFocusId == R.id.rbFullBody) {
            currentUser.focusArea = "Full Body";
        } else if (selectedFocusId == R.id.rbUpperBody) {
            currentUser.focusArea = "Upper Body";
        } else if (selectedFocusId == R.id.rbLowerBody) {
            currentUser.focusArea = "Lower Body";
        } else if (selectedFocusId == R.id.rbCore) {
            currentUser.focusArea = "Core/Abs";
        }

        // Update Health Issue
        int selectedHealthId = rgHealthIssue.getCheckedRadioButtonId();
        if (selectedHealthId == R.id.rbNoIssues) {
            currentUser.healthIssue = "No Issues";
        } else if (selectedHealthId == R.id.rbJointPain) {
            currentUser.healthIssue = "Joint, Knee Pain";
        } else if (selectedHealthId == R.id.rbBackPain) {
            currentUser.healthIssue = "Back Pain";
        } else if (selectedHealthId == R.id.rbCardioIssues) {
            currentUser.healthIssue = "Heart or CardioIsssues";
        }

        // Update Available Time
        int selectedTimeId = rgAvailableTime.getCheckedRadioButtonId();
        if (selectedTimeId == R.id.rb15Min) {
            currentUser.availableTime = 15;
        } else if (selectedTimeId == R.id.rb30Min) {
            currentUser.availableTime = 30;
        } else if (selectedTimeId == R.id.rb45Min) {
            currentUser.availableTime = 45;
        } else if (selectedTimeId == R.id.rb60MinPlus) {
            currentUser.availableTime = 61;
        }

        // Update Days per week
        int selectedDayId = rgDayPerWeek.getCheckedRadioButtonId();
        if (selectedDayId == R.id.rb2Days) {
            currentUser.dayPerWeek = 2;
        } else if (selectedDayId == R.id.rb3Days) {
            currentUser.dayPerWeek = 3;
        } else if (selectedDayId == R.id.rb4Days) {
            currentUser.dayPerWeek = 4;
        } else if (selectedDayId == R.id.rb5Days) {
            currentUser.dayPerWeek = 5;
        } else if (selectedDayId == R.id.rb6Days) {
            currentUser.dayPerWeek = 6;
        }

        // Tính lại totalWorkoutDays dựa trên dayPerWeek và availableTime
        currentUser.totalWorkoutDays = ScheduleBuild.calculateTotalWorkoutDays(currentUser);

        // Save to Firebase
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        firebaseHelper.updateUser(currentUser, new Callback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                
                // Kiểm tra thay đổi quan trọng: Goal hoặc HealthIssue
                boolean goalChanged = (oldGoal != null && currentUser.goal != null && !oldGoal.equals(currentUser.goal));
                boolean healthIssueChanged = (oldHealthIssue != null && currentUser.healthIssue != null && !oldHealthIssue.equals(currentUser.healthIssue));
                
                // Kiểm tra nếu weight hoặc height thay đổi đáng kể (> 2kg hoặc > 5cm)
                boolean bodyChanged = false;
                if (oldWeight != null && currentUser.weight != null) {
                    if (Math.abs(currentUser.weight - oldWeight) > 2.0f) {
                        bodyChanged = true;
                    }
                }
                if (oldHeight != null && currentUser.height != null) {
                    if (Math.abs(currentUser.height - oldHeight) > 5.0f) {
                        bodyChanged = true;
                    }
                }

                // Logic: Nếu thay đổi Goal hoặc HealthIssue -> Rebuild schedule (reset về ngày 1)
                if (goalChanged || healthIssueChanged) {
                    Toast.makeText(ProfileActivity.this, 
                            "Important changes detected! Rebuilding workout plan from Day 1...", 
                            Toast.LENGTH_SHORT).show();
                    
                    firebaseHelper.rebuildScheduleAfterBodyChange(true, new Callback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ProfileActivity.this, 
                                    "✓ Workout plan rebuilt from Day 1 based on your new goals!", 
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(ProfileActivity.this, 
                                    "Profile updated, but schedule rebuild failed: " + errorMessage, 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } 
                // Nếu chỉ thay đổi body parameters -> Chỉ điều chỉnh
                else if (bodyChanged) {
                    Toast.makeText(ProfileActivity.this, 
                            "Adjusting workout plan...", 
                            Toast.LENGTH_SHORT).show();
                    
                    firebaseHelper.adjustAllExercisesByBodyChange(new Callback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ProfileActivity.this, 
                                    "✓ Workout plan adjusted based on your new body parameters", 
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(ProfileActivity.this, 
                                    "Profile updated, but schedule adjustment failed: " + errorMessage, 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } 
                // Các thay đổi khác (experience, focusArea, availableTime, dayPerWeek) -> Chỉ điều chỉnh nhẹ
                else {
                    // Kiểm tra xem có thay đổi các thông số khác không
                    boolean otherChanged = false;
                    // Note: Có thể thêm logic kiểm tra thay đổi experience, focusArea, etc.
                    
                    if (otherChanged) {
                        Toast.makeText(ProfileActivity.this, 
                                "✓ Profile updated. Workout plan will adjust gradually", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "✓ Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}


