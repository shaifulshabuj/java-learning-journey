# Day 48: Testing Best Practices & TDD (Test-Driven Development)

## Learning Goals
By the end of this session, you will:
- Master Test-Driven Development (TDD) methodology and Red-Green-Refactor cycle
- Implement comprehensive testing strategies including unit, integration, and acceptance tests
- Apply SOLID principles to create testable, maintainable code
- Use advanced testing patterns: Test Doubles, Page Object Model, Builder Pattern
- Implement performance-aware testing with continuous monitoring
- Create test pyramids and understand testing anti-patterns
- Build robust test suites with proper test organization and lifecycle management

## Table of Contents
1. [Test-Driven Development Fundamentals](#tdd-fundamentals)
2. [Testing Best Practices & Patterns](#testing-best-practices)
3. [Advanced TDD Techniques](#advanced-tdd)
4. [Performance-Aware Testing](#performance-aware-testing)
5. [Test Organization & Architecture](#test-organization)
6. [Testing Anti-Patterns & Solutions](#anti-patterns)
7. [Hands-On Practice](#hands-on-practice)
8. [Challenges](#challenges)
9. [Summary](#summary)

---

## TDD Fundamentals

### The Red-Green-Refactor Cycle

Test-Driven Development follows a simple but powerful cycle:
1. **Red**: Write a failing test
2. **Green**: Write minimal code to make the test pass
3. **Refactor**: Improve code quality while keeping tests green

Let's implement a complete TDD example:

```java
// TDDCalculatorTest.java - Starting with failing tests
package com.javajourney.tdd;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Calculator Test Suite
 * Demonstrates complete Red-Green-Refactor cycle with performance monitoring
 */
@ExtendWith(PerformanceTestExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class TDDCalculatorTest {
    
    private Calculator calculator;
    private static final double DELTA = 0.0001;
    
    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }
    
    // RED PHASE: Write failing tests first
    @Test
    @Order(1)
    @DisplayName("Should add two positive numbers")
    void shouldAddTwoPositiveNumbers() {
        // This test will fail initially - Calculator doesn't exist yet
        double result = calculator.add(2.0, 3.0);
        assertEquals(5.0, result, DELTA);
    }
    
    @Test
    @Order(2)
    @DisplayName("Should handle negative numbers in addition")
    void shouldHandleNegativeNumbers() {
        assertEquals(-1.0, calculator.add(-3.0, 2.0), DELTA);
        assertEquals(-5.0, calculator.add(-2.0, -3.0), DELTA);
    }
    
    @Test
    @Order(3)
    @DisplayName("Should subtract numbers correctly")
    void shouldSubtractNumbers() {
        assertEquals(2.0, calculator.subtract(5.0, 3.0), DELTA);
        assertEquals(-8.0, calculator.subtract(-3.0, 5.0), DELTA);
    }
    
    @Test
    @Order(4)
    @DisplayName("Should multiply numbers correctly")
    void shouldMultiplyNumbers() {
        assertEquals(15.0, calculator.multiply(3.0, 5.0), DELTA);
        assertEquals(-12.0, calculator.multiply(-3.0, 4.0), DELTA);
        assertEquals(0.0, calculator.multiply(0.0, 100.0), DELTA);
    }
    
    @Test
    @Order(5)
    @DisplayName("Should divide numbers correctly")
    void shouldDivideNumbers() {
        assertEquals(2.0, calculator.divide(10.0, 5.0), DELTA);
        assertEquals(-2.5, calculator.divide(-5.0, 2.0), DELTA);
    }
    
    @Test
    @Order(6)
    @DisplayName("Should throw exception when dividing by zero")
    void shouldThrowExceptionForDivisionByZero() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculator.divide(10.0, 0.0)
        );
        assertEquals("Division by zero is not allowed", exception.getMessage());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.0, -1.0, 100.0, -100.0})
    @DisplayName("Should handle edge cases in operations")
    void shouldHandleEdgeCases(double value) {
        assertEquals(value, calculator.add(value, 0.0), DELTA);
        assertEquals(value, calculator.subtract(value, 0.0), DELTA);
        assertEquals(0.0, calculator.multiply(value, 0.0), DELTA);
    }
    
    @ParameterizedTest
    @CsvSource({
        "2.0, 3.0, 5.0",
        "-1.0, 1.0, 0.0",
        "0.5, 0.5, 1.0",
        "1000000.0, 1.0, 1000001.0"
    })
    @DisplayName("Should add various number combinations")
    void shouldAddVariousNumbers(double a, double b, double expected) {
        assertEquals(expected, calculator.add(a, b), DELTA);
    }
}

// GREEN PHASE: Minimal implementation to make tests pass
// Calculator.java
package com.javajourney.tdd;

/**
 * Calculator implementation following TDD principles
 * Started with minimal implementation, evolved through refactoring
 */
public class Calculator {
    
    // Minimal implementation - just enough to make tests pass
    public double add(double a, double b) {
        return a + b;
    }
    
    public double subtract(double a, double b) {
        return a - b;
    }
    
    public double multiply(double a, double b) {
        return a * b;
    }
    
    public double divide(double a, double b) {
        if (b == 0.0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return a / b;
    }
}
```

### Advanced TDD with Complex Business Logic

```java
// TDDOrderProcessorTest.java - Complex business logic with TDD
package com.javajourney.tdd;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Order Processor Test Suite
 * Demonstrates TDD with complex business rules and dependencies
 */
class TDDOrderProcessorTest {
    
    @Mock private PaymentService paymentService;
    @Mock private InventoryService inventoryService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    
    private OrderProcessor orderProcessor;
    private Order testOrder;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderProcessor = new OrderProcessor(
            paymentService, inventoryService, notificationService, auditService
        );
        
        testOrder = Order.builder()
            .orderId("ORD-001")
            .customerId("CUST-001")
            .items(Arrays.asList(
                new OrderItem("ITEM-001", 2, new BigDecimal("10.00")),
                new OrderItem("ITEM-002", 1, new BigDecimal("25.00"))
            ))
            .build();
    }
    
    // RED: Write failing test for successful order processing
    @Test
    @DisplayName("Should process valid order successfully")
    void shouldProcessValidOrderSuccessfully() {
        // Arrange
        when(inventoryService.isAvailable("ITEM-001", 2)).thenReturn(true);
        when(inventoryService.isAvailable("ITEM-002", 1)).thenReturn(true);
        when(paymentService.processPayment(eq("CUST-001"), any(BigDecimal.class)))
            .thenReturn(new PaymentResult(true, "PAY-001"));
        
        // Act
        OrderResult result = orderProcessor.processOrder(testOrder);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals("ORD-001", result.getOrderId());
        assertNotNull(result.getPaymentId());
        
        // Verify interactions
        verify(inventoryService).reserveItems(any(List.class));
        verify(paymentService).processPayment(eq("CUST-001"), eq(new BigDecimal("45.00")));
        verify(notificationService).sendOrderConfirmation("CUST-001", "ORD-001");
        verify(auditService).logOrderProcessing("ORD-001", OrderStatus.COMPLETED);
    }
    
    // RED: Test for insufficient inventory
    @Test
    @DisplayName("Should handle insufficient inventory")
    void shouldHandleInsufficientInventory() {
        // Arrange
        when(inventoryService.isAvailable("ITEM-001", 2)).thenReturn(false);
        
        // Act
        OrderResult result = orderProcessor.processOrder(testOrder);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Insufficient inventory for item: ITEM-001", result.getErrorMessage());
        
        // Verify no payment was attempted
        verify(paymentService, never()).processPayment(any(), any());
        verify(auditService).logOrderProcessing("ORD-001", OrderStatus.FAILED);
    }
    
    // RED: Test for payment failure
    @Test
    @DisplayName("Should handle payment failure with rollback")
    void shouldHandlePaymentFailureWithRollback() {
        // Arrange
        when(inventoryService.isAvailable(anyString(), anyInt())).thenReturn(true);
        when(paymentService.processPayment(any(), any()))
            .thenReturn(new PaymentResult(false, "Insufficient funds"));
        
        // Act
        OrderResult result = orderProcessor.processOrder(testOrder);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Payment failed: Insufficient funds", result.getErrorMessage());
        
        // Verify rollback occurred
        verify(inventoryService).releaseReservation("ORD-001");
        verify(auditService).logOrderProcessing("ORD-001", OrderStatus.PAYMENT_FAILED);
    }
    
    // RED: Test for concurrent order processing
    @Test
    @DisplayName("Should handle concurrent order processing safely")
    void shouldHandleConcurrentOrderProcessingSafely() throws InterruptedException {
        // Arrange
        when(inventoryService.isAvailable(anyString(), anyInt())).thenReturn(true);
        when(paymentService.processPayment(any(), any()))
            .thenReturn(new PaymentResult(true, "PAY-001"));
        
        // Act - Process multiple orders concurrently
        List<Thread> threads = Arrays.asList(
            new Thread(() -> orderProcessor.processOrder(testOrder)),
            new Thread(() -> orderProcessor.processOrder(testOrder)),
            new Thread(() -> orderProcessor.processOrder(testOrder))
        );
        
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Assert - Verify thread safety
        verify(auditService, times(3)).logOrderProcessing(eq("ORD-001"), any());
    }
}

// GREEN PHASE: Implement OrderProcessor to make tests pass
// OrderProcessor.java
package com.javajourney.tdd;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Order Processor implementation following TDD principles
 * Handles complex business logic with proper error handling and rollback
 */
public class OrderProcessor {
    
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ReentrantLock processLock = new ReentrantLock();
    
    public OrderProcessor(PaymentService paymentService,
                         InventoryService inventoryService,
                         NotificationService notificationService,
                         AuditService auditService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }
    
    public OrderResult processOrder(Order order) {
        processLock.lock();
        try {
            auditService.logOrderProcessing(order.getOrderId(), OrderStatus.PROCESSING);
            
            // Step 1: Check inventory availability
            if (!checkInventoryAvailability(order)) {
                auditService.logOrderProcessing(order.getOrderId(), OrderStatus.FAILED);
                return OrderResult.failure(order.getOrderId(), 
                    "Insufficient inventory for item: " + getUnavailableItem(order));
            }
            
            // Step 2: Reserve inventory
            inventoryService.reserveItems(order.getItems());
            
            // Step 3: Process payment
            BigDecimal totalAmount = calculateTotalAmount(order);
            PaymentResult paymentResult = paymentService.processPayment(
                order.getCustomerId(), totalAmount);
            
            if (!paymentResult.isSuccessful()) {
                // Rollback inventory reservation
                inventoryService.releaseReservation(order.getOrderId());
                auditService.logOrderProcessing(order.getOrderId(), OrderStatus.PAYMENT_FAILED);
                return OrderResult.failure(order.getOrderId(), 
                    "Payment failed: " + paymentResult.getErrorMessage());
            }
            
            // Step 4: Send confirmation
            notificationService.sendOrderConfirmation(
                order.getCustomerId(), order.getOrderId());
            
            auditService.logOrderProcessing(order.getOrderId(), OrderStatus.COMPLETED);
            return OrderResult.success(order.getOrderId(), paymentResult.getPaymentId());
            
        } catch (Exception e) {
            auditService.logOrderProcessing(order.getOrderId(), OrderStatus.FAILED);
            return OrderResult.failure(order.getOrderId(), "Processing error: " + e.getMessage());
        } finally {
            processLock.unlock();
        }
    }
    
    private boolean checkInventoryAvailability(Order order) {
        return order.getItems().stream()
            .allMatch(item -> inventoryService.isAvailable(
                item.getProductId(), item.getQuantity()));
    }
    
    private String getUnavailableItem(Order order) {
        return order.getItems().stream()
            .filter(item -> !inventoryService.isAvailable(
                item.getProductId(), item.getQuantity()))
            .map(OrderItem::getProductId)
            .findFirst()
            .orElse("Unknown");
    }
    
    private BigDecimal calculateTotalAmount(Order order) {
        return order.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

---

## Testing Best Practices

### Test Doubles and Mocking Strategies

```java
// TestDoubleExamples.java - Comprehensive test doubles demonstration
package com.javajourney.testing.doubles;

import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Test Doubles Examples
 * Demonstrates Dummy, Fake, Stub, Spy, and Mock objects
 */
class TestDoubleExamples {
    
    // 1. DUMMY OBJECTS - Objects passed but never used
    @Test
    @DisplayName("Dummy objects are passed but never actually used")
    void dummyObjectExample() {
        EmailService dummyEmailService = mock(EmailService.class);
        UserService userService = new UserService(dummyEmailService);
        
        // dummyEmailService is never called in this operation
        User user = userService.createUser("john", "password");
        assertNotNull(user);
    }
    
    // 2. FAKE OBJECTS - Working implementations with shortcuts
    @Test
    @DisplayName("Fake objects have working implementations but take shortcuts")
    void fakeObjectExample() {
        // In-memory database instead of real database
        UserRepository fakeRepository = new InMemoryUserRepository();
        UserService userService = new UserService(fakeRepository);
        
        User user = userService.createUser("jane", "password");
        User retrieved = userService.findUser("jane");
        
        assertEquals(user.getUsername(), retrieved.getUsername());
    }
    
    // 3. STUB OBJECTS - Provide canned answers to calls
    @Test
    @DisplayName("Stub objects provide canned answers to calls made during test")
    void stubObjectExample() {
        PaymentGateway stubGateway = mock(PaymentGateway.class);
        
        // Stub returns predetermined response
        when(stubGateway.processPayment(any(BigDecimal.class)))
            .thenReturn(new PaymentResult(true, "PAYMENT-123"));
        
        PaymentService paymentService = new PaymentService(stubGateway);
        PaymentResult result = paymentService.processPayment(new BigDecimal("100.00"));
        
        assertTrue(result.isSuccessful());
        assertEquals("PAYMENT-123", result.getTransactionId());
    }
    
    // 4. SPY OBJECTS - Real objects that record how they were called
    @Test
    @DisplayName("Spy objects are real objects that record how they were called")
    void spyObjectExample() {
        // Spy on real object
        EmailService realEmailService = new EmailService();
        EmailService spyEmailService = spy(realEmailService);
        
        UserService userService = new UserService(spyEmailService);
        userService.registerUser("bob", "bob@example.com");
        
        // Verify the spy was called with correct parameters
        verify(spyEmailService).sendWelcomeEmail("bob@example.com");
    }
    
    // 5. MOCK OBJECTS - Pre-programmed with expectations
    @Mock private NotificationService mockNotificationService;
    @Mock private AuditService mockAuditService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    @DisplayName("Mock objects are pre-programmed with expectations of calls they receive")
    void mockObjectExample() {
        OrderService orderService = new OrderService(mockNotificationService, mockAuditService);
        
        // Pre-program mock expectations
        doNothing().when(mockNotificationService).sendNotification(any(), any());
        doNothing().when(mockAuditService).logEvent(any());
        
        // Execute
        orderService.processOrder("ORDER-123");
        
        // Verify mock expectations were met
        verify(mockNotificationService).sendNotification("ORDER-123", "Order processed");
        verify(mockAuditService).logEvent("Order ORDER-123 processed");
    }
    
    // Advanced Mocking - Argument Captors
    @Test
    @DisplayName("Argument captors allow verification of complex arguments")
    void argumentCaptorExample() {
        ArgumentCaptor<NotificationRequest> captor = 
            ArgumentCaptor.forClass(NotificationRequest.class);
        
        NotificationService mockService = mock(NotificationService.class);
        OrderService orderService = new OrderService(mockService, mockAuditService);
        
        orderService.processOrder("ORDER-456");
        
        verify(mockService).sendComplexNotification(captor.capture());
        NotificationRequest captured = captor.getValue();
        
        assertEquals("ORDER-456", captured.getOrderId());
        assertEquals(NotificationType.ORDER_CONFIRMATION, captured.getType());
    }
}
```

### Page Object Model for UI Testing

```java
// PageObjectModelExample.java - UI testing with Page Object pattern
package com.javajourney.testing.pageobject;

import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/**
 * Page Object Model implementation for maintainable UI tests
 * Separates page structure from test logic
 */

// Page Object for Login Page
class LoginPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    
    @FindBy(id = "username")
    private WebElement usernameField;
    
    @FindBy(id = "password")
    private WebElement passwordField;
    
    @FindBy(id = "login-button")
    private WebElement loginButton;
    
    @FindBy(className = "error-message")
    private WebElement errorMessage;
    
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }
    
    public LoginPage enterUsername(String username) {
        usernameField.clear();
        usernameField.sendKeys(username);
        return this;
    }
    
    public LoginPage enterPassword(String password) {
        passwordField.clear();
        passwordField.sendKeys(password);
        return this;
    }
    
    public DashboardPage clickLoginExpectingSuccess() {
        loginButton.click();
        return new DashboardPage(driver);
    }
    
    public LoginPage clickLoginExpectingFailure() {
        loginButton.click();
        return this;
    }
    
    public String getErrorMessage() {
        return errorMessage.getText();
    }
    
    public boolean isErrorDisplayed() {
        return errorMessage.isDisplayed();
    }
}

// Page Object for Dashboard Page
class DashboardPage {
    private final WebDriver driver;
    
    @FindBy(className = "welcome-message")
    private WebElement welcomeMessage;
    
    @FindBy(id = "user-profile")
    private WebElement userProfile;
    
    @FindBy(id = "logout-button")
    private WebElement logoutButton;
    
    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }
    
    public String getWelcomeMessage() {
        return welcomeMessage.getText();
    }
    
    public boolean isLogoutButtonVisible() {
        return logoutButton.isDisplayed();
    }
    
    public LoginPage logout() {
        logoutButton.click();
        return new LoginPage(driver);
    }
}

// Test class using Page Objects
@TestMethodOrder(OrderAnnotation.class)
class LoginFlowTest {
    private WebDriver driver;
    private LoginPage loginPage;
    
    @BeforeEach
    void setUp() {
        // Setup WebDriver (implementation details omitted)
        driver = createWebDriver();
        driver.get("http://localhost:8080/login");
        loginPage = new LoginPage(driver);
    }
    
    @Test
    @Order(1)
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        DashboardPage dashboard = loginPage
            .enterUsername("testuser")
            .enterPassword("password123")
            .clickLoginExpectingSuccess();
        
        assertTrue(dashboard.getWelcomeMessage().contains("Welcome"));
        assertTrue(dashboard.isLogoutButtonVisible());
    }
    
    @Test
    @Order(2)
    @DisplayName("Should show error for invalid credentials")
    void shouldShowErrorForInvalidCredentials() {
        loginPage
            .enterUsername("invalid")
            .enterPassword("wrong")
            .clickLoginExpectingFailure();
        
        assertTrue(loginPage.isErrorDisplayed());
        assertEquals("Invalid username or password", loginPage.getErrorMessage());
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    private WebDriver createWebDriver() {
        // WebDriver setup implementation
        return null; // Placeholder
    }
}
```

---

## Advanced TDD

### Builder Pattern for Test Data

```java
// TestDataBuilderPattern.java - Builder pattern for maintainable test data
package com.javajourney.testing.builders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Data Builders for creating complex test objects
 * Provides fluent API for test data creation and maintenance
 */

// User Builder
class UserTestDataBuilder {
    private String username = "defaultUser";
    private String email = "default@example.com";
    private String password = "defaultPassword";
    private UserRole role = UserRole.USER;
    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private List<String> permissions = new ArrayList<>();
    
    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }
    
    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }
    
    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public UserTestDataBuilder withPassword(String password) {
        this.password = password;
        return this;
    }
    
    public UserTestDataBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }
    
    public UserTestDataBuilder inactive() {
        this.active = false;
        return this;
    }
    
    public UserTestDataBuilder withPermissions(String... permissions) {
        this.permissions = Arrays.asList(permissions);
        return this;
    }
    
    public UserTestDataBuilder admin() {
        return withRole(UserRole.ADMIN)
               .withPermissions("READ", "WRITE", "DELETE", "ADMIN");
    }
    
    public User build() {
        return new User(username, email, password, role, active, createdAt, permissions);
    }
}

// Order Builder with Complex Relationships
class OrderTestDataBuilder {
    private String orderId = "ORD-" + System.currentTimeMillis();
    private String customerId = "CUST-001";
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.PENDING;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String promoCode = null;
    private LocalDateTime orderDate = LocalDateTime.now();
    
    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }
    
    public OrderTestDataBuilder withId(String orderId) {
        this.orderId = orderId;
        return this;
    }
    
    public OrderTestDataBuilder forCustomer(String customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public OrderTestDataBuilder withItem(String productId, int quantity, BigDecimal price) {
        this.items.add(new OrderItem(productId, quantity, price));
        return this;
    }
    
    public OrderTestDataBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }
    
    public OrderTestDataBuilder withDiscount(BigDecimal amount, String promoCode) {
        this.discountAmount = amount;
        this.promoCode = promoCode;
        return this;
    }
    
    // Convenience methods for common scenarios
    public OrderTestDataBuilder smallOrder() {
        return withItem("ITEM-001", 1, new BigDecimal("10.00"));
    }
    
    public OrderTestDataBuilder largeOrder() {
        return withItem("ITEM-001", 5, new BigDecimal("100.00"))
               .withItem("ITEM-002", 3, new BigDecimal("50.00"))
               .withItem("ITEM-003", 2, new BigDecimal("25.00"));
    }
    
    public OrderTestDataBuilder completedOrder() {
        return withStatus(OrderStatus.COMPLETED)
               .withOrderDate(LocalDateTime.now().minusDays(1));
    }
    
    public Order build() {
        return new Order(orderId, customerId, items, status, 
                        discountAmount, promoCode, orderDate);
    }
}

// Usage in Tests
class OrderProcessingTestWithBuilders {
    
    @Test
    @DisplayName("Should apply discount for premium customers")
    void shouldApplyDiscountForPremiumCustomers() {
        // Arrange - Readable test data creation
        User premiumUser = UserTestDataBuilder.aUser()
            .withUsername("premium_customer")
            .withRole(UserRole.PREMIUM)
            .withPermissions("ORDER_DISCOUNT")
            .build();
        
        Order largeOrder = OrderTestDataBuilder.anOrder()
            .forCustomer(premiumUser.getId())
            .largeOrder()
            .build();
        
        // Act
        OrderResult result = orderProcessor.processOrder(largeOrder, premiumUser);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertTrue(result.getDiscountApplied().compareTo(BigDecimal.ZERO) > 0);
    }
    
    @Test
    @DisplayName("Should handle order for new customer")
    void shouldHandleOrderForNewCustomer() {
        // Arrange
        User newUser = UserTestDataBuilder.aUser()
            .withUsername("new_customer")
            .withCreatedDate(LocalDateTime.now())
            .build();
        
        Order firstOrder = OrderTestDataBuilder.anOrder()
            .forCustomer(newUser.getId())
            .smallOrder()
            .build();
        
        // Act & Assert
        OrderResult result = orderProcessor.processOrder(firstOrder, newUser);
        assertTrue(result.isSuccessful());
        assertEquals("Welcome bonus applied", result.getSpecialMessage());
    }
}
```

### Property-Based Testing

```java
// PropertyBasedTestingExample.java - Property-based testing with jqwik
package com.javajourney.testing.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-Based Testing Examples
 * Tests properties that should hold for wide ranges of inputs
 */
class PropertyBasedTestingExample {
    
    // Property: Addition is commutative
    @Property
    @Report(Reporting.GENERATED)
    void additionIsCommutative(@ForAll int a, @ForAll int b) {
        Calculator calculator = new Calculator();
        
        double result1 = calculator.add(a, b);
        double result2 = calculator.add(b, a);
        
        assertEquals(result1, result2, 0.0001, 
            "Addition should be commutative: a + b = b + a");
    }
    
    // Property: String reverse is involutive (applying twice returns original)
    @Property
    void stringReverseIsInvolutive(@ForAll @AlphaChars @StringLength(min = 1, max = 100) String str) {
        StringUtility utility = new StringUtility();
        
        String reversed = utility.reverse(str);
        String doubleReversed = utility.reverse(reversed);
        
        assertEquals(str, doubleReversed, 
            "Reversing a string twice should return the original");
    }
    
    // Property: Sorted list properties
    @Property
    void sortedListProperties(@ForAll @Size(min = 1, max = 100) List<@IntRange(min = -1000, max = 1000) Integer> numbers) {
        ListSorter sorter = new ListSorter();
        
        List<Integer> sorted = sorter.sort(new ArrayList<>(numbers));
        
        // Property 1: Same size
        assertEquals(numbers.size(), sorted.size(), "Sorted list should have same size");
        
        // Property 2: Contains all original elements
        assertTrue(sorted.containsAll(numbers), "Sorted list should contain all original elements");
        
        // Property 3: Is actually sorted
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i) <= sorted.get(i + 1), 
                "List should be sorted in ascending order");
        }
    }
    
    // Property: Email validation
    @Property
    void emailValidationProperties(
        @ForAll @AlphaChars @StringLength(min = 1, max = 50) String localPart,
        @ForAll @AlphaChars @StringLength(min = 1, max = 50) String domain,
        @ForAll @AlphaChars @StringLength(min = 2, max = 4) String tld) {
        
        EmailValidator validator = new EmailValidator();
        String email = localPart + "@" + domain + "." + tld;
        
        boolean isValid = validator.isValid(email);
        
        if (isValid) {
            // If email is valid, it should contain @ and .
            assertTrue(email.contains("@"), "Valid email should contain @");
            assertTrue(email.contains("."), "Valid email should contain .");
            assertTrue(email.indexOf("@") < email.lastIndexOf("."), 
                "@ should come before the last .");
        }
    }
    
    // Property: Concurrent operations safety
    @Property
    @Report(Reporting.GENERATED)
    void concurrentOperationsAreSafe(@ForAll @Size(min = 1, max = 10) List<@IntRange(min = 1, max = 100) Integer> operations) 
        throws InterruptedException {
        
        ThreadSafeCounter counter = new ThreadSafeCounter();
        List<Thread> threads = new ArrayList<>();
        
        // Create threads for each operation
        for (Integer op : operations) {
            threads.add(new Thread(() -> counter.increment(op)));
        }
        
        // Start all threads
        threads.forEach(Thread::start);
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Property: Final value should equal sum of all operations
        int expectedSum = operations.stream().mapToInt(Integer::intValue).sum();
        assertEquals(expectedSum, counter.getValue(), 
            "Thread-safe counter should equal sum of all increments");
    }
    
    // Custom generator for complex objects
    @Provide
    Arbitrary<User> validUsers() {
        return Combinators.combine(
            Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().withCharRange('a', 'z').ofMinLength(5).ofMaxLength(30),
            Arbitraries.of(UserRole.values())
        ).as(User::new);
    }
    
    @Property
    void userPropertiesAreConsistent(@ForAll("validUsers") User user) {
        UserService userService = new UserService();
        
        // Property: User creation should preserve properties
        User created = userService.createUser(user.getUsername(), user.getEmail(), user.getRole());
        
        assertEquals(user.getUsername(), created.getUsername());
        assertEquals(user.getEmail(), created.getEmail());
        assertEquals(user.getRole(), created.getRole());
        assertNotNull(created.getId());
        assertTrue(created.isActive());
    }
}
```

---

## Performance-Aware Testing

### Custom JUnit 5 Extensions for Performance

```java
// PerformanceTestExtension.java - Custom JUnit 5 extension for performance monitoring
package com.javajourney.testing.performance;

