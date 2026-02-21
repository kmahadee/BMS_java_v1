package com.izak.demobankingapp20260118.ui.customerDashboardFragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.adapters.MyLoanAdapter;
import com.izak.demobankingapp20260118.database.DatabaseHelper;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;
import com.izak.demobankingapp20260118.models.Loan;
import com.izak.demobankingapp20260118.models.LoanRepayment;
import com.izak.demobankingapp20260118.models.TransactionRecord;
import com.izak.demobankingapp20260118.ui.CustomerDashboardActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MyLoansFragment extends Fragment implements MyLoanAdapter.OnLoanRepaymentListener {

    private RecyclerView recyclerView;
    private View emptyView;
    private LinearProgressIndicator progressBar;
    private TextView emptyTitleText;
    private TextView emptyMessageText;

    private MyLoanAdapter adapter;
    private List<Loan> loanList;
    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;
    private String currentUserId;
    private String accountId;
    private String accountNumber;
    private double currentBalance;
    private ValueEventListener loansListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_loans, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        databaseHelper = DatabaseHelper.getInstance(requireContext());

        // Get account data from parent activity
        if (getActivity() instanceof CustomerDashboardActivity) {
            CustomerDashboardActivity activity = (CustomerDashboardActivity) getActivity();
            currentUserId = activity.getCurrentUserId();
            accountId = activity.getAccountId();
            accountNumber = activity.getAccountNumber();
        }

        if (currentUserId == null) {
            Toast.makeText(getContext(), "Unable to load user information", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        initializeViews(view);
        setupRecyclerView();
        loadAccountBalance();
        loadUserLoans();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewLoans);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyTitleText = view.findViewById(R.id.emptyTitleText);
        emptyMessageText = view.findViewById(R.id.emptyMessageText);

        // Set empty view texts
        emptyTitleText.setText("No Loans Found");
        emptyMessageText.setText("You haven't applied for any loans yet.");
    }

    private void setupRecyclerView() {
        loanList = new ArrayList<>();
        adapter = new MyLoanAdapter(loanList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadAccountBalance() {
        if (accountId == null) return;

        firebaseManager.getAccountsRef().child(accountId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Account account = snapshot.getValue(Account.class);
                            if (account != null) {
                                currentBalance = account.getBalance();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Silently handle error
                    }
                });
    }

    private void loadUserLoans() {
        progressBar.setVisibility(View.VISIBLE);

        loansListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loanList.clear();

                if (!snapshot.exists()) {
                    progressBar.setVisibility(View.GONE);
                    updateEmptyState();
                    return;
                }

                for (DataSnapshot loanSnapshot : snapshot.getChildren()) {
                    Loan loan = loanSnapshot.getValue(Loan.class);
                    if (loan != null && currentUserId.equals(loan.getUserId())) {
                        loan.setLoanId(loanSnapshot.getKey());
                        loanList.add(loan);
                    }
                }

                // Sort by requested date (newest first)
                loanList.sort((l1, l2) -> Long.compare(l2.getRequestedAt(), l1.getRequestedAt()));

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(),
                        "Error loading loans: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        };

        firebaseManager.getLoansRef().orderByChild("userId").equalTo(currentUserId)
                .addValueEventListener(loansListener);
    }

    private void updateEmptyState() {
        if (loanList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRepayClicked(Loan loan) {
        showRepaymentDialog(loan);
    }

    private void showRepaymentDialog(Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Repay Loan");

        // Calculate max repayment amount (min of remaining amount and current balance)
        double maxRepayment = Math.min(loan.getRemainingAmount(), currentBalance);

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loan_repayment, null);

        TextView loanInfoText = dialogView.findViewById(R.id.loanInfoText);
        TextView remainingAmountText = dialogView.findViewById(R.id.remainingAmountText);
        TextView currentBalanceText = dialogView.findViewById(R.id.currentBalanceText);
        TextView maxAmountText = dialogView.findViewById(R.id.maxAmountText);
        EditText amountInput = dialogView.findViewById(R.id.amountInput);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // Set loan information
        loanInfoText.setText("Loan ID: " + loan.getLoanId().substring(0, 8).toUpperCase());
        remainingAmountText.setText("Remaining: " + currencyFormat.format(loan.getRemainingAmount()));
        currentBalanceText.setText("Your Balance: " + currencyFormat.format(currentBalance));
        maxAmountText.setText("Max: " + currencyFormat.format(maxRepayment));

        // Set hint for amount input
        amountInput.setHint("Enter amount up to " + currencyFormat.format(maxRepayment));

        builder.setView(dialogView);

        builder.setPositiveButton("Repay", (dialog, which) -> {
            String amountStr = amountInput.getText().toString().trim();
            if (validateRepaymentAmount(amountStr, loan.getRemainingAmount(), maxRepayment)) {
                double amount = Double.parseDouble(amountStr);
                processRepayment(loan, amount);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    private boolean validateRepaymentAmount(String amountStr, double remainingAmount, double maxAmount) {
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "Please enter repayment amount", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            if (amount <= 0) {
                Toast.makeText(getContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (amount > remainingAmount) {
                Toast.makeText(getContext(), "Amount exceeds remaining loan amount", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (amount > maxAmount) {
                Toast.makeText(getContext(), "Amount exceeds your current balance", Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid amount format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void processRepayment(Loan loan, double repaymentAmount) {
        // Start atomic transaction
        DatabaseReference loanRef = firebaseManager.getLoansRef().child(loan.getLoanId()).child("remainingAmount");
        DatabaseReference accountRef = firebaseManager.getAccountsRef().child(accountId).child("balance");

        // Update loan remaining amount (atomic operation)
        loanRef.setValue(loan.getRemainingAmount() - repaymentAmount)
                .addOnSuccessListener(aVoid -> {
                    // Update account balance
                    accountRef.setValue(currentBalance - repaymentAmount)
                            .addOnSuccessListener(aVoid1 -> {
                                // Update local balance
                                currentBalance -= repaymentAmount;

                                // Create repayment record
                                createRepaymentRecord(loan, repaymentAmount);

                                // Create transaction record
                                createTransactionRecord(loan, repaymentAmount);

                                // Check if loan is fully paid
                                double newRemainingAmount = loan.getRemainingAmount() - repaymentAmount;
                                if (newRemainingAmount <= 0.01) { // Using epsilon for floating point comparison
                                    markLoanAsPaid(loan.getLoanId());
                                }

                                showRepaymentSuccess(loan, repaymentAmount);
                            })
                            .addOnFailureListener(e -> {
                                // Rollback loan update
                                loanRef.setValue(loan.getRemainingAmount());
                                Toast.makeText(getContext(),
                                        "Repayment failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Repayment failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });



        Map<String, Object> updates = new HashMap<>();
        updates.put("remainingAmount", loan.getRemainingAmount() - repaymentAmount);

        firebaseManager.getCrudManager().updateLoan(
                loan.getLoanId(),
                updates,
                success -> {
                    if (success) {
                        // Update account balance using CRUD manager
                        firebaseManager.getCrudManager().updateAccountBalance(
                                accountId,
                                currentBalance - repaymentAmount,
                                accountSuccess -> {
                                    if (accountSuccess) {
                                        // Create repayment and transaction records
                                        createRepaymentRecord(loan, repaymentAmount);
                                        createTransactionRecord(loan, repaymentAmount);

                                        // Check if fully paid
                                        if (loan.getRemainingAmount() - repaymentAmount <= 0.01) {
                                            firebaseManager.getCrudManager().markLoanAsFullyPaid(
                                                    loan.getLoanId(),
                                                    paidSuccess -> {}
                                            );
                                        }

                                        showRepaymentSuccess(loan, repaymentAmount);
                                    } else {
                                        // Rollback loan update
                                        updates.put("remainingAmount", loan.getRemainingAmount());
                                        firebaseManager.getCrudManager().updateLoan(
                                                loan.getLoanId(),
                                                updates,
                                                rollbackSuccess -> {}
                                        );
                                        Toast.makeText(getContext(),
                                                "Failed to update account balance",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    } else {
                        Toast.makeText(getContext(),
                                "Repayment failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );


    }

    private void createRepaymentRecord(Loan loan, double amount) {
        String repaymentId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        double remainingAfter = loan.getRemainingAmount() - amount;


        LoanRepayment repayment = new LoanRepayment(
                repaymentId,
                loan.getLoanId(),
                amount,
                timestamp,
                remainingAfter
        );

        // Save to SQLite
        databaseHelper.insertLoanRepayment(repayment);
    }

    private void createTransactionRecord(Loan loan, double amount) {
        String transactionId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        double balanceAfter = currentBalance - amount;


        TransactionRecord transaction = new TransactionRecord(
                transactionId,
                accountNumber,
                "LOAN_REPAYMENT",
                amount,
                "Loan repayment - Loan ID: " + loan.getLoanId().substring(0, 8).toUpperCase(),
                timestamp,
                "BANK",
                balanceAfter
        );

        // Save to SQLite
        databaseHelper.insertTransactionRecord(transaction);
    }

    private void markLoanAsPaid(String loanId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "FULLY_PAID");
        updates.put("remainingAmount", 0.0);

        firebaseManager.getLoansRef().child(loanId).updateChildren(updates);
    }

    private void showRepaymentSuccess(Loan loan, double amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        String message = "✅ Repayment Successful!\n\n" +
                "Amount: " + currencyFormat.format(amount) + "\n" +
                "Loan ID: " + loan.getLoanId().substring(0, 8).toUpperCase() + "\n" +
                "Time: " + sdf.format(new Date()) + "\n\n" +
                "Your repayment has been processed successfully.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Payment Confirmed")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove listener to prevent memory leaks
        if (loansListener != null) {
            firebaseManager.getLoansRef().removeEventListener(loansListener);
        }

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}