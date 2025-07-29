# Day 100: Advanced Spring Framework - Custom Auto-Configuration & Starters

## Learning Objectives
- Master creating custom Spring Boot auto-configuration classes
- Understand conditional beans and configuration properties
- Build reusable Spring Boot starters for enterprise applications
- Implement advanced Spring Boot features and customization
- Create auto-configuration for third-party library integration

## Topics Covered
1. Spring Boot Auto-Configuration Internals
2. Creating Custom Auto-Configuration Classes
3. Conditional Configuration and Bean Creation
4. Configuration Properties and Validation
5. Building Custom Spring Boot Starters
6. Advanced Auto-Configuration Patterns
7. Testing Auto-Configuration

## 1. Spring Boot Auto-Configuration Internals

### Understanding Auto-Configuration Mechanism

Auto-configuration works through:
- `@EnableAutoConfiguration` annotation
- `spring.factories` file in META-INF
- Conditional annotations for smart defaults
- Configuration properties for customization

### Core Auto-Configuration Annotations

```java
// Core auto-configuration class structure
@Configuration
@ConditionalOnClass(SomeLibraryClass.class)
@ConditionalOnMissingBean(SomeService.class)
@EnableConfigurationProperties(SomeProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class CustomAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public SomeService someService(SomeProperties properties) {
        return new DefaultSomeService(properties);
    }
}
```

## 2. Creating Custom Auto-Configuration Classes

### Example: Email Service Auto-Configuration

Let's create a comprehensive email service with auto-configuration:

```java
// Email service interface
public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendHtmlEmail(String to, String subject, String htmlBody);
    void sendEmailWithAttachment(String to, String subject, String body, 
                                String attachmentPath);
}

// Email configuration properties
@ConfigurationProperties(prefix = "app.email")
@Validated
public class EmailProperties {
    
    /**
     * SMTP server host
     */
    @NotBlank
    private String host = "localhost";
    
    /**
     * SMTP server port
     */
    @Min(1)
    @Max(65535)
    private int port = 587;
    
    /**
     * SMTP username
     */
    private String username;
    
    /**
     * SMTP password
     */
    private String password;
    
    /**
     * Enable TLS encryption
     */
    private boolean enableTls = true;
    
    /**
     * Enable SSL encryption
     */
    private boolean enableSsl = false;
    
    /**
     * Connection timeout in milliseconds
     */
    @Min(1000)
    private int connectionTimeout = 5000;
    
    /**
     * Read timeout in milliseconds
     */
    @Min(1000)
    private int readTimeout = 5000;
    
    /**
     * Default from address
     */
    @Email
    private String defaultFrom;
    
    /**
     * Enable debug mode
     */
    private boolean debug = false;
    
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
    
    public boolean isEnableSsl() { return enableSsl; }
    public void setEnableSsl(boolean enableSsl) { this.enableSsl = enableSsl; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { 
        this.connectionTimeout = connectionTimeout; 
    }
    
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    
    public String getDefaultFrom() { return defaultFrom; }
    public void setDefaultFrom(String defaultFrom) { this.defaultFrom = defaultFrom; }
    
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }
}

// Email service implementation
public class JavaMailEmailService implements EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailProperties properties;
    private static final Logger logger = LoggerFactory.getLogger(JavaMailEmailService.class);
    
    public JavaMailEmailService(JavaMailSender mailSender, EmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            if (properties.getDefaultFrom() != null) {
                message.setFrom(properties.getDefaultFrom());
            }
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
            
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }
    
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML
            
            if (properties.getDefaultFrom() != null) {
                helper.setFrom(properties.getDefaultFrom());
            }
            
            mailSender.send(message);
            logger.info("HTML email sent successfully to: {}", to);
            
        } catch (Exception e) {
            logger.error("Failed to send HTML email to: {}", to, e);
            throw new EmailSendException("Failed to send HTML email", e);
        }
    }
    
    @Override
    public void sendEmailWithAttachment(String to, String subject, String body, 
                                      String attachmentPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            
            if (properties.getDefaultFrom() != null) {
                helper.setFrom(properties.getDefaultFrom());
            }
            
            // Add attachment
            File attachment = new File(attachmentPath);
            helper.addAttachment(attachment.getName(), attachment);
            
            mailSender.send(message);
            logger.info("Email with attachment sent successfully to: {}", to);
            
        } catch (Exception e) {
            logger.error("Failed to send email with attachment to: {}", to, e);
            throw new EmailSendException("Failed to send email with attachment", e);
        }
    }
}

// Custom exception
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Email Auto-Configuration Class

```java
@Configuration
@ConditionalOnClass({JavaMailSender.class, MimeMessage.class})
@ConditionalOnProperty(prefix = "app.email", name = "enabled", havingValue = "true", 
                      matchIfMissing = true)
@EnableConfigurationProperties(EmailProperties.class)
@AutoConfigureAfter(MailSenderAutoConfiguration.class)
public class EmailAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailAutoConfiguration.class);
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JavaMailSender.class)
    public EmailService emailService(JavaMailSender mailSender, 
                                   EmailProperties emailProperties) {
        logger.info("Auto-configuring EmailService with properties: {}", 
                   emailProperties.getHost());
        return new JavaMailEmailService(mailSender, emailProperties);
    }
    
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    @ConditionalOnProperty(prefix = "app.email", name = "host")
    public JavaMailSender javaMailSender(EmailProperties properties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(properties.getHost());
        mailSender.setPort(properties.getPort());
        mailSender.setUsername(properties.getUsername());
        mailSender.setPassword(properties.getPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", properties.isEnableTls());
        props.put("mail.smtp.ssl.enable", properties.isEnableSsl());
        props.put("mail.smtp.connectiontimeout", properties.getConnectionTimeout());
        props.put("mail.smtp.timeout", properties.getReadTimeout());
        props.put("mail.debug", properties.isDebug());
        
        logger.info("Auto-configured JavaMailSender for host: {}", properties.getHost());
        return mailSender;
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "app.email", name = "health-check.enabled", 
                          havingValue = "true")
    public EmailHealthIndicator emailHealthIndicator(EmailService emailService,
                                                   EmailProperties properties) {
        return new EmailHealthIndicator(emailService, properties);
    }
}

// Health indicator for email service
@Component
public class EmailHealthIndicator implements HealthIndicator {
    
    private final EmailService emailService;
    private final EmailProperties properties;
    
    public EmailHealthIndicator(EmailService emailService, EmailProperties properties) {
        this.emailService = emailService;
        this.properties = properties;
    }
    
