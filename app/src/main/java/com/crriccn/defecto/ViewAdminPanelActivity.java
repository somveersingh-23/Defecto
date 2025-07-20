package com.crriccn.defecto;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ViewAdminPanelActivity extends AppCompatActivity {
    private CardView cardViewallComplaints, cardManageComplaints, cardExportData;
    private BottomNavigationView bottomNavigationView;
    private TextView tvTotalUsers, tvTotalComplaints;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Link views
        cardViewallComplaints = findViewById(R.id.card_viewallcomplaints);
        cardManageComplaints = findViewById(R.id.card_manage_complaints);
        cardExportData = findViewById(R.id.card_exportdata);
        bottomNavigationView = findViewById(R.id.adminbottom_navigation);
        tvTotalUsers = findViewById(R.id.tv_total_users);
        tvTotalComplaints = findViewById(R.id.tv_total_complaints);

        // Set initial loading states
        tvTotalUsers.setText("Loading...");
        tvTotalComplaints.setText("Loading...");

        // Bottom navigation actions
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_logout) {
                    Intent intent = new Intent(ViewAdminPanelActivity.this, AdminLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        // Card click actions
        cardViewallComplaints.setOnClickListener(v -> startActivity(new Intent(ViewAdminPanelActivity.this, ViewAllComplaintActivity.class)));
        cardExportData.setOnClickListener(v -> startActivity(new Intent(ViewAdminPanelActivity.this, ExportComplaintsActivity.class)));
        cardManageComplaints.setOnClickListener(v -> startActivity(new Intent(ViewAdminPanelActivity.this, ManageComplaintsActivity.class)));

        // Fetch counts
        fetchTotalUsers();
        fetchTotalComplaints();
    }

    private void fetchTotalUsers() {
        db.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isFinishing()) {
                        int count = queryDocumentSnapshots.size();
                        tvTotalUsers.setText(String.valueOf(count));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        tvTotalUsers.setText("Error");
                        Toast.makeText(ViewAdminPanelActivity.this, "Failed to fetch user count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchTotalComplaints() {
        db.collection("Complaints")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isFinishing()) {
                        int count = queryDocumentSnapshots.size();
                        tvTotalComplaints.setText(String.valueOf(count));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        tvTotalComplaints.setText("Error");
                        Toast.makeText(ViewAdminPanelActivity.this, "Failed to fetch complaint count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}