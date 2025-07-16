# Day 17: Introduction to Spring Framework (July 26, 2025)

**Date:** July 26, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Spring Framework fundamentals: Dependency Injection, IoC, and configuration patterns

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand Spring Framework architecture and core concepts
- ✅ Master Dependency Injection (DI) and Inversion of Control (IoC)
- ✅ Learn annotation-based and Java-based configuration
- ✅ Implement Spring beans and manage their lifecycle
- ✅ Build a complete Spring application with multiple layers
- ✅ Understand Spring's role in enterprise Java development

---

## 🌱 Part 1: Spring Framework Foundation (45 minutes)

### **Understanding Spring Framework**

**Spring Framework** is a comprehensive programming and configuration model for modern Java-based enterprise applications. It provides infrastructure support so you can focus on your application's business logic.

**Core Principles:**
- **Dependency Injection (DI)**: Objects define their dependencies through constructor arguments, factory method arguments, or properties
- **Inversion of Control (IoC)**: The framework controls object creation and dependency management
- **Aspect-Oriented Programming (AOP)**: Separation of cross-cutting concerns
- **Lightweight Container**: Manages object lifecycle and configuration

### **Spring Architecture Overview**

Create `SpringArchitectureDemo.java`:

```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Spring Framework architecture demonstration
 * Shows core concepts: IoC, DI, beans, and component scanning
 */
@Configuration
public class SpringArchitectureDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Spring Framework Architecture Demo ===\n");
        
        // Create Spring Application Context
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext(SpringArchitectureDemo.class);
        
        // Demonstrate IoC container
        demonstrateIoCContainer(context);
        
        // Demonstrate dependency injection
        demonstrateDependencyInjection(context);
        
        // Demonstrate bean lifecycle
        demonstrateBeanLifecycle(context);
        
        // Clean up
        context.close();
    }
    
    private static void demonstrateIoCContainer(AnnotationConfigApplicationContext context) {
        System.out.println("1. IoC Container Demonstration:\n");
        
        // Get bean from container
        UserService userService = context.getBean(UserService.class);
        userService.createUser("John Doe", "john@example.com");
        
        // Show container manages object creation
        UserService anotherInstance = context.getBean(UserService.class);
        System.out.println("Same instance (singleton): " + (userService == anotherInstance));
        
        // Display all beans in container
        System.out.println("\n📦 Beans in Container:");
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            if (!beanName.startsWith("org.springframework")) {
                System.out.println("   - " + beanName + " (" + context.getBean(beanName).getClass().getSimpleName() + ")");
            }
        }
        System.out.println();
    }
    
    private static void demonstrateDependencyInjection(AnnotationConfigApplicationContext context) {
        System.out.println("2. Dependency Injection Demonstration:\n");
        
        // Get service with injected dependencies
        UserService userService = context.getBean(UserService.class);
        System.out.println("UserService dependencies injected automatically:");
        System.out.println("   - UserRepository: " + userService.getRepositoryInfo());
        System.out.println("   - NotificationService: " + userService.getNotificationInfo());
        
        // Demonstrate different injection types
        OrderService orderService = context.getBean(OrderService.class);
        orderService.processOrder("ORD-001", 99.99);
        
        System.out.println();
    }
    
    private static void demonstrateBeanLifecycle(AnnotationConfigApplicationContext context) {
        System.out.println("3. Bean Lifecycle Demonstration:\n");
        
        // Lifecycle methods are called automatically
        LifecycleBean lifecycleBean = context.getBean(LifecycleBean.class);
        lifecycleBean.doWork();
        
        System.out.println("   Bean lifecycle methods called during creation and destruction");
        System.out.println();
    }
    
    // Bean definitions
    @Bean
    public NotificationService emailNotificationService() {
        return new EmailNotificationService();
    }
    
    @Bean
    public NotificationService smsNotificationService() {
        return new SmsNotificationService();
    }
    
    @Bean
    public PaymentProcessor creditCardProcessor() {
        return new CreditCardProcessor();
    }
}

// Service layer components
@Service
class UserService {
    
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    // Constructor injection (recommended)
    public UserService(UserRepository userRepository, 
                      @Qualifier("emailNotificationService") NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        System.out.println("✅ UserService created with constructor injection");
    }
    
    public void createUser(String name, String email) {
        System.out.println("Creating user: " + name + " (" + email + ")");
        
        // Use injected dependencies
        User user = new User(name, email);
        userRepository.save(user);
        notificationService.sendNotification("Welcome " + name + "!");
        
        System.out.println("✅ User created successfully");
    }
    
    public String getRepositoryInfo() {
        return userRepository.getClass().getSimpleName();
    }
    
    public String getNotificationInfo() {
        return notificationService.getClass().getSimpleName();
    }
}

@Service
class OrderService {
    
    @Autowired
    private PaymentProcessor paymentProcessor;
    
    @Autowired
    @Qualifier("smsNotificationService")
    private NotificationService notificationService;
    
    public void processOrder(String orderId, double amount) {
        System.out.println("Processing order: " + orderId + " for $" + amount);
        
        // Use injected dependencies
        boolean paymentSuccess = paymentProcessor.processPayment(amount);
        
        if (paymentSuccess) {
            notificationService.sendNotification("Order " + orderId + " processed successfully!");
            System.out.println("✅ Order processed successfully");
        } else {
            System.out.println("❌ Order processing failed");
        }
    }
}

// Repository layer
@Repository
class UserRepository {
    
    public void save(User user) {
        System.out.println("   📀 Saving user to database: " + user.getName());
        // Database save logic would go here
    }
    
    public User findById(Long id) {
        System.out.println("   🔍 Finding user by ID: " + id);
        return new User("Found User", "found@example.com");
    }
}

// Notification services
interface NotificationService {
    void sendNotification(String message);
}

class EmailNotificationService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("   📧 Email notification: " + message);
    }
}

class SmsNotificationService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("   📱 SMS notification: " + message);
    }
}

// Payment processing
interface PaymentProcessor {
    boolean processPayment(double amount);
}

class CreditCardProcessor implements PaymentProcessor {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("   💳 Processing credit card payment: $" + amount);
        return true; // Simulate successful payment
    }
}

// Bean lifecycle demonstration
@Component
class LifecycleBean {
    
    @PostConstruct
    public void init() {
        System.out.println("   🔄 LifecycleBean initialized (@PostConstruct)");
    }
    
    public void doWork() {
        System.out.println("   ⚡ LifecycleBean doing work...");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("   🧹 LifecycleBean cleaning up (@PreDestroy)");
    }
}

// Domain model
class User {
    private String name;
    private String email;
    
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    public String getName() { return name; }
    public String getEmail() { return email; }
}
```

