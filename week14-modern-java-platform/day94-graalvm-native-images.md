# Day 94: GraalVM & Native Images - Ahead-of-Time Compilation

## Learning Objectives
- Understand GraalVM architecture and benefits
- Master native image compilation techniques
- Implement GraalVM-optimized applications
- Compare JIT vs AOT compilation performance
- Build production-ready native applications

## 1. GraalVM Fundamentals

### Understanding GraalVM

GraalVM is a high-performance runtime that provides significant improvements in application performance and efficiency through ahead-of-time (AOT) compilation.

```java
package com.javajourney.graalvm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// GraalVM-optimized Spring Boot application
@SpringBootApplication
@RestController
public class GraalVMDemoApplication {
    
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        // Configure for native image
        System.setProperty("spring.aot.enabled", "true");
        System.setProperty("spring.native.enabled", "true");
        
        SpringApplication.run(GraalVMDemoApplication.class, args);
    }
    
    @PostConstruct
    public void init() {
        System.out.println("Application started at: " + LocalDateTime.now());
        System.out.println("Runtime: " + System.getProperty("java.vm.name"));
        System.out.println("Version: " + System.getProperty("java.version"));
        
        // Pre-populate cache for better native performance
        cache.put("default", "Welcome to GraalVM Demo");
        cache.put("status", "Application is running");
    }
    
    @GetMapping("/")
    public String home() {
        return cache.get("default");
    }
    
    @GetMapping("/status")
    public String status() {
        return String.format("%s - Current time: %s", 
            cache.get("status"), LocalDateTime.now());
    }
    
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "runtime", System.getProperty("java.vm.name"),
            "version", System.getProperty("java.version"),
            "processors", Runtime.getRuntime().availableProcessors(),
            "memory", Runtime.getRuntime().maxMemory(),
            "timestamp", LocalDateTime.now()
        );
    }
    
    @GetMapping("/compute/{n}")
    public Map<String, Object> compute(@PathVariable int n) {
        long startTime = System.nanoTime();
        
        // CPU-intensive computation
        long result = fibonacci(n);
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        return Map.of(
            "input", n,
            "result", result,
            "computationTime", duration + "ms",
            "runtime", System.getProperty("java.vm.name")
        );
    }
    
    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
```

### Native Image Configuration

```java
package com.javajourney.graalvm.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

// Native image runtime hints configuration
@Configuration
@ImportRuntimeHints(NativeConfiguration.class)
public class NativeConfiguration implements RuntimeHintsRegistrar {
    
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register reflection hints
        hints.reflection()
            .registerType(User.class, hint -> hint
                .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                           org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
                           org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
            .registerType(UserService.class, hint -> hint
                .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS));
        
        // Register resource hints
        hints.resources()
            .registerPattern("application*.yml")
            .registerPattern("application*.yaml")
            .registerPattern("application*.properties")
            .registerPattern("META-INF/spring.factories")
            .registerPattern("static/**")
            .registerPattern("templates/**");
        
        // Register serialization hints
        hints.serialization()
            .registerType(User.class)
            .registerType(ApiResponse.class);
        
        // Register JNI hints if needed
        hints.jni()
            .registerType(DatabaseConnection.class, hint -> hint
                .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS));
    }
}

// Data classes for native image
record User(String id, String name, String email) {}

record ApiResponse<T>(T data, String status, long timestamp) {}

// Mock classes for demonstration
class UserService {
    public User findById(String id) {
        return new User(id, "Sample User", "user@example.com");
    }
}

class DatabaseConnection {
    public native void connect();
    public native void disconnect();
}
```

### Build Configuration for Native Images

