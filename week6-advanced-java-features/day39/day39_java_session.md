# Day 39: Optional Class & Null Safety (August 17, 2025)

**Date:** August 17, 2025  
**Duration:** 3 hours  
**Focus:** Eliminate null pointer exceptions with Optional and build null-safe applications

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Understand the problems with null references and null pointer exceptions
- ✅ Master Optional creation, chaining, and transformation methods
- ✅ Learn Optional best practices and anti-patterns to avoid
- ✅ Implement null safety in collections and streams
- ✅ Apply defensive programming techniques with Optional
- ✅ Build robust applications that gracefully handle absent values

---

## 🚀 Part 1: Understanding Null Problems & Optional Basics (60 minutes)

### **The Billion Dollar Mistake: Null References**

**Problems with null:**
- **NullPointerException**: Runtime errors that crash applications
- **Defensive checks**: Cluttered code with repetitive null checks
- **Hidden contracts**: Methods may return null without clear indication
- **Composability**: Difficult to chain operations safely

### **Optional as a Solution**

**Optional** is a container that may or may not contain a value:
- **Type safety**: Makes nullability explicit in method signatures
- **Functional style**: Enables safe method chaining
- **Intent clarity**: Communicates that a value might be absent

Create `OptionalBasics.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Comprehensive demonstration of Optional fundamentals and null safety
 */
public class OptionalBasics {
    
    public static void main(String[] args) {
        System.out.println("=== Optional Basics & Null Safety ===\n");
        
        // 1. Problems with null
        System.out.println("1. Problems with Null References:");
        demonstrateNullProblems();
        
        // 2. Optional creation
        System.out.println("\n2. Optional Creation Methods:");
        demonstrateOptionalCreation();
        
        // 3. Optional basic operations
        System.out.println("\n3. Basic Optional Operations:");
        demonstrateBasicOperations();
        
        // 4. Optional chaining
        System.out.println("\n4. Optional Method Chaining:");
        demonstrateOptionalChaining();
        
        System.out.println("\n=== Optional Basics Completed ===");
    }
    
    private static void demonstrateNullProblems() {
        // Traditional approach with null checks
        User user = findUserById(1);  // May return null
        
        // Verbose null checking
        if (user != null) {
            Address address = user.getAddress();
            if (address != null) {
                String city = address.getCity();
                if (city != null) {
                    System.out.println("User lives in: " + city.toUpperCase());
                } else {
                    System.out.println("City not available");
                }
            } else {
                System.out.println("Address not available");
            }
        } else {
            System.out.println("User not found");
        }
        
        // What happens without proper null checks
        try {
            User nullUser = findUserById(999);  // Returns null
            // This will throw NullPointerException!
            String result = nullUser.getAddress().getCity().toUpperCase();
            System.out.println("This won't be reached: " + result);
        } catch (NullPointerException e) {
            System.out.println("💥 NullPointerException caught: " + e.getMessage());
        }
        
        // Problems with collections and null
        List<String> names = Arrays.asList("Alice", null, "Bob", null, "Charlie");
        System.out.println("List with nulls: " + names);
        
        try {
            // This will fail when processing null elements
            List<String> upperNames = names.stream()
                .map(String::toUpperCase)  // NPE on null elements
                .collect(Collectors.toList());
            System.out.println("Upper names: " + upperNames);
        } catch (NullPointerException e) {
            System.out.println("💥 NPE in stream processing: " + e.getMessage());
        }
        
        // Safe processing with null checks
        List<String> safeUpperNames = names.stream()
            .filter(Objects::nonNull)  // Remove nulls
            .map(String::toUpperCase)
            .collect(Collectors.toList());
        System.out.println("Safe upper names: " + safeUpperNames);
    }
    
    private static void demonstrateOptionalCreation() {
        // 1. Optional.empty() - represents absence of value
        Optional<String> emptyOptional = Optional.empty();
        System.out.println("Empty optional: " + emptyOptional);
        System.out.println("Is empty: " + emptyOptional.isEmpty());
        System.out.println("Is present: " + emptyOptional.isPresent());
        
        // 2. Optional.of() - for non-null values (throws NPE if null)
        Optional<String> presentOptional = Optional.of("Hello World");
        System.out.println("Present optional: " + presentOptional);
        System.out.println("Is present: " + presentOptional.isPresent());
        
        try {
            Optional<String> nullOptional = Optional.of(null);  // This will throw NPE
        } catch (NullPointerException e) {
            System.out.println("💥 Optional.of(null) throws NPE");
        }
        
        // 3. Optional.ofNullable() - safe creation from potentially null values
        String nullableValue = null;
        Optional<String> nullableOptional = Optional.ofNullable(nullableValue);
        System.out.println("Nullable optional (null): " + nullableOptional);
        
        String nonNullValue = "Valid value";
        Optional<String> validOptional = Optional.ofNullable(nonNullValue);
        System.out.println("Nullable optional (valid): " + validOptional);
        
        // 4. Converting from traditional null-returning methods
        Optional<User> userOptional = findUserByIdOptional(1);
        System.out.println("User optional: " + userOptional);
        
        Optional<User> missingUserOptional = findUserByIdOptional(999);
        System.out.println("Missing user optional: " + missingUserOptional);
        
        // 5. Optional in method return types
        Optional<String> configValue = getConfigValue("database.url");
        System.out.println("Config value: " + configValue);
        
        Optional<String> missingConfig = getConfigValue("nonexistent.key");
        System.out.println("Missing config: " + missingConfig);
    }
    
    private static void demonstrateBasicOperations() {
        Optional<String> value = Optional.of("Hello World");
        Optional<String> emptyValue = Optional.empty();
        
        // 1. isPresent() and isEmpty()
        System.out.println("Value present: " + value.isPresent());
        System.out.println("Value empty: " + value.isEmpty());
        System.out.println("Empty present: " + emptyValue.isPresent());
        System.out.println("Empty empty: " + emptyValue.isEmpty());
        
        // 2. get() - unsafe, only use when certain value is present
        if (value.isPresent()) {
            String actual = value.get();
            System.out.println("Got value safely: " + actual);
        }
        
        try {
            String missing = emptyValue.get();  // This will throw NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("💥 get() on empty Optional throws exception");
        }
        
        // 3. orElse() - provide default value
        String result1 = value.orElse("Default");
        String result2 = emptyValue.orElse("Default");
        System.out.println("orElse with value: " + result1);
        System.out.println("orElse with empty: " + result2);
        
        // 4. orElseGet() - lazy default value computation
        String result3 = value.orElseGet(() -> generateExpensiveDefault());
        String result4 = emptyValue.orElseGet(() -> generateExpensiveDefault());
        System.out.println("orElseGet with value: " + result3);
        System.out.println("orElseGet with empty: " + result4);
        
        // 5. orElseThrow() - throw exception if empty
        try {
            String result5 = value.orElseThrow();
            System.out.println("orElseThrow with value: " + result5);
            
            String result6 = emptyValue.orElseThrow();  // Throws NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("💥 orElseThrow on empty Optional");
        }
        
        // Custom exception with orElseThrow
        try {
            String result7 = emptyValue.orElseThrow(() -> 
                new IllegalStateException("Value is required but not present"));
        } catch (IllegalStateException e) {
            System.out.println("Custom exception: " + e.getMessage());
        }
        
        // 6. ifPresent() - perform action if value is present
        value.ifPresent(v -> System.out.println("Value is present: " + v));
        emptyValue.ifPresent(v -> System.out.println("This won't be printed"));
        
        // 7. ifPresentOrElse() - perform action if present, else run alternative
        value.ifPresentOrElse(
            v -> System.out.println("Processing value: " + v),
            () -> System.out.println("No value to process")
        );
        
        emptyValue.ifPresentOrElse(
            v -> System.out.println("Processing value: " + v),
            () -> System.out.println("No value to process")
        );
    }
    
    private static void demonstrateOptionalChaining() {
        Optional<User> userOpt = findUserByIdOptional(1);
        
        // Traditional approach with multiple null checks
        System.out.println("Traditional approach:");
        User user = findUserById(1);
        if (user != null) {
            Address address = user.getAddress();
            if (address != null) {
                String city = address.getCity();
                if (city != null) {
                    String upperCity = city.toUpperCase();
                    System.out.println("City: " + upperCity);
                } else {
                    System.out.println("City not available");
                }
            } else {
                System.out.println("Address not available");
            }
        } else {
            System.out.println("User not found");
        }
        
        // Optional chaining approach
        System.out.println("\nOptional chaining approach:");
        String cityResult = userOpt
            .map(User::getAddress)           // Optional<Address>
            .map(Address::getCity)           // Optional<String>
            .map(String::toUpperCase)        // Optional<String>
            .orElse("City not available");
        
        System.out.println("City: " + cityResult);
        
        // More complex chaining example
        System.out.println("\nComplex chaining:");
        Optional<String> formattedAddress = userOpt
            .map(User::getAddress)
            .filter(addr -> addr.getCity() != null && addr.getStreet() != null)
            .map(addr -> String.format("%s, %s %s", 
                addr.getStreet(), addr.getCity(), addr.getZipCode()));
        
        formattedAddress.ifPresentOrElse(
            addr -> System.out.println("Formatted address: " + addr),
            () -> System.out.println("Complete address not available")
        );
        
        // Chaining with filtering
        System.out.println("\nChaining with filtering:");
        userOpt
            .map(User::getEmail)
            .filter(email -> email.contains("@"))
            .filter(email -> email.endsWith(".com"))
            .map(String::toLowerCase)
            .ifPresentOrElse(
                email -> System.out.println("Valid email: " + email),
                () -> System.out.println("No valid email found")
            );
        
        // Multiple Optional composition
        System.out.println("\nMultiple Optional composition:");
        Optional<User> user1 = findUserByIdOptional(1);
        Optional<User> user2 = findUserByIdOptional(2);
        
        // Combine two optionals
        String combinedInfo = user1
            .flatMap(u1 -> user2.map(u2 -> 
                String.format("Users: %s and %s", u1.getName(), u2.getName())))
            .orElse("Cannot combine user information");
        
        System.out.println("Combined info: " + combinedInfo);
    }
    
    private static String generateExpensiveDefault() {
        System.out.println("Generating expensive default...");
        // Simulate expensive computation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Expensive Default Value";
    }
    
    // Helper methods for demonstration
    private static User findUserById(int id) {
        // Simulate database lookup that may return null
        if (id == 1) {
            return new User("Alice", "alice@example.com", 
                new Address("123 Main St", "Springfield", "12345"));
        } else if (id == 2) {
            return new User("Bob", "bob@example.com", 
                new Address("456 Oak Ave", "Riverside", "67890"));
        }
        return null;  // Traditional approach - may return null
    }
    
    private static Optional<User> findUserByIdOptional(int id) {
        // Modern approach - explicitly return Optional
        return Optional.ofNullable(findUserById(id));
    }
    
    private static Optional<String> getConfigValue(String key) {
        Map<String, String> config = Map.of(
            "database.url", "jdbc:postgresql://localhost/mydb",
            "api.timeout", "5000",
            "debug.enabled", "true"
        );
        return Optional.ofNullable(config.get(key));
    }
    
    // Helper classes
    static class User {
        private final String name;
        private final String email;
        private final Address address;
        
        public User(String name, String email, Address address) {
            this.name = name;
            this.email = email;
            this.address = address;
        }
        
        public String getName() { return name; }
        public String getEmail() { return email; }
        public Address getAddress() { return address; }
        
        @Override
        public String toString() {
            return String.format("User{name='%s', email='%s'}", name, email);
        }
    }
    
    static class Address {
        private final String street;
        private final String city;
        private final String zipCode;
        
        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
        
        public String getStreet() { return street; }
        public String getCity() { return city; }
        public String getZipCode() { return zipCode; }
        
        @Override
        public String toString() {
            return String.format("Address{street='%s', city='%s', zipCode='%s'}", 
                street, city, zipCode);
        }
    }
}
```

