# Day 45: Unit Testing with JUnit 5 & Mockito (August 23, 2025)

**Date:** August 23, 2025  
**Duration:** 2.5 hours (Extended Session)  
**Focus:** Master modern unit testing with JUnit 5 and Mockito for performance-critical applications

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Master JUnit 5 architecture and advanced testing features
- ✅ Implement comprehensive mocking strategies with Mockito
- ✅ Build performance-aware test suites with monitoring integration
- ✅ Create parameterized and dynamic tests for thorough coverage
- ✅ Develop test-driven development workflows for complex systems
- ✅ Integrate testing with memory and performance profiling

---

## 📚 Part 1: JUnit 5 Architecture and Features (45 minutes)

### **JUnit 5 Platform Architecture**

```
┌─────────────────────────────────────────────────────────────────┐
│                    JUNIT 5 PLATFORM                            │
├─────────────────────────────────────────────────────────────────┤
│ JUnit Platform                                                 │
│ ├─ Test Engine API                                             │
│ ├─ Console Launcher                                            │
│ ├─ JUnit Platform Suite Engine                                │
│ └─ Platform Commons                                            │
├─────────────────────────────────────────────────────────────────┤
│ JUnit Jupiter (JUnit 5)        │ JUnit Vintage (JUnit 3&4)    │
│ ├─ Jupiter API                 │ ├─ Vintage Engine            │
│ ├─ Jupiter Engine              │ └─ Legacy Support            │
│ └─ Jupiter Params              │                               │
├─────────────────────────────────────────────────────────────────┤
│ Third-party Test Engines                                       │
│ ├─ Spock Framework                                            │
│ ├─ Cucumber                                                   │
│ └─ TestNG                                                     │
└─────────────────────────────────────────────────────────────────┘
```

### **Key JUnit 5 Improvements**

**Modern Annotations:**
- `@Test` - Basic test method
- `@ParameterizedTest` - Data-driven tests
- `@RepeatedTest` - Repeated execution
- `@TestFactory` - Dynamic test generation
- `@Nested` - Hierarchical test organization
- `@DisplayName` - Custom test names
- `@Tag` - Test categorization
- `@Timeout` - Test execution time limits

---

## 💻 Part 2: Advanced JUnit 5 Testing (40 minutes)

### **Exercise 1: Performance-Aware Test Framework**

Create `PerformanceAwareTestSuite.java`:

```java
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.lang.management.*;

/**
 * Advanced JUnit 5 test suite with performance monitoring integration
 */
@ExtendWith(PerformanceMonitoringExtension.class)
@DisplayName("Performance-Aware Test Suite")
class PerformanceAwareTestSuite {
    
    private static PerformanceMonitor performanceMonitor;
    private Calculator calculator;
    private List<String> testData;
    
    @BeforeAll
    static void setupPerformanceMonitoring() {
        performanceMonitor = new PerformanceMonitor();
        performanceMonitor.startMonitoring(Duration.ofSeconds(1));
        System.out.println("🔄 Performance monitoring started for test suite");
    }
    
    @AfterAll
    static void tearDownPerformanceMonitoring() {
        performanceMonitor.stopMonitoring();
        performanceMonitor.printPerformanceReport();
        System.out.println("📊 Performance monitoring completed");
    }
    
    @BeforeEach
    void setUp() {
        calculator = new Calculator();
        testData = Arrays.asList("test1", "test2", "test3", "test4", "test5");
    }
    
    @AfterEach
    void tearDown() {
        calculator = null;
        testData = null;
        // Force garbage collection between tests to ensure clean state
        System.gc();
    }
    
    /**
     * Basic test with performance assertions
     */
    @Test
    @DisplayName("Basic Calculator Operations with Performance Checks")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testBasicOperationsPerformance() {
        // Performance measurement
        long startTime = System.nanoTime();
        
        // Test operations
        Assertions.assertEquals(10, calculator.add(4, 6));
        Assertions.assertEquals(2, calculator.subtract(8, 6));
        Assertions.assertEquals(15, calculator.multiply(3, 5));
        Assertions.assertEquals(4, calculator.divide(12, 3));
        
        long duration = System.nanoTime() - startTime;
        double durationMs = duration / 1_000_000.0;
        
        // Performance assertion
        Assertions.assertTrue(durationMs < 50, 
            () -> String.format("Operations took too long: %.2f ms", durationMs));
        
        System.out.printf("✅ Basic operations completed in %.2f ms%n", durationMs);
    }
    
    /**
     * Parameterized test with multiple data sets
     */
    @ParameterizedTest(name = "Addition Test: {0} + {1} = {2}")
    @CsvSource({
        "1, 2, 3",
        "5, 7, 12", 
        "10, -5, 5",
        "-3, -7, -10",
        "0, 0, 0",
        "1000000, 2000000, 3000000"
    })
    @DisplayName("Parameterized Addition Tests")
    void testAdditionWithMultipleInputs(int a, int b, int expected) {
        long startTime = System.nanoTime();
        
        int result = calculator.add(a, b);
        
        long duration = System.nanoTime() - startTime;
        
        Assertions.assertEquals(expected, result);
        
        // Performance check for each parameter set
        Assertions.assertTrue(duration < 1_000_000, // 1ms in nanoseconds
            () -> String.format("Addition of %d + %d took too long: %d ns", a, b, duration));
    }
    
    /**
     * Dynamic tests with performance monitoring
     */
    @TestFactory
    @DisplayName("Dynamic Performance Tests")
    Stream<DynamicTest> dynamicPerformanceTests() {
        return Stream.of(
            Arrays.asList(1, 10),
            Arrays.asList(100, 1000), 
            Arrays.asList(1000, 10000),
            Arrays.asList(10000, 100000)
        ).map(range -> {
            int start = range.get(0);
            int end = range.get(1);
            
            return DynamicTest.dynamicTest(
                String.format("Performance test for range [%d, %d]", start, end),
                () -> {
                    long startTime = System.nanoTime();
                    
                    long sum = 0;
                    for (int i = start; i < end; i++) {
                        sum += calculator.add(i, 1);
                    }
                    
                    long duration = System.nanoTime() - startTime;
                    double durationMs = duration / 1_000_000.0;
                    
                    Assertions.assertTrue(sum > 0, "Sum should be positive");
                    
                    // Dynamic performance threshold based on range size
                    int rangeSize = end - start;
                    double expectedMaxTime = rangeSize * 0.01; // 0.01ms per operation
                    
                    Assertions.assertTrue(durationMs < expectedMaxTime,
                        () -> String.format("Range [%d, %d] took %.2f ms, expected < %.2f ms", 
                                           start, end, durationMs, expectedMaxTime));
                    
                    System.out.printf("✅ Range [%d, %d]: %.2f ms%n", start, end, durationMs);
                }
            );
        });
    }
    
    /**
     * Repeated test to check consistency
     */
    @RepeatedTest(value = 10, name = "Memory Allocation Test {currentRepetition}/{totalRepetitions}")
    @DisplayName("Repeated Memory Allocation Tests")
    void testMemoryAllocationConsistency(RepetitionInfo repetitionInfo) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long initialMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        // Allocate some memory
        List<byte[]> allocations = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            allocations.add(new byte[1024]); // 1KB each
        }
        
        long afterAllocation = memoryBean.getHeapMemoryUsage().getUsed();
        long memoryUsed = afterAllocation - initialMemory;
        
        // Clear allocations
        allocations.clear();
        System.gc();
        
        long afterCleanup = memoryBean.getHeapMemoryUsage().getUsed();
        long memoryRecovered = afterAllocation - afterCleanup;
        
        // Assertions
        Assertions.assertTrue(memoryUsed > 0, "Memory should have been allocated");
        
        // Memory recovery should be significant (at least 50% of allocated)
        double recoveryRate = (double) memoryRecovered / memoryUsed;
        Assertions.assertTrue(recoveryRate > 0.5, 
            () -> String.format("Poor memory recovery rate: %.2f%% in repetition %d", 
                               recoveryRate * 100, repetitionInfo.getCurrentRepetition()));
        
        System.out.printf("📊 Repetition %d: Used %d bytes, recovered %.1f%%%n", 
                         repetitionInfo.getCurrentRepetition(), memoryUsed, recoveryRate * 100);
    }
    
    /**
     * Nested test classes for organized testing
     */
    @Nested
    @DisplayName("Advanced Calculator Features")
    class AdvancedCalculatorTests {
        
        @Test
        @DisplayName("Complex Mathematical Operations")
        void testComplexOperations() {
            Assertions.assertTimeout(Duration.ofMillis(200), () -> {
                double result = calculator.power(2, 10);
                Assertions.assertEquals(1024, result, 0.001);
                
                result = calculator.sqrt(144);
                Assertions.assertEquals(12, result, 0.001);
                
                result = calculator.factorial(5);
                Assertions.assertEquals(120, result, 0.001);
            });
        }
        
        @Test
        @DisplayName("Error Handling Performance")
        void testErrorHandlingPerformance() {
            long startTime = System.nanoTime();
            
            Assertions.assertThrows(ArithmeticException.class, () -> {
                calculator.divide(10, 0);
            });
            
            long duration = System.nanoTime() - startTime;
            
            // Exception handling should be fast
            Assertions.assertTrue(duration < 1_000_000, // 1ms
                "Exception handling should be fast");
        }
    }
    
    /**
     * Conditional tests based on system properties
     */
    @Test
    @DisplayName("Performance Test - High Memory Environment")
    @EnabledIf("java.lang.Runtime.getRuntime().maxMemory() > 1024 * 1024 * 1024") // > 1GB
    void testHighMemoryEnvironment() {
        // Test that only runs on systems with sufficient memory
        List<byte[]> largeAllocations = new ArrayList<>();
        
        Assertions.assertTimeout(Duration.ofSeconds(5), () -> {
            for (int i = 0; i < 100; i++) {
                largeAllocations.add(new byte[10 * 1024 * 1024]); // 10MB each
            }
        });
        
        Assertions.assertEquals(100, largeAllocations.size());
        largeAllocations.clear();
    }
    
    /**
     * Tagged tests for categorization
     */
    @Test
    @Tag("performance")
    @Tag("slow")
    @DisplayName("Intensive Performance Test")
    void testIntensivePerformance() {
        Assumptions.assumeTrue(System.getProperty("test.profile", "fast").equals("full"));
        
        long startTime = System.currentTimeMillis();
        
        // CPU-intensive operation
        long sum = 0;
        for (int i = 0; i < 10_000_000; i++) {
            sum += calculator.add(i, 1);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        Assertions.assertTrue(sum > 0);
        System.out.printf("🚀 Intensive test completed in %d ms%n", duration);
    }
    
    /**
     * Custom performance assertion helper
     */
    private void assertPerformance(String operation, Runnable task, long maxDurationMs) {
        long startTime = System.nanoTime();
        task.run();
        long duration = System.nanoTime() - startTime;
        long durationMs = duration / 1_000_000;
        
        Assertions.assertTrue(durationMs <= maxDurationMs,
            () -> String.format("%s took %d ms, expected <= %d ms", operation, durationMs, maxDurationMs));
    }
}

/**
 * Custom JUnit 5 extension for performance monitoring
 */
class PerformanceMonitoringExtension implements BeforeEachCallback, AfterEachCallback {
    private final Map<String, Long> testStartTimes = new ConcurrentHashMap<>();
    
    @Override
    public void beforeEach(ExtensionContext context) {
        String testName = context.getDisplayName();
        testStartTimes.put(testName, System.nanoTime());
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        String testName = context.getDisplayName();
        Long startTime = testStartTimes.remove(testName);
        
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            double durationMs = duration / 1_000_000.0;
            
            System.out.printf("⏱️  Test '%s' took %.2f ms%n", testName, durationMs);
            
            // Log slow tests
            if (durationMs > 1000) { // 1 second
                System.out.printf("⚠️  SLOW TEST: '%s' took %.2f ms%n", testName, durationMs);
            }
        }
    }
}

/**
 * Calculator class for testing
 */
class Calculator {
    
    public int add(int a, int b) {
        return a + b;
    }
    
    public int subtract(int a, int b) {
        return a - b;
    }
    
    public int multiply(int a, int b) {
        return a * b;
    }
    
    public int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a / b;
    }
    
    public double power(double base, int exponent) {
        return Math.pow(base, exponent);
    }
    
    public double sqrt(double number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot calculate square root of negative number");
        }
        return Math.sqrt(number);
    }
    
    public long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        }
        if (n == 0 || n == 1) {
            return 1;
        }
        
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
```

