package com.izak.demobankingapp20260118.models;

public class TransactionRecord {
    private int id;
    private String transactionId;
    private String accountNumber;
    private String type; // TRANSFER_IN, TRANSFER_OUT, DEPOSIT, WITHDRAWAL, LOAN_REPAYMENT, LOAN_DISBURSEMENT
    private double amount;
    private String description;
    private long timestamp;
    private String recipientAccount;
    private double balanceAfter; // NEW FIELD: Balance after this transaction

    public TransactionRecord() {
    }

    // Updated constructor with balanceAfter
    public TransactionRecord(String transactionId, String accountNumber, String type,
                             double amount, String description, long timestamp,
                             String recipientAccount, double balanceAfter) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.recipientAccount = recipientAccount;
        this.balanceAfter = balanceAfter;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRecipientAccount() {
        return recipientAccount;
    }

    public void setRecipientAccount(String recipientAccount) {
        this.recipientAccount = recipientAccount;
    }

    // NEW GETTER AND SETTER
    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                ", recipientAccount='" + recipientAccount + '\'' +
                ", balanceAfter=" + balanceAfter +
                '}';
    }
}