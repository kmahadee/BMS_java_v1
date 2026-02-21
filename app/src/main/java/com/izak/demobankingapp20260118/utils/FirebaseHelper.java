package com.izak.demobankingapp20260118.utils;

import com.izak.demobankingapp20260118.firebase.FirebaseCRUDManager;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.*;
import java.util.Map;

public class FirebaseHelper {

    public static void transferMoney(String fromAccountId, String toAccountId,
                                     String fromAccountNumber, String toAccountNumber,
                                     double amount, String description,
                                     OnTransferCompleteListener listener) {

        FirebaseCRUDManager crudManager = FirebaseManager.getInstance().getCrudManager();

        // Get both account balances first
        crudManager.getAccount(fromAccountId, fromAccount -> {
            if (fromAccount == null || fromAccount.getBalance() < amount) {
                listener.onFailure("Insufficient balance");
                return;
            }

            crudManager.getAccount(toAccountId, toAccount -> {
                if (toAccount == null) {
                    listener.onFailure("Recipient account not found");
                    return;
                }

                // Prepare updates
                Map<String, Object> updates = Map.of(
                        fromAccountId, fromAccount.getBalance() - amount,
                        toAccountId, toAccount.getBalance() + amount
                );

                // Create transaction record
                Transaction transaction = new Transaction(
                        java.util.UUID.randomUUID().toString(),
                        fromAccountId,
                        toAccountId,
                        amount,
                        "TRANSFER",
                        System.currentTimeMillis(),
                        description
                );

                // Execute updates
                crudManager.createTransaction(transaction, success -> {
                    if (success) {
                        listener.onSuccess(transaction);
                    } else {
                        listener.onFailure("Failed to record transaction");
                    }
                });
            });
        });
    }

    public static void createLoanRequest(String userId, String accountId,
                                         double amount, String purpose,
                                         OnLoanRequestCompleteListener listener) {

        FirebaseCRUDManager crudManager = FirebaseManager.getInstance().getCrudManager();

        // Check if user has pending loan
        crudManager.getLoansByUser(userId, loans -> {
            boolean hasPending = false;
            boolean hasUnpaid = false;

            for (Loan loan : loans.values()) {
                if ("PENDING".equals(loan.getStatus())) {
                    hasPending = true;
                }
                if ("APPROVED".equals(loan.getStatus()) && loan.getRemainingAmount() > 0) {
                    hasUnpaid = true;
                }
            }

            if (hasPending) {
                listener.onFailure("You already have a pending loan request");
                return;
            }

            if (hasUnpaid) {
                listener.onFailure("You have an unpaid loan");
                return;
            }

            // Create loan request
            Loan loan = new Loan(
                    java.util.UUID.randomUUID().toString(),
                    userId,
                    accountId,
                    amount,
                    0.0,
                    0.0,
                    "PENDING",
                    System.currentTimeMillis(),
                    0L,
                    0.0,
                    purpose
            );

            crudManager.createLoan(loan, success -> {
                if (success) {
                    listener.onSuccess(loan);
                } else {
                    listener.onFailure("Failed to submit loan request");
                }
            });
        });
    }

    public interface OnTransferCompleteListener {
        void onSuccess(Transaction transaction);
        void onFailure(String error);
    }

    public interface OnLoanRequestCompleteListener {
        void onSuccess(Loan loan);
        void onFailure(String error);
    }
}