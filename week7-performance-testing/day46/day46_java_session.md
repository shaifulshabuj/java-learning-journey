# Day 46: Integration Testing & Test Containers (August 24, 2025)

**Date:** August 24, 2025  
**Duration:** 3 hours (Extended Session)  
**Focus:** Master integration testing with TestContainers, database testing, and microservice integration

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Master TestContainers for Docker-based integration testing
- ✅ Implement comprehensive database integration testing strategies
- ✅ Build microservice integration test suites with performance monitoring
- ✅ Create end-to-end testing pipelines with real external dependencies
- ✅ Develop service virtualization and test environment management
- ✅ Integrate performance testing with realistic production-like scenarios

---

## 📚 Part 1: TestContainers Foundation (45 minutes)

### **TestContainers Architecture**

```
┌─────────────────────────────────────────────────────────────────┐
│                    TESTCONTAINERS ECOSYSTEM                    │
├─────────────────────────────────────────────────────────────────┤
│ Core TestContainers                                            │
│ ├─ Generic Container                                           │
│ ├─ Docker Compose                                             │
│ ├─ Network Management                                         │
│ └─ Lifecycle Management                                       │
├─────────────────────────────────────────────────────────────────┤
│ Specialized Modules                                            │
│ ├─ Databases: PostgreSQL, MySQL, MongoDB, Redis              │
│ ├─ Message Queues: Kafka, RabbitMQ, ActiveMQ                │
│ ├─ Search: Elasticsearch, Solr                               │
│ ├─ Web: Selenium, Nginx                                      │
│ └─ Cloud: AWS LocalStack, Azure, GCP                         │
├─────────────────────────────────────────────────────────────────┤
│ Integration Layer                                              │
│ ├─ JUnit 5 Extension                                         │
│ ├─ Spring Boot Test                                           │
│ ├─ Performance Monitoring                                     │
│ └─ Custom Extensions                                          │
└─────────────────────────────────────────────────────────────────┘
```

### **Key TestContainers Benefits**

- **Isolation**: Each test gets fresh container instances
- **Reproducibility**: Consistent environment across all machines
- **Realistic Testing**: Test against real dependencies, not mocks
- **CI/CD Integration**: Works seamlessly in Docker-enabled environments
- **Performance Testing**: Realistic performance characteristics

---

## 💻 Part 2: Database Integration Testing (50 minutes)

### **Exercise 1: Comprehensive Database Testing Framework**

Create `DatabaseIntegrationTestSuite.java`:

```java
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.*;
import java.util.*;
import java.time.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Comprehensive database integration testing with performance monitoring
 */
@Testcontainers
@DisplayName("Database Integration Test Suite")
class DatabaseIntegrationTestSuite {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init.sql");
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    private static DataSource postgresDataSource;
    private static DataSource mysqlDataSource;
    private static UserRepository userRepository;
    private static CacheService cacheService;
    
    @BeforeAll
    static void setUpContainers() {
        System.out.println("🐳 Starting database containers...");
        
        // Setup PostgreSQL connection
        postgresDataSource = createDataSource(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
        
        // Setup MySQL connection
        mysqlDataSource = createDataSource(
            mysql.getJdbcUrl(),
            mysql.getUsername(),
            mysql.getPassword()
        );
        
        // Setup Redis connection
        String redisHost = redis.getHost();
        Integer redisPort = redis.getMappedPort(6379);
        cacheService = new RedisCacheService(redisHost, redisPort);
        
        // Initialize repository
        userRepository = new UserRepository(postgresDataSource, cacheService);
        
        System.out.println("✅ All containers started successfully");
    }
    
    @BeforeEach
    void setUp() {
        // Clean up data before each test
        try (Connection conn = postgresDataSource.getConnection()) {
            conn.createStatement().execute("TRUNCATE TABLE users CASCADE");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clean up test data", e);
        }
        
        // Clear cache
        cacheService.clear();
    }
    
    /**
     * Basic database operations test
     */
    @Test
    @DisplayName("Basic Database Operations")
    void testBasicDatabaseOperations() throws SQLException {
        // Create test user
        User user = new User(null, "John Doe", "john@example.com", LocalDateTime.now());
        
        // Test insert
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());
        assertEquals("John Doe", savedUser.getName());
        
        // Test find by id
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals("john@example.com", foundUser.get().getEmail());
        
        // Test update
        User updatedUser = foundUser.get().withName("John Smith");
        User result = userRepository.save(updatedUser);
        assertEquals("John Smith", result.getName());
        
        // Test delete
        userRepository.deleteById(savedUser.getId());
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }
    
    /**
     * Performance test with large dataset
     */
    @Test
    @DisplayName("Database Performance Test")
    void testDatabasePerformance() {
        int userCount = 10000;
        
        // Measure batch insert performance
        long startTime = System.currentTimeMillis();
        
        List<User> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            users.add(new User(
                null,
                "User " + i,
                "user" + i + "@example.com",
                LocalDateTime.now()
            ));
        }
        
        List<User> savedUsers = userRepository.batchSave(users);
        long insertTime = System.currentTimeMillis() - startTime;
        
        assertEquals(userCount, savedUsers.size());
        assertTrue(insertTime < 5000, "Batch insert should complete within 5 seconds");
        
        System.out.printf("📊 Inserted %d users in %d ms (%.2f users/sec)%n",
                         userCount, insertTime, userCount * 1000.0 / insertTime);
        
        // Measure query performance
        startTime = System.currentTimeMillis();
        List<User> allUsers = userRepository.findAll();
        long queryTime = System.currentTimeMillis() - startTime;
        
        assertEquals(userCount, allUsers.size());
        assertTrue(queryTime < 2000, "Query should complete within 2 seconds");
        
        System.out.printf("📊 Queried %d users in %d ms%n", userCount, queryTime);
    }
    
    /**
     * Transaction testing
     */
    @Test
    @DisplayName("Transaction Management")
    void testTransactionManagement() {
        // Test successful transaction
        List<User> users = Arrays.asList(
            new User(null, "User 1", "user1@example.com", LocalDateTime.now()),
            new User(null, "User 2", "user2@example.com", LocalDateTime.now())
        );
        
        List<User> savedUsers = userRepository.saveInTransaction(users);
        assertEquals(2, savedUsers.size());
        
        // Verify both users are saved
        assertEquals(2, userRepository.count());
        
        // Test transaction rollback
        List<User> problematicUsers = Arrays.asList(
            new User(null, "User 3", "user3@example.com", LocalDateTime.now()),
            new User(null, "User 4", null, LocalDateTime.now()) // Invalid email
        );
        
        assertThrows(RuntimeException.class, () -> {
            userRepository.saveInTransaction(problematicUsers);
        });
        
        // Verify rollback - should still be 2 users
        assertEquals(2, userRepository.count());
    }
    
    /**
     * Cache integration testing
     */
    @Test
    @DisplayName("Cache Integration")
    void testCacheIntegration() {
        // Create and save user
        User user = new User(null, "Cached User", "cached@example.com", LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // First access - should hit database
        long startTime = System.nanoTime();
        Optional<User> firstAccess = userRepository.findByIdWithCache(savedUser.getId());
        long firstAccessTime = System.nanoTime() - startTime;
        
        assertTrue(firstAccess.isPresent());
        
        // Second access - should hit cache
        startTime = System.nanoTime();
        Optional<User> secondAccess = userRepository.findByIdWithCache(savedUser.getId());
        long secondAccessTime = System.nanoTime() - startTime;
        
        assertTrue(secondAccess.isPresent());
        assertEquals(firstAccess.get().getName(), secondAccess.get().getName());
        
        // Cache access should be significantly faster
        assertTrue(secondAccessTime < firstAccessTime / 2,
                  String.format("Cache access (%d ns) should be faster than DB access (%d ns)",
                               secondAccessTime, firstAccessTime));
        
        System.out.printf("📊 DB access: %d ns, Cache access: %d ns (%.1fx faster)%n",
                         firstAccessTime, secondAccessTime, (double) firstAccessTime / secondAccessTime);
    }
    
    /**
     * Concurrent access testing
     */
    @Test
    @DisplayName("Concurrent Database Access")
    void testConcurrentDatabaseAccess() throws InterruptedException {
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            User user = new User(
                                null,
                                "Thread" + threadId + "_User" + j,
                                "thread" + threadId + "_user" + j + "@example.com",
                                LocalDateTime.now()
                            );
                            
                            User saved = userRepository.save(user);
                            userRepository.findById(saved.getId());
                            
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.printf("Error in thread %d: %s%n", threadId, e.getMessage());
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
        
        int totalOperations = numThreads * operationsPerThread;
        double successRate = (double) successCount.get() / totalOperations;
        
        System.out.printf("📊 Concurrent test: %d operations in %d ms%n", totalOperations, duration);
        System.out.printf("📊 Success rate: %.1f%% (%d/%d)%n", 
                         successRate * 100, successCount.get(), totalOperations);
        System.out.printf("📊 Throughput: %.1f ops/sec%n", totalOperations * 1000.0 / duration);
        
        assertTrue(successRate > 0.95, "Success rate should be > 95%");
        assertTrue(errorCount.get() < totalOperations * 0.05, "Error rate should be < 5%");
    }
    
    /**
     * Cross-database testing
     */
    @Test
    @DisplayName("Cross-Database Compatibility")
    void testCrossDatabaseCompatibility() {
        // Test same operations on different databases
        User user = new User(null, "Cross DB User", "crossdb@example.com", LocalDateTime.now());
        
        // Test PostgreSQL
        UserRepository postgresRepo = new UserRepository(postgresDataSource, cacheService);
        User postgresUser = postgresRepo.save(user);
        assertNotNull(postgresUser.getId());
        
        // Test MySQL (setup schema first)
        setupMySQLSchema();
        UserRepository mysqlRepo = new UserRepository(mysqlDataSource, cacheService);
        User mysqlUser = mysqlRepo.save(user);
        assertNotNull(mysqlUser.getId());
        
        // Both should work identically
        assertEquals(postgresUser.getName(), mysqlUser.getName());
        assertEquals(postgresUser.getEmail(), mysqlUser.getEmail());
    }
    
    /**
     * Data integrity and constraint testing
     */
    @Test
    @DisplayName("Data Integrity and Constraints")
    void testDataIntegrityAndConstraints() {
        // Test unique constraint
        User user1 = new User(null, "User 1", "unique@example.com", LocalDateTime.now());
        User user2 = new User(null, "User 2", "unique@example.com", LocalDateTime.now());
        
        userRepository.save(user1);
        
        assertThrows(RuntimeException.class, () -> {
            userRepository.save(user2); // Should fail due to unique email constraint
        });
        
        // Test not null constraint
        User invalidUser = new User(null, null, "test@example.com", LocalDateTime.now());
        
        assertThrows(RuntimeException.class, () -> {
            userRepository.save(invalidUser); // Should fail due to not null constraint
        });
        
        // Test data consistency after constraint violations
        List<User> allUsers = userRepository.findAll();
        assertEquals(1, allUsers.size());
        assertEquals("unique@example.com", allUsers.get(0).getEmail());
    }
    
    private static DataSource createDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
    
    private void setupMySQLSchema() {
        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to setup MySQL schema", e);
        }
    }
}

/**
 * User repository with caching support
 */
class UserRepository {
    private final DataSource dataSource;
    private final CacheService cacheService;
    
    public UserRepository(DataSource dataSource, CacheService cacheService) {
        this.dataSource = dataSource;
        this.cacheService = cacheService;
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }
    
    private User insert(User user) {
        String sql = "INSERT INTO users (name, email, created_at) VALUES (?, ?, ?) RETURNING id";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    User savedUser = user.withId(id);
                    
                    // Cache the saved user
                    cacheService.put("user:" + id, savedUser);
                    
                    return savedUser;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
        
        throw new RuntimeException("Failed to get generated ID");
    }
    
    private User update(User user) {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setLong(3, user.getId());
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new RuntimeException("User not found: " + user.getId());
            }
            
            // Update cache
            cacheService.put("user:" + user.getId(), user);
            
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }
    
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, name, email, created_at FROM users WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user", e);
        }
        
        return Optional.empty();
    }
    
    public Optional<User> findByIdWithCache(Long id) {
        String cacheKey = "user:" + id;
        
        // Try cache first
        User cached = cacheService.get(cacheKey, User.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Cache miss - go to database
        Optional<User> user = findById(id);
        if (user.isPresent()) {
            cacheService.put(cacheKey, user.get());
        }
        
        return user;
    }
    
    public List<User> findAll() {
        String sql = "SELECT id, name, email, created_at FROM users ORDER BY id";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all users", e);
        }
        
        return users;
    }
    
    public List<User> batchSave(List<User> users) {
        String sql = "INSERT INTO users (name, email, created_at) VALUES (?, ?, ?)";
        List<User> savedUsers = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (User user : users) {
                    stmt.setString(1, user.getName());
                    stmt.setString(2, user.getEmail());
                    stmt.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    int index = 0;
                    while (rs.next() && index < users.size()) {
                        Long id = rs.getLong(1);
                        savedUsers.add(users.get(index).withId(id));
                        index++;
                    }
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to batch save users", e);
        }
        
        return savedUsers;
    }
    
    public List<User> saveInTransaction(List<User> users) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                List<User> savedUsers = new ArrayList<>();
                
                for (User user : users) {
                    if (user.getEmail() == null) {
                        throw new RuntimeException("Email cannot be null");
                    }
                    savedUsers.add(save(user));
                }
                
                conn.commit();
                return savedUsers;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute transaction", e);
        }
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
            
            // Remove from cache
            cacheService.remove("user:" + id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }
    
    public int count() {
        String sql = "SELECT COUNT(*) FROM users";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count users", e);
        }
        
        return 0;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}

/**
 * User domain model
 */
record User(Long id, String name, String email, LocalDateTime createdAt) {
    public User withId(Long newId) {
        return new User(newId, name, email, createdAt);
    }
    
    public User withName(String newName) {
        return new User(id, newName, email, createdAt);
    }
}

/**
 * Cache service interface
 */
interface CacheService {
    void put(String key, Object value);
    <T> T get(String key, Class<T> type);
    void remove(String key);
    void clear();
}

/**
 * Redis-based cache implementation
 */
class RedisCacheService implements CacheService {
    private final String host;
    private final int port;
    private final Map<String, Object> simpleCache; // Simplified for demo
    
    public RedisCacheService(String host, int port) {
        this.host = host;
        this.port = port;
        this.simpleCache = new ConcurrentHashMap<>();
        System.out.printf("📡 Connected to Redis at %s:%d%n", host, port);
    }
    
    @Override
    public void put(String key, Object value) {
        simpleCache.put(key, value);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = simpleCache.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    @Override
    public void remove(String key) {
        simpleCache.remove(key);
    }
    
    @Override
    public void clear() {
        simpleCache.clear();
    }
}
```

