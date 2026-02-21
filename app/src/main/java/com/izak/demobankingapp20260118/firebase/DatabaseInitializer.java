package com.izak.demobankingapp20260118.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.izak.demobankingapp20260118.models.*;

import java.util.HashMap;
import java.util.Map;

public class DatabaseInitializer {

    public static void initializeSampleData() {
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        DatabaseReference database = firebaseManager.getDatabase();

        // Create initial admin user if not exists
        database.child("users").orderByChild("email").equalTo("admin@bank.com")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            // Create admin user
                            Map<String, Object> adminUser = new HashMap<>();
                            adminUser.put("email", "admin@bank.com");
                            adminUser.put("role", "ADMIN");
                            adminUser.put("isApproved", true);
                            adminUser.put("createdAt", System.currentTimeMillis());

                            // Create a unique ID (you might want to use actual auth UID)
                            String adminId = "admin_user_id"; // Replace with actual auth UID
                            database.child("users").child(adminId).setValue(adminUser);
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        // Handle error
                    }
                });

        // Initialize database structure
        initializeDatabaseStructure();
    }

    private static void initializeDatabaseStructure() {
        // This ensures all required paths exist
        Map<String, Object> initialStructure = new HashMap<>();

        // Initialize empty nodes if they don't exist
        initialStructure.put("users", new HashMap<>());
        initialStructure.put("accounts", new HashMap<>());
        initialStructure.put("loans", new HashMap<>());
        initialStructure.put("loanRepayments", new HashMap<>());
        initialStructure.put("transactions", new HashMap<>());

        FirebaseManager.getInstance().getDatabase().updateChildren(initialStructure);
    }

    // Method to reset database (for testing purposes only)
    public static void resetDatabase() {
        Map<String, Object> resetData = new HashMap<>();
        resetData.put("users", new HashMap<>());
        resetData.put("accounts", new HashMap<>());
        resetData.put("loans", new HashMap<>());
        resetData.put("loanRepayments", new HashMap<>());
        resetData.put("transactions", new HashMap<>());

        FirebaseManager.getInstance().getDatabase().setValue(resetData);
    }

    // Method to create sample data for testing
    public static void createSampleData() {
        // Create sample customers
        for (int i = 1; i <= 5; i++) {
            createSampleCustomer(i);
        }
    }

    private static void createSampleCustomer(final int customerNumber) {
        FirebaseCRUDManager crudManager = FirebaseCRUDManager.getInstance();

        User user = new User(
                "customer_" + customerNumber,
                "customer" + customerNumber + "@email.com",
                "CUSTOMER",
                customerNumber % 2 == 0, // Alternate approval status
                System.currentTimeMillis()
        );

        crudManager.createUser(user, success -> {
            if (success && user.isApproved()) {
                createAccountForCustomer(user, customerNumber);
            }
        });
    }

    private static void createAccountForCustomer(User user, final int customerNumber) {
        FirebaseCRUDManager crudManager = FirebaseCRUDManager.getInstance();

        Account account = new Account(
                "account_" + user.getUserId(),
                user.getUserId(),
                1000.00 * customerNumber,
                generateAccountNumber(user.getUserId()),
                System.currentTimeMillis()
        );

        crudManager.createAccount(account, accountSuccess -> {
            if (accountSuccess && customerNumber % 3 == 0) {
                createLoanForCustomer(user, account, customerNumber);
            }
        });
    }

    private static void createLoanForCustomer(User user, Account account, int customerNumber) {
        FirebaseCRUDManager crudManager = FirebaseCRUDManager.getInstance();

        Loan loan = new Loan(
                "loan_" + user.getUserId(),
                user.getUserId(),
                account.getAccountId(),
                5000.00,
                0.0,
                0.0,
                customerNumber % 2 == 0 ? "PENDING" : "APPROVED",
                System.currentTimeMillis(),
                customerNumber % 2 == 0 ? 0L : System.currentTimeMillis(),
                5.0,
                "Home Renovation"
        );

        crudManager.createLoan(loan, loanSuccess -> {});
    }

    private static String generateAccountNumber(String userId) {
        long hash = Math.abs(userId.hashCode());
        long timestamp = System.currentTimeMillis();
        long combined = (hash + timestamp) % 10000000000L;
        return String.format("%010d", combined);
    }
}






