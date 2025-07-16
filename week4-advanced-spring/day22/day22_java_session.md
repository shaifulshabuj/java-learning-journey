# Day 22: RESTful Web Services (July 31, 2025)

**Date:** July 31, 2025  
**Duration:** 2.5 hours  
**Focus:** Master REST API design principles, Spring REST controllers, and building production-ready APIs

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand REST architectural principles and constraints
- ✅ Master @RestController and HTTP method mappings
- ✅ Work with JSON serialization and request/response bodies
- ✅ Implement proper HTTP status codes and error handling
- ✅ Design RESTful APIs following best practices
- ✅ Build a complete Product Management REST API

---

## 🌐 Part 1: REST Architecture & Spring REST (45 minutes)

### **Understanding REST Principles**

**REST (Representational State Transfer)** is an architectural style that defines constraints for creating web services:

1. **Client-Server Architecture**: Separation of concerns
2. **Statelessness**: Each request contains all information needed
3. **Cacheability**: Responses must define themselves as cacheable or not
4. **Uniform Interface**: Consistent way to interact with resources
5. **Layered System**: Architecture composed of hierarchical layers
6. **Code on Demand** (optional): Server can extend client functionality

### **RESTful API Design Best Practices**

- **Resources**: Nouns, not verbs (e.g., `/users`, not `/getUsers`)
- **HTTP Methods**: GET (read), POST (create), PUT (update), DELETE (delete)
- **Status Codes**: Use appropriate HTTP status codes
- **Versioning**: Include version in URL or headers
- **Pagination**: Handle large datasets efficiently
- **Filtering**: Allow clients to filter results

### **Basic REST Controller Demo**

Create `BasicRestControllerDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Basic REST Controller demonstration
 * Shows fundamental REST API concepts with Spring Boot
 */
@SpringBootApplication
public class BasicRestControllerDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Basic REST Controller Demo ===\n");
        SpringApplication.run(BasicRestControllerDemo.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("🚀 REST API Started Successfully!");
        System.out.println("📍 Base URL: http://localhost:8080");
        System.out.println("\n📋 Available Endpoints:");
        System.out.println("   GET    /api/greeting");
        System.out.println("   GET    /api/greeting/{name}");
        System.out.println("   GET    /api/users");
        System.out.println("   GET    /api/users/{id}");
        System.out.println("   POST   /api/users");
        System.out.println("   PUT    /api/users/{id}");
        System.out.println("   DELETE /api/users/{id}");
        System.out.println();
    }
}

// Basic REST controller
@RestController
@RequestMapping("/api")
class GreetingController {
    
    // Simple GET endpoint
    @GetMapping("/greeting")
    public Map<String, Object> greeting() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, REST World!");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "success");
        return response;
    }
    
    // GET with path variable
    @GetMapping("/greeting/{name}")
    public Map<String, Object> personalGreeting(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", String.format("Hello, %s!", name));
        response.put("timestamp", LocalDateTime.now());
        response.put("personalized", true);
        return response;
    }
}

// User model
class User {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public User() {}
    
    public User(String username, String email, String fullName) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

// User REST controller with CRUD operations
@RestController
@RequestMapping("/api/users")
class UserController {
    
    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserController() {
        // Initialize with sample data
        createUser(new User("john_doe", "john@example.com", "John Doe"));
        createUser(new User("jane_smith", "jane@example.com", "Jane Smith"));
    }
    
    // GET all users
    @GetMapping
    public List<User> getAllUsers() {
        return new ArrayList<>(userDatabase.values());
    }
    
    // GET user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userDatabase.get(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // POST - Create new user
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        Long id = idGenerator.getAndIncrement();
        user.setId(id);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userDatabase.put(id, user);
        
        System.out.println("Created user: " + user.getUsername());
        return user;
    }
    
    // PUT - Update existing user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        User existingUser = userDatabase.get(id);
        if (existingUser != null) {
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setFullName(updatedUser.getFullName());
            existingUser.setUpdatedAt(LocalDateTime.now());
            
            System.out.println("Updated user: " + existingUser.getUsername());
            return ResponseEntity.ok(existingUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // DELETE - Remove user
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        User removedUser = userDatabase.remove(id);
        if (removedUser != null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("username", removedUser.getUsername());
            
            System.out.println("Deleted user: " + removedUser.getUsername());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
```

