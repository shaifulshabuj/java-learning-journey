# Day 65: Spring Cloud Netflix - Eureka, Hystrix, Zuul

## Learning Objectives
- Master service discovery with Netflix Eureka
- Implement circuit breakers with Hystrix
- Build API gateways with Zuul
- Understand resilience patterns in microservices
- Apply Netflix OSS components in production scenarios

---

## 1. Introduction to Spring Cloud Netflix

### 1.1 Netflix OSS Overview

```java
// Netflix OSS Components in Spring Cloud
@Configuration
public class NetflixOSSOverview {
    
    /*
     * Key Netflix Components:
     * 
     * 1. Eureka - Service Discovery
     *    - Service registration and discovery
     *    - Client-side load balancing
     *    - Health monitoring
     *    
     * 2. Hystrix - Circuit Breaker
     *    - Fault tolerance
     *    - Latency tolerance
     *    - Fallback mechanisms
     *    
     * 3. Zuul - API Gateway
     *    - Dynamic routing
     *    - Monitoring
     *    - Security
     *    - Load balancing
     *    
     * 4. Ribbon - Client Side Load Balancer
     *    - Load balancing algorithms
     *    - Service discovery integration
     *    - Retry logic
     *    
     * 5. Feign - Declarative HTTP Client
     *    - Simplified HTTP clients
     *    - Integration with Eureka
     *    - Built-in Hystrix support
     */
}

// Why Netflix OSS?
@Component
public class NetflixOSSBenefits {
    
    // Battle-tested in production at Netflix scale
    // Handles billions of requests per day
    // Proven resilience patterns
    // Strong Spring Cloud integration
    // Active community and support
    
    public void demonstrateResilience() {
        // Without Netflix OSS
        // - Manual service discovery
        // - No circuit breakers
        // - Complex routing logic
        // - No built-in resilience
        
        // With Netflix OSS
        // - Automatic service discovery
        // - Circuit breakers prevent cascading failures
        // - Dynamic routing with Zuul
        // - Built-in resilience patterns
    }
}
```

---

## 2. Eureka - Service Discovery

### 2.1 Eureka Server Setup

```java
// Eureka Server Application
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}

// Eureka Server Configuration
@Configuration
public class EurekaServerConfig {
    
    /*
    # application.yml
    spring:
      application:
        name: eureka-server
        
    server:
      port: 8761
      
    eureka:
      instance:
        hostname: localhost
      client:
        register-with-eureka: false  # Don't register server as a client
        fetch-registry: false        # Don't fetch registry
        service-url:
          defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
      server:
        enable-self-preservation: true
        eviction-interval-timer-in-ms: 60000
        renewal-percent-threshold: 0.85
        renewal-threshold-update-interval-ms: 900000
        
    # For HA setup
    ---
    spring:
      profiles: peer1
      
    server:
      port: 8761
      
    eureka:
      instance:
        hostname: peer1
      client:
        service-url:
          defaultZone: http://peer2:8762/eureka/
          
    ---
    spring:
      profiles: peer2
      
    server:
      port: 8762
      
    eureka:
      instance:
        hostname: peer2
      client:
        service-url:
          defaultZone: http://peer1:8761/eureka/
    */
}

// Custom Eureka Server Configuration
@Configuration
public class CustomEurekaServerConfig {
    
    @Bean
    public EurekaServerConfigBean eurekaServerConfigBean() {
        EurekaServerConfigBean config = new EurekaServerConfigBean();
        config.setEnableSelfPreservation(true);
        config.setRenewalPercentThreshold(0.85);
        return config;
    }
    
    @Bean
    public PeerAwareInstanceRegistry peerAwareInstanceRegistry(
            ServerCodecs serverCodecs) {
        return new CustomPeerAwareInstanceRegistry(serverCodecs);
    }
}

// Custom Instance Registry for Advanced Features
public class CustomPeerAwareInstanceRegistry extends PeerAwareInstanceRegistryImpl {
    
    public CustomPeerAwareInstanceRegistry(ServerCodecs serverCodecs) {
        super(serverCodecs);
    }
    
    @Override
    public void register(InstanceInfo info, boolean isReplication) {
        // Custom registration logic
        logger.info("Registering instance: {} - {}", info.getAppName(), info.getId());
        
        // Validate instance
        validateInstance(info);
        
        // Custom metadata processing
        processCustomMetadata(info);
        
        super.register(info, isReplication);
        
        // Post-registration hooks
        notifyRegistration(info);
    }
    
    @Override
    public boolean cancel(String appName, String id, boolean isReplication) {
        logger.info("Cancelling instance: {} - {}", appName, id);
        
        boolean result = super.cancel(appName, id, isReplication);
        
        if (result) {
            // Post-cancellation hooks
            notifyCancellation(appName, id);
        }
        
        return result;
    }
    
    private void validateInstance(InstanceInfo info) {
        if (info.getMetadata().get("version") == null) {
            throw new IllegalArgumentException("Instance must have version metadata");
        }
    }
    
    private void processCustomMetadata(InstanceInfo info) {
        // Process custom metadata
        info.getMetadata().put("registrationTime", String.valueOf(System.currentTimeMillis()));
    }
    
    private void notifyRegistration(InstanceInfo info) {
        // Send notifications, update metrics, etc.
    }
    
    private void notifyCancellation(String appName, String id) {
        // Cleanup, notifications, etc.
    }
}

// Eureka Dashboard Customization
@Controller
public class EurekaDashboardController {
    
    @Autowired
    private PeerAwareInstanceRegistry registry;
    
    @GetMapping("/custom-dashboard")
    public String customDashboard(Model model) {
        List<Application> apps = new ArrayList<>(registry.getApplications().getRegisteredApplications());
        
        Map<String, ApplicationInfo> appInfoMap = apps.stream()
                .collect(Collectors.toMap(
                    Application::getName,
                    app -> {
                        ApplicationInfo info = new ApplicationInfo();
                        info.setName(app.getName());
                        info.setInstanceCount(app.getInstances().size());
                        info.setHealthyCount(countHealthyInstances(app));
                        info.setVersions(getVersions(app));
                        return info;
                    }
                ));
        
        model.addAttribute("applications", appInfoMap);
        model.addAttribute("totalInstances", getTotalInstances(apps));
        model.addAttribute("healthyInstances", getTotalHealthyInstances(apps));
        
        return "custom-dashboard";
    }
    
    private int countHealthyInstances(Application app) {
        return (int) app.getInstances().stream()
                .filter(instance -> instance.getStatus() == InstanceStatus.UP)
                .count();
    }
    
    private Set<String> getVersions(Application app) {
        return app.getInstances().stream()
                .map(instance -> instance.getMetadata().get("version"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    private int getTotalInstances(List<Application> apps) {
        return apps.stream()
                .mapToInt(app -> app.getInstances().size())
                .sum();
    }
    
    private int getTotalHealthyInstances(List<Application> apps) {
        return apps.stream()
                .mapToInt(this::countHealthyInstances)
                .sum();
    }
}

@Data
class ApplicationInfo {
    private String name;
    private int instanceCount;
    private int healthyCount;
    private Set<String> versions;
}
```