```xml
<!-- pom.xml configuration for GraalVM native image -->
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <spring-boot.version>3.2.0</spring-boot.version>
    <graalvm.version>23.1.0</graalvm.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- GraalVM dependencies -->
    <dependency>
        <groupId>org.graalvm.sdk</groupId>
        <artifactId>graal-sdk</artifactId>
        <version>${graalvm.version}</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.experimental</groupId>
        <artifactId>spring-native</artifactId>
        <version>0.12.1</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>${spring-boot.version}</version>
            <configuration>
                <image>
                    <builder>paketobuildpacks/builder:tiny</builder>
                    <env>
                        <BP_NATIVE_IMAGE>true</BP_NATIVE_IMAGE>
                        <BP_JVM_VERSION>21</BP_JVM_VERSION>
                    </env>
                </image>
            </configuration>
        </plugin>
        
        <!-- Native Build Tools Plugin -->
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
            <version>0.9.28</version>
            <extensions>true</extensions>
            <executions>
                <execution>
                    <id>test-native</id>
                    <phase>test</phase>
                    <goals>
                        <goal>test</goal>
                    </goals>
                </execution>
                <execution>
                    <id>build-native</id>
                    <phase>package</phase>
                    <goals>
                        <goal>build</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <buildArgs>
                    <buildArg>--verbose</buildArg>
                    <buildArg>--no-fallback</buildArg>
                    <buildArg>--enable-http</buildArg>
                    <buildArg>--enable-https</buildArg>
                    <buildArg>--initialize-at-build-time=ch.qos.logback</buildArg>
                    <buildArg>--initialize-at-run-time=io.netty</buildArg>
                    <buildArg>-H:+ReportExceptionStackTraces</buildArg>
                    <buildArg>-H:+AddAllCharsets</buildArg>
                    <buildArg>-H:IncludeResources=application.*\.yml|application.*\.yaml|application.*\.properties</buildArg>
                </buildArgs>
            </configuration>
        </plugin>
    </plugins>
</build>

<profiles>
    <!-- Native profile for building native images -->
    <profile>
        <id>native</id>
        <properties>
            <repackage.classifier>exec</repackage.classifier>
            <native-buildtools.version>0.9.28</native-buildtools.version>
        </properties>
        <dependencies>
            <dependency>
                <groupId>org.junit.platform</groupId>
                <artifactId>junit-platform-launcher</artifactId>
                <scope>test</scope>
            </dependency>
        </dependencies>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.graalvm.buildtools</groupId>
                    <artifactId>native-maven-plugin</artifactId>
                    <version>${native-buildtools.version}</version>
                    <extensions>true</extensions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

## 2. Advanced Native Image Features

### Reflection and Dynamic Features

```java
package com.javajourney.graalvm.advanced;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aot.hint.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

// Advanced configuration for reflection-heavy scenarios
@Configuration
@ImportRuntimeHints(AdvancedNativeConfiguration.ReflectionHints.class)
public class AdvancedNativeConfiguration {
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public DynamicProcessor dynamicProcessor() {
        return new DynamicProcessor();
    }
    
    // Runtime hints for complex reflection scenarios
    static class ReflectionHints implements RuntimeHintsRegistrar {
        
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register all data classes
            registerDataClasses(hints);
            
            // Register service classes
            registerServiceClasses(hints);
            
            // Register configuration properties
            registerConfigurationProperties(hints);
            
            // Register JSON processing
            registerJsonProcessing(hints);
            
            // Register proxy interfaces
            registerProxyInterfaces(hints);
        }
        
        private void registerDataClasses(RuntimeHints hints) {
            hints.reflection()
                .registerType(TypeReference.of(Customer.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               MemberCategory.INVOKE_DECLARED_METHODS,
                               MemberCategory.DECLARED_FIELDS))
                .registerType(TypeReference.of(Product.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               MemberCategory.INVOKE_DECLARED_METHODS,
                               MemberCategory.DECLARED_FIELDS))
                .registerType(TypeReference.of(Order.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               MemberCategory.INVOKE_DECLARED_METHODS,
                               MemberCategory.DECLARED_FIELDS));
        }
        
