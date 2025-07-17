# Day 36: Lambda Expressions & Functional Interfaces (August 14, 2025)

**Date:** August 14, 2025  
**Duration:** 3 hours  
**Focus:** Master functional programming fundamentals with lambda expressions and functional interfaces

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Understand functional programming concepts and benefits
- ✅ Master lambda expression syntax and usage patterns
- ✅ Learn built-in functional interfaces (Predicate, Function, Consumer, Supplier)
- ✅ Implement method references and constructor references
- ✅ Understand variable scope and closure behavior in lambdas
- ✅ Build practical applications using functional programming principles

---

## 🚀 Part 1: Lambda Expressions Fundamentals (60 minutes)

### **Understanding Functional Programming**

**Functional Programming** treats computation as evaluation of mathematical functions:

- **First-class functions**: Functions can be assigned to variables, passed as arguments
- **Pure functions**: No side effects, same input always produces same output
- **Immutability**: Data structures don't change after creation
- **Higher-order functions**: Functions that take or return other functions

### **Lambda Expression Syntax**

Create `LambdaBasics.java`:

```java
import java.util.*;
import java.util.function.*;

/**
 * Comprehensive demonstration of lambda expression fundamentals
 */
public class LambdaBasics {
    
    public static void main(String[] args) {
        System.out.println("=== Lambda Expression Fundamentals ===\n");
        
        // 1. Traditional vs Lambda approach
        System.out.println("1. Traditional vs Lambda Approach:");
        demonstrateTraditionalVsLambda();
        
        // 2. Lambda syntax variations
        System.out.println("\n2. Lambda Syntax Variations:");
        demonstrateLambdaSyntax();
        
        // 3. Type inference and explicit types
        System.out.println("\n3. Type Inference:");
        demonstrateTypeInference();
        
        // 4. Variable capture and scope
        System.out.println("\n4. Variable Capture and Scope:");
        demonstrateVariableCapture();
        
        System.out.println("\n=== Lambda Fundamentals Completed ===");
    }
    
    private static void demonstrateTraditionalVsLambda() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Diana", "Eve");
        
        System.out.println("Original list: " + names);
        
        // Traditional approach with anonymous inner class
        System.out.println("\nTraditional approach (Anonymous Inner Class):");
        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.length() - b.length();
            }
        });
        System.out.println("Sorted by length: " + names);
        
        // Reset list
        names = Arrays.asList("Alice", "Bob", "Charlie", "Diana", "Eve");
        
        // Lambda approach
        System.out.println("\nLambda approach:");
        Collections.sort(names, (a, b) -> a.length() - b.length());
        System.out.println("Sorted by length: " + names);
        
        // Even more concise with method reference
        names = Arrays.asList("Alice", "Bob", "Charlie", "Diana", "Eve");
        Collections.sort(names, Comparator.comparing(String::length));
        System.out.println("Method reference approach: " + names);
    }
    
    private static void demonstrateLambdaSyntax() {
        // 1. No parameters
        Runnable noParams = () -> System.out.println("No parameters lambda");
        noParams.run();
        
        // 2. Single parameter (parentheses optional)
        Consumer<String> singleParam1 = s -> System.out.println("Single param: " + s);
        Consumer<String> singleParam2 = (s) -> System.out.println("Single param with parens: " + s);
        singleParam1.accept("Hello");
        singleParam2.accept("World");
        
        // 3. Multiple parameters
        BinaryOperator<Integer> multiParams = (a, b) -> a + b;
        System.out.println("Multiple params (5 + 3): " + multiParams.apply(5, 3));
        
        // 4. Block body vs expression body
        Function<String, String> expressionBody = s -> s.toUpperCase();
        Function<String, String> blockBody = s -> {
            String result = s.toUpperCase();
            System.out.println("Processing: " + s + " -> " + result);
            return result;
        };
        
        System.out.println("Expression body: " + expressionBody.apply("hello"));
        System.out.println("Block body: " + blockBody.apply("world"));
        
        // 5. Explicit types
        BiFunction<Integer, Integer, Integer> explicitTypes = 
            (Integer x, Integer y) -> x * y;
        System.out.println("Explicit types (4 * 6): " + explicitTypes.apply(4, 6));
    }
    
    private static void demonstrateTypeInference() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        
        // Type inference - compiler knows the types
        numbers.forEach(n -> System.out.print(n + " "));
        System.out.println();
        
        // Explicit types (usually unnecessary)
        numbers.forEach((Integer n) -> System.out.print(n * 2 + " "));
        System.out.println();
        
        // Complex type inference
        Map<String, List<Integer>> data = new HashMap<>();
        data.put("evens", Arrays.asList(2, 4, 6));
        data.put("odds", Arrays.asList(1, 3, 5));
        
        // Compiler infers types for entry, key, and value
        data.forEach((key, value) -> 
            System.out.println(key + ": " + value.size() + " elements"));
    }
    
    private static void demonstrateVariableCapture() {
        String prefix = "Result: ";  // Effectively final
        int multiplier = 10;         // Effectively final
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        
        // Capturing effectively final variables
        numbers.forEach(n -> System.out.println(prefix + (n * multiplier)));
        
        // This would cause compilation error:
        // multiplier = 20;  // Cannot modify captured variable
        
        // Instance variables and static variables can be modified
        Counter counter = new Counter();
        numbers.forEach(n -> counter.increment());
        System.out.println("Counter value: " + counter.getValue());
        
        // Mutable objects can be modified
        List<String> results = new ArrayList<>();
        numbers.forEach(n -> results.add("Item " + n));
        System.out.println("Results: " + results);
    }
    
    // Helper class for demonstrating variable capture
    static class Counter {
        private int value = 0;
        
        public void increment() {
            value++;
        }
        
        public int getValue() {
            return value;
        }
    }
}
```