---

## 🔧 Part 2: Advanced Optional Operations & Patterns (60 minutes)

### **Complex Optional Transformations and Compositions**

Create `AdvancedOptionalOperations.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Advanced Optional operations, patterns, and transformations
 */
public class AdvancedOptionalOperations {
    
    public static void main(String[] args) {
        System.out.println("=== Advanced Optional Operations ===\n");
        
        // 1. Optional transformations
        System.out.println("1. Optional Transformations:");
        demonstrateTransformations();
        
        // 2. Optional composition patterns
        System.out.println("\n2. Optional Composition Patterns:");
        demonstrateComposition();
        
        // 3. Optional with collections
        System.out.println("\n3. Optional with Collections:");
        demonstrateCollectionsIntegration();
        
        // 4. Optional in streams
        System.out.println("\n4. Optional in Streams:");
        demonstrateStreamIntegration();
        
        // 5. Error handling with Optional
        System.out.println("\n5. Error Handling with Optional:");
        demonstrateErrorHandling();
        
        System.out.println("\n=== Advanced Optional Operations Completed ===");
    }
    
    private static void demonstrateTransformations() {
        Optional<String> value = Optional.of("  Hello World  ");
        Optional<String> emptyValue = Optional.empty();
        
        // 1. map() - transform the value if present
        Optional<String> trimmed = value.map(String::trim);
        Optional<Integer> length = value.map(String::trim).map(String::length);
        Optional<String> upperCase = value.map(String::trim).map(String::toUpperCase);
        
        System.out.println("Original: " + value);
        System.out.println("Trimmed: " + trimmed);
        System.out.println("Length: " + length);
        System.out.println("Upper case: " + upperCase);
        
        // map() on empty Optional returns empty
        Optional<String> emptyTrimmed = emptyValue.map(String::trim);
        System.out.println("Empty trimmed: " + emptyTrimmed);
        
        // 2. flatMap() - for operations that return Optional
        Optional<User> userOpt = Optional.of(new User("Alice", "alice@example.com", null));
        
        // Wrong: map() would return Optional<Optional<String>>
        // Optional<Optional<String>> nested = userOpt.map(User::getOptionalEmail);
        
        // Correct: flatMap() flattens the nested Optional
        Optional<String> email = userOpt.flatMap(User::getOptionalEmail);
        System.out.println("User email: " + email);
        
        // Chain flatMap operations
        Optional<String> domain = userOpt
            .flatMap(User::getOptionalEmail)
            .map(e -> e.substring(e.indexOf('@') + 1));
        System.out.println("Email domain: " + domain);
        
        // 3. filter() - conditional presence
        Optional<String> longValue = Optional.of("This is a long string");
        Optional<String> shortValue = Optional.of("Short");
        
        Optional<String> longEnough = longValue.filter(s -> s.length() > 10);
        Optional<String> tooShort = shortValue.filter(s -> s.length() > 10);
        
        System.out.println("Long enough: " + longEnough);
        System.out.println("Too short: " + tooShort);
        
        // Complex filtering chain
        Optional<User> validUser = userOpt
            .filter(user -> user.getName() != null)
            .filter(user -> user.getName().length() > 2)
            .filter(user -> user.getEmail() != null)
            .filter(user -> user.getEmail().contains("@"));
        
        System.out.println("Valid user: " + validUser);
        
        // 4. or() - provide alternative Optional
        Optional<String> primary = Optional.empty();
        Optional<String> secondary = Optional.of("Secondary value");
        Optional<String> tertiary = Optional.of("Tertiary value");
        
        Optional<String> result = primary
            .or(() -> secondary)
            .or(() -> tertiary);
        
        System.out.println("First available: " + result);
        
        // Lazy evaluation with or()
        Optional<String> lazyResult = primary
            .or(() -> {
                System.out.println("Computing secondary...");
                return secondary;
            })
            .or(() -> {
                System.out.println("Computing tertiary (won't be called)...");
                return tertiary;
            });
        
        System.out.println("Lazy result: " + lazyResult);
    }
    
    private static void demonstrateComposition() {
        // 1. Combining multiple Optionals
        Optional<String> firstName = Optional.of("John");
        Optional<String> lastName = Optional.of("Doe");
        Optional<String> emptyName = Optional.empty();
        
        // Combine two Optionals
        Optional<String> fullName = firstName.flatMap(first -> 
            lastName.map(last -> first + " " + last));
        
        System.out.println("Full name: " + fullName);
        
        // Combine with empty Optional
        Optional<String> incompleteName = firstName.flatMap(first -> 
            emptyName.map(last -> first + " " + last));
        
        System.out.println("Incomplete name: " + incompleteName);
        
        // 2. Multiple Optional parameters using utility method
        Optional<String> combinedName = combine(firstName, lastName, 
            (first, last) -> first + " " + last);
        
        System.out.println("Combined name: " + combinedName);
        
        // 3. Conditional composition
        Optional<String> title = Optional.of("Dr.");
        Optional<String> middleName = Optional.empty();
        
        Optional<String> formalName = buildFormalName(title, firstName, middleName, lastName);
        System.out.println("Formal name: " + formalName);
        
        // 4. Optional chaining with multiple branches
        Optional<User> userOpt = Optional.of(new User("Alice", "alice@example.com", 
            new Address("123 Main St", "Springfield", "12345")));
        
        Optional<String> locationInfo = userOpt
            .map(User::getAddress)
            .flatMap(this::getLocationDescription);
        
        System.out.println("Location info: " + locationInfo);
        
        // 5. Complex business logic composition
        Optional<Order> orderOpt = Optional.of(new Order("ORD-001", "alice@example.com", 299.99));
        
        Optional<String> orderSummary = processOrder(orderOpt);
        System.out.println("Order summary: " + orderSummary);
    }
    
    private static void demonstrateCollectionsIntegration() {
        // 1. List of Optionals
        List<Optional<String>> optionalList = Arrays.asList(
            Optional.of("Apple"),
            Optional.empty(),
            Optional.of("Banana"),
            Optional.empty(),
            Optional.of("Cherry")
        );
        
        System.out.println("Original optional list: " + optionalList);
        
        // Extract present values
        List<String> presentValues = optionalList.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        
        System.out.println("Present values: " + presentValues);
        
        // Better approach using flatMap
        List<String> flatMappedValues = optionalList.stream()
            .flatMap(Optional::stream)  // Java 9+
            .collect(Collectors.toList());
        
        System.out.println("Flat mapped values: " + flatMappedValues);
        
        // 2. Optional in collections
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Diana");
        
        // Find first name starting with 'C'
        Optional<String> firstC = names.stream()
            .filter(name -> name.startsWith("C"))
            .findFirst();
        
        System.out.println("First name starting with C: " + firstC);
        
        // Find any name containing 'a'
        Optional<String> anyWithA = names.stream()
            .filter(name -> name.toLowerCase().contains("a"))
            .findAny();
        
        System.out.println("Any name containing 'a': " + anyWithA);
        
        // 3. Optional as Map values
        Map<String, Optional<String>> configMap = new HashMap<>();
        configMap.put("database.url", Optional.of("jdbc:postgresql://localhost/db"));
        configMap.put("api.key", Optional.empty());
        configMap.put("debug.mode", Optional.of("true"));
        
        System.out.println("Config map: " + configMap);
        
        // Safe access to map values
        String dbUrl = configMap.get("database.url")
            .orElse(Optional.of("default-url"))
            .orElse("fallback-url");
        
        System.out.println("Database URL: " + dbUrl);
        
        // 4. Creating Optional collections
        List<User> users = Arrays.asList(
            new User("Alice", "alice@example.com", null),
            new User("Bob", null, new Address("456 Oak", "Riverside", "67890")),
            new User("Charlie", "charlie@example.com", 
                new Address("789 Pine", "Lakeside", "54321"))
        );
        
        // Collect all present emails
        List<String> emails = users.stream()
            .map(User::getEmail)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("User emails: " + emails);
        
        // Collect all present addresses
        List<Address> addresses = users.stream()
            .map(User::getAddress)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("User addresses: " + addresses.size() + " addresses found");
        
        // Group users by presence of email
        Map<Boolean, List<User>> groupedByEmail = users.stream()
            .collect(Collectors.partitioningBy(user -> user.getEmail() != null));
        
        System.out.println("Users with email: " + groupedByEmail.get(true).size());
        System.out.println("Users without email: " + groupedByEmail.get(false).size());
    }
    
    private static void demonstrateStreamIntegration() {
        List<String> data = Arrays.asList("1", "2", "invalid", "4", "5", "not-a-number", "7");
        
        // 1. Safe parsing with Optional
        List<Optional<Integer>> parsedOptionals = data.stream()
            .map(this::safeParseInt)
            .collect(Collectors.toList());
        
        System.out.println("Parsed optionals: " + parsedOptionals);
        
        // 2. Extract only successful parses
        List<Integer> validNumbers = data.stream()
            .map(this::safeParseInt)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        
        System.out.println("Valid numbers: " + validNumbers);
        
        // 3. Using flatMap with Optional streams
        List<Integer> flatMappedNumbers = data.stream()
            .map(this::safeParseInt)
            .flatMap(Optional::stream)  // Java 9+
            .collect(Collectors.toList());
        
        System.out.println("Flat mapped numbers: " + flatMappedNumbers);
        
        // 4. Complex stream processing with Optional
        List<User> users = Arrays.asList(
            new User("Alice", "alice@example.com", 
                new Address("123 Main St", "Springfield", "12345")),
            new User("Bob", "bob@domain.org", null),
            new User("Charlie", null, 
                new Address("789 Pine Ave", "Riverside", "67890"))
        );
        
        // Extract all cities where users live
        List<String> cities = users.stream()
            .map(User::getAddress)
            .filter(Objects::nonNull)
            .map(Address::getCity)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("Cities: " + cities);
        
        // Using Optional for safer processing
        List<String> safeCities = users.stream()
            .map(user -> Optional.ofNullable(user.getAddress())
                .map(Address::getCity)
                .orElse("Unknown"))
            .collect(Collectors.toList());
        
        System.out.println("Safe cities: " + safeCities);
        
        // 5. Reducing with Optional
        Optional<String> longestCity = users.stream()
            .map(User::getAddress)
            .filter(Objects::nonNull)
            .map(Address::getCity)
            .filter(Objects::nonNull)
            .reduce((city1, city2) -> city1.length() > city2.length() ? city1 : city2);
        
        System.out.println("Longest city name: " + longestCity);
        
        // 6. Optional in complex stream operations
        Map<String, Optional<String>> emailDomains = users.stream()
            .collect(Collectors.toMap(
                User::getName,
                user -> Optional.ofNullable(user.getEmail())
                    .filter(email -> email.contains("@"))
                    .map(email -> email.substring(email.indexOf('@') + 1))
            ));
        
        System.out.println("Email domains: " + emailDomains);
    }
    
    private static void demonstrateErrorHandling() {
        // 1. Converting exceptions to Optional
        System.out.println("Exception to Optional conversion:");
        
        List<String> inputs = Arrays.asList("123", "456", "invalid", "789");
        
        List<Optional<Integer>> results = inputs.stream()
            .map(this::safeParseInt)
            .collect(Collectors.toList());
        
        System.out.println("Parse results: " + results);
        
        // 2. Try-catch with Optional
        Optional<String> fileContent = readFileContent("config.properties");
        System.out.println("File content: " + fileContent);
        
        // 3. Optional for resource management
        Optional<DatabaseConnection> dbConnection = getDatabaseConnection();
        dbConnection.ifPresentOrElse(
            conn -> {
                System.out.println("Connected to database: " + conn);
                // Use connection
                conn.close();
            },
            () -> System.out.println("Failed to connect to database")
        );
        
        // 4. Validation with Optional
        String email = "invalid-email";
        Optional<String> validatedEmail = validateEmail(email);
        
        validatedEmail.ifPresentOrElse(
            valid -> System.out.println("Valid email: " + valid),
            () -> System.out.println("Invalid email provided")
        );
        
        // 5. Chained validation
        String userInput = "john.doe@example.com";
        Optional<String> processedInput = validateAndProcessEmail(userInput);
        
        System.out.println("Processed input: " + processedInput);
        
        // 6. Optional for graceful degradation
        Optional<String> primaryService = callPrimaryService();
        Optional<String> fallbackService = callFallbackService();
        
        String result = primaryService
            .or(() -> fallbackService)
            .orElse("Service unavailable");
        
        System.out.println("Service result: " + result);
    }
    
    // Helper methods
    private Optional<Integer> safeParseInt(String str) {
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    
    private Optional<String> getLocationDescription(Address address) {
        if (address == null || address.getCity() == null) {
            return Optional.empty();
        }
        
        String description = "Located in " + address.getCity();
        if (address.getZipCode() != null) {
            description += " (" + address.getZipCode() + ")";
        }
        return Optional.of(description);
    }
    
    private static <T, U, R> Optional<R> combine(Optional<T> opt1, Optional<U> opt2, 
                                                 BinaryOperator<String> combiner) {
        return opt1.flatMap(val1 -> 
            opt2.map(val2 -> 
                (R) combiner.apply(val1.toString(), val2.toString())));
    }
    
    private static Optional<String> buildFormalName(Optional<String> title, 
                                                   Optional<String> firstName,
                                                   Optional<String> middleName, 
                                                   Optional<String> lastName) {
        StringBuilder nameBuilder = new StringBuilder();
        
        title.ifPresent(t -> nameBuilder.append(t).append(" "));
        firstName.ifPresent(f -> nameBuilder.append(f).append(" "));
        middleName.ifPresent(m -> nameBuilder.append(m).append(" "));
        lastName.ifPresent(l -> nameBuilder.append(l));
        
        String result = nameBuilder.toString().trim();
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }
    
    private Optional<String> processOrder(Optional<Order> orderOpt) {
        return orderOpt
            .filter(order -> order.getAmount() > 0)
            .filter(order -> order.getCustomerEmail() != null)
            .map(order -> String.format("Order %s for %s: $%.2f", 
                order.getId(), order.getCustomerEmail(), order.getAmount()));
    }
    
    private Optional<String> readFileContent(String filename) {
        try {
            // Simulate file reading that might fail
            if (filename.equals("config.properties")) {
                return Optional.of("database.url=localhost\napi.timeout=5000");
            } else {
                throw new java.io.IOException("File not found");
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    private Optional<DatabaseConnection> getDatabaseConnection() {
        try {
            // Simulate database connection that might fail
            if (Math.random() > 0.3) {  // 70% success rate
                return Optional.of(new DatabaseConnection("localhost:5432"));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    private Optional<String> validateEmail(String email) {
        if (email != null && email.contains("@") && email.contains(".")) {
            return Optional.of(email.toLowerCase());
        }
        return Optional.empty();
    }
    
    private Optional<String> validateAndProcessEmail(String email) {
        return Optional.ofNullable(email)
            .filter(e -> e != null && !e.trim().isEmpty())
            .map(String::trim)
            .filter(e -> e.contains("@"))
            .filter(e -> e.contains("."))
            .filter(e -> e.length() > 5)
            .map(String::toLowerCase);
    }
    
    private Optional<String> callPrimaryService() {
        // Simulate service call that might fail
        if (Math.random() > 0.7) {  // 30% success rate
            return Optional.of("Primary service response");
        }
        return Optional.empty();
    }
    
    private Optional<String> callFallbackService() {
        // Simulate fallback service with higher success rate
        if (Math.random() > 0.2) {  // 80% success rate
            return Optional.of("Fallback service response");
        }
        return Optional.empty();
    }
    
    // Helper classes
    static class User {
        private final String name;
        private final String email;
        private final Address address;
        
        public User(String name, String email, Address address) {
            this.name = name;
            this.email = email;
            this.address = address;
        }
        
        public String getName() { return name; }
        public String getEmail() { return email; }
        public Address getAddress() { return address; }
        
        public Optional<String> getOptionalEmail() {
            return Optional.ofNullable(email);
        }
        
        @Override
        public String toString() {
            return String.format("User{name='%s', email='%s'}", name, email);
        }
    }
    
    static class Address {
        private final String street;
        private final String city;
        private final String zipCode;
        
        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
        
        public String getStreet() { return street; }
        public String getCity() { return city; }
        public String getZipCode() { return zipCode; }
        
        @Override
        public String toString() {
            return String.format("%s, %s %s", street, city, zipCode);
        }
    }
    
    static class Order {
        private final String id;
        private final String customerEmail;
        private final double amount;
        
        public Order(String id, String customerEmail, double amount) {
            this.id = id;
            this.customerEmail = customerEmail;
            this.amount = amount;
        }
        
        public String getId() { return id; }
        public String getCustomerEmail() { return customerEmail; }
        public double getAmount() { return amount; }
        
        @Override
        public String toString() {
            return String.format("Order{id='%s', customer='%s', amount=%.2f}", 
                id, customerEmail, amount);
        }
    }
    
    static class DatabaseConnection {
        private final String connectionString;
        
        public DatabaseConnection(String connectionString) {
            this.connectionString = connectionString;
        }
        
        public void close() {
            System.out.println("Closing database connection");
        }
        
        @Override
        public String toString() {
            return "DatabaseConnection{" + connectionString + "}";
        }
    }
}
```