---

## 🔧 Part 2: Advanced REST Features (45 minutes)

### **HTTP Methods and Status Codes**

Create `RestApiDesignDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * REST API design demonstration
 * Shows proper HTTP methods, status codes, and API design patterns
 */
@SpringBootApplication
public class RestApiDesignDemo {
    
    public static void main(String[] args) {
        System.out.println("=== REST API Design Demo ===\n");
        SpringApplication.run(RestApiDesignDemo.class, args);
    }
}

// Product model with validation
class Product {
    private Long id;
    
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    private String name;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;
    
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;
    
    private String category;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors, getters, and setters
    public Product() {
        this.tags = new ArrayList<>();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

// API response wrapper
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
    
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

// Paginated response
class PagedResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    public PagedResponse(List<T> content, int pageNumber, int pageSize, long totalElements) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        this.first = pageNumber == 0;
        this.last = pageNumber >= totalPages - 1;
    }
    
    // Getters
    public List<T> getContent() { return content; }
    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isFirst() { return first; }
    public boolean isLast() { return last; }
}

// Advanced Product REST controller
@RestController
@RequestMapping("/api/v1/products")
@Validated
class ProductController {
    
    private final Map<Long, Product> productDatabase = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public ProductController() {
        // Initialize with sample products
        initializeSampleProducts();
    }
    
    private void initializeSampleProducts() {
        Product laptop = new Product();
        laptop.setName("Dell XPS 15");
        laptop.setDescription("High-performance laptop for professionals");
        laptop.setPrice(1499.99);
        laptop.setStockQuantity(50);
        laptop.setCategory("Electronics");
        laptop.setTags(Arrays.asList("laptop", "dell", "professional"));
        createProduct(laptop);
        
        Product phone = new Product();
        phone.setName("iPhone 13 Pro");
        phone.setDescription("Latest Apple smartphone with advanced features");
        phone.setPrice(999.99);
        phone.setStockQuantity(100);
        phone.setCategory("Electronics");
        phone.setTags(Arrays.asList("phone", "apple", "smartphone"));
        createProduct(phone);
    }
    
    // GET all products with pagination and filtering
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Product>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        
        List<Product> allProducts = new ArrayList<>(productDatabase.values());
        
        // Apply filters
        if (category != null && !category.isEmpty()) {
            allProducts = allProducts.stream()
                .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                .collect(Collectors.toList());
        }
        
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            allProducts = allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(searchLower) ||
                           p.getDescription().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }
        
        // Apply pagination
        int totalElements = allProducts.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        
        List<Product> pagedProducts = allProducts.subList(fromIndex, toIndex);
        PagedResponse<Product> pagedResponse = new PagedResponse<>(pagedProducts, page, size, totalElements);
        
        ApiResponse<PagedResponse<Product>> response = ApiResponse.success(pagedResponse, "Products retrieved successfully");
        response.getMetadata().put("filterApplied", category != null || search != null);
        
        return ResponseEntity.ok(response);
    }
    
    // GET product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        Product product = productDatabase.get(id);
        if (product != null) {
            return ResponseEntity.ok(ApiResponse.success(product, "Product found"));
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id);
        }
    }
    
    // POST - Create new product
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@Valid @RequestBody Product product) {
        Long id = idGenerator.getAndIncrement();
        product.setId(id);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productDatabase.put(id, product);
        
        ApiResponse<Product> response = ApiResponse.success(product, "Product created successfully");
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("Location", "/api/v1/products/" + id)
            .body(response);
    }
    
    // PUT - Full update
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id, 
            @Valid @RequestBody Product updatedProduct) {
        
        Product existingProduct = productDatabase.get(id);
        if (existingProduct == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id);
        }
        
        // Full update - replace all fields
        updatedProduct.setId(id);
        updatedProduct.setCreatedAt(existingProduct.getCreatedAt());
        updatedProduct.setUpdatedAt(LocalDateTime.now());
        productDatabase.put(id, updatedProduct);
        
        return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Product updated successfully"));
    }
    
    // PATCH - Partial update
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> partialUpdateProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        
        Product existingProduct = productDatabase.get(id);
        if (existingProduct == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id);
        }
        
        // Apply partial updates
        updates.forEach((key, value) -> {
            switch (key) {
                case "name":
                    existingProduct.setName((String) value);
                    break;
                case "description":
                    existingProduct.setDescription((String) value);
                    break;
                case "price":
                    existingProduct.setPrice(((Number) value).doubleValue());
                    break;
                case "stockQuantity":
                    existingProduct.setStockQuantity(((Number) value).intValue());
                    break;
                case "category":
                    existingProduct.setCategory((String) value);
                    break;
                case "tags":
                    existingProduct.setTags((List<String>) value);
                    break;
            }
        });
        
        existingProduct.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(existingProduct, "Product partially updated"));
    }
    
    // DELETE - Remove product
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        Product removedProduct = productDatabase.remove(id);
        if (removedProduct == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id);
        }
        
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }
    
    // HEAD - Check if product exists
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkProductExists(@PathVariable Long id) {
        if (productDatabase.containsKey(id)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // OPTIONS - Get allowed methods
    @RequestMapping(value = "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> getProductOptions(@PathVariable Long id) {
        return ResponseEntity.ok()
            .header("Allow", "GET, PUT, PATCH, DELETE, HEAD, OPTIONS")
            .build();
    }
}
```

