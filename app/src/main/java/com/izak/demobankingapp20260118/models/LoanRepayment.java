package com.izak.demobankingapp20260118.models;

public class LoanRepayment {
    private String repaymentId;
    private String loanId;
    private double amount;
    private long timestamp;
    private double remainingAfter; // NEW FIELD: Remaining balance after this repayment

    public LoanRepayment() {
    }

    // Updated constructor with remainingAfter
    public LoanRepayment(String repaymentId, String loanId, double amount,
                         long timestamp, double remainingAfter) {
        this.repaymentId = repaymentId;
        this.loanId = loanId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.remainingAfter = remainingAfter;
    }

    public String getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(String repaymentId) {
        this.repaymentId = repaymentId;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // NEW GETTER AND SETTER
    public double getRemainingAfter() {
        return remainingAfter;
    }

    public void setRemainingAfter(double remainingAfter) {
        this.remainingAfter = remainingAfter;
    }

    @Override
    public String toString() {
        return "LoanRepayment{" +
                "repaymentId='" + repaymentId + '\'' +
                ", loanId='" + loanId + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", remainingAfter=" + remainingAfter +
                '}';
    }
}

//package com.izak.demobankingapp20260118.models;
//
//public class LoanRepayment {
//    private String repaymentId;
//    private String loanId;
//    private double amount;
//    private long timestamp;
//
//    public LoanRepayment() {
//    }
//
//    public LoanRepayment(String repaymentId, String loanId, double amount, long timestamp) {
//        this.repaymentId = repaymentId;
//        this.loanId = loanId;
//        this.amount = amount;
//        this.timestamp = timestamp;
//    }
//
//    public String getRepaymentId() {
//        return repaymentId;
//    }
//
//    public void setRepaymentId(String repaymentId) {
//        this.repaymentId = repaymentId;
//    }
//
//    public String getLoanId() {
//        return loanId;
//    }
//
//    public void setLoanId(String loanId) {
//        this.loanId = loanId;
//    }
//
//    public double getAmount() {
//        return amount;
//    }
//
//    public void setAmount(double amount) {
//        this.amount = amount;
//    }
//
//    public long getTimestamp() {
//        return timestamp;
//    }
//
//    public void setTimestamp(long timestamp) {
//        this.timestamp = timestamp;
//    }
//
//    @Override
//    public String toString() {
//        return "LoanRepayment{" +
//                "repaymentId='" + repaymentId + '\'' +
//                ", loanId='" + loanId + '\'' +
//                ", amount=" + amount +
//                ", timestamp=" + timestamp +
//                '}';
//    }
//}