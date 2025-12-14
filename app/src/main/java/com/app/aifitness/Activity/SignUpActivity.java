package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.R;

public class SignUpActivity extends AppCompatActivity {
    private EditText edtEmail, edtPassword, edtRPassword;
    private Button btnSignIn, btnSignUp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);
        edtEmail = findViewById(R.id.Email);
        edtPassword = findViewById(R.id.Password);
        edtRPassword = findViewById(R.id.RPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String rpassword = edtRPassword.getText().toString().trim();
            if(email.isEmpty() || password.isEmpty() || rpassword.isEmpty()){
                Toast.makeText(this, "Please fill", Toast.LENGTH_SHORT).show();
                return;
            }
            if(!password.equals(rpassword)){
                Toast.makeText(this, "Password and RepeatPassword is not match", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseHelper.getInstance().registerUser(email, password, new Callback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(SignUpActivity.this, "Sign Up Succressfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(SignUpActivity.this, error, Toast.LENGTH_SHORT).show();
                    return;
                }
            });
        });
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
        });
    }
}