import org.junit.jupiter.api.extension.*;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * JUnit 5 Extension for Performance Monitoring
 * Tracks execution time, memory usage, and performance regressions
 */
public class PerformanceTestExtension implements BeforeTestExecutionCallback, 
                                               AfterTestExecutionCallback,
                                               BeforeAllCallback {
    
    private static final Map<String, PerformanceMetrics> performanceHistory = 
        new ConcurrentHashMap<>();
    private static final String START_TIME = "start_time";
    private static final String INITIAL_MEMORY = "initial_memory";
    
    @Override
    public void beforeAll(ExtensionContext context) {
        System.out.println("=== Performance Monitoring Started ===");
        Runtime.getRuntime().gc(); // Suggest garbage collection before testing
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        // Record start time and memory
        long startTime = System.nanoTime();
        long initialMemory = getUsedMemory();
        
        getStore(context).put(START_TIME, startTime);
        getStore(context).put(INITIAL_MEMORY, initialMemory);
        
        String testName = context.getDisplayName();
        System.out.printf("⏱️  Starting test: %s%n", testName);
    }
    
    @Override
    public void afterTestExecution(ExtensionContext context) {
        // Calculate execution time and memory usage
        long startTime = getStore(context).remove(START_TIME, long.class);
        long initialMemory = getStore(context).remove(INITIAL_MEMORY, long.class);
        
        long endTime = System.nanoTime();
        long executionTime = endTime - startTime;
        long finalMemory = getUsedMemory();
        long memoryUsed = finalMemory - initialMemory;
        
        String testName = context.getDisplayName();
        String testMethod = context.getRequiredTestMethod().getName();
        
        // Create performance metrics
        PerformanceMetrics metrics = new PerformanceMetrics(
            testMethod, executionTime, memoryUsed, System.currentTimeMillis()
        );
        
        // Store metrics
        performanceHistory.put(testMethod, metrics);
        
        // Report performance
        reportPerformance(testName, metrics);
        
        // Check for performance regressions
        checkPerformanceRegression(testMethod, metrics);
    }
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    private void reportPerformance(String testName, PerformanceMetrics metrics) {
        double executionTimeMs = metrics.getExecutionTimeNanos() / 1_000_000.0;
        double memoryUsedMB = metrics.getMemoryUsedBytes() / (1024.0 * 1024.0);
        
        System.out.printf("📊 Test: %s%n", testName);
        System.out.printf("   ⏱️  Execution Time: %.2f ms%n", executionTimeMs);
        System.out.printf("   🧠 Memory Used: %.2f MB%n", memoryUsedMB);
        
        // Color-coded performance indicators
        if (executionTimeMs < 100) {
            System.out.printf("   ✅ Performance: EXCELLENT%n");
        } else if (executionTimeMs < 500) {
            System.out.printf("   ⚠️  Performance: GOOD%n");
        } else {
            System.out.printf("   🚨 Performance: NEEDS ATTENTION%n");
        }
    }
    
    private void checkPerformanceRegression(String testMethod, PerformanceMetrics current) {
        PerformanceMetrics previous = performanceHistory.get(testMethod + "_previous");
        if (previous != null) {
            double timeIncrease = (double) current.getExecutionTimeNanos() / previous.getExecutionTimeNanos();
            double memoryIncrease = (double) current.getMemoryUsedBytes() / previous.getMemoryUsedBytes();
            
            if (timeIncrease > 1.5) { // 50% slower
                System.out.printf("🚨 PERFORMANCE REGRESSION: %s is %.1fx slower%n", 
                    testMethod, timeIncrease);
            }
            
            if (memoryIncrease > 2.0) { // 100% more memory
                System.out.printf("🚨 MEMORY REGRESSION: %s uses %.1fx more memory%n", 
                    testMethod, memoryIncrease);
            }
        }
        
        // Store current as previous for next run
        performanceHistory.put(testMethod + "_previous", current);
    }
    
    public static Map<String, PerformanceMetrics> getPerformanceHistory() {
        return new ConcurrentHashMap<>(performanceHistory);
    }
}

