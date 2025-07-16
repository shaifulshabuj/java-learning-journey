# Day 18: Spring Boot Fundamentals (July 27, 2025)

**Date:** July 27, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Spring Boot auto-configuration, embedded servers, and rapid application development

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand Spring Boot's auto-configuration magic
- ✅ Master Spring Boot project structure and conventions
- ✅ Work with embedded servers (Tomcat, Jetty, Undertow)
- ✅ Configure applications with properties and YAML
- ✅ Implement REST APIs with Spring Boot
- ✅ Build a complete microservice with Spring Boot

---

## 🚀 Part 1: Spring Boot Auto-Configuration (45 minutes)

### **Understanding Spring Boot**

**Spring Boot** is an opinionated framework that simplifies Spring application development by providing:
- **Auto-configuration**: Automatically configures Spring applications based on dependencies
- **Embedded servers**: No need for external application servers
- **Production-ready features**: Health checks, metrics, externalized configuration
- **Starter dependencies**: Curated set of dependencies for common use cases

### **Spring Boot Auto-Configuration Demo**

Create `SpringBootAutoConfigDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Boot auto-configuration demonstration
 * Shows how Spring Boot automatically configures beans based on classpath and properties
 */
@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class SpringBootAutoConfigDemo implements CommandLineRunner {
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private ApplicationProperties appProperties;
    
    @Autowired
    private AutoConfigurationReport autoConfigReport;
    
    public static void main(String[] args) {
        System.out.println("=== Spring Boot Auto-Configuration Demo ===\n");
        SpringApplication.run(SpringBootAutoConfigDemo.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Spring Boot Application Started!\n");
        
        // Show environment information
        showEnvironmentInfo();
        
        // Show auto-configuration details
        showAutoConfigurationDetails();
        
        // Show configuration properties
        showConfigurationProperties();
        
        // Show conditional beans
        showConditionalBeans();
        
        System.out.println("\n✅ Demo completed successfully!");
    }
    
    private void showEnvironmentInfo() {
        System.out.println("1. Environment Information:\n");
        
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();
        
        System.out.println("   Active profiles: " + Arrays.toString(activeProfiles));
        System.out.println("   Default profiles: " + Arrays.toString(defaultProfiles));
        System.out.println("   Server port: " + environment.getProperty("server.port", "8080"));
        System.out.println("   Application name: " + environment.getProperty("spring.application.name", "demo"));
        System.out.println("   Java version: " + System.getProperty("java.version"));
        System.out.println("   Spring Boot version: " + SpringApplication.class.getPackage().getImplementationVersion());
        System.out.println();
    }
    
    private void showAutoConfigurationDetails() {
        System.out.println("2. Auto-Configuration Details:\n");
        
        // Show what Spring Boot auto-configured
        System.out.println("   Auto-configured components:");
        System.out.println("   ✅ Embedded Tomcat Server");
        System.out.println("   ✅ Spring MVC DispatcherServlet");
        System.out.println("   ✅ Default Error Handler");
        System.out.println("   ✅ Jackson JSON Serialization");
        System.out.println("   ✅ Logging Framework (Logback)");
        System.out.println("   ✅ Actuator Health Endpoints");
        
        // Show configuration classes that were applied
        System.out.println("\n   Key auto-configuration classes:");
        System.out.println("   - WebMvcAutoConfiguration");
        System.out.println("   - EmbeddedWebServerFactoryCustomizerAutoConfiguration");
        System.out.println("   - HttpMessageConvertersAutoConfiguration");
        System.out.println("   - JacksonAutoConfiguration");
        System.out.println("   - LoggingAutoConfiguration");
        System.out.println();
    }
    
    private void showConfigurationProperties() {
        System.out.println("3. Configuration Properties:\n");
        
        System.out.println("   Application Properties:");
        System.out.println("   - Name: " + appProperties.getName());
        System.out.println("   - Version: " + appProperties.getVersion());
        System.out.println("   - Description: " + appProperties.getDescription());
        System.out.println("   - Features: " + appProperties.getFeatures());
        System.out.println("   - Database URL: " + appProperties.getDatabase().getUrl());
        System.out.println("   - Database Pool Size: " + appProperties.getDatabase().getPoolSize());
        System.out.println();
    }
    
    private void showConditionalBeans() {
        System.out.println("4. Conditional Beans:\n");
        
        autoConfigReport.reportConditionalBeans();
        System.out.println();
    }
}

// Configuration properties class
@ConfigurationProperties(prefix = "app")
class ApplicationProperties {
    private String name = "Default App";
    private String version = "1.0.0";
    private String description = "Default description";
    private List<String> features = Arrays.asList("feature1", "feature2");
    private Database database = new Database();
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }
    
    public static class Database {
        private String url = "jdbc:h2:mem:testdb";
        private int poolSize = 10;
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    }
}

// Auto-configuration report component
@Component
class AutoConfigurationReport {
    
    @Autowired(required = false)
    private CacheService cacheService;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Autowired(required = false)
    private DatabaseService databaseService;
    
    public void reportConditionalBeans() {
        System.out.println("   Conditional beans status:");
        System.out.println("   - CacheService: " + (cacheService != null ? "✅ Available" : "❌ Not available"));
        System.out.println("   - EmailService: " + (emailService != null ? "✅ Available" : "❌ Not available"));
        System.out.println("   - DatabaseService: " + (databaseService != null ? "✅ Available" : "❌ Not available"));
    }
}

// Conditional configuration classes
@Configuration
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
class CacheConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public CacheService cacheService() {
        return new CacheService();
    }
}

@Configuration
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
class EmailConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public EmailService emailService() {
        return new EmailService();
    }
}

@Configuration
@ConditionalOnClass(name = "javax.sql.DataSource")
class DatabaseConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DatabaseService databaseService() {
        return new DatabaseService();
    }
}

// Service classes
class CacheService {
    
    @PostConstruct
    public void init() {
        System.out.println("   📦 CacheService initialized");
    }
    
    public void cache(String key, Object value) {
        System.out.println("   Caching: " + key);
    }
}

class EmailService {
    
    @PostConstruct
    public void init() {
        System.out.println("   📧 EmailService initialized");
    }
    
    public void sendEmail(String to, String subject) {
        System.out.println("   Sending email to: " + to);
    }
}

class DatabaseService {
    
    @PostConstruct
    public void init() {
        System.out.println("   🗄️ DatabaseService initialized");
    }
    
    public void save(Object entity) {
        System.out.println("   Saving entity: " + entity.getClass().getSimpleName());
    }
}

// Custom health indicator
@Component
class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check some application-specific health conditions
        boolean isHealthy = checkApplicationHealth();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("status", "All systems operational")
                .withDetail("database", "Connected")
                .withDetail("cache", "Available")
                .build();
        } else {
            return Health.down()
                .withDetail("status", "System degraded")
                .withDetail("error", "Database connection failed")
                .build();
        }
    }
    
    private boolean checkApplicationHealth() {
        // Simulate health check
        return Math.random() > 0.1; // 90% chance of being healthy
    }
}
```

