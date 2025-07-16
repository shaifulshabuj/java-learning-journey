# Day 26: Application Properties & Profiles (August 4, 2025)

**Date:** August 4, 2025  
**Duration:** 2 hours  
**Focus:** Master configuration management with application properties, profiles, and externalized configuration

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand Spring Boot configuration hierarchy and precedence
- ✅ Master application.properties vs application.yml formats
- ✅ Implement environment-specific profiles (dev, test, prod)
- ✅ Create type-safe configuration with @ConfigurationProperties
- ✅ Handle externalized configuration and environment variables
- ✅ Build conditional beans and profile-specific configurations

---

## ⚙️ Part 1: Configuration Fundamentals (30 minutes)

### **Spring Boot Configuration Hierarchy**

Spring Boot follows a specific **configuration precedence order**:

1. **Command line arguments** (`--server.port=8081`)
2. **Java System properties** (`-Dserver.port=8081`)
3. **OS environment variables** (`SERVER_PORT=8081`)
4. **Profile-specific properties** (`application-prod.properties`)
5. **Application properties** (`application.properties`)
6. **Default properties** (defined in code)

### **Properties vs YAML Configuration**

Create `ConfigurationPropertiesDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import javax.validation.constraints.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties demonstration
 * Shows different ways to handle application configuration
 */
@SpringBootApplication
@EnableConfigurationProperties({
    AppProperties.class,
    DatabaseProperties.class,
    SecurityProperties.class
})
public class ConfigurationPropertiesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Configuration Properties Demo ===\n");
        
        // Example of setting properties programmatically
        System.setProperty("app.debug", "true");
        System.setProperty("app.max-connections", "100");
        
        SpringApplication.run(ConfigurationPropertiesDemo.class, args);
        
        System.out.println("⚙️ Configuration Features:");
        System.out.println("   - Type-safe configuration binding");
        System.out.println("   - Validation and constraints");
        System.out.println("   - Profile-specific properties");
        System.out.println("   - Environment variable support");
        System.out.println("   - Conditional configuration");
        System.out.println();
        
        System.out.println("📊 Configuration Sources:");
        System.out.println("   - application.properties");
        System.out.println("   - application.yml");
        System.out.println("   - Environment variables");
        System.out.println("   - Command line arguments");
        System.out.println("   - System properties");
        System.out.println();
    }
}

/**
 * Main application properties
 * Demonstrates type-safe configuration binding
 */
@ConfigurationProperties(prefix = "app")
@Component
class AppProperties {
    
    @NotBlank(message = "Application name is required")
    private String name = "Spring Boot Application";
    
    @NotBlank(message = "Version is required")
    private String version = "1.0.0";
    
    @NotBlank(message = "Description is required")
    private String description = "A sample Spring Boot application";
    
    @Min(value = 1, message = "Max connections must be at least 1")
    @Max(value = 1000, message = "Max connections cannot exceed 1000")
    private int maxConnections = 50;
    
    @Min(value = 1000, message = "Timeout must be at least 1000ms")
    private long timeoutMs = 30000;
    
    private boolean debug = false;
    private boolean enableMetrics = true;
    private boolean enableHealthCheck = true;
    
    @NotNull(message = "Features list cannot be null")
    private List<String> features = List.of("security", "monitoring", "logging");
    
    @NotNull(message = "Configuration map cannot be null")
    private Map<String, String> configuration = Map.of(
        "log.level", "INFO",
        "cache.enabled", "true",
        "backup.enabled", "false"
    );
    
    // Nested configuration
    private final Api api = new Api();
    private final Mail mail = new Mail();
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }
    
    public boolean isEnableMetrics() { return enableMetrics; }
    public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
    
    public boolean isEnableHealthCheck() { return enableHealthCheck; }
    public void setEnableHealthCheck(boolean enableHealthCheck) { this.enableHealthCheck = enableHealthCheck; }
    
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    
    public Map<String, String> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, String> configuration) { this.configuration = configuration; }
    
    public Api getApi() { return api; }
    public Mail getMail() { return mail; }
    
    // Nested classes for grouped configuration
    public static class Api {
        private String baseUrl = "http://localhost:8080/api";
        private String version = "v1";
        private int rateLimit = 100;
        private Duration timeout = Duration.ofSeconds(30);
        private boolean enableCors = true;
        
        // Getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public int getRateLimit() { return rateLimit; }
        public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
        
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        
        public boolean isEnableCors() { return enableCors; }
        public void setEnableCors(boolean enableCors) { this.enableCors = enableCors; }
    }
    
    public static class Mail {
        private String host = "localhost";
        private int port = 587;
        private String username = "";
        private String password = "";
        private boolean enableTls = true;
        private boolean enableAuth = true;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public boolean isEnableTls() { return enableTls; }
        public void setEnableTls(boolean enableTls) { this.enableTls = enableTls; }
        
        public boolean isEnableAuth() { return enableAuth; }
        public void setEnableAuth(boolean enableAuth) { this.enableAuth = enableAuth; }
    }
}

/**
 * Database configuration properties
 * Demonstrates different data types and validation
 */
@ConfigurationProperties(prefix = "database")
@Component
class DatabaseProperties {
    
    @NotBlank(message = "Database URL is required")
    private String url = "jdbc:h2:mem:testdb";
    
    @NotBlank(message = "Database username is required")
    private String username = "sa";
    
    private String password = "";
    
    @NotBlank(message = "Driver class name is required")
    private String driverClassName = "org.h2.Driver";
    
    @Min(value = 1, message = "Minimum pool size must be at least 1")
    private int minPoolSize = 5;
    
    @Min(value = 1, message = "Maximum pool size must be at least 1")
    private int maxPoolSize = 20;
    
    @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
    private long connectionTimeoutMs = 30000;
    
    @Min(value = 1000, message = "Max lifetime must be at least 1000ms")
    private long maxLifetimeMs = 1800000;
    
    private boolean showSql = false;
    private boolean formatSql = false;
    
    @NotNull(message = "JPA properties cannot be null")
    private Map<String, String> jpaProperties = Map.of(
        "hibernate.hbm2ddl.auto", "create-drop",
        "hibernate.show_sql", "false",
        "hibernate.format_sql", "false"
    );
    
    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    
    public int getMinPoolSize() { return minPoolSize; }
    public void setMinPoolSize(int minPoolSize) { this.minPoolSize = minPoolSize; }
    
    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
    
    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
    
    public long getMaxLifetimeMs() { return maxLifetimeMs; }
    public void setMaxLifetimeMs(long maxLifetimeMs) { this.maxLifetimeMs = maxLifetimeMs; }
    
    public boolean isShowSql() { return showSql; }
    public void setShowSql(boolean showSql) { this.showSql = showSql; }
    
    public boolean isFormatSql() { return formatSql; }
    public void setFormatSql(boolean formatSql) { this.formatSql = formatSql; }
    
    public Map<String, String> getJpaProperties() { return jpaProperties; }
    public void setJpaProperties(Map<String, String> jpaProperties) { this.jpaProperties = jpaProperties; }
}

/**
 * Security configuration properties
 * Demonstrates complex validation and nested structures
 */
@ConfigurationProperties(prefix = "security")
@Component
class SecurityProperties {
    
    private boolean enabled = true;
    private boolean enableCsrf = true;
    private boolean enableFrameOptions = true;
    
    @NotNull(message = "JWT configuration cannot be null")
    private final Jwt jwt = new Jwt();
    
    @NotNull(message = "OAuth2 configuration cannot be null")
    private final OAuth2 oauth2 = new OAuth2();
    
    @NotNull(message = "CORS configuration cannot be null")
    private final Cors cors = new Cors();
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isEnableCsrf() { return enableCsrf; }
    public void setEnableCsrf(boolean enableCsrf) { this.enableCsrf = enableCsrf; }
    
    public boolean isEnableFrameOptions() { return enableFrameOptions; }
    public void setEnableFrameOptions(boolean enableFrameOptions) { this.enableFrameOptions = enableFrameOptions; }
    
    public Jwt getJwt() { return jwt; }
    public OAuth2 getOauth2() { return oauth2; }
    public Cors getCors() { return cors; }
    
    // JWT Configuration
    public static class Jwt {
        @NotBlank(message = "JWT secret is required")
        private String secret = "mySecretKey";
        
        @Min(value = 60, message = "Token expiration must be at least 60 seconds")
        private long expirationSeconds = 3600;
        
        @NotBlank(message = "JWT issuer is required")
        private String issuer = "spring-boot-app";
        
        // Getters and setters
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        
        public long getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
        
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
    }
    
    // OAuth2 Configuration
    public static class OAuth2 {
        private boolean enabled = false;
        
        @NotNull(message = "OAuth2 providers cannot be null")
        private Map<String, Provider> providers = Map.of(
            "google", new Provider("google-client-id", "google-client-secret"),
            "github", new Provider("github-client-id", "github-client-secret")
        );
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Map<String, Provider> getProviders() { return providers; }
        public void setProviders(Map<String, Provider> providers) { this.providers = providers; }
        
        // OAuth2 Provider
        public static class Provider {
            private String clientId;
            private String clientSecret;
            
            public Provider() {}
            
            public Provider(String clientId, String clientSecret) {
                this.clientId = clientId;
                this.clientSecret = clientSecret;
            }
            
            public String getClientId() { return clientId; }
            public void setClientId(String clientId) { this.clientId = clientId; }
            
            public String getClientSecret() { return clientSecret; }
            public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        }
    }
    
    // CORS Configuration
    public static class Cors {
        private boolean enabled = true;
        
        @NotNull(message = "Allowed origins cannot be null")
        private List<String> allowedOrigins = List.of("http://localhost:3000", "https://example.com");
        
        @NotNull(message = "Allowed methods cannot be null")
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        
        @NotNull(message = "Allowed headers cannot be null")
        private List<String> allowedHeaders = List.of("*");
        
        @Min(value = 0, message = "Max age cannot be negative")
        private long maxAge = 3600;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
        
        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    }
}

/**
 * Configuration controller to display current configuration
 */
@RestController
class ConfigurationController {
    
    private final AppProperties appProperties;
    private final DatabaseProperties databaseProperties;
    private final SecurityProperties securityProperties;
    private final Environment environment;
    
    @Value("${server.port:8080}")
    private int serverPort;
    
    @Value("${spring.application.name:unknown}")
    private String applicationName;
    
    @Value("${java.version:unknown}")
    private String javaVersion;
    
    public ConfigurationController(AppProperties appProperties,
                                 DatabaseProperties databaseProperties,
                                 SecurityProperties securityProperties,
                                 Environment environment) {
        this.appProperties = appProperties;
        this.databaseProperties = databaseProperties;
        this.securityProperties = securityProperties;
        this.environment = environment;
    }
    
    @GetMapping("/api/config/app")
    public AppProperties getAppConfiguration() {
        return appProperties;
    }
    
    @GetMapping("/api/config/database")
    public DatabaseProperties getDatabaseConfiguration() {
        return databaseProperties;
    }
    
    @GetMapping("/api/config/security")
    public SecurityProperties getSecurityConfiguration() {
        return securityProperties;
    }
    
    @GetMapping("/api/config/environment")
    public Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("activeProfiles", environment.getActiveProfiles());
        info.put("defaultProfiles", environment.getDefaultProfiles());
        info.put("serverPort", serverPort);
        info.put("applicationName", applicationName);
        info.put("javaVersion", javaVersion);
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("userHome", System.getProperty("user.home"));
        info.put("workingDirectory", System.getProperty("user.dir"));
        return info;
    }
    
    @GetMapping("/api/config/all")
    public Map<String, Object> getAllConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("app", appProperties);
        config.put("database", databaseProperties);
        config.put("security", securityProperties);
        config.put("environment", getEnvironmentInfo());
        return config;
    }
}
```

