# Day 160: Advanced Pattern Matching and Switch Expressions

## Learning Objectives
By the end of this session, you will:
- Master advanced pattern matching with record patterns and guard conditions
- Implement sophisticated switch expressions with pattern matching
- Utilize sealed classes effectively with pattern matching for type safety
- Apply performance optimization techniques in pattern matching scenarios
- Build type-safe and expressive APIs using modern pattern matching features
- Create complex data processing pipelines with pattern matching

## Theoretical Foundation

### Pattern Matching Evolution

**Understanding Modern Pattern Matching:**
```java
// Evolution of pattern matching in Java
public class PatternMatchingEvolution {
    
    /**
     * Pattern Matching Timeline:
     * Java 14: instanceof pattern matching (preview)
     * Java 16: instanceof pattern matching (standard)
     * Java 17: Switch expressions with patterns (preview)
     * Java 19: Record patterns (preview)
     * Java 20: Pattern matching for switch (fourth preview)
     * Java 21: Pattern matching for switch (standard)
     * 
     * Advanced Features:
     * - Record patterns with deconstruction
     * - Guard patterns with when clauses
     * - Nested pattern matching
     * - Pattern matching with sealed classes
     * - Exhaustiveness checking
     */
    
    public void demonstratePatternMatchingCapabilities() {
        System.out.println("=== Advanced Pattern Matching Features ===");
        
        Object obj = new Point(3, 4);
        
        // Traditional approach
        if (obj instanceof Point) {
            Point p = (Point) obj;
            System.out.println("Traditional: Point(" + p.x() + ", " + p.y() + ")");
        }
        
        // Modern pattern matching
        if (obj instanceof Point(var x, var y)) {
            System.out.println("Pattern matching: Point(" + x + ", " + y + ")");
        }
        
        // Switch expression with patterns
        String result = switch (obj) {
            case Point(var x, var y) when x > 0 && y > 0 -> 
                "Point in first quadrant: (" + x + ", " + y + ")";
            case Point(var x, var y) -> 
                "Point at (" + x + ", " + y + ")";
            case String s -> "String: " + s;
            case Integer i -> "Integer: " + i;
            default -> "Unknown type: " + obj.getClass().getSimpleName();
        };
        
        System.out.println(result);
    }
    
    // Records for pattern matching
    record Point(int x, int y) {}
    record Circle(Point center, double radius) {}
    record Rectangle(Point topLeft, Point bottomRight) {}
}
```

### Sealed Classes and Pattern Matching

**Type-Safe Hierarchies:**
```java
// Sealed class hierarchy for expression evaluation
public sealed interface Expression 
    permits Literal, Addition, Multiplication, Division, Variable {
    
    // Common method for all expressions
    double evaluate(Map<String, Double> variables);
}

record Literal(double value) implements Expression {
    @Override
    public double evaluate(Map<String, Double> variables) {
        return value;
    }
}

record Addition(Expression left, Expression right) implements Expression {
    @Override
    public double evaluate(Map<String, Double> variables) {
        return left.evaluate(variables) + right.evaluate(variables);
    }
}

record Multiplication(Expression left, Expression right) implements Expression {
    @Override
    public double evaluate(Map<String, Double> variables) {
        return left.evaluate(variables) * right.evaluate(variables);
    }
}

record Division(Expression left, Expression right) implements Expression {
    @Override
    public double evaluate(Map<String, Double> variables) {
        double rightValue = right.evaluate(variables);
        if (rightValue == 0) throw new ArithmeticException("Division by zero");
        return left.evaluate(variables) / rightValue;
    }
}

record Variable(String name) implements Expression {
    @Override
    public double evaluate(Map<String, Double> variables) {
        Double value = variables.get(name);
        if (value == null) throw new IllegalArgumentException("Undefined variable: " + name);
        return value;
    }
}

// Advanced expression evaluator using pattern matching
public class ExpressionEvaluator {
    
    public String formatExpression(Expression expr) {
        return switch (expr) {
            case Literal(var value) -> String.valueOf(value);
            case Variable(var name) -> name;
            case Addition(var left, var right) -> 
                "(" + formatExpression(left) + " + " + formatExpression(right) + ")";
            case Multiplication(var left, var right) -> 
                "(" + formatExpression(left) + " * " + formatExpression(right) + ")";
            case Division(var left, var right) -> 
                "(" + formatExpression(left) + " / " + formatExpression(right) + ")";
        };
    }
    
    public Expression simplify(Expression expr) {
        return switch (expr) {
            // Simplify addition
            case Addition(Literal(0.0), var right) -> simplify(right);
            case Addition(var left, Literal(0.0)) -> simplify(left);
            case Addition(Literal(var a), Literal(var b)) -> new Literal(a + b);
            case Addition(var left, var right) -> 
                new Addition(simplify(left), simplify(right));
            
            // Simplify multiplication
            case Multiplication(Literal(0.0), var right) -> new Literal(0.0);
            case Multiplication(var left, Literal(0.0)) -> new Literal(0.0);
            case Multiplication(Literal(1.0), var right) -> simplify(right);
            case Multiplication(var left, Literal(1.0)) -> simplify(left);
            case Multiplication(Literal(var a), Literal(var b)) -> new Literal(a * b);
            case Multiplication(var left, var right) -> 
                new Multiplication(simplify(left), simplify(right));
            
            // Simplify division
            case Division(Literal(0.0), var right) -> new Literal(0.0);
            case Division(var left, Literal(1.0)) -> simplify(left);
            case Division(Literal(var a), Literal(var b)) when b != 0.0 -> new Literal(a / b);
            case Division(var left, var right) -> 
                new Division(simplify(left), simplify(right));
            
            // Base cases
            case Literal(var value) -> expr;
            case Variable(var name) -> expr;
        };
    }
    
    public Set<String> getVariables(Expression expr) {
        return switch (expr) {
            case Literal(var value) -> Set.of();
            case Variable(var name) -> Set.of(name);
            case Addition(var left, var right) -> {
                Set<String> result = new HashSet<>(getVariables(left));
                result.addAll(getVariables(right));
                yield result;
            }
            case Multiplication(var left, var right) -> {
                Set<String> result = new HashSet<>(getVariables(left));
                result.addAll(getVariables(right));
                yield result;
            }
            case Division(var left, var right) -> {
                Set<String> result = new HashSet<>(getVariables(left));
                result.addAll(getVariables(right));
                yield result;
            }
        };
    }
}
```

