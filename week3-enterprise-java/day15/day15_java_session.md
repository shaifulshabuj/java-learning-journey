# Day 15: JDBC & Database Connectivity - Part 1 (July 24, 2025)

**Date:** July 24, 2025  
**Duration:** 2.5 hours  
**Focus:** Master database connection fundamentals and basic CRUD operations with JDBC

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand JDBC architecture and driver management
- ✅ Establish database connections securely
- ✅ Master PreparedStatement vs Statement differences
- ✅ Handle ResultSet navigation and data retrieval
- ✅ Implement complete CRUD operations
- ✅ Build a database-driven student management system

---

## 🏗️ Part 1: Database Setup and JDBC Architecture (30 minutes)

### **Understanding JDBC Architecture**

**JDBC (Java Database Connectivity)** is a Java API that allows Java applications to interact with relational databases.

**JDBC Components:**
- **Driver Manager:** Manages database drivers
- **Driver:** Database-specific implementation
- **Connection:** Represents a connection to the database
- **Statement:** Executes SQL statements
- **ResultSet:** Represents query results
- **SQLException:** Handles database errors

### **Database Setup and Connection**

Create `DatabaseSetupDemo.java`:

```java
import java.sql.*;
import java.util.Properties;

/**
 * Database setup and connection demonstration
 * Shows how to establish connections to different databases
 */
public class DatabaseSetupDemo {
    
    // Database connection constants
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/student_db";
    private static final String POSTGRESQL_URL = "jdbc:postgresql://localhost:5432/student_db";
    private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    
    private static final String USERNAME = "root";
    private static final String PASSWORD = "password";
    
    public static void main(String[] args) {
        System.out.println("=== Database Setup and Connection Demo ===\n");
        
        // Test different database connections
        testH2Connection();
        testMySQLConnection();
        testPostgreSQLConnection();
        
        // Demonstrate connection properties
        demonstrateConnectionProperties();
        
        // Show connection metadata
        demonstrateConnectionMetadata();
    }
    
    private static void testH2Connection() {
        System.out.println("1. Testing H2 Database Connection:");
        
        try {
            // Load H2 driver (optional in modern JDBC)
            Class.forName("org.h2.Driver");
            
            // Establish connection
            Connection connection = DriverManager.getConnection(H2_URL, "sa", "");
            
            System.out.println("✅ H2 connection successful!");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("Auto-commit: " + connection.getAutoCommit());
            System.out.println("Read-only: " + connection.isReadOnly());
            
            // Create a simple table for testing
            createStudentTable(connection);
            
            connection.close();
            System.out.println("✅ H2 connection closed successfully\n");
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ H2 Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ H2 Connection failed: " + e.getMessage());
        }
    }
    
    private static void testMySQLConnection() {
        System.out.println("2. Testing MySQL Database Connection:");
        
        try {
            // Modern approach - driver auto-loaded
            Connection connection = DriverManager.getConnection(MYSQL_URL, USERNAME, PASSWORD);
            
            System.out.println("✅ MySQL connection successful!");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("Connection URL: " + connection.getMetaData().getURL());
            
            connection.close();
            System.out.println("✅ MySQL connection closed successfully\n");
            
        } catch (SQLException e) {
            System.err.println("❌ MySQL Connection failed: " + e.getMessage());
            System.err.println("💡 Make sure MySQL is running and database 'student_db' exists\n");
        }
    }
    
    private static void testPostgreSQLConnection() {
        System.out.println("3. Testing PostgreSQL Database Connection:");
        
        try {
            Connection connection = DriverManager.getConnection(POSTGRESQL_URL, USERNAME, PASSWORD);
            
            System.out.println("✅ PostgreSQL connection successful!");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("Schema: " + connection.getSchema());
            
            connection.close();
            System.out.println("✅ PostgreSQL connection closed successfully\n");
            
        } catch (SQLException e) {
            System.err.println("❌ PostgreSQL Connection failed: " + e.getMessage());
            System.err.println("💡 Make sure PostgreSQL is running and database 'student_db' exists\n");
        }
    }
    
    private static void demonstrateConnectionProperties() {
        System.out.println("4. Connection Properties Demo:");
        
        try {
            // Use Properties for connection configuration
            Properties props = new Properties();
            props.setProperty("user", "sa");
            props.setProperty("password", "");
            props.setProperty("autoReconnect", "true");
            props.setProperty("characterEncoding", "UTF-8");
            
            Connection connection = DriverManager.getConnection(H2_URL, props);
            
            System.out.println("✅ Connection with properties successful!");
            System.out.println("Transaction isolation: " + connection.getTransactionIsolation());
            System.out.println("Network timeout: " + connection.getNetworkTimeout());
            
            connection.close();
            
        } catch (SQLException e) {
            System.err.println("❌ Connection with properties failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateConnectionMetadata() {
        System.out.println("5. Connection Metadata Demo:");
        
        try {
            Connection connection = DriverManager.getConnection(H2_URL, "sa", "");
            DatabaseMetaData metaData = connection.getMetaData();
            
            System.out.println("Database Product: " + metaData.getDatabaseProductName());
            System.out.println("Database Version: " + metaData.getDatabaseProductVersion());
            System.out.println("Driver Name: " + metaData.getDriverName());
            System.out.println("Driver Version: " + metaData.getDriverVersion());
            System.out.println("JDBC Version: " + metaData.getJDBCMajorVersion() + "." + metaData.getJDBCMinorVersion());
            System.out.println("Max Connections: " + metaData.getMaxConnections());
            System.out.println("Supports Transactions: " + metaData.supportsTransactions());
            System.out.println("Supports Batch Updates: " + metaData.supportsBatchUpdates());
            
            connection.close();
            
        } catch (SQLException e) {
            System.err.println("❌ Metadata retrieval failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void createStudentTable(Connection connection) {
        System.out.println("📋 Creating student table for testing...");
        
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS students (
                id INT PRIMARY KEY AUTO_INCREMENT,
                first_name VARCHAR(50) NOT NULL,
                last_name VARCHAR(50) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                age INT CHECK (age >= 16 AND age <= 100),
                gpa DECIMAL(3,2) CHECK (gpa >= 0.0 AND gpa <= 4.0),
                enrollment_date DATE DEFAULT CURRENT_DATE,
                is_active BOOLEAN DEFAULT TRUE
            )
            """;
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            System.out.println("✅ Student table created successfully");
        } catch (SQLException e) {
            System.err.println("❌ Table creation failed: " + e.getMessage());
        }
    }
}
```

---

## 📝 Part 2: Statement vs PreparedStatement (40 minutes)

### **Understanding Statement Types**

**Statement Types:**
- **Statement:** Basic SQL execution (vulnerable to SQL injection)
- **PreparedStatement:** Precompiled SQL with parameters (safe and efficient)
- **CallableStatement:** For stored procedures

### **Statement vs PreparedStatement Comparison**

Create `PreparedStatementDemo.java`:

```java
import java.sql.*;
import java.util.Scanner;

/**
 * Comprehensive demonstration of Statement vs PreparedStatement
 * Shows security, performance, and usability differences
 */
public class PreparedStatementDemo {
    
    private static final String DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    
    public static void main(String[] args) {
        System.out.println("=== Statement vs PreparedStatement Demo ===\n");
        
        try {
            // Initialize database
            initializeDatabase();
            
            // Demonstrate Statement vulnerabilities
            demonstrateStatementVulnerabilities();
            
            // Demonstrate PreparedStatement security
            demonstratePreparedStatementSecurity();
            
            // Performance comparison
            demonstratePerformanceComparison();
            
            // Advanced PreparedStatement features
            demonstrateAdvancedFeatures();
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
    
    private static void initializeDatabase() throws SQLException {
        System.out.println("🔧 Initializing database...");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Create students table
            String createTableSQL = """
                CREATE TABLE students (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    age INT CHECK (age >= 16 AND age <= 100),
                    gpa DECIMAL(3,2) CHECK (gpa >= 0.0 AND gpa <= 4.0),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
                System.out.println("✅ Database initialized successfully\n");
            }
            
            // Insert sample data
            insertSampleData(connection);
        }
    }
    
    private static void insertSampleData(Connection connection) throws SQLException {
        System.out.println("📝 Inserting sample data...");
        
        String insertSQL = "INSERT INTO students (first_name, last_name, email, age, gpa) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            
            // Sample student data
            Object[][] students = {
                {"John", "Doe", "john.doe@email.com", 20, 3.5},
                {"Jane", "Smith", "jane.smith@email.com", 19, 3.8},
                {"Bob", "Johnson", "bob.johnson@email.com", 21, 3.2},
                {"Alice", "Brown", "alice.brown@email.com", 22, 3.9},
                {"Charlie", "Davis", "charlie.davis@email.com", 20, 3.6}
            };
            
            for (Object[] student : students) {
                pstmt.setString(1, (String) student[0]);
                pstmt.setString(2, (String) student[1]);
                pstmt.setString(3, (String) student[2]);
                pstmt.setInt(4, (Integer) student[3]);
                pstmt.setDouble(5, (Double) student[4]);
                pstmt.executeUpdate();
            }
            
            System.out.println("✅ Sample data inserted successfully\n");
        }
    }
    
    private static void demonstrateStatementVulnerabilities() {
        System.out.println("⚠️  Statement Vulnerabilities Demo:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Vulnerable query using Statement
            String userInput = "'; DROP TABLE students; --";
            String vulnerableQuery = "SELECT * FROM students WHERE first_name = '" + userInput + "'";
            
            System.out.println("🔓 Vulnerable query: " + vulnerableQuery);
            
            try (Statement statement = connection.createStatement()) {
                // This would be dangerous in a real application
                // ResultSet rs = statement.executeQuery(vulnerableQuery);
                
                System.out.println("❌ This query is vulnerable to SQL injection!");
                System.out.println("💡 Input: " + userInput);
                System.out.println("💡 Could potentially drop the entire table!\n");
            }
            
            // Performance issue with Statement
            demonstrateStatementPerformanceIssue(connection);
            
        } catch (SQLException e) {
            System.err.println("❌ Statement demonstration failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateStatementPerformanceIssue(Connection connection) throws SQLException {
        System.out.println("🐌 Statement Performance Issue:");
        
        long startTime = System.currentTimeMillis();
        
        try (Statement statement = connection.createStatement()) {
            // Each query is parsed and compiled separately
            for (int i = 0; i < 100; i++) {
                String query = "SELECT * FROM students WHERE id = " + (i % 5 + 1);
                try (ResultSet rs = statement.executeQuery(query)) {
                    while (rs.next()) {
                        // Process results
                    }
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("⏱️  Statement execution time: " + (endTime - startTime) + " ms");
        System.out.println("💡 Each query is parsed and compiled separately\n");
    }
    
    private static void demonstratePreparedStatementSecurity() {
        System.out.println("🔒 PreparedStatement Security Demo:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Safe query using PreparedStatement
            String safeQuery = "SELECT * FROM students WHERE first_name = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(safeQuery)) {
                
                // Malicious input is safely handled
                String maliciousInput = "'; DROP TABLE students; --";
                pstmt.setString(1, maliciousInput);
                
                System.out.println("🔐 Safe query: " + safeQuery);
                System.out.println("🔐 Malicious input safely handled: " + maliciousInput);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                    }
                    System.out.println("✅ Query executed safely, results: " + count + " rows");
                    System.out.println("✅ Table still exists and data is safe!\n");
                }
            }
            
            // Performance advantage of PreparedStatement
            demonstratePreparedStatementPerformance(connection);
            
        } catch (SQLException e) {
            System.err.println("❌ PreparedStatement demonstration failed: " + e.getMessage());
        }
    }
    
    private static void demonstratePreparedStatementPerformance(Connection connection) throws SQLException {
        System.out.println("🚀 PreparedStatement Performance Advantage:");
        
        long startTime = System.currentTimeMillis();
        
        // Query is parsed and compiled once
        String query = "SELECT * FROM students WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            
            for (int i = 0; i < 100; i++) {
                pstmt.setInt(1, i % 5 + 1);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        // Process results
                    }
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("⏱️  PreparedStatement execution time: " + (endTime - startTime) + " ms");
        System.out.println("✅ Query is parsed and compiled once, then reused\n");
    }
    
    private static void demonstrateAdvancedFeatures() {
        System.out.println("🔧 Advanced PreparedStatement Features:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Parameter metadata
            demonstrateParameterMetadata(connection);
            
            // Batch operations
            demonstrateBatchOperations(connection);
            
            // Generated keys
            demonstrateGeneratedKeys(connection);
            
        } catch (SQLException e) {
            System.err.println("❌ Advanced features demonstration failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateParameterMetadata(Connection connection) throws SQLException {
        System.out.println("📋 Parameter Metadata:");
        
        String query = "SELECT * FROM students WHERE age > ? AND gpa > ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            
            ParameterMetaData paramMetaData = pstmt.getParameterMetaData();
            
            System.out.println("Parameter count: " + paramMetaData.getParameterCount());
            for (int i = 1; i <= paramMetaData.getParameterCount(); i++) {
                System.out.println("Parameter " + i + ":");
                System.out.println("  Type: " + paramMetaData.getParameterTypeName(i));
                System.out.println("  Mode: " + paramMetaData.getParameterMode(i));
            }
            
            System.out.println();
        }
    }
    
    private static void demonstrateBatchOperations(Connection connection) throws SQLException {
        System.out.println("⚡ Batch Operations:");
        
        String insertSQL = "INSERT INTO students (first_name, last_name, email, age, gpa) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            
            // Add multiple operations to batch
            String[][] batchData = {
                {"Mike", "Wilson", "mike.wilson@email.com", "23", "3.4"},
                {"Sarah", "Taylor", "sarah.taylor@email.com", "19", "3.7"},
                {"David", "Anderson", "david.anderson@email.com", "21", "3.1"}
            };
            
            for (String[] student : batchData) {
                pstmt.setString(1, student[0]);
                pstmt.setString(2, student[1]);
                pstmt.setString(3, student[2]);
                pstmt.setInt(4, Integer.parseInt(student[3]));
                pstmt.setDouble(5, Double.parseDouble(student[4]));
                pstmt.addBatch();
            }
            
            // Execute batch
            int[] results = pstmt.executeBatch();
            
            System.out.println("✅ Batch executed successfully!");
            System.out.println("Affected rows: " + results.length);
            System.out.println();
        }
    }
    
    private static void demonstrateGeneratedKeys(Connection connection) throws SQLException {
        System.out.println("🔑 Generated Keys:");
        
        String insertSQL = "INSERT INTO students (first_name, last_name, email, age, gpa) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, "Emma");
            pstmt.setString(2, "Martinez");
            pstmt.setString(3, "emma.martinez@email.com");
            pstmt.setInt(4, 20);
            pstmt.setDouble(5, 3.8);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        System.out.println("✅ Student inserted with ID: " + generatedId);
                    }
                }
            }
            
            System.out.println();
        }
    }
}
```