### **Exercise 2: Advanced Mockito Implementation**

Create `MockitoAdvancedTestSuite.java`:

```java
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import java.util.function.*;

/**
 * Advanced Mockito testing with performance monitoring and complex scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Advanced Mockito Test Suite")
class MockitoAdvancedTestSuite {
    
    @Mock
    private DatabaseService databaseService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private MetricsCollector metricsCollector;
    
    @Spy
    private PerformanceTracker performanceTracker = new PerformanceTracker();
    
    @InjectMocks
    private UserService userService;
    
    @Captor
    private ArgumentCaptor<String> stringCaptor;
    
    @Captor
    private ArgumentCaptor<User> userCaptor;
    
    @BeforeEach
    void setUp() {
        // Additional setup if needed
        System.out.println("🔧 Setting up mocks for test");
    }
    
    /**
     * Test with basic mocking and verification
     */
    @Test
    @DisplayName("Basic User Service Operations with Mocking")
    void testBasicUserOperations() {
        // Arrange
        User expectedUser = new User("123", "John Doe", "john@example.com");
        when(databaseService.findUserById("123")).thenReturn(expectedUser);
        when(cacheService.get("user:123")).thenReturn(null);
        
        // Act
        User actualUser = userService.getUserById("123");
        
        // Assert
        assertNotNull(actualUser);
        assertEquals("John Doe", actualUser.getName());
        assertEquals("john@example.com", actualUser.getEmail());
        
        // Verify interactions
        verify(cacheService).get("user:123");
        verify(databaseService).findUserById("123");
        verify(cacheService).put("user:123", expectedUser);
        verify(metricsCollector).recordCacheMiss("user:123");
    }
    
    /**
     * Test with argument matchers and custom matching
     */
    @Test
    @DisplayName("Advanced Argument Matching")
    void testAdvancedArgumentMatching() {
        // Arrange
        User user = new User("456", "Jane Smith", "jane@example.com");
        
        // Using argument matchers
        when(databaseService.findUserById(anyString())).thenReturn(user);
        when(cacheService.get(startsWith("user:"))).thenReturn(null);
        when(databaseService.updateUser(argThat(u -> u.getEmail().contains("@example.com"))))
            .thenReturn(true);
        
        // Act
        User retrievedUser = userService.getUserById("456");
        boolean updateResult = userService.updateUser(user);
        
        // Assert
        assertNotNull(retrievedUser);
        assertTrue(updateResult);
        
        // Verify with argument captors
        verify(databaseService).updateUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("jane@example.com", capturedUser.getEmail());
    }
    
    /**
     * Test with spy objects and partial mocking
     */
    @Test
    @DisplayName("Spy Objects and Partial Mocking")
    void testSpyObjectsAndPartialMocking() {
        // Arrange - spy allows calling real methods and stubbing specific ones
        doReturn(Duration.ofMillis(150)).when(performanceTracker).measureExecutionTime(any());
        
        User user = new User("789", "Bob Wilson", "bob@example.com");
        when(databaseService.findUserById("789")).thenReturn(user);
        
        // Act
        User result = userService.getUserByIdWithPerformanceTracking("789");
        Duration executionTime = performanceTracker.getLastExecutionTime();
        
        // Assert
        assertNotNull(result);
        assertEquals("Bob Wilson", result.getName());
        assertNotNull(executionTime);
        assertEquals(150, executionTime.toMillis());
        
        // Verify spy interaction
        verify(performanceTracker).measureExecutionTime(any());
    }
    
    /**
     * Test with mock responses based on invocation count
     */
    @Test
    @DisplayName("Sequential Mock Responses")
    void testSequentialMockResponses() {
        // Arrange - different responses for consecutive calls
        when(databaseService.findUserById("123"))
            .thenReturn(null)  // First call
            .thenReturn(new User("123", "John Doe", "john@example.com"))  // Second call
            .thenThrow(new DatabaseException("Connection timeout"));  // Third call
        
        // Act & Assert
        // First call - user not found
        User firstCall = userService.getUserById("123");
        assertNull(firstCall);
        
        // Second call - user found
        User secondCall = userService.getUserById("123");
        assertNotNull(secondCall);
        assertEquals("John Doe", secondCall.getName());
        
        // Third call - exception thrown
        assertThrows(DatabaseException.class, () -> {
            userService.getUserById("123");
        });
        
        // Verify call count
        verify(databaseService, times(3)).findUserById("123");
    }
    
    /**
     * Test with timeout and async operations
     */
    @Test
    @DisplayName("Async Operations with Timeout")
    void testAsyncOperationsWithTimeout() {
        // Arrange
        CompletableFuture<User> userFuture = CompletableFuture.completedFuture(
            new User("async123", "Async User", "async@example.com")
        );
        when(databaseService.findUserByIdAsync("async123")).thenReturn(userFuture);
        
        // Act & Assert
        assertTimeout(Duration.ofSeconds(2), () -> {
            CompletableFuture<User> result = userService.getUserByIdAsync("async123");
            User user = result.get();
            
            assertNotNull(user);
            assertEquals("Async User", user.getName());
        });
        
        verify(databaseService).findUserByIdAsync("async123");
    }
    
    /**
     * Test with mock performance verification
     */
    @Test
    @DisplayName("Mock Performance and Interaction Verification")
    void testMockPerformanceVerification() {
        // Arrange
        List<User> users = Arrays.asList(
            new User("1", "User 1", "user1@example.com"),
            new User("2", "User 2", "user2@example.com"),
            new User("3", "User 3", "user3@example.com")
        );
        when(databaseService.findAllUsers()).thenReturn(users);
        
        // Act
        long startTime = System.nanoTime();
        List<User> result = userService.getAllUsers();
        long duration = System.nanoTime() - startTime;
        
        // Assert
        assertEquals(3, result.size());
        assertTrue(duration < 1_000_000, "Mock call should be fast"); // < 1ms
        
        // Verify no unnecessary interactions
        verify(databaseService, only()).findAllUsers();
        verifyNoInteractions(cacheService); // Cache not used for bulk operations
    }
    
    /**
     * Test with custom answer for complex mock behavior
     */
    @Test
    @DisplayName("Custom Answer for Complex Mock Behavior")
    void testCustomAnswerBehavior() {
        // Arrange - custom answer that simulates realistic behavior
        when(databaseService.findUsersByRole(anyString())).thenAnswer(invocation -> {
            String role = invocation.getArgument(0);
            
            // Simulate different response times based on role
            if ("admin".equals(role)) {
                Thread.sleep(100); // Simulate slow admin lookup
                return Arrays.asList(
                    new User("admin1", "Admin User", "admin@example.com")
                );
            } else if ("user".equals(role)) {
                Thread.sleep(50); // Faster user lookup
                return Arrays.asList(
                    new User("user1", "Regular User", "user@example.com"),
                    new User("user2", "Another User", "user2@example.com")
                );
            } else {
                return Collections.emptyList();
            }
        });
        
        // Act & Assert
        long startTime = System.currentTimeMillis();
        List<User> adminUsers = userService.getUsersByRole("admin");
        long adminDuration = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        List<User> regularUsers = userService.getUsersByRole("user");
        long userDuration = System.currentTimeMillis() - startTime;
        
        // Verify results
        assertEquals(1, adminUsers.size());
        assertEquals(2, regularUsers.size());
        
        // Verify performance characteristics
        assertTrue(adminDuration >= 100, "Admin lookup should be slower");
        assertTrue(userDuration >= 50, "User lookup should be moderately fast");
        assertTrue(adminDuration > userDuration, "Admin lookup should be slower than user lookup");
    }
    
    /**
     * Test with verification of specific interaction patterns
     */
    @Test
    @DisplayName("Complex Interaction Pattern Verification")
    void testComplexInteractionPatterns() {
        // Arrange
        User user = new User("pattern123", "Pattern User", "pattern@example.com");
        when(cacheService.get("user:pattern123")).thenReturn(null);
        when(databaseService.findUserById("pattern123")).thenReturn(user);
        
        // Act
        userService.getUserById("pattern123");
        userService.getUserById("pattern123"); // Second call should hit cache
        
        // Reset cache mock to return user for second call
        when(cacheService.get("user:pattern123")).thenReturn(user);
        userService.getUserById("pattern123"); // Third call should use cache
        
        // Assert interaction patterns
        InOrder inOrder = inOrder(cacheService, databaseService);
        
        // First call pattern
        inOrder.verify(cacheService).get("user:pattern123");
        inOrder.verify(databaseService).findUserById("pattern123");
        inOrder.verify(cacheService).put("user:pattern123", user);
        
        // Verify database was called exactly once despite multiple getUserById calls
        verify(databaseService, times(1)).findUserById("pattern123");
        
        // Verify cache was checked multiple times
        verify(cacheService, atLeast(2)).get("user:pattern123");
    }
    
    /**
     * Test with mock reset and state management
     */
    @Test
    @DisplayName("Mock Reset and State Management")
    void testMockResetAndStateManagement() {
        // Initial setup
        when(databaseService.findUserById("reset123")).thenReturn(
            new User("reset123", "Initial User", "initial@example.com")
        );
        
        // First assertion
        User initialUser = userService.getUserById("reset123");
        assertEquals("Initial User", initialUser.getName());
        
        // Reset mock and setup new behavior
        reset(databaseService);
        when(databaseService.findUserById("reset123")).thenReturn(
            new User("reset123", "Updated User", "updated@example.com")
        );
        
        // Second assertion with reset mock
        User updatedUser = userService.getUserById("reset123");
        assertEquals("Updated User", updatedUser.getName());
        
        // Verify reset cleared previous interactions
        verify(databaseService, times(1)).findUserById("reset123");
    }
    
    /**
     * Performance test with mock overhead measurement
     */
    @Test
    @DisplayName("Mock Overhead Performance Test")
    void testMockOverheadPerformance() {
        // Setup mocks
        when(databaseService.findUserById(anyString())).thenReturn(
            new User("perf123", "Performance User", "perf@example.com")
        );
        
        int iterations = 10000;
        
        // Measure mock call performance
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            userService.getUserById("perf" + i);
        }
        long duration = System.nanoTime() - startTime;
        
        double avgTimePerCallNs = (double) duration / iterations;
        double avgTimePerCallMs = avgTimePerCallNs / 1_000_000;
        
        // Assert performance characteristics
        assertTrue(avgTimePerCallMs < 1.0, 
            String.format("Mock calls should be fast: %.3f ms average", avgTimePerCallMs));
        
        System.out.printf("📊 Mock performance: %.3f ms average over %d calls%n", 
                         avgTimePerCallMs, iterations);
        
        // Verify all calls were made
        verify(databaseService, times(iterations)).findUserById(anyString());
    }
}

/**
 * Supporting classes for testing
 */
class User {
    private final String id;
    private final String name;
    private final String email;
    
    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(id, user.id) && 
               Objects.equals(name, user.name) && 
               Objects.equals(email, user.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, email);
    }
    
    @Override
    public String toString() {
        return String.format("User{id='%s', name='%s', email='%s'}", id, name, email);
    }
}

interface DatabaseService {
    User findUserById(String id);
    List<User> findAllUsers();
    List<User> findUsersByRole(String role);
    boolean updateUser(User user);
    CompletableFuture<User> findUserByIdAsync(String id);
}

interface CacheService {
    Object get(String key);
    void put(String key, Object value);
    void evict(String key);
}

interface MetricsCollector {
    void recordCacheHit(String key);
    void recordCacheMiss(String key);
    void recordDatabaseCall(String operation);
}

class PerformanceTracker {
    private Duration lastExecutionTime;
    
    public Duration measureExecutionTime(Supplier<Object> operation) {
        long startTime = System.nanoTime();
        operation.get();
        long duration = System.nanoTime() - startTime;
        lastExecutionTime = Duration.ofNanos(duration);
        return lastExecutionTime;
    }
    
    public Duration getLastExecutionTime() {
        return lastExecutionTime;
    }
}

class UserService {
    private final DatabaseService databaseService;
    private final CacheService cacheService;
    private final MetricsCollector metricsCollector;
    private final PerformanceTracker performanceTracker;
    
    public UserService(DatabaseService databaseService, CacheService cacheService, 
                      MetricsCollector metricsCollector, PerformanceTracker performanceTracker) {
        this.databaseService = databaseService;
        this.cacheService = cacheService;
        this.metricsCollector = metricsCollector;
        this.performanceTracker = performanceTracker;
    }
    
    public User getUserById(String id) {
        String cacheKey = "user:" + id;
        
        // Try cache first
        User cachedUser = (User) cacheService.get(cacheKey);
        if (cachedUser != null) {
            metricsCollector.recordCacheHit(cacheKey);
            return cachedUser;
        }
        
        // Cache miss - go to database
        metricsCollector.recordCacheMiss(cacheKey);
        User user = databaseService.findUserById(id);
        
        if (user != null) {
            cacheService.put(cacheKey, user);
        }
        
        return user;
    }
    
    public User getUserByIdWithPerformanceTracking(String id) {
        return (User) performanceTracker.measureExecutionTime(() -> getUserById(id)).toString();
    }
    
    public List<User> getAllUsers() {
        return databaseService.findAllUsers();
    }
    
    public List<User> getUsersByRole(String role) {
        return databaseService.findUsersByRole(role);
    }
    
    public boolean updateUser(User user) {
        boolean result = databaseService.updateUser(user);
        if (result) {
            // Invalidate cache
            cacheService.evict("user:" + user.getId());
        }
        return result;
    }
    
    public CompletableFuture<User> getUserByIdAsync(String id) {
        return databaseService.findUserByIdAsync(id);
    }
}

class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
}
```

