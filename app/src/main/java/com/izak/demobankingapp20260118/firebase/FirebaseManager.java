package com.izak.demobankingapp20260118.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.izak.demobankingapp20260118.models.Account;

import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private DatabaseReference database;

    private FirebaseCRUDManager crudManager;

    private FirebaseManager() {
        initializeFirebase();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    private void initializeFirebase() {
        // Firebase is automatically initialized in Android using google-services.json
        // No manual initialization needed
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        crudManager = FirebaseCRUDManager.getInstance();
        System.out.println("Firebase initialized successfully");
    }

    public FirebaseCRUDManager getCrudManager() {
        return crudManager;
    }



    public FirebaseAuth getAuth() {
        return auth;
    }

    public DatabaseReference getDatabase() {
        return database;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            String userId = user.getUid();

                            // Verify user data exists in database
                            database.child("users").child(userId).addListenerForSingleValueEvent(
                                    new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                callback.onSuccess(userId);
                                            } else {
                                                callback.onFailure("User data not found in database");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            callback.onFailure(error.getMessage());
                                        }
                                    }
                            );
                        } else {
                            callback.onFailure("Authentication failed");
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign in failed";
                        callback.onFailure(errorMessage);
                    }
                });
    }

    public void signUp(String email, String password, String role, SignUpCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            String userId = user.getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("role", role);
                            userData.put("isApproved", role.equals("ADMIN"));
                            userData.put("createdAt", System.currentTimeMillis());

                            database.child("users").child(userId).setValue(userData)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            callback.onSuccess(userId);
                                        } else {
                                            // If database write fails, delete the auth user
                                            user.delete();
                                            String errorMessage = dbTask.getException() != null
                                                    ? dbTask.getException().getMessage()
                                                    : "Failed to create user data";
                                            callback.onFailure(errorMessage);
                                        }
                                    });
                        } else {
                            callback.onFailure("User creation failed");
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign up failed";
                        callback.onFailure(errorMessage);
                    }
                });
    }

    public void signOut() {
        auth.signOut();
        System.out.println("User signed out successfully");
    }

    public void getCurrentUserRole(String userId, RoleCallback callback) {
        if (userId == null) {
            callback.onFailure("No user is currently signed in");
            return;
        }

        database.child("users").child(userId).child("role").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String role = snapshot.getValue(String.class);
                            callback.onSuccess(role);
                        } else {
                            callback.onFailure("User role not found");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                }
        );
    }

    public void isUserApproved(String userId, ApprovalCallback callback) {
        System.out.println("DEBUG isUserApproved: Checking approval for userId: " + userId);

        database.child("users").child(userId).child("isApproved").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println("DEBUG isUserApproved: Snapshot exists: " + snapshot.exists());

                        if (snapshot.exists()) {
                            Boolean isApproved = snapshot.getValue(Boolean.class);
                            System.out.println("DEBUG isUserApproved: isApproved value: " + isApproved);
                            callback.onResult(isApproved != null && isApproved);
                        } else {
                            System.out.println("DEBUG isUserApproved: No isApproved field found for user");
                            callback.onResult(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        System.out.println("DEBUG isUserApproved: Database error - " + error.getMessage());
                        callback.onResult(false);
                    }
                }
        );
    }


    public DatabaseReference getUsersRef() {
        return database.child("users");
    }

    public DatabaseReference getAccountsRef() {
        return database.child("accounts");
    }

    public DatabaseReference getTransactionsRef() {
        return database.child("transactions");
    }

    public DatabaseReference getLoansRef() {
        return database.child("loans");
    }

    public DatabaseReference getLoanRepaymentsRef() {
        return database.child("loanRepayments");
    }

    public interface AuthCallback {
        void onSuccess(String userId);
        void onFailure(String error);
    }

    public interface SignUpCallback {
        void onSuccess(String userId);
        void onFailure(String error);
    }

    public interface RoleCallback {
        void onSuccess(String role);
        void onFailure(String error);
    }

    public interface ApprovalCallback {
        void onResult(boolean isApproved);
    }


    public void createAccountForUser(String userId, double initialBalance, String accountNumber, OnAccountCreatedListener listener) {
        String accountId = getAccountsRef().push().getKey();
        if (accountId == null) {
            listener.onFailure("Failed to generate account ID");
            return;
        }

        Account account = new Account(
                accountId,
                userId,
                initialBalance,
                accountNumber,
                System.currentTimeMillis()
        );

        crudManager.createAccount(account, success -> {
            if (success) {
                listener.onSuccess(account);
            } else {
                listener.onFailure("Failed to create account");
            }
        });
    }

    public interface OnAccountCreatedListener {
        void onSuccess(Account account);
        void onFailure(String error);
    }

}