---

## 🔍 Part 3: ResultSet Navigation and Data Retrieval (40 minutes)

### **Understanding ResultSet**

**ResultSet Features:**
- **Forward-only:** Default navigation (next() only)
- **Scrollable:** Can move forward and backward
- **Updatable:** Can modify data directly
- **Sensitive:** Reflects database changes

### **ResultSet Navigation Demonstration**

Create `ResultSetNavigationDemo.java`:

```java
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive ResultSet navigation and data retrieval demonstration
 * Shows different ResultSet types and navigation patterns
 */
public class ResultSetNavigationDemo {
    
    private static final String DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    
    public static void main(String[] args) {
        System.out.println("=== ResultSet Navigation Demo ===\n");
        
        try {
            // Initialize database with sample data
            initializeDatabase();
            
            // Demonstrate basic ResultSet navigation
            demonstrateBasicNavigation();
            
            // Demonstrate scrollable ResultSet
            demonstrateScrollableResultSet();
            
            // Demonstrate updatable ResultSet
            demonstrateUpdatableResultSet();
            
            // Demonstrate data type retrieval
            demonstrateDataTypeRetrieval();
            
            // Demonstrate metadata usage
            demonstrateResultSetMetadata();
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
    
    private static void initializeDatabase() throws SQLException {
        System.out.println("🔧 Initializing database with sample data...");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Create students table
            String createTableSQL = """
                CREATE TABLE students (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    age INT CHECK (age >= 16 AND age <= 100),
                    gpa DECIMAL(3,2) CHECK (gpa >= 0.0 AND gpa <= 4.0),
                    enrollment_date DATE,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
            }
            
            // Insert comprehensive sample data
            insertSampleData(connection);
            
            System.out.println("✅ Database initialized successfully\n");
        }
    }
    
    private static void insertSampleData(Connection connection) throws SQLException {
        String insertSQL = """
            INSERT INTO students (first_name, last_name, email, age, gpa, enrollment_date, is_active) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            
            // Comprehensive sample data
            Object[][] students = {
                {"John", "Doe", "john.doe@email.com", 20, 3.5, "2024-01-15", true},
                {"Jane", "Smith", "jane.smith@email.com", 19, 3.8, "2024-01-16", true},
                {"Bob", "Johnson", "bob.johnson@email.com", 21, 3.2, "2024-01-17", false},
                {"Alice", "Brown", "alice.brown@email.com", 22, 3.9, "2024-01-18", true},
                {"Charlie", "Davis", "charlie.davis@email.com", 20, 3.6, "2024-01-19", true},
                {"Diana", "Wilson", "diana.wilson@email.com", 23, 3.4, "2024-01-20", false},
                {"Edward", "Taylor", "edward.taylor@email.com", 19, 3.7, "2024-01-21", true},
                {"Fiona", "Anderson", "fiona.anderson@email.com", 21, 3.1, "2024-01-22", true}
            };
            
            for (Object[] student : students) {
                pstmt.setString(1, (String) student[0]);
                pstmt.setString(2, (String) student[1]);
                pstmt.setString(3, (String) student[2]);
                pstmt.setInt(4, (Integer) student[3]);
                pstmt.setDouble(5, (Double) student[4]);
                pstmt.setDate(6, Date.valueOf((String) student[5]));
                pstmt.setBoolean(7, (Boolean) student[6]);
                pstmt.executeUpdate();
            }
        }
    }
    
    private static void demonstrateBasicNavigation() {
        System.out.println("🧭 Basic ResultSet Navigation:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            String query = "SELECT id, first_name, last_name, email, gpa FROM students ORDER BY gpa DESC";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {
                
                System.out.println("📊 Students ordered by GPA (descending):");
                System.out.println("-".repeat(70));
                System.out.printf("%-5s %-15s %-15s %-25s %-5s%n", "ID", "First Name", "Last Name", "Email", "GPA");
                System.out.println("-".repeat(70));
                
                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                    int id = rs.getInt("id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String email = rs.getString("email");
                    double gpa = rs.getDouble("gpa");
                    
                    System.out.printf("%-5d %-15s %-15s %-25s %-5.2f%n", 
                                    id, firstName, lastName, email, gpa);
                }
                
                System.out.println("-".repeat(70));
                System.out.println("Total rows: " + rowCount);
                System.out.println();
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Basic navigation failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateScrollableResultSet() {
        System.out.println("🔄 Scrollable ResultSet Navigation:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            String query = "SELECT id, first_name, last_name, gpa FROM students";
            
            // Create scrollable ResultSet
            try (PreparedStatement pstmt = connection.prepareStatement(query, 
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                 ResultSet rs = pstmt.executeQuery()) {
                
                System.out.println("🔍 Scrollable ResultSet capabilities:");
                
                // Move to last row
                if (rs.last()) {
                    System.out.println("📍 Last row: " + rs.getString("first_name") + " " + rs.getString("last_name"));
                    System.out.println("📊 Total rows: " + rs.getRow());
                }
                
                // Move to first row
                if (rs.first()) {
                    System.out.println("📍 First row: " + rs.getString("first_name") + " " + rs.getString("last_name"));
                }
                
                // Move to specific row
                if (rs.absolute(3)) {
                    System.out.println("📍 Row 3: " + rs.getString("first_name") + " " + rs.getString("last_name"));
                }
                
                // Navigate relatively
                if (rs.relative(2)) {
                    System.out.println("📍 2 rows forward: " + rs.getString("first_name") + " " + rs.getString("last_name"));
                }
                
                // Navigate backward
                if (rs.relative(-1)) {
                    System.out.println("📍 1 row back: " + rs.getString("first_name") + " " + rs.getString("last_name"));
                }
                
                // Move to before first
                rs.beforeFirst();
                System.out.println("📍 Moved to before first");
                
                // Iterate through all rows
                System.out.println("\n📋 Forward iteration:");
                while (rs.next()) {
                    System.out.println("  Row " + rs.getRow() + ": " + rs.getString("first_name") + " " + rs.getString("last_name"));
                }
                
                // Iterate backward
                System.out.println("\n📋 Backward iteration:");
                while (rs.previous()) {
                    System.out.println("  Row " + rs.getRow() + ": " + rs.getString("first_name") + " " + rs.getString("last_name"));
                }
                
                System.out.println();
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Scrollable ResultSet failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateUpdatableResultSet() {
        System.out.println("✏️ Updatable ResultSet:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            String query = "SELECT id, first_name, last_name, gpa FROM students WHERE id <= 3";
            
            // Create updatable ResultSet
            try (PreparedStatement pstmt = connection.prepareStatement(query,
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                 ResultSet rs = pstmt.executeQuery()) {
                
                System.out.println("📝 Original data:");
                displayResultSet(rs);
                
                // Update existing row
                rs.first();
                rs.updateDouble("gpa", 3.95);
                rs.updateRow();
                System.out.println("✅ Updated first row's GPA to 3.95");
                
                // Insert new row
                rs.moveToInsertRow();
                rs.updateString("first_name", "New");
                rs.updateString("last_name", "Student");
                rs.updateDouble("gpa", 4.0);
                rs.insertRow();
                rs.moveToCurrentRow();
                System.out.println("✅ Inserted new student");
                
                // Show updated data
                System.out.println("\n📝 Updated data:");
                rs.beforeFirst();
                displayResultSet(rs);
                
                System.out.println();
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Updatable ResultSet failed: " + e.getMessage());
        }
    }
    
    private static void displayResultSet(ResultSet rs) throws SQLException {
        System.out.println("-".repeat(50));
        System.out.printf("%-5s %-15s %-15s %-5s%n", "ID", "First Name", "Last Name", "GPA");
        System.out.println("-".repeat(50));
        
        while (rs.next()) {
            System.out.printf("%-5d %-15s %-15s %-5.2f%n",
                            rs.getInt("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getDouble("gpa"));
        }
        System.out.println("-".repeat(50));
    }
    
    private static void demonstrateDataTypeRetrieval() {
        System.out.println("📊 Data Type Retrieval:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            String query = "SELECT * FROM students WHERE id = 1";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {
                
                if (rs.next()) {
                    System.out.println("🔍 Retrieved student data:");
                    
                    // Different ways to retrieve data
                    System.out.println("ID (int): " + rs.getInt(1));
                    System.out.println("ID (by name): " + rs.getInt("id"));
                    System.out.println("First Name: " + rs.getString("first_name"));
                    System.out.println("Last Name: " + rs.getString("last_name"));
                    System.out.println("Email: " + rs.getString("email"));
                    System.out.println("Age: " + rs.getInt("age"));
                    System.out.println("GPA: " + rs.getDouble("gpa"));
                    System.out.println("Enrollment Date: " + rs.getDate("enrollment_date"));
                    System.out.println("Is Active: " + rs.getBoolean("is_active"));
                    System.out.println("Created At: " + rs.getTimestamp("created_at"));
                    
                    // Handle null values
                    String email = rs.getString("email");
                    if (rs.wasNull()) {
                        System.out.println("Email was null");
                    }
                    
                    // Convert to different types
                    System.out.println("\n🔄 Type conversions:");
                    System.out.println("Age as String: " + rs.getString("age"));
                    System.out.println("GPA as String: " + rs.getString("gpa"));
                    System.out.println("Is Active as String: " + rs.getString("is_active"));
                }
                
                System.out.println();
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Data type retrieval failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateResultSetMetadata() {
        System.out.println("📋 ResultSet Metadata:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            String query = "SELECT * FROM students LIMIT 1";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {
                
                ResultSetMetaData metaData = rs.getMetaData();
                
                System.out.println("📊 Column information:");
                int columnCount = metaData.getColumnCount();
                System.out.println("Column count: " + columnCount);
                System.out.println();
                
                for (int i = 1; i <= columnCount; i++) {
                    System.out.println("Column " + i + ":");
                    System.out.println("  Name: " + metaData.getColumnName(i));
                    System.out.println("  Label: " + metaData.getColumnLabel(i));
                    System.out.println("  Type: " + metaData.getColumnTypeName(i));
                    System.out.println("  Java Type: " + metaData.getColumnClassName(i));
                    System.out.println("  Size: " + metaData.getColumnDisplaySize(i));
                    System.out.println("  Precision: " + metaData.getPrecision(i));
                    System.out.println("  Scale: " + metaData.getScale(i));
                    System.out.println("  Nullable: " + (metaData.isNullable(i) == ResultSetMetaData.columnNullable));
                    System.out.println("  Auto Increment: " + metaData.isAutoIncrement(i));
                    System.out.println();
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Metadata retrieval failed: " + e.getMessage());
        }
    }
}
```

