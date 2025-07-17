# Day 40: Method References & Functional Composition (August 18, 2025)

**Date:** August 18, 2025  
**Duration:** 3 hours  
**Focus:** Master advanced functional programming techniques with method references and function composition

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Master all types of method references (static, instance, constructor, arbitrary object)
- ✅ Understand when and how to use method references effectively
- ✅ Learn function composition patterns and higher-order functions
- ✅ Implement currying and partial application techniques
- ✅ Build modular, reusable functional components
- ✅ Create sophisticated functional programming architectures

---

## 🚀 Part 1: Method References Mastery (60 minutes)

### **Understanding Method References**

**Method References** provide a compact way to refer to methods without executing them:
- **Cleaner syntax**: More readable than equivalent lambda expressions
- **Reusability**: Reference existing methods instead of creating new lambdas
- **Performance**: Potentially more efficient than lambda expressions
- **Type safety**: Compile-time checking ensures method compatibility

### **Types of Method References**

Create `MethodReferencesDemo.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Comprehensive demonstration of method references and their applications
 */
public class MethodReferencesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Method References Mastery ===\n");
        
        // 1. Static method references
        System.out.println("1. Static Method References:");
        demonstrateStaticMethodReferences();
        
        // 2. Instance method references
        System.out.println("\n2. Instance Method References:");
        demonstrateInstanceMethodReferences();
        
        // 3. Constructor references
        System.out.println("\n3. Constructor References:");
        demonstrateConstructorReferences();
        
        // 4. Arbitrary object method references
        System.out.println("\n4. Arbitrary Object Method References:");
        demonstrateArbitraryObjectMethodReferences();
        
        // 5. Method reference vs lambda comparison
        System.out.println("\n5. Method References vs Lambdas:");
        demonstrateMethodReferenceVsLambda();
        
        System.out.println("\n=== Method References Completed ===");
    }
    
    private static void demonstrateStaticMethodReferences() {
        List<String> numbers = Arrays.asList("1", "2", "3", "4", "5");
        
        // 1. Basic static method reference
        System.out.println("Parsing numbers:");
        
        // Lambda approach
        List<Integer> lambdaParsed = numbers.stream()
            .map(s -> Integer.parseInt(s))
            .collect(Collectors.toList());
        
        // Method reference approach
        List<Integer> methodRefParsed = numbers.stream()
            .map(Integer::parseInt)  // Static method reference
            .collect(Collectors.toList());
        
        System.out.println("Lambda result: " + lambdaParsed);
        System.out.println("Method reference result: " + methodRefParsed);
        
        // 2. Mathematical operations
        List<Double> values = Arrays.asList(1.0, 4.0, 9.0, 16.0, 25.0);
        
        List<Double> squareRoots = values.stream()
            .map(Math::sqrt)  // Static method reference
            .collect(Collectors.toList());
        
        System.out.println("Square roots: " + squareRoots);
        
        // 3. Custom static methods
        List<String> words = Arrays.asList("hello", "world", "java", "programming");
        
        List<String> capitalized = words.stream()
            .map(StringUtils::capitalize)  // Custom static method reference
            .collect(Collectors.toList());
        
        System.out.println("Capitalized: " + capitalized);
        
        // 4. Static method references in reduction
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
        
        Optional<Integer> max = nums.stream()
            .reduce(Integer::max);  // Static method reference
        
        Optional<Integer> sum = nums.stream()
            .reduce(Integer::sum);  // Static method reference
        
        System.out.println("Max: " + max.orElse(0));
        System.out.println("Sum: " + sum.orElse(0));
        
        // 5. Comparator with static method references
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David");
        
        List<String> sortedByLength = names.stream()
            .sorted(Comparator.comparing(String::length))  // Method reference in comparator
            .collect(Collectors.toList());
        
        System.out.println("Sorted by length: " + sortedByLength);
        
        // 6. Multiple static method references in pipeline
        List<String> processed = words.stream()
            .map(String::toUpperCase)     // Instance method reference
            .filter(StringUtils::isLongWord)  // Custom static method reference
            .map(StringUtils::addPrefix)   // Custom static method reference
            .collect(Collectors.toList());
        
        System.out.println("Processed words: " + processed);
    }
    
    private static void demonstrateInstanceMethodReferences() {
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date");
        
        // 1. Instance method references on specific object
        StringProcessor processor = new StringProcessor("-> ");
        
        List<String> processed = words.stream()
            .map(processor::process)  // Instance method reference on specific object
            .collect(Collectors.toList());
        
        System.out.println("Processed by instance: " + processed);
        
        // 2. Chaining instance method references
        List<String> chained = words.stream()
            .map(processor::process)
            .map(processor::addSuffix)
            .collect(Collectors.toList());
        
        System.out.println("Chained processing: " + chained);
        
        // 3. Instance method references with different objects
        List<StringProcessor> processors = Arrays.asList(
            new StringProcessor("["),
            new StringProcessor("{"),
            new StringProcessor("(")
        );
        
        IntStream.range(0, Math.min(words.size(), processors.size()))
            .forEach(i -> {
                String result = processors.get(i).process(words.get(i));
                System.out.println("Processor " + i + ": " + result);
            });
        
        // 4. Instance method references with Optional
        Optional<String> word = Optional.of("hello");
        
        // Using instance method reference with Optional
        String result = word
            .map(processor::process)
            .map(processor::addSuffix)
            .orElse("No word");
        
        System.out.println("Optional processing: " + result);
        
        // 5. Instance method references in collectors
        Map<Integer, List<String>> groupedByLength = words.stream()
            .collect(Collectors.groupingBy(
                processor::getProcessedLength  // Instance method reference
            ));
        
        System.out.println("Grouped by processed length: " + groupedByLength);
        
        // 6. Method references on mutable objects
        Counter counter = new Counter();
        
        words.stream()
            .map(counter::incrementAndGet)  // Instance method reference on mutable object
            .forEach(System.out::println);
        
        System.out.println("Final counter value: " + counter.getValue());
    }
    
    private static void demonstrateConstructorReferences() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        List<Integer> ages = Arrays.asList(25, 30, 35);
        
        // 1. Basic constructor references
        System.out.println("Creating Person objects:");
        
        // Lambda approach
        List<Person> lambdaPersons = names.stream()
            .map(name -> new Person(name))
            .collect(Collectors.toList());
        
        // Constructor reference approach
        List<Person> constructorRefPersons = names.stream()
            .map(Person::new)  // Constructor reference
            .collect(Collectors.toList());
        
        System.out.println("Lambda persons: " + lambdaPersons);
        System.out.println("Constructor ref persons: " + constructorRefPersons);
        
        // 2. Constructor references with multiple parameters
        List<Person> personsWithAge = IntStream.range(0, names.size())
            .mapToObj(i -> new Person(names.get(i), ages.get(i)))
            .collect(Collectors.toList());
        
        System.out.println("Persons with age: " + personsWithAge);
        
        // 3. Array constructor references
        String[] wordsArray = names.stream()
            .toArray(String[]::new);  // Array constructor reference
        
        System.out.println("String array: " + Arrays.toString(wordsArray));
        
        // 4. Generic constructor references
        List<List<String>> nestedLists = names.stream()
            .map(name -> Arrays.asList(name))
            .collect(Collectors.toCollection(ArrayList::new));  // Constructor reference
        
        System.out.println("Nested lists: " + nestedLists);
        
        // 5. Constructor references with collectors
        Map<Character, List<Person>> groupedByFirstLetter = names.stream()
            .map(Person::new)
            .collect(Collectors.groupingBy(
                person -> person.getName().charAt(0)
            ));
        
        System.out.println("Grouped by first letter: " + groupedByFirstLetter);
        
        // 6. Factory pattern with constructor references
        PersonFactory factory = Person::new;  // Constructor reference as factory
        
        List<Person> factoryPersons = names.stream()
            .map(factory::create)
            .collect(Collectors.toList());
        
        System.out.println("Factory persons: " + factoryPersons);
        
        // 7. Complex object construction
        List<Product> products = Arrays.asList("Laptop", "Mouse", "Keyboard").stream()
            .map(Product::new)  // Constructor reference
            .peek(product -> product.setPrice(Math.random() * 1000))
            .collect(Collectors.toList());
        
        System.out.println("Products: " + products);
    }
    
    private static void demonstrateArbitraryObjectMethodReferences() {
        List<String> words = Arrays.asList("hello", "WORLD", "Java", "Programming");
        
        // 1. Instance method reference of arbitrary object of particular type
        System.out.println("String method references:");
        
        // toLowerCase() on arbitrary String objects
        List<String> lowerCase = words.stream()
            .map(String::toLowerCase)  // Arbitrary object method reference
            .collect(Collectors.toList());
        
        System.out.println("Lower case: " + lowerCase);
        
        // 2. Chaining arbitrary object method references
        List<String> processed = words.stream()
            .map(String::toLowerCase)    // Arbitrary object method reference
            .map(String::trim)           // Arbitrary object method reference
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        System.out.println("Processed: " + processed);
        
        // 3. Comparison with arbitrary object method references
        List<String> sorted = words.stream()
            .sorted(String::compareToIgnoreCase)  // Arbitrary object method reference
            .collect(Collectors.toList());
        
        System.out.println("Sorted (ignore case): " + sorted);
        
        // 4. Custom objects with arbitrary method references
        List<Person> persons = Arrays.asList(
            new Person("Alice", 25),
            new Person("Bob", 30),
            new Person("Charlie", 35)
        );
        
        List<String> names = persons.stream()
            .map(Person::getName)  // Arbitrary object method reference
            .collect(Collectors.toList());
        
        System.out.println("Person names: " + names);
        
        // 5. Method references with predicates
        List<String> longWords = words.stream()
            .filter(StringUtils::isLongWord)  // Static method reference as predicate
            .collect(Collectors.toList());
        
        System.out.println("Long words: " + longWords);
        
        // 6. Complex arbitrary object method references
        Map<Integer, List<Person>> groupedByAge = persons.stream()
            .collect(Collectors.groupingBy(Person::getAge));  // Arbitrary object method reference
        
        System.out.println("Grouped by age: " + groupedByAge);
        
        // 7. Nested method references
        List<Character> firstLetters = persons.stream()
            .map(Person::getName)       // Arbitrary object method reference
            .map(String::toLowerCase)   // Arbitrary object method reference
            .map(s -> s.charAt(0))      // Lambda for complex operations
            .collect(Collectors.toList());
        
        System.out.println("First letters: " + firstLetters);
    }
    
    private static void demonstrateMethodReferenceVsLambda() {
        List<String> data = Arrays.asList("apple", "banana", "cherry", "date");
        
        System.out.println("Comparing method references vs lambdas:");
        
        // 1. Simple transformation
        System.out.println("\nSimple transformation:");
        
        // Lambda
        List<String> lambdaUpper = data.stream()
            .map(s -> s.toUpperCase())
            .collect(Collectors.toList());
        
        // Method reference
        List<String> methodRefUpper = data.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());
        
        System.out.println("Lambda result: " + lambdaUpper);
        System.out.println("Method ref result: " + methodRefUpper);
        
        // 2. When to use lambda vs method reference
        System.out.println("\nWhen to use each approach:");
        
        // Use method reference: direct method call
        List<Integer> lengths1 = data.stream()
            .map(String::length)  // Good: direct method call
            .collect(Collectors.toList());
        
        // Use lambda: additional logic needed
        List<Integer> lengths2 = data.stream()
            .map(s -> s.length() + 10)  // Good: additional logic
            .collect(Collectors.toList());
        
        // Use lambda: multiple operations
        List<String> processed = data.stream()
            .map(s -> s.toUpperCase().substring(0, Math.min(3, s.length())))
            .collect(Collectors.toList());
        
        System.out.println("Direct method call: " + lengths1);
        System.out.println("With additional logic: " + lengths2);
        System.out.println("Complex processing: " + processed);
        
        // 3. Performance considerations
        System.out.println("\nPerformance considerations:");
        
        int iterations = 1_000_000;
        List<String> largeData = Collections.nCopies(iterations, "test");
        
        // Lambda performance
        long start = System.nanoTime();
        long lambdaSum = largeData.stream()
            .mapToInt(s -> s.length())
            .sum();
        long lambdaTime = System.nanoTime() - start;
        
        // Method reference performance
        start = System.nanoTime();
        long methodRefSum = largeData.stream()
            .mapToInt(String::length)
            .sum();
        long methodRefTime = System.nanoTime() - start;
        
        System.out.printf("Lambda time: %d ms, result: %d%n", lambdaTime / 1_000_000, lambdaSum);
        System.out.printf("Method ref time: %d ms, result: %d%n", methodRefTime / 1_000_000, methodRefSum);
        
        // 4. Readability comparison
        System.out.println("\nReadability comparison:");
        
        List<Person> persons = Arrays.asList(
            new Person("Alice", 25),
            new Person("Bob", 30)
        );
        
        // Less readable with lambda
        List<String> names1 = persons.stream()
            .map(p -> p.getName())
            .collect(Collectors.toList());
        
        // More readable with method reference
        List<String> names2 = persons.stream()
            .map(Person::getName)
            .collect(Collectors.toList());
        
        System.out.println("Names (lambda): " + names1);
        System.out.println("Names (method ref): " + names2);
    }
    
    // Utility classes and methods
    static class StringUtils {
        public static String capitalize(String str) {
            if (str == null || str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
        
        public static boolean isLongWord(String str) {
            return str != null && str.length() > 4;
        }
        
        public static String addPrefix(String str) {
            return "*** " + str;
        }
    }
    
    static class StringProcessor {
        private final String prefix;
        
        public StringProcessor(String prefix) {
            this.prefix = prefix;
        }
        
        public String process(String input) {
            return prefix + input;
        }
        
        public String addSuffix(String input) {
            return input + " ***";
        }
        
        public int getProcessedLength(String input) {
            return process(input).length();
        }
    }
    
    static class Counter {
        private int value = 0;
        
        public String incrementAndGet(String input) {
            return (++value) + ": " + input;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    static class Person {
        private final String name;
        private final int age;
        
        public Person(String name) {
            this(name, 0);
        }
        
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        
        @Override
        public String toString() {
            return String.format("Person{name='%s', age=%d}", name, age);
        }
    }
    
    static class Product {
        private final String name;
        private double price;
        
        public Product(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        
        @Override
        public String toString() {
            return String.format("Product{name='%s', price=%.2f}", name, price);
        }
    }
    
    @FunctionalInterface
    interface PersonFactory {
        Person create(String name);
    }
}
```