### 2.2 Eureka Client Configuration

```java
// Eureka Client Application
@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

// Eureka Client Configuration
@Configuration
public class EurekaClientConfig {
    
    /*
    # application.yml
    spring:
      application:
        name: user-service
        
    server:
      port: ${PORT:8080}
      
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:8761/eureka/
        registry-fetch-interval-seconds: 30
        instance-info-replication-interval-seconds: 30
        initial-instance-info-replication-interval-seconds: 40
        eureka-service-url-poll-interval-seconds: 300
        
      instance:
        instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}
        hostname: ${HOSTNAME:localhost}
        prefer-ip-address: true
        lease-renewal-interval-in-seconds: 30
        lease-expiration-duration-in-seconds: 90
        metadata-map:
          version: @project.version@
          zone: primary
          region: us-east-1
        health-check-url-path: /actuator/health
        status-page-url-path: /actuator/info
    */
}

// Custom Eureka Client Configuration
@Component
public class CustomEurekaClient {
    
    @Autowired
    private EurekaClient eurekaClient;
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    public List<ServiceInstance> getServiceInstances(String serviceName) {
        return discoveryClient.getInstances(serviceName);
    }
    
    public String getServiceUrl(String serviceName) {
        List<ServiceInstance> instances = getServiceInstances(serviceName);
        
        if (instances.isEmpty()) {
            throw new ServiceUnavailableException("No instances available for " + serviceName);
        }
        
        // Simple round-robin selection
        ServiceInstance instance = instances.get(new Random().nextInt(instances.size()));
        return instance.getUri().toString();
    }
    
    @Scheduled(fixedDelay = 60000)
    public void logServiceInstances() {
        discoveryClient.getServices().forEach(service -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            logger.info("Service: {} has {} instances", service, instances.size());
            instances.forEach(instance -> 
                logger.info("  Instance: {} - {}", instance.getInstanceId(), instance.getUri())
            );
        });
    }
    
    @EventListener
    public void handleHeartbeatEvent(HeartbeatEvent event) {
        logger.info("Heartbeat event received: {}", event.getValue());
        // Refresh local cache or perform other actions
    }
    
    public void registerCustomMetadata(String key, String value) {
        eurekaClient.getApplicationInfoManager()
                .getInfo()
                .getMetadata()
                .put(key, value);
                
        // Trigger re-registration
        eurekaClient.getApplicationInfoManager().setInstanceStatus(InstanceStatus.UP);
    }
}

// Service Discovery with RestTemplate
@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Add interceptors
        restTemplate.getInterceptors().add(new LoggingInterceptor());
        restTemplate.getInterceptors().add(new MetricsInterceptor());
        
        return restTemplate;
    }
}

@Component
public class UserServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public User getUser(Long userId) {
        // Service name from Eureka
        String url = "http://user-service/api/users/" + userId;
        return restTemplate.getForObject(url, User.class);
    }
    
    public List<User> getUsers() {
        String url = "http://user-service/api/users";
        ResponseEntity<List<User>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<User>>() {}
        );
        return response.getBody();
    }
}

// Health Check Configuration
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Custom health check logic
        boolean databaseHealthy = checkDatabase();
        boolean externalServiceHealthy = checkExternalService();
        
        if (databaseHealthy && externalServiceHealthy) {
            return Health.up()
                    .withDetail("database", "UP")
                    .withDetail("externalService", "UP")
                    .build();
        } else {
            return Health.down()
                    .withDetail("database", databaseHealthy ? "UP" : "DOWN")
                    .withDetail("externalService", externalServiceHealthy ? "UP" : "DOWN")
                    .build();
        }
    }
    
    private boolean checkDatabase() {
        // Database health check logic
        return true;
    }
    
    private boolean checkExternalService() {
        // External service health check logic
        return true;
    }
}
```