---

## 🔧 Part 4: Complete CRUD Operations (40 minutes)

### **CRUD Operations Implementation**

Create `BasicCRUDOperations.java`:

```java
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Complete CRUD (Create, Read, Update, Delete) operations demonstration
 * Shows professional database interaction patterns
 */
public class BasicCRUDOperations {
    
    private static final String DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    
    // Student data class
    static class Student {
        private int id;
        private String firstName;
        private String lastName;
        private String email;
        private int age;
        private double gpa;
        private LocalDate enrollmentDate;
        private boolean isActive;
        
        // Constructors
        public Student() {}
        
        public Student(String firstName, String lastName, String email, int age, double gpa, LocalDate enrollmentDate, boolean isActive) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.age = age;
            this.gpa = gpa;
            this.enrollmentDate = enrollmentDate;
            this.isActive = isActive;
        }
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public double getGpa() { return gpa; }
        public void setGpa(double gpa) { this.gpa = gpa; }
        
        public LocalDate getEnrollmentDate() { return enrollmentDate; }
        public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
        
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        
        @Override
        public String toString() {
            return String.format("Student{id=%d, firstName='%s', lastName='%s', email='%s', age=%d, gpa=%.2f, enrollmentDate=%s, isActive=%s}",
                               id, firstName, lastName, email, age, gpa, enrollmentDate, isActive);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Complete CRUD Operations Demo ===\n");
        
        try {
            // Initialize database
            initializeDatabase();
            
            // Demonstrate all CRUD operations
            demonstrateCreateOperations();
            demonstrateReadOperations();
            demonstrateUpdateOperations();
            demonstrateDeleteOperations();
            
            // Advanced operations
            demonstrateAdvancedQueries();
            demonstrateTransactionalOperations();
            
        } catch (SQLException e) {
            System.err.println("❌ CRUD operations failed: " + e.getMessage());
        }
    }
    
    private static void initializeDatabase() throws SQLException {
        System.out.println("🔧 Initializing database...");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            String createTableSQL = """
                CREATE TABLE students (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    age INT CHECK (age >= 16 AND age <= 100),
                    gpa DECIMAL(3,2) CHECK (gpa >= 0.0 AND gpa <= 4.0),
                    enrollment_date DATE,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
                System.out.println("✅ Database initialized successfully\n");
            }
        }
    }
    
    private static void demonstrateCreateOperations() throws SQLException {
        System.out.println("➕ CREATE Operations:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Single insert
            Student newStudent = new Student("John", "Doe", "john.doe@email.com", 20, 3.5, LocalDate.of(2024, 1, 15), true);
            int insertedId = insertStudent(connection, newStudent);
            System.out.println("✅ Inserted student with ID: " + insertedId);
            
            // Multiple inserts
            List<Student> students = List.of(
                new Student("Jane", "Smith", "jane.smith@email.com", 19, 3.8, LocalDate.of(2024, 1, 16), true),
                new Student("Bob", "Johnson", "bob.johnson@email.com", 21, 3.2, LocalDate.of(2024, 1, 17), true),
                new Student("Alice", "Brown", "alice.brown@email.com", 22, 3.9, LocalDate.of(2024, 1, 18), true)
            );
            
            int[] insertedIds = insertStudents(connection, students);
            System.out.println("✅ Inserted " + insertedIds.length + " students");
            
            // Bulk insert with transaction
            bulkInsertStudents(connection);
            
            System.out.println();
        }
    }
    
    private static int insertStudent(Connection connection, Student student) throws SQLException {
        String insertSQL = """
            INSERT INTO students (first_name, last_name, email, age, gpa, enrollment_date, is_active) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, student.getFirstName());
            pstmt.setString(2, student.getLastName());
            pstmt.setString(3, student.getEmail());
            pstmt.setInt(4, student.getAge());
            pstmt.setDouble(5, student.getGpa());
            pstmt.setDate(6, Date.valueOf(student.getEnrollmentDate()));
            pstmt.setBoolean(7, student.isActive());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
            
            throw new SQLException("Failed to insert student, no ID generated");
        }
    }
    
    private static int[] insertStudents(Connection connection, List<Student> students) throws SQLException {
        String insertSQL = """
            INSERT INTO students (first_name, last_name, email, age, gpa, enrollment_date, is_active) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            
            for (Student student : students) {
                pstmt.setString(1, student.getFirstName());
                pstmt.setString(2, student.getLastName());
                pstmt.setString(3, student.getEmail());
                pstmt.setInt(4, student.getAge());
                pstmt.setDouble(5, student.getGpa());
                pstmt.setDate(6, Date.valueOf(student.getEnrollmentDate()));
                pstmt.setBoolean(7, student.isActive());
                pstmt.addBatch();
            }
            
            return pstmt.executeBatch();
        }
    }
    
    private static void bulkInsertStudents(Connection connection) throws SQLException {
        System.out.println("📦 Bulk insert with transaction:");
        
        connection.setAutoCommit(false);
        
        try {
            String insertSQL = """
                INSERT INTO students (first_name, last_name, email, age, gpa, enrollment_date, is_active) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                
                // Generate bulk data
                for (int i = 1; i <= 100; i++) {
                    pstmt.setString(1, "Student" + i);
                    pstmt.setString(2, "LastName" + i);
                    pstmt.setString(3, "student" + i + "@email.com");
                    pstmt.setInt(4, 18 + (i % 10));
                    pstmt.setDouble(5, 2.0 + (i % 21) * 0.1);
                    pstmt.setDate(6, Date.valueOf(LocalDate.of(2024, 1, 1).plusDays(i)));
                    pstmt.setBoolean(7, i % 10 != 0);
                    pstmt.addBatch();
                    
                    // Execute batch every 50 records
                    if (i % 50 == 0) {
                        pstmt.executeBatch();
                    }
                }
                
                // Execute remaining
                pstmt.executeBatch();
            }
            
            connection.commit();
            System.out.println("✅ Bulk insert completed successfully");
            
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("❌ Bulk insert failed, rolled back: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    private static void demonstrateReadOperations() throws SQLException {
        System.out.println("📖 READ Operations:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Find by ID
            Optional<Student> student = findStudentById(connection, 1);
            if (student.isPresent()) {
                System.out.println("🔍 Student found by ID: " + student.get());
            }
            
            // Find by email
            Optional<Student> studentByEmail = findStudentByEmail(connection, "jane.smith@email.com");
            if (studentByEmail.isPresent()) {
                System.out.println("🔍 Student found by email: " + studentByEmail.get());
            }
            
            // Find all students
            List<Student> allStudents = findAllStudents(connection);
            System.out.println("📚 Total students: " + allStudents.size());
            
            // Find with conditions
            List<Student> activeStudents = findActiveStudents(connection);
            System.out.println("✅ Active students: " + activeStudents.size());
            
            // Find with pagination
            List<Student> pagedStudents = findStudentsWithPagination(connection, 1, 10);
            System.out.println("📄 Page 1 (10 records): " + pagedStudents.size());
            
            // Complex queries
            List<Student> topStudents = findTopStudentsByGPA(connection, 5);
            System.out.println("🏆 Top 5 students by GPA: " + topStudents.size());
            
            System.out.println();
        }
    }
    
    private static Optional<Student> findStudentById(Connection connection, int id) throws SQLException {
        String query = "SELECT * FROM students WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    private static Optional<Student> findStudentByEmail(Connection connection, String email) throws SQLException {
        String query = "SELECT * FROM students WHERE email = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, email);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    private static List<Student> findAllStudents(Connection connection) throws SQLException {
        String query = "SELECT * FROM students ORDER BY id";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            List<Student> students = new ArrayList<>();
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
            
            return students;
        }
    }
    
    private static List<Student> findActiveStudents(Connection connection) throws SQLException {
        String query = "SELECT * FROM students WHERE is_active = true ORDER BY gpa DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            List<Student> students = new ArrayList<>();
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
            
            return students;
        }
    }
    
    private static List<Student> findStudentsWithPagination(Connection connection, int page, int size) throws SQLException {
        String query = "SELECT * FROM students ORDER BY id LIMIT ? OFFSET ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, size);
            pstmt.setInt(2, (page - 1) * size);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Student> students = new ArrayList<>();
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
                
                return students;
            }
        }
    }
    
    private static List<Student> findTopStudentsByGPA(Connection connection, int limit) throws SQLException {
        String query = "SELECT * FROM students WHERE is_active = true ORDER BY gpa DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Student> students = new ArrayList<>();
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
                
                return students;
            }
        }
    }
    
    private static Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId(rs.getInt("id"));
        student.setFirstName(rs.getString("first_name"));
        student.setLastName(rs.getString("last_name"));
        student.setEmail(rs.getString("email"));
        student.setAge(rs.getInt("age"));
        student.setGpa(rs.getDouble("gpa"));
        
        Date enrollmentDate = rs.getDate("enrollment_date");
        if (enrollmentDate != null) {
            student.setEnrollmentDate(enrollmentDate.toLocalDate());
        }
        
        student.setActive(rs.getBoolean("is_active"));
        
        return student;
    }
    
    private static void demonstrateUpdateOperations() throws SQLException {
        System.out.println("✏️ UPDATE Operations:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Update single student
            boolean updated = updateStudentGPA(connection, 1, 3.9);
            System.out.println("✅ Student GPA updated: " + updated);
            
            // Update multiple students
            int updatedCount = updateStudentStatusByAge(connection, 25, false);
            System.out.println("✅ Updated " + updatedCount + " students to inactive");
            
            // Conditional update
            int conditionalUpdates = updateGPAForActiveStudents(connection, 0.1);
            System.out.println("✅ Boosted GPA for " + conditionalUpdates + " active students");
            
            System.out.println();
        }
    }
    
    private static boolean updateStudentGPA(Connection connection, int studentId, double newGPA) throws SQLException {
        String updateSQL = "UPDATE students SET gpa = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setDouble(1, newGPA);
            pstmt.setInt(2, studentId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    private static int updateStudentStatusByAge(Connection connection, int minAge, boolean isActive) throws SQLException {
        String updateSQL = "UPDATE students SET is_active = ? WHERE age >= ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setBoolean(1, isActive);
            pstmt.setInt(2, minAge);
            
            return pstmt.executeUpdate();
        }
    }
    
    private static int updateGPAForActiveStudents(Connection connection, double gpaBoost) throws SQLException {
        String updateSQL = "UPDATE students SET gpa = gpa + ? WHERE is_active = true AND gpa + ? <= 4.0";
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setDouble(1, gpaBoost);
            pstmt.setDouble(2, gpaBoost);
            
            return pstmt.executeUpdate();
        }
    }
    
    private static void demonstrateDeleteOperations() throws SQLException {
        System.out.println("🗑️ DELETE Operations:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Delete by ID
            boolean deleted = deleteStudentById(connection, 50);
            System.out.println("✅ Student deleted by ID: " + deleted);
            
            // Delete by condition
            int deletedCount = deleteInactiveStudents(connection);
            System.out.println("✅ Deleted " + deletedCount + " inactive students");
            
            // Conditional delete with limit
            int limitedDeletes = deleteStudentsWithLowGPA(connection, 2.0, 10);
            System.out.println("✅ Deleted " + limitedDeletes + " students with low GPA");
            
            System.out.println();
        }
    }
    
    private static boolean deleteStudentById(Connection connection, int studentId) throws SQLException {
        String deleteSQL = "DELETE FROM students WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setInt(1, studentId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    private static int deleteInactiveStudents(Connection connection) throws SQLException {
        String deleteSQL = "DELETE FROM students WHERE is_active = false";
        
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            return pstmt.executeUpdate();
        }
    }
    
    private static int deleteStudentsWithLowGPA(Connection connection, double maxGPA, int limit) throws SQLException {
        // First, find IDs to delete
        String selectSQL = "SELECT id FROM students WHERE gpa < ? LIMIT ?";
        List<Integer> idsToDelete = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setDouble(1, maxGPA);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    idsToDelete.add(rs.getInt("id"));
                }
            }
        }
        
        // Delete by IDs
        if (idsToDelete.isEmpty()) {
            return 0;
        }
        
        String deleteSQL = "DELETE FROM students WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            for (int id : idsToDelete) {
                pstmt.setInt(1, id);
                pstmt.addBatch();
            }
            
            int[] results = pstmt.executeBatch();
            return results.length;
        }
    }
    
    private static void demonstrateAdvancedQueries() throws SQLException {
        System.out.println("🔍 Advanced Query Operations:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Aggregate functions
            demonstrateAggregateQueries(connection);
            
            // Group by queries
            demonstrateGroupByQueries(connection);
            
            // Subqueries
            demonstrateSubqueries(connection);
            
            System.out.println();
        }
    }
    
    private static void demonstrateAggregateQueries(Connection connection) throws SQLException {
        System.out.println("📊 Aggregate Queries:");
        
        String[] queries = {
            "SELECT COUNT(*) as total_students FROM students",
            "SELECT AVG(gpa) as average_gpa FROM students WHERE is_active = true",
            "SELECT MAX(gpa) as max_gpa, MIN(gpa) as min_gpa FROM students",
            "SELECT SUM(CASE WHEN is_active THEN 1 ELSE 0 END) as active_count FROM students"
        };
        
        for (String query : queries) {
            try (PreparedStatement pstmt = connection.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {
                
                if (rs.next()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    System.out.print("  ");
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(metaData.getColumnName(i) + ": " + rs.getString(i));
                        if (i < columnCount) System.out.print(", ");
                    }
                    System.out.println();
                }
            }
        }
    }
    
    private static void demonstrateGroupByQueries(Connection connection) throws SQLException {
        System.out.println("\n📈 Group By Queries:");
        
        String query = """
            SELECT age, COUNT(*) as student_count, AVG(gpa) as avg_gpa
            FROM students 
            WHERE is_active = true 
            GROUP BY age 
            ORDER BY age
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.println("  Age | Count | Avg GPA");
            System.out.println("  ----|-------|--------");
            
            while (rs.next()) {
                System.out.printf("  %2d  |  %2d   | %5.2f%n", 
                                rs.getInt("age"), 
                                rs.getInt("student_count"), 
                                rs.getDouble("avg_gpa"));
            }
        }
    }
    
    private static void demonstrateSubqueries(Connection connection) throws SQLException {
        System.out.println("\n🔍 Subquery Examples:");
        
        String query = """
            SELECT first_name, last_name, gpa
            FROM students
            WHERE gpa > (SELECT AVG(gpa) FROM students WHERE is_active = true)
            AND is_active = true
            ORDER BY gpa DESC
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.println("  Students with above-average GPA:");
            while (rs.next()) {
                System.out.printf("  %s %s (%.2f)%n", 
                                rs.getString("first_name"), 
                                rs.getString("last_name"), 
                                rs.getDouble("gpa"));
            }
        }
    }
    
    private static void demonstrateTransactionalOperations() throws SQLException {
        System.out.println("🔄 Transactional Operations:");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            // Demonstrate successful transaction
            demonstrateSuccessfulTransaction(connection);
            
            // Demonstrate rollback transaction
            demonstrateRollbackTransaction(connection);
            
            System.out.println();
        }
    }
    
    private static void demonstrateSuccessfulTransaction(Connection connection) throws SQLException {
        System.out.println("✅ Successful Transaction:");
        
        connection.setAutoCommit(false);
        
        try {
            // Insert a new student
            String insertSQL = "INSERT INTO students (first_name, last_name, email, age, gpa, enrollment_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, "Transaction");
                pstmt.setString(2, "Student");
                pstmt.setString(3, "transaction@email.com");
                pstmt.setInt(4, 20);
                pstmt.setDouble(5, 3.5);
                pstmt.setDate(6, Date.valueOf(LocalDate.now()));
                pstmt.setBoolean(7, true);
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            System.out.println("  Inserted student with ID: " + newId);
                        }
                    }
                }
            }
            
            // Update another student
            String updateSQL = "UPDATE students SET gpa = gpa + 0.1 WHERE id = 1";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
                int rowsAffected = pstmt.executeUpdate();
                System.out.println("  Updated " + rowsAffected + " student(s)");
            }
            
            // Commit transaction
            connection.commit();
            System.out.println("  ✅ Transaction committed successfully");
            
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("  ❌ Transaction failed, rolled back: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    private static void demonstrateRollbackTransaction(Connection connection) throws SQLException {
        System.out.println("🔄 Rollback Transaction:");
        
        connection.setAutoCommit(false);
        
        try {
            // This will succeed
            String updateSQL = "UPDATE students SET gpa = gpa + 0.1 WHERE id = 1";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
                int rowsAffected = pstmt.executeUpdate();
                System.out.println("  Updated " + rowsAffected + " student(s)");
            }
            
            // This will fail (duplicate email)
            String insertSQL = "INSERT INTO students (first_name, last_name, email, age, gpa, enrollment_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                pstmt.setString(1, "Duplicate");
                pstmt.setString(2, "Email");
                pstmt.setString(3, "john.doe@email.com"); // This email already exists
                pstmt.setInt(4, 20);
                pstmt.setDouble(5, 3.5);
                pstmt.setDate(6, Date.valueOf(LocalDate.now()));
                pstmt.setBoolean(7, true);
                
                pstmt.executeUpdate();
            }
            
            // This won't be reached
            connection.commit();
            
        } catch (SQLException e) {
            connection.rollback();
            System.out.println("  🔄 Transaction rolled back due to error: " + e.getMessage());
            System.out.println("  ✅ All changes reverted successfully");
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
```

