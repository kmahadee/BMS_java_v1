package com.izak.demobankingapp20260118;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.appcheck.interop.BuildConfig;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.firebase.FirebaseDatabaseInitializer;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.ui.AdminDashboardActivity;
import com.izak.demobankingapp20260118.ui.LoginActivity;
import com.izak.demobankingapp20260118.ui.RegistrationActivity;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabaseInitializer firebaseDatabaseInitializer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button login = findViewById(R.id.loginm);
        login.setOnClickListener(view -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
        Button reg = findViewById(R.id.reg);
        reg.setOnClickListener(view -> {
            startActivity(new Intent(this, RegistrationActivity.class));
        });
        Button admn = findViewById(R.id.admindash);
        admn.setOnClickListener(view -> {
            startActivity(new Intent(this, AdminDashboardActivity.class));
        });


        // Initialize Firebase database structure
        FirebaseDatabaseInitializer.checkDatabaseStructure(isInitialized -> {
            if (!isInitialized) {
                // Initialize if not already done
                FirebaseDatabaseInitializer.initializeDatabase();
            }
        });




        if (BuildConfig.DEBUG) {
            FirebaseDatabaseInitializer.initializeSampleData();
        }
    }






//=============================================


    private void verifyDatabaseStructure() {
        System.out.println("========================================");
        System.out.println("DATABASE STRUCTURE VERIFICATION");
        System.out.println("========================================");

        FirebaseManager firebaseManager = FirebaseManager.getInstance();

        // Check users
        firebaseManager.getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                System.out.println("\n--- USERS ---");
                System.out.println("Total users: " + snapshot.getChildrenCount());

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String email = userSnapshot.child("email").getValue(String.class);
                    String role = userSnapshot.child("role").getValue(String.class);
                    Boolean isApproved = userSnapshot.child("isApproved").getValue(Boolean.class);

                    System.out.println("  User ID: " + userId);
                    System.out.println("    Email: " + email);
                    System.out.println("    Role: " + role);
                    System.out.println("    Approved: " + isApproved);
                }

                // Check accounts
                firebaseManager.getAccountsRef().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        System.out.println("\n--- ACCOUNTS ---");
                        System.out.println("Total accounts: " + snapshot.getChildrenCount());

                        for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
                            String accountId = accountSnapshot.getKey();
                            String userId = accountSnapshot.child("userId").getValue(String.class);
                            String accountNumber = accountSnapshot.child("accountNumber").getValue(String.class);
                            Double balance = accountSnapshot.child("balance").getValue(Double.class);

                            System.out.println("  Account ID: " + accountId);
                            System.out.println("    User ID: " + userId);
                            System.out.println("    Account Number: " + accountNumber);
                            System.out.println("    Balance: " + balance);
                        }

                        System.out.println("========================================");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        System.out.println("Error checking accounts: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Error checking users: " + error.getMessage());
            }
        });
    }













}