## Advanced Pattern Matching Techniques

### Nested Pattern Matching

**Complex Data Structure Processing:**
```java
// Geometric shapes with nested patterns
public sealed interface Shape 
    permits Point, Line, Polygon, Circle, Group {
}

record Point(double x, double y) implements Shape {}
record Line(Point start, Point end) implements Shape {}
record Circle(Point center, double radius) implements Shape {}
record Polygon(List<Point> vertices) implements Shape {}
record Group(List<Shape> shapes) implements Shape {}

public class GeometryProcessor {
    
    public double calculateArea(Shape shape) {
        return switch (shape) {
            case Point(var x, var y) -> 0.0;
            case Line(var start, var end) -> 0.0;
            case Circle(var center, var radius) -> Math.PI * radius * radius;
            case Polygon(var vertices) -> calculatePolygonArea(vertices);
            case Group(var shapes) -> shapes.stream()
                .mapToDouble(this::calculateArea)
                .sum();
        };
    }
    
    public Shape translate(Shape shape, double dx, double dy) {
        return switch (shape) {
            case Point(var x, var y) -> new Point(x + dx, y + dy);
            case Line(var start, var end) -> 
                new Line(translate(start, dx, dy), translate(end, dx, dy));
            case Circle(var center, var radius) -> 
                new Circle((Point) translate(center, dx, dy), radius);
            case Polygon(var vertices) -> new Polygon(
                vertices.stream()
                    .map(p -> (Point) translate(p, dx, dy))
                    .toList()
            );
            case Group(var shapes) -> new Group(
                shapes.stream()
                    .map(s -> translate(s, dx, dy))
                    .toList()
            );
        };
    }
    
    public String describe(Shape shape) {
        return switch (shape) {
            case Point(var x, var y) -> 
                String.format("Point at (%.1f, %.1f)", x, y);
            case Line(Point(var x1, var y1), Point(var x2, var y2)) -> 
                String.format("Line from (%.1f, %.1f) to (%.1f, %.1f)", x1, y1, x2, y2);
            case Circle(Point(var cx, var cy), var radius) -> 
                String.format("Circle at (%.1f, %.1f) with radius %.1f", cx, cy, radius);
            case Polygon(var vertices) when vertices.size() == 3 -> 
                "Triangle with " + vertices.size() + " vertices";
            case Polygon(var vertices) when vertices.size() == 4 -> 
                "Quadrilateral with " + vertices.size() + " vertices";
            case Polygon(var vertices) -> 
                "Polygon with " + vertices.size() + " vertices";
            case Group(var shapes) -> 
                "Group containing " + shapes.size() + " shapes";
        };
    }
    
    public List<Point> getAllPoints(Shape shape) {
        return switch (shape) {
            case Point p -> List.of(p);
            case Line(var start, var end) -> List.of(start, end);
            case Circle(var center, var radius) -> List.of(center);
            case Polygon(var vertices) -> vertices;
            case Group(var shapes) -> shapes.stream()
                .flatMap(s -> getAllPoints(s).stream())
                .toList();
        };
    }
    
    private double calculatePolygonArea(List<Point> vertices) {
        if (vertices.size() < 3) return 0.0;
        
        double area = 0.0;
        for (int i = 0; i < vertices.size(); i++) {
            Point current = vertices.get(i);
            Point next = vertices.get((i + 1) % vertices.size());
            area += current.x() * next.y() - next.x() * current.y();
        }
        return Math.abs(area) / 2.0;
    }
}
```

### Guard Patterns and Conditional Matching

