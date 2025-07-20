package com.crriccn.defecto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
public class ViewComplaintStatusActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private LinearLayout complaintContainer;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewcomplaintstatus);


        firestore = FirebaseFirestore.getInstance();

        // Retrieve user ID from SharedPreferences
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userId", null);

        if (userId != null) {
            fetchComplaints(userId);
        } else {
            // Redirect to login if user ID is not found
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        complaintContainer = findViewById(R.id.complaint_container);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            //  navigate to login screen
            startActivity(new Intent(this, DashboardActivity.class));

        });

    }

    private void fetchComplaints(String userId) {
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userId", null);
        CollectionReference complaintsRef = firestore.collection("complaints");

        complaintsRef.whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            addComplaintCard(
                                    document.getString("complaintType"),
                                    document.getString("location"),
                                    document.getString("address"),
                                    document.getString("description"),
                                    document.getString("status"),
                                    document.getString("adminComment"),
                                    document.getString("imageUrl"),
                                    document.getString("statusIcon")
                            );
                        }
                    } else {
                        Toast.makeText(this, "Failed to load complaints", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addComplaintCard(String complaintType, String location, String address, String description,
                                  String status, String adminComment, String imageUrl, String statusIcon) {

        View cardView = getLayoutInflater().inflate(R.layout.complaint_card, null);

        // Set layout parameters with bottom margin
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 30); // Adds 30dp bottom margin between cards
        cardView.setLayoutParams(layoutParams);

        ImageView image = cardView.findViewById(R.id.complaint_image);
        TextView typeText = cardView.findViewById(R.id.complaint_type);
        TextView locationText = cardView.findViewById(R.id.complaint_location);
        TextView addressText = cardView.findViewById(R.id.complaint_address);
        TextView descriptionText = cardView.findViewById(R.id.complaint_description);
        TextView statusText = cardView.findViewById(R.id.complaint_status);
        ImageView statusIconView = cardView.findViewById(R.id.complaint_status_icon);
        TextView commentsText = cardView.findViewById(R.id.complaint_comments);

        // Set data
        typeText.setText("complaintType: " + complaintType);
        locationText.setText("Location: " + location);
        addressText.setText("Address: " + address);
        descriptionText.setText("Issue: " + description);
        if (status.equalsIgnoreCase("resolved")) {
            statusText.setTextColor(ContextCompat.getColor(this, R.color.resolved_green)); // e.g., #4CAF50
            statusText.setText("Status: " + status);
            statusIconView.setImageResource(R.drawable.ic_statusresolved); // Your resolved icon
        } else if (status.equalsIgnoreCase("pending")) {
            statusText.setTextColor(ContextCompat.getColor(this, R.color.pending_red)); // e.g., #d32f2f
            statusText.setText("Status: " + status);
            statusIconView.setImageResource(R.drawable.ic_statuspending); // Your pending icon
        } else {
            // Default/fallback
            statusText.setText("Status: " + status);
            statusIconView.setImageResource(R.drawable.ic_default_status);
        }

        commentsText.setText("Comments: " + adminComment);

        // Load image using Glide
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder) // Add a placeholder drawable
                .into(image);

        // Load status icon using Glide or use default if null
        if (statusIcon != null && !statusIcon.isEmpty()) {
            Glide.with(this)
                    .load(statusIcon)
                    .into(statusIconView);
        }

        // Add card to layout
        complaintContainer.addView(cardView);
    }
}