---

## 🔧 Part 3: Test-Driven Development with Performance Focus (45 minutes)

### **Exercise 3: TDD Performance Cache Implementation**

Create `TDDPerformanceCacheTest.java`:

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

/**
 * Test-Driven Development of a high-performance cache with comprehensive testing
 */
@DisplayName("TDD Performance Cache Implementation")
class TDDPerformanceCacheTest {
    
    private PerformanceCache<String, String> cache;
    
    @BeforeEach
    void setUp() {
        cache = new PerformanceCache<>(100, Duration.ofMinutes(5));
    }
    
    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.clear();
        }
    }
    
    /**
     * TDD Step 1: Basic functionality tests
     */
    @Nested
    @DisplayName("Basic Cache Operations")
    class BasicCacheOperations {
        
        @Test
        @DisplayName("Should store and retrieve values")
        void shouldStoreAndRetrieveValues() {
            // Act
            cache.put("key1", "value1");
            String result = cache.get("key1");
            
            // Assert
            assertEquals("value1", result);
        }
        
        @Test
        @DisplayName("Should return null for non-existent keys")
        void shouldReturnNullForNonExistentKeys() {
            // Act
            String result = cache.get("nonexistent");
            
            // Assert
            assertNull(result);
        }
        
        @Test
        @DisplayName("Should overwrite existing values")
        void shouldOverwriteExistingValues() {
            // Arrange
            cache.put("key1", "value1");
            
            // Act
            cache.put("key1", "value2");
            String result = cache.get("key1");
            
            // Assert
            assertEquals("value2", result);
        }
        
        @Test
        @DisplayName("Should remove values")
        void shouldRemoveValues() {
            // Arrange
            cache.put("key1", "value1");
            
            // Act
            cache.remove("key1");
            String result = cache.get("key1");
            
            // Assert
            assertNull(result);
        }
    }
    
    /**
     * TDD Step 2: Size limit enforcement
     */
    @Nested
    @DisplayName("Size Limit Enforcement")
    class SizeLimitEnforcement {
        
        @Test
        @DisplayName("Should enforce maximum size")
        void shouldEnforceMaximumSize() {
            // Arrange
            PerformanceCache<String, String> smallCache = new PerformanceCache<>(2, Duration.ofMinutes(5));
            
            // Act
            smallCache.put("key1", "value1");
            smallCache.put("key2", "value2");
            smallCache.put("key3", "value3"); // Should evict oldest
            
            // Assert
            assertEquals(2, smallCache.size());
            assertNull(smallCache.get("key1")); // Oldest should be evicted
            assertEquals("value2", smallCache.get("key2"));
            assertEquals("value3", smallCache.get("key3"));
        }
        
        @Test
        @DisplayName("Should evict least recently used items")
        void shouldEvictLeastRecentlyUsedItems() {
            // Arrange
            PerformanceCache<String, String> lruCache = new PerformanceCache<>(2, Duration.ofMinutes(5));
            
            // Act
            lruCache.put("key1", "value1");
            lruCache.put("key2", "value2");
            lruCache.get("key1"); // Access key1 to make it recently used
            lruCache.put("key3", "value3"); // Should evict key2
            
            // Assert
            assertEquals(2, lruCache.size());
            assertEquals("value1", lruCache.get("key1"));
            assertNull(lruCache.get("key2")); // Should be evicted
            assertEquals("value3", lruCache.get("key3"));
        }
    }
    
    /**
     * TDD Step 3: TTL (Time To Live) functionality
     */
    @Nested
    @DisplayName("Time To Live Functionality")
    class TimeToLiveFunctionality {
        
        @Test
        @DisplayName("Should expire entries after TTL")
        void shouldExpireEntriesAfterTTL() throws InterruptedException {
            // Arrange
            PerformanceCache<String, String> ttlCache = new PerformanceCache<>(100, Duration.ofMillis(100));
            
            // Act
            ttlCache.put("key1", "value1");
            String beforeExpiry = ttlCache.get("key1");
            
            Thread.sleep(150); // Wait for expiry
            String afterExpiry = ttlCache.get("key1");
            
            // Assert
            assertEquals("value1", beforeExpiry);
            assertNull(afterExpiry);
        }
        
        @Test
        @DisplayName("Should not expire entries before TTL")
        void shouldNotExpireEntriesBeforeTTL() throws InterruptedException {
            // Arrange
            PerformanceCache<String, String> ttlCache = new PerformanceCache<>(100, Duration.ofMillis(200));
            
            // Act
            ttlCache.put("key1", "value1");
            Thread.sleep(100); // Half the TTL
            String result = ttlCache.get("key1");
            
            // Assert
            assertEquals("value1", result);
        }
        
        @Test
        @DisplayName("Should update access time on get")
        void shouldUpdateAccessTimeOnGet() throws InterruptedException {
            // Arrange
            PerformanceCache<String, String> ttlCache = new PerformanceCache<>(100, Duration.ofMillis(150));
            
            // Act
            ttlCache.put("key1", "value1");
            Thread.sleep(100);
            ttlCache.get("key1"); // Should reset TTL
            Thread.sleep(100);
            String result = ttlCache.get("key1");
            
            // Assert
            assertEquals("value1", result); // Should still be valid due to access
        }
    }
    
    /**
     * TDD Step 4: Performance requirements
     */
    @Nested
    @DisplayName("Performance Requirements")
    class PerformanceRequirements {
        
        @Test
        @DisplayName("Should perform single operations in under 1ms")
        void shouldPerformSingleOperationsQuickly() {
            // Act & Assert
            assertTimeout(Duration.ofMillis(1), () -> {
                cache.put("key1", "value1");
            });
            
            assertTimeout(Duration.ofMillis(1), () -> {
                cache.get("key1");
            });
            
            assertTimeout(Duration.ofMillis(1), () -> {
                cache.remove("key1");
            });
        }
        
        @Test
        @DisplayName("Should handle bulk operations efficiently")
        void shouldHandleBulkOperationsEfficiently() {
            // Arrange
            int numOperations = 10000;
            
            // Act & Assert
            assertTimeout(Duration.ofSeconds(1), () -> {
                for (int i = 0; i < numOperations; i++) {
                    cache.put("key" + i, "value" + i);
                }
            });
            
            assertTimeout(Duration.ofSeconds(1), () -> {
                for (int i = 0; i < numOperations; i++) {
                    cache.get("key" + i);
                }
            });
        }
        
        @Test
        @DisplayName("Should maintain performance under concurrent access")
        void shouldMaintainPerformanceUnderConcurrentAccess() throws InterruptedException {
            // Arrange
            int numThreads = 10;
            int operationsPerThread = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger successCount = new AtomicInteger(0);
            
            // Act
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            String key = "thread" + threadId + "_key" + j;
                            String value = "value" + j;
                            
                            cache.put(key, value);
                            String retrieved = cache.get(key);
                            
                            if (value.equals(retrieved)) {
                                successCount.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            long duration = System.currentTimeMillis() - startTime;
            executor.shutdown();
            
            // Assert
            assertTrue(duration < 5000, "Concurrent operations should complete within 5 seconds");
            assertEquals(numThreads * operationsPerThread, successCount.get(), 
                        "All operations should succeed");
            
            System.out.printf("✅ Concurrent test: %d operations in %d ms%n", 
                             numThreads * operationsPerThread, duration);
        }
    }
    
    /**
     * TDD Step 5: Memory efficiency tests
     */
    @Nested
    @DisplayName("Memory Efficiency")
    class MemoryEfficiency {
        
        @Test
        @DisplayName("Should not cause memory leaks")
        void shouldNotCauseMemoryLeaks() {
            // Arrange
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Act - fill cache and let it evict entries
            for (int i = 0; i < 10000; i++) {
                cache.put("key" + i, "value" + i + "_".repeat(100)); // Larger values
            }
            
            // Force garbage collection
            System.gc();
            System.gc();
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Assert - memory increase should be reasonable (cache size limited)
            assertTrue(memoryIncrease < 10 * 1024 * 1024, // Less than 10MB
                      String.format("Memory increase too large: %d bytes", memoryIncrease));
            
            System.out.printf("📊 Memory increase: %d bytes%n", memoryIncrease);
        }
        
        @Test
        @DisplayName("Should cleanup expired entries")
        void shouldCleanupExpiredEntries() throws InterruptedException {
            // Arrange
            PerformanceCache<String, String> ttlCache = new PerformanceCache<>(1000, Duration.ofMillis(50));
            
            // Act
            for (int i = 0; i < 100; i++) {
                ttlCache.put("key" + i, "value" + i);
            }
            
            assertEquals(100, ttlCache.size());
            
            Thread.sleep(100); // Wait for expiry
            
            // Trigger cleanup by accessing
            ttlCache.get("key1");
            
            // Assert
            assertTrue(ttlCache.size() < 100, "Expired entries should be cleaned up");
        }
    }
    
    /**
     * TDD Step 6: Statistics and monitoring
     */
    @Nested
    @DisplayName("Statistics and Monitoring")
    class StatisticsAndMonitoring {
        
        @Test
        @DisplayName("Should track hit and miss statistics")
        void shouldTrackHitAndMissStatistics() {
            // Act
            cache.put("key1", "value1");
            cache.get("key1"); // Hit
            cache.get("key2"); // Miss
            cache.get("key1"); // Hit
            cache.get("key3"); // Miss
            
            CacheStatistics stats = cache.getStatistics();
            
            // Assert
            assertEquals(2, stats.getHitCount());
            assertEquals(2, stats.getMissCount());
            assertEquals(0.5, stats.getHitRate(), 0.01);
        }
        
        @Test
        @DisplayName("Should track eviction statistics")
        void shouldTrackEvictionStatistics() {
            // Arrange
            PerformanceCache<String, String> smallCache = new PerformanceCache<>(2, Duration.ofMinutes(5));
            
            // Act
            smallCache.put("key1", "value1");
            smallCache.put("key2", "value2");
            smallCache.put("key3", "value3"); // Causes eviction
            smallCache.put("key4", "value4"); // Causes eviction
            
            CacheStatistics stats = smallCache.getStatistics();
            
            // Assert
            assertEquals(2, stats.getEvictionCount());
        }
    }
}

