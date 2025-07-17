# Day 42: Week 6 Capstone - Functional Data Processing System (August 20, 2025)

**Date:** August 20, 2025  
**Duration:** 4 hours  
**Focus:** Build a comprehensive functional data processing system integrating all Week 6 concepts

---

## 🎯 Today's Learning Goals

By the end of this session, you'll have built:

- ✅ A complete functional data processing pipeline using Streams and Lambda expressions
- ✅ Modern data models using Records and Sealed Classes
- ✅ Robust null safety with Optional patterns
- ✅ Advanced functional composition and method references
- ✅ Modern Java features for clean, maintainable code
- ✅ A real-world application demonstrating enterprise-level functional programming

---

## 🏗️ Capstone Project: Enterprise Data Analytics Platform

**Project Overview:** Build a comprehensive data analytics platform that processes sales, customer, and product data using modern functional programming techniques.

**Core Features:**
- **Data Ingestion**: Reading and parsing various data formats
- **Data Transformation**: Complex data processing pipelines
- **Analytics Engine**: Statistical analysis and reporting
- **Data Export**: Multiple output formats with functional compositions
- **Error Handling**: Robust error management with Optional and Result types
- **Modern Java Integration**: Using latest Java features throughout

### **System Architecture**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Data Sources  │ -> │ Processing Core  │ -> │   Analytics     │
│                 │    │                  │    │                 │
│ • CSV Files     │    │ • Stream API     │    │ • Statistical   │
│ • JSON Data     │    │ • Functional     │    │ • Aggregation   │
│ • HTTP APIs     │    │   Composition    │    │ • Reporting     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                       ┌──────────────────┐
                       │   Output Layer   │
                       │                  │
                       │ • Reports        │
                       │ • Dashboards     │
                       │ • Exports        │
                       └──────────────────┘
```

---

## 🚀 Phase 1: Core Data Models and Infrastructure (60 minutes)

### **Modern Data Models with Records and Sealed Classes**

Create `DataModels.java`:

```java
import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * Modern data models using Records and Sealed Classes for the analytics platform
 */
public class DataModels {
    
