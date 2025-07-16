# Day 23: API Documentation & Testing (August 1, 2025)

**Date:** August 1, 2025  
**Duration:** 2.5 hours  
**Focus:** Master API documentation with Swagger/OpenAPI, testing strategies, and versioning patterns

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Create comprehensive API documentation with Swagger/OpenAPI
- ✅ Configure SpringDoc for automatic documentation generation
- ✅ Implement API versioning strategies
- ✅ Master API testing with RestTemplate and WebTestClient
- ✅ Build integration tests with MockMvc
- ✅ Create a fully documented and tested API

---

## 📚 Part 1: Swagger/OpenAPI Documentation (45 minutes)

### **Understanding API Documentation**

**OpenAPI (formerly Swagger)** is a specification for documenting REST APIs. It provides:
- **Interactive documentation**: Test APIs directly from the browser
- **Code generation**: Generate client SDKs and server stubs
- **Validation**: Ensure API implementation matches specification
- **Standardization**: Common format for API documentation

### **SpringDoc Configuration**

**SpringDoc** is a library that generates OpenAPI documentation from Spring Boot applications automatically.

Create `SwaggerConfigurationDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * Swagger/OpenAPI configuration demonstration
 * Shows how to configure comprehensive API documentation
 */
@SpringBootApplication
public class SwaggerConfigurationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Swagger Configuration Demo ===\n");
        SpringApplication.run(SwaggerConfigurationDemo.class, args);
        
        System.out.println("📚 API Documentation Available:");
        System.out.println("   Swagger UI: http://localhost:8080/swagger-ui/index.html");
        System.out.println("   OpenAPI JSON: http://localhost:8080/v3/api-docs");
        System.out.println("   OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml");
        System.out.println();
    }
}

@Configuration
class OpenApiConfig {
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${app.title:Product Management API}")
    private String appTitle;
    
    @Value("${app.description:RESTful API for managing products}")
    private String appDescription;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(appTitle)
                .version(appVersion)
                .description(appDescription)
                .contact(new Contact()
                    .name("API Support Team")
                    .email("support@example.com")
                    .url("https://example.com/support"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(Arrays.asList(
                new Server().url("http://localhost:8080").description("Development server"),
                new Server().url("https://api.example.com").description("Production server")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
    
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .displayName("Public API")
            .pathsToMatch("/api/v1/**")
            .pathsToExclude("/api/v1/internal/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .displayName("Admin API")
            .pathsToMatch("/api/v1/admin/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
            .group("internal")
            .displayName("Internal API")
            .pathsToMatch("/api/v1/internal/**")
            .build();
    }
    
    @Bean
    public OpenApiCustomiser sortSchemasAlphabetically() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().forEach((key, schema) -> {
                    if (schema.getProperties() != null) {
                        schema.getProperties().keySet().stream()
                            .sorted()
                            .forEach(prop -> {
                                // Sort properties alphabetically
                            });
                    }
                });
            }
        };
    }
}
```

### **API Documentation Annotations**

