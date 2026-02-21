package com.izak.demobankingapp20260118.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.models.CustomerInfo;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {

    // 👉 Adapter owns its own lists
    private final List<CustomerInfo> customers = new ArrayList<>();
    private final List<CustomerInfo> customersFull = new ArrayList<>();

    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(CustomerInfo customer);
    }

    public CustomerAdapter(List<CustomerInfo> initialData) {
        setData(initialData);
    }

    public CustomerAdapter(List<CustomerInfo> initialData, OnCustomerClickListener listener) {
        this.listener = listener;
        setData(initialData);
    }

    // ===================== IMPORTANT FIX =====================

    public void setData(List<CustomerInfo> newCustomers) {
        customers.clear();
        customersFull.clear();

        if (newCustomers != null) {
            customers.addAll(newCustomers);
            customersFull.addAll(newCustomers);
        }

        notifyDataSetChanged();
    }

    // ===================== FILTER =====================

    public void filter(String query) {
        customers.clear();

        if (query == null || query.trim().isEmpty()) {
            customers.addAll(customersFull);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault()).trim();

            for (CustomerInfo customer : customersFull) {
                if (customer.getEmail().toLowerCase().contains(lowerCaseQuery) ||
                        customer.getAccountNumber().contains(lowerCaseQuery)) {
                    customers.add(customer);
                }
            }
        }

        notifyDataSetChanged();
    }

    // ===================== ADAPTER =====================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(customers.get(position));
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    // ===================== VIEW HOLDER =====================

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView emailText;
        TextView accountNumberText;
        TextView balanceText;
        TextView statusText;
        TextView createdDateText;

        ViewHolder(View itemView) {
            super(itemView);

            emailText = itemView.findViewById(R.id.customerEmailText);
            accountNumberText = itemView.findViewById(R.id.accountNumberText);
            balanceText = itemView.findViewById(R.id.balanceText);
            statusText = itemView.findViewById(R.id.statusText);
            createdDateText = itemView.findViewById(R.id.createdDateText);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCustomerClick(customers.get(pos));
                }
            });
        }

        void bind(CustomerInfo customer) {
            emailText.setText(customer.getEmail());
            accountNumberText.setText("A/C: " + customer.getAccountNumber());

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            balanceText.setText(currencyFormat.format(customer.getBalance()));

            balanceText.setTextColor(itemView.getContext().getResources()
                    .getColor(customer.getBalance() >= 0
                            ? android.R.color.holo_green_dark
                            : android.R.color.holo_red_dark));

            statusText.setText(customer.isApproved() ? "Active" : "Pending");
            statusText.setTextColor(itemView.getContext().getResources()
                    .getColor(customer.isApproved()
                            ? android.R.color.holo_green_dark
                            : android.R.color.holo_orange_dark));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            createdDateText.setText("Since: " + sdf.format(new Date(customer.getCreatedAt())));
        }
    }
}



//package com.izak.demobankingapp20260118.adapters;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import com.izak.demobankingapp20260118.R;
//import com.izak.demobankingapp20260118.models.CustomerInfo;
//
//import java.text.NumberFormat;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {
//
//    private List<CustomerInfo> allCustomers;
//    private List<CustomerInfo> filteredCustomers;
//    private OnCustomerClickListener listener;
//
//    public interface OnCustomerClickListener {
//        void onCustomerClick(CustomerInfo customer);
//    }
//
//    public CustomerAdapter(List<CustomerInfo> customers) {
//        this.allCustomers = customers;
//        this.filteredCustomers = new ArrayList<>(customers);
//    }
//
//    public CustomerAdapter(List<CustomerInfo> customers, OnCustomerClickListener listener) {
//        this.allCustomers = customers;
//        this.filteredCustomers = new ArrayList<>(customers);
//        this.listener = listener;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_customer, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        CustomerInfo customer = filteredCustomers.get(position);
//        holder.bind(customer);
//    }
//
//    @Override
//    public int getItemCount() {
//        return filteredCustomers.size();
//    }
//
//    public void filter(String query) {
//        filteredCustomers.clear();
//
//        if (query == null || query.trim().isEmpty()) {
//            filteredCustomers.addAll(allCustomers);
//        } else {
//            String lowerCaseQuery = query.toLowerCase().trim();
//
//            for (CustomerInfo customer : allCustomers) {
//                // Search in email, account number
//                if (customer.getEmail().toLowerCase().contains(lowerCaseQuery) ||
//                        customer.getAccountNumber().contains(lowerCaseQuery)) {
//                    filteredCustomers.add(customer);
//                }
//            }
//        }
//
//        notifyDataSetChanged();
//    }
//
//    class ViewHolder extends RecyclerView.ViewHolder {
//        TextView emailText;
//        TextView accountNumberText;
//        TextView balanceText;
//        TextView statusText;
//        TextView createdDateText;
//
//        ViewHolder(View itemView) {
//            super(itemView);
//            emailText = itemView.findViewById(R.id.customerEmailText);
//            accountNumberText = itemView.findViewById(R.id.accountNumberText);
//            balanceText = itemView.findViewById(R.id.balanceText);
//            statusText = itemView.findViewById(R.id.statusText);
//            createdDateText = itemView.findViewById(R.id.createdDateText);
//
//            // Set click listener for the entire item
//            itemView.setOnClickListener(v -> {
//                int position = getAdapterPosition();
//                if (position != RecyclerView.NO_POSITION && listener != null) {
//                    listener.onCustomerClick(filteredCustomers.get(position));
//                }
//            });
//        }
//
//        void bind(CustomerInfo customer) {
//            emailText.setText(customer.getEmail());
//            accountNumberText.setText("A/C: " + customer.getAccountNumber());
//
//            // Format balance with currency symbol
//            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
//            balanceText.setText(currencyFormat.format(customer.getBalance()));
//
//            // Set balance color based on value
//            if (customer.getBalance() >= 0) {
//                balanceText.setTextColor(itemView.getContext().getResources()
//                        .getColor(android.R.color.holo_green_dark));
//            } else {
//                balanceText.setTextColor(itemView.getContext().getResources()
//                        .getColor(android.R.color.holo_red_dark));
//            }
//
//            // Status
//            statusText.setText(customer.isApproved() ? "Active" : "Pending");
//            statusText.setTextColor(itemView.getContext().getResources()
//                    .getColor(customer.isApproved() ?
//                            android.R.color.holo_green_dark :
//                            android.R.color.holo_orange_dark));
//
//            // Format created date
//            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
//            String formattedDate = sdf.format(new Date(customer.getCreatedAt()));
//            createdDateText.setText("Since: " + formattedDate);
//        }
//    }
//}