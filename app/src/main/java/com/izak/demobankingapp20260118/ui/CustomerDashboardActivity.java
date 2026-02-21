package com.izak.demobankingapp20260118.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.izak.demobankingapp20260118.R;
import com.izak.demobankingapp20260118.firebase.FirebaseManager;
import com.izak.demobankingapp20260118.models.Account;
import com.izak.demobankingapp20260118.ui.customerDashboardFragments.AccountBalanceFragment;
import com.izak.demobankingapp20260118.ui.customerDashboardFragments.MyLoansFragment;
import com.izak.demobankingapp20260118.ui.customerDashboardFragments.RequestLoanFragment;
import com.izak.demobankingapp20260118.ui.customerDashboardFragments.TransactionStatementFragment;
import com.izak.demobankingapp20260118.ui.customerDashboardFragments.TransferMoneyFragment;

public class CustomerDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;

    private TextView navHeaderEmail;
    private TextView navHeaderAccountNumber;
    private View navHeaderView;

    private String currentUserId;
    private String accountId;
    private String accountNumber;
    private boolean isAccountApproved = false;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "CustomerPrefs";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_ACCOUNT_NUMBER = "account_number";
    private static final String KEY_IS_APPROVED = "is_approved";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        firebaseManager = FirebaseManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if user is authenticated
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        currentUserId = currentUser.getUid();

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupNavigationHeader();

        // Load cached account data
        loadCachedAccountData();

        // Fetch fresh account data from Firebase
        fetchAccountData();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("Account Balance");
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupNavigationHeader() {
        navHeaderView = navigationView.getHeaderView(0);
        navHeaderEmail = navHeaderView.findViewById(R.id.nav_header_email);
        navHeaderAccountNumber = navHeaderView.findViewById(R.id.nav_header_account_number);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navHeaderEmail.setText(currentUser.getEmail());
        }
    }

    private void loadCachedAccountData() {
        accountId = sharedPreferences.getString(KEY_ACCOUNT_ID, null);
        accountNumber = sharedPreferences.getString(KEY_ACCOUNT_NUMBER, "Loading...");
        isAccountApproved = sharedPreferences.getBoolean(KEY_IS_APPROVED, false);

        // Update UI with cached data
        if (navHeaderAccountNumber != null) {
            navHeaderAccountNumber.setText("A/C: " + accountNumber);
        }

        // Load default fragment if account is approved and no fragment is currently loaded
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (isAccountApproved && currentFragment == null) {
            loadFragment(new AccountBalanceFragment(), "Account Balance");
            navigationView.setCheckedItem(R.id.nav_account_balance);
        }
    }

