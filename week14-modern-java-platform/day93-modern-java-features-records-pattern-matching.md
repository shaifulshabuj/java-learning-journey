# Day 93: Advanced Java Features - Records, Pattern Matching & Sealed Classes

## Learning Objectives
- Master Java Records for immutable data modeling
- Implement pattern matching with switch expressions and statements
- Understand and apply sealed classes for type hierarchies
- Combine modern Java features for clean, expressive code
- Build type-safe applications with enhanced compile-time checking

## 1. Java Records Deep Dive

### Understanding Records

Records are a special kind of class designed to be immutable data carriers, reducing boilerplate code significantly.

```java
package com.javajourney.modern;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// Traditional POJO approach
class TraditionalUser {
    private final String id;
    private final String name;
    private final String email;
    private final LocalDateTime createdAt;
    
    public TraditionalUser(String id, String name, String email, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TraditionalUser that = (TraditionalUser) obj;
        return Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(email, that.email) &&
               Objects.equals(createdAt, that.createdAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, createdAt);
    }
    
    @Override
    public String toString() {
        return "TraditionalUser{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}

// Records approach - much more concise!
public record User(String id, String name, String email, LocalDateTime createdAt) {
    
    // Compact constructor for validation
    public User {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    // Additional methods can be added
    public boolean isRecentlyCreated() {
        return createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }
    
    public String getDisplayName() {
        return name + " (" + email + ")";
    }
    
    // Static factory methods
    public static User newUser(String name, String email) {
        return new User(
            java.util.UUID.randomUUID().toString(),
            name,
            email,
            LocalDateTime.now()
        );
    }
}

// Nested records for complex data structures
public record Address(String street, String city, String state, String zipCode) {
    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(zipCode, "Zip code cannot be null");
    }
}

public record UserProfile(
    User user,
    Address address,
    List<String> phoneNumbers,
    boolean isActive
) {
    public UserProfile {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(address, "Address cannot be null");
        Objects.requireNonNull(phoneNumbers, "Phone numbers cannot be null");
        
        // Make defensive copy
        phoneNumbers = List.copyOf(phoneNumbers);
    }
    
    public boolean hasPhoneNumber() {
        return !phoneNumbers.isEmpty();
    }
    
    public String getPrimaryPhone() {
        return phoneNumbers.isEmpty() ? null : phoneNumbers.get(0);
    }
}
```

### Advanced Records Patterns

```java
package com.javajourney.modern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Generic records
public record Result<T, E>(T value, E error) {
    public boolean isSuccess() {
        return error == null;
    }
    
    public boolean isError() {
        return error != null;
    }
    
    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }
    
    public Optional<E> getError() {
        return Optional.ofNullable(error);
    }
    
    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null);
    }
    
    public static <T, E> Result<T, E> error(E error) {
        return new Result<>(null, error);
    }
}

// Builder pattern with records
public record OrderBuilder(
    String id,
    String customerId,
    List<OrderItem> items,
    BigDecimal total,
    LocalDate orderDate,
    OrderStatus status
) {
    public static OrderBuilder create() {
        return new OrderBuilder(null, null, List.of(), BigDecimal.ZERO, LocalDate.now(), OrderStatus.PENDING);
    }
    
    public OrderBuilder withId(String id) {
        return new OrderBuilder(id, customerId, items, total, orderDate, status);
    }
    
    public OrderBuilder withCustomerId(String customerId) {
        return new OrderBuilder(id, customerId, items, total, orderDate, status);
    }
    
    public OrderBuilder withItems(List<OrderItem> items) {
        var newTotal = items.stream()
            .map(OrderItem::price)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderBuilder(id, customerId, items, newTotal, orderDate, status);
    }
    
    public OrderBuilder withStatus(OrderStatus status) {
        return new OrderBuilder(id, customerId, items, total, orderDate, status);
    }
    
    public Order build() {
        if (id == null || customerId == null) {
            throw new IllegalStateException("ID and customer ID are required");
        }
        return new Order(id, customerId, items, total, orderDate, status);
    }
}

public record OrderItem(String productId, String name, int quantity, BigDecimal price) {
    public OrderItem {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(name);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Objects.requireNonNull(price);
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }
    
    public BigDecimal getTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}

public record Order(
    String id,
    String customerId,
    List<OrderItem> items,
    BigDecimal total,
    LocalDate orderDate,
    OrderStatus status
) {
    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(items);
        Objects.requireNonNull(total);
        Objects.requireNonNull(orderDate);
        Objects.requireNonNull(status);
        
        items = List.copyOf(items); // Defensive copy
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
}

enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
```