/**
 * Performance Cache implementation (created through TDD)
 */
class PerformanceCache<K, V> {
    private final int maxSize;
    private final Duration ttl;
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final ConcurrentLinkedQueue<K> accessOrder;
    private final CacheStatistics statistics;
    
    public PerformanceCache(int maxSize, Duration ttl) {
        this.maxSize = maxSize;
        this.ttl = ttl;
        this.cache = new ConcurrentHashMap<>();
        this.accessOrder = new ConcurrentLinkedQueue<>();
        this.statistics = new CacheStatistics();
    }
    
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        
        if (entry == null) {
            statistics.recordMiss();
            return null;
        }
        
        if (isExpired(entry)) {
            cache.remove(key);
            accessOrder.remove(key);
            statistics.recordMiss();
            return null;
        }
        
        // Update access time
        entry.lastAccessed = System.currentTimeMillis();
        
        // Update access order
        accessOrder.remove(key);
        accessOrder.offer(key);
        
        statistics.recordHit();
        return entry.value;
    }
    
    public void put(K key, V value) {
        // Remove if already exists
        if (cache.containsKey(key)) {
            accessOrder.remove(key);
        }
        
        // Evict if necessary
        while (cache.size() >= maxSize) {
            evictOldest();
        }
        
        // Add new entry
        CacheEntry<V> entry = new CacheEntry<>(value, System.currentTimeMillis());
        cache.put(key, entry);
        accessOrder.offer(key);
    }
    
    public void remove(K key) {
        cache.remove(key);
        accessOrder.remove(key);
    }
    
    public void clear() {
        cache.clear();
        accessOrder.clear();
        statistics.reset();
    }
    
    public int size() {
        cleanupExpired();
        return cache.size();
    }
    
    public CacheStatistics getStatistics() {
        return statistics;
    }
    
    private boolean isExpired(CacheEntry<V> entry) {
        return System.currentTimeMillis() - entry.lastAccessed > ttl.toMillis();
    }
    
    private void evictOldest() {
        K oldestKey = accessOrder.poll();
        if (oldestKey != null) {
            cache.remove(oldestKey);
            statistics.recordEviction();
        }
    }
    
    private void cleanupExpired() {
        cache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
    
    private static class CacheEntry<V> {
        final V value;
        volatile long lastAccessed;
        
        CacheEntry(V value, long lastAccessed) {
            this.value = value;
            this.lastAccessed = lastAccessed;
        }
    }
}

