package com.crriccn.defecto;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ForgotAdminPasswordActivity extends AppCompatActivity {

    private EditText etForgotEmail;
    private ProgressBar progressBar;
    private Button btnFindAccount;
    private ImageView backLoginIc;

    // OTP management
    private String generatedOtp;
    private long otpGeneratedTime;
    private final int otpTimeout = 60_000;    // 60 seconds in milliseconds
    private CountDownTimer countDownTimer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String adminEmail;
    private String appPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        appPassword = MetaDataUtil.getMetaDataValue(this, "APP_PASSWORD");
        adminEmail = MetaDataUtil.getMetaDataValue(this, "ADMIN_EMAIL");

        backLoginIc     = findViewById(R.id.backLoginID);
        etForgotEmail   = findViewById(R.id.etEmail);
        progressBar     = findViewById(R.id.progressBarForgetPass);
        btnFindAccount  = findViewById(R.id.btnFindAccount);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        backLoginIc.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnFindAccount.setOnClickListener(v -> {
            String email = etForgotEmail.getText().toString().trim();
            if (email.isEmpty()) {
                etForgotEmail.setError("Enter your registered email");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            db.collection("admin")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(snap -> {
                        progressBar.setVisibility(View.GONE);
                        if (!snap.isEmpty()) {
                            sendOtpToEmail(email);
                        } else {
                            Toast.makeText(this, "Account not found!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void sendOtpToEmail(String email) {
        generatedOtp       = String.valueOf((int)(Math.random() * 9000 + 1000));
        otpGeneratedTime   = System.currentTimeMillis();

        new Thread(() -> {
            try {
                GmailSender.sendEmail(
                        email,
                        "CSIR-CRRI New Delhi – Defecto App Password Reset OTP",
                        "Please Don't share the OTP to anyone. " +
                                "Your OTP is: " + generatedOtp,
                        adminEmail,
                        appPassword
                );
                runOnUiThread(() -> showOtpDialog(email));
            } catch (Exception ex) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to send OTP: " + ex.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showOtpDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp, null);
        builder.setView(dialogView);
        AlertDialog otpDialog = builder.create();
        otpDialog.setCancelable(false);

        EditText  etOtp        = dialogView.findViewById(R.id.etOtp);
        TextView  tvOtpTimer   = dialogView.findViewById(R.id.tvOtpTimer);
        Button    btnResendOtp = dialogView.findViewById(R.id.btnResendOtp);
        Button    btnCancelOtp = dialogView.findViewById(R.id.btnCancelOtp);
        Button    btnVerifyOtp = dialogView.findViewById(R.id.btnVerifyOtp);

        btnResendOtp.setEnabled(false);
        startCountdown(tvOtpTimer, btnResendOtp);

        btnCancelOtp.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            otpDialog.dismiss();
        });

        btnVerifyOtp.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - otpGeneratedTime > otpTimeout) {
                Toast.makeText(this, "OTP expired. Please resend.", Toast.LENGTH_SHORT).show();
                return;
            }
            String entered = etOtp.getText().toString().trim();
            if (entered.equals(generatedOtp)) {
                otpDialog.dismiss();
                if (countDownTimer != null) countDownTimer.cancel();
                showResetPasswordDialog(email);
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });

        btnResendOtp.setOnClickListener(v -> {
            // regenerate & resend
            generatedOtp     = String.valueOf((int)(Math.random() * 9000 + 1000));
            otpGeneratedTime = System.currentTimeMillis();
            sendOtpToEmail(email);
            btnResendOtp.setEnabled(false);
            startCountdown(tvOtpTimer, btnResendOtp);
        });

        otpDialog.show();
    }

    private void startCountdown(TextView timerView, Button resendBtn) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(otpTimeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerView.setText("Resend OTP in: " + (millisUntilFinished/1000) + "s");
            }
            @Override
            public void onFinish() {
                resendBtn.setEnabled(true);
                resendBtn.setBackgroundColor(Color.parseColor("#FF9800"));
                timerView.setText("You can resend the OTP now.");
            }
        }.start();
    }

    private void showResetPasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null);
        builder.setView(dialogView);
        AlertDialog resetDialog = builder.create();
        resetDialog.setCancelable(false);

        EditText  etNewPwd      = dialogView.findViewById(R.id.etNewPassword);
        EditText  etConfirmPwd  = dialogView.findViewById(R.id.etConfirmPassword);
        ImageView ivToggle1     = dialogView.findViewById(R.id.ivTogglePwd1);
        ImageView ivToggle2     = dialogView.findViewById(R.id.ivTogglePwd2);
        Button    btnCancel     = dialogView.findViewById(R.id.btnCancelReset);
        Button    btnSubmit     = dialogView.findViewById(R.id.btnSubmitReset);

        ivToggle1.setOnClickListener(v -> togglePasswordVisibility(etNewPwd, ivToggle1));
        ivToggle2.setOnClickListener(v -> togglePasswordVisibility(etConfirmPwd, ivToggle2));

        btnCancel.setOnClickListener(v -> resetDialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String newPwd     = etNewPwd.getText().toString().trim();
            String confirmPwd = etConfirmPwd.getText().toString().trim();

            if (newPwd.isEmpty() || confirmPwd.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            } else if (!newPwd.equals(confirmPwd)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(newPwd, org.mindrot.jbcrypt.BCrypt.gensalt());

                db.collection("admin")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (!snapshot.isEmpty()) {
                                String docId = snapshot.getDocuments().get(0).getId();
                                Map<String, Object> updateMap = new HashMap<>();
                                updateMap.put("password", hashedPassword);

                                db.collection("admin").document(docId).update(updateMap)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                            resetDialog.dismiss();
                                            startActivity(new Intent(this, LoginActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                        );
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to fetch user: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
            }
        });

        resetDialog.show();
    }

    private void togglePasswordVisibility(EditText et, ImageView icon) {
        int type = et.getInputType();
        if ((type & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye_hide);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye_show);
        }
        et.setSelection(et.getText().length());
    }

    private GradientDrawable getRoundedBackground(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(20f);
        return gd;
    }
}
