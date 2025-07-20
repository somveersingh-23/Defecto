package com.crriccn.defecto;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etUsername, etPhone, tvAddress, etOtherUserType;
    private AutoCompleteTextView tvUserType;
    private Button btnSave, btnCamera, btnGallery;
    private ImageView imgProfile, backIcon;
    private ProgressBar progressBar;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private Bitmap capturedBitmap;
    private Uri profileImageUri;
    private String userId;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private String imgbbApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editprofileinfo);
        imgbbApi = MetaDataUtil.getMetaDataValue(this,"IMGBB_API_KEY");
        etUsername = findViewById(R.id.etUsername);
        etPhone = findViewById(R.id.etPhone);
        tvUserType = findViewById(R.id.tvUserType);
        etOtherUserType = findViewById(R.id.etOtherUserType); // new field in layout
        tvAddress = findViewById(R.id.tvAddress);
        btnSave = findViewById(R.id.btnSave);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        imgProfile = findViewById(R.id.imgProfile);
        progressBar = findViewById(R.id.progressBar);
        backIcon = findViewById(R.id.backIcon);
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Retrieve user ID from SharedPreferences
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userId", null);

        if (userId != null) {
            loadUserData(userId);
        } else {
            // Redirect to login if user ID is not found
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        String[] userTypes = {"Self-employed", "Government Employee", "Businessmen", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userTypes);
        tvUserType.setAdapter(adapter);
        tvUserType.setInputType(InputType.TYPE_CLASS_TEXT);
        tvUserType.setOnClickListener(v -> tvUserType.showDropDown());

        tvUserType.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            if (selected.equals("Others")) {
                etOtherUserType.setVisibility(View.VISIBLE);
            } else {
                etOtherUserType.setVisibility(View.GONE);
            }
        });


        btnSave.setOnClickListener(v -> saveProfileChanges(userId));
        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> openGallery());
        backIcon.setOnClickListener(v -> finish());
    }

    private void loadUserData(String userId) {
        String uid = userId;
        if (uid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (doc.exists()) {
                        etUsername.setText(doc.getString("username"));
                        etPhone.setText(doc.getString("phone"));
                        String userType = doc.getString("userType");
                        tvUserType.setText(userType);
                        tvAddress.setText(doc.getString("address"));

                        if (userType != null && !userType.equals("Self-employed") && !userType.equals("Government Employee") && !userType.equals("Businessmen")) {
                            etOtherUserType.setText(userType);
                            etOtherUserType.setVisibility(View.VISIBLE);
                        } else {
                            etOtherUserType.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfileChanges(String userId) {
        String uid = userId;
        if (uid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", etUsername.getText().toString());
        updates.put("phone", etPhone.getText().toString());

        // Handle userType logic
        String selectedType = tvUserType.getText().toString();
        if (selectedType.equals("Others")) {
            String otherType = etOtherUserType.getText().toString().trim();
            if (otherType.isEmpty()) {
                Toast.makeText(this, "Please specify your user type.", Toast.LENGTH_SHORT).show();
                return;
            }
            updates.put("userType", otherType);
        } else {
            updates.put("userType", selectedType);
        }

        updates.put("address", tvAddress.getText().toString());

        progressBar.setVisibility(View.VISIBLE);

        if (profileImageUri != null || capturedBitmap != null) {
            uploadProfileImage(uid, updates);
        } else {
            updateFirestoreProfile(uid, updates);
        }
    }

    private void uploadProfileImage(String uid, Map<String, Object> updates) {
        try {
            Bitmap bitmap = capturedBitmap != null ? capturedBitmap
                    : MediaStore.Images.Media.getBitmap(this.getContentResolver(), profileImageUri);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            new Thread(() -> {
                try {
                    String apiKey = imgbbApi;
                    URL url = new URL("https://api.imgbb.com/1/upload?key=" + apiKey);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    String data = "image=" + URLEncoder.encode(encodedImage, "UTF-8");
                    OutputStream os = connection.getOutputStream();
                    os.write(data.getBytes());
                    os.flush();
                    os.close();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = connection.getInputStream();
                        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                        String result = s.hasNext() ? s.next() : "";

                        JSONObject json = new JSONObject(result);
                        String imageUrl = json.getJSONObject("data").getString("url");

                        runOnUiThread(() -> {
                            updates.put("profileImageUrl", imageUrl);
                            updateFirestoreProfile(uid, updates);
                        });
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFirestoreProfile(String uid, Map<String, Object> updates) {
        firestore.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, ProfileInfoActivity.class));
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                capturedBitmap = (Bitmap) data.getExtras().get("data");
                imgProfile.setImageBitmap(capturedBitmap);
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                profileImageUri = data.getData();
                imgProfile.setImageURI(profileImageUri);
            }
        }
    }
}