**Advanced Conditional Logic:**
```java
// Financial transaction processing with guard patterns
public sealed interface Transaction 
    permits Purchase, Refund, Transfer, Deposit, Withdrawal {
}

record Purchase(String merchant, double amount, String category) implements Transaction {}
record Refund(String merchant, double amount, String reason) implements Transaction {}
record Transfer(String fromAccount, String toAccount, double amount) implements Transaction {}
record Deposit(String account, double amount, String source) implements Transaction {}
record Withdrawal(String account, double amount, String location) implements Transaction {}

public class TransactionProcessor {
    private static final double LARGE_TRANSACTION_THRESHOLD = 10000.0;
    private static final double SUSPICIOUS_CASH_THRESHOLD = 5000.0;
    
    public String categorizeTransaction(Transaction transaction) {
        return switch (transaction) {
            // Large transactions requiring special handling
            case Purchase(var merchant, var amount, var category) 
                when amount > LARGE_TRANSACTION_THRESHOLD -> 
                "LARGE_PURCHASE: " + merchant + " - $" + amount;
            
            case Transfer(var from, var to, var amount) 
                when amount > LARGE_TRANSACTION_THRESHOLD -> 
                "LARGE_TRANSFER: " + from + " → " + to + " - $" + amount;
            
            // Suspicious cash transactions
            case Withdrawal(var account, var amount, var location) 
                when amount > SUSPICIOUS_CASH_THRESHOLD && location.contains("ATM") -> 
                "SUSPICIOUS_CASH: ATM withdrawal of $" + amount + " at " + location;
            
            case Deposit(var account, var amount, var source) 
                when amount > SUSPICIOUS_CASH_THRESHOLD && source.equals("CASH") -> 
                "SUSPICIOUS_CASH: Cash deposit of $" + amount;
            
            // Category-specific purchases
            case Purchase(var merchant, var amount, var category) 
                when category.equals("GAMBLING") -> 
                "GAMBLING: " + merchant + " - $" + amount;
            
            case Purchase(var merchant, var amount, var category) 
                when category.equals("CRYPTO") -> 
                "CRYPTO: " + merchant + " - $" + amount;
            
            // Regular transactions
            case Purchase(var merchant, var amount, var category) -> 
                "PURCHASE: " + merchant + " (" + category + ") - $" + amount;
            
            case Refund(var merchant, var amount, var reason) -> 
                "REFUND: " + merchant + " - $" + amount + " (" + reason + ")";
            
            case Transfer(var from, var to, var amount) -> 
                "TRANSFER: " + from + " → " + to + " - $" + amount;
            
            case Deposit(var account, var amount, var source) -> 
                "DEPOSIT: $" + amount + " from " + source;
            
            case Withdrawal(var account, var amount, var location) -> 
                "WITHDRAWAL: $" + amount + " at " + location;
        };
    }
    
    public boolean requiresApproval(Transaction transaction) {
        return switch (transaction) {
            case Purchase(var merchant, var amount, var category) 
                when amount > LARGE_TRANSACTION_THRESHOLD || 
                     category.equals("GAMBLING") || 
                     category.equals("CRYPTO") -> true;
            
            case Transfer(var from, var to, var amount) 
                when amount > LARGE_TRANSACTION_THRESHOLD -> true;
            
            case Withdrawal(var account, var amount, var location) 
                when amount > SUSPICIOUS_CASH_THRESHOLD -> true;
            
            case Deposit(var account, var amount, var source) 
                when amount > SUSPICIOUS_CASH_THRESHOLD && source.equals("CASH") -> true;
            
            default -> false;
        };
    }
    
    public double calculateFee(Transaction transaction) {
        return switch (transaction) {
            case Purchase(var merchant, var amount, var category) -> 0.0; // No fee for purchases
            
            case Transfer(var from, var to, var amount) 
                when from.startsWith("INT") || to.startsWith("INT") -> amount * 0.03; // International transfer
            
            case Transfer(var from, var to, var amount) -> 
                amount > 1000 ? 5.0 : 2.0; // Domestic transfer
            
            case Withdrawal(var account, var amount, var location) 
                when !location.contains("OWN_BANK") -> 3.50; // ATM fee
            
            case Withdrawal(var account, var amount, var location) -> 0.0; // Own bank ATM
            
            default -> 0.0;
        };
    }
    
    public List<String> generateAlerts(Transaction transaction) {
        List<String> alerts = new ArrayList<>();
        
        switch (transaction) {
            case Purchase(var merchant, var amount, var category) 
                when amount > LARGE_TRANSACTION_THRESHOLD -> 
                alerts.add("Large purchase alert: $" + amount);
            
            case Purchase(var merchant, var amount, var category) 
                when category.equals("GAMBLING") -> 
                alerts.add("Gambling transaction alert");
            
            case Transfer(var from, var to, var amount) 
                when amount > LARGE_TRANSACTION_THRESHOLD -> 
                alerts.add("Large transfer alert: $" + amount);
            
            case Withdrawal(var account, var amount, var location) 
                when amount > SUSPICIOUS_CASH_THRESHOLD -> 
                alerts.add("Large cash withdrawal: $" + amount + " at " + location);
            
            case Deposit(var account, var amount, var source) 
                when amount > SUSPICIOUS_CASH_THRESHOLD && source.equals("CASH") -> 
                alerts.add("Large cash deposit: $" + amount);
            
            default -> { /* No alerts for regular transactions */ }
        }
        
        return alerts;
    }
}
```

## High-Performance Pattern Matching

### Optimized Switch Expressions

**Performance-Critical Pattern Matching:**
```java
// High-performance data processing with pattern matching
public class DataProcessor {
    
    // Sealed interface for different data types
    public sealed interface DataValue 
        permits IntValue, LongValue, DoubleValue, StringValue, BooleanValue, ListValue, MapValue {
    }
    
    record IntValue(int value) implements DataValue {}
    record LongValue(long value) implements DataValue {}
    record DoubleValue(double value) implements DataValue {}
    record StringValue(String value) implements DataValue {}
    record BooleanValue(boolean value) implements DataValue {}
    record ListValue(List<DataValue> values) implements DataValue {}
    record MapValue(Map<String, DataValue> values) implements DataValue {}
    
    // High-performance type conversion
    public double toDouble(DataValue value) {
        return switch (value) {
            case IntValue(var i) -> (double) i;
            case LongValue(var l) -> (double) l;
            case DoubleValue(var d) -> d;
            case StringValue(var s) -> {
                try {
                    yield Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    yield 0.0;
                }
            }
            case BooleanValue(var b) -> b ? 1.0 : 0.0;
            default -> 0.0;
        };
    }
    
    // Optimized comparison operations
    public int compare(DataValue a, DataValue b) {
        return switch (a) {
            case IntValue(var aVal) -> switch (b) {
                case IntValue(var bVal) -> Integer.compare(aVal, bVal);
                case LongValue(var bVal) -> Long.compare(aVal, bVal);
                case DoubleValue(var bVal) -> Double.compare(aVal, bVal);
                default -> Double.compare(aVal, toDouble(b));
            };
            
            case LongValue(var aVal) -> switch (b) {
                case IntValue(var bVal) -> Long.compare(aVal, bVal);
                case LongValue(var bVal) -> Long.compare(aVal, bVal);
                case DoubleValue(var bVal) -> Double.compare(aVal, bVal);
                default -> Double.compare(aVal, toDouble(b));
            };
            
            case DoubleValue(var aVal) -> switch (b) {
                case IntValue(var bVal) -> Double.compare(aVal, bVal);
                case LongValue(var bVal) -> Double.compare(aVal, bVal);
                case DoubleValue(var bVal) -> Double.compare(aVal, bVal);
                default -> Double.compare(aVal, toDouble(b));
            };
            
            case StringValue(var aVal) -> switch (b) {
                case StringValue(var bVal) -> aVal.compareTo(bVal);
                default -> aVal.compareTo(String.valueOf(toDouble(b)));
            };
            
            case BooleanValue(var aVal) -> switch (b) {
                case BooleanValue(var bVal) -> Boolean.compare(aVal, bVal);
                default -> Double.compare(aVal ? 1.0 : 0.0, toDouble(b));
            };
            
            default -> Double.compare(toDouble(a), toDouble(b));
        };
    }
    
    // Efficient aggregation operations
    public DataValue aggregate(List<DataValue> values, String operation) {
        if (values.isEmpty()) return new DoubleValue(0.0);
        
        return switch (operation.toUpperCase()) {
            case "SUM" -> {
                double sum = values.stream().mapToDouble(this::toDouble).sum();
                yield new DoubleValue(sum);
            }
            
            case "AVERAGE" -> {
                double avg = values.stream().mapToDouble(this::toDouble).average().orElse(0.0);
                yield new DoubleValue(avg);
            }
            
            case "MAX" -> {
                DataValue max = values.get(0);
                for (int i = 1; i < values.size(); i++) {
                    if (compare(values.get(i), max) > 0) {
                        max = values.get(i);
                    }
                }
                yield max;
            }
            
            case "MIN" -> {
                DataValue min = values.get(0);
                for (int i = 1; i < values.size(); i++) {
                    if (compare(values.get(i), min) < 0) {
                        min = values.get(i);
                    }
                }
                yield min;
            }
            
            case "COUNT" -> new IntValue(values.size());
            
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }
    
    // Pattern-based data transformation
    public DataValue transform(DataValue value, String transformation) {
        return switch (transformation.toUpperCase()) {
            case "ABSOLUTE" -> switch (value) {
                case IntValue(var i) -> new IntValue(Math.abs(i));
                case LongValue(var l) -> new LongValue(Math.abs(l));
                case DoubleValue(var d) -> new DoubleValue(Math.abs(d));
                default -> value;
            };
            
            case "SQUARE" -> switch (value) {
                case IntValue(var i) -> new LongValue((long) i * i);
                case LongValue(var l) -> new LongValue(l * l);
                case DoubleValue(var d) -> new DoubleValue(d * d);
                default -> new DoubleValue(Math.pow(toDouble(value), 2));
            };
            
            case "SQRT" -> new DoubleValue(Math.sqrt(toDouble(value)));
            
            case "UPPERCASE" -> switch (value) {
                case StringValue(var s) -> new StringValue(s.toUpperCase());
                default -> new StringValue(String.valueOf(toDouble(value)).toUpperCase());
            };
            
            case "LOWERCASE" -> switch (value) {
                case StringValue(var s) -> new StringValue(s.toLowerCase());
                default -> new StringValue(String.valueOf(toDouble(value)).toLowerCase());
            };
            
            default -> value;
        };
    }
}
```