---

## 📊 Part 3: Best Practices, Anti-patterns & Real-World Applications (60 minutes)

### **Optional Guidelines and Practical Implementation**

Create `OptionalBestPractices.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Optional best practices, anti-patterns, and real-world applications
 */
public class OptionalBestPractices {
    
    public static void main(String[] args) {
        System.out.println("=== Optional Best Practices ===\n");
        
        // 1. Best practices
        System.out.println("1. Optional Best Practices:");
        demonstrateBestPractices();
        
        // 2. Anti-patterns to avoid
        System.out.println("\n2. Anti-Patterns to Avoid:");
        demonstrateAntiPatterns();
        
        // 3. Performance considerations
        System.out.println("\n3. Performance Considerations:");
        demonstratePerformanceConsiderations();
        
        // 4. Real-world applications
        System.out.println("\n4. Real-World Applications:");
        demonstrateRealWorldApplications();
        
        System.out.println("\n=== Optional Best Practices Completed ===");
    }
    
    private static void demonstrateBestPractices() {
        System.out.println("✅ DO: Use Optional for return types that might be absent");
        
        // Good: Method clearly indicates it might not find a user
        public Optional<User> findUserByEmail(String email) {
            // Database lookup logic
            return email.equals("alice@example.com") ? 
                Optional.of(new User("Alice", email)) : 
                Optional.empty();
        }
        
        Optional<User> user = findUserByEmail("alice@example.com");
        System.out.println("Found user: " + user);
        
        System.out.println("\n✅ DO: Use orElse() for simple default values");
        String name = user.map(User::getName).orElse("Anonymous");
        System.out.println("User name: " + name);
        
        System.out.println("\n✅ DO: Use orElseGet() for expensive computations");
        String expensiveDefault = user.map(User::getName)
            .orElseGet(() -> generateExpensiveDefault());
        System.out.println("With expensive default: " + expensiveDefault);
        
        System.out.println("\n✅ DO: Use ifPresent() for side effects");
        user.ifPresent(u -> System.out.println("Processing user: " + u.getName()));
        
        System.out.println("\n✅ DO: Chain Optional operations");
        Optional<String> emailDomain = user
            .map(User::getEmail)
            .filter(email -> email.contains("@"))
            .map(email -> email.substring(email.indexOf('@') + 1));
        
        System.out.println("Email domain: " + emailDomain);
        
        System.out.println("\n✅ DO: Use filter() for conditional presence");
        Optional<User> verifiedUser = user
            .filter(u -> u.getEmail() != null)
            .filter(u -> u.getEmail().contains("@"));
        
        System.out.println("Verified user: " + verifiedUser);
        
        System.out.println("\n✅ DO: Use flatMap() for nested Optional operations");
        Optional<String> profilePicture = user
            .flatMap(User::getProfilePicture);
        
        System.out.println("Profile picture: " + profilePicture);
        
        System.out.println("\n✅ DO: Use Optional.of() for guaranteed non-null values");
        Optional<String> guaranteedValue = Optional.of("This is never null");
        System.out.println("Guaranteed: " + guaranteedValue);
        
        System.out.println("\n✅ DO: Use Optional.ofNullable() for potentially null values");
        String potentiallyNull = null;
        Optional<String> safe = Optional.ofNullable(potentiallyNull);
        System.out.println("Safe from null: " + safe);
        
        System.out.println("\n✅ DO: Combine Optionals properly");
        Optional<String> firstName = Optional.of("John");
        Optional<String> lastName = Optional.of("Doe");
        
        Optional<String> fullName = firstName.flatMap(first ->
            lastName.map(last -> first + " " + last));
        
        System.out.println("Combined name: " + fullName);
    }
    
    private static void demonstrateAntiPatterns() {
        Optional<User> user = Optional.of(new User("Alice", "alice@example.com"));
        
        System.out.println("❌ DON'T: Use get() without checking isPresent()");
        try {
            Optional<User> emptyUser = Optional.empty();
            User badUser = emptyUser.get();  // This will throw exception!
        } catch (NoSuchElementException e) {
            System.out.println("💥 Exception thrown: " + e.getClass().getSimpleName());
        }
        
        System.out.println("\n❌ DON'T: Use isPresent() and get() together");
        // Bad pattern
        if (user.isPresent()) {
            User u = user.get();
            System.out.println("Bad pattern - user: " + u.getName());
        }
        
        // Good pattern
        user.ifPresent(u -> System.out.println("Good pattern - user: " + u.getName()));
        
        System.out.println("\n❌ DON'T: Use Optional as field type");
        // This is an anti-pattern - Optional should not be used for fields
        class BadUserClass {
            private Optional<String> name;  // Wrong!
            private Optional<String> email; // Wrong!
            
            // Prefer nullable fields with Optional getters
        }
        
        class GoodUserClass {
            private String name;  // Nullable field
            private String email; // Nullable field
            
            public Optional<String> getName() {
                return Optional.ofNullable(name);
            }
            
            public Optional<String> getEmail() {
                return Optional.ofNullable(email);
            }
        }
        
        System.out.println("Use Optional for return types, not fields");
        
        System.out.println("\n❌ DON'T: Use Optional for method parameters");
        // Bad - Don't do this
        public void badMethod(Optional<String> name) {
            name.ifPresent(System.out::println);
        }
        
        // Good - Use overloading or nullable parameters
        public void goodMethod(String name) {
            if (name != null) {
                System.out.println(name);
            }
        }
        
        public void goodMethodOverload() {
            System.out.println("Default behavior");
        }
        
        System.out.println("Use method overloading instead of Optional parameters");
        
        System.out.println("\n❌ DON'T: Use orElse() with expensive operations");
        // Bad - expensive operation always executes
        String badDefault = user.map(User::getName)
            .orElse(generateExpensiveDefault()); // Always called!
        
        // Good - lazy evaluation
        String goodDefault = user.map(User::getName)
            .orElseGet(() -> generateExpensiveDefault()); // Only called if needed
        
        System.out.println("Use orElseGet() for expensive defaults");
        
        System.out.println("\n❌ DON'T: Use Optional for collections");
        // Bad
        Optional<List<String>> badList = Optional.of(Arrays.asList("a", "b", "c"));
        
        // Good - use empty collection instead
        List<String> goodList = Arrays.asList("a", "b", "c"); // Or Collections.emptyList()
        
        System.out.println("Use empty collections instead of Optional<Collection>");
        
        System.out.println("\n❌ DON'T: Create deeply nested Optional chains");
        // Bad - hard to read and maintain
        Optional<String> badNested = user
            .map(User::getAddress)
            .map(addr -> Optional.ofNullable(addr))
            .orElse(Optional.empty())
            .map(Address::getCity);
        
        // Good - clean chain
        Optional<String> goodNested = user
            .map(User::getAddress)
            .map(Address::getCity);
        
        System.out.println("Keep Optional chains clean and readable");
        
        System.out.println("\n❌ DON'T: Serialize Optional");
        // Optional is not Serializable - don't use in DTOs or entities
        System.out.println("Don't use Optional in serializable classes (DTOs, entities)");
    }
    
    private static void demonstratePerformanceConsiderations() {
        int iterations = 1_000_000;
        
        // 1. Optional vs null checking performance
        System.out.println("Performance comparison with " + iterations + " iterations:");
        
        // Traditional null checking
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String value = getValue(i % 100);
            String result = value != null ? value.toUpperCase() : "DEFAULT";
        }
        long traditionalTime = System.nanoTime() - start;
        
        // Optional approach
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Optional<String> value = getOptionalValue(i % 100);
            String result = value.map(String::toUpperCase).orElse("DEFAULT");
        }
        long optionalTime = System.nanoTime() - start;
        
        System.out.printf("Traditional null check: %d ms\n", traditionalTime / 1_000_000);
        System.out.printf("Optional approach: %d ms\n", optionalTime / 1_000_000);
        System.out.printf("Overhead: %.2fx\n", (double) optionalTime / traditionalTime);
        
        // 2. Memory usage considerations
        System.out.println("\nMemory considerations:");
        System.out.println("- Optional creates object wrapper (overhead)");
        System.out.println("- Use Optional for return types, not internal computations");
        System.out.println("- Consider primitive optionals: OptionalInt, OptionalLong, OptionalDouble");
        
        // Primitive optionals example
        OptionalInt primitiveOpt = OptionalInt.of(42);
        OptionalDouble doubleOpt = OptionalDouble.of(3.14);
        
        System.out.println("Primitive optional int: " + primitiveOpt);
        System.out.println("Primitive optional double: " + doubleOpt);
        
        // 3. When to use Optional
        System.out.println("\nWhen to use Optional:");
        System.out.println("✅ Return types that might be absent");
        System.out.println("✅ Complex null-safe operations");
        System.out.println("✅ API design for clarity");
        System.out.println("❌ High-performance inner loops");
        System.out.println("❌ Simple null checks");
        System.out.println("❌ Collection/array elements");
    }
    
    private static void demonstrateRealWorldApplications() {
        // 1. Configuration management
        System.out.println("Configuration Management:");
        ConfigurationManager config = new ConfigurationManager();
        
        String dbUrl = config.getProperty("database.url")
            .orElse("jdbc:h2:mem:testdb");
        
        int timeout = config.getIntProperty("connection.timeout")
            .orElse(5000);
        
        boolean debugMode = config.getBooleanProperty("debug.enabled")
            .orElse(false);
        
        System.out.println("Database URL: " + dbUrl);
        System.out.println("Timeout: " + timeout + "ms");
        System.out.println("Debug mode: " + debugMode);
        
        // 2. Service layer pattern
        System.out.println("\nService Layer Pattern:");
        UserService userService = new UserService();
        
        userService.findUserById(1)
            .map(User::getName)
            .ifPresentOrElse(
                name -> System.out.println("Found user: " + name),
                () -> System.out.println("User not found")
            );
        
        // 3. Data transformation pipeline
        System.out.println("\nData Transformation Pipeline:");
        DataProcessor processor = new DataProcessor();
        
        Optional<ProcessedData> result = processor.processInput("sample-data")
            .flatMap(processor::validate)
            .flatMap(processor::transform)
            .flatMap(processor::enrich);
        
        result.ifPresentOrElse(
            data -> System.out.println("Processed: " + data),
            () -> System.out.println("Processing failed")
        );
        
        // 4. Error handling and recovery
        System.out.println("\nError Handling and Recovery:");
        PaymentService paymentService = new PaymentService();
        
        String paymentResult = paymentService.processPayment("12345", 100.0)
            .or(() -> paymentService.processPaymentWithFallback("12345", 100.0))
            .map(payment -> "Payment successful: " + payment.getId())
            .orElse("Payment failed - please try again later");
        
        System.out.println(paymentResult);
        
        // 5. API response handling
        System.out.println("\nAPI Response Handling:");
        ApiClient apiClient = new ApiClient();
        
        apiClient.fetchUserData("alice")
            .filter(userData -> userData.isValid())
            .map(userData -> userData.toDisplayFormat())
            .ifPresentOrElse(
                display -> System.out.println("User data: " + display),
                () -> System.out.println("Invalid or missing user data")
            );
        
        // 6. Caching with Optional
        System.out.println("\nCaching with Optional:");
        CacheService cache = new CacheService();
        
        String cachedValue = cache.get("user:123")
            .orElseGet(() -> {
                System.out.println("Cache miss - fetching from database");
                String value = "User data from database";
                cache.put("user:123", value);
                return value;
            });
        
        System.out.println("Cached value: " + cachedValue);
    }
    
    // Helper methods
    private String getValue(int key) {
        return key % 10 == 0 ? null : "Value" + key;
    }
    
    private Optional<String> getOptionalValue(int key) {
        return key % 10 == 0 ? Optional.empty() : Optional.of("Value" + key);
    }
    
    private String generateExpensiveDefault() {
        // Simulate expensive computation
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Expensive Default";
    }
    
    // Real-world example classes
    static class ConfigurationManager {
        private final Map<String, String> properties = Map.of(
            "database.url", "jdbc:postgresql://localhost/mydb",
            "connection.timeout", "3000",
            "debug.enabled", "false"
        );
        
        public Optional<String> getProperty(String key) {
            return Optional.ofNullable(properties.get(key));
        }
        
        public OptionalInt getIntProperty(String key) {
            return getProperty(key)
                .map(value -> {
                    try {
                        return OptionalInt.of(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        return OptionalInt.empty();
                    }
                })
                .orElse(OptionalInt.empty());
        }
        
        public Optional<Boolean> getBooleanProperty(String key) {
            return getProperty(key)
                .map(Boolean::parseBoolean);
        }
    }
    
    static class UserService {
        private final List<User> users = Arrays.asList(
            new User("Alice", "alice@example.com"),
            new User("Bob", "bob@example.com")
        );
        
        public Optional<User> findUserById(int id) {
            return id < users.size() ? Optional.of(users.get(id)) : Optional.empty();
        }
        
        public Optional<User> findUserByEmail(String email) {
            return users.stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst();
        }
    }
    
    static class DataProcessor {
        public Optional<RawData> processInput(String input) {
            if (input != null && !input.trim().isEmpty()) {
                return Optional.of(new RawData(input));
            }
            return Optional.empty();
        }
        
        public Optional<ValidData> validate(RawData raw) {
            if (raw.getData().length() > 3) {
                return Optional.of(new ValidData(raw.getData()));
            }
            return Optional.empty();
        }
        
        public Optional<TransformedData> transform(ValidData valid) {
            String transformed = valid.getData().toUpperCase() + "_TRANSFORMED";
            return Optional.of(new TransformedData(transformed));
        }
        
        public Optional<ProcessedData> enrich(TransformedData transformed) {
            String enriched = transformed.getData() + "_ENRICHED";
            return Optional.of(new ProcessedData(enriched));
        }
    }
    
    static class PaymentService {
        public Optional<Payment> processPayment(String accountId, double amount) {
            // Simulate payment processing that might fail
            if (Math.random() > 0.7) {  // 30% success rate
                return Optional.of(new Payment("PAY-" + System.currentTimeMillis(), amount));
            }
            return Optional.empty();
        }
        
        public Optional<Payment> processPaymentWithFallback(String accountId, double amount) {
            // Fallback payment method with higher success rate
            if (Math.random() > 0.3) {  // 70% success rate
                return Optional.of(new Payment("FB-PAY-" + System.currentTimeMillis(), amount));
            }
            return Optional.empty();
        }
    }
    
    static class ApiClient {
        public Optional<UserData> fetchUserData(String username) {
            // Simulate API call that might fail or return invalid data
            if ("alice".equals(username)) {
                return Optional.of(new UserData("Alice", "alice@example.com", true));
            }
            return Optional.empty();
        }
    }
    
    static class CacheService {
        private final Map<String, String> cache = new ConcurrentHashMap<>();
        
        public Optional<String> get(String key) {
            return Optional.ofNullable(cache.get(key));
        }
        
        public void put(String key, String value) {
            cache.put(key, value);
        }
    }
    
    // Helper classes
    static class User {
        private final String name;
        private final String email;
        private final Address address;
        
        public User(String name, String email) {
            this(name, email, null);
        }
        
        public User(String name, String email, Address address) {
            this.name = name;
            this.email = email;
            this.address = address;
        }
        
        public String getName() { return name; }
        public String getEmail() { return email; }
        public Address getAddress() { return address; }
        
        public Optional<String> getProfilePicture() {
            // Simulate optional profile picture
            return name.equals("Alice") ? 
                Optional.of("profile_" + name.toLowerCase() + ".jpg") : 
                Optional.empty();
        }
        
        @Override
        public String toString() {
            return String.format("User{name='%s', email='%s'}", name, email);
        }
    }
    
    static class Address {
        private final String street;
        private final String city;
        private final String zipCode;
        
        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
        
        public String getStreet() { return street; }
        public String getCity() { return city; }
        public String getZipCode() { return zipCode; }
        
        @Override
        public String toString() {
            return String.format("%s, %s %s", street, city, zipCode);
        }
    }
    
    static class RawData {
        private final String data;
        public RawData(String data) { this.data = data; }
        public String getData() { return data; }
    }
    
    static class ValidData {
        private final String data;
        public ValidData(String data) { this.data = data; }
        public String getData() { return data; }
    }
    
    static class TransformedData {
        private final String data;
        public TransformedData(String data) { this.data = data; }
        public String getData() { return data; }
    }
    
    static class ProcessedData {
        private final String data;
        public ProcessedData(String data) { this.data = data; }
        public String getData() { return data; }
        @Override
        public String toString() { return data; }
    }
    
    static class Payment {
        private final String id;
        private final double amount;
        
        public Payment(String id, double amount) {
            this.id = id;
            this.amount = amount;
        }
        
        public String getId() { return id; }
        public double getAmount() { return amount; }
        
        @Override
        public String toString() {
            return String.format("Payment{id='%s', amount=%.2f}", id, amount);
        }
    }
    
    static class UserData {
        private final String name;
        private final String email;
        private final boolean valid;
        
        public UserData(String name, String email, boolean valid) {
            this.name = name;
            this.email = email;
            this.valid = valid;
        }
        
        public String getName() { return name; }
        public String getEmail() { return email; }
        public boolean isValid() { return valid; }
        
        public String toDisplayFormat() {
            return String.format("%s <%s>", name, email);
        }
        
        @Override
        public String toString() {
            return String.format("UserData{name='%s', email='%s', valid=%s}", 
                name, email, valid);
        }
    }
}
```

