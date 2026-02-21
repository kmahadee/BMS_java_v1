package com.izak.demobankingapp20260118.models;

public class Loan {
    private String loanId;
    private String userId;
    private String accountId;
    private double requestedAmount;
    private double approvedAmount;
    private double remainingAmount;
    private String status;
    private long requestedAt;
    private long approvedAt;
    private double interestRate;


    private String purpose;

    public Loan() {
    }

    public Loan(String loanId, String userId, String accountId, double requestedAmount,
                double approvedAmount, double remainingAmount, String status,
                long requestedAt, long approvedAt, double interestRate, String purpose) {
        this.loanId = loanId;
        this.userId = userId;
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.remainingAmount = remainingAmount;
        this.status = status;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.interestRate = interestRate;
        this.purpose = purpose;

    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(double requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public double getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(double approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }

    public long getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(long approvedAt) {
        this.approvedAt = approvedAt;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }


    @Override
    public String toString() {
        return "Loan{" +
                "loanId='" + loanId + '\'' +
                ", userId='" + userId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", requestedAmount=" + requestedAmount +
                ", approvedAmount=" + approvedAmount +
                ", remainingAmount=" + remainingAmount +
                ", status='" + status + '\'' +
                ", requestedAt=" + requestedAt +
                ", approvedAt=" + approvedAt +
                ", interestRate=" + interestRate +
                ", purpose='" + purpose + '\'' +
                '}';
    }
}
