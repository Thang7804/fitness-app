package com.app.aifitness.Activity.Question;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Model.User;
import com.app.aifitness.R;
import com.google.android.material.textview.MaterialTextView;

public class GoalInfor extends AppCompatActivity {
    private RadioGroup rgGoal;
    private EditText edtGoalWeight;
    private Button btnSubmit;
    private TextView btnBack ;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.goalinfo);
        rgGoal = findViewById(R.id.rgFitnessGoal);
        edtGoalWeight= findViewById(R.id.edtTargetWeight);
        btnSubmit = findViewById(R.id.btnNextGoal);
        btnBack = findViewById(R.id.btnBack);
        currentUser = (User) getIntent().getSerializableExtra("user") ;
        btnBack.setOnClickListener(v->{
            Intent intent = new Intent(GoalInfor.this, BasicInfo.class);
            startActivity(intent);
        });
        btnSubmit.setOnClickListener(v->{
            int selectedId = rgGoal.getCheckedRadioButtonId();
            if (selectedId == R.id.rbLoseWeight) {
                currentUser.goal = "Lose Weight";
            } else if (selectedId == R.id.rbGainMuscle) {
                currentUser.goal = "Gain Muscle";
            }  else if (selectedId == R.id.rbMaintainFitness){
                currentUser.goal="Maintain Fitness";
            } else if (selectedId == R.id.rbIncreaseStamina){
                currentUser.goal="Increase Stamina";
            }
            else {
                Toast.makeText(this, "Please select!", Toast.LENGTH_SHORT).show();
                return;
            }
            String goalWeightstr = edtGoalWeight.getText().toString().trim().replace(',', '.');;
            try {
                float goalWeight = Float.parseFloat(goalWeightstr);

                if ( goalWeight<= 0) {
                    Toast.makeText(this, "Height and weight must be greater than 0!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(selectedId== R.id.rbLoseWeight && goalWeight>= currentUser.weight){
                    Toast.makeText(this, "Height and weight must be lower than your weight!", Toast.LENGTH_SHORT).show();
                }

                currentUser.goalWeight = goalWeight;

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(GoalInfor.this, SheduleInfo.class);
            intent.putExtra("user", currentUser);
            startActivity(intent);

        });
    }
}