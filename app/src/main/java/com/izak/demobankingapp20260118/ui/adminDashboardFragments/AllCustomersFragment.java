package com.izak.demobankingapp20260118.ui.adminDashboardFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.adapters.CustomerAdapter;
import com.izak.demobankingapp20260118.firebase.FirebaseCRUDManager;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;
import com.izak.demobankingapp20260118.models.CustomerInfo;
import com.izak.demobankingapp20260118.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllCustomersFragment extends Fragment {

    private RecyclerView recyclerView;
    private SearchView searchView;
    private View emptyView;
    private ProgressBar progressBar;
    private CustomerAdapter adapter;
    private List<CustomerInfo> allCustomers;
    private FirebaseManager firebaseManager;
    private FirebaseCRUDManager crudManager;
    private ValueEventListener accountsListener;
    private ValueEventListener usersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_customers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();
        crudManager = firebaseManager.getCrudManager();
        allCustomers = new ArrayList<>();

        initializeViews(view);
        setupRecyclerView();
        setupSearchView();

        // Use the optimized method with CRUD manager
        loadAllCustomersOptimized();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewCustomers);
        searchView = view.findViewById(R.id.searchView);
        emptyView = view.findViewById(R.id.emptyViewCustomers);
        progressBar = view.findViewById(R.id.progressBarCustomers);
    }

    private void setupRecyclerView() {
        adapter = new CustomerAdapter(allCustomers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            adapter.filter("");
            return false;
        });
    }

    // ==================== OPTIMIZED LOADING METHOD ====================

    private void loadAllCustomersOptimized() {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        showLoading(true);

        System.out.println("DEBUG AllCustomers: Starting to load customers...");

        System.out.println("DEBUG AllCustomers: Starting to load customers...");

        // Get all accounts first
        crudManager.getAllAccounts(accounts -> {
            // ADDED: Check if fragment is still attached
            if (!isAdded() || getContext() == null) {
                return;
            }

            System.out.println("DEBUG AllCustomers: Found " + accounts.size() + " accounts");

            if (accounts.isEmpty()) {
                showLoading(false);
                updateEmptyState();
                return;
            }

            // Get all users
            crudManager.getAllUsers(users -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }

                System.out.println("DEBUG AllCustomers: Found " + users.size() + " users");

                // Create a new list to collect customers
                List<CustomerInfo> newCustomers = new ArrayList<>();

                for (Map.Entry<String, Account> accountEntry : accounts.entrySet()) {
                    Account account = accountEntry.getValue();
                    account.setAccountId(accountEntry.getKey());

                    User user = users.get(account.getUserId());

                    System.out.println("DEBUG AllCustomers: Processing account " + account.getAccountId() +
                            " for user " + account.getUserId());

                    if (user != null) {
                        System.out.println("DEBUG AllCustomers: User found - Email: " + user.getEmail() +
                                ", Role: " + user.getRole() +
                                ", Approved: " + user.isApproved());

                        if ("CUSTOMER".equals(user.getRole())) {
                            CustomerInfo customerInfo = new CustomerInfo(
                                    account.getAccountId(),
                                    account.getUserId(),
                                    user.getEmail(),
                                    account.getAccountNumber(),
                                    account.getBalance(),
                                    user.isApproved(),
                                    account.getCreatedAt()
                            );
                            newCustomers.add(customerInfo); // ✅ Add to new list
                            System.out.println("DEBUG AllCustomers: Added customer - " + user.getEmail());
                        }
                    }
                }

                System.out.println("DEBUG AllCustomers: Total customers loaded: " + newCustomers.size());

                // ✅ Update the adapter's internal data
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setData(newCustomers); // ✅ This updates adapter's internal lists AND calls notifyDataSetChanged()
                        allCustomers.clear();
                        allCustomers.addAll(newCustomers); // Keep fragment's list in sync
                        showLoading(false);
                        updateEmptyState();
                    });
                }
            });
        });
    }

    // ==================== REAL-TIME UPDATES VERSION ====================

    private void loadAllCustomersRealTime() {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        showLoading(true);

        // Real-time listener for accounts
        accountsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // ADDED: Check if fragment is still attached
                if (!isAdded() || getContext() == null) {
                    return;
                }

                System.out.println("DEBUG AllCustomers: Accounts updated, reloading...");
                loadAllCustomersOptimized();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ADDED: Check if fragment is still attached
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showLoading(false);
                Toast.makeText(getContext(),
                        "Error loading accounts: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        firebaseManager.getAccountsRef().addValueEventListener(accountsListener);
    }

    // ==================== COMMON METHODS ====================

    private void showLoading(boolean show) {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (allCustomers.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        refreshCustomers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove listeners to prevent memory leaks
        if (accountsListener != null) {
            firebaseManager.getAccountsRef().removeEventListener(accountsListener);
        }
        if (usersListener != null) {
            firebaseManager.getUsersRef().removeEventListener(usersListener);
        }
    }

    // ==================== REFRESH METHOD ====================

    public void refreshCustomers() {
        // ADDED: Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        allCustomers.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        loadAllCustomersOptimized();
    }
}