## 2. Pattern Matching with Switch Expressions

### Modern Switch Expressions

```java
package com.javajourney.modern;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class PatternMatchingExamples {
    
    // Traditional switch statement
    public static String getTraditionalDayType(DayOfWeek day) {
        switch (day) {
            case MONDAY:
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
            case FRIDAY:
                return "Weekday";
            case SATURDAY:
            case SUNDAY:
                return "Weekend";
            default:
                throw new IllegalArgumentException("Invalid day: " + day);
        }
    }
    
    // Modern switch expression
    public static String getDayType(DayOfWeek day) {
        return switch (day) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Weekday";
            case SATURDAY, SUNDAY -> "Weekend";
        };
    }
    
    // Switch expression with complex logic
    public static int calculateWorkingHours(DayOfWeek day, boolean isHoliday) {
        return switch (day) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY -> isHoliday ? 0 : 8;
            case FRIDAY -> isHoliday ? 0 : 6;
            case SATURDAY, SUNDAY -> 0;
        };
    }
    
    // Pattern matching with instance checks
    public static String processObject(Object obj) {
        return switch (obj) {
            case String s -> "String with length: " + s.length();
            case Integer i -> "Integer with value: " + i;
            case Double d -> "Double with value: " + d;
            case null -> "Null value";
            default -> "Unknown type: " + obj.getClass().getSimpleName();
        };
    }
    
    // Pattern matching with guards (when available)
    public static String categorizeNumber(Object obj) {
        return switch (obj) {
            case Integer i when i < 0 -> "Negative integer: " + i;
            case Integer i when i == 0 -> "Zero";
            case Integer i when i > 0 && i <= 10 -> "Small positive integer: " + i;
            case Integer i when i > 10 -> "Large positive integer: " + i;
            case Double d when d < 0.0 -> "Negative double: " + d;
            case Double d when d >= 0.0 -> "Non-negative double: " + d;
            case null -> "Null value";
            default -> "Not a number: " + obj;
        };
    }
    
    // Using switch with records
    public static String processResult(Result<String, String> result) {
        return switch (result) {
            case Result(var value, null) -> "Success: " + value;
            case Result(null, var error) -> "Error: " + error;
            case Result(var value, var error) -> "Invalid state: both value and error present";
        };
    }
    
    // Complex pattern matching with records
    public static String analyzeOrder(Order order) {
        return switch (order) {
            case Order(var id, var customerId, var items, var total, var date, OrderStatus.PENDING) 
                when items.isEmpty() -> "Empty pending order " + id;
            
            case Order(var id, var customerId, var items, var total, var date, OrderStatus.PENDING) 
                when total.compareTo(BigDecimal.valueOf(1000)) > 0 -> 
                "High-value pending order " + id + " ($" + total + ")";
            
            case Order(var id, var customerId, var items, var total, var date, OrderStatus.CONFIRMED) -> 
                "Confirmed order " + id + " with " + items.size() + " items";
            
            case Order(var id, var customerId, var items, var total, var date, OrderStatus.SHIPPED) -> 
                "Shipped order " + id + " on " + date;
            
            case Order(var id, var customerId, var items, var total, var date, OrderStatus.DELIVERED) -> 
                "Delivered order " + id + " (total: $" + total + ")";
            
            case Order(var id, var customerId, var items, var total, var date, OrderStatus.CANCELLED) -> 
                "Cancelled order " + id;
        };
    }
}
```

