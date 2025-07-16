# Day 9: Exception Handling (July 18, 2025)

**Date:** July 18, 2025  
**Duration:** 2.5 hours  
**Focus:** Master comprehensive exception handling and building robust applications

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master try-catch-finally blocks and exception handling flow
- ✅ Understand checked vs unchecked exceptions and the exception hierarchy
- ✅ Create and throw custom exceptions with meaningful error messages
- ✅ Learn exception handling best practices and patterns
- ✅ Build robust applications with comprehensive error handling

---

## 📚 Part 1: Exception Hierarchy and Types (45 minutes)

### **Understanding Java Exception Hierarchy**

```
Throwable
├── Error (Unchecked - JVM errors, not recoverable)
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── VirtualMachineError
└── Exception
    ├── RuntimeException (Unchecked - programming errors)
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── IllegalArgumentException
    │   └── NumberFormatException
    └── Checked Exceptions (Must be handled or declared)
        ├── IOException
        ├── SQLException
        ├── ClassNotFoundException
        └── ParseException
```

### **Try-Catch-Finally Structure**

```java
try {
    // Code that might throw an exception
    // This block is mandatory
} catch (SpecificException e) {
    // Handle specific exception
    // Multiple catch blocks allowed
} catch (AnotherException e) {
    // Handle another type of exception
} catch (Exception e) {
    // Handle any remaining exceptions (should be last)
} finally {
    // Code that always executes
    // Optional block
    // Used for cleanup (closing files, connections)
}
```

### **Basic Exception Handling Examples**

Create `BasicExceptionDemo.java`:

```java
/**
 * Basic Exception Handling Demonstration
 * Shows common exception types and handling patterns
 */
public class BasicExceptionDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Basic Exception Handling Demo ===\n");
        
        // 1. NullPointerException handling
        demonstrateNullPointerException();
        
        // 2. ArrayIndexOutOfBoundsException handling
        demonstrateArrayIndexException();
        
        // 3. NumberFormatException handling
        demonstrateNumberFormatException();
        
        // 4. Multiple catch blocks
        demonstrateMultipleCatchBlocks();
        
        // 5. Finally block demonstration
        demonstrateFinallyBlock();
    }
    
    private static void demonstrateNullPointerException() {
        System.out.println("1. NullPointerException Handling:");
        try {
            String str = null;
            int length = str.length(); // This will throw NullPointerException
            System.out.println("Length: " + length);
        } catch (NullPointerException e) {
            System.out.println("   ❌ Caught NullPointerException: " + e.getMessage());
            System.out.println("   💡 Fix: Check for null before using object");
        }
        
        // Safe approach
        String str = null;
        if (str != null) {
            System.out.println("   ✅ Safe approach: Length is " + str.length());
        } else {
            System.out.println("   ✅ Safe approach: String is null, length is 0");
        }
        System.out.println();
    }
    
    private static void demonstrateArrayIndexException() {
        System.out.println("2. ArrayIndexOutOfBoundsException Handling:");
        try {
            int[] numbers = {1, 2, 3};
            int value = numbers[5]; // Index 5 doesn't exist
            System.out.println("Value: " + value);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("   ❌ Caught ArrayIndexOutOfBoundsException: " + e.getMessage());
            System.out.println("   💡 Fix: Check array bounds before accessing");
        }
        
        // Safe approach
        int[] numbers = {1, 2, 3};
        int index = 5;
        if (index >= 0 && index < numbers.length) {
            System.out.println("   ✅ Safe approach: Value is " + numbers[index]);
        } else {
            System.out.println("   ✅ Safe approach: Index " + index + " is out of bounds");
        }
        System.out.println();
    }
    
    private static void demonstrateNumberFormatException() {
        System.out.println("3. NumberFormatException Handling:");
        try {
            String invalidNumber = "abc123";
            int number = Integer.parseInt(invalidNumber);
            System.out.println("Number: " + number);
        } catch (NumberFormatException e) {
            System.out.println("   ❌ Caught NumberFormatException: " + e.getMessage());
            System.out.println("   💡 Fix: Validate string format before parsing");
        }
        
        // Safe approach with validation
        String input = "abc123";
        try {
            int number = Integer.parseInt(input);
            System.out.println("   ✅ Safe approach: Number is " + number);
        } catch (NumberFormatException e) {
            System.out.println("   ✅ Safe approach: '" + input + "' is not a valid number, using default value 0");
        }
        System.out.println();
    }
    
    private static void demonstrateMultipleCatchBlocks() {
        System.out.println("4. Multiple Catch Blocks:");
        
        String[] testInputs = {"123", "abc", null};
        
        for (String input : testInputs) {
            try {
                int result = processInput(input);
                System.out.println("   ✅ Processed '" + input + "' → " + result);
                
            } catch (NumberFormatException e) {
                System.out.println("   ❌ NumberFormatException for '" + input + "': " + e.getMessage());
                
            } catch (NullPointerException e) {
                System.out.println("   ❌ NullPointerException for null input: " + e.getMessage());
                
            } catch (Exception e) {
                System.out.println("   ❌ Unexpected exception: " + e.getMessage());
                
            } finally {
                System.out.println("   🧹 Cleanup completed for input: " + input);
            }
        }
        System.out.println();
    }
    
    private static int processInput(String input) {
        return Integer.parseInt(input) * 2;
    }
    
    private static void demonstrateFinallyBlock() {
        System.out.println("5. Finally Block Demonstration:");
        
        try {
            System.out.println("   📝 Executing try block");
            int result = 10 / 0; // This will throw ArithmeticException
            System.out.println("   Result: " + result);
        } catch (ArithmeticException e) {
            System.out.println("   ❌ Caught ArithmeticException: " + e.getMessage());
        } finally {
            System.out.println("   🧹 Finally block always executes - cleanup completed");
        }
        
        System.out.println("   ✅ Program continues after exception handling");
    }
}
```