---

## 🎯 Summary

**Congratulations!** You've completed **Day 39: Optional Class & Null Safety** and mastered:

- **Null Problem Understanding**: Recognizing the billion-dollar mistake of null references
- **Optional Creation**: Using `Optional.of()`, `Optional.empty()`, and `Optional.ofNullable()`
- **Optional Operations**: Transforming, filtering, and extracting values safely
- **Method Chaining**: Building fluent, null-safe operation pipelines
- **Best Practices**: Following Optional guidelines and avoiding common anti-patterns
- **Real-World Applications**: Implementing null safety in configuration, services, and APIs

**Key Takeaways:**
- Optional makes nullability explicit and enables safer method chaining
- Use Optional for return types, not fields or method parameters
- Prefer `orElseGet()` over `orElse()` for expensive default computations
- Chain Optional operations with `map()`, `flatMap()`, and `filter()` for clean code
- Avoid calling `get()` without checking `isPresent()` first
- Optional has performance overhead - use judiciously in high-performance scenarios

**Best Practices Summary:**
- ✅ Use Optional for return types that might be absent
- ✅ Chain operations with map(), flatMap(), filter()
- ✅ Use orElseGet() for expensive default values
- ✅ Use ifPresent() and ifPresentOrElse() for side effects
- ❌ Don't use Optional as field types
- ❌ Don't use Optional for method parameters
- ❌ Don't call get() without checking isPresent()
- ❌ Don't use Optional for collections (use empty collections instead)