Create `ApiDocumentationDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API Documentation with annotations demonstration
 * Shows how to document REST APIs comprehensively
 */
@SpringBootApplication
public class ApiDocumentationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== API Documentation Demo ===\n");
        SpringApplication.run(ApiDocumentationDemo.class, args);
    }
}

// Documented Product model
@Schema(description = "Product entity representing items in the catalog")
class DocumentedProduct {
    @Schema(description = "Unique identifier for the product", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    
    @NotBlank(message = "Product name is required")
    @Schema(description = "Name of the product", example = "MacBook Pro 16\"", required = true)
    private String name;
    
    @NotBlank(message = "Description is required")
    @Schema(description = "Detailed description of the product", 
            example = "High-performance laptop with M2 chip and 16GB RAM", 
            required = true, maxLength = 1000)
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    @Schema(description = "Price of the product in USD", example = "2499.99", required = true)
    private Double price;
    
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Schema(description = "Available stock quantity", example = "25", required = true)
    private Integer stock;
    
    @NotBlank(message = "Category is required")
    @Schema(description = "Product category", example = "Electronics", required = true)
    private String category;
    
    @Schema(description = "Product brand", example = "Apple")
    private String brand;
    
    @Schema(description = "Product tags for search and filtering", 
            example = "[\"laptop\", \"professional\", \"apple\"]")
    private List<String> tags = new ArrayList<>();
    
    @Schema(description = "Product availability status", example = "true")
    private boolean available = true;
    
    @Schema(description = "Product creation timestamp", 
            example = "2025-08-01T10:30:00", 
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
    
    @Schema(description = "Product last update timestamp", 
            example = "2025-08-01T15:45:00", 
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
    
    // Constructors, getters, and setters
    public DocumentedProduct() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

// Error response model for documentation
@Schema(description = "Standard error response")
class ErrorResponse {
    @Schema(description = "Error timestamp", example = "2025-08-01T10:30:00")
    private String timestamp;
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Error type", example = "Bad Request")
    private String error;
    
    @Schema(description = "Error message", example = "Validation failed")
    private String message;
    
    @Schema(description = "Request path", example = "/api/v1/products")
    private String path;
    
    @Schema(description = "Field validation errors")
    private Map<String, String> validationErrors = new HashMap<>();
    
    // Constructors, getters, and setters
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now().toString();
    }
    
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
}

// Fully documented REST controller
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management API")
class DocumentedProductController {
    
    private final Map<Long, DocumentedProduct> database = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public DocumentedProductController() {
        // Initialize sample data
        DocumentedProduct product = new DocumentedProduct();
        product.setName("MacBook Pro 16\"");
        product.setDescription("High-performance laptop with M2 chip");
        product.setPrice(2499.99);
        product.setStock(25);
        product.setCategory("Electronics");
        product.setBrand("Apple");
        product.setTags(Arrays.asList("laptop", "professional", "apple"));
        createProduct(product);
    }
    
    @Operation(
        summary = "Get all products",
        description = "Retrieve a paginated list of products with optional filtering",
        tags = {"Products"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved products",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<Page<DocumentedProduct>> getAllProducts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "Search term for name or description", example = "MacBook")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Filter by category", example = "Electronics")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Filter by availability", example = "true")
            @RequestParam(required = false) Boolean available) {
        
        List<DocumentedProduct> products = new ArrayList<>(database.values());
        
        // Apply filters (simplified for demo)
        if (search != null && !search.isEmpty()) {
            products = products.stream()
                .filter(p -> p.getName().toLowerCase().contains(search.toLowerCase()) ||
                           p.getDescription().toLowerCase().contains(search.toLowerCase()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        if (available != null) {
            products = products.stream()
                .filter(p -> p.isAvailable() == available)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        // Create pageable response (simplified)
        Page<DocumentedProduct> pagedProducts = new PageImpl<>(products);
        
        return ResponseEntity.ok(pagedProducts);
    }
    
    @Operation(
        summary = "Get product by ID",
        description = "Retrieve a specific product by its unique identifier",
        tags = {"Products"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentedProduct.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentedProduct> getProductById(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long id) {
        
        DocumentedProduct product = database.get(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
        summary = "Create a new product",
        description = "Add a new product to the catalog",
        tags = {"Products"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Product created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentedProduct.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid product data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = "{\"timestamp\":\"2025-08-01T10:30:00\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Validation failed\",\"path\":\"/api/v1/products\",\"validationErrors\":{\"name\":\"Product name is required\",\"price\":\"Price must be positive\"}}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Product already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<DocumentedProduct> createProduct(
            @Parameter(description = "Product data", required = true)
            @Valid @RequestBody DocumentedProduct product) {
        
        DocumentedProduct created = createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @Operation(
        summary = "Update an existing product",
        description = "Update all fields of an existing product",
        tags = {"Products"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentedProduct.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid product data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<DocumentedProduct> updateProduct(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long id,
            
            @Parameter(description = "Updated product data", required = true)
            @Valid @RequestBody DocumentedProduct product) {
        
        if (!database.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        product.setId(id);
        product.setUpdatedAt(LocalDateTime.now());
        database.put(id, product);
        
        return ResponseEntity.ok(product);
    }
    
    @Operation(
        summary = "Delete a product",
        description = "Remove a product from the catalog",
        tags = {"Products"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Product deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long id) {
        
        if (!database.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        database.remove(id);
        return ResponseEntity.noContent().build();
    }
    
    private DocumentedProduct createProduct(DocumentedProduct product) {
        Long id = idGenerator.getAndIncrement();
        product.setId(id);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        database.put(id, product);
        return product;
    }
}
```

---

## 🔄 Part 2: API Versioning Strategies (30 minutes)