---

## 🎯 Part 2: Custom Exceptions (45 minutes)

### **Creating Custom Exception Classes**

Create `BankAccountException.java`:

```java
/**
 * Custom checked exception for bank account operations
 * Demonstrates creating domain-specific exceptions with detailed error information
 */
public class BankAccountException extends Exception {
    private String accountNumber;
    private String operation;
    private double attemptedAmount;
    
    // Basic constructor with message
    public BankAccountException(String message) {
        super(message);
    }
    
    // Constructor with message and cause (exception chaining)
    public BankAccountException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // Detailed constructor for bank operations
    public BankAccountException(String message, String accountNumber, String operation, double attemptedAmount) {
        super(message);
        this.accountNumber = accountNumber;
        this.operation = operation;
        this.attemptedAmount = attemptedAmount;
    }
    
    // Constructor with all parameters including cause
    public BankAccountException(String message, String accountNumber, String operation, 
                               double attemptedAmount, Throwable cause) {
        super(message, cause);
        this.accountNumber = accountNumber;
        this.operation = operation;
        this.attemptedAmount = attemptedAmount;
    }
    
    // Getters for detailed error information
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public double getAttemptedAmount() {
        return attemptedAmount;
    }
    
    // Enhanced error message with details
    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        
        if (accountNumber != null || operation != null || attemptedAmount > 0) {
            message.append(" [Details: ");
            
            if (accountNumber != null) {
                message.append("Account: ").append(accountNumber);
            }
            
            if (operation != null) {
                if (accountNumber != null) message.append(", ");
                message.append("Operation: ").append(operation);
            }
            
            if (attemptedAmount > 0) {
                if (accountNumber != null || operation != null) message.append(", ");
                message.append("Amount: $").append(String.format("%.2f", attemptedAmount));
            }
            
            message.append("]");
        }
        
        return message.toString();
    }
    
    @Override
    public String toString() {
        return "BankAccountException: " + getMessage();
    }
}
```

Create `InsufficientFundsException.java`:

```java
/**
 * Custom unchecked exception for insufficient funds scenarios
 * Extends RuntimeException for programming logic errors
 */
public class InsufficientFundsException extends RuntimeException {
    private double currentBalance;
    private double requestedAmount;
    private double shortfall;
    private String accountNumber;
    
    public InsufficientFundsException(double currentBalance, double requestedAmount, String accountNumber) {
        super(createMessage(currentBalance, requestedAmount, accountNumber));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.shortfall = requestedAmount - currentBalance;
        this.accountNumber = accountNumber;
    }
    
    public InsufficientFundsException(String message, double currentBalance, double requestedAmount, String accountNumber) {
        super(message);
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.shortfall = requestedAmount - currentBalance;
        this.accountNumber = accountNumber;
    }
    
    private static String createMessage(double currentBalance, double requestedAmount, String accountNumber) {
        return String.format(
            "Insufficient funds in account %s. Current balance: $%.2f, Requested: $%.2f, Shortfall: $%.2f",
            accountNumber, currentBalance, requestedAmount, requestedAmount - currentBalance
        );
    }
    
    // Getters for detailed information
    public double getCurrentBalance() {
        return currentBalance;
    }
    
    public double getRequestedAmount() {
        return requestedAmount;
    }
    
    public double getShortfall() {
        return shortfall;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    // Helper method to suggest solutions
    public String getSuggestion() {
        if (shortfall <= 100) {
            return "Consider a small deposit of $" + String.format("%.2f", shortfall) + " to complete this transaction.";
        } else if (shortfall <= 1000) {
            return "You may want to transfer funds from another account or make a larger deposit.";
        } else {
            return "This transaction requires a significant deposit. Please contact customer service for assistance.";
        }
    }
    
    @Override
    public String toString() {
        return "InsufficientFundsException: " + getMessage() + "\\nSuggestion: " + getSuggestion();
    }
}
```

---

## 🏦 Part 3: Banking System with Exception Handling (45 minutes)

### **Robust Banking System Implementation**

Create `BankAccount.java`:

```java
import java.time.LocalDateTime;
import java.util.*;

/**
 * Banking System with comprehensive exception handling
 * Demonstrates custom exceptions, validation, and error recovery
 */
public class BankAccount {
    private String accountNumber;
    private String accountHolderName;
    private double balance;
    private boolean isActive;
    private List<String> transactionHistory;
    private static final double MINIMUM_BALANCE = 0.0;
    private static final double MAXIMUM_TRANSACTION = 50000.0;
    
    public BankAccount(String accountNumber, String accountHolderName, double initialBalance) 
            throws BankAccountException {
        validateAccountNumber(accountNumber);
        validateAccountHolderName(accountHolderName);
        validateInitialBalance(initialBalance);
        
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = initialBalance;
        this.isActive = true;
        this.transactionHistory = new ArrayList<>();
        
        logTransaction("Account created with initial balance: $" + String.format("%.2f", initialBalance));
    }
    
    /**
     * Deposits money with comprehensive validation and exception handling
     */
    public void deposit(double amount) throws BankAccountException {
        try {
            // Pre-transaction validations
            validateAccountActive();
            validateTransactionAmount(amount, "deposit");
            
            // Perform transaction
            double previousBalance = balance;
            balance += amount;
            
            // Log successful transaction
            String transaction = String.format("DEPOSIT: $%.2f (Balance: $%.2f → $%.2f)", 
                                             amount, previousBalance, balance);
            logTransaction(transaction);
            
            System.out.println("✅ Deposit successful: " + transaction);
            
        } catch (IllegalArgumentException e) {
            String error = "Deposit failed due to invalid amount";
            logTransaction("FAILED DEPOSIT: " + error + " - " + e.getMessage());
            throw new BankAccountException(error, accountNumber, "deposit", amount, e);
            
        } catch (IllegalStateException e) {
            String error = "Deposit failed due to account state";
            logTransaction("FAILED DEPOSIT: " + error + " - " + e.getMessage());
            throw new BankAccountException(error, accountNumber, "deposit", amount, e);
        }
    }
    
    /**
     * Withdraws money with overdraft protection and detailed exception handling
     */
    public void withdraw(double amount) throws BankAccountException, InsufficientFundsException {
        try {
            // Pre-transaction validations
            validateAccountActive();
            validateTransactionAmount(amount, "withdrawal");
            
            // Check for sufficient funds
            if (balance < amount) {
                throw new InsufficientFundsException(balance, amount, accountNumber);
            }
            
            // Perform transaction
            double previousBalance = balance;
            balance -= amount;
            
            // Log successful transaction
            String transaction = String.format("WITHDRAWAL: $%.2f (Balance: $%.2f → $%.2f)", 
                                             amount, previousBalance, balance);
            logTransaction(transaction);
            
            System.out.println("✅ Withdrawal successful: " + transaction);
            
        } catch (IllegalArgumentException e) {
            String error = "Withdrawal failed due to invalid amount";
            logTransaction("FAILED WITHDRAWAL: " + error + " - " + e.getMessage());
            throw new BankAccountException(error, accountNumber, "withdrawal", amount, e);
            
        } catch (IllegalStateException e) {
            String error = "Withdrawal failed due to account state";
            logTransaction("FAILED WITHDRAWAL: " + error + " - " + e.getMessage());
            throw new BankAccountException(error, accountNumber, "withdrawal", amount, e);
            
        } catch (InsufficientFundsException e) {
            // Log the insufficient funds attempt
            logTransaction("FAILED WITHDRAWAL: " + e.getMessage());
            throw e; // Re-throw the specific exception
        }
    }
    
    /**
     * Transfers money between accounts with comprehensive error handling
     */
    public void transfer(BankAccount destinationAccount, double amount) 
            throws BankAccountException, InsufficientFundsException {
        
        if (destinationAccount == null) {
            throw new BankAccountException("Transfer failed: destination account is null", 
                                         accountNumber, "transfer", amount);
        }
        
        if (destinationAccount == this) {
            throw new BankAccountException("Transfer failed: cannot transfer to same account", 
                                         accountNumber, "transfer", amount);
        }
        
        // Use a transaction-like approach
        double originalBalance = this.balance;
        
        try {
            // Step 1: Withdraw from source account
            this.withdraw(amount);
            
            try {
                // Step 2: Deposit to destination account
                destinationAccount.deposit(amount);
                
                // Log successful transfer
                String transferLog = String.format("TRANSFER: $%.2f to account %s", 
                                                 amount, destinationAccount.accountNumber);
                this.logTransaction(transferLog);
                destinationAccount.logTransaction("RECEIVED TRANSFER: $" + String.format("%.2f", amount) + 
                                                " from account " + this.accountNumber);
                
                System.out.println("✅ Transfer successful: " + transferLog);
                
            } catch (BankAccountException e) {
                // Rollback: restore source account balance
                this.balance = originalBalance;
                this.logTransaction("TRANSFER ROLLBACK: Restored balance due to destination deposit failure");
                
                throw new BankAccountException("Transfer failed during deposit to destination account", 
                                             accountNumber, "transfer", amount, e);
            }
            
        } catch (InsufficientFundsException | BankAccountException e) {
            // Transfer failed during withdrawal - no rollback needed
            String error = "Transfer failed during withdrawal from source account";
            this.logTransaction("FAILED TRANSFER: " + error);
            
            if (e instanceof InsufficientFundsException) {
                throw e; // Re-throw insufficient funds exception
            } else {
                throw new BankAccountException(error, accountNumber, "transfer", amount, e);
            }
        }
    }
    
    // Validation methods that throw specific exceptions
    private void validateAccountNumber(String accountNumber) throws BankAccountException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BankAccountException("Account number cannot be null or empty");
        }
        
        if (!accountNumber.matches("\\\\d{4}-\\\\d{4}-\\\\d{4}")) {
            throw new BankAccountException("Invalid account number format. Expected: XXXX-XXXX-XXXX");
        }
    }
    
    private void validateAccountHolderName(String name) throws BankAccountException {
        if (name == null || name.trim().isEmpty()) {
            throw new BankAccountException("Account holder name cannot be null or empty");
        }
        
        if (name.trim().length() < 2) {
            throw new BankAccountException("Account holder name must be at least 2 characters long");
        }
        
        if (!name.matches("[a-zA-Z\\\\s]+")) {
            throw new BankAccountException("Account holder name can only contain letters and spaces");
        }
    }
    
    private void validateInitialBalance(double balance) throws BankAccountException {
        if (balance < MINIMUM_BALANCE) {
            throw new BankAccountException("Initial balance cannot be less than $" + 
                                         String.format("%.2f", MINIMUM_BALANCE));
        }
        
        if (balance > MAXIMUM_TRANSACTION) {
            throw new BankAccountException("Initial balance cannot exceed $" + 
                                         String.format("%.2f", MAXIMUM_TRANSACTION));
        }
    }
    
    private void validateAccountActive() throws IllegalStateException {
        if (!isActive) {
            throw new IllegalStateException("Account is closed and cannot be used for transactions");
        }
    }
    
    private void validateTransactionAmount(double amount, String operation) throws IllegalArgumentException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive for " + operation);
        }
        
        if (amount > MAXIMUM_TRANSACTION) {
            throw new IllegalArgumentException("Transaction amount exceeds maximum limit of $" + 
                                             String.format("%.2f", MAXIMUM_TRANSACTION) + " for " + operation);
        }
        
        // Check for reasonable decimal places (no more than 2)
        if (amount * 100 != Math.floor(amount * 100)) {
            throw new IllegalArgumentException("Transaction amount cannot have more than 2 decimal places");
        }
    }
    
    private void logTransaction(String transaction) {
        String timestamp = LocalDateTime.now().toString();
        String logEntry = "[" + timestamp + "] " + transaction;
        transactionHistory.add(logEntry);
    }
    
    // Safe getters that don't throw exceptions
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public List<String> getTransactionHistory() {
        return new ArrayList<>(transactionHistory); // Return copy to prevent modification
    }
    
    public void printTransactionHistory() {
        System.out.println("📊 Transaction History for Account " + accountNumber + ":");
        System.out.println("Account Holder: " + accountHolderName);
        System.out.println("Current Balance: $" + String.format("%.2f", balance));
        System.out.println("Account Status: " + (isActive ? "Active" : "Closed"));
        System.out.println("-".repeat(60));
        
        if (transactionHistory.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            for (String transaction : transactionHistory) {
                System.out.println(transaction);
            }
        }
        System.out.println("-".repeat(60));
    }
    
    @Override
    public String toString() {
        return "BankAccount[" + accountNumber + ", " + accountHolderName + 
               ", Balance: $" + String.format("%.2f", balance) + 
               ", Status: " + (isActive ? "Active" : "Closed") + "]";
    }
}
```