---

## 🏫 Part 5: Student Database Management System (30 minutes)

### **Complete Database-Driven Application**

Create `StudentDatabaseManager.java`:

```java
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complete Student Database Management System
 * Demonstrates professional database application architecture
 */
public class StudentDatabaseManager {
    
    private static final String DB_URL = "jdbc:h2:mem:student_management;DB_CLOSE_DELAY=-1";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    
    // Connection pool simulation
    private static final Map<String, Connection> connectionPool = new ConcurrentHashMap<>();
    private static final int MAX_CONNECTIONS = 10;
    
    // Student class (reusing from previous example)
    static class Student {
        private int id;
        private String firstName;
        private String lastName;
        private String email;
        private int age;
        private double gpa;
        private LocalDate enrollmentDate;
        private boolean isActive;
        
        public Student() {}
        
        public Student(String firstName, String lastName, String email, int age, double gpa) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.age = age;
            this.gpa = gpa;
            this.enrollmentDate = LocalDate.now();
            this.isActive = true;
        }
        
        // Getters and setters (same as previous example)
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public double getGpa() { return gpa; }
        public void setGpa(double gpa) { this.gpa = gpa; }
        
        public LocalDate getEnrollmentDate() { return enrollmentDate; }
        public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
        
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        
        @Override
        public String toString() {
            return String.format("%s %s (ID: %d, Email: %s, Age: %d, GPA: %.2f, Active: %s)",
                               firstName, lastName, id, email, age, gpa, isActive);
        }
    }
    
    // Data Access Object (DAO) pattern
    static class StudentDAO {
        private Connection connection;
        
        public StudentDAO(Connection connection) {
            this.connection = connection;
        }
        
        public int save(Student student) throws SQLException {
            String sql = """
                INSERT INTO students (first_name, last_name, email, age, gpa, enrollment_date, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, student.getFirstName());
                pstmt.setString(2, student.getLastName());
                pstmt.setString(3, student.getEmail());
                pstmt.setInt(4, student.getAge());
                pstmt.setDouble(5, student.getGpa());
                pstmt.setDate(6, Date.valueOf(student.getEnrollmentDate()));
                pstmt.setBoolean(7, student.isActive());
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            student.setId(generatedKeys.getInt(1));
                            return student.getId();
                        }
                    }
                }
                
                throw new SQLException("Failed to save student");
            }
        }
        
        public Optional<Student> findById(int id) throws SQLException {
            String sql = "SELECT * FROM students WHERE id = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToStudent(rs));
                    }
                }
            }
            
            return Optional.empty();
        }
        
        public List<Student> findAll() throws SQLException {
            String sql = "SELECT * FROM students ORDER BY last_name, first_name";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                List<Student> students = new ArrayList<>();
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
                
                return students;
            }
        }
        
        public List<Student> findByGPARange(double minGPA, double maxGPA) throws SQLException {
            String sql = "SELECT * FROM students WHERE gpa BETWEEN ? AND ? ORDER BY gpa DESC";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, minGPA);
                pstmt.setDouble(2, maxGPA);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    List<Student> students = new ArrayList<>();
                    while (rs.next()) {
                        students.add(mapResultSetToStudent(rs));
                    }
                    
                    return students;
                }
            }
        }
        
        public boolean update(Student student) throws SQLException {
            String sql = """
                UPDATE students 
                SET first_name = ?, last_name = ?, email = ?, age = ?, gpa = ?, is_active = ?
                WHERE id = ?
                """;
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, student.getFirstName());
                pstmt.setString(2, student.getLastName());
                pstmt.setString(3, student.getEmail());
                pstmt.setInt(4, student.getAge());
                pstmt.setDouble(5, student.getGpa());
                pstmt.setBoolean(6, student.isActive());
                pstmt.setInt(7, student.getId());
                
                return pstmt.executeUpdate() > 0;
            }
        }
        
        public boolean delete(int id) throws SQLException {
            String sql = "DELETE FROM students WHERE id = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                return pstmt.executeUpdate() > 0;
            }
        }
        
        public Map<String, Object> getStatistics() throws SQLException {
            Map<String, Object> stats = new HashMap<>();
            
            // Total students
            String totalSQL = "SELECT COUNT(*) as total FROM students";
            try (PreparedStatement pstmt = connection.prepareStatement(totalSQL);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalStudents", rs.getInt("total"));
                }
            }
            
            // Active students
            String activeSQL = "SELECT COUNT(*) as active FROM students WHERE is_active = true";
            try (PreparedStatement pstmt = connection.prepareStatement(activeSQL);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("activeStudents", rs.getInt("active"));
                }
            }
            
            // Average GPA
            String avgGPASQL = "SELECT AVG(gpa) as avg_gpa FROM students WHERE is_active = true";
            try (PreparedStatement pstmt = connection.prepareStatement(avgGPASQL);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("averageGPA", rs.getDouble("avg_gpa"));
                }
            }
            
            // GPA distribution
            String gpaDistSQL = """
                SELECT 
                    CASE 
                        WHEN gpa >= 3.5 THEN 'A (3.5+)'
                        WHEN gpa >= 3.0 THEN 'B (3.0-3.4)'
                        WHEN gpa >= 2.5 THEN 'C (2.5-2.9)'
                        WHEN gpa >= 2.0 THEN 'D (2.0-2.4)'
                        ELSE 'F (Below 2.0)'
                    END as grade_range,
                    COUNT(*) as count
                FROM students
                WHERE is_active = true
                GROUP BY grade_range
                ORDER BY MIN(gpa) DESC
                """;
            
            try (PreparedStatement pstmt = connection.prepareStatement(gpaDistSQL);
                 ResultSet rs = pstmt.executeQuery()) {
                
                Map<String, Integer> gpaDistribution = new HashMap<>();
                while (rs.next()) {
                    gpaDistribution.put(rs.getString("grade_range"), rs.getInt("count"));
                }
                stats.put("gpaDistribution", gpaDistribution);
            }
            
            return stats;
        }
        
        private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
            Student student = new Student();
            student.setId(rs.getInt("id"));
            student.setFirstName(rs.getString("first_name"));
            student.setLastName(rs.getString("last_name"));
            student.setEmail(rs.getString("email"));
            student.setAge(rs.getInt("age"));
            student.setGpa(rs.getDouble("gpa"));
            
            Date enrollmentDate = rs.getDate("enrollment_date");
            if (enrollmentDate != null) {
                student.setEnrollmentDate(enrollmentDate.toLocalDate());
            }
            
            student.setActive(rs.getBoolean("is_active"));
            
            return student;
        }
    }
    
    // Service layer
    static class StudentService {
        private StudentDAO studentDAO;
        
        public StudentService(StudentDAO studentDAO) {
            this.studentDAO = studentDAO;
        }
        
        public Student createStudent(String firstName, String lastName, String email, int age, double gpa) throws SQLException {
            // Validation
            if (firstName == null || firstName.trim().isEmpty()) {
                throw new IllegalArgumentException("First name cannot be empty");
            }
            if (lastName == null || lastName.trim().isEmpty()) {
                throw new IllegalArgumentException("Last name cannot be empty");
            }
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Invalid email format");
            }
            if (age < 16 || age > 100) {
                throw new IllegalArgumentException("Age must be between 16 and 100");
            }
            if (gpa < 0.0 || gpa > 4.0) {
                throw new IllegalArgumentException("GPA must be between 0.0 and 4.0");
            }
            
            Student student = new Student(firstName, lastName, email, age, gpa);
            int id = studentDAO.save(student);
            student.setId(id);
            
            return student;
        }
        
        public Optional<Student> getStudent(int id) throws SQLException {
            return studentDAO.findById(id);
        }
        
        public List<Student> getAllStudents() throws SQLException {
            return studentDAO.findAll();
        }
        
        public List<Student> getHonorStudents() throws SQLException {
            return studentDAO.findByGPARange(3.5, 4.0);
        }
        
        public boolean updateStudent(Student student) throws SQLException {
            return studentDAO.update(student);
        }
        
        public boolean deleteStudent(int id) throws SQLException {
            return studentDAO.delete(id);
        }
        
        public Map<String, Object> getStatistics() throws SQLException {
            return studentDAO.getStatistics();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Student Database Management System ===\n");
        
        try {
            // Initialize database
            initializeDatabase();
            
            // Create service layer
            Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            StudentDAO studentDAO = new StudentDAO(connection);
            StudentService studentService = new StudentService(studentDAO);
            
            // Demonstrate the application
            demonstrateApplication(studentService);
            
            connection.close();
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
    
    private static void initializeDatabase() throws SQLException {
        System.out.println("🔧 Initializing Student Management Database...");
        
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            
            String createTableSQL = """
                CREATE TABLE students (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    age INT CHECK (age >= 16 AND age <= 100),
                    gpa DECIMAL(3,2) CHECK (gpa >= 0.0 AND gpa <= 4.0),
                    enrollment_date DATE,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
            
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
                System.out.println("✅ Database initialized successfully\n");
            }
        }
    }
    
    private static void demonstrateApplication(StudentService studentService) {
        System.out.println("🎓 Student Management System Demo:");
        
        try {
            // Create students
            System.out.println("\n➕ Creating students...");
            Student student1 = studentService.createStudent("John", "Doe", "john.doe@university.edu", 20, 3.5);
            Student student2 = studentService.createStudent("Jane", "Smith", "jane.smith@university.edu", 19, 3.8);
            Student student3 = studentService.createStudent("Bob", "Johnson", "bob.johnson@university.edu", 21, 3.2);
            Student student4 = studentService.createStudent("Alice", "Brown", "alice.brown@university.edu", 22, 3.9);
            Student student5 = studentService.createStudent("Charlie", "Davis", "charlie.davis@university.edu", 20, 3.6);
            
            System.out.println("✅ Created 5 students");
            
            // Display all students
            System.out.println("\n📚 All Students:");
            List<Student> allStudents = studentService.getAllStudents();
            allStudents.forEach(System.out::println);
            
            // Show honor students
            System.out.println("\n🏆 Honor Students (GPA >= 3.5):");
            List<Student> honorStudents = studentService.getHonorStudents();
            honorStudents.forEach(System.out::println);
            
            // Update a student
            System.out.println("\n✏️ Updating student...");
            Optional<Student> studentToUpdate = studentService.getStudent(1);
            if (studentToUpdate.isPresent()) {
                Student student = studentToUpdate.get();
                student.setGpa(3.7);
                student.setAge(21);
                
                if (studentService.updateStudent(student)) {
                    System.out.println("✅ Updated: " + student);
                }
            }
            
            // Generate statistics
            System.out.println("\n📊 Statistics:");
            Map<String, Object> stats = studentService.getStatistics();
            System.out.println("Total Students: " + stats.get("totalStudents"));
            System.out.println("Active Students: " + stats.get("activeStudents"));
            System.out.println("Average GPA: " + String.format("%.2f", (Double) stats.get("averageGPA")));
            
            System.out.println("\n📈 GPA Distribution:");
            @SuppressWarnings("unchecked")
            Map<String, Integer> gpaDistribution = (Map<String, Integer>) stats.get("gpaDistribution");
            gpaDistribution.forEach((grade, count) -> 
                System.out.println("  " + grade + ": " + count + " students"));
            
            // Interactive menu simulation
            System.out.println("\n🎮 Interactive Menu Simulation:");
            simulateInteractiveMenu(studentService);
            
        } catch (SQLException e) {
            System.err.println("❌ Service error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Validation error: " + e.getMessage());
        }
    }
    
    private static void simulateInteractiveMenu(StudentService studentService) throws SQLException {
        System.out.println("Simulating user selecting option '2' (Search by ID)...");
        
        // Simulate user input
        int searchId = 2;
        Optional<Student> foundStudent = studentService.getStudent(searchId);
        
        if (foundStudent.isPresent()) {
            System.out.println("🔍 Found student: " + foundStudent.get());
        } else {
            System.out.println("❌ Student with ID " + searchId + " not found");
        }
        
        System.out.println("\nSimulating user selecting option '4' (Delete student)...");
        
        // Simulate deletion
        int deleteId = 3;
        if (studentService.deleteStudent(deleteId)) {
            System.out.println("✅ Student with ID " + deleteId + " deleted successfully");
        } else {
            System.out.println("❌ Failed to delete student with ID " + deleteId);
        }
        
        // Show updated list
        System.out.println("\n📚 Updated Student List:");
        List<Student> updatedStudents = studentService.getAllStudents();
        updatedStudents.forEach(System.out::println);
    }
}
```

