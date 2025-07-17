# Day 41: Modern Java Features (August 19, 2025)

**Date:** August 19, 2025  
**Duration:** 3 hours  
**Focus:** Explore cutting-edge Java 8-17+ features and modern syntax for powerful, expressive programming

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Master modern Java syntax and language features from Java 8-17+
- ✅ Understand var keyword, pattern matching, and switch expressions
- ✅ Learn new APIs for time, collections, and text processing
- ✅ Implement records, sealed classes, and modern data modeling
- ✅ Apply text blocks, enhanced instanceof, and modern control flow
- ✅ Build applications using cutting-edge Java features

---

## 🚀 Part 1: Modern Language Features (Java 8-14) (60 minutes)

### **Java Version Evolution Overview**

**Major Feature Timeline:**
- **Java 8 (2014)**: Lambdas, Streams, Optional, new Date/Time API
- **Java 9 (2017)**: Modules, private methods in interfaces, new HTTP client
- **Java 10 (2018)**: Local variable type inference (`var` keyword)
- **Java 11 (2018)**: String methods, HTTP client, file operations
- **Java 12-13 (2019)**: Switch expressions (preview), text blocks (preview)
- **Java 14 (2020)**: Pattern matching instanceof (preview), records (preview)
- **Java 15 (2020)**: Text blocks, sealed classes (preview)
- **Java 16 (2021)**: Records, pattern matching instanceof, sealed classes
- **Java 17 (2021)**: LTS release with all stabilized features

### **Modern Syntax and Language Improvements**