---

## 💻 Part 4: Exception Handling Patterns and Best Practices (30 minutes)

### **Comprehensive Exception Handling Demo**

Create `ExceptionHandlingDemo.java`:

```java
/**
 * Comprehensive Exception Handling Demonstration
 * Shows all types of exceptions, handling patterns, and best practices
 */
public class ExceptionHandlingDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("🚨 COMPREHENSIVE EXCEPTION HANDLING DEMONSTRATION");
        System.out.println("=".repeat(70));
        
        // Part 1: Exception Handling Patterns
        demonstrateExceptionPatterns();
        
        System.out.println("\\n" + "=".repeat(70));
        
        // Part 2: Banking System with Exception Handling
        demonstrateBankingExceptions();
        
        System.out.println("\\n" + "=".repeat(70));
        
        // Part 3: Try-with-Resources
        demonstrateTryWithResources();
        
        System.out.println("\\n" + "=".repeat(70));
        System.out.println("🎉 EXCEPTION HANDLING DEMONSTRATION COMPLETE!");
        System.out.println("=".repeat(70));
    }
    
    public static void demonstrateExceptionPatterns() {
        System.out.println("🔄 EXCEPTION HANDLING PATTERNS");
        System.out.println("-".repeat(50));
        
        // Pattern 1: Fail Fast
        System.out.println("1. Fail Fast Pattern:");
        try {
            processUserData(null, "", -1);
        } catch (IllegalArgumentException e) {
            System.out.println("   ❌ Validation failed early: " + e.getMessage());
            System.out.println("   💡 Fail fast prevents deeper issues");
        }
        
        // Pattern 2: Exception Translation
        System.out.println("\\n2. Exception Translation:");
        try {
            performDatabaseOperation();
        } catch (DataAccessException e) {
            System.out.println("   ❌ Application exception: " + e.getMessage());
            System.out.println("   🔗 Original cause: " + e.getCause().getMessage());
        }
        
        // Pattern 3: Graceful Degradation
        System.out.println("\\n3. Graceful Degradation:");
        String result = getDataWithFallback();
        System.out.println("   ✅ Got data: " + result);
        
        // Pattern 4: Retry Pattern
        System.out.println("\\n4. Retry Pattern:");
        try {
            String retryResult = performWithRetry();
            System.out.println("   ✅ Operation succeeded: " + retryResult);
        } catch (Exception e) {
            System.out.println("   ❌ Operation failed after all retries: " + e.getMessage());
        }
    }
    
    private static void processUserData(String name, String email, int age) {
        // Fail fast - validate all inputs immediately
        if (name == null) throw new IllegalArgumentException("Name cannot be null");
        if (email.isEmpty()) throw new IllegalArgumentException("Email cannot be empty");
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
        
        System.out.println("   ✅ Processing user: " + name + ", " + email + ", " + age);
    }
    
    private static void performDatabaseOperation() throws DataAccessException {
        try {
            // Simulate low-level database exception
            throw new SQLException("Connection timeout");
        } catch (SQLException e) {
            // Translate to application-specific exception
            throw new DataAccessException("User data retrieval failed", e);
        }
    }
    
    private static String getDataWithFallback() {
        try {
            return getPrimaryData();
        } catch (Exception e) {
            System.out.println("   ⚠️ Primary data source failed, using fallback");
            return getCachedData();
        }
    }
    
    private static String getPrimaryData() throws Exception {
        throw new Exception("Primary service unavailable");
    }
    
    private static String getCachedData() {
        return "Cached data from local storage";
    }
    
    private static String performWithRetry() throws Exception {
        int maxRetries = 3;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return performUnreliableOperation(attempt);
            } catch (Exception e) {
                lastException = e;
                System.out.println("   ⚠️ Attempt " + attempt + " failed: " + e.getMessage());
                
                if (attempt < maxRetries) {
                    System.out.println("   🔄 Retrying...");
                    try {
                        Thread.sleep(100); // Small delay
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new Exception("Operation failed after " + maxRetries + " attempts", lastException);
    }
    
    private static String performUnreliableOperation(int attempt) throws Exception {
        if (attempt < 3) {
            throw new Exception("Simulated failure on attempt " + attempt);
        }
        return "Success on attempt " + attempt;
    }
    
    public static void demonstrateBankingExceptions() {
        System.out.println("🏦 BANKING SYSTEM EXCEPTION HANDLING");
        System.out.println("-".repeat(50));
        
        try {
            // Create accounts
            BankAccount account1 = new BankAccount("1234-5678-9012", "John Doe", 1000.0);
            BankAccount account2 = new BankAccount("9876-5432-1098", "Jane Smith", 500.0);
            
            System.out.println("✅ Accounts created successfully");
            
            // Successful operations
            account1.deposit(200.0);
            account1.withdraw(150.0);
            
            // Transfer money
            account1.transfer(account2, 300.0);
            
            // Try operations that will fail
            System.out.println("\\n🧪 Testing exception scenarios:");
            
            // Test insufficient funds
            try {
                account2.withdraw(2000.0);
            } catch (InsufficientFundsException e) {
                System.out.println("   ❌ " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("   💡 " + e.getSuggestion());
            }
            
            // Test invalid account creation
            try {
                BankAccount invalidAccount = new BankAccount("invalid", "Test User", 100.0);
            } catch (BankAccountException e) {
                System.out.println("   ❌ " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            
            // Show final account states
            System.out.println("\\n📊 Final Account States:");
            System.out.println("   " + account1);
            System.out.println("   " + account2);
            
        } catch (BankAccountException e) {
            System.out.println("❌ Account setup failed: " + e.getMessage());
        }
    }
    
    public static void demonstrateTryWithResources() {
        System.out.println("🛠️ TRY-WITH-RESOURCES DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        System.out.println("1. Traditional try-finally vs try-with-resources:");
        
        // Traditional approach (more verbose)
        System.out.println("   Traditional approach requires manual cleanup");
        
        // Try-with-resources (automatic cleanup)
        System.out.println("   Try-with-resources provides automatic cleanup:");
        
        // Simulated file operations
        try {
            System.out.println("   📁 File operations completed successfully");
        } catch (Exception e) {
            System.out.println("   ❌ File operation failed: " + e.getMessage());
        }
        // Resources automatically closed here
        System.out.println("   🧹 Resources closed automatically");
    }
    
    // Helper exception classes for demonstration
    static class DataAccessException extends Exception {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    static class SQLException extends Exception {
        public SQLException(String message) {
            super(message);
        }
    }
}
```