// PerformanceMetrics.java - Data class for performance metrics
class PerformanceMetrics {
    private final String testMethod;
    private final long executionTimeNanos;
    private final long memoryUsedBytes;
    private final long timestamp;
    
    public PerformanceMetrics(String testMethod, long executionTimeNanos, 
                             long memoryUsedBytes, long timestamp) {
        this.testMethod = testMethod;
        this.executionTimeNanos = executionTimeNanos;
        this.memoryUsedBytes = memoryUsedBytes;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getTestMethod() { return testMethod; }
    public long getExecutionTimeNanos() { return executionTimeNanos; }
    public long getMemoryUsedBytes() { return memoryUsedBytes; }
    public long getTimestamp() { return timestamp; }
    
    public double getExecutionTimeMs() { return executionTimeNanos / 1_000_000.0; }
    public double getMemoryUsedMB() { return memoryUsedBytes / (1024.0 * 1024.0); }
}

// Performance benchmark annotations
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface PerformanceBenchmark {
    long maxExecutionTimeMs() default 1000;
    long maxMemoryMB() default 100;
    boolean enableRegression() default true;
}

// Enhanced Performance Test Extension with Benchmarks
public class BenchmarkTestExtension extends PerformanceTestExtension 
                                   implements AfterTestExecutionCallback {
    
    @Override
    public void afterTestExecution(ExtensionContext context) {
        super.afterTestExecution(context);
        
        Method testMethod = context.getRequiredTestMethod();
        PerformanceBenchmark benchmark = testMethod.getAnnotation(PerformanceBenchmark.class);
        
        if (benchmark != null) {
            PerformanceMetrics metrics = getPerformanceHistory().get(testMethod.getName());
            validateBenchmarks(testMethod.getName(), metrics, benchmark);
        }
    }
    
    private void validateBenchmarks(String testName, PerformanceMetrics metrics, 
                                  PerformanceBenchmark benchmark) {
        boolean passed = true;
        
        // Check execution time
        if (metrics.getExecutionTimeMs() > benchmark.maxExecutionTimeMs()) {
            System.out.printf("❌ BENCHMARK FAILED: %s execution time %.2f ms exceeds limit %d ms%n",
                testName, metrics.getExecutionTimeMs(), benchmark.maxExecutionTimeMs());
            passed = false;
        }
        
        // Check memory usage
        if (metrics.getMemoryUsedMB() > benchmark.maxMemoryMB()) {
            System.out.printf("❌ BENCHMARK FAILED: %s memory usage %.2f MB exceeds limit %d MB%n",
                testName, metrics.getMemoryUsedMB(), benchmark.maxMemoryMB());
            passed = false;
        }
        
        if (passed) {
            System.out.printf("✅ BENCHMARK PASSED: %s meets performance requirements%n", testName);
        }
    }
}
```

### Integration with Performance Tools

```java
// PerformanceIntegrationTest.java - Integration with external performance tools
package com.javajourney.testing.performance;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

/**
 * Performance Integration Testing
 * Demonstrates integration with JMH, profilers, and monitoring tools
 */
@ExtendWith({PerformanceTestExtension.class, BenchmarkTestExtension.class})
class PerformanceIntegrationTest {
    
