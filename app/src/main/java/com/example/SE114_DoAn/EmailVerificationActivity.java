package com.example.SE114_DoAn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmailVerificationActivity extends AppCompatActivity {

    private Button resendCodeBtn;
    private TextView toLoginText;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();

        resendCodeBtn = findViewById(R.id.resendCodeBtn);
        toLoginText = findViewById(R.id.toLoginText);

        resendCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.getCurrentUser().sendEmailVerification()
                        .addOnSuccessListener(unused -> Toast.makeText(EmailVerificationActivity.this, "Verification Email đã được gửi", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Log.d("REGISTER", "Verification Email không được gửi", e));
            }
        });

        toLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(EmailVerificationActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}