---

## 📝 Key Takeaways from Day 9

### **Exception Types:**
- ✅ **Checked Exceptions:** Must be handled or declared (IOException, SQLException)
- ✅ **Unchecked Exceptions:** Runtime exceptions (NullPointerException, IllegalArgumentException)
- ✅ **Error:** JVM errors that shouldn't be caught (OutOfMemoryError)
- ✅ **Custom Exceptions:** Domain-specific exceptions for business logic

### **Exception Handling Best Practices:**
- ✅ **Catch Specific Exceptions:** Avoid catching generic Exception
- ✅ **Don't Suppress Exceptions:** Always log or handle appropriately
- ✅ **Use Finally for Cleanup:** Ensure resources are released
- ✅ **Create Meaningful Messages:** Provide actionable error information
- ✅ **Exception Chaining:** Preserve original exception as cause

### **Advanced Patterns:**
- ✅ **Fail Fast:** Validate early and throw exceptions immediately
- ✅ **Exception Translation:** Convert low-level to application-specific exceptions
- ✅ **Graceful Degradation:** Provide fallback behavior when possible
- ✅ **Try-with-Resources:** Automatic resource management (Java 7+)
- ✅ **Retry Pattern:** Attempt operations multiple times with delays

### **Banking System Learnings:**
- ✅ **Validation:** Comprehensive input validation with meaningful exceptions
- ✅ **Transaction Safety:** Rollback mechanisms for failed operations
- ✅ **Logging:** Track all operations and failures for debugging
- ✅ **User Feedback:** Provide helpful suggestions for resolving issues