---

## 3. Hystrix - Circuit Breaker Pattern

### 3.1 Hystrix Configuration and Usage

```java
// Enable Hystrix in Application
@SpringBootApplication
@EnableCircuitBreaker
@EnableHystrixDashboard
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
    
    @Bean
    public HystrixCommandAspect hystrixAspect() {
        return new HystrixCommandAspect();
    }
}

// Basic Hystrix Command
@Service
public class UserService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @HystrixCommand(
        fallbackMethod = "getUserFallback",
        commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"),
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50"),
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000")
        },
        threadPoolProperties = {
            @HystrixProperty(name = "coreSize", value = "20"),
            @HystrixProperty(name = "maxQueueSize", value = "10")
        }
    )
    public User getUser(Long userId) {
        String url = "http://user-service/api/users/" + userId;
        return restTemplate.getForObject(url, User.class);
    }
    
    public User getUserFallback(Long userId) {
        // Fallback logic
        User fallbackUser = new User();
        fallbackUser.setId(userId);
        fallbackUser.setName("Fallback User");
        fallbackUser.setEmail("fallback@example.com");
        fallbackUser.setStatus("FALLBACK");
        return fallbackUser;
    }
    
    @HystrixCommand(
        fallbackMethod = "getUsersFallback",
        commandKey = "getUsersCommand",
        groupKey = "UserServiceGroup",
        threadPoolKey = "UserServiceThreadPool"
    )
    public List<User> getUsers() {
        String url = "http://user-service/api/users";
        ResponseEntity<List<User>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<User>>() {}
        );
        return response.getBody();
    }
    
    public List<User> getUsersFallback() {
        // Return cached or default data
        return Collections.emptyList();
    }
}

// Advanced Hystrix Command Implementation
public class GetUserCommand extends HystrixCommand<User> {
    
    private final RestTemplate restTemplate;
    private final Long userId;
    
    public GetUserCommand(RestTemplate restTemplate, Long userId) {
        super(Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey("UserService"))
            .andCommandKey(HystrixCommandKey.Factory.asKey("GetUser"))
            .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("UserServicePool"))
            .andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter()
                    .withExecutionTimeoutInMilliseconds(3000)
                    .withCircuitBreakerRequestVolumeThreshold(20)
                    .withCircuitBreakerErrorThresholdPercentage(50)
                    .withCircuitBreakerSleepWindowInMilliseconds(5000)
                    .withMetricsRollingStatisticalWindowInMilliseconds(10000)
            )
            .andThreadPoolPropertiesDefaults(
                HystrixThreadPoolProperties.Setter()
                    .withCoreSize(20)
                    .withMaxQueueSize(10)
                    .withQueueSizeRejectionThreshold(10)
            )
        );
        
        this.restTemplate = restTemplate;
        this.userId = userId;
    }
    
    @Override
    protected User run() throws Exception {
        String url = "http://user-service/api/users/" + userId;
        return restTemplate.getForObject(url, User.class);
    }
    
    @Override
    protected User getFallback() {
        // Check if failure was due to circuit breaker
        if (isCircuitBreakerOpen()) {
            logger.warn("Circuit breaker is open for GetUser command");
        }
        
        // Check failure exception
        Throwable failureCause = getFailedExecutionException();
        if (failureCause != null) {
            logger.error("GetUser command failed", failureCause);
        }
        
        // Return fallback user
        User fallbackUser = new User();
        fallbackUser.setId(userId);
        fallbackUser.setName("Fallback User");
        fallbackUser.setStatus("CIRCUIT_BREAKER_OPEN");
        return fallbackUser;
    }
    
    @Override
    protected String getCacheKey() {
        return "user-" + userId;
    }
}

// Hystrix Observable Command
public class GetUsersObservableCommand extends HystrixObservableCommand<User> {
    
    private final WebClient webClient;
    private final List<Long> userIds;
    
    public GetUsersObservableCommand(WebClient webClient, List<Long> userIds) {
        super(Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey("UserService"))
            .andCommandKey(HystrixCommandKey.Factory.asKey("GetUsersObservable"))
        );
        
        this.webClient = webClient;
        this.userIds = userIds;
    }
    
    @Override
    protected Observable<User> construct() {
        return Observable.create(observer -> {
            userIds.forEach(userId -> {
                webClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(User.class)
                    .subscribe(
                        user -> observer.onNext(user),
                        error -> observer.onError(error)
                    );
            });
            observer.onCompleted();
        });
    }
    
    @Override
    protected Observable<User> resumeWithFallback() {
        return Observable.create(observer -> {
            userIds.forEach(userId -> {
                User fallbackUser = new User();
                fallbackUser.setId(userId);
                fallbackUser.setName("Fallback User " + userId);
                observer.onNext(fallbackUser);
            });
            observer.onCompleted();
        });
    }
}

// Hystrix Configuration
@Configuration
public class HystrixConfig {
    
    @Bean
    public HystrixConcurrencyStrategy customConcurrencyStrategy() {
        return new CustomHystrixConcurrencyStrategy();
    }
}

public class CustomHystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {
    
    @Override
    public <T> Callable<T> wrapCallable(Callable<T> callable) {
        // Wrap callable to propagate request context
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        return () -> {
            try {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                return callable.call();
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }
}
```

