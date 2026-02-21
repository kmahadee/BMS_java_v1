package com.izak.demobankingapp20260118.ui.adminDashboardFragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.izak.demobankingapp20260118.adapters.PendingLoanAdapter;
import com.izak.demobankingapp20260118.database.DatabaseHelper;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;
import com.izak.demobankingapp20260118.models.Loan;
import com.izak.demobankingapp20260118.models.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PendingLoansFragment extends Fragment implements PendingLoanAdapter.OnLoanActionListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private PendingLoanAdapter adapter;
    private List<Loan> pendingLoans;
    private FirebaseManager firebaseManager;
    private DatabaseHelper databaseHelper;
    private ValueEventListener loansListener;
    private static final double DEFAULT_INTEREST_RATE = 5.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_loans, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        databaseHelper = DatabaseHelper.getInstance(getContext());
        pendingLoans = new ArrayList<>();

        initializeViews(view);
        setupRecyclerView();
        loadPendingLoans();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewPendingLoans);
        emptyView = view.findViewById(R.id.emptyViewLoans);
    }

    private void setupRecyclerView() {
        adapter = new PendingLoanAdapter(pendingLoans, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadPendingLoans() {
        DatabaseReference loansRef = firebaseManager.getLoansRef();

        loansListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingLoans.clear();

                for (DataSnapshot loanSnapshot : snapshot.getChildren()) {
                    Loan loan = loanSnapshot.getValue(Loan.class);

                    if (loan != null && "PENDING".equals(loan.getStatus())) {
                        loan.setLoanId(loanSnapshot.getKey());
                        pendingLoans.add(loan);

                        // Fetch customer email for each loan
                        fetchCustomerEmail(loan, pendingLoans.size() - 1);
                    }
                }

                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Error loading pending loans: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        loansRef.addValueEventListener(loansListener);
    }

    private void fetchCustomerEmail(Loan loan, int position) {
        firebaseManager.getUsersRef().child(loan.getUserId()).child("email")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String email = snapshot.getValue(String.class);
                            adapter.updateCustomerEmail(position, email);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Ignore error, keep "Loading..." text
                    }
                });
    }

    private void updateEmptyState() {
        if (pendingLoans.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onApprove(Loan loan) {
        showApprovalDialog(loan);
    }

    @Override
    public void onReject(Loan loan) {
        showRejectionDialog(loan);
    }

    private void showApprovalDialog(Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Approve Loan Request");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView messageText = new TextView(getContext());
        messageText.setText("Requested Amount: $" + String.format("%.2f", loan.getRequestedAmount()));
        messageText.setPadding(0, 0, 0, 20);
        layout.addView(messageText);

        TextView approvedLabel = new TextView(getContext());
        approvedLabel.setText("Approved Amount:");
        layout.addView(approvedLabel);

        final EditText approvedAmountInput = new EditText(getContext());
        approvedAmountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        approvedAmountInput.setText(String.valueOf(loan.getRequestedAmount()));
        approvedAmountInput.setHint("Enter approved amount");
        layout.addView(approvedAmountInput);

        TextView interestLabel = new TextView(getContext());
        interestLabel.setText("Interest Rate (%):");
        interestLabel.setPadding(0, 20, 0, 0);
        layout.addView(interestLabel);

        final EditText interestRateInput = new EditText(getContext());
        interestRateInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        interestRateInput.setText(String.valueOf(DEFAULT_INTEREST_RATE));
        interestRateInput.setHint("Enter interest rate");
        layout.addView(interestRateInput);

        builder.setView(layout);

        builder.setPositiveButton("Approve", (dialog, which) -> {
            String approvedAmountStr = approvedAmountInput.getText().toString().trim();
            String interestRateStr = interestRateInput.getText().toString().trim();

            if (approvedAmountStr.isEmpty() || interestRateStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double approvedAmount = Double.parseDouble(approvedAmountStr);
                double interestRate = Double.parseDouble(interestRateStr);

                if (approvedAmount <= 0) {
                    Toast.makeText(getContext(), "Approved amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (interestRate < 0 || interestRate > 100) {
                    Toast.makeText(getContext(), "Interest rate must be between 0 and 100", Toast.LENGTH_SHORT).show();
                    return;
                }

                approveLoan(loan, approvedAmount, interestRate);

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid input values", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void approveLoan(Loan loan, double approvedAmount, double interestRate) {
        // Calculate remaining amount with interest
        double remainingAmount = approvedAmount * (1 + interestRate / 100);

        // Update loan details
        Map<String, Object> loanUpdates = new HashMap<>();
        loanUpdates.put("status", "APPROVED");
        loanUpdates.put("approvedAmount", approvedAmount);
        loanUpdates.put("remainingAmount", remainingAmount);
        loanUpdates.put("interestRate", interestRate);
        loanUpdates.put("approvedAt", System.currentTimeMillis());

        firebaseManager.getLoansRef().child(loan.getLoanId()).updateChildren(loanUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Update account balance
                    updateAccountBalance(loan, approvedAmount);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Failed to approve loan: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

        firebaseManager.getCrudManager().approveLoan(
                loan.getLoanId(),
                approvedAmount,
                interestRate,
                success -> {
                    if (success) {
                        updateAccountBalance(loan, approvedAmount);
                    } else {
                        Toast.makeText(getContext(),
                                "Failed to approve loan",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void updateAccountBalance(Loan loan, double approvedAmount) {
        DatabaseReference accountRef = firebaseManager.getAccountsRef().child(loan.getAccountId());

        accountRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Account account = snapshot.getValue(Account.class);
                    if (account != null) {
                        double newBalance = account.getBalance() + approvedAmount;

                        accountRef.child("balance").setValue(newBalance)
                                .addOnSuccessListener(aVoid -> {
                                    // Create transaction record
                                    createLoanDisbursementTransaction(loan, approvedAmount);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(),
                                            "Failed to update account balance: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    rollbackLoanApproval(loan.getLoanId());
                                });
                    }
                } else {
                    Toast.makeText(getContext(), "Account not found", Toast.LENGTH_SHORT).show();
                    rollbackLoanApproval(loan.getLoanId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Error fetching account: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                rollbackLoanApproval(loan.getLoanId());
            }
        });
    }

    private void createLoanDisbursementTransaction(Loan loan, double approvedAmount) {
        String transactionId = UUID.randomUUID().toString();

        Transaction transaction = new Transaction(
                transactionId,
                null, // From bank (no account)
                loan.getAccountId(),
                approvedAmount,
                "LOAN_DISBURSEMENT",
                System.currentTimeMillis(),
                "Loan disbursement - Loan ID: " + loan.getLoanId()
        );

        // Save to SQLite
        long result = databaseHelper.insertTransaction(transaction);

        if (result > 0) {
            Toast.makeText(getContext(),
                    "Loan approved successfully! Amount: $" + String.format("%.2f", approvedAmount),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(),
                    "Loan approved but transaction record failed to save locally",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void rollbackLoanApproval(String loanId) {
        Map<String, Object> rollbackUpdates = new HashMap<>();
        rollbackUpdates.put("status", "PENDING");
        rollbackUpdates.put("approvedAmount", 0);
        rollbackUpdates.put("remainingAmount", 0);
        rollbackUpdates.put("interestRate", 0);
        rollbackUpdates.put("approvedAt", 0);

        firebaseManager.getLoansRef().child(loanId).updateChildren(rollbackUpdates);
    }

    private void showRejectionDialog(Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Reject Loan Request");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView messageText = new TextView(getContext());
        messageText.setText("Are you sure you want to reject this loan request?\n\nAmount: $" +
                String.format("%.2f", loan.getRequestedAmount()));
        messageText.setPadding(0, 0, 0, 20);
        layout.addView(messageText);

        TextView reasonLabel = new TextView(getContext());
        reasonLabel.setText("Reason (optional):");
        layout.addView(reasonLabel);

        final EditText reasonInput = new EditText(getContext());
        reasonInput.setHint("Enter rejection reason");
        reasonInput.setMinLines(2);
        layout.addView(reasonInput);

        builder.setView(layout);

        builder.setPositiveButton("Reject", (dialog, which) -> {
            String reason = reasonInput.getText().toString().trim();
            rejectLoan(loan, reason);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }

    private void rejectLoan(Loan loan, String reason) {
        Map<String, Object> loanUpdates = new HashMap<>();
        loanUpdates.put("status", "REJECTED");
        loanUpdates.put("rejectedAt", System.currentTimeMillis());

        if (!reason.isEmpty()) {
            loanUpdates.put("rejectionReason", reason);
        }

        firebaseManager.getLoansRef().child(loan.getLoanId()).updateChildren(loanUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            "Loan request rejected successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Failed to reject loan: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (loansListener != null) {
            firebaseManager.getLoansRef().removeEventListener(loansListener);
        }
    }
}