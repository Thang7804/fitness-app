package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.app.aifitness.R;

public class RestDayActivity extends AppCompatActivity {

        private Button btnBack;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_rest_day);

            btnBack = findViewById(R.id.btnBack);

            btnBack.setOnClickListener(v -> {

                Intent intent = new Intent(RestDayActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
}