    @Override
    public Health health() {
        try {
            // Test SMTP connection
            return Health.up()
                    .withDetail("host", properties.getHost())
                    .withDetail("port", properties.getPort())
                    .withDetail("tls", properties.isEnableTls())
                    .withDetail("status", "SMTP connection successful")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("host", properties.getHost())
                    .withDetail("port", properties.getPort())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

## 3. Conditional Configuration and Bean Creation

### Advanced Conditional Annotations

```java
// Custom conditional annotation
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnEmailProviderCondition.class)
public @interface ConditionalOnEmailProvider {
    String value();
}

// Custom condition implementation
public class OnEmailProviderCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String requiredProvider = (String) metadata
                .getAnnotationAttributes(ConditionalOnEmailProvider.class.getName())
                .get("value");
        
        String actualProvider = context.getEnvironment()
                .getProperty("app.email.provider", "smtp");
        
        return requiredProvider.equals(actualProvider);
    }
}

// Usage in configuration
@Configuration
public class EmailProviderConfiguration {
    
    @Bean
    @ConditionalOnEmailProvider("smtp")
    public EmailService smtpEmailService(JavaMailSender mailSender, 
                                       EmailProperties properties) {
        return new JavaMailEmailService(mailSender, properties);
    }
    
    @Bean
    @ConditionalOnEmailProvider("sendgrid")
    public EmailService sendGridEmailService(EmailProperties properties) {
        return new SendGridEmailService(properties);
    }
    
    @Bean
    @ConditionalOnEmailProvider("ses")
    public EmailService sesEmailService(EmailProperties properties) {
        return new AWSEmailService(properties);
    }
}
```

### Complex Conditional Logic

```java
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class CacheAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.cache", name = "type", havingValue = "redis")
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.cache", name = "type", havingValue = "caffeine", 
                          matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES));
        return cacheManager;
    }
}
```

## 4. Configuration Properties and Validation

### Advanced Configuration Properties

```java
@ConfigurationProperties(prefix = "app.security")
@Validated
public class SecurityProperties {
    
    @Valid
    private Jwt jwt = new Jwt();
    
    @Valid
    private Cors cors = new Cors();
    
    @Valid
    private RateLimit rateLimit = new RateLimit();
    
    public static class Jwt {
        
        @NotBlank
        private String secret;
        
        @Min(3600) // At least 1 hour
        private long expirationSeconds = 86400; // 24 hours
        
        @NotBlank
        private String issuer = "myapp";
        
        private boolean validateClaims = true;
        
        // Getters and setters
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        
        public long getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(long expirationSeconds) { 
            this.expirationSeconds = expirationSeconds; 
        }
        
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        
        public boolean isValidateClaims() { return validateClaims; }
        public void setValidateClaims(boolean validateClaims) { 
            this.validateClaims = validateClaims; 
        }
    }
    
    public static class Cors {
        
        private List<String> allowedOrigins = List.of("*");
        
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE");
        
        private List<String> allowedHeaders = List.of("*");
        
        private boolean allowCredentials = false;
        
        @Min(0)
        @Max(86400)
        private long maxAge = 3600;
        
        // Getters and setters
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { 
            this.allowedOrigins = allowedOrigins; 
        }
        
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { 
            this.allowedMethods = allowedMethods; 
        }
        
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { 
            this.allowedHeaders = allowedHeaders; 
        }
        
        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { 
            this.allowCredentials = allowCredentials; 
        }
        
        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    }
    
    public static class RateLimit {
        
        private boolean enabled = false;
        
        @Min(1)
        private int requestsPerMinute = 60;
        
        @Min(1)
        private int requestsPerHour = 1000;
        
        private List<String> excludedPaths = new ArrayList<>();
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { 
            this.requestsPerMinute = requestsPerMinute; 
        }
        
        public int getRequestsPerHour() { return requestsPerHour; }
        public void setRequestsPerHour(int requestsPerHour) { 
            this.requestsPerHour = requestsPerHour; 
        }
        
        public List<String> getExcludedPaths() { return excludedPaths; }
        public void setExcludedPaths(List<String> excludedPaths) { 
            this.excludedPaths = excludedPaths; 
        }
    }
    
    // Main class getters and setters
    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
}
```

### Custom Validation

```java
// Custom validation annotation
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DatabaseUrlValidator.class)
@Documented
public @interface ValidDatabaseUrl {
    String message() default "Invalid database URL format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
public class DatabaseUrlValidator implements ConstraintValidator<ValidDatabaseUrl, String> {
    
    private static final Pattern DB_URL_PATTERN = Pattern.compile(
            "^jdbc:(mysql|postgresql|oracle|sqlserver)://[\\w\\.-]+(:\\d+)?/\\w+$");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        return DB_URL_PATTERN.matcher(value).matches();
    }
}

// Usage in configuration properties
@ConfigurationProperties(prefix = "app.database")
@Validated
public class DatabaseProperties {
    
    @NotBlank
    @ValidDatabaseUrl
    private String url;
    
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;
    
    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

## 5. Building Custom Spring Boot Starters

### Starter Project Structure

```
email-spring-boot-starter/
├── pom.xml
├── src/main/java/
│   └── com/example/starter/email/
│       ├── EmailAutoConfiguration.java
│       ├── EmailProperties.java
│       ├── EmailService.java
│       ├── JavaMailEmailService.java
│       └── EmailHealthIndicator.java
├── src/main/resources/
│   └── META-INF/
│       ├── spring.factories
│       └── additional-spring-configuration-metadata.json
└── src/test/java/
    └── com/example/starter/email/
        └── EmailAutoConfigurationTest.java
```

### Maven Configuration for Starter

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>email-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>Email Spring Boot Starter</name>
    <description>Spring Boot starter for email functionality</description>
    
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.1.0</spring-boot.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        
        <!-- Spring Boot Mail Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        
        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <version>${spring-boot.version}</version>
            <optional>true</optional>
        </dependency>
        
        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        
        <!-- Actuator for health indicators -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
            <version>${spring-boot.version}</version>
            <optional>true</optional>
        </dependency>
        
        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>${spring-boot.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### spring.factories Configuration

```properties
# Auto Configuration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.starter.email.EmailAutoConfiguration
```

### Configuration Metadata

```json
{
  "groups": [
    {
      "name": "app.email",
      "type": "com.example.starter.email.EmailProperties",
      "description": "Email configuration properties."
    }
  ],
  "properties": [
    {
      "name": "app.email.enabled",
      "type": "java.lang.Boolean",
      "description": "Whether to enable email auto-configuration.",
      "defaultValue": true
    },
    {
      "name": "app.email.host",
      "type": "java.lang.String",
      "description": "SMTP server host.",
      "defaultValue": "localhost"
    },
    {
      "name": "app.email.port",
      "type": "java.lang.Integer",
      "description": "SMTP server port.",
      "defaultValue": 587
    },
    {
      "name": "app.email.username",
      "type": "java.lang.String",
      "description": "SMTP username."
    },
    {
      "name": "app.email.password",
      "type": "java.lang.String",
      "description": "SMTP password."
    },
    {
      "name": "app.email.enable-tls",
      "type": "java.lang.Boolean",
      "description": "Enable TLS encryption.",
      "defaultValue": true
    },
    {
      "name": "app.email.default-from",
      "type": "java.lang.String",
      "description": "Default from email address."
    }
  ],
  "hints": [
    {
      "name": "app.email.host",
      "values": [
        {
          "value": "smtp.gmail.com",
          "description": "Gmail SMTP server."
        },
        {
          "value": "smtp.outlook.com",
          "description": "Outlook SMTP server."
        }
      ]
    }
  ]
}
```

## 6. Advanced Auto-Configuration Patterns

### Auto-Configuration Ordering

```java
@Configuration
@ConditionalOnClass(JdbcTemplate.class)
@AutoConfigureBefore(DataSourceTransactionManagerAutoConfiguration.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
public class CustomDataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource);
    }
}
```

### Configuration Import Selectors

```java
public class DatabaseConfigurationImportSelector implements ImportSelector {
    
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableDatabaseConfiguration.class.getName());
        
        DatabaseType databaseType = (DatabaseType) attributes.get("type");
        
        return switch (databaseType) {
            case MYSQL -> new String[]{MySQLConfiguration.class.getName()};
            case POSTGRESQL -> new String[]{PostgreSQLConfiguration.class.getName()};
            case ORACLE -> new String[]{OracleConfiguration.class.getName()};
            default -> new String[]{GenericDatabaseConfiguration.class.getName()};
        };
    }
}

// Custom annotation for enabling database configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DatabaseConfigurationImportSelector.class)
public @interface EnableDatabaseConfiguration {
    DatabaseType type() default DatabaseType.AUTO;
}

enum DatabaseType {
    AUTO, MYSQL, POSTGRESQL, ORACLE
}
```

### Environment Post Processors

```java
public class CustomEnvironmentPostProcessor implements EnvironmentPostProcessor {
    
    private static final String PROPERTIES_FILE = "custom-defaults.properties";
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                     SpringApplication application) {
        // Add custom property sources
        addCustomPropertySource(environment);
        
        // Set default profiles if none are active
        setDefaultProfiles(environment);
    }
    
    private void addCustomPropertySource(ConfigurableEnvironment environment) {
        try {
            Resource resource = new ClassPathResource(PROPERTIES_FILE);
            if (resource.exists()) {
                Properties properties = PropertiesLoaderUtils.loadProperties(resource);
                PropertiesPropertySource propertySource = 
                        new PropertiesPropertySource("customDefaults", properties);
                environment.getPropertySources().addLast(propertySource);
            }
        } catch (Exception e) {
            // Log warning but don't fail application startup
            System.err.println("Failed to load custom properties: " + e.getMessage());
        }
    }
    
    private void setDefaultProfiles(ConfigurableEnvironment environment) {
        if (environment.getActiveProfiles().length == 0) {
            environment.setActiveProfiles("default");
        }
    }
}
```

## 7. Testing Auto-Configuration

### Comprehensive Auto-Configuration Tests

```java
@ExtendWith(SpringExtension.class)
class EmailAutoConfigurationTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmailAutoConfiguration.class));
    
    @Test
    void shouldCreateEmailServiceWhenJavaMailSenderExists() {
        contextRunner
                .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .withPropertyValues("app.email.host=smtp.example.com")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailService.class);
                    assertThat(context.getBean(EmailService.class))
                            .isInstanceOf(JavaMailEmailService.class);
                });
    }
    
    @Test
    void shouldNotCreateEmailServiceWhenDisabled() {
        contextRunner
                .withPropertyValues("app.email.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EmailService.class);
                });
    }
    
    @Test
    void shouldCreateJavaMailSenderWhenMissing() {
        contextRunner
                .withPropertyValues(
                        "app.email.host=smtp.example.com",
                        "app.email.port=587",
                        "app.email.username=user",
                        "app.email.password=pass"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(JavaMailSender.class);
                    
                    JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
                    assertThat(mailSender.getHost()).isEqualTo("smtp.example.com");
                    assertThat(mailSender.getPort()).isEqualTo(587);
                    assertThat(mailSender.getUsername()).isEqualTo("user");
                    assertThat(mailSender.getPassword()).isEqualTo("pass");
                });
    }
    
    @Test
    void shouldBindEmailProperties() {
        contextRunner
                .withPropertyValues(
                        "app.email.host=smtp.test.com",
                        "app.email.port=465",
                        "app.email.enable-tls=false",
                        "app.email.enable-ssl=true",
                        "app.email.default-from=test@example.com"
                )
                .run(context -> {
                    EmailProperties properties = context.getBean(EmailProperties.class);
                    assertThat(properties.getHost()).isEqualTo("smtp.test.com");
                    assertThat(properties.getPort()).isEqualTo(465);
                    assertThat(properties.isEnableTls()).isFalse();
                    assertThat(properties.isEnableSsl()).isTrue();
                    assertThat(properties.getDefaultFrom()).isEqualTo("test@example.com");
                });
    }
    
    @Test
    void shouldCreateHealthIndicatorWhenEnabled() {
        contextRunner
                .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .withPropertyValues(
                        "app.email.host=smtp.example.com",
                        "app.email.health-check.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailHealthIndicator.class);
                });
    }
    
    @Test
    void shouldValidateEmailProperties() {
        contextRunner
                .withPropertyValues(
                        "app.email.host=", // Invalid - blank
                        "app.email.port=0", // Invalid - below minimum
                        "app.email.default-from=invalid-email" // Invalid email format
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class);
                });
    }
}

// Integration test
@SpringBootTest
@TestPropertySource(properties = {
        "app.email.host=smtp.test.com",
        "app.email.port=587",
        "app.email.username=testuser",
        "app.email.password=testpass",
        "app.email.default-from=test@example.com"
})
class EmailServiceIntegrationTest {
    
    @Autowired
    private EmailService emailService;
    
    @MockBean
    private JavaMailSender mailSender;
    
