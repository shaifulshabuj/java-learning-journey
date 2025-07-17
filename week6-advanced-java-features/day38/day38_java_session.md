# Day 38: Advanced Stream Operations (August 16, 2025)

**Date:** August 16, 2025  
**Duration:** 3 hours  
**Focus:** Complex data transformations, parallel processing, and advanced stream techniques

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Master complex data transformations with advanced collectors
- ✅ Understand parallel streams and performance characteristics
- ✅ Implement custom collectors for specialized data processing
- ✅ Learn advanced grouping and partitioning techniques
- ✅ Apply stream composition patterns and best practices
- ✅ Build high-performance data processing applications

---

## 🚀 Part 1: Advanced Collectors & Complex Transformations (60 minutes)

### **Complex Data Aggregation Patterns**

Create `AdvancedStreamOperations.java`:

```java
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Advanced Stream operations and complex data transformations
 */
public class AdvancedStreamOperations {
    
    public static void main(String[] args) {
        System.out.println("=== Advanced Stream Operations ===\n");
        
        // 1. Advanced collectors
        System.out.println("1. Advanced Collectors:");
        demonstrateAdvancedCollectors();
        
        // 2. Complex transformations
        System.out.println("\n2. Complex Transformations:");
        demonstrateComplexTransformations();
        
        // 3. Multi-level data processing
        System.out.println("\n3. Multi-level Data Processing:");
        demonstrateMultiLevelProcessing();
        
        // 4. Stream composition patterns
        System.out.println("\n4. Stream Composition Patterns:");
        demonstrateStreamComposition();
        
        System.out.println("\n=== Advanced Stream Operations Completed ===");
    }
    
    private static void demonstrateAdvancedCollectors() {
        List<Employee> employees = createSampleEmployees();
        
        System.out.println("Employee data:");
        employees.forEach(System.out::println);
        
        // 1. Advanced grouping with multiple classifiers
        Map<String, Map<String, Map<String, List<Employee>>>> multiLevelGrouping = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::getDepartment,
                Collectors.groupingBy(
                    Employee::getLevel,
                    Collectors.groupingBy(emp -> emp.getSalary() > 80000 ? "High" : "Normal")
                )
            ));
        
        System.out.println("\nMulti-level grouping (Department -> Level -> Salary Range):");
        multiLevelGrouping.forEach((dept, levelMap) -> {
            System.out.println(dept + ":");
            levelMap.forEach((level, salaryMap) -> {
                System.out.println("  " + level + ":");
                salaryMap.forEach((salaryRange, empList) -> 
                    System.out.println("    " + salaryRange + ": " + empList.size() + " employees"));
            });
        });
        
        // 2. Collectors.teeing() - split stream into two paths
        Map<String, Object> salaryAnalysis = employees.stream()
            .collect(Collectors.teeing(
                Collectors.averagingDouble(Employee::getSalary),
                Collectors.maxBy(Comparator.comparing(Employee::getSalary)),
                (avgSalary, maxSalaryEmp) -> Map.of(
                    "averageSalary", avgSalary,
                    "highestPaidEmployee", maxSalaryEmp.orElse(null)
                )
            ));
        
        System.out.println("\nSalary analysis using teeing:");
        System.out.println("Average salary: $" + salaryAnalysis.get("averageSalary"));
        System.out.println("Highest paid: " + salaryAnalysis.get("highestPaidEmployee"));
        
        // 3. Collectors.filtering() and mapping() with downstream collectors
        Map<String, List<String>> seniorEmployeeNames = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::getDepartment,
                Collectors.filtering(
                    emp -> emp.getLevel().equals("Senior"),
                    Collectors.mapping(Employee::getName, Collectors.toList())
                )
            ));
        
        System.out.println("\nSenior employees by department:");
        seniorEmployeeNames.forEach((dept, names) -> 
            System.out.println(dept + ": " + names));
        
        // 4. Custom reduction with complex logic
        Optional<Employee> mostExperiencedInEngineering = employees.stream()
            .filter(emp -> emp.getDepartment().equals("Engineering"))
            .reduce((emp1, emp2) -> 
                emp1.getYearsOfExperience() > emp2.getYearsOfExperience() ? emp1 : emp2);
        
        System.out.println("\nMost experienced in Engineering: " + 
            mostExperiencedInEngineering.orElse(null));
        
        // 5. Parallel aggregation with thread-safe collectors
        ConcurrentHashMap<String, Double> concurrentSalaryByDept = employees.parallelStream()
            .collect(Collectors.groupingByConcurrent(
                Employee::getDepartment,
                Collectors.averagingDouble(Employee::getSalary)
            ));
        
        System.out.println("\nConcurrent salary averages by department: " + concurrentSalaryByDept);
        
        // 6. Complex statistical analysis
        Map<String, DoubleSummaryStatistics> salaryStatsByDept = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::getDepartment,
                Collectors.summarizingDouble(Employee::getSalary)
            ));
        
        System.out.println("\nDetailed salary statistics by department:");
        salaryStatsByDept.forEach((dept, stats) -> {
            System.out.printf("%s: Count=%d, Sum=$%.0f, Avg=$%.0f, Min=$%.0f, Max=$%.0f\n",
                dept, stats.getCount(), stats.getSum(), stats.getAverage(), 
                stats.getMin(), stats.getMax());
        });
    }
    
    private static void demonstrateComplexTransformations() {
        List<Order> orders = createSampleOrders();
        
        System.out.println("Order data:");
        orders.forEach(System.out::println);
        
        // 1. Complex flatMap transformation
        List<OrderLineItem> allLineItems = orders.stream()
            .flatMap(order -> order.getLineItems().stream())
            .collect(Collectors.toList());
        
        System.out.println("\nAll line items (" + allLineItems.size() + " total):");
        allLineItems.stream().limit(5).forEach(System.out::println);
        
        // 2. Hierarchical data flattening with context preservation
        List<OrderItemSummary> itemSummaries = orders.stream()
            .flatMap(order -> order.getLineItems().stream()
                .map(item -> new OrderItemSummary(
                    order.getOrderId(),
                    order.getCustomer(),
                    item.getProduct(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getQuantity() * item.getPrice()
                )))
            .collect(Collectors.toList());
        
        System.out.println("\nOrder item summaries:");
        itemSummaries.stream().limit(3).forEach(System.out::println);
        
        // 3. Complex aggregation with multiple dimensions
        Map<String, Map<String, BigDecimal>> revenueByCustomerAndProduct = itemSummaries.stream()
            .collect(Collectors.groupingBy(
                OrderItemSummary::getCustomer,
                Collectors.groupingBy(
                    OrderItemSummary::getProduct,
                    Collectors.mapping(
                        OrderItemSummary::getTotalPrice,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                    )
                )
            ));
        
        System.out.println("\nRevenue by customer and product:");
        revenueByCustomerAndProduct.forEach((customer, productRevenue) -> {
            System.out.println(customer + ":");
            productRevenue.forEach((product, revenue) -> 
                System.out.printf("  %s: $%.2f\n", product, revenue));
        });
        
        // 4. Advanced filtering with complex predicates
        Predicate<Order> highValueOrder = order -> 
            order.getLineItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum() > 1000;
        
        Predicate<Order> multiItemOrder = order -> order.getLineItems().size() > 2;
        
        List<Order> premiumOrders = orders.stream()
            .filter(highValueOrder.and(multiItemOrder))
            .sorted(Comparator.comparing(order -> 
                order.getLineItems().stream()
                    .mapToDouble(item -> item.getQuantity() * item.getPrice())
                    .sum()).reversed())
            .collect(Collectors.toList());
        
        System.out.println("\nPremium orders (high value + multiple items):");
        premiumOrders.forEach(order -> {
            double total = order.getLineItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
            System.out.printf("Order %s (%s): $%.2f\n", 
                order.getOrderId(), order.getCustomer(), total);
        });
        
        // 5. Stream transformation with error handling
        List<String> processedOrderIds = orders.stream()
            .map(this::processOrderSafely)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("\nProcessed order IDs: " + processedOrderIds);
    }
    
    private String processOrderSafely(Order order) {
        try {
            // Simulate processing that might fail
            if (order.getOrderId().hashCode() % 7 == 0) {
                throw new RuntimeException("Processing error for order " + order.getOrderId());
            }
            return "PROCESSED_" + order.getOrderId();
        } catch (Exception e) {
            System.err.println("Error processing order: " + e.getMessage());
            return null;
        }
    }
    
    private static void demonstrateMultiLevelProcessing() {
        List<University> universities = createSampleUniversities();
        
        System.out.println("University data:");
        universities.forEach(System.out::println);
        
        // 1. Deep nested stream operations
        Map<String, Double> avgGpaByUniversity = universities.stream()
            .collect(Collectors.toMap(
                University::getName,
                university -> university.getDepartments().stream()
                    .flatMap(dept -> dept.getStudents().stream())
                    .mapToDouble(Student::getGpa)
                    .average()
                    .orElse(0.0)
            ));
        
        System.out.println("\nAverage GPA by university:");
        avgGpaByUniversity.forEach((name, gpa) -> 
            System.out.printf("%s: %.2f\n", name, gpa));
        
        // 2. Cross-level aggregation
        Map<String, Long> totalStudentsByUniversity = universities.stream()
            .collect(Collectors.toMap(
                University::getName,
                university -> university.getDepartments().stream()
                    .mapToLong(dept -> dept.getStudents().size())
                    .sum()
            ));
        
        System.out.println("\nTotal students by university:");
        totalStudentsByUniversity.forEach((name, count) -> 
            System.out.println(name + ": " + count + " students"));
        
        // 3. Complex hierarchical analysis
        Map<String, Map<String, List<Student>>> topStudentsByUniversityAndDept = universities.stream()
            .collect(Collectors.toMap(
                University::getName,
                university -> university.getDepartments().stream()
                    .collect(Collectors.toMap(
                        Department::getName,
                        dept -> dept.getStudents().stream()
                            .filter(student -> student.getGpa() >= 3.5)
                            .sorted(Comparator.comparing(Student::getGpa).reversed())
                            .limit(2)
                            .collect(Collectors.toList())
                    ))
            ));
        
        System.out.println("\nTop students (GPA >= 3.5) by university and department:");
        topStudentsByUniversityAndDept.forEach((uniName, deptMap) -> {
            System.out.println(uniName + ":");
            deptMap.forEach((deptName, students) -> {
                System.out.println("  " + deptName + ":");
                students.forEach(student -> 
                    System.out.printf("    %s (GPA: %.2f)\n", student.getName(), student.getGpa()));
            });
        });
        
        // 4. Performance ranking across institutions
        List<StudentRanking> allStudentsRanked = universities.stream()
            .flatMap(university -> university.getDepartments().stream()
                .flatMap(dept -> dept.getStudents().stream()
                    .map(student -> new StudentRanking(
                        student.getName(),
                        university.getName(),
                        dept.getName(),
                        student.getGpa()
                    ))))
            .sorted(Comparator.comparing(StudentRanking::getGpa).reversed())
            .collect(Collectors.toList());
        
        System.out.println("\nTop 5 students across all universities:");
        allStudentsRanked.stream()
            .limit(5)
            .forEach(ranking -> System.out.printf("%s (%s, %s): %.2f\n",
                ranking.getStudentName(), ranking.getUniversityName(),
                ranking.getDepartmentName(), ranking.getGpa()));
    }
    
    private static void demonstrateStreamComposition() {
        List<Transaction> transactions = createSampleTransactions();
        
        System.out.println("Transaction data (" + transactions.size() + " transactions):");
        transactions.stream().limit(5).forEach(System.out::println);
        
        // 1. Function composition for complex transformations
        Function<Transaction, String> getAccountType = Transaction::getAccount;
        Function<String, String> normalizeAccountType = account -> 
            account.toLowerCase().replace("_", " ");
        Function<Transaction, String> getFormattedAccount = getAccountType.andThen(normalizeAccountType);
        
        Map<String, Long> transactionsByAccountType = transactions.stream()
            .collect(Collectors.groupingBy(
                getFormattedAccount,
                Collectors.counting()
            ));
        
        System.out.println("\nTransactions by account type:");
        transactionsByAccountType.forEach((account, count) -> 
            System.out.println(account + ": " + count + " transactions"));
        
        // 2. Predicate composition for complex filtering
        Predicate<Transaction> isHighValue = tx -> tx.getAmount() > 1000;
        Predicate<Transaction> isRecent = tx -> tx.getDate().contains("2025");
        Predicate<Transaction> isDebit = tx -> tx.getType().equals("DEBIT");
        
        Predicate<Transaction> suspiciousTransaction = isHighValue
            .and(isRecent)
            .and(isDebit);
        
        List<Transaction> suspiciousTransactions = transactions.stream()
            .filter(suspiciousTransaction)
            .sorted(Comparator.comparing(Transaction::getAmount).reversed())
            .collect(Collectors.toList());
        
        System.out.println("\nSuspicious transactions (high value, recent, debit):");
        suspiciousTransactions.forEach(System.out::println);
        
        // 3. Consumer composition for multi-stage processing
        Consumer<Transaction> logTransaction = tx -> 
            System.out.println("Processing: " + tx.getId());
        Consumer<Transaction> validateTransaction = tx -> {
            if (tx.getAmount() < 0) {
                System.err.println("Invalid amount: " + tx.getId());
            }
        };
        Consumer<Transaction> auditTransaction = tx -> 
            System.out.println("Audited: " + tx.getId() + " ($" + tx.getAmount() + ")");
        
        Consumer<Transaction> fullProcessing = logTransaction
            .andThen(validateTransaction)
            .andThen(auditTransaction);
        
        System.out.println("\nFull transaction processing:");
        transactions.stream()
            .limit(3)
            .forEach(fullProcessing);
        
        // 4. Stream pipeline composition
        Function<List<Transaction>, Map<String, Double>> analyzeTransactions = txList -> txList.stream()
            .filter(tx -> tx.getAmount() > 0)
            .collect(Collectors.groupingBy(
                Transaction::getAccount,
                Collectors.summingDouble(Transaction::getAmount)
            ));
        
        Function<Map<String, Double>, List<Map.Entry<String, Double>>> rankAccounts = accountTotals ->
            accountTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        Function<List<Transaction>, List<Map.Entry<String, Double>>> fullAnalysis = 
            analyzeTransactions.andThen(rankAccounts);
        
        List<Map.Entry<String, Double>> accountRanking = fullAnalysis.apply(transactions);
        
        System.out.println("\nAccount ranking by total transaction volume:");
        accountRanking.forEach(entry -> 
            System.out.printf("%s: $%.2f\n", entry.getKey(), entry.getValue()));
        
        // 5. Conditional stream processing
        Function<List<Transaction>, Stream<Transaction>> conditionalFilter = txList -> {
            boolean hasHighValueTransactions = txList.stream()
                .anyMatch(tx -> tx.getAmount() > 5000);
            
            return hasHighValueTransactions 
                ? txList.stream().filter(tx -> tx.getAmount() > 1000)
                : txList.stream();
        };
        
        List<Transaction> conditionallyFiltered = conditionalFilter.apply(transactions)
            .collect(Collectors.toList());
        
        System.out.println("\nConditionally filtered transactions: " + 
            conditionallyFiltered.size() + " transactions");
    }
    
    // Helper methods to create sample data
    private static List<Employee> createSampleEmployees() {
        return Arrays.asList(
            new Employee("Alice", "Engineering", "Senior", 95000, 8),
            new Employee("Bob", "Engineering", "Junior", 65000, 2),
            new Employee("Charlie", "Marketing", "Senior", 85000, 6),
            new Employee("Diana", "Engineering", "Mid", 75000, 4),
            new Employee("Eve", "Marketing", "Junior", 55000, 1),
            new Employee("Frank", "Sales", "Senior", 90000, 7),
            new Employee("Grace", "Sales", "Mid", 70000, 3),
            new Employee("Henry", "Engineering", "Mid", 80000, 5)
        );
    }
    
    private static List<Order> createSampleOrders() {
        return Arrays.asList(
            new Order("O001", "Alice", Arrays.asList(
                new OrderLineItem("Laptop", 1, 999.99),
                new OrderLineItem("Mouse", 2, 29.99),
                new OrderLineItem("Keyboard", 1, 79.99)
            )),
            new Order("O002", "Bob", Arrays.asList(
                new OrderLineItem("Phone", 1, 699.99),
                new OrderLineItem("Case", 1, 19.99)
            )),
            new Order("O003", "Alice", Arrays.asList(
                new OrderLineItem("Monitor", 2, 299.99),
                new OrderLineItem("Cable", 3, 9.99),
                new OrderLineItem("Stand", 2, 49.99)
            ))
        );
    }
    
    private static List<University> createSampleUniversities() {
        return Arrays.asList(
            new University("Tech University", Arrays.asList(
                new Department("Computer Science", Arrays.asList(
                    new Student("John", 3.8),
                    new Student("Jane", 3.9),
                    new Student("Jake", 3.2)
                )),
                new Department("Mathematics", Arrays.asList(
                    new Student("Mary", 3.7),
                    new Student("Mark", 3.5)
                ))
            )),
            new University("State University", Arrays.asList(
                new Department("Computer Science", Arrays.asList(
                    new Student("Sarah", 3.6),
                    new Student("Sam", 3.4)
                )),
                new Department("Physics", Arrays.asList(
                    new Student("Paul", 3.9),
                    new Student("Peter", 3.3)
                ))
            ))
        );
    }
    
    private static List<Transaction> createSampleTransactions() {
        Random random = new Random(42); // Fixed seed for reproducible results
        String[] accounts = {"CHECKING", "SAVINGS", "CREDIT_CARD", "INVESTMENT"};
        String[] types = {"DEBIT", "CREDIT"};
        
        return IntStream.rangeClosed(1, 20)
            .mapToObj(i -> new Transaction(
                "TX" + String.format("%03d", i),
                "2025-08-" + String.format("%02d", random.nextInt(15) + 1),
                accounts[random.nextInt(accounts.length)],
                types[random.nextInt(types.length)],
                100 + random.nextDouble() * 2000
            ))
            .collect(Collectors.toList());
    }
    
    // Helper classes
    static class Employee {
        private final String name;
        private final String department;
        private final String level;
        private final double salary;
        private final int yearsOfExperience;
        
        public Employee(String name, String department, String level, double salary, int yearsOfExperience) {
            this.name = name;
            this.department = department;
            this.level = level;
            this.salary = salary;
            this.yearsOfExperience = yearsOfExperience;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public String getLevel() { return level; }
        public double getSalary() { return salary; }
        public int getYearsOfExperience() { return yearsOfExperience; }
        
        @Override
        public String toString() {
            return String.format("%s (%s, %s, $%.0f, %d years)", 
                name, department, level, salary, yearsOfExperience);
        }
    }
    
    static class Order {
        private final String orderId;
        private final String customer;
        private final List<OrderLineItem> lineItems;
        
        public Order(String orderId, String customer, List<OrderLineItem> lineItems) {
            this.orderId = orderId;
            this.customer = customer;
            this.lineItems = lineItems;
        }
        
        public String getOrderId() { return orderId; }
        public String getCustomer() { return customer; }
        public List<OrderLineItem> getLineItems() { return lineItems; }
        
        @Override
        public String toString() {
            return String.format("Order %s (%s): %d items", orderId, customer, lineItems.size());
        }
    }
    
    static class OrderLineItem {
        private final String product;
        private final int quantity;
        private final double price;
        
        public OrderLineItem(String product, int quantity, double price) {
            this.product = product;
            this.quantity = quantity;
            this.price = price;
        }
        
        public String getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        
        @Override
        public String toString() {
            return String.format("%s x%d @ $%.2f", product, quantity, price);
        }
    }
    
    static class OrderItemSummary {
        private final String orderId;
        private final String customer;
        private final String product;
        private final int quantity;
        private final double unitPrice;
        private final BigDecimal totalPrice;
        
        public OrderItemSummary(String orderId, String customer, String product, 
                               int quantity, double unitPrice, double totalPrice) {
            this.orderId = orderId;
            this.customer = customer;
            this.product = product;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = BigDecimal.valueOf(totalPrice).setScale(2, RoundingMode.HALF_UP);
        }
        
        public String getOrderId() { return orderId; }
        public String getCustomer() { return customer; }
        public String getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        
        @Override
        public String toString() {
            return String.format("%s: %s bought %s x%d = $%.2f", 
                orderId, customer, product, quantity, totalPrice);
        }
    }
    
    static class University {
        private final String name;
        private final List<Department> departments;
        
        public University(String name, List<Department> departments) {
            this.name = name;
            this.departments = departments;
        }
        
        public String getName() { return name; }
        public List<Department> getDepartments() { return departments; }
        
        @Override
        public String toString() {
            return String.format("%s (%d departments)", name, departments.size());
        }
    }
    
    static class Department {
        private final String name;
        private final List<Student> students;
        
        public Department(String name, List<Student> students) {
            this.name = name;
            this.students = students;
        }
        
        public String getName() { return name; }
        public List<Student> getStudents() { return students; }
        
        @Override
        public String toString() {
            return String.format("%s (%d students)", name, students.size());
        }
    }
    
    static class Student {
        private final String name;
        private final double gpa;
        
        public Student(String name, double gpa) {
            this.name = name;
            this.gpa = gpa;
        }
        
        public String getName() { return name; }
        public double getGpa() { return gpa; }
        
        @Override
        public String toString() {
            return String.format("%s (GPA: %.2f)", name, gpa);
        }
    }
    
    static class StudentRanking {
        private final String studentName;
        private final String universityName;
        private final String departmentName;
        private final double gpa;
        
        public StudentRanking(String studentName, String universityName, 
                            String departmentName, double gpa) {
            this.studentName = studentName;
            this.universityName = universityName;
            this.departmentName = departmentName;
            this.gpa = gpa;
        }
        
        public String getStudentName() { return studentName; }
        public String getUniversityName() { return universityName; }
        public String getDepartmentName() { return departmentName; }
        public double getGpa() { return gpa; }
    }
    
    static class Transaction {
        private final String id;
        private final String date;
        private final String account;
        private final String type;
        private final double amount;
        
        public Transaction(String id, String date, String account, String type, double amount) {
            this.id = id;
            this.date = date;
            this.account = account;
            this.type = type;
            this.amount = amount;
        }
        
        public String getId() { return id; }
        public String getDate() { return date; }
        public String getAccount() { return account; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        
        @Override
        public String toString() {
            return String.format("%s: %s %s $%.2f (%s)", id, type, account, amount, date);
        }
    }
}
```

