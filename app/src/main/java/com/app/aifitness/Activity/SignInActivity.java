package com.app.aifitness.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.aifitness.Activity.Question.BasicInfo;
import com.app.aifitness.Activity.Question.EquipHeatlth;
import com.app.aifitness.Firebase.Callback;
import com.app.aifitness.Firebase.DataCallBack;
import com.app.aifitness.Firebase.FirebaseHelper;
import com.app.aifitness.Model.User;
import com.app.aifitness.R;

public class SignInActivity extends AppCompatActivity {
    private EditText edtemail, edtpassword;
    private Button btnSignIn, btnSignUp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);
        
        edtemail = findViewById(R.id.Email);
        edtpassword= findViewById(R.id.Password);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp= findViewById(R.id.btnSignUp);
        
        // Kiểm tra xem user đã đăng nhập chưa (auto login)
        checkAutoLogin();
        btnSignIn.setOnClickListener(v-> {
                    String email = edtemail.getText().toString().trim();
                    String password = edtpassword.getText().toString().trim();
                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseHelper.getInstance().loginUser(email, password, new Callback() {
                        @Override
                        public void onSuccess() {
                            String userId = FirebaseHelper.getInstance().getCurrentUserId();
                            FirebaseHelper.getInstance().getCurrentUser(userId, new DataCallBack<User>() {
                                @Override
                                public void onSuccess(User user) {
                                    if (user.isNew) {
                                        Toast.makeText(SignInActivity.this, "Welcome new user!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignInActivity.this, BasicInfo.class));
                                    } else {
                                        Toast.makeText(SignInActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                    }
                                }
                                @Override
                                public void onError(String error) {
                                    Toast.makeText(SignInActivity.this, error, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(SignInActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
    
    /**
     * Kiểm tra xem user đã đăng nhập chưa và tự động đăng nhập nếu có
     */
    private void checkAutoLogin() {
        String userId = FirebaseHelper.getInstance().getCurrentUserId();
        if (userId != null) {
            // User đã đăng nhập, lấy thông tin user và chuyển đến màn hình phù hợp
            FirebaseHelper.getInstance().getCurrentUser(userId, new DataCallBack<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        if (user.isNew) {
                            // User mới, chuyển đến BasicInfo để điền thông tin
                            Intent intent = new Intent(SignInActivity.this, BasicInfo.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // User đã có thông tin, chuyển đến MainActivity
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    // Nếu không lấy được user, có thể session đã hết hạn
                    // Cho phép user đăng nhập lại
                }
            });
        }
        // Nếu userId == null, user chưa đăng nhập, hiển thị màn hình đăng nhập bình thường
    }
}