---

## 🚨 Part 3: Error Handling & Exception Mapping (30 minutes)

### **Global Exception Handling**

Create `ErrorHandlingDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Error handling and exception mapping demonstration
 * Shows how to properly handle errors in REST APIs
 */
@SpringBootApplication
public class ErrorHandlingDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Error Handling Demo ===\n");
        SpringApplication.run(ErrorHandlingDemo.class, args);
    }
}

// Custom exception classes
class ResourceNotFoundException extends RuntimeException {
    private String resourceName;
    private String fieldName;
    private Object fieldValue;
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    // Getters
    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}

class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}

// Error response model
class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
    
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now().toString();
        this.validationErrors = new HashMap<>();
    }
    
    // Builder pattern for easy construction
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }
    
    // Getters and setters
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Map<String, String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(Map<String, String> validationErrors) { 
        this.validationErrors = validationErrors; 
    }
    
    // Builder class
    static class ErrorResponseBuilder {
        private ErrorResponse errorResponse = new ErrorResponse();
        
        public ErrorResponseBuilder status(HttpStatus status) {
            errorResponse.setStatus(status.value());
            errorResponse.setError(status.getReasonPhrase());
            return this;
        }
        
        public ErrorResponseBuilder message(String message) {
            errorResponse.setMessage(message);
            return this;
        }
        
        public ErrorResponseBuilder path(String path) {
            errorResponse.setPath(path);
            return this;
        }
        
        public ErrorResponseBuilder validationError(String field, String message) {
            errorResponse.getValidationErrors().put(field, message);
            return this;
        }
        
        public ErrorResponse build() {
            return errorResponse;
        }
    }
}

// Global exception handler
@RestControllerAdvice
class GlobalExceptionHandler {
    
    // Handle specific custom exceptions
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND)
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.CONFLICT)
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperationException(
            InvalidOperationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST)
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST)
            .message("Validation failed")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errorResponse.getValidationErrors().put(fieldName, errorMessage);
        });
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle constraint violations
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST)
            .message("Validation failed")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errorResponse.getValidationErrors().put(propertyPath, message);
        });
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle type mismatch
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
            
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST)
            .message(message)
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle 404 - Not Found
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND)
            .message("The requested resource was not found")
            .path(ex.getRequestURL())
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .message("An unexpected error occurred")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        // Log the actual exception for debugging
        System.err.println("Unexpected error: " + ex.getMessage());
        ex.printStackTrace();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// Sample controller to demonstrate error handling
@RestController
@RequestMapping("/api/demo")
class ErrorDemoController {
    
    private final Map<Long, String> dataStore = new HashMap<>();
    
    @GetMapping("/resource/{id}")
    public ResponseEntity<Map<String, Object>> getResource(@PathVariable Long id) {
        if (!dataStore.containsKey(id)) {
            throw new ResourceNotFoundException("Resource", "id", id);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("data", dataStore.get(id));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resource")
    public ResponseEntity<Map<String, Object>> createResource(@RequestBody Map<String, Object> data) {
        Long id = (Long) data.get("id");
        if (dataStore.containsKey(id)) {
            throw new DuplicateResourceException("Resource with id " + id + " already exists");
        }
        
        String value = (String) data.get("value");
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidOperationException("Value cannot be null or empty");
        }
        
        dataStore.put(id, value);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "Resource created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/error")
    public void triggerError() {
        throw new RuntimeException("This is a test error");
    }
}
```