---

## 🔧 Part 2: Function Composition & Higher-Order Functions (60 minutes)

### **Advanced Functional Programming Patterns**

Create `FunctionalComposition.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Advanced function composition and higher-order function patterns
 */
public class FunctionalComposition {
    
    public static void main(String[] args) {
        System.out.println("=== Function Composition & Higher-Order Functions ===\n");
        
        // 1. Basic function composition
        System.out.println("1. Basic Function Composition:");
        demonstrateBasicComposition();
        
        // 2. Complex composition patterns
        System.out.println("\n2. Complex Composition Patterns:");
        demonstrateComplexComposition();
        
        // 3. Higher-order functions
        System.out.println("\n3. Higher-Order Functions:");
        demonstrateHigherOrderFunctions();
        
        // 4. Currying and partial application
        System.out.println("\n4. Currying and Partial Application:");
        demonstrateCurryingAndPartialApplication();
        
        // 5. Function factories and builders
        System.out.println("\n5. Function Factories and Builders:");
        demonstrateFunctionFactories();
        
        System.out.println("\n=== Function Composition Completed ===");
    }
    
    private static void demonstrateBasicComposition() {
        // 1. Function.andThen() composition
        Function<String, String> addPrefix = s -> "Hello, " + s;
        Function<String, String> addSuffix = s -> s + "!";
        Function<String, String> toUpper = String::toUpperCase;
        
        // Compose functions with andThen
        Function<String, String> greetingComposed = addPrefix
            .andThen(addSuffix)
            .andThen(toUpper);
        
        String result = greetingComposed.apply("World");
        System.out.println("Composed greeting: " + result);
        
        // 2. Function.compose() - reverse composition
        Function<String, String> reverseComposed = toUpper
            .compose(addSuffix)
            .compose(addPrefix);
        
        String reverseResult = reverseComposed.apply("World");
        System.out.println("Reverse composed: " + reverseResult);
        
        // 3. Mathematical function composition
        Function<Double, Double> multiplyBy2 = x -> x * 2;
        Function<Double, Double> add10 = x -> x + 10;
        Function<Double, Double> square = x -> x * x;
        
        Function<Double, Double> mathComposed = multiplyBy2
            .andThen(add10)
            .andThen(square);
        
        Double mathResult = mathComposed.apply(5.0);
        System.out.println("Math composition f(g(h(5))): " + mathResult); // ((5*2)+10)^2 = 400
        
        // 4. Predicate composition
        Predicate<String> isLong = s -> s.length() > 5;
        Predicate<String> hasVowels = s -> s.matches(".*[aeiou].*");
        Predicate<String> startsWithUppercase = s -> Character.isUpperCase(s.charAt(0));
        
        Predicate<String> complexPredicate = isLong
            .and(hasVowels)
            .and(startsWithUppercase);
        
        List<String> words = Arrays.asList("Hello", "Programming", "Java", "Code", "Beautiful");
        
        List<String> filtered = words.stream()
            .filter(complexPredicate)
            .collect(Collectors.toList());
        
        System.out.println("Complex predicate filter: " + filtered);
        
        // 5. Consumer composition
        Consumer<String> print = System.out::println;
        Consumer<String> log = s -> System.out.println("LOG: " + s);
        Consumer<String> debug = s -> System.out.println("DEBUG: " + s.toUpperCase());
        
        Consumer<String> combinedConsumer = print.andThen(log).andThen(debug);
        
        System.out.println("Combined consumer output:");
        combinedConsumer.accept("Function composition");
    }
    
    private static void demonstrateComplexComposition() {
        // 1. Pipeline composition for data processing
        Function<String, String> clean = s -> s.trim().toLowerCase();
        Function<String, String> validate = s -> s.isEmpty() ? "invalid" : s;
        Function<String, String> format = s -> s.equals("invalid") ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
        Function<String, String> tag = s -> s.equals("invalid") ? s : "[PROCESSED] " + s;
        
        Function<String, String> dataProcessor = clean
            .andThen(validate)
            .andThen(format)
            .andThen(tag);
        
        List<String> rawData = Arrays.asList("  hello  ", "WORLD", "", "   java   ");
        
        List<String> processedData = rawData.stream()
            .map(dataProcessor)
            .collect(Collectors.toList());
        
        System.out.println("Data processing pipeline: " + processedData);
        
        // 2. Conditional composition
        Function<Integer, String> evenProcessor = n -> "Even: " + (n * 2);
        Function<Integer, String> oddProcessor = n -> "Odd: " + (n * 3 + 1);
        
        Function<Integer, String> conditionalProcessor = createConditionalFunction(
            n -> n % 2 == 0,
            evenProcessor,
            oddProcessor
        );
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<String> conditionalResults = numbers.stream()
            .map(conditionalProcessor)
            .collect(Collectors.toList());
        
        System.out.println("Conditional processing: " + conditionalResults);
        
        // 3. Exception-safe composition
        Function<String, Integer> safeParse = createSafeFunction(
            Integer::parseInt,
            -1  // default value for parse errors
        );
        
        Function<Integer, String> formatNumber = n -> 
            n == -1 ? "Invalid number" : "Number: " + n;
        
        Function<String, String> safeNumberProcessor = safeParse.andThen(formatNumber);
        
        List<String> numberStrings = Arrays.asList("123", "456", "invalid", "789");
        List<String> safeResults = numberStrings.stream()
            .map(safeNumberProcessor)
            .collect(Collectors.toList());
        
        System.out.println("Safe number processing: " + safeResults);
        
        // 4. Recursive composition
        Function<Integer, Integer> factorial = createRecursiveFunction(
            n -> n <= 1,
            n -> 1,
            n -> n * factorial.apply(n - 1)
        );
        
        List<Integer> factorialResults = Arrays.asList(0, 1, 2, 3, 4, 5).stream()
            .map(factorial)
            .collect(Collectors.toList());
        
        System.out.println("Factorial results: " + factorialResults);
        
        // 5. Validation composition chain
        Validator<User> nameValidator = user -> 
            user.getName() != null && !user.getName().trim().isEmpty() ? 
            ValidationResult.valid() : ValidationResult.invalid("Name is required");
        
        Validator<User> emailValidator = user -> 
            user.getEmail() != null && user.getEmail().contains("@") ? 
            ValidationResult.valid() : ValidationResult.invalid("Valid email is required");
        
        Validator<User> ageValidator = user -> 
            user.getAge() >= 18 ? 
            ValidationResult.valid() : ValidationResult.invalid("Age must be 18 or older");
        
        Validator<User> composedValidator = composeValidators(nameValidator, emailValidator, ageValidator);
        
        User validUser = new User("Alice", "alice@example.com", 25);
        User invalidUser = new User("", "invalid-email", 16);
        
        System.out.println("Valid user validation: " + composedValidator.validate(validUser));
        System.out.println("Invalid user validation: " + composedValidator.validate(invalidUser));
    }
    
    private static void demonstrateHigherOrderFunctions() {
        // 1. Functions that return functions
        Function<String, Function<String, String>> prefixAdder = prefix -> 
            text -> prefix + text;
        
        Function<String, String> helloAdder = prefixAdder.apply("Hello, ");
        Function<String, String> debugAdder = prefixAdder.apply("DEBUG: ");
        
        System.out.println("Hello adder: " + helloAdder.apply("World"));
        System.out.println("Debug adder: " + debugAdder.apply("Processing"));
        
        // 2. Functions that take functions as parameters
        Function<String, String> processWithLogging = createLoggingWrapper(
            String::toUpperCase,
            "Converting to uppercase"
        );
        
        String loggedResult = processWithLogging.apply("hello world");
        System.out.println("Logged result: " + loggedResult);
        
        // 3. Higher-order function for retry logic
        Function<String, Optional<Integer>> retryableParse = createRetryableFunction(
            Integer::parseInt,
            3  // max retries
        );
        
        Optional<Integer> retryResult = retryableParse.apply("123");
        System.out.println("Retry result: " + retryResult);
        
        // 4. Function caching (memoization)
        Function<Integer, Integer> expensiveCalculation = n -> {
            System.out.println("Computing expensive calculation for: " + n);
            try {
                Thread.sleep(100); // Simulate expensive operation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return n * n * n;
        };
        
        Function<Integer, Integer> memoizedCalculation = memoize(expensiveCalculation);
        
        System.out.println("First call: " + memoizedCalculation.apply(5));
        System.out.println("Second call (cached): " + memoizedCalculation.apply(5));
        System.out.println("Third call (new): " + memoizedCalculation.apply(6));
        
        // 5. Function transformation utilities
        List<Function<Integer, Integer>> transformations = Arrays.asList(
            x -> x + 1,
            x -> x * 2,
            x -> x * x
        );
        
        Function<Integer, Integer> chainedTransformation = 
            transformations.stream()
                .reduce(Function.identity(), Function::andThen);
        
        Integer chainResult = chainedTransformation.apply(3); // ((3+1)*2)^2 = 64
        System.out.println("Chained transformation result: " + chainResult);
        
        // 6. Conditional function application
        Function<Integer, Integer> conditionalTransform = createConditionalTransform(
            n -> n > 10,
            n -> n / 2,
            n -> n * 3
        );
        
        List<Integer> testNumbers = Arrays.asList(5, 15, 8, 20);
        List<Integer> transformedNumbers = testNumbers.stream()
            .map(conditionalTransform)
            .collect(Collectors.toList());
        
        System.out.println("Conditional transform: " + transformedNumbers);
    }
    
    private static void demonstrateCurryingAndPartialApplication() {
        // 1. Basic currying
        TriFunction<Integer, Integer, Integer, Integer> add3 = (a, b, c) -> a + b + c;
        
        Function<Integer, Function<Integer, Function<Integer, Integer>>> curriedAdd3 = 
            a -> b -> c -> add3.apply(a, b, c);
        
        Function<Integer, Function<Integer, Integer>> add5AndSomething = curriedAdd3.apply(5);
        Function<Integer, Integer> add5And3AndSomething = add5AndSomething.apply(3);
        
        Integer curriedResult = add5And3AndSomething.apply(2); // 5 + 3 + 2 = 10
        System.out.println("Curried addition result: " + curriedResult);
        
        // 2. Practical currying example - string formatting
        QuadFunction<String, String, String, String, String> formatter = 
            (template, arg1, arg2, arg3) -> String.format(template, arg1, arg2, arg3);
        
        Function<String, Function<String, Function<String, String>>> curriedFormatter = 
            template -> arg1 -> arg2 -> arg3 -> formatter.apply(template, arg1, arg2, arg3);
        
        Function<String, Function<String, String>> logFormatter = 
            curriedFormatter.apply("[%s] %s: %s");
        
        Function<String, String> infoLogger = logFormatter.apply("INFO");
        Function<String, String> errorLogger = logFormatter.apply("ERROR");
        
        System.out.println("Info log: " + infoLogger.apply("Application started"));
        System.out.println("Error log: " + errorLogger.apply("Connection failed"));
        
        // 3. Partial application utility
        BinaryOperator<Integer> multiply = (a, b) -> a * b;
        
        Function<Integer, Integer> multiplyBy10 = partialApply(multiply, 10);
        Function<Integer, Integer> multiplyBy3 = partialApply(multiply, 3);
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        
        List<Integer> multipliedBy10 = numbers.stream()
            .map(multiplyBy10)
            .collect(Collectors.toList());
        
        List<Integer> multipliedBy3 = numbers.stream()
            .map(multiplyBy3)
            .collect(Collectors.toList());
        
        System.out.println("Multiplied by 10: " + multipliedBy10);
        System.out.println("Multiplied by 3: " + multipliedBy3);
        
        // 4. Configuration-based function creation
        DatabaseConfig config = new DatabaseConfig("localhost", 5432, "mydb");
        
        Function<String, String> connectionStringBuilder = buildConnectionStringFunction(config);
        
        String connectionString = connectionStringBuilder.apply("user123");
        System.out.println("Connection string: " + connectionString);
        
        // 5. Event handler currying
        TriFunction<String, String, String, String> eventHandler = 
            (eventType, source, message) -> 
                String.format("Event[%s] from %s: %s", eventType, source, message);
        
        Function<String, Function<String, String>> infoEventHandler = 
            curry(eventHandler).apply("INFO");
        
        Function<String, String> systemInfoHandler = infoEventHandler.apply("SYSTEM");
        Function<String, String> userInfoHandler = infoEventHandler.apply("USER");
        
        System.out.println("System event: " + systemInfoHandler.apply("Service started"));
        System.out.println("User event: " + userInfoHandler.apply("User logged in"));
    }
    
    private static void demonstrateFunctionFactories() {
        // 1. Validation function factory
        Function<String, Predicate<String>> validationFactory = createValidationFactory();
        
        Predicate<String> emailValidator = validationFactory.apply("email");
        Predicate<String> phoneValidator = validationFactory.apply("phone");
        
        System.out.println("Email validation: " + emailValidator.test("user@example.com"));
        System.out.println("Phone validation: " + phoneValidator.test("123-456-7890"));
        
        // 2. Converter function factory
        Map<String, Function<String, Object>> converterFactory = createConverterFactory();
        
        Function<String, Object> intConverter = converterFactory.get("int");
        Function<String, Object> doubleConverter = converterFactory.get("double");
        Function<String, Object> boolConverter = converterFactory.get("boolean");
        
        System.out.println("Int conversion: " + intConverter.apply("123"));
        System.out.println("Double conversion: " + doubleConverter.apply("123.45"));
        System.out.println("Boolean conversion: " + boolConverter.apply("true"));
        
        // 3. Mathematical operation factory
        BiFunction<String, Double, Function<Double, Double>> mathFactory = createMathOperationFactory();
        
        Function<Double, Double> squareRoot = mathFactory.apply("sqrt", 0.0);
        Function<Double, Double> power2 = mathFactory.apply("power", 2.0);
        Function<Double, Double> power3 = mathFactory.apply("power", 3.0);
        
        List<Double> values = Arrays.asList(4.0, 9.0, 16.0);
        
        System.out.println("Square roots: " + values.stream().map(squareRoot).collect(Collectors.toList()));
        System.out.println("Power of 2: " + values.stream().map(power2).collect(Collectors.toList()));
        System.out.println("Power of 3: " + values.stream().map(power3).collect(Collectors.toList()));
        
        // 4. Builder pattern with functions
        ProcessorBuilder builder = ProcessorBuilder.create()
            .addStep(String::trim)
            .addStep(String::toLowerCase)
            .addConditionalStep(s -> s.length() > 5, s -> s.substring(0, 5))
            .addStep(s -> "Processed: " + s);
        
        Function<String, String> processor = builder.build();
        
        String builderResult = processor.apply("  HELLO WORLD  ");
        System.out.println("Builder result: " + builderResult);
        
        // 5. Dynamic function composition factory
        FunctionComposer<String, String> composer = new FunctionComposer<>();
        
        Function<String, String> dynamicProcessor = composer
            .then(String::trim)
            .then(s -> s.replaceAll("\\s+", "_"))
            .then(String::toUpperCase)
            .build();
        
        String dynamicResult = dynamicProcessor.apply("  hello    world  ");
        System.out.println("Dynamic composer result: " + dynamicResult);
    }
    
    // Utility methods for composition and higher-order functions
    private static <T> Function<T, String> createConditionalFunction(
            Predicate<T> condition,
            Function<T, String> trueFunction,
            Function<T, String> falseFunction) {
        return input -> condition.test(input) ? trueFunction.apply(input) : falseFunction.apply(input);
    }
    
    private static <T, R> Function<T, R> createSafeFunction(Function<T, R> function, R defaultValue) {
        return input -> {
            try {
                return function.apply(input);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }
    
    private static <T, R> Function<T, R> createRecursiveFunction(
            Predicate<T> baseCase,
            Function<T, R> baseFunction,
            Function<T, R> recursiveFunction) {
        return new Function<T, R>() {
            @Override
            public R apply(T input) {
                return baseCase.test(input) ? baseFunction.apply(input) : recursiveFunction.apply(input);
            }
        };
    }
    
    private static <T> Function<T, String> createLoggingWrapper(Function<T, String> function, String logMessage) {
        return input -> {
            System.out.println("LOG: " + logMessage);
            return function.apply(input);
        };
    }
    
    private static <T, R> Function<T, Optional<R>> createRetryableFunction(Function<T, R> function, int maxRetries) {
        return input -> {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    return Optional.of(function.apply(input));
                } catch (Exception e) {
                    if (i == maxRetries - 1) {
                        return Optional.empty();
                    }
                }
            }
            return Optional.empty();
        };
    }
    
    private static <T, R> Function<T, R> memoize(Function<T, R> function) {
        Map<T, R> cache = new HashMap<>();
        return input -> cache.computeIfAbsent(input, function);
    }
    
    private static <T> Function<T, T> createConditionalTransform(
            Predicate<T> condition,
            Function<T, T> trueTransform,
            Function<T, T> falseTransform) {
        return input -> condition.test(input) ? trueTransform.apply(input) : falseTransform.apply(input);
    }
    
    private static <T> Function<T, T> partialApply(BinaryOperator<T> operator, T fixedValue) {
        return input -> operator.apply(fixedValue, input);
    }
    
    private static Function<String, String> buildConnectionStringFunction(DatabaseConfig config) {
        return username -> String.format("jdbc:postgresql://%s:%d/%s?user=%s", 
            config.host, config.port, config.database, username);
    }
    
    private static <A, B, C, R> Function<A, Function<B, Function<C, R>>> curry(TriFunction<A, B, C, R> function) {
        return a -> b -> c -> function.apply(a, b, c);
    }
    
    private static <T> Validator<T> composeValidators(Validator<T>... validators) {
        return input -> {
            List<String> errors = new ArrayList<>();
            for (Validator<T> validator : validators) {
                ValidationResult result = validator.validate(input);
                if (!result.isValid()) {
                    errors.add(result.getErrorMessage());
                }
            }
            return errors.isEmpty() ? ValidationResult.valid() : 
                   ValidationResult.invalid(String.join(", ", errors));
        };
    }
    
    private static Function<String, Predicate<String>> createValidationFactory() {
        Map<String, Predicate<String>> validators = Map.of(
            "email", s -> s.contains("@") && s.contains("."),
            "phone", s -> s.matches("\\d{3}-\\d{3}-\\d{4}"),
            "zipcode", s -> s.matches("\\d{5}")
        );
        return type -> validators.getOrDefault(type, s -> true);
    }
    
    private static Map<String, Function<String, Object>> createConverterFactory() {
        return Map.of(
            "int", s -> { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } },
            "double", s -> { try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; } },
            "boolean", Boolean::parseBoolean
        );
    }
    
    private static BiFunction<String, Double, Function<Double, Double>> createMathOperationFactory() {
        return (operation, parameter) -> {
            switch (operation) {
                case "sqrt": return Math::sqrt;
                case "power": return x -> Math.pow(x, parameter);
                case "multiply": return x -> x * parameter;
                default: return Function.identity();
            }
        };
    }
    
    // Supporting classes and interfaces
    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
    
    @FunctionalInterface
    interface QuadFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }
    
    @FunctionalInterface
    interface Validator<T> {
        ValidationResult validate(T input);
    }
    
    static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + errorMessage;
        }
    }
    
    static class User {
        private final String name;
        private final String email;
        private final int age;
        
        public User(String name, String email, int age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }
        
        public String getName() { return name; }
        public String getEmail() { return email; }
        public int getAge() { return age; }
    }
    
    static class DatabaseConfig {
        final String host;
        final int port;
        final String database;
        
        public DatabaseConfig(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
    }
    
    static class ProcessorBuilder {
        private Function<String, String> processor = Function.identity();
        
        public static ProcessorBuilder create() {
            return new ProcessorBuilder();
        }
        
        public ProcessorBuilder addStep(Function<String, String> step) {
            processor = processor.andThen(step);
            return this;
        }
        
        public ProcessorBuilder addConditionalStep(Predicate<String> condition, Function<String, String> step) {
            processor = processor.andThen(s -> condition.test(s) ? step.apply(s) : s);
            return this;
        }
        
        public Function<String, String> build() {
            return processor;
        }
    }
    
    static class FunctionComposer<T, R> {
        private Function<T, R> function = (Function<T, R>) Function.identity();
        
        public <U> FunctionComposer<T, U> then(Function<R, U> nextFunction) {
            FunctionComposer<T, U> newComposer = new FunctionComposer<>();
            newComposer.function = function.andThen(nextFunction);
            return newComposer;
        }
        
        public Function<T, R> build() {
            return function;
        }
    }
}
```