//    private void loadCachedAccountData() {
//        accountId = sharedPreferences.getString(KEY_ACCOUNT_ID, null);
//        accountNumber = sharedPreferences.getString(KEY_ACCOUNT_NUMBER, "Loading...");
//        isAccountApproved = sharedPreferences.getBoolean(KEY_IS_APPROVED, false);
//
//        // Update UI with cached data
//        if (navHeaderAccountNumber != null) {
//            navHeaderAccountNumber.setText("A/C: " + accountNumber);
//        }
//
//        // Load default fragment if account is approved
//        if (isAccountApproved && savedInstanceState == null) {
//            loadFragment(new AccountBalanceFragment(), "Account Balance");
//            navigationView.setCheckedItem(R.id.nav_account_balance);
//        }
//    }

    private void fetchAccountData() {
        DatabaseReference accountsRef = firebaseManager.getAccountsRef();

        accountsRef.orderByChild("userId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot accountSnapshot : snapshot.getChildren()) {
                                Account account = accountSnapshot.getValue(Account.class);
                                if (account != null) {
                                    accountId = accountSnapshot.getKey();
                                    accountNumber = account.getAccountNumber();

                                    // Update UI
                                    if (navHeaderAccountNumber != null) {
                                        navHeaderAccountNumber.setText("A/C: " + accountNumber);
                                    }

                                    // Cache the data
                                    cacheAccountData(accountId, accountNumber, true);

                                    // Check user approval status
                                    checkUserApproval();

                                    return;
                                }
                            }
                        } else {
                            // No account found
                            handleNoAccount();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CustomerDashboardActivity.this,
                                "Error loading account: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        firebaseManager.getCrudManager().getAccountByUserId(
                currentUserId,
                account -> {
                    if (account != null) {
                        accountId = account.getAccountId();
                        accountNumber = account.getAccountNumber();

                        if (navHeaderAccountNumber != null) {
                            navHeaderAccountNumber.setText("A/C: " + accountNumber);
                        }

                        cacheAccountData(accountId, accountNumber, true);
                        checkUserApproval();
                    } else {
                        handleNoAccount();
                    }
                }
        );
    }

    private void checkUserApproval() {
        firebaseManager.isUserApproved(currentUserId, isApproved -> {
            if (isApproved) {
                isAccountApproved = true;
                cacheAccountData(accountId, accountNumber, true);

                // Load default fragment if not already loaded
                if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                    loadFragment(new AccountBalanceFragment(), "Account Balance");
                    navigationView.setCheckedItem(R.id.nav_account_balance);
                }
            } else {
                handleAccountNotApproved();
            }
        });
    }

    private void handleNoAccount() {
        Toast.makeText(this,
                "No account found. Please contact administrator.",
                Toast.LENGTH_LONG).show();
        performLogout();
    }

    private void handleAccountNotApproved() {
        isAccountApproved = false;
        cacheAccountData(null, "Pending Approval", false);

        Toast.makeText(this,
                "Your account is pending approval. Please contact administrator.",
                Toast.LENGTH_LONG).show();

        // Show account not approved message
        TextView notApprovedText = findViewById(R.id.notApprovedText);
        if (notApprovedText != null) {
            notApprovedText.setVisibility(View.VISIBLE);
        }

        // Disable navigation
        for (int i = 0; i < navigationView.getMenu().size(); i++) {
            navigationView.getMenu().getItem(i).setEnabled(false);
        }
    }

    private void cacheAccountData(String accountId, String accountNumber, boolean isApproved) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ACCOUNT_ID, accountId);
        editor.putString(KEY_ACCOUNT_NUMBER, accountNumber);
        editor.putBoolean(KEY_IS_APPROVED, isApproved);
        editor.apply();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (!isAccountApproved) {
            Toast.makeText(this, "Account pending approval", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }

        Fragment fragment = null;
        String title = "";

        if (id == R.id.nav_account_balance) {
            fragment = new AccountBalanceFragment();
            title = "Account Balance";
        } else if (id == R.id.nav_transfer_money) {
            fragment = new TransferMoneyFragment();
            title = "Transfer Money";
        } else if (id == R.id.nav_request_loan) {
            fragment = new RequestLoanFragment();
            title = "Request Loan";
        } else if (id == R.id.nav_my_loans) {
            fragment = new MyLoansFragment();
            title = "My Loans";
        } else if (id == R.id.nav_transaction_statement) {
            fragment = new TransactionStatementFragment(); // Use the new fragment
            title = "Transaction Statement";
        } else if (id == R.id.nav_loan_repayment) {
            // This can be merged with MyLoansFragment
            fragment = new MyLoansFragment();
            title = "Loan Repayment";
        } else if (id == R.id.nav_logout) {
            performLogout();
            return true;
        }

        if (fragment != null) {
            loadFragment(fragment, title);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


//    @Override
//    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//        int id = item.getItemId();
//
//        if (!isAccountApproved) {
//            Toast.makeText(this, "Account pending approval", Toast.LENGTH_SHORT).show();
//            drawerLayout.closeDrawer(GravityCompat.START);
//            return false;
//        }
//
//        Fragment fragment = null;
//        String title = "";
//
//        if (id == R.id.nav_account_balance) {
//            fragment = new AccountBalanceFragment();
//            title = "Account Balance";
//        } else if (id == R.id.nav_transfer_money) {
//            // Load TransferMoneyFragment (from existing code)
//            try {
//                Class<?> fragmentClass = Class.forName("com.izak.demobankingapp20260118.ui.customerDashboardFragments.TransferMoneyFragment");
//                fragment = (Fragment) fragmentClass.newInstance();
//                title = "Transfer Money";
//            } catch (Exception e) {
//                Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show();
//                drawerLayout.closeDrawer(GravityCompat.START);
//                return true;
//            }
//        } else if (id == R.id.nav_request_loan) {
//            // TODO: Create RequestLoanFragment
//            Toast.makeText(this, "Request Loan - Coming Soon", Toast.LENGTH_SHORT).show();
//            drawerLayout.closeDrawer(GravityCompat.START);
//            return true;
//        } else if (id == R.id.nav_my_loans) {
//            // TODO: Create MyLoansFragment
//            Toast.makeText(this, "My Loans - Coming Soon", Toast.LENGTH_SHORT).show();
//            drawerLayout.closeDrawer(GravityCompat.START);
//            return true;
//        } else if (id == R.id.nav_transaction_statement) {
//            // TODO: Create TransactionStatementFragment
//            Toast.makeText(this, "Transaction Statement - Coming Soon", Toast.LENGTH_SHORT).show();
//            drawerLayout.closeDrawer(GravityCompat.START);
//            return true;
//        } else if (id == R.id.nav_loan_repayment) {
//            // TODO: Create LoanRepaymentFragment
//            Toast.makeText(this, "Loan Repayment - Coming Soon", Toast.LENGTH_SHORT).show();
//            drawerLayout.closeDrawer(GravityCompat.START);
//            return true;
//        } else if (id == R.id.nav_logout) {
//            performLogout();
//            return true;
//        }
//
//        if (fragment != null) {
//            loadFragment(fragment, title);
//        }
//
//        drawerLayout.closeDrawer(GravityCompat.START);
//        return true;
//    }

    private void loadFragment(Fragment fragment, String title) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void performLogout() {
        // Clear cached data
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Sign out from Firebase
        firebaseManager.signOut();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(CustomerDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Getter methods for fragments to access account data
    public String getCurrentUserId() {
        return currentUserId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public boolean isAccountApproved() {
        return isAccountApproved;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawerLayout != null) {
            drawerLayout.removeAllViews();
        }
    }
}