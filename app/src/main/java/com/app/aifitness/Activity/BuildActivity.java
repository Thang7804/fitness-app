package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.BuildExLogic.Rules;
import com.app.aifitness.BuildExLogic.ScheduleBuild;
import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.Exercise;
import com.app.aifitness.Model.User;

import java.util.ArrayList;
import java.util.List;

public class BuildActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private Rules rules;
    private ScheduleBuild scheduler;
    private List<Exercise> allExercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseHelper = FirebaseHelper.getInstance();
        rules = new Rules();
        scheduler = new ScheduleBuild();

        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) {
            goToLogin();
            return;
        }

        firebaseHelper.getCurrentUser(userId, new DataCallBack<User>() {
            @Override
            public void onSuccess(User user) {
                Log.d("BuildActivity", "Fetched user: " + user.email);

                firebaseHelper.getAllExercises(new DataCallBack<List<Exercise>>() {
                    @Override
                    public void onSuccess(List<Exercise> exercises) {
                        allExercises.clear();
                        allExercises.addAll(exercises);
                        List<Exercise> recommended = rules.filterRecommended(user, allExercises);
                        if (user.currentDay == null) user.currentDay = 0;
                        for(int i=0; i<30; i++){
                            scheduler.buildNextDay(user, recommended);
                        }
                        firebaseHelper.updateUser(user, new Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d("BuildActivity", "User schedule updated.");
                                goToMain();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e("BuildActivity", "Update failed: " + errorMessage);
                                goToMain();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e("BuildActivity", "Load exercises failed: " + errorMessage);
                        goToMain();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("BuildActivity", "Fetch user failed: " + errorMessage);
                goToLogin();
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(BuildActivity.this, MainActivity.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(BuildActivity.this, SignInActivity.class));
        finish();
    }
}
