package com.crriccn.defecto;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.drawable.ColorDrawable;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.view.LayoutInflater;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText firstName, lastName, email, phone, otp, password;
    private Button sendOtpBtn, submitBtn;
    private TextView loginRedirectView;
    private ProgressBar progressBar;
    private String generatedOtp = "";
    private long otpGeneratedTime = 0;
    private final int otpTimeLimit = 60 * 1000; // 60 seconds in milliseconds
    private boolean canResend = false;
    private ImageView eyeIcon;
    private boolean isPasswordVisible = false;
    private Handler otpHandler = new Handler();
    private String adminEmail;
    private String appPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        appPassword = MetaDataUtil.getMetaDataValue(this, "APP_PASSWORD");
        adminEmail = MetaDataUtil.getMetaDataValue(this, "ADMIN_EMAIL");
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        otp = findViewById(R.id.otp);
        password = findViewById(R.id.password);
        sendOtpBtn = findViewById(R.id.sendOtpBtn);
        submitBtn = findViewById(R.id.submitBtn);
        progressBar = findViewById(R.id.progressBar);
        loginRedirectView = findViewById(R.id.loginRedirect);
        eyeIcon = findViewById(R.id.eyeIcon);

        progressBar.setVisibility(View.GONE);



        eyeIcon.setOnClickListener(v -> {
            if (isPasswordVisible) {
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                eyeIcon.setImageResource(R.drawable.ic_eye_hide);
            } else {
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                eyeIcon.setImageResource(R.drawable.ic_eye_show);
            }
            password.setSelection(password.length());
            isPasswordVisible = !isPasswordVisible;
        });

        loginRedirectView.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );

        submitBtn.setOnClickListener(v -> {
            String userEmail = email.getText().toString().trim();

            if (userEmail.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            submitBtn.setEnabled(false);
            new Handler().postDelayed(() -> submitBtn.setEnabled(true), 10000);

            generatedOtp = String.valueOf((int) (Math.random() * 9000) + 1000);
            otpGeneratedTime = System.currentTimeMillis();

            progressBar.setVisibility(View.VISIBLE);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                showOtpDialog(userEmail);
            });

            new Thread(() -> GmailSender.sendEmail(
                    userEmail,
                    "Central Road Research Institute New Delhi - Your OTP Code",
                    "Welcome to the Defecto App of CSIR-CRRI New Delhi.\n\nPlease don't share the OTP with anyone.\n\nYour OTP is: " + generatedOtp,
                    adminEmail,
                    appPassword
            )).start();
        });
    }

    private void showOtpDialog(String email) {
        final EditText inputOtp = new EditText(this);
        inputOtp.setHint("Enter OTP");
        inputOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputOtp.setPadding(40, 40, 40, 40);
        inputOtp.setBackground(getRoundedBackground(Color.WHITE));

        final TextView timerView = new TextView(this);
        timerView.setPadding(20, 30, 20, 30);
        timerView.setGravity(Gravity.CENTER);

        final Button resendBtn = new Button(this);
        resendBtn.setText("Resend OTP");
        resendBtn.setEnabled(false);
        resendBtn.setTextColor(Color.WHITE);
        resendBtn.setBackgroundColor(Color.parseColor("#FF9800"));

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 40);
        dialogLayout.setBackground(getRoundedBackground(Color.parseColor("#E3F2FD")));
        dialogLayout.addView(inputOtp);
        dialogLayout.addView(timerView);
        dialogLayout.addView(resendBtn);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("OTP Sent to " + email)
                .setView(dialogLayout)
                .setCancelable(false)
                .setPositiveButton("Verify", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            Button verifyBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            verifyBtn.setTextColor(Color.WHITE);
            verifyBtn.setBackgroundColor(Color.parseColor("#4CAF50"));

            cancelBtn.setTextColor(Color.WHITE);
            cancelBtn.setBackgroundColor(Color.parseColor("#F44336"));

            verifyBtn.setOnClickListener(v -> {
                String enteredOtp = inputOtp.getText().toString().trim();
                long currentTime = System.currentTimeMillis();
                if (currentTime - otpGeneratedTime > otpTimeLimit) {
                    Toast.makeText(this, "OTP expired. Please resend OTP.", Toast.LENGTH_SHORT).show();
                } else if (enteredOtp.equals(generatedOtp)) {
                    dialog.dismiss();
                    showOtpSuccessDialog();
                } else {
                    Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
        startOtpTimer(timerView, resendBtn, email);
        resendBtn.setOnClickListener(v -> {
            if (canResend) {
                resendOtp(email);
                resendBtn.setEnabled(false);
                startOtpTimer(timerView, resendBtn, email);
            }
        });
    }

    private void startOtpTimer(TextView timerView, Button resendBtn, String email) {
        new Thread(() -> {
            canResend = false;
            for (int i = otpTimeLimit / 1000; i >= 0; i--) {
                int finalI = i;
                runOnUiThread(() -> timerView.setText("Resend OTP in " + finalI + " seconds"));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            runOnUiThread(() -> {
                canResend = true;
                resendBtn.setEnabled(true);
                timerView.setText("You can resend the OTP now.");
            });
        }).start();
    }

    private void resendOtp(String email) {
        generatedOtp = String.valueOf((int) (Math.random() * 9000) + 1000);
        otpGeneratedTime = System.currentTimeMillis();

        new Thread(() -> {
            GmailSender.sendEmail(
                    email,
                    "Central Road Research Institute New Delhi - Your OTP Code",
                    "Your new OTP is: " + generatedOtp + "\nPlease do not share it with anyone.",
                    adminEmail,
                    appPassword
            );
            runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "OTP Resent to " + email, Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showOtpSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_otp_success, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView titleText = view.findViewById(R.id.titleText);
        TextView messageText = view.findViewById(R.id.messageText);
        ImageView successIcon = view.findViewById(R.id.successIcon);
        Button submitButton = view.findViewById(R.id.submitButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        titleText.setText("OTP Verified");
        messageText.setText("Your OTP is correct!\nCreating account...");
        successIcon.setImageResource(R.drawable.ic_success);

        submitButton.setOnClickListener(v -> {
            dialog.dismiss();
            storeUserInDatabase();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void storeUserInDatabase() {
        String fName = firstName.getText().toString().trim();
        String lName = lastName.getText().toString().trim();
        String userEmail = email.getText().toString().trim();
        String userPhone = phone.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty() || userEmail.isEmpty() || userPhone.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Hash the password before storing
        String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(userPassword, org.mindrot.jbcrypt.BCrypt.gensalt());

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        // Generate firstDigit and lastDigit
                        int firstDigit = fName.toLowerCase().charAt(0) % 10;
                        int lastDigit = lName.toLowerCase().charAt(lName.length() - 1) % 10;
                        int hash = Math.abs(uid.hashCode());
                        String middlePart = String.format("%010d", Long.parseLong(String.valueOf(hash).substring(0, Math.min(10, String.valueOf(hash).length()))));
                        String baseDisplayId = firstDigit + middlePart + lastDigit;

                        // Check uniqueness
                        FirebaseFirestore.getInstance().collection("users")
                                .whereEqualTo("displayUserId", baseDisplayId)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    String finalDisplayId = baseDisplayId;
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        // Collision detected - regenerate by appending a random digit
                                        int randomDigit = new java.util.Random().nextInt(10);
                                        finalDisplayId = baseDisplayId.substring(0, 11) + randomDigit;
                                    }

                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("firstName", fName);
                                    userData.put("lastName", lName);
                                    userData.put("email", userEmail);
                                    userData.put("phone", userPhone);
                                    userData.put("uid", uid); // Keep uid for your existing references
                                    userData.put("displayUserId", finalDisplayId); // Optional, just for display
                                    userData.put("password", hashedPassword);

                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(uid)
                                            .set(userData)
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(this, "User Registered Successfully", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(SignUpActivity.this,LoginActivity.class));
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Error storing user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to verify unique ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private GradientDrawable getRoundedBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(30f);
        drawable.setStroke(2, Color.GRAY);
        return drawable;
    }
}