    private DataProcessor processor;
    private MemoryMXBean memoryMXBean;
    private ThreadMXBean threadMXBean;
    
    @BeforeEach
    void setUp() {
        processor = new DataProcessor();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        threadMXBean = ManagementFactory.getThreadMXBean();
    }
    
    @Test
    @PerformanceBenchmark(maxExecutionTimeMs = 500, maxMemoryMB = 50)
    @DisplayName("Should process large dataset within performance limits")
    void shouldProcessLargeDatasetWithinLimits() {
        // Arrange
        List<DataRecord> largeDataset = generateLargeDataset(10000);
        
        // Act & Monitor
        long startCpuTime = threadMXBean.getCurrentThreadCpuTime();
        ProcessingResult result = processor.processData(largeDataset);
        long endCpuTime = threadMXBean.getCurrentThreadCpuTime();
        
        // Assert
        assertNotNull(result);
        assertEquals(10000, result.getProcessedCount());
        
        // Performance assertions
        long cpuTimeNanos = endCpuTime - startCpuTime;
        double cpuTimeMs = cpuTimeNanos / 1_000_000.0;
        
        assertTrue(cpuTimeMs < 400, 
            String.format("CPU time %.2f ms should be less than 400 ms", cpuTimeMs));
    }
    
    @Test
    @PerformanceBenchmark(maxExecutionTimeMs = 100, maxMemoryMB = 10)
    @DisplayName("Should cache frequently accessed data for performance")
    void shouldCacheFrequentlyAccessedData() {
        // Arrange
        String frequentKey = "frequent_data";
        CacheService cacheService = new CacheService();
        
        // Act - First access (cache miss)
        long start1 = System.nanoTime();
        String result1 = cacheService.getData(frequentKey);
        long time1 = System.nanoTime() - start1;
        
        // Act - Second access (cache hit)
        long start2 = System.nanoTime();
        String result2 = cacheService.getData(frequentKey);
        long time2 = System.nanoTime() - start2;
        
        // Assert
        assertEquals(result1, result2);
        assertTrue(time2 < time1 / 2, 
            "Cached access should be at least 2x faster than initial access");
    }
    