### **API Versioning Patterns**

Create `ApiVersioningDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.*;

/**
 * API versioning strategies demonstration
 * Shows different approaches to versioning REST APIs
 */
@SpringBootApplication
public class ApiVersioningDemo {
    
    public static void main(String[] args) {
        System.out.println("=== API Versioning Demo ===\n");
        SpringApplication.run(ApiVersioningDemo.class, args);
        
        System.out.println("📍 API Versioning Examples:");
        System.out.println("   URL Versioning:");
        System.out.println("     GET /api/v1/users/1");
        System.out.println("     GET /api/v2/users/1");
        System.out.println("   Header Versioning:");
        System.out.println("     GET /api/users/1 (X-API-Version: 1)");
        System.out.println("     GET /api/users/1 (X-API-Version: 2)");
        System.out.println("   Parameter Versioning:");
        System.out.println("     GET /api/users/1?version=1");
        System.out.println("     GET /api/users/1?version=2");
        System.out.println("   Accept Header Versioning:");
        System.out.println("     GET /api/users/1 (Accept: application/vnd.api.v1+json)");
        System.out.println("     GET /api/users/1 (Accept: application/vnd.api.v2+json)");
        System.out.println();
    }
}

// User models for different versions
class UserV1 {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    
    // Constructors, getters, and setters
    public UserV1() {}
    
    public UserV1(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

class UserV2 {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
    
    // Constructors, getters, and setters
    public UserV2() {}
    
    public UserV2(Long id, String firstName, String lastName, String email, String phone) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

// 1. URL Versioning - Most common approach
@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller {
    
    private final Map<Long, UserV1> users = new HashMap<>();
    
    public UserV1Controller() {
        // Initialize sample data
        users.put(1L, new UserV1(1L, "John Doe", "john@example.com"));
        users.put(2L, new UserV1(2L, "Jane Smith", "jane@example.com"));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserV1> getUserV1(@PathVariable Long id) {
        UserV1 user = users.get(id);
        if (user != null) {
            System.out.println("Serving user V1: " + user.getName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping
    public ResponseEntity<List<UserV1>> getAllUsersV1() {
        System.out.println("Serving all users V1");
        return ResponseEntity.ok(new ArrayList<>(users.values()));
    }
}

@RestController
@RequestMapping("/api/v2/users")
class UserV2Controller {
    
    private final Map<Long, UserV2> users = new HashMap<>();
    
    public UserV2Controller() {
        // Initialize sample data with new structure
        users.put(1L, new UserV2(1L, "John", "Doe", "john@example.com", "+1-555-0123"));
        users.put(2L, new UserV2(2L, "Jane", "Smith", "jane@example.com", "+1-555-0456"));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserV2> getUserV2(@PathVariable Long id) {
        UserV2 user = users.get(id);
        if (user != null) {
            System.out.println("Serving user V2: " + user.getFirstName() + " " + user.getLastName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping
    public ResponseEntity<List<UserV2>> getAllUsersV2() {
        System.out.println("Serving all users V2");
        return ResponseEntity.ok(new ArrayList<>(users.values()));
    }
}

// 2. Header Versioning
@RestController
@RequestMapping("/api/users")
class HeaderVersionedController {
    
    private final Map<Long, UserV1> usersV1 = new HashMap<>();
    private final Map<Long, UserV2> usersV2 = new HashMap<>();
    
    public HeaderVersionedController() {
        // Initialize data for both versions
        usersV1.put(1L, new UserV1(1L, "John Doe", "john@example.com"));
        usersV2.put(1L, new UserV2(1L, "John", "Doe", "john@example.com", "+1-555-0123"));
    }
    
    @GetMapping(value = "/{id}", headers = "X-API-Version=1")
    public ResponseEntity<UserV1> getUserV1Header(@PathVariable Long id) {
        UserV1 user = usersV1.get(id);
        if (user != null) {
            System.out.println("Serving user V1 via header: " + user.getName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping(value = "/{id}", headers = "X-API-Version=2")
    public ResponseEntity<UserV2> getUserV2Header(@PathVariable Long id) {
        UserV2 user = usersV2.get(id);
        if (user != null) {
            System.out.println("Serving user V2 via header: " + user.getFirstName() + " " + user.getLastName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
}

// 3. Parameter Versioning
@RestController
@RequestMapping("/api/users-param")
class ParameterVersionedController {
    
    private final Map<Long, UserV1> usersV1 = new HashMap<>();
    private final Map<Long, UserV2> usersV2 = new HashMap<>();
    
    public ParameterVersionedController() {
        // Initialize data for both versions
        usersV1.put(1L, new UserV1(1L, "John Doe", "john@example.com"));
        usersV2.put(1L, new UserV2(1L, "John", "Doe", "john@example.com", "+1-555-0123"));
    }
    
    @GetMapping(value = "/{id}", params = "version=1")
    public ResponseEntity<UserV1> getUserV1Param(@PathVariable Long id) {
        UserV1 user = usersV1.get(id);
        if (user != null) {
            System.out.println("Serving user V1 via parameter: " + user.getName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping(value = "/{id}", params = "version=2")
    public ResponseEntity<UserV2> getUserV2Param(@PathVariable Long id) {
        UserV2 user = usersV2.get(id);
        if (user != null) {
            System.out.println("Serving user V2 via parameter: " + user.getFirstName() + " " + user.getLastName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
}

// 4. Accept Header Versioning (Content Negotiation)
@RestController
@RequestMapping("/api/users-accept")
class AcceptHeaderVersionedController {
    
    private final Map<Long, UserV1> usersV1 = new HashMap<>();
    private final Map<Long, UserV2> usersV2 = new HashMap<>();
    
    public AcceptHeaderVersionedController() {
        // Initialize data for both versions
        usersV1.put(1L, new UserV1(1L, "John Doe", "john@example.com"));
        usersV2.put(1L, new UserV2(1L, "John", "Doe", "john@example.com", "+1-555-0123"));
    }
    
    @GetMapping(value = "/{id}", produces = "application/vnd.api.v1+json")
    public ResponseEntity<UserV1> getUserV1Accept(@PathVariable Long id) {
        UserV1 user = usersV1.get(id);
        if (user != null) {
            System.out.println("Serving user V1 via accept header: " + user.getName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping(value = "/{id}", produces = "application/vnd.api.v2+json")
    public ResponseEntity<UserV2> getUserV2Accept(@PathVariable Long id) {
        UserV2 user = usersV2.get(id);
        if (user != null) {
            System.out.println("Serving user V2 via accept header: " + user.getFirstName() + " " + user.getLastName());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
}

// Version compatibility service
@RestController
@RequestMapping("/api/compatibility")
class VersionCompatibilityController {
    
    @GetMapping("/versions")
    public ResponseEntity<Map<String, Object>> getSupportedVersions() {
        Map<String, Object> versions = new HashMap<>();
        versions.put("supported", Arrays.asList("1.0", "2.0"));
        versions.put("deprecated", Arrays.asList("0.9"));
        versions.put("sunset", Arrays.asList("0.8"));
        versions.put("current", "2.0");
        versions.put("latest", "2.0");
        
        return ResponseEntity.ok(versions);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getVersionHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", "2.0");
        health.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(health);
    }
}
```

