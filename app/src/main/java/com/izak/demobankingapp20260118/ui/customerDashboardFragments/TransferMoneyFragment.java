package com.izak.demobankingapp20260118.ui.customerDashboardFragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.adapters.RecentTransferAdapter;
import com.izak.demobankingapp20260118.database.DatabaseHelper;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;
import com.izak.demobankingapp20260118.models.TransactionRecord;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransferMoneyFragment extends Fragment {

    private TextInputLayout recipientAccountLayout;
    private TextInputLayout amountLayout;
    private TextInputLayout descriptionLayout;
    private TextInputEditText recipientAccountInput;
    private TextInputEditText amountInput;
    private TextInputEditText descriptionInput;
    private Button transferButton;
    private ProgressBar progressBar;
    private RecyclerView recentTransfersRecyclerView;
    private RecentTransferAdapter recentTransferAdapter;

    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;
    private String currentUserId;
    private String senderAccountId;
    private String senderAccountNumber;
    private double currentBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer_money, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        databaseHelper = DatabaseHelper.getInstance(requireContext());


        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        initializeViews(view);
        setupTransferButton();
        loadSenderAccount();
        loadRecentTransfers();
    }

    private void initializeViews(View view) {
        recipientAccountLayout = view.findViewById(R.id.recipientAccountLayout);
        amountLayout = view.findViewById(R.id.amountLayout);
        descriptionLayout = view.findViewById(R.id.descriptionLayout);
        recipientAccountInput = view.findViewById(R.id.recipientAccountInput);
        amountInput = view.findViewById(R.id.amountInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        transferButton = view.findViewById(R.id.transferButton);
        progressBar = view.findViewById(R.id.progressBarTransfer);
        recentTransfersRecyclerView = view.findViewById(R.id.recentTransfersRecyclerView);

        // Setup RecyclerView for recent transfers
        recentTransferAdapter = new RecentTransferAdapter(new ArrayList<>());
        recentTransfersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentTransfersRecyclerView.setAdapter(recentTransferAdapter);
    }

    private void setupTransferButton() {
        transferButton.setOnClickListener(v -> {
            if (validateInputs()) {
                showConfirmationDialog();
            }
        });
    }

    private void loadSenderAccount() {
        showLoading(true);
        DatabaseReference accountsRef = firebaseManager.getAccountsRef();

        accountsRef.orderByChild("userId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        showLoading(false);
                        if (snapshot.exists()) {
                            for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
                                Account account = accountSnapshot.getValue(Account.class);
                                if (account != null) {
                                    senderAccountId = accountSnapshot.getKey();
                                    senderAccountNumber = account.getAccountNumber();
                                    currentBalance = account.getBalance();
                                }
                                break;
                            }
                        } else {
                            Toast.makeText(getContext(), "No account found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Clear previous errors
        recipientAccountLayout.setError(null);
        amountLayout.setError(null);

        // Validate recipient account number
        String recipientAccount = recipientAccountInput.getText().toString().trim();
        if (TextUtils.isEmpty(recipientAccount)) {
            recipientAccountLayout.setError("Account number is required");
            isValid = false;
        } else if (!recipientAccount.matches("\\d{10}")) {
            recipientAccountLayout.setError("Account number must be exactly 10 digits");
            isValid = false;
        } else if (recipientAccount.equals(senderAccountNumber)) {
            recipientAccountLayout.setError("Cannot transfer to your own account");
            isValid = false;
        }

        // Validate amount
        String amountStr = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            amountLayout.setError("Amount is required");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    amountLayout.setError("Amount must be greater than 0");
                    isValid = false;
                } else if (amount > currentBalance) {
                    amountLayout.setError("Insufficient balance");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                amountLayout.setError("Invalid amount");
                isValid = false;
            }
        }

        return isValid;
    }

    private void showConfirmationDialog() {
        String recipientAccount = recipientAccountInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        double amount = Double.parseDouble(amountStr);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        String formattedAmount = currencyFormat.format(amount);

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Transfer")
                .setMessage("Transfer " + formattedAmount + " to account " + recipientAccount + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    verifyRecipientAndTransfer(recipientAccount, amount);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void verifyRecipientAndTransfer(String recipientAccountNumber, double amount) {
        showLoading(true);
        DatabaseReference accountsRef = firebaseManager.getAccountsRef();

        accountsRef.orderByChild("accountNumber").equalTo(recipientAccountNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
                                String recipientAccountId = accountSnapshot.getKey();
                                Account recipientAccount = accountSnapshot.getValue(Account.class);

                                if (recipientAccount != null) {
                                    // Verify recipient is not sender
                                    if (recipientAccountId.equals(senderAccountId)) {
                                        showLoading(false);
                                        Toast.makeText(getContext(), "Cannot transfer to yourself", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    performTransfer(recipientAccountId, recipientAccountNumber, amount);
                                }
                                break;
                            }
                        } else {
                            showLoading(false);
                            recipientAccountLayout.setError("Recipient account not found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void performTransfer(String recipientAccountId, String recipientAccountNumber, double amount) {
        final String transactionId = UUID.randomUUID().toString();
        final String description = descriptionInput.getText().toString().trim();
        final long timestamp = System.currentTimeMillis();

        DatabaseReference senderRef = firebaseManager.getAccountsRef().child(senderAccountId).child("balance");
        DatabaseReference recipientRef = firebaseManager.getAccountsRef().child(recipientAccountId).child("balance");

        // Update sender balance using Firebase Transaction
        senderRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Double senderBalance = mutableData.getValue(Double.class);
                if (senderBalance == null) {
                    return Transaction.abort();
                }

                if (senderBalance < amount) {
                    return Transaction.abort();
                }

                mutableData.setValue(senderBalance - amount);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (committed) {
                    // Update recipient balance
                    recipientRef.runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                            Double recipientBalance = mutableData.getValue(Double.class);
                            if (recipientBalance == null) {
                                recipientBalance = 0.0;
                            }

                            mutableData.setValue(recipientBalance + amount);
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                            showLoading(false);
                            if (committed) {
                                // Save transactions to SQLite
                                saveTransactionToSQLite(transactionId, recipientAccountNumber, amount, description, timestamp, true);
                                saveTransactionToSQLite(transactionId, senderAccountNumber, amount, description, timestamp, false);

                                // Update current balance
                                currentBalance -= amount;

                                showSuccessDialog(transactionId, recipientAccountNumber, amount, timestamp);
                                clearForm();
                                loadRecentTransfers();
                            } else {
                                Toast.makeText(getContext(), "Transfer failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    showLoading(false);
                    if (error != null) {
                        Toast.makeText(getContext(), "Transfer failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Insufficient balance", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    private void saveTransactionToSQLite(String transactionId, String otherAccount, double amount,
                                         String description, long timestamp, boolean isSent) {
        TransactionRecord transaction = new TransactionRecord();
        transaction.setTransactionId(transactionId);
        transaction.setAccountNumber(isSent ? senderAccountNumber : otherAccount);
        transaction.setType(isSent ? "TRANSFER_OUT" : "TRANSFER_IN");
        transaction.setAmount(amount);
        transaction.setDescription(TextUtils.isEmpty(description) ?
                (isSent ? "Transfer to " + otherAccount : "Transfer from " + otherAccount) : description);
        transaction.setTimestamp(timestamp);
        transaction.setRecipientAccount(isSent ? otherAccount : senderAccountNumber);

//        databaseHelper.insertTransaction(transaction);
        databaseHelper.insertTransactionRecord(transaction);

    }

    private void showSuccessDialog(String transactionId, String recipientAccount, double amount, long timestamp) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        String message = "Transaction ID: " + transactionId + "\n" +
                "Amount: " + currencyFormat.format(amount) + "\n" +
                "To: " + recipientAccount + "\n" +
                "Date: " + dateFormat.format(new Date(timestamp)) + "\n\n" +
                "Transfer completed successfully!";

        new AlertDialog.Builder(requireContext())
                .setTitle("Transfer Successful")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void clearForm() {
        recipientAccountInput.setText("");
        amountInput.setText("");
        descriptionInput.setText("");
        recipientAccountLayout.setError(null);
        amountLayout.setError(null);
    }

    private void loadRecentTransfers() {
        List<TransactionRecord> recentTransfers = databaseHelper.getRecentTransactions(5);
        recentTransferAdapter.updateTransfers(recentTransfers);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (transferButton != null) {
            transferButton.setEnabled(!show);
            transferButton.setAlpha(show ? 0.5f : 1.0f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        if (databaseHelper != null) {
//            databaseHelper.close();
//        }
    }
}