---

## 🌍 Part 2: Profiles for Environment Management (30 minutes)

### **Profile-Based Configuration**

Create `ProfilesDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * Spring profiles demonstration
 * Shows how to configure different environments
 */
@SpringBootApplication
public class ProfilesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Spring Profiles Demo ===\n");
        
        // Example of setting active profiles programmatically
        // System.setProperty("spring.profiles.active", "dev");
        
        SpringApplication.run(ProfilesDemo.class, args);
        
        System.out.println("🌍 Profile Management:");
        System.out.println("   - Development profile (dev)");
        System.out.println("   - Testing profile (test)");
        System.out.println("   - Production profile (prod)");
        System.out.println("   - Profile-specific beans");
        System.out.println("   - Conditional configuration");
        System.out.println();
        
        System.out.println("🚀 Activation Methods:");
        System.out.println("   - Command line: --spring.profiles.active=dev");
        System.out.println("   - Environment: SPRING_PROFILES_ACTIVE=dev");
        System.out.println("   - Properties: spring.profiles.active=dev");
        System.out.println("   - Programmatic: System.setProperty()");
        System.out.println();
    }
}

/**
 * Email service interface with profile-specific implementations
 */
interface EmailService {
    void sendEmail(String to, String subject, String body);
    String getServiceType();
    Map<String, Object> getConfiguration();
}

/**
 * Development email service - logs emails instead of sending
 */
@Service
@Profile("dev")
class DevEmailService implements EmailService {
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.println("=== DEV EMAIL SERVICE ===");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
        System.out.println("Email logged to console (dev mode)");
        System.out.println("========================");
    }
    
    @Override
    public String getServiceType() {
        return "Development Email Service";
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "development");
        config.put("enabled", true);
        config.put("logToConsole", true);
        config.put("sendActualEmails", false);
        return config;
    }
}

/**
 * Test email service - stores emails in memory for testing
 */
@Service
@Profile("test")
class TestEmailService implements EmailService {
    
    private final java.util.List<EmailMessage> sentEmails = new java.util.ArrayList<>();
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        EmailMessage email = new EmailMessage(to, subject, body);
        sentEmails.add(email);
        System.out.println("Email stored in memory for testing: " + email);
    }
    
    @Override
    public String getServiceType() {
        return "Test Email Service";
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "test");
        config.put("enabled", true);
        config.put("storeInMemory", true);
        config.put("emailCount", sentEmails.size());
        return config;
    }
    
    public java.util.List<EmailMessage> getSentEmails() {
        return new java.util.ArrayList<>(sentEmails);
    }
    
    public void clearEmails() {
        sentEmails.clear();
    }
    
    // Email message class
    public static class EmailMessage {
        private String to;
        private String subject;
        private String body;
        private java.time.LocalDateTime sentAt;
        
        public EmailMessage(String to, String subject, String body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
            this.sentAt = java.time.LocalDateTime.now();
        }
        
        // Getters
        public String getTo() { return to; }
        public String getSubject() { return subject; }
        public String getBody() { return body; }
        public java.time.LocalDateTime getSentAt() { return sentAt; }
        
        @Override
        public String toString() {
            return String.format("Email[to=%s, subject=%s, sentAt=%s]", to, subject, sentAt);
        }
    }
}

/**
 * Production email service - sends actual emails
 */
@Service
@Profile("prod")
class ProdEmailService implements EmailService {
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        // In real implementation, this would use JavaMail API
        System.out.println("=== PRODUCTION EMAIL SERVICE ===");
        System.out.println("Sending actual email to: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Using SMTP server: smtp.gmail.com");
        System.out.println("Email sent successfully!");
        System.out.println("===============================");
    }
    
    @Override
    public String getServiceType() {
        return "Production Email Service";
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "production");
        config.put("enabled", true);
        config.put("smtpServer", "smtp.gmail.com");
        config.put("port", 587);
        config.put("enableTLS", true);
        return config;
    }
}

/**
 * Database service interface with profile-specific implementations
 */
interface DatabaseService {
    void saveData(String data);
    String getDatabaseType();
    Map<String, Object> getConnectionInfo();
}

/**
 * Development database service - uses H2 in-memory database
 */
@Service
@Profile("dev")
class DevDatabaseService implements DatabaseService {
    
    @Override
    public void saveData(String data) {
        System.out.println("Saving to H2 in-memory database: " + data);
    }
    
    @Override
    public String getDatabaseType() {
        return "H2 In-Memory Database";
    }
    
    @Override
    public Map<String, Object> getConnectionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("type", "H2");
        info.put("url", "jdbc:h2:mem:devdb");
        info.put("inMemory", true);
        info.put("persistent", false);
        return info;
    }
}

/**
 * Test database service - uses embedded test database
 */
@Service
@Profile("test")
class TestDatabaseService implements DatabaseService {
    
    @Override
    public void saveData(String data) {
        System.out.println("Saving to embedded test database: " + data);
    }
    
    @Override
    public String getDatabaseType() {
        return "Embedded Test Database";
    }
    
    @Override
    public Map<String, Object> getConnectionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("type", "TestDB");
        info.put("url", "jdbc:h2:mem:testdb");
        info.put("inMemory", true);
        info.put("resetOnRestart", true);
        return info;
    }
}

/**
 * Production database service - uses MySQL/PostgreSQL
 */
@Service
@Profile("prod")
class ProdDatabaseService implements DatabaseService {
    
    @Override
    public void saveData(String data) {
        System.out.println("Saving to production PostgreSQL database: " + data);
    }
    
    @Override
    public String getDatabaseType() {
        return "PostgreSQL Production Database";
    }
    
    @Override
    public Map<String, Object> getConnectionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("type", "PostgreSQL");
        info.put("url", "jdbc:postgresql://prod-db:5432/myapp");
        info.put("persistent", true);
        info.put("backup", true);
        info.put("replication", true);
        return info;
    }
}

/**
 * Configuration classes for different profiles
 */
@Configuration
@Profile("dev")
class DevConfiguration {
    
    @Bean
    public String devMessage() {
        return "Running in DEVELOPMENT mode";
    }
    
    @Bean
    public Map<String, Object> devSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("debugMode", true);
        settings.put("logLevel", "DEBUG");
        settings.put("enableHotReload", true);
        settings.put("enableSwagger", true);
        settings.put("enableH2Console", true);
        return settings;
    }
}

@Configuration
@Profile("test")
class TestConfiguration {
    
    @Bean
    public String testMessage() {
        return "Running in TEST mode";
    }
    
    @Bean
    public Map<String, Object> testSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("debugMode", false);
        settings.put("logLevel", "INFO");
        settings.put("enableTestData", true);
        settings.put("enableMockServices", true);
        settings.put("enableTestReports", true);
        return settings;
    }
}

@Configuration
@Profile("prod")
class ProdConfiguration {
    
    @Bean
    public String prodMessage() {
        return "Running in PRODUCTION mode";
    }
    
    @Bean
    public Map<String, Object> prodSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("debugMode", false);
        settings.put("logLevel", "WARN");
        settings.put("enableSecurity", true);
        settings.put("enableMonitoring", true);
        settings.put("enableBackup", true);
        settings.put("enableCaching", true);
        return settings;
    }
}

/**
 * Profile-aware controller
 */
@RestController
class ProfileController {
    
    private final EmailService emailService;
    private final DatabaseService databaseService;
    private final Environment environment;
    
    @Autowired(required = false)
    private String devMessage;
    
    @Autowired(required = false)
    private String testMessage;
    
    @Autowired(required = false)
    private String prodMessage;
    
    @Autowired(required = false)
    private Map<String, Object> devSettings;
    
    @Autowired(required = false)
    private Map<String, Object> testSettings;
    
    @Autowired(required = false)
    private Map<String, Object> prodSettings;
    
    public ProfileController(EmailService emailService,
                           DatabaseService databaseService,
                           Environment environment) {
        this.emailService = emailService;
        this.databaseService = databaseService;
        this.environment = environment;
    }
    
    @GetMapping("/api/profiles/current")
    public Map<String, Object> getCurrentProfile() {
        Map<String, Object> info = new HashMap<>();
        info.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        info.put("defaultProfiles", Arrays.asList(environment.getDefaultProfiles()));
        
        // Add profile-specific message
        if (devMessage != null) info.put("message", devMessage);
        if (testMessage != null) info.put("message", testMessage);
        if (prodMessage != null) info.put("message", prodMessage);
        
        // Add profile-specific settings
        if (devSettings != null) info.put("settings", devSettings);
        if (testSettings != null) info.put("settings", testSettings);
        if (prodSettings != null) info.put("settings", prodSettings);
        
        return info;
    }
    
    @GetMapping("/api/profiles/services")
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> services = new HashMap<>();
        
        // Email service info
        Map<String, Object> emailInfo = new HashMap<>();
        emailInfo.put("type", emailService.getServiceType());
        emailInfo.put("configuration", emailService.getConfiguration());
        services.put("email", emailInfo);
        
        // Database service info
        Map<String, Object> dbInfo = new HashMap<>();
        dbInfo.put("type", databaseService.getDatabaseType());
        dbInfo.put("connection", databaseService.getConnectionInfo());
        services.put("database", dbInfo);
        
        return services;
    }
    
    @GetMapping("/api/profiles/send-email")
    public Map<String, Object> sendTestEmail() {
        String to = "user@example.com";
        String subject = "Test Email from " + emailService.getServiceType();
        String body = "This is a test email sent from profile: " + 
                     Arrays.toString(environment.getActiveProfiles());
        
        emailService.sendEmail(to, subject, body);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Email sent successfully");
        response.put("serviceType", emailService.getServiceType());
        response.put("to", to);
        response.put("subject", subject);
        
        return response;
    }
    
    @GetMapping("/api/profiles/save-data")
    public Map<String, Object> saveTestData() {
        String data = "Test data from profile: " + Arrays.toString(environment.getActiveProfiles());
        databaseService.saveData(data);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Data saved successfully");
        response.put("databaseType", databaseService.getDatabaseType());
        response.put("data", data);
        
        return response;
    }
}
```