### Advanced Pattern Matching Scenarios

```java
package com.javajourney.modern;

import java.util.List;
import java.util.Optional;

public class AdvancedPatternMatching {
    
    // Pattern matching with sealed classes (covered in next section)
    public static String processShape(Shape shape) {
        return switch (shape) {
            case Circle(var radius) -> 
                "Circle with area: " + (Math.PI * radius * radius);
            
            case Rectangle(var width, var height) -> 
                "Rectangle with area: " + (width * height);
            
            case Triangle(var base, var height) -> 
                "Triangle with area: " + (0.5 * base * height);
        };
    }
    
    // Pattern matching with collections
    public static String analyzeList(List<Integer> numbers) {
        return switch (numbers.size()) {
            case 0 -> "Empty list";
            case 1 -> "Single element: " + numbers.get(0);
            case 2 -> "Two elements: " + numbers.get(0) + ", " + numbers.get(1);
            default -> {
                int sum = numbers.stream().mapToInt(Integer::intValue).sum();
                yield "List of " + numbers.size() + " elements, sum: " + sum;
            }
        };
    }
    
    // Pattern matching with optional
    public static String processOptional(Optional<String> optional) {
        return switch (optional.isEmpty() ? "empty" : "present") {
            case "empty" -> "No value present";
            case "present" -> "Value: " + optional.get();
            default -> "Invalid state";
        };
    }
    
    // Nested pattern matching
    public static String processNestedData(Result<Order, String> orderResult) {
        return switch (orderResult) {
            case Result(Order order, null) -> switch (order.status()) {
                case PENDING -> "Processing pending order: " + order.id();
                case CONFIRMED -> "Order confirmed: " + order.id();
                case SHIPPED -> "Order shipped: " + order.id();
                case DELIVERED -> "Order delivered: " + order.id();
                case CANCELLED -> "Order cancelled: " + order.id();
            };
            
            case Result(null, var error) -> "Failed to get order: " + error;
            
            case Result(var order, var error) -> "Invalid state: " + error;
        };
    }
    
    // Pattern matching in methods
    public static double calculateArea(Shape shape) {
        return switch (shape) {
            case Circle(var radius) -> Math.PI * radius * radius;
            case Rectangle(var width, var height) -> width * height;
            case Triangle(var base, var height) -> 0.5 * base * height;
        };
    }
    
    public static boolean isLarge(Shape shape) {
        return switch (shape) {
            case Circle(var radius) when radius > 10 -> true;
            case Rectangle(var width, var height) when width > 20 || height > 20 -> true;
            case Triangle(var base, var height) when base > 15 && height > 15 -> true;
            default -> false;
        };
    }
}
```

## 3. Sealed Classes and Interfaces

### Understanding Sealed Classes

Sealed classes restrict which classes can extend them, providing better control over type hierarchies.

```java
package com.javajourney.modern;

// Sealed class defining a restricted hierarchy
public sealed class Shape permits Circle, Rectangle, Triangle {
    
    public abstract double area();
    public abstract double perimeter();
    
    // Common methods for all shapes
    public String getShapeInfo() {
        return getClass().getSimpleName() + " - Area: " + area() + ", Perimeter: " + perimeter();
    }
}

// Permitted subclasses
public final class Circle extends Shape {
    private final double radius;
    
    public Circle(double radius) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        this.radius = radius;
    }
    
    public double radius() {
        return radius;
    }
    
    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
    
    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }
    
    @Override
    public String toString() {
        return "Circle[radius=" + radius + "]";
    }
}

public final class Rectangle extends Shape {
    private final double width;
    private final double height;
    
    public Rectangle(double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        this.width = width;
        this.height = height;
    }
    
    public double width() {
        return width;
    }
    
    public double height() {
        return height;
    }
    
    @Override
    public double area() {
        return width * height;
    }
    
    @Override
    public double perimeter() {
        return 2 * (width + height);
    }
    
    public boolean isSquare() {
        return Math.abs(width - height) < 0.001;
    }
    
    @Override
    public String toString() {
        return "Rectangle[width=" + width + ", height=" + height + "]";
    }
}

public final class Triangle extends Shape {
    private final double base;
    private final double height;
    
    public Triangle(double base, double height) {
        if (base <= 0 || height <= 0) {
            throw new IllegalArgumentException("Base and height must be positive");
        }
        this.base = base;
        this.height = height;
    }
    
    public double base() {
        return base;
    }
    
    public double height() {
        return height;
    }
    
    @Override
    public double area() {
        return 0.5 * base * height;
    }
    
    @Override
    public double perimeter() {
        // Assuming right triangle for simplicity
        double hypotenuse = Math.sqrt(base * base + height * height);
        return base + height + hypotenuse;
    }
    
    @Override
    public String toString() {
        return "Triangle[base=" + base + ", height=" + height + "]";
    }
}
```

