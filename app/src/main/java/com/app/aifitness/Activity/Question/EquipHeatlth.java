package com.app.aifitness.Activity.Question;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Activity.BuildActivity;
import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.User;
import com.app.aifitness.R;

public class EquipHeatlth extends AppCompatActivity {
    private RadioGroup rgEquip;
    private RadioGroup rgHealth;
    private Button btnSubmit;
    private TextView btnBack ;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.equiphealth);
        rgEquip= findViewById(R.id.rgEquipment);
        rgHealth= findViewById(R.id.rgHealthIssues);
        btnSubmit= findViewById(R.id.btnFinishSetup);
        btnBack= findViewById(R.id.btnBack);
        currentUser=(User) getIntent().getSerializableExtra("user");
        btnBack.setOnClickListener(v->{
            Intent intent = new Intent(EquipHeatlth.this, FocusLevel.class);
            startActivity(intent);
        });
        btnSubmit.setOnClickListener(v->{
            int selectedId1 = rgEquip.getCheckedRadioButtonId();
            if (selectedId1 == R.id.rbNoEquipment) {
                currentUser.hasEquipment = false;
            } else if (selectedId1 == R.id.rbHaveDumbbell) {
                currentUser.hasEquipment = true;
            }
            else {
                Toast.makeText(this, "Please select!", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedId2 = rgHealth.getCheckedRadioButtonId();
            if (selectedId2 == R.id.rbNoIssues) {
                currentUser.healthIssue = "No Issues";
            } else if (selectedId2 == R.id.rbJointPain) {
                currentUser.healthIssue = "Joint, Knee Pain";
            }  else if (selectedId2 == R.id.rbBackPain){
                currentUser.healthIssue="Back Pain";
            } else if (selectedId2== R.id.rbCardioIssues) {
                currentUser.healthIssue="Heart or CardioIsssues";
            } else {
                Toast.makeText(this, "Please select!", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseHelper.getInstance().updateUser(currentUser, new Callback() {
                @Override
                public void onSuccess() {
                    FirebaseHelper.getInstance().updateIsNewStatus(FirebaseHelper.getInstance().getCurrentUserId(),false,new Callback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(EquipHeatlth.this,"Success save user info", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(EquipHeatlth.this, BuildActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        @Override
                        public void onError(String error) {
                            Toast.makeText(EquipHeatlth.this,error, Toast.LENGTH_SHORT).show();
                        }
                    });

                }

                @Override
                public void onError(String error) {
                    Toast.makeText(EquipHeatlth.this,error, Toast.LENGTH_SHORT).show();
                }
            });

        });
    }
}