---

## 🔧 Part 2: Spring Configuration Patterns (45 minutes)

### **Configuration Approaches**

Spring provides multiple ways to configure beans and dependencies:
1. **Annotation-based Configuration** (modern approach)
2. **Java-based Configuration** (type-safe)
3. **XML Configuration** (legacy, still supported)

### **Comprehensive Configuration Demo**

Create `SpringConfigurationDemo.java`:

```java
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Spring configuration patterns demonstration
 * Shows annotation-based, Java-based, and property-based configuration
 */
@Configuration
@ComponentScan(basePackages = "com.example")
@PropertySource("classpath:application.properties")
public class SpringConfigurationDemo {
    
    @Autowired
    private Environment environment;
    
    public static void main(String[] args) {
        System.out.println("=== Spring Configuration Demo ===\n");
        
        // Create application context with configuration
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext(SpringConfigurationDemo.class);
        
        // Demonstrate annotation-based configuration
        demonstrateAnnotationConfig(context);
        
        // Demonstrate Java-based configuration
        demonstrateJavaConfig(context);
        
        // Demonstrate property injection
        demonstratePropertyInjection(context);
        
        // Demonstrate conditional configuration
        demonstrateConditionalConfig(context);
        
        // Demonstrate profiles
        demonstrateProfiles(context);
        
        context.close();
    }
    
    private static void demonstrateAnnotationConfig(AnnotationConfigApplicationContext context) {
        System.out.println("1. Annotation-based Configuration:\n");
        
        // Get beans created through annotations
        BookService bookService = context.getBean(BookService.class);
        bookService.addBook("Spring in Action", "Craig Walls");
        
        System.out.println("   Components discovered through @ComponentScan:");
        System.out.println("   - BookService (@Service)");
        System.out.println("   - BookRepository (@Repository)");
        System.out.println("   - CacheService (@Component)");
        System.out.println();
    }
    
    private static void demonstrateJavaConfig(AnnotationConfigApplicationContext context) {
        System.out.println("2. Java-based Configuration:\n");
        
        // Get beans defined in @Configuration class
        DatabaseConfig dbConfig = context.getBean(DatabaseConfig.class);
        System.out.println("   Database URL: " + dbConfig.getUrl());
        System.out.println("   Connection Pool: " + dbConfig.getConnectionPool().getClass().getSimpleName());
        
        // Get service with injected configuration
        ReportService reportService = context.getBean(ReportService.class);
        reportService.generateReport();
        
        System.out.println();
    }
    
    private static void demonstratePropertyInjection(AnnotationConfigApplicationContext context) {
        System.out.println("3. Property Injection:\n");
        
        // Get service with injected properties
        ConfigurableService configurableService = context.getBean(ConfigurableService.class);
        configurableService.displayConfiguration();
        
        System.out.println();
    }
    
    private static void demonstrateConditionalConfig(AnnotationConfigApplicationContext context) {
        System.out.println("4. Conditional Configuration:\n");
        
        // Check if conditional beans exist
        try {
            DevelopmentService devService = context.getBean(DevelopmentService.class);
            devService.debugInfo();
        } catch (Exception e) {
            System.out.println("   DevelopmentService not available (not in development profile)");
        }
        
        System.out.println();
    }
    
    private static void demonstrateProfiles(AnnotationConfigApplicationContext context) {
        System.out.println("5. Spring Profiles:\n");
        
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        String[] defaultProfiles = context.getEnvironment().getDefaultProfiles();
        
        System.out.println("   Active profiles: " + Arrays.toString(activeProfiles));
        System.out.println("   Default profiles: " + Arrays.toString(defaultProfiles));
        
        System.out.println();
    }
    
    // Java-based bean definitions
    @Bean
    public DatabaseConfig databaseConfig() {
        return new DatabaseConfig("jdbc:h2:mem:testdb", "sa", "");
    }
    
    @Bean
    public ConnectionPool connectionPool() {
        return new HikariConnectionPool(databaseConfig());
    }
    
    @Bean
    public ReportService reportService() {
        return new ReportService(connectionPool());
    }
    
    @Bean
    @Profile("development")
    public DevelopmentService developmentService() {
        return new DevelopmentService();
    }
    
    @Bean
    @Profile("production")
    public ProductionService productionService() {
        return new ProductionService();
    }
    
    // Property placeholder configurer
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        
        // Create default properties
        Properties properties = new Properties();
        properties.setProperty("app.name", "Spring Demo Application");
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("app.environment", "development");
        properties.setProperty("cache.enabled", "true");
        properties.setProperty("cache.size", "100");
        properties.setProperty("database.pool.size", "10");
        properties.setProperty("logging.level", "INFO");
        
        configurer.setProperties(properties);
        return configurer;
    }
}

// Service layer with annotation-based configuration
@Service
class BookService {
    
    private final BookRepository bookRepository;
    private final CacheService cacheService;
    
    public BookService(BookRepository bookRepository, CacheService cacheService) {
        this.bookRepository = bookRepository;
        this.cacheService = cacheService;
        System.out.println("✅ BookService created with constructor injection");
    }
    
    public void addBook(String title, String author) {
        System.out.println("Adding book: " + title + " by " + author);
        
        Book book = new Book(title, author);
        bookRepository.save(book);
        cacheService.cache("book:" + title, book);
        
        System.out.println("✅ Book added successfully");
    }
    
    public Book findBook(String title) {
        // Check cache first
        Book cachedBook = cacheService.get("book:" + title);
        if (cachedBook != null) {
            System.out.println("📦 Book found in cache: " + title);
            return cachedBook;
        }
        
        // Load from repository
        return bookRepository.findByTitle(title);
    }
}

@Repository
class BookRepository {
    
    public void save(Book book) {
        System.out.println("   📀 Saving book to database: " + book.getTitle());
    }
    
    public Book findByTitle(String title) {
        System.out.println("   🔍 Finding book by title: " + title);
        return new Book(title, "Unknown Author");
    }
}

@Component
class CacheService {
    
    private final java.util.Map<String, Object> cache = new java.util.concurrent.ConcurrentHashMap<>();
    
    public void cache(String key, Object value) {
        cache.put(key, value);
        System.out.println("   💾 Cached: " + key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }
}

// Java-based configuration beans
class DatabaseConfig {
    private final String url;
    private final String username;
    private final String password;
    private final ConnectionPool connectionPool;
    
    public DatabaseConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.connectionPool = new HikariConnectionPool(this);
        System.out.println("✅ DatabaseConfig created: " + url);
    }
    
    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public ConnectionPool getConnectionPool() { return connectionPool; }
}

interface ConnectionPool {
    void getConnection();
    void releaseConnection();
}

class HikariConnectionPool implements ConnectionPool {
    private final DatabaseConfig config;
    
    public HikariConnectionPool(DatabaseConfig config) {
        this.config = config;
        System.out.println("✅ HikariConnectionPool created for: " + config.getUrl());
    }
    
    @Override
    public void getConnection() {
        System.out.println("   🔗 Getting connection from pool");
    }
    
    @Override
    public void releaseConnection() {
        System.out.println("   🔓 Releasing connection to pool");
    }
}

class ReportService {
    private final ConnectionPool connectionPool;
    
    public ReportService(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        System.out.println("✅ ReportService created with connection pool");
    }
    
    public void generateReport() {
        System.out.println("Generating report...");
        connectionPool.getConnection();
        System.out.println("   📊 Report generated successfully");
        connectionPool.releaseConnection();
    }
}

// Property injection example
@Component
class ConfigurableService {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String appVersion;
    
    @Value("${app.environment}")
    private String environment;
    
    @Value("${cache.enabled:false}")
    private boolean cacheEnabled;
    
    @Value("${cache.size:50}")
    private int cacheSize;
    
    @Value("${database.pool.size:5}")
    private int poolSize;
    
    @Value("${logging.level:DEBUG}")
    private String loggingLevel;
    
    public void displayConfiguration() {
        System.out.println("   Application Configuration:");
        System.out.println("   - Name: " + appName);
        System.out.println("   - Version: " + appVersion);
        System.out.println("   - Environment: " + environment);
        System.out.println("   - Cache enabled: " + cacheEnabled);
        System.out.println("   - Cache size: " + cacheSize);
        System.out.println("   - Pool size: " + poolSize);
        System.out.println("   - Logging level: " + loggingLevel);
    }
}

// Profile-based configuration
@Profile("development")
class DevelopmentService {
    
    public void debugInfo() {
        System.out.println("   🔧 Development mode enabled");
        System.out.println("   - Debug logging: ON");
        System.out.println("   - Hot reload: ON");
        System.out.println("   - Mock services: ON");
    }
}

@Profile("production")
class ProductionService {
    
    public void optimizePerformance() {
        System.out.println("   🚀 Production mode enabled");
        System.out.println("   - Performance optimization: ON");
        System.out.println("   - Security hardening: ON");
        System.out.println("   - Monitoring: ON");
    }
}

// Domain model
class Book {
    private String title;
    private String author;
    
    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }
    
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
}
```

