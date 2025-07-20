package com.crriccn.defecto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout; // Added import for LinearLayout

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewAllComplaintActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ComplaintAdapter adapter;
    private List<ComplaintModel> complaintList = new ArrayList<>();
    private Spinner spinnerFilter, spinnerSort;
    private ImageView backIcon;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference complaintRef = db.collection("complaints");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewallcomplaints);

        recyclerView = findViewById(R.id.recyclerViewComplaints);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        spinnerSort = findViewById(R.id.spinnerSort);
        backIcon = findViewById(R.id.backIcon);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ComplaintAdapter(complaintList);
        recyclerView.setAdapter(adapter);

        loadData();
        backIcon.setOnClickListener(v -> finish());

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadData() {
        complaintRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            complaintList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                ComplaintModel complaint = new ComplaintModel();
                complaint.setTitle(doc.getString("displayId"));
                complaint.setDescription(doc.getString("description"));
                complaint.setStatus(doc.getString("status"));
                complaint.setComplaintType(doc.getString("complaintType"));
                complaint.setImageUrl(doc.getString("imageUrl"));
                complaint.setLocation(doc.getString("location"));
                complaint.setOtherComplaintType(doc.getString("otherComplaintType"));
                complaint.setUserId(doc.getString("userId"));

                Object ts = doc.get("timestamp");
                if (ts instanceof Long) {
                    complaint.setTimestamp(new Date((Long) ts));
                } else if (ts instanceof com.google.firebase.Timestamp) {
                    complaint.setTimestamp(((com.google.firebase.Timestamp) ts).toDate());
                }

                if (applyFilter(complaint)) {
                    complaintList.add(complaint);
                }
            }

            applySort();
            adapter.notifyDataSetChanged();
        });
    }

    private boolean applyFilter(ComplaintModel complaint) {
        String selected = spinnerFilter.getSelectedItem().toString();
        switch (selected) {
            case "Last 24 Hours":
                Date parsedTimestamp = complaint.getParsedTimestamp();
                return parsedTimestamp != null &&
                        parsedTimestamp.after(new Date(System.currentTimeMillis() - 86400000));
            case "Resolved Complaints":
                return "resolved".equalsIgnoreCase(complaint.getStatus());
            case "Pending Complaints":
                return "pending".equalsIgnoreCase(complaint.getStatus());
            default:
                return true;
        }
    }

    private void applySort() {
        String selectedSort = spinnerSort.getSelectedItem().toString();
        if (selectedSort.contains("Newest")) {
            Collections.sort(complaintList, (a, b) -> {
                Date d1 = a.getParsedTimestamp();
                Date d2 = b.getParsedTimestamp();
                return d2 != null && d1 != null ? d2.compareTo(d1) : 0;
            });
        } else {
            Collections.sort(complaintList, Comparator.comparing(ComplaintModel::getParsedTimestamp, Comparator.nullsLast(Date::compareTo)));
        }
    }

    class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ViewHolder> {
        private List<ComplaintModel> complaints;

        ComplaintAdapter(List<ComplaintModel> complaints) {
            this.complaints = complaints;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.complaint_table_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ComplaintModel complaint = complaints.get(position);

            // Show header for the first item (optional)
            if (position == 0) {
                holder.headerRow.setVisibility(View.VISIBLE);
            } else {
                holder.headerRow.setVisibility(View.GONE);
            }

            // Bind table fields
            holder.title.setText(complaint.getTitle() != null ? complaint.getTitle() : "N/A");
            holder.status.setText(complaint.getStatus() != null ? complaint.getStatus() : "N/A");
            holder.complaintType.setText(complaint.getComplaintType() != null ? complaint.getComplaintType() : "N/A");

            // Format timestamp
            if (complaint.getParsedTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                holder.timestamp.setText(sdf.format(complaint.getParsedTimestamp()));
            } else {
                holder.timestamp.setText("N/A");
            }

            // Bind expandable content
            holder.description.setText(complaint.getDescription() != null ? complaint.getDescription() : "N/A");
            holder.location.setText(complaint.getLocation() != null ? complaint.getLocation() : "N/A");
            holder.userId.setText(complaint.getUserId() != null ? complaint.getUserId() : "N/A");

            // Load image
            if (complaint.getImageUrl() != null && !complaint.getImageUrl().isEmpty()) {
                Glide.with(ViewAllComplaintActivity.this)
                        .load(complaint.getImageUrl())
                        .error(R.drawable.ic_placeholder)
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_placeholder);
            }

            // Toggle expandable content
            holder.itemView.setOnClickListener(v -> {
                boolean isVisible = holder.expandableContent.getVisibility() == View.VISIBLE;
                holder.expandableContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            });
        }

        @Override
        public int getItemCount() {
            return complaints.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView title, status, complaintType, timestamp, description, location, userId;
            LinearLayout headerRow, expandableContent;

            ViewHolder(View itemView) {
                super(itemView);
                headerRow = itemView.findViewById(R.id.headerRow);
                expandableContent = itemView.findViewById(R.id.expandableContent);
                imageView = itemView.findViewById(R.id.complaintImageView);
                title = itemView.findViewById(R.id.complaintTitle);
                status = itemView.findViewById(R.id.complaintStatus);
                complaintType = itemView.findViewById(R.id.complaintType);
                timestamp = itemView.findViewById(R.id.complaintTimestamp);
                description = itemView.findViewById(R.id.complaintDescription);
                location = itemView.findViewById(R.id.complaintLocation);
                userId = itemView.findViewById(R.id.complaintUserId);
            }
        }
    }
}