    @Test
    @DisplayName("Should handle concurrent access without performance degradation")
    void shouldHandleConcurrentAccessEfficiently() throws InterruptedException {
        // Arrange
        ConcurrentDataProcessor concurrentProcessor = new ConcurrentDataProcessor();
        int threadCount = 10;
        int operationsPerThread = 1000;
        List<Thread> threads = new ArrayList<>();
        List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());
        
        // Act
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                long startTime = System.nanoTime();
                for (int j = 0; j < operationsPerThread; j++) {
                    concurrentProcessor.processItem("item-" + j);
                }
                long endTime = System.nanoTime();
                executionTimes.add(endTime - startTime);
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Assert
        double avgExecutionTimeMs = executionTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0;
        
        assertTrue(avgExecutionTimeMs < 1000, 
            String.format("Average execution time %.2f ms should be reasonable", avgExecutionTimeMs));
        
        // Check that performance is consistent across threads
        double maxExecutionTimeMs = executionTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L) / 1_000_000.0;
        
        double minExecutionTimeMs = executionTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0L) / 1_000_000.0;
        
        double variationRatio = maxExecutionTimeMs / minExecutionTimeMs;
        assertTrue(variationRatio < 3.0, 
            String.format("Performance variation ratio %.2f should be reasonable", variationRatio));
    }
    
    // Memory leak detection test
    @Test
    @DisplayName("Should not have memory leaks in long-running operations")
    void shouldNotHaveMemoryLeaks() {
        // Arrange
        MemoryLeakDetector detector = new MemoryLeakDetector();
        long initialMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        
        // Act - Simulate long-running operation
        for (int i = 0; i < 1000; i++) {
            processor.processData(generateSmallDataset(100));
            
            // Force GC periodically
            if (i % 100 == 0) {
                System.gc();
                Thread.yield();
            }
        }
        
        // Final GC to clean up any remaining objects
        System.gc();
        Thread.yield();
        System.gc();
        
        long finalMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        long memoryIncrease = finalMemory - initialMemory;
        double memoryIncreaseMB = memoryIncrease / (1024.0 * 1024.0);
        
        // Assert - Memory increase should be minimal
        assertTrue(memoryIncreaseMB < 10, 
            String.format("Memory increase %.2f MB suggests potential memory leak", memoryIncreaseMB));
    }
    
    private List<DataRecord> generateLargeDataset(int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> new DataRecord("Record-" + i, "Data-" + i))
            .collect(Collectors.toList());
    }
    
    private List<DataRecord> generateSmallDataset(int size) {
        return generateLargeDataset(size);
    }
}
```

---

## Test Organization

### Test Categories and Suites

```java
// TestCategories.java - Test categorization and organization
package com.javajourney.testing.organization;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Test Categories and Organization
 * Demonstrates different test types and organization strategies
 */

