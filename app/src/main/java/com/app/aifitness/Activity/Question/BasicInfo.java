package com.app.aifitness.Activity.Question;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.User;
import com.app.aifitness.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BasicInfo extends AppCompatActivity {
    private EditText edtHeight, edtWeight;
    private RadioGroup rgGender;

    private DatePicker datePickerDOB;
    private Button btnNext;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.basicinfo);
        edtHeight = findViewById(R.id.edtHeight);
        edtWeight = findViewById(R.id.edtWeight);
        rgGender = findViewById(R.id.rgGender);
        datePickerDOB = findViewById(R.id.datePickerDOB);
        btnNext = findViewById(R.id.btnNextBasic);
        String email=FirebaseHelper.getInstance().getCurrentUserMail();
        currentUser = new User(email);
        btnNext.setOnClickListener(v->{
            String heightStr = edtHeight.getText().toString().trim().replace(',', '.');
            String weightStr = edtWeight.getText().toString().trim().replace(',', '.');

            if (heightStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please enter full height and weight!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                float height = Float.parseFloat(heightStr);
                float weight = Float.parseFloat(weightStr);

                if (height <= 0 || weight <= 0) {
                    Toast.makeText(this, "Height and weight must be greater than 0!", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentUser.height = height;
                currentUser.weight = weight;

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId == R.id.rbMale) {
                currentUser.gender = "Male";
            } else if (selectedId == R.id.rbFemale) {
                currentUser.gender = "Female";
            } else {
                Toast.makeText(this, "Please select your gender!", Toast.LENGTH_SHORT).show();
                return;
            }

            int day = datePickerDOB.getDayOfMonth();
            int month = datePickerDOB.getMonth();
            int year = datePickerDOB.getYear();

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            currentUser.Dob = sdf.format(calendar.getTime());


            Intent intent = new Intent(BasicInfo.this, GoalInfor.class);
            intent.putExtra("user", currentUser);
            startActivity(intent);
        });
    }
}