---

## ⚡ Part 2: Parallel Streams & Performance Optimization (60 minutes)

### **High-Performance Parallel Processing**

Create `ParallelStreamProcessing.java`:

```java
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.time.Instant;
import java.time.Duration;

/**
 * Parallel stream processing and performance optimization techniques
 */
public class ParallelStreamProcessing {
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Stream Processing ===\n");
        
        // 1. Parallel vs sequential performance
        System.out.println("1. Performance Comparison:");
        demonstratePerformanceComparison();
        
        // 2. Parallel stream characteristics
        System.out.println("\n2. Parallel Stream Characteristics:");
        demonstrateParallelCharacteristics();
        
        // 3. Thread pool customization
        System.out.println("\n3. Thread Pool Customization:");
        demonstrateCustomThreadPool();
        
        // 4. Best practices and pitfalls
        System.out.println("\n4. Best Practices and Pitfalls:");
        demonstrateBestPractices();
        
        System.out.println("\n=== Parallel Stream Processing Completed ===");
    }
    
    private static void demonstratePerformanceComparison() {
        // Create large dataset for meaningful performance comparison
        List<Integer> largeList = IntStream.rangeClosed(1, 10_000_000)
            .boxed()
            .collect(Collectors.toList());
        
        System.out.println("Testing with " + largeList.size() + " elements");
        
        // Test 1: Simple mathematical operations
        System.out.println("\nTest 1: Mathematical Operations (sum of squares of even numbers)");
        
        // Sequential version
        Instant start = Instant.now();
        long sequentialResult = largeList.stream()
            .filter(n -> n % 2 == 0)
            .mapToLong(n -> (long) n * n)
            .sum();
        Duration sequentialTime = Duration.between(start, Instant.now());
        
        // Parallel version
        start = Instant.now();
        long parallelResult = largeList.parallelStream()
            .filter(n -> n % 2 == 0)
            .mapToLong(n -> (long) n * n)
            .sum();
        Duration parallelTime = Duration.between(start, Instant.now());
        
        System.out.println("Sequential: " + sequentialTime.toMillis() + "ms, result: " + sequentialResult);
        System.out.println("Parallel: " + parallelTime.toMillis() + "ms, result: " + parallelResult);
        System.out.println("Speedup: " + String.format("%.2fx", 
            (double) sequentialTime.toMillis() / parallelTime.toMillis()));
        
        // Test 2: CPU-intensive operations
        System.out.println("\nTest 2: CPU-Intensive Operations (prime checking)");
        List<Integer> numbersToCheck = IntStream.rangeClosed(1000, 2000)
            .boxed()
            .collect(Collectors.toList());
        
        start = Instant.now();
        long sequentialPrimes = numbersToCheck.stream()
            .filter(ParallelStreamProcessing::isPrime)
            .count();
        Duration sequentialPrimeTime = Duration.between(start, Instant.now());
        
        start = Instant.now();
        long parallelPrimes = numbersToCheck.parallelStream()
            .filter(ParallelStreamProcessing::isPrime)
            .count();
        Duration parallelPrimeTime = Duration.between(start, Instant.now());
        
        System.out.println("Sequential prime count: " + sequentialPrimeTime.toMillis() + 
            "ms, found: " + sequentialPrimes);
        System.out.println("Parallel prime count: " + parallelPrimeTime.toMillis() + 
            "ms, found: " + parallelPrimes);
        System.out.println("Speedup: " + String.format("%.2fx", 
            (double) sequentialPrimeTime.toMillis() / parallelPrimeTime.toMillis()));
        
        // Test 3: I/O simulation (should not use parallel)
        System.out.println("\nTest 3: I/O Simulation (should favor sequential)");
        List<String> urls = IntStream.rangeClosed(1, 100)
            .mapToObj(i -> "https://api.example.com/data/" + i)
            .collect(Collectors.toList());
        
        start = Instant.now();
        long sequentialIo = urls.stream()
            .mapToLong(ParallelStreamProcessing::simulateIoOperation)
            .sum();
        Duration sequentialIoTime = Duration.between(start, Instant.now());
        
        start = Instant.now();
        long parallelIo = urls.parallelStream()
            .mapToLong(ParallelStreamProcessing::simulateIoOperation)
            .sum();
        Duration parallelIoTime = Duration.between(start, Instant.now());
        
        System.out.println("Sequential I/O: " + sequentialIoTime.toMillis() + 
            "ms, result: " + sequentialIo);
        System.out.println("Parallel I/O: " + parallelIoTime.toMillis() + 
            "ms, result: " + parallelIo);
        
        if (parallelIoTime.toMillis() > 0) {
            System.out.println("Speedup: " + String.format("%.2fx", 
                (double) sequentialIoTime.toMillis() / parallelIoTime.toMillis()));
        }
    }
    
    private static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }
    
    private static long simulateIoOperation(String url) {
        try {
            // Simulate I/O latency
            Thread.sleep(1);
            return url.hashCode() % 1000;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    private static void demonstrateParallelCharacteristics() {
        List<String> data = IntStream.rangeClosed(1, 1000)
            .mapToObj(i -> "Item" + i)
            .collect(Collectors.toList());
        
        // 1. Thread usage analysis
        System.out.println("Thread usage in parallel streams:");
        Set<String> sequentialThreads = Collections.synchronizedSet(new HashSet<>());
        Set<String> parallelThreads = Collections.synchronizedSet(new HashSet<>());
        
        // Sequential processing
        data.stream()
            .limit(100)
            .forEach(item -> sequentialThreads.add(Thread.currentThread().getName()));
        
        // Parallel processing
        data.parallelStream()
            .limit(100)
            .forEach(item -> parallelThreads.add(Thread.currentThread().getName()));
        
        System.out.println("Sequential threads used: " + sequentialThreads);
        System.out.println("Parallel threads used: " + parallelThreads.size() + " threads");
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        
        // 2. Splitting characteristics
        System.out.println("\nSplitting characteristics:");
        Spliterator<String> spliterator = data.parallelStream().spliterator();
        System.out.println("Estimated size: " + spliterator.estimateSize());
        System.out.println("Exact size known: " + spliterator.hasCharacteristics(Spliterator.SIZED));
        System.out.println("Ordered: " + spliterator.hasCharacteristics(Spliterator.ORDERED));
        System.out.println("Distinct: " + spliterator.hasCharacteristics(Spliterator.DISTINCT));
        System.out.println("Sorted: " + spliterator.hasCharacteristics(Spliterator.SORTED));
        
        // 3. Order preservation
        System.out.println("\nOrder preservation:");
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        System.out.print("Sequential order: ");
        numbers.stream()
            .map(n -> n * 2)
            .forEach(n -> System.out.print(n + " "));
        System.out.println();
        
        System.out.print("Parallel unordered: ");
        numbers.parallelStream()
            .unordered()
            .map(n -> n * 2)
            .forEach(n -> System.out.print(n + " "));
        System.out.println();
        
        System.out.print("Parallel ordered: ");
        numbers.parallelStream()
            .map(n -> n * 2)
            .forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();
        
        // 4. Reduction operations
        System.out.println("\nReduction operations:");
        List<Integer> bigList = IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        
        // Associative reduction (correct for parallel)
        int sum1 = bigList.parallelStream().reduce(0, Integer::sum);
        System.out.println("Parallel sum (associative): " + sum1);
        
        // Non-associative reduction (incorrect for parallel)
        int sum2 = bigList.parallelStream().reduce(0, (a, b) -> a + b - 1); // Incorrect!
        System.out.println("Parallel sum (non-associative): " + sum2 + " (incorrect)");
        
        // Correct parallel reduction with combiner
        int sum3 = bigList.parallelStream().reduce(
            0,                          // identity
            (a, b) -> a + b,           // accumulator
            (a, b) -> a + b            // combiner
        );
        System.out.println("Parallel sum (with combiner): " + sum3);
    }
    
    private static void demonstrateCustomThreadPool() {
        List<Integer> data = IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        
        // Default ForkJoinPool
        System.out.println("Default ForkJoinPool parallelism: " + 
            ForkJoinPool.commonPool().getParallelism());
        
        // Custom ForkJoinPool
        ForkJoinPool customThreadPool = new ForkJoinPool(2);
        try {
            System.out.println("Custom ForkJoinPool parallelism: " + 
                customThreadPool.getParallelism());
            
            // Use custom thread pool
            Integer result = customThreadPool.submit(() ->
                data.parallelStream()
                    .mapToInt(Integer::intValue)
                    .sum()
            ).get();
            
            System.out.println("Result with custom thread pool: " + result);
            
            // Demonstrate thread pool usage
            Set<String> threadsUsed = Collections.synchronizedSet(new HashSet<>());
            
            customThreadPool.submit(() ->
                data.parallelStream()
                    .forEach(item -> threadsUsed.add(Thread.currentThread().getName()))
            ).get();
            
            System.out.println("Threads used in custom pool: " + threadsUsed.size());
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error with custom thread pool: " + e.getMessage());
        } finally {
            customThreadPool.shutdown();
        }
        
        // System property approach (affects all parallel streams)
        System.out.println("\nSystem property approach:");
        String originalParallelism = System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");
        
        // Note: This only affects new ForkJoinPool instances
        System.out.println("Modified parallelism setting (for new pools): 4");
        
        // Restore original setting
        if (originalParallelism != null) {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", originalParallelism);
        } else {
            System.clearProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
        }
    }
    
    private static void demonstrateBestPractices() {
        // 1. When to use parallel streams
        System.out.println("When to use parallel streams:");
        
        List<Integer> smallList = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());
        List<Integer> largeList = IntStream.rangeClosed(1, 100_000).boxed().collect(Collectors.toList());
        
        // Small dataset - parallel overhead outweighs benefits
        Instant start = Instant.now();
        int smallSum = smallList.stream().mapToInt(n -> n * n).sum();
        Duration sequentialSmall = Duration.between(start, Instant.now());
        
        start = Instant.now();
        int smallSumParallel = smallList.parallelStream().mapToInt(n -> n * n).sum();
        Duration parallelSmall = Duration.between(start, Instant.now());
        
        System.out.println("Small dataset (100 elements):");
        System.out.println("  Sequential: " + sequentialSmall.toNanos() + "ns");
        System.out.println("  Parallel: " + parallelSmall.toNanos() + "ns");
        System.out.println("  Recommendation: Use sequential for small datasets");
        
        // 2. Data structure considerations
        System.out.println("\nData structure splitting efficiency:");
        
        // ArrayList (excellent splitting)
        List<Integer> arrayList = new ArrayList<>(largeList);
        start = Instant.now();
        arrayList.parallelStream().mapToInt(n -> n * n).sum();
        Duration arrayListTime = Duration.between(start, Instant.now());
        
        // LinkedList (poor splitting)
        List<Integer> linkedList = new LinkedList<>(largeList);
        start = Instant.now();
        linkedList.parallelStream().mapToInt(n -> n * n).sum();
        Duration linkedListTime = Duration.between(start, Instant.now());
        
        System.out.println("ArrayList parallel processing: " + arrayListTime.toMillis() + "ms");
        System.out.println("LinkedList parallel processing: " + linkedListTime.toMillis() + "ms");
        System.out.println("Recommendation: Use ArrayList, arrays, or ranges for parallel streams");
        
        // 3. State mutation issues
        System.out.println("\nState mutation issues:");
        
        List<Integer> numbers = IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        
        // Wrong: shared mutable state
        List<Integer> sharedList = Collections.synchronizedList(new ArrayList<>());
        start = Instant.now();
        numbers.parallelStream()
            .filter(n -> n % 2 == 0)
            .forEach(sharedList::add); // Race condition possible!
        Duration mutatingTime = Duration.between(start, Instant.now());
        
        // Correct: collecting to result
        start = Instant.now();
        List<Integer> collectedList = numbers.parallelStream()
            .filter(n -> n % 2 == 0)
            .collect(Collectors.toList());
        Duration collectingTime = Duration.between(start, Instant.now());
        
        System.out.println("Shared mutable state: " + mutatingTime.toMillis() + 
            "ms, size: " + sharedList.size() + " (may be incorrect due to race conditions)");
        System.out.println("Proper collection: " + collectingTime.toMillis() + 
            "ms, size: " + collectedList.size());
        
        // 4. Side effects demonstration
        System.out.println("\nSide effects demonstration:");
        
        // Wrong: side effects in parallel streams
        AtomicInteger counter = new AtomicInteger(0);
        numbers.parallelStream()
            .filter(n -> n % 2 == 0)
            .forEach(n -> counter.incrementAndGet()); // Side effect!
        
        System.out.println("Side effect counter: " + counter.get());
        
        // Correct: pure functional approach
        long count = numbers.parallelStream()
            .filter(n -> n % 2 == 0)
            .count();
        
        System.out.println("Functional count: " + count);
        
        // 5. Exception handling in parallel streams
        System.out.println("\nException handling:");
        
        List<String> problematicData = Arrays.asList("1", "2", "not-a-number", "4", "5");
        
        // Handle exceptions properly
        List<Integer> parsedNumbers = problematicData.parallelStream()
            .map(s -> {
                try {
                    return Optional.of(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    return Optional.<Integer>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        
        System.out.println("Safely parsed numbers: " + parsedNumbers);
        
        // 6. Performance guidelines summary
        System.out.println("\nPerformance Guidelines Summary:");
        System.out.println("✓ Use parallel streams for:");
        System.out.println("  - Large datasets (>10,000 elements)");
        System.out.println("  - CPU-intensive operations");
        System.out.println("  - ArrayList, arrays, ranges");
        System.out.println("  - Independent operations");
        
        System.out.println("✗ Avoid parallel streams for:");
        System.out.println("  - Small datasets (<1,000 elements)");
        System.out.println("  - I/O-bound operations");
        System.out.println("  - LinkedList, Iterator-based collections");
        System.out.println("  - Operations with side effects");
        System.out.println("  - Non-associative operations");
    }
}
```