        private void registerServiceClasses(RuntimeHints hints) {
            hints.reflection()
                .registerType(TypeReference.of(CustomerService.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(TypeReference.of(ProductService.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(TypeReference.of(OrderService.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
        }
        
        private void registerConfigurationProperties(RuntimeHints hints) {
            hints.reflection()
                .registerType(TypeReference.of(ApplicationProperties.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               MemberCategory.INVOKE_DECLARED_METHODS,
                               MemberCategory.DECLARED_FIELDS));
        }
        
        private void registerJsonProcessing(RuntimeHints hints) {
            // Jackson ObjectMapper requirements
            hints.reflection()
                .registerType(TypeReference.of(ObjectMapper.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(TypeReference.of(com.fasterxml.jackson.databind.JsonNode.class), hint -> hint
                    .withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
            
            // Serialization support
            hints.serialization()
                .registerType(Customer.class)
                .registerType(Product.class)
                .registerType(Order.class);
        }
        
        private void registerProxyInterfaces(RuntimeHints hints) {
            hints.proxies()
                .registerJdkProxy(CustomerRepository.class)
                .registerJdkProxy(ProductRepository.class)
                .registerJdkProxy(OrderRepository.class);
        }
    }
}

// Configuration properties for native image
@ConfigurationProperties(prefix = "app")
record ApplicationProperties(
    String name,
    String version,
    DatabaseConfig database,
    CacheConfig cache
) {
    record DatabaseConfig(String url, String username, int maxConnections) {}
    record CacheConfig(boolean enabled, int ttl, int maxSize) {}
}

// Dynamic processing with reflection
class DynamicProcessor {
    
    public <T> T processObject(T object, Map<String, Object> updates) {
        try {
            Class<?> clazz = object.getClass();
            
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                // Find setter method
                String setterName = "set" + capitalize(fieldName);
                Method setter = findMethod(clazz, setterName, value.getClass());
                
                if (setter != null) {
                    setter.invoke(object, value);
                }
            }
            
            return object;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process object dynamically", e);
        }
    }
    
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private Method findMethod(Class<?> clazz, String methodName, Class<?> paramType) {
        try {
            return clazz.getMethod(methodName, paramType);
        } catch (NoSuchMethodException e) {
            // Try with primitive types
            if (paramType == Integer.class) {
                try {
                    return clazz.getMethod(methodName, int.class);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }
            return null;
        }
    }
}

// Data classes
record Customer(String id, String name, String email, String address) {}
record Product(String id, String name, String description, double price) {}
record Order(String id, String customerId, List<String> productIds, double total) {}

// Repository interfaces for proxy registration
interface CustomerRepository {
    Customer findById(String id);
    List<Customer> findAll();
    Customer save(Customer customer);
}

interface ProductRepository {
    Product findById(String id);
    List<Product> findAll();
    Product save(Product product);
}

interface OrderRepository {
    Order findById(String id);
    List<Order> findByCustomerId(String customerId);
    Order save(Order order);
}

// Service classes
class CustomerService {
    private final CustomerRepository repository;
    
    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }
    
    public Customer getCustomer(String id) {
        return repository.findById(id);
    }
}

class ProductService {
    private final ProductRepository repository;
    
    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }
    
    public Product getProduct(String id) {
        return repository.findById(id);
    }
}

class OrderService {
    private final OrderRepository repository;
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
    
    public Order getOrder(String id) {
        return repository.findById(id);
    }
}
```

### Resource and JNI Configuration

```java
package com.javajourney.graalvm.resources;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// Resource management for native images
public class ResourceConfiguration implements RuntimeHintsRegistrar {
    
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register static resources
        hints.resources()
            .registerPattern("static/**")
            .registerPattern("templates/**")
            .registerPattern("META-INF/**")
            .registerPattern("config/**")
            .registerPattern("data/**")
            .registerPattern("*.properties")
            .registerPattern("*.yml")
            .registerPattern("*.yaml")
            .registerPattern("*.json")
            .registerPattern("*.xml");
        
        // Register bundles for internationalization
        hints.resources()
            .registerResourceBundle("messages")
            .registerResourceBundle("errors")
            .registerResourceBundle("validation");
        
        // Register patterns for specific libraries
        registerLibraryResources(hints);
    }
    
    private void registerLibraryResources(RuntimeHints hints) {
        // Spring Boot resources
        hints.resources()
            .registerPattern("org/springframework/**")
            .registerPattern("META-INF/spring.factories")
            .registerPattern("META-INF/spring-configuration-metadata.json");
        
        // Jackson resources
        hints.resources()
            .registerPattern("com/fasterxml/jackson/**");
        
        // Logging resources
        hints.resources()
            .registerPattern("ch/qos/logback/**")
            .registerPattern("org/slf4j/**");
    }
}

// Resource loading utilities
class ResourceLoader {
    
    private final PathMatchingResourcePatternResolver resolver;
    
    public ResourceLoader() {
        this.resolver = new PathMatchingResourcePatternResolver();
    }
    
    public String loadResourceAsString(String path) {
        try {
            Resource resource = resolver.getResource("classpath:" + path);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + path, e);
        }
    }
    
    public Properties loadProperties(String path) {
        try {
            Resource resource = resolver.getResource("classpath:" + path);
            Properties properties = new Properties();
            try (InputStream is = resource.getInputStream()) {
                properties.load(is);
            }
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + path, e);
        }
    }
    
    public Resource[] findResources(String pattern) {
        try {
            return resolver.getResources("classpath*:" + pattern);
        } catch (IOException e) {
            throw new RuntimeException("Failed to find resources: " + pattern, e);
        }
    }
}

// JNI configuration example
class NativeLibraryIntegration {
    
    static {
        // Load native library
        try {
            System.loadLibrary("native-lib");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native library not found: " + e.getMessage());
        }
    }
    
    // Native method declarations
    public native String processData(String input);
    public native int calculateHash(byte[] data);
    public native void initializeNativeResources();
    public native void cleanupNativeResources();
    
    // Java wrapper methods
    public String safeProcessData(String input) {
        if (input == null) {
            return null;
        }
        
        try {
            return processData(input);
        } catch (UnsatisfiedLinkError e) {
            // Fallback to Java implementation
            return "Fallback: " + input.toUpperCase();
        }
    }
    
    public int safeCalculateHash(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }
        
        try {
            return calculateHash(data);
        } catch (UnsatisfiedLinkError e) {
            // Fallback to Java implementation
            return java.util.Arrays.hashCode(data);
        }
    }
}
```

## 3. Performance Optimization

### Startup Time and Memory Optimization

```java
package com.javajourney.graalvm.performance;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

// Performance monitoring for native images
@Component
public class PerformanceMonitor implements ApplicationListener<ApplicationReadyEvent> {
    
    private final Instant startTime = Instant.now();
    private final AtomicLong requestCount = new AtomicLong(0);
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Duration startupTime = Duration.between(startTime, Instant.now());
        
        System.out.println("=== GraalVM Performance Metrics ===");
        System.out.println("Startup time: " + startupTime.toMillis() + "ms");
        System.out.println("Runtime: " + runtimeBean.getVmName());
        System.out.println("Version: " + runtimeBean.getVmVersion());
        System.out.println("Uptime: " + runtimeBean.getUptime() + "ms");
        
        printMemoryUsage();
        
        // Schedule periodic monitoring
        schedulePeriodicMonitoring();
    }
    
    public void recordRequest() {
        requestCount.incrementAndGet();
    }
    
    public PerformanceMetrics getCurrentMetrics() {
        var memory = memoryBean.getHeapMemoryUsage();
        
        return new PerformanceMetrics(
            Duration.between(startTime, Instant.now()),
            requestCount.get(),
            memory.getUsed(),
            memory.getMax(),
            memory.getCommitted(),
            runtimeBean.getUptime()
        );
    }
    
    private void printMemoryUsage() {
        var heapMemory = memoryBean.getHeapMemoryUsage();
        var nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + formatBytes(heapMemory.getUsed()));
        System.out.println("  Committed: " + formatBytes(heapMemory.getCommitted()));
        System.out.println("  Max: " + formatBytes(heapMemory.getMax()));
        
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + formatBytes(nonHeapMemory.getUsed()));
        System.out.println("  Committed: " + formatBytes(nonHeapMemory.getCommitted()));
        System.out.println("  Max: " + formatBytes(nonHeapMemory.getMax()));
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void schedulePeriodicMonitoring() {
        // Simple monitoring - in production, use proper scheduling
        Thread monitoringThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(30000); // 30 seconds
                    
                    var metrics = getCurrentMetrics();
                    System.out.printf("Performance Update - Uptime: %ds, Requests: %d, Memory: %s%n",
                        metrics.uptime().toSeconds(),
                        metrics.requestCount(),
                        formatBytes(metrics.memoryUsed())
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        monitoringThread.setDaemon(true);
        monitoringThread.setName("performance-monitor");
        monitoringThread.start();
    }
    
    @PreDestroy
    public void shutdown() {
        Duration totalUptime = Duration.between(startTime, Instant.now());
        System.out.println("=== Shutdown Metrics ===");
        System.out.println("Total uptime: " + totalUptime.toSeconds() + "s");
        System.out.println("Total requests: " + requestCount.get());
        System.out.println("Requests per second: " + 
            (requestCount.get() / Math.max(1, totalUptime.toSeconds())));
    }
    
    public record PerformanceMetrics(
        Duration uptime,
        long requestCount,
        long memoryUsed,
        long memoryMax,
        long memoryCommitted,
        long vmUptime
    ) {}
}

// Optimized service layer for native images
@Component
class OptimizedBusinessService {
    
