package com.izak.demobankingapp20260118.ui.customerDashboardFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountBalanceFragment extends Fragment {

    private TextView balanceText;
    private TextView accountNumberText;
    private TextView lastUpdatedText;
    private TextView accountStatusText;
    private ProgressBar progressBar;
    private FloatingActionButton refreshButton;
    private View balanceCard;
    private View errorView;
    private TextView errorMessageText;

    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;
    private ValueEventListener accountListener;
    private String currentUserId;
    private String accountId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_balance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            showError("User not authenticated");
            return;
        }

        initializeViews(view);
        setupRefreshButton();
        loadAccountBalance();
    }

    private void initializeViews(View view) {
        balanceText = view.findViewById(R.id.balanceText);
        accountNumberText = view.findViewById(R.id.accountNumberText);
        lastUpdatedText = view.findViewById(R.id.lastUpdatedText);
        accountStatusText = view.findViewById(R.id.accountStatusText);
        progressBar = view.findViewById(R.id.progressBarBalance);
        refreshButton = view.findViewById(R.id.refreshButton);
        balanceCard = view.findViewById(R.id.balanceCard);
        errorView = view.findViewById(R.id.errorView);
        errorMessageText = view.findViewById(R.id.errorMessageText);
    }

    private void setupRefreshButton() {
        refreshButton.setOnClickListener(v -> {
            // Animate the refresh button
            v.animate().rotation(360f).setDuration(500).withEndAction(() -> {
                v.setRotation(0f);
            }).start();

            refreshAccountBalance();
        });
    }

    private void loadAccountBalance() {
        showLoading(true);
        hideError();

        // First, get the account ID for the current user
        DatabaseReference accountsRef = firebaseManager.getAccountsRef();

        accountsRef.orderByChild("userId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // ADDED: Check if fragment is still attached
                        if (!isAdded() || getContext() == null) {
                            return;
                        }

                        if (snapshot.exists()) {
                            for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
                                accountId = accountSnapshot.getKey();
                                // Set up real-time listener for this account
                                setupRealtimeListener();
                                break;
                            }
                        } else {
                            showLoading(false);
                            showError("No account found for this user");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // ADDED: Check if fragment is still attached
                        if (!isAdded() || getContext() == null) {
                            return;
                        }

                        showLoading(false);
                        showError("Error loading account: " + error.getMessage());
                    }
                });
    }

    private void setupRealtimeListener() {
        if (accountId == null) return;

        DatabaseReference accountRef = firebaseManager.getAccountsRef().child(accountId);

        accountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // ADDED: Check if fragment is still attached before processing
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showLoading(false);

                if (snapshot.exists()) {
                    Account account = snapshot.getValue(Account.class);
                    if (account != null) {
                        account.setAccountId(accountId);
                        displayAccountData(account);
                        hideError();
                    } else {
                        showError("Failed to parse account data");
                    }
                } else {
                    showError("Account not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ADDED: Check if fragment is still attached before processing
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showLoading(false);
                showError("Database error: " + error.getMessage());
                Toast.makeText(getContext(),
                        "Failed to load balance: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        accountRef.addValueEventListener(accountListener);
    }

    private void refreshAccountBalance() {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (accountId == null) {
            Toast.makeText(getContext(), "No account loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        hideError();

        // The real-time listener will automatically update the UI
        // This method just provides user feedback
        DatabaseReference accountRef = firebaseManager.getAccountsRef().child(accountId);
        accountRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // ADDED: Check if fragment is still attached
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showLoading(false);
                if (snapshot.exists()) {
                    Toast.makeText(getContext(), "Balance refreshed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ADDED: Check if fragment is still attached
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showLoading(false);
                Toast.makeText(getContext(),
                        "Refresh failed: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAccountData(Account account) {
        // ADDED: Safety check at the beginning - this was the main crash point
        if (!isAdded() || getContext() == null) {
            return;
        }

        // Format and display balance
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        String formattedBalance = currencyFormat.format(account.getBalance());
        balanceText.setText(formattedBalance);

        // Set balance color based on value
        int balanceColor;
        if (account.getBalance() >= 1000) {
//            balanceColor = getResources().getColor(android.R.color.white);
            balanceColor = getResources().getColor(android.R.color.holo_green_dark);
        } else if (account.getBalance() >= 0) {
            balanceColor = getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            balanceColor = getResources().getColor(android.R.color.holo_red_dark);
        }
        balanceText.setTextColor(balanceColor);

        // Display account number
        accountNumberText.setText("Account: " + account.getAccountNumber());

        // Display last updated timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        String formattedDate = sdf.format(new Date());
        lastUpdatedText.setText("Last updated: " + formattedDate);

        // Display account status
        String status = account.getBalance() >= 0 ? "Good Standing" : "Overdrawn";
        accountStatusText.setText("Status: " + status);
        accountStatusText.setTextColor(balanceColor);

        // Show the balance card
        balanceCard.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        // ADDED: Null checks for safety
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (refreshButton != null) {
            refreshButton.setEnabled(!show);
            refreshButton.setAlpha(show ? 0.5f : 1.0f);
        }
    }

    private void showError(String message) {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (errorView != null) {
            errorView.setVisibility(View.VISIBLE);
        }
        if (errorMessageText != null) {
            errorMessageText.setText(message);
        }
        if (balanceCard != null) {
            balanceCard.setVisibility(View.GONE);
        }
    }

    private void hideError() {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove listener to prevent memory leaks
        if (accountListener != null && accountId != null) {
            firebaseManager.getAccountsRef().child(accountId)
                    .removeEventListener(accountListener);
        }
    }
}