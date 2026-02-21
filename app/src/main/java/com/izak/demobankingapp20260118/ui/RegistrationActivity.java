package com.izak.demobankingapp20260118.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.izak.demobankingapp20260118.R;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;

public class RegistrationActivity extends AppCompatActivity {
    private TextInputEditText emailInput, passwordInput, confirmPasswordInput;
    private RadioGroup roleRadioGroup;
    private RadioButton adminRadio, customerRadio;
    private Button registerButton;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        firebaseManager = FirebaseManager.getInstance();
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        adminRadio = findViewById(R.id.adminRadio);
        customerRadio = findViewById(R.id.customerRadio);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        // Set default selection to CUSTOMER
        customerRadio.setChecked(true);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> validateAndRegister());
    }

    private void validateAndRegister() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!validateInput(email, password, confirmPassword)) {
            return;
        }

        String role = getSelectedRole();
        performRegistration(email, password, role);
    }

    private boolean validateInput(String email, String password, String confirmPassword) {
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

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError("Please confirm your password");
            confirmPasswordInput.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return false;
        }

        if (roleRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getSelectedRole() {
        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.adminRadio) {
            return "ADMIN";
        } else {
            return "CUSTOMER";
        }
    }

    private void performRegistration(String email, String password, String role) {
        showLoading(true);

        firebaseManager.signUp(email, password, role, new FirebaseManager.SignUpCallback() {
            @Override
            public void onSuccess(String userId) {
                // CRITICAL: Run on UI thread
                runOnUiThread(() -> {
                    System.out.println("DEBUG Registration: User created - ID: " + userId + ", Role: " + role);

                    String message = role.equals("CUSTOMER")
                            ? "✅ Registration successful!\n\nYour account is pending admin approval.\nYou'll be notified once approved."
                            : "✅ Registration successful!\n\nYou can now login as an administrator.";

                    Toast.makeText(RegistrationActivity.this, message, Toast.LENGTH_LONG).show();

                    // CRITICAL: Sign out the user after registration
                    firebaseManager.signOut();
                    System.out.println("DEBUG Registration: User signed out after registration");

                    // Navigate back to login after a short delay
                    registerButton.postDelayed(() -> {
                        Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }, 1500); // 1.5 second delay to show the toast

                    showLoading(false);
                });
            }

            @Override
            public void onFailure(String error) {
                // CRITICAL: Run on UI thread
                runOnUiThread(() -> {
                    showLoading(false);
                    System.out.println("DEBUG Registration: Failed - " + error);
                    Toast.makeText(RegistrationActivity.this,
                            "Registration failed: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
        confirmPasswordInput.setEnabled(!show);
        roleRadioGroup.setEnabled(!show);
        for (int i = 0; i < roleRadioGroup.getChildCount(); i++) {
            roleRadioGroup.getChildAt(i).setEnabled(!show);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}


//package com.izak.demobankingapp20260118.ui;
//
//import android.os.Bundle;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.izak.demobankingapp20260118.R;
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ProgressBar;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import com.google.android.material.textfield.TextInputEditText;
//import com.izak.demobankingapp20260118.firebase.FirebaseManager;
//
//
//public class RegistrationActivity extends AppCompatActivity {
//    private TextInputEditText emailInput, passwordInput, confirmPasswordInput;
//    private RadioGroup roleRadioGroup;
//    private RadioButton adminRadio, customerRadio;
//    private Button registerButton;
//    private ProgressBar progressBar;
//    private FirebaseManager firebaseManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_registration);
//
//        firebaseManager = FirebaseManager.getInstance();
//        initializeViews();
//        setupListeners();
//    }
//
//    private void initializeViews() {
//        emailInput = findViewById(R.id.emailInput);
//        passwordInput = findViewById(R.id.passwordInput);
//        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
//        roleRadioGroup = findViewById(R.id.roleRadioGroup);
//        adminRadio = findViewById(R.id.adminRadio);
//        customerRadio = findViewById(R.id.customerRadio);
//        registerButton = findViewById(R.id.registerButton);
//        progressBar = findViewById(R.id.progressBar);
//
//        // Set default selection to CUSTOMER
//        customerRadio.setChecked(true);
//    }
//
//    private void setupListeners() {
//        registerButton.setOnClickListener(v -> validateAndRegister());
//    }
//
//    private void validateAndRegister() {
//        String email = emailInput.getText().toString().trim();
//        String password = passwordInput.getText().toString().trim();
//        String confirmPassword = confirmPasswordInput.getText().toString().trim();
//
//        if (!validateInput(email, password, confirmPassword)) {
//            return;
//        }
//
//        String role = getSelectedRole();
//        performRegistration(email, password, role);
//    }
//
//    private boolean validateInput(String email, String password, String confirmPassword) {
//        if (email.isEmpty()) {
//            emailInput.setError("Email is required");
//            emailInput.requestFocus();
//            return false;
//        }
//
//        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            emailInput.setError("Please enter a valid email");
//            emailInput.requestFocus();
//            return false;
//        }
//
//        if (password.isEmpty()) {
//            passwordInput.setError("Password is required");
//            passwordInput.requestFocus();
//            return false;
//        }
//
//        if (password.length() < 6) {
//            passwordInput.setError("Password must be at least 6 characters");
//            passwordInput.requestFocus();
//            return false;
//        }
//
//        if (confirmPassword.isEmpty()) {
//            confirmPasswordInput.setError("Please confirm your password");
//            confirmPasswordInput.requestFocus();
//            return false;
//        }
//
//        if (!password.equals(confirmPassword)) {
//            confirmPasswordInput.setError("Passwords do not match");
//            confirmPasswordInput.requestFocus();
//            return false;
//        }
//
//        if (roleRadioGroup.getCheckedRadioButtonId() == -1) {
//            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        return true;
//    }
//
//    private String getSelectedRole() {
//        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
//        if (selectedId == R.id.adminRadio) {
//            return "ADMIN";
//        } else {
//            return "CUSTOMER";
//        }
//    }
//
//    private void performRegistration(String email, String password, String role) {
//        showLoading(true);
//
//        firebaseManager.signUp(email, password, role, new FirebaseManager.SignUpCallback() {
//            @Override
//            public void onSuccess(String userId) {
//                showLoading(false);
//                String message = role.equals("CUSTOMER")
//                        ? "Registration successful! Your account is pending approval."
//                        : "Registration successful! You can now login.";
//
//                Toast.makeText(RegistrationActivity.this, message, Toast.LENGTH_LONG).show();
//
//                // Navigate back to login
//                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                finish();
//            }
//
//            @Override
//            public void onFailure(String error) {
//                showLoading(false);
//                Toast.makeText(RegistrationActivity.this,
//                        "Registration failed: " + error,
//                        Toast.LENGTH_LONG).show();
//            }
//        });
//    }
//
//    private void showLoading(boolean show) {
//        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
//        registerButton.setEnabled(!show);
//        emailInput.setEnabled(!show);
//        passwordInput.setEnabled(!show);
//        confirmPasswordInput.setEnabled(!show);
//        roleRadioGroup.setEnabled(!show);
//        for (int i = 0; i < roleRadioGroup.getChildCount(); i++) {
//            roleRadioGroup.getChildAt(i).setEnabled(!show);
//        }
//    }
//
//    @Override
//    public boolean onSupportNavigateUp() {
//        onBackPressed();
//        return true;
//    }
//}