    private final PerformanceMonitor performanceMonitor;
    
    // Pre-computed values for better native performance
    private static final Map<String, String> RESPONSE_TEMPLATES = Map.of(
        "success", "Operation completed successfully",
        "error", "An error occurred",
        "warning", "Warning: please check your input",
        "info", "Information message"
    );
    
    public OptimizedBusinessService(PerformanceMonitor performanceMonitor) {
        this.performanceMonitor = performanceMonitor;
    }
    
    public BusinessResult processRequest(BusinessRequest request) {
        performanceMonitor.recordRequest();
        
        long startTime = System.nanoTime();
        
        try {
            // Optimized processing logic
            String result = processBusinessLogic(request);
            
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            
            return new BusinessResult(
                result,
                "success",
                duration,
                System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000;
            
            return new BusinessResult(
                RESPONSE_TEMPLATES.get("error"),
                "error",
                duration,
                System.currentTimeMillis()
            );
        }
    }
    
    private String processBusinessLogic(BusinessRequest request) {
        // Simulate business logic
        if (request.data() == null || request.data().isEmpty()) {
            return RESPONSE_TEMPLATES.get("warning");
        }
        
        // Use pre-computed templates for better performance
        return RESPONSE_TEMPLATES.get("success") + ": " + request.data().toUpperCase();
    }
    
    public record BusinessRequest(String id, String data, Map<String, String> metadata) {}
    public record BusinessResult(String result, String status, long processingTime, long timestamp) {}
}
```

### Build-Time Optimization

```java
package com.javajourney.graalvm.optimization;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Build-time optimizations
@Configuration
@ImportRuntimeHints(BuildTimeOptimization.OptimizationHints.class)
public class BuildTimeOptimization {
    