---

## 🔧 Part 3: External Configuration & Environment Variables (30 minutes)

### **External Configuration Management**

Create `ExternalConfigDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * External configuration demonstration
 * Shows how to use environment variables and external config files
 */
@SpringBootApplication
public class ExternalConfigDemo {
    
    public static void main(String[] args) {
        System.out.println("=== External Configuration Demo ===\n");
        
        // Set some environment variables programmatically for demo
        System.setProperty("APP_NAME", "External Config Demo");
        System.setProperty("APP_VERSION", "2.0.0");
        System.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/myapp");
        System.setProperty("REDIS_HOST", "localhost");
        System.setProperty("REDIS_PORT", "6379");
        
        SpringApplication.run(ExternalConfigDemo.class, args);
        
        System.out.println("🔧 External Configuration Sources:");
        System.out.println("   - Environment variables (APP_NAME, DATABASE_URL)");
        System.out.println("   - System properties (-Dapp.name=MyApp)");
        System.out.println("   - Command line arguments (--server.port=8081)");
        System.out.println("   - External config files (application-prod.yml)");
        System.out.println("   - Cloud configuration services");
        System.out.println();
        
        System.out.println("📊 Configuration Examples:");
        System.out.println("   export APP_NAME='My Application'");
        System.out.println("   export DATABASE_URL='jdbc:mysql://localhost:3306/mydb'");
        System.out.println("   java -Dserver.port=8081 -jar myapp.jar");
        System.out.println("   java -jar myapp.jar --spring.profiles.active=prod");
        System.out.println();
    }
}

/**
 * External configuration properties
 * Maps environment variables to Java properties
 */
@ConfigurationProperties(prefix = "external")
@Component
class ExternalConfigProperties {
    
    // These will be mapped from environment variables or system properties
    private String appName;
    private String appVersion;
    private String environment;
    private String buildNumber;
    private String deploymentDate;
    
    // Database configuration from environment
    private final Database database = new Database();
    
    // Redis configuration from environment
    private final Redis redis = new Redis();
    
    // AWS configuration from environment
    private final Aws aws = new Aws();
    
    // Monitoring configuration
    private final Monitoring monitoring = new Monitoring();
    
    // Getters and setters
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public String getBuildNumber() { return buildNumber; }
    public void setBuildNumber(String buildNumber) { this.buildNumber = buildNumber; }
    
    public String getDeploymentDate() { return deploymentDate; }
    public void setDeploymentDate(String deploymentDate) { this.deploymentDate = deploymentDate; }
    
    public Database getDatabase() { return database; }
    public Redis getRedis() { return redis; }
    public Aws getAws() { return aws; }
    public Monitoring getMonitoring() { return monitoring; }
    
    // Database configuration
    public static class Database {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private int maxPoolSize = 20;
        private int minPoolSize = 5;
        private boolean enableSsl = false;
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        
        public int getMinPoolSize() { return minPoolSize; }
        public void setMinPoolSize(int minPoolSize) { this.minPoolSize = minPoolSize; }
        
        public boolean isEnableSsl() { return enableSsl; }
        public void setEnableSsl(boolean enableSsl) { this.enableSsl = enableSsl; }
    }
    
    // Redis configuration
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private int timeout = 2000;
        private boolean enableSsl = false;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public int getDatabase() { return database; }
        public void setDatabase(int database) { this.database = database; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public boolean isEnableSsl() { return enableSsl; }
        public void setEnableSsl(boolean enableSsl) { this.enableSsl = enableSsl; }
    }
    
    // AWS configuration
    public static class Aws {
        private String accessKeyId;
        private String secretAccessKey;
        private String region = "us-east-1";
        private String s3BucketName;
        private String sqsQueueUrl;
        private String snsTopicArn;
        
        // Getters and setters
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        
        public String getSecretAccessKey() { return secretAccessKey; }
        public void setSecretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; }
        
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        
        public String getS3BucketName() { return s3BucketName; }
        public void setS3BucketName(String s3BucketName) { this.s3BucketName = s3BucketName; }
        
        public String getSqsQueueUrl() { return sqsQueueUrl; }
        public void setSqsQueueUrl(String sqsQueueUrl) { this.sqsQueueUrl = sqsQueueUrl; }
        
        public String getSnsTopicArn() { return snsTopicArn; }
        public void setSnsTopicArn(String snsTopicArn) { this.snsTopicArn = snsTopicArn; }
    }
    
    // Monitoring configuration
    public static class Monitoring {
        private boolean enableMetrics = true;
        private boolean enableHealthCheck = true;
        private boolean enableTracing = false;
        private String metricsEndpoint = "/actuator/metrics";
        private String healthEndpoint = "/actuator/health";
        private int metricsIntervalSeconds = 60;
        
        // Getters and setters
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        
        public boolean isEnableHealthCheck() { return enableHealthCheck; }
        public void setEnableHealthCheck(boolean enableHealthCheck) { this.enableHealthCheck = enableHealthCheck; }
        
        public boolean isEnableTracing() { return enableTracing; }
        public void setEnableTracing(boolean enableTracing) { this.enableTracing = enableTracing; }
        
        public String getMetricsEndpoint() { return metricsEndpoint; }
        public void setMetricsEndpoint(String metricsEndpoint) { this.metricsEndpoint = metricsEndpoint; }
        
        public String getHealthEndpoint() { return healthEndpoint; }
        public void setHealthEndpoint(String healthEndpoint) { this.healthEndpoint = healthEndpoint; }
        
        public int getMetricsIntervalSeconds() { return metricsIntervalSeconds; }
        public void setMetricsIntervalSeconds(int metricsIntervalSeconds) { this.metricsIntervalSeconds = metricsIntervalSeconds; }
    }
}

/**
 * Service that uses external configuration
 */
@Component
class ExternalConfigService {
    
    private final ExternalConfigProperties config;
    
    // Direct environment variable access
    @Value("${APP_NAME:Default App Name}")
    private String appNameFromEnv;
    
    @Value("${DATABASE_URL:jdbc:h2:mem:testdb}")
    private String databaseUrlFromEnv;
    
    @Value("${REDIS_HOST:localhost}")
    private String redisHostFromEnv;
    
    @Value("${REDIS_PORT:6379}")
    private int redisPortFromEnv;
    
    // System properties
    @Value("${user.name:unknown}")
    private String systemUser;
    
    @Value("${java.version:unknown}")
    private String javaVersion;
    
    @Value("${os.name:unknown}")
    private String osName;
    
    public ExternalConfigService(ExternalConfigProperties config) {
        this.config = config;
    }
    
    public Map<String, Object> getEnvironmentVariables() {
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("APP_NAME", appNameFromEnv);
        envVars.put("DATABASE_URL", databaseUrlFromEnv);
        envVars.put("REDIS_HOST", redisHostFromEnv);
        envVars.put("REDIS_PORT", redisPortFromEnv);
        return envVars;
    }
    
    public Map<String, Object> getSystemProperties() {
        Map<String, Object> sysProps = new HashMap<>();
        sysProps.put("user.name", systemUser);
        sysProps.put("java.version", javaVersion);
        sysProps.put("os.name", osName);
        sysProps.put("java.home", System.getProperty("java.home"));
        sysProps.put("user.dir", System.getProperty("user.dir"));
        return sysProps;
    }
    
    public Map<String, Object> getExternalConfiguration() {
        Map<String, Object> externalConfig = new HashMap<>();
        externalConfig.put("appName", config.getAppName());
        externalConfig.put("appVersion", config.getAppVersion());
        externalConfig.put("environment", config.getEnvironment());
        externalConfig.put("buildNumber", config.getBuildNumber());
        externalConfig.put("deploymentDate", config.getDeploymentDate());
        return externalConfig;
    }
    
    public boolean isDatabaseConfigured() {
        return config.getDatabase().getUrl() != null && 
               !config.getDatabase().getUrl().isEmpty();
    }
    
    public boolean isRedisConfigured() {
        return config.getRedis().getHost() != null && 
               !config.getRedis().getHost().isEmpty();
    }
    
    public boolean isAwsConfigured() {
        return config.getAws().getAccessKeyId() != null && 
               !config.getAws().getAccessKeyId().isEmpty();
    }
    
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("database", isDatabaseConfigured());
        status.put("redis", isRedisConfigured());
        status.put("aws", isAwsConfigured());
        status.put("monitoring", config.getMonitoring().isEnableMetrics());
        return status;
    }
}

/**
 * Controller for external configuration demonstration
 */
@RestController
class ExternalConfigController {
    
    private final ExternalConfigProperties config;
    private final ExternalConfigService configService;
    private final Environment environment;
    
    public ExternalConfigController(ExternalConfigProperties config,
                                   ExternalConfigService configService,
                                   Environment environment) {
        this.config = config;
        this.configService = configService;
        this.environment = environment;
    }
    
    @GetMapping("/api/external-config/environment")
    public Map<String, Object> getEnvironmentVariables() {
        return configService.getEnvironmentVariables();
    }
    
    @GetMapping("/api/external-config/system")
    public Map<String, Object> getSystemProperties() {
        return configService.getSystemProperties();
    }
    
    @GetMapping("/api/external-config/external")
    public Map<String, Object> getExternalConfiguration() {
        return configService.getExternalConfiguration();
    }
    
    @GetMapping("/api/external-config/database")
    public ExternalConfigProperties.Database getDatabaseConfig() {
        return config.getDatabase();
    }
    
    @GetMapping("/api/external-config/redis")
    public ExternalConfigProperties.Redis getRedisConfig() {
        return config.getRedis();
    }
    
    @GetMapping("/api/external-config/aws")
    public ExternalConfigProperties.Aws getAwsConfig() {
        return config.getAws();
    }
    
    @GetMapping("/api/external-config/monitoring")
    public ExternalConfigProperties.Monitoring getMonitoringConfig() {
        return config.getMonitoring();
    }
    
    @GetMapping("/api/external-config/status")
    public Map<String, Object> getConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("services", configService.getServiceStatus());
        status.put("activeProfiles", environment.getActiveProfiles());
        status.put("propertySourceNames", getPropertySourceNames());
        return status;
    }
    
    @GetMapping("/api/external-config/all")
    public Map<String, Object> getAllConfiguration() {
        Map<String, Object> allConfig = new HashMap<>();
        allConfig.put("environment", configService.getEnvironmentVariables());
        allConfig.put("system", configService.getSystemProperties());
        allConfig.put("external", configService.getExternalConfiguration());
        allConfig.put("database", config.getDatabase());
        allConfig.put("redis", config.getRedis());
        allConfig.put("aws", config.getAws());
        allConfig.put("monitoring", config.getMonitoring());
        allConfig.put("status", configService.getServiceStatus());
        return allConfig;
    }
    
    private java.util.List<String> getPropertySourceNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        
        // Get property source names from environment
        if (environment instanceof org.springframework.core.env.ConfigurableEnvironment) {
            org.springframework.core.env.ConfigurableEnvironment configurableEnv = 
                (org.springframework.core.env.ConfigurableEnvironment) environment;
            
            configurableEnv.getPropertySources().forEach(source -> {
                names.add(source.getName());
            });
        }
        
        return names;
    }
}
```