Create `ModernJavaFeatures.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;
import java.io.IOException;

/**
 * Comprehensive demonstration of modern Java features (Java 8-17+)
 */
public class ModernJavaFeatures {
    
    public static void main(String[] args) {
        System.out.println("=== Modern Java Features (Java 8-17+) ===\n");
        
        // 1. Local variable type inference (Java 10+)
        System.out.println("1. Local Variable Type Inference (var):");
        demonstrateVarKeyword();
        
        // 2. Enhanced switch expressions (Java 12+/14)
        System.out.println("\n2. Enhanced Switch Expressions:");
        demonstrateSwitchExpressions();
        
        // 3. Text blocks (Java 15+)
        System.out.println("\n3. Text Blocks:");
        demonstrateTextBlocks();
        
        // 4. Pattern matching with instanceof (Java 16+)
        System.out.println("\n4. Pattern Matching with instanceof:");
        demonstratePatternMatching();
        
        // 5. Modern String API enhancements
        System.out.println("\n5. Modern String API:");
        demonstrateStringEnhancements();
        
        System.out.println("\n=== Modern Language Features Completed ===");
    }
    
    private static void demonstrateVarKeyword() {
        // Traditional explicit type declarations
        String explicitString = "Hello World";
        List<String> explicitList = new ArrayList<>();
        Map<String, Integer> explicitMap = new HashMap<>();
        
        // Modern var keyword (Java 10+)
        var inferredString = "Hello Modern Java!";
        var inferredList = new ArrayList<String>();
        var inferredMap = Map.of("one", 1, "two", 2, "three", 3);
        var inferredStream = Stream.of(1, 2, 3, 4, 5);
        
        System.out.println("Inferred string: " + inferredString);
        System.out.println("Inferred list type: " + inferredList.getClass().getSimpleName());
        System.out.println("Inferred map: " + inferredMap);
        
        // Complex type inference
        var complexData = Map.of(
            "users", List.of("Alice", "Bob", "Charlie"),
            "admins", List.of("Admin1", "Admin2"),
            "guests", List.of("Guest1", "Guest2", "Guest3")
        );
        
        System.out.println("Complex inferred type works with: " + complexData.keySet());
        
        // Loop variable inference
        var numbers = List.of(1, 2, 3, 4, 5);
        for (var number : numbers) {
            System.out.print(number * 2 + " ");
        }
        System.out.println();
        
        // Lambda parameter inference (Java 11+)
        var stringProcessor = (Function<String, String>) s -> s.toUpperCase();
        System.out.println("Lambda with var: " + stringProcessor.apply("modern java"));
        
        // File operations with var
        try {
            var tempFile = Files.createTempFile("modern-java", ".txt");
            var content = "Modern Java features are amazing!";
            Files.writeString(tempFile, content);
            var readContent = Files.readString(tempFile);
            System.out.println("File content: " + readContent);
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            System.err.println("File operation error: " + e.getMessage());
        }
    }
    
    private static void demonstrateSwitchExpressions() {
        // Traditional switch statement
        String dayType1 = getDayTypeTraditional("MONDAY");
        System.out.println("Traditional switch result: " + dayType1);
        
        // Modern switch expression (Java 14+)
        String dayType2 = getDayTypeModern("SATURDAY");
        System.out.println("Modern switch result: " + dayType2);
        
        // Switch with multiple values
        int workDays = getWorkDaysInMonth("FEBRUARY", false);
        System.out.println("Work days in February (non-leap): " + workDays);
        
        // Switch with complex expressions
        var httpStatus = 404;
        var statusMessage = switch (httpStatus) {
            case 200 -> "OK - Request successful";
            case 201 -> "Created - Resource created successfully";
            case 400 -> "Bad Request - Invalid request syntax";
            case 401 -> "Unauthorized - Authentication required";
            case 403 -> "Forbidden - Access denied";
            case 404 -> "Not Found - Resource not found";
            case 500 -> "Internal Server Error - Server encountered an error";
            default -> "Unknown status code: " + httpStatus;
        };
        System.out.println("HTTP Status " + httpStatus + ": " + statusMessage);
        
        // Switch with yield for complex logic
        var grade = 'B';
        var gpaPoints = switch (grade) {
            case 'A' -> 4.0;
            case 'B' -> 3.0;
            case 'C' -> 2.0;
            case 'D' -> 1.0;
            case 'F' -> 0.0;
            default -> {
                System.out.println("Processing unusual grade: " + grade);
                yield -1.0; // yield keyword for complex blocks
            }
        };
        System.out.println("Grade " + grade + " = " + gpaPoints + " GPA points");
    }
    
    private static String getDayTypeTraditional(String day) {
        String result;
        switch (day) {
            case "MONDAY":
            case "TUESDAY":
            case "WEDNESDAY":
            case "THURSDAY":
            case "FRIDAY":
                result = "Weekday";
                break;
            case "SATURDAY":
            case "SUNDAY":
                result = "Weekend";
                break;
            default:
                result = "Invalid day";
                break;
        }
        return result;
    }
    
    private static String getDayTypeModern(String day) {
        return switch (day) {
            case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "Weekday";
            case "SATURDAY", "SUNDAY" -> "Weekend";
            default -> "Invalid day";
        };
    }
    
    private static int getWorkDaysInMonth(String month, boolean isLeapYear) {
        return switch (month) {
            case "JANUARY", "MARCH", "MAY", "JULY", "AUGUST", "OCTOBER", "DECEMBER" -> 31;
            case "APRIL", "JUNE", "SEPTEMBER", "NOVEMBER" -> 30;
            case "FEBRUARY" -> isLeapYear ? 29 : 28;
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
    }
    
    private static void demonstrateTextBlocks() {
        // Traditional string concatenation
        String traditionalJson = "{\n" +
                "  \"name\": \"Alice\",\n" +
                "  \"age\": 30,\n" +
                "  \"city\": \"New York\",\n" +
                "  \"skills\": [\"Java\", \"Python\", \"JavaScript\"]\n" +
                "}";
        
        // Modern text blocks (Java 15+)
        var modernJson = """
                {
                  "name": "Alice",
                  "age": 30,
                  "city": "New York",
                  "skills": ["Java", "Python", "JavaScript"]
                }
                """;
        
        System.out.println("Traditional JSON construction:");
        System.out.println(traditionalJson);
        
        System.out.println("\nModern text block:");
        System.out.println(modernJson);
        
        // HTML template with text blocks
        var name = "Bob";
        var age = 25;
        var htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>User Profile</title>
                </head>
                <body>
                    <h1>Welcome, %s!</h1>
                    <p>Age: %d years old</p>
                    <div class="profile">
                        <img src="/avatar/%s.jpg" alt="Profile picture">
                    </div>
                </body>
                </html>
                """.formatted(name, age, name.toLowerCase());
        
        System.out.println("HTML template with text blocks:");
        System.out.println(htmlTemplate);
        
        // SQL query with text blocks
        var userId = 12345;
        var sqlQuery = """
                SELECT u.id, u.name, u.email, p.title, p.content
                FROM users u
                LEFT JOIN posts p ON u.id = p.user_id
                WHERE u.id = %d
                  AND u.active = true
                  AND p.published_date > '2024-01-01'
                ORDER BY p.published_date DESC
                LIMIT 10
                """.formatted(userId);
        
        System.out.println("SQL query with text blocks:");
        System.out.println(sqlQuery);
        
        // Text block processing methods
        var multilineText = """
                Line 1: Introduction
                Line 2: Content
                Line 3: Conclusion
                """;
        
        System.out.println("Text block processing:");
        System.out.println("Lines: " + multilineText.lines().count());
        System.out.println("Stripped: '" + multilineText.strip() + "'");
        System.out.println("Indent +2: \n" + multilineText.indent(2));
    }
    
    private static void demonstratePatternMatching() {
        Object[] objects = {
            "Hello World",
            42,
            List.of(1, 2, 3),
            Map.of("key", "value"),
            new StringBuilder("Mutable string"),
            3.14159,
            null
        };
        
        System.out.println("Pattern matching with instanceof:");
        
        for (var obj : objects) {
            // Traditional approach
            String traditionalResult = processObjectTraditional(obj);
            
            // Modern pattern matching (Java 16+)
            String modernResult = processObjectModern(obj);
            
            System.out.println("Object: " + obj + 
                " -> Traditional: " + traditionalResult + 
                " | Modern: " + modernResult);
        }
        
        // Complex pattern matching examples
        System.out.println("\nComplex pattern matching:");
        var complexObjects = List.of(
            "Short",
            "This is a longer string",
            List.of(),
            List.of("single"),
            List.of("first", "second", "third"),
            42,
            100
        );
        
        for (var obj : complexObjects) {
            var description = describeObject(obj);
            System.out.println(obj + " -> " + description);
        }
    }
    
    private static String processObjectTraditional(Object obj) {
        if (obj == null) {
            return "null value";
        } else if (obj instanceof String) {
            String str = (String) obj;
            return "String with length " + str.length();
        } else if (obj instanceof Integer) {
            Integer num = (Integer) obj;
            return "Integer: " + (num % 2 == 0 ? "even" : "odd");
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return "List with " + list.size() + " elements";
        } else {
            return "Unknown type: " + obj.getClass().getSimpleName();
        }
    }
    
    private static String processObjectModern(Object obj) {
        if (obj == null) {
            return "null value";
        } else if (obj instanceof String str) {
            return "String with length " + str.length();
        } else if (obj instanceof Integer num) {
            return "Integer: " + (num % 2 == 0 ? "even" : "odd");
        } else if (obj instanceof List<?> list) {
            return "List with " + list.size() + " elements";
        } else {
            return "Unknown type: " + obj.getClass().getSimpleName();
        }
    }
    
    private static String describeObject(Object obj) {
        return switch (obj) {
            case null -> "Nothing here";
            case String s when s.length() < 10 -> "Short string: '" + s + "'";
            case String s -> "Long string with " + s.length() + " characters";
            case List<?> list when list.isEmpty() -> "Empty list";
            case List<?> list when list.size() == 1 -> "Singleton list: " + list.get(0);
            case List<?> list -> "List with " + list.size() + " items";
            case Integer i when i < 50 -> "Small number: " + i;
            case Integer i -> "Large number: " + i;
            default -> "Something else: " + obj.getClass().getSimpleName();
        };
    }
    
    private static void demonstrateStringEnhancements() {
        var text = "  Modern Java String API  ";
        
        System.out.println("Original: '" + text + "'");
        
        // Java 11+ String methods
        System.out.println("isBlank(): " + text.isBlank());
        System.out.println("strip(): '" + text.strip() + "'");
        System.out.println("stripLeading(): '" + text.stripLeading() + "'");
        System.out.println("stripTrailing(): '" + text.stripTrailing() + "'");
        
        // String repeat (Java 11+)
        var pattern = "=*=";
        System.out.println("Repeated pattern: " + pattern.repeat(5));
        
        // String lines processing (Java 11+)
        var multilineString = """
                First line
                Second line
                Third line
                
                Fifth line after empty
                """;
        
        System.out.println("Lines processing:");
        multilineString.lines()
            .filter(line -> !line.isBlank())
            .map(line -> "-> " + line.strip())
            .forEach(System.out::println);
        
        // String formatting (Java 15+)
        var name = "Alice";
        var age = 30;
        var score = 95.5;
        
        var formatted = "Name: %s, Age: %d, Score: %.1f%%".formatted(name, age, score);
        System.out.println("Formatted string: " + formatted);
        
        // String transformations (Java 12+)
        var input = "transform this text";
        var result = input
            .transform(String::toUpperCase)
            .transform(s -> s.replace(" ", "_"))
            .transform(s -> "[" + s + "]");
        
        System.out.println("Transformation chain: " + input + " -> " + result);
        
        // String comparison utilities
        var strings = List.of("apple", "banana", "Apple", "BANANA", "", "  ", null);
        
        System.out.println("String utilities:");
        for (var str : strings) {
            if (str == null) {
                System.out.println("null -> isNull: true");
            } else {
                System.out.printf("'%s' -> isEmpty: %b, isBlank: %b, length: %d%n",
                    str, str.isEmpty(), str.isBlank(), str.length());
            }
        }
    }
}
```