---

## 📊 Part 3: Practical Applications & Design Patterns (60 minutes)

### **Real-World Functional Programming Architectures**

Create `FunctionalDesignPatterns.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Practical applications of functional programming patterns in real-world scenarios
 */
public class FunctionalDesignPatterns {
    
    public static void main(String[] args) {
        System.out.println("=== Functional Design Patterns ===\n");
        
        // 1. Command pattern with functions
        System.out.println("1. Command Pattern with Functions:");
        demonstrateCommandPattern();
        
        // 2. Strategy pattern with functions
        System.out.println("\n2. Strategy Pattern with Functions:");
        demonstrateStrategyPattern();
        
        // 3. Observer pattern with functions
        System.out.println("\n3. Observer Pattern with Functions:");
        demonstrateObserverPattern();
        
        // 4. Pipeline architecture
        System.out.println("\n4. Pipeline Architecture:");
        demonstratePipelineArchitecture();
        
        // 5. Functional dependency injection
        System.out.println("\n5. Functional Dependency Injection:");
        demonstrateFunctionalDI();
        
        System.out.println("\n=== Functional Design Patterns Completed ===");
    }
    
    private static void demonstrateCommandPattern() {
        // Traditional command pattern using functions
        CommandProcessor processor = new CommandProcessor();
        
        // Define commands as functions
        Command saveCommand = () -> "Document saved";
        Command printCommand = () -> "Document printed";
        Command emailCommand = () -> "Document emailed";
        
        // Execute commands
        processor.execute(saveCommand);
        processor.execute(printCommand);
        processor.execute(emailCommand);
        
        // Undo functionality
        System.out.println("Undo last command: " + processor.undo());
        
        // Parametrized commands
        Function<String, Command> createSaveCommand = filename -> 
            () -> "Saved file: " + filename;
        
        Function<String, Command> createPrintCommand = printer -> 
            () -> "Printed on: " + printer;
        
        processor.execute(createSaveCommand.apply("document.pdf"));
        processor.execute(createPrintCommand.apply("HP LaserJet"));
        
        // Macro commands (composite)
        Command macroCommand = () -> {
            String result1 = saveCommand.execute();
            String result2 = printCommand.execute();
            String result3 = emailCommand.execute();
            return String.format("Macro: %s, %s, %s", result1, result2, result3);
        };
        
        processor.execute(macroCommand);
        
        // Conditional commands
        Predicate<LocalContext> hasPermission = ctx -> ctx.userRole.equals("ADMIN");
        
        Command conditionalCommand = createConditionalCommand(
            hasPermission,
            () -> "Admin operation executed",
            () -> "Access denied"
        );
        
        LocalContext adminContext = new LocalContext("ADMIN");
        LocalContext userContext = new LocalContext("USER");
        
        ContextualCommandProcessor contextProcessor = new ContextualCommandProcessor();
        contextProcessor.execute(conditionalCommand, adminContext);
        contextProcessor.execute(conditionalCommand, userContext);
    }
    
    private static void demonstrateStrategyPattern() {
        // Payment processing strategies
        PaymentProcessor processor = new PaymentProcessor();
        
        // Define strategies as functions
        PaymentStrategy creditCardStrategy = amount -> 
            String.format("Charged $%.2f to credit card", amount);
        
        PaymentStrategy paypalStrategy = amount -> 
            String.format("Paid $%.2f via PayPal", amount);
        
        PaymentStrategy bankTransferStrategy = amount -> 
            String.format("Transferred $%.2f via bank", amount);
        
        // Process payments with different strategies
        processor.processPayment(100.0, creditCardStrategy);
        processor.processPayment(250.0, paypalStrategy);
        processor.processPayment(500.0, bankTransferStrategy);
        
        // Dynamic strategy selection
        Function<Double, PaymentStrategy> strategySelector = amount -> {
            if (amount < 100) return creditCardStrategy;
            else if (amount < 500) return paypalStrategy;
            else return bankTransferStrategy;
        };
        
        List<Double> amounts = Arrays.asList(50.0, 200.0, 750.0);
        amounts.forEach(amount -> {
            PaymentStrategy strategy = strategySelector.apply(amount);
            processor.processPayment(amount, strategy);
        });
        
        // Sorting strategies
        List<Person> people = Arrays.asList(
            new Person("Alice", 25),
            new Person("Bob", 30),
            new Person("Charlie", 35)
        );
        
        // Different sorting strategies
        Comparator<Person> byName = Comparator.comparing(Person::getName);
        Comparator<Person> byAge = Comparator.comparing(Person::getAge);
        Comparator<Person> byNameLength = Comparator.comparing(p -> p.getName().length());
        
        System.out.println("Original: " + people);
        System.out.println("By name: " + people.stream().sorted(byName).collect(Collectors.toList()));
        System.out.println("By age: " + people.stream().sorted(byAge).collect(Collectors.toList()));
        System.out.println("By name length: " + people.stream().sorted(byNameLength).collect(Collectors.toList()));
        
        // Validation strategies
        ValidationEngine validator = new ValidationEngine();
        
        validator.addRule("email", email -> email.contains("@") ? 
            ValidationResult.valid() : ValidationResult.invalid("Invalid email format"));
        
        validator.addRule("age", ageStr -> {
            try {
                int age = Integer.parseInt(ageStr);
                return age >= 18 ? ValidationResult.valid() : ValidationResult.invalid("Must be 18 or older");
            } catch (NumberFormatException e) {
                return ValidationResult.invalid("Age must be a number");
            }
        });
        
        Map<String, String> userData = Map.of(
            "email", "user@example.com",
            "age", "25"
        );
        
        ValidationResult validationResult = validator.validate(userData);
        System.out.println("Validation result: " + validationResult);
    }
    
    private static void demonstrateObserverPattern() {
        // Event-driven architecture with functions
        EventBus eventBus = new EventBus();
        
        // Register event handlers as functions
        Consumer<UserEvent> emailNotifier = event -> 
            System.out.printf("📧 Email: User %s %s\n", event.username, event.action);
        
        Consumer<UserEvent> logHandler = event -> 
            System.out.printf("📝 Log: [%s] User %s performed %s\n", 
                new Date(), event.username, event.action);
        
        Consumer<UserEvent> auditHandler = event -> 
            System.out.printf("🔍 Audit: Security log - %s by %s at %s\n", 
                event.action, event.username, new Date());
        
        // Subscribe handlers to events
        eventBus.subscribe("user.login", emailNotifier);
        eventBus.subscribe("user.login", logHandler);
        eventBus.subscribe("user.login", auditHandler);
        
        eventBus.subscribe("user.logout", logHandler);
        eventBus.subscribe("user.logout", auditHandler);
        
        // Publish events
        eventBus.publish("user.login", new UserEvent("alice", "login"));
        eventBus.publish("user.logout", new UserEvent("alice", "logout"));
        
        // Conditional observers
        Consumer<UserEvent> adminNotifier = event -> {
            if (event.username.startsWith("admin")) {
                System.out.printf("👑 Admin notification: %s %s\n", event.username, event.action);
            }
        };
        
        eventBus.subscribe("user.login", adminNotifier);
        eventBus.publish("user.login", new UserEvent("admin_bob", "login"));
        
        // Reactive streams with functional composition
        ReactiveStream<String> dataStream = new ReactiveStream<>();
        
        dataStream
            .filter(data -> data.length() > 3)
            .map(String::toUpperCase)
            .forEach(data -> System.out.println("Processed: " + data));
        
        dataStream.emit("hi");
        dataStream.emit("hello");
        dataStream.emit("world");
        dataStream.emit("java");
    }
    
    private static void demonstratePipelineArchitecture() {
        // Data processing pipeline
        DataPipeline<String> pipeline = DataPipeline.<String>builder()
            .addStage("clean", data -> data.trim().toLowerCase())
            .addStage("validate", data -> data.isEmpty() ? null : data)
            .addStage("transform", data -> data.replace(" ", "_"))
            .addStage("format", data -> "[PROCESSED] " + data)
            .build();
        
        List<String> rawData = Arrays.asList("  Hello World  ", "", "Java Programming", "  ");
        
        List<String> processedData = rawData.stream()
            .map(pipeline::process)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("Pipeline processed: " + processedData);
        
        // Async pipeline
        AsyncPipeline<Integer, String> asyncPipeline = AsyncPipeline.<Integer, String>builder()
            .addAsyncStage("double", x -> CompletableFuture.supplyAsync(() -> x * 2))
            .addAsyncStage("toString", x -> CompletableFuture.supplyAsync(() -> "Value: " + x))
            .addAsyncStage("format", s -> CompletableFuture.supplyAsync(() -> s.toUpperCase()))
            .build();
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        
        List<CompletableFuture<String>> futures = numbers.stream()
            .map(asyncPipeline::processAsync)
            .collect(Collectors.toList());
        
        List<String> asyncResults = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        System.out.println("Async pipeline results: " + asyncResults);
        
        // Error handling pipeline
        ErrorHandlingPipeline<String> errorPipeline = ErrorHandlingPipeline.<String>builder()
            .addStage("parse", s -> Integer.parseInt(s), -1)
            .addStage("double", i -> i * 2, -1)
            .addStage("format", i -> "Result: " + i, "Error")
            .build();
        
        List<String> testInputs = Arrays.asList("123", "invalid", "456");
        List<String> errorResults = testInputs.stream()
            .map(errorPipeline::process)
            .collect(Collectors.toList());
        
        System.out.println("Error handling results: " + errorResults);
    }
    
    private static void demonstrateFunctionalDI() {
        // Functional dependency injection
        ServiceRegistry registry = new ServiceRegistry();
        
        // Register services as functions
        registry.register("logger", () -> (Consumer<String>) msg -> 
            System.out.println("[LOG] " + msg));
        
        registry.register("validator", () -> (Function<String, Boolean>) email -> 
            email.contains("@"));
        
        registry.register("formatter", () -> (Function<String, String>) text -> 
            text.toUpperCase());
        
        // Create service that depends on other services
        Function<ServiceRegistry, UserService> userServiceFactory = reg -> {
            Consumer<String> logger = reg.get("logger");
            Function<String, Boolean> validator = reg.get("validator");
            Function<String, String> formatter = reg.get("formatter");
            
            return new UserService(logger, validator, formatter);
        };
        
        UserService userService = userServiceFactory.apply(registry);
        
        userService.processUser("alice@example.com");
        userService.processUser("invalid-email");
        
        // Configuration-based factory
        Configuration config = new Configuration();
        config.set("database.host", "localhost");
        config.set("database.port", "5432");
        config.set("cache.enabled", "true");
        
        Function<Configuration, DatabaseService> dbServiceFactory = cfg -> {
            String host = cfg.get("database.host");
            String port = cfg.get("database.port");
            boolean cacheEnabled = Boolean.parseBoolean(cfg.get("cache.enabled"));
            
            return new DatabaseService(host, port, cacheEnabled);
        };
        
        DatabaseService dbService = dbServiceFactory.apply(config);
        dbService.connect();
        
        // Conditional service creation
        Function<String, Optional<MessageService>> messageServiceFactory = environment -> {
            switch (environment) {
                case "production":
                    return Optional.of(new EmailMessageService());
                case "development":
                    return Optional.of(new ConsoleMessageService());
                default:
                    return Optional.empty();
            }
        };
        
        messageServiceFactory.apply("production")
            .ifPresent(service -> service.send("Hello from production"));
        
        messageServiceFactory.apply("development")
            .ifPresent(service -> service.send("Hello from development"));
    }
    
    // Helper classes and utility methods
    private static Command createConditionalCommand(Predicate<LocalContext> condition, 
                                                   Command trueCommand, 
                                                   Command falseCommand) {
        return () -> {
            // This would need context passing in real implementation
            return "Conditional command executed";
        };
    }
    
    // Supporting classes
    @FunctionalInterface
    interface Command {
        String execute();
    }
    
    static class CommandProcessor {
        private final Stack<Command> history = new Stack<>();
        
        public void execute(Command command) {
            String result = command.execute();
            history.push(command);
            System.out.println("Executed: " + result);
        }
        
        public String undo() {
            if (!history.isEmpty()) {
                Command lastCommand = history.pop();
                return "Undoing last command";
            }
            return "Nothing to undo";
        }
    }
    
    static class LocalContext {
        final String userRole;
        
        public LocalContext(String userRole) {
            this.userRole = userRole;
        }
    }
    
    static class ContextualCommandProcessor {
        public void execute(Command command, LocalContext context) {
            String result = command.execute();
            System.out.println("Context " + context.userRole + " - " + result);
        }
    }
    
    @FunctionalInterface
    interface PaymentStrategy {
        String processPayment(double amount);
    }
    
    static class PaymentProcessor {
        public void processPayment(double amount, PaymentStrategy strategy) {
            String result = strategy.processPayment(amount);
            System.out.println("Payment: " + result);
        }
    }
    
    static class Person {
        private final String name;
        private final int age;
        
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        
        @Override
        public String toString() {
            return String.format("%s(%d)", name, age);
        }
    }
    
    static class ValidationEngine {
        private final Map<String, Function<String, ValidationResult>> rules = new HashMap<>();
        
        public void addRule(String field, Function<String, ValidationResult> rule) {
            rules.put(field, rule);
        }
        
        public ValidationResult validate(Map<String, String> data) {
            List<String> errors = new ArrayList<>();
            
            data.forEach((field, value) -> {
                Function<String, ValidationResult> rule = rules.get(field);
                if (rule != null) {
                    ValidationResult result = rule.apply(value);
                    if (!result.isValid()) {
                        errors.add(field + ": " + result.getErrorMessage());
                    }
                }
            });
            
            return errors.isEmpty() ? ValidationResult.valid() : 
                   ValidationResult.invalid(String.join(", ", errors));
        }
    }
    
    static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + errorMessage;
        }
    }
    
    static class UserEvent {
        final String username;
        final String action;
        
        public UserEvent(String username, String action) {
            this.username = username;
            this.action = action;
        }
    }
    
    static class EventBus {
        private final Map<String, List<Consumer<UserEvent>>> subscribers = new ConcurrentHashMap<>();
        
        public void subscribe(String eventType, Consumer<UserEvent> handler) {
            subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        }
        
        public void publish(String eventType, UserEvent event) {
            List<Consumer<UserEvent>> handlers = subscribers.get(eventType);
            if (handlers != null) {
                handlers.forEach(handler -> handler.accept(event));
            }
        }
    }
    
    static class ReactiveStream<T> {
        private final List<Consumer<T>> handlers = new ArrayList<>();
        private Function<T, T> transformer = Function.identity();
        private Predicate<T> filter = t -> true;
        
        public ReactiveStream<T> filter(Predicate<T> predicate) {
            this.filter = filter.and(predicate);
            return this;
        }
        
        public ReactiveStream<T> map(Function<T, T> mapper) {
            this.transformer = transformer.andThen(mapper);
            return this;
        }
        
        public void forEach(Consumer<T> handler) {
            handlers.add(handler);
        }
        
        public void emit(T data) {
            if (filter.test(data)) {
                T transformedData = transformer.apply(data);
                handlers.forEach(handler -> handler.accept(transformedData));
            }
        }
    }
    
    static class DataPipeline<T> {
        private final List<Function<T, T>> stages;
        
        private DataPipeline(List<Function<T, T>> stages) {
            this.stages = stages;
        }
        
        public T process(T input) {
            T result = input;
            for (Function<T, T> stage : stages) {
                if (result == null) break;
                result = stage.apply(result);
            }
            return result;
        }
        
        public static <T> Builder<T> builder() {
            return new Builder<>();
        }
        
        static class Builder<T> {
            private final List<Function<T, T>> stages = new ArrayList<>();
            
            public Builder<T> addStage(String name, Function<T, T> stage) {
                stages.add(stage);
                return this;
            }
            
            public DataPipeline<T> build() {
                return new DataPipeline<>(stages);
            }
        }
    }
    
    static class AsyncPipeline<T, R> {
        private final List<Function<Object, CompletableFuture<Object>>> stages;
        
        private AsyncPipeline(List<Function<Object, CompletableFuture<Object>>> stages) {
            this.stages = stages;
        }
        
        @SuppressWarnings("unchecked")
        public CompletableFuture<R> processAsync(T input) {
            CompletableFuture<Object> result = CompletableFuture.completedFuture(input);
            
            for (Function<Object, CompletableFuture<Object>> stage : stages) {
                result = result.thenCompose(stage);
            }
            
            return result.thenApply(obj -> (R) obj);
        }
        
        public static <T, R> Builder<T, R> builder() {
            return new Builder<>();
        }
        
        static class Builder<T, R> {
            private final List<Function<Object, CompletableFuture<Object>>> stages = new ArrayList<>();
            
            @SuppressWarnings("unchecked")
            public <U> Builder<T, R> addAsyncStage(String name, Function<Object, CompletableFuture<U>> stage) {
                stages.add((Function<Object, CompletableFuture<Object>>) stage);
                return this;
            }
            
            public AsyncPipeline<T, R> build() {
                return new AsyncPipeline<>(stages);
            }
        }
    }
    
    static class ErrorHandlingPipeline<T> {
        private final List<StageWithDefault<Object, Object>> stages;
        
        private ErrorHandlingPipeline(List<StageWithDefault<Object, Object>> stages) {
            this.stages = stages;
        }
        
        @SuppressWarnings("unchecked")
        public T process(T input) {
            Object result = input;
            
            for (StageWithDefault<Object, Object> stage : stages) {
                try {
                    result = stage.function.apply(result);
                } catch (Exception e) {
                    result = stage.defaultValue;
                }
            }
            
            return (T) result;
        }
        
        public static <T> Builder<T> builder() {
            return new Builder<>();
        }
        
        static class Builder<T> {
            private final List<StageWithDefault<Object, Object>> stages = new ArrayList<>();
            
            @SuppressWarnings("unchecked")
            public <U> Builder<T> addStage(String name, Function<Object, U> stage, U defaultValue) {
                stages.add(new StageWithDefault<>((Function<Object, Object>) stage, defaultValue));
                return this;
            }
            
            public ErrorHandlingPipeline<T> build() {
                return new ErrorHandlingPipeline<>(stages);
            }
        }
        
        static class StageWithDefault<T, R> {
            final Function<T, R> function;
            final R defaultValue;
            
            StageWithDefault(Function<T, R> function, R defaultValue) {
                this.function = function;
                this.defaultValue = defaultValue;
            }
        }
    }
    
    static class ServiceRegistry {
        private final Map<String, Supplier<Object>> services = new HashMap<>();
        
        public void register(String name, Supplier<Object> serviceFactory) {
            services.put(name, serviceFactory);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T get(String name) {
            Supplier<Object> factory = services.get(name);
            return factory != null ? (T) factory.get() : null;
        }
    }
    
    static class UserService {
        private final Consumer<String> logger;
        private final Function<String, Boolean> validator;
        private final Function<String, String> formatter;
        
        public UserService(Consumer<String> logger, 
                          Function<String, Boolean> validator, 
                          Function<String, String> formatter) {
            this.logger = logger;
            this.validator = validator;
            this.formatter = formatter;
        }
        
        public void processUser(String email) {
            logger.accept("Processing user: " + email);
            
            if (validator.apply(email)) {
                String formatted = formatter.apply(email);
                logger.accept("User processed: " + formatted);
            } else {
                logger.accept("Invalid user email: " + email);
            }
        }
    }
    
    static class Configuration {
        private final Map<String, String> properties = new HashMap<>();
        
        public void set(String key, String value) {
            properties.put(key, value);
        }
        
        public String get(String key) {
            return properties.get(key);
        }
    }
    
    static class DatabaseService {
        private final String host;
        private final String port;
        private final boolean cacheEnabled;
        
        public DatabaseService(String host, String port, boolean cacheEnabled) {
            this.host = host;
            this.port = port;
            this.cacheEnabled = cacheEnabled;
        }
        
        public void connect() {
            System.out.printf("Connected to database at %s:%s (cache: %s)%n", 
                host, port, cacheEnabled);
        }
    }
    
    interface MessageService {
        void send(String message);
    }
    
    static class EmailMessageService implements MessageService {
        @Override
        public void send(String message) {
            System.out.println("📧 Email: " + message);
        }
    }
    
    static class ConsoleMessageService implements MessageService {
        @Override
        public void send(String message) {
            System.out.println("💻 Console: " + message);
        }
    }
}
```