    // Pre-initialize commonly used objects
    @Bean
    public DataCache dataCache() {
        DataCache cache = new DataCache();
        
        // Pre-populate cache with common data
        cache.put("default-message", "Welcome to the application");
        cache.put("error-message", "An error occurred");
        cache.put("success-message", "Operation completed successfully");
        
        return cache;
    }
    
    @Bean
    public ConfigurationManager configurationManager() {
        return new ConfigurationManager();
    }
    
    // Runtime hints for optimization
    static class OptimizationHints implements RuntimeHintsRegistrar {
        
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Optimize reflection access
            hints.reflection()
                .registerType(DataCache.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(ConfigurationManager.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS));
            
            // Pre-register commonly used classes
            hints.reflection()
                .registerType(String.class)
                .registerType(Map.class)
                .registerType(ConcurrentHashMap.class);
        }
    }
}

// Optimized cache implementation
class DataCache {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    public void put(String key, Object value) {
        cache.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = cache.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    public String getString(String key) {
        return get(key, String.class);
    }
    
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }
    
    public void clear() {
        cache.clear();
    }
    
    public int size() {
        return cache.size();
    }
}

// Configuration management optimized for native images
class ConfigurationManager {
    
    private final Map<String, String> config = new ConcurrentHashMap<>();
    
    public ConfigurationManager() {
        // Initialize with default configuration
        loadDefaultConfiguration();
    }
    
    private void loadDefaultConfiguration() {
        config.put("app.name", "GraalVM Demo");
        config.put("app.version", "1.0.0");
        config.put("app.environment", "production");
        config.put("database.pool.size", "10");
        config.put("cache.enabled", "true");
        config.put("cache.ttl", "3600");
    }
    
    public String getString(String key) {
        return config.get(key);
    }
    
    public String getString(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        String value = config.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = config.get(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    public void setProperty(String key, String value) {
        config.put(key, value);
    }
    
    public Map<String, String> getAllProperties() {
        return Map.copyOf(config);
    }
}
```

## 4. Testing Native Images

### Native Test Configuration

```java
package com.javajourney.graalvm.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

// Native tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.aot.enabled=true",
    "spring.native.enabled=true"
})
class NativeImageIntegrationTest {
    
    @Test
    void contextLoads() {
        // Test that the application context loads successfully in native mode
        assertTrue(true, "Application context should load");
    }
    