/**
 * Cache statistics tracking
 */
class CacheStatistics {
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    public void recordHit() {
        hitCount.incrementAndGet();
    }
    
    public void recordMiss() {
        missCount.incrementAndGet();
    }
    
    public void recordEviction() {
        evictionCount.incrementAndGet();
    }
    
    public long getHitCount() {
        return hitCount.get();
    }
    
    public long getMissCount() {
        return missCount.get();
    }
    
    public long getEvictionCount() {
        return evictionCount.get();
    }
    
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        return total == 0 ? 0.0 : (double) hitCount.get() / total;
    }
    
    public void reset() {
        hitCount.set(0);
        missCount.set(0);
        evictionCount.set(0);
    }
    
    @Override
    public String toString() {
        return String.format("CacheStatistics{hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d}",
                           getHitCount(), getMissCount(), getHitRate() * 100, getEvictionCount());
    }
}
```

---

## 🎯 Today's Challenges

### **Challenge 1: Performance Test Framework**

Create `PerformanceTestFramework.java`:

```java
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import java.lang.annotation.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.*;

/**
 * Custom performance testing framework built on JUnit 5
 */
@DisplayName("Performance Test Framework")
class PerformanceTestFramework {
    
    /**
     * Custom annotation for performance tests
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Test
    @ExtendWith(PerformanceTestExtension.class)
    public @interface PerformanceTest {
        long maxExecutionTimeMs() default 1000;
        long maxMemoryUsageMB() default 100;
        int warmupIterations() default 5;
        int measurementIterations() default 10;
        double maxStandardDeviation() default 0.2;
    }
    
    /**
     * Benchmark test annotation
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Test
    @ExtendWith(BenchmarkTestExtension.class)
    public @interface BenchmarkTest {
        int iterations() default 1000;
        TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
        boolean measureMemory() default true;
        boolean measureGC() default true;
    }
    
    /**
     * Test class demonstrating the framework
     */
    @Nested
    @DisplayName("Performance Test Examples")
    class PerformanceTestExamples {
        