---

## 🌐 Part 2: Embedded Servers and Web Development (45 minutes)

### **Embedded Server Configuration**

Spring Boot comes with embedded servers that eliminate the need for external application servers. You can easily switch between Tomcat, Jetty, and Undertow.

### **REST API with Spring Boot**

Create `SpringBootRestApiDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Spring Boot REST API demonstration
 * Shows how to quickly build REST APIs with embedded servers
 */
@SpringBootApplication
public class SpringBootRestApiDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Spring Boot REST API Demo ===\n");
        
        SpringApplication app = new SpringApplication(SpringBootRestApiDemo.class);
        
        // Customize application properties programmatically
        Properties props = new Properties();
        props.setProperty("server.port", "8080");
        props.setProperty("spring.application.name", "rest-api-demo");
        props.setProperty("logging.level.com.example", "DEBUG");
        props.setProperty("management.endpoints.web.exposure.include", "health,info,metrics");
        
        app.setDefaultProperties(props);
        app.run(args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("🚀 REST API Server started successfully!");
        System.out.println("📡 Server running on: http://localhost:8080");
        System.out.println("🔍 Health check: http://localhost:8080/actuator/health");
        System.out.println("📊 API endpoints:");
        System.out.println("   GET    /api/users          - Get all users");
        System.out.println("   POST   /api/users          - Create user");
        System.out.println("   GET    /api/users/{id}     - Get user by ID");
        System.out.println("   PUT    /api/users/{id}     - Update user");
        System.out.println("   DELETE /api/users/{id}     - Delete user");
        System.out.println("   GET    /api/users/search   - Search users");
        System.out.println();
    }
}

// REST Controller
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
class UserController {
    
    @Autowired
    private UserService userService;
    
    // Get all users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }
    
    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Create new user
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.getName(), request.getEmail(), request.getAge());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    // Update user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, 
                                          @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(id, request.getName(), request.getEmail(), request.getAge());
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Search users
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam(required = false) String name,
                                                 @RequestParam(required = false) String email,
                                                 @RequestParam(required = false) Integer minAge,
                                                 @RequestParam(required = false) Integer maxAge) {
        List<User> users = userService.searchUsers(name, email, minAge, maxAge);
        return ResponseEntity.ok(users);
    }
    
    // Get user statistics
    @GetMapping("/stats")
    public ResponseEntity<UserStats> getUserStats() {
        UserStats stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }
}

// Product Controller (additional REST endpoints)
@RestController
@RequestMapping("/api/products")
class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.findAll();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return product != null ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request.getName(), request.getDescription(), 
                                                       request.getPrice(), request.getCategory());
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.findByCategory(category);
        return ResponseEntity.ok(products);
    }
}

// Service Layer
@Service
class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public User findById(Long id) {
        return userRepository.findById(id);
    }
    
    public User createUser(String name, String email, Integer age) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public User updateUser(Long id, String name, String email, Integer age) {
        User user = userRepository.findById(id);
        if (user != null) {
            user.setName(name);
            user.setEmail(email);
            user.setAge(age);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return null;
    }
    
    public boolean deleteUser(Long id) {
        return userRepository.deleteById(id);
    }
    
    public List<User> searchUsers(String name, String email, Integer minAge, Integer maxAge) {
        return userRepository.search(name, email, minAge, maxAge);
    }
    
    public UserStats getUserStats() {
        List<User> users = userRepository.findAll();
        
        UserStats stats = new UserStats();
        stats.setTotalUsers(users.size());
        stats.setAverageAge(users.stream().mapToInt(User::getAge).average().orElse(0.0));
        stats.setOldestUser(users.stream().mapToInt(User::getAge).max().orElse(0));
        stats.setYoungestUser(users.stream().mapToInt(User::getAge).min().orElse(0));
        
        return stats;
    }
}

@Service
class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    public Product findById(Long id) {
        return productRepository.findById(id);
    }
    
    public Product createProduct(String name, String description, Double price, String category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());
        
        return productRepository.save(product);
    }
    
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}

// Repository Layer
@Repository
class UserRepository {
    
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserRepository() {
        // Initialize with sample data
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        createSampleUser("John Doe", "john@example.com", 30);
        createSampleUser("Jane Smith", "jane@example.com", 25);
        createSampleUser("Bob Johnson", "bob@example.com", 35);
        createSampleUser("Alice Brown", "alice@example.com", 28);
    }
    
    private void createSampleUser(String name, String email, int age) {
        User user = new User();
        user.setId(idGenerator.getAndIncrement());
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        users.put(user.getId(), user);
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
    
    public User findById(Long id) {
        return users.get(id);
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        users.put(user.getId(), user);
        return user;
    }
    
    public boolean deleteById(Long id) {
        return users.remove(id) != null;
    }
    
    public List<User> search(String name, String email, Integer minAge, Integer maxAge) {
        return users.values().stream()
            .filter(user -> name == null || user.getName().toLowerCase().contains(name.toLowerCase()))
            .filter(user -> email == null || user.getEmail().toLowerCase().contains(email.toLowerCase()))
            .filter(user -> minAge == null || user.getAge() >= minAge)
            .filter(user -> maxAge == null || user.getAge() <= maxAge)
            .collect(Collectors.toList());
    }
}

@Repository
class ProductRepository {
    
    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public ProductRepository() {
        // Initialize with sample data
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        createSampleProduct("Laptop", "High-performance laptop", 999.99, "Electronics");
        createSampleProduct("Smartphone", "Latest model smartphone", 699.99, "Electronics");
        createSampleProduct("Book", "Java programming book", 49.99, "Books");
        createSampleProduct("Headphones", "Wireless headphones", 199.99, "Electronics");
    }
    
    private void createSampleProduct(String name, String description, double price, String category) {
        Product product = new Product();
        product.setId(idGenerator.getAndIncrement());
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());
        products.put(product.getId(), product);
    }
    
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }
    
    public Product findById(Long id) {
        return products.get(id);
    }
    
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(idGenerator.getAndIncrement());
        }
        products.put(product.getId(), product);
        return product;
    }
    
    public List<Product> findByCategory(String category) {
        return products.values().stream()
            .filter(product -> product.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }
}

// Domain Models
class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

class Product {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private LocalDateTime createdAt;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

// Request/Response DTOs
class CreateUserRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotNull(message = "Age is required")
    private Integer age;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}

class UpdateUserRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotNull(message = "Age is required")
    private Integer age;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}

class CreateProductRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Price is required")
    private Double price;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}

class UserStats {
    private int totalUsers;
    private double averageAge;
    private int oldestUser;
    private int youngestUser;
    
    // Getters and setters
    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
    
    public double getAverageAge() { return averageAge; }
    public void setAverageAge(double averageAge) { this.averageAge = averageAge; }
    
    public int getOldestUser() { return oldestUser; }
    public void setOldestUser(int oldestUser) { this.oldestUser = oldestUser; }
    
    public int getYoungestUser() { return youngestUser; }
    public void setYoungestUser(int youngestUser) { this.youngestUser = youngestUser; }
}
```