---

## 🔄 Part 3: Spring Bean Lifecycle and Scopes (30 minutes)

### **Understanding Bean Lifecycle**

Spring manages the complete lifecycle of beans from creation to destruction, providing hooks for custom initialization and cleanup.

### **Bean Lifecycle and Scopes Demo**

Create `SpringBeanLifecycleDemo.java`:

```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring bean lifecycle and scopes demonstration
 * Shows different bean scopes and lifecycle hooks
 */
@Configuration
public class SpringBeanLifecycleDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Spring Bean Lifecycle and Scopes Demo ===\n");
        
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext(SpringBeanLifecycleDemo.class);
        
        // Demonstrate bean lifecycle
        demonstrateBeanLifecycle(context);
        
        // Demonstrate bean scopes
        demonstrateBeanScopes(context);
        
        // Demonstrate lazy initialization
        demonstrateLazyInitialization(context);
        
        // Demonstrate depends-on
        demonstrateDependsOn(context);
        
        System.out.println("🔄 Closing application context...");
        context.close();
        System.out.println("✅ Application context closed\n");
    }
    
    private static void demonstrateBeanLifecycle(AnnotationConfigApplicationContext context) {
        System.out.println("1. Bean Lifecycle Demonstration:\n");
        
        // Bean was created during context initialization
        LifecycleAwareService service = context.getBean(LifecycleAwareService.class);
        service.doWork();
        
        // Lifecycle methods will be called during context shutdown
        System.out.println();
    }
    
    private static void demonstrateBeanScopes(AnnotationConfigApplicationContext context) {
        System.out.println("2. Bean Scopes Demonstration:\n");
        
        // Singleton scope (default)
        System.out.println("Singleton scope:");
        SingletonService singleton1 = context.getBean(SingletonService.class);
        SingletonService singleton2 = context.getBean(SingletonService.class);
        System.out.println("   Same instance: " + (singleton1 == singleton2));
        System.out.println("   Instance count: " + SingletonService.getInstanceCount());
        
        // Prototype scope
        System.out.println("\nPrototype scope:");
        PrototypeService prototype1 = context.getBean(PrototypeService.class);
        PrototypeService prototype2 = context.getBean(PrototypeService.class);
        System.out.println("   Same instance: " + (prototype1 == prototype2));
        System.out.println("   Instance count: " + PrototypeService.getInstanceCount());
        
        System.out.println();
    }
    
    private static void demonstrateLazyInitialization(AnnotationConfigApplicationContext context) {
        System.out.println("3. Lazy Initialization:\n");
        
        System.out.println("Getting lazy service (should trigger initialization):");
        LazyService lazyService = context.getBean(LazyService.class);
        lazyService.performAction();
        
        System.out.println();
    }
    
    private static void demonstrateDependsOn(AnnotationConfigApplicationContext context) {
        System.out.println("4. Depends-On Demonstration:\n");
        
        // DependentService depends on ConfigurationService
        DependentService dependentService = context.getBean(DependentService.class);
        dependentService.execute();
        
        System.out.println();
    }
    
    // Bean definitions with different scopes
    @Bean
    @Scope("singleton")
    public SingletonService singletonService() {
        return new SingletonService();
    }
    
    @Bean
    @Scope("prototype")
    public PrototypeService prototypeService() {
        return new PrototypeService();
    }
    
    @Bean
    @Scope("singleton")
    @Lazy
    public LazyService lazyService() {
        return new LazyService();
    }
    
    @Bean
    public ConfigurationService configurationService() {
        return new ConfigurationService();
    }
    
    @Bean
    @DependsOn("configurationService")
    public DependentService dependentService() {
        return new DependentService();
    }
}

// Lifecycle-aware service with multiple lifecycle hooks
@Component
class LifecycleAwareService implements InitializingBean, DisposableBean {
    
    private String status = "NOT_INITIALIZED";
    
    // 1. Constructor
    public LifecycleAwareService() {
        System.out.println("   🔨 Constructor called");
        this.status = "CONSTRUCTED";
    }
    
    // 2. @PostConstruct
    @PostConstruct
    public void init() {
        System.out.println("   🔄 @PostConstruct called");
        this.status = "POST_CONSTRUCT_COMPLETE";
    }
    
    // 3. InitializingBean.afterPropertiesSet()
    @Override
    public void afterPropertiesSet() {
        System.out.println("   ⚙️ afterPropertiesSet() called");
        this.status = "FULLY_INITIALIZED";
    }
    
    // Custom initialization method
    public void customInit() {
        System.out.println("   🔧 Custom initialization method called");
    }
    
    public void doWork() {
        System.out.println("   ⚡ LifecycleAwareService doing work (status: " + status + ")");
    }
    
    // 4. @PreDestroy
    @PreDestroy
    public void cleanup() {
        System.out.println("   🧹 @PreDestroy called");
        this.status = "PRE_DESTROY_COMPLETE";
    }
    
    // 5. DisposableBean.destroy()
    @Override
    public void destroy() {
        System.out.println("   💀 destroy() called");
        this.status = "DESTROYED";
    }
}

// Singleton scope service
class SingletonService {
    private static final AtomicInteger instanceCount = new AtomicInteger(0);
    private final int instanceNumber;
    
    public SingletonService() {
        this.instanceNumber = instanceCount.incrementAndGet();
        System.out.println("   🔵 SingletonService instance " + instanceNumber + " created");
    }
    
    public void performAction() {
        System.out.println("   SingletonService instance " + instanceNumber + " performing action");
    }
    
    public static int getInstanceCount() {
        return instanceCount.get();
    }
}

// Prototype scope service
class PrototypeService {
    private static final AtomicInteger instanceCount = new AtomicInteger(0);
    private final int instanceNumber;
    
    public PrototypeService() {
        this.instanceNumber = instanceCount.incrementAndGet();
        System.out.println("   🔴 PrototypeService instance " + instanceNumber + " created");
    }
    
    public void performAction() {
        System.out.println("   PrototypeService instance " + instanceNumber + " performing action");
    }
    
    public static int getInstanceCount() {
        return instanceCount.get();
    }
}

// Lazy initialization service
class LazyService {
    
    public LazyService() {
        System.out.println("   💤 LazyService created (lazy initialization)");
    }
    
    public void performAction() {
        System.out.println("   LazyService performing action");
    }
}

// Configuration service (must be created first)
class ConfigurationService {
    
    public ConfigurationService() {
        System.out.println("   ⚙️ ConfigurationService created (dependency)");
        // Simulate configuration loading
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public String getConfigValue(String key) {
        return "config-value-for-" + key;
    }
}

// Service that depends on ConfigurationService
class DependentService {
    
    @Autowired
    private ConfigurationService configurationService;
    
    public DependentService() {
        System.out.println("   🔗 DependentService created (depends on ConfigurationService)");
    }
    
    public void execute() {
        String config = configurationService.getConfigValue("database.url");
        System.out.println("   DependentService using config: " + config);
    }
}
```