### Sealed Interfaces and Complex Hierarchies

```java
package com.javajourney.modern;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Sealed interface for API responses
public sealed interface ApiResponse permits SuccessResponse, ErrorResponse, LoadingResponse {
    LocalDateTime timestamp();
    String requestId();
}

public record SuccessResponse<T>(
    T data,
    LocalDateTime timestamp,
    String requestId,
    Map<String, String> metadata
) implements ApiResponse {}

public record ErrorResponse(
    String errorCode,
    String message,
    List<String> details,
    LocalDateTime timestamp,
    String requestId
) implements ApiResponse {}

public record LoadingResponse(
    String message,
    double progress,
    LocalDateTime timestamp,
    String requestId
) implements ApiResponse {}

// Sealed class for database operations
public sealed class DatabaseOperation permits QueryOperation, InsertOperation, UpdateOperation, DeleteOperation {
    protected final String tableName;
    protected final LocalDateTime createdAt;
    
    protected DatabaseOperation(String tableName) {
        this.tableName = tableName;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public abstract String toSql();
}

public final class QueryOperation extends DatabaseOperation {
    private final List<String> columns;
    private final String whereClause;
    private final String orderBy;
    private final Integer limit;
    
    public QueryOperation(String tableName, List<String> columns, String whereClause, String orderBy, Integer limit) {
        super(tableName);
        this.columns = columns != null ? List.copyOf(columns) : List.of("*");
        this.whereClause = whereClause;
        this.orderBy = orderBy;
        this.limit = limit;
    }
    
    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(String.join(", ", columns));
        sql.append(" FROM ").append(tableName);
        
        if (whereClause != null && !whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        if (orderBy != null && !orderBy.isBlank()) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        
        return sql.toString();
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public String getWhereClause() {
        return whereClause;
    }
}

public final class InsertOperation extends DatabaseOperation {
    private final Map<String, Object> values;
    
    public InsertOperation(String tableName, Map<String, Object> values) {
        super(tableName);
        this.values = Map.copyOf(values);
    }
    
    @Override
    public String toSql() {
        if (values.isEmpty()) {
            throw new IllegalStateException("No values to insert");
        }
        
        String columns = String.join(", ", values.keySet());
        String placeholders = values.keySet().stream()
            .map(k -> "?")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }
    
    public Map<String, Object> getValues() {
        return values;
    }
}

public final class UpdateOperation extends DatabaseOperation {
    private final Map<String, Object> values;
    private final String whereClause;
    
    public UpdateOperation(String tableName, Map<String, Object> values, String whereClause) {
        super(tableName);
        this.values = Map.copyOf(values);
        this.whereClause = whereClause;
    }
    
    @Override
    public String toSql() {
        if (values.isEmpty()) {
            throw new IllegalStateException("No values to update");
        }
        
        String setClause = values.keySet().stream()
            .map(key -> key + " = ?")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ").append(setClause);
        
        if (whereClause != null && !whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        return sql.toString();
    }
    
    public Map<String, Object> getValues() {
        return values;
    }
    
    public String getWhereClause() {
        return whereClause;
    }
}

public final class DeleteOperation extends DatabaseOperation {
    private final String whereClause;
    
    public DeleteOperation(String tableName, String whereClause) {
        super(tableName);
        this.whereClause = whereClause;
    }
    
    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(tableName);
        
        if (whereClause != null && !whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        return sql.toString();
    }
    
    public String getWhereClause() {
        return whereClause;
    }
}
```

