package com.izak.demobankingapp20260118.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.models.User;

public class FirebaseDatabaseInitializer {

    public static void initializeDatabase() {
        DatabaseReference rootRef = FirebaseManager.getInstance().getDatabase();

        // Ensure all required paths exist
        String[] requiredPaths = {
                "users",
                "accounts",
                "loans",
                "loanRepayments",
                "transactions"
        };

        for (String path : requiredPaths) {
            rootRef.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        rootRef.child(path).setValue("{}");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Handle error
                }
            });
        }
    }



    public static void initializeSampleData() {
        FirebaseCRUDManager crudManager = FirebaseCRUDManager.getInstance();

        // Create sample admin user (only if doesn't exist)
        DatabaseReference usersRef = FirebaseManager.getInstance().getDatabase().child("users");

        usersRef.orderByChild("email").equalTo("admin@bank.com")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            User adminUser = new User(
                                    "admin_user_id",  // You should use actual auth UID
                                    "admin@bank.com",
                                    "ADMIN",
                                    true,
                                    System.currentTimeMillis()
                            );

                            crudManager.createUser(adminUser, success -> {
                                if (success) {
                                    System.out.println("Admin user created successfully");
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        System.out.println("Error checking admin user: " + error.getMessage());
                    }
                });
    }




//===================================

    public static void checkDatabaseStructure(OnStructureCheckedListener listener) {
        DatabaseReference rootRef = FirebaseManager.getInstance().getDatabase();

        String[] requiredPaths = {
                "users",
                "accounts",
                "loans",
                "loanRepayments",
                "transactions"
        };

        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean allPathsExist = true;
                for (String path : requiredPaths) {
                    if (!snapshot.hasChild(path)) {
                        allPathsExist = false;
                        break;
                    }
                }
                listener.onChecked(allPathsExist);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onChecked(false);
            }
        });
    }

    // Add this interface as well
    public interface OnStructureCheckedListener {
        void onChecked(boolean isProperlyInitialized);
    }




}