### 3.2 Hystrix Dashboard and Monitoring

```java
// Hystrix Dashboard Application
@SpringBootApplication
@EnableHystrixDashboard
public class HystrixDashboardApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HystrixDashboardApplication.class, args);
    }
}

// Turbine for Aggregating Hystrix Streams
@SpringBootApplication
@EnableTurbine
public class TurbineServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TurbineServerApplication.class, args);
    }
}

// Turbine Configuration
@Configuration
public class TurbineConfig {
    
    /*
    # application.yml
    spring:
      application:
        name: turbine-server
        
    server:
      port: 8989
      
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:8761/eureka/
          
    turbine:
      app-config: user-service,order-service,product-service
      cluster-name-expression: new String("default")
      combine-host-port: true
      
    # For multiple clusters
    turbine:
      aggregator:
        cluster-config: SYSTEM,USER
      app-config: user-service,order-service
      cluster-name-expression: metadata['cluster']
    */
}

// Custom Hystrix Metrics Publisher
@Component
public class CustomHystrixMetricsPublisher extends HystrixMetricsPublisher {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(
            HystrixCommandKey commandKey,
            HystrixCommandGroupKey commandGroupKey,
            HystrixCommandMetrics metrics,
            HystrixCircuitBreaker circuitBreaker,
            HystrixCommandProperties properties) {
        
        return new CustomHystrixMetricsPublisherCommand(
            commandKey, commandGroupKey, metrics, circuitBreaker, properties, meterRegistry
        );
    }
}

public class CustomHystrixMetricsPublisherCommand implements HystrixMetricsPublisherCommand {
    
    private final HystrixCommandKey commandKey;
    private final HystrixCommandMetrics metrics;
    private final MeterRegistry meterRegistry;
    
    public CustomHystrixMetricsPublisherCommand(
            HystrixCommandKey commandKey,
            HystrixCommandGroupKey commandGroupKey,
            HystrixCommandMetrics metrics,
            HystrixCircuitBreaker circuitBreaker,
            HystrixCommandProperties properties,
            MeterRegistry meterRegistry) {
        
        this.commandKey = commandKey;
        this.metrics = metrics;
        this.meterRegistry = meterRegistry;
        
        // Register metrics
        registerMetrics();
    }
    
    private void registerMetrics() {
        // Success rate
        Gauge.builder("hystrix.command.success.rate", metrics, 
                m -> m.getHealthCounts().getSuccessPercentage())
            .tag("command", commandKey.name())
            .register(meterRegistry);
        
        // Error rate
        Gauge.builder("hystrix.command.error.rate", metrics,
                m -> m.getHealthCounts().getErrorPercentage())
            .tag("command", commandKey.name())
            .register(meterRegistry);
        
        // Circuit breaker status
        Gauge.builder("hystrix.circuit.breaker.open", metrics,
                m -> m.isCircuitBreakerOpen() ? 1.0 : 0.0)
            .tag("command", commandKey.name())
            .register(meterRegistry);
    }
    
    @Override
    public void initialize() {
        // Initialization logic
    }
}

// Hystrix Event Stream
@RestController
public class HystrixStreamController {
    
    @GetMapping(value = "/hystrix.stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> hystrixStream() {
        return Flux.interval(Duration.ofSeconds(1))
            .map(i -> getHystrixMetrics())
            .map(metrics -> "data: " + metrics + "\n\n");
    }
    
    private String getHystrixMetrics() {
        // Collect and format Hystrix metrics
        JSONObject metrics = new JSONObject();
        
        HystrixCommandMetrics.getInstances().forEach(commandMetrics -> {
            JSONObject commandData = new JSONObject();
            commandData.put("errorPercentage", commandMetrics.getHealthCounts().getErrorPercentage());
            commandData.put("requestCount", commandMetrics.getHealthCounts().getTotalRequests());
            commandData.put("isCircuitBreakerOpen", commandMetrics.isCircuitBreakerOpen());
            
            metrics.put(commandMetrics.getCommandKey().name(), commandData);
        });
        
        return metrics.toString();
    }
}
```

---

## 4. Zuul - API Gateway

### 4.1 Zuul Gateway Setup

