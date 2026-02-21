package com.izak.demobankingapp20260118.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.models.Loan;
import com.izak.demobankingapp20260118.models.LoanRepayment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoanRepaymentAdapter extends RecyclerView.Adapter<LoanRepaymentAdapter.ViewHolder> {

    private List<Loan> loanList;
    private Map<String, List<LoanRepayment>> repaymentsMap;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    public LoanRepaymentAdapter(List<Loan> loanList, Map<String, List<LoanRepayment>> repaymentsMap) {
        this.loanList = loanList;
        this.repaymentsMap = repaymentsMap;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loan_repayment_statement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Loan loan = loanList.get(position);
        List<LoanRepayment> repayments = repaymentsMap.get(loan.getLoanId());
        holder.bind(loan, repayments);
    }

    @Override
    public int getItemCount() {
        return loanList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView loanIdText;
        TextView loanAmountText;
        TextView loanStatusText;
        TextView remainingAmountText;
        TextView requestDateText;
        TextView repaymentCountText;
        ImageView expandIcon;
        LinearLayout repaymentsContainer;
        View headerContainer;

        ViewHolder(View itemView) {
            super(itemView);
            loanIdText = itemView.findViewById(R.id.loanIdText);
            loanAmountText = itemView.findViewById(R.id.loanAmountText);
            loanStatusText = itemView.findViewById(R.id.loanStatusText);
            remainingAmountText = itemView.findViewById(R.id.remainingAmountText);
            requestDateText = itemView.findViewById(R.id.requestDateText);
            repaymentCountText = itemView.findViewById(R.id.repaymentCountText);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            repaymentsContainer = itemView.findViewById(R.id.repaymentsContainer);
            headerContainer = itemView.findViewById(R.id.headerContainer);
        }

        void bind(Loan loan, List<LoanRepayment> repayments) {
            // Set loan header information
            loanIdText.setText("Loan ID: " + loan.getLoanId().substring(0, 8).toUpperCase());
            loanAmountText.setText("Amount: " + currencyFormat.format(loan.getApprovedAmount()));

            // Set status with color
            String status = loan.getStatus();
            loanStatusText.setText(status.replace("_", " "));

            int statusColor;
            switch (status) {
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
            loanStatusText.setTextColor(itemView.getContext().getResources().getColor(statusColor));

            // Set remaining amount
            double remaining = loan.getRemainingAmount();
            remainingAmountText.setText("Remaining: " + currencyFormat.format(remaining));

            if (remaining > 0) {
                remainingAmountText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            } else {
                remainingAmountText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            }

            // Set request date
            requestDateText.setText("Requested: " + dateFormat.format(new Date(loan.getRequestedAt())));

            // Set repayment count
            int repaymentCount = (repayments != null) ? repayments.size() : 0;
            repaymentCountText.setText(repaymentCount + " repayment(s)");

            // Clear previous repayments
            repaymentsContainer.removeAllViews();

            // Add repayments if expanded
            if (repayments != null && !repayments.isEmpty()) {
                for (LoanRepayment repayment : repayments) {
                    addRepaymentView(repayment);
                }
            }

            // Set expand/collapse functionality
            headerContainer.setOnClickListener(v -> toggleExpand());

            // Initially hide repayments
            repaymentsContainer.setVisibility(View.GONE);
            expandIcon.setRotation(0);
        }

        private void addRepaymentView(LoanRepayment repayment) {
            View repaymentView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_repayment_detail, repaymentsContainer, false);

            TextView repaymentAmountText = repaymentView.findViewById(R.id.repaymentAmountText);
            TextView repaymentDateText = repaymentView.findViewById(R.id.repaymentDateText);
            TextView remainingAfterText = repaymentView.findViewById(R.id.remainingAfterText);

            repaymentAmountText.setText("Amount: " + currencyFormat.format(repayment.getAmount()));
            repaymentDateText.setText("Date: " + dateFormat.format(new Date(repayment.getTimestamp())));

            if (repayment.getRemainingAfter() >= 0) {
                remainingAfterText.setText("Balance: " + currencyFormat.format(repayment.getRemainingAfter()));
            } else {
                remainingAfterText.setText("Fully Paid");
                remainingAfterText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            }

            repaymentsContainer.addView(repaymentView);

            // Add separator between repayments (except for last one)
            if (repaymentsContainer.indexOfChild(repaymentView) < repaymentsContainer.getChildCount() - 1) {
                View separator = new View(itemView.getContext());
                separator.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                ));
                separator.setBackgroundColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                repaymentsContainer.addView(separator);
            }
        }

        private void toggleExpand() {
            if (repaymentsContainer.getVisibility() == View.VISIBLE) {
                repaymentsContainer.setVisibility(View.GONE);
                expandIcon.setRotation(0);
            } else {
                repaymentsContainer.setVisibility(View.VISIBLE);
                expandIcon.setRotation(180);
            }
        }
    }

    public void updateData(List<Loan> newLoanList, Map<String, List<LoanRepayment>> newRepaymentsMap) {
        this.loanList = newLoanList;
        this.repaymentsMap = newRepaymentsMap;
        notifyDataSetChanged();
    }
}