---

## ⚙️ Part 3: Configuration and Profiles (30 minutes)

### **External Configuration**

Spring Boot supports various configuration sources and formats, allowing you to externalize configuration from your code.

### **Configuration and Profiles Demo**

Create `SpringBootConfigurationDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.CommandLineRunner;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Spring Boot configuration and profiles demonstration
 * Shows how to manage configuration across different environments
 */
@SpringBootApplication
@EnableConfigurationProperties({DatabaseProperties.class, ApplicationProperties.class})
public class SpringBootConfigurationDemo implements CommandLineRunner {
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private DatabaseProperties databaseProperties;
    
    @Autowired
    private ApplicationProperties applicationProperties;
    
    @Autowired
    private ConfigurationService configurationService;
    
    public static void main(String[] args) {
        System.out.println("=== Spring Boot Configuration Demo ===\n");
        
        SpringApplication app = new SpringApplication(SpringBootConfigurationDemo.class);
        
        // Set default properties
        app.setDefaultProperties(getDefaultProperties());
        
        app.run(args);
    }
    
    private static java.util.Properties getDefaultProperties() {
        java.util.Properties props = new java.util.Properties();
        
        // Application properties
        props.setProperty("spring.application.name", "configuration-demo");
        props.setProperty("server.port", "8080");
        
        // Database properties
        props.setProperty("database.url", "jdbc:h2:mem:testdb");
        props.setProperty("database.username", "sa");
        props.setProperty("database.password", "");
        props.setProperty("database.pool.initial-size", "5");
        props.setProperty("database.pool.max-size", "20");
        props.setProperty("database.pool.timeout", "30000");
        
        // Application-specific properties
        props.setProperty("app.name", "Spring Boot Demo");
        props.setProperty("app.version", "1.0.0");
        props.setProperty("app.description", "Configuration demonstration application");
        props.setProperty("app.features[0]", "Configuration Management");
        props.setProperty("app.features[1]", "Profile Support");
        props.setProperty("app.features[2]", "Property Binding");
        
        // API configuration
        props.setProperty("app.api.timeout", "5000");
        props.setProperty("app.api.retry-count", "3");
        props.setProperty("app.api.base-url", "https://api.example.com");
        
        // Email configuration
        props.setProperty("app.email.enabled", "true");
        props.setProperty("app.email.smtp.host", "smtp.gmail.com");
        props.setProperty("app.email.smtp.port", "587");
        props.setProperty("app.email.from", "noreply@example.com");
        
        // Cache configuration
        props.setProperty("app.cache.enabled", "true");
        props.setProperty("app.cache.ttl", "3600");
        props.setProperty("app.cache.max-size", "1000");
        
        return props;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Spring Boot Configuration Demo Started!\n");
        
        // Show active profiles
        showActiveProfiles();
        
        // Show configuration properties
        showConfigurationProperties();
        
        // Show environment-specific services
        showProfileSpecificServices();
        
        // Show property binding
        showPropertyBinding();
        
        System.out.println("\n✅ Configuration demo completed successfully!");
    }
    
    private void showActiveProfiles() {
        System.out.println("1. Active Profiles:\n");
        
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();
        
        System.out.println("   Active profiles: " + Arrays.toString(activeProfiles));
        System.out.println("   Default profiles: " + Arrays.toString(defaultProfiles));
        
        // Show profile-specific behavior
        if (activeProfiles.length == 0) {
            System.out.println("   Running with default profile");
        } else {
            for (String profile : activeProfiles) {
                System.out.println("   Profile '" + profile + "' is active");
            }
        }
        System.out.println();
    }
    
    private void showConfigurationProperties() {
        System.out.println("2. Configuration Properties:\n");
        
        // Database properties
        System.out.println("   Database Configuration:");
        System.out.println("   - URL: " + databaseProperties.getUrl());
        System.out.println("   - Username: " + databaseProperties.getUsername());
        System.out.println("   - Password: " + maskPassword(databaseProperties.getPassword()));
        System.out.println("   - Pool initial size: " + databaseProperties.getPool().getInitialSize());
        System.out.println("   - Pool max size: " + databaseProperties.getPool().getMaxSize());
        System.out.println("   - Pool timeout: " + databaseProperties.getPool().getTimeout());
        
        // Application properties
        System.out.println("\n   Application Configuration:");
        System.out.println("   - Name: " + applicationProperties.getName());
        System.out.println("   - Version: " + applicationProperties.getVersion());
        System.out.println("   - Description: " + applicationProperties.getDescription());
        System.out.println("   - Features: " + applicationProperties.getFeatures());
        
        // API properties
        System.out.println("\n   API Configuration:");
        System.out.println("   - Base URL: " + applicationProperties.getApi().getBaseUrl());
        System.out.println("   - Timeout: " + applicationProperties.getApi().getTimeout());
        System.out.println("   - Retry count: " + applicationProperties.getApi().getRetryCount());
        
        // Email properties
        System.out.println("\n   Email Configuration:");
        System.out.println("   - Enabled: " + applicationProperties.getEmail().isEnabled());
        System.out.println("   - SMTP Host: " + applicationProperties.getEmail().getSmtp().getHost());
        System.out.println("   - SMTP Port: " + applicationProperties.getEmail().getSmtp().getPort());
        System.out.println("   - From: " + applicationProperties.getEmail().getFrom());
        
        // Cache properties
        System.out.println("\n   Cache Configuration:");
        System.out.println("   - Enabled: " + applicationProperties.getCache().isEnabled());
        System.out.println("   - TTL: " + applicationProperties.getCache().getTtl());
        System.out.println("   - Max size: " + applicationProperties.getCache().getMaxSize());
        
        System.out.println();
    }
    
    private void showProfileSpecificServices() {
        System.out.println("3. Profile-Specific Services:\n");
        
        configurationService.showEnvironmentInfo();
        System.out.println();
    }
    
    private void showPropertyBinding() {
        System.out.println("4. Property Binding Examples:\n");
        
        // Direct property injection
        System.out.println("   Direct property values:");
        System.out.println("   - Application name: " + environment.getProperty("spring.application.name"));
        System.out.println("   - Server port: " + environment.getProperty("server.port"));
        System.out.println("   - Database URL: " + environment.getProperty("database.url"));
        
        // Complex property binding
        System.out.println("\n   Complex property binding:");
        System.out.println("   - Features list: " + applicationProperties.getFeatures());
        System.out.println("   - Nested objects: Database pool configuration");
        System.out.println("   - Boolean values: Email enabled = " + applicationProperties.getEmail().isEnabled());
        
        System.out.println();
    }
    
    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "[empty]";
        }
        return "*".repeat(password.length());
    }
}

// Database configuration properties
@ConfigurationProperties(prefix = "database")
class DatabaseProperties {
    private String url;
    private String username;
    private String password;
    private Pool pool = new Pool();
    
    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Pool getPool() { return pool; }
    public void setPool(Pool pool) { this.pool = pool; }
    
    public static class Pool {
        private int initialSize = 5;
        private int maxSize = 20;
        private int timeout = 30000;
        
        public int getInitialSize() { return initialSize; }
        public void setInitialSize(int initialSize) { this.initialSize = initialSize; }
        
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }
}

// Application configuration properties
@ConfigurationProperties(prefix = "app")
class ApplicationProperties {
    private String name;
    private String version;
    private String description;
    private List<String> features;
    private Api api = new Api();
    private Email email = new Email();
    private Cache cache = new Cache();
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    
    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }
    
    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }
    
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }
    
    public static class Api {
        private String baseUrl;
        private int timeout = 5000;
        private int retryCount = 3;
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    }
    
    public static class Email {
        private boolean enabled = true;
        private String from;
        private Smtp smtp = new Smtp();
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public Smtp getSmtp() { return smtp; }
        public void setSmtp(Smtp smtp) { this.smtp = smtp; }
        
        public static class Smtp {
            private String host;
            private int port = 587;
            
            public String getHost() { return host; }
            public void setHost(String host) { this.host = host; }
            
            public int getPort() { return port; }
            public void setPort(int port) { this.port = port; }
        }
    }
    
    public static class Cache {
        private boolean enabled = true;
        private int ttl = 3600;
        private int maxSize = 1000;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getTtl() { return ttl; }
        public void setTtl(int ttl) { this.ttl = ttl; }
        
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
    }
}

// Configuration service with profile-specific beans
@Component
class ConfigurationService {
    
    @Autowired(required = false)
    private DevelopmentService developmentService;
    
    @Autowired(required = false)
    private ProductionService productionService;
    
    @Autowired(required = false)
    private TestingService testingService;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
    public void showEnvironmentInfo() {
        System.out.println("   Environment-specific services:");
        
        if (developmentService != null) {
            developmentService.showInfo();
        }
        
        if (productionService != null) {
            productionService.showInfo();
        }
        
        if (testingService != null) {
            testingService.showInfo();
        }
        
        if (developmentService == null && productionService == null && testingService == null) {
            System.out.println("   - Using default configuration (no specific profile services)");
        }
    }
}

// Profile-specific service beans
@Component
@Profile("development")
class DevelopmentService {
    
    public void showInfo() {
        System.out.println("   - Development environment detected");
        System.out.println("     * Debug logging enabled");
        System.out.println("     * Hot reloading enabled");
        System.out.println("     * Mock services enabled");
    }
}

@Component
@Profile("production")
class ProductionService {
    
    public void showInfo() {
        System.out.println("   - Production environment detected");
        System.out.println("     * Performance optimization enabled");
        System.out.println("     * Security hardening enabled");
        System.out.println("     * Monitoring enabled");
    }
}

@Component
@Profile("test")
class TestingService {
    
    public void showInfo() {
        System.out.println("   - Testing environment detected");
        System.out.println("     * Test database enabled");
        System.out.println("     * Mock external services enabled");
        System.out.println("     * Fast startup enabled");
    }
}
```

