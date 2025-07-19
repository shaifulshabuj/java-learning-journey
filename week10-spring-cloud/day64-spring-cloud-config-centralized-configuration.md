# Day 64: Spring Cloud Config - Centralized Configuration

## Learning Objectives
- Master centralized configuration management with Spring Cloud Config
- Set up Config Server and Config Clients
- Implement dynamic configuration updates
- Use various configuration backends (Git, Vault, Native)
- Apply security and encryption to configuration

---

## 1. Introduction to Spring Cloud Config

### 1.1 Why Centralized Configuration?

```java
// Traditional Application Configuration Problems
@SpringBootApplication
public class TraditionalApplication {
    
    // Problem 1: Environment-specific properties scattered across services
    // application-dev.properties
    // application-qa.properties
    // application-prod.properties
    
    // Problem 2: Configuration changes require redeployment
    // Problem 3: No version control for configuration
    // Problem 4: No centralized security for sensitive data
    // Problem 5: Difficult to maintain consistency across microservices
}

// Spring Cloud Config Solution
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
    
    // Benefits:
    // - Centralized configuration management
    // - Version controlled configuration (Git)
    // - Dynamic configuration updates
    // - Environment-specific configuration
    // - Encryption for sensitive data
    // - Audit trail for configuration changes
}
```

### 1.2 Spring Cloud Config Architecture

```java
// Config Server Setup
@Configuration
public class ConfigServerArchitecture {
    
    /*
     * Architecture Components:
     * 
     * 1. Config Server
     *    - Centralized configuration service
     *    - Serves configuration from backend store
     *    - Provides REST API for configuration
     *    
     * 2. Config Client
     *    - Microservices that consume configuration
     *    - Fetches configuration on startup
     *    - Can refresh configuration at runtime
     *    
     * 3. Configuration Backend
     *    - Git repository (most common)
     *    - File system
     *    - Vault
     *    - JDBC
     *    
     * 4. Configuration Flow:
     *    Client -> Config Server -> Backend Store
     */
}
```

---

## 2. Setting Up Config Server

### 2.1 Config Server Implementation

```java
// Config Server Application
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

// application.yml for Config Server
@Configuration
@ConfigurationProperties
public class ConfigServerProperties {
    /*
    server:
      port: 8888
      
    spring:
      application:
        name: config-server
      cloud:
        config:
          server:
            git:
              uri: https://github.com/your-org/config-repo
              default-label: main
              search-paths:
                - '{application}'
                - '{application}/{profile}'
              clone-on-start: true
              force-pull: true
              
    # For local file system
    # spring:
    #   profiles:
    #     active: native
    #   cloud:
    #     config:
    #       server:
    #         native:
    #           search-locations: classpath:/config, file:./config
    
    # Security
    security:
      user:
        name: admin
        password: secret
        
    # Encryption
    encrypt:
      key: ${ENCRYPT_KEY:defaultkey}
    */
}

// Custom Repository Configuration
@Configuration
public class CustomRepositoryConfiguration {
    
    @Bean
    @Profile("composite")
    public CompositeEnvironmentRepository compositeEnvironmentRepository(
            EnvironmentRepositoryFactory factory) {
        
        CompositeEnvironmentRepository repository = new CompositeEnvironmentRepository();
        
        // Git repository for main configuration
        GitEnvironmentRepository gitRepo = new GitEnvironmentRepository();
        gitRepo.setUri("https://github.com/your-org/config-repo");
        gitRepo.setLabel("main");
        repository.add("git", gitRepo);
        
        // JDBC repository for dynamic configuration
        JdbcEnvironmentRepository jdbcRepo = new JdbcEnvironmentRepository();
        jdbcRepo.setSql("SELECT key, value FROM properties WHERE application = ? AND profile = ?");
        repository.add("jdbc", jdbcRepo);
        
        // Vault for secrets
        VaultEnvironmentRepository vaultRepo = new VaultEnvironmentRepository();
        vaultRepo.setHost("vault.example.com");
        vaultRepo.setPort(8200);
        repository.add("vault", vaultRepo);
        
        return repository;
    }
}

// Health Check and Monitoring
@RestController
@RequestMapping("/admin")
public class ConfigServerAdminController {
    
    @Autowired
    private EnvironmentRepository environmentRepository;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        
        try {
            // Check repository connectivity
            Environment env = environmentRepository.findOne("test", "default", "main");
            health.put("repository", "CONNECTED");
            health.put("propertyCount", env.getPropertySources().size());
        } catch (Exception e) {
            health.put("repository", "DISCONNECTED");
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/refresh-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> refreshAllClients() {
        // Trigger refresh for all connected clients
        // This would typically use Spring Cloud Bus
        return ResponseEntity.ok("Refresh triggered for all clients");
    }
}
```