---

## 🧪 Part 3: API Testing with RestTemplate and MockMvc (45 minutes)

### **RestTemplate Testing**

Create `RestTemplateTestingDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.*;
import org.springframework.boot.CommandLineRunner;

import java.time.Duration;
import java.util.*;

/**
 * RestTemplate testing demonstration
 * Shows how to test REST APIs programmatically
 */
@SpringBootApplication
public class RestTemplateTestingDemo implements CommandLineRunner {
    
    private final RestTemplate restTemplate;
    
    public RestTemplateTestingDemo(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public static void main(String[] args) {
        System.out.println("=== RestTemplate Testing Demo ===\n");
        SpringApplication.run(RestTemplateTestingDemo.class, args);
    }
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Wait for server to start
        Thread.sleep(2000);
        
        System.out.println("🧪 Running RestTemplate Tests...\n");
        
        // Test basic GET requests
        testGetRequests();
        
        // Test POST requests
        testPostRequests();
        
        // Test PUT requests
        testPutRequests();
        
        // Test DELETE requests
        testDeleteRequests();
        
        // Test error handling
        testErrorHandling();
        
        // Test different content types
        testContentTypes();
        
        System.out.println("\n✅ All RestTemplate tests completed!\n");
    }
    
    private void testGetRequests() {
        System.out.println("1. Testing GET Requests:");
        
        try {
            // Test GET with response entity
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8080/api/v1/products", String.class);
            
            System.out.println("   Status: " + response.getStatusCode());
            System.out.println("   Headers: " + response.getHeaders().getFirst("Content-Type"));
            System.out.println("   Body length: " + response.getBody().length() + " characters");
            
            // Test GET with path variables
            ResponseEntity<String> productResponse = restTemplate.getForEntity(
                "http://localhost:8080/api/v1/products/{id}", String.class, 1L);
            
            System.out.println("   Product response status: " + productResponse.getStatusCode());
            
            // Test GET with query parameters
            String url = "http://localhost:8080/api/v1/products?page=0&size=5&search=MacBook";
            ResponseEntity<String> searchResponse = restTemplate.getForEntity(url, String.class);
            
            System.out.println("   Search response status: " + searchResponse.getStatusCode());
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private void testPostRequests() {
        System.out.println("2. Testing POST Requests:");
        
        try {
            // Create test product
            Map<String, Object> product = new HashMap<>();
            product.put("name", "Test Product");
            product.put("description", "A test product created via RestTemplate");
            product.put("price", 99.99);
            product.put("stock", 10);
            product.put("category", "Testing");
            product.put("brand", "TestBrand");
            product.put("tags", Arrays.asList("test", "demo"));
            
            // Test POST with request entity
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(product, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:8080/api/v1/products", requestEntity, String.class);
            
            System.out.println("   POST Status: " + response.getStatusCode());
            System.out.println("   Location Header: " + response.getHeaders().getFirst("Location"));
            System.out.println("   Created product response: " + response.getBody().substring(0, 100) + "...");
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private void testPutRequests() {
        System.out.println("3. Testing PUT Requests:");
        
        try {
            // Update existing product
            Map<String, Object> updatedProduct = new HashMap<>();
            updatedProduct.put("name", "Updated Product");
            updatedProduct.put("description", "Updated description");
            updatedProduct.put("price", 149.99);
            updatedProduct.put("stock", 15);
            updatedProduct.put("category", "Updated");
            updatedProduct.put("brand", "UpdatedBrand");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updatedProduct, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8080/api/v1/products/{id}",
                HttpMethod.PUT,
                requestEntity,
                String.class,
                1L);
            
            System.out.println("   PUT Status: " + response.getStatusCode());
            System.out.println("   Updated product: " + response.getBody().substring(0, 100) + "...");
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private void testDeleteRequests() {
        System.out.println("4. Testing DELETE Requests:");
        
        try {
            // First create a product to delete
            Map<String, Object> product = new HashMap<>();
            product.put("name", "Product to Delete");
            product.put("description", "This product will be deleted");
            product.put("price", 1.00);
            product.put("stock", 1);
            product.put("category", "Temporary");
            product.put("brand", "Delete");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(product, headers);
            
            ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "http://localhost:8080/api/v1/products", requestEntity, String.class);
            
            if (createResponse.getStatusCode() == HttpStatus.CREATED) {
                // Extract ID from response (simplified)
                Long productId = 2L; // Assuming second product created
                
                // Now delete the product
                ResponseEntity<String> deleteResponse = restTemplate.exchange(
                    "http://localhost:8080/api/v1/products/{id}",
                    HttpMethod.DELETE,
                    null,
                    String.class,
                    productId);
                
                System.out.println("   DELETE Status: " + deleteResponse.getStatusCode());
                
                // Verify deletion
                try {
                    restTemplate.getForEntity(
                        "http://localhost:8080/api/v1/products/{id}", String.class, productId);
                    System.out.println("   Error: Product still exists after deletion");
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        System.out.println("   Verification: Product successfully deleted");
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private void testErrorHandling() {
        System.out.println("5. Testing Error Handling:");
        
        try {
            // Test 404 error
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8080/api/v1/products/999", String.class);
            
            System.out.println("   Unexpected success: " + response.getStatusCode());
            
        } catch (HttpClientErrorException e) {
            System.out.println("   Expected 404 error: " + e.getStatusCode());
            System.out.println("   Error response: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("   Unexpected error: " + e.getMessage());
        }
        
        try {
            // Test 400 error with invalid data
            Map<String, Object> invalidProduct = new HashMap<>();
            invalidProduct.put("name", ""); // Invalid - empty name
            invalidProduct.put("price", -10.0); // Invalid - negative price
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(invalidProduct, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:8080/api/v1/products", requestEntity, String.class);
            
            System.out.println("   Unexpected success: " + response.getStatusCode());
            
        } catch (HttpClientErrorException e) {
            System.out.println("   Expected 400 error: " + e.getStatusCode());
            System.out.println("   Validation error: " + e.getResponseBodyAsString().substring(0, 200) + "...");
        } catch (Exception e) {
            System.out.println("   Unexpected error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private void testContentTypes() {
        System.out.println("6. Testing Content Types:");
        
        try {
            // Test JSON content type
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
            jsonHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            
            HttpEntity<String> jsonEntity = new HttpEntity<>(jsonHeaders);
            
            ResponseEntity<String> jsonResponse = restTemplate.exchange(
                "http://localhost:8080/api/v1/products/1",
                HttpMethod.GET,
                jsonEntity,
                String.class);
            
            System.out.println("   JSON Response Status: " + jsonResponse.getStatusCode());
            System.out.println("   JSON Content-Type: " + jsonResponse.getHeaders().getFirst("Content-Type"));
            
            // Test with custom headers
            HttpHeaders customHeaders = new HttpHeaders();
            customHeaders.set("X-API-Version", "2");
            customHeaders.set("X-Client-ID", "test-client");
            
            HttpEntity<String> customEntity = new HttpEntity<>(customHeaders);
            
            ResponseEntity<String> customResponse = restTemplate.exchange(
                "http://localhost:8080/api/users/1",
                HttpMethod.GET,
                customEntity,
                String.class);
            
            System.out.println("   Custom Headers Response Status: " + customResponse.getStatusCode());
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        System.out.println();
    }
}
```