---

## 🔧 Part 3: Custom Collectors & Advanced Patterns (60 minutes)

### **Building Specialized Data Processing Tools**

Create `CustomCollectors.java`:

```java
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom collectors and advanced stream patterns
 */
public class CustomCollectors {
    
    public static void main(String[] args) {
        System.out.println("=== Custom Collectors & Advanced Patterns ===\n");
        
        // 1. Basic custom collectors
        System.out.println("1. Basic Custom Collectors:");
        demonstrateBasicCustomCollectors();
        
        // 2. Advanced custom collectors
        System.out.println("\n2. Advanced Custom Collectors:");
        demonstrateAdvancedCustomCollectors();
        
        // 3. Utility collectors
        System.out.println("\n3. Utility Collectors:");
        demonstrateUtilityCollectors();
        
        // 4. Performance-optimized collectors
        System.out.println("\n4. Performance-Optimized Collectors:");
        demonstratePerformanceCollectors();
        
        System.out.println("\n=== Custom Collectors Completed ===");
    }
    
    private static void demonstrateBasicCustomCollectors() {
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        
        // 1. Simple aggregation collector
        Collector<String, ?, String> joinWithCount = Collector.of(
            StringBuilder::new,                                    // supplier
            (sb, word) -> sb.append(word).append(","),            // accumulator
            StringBuilder::append,                                // combiner
            sb -> {                                              // finisher
                String result = sb.toString();
                if (result.endsWith(",")) {
                    result = result.substring(0, result.length() - 1);
                }
                int count = result.isEmpty() ? 0 : result.split(",").length;
                return result + " (" + count + " items)";
            }
        );
        
        String result = words.stream().collect(joinWithCount);
        System.out.println("Join with count: " + result);
        
        // 2. Statistical collector
        Collector<String, ?, Map<String, Object>> wordStatistics = Collector.of(
            () -> new HashMap<String, Object>() {{
                put("count", 0);
                put("totalLength", 0);
                put("shortest", "");
                put("longest", "");
            }},
            (map, word) -> {
                map.put("count", (Integer) map.get("count") + 1);
                map.put("totalLength", (Integer) map.get("totalLength") + word.length());
                
                String shortest = (String) map.get("shortest");
                if (shortest.isEmpty() || word.length() < shortest.length()) {
                    map.put("shortest", word);
                }
                
                String longest = (String) map.get("longest");
                if (word.length() > longest.length()) {
                    map.put("longest", word);
                }
            },
            (map1, map2) -> {
                map1.put("count", (Integer) map1.get("count") + (Integer) map2.get("count"));
                map1.put("totalLength", (Integer) map1.get("totalLength") + (Integer) map2.get("totalLength"));
                
                String shortest1 = (String) map1.get("shortest");
                String shortest2 = (String) map2.get("shortest");
                if (shortest1.isEmpty() || (!shortest2.isEmpty() && shortest2.length() < shortest1.length())) {
                    map1.put("shortest", shortest2);
                }
                
                String longest1 = (String) map1.get("longest");
                String longest2 = (String) map2.get("longest");
                if (longest2.length() > longest1.length()) {
                    map1.put("longest", longest2);
                }
                
                return map1;
            },
            map -> {
                int count = (Integer) map.get("count");
                int totalLength = (Integer) map.get("totalLength");
                map.put("averageLength", count > 0 ? (double) totalLength / count : 0.0);
                return map;
            }
        );
        
        Map<String, Object> stats = words.stream().collect(wordStatistics);
        System.out.println("Word statistics: " + stats);
        
        // 3. Immutable list collector
        Collector<String, ?, List<String>> toImmutableList = Collector.of(
            ArrayList::new,
            List::add,
            (list1, list2) -> { list1.addAll(list2); return list1; },
            Collections::unmodifiableList
        );
        
        List<String> immutableWords = words.stream()
            .filter(word -> word.length() > 4)
            .collect(toImmutableList);
        
        System.out.println("Immutable filtered list: " + immutableWords);
        // immutableWords.add("test"); // Would throw UnsupportedOperationException
    }
    
    private static void demonstrateAdvancedCustomCollectors() {
        List<Product> products = createSampleProducts();
        
        System.out.println("Product data:");
        products.forEach(System.out::println);
        
        // 1. Multi-criteria grouping collector
        Collector<Product, ?, Map<String, Map<String, List<Product>>>> multiGroupCollector = 
            Collector.of(
                HashMap::new,
                (map, product) -> {
                    String category = product.getCategory();
                    String priceRange = product.getPrice() > 500 ? "Expensive" : "Affordable";
                    
                    map.computeIfAbsent(category, k -> new HashMap<>())
                       .computeIfAbsent(priceRange, k -> new ArrayList<>())
                       .add(product);
                },
                (map1, map2) -> {
                    map2.forEach((category, priceMap) -> {
                        Map<String, List<Product>> existing = map1.computeIfAbsent(category, k -> new HashMap<>());
                        priceMap.forEach((priceRange, products_) -> 
                            existing.computeIfAbsent(priceRange, k -> new ArrayList<>()).addAll(products_));
                    });
                    return map1;
                }
            );
        
        Map<String, Map<String, List<Product>>> grouped = products.stream()
            .collect(multiGroupCollector);
        
        System.out.println("\nMulti-criteria grouping:");
        grouped.forEach((category, priceMap) -> {
            System.out.println(category + ":");
            priceMap.forEach((priceRange, productList) -> 
                System.out.println("  " + priceRange + ": " + productList.size() + " products"));
        });
        
        // 2. Top-N collector
        Collector<Product, ?, List<Product>> topNByPrice = topN(3, 
            Comparator.comparing(Product::getPrice).reversed());
        
        List<Product> topExpensive = products.stream().collect(topNByPrice);
        System.out.println("\nTop 3 most expensive products:");
        topExpensive.forEach(System.out::println);
        
        // 3. Conditional collector
        Collector<Product, ?, Map<String, Object>> conditionalAnalysis = Collector.of(
            () -> new ConditionalAnalysisAccumulator(),
            ConditionalAnalysisAccumulator::add,
            ConditionalAnalysisAccumulator::combine,
            ConditionalAnalysisAccumulator::finish
        );
        
        Map<String, Object> analysis = products.stream().collect(conditionalAnalysis);
        System.out.println("\nConditional analysis: " + analysis);
        
        // 4. Sliding window collector
        Collector<Product, ?, List<List<Product>>> slidingWindow = slidingWindow(3);
        
        List<List<Product>> windows = products.stream()
            .sorted(Comparator.comparing(Product::getPrice))
            .collect(slidingWindow);
        
        System.out.println("\nSliding windows (size 3):");
        for (int i = 0; i < Math.min(3, windows.size()); i++) {
            System.out.println("Window " + (i + 1) + ": " + windows.get(i));
        }
    }
    
    private static void demonstrateUtilityCollectors() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // 1. Batch collector
        Collector<Integer, ?, List<List<Integer>>> batchCollector = batching(3);
        
        List<List<Integer>> batches = numbers.stream().collect(batchCollector);
        System.out.println("Batches of 3: " + batches);
        
        // 2. Index mapping collector
        Collector<Integer, ?, Map<Integer, Integer>> indexMapping = Collector.of(
            HashMap::new,
            new IndexedAccumulator<Integer>()::accumulate,
            (map1, map2) -> { map1.putAll(map2); return map1; }
        );
        
        Map<Integer, Integer> indexed = numbers.stream()
            .filter(n -> n % 2 == 0)
            .collect(indexMapping);
        
        System.out.println("Index mapping (even numbers): " + indexed);
        
        // 3. Distinct by property collector
        List<String> words = Arrays.asList("apple", "apricot", "banana", "berry", "cherry");
        
        Collector<String, ?, List<String>> distinctByFirstLetter = distinctBy(word -> word.charAt(0));
        
        List<String> distinctWords = words.stream().collect(distinctByFirstLetter);
        System.out.println("Distinct by first letter: " + distinctWords);
        
        // 4. Frequency collector
        Collector<String, ?, Map<Character, Long>> charFrequency = Collector.of(
            HashMap::new,
            (map, word) -> {
                for (char c : word.toCharArray()) {
                    map.merge(c, 1L, Long::sum);
                }
            },
            (map1, map2) -> {
                map2.forEach((char_, count) -> map1.merge(char_, count, Long::sum));
                return map1;
            }
        );
        
        Map<Character, Long> frequencies = words.stream().collect(charFrequency);
        System.out.println("Character frequencies: " + frequencies);
    }
    
    private static void demonstratePerformanceCollectors() {
        List<Transaction> transactions = createSampleTransactions();
        
        System.out.println("Analyzing " + transactions.size() + " transactions");
        
        // 1. Parallel-optimized collector
        Collector<Transaction, ?, ConcurrentHashMap<String, Double>> parallelGrouping = 
            Collector.of(
                ConcurrentHashMap::new,
                (map, transaction) -> 
                    map.merge(transaction.getCategory(), transaction.getAmount(), Double::sum),
                (map1, map2) -> {
                    map2.forEach((category, amount) -> 
                        map1.merge(category, amount, Double::sum));
                    return map1;
                },
                Function.identity(),
                Collector.Characteristics.CONCURRENT, 
                Collector.Characteristics.UNORDERED
            );
        
        long start = System.nanoTime();
        ConcurrentHashMap<String, Double> parallelResult = transactions.parallelStream()
            .collect(parallelGrouping);
        long parallelTime = System.nanoTime() - start;
        
        start = System.nanoTime();
        Map<String, Double> sequentialResult = transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingDouble(Transaction::getAmount)
            ));
        long sequentialTime = System.nanoTime() - start;
        
        System.out.println("Parallel collection: " + parallelTime / 1_000_000 + "ms");
        System.out.println("Sequential collection: " + sequentialTime / 1_000_000 + "ms");
        System.out.println("Results match: " + 
            parallelResult.equals(new HashMap<>(sequentialResult)));
        
        // 2. Memory-efficient collector for large datasets
        Collector<Transaction, ?, DoubleSummaryStatistics> memoryEfficientStats = 
            Collector.of(
                DoubleSummaryStatistics::new,
                (stats, transaction) -> stats.accept(transaction.getAmount()),
                (stats1, stats2) -> { stats1.combine(stats2); return stats1; }
            );
        
        DoubleSummaryStatistics stats = transactions.stream()
            .collect(memoryEfficientStats);
        
        System.out.println("Memory-efficient statistics: " + stats);
        
        // 3. Early termination collector
        Collector<Transaction, ?, Optional<Transaction>> firstHighValue = Collector.of(
            () -> new OptionalAccumulator<Transaction>(),
            (acc, transaction) -> {
                if (!acc.isFound() && transaction.getAmount() > 5000) {
                    acc.setValue(transaction);
                }
            },
            (acc1, acc2) -> acc1.isFound() ? acc1 : acc2,
            OptionalAccumulator::getResult
        );
        
        Optional<Transaction> highValueTransaction = transactions.stream()
            .collect(firstHighValue);
        
        System.out.println("First high-value transaction: " + highValueTransaction);
    }
    
    // Utility methods for custom collectors
    public static <T> Collector<T, ?, List<T>> topN(int n, Comparator<T> comparator) {
        return Collector.of(
            () -> new PriorityQueue<>(comparator.reversed()),
            (queue, item) -> {
                queue.offer(item);
                if (queue.size() > n) {
                    queue.poll();
                }
            },
            (queue1, queue2) -> {
                queue1.addAll(queue2);
                return queue1.stream()
                    .sorted(comparator.reversed())
                    .limit(n)
                    .collect(Collectors.toCollection(() -> new PriorityQueue<>(comparator.reversed())));
            },
            queue -> queue.stream()
                .sorted(comparator)
                .collect(Collectors.toList())
        );
    }
    
    public static <T> Collector<T, ?, List<List<T>>> slidingWindow(int windowSize) {
        return Collector.of(
            ArrayList::new,
            List::add,
            (list1, list2) -> { list1.addAll(list2); return list1; },
            list -> {
                List<List<T>> windows = new ArrayList<>();
                for (int i = 0; i <= list.size() - windowSize; i++) {
                    windows.add(new ArrayList<>(list.subList(i, i + windowSize)));
                }
                return windows;
            }
        );
    }
    
    public static <T> Collector<T, ?, List<List<T>>> batching(int batchSize) {
        return Collector.of(
            ArrayList::new,
            List::add,
            (list1, list2) -> { list1.addAll(list2); return list1; },
            list -> {
                List<List<T>> batches = new ArrayList<>();
                for (int i = 0; i < list.size(); i += batchSize) {
                    batches.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
                }
                return batches;
            }
        );
    }
    
    public static <T, K> Collector<T, ?, List<T>> distinctBy(Function<T, K> keyExtractor) {
        return Collector.of(
            () -> new LinkedHashMap<K, T>(),
            (map, item) -> map.putIfAbsent(keyExtractor.apply(item), item),
            (map1, map2) -> {
                map2.forEach(map1::putIfAbsent);
                return map1;
            },
            map -> new ArrayList<>(map.values())
        );
    }
    
    // Helper classes for custom collectors
    static class ConditionalAnalysisAccumulator {
        private int expensiveCount = 0;
        private int affordableCount = 0;
        private double totalValue = 0;
        private String mostExpensiveCategory = "";
        private double maxPrice = 0;
        
        void add(Product product) {
            if (product.getPrice() > 500) {
                expensiveCount++;
            } else {
                affordableCount++;
            }
            
            totalValue += product.getPrice();
            
            if (product.getPrice() > maxPrice) {
                maxPrice = product.getPrice();
                mostExpensiveCategory = product.getCategory();
            }
        }
        
        ConditionalAnalysisAccumulator combine(ConditionalAnalysisAccumulator other) {
            this.expensiveCount += other.expensiveCount;
            this.affordableCount += other.affordableCount;
            this.totalValue += other.totalValue;
            
            if (other.maxPrice > this.maxPrice) {
                this.maxPrice = other.maxPrice;
                this.mostExpensiveCategory = other.mostExpensiveCategory;
            }
            
            return this;
        }
        
        Map<String, Object> finish() {
            Map<String, Object> result = new HashMap<>();
            result.put("expensiveCount", expensiveCount);
            result.put("affordableCount", affordableCount);
            result.put("totalValue", totalValue);
            result.put("averagePrice", (expensiveCount + affordableCount) > 0 ? 
                totalValue / (expensiveCount + affordableCount) : 0);
            result.put("mostExpensiveCategory", mostExpensiveCategory);
            result.put("maxPrice", maxPrice);
            return result;
        }
    }
    
    static class IndexedAccumulator<T> {
        private int index = 0;
        
        void accumulate(Map<Integer, T> map, T item) {
            map.put(index++, item);
        }
    }
    
    static class OptionalAccumulator<T> {
        private T value;
        private boolean found = false;
        
        void setValue(T value) {
            if (!found) {
                this.value = value;
                this.found = true;
            }
        }
        
        boolean isFound() {
            return found;
        }
        
        Optional<T> getResult() {
            return found ? Optional.of(value) : Optional.empty();
        }
    }
    
    // Sample data creation methods
    private static List<Product> createSampleProducts() {
        return Arrays.asList(
            new Product("Laptop", "Electronics", 999.99),
            new Product("Mouse", "Electronics", 29.99),
            new Product("Desk", "Furniture", 299.99),
            new Product("Chair", "Furniture", 199.99),
            new Product("Monitor", "Electronics", 399.99),
            new Product("Book", "Education", 19.99),
            new Product("Tablet", "Electronics", 499.99),
            new Product("Lamp", "Furniture", 79.99)
        );
    }
    
    private static List<Transaction> createSampleTransactions() {
        Random random = new Random(42);
        String[] categories = {"Food", "Transport", "Entertainment", "Shopping", "Bills"};
        
        return IntStream.rangeClosed(1, 1000)
            .mapToObj(i -> new Transaction(
                "TX" + i,
                categories[random.nextInt(categories.length)],
                100 + random.nextDouble() * 5000
            ))
            .collect(Collectors.toList());
    }
    
    // Helper classes
    static class Product {
        private final String name;
        private final String category;
        private final double price;
        
        public Product(String name, String category, double price) {
            this.name = name;
            this.category = category;
            this.price = price;
        }
        
        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }
        
        @Override
        public String toString() {
            return String.format("%s (%s): $%.2f", name, category, price);
        }
    }
    
    static class Transaction {
        private final String id;
        private final String category;
        private final double amount;
        
        public Transaction(String id, String category, double amount) {
            this.id = id;
            this.category = category;
            this.amount = amount;
        }
        
        public String getId() { return id; }
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        
        @Override
        public String toString() {
            return String.format("%s: %s $%.2f", id, category, amount);
        }
    }
}
```