### 2.2 Advanced Config Server Features

```java
// Environment-Specific Configuration
@Component
public class EnvironmentProcessor implements EnvironmentPostProcessor {
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, 
                                     SpringApplication application) {
        
        // Add custom property sources
        MutablePropertySources propertySources = environment.getPropertySources();
        
        // Add environment-specific overrides
        Map<String, Object> overrides = new HashMap<>();
        String env = environment.getProperty("spring.profiles.active", "default");
        
        if ("production".equals(env)) {
            overrides.put("server.ssl.enabled", "true");
            overrides.put("management.endpoints.web.exposure.include", "health,info");
        } else {
            overrides.put("server.ssl.enabled", "false");
            overrides.put("management.endpoints.web.exposure.include", "*");
        }
        
        propertySources.addFirst(new MapPropertySource("overrides", overrides));
    }
}

// Custom Property Source
@Component
public class DatabasePropertySource extends PropertySource<DataSource> {
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabasePropertySource(DataSource dataSource) {
        super("database", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
    @Override
    public Object getProperty(String name) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT value FROM config WHERE key = ?",
                String.class,
                name
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}

// Configuration Webhook Handler
@RestController
@RequestMapping("/webhook")
public class ConfigWebhookController {
    
    @Autowired
    private RefreshScope refreshScope;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestBody Map<String, Object> payload) {
        
        if ("push".equals(event)) {
            String branch = extractBranch(payload);
            List<String> modifiedFiles = extractModifiedFiles(payload);
            
            // Check if configuration files were modified
            boolean configModified = modifiedFiles.stream()
                    .anyMatch(file -> file.endsWith(".yml") || file.endsWith(".properties"));
            
            if (configModified) {
                // Trigger configuration refresh
                eventPublisher.publishEvent(new RefreshRemoteApplicationEvent(
                    this, "config-server", null, "**"
                ));
                
                return ResponseEntity.ok("Configuration refresh triggered");
            }
        }
        
        return ResponseEntity.ok("No action taken");
    }
    
    private String extractBranch(Map<String, Object> payload) {
        String ref = (String) payload.get("ref");
        return ref.substring(ref.lastIndexOf("/") + 1);
    }
    
    @SuppressWarnings("unchecked")
    private List<String> extractModifiedFiles(Map<String, Object> payload) {
        List<String> files = new ArrayList<>();
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
        
        for (Map<String, Object> commit : commits) {
            files.addAll((List<String>) commit.get("modified"));
            files.addAll((List<String>) commit.get("added"));
        }
        
        return files;
    }
}
```

---

## 3. Config Client Implementation

### 3.1 Basic Config Client

```java
// Config Client Application
@SpringBootApplication
@EnableDiscoveryClient
public class ConfigClientApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ConfigClientApplication.class, args);
    }
}

// bootstrap.yml for Config Client
@Configuration
@ConfigurationProperties
public class ConfigClientBootstrap {
    /*
    spring:
      application:
        name: user-service
      cloud:
        config:
          uri: http://localhost:8888
          username: admin
          password: secret
          fail-fast: true
          retry:
            initial-interval: 1000
            max-attempts: 6
            max-interval: 2000
            multiplier: 1.1
          
      profiles:
        active: ${SPRING_PROFILES_ACTIVE:dev}
        
    # For service discovery
    # spring:
    #   cloud:
    #     config:
    #       discovery:
    #         enabled: true
    #         service-id: config-server
    */
}

// Configuration Properties
@Component
@ConfigurationProperties(prefix = "app")
@RefreshScope
@Data
public class ApplicationProperties {
    
    private String name;
    private String version;
    private Database database = new Database();
    private Cache cache = new Cache();
    private Security security = new Security();
    private List<String> allowedOrigins = new ArrayList<>();
    private Map<String, String> features = new HashMap<>();
    
    @Data
    public static class Database {
        private String url;
        private String username;
        private String password;
        private int maxConnections = 10;
        private Duration connectionTimeout = Duration.ofSeconds(30);
    }
    
    @Data
    public static class Cache {
        private boolean enabled = true;
        private int maxSize = 1000;
        private Duration ttl = Duration.ofMinutes(60);
        private String type = "redis";
    }
    
    @Data
    public static class Security {
        private String jwtSecret;
        private Duration jwtExpiration = Duration.ofHours(24);
        private boolean enabled = true;
        private List<String> publicPaths = Arrays.asList("/health", "/info");
    }
    
    @PostConstruct
    public void validateConfiguration() {
        if (database.getUrl() == null || database.getUrl().isEmpty()) {
            throw new IllegalStateException("Database URL is required");
        }
        
        if (security.isEnabled() && security.getJwtSecret() == null) {
            throw new IllegalStateException("JWT secret is required when security is enabled");
        }
    }
}

// Using Configuration in Services
@Service
@RefreshScope
public class UserService {
    
    @Autowired
    private ApplicationProperties properties;
    
    @Value("${app.feature.user-registration-enabled:true}")
    private boolean registrationEnabled;
    
    public UserResponse createUser(CreateUserRequest request) {
        if (!registrationEnabled) {
            throw new FeatureDisabledException("User registration is currently disabled");
        }
        
        // Use configuration properties
        String appName = properties.getName();
        String version = properties.getVersion();
        
        // Database configuration
        String dbUrl = properties.getDatabase().getUrl();
        int maxConnections = properties.getDatabase().getMaxConnections();
        
        // Create user logic here
        User user = new User();
        user.setCreatedBy(appName + " v" + version);
        
        return UserResponse.fromUser(user);
    }
    
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        System.out.println("Configuration refreshed. Registration enabled: " + registrationEnabled);
        
        // Re-initialize connections or caches if needed
        reinitializeConnections();
    }
    
    private void reinitializeConnections() {
        // Reinitialize database connections with new configuration
        // Refresh caches
        // Update security settings
    }
}
```