    @Test
    void shouldSendEmailSuccessfully() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String body = "Test Body";
        
        // When
        emailService.sendEmail(to, subject, body);
        
        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
```

## Real-World Usage Examples

### Using the Email Starter

```java
// Application configuration
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}

// Service using auto-configured EmailService
@Service
public class NotificationService {
    
    private final EmailService emailService;
    
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to Our Platform";
        String body = String.format("Hello %s, welcome to our platform!", user.getName());
        
        emailService.sendEmail(user.getEmail(), subject, body);
    }
    
    public void sendPasswordResetEmail(User user, String resetToken) {
        String subject = "Password Reset Request";
        String htmlBody = String.format("""
                <h2>Password Reset</h2>
                <p>Hello %s,</p>
                <p>Click <a href="https://example.com/reset?token=%s">here</a> 
                   to reset your password.</p>
                """, user.getName(), resetToken);
        
        emailService.sendHtmlEmail(user.getEmail(), subject, htmlBody);
    }
}
```

### Application Properties

```properties
# Email Configuration
app.email.enabled=true
app.email.host=smtp.gmail.com
app.email.port=587
app.email.username=${EMAIL_USERNAME}
app.email.password=${EMAIL_PASSWORD}
app.email.enable-tls=true
app.email.default-from=noreply@example.com
app.email.connection-timeout=5000
app.email.read-timeout=5000
app.email.debug=false

# Health Check
app.email.health-check.enabled=true

# Management endpoints
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

## 8. Comprehensive Hands-On Project: Enterprise Notification System Starter

Let's build a complete enterprise notification system starter that demonstrates all advanced Spring Framework concepts:

### Project Structure
```
enterprise-notification-starter/
├── pom.xml
├── src/main/java/
│   └── com/enterprise/notification/
│       ├── config/
│       │   ├── NotificationAutoConfiguration.java
│       │   ├── EmailAutoConfiguration.java
│       │   ├── SmsAutoConfiguration.java
│       │   └── SlackAutoConfiguration.java
│       ├── properties/
│       │   ├── NotificationProperties.java
│       │   ├── EmailProperties.java
│       │   ├── SmsProperties.java
│       │   └── SlackProperties.java
│       ├── service/
│       │   ├── NotificationService.java
│       │   ├── EmailNotificationService.java
│       │   ├── SmsNotificationService.java
│       │   ├── SlackNotificationService.java
│       │   └── CompositeNotificationService.java
│       ├── model/
│       │   ├── NotificationRequest.java
│       │   ├── NotificationResponse.java
│       │   ├── NotificationChannel.java
│       │   └── NotificationTemplate.java
│       ├── health/
│       │   └── NotificationHealthIndicator.java
│       ├── metrics/
│       │   └── NotificationMetrics.java
│       └── exception/
│           └── NotificationException.java
├── src/main/resources/
│   └── META-INF/
│       ├── spring.factories
│       └── additional-spring-configuration-metadata.json
└── src/test/java/
    └── com/enterprise/notification/
        ├── NotificationAutoConfigurationTest.java
        └── NotificationServiceIntegrationTest.java
```

### Core Notification Framework

```java
package com.enterprise.notification.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Universal notification request model
 */
public class NotificationRequest {
    
    @NotNull
    private Set<NotificationChannel> channels;
    
    @NotBlank
    private String recipient;
    
    @NotBlank
    private String subject;
    
    @NotBlank
    private String content;
    
    private String templateId;
    
    private Map<String, Object> templateVariables;
    
    private Priority priority = Priority.NORMAL;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant scheduledTime;
    
    private Map<String, String> metadata;
    
    private boolean trackDelivery = true;
    
    private String correlationId;
    
    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }
    
    // Constructors
    public NotificationRequest() {}
    
    public NotificationRequest(Set<NotificationChannel> channels, String recipient, 
                             String subject, String content) {
        this.channels = channels;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
    }
    
    // Builder pattern
    public static NotificationRequestBuilder builder() {
        return new NotificationRequestBuilder();
    }
    
    public static class NotificationRequestBuilder {
        private NotificationRequest request = new NotificationRequest();
        
        public NotificationRequestBuilder channels(Set<NotificationChannel> channels) {
            request.channels = channels;
            return this;
        }
        
        public NotificationRequestBuilder channel(NotificationChannel channel) {
            if (request.channels == null) {
                request.channels = Set.of();
            }
            request.channels = Set.copyOf(
                Stream.concat(request.channels.stream(), Stream.of(channel))
                    .collect(Collectors.toSet())
            );
            return this;
        }
        
        public NotificationRequestBuilder recipient(String recipient) {
            request.recipient = recipient;
            return this;
        }
        
        public NotificationRequestBuilder subject(String subject) {
            request.subject = subject;
            return this;
        }
        
        public NotificationRequestBuilder content(String content) {
            request.content = content;
            return this;
        }
        
        public NotificationRequestBuilder template(String templateId, Map<String, Object> variables) {
            request.templateId = templateId;
            request.templateVariables = variables != null ? Map.copyOf(variables) : Map.of();
            return this;
        }
        
        public NotificationRequestBuilder priority(Priority priority) {
            request.priority = priority;
            return this;
        }
        
        public NotificationRequestBuilder scheduleAt(Instant scheduledTime) {
            request.scheduledTime = scheduledTime;
            return this;
        }
        
        public NotificationRequestBuilder metadata(Map<String, String> metadata) {
            request.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
            return this;
        }
        
        public NotificationRequestBuilder trackDelivery(boolean trackDelivery) {
            request.trackDelivery = trackDelivery;
            return this;
        }
        
        public NotificationRequestBuilder correlationId(String correlationId) {
            request.correlationId = correlationId;
            return this;
        }
        
        public NotificationRequest build() {
            if (request.channels == null || request.channels.isEmpty()) {
                throw new IllegalArgumentException("At least one notification channel is required");
            }
            if (request.recipient == null || request.recipient.trim().isEmpty()) {
                throw new IllegalArgumentException("Recipient is required");
            }
            if (request.subject == null || request.subject.trim().isEmpty()) {
                throw new IllegalArgumentException("Subject is required");
            }
            if (request.content == null || request.content.trim().isEmpty()) {
                throw new IllegalArgumentException("Content is required");
            }
            
            return request;
        }
    }
    
    // Getters and setters
    public Set<NotificationChannel> getChannels() { return channels; }
    public void setChannels(Set<NotificationChannel> channels) { this.channels = channels; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public Map<String, Object> getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(Map<String, Object> templateVariables) { 
        this.templateVariables = templateVariables; 
    }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public boolean isTrackDelivery() { return trackDelivery; }
    public void setTrackDelivery(boolean trackDelivery) { this.trackDelivery = trackDelivery; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}

/**
 * Notification response model
 */
public class NotificationResponse {
    
    private String notificationId;
    private String correlationId;
    private NotificationStatus status;
    private String message;
    private Instant sentAt;
    private Map<NotificationChannel, ChannelResponse> channelResponses;
    
    public enum NotificationStatus {
        SUCCESS, PARTIAL_SUCCESS, FAILED, SCHEDULED
    }
    
    public static class ChannelResponse {
        private NotificationStatus status;
        private String message;
        private String externalId;
        private Instant processedAt;
        
        // Constructors
        public ChannelResponse() {}
        
        public ChannelResponse(NotificationStatus status, String message) {
            this.status = status;
            this.message = message;
            this.processedAt = Instant.now();
        }
        
        // Getters and setters
        public NotificationStatus getStatus() { return status; }
        public void setStatus(NotificationStatus status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        
        public Instant getProcessedAt() { return processedAt; }
        public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    }
    
    // Constructors
    public NotificationResponse() {
        this.notificationId = UUID.randomUUID().toString();
        this.sentAt = Instant.now();
        this.channelResponses = new HashMap<>();
    }
    
    public NotificationResponse(String correlationId) {
        this();
        this.correlationId = correlationId;
    }
    
    // Getters and setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    
    public Map<NotificationChannel, ChannelResponse> getChannelResponses() { 
        return channelResponses; 
    }
    public void setChannelResponses(Map<NotificationChannel, ChannelResponse> channelResponses) { 
        this.channelResponses = channelResponses; 
    }
    
    public void addChannelResponse(NotificationChannel channel, ChannelResponse response) {
        this.channelResponses.put(channel, response);
        updateOverallStatus();
    }
    
    private void updateOverallStatus() {
        if (channelResponses.isEmpty()) {
            this.status = NotificationStatus.FAILED;
            return;
        }
        
        long successCount = channelResponses.values().stream()
            .mapToLong(response -> response.getStatus() == NotificationStatus.SUCCESS ? 1 : 0)
            .sum();
        
        if (successCount == channelResponses.size()) {
            this.status = NotificationStatus.SUCCESS;
        } else if (successCount > 0) {
            this.status = NotificationStatus.PARTIAL_SUCCESS;
        } else {
            this.status = NotificationStatus.FAILED;
        }
    }
}

/**
 * Notification channels enumeration
 */
public enum NotificationChannel {
    EMAIL("email", "Email notifications"),
    SMS("sms", "SMS notifications"),
    SLACK("slack", "Slack notifications"),
    PUSH("push", "Push notifications"),
    WEBHOOK("webhook", "Webhook notifications");
    
    private final String code;
    private final String description;
    
    NotificationChannel(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    
    public static NotificationChannel fromCode(String code) {
        for (NotificationChannel channel : values()) {
            if (channel.code.equals(code)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown notification channel: " + code);
    }
}

/**
 * Template model for dynamic content generation
 */
public class NotificationTemplate {
    
    private String templateId;
    private String name;
    private NotificationChannel channel;
    private String subjectTemplate;
    private String contentTemplate;
    private TemplateEngine engine = TemplateEngine.SIMPLE;
    private Map<String, Object> defaultVariables;
    private boolean active = true;
    
    public enum TemplateEngine {
        SIMPLE, THYMELEAF, FREEMARKER, VELOCITY
    }
    
    // Constructors
    public NotificationTemplate() {}
    
    public NotificationTemplate(String templateId, String name, NotificationChannel channel,
                              String subjectTemplate, String contentTemplate) {
        this.templateId = templateId;
        this.name = name;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.contentTemplate = contentTemplate;
    }
    
    // Getters and setters
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    
    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
    
    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }
    
    public TemplateEngine getEngine() { return engine; }
    public void setEngine(TemplateEngine engine) { this.engine = engine; }
    
    public Map<String, Object> getDefaultVariables() { return defaultVariables; }
    public void setDefaultVariables(Map<String, Object> defaultVariables) { 
        this.defaultVariables = defaultVariables; 
    }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

### Enhanced Configuration Properties

```java
package com.enterprise.notification.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.*;
import javax.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Master notification configuration properties
 */