### **Exercise 2: Microservice Integration Testing**

Create `MicroserviceIntegrationTest.java`:

```java
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Microservice integration testing with performance monitoring
 */
@Testcontainers
@DisplayName("Microservice Integration Tests")
class MicroserviceIntegrationTest {
    
    private static final Network network = Network.newNetwork();
    
    @Container
    static GenericContainer<?> userService = new GenericContainer<>(DockerImageName.parse("nginx:alpine"))
            .withNetwork(network)
            .withNetworkAliases("user-service")
            .withExposedPorts(80)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .withCommand("sh", "-c", "echo 'server { listen 80; location /health { return 200 \"OK\"; } location /users { return 200 \"{\\\"users\\\":[{\\\"id\\\":1,\\\"name\\\":\\\"John\\\"}]}\"; } }' > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'");
    
    @Container
    static GenericContainer<?> orderService = new GenericContainer<>(DockerImageName.parse("nginx:alpine"))
            .withNetwork(network)
            .withNetworkAliases("order-service")
            .withExposedPorts(80)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .withCommand("sh", "-c", "echo 'server { listen 80; location /health { return 200 \"OK\"; } location /orders { return 200 \"{\\\"orders\\\":[{\\\"id\\\":1,\\\"userId\\\":1,\\\"total\\\":100.0}]}\"; } }' > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'");
    
    @Container
    static GenericContainer<?> apiGateway = new GenericContainer<>(DockerImageName.parse("nginx:alpine"))
            .withNetwork(network)
            .withExposedPorts(80)
            .dependsOn(userService, orderService)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .withCommand("sh", "-c", "echo 'upstream user-service { server user-service:80; } upstream order-service { server order-service:80; } server { listen 80; location /health { return 200 \"OK\"; } location /api/users { proxy_pass http://user-service/users; } location /api/orders { proxy_pass http://order-service/orders; } }' > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'");
    
    private static HttpClient httpClient;
    private static String gatewayBaseUrl;
    private static ObjectMapper objectMapper;
    
    @BeforeAll
    static void setUp() {
        httpClient = HttpClient.newBuilder()
                              .connectTimeout(Duration.ofSeconds(10))
                              .build();
        
        gatewayBaseUrl = String.format("http://%s:%d", 
                                     apiGateway.getHost(), 
                                     apiGateway.getMappedPort(80));
        
        objectMapper = new ObjectMapper();
        
        System.out.printf("🌐 API Gateway available at: %s%n", gatewayBaseUrl);
    }
    
    /**
     * Test service health checks
     */
    @Test
    @DisplayName("Service Health Checks")
    void testServiceHealthChecks() throws Exception {
        // Test individual services
        assertServiceHealth(userService, "User Service");
        assertServiceHealth(orderService, "Order Service");
        assertServiceHealth(apiGateway, "API Gateway");
        
        // Test gateway health
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(gatewayBaseUrl + "/health"))
                .timeout(Duration.ofSeconds(5))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
    }
    
    /**
     * Test end-to-end API calls through gateway
     */
    @Test
    @DisplayName("End-to-End API Calls")
    void testEndToEndApiCalls() throws Exception {
        // Test user service through gateway
        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create(gatewayBaseUrl + "/api/users"))
                .timeout(Duration.ofSeconds(5))
                .build();
        
        HttpResponse<String> userResponse = httpClient.send(userRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, userResponse.statusCode());
        assertTrue(userResponse.body().contains("users"));
        
        // Test order service through gateway
        HttpRequest orderRequest = HttpRequest.newBuilder()
                .uri(URI.create(gatewayBaseUrl + "/api/orders"))
                .timeout(Duration.ofSeconds(5))
                .build();
        
        HttpResponse<String> orderResponse = httpClient.send(orderRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, orderResponse.statusCode());
        assertTrue(orderResponse.body().contains("orders"));
    }
    
    /**
     * Test API Gateway performance
     */
    @Test
    @DisplayName("API Gateway Performance Test")
    void testApiGatewayPerformance() throws Exception {
        int numRequests = 100;
        int numThreads = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numRequests; i++) {
            executor.submit(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(gatewayBaseUrl + "/api/users"))
                            .timeout(Duration.ofSeconds(10))
                            .build();
                    
                    HttpResponse<String> response = httpClient.send(request,
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.printf("Request failed: %s%n", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        double successRate = (double) successCount.get() / numRequests;
        double throughput = numRequests * 1000.0 / duration;
        
        System.out.printf("📊 Performance Test Results:%n");
        System.out.printf("   Total requests: %d%n", numRequests);
        System.out.printf("   Success rate: %.1f%% (%d/%d)%n", 
                         successRate * 100, successCount.get(), numRequests);
        System.out.printf("   Error count: %d%n", errorCount.get());
        System.out.printf("   Duration: %d ms%n", duration);
        System.out.printf("   Throughput: %.1f req/sec%n", throughput);
        System.out.printf("   Average response time: %.1f ms%n", (double) duration / numRequests);
        
        assertTrue(successRate >= 0.95, "Success rate should be >= 95%");
        assertTrue(throughput >= 50, "Throughput should be >= 50 req/sec");
    }
    
    /**
     * Test service resilience and fault tolerance
     */
    @Test
    @DisplayName("Service Resilience Test")
    void testServiceResilience() throws Exception {
        // First, ensure services are working
        testEndToEndApiCalls();
        
        // Simulate service failure by stopping user service
        userService.stop();
        
        // Wait a moment for the change to propagate
        Thread.sleep(2000);
        
        // Test that order service still works
        HttpRequest orderRequest = HttpRequest.newBuilder()
                .uri(URI.create(gatewayBaseUrl + "/api/orders"))
                .timeout(Duration.ofSeconds(5))
                .build();
        
        HttpResponse<String> orderResponse = httpClient.send(orderRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, orderResponse.statusCode(), 
                    "Order service should still work when user service is down");
        
        // Test that user service requests fail gracefully
        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create(gatewayBaseUrl + "/api/users"))
                .timeout(Duration.ofSeconds(5))
                .build();
        
        HttpResponse<String> userResponse = httpClient.send(userRequest,
                HttpResponse.BodyHandlers.ofString());
        
        // Should get a gateway error (502, 503, or 504)
        assertTrue(userResponse.statusCode() >= 500,
                  "Should get server error when user service is down");
        
        // Restart user service
        userService.start();
        
        // Wait for service to be ready
        Thread.sleep(5000);
        
        // Test that user service works again
        userResponse = httpClient.send(userRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, userResponse.statusCode(),
                    "User service should work after restart");
    }
    
    /**
     * Test load balancing and scaling
     */
    @Test
    @DisplayName("Load Balancing Test")
    void testLoadBalancing() throws Exception {
        // This test simulates load balancing by making many concurrent requests
        // and verifying they're distributed properly
        
        int numRequests = 500;
        int numThreads = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numRequests);
        ConcurrentHashMap<Integer, AtomicInteger> responseTimeDistribution = new ConcurrentHashMap<>();
        AtomicInteger totalResponseTime = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numRequests; i++) {
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(gatewayBaseUrl + "/api/users"))
                            .timeout(Duration.ofSeconds(10))
                            .build();
                    
                    HttpResponse<String> response = httpClient.send(request,
                            HttpResponse.BodyHandlers.ofString());
                    
                    long requestDuration = System.currentTimeMillis() - requestStart;
                    totalResponseTime.addAndGet((int) requestDuration);
                    
                    // Group response times into buckets
                    int bucket = (int) (requestDuration / 50) * 50; // 50ms buckets
                    responseTimeDistribution.computeIfAbsent(bucket, k -> new AtomicInteger(0))
                                          .incrementAndGet();
                    
                    assertEquals(200, response.statusCode());
                } catch (Exception e) {
                    System.err.printf("Load test request failed: %s%n", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long totalDuration = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        double avgResponseTime = (double) totalResponseTime.get() / numRequests;
        double throughput = numRequests * 1000.0 / totalDuration;
        
        System.out.printf("📊 Load Balancing Test Results:%n");
        System.out.printf("   Total requests: %d%n", numRequests);
        System.out.printf("   Total duration: %d ms%n", totalDuration);
        System.out.printf("   Average response time: %.1f ms%n", avgResponseTime);
        System.out.printf("   Throughput: %.1f req/sec%n", throughput);
        
        System.out.println("\n📊 Response Time Distribution:");
        responseTimeDistribution.entrySet().stream()
                               .sorted(Map.Entry.comparingByKey())
                               .forEach(entry -> {
                                   int bucket = entry.getKey();
                                   int count = entry.getValue().get();
                                   double percentage = (double) count / numRequests * 100;
                                   System.out.printf("   %3d-%3d ms: %3d requests (%.1f%%)%n",
                                                    bucket, bucket + 49, count, percentage);
                               });
        
        // Performance assertions
        assertTrue(avgResponseTime < 500, "Average response time should be < 500ms");
        assertTrue(throughput >= 20, "Throughput should be >= 20 req/sec");
    }
    
    /**
     * Test container networking and service discovery
     */
    @Test
    @DisplayName("Container Networking Test")
    void testContainerNetworking() {
        // Verify that containers can communicate with each other
        assertTrue(userService.isRunning(), "User service should be running");
        assertTrue(orderService.isRunning(), "Order service should be running");
        assertTrue(apiGateway.isRunning(), "API Gateway should be running");
        
        // Verify network configuration
        assertEquals(network, userService.getNetwork());
        assertEquals(network, orderService.getNetwork());
        assertEquals(network, apiGateway.getNetwork());
        
        // Verify that containers are accessible on their mapped ports
        String userServiceUrl = String.format("http://%s:%d/health",
                userService.getHost(), userService.getMappedPort(80));
        String orderServiceUrl = String.format("http://%s:%d/health",
                orderService.getHost(), orderService.getMappedPort(80));
        
        assertDoesNotThrow(() -> {
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create(userServiceUrl))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
        }, "Should be able to connect to user service directly");
        
        assertDoesNotThrow(() -> {
            HttpRequest orderRequest = HttpRequest.newBuilder()
                    .uri(URI.create(orderServiceUrl))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            httpClient.send(orderRequest, HttpResponse.BodyHandlers.ofString());
        }, "Should be able to connect to order service directly");
    }
    
    private void assertServiceHealth(GenericContainer<?> service, String serviceName) 
            throws Exception {
        String healthUrl = String.format("http://%s:%d/health",
                service.getHost(), service.getMappedPort(80));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(5))
                .build();
        
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), serviceName + " should be healthy");
        System.out.printf("✅ %s is healthy at %s%n", serviceName, healthUrl);
    }
}
```