---

## 🎯 Summary

**Congratulations!** You've completed **Day 40: Method References & Functional Composition** and mastered:

- **Method References**: All four types (static, instance, constructor, arbitrary object) with practical applications
- **Function Composition**: Building complex functionality from simple, reusable components
- **Higher-Order Functions**: Functions that create, modify, or operate on other functions
- **Currying & Partial Application**: Advanced functional programming techniques for flexible function creation
- **Functional Design Patterns**: Command, Strategy, Observer patterns implemented with functional approaches
- **Real-World Architectures**: Pipeline processing, dependency injection, and event-driven systems

**Key Takeaways:**
- Method references provide cleaner, more readable code than equivalent lambdas
- Function composition enables building complex logic from simple, testable components
- Higher-order functions allow for flexible, configurable behavior patterns
- Currying and partial application enable function specialization and reuse
- Functional programming patterns often result in more maintainable and testable code
- Immutability and pure functions make concurrent programming safer and more predictable

**Method Reference Guidelines:**
- Use `ClassName::staticMethod` for static method references
- Use `object::instanceMethod` for instance methods on specific objects
- Use `ClassName::new` for constructor references
- Use `ClassName::instanceMethod` for instance methods on arbitrary objects
- Prefer method references over lambdas when they're equivalent for better readability

