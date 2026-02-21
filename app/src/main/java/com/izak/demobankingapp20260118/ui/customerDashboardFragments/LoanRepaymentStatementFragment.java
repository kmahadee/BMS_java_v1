package com.izak.demobankingapp20260118.ui.customerDashboardFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.izak.demobankingapp20260118.adapters.LoanRepaymentAdapter;
import com.izak.demobankingapp20260118.database.DatabaseHelper;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Loan;
import com.izak.demobankingapp20260118.models.LoanRepayment;
import com.izak.demobankingapp20260118.ui.CustomerDashboardActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoanRepaymentStatementFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearProgressIndicator progressBar;
    private View emptyView;
    private TextView emptyTitleText;
    private TextView emptyMessageText;
    private TextView summaryBorrowedText;
    private TextView summaryRepaidText;
    private TextView summaryRemainingText;
    private View summaryCard;

    private LoanRepaymentAdapter adapter;
    private List<Loan> loanList;
    private Map<String, List<LoanRepayment>> repaymentsMap;
    private DatabaseHelper databaseHelper;
    private FirebaseManager firebaseManager;
    private String currentUserId;
    private ValueEventListener loansListener;

    // Summary totals
    private double totalBorrowed = 0;
    private double totalRepaid = 0;
    private double totalRemaining = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_loan_repayment_statement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        databaseHelper = DatabaseHelper.getInstance(requireContext());

        // Get user ID from parent activity
        if (getActivity() instanceof CustomerDashboardActivity) {
            CustomerDashboardActivity activity = (CustomerDashboardActivity) getActivity();
            currentUserId = activity.getCurrentUserId();
        }

        if (currentUserId == null) {
            Toast.makeText(getContext(), "Unable to load user information", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        initializeViews(view);
        setupRecyclerView();
        loadLoanData();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewLoanRepayments);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        emptyTitleText = view.findViewById(R.id.emptyTitleText);
        emptyMessageText = view.findViewById(R.id.emptyMessageText);
        summaryCard = view.findViewById(R.id.summaryCard);
        summaryBorrowedText = view.findViewById(R.id.summaryBorrowedText);
        summaryRepaidText = view.findViewById(R.id.summaryRepaidText);
        summaryRemainingText = view.findViewById(R.id.summaryRemainingText);

        // Set initial empty view texts
        emptyTitleText.setText("No Loan History");
        emptyMessageText.setText("You haven't borrowed any loans yet.");
    }

    private void setupRecyclerView() {
        loanList = new ArrayList<>();
        repaymentsMap = new HashMap<>();
        adapter = new LoanRepaymentAdapter(loanList, repaymentsMap);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadLoanData() {
        progressBar.setVisibility(View.VISIBLE);
        summaryCard.setVisibility(View.GONE);

        loansListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loanList.clear();
                repaymentsMap.clear();
                resetSummaryTotals();

                if (!snapshot.exists()) {
                    progressBar.setVisibility(View.GONE);
                    updateEmptyState();
                    return;
                }

                int totalLoans = (int) snapshot.getChildrenCount();
                final int[] loadedLoans = {0};

                for (DataSnapshot loanSnapshot : snapshot.getChildren()) {
                    Loan loan = loanSnapshot.getValue(Loan.class);
                    if (loan != null && currentUserId.equals(loan.getUserId())) {
                        loan.setLoanId(loanSnapshot.getKey());

                        // Only show APPROVED, REJECTED, or FULLY_PAID loans (skip PENDING)
                        String status = loan.getStatus();
                        if ("APPROVED".equals(status) || "REJECTED".equals(status) || "FULLY_PAID".equals(status)) {
                            loanList.add(loan);

                            // Update summary totals
                            updateSummaryWithLoan(loan);

                            // Load repayments for this loan
                            loadRepaymentsForLoan(loan, totalLoans, loadedLoans);
                        } else {
                            loadedLoans[0]++;
                            checkAllLoansLoaded(totalLoans, loadedLoans[0]);
                        }
                    } else {
                        loadedLoans[0]++;
                        checkAllLoansLoaded(totalLoans, loadedLoans[0]);
                    }
                }

                // Handle case where no valid loans found
                if (totalLoans == 0) {
                    progressBar.setVisibility(View.GONE);
                    updateEmptyState();
                }
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

    private void loadRepaymentsForLoan(Loan loan, int totalLoans, int[] loadedLoans) {
        new Thread(() -> {
            // Load repayments from SQLite
            List<LoanRepayment> repayments = databaseHelper.getAllRepaymentsForLoan(loan.getLoanId());

            // Sort by timestamp (newest first)
            repayments.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

            // Calculate running remaining balance for each repayment
            calculateRepaymentDetails(loan, repayments);

            // Store in map
            repaymentsMap.put(loan.getLoanId(), repayments);

            // Update UI on main thread
            requireActivity().runOnUiThread(() -> {
                loadedLoans[0]++;
                checkAllLoansLoaded(totalLoans, loadedLoans[0]);
            });
        }).start();
    }

    private void calculateRepaymentDetails(Loan loan, List<LoanRepayment> repayments) {
        double approvedAmount = loan.getApprovedAmount();
        double remainingAmount = loan.getRemainingAmount();

        // Sort repayments chronologically (oldest first) for calculation
        List<LoanRepayment> chronologicalRepayments = new ArrayList<>(repayments);
        chronologicalRepayments.sort((r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));

        // Calculate remaining balance after each repayment
        double runningBalance = approvedAmount;
        for (LoanRepayment repayment : chronologicalRepayments) {
            runningBalance -= repayment.getAmount();
            repayment.setRemainingAfter(runningBalance);
        }
    }

    private void checkAllLoansLoaded(int totalLoans, int loadedCount) {
        if (loadedCount >= totalLoans) {
            progressBar.setVisibility(View.GONE);

            // Sort loans by requested date (newest first)
            loanList.sort((l1, l2) -> Long.compare(l2.getRequestedAt(), l1.getRequestedAt()));

            adapter.notifyDataSetChanged();
            updateSummaryDisplay();
            updateEmptyState();
        }
    }

    private void resetSummaryTotals() {
        totalBorrowed = 0;
        totalRepaid = 0;
        totalRemaining = 0;
    }

    private void updateSummaryWithLoan(Loan loan) {
        String status = loan.getStatus();
        double approvedAmount = loan.getApprovedAmount();

        if ("APPROVED".equals(status) || "FULLY_PAID".equals(status)) {
            totalBorrowed += approvedAmount;

            // Calculate repaid amount
            double repaidAmount = approvedAmount - loan.getRemainingAmount();
            totalRepaid += repaidAmount;

            // Add remaining amount
            totalRemaining += loan.getRemainingAmount();
        }
    }

    private void updateSummaryDisplay() {
        if (totalBorrowed > 0) {
            summaryCard.setVisibility(View.VISIBLE);

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

            summaryBorrowedText.setText(currencyFormat.format(totalBorrowed));
            summaryRepaidText.setText(currencyFormat.format(totalRepaid));
            summaryRemainingText.setText(currencyFormat.format(totalRemaining));

            // Color code the remaining amount
            if (totalRemaining > 0) {
                summaryRemainingText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                summaryRemainingText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        } else {
            summaryCard.setVisibility(View.GONE);
        }
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
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (currentUserId != null) {
            loadLoanData();
        }
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