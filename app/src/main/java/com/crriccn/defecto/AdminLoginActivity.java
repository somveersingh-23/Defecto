package com.crriccn.defecto;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private ImageView ivTogglePwd, icBack;
    private boolean isPasswordVisible = false;
    private FirebaseFirestore db;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminlogin);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ivTogglePwd = findViewById(R.id.ivTogglePwd);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        icBack = findViewById(R.id.icBack);

        db = FirebaseFirestore.getInstance();

        icBack.setOnClickListener(v -> finish());

        ivTogglePwd.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePwd.setImageResource(R.drawable.ic_eye_hide);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePwd.setImageResource(R.drawable.ic_eye_show);
            }
            isPasswordVisible = !isPasswordVisible;
            etPassword.setSelection(etPassword.getText().length());
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(AdminLoginActivity.this, ForgotAdminPasswordActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoadingDialog("Logging in...");

            db.collection("admin")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        dismissLoadingDialog();

                        if (!queryDocumentSnapshots.isEmpty()) {
                            String adminId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            String storedHashedPassword = queryDocumentSnapshots.getDocuments().get(0).getString("password");

                            if (storedHashedPassword != null && BCrypt.checkpw(password, storedHashedPassword)) {
                                Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(AdminLoginActivity.this, ViewAdminPanelActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No admin found with this email", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        dismissLoadingDialog();
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void showLoadingDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.progress_dialog);
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