```java
// Zuul Gateway Application
@SpringBootApplication
@EnableZuulProxy
@EnableDiscoveryClient
public class ZuulGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ZuulGatewayApplication.class, args);
    }
}

// Zuul Configuration
@Configuration
public class ZuulConfig {
    
    /*
    # application.yml
    spring:
      application:
        name: zuul-gateway
        
    server:
      port: 8080
      
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:8761/eureka/
          
    zuul:
      prefix: /api
      strip-prefix: true
      sensitive-headers: Cookie,Set-Cookie  # Don't strip these headers
      
      routes:
        user-service:
          path: /users/**
          service-id: user-service
          strip-prefix: false
          
        order-service:
          path: /orders/**
          service-id: order-service
          
        product-service:
          path: /products/**
          url: http://localhost:8082  # Direct URL routing
          
      # Route with custom configuration
      routes:
        auth-service:
          path: /auth/**
          service-id: auth-service
          sensitive-headers:  # Empty means forward all headers
          custom-sensitive-headers: true
          
      # Disable auto-discovery for specific services
      ignored-services: admin-service,internal-service
      
      # Rate limiting
      ratelimit:
        enabled: true
        repository: REDIS
        default-policy-list:
          - limit: 10
            refresh-interval: 60
            type:
              - user
              - origin
              - url
              
      # Retry configuration
      retryable: true
      
    ribbon:
      ConnectTimeout: 3000
      ReadTimeout: 60000
      
    hystrix:
      command:
        default:
          execution:
            isolation:
              thread:
                timeoutInMilliseconds: 60000
    */
}

// Custom Zuul Filter
@Component
public class AuthenticationFilter extends ZuulFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }
    
    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1;
    }
    
    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        String uri = context.getRequest().getRequestURI();
        
        // Skip authentication for public endpoints
        return !isPublicEndpoint(uri);
    }
    
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        
        String token = extractToken(request);
        
        if (token == null || !tokenProvider.validateToken(token)) {
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            context.setResponseBody("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}");
            context.getResponse().setContentType("application/json");
            return null;
        }
        
        // Add user information to request headers
        Long userId = tokenProvider.getUserIdFromToken(token);
        String userRole = tokenProvider.getUserRoleFromToken(token);
        
        context.addZuulRequestHeader("X-User-Id", userId.toString());
        context.addZuulRequestHeader("X-User-Role", userRole);
        
        return null;
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/auth/login") ||
               uri.startsWith("/api/auth/register") ||
               uri.startsWith("/api/public") ||
               uri.startsWith("/actuator/health");
    }
}

// Logging Filter
@Component
public class LoggingFilter extends ZuulFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    
    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }
    
    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER - 1;
    }
    
    @Override
    public boolean shouldFilter() {
        return true;
    }
    
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        HttpServletResponse response = context.getResponse();
        
        long startTime = (Long) context.get("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Request: {} {} - Status: {} - Duration: {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
        
        // Add response headers
        response.addHeader("X-Response-Time", duration + "ms");
        
        return null;
    }
}

// Rate Limiting Filter
@Component
public class RateLimitingFilter extends ZuulFilter {
    
    @Autowired
    private RateLimiter rateLimiter;
    
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }
    
    @Override
    public int filterOrder() {
        return FilterConstants.FORM_BODY_WRAPPER_FILTER_ORDER - 1;
    }
    
    @Override
    public boolean shouldFilter() {
        return true;
    }
    
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        
        String key = generateKey(request);
        
        if (!rateLimiter.tryAcquire(key)) {
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(HttpStatus.TOO_MANY_REQUESTS.value());
            context.setResponseBody("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests\"}");
            context.getResponse().setContentType("application/json");
            
            // Add rate limit headers
            RateLimitInfo info = rateLimiter.getRateLimitInfo(key);
            context.getResponse().addHeader("X-RateLimit-Limit", String.valueOf(info.getLimit()));
            context.getResponse().addHeader("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
            context.getResponse().addHeader("X-RateLimit-Reset", String.valueOf(info.getReset()));
        }
        
        return null;
    }
    
    private String generateKey(HttpServletRequest request) {
        // Generate rate limit key based on user or IP
        String userId = request.getHeader("X-User-Id");
        if (userId != null) {
            return "user:" + userId;
        }
        return "ip:" + request.getRemoteAddr();
    }
}

// Error Filter
@Component
public class ErrorFilter extends ZuulFilter {
    
    @Override
    public String filterType() {
        return FilterConstants.ERROR_TYPE;
    }
    
    @Override
    public int filterOrder() {
        return FilterConstants.SEND_ERROR_FILTER_ORDER;
    }
    
    @Override
    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getThrowable() != null;
    }
    
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        Throwable throwable = context.getThrowable();
        
        logger.error("Error during filtering", throwable);
        
        context.setResponseStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        context.setResponseBody(buildErrorResponse(throwable));
        context.getResponse().setContentType("application/json");
        
        return null;
    }
    
    private String buildErrorResponse(Throwable throwable) {
        JSONObject error = new JSONObject();
        error.put("error", "Internal Server Error");
        error.put("message", throwable.getMessage());
        error.put("timestamp", System.currentTimeMillis());
        
        if (isDevelopmentMode()) {
            error.put("exception", throwable.getClass().getName());
            error.put("trace", getStackTrace(throwable));
        }
        
        return error.toString();
    }
    
    private boolean isDevelopmentMode() {
        return "dev".equals(System.getProperty("spring.profiles.active"));
    }
    
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
```

