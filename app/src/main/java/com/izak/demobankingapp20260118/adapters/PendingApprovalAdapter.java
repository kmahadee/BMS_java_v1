package com.izak.demobankingapp20260118.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PendingApprovalAdapter extends RecyclerView.Adapter<PendingApprovalAdapter.ViewHolder> {

    private List<User> pendingUsers;
    private OnApprovalActionListener listener;

    public interface OnApprovalActionListener {
        void onApprove(User user);
        void onReject(User user);
    }

    public PendingApprovalAdapter(List<User> pendingUsers, OnApprovalActionListener listener) {
        this.pendingUsers = pendingUsers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = pendingUsers.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return pendingUsers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView emailText;
        TextView registrationDateText;
        Button approveButton;
        Button rejectButton;

        ViewHolder(View itemView) {
            super(itemView);
            emailText = itemView.findViewById(R.id.userEmail);
            registrationDateText = itemView.findViewById(R.id.registrationDate);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }

        void bind(User user) {
            emailText.setText(user.getEmail());

            // Format registration date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(user.getCreatedAt()));
            registrationDateText.setText("Registered: " + formattedDate);

            approveButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(user);
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(user);
                }
            });
        }
    }
}