## 4. Combining Modern Features

### Real-World Application Example

```java
package com.javajourney.modern;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Modern Java application combining all features
public class ModernOrderProcessingSystem {
    
    // Sealed interface for order events
    public sealed interface OrderEvent permits OrderCreated, OrderConfirmed, OrderShipped, OrderDelivered, OrderCancelled {
        String orderId();
        LocalDateTime timestamp();
        String userId();
    }
    
    public record OrderCreated(String orderId, LocalDateTime timestamp, String userId, List<OrderItem> items) 
        implements OrderEvent {}
    
    public record OrderConfirmed(String orderId, LocalDateTime timestamp, String userId, BigDecimal total) 
        implements OrderEvent {}
    
    public record OrderShipped(String orderId, LocalDateTime timestamp, String userId, String trackingNumber) 
        implements OrderEvent {}
    
    public record OrderDelivered(String orderId, LocalDateTime timestamp, String userId, String deliveryLocation) 
        implements OrderEvent {}
    
    public record OrderCancelled(String orderId, LocalDateTime timestamp, String userId, String reason) 
        implements OrderEvent {}
    
    // Processing orders with pattern matching
    public static String processOrderEvent(OrderEvent event) {
        return switch (event) {
            case OrderCreated(var orderId, var timestamp, var userId, var items) -> {
                var itemCount = items.size();
                var total = items.stream()
                    .map(OrderItem::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield String.format("Order %s created by user %s with %d items (total: $%s)", 
                    orderId, userId, itemCount, total);
            }
            
            case OrderConfirmed(var orderId, var timestamp, var userId, var total) ->
                String.format("Order %s confirmed for user %s (total: $%s)", orderId, userId, total);
            
            case OrderShipped(var orderId, var timestamp, var userId, var trackingNumber) ->
                String.format("Order %s shipped to user %s (tracking: %s)", orderId, userId, trackingNumber);
            
            case OrderDelivered(var orderId, var timestamp, var userId, var deliveryLocation) ->
                String.format("Order %s delivered to user %s at %s", orderId, userId, deliveryLocation);
            
            case OrderCancelled(var orderId, var timestamp, var userId, var reason) ->
                String.format("Order %s cancelled for user %s (reason: %s)", orderId, userId, reason);
        };
    }
    
    // Business logic with pattern matching
    public static OrderProcessingResult processOrder(Order order, List<OrderEvent> events) {
        return switch (order.status()) {
            case PENDING -> validatePendingOrder(order);
            case CONFIRMED -> processConfirmedOrder(order, events);
            case SHIPPED -> trackShippedOrder(order, events);
            case DELIVERED -> completeDeliveredOrder(order);
            case CANCELLED -> handleCancelledOrder(order, events);
        };
    }
    
    private static OrderProcessingResult validatePendingOrder(Order order) {
        return switch (order.items().size()) {
            case 0 -> OrderProcessingResult.error("Order cannot be empty");
            case 1 -> {
                var item = order.items().get(0);
                if (item.quantity() > 100) {
                    yield OrderProcessingResult.warning("Large quantity order requires approval");
                } else {
                    yield OrderProcessingResult.success("Single item order validated");
                }
            }
            default -> {
                if (order.total().compareTo(BigDecimal.valueOf(10000)) > 0) {
                    yield OrderProcessingResult.warning("High-value order requires manual review");
                } else {
                    yield OrderProcessingResult.success("Multi-item order validated");
                }
            }
        };
    }
    
    private static OrderProcessingResult processConfirmedOrder(Order order, List<OrderEvent> events) {
        var hasConfirmationEvent = events.stream()
            .anyMatch(event -> event instanceof OrderConfirmed);
        
        return hasConfirmationEvent 
            ? OrderProcessingResult.success("Order ready for shipping")
            : OrderProcessingResult.error("Missing confirmation event");
    }
    
    private static OrderProcessingResult trackShippedOrder(Order order, List<OrderEvent> events) {
        return events.stream()
            .filter(event -> event instanceof OrderShipped)
            .findFirst()
            .map(event -> switch (event) {
                case OrderShipped(var orderId, var timestamp, var userId, var trackingNumber) ->
                    OrderProcessingResult.success("Order tracked: " + trackingNumber);
                default -> OrderProcessingResult.error("Invalid shipping event");
            })
            .orElse(OrderProcessingResult.error("No shipping event found"));
    }
    
    private static OrderProcessingResult completeDeliveredOrder(Order order) {
        return OrderProcessingResult.success("Order delivery completed");
    }
    
    private static OrderProcessingResult handleCancelledOrder(Order order, List<OrderEvent> events) {
        return events.stream()
            .filter(event -> event instanceof OrderCancelled)
            .findFirst()
            .map(event -> switch (event) {
                case OrderCancelled(var orderId, var timestamp, var userId, var reason) ->
                    OrderProcessingResult.success("Order cancelled: " + reason);
                default -> OrderProcessingResult.error("Invalid cancellation event");
            })
            .orElse(OrderProcessingResult.error("Missing cancellation reason"));
    }
    
    // Result type using sealed interface
    public sealed interface OrderProcessingResult permits Success, Warning, Error {
        String message();
        
        default boolean isSuccess() {
            return this instanceof Success;
        }
        
        default boolean isWarning() {
            return this instanceof Warning;
        }
        
        default boolean isError() {
            return this instanceof Error;
        }
    }
    
    public record Success(String message) implements OrderProcessingResult {
        public static Success of(String message) {
            return new Success(message);
        }
    }
    
    public record Warning(String message) implements OrderProcessingResult {
        public static Warning of(String message) {
            return new Warning(message);
        }
    }
    
    public record Error(String message) implements OrderProcessingResult {
        public static Error of(String message) {
            return new Error(message);
        }
    }
    
    // Factory methods using modern features
    public static class OrderProcessingResult {
        public static Success success(String message) {
            return Success.of(message);
        }
        
        public static Warning warning(String message) {
            return Warning.of(message);
        }
        
        public static Error error(String message) {
            return Error.of(message);
        }
    }
    
    // Utility methods for working with results
    public static String formatResult(OrderProcessingResult result) {
        return switch (result) {
            case Success(var message) -> "✓ " + message;
            case Warning(var message) -> "⚠ " + message;
            case Error(var message) -> "✗ " + message;
        };
    }
    
    public static Optional<String> getErrorMessage(OrderProcessingResult result) {
        return switch (result) {
            case Error(var message) -> Optional.of(message);
            default -> Optional.empty();
        };
    }
}
```