---

## 🏗️ Part 4: Complete Spring Application Example (30 minutes)

### **Building a Task Management System**

Let's create a complete Spring application that demonstrates all the concepts we've learned.

Create `TaskManagementSystem.java`:

```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Complete Spring-based Task Management System
 * Demonstrates enterprise application structure with Spring DI
 */
@Configuration
@ComponentScan(basePackages = "com.taskmanagement")
@PropertySource("classpath:task-management.properties")
public class TaskManagementSystem {
    
    public static void main(String[] args) {
        System.out.println("=== Spring Task Management System ===\n");
        
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext(TaskManagementSystem.class);
        
        // Get main application service
        TaskManagementApplication app = context.getBean(TaskManagementApplication.class);
        
        // Run the application
        app.run();
        
        context.close();
    }
    
    // Configuration beans
    @Bean
    public TaskValidator taskValidator() {
        return new TaskValidator();
    }
    
    @Bean
    public NotificationService emailNotificationService() {
        return new EmailNotificationService();
    }
    
    @Bean
    public NotificationService smsNotificationService() {
        return new SmsNotificationService();
    }
    
    @Bean
    public AuditService auditService() {
        return new AuditService();
    }
    
    // Property placeholder configuration
    @Bean
    public static org.springframework.context.support.PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        org.springframework.context.support.PropertySourcesPlaceholderConfigurer configurer = 
            new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();
        
        Properties properties = new Properties();
        properties.setProperty("app.name", "Task Management System");
        properties.setProperty("app.version", "2.0.0");
        properties.setProperty("notification.email.enabled", "true");
        properties.setProperty("notification.sms.enabled", "false");
        properties.setProperty("audit.enabled", "true");
        properties.setProperty("task.max.per.user", "50");
        properties.setProperty("task.default.priority", "MEDIUM");
        
        configurer.setProperties(properties);
        return configurer;
    }
}

// Main application class
@Component
class TaskManagementApplication {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ReportService reportService;
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String appVersion;
    
    @PostConstruct
    public void init() {
        System.out.println("🚀 " + appName + " v" + appVersion + " initialized successfully");
    }
    
    public void run() {
        System.out.println("\n=== Application Demo ===\n");
        
        // Create users
        User john = userService.createUser("John Doe", "john@example.com");
        User jane = userService.createUser("Jane Smith", "jane@example.com");
        
        // Create tasks
        Task task1 = taskService.createTask("Implement login feature", "Create user authentication system", 
                                          TaskPriority.HIGH, john.getId());
        Task task2 = taskService.createTask("Write documentation", "Document API endpoints", 
                                          TaskPriority.MEDIUM, jane.getId());
        Task task3 = taskService.createTask("Fix bug #123", "Resolve null pointer exception", 
                                          TaskPriority.HIGH, john.getId());
        
        // Update task status
        taskService.updateTaskStatus(task1.getId(), TaskStatus.IN_PROGRESS);
        taskService.updateTaskStatus(task2.getId(), TaskStatus.COMPLETED);
        
        // Assign task to different user
        taskService.assignTask(task3.getId(), jane.getId());
        
        // Generate reports
        reportService.generateUserReport(john.getId());
        reportService.generateSystemReport();
        
        System.out.println("\n✅ Demo completed successfully!");
    }
}

// Service Layer
@Service
class TaskService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final TaskValidator taskValidator;
    
    @Value("${task.max.per.user:10}")
    private int maxTasksPerUser;
    
    @Value("${task.default.priority:MEDIUM}")
    private TaskPriority defaultPriority;
    
    public TaskService(TaskRepository taskRepository, 
                      UserRepository userRepository,
                      @Qualifier("emailNotificationService") NotificationService notificationService,
                      AuditService auditService,
                      TaskValidator taskValidator) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.taskValidator = taskValidator;
        System.out.println("✅ TaskService initialized");
    }
    
    public Task createTask(String title, String description, TaskPriority priority, Long assigneeId) {
        System.out.println("Creating task: " + title);
        
        // Validate task
        taskValidator.validateTask(title, description);
        
        // Check user task limit
        List<Task> userTasks = taskRepository.findByAssigneeId(assigneeId);
        if (userTasks.size() >= maxTasksPerUser) {
            throw new IllegalStateException("User has reached maximum task limit: " + maxTasksPerUser);
        }
        
        // Create task
        Task task = new Task(title, description, priority != null ? priority : defaultPriority, assigneeId);
        taskRepository.save(task);
        
        // Send notification
        User assignee = userRepository.findById(assigneeId);
        notificationService.sendNotification("New task assigned: " + title, assignee.getEmail());
        
        // Audit log
        auditService.logAction("TASK_CREATED", "Task created: " + title + " (ID: " + task.getId() + ")");
        
        System.out.println("✅ Task created successfully: " + task.getId());
        return task;
    }
    
    public void updateTaskStatus(Long taskId, TaskStatus status) {
        System.out.println("Updating task status: " + taskId + " -> " + status);
        
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        
        // Send notification on completion
        if (status == TaskStatus.COMPLETED) {
            User assignee = userRepository.findById(task.getAssigneeId());
            notificationService.sendNotification("Task completed: " + task.getTitle(), assignee.getEmail());
        }
        
        // Audit log
        auditService.logAction("TASK_STATUS_UPDATED", 
                             "Task " + taskId + " status changed from " + oldStatus + " to " + status);
        
        System.out.println("✅ Task status updated successfully");
    }
    
    public void assignTask(Long taskId, Long newAssigneeId) {
        System.out.println("Reassigning task: " + taskId + " to user: " + newAssigneeId);
        
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        Long oldAssigneeId = task.getAssigneeId();
        task.setAssigneeId(newAssigneeId);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        
        // Send notifications
        User newAssignee = userRepository.findById(newAssigneeId);
        notificationService.sendNotification("Task assigned to you: " + task.getTitle(), newAssignee.getEmail());
        
        // Audit log
        auditService.logAction("TASK_REASSIGNED", 
                             "Task " + taskId + " reassigned from user " + oldAssigneeId + " to " + newAssigneeId);
        
        System.out.println("✅ Task reassigned successfully");
    }
    
    public List<Task> getTasksByUser(Long userId) {
        return taskRepository.findByAssigneeId(userId);
    }
    
    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }
}

@Service
class UserService {
    
    private final UserRepository userRepository;
    private final AuditService auditService;
    
    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        System.out.println("✅ UserService initialized");
    }
    
    public User createUser(String name, String email) {
        System.out.println("Creating user: " + name + " (" + email + ")");
        
        User user = new User(name, email);
        userRepository.save(user);
        
        auditService.logAction("USER_CREATED", "User created: " + name + " (ID: " + user.getId() + ")");
        
        System.out.println("✅ User created successfully: " + user.getId());
        return user;
    }
    
    public User findUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}

@Service
class ReportService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    public ReportService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        System.out.println("✅ ReportService initialized");
    }
    
    public void generateUserReport(Long userId) {
        System.out.println("\n📊 User Report for ID: " + userId);
        
        User user = userRepository.findById(userId);
        if (user == null) {
            System.out.println("User not found");
            return;
        }
        
        List<Task> userTasks = taskRepository.findByAssigneeId(userId);
        
        System.out.println("   User: " + user.getName() + " (" + user.getEmail() + ")");
        System.out.println("   Total tasks: " + userTasks.size());
        
        Map<TaskStatus, Long> tasksByStatus = userTasks.stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        
        System.out.println("   Tasks by status:");
        tasksByStatus.forEach((status, count) -> 
            System.out.println("     " + status + ": " + count));
        
        Map<TaskPriority, Long> tasksByPriority = userTasks.stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        
        System.out.println("   Tasks by priority:");
        tasksByPriority.forEach((priority, count) -> 
            System.out.println("     " + priority + ": " + count));
    }
    
    public void generateSystemReport() {
        System.out.println("\n📈 System Report");
        
        List<Task> allTasks = taskRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        
        System.out.println("   Total users: " + allUsers.size());
        System.out.println("   Total tasks: " + allTasks.size());
        
        if (!allTasks.isEmpty()) {
            Map<TaskStatus, Long> tasksByStatus = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
            
            System.out.println("   System tasks by status:");
            tasksByStatus.forEach((status, count) -> 
                System.out.println("     " + status + ": " + count));
        }
    }
}

// Repository Layer
@Repository
class TaskRepository {
    
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public TaskRepository() {
        System.out.println("✅ TaskRepository initialized");
    }
    
    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(idGenerator.getAndIncrement());
        }
        tasks.put(task.getId(), task);
        return task;
    }
    
    public Task findById(Long id) {
        return tasks.get(id);
    }
    
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }
    
    public List<Task> findByAssigneeId(Long assigneeId) {
        return tasks.values().stream()
            .filter(task -> Objects.equals(task.getAssigneeId(), assigneeId))
            .collect(Collectors.toList());
    }
    
    public List<Task> findByStatus(TaskStatus status) {
        return tasks.values().stream()
            .filter(task -> task.getStatus() == status)
            .collect(Collectors.toList());
    }
}

@Repository
class UserRepository {
    
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserRepository() {
        System.out.println("✅ UserRepository initialized");
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        users.put(user.getId(), user);
        return user;
    }
    
    public User findById(Long id) {
        return users.get(id);
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}

// Support Components
@Component
class TaskValidator {
    
    public void validateTask(String title, String description) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        
        if (title.length() > 100) {
            throw new IllegalArgumentException("Task title cannot exceed 100 characters");
        }
        
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("Task description cannot exceed 1000 characters");
        }
    }
}

interface NotificationService {
    void sendNotification(String message, String recipient);
}

class EmailNotificationService implements NotificationService {
    
    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;
    
    @Override
    public void sendNotification(String message, String recipient) {
        if (emailEnabled) {
            System.out.println("   📧 Email to " + recipient + ": " + message);
        }
    }
}

class SmsNotificationService implements NotificationService {
    
    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;
    
    @Override
    public void sendNotification(String message, String recipient) {
        if (smsEnabled) {
            System.out.println("   📱 SMS to " + recipient + ": " + message);
        }
    }
}

class AuditService {
    
    @Value("${audit.enabled:true}")
    private boolean auditEnabled;
    
    public void logAction(String action, String details) {
        if (auditEnabled) {
            System.out.println("   📋 AUDIT: " + action + " - " + details + " at " + LocalDateTime.now());
        }
    }
}

// Domain Models
class Task {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Task(String title, String description, TaskPriority priority, Long assigneeId) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assigneeId = assigneeId;
        this.status = TaskStatus.TODO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

class User {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    
    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

// Enums
enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED, CANCELLED
}

enum TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}
```

