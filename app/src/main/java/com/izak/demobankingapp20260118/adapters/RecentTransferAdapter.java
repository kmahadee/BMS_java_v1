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

public class RecentTransferAdapter extends RecyclerView.Adapter<RecentTransferAdapter.ViewHolder> {

    private List<TransactionRecord> transfers;

    public RecentTransferAdapter(List<TransactionRecord> transfers) {
        this.transfers = transfers;
    }

    public void updateTransfers(List<TransactionRecord> newTransfers) {
        this.transfers = newTransfers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_transfer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionRecord transfer = transfers.get(position);
        holder.bind(transfer);
    }

    @Override
    public int getItemCount() {
        return transfers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView accountText;
        TextView descriptionText;
        TextView amountText;
        TextView dateText;
        View statusIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.transferIcon);
            accountText = itemView.findViewById(R.id.transferAccountText);
            descriptionText = itemView.findViewById(R.id.transferDescriptionText);
            amountText = itemView.findViewById(R.id.transferAmountText);
            dateText = itemView.findViewById(R.id.transferDateText);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }

        void bind(TransactionRecord transfer) {
            boolean isOutgoing = "TRANSFER_OUT".equals(transfer.getType());

            // Set icon and color based on transaction type
            if (isOutgoing) {
                iconImageView.setImageResource(android.R.drawable.ic_menu_upload);
                iconImageView.setColorFilter(itemView.getContext().getResources()
                        .getColor(android.R.color.holo_red_light));
                statusIndicator.setBackgroundColor(itemView.getContext().getResources()
                        .getColor(android.R.color.holo_red_light));
            } else {
                iconImageView.setImageResource(android.R.drawable.ic_menu_revert);
                iconImageView.setColorFilter(itemView.getContext().getResources()
                        .getColor(android.R.color.holo_green_dark));
                statusIndicator.setBackgroundColor(itemView.getContext().getResources()
                        .getColor(android.R.color.holo_green_dark));
            }

            // Display account number
            String accountLabel = isOutgoing ? "To: " : "From: ";
            String account = transfer.getRecipientAccount() != null ?
                    transfer.getRecipientAccount() : "Unknown";
            accountText.setText(accountLabel + account);

            // Display description
            String description = transfer.getDescription();
            if (description == null || description.isEmpty()) {
                description = isOutgoing ? "Money Sent" : "Money Received";
            }
            descriptionText.setText(description);

            // Format and display amount
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            String formattedAmount = (isOutgoing ? "- " : "+ ") +
                    currencyFormat.format(transfer.getAmount());
            amountText.setText(formattedAmount);

            // Set amount color
            int amountColor = isOutgoing ?
                    android.R.color.holo_red_dark :
                    android.R.color.holo_green_dark;
            amountText.setTextColor(itemView.getContext().getResources().getColor(amountColor));

            // Format and display date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(transfer.getTimestamp()));
            dateText.setText(formattedDate);
        }
    }
}