### 4.2 Advanced Zuul Features

```java
// Dynamic Routing
@Component
public class DynamicRouteLocator extends SimpleRouteLocator {
    
    @Autowired
    private RouteRepository routeRepository;
    
    public DynamicRouteLocator(String servletPath, ZuulProperties properties) {
        super(servletPath, properties);
    }
    
    @Override
    protected Map<String, ZuulRoute> locateRoutes() {
        LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<>();
        
        // Load routes from parent
        routesMap.putAll(super.locateRoutes());
        
        // Load dynamic routes from database
        routeRepository.findAll().forEach(route -> {
            ZuulRoute zuulRoute = new ZuulRoute();
            zuulRoute.setId(route.getId());
            zuulRoute.setPath(route.getPath());
            zuulRoute.setServiceId(route.getServiceId());
            zuulRoute.setUrl(route.getUrl());
            zuulRoute.setStripPrefix(route.isStripPrefix());
            zuulRoute.setRetryable(route.isRetryable());
            
            routesMap.put(route.getPath(), zuulRoute);
        });
        
        return routesMap;
    }
}

// Route Management API
@RestController
@RequestMapping("/admin/routes")
public class RouteManagementController {
    
    @Autowired
    private RouteRepository routeRepository;
    
    @Autowired
    private RouteLocator routeLocator;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @GetMapping
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }
    
    @PostMapping
    public Route createRoute(@RequestBody Route route) {
        Route savedRoute = routeRepository.save(route);
        refreshRoutes();
        return savedRoute;
    }
    
    @PutMapping("/{id}")
    public Route updateRoute(@PathVariable String id, @RequestBody Route route) {
        route.setId(id);
        Route updatedRoute = routeRepository.save(route);
        refreshRoutes();
        return updatedRoute;
    }
    
    @DeleteMapping("/{id}")
    public void deleteRoute(@PathVariable String id) {
        routeRepository.deleteById(id);
        refreshRoutes();
    }
    
    @PostMapping("/refresh")
    public void refreshRoutes() {
        eventPublisher.publishEvent(new RoutesRefreshedEvent(routeLocator));
    }
}

// Request Modification Filter
@Component
public class RequestModificationFilter extends ZuulFilter {
    
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }
    
    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 1;
    }
    
    @Override
    public boolean shouldFilter() {
        return true;
    }
    
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        
        // Add correlation ID
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        context.addZuulRequestHeader("X-Correlation-ID", correlationId);
        
        // Add timestamp
        context.addZuulRequestHeader("X-Request-Time", 
                String.valueOf(System.currentTimeMillis()));
        
        // Modify request body if needed
        if ("POST".equals(request.getMethod()) && 
            request.getContentType() != null && 
            request.getContentType().contains("application/json")) {
            
            try {
                String body = StreamUtils.copyToString(request.getInputStream(), 
                        StandardCharsets.UTF_8);
                
                // Modify body
                JSONObject jsonBody = new JSONObject(body);
                jsonBody.put("gatewayTimestamp", System.currentTimeMillis());
                
                context.setRequest(new CustomHttpServletRequestWrapper(request, 
                        jsonBody.toString()));
                
            } catch (IOException e) {
                logger.error("Error reading request body", e);
            }
        }
        
        return null;
    }
}

// Response Modification Filter
@Component
public class ResponseModificationFilter extends ZuulFilter {
    
    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }
    
    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER - 2;
    }
    
    @Override
    public boolean shouldFilter() {
        return true;
    }
    
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        
        // Modify response headers
        HttpServletResponse response = context.getResponse();
        response.addHeader("X-Gateway-Version", "1.0.0");
        
        // Modify response body if JSON
        String contentType = response.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                String responseBody = context.getResponseBody();
                if (responseBody == null) {
                    InputStream stream = context.getResponseDataStream();
                    responseBody = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
                }
                
                // Modify response
                JSONObject jsonResponse = new JSONObject(responseBody);
                jsonResponse.put("gatewayProcessed", true);
                jsonResponse.put("processedAt", System.currentTimeMillis());
                
                context.setResponseBody(jsonResponse.toString());
                
            } catch (Exception e) {
                logger.error("Error modifying response", e);
            }
        }
        
        return null;
    }
}

// Fallback Provider for Zuul
@Component
public class CustomFallbackProvider implements FallbackProvider {
    
    @Override
    public String getRoute() {
        return "*"; // Apply to all routes
    }
    
    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            
            @Override
            public int getRawStatusCode() throws IOException {
                return HttpStatus.SERVICE_UNAVAILABLE.value();
            }
            
            @Override
            public String getStatusText() throws IOException {
                return HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase();
            }
            
            @Override
            public void close() {
            }
            
            @Override
            public InputStream getBody() throws IOException {
                JSONObject fallback = new JSONObject();
                fallback.put("error", "Service Unavailable");
                fallback.put("message", "The service is temporarily unavailable");
                fallback.put("route", route);
                fallback.put("timestamp", System.currentTimeMillis());
                
                if (cause != null) {
                    fallback.put("cause", cause.getMessage());
                }
                
                return new ByteArrayInputStream(fallback.toString().getBytes());
            }
            
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }
}
```

---