### 3.2 Dynamic Configuration Updates

```java
// Refresh Endpoint Controller
@RestController
@RequestMapping("/admin")
public class ConfigRefreshController {
    
    @Autowired
    private RefreshScope refreshScope;
    
    @Autowired
    private ApplicationProperties properties;
    
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        Map<String, Object> response = new HashMap<>();
        
        // Capture before values
        Map<String, Object> before = captureCurrentConfiguration();
        
        // Refresh configuration
        Set<String> refreshed = refreshScope.refreshAll();
        
        // Capture after values
        Map<String, Object> after = captureCurrentConfiguration();
        
        response.put("refreshedBeans", refreshed);
        response.put("before", before);
        response.put("after", after);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getCurrentConfig() {
        return ResponseEntity.ok(captureCurrentConfiguration());
    }
    
    private Map<String, Object> captureCurrentConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("appName", properties.getName());
        config.put("appVersion", properties.getVersion());
        config.put("cacheEnabled", properties.getCache().isEnabled());
        config.put("cacheSize", properties.getCache().getMaxSize());
        config.put("features", properties.getFeatures());
        
        return config;
    }
}

// Configuration Change Listener
@Component
public class ConfigurationChangeListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationChangeListener.class);
    
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        logger.info("Application context refreshed");
    }
    
    @EventListener
    public void handleEnvironmentChange(EnvironmentChangeEvent event) {
        logger.info("Environment changed. Keys: {}", event.getKeys());
        
        for (String key : event.getKeys()) {
            logger.info("Property changed: {}", key);
            
            // Handle specific property changes
            if (key.startsWith("app.cache")) {
                handleCacheConfigChange();
            } else if (key.startsWith("app.database")) {
                handleDatabaseConfigChange();
            }
        }
    }
    
    @EventListener
    public void handleRefreshScopeRefresh(RefreshScopeRefreshedEvent event) {
        logger.info("Refresh scope refreshed: {}", event.getName());
    }
    
    private void handleCacheConfigChange() {
        logger.info("Cache configuration changed, reinitializing cache...");
        // Reinitialize cache with new configuration
    }
    
    private void handleDatabaseConfigChange() {
        logger.info("Database configuration changed, updating connection pool...");
        // Update database connection pool
    }
}

// Custom Configuration Watcher
@Component
public class ConfigurationWatcher {
    
    @Autowired
    private ConfigServicePropertySourceLocator locator;
    
    @Autowired
    private RefreshScope refreshScope;
    
    @Scheduled(fixedDelay = 60000) // Check every minute
    public void watchConfiguration() {
        try {
            // Check if configuration has changed
            Environment currentEnv = locator.locate(new StandardEnvironment());
            
            if (hasConfigurationChanged(currentEnv)) {
                logger.info("Configuration change detected, triggering refresh");
                refreshScope.refreshAll();
            }
        } catch (Exception e) {
            logger.error("Error checking configuration", e);
        }
    }
    
    private boolean hasConfigurationChanged(Environment newEnv) {
        // Implementation to compare current and new configuration
        // Could use checksums, timestamps, or version numbers
        return false; // Placeholder
    }
}
```

---

## 4. Configuration Security and Encryption

### 4.1 Encryption Implementation

