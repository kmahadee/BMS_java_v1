package com.izak.demobankingapp20260118.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.izak.demobankingapp20260118.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.ui.adminDashboardFragments.PendingApprovalsFragment;
import com.izak.demobankingapp20260118.ui.adminDashboardFragments.PendingLoansFragment;
import com.izak.demobankingapp20260118.ui.adminDashboardFragments.AllCustomersFragment;

public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FirebaseManager firebaseManager;
    private TextView navHeaderEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        firebaseManager = FirebaseManager.getInstance();

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupNavigationHeader();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new PendingApprovalsFragment(), "Pending Approvals");
            navigationView.setCheckedItem(R.id.nav_pending_approvals);
        }
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email);
        TextView navHeaderTitle = headerView.findViewById(R.id.nav_header_title);

        FirebaseUser currentUser = firebaseManager.getCurrentUser();
        if (currentUser != null) {
            navHeaderEmail.setText(currentUser.getEmail());
            navHeaderTitle.setText("Admin Dashboard");
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_pending_approvals) {
            loadFragment(new PendingApprovalsFragment(), "Pending Approvals");
        } else if (id == R.id.nav_pending_loans) {
            loadFragment(new PendingLoansFragment(), "Pending Loans");
        } else if (id == R.id.nav_all_customers) {
            loadFragment(new AllCustomersFragment(), "All Customers");
        } else if (id == R.id.nav_logout) {
            performLogout();
            return true;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, String title) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void performLogout() {
        firebaseManager.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawerLayout != null) {
            drawerLayout.removeAllViews();
        }
    }
}
