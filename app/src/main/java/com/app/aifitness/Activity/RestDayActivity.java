package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.R;

public class RestDayActivity extends AppCompatActivity {

        private Button btnBack;
        private String dayName;
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_rest_day);

            btnBack = findViewById(R.id.btnBack);
            dayName = getIntent().getStringExtra("dayName");

            // Tự động tăng currentDay khi mở Rest day
            updateCurrentDayForRestDay();

            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(RestDayActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
        
        /**
         * Tự động tăng currentDay khi mở Rest day
         * Chỉ tăng nếu dayNumber > currentDay (logic này được xử lý trong updateCurrentDayIfNeeded)
         */
        private void updateCurrentDayForRestDay() {
            if (dayName == null || dayName.isEmpty()) {
                return;
            }
            
            // Parse dayNumber từ dayName (ví dụ: "day1" -> 1)
            int dayNumber;
            try {
                dayNumber = Integer.parseInt(dayName.replaceAll("\\D+", ""));
            } catch (NumberFormatException e) {
                return;
            }
            
            // Gọi updateCurrentDayIfNeeded - nó sẽ tự động kiểm tra dayNumber > currentDay
            FirebaseHelper.getInstance().updateCurrentDayIfNeeded(dayNumber, new Callback() {
                @Override
                public void onSuccess() {
                    // Silent success - không cần thông báo
                }
                
                @Override
                public void onError(String errorMessage) {
                    // Silent fail - không cần thông báo
                }
            });
        }
}