```java
// Encryption Configuration
@Configuration
public class EncryptionConfiguration {
    
    @Bean
    public TextEncryptor textEncryptor(
            @Value("${encrypt.key}") String encryptKey,
            @Value("${encrypt.salt:deadbeef}") String salt) {
        
        return Encryptors.text(encryptKey, salt);
    }
    
    @Bean
    public RsaSecretEncryptor rsaSecretEncryptor() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        Resource resource = new ClassPathResource("server.jks");
        keyStore.load(resource.getInputStream(), "password".toCharArray());
        
        KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) 
            keyStore.getEntry("config-server-key", 
                new KeyStore.PasswordProtection("password".toCharArray()));
        
        return new RsaSecretEncryptor(
            new KeyPair(pkEntry.getCertificate().getPublicKey(), pkEntry.getPrivateKey())
        );
    }
}

// Encrypted Properties Handler
@Component
public class EncryptedPropertiesHandler {
    
    @Autowired
    private TextEncryptor textEncryptor;
    
    public String encryptProperty(String plainText) {
        return "{cipher}" + textEncryptor.encrypt(plainText);
    }
    
    public String decryptProperty(String encryptedText) {
        if (encryptedText.startsWith("{cipher}")) {
            String cipherText = encryptedText.substring(8);
            return textEncryptor.decrypt(cipherText);
        }
        return encryptedText;
    }
    
    @PostConstruct
    public void demonstrateEncryption() {
        String dbPassword = "mySecretPassword";
        String encrypted = encryptProperty(dbPassword);
        System.out.println("Encrypted password: " + encrypted);
        
        String decrypted = decryptProperty(encrypted);
        System.out.println("Decrypted password: " + decrypted);
    }
}

// Secure Configuration Example
@Configuration
@ConfigurationProperties(prefix = "secure")
public class SecureConfiguration {
    
    // These will be automatically decrypted by Spring Cloud Config
    @Value("${secure.database.password}")
    private String databasePassword;
    
    @Value("${secure.api.key}")
    private String apiKey;
    
    @Value("${secure.jwt.secret}")
    private String jwtSecret;
    
    // Custom decryption for complex scenarios
    @Bean
    public DataSource dataSource(TextEncryptor encryptor) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
        config.setUsername("dbuser");
        
        // Decrypt password if encrypted
        String password = databasePassword;
        if (password.startsWith("{cipher}")) {
            password = encryptor.decrypt(password.substring(8));
        }
        config.setPassword(password);
        
        return new HikariDataSource(config);
    }
}

// Vault Integration for Secrets
@Configuration
@EnableConfigurationProperties(VaultProperties.class)
public class VaultConfiguration {
    
    @Bean
    public VaultTemplate vaultTemplate(VaultProperties properties) {
        VaultEndpoint endpoint = VaultEndpoint.create(properties.getHost(), properties.getPort());
        endpoint.setScheme("https");
        
        ClientAuthentication authentication = new TokenAuthentication(properties.getToken());
        
        return new VaultTemplate(endpoint, authentication);
    }
    
    @Bean
    public PropertySource<?> vaultPropertySource(VaultTemplate vaultTemplate) {
        return new VaultPropertySource(vaultTemplate, "secret/application");
    }
}

@Data
@ConfigurationProperties(prefix = "vault")
public class VaultProperties {
    private String host = "localhost";
    private int port = 8200;
    private String token;
    private String backend = "secret";
    private Duration leaseDuration = Duration.ofHours(1);
}
```

### 4.2 Configuration Access Control

```java
// Config Server Security Configuration
@Configuration
@EnableWebSecurity
public class ConfigServerSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/encrypt", "/decrypt").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults())
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/encrypt", "/decrypt", "/webhook/**")
            );
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("USER", "ADMIN")
            .build();
        
        return new InMemoryUserDetailsManager(user, admin);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// Client-Side Security for Config Access
@Component
public class ConfigClientSecurityInterceptor implements ClientHttpRequestInterceptor {
    
    @Value("${spring.cloud.config.username}")
    private String username;
    
    @Value("${spring.cloud.config.password}")
    private String password;
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                      ClientHttpRequestExecution execution) throws IOException {
        
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        
        request.getHeaders().add("Authorization", authHeader);
        
        return execution.execute(request, body);
    }
}

// Role-Based Configuration Access
@Component
public class RoleBasedConfigurationFilter {
    
    @Autowired
    private ApplicationProperties properties;
    
    public Map<String, Object> getFilteredConfiguration(Authentication authentication) {
        Map<String, Object> config = new HashMap<>();
        
        // All users can see basic configuration
        config.put("appName", properties.getName());
        config.put("version", properties.getVersion());
        
        // Only admins can see sensitive configuration
        if (hasRole(authentication, "ADMIN")) {
            config.put("database", properties.getDatabase());
            config.put("security", properties.getSecurity());
        }
        
        // Developers can see feature flags
        if (hasRole(authentication, "DEVELOPER") || hasRole(authentication, "ADMIN")) {
            config.put("features", properties.getFeatures());
        }
        
        return config;
    }
    
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
```

