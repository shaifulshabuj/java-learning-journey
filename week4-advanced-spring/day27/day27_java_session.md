# Day 27: Testing with Spring Boot (August 5, 2025)

**Date:** August 5, 2025  
**Duration:** 2.5 hours  
**Focus:** Master comprehensive testing strategies with JUnit 5, MockMvc, test slices, and integration testing

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master JUnit 5 with Spring Boot Test integration
- ✅ Implement web layer testing with MockMvc
- ✅ Use test slices for focused testing (@WebMvcTest, @DataJpaTest)
- ✅ Create comprehensive integration tests
- ✅ Handle mocking with @MockBean and Mockito
- ✅ Build a complete testing strategy for Spring Boot applications

---

## 🧪 Part 1: JUnit 5 & Spring Boot Test Fundamentals (45 minutes)

### **Testing Architecture Overview**

**Spring Boot Test** provides comprehensive testing support with:
- **@SpringBootTest**: Full application context loading
- **Test slices**: Focused testing with minimal context
- **Auto-configuration**: Automatic test configuration
- **TestContainers**: Integration with external services
- **MockMvc**: Web layer testing without HTTP server

### **JUnit 5 Basic Testing Demo**

Create `JUnit5BasicDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * JUnit 5 basic testing demonstration
 * Shows fundamental testing concepts and Spring Boot integration
 */
@SpringBootApplication
public class JUnit5BasicDemo {
    
    public static void main(String[] args) {
        System.out.println("=== JUnit 5 Basic Testing Demo ===\n");
        SpringApplication.run(JUnit5BasicDemo.class, args);
        
        System.out.println("🧪 Testing Features:");
        System.out.println("   - JUnit 5 lifecycle methods");
        System.out.println("   - Assertions and assumptions");
        System.out.println("   - Parameterized tests");
        System.out.println("   - Test method ordering");
        System.out.println("   - Conditional test execution");
        System.out.println("   - Spring Boot test integration");
        System.out.println();
    }
}

// Calculator service for testing
@Service
class CalculatorService {
    
    public int add(int a, int b) {
        return a + b;
    }
    
    public int subtract(int a, int b) {
        return a - b;
    }
    
    public int multiply(int a, int b) {
        return a * b;
    }
    
    public double divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return (double) a / b;
    }
    
    public boolean isPrime(int number) {
        if (number < 2) return false;
        if (number == 2) return true;
        if (number % 2 == 0) return false;
        
        for (int i = 3; i * i <= number; i += 2) {
            if (number % i == 0) return false;
        }
        return true;
    }
    
    public int factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }
}

// String utility service for testing
@Service
class StringUtilityService {
    
    public String reverse(String input) {
        if (input == null) return null;
        return new StringBuilder(input).reverse().toString();
    }
    
    public boolean isPalindrome(String input) {
        if (input == null) return false;
        String cleaned = input.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return cleaned.equals(reverse(cleaned));
    }
    
    public String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
    
    public int countWords(String input) {
        if (input == null || input.trim().isEmpty()) return 0;
        return input.trim().split("\\s+").length;
    }
    
    public List<String> splitByDelimiter(String input, String delimiter) {
        if (input == null) return Arrays.asList();
        return Arrays.asList(input.split(delimiter));
    }
}

/**
 * JUnit 5 basic test class demonstrating lifecycle and assertions
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class JUnit5BasicTest {
    
    @Autowired
    private CalculatorService calculatorService;
    
    @Autowired
    private StringUtilityService stringUtilityService;
    
    @BeforeAll
    static void setUpClass() {
        System.out.println("🚀 Setting up test class - executed once before all tests");
    }
    
    @BeforeEach
    void setUp() {
        System.out.println("🔧 Setting up test method - executed before each test");
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("🧹 Tearing down test method - executed after each test");
    }
    
    @AfterAll
    static void tearDownClass() {
        System.out.println("🏁 Tearing down test class - executed once after all tests");
    }
    
    @Test
    @Order(1)
    @DisplayName("Should add two positive numbers correctly")
    void shouldAddTwoPositiveNumbers() {
        // Given
        int a = 5;
        int b = 3;
        
        // When
        int result = calculatorService.add(a, b);
        
        // Then
        assertEquals(8, result, "5 + 3 should equal 8");
        assertTrue(result > 0, "Result should be positive");
        assertAll(
            () -> assertEquals(8, result),
            () -> assertTrue(result > a),
            () -> assertTrue(result > b)
        );
    }
    
    @Test
    @Order(2)
    @DisplayName("Should handle division by zero")
    void shouldHandleDivisionByZero() {
        // Given
        int dividend = 10;
        int divisor = 0;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculatorService.divide(dividend, divisor)
        );
        
        assertEquals("Division by zero is not allowed", exception.getMessage());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should calculate factorial correctly")
    void shouldCalculateFactorialCorrectly() {
        // Test cases
        assertEquals(1, calculatorService.factorial(0));
        assertEquals(1, calculatorService.factorial(1));
        assertEquals(2, calculatorService.factorial(2));
        assertEquals(6, calculatorService.factorial(3));
        assertEquals(24, calculatorService.factorial(4));
        assertEquals(120, calculatorService.factorial(5));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should identify prime numbers correctly")
    void shouldIdentifyPrimeNumbersCorrectly() {
        // Test prime numbers
        assertTrue(calculatorService.isPrime(2));
        assertTrue(calculatorService.isPrime(3));
        assertTrue(calculatorService.isPrime(5));
        assertTrue(calculatorService.isPrime(7));
        assertTrue(calculatorService.isPrime(11));
        assertTrue(calculatorService.isPrime(13));
        
        // Test non-prime numbers
        assertFalse(calculatorService.isPrime(1));
        assertFalse(calculatorService.isPrime(4));
        assertFalse(calculatorService.isPrime(6));
        assertFalse(calculatorService.isPrime(8));
        assertFalse(calculatorService.isPrime(9));
        assertFalse(calculatorService.isPrime(10));
    }
    
    @Test
    @Order(5)
    @DisplayName("Should reverse strings correctly")
    void shouldReverseStringsCorrectly() {
        assertEquals("olleh", stringUtilityService.reverse("hello"));
        assertEquals("dlrow", stringUtilityService.reverse("world"));
        assertEquals("", stringUtilityService.reverse(""));
        assertNull(stringUtilityService.reverse(null));
    }
    
    @Test
    @Order(6)
    @DisplayName("Should identify palindromes correctly")
    void shouldIdentifyPalindromesCorrectly() {
        assertTrue(stringUtilityService.isPalindrome("racecar"));
        assertTrue(stringUtilityService.isPalindrome("A man a plan a canal Panama"));
        assertTrue(stringUtilityService.isPalindrome("Was it a car or a cat I saw?"));
        
        assertFalse(stringUtilityService.isPalindrome("hello"));
        assertFalse(stringUtilityService.isPalindrome("world"));
        assertFalse(stringUtilityService.isPalindrome(null));
    }
    
    @Test
    @Order(7)
    @DisplayName("Should count words correctly")
    void shouldCountWordsCorrectly() {
        assertEquals(3, stringUtilityService.countWords("hello world java"));
        assertEquals(1, stringUtilityService.countWords("hello"));
        assertEquals(0, stringUtilityService.countWords(""));
        assertEquals(0, stringUtilityService.countWords("   "));
        assertEquals(0, stringUtilityService.countWords(null));
    }
    
    @Test
    @Order(8)
    @DisplayName("Should handle edge cases")
    void shouldHandleEdgeCases() {
        // Test null inputs
        assertNull(stringUtilityService.reverse(null));
        assertNull(stringUtilityService.capitalize(null));
        
        // Test empty strings
        assertEquals("", stringUtilityService.reverse(""));
        assertEquals("", stringUtilityService.capitalize(""));
        
        // Test negative numbers
        assertThrows(IllegalArgumentException.class, () -> calculatorService.factorial(-1));
        assertThrows(IllegalArgumentException.class, () -> calculatorService.divide(10, 0));
    }
    
    @Test
    @Order(9)
    @DisplayName("Should demonstrate assumptions")
    void shouldDemonstrateAssumptions() {
        // This test will only run if the assumption is true
        assumeTrue(System.getProperty("os.name").toLowerCase().contains("mac") || 
                  System.getProperty("os.name").toLowerCase().contains("linux") ||
                  System.getProperty("os.name").toLowerCase().contains("windows"));
        
        // Test will continue only if assumption passes
        assertEquals(4, calculatorService.add(2, 2));
    }
    
    @Test
    @Order(10)
    @DisplayName("Should demonstrate timeout")
    @Timeout(value = 2, unit = java.util.concurrent.TimeUnit.SECONDS)
    void shouldCompleteWithinTimeout() {
        // This test must complete within 2 seconds
        int result = calculatorService.factorial(10);
        assertTrue(result > 0);
    }
}

/**
 * Parameterized test examples
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParameterizedTestExamples {
    
    @Autowired
    private CalculatorService calculatorService;
    
    @Autowired
    private StringUtilityService stringUtilityService;
    
    @ParameterizedTest
    @Order(1)
    @DisplayName("Should add numbers correctly with different inputs")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void shouldAddNumbersCorrectly(int number) {
        int result = calculatorService.add(number, 10);
        assertTrue(result > 10);
        assertEquals(number + 10, result);
    }
    
    @ParameterizedTest
    @Order(2)
    @DisplayName("Should identify prime numbers with different inputs")
    @ValueSource(ints = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29})
    void shouldIdentifyPrimeNumbers(int number) {
        assertTrue(calculatorService.isPrime(number), number + " should be prime");
    }
    
    @ParameterizedTest
    @Order(3)
    @DisplayName("Should identify non-prime numbers with different inputs")
    @ValueSource(ints = {1, 4, 6, 8, 9, 10, 12, 14, 15, 16})
    void shouldIdentifyNonPrimeNumbers(int number) {
        assertFalse(calculatorService.isPrime(number), number + " should not be prime");
    }
    
    @ParameterizedTest
    @Order(4)
    @DisplayName("Should perform calculations with CSV data")
    @CsvSource({
        "1, 2, 3",
        "5, 5, 10",
        "10, 15, 25",
        "100, 200, 300",
        "-5, 10, 5"
    })
    void shouldPerformCalculationsWithCsvData(int a, int b, int expected) {
        assertEquals(expected, calculatorService.add(a, b));
    }
    
    @ParameterizedTest
    @Order(5)
    @DisplayName("Should handle division with CSV data")
    @CsvSource({
        "10, 2, 5.0",
        "15, 3, 5.0",
        "20, 4, 5.0",
        "100, 10, 10.0"
    })
    void shouldHandleDivisionWithCsvData(int dividend, int divisor, double expected) {
        assertEquals(expected, calculatorService.divide(dividend, divisor), 0.001);
    }
    
    @ParameterizedTest
    @Order(6)
    @DisplayName("Should capitalize strings with method source")
    @MethodSource("stringProvider")
    void shouldCapitalizeStringsWithMethodSource(String input, String expected) {
        assertEquals(expected, stringUtilityService.capitalize(input));
    }
    
    static Stream<org.junit.jupiter.params.provider.Arguments> stringProvider() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("hello", "Hello"),
            org.junit.jupiter.params.provider.Arguments.of("WORLD", "World"),
            org.junit.jupiter.params.provider.Arguments.of("jAvA", "Java"),
            org.junit.jupiter.params.provider.Arguments.of("tEsT", "Test")
        );
    }
    
    @ParameterizedTest
    @Order(7)
    @DisplayName("Should identify palindromes with method source")
    @MethodSource("palindromeProvider")
    void shouldIdentifyPalindromesWithMethodSource(String input, boolean expected) {
        assertEquals(expected, stringUtilityService.isPalindrome(input));
    }
    
    static Stream<org.junit.jupiter.params.provider.Arguments> palindromeProvider() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("racecar", true),
            org.junit.jupiter.params.provider.Arguments.of("hello", false),
            org.junit.jupiter.params.provider.Arguments.of("madam", true),
            org.junit.jupiter.params.provider.Arguments.of("A man a plan a canal Panama", true),
            org.junit.jupiter.params.provider.Arguments.of("race a car", false),
            org.junit.jupiter.params.provider.Arguments.of("", false)
        );
    }
}

/**
 * Conditional test execution examples
 */
@SpringBootTest
class ConditionalTestExamples {
    
    @Autowired
    private CalculatorService calculatorService;
    
    @Test
    @org.junit.jupiter.api.condition.EnabledOnOs(org.junit.jupiter.api.condition.OS.MAC)
    @DisplayName("Should run only on macOS")
    void shouldRunOnlyOnMacOS() {
        assertEquals(4, calculatorService.add(2, 2));
    }
    
    @Test
    @org.junit.jupiter.api.condition.EnabledOnOs({org.junit.jupiter.api.condition.OS.LINUX, org.junit.jupiter.api.condition.OS.MAC})
    @DisplayName("Should run on Linux or macOS")
    void shouldRunOnLinuxOrMacOS() {
        assertEquals(6, calculatorService.add(2, 4));
    }
    
    @Test
    @org.junit.jupiter.api.condition.EnabledOnJre(org.junit.jupiter.api.condition.JRE.JAVA_11)
    @DisplayName("Should run only on Java 11")
    void shouldRunOnlyOnJava11() {
        assertEquals(8, calculatorService.add(3, 5));
    }
    
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "test.environment", matches = "integration")
    @DisplayName("Should run only in integration environment")
    void shouldRunOnlyInIntegrationEnvironment() {
        assertEquals(10, calculatorService.add(4, 6));
    }
    
    @Test
    @org.junit.jupiter.api.condition.DisabledIfSystemProperty(named = "test.skip", matches = "true")
    @DisplayName("Should be disabled if test.skip is true")
    void shouldBeDisabledIfTestSkipIsTrue() {
        assertEquals(12, calculatorService.add(5, 7));
    }
    
    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "TEST_ENV", matches = "development")
    @DisplayName("Should run only in development environment")
    void shouldRunOnlyInDevelopmentEnvironment() {
        assertEquals(14, calculatorService.add(6, 8));
    }
}

/**
 * Test instance lifecycle examples
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestInstanceLifecycleExamples {
    
    @Autowired
    private CalculatorService calculatorService;
    
    private int counter;
    
    @BeforeAll
    void initializeCounter() {
        counter = 0;
        System.out.println("Counter initialized to: " + counter);
    }
    
    @Test
    @DisplayName("Should increment counter - Test 1")
    void shouldIncrementCounterTest1() {
        counter++;
        assertEquals(1, counter);
        System.out.println("Counter after test 1: " + counter);
    }
    
    @Test
    @DisplayName("Should increment counter - Test 2")
    void shouldIncrementCounterTest2() {
        counter++;
        assertEquals(2, counter);
        System.out.println("Counter after test 2: " + counter);
    }
    
    @Test
    @DisplayName("Should increment counter - Test 3")
    void shouldIncrementCounterTest3() {
        counter++;
        assertEquals(3, counter);
        System.out.println("Counter after test 3: " + counter);
    }
    
    @AfterAll
    void printFinalCounter() {
        System.out.println("Final counter value: " + counter);
    }
}
```

