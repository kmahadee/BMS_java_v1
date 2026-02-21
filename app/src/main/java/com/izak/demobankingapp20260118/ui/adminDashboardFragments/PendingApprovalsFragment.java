package com.izak.demobankingapp20260118.ui.adminDashboardFragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.adapters.PendingApprovalAdapter;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;
import com.izak.demobankingapp20260118.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingApprovalsFragment extends Fragment implements PendingApprovalAdapter.OnApprovalActionListener {

    private RecyclerView recyclerView;
    private View emptyView;
    private ProgressBar progressBar;
    private PendingApprovalAdapter adapter;
    private List<User> pendingUsers;
    private FirebaseManager firebaseManager;
    private ValueEventListener usersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_approvals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        pendingUsers = new ArrayList<>();

        initializeViews(view);
        setupRecyclerView();
        loadPendingApprovals();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewPendingApprovals);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new PendingApprovalAdapter(pendingUsers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadPendingApprovals() {
        showLoading(true);
        DatabaseReference usersRef = firebaseManager.getUsersRef();

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Run on UI thread to ensure proper updates
                if (getActivity() == null || !isAdded()) {
                    return; // Fragment not attached
                }

                getActivity().runOnUiThread(() -> {
                    pendingUsers.clear();

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        try {
                            User user = userSnapshot.getValue(User.class);

                            Boolean rawApproved = userSnapshot.child("isApproved").getValue(Boolean.class);

                            System.out.println(
                                    "---------------RAW------------------------RAW DB VALUE = " + rawApproved +
                                            " | MODEL VALUE = " + user.isApproved()
                            );


                            if (user != null) {
                                user.setUserId(userSnapshot.getKey());

                                // Debug logging
                                 System.out.println("--------------------User Info---------------------DEBUG: User " + user.getEmail() +
                                        " - Role: " + user.getRole() +
                                        ", Approved: " + user.isApproved());

                                // Only add customers who are NOT approved
                                if ("CUSTOMER".equals(user.getRole()) && !user.isApproved()) {
                                    pendingUsers.add(user);
                                     System.out.println("-----------------------------------------DEBUG: Added to pending list: " + user.getEmail());
                                }
                            }
                        } catch (Exception e) {
                             System.out.println("-----------------------------------------DEBUG: Error parsing user: " + e.getMessage());
                        }
                    }

                     System.out.println("-----------------------------------------DEBUG: Total pending users: " + pendingUsers.size());

                    // Notify adapter of complete dataset change
                    adapter.notifyDataSetChanged();
                    showLoading(false);
                    updateEmptyState();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() == null || !isAdded()) {
                    return;
                }

                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(),
                            "Error loading pending approvals: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        };

        usersRef.addValueEventListener(usersListener);
    }

    private void updateEmptyState() {
        if (pendingUsers.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onApprove(User user) {
        showInitialBalanceDialog(user);
    }

    @Override
    public void onReject(User user) {
        showRejectConfirmationDialog(user);
    }

    private void showInitialBalanceDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Set Initial Balance");
        builder.setMessage("Enter the initial balance for " + user.getEmail());

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("0.00");
        builder.setView(input);

        builder.setPositiveButton("Approve", (dialog, which) -> {
            String balanceStr = input.getText().toString().trim();

            if (balanceStr.isEmpty()) {
                Toast.makeText(getContext(), "----------------Toast---------------Please enter an initial balance", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double initialBalance = Double.parseDouble(balanceStr);

                if (initialBalance < 0) {
                    Toast.makeText(getContext(), "----------------Toast---------------Balance must be greater than or equal to 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                approveUser(user, initialBalance);

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "----------------Toast---------------Invalid balance amount", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void approveUser(User user, double initialBalance) {
        showLoading(true);
         System.out.println("-----------------------------------------DEBUG: Starting approval for user: " + user.getUserId() + ", email: " + user.getEmail());

        // Update user approval status
        firebaseManager.getCrudManager().approveUser(user.getUserId(), success -> {
            if (success) {
                 System.out.println("-----------------------------------------DEBUG: User approval status updated successfully-------------------------");
                // Create account for the user
                createAccountForUser(user, initialBalance);
            } else {
                showLoading(false);
                 System.out.println("-----------------------------------------DEBUG: Failed to approve user in database");
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Failed to approve user. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createAccountForUser(User user, double initialBalance) {
        String accountId = firebaseManager.getAccountsRef().push().getKey();

        if (accountId == null) {
            showLoading(false);
            if (getContext() != null) {
                Toast.makeText(getContext(), "----------------Toast---------------Failed to generate account ID", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Generate unique 10-digit account number
        String accountNumber = generateAccountNumber(user.getUserId());

         System.out.println("-----------------------------------------DEBUG: Creating account - ID: " + accountId + ", Number: " + accountNumber + ", Balance: " + initialBalance);

        // Create account using FirebaseManager helper
        firebaseManager.createAccountForUser(
                user.getUserId(),
                initialBalance,
                accountNumber,
                new FirebaseManager.OnAccountCreatedListener() {
                    @Override
                    public void onSuccess(Account account) {
                        showLoading(false);
                         System.out.println("-----------------------------------------DEBUG: Account created successfully for user: " + user.getEmail());

                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "✅ User approved successfully!\n\n" +
                                            "Email: " + user.getEmail() + "\n" +
                                            "Account Number: " + accountNumber + "\n" +
                                            "Initial Balance: $" + String.format("%.2f", initialBalance),
                                    Toast.LENGTH_LONG).show();
                        }

                        // Force immediate refresh - the listener should handle this automatically
                        // but we'll trigger it manually as backup
                        loadPendingApprovals();
                    }

                    @Override
                    public void onFailure(String error) {
                        showLoading(false);
                         System.out.println("-----------------------------------------DEBUG: Failed to create account: " + error);

                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Failed to create account: " + error + "\nRolling back approval...",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Rollback approval
                        Map<String, Object> rollbackUpdate = new HashMap<>();
                        rollbackUpdate.put("isApproved", false);
                        firebaseManager.getCrudManager().updateUser(
                                user.getUserId(),
                                rollbackUpdate,
                                rollbackSuccess -> {
                                    if (rollbackSuccess) {
                                         System.out.println("-----------------------------------------DEBUG: Successfully rolled back approval");
                                    } else {
                                         System.out.println("-----------------------------------------DEBUG: Failed to rollback approval");
                                    }
                                }
                        );
                    }
                }
        );
    }

    private String generateAccountNumber(String userId) {
        // Generate a unique 10-digit account number
        long hash = Math.abs(userId.hashCode());
        long timestamp = System.currentTimeMillis();
        long combined = (hash + timestamp) % 10000000000L;

        return String.format("%010d", combined);
    }

    private void showRejectConfirmationDialog(User user) {
        new AlertDialog.Builder(getContext())
                .setTitle("Reject User")
                .setMessage("Are you sure you want to reject " + user.getEmail() + "? This will permanently delete their account.")
                .setPositiveButton("Reject", (dialog, which) -> rejectUser(user))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void rejectUser(User user) {
        showLoading(true);

        // Delete user from Firebase Database
        firebaseManager.getUsersRef().child(user.getUserId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "User " + user.getEmail() + " has been rejected",
                                Toast.LENGTH_SHORT).show();
                    }
                    // Force refresh
                    loadPendingApprovals();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Failed to reject user: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setAlpha(show ? 0.5f : 1.0f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove listener to prevent memory leaks
        if (usersListener != null && firebaseManager != null) {
            firebaseManager.getUsersRef().removeEventListener(usersListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (firebaseManager != null) {
            loadPendingApprovals();
        }
    }
}



//package com.izak.demobankingapp20260118.ui.adminDashboardFragments;
//
//import android.app.AlertDialog;
//import android.os.Bundle;
//import android.text.InputType;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.ValueEventListener;
//import com.izak.demobankingapp20260118.R;
//import com.izak.demobankingapp20260118.adapters.PendingApprovalAdapter;
//import com.izak.demobankingapp20260118.firebase.FirebaseManager;
//import com.izak.demobankingapp20260118.models.Account;
//import com.izak.demobankingapp20260118.models.User;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class PendingApprovalsFragment extends Fragment implements PendingApprovalAdapter.OnApprovalActionListener {
//
//    private RecyclerView recyclerView;
//    private View emptyView;
//    private ProgressBar progressBar;  // ADD THIS
//    private PendingApprovalAdapter adapter;
//    private List<User> pendingUsers;
//    private FirebaseManager firebaseManager;
//    private ValueEventListener usersListener;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater,
//                             @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_pending_approvals, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        firebaseManager = FirebaseManager.getInstance();
//        pendingUsers = new ArrayList<>();
//
//        initializeViews(view);
//        setupRecyclerView();
//        loadPendingApprovals();
//    }
//
//    private void initializeViews(View view) {
//        recyclerView = view.findViewById(R.id.recyclerViewPendingApprovals);
//        emptyView = view.findViewById(R.id.emptyView);
//        progressBar = view.findViewById(R.id.progressBar);  // ADD THIS - make sure this ID exists in your layout
//    }
//
//    private void setupRecyclerView() {
//        adapter = new PendingApprovalAdapter(pendingUsers, this);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//        recyclerView.setAdapter(adapter);
//    }
//
//    private void loadPendingApprovals() {
//        showLoading(true);  // Show loading when starting to load
//        DatabaseReference usersRef = firebaseManager.getUsersRef();
//
//        usersListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                pendingUsers.clear();
//
//                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
//                    User user = userSnapshot.getValue(User.class);
//
//                    if (user != null &&
//                            "CUSTOMER".equals(user.getRole()) &&
//                            !user.isApproved()) {
//                        user.setUserId(userSnapshot.getKey());
//                        pendingUsers.add(user);
//                    }
//                }
//
//                adapter.notifyDataSetChanged();
//                showLoading(false);  // Hide loading when done
//                updateEmptyState();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                showLoading(false);  // Hide loading on error
//                Toast.makeText(getContext(),
//                        "Error loading pending approvals: " + error.getMessage(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        };
//
//        usersRef.addValueEventListener(usersListener);
//    }
//
//    private void updateEmptyState() {
//        if (pendingUsers.isEmpty()) {
//            recyclerView.setVisibility(View.GONE);
//            emptyView.setVisibility(View.VISIBLE);
//        } else {
//            recyclerView.setVisibility(View.VISIBLE);
//            emptyView.setVisibility(View.GONE);
//        }
//    }
//
//    @Override
//    public void onApprove(User user) {
//        showInitialBalanceDialog(user);
//    }
//
//    @Override
//    public void onReject(User user) {
//        showRejectConfirmationDialog(user);
//    }
//
//    private void showInitialBalanceDialog(User user) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//        builder.setTitle("Set Initial Balance");
//        builder.setMessage("Enter the initial balance for " + user.getEmail());
//
//        final EditText input = new EditText(getContext());
//        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
//        input.setHint("0.00");
//        builder.setView(input);
//
//        builder.setPositiveButton("Approve", (dialog, which) -> {
//            String balanceStr = input.getText().toString().trim();
//
//            if (balanceStr.isEmpty()) {
//                Toast.makeText(getContext(), "----------------Toast---------------Please enter an initial balance", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            try {
//                double initialBalance = Double.parseDouble(balanceStr);
//
//                if (initialBalance < 0) {
//                    Toast.makeText(getContext(), "----------------Toast---------------Balance must be greater than or equal to 0", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                approveUser(user, initialBalance);
//
//            } catch (NumberFormatException e) {
//                Toast.makeText(getContext(), "----------------Toast---------------Invalid balance amount", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
//        builder.show();
//    }
//
//    private void approveUser(User user, double initialBalance) {
//        showLoading(true);
//         System.out.println("-----------------------------------------DEBUG: Starting approval for user: " + user.getUserId() + ", email: " + user.getEmail());
//
//        // Update user approval status using CRUD Manager (single source of truth)
//        firebaseManager.getCrudManager().approveUser(user.getUserId(), success -> {
//            if (success) {
//                 System.out.println("-----------------------------------------DEBUG: User approval status updated successfully");
//                // Create account for the user
//                createAccountForUser(user, initialBalance);
//            } else {
//                showLoading(false);
//                 System.out.println("-----------------------------------------DEBUG: Failed to approve user in database");
//                Toast.makeText(getContext(),
//                        "Failed to approve user. Please try again.",
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void createAccountForUser(User user, double initialBalance) {
//        String accountId = firebaseManager.getAccountsRef().push().getKey();
//
//        if (accountId == null) {
//            showLoading(false);
//            Toast.makeText(getContext(), "----------------Toast---------------Failed to generate account ID", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Generate unique 10-digit account number
//        String accountNumber = generateAccountNumber(user.getUserId());
//
//         System.out.println("-----------------------------------------DEBUG: Creating account - ID: " + accountId + ", Number: " + accountNumber + ", Balance: " + initialBalance);
//
//        // Create account using FirebaseManager helper
//        firebaseManager.createAccountForUser(
//                user.getUserId(),
//                initialBalance,
//                accountNumber,
//                new FirebaseManager.OnAccountCreatedListener() {
//                    @Override
//                    public void onSuccess(Account account) {
//                        showLoading(false);
//                         System.out.println("-----------------------------------------DEBUG: Account created successfully for user: " + user.getEmail());
//
//                        Toast.makeText(getContext(),
//                                "✅ User approved successfully!\n\n" +
//                                        "Email: " + user.getEmail() + "\n" +
//                                        "Account Number: " + accountNumber + "\n" +
//                                        "Initial Balance: $" + String.format("%.2f", initialBalance),
//                                Toast.LENGTH_LONG).show();
//                    }
//
//                    @Override
//                    public void onFailure(String error) {
//                        showLoading(false);
//                         System.out.println("-----------------------------------------DEBUG: Failed to create account: " + error);
//
//                        Toast.makeText(getContext(),
//                                "Failed to create account: " + error + "\nRolling back approval...",
//                                Toast.LENGTH_SHORT).show();
//
//                        // Rollback approval using CRUD manager
//                        Map<String, Object> rollbackUpdate = new HashMap<>();
//                        rollbackUpdate.put("isApproved", false);
//                        firebaseManager.getCrudManager().updateUser(
//                                user.getUserId(),
//                                rollbackUpdate,
//                                rollbackSuccess -> {
//                                    if (rollbackSuccess) {
//                                         System.out.println("-----------------------------------------DEBUG: Successfully rolled back approval");
//                                    } else {
//                                         System.out.println("-----------------------------------------DEBUG: Failed to rollback approval");
//                                    }
//                                }
//                        );
//                    }
//                }
//        );
//    }
//
//    private String generateAccountNumber(String userId) {
//        // Generate a unique 10-digit account number
//        long hash = Math.abs(userId.hashCode());
//        long timestamp = System.currentTimeMillis();
//        long combined = (hash + timestamp) % 10000000000L; // Ensure 10 digits
//
//        // Pad with zeros if needed
//        return String.format("%010d", combined);
//    }
//
//    private void showRejectConfirmationDialog(User user) {
//        new AlertDialog.Builder(getContext())
//                .setTitle("Reject User")
//                .setMessage("Are you sure you want to reject " + user.getEmail() + "? This will permanently delete their account.")
//                .setPositiveButton("Reject", (dialog, which) -> rejectUser(user))
//                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show();
//    }
//
//    private void rejectUser(User user) {
//        showLoading(true);
//
//        // Delete user from Firebase Database
//        firebaseManager.getUsersRef().child(user.getUserId()).removeValue()
//                .addOnSuccessListener(aVoid -> {
//                    showLoading(false);
//                    Toast.makeText(getContext(),
//                            "User " + user.getEmail() + " has been rejected",
//                            Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    showLoading(false);
//                    Toast.makeText(getContext(),
//                            "Failed to reject user: " + e.getMessage(),
//                            Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    // ADD THIS METHOD - Show/hide loading indicator
//    private void showLoading(boolean show) {
//        if (progressBar != null) {
//            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
//        }
//        if (recyclerView != null) {
//            recyclerView.setAlpha(show ? 0.5f : 1.0f);
//        }
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//
//        // Remove listener to prevent memory leaks
//        if (usersListener != null) {
//            firebaseManager.getUsersRef().removeEventListener(usersListener);
//        }
//    }
//}