---

## 5. Advanced Configuration Patterns

### 5.1 Multi-Environment Configuration

```java
// Environment-Specific Configuration Structure
@Component
public class MultiEnvironmentConfig {
    
    /*
     * Configuration Repository Structure:
     * 
     * config-repo/
     * ├── application.yml                    # Common configuration
     * ├── application-dev.yml               # Development overrides
     * ├── application-qa.yml                # QA overrides
     * ├── application-prod.yml              # Production overrides
     * ├── user-service/
     * │   ├── user-service.yml              # Service-specific common
     * │   ├── user-service-dev.yml         # Service + environment specific
     * │   ├── user-service-qa.yml
     * │   └── user-service-prod.yml
     * ├── order-service/
     * │   ├── order-service.yml
     * │   ├── order-service-dev.yml
     * │   ├── order-service-qa.yml
     * │   └── order-service-prod.yml
     * └── secrets/
     *     ├── application-dev.yml          # Environment-specific secrets
     *     ├── application-qa.yml
     *     └── application-prod.yml
     */
}

// Composite Configuration
@Configuration
public class CompositeConfigurationExample {
    
    @Bean
    @ConfigurationProperties(prefix = "composite")
    public CompositeConfiguration compositeConfiguration() {
        return new CompositeConfiguration();
    }
}

@Data
public class CompositeConfiguration {
    
    // From application.yml
    private CommonConfig common = new CommonConfig();
    
    // From application-{profile}.yml
    private EnvironmentConfig environment = new EnvironmentConfig();
    
    // From {application}.yml
    private ServiceConfig service = new ServiceConfig();
    
    // From external sources
    private ExternalConfig external = new ExternalConfig();
    
    @Data
    public static class CommonConfig {
        private String organizationName;
        private String supportEmail;
        private List<String> sharedFeatures;
    }
    
    @Data
    public static class EnvironmentConfig {
        private String environmentName;
        private String dataCenter;
        private boolean debugEnabled;
        private LogLevel logLevel = LogLevel.INFO;
    }
    
    @Data
    public static class ServiceConfig {
        private String serviceName;
        private String serviceVersion;
        private int instanceId;
        private ServiceEndpoints endpoints = new ServiceEndpoints();
    }
    
    @Data
    public static class ExternalConfig {
        private Map<String, String> externalApis = new HashMap<>();
        private Map<String, String> partnerEndpoints = new HashMap<>();
    }
    
    @Data
    public static class ServiceEndpoints {
        private String health = "/health";
        private String metrics = "/metrics";
        private String info = "/info";
    }
    
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}

// Configuration Profiles Manager
@Component
public class ConfigurationProfilesManager {
    
    @Autowired
    private Environment environment;
    
    public ConfigurationProfile getCurrentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        
        ConfigurationProfile profile = new ConfigurationProfile();
        profile.setActiveProfiles(Arrays.asList(activeProfiles));
        profile.setDefaultProfiles(Arrays.asList(environment.getDefaultProfiles()));
        
        // Determine environment type
        if (Arrays.asList(activeProfiles).contains("prod")) {
            profile.setEnvironmentType(EnvironmentType.PRODUCTION);
            profile.setStrictMode(true);
        } else if (Arrays.asList(activeProfiles).contains("qa")) {
            profile.setEnvironmentType(EnvironmentType.QA);
            profile.setStrictMode(true);
        } else {
            profile.setEnvironmentType(EnvironmentType.DEVELOPMENT);
            profile.setStrictMode(false);
        }
        
        return profile;
    }
    
    @Data
    public static class ConfigurationProfile {
        private List<String> activeProfiles;
        private List<String> defaultProfiles;
        private EnvironmentType environmentType;
        private boolean strictMode;
    }
    
    public enum EnvironmentType {
        DEVELOPMENT, QA, STAGING, PRODUCTION
    }
}
```

### 5.2 Configuration Versioning and Rollback

