package com.crriccn.defecto;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import org.mindrot.jbcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginBtn, adminLoginBtn;
    private ProgressBar progressBar;
    private TextView signup, tvForgotPassword;
    private ImageView togglePwdIcon;
    private boolean isPasswordVisible = false;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.etEmail);
        passwordInput = findViewById(R.id.etPassword);
        loginBtn = findViewById(R.id.btnLogin);
        adminLoginBtn = findViewById(R.id.btnAdminLogin);
        progressBar = findViewById(R.id.progressBar);
        signup = findViewById(R.id.signup);
        togglePwdIcon = findViewById(R.id.ivTogglePwd);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        db = FirebaseFirestore.getInstance();

        togglePwdIcon.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePwdIcon.setImageResource(R.drawable.ic_eye_hide);
            } else {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePwdIcon.setImageResource(R.drawable.ic_eye_show);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordInput.setSelection(passwordInput.getText().length());
        });

        signup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        adminLoginBtn.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, AdminLoginActivity.class));
        });

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            String storedHashedPassword = task.getResult().getDocuments().get(0).getString("password");
                            String userId = task.getResult().getDocuments().get(0).getId();

                            if (storedHashedPassword != null && BCrypt.checkpw(password, storedHashedPassword)) {
                                // Store user ID in SharedPreferences
                                getSharedPreferences("user_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("userId", userId)
                                        .apply();

                                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No user found with this email", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }
}