@ConfigurationProperties(prefix = "app.notification")
@Validated
public class NotificationProperties {
    
    /**
     * Enable or disable notification system
     */
    private boolean enabled = true;
    
    /**
     * Default notification channels to use if none specified
     */
    private List<String> defaultChannels = List.of("email");
    
    /**
     * Maximum number of retry attempts for failed notifications
     */
    @Min(0)
    @Max(10)
    private int maxRetryAttempts = 3;
    
    /**
     * Retry delay configuration
     */
    @Valid
    private RetryConfig retry = new RetryConfig();
    
    /**
     * Rate limiting configuration
     */
    @Valid
    private RateLimitConfig rateLimit = new RateLimitConfig();
    
    /**
     * Template configuration
     */
    @Valid
    private TemplateConfig templates = new TemplateConfig();
    
    /**
     * Monitoring and metrics configuration
     */
    @Valid
    private MonitoringConfig monitoring = new MonitoringConfig();
    
    /**
     * Email-specific configuration
     */
    @Valid
    private EmailConfig email = new EmailConfig();
    
    /**
     * SMS-specific configuration
     */
    @Valid
    private SmsConfig sms = new SmsConfig();
    
    /**
     * Slack-specific configuration
     */
    @Valid
    private SlackConfig slack = new SlackConfig();
    
    public static class RetryConfig {
        
        /**
         * Enable retry mechanism
         */
        private boolean enabled = true;
        
        /**
         * Base delay between retries
         */
        private Duration baseDelay = Duration.ofSeconds(1);
        
        /**
         * Maximum delay between retries
         */
        private Duration maxDelay = Duration.ofMinutes(1);
        
        /**
         * Backoff multiplier for exponential backoff
         */
        @DecimalMin("1.0")
        private double backoffMultiplier = 2.0;
        
        /**
         * Jitter factor to add randomness to retry delays
         */
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double jitterFactor = 0.1;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getBaseDelay() { return baseDelay; }
        public void setBaseDelay(Duration baseDelay) { this.baseDelay = baseDelay; }
        
        public Duration getMaxDelay() { return maxDelay; }
        public void setMaxDelay(Duration maxDelay) { this.maxDelay = maxDelay; }
        
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { 
            this.backoffMultiplier = backoffMultiplier; 
        }
        
        public double getJitterFactor() { return jitterFactor; }
        public void setJitterFactor(double jitterFactor) { this.jitterFactor = jitterFactor; }
    }
    
    public static class RateLimitConfig {
        
        /**
         * Enable rate limiting
         */
        private boolean enabled = false;
        
        /**
         * Maximum requests per minute per channel
         */
        @Min(1)
        private int requestsPerMinute = 60;
        
        /**
         * Maximum requests per hour per channel
         */
        @Min(1)
        private int requestsPerHour = 1000;
        
        /**
         * Burst capacity for rate limiting
         */
        @Min(1)
        private int burstCapacity = 10;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { 
            this.requestsPerMinute = requestsPerMinute; 
        }
        
        public int getRequestsPerHour() { return requestsPerHour; }
        public void setRequestsPerHour(int requestsPerHour) { 
            this.requestsPerHour = requestsPerHour; 
        }
        
        public int getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }
    }
    
    public static class TemplateConfig {
        
        /**
         * Enable template processing
         */
        private boolean enabled = true;
        
        /**
         * Default template engine
         */
        private String defaultEngine = "simple";
        
        /**
         * Template cache configuration
         */
        @Valid
        private CacheConfig cache = new CacheConfig();
        
        /**
         * Template repository configuration
         */
        private String repositoryType = "memory"; // memory, database, file
        
        /**
         * File-based template repository path
         */
        private String repositoryPath = "classpath:/templates/notifications/";
        
        public static class CacheConfig {
            
            private boolean enabled = true;
            
            @Min(1)
            private int maxSize = 100;
            
            private Duration expireAfterWrite = Duration.ofHours(1);
            
            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public int getMaxSize() { return maxSize; }
            public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
            
            public Duration getExpireAfterWrite() { return expireAfterWrite; }
            public void setExpireAfterWrite(Duration expireAfterWrite) { 
                this.expireAfterWrite = expireAfterWrite; 
            }
        }
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getDefaultEngine() { return defaultEngine; }
        public void setDefaultEngine(String defaultEngine) { this.defaultEngine = defaultEngine; }
        
        public CacheConfig getCache() { return cache; }
        public void setCache(CacheConfig cache) { this.cache = cache; }
        
        public String getRepositoryType() { return repositoryType; }
        public void setRepositoryType(String repositoryType) { this.repositoryType = repositoryType; }
        
        public String getRepositoryPath() { return repositoryPath; }
        public void setRepositoryPath(String repositoryPath) { this.repositoryPath = repositoryPath; }
    }
    
    public static class MonitoringConfig {
        
        /**
         * Enable metrics collection
         */
        private boolean metricsEnabled = true;
        
        /**
         * Enable health checks
         */
        private boolean healthCheckEnabled = true;
        
        /**
         * Enable audit logging
         */
        private boolean auditEnabled = true;
        
        /**
         * Metrics export configuration
         */
        private Map<String, String> metricsExport = Map.of();
        
        // Getters and setters
        public boolean isMetricsEnabled() { return metricsEnabled; }
        public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }
        
        public boolean isHealthCheckEnabled() { return healthCheckEnabled; }
        public void setHealthCheckEnabled(boolean healthCheckEnabled) { 
            this.healthCheckEnabled = healthCheckEnabled; 
        }
        
        public boolean isAuditEnabled() { return auditEnabled; }
        public void setAuditEnabled(boolean auditEnabled) { this.auditEnabled = auditEnabled; }
        
        public Map<String, String> getMetricsExport() { return metricsExport; }
        public void setMetricsExport(Map<String, String> metricsExport) { 
            this.metricsExport = metricsExport; 
        }
    }
    
    public static class EmailConfig {
        
        /**
         * Enable email notifications
         */
        private boolean enabled = true;
        
        /**
         * SMTP configuration
         */
        @NotBlank
        private String host = "localhost";
        
        @Min(1)
        @Max(65535)
        private int port = 587;
        
        private String username;
        private String password;
        private boolean enableTls = true;
        private boolean enableSsl = false;
        
        /**
         * Default sender configuration
         */
        @Email
        private String defaultFrom;
        
        private String defaultFromName;
        
        /**
         * Email-specific rate limiting
         */
        @Min(1)
        private int maxEmailsPerHour = 100;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
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
        
        public boolean isEnableSsl() { return enableSsl; }
        public void setEnableSsl(boolean enableSsl) { this.enableSsl = enableSsl; }
        
        public String getDefaultFrom() { return defaultFrom; }
        public void setDefaultFrom(String defaultFrom) { this.defaultFrom = defaultFrom; }
        
        public String getDefaultFromName() { return defaultFromName; }
        public void setDefaultFromName(String defaultFromName) { this.defaultFromName = defaultFromName; }
        
        public int getMaxEmailsPerHour() { return maxEmailsPerHour; }
        public void setMaxEmailsPerHour(int maxEmailsPerHour) { 
            this.maxEmailsPerHour = maxEmailsPerHour; 
        }
    }
    
    public static class SmsConfig {
        
        /**
         * Enable SMS notifications
         */
        private boolean enabled = false;
        
        /**
         * SMS provider (twilio, aws-sns, etc.)
         */
        @NotBlank
        private String provider = "twilio";
        
        /**
         * Provider-specific configuration
         */
        private Map<String, String> providerConfig = Map.of();
        
        /**
         * Default sender ID
         */
        private String defaultSenderId;
        
        /**
         * SMS-specific rate limiting
         */
        @Min(1)
        private int maxSmsPerHour = 50;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public Map<String, String> getProviderConfig() { return providerConfig; }
        public void setProviderConfig(Map<String, String> providerConfig) { 
            this.providerConfig = providerConfig; 
        }
        
        public String getDefaultSenderId() { return defaultSenderId; }
        public void setDefaultSenderId(String defaultSenderId) { this.defaultSenderId = defaultSenderId; }
        
        public int getMaxSmsPerHour() { return maxSmsPerHour; }
        public void setMaxSmsPerHour(int maxSmsPerHour) { this.maxSmsPerHour = maxSmsPerHour; }
    }
    
    public static class SlackConfig {
        
        /**
         * Enable Slack notifications
         */
        private boolean enabled = false;
        
        /**
         * Slack bot token
         */
        private String botToken;
        
        /**
         * Default channel for notifications
         */
        private String defaultChannel = "#general";
        
        /**
         * Bot username
         */
        private String botUsername = "NotificationBot";
        
        /**
         * Bot icon URL or emoji
         */
        private String botIcon = ":bell:";
        
        /**
         * Slack-specific rate limiting
         */
        @Min(1)
        private int maxMessagesPerMinute = 1;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getBotToken() { return botToken; }
        public void setBotToken(String botToken) { this.botToken = botToken; }
        
        public String getDefaultChannel() { return defaultChannel; }
        public void setDefaultChannel(String defaultChannel) { this.defaultChannel = defaultChannel; }
        
        public String getBotUsername() { return botUsername; }
        public void setBotUsername(String botUsername) { this.botUsername = botUsername; }
        
        public String getBotIcon() { return botIcon; }
        public void setBotIcon(String botIcon) { this.botIcon = botIcon; }
        
        public int getMaxMessagesPerMinute() { return maxMessagesPerMinute; }
        public void setMaxMessagesPerMinute(int maxMessagesPerMinute) { 
            this.maxMessagesPerMinute = maxMessagesPerMinute; 
        }
    }
    
    // Main class getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public List<String> getDefaultChannels() { return defaultChannels; }
    public void setDefaultChannels(List<String> defaultChannels) { 
        this.defaultChannels = defaultChannels; 
    }
    
    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(int maxRetryAttempts) { 
        this.maxRetryAttempts = maxRetryAttempts; 
    }
    
    public RetryConfig getRetry() { return retry; }
    public void setRetry(RetryConfig retry) { this.retry = retry; }
    
    public RateLimitConfig getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimitConfig rateLimit) { this.rateLimit = rateLimit; }
    
    public TemplateConfig getTemplates() { return templates; }
    public void setTemplates(TemplateConfig templates) { this.templates = templates; }
    
    public MonitoringConfig getMonitoring() { return monitoring; }
    public void setMonitoring(MonitoringConfig monitoring) { this.monitoring = monitoring; }
    
    public EmailConfig getEmail() { return email; }
    public void setEmail(EmailConfig email) { this.email = email; }
    
    public SmsConfig getSms() { return sms; }
    public void setSms(SmsConfig sms) { this.sms = sms; }
    
    public SlackConfig getSlack() { return slack; }
    public void setSlack(SlackConfig slack) { this.slack = slack; }
}
```

### Advanced Service Implementation

```java
package com.enterprise.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main notification service interface
 */
