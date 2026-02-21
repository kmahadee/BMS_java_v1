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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;


public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseManager = FirebaseManager.getInstance();
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> validateAndLogin());
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
    }

    private void validateAndLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        performLogin(email, password);
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void performLogin(String email, String password) {
        showLoading(true);

        firebaseManager.signIn(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                checkUserRoleAndNavigate(userId);
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkUserRoleAndNavigate(String userId) {
        firebaseManager.getCurrentUserRole(userId, new FirebaseManager.RoleCallback() {
            @Override
            public void onSuccess(String role) {
                if (role.equals("ADMIN")) {
                    navigateToAdminDashboard();
                } else if (role.equals("CUSTOMER")) {
                    checkCustomerApproval(userId);
                }
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }


    // Replace the checkCustomerApproval method in LoginActivity.java

    private void checkCustomerApproval(String userId) {
        System.out.println("DEBUG: Checking approval for user: " + userId);

        firebaseManager.isUserApproved(userId, isApproved -> {
            System.out.println("DEBUG: Approval result: " + isApproved);
            showLoading(false);

            if (isApproved) {
                System.out.println("DEBUG: User approved, navigating to dashboard");
                navigateToCustomerDashboard();
            } else {
                System.out.println("DEBUG: User NOT approved, showing message");
                Toast.makeText(LoginActivity.this,
                        "Your account is pending approval. Please contact an administrator.",
                        Toast.LENGTH_LONG).show();

                firebaseManager.signOut();
            }
        });
    }


//    private void checkCustomerApproval(String userId) {
//        firebaseManager.isUserApproved(userId, isApproved -> {
//            showLoading(false);
//            if (isApproved) {
//                navigateToCustomerDashboard();
//            } else {
//                Toast.makeText(LoginActivity.this,
//                        "Your account is pending approval. Please contact an administrator.",
//                        Toast.LENGTH_LONG).show();
//                firebaseManager.signOut();
//            }
//        });
//    }

    private void navigateToAdminDashboard() {
        showLoading(false);
        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCustomerDashboard() {
        showLoading(false);
        Intent intent = new Intent(LoginActivity.this,
                CustomerDashboardActivity.class
        );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
    }
}