---

## 🏗️ Part 4: Complete Microservice Example (30 minutes)

### **E-commerce Product Service**

Let's build a complete microservice using all Spring Boot features we've learned.

Create `ProductMicroservice.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Complete E-commerce Product Microservice
 * Demonstrates production-ready Spring Boot microservice
 */
@SpringBootApplication
@EnableScheduling
public class ProductMicroservice {
    
    public static void main(String[] args) {
        System.out.println("=== E-commerce Product Microservice ===\n");
        
        SpringApplication app = new SpringApplication(ProductMicroservice.class);
        
        // Configure application properties
        java.util.Properties props = new java.util.Properties();
        props.setProperty("spring.application.name", "product-service");
        props.setProperty("server.port", "8080");
        props.setProperty("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        props.setProperty("management.endpoint.health.show-details", "always");
        props.setProperty("logging.level.com.ecommerce", "INFO");
        
        app.setDefaultProperties(props);
        app.run(args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("🚀 Product Microservice started successfully!");
        System.out.println("📡 Service running on: http://localhost:8080");
        System.out.println("🔍 Health check: http://localhost:8080/actuator/health");
        System.out.println("📊 Info endpoint: http://localhost:8080/actuator/info");
        System.out.println("📈 Metrics: http://localhost:8080/actuator/metrics");
        System.out.println("\n📋 API Documentation:");
        System.out.println("   GET    /api/products                 - Get all products");
        System.out.println("   POST   /api/products                 - Create product");
        System.out.println("   GET    /api/products/{id}            - Get product by ID");
        System.out.println("   PUT    /api/products/{id}            - Update product");
        System.out.println("   DELETE /api/products/{id}            - Delete product");
        System.out.println("   GET    /api/products/search          - Search products");
        System.out.println("   GET    /api/products/category/{cat}  - Get by category");
        System.out.println("   GET    /api/products/featured        - Get featured products");
        System.out.println("   GET    /api/products/stats           - Get product statistics");
        System.out.println();
    }
}

// REST Controller with comprehensive endpoints
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy) {
        
        List<Product> products = productService.findAll(page, size, sortBy);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return product != null ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, 
                                                @Valid @RequestBody UpdateProductRequest request) {
        Product product = productService.updateProduct(id, request);
        return product != null ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock) {
        
        List<Product> products = productService.searchProducts(name, category, minPrice, maxPrice, inStock);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.findByCategory(category);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/featured")
    public ResponseEntity<List<Product>> getFeaturedProducts() {
        List<Product> products = productService.findFeaturedProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ProductStatistics> getProductStats() {
        ProductStatistics stats = productService.getProductStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @PutMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable Long id, 
                                              @RequestParam int quantity) {
        Product product = productService.updateStock(id, quantity);
        return product != null ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}/price")
    public ResponseEntity<Product> updatePrice(@PathVariable Long id, 
                                              @RequestParam double price) {
        Product product = productService.updatePrice(id, price);
        return product != null ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }
}

// Service layer with business logic
@Service
class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private MetricsService metricsService;
    
    @Value("${app.featured.threshold:4.0}")
    private double featuredThreshold;
    
    public List<Product> findAll(int page, int size, String sortBy) {
        metricsService.incrementProductQueries();
        return productRepository.findAll(page, size, sortBy);
    }
    
    public Product findById(Long id) {
        metricsService.incrementProductQueries();
        return productRepository.findById(id);
    }
    
    public Product createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(request.getStockQuantity());
        product.setFeatured(request.isFeatured());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        
        Product savedProduct = productRepository.save(product);
        metricsService.incrementProductCreations();
        return savedProduct;
    }
    
    public Product updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id);
        if (product != null) {
            product.setName(request.getName());
            product.setDescription(request.getDescription());
            product.setPrice(request.getPrice());
            product.setCategory(request.getCategory());
            product.setStockQuantity(request.getStockQuantity());
            product.setFeatured(request.isFeatured());
            product.setUpdatedAt(LocalDateTime.now());
            
            Product updatedProduct = productRepository.save(product);
            metricsService.incrementProductUpdates();
            return updatedProduct;
        }
        return null;
    }
    
    public boolean deleteProduct(Long id) {
        boolean deleted = productRepository.deleteById(id);
        if (deleted) {
            metricsService.incrementProductDeletions();
        }
        return deleted;
    }
    
    public List<Product> searchProducts(String name, String category, Double minPrice, Double maxPrice, Boolean inStock) {
        metricsService.incrementProductQueries();
        return productRepository.search(name, category, minPrice, maxPrice, inStock);
    }
    
    public List<Product> findByCategory(String category) {
        metricsService.incrementProductQueries();
        return productRepository.findByCategory(category);
    }
    
    public List<Product> findFeaturedProducts() {
        metricsService.incrementProductQueries();
        return productRepository.findFeaturedProducts();
    }
    
    public Product updateStock(Long id, int quantity) {
        Product product = productRepository.findById(id);
        if (product != null) {
            product.setStockQuantity(quantity);
            product.setUpdatedAt(LocalDateTime.now());
            return productRepository.save(product);
        }
        return null;
    }
    
    public Product updatePrice(Long id, double price) {
        Product product = productRepository.findById(id);
        if (product != null) {
            product.setPrice(price);
            product.setUpdatedAt(LocalDateTime.now());
            return productRepository.save(product);
        }
        return null;
    }
    
    public ProductStatistics getProductStatistics() {
        return productRepository.getStatistics();
    }
}

// Repository layer
@Repository
class ProductRepository {
    
    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public ProductRepository() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        createSampleProduct("iPhone 14", "Latest Apple smartphone", 999.99, "Electronics", 50, true);
        createSampleProduct("MacBook Pro", "High-performance laptop", 1999.99, "Electronics", 25, true);
        createSampleProduct("Java Programming Book", "Comprehensive Java guide", 49.99, "Books", 100, false);
        createSampleProduct("Wireless Headphones", "Noise-cancelling headphones", 299.99, "Electronics", 75, true);
        createSampleProduct("Office Chair", "Ergonomic office chair", 199.99, "Furniture", 20, false);
        createSampleProduct("Coffee Maker", "Programmable coffee maker", 129.99, "Appliances", 30, false);
        createSampleProduct("Running Shoes", "Professional running shoes", 159.99, "Sports", 40, true);
        createSampleProduct("Tablet", "10-inch tablet", 399.99, "Electronics", 60, false);
    }
    
    private void createSampleProduct(String name, String description, double price, 
                                   String category, int stock, boolean featured) {
        Product product = new Product();
        product.setId(idGenerator.getAndIncrement());
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setStockQuantity(stock);
        product.setFeatured(featured);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        
        products.put(product.getId(), product);
    }
    
    public List<Product> findAll(int page, int size, String sortBy) {
        List<Product> allProducts = new ArrayList<>(products.values());
        
        // Simple sorting
        if ("name".equals(sortBy)) {
            allProducts.sort(Comparator.comparing(Product::getName));
        } else if ("price".equals(sortBy)) {
            allProducts.sort(Comparator.comparing(Product::getPrice));
        } else if ("category".equals(sortBy)) {
            allProducts.sort(Comparator.comparing(Product::getCategory));
        }
        
        // Simple pagination
        int start = page * size;
        int end = Math.min(start + size, allProducts.size());
        
        if (start >= allProducts.size()) {
            return new ArrayList<>();
        }
        
        return allProducts.subList(start, end);
    }
    
    public Product findById(Long id) {
        return products.get(id);
    }
    
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(idGenerator.getAndIncrement());
        }
        products.put(product.getId(), product);
        return product;
    }
    
    public boolean deleteById(Long id) {
        return products.remove(id) != null;
    }
    
    public List<Product> search(String name, String category, Double minPrice, Double maxPrice, Boolean inStock) {
        return products.values().stream()
            .filter(p -> name == null || p.getName().toLowerCase().contains(name.toLowerCase()))
            .filter(p -> category == null || p.getCategory().equalsIgnoreCase(category))
            .filter(p -> minPrice == null || p.getPrice() >= minPrice)
            .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
            .filter(p -> inStock == null || (inStock ? p.getStockQuantity() > 0 : p.getStockQuantity() == 0))
            .collect(Collectors.toList());
    }
    
    public List<Product> findByCategory(String category) {
        return products.values().stream()
            .filter(p -> p.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }
    
    public List<Product> findFeaturedProducts() {
        return products.values().stream()
            .filter(Product::isFeatured)
            .collect(Collectors.toList());
    }
    
    public ProductStatistics getStatistics() {
        List<Product> allProducts = new ArrayList<>(products.values());
        
        ProductStatistics stats = new ProductStatistics();
        stats.setTotalProducts(allProducts.size());
        stats.setTotalValue(allProducts.stream().mapToDouble(p -> p.getPrice() * p.getStockQuantity()).sum());
        stats.setAveragePrice(allProducts.stream().mapToDouble(Product::getPrice).average().orElse(0.0));
        stats.setOutOfStockProducts((int) allProducts.stream().filter(p -> p.getStockQuantity() == 0).count());
        stats.setFeaturedProducts((int) allProducts.stream().filter(Product::isFeatured).count());
        
        Map<String, Long> categoryCount = allProducts.stream()
            .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));
        stats.setCategoryCounts(categoryCount);
        
        return stats;
    }
}

// Metrics service
@Service
class MetricsService {
    
    private final AtomicLong productQueries = new AtomicLong(0);
    private final AtomicLong productCreations = new AtomicLong(0);
    private final AtomicLong productUpdates = new AtomicLong(0);
    private final AtomicLong productDeletions = new AtomicLong(0);
    
    public void incrementProductQueries() {
        productQueries.incrementAndGet();
    }
    
    public void incrementProductCreations() {
        productCreations.incrementAndGet();
    }
    
    public void incrementProductUpdates() {
        productUpdates.incrementAndGet();
    }
    
    public void incrementProductDeletions() {
        productDeletions.incrementAndGet();
    }
    
    public long getProductQueries() { return productQueries.get(); }
    public long getProductCreations() { return productCreations.get(); }
    public long getProductUpdates() { return productUpdates.get(); }
    public long getProductDeletions() { return productDeletions.get(); }
}

// Scheduled tasks
@Component
class ScheduledTasks {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void logMetrics() {
        System.out.println("📊 Service Metrics:");
        System.out.println("   Product queries: " + metricsService.getProductQueries());
        System.out.println("   Product creations: " + metricsService.getProductCreations());
        System.out.println("   Product updates: " + metricsService.getProductUpdates());
        System.out.println("   Product deletions: " + metricsService.getProductDeletions());
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void generateStatistics() {
        ProductStatistics stats = productService.getProductStatistics();
        System.out.println("📈 Product Statistics:");
        System.out.println("   Total products: " + stats.getTotalProducts());
        System.out.println("   Total value: $" + String.format("%.2f", stats.getTotalValue()));
        System.out.println("   Average price: $" + String.format("%.2f", stats.getAveragePrice()));
        System.out.println("   Out of stock: " + stats.getOutOfStockProducts());
        System.out.println("   Featured products: " + stats.getFeaturedProducts());
    }
}

// Health indicator
@Component
class ProductHealthIndicator implements HealthIndicator {
    
    @Autowired
    private ProductService productService;
    
    @Override
    public Health health() {
        try {
            ProductStatistics stats = productService.getProductStatistics();
            
            if (stats.getTotalProducts() > 0) {
                return Health.up()
                    .withDetail("status", "Healthy")
                    .withDetail("totalProducts", stats.getTotalProducts())
                    .withDetail("outOfStockProducts", stats.getOutOfStockProducts())
                    .withDetail("featuredProducts", stats.getFeaturedProducts())
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "No products available")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "Error checking product health")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

// Info contributor
@Component
class ProductInfoContributor implements InfoContributor {
    
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("service", "Product Microservice")
               .withDetail("version", "1.0.0")
               .withDetail("description", "E-commerce product management service")
               .withDetail("features", Arrays.asList(
                   "Product CRUD operations",
                   "Product search and filtering",
                   "Stock management",
                   "Featured products",
                   "Category management",
                   "Real-time metrics"
               ))
               .withDetail("buildTime", LocalDateTime.now().toString());
    }
}

// Domain model
class Product {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private Integer stockQuantity;
    private boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

// Request DTOs
class CreateProductRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    
    private boolean featured = false;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
}

class UpdateProductRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    
    private boolean featured = false;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
}

// Statistics DTO
class ProductStatistics {
    private int totalProducts;
    private double totalValue;
    private double averagePrice;
    private int outOfStockProducts;
    private int featuredProducts;
    private Map<String, Long> categoryCounts;
    
    // Getters and setters
    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }
    
    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }
    
    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }
    
    public int getOutOfStockProducts() { return outOfStockProducts; }
    public void setOutOfStockProducts(int outOfStockProducts) { this.outOfStockProducts = outOfStockProducts; }
    
    public int getFeaturedProducts() { return featuredProducts; }
    public void setFeaturedProducts(int featuredProducts) { this.featuredProducts = featuredProducts; }
    
    public Map<String, Long> getCategoryCounts() { return categoryCounts; }
    public void setCategoryCounts(Map<String, Long> categoryCounts) { this.categoryCounts = categoryCounts; }
}
```

