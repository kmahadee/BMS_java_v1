package com.izak.demobankingapp20260118.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.database.*;
import com.izak.demobankingapp20260118.models.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseCRUDManager {

    private static FirebaseCRUDManager instance;
    private final DatabaseReference database;

    private FirebaseCRUDManager() {
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FirebaseCRUDManager getInstance() {
        if (instance == null) {
            instance = new FirebaseCRUDManager();
        }
        return instance;
    }

    // ==================== USER CRUD OPERATIONS ====================

    public void createUser(User user, Consumer<Boolean> callback) {
        Map<String, Object> userValues = user.toMap();
        database.child("users").child(user.getUserId()).setValue(userValues)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void getUser(String userId, Consumer<User> callback) {
        database.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                user.setUserId(snapshot.getKey());
                                callback.accept(user);
                            } else {
                                callback.accept(null);
                            }
                        } else {
                            callback.accept(null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(null);
                    }
                }
        );
    }

    public void updateUser(String userId, Map<String, Object> updates, Consumer<Boolean> callback) {
        database.child("users").child(userId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void deleteUser(String userId, Consumer<Boolean> callback) {
        database.child("users").child(userId).removeValue()
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void approveUser(String userId, Consumer<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isApproved", true);
        updateUser(userId, updates, callback);
    }

    public void getAllUsers(Consumer<Map<String, User>> callback) {
        database.child("users").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, User> users = new HashMap<>();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            User user = userSnapshot.getValue(User.class);
                            if (user != null) {
                                user.setUserId(userSnapshot.getKey());
                                users.put(userSnapshot.getKey(), user);
                            }
                        }
                        callback.accept(users);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(new HashMap<>());
                    }
                }
        );
    }

    // ==================== ACCOUNT CRUD OPERATIONS ====================

    public void createAccount(Account account, Consumer<Boolean> callback) {
        Map<String, Object> accountValues = new HashMap<>();
        accountValues.put("userId", account.getUserId());
        accountValues.put("balance", account.getBalance());
        accountValues.put("accountNumber", account.getAccountNumber());
        accountValues.put("createdAt", account.getCreatedAt());

        database.child("accounts").child(account.getAccountId()).setValue(accountValues)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void getAccount(String accountId, Consumer<Account> callback) {
        database.child("accounts").child(accountId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Account account = snapshot.getValue(Account.class);
                            if (account != null) {
                                account.setAccountId(snapshot.getKey());
                                callback.accept(account);
                            } else {
                                callback.accept(null);
                            }
                        } else {
                            callback.accept(null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(null);
                    }
                }
        );
    }

    public void getAccountByUserId(String userId, Consumer<Account> callback) {
        database.child("accounts").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
                                Account account = accountSnapshot.getValue(Account.class);
                                if (account != null) {
                                    account.setAccountId(accountSnapshot.getKey());
                                    callback.accept(account);
                                    return;
                                }
                            }
                        }
                        callback.accept(null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(null);
                    }
                });
    }

    public void updateAccountBalance(String accountId, double newBalance, Consumer<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("balance", newBalance);

        database.child("accounts").child(accountId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void updateAccount(String accountId, Map<String, Object> updates, Consumer<Boolean> callback) {
        database.child("accounts").child(accountId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }
    public void getAllAccounts(Consumer<Map<String, Account>> callback) {
        System.out.println("------------DEBUG------------ getAllAccounts() called");

        database.child("accounts").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        System.out.println("------------DEBUG------------ onDataChange triggered");
                        System.out.println("------------DEBUG------------ accounts node exists: " + snapshot.exists());
                        System.out.println("------------DEBUG------------ total children: " + snapshot.getChildrenCount());

                        Map<String, Account> accounts = new HashMap<>();

                        for (DataSnapshot accountSnapshot : snapshot.getChildren()) {

                            System.out.println("------------DEBUG------------ reading account key: " + accountSnapshot.getKey());

                            Account account = accountSnapshot.getValue(Account.class);

                            if (account == null) {
                                System.out.println("------------DEBUG------------ Account model is NULL for key: "
                                        + accountSnapshot.getKey());
                                continue;
                            }

                            account.setAccountId(accountSnapshot.getKey());

                            // Optional: log important fields
                            System.out.println(
                                    "------------DEBUG------------ Account loaded | ID: " + account.getAccountId()
                                            + ", UserId: " + account.getUserId()
                                            + ", Balance: " + account.getBalance()
                                            + ", Number: " + account.getAccountNumber()
                            );

                            accounts.put(accountSnapshot.getKey(), account);
                        }

                        System.out.println("------------DEBUG------------ TOTAL accounts loaded: " + accounts.size());
                        callback.accept(accounts);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        System.out.println("------------DEBUG------------ getAllAccounts CANCELLED");
                        System.out.println("------------DEBUG------------ error: " + error.getMessage());
                        callback.accept(new HashMap<>());
                    }
                }
        );
    }

