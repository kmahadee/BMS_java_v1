package com.izak.demobankingapp20260118.ui.customerDashboardFragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Loan;
import com.izak.demobankingapp20260118.ui.CustomerDashboardActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestLoanFragment extends Fragment {

    private TextInputLayout amountLayout;
    private TextInputLayout purposeLayout;
    private TextInputEditText amountInput;
    private TextInputEditText purposeInput;
    private Button submitButton;
    private ProgressBar progressBar;
    private TextView termsTextView;
    private TextView loanStatusTextView;

    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private String accountId;
    private boolean hasPendingLoan = false;
    private boolean hasUnpaidLoan = false;

    // Loan constants
    private static final double MAX_LOAN_AMOUNT = 100000.00;
    private static final double MIN_LOAN_AMOUNT = 100.00;
    private static final double INTEREST_RATE = 5.0; // 5% annual interest

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_request_loan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Get account data from parent activity
        if (getActivity() instanceof CustomerDashboardActivity) {
            CustomerDashboardActivity activity = (CustomerDashboardActivity) getActivity();
            currentUserId = activity.getCurrentUserId();
            accountId = activity.getAccountId();
        }

        if (currentUserId == null || accountId == null) {
            Toast.makeText(getContext(), "Unable to load account information", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        initializeViews(view);
        setupLoanTerms();
        setupSubmitButton();
        checkExistingLoans();
    }

    private void initializeViews(View view) {
        amountLayout = view.findViewById(R.id.amountLayout);
        purposeLayout = view.findViewById(R.id.purposeLayout);
        amountInput = view.findViewById(R.id.amountInput);
        purposeInput = view.findViewById(R.id.purposeInput);
        submitButton = view.findViewById(R.id.submitButton);
        progressBar = view.findViewById(R.id.progressBar);
        termsTextView = view.findViewById(R.id.termsTextView);
        loanStatusTextView = view.findViewById(R.id.loanStatusTextView);

        // Set hint for amount input
        amountInput.setHint(String.format("Up to $%,.2f", MAX_LOAN_AMOUNT));
    }

    private void setupLoanTerms() {
        String termsText = "Loan Terms and Conditions:\n\n" +
                "1. Maximum loan amount: $" + String.format("%,.2f", MAX_LOAN_AMOUNT) + "\n" +
                "2. Minimum loan amount: $" + String.format("%,.2f", MIN_LOAN_AMOUNT) + "\n" +
                "3. Annual interest rate: " + INTEREST_RATE + "%\n" +
                "4. Loan processing time: 1-3 business days\n" +
                "5. Loan tenure: 12-36 months\n" +
                "6. Late payment fee: 2% of monthly installment\n" +
                "7. Early repayment allowed with no penalty\n\n" +
                "By submitting a loan request, you agree to these terms.";

        termsTextView.setText(termsText);
    }

    private void setupSubmitButton() {
        submitButton.setOnClickListener(v -> {
            if (validateInputs()) {
                submitLoanRequest();
            }
        });
    }

    private void checkExistingLoans() {
        showLoading(true);

        DatabaseReference loansRef = firebaseManager.getLoansRef();

        loansRef.orderByChild("userId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        hasPendingLoan = false;
                        hasUnpaidLoan = false;

                        if (snapshot.exists()) {
                            for (DataSnapshot loanSnapshot : snapshot.getChildren()) {
                                Loan loan = loanSnapshot.getValue(Loan.class);
                                if (loan != null) {
                                    String status = loan.getStatus();

                                    if ("PENDING".equals(status)) {
                                        hasPendingLoan = true;
                                        showLoanStatus("You already have a pending loan request. Please wait for approval.");
                                    } else if ("APPROVED".equals(status) && loan.getRemainingAmount() > 0) {
                                        hasUnpaidLoan = true;
                                        showLoanStatus("You have an unpaid loan of $" +
                                                String.format("%,.2f", loan.getRemainingAmount()) +
                                                ". Please repay before requesting a new loan.");
                                    }
                                }
                            }
                        }

                        updateSubmitButtonState();
                        showLoading(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(getContext(),
                                "Error checking loan status: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoanStatus(String message) {
        if (loanStatusTextView != null) {
            loanStatusTextView.setText(message);
            loanStatusTextView.setVisibility(View.VISIBLE);
        }
    }

    private void updateSubmitButtonState() {
        if (submitButton != null) {
            boolean canRequestLoan = !hasPendingLoan && !hasUnpaidLoan;
            submitButton.setEnabled(canRequestLoan);
            submitButton.setAlpha(canRequestLoan ? 1.0f : 0.5f);

            if (!canRequestLoan) {
                if (hasPendingLoan) {
                    submitButton.setText("Pending Loan Exists");
                } else if (hasUnpaidLoan) {
                    submitButton.setText("Unpaid Loan Exists");
                }
            } else {
                submitButton.setText("Submit Loan Request");
            }
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Clear previous errors
        amountLayout.setError(null);
        purposeLayout.setError(null);

        // Validate amount
        String amountStr = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            amountLayout.setError("Loan amount is required");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountStr);

                if (amount < MIN_LOAN_AMOUNT) {
                    amountLayout.setError("Minimum loan amount is $" + String.format("%,.2f", MIN_LOAN_AMOUNT));
                    isValid = false;
                } else if (amount > MAX_LOAN_AMOUNT) {
                    amountLayout.setError("Maximum loan amount is $" + String.format("%,.2f", MAX_LOAN_AMOUNT));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                amountLayout.setError("Invalid amount format");
                isValid = false;
            }
        }

        // Validate purpose
        String purpose = purposeInput.getText().toString().trim();
        if (TextUtils.isEmpty(purpose)) {
            purposeLayout.setError("Please describe the purpose of the loan");
            isValid = false;
        } else if (purpose.length() < 10) {
            purposeLayout.setError("Please provide a detailed purpose (min. 10 characters)");
            isValid = false;
        }

        // Check for existing loans
        if (hasPendingLoan) {
            Toast.makeText(getContext(), "You already have a pending loan request", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (hasUnpaidLoan) {
            Toast.makeText(getContext(), "You have an unpaid loan", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void submitLoanRequest() {
        showLoading(true);

        String loanId = UUID.randomUUID().toString();
        double requestedAmount = Double.parseDouble(amountInput.getText().toString().trim());
        String purpose = purposeInput.getText().toString().trim();
        long requestedAt = System.currentTimeMillis();

        // Create loan object
        Map<String, Object> loanData = new HashMap<>();
        loanData.put("loanId", loanId);
        loanData.put("userId", currentUserId);
        loanData.put("accountId", accountId);
        loanData.put("requestedAmount", requestedAmount);
        loanData.put("approvedAmount", 0.0);
        loanData.put("remainingAmount", 0.0);
        loanData.put("status", "PENDING");
        loanData.put("requestedAt", requestedAt);
        loanData.put("approvedAt", 0L);
        loanData.put("interestRate", 0.0);
        loanData.put("purpose", purpose);

        // Save to Firebase
        firebaseManager.getLoansRef().child(loanId).setValue(loanData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    showSuccessMessage(loanId, requestedAmount);
                    disableForm();
                    clearForm();
                    hasPendingLoan = true;
                    updateSubmitButtonState();

                    // Show loan status
                    showLoanStatus("Loan request submitted successfully! Your request ID: " +
                            loanId.substring(0, 8).toUpperCase());
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(),
                            "Failed to submit loan request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessMessage(String loanId, double amount) {
        String message = "✅ Loan Request Submitted Successfully!\n\n" +
                "Request ID: " + loanId.substring(0, 8).toUpperCase() + "\n" +
                "Amount: $" + String.format("%,.2f", amount) + "\n" +
                "Status: Pending Approval\n\n" +
                "Your request will be reviewed by an administrator within 1-3 business days.";

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Loan Request Submitted")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void disableForm() {
        amountInput.setEnabled(false);
        purposeInput.setEnabled(false);
        submitButton.setText("Request Submitted");
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.5f);
    }

    private void clearForm() {
        amountInput.setText("");
        purposeInput.setText("");
        amountLayout.setError(null);
        purposeLayout.setError(null);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (submitButton != null) {
            submitButton.setEnabled(!show);
            submitButton.setAlpha(show ? 0.5f : 1.0f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh loan status when fragment becomes visible
        if (currentUserId != null) {
            checkExistingLoans();
        }
    }
}