package com.izak.demobankingapp20260118.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.izak.demobankingapp20260118.models.Transaction;
import com.izak.demobankingapp20260118.models.TransactionRecord;
import com.izak.demobankingapp20260118.models.LoanRepayment;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "BankingAppDB";
    private static final int DATABASE_VERSION = 3; // Incremented version for merged schema

    // Table Names
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String TABLE_LOAN_REPAYMENTS = "loan_repayments";

    // Merged Transactions Table Columns
    private static final String KEY_ID = "id"; // Auto-increment ID
    private static final String KEY_TRANSACTION_ID = "transaction_id";
    private static final String KEY_FROM_ACCOUNT_ID = "from_account_id"; // Also used as account_number
    private static final String KEY_TO_ACCOUNT_ID = "to_account_id";
    private static final String KEY_RECIPIENT_ACCOUNT = "recipient_account"; // From Helper 2
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_TYPE = "type";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_BALANCE_AFTER = "balance_after"; // NEW: For TransactionRecord

    // Loan Repayments Table Columns
    private static final String KEY_REPAYMENT_ID = "repayment_id";
    private static final String KEY_LOAN_ID = "loan_id";
    private static final String KEY_REPAYMENT_AMOUNT = "amount";
    private static final String KEY_REPAYMENT_TIMESTAMP = "timestamp";
    private static final String KEY_REMAINING_AFTER = "remaining_after"; // NEW: For LoanRepayment

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Merged Transactions Table
        String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_TRANSACTION_ID + " TEXT NOT NULL, " +
                KEY_FROM_ACCOUNT_ID + " TEXT, " +
                KEY_TO_ACCOUNT_ID + " TEXT, " +
                KEY_RECIPIENT_ACCOUNT + " TEXT, " +
                KEY_AMOUNT + " REAL NOT NULL, " +
                KEY_TYPE + " TEXT NOT NULL, " +
                KEY_TIMESTAMP + " INTEGER NOT NULL, " +
                KEY_DESCRIPTION + " TEXT, " +
                KEY_BALANCE_AFTER + " REAL DEFAULT 0.0" +
                ")";
        db.execSQL(CREATE_TRANSACTIONS_TABLE);

        // Create Loan Repayments Table
        String CREATE_LOAN_REPAYMENTS_TABLE = "CREATE TABLE " + TABLE_LOAN_REPAYMENTS + " (" +
                KEY_REPAYMENT_ID + " TEXT PRIMARY KEY, " +
                KEY_LOAN_ID + " TEXT NOT NULL, " +
                KEY_REPAYMENT_AMOUNT + " REAL NOT NULL, " +
                KEY_REPAYMENT_TIMESTAMP + " INTEGER NOT NULL, " +
                KEY_REMAINING_AFTER + " REAL DEFAULT 0.0" +
                ")";
        db.execSQL(CREATE_LOAN_REPAYMENTS_TABLE);

        // Indexes for performance
        db.execSQL("CREATE INDEX idx_transactions_from_account ON " + TABLE_TRANSACTIONS + "(" + KEY_FROM_ACCOUNT_ID + ")");
        db.execSQL("CREATE INDEX idx_transactions_to_account ON " + TABLE_TRANSACTIONS + "(" + KEY_TO_ACCOUNT_ID + ")");
        db.execSQL("CREATE INDEX idx_transactions_timestamp ON " + TABLE_TRANSACTIONS + "(" + KEY_TIMESTAMP + " DESC)");
        db.execSQL("CREATE INDEX idx_transactions_type ON " + TABLE_TRANSACTIONS + "(" + KEY_TYPE + ")");
        db.execSQL("CREATE INDEX idx_loan_repayments_loan_id ON " + TABLE_LOAN_REPAYMENTS + "(" + KEY_LOAN_ID + ")");
        db.execSQL("CREATE INDEX idx_loan_repayments_timestamp ON " + TABLE_LOAN_REPAYMENTS + "(" + KEY_REPAYMENT_TIMESTAMP + " DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migration from version 1 to 2
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOAN_REPAYMENTS);
            onCreate(db);
        } else if (oldVersion < 3) {
            // Migration from version 2 to 3 - add new columns
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + KEY_BALANCE_AFTER + " REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_LOAN_REPAYMENTS + " ADD COLUMN " + KEY_REMAINING_AFTER + " REAL DEFAULT 0.0");
        }
    }

    // ==================== TRANSACTION METHODS (Merged) ====================

    /**
     * Insert a new Transaction (from old Helper 1)
     */
    public long insertTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_TRANSACTION_ID, transaction.getTransactionId());
        values.put(KEY_FROM_ACCOUNT_ID, transaction.getFromAccountId());
        values.put(KEY_TO_ACCOUNT_ID, transaction.getToAccountId());
        values.put(KEY_AMOUNT, transaction.getAmount());
        values.put(KEY_TYPE, transaction.getType());
        values.put(KEY_TIMESTAMP, transaction.getTimestamp());
        values.put(KEY_DESCRIPTION, transaction.getDescription());
        values.put(KEY_BALANCE_AFTER, 0.0); // Default value

        long result = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return result;
    }

    /**
     * Insert a new TransactionRecord (from new Helper 2)
     */
    public long insertTransactionRecord(TransactionRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_TRANSACTION_ID, record.getTransactionId());
        values.put(KEY_FROM_ACCOUNT_ID, record.getAccountNumber());
        values.put(KEY_RECIPIENT_ACCOUNT, record.getRecipientAccount());
        values.put(KEY_TYPE, record.getType());
        values.put(KEY_AMOUNT, record.getAmount());
        values.put(KEY_DESCRIPTION, record.getDescription());
        values.put(KEY_TIMESTAMP, record.getTimestamp());
        values.put(KEY_BALANCE_AFTER, record.getBalanceAfter());

        long result = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return result;
    }

    /**
     * Get all TransactionRecords (from new Helper 2)
     */
    public List<TransactionRecord> getAllTransactionRecords() {
        List<TransactionRecord> list = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToTransactionRecord(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * Get all transactions for a specific account (sent or received) - from old Helper 1
     */
    public List<Transaction> getAllTransactionsForAccount(String accountId) {
        List<Transaction> transactions = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " WHERE " + KEY_FROM_ACCOUNT_ID + " = ? OR " +
                KEY_TO_ACCOUNT_ID + " = ? " +
                " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{accountId, accountId});

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = cursorToTransaction(cursor);
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return transactions;
    }

    /**
     * Get transactions by type - from old Helper 1
     */
    public List<Transaction> getTransactionsByType(String type) {
        List<Transaction> transactions = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " WHERE " + KEY_TYPE + " = ? " +
                " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = cursorToTransaction(cursor);
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return transactions;
    }

    /**
     * Get transactions by type for a specific account - from old Helper 1
     */
    public List<Transaction> getTransactionsByTypeForAccount(String accountId, String type) {
        List<Transaction> transactions = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " WHERE (" + KEY_FROM_ACCOUNT_ID + " = ? OR " +
                KEY_TO_ACCOUNT_ID + " = ?) AND " +
                KEY_TYPE + " = ? " +
                " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{accountId, accountId, type});

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = cursorToTransaction(cursor);
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return transactions;
    }

    /**
     * Get account statement (all transactions ordered by timestamp) - from old Helper 1
     */
    public List<Transaction> getAccountStatement(String accountId, long startDate, long endDate) {
        List<Transaction> transactions = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " WHERE (" + KEY_FROM_ACCOUNT_ID + " = ? OR " +
                KEY_TO_ACCOUNT_ID + " = ?) AND " +
                KEY_TIMESTAMP + " BETWEEN ? AND ? " +
                " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query,
                new String[]{accountId, accountId, String.valueOf(startDate), String.valueOf(endDate)});

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = cursorToTransaction(cursor);
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return transactions;
    }

    /**
     * Get a single transaction by ID - from old Helper 1
     */
    public Transaction getTransactionById(String transactionId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRANSACTIONS,
                null,
                KEY_TRANSACTION_ID + " = ?",
                new String[]{transactionId},
                null, null, null);

        Transaction transaction = null;
        if (cursor != null && cursor.moveToFirst()) {
            transaction = cursorToTransaction(cursor);
            cursor.close();
        }

        db.close();
        return transaction;
    }

    /**
     * Delete a transaction - from old Helper 1
     */
    public int deleteTransaction(String transactionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_TRANSACTIONS,
                KEY_TRANSACTION_ID + " = ?",
                new String[]{transactionId});
        db.close();
        return result;
    }

    /**
     * Get total transaction count for an account - from old Helper 1
     */
    public int getTransactionCount(String accountId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS +
                " WHERE " + KEY_FROM_ACCOUNT_ID + " = ? OR " +
                KEY_TO_ACCOUNT_ID + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{accountId, accountId});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    /**
     * Get recent transactions (from new Helper 2)
     */
    public List<TransactionRecord> getRecentTransactions(int limit) {
        List<TransactionRecord> list = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + KEY_TIMESTAMP + " DESC LIMIT " + limit;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToTransactionRecord(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    // ==================== LOAN REPAYMENT METHODS ====================

    /**
     * Insert a new loan repayment (merged from both helpers)
     */
    public long insertLoanRepayment(LoanRepayment repayment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_REPAYMENT_ID, repayment.getRepaymentId());
        values.put(KEY_LOAN_ID, repayment.getLoanId());
        values.put(KEY_REPAYMENT_AMOUNT, repayment.getAmount());
        values.put(KEY_REPAYMENT_TIMESTAMP, repayment.getTimestamp());
        values.put(KEY_REMAINING_AFTER, repayment.getRemainingAfter());

        long result = db.insert(TABLE_LOAN_REPAYMENTS, null, values);
        db.close();
        return result;
    }

    /**
     * Get all repayments for a specific loan - from old Helper 1
     */
    public List<LoanRepayment> getAllRepaymentsForLoan(String loanId) {
        List<LoanRepayment> repayments = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_LOAN_REPAYMENTS +
                " WHERE " + KEY_LOAN_ID + " = ? " +
                " ORDER BY " + KEY_REPAYMENT_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{loanId});

        if (cursor.moveToFirst()) {
            do {
                LoanRepayment repayment = cursorToLoanRepayment(cursor);
                repayments.add(repayment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return repayments;
    }

    /**
     * Get total repaid amount for a loan (merged from both helpers)
     */
    public double getTotalRepaidAmount(String loanId) {
        String query = "SELECT SUM(" + KEY_REPAYMENT_AMOUNT + ") FROM " + TABLE_LOAN_REPAYMENTS +
                " WHERE " + KEY_LOAN_ID + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{loanId});

        double totalRepaid = 0.0;
        if (cursor.moveToFirst()) {
            totalRepaid = cursor.getDouble(0);
        }

        cursor.close();
        db.close();
        return totalRepaid;
    }

    /**
     * Get loan repayment statement - from old Helper 1
     */
    public List<LoanRepayment> getLoanRepaymentStatement(String loanId, long startDate, long endDate) {
        List<LoanRepayment> repayments = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_LOAN_REPAYMENTS +
                " WHERE " + KEY_LOAN_ID + " = ? AND " +
                KEY_REPAYMENT_TIMESTAMP + " BETWEEN ? AND ? " +
                " ORDER BY " + KEY_REPAYMENT_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query,
                new String[]{loanId, String.valueOf(startDate), String.valueOf(endDate)});

        if (cursor.moveToFirst()) {
            do {
                LoanRepayment repayment = cursorToLoanRepayment(cursor);
                repayments.add(repayment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return repayments;
    }

    /**
     * Get a single repayment by ID - from old Helper 1
     */
    public LoanRepayment getRepaymentById(String repaymentId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_LOAN_REPAYMENTS,
                null,
                KEY_REPAYMENT_ID + " = ?",
                new String[]{repaymentId},
                null, null, null);

        LoanRepayment repayment = null;
        if (cursor != null && cursor.moveToFirst()) {
            repayment = cursorToLoanRepayment(cursor);
            cursor.close();
        }

        db.close();
        return repayment;
    }

    /**
     * Delete a repayment - from old Helper 1
     */
    public int deleteRepayment(String repaymentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_LOAN_REPAYMENTS,
                KEY_REPAYMENT_ID + " = ?",
                new String[]{repaymentId});
        db.close();
        return result;
    }

    /**
     * Get repayment count for a loan - from old Helper 1
     */
    public int getRepaymentCount(String loanId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_LOAN_REPAYMENTS +
                " WHERE " + KEY_LOAN_ID + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{loanId});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    // ==================== UTILITY & CLEANUP ====================

    /**
     * Clear all data from database (merged from both helpers)
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, null, null);
        db.delete(TABLE_LOAN_REPAYMENTS, null, null);
        db.close();
    }

    /**
     * Close database connection - from old Helper 1
     */
    public void closeDatabase() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Convert cursor to Transaction object - from old Helper 1
     */
    private Transaction cursorToTransaction(Cursor cursor) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRANSACTION_ID)));
        transaction.setFromAccountId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_ACCOUNT_ID)));
        transaction.setToAccountId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_ACCOUNT_ID)));
        transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)));
        transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
        transaction.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
        transaction.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
        return transaction;
    }

    /**
     * Convert cursor to TransactionRecord object - from new Helper 2 (enhanced)
     */
    private TransactionRecord cursorToTransactionRecord(Cursor cursor) {
        TransactionRecord record = new TransactionRecord();
        record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
        record.setTransactionId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRANSACTION_ID)));
        record.setAccountNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_ACCOUNT_ID)));
        record.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
        record.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)));
        record.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
        record.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
        record.setRecipientAccount(cursor.getString(cursor.getColumnIndexOrThrow(KEY_RECIPIENT_ACCOUNT)));

        // Get balance_after if column exists
        int balanceAfterColumnIndex = cursor.getColumnIndex(KEY_BALANCE_AFTER);
        if (balanceAfterColumnIndex != -1) {
            record.setBalanceAfter(cursor.getDouble(balanceAfterColumnIndex));
        }

        return record;
    }

    /**
     * Convert cursor to LoanRepayment object - from old Helper 1 (enhanced)
     */
    private LoanRepayment cursorToLoanRepayment(Cursor cursor) {
        LoanRepayment repayment = new LoanRepayment();
        repayment.setRepaymentId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPAYMENT_ID)));
        repayment.setLoanId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOAN_ID)));
        repayment.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_REPAYMENT_AMOUNT)));
        repayment.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_REPAYMENT_TIMESTAMP)));

        // Get remaining_after if column exists
        int remainingAfterColumnIndex = cursor.getColumnIndex(KEY_REMAINING_AFTER);
        if (remainingAfterColumnIndex != -1) {
            repayment.setRemainingAfter(cursor.getDouble(remainingAfterColumnIndex));
        }

        return repayment;
    }

    /**
     * Update balance after for a transaction
     */
    public int updateTransactionBalanceAfter(String transactionId, double balanceAfter) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BALANCE_AFTER, balanceAfter);

        int result = db.update(TABLE_TRANSACTIONS, values,
                KEY_TRANSACTION_ID + " = ?",
                new String[]{transactionId});
        db.close();
        return result;
    }

    /**
     * Update remaining after for a loan repayment
     */
    public int updateRepaymentRemainingAfter(String repaymentId, double remainingAfter) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_REMAINING_AFTER, remainingAfter);

        int result = db.update(TABLE_LOAN_REPAYMENTS, values,
                KEY_REPAYMENT_ID + " = ?",
                new String[]{repaymentId});
        db.close();
        return result;
    }

    /**
     * Get transactions for a specific account number (for TransactionRecord style)
     */
    public List<TransactionRecord> getTransactionRecordsForAccount(String accountNumber) {
        List<TransactionRecord> list = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " WHERE " + KEY_FROM_ACCOUNT_ID + " = ?" +
                " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{accountNumber});

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToTransactionRecord(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }
}