---

## 🌐 Part 2: Web Layer Testing with MockMvc (45 minutes)

### **MockMvc Testing Patterns**

Create `MockMvcTestDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

/**
 * MockMvc testing demonstration
 * Shows comprehensive web layer testing techniques
 */
@SpringBootApplication
public class MockMvcTestDemo {
    
    public static void main(String[] args) {
        System.out.println("=== MockMvc Testing Demo ===\n");
        SpringApplication.run(MockMvcTestDemo.class, args);
    }
}

// Book entity for testing
class Book {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private double price;
    private LocalDateTime publishedDate;
    private String category;
    private boolean available;
    
    public Book() {}
    
    public Book(Long id, String title, String author, String isbn, double price, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.category = category;
        this.available = true;
        this.publishedDate = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public LocalDateTime getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDateTime publishedDate) { this.publishedDate = publishedDate; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}

// Book service interface
interface BookService {
    List<Book> findAllBooks();
    Optional<Book> findBookById(Long id);
    Book createBook(Book book);
    Book updateBook(Long id, Book book);
    void deleteBook(Long id);
    List<Book> findBooksByAuthor(String author);
    List<Book> findBooksByCategory(String category);
}

// Book controller for testing
@RestController
@RequestMapping("/api/books")
class BookController {
    
    private final BookService bookService;
    
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }
    
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.findAllBooks();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.findBookById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/author/{author}")
    public List<Book> getBooksByAuthor(@PathVariable String author) {
        return bookService.findBooksByAuthor(author);
    }
    
    @GetMapping("/category/{category}")
    public List<Book> getBooksByCategory(@PathVariable String category) {
        return bookService.findBooksByCategory(category);
    }
    
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book createdBook = bookService.createBook(book);
        return ResponseEntity.ok(createdBook);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        try {
            Book updatedBook = bookService.updateBook(id, book);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam(required = false) String title,
                                 @RequestParam(required = false) String author,
                                 @RequestParam(required = false) String category) {
        
        List<Book> allBooks = bookService.findAllBooks();
        
        if (title != null && !title.isEmpty()) {
            allBooks = allBooks.stream()
                .filter(book -> book.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        if (author != null && !author.isEmpty()) {
            allBooks = allBooks.stream()
                .filter(book -> book.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        if (category != null && !category.isEmpty()) {
            allBooks = allBooks.stream()
                .filter(book -> book.getCategory().equalsIgnoreCase(category))
                .collect(java.util.stream.Collectors.toList());
        }
        
        return allBooks;
    }
}

/**
 * MockMvc test class demonstrating web layer testing
 */
@WebMvcTest(BookController.class)
class BookControllerMockMvcTest {
    
    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private BookService bookService;
    
    private Book sampleBook;
    private List<Book> sampleBooks;
    
    @BeforeEach
    void setUp() {
        sampleBook = new Book(1L, "Java Programming", "John Doe", "978-1234567890", 49.99, "Programming");
        
        sampleBooks = Arrays.asList(
            new Book(1L, "Java Programming", "John Doe", "978-1234567890", 49.99, "Programming"),
            new Book(2L, "Spring Boot Guide", "Jane Smith", "978-0987654321", 39.99, "Programming"),
            new Book(3L, "Database Design", "Bob Johnson", "978-1122334455", 54.99, "Database")
        );
    }
    
    @Nested
    @DisplayName("GET /api/books")
    class GetAllBooksTests {
        
        @Test
        @DisplayName("Should return all books")
        void shouldReturnAllBooks() throws Exception {
            // Given
            given(bookService.findAllBooks()).willReturn(sampleBooks);
            
            // When & Then
            mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Java Programming")))
                .andExpect(jsonPath("$[0].author", is("John Doe")))
                .andExpect(jsonPath("$[0].price", is(49.99)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].title", is("Spring Boot Guide")))
                .andExpect(jsonPath("$[2].id", is(3)))
                .andExpect(jsonPath("$[2].title", is("Database Design")))
                .andDo(print());
        }
        
        @Test
        @DisplayName("Should return empty list when no books exist")
        void shouldReturnEmptyListWhenNoBooksExist() throws Exception {
            // Given
            given(bookService.findAllBooks()).willReturn(Arrays.asList());
            
            // When & Then
            mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)))
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("GET /api/books/{id}")
    class GetBookByIdTests {
        
        @Test
        @DisplayName("Should return book when found")
        void shouldReturnBookWhenFound() throws Exception {
            // Given
            given(bookService.findBookById(1L)).willReturn(Optional.of(sampleBook));
            
            // When & Then
            mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Java Programming")))
                .andExpect(jsonPath("$.author", is("John Doe")))
                .andExpect(jsonPath("$.isbn", is("978-1234567890")))
                .andExpect(jsonPath("$.price", is(49.99)))
                .andExpect(jsonPath("$.category", is("Programming")))
                .andDo(print());
        }
        
        @Test
        @DisplayName("Should return 404 when book not found")
        void shouldReturn404WhenBookNotFound() throws Exception {
            // Given
            given(bookService.findBookById(999L)).willReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("POST /api/books")
    class CreateBookTests {
        
        @Test
        @DisplayName("Should create book successfully")
        void shouldCreateBookSuccessfully() throws Exception {
            // Given
            given(bookService.createBook(any(Book.class))).willReturn(sampleBook);
            
            String bookJson = """
                {
                    "title": "Java Programming",
                    "author": "John Doe",
                    "isbn": "978-1234567890",
                    "price": 49.99,
                    "category": "Programming"
                }
                """;
            
            // When & Then
            mockMvc.perform(post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Java Programming")))
                .andExpect(jsonPath("$.author", is("John Doe")))
                .andExpect(jsonPath("$.price", is(49.99)))
                .andDo(print());
            
            // Verify service method was called
            verify(bookService).createBook(any(Book.class));
        }
        
        @Test
        @DisplayName("Should handle invalid JSON")
        void shouldHandleInvalidJson() throws Exception {
            // Given
            String invalidJson = "{invalid json}";
            
            // When & Then
            mockMvc.perform(post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("PUT /api/books/{id}")
    class UpdateBookTests {
        
        @Test
        @DisplayName("Should update book successfully")
        void shouldUpdateBookSuccessfully() throws Exception {
            // Given
            Book updatedBook = new Book(1L, "Updated Java Programming", "John Doe", "978-1234567890", 59.99, "Programming");
            given(bookService.updateBook(eq(1L), any(Book.class))).willReturn(updatedBook);
            
            String bookJson = """
                {
                    "title": "Updated Java Programming",
                    "author": "John Doe",
                    "isbn": "978-1234567890",
                    "price": 59.99,
                    "category": "Programming"
                }
                """;
            
            // When & Then
            mockMvc.perform(put("/api/books/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Java Programming")))
                .andExpect(jsonPath("$.price", is(59.99)))
                .andDo(print());
            
            // Verify service method was called with correct arguments
            verify(bookService).updateBook(eq(1L), any(Book.class));
        }
        
        @Test
        @DisplayName("Should return 404 when updating non-existent book")
        void shouldReturn404WhenUpdatingNonExistentBook() throws Exception {
            // Given
            given(bookService.updateBook(eq(999L), any(Book.class))).willThrow(new RuntimeException("Book not found"));
            
            String bookJson = """
                {
                    "title": "Non-existent Book",
                    "author": "Unknown",
                    "isbn": "978-0000000000",
                    "price": 29.99,
                    "category": "Unknown"
                }
                """;
            
            // When & Then
            mockMvc.perform(put("/api/books/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookJson))
                .andExpect(status().isNotFound())
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("DELETE /api/books/{id}")
    class DeleteBookTests {
        
        @Test
        @DisplayName("Should delete book successfully")
        void shouldDeleteBookSuccessfully() throws Exception {
            // Given
            doNothing().when(bookService).deleteBook(1L);
            
            // When & Then
            mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent())
                .andDo(print());
            
            // Verify service method was called
            verify(bookService).deleteBook(1L);
        }
        
        @Test
        @DisplayName("Should return 404 when deleting non-existent book")
        void shouldReturn404WhenDeletingNonExistentBook() throws Exception {
            // Given
            doThrow(new RuntimeException("Book not found")).when(bookService).deleteBook(999L);
            
            // When & Then
            mockMvc.perform(delete("/api/books/999"))
                .andExpect(status().isNotFound())
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("GET /api/books/search")
    class SearchBooksTests {
        
        @Test
        @DisplayName("Should search books by title")
        void shouldSearchBooksByTitle() throws Exception {
            // Given
            List<Book> javaBooks = Arrays.asList(sampleBooks.get(0));
            given(bookService.findAllBooks()).willReturn(sampleBooks);
            
            // When & Then
            mockMvc.perform(get("/api/books/search")
                    .param("title", "Java"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Java")))
                .andDo(print());
        }
        
        @Test
        @DisplayName("Should search books by author")
        void shouldSearchBooksByAuthor() throws Exception {
            // Given
            given(bookService.findAllBooks()).willReturn(sampleBooks);
            
            // When & Then
            mockMvc.perform(get("/api/books/search")
                    .param("author", "John"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].author", containsString("John")))
                .andDo(print());
        }
        
        @Test
        @DisplayName("Should search books by category")
        void shouldSearchBooksByCategory() throws Exception {
            // Given
            given(bookService.findAllBooks()).willReturn(sampleBooks);
            
            // When & Then
            mockMvc.perform(get("/api/books/search")
                    .param("category", "Programming"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].category", is("Programming")))
                .andExpect(jsonPath("$[1].category", is("Programming")))
                .andDo(print());
        }
        
        @Test
        @DisplayName("Should return all books when no search parameters provided")
        void shouldReturnAllBooksWhenNoSearchParameters() throws Exception {
            // Given
            given(bookService.findAllBooks()).willReturn(sampleBooks);
            
            // When & Then
            mockMvc.perform(get("/api/books/search"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andDo(print());
        }
    }
    
    @Test
    @DisplayName("Should verify service interactions with argument captors")
    void shouldVerifyServiceInteractionsWithArgumentCaptors() throws Exception {
        // Given
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        given(bookService.createBook(any(Book.class))).willReturn(sampleBook);
        
        String bookJson = """
            {
                "title": "Captured Book",
                "author": "Captured Author",
                "isbn": "978-1111111111",
                "price": 25.99,
                "category": "Test"
            }
            """;
        
        // When
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookJson))
            .andExpect(status().isOk());
        
        // Then
        verify(bookService).createBook(bookCaptor.capture());
        Book capturedBook = bookCaptor.getValue();
        
        assertEquals("Captured Book", capturedBook.getTitle());
        assertEquals("Captured Author", capturedBook.getAuthor());
        assertEquals("978-1111111111", capturedBook.getIsbn());
        assertEquals(25.99, capturedBook.getPrice());
        assertEquals("Test", capturedBook.getCategory());
    }
}
```

