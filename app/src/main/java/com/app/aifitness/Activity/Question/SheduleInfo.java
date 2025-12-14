package com.app.aifitness.Activity.Question;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.app.aifitness.Model.User;
import com.app.aifitness.R;

public class SheduleInfo extends AppCompatActivity {
    private RadioGroup rgDayPerWeek;
    private RadioGroup rgWorkoutTime;
    private Button btnSubmit;
    private TextView btnBack ;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scheduleinfo);

        rgDayPerWeek = findViewById(R.id.rgDaysPerWeek);
        rgWorkoutTime = findViewById(R.id.rgWorkoutTime);
        btnSubmit = findViewById(R.id.btnNextSchedule);
        btnBack = findViewById(R.id.btnBack);
        currentUser =(User) getIntent().getSerializableExtra("user");
        btnBack.setOnClickListener(v->{
            Intent intent = new Intent(SheduleInfo.this, GoalInfor.class);
            startActivity(intent);
                });
        btnSubmit.setOnClickListener(v->{
            int selectedId1 = rgDayPerWeek.getCheckedRadioButtonId();
            if (selectedId1 == R.id.rb2Days) {
                currentUser.dayPerWeek = 2;
            } else if (selectedId1 == R.id.rb3Days) {
                currentUser.dayPerWeek = 3;
            }  else if (selectedId1 == R.id.rb4Days){
                currentUser.dayPerWeek=4;
            } else if (selectedId1 == R.id.rb5Days){
                currentUser.dayPerWeek=5;
            }else if (selectedId1 == R.id.rb6Days) {
                currentUser.dayPerWeek = 6;
            }
            else {
                Toast.makeText(this, "Please select!", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedId2 = rgWorkoutTime.getCheckedRadioButtonId();
            if (selectedId2 == R.id.rb15Min) {
                currentUser.availableTime = 15;
            } else if (selectedId2 == R.id.rb30Min) {
                currentUser.availableTime = 30;
            }  else if (selectedId2 == R.id.rb45Min){
                currentUser.availableTime=45;
            } else if (selectedId2== R.id.rb60MinPlus) {
                currentUser.availableTime=61;
            } else {
                Toast.makeText(this, "Please select!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(SheduleInfo.this, FocusLevel.class);
            intent.putExtra("user", currentUser);
            startActivity(intent);
        });
    }
}