---

## 📝 Key Takeaways from Day 15

### **JDBC Architecture:**
- ✅ **Driver Manager:** Manages database connections and drivers
- ✅ **Connection:** Represents database connection with transaction control
- ✅ **PreparedStatement:** Secure and efficient SQL execution
- ✅ **ResultSet:** Flexible result navigation and data retrieval
- ✅ **Exception Handling:** Comprehensive error handling with SQLException

### **Best Practices:**
- ✅ **Security:** Always use PreparedStatement to prevent SQL injection
- ✅ **Resource Management:** Close connections, statements, and result sets properly
- ✅ **Transactions:** Use transactions for data consistency
- ✅ **Connection Pooling:** Reuse connections for better performance
- ✅ **Error Handling:** Implement robust exception handling

### **Database Operations:**
- ✅ **CRUD Operations:** Complete Create, Read, Update, Delete functionality
- ✅ **Batch Processing:** Efficient bulk operations
- ✅ **Metadata Access:** Database and result set metadata information
- ✅ **Advanced Queries:** Aggregations, joins, subqueries, and pagination
- ✅ **Data Validation:** Input validation and constraint handling

---

## 🎯 Today's Success Criteria

**Completed Tasks:**
- [ ] Establish secure database connections with proper error handling
- [ ] Implement all CRUD operations using PreparedStatement
- [ ] Navigate ResultSet with different cursor types and directions
- [ ] Handle various data types and null values correctly
- [ ] Build a complete database-driven application with DAO pattern
- [ ] Implement transaction management and error recovery

**Bonus Achievements:**
- [ ] Create connection pooling mechanism
- [ ] Implement database migration scripts
- [ ] Add comprehensive logging and monitoring
- [ ] Create database backup and restore functionality
- [ ] Implement caching layer for frequently accessed data

---

## 🚀 Tomorrow's Preview (Day 16 - July 25)

**Focus:** JDBC & Database Connectivity - Part 2
- Connection pooling with HikariCP
- Advanced transaction management
- Batch processing for high performance
- Stored procedures and functions
- Database optimization techniques

---

## 🎉 Excellent Progress!

You've mastered database connectivity fundamentals! JDBC is the foundation for all Java database operations. Tomorrow we'll explore advanced features for production-ready applications.

**Keep building with enterprise Java!** 🚀