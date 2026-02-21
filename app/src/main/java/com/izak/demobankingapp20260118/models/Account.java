package com.izak.demobankingapp20260118.models;

public class Account {
    private String accountId;
    private String userId;
    private double balance;
    private String accountNumber;
    private long createdAt;

    public Account() {
    }

    public Account(String accountId, String userId, double balance, String accountNumber, long createdAt) {
        this.accountId = accountId;
        this.userId = userId;
        this.balance = balance;
        this.accountNumber = accountNumber;
        this.createdAt = createdAt;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", userId='" + userId + '\'' +
                ", balance=" + balance +
                ", accountNumber='" + accountNumber + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