**Performance Considerations:**
- Optional creates object wrappers (memory overhead)
- Use primitive optionals (OptionalInt, OptionalLong, OptionalDouble) when appropriate
- Consider traditional null checking for high-performance inner loops
- Optional is best for API design and complex null-safe operations

**Next Steps:** Tomorrow we'll explore **Method References & Functional Composition** to further enhance your functional programming skills with advanced techniques for building modular, reusable code.

**🏆 Day 39 Complete!** You now have the skills to write null-safe, robust Java applications using Optional, eliminating the dreaded NullPointerException and making your code more expressive and maintainable.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "Create Week 6 directory structure for Advanced Java Features", "status": "completed", "priority": "high", "id": "create-week6-structure"}, {"content": "Create Day 36: Lambda Expressions & Functional Interfaces with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day36"}, {"content": "Create Day 37: Stream API Fundamentals with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day37"}, {"content": "Create Day 38: Advanced Stream Operations with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day38"}, {"content": "Create Day 39: Optional Class & Null Safety with embedded Java examples", "status": "completed", "priority": "high", "id": "create-day39"}, {"content": "Create Day 40: Method References & Functional Composition with embedded Java examples", "status": "in_progress", "priority": "high", "id": "create-day40"}, {"content": "Create Day 41: Modern Java Features (Java 8-17+) with embedded Java examples", "status": "pending", "priority": "high", "id": "create-day41"}, {"content": "Create Day 42: Week 6 Capstone - Functional Data Processing System", "status": "pending", "priority": "high", "id": "create-day42"}]