### Pattern Matching Performance Optimization

**Benchmarking and Optimization:**
```java
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.stream.IntStream;

public class PatternMatchingBenchmark {
    
    public static void main(String[] args) {
        PatternMatchingBenchmark benchmark = new PatternMatchingBenchmark();
        
        System.out.println("=== Pattern Matching Performance Benchmarks ===\n");
        
        benchmark.benchmarkSwitchExpressions();
        benchmark.benchmarkPatternMatching();
        benchmark.benchmarkGuardPerformance();
    }
    
    public void benchmarkSwitchExpressions() {
        System.out.println("1. Switch Expression Performance");
        System.out.println("================================");
        
        Random random = new Random(42);
        int iterations = 1_000_000;
        
        // Generate test data
        Object[] testData = IntStream.range(0, iterations)
            .mapToObj(i -> switch (random.nextInt(5)) {
                case 0 -> random.nextInt(1000);
                case 1 -> random.nextDouble() * 1000;
                case 2 -> "String_" + random.nextInt(100);
                case 3 -> random.nextBoolean();
                default -> random.nextLong();
            })
            .toArray();
        
        // Traditional instanceof approach
        long traditionalTime = measureTime(() -> {
            int count = 0;
            for (Object obj : testData) {
                if (obj instanceof Integer) {
                    count++;
                } else if (obj instanceof Double) {
                    count++;
                } else if (obj instanceof String) {
                    count++;
                } else if (obj instanceof Boolean) {
                    count++;
                } else if (obj instanceof Long) {
                    count++;
                }
            }
            return count;
        });
        
        // Pattern matching switch
        long patternTime = measureTime(() -> {
            int count = 0;
            for (Object obj : testData) {
                count += switch (obj) {
                    case Integer i -> 1;
                    case Double d -> 1;
                    case String s -> 1;
                    case Boolean b -> 1;
                    case Long l -> 1;
                    default -> 0;
                };
            }
            return count;
        });
        
        System.out.printf("Traditional instanceof: %d ms%n", traditionalTime);
        System.out.printf("Pattern matching switch: %d ms%n", patternTime);
        System.out.printf("Improvement: %.2fx%n", (double) traditionalTime / patternTime);
        System.out.println();
    }
    
    public void benchmarkPatternMatching() {
        System.out.println("2. Record Pattern Performance");
        System.out.println("=============================");
        
        Random random = new Random(42);
        int iterations = 500_000;
        
        // Generate geometric shapes
        Shape[] shapes = IntStream.range(0, iterations)
            .mapToObj(i -> switch (random.nextInt(4)) {
                case 0 -> new Point(random.nextDouble() * 100, random.nextDouble() * 100);
                case 1 -> new Line(
                    new Point(random.nextDouble() * 100, random.nextDouble() * 100),
                    new Point(random.nextDouble() * 100, random.nextDouble() * 100)
                );
                case 2 -> new Circle(
                    new Point(random.nextDouble() * 100, random.nextDouble() * 100),
                    random.nextDouble() * 50
                );
                default -> new Polygon(
                    IntStream.range(0, 3 + random.nextInt(5))
                        .mapToObj(j -> new Point(random.nextDouble() * 100, random.nextDouble() * 100))
                        .toList()
                );
            })
            .toArray(Shape[]::new);
        
        GeometryProcessor processor = new GeometryProcessor();
        
        // Measure pattern matching performance
        long patternTime = measureTime(() -> {
            double totalArea = 0.0;
            for (Shape shape : shapes) {
                totalArea += processor.calculateArea(shape);
            }
            return totalArea;
        });
        
        System.out.printf("Pattern matching calculation: %d ms%n", patternTime);
        System.out.printf("Shapes processed per second: %.0f%n", 
                         shapes.length * 1000.0 / patternTime);
        System.out.println();
    }
    
    public void benchmarkGuardPerformance() {
        System.out.println("3. Guard Pattern Performance");
        System.out.println("============================");
        
        Random random = new Random(42);
        int iterations = 200_000;
        
        // Generate transactions
        Transaction[] transactions = IntStream.range(0, iterations)
            .mapToObj(i -> {
                double amount = random.nextDouble() * 20000;
                return switch (random.nextInt(5)) {
                    case 0 -> new Purchase("Merchant_" + random.nextInt(100), amount, 
                                         random.nextBoolean() ? "REGULAR" : "GAMBLING");
                    case 1 -> new Transfer("ACC_" + random.nextInt(100), "ACC_" + random.nextInt(100), amount);
                    case 2 -> new Withdrawal("ACC_" + random.nextInt(100), amount, 
                                           random.nextBoolean() ? "OWN_BANK" : "ATM_External");
                    case 3 -> new Deposit("ACC_" + random.nextInt(100), amount, 
                                        random.nextBoolean() ? "WIRE" : "CASH");
                    default -> new Refund("Merchant_" + random.nextInt(100), amount, "Defective");
                };
            })
            .toArray(Transaction[]::new);
        
        TransactionProcessor processor = new TransactionProcessor();
        
        // Measure guard pattern performance
        long guardTime = measureTime(() -> {
            int approvalCount = 0;
            for (Transaction transaction : transactions) {
                if (processor.requiresApproval(transaction)) {
                    approvalCount++;
                }
            }
            return approvalCount;
        });
        
        System.out.printf("Guard pattern evaluation: %d ms%n", guardTime);
        System.out.printf("Transactions processed per second: %.0f%n", 
                         transactions.length * 1000.0 / guardTime);
        System.out.println();
    }
    
    private long measureTime(java.util.function.Supplier<?> operation) {
        Instant start = Instant.now();
        operation.get();
        Instant end = Instant.now();
        return Duration.between(start, end).toMillis();
    }
}
```