// Test Categories using Tags
@Tag("unit")
@Tag("fast")
interface UnitTest { }

@Tag("integration")
@Tag("slow")
interface IntegrationTest { }

@Tag("contract")
interface ContractTest { }

@Tag("performance")
interface PerformanceTest { }

@Tag("smoke")
interface SmokeTest { }

// Unit Tests - Fast, isolated, no external dependencies
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class UserServiceUnitTests implements UnitTest {
    
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, emailService);
    }
    
    @Nested
    @DisplayName("User Creation Tests")
    class UserCreationTests {
        
        @Test
        @DisplayName("Should create user with valid data")
        void shouldCreateUserWithValidData() {
            // Arrange
            when(userRepository.findByUsername("john")).thenReturn(null);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId("USER-001");
                return user;
            });
            
            // Act
            User result = userService.createUser("john", "john@example.com", "password");
            
            // Assert
            assertNotNull(result);
            assertEquals("john", result.getUsername());
            assertEquals("USER-001", result.getId());
            verify(emailService).sendWelcomeEmail("john@example.com");
        }
        
        @Test
        @DisplayName("Should throw exception for duplicate username")
        void shouldThrowExceptionForDuplicateUsername() {
            // Arrange
            when(userRepository.findByUsername("existing")).thenReturn(new User());
            
            // Act & Assert
            assertThrows(DuplicateUserException.class, 
                () -> userService.createUser("existing", "email@example.com", "password"));
        }
    }
    
    @Nested
    @DisplayName("User Validation Tests")
    class UserValidationTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "a", "ab"})
        @DisplayName("Should reject invalid usernames")
        void shouldRejectInvalidUsernames(String invalidUsername) {
            assertThrows(ValidationException.class,
                () -> userService.createUser(invalidUsername, "email@example.com", "password"));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"invalid", "invalid@", "@invalid.com", "invalid@.com"})
        @DisplayName("Should reject invalid email addresses")
        void shouldRejectInvalidEmails(String invalidEmail) {
            assertThrows(ValidationException.class,
                () -> userService.createUser("username", invalidEmail, "password"));
        }
    }
}

// Integration Tests - Test component interactions
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class UserServiceIntegrationTests implements IntegrationTest {
    
    @Autowired private UserService userService;
    @Autowired private TestEntityManager entityManager;
    private TestContainers containers;
    
    @BeforeAll
    static void setUpContainers() {
        containers = new TestContainers();
        containers.start();
    }
    
    @AfterAll
    static void tearDownContainers() {
        containers.stop();
    }
    
    @Test
    @DisplayName("Should persist user to database")
    void shouldPersistUserToDatabase() {
        // Act
        User created = userService.createUser("dbuser", "dbuser@example.com", "password");
        entityManager.flush();
        entityManager.clear();
        
        // Assert
        User found = entityManager.find(User.class, created.getId());
        assertNotNull(found);
        assertEquals("dbuser", found.getUsername());
    }
    
    @Test
    @DisplayName("Should handle database constraints")
    void shouldHandleDatabaseConstraints() {
        // Arrange
        userService.createUser("constraint", "constraint@example.com", "password");
        entityManager.flush();
        
        // Act & Assert
        assertThrows(DuplicateUserException.class,
            () -> userService.createUser("constraint", "different@example.com", "password"));
    }
}

// Contract Tests - API contract verification
class UserApiContractTests implements ContractTest {
    
    @Test
    @DisplayName("Should maintain API contract for user creation")
    void shouldMaintainApiContractForUserCreation() {
        // This would typically use Pact or Spring Cloud Contract
        // Simplified example
        UserApiClient client = new UserApiClient("http://localhost:8080");
        
        CreateUserRequest request = new CreateUserRequest("contractuser", 
            "contract@example.com", "password");
        CreateUserResponse response = client.createUser(request);
        
        // Contract assertions
        assertNotNull(response.getUserId());
        assertEquals("contractuser", response.getUsername());
        assertEquals(201, response.getStatusCode());
    }
}

// Performance Tests
@Execution(ExecutionMode.CONCURRENT)
class UserServicePerformanceTests implements PerformanceTest {
    
    @Test
    @DisplayName("Should handle high load user creation")
    void shouldHandleHighLoadUserCreation() throws InterruptedException {
        UserService userService = new UserService(new InMemoryUserRepository(), 
            new MockEmailService());
        
        int threadCount = 100;
        int usersPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < usersPerThread; j++) {
                        userService.createUser("user" + threadId + "_" + j, 
                            "email" + threadId + "_" + j + "@example.com", "password");
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Log error
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Performance test should complete within 30 seconds");
        assertEquals(threadCount * usersPerThread, successCount.get(), 
            "All user creations should succeed");
    }
}

// Smoke Tests - Basic functionality verification
class UserServiceSmokeTests implements SmokeTest {
    
    @Test
    @DisplayName("Should create and retrieve user successfully")
    void shouldCreateAndRetrieveUserSuccessfully() {
        UserService userService = new UserService(new InMemoryUserRepository(), 
            new MockEmailService());
        
        User created = userService.createUser("smoketest", "smoke@example.com", "password");
        User retrieved = userService.findUser("smoketest");
        
        assertNotNull(created);
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }
}
```

### Test Configuration and Profiles

```java
// TestConfiguration.java - Test configuration and profiles
package com.javajourney.testing.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test Configuration and Profiles
 * Demonstrates different test configurations for different environments
 */

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Profile("test")
    public UserRepository testUserRepository() {
        return new InMemoryUserRepository();
    }
    
    @Bean
    @Profile("integration")
    public UserRepository integrationUserRepository() {
        return new DatabaseUserRepository();
    }
    
    @Bean
    @Profile({"test", "integration"})
    public EmailService testEmailService() {
        return new MockEmailService();
    }
    
    @Bean
    @Profile("performance")
    public PerformanceMonitor performanceMonitor() {
        return new DetailedPerformanceMonitor();
    }
}

// Base Test Classes
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
abstract class BaseUnitTest {
    // Common setup for unit tests
}

@ActiveProfiles("integration")
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration.properties")
abstract class BaseIntegrationTest {
    // Common setup for integration tests
}

@ActiveProfiles("performance")
@ExtendWith({PerformanceTestExtension.class})
abstract class BasePerformanceTest {
    // Common setup for performance tests
}
```

---

## Anti-Patterns

### Common Testing Anti-Patterns and Solutions

```java
// TestingAntiPatterns.java - Common anti-patterns and their solutions
package com.javajourney.testing.antipatterns;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing Anti-Patterns and Solutions
 * Demonstrates what NOT to do and better alternatives
 */
class TestingAntiPatterns {
    
    // ANTI-PATTERN 1: Mystery Guest
    // Bad: Dependencies on external state not visible in test
    @Test
    @DisplayName("BAD: Mystery Guest - Hidden dependencies")
    void badMysteryGuestExample() {
        // This test depends on data that might exist in database
        // but it's not clear from the test what that data is
        UserService userService = new UserService();
        User user = userService.findUser("admin"); // Where does "admin" come from?
        
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
    }
    
    // SOLUTION: Make dependencies explicit
    @Test
    @DisplayName("GOOD: Explicit dependencies and setup")
    void goodExplicitDependenciesExample() {
        // Clear setup of test data
        UserRepository repository = new InMemoryUserRepository();
        User adminUser = new User("admin", "admin@example.com", UserRole.ADMIN);
        repository.save(adminUser);
        
        UserService userService = new UserService(repository);
        User foundUser = userService.findUser("admin");
        
        assertNotNull(foundUser);
        assertEquals("admin", foundUser.getUsername());
    }
    
    // ANTI-PATTERN 2: Test Interdependence
    // Bad: Tests depend on execution order
    private static User globalUser; // Shared state between tests
    