---

## 🔗 Part 4: Conditional Configuration & Beans (30 minutes)

### **Advanced Conditional Configuration**

Create `ConditionalBeansDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Conditional beans and configuration demonstration
 * Shows how to conditionally create beans based on various conditions
 */
@SpringBootApplication
public class ConditionalBeansDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Conditional Beans Demo ===\n");
        SpringApplication.run(ConditionalBeansDemo.class, args);
        
        System.out.println("🔗 Conditional Configuration:");
        System.out.println("   - @ConditionalOnProperty");
        System.out.println("   - @ConditionalOnClass");
        System.out.println("   - @ConditionalOnBean");
        System.out.println("   - @ConditionalOnProfile");
        System.out.println("   - Custom conditions");
        System.out.println();
    }
}

/**
 * Cache service interface
 */
interface CacheService {
    void put(String key, Object value);
    Object get(String key);
    void remove(String key);
    void clear();
    String getCacheType();
    Map<String, Object> getStatistics();
}

/**
 * In-memory cache service - always available
 */
@Service
@ConditionalOnMissingBean(CacheService.class)
class InMemoryCacheService implements CacheService {
    
    private final Map<String, Object> cache = new HashMap<>();
    
    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
        System.out.println("Stored in memory cache: " + key);
    }
    
    @Override
    public Object get(String key) {
        return cache.get(key);
    }
    
    @Override
    public void remove(String key) {
        cache.remove(key);
        System.out.println("Removed from memory cache: " + key);
    }
    
    @Override
    public void clear() {
        cache.clear();
        System.out.println("Cleared memory cache");
    }
    
    @Override
    public String getCacheType() {
        return "In-Memory Cache";
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "in-memory");
        stats.put("size", cache.size());
        stats.put("maxSize", "unlimited");
        return stats;
    }
}

/**
 * Redis cache service - only if Redis is configured
 */
@Service
@ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
class RedisCacheService implements CacheService {
    
    private final Map<String, Object> redisSimulation = new HashMap<>();
    
    @Override
    public void put(String key, Object value) {
        redisSimulation.put(key, value);
        System.out.println("Stored in Redis cache: " + key);
    }
    
    @Override
    public Object get(String key) {
        return redisSimulation.get(key);
    }
    
    @Override
    public void remove(String key) {
        redisSimulation.remove(key);
        System.out.println("Removed from Redis cache: " + key);
    }
    
    @Override
    public void clear() {
        redisSimulation.clear();
        System.out.println("Cleared Redis cache");
    }
    
    @Override
    public String getCacheType() {
        return "Redis Cache";
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "redis");
        stats.put("size", redisSimulation.size());
        stats.put("persistent", true);
        stats.put("distributed", true);
        return stats;
    }
}

/**
 * File storage service interface
 */
interface FileStorageService {
    void storeFile(String filename, byte[] content);
    byte[] retrieveFile(String filename);
    void deleteFile(String filename);
    String getStorageType();
    Map<String, Object> getConfiguration();
}

/**
 * Local file storage - default implementation
 */
@Service
@ConditionalOnMissingBean(FileStorageService.class)
class LocalFileStorageService implements FileStorageService {
    
    private final Map<String, byte[]> localStorage = new HashMap<>();
    
    @Override
    public void storeFile(String filename, byte[] content) {
        localStorage.put(filename, content);
        System.out.println("Stored file locally: " + filename);
    }
    
    @Override
    public byte[] retrieveFile(String filename) {
        return localStorage.get(filename);
    }
    
    @Override
    public void deleteFile(String filename) {
        localStorage.remove(filename);
        System.out.println("Deleted local file: " + filename);
    }
    
    @Override
    public String getStorageType() {
        return "Local File Storage";
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "local");
        config.put("location", "/tmp/files");
        config.put("maxSize", "1GB");
        return config;
    }
}

/**
 * AWS S3 storage service - only if AWS is configured
 */
@Service
@ConditionalOnProperty(name = "storage.aws.enabled", havingValue = "true")
class S3FileStorageService implements FileStorageService {
    
    private final Map<String, byte[]> s3Simulation = new HashMap<>();
    
    @Override
    public void storeFile(String filename, byte[] content) {
        s3Simulation.put(filename, content);
        System.out.println("Stored file in S3: " + filename);
    }
    
    @Override
    public byte[] retrieveFile(String filename) {
        return s3Simulation.get(filename);
    }
    
    @Override
    public void deleteFile(String filename) {
        s3Simulation.remove(filename);
        System.out.println("Deleted S3 file: " + filename);
    }
    
    @Override
    public String getStorageType() {
        return "AWS S3 Storage";
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "s3");
        config.put("bucket", "my-app-bucket");
        config.put("region", "us-east-1");
        config.put("encryption", true);
        return config;
    }
}

/**
 * Monitoring service interface
 */
interface MonitoringService {
    void recordMetric(String name, double value);
    void recordEvent(String event);
    String getMonitoringType();
    Map<String, Object> getMetrics();
}

/**
 * Console monitoring - always available
 */
@Service
@ConditionalOnMissingBean(MonitoringService.class)
class ConsoleMonitoringService implements MonitoringService {
    
    private final Map<String, Double> metrics = new HashMap<>();
    private final java.util.List<String> events = new java.util.ArrayList<>();
    
    @Override
    public void recordMetric(String name, double value) {
        metrics.put(name, value);
        System.out.println("Metric recorded: " + name + " = " + value);
    }
    
    @Override
    public void recordEvent(String event) {
        events.add(event);
        System.out.println("Event recorded: " + event);
    }
    
    @Override
    public String getMonitoringType() {
        return "Console Monitoring";
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "console");
        result.put("metrics", metrics);
        result.put("events", events.size());
        return result;
    }
}

/**
 * Prometheus monitoring - only if Prometheus is enabled
 */
@Service
@ConditionalOnProperty(name = "monitoring.prometheus.enabled", havingValue = "true")
class PrometheusMonitoringService implements MonitoringService {
    
    private final Map<String, Double> prometheusMetrics = new HashMap<>();
    
    @Override
    public void recordMetric(String name, double value) {
        prometheusMetrics.put(name, value);
        System.out.println("Prometheus metric recorded: " + name + " = " + value);
    }
    
    @Override
    public void recordEvent(String event) {
        System.out.println("Prometheus event recorded: " + event);
    }
    
    @Override
    public String getMonitoringType() {
        return "Prometheus Monitoring";
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "prometheus");
        result.put("metrics", prometheusMetrics);
        result.put("endpoint", "/actuator/prometheus");
        return result;
    }
}

/**
 * Configuration for conditional beans
 */
@Configuration
class ConditionalConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "features.advanced-logging", havingValue = "true")
    public String advancedLoggingConfig() {
        System.out.println("Advanced logging feature enabled");
        return "Advanced logging configured";
    }
    
    @Bean
    @ConditionalOnProperty(name = "features.security-audit", havingValue = "true")
    public String securityAuditConfig() {
        System.out.println("Security audit feature enabled");
        return "Security audit configured";
    }
    
    @Bean
    @ConditionalOnProperty(name = "features.performance-monitoring", havingValue = "true")
    public String performanceMonitoringConfig() {
        System.out.println("Performance monitoring feature enabled");
        return "Performance monitoring configured";
    }
    
    @Bean
    @ConditionalOnProperty(name = "integrations.slack.enabled", havingValue = "true")
    public String slackIntegration() {
        System.out.println("Slack integration enabled");
        return "Slack integration configured";
    }
    
    @Bean
    @ConditionalOnProperty(name = "integrations.email.enabled", havingValue = "true")
    public String emailIntegration() {
        System.out.println("Email integration enabled");
        return "Email integration configured";
    }
    
    // Custom conditional bean
    @Bean
    @Conditional(ProductionEnvironmentCondition.class)
    public String productionOnlyService() {
        System.out.println("Production-only service created");
        return "Production service configured";
    }
}

/**
 * Custom condition for production environment
 */
class ProductionEnvironmentCondition implements org.springframework.context.annotation.Condition {
    
    @Override
    public boolean matches(org.springframework.context.annotation.ConditionContext context,
                          org.springframework.core.type.AnnotatedTypeMetadata metadata) {
        
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        
        // Check if production profile is active
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        
        // Check if production property is set
        return "production".equals(context.getEnvironment().getProperty("app.environment"));
    }
}

/**
 * Controller to demonstrate conditional beans
 */
@RestController
class ConditionalController {
    
    private final CacheService cacheService;
    private final FileStorageService fileStorageService;
    private final MonitoringService monitoringService;
    
    public ConditionalController(CacheService cacheService,
                               FileStorageService fileStorageService,
                               MonitoringService monitoringService) {
        this.cacheService = cacheService;
        this.fileStorageService = fileStorageService;
        this.monitoringService = monitoringService;
    }
    
    @GetMapping("/api/conditional/services")
    public Map<String, Object> getActiveServices() {
        Map<String, Object> services = new HashMap<>();
        services.put("cache", cacheService.getCacheType());
        services.put("storage", fileStorageService.getStorageType());
        services.put("monitoring", monitoringService.getMonitoringType());
        return services;
    }
    
    @GetMapping("/api/conditional/test-cache")
    public Map<String, Object> testCache() {
        // Test cache functionality
        cacheService.put("test-key", "test-value");
        Object value = cacheService.get("test-key");
        
        Map<String, Object> result = new HashMap<>();
        result.put("service", cacheService.getCacheType());
        result.put("stored", "test-value");
        result.put("retrieved", value);
        result.put("statistics", cacheService.getStatistics());
        
        return result;
    }
    
    @GetMapping("/api/conditional/test-storage")
    public Map<String, Object> testStorage() {
        // Test file storage functionality
        byte[] content = "Hello, World!".getBytes();
        fileStorageService.storeFile("test.txt", content);
        byte[] retrieved = fileStorageService.retrieveFile("test.txt");
        
        Map<String, Object> result = new HashMap<>();
        result.put("service", fileStorageService.getStorageType());
        result.put("stored", "test.txt");
        result.put("retrieved", retrieved != null ? new String(retrieved) : null);
        result.put("configuration", fileStorageService.getConfiguration());
        
        return result;
    }
    
    @GetMapping("/api/conditional/test-monitoring")
    public Map<String, Object> testMonitoring() {
        // Test monitoring functionality
        monitoringService.recordMetric("test.metric", 42.0);
        monitoringService.recordEvent("Test event occurred");
        
        Map<String, Object> result = new HashMap<>();
        result.put("service", monitoringService.getMonitoringType());
        result.put("metricRecorded", "test.metric = 42.0");
        result.put("eventRecorded", "Test event occurred");
        result.put("metrics", monitoringService.getMetrics());
        
        return result;
    }
    
    @GetMapping("/api/conditional/all")
    public Map<String, Object> testAllServices() {
        Map<String, Object> results = new HashMap<>();
        
        // Test all services
        results.put("cache", testCache());
        results.put("storage", testStorage());
        results.put("monitoring", testMonitoring());
        results.put("activeServices", getActiveServices());
        
        return results;
    }
}
```