        @PerformanceTest(maxExecutionTimeMs = 100, maxMemoryUsageMB = 10)
        @DisplayName("String Operations Performance")
        void testStringOperationsPerformance() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("test").append(i);
            }
            String result = sb.toString();
            Assertions.assertTrue(result.length() > 0);
        }
        
        @PerformanceTest(
            maxExecutionTimeMs = 500, 
            maxMemoryUsageMB = 50,
            warmupIterations = 3,
            measurementIterations = 5,
            maxStandardDeviation = 0.1
        )
        @DisplayName("Collection Operations Performance")
        void testCollectionOperationsPerformance() {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                list.add(i);
            }
            
            Collections.sort(list);
            Assertions.assertEquals(100000, list.size());
        }
        
        @BenchmarkTest(iterations = 5000, measureMemory = true, measureGC = true)
        @DisplayName("Algorithm Benchmark")
        void benchmarkSortingAlgorithm() {
            int[] array = generateRandomArray(1000);
            quickSort(array, 0, array.length - 1);
            Assertions.assertTrue(isSorted(array));
        }
        
        @PerformanceTest(maxExecutionTimeMs = 2000, maxMemoryUsageMB = 200)
        @DisplayName("Concurrent Operations Performance")
        void testConcurrentOperationsPerformance() throws InterruptedException {
            int numThreads = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
            
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 10000; j++) {
                            map.put("key" + threadId + "_" + j, j);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            Assertions.assertEquals(numThreads * 10000, map.size());
        }
        
        // Helper methods
        private int[] generateRandomArray(int size) {
            Random random = new Random();
            int[] array = new int[size];
            for (int i = 0; i < size; i++) {
                array[i] = random.nextInt(10000);
            }
            return array;
        }
        
        private void quickSort(int[] arr, int low, int high) {
            if (low < high) {
                int pi = partition(arr, low, high);
                quickSort(arr, low, pi - 1);
                quickSort(arr, pi + 1, high);
            }
        }
        
        private int partition(int[] arr, int low, int high) {
            int pivot = arr[high];
            int i = (low - 1);
            
            for (int j = low; j < high; j++) {
                if (arr[j] <= pivot) {
                    i++;
                    int temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
            }
            
            int temp = arr[i + 1];
            arr[i + 1] = arr[high];
            arr[high] = temp;
            
            return i + 1;
        }
        
        private boolean isSorted(int[] array) {
            for (int i = 1; i < array.length; i++) {
                if (array[i] < array[i - 1]) {
                    return false;
                }
            }
            return true;
        }
    }
}

/**
 * Performance test extension
 */
class PerformanceTestExtension implements BeforeEachCallback, AfterEachCallback {
    private final Map<String, PerformanceMetrics> testMetrics = new ConcurrentHashMap<>();
    
    @Override
    public void beforeEach(ExtensionContext context) {
        String testName = context.getDisplayName();
        PerformanceMetrics metrics = new PerformanceMetrics();
        testMetrics.put(testName, metrics);
        
        // Get performance test annotation
        PerformanceTestFramework.PerformanceTest annotation = 
            context.getRequiredTestMethod().getAnnotation(PerformanceTestFramework.PerformanceTest.class);
        
        if (annotation != null) {
            // Perform warmup iterations
            performWarmup(context, annotation.warmupIterations());
            
            // Start performance measurement
            metrics.startMeasurement();
        }
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        String testName = context.getDisplayName();
        PerformanceMetrics metrics = testMetrics.get(testName);
        
        if (metrics != null) {
            metrics.stopMeasurement();
            
            PerformanceTestFramework.PerformanceTest annotation = 
                context.getRequiredTestMethod().getAnnotation(PerformanceTestFramework.PerformanceTest.class);
            
            if (annotation != null) {
                validatePerformanceConstraints(metrics, annotation);
                printPerformanceReport(testName, metrics);
            }
        }
    }
    
    private void performWarmup(ExtensionContext context, int warmupIterations) {
        System.out.printf("🔥 Warming up %s (%d iterations)...%n", 
                         context.getDisplayName(), warmupIterations);
        
        // Note: In a real implementation, we would invoke the test method
        // This is a simplified version for demonstration
        for (int i = 0; i < warmupIterations; i++) {
            System.gc(); // Encourage GC between warmup iterations
        }
    }
    
