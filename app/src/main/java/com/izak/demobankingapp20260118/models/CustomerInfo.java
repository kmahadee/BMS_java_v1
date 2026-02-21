package com.izak.demobankingapp20260118.models;

public class CustomerInfo {
    private String accountId;
    private String userId;
    private String email;
    private String accountNumber;
    private double balance;
    private boolean isApproved;
    private long createdAt;

    public CustomerInfo() {
    }

    public CustomerInfo(String accountId, String userId, String email, String accountNumber,
                        double balance, boolean isApproved, long createdAt) {
        this.accountId = accountId;
        this.userId = userId;
        this.email = email;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.isApproved = isApproved;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CustomerInfo{" +
                "accountId='" + accountId + '\'' +
                ", userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", balance=" + balance +
                ", isApproved=" + isApproved +
                ", createdAt=" + createdAt +
                '}';
    }
}