---

## 🗄️ Part 3: Data Layer Testing with @DataJpaTest (30 minutes)

### **Repository Testing Patterns**

Create `RepositoryTestDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository testing demonstration
 * Shows data layer testing with @DataJpaTest
 */
@SpringBootApplication
public class RepositoryTestDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Repository Testing Demo ===\n");
        SpringApplication.run(RepositoryTestDemo.class, args);
    }
}

// Author entity
@Entity
@Table(name = "authors")
class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column
    private String bio;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookEntity> books;
    
    public Author() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Author(String firstName, String lastName, String email) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<BookEntity> getBooks() { return books; }
    public void setBooks(List<BookEntity> books) { this.books = books; }
}

// Book entity for repository testing
@Entity
@Table(name = "books")
class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String isbn;
    
    @Column(nullable = false)
    private Double price;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    private Boolean available;
    
    @Column(nullable = false)
    private LocalDateTime publishedDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;
    
    public BookEntity() {
        this.available = true;
        this.publishedDate = LocalDateTime.now();
    }
    
    public BookEntity(String title, String isbn, Double price, String category, Author author) {
        this();
        this.title = title;
        this.isbn = isbn;
        this.price = price;
        this.category = category;
        this.author = author;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    
    public LocalDateTime getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDateTime publishedDate) { this.publishedDate = publishedDate; }
    
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
}

// Author repository
interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByEmail(String email);
    List<Author> findByFirstNameContainingIgnoreCase(String firstName);
    List<Author> findByLastNameContainingIgnoreCase(String lastName);
    
    @Query("SELECT a FROM Author a WHERE a.firstName = ?1 AND a.lastName = ?2")
    Optional<Author> findByFullName(String firstName, String lastName);
    
    @Query("SELECT a FROM Author a WHERE SIZE(a.books) > :minBooks")
    List<Author> findAuthorsWithMoreThanBooks(@Param("minBooks") int minBooks);
}

// Book repository
interface BookRepository extends JpaRepository<BookEntity, Long> {
    List<BookEntity> findByCategory(String category);
    List<BookEntity> findByAvailableTrue();
    List<BookEntity> findByPriceBetween(Double minPrice, Double maxPrice);
    List<BookEntity> findByAuthor(Author author);
    Optional<BookEntity> findByIsbn(String isbn);
    
    @Query("SELECT b FROM BookEntity b WHERE b.title LIKE %:title%")
    List<BookEntity> findByTitleContaining(@Param("title") String title);
    
    @Query("SELECT b FROM BookEntity b WHERE b.author.firstName = :firstName AND b.author.lastName = :lastName")
    List<BookEntity> findByAuthorName(@Param("firstName") String firstName, @Param("lastName") String lastName);
    
    @Query("SELECT AVG(b.price) FROM BookEntity b WHERE b.category = :category")
    Double findAveragePriceByCategory(@Param("category") String category);
    
    @Query("SELECT b.category, COUNT(b) FROM BookEntity b GROUP BY b.category")
    List<Object[]> findCategoryStatistics();
}

/**
 * Author repository test class
 */
@DataJpaTest
class AuthorRepositoryTest {
    
    @org.springframework.beans.factory.annotation.Autowired
    private TestEntityManager entityManager;
    
    @org.springframework.beans.factory.annotation.Autowired
    private AuthorRepository authorRepository;
    
    private Author author1;
    private Author author2;
    
    @BeforeEach
    void setUp() {
        author1 = new Author("John", "Doe", "john.doe@example.com");
        author1.setBio("Experienced Java developer and author");
        
        author2 = new Author("Jane", "Smith", "jane.smith@example.com");
        author2.setBio("Spring framework expert");
        
        entityManager.persistAndFlush(author1);
        entityManager.persistAndFlush(author2);
    }
    
    @Nested
    @DisplayName("Finding authors")
    class FindingAuthors {
        
        @Test
        @DisplayName("Should find all authors")
        void shouldFindAllAuthors() {
            // When
            List<Author> authors = authorRepository.findAll();
            
            // Then
            assertThat(authors).hasSize(2);
            assertThat(authors)
                .extracting(Author::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
        }
        
        @Test
        @DisplayName("Should find author by email")
        void shouldFindAuthorByEmail() {
            // When
            Optional<Author> found = authorRepository.findByEmail("john.doe@example.com");
            
            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getLastName()).isEqualTo("Doe");
        }
        
        @Test
        @DisplayName("Should return empty when author not found by email")
        void shouldReturnEmptyWhenAuthorNotFoundByEmail() {
            // When
            Optional<Author> found = authorRepository.findByEmail("nonexistent@example.com");
            
            // Then
            assertThat(found).isEmpty();
        }
        
        @Test
        @DisplayName("Should find authors by first name containing")
        void shouldFindAuthorsByFirstNameContaining() {
            // When
            List<Author> authors = authorRepository.findByFirstNameContainingIgnoreCase("jo");
            
            // Then
            assertThat(authors).hasSize(1);
            assertThat(authors.get(0).getFirstName()).isEqualTo("John");
        }
        
        @Test
        @DisplayName("Should find author by full name")
        void shouldFindAuthorByFullName() {
            // When
            Optional<Author> found = authorRepository.findByFullName("John", "Doe");
            
            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
        }
    }
    
    @Nested
    @DisplayName("Creating and updating authors")
    class CreatingAndUpdatingAuthors {
        
        @Test
        @DisplayName("Should save new author")
        void shouldSaveNewAuthor() {
            // Given
            Author newAuthor = new Author("Bob", "Johnson", "bob.johnson@example.com");
            
            // When
            Author saved = authorRepository.save(newAuthor);
            
            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getFirstName()).isEqualTo("Bob");
            assertThat(saved.getLastName()).isEqualTo("Johnson");
            assertThat(saved.getEmail()).isEqualTo("bob.johnson@example.com");
            assertThat(saved.getCreatedAt()).isNotNull();
        }
        
        @Test
        @DisplayName("Should update existing author")
        void shouldUpdateExistingAuthor() {
            // Given
            Author existing = authorRepository.findByEmail("john.doe@example.com").get();
            existing.setBio("Updated bio");
            
            // When
            Author updated = authorRepository.save(existing);
            
            // Then
            assertThat(updated.getBio()).isEqualTo("Updated bio");
            assertThat(updated.getId()).isEqualTo(existing.getId());
        }
        
        @Test
        @DisplayName("Should delete author")
        void shouldDeleteAuthor() {
            // Given
            Author toDelete = authorRepository.findByEmail("john.doe@example.com").get();
            
            // When
            authorRepository.delete(toDelete);
            
            // Then
            Optional<Author> found = authorRepository.findByEmail("john.doe@example.com");
            assertThat(found).isEmpty();
        }
    }
}

/**
 * Book repository test class
 */
@DataJpaTest
class BookRepositoryTest {
    
    @org.springframework.beans.factory.annotation.Autowired
    private TestEntityManager entityManager;
    
    @org.springframework.beans.factory.annotation.Autowired
    private BookRepository bookRepository;
    
    private Author author1;
    private Author author2;
    private BookEntity book1;
    private BookEntity book2;
    private BookEntity book3;
    
    @BeforeEach
    void setUp() {
        // Create authors
        author1 = new Author("John", "Doe", "john.doe@example.com");
        author2 = new Author("Jane", "Smith", "jane.smith@example.com");
        
        entityManager.persistAndFlush(author1);
        entityManager.persistAndFlush(author2);
        
        // Create books
        book1 = new BookEntity("Java Programming", "978-1234567890", 49.99, "Programming", author1);
        book2 = new BookEntity("Spring Boot Guide", "978-0987654321", 39.99, "Programming", author2);
        book3 = new BookEntity("Database Design", "978-1122334455", 59.99, "Database", author1);
        
        entityManager.persistAndFlush(book1);
        entityManager.persistAndFlush(book2);
        entityManager.persistAndFlush(book3);
    }
    
    @Nested
    @DisplayName("Finding books")
    class FindingBooks {
        
        @Test
        @DisplayName("Should find all books")
        void shouldFindAllBooks() {
            // When
            List<BookEntity> books = bookRepository.findAll();
            
            // Then
            assertThat(books).hasSize(3);
            assertThat(books)
                .extracting(BookEntity::getTitle)
                .containsExactlyInAnyOrder("Java Programming", "Spring Boot Guide", "Database Design");
        }
        
        @Test
        @DisplayName("Should find books by category")
        void shouldFindBooksByCategory() {
            // When
            List<BookEntity> programmingBooks = bookRepository.findByCategory("Programming");
            
            // Then
            assertThat(programmingBooks).hasSize(2);
            assertThat(programmingBooks)
                .extracting(BookEntity::getTitle)
                .containsExactlyInAnyOrder("Java Programming", "Spring Boot Guide");
        }
        
        @Test
        @DisplayName("Should find available books")
        void shouldFindAvailableBooks() {
            // When
            List<BookEntity> availableBooks = bookRepository.findByAvailableTrue();
            
            // Then
            assertThat(availableBooks).hasSize(3);
        }
        
        @Test
        @DisplayName("Should find books by price range")
        void shouldFindBooksByPriceRange() {
            // When
            List<BookEntity> booksInRange = bookRepository.findByPriceBetween(40.0, 50.0);
            
            // Then
            assertThat(booksInRange).hasSize(1);
            assertThat(booksInRange.get(0).getTitle()).isEqualTo("Java Programming");
        }
        
        @Test
        @DisplayName("Should find books by author")
        void shouldFindBooksByAuthor() {
            // When
            List<BookEntity> johnBooks = bookRepository.findByAuthor(author1);
            
            // Then
            assertThat(johnBooks).hasSize(2);
            assertThat(johnBooks)
                .extracting(BookEntity::getTitle)
                .containsExactlyInAnyOrder("Java Programming", "Database Design");
        }
        
        @Test
        @DisplayName("Should find book by ISBN")
        void shouldFindBookByIsbn() {
            // When
            Optional<BookEntity> found = bookRepository.findByIsbn("978-1234567890");
            
            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Java Programming");
        }
        
        @Test
        @DisplayName("Should find books by title containing")
        void shouldFindBooksByTitleContaining() {
            // When
            List<BookEntity> books = bookRepository.findByTitleContaining("Java");
            
            // Then
            assertThat(books).hasSize(1);
            assertThat(books.get(0).getTitle()).isEqualTo("Java Programming");
        }
        
        @Test
        @DisplayName("Should find books by author name")
        void shouldFindBooksByAuthorName() {
            // When
            List<BookEntity> books = bookRepository.findByAuthorName("John", "Doe");
            
            // Then
            assertThat(books).hasSize(2);
            assertThat(books)
                .extracting(BookEntity::getTitle)
                .containsExactlyInAnyOrder("Java Programming", "Database Design");
        }
    }
    
    @Nested
    @DisplayName("Book statistics")
    class BookStatistics {
        
        @Test
        @DisplayName("Should calculate average price by category")
        void shouldCalculateAveragePriceByCategory() {
            // When
            Double averagePrice = bookRepository.findAveragePriceByCategory("Programming");
            
            // Then
            assertThat(averagePrice).isEqualTo(44.99);
        }
        
        @Test
        @DisplayName("Should get category statistics")
        void shouldGetCategoryStatistics() {
            // When
            List<Object[]> statistics = bookRepository.findCategoryStatistics();
            
            // Then
            assertThat(statistics).hasSize(2);
            
            // Find programming category stats
            Object[] programmingStats = statistics.stream()
                .filter(stat -> "Programming".equals(stat[0]))
                .findFirst()
                .orElse(null);
            
            assertThat(programmingStats).isNotNull();
            assertThat(programmingStats[0]).isEqualTo("Programming");
            assertThat(programmingStats[1]).isEqualTo(2L);
        }
    }
    
    @Nested
    @DisplayName("Creating and updating books")
    class CreatingAndUpdatingBooks {
        
        @Test
        @DisplayName("Should save new book")
        void shouldSaveNewBook() {
            // Given
            BookEntity newBook = new BookEntity("Test Book", "978-9999999999", 29.99, "Test", author1);
            
            // When
            BookEntity saved = bookRepository.save(newBook);
            
            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTitle()).isEqualTo("Test Book");
            assertThat(saved.getIsbn()).isEqualTo("978-9999999999");
            assertThat(saved.getPrice()).isEqualTo(29.99);
            assertThat(saved.getCategory()).isEqualTo("Test");
            assertThat(saved.getAvailable()).isTrue();
            assertThat(saved.getPublishedDate()).isNotNull();
        }
        
        @Test
        @DisplayName("Should update existing book")
        void shouldUpdateExistingBook() {
            // Given
            BookEntity existing = bookRepository.findByIsbn("978-1234567890").get();
            existing.setPrice(54.99);
            existing.setAvailable(false);
            
            // When
            BookEntity updated = bookRepository.save(existing);
            
            // Then
            assertThat(updated.getPrice()).isEqualTo(54.99);
            assertThat(updated.getAvailable()).isFalse();
            assertThat(updated.getId()).isEqualTo(existing.getId());
        }
        
        @Test
        @DisplayName("Should delete book")
        void shouldDeleteBook() {
            // Given
            BookEntity toDelete = bookRepository.findByIsbn("978-1234567890").get();
            
            // When
            bookRepository.delete(toDelete);
            
            // Then
            Optional<BookEntity> found = bookRepository.findByIsbn("978-1234567890");
            assertThat(found).isEmpty();
        }
    }
}
```

