package com.crriccn.defecto;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;
import android.content.Intent;
public class PostComplaintActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_LOCATION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;

    private String IMGBB_API_KEY;

    private ImageView imagePreview;
    private TextView locationText, userIdText;
    private EditText descriptionEditText, otherComplaintInput, issueAddressInput;
    private Spinner complaintTypeSpinner;
    private Bitmap capturedImageBitmap;
    private ProgressBar locationProgressBar;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FusedLocationProviderClient locationProvider;
    private ProgressDialog progressDialog;
    private String displayUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postcomplaint);
        IMGBB_API_KEY = MetaDataUtil.getMetaDataValue(this,"IMGBB_API_KEY");
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        complaintTypeSpinner = findViewById(R.id.spinner_complaint_type);
        imagePreview = findViewById(R.id.image_preview);
        locationText = findViewById(R.id.textview_location);
        descriptionEditText = findViewById(R.id.edittext_description);
        otherComplaintInput = findViewById(R.id.other_complaint);
        issueAddressInput = findViewById(R.id.et_address);
        locationProgressBar = findViewById(R.id.progress_bar);
        userIdText = findViewById(R.id.user_id);

        Button btnTakePicture = findViewById(R.id.btn_take_picture);
        Button btnSubmit = findViewById(R.id.btn_submit);
        Button btnFetchLocation = findViewById(R.id.btn_fetch_location);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_profile) {
                    startActivity(new Intent( PostComplaintActivity.this,ProfileInfoActivity.class));
                }else if(id==R.id.nav_logout){
                    // Clear activity stack and start LoginActivity
                    Intent intent = new Intent(PostComplaintActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else if (id == R.id.nav_home) {
                    startActivity(new Intent( PostComplaintActivity.this,DashboardActivity.class));
                }
                return false;
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting complaint...");
        progressDialog.setCancelable(false);

        String userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userId", null);

        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            displayUserId = documentSnapshot.getString("displayUserId");
                            if (displayUserId != null) {
                                userIdText.setText("User ID: " + displayUserId);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Permission denied "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.complaint_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        complaintTypeSpinner.setAdapter(adapter);

        complaintTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean showOther = parent.getItemAtPosition(position).toString().equalsIgnoreCase("Others");
                otherComplaintInput.setVisibility(showOther ? View.VISIBLE : View.GONE);
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnTakePicture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            } else {
                openCamera();
            }
        });

        btnFetchLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            } else {
                fetchLocationWithProgress();
            }
        });

        btnSubmit.setOnClickListener(v -> submitComplaint(userId));
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void fetchLocationWithProgress() {
        ProgressDialog locationDialog = new ProgressDialog(this);
        locationDialog.setMessage("Fetching location... Please wait");
        locationDialog.setCancelable(false);
        locationDialog.show();

        new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                    locationDialog.dismiss();
                    return;
                }

                locationProvider.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        String loc = location.getLatitude() + ", " + location.getLongitude();
                        locationText.setText(loc);
                    } else {
                        locationText.setText("Unable to determine location.");
                    }
                    locationDialog.dismiss();
                }).addOnFailureListener(e -> {
                    locationText.setText("Error fetching location");
                    locationDialog.dismiss();
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            });
        }).start();
    }

    private void submitComplaint(String userId) {
        String type = complaintTypeSpinner.getSelectedItem().toString();
        String customType = otherComplaintInput.getText().toString().trim();
        String complaintType = type.equalsIgnoreCase("Others") ? customType : type;

        String description = descriptionEditText.getText().toString().trim();
        String location = locationText.getText().toString().trim();
        String address = issueAddressInput.getText().toString().trim();

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (capturedImageBitmap != null) {
            uploadImageToImgbb(capturedImageBitmap, imageUrl -> {
                uploadComplaintToFirestore(userId, complaintType, description, location, address, imageUrl);
            });
        } else {
            uploadComplaintToFirestore(userId, complaintType, description, location, address, null);
        }
    }

    private void uploadImageToImgbb(Bitmap bitmap, ImgbbCallback callback) {
        new Thread(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                URL url = new URL("https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String data = "image=" + URLEncoder.encode(imageBase64, "UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                os.close();

                InputStream inputStream = conn.getInputStream();
                String response = new java.util.Scanner(inputStream).useDelimiter("\\A").next();

                JSONObject json = new JSONObject(response);
                String imageUrl = json.getJSONObject("data").getString("url");

                runOnUiThread(() -> callback.onSuccess(imageUrl));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void uploadComplaintToFirestore(String userId, String type, String desc, String loc, String address, String imageUrl) {
        Map<String, Object> complaint = new HashMap<>();
        complaint.put("userId", userId);
        complaint.put("displayId", displayUserId);
        complaint.put("complaintType", type);
        complaint.put("description", desc);
        complaint.put("location", loc);
        complaint.put("address", address);
        complaint.put("status", "pending");
        complaint.put("timestamp", System.currentTimeMillis());
        if (imageUrl != null) {
            complaint.put("imageUrl", imageUrl);
        }

        firestore.collection("complaints")
                .add(complaint)
                .addOnSuccessListener(doc -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Complaint submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private interface ImgbbCallback {
        void onSuccess(String imageUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            capturedImageBitmap = (Bitmap) data.getExtras().get("data");
            imagePreview.setImageBitmap(capturedImageBitmap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (requestCode == REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationWithProgress();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