---

## 🔧 Part 2: Built-in Functional Interfaces (60 minutes)

### **Core Functional Interface Types**

Java provides several built-in functional interfaces in `java.util.function` package:

- **Predicate<T>**: Takes T, returns boolean
- **Function<T,R>**: Takes T, returns R  
- **Consumer<T>**: Takes T, returns void
- **Supplier<T>**: Takes nothing, returns T

Create `FunctionalInterfacesDemo.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Comprehensive demonstration of built-in functional interfaces
 */
public class FunctionalInterfacesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Built-in Functional Interfaces ===\n");
        
        // 1. Predicate interface
        System.out.println("1. Predicate Interface:");
        demonstratePredicates();
        
        // 2. Function interface
        System.out.println("\n2. Function Interface:");
        demonstrateFunctions();
        
        // 3. Consumer interface
        System.out.println("\n3. Consumer Interface:");
        demonstrateConsumers();
        
        // 4. Supplier interface
        System.out.println("\n4. Supplier Interface:");
        demonstrateSuppliers();
        
        // 5. Specialized functional interfaces
        System.out.println("\n5. Specialized Functional Interfaces:");
        demonstrateSpecializedInterfaces();
        
        // 6. Real-world application
        System.out.println("\n6. Real-World Application:");
        demonstrateDataProcessingPipeline();
        
        System.out.println("\n=== Functional Interfaces Demo Completed ===");
    }
    
    private static void demonstratePredicates() {
        List<String> words = Arrays.asList("hello", "world", "java", "lambda", "stream", "function");
        
        // Basic predicates
        Predicate<String> isLongWord = s -> s.length() > 5;
        Predicate<String> startsWithJ = s -> s.startsWith("j");
        Predicate<String> containsA = s -> s.contains("a");
        
        System.out.println("Original words: " + words);
        
        System.out.println("Long words (>5 chars): " + 
            words.stream().filter(isLongWord).collect(Collectors.toList()));
        
        System.out.println("Words starting with 'j': " + 
            words.stream().filter(startsWithJ).collect(Collectors.toList()));
        
        // Predicate composition
        Predicate<String> longWordsWithA = isLongWord.and(containsA);
        Predicate<String> shortOrStartsWithJ = isLongWord.negate().or(startsWithJ);
        
        System.out.println("Long words containing 'a': " + 
            words.stream().filter(longWordsWithA).collect(Collectors.toList()));
        
        System.out.println("Short words OR starting with 'j': " + 
            words.stream().filter(shortOrStartsWithJ).collect(Collectors.toList()));
        
        // Using Predicate.isEqual()
        Predicate<String> isJava = Predicate.isEqual("java");
        System.out.println("Contains 'java': " + words.stream().anyMatch(isJava));
    }
    
    private static void demonstrateFunctions() {
        List<String> words = Arrays.asList("hello", "world", "java");
        
        // Basic functions
        Function<String, Integer> stringLength = String::length;
        Function<String, String> toUpperCase = String::toUpperCase;
        Function<Integer, Integer> square = x -> x * x;
        
        System.out.println("Original words: " + words);
        
        System.out.println("Word lengths: " + 
            words.stream().map(stringLength).collect(Collectors.toList()));
        
        System.out.println("Uppercase words: " + 
            words.stream().map(toUpperCase).collect(Collectors.toList()));
        
        // Function composition
        Function<String, Integer> lengthSquared = stringLength.andThen(square);
        Function<String, String> upperCaseFirst = toUpperCase.compose(s -> s);
        
        System.out.println("Length squared: " + 
            words.stream().map(lengthSquared).collect(Collectors.toList()));
        
        // BiFunction for two arguments
        BiFunction<String, String, String> concatenate = (s1, s2) -> s1 + " " + s2;
        System.out.println("Concatenated: " + concatenate.apply("Hello", "World"));
        
        // Function.identity()
        System.out.println("Identity function: " + 
            words.stream().map(Function.identity()).collect(Collectors.toList()));
    }
    
    private static void demonstrateConsumers() {
        List<String> words = Arrays.asList("hello", "world", "java");
        
        // Basic consumers
        Consumer<String> printWord = System.out::println;
        Consumer<String> printLength = s -> System.out.println(s + " has " + s.length() + " characters");
        Consumer<String> printUppercase = s -> System.out.println(s.toUpperCase());
        
        System.out.println("Basic consumer (print each word):");
        words.forEach(printWord);
        
        System.out.println("\nPrint word lengths:");
        words.forEach(printLength);
        
        // Consumer chaining
        Consumer<String> printWordAndLength = printWord.andThen(printLength);
        System.out.println("\nChained consumers:");
        words.forEach(printWordAndLength);
        
        // BiConsumer for two arguments
        BiConsumer<String, Integer> printIndexedWord = 
            (word, index) -> System.out.println(index + ": " + word);
        
        System.out.println("\nIndexed words:");
        for (int i = 0; i < words.size(); i++) {
            printIndexedWord.accept(words.get(i), i);
        }
        
        // Practical example: logging with different levels
        Consumer<String> infoLogger = msg -> System.out.println("[INFO] " + msg);
        Consumer<String> errorLogger = msg -> System.err.println("[ERROR] " + msg);
        Consumer<String> debugLogger = msg -> System.out.println("[DEBUG] " + msg);
        
        System.out.println("\nLogging example:");
        infoLogger.accept("Application started");
        errorLogger.accept("Connection failed");
        debugLogger.accept("Variable value: " + words.size());
    }
    
    private static void demonstrateSuppliers() {
        // Basic suppliers
        Supplier<String> randomGreeting = () -> {
            String[] greetings = {"Hello", "Hi", "Hey", "Greetings"};
            return greetings[new Random().nextInt(greetings.length)];
        };
        
        Supplier<Integer> randomNumber = () -> new Random().nextInt(100);
        Supplier<Date> currentTime = Date::new;
        
        System.out.println("Random greeting: " + randomGreeting.get());
        System.out.println("Random number: " + randomNumber.get());
        System.out.println("Current time: " + currentTime.get());
        
        // Lazy evaluation with suppliers
        System.out.println("\nLazy evaluation example:");
        String result = getExpensiveValue(() -> performExpensiveOperation(), false);
        System.out.println("Result without computation: " + result);
        
        result = getExpensiveValue(() -> performExpensiveOperation(), true);
        System.out.println("Result with computation: " + result);
        
        // Factory pattern with suppliers
        Map<String, Supplier<List<String>>> listFactories = new HashMap<>();
        listFactories.put("arraylist", ArrayList::new);
        listFactories.put("linkedlist", LinkedList::new);
        listFactories.put("vector", Vector::new);
        
        System.out.println("\nFactory pattern with suppliers:");
        List<String> arrayList = listFactories.get("arraylist").get();
        arrayList.add("item1");
        System.out.println("Created ArrayList: " + arrayList.getClass().getSimpleName());
    }
    
    private static String getExpensiveValue(Supplier<String> supplier, boolean needValue) {
        if (needValue) {
            return supplier.get();  // Only compute if needed
        }
        return "Not computed";
    }
    
    private static String performExpensiveOperation() {
        System.out.println("Performing expensive operation...");
        try {
            Thread.sleep(1000);  // Simulate expensive operation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Expensive result";
    }
    
    private static void demonstrateSpecializedInterfaces() {
        // Primitive specializations avoid boxing/unboxing
        IntPredicate isEven = n -> n % 2 == 0;
        IntFunction<String> numberToString = n -> "Number: " + n;
        IntConsumer printSquare = n -> System.out.println(n + "² = " + (n * n));
        IntSupplier randomInt = () -> new Random().nextInt(10);
        
        System.out.println("Primitive specializations:");
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        
        System.out.println("Even numbers:");
        Arrays.stream(numbers).filter(isEven).forEach(System.out::print);
        System.out.println();
        
        System.out.println("Number to string conversion:");
        Arrays.stream(numbers).limit(3)
            .mapToObj(numberToString)
            .forEach(System.out::println);
        
        System.out.println("Print squares:");
        Arrays.stream(numbers).limit(5).forEach(printSquare);
        
        System.out.println("Random int: " + randomInt.getAsInt());
        
        // Binary operators
        BinaryOperator<String> stringConcatenator = (s1, s2) -> s1 + " + " + s2;
        IntBinaryOperator intMultiplier = (a, b) -> a * b;
        
        System.out.println("String concatenation: " + 
            stringConcatenator.apply("Hello", "World"));
        System.out.println("Int multiplication: " + intMultiplier.applyAsInt(6, 7));
        
        // Unary operators
        UnaryOperator<String> addExclamation = s -> s + "!";
        IntUnaryOperator doubleValue = n -> n * 2;
        
        System.out.println("Add exclamation: " + addExclamation.apply("Amazing"));
        System.out.println("Double value: " + doubleValue.applyAsInt(21));
    }
    
    private static void demonstrateDataProcessingPipeline() {
        List<Person> people = Arrays.asList(
            new Person("Alice", 25, "Engineer"),
            new Person("Bob", 30, "Manager"),
            new Person("Charlie", 35, "Engineer"),
            new Person("Diana", 28, "Designer"),
            new Person("Eve", 32, "Manager")
        );
        
        System.out.println("Original data: " + people);
        
        // Functional pipeline for data processing
        Predicate<Person> isAdult = person -> person.getAge() >= 18;
        Predicate<Person> isEngineer = person -> "Engineer".equals(person.getJob());
        Function<Person, String> getNameAndAge = person -> 
            person.getName() + " (" + person.getAge() + ")";
        Consumer<String> printResult = result -> 
            System.out.println("  " + result);
        
        System.out.println("\nAdult Engineers:");
        people.stream()
            .filter(isAdult.and(isEngineer))
            .map(getNameAndAge)
            .forEach(printResult);
        
        // Statistics with functional interfaces
        Function<List<Person>, Double> averageAge = personList ->
            personList.stream().mapToInt(Person::getAge).average().orElse(0.0);
        
        Function<List<Person>, Map<String, Long>> jobDistribution = personList ->
            personList.stream().collect(Collectors.groupingBy(
                Person::getJob, Collectors.counting()));
        
        System.out.println("\nStatistics:");
        System.out.println("Average age: " + averageAge.apply(people));
        System.out.println("Job distribution: " + jobDistribution.apply(people));
    }
    
    // Helper class for demonstration
    static class Person {
        private final String name;
        private final int age;
        private final String job;
        
        public Person(String name, int age, String job) {
            this.name = name;
            this.age = age;
            this.job = job;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getJob() { return job; }
        
        @Override
        public String toString() {
            return String.format("%s(%d, %s)", name, age, job);
        }
    }
}
```

