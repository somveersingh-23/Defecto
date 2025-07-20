package com.crriccn.defecto;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ProfileInfoActivity extends AppCompatActivity {

    private TextView tvUsername, tvEmail, tvPhone, tvUserType, tvAddress, tvUserId;
    private Button btnEditProfile;
    private BottomNavigationView bottomNavigationView;
    private ImageView profileImage;

    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profileinfo);

        initializeViews();
        setupBottomNavigation();

        firestore = FirebaseFirestore.getInstance();

        // Retrieve user ID from SharedPreferences
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userId", null);

        if (userId != null) {
            fetchUserData(userId);
        } else {
            // Redirect to login if user ID is not found
            startActivity(new Intent(ProfileInfoActivity.this, LoginActivity.class));
            finish();
        }

        btnEditProfile.setOnClickListener(view ->
                startActivity(new Intent(ProfileInfoActivity.this, EditProfileActivity.class))
        );
    }

    private void initializeViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvUserType = findViewById(R.id.tvUserType);
        tvAddress = findViewById(R.id.tvAddress);
        tvUserId = findViewById(R.id.tvUserId);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        profileImage = findViewById(R.id.profile_image);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileInfoActivity.this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_logout) {
                // Clear user ID from SharedPreferences
                getSharedPreferences("user_prefs", MODE_PRIVATE).edit().remove("userId").apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void fetchUserData(String uid) {
        if (listenerRegistration != null) listenerRegistration.remove();

        listenerRegistration = firestore.collection("users")
                .document(uid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) return;

                    tvUsername.setText(documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName"));
                    tvEmail.setText("Email: " + documentSnapshot.getString("email"));
                    tvPhone.setText("Phone: " + documentSnapshot.getString("phone"));
                    tvUserType.setText("User Type: " + documentSnapshot.getString("userType"));
                    tvAddress.setText("Address: " + documentSnapshot.getString("address"));

                    // 🔁 Use custom display ID instead of raw UID
                    String displayId = documentSnapshot.getString("displayUserId");
                    if (displayId != null && !displayId.isEmpty()) {
                        tvUserId.setText("User ID: " + displayId);
                    } else {
                        tvUserId.setText("User ID: " + uid); // fallback
                    }

                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ProfileInfoActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .into(profileImage);
                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