---

## 🔧 Part 3: Advanced Integration Testing Patterns (55 minutes)

### **Exercise 3: End-to-End Testing Pipeline**

Create `EndToEndTestPipeline.java`:

```java
import org.junit.jupiter.api.*;
import org.testcontainers.containers.*;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;

/**
 * Comprehensive end-to-end testing pipeline with performance monitoring
 */
@Testcontainers
@DisplayName("End-to-End Test Pipeline")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndTestPipeline {
    
    private static final Network testNetwork = Network.newNetwork();
    
    @Container
    static PostgreSQLContainer<?> database = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withNetwork(testNetwork)
            .withNetworkAliases("postgres")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("e2e-init.sql");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(testNetwork)
            .withNetworkAliases("redis")
            .withExposedPorts(6379);
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withNetwork(testNetwork)
            .withNetworkAliases("kafka");
    
    @Container
    static GenericContainer<?> elasticsearch = new GenericContainer<>(DockerImageName.parse("elasticsearch:8.8.0"))
            .withNetwork(testNetwork)
            .withNetworkAliases("elasticsearch")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withExposedPorts(9200)
            .waitingFor(Wait.forHttp("/_cluster/health").forPort(9200));
    
    @Container
    static DockerComposeContainer<?> applicationStack = new DockerComposeContainer<>(
            MountableFile.forClasspathResource("docker-compose-test.yml"))
            .withNetwork(testNetwork)
            .withExposedService("app", 8080, Wait.forHttp("/actuator/health").forStatusCode(200))
            .withExposedService("worker", 8081, Wait.forHttp("/health").forStatusCode(200));
    
    private static HttpClient httpClient;
    private static String appBaseUrl;
    private static TestMetricsCollector metricsCollector;
    
    @BeforeAll
    static void setUpTestEnvironment() {
        System.out.println("🏗️  Setting up end-to-end test environment...");
        
        httpClient = HttpClient.newBuilder()
                              .connectTimeout(Duration.ofSeconds(30))
                              .build();
        
        appBaseUrl = String.format("http://%s:%d",
                applicationStack.getServiceHost("app", 8080),
                applicationStack.getServicePort("app", 8080));
        
        metricsCollector = new TestMetricsCollector();
        
        System.out.printf("🚀 Application available at: %s%n", appBaseUrl);
        
        // Wait for all services to be fully ready
        waitForServiceReady();
    }
    
    @AfterAll
    static void tearDownTestEnvironment() {
        metricsCollector.printFinalReport();
        System.out.println("🧹 Test environment cleaned up");
    }
    
    /**
     * Test 1: Infrastructure readiness
     */
    @Test
    @Order(1)
    @DisplayName("Infrastructure Readiness Check")
    void testInfrastructureReadiness() throws Exception {
        System.out.println("🔍 Checking infrastructure readiness...");
        
        // Check database
        assertTrue(database.isRunning(), "Database should be running");
        
        // Check Redis
        assertTrue(redis.isRunning(), "Redis should be running");
        
        // Check Kafka
        assertTrue(kafka.isRunning(), "Kafka should be running");
        
        // Check Elasticsearch
        HttpRequest esRequest = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://%s:%d/_cluster/health",
                        elasticsearch.getHost(), elasticsearch.getMappedPort(9200))))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> esResponse = httpClient.send(esRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, esResponse.statusCode());
        
        // Check application services
        HttpRequest appHealthRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/actuator/health"))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> appHealthResponse = httpClient.send(appHealthRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, appHealthResponse.statusCode());
        
        System.out.println("✅ All infrastructure components are ready");
    }
    
    /**
     * Test 2: Data flow and persistence
     */
    @Test
    @Order(2)
    @DisplayName("Data Flow and Persistence")
    void testDataFlowAndPersistence() throws Exception {
        System.out.println("📊 Testing data flow and persistence...");
        
        long startTime = System.currentTimeMillis();
        
        // Create test data through API
        String testData = """
            {
                "name": "Integration Test User",
                "email": "integration@example.com",
                "metadata": {
                    "source": "e2e-test",
                    "timestamp": "%s"
                }
            }
            """.formatted(System.currentTimeMillis());
        
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(testData))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> createResponse = httpClient.send(createRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(201, createResponse.statusCode());
        assertTrue(createResponse.body().contains("integration@example.com"));
        
        // Extract user ID from response
        String userId = extractUserIdFromResponse(createResponse.body());
        assertNotNull(userId, "User ID should be returned");
        
        // Verify data persistence
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/users/" + userId))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> getResponse = httpClient.send(getRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("Integration Test User"));
        
        long duration = System.currentTimeMillis() - startTime;
        metricsCollector.recordTest("data_flow_persistence", duration, true);
        
        System.out.printf("✅ Data flow test completed in %d ms%n", duration);
    }
    
    /**
     * Test 3: Caching and performance
     */
    @Test
    @Order(3)
    @DisplayName("Caching and Performance")
    void testCachingAndPerformance() throws Exception {
        System.out.println("⚡ Testing caching and performance...");
        
        // Create test user first
        String testData = """
            {
                "name": "Cache Test User",
                "email": "cache@example.com",
                "metadata": {"test": "cache"}
            }
            """;
        
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(testData))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> createResponse = httpClient.send(createRequest,
                HttpResponse.BodyHandlers.ofString());
        String userId = extractUserIdFromResponse(createResponse.body());
        
        // First access - should hit database
        long firstAccessStart = System.nanoTime();
        HttpRequest firstRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/users/" + userId))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> firstResponse = httpClient.send(firstRequest,
                HttpResponse.BodyHandlers.ofString());
        long firstAccessTime = System.nanoTime() - firstAccessStart;
        
        assertEquals(200, firstResponse.statusCode());
        
        // Second access - should hit cache
        long secondAccessStart = System.nanoTime();
        HttpResponse<String> secondResponse = httpClient.send(firstRequest,
                HttpResponse.BodyHandlers.ofString());
        long secondAccessTime = System.nanoTime() - secondAccessStart;
        
        assertEquals(200, secondResponse.statusCode());
        assertEquals(firstResponse.body(), secondResponse.body());
        
        // Cache should be faster
        double speedupRatio = (double) firstAccessTime / secondAccessTime;
        assertTrue(speedupRatio > 1.5, 
                  String.format("Cache should be faster (speedup: %.1fx)", speedupRatio));
        
        metricsCollector.recordTest("cache_performance", 
                                   (firstAccessTime + secondAccessTime) / 2_000_000, true);
        
        System.out.printf("✅ Cache test: first access %d ns, second access %d ns (%.1fx speedup)%n",
                         firstAccessTime, secondAccessTime, speedupRatio);
    }
    
    /**
     * Test 4: Message processing and events
     */
    @Test
    @Order(4)
    @DisplayName("Message Processing and Events")
    void testMessageProcessingAndEvents() throws Exception {
        System.out.println("📨 Testing message processing and events...");
        
        long startTime = System.currentTimeMillis();
        
        // Trigger an event that should be processed asynchronously
        String eventData = """
            {
                "eventType": "USER_REGISTERED",
                "userId": "test-user-123",
                "timestamp": "%s",
                "data": {
                    "email": "event@example.com",
                    "source": "e2e-test"
                }
            }
            """.formatted(System.currentTimeMillis());
        
        HttpRequest eventRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/events"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(eventData))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> eventResponse = httpClient.send(eventRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(202, eventResponse.statusCode()); // Accepted for processing
        
        // Wait for asynchronous processing and verify
        boolean eventProcessed = false;
        for (int i = 0; i < 30; i++) { // Wait up to 30 seconds
            Thread.sleep(1000);
            
            HttpRequest statusRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appBaseUrl + "/api/events/test-user-123/status"))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            HttpResponse<String> statusResponse = httpClient.send(statusRequest,
                    HttpResponse.BodyHandlers.ofString());
            
            if (statusResponse.statusCode() == 200 && 
                statusResponse.body().contains("PROCESSED")) {
                eventProcessed = true;
                break;
            }
        }
        
        assertTrue(eventProcessed, "Event should be processed within 30 seconds");
        
        long duration = System.currentTimeMillis() - startTime;
        metricsCollector.recordTest("message_processing", duration, true);
        
        System.out.printf("✅ Message processing test completed in %d ms%n", duration);
    }
    
    /**
     * Test 5: Search and indexing
     */
    @Test
    @Order(5)
    @DisplayName("Search and Indexing")
    void testSearchAndIndexing() throws Exception {
        System.out.println("🔍 Testing search and indexing...");
        
        long startTime = System.currentTimeMillis();
        
        // Create searchable content
        String[] testUsers = {
            """
            {
                "name": "Alice Johnson",
                "email": "alice@example.com",
                "department": "Engineering",
                "skills": ["Java", "Spring", "Microservices"]
            }
            """,
            """
            {
                "name": "Bob Smith",
                "email": "bob@example.com", 
                "department": "Engineering",
                "skills": ["Python", "Django", "Machine Learning"]
            }
            """,
            """
            {
                "name": "Carol Davis",
                "email": "carol@example.com",
                "department": "Marketing",
                "skills": ["SEO", "Content Marketing", "Analytics"]
            }
            """
        };
        
        // Create users (should trigger indexing)
        for (String userData : testUsers) {
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appBaseUrl + "/api/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(userData))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(createRequest,
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(201, response.statusCode());
        }
        
        // Wait for indexing
        Thread.sleep(3000);
        
        // Test search functionality
        HttpRequest searchRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/search?q=Engineering&department=Engineering"))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> searchResponse = httpClient.send(searchRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, searchResponse.statusCode());
        assertTrue(searchResponse.body().contains("Alice Johnson"));
        assertTrue(searchResponse.body().contains("Bob Smith"));
        assertFalse(searchResponse.body().contains("Carol Davis"));
        
        // Test skill-based search
        HttpRequest skillSearchRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/search?q=Java"))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> skillSearchResponse = httpClient.send(skillSearchRequest,
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, skillSearchResponse.statusCode());
        assertTrue(skillSearchResponse.body().contains("Alice Johnson"));
        
        long duration = System.currentTimeMillis() - startTime;
        metricsCollector.recordTest("search_indexing", duration, true);
        
        System.out.printf("✅ Search and indexing test completed in %d ms%n", duration);
    }
    
    /**
     * Test 6: Load and stress testing
     */
    @Test
    @Order(6)
    @DisplayName("Load and Stress Testing")
    void testLoadAndStress() throws Exception {
        System.out.println("💪 Running load and stress tests...");
        
        int numRequests = 1000;
        int numThreads = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    
                    // Mix of different operations
                    if (requestId % 3 == 0) {
                        // Create user
                        String userData = String.format("""
                            {
                                "name": "Load Test User %d",
                                "email": "load%d@example.com",
                                "metadata": {"test": "load", "id": %d}
                            }
                            """, requestId, requestId, requestId);
                        
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(appBaseUrl + "/api/users"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(userData))
                                .timeout(Duration.ofSeconds(30))
                                .build();
                        
                        HttpResponse<String> response = httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());
                        
                        if (response.statusCode() == 201) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } else {
                        // Read operation
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(appBaseUrl + "/api/users?page=0&size=10"))
                                .timeout(Duration.ofSeconds(30))
                                .build();
                        
                        HttpResponse<String> response = httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());
                        
                        if (response.statusCode() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    }
                    
                    long requestDuration = System.currentTimeMillis() - requestStart;
                    responseTimes.add(requestDuration);
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.printf("Load test request %d failed: %s%n", requestId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long totalDuration = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        // Calculate statistics
        double successRate = (double) successCount.get() / numRequests;
        double throughput = numRequests * 1000.0 / totalDuration;
        
        responseTimes.sort(Long::compareTo);
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p95ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.99));
        long maxResponseTime = responseTimes.get(responseTimes.size() - 1);
        
        System.out.printf("📊 Load Test Results:%n");
        System.out.printf("   Total requests: %d%n", numRequests);
        System.out.printf("   Success rate: %.1f%% (%d/%d)%n", 
                         successRate * 100, successCount.get(), numRequests);
        System.out.printf("   Error count: %d%n", errorCount.get());
        System.out.printf("   Total duration: %d ms%n", totalDuration);
        System.out.printf("   Throughput: %.1f req/sec%n", throughput);
        System.out.printf("   Avg response time: %.1f ms%n", avgResponseTime);
        System.out.printf("   P95 response time: %d ms%n", p95ResponseTime);
        System.out.printf("   P99 response time: %d ms%n", p99ResponseTime);
        System.out.printf("   Max response time: %d ms%n", maxResponseTime);
        
        metricsCollector.recordTest("load_stress", totalDuration, true);
        
        // Performance assertions
        assertTrue(successRate >= 0.95, "Success rate should be >= 95%");
        assertTrue(throughput >= 10, "Throughput should be >= 10 req/sec");
        assertTrue(p95ResponseTime <= 5000, "P95 response time should be <= 5 seconds");
        assertTrue(avgResponseTime <= 2000, "Average response time should be <= 2 seconds");
    }
    
    /**
     * Test 7: Failure recovery and resilience
     */
    @Test
    @Order(7)
    @DisplayName("Failure Recovery and Resilience")
    void testFailureRecoveryAndResilience() throws Exception {
        System.out.println("🔄 Testing failure recovery and resilience...");
        
        // First, ensure system is working normally
        HttpRequest healthRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/actuator/health"))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> healthResponse = httpClient.send(healthRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, healthResponse.statusCode());
        
        // Simulate database failure by stopping database container
        database.stop();
        
        // Wait for failure to propagate
        Thread.sleep(5000);
        
        // System should degrade gracefully
        HttpResponse<String> degradedHealthResponse = httpClient.send(healthRequest,
                HttpResponse.BodyHandlers.ofString());
        
        // Health check might return 503 (Service Unavailable) but shouldn't crash
        assertTrue(degradedHealthResponse.statusCode() >= 200 && degradedHealthResponse.statusCode() < 600,
                  "System should respond to health checks even during failures");
        
        // Restart database
        database.start();
        
        // Wait for recovery
        boolean recovered = false;
        for (int i = 0; i < 60; i++) { // Wait up to 60 seconds for recovery
            Thread.sleep(1000);
            
            try {
                HttpResponse<String> recoveryHealthResponse = httpClient.send(healthRequest,
                        HttpResponse.BodyHandlers.ofString());
                
                if (recoveryHealthResponse.statusCode() == 200) {
                    recovered = true;
                    break;
                }
            } catch (Exception e) {
                // Continue waiting
            }
        }
        
        assertTrue(recovered, "System should recover within 60 seconds");
        
        // Verify full functionality is restored
        String testData = """
            {
                "name": "Recovery Test User",
                "email": "recovery@example.com",
                "metadata": {"test": "recovery"}
            }
            """;
        
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(appBaseUrl + "/api/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(testData))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> createResponse = httpClient.send(createRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResponse.statusCode());
        
        metricsCollector.recordTest("failure_recovery", 60000, true);
        
        System.out.println("✅ Failure recovery test completed successfully");
    }
    
    // Helper methods
    private static void waitForServiceReady() {
        try {
            Thread.sleep(10000); // Give services time to start and connect
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private String extractUserIdFromResponse(String responseBody) {
        // Simplified extraction - in real implementation, use JSON parser
        if (responseBody.contains("\"id\":")) {
            int start = responseBody.indexOf("\"id\":") + 5;
            int end = responseBody.indexOf(",", start);
            if (end == -1) end = responseBody.indexOf("}", start);
            return responseBody.substring(start, end).trim().replace("\"", "");
        }
        return null;
    }
}

/**
 * Test metrics collector for performance tracking
 */
class TestMetricsCollector {
    private final Map<String, List<TestResult>> testResults = new ConcurrentHashMap<>();
    
    public void recordTest(String testName, long durationMs, boolean success) {
        testResults.computeIfAbsent(testName, k -> new ArrayList<>())
                  .add(new TestResult(durationMs, success, System.currentTimeMillis()));
    }
    
    public void printFinalReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 END-TO-END TEST PERFORMANCE REPORT");
        System.out.println("=".repeat(80));
        
        testResults.forEach((testName, results) -> {
            long totalDuration = results.stream().mapToLong(r -> r.durationMs).sum();
            long successCount = results.stream().mapToLong(r -> r.success ? 1 : 0).sum();
            double avgDuration = results.stream().mapToLong(r -> r.durationMs).average().orElse(0.0);
            
            System.out.printf("\n🧪 %s:%n", testName);
            System.out.printf("   Executions: %d%n", results.size());
            System.out.printf("   Success rate: %.1f%% (%d/%d)%n", 
                             (double) successCount / results.size() * 100, successCount, results.size());
            System.out.printf("   Total time: %d ms%n", totalDuration);
            System.out.printf("   Average time: %.1f ms%n", avgDuration);
        });
        
        long overallDuration = testResults.values().stream()
                                         .flatMap(List::stream)
                                         .mapToLong(r -> r.durationMs)
                                         .sum();
        
        System.out.printf("\n📈 OVERALL SUMMARY:%n");
        System.out.printf("   Total test time: %d ms (%.1f seconds)%n", 
                         overallDuration, overallDuration / 1000.0);
        System.out.printf("   Tests executed: %d%n", testResults.size());
    }
    
    private record TestResult(long durationMs, boolean success, long timestamp) {}
}
```

