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

## Key Takeaways

1. **Auto-Configuration Design**: Use conditional annotations to create smart defaults
2. **Configuration Properties**: Leverage validation and nested properties for complex configuration
3. **Starter Development**: Follow Spring Boot conventions for creating reusable starters
4. **Testing Strategy**: Use ApplicationContextRunner for comprehensive auto-configuration testing
5. **Documentation**: Provide configuration metadata for IDE support
6. **Health Indicators**: Include health checks for production monitoring
7. **Ordering**: Use auto-configuration ordering annotations for complex dependencies

Auto-configuration and custom starters enable building reusable, enterprise-grade components that follow Spring Boot conventions and provide excellent developer experience.