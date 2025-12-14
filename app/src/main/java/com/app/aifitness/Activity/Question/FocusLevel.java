package com.app.aifitness.Activity.Question;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
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

public class FocusLevel extends AppCompatActivity {
    private RadioGroup rgFocus;
    private RadioGroup rgLevel;
    private Button btnNext;
    private TextView btnBack ;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.focuslevel);
        rgFocus = findViewById(R.id.rgBodyFocus);
        rgLevel= findViewById(R.id.rgFitnessLevel);
        btnNext = findViewById(R.id.btnNextFocusLevel);
        btnBack = findViewById(R.id.btnBack);
        currentUser = (User) getIntent().getSerializableExtra("user");
        btnBack.setOnClickListener(v->{
            Intent intent = new Intent(FocusLevel.this, SheduleInfo.class);
            startActivity(intent);
        });
        btnNext.setOnClickListener(v->{
            int selectedId1 = rgFocus.getCheckedRadioButtonId();
            if (selectedId1 == R.id.rbFullBody) {
                currentUser.focusArea = "Full Body";
            } else if (selectedId1 == R.id.rbUpperBody) {
                currentUser.focusArea = "Upper Body";
            }  else if (selectedId1 == R.id.rbLowerBody){
                currentUser.focusArea="Lower Body";
            } else if (selectedId1 == R.id.rbCore){
                currentUser.focusArea="Core/Abs";
            }
            else {
                Toast.makeText(this, "Please select!", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedId2 = rgLevel.getCheckedRadioButtonId();
            if (selectedId2 == R.id.rbBeginner) {
                currentUser.level = 1;
            } else if (selectedId2 == R.id.rbIntermediate) {
                currentUser.level = 2;
            }  else if (selectedId2 == R.id.rbAdvanced){
                currentUser.level=3;
            }
            else {
                Toast.makeText(this, "Please select!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(FocusLevel.this, EquipHeatlth.class);
            intent.putExtra("user", currentUser);
            startActivity(intent);

        });
    }
}