    private void validatePerformanceConstraints(PerformanceMetrics metrics, 
                                               PerformanceTestFramework.PerformanceTest annotation) {
        // Validate execution time
        if (metrics.getExecutionTimeMs() > annotation.maxExecutionTimeMs()) {
            throw new AssertionError(String.format(
                "Execution time %d ms exceeded maximum %d ms",
                metrics.getExecutionTimeMs(), annotation.maxExecutionTimeMs()));
        }
        
        // Validate memory usage
        if (metrics.getMemoryUsageMB() > annotation.maxMemoryUsageMB()) {
            throw new AssertionError(String.format(
                "Memory usage %.2f MB exceeded maximum %d MB",
                metrics.getMemoryUsageMB(), annotation.maxMemoryUsageMB()));
        }
    }
    
    private void printPerformanceReport(String testName, PerformanceMetrics metrics) {
        System.out.println("\n📊 Performance Report: " + testName);
        System.out.printf("   Execution time: %d ms%n", metrics.getExecutionTimeMs());
        System.out.printf("   Memory usage: %.2f MB%n", metrics.getMemoryUsageMB());
        System.out.printf("   GC collections: %d%n", metrics.getGcCollections());
        System.out.printf("   GC time: %d ms%n", metrics.getGcTimeMs());
    }
}

/**
 * Benchmark test extension
 */
class BenchmarkTestExtension implements BeforeEachCallback, AfterEachCallback {
    private final Map<String, BenchmarkResults> benchmarkResults = new ConcurrentHashMap<>();
    
    @Override
    public void beforeEach(ExtensionContext context) {
        String testName = context.getDisplayName();
        PerformanceTestFramework.BenchmarkTest annotation = 
            context.getRequiredTestMethod().getAnnotation(PerformanceTestFramework.BenchmarkTest.class);
        
        if (annotation != null) {
            BenchmarkResults results = new BenchmarkResults(annotation.iterations());
            benchmarkResults.put(testName, results);
            results.startBenchmark();
        }
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        String testName = context.getDisplayName();
        BenchmarkResults results = benchmarkResults.get(testName);
        
        if (results != null) {
            results.stopBenchmark();
            printBenchmarkReport(testName, results);
        }
    }
    
    private void printBenchmarkReport(String testName, BenchmarkResults results) {
        System.out.println("\n🏁 Benchmark Report: " + testName);
        System.out.printf("   Iterations: %d%n", results.getIterations());
        System.out.printf("   Total time: %d ms%n", results.getTotalTimeMs());
        System.out.printf("   Average time per iteration: %.3f ms%n", results.getAverageTimePerIteration());
        System.out.printf("   Throughput: %.0f ops/sec%n", results.getThroughput());
        
        if (results.isMemoryMeasured()) {
            System.out.printf("   Memory usage: %.2f MB%n", results.getMemoryUsageMB());
        }
        
        if (results.isGcMeasured()) {
            System.out.printf("   GC collections: %d%n", results.getGcCollections());
            System.out.printf("   GC time: %d ms%n", results.getGcTimeMs());
        }
    }
}

/**
 * Performance metrics collection
 */
class PerformanceMetrics {
    private long startTime;
    private long endTime;
    private long startMemory;
    private long endMemory;
    private long startGcCollections;
    private long endGcCollections;
    private long startGcTime;
    private long endGcTime;
    
    public void startMeasurement() {
        // Force GC before measurement
        System.gc();
        
        this.startTime = System.currentTimeMillis();
        this.startMemory = getCurrentMemoryUsage();
        this.startGcCollections = getTotalGcCollections();
        this.startGcTime = getTotalGcTime();
    }
    
    public void stopMeasurement() {
        this.endTime = System.currentTimeMillis();
        this.endMemory = getCurrentMemoryUsage();
        this.endGcCollections = getTotalGcCollections();
        this.endGcTime = getTotalGcTime();
    }
    
    public long getExecutionTimeMs() {
        return endTime - startTime;
    }
    
    public double getMemoryUsageMB() {
        return Math.max(0, endMemory - startMemory) / (1024.0 * 1024.0);
    }
    
    public long getGcCollections() {
        return endGcCollections - startGcCollections;
    }
    
    public long getGcTimeMs() {
        return endGcTime - startGcTime;
    }
    
    private long getCurrentMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getUsed();
    }
    
    private long getTotalGcCollections() {
        return ManagementFactory.getGarbageCollectorMXBeans()
                               .stream()
                               .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                               .sum();
    }
    
    private long getTotalGcTime() {
        return ManagementFactory.getGarbageCollectorMXBeans()
                               .stream()
                               .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                               .sum();
    }
}

/**
 * Benchmark results collection
 */
class BenchmarkResults {
    private final int iterations;
    private long startTime;
    private long endTime;
    private long startMemory;
    private long endMemory;
    private long startGcCollections;
    private long endGcCollections;
    private long startGcTime;
    private long endGcTime;
    private boolean memoryMeasured = true;
    private boolean gcMeasured = true;
    
    public BenchmarkResults(int iterations) {
        this.iterations = iterations;
    }
    
    public void startBenchmark() {
        System.gc();
        this.startTime = System.currentTimeMillis();
        
        if (memoryMeasured) {
            this.startMemory = getCurrentMemoryUsage();
        }
        
        if (gcMeasured) {
            this.startGcCollections = getTotalGcCollections();
            this.startGcTime = getTotalGcTime();
        }
    }
    
    public void stopBenchmark() {
        this.endTime = System.currentTimeMillis();
        
        if (memoryMeasured) {
            this.endMemory = getCurrentMemoryUsage();
        }
        
        if (gcMeasured) {
            this.endGcCollections = getTotalGcCollections();
            this.endGcTime = getTotalGcTime();
        }
    }
    
    public int getIterations() {
        return iterations;
    }
    
    public long getTotalTimeMs() {
        return endTime - startTime;
    }
    
    public double getAverageTimePerIteration() {
        return (double) getTotalTimeMs() / iterations;
    }
    
    public double getThroughput() {
        return iterations * 1000.0 / getTotalTimeMs();
    }
    
    public double getMemoryUsageMB() {
        return Math.max(0, endMemory - startMemory) / (1024.0 * 1024.0);
    }
    
    public long getGcCollections() {
        return endGcCollections - startGcCollections;
    }
    
    public long getGcTimeMs() {
        return endGcTime - startGcTime;
    }
    
    public boolean isMemoryMeasured() {
        return memoryMeasured;
    }
    
    public boolean isGcMeasured() {
        return gcMeasured;
    }
    
    private long getCurrentMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getUsed();
    }
    
    private long getTotalGcCollections() {
        return ManagementFactory.getGarbageCollectorMXBeans()
                               .stream()
                               .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                               .sum();
    }
    
    private long getTotalGcTime() {
        return ManagementFactory.getGarbageCollectorMXBeans()
                               .stream()
                               .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                               .sum();
    }
}
```

### **Challenge 2: Integration Testing Prerequisites**

Create `TestDataBuilder.java`:

```java
import java.util.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Builder pattern for creating test data with performance considerations
 */
public class TestDataBuilder {
    
    /**
     * User test data builder
     */
    public static class UserBuilder {
        private static final AtomicInteger idCounter = new AtomicInteger(1);
        
        private String id;
        private String name;
        private String email;
        private LocalDateTime createdAt;
        private UserRole role;
        private boolean active;
        private Map<String, Object> metadata;
        