---

## 🏗️ Part 4: Complete Product REST API (30 minutes)

### **Production-Ready REST API Application**

Create `ProductRestApiApplication.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Complete Product Management REST API
 * Production-ready implementation with all best practices
 */
@SpringBootApplication
public class ProductRestApiApplication {
    
    public static void main(String[] args) {
        System.out.println("=== Product Management REST API ===\n");
        SpringApplication.run(ProductRestApiApplication.class, args);
    }
    
    // Request logging configuration
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(10000);
        loggingFilter.setIncludeHeaders(false);
        loggingFilter.setAfterMessagePrefix("REQUEST DATA : ");
        return loggingFilter;
    }
}

// Enhanced Product entity
class ProductEntity {
    private Long id;
    
    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z]{3}-[0-9]{4}$", message = "SKU must match pattern XXX-0000")
    private String sku;
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100)
    private String name;
    
    @NotBlank(message = "Description is required")
    @Size(max = 1000)
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "999999.99")
    private Double price;
    
    @NotNull(message = "Stock is required")
    @Min(0)
    private Integer stock;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    private String brand;
    private List<String> tags = new ArrayList<>();
    private Map<String, String> attributes = new HashMap<>();
    
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Full constructor
    public ProductEntity() {}
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

// Product service interface
interface ProductService {
    Page<ProductEntity> findAll(Pageable pageable, String search, String category, Boolean active);
    Optional<ProductEntity> findById(Long id);
    Optional<ProductEntity> findBySku(String sku);
    ProductEntity create(ProductEntity product);
    ProductEntity update(Long id, ProductEntity product);
    ProductEntity partialUpdate(Long id, Map<String, Object> updates);
    void delete(Long id);
    boolean existsById(Long id);
    boolean existsBySku(String sku);
    List<String> findAllCategories();
    Map<String, Object> getStatistics();
}

// Product service implementation
@Service
class ProductServiceImpl implements ProductService {
    
    private final Map<Long, ProductEntity> database = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public ProductServiceImpl() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        createSampleProduct("LAP-1001", "MacBook Pro 16\"", "High-performance laptop", 
            2499.99, 25, "Laptops", "Apple");
        createSampleProduct("PHO-2001", "iPhone 14 Pro", "Latest smartphone", 
            999.99, 50, "Smartphones", "Apple");
        createSampleProduct("TAB-3001", "iPad Air", "Versatile tablet", 
            599.99, 40, "Tablets", "Apple");
        createSampleProduct("MON-4001", "Dell UltraSharp 27\"", "4K Monitor", 
            449.99, 30, "Monitors", "Dell");
    }
    
    private void createSampleProduct(String sku, String name, String description, 
                                   double price, int stock, String category, String brand) {
        ProductEntity product = new ProductEntity();
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);
        product.setBrand(brand);
        product.setTags(Arrays.asList(category.toLowerCase(), brand.toLowerCase()));
        create(product);
    }
    
    @Override
    public Page<ProductEntity> findAll(Pageable pageable, String search, String category, Boolean active) {
        List<ProductEntity> allProducts = new ArrayList<>(database.values());
        
        // Apply filters
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            allProducts = allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(searchLower) ||
                           p.getDescription().toLowerCase().contains(searchLower) ||
                           p.getSku().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }
        
        if (category != null && !category.isEmpty()) {
            allProducts = allProducts.stream()
                .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                .collect(Collectors.toList());
        }
        
        if (active != null) {
            allProducts = allProducts.stream()
                .filter(p -> p.isActive() == active)
                .collect(Collectors.toList());
        }
        
        // Sort
        allProducts.sort((p1, p2) -> {
            Sort.Order order = pageable.getSort().iterator().hasNext() ? 
                pageable.getSort().iterator().next() : null;
            
            if (order != null) {
                int result = 0;
                switch (order.getProperty()) {
                    case "name":
                        result = p1.getName().compareTo(p2.getName());
                        break;
                    case "price":
                        result = p1.getPrice().compareTo(p2.getPrice());
                        break;
                    case "createdAt":
                        result = p1.getCreatedAt().compareTo(p2.getCreatedAt());
                        break;
                    default:
                        result = p1.getId().compareTo(p2.getId());
                }
                return order.isAscending() ? result : -result;
            }
            return p1.getId().compareTo(p2.getId());
        });
        
        // Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allProducts.size());
        List<ProductEntity> pageContent = allProducts.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, allProducts.size());
    }
    
    @Override
    public Optional<ProductEntity> findById(Long id) {
        return Optional.ofNullable(database.get(id));
    }
    
    @Override
    public Optional<ProductEntity> findBySku(String sku) {
        return database.values().stream()
            .filter(p -> p.getSku().equals(sku))
            .findFirst();
    }
    
    @Override
    public ProductEntity create(ProductEntity product) {
        if (existsBySku(product.getSku())) {
            throw new DuplicateResourceException("Product with SKU " + product.getSku() + " already exists");
        }
        
        Long id = idGenerator.getAndIncrement();
        product.setId(id);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setCreatedBy("system"); // In real app, get from security context
        product.setUpdatedBy("system");
        
        database.put(id, product);
        return product;
    }
    
    @Override
    public ProductEntity update(Long id, ProductEntity product) {
        ProductEntity existing = database.get(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        
        // Check SKU uniqueness if changed
        if (!existing.getSku().equals(product.getSku()) && existsBySku(product.getSku())) {
            throw new DuplicateResourceException("Product with SKU " + product.getSku() + " already exists");
        }
        
        product.setId(id);
        product.setCreatedAt(existing.getCreatedAt());
        product.setCreatedBy(existing.getCreatedBy());
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy("system");
        
        database.put(id, product);
        return product;
    }
    
    @Override
    public ProductEntity partialUpdate(Long id, Map<String, Object> updates) {
        ProductEntity existing = database.get(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        
        // Apply updates
        updates.forEach((key, value) -> {
            switch (key) {
                case "name":
                    existing.setName((String) value);
                    break;
                case "description":
                    existing.setDescription((String) value);
                    break;
                case "price":
                    existing.setPrice(((Number) value).doubleValue());
                    break;
                case "stock":
                    existing.setStock(((Number) value).intValue());
                    break;
                case "category":
                    existing.setCategory((String) value);
                    break;
                case "brand":
                    existing.setBrand((String) value);
                    break;
                case "active":
                    existing.setActive((Boolean) value);
                    break;
                case "tags":
                    existing.setTags((List<String>) value);
                    break;
                case "attributes":
                    existing.setAttributes((Map<String, String>) value);
                    break;
            }
        });
        
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy("system");
        
        return existing;
    }
    
    @Override
    public void delete(Long id) {
        if (!existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        database.remove(id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return database.containsKey(id);
    }
    
    @Override
    public boolean existsBySku(String sku) {
        return database.values().stream()
            .anyMatch(p -> p.getSku().equals(sku));
    }
    
    @Override
    public List<String> findAllCategories() {
        return database.values().stream()
            .map(ProductEntity::getCategory)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<ProductEntity> products = new ArrayList<>(database.values());
        
        stats.put("totalProducts", products.size());
        stats.put("activeProducts", products.stream().filter(ProductEntity::isActive).count());
        stats.put("totalValue", products.stream()
            .mapToDouble(p -> p.getPrice() * p.getStock())
            .sum());
        stats.put("outOfStock", products.stream().filter(p -> p.getStock() == 0).count());
        stats.put("categories", findAllCategories());
        
        return stats;
    }
}

// Enhanced Product REST Controller
@RestController
@RequestMapping("/api/v1/products")
class ProductRestController {
    
    private final ProductService productService;
    
    public ProductRestController(ProductService productService) {
        this.productService = productService;
    }
    
    // GET all products with advanced filtering
    @GetMapping
    public ResponseEntity<Page<ProductEntity>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active) {
        
        Sort.Direction direction = sort[1].equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Sort.Order order = new Sort.Order(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, Sort.by(order));
        
        Page<ProductEntity> products = productService.findAll(pageable, search, category, active);
        
        return ResponseEntity.ok()
            .header("X-Total-Count", String.valueOf(products.getTotalElements()))
            .body(products);
    }
    
    // GET product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductEntity> getProductById(@PathVariable Long id) {
        return productService.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }
    
    // GET product by SKU
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductEntity> getProductBySku(@PathVariable String sku) {
        return productService.findBySku(sku)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
    }
    
    // POST - Create new product
    @PostMapping
    public ResponseEntity<ProductEntity> createProduct(@Valid @RequestBody ProductEntity product) {
        ProductEntity created = productService.create(product);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
            
        return ResponseEntity.created(location).body(created);
    }
    
    // PUT - Full update
    @PutMapping("/{id}")
    public ResponseEntity<ProductEntity> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductEntity product) {
        
        ProductEntity updated = productService.update(id, product);
        return ResponseEntity.ok(updated);
    }
    
    // PATCH - Partial update
    @PatchMapping("/{id}")
    public ResponseEntity<ProductEntity> partialUpdateProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        
        ProductEntity updated = productService.partialUpdate(id, updates);
        return ResponseEntity.ok(updated);
    }
    
    // DELETE product
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.delete(id);
    }
    
    // GET all categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(productService.findAllCategories());
    }
    
    // GET statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(productService.getStatistics());
    }
}
```