---

## 🎯 Day 18 Summary

**Concepts Mastered:**
- ✅ **Spring Boot Auto-Configuration**: Automatic bean configuration based on classpath
- ✅ **Embedded Servers**: Tomcat, Jetty, and server configuration
- ✅ **REST API Development**: Complete REST endpoints with Spring Boot
- ✅ **Configuration Management**: Properties, YAML, and externalized configuration
- ✅ **Production Features**: Health checks, metrics, and monitoring
- ✅ **Complete Microservice**: Production-ready service with all features

**Key Takeaways:**
1. **Auto-configuration** eliminates boilerplate configuration code
2. **Embedded servers** simplify deployment and remove external dependencies
3. **Spring Boot Starter** dependencies provide curated library sets
4. **Actuator endpoints** provide production-ready monitoring capabilities
5. **Configuration properties** enable externalized configuration management
6. **Profile-based configuration** supports multiple environments

**Enterprise Benefits:**
- **Rapid Development**: Minimal configuration, maximum productivity
- **Production-Ready**: Built-in monitoring, health checks, and metrics
- **Microservice Architecture**: Perfect for cloud-native applications
- **DevOps Friendly**: Easy deployment and container support
- **Scalable**: Embedded servers scale efficiently

**Next Steps:**
- Day 19: Spring MVC & Web Applications with templates and forms
- Advanced Spring Boot features: Security, data access, and testing
- Microservice patterns: Service discovery, circuit breakers, and API gateways
- Cloud deployment: Docker, Kubernetes, and CI/CD pipelines

---

**🎉 Congratulations! You've mastered Spring Boot fundamentals and built a complete production-ready microservice!**