    @Test
    @Order(1)
    @DisplayName("BAD: Test creates shared state")
    void badTestCreatesSharedState() {
        globalUser = new User("shared", "shared@example.com");
        assertNotNull(globalUser);
    }
    
    @Test
    @Order(2)
    @DisplayName("BAD: Test depends on previous test")
    void badTestDependsOnPreviousTest() {
        // This test will fail if the first test doesn't run
        assertNotNull(globalUser);
        assertEquals("shared", globalUser.getUsername());
    }
    
    // SOLUTION: Independent tests
    @Test
    @DisplayName("GOOD: Each test sets up its own data")
    void goodIndependentTest1() {
        User user = new User("independent1", "user1@example.com");
        assertNotNull(user);
        assertEquals("independent1", user.getUsername());
    }
    
    @Test
    @DisplayName("GOOD: Each test is completely independent")
    void goodIndependentTest2() {
        User user = new User("independent2", "user2@example.com");
        assertNotNull(user);
        assertEquals("independent2", user.getUsername());
    }
    
    // ANTI-PATTERN 3: Overly Complex Tests
    @Test
    @DisplayName("BAD: Overly complex test doing too much")
    void badOverlyComplexTest() {
        // This test is doing too many things
        UserService userService = new UserService();
        EmailService emailService = new EmailService();
        NotificationService notificationService = new NotificationService();
        AuditService auditService = new AuditService();
        
        // Creating user
        User user = userService.createUser("complex", "complex@example.com", "password");
        assertNotNull(user);
        
        // Updating user
        user.setEmail("updated@example.com");
        userService.updateUser(user);
        assertEquals("updated@example.com", user.getEmail());
        
        // Sending email
        emailService.sendEmail(user.getEmail(), "Welcome", "Welcome message");
        
        // Sending notification
        notificationService.sendNotification(user.getId(), "User created");
        
        // Auditing
        auditService.logUserAction(user.getId(), "USER_CREATED");
        
        // Too many assertions testing different concerns
        assertTrue(emailService.wasEmailSent());
        assertTrue(notificationService.wasNotificationSent());
        assertTrue(auditService.wasActionLogged());
    }
    
    // SOLUTION: Split into focused tests
    @Nested
    @DisplayName("GOOD: Focused tests for specific concerns")
    class FocusedUserTests {
        
        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            UserService userService = new UserService();
            
            User user = userService.createUser("focused", "focused@example.com", "password");
            
            assertNotNull(user);
            assertEquals("focused", user.getUsername());
            assertEquals("focused@example.com", user.getEmail());
        }
        
        @Test
        @DisplayName("Should update user email")
        void shouldUpdateUserEmail() {
            UserService userService = new UserService();
            User user = new User("test", "old@example.com");
            
            userService.updateUserEmail(user, "new@example.com");
            
            assertEquals("new@example.com", user.getEmail());
        }
        
        @Test
        @DisplayName("Should send welcome email after user creation")
        void shouldSendWelcomeEmailAfterUserCreation() {
            EmailService mockEmailService = mock(EmailService.class);
            UserService userService = new UserService(mockEmailService);
            
            userService.createUser("email", "email@example.com", "password");
            
            verify(mockEmailService).sendWelcomeEmail("email@example.com");
        }
    }
    
    // ANTI-PATTERN 4: Testing Implementation Details
    @Test
    @DisplayName("BAD: Testing implementation details")
    void badTestingImplementationDetails() {
        PasswordHasher hasher = new PasswordHasher();
        
        // Testing internal implementation rather than behavior
        String hashed = hasher.hashPassword("password");
        assertTrue(hashed.startsWith("$2a$")); // Testing BCrypt implementation detail
        assertEquals(60, hashed.length()); // Testing BCrypt hash length
    }
    
    // SOLUTION: Test behavior, not implementation
    @Test
    @DisplayName("GOOD: Testing behavior and contracts")
    void goodTestingBehavior() {
        PasswordHasher hasher = new PasswordHasher();
        String password = "password";
        
        String hashed = hasher.hashPassword(password);
        
        // Test the behavior: can we verify the password?
        assertTrue(hasher.verifyPassword(password, hashed));
        assertFalse(hasher.verifyPassword("wrongpassword", hashed));
        
        // Test that each hash is unique (salt behavior)
        String anotherHash = hasher.hashPassword(password);
        assertNotEquals(hashed, anotherHash);
        assertTrue(hasher.verifyPassword(password, anotherHash));
    }
    
    // ANTI-PATTERN 5: Slow Tests
    @Test
    @DisplayName("BAD: Unnecessarily slow test")
    void badUnnecessarilySlowTest() throws InterruptedException {
        // Unnecessary delays in tests
        EmailService emailService = new EmailService();
        
        emailService.sendEmail("test@example.com", "Subject", "Body");
        
        // Arbitrary sleep - bad practice
        Thread.sleep(5000); // 5 seconds!
        
        assertTrue(emailService.wasEmailSent());
    }
    
    // SOLUTION: Fast, deterministic tests
    @Test
    @DisplayName("GOOD: Fast, deterministic test")
    void goodFastDeterministicTest() {
        EmailService mockEmailService = mock(EmailService.class);
        when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);
        
        boolean result = mockEmailService.sendEmail("test@example.com", "Subject", "Body");
        
        assertTrue(result);
        verify(mockEmailService).sendEmail("test@example.com", "Subject", "Body");
        // No delays, immediate verification
    }
    
    // ANTI-PATTERN 6: Test Code Duplication
    // Bad: Lots of duplicated setup code (shown conceptually)
    // Instead, use @BeforeEach, builders, factories, or helper methods
    
    // SOLUTION: Extract common setup
    private User createTestUser(String username, String email) {
        return User.builder()
            .username(username)
            .email(email)
            .password("defaultPassword")
            .role(UserRole.USER)
            .active(true)
            .build();
    }
    
    @Test
    @DisplayName("GOOD: Using helper methods to reduce duplication")
    void goodUsingHelperMethods() {
        User user = createTestUser("helper", "helper@example.com");
        
        assertNotNull(user);
        assertEquals("helper", user.getUsername());
        assertTrue(user.isActive());
    }
}
```

---

## Hands-On Practice

### Complete TDD Exercise: Shopping Cart

```java
// ShoppingCartTDDExercise.java - Complete TDD exercise
package com.javajourney.testing.practice;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete TDD Exercise: Shopping Cart Implementation
 * Follow Red-Green-Refactor cycle to build a shopping cart system
 */
class ShoppingCartTDDExercise {
    