---

## 📋 Practice Exercises

### Exercise 1: Order Management API
Create a REST API for managing orders with the following requirements:
- Order entity with items, customer info, and status
- Full CRUD operations with proper HTTP methods
- Order status workflow (PENDING → PROCESSING → SHIPPED → DELIVERED)
- Calculate order totals and apply discounts
- Search orders by customer, date range, and status

### Exercise 2: User Authentication API
Build a user registration and authentication API:
- User registration with validation
- Login endpoint returning JWT token
- Password reset functionality
- User profile management
- Proper error handling for authentication failures

### Exercise 3: File Upload API
Implement a file management REST API:
- Upload files with metadata
- Download files by ID
- List files with pagination
- Delete files
- Support for multiple file types with validation

---

## 🎯 Key Takeaways

### REST API Best Practices:
1. **Use proper HTTP methods**: GET for reading, POST for creating, PUT for full updates, PATCH for partial updates, DELETE for removal
2. **Return appropriate status codes**: 200 for success, 201 for created, 204 for no content, 400 for bad request, 404 for not found, 500 for server errors
3. **Design resource-based URLs**: Use nouns, not verbs (`/users` not `/getUsers`)
4. **Implement pagination**: Handle large datasets efficiently
5. **Version your APIs**: Use URL or header versioning
6. **Handle errors gracefully**: Provide meaningful error messages
7. **Document your APIs**: Use tools like Swagger/OpenAPI

### Spring REST Features:
- `@RestController` = `@Controller` + `@ResponseBody`
- `@RequestMapping` and HTTP method-specific annotations
- Automatic JSON serialization/deserialization
- Exception handling with `@ControllerAdvice`
- Request/response validation
- Content negotiation

---

## 🚀 Next Steps

Tomorrow, we'll explore **API Documentation & Testing** (Day 23), where you'll learn:
- Swagger/OpenAPI documentation
- API testing with Postman
- Integration testing with MockMvc
- API versioning strategies
- Testing best practices

Keep practicing with REST APIs - they're the foundation of modern web services! 🌐