---

## 🎯 Today's Success Criteria

**Completed Tasks:**
- [ ] Handle multiple exception types with specific catch blocks
- [ ] Create and use custom exception classes with detailed information
- [ ] Implement proper resource cleanup with finally blocks
- [ ] Use try-with-resources for automatic resource management
- [ ] Build error-resistant banking application
- [ ] Demonstrate exception chaining and translation patterns
- [ ] Apply exception handling best practices throughout

**Bonus Achievements:**
- [ ] Implement retry patterns for unreliable operations
- [ ] Create exception hierarchy for different error types
- [ ] Add logging and monitoring for exception tracking
- [ ] Experiment with advanced exception handling patterns
- [ ] Build comprehensive error reporting system

---

## 🚀 Tomorrow's Preview (Day 10 - July 19)

**Focus:** Collections Framework Part 1
- List interface (ArrayList, LinkedList, Vector)
- Set interface (HashSet, TreeSet, LinkedHashSet)
- Iterator and enhanced for-loop patterns
- Building dynamic data structures for efficient data management

---

## 🎉 Excellent Progress!

You've mastered exception handling and built robust, error-resistant applications! Exception handling is crucial for building production-ready software. Tomorrow we'll dive into the Collections Framework for efficient data management.

**Keep building resilient applications!** 🚀