    private ShoppingCart cart;
    private Product laptop;
    private Product mouse;
    private Product keyboard;
    
    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
        laptop = new Product("LAPTOP-001", "Gaming Laptop", new BigDecimal("999.99"));
        mouse = new Product("MOUSE-001", "Wireless Mouse", new BigDecimal("29.99"));
        keyboard = new Product("KEYBOARD-001", "Mechanical Keyboard", new BigDecimal("129.99"));
    }
    
    // RED PHASE: Write failing tests first
    
    @Test
    @DisplayName("Should start with empty cart")
    void shouldStartWithEmptyCart() {
        assertTrue(cart.isEmpty());
        assertEquals(0, cart.getItemCount());
        assertEquals(BigDecimal.ZERO, cart.getSubtotal());
    }
    
    @Test
    @DisplayName("Should add single item to cart")
    void shouldAddSingleItemToCart() {
        cart.addItem(laptop, 1);
        
        assertFalse(cart.isEmpty());
        assertEquals(1, cart.getItemCount());
        assertEquals(new BigDecimal("999.99"), cart.getSubtotal());
        assertTrue(cart.containsProduct("LAPTOP-001"));
    }
    
    @Test
    @DisplayName("Should add multiple quantities of same item")
    void shouldAddMultipleQuantitiesOfSameItem() {
        cart.addItem(mouse, 3);
        
        assertEquals(3, cart.getQuantity("MOUSE-001"));
        assertEquals(new BigDecimal("89.97"), cart.getSubtotal()); // 29.99 * 3
    }
    
    @Test
    @DisplayName("Should add different items to cart")
    void shouldAddDifferentItemsToCart() {
        cart.addItem(laptop, 1);
        cart.addItem(mouse, 2);
        
        assertEquals(2, cart.getUniqueItemCount());
        assertEquals(3, cart.getItemCount()); // 1 laptop + 2 mice
        assertEquals(new BigDecimal("1059.97"), cart.getSubtotal()); // 999.99 + (29.99 * 2)
    }
    
    @Test
    @DisplayName("Should remove item from cart")
    void shouldRemoveItemFromCart() {
        cart.addItem(laptop, 1);
        cart.addItem(mouse, 2);
        
        cart.removeItem("MOUSE-001");
        
        assertEquals(1, cart.getUniqueItemCount());
        assertFalse(cart.containsProduct("MOUSE-001"));
        assertEquals(new BigDecimal("999.99"), cart.getSubtotal());
    }
    
    @Test
    @DisplayName("Should update item quantity")
    void shouldUpdateItemQuantity() {
        cart.addItem(keyboard, 2);
        cart.updateQuantity("KEYBOARD-001", 5);
        
        assertEquals(5, cart.getQuantity("KEYBOARD-001"));
        assertEquals(new BigDecimal("649.95"), cart.getSubtotal()); // 129.99 * 5
    }
    
    @Test
    @DisplayName("Should clear entire cart")
    void shouldClearEntireCart() {
        cart.addItem(laptop, 1);
        cart.addItem(mouse, 2);
        cart.addItem(keyboard, 1);
        
        cart.clear();
        
        assertTrue(cart.isEmpty());
        assertEquals(0, cart.getItemCount());
        assertEquals(BigDecimal.ZERO, cart.getSubtotal());
    }
    
    @Test
    @DisplayName("Should calculate tax correctly")
    void shouldCalculateTaxCorrectly() {
        cart.addItem(laptop, 1); // $999.99
        cart.setTaxRate(new BigDecimal("0.08")); // 8% tax
        
        BigDecimal expectedTax = new BigDecimal("80.00"); // 999.99 * 0.08 = 79.9992, rounded
        assertEquals(expectedTax, cart.getTaxAmount());
        assertEquals(new BigDecimal("1079.99"), cart.getTotal());
    }
    
    @Test
    @DisplayName("Should apply discount correctly")
    void shouldApplyDiscountCorrectly() {
        cart.addItem(laptop, 1);
        cart.addItem(keyboard, 1);
        // Subtotal: 999.99 + 129.99 = 1129.98
        
        cart.applyDiscount(new BigDecimal("10.00")); // $10 off
        
        assertEquals(new BigDecimal("1119.98"), cart.getSubtotalAfterDiscount());
    }
    
    @Test
    @DisplayName("Should handle percentage discount")
    void shouldHandlePercentageDiscount() {
        cart.addItem(laptop, 1); // $999.99
        cart.applyPercentageDiscount(new BigDecimal("10")); // 10% off
        
        BigDecimal expectedDiscount = new BigDecimal("100.00"); // 999.99 * 0.10 = 99.999, rounded
        assertEquals(expectedDiscount, cart.getDiscountAmount());
        assertEquals(new BigDecimal("899.99"), cart.getSubtotalAfterDiscount());
    }
    
    @Test
    @DisplayName("Should throw exception for invalid operations")
    void shouldThrowExceptionForInvalidOperations() {
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(laptop, 0));
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(laptop, -1));
        
        assertThrows(ProductNotFoundException.class, () -> cart.updateQuantity("NONEXISTENT", 1));
        assertThrows(ProductNotFoundException.class, () -> cart.removeItem("NONEXISTENT"));
    }
    
    @Test
    @DisplayName("Should support cart serialization")
    void shouldSupportCartSerialization() {
        cart.addItem(laptop, 1);
        cart.addItem(mouse, 2);
        cart.setTaxRate(new BigDecimal("0.08"));
        cart.applyDiscount(new BigDecimal("50.00"));
        
        String serialized = cart.serialize();
        ShoppingCart deserializedCart = ShoppingCart.deserialize(serialized);
        
        assertEquals(cart.getItemCount(), deserializedCart.getItemCount());
        assertEquals(cart.getSubtotal(), deserializedCart.getSubtotal());
        assertEquals(cart.getTaxRate(), deserializedCart.getTaxRate());
        assertEquals(cart.getDiscountAmount(), deserializedCart.getDiscountAmount());
    }
}

// GREEN PHASE: Implement ShoppingCart to make tests pass
// This would be implemented step by step, starting with minimal functionality

// REFACTOR PHASE: Improve design while keeping tests green
```

---

## Challenges

### Challenge 1: TDD Kata - String Calculator
Implement a String Calculator using strict TDD:

1. Start with simplest test case
2. Follow Red-Green-Refactor cycle
3. Add features incrementally:
   - Handle empty string (returns 0)
   - Handle single number (returns the number)
   - Handle two numbers separated by comma
   - Handle multiple numbers
   - Handle new line as delimiter
   - Support custom delimiter
   - Throw exception for negative numbers
   - Ignore numbers greater than 1000

### Challenge 2: Performance-Aware Test Suite
Create a comprehensive test suite for a web service that:

1. Uses custom JUnit 5 extensions for performance monitoring
2. Implements property-based testing for business logic
3. Includes contract tests for API endpoints
4. Has performance benchmarks with regression detection
5. Demonstrates proper test organization and categorization

### Challenge 3: Legacy Code Testing
Given a legacy code base (simulate with tightly coupled code):

1. Add characterization tests to capture current behavior
2. Use TDD to add new features
3. Refactor code to make it more testable
4. Maintain 100% test coverage during refactoring
5. Document testing strategy and decisions

---

## Summary

### Key Takeaways

1. **TDD Methodology**:
   - Red-Green-Refactor cycle ensures comprehensive test coverage
   - Write failing tests first to clarify requirements
   - Implement minimal code to pass tests
   - Refactor confidently with test safety net

2. **Testing Best Practices**:
   - Test behavior, not implementation details
   - Keep tests fast, focused, and independent
   - Use appropriate test doubles for dependencies
   - Organize tests with clear naming and structure

3. **Advanced Techniques**:
   - Builder pattern for maintainable test data
   - Property-based testing for comprehensive coverage
   - Performance monitoring integrated into test suites
   - Page Object Model for UI testing

4. **Test Organization**:
   - Categorize tests by type and speed
   - Use test profiles for different environments
   - Implement proper test lifecycle management
   - Avoid common anti-patterns

5. **Performance Integration**:
   - Monitor performance during testing
   - Detect performance regressions automatically
   - Set performance benchmarks and budgets
   - Include memory and concurrency testing

### Performance Checklist
- [ ] Tests run in under 10 seconds for unit test suite
- [ ] Integration tests complete within 2 minutes
- [ ] Performance tests include regression detection
- [ ] Memory usage monitored during long-running tests
- [ ] Concurrent access patterns tested for thread safety

### Quality Gates Checklist
- [ ] All tests follow naming conventions
- [ ] Test coverage above 90% for critical code paths
- [ ] No test interdependencies or shared state
- [ ] Performance benchmarks established and monitored
- [ ] Contract tests verify API compatibility
- [ ] Property-based tests cover edge cases

### Tomorrow's Preview
Tomorrow we'll build the **Week 7 Capstone: Performance-Optimized Testing Framework** where we'll integrate all the concepts from this week into a comprehensive framework that combines performance monitoring, advanced testing patterns, and automated quality assurance into a production-ready testing solution.

---

*Day 48 Complete! You've mastered TDD methodology and testing best practices. You can now write comprehensive, maintainable test suites that include performance monitoring and follow industry best practices. Tomorrow we'll culminate Week 7 with a complete performance-optimized testing framework.*