### **MockMvc Integration Testing**

Create `MockMvcIntegrationTestDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.hamcrest.Matchers.*;

/**
 * MockMvc integration testing demonstration
 * Shows how to test REST controllers with MockMvc
 */
@SpringBootApplication
public class MockMvcIntegrationTestDemo {
    
    public static void main(String[] args) {
        System.out.println("=== MockMvc Integration Test Demo ===\n");
        SpringApplication.run(MockMvcIntegrationTestDemo.class, args);
    }
}

// Test class demonstrating MockMvc usage
@SpringBootTest
@AutoConfigureWebMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerIntegrationTest {
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setup(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build();
    }
    
    @Test
    @Order(1)
    @DisplayName("Should get all products with pagination")
    void shouldGetAllProductsWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andDo(print());
    }
    
    @Test
    @Order(2)
    @DisplayName("Should get product by ID")
    void shouldGetProductById() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.stock").exists())
                .andDo(print());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should return 404 for non-existent product")
    void shouldReturn404ForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
    
    @Test
    @Order(4)
    @DisplayName("Should create new product")
    void shouldCreateNewProduct() throws Exception {
        String productJson = """
            {
                "name": "Test Product",
                "description": "A test product for MockMvc testing",
                "price": 99.99,
                "stock": 50,
                "category": "Testing",
                "brand": "TestBrand",
                "tags": ["test", "mockmvc"],
                "available": true
            }
            """;
        
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.category").value("Testing"))
                .andExpect(jsonPath("$.brand").value("TestBrand"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andDo(print());
    }
    
    @Test
    @Order(5)
    @DisplayName("Should validate product creation")
    void shouldValidateProductCreation() throws Exception {
        String invalidProductJson = """
            {
                "name": "",
                "description": "",
                "price": -10.0,
                "stock": -5,
                "category": "",
                "brand": ""
            }
            """;
        
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidProductJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors").exists())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stock").exists())
                .andDo(print());
    }
    
    @Test
    @Order(6)
    @DisplayName("Should update existing product")
    void shouldUpdateExistingProduct() throws Exception {
        String updatedProductJson = """
            {
                "name": "Updated Test Product",
                "description": "Updated description for test product",
                "price": 149.99,
                "stock": 75,
                "category": "Updated Testing",
                "brand": "UpdatedBrand",
                "tags": ["updated", "test"],
                "available": true
            }
            """;
        
        mockMvc.perform(put("/api/v1/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedProductJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Test Product"))
                .andExpect(jsonPath("$.price").value(149.99))
                .andExpect(jsonPath("$.stock").value(75))
                .andExpect(jsonPath("$.category").value("Updated Testing"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andDo(print());
    }
    
    @Test
    @Order(7)
    @DisplayName("Should search products")
    void shouldSearchProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("search", "MacBook")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].name", everyItem(containsStringIgnoringCase("MacBook"))))
                .andDo(print());
    }
    
    @Test
    @Order(8)
    @DisplayName("Should filter products by category")
    void shouldFilterProductsByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("category", "Electronics")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].category", everyItem(equalTo("Electronics"))))
                .andDo(print());
    }
    
    @Test
    @Order(9)
    @DisplayName("Should get product categories")
    void shouldGetProductCategories() throws Exception {
        mockMvc.perform(get("/api/v1/products/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andDo(print());
    }
    
    @Test
    @Order(10)
    @DisplayName("Should get product statistics")
    void shouldGetProductStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/products/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalProducts").exists())
                .andExpect(jsonPath("$.activeProducts").exists())
                .andExpect(jsonPath("$.totalValue").exists())
                .andExpect(jsonPath("$.outOfStock").exists())
                .andExpect(jsonPath("$.categories").isArray())
                .andDo(print());
    }
    
    @Test
    @Order(11)
    @DisplayName("Should test API versioning")
    void shouldTestApiVersioning() throws Exception {
        // Test URL versioning
        mockMvc.perform(get("/api/v1/users/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andDo(print());
        
        mockMvc.perform(get("/api/v2/users/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andDo(print());
        
        // Test header versioning
        mockMvc.perform(get("/api/users/{id}", 1L)
                .header("X-API-Version", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andDo(print());
        
        mockMvc.perform(get("/api/users/{id}", 1L)
                .header("X-API-Version", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andDo(print());
    }
    
    @Test
    @Order(12)
    @DisplayName("Should test error handling")
    void shouldTestErrorHandling() throws Exception {
        // Test custom error handling
        mockMvc.perform(get("/api/demo/resource/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists())
                .andDo(print());
        
        // Test validation error handling
        String invalidData = """
            {
                "id": 1,
                "value": ""
            }
            """;
        
        mockMvc.perform(post("/api/demo/resource")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidData))
                .andExpect(status().isBadRequest())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andDo(print());
    }
}
```