**Composition Best Practices:**
- Build small, focused functions that do one thing well
- Use `andThen()` for forward composition and `compose()` for reverse composition
- Leverage currying for creating specialized function variants
- Implement error handling through composition rather than exceptions
- Design pipelines for data transformation and processing workflows

**Performance Considerations:**
- Method references can be more efficient than lambdas due to JVM optimizations
- Function composition has minimal overhead for most use cases
- Consider memoization for expensive computations in composed functions
- Be mindful of object creation in curried function chains

**Next Steps:** Tomorrow we'll explore **Modern Java Features (Java 8-17+)** to learn the latest syntax enhancements and API improvements that build upon these functional programming foundations.

**🏆 Day 40 Complete!** You now have advanced functional programming skills and can design elegant, maintainable applications using method references, function composition, and sophisticated functional patterns.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "Create Week 6 directory structure for Advanced Java Features", "status": "completed", "priority": "high", "id": "create-week6-structure"}, {"content": "Create Day 36: Lambda Expressions & Functional Interfaces with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day36"}, {"content": "Create Day 37: Stream API Fundamentals with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day37"}, {"content": "Create Day 38: Advanced Stream Operations with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day38"}, {"content": "Create Day 39: Optional Class & Null Safety with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day39"}, {"content": "Create Day 40: Method References & Functional Composition with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day40"}, {"content": "Create Day 41: Modern Java Features (Java 8-17+) with embedded Java examples", "status": "in_progress", "priority": "high", "id": "create-day41"}, {"content": "Create Day 42: Week 6 Capstone - Functional Data Processing System", "status": "pending", "priority": "high", "id": "create-day42"}]