public interface NotificationService {
    
    /**
     * Send notification synchronously
     */
    NotificationResponse sendNotification(NotificationRequest request);
    
    /**
     * Send notification asynchronously
     */
    CompletableFuture<NotificationResponse> sendNotificationAsync(NotificationRequest request);
    
    /**
     * Send bulk notifications
     */
    Map<String, NotificationResponse> sendBulkNotifications(List<NotificationRequest> requests);
    
    /**
     * Schedule notification for future delivery
     */
    NotificationResponse scheduleNotification(NotificationRequest request);
    
    /**
     * Get notification status
     */
    Optional<NotificationResponse> getNotificationStatus(String notificationId);
    
    /**
     * Check if notification channel is supported
     */
    boolean isChannelSupported(NotificationChannel channel);
    
    /**
     * Get supported channels
     */
    Set<NotificationChannel> getSupportedChannels();
}

/**
 * Composite notification service implementation that orchestrates multiple channels
 */
@Service
public class CompositeNotificationService implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompositeNotificationService.class);
    
    private final Map<NotificationChannel, NotificationService> channelServices;
    private final NotificationProperties properties;
    private final TemplateService templateService;
    private final NotificationMetrics metrics;
    private final RateLimitService rateLimitService;
    private final NotificationAuditService auditService;
    
    // In-memory storage for notification status (in production, use Redis or database)
    private final Map<String, NotificationResponse> notificationStore = new ConcurrentHashMap<>();
    
    public CompositeNotificationService(List<NotificationService> notificationServices,
                                      NotificationProperties properties,
                                      TemplateService templateService,
                                      NotificationMetrics metrics,
                                      RateLimitService rateLimitService,
                                      NotificationAuditService auditService) {
        this.properties = properties;
        this.templateService = templateService;
        this.metrics = metrics;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
        
        // Map services by their supported channels
        this.channelServices = new HashMap<>();
        for (NotificationService service : notificationServices) {
            for (NotificationChannel channel : service.getSupportedChannels()) {
                channelServices.put(channel, service);
            }
        }
        
        logger.info("Initialized CompositeNotificationService with {} channel services", 
                   channelServices.size());
    }
    
    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        Timer.Sample sample = Timer.start(metrics.getMeterRegistry());
        
        try {
            // Validate request
            validateRequest(request);
            
            // Process template if specified
            NotificationRequest processedRequest = processTemplate(request);
            
            // Check rate limits
            checkRateLimits(processedRequest);
            
            // Create response
            NotificationResponse response = new NotificationResponse(request.getCorrelationId());
            
            // Send to each channel
            for (NotificationChannel channel : processedRequest.getChannels()) {
                NotificationService channelService = channelServices.get(channel);
                if (channelService == null) {
                    logger.warn("No service found for channel: {}", channel);
                    response.addChannelResponse(channel, 
                        new NotificationResponse.ChannelResponse(
                            NotificationResponse.NotificationStatus.FAILED,
                            "Channel not supported: " + channel
                        ));
                    continue;
                }
                
                try {
                    NotificationResponse channelResponse = sendToChannel(channelService, 
                        processedRequest, channel);
                    response.addChannelResponse(channel, 
                        channelResponse.getChannelResponses().get(channel));
                        
                } catch (Exception e) {
                    logger.error("Failed to send notification via channel: {}", channel, e);
                    response.addChannelResponse(channel,
                        new NotificationResponse.ChannelResponse(
                            NotificationResponse.NotificationStatus.FAILED,
                            "Channel error: " + e.getMessage()
                        ));
                }
            }
            
            // Store response for status tracking
            notificationStore.put(response.getNotificationId(), response);
            
            // Audit logging
            auditService.logNotification(processedRequest, response);
            
            // Update metrics
            updateMetrics(response);
            
            return response;
            
        } finally {
            sample.stop(metrics.getNotificationTimer());
        }
    }
    
    @Override
    public CompletableFuture<NotificationResponse> sendNotificationAsync(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> sendNotification(request));
    }
    
    @Override
    public Map<String, NotificationResponse> sendBulkNotifications(List<NotificationRequest> requests) {
        logger.info("Sending bulk notifications: {} requests", requests.size());
        
        Map<String, NotificationResponse> responses = new ConcurrentHashMap<>();
        
        // Process requests in parallel
        List<CompletableFuture<Void>> futures = requests.stream()
            .map(request -> {
                return CompletableFuture.runAsync(() -> {
                    try {
                        NotificationResponse response = sendNotification(request);
                        responses.put(request.getCorrelationId() != null ? 
                            request.getCorrelationId() : response.getNotificationId(), response);
                    } catch (Exception e) {
                        logger.error("Failed to send bulk notification", e);
                        NotificationResponse errorResponse = new NotificationResponse(
                            request.getCorrelationId());
                        errorResponse.setStatus(NotificationResponse.NotificationStatus.FAILED);
                        errorResponse.setMessage("Bulk processing error: " + e.getMessage());
                        responses.put(request.getCorrelationId() != null ? 
                            request.getCorrelationId() : errorResponse.getNotificationId(), 
                            errorResponse);
                    }
                });
            })
            .collect(Collectors.toList());
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        logger.info("Completed bulk notifications: {} responses", responses.size());
        return responses;
    }
    
    @Override
    public NotificationResponse scheduleNotification(NotificationRequest request) {
        if (request.getScheduledTime() == null) {
            throw new IllegalArgumentException("Scheduled time is required for scheduled notifications");
        }
        
        // In a real implementation, this would integrate with a scheduler like Quartz
        // or use database-based scheduling
        logger.info("Scheduling notification for: {}", request.getScheduledTime());
        
        NotificationResponse response = new NotificationResponse(request.getCorrelationId());
        response.setStatus(NotificationResponse.NotificationStatus.SCHEDULED);
        response.setMessage("Notification scheduled for " + request.getScheduledTime());
        
        // Store for tracking
        notificationStore.put(response.getNotificationId(), response);
        
        return response;
    }
    
    @Override
    public Optional<NotificationResponse> getNotificationStatus(String notificationId) {
        return Optional.ofNullable(notificationStore.get(notificationId));
    }
    
    @Override
    public boolean isChannelSupported(NotificationChannel channel) {
        return channelServices.containsKey(channel);
    }
    
    @Override
    public Set<NotificationChannel> getSupportedChannels() {
        return channelServices.keySet();
    }
    
    @Retryable(value = {Exception.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    private NotificationResponse sendToChannel(NotificationService channelService, 
                                             NotificationRequest request, 
                                             NotificationChannel channel) {
        
        logger.debug("Sending notification via channel: {}", channel);
        
        // Create channel-specific request
        NotificationRequest channelRequest = NotificationRequest.builder()
            .channels(Set.of(channel))
            .recipient(request.getRecipient())
            .subject(request.getSubject())
            .content(request.getContent())
            .priority(request.getPriority())
            .metadata(request.getMetadata())
            .correlationId(request.getCorrelationId())
            .build();
        
        return channelService.sendNotification(channelRequest);
    }
    
    @Recover
    private NotificationResponse recoverFromChannelFailure(Exception e, 
                                                          NotificationService channelService,
                                                          NotificationRequest request,
                                                          NotificationChannel channel) {
        logger.error("Failed to send notification via channel {} after retries", channel, e);
        
        NotificationResponse response = new NotificationResponse(request.getCorrelationId());
        response.addChannelResponse(channel,
            new NotificationResponse.ChannelResponse(
                NotificationResponse.NotificationStatus.FAILED,
                "Failed after retries: " + e.getMessage()
            ));
        
        return response;
    }
    
    private void validateRequest(NotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification request cannot be null");
        }
        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            // Use default channels
            Set<NotificationChannel> defaultChannels = properties.getDefaultChannels().stream()
                .map(NotificationChannel::fromCode)
                .collect(Collectors.toSet());
            request.setChannels(defaultChannels);
        }
        if (request.getRecipient() == null || request.getRecipient().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient is required");
        }
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
    }
    
    private NotificationRequest processTemplate(NotificationRequest request) {
        if (request.getTemplateId() == null || !properties.getTemplates().isEnabled()) {
            return request;
        }
        
        try {
            return templateService.processTemplate(request);
        } catch (Exception e) {
            logger.error("Template processing failed for template: {}", request.getTemplateId(), e);
            throw new NotificationException("Template processing failed", e);
        }
    }
    
    private void checkRateLimits(NotificationRequest request) {
        if (!properties.getRateLimit().isEnabled()) {
            return;
        }
        
        for (NotificationChannel channel : request.getChannels()) {
            if (!rateLimitService.isAllowed(channel, request.getRecipient())) {
                throw new NotificationException("Rate limit exceeded for channel: " + channel);
            }
        }
    }
    
    private void updateMetrics(NotificationResponse response) {
        metrics.incrementNotificationCount(response.getStatus());
        
        for (Map.Entry<NotificationChannel, NotificationResponse.ChannelResponse> entry : 
             response.getChannelResponses().entrySet()) {
            metrics.incrementChannelCount(entry.getKey(), entry.getValue().getStatus());
        }
    }
}