---

## 📋 Practice Exercises

### Exercise 1: Complete API Documentation
Enhance the product API with comprehensive documentation:
- Add detailed descriptions for all endpoints
- Include request/response examples
- Document error responses
- Add parameter validations
- Create multiple API groups (public, admin, internal)

### Exercise 2: API Testing Suite
Create a comprehensive test suite:
- Unit tests for all controller methods
- Integration tests with MockMvc
- Error handling tests
- Performance tests
- Contract tests

### Exercise 3: API Versioning Strategy
Implement a complete versioning strategy:
- Support multiple versions simultaneously
- Implement version deprecation warnings
- Create migration guides
- Add version compatibility checks
- Handle breaking changes gracefully

---

## 🎯 Key Takeaways

### API Documentation Best Practices:
1. **Use OpenAPI/Swagger**: Industry standard for API documentation
2. **Document everything**: All endpoints, parameters, responses, and errors
3. **Provide examples**: Include request/response examples
4. **Keep it updated**: Documentation should match implementation
5. **Make it interactive**: Allow testing directly from documentation
6. **Version your docs**: Different versions for different API versions

### API Testing Strategies:
1. **Unit testing**: Test individual components in isolation
2. **Integration testing**: Test complete request/response cycles
3. **Contract testing**: Ensure API contracts are maintained
4. **Performance testing**: Test under load and stress
5. **Security testing**: Test authentication and authorization
6. **Error testing**: Test all error scenarios

### API Versioning Considerations:
1. **URL versioning**: Most explicit and widely used
2. **Header versioning**: Keeps URLs clean
3. **Parameter versioning**: Simple but can clutter URLs
4. **Content negotiation**: Most RESTful approach
5. **Backwards compatibility**: Maintain compatibility when possible
6. **Deprecation strategy**: Plan for retiring old versions

---

## 🚀 Next Steps

Tomorrow, we'll explore **Spring Security Basics** (Day 24), where you'll learn:
- Authentication and authorization concepts
- Spring Security configuration
- User management and password encoding
- Method-level security
- JWT tokens and session management

Your API documentation and testing skills will be essential for building secure, well-documented APIs! 🔐