## Advanced Data Processing Pipelines

### Stream Processing with Pattern Matching

**Functional Data Pipeline:**
```java
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

// Event-driven data processing system
public sealed interface Event 
    permits UserEvent, SystemEvent, TransactionEvent, ErrorEvent {
    
    long timestamp();
    String source();
}

record UserEvent(long timestamp, String source, String userId, String action, 
                Map<String, Object> properties) implements Event {}

record SystemEvent(long timestamp, String source, String component, String level, 
                  String message) implements Event {}

record TransactionEvent(long timestamp, String source, String transactionId, 
                       String type, double amount, String status) implements Event {}

record ErrorEvent(long timestamp, String source, String errorCode, String message, 
                 String stackTrace) implements Event {}

public class EventProcessor {
    
    public void processEventStream(Stream<Event> events) {
        var results = events
            .parallel()
            .collect(Collectors.groupingBy(this::categorizeEvent))
            .entrySet()
            .parallelStream()
            .map(entry -> CompletableFuture.supplyAsync(() -> 
                processEventCategory(entry.getKey(), entry.getValue())))
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        results.forEach(System.out::println);
    }
    
    private String categorizeEvent(Event event) {
        return switch (event) {
            case UserEvent(var ts, var src, var userId, var action, var props) 
                when action.equals("LOGIN") -> "AUTH";
            
            case UserEvent(var ts, var src, var userId, var action, var props) 
                when action.startsWith("PURCHASE") -> "COMMERCE";
            
            case UserEvent(var ts, var src, var userId, var action, var props) -> "USER_ACTIVITY";
            
            case SystemEvent(var ts, var src, var comp, var level, var msg) 
                when level.equals("ERROR") -> "SYSTEM_ERROR";
            
            case SystemEvent(var ts, var src, var comp, var level, var msg) 
                when level.equals("WARN") -> "SYSTEM_WARNING";
            
            case SystemEvent(var ts, var src, var comp, var level, var msg) -> "SYSTEM_INFO";
            
            case TransactionEvent(var ts, var src, var txId, var type, var amount, var status) 
                when amount > 10000 -> "HIGH_VALUE_TRANSACTION";
            
            case TransactionEvent(var ts, var src, var txId, var type, var amount, var status) 
                when status.equals("FAILED") -> "FAILED_TRANSACTION";
            
            case TransactionEvent(var ts, var src, var txId, var type, var amount, var status) -> 
                "REGULAR_TRANSACTION";
            
            case ErrorEvent(var ts, var src, var code, var msg, var stack) -> "APPLICATION_ERROR";
        };
    }
    
    private String processEventCategory(String category, List<Event> events) {
        return switch (category) {
            case "AUTH" -> processAuthEvents(events);
            case "COMMERCE" -> processCommerceEvents(events);
            case "SYSTEM_ERROR" -> processSystemErrors(events);
            case "HIGH_VALUE_TRANSACTION" -> processHighValueTransactions(events);
            case "APPLICATION_ERROR" -> processApplicationErrors(events);
            default -> processGenericEvents(category, events);
        };
    }
    
    private String processAuthEvents(List<Event> events) {
        Map<String, Long> loginCounts = events.stream()
            .filter(UserEvent.class::isInstance)
            .map(UserEvent.class::cast)
            .collect(Collectors.groupingBy(UserEvent::userId, Collectors.counting()));
        
        return "Auth Events: " + events.size() + " events, " + 
               loginCounts.size() + " unique users";
    }
    
    private String processCommerceEvents(List<Event> events) {
        double totalValue = events.stream()
            .filter(UserEvent.class::isInstance)
            .map(UserEvent.class::cast)
            .mapToDouble(event -> (Double) event.properties().getOrDefault("amount", 0.0))
            .sum();
        
        return "Commerce Events: " + events.size() + " events, $" + 
               String.format("%.2f", totalValue) + " total value";
    }
    
    private String processSystemErrors(List<Event> events) {
        Map<String, Long> errorsByComponent = events.stream()
            .filter(SystemEvent.class::isInstance)
            .map(SystemEvent.class::cast)
            .collect(Collectors.groupingBy(SystemEvent::component, Collectors.counting()));
        
        return "System Errors: " + events.size() + " errors across " + 
               errorsByComponent.size() + " components";
    }
    
    private String processHighValueTransactions(List<Event> events) {
        double totalAmount = events.stream()
            .filter(TransactionEvent.class::isInstance)
            .map(TransactionEvent.class::cast)
            .mapToDouble(TransactionEvent::amount)
            .sum();
        
        return "High Value Transactions: " + events.size() + " transactions, $" + 
               String.format("%.2f", totalAmount) + " total";
    }
    
    private String processApplicationErrors(List<Event> events) {
        Map<String, Long> errorsByCode = events.stream()
            .filter(ErrorEvent.class::isInstance)
            .map(ErrorEvent.class::cast)
            .collect(Collectors.groupingBy(ErrorEvent::errorCode, Collectors.counting()));
        
        return "Application Errors: " + events.size() + " errors, " + 
               errorsByCode.size() + " unique error codes";
    }
    
    private String processGenericEvents(String category, List<Event> events) {
        return category + ": " + events.size() + " events processed";
    }
    
    // Real-time pattern matching for event filtering
    public Predicate<Event> createEventFilter(String filterExpression) {
        return event -> switch (filterExpression.toUpperCase()) {
            case "HIGH_VALUE" -> switch (event) {
                case TransactionEvent(var ts, var src, var txId, var type, var amount, var status) 
                    when amount > 10000 -> true;
                case UserEvent(var ts, var src, var userId, var action, var props) 
                    when props.containsKey("amount") && 
                         (Double) props.get("amount") > 10000 -> true;
                default -> false;
            };
            
            case "ERRORS_ONLY" -> switch (event) {
                case SystemEvent(var ts, var src, var comp, var level, var msg) 
                    when level.equals("ERROR") -> true;
                case ErrorEvent(var ts, var src, var code, var msg, var stack) -> true;
                case TransactionEvent(var ts, var src, var txId, var type, var amount, var status) 
                    when status.equals("FAILED") -> true;
                default -> false;
            };
            
            case "USER_ACTIVITY" -> event instanceof UserEvent;
            
            case "SYSTEM_ACTIVITY" -> event instanceof SystemEvent;
            
            default -> true; // No filter
        };
    }
}
```