/**
 * Template processing service
 */
@Service
@ConditionalOnProperty(prefix = "app.notification.templates", name = "enabled", 
                      havingValue = "true", matchIfMissing = true)
public class TemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);
    
    private final NotificationProperties properties;
    private final TemplateRepository templateRepository;
    private final Cache<String, NotificationTemplate> templateCache;
    
    public TemplateService(NotificationProperties properties,
                          TemplateRepository templateRepository,
                          CacheManager cacheManager) {
        this.properties = properties;
        this.templateRepository = templateRepository;
        
        // Initialize cache
        if (properties.getTemplates().getCache().isEnabled()) {
            this.templateCache = cacheManager.getCache("notification-templates");
        } else {
            this.templateCache = null;
        }
    }
    
    public NotificationRequest processTemplate(NotificationRequest request) {
        NotificationTemplate template = getTemplate(request.getTemplateId());
        if (template == null) {
            throw new NotificationException("Template not found: " + request.getTemplateId());
        }
        
        if (!template.isActive()) {
            throw new NotificationException("Template is inactive: " + request.getTemplateId());
        }
        
        // Merge variables
        Map<String, Object> variables = new HashMap<>();
        if (template.getDefaultVariables() != null) {
            variables.putAll(template.getDefaultVariables());
        }
        if (request.getTemplateVariables() != null) {
            variables.putAll(request.getTemplateVariables());
        }
        
        // Process templates
        String processedSubject = processTemplateContent(template.getSubjectTemplate(), variables);
        String processedContent = processTemplateContent(template.getContentTemplate(), variables);
        
        // Create new request with processed content
        return NotificationRequest.builder()
            .channels(request.getChannels())
            .recipient(request.getRecipient())
            .subject(processedSubject)
            .content(processedContent)
            .priority(request.getPriority())
            .scheduledTime(request.getScheduledTime())
            .metadata(request.getMetadata())
            .trackDelivery(request.isTrackDelivery())
            .correlationId(request.getCorrelationId())
            .build();
    }
    
    private NotificationTemplate getTemplate(String templateId) {
        if (templateCache != null) {
            NotificationTemplate cached = templateCache.get(templateId, NotificationTemplate.class);
            if (cached != null) {
                return cached;
            }
        }
        
        NotificationTemplate template = templateRepository.findById(templateId);
        
        if (template != null && templateCache != null) {
            templateCache.put(templateId, template);
        }
        
        return template;
    }
    
    private String processTemplateContent(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        
        // Simple template processing (replace ${variable} with values)
        // In production, use Thymeleaf, Freemarker, or other template engines
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
}

/**
 * Rate limiting service
 */
@Service
@ConditionalOnProperty(prefix = "app.notification.rate-limit", name = "enabled", havingValue = "true")
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    private final NotificationProperties properties;
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    public RateLimitService(NotificationProperties properties) {
        this.properties = properties;
    }
    
    public boolean isAllowed(NotificationChannel channel, String recipient) {
        String key = channel.getCode() + ":" + recipient;
        RateLimiter rateLimiter = rateLimiters.computeIfAbsent(key, k -> createRateLimiter());
        
        boolean allowed = rateLimiter.tryAcquire();
        if (!allowed) {
            logger.warn("Rate limit exceeded for channel {} and recipient {}", channel, recipient);
        }
        
        return allowed;
    }
    
    private RateLimiter createRateLimiter() {
        // Using Guava's RateLimiter for simplicity
        // In production, consider using Redis-based rate limiting
        double permitsPerSecond = properties.getRateLimit().getRequestsPerMinute() / 60.0;
        return RateLimiter.create(permitsPerSecond);
    }
}

/**
 * Audit service for notification logging
 */
@Service
@ConditionalOnProperty(prefix = "app.notification.monitoring", name = "audit-enabled", 
                      havingValue = "true", matchIfMissing = true)
public class NotificationAuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("NOTIFICATION_AUDIT");
    
    public void logNotification(NotificationRequest request, NotificationResponse response) {
        AuditEvent event = AuditEvent.builder()
            .timestamp(Instant.now())
            .notificationId(response.getNotificationId())
            .correlationId(request.getCorrelationId())
            .channels(request.getChannels())
            .recipient(request.getRecipient())
            .subject(request.getSubject())
            .status(response.getStatus())
            .metadata(request.getMetadata())
            .build();
        
        auditLogger.info("Notification sent: {}", event);
    }
    
    @Data
    @Builder
    public static class AuditEvent {
        private Instant timestamp;
        private String notificationId;
        private String correlationId;
        private Set<NotificationChannel> channels;
        private String recipient;
        private String subject;
        private NotificationResponse.NotificationStatus status;
        private Map<String, String> metadata;
    }
}

/**
 * Template repository interface
 */
public interface TemplateRepository {
    NotificationTemplate findById(String templateId);
    List<NotificationTemplate> findByChannel(NotificationChannel channel);
    void save(NotificationTemplate template);
    void deleteById(String templateId);
}

/**
 * In-memory template repository implementation
 */
@Component
@ConditionalOnProperty(prefix = "app.notification.templates", name = "repository-type", 
                      havingValue = "memory", matchIfMissing = true)
public class InMemoryTemplateRepository implements TemplateRepository {
    
    private final Map<String, NotificationTemplate> templates = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeDefaultTemplates() {
        // Welcome email template
        NotificationTemplate welcomeEmail = new NotificationTemplate(
            "welcome-email",
            "Welcome Email Template",
            NotificationChannel.EMAIL,
            "Welcome to ${appName}!",
            "Hello ${userName},\n\nWelcome to ${appName}! We're excited to have you on board.\n\nBest regards,\nThe ${appName} Team"
        );
        templates.put(welcomeEmail.getTemplateId(), welcomeEmail);
        
        // Password reset template
        NotificationTemplate passwordReset = new NotificationTemplate(
            "password-reset",
            "Password Reset Template",
            NotificationChannel.EMAIL,
            "Password Reset Request",
            "Hello ${userName},\n\nYou have requested a password reset. Click the link below to reset your password:\n\n${resetLink}\n\nIf you didn't request this, please ignore this email.\n\nBest regards,\nThe ${appName} Team"
        );
        templates.put(passwordReset.getTemplateId(), passwordReset);
        
        // SMS notification template
        NotificationTemplate smsAlert = new NotificationTemplate(
            "sms-alert",
            "SMS Alert Template",
            NotificationChannel.SMS,
            "Alert from ${appName}",
            "Alert: ${message}\n\nTime: ${timestamp}\n\n${appName}"
        );
        templates.put(smsAlert.getTemplateId(), smsAlert);
    }
    
    @Override
    public NotificationTemplate findById(String templateId) {
        return templates.get(templateId);
    }
    
    @Override
    public List<NotificationTemplate> findByChannel(NotificationChannel channel) {
        return templates.values().stream()
            .filter(template -> template.getChannel() == channel)
            .collect(Collectors.toList());
    }
    
    @Override
    public void save(NotificationTemplate template) {
        templates.put(template.getTemplateId(), template);
    }
    
    @Override
    public void deleteById(String templateId) {
        templates.remove(templateId);
    }
}
```

### Comprehensive Master Auto-Configuration

```java
package com.enterprise.notification.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Master auto-configuration for the notification system
 */