//    public void getAllAccounts(Consumer<Map<String, Account>> callback) {
//        database.child("accounts").addListenerForSingleValueEvent(
//                new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        Map<String, Account> accounts = new HashMap<>();
//                        for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
//                            Account account = accountSnapshot.getValue(Account.class);
//                            if (account != null) {
//                                account.setAccountId(accountSnapshot.getKey());
//                                accounts.put(accountSnapshot.getKey(), account);
//                            }
//                        }
//                        callback.accept(accounts);
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        callback.accept(new HashMap<>());
//                    }
//                }
//        );
//    }

    public void getAccountByAccountNumber(String accountNumber, Consumer<Account> callback) {
        database.child("accounts").orderByChild("accountNumber").equalTo(accountNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
                                Account account = accountSnapshot.getValue(Account.class);
                                if (account != null) {
                                    account.setAccountId(accountSnapshot.getKey());
                                    callback.accept(account);
                                    return;
                                }
                            }
                        }
                        callback.accept(null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(null);
                    }
                });
    }

    // ==================== LOAN CRUD OPERATIONS ====================

    public void createLoan(Loan loan, Consumer<Boolean> callback) {
        Map<String, Object> loanValues = new HashMap<>();
        loanValues.put("userId", loan.getUserId());
        loanValues.put("accountId", loan.getAccountId());
        loanValues.put("requestedAmount", loan.getRequestedAmount());
        loanValues.put("approvedAmount", loan.getApprovedAmount());
        loanValues.put("remainingAmount", loan.getRemainingAmount());
        loanValues.put("status", loan.getStatus());
        loanValues.put("requestedAt", loan.getRequestedAt());
        loanValues.put("approvedAt", loan.getApprovedAt());
        loanValues.put("interestRate", loan.getInterestRate());
        loanValues.put("purpose", loan.getPurpose());

        database.child("loans").child(loan.getLoanId()).setValue(loanValues)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void getLoan(String loanId, Consumer<Loan> callback) {
        database.child("loans").child(loanId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Loan loan = snapshot.getValue(Loan.class);
                            if (loan != null) {
                                loan.setLoanId(snapshot.getKey());
                                callback.accept(loan);
                            } else {
                                callback.accept(null);
                            }
                        } else {
                            callback.accept(null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(null);
                    }
                }
        );
    }

    public void updateLoan(String loanId, Map<String, Object> updates, Consumer<Boolean> callback) {
        database.child("loans").child(loanId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void approveLoan(String loanId, double approvedAmount, double interestRate, Consumer<Boolean> callback) {
        double remainingAmount = approvedAmount * (1 + interestRate / 100);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "APPROVED");
        updates.put("approvedAmount", approvedAmount);
        updates.put("remainingAmount", remainingAmount);
        updates.put("interestRate", interestRate);
        updates.put("approvedAt", System.currentTimeMillis());

        updateLoan(loanId, updates, callback);
    }

    public void rejectLoan(String loanId, String reason, Consumer<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "REJECTED");
        updates.put("rejectedAt", System.currentTimeMillis());
        if (reason != null && !reason.isEmpty()) {
            updates.put("rejectionReason", reason);
        }

        updateLoan(loanId, updates, callback);
    }

    public void markLoanAsFullyPaid(String loanId, Consumer<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "FULLY_PAID");
        updates.put("remainingAmount", 0.0);

        updateLoan(loanId, updates, callback);
    }

    public void getLoansByUser(String userId, Consumer<Map<String, Loan>> callback) {
        database.child("loans").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Loan> loans = new HashMap<>();
                        for (DataSnapshot loanSnapshot : snapshot.getChildren()) {
                            Loan loan = loanSnapshot.getValue(Loan.class);
                            if (loan != null) {
                                loan.setLoanId(loanSnapshot.getKey());
                                loans.put(loanSnapshot.getKey(), loan);
                            }
                        }
                        callback.accept(loans);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(new HashMap<>());
                    }
                });
    }

    public void getAllLoans(Consumer<Map<String, Loan>> callback) {
        database.child("loans").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Loan> loans = new HashMap<>();
                        for (DataSnapshot loanSnapshot : snapshot.getChildren()) {
                            Loan loan = loanSnapshot.getValue(Loan.class);
                            if (loan != null) {
                                loan.setLoanId(loanSnapshot.getKey());
                                loans.put(loanSnapshot.getKey(), loan);
                            }
                        }
                        callback.accept(loans);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(new HashMap<>());
                    }
                }
        );
    }

    public void getPendingLoans(Consumer<Map<String, Loan>> callback) {
        database.child("loans").orderByChild("status").equalTo("PENDING")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Loan> loans = new HashMap<>();
                        for (DataSnapshot loanSnapshot : snapshot.getChildren()) {
                            Loan loan = loanSnapshot.getValue(Loan.class);
                            if (loan != null) {
                                loan.setLoanId(loanSnapshot.getKey());
                                loans.put(loanSnapshot.getKey(), loan);
                            }
                        }
                        callback.accept(loans);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(new HashMap<>());
                    }
                });
    }

    // ==================== LOAN REPAYMENT CRUD OPERATIONS ====================

    public void createLoanRepayment(LoanRepayment repayment, Consumer<Boolean> callback) {
        Map<String, Object> repaymentValues = new HashMap<>();
        repaymentValues.put("loanId", repayment.getLoanId());
        repaymentValues.put("amount", repayment.getAmount());
        repaymentValues.put("timestamp", repayment.getTimestamp());
        repaymentValues.put("remainingAfter", repayment.getRemainingAfter());

        database.child("loanRepayments").child(repayment.getRepaymentId()).setValue(repaymentValues)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void getRepaymentsByLoan(String loanId, Consumer<Map<String, LoanRepayment>> callback) {
        database.child("loanRepayments").orderByChild("loanId").equalTo(loanId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, LoanRepayment> repayments = new HashMap<>();
                        for (DataSnapshot repaymentSnapshot : snapshot.getChildren()) {
                            LoanRepayment repayment = repaymentSnapshot.getValue(LoanRepayment.class);
                            if (repayment != null) {
                                repayment.setRepaymentId(repaymentSnapshot.getKey());
                                repayments.put(repaymentSnapshot.getKey(), repayment);
                            }
                        }
                        callback.accept(repayments);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(new HashMap<>());
                    }
                });
    }

    // ==================== TRANSACTION CRUD OPERATIONS ====================

    public void createTransaction(com.izak.demobankingapp20260118.models.Transaction transaction, Consumer<Boolean> callback) {
        Map<String, Object> transactionValues = new HashMap<>();
        transactionValues.put("fromAccountId", transaction.getFromAccountId());
        transactionValues.put("toAccountId", transaction.getToAccountId());
        transactionValues.put("amount", transaction.getAmount());
        transactionValues.put("type", transaction.getType());
        transactionValues.put("timestamp", transaction.getTimestamp());
        transactionValues.put("description", transaction.getDescription());

        database.child("transactions").child(transaction.getTransactionId()).setValue(transactionValues)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

    public void getTransactionsByAccount(String accountId, Consumer<Map<String, com.izak.demobankingapp20260118.models.Transaction>> callback) {
        database.child("transactions").orderByChild("fromAccountId").equalTo(accountId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, com.izak.demobankingapp20260118.models.Transaction> transactions = new HashMap<>();
                        for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                            com.izak.demobankingapp20260118.models.Transaction transaction =
                                    transactionSnapshot.getValue(com.izak.demobankingapp20260118.models.Transaction.class);
                            if (transaction != null) {
                                transaction.setTransactionId(transactionSnapshot.getKey());
                                transactions.put(transactionSnapshot.getKey(), transaction);
                            }
                        }

                        // Also get transactions where this account is the recipient
                        database.child("transactions").orderByChild("toAccountId").equalTo(accountId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot2) {
                                        for (DataSnapshot transactionSnapshot : snapshot2.getChildren()) {
                                            com.izak.demobankingapp20260118.models.Transaction transaction =
                                                    transactionSnapshot.getValue(com.izak.demobankingapp20260118.models.Transaction.class);
                                            if (transaction != null) {
                                                transaction.setTransactionId(transactionSnapshot.getKey());
                                                transactions.put(transactionSnapshot.getKey(), transaction);
                                            }
                                        }
                                        callback.accept(transactions);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        callback.accept(transactions);
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(new HashMap<>());
                    }
                });
    }

    // ==================== REAL-TIME LISTENERS ====================

    public DatabaseReference getUsersListener() {
        return database.child("users");
    }

    public DatabaseReference getAccountsListener() {
        return database.child("accounts");
    }

    public DatabaseReference getLoansListener() {
        return database.child("loans");
    }

    public DatabaseReference getLoanListener(String loanId) {
        return database.child("loans").child(loanId);
    }

    public DatabaseReference getAccountListener(String accountId) {
        return database.child("accounts").child(accountId);
    }

    public DatabaseReference getUserListener(String userId) {
        return database.child("users").child(userId);
    }

    // ==================== UTILITY METHODS ====================

    public void removeListener(DatabaseReference ref, ValueEventListener listener) {
        if (ref != null && listener != null) {
            ref.removeEventListener(listener);
        }
    }

    public DatabaseReference generateId(String path) {
        return database.child(path).push();
    }

    public void updateMultiplePaths(Map<String, Object> updates, Consumer<Boolean> callback) {
        database.updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }
}