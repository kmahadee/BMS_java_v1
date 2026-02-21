package com.izak.demobankingapp20260118.ui.customerDashboardFragments;

import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.adapters.TransactionAdapter;
import com.izak.demobankingapp20260118.database.DatabaseHelper;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;
import com.izak.demobankingapp20260118.models.TransactionRecord;
import com.izak.demobankingapp20260118.ui.CustomerDashboardActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionStatementFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChipGroup filterChipGroup;
    private Chip allTransactionsChip;
    private Chip transfersChip;
    private Chip loansChip;
    private Button dateRangeButton;
    private Button clearDatesButton;
    private ProgressBar progressBar; // FIXED: Changed from TextView to ProgressBar
    private View emptyView;
    private TextView emptyTitleText;
    private TextView emptyMessageText;
    private TextView dateRangeText;
    private TextView balanceSummaryText;

    private TransactionAdapter adapter;
    private List<TransactionRecord> allTransactions;
    private List<TransactionRecord> filteredTransactions;
    private DatabaseHelper databaseHelper;
    private FirebaseManager firebaseManager;
    private String accountNumber;
    private String accountId;
    private String currentFilter = "ALL";
    private long startDate = 0;
    private long endDate = 0;
    private double currentBalance = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_statement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        databaseHelper = DatabaseHelper.getInstance(requireContext());

        // Get account data from parent activity
        if (getActivity() instanceof CustomerDashboardActivity) {
            CustomerDashboardActivity activity = (CustomerDashboardActivity) getActivity();
            accountNumber = activity.getAccountNumber();
            accountId = activity.getAccountId();
        }

        if (accountNumber == null || accountId == null) {
            Toast.makeText(getContext(), "Unable to load account information", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        initializeViews(view);
        setupRecyclerView();
        setupFilterChips();
        setupDateRangeButton();
        setupClearDatesButton();
        loadCurrentBalance();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewTransactions);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        allTransactionsChip = view.findViewById(R.id.allTransactionsChip);
        transfersChip = view.findViewById(R.id.transfersChip);
        loansChip = view.findViewById(R.id.loansChip);
        dateRangeButton = view.findViewById(R.id.dateRangeButton);
        clearDatesButton = view.findViewById(R.id.clearDatesButton);
        progressBar = view.findViewById(R.id.progressBar); // FIXED: Now correctly casting to ProgressBar
        emptyView = view.findViewById(R.id.emptyView);
        emptyTitleText = view.findViewById(R.id.emptyTitleText);
        emptyMessageText = view.findViewById(R.id.emptyMessageText);
        dateRangeText = view.findViewById(R.id.dateRangeText);
        balanceSummaryText = view.findViewById(R.id.balanceSummaryText);

        // Set initial date range text
        updateDateRangeText();
        updateBalanceSummary(0, 0, 0);
    }

    private void setupRecyclerView() {
        allTransactions = new ArrayList<>();
        filteredTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(filteredTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterChips() {
        allTransactionsChip.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilters();
            }
        });

        transfersChip.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                currentFilter = "TRANSFER";
                applyFilters();
            }
        });

        loansChip.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                currentFilter = "LOAN";
                applyFilters();
            }
        });
    }

    private void setupDateRangeButton() {
        dateRangeButton.setOnClickListener(v -> showDateRangePicker());
    }

    private void setupClearDatesButton() {
        clearDatesButton.setOnClickListener(v -> {
            startDate = 0;
            endDate = 0;
            updateDateRangeText();
            applyFilters();
        });
    }

    private void showDateRangePicker() {
        // ADDED: Context check before showing dialog
        if (!isAdded() || getContext() == null) {
            return;
        }

        // Create date range picker
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> datePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .setSelection(androidx.core.util.Pair.create(
                                startDate > 0 ? startDate : null,
                                endDate > 0 ? endDate : null
                        ))
                        .build();

        datePicker.addOnPositiveButtonClickListener(
                (MaterialPickerOnPositiveButtonClickListener<androidx.core.util.Pair<Long, Long>>) selection -> {
                    if (!isAdded() || getContext() == null) return; // ADDED: Safety check

                    startDate = selection.first;
                    endDate = selection.second;
                    updateDateRangeText();
                    applyFilters();
                });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateDateRangeText() {
        // ADDED: Context check
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (startDate == 0 || endDate == 0) {
            dateRangeText.setText("All Dates");
            clearDatesButton.setVisibility(View.GONE);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String startDateStr = sdf.format(new Date(startDate));
            String endDateStr = sdf.format(new Date(endDate));
            dateRangeText.setText(startDateStr + " - " + endDateStr);
            clearDatesButton.setVisibility(View.VISIBLE);
        }
    }

    private void loadCurrentBalance() {
        if (accountId == null) return;

        firebaseManager.getAccountsRef().child(accountId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // ADDED: Context check before processing data
                        if (!isAdded() || getContext() == null) {
                            return;
                        }

                        if (snapshot.exists()) {
                            Account account = snapshot.getValue(Account.class);
                            if (account != null) {
                                currentBalance = account.getBalance();
                                // Now load transactions with the current balance
                                loadTransactions();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // ADDED: Context check before showing toast
                        if (!isAdded() || getContext() == null) {
                            return;
                        }

                        Toast.makeText(getContext(),
                                "Error loading balance: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTransactions() {
        // ADDED: Context check
        if (!isAdded() || getContext() == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        new Thread(() -> {
            // Load all transactions for this account from SQLite
            List<TransactionRecord> transactions = databaseHelper.getAllTransactionRecords();

            // Filter by account number
            allTransactions.clear();
            for (TransactionRecord transaction : transactions) {
                if (accountNumber.equals(transaction.getAccountNumber())) {
                    allTransactions.add(transaction);
                }
            }

            // Sort by timestamp (newest first)
            allTransactions.sort((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));

            // Calculate running balance for each transaction
            calculateRunningBalance();

            // ADDED: Check if fragment is still attached before updating UI
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null) return; // ADDED: Safety check

                    progressBar.setVisibility(View.GONE);
                    applyFilters();
                    updateEmptyState();
                });
            }
        }).start();
    }

    private void calculateRunningBalance() {
        // Sort by timestamp (oldest first) for balance calculation
        List<TransactionRecord> sortedTransactions = new ArrayList<>(allTransactions);
        sortedTransactions.sort((t1, t2) -> Long.compare(t1.getTimestamp(), t2.getTimestamp()));

        double runningBalance = currentBalance;

        // Calculate running balance from latest to earliest (backwards)
        for (int i = sortedTransactions.size() - 1; i >= 0; i--) {
            TransactionRecord transaction = sortedTransactions.get(i);
            double amount = transaction.getAmount();
            String type = transaction.getType();

            // Determine if it's credit or debit
            boolean isCredit = isCreditTransaction(type);

            // For display purposes, we want balance before this transaction
            // So we subtract credits and add debits
            if (isCredit) {
                runningBalance -= amount;
            } else {
                runningBalance += amount;
            }

            // Store the balance AFTER this transaction (for display)
            // To get balance after, we need to reverse the calculation
            double balanceAfter = isCredit ? runningBalance + amount : runningBalance - amount;
            transaction.setBalanceAfter(balanceAfter);
        }

        // Restore original order (newest first)
        allTransactions.sort((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
    }

    private boolean isCreditTransaction(String type) {
        if (type == null) return false;
        return type.equals("TRANSFER_IN") || type.equals("DEPOSIT") || type.equals("LOAN_DISBURSEMENT");
    }

    private void applyFilters() {
        // ADDED: Context check
        if (!isAdded() || getContext() == null) {
            return;
        }

        filteredTransactions.clear();

        // Filter by date and type
        for (TransactionRecord transaction : allTransactions) {
            // Apply date filter
            if (startDate > 0 && endDate > 0) {
                long timestamp = transaction.getTimestamp();
                if (timestamp < startDate || timestamp > endDate) {
                    continue;
                }
            }

            // Apply type filter
            String type = transaction.getType();
            switch (currentFilter) {
                case "ALL":
                    filteredTransactions.add(transaction);
                    break;
                case "TRANSFER":
                    if (type != null && (type.contains("TRANSFER") || type.contains("TRANSFER_"))) {
                        filteredTransactions.add(transaction);
                    }
                    break;
                case "LOAN":
                    if (type != null && (type.contains("LOAN") || type.contains("REPAYMENT"))) {
                        filteredTransactions.add(transaction);
                    }
                    break;
            }
        }

        adapter.updateTransactions(filteredTransactions);

        // Calculate and display summary
        calculateSummary();
        updateEmptyState();
    }

    private void calculateSummary() {
        double totalCredit = 0;
        double totalDebit = 0;
        int transactionCount = filteredTransactions.size();

        for (TransactionRecord transaction : filteredTransactions) {
            double amount = transaction.getAmount();
            String type = transaction.getType();

            if (isCreditTransaction(type)) {
                totalCredit += amount;
            } else {
                totalDebit += amount;
            }
        }

        updateBalanceSummary(totalCredit, totalDebit, transactionCount);
    }

    private void updateBalanceSummary(double totalCredit, double totalDebit, int transactionCount) {
        // ADDED: Context check
        if (!isAdded() || getContext() == null) {
            return;
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        String summaryText = String.format(Locale.getDefault(),
                "Transactions: %d | Credit: %s | Debit: %s",
                transactionCount,
                currencyFormat.format(totalCredit),
                currencyFormat.format(totalDebit));

        balanceSummaryText.setText(summaryText);
    }

    private void updateEmptyState() {
        // ADDED: Context check
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (filteredTransactions.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);

            // Set appropriate empty message
            if (allTransactions.isEmpty()) {
                emptyTitleText.setText("No Transactions");
                emptyMessageText.setText("You haven't made any transactions yet.");
            } else {
                emptyTitleText.setText("No Matching Transactions");
                emptyMessageText.setText("No transactions found for the selected filters.");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh transactions when fragment becomes visible
        if (accountId != null) {
            loadCurrentBalance();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}