---

## 📝 Key Takeaways from Day 46

### **TestContainers Mastery:**
- ✅ **Container Lifecycle**: Managing Docker containers for integration tests
- ✅ **Network Configuration**: Setting up container networks and service discovery
- ✅ **Database Testing**: Comprehensive database integration with real databases
- ✅ **Service Orchestration**: Multi-container environments with Docker Compose
- ✅ **Performance Integration**: Monitoring performance in realistic environments

### **Integration Testing Patterns:**
- ✅ **End-to-End Pipelines**: Complete user journey testing with real dependencies
- ✅ **Microservice Testing**: Service-to-service communication and API testing
- ✅ **Data Flow Testing**: Persistence, caching, and message processing validation
- ✅ **Resilience Testing**: Failure simulation and recovery verification
- ✅ **Load Testing**: Performance under realistic load conditions

### **Production-Like Testing:**
- ✅ **Real Dependencies**: Testing against actual databases, message queues, and services
- ✅ **Network Simulation**: Container networking and service discovery patterns
- ✅ **Performance Monitoring**: Metrics collection and performance validation
- ✅ **Failure Recovery**: Chaos engineering and resilience testing
- ✅ **Test Environment Management**: Automated setup and teardown of complex environments

---

## 🚀 Tomorrow's Preview (Day 47 - August 25)