## Practical Exercises

### Exercise 1: JSON Processing with Pattern Matching

**Objective:** Build a high-performance JSON processor using pattern matching.

```java
// JSON value representation using sealed classes
public sealed interface JsonValue 
    permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {
}

record JsonObject(Map<String, JsonValue> fields) implements JsonValue {}
record JsonArray(List<JsonValue> elements) implements JsonValue {}
record JsonString(String value) implements JsonValue {}
record JsonNumber(double value) implements JsonValue {}
record JsonBoolean(boolean value) implements JsonValue {}
record JsonNull() implements JsonValue {}

public class JsonProcessor {
    
    public String formatJson(JsonValue json, int indentLevel) {
        String indent = "  ".repeat(indentLevel);
        String nextIndent = "  ".repeat(indentLevel + 1);
        
        return switch (json) {
            case JsonNull() -> "null";
            case JsonBoolean(var value) -> String.valueOf(value);
            case JsonNumber(var value) -> 
                value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
            case JsonString(var value) -> "\"" + escapeString(value) + "\"";
            
            case JsonArray(var elements) when elements.isEmpty() -> "[]";
            case JsonArray(var elements) -> {
                StringBuilder sb = new StringBuilder("[\n");
                for (int i = 0; i < elements.size(); i++) {
                    sb.append(nextIndent).append(formatJson(elements.get(i), indentLevel + 1));
                    if (i < elements.size() - 1) sb.append(",");
                    sb.append("\n");
                }
                sb.append(indent).append("]");
                yield sb.toString();
            }
            
            case JsonObject(var fields) when fields.isEmpty() -> "{}";
            case JsonObject(var fields) -> {
                StringBuilder sb = new StringBuilder("{\n");
                var entries = fields.entrySet().toArray(Map.Entry[]::new);
                for (int i = 0; i < entries.length; i++) {
                    var entry = entries[i];
                    sb.append(nextIndent)
                      .append("\"").append(escapeString((String) entry.getKey())).append("\": ")
                      .append(formatJson((JsonValue) entry.getValue(), indentLevel + 1));
                    if (i < entries.length - 1) sb.append(",");
                    sb.append("\n");
                }
                sb.append(indent).append("}");
                yield sb.toString();
            }
        };
    }
    
    public JsonValue queryPath(JsonValue json, String path) {
        String[] parts = path.split("\\.");
        JsonValue current = json;
        
        for (String part : parts) {
            current = switch (current) {
                case JsonObject(var fields) -> fields.get(part);
                case JsonArray(var elements) -> {
                    try {
                        int index = Integer.parseInt(part);
                        yield index >= 0 && index < elements.size() ? elements.get(index) : null;
                    } catch (NumberFormatException e) {
                        yield null;
                    }
                }
                default -> null;
            };
            
            if (current == null) break;
        }
        
        return current != null ? current : new JsonNull();
    }
    
    public JsonValue transform(JsonValue json, String transformation) {
        return switch (transformation.toUpperCase()) {
            case "UPPERCASE_STRINGS" -> switch (json) {
                case JsonString(var value) -> new JsonString(value.toUpperCase());
                case JsonObject(var fields) -> new JsonObject(
                    fields.entrySet().stream()
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> transform(entry.getValue(), transformation)
                        ))
                );
                case JsonArray(var elements) -> new JsonArray(
                    elements.stream()
                        .map(element -> transform(element, transformation))
                        .toList()
                );
                default -> json;
            };
            
            case "DOUBLE_NUMBERS" -> switch (json) {
                case JsonNumber(var value) -> new JsonNumber(value * 2);
                case JsonObject(var fields) -> new JsonObject(
                    fields.entrySet().stream()
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> transform(entry.getValue(), transformation)
                        ))
                );
                case JsonArray(var elements) -> new JsonArray(
                    elements.stream()
                        .map(element -> transform(element, transformation))
                        .toList()
                );
                default -> json;
            };
            
            default -> json;
        };
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
```

### Exercise 2: State Machine with Pattern Matching

**Objective:** Implement a sophisticated state machine using pattern matching.

