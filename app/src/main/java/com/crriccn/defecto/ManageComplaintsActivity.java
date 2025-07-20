package com.crriccn.defecto;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageComplaintsActivity extends AppCompatActivity {
    private static final String TAG = "ManageComplaints";
    private RecyclerView complaintsRecyclerView;
    private List<ComplaintModel> complaintList = new ArrayList<>();
    private FirebaseFirestore db;
    private ImageView backIcon;
    private String appPassword;
    private String adminEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_managecomplaints);

        complaintsRecyclerView = findViewById(R.id.complaintsRecyclerView);
        complaintsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        fetchComplaints();
        backIcon = findViewById(R.id.backIcon);
        backIcon.setOnClickListener(v -> finish());
        appPassword = MetaDataUtil.getMetaDataValue(this, "APP_PASSWORD");
        adminEmail = MetaDataUtil.getMetaDataValue(this, "ADMIN_EMAIL");
    }

    private void fetchComplaints() {
        db.collection("complaints")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        complaintList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ComplaintModel complaint = document.toObject(ComplaintModel.class);
                            complaint.setDocumentId(document.getId());
                            complaintList.add(complaint);
                        }
                        complaintsRecyclerView.setAdapter(new AdminComplaintAdapter(complaintList));
                    } else {
                        Log.e(TAG, "Error fetching complaints: ", task.getException());
                        Toast.makeText(this, "Failed to fetch complaints", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    class AdminComplaintAdapter extends RecyclerView.Adapter<AdminComplaintAdapter.ViewHolder> {
        private List<ComplaintModel> localList;
        private final String[] statuses = {"Pending", "Resolved"};

        AdminComplaintAdapter(List<ComplaintModel> complaints) {
            this.localList = complaints;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.complaint_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ComplaintModel model = localList.get(position);

            // Load image using Glide
            if (model.getImageUrl() != null && !model.getImageUrl().isEmpty()) {
                Glide.with(ManageComplaintsActivity.this)
                        .load(model.getImageUrl())
                        .error(R.drawable.ic_placeholder)
                        .into(holder.complaintImageView);
            } else {
                holder.complaintImageView.setImageResource(R.drawable.ic_placeholder);
            }

            // Reset button states to avoid recycling issues
            holder.updateButton.setEnabled(true);
            holder.deleteButton.setEnabled(true);

            holder.title.setText(model.getTitle());
            String status = model.getStatus() != null ? model.getStatus() : "Pending";
            String address = model.getAddress() != null ? model.getAddress() : "N/A";

            holder.details.setText("Type: " + model.getComplaintType() +
                    " | ID: " + model.getDisplayId() +
                    " | Status: " + status +
                    " | Location: " + model.getLocation() +
                    " | Address: " + address);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageComplaintsActivity.this,
                    android.R.layout.simple_spinner_item, statuses);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.statusSpinner.setAdapter(adapter);
            holder.statusSpinner.setSelection(status.equalsIgnoreCase("Resolved") ? 1 : 0);

            holder.updateButton.setOnClickListener(v -> {
                Log.d(TAG, "Update button clicked for complaint: " + model.getDocumentId());
                holder.updateButton.setEnabled(false);
                holder.deleteButton.setEnabled(false);

                String newStatus = holder.statusSpinner.getSelectedItem().toString();
                String comment = holder.commentEditText.getText().toString().trim();
                String userId = model.getUserId();

                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(ManageComplaintsActivity.this, "Invalid User ID", Toast.LENGTH_SHORT).show();
                    resetButtonStates(holder);
                    return;
                }

                String subject = "CENTRAL ROAD RESEARCH INSTITUTE NEW DELHI--Complaint Status Updated";
                String message = "Hello, user with userId: " + model.getDisplayId() + "\nI hope you are doing well\n" +
                        "Your complaint titled '" + model.getComplaintType() + "\nlocation is: " + model.getLocation() +
                        "\nAddress: " + model.getAddress() + "\nImageUrl is: " + model.getImageUrl() + "' \nhas been updated.\n" +
                        "\nNew Status: " + newStatus +
                        "\nComment: " + (comment.isEmpty() ? "No comment provided." : comment);

                db.collection("users").document(userId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String email = documentSnapshot.getString("email");
                                if (email != null && !email.isEmpty()) {
                                    sendEmail(email, subject, message, () -> {
                                        db.collection("complaints").document(model.getDocumentId())
                                                .update("status", newStatus, "adminComment", comment)
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(ManageComplaintsActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                                                    holder.commentEditText.setText("");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Update failed: ", e);
                                                    Toast.makeText(ManageComplaintsActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnCompleteListener(task -> resetButtonStates(holder));
                                    }, () -> resetButtonStates(holder));
                                } else {
                                    Toast.makeText(ManageComplaintsActivity.this, "User email not found", Toast.LENGTH_SHORT).show();
                                    resetButtonStates(holder);
                                }
                            } else {
                                Toast.makeText(ManageComplaintsActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                                resetButtonStates(holder);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch user email: ", e);
                            Toast.makeText(ManageComplaintsActivity.this, "Failed to fetch user email", Toast.LENGTH_SHORT).show();
                            resetButtonStates(holder);
                        });
            });

            holder.deleteButton.setOnClickListener(v -> {
                Log.d(TAG, "Delete button clicked for complaint: " + model.getDocumentId());
                holder.deleteButton.setEnabled(false);
                holder.updateButton.setEnabled(false);

                String comment = holder.commentEditText.getText().toString().trim();
                String userId = model.getUserId();

                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(ManageComplaintsActivity.this, "Invalid User ID", Toast.LENGTH_SHORT).show();
                    resetButtonStates(holder);
                    return;
                }

                String subject = "CENTRAL ROAD RESEARCH INSTITUTE NEW DELHI--Complaint Deleted";
                String message = "Hello, user with userId: " + model.getDisplayId() + "\nI hope you are doing well\n" +
                        "Your complaint titled '" + model.getComplaintType() + "\nlocation is: " + model.getLocation() +
                        "\nAddress: " + model.getAddress() + "\nImageUrl is: " + model.getImageUrl() + "'\n has been deleted.\n" +
                        "Comment: " + (comment.isEmpty() ? "No comment provided." : comment);

                db.collection("users").document(userId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String email = documentSnapshot.getString("email");
                                if (email != null && !email.isEmpty()) {
                                    sendEmail(email, subject, message, () -> {
                                        db.collection("complaints").document(model.getDocumentId())
                                                .delete()
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(ManageComplaintsActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                                    int pos = holder.getAdapterPosition();
                                                    if (pos != RecyclerView.NO_POSITION) {
                                                        complaintList.remove(pos);
                                                        notifyItemRemoved(pos);
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Delete failed: ", e);
                                                    Toast.makeText(ManageComplaintsActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnCompleteListener(task -> resetButtonStates(holder));
                                    }, () -> resetButtonStates(holder));
                                } else {
                                    Toast.makeText(ManageComplaintsActivity.this, "User email not found", Toast.LENGTH_SHORT).show();
                                    resetButtonStates(holder);
                                }
                            } else {
                                Toast.makeText(ManageComplaintsActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                                resetButtonStates(holder);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch user email: ", e);
                            Toast.makeText(ManageComplaintsActivity.this, "Failed to fetch user email", Toast.LENGTH_SHORT).show();
                            resetButtonStates(holder);
                        });
            });
        }

        private void resetButtonStates(ViewHolder holder) {
            holder.updateButton.setEnabled(true);
            holder.deleteButton.setEnabled(true);
        }

        @Override
        public int getItemCount() {
            return localList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, details;
            Spinner statusSpinner;
            EditText commentEditText;
            Button updateButton, deleteButton;
            ImageView complaintImageView;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.complaintTitle);
                details = itemView.findViewById(R.id.complaintDetails);
                statusSpinner = itemView.findViewById(R.id.statusSpinner);
                commentEditText = itemView.findViewById(R.id.commentEditText);
                updateButton = itemView.findViewById(R.id.updateButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                complaintImageView = itemView.findViewById(R.id.complaintImageView);
            }
        }

        private void sendEmail(String email, String subject, String message, Runnable onSuccess, Runnable onFailure) {
            new Thread(() -> {
                try {
                    GmailSender.sendEmail(
                            email,
                            subject,
                            message,
                            adminEmail,
                            appPassword
                    );
                    runOnUiThread(onSuccess);
                } catch (Exception e) {
                    Log.e(TAG, "Email sending failed: ", e);
                    runOnUiThread(() -> {
                        Toast.makeText(ManageComplaintsActivity.this, "Email failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        onFailure.run();
                    });
                }
            }).start();
        }
    }
}