---

## 🔧 Part 4: Integration Testing & Test Configuration (30 minutes)

### **Complete Integration Testing**

Create `IntegrationTestDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration testing demonstration
 * Shows full application context testing with real HTTP requests
 */
@SpringBootApplication
public class IntegrationTestDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Integration Testing Demo ===\n");
        SpringApplication.run(IntegrationTestDemo.class, args);
    }
}

/**
 * Full integration test class
 * Tests the complete application stack
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.springframework.web=DEBUG"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/books";
    }
    
    @Test
    @DisplayName("Should perform complete book CRUD operations")
    void shouldPerformCompleteBookCrudOperations() {
        // 1. Create a book
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("title", "Integration Test Book");
        bookData.put("author", "Test Author");
        bookData.put("isbn", "978-1111111111");
        bookData.put("price", 35.99);
        bookData.put("category", "Testing");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(bookData, headers);
        
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(baseUrl, createRequest, Map.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).isNotNull();
        
        // Extract book ID from response
        Map<String, Object> createdBook = createResponse.getBody();
        Object bookId = createdBook.get("id");
        
        // 2. Read the created book
        ResponseEntity<Map> readResponse = restTemplate.getForEntity(baseUrl + "/" + bookId, Map.class);
        
        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readResponse.getBody()).isNotNull();
        
        Map<String, Object> readBook = readResponse.getBody();
        assertThat(readBook.get("title")).isEqualTo("Integration Test Book");
        assertThat(readBook.get("author")).isEqualTo("Test Author");
        assertThat(readBook.get("isbn")).isEqualTo("978-1111111111");
        assertThat(readBook.get("price")).isEqualTo(35.99);
        assertThat(readBook.get("category")).isEqualTo("Testing");
        
        // 3. Update the book
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("title", "Updated Integration Test Book");
        updateData.put("author", "Updated Test Author");
        updateData.put("isbn", "978-1111111111");
        updateData.put("price", 39.99);
        updateData.put("category", "Updated Testing");
        
        HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(updateData, headers);
        
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
            baseUrl + "/" + bookId,
            org.springframework.http.HttpMethod.PUT,
            updateRequest,
            Map.class
        );
        
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        
        Map<String, Object> updatedBook = updateResponse.getBody();
        assertThat(updatedBook.get("title")).isEqualTo("Updated Integration Test Book");
        assertThat(updatedBook.get("author")).isEqualTo("Updated Test Author");
        assertThat(updatedBook.get("price")).isEqualTo(39.99);
        assertThat(updatedBook.get("category")).isEqualTo("Updated Testing");
        
        // 4. Delete the book
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            baseUrl + "/" + bookId,
            org.springframework.http.HttpMethod.DELETE,
            null,
            Void.class
        );
        
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // 5. Verify deletion
        ResponseEntity<Map> verifyResponse = restTemplate.getForEntity(baseUrl + "/" + bookId, Map.class);
        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    @DisplayName("Should handle search operations")
    void shouldHandleSearchOperations() {
        // First, create some test books
        createTestBook("Java Fundamentals", "John Doe", "Programming");
        createTestBook("Spring Boot Mastery", "Jane Smith", "Programming");
        createTestBook("Database Systems", "Bob Johnson", "Database");
        
        // Test search by title
        ResponseEntity<Object[]> titleSearchResponse = restTemplate.getForEntity(
            baseUrl + "/search?title=Java",
            Object[].class
        );
        
        assertThat(titleSearchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(titleSearchResponse.getBody()).isNotNull();
        assertThat(titleSearchResponse.getBody()).hasSize(1);
        
        // Test search by author
        ResponseEntity<Object[]> authorSearchResponse = restTemplate.getForEntity(
            baseUrl + "/search?author=John",
            Object[].class
        );
        
        assertThat(authorSearchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authorSearchResponse.getBody()).isNotNull();
        assertThat(authorSearchResponse.getBody()).hasSize(1);
        
        // Test search by category
        ResponseEntity<Object[]> categorySearchResponse = restTemplate.getForEntity(
            baseUrl + "/search?category=Programming",
            Object[].class
        );
        
        assertThat(categorySearchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categorySearchResponse.getBody()).isNotNull();
        assertThat(categorySearchResponse.getBody()).hasSize(2);
    }
    
    @Test
    @DisplayName("Should handle error scenarios")
    void shouldHandleErrorScenarios() {
        // Test 404 for non-existent book
        ResponseEntity<Map> notFoundResponse = restTemplate.getForEntity(baseUrl + "/999", Map.class);
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        // Test invalid JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> invalidRequest = new HttpEntity<>("{invalid json}", headers);
        
        ResponseEntity<Map> invalidResponse = restTemplate.postForEntity(baseUrl, invalidRequest, Map.class);
        assertThat(invalidResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Test delete non-existent book
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            baseUrl + "/999",
            org.springframework.http.HttpMethod.DELETE,
            null,
            Void.class
        );
        
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    private void createTestBook(String title, String author, String category) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("isbn", "978-" + System.currentTimeMillis());
        bookData.put("price", 29.99);
        bookData.put("category", category);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(bookData, headers);
        
        restTemplate.postForEntity(baseUrl, request, Map.class);
    }
}

/**
 * Performance integration test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PerformanceIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/books";
    }
    
    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws InterruptedException {
        int numberOfThreads = 10;
        int requestsPerThread = 5;
        
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(numberOfThreads);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            Map<String, Object> bookData = new HashMap<>();
                            bookData.put("title", "Concurrent Book " + threadId + "-" + j);
                            bookData.put("author", "Thread Author " + threadId);
                            bookData.put("isbn", "978-" + threadId + String.format("%03d", j));
                            bookData.put("price", 25.99 + threadId);
                            bookData.put("category", "Concurrent");
                            
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            HttpEntity<Map<String, Object>> request = new HttpEntity<>(bookData, headers);
                            
                            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);
                            
                            if (response.getStatusCode() == HttpStatus.OK) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        
        assertThat(successCount.get()).isEqualTo(numberOfThreads * requestsPerThread);
        assertThat(errorCount.get()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should respond within acceptable time limits")
    void shouldRespondWithinAcceptableTimeLimits() {
        long startTime = System.currentTimeMillis();
        
        ResponseEntity<Object[]> response = restTemplate.getForEntity(baseUrl, Object[].class);
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseTime).isLessThan(1000L); // Should respond within 1 second
    }
}
```