```java
// Order processing state machine
public sealed interface OrderState 
    permits Pending, Confirmed, Shipped, Delivered, Cancelled, Returned {
}

record Pending(String orderId, long timestamp) implements OrderState {}
record Confirmed(String orderId, long timestamp, String paymentId) implements OrderState {}
record Shipped(String orderId, long timestamp, String trackingNumber) implements OrderState {}
record Delivered(String orderId, long timestamp, String signature) implements OrderState {}
record Cancelled(String orderId, long timestamp, String reason) implements OrderState {}
record Returned(String orderId, long timestamp, String returnReason) implements OrderState {}

// Events that trigger state transitions
public sealed interface OrderEvent 
    permits ConfirmPayment, ShipOrder, DeliverOrder, CancelOrder, ReturnOrder {
}

record ConfirmPayment(String paymentId) implements OrderEvent {}
record ShipOrder(String trackingNumber) implements OrderEvent {}
record DeliverOrder(String signature) implements OrderEvent {}
record CancelOrder(String reason) implements OrderEvent {}
record ReturnOrder(String returnReason) implements OrderEvent {}

public class OrderStateMachine {
    
    public OrderState processEvent(OrderState currentState, OrderEvent event) {
        return switch (currentState) {
            case Pending(var orderId, var timestamp) -> switch (event) {
                case ConfirmPayment(var paymentId) -> 
                    new Confirmed(orderId, System.currentTimeMillis(), paymentId);
                case CancelOrder(var reason) -> 
                    new Cancelled(orderId, System.currentTimeMillis(), reason);
                default -> throw new IllegalStateException(
                    "Invalid transition from Pending with " + event.getClass().getSimpleName());
            };
            
            case Confirmed(var orderId, var timestamp, var paymentId) -> switch (event) {
                case ShipOrder(var trackingNumber) -> 
                    new Shipped(orderId, System.currentTimeMillis(), trackingNumber);
                case CancelOrder(var reason) -> 
                    new Cancelled(orderId, System.currentTimeMillis(), reason);
                default -> throw new IllegalStateException(
                    "Invalid transition from Confirmed with " + event.getClass().getSimpleName());
            };
            
            case Shipped(var orderId, var timestamp, var trackingNumber) -> switch (event) {
                case DeliverOrder(var signature) -> 
                    new Delivered(orderId, System.currentTimeMillis(), signature);
                default -> throw new IllegalStateException(
                    "Invalid transition from Shipped with " + event.getClass().getSimpleName());
            };
            
            case Delivered(var orderId, var timestamp, var signature) -> switch (event) {
                case ReturnOrder(var returnReason) -> 
                    new Returned(orderId, System.currentTimeMillis(), returnReason);
                default -> throw new IllegalStateException(
                    "Invalid transition from Delivered with " + event.getClass().getSimpleName());
            };
            
            case Cancelled(var orderId, var timestamp, var reason) -> 
                throw new IllegalStateException("Cannot process events for cancelled order");
            
            case Returned(var orderId, var timestamp, var returnReason) -> 
                throw new IllegalStateException("Cannot process events for returned order");
        };
    }
    
    public List<OrderEvent> getValidEvents(OrderState state) {
        return switch (state) {
            case Pending(var orderId, var timestamp) -> List.of(
                new ConfirmPayment("dummy"), 
                new CancelOrder("dummy")
            );
            
            case Confirmed(var orderId, var timestamp, var paymentId) -> List.of(
                new ShipOrder("dummy"), 
                new CancelOrder("dummy")
            );
            
            case Shipped(var orderId, var timestamp, var trackingNumber) -> List.of(
                new DeliverOrder("dummy")
            );
            
            case Delivered(var orderId, var timestamp, var signature) -> List.of(
                new ReturnOrder("dummy")
            );
            
            case Cancelled(var orderId, var timestamp, var reason) -> List.of();
            
            case Returned(var orderId, var timestamp, var returnReason) -> List.of();
        };
    }
    
    public String getStateDescription(OrderState state) {
        return switch (state) {
            case Pending(var orderId, var timestamp) -> 
                "Order " + orderId + " is pending payment confirmation";
            
            case Confirmed(var orderId, var timestamp, var paymentId) -> 
                "Order " + orderId + " confirmed with payment " + paymentId;
            
            case Shipped(var orderId, var timestamp, var trackingNumber) -> 
                "Order " + orderId + " shipped with tracking " + trackingNumber;
            
            case Delivered(var orderId, var timestamp, var signature) -> 
                "Order " + orderId + " delivered and signed by " + signature;
            
            case Cancelled(var orderId, var timestamp, var reason) -> 
                "Order " + orderId + " cancelled: " + reason;
            
            case Returned(var orderId, var timestamp, var returnReason) -> 
                "Order " + orderId + " returned: " + returnReason;
        };
    }
    
    public boolean isTerminalState(OrderState state) {
        return switch (state) {
            case Cancelled(var orderId, var timestamp, var reason) -> true;
            case Returned(var orderId, var timestamp, var returnReason) -> true;
            default -> false;
        };
    }
}
```

## Assessment and Testing

### Comprehensive Testing Suite