## 5. Integration Examples

### 5.1 Complete Microservice with Netflix Stack

```java
// Complete User Service with Netflix Components
@SpringBootApplication
@EnableEurekaClient
@EnableCircuitBreaker
@EnableFeignClients
@EnableHystrixDashboard
public class UserServiceCompleteApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceCompleteApplication.class, args);
    }
}

// Feign Client with Hystrix
@FeignClient(name = "order-service", fallback = OrderServiceFallback.class)
public interface OrderServiceClient {
    
    @GetMapping("/api/orders/user/{userId}")
    List<Order> getUserOrders(@PathVariable("userId") Long userId);
    
    @GetMapping("/api/orders/{orderId}")
    Order getOrder(@PathVariable("orderId") Long orderId);
    
    @PostMapping("/api/orders")
    Order createOrder(@RequestBody CreateOrderRequest request);
}

@Component
public class OrderServiceFallback implements OrderServiceClient {
    
    @Override
    public List<Order> getUserOrders(Long userId) {
        logger.warn("Fallback: getUserOrders for userId: {}", userId);
        return Collections.emptyList();
    }
    
    @Override
    public Order getOrder(Long orderId) {
        logger.warn("Fallback: getOrder for orderId: {}", orderId);
        Order fallbackOrder = new Order();
        fallbackOrder.setId(orderId);
        fallbackOrder.setStatus("UNKNOWN");
        return fallbackOrder;
    }
    
    @Override
    public Order createOrder(CreateOrderRequest request) {
        logger.warn("Fallback: createOrder");
        throw new ServiceUnavailableException("Order service is unavailable");
    }
}

// Service Implementation
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderServiceClient orderServiceClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
    @HystrixCommand(
        fallbackMethod = "getUserWithOrdersFallback",
        commandProperties = {
            @HystrixProperty(name = "execution.isolation.strategy", value = "THREAD"),
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50"),
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000")
        }
    )
    public UserWithOrders getUserWithOrders(Long userId) {
        // Get user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Get orders from order service
        List<Order> orders = orderServiceClient.getUserOrders(userId);
        
        // Build response
        UserWithOrders response = new UserWithOrders();
        response.setUser(user);
        response.setOrders(orders);
        response.setOrderCount(orders.size());
        
        return response;
    }
    
    public UserWithOrders getUserWithOrdersFallback(Long userId) {
        // Get user from cache or database
        User user = userRepository.findById(userId)
                .orElse(createFallbackUser(userId));
        
        UserWithOrders response = new UserWithOrders();
        response.setUser(user);
        response.setOrders(Collections.emptyList());
        response.setOrderCount(0);
        response.setFallback(true);
        response.setMessage("Order service is temporarily unavailable");
        
        return response;
    }
    
    @Override
    @HystrixCommand(
        fallbackMethod = "getAllUsersFallback",
        threadPoolKey = "userServiceThreadPool",
        threadPoolProperties = {
            @HystrixProperty(name = "coreSize", value = "20"),
            @HystrixProperty(name = "maxQueueSize", value = "10")
        }
    )
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> getAllUsersFallback() {
        logger.warn("Fallback: getAllUsers");
        // Return cached data or empty list
        return getCachedUsers();
    }
    
    private User createFallbackUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setName("Unknown User");
        user.setEmail("unknown@example.com");
        return user;
    }
    
    private List<User> getCachedUsers() {
        // Implementation to get cached users
        return Collections.emptyList();
    }
}

// Controller with Circuit Breaker Dashboard
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{userId}/with-orders")
    public ResponseEntity<UserWithOrders> getUserWithOrders(@PathVariable Long userId) {
        UserWithOrders response = userService.getUserWithOrders(userId);
        
        if (response.isFallback()) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("hystrix", getHystrixHealth());
        health.put("eureka", getEurekaHealth());
        
        return ResponseEntity.ok(health);
    }
    
    private Map<String, Object> getHystrixHealth() {
        Map<String, Object> hystrixHealth = new HashMap<>();
        
        HystrixCommandMetrics.getInstances().forEach(metrics -> {
            Map<String, Object> commandHealth = new HashMap<>();
            commandHealth.put("errorPercentage", metrics.getHealthCounts().getErrorPercentage());
            commandHealth.put("requestCount", metrics.getHealthCounts().getTotalRequests());
            commandHealth.put("isCircuitBreakerOpen", metrics.isCircuitBreakerOpen());
            
            hystrixHealth.put(metrics.getCommandKey().name(), commandHealth);
        });
        
        return hystrixHealth;
    }
    
    private Map<String, Object> getEurekaHealth() {
        Map<String, Object> eurekaHealth = new HashMap<>();
        // Implementation to check Eureka health
        return eurekaHealth;
    }
}
```

---

## 6. Best Practices and Production Considerations

### 6.1 Production Configuration