**Focus:** Load Testing & Performance Benchmarking
- JMH microbenchmarking framework for precise performance measurement
- Load testing tools and strategies for scalability testing
- Performance regression detection and continuous benchmarking
- Memory and CPU profiling under load conditions
- Building comprehensive performance test suites

**Preparation:**
- Review today's integration testing patterns
- Think about performance bottlenecks in complex systems
- Consider load testing scenarios for your applications

---

## ✅ Day 46 Checklist

**Completed Learning Objectives:**
- [ ] Mastered TestContainers for Docker-based integration testing
- [ ] Implemented comprehensive database integration testing strategies
- [ ] Built microservice integration test suites with performance monitoring
- [ ] Created end-to-end testing pipelines with real external dependencies
- [ ] Developed service virtualization and test environment management
- [ ] Integrated performance testing with realistic production-like scenarios
- [ ] Built resilience and failure recovery testing patterns
- [ ] Practiced advanced container orchestration for testing

**Bonus Achievements:**
- [ ] Extended integration tests with chaos engineering patterns
- [ ] Implemented automated performance regression detection
- [ ] Created reusable test environment templates
- [ ] Explored advanced TestContainers features and modules

---

## 🎉 Professional Integration Testing!

You've mastered comprehensive integration testing with TestContainers, building production-grade test suites that validate entire system behavior. Tomorrow we'll focus on performance benchmarking and load testing for scalability validation.

**Your integration testing skills are enterprise-ready!** 🚀