        public UserBuilder() {
            this.id = "user_" + idCounter.getAndIncrement();
            this.name = "Test User " + id;
            this.email = "test" + id + "@example.com";
            this.createdAt = LocalDateTime.now();
            this.role = UserRole.USER;
            this.active = true;
            this.metadata = new HashMap<>();
        }
        
        public UserBuilder withId(String id) {
            this.id = id;
            return this;
        }
        
        public UserBuilder withName(String name) {
            this.name = name;
            return this;
        }
        
        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }
        
        public UserBuilder withRole(UserRole role) {
            this.role = role;
            return this;
        }
        
        public UserBuilder withCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public UserBuilder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public UserBuilder withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public User build() {
            return new User(id, name, email, createdAt, role, active, new HashMap<>(metadata));
        }
        
        public List<User> buildList(int count) {
            List<User> users = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                users.add(build());
            }
            return users;
        }
        
        public List<User> buildListWithSupplier(int count, Supplier<UserBuilder> builderSupplier) {
            List<User> users = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                users.add(builderSupplier.get().build());
            }
            return users;
        }
    }
    
    /**
     * Performance test data generator
     */
    public static class PerformanceDataGenerator {
        
        public static List<String> generateRandomStrings(int count, int length) {
            List<String> strings = new ArrayList<>();
            Random random = new Random();
            
            for (int i = 0; i < count; i++) {
                StringBuilder sb = new StringBuilder(length);
                for (int j = 0; j < length; j++) {
                    sb.append((char) ('a' + random.nextInt(26)));
                }
                strings.add(sb.toString());
            }
            
            return strings;
        }
        
        public static int[] generateRandomIntegers(int count, int min, int max) {
            Random random = new Random();
            int[] integers = new int[count];
            
            for (int i = 0; i < count; i++) {
                integers[i] = random.nextInt(max - min + 1) + min;
            }
            
            return integers;
        }
        
        public static Map<String, Object> generateLargeDataStructure(int size) {
            Map<String, Object> data = new HashMap<>();
            Random random = new Random();
            
            for (int i = 0; i < size; i++) {
                String key = "key_" + i;
                Object value;
                
                switch (i % 4) {
                    case 0:
                        value = "string_value_" + i;
                        break;
                    case 1:
                        value = random.nextInt(10000);
                        break;
                    case 2:
                        value = random.nextDouble() * 1000;
                        break;
                    default:
                        value = Arrays.asList("item1", "item2", "item3");
                        break;
                }
                
                data.put(key, value);
            }
            
            return data;
        }
    }
    
    /**
     * Memory-efficient test data factory
     */
    public static class MemoryEfficientDataFactory {
        private static final Object[][] SAMPLE_DATA = {
            {"John", "Doe", "john.doe@example.com"},
            {"Jane", "Smith", "jane.smith@example.com"},
            {"Bob", "Johnson", "bob.johnson@example.com"},
            {"Alice", "Williams", "alice.williams@example.com"},
            {"Charlie", "Brown", "charlie.brown@example.com"}
        };
        
        public static User createSampleUser(int index) {
            Object[] data = SAMPLE_DATA[index % SAMPLE_DATA.length];
            
            return new UserBuilder()
                .withName(data[0] + " " + data[1])
                .withEmail((String) data[2])
                .withId("sample_" + index)
                .build();
        }
        
        public static List<User> createSampleUsers(int count) {
            List<User> users = new ArrayList<>();
            
            for (int i = 0; i < count; i++) {
                users.add(createSampleUser(i));
            }
            
            return users;
        }
        
        public static Iterator<User> createUserIterator(int count) {
            return new Iterator<User>() {
                private int current = 0;
                
                @Override
                public boolean hasNext() {
                    return current < count;
                }
                
                @Override
                public User next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return createSampleUser(current++);
                }
            };
        }
    }
    
    // Supporting classes
    public enum UserRole {
        ADMIN, MANAGER, USER, GUEST
    }
    
    public static class User {
        private final String id;
        private final String name;
        private final String email;
        private final LocalDateTime createdAt;
        private final UserRole role;
        private final boolean active;
        private final Map<String, Object> metadata;
        
        public User(String id, String name, String email, LocalDateTime createdAt,
                   UserRole role, boolean active, Map<String, Object> metadata) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.createdAt = createdAt;
            this.role = role;
            this.active = active;
            this.metadata = metadata;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public UserRole getRole() { return role; }
        public boolean isActive() { return active; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            User user = (User) obj;
            return Objects.equals(id, user.id);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
        
        @Override
        public String toString() {
            return String.format("User{id='%s', name='%s', email='%s', role=%s, active=%s}",
                               id, name, email, role, active);
        }
    }
}
```

---

## 📝 Key Takeaways from Day 45

### **JUnit 5 Mastery:**
- ✅ **Platform Architecture**: Understanding JUnit 5's modular design and capabilities
- ✅ **Modern Annotations**: @ParameterizedTest, @RepeatedTest, @TestFactory, @Nested
- ✅ **Dynamic Tests**: Creating tests programmatically with complex logic
- ✅ **Extensions**: Building custom test extensions for cross-cutting concerns
- ✅ **Performance Integration**: Combining testing with performance monitoring

### **Advanced Mockito Techniques:**
- ✅ **Mock Lifecycle**: Setup, configuration, verification, and cleanup patterns
- ✅ **Argument Matchers**: Complex matching strategies and custom matchers
- ✅ **Spy Objects**: Partial mocking for integration with real implementations
- ✅ **Sequential Responses**: Configuring different responses for repeated calls
- ✅ **Async Testing**: Testing asynchronous operations with CompletableFuture

### **Test-Driven Development:**
- ✅ **TDD Workflow**: Red-Green-Refactor cycle with performance considerations
- ✅ **Performance-First Testing**: Writing tests that enforce performance requirements
- ✅ **Test Organization**: Nested test classes and logical test grouping
- ✅ **Custom Assertions**: Building domain-specific assertion helpers
- ✅ **Test Data Management**: Efficient test data creation and cleanup

---

## 🚀 Tomorrow's Preview (Day 46 - August 24)

**Focus:** Integration Testing & Test Containers
- Docker-based integration testing with TestContainers
- Database and service integration testing patterns
- End-to-end testing pipelines with performance monitoring
- Microservice testing strategies and service virtualization
- Production-like testing environments

**Preparation:**
- Ensure Docker is installed and running on your system
- Review today's testing patterns and performance integration
- Think about integration scenarios for complex systems

---

## ✅ Day 45 Checklist

**Completed Learning Objectives:**
- [ ] Mastered JUnit 5 architecture and advanced features
- [ ] Implemented comprehensive Mockito testing strategies
- [ ] Built performance-aware test suites with monitoring
- [ ] Created parameterized and dynamic tests for thorough coverage
- [ ] Developed TDD workflows for complex systems
- [ ] Integrated testing with memory and performance profiling
- [ ] Built custom testing frameworks and extensions
- [ ] Practiced modern testing patterns and best practices

**Bonus Achievements:**
- [ ] Extended testing framework with custom metrics
- [ ] Implemented automated performance regression detection
- [ ] Created test data generation strategies for large datasets
- [ ] Explored advanced mocking scenarios and edge cases

---

## 🎉 Excellent Testing Foundation!

You've mastered modern unit testing with JUnit 5 and Mockito, building production-grade test suites with performance integration. Tomorrow we'll expand to integration testing with real external dependencies using TestContainers.

**Your testing expertise is reaching professional standards!** 🚀