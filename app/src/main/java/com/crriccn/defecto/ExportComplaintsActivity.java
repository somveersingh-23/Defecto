package com.crriccn.defecto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.os.Build;
import android.provider.Settings;
import android.content.Intent;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportComplaintsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Spinner spinnerFilter, spinnerSort;
    private Button btnExport;
    private ImageView backIcon;
    private ComplaintAdapter adapter;
    private List<ComplaintModel> allComplaints = new ArrayList<>();
    private List<ComplaintModel> displayedComplaints = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference complaintRef = db.collection("complaints");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_complaints);

        recyclerView = findViewById(R.id.recyclerViewComplaints);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        spinnerSort = findViewById(R.id.spinnerSort);
        btnExport = findViewById(R.id.btnExport);
        backIcon = findViewById(R.id.backIcon);
        adapter = new ComplaintAdapter(displayedComplaints);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        backIcon.setOnClickListener(v -> finish());
        loadData();

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilterAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerFilter.setOnItemSelectedListener(listener);
        spinnerSort.setOnItemSelectedListener(listener);

        btnExport.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    exportToCSV();
                } else {
                    // Request All Files Access permission
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            } else {
                if (checkPermission()) {
                    exportToCSV();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }

        });
    }

    private void loadData() {
        complaintRef.get().addOnSuccessListener(snapshot -> {
            allComplaints.clear();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                ComplaintModel complaint = new ComplaintModel();
                complaint.setTitle(doc.getString("title"));
                complaint.setDescription(doc.getString("description"));
                complaint.setStatus(doc.getString("status"));
                complaint.setComplaintType(doc.getString("complaintType"));
                complaint.setImageUrl(doc.getString("imageUrl"));
                complaint.setLocation(doc.getString("location"));
                complaint.setOtherComplaintType(doc.getString("otherComplaintType"));
                complaint.setUserId(doc.getString("userId"));
                complaint.setUserId(doc.getString("displayId"));

                Object ts = doc.get("timestamp");
                if (ts instanceof Long) {
                    complaint.setTimestamp(new Date((Long) ts));
                } else if (ts instanceof com.google.firebase.Timestamp) {
                    complaint.setTimestamp(((com.google.firebase.Timestamp) ts).toDate());
                }


                allComplaints.add(complaint);
            }
            applyFilterAndSort();
        });
    }

    private void applyFilterAndSort() {
        String filter = spinnerFilter.getSelectedItem().toString();
        String sort = spinnerSort.getSelectedItem().toString();

        displayedComplaints.clear();
        long last24h = System.currentTimeMillis() - 86400000;

        for (ComplaintModel complaint : allComplaints) {
            Date timestamp = complaint.getParsedTimestamp();
            switch (filter) {
                case "Last 24 Hours":
                    if (timestamp != null && timestamp.after(new Date(last24h))) {
                        displayedComplaints.add(complaint);
                    }
                    break;
                case "Posted Complaints":
                    if ("posted".equalsIgnoreCase(complaint.getStatus())) {
                        displayedComplaints.add(complaint);
                    }
                    break;
                case "Resolved Complaints":
                    if ("resolved".equalsIgnoreCase(complaint.getStatus())) {
                        displayedComplaints.add(complaint);
                    }
                    break;
                case "Pending Complaints":
                    if ("pending".equalsIgnoreCase(complaint.getStatus())) {
                        displayedComplaints.add(complaint);
                    }
                    break;
                default:
                    displayedComplaints.add(complaint);
            }
        }

        if (sort.contains("Newest")) {
            Collections.sort(displayedComplaints, (a, b) -> {
                Date d1 = a.getParsedTimestamp();
                Date d2 = b.getParsedTimestamp();
                return d2 != null && d1 != null ? d2.compareTo(d1) : 0;
            });
        } else {
            Collections.sort(displayedComplaints, Comparator.comparing(ComplaintModel::getParsedTimestamp, Comparator.nullsLast(Date::compareTo)));
        }

        adapter.notifyDataSetChanged();
    }

    private void exportToCSV() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "DefectoExports");
            if (!dir.exists()) dir.mkdirs();

            // Get selected filter and sort values
            String filter = spinnerFilter.getSelectedItem().toString();
            String sort = spinnerSort.getSelectedItem().toString();

            // Sanitize and create filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String safeFilter = sanitizeForFilename(filter);
            String safeSort = sanitizeForFilename(sort);

            String filename = "complaints_" + safeFilter + "_" + safeSort + "_" + timestamp + ".csv";

            File file = new File(dir, filename);
            FileWriter writer = new FileWriter(file);

            writer.append("Title,Description,Status,Timestamp,ComplaintType,ImageUrl,Location,OtherComplaintType,UserId\n");

            for (ComplaintModel complaint : displayedComplaints) {
                writer.append(escapeCSV(complaint.getTitle())).append(",")
                        .append(escapeCSV(complaint.getDescription())).append(",")
                        .append(escapeCSV(complaint.getStatus())).append(",")
                        .append(String.valueOf(complaint.getParsedTimestamp())).append(",")
                        .append(escapeCSV(complaint.getComplaintType())).append(",")
                        .append(escapeCSV(complaint.getImageUrl())).append(",")
                        .append(escapeCSV(complaint.getLocation())).append(",")
                        .append(escapeCSV(complaint.getOtherComplaintType())).append(",")
                        .append(escapeCSV(complaint.getDisplayId())).append("\n");

            }

            writer.flush();
            writer.close();
            Toast.makeText(this, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String sanitizeForFilename(String input) {
        if (input == null) return "default";
        return input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    private String escapeCSV(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportToCSV();
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