```java
// Configuration Version Control
@Component
public class ConfigurationVersionManager {
    
    @Autowired
    private GitOperations gitOperations;
    
    @Autowired
    private ConfigurationHistoryRepository historyRepository;
    
    public ConfigurationVersion getCurrentVersion(String application, String profile) {
        String configPath = buildConfigPath(application, profile);
        
        GitCommit latestCommit = gitOperations.getLatestCommit(configPath);
        
        ConfigurationVersion version = new ConfigurationVersion();
        version.setVersion(latestCommit.getHash());
        version.setAuthor(latestCommit.getAuthor());
        version.setTimestamp(latestCommit.getTimestamp());
        version.setMessage(latestCommit.getMessage());
        version.setApplication(application);
        version.setProfile(profile);
        
        return version;
    }
    
    public List<ConfigurationVersion> getVersionHistory(String application, String profile, int limit) {
        String configPath = buildConfigPath(application, profile);
        
        return gitOperations.getCommitHistory(configPath, limit).stream()
                .map(commit -> {
                    ConfigurationVersion version = new ConfigurationVersion();
                    version.setVersion(commit.getHash());
                    version.setAuthor(commit.getAuthor());
                    version.setTimestamp(commit.getTimestamp());
                    version.setMessage(commit.getMessage());
                    version.setApplication(application);
                    version.setProfile(profile);
                    return version;
                })
                .collect(Collectors.toList());
    }
    
    public void rollbackConfiguration(String application, String profile, String targetVersion) {
        String configPath = buildConfigPath(application, profile);
        
        // Create rollback commit
        String rollbackMessage = String.format(
            "Rollback %s-%s configuration to version %s",
            application, profile, targetVersion
        );
        
        gitOperations.revertToCommit(configPath, targetVersion, rollbackMessage);
        
        // Record rollback in history
        ConfigurationHistory history = new ConfigurationHistory();
        history.setAction(ConfigurationAction.ROLLBACK);
        history.setApplication(application);
        history.setProfile(profile);
        history.setFromVersion(getCurrentVersion(application, profile).getVersion());
        history.setToVersion(targetVersion);
        history.setPerformedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        history.setTimestamp(LocalDateTime.now());
        
        historyRepository.save(history);
    }
    
    private String buildConfigPath(String application, String profile) {
        if ("default".equals(profile)) {
            return application + "/" + application + ".yml";
        }
        return application + "/" + application + "-" + profile + ".yml";
    }
}

@Data
public class ConfigurationVersion {
    private String version;
    private String author;
    private LocalDateTime timestamp;
    private String message;
    private String application;
    private String profile;
}

@Entity
@Data
public class ConfigurationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private ConfigurationAction action;
    
    private String application;
    private String profile;
    private String fromVersion;
    private String toVersion;
    private String performedBy;
    private LocalDateTime timestamp;
    private String details;
}

public enum ConfigurationAction {
    CREATE, UPDATE, DELETE, ROLLBACK, MERGE
}

// Configuration Diff Service
@Service
public class ConfigurationDiffService {
    
    @Autowired
    private GitOperations gitOperations;
    
    public ConfigurationDiff compareVersions(String application, String profile, 
                                           String version1, String version2) {
        String configPath = buildConfigPath(application, profile);
        
        String content1 = gitOperations.getFileContent(configPath, version1);
        String content2 = gitOperations.getFileContent(configPath, version2);
        
        Map<String, Object> properties1 = parseYaml(content1);
        Map<String, Object> properties2 = parseYaml(content2);
        
        ConfigurationDiff diff = new ConfigurationDiff();
        diff.setApplication(application);
        diff.setProfile(profile);
        diff.setFromVersion(version1);
        diff.setToVersion(version2);
        
        // Find added properties
        properties2.forEach((key, value) -> {
            if (!properties1.containsKey(key)) {
                diff.getAdded().put(key, value);
            }
        });
        
        // Find removed properties
        properties1.forEach((key, value) -> {
            if (!properties2.containsKey(key)) {
                diff.getRemoved().put(key, value);
            }
        });
        
        // Find modified properties
        properties1.forEach((key, value1) -> {
            if (properties2.containsKey(key)) {
                Object value2 = properties2.get(key);
                if (!Objects.equals(value1, value2)) {
                    diff.getModified().put(key, new PropertyChange(value1, value2));
                }
            }
        });
        
        return diff;
    }
    
    private Map<String, Object> parseYaml(String content) {
        Yaml yaml = new Yaml();
        return yaml.load(content);
    }
    
    private String buildConfigPath(String application, String profile) {
        return application + "/" + application + "-" + profile + ".yml";
    }
}

@Data
public class ConfigurationDiff {
    private String application;
    private String profile;
    private String fromVersion;
    private String toVersion;
    private Map<String, Object> added = new HashMap<>();
    private Map<String, Object> removed = new HashMap<>();
    private Map<String, PropertyChange> modified = new HashMap<>();
}

@Data
@AllArgsConstructor
public class PropertyChange {
    private Object oldValue;
    private Object newValue;
}
```

---

## 6. Config Server High Availability

### 6.1 Config Server Clustering