**Pattern Matching Testing Framework:**
```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class PatternMatchingTest {
    
    @Test
    public void testExpressionEvaluation() {
        Map<String, Double> variables = Map.of("x", 5.0, "y", 3.0);
        
        Expression expr = new Addition(
            new Variable("x"),
            new Multiplication(new Variable("y"), new Literal(2.0))
        );
        
        assertEquals(11.0, expr.evaluate(variables), 0.001);
        
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        assertEquals("(x + (y * 2.0))", evaluator.formatExpression(expr));
        
        Expression simplified = evaluator.simplify(
            new Addition(new Literal(0.0), new Variable("x"))
        );
        assertTrue(simplified instanceof Variable);
        assertEquals("x", ((Variable) simplified).name());
    }
    
    @Test
    public void testGeometryProcessing() {
        GeometryProcessor processor = new GeometryProcessor();
        
        // Test area calculation
        Circle circle = new Circle(new Point(0, 0), 5.0);
        double area = processor.calculateArea(circle);
        assertEquals(Math.PI * 25, area, 0.001);
        
        // Test translation
        Point translatedPoint = (Point) processor.translate(new Point(1, 2), 3, 4);
        assertEquals(4, translatedPoint.x());
        assertEquals(6, translatedPoint.y());
        
        // Test description
        String description = processor.describe(circle);
        assertTrue(description.contains("Circle"));
        assertTrue(description.contains("radius 5.0"));
    }
    
    @Test
    public void testTransactionProcessing() {
        TransactionProcessor processor = new TransactionProcessor();
        
        // Test large transaction
        Purchase largePurchase = new Purchase("BigStore", 15000.0, "ELECTRONICS");
        assertTrue(processor.requiresApproval(largePurchase));
        
        String category = processor.categorizeTransaction(largePurchase);
        assertTrue(category.startsWith("LARGE_PURCHASE"));
        
        // Test gambling transaction
        Purchase gambling = new Purchase("Casino", 500.0, "GAMBLING");
        assertTrue(processor.requiresApproval(gambling));
        
        // Test fee calculation
        Transfer internationalTransfer = new Transfer("INT123", "INT456", 1000.0);
        assertEquals(30.0, processor.calculateFee(internationalTransfer), 0.01);
    }
    
    @Test
    public void testDataProcessing() {
        DataProcessor processor = new DataProcessor();
        
        // Test type conversion
        assertEquals(42.0, processor.toDouble(new DataProcessor.IntValue(42)), 0.001);
        assertEquals(3.14, processor.toDouble(new DataProcessor.DoubleValue(3.14)), 0.001);
        assertEquals(1.0, processor.toDouble(new DataProcessor.BooleanValue(true)), 0.001);
        
        // Test comparison
        assertTrue(processor.compare(
            new DataProcessor.IntValue(5), 
            new DataProcessor.IntValue(3)
        ) > 0);
        
        // Test aggregation
        List<DataProcessor.DataValue> values = List.of(
            new DataProcessor.IntValue(1),
            new DataProcessor.IntValue(2),
            new DataProcessor.IntValue(3)
        );
        
        DataProcessor.DataValue sum = processor.aggregate(values, "SUM");
        assertEquals(6.0, processor.toDouble(sum), 0.001);
    }
    
    @Test
    public void testStateMachine() {
        OrderStateMachine stateMachine = new OrderStateMachine();
        
        // Test state transitions
        OrderState pending = new Pending("ORDER123", System.currentTimeMillis());
        OrderState confirmed = stateMachine.processEvent(pending, new ConfirmPayment("PAY456"));
        
        assertTrue(confirmed instanceof Confirmed);
        assertEquals("ORDER123", ((Confirmed) confirmed).orderId());
        
        // Test invalid transition
        assertThrows(IllegalStateException.class, () -> 
            stateMachine.processEvent(pending, new ShipOrder("TRACK789"))
        );
        
        // Test terminal states
        OrderState cancelled = new Cancelled("ORDER123", System.currentTimeMillis(), "Customer request");
        assertTrue(stateMachine.isTerminalState(cancelled));
        assertFalse(stateMachine.isTerminalState(pending));
    }
    
    @Test
    public void testJsonProcessing() {
        JsonProcessor processor = new JsonProcessor();
        
        // Create test JSON
        JsonValue json = new JsonObject(Map.of(
            "name", new JsonString("John"),
            "age", new JsonNumber(30),
            "active", new JsonBoolean(true),
            "scores", new JsonArray(List.of(
                new JsonNumber(85),
                new JsonNumber(92),
                new JsonNumber(78)
            ))
        ));
        
        // Test formatting
        String formatted = processor.formatJson(json, 0);
        assertTrue(formatted.contains("\"name\": \"John\""));
        assertTrue(formatted.contains("\"age\": 30"));
        
        // Test querying
        JsonValue name = processor.queryPath(json, "name");
        assertTrue(name instanceof JsonString);
        assertEquals("John", ((JsonString) name).value());
        
        JsonValue firstScore = processor.queryPath(json, "scores.0");
        assertTrue(firstScore instanceof JsonNumber);
        assertEquals(85.0, ((JsonNumber) firstScore).value(), 0.001);
        
        // Test transformation
        JsonValue transformed = processor.transform(json, "UPPERCASE_STRINGS");
        JsonValue transformedName = processor.queryPath(transformed, "name");
        assertEquals("JOHN", ((JsonString) transformedName).value());
    }
    
    @Test
    public void testPatternMatchingPerformance() {
        // Performance test with large dataset
        Random random = new Random(42);
        int size = 100_000;
        
        Object[] testData = IntStream.range(0, size)
            .mapToObj(i -> switch (random.nextInt(4)) {
                case 0 -> random.nextInt(1000);
                case 1 -> random.nextDouble() * 1000;
                case 2 -> "String_" + random.nextInt(100);
                default -> random.nextBoolean();
            })
            .toArray();
        
        long startTime = System.nanoTime();
        
        int count = 0;
        for (Object obj : testData) {
            count += switch (obj) {
                case Integer i -> 1;
                case Double d -> 1;
                case String s -> 1;
                case Boolean b -> 1;
                default -> 0;
            };
        }
        
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        
        assertEquals(size, count);
        assertTrue(duration < 100_000_000, "Pattern matching should be fast"); // Less than 100ms
        
        System.out.printf("Pattern matching performance: %.2f ms for %d items%n", 
                         duration / 1_000_000.0, size);
    }
}
```

## Best Practices and Production Deployment

### Pattern Matching Best Practices

**Design Guidelines:**
```java
public class PatternMatchingBestPractices {
    
    public static void demonstrateBestPractices() {
        System.out.println("=== Pattern Matching Best Practices ===");
        System.out.println();
        
        System.out.println("1. Exhaustiveness:");
        System.out.println("   - Use sealed classes to ensure exhaustive pattern matching");
        System.out.println("   - Compiler will warn about missing cases");
        System.out.println("   - Avoid default cases when possible");
        System.out.println();
        
        System.out.println("2. Performance:");
        System.out.println("   - Pattern matching is generally faster than instanceof chains");
        System.out.println("   - Guard patterns add runtime overhead - use judiciously");
        System.out.println("   - Consider compilation to efficient bytecode");
        System.out.println();
        
        System.out.println("3. Readability:");
        System.out.println("   - Use descriptive variable names in patterns");
        System.out.println("   - Keep guard conditions simple and clear");
        System.out.println("   - Prefer pattern matching over complex conditional logic");
        System.out.println();
        
        System.out.println("4. Maintainability:");
        System.out.println("   - Group related patterns together");
        System.out.println("   - Use nested patterns judiciously to avoid complexity");
        System.out.println("   - Document complex pattern matching logic");
    }
    
    // Example of well-structured pattern matching
    public String processResult(Result<String, String> result) {
        return switch (result) {
            case Success(var value) when value.length() > 100 -> 
                "Large success: " + value.substring(0, 100) + "...";
            
            case Success(var value) -> 
                "Success: " + value;
            
            case Error(var message) when message.startsWith("FATAL") -> 
                "Critical error: " + message;
            
            case Error(var message) -> 
                "Error: " + message;
        };
    }
    
    // Result type for demonstration
    sealed interface Result<T, E> permits Success, Error {}
    record Success<T, E>(T value) implements Result<T, E> {}
    record Error<T, E>(E error) implements Result<T, E> {}
}
```

This comprehensive lesson on advanced pattern matching and switch expressions provides students with the knowledge and skills to leverage modern Java's most powerful language features for building expressive, type-safe, and high-performance applications.