---

## 🎯 Part 3: Practical Applications & Event Handling (60 minutes)

### **Building Functional Applications**

Create `FunctionalCalculator.java`:

```java
import java.util.*;
import java.util.function.*;

/**
 * Advanced calculator using functional programming principles
 */
public class FunctionalCalculator {
    
    // Function registry for operations
    private final Map<String, BinaryOperator<Double>> operations = new HashMap<>();
    private final Map<String, UnaryOperator<Double>> unaryOperations = new HashMap<>();
    private final List<String> history = new ArrayList<>();
    
    public FunctionalCalculator() {
        initializeOperations();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Functional Calculator Demo ===\n");
        
        FunctionalCalculator calculator = new FunctionalCalculator();
        calculator.demonstrateCalculator();
        
        System.out.println("\n=== Text Processing Pipeline Demo ===\n");
        TextProcessor processor = new TextProcessor();
        processor.demonstrateTextProcessing();
        
        System.out.println("\n=== Event Handling System Demo ===\n");
        EventSystem eventSystem = new EventSystem();
        eventSystem.demonstrateEventHandling();
        
        System.out.println("\n=== Functional Applications Completed ===");
    }
    
    private void initializeOperations() {
        // Binary operations
        operations.put("add", (a, b) -> a + b);
        operations.put("subtract", (a, b) -> a - b);
        operations.put("multiply", (a, b) -> a * b);
        operations.put("divide", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Division by zero");
            return a / b;
        });
        operations.put("power", Math::pow);
        operations.put("mod", (a, b) -> a % b);
        
        // Unary operations
        unaryOperations.put("sqrt", Math::sqrt);
        unaryOperations.put("square", x -> x * x);
        unaryOperations.put("negate", x -> -x);
        unaryOperations.put("abs", Math::abs);
        unaryOperations.put("sin", Math::sin);
        unaryOperations.put("cos", Math::cos);
        unaryOperations.put("log", Math::log);
    }
    
    public double calculate(String operation, double a, double b) {
        BinaryOperator<Double> op = operations.get(operation.toLowerCase());
        if (op == null) {
            throw new IllegalArgumentException("Unknown operation: " + operation);
        }
        
        double result = op.apply(a, b);
        String record = String.format("%.2f %s %.2f = %.2f", a, operation, b, result);
        history.add(record);
        
        return result;
    }
    
    public double calculate(String operation, double value) {
        UnaryOperator<Double> op = unaryOperations.get(operation.toLowerCase());
        if (op == null) {
            throw new IllegalArgumentException("Unknown unary operation: " + operation);
        }
        
        double result = op.apply(value);
        String record = String.format("%s(%.2f) = %.2f", operation, value, result);
        history.add(record);
        
        return result;
    }
    
    // Chain operations using function composition
    public double chainOperations(double initial, UnaryOperator<Double>... operations) {
        UnaryOperator<Double> combined = Arrays.stream(operations)
            .reduce(UnaryOperator.identity(), UnaryOperator::andThen);
        
        double result = combined.apply(initial);
        history.add(String.format("Chain operation on %.2f = %.2f", initial, result));
        
        return result;
    }
    
    // Conditional operations
    public double conditionalOperation(double value, 
                                     Predicate<Double> condition,
                                     UnaryOperator<Double> trueOperation,
                                     UnaryOperator<Double> falseOperation) {
        UnaryOperator<Double> operation = condition.test(value) ? trueOperation : falseOperation;
        return operation.apply(value);
    }
    
    public void demonstrateCalculator() {
        System.out.println("Basic operations:");
        System.out.println("10 + 5 = " + calculate("add", 10, 5));
        System.out.println("10 - 3 = " + calculate("subtract", 10, 3));
        System.out.println("4 * 6 = " + calculate("multiply", 4, 6));
        System.out.println("15 / 3 = " + calculate("divide", 15, 3));
        
        System.out.println("\nUnary operations:");
        System.out.println("sqrt(16) = " + calculate("sqrt", 16));
        System.out.println("square(7) = " + calculate("square", 7));
        System.out.println("abs(-5) = " + calculate("abs", -5));
        
        System.out.println("\nChained operations:");
        double result = chainOperations(5,
            x -> x * 2,      // multiply by 2
            x -> x + 3,      // add 3  
            x -> x * x       // square
        );
        System.out.println("Chain: 5 -> *2 -> +3 -> square = " + result);
        
        System.out.println("\nConditional operations:");
        double value = 10;
        double conditionalResult = conditionalOperation(value,
            x -> x > 5,           // condition: greater than 5
            x -> x * 2,           // if true: double it
            x -> x / 2            // if false: halve it
        );
        System.out.println(value + " > 5 ? double : halve = " + conditionalResult);
        
        System.out.println("\nCalculation history:");
        history.forEach(System.out::println);
    }
    
    public void clearHistory() {
        history.clear();
    }
    
    public List<String> getHistory() {
        return new ArrayList<>(history);
    }
}

/**
 * Functional text processing pipeline
 */
class TextProcessor {
    
    public void demonstrateTextProcessing() {
        String text = "Hello World! This is a SAMPLE text for processing. " +
                     "It contains various words and UPPERCASE letters.";
        
        System.out.println("Original text: " + text);
        
        // Define text processing functions
        Function<String, String> removeExclamation = s -> s.replace("!", "");
        Function<String, String> toLowerCase = String::toLowerCase;
        Function<String, String> removeExtraSpaces = s -> s.replaceAll("\\s+", " ");
        Function<String, String[]> splitToWords = s -> s.split("\\s+");
        Function<String[], List<String>> arrayToList = Arrays::asList;
        
        // Predicate for filtering
        Predicate<String> isLongWord = word -> word.length() > 4;
        Predicate<String> containsVowel = word -> word.matches(".*[aeiou].*");
        
        // Process text step by step
        System.out.println("\nStep-by-step processing:");
        
        String step1 = removeExclamation.apply(text);
        System.out.println("1. Remove exclamation: " + step1);
        
        String step2 = toLowerCase.apply(step1);
        System.out.println("2. To lowercase: " + step2);
        
        String step3 = removeExtraSpaces.apply(step2);
        System.out.println("3. Remove extra spaces: " + step3);
        
        String[] words = splitToWords.apply(step3);
        List<String> wordList = arrayToList.apply(words);
        System.out.println("4. Split to words: " + wordList);
        
        // Filter words
        List<String> longWords = wordList.stream()
            .filter(isLongWord)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        System.out.println("5. Long words (>4 chars): " + longWords);
        
        List<String> longWordsWithVowels = wordList.stream()
            .filter(isLongWord.and(containsVowel))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        System.out.println("6. Long words with vowels: " + longWordsWithVowels);
        
        // Compose all transformations
        Function<String, List<String>> textProcessor = removeExclamation
            .andThen(toLowerCase)
            .andThen(removeExtraSpaces)
            .andThen(splitToWords)
            .andThen(arrayToList);
        
        System.out.println("\nComposed processing:");
        List<String> processedWords = textProcessor.apply(text);
        System.out.println("All processed words: " + processedWords);
        
        // Statistics using functional interfaces
        Supplier<Map<Integer, List<String>>> wordsByLength = () ->
            processedWords.stream()
                .collect(Collectors.groupingBy(String::length));
        
        System.out.println("\nWord statistics:");
        System.out.println("Words by length: " + wordsByLength.get());
        
        Function<List<String>, OptionalDouble> averageLength = words_ ->
            words_.stream().mapToInt(String::length).average();
        
        System.out.println("Average word length: " + 
            averageLength.apply(processedWords).orElse(0.0));
    }
}

/**
 * Event handling system using functional interfaces
 */
class EventSystem {
    
    private final Map<String, List<Consumer<String>>> eventHandlers = new HashMap<>();
    private final List<String> eventLog = new ArrayList<>();
    
    public void addEventListener(String eventType, Consumer<String> handler) {
        eventHandlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }
    
    public void removeEventListener(String eventType, Consumer<String> handler) {
        List<Consumer<String>> handlers = eventHandlers.get(eventType);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }
    
    public void fireEvent(String eventType, String eventData) {
        String logEntry = String.format("[%s] %s: %s", 
            new Date(), eventType, eventData);
        eventLog.add(logEntry);
        
        List<Consumer<String>> handlers = eventHandlers.get(eventType);
        if (handlers != null) {
            handlers.forEach(handler -> {
                try {
                    handler.accept(eventData);
                } catch (Exception e) {
                    System.err.println("Error in event handler: " + e.getMessage());
                }
            });
        }
    }
    
    public void demonstrateEventHandling() {
        // Define event handlers using lambdas
        Consumer<String> userLoginHandler = userData -> 
            System.out.println("User logged in: " + userData);
        
        Consumer<String> userLogoutHandler = userData -> 
            System.out.println("User logged out: " + userData);
        
        Consumer<String> securityHandler = userData -> 
            System.out.println("Security check for: " + userData);
        
        Consumer<String> auditHandler = userData -> 
            System.out.println("Audit log: " + userData);
        
        // Register event handlers
        addEventListener("login", userLoginHandler);
        addEventListener("login", securityHandler);
        addEventListener("login", auditHandler);
        
        addEventListener("logout", userLogoutHandler);
        addEventListener("logout", auditHandler);
        
        // Fire some events
        System.out.println("Firing events:");
        fireEvent("login", "alice@example.com");
        fireEvent("logout", "bob@example.com");
        fireEvent("login", "charlie@example.com");
        
        // Complex event handler with chaining
        Consumer<String> emailNotification = userData -> 
            System.out.println("Email notification sent to: " + userData);
        
        Consumer<String> smsNotification = userData -> 
            System.out.println("SMS notification sent to: " + userData);
        
        Consumer<String> combinedNotification = emailNotification.andThen(smsNotification);
        
        addEventListener("important", combinedNotification);
        fireEvent("important", "admin@example.com");
        
        System.out.println("\nEvent log:");
        eventLog.forEach(System.out::println);
        
        // Conditional event handling
        Predicate<String> isVipUser = email -> email.contains("vip");
        Consumer<String> vipHandler = userData -> {
            if (isVipUser.test(userData)) {
                System.out.println("VIP treatment for: " + userData);
            } else {
                System.out.println("Standard treatment for: " + userData);
            }
        };
        
        addEventListener("service", vipHandler);
        fireEvent("service", "vip@example.com");
        fireEvent("service", "regular@example.com");
    }
    
    public List<String> getEventLog() {
        return new ArrayList<>(eventLog);
    }
}
```

---

## 🎯 Summary

**Congratulations!** You've completed **Day 36: Lambda Expressions & Functional Interfaces** and mastered:

- **Lambda Expression Syntax**: From basic syntax to complex expressions with proper scope handling
- **Built-in Functional Interfaces**: Predicate, Function, Consumer, Supplier, and their specialized variants
- **Function Composition**: Chaining and combining functions for complex operations
- **Variable Capture**: Understanding effectively final variables and closure behavior
- **Practical Applications**: Functional calculator, text processing pipeline, and event handling system

**Key Takeaways:**
- Lambdas enable concise, readable code for functional programming
- Built-in functional interfaces provide standardized patterns for common operations
- Function composition allows building complex logic from simple, reusable components
- Functional programming promotes immutability and side-effect-free code
- Event-driven systems benefit greatly from functional interface patterns

**Next Steps:** Tomorrow we'll dive into **Stream API Fundamentals** to learn powerful data processing techniques that leverage the functional programming concepts you've mastered today.

**🏆 Day 36 Complete!** You now understand the foundation of functional programming in Java and can write elegant, maintainable code using lambda expressions and functional interfaces.