package com.example.SE114_DoAn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView backToLoginText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Thêm biến cho Firestore
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Khởi tạo Firestore
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        backToLoginText = findViewById(R.id.back_to_login_text);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d("REGISTER", "Tạo tài khoản Auth thành công");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // ---- PHẦN THÊM MỚI  ----
                                // Tạo một đối tượng User mới và lưu vào Firestore
                                String uid = firebaseUser.getUid();
                                // Lấy phần tên từ email làm username mặc định
                                String username = email.substring(0, email.indexOf('@'));
                                User newUser = new User(username, email);

                                db.collection("User").document(uid).set(newUser)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("REGISTER", "Lưu thông tin User vào Firestore thành công");
                                            // Chuyển sang MainActivity sau khi đã lưu xong
                                            navigateToMain();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("REGISTER", "Lỗi khi lưu User vào Firestore", e);
                                            Toast.makeText(RegisterActivity.this, "Lỗi khi tạo hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                                        });
                                // ---- KẾT THÚC PHẦN THÊM MỚI ----
                            }
                        } else {
                            Log.e("REGISTER", "Tạo tài khoản thất bại", task.getException());
                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        backToLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void navigateToMain() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.apply();
        Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}