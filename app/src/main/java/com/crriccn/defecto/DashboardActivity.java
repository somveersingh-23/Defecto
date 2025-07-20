package com.crriccn.defecto;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DashboardActivity extends AppCompatActivity {

    CardView cardProfile, cardPostComplain, cardViewMap, cardStaffList, cardViewWebsite, cardComplaintStatus;
    FloatingActionButton btnPostComplaint;
    ImageView chatbtn;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize card views
        cardProfile = findViewById(R.id.card_profile);
        cardPostComplain = findViewById(R.id.card_post_complaint);
        cardViewMap = findViewById(R.id.card_view_map);
        cardStaffList = findViewById(R.id.card_staff_list);
        cardViewWebsite = findViewById(R.id.card_view_website);
        cardComplaintStatus = findViewById(R.id.card_viewstatus);
        btnPostComplaint = findViewById(R.id.fab_post_complain);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        chatbtn = findViewById(R.id.chatIcon);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_profile) {
                    startActivity(new Intent( DashboardActivity.this,ProfileInfoActivity.class));
                }else if(id==R.id.nav_logout){
                    // Clear activity stack and start LoginActivity
                    Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else if (id == R.id.nav_home) {
                    return true; // Already on home
                }
                return false;
            }
        });

        cardProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileInfoActivity.class)));

        cardPostComplain.setOnClickListener(v -> startActivity(new Intent(this, PostComplaintActivity.class)));

        cardViewMap.setOnClickListener(v -> startActivity(new Intent(this, ViewMapActivity.class)));

        cardStaffList.setOnClickListener(v -> startActivity(new Intent(this, StaffListActivity.class)));

        cardViewWebsite.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewWebActivity.class));
        });
        btnPostComplaint.setOnClickListener(v -> {
            //  navigate to login screen
            startActivity(new Intent(this, PostComplaintActivity.class));

        });
        cardComplaintStatus.setOnClickListener(v -> {
            //  navigate to login screen
            startActivity(new Intent(this, ViewComplaintStatusActivity.class));

        });
        chatbtn.setOnClickListener(v -> {
            //  navigate to login screen
            startActivity(new Intent(this, ChatActivity.class));

        });

    }
}