//package com.izak.demobankingapp20260118.database;
//
//import android.content.ContentValues;
//import android.content.Context;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//import com.izak.demobankingapp20260118.models.Transaction;
//import com.izak.demobankingapp20260118.models.LoanRepayment;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class DatabaseHelper extends SQLiteOpenHelper {
//
//    // Database Info
//    private static final String DATABASE_NAME = "BankingAppDB";
//    private static final int DATABASE_VERSION = 1;
//
//    // Table Names
//    private static final String TABLE_TRANSACTIONS = "transactions";
//    private static final String TABLE_LOAN_REPAYMENTS = "loan_repayments";
//
//    // Transactions Table Columns
//    private static final String KEY_TRANSACTION_ID = "transaction_id";
//    private static final String KEY_FROM_ACCOUNT_ID = "from_account_id";
//    private static final String KEY_TO_ACCOUNT_ID = "to_account_id";
//    private static final String KEY_AMOUNT = "amount";
//    private static final String KEY_TYPE = "type";
//    private static final String KEY_TIMESTAMP = "timestamp";
//    private static final String KEY_DESCRIPTION = "description";
//
//    // Loan Repayments Table Columns
//    private static final String KEY_REPAYMENT_ID = "repayment_id";
//    private static final String KEY_LOAN_ID = "loan_id";
//
//    private static DatabaseHelper instance;
//
//    public static synchronized DatabaseHelper getInstance(Context context) {
//        if (instance == null) {
//            instance = new DatabaseHelper(context.getApplicationContext());
//        }
//        return instance;
//    }
//
//    private DatabaseHelper(Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        // Create Transactions Table
//        String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
//                KEY_TRANSACTION_ID + " TEXT PRIMARY KEY, " +
//                KEY_FROM_ACCOUNT_ID + " TEXT, " +
//                KEY_TO_ACCOUNT_ID + " TEXT, " +
//                KEY_AMOUNT + " REAL NOT NULL, " +
//                KEY_TYPE + " TEXT NOT NULL, " +
//                KEY_TIMESTAMP + " INTEGER NOT NULL, " +
//                KEY_DESCRIPTION + " TEXT" +
//                ")";
//        db.execSQL(CREATE_TRANSACTIONS_TABLE);
//
//        // Create Loan Repayments Table
//        String CREATE_LOAN_REPAYMENTS_TABLE = "CREATE TABLE " + TABLE_LOAN_REPAYMENTS + " (" +
//                KEY_REPAYMENT_ID + " TEXT PRIMARY KEY, " +
//                KEY_LOAN_ID + " TEXT NOT NULL, " +
//                KEY_AMOUNT + " REAL NOT NULL, " +
//                KEY_TIMESTAMP + " INTEGER NOT NULL" +
//                ")";
//        db.execSQL(CREATE_LOAN_REPAYMENTS_TABLE);
//
//        // Create indexes for better query performance
//        db.execSQL("CREATE INDEX idx_transactions_from_account ON " + TABLE_TRANSACTIONS +
//                "(" + KEY_FROM_ACCOUNT_ID + ")");
//        db.execSQL("CREATE INDEX idx_transactions_to_account ON " + TABLE_TRANSACTIONS +
//                "(" + KEY_TO_ACCOUNT_ID + ")");
//        db.execSQL("CREATE INDEX idx_transactions_timestamp ON " + TABLE_TRANSACTIONS +
//                "(" + KEY_TIMESTAMP + ")");
//        db.execSQL("CREATE INDEX idx_loan_repayments_loan_id ON " + TABLE_LOAN_REPAYMENTS +
//                "(" + KEY_LOAN_ID + ")");
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        // Drop older tables if existed
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOAN_REPAYMENTS);
//
//        // Create tables again
//        onCreate(db);
//    }
//
//    // ==================== TRANSACTION METHODS ====================
//
//    /**
//     * Insert a new transaction
//     */
//    public long insertTransaction(Transaction transaction) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//
//        values.put(KEY_TRANSACTION_ID, transaction.getTransactionId());
//        values.put(KEY_FROM_ACCOUNT_ID, transaction.getFromAccountId());
//        values.put(KEY_TO_ACCOUNT_ID, transaction.getToAccountId());
//        values.put(KEY_AMOUNT, transaction.getAmount());
//        values.put(KEY_TYPE, transaction.getType());
//        values.put(KEY_TIMESTAMP, transaction.getTimestamp());
//        values.put(KEY_DESCRIPTION, transaction.getDescription());
//
//        long result = db.insert(TABLE_TRANSACTIONS, null, values);
//        db.close();
//        return result;
//    }
//
//    /**
//     * Get all transactions for a specific account (sent or received)
//     */
//    public List<Transaction> getAllTransactionsForAccount(String accountId) {
//        List<Transaction> transactions = new ArrayList<>();
//
//        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
//                " WHERE " + KEY_FROM_ACCOUNT_ID + " = ? OR " +
//                KEY_TO_ACCOUNT_ID + " = ? " +
//                " ORDER BY " + KEY_TIMESTAMP + " DESC";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, new String[]{accountId, accountId});
//
//        if (cursor.moveToFirst()) {
//            do {
//                Transaction transaction = cursorToTransaction(cursor);
//                transactions.add(transaction);
//            } while (cursor.moveToNext());
//        }
//
//        cursor.close();
//        db.close();
//        return transactions;
//    }
//
//    /**
//     * Get transactions by type
//     */
//    public List<Transaction> getTransactionsByType(String type) {
//        List<Transaction> transactions = new ArrayList<>();
//
//        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
//                " WHERE " + KEY_TYPE + " = ? " +
//                " ORDER BY " + KEY_TIMESTAMP + " DESC";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, new String[]{type});
//
//        if (cursor.moveToFirst()) {
//            do {
//                Transaction transaction = cursorToTransaction(cursor);
//                transactions.add(transaction);
//            } while (cursor.moveToNext());
//        }
//
//        cursor.close();
//        db.close();
//        return transactions;
//    }
//
//    /**
//     * Get transactions by type for a specific account
//     */
//    public List<Transaction> getTransactionsByTypeForAccount(String accountId, String type) {
//        List<Transaction> transactions = new ArrayList<>();
//
//        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
//                " WHERE (" + KEY_FROM_ACCOUNT_ID + " = ? OR " +
//                KEY_TO_ACCOUNT_ID + " = ?) AND " +
//                KEY_TYPE + " = ? " +
//                " ORDER BY " + KEY_TIMESTAMP + " DESC";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, new String[]{accountId, accountId, type});
//
//        if (cursor.moveToFirst()) {
//            do {
//                Transaction transaction = cursorToTransaction(cursor);
//                transactions.add(transaction);
//            } while (cursor.moveToNext());
//        }
//
//        cursor.close();
//        db.close();
//        return transactions;
//    }
//
//    /**
//     * Get account statement (all transactions ordered by timestamp)
//     */
//    public List<Transaction> getAccountStatement(String accountId, long startDate, long endDate) {
//        List<Transaction> transactions = new ArrayList<>();
//
//        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
//                " WHERE (" + KEY_FROM_ACCOUNT_ID + " = ? OR " +
//                KEY_TO_ACCOUNT_ID + " = ?) AND " +
//                KEY_TIMESTAMP + " BETWEEN ? AND ? " +
//                " ORDER BY " + KEY_TIMESTAMP + " DESC";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query,
//                new String[]{accountId, accountId, String.valueOf(startDate), String.valueOf(endDate)});
//
//        if (cursor.moveToFirst()) {
//            do {
//                Transaction transaction = cursorToTransaction(cursor);
//                transactions.add(transaction);
//            } while (cursor.moveToNext());
//        }
//
//        cursor.close();
//        db.close();
//        return transactions;
//    }
//
//    /**
//     * Get a single transaction by ID
//     */
//    public Transaction getTransactionById(String transactionId) {
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        Cursor cursor = db.query(TABLE_TRANSACTIONS,
//                null,
//                KEY_TRANSACTION_ID + " = ?",
//                new String[]{transactionId},
//                null, null, null);
//
//        Transaction transaction = null;
//        if (cursor != null && cursor.moveToFirst()) {
//            transaction = cursorToTransaction(cursor);
//            cursor.close();
//        }
//
//        db.close();
//        return transaction;
//    }
//
//    /**
//     * Delete a transaction
//     */
//    public int deleteTransaction(String transactionId) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        int result = db.delete(TABLE_TRANSACTIONS,
//                KEY_TRANSACTION_ID + " = ?",
//                new String[]{transactionId});
//        db.close();
//        return result;
//    }
//
//    /**
//     * Get total transaction count for an account
//     */
//    public int getTransactionCount(String accountId) {
//        String query = "SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS +
//                " WHERE " + KEY_FROM_ACCOUNT_ID + " = ? OR " +
//                KEY_TO_ACCOUNT_ID + " = ?";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, new String[]{accountId, accountId});
//
//        int count = 0;
//        if (cursor.moveToFirst()) {
//            count = cursor.getInt(0);
//        }
//
//        cursor.close();
//        db.close();
//        return count;
//    }
//
//    // ==================== LOAN REPAYMENT METHODS ====================
//
//    /**
//     * Insert a new loan repayment
//     */
//    public long insertLoanRepayment(LoanRepayment repayment) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//
//        values.put(KEY_REPAYMENT_ID, repayment.getRepaymentId());
//        values.put(KEY_LOAN_ID, repayment.getLoanId());
//        values.put(KEY_AMOUNT, repayment.getAmount());
//        values.put(KEY_TIMESTAMP, repayment.getTimestamp());
//
//        long result = db.insert(TABLE_LOAN_REPAYMENTS, null, values);
//        db.close();
//        return result;
//    }
//
//    /**
//     * Get all repayments for a specific loan
//     */
//    public List<LoanRepayment> getAllRepaymentsForLoan(String loanId) {
//        List<LoanRepayment> repayments = new ArrayList<>();
//
//        String query = "SELECT * FROM " + TABLE_LOAN_REPAYMENTS +
//                " WHERE " + KEY_LOAN_ID + " = ? " +
//                " ORDER BY " + KEY_TIMESTAMP + " DESC";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, new String[]{loanId});
//
//        if (cursor.moveToFirst()) {
//            do {
//                LoanRepayment repayment = cursorToLoanRepayment(cursor);
//                repayments.add(repayment);
//            } while (cursor.moveToNext());
//        }
//
//        cursor.close();
//        db.close();
//        return repayments;
//    }
//
//    /**
//     * Get total repaid amount for a loan
//     */
//    public double getTotalRepaidAmount(String loanId) {
//        String query = "SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_LOAN_REPAYMENTS +
//                " WHERE " + KEY_LOAN_ID + " = ?";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, new String[]{loanId});
//
//        double totalRepaid = 0.0;
//        if (cursor.moveToFirst()) {
//            totalRepaid = cursor.getDouble(0);
//        }
//
//        cursor.close();
//        db.close();
//        return totalRepaid;
//    }
//
//    /**
//     * Get loan repayment statement
//     */
//    public List<LoanRepayment> getLoanRepaymentStatement(String loanId, long startDate, long endDate) {
//        List<LoanRepayment> repayments = new ArrayList<>();
//
//        String query = "SELECT * FROM " + TABLE_LOAN_REPAYMENTS +
//                " WHERE " + KEY_LOAN_ID + " = ? AND " +
//                KEY_TIMESTAMP + " BETWEEN ? AND ? " +
//                " ORDER BY " + KEY_TIMESTAMP + " DESC";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query,
//                new String[]{loanId, String.valueOf(startDate), String.valueOf(endDate)});
//
//        if (cursor.moveToFirst()) {
//            do {
//                LoanRepayment repayment = cursorToLoanRepayment(cursor);
//                repayments.add(repayment);
//            } while (cursor.moveToNext());
//        }
//
//        cursor.close();
//        db.close();
//        return repayments;
//    }
//
//    /**
//     * Get a single repayment by ID
//     */
//    public LoanRepayment getRepaymentById(String repaymentId) {
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        Cursor cursor = db.query(TABLE_LOAN_REPAYMENTS,
//                null,
//                KEY_REPAYMENT_ID + " = ?",
//                new String[]{repaymentId},
//                null, null, null);
//
//        LoanRepayment repayment = null;
//        if (cursor != null && cursor.moveToFirst()) {
//            repayment = cursorToLoanRepayment(cursor);
//            cursor.close();
//        }
//
//        db.close();
//        return repayment;
//    }
//
//    /**
//     * Delete a repayment
//     */
//    public int deleteRepayment(String repaymentId) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        int result = db.delete(TABLE_LOAN_REPAYMENTS,
//                KEY_REPAYMENT_ID + " = ?",
//                new String[]{repaymentId});
//        db.close();
//        return result;
//    }
//
//    /**
//     * Get repayment count for a loan
//     */
//    public int getRepaymentCount(String loanId) {
//        String query = "SELECT COUNT(*) FROM " + TABLE_LOAN_REPAYMENTS +
//                " WHERE " + KEY_LOAN_ID + " = ?";
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, new String[]{loanId});
//
//        int count = 0;
//        if (cursor.moveToFirst()) {
//            count = cursor.getInt(0);
//        }
//
//        cursor.close();
//        db.close();
//        return count;
//    }
//
//    // ==================== UTILITY METHODS ====================
//
//    /**
//     * Clear all data from database
//     */
//    public void clearAllData() {
//        SQLiteDatabase db = this.getWritableDatabase();
//        db.delete(TABLE_TRANSACTIONS, null, null);
//        db.delete(TABLE_LOAN_REPAYMENTS, null, null);
//        db.close();
//    }
//
//    /**
//     * Close database connection
//     */
//    public void closeDatabase() {
//        SQLiteDatabase db = this.getReadableDatabase();
//        if (db != null && db.isOpen()) {
//            db.close();
//        }
//    }
//
//    // ==================== HELPER METHODS ====================
//
//    /**
//     * Convert cursor to Transaction object
//     */
//    private Transaction cursorToTransaction(Cursor cursor) {
//        Transaction transaction = new Transaction();
//        transaction.setTransactionId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRANSACTION_ID)));
//        transaction.setFromAccountId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_ACCOUNT_ID)));
//        transaction.setToAccountId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_ACCOUNT_ID)));
//        transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)));
//        transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
//        transaction.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
//        transaction.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
//        return transaction;
//    }
//
//    /**
//     * Convert cursor to LoanRepayment object
//     */
//    private LoanRepayment cursorToLoanRepayment(Cursor cursor) {
//        LoanRepayment repayment = new LoanRepayment();
//        repayment.setRepaymentId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPAYMENT_ID)));
//        repayment.setLoanId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOAN_ID)));
//        repayment.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)));
//        repayment.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
//        return repayment;
//    }
//}