---

## 🔧 Part 2: Records and Sealed Classes (Java 14-17) (60 minutes)

### **Modern Data Modeling with Records**

Records provide a compact syntax for immutable data classes with automatic implementations of equals, hashCode, toString, and accessor methods.

Create `ModernDataModeling.java`:

```java
import java.util.*;
import java.util.function.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Modern data modeling with Records, Sealed Classes, and Pattern Matching
 */
public class ModernDataModeling {
    
    public static void main(String[] args) {
        System.out.println("=== Modern Data Modeling (Records & Sealed Classes) ===\n");
        
        // 1. Records demonstration
        System.out.println("1. Records:");
        demonstrateRecords();
        
        // 2. Sealed classes demonstration
        System.out.println("\n2. Sealed Classes:");
        demonstrateSealedClasses();
        
        // 3. Pattern matching with records
        System.out.println("\n3. Pattern Matching with Records:");
        demonstratePatternMatchingWithRecords();
        
        // 4. Real-world application
        System.out.println("\n4. Real-World Application:");
        demonstrateRealWorldUsage();
        
        System.out.println("\n=== Modern Data Modeling Completed ===");
    }
    
    private static void demonstrateRecords() {
        // Simple record
        var person1 = new Person("Alice", 30, "alice@example.com");
        var person2 = new Person("Bob", 25, "bob@example.com");
        var person3 = new Person("Alice", 30, "alice@example.com");
        
        System.out.println("Person 1: " + person1);
        System.out.println("Person 2: " + person2);
        System.out.println("Person 3: " + person3);
        
        // Automatic equals and hashCode
        System.out.println("person1.equals(person3): " + person1.equals(person3));
        System.out.println("person1.hashCode() == person3.hashCode(): " + 
            (person1.hashCode() == person3.hashCode()));
        
        // Accessor methods (no get prefix)
        System.out.println("Person 1 name: " + person1.name());
        System.out.println("Person 1 age: " + person1.age());
        System.out.println("Person 1 email: " + person1.email());
        
        // Records in collections
        var people = List.of(person1, person2, person3);
        var uniquePeople = new HashSet<>(people);
        System.out.println("People list size: " + people.size());
        System.out.println("Unique people size: " + uniquePeople.size());
        
        // Complex record with validation
        try {
            var product = new Product("Laptop", 999.99, 10);
            System.out.println("Valid product: " + product);
            
            var invalidProduct = new Product("", -100, -5);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid product error: " + e.getMessage());
        }
        
        // Record with custom methods
        var address = new Address("123 Main St", "New York", "NY", "10001", "USA");
        System.out.println("Address: " + address);
        System.out.println("Formatted: " + address.getFormattedAddress());
        System.out.println("Is US address: " + address.isUSAddress());
        
        // Nested records
        var contact = new Contact(person1, address);
        System.out.println("Contact: " + contact);
        System.out.println("Contact in US: " + contact.address().isUSAddress());
        
        // Record with collections
        var order = new Order(
            "ORD-001",
            List.of(
                new OrderItem("Laptop", 2, 999.99),
                new OrderItem("Mouse", 1, 29.99),
                new OrderItem("Keyboard", 1, 79.99)
            ),
            LocalDateTime.now()
        );
        
        System.out.println("Order: " + order);
        System.out.println("Total amount: $" + order.getTotalAmount());
        System.out.println("Item count: " + order.getItemCount());
    }
    
    private static void demonstrateSealedClasses() {
        // Sealed class examples
        var shapes = List.of(
            new Circle(5.0),
            new Rectangle(4.0, 6.0),
            new Triangle(3.0, 4.0, 5.0)
        );
        
        System.out.println("Shape calculations:");
        for (var shape : shapes) {
            var area = calculateArea(shape);
            var perimeter = calculatePerimeter(shape);
            System.out.printf("%s: Area = %.2f, Perimeter = %.2f%n", 
                shape.getClass().getSimpleName(), area, perimeter);
        }
        
        // Result type with sealed classes
        var results = List.of(
            new Success<>("Operation completed successfully"),
            new Error<>("Database connection failed"),
            new Success<>(42),
            new Error<>("Invalid input parameter")
        );
        
        System.out.println("\nResult processing:");
        for (var result : results) {
            processResult(result);
        }
        
        // State machine with sealed classes
        var stateMachine = new OrderStateMachine();
        System.out.println("\nOrder state machine:");
        System.out.println("Initial state: " + stateMachine.getCurrentState());
        
        stateMachine.process();
        stateMachine.ship();
        stateMachine.deliver();
        stateMachine.complete();
    }
    
    private static double calculateArea(Shape shape) {
        return switch (shape) {
            case Circle(var radius) -> Math.PI * radius * radius;
            case Rectangle(var width, var height) -> width * height;
            case Triangle(var a, var b, var c) -> {
                var s = (a + b + c) / 2;
                yield Math.sqrt(s * (s - a) * (s - b) * (s - c));
            }
        };
    }
    
    private static double calculatePerimeter(Shape shape) {
        return switch (shape) {
            case Circle(var radius) -> 2 * Math.PI * radius;
            case Rectangle(var width, var height) -> 2 * (width + height);
            case Triangle(var a, var b, var c) -> a + b + c;
        };
    }
    
    private static void processResult(Result<?> result) {
        var message = switch (result) {
            case Success(var value) -> "✅ Success: " + value;
            case Error(var message) -> "❌ Error: " + message;
        };
        System.out.println(message);
    }
    
    private static void demonstratePatternMatchingWithRecords() {
        var contacts = List.of(
            new Contact(new Person("Alice", 30, "alice@example.com"), 
                       new Address("123 Main St", "New York", "NY", "10001", "USA")),
            new Contact(new Person("Bob", 25, "bob@example.com"), 
                       new Address("456 Oak Ave", "Los Angeles", "CA", "90210", "USA")),
            new Contact(new Person("Charlie", 35, "charlie@example.com"), 
                       new Address("789 Pine Rd", "Toronto", "ON", "M5V 1A1", "Canada"))
        );
        
        System.out.println("Pattern matching with records:");
        
        for (var contact : contacts) {
            var info = analyzeContact(contact);
            System.out.println(info);
        }
        
        // Advanced pattern matching
        var orders = List.of(
            new Order("ORD-001", List.of(new OrderItem("Laptop", 1, 999.99)), LocalDateTime.now()),
            new Order("ORD-002", List.of(), LocalDateTime.now()),
            new Order("ORD-003", List.of(
                new OrderItem("Phone", 2, 599.99),
                new OrderItem("Case", 2, 19.99)
            ), LocalDateTime.now().minusDays(1))
        );
        
        System.out.println("\nOrder analysis:");
        for (var order : orders) {
            var analysis = analyzeOrder(order);
            System.out.println(analysis);
        }
    }
    
    private static String analyzeContact(Contact contact) {
        return switch (contact) {
            case Contact(Person(var name, var age, var email), 
                        Address(var street, var city, "NY", var zip, "USA")) ->
                String.format("%s (%d) is in New York: %s, %s %s", 
                    name, age, street, city, zip);
            
            case Contact(Person(var name, var age, var email), 
                        Address(var street, var city, "CA", var zip, "USA")) ->
                String.format("%s (%d) is in California: %s, %s %s", 
                    name, age, street, city, zip);
            
            case Contact(Person(var name, var age, var email), 
                        Address(var street, var city, var state, var zip, "USA")) ->
                String.format("%s (%d) is in the US (%s): %s, %s", 
                    name, age, state, street, city);
            
            case Contact(Person(var name, var age, var email), 
                        Address(var street, var city, var state, var zip, var country)) ->
                String.format("%s (%d) is international (%s): %s, %s", 
                    name, age, country, street, city);
        };
    }
    
    private static String analyzeOrder(Order order) {
        return switch (order) {
            case Order(var id, var items, var timestamp) when items.isEmpty() ->
                "Empty order " + id + " placed on " + timestamp.toLocalDate();
            
            case Order(var id, var items, var timestamp) when items.size() == 1 ->
                String.format("Single item order %s: %s x%d ($%.2f)", 
                    id, items.get(0).name(), items.get(0).quantity(), items.get(0).price());
            
            case Order(var id, var items, var timestamp) when items.size() > 5 ->
                String.format("Large order %s with %d items (total: $%.2f)", 
                    id, items.size(), calculateTotal(items));
            
            case Order(var id, var items, var timestamp) ->
                String.format("Regular order %s with %d items (total: $%.2f)", 
                    id, items.size(), calculateTotal(items));
        };
    }
    
    private static double calculateTotal(List<OrderItem> items) {
        return items.stream()
            .mapToDouble(item -> item.quantity() * item.price())
            .sum();
    }
    
    private static void demonstrateRealWorldUsage() {
        // Configuration system using records
        var dbConfig = new DatabaseConfig(
            "localhost", 5432, "myapp", "user", "password", 10, 5000
        );
        
        var apiConfig = new ApiConfig(
            "https://api.example.com", "v1", "api-key-12345", 30, true
        );
        
        var appConfig = new ApplicationConfig(dbConfig, apiConfig, "production", true);
        
        System.out.println("Application configuration:");
        System.out.println(appConfig);
        System.out.println("Database URL: " + appConfig.database().getJdbcUrl());
        System.out.println("API endpoint: " + appConfig.api().getFullEndpoint());
        
        // Event system using sealed classes and records
        var eventProcessor = new EventProcessor();
        
        var events = List.of(
            new UserLoginEvent("alice@example.com", LocalDateTime.now(), "192.168.1.1"),
            new OrderCreatedEvent("ORD-001", "alice@example.com", 999.99, LocalDateTime.now()),
            new PaymentProcessedEvent("ORD-001", 999.99, "CARD", LocalDateTime.now()),
            new UserLogoutEvent("alice@example.com", LocalDateTime.now())
        );
        
        System.out.println("\nEvent processing:");
        for (var event : events) {
            eventProcessor.processEvent(event);
        }
        
        // Data analytics using records
        var analytics = new DataAnalytics();
        var salesData = List.of(
            new SalesRecord("2024-01", "Electronics", 15000.0, 150),
            new SalesRecord("2024-01", "Clothing", 8000.0, 200),
            new SalesRecord("2024-02", "Electronics", 18000.0, 180),
            new SalesRecord("2024-02", "Clothing", 9500.0, 190),
            new SalesRecord("2024-03", "Electronics", 16500.0, 165),
            new SalesRecord("2024-03", "Clothing", 7800.0, 156)
        );
        
        System.out.println("\nSales analytics:");
        var summary = analytics.generateSummary(salesData);
        System.out.println(summary);
    }
}

// Simple record
record Person(String name, int age, String email) {}

// Record with validation
record Product(String name, double price, int quantity) {
    public Product {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}

// Record with custom methods
record Address(String street, String city, String state, String zipCode, String country) {
    public String getFormattedAddress() {
        return String.format("%s, %s, %s %s, %s", street, city, state, zipCode, country);
    }
    
    public boolean isUSAddress() {
        return "USA".equalsIgnoreCase(country);
    }
}

// Nested records
record Contact(Person person, Address address) {}

record OrderItem(String name, int quantity, double price) {}

record Order(String id, List<OrderItem> items, LocalDateTime timestamp) {
    public double getTotalAmount() {
        return items.stream().mapToDouble(item -> item.quantity() * item.price()).sum();
    }
    
    public int getItemCount() {
        return items.stream().mapToInt(OrderItem::quantity).sum();
    }
}

// Sealed classes for type safety
sealed interface Shape permits Circle, Rectangle, Triangle {}

record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double a, double b, double c) implements Shape {}

// Sealed class for result types
sealed interface Result<T> permits Success, Error {}
record Success<T>(T value) implements Result<T> {}
record Error<T>(String message) implements Result<T> {}

// Sealed class for state machine
sealed interface OrderState permits Pending, Processing, Shipped, Delivered, Completed {}
record Pending() implements OrderState {}
record Processing() implements OrderState {}
record Shipped(String trackingNumber) implements OrderState {}
record Delivered(LocalDateTime deliveredAt) implements OrderState {}
record Completed() implements OrderState {}

class OrderStateMachine {
    private OrderState currentState = new Pending();
    
    public OrderState getCurrentState() {
        return currentState;
    }
    
    public void process() {
        currentState = switch (currentState) {
            case Pending() -> {
                System.out.println("Order is now being processed");
                yield new Processing();
            }
            default -> {
                System.out.println("Cannot process order in current state: " + currentState);
                yield currentState;
            }
        };
    }
    
    public void ship() {
        currentState = switch (currentState) {
            case Processing() -> {
                var trackingNumber = "TRK-" + System.currentTimeMillis();
                System.out.println("Order shipped with tracking: " + trackingNumber);
                yield new Shipped(trackingNumber);
            }
            default -> {
                System.out.println("Cannot ship order in current state: " + currentState);
                yield currentState;
            }
        };
    }
    
    public void deliver() {
        currentState = switch (currentState) {
            case Shipped(var tracking) -> {
                var deliveredAt = LocalDateTime.now();
                System.out.println("Order delivered at: " + deliveredAt);
                yield new Delivered(deliveredAt);
            }
            default -> {
                System.out.println("Cannot deliver order in current state: " + currentState);
                yield currentState;
            }
        };
    }
    
    public void complete() {
        currentState = switch (currentState) {
            case Delivered(var deliveredAt) -> {
                System.out.println("Order completed");
                yield new Completed();
            }
            default -> {
                System.out.println("Cannot complete order in current state: " + currentState);
                yield currentState;
            }
        };
    }
}

// Configuration records
record DatabaseConfig(
    String host,
    int port,
    String database,
    String username,
    String password,
    int maxConnections,
    int timeoutMs
) {
    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }
}

record ApiConfig(
    String baseUrl,
    String version,
    String apiKey,
    int timeoutSeconds,
    boolean enableRetry
) {
    public String getFullEndpoint() {
        return baseUrl + "/" + version;
    }
}

record ApplicationConfig(
    DatabaseConfig database,
    ApiConfig api,
    String environment,
    boolean debugMode
) {}

// Event system with sealed classes
sealed interface ApplicationEvent permits UserLoginEvent, UserLogoutEvent, OrderCreatedEvent, PaymentProcessedEvent {}

record UserLoginEvent(String email, LocalDateTime timestamp, String ipAddress) implements ApplicationEvent {}
record UserLogoutEvent(String email, LocalDateTime timestamp) implements ApplicationEvent {}
record OrderCreatedEvent(String orderId, String userEmail, double amount, LocalDateTime timestamp) implements ApplicationEvent {}
record PaymentProcessedEvent(String orderId, double amount, String paymentMethod, LocalDateTime timestamp) implements ApplicationEvent {}

class EventProcessor {
    public void processEvent(ApplicationEvent event) {
        var result = switch (event) {
            case UserLoginEvent(var email, var timestamp, var ip) ->
                String.format("User %s logged in from %s at %s", email, ip, timestamp);
            
            case UserLogoutEvent(var email, var timestamp) ->
                String.format("User %s logged out at %s", email, timestamp);
            
            case OrderCreatedEvent(var orderId, var email, var amount, var timestamp) ->
                String.format("Order %s created by %s for $%.2f at %s", orderId, email, amount, timestamp);
            
            case PaymentProcessedEvent(var orderId, var amount, var method, var timestamp) ->
                String.format("Payment of $%.2f processed for order %s via %s at %s", 
                    amount, orderId, method, timestamp);
        };
        
        System.out.println("Event: " + result);
    }
}

// Analytics records
record SalesRecord(String month, String category, double revenue, int units) {}

record SalesSummary(
    double totalRevenue,
    int totalUnits,
    Map<String, Double> revenueByCategory,
    Map<String, Integer> unitsByCategory,
    String topCategory
) {}

class DataAnalytics {
    public SalesSummary generateSummary(List<SalesRecord> salesData) {
        var totalRevenue = salesData.stream()
            .mapToDouble(SalesRecord::revenue)
            .sum();
        
        var totalUnits = salesData.stream()
            .mapToInt(SalesRecord::units)
            .sum();
        
        var revenueByCategory = salesData.stream()
            .collect(Collectors.groupingBy(
                SalesRecord::category,
                Collectors.summingDouble(SalesRecord::revenue)
            ));
        
        var unitsByCategory = salesData.stream()
            .collect(Collectors.groupingBy(
                SalesRecord::category,
                Collectors.summingInt(SalesRecord::units)
            ));
        
        var topCategory = revenueByCategory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None");
        
        return new SalesSummary(
            totalRevenue,
            totalUnits,
            revenueByCategory,
            unitsByCategory,
            topCategory
        );
    }
}
```