```java
// Config Server HA Setup
@Configuration
@Profile("ha")
public class ConfigServerHAConfiguration {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    @Bean
    public ConfigurationCache configurationCache(RedisTemplate<String, Object> redisTemplate) {
        return new RedisConfigurationCache(redisTemplate);
    }
}

// Configuration Cache Implementation
@Component
public class RedisConfigurationCache implements ConfigurationCache {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration cacheTtl = Duration.ofMinutes(5);
    
    public RedisConfigurationCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Optional<Environment> get(String application, String profile, String label) {
        String key = buildKey(application, profile, label);
        Environment cached = (Environment) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(cached);
    }
    
    @Override
    public void put(String application, String profile, String label, Environment environment) {
        String key = buildKey(application, profile, label);
        redisTemplate.opsForValue().set(key, environment, cacheTtl);
    }
    
    @Override
    public void evict(String application, String profile, String label) {
        String key = buildKey(application, profile, label);
        redisTemplate.delete(key);
    }
    
    @Override
    public void evictAll() {
        Set<String> keys = redisTemplate.keys("config:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    
    private String buildKey(String application, String profile, String label) {
        return String.format("config:%s:%s:%s", application, profile, label);
    }
}

// Load Balancer Configuration for Config Clients
@Configuration
public class ConfigClientLoadBalancerConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public ServiceInstanceListSupplier configServerInstanceSupplier() {
        return ServiceInstanceListSupplier.builder()
                .withDiscoveryClient()
                .withHealthChecks()
                .build();
    }
}

// Health Check for Config Server
@Component
public class ConfigServerHealthIndicator implements HealthIndicator {
    
    @Autowired
    private GitOperations gitOperations;
    
    @Autowired
    private ConfigurationCache cache;
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        
        // Check Git repository connectivity
        boolean gitHealthy = checkGitHealth();
        details.put("git", gitHealthy ? "UP" : "DOWN");
        
        // Check cache connectivity
        boolean cacheHealthy = checkCacheHealth();
        details.put("cache", cacheHealthy ? "UP" : "DOWN");
        
        // Overall health
        if (gitHealthy && cacheHealthy) {
            return Health.up().withDetails(details).build();
        } else if (gitHealthy) {
            // Can still serve from Git even if cache is down
            return Health.up()
                    .withDetail("status", "DEGRADED")
                    .withDetails(details)
                    .build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
    
    private boolean checkGitHealth() {
        try {
            gitOperations.testConnection();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkCacheHealth() {
        try {
            cache.get("test", "test", "test");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 7. Monitoring and Management

### 7.1 Config Server Metrics

```java
// Metrics Configuration
@Configuration
public class ConfigServerMetricsConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public ConfigServerMetrics configServerMetrics(MeterRegistry meterRegistry) {
        return new ConfigServerMetrics(meterRegistry);
    }
}

@Component
public class ConfigServerMetrics {
    
    private final Counter configRequestCounter;
    private final Timer configRequestTimer;
    private final Gauge activeConnectionsGauge;
    private final Counter configUpdateCounter;
    
    public ConfigServerMetrics(MeterRegistry meterRegistry) {
        this.configRequestCounter = Counter.builder("config.requests")
                .description("Number of configuration requests")
                .register(meterRegistry);
        
        this.configRequestTimer = Timer.builder("config.request.duration")
                .description("Configuration request duration")
                .register(meterRegistry);
        
        this.activeConnectionsGauge = Gauge.builder("config.connections.active", this, 
                ConfigServerMetrics::getActiveConnections)
                .description("Number of active connections")
                .register(meterRegistry);
        
        this.configUpdateCounter = Counter.builder("config.updates")
                .description("Number of configuration updates")
                .register(meterRegistry);
    }
    
    public void recordConfigRequest(String application, String profile, boolean success) {
        configRequestCounter.increment(Tags.of(
                "application", application,
                "profile", profile,
                "success", String.valueOf(success)
        ));
    }
    
    public Timer.Sample startTimer() {
        return Timer.start();
    }
    
    public void recordRequestDuration(Timer.Sample sample, String application, String profile) {
        sample.stop(Timer.builder("config.request.duration")
                .tag("application", application)
                .tag("profile", profile)
                .register(configRequestTimer.getMeter().getId().getMeterRegistry()));
    }
    
    public void recordConfigUpdate(String application, String profile) {
        configUpdateCounter.increment(Tags.of(
                "application", application,
                "profile", profile
        ));
    }
    
    private double getActiveConnections() {
        // Implementation to get active connection count
        return 0.0;
    }
}

// Audit Logging
@Component
public class ConfigServerAuditLogger {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("audit");
    
    @EventListener
    public void handleConfigRequest(ConfigRequestEvent event) {
        AuditEntry entry = new AuditEntry();
        entry.setEventType("CONFIG_REQUEST");
        entry.setApplication(event.getApplication());
        entry.setProfile(event.getProfile());
        entry.setLabel(event.getLabel());
        entry.setClientIp(event.getClientIp());
        entry.setTimestamp(event.getTimestamp());
        entry.setSuccess(event.isSuccess());
        
        auditLogger.info(entry.toJson());
    }
    