    @Test
    void testNativePerformance() {
        long startTime = System.nanoTime();
        
        // Perform some computation
        int result = fibonacci(30);
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        assertEquals(832040, result);
        assertTrue(duration < 1000, "Native computation should be fast");
        
        System.out.println("Native fibonacci(30) took: " + duration + "ms");
    }
    
    @Test
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create some objects
        for (int i = 0; i < 1000; i++) {
            String data = "Test data " + i;
            processData(data);
        }
        
        // Force garbage collection
        System.gc();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("Memory increase: " + memoryIncrease + " bytes");
        assertTrue(memoryIncrease < 10_000_000, "Memory usage should be reasonable");
    }
    
    private int fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
    
    private String processData(String data) {
        return data.toUpperCase().replace(" ", "_");
    }
}

// Native-specific test utilities
class NativeTestUtils {
    
    public static void printRuntimeInfo() {
        System.out.println("=== Runtime Information ===");
        System.out.println("VM Name: " + System.getProperty("java.vm.name"));
        System.out.println("VM Version: " + System.getProperty("java.vm.version"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        
        Runtime runtime = Runtime.getRuntime();
        System.out.println("Available Processors: " + runtime.availableProcessors());
        System.out.println("Max Memory: " + formatBytes(runtime.maxMemory()));
        System.out.println("Total Memory: " + formatBytes(runtime.totalMemory()));
        System.out.println("Free Memory: " + formatBytes(runtime.freeMemory()));
    }
    
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    public static void performanceTest(String testName, Runnable test) {
        System.out.println("Starting performance test: " + testName);
        long startTime = System.nanoTime();
        
        test.run();
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        System.out.println("Test '" + testName + "' completed in: " + duration + "ms");
    }
}
```

### Build Scripts and Docker Configuration

```bash
#!/bin/bash
# build-native.sh - Script to build GraalVM native image

set -e

echo "Building GraalVM Native Image..."

# Clean previous builds
./mvnw clean

# Build native image
./mvnw -Pnative native:compile

echo "Native image build completed!"

# Display build information
if [ -f "target/graalvm-demo" ]; then
    echo "Native executable created: target/graalvm-demo"
    echo "File size: $(du -h target/graalvm-demo | cut -f1)"
    echo "Executable permissions: $(ls -la target/graalvm-demo)"
fi

# Test the native image
echo "Testing native image..."
./target/graalvm-demo --version || echo "Version command not available"

echo "Build and test completed successfully!"
```

```dockerfile
# Dockerfile for GraalVM native image
FROM ghcr.io/graalvm/graalvm-ce:ol8-java17-22.3.0 AS builder

# Install native-image
RUN gu install native-image

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build native image
RUN ./mvnw -Pnative native:compile

# Create minimal runtime image
FROM oraclelinux:8-slim

RUN microdnf install glibc-langpack-en gzip tar \
    && microdnf clean all

# Create non-root user
RUN useradd --uid 1001 --gid 0 --shell /bin/bash --create-home spring

# Copy native executable
COPY --from=builder /app/target/graalvm-demo /app/graalvm-demo

# Set ownership and permissions
RUN chown 1001:0 /app/graalvm-demo && chmod +x /app/graalvm-demo

USER 1001

EXPOSE 8080

ENTRYPOINT ["/app/graalvm-demo"]
```

## Practice Exercises

### Exercise 1: Native Image Migration

Convert an existing Spring Boot application to run as a GraalVM native image, including all necessary configuration.

### Exercise 2: Performance Comparison

Build both JVM and native versions of the same application and compare startup time, memory usage, and throughput.

### Exercise 3: Advanced Configuration

Implement a complex application with reflection, resources, and JNI, ensuring it works correctly as a native image.

### Exercise 4: Production Deployment

Create a complete CI/CD pipeline for building and deploying GraalVM native images to production.

## Key Takeaways

1. **Native Images**: Compile Java applications ahead-of-time for faster startup and lower memory usage
2. **Configuration**: Proper runtime hints are essential for reflection, resources, and dynamic features
3. **Performance**: Native images excel in startup time and memory efficiency, especially for microservices
4. **Trade-offs**: Build time increases, but runtime performance and resource efficiency improve significantly
5. **Ecosystem**: Growing support from frameworks like Spring Boot, Quarkus, and Micronaut

GraalVM native images represent a significant advancement in Java deployment, enabling Java applications to compete with native languages in startup time and memory efficiency while maintaining the rich Java ecosystem.