---

## 🎯 Part 3: Modern APIs and HTTP Client (60 minutes)

### **New Time API and HTTP Client**

Create `ModernAPIs.java`:

```java
import java.net.http.*;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.Collectors;
import java.nio.file.*;
import java.io.IOException;

/**
 * Modern Java APIs - Time API, HTTP Client, and enhanced Collections
 */
public class ModernAPIs {
    
    public static void main(String[] args) {
        System.out.println("=== Modern Java APIs ===\n");
        
        // 1. Modern Time API (Java 8+)
        System.out.println("1. Modern Time API:");
        demonstrateTimeAPI();
        
        // 2. HTTP Client (Java 11+)
        System.out.println("\n2. HTTP Client:");
        demonstrateHttpClient();
        
        // 3. Collection factory methods (Java 9+)
        System.out.println("\n3. Collection Factory Methods:");
        demonstrateCollectionFactories();
        
        // 4. File operations (Java 11+)
        System.out.println("\n4. Modern File Operations:");
        demonstrateFileOperations();
        
        // 5. Process API (Java 9+)
        System.out.println("\n5. Process API:");
        demonstrateProcessAPI();
        
        System.out.println("\n=== Modern APIs Completed ===");
    }
    
    private static void demonstrateTimeAPI() {
        // Current date and time
        var now = LocalDateTime.now();
        var today = LocalDate.now();
        var currentTime = LocalTime.now();
        var instant = Instant.now();
        var zonedDateTime = ZonedDateTime.now();
        
        System.out.println("Current date/time:");
        System.out.println("LocalDateTime: " + now);
        System.out.println("LocalDate: " + today);
        System.out.println("LocalTime: " + currentTime);
        System.out.println("Instant: " + instant);
        System.out.println("ZonedDateTime: " + zonedDateTime);
        
        // Formatting
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var customFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");
        
        System.out.println("\nFormatted dates:");
        System.out.println("Standard format: " + now.format(formatter));
        System.out.println("Custom format: " + now.format(customFormatter));
        System.out.println("ISO format: " + now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Date arithmetic
        var futureDate = today.plusDays(30).plusMonths(2).plusYears(1);
        var pastDate = today.minusWeeks(5).minusDays(3);
        var nextFriday = today.with(java.time.DayOfWeek.FRIDAY);
        
        System.out.println("\nDate arithmetic:");
        System.out.println("Today: " + today);
        System.out.println("Future date (+30 days, +2 months, +1 year): " + futureDate);
        System.out.println("Past date (-5 weeks, -3 days): " + pastDate);
        System.out.println("Next Friday: " + nextFriday);
        
        // Duration and Period
        var startTime = LocalTime.of(9, 0);
        var endTime = LocalTime.of(17, 30);
        var workDuration = Duration.between(startTime, endTime);
        
        var birthDate = LocalDate.of(1990, 5, 15);
        var age = Period.between(birthDate, today);
        
        System.out.println("\nDuration and Period:");
        System.out.println("Work duration: " + workDuration.toHours() + " hours, " + 
                          workDuration.toMinutesPart() + " minutes");
        System.out.println("Age: " + age.getYears() + " years, " + age.getMonths() + " months");
        
        // Time zones
        var utcTime = ZonedDateTime.now(ZoneId.of("UTC"));
        var tokyoTime = utcTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
        var newYorkTime = utcTime.withZoneSameInstant(ZoneId.of("America/New_York"));
        var londonTime = utcTime.withZoneSameInstant(ZoneId.of("Europe/London"));
        
        System.out.println("\nTime zones:");
        System.out.println("UTC: " + utcTime.format(customFormatter));
        System.out.println("Tokyo: " + tokyoTime.format(customFormatter));
        System.out.println("New York: " + newYorkTime.format(customFormatter));
        System.out.println("London: " + londonTime.format(customFormatter));
        
        // Business day calculations
        var startDate = LocalDate.of(2024, 8, 1);
        var endDate = LocalDate.of(2024, 8, 31);
        var businessDays = calculateBusinessDays(startDate, endDate);
        
        System.out.println("\nBusiness calculations:");
        System.out.println("Business days in August 2024: " + businessDays);
        
        // Meeting scheduler
        var meetings = scheduleMeetings(LocalDate.now(), 5, Duration.ofHours(1));
        System.out.println("\nScheduled meetings:");
        meetings.forEach(meeting -> 
            System.out.println("Meeting: " + meeting.format(customFormatter)));
    }
    
    private static long calculateBusinessDays(LocalDate start, LocalDate end) {
        return start.datesUntil(end.plusDays(1))
            .filter(date -> {
                var dayOfWeek = date.getDayOfWeek();
                return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
            })
            .count();
    }
    
    private static List<LocalDateTime> scheduleMeetings(LocalDate startDate, int count, Duration duration) {
        var meetings = new ArrayList<LocalDateTime>();
        var currentDate = startDate;
        var startTime = LocalTime.of(9, 0);
        
        for (int i = 0; i < count; i++) {
            // Skip weekends
            while (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || 
                   currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                currentDate = currentDate.plusDays(1);
            }
            
            meetings.add(LocalDateTime.of(currentDate, startTime.plus(duration.multipliedBy(i % 3))));
            
            if ((i + 1) % 3 == 0) {
                currentDate = currentDate.plusDays(1);
            }
        }
        
        return meetings;
    }
    
    private static void demonstrateHttpClient() {
        var client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        try {
            // Simple GET request
            var getRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts/1"))
                .timeout(Duration.ofSeconds(5))
                .build();
            
            var getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("GET Request:");
            System.out.println("Status: " + getResponse.statusCode());
            System.out.println("Headers: " + getResponse.headers().map());
            System.out.println("Body: " + getResponse.body().substring(0, Math.min(200, getResponse.body().length())) + "...");
            
            // POST request with JSON
            var postData = """
                {
                    "title": "Modern Java Post",
                    "body": "This is a test post using modern Java HTTP client",
                    "userId": 1
                }
                """;
            
            var postRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();
            
            var postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("\nPOST Request:");
            System.out.println("Status: " + postResponse.statusCode());
            System.out.println("Response: " + postResponse.body().substring(0, Math.min(200, postResponse.body().length())) + "...");
            
            // Async request
            var asyncRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/users"))
                .build();
            
            var asyncResponse = client.sendAsync(asyncRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("\nAsync Request Completed:");
                    System.out.println("Status: " + response.statusCode());
                    return response.body();
                })
                .thenApply(body -> {
                    System.out.println("Body length: " + body.length());
                    return body;
                });
            
            // Wait for async completion (for demo purposes)
            asyncResponse.join();
            
        } catch (IOException | InterruptedException e) {
            System.err.println("HTTP request failed: " + e.getMessage());
        }
        
        // Multiple concurrent requests
        var urls = List.of(
            "https://jsonplaceholder.typicode.com/posts/1",
            "https://jsonplaceholder.typicode.com/posts/2",
            "https://jsonplaceholder.typicode.com/posts/3"
        );
        
        System.out.println("\nConcurrent requests:");
        var futures = urls.stream()
            .map(url -> HttpRequest.newBuilder().uri(URI.create(url)).build())
            .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
            .map(future -> future.thenApply(response -> 
                "URL: " + response.request().uri() + " -> Status: " + response.statusCode()))
            .collect(Collectors.toList());
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                futures.forEach(future -> {
                    try {
                        System.out.println(future.get());
                    } catch (Exception e) {
                        System.err.println("Request failed: " + e.getMessage());
                    }
                });
            })
            .join();
    }
    
    private static void demonstrateCollectionFactories() {
        // Immutable lists (Java 9+)
        var immutableList = List.of("apple", "banana", "cherry");
        var numbers = List.of(1, 2, 3, 4, 5);
        
        System.out.println("Immutable list: " + immutableList);
        System.out.println("Numbers: " + numbers);
        
        // Immutable sets
        var colors = Set.of("red", "green", "blue");
        var uniqueNumbers = Set.of(1, 2, 3, 3, 4, 5); // Duplicates ignored
        
        System.out.println("Colors set: " + colors);
        System.out.println("Unique numbers: " + uniqueNumbers);
        
        // Immutable maps
        var countryCapitals = Map.of(
            "USA", "Washington D.C.",
            "France", "Paris",
            "Japan", "Tokyo",
            "Canada", "Ottawa"
        );
        
        var moreCountries = Map.ofEntries(
            Map.entry("Germany", "Berlin"),
            Map.entry("Italy", "Rome"),
            Map.entry("Spain", "Madrid"),
            Map.entry("Netherlands", "Amsterdam"),
            Map.entry("Sweden", "Stockholm")
        );
        
        System.out.println("Country capitals: " + countryCapitals);
        System.out.println("More countries: " + moreCountries);
        
        // Attempting to modify immutable collections (will throw exception)
        try {
            immutableList.add("orange");
        } catch (UnsupportedOperationException e) {
            System.out.println("Cannot modify immutable list: " + e.getClass().getSimpleName());
        }
        
        // Collection copying utilities
        var mutableList = new ArrayList<>(List.of("a", "b", "c"));
        mutableList.add("d");
        var copiedList = List.copyOf(mutableList); // Creates immutable copy
        
        System.out.println("Original mutable list: " + mutableList);
        System.out.println("Immutable copy: " + copiedList);
        
        // Null handling in collections
        try {
            var listWithNull = List.of("a", null, "c");
        } catch (NullPointerException e) {
            System.out.println("Cannot create List.of() with null elements");
        }
        
        // Creating collections with repeated elements
        var repeatedElements = Collections.nCopies(5, "repeat");
        System.out.println("Repeated elements: " + repeatedElements);
        
        // Collection streaming
        var result = countryCapitals.entrySet().stream()
            .filter(entry -> entry.getValue().length() > 6)
            .map(entry -> entry.getKey() + " -> " + entry.getValue())
            .collect(Collectors.toList());
        
        System.out.println("Countries with long capital names: " + result);
    }
    
    private static void demonstrateFileOperations() {
        try {
            // Reading files (Java 11+)
            var tempFile = Files.createTempFile("modern-java", ".txt");
            var content = """
                Line 1: Modern Java file operations
                Line 2: Reading and writing made easy
                Line 3: Text blocks and file handling
                Line 4: Powerful and concise APIs
                """;
            
            // Writing string to file
            Files.writeString(tempFile, content);
            System.out.println("File written: " + tempFile);
            
            // Reading string from file
            var readContent = Files.readString(tempFile);
            System.out.println("File content:");
            System.out.println(readContent);
            
            // Reading lines
            var lines = Files.readAllLines(tempFile);
            System.out.println("Lines count: " + lines.size());
            lines.forEach(line -> System.out.println("-> " + line));
            
            // Streaming lines
            System.out.println("\nProcessing lines with streams:");
            Files.lines(tempFile)
                .filter(line -> line.contains("Java"))
                .map(String::toUpperCase)
                .forEach(System.out::println);
            
            // File attributes and metadata
            var attributes = Files.readAttributes(tempFile, "*");
            System.out.println("\nFile attributes:");
            attributes.forEach((key, value) -> 
                System.out.println(key + ": " + value));
            
            // Walking file tree
            var tempDir = Files.createTempDirectory("modern-java");
            Files.createDirectories(tempDir.resolve("subdir1/subdir2"));
            Files.createFile(tempDir.resolve("file1.txt"));
            Files.createFile(tempDir.resolve("subdir1/file2.txt"));
            Files.createFile(tempDir.resolve("subdir1/subdir2/file3.txt"));
            
            System.out.println("\nWalking directory tree:");
            Files.walk(tempDir)
                .forEach(path -> {
                    var indent = "  ".repeat(tempDir.relativize(path).getNameCount() - 1);
                    System.out.println(indent + path.getFileName());
                });
            
            // Cleanup
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
            
            Files.deleteIfExists(tempFile);
            
        } catch (IOException e) {
            System.err.println("File operation error: " + e.getMessage());
        }
    }
    
    private static void demonstrateProcessAPI() {
        try {
            // Get current process information
            var currentProcess = ProcessHandle.current();
            System.out.println("Current process PID: " + currentProcess.pid());
            
            var info = currentProcess.info();
            System.out.println("Command: " + info.command().orElse("Unknown"));
            System.out.println("Arguments: " + info.arguments().map(Arrays::toString).orElse("None"));
            System.out.println("Start time: " + info.startInstant().orElse(Instant.MIN));
            System.out.println("CPU time: " + info.totalCpuDuration().orElse(Duration.ZERO));
            
            // List all processes
            System.out.println("\nProcess count: " + ProcessHandle.allProcesses().count());
            
            // Find processes by criteria
            var javaProcesses = ProcessHandle.allProcesses()
                .filter(process -> process.info().command()
                    .map(cmd -> cmd.contains("java"))
                    .orElse(false))
                .limit(5)
                .collect(Collectors.toList());
            
            System.out.println("\nJava processes:");
            javaProcesses.forEach(process -> {
                var processInfo = process.info();
                System.out.printf("PID: %d, Command: %s%n", 
                    process.pid(),
                    processInfo.command().orElse("Unknown"));
            });
            
            // Start a new process (simple command that works on most systems)
            var processBuilder = new ProcessBuilder("java", "-version");
            processBuilder.redirectErrorStream(true);
            
            var process = processBuilder.start();
            var processHandle = process.toHandle();
            
            System.out.println("\nStarted process PID: " + processHandle.pid());
            
            // Wait for process completion with timeout
            var completed = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            
            if (completed) {
                System.out.println("Process exit code: " + process.exitValue());
                
                // Read process output
                var output = new String(process.getInputStream().readAllBytes());
                System.out.println("Process output:");
                System.out.println(output);
            } else {
                System.out.println("Process timed out");
                process.destroyForcibly();
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Process API error: " + e.getMessage());
        }
    }
}
```

---

## 🎯 Summary

**Congratulations!** You've completed **Day 41: Modern Java Features** and mastered:

- **Modern Language Features**: var keyword, switch expressions, text blocks, and pattern matching
- **Records and Sealed Classes**: Immutable data modeling with type safety and pattern matching
- **Enhanced APIs**: New Time API, HTTP Client, Collection factories, and File operations
- **Advanced Pattern Matching**: Complex pattern matching with records and sealed classes
- **Real-World Applications**: Configuration systems, event processing, and data analytics

**Key Takeaways:**
- Modern Java significantly reduces boilerplate code while improving type safety
- Records provide a concise way to create immutable data classes
- Sealed classes enable exhaustive pattern matching and better type modeling
- Text blocks make multi-line strings readable and maintainable
- Pattern matching with instanceof and switch expressions improves code clarity
- Modern APIs provide more intuitive and powerful ways to handle common tasks

**Next Steps:** Tomorrow we'll tackle the **Week 6 Capstone Project** where you'll build a comprehensive functional data processing system using all the advanced features you've learned this week.

**🏆 Day 41 Complete!** You now have the skills to write modern, expressive Java code using the latest language features and APIs.