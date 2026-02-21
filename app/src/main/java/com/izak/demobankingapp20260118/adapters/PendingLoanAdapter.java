package com.izak.demobankingapp20260118.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class PendingLoanAdapter extends RecyclerView.Adapter<PendingLoanAdapter.ViewHolder> {

    private List<Loan> pendingLoans;
    private OnLoanActionListener listener;

    public interface OnLoanActionListener {
        void onApprove(Loan loan);
        void onReject(Loan loan);
    }

    public PendingLoanAdapter(List<Loan> pendingLoans, OnLoanActionListener listener) {
        this.pendingLoans = pendingLoans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_loan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Loan loan = pendingLoans.get(position);
        holder.bind(loan);
    }

    @Override
    public int getItemCount() {
        return pendingLoans.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerEmailText;
        TextView requestedAmountText;
        TextView requestDateText;
        Button approveButton;
        Button rejectButton;

        ViewHolder(View itemView) {
            super(itemView);
            customerEmailText = itemView.findViewById(R.id.customerEmail);
            requestedAmountText = itemView.findViewById(R.id.requestedAmount);
            requestDateText = itemView.findViewById(R.id.requestDate);
            approveButton = itemView.findViewById(R.id.approveLoanButton);
            rejectButton = itemView.findViewById(R.id.rejectLoanButton);
        }

        void bind(Loan loan) {
            // Email will be set by the fragment after fetching
            customerEmailText.setText("Loading...");

            // Format requested amount
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            requestedAmountText.setText("Requested: " + currencyFormat.format(loan.getRequestedAmount()));

            // Format request date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(loan.getRequestedAt()));
            requestDateText.setText("Requested on: " + formattedDate);

            approveButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(loan);
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(loan);
                }
            });
        }

        public void setCustomerEmail(String email) {
            customerEmailText.setText(email);
        }
    }

    public void updateCustomerEmail(int position, String email) {
        if (position >= 0 && position < pendingLoans.size()) {
            notifyItemChanged(position, email);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.get(0) instanceof String) {
            holder.setCustomerEmail((String) payloads.get(0));
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }
}