```java
// Production-Ready Configuration
@Configuration
@Profile("production")
public class ProductionConfig {
    
    // Eureka Configuration for Production
    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfig() {
        EurekaInstanceConfigBean config = new EurekaInstanceConfigBean();
        
        // Use IP address instead of hostname
        config.setPreferIpAddress(true);
        
        // Set instance ID
        config.setInstanceId(
            String.format("%s:%s:%d",
                config.getHostname(),
                config.getAppname(),
                config.getNonSecurePort()
            )
        );
        
        // Health check
        config.setHealthCheckUrlPath("/actuator/health");
        config.setStatusPageUrlPath("/actuator/info");
        
        // Lease configuration
        config.setLeaseRenewalIntervalInSeconds(10);
        config.setLeaseExpirationDurationInSeconds(30);
        
        return config;
    }
    
    // Hystrix Configuration for Production
    @Bean
    public HystrixCommandAspect hystrixCommandAspect() {
        // Configure Hystrix for production
        HystrixPlugins.getInstance().registerMetricsPublisher(new CustomHystrixMetricsPublisher());
        HystrixPlugins.getInstance().registerConcurrencyStrategy(new CustomHystrixConcurrencyStrategy());
        
        return new HystrixCommandAspect();
    }
    
    // Zuul Configuration for Production
    @Bean
    public ZuulProperties zuulProperties() {
        ZuulProperties properties = new ZuulProperties();
        
        // Connection settings
        properties.setHost(new ZuulProperties.Host());
        properties.getHost().setConnectTimeoutMillis(10000);
        properties.getHost().setSocketTimeoutMillis(60000);
        properties.getHost().setMaxTotalConnections(500);
        properties.getHost().setMaxPerRouteConnections(50);
        
        // Retry settings
        properties.setRetryable(true);
        properties.setRibbonIsolationStrategy(THREAD);
        
        return properties;
    }
}

// Monitoring and Alerting
@Component
public class NetflixStackMonitoring {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 60000)
    public void monitorCircuitBreakers() {
        HystrixCommandMetrics.getInstances().forEach(metrics -> {
            String commandKey = metrics.getCommandKey().name();
            
            // Record metrics
            meterRegistry.gauge("hystrix.circuit.breaker.open",
                Tags.of("command", commandKey),
                metrics.isCircuitBreakerOpen() ? 1 : 0);
            
            meterRegistry.gauge("hystrix.error.percentage",
                Tags.of("command", commandKey),
                metrics.getHealthCounts().getErrorPercentage());
            
            // Alert if circuit breaker is open
            if (metrics.isCircuitBreakerOpen()) {
                sendAlert("Circuit breaker open for command: " + commandKey);
            }
            
            // Alert if error rate is high
            if (metrics.getHealthCounts().getErrorPercentage() > 25) {
                sendAlert("High error rate for command: " + commandKey);
            }
        });
    }
    
    @Scheduled(fixedRate = 60000)
    public void monitorEurekaInstances() {
        EurekaClient eurekaClient = EurekaClientAutoConfiguration.RefreshableEurekaClientConfiguration.eurekaClient;
        
        Applications applications = eurekaClient.getApplications();
        applications.getRegisteredApplications().forEach(app -> {
            int totalInstances = app.getInstances().size();
            long healthyInstances = app.getInstances().stream()
                    .filter(instance -> instance.getStatus() == InstanceStatus.UP)
                    .count();
            
            meterRegistry.gauge("eureka.instances.total",
                Tags.of("application", app.getName()),
                totalInstances);
            
            meterRegistry.gauge("eureka.instances.healthy",
                Tags.of("application", app.getName()),
                healthyInstances);
            
            // Alert if no healthy instances
            if (healthyInstances == 0 && totalInstances > 0) {
                sendAlert("No healthy instances for application: " + app.getName());
            }
        });
    }
    
    private void sendAlert(String message) {
        // Send alert to monitoring system
        logger.error("ALERT: {}", message);
    }
}
```

---

## 7. Summary and Key Takeaways

### Components Mastered

1. **Eureka - Service Discovery**
   - Service registration and discovery
   - Client-side load balancing
   - Health monitoring
   - High availability setup

2. **Hystrix - Circuit Breaker**
   - Fault tolerance patterns
   - Fallback mechanisms
   - Dashboard and monitoring
   - Thread isolation

3. **Zuul - API Gateway**
   - Dynamic routing
   - Filter pipeline
   - Rate limiting
   - Authentication/Authorization

### Best Practices

✅ **Use circuit breakers for all external calls**  
✅ **Implement meaningful fallbacks**  
✅ **Monitor circuit breaker states**  
✅ **Configure timeouts appropriately**  
✅ **Use Eureka for service discovery**  
✅ **Implement health checks**  
✅ **Use Zuul filters for cross-cutting concerns**  

### Common Pitfalls to Avoid

❌ **Not tuning Hystrix thread pools**  
❌ **Missing fallback implementations**  
❌ **Incorrect timeout configurations**  
❌ **Not monitoring circuit breaker metrics**  
❌ **Hardcoding service URLs**  

### Next Steps

Tomorrow, we'll explore Spring Cloud Gateway as the modern alternative to Zuul, along with advanced load balancing strategies.

---

**Practice Assignment**: Build a complete microservice architecture with Eureka for service discovery, Hystrix for circuit breaking, and Zuul as the API gateway. Implement custom filters and fallback mechanisms.

---

*"Netflix OSS components have proven their worth at scale - they're not just patterns, they're battle-tested solutions for real-world distributed systems challenges."*