---

## 📋 Practice Exercises

### Exercise 1: Multi-Environment Configuration
Create a complete multi-environment setup:
- Development, staging, and production profiles
- Environment-specific database configurations
- Profile-specific security settings
- External configuration for sensitive data
- Docker-based deployment configurations

### Exercise 2: Feature Flags System
Implement a feature flags system using:
- @ConditionalOnProperty for feature toggles
- Runtime configuration changes
- A/B testing configurations
- User-specific feature flags
- Admin interface for managing flags

### Exercise 3: Cloud-Native Configuration
Build a cloud-native configuration system:
- 12-factor app compliance
- Kubernetes ConfigMaps and Secrets
- Environment variable injection
- Configuration validation and monitoring
- Hot reloading capabilities

---

## 🎯 Key Takeaways

### Configuration Best Practices:
1. **Type-safe configuration**: Use @ConfigurationProperties for complex configuration
2. **Profile separation**: Keep environment-specific settings separate
3. **External configuration**: Use environment variables for sensitive data
4. **Validation**: Validate configuration properties at startup
5. **Documentation**: Document all configuration options clearly

### Profile Management:
1. **Clear separation**: Keep profiles distinct and focused
2. **Naming convention**: Use consistent profile naming (dev, test, prod)
3. **Default values**: Provide sensible defaults for all properties
4. **Testing**: Test all profile configurations thoroughly
5. **Security**: Never commit sensitive configuration to version control

### Conditional Configuration:
1. **Conditional beans**: Use conditions to create environment-specific beans
2. **Feature flags**: Implement feature toggles with conditional properties
3. **Graceful degradation**: Provide fallback implementations
4. **Resource efficiency**: Only create beans when needed
5. **Testability**: Make conditional logic easily testable

### External Configuration:
1. **Environment variables**: Use for deployment-specific configuration
2. **Configuration hierarchy**: Understand Spring Boot's property precedence
3. **Secrets management**: Use proper secret management tools
4. **Configuration validation**: Validate external configuration at startup
5. **Monitoring**: Monitor configuration changes and their effects

---

## 🚀 Next Steps

Tomorrow, we'll explore **Testing with Spring Boot** (Day 27), where you'll learn:
- JUnit 5 with Spring Boot Test
- MockMvc for web layer testing
- @DataJpaTest for repository testing
- Integration testing strategies
- Test slices and test configuration

Your configuration management skills will be essential for building robust, deployable applications! 📊