    @EventListener
    public void handleConfigUpdate(ConfigUpdateEvent event) {
        AuditEntry entry = new AuditEntry();
        entry.setEventType("CONFIG_UPDATE");
        entry.setApplication(event.getApplication());
        entry.setProfile(event.getProfile());
        entry.setChangedBy(event.getChangedBy());
        entry.setChangedProperties(event.getChangedProperties());
        entry.setTimestamp(event.getTimestamp());
        
        auditLogger.info(entry.toJson());
    }
}

@Data
public class AuditEntry {
    private String eventType;
    private String application;
    private String profile;
    private String label;
    private String clientIp;
    private LocalDateTime timestamp;
    private boolean success;
    private String changedBy;
    private List<String> changedProperties;
    
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return toString();
        }
    }
}
```

---

## 8. Best Practices and Common Patterns

### 8.1 Configuration Best Practices

```java
// Configuration Validation
@Component
public class ConfigurationValidator {
    
    @Autowired
    private ApplicationProperties properties;
    
    @PostConstruct
    public void validateConfiguration() {
        List<String> errors = new ArrayList<>();
        
        // Validate required properties
        if (properties.getDatabase().getUrl() == null) {
            errors.add("Database URL is required");
        }
        
        if (properties.getSecurity().isEnabled() && 
            properties.getSecurity().getJwtSecret() == null) {
            errors.add("JWT secret is required when security is enabled");
        }
        
        // Validate property formats
        if (!isValidUrl(properties.getDatabase().getUrl())) {
            errors.add("Invalid database URL format");
        }
        
        // Validate property relationships
        if (properties.getCache().isEnabled() && 
            properties.getCache().getMaxSize() < 100) {
            errors.add("Cache size should be at least 100 when enabled");
        }
        
        if (!errors.isEmpty()) {
            throw new ConfigurationException("Configuration validation failed: " + 
                    String.join(", ", errors));
        }
    }
    
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}

// Feature Flags Management
@Component
@RefreshScope
public class FeatureFlagManager {
    
    @Value("#{${feature.flags}}")
    private Map<String, Boolean> featureFlags;
    
    public boolean isFeatureEnabled(String feature) {
        return featureFlags.getOrDefault(feature, false);
    }
    
    public Map<String, Boolean> getAllFeatures() {
        return new HashMap<>(featureFlags);
    }
    
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh() {
        logger.info("Feature flags refreshed: {}", featureFlags);
    }
}

// Configuration Templates
@Component
public class ConfigurationTemplateProcessor {
    
    @Autowired
    private Environment environment;
    
    public String processTemplate(String template) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = environment.getProperty(placeholder, "");
            matcher.appendReplacement(result, value);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    public Map<String, Object> processConfigurationMap(Map<String, Object> config) {
        Map<String, Object> processed = new HashMap<>();
        
        config.forEach((key, value) -> {
            if (value instanceof String) {
                processed.put(key, processTemplate((String) value));
            } else if (value instanceof Map) {
                processed.put(key, processConfigurationMap((Map<String, Object>) value));
            } else {
                processed.put(key, value);
            }
        });
        
        return processed;
    }
}
```

---

## 9. Summary and Key Takeaways

### Key Concepts Mastered

1. **Centralized Configuration Management**
   - Config Server setup and configuration
   - Multiple backend support (Git, File, Vault)
   - Environment-specific configurations

2. **Dynamic Configuration**
   - Runtime configuration refresh
   - Configuration change notifications
   - Feature flag management

3. **Security and Encryption**
   - Property encryption/decryption
   - Access control and authentication
   - Secure secret management

4. **High Availability**
   - Config Server clustering
   - Caching strategies
   - Health monitoring

5. **Best Practices**
   - Configuration versioning
   - Audit logging
   - Configuration validation

### Common Pitfalls to Avoid

❌ **Storing secrets in plain text**  
❌ **Not implementing proper access control**  
❌ **Missing configuration validation**  
❌ **Ignoring configuration versioning**  
❌ **Not planning for high availability**  

### Next Steps

Tomorrow, we'll explore Spring Cloud Netflix components including Eureka for service discovery, Hystrix for circuit breakers, and Zuul for API gateway functionality.

---

**Practice Assignment**: Set up a Config Server with Git backend, implement encryption for sensitive properties, and create a client application that refreshes its configuration dynamically.

---

*"Centralized configuration is not just about convenience - it's about maintaining consistency, security, and agility across your entire microservices ecosystem."*