### Advanced Usage Examples

```java
package com.javajourney.modern;

import java.util.List;
import java.util.stream.Collectors;

public class ModernJavaDemo {
    
    public static void main(String[] args) {
        demonstrateRecords();
        demonstratePatternMatching();
        demonstrateSealedClasses();
        demonstrateCombinedFeatures();
    }
    
    private static void demonstrateRecords() {
        System.out.println("=== Records Demo ===");
        
        // Create users
        var user1 = User.newUser("John Doe", "john@example.com");
        var user2 = User.newUser("Jane Smith", "jane@example.com");
        
        System.out.println("User 1: " + user1);
        System.out.println("User 2: " + user2);
        System.out.println("User 1 recently created: " + user1.isRecentlyCreated());
        
        // Create profile
        var address = new Address("123 Main St", "Anytown", "CA", "12345");
        var profile = new UserProfile(user1, address, List.of("555-1234", "555-5678"), true);
        
        System.out.println("Profile: " + profile);
        System.out.println("Primary phone: " + profile.getPrimaryPhone());
    }
    
    private static void demonstratePatternMatching() {
        System.out.println("\n=== Pattern Matching Demo ===");
        
        // Create shapes
        List<Shape> shapes = List.of(
            new Circle(5.0),
            new Rectangle(4.0, 6.0),
            new Triangle(3.0, 4.0)
        );
        
        shapes.forEach(shape -> {
            System.out.println("Shape: " + shape);
            System.out.println("Analysis: " + AdvancedPatternMatching.processShape(shape));
            System.out.println("Area: " + AdvancedPatternMatching.calculateArea(shape));
            System.out.println("Is large: " + AdvancedPatternMatching.isLarge(shape));
            System.out.println();
        });
        
        // Test with different objects
        List<Object> objects = List.of("Hello", 42, 3.14, null, List.of(1, 2, 3));
        objects.forEach(obj -> 
            System.out.println("Object " + obj + " -> " + PatternMatchingExamples.processObject(obj))
        );
    }
    
    private static void demonstrateSealedClasses() {
        System.out.println("\n=== Sealed Classes Demo ===");
        
        // Create API responses
        var successResponse = new SuccessResponse<String>(
            "Operation completed successfully",
            LocalDateTime.now(),
            "req-123",
            Map.of("version", "1.0", "server", "app-01")
        );
        
        var errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Invalid input data",
            List.of("Name is required", "Email format is invalid"),
            LocalDateTime.now(),
            "req-124"
        );
        
        List<ApiResponse> responses = List.of(successResponse, errorResponse);
        responses.forEach(response -> {
            String result = switch (response) {
                case SuccessResponse<?> sr -> "Success: " + sr.data();
                case ErrorResponse er -> "Error: " + er.message() + " (" + er.errorCode() + ")";
                case LoadingResponse lr -> "Loading: " + lr.message() + " (" + lr.progress() + "%)";
            };
            System.out.println(result);
        });
    }
    
    private static void demonstrateCombinedFeatures() {
        System.out.println("\n=== Combined Features Demo ===");
        
        // Create order
        var items = List.of(
            new OrderItem("prod-1", "Laptop", 1, BigDecimal.valueOf(999.99)),
            new OrderItem("prod-2", "Mouse", 2, BigDecimal.valueOf(29.99))
        );
        
        var order = OrderBuilder.create()
            .withId("order-123")
            .withCustomerId("customer-456")
            .withItems(items)
            .withStatus(OrderStatus.PENDING)
            .build();
        
        System.out.println("Order: " + order);
        
        // Create events
        var events = List.of(
            new ModernOrderProcessingSystem.OrderCreated(
                order.id(), 
                LocalDateTime.now(), 
                order.customerId(), 
                order.items()
            )
        );
        
        // Process order
        var result = ModernOrderProcessingSystem.processOrder(order, events);
        System.out.println("Processing result: " + ModernOrderProcessingSystem.formatResult(result));
        
        // Process events
        events.forEach(event -> 
            System.out.println("Event: " + ModernOrderProcessingSystem.processOrderEvent(event))
        );
    }
}
```

## Practice Exercises

### Exercise 1: Records Migration

Convert a traditional class hierarchy to use records, including validation and factory methods.

### Exercise 2: Pattern Matching Implementation

Implement a mathematical expression evaluator using sealed classes and pattern matching.

### Exercise 3: API Design with Modern Features

Design a REST API response system using sealed interfaces, records, and pattern matching.

### Exercise 4: Business Logic Modernization

Refactor existing business logic to use modern Java features for better type safety and expressiveness.

## Key Takeaways

1. **Records**: Immutable data carriers that eliminate boilerplate code while providing built-in equals, hashCode, and toString methods
2. **Pattern Matching**: Enhanced switch expressions enable more expressive and type-safe conditional logic
3. **Sealed Classes**: Restrict inheritance hierarchies for better control and exhaustive pattern matching
4. **Combined Power**: Modern features work together to create more maintainable, type-safe, and expressive code
5. **Migration Strategy**: Legacy code can be incrementally modernized to leverage these new language features

Modern Java features represent a significant evolution in the language, enabling developers to write more concise, safe, and expressive code while maintaining backward compatibility.