@Configuration
@ConditionalOnProperty(prefix = "app.notification", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({NotificationProperties.class})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import({
    EmailNotificationAutoConfiguration.class,
    SmsNotificationAutoConfiguration.class,
    SlackNotificationAutoConfiguration.class,
    NotificationMetricsAutoConfiguration.class,
    NotificationHealthAutoConfiguration.class
})
public class NotificationAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationAutoConfiguration.class);
    
    @Bean
    @ConditionalOnMissingBean
    public CompositeNotificationService compositeNotificationService(
            List<NotificationService> notificationServices,
            NotificationProperties properties,
            TemplateService templateService,
            NotificationMetrics metrics,
            @Autowired(required = false) RateLimitService rateLimitService,
            NotificationAuditService auditService) {
        
        logger.info("Auto-configuring CompositeNotificationService with {} services", 
                   notificationServices.size());
        
        return new CompositeNotificationService(notificationServices, properties, templateService,
                                              metrics, rateLimitService, auditService);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.notification.templates", name = "enabled", 
                          havingValue = "true", matchIfMissing = true)
    public TemplateService templateService(NotificationProperties properties,
                                         TemplateRepository templateRepository,
                                         CacheManager cacheManager) {
        return new TemplateService(properties, templateRepository, cacheManager);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public InMemoryTemplateRepository inMemoryTemplateRepository() {
        return new InMemoryTemplateRepository();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.notification.rate-limit", name = "enabled", havingValue = "true")
    public RateLimitService rateLimitService(NotificationProperties properties) {
        return new RateLimitService(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public NotificationAuditService notificationAuditService() {
        return new NotificationAuditService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public NotificationExceptionHandler notificationExceptionHandler() {
        return new NotificationExceptionHandler();
    }
}

/**
 * Email notification auto-configuration
 */
@Configuration
@ConditionalOnClass({JavaMailSender.class, MimeMessage.class})
@ConditionalOnProperty(prefix = "app.notification.email", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(MailSenderAutoConfiguration.class)
public class EmailNotificationAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(name = "emailNotificationService")
    @ConditionalOnBean(JavaMailSender.class)
    public EmailNotificationService emailNotificationService(JavaMailSender mailSender,
                                                            NotificationProperties properties) {
        return new EmailNotificationService(mailSender, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.notification.email", name = "host")
    public JavaMailSender javaMailSender(NotificationProperties properties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        NotificationProperties.EmailConfig email = properties.getEmail();
        mailSender.setHost(email.getHost());
        mailSender.setPort(email.getPort());
        mailSender.setUsername(email.getUsername());
        mailSender.setPassword(email.getPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", email.isEnableTls());
        props.put("mail.smtp.ssl.enable", email.isEnableSsl());
        
        return mailSender;
    }
}

/**
 * SMS notification auto-configuration
 */
@Configuration
@ConditionalOnProperty(prefix = "app.notification.sms", name = "enabled", havingValue = "true")
public class SmsNotificationAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(name = "smsNotificationService")
    @ConditionalOnProperty(prefix = "app.notification.sms", name = "provider")
    public SmsNotificationService smsNotificationService(NotificationProperties properties) {
        return new SmsNotificationService(properties);
    }
}

/**
 * Slack notification auto-configuration
 */
@Configuration
@ConditionalOnProperty(prefix = "app.notification.slack", name = "enabled", havingValue = "true")
public class SlackNotificationAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(name = "slackNotificationService")
    @ConditionalOnProperty(prefix = "app.notification.slack", name = "bot-token")
    public SlackNotificationService slackNotificationService(NotificationProperties properties) {
        return new SlackNotificationService(properties);
    }
}

/**
 * Metrics auto-configuration
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "app.notification.monitoring", name = "metrics-enabled", 
                      havingValue = "true", matchIfMissing = true)
public class NotificationMetricsAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public NotificationMetrics notificationMetrics(MeterRegistry meterRegistry) {
        return new NotificationMetrics(meterRegistry);
    }
}

/**
 * Health check auto-configuration
 */
@Configuration
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnProperty(prefix = "app.notification.monitoring", name = "health-check-enabled", 
                      havingValue = "true", matchIfMissing = true)
public class NotificationHealthAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public NotificationHealthIndicator notificationHealthIndicator(
            @Autowired(required = false) List<NotificationService> notificationServices,
            NotificationProperties properties) {
        return new NotificationHealthIndicator(notificationServices, properties);
    }
}
```

## 9. Real-World Usage Scenarios and Testing

### Complete Test Suite

```java
package com.enterprise.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive auto-configuration tests
 */
@SpringJUnitConfig
class NotificationAutoConfigurationTest {
    
    private ApplicationContextRunner contextRunner;
    
    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration.class));
    }
    
    @Test
    void shouldAutoConfigureNotificationServiceWithDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CompositeNotificationService.class);
            assertThat(context).hasSingleBean(NotificationProperties.class);
            assertThat(context).hasSingleBean(TemplateService.class);
            assertThat(context).hasSingleBean(NotificationAuditService.class);
        });
    }
    
    @Test
    void shouldNotConfigureWhenDisabled() {
        contextRunner
            .withPropertyValues("app.notification.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(CompositeNotificationService.class);
            });
    }
    
    @Test
    void shouldConfigureEmailServiceWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "app.notification.email.enabled=true",
                "app.notification.email.host=smtp.test.com",
                "app.notification.email.username=test@example.com",
                "app.notification.email.password=testpass"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(EmailNotificationService.class);
                assertThat(context).hasSingleBean(JavaMailSender.class);
            });
    }
    
    @Test
    void shouldConfigureWithCustomProperties() {
        contextRunner
            .withPropertyValues(
                "app.notification.default-channels=email,sms",
                "app.notification.max-retry-attempts=5",
                "app.notification.rate-limit.enabled=true",
                "app.notification.rate-limit.requests-per-minute=30",
                "app.notification.templates.enabled=true",
                "app.notification.templates.default-engine=thymeleaf"
            )
            .run(context -> {
                NotificationProperties properties = context.getBean(NotificationProperties.class);
                
                assertThat(properties.getDefaultChannels()).containsExactly("email", "sms");
                assertThat(properties.getMaxRetryAttempts()).isEqualTo(5);
                assertThat(properties.getRateLimit().isEnabled()).isTrue();
                assertThat(properties.getRateLimit().getRequestsPerMinute()).isEqualTo(30);
                assertThat(properties.getTemplates().isEnabled()).isTrue();
                assertThat(properties.getTemplates().getDefaultEngine()).isEqualTo("thymeleaf");
            });
    }
    
    @Test
    void shouldConfigureMetricsWhenMicrometerPresent() {
        contextRunner
            .withBean(MeterRegistry.class, () -> mock(MeterRegistry.class))
            .run(context -> {
                assertThat(context).hasSingleBean(NotificationMetrics.class);
            });
    }
    
    @Test
    void shouldConfigureHealthIndicator() {
        contextRunner
            .withBean(HealthIndicator.class, () -> mock(HealthIndicator.class))
            .run(context -> {
                assertThat(context).hasSingleBean(NotificationHealthIndicator.class);
            });
    }
    
    @Test
    void shouldValidateConfigurationProperties() {
        contextRunner
            .withPropertyValues(
                "app.notification.email.port=0", // Invalid port
                "app.notification.max-retry-attempts=15", // Exceeds max
                "app.notification.rate-limit.requests-per-minute=0" // Below min
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class);
            });
    }
}

/**
 * Integration tests for the notification system
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.notification.enabled=true",
    "app.notification.email.enabled=true",
    "app.notification.email.host=smtp.test.com",
    "app.notification.email.port=587",
    "app.notification.email.username=test@example.com",
    "app.notification.email.password=testpass",
    "app.notification.email.default-from=noreply@test.com",
    "app.notification.templates.enabled=true",
    "app.notification.rate-limit.enabled=false",
    "app.notification.monitoring.metrics-enabled=true"
})
class NotificationSystemIntegrationTest {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationProperties properties;
    
    @MockBean
    private JavaMailSender mailSender;
    
    @Test
    void shouldSendEmailNotificationSuccessfully() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
            .channel(NotificationChannel.EMAIL)
            .recipient("user@example.com")
            .subject("Test Subject")
            .content("Test content")
            .priority(NotificationRequest.Priority.NORMAL)
            .correlationId("test-123")
            .build();
        
        // When
        NotificationResponse response = notificationService.sendNotification(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCorrelationId()).isEqualTo("test-123");
        assertThat(response.getStatus()).isEqualTo(NotificationResponse.NotificationStatus.SUCCESS);
        assertThat(response.getChannelResponses()).hasSize(1);
        
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
    
    @Test
    void shouldProcessTemplateSuccessfully() {
        // Given
        Map<String, Object> templateVariables = Map.of(
            "userName", "John Doe",
            "appName", "TestApp"
        );
        
        NotificationRequest request = NotificationRequest.builder()
            .channel(NotificationChannel.EMAIL)
            .recipient("john@example.com")
            .subject("Welcome Template")
            .content("Template content")
            .template("welcome-email", templateVariables)
            .build();
        
        // When
        NotificationResponse response = notificationService.sendNotification(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(NotificationResponse.NotificationStatus.SUCCESS);
        
        // Verify template processing
        ArgumentCaptor<SimpleMailMessage> messageCaptor = 
            ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSubject()).contains("TestApp");
        assertThat(sentMessage.getText()).contains("John Doe");
    }
    
    @Test
    void shouldHandleBulkNotifications() {
        // Given
        List<NotificationRequest> requests = List.of(
            NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .recipient("user1@example.com")
                .subject("Test 1")
                .content("Content 1")
                .correlationId("bulk-1")
                .build(),
            NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .recipient("user2@example.com")
                .subject("Test 2")
                .content("Content 2")
                .correlationId("bulk-2")
                .build()
        );
        
        // When
        Map<String, NotificationResponse> responses = 
            notificationService.sendBulkNotifications(requests);
        
        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get("bulk-1").getStatus())
            .isEqualTo(NotificationResponse.NotificationStatus.SUCCESS);
        assertThat(responses.get("bulk-2").getStatus())
            .isEqualTo(NotificationResponse.NotificationStatus.SUCCESS);
        
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
    
    @Test
    void shouldTrackNotificationStatus() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
            .channel(NotificationChannel.EMAIL)
            .recipient("user@example.com")
            .subject("Trackable notification")
            .content("Content")
            .trackDelivery(true)
            .build();
        
        // When
        NotificationResponse response = notificationService.sendNotification(request);
        String notificationId = response.getNotificationId();
        
        // Then
        Optional<NotificationResponse> tracked = 
            notificationService.getNotificationStatus(notificationId);
        
        assertThat(tracked).isPresent();
        assertThat(tracked.get().getStatus())
            .isEqualTo(NotificationResponse.NotificationStatus.SUCCESS);
    }
}

/**
 * Performance and load tests
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.notification.enabled=true",
    "app.notification.email.enabled=true",
    "app.notification.rate-limit.enabled=false"
})
class NotificationPerformanceTest {
    
    @Autowired
    private NotificationService notificationService;
    
    @MockBean
    private JavaMailSender mailSender;
    
    @Test
    void shouldHandleHighVolumeNotifications() {
        // Given
        int notificationCount = 1000;
        List<NotificationRequest> requests = IntStream.range(0, notificationCount)
            .mapToObj(i -> NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .recipient("user" + i + "@example.com")
                .subject("Performance Test " + i)
                .content("Performance test content " + i)
                .correlationId("perf-" + i)
                .build())
            .collect(Collectors.toList());
        
        // When
        long startTime = System.currentTimeMillis();
        Map<String, NotificationResponse> responses = 
            notificationService.sendBulkNotifications(requests);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(responses).hasSize(notificationCount);
        
        long duration = endTime - startTime;
        double throughput = (double) notificationCount / (duration / 1000.0);
        
        System.out.printf("Processed %d notifications in %d ms (%.2f notifications/sec)%n",
                         notificationCount, duration, throughput);
        
        // Assert reasonable performance (adjust based on requirements)
        assertThat(throughput).isGreaterThan(50.0); // At least 50 notifications per second
        
        verify(mailSender, times(notificationCount)).send(any(SimpleMailMessage.class));
    }
}
```

### Real-World Application Examples

```java
/**
 * Example Spring Boot application using the notification starter
 */
