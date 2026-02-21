package com.izak.demobankingapp20260118.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.models.Loan;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyLoanAdapter extends RecyclerView.Adapter<MyLoanAdapter.ViewHolder> {

    private List<Loan> loanList;
    private OnLoanRepaymentListener listener;

    public interface OnLoanRepaymentListener {
        void onRepayClicked(Loan loan);
    }

    public MyLoanAdapter(List<Loan> loanList, OnLoanRepaymentListener listener) {
        this.loanList = loanList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Loan loan = loanList.get(position);
        holder.bind(loan);
    }

    @Override
    public int getItemCount() {
        return loanList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView loanIdText;
        TextView amountText;
        TextView statusText;
        TextView remainingAmountText;
        TextView requestDateText;
        TextView purposeText;
        TextView interestRateText;
        ProgressBar repaymentProgress;
        Button repayButton;
        View statusIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            loanIdText = itemView.findViewById(R.id.loanIdText);
            amountText = itemView.findViewById(R.id.amountText);
            statusText = itemView.findViewById(R.id.statusText);
            remainingAmountText = itemView.findViewById(R.id.remainingAmountText);
            requestDateText = itemView.findViewById(R.id.requestDateText);
            purposeText = itemView.findViewById(R.id.purposeText);
            interestRateText = itemView.findViewById(R.id.interestRateText);
            repaymentProgress = itemView.findViewById(R.id.repaymentProgress);
            repayButton = itemView.findViewById(R.id.repayButton);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }

        void bind(Loan loan) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            // Loan ID
            loanIdText.setText("ID: " + loan.getLoanId().substring(0, 8).toUpperCase());

            // Amount
            if ("PENDING".equals(loan.getStatus())) {
                amountText.setText("Requested: " + currencyFormat.format(loan.getRequestedAmount()));
            } else if ("APPROVED".equals(loan.getStatus()) || "FULLY_PAID".equals(loan.getStatus())) {
                amountText.setText("Approved: " + currencyFormat.format(loan.getApprovedAmount()));
            } else {
                amountText.setText("Requested: " + currencyFormat.format(loan.getRequestedAmount()));
            }

            // Status with color coding
            String status = loan.getStatus();
            statusText.setText(status.replace("_", " "));

            int statusColor;
            switch (status) {
                case "PENDING":
                    statusColor = android.R.color.holo_orange_dark;
                    break;
                case "APPROVED":
                    statusColor = android.R.color.holo_green_dark;
                    break;
                case "REJECTED":
                    statusColor = android.R.color.holo_red_dark;
                    break;
                case "FULLY_PAID":
                    statusColor = android.R.color.holo_blue_dark;
                    break;
                default:
                    statusColor = android.R.color.darker_gray;
            }

            statusText.setTextColor(itemView.getContext().getResources().getColor(statusColor));
            statusIndicator.setBackgroundColor(itemView.getContext().getResources().getColor(statusColor));

            // Remaining amount (only for approved loans)
            if ("APPROVED".equals(status)) {
                remainingAmountText.setText("Remaining: " + currencyFormat.format(loan.getRemainingAmount()));
                remainingAmountText.setVisibility(View.VISIBLE);
            } else {
                remainingAmountText.setVisibility(View.GONE);
            }

            // Request date
            requestDateText.setText("Requested: " + sdf.format(new Date(loan.getRequestedAt())));

            // Purpose
            if (loan.getPurpose() != null && !loan.getPurpose().isEmpty()) {
                purposeText.setText(loan.getPurpose());
                purposeText.setVisibility(View.VISIBLE);
            } else {
                purposeText.setVisibility(View.GONE);
            }

            // Interest rate (only for approved/fully paid loans)
            if (("APPROVED".equals(status) || "FULLY_PAID".equals(status)) && loan.getInterestRate() > 0) {
                interestRateText.setText("Interest: " + String.format("%.1f", loan.getInterestRate()) + "%");
                interestRateText.setVisibility(View.VISIBLE);
            } else {
                interestRateText.setVisibility(View.GONE);
            }

            // Repayment progress (only for approved loans)
            if ("APPROVED".equals(status) && loan.getApprovedAmount() > 0) {
                double paidAmount = loan.getApprovedAmount() - loan.getRemainingAmount();
                int progress = (int) ((paidAmount / loan.getApprovedAmount()) * 100);

                repaymentProgress.setProgress(progress);
                repaymentProgress.setVisibility(View.VISIBLE);

                // Show progress text
//                repaymentProgress.setIndicatorColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                repaymentProgress.getProgressDrawable().setColorFilter(
                        itemView.getContext().getResources().getColor(android.R.color.holo_green_dark),
                        android.graphics.PorterDuff.Mode.SRC_IN
                );

            } else {
                repaymentProgress.setVisibility(View.GONE);
            }

            // Repay button (only for approved loans with remaining amount)
            if ("APPROVED".equals(status) && loan.getRemainingAmount() > 0) {
                repayButton.setVisibility(View.VISIBLE);
                repayButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRepayClicked(loan);
                    }
                });
            } else {
                repayButton.setVisibility(View.GONE);
            }
        }
    }

    public void updateLoans(List<Loan> newLoans) {
        loanList.clear();
        loanList.addAll(newLoans);
        notifyDataSetChanged();
    }
}