    // Core business entities using records
    public record Customer(
        String id,
        String name,
        String email,
        LocalDate registrationDate,
        CustomerTier tier,
        Address address,
        Optional<String> phoneNumber
    ) {
        // Validation constructor
        public Customer {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Customer ID cannot be blank");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Customer name cannot be blank");
            }
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Valid email required");
            }
            if (registrationDate == null) {
                throw new IllegalArgumentException("Registration date required");
            }
        }
        
        // Business methods
        public boolean isLongTermCustomer() {
            return ChronoUnit.YEARS.between(registrationDate, LocalDate.now()) >= 2;
        }
        
        public boolean isPremiumTier() {
            return tier == CustomerTier.PREMIUM || tier == CustomerTier.VIP;
        }
        
        public String getDisplayName() {
            return name + " (" + tier.getDisplayName() + ")";
        }
    }
    
    public record Product(
        String id,
        String name,
        String category,
        double price,
        Optional<String> description,
        LocalDate launchDate,
        ProductStatus status
    ) {
        public Product {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Product ID cannot be blank");
            }
            if (price < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
        }
        
        public boolean isNewProduct() {
            return ChronoUnit.MONTHS.between(launchDate, LocalDate.now()) <= 6;
        }
        
        public boolean isAvailable() {
            return status == ProductStatus.ACTIVE;
        }
        
        public String getFormattedPrice() {
            return String.format("$%.2f", price);
        }
    }
    
    public record Sale(
        String id,
        String customerId,
        String productId,
        int quantity,
        double unitPrice,
        double totalAmount,
        LocalDateTime saleDate,
        PaymentMethod paymentMethod,
        Optional<String> discountCode
    ) {
        public Sale {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (unitPrice < 0) {
                throw new IllegalArgumentException("Unit price cannot be negative");
            }
            if (Math.abs(totalAmount - (quantity * unitPrice)) > 0.01) {
                throw new IllegalArgumentException("Total amount must equal quantity * unit price");
            }
        }
        
        public boolean hasDiscount() {
            return discountCode.isPresent();
        }
        
        public boolean isRecentSale() {
            return ChronoUnit.DAYS.between(saleDate.toLocalDate(), LocalDate.now()) <= 30;
        }
        
        public double getRevenue() {
            return totalAmount;
        }
    }
    
    public record Address(
        String street,
        String city,
        String state,
        String zipCode,
        String country
    ) {
        public String getFullAddress() {
            return String.format("%s, %s, %s %s, %s", street, city, state, zipCode, country);
        }
        
        public boolean isUSAddress() {
            return "USA".equalsIgnoreCase(country) || "US".equalsIgnoreCase(country);
        }
    }
    
    // Enums for type safety
    public enum CustomerTier {
        BASIC("Basic"),
        PREMIUM("Premium"), 
        VIP("VIP");
        
        private final String displayName;
        
        CustomerTier(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ProductStatus {
        ACTIVE, DISCONTINUED, OUT_OF_STOCK, COMING_SOON
    }
    
    public enum PaymentMethod {
        CREDIT_CARD("Credit Card"),
        DEBIT_CARD("Debit Card"),
        PAYPAL("PayPal"),
        BANK_TRANSFER("Bank Transfer"),
        CASH("Cash");
        
        private final String displayName;
        
        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Sealed classes for result types and data processing states
    public sealed interface ProcessingResult<T> permits Success, Warning, Error {
        
        default boolean isSuccess() {
            return this instanceof Success<T>;
        }
        
        default boolean hasWarnings() {
            return this instanceof Warning<T>;
        }
        
        default boolean isError() {
            return this instanceof Error<T>;
        }
        
        default Optional<T> getValue() {
            return switch (this) {
                case Success<T>(var value) -> Optional.of(value);
                case Warning<T>(var value, var warnings) -> Optional.of(value);
                case Error<T>(var message) -> Optional.empty();
            };
        }
        
        default String getMessage() {
            return switch (this) {
                case Success<T>(var value) -> "Success";
                case Warning<T>(var value, var warnings) -> "Warning: " + String.join(", ", warnings);
                case Error<T>(var message) -> "Error: " + message;
            };
        }
    }
    
    public record Success<T>(T value) implements ProcessingResult<T> {}
    
    public record Warning<T>(T value, List<String> warnings) implements ProcessingResult<T> {}
    
    public record Error<T>(String message) implements ProcessingResult<T> {}
    
    // Analytics result models
    public record SalesMetrics(
        double totalRevenue,
        int totalTransactions,
        double averageTransactionValue,
        double medianTransactionValue,
        int uniqueCustomers,
        int uniqueProducts,
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        public String getFormattedRevenue() {
            return String.format("$%,.2f", totalRevenue);
        }
        
        public String getSummary() {
            return String.format(
                "Period: %s to %s | Revenue: %s | Transactions: %,d | Avg: $%.2f",
                periodStart, periodEnd, getFormattedRevenue(), totalTransactions, averageTransactionValue
            );
        }
    }
    
    public record CustomerAnalysis(
        String customerId,
        String customerName,
        int totalPurchases,
        double totalSpent,
        double averageOrderValue,
        LocalDate firstPurchase,
        LocalDate lastPurchase,
        List<String> favoriteCategories,
        CustomerSegment segment
    ) {
        public boolean isActiveCustomer() {
            return ChronoUnit.DAYS.between(lastPurchase, LocalDate.now()) <= 90;
        }
        
        public String getLifetimeValueCategory() {
            return switch (segment) {
                case HIGH_VALUE -> "High Value ($1000+)";
                case MEDIUM_VALUE -> "Medium Value ($500-$999)";
                case LOW_VALUE -> "Low Value (<$500)";
                case INACTIVE -> "Inactive";
            };
        }
    }
    
    public record ProductPerformance(
        String productId,
        String productName,
        String category,
        int unitsSold,
        double revenue,
        double averageRating,
        int numberOfReviews,
        LocalDate firstSale,
        LocalDate lastSale
    ) {
        public boolean isPopularProduct() {
            return unitsSold > 100 && averageRating >= 4.0;
        }
        
        public String getPerformanceCategory() {
            if (unitsSold > 500) return "Best Seller";
            if (unitsSold > 100) return "Popular";
            if (unitsSold > 50) return "Moderate";
            return "Slow Moving";
        }
    }
    
    public enum CustomerSegment {
        HIGH_VALUE, MEDIUM_VALUE, LOW_VALUE, INACTIVE
    }
    
    // Configuration records
    public record AnalyticsConfig(
        LocalDate analysisStartDate,
        LocalDate analysisEndDate,
        boolean includeInactiveCustomers,
        boolean includeDiscontinuedProducts,
        double highValueCustomerThreshold,
        int minimumTransactionsForAnalysis
    ) {
        public AnalyticsConfig {
            if (analysisStartDate.isAfter(analysisEndDate)) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
            if (highValueCustomerThreshold < 0) {
                throw new IllegalArgumentException("High value threshold cannot be negative");
            }
        }
        
        public static AnalyticsConfig defaultConfig() {
            return new AnalyticsConfig(
                LocalDate.now().minusYears(1),
                LocalDate.now(),
                false,
                false,
                1000.0,
                5
            );
        }
        
        public Period getAnalysisPeriod() {
            return Period.between(analysisStartDate, analysisEndDate);
        }
    }
    
    public record DataQualityReport(
        int totalRecords,
        int validRecords,
        int invalidRecords,
        List<String> validationErrors,
        Map<String, Integer> errorCounts,
        double dataQualityScore
    ) {
        public boolean hasGoodQuality() {
            return dataQualityScore >= 0.95;
        }
        
        public String getQualityGrade() {
            if (dataQualityScore >= 0.95) return "Excellent";
            if (dataQualityScore >= 0.90) return "Good";
            if (dataQualityScore >= 0.80) return "Fair";
            return "Poor";
        }
        
        public String getSummary() {
            return String.format(
                "Data Quality: %s (%.1f%%) | Valid: %d/%d | Errors: %d",
                getQualityGrade(), dataQualityScore * 100, validRecords, totalRecords, invalidRecords
            );
        }
    }
}
```

---

## 🔧 Phase 2: Functional Data Processing Engine (90 minutes)

### **Stream-Based Data Processing with Functional Composition**

Create `DataProcessingEngine.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import static java.util.stream.Collectors.*;

/**
 * Functional data processing engine using Streams, Optional, and method references
 */
public class DataProcessingEngine {
    
    private final List<DataModels.Customer> customers = new ArrayList<>();
    private final List<DataModels.Product> products = new ArrayList<>();
    private final List<DataModels.Sale> sales = new ArrayList<>();
    private final DataModels.AnalyticsConfig config;
    
    public DataProcessingEngine(DataModels.AnalyticsConfig config) {
        this.config = config;
    }
    
    public static void main(String[] args) {
        System.out.println("=== Functional Data Processing Engine ===\n");
        
        var config = DataModels.AnalyticsConfig.defaultConfig();
        var engine = new DataProcessingEngine(config);
        
        // 1. Data generation and loading
        System.out.println("1. Loading Sample Data:");
        engine.loadSampleData();
        
        // 2. Data validation and quality assessment
        System.out.println("\n2. Data Quality Assessment:");
        engine.performDataQualityAnalysis();
        
        // 3. Customer analytics
        System.out.println("\n3. Customer Analytics:");
        engine.performCustomerAnalytics();
        
        // 4. Product performance analysis
        System.out.println("\n4. Product Performance Analysis:");
        engine.performProductAnalysis();
        
        // 5. Sales trend analysis
        System.out.println("\n5. Sales Trend Analysis:");
        engine.performSalesTrendAnalysis();
        
        // 6. Advanced analytics with parallel processing
        System.out.println("\n6. Advanced Parallel Analytics:");
        engine.performParallelAnalytics();
        
        System.out.println("\n=== Data Processing Engine Completed ===");
    }
    
    private void loadSampleData() {
        // Generate sample customers using functional programming
        var customerGenerator = createCustomerGenerator();
        var generatedCustomers = Stream.generate(customerGenerator)
            .limit(1000)
            .collect(toList());
        customers.addAll(generatedCustomers);
        
        // Generate sample products
        var productGenerator = createProductGenerator();
        var generatedProducts = Stream.generate(productGenerator)
            .limit(200)
            .collect(toList());
        products.addAll(generatedProducts);
        
        // Generate sample sales
        var salesGenerator = createSalesGenerator();
        var generatedSales = Stream.generate(salesGenerator)
            .limit(5000)
            .collect(toList());
        sales.addAll(generatedSales);
        
        System.out.printf("Loaded: %d customers, %d products, %d sales%n",
            customers.size(), products.size(), sales.size());
    }
    
    private Supplier<DataModels.Customer> createCustomerGenerator() {
        var random = new Random();
        var firstNames = List.of("John", "Jane", "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry");
        var lastNames = List.of("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez");
        var domains = List.of("@gmail.com", "@yahoo.com", "@hotmail.com", "@company.com", "@business.org");
        var tiers = DataModels.CustomerTier.values();
        var states = List.of("CA", "NY", "TX", "FL", "IL", "PA", "OH", "GA", "NC", "MI");
        
        return () -> {
            var firstName = firstNames.get(random.nextInt(firstNames.size()));
            var lastName = lastNames.get(random.nextInt(lastNames.size()));
            var email = firstName.toLowerCase() + "." + lastName.toLowerCase() + domains.get(random.nextInt(domains.size()));
            var tier = tiers[random.nextInt(tiers.length)];
            var registrationDate = LocalDate.now().minusDays(random.nextInt(1000));
            
            var address = new DataModels.Address(
                (random.nextInt(9999) + 1) + " " + lastNames.get(random.nextInt(lastNames.size())) + " St",
                "City" + random.nextInt(100),
                states.get(random.nextInt(states.size())),
                String.format("%05d", random.nextInt(100000)),
                "USA"
            );
            
            var phoneNumber = random.nextBoolean() ? 
                Optional.of(String.format("(%03d) %03d-%04d", 
                    random.nextInt(900) + 100,
                    random.nextInt(900) + 100,
                    random.nextInt(10000))) :
                Optional.<String>empty();
            
            return new DataModels.Customer(
                "CUST-" + String.format("%06d", random.nextInt(1000000)),
                firstName + " " + lastName,
                email,
                registrationDate,
                tier,
                address,
                phoneNumber
            );
        };
    }
    
    private Supplier<DataModels.Product> createProductGenerator() {
        var random = new Random();
        var categories = List.of("Electronics", "Clothing", "Books", "Home & Garden", "Sports", "Toys", "Automotive");
        var productNames = Map.of(
            "Electronics", List.of("Laptop", "Smartphone", "Tablet", "Headphones", "Camera", "Speaker"),
            "Clothing", List.of("T-Shirt", "Jeans", "Dress", "Jacket", "Shoes", "Hat"),
            "Books", List.of("Novel", "Textbook", "Biography", "Cookbook", "Travel Guide", "Manual"),
            "Home & Garden", List.of("Chair", "Table", "Lamp", "Plant", "Tool Set", "Decoration"),
            "Sports", List.of("Baseball", "Soccer Ball", "Tennis Racket", "Running Shoes", "Gym Equipment", "Bicycle"),
            "Toys", List.of("Action Figure", "Board Game", "Puzzle", "Doll", "Building Blocks", "Remote Car"),
            "Automotive", List.of("Car Parts", "Oil", "Tires", "Battery", "GPS", "Car Cover")
        );
        var statuses = DataModels.ProductStatus.values();
        
        return () -> {
            var category = categories.get(random.nextInt(categories.size()));
            var nameOptions = productNames.get(category);
            var baseName = nameOptions.get(random.nextInt(nameOptions.size()));
            var name = baseName + " " + (char)('A' + random.nextInt(26)) + random.nextInt(1000);
            var price = 10.0 + random.nextDouble() * 1000.0;
            var launchDate = LocalDate.now().minusDays(random.nextInt(2000));
            var status = statuses[random.nextInt(statuses.length)];
            
            var description = random.nextBoolean() ? 
                Optional.of("High quality " + baseName.toLowerCase() + " with premium features") :
                Optional.<String>empty();
            
            return new DataModels.Product(
                "PROD-" + String.format("%06d", random.nextInt(1000000)),
                name,
                category,
                Math.round(price * 100.0) / 100.0,
                description,
                launchDate,
                status
            );
        };
    }
    
    private Supplier<DataModels.Sale> createSalesGenerator() {
        var random = new Random();
        var paymentMethods = DataModels.PaymentMethod.values();
        var discountCodes = List.of("SAVE10", "WELCOME20", "PREMIUM15", "BULK25", "STUDENT10");
        
        return () -> {
            if (customers.isEmpty() || products.isEmpty()) {
                throw new IllegalStateException("Customers and products must be loaded first");
            }
            
            var customer = customers.get(random.nextInt(customers.size()));
            var product = products.get(random.nextInt(products.size()));
            var quantity = random.nextInt(5) + 1;
            var unitPrice = product.price() * (0.8 + random.nextDouble() * 0.4); // Some price variation
            var totalAmount = quantity * unitPrice;
            var saleDate = LocalDateTime.now().minusDays(random.nextInt(365));
            var paymentMethod = paymentMethods[random.nextInt(paymentMethods.length)];
            
            var discountCode = random.nextBoolean() && random.nextDouble() < 0.3 ? 
                Optional.of(discountCodes.get(random.nextInt(discountCodes.size()))) :
                Optional.<String>empty();
            
            return new DataModels.Sale(
                "SALE-" + String.format("%08d", random.nextInt(100000000)),
                customer.id(),
                product.id(),
                quantity,
                Math.round(unitPrice * 100.0) / 100.0,
                Math.round(totalAmount * 100.0) / 100.0,
                saleDate,
                paymentMethod,
                discountCode
            );
        };
    }
    
    private void performDataQualityAnalysis() {
        // Functional data validation pipeline
        var customerValidation = validateData(customers, this::validateCustomer, "Customer");
        var productValidation = validateData(products, this::validateProduct, "Product");
        var salesValidation = validateData(sales, this::validateSale, "Sale");
        
        // Combine validation results
        var totalRecords = customers.size() + products.size() + sales.size();
        var totalValid = customerValidation.validRecords() + productValidation.validRecords() + salesValidation.validRecords();
        var totalInvalid = customerValidation.invalidRecords() + productValidation.invalidRecords() + salesValidation.invalidRecords();
        
        var combinedErrors = Stream.of(customerValidation, productValidation, salesValidation)
            .flatMap(report -> report.validationErrors().stream())
            .collect(toList());
        
        var combinedErrorCounts = Stream.of(customerValidation, productValidation, salesValidation)
            .flatMap(report -> report.errorCounts().entrySet().stream())
            .collect(groupingBy(Map.Entry::getKey, summingInt(Map.Entry::getValue)));
        
        var qualityScore = totalRecords > 0 ? (double) totalValid / totalRecords : 1.0;
        
        var overallReport = new DataModels.DataQualityReport(
            totalRecords, totalValid, totalInvalid, combinedErrors, combinedErrorCounts, qualityScore
        );
        
        System.out.println("Data Quality Assessment:");
        System.out.println("  " + overallReport.getSummary());
        System.out.println("  Customer Data: " + customerValidation.getSummary());
        System.out.println("  Product Data: " + productValidation.getSummary());
        System.out.println("  Sales Data: " + salesValidation.getSummary());
        
        if (!overallReport.hasGoodQuality()) {
            System.out.println("  ⚠️ Data quality below threshold. Top errors:");
            overallReport.errorCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> System.out.println("    " + entry.getKey() + ": " + entry.getValue() + " occurrences"));
        }
    }
    
    private <T> DataModels.DataQualityReport validateData(List<T> data, Function<T, List<String>> validator, String dataType) {
        var validationResults = data.stream()
            .map(validator)
            .collect(toList());
        
        var validRecords = (int) validationResults.stream().filter(List::isEmpty).count();
        var invalidRecords = data.size() - validRecords;
        
        var allErrors = validationResults.stream()
            .flatMap(Collection::stream)
            .collect(toList());
        
        var errorCounts = allErrors.stream()
            .collect(groupingBy(Function.identity(), summingInt(e -> 1)));
        
        var qualityScore = data.size() > 0 ? (double) validRecords / data.size() : 1.0;
        
        return new DataModels.DataQualityReport(
            data.size(), validRecords, invalidRecords, allErrors, errorCounts, qualityScore
        );
    }
    
    private List<String> validateCustomer(DataModels.Customer customer) {
        var errors = new ArrayList<String>();
        
        if (customer.name().isBlank()) errors.add("Customer name is blank");
        if (!customer.email().contains("@")) errors.add("Invalid email format");
        if (customer.registrationDate().isAfter(LocalDate.now())) errors.add("Registration date in future");
        
        return errors;
    }
    
    private List<String> validateProduct(DataModels.Product product) {
        var errors = new ArrayList<String>();
        
        if (product.name().isBlank()) errors.add("Product name is blank");
        if (product.price() < 0) errors.add("Negative price");
        if (product.launchDate().isAfter(LocalDate.now())) errors.add("Launch date in future");
        
        return errors;
    }
    
    private List<String> validateSale(DataModels.Sale sale) {
        var errors = new ArrayList<String>();
        
        if (sale.quantity() <= 0) errors.add("Invalid quantity");
        if (sale.unitPrice() < 0) errors.add("Negative unit price");
        if (sale.totalAmount() < 0) errors.add("Negative total amount");
        if (sale.saleDate().isAfter(LocalDateTime.now())) errors.add("Sale date in future");
        
        return errors;
    }
    
    private void performCustomerAnalytics() {
        // Customer segmentation using functional programming
        var customerAnalytics = customers.stream()
            .map(this::analyzeCustomer)
            .filter(analysis -> analysis.totalPurchases() >= config.minimumTransactionsForAnalysis())
            .collect(toList());
        
        // Customer segmentation summary
        var segmentDistribution = customerAnalytics.stream()
            .collect(groupingBy(DataModels.CustomerAnalysis::segment, counting()));
        
        System.out.println("Customer Segmentation:");
        segmentDistribution.entrySet().stream()
            .sorted(Map.Entry.<DataModels.CustomerSegment, Long>comparingByValue().reversed())
            .forEach(entry -> System.out.printf("  %s: %d customers%n", entry.getKey(), entry.getValue()));
        
        // Top customers by value
        var topCustomers = customerAnalytics.stream()
            .sorted(Comparator.comparingDouble(DataModels.CustomerAnalysis::totalSpent).reversed())
            .limit(5)
            .collect(toList());
        
        System.out.println("\nTop 5 Customers by Total Spent:");
        topCustomers.forEach(customer -> 
            System.out.printf("  %s: $%.2f (%d purchases, avg: $%.2f)%n",
                customer.customerName(), customer.totalSpent(), 
                customer.totalPurchases(), customer.averageOrderValue()));
        
        // Customer retention analysis
        var activeCustomers = customerAnalytics.stream()
            .filter(DataModels.CustomerAnalysis::isActiveCustomer)
            .count();
        
        var retentionRate = customerAnalytics.isEmpty() ? 0.0 : 
            (double) activeCustomers / customerAnalytics.size() * 100;
        
        System.out.printf("\nCustomer Retention: %.1f%% (%d/%d customers active)%n",
            retentionRate, activeCustomers, customerAnalytics.size());
        
        // Category preferences analysis
        var categoryPreferences = customerAnalytics.stream()
            .flatMap(analysis -> analysis.favoriteCategories().stream())
            .collect(groupingBy(Function.identity(), counting()));
        
        System.out.println("\nMost Popular Categories:");
        categoryPreferences.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> System.out.printf("  %s: %d customers%n", entry.getKey(), entry.getValue()));
    }
    
    private DataModels.CustomerAnalysis analyzeCustomer(DataModels.Customer customer) {
        var customerSales = sales.stream()
            .filter(sale -> sale.customerId().equals(customer.id()))
            .collect(toList());
        
        if (customerSales.isEmpty()) {
            return new DataModels.CustomerAnalysis(
                customer.id(), customer.name(), 0, 0.0, 0.0,
                LocalDate.now(), LocalDate.now().minusYears(10),
                List.of(), DataModels.CustomerSegment.INACTIVE
            );
        }
        
        var totalSpent = customerSales.stream()
            .mapToDouble(DataModels.Sale::totalAmount)
            .sum();
        
        var averageOrderValue = totalSpent / customerSales.size();
        
        var firstPurchase = customerSales.stream()
            .map(sale -> sale.saleDate().toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
        
        var lastPurchase = customerSales.stream()
            .map(sale -> sale.saleDate().toLocalDate())
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now().minusYears(10));
        
        var favoriteCategories = customerSales.stream()
            .map(DataModels.Sale::productId)
            .map(this::findProductById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(DataModels.Product::category)
            .collect(groupingBy(Function.identity(), counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(toList());
        
        var segment = determineCustomerSegment(totalSpent, customerSales.size(), lastPurchase);
        
        return new DataModels.CustomerAnalysis(
            customer.id(), customer.name(), customerSales.size(), totalSpent,
            averageOrderValue, firstPurchase, lastPurchase, favoriteCategories, segment
        );
    }
    
    private Optional<DataModels.Product> findProductById(String productId) {
        return products.stream()
            .filter(product -> product.id().equals(productId))
            .findFirst();
    }
    
    private DataModels.CustomerSegment determineCustomerSegment(double totalSpent, int purchaseCount, LocalDate lastPurchase) {
        var daysSinceLastPurchase = ChronoUnit.DAYS.between(lastPurchase, LocalDate.now());
        
        if (daysSinceLastPurchase > 180) {
            return DataModels.CustomerSegment.INACTIVE;
        }
        
        if (totalSpent >= config.highValueCustomerThreshold()) {
            return DataModels.CustomerSegment.HIGH_VALUE;
        } else if (totalSpent >= 500.0) {
            return DataModels.CustomerSegment.MEDIUM_VALUE;
        } else {
            return DataModels.CustomerSegment.LOW_VALUE;
        }
    }
    
    private void performProductAnalysis() {
        var productPerformance = products.stream()
            .map(this::analyzeProduct)
            .filter(perf -> perf.unitsSold() > 0)
            .collect(toList());
        
        // Top performing products
        var topProducts = productPerformance.stream()
            .sorted(Comparator.comparingDouble(DataModels.ProductPerformance::revenue).reversed())
            .limit(10)
            .collect(toList());
        
        System.out.println("Top 10 Products by Revenue:");
        topProducts.forEach(product ->
            System.out.printf("  %s: $%.2f (%d units, %s)%n",
                product.productName(), product.revenue(), 
                product.unitsSold(), product.getPerformanceCategory()));
        
        // Category performance
        var categoryPerformance = productPerformance.stream()
            .collect(groupingBy(DataModels.ProductPerformance::category,
                teeing(
                    summingDouble(DataModels.ProductPerformance::revenue),
                    summingInt(DataModels.ProductPerformance::unitsSold),
                    (revenue, units) -> Map.of("revenue", revenue, "units", (double) units)
                )));
        
        System.out.println("\nCategory Performance:");
        categoryPerformance.entrySet().stream()
            .sorted(Map.Entry.<String, Map<String, Double>>comparingByValue(
                Comparator.comparingDouble(map -> map.get("revenue"))).reversed())
            .forEach(entry -> {
                var category = entry.getKey();
                var metrics = entry.getValue();
                System.out.printf("  %s: $%.2f revenue, %.0f units%n",
                    category, metrics.get("revenue"), metrics.get("units"));
            });
        
        // Product lifecycle analysis
        var newProducts = productPerformance.stream()
            .filter(product -> {
                var productObj = findProductById(product.productId()).orElse(null);
                return productObj != null && productObj.isNewProduct();
            })
            .count();
        
        System.out.printf("\nProduct Insights:%n");
        System.out.printf("  New products (< 6 months): %d%n", newProducts);
        System.out.printf("  Best sellers (> 500 units): %d%n",
            productPerformance.stream().filter(p -> p.unitsSold() > 500).count());
        System.out.printf("  Slow moving (< 50 units): %d%n",
            productPerformance.stream().filter(p -> p.unitsSold() < 50).count());
    }
    
    private DataModels.ProductPerformance analyzeProduct(DataModels.Product product) {
        var productSales = sales.stream()
            .filter(sale -> sale.productId().equals(product.id()))
            .collect(toList());
        
        if (productSales.isEmpty()) {
            return new DataModels.ProductPerformance(
                product.id(), product.name(), product.category(),
                0, 0.0, 0.0, 0, 
                LocalDate.now(), LocalDate.now().minusYears(10)
            );
        }
        
        var unitsSold = productSales.stream()
            .mapToInt(DataModels.Sale::quantity)
            .sum();
        
        var revenue = productSales.stream()
            .mapToDouble(DataModels.Sale::totalAmount)
            .sum();
        
        var firstSale = productSales.stream()
            .map(sale -> sale.saleDate().toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
        
        var lastSale = productSales.stream()
            .map(sale -> sale.saleDate().toLocalDate())
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now().minusYears(10));
        
        // Simulated rating data
        var averageRating = 3.0 + Math.random() * 2.0;
        var numberOfReviews = Math.max(1, unitsSold / 10);
        
        return new DataModels.ProductPerformance(
            product.id(), product.name(), product.category(),
            unitsSold, revenue, averageRating, numberOfReviews,
            firstSale, lastSale
        );
    }
    
    private void performSalesTrendAnalysis() {
        // Monthly sales trends
        var monthlySales = sales.stream()
            .collect(groupingBy(
                sale -> sale.saleDate().toLocalDate().withDayOfMonth(1),
                TreeMap::new,
                teeing(
                    summingDouble(DataModels.Sale::totalAmount),
                    counting(),
                    (revenue, count) -> Map.of("revenue", revenue, "count", count.doubleValue())
                )
            ));
        
        System.out.println("Monthly Sales Trends:");
        monthlySales.entrySet().stream()
            .limit(12)
            .forEach(entry -> {
                var month = entry.getKey();
                var metrics = entry.getValue();
                System.out.printf("  %s: $%.2f (%.0f transactions)%n",
                    month.getMonth(), metrics.get("revenue"), metrics.get("count"));
            });
        
        // Payment method analysis
        var paymentMethodStats = sales.stream()
            .collect(groupingBy(DataModels.Sale::paymentMethod,
                teeing(
                    summingDouble(DataModels.Sale::totalAmount),
                    counting(),
                    (revenue, count) -> new PaymentMethodStats(revenue, count.intValue())
                )));
        
        System.out.println("\nPayment Method Analysis:");
        paymentMethodStats.entrySet().stream()
            .sorted(Map.Entry.<DataModels.PaymentMethod, PaymentMethodStats>comparingByValue(
                Comparator.comparingDouble(PaymentMethodStats::revenue)).reversed())
            .forEach(entry -> {
                var method = entry.getKey();
                var stats = entry.getValue();
                System.out.printf("  %s: $%.2f (%.1f%% of transactions)%n",
                    method.getDisplayName(), stats.revenue(),
                    (double) stats.count() / sales.size() * 100);
            });
        
        // Discount analysis
        var discountStats = sales.stream()
            .collect(partitioningBy(DataModels.Sale::hasDiscount,
                teeing(
                    summingDouble(DataModels.Sale::totalAmount),
                    counting(),
                    (revenue, count) -> new DiscountStats(revenue, count.intValue())
                )));
        
        var withDiscount = discountStats.get(true);
        var withoutDiscount = discountStats.get(false);
        
        System.out.println("\nDiscount Impact Analysis:");
        System.out.printf("  With discounts: $%.2f (%d transactions)%n", 
            withDiscount.revenue(), withDiscount.count());
        System.out.printf("  Without discounts: $%.2f (%d transactions)%n", 
            withoutDiscount.revenue(), withoutDiscount.count());
        
        if (withDiscount.count() > 0 && withoutDiscount.count() > 0) {
            var avgWithDiscount = withDiscount.revenue() / withDiscount.count();
            var avgWithoutDiscount = withoutDiscount.revenue() / withoutDiscount.count();
            System.out.printf("  Average transaction with discount: $%.2f%n", avgWithDiscount);
            System.out.printf("  Average transaction without discount: $%.2f%n", avgWithoutDiscount);
        }
    }
    
    private record PaymentMethodStats(double revenue, int count) {}
    private record DiscountStats(double revenue, int count) {}
    
    private void performParallelAnalytics() {
        System.out.println("Running parallel analytics...");
        
        var startTime = System.currentTimeMillis();
        
        // Parallel computation of multiple analytics
        var futureCustomerMetrics = CompletableFuture.supplyAsync(() -> 
            computeCustomerMetrics());
        
        var futureProductMetrics = CompletableFuture.supplyAsync(() -> 
            computeProductMetrics());
        
        var futureSalesMetrics = CompletableFuture.supplyAsync(() -> 
            computeSalesMetrics());
        
        var futureAdvancedAnalytics = CompletableFuture.supplyAsync(() -> 
            computeAdvancedAnalytics());
        
        // Wait for all computations to complete
        CompletableFuture.allOf(futureCustomerMetrics, futureProductMetrics, 
                               futureSalesMetrics, futureAdvancedAnalytics)
            .join();
        
        var endTime = System.currentTimeMillis();
        
        try {
            var customerMetrics = futureCustomerMetrics.get();
            var productMetrics = futureProductMetrics.get();
            var salesMetrics = futureSalesMetrics.get();
            var advancedMetrics = futureAdvancedAnalytics.get();
            
            System.out.println("Parallel Analytics Results:");
            System.out.println("  Customer Metrics: " + customerMetrics);
            System.out.println("  Product Metrics: " + productMetrics);
            System.out.println("  Sales Metrics: " + salesMetrics.getSummary());
            System.out.println("  Advanced Analytics: " + advancedMetrics);
            
        } catch (Exception e) {
            System.err.println("Error in parallel computation: " + e.getMessage());
        }
        
        System.out.printf("Parallel processing completed in %d ms%n", endTime - startTime);
    }
    
    private String computeCustomerMetrics() {
        var totalCustomers = customers.size();
        var premiumCustomers = customers.parallelStream()
            .filter(DataModels.Customer::isPremiumTier)
            .count();
        var longTermCustomers = customers.parallelStream()
            .filter(DataModels.Customer::isLongTermCustomer)
            .count();
        
        return String.format("Total: %d, Premium: %d, Long-term: %d", 
            totalCustomers, premiumCustomers, longTermCustomers);
    }
    
    private String computeProductMetrics() {
        var totalProducts = products.size();
        var activeProducts = products.parallelStream()
            .filter(DataModels.Product::isAvailable)
            .count();
        var newProducts = products.parallelStream()
            .filter(DataModels.Product::isNewProduct)
            .count();
        
        return String.format("Total: %d, Active: %d, New: %d", 
            totalProducts, activeProducts, newProducts);
    }
    
    private DataModels.SalesMetrics computeSalesMetrics() {
        var totalRevenue = sales.parallelStream()
            .mapToDouble(DataModels.Sale::totalAmount)
            .sum();
        
        var totalTransactions = sales.size();
        
        var averageTransactionValue = totalTransactions > 0 ? totalRevenue / totalTransactions : 0.0;
        
        var sortedAmounts = sales.parallelStream()
            .mapToDouble(DataModels.Sale::totalAmount)
            .sorted()
            .toArray();
        
        var medianTransactionValue = sortedAmounts.length > 0 ? 
            (sortedAmounts.length % 2 == 0 ? 
                (sortedAmounts[sortedAmounts.length / 2 - 1] + sortedAmounts[sortedAmounts.length / 2]) / 2.0 :
                sortedAmounts[sortedAmounts.length / 2]) : 0.0;
        
        var uniqueCustomers = (int) sales.parallelStream()
            .map(DataModels.Sale::customerId)
            .distinct()
            .count();
        
        var uniqueProducts = (int) sales.parallelStream()
            .map(DataModels.Sale::productId)
            .distinct()
            .count();
        
        var periodStart = sales.parallelStream()
            .map(sale -> sale.saleDate().toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
        
        var periodEnd = sales.parallelStream()
            .map(sale -> sale.saleDate().toLocalDate())
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());
        
        return new DataModels.SalesMetrics(
            totalRevenue, totalTransactions, averageTransactionValue, medianTransactionValue,
            uniqueCustomers, uniqueProducts, periodStart, periodEnd
        );
    }
    
    private String computeAdvancedAnalytics() {
        // Complex parallel computations
        var topCustomersByRevenue = sales.parallelStream()
            .collect(groupingBy(DataModels.Sale::customerId,
                     summingDouble(DataModels.Sale::totalAmount)))
            .entrySet().parallelStream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(3)
            .map(entry -> findCustomerById(entry.getKey())
                .map(customer -> customer.name() + ": $" + String.format("%.2f", entry.getValue()))
                .orElse("Unknown: $" + String.format("%.2f", entry.getValue())))
            .collect(joining(", "));
        
        return "Top customers: " + topCustomersByRevenue;
    }
    
    private Optional<DataModels.Customer> findCustomerById(String customerId) {
        return customers.stream()
            .filter(customer -> customer.id().equals(customerId))
            .findFirst();
    }
}
```

---

## 🎯 Summary

**Congratulations!** You've completed the **Week 6 Capstone Project** and built a comprehensive functional data processing system that demonstrates:

- **Advanced Functional Programming**: Lambda expressions, method references, and functional composition throughout the entire system
- **Modern Data Modeling**: Records and sealed classes for type-safe, immutable data structures
- **Stream API Mastery**: Complex data processing pipelines with parallel streams and advanced collectors
- **Optional Integration**: Robust null safety and error handling patterns
- **Modern Java Features**: Latest syntax and APIs for clean, maintainable code
- **Real-World Architecture**: Enterprise-level design patterns and functional programming practices

**Key Architectural Achievements:**
- **Type Safety**: Sealed classes and pattern matching ensure compile-time correctness
- **Immutability**: Records provide immutable data structures with automatic implementations
- **Functional Composition**: Complex business logic built from simple, composable functions
- **Parallel Processing**: Efficient concurrent analytics using parallel streams
- **Error Handling**: Comprehensive error management with functional result types
- **Performance**: Optimized data processing with streaming and lazy evaluation

**Technical Skills Demonstrated:**
- Functional data transformation pipelines
- Advanced stream operations and collectors
- Pattern matching with modern Java syntax
- Parallel processing and concurrent analytics
- Type-safe error handling with sealed classes
- Modern API integration and file operations

**Business Value Delivered:**
- Customer segmentation and retention analysis
- Product performance and lifecycle insights
- Sales trend analysis and payment optimization
- Data quality assessment and validation
- Real-time analytics with parallel processing

**🏆 Week 6 Complete!** You've mastered advanced Java functional programming and can now build sophisticated, maintainable applications using the latest Java features and patterns. This capstone project serves as a comprehensive reference for implementing enterprise-level functional programming solutions in Java.

**Next Steps:** You're now ready to apply these advanced functional programming concepts to real-world projects, combining the power of streams, lambdas, records, and modern Java features to build robust, scalable applications.