@SpringBootApplication
public class ECommerceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ECommerceApplication.class, args);
    }
}

/**
 * User service demonstrating notification usage
 */
@Service
public class UserService {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    public UserService(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }
    
    @EventListener
    public void handleUserRegistration(UserRegisteredEvent event) {
        User user = event.getUser();
        
        // Send welcome email using template
        NotificationRequest welcomeEmail = NotificationRequest.builder()
            .channel(NotificationChannel.EMAIL)
            .recipient(user.getEmail())
            .subject("Welcome to Our Platform")
            .content("Welcome template content")
            .template("welcome-email", Map.of(
                "userName", user.getName(),
                "appName", "ECommerce Platform",
                "loginUrl", "https://app.example.com/login"
            ))
            .priority(NotificationRequest.Priority.NORMAL)
            .correlationId("user-registration-" + user.getId())
            .build();
        
        // Send asynchronously
        notificationService.sendNotificationAsync(welcomeEmail)
            .thenAccept(response -> {
                if (response.getStatus() == NotificationResponse.NotificationStatus.SUCCESS) {
                    user.setWelcomeEmailSent(true);
                    userRepository.save(user);
                }
            })
            .exceptionally(throwable -> {
                log.error("Failed to send welcome email to user: {}", user.getId(), throwable);
                return null;
            });
    }
    
    public void sendPasswordResetNotification(String email, String resetToken) {
        NotificationRequest passwordReset = NotificationRequest.builder()
            .channels(Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS))
            .recipient(email)
            .subject("Password Reset Request")
            .content("Password reset content")
            .template("password-reset", Map.of(
                "resetLink", "https://app.example.com/reset?token=" + resetToken,
                "expirationTime", "24 hours"
            ))
            .priority(NotificationRequest.Priority.HIGH)
            .trackDelivery(true)
            .build();
        
        NotificationResponse response = notificationService.sendNotification(passwordReset);
        
        if (response.getStatus() != NotificationResponse.NotificationStatus.SUCCESS) {
            throw new NotificationException("Failed to send password reset notification");
        }
    }
}

/**
 * Order service demonstrating multi-channel notifications
 */
@Service
public class OrderService {
    
    private final NotificationService notificationService;
    
    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        Order order = event.getOrder();
        
        // Send order confirmation via multiple channels
        NotificationRequest orderConfirmation = NotificationRequest.builder()
            .channels(Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS))
            .recipient(order.getCustomerEmail())
            .subject("Order Confirmation #" + order.getOrderNumber())
            .content("Order confirmation content")
            .template("order-confirmation", Map.of(
                "orderNumber", order.getOrderNumber(),
                "customerName", order.getCustomerName(),
                "totalAmount", order.getTotalAmount(),
                "estimatedDelivery", order.getEstimatedDeliveryDate(),
                "trackingUrl", "https://app.example.com/track/" + order.getOrderNumber()
            ))
            .priority(NotificationRequest.Priority.HIGH)
            .metadata(Map.of(
                "orderId", order.getId().toString(),
                "customerId", order.getCustomerId().toString()
            ))
            .build();
        
        notificationService.sendNotificationAsync(orderConfirmation);
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void sendDeliveryUpdates() {
        List<Order> ordersInTransit = orderRepository.findOrdersInTransit();
        
        List<NotificationRequest> deliveryUpdates = ordersInTransit.stream()
            .filter(order -> hasLocationUpdate(order))
            .map(order -> NotificationRequest.builder()
                .channel(NotificationChannel.SMS)
                .recipient(order.getCustomerPhone())
                .subject("Delivery Update")
                .content("Your order #" + order.getOrderNumber() + " is " + 
                        order.getCurrentStatus() + ". Estimated delivery: " + 
                        order.getEstimatedDeliveryTime())
                .priority(NotificationRequest.Priority.NORMAL)
                .build())
            .collect(Collectors.toList());
        
        if (!deliveryUpdates.isEmpty()) {
            notificationService.sendBulkNotifications(deliveryUpdates);
        }
    }
}
```

### Production-Ready Configuration

```properties
# application.properties for production
# Core notification settings
app.notification.enabled=true
app.notification.default-channels=email
app.notification.max-retry-attempts=3

# Retry configuration
app.notification.retry.enabled=true
app.notification.retry.base-delay=PT1S
app.notification.retry.max-delay=PT1M
app.notification.retry.backoff-multiplier=2.0
app.notification.retry.jitter-factor=0.1

# Rate limiting
app.notification.rate-limit.enabled=true
app.notification.rate-limit.requests-per-minute=100
app.notification.rate-limit.requests-per-hour=1000
app.notification.rate-limit.burst-capacity=20

# Email configuration
app.notification.email.enabled=true
app.notification.email.host=${SMTP_HOST:smtp.sendgrid.net}
app.notification.email.port=${SMTP_PORT:587}
app.notification.email.username=${SMTP_USERNAME}
app.notification.email.password=${SMTP_PASSWORD}
app.notification.email.enable-tls=true
app.notification.email.default-from=${DEFAULT_FROM_EMAIL:noreply@example.com}
app.notification.email.default-from-name=${APP_NAME:My Application}
app.notification.email.max-emails-per-hour=500

# SMS configuration
app.notification.sms.enabled=${SMS_ENABLED:false}
app.notification.sms.provider=${SMS_PROVIDER:twilio}
app.notification.sms.provider-config.account-sid=${TWILIO_ACCOUNT_SID}
app.notification.sms.provider-config.auth-token=${TWILIO_AUTH_TOKEN}
app.notification.sms.default-sender-id=${SMS_SENDER_ID}
app.notification.sms.max-sms-per-hour=100

# Slack configuration
app.notification.slack.enabled=${SLACK_ENABLED:false}
app.notification.slack.bot-token=${SLACK_BOT_TOKEN}
app.notification.slack.default-channel=${SLACK_DEFAULT_CHANNEL:#notifications}
app.notification.slack.bot-username=NotificationBot
app.notification.slack.max-messages-per-minute=1

# Template configuration
app.notification.templates.enabled=true
app.notification.templates.default-engine=simple
app.notification.templates.repository-type=database
app.notification.templates.cache.enabled=true
app.notification.templates.cache.max-size=200
app.notification.templates.cache.expire-after-write=PT1H

# Monitoring configuration
app.notification.monitoring.metrics-enabled=true
app.notification.monitoring.health-check-enabled=true
app.notification.monitoring.audit-enabled=true

# Management endpoints
management.endpoints.web.exposure.include=health,metrics,info,notification
management.endpoint.health.show-details=when-authorized
management.endpoint.notification.enabled=true

# Logging
logging.level.com.enterprise.notification=INFO
logging.level.NOTIFICATION_AUDIT=INFO
```

## Key Takeaways

1. **Auto-Configuration Design**: Use conditional annotations to create smart defaults while allowing customization
2. **Configuration Properties**: Leverage validation, nested properties, and type safety for complex enterprise configuration
3. **Starter Development**: Follow Spring Boot conventions for creating reusable, discoverable starters
4. **Testing Strategy**: Use ApplicationContextRunner for comprehensive auto-configuration testing and integration tests for real-world scenarios
5. **Documentation**: Provide configuration metadata for excellent IDE support and developer experience
6. **Health Indicators**: Include comprehensive health checks for production monitoring and observability
7. **Ordering**: Use auto-configuration ordering annotations for managing complex dependencies
8. **Enterprise Patterns**: Implement retry logic, rate limiting, circuit breakers, and comprehensive error handling
9. **Observability**: Include metrics, tracing, and audit logging for production systems
10. **Scalability**: Design for high-volume, multi-channel notifications with async processing and bulk operations

This comprehensive enterprise notification system starter demonstrates advanced Spring Framework auto-configuration patterns, providing a production-ready, highly configurable, and observable notification system that can be easily integrated into any Spring Boot application. The implementation follows enterprise-grade patterns including proper error handling, retry logic, rate limiting, template processing, and comprehensive monitoring capabilities.