---

## 📋 Practice Exercises

### Exercise 1: Complete Testing Suite
Create a comprehensive testing suite for a User Management System:
- Unit tests for service layer
- Repository tests with @DataJpaTest
- Web layer tests with MockMvc
- Integration tests with TestRestTemplate
- Performance tests for concurrent operations

### Exercise 2: Test Data Management
Implement proper test data management:
- @Sql for database initialization
- @TestConfiguration for test-specific beans
- Test profiles for different environments
- TestContainers for external services
- Custom test assertions and matchers

### Exercise 3: Advanced Testing Patterns
Implement advanced testing patterns:
- Contract testing with Spring Cloud Contract
- Property-based testing with junit-quickcheck
- Mutation testing with PIT
- Test coverage analysis with JaCoCo
- BDD testing with Cucumber

---

## 🎯 Key Takeaways

### Testing Best Practices:
1. **Test pyramid**: More unit tests, fewer integration tests
2. **Test isolation**: Each test should be independent
3. **Descriptive names**: Clear test method names and display names
4. **AAA pattern**: Arrange, Act, Assert structure
5. **Test data management**: Use builders and factories for test data

### Spring Boot Testing Features:
1. **@SpringBootTest**: Full application context testing
2. **Test slices**: Focused testing with minimal context
3. **MockMvc**: Web layer testing without HTTP server
4. **TestRestTemplate**: Integration testing with real HTTP
5. **@MockBean**: Mock Spring beans in tests

### JUnit 5 Features:
1. **Lifecycle methods**: @BeforeEach, @AfterEach, @BeforeAll, @AfterAll
2. **Assertions**: assertEquals, assertTrue, assertAll, assertThrows
3. **Parameterized tests**: @ValueSource, @CsvSource, @MethodSource
4. **Conditional execution**: @EnabledOnOs, @EnabledOnJre
5. **Nested tests**: @Nested for grouping related tests

### Testing Strategies:
1. **Unit testing**: Test individual components in isolation
2. **Integration testing**: Test component interactions
3. **Contract testing**: Test API contracts
4. **Performance testing**: Test under load and stress
5. **End-to-end testing**: Test complete user workflows

---

## 🚀 Next Steps

Tomorrow, we'll conclude Week 4 with the **Week 4 Capstone Project - Blog Application** (Day 28), where you'll:
- Integrate all Week 4 concepts into one application
- Build a complete blog platform with authentication
- Implement REST APIs and web interface
- Create comprehensive tests
- Deploy with proper configuration management

Your testing skills will be crucial for ensuring the quality and reliability of your capstone project! 🎯