---

## 🎯 Summary

**Congratulations!** You've completed **Day 38: Advanced Stream Operations** and mastered:

- **Advanced Collectors**: Complex grouping, teeing, filtering, and custom aggregation patterns
- **Parallel Streams**: Performance characteristics, thread pool management, and optimization techniques
- **Custom Collectors**: Building specialized collectors for complex data processing requirements
- **Performance Optimization**: Understanding when and how to use parallel processing effectively
- **Advanced Patterns**: Function composition, conditional processing, and stream pipeline design
- **Best Practices**: Avoiding pitfalls, handling exceptions, and choosing appropriate data structures

**Key Takeaways:**
- Advanced collectors enable sophisticated data aggregation and transformation
- Parallel streams provide significant performance benefits for CPU-intensive operations on large datasets
- Custom collectors allow specialized data processing that's not available with built-in collectors
- Performance optimization requires understanding data structures, operation characteristics, and parallelization overhead
- Proper error handling and avoiding side effects are crucial for robust stream operations

**Performance Guidelines:**
- Use parallel streams for large datasets (>10,000 elements) with CPU-intensive operations
- Avoid parallel streams for I/O-bound operations or small datasets
- Choose ArrayList, arrays, or ranges for optimal parallel performance
- Implement associative and stateless operations for parallel safety
- Monitor thread usage and consider custom ForkJoinPool when appropriate

**Next Steps:** Tomorrow we'll explore **Optional Class & Null Safety** to eliminate null pointer exceptions and write more robust code.

**🏆 Day 38 Complete!** You now have advanced skills in stream processing and can build high-performance data processing applications using sophisticated stream operations and custom collectors.