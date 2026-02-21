package com.izak.demobankingapp20260118.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.models.TransactionRecord;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionRecord> transactions;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    public TransactionAdapter(List<TransactionRecord> transactions) {
        this.transactions = transactions;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    }

    public void updateTransactions(List<TransactionRecord> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionRecord transaction = transactions.get(position);
        holder.bind(transaction, position);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView descriptionText;
        TextView dateText;
        TextView amountText;
        TextView balanceText;
        TextView typeText;
        TextView recipientText;

        ViewHolder(View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            dateText = itemView.findViewById(R.id.dateText);
            amountText = itemView.findViewById(R.id.amountText);
            balanceText = itemView.findViewById(R.id.balanceText);
            typeText = itemView.findViewById(R.id.typeText);
            recipientText = itemView.findViewById(R.id.recipientText);
        }

        void bind(TransactionRecord transaction, int position) {
            String type = transaction.getType();
            boolean isCredit = isCreditTransaction(type);

            // Set icon based on transaction type
            setTransactionIcon(type, isCredit);

            // Set description
            descriptionText.setText(transaction.getDescription());

            // Set date
            dateText.setText(dateFormat.format(new Date(transaction.getTimestamp())));

            // Set amount with sign and color
            String amountPrefix = isCredit ? "+ " : "- ";
            amountText.setText(amountPrefix + currencyFormat.format(Math.abs(transaction.getAmount())));

            int amountColor = isCredit ?
                    itemView.getContext().getResources().getColor(android.R.color.holo_green_dark) :
                    itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
            amountText.setTextColor(amountColor);

            // Set balance after transaction
            double balanceAfter = transaction.getBalanceAfter();
            if (transaction.getBalanceAfter() != 0) {
                balanceText.setText("Balance: " + currencyFormat.format(transaction.getBalanceAfter()));
                balanceText.setVisibility(View.VISIBLE);
            } else {
                balanceText.setVisibility(View.GONE);
            }

            // Set type with formatted name
            String formattedType = formatTransactionType(type);
            typeText.setText(formattedType);

            // Set type text color
            int typeColor = isCredit ?
                    itemView.getContext().getResources().getColor(R.color.credit_type) :
                    itemView.getContext().getResources().getColor(R.color.debit_type);
            typeText.setTextColor(typeColor);

            // Set recipient if available
            if (transaction.getRecipientAccount() != null && !transaction.getRecipientAccount().isEmpty()) {
                String recipient = formatRecipient(transaction.getRecipientAccount(), type);
                recipientText.setText(recipient);
                recipientText.setVisibility(View.VISIBLE);
            } else {
                recipientText.setVisibility(View.GONE);
            }

            // Add bottom margin for last item
            if (position == transactions.size() - 1) {
                itemView.setPadding(0, 0, 0, 32);
            } else {
                itemView.setPadding(0, 0, 0, 0);
            }
        }

        private boolean isCreditTransaction(String type) {
            if (type == null) return false;

            switch (type) {
                case "TRANSFER_IN":
                case "DEPOSIT":
                case "LOAN_DISBURSEMENT":
                    return true;
                case "TRANSFER_OUT":
                case "WITHDRAWAL":
                case "LOAN_REPAYMENT":
                    return false;
                default:
                    return false;
            }
        }

        private void setTransactionIcon(String type, boolean isCredit) {
            if (type == null) return;

            int iconRes;
            int iconColor;

            if (type.contains("TRANSFER")) {
                iconRes = isCredit ?
                        R.drawable.ic_transfer_in :
                        R.drawable.ic_transfer_out;
                iconColor = isCredit ?
                        android.R.color.holo_green_dark :
                        android.R.color.holo_red_dark;
            } else if (type.contains("LOAN")) {
                iconRes = isCredit ?
                        R.drawable.ic_loan_disbursed :
                        R.drawable.ic_loan_repayment;
                iconColor = isCredit ?
                        android.R.color.holo_green_dark :
                        android.R.color.holo_red_dark;
            } else if (type.contains("DEPOSIT")) {
                iconRes = R.drawable.ic_deposit;
                iconColor = android.R.color.holo_green_dark;
            } else if (type.contains("WITHDRAWAL")) {
                iconRes = R.drawable.ic_withdrawal;
                iconColor = android.R.color.holo_red_dark;
            } else {
                iconRes = R.drawable.ic_transaction_default;
                iconColor = android.R.color.darker_gray;
            }

            iconImageView.setImageResource(iconRes);
            iconImageView.setColorFilter(itemView.getContext().getResources().getColor(iconColor));
        }

        private String formatTransactionType(String type) {
            if (type == null) return "Unknown";

            switch (type) {
                case "TRANSFER_IN":
                    return "Money Received";
                case "TRANSFER_OUT":
                    return "Money Sent";
                case "DEPOSIT":
                    return "Deposit";
                case "WITHDRAWAL":
                    return "Withdrawal";
                case "LOAN_DISBURSEMENT":
                    return "Loan Received";
                case "LOAN_REPAYMENT":
                    return "Loan Repayment";
                default:
                    return type.replace("_", " ");
            }
        }

        private String formatRecipient(String recipientAccount, String type) {
            if (type == null) return "Account: " + recipientAccount;

            switch (type) {
                case "TRANSFER_IN":
                    return "From: " + recipientAccount;
                case "TRANSFER_OUT":
                    return "To: " + recipientAccount;
                case "LOAN_REPAYMENT":
                    return "Bank";
                default:
                    return recipientAccount;
            }
        }
    }
}