//package com.izak.demobankingapp20260118.firebase;
//
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.izak.demobankingapp20260118.models.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class DatabaseInitializer {
//
//    public static void initializeSampleData() {
//        FirebaseManager firebaseManager = FirebaseManager.getInstance();
//        DatabaseReference database = firebaseManager.getDatabase();
//
//        // Create initial admin user if not exists
//        database.child("users").orderByChild("email").equalTo("admin@bank.com")
//                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
//                    @Override
//                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
//                        if (!snapshot.exists()) {
//                            // Create admin user
//                            Map<String, Object> adminUser = new HashMap<>();
//                            adminUser.put("email", "admin@bank.com");
//                            adminUser.put("role", "ADMIN");
//                            adminUser.put("isApproved", true);
//                            adminUser.put("createdAt", System.currentTimeMillis());
//
//                            // Create a unique ID (you might want to use actual auth UID)
//                            String adminId = "admin_user_id"; // Replace with actual auth UID
//                            database.child("users").child(adminId).setValue(adminUser);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
//                        // Handle error
//                    }
//                });
//
//        // Initialize database structure
//        initializeDatabaseStructure();
//    }
//
//    private static void initializeDatabaseStructure() {
//        // This ensures all required paths exist
//        Map<String, Object> initialStructure = new HashMap<>();
//
//        // Initialize empty nodes if they don't exist
//        initialStructure.put("users", new HashMap<>());
//        initialStructure.put("accounts", new HashMap<>());
//        initialStructure.put("loans", new HashMap<>());
//        initialStructure.put("loanRepayments", new HashMap<>());
//        initialStructure.put("transactions", new HashMap<>());
//
//        FirebaseManager.getInstance().getDatabase().updateChildren(initialStructure);
//    }
//
//    // Method to reset database (for testing purposes only)
//    public static void resetDatabase() {
//        Map<String, Object> resetData = new HashMap<>();
//        resetData.put("users", new HashMap<>());
//        resetData.put("accounts", new HashMap<>());
//        resetData.put("loans", new HashMap<>());
//        resetData.put("loanRepayments", new HashMap<>());
//        resetData.put("transactions", new HashMap<>());
//
//        FirebaseManager.getInstance().getDatabase().setValue(resetData);
//    }
//
//    // Method to create sample data for testing
//    public static void createSampleData() {
//        FirebaseCRUDManager crudManager = FirebaseCRUDManager.getInstance();
//
//        // Create sample customers
//        for (int i = 1; i <= 5; i++) {
//            User user = new User(
//                    "customer_" + i,
//                    "customer" + i + "@email.com",
//                    "CUSTOMER",
//                    i % 2 == 0, // Alternate approval status
//                    System.currentTimeMillis()
//            );
//
//            crudManager.createUser(user, success -> {
//                if (success) {
//                    // Create account for approved customers
//                    if (user.isApproved()) {
//                        Account account = new Account(
//                                "account_" + user.getUserId(),
//                                user.getUserId(),
//                                1000.00 * i, // Different balances
//                                generateAccountNumber(user.getUserId()),
//                                System.currentTimeMillis()
//                        );
//
//                        crudManager.createAccount(account, accountSuccess -> {
//                            if (accountSuccess) {
//                                // Create sample loan for some customers
//                                if (i % 3 == 0) {
//                                    Loan loan = new Loan(
//                                            "loan_" + user.getUserId(),
//                                            user.getUserId(),
//                                            account.getAccountId(),
//                                            5000.00,
//                                            0.0,
//                                            0.0,
//                                            i % 2 == 0 ? "PENDING" : "APPROVED",
//                                            System.currentTimeMillis(),
//                                            i % 2 == 0 ? 0L : System.currentTimeMillis(),
//                                            5.0,
//                                            "Home Renovation"
//                                    );
//
//                                    crudManager.createLoan(loan, loanSuccess -> {});
//                                }
//                            }
//                        });
//                    }
//                }
//            });
//        }
//    }
//
//    private static String generateAccountNumber(String userId) {
//        long hash = Math.abs(userId.hashCode());
//        long timestamp = System.currentTimeMillis();
//        long combined = (hash + timestamp) % 10000000000L;
//        return String.format("%010d", combined);
//    }
//}