---

## 🎯 Day 17 Summary

**Concepts Mastered:**
- ✅ **Spring Framework Architecture**: IoC container, DI, and core principles
- ✅ **Dependency Injection**: Constructor, setter, and field injection patterns
- ✅ **Configuration Patterns**: Annotation-based, Java-based, and property injection
- ✅ **Bean Lifecycle**: Initialization, destruction, and lifecycle hooks
- ✅ **Bean Scopes**: Singleton, prototype, and lazy initialization
- ✅ **Enterprise Application Structure**: Service, repository, and component layers

**Key Takeaways:**
1. **Dependency Injection** eliminates tight coupling and improves testability
2. **IoC Container** manages object creation and lifecycle automatically
3. **Spring Configuration** provides flexible ways to wire dependencies
4. **Bean Scopes** control object creation and sharing strategies
5. **Component Scanning** automatically discovers and registers beans
6. **Property Injection** enables external configuration management

**Enterprise Benefits:**
- **Loose Coupling**: Easy to swap implementations
- **Testability**: Dependencies can be mocked for testing
- **Maintainability**: Clear separation of concerns
- **Scalability**: Container manages resources efficiently
- **Configuration Management**: External properties and profiles

**Next Steps:**
- Day 18: Spring Boot Fundamentals with auto-configuration
- Advanced Spring features: AOP, transactions, and security
- Spring testing strategies and best practices
- Production deployment and monitoring

---

**🎉 Congratulations! You've mastered Spring Framework fundamentals and built a complete enterprise application!**