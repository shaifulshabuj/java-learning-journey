# Day 61: Service Discovery & Load Balancing

## Learning Objectives
- Master service discovery patterns and implementations
- Understand client-side vs server-side discovery
- Implement health checking and service registration
- Apply advanced load balancing strategies
- Build resilient service discovery mechanisms

---

## 1. Service Discovery Fundamentals

### 1.1 Service Discovery Patterns

In microservices architecture, service discovery is crucial for services to locate and communicate with each other dynamically.

```java
// Hard-coded Service URLs (Anti-pattern)
@Service
public class OrderService {
    
    // ❌ Hard-coded URLs - not scalable
    private final String customerServiceUrl = "http://localhost:8081";
    private final String productServiceUrl = "http://localhost:8082";
    private final String paymentServiceUrl = "http://localhost:8083";
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Order createOrder(CreateOrderRequest request) {
        // Hard-coded service calls
        Customer customer = restTemplate.getForObject(
            customerServiceUrl + "/api/v1/customers/" + request.getCustomerId(), 
            Customer.class);
        
        Product product = restTemplate.getForObject(
            productServiceUrl + "/api/v1/products/" + request.getProductId(), 
            Product.class);
        
        // More hard-coded calls...
        return new Order();
    }
}

// Service Discovery Pattern (Correct approach)
@Service
public class DiscoveryAwareOrderService {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Order createOrder(CreateOrderRequest request) {
        // Dynamic service discovery
        Customer customer = getCustomer(request.getCustomerId());
        Product product = getProduct(request.getProductId());
        PaymentResult payment = processPayment(request.getPaymentDetails());
        
        return buildOrder(customer, product, payment);
    }
    
    private Customer getCustomer(Long customerId) {
        ServiceInstance instance = loadBalancerClient.choose("customer-service");
        if (instance == null) {
            throw new ServiceUnavailableException("Customer service not available");
        }
        
        String url = String.format("http://%s:%d/api/v1/customers/%d", 
            instance.getHost(), instance.getPort(), customerId);
        
        return restTemplate.getForObject(url, Customer.class);
    }
    
    private Product getProduct(Long productId) {
        List<ServiceInstance> instances = discoveryClient.getInstances("product-service");
        if (instances.isEmpty()) {
            throw new ServiceUnavailableException("Product service not available");
        }
        
        // Simple round-robin selection
        ServiceInstance instance = instances.get(productId.intValue() % instances.size());
        
        String url = String.format("http://%s:%d/api/v1/products/%d", 
            instance.getHost(), instance.getPort(), productId);
        
        return restTemplate.getForObject(url, Product.class);
    }
    
    private PaymentResult processPayment(PaymentDetails details) {
        ServiceInstance instance = loadBalancerClient.choose("payment-service");
        if (instance == null) {
            throw new ServiceUnavailableException("Payment service not available");
        }
        
        String url = String.format("http://%s:%d/api/v1/payments", 
            instance.getHost(), instance.getPort());
        
        return restTemplate.postForObject(url, details, PaymentResult.class);
    }
}
```

---

## 2. Netflix Eureka Implementation

### 2.1 Eureka Server Configuration

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
    
    @Bean
    public EurekaServerConfigBean eurekaServerConfigBean() {
        EurekaServerConfigBean config = new EurekaServerConfigBean();
        config.setEnableSelfPreservation(true);
        config.setRenewalPercentThreshold(0.85);
        config.setEvictionIntervalTimerInMs(60000);
        config.setResponseCacheUpdateIntervalMs(30000);
        config.setDeltaRetentionTimerIntervalInMs(30000);
        return config;
    }
    
    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
        EurekaInstanceConfigBean config = new EurekaInstanceConfigBean();
        config.setInstanceId("eureka-server-1");
        config.setPreferIpAddress(true);
        config.setLeaseRenewalIntervalInSeconds(30);
        config.setLeaseExpirationDurationInSeconds(90);
        return config;
    }
}
```

### 2.2 Service Registration

```java
// Customer Service Registration
@SpringBootApplication
@EnableEurekaClient
@EnableDiscoveryClient
public class CustomerServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
    
    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfig() {
        EurekaInstanceConfigBean config = new EurekaInstanceConfigBean();
        config.setInstanceId("customer-service-" + UUID.randomUUID().toString());
        config.setPreferIpAddress(true);
        config.setLeaseRenewalIntervalInSeconds(30);
        config.setLeaseExpirationDurationInSeconds(90);
        
        // Health check configuration
        config.setHealthCheckUrl("http://localhost:8081/actuator/health");
        config.setStatusPageUrl("http://localhost:8081/actuator/info");
        config.setHomePageUrl("http://localhost:8081/");
        
        // Metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("environment", "production");
        metadata.put("team", "customer-team");
        config.setMetadataMap(metadata);
        
        return config;
    }
}

// Custom Service Registration
@Component
public class CustomServiceRegistration {
    
    @Autowired
    private EurekaClient eurekaClient;
    
    @Autowired
    private ApplicationInfoManager applicationInfoManager;
    
    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        // Custom registration logic
        registerService();
    }
    
    private void registerService() {
        try {
            InstanceInfo instanceInfo = applicationInfoManager.getInfo();
            
            // Add custom metadata
            Map<String, String> metadata = new HashMap<>(instanceInfo.getMetadata());
            metadata.put("startup-time", String.valueOf(System.currentTimeMillis()));
            metadata.put("git-commit", getGitCommit());
            metadata.put("build-version", getBuildVersion());
            
            InstanceInfo updatedInfo = new InstanceInfo.Builder(instanceInfo)
                .setMetadata(metadata)
                .build();
            
            applicationInfoManager.setInstanceInfo(updatedInfo);
            
            logger.info("Service registered with Eureka: {}", instanceInfo.getAppName());
            
        } catch (Exception e) {
            logger.error("Failed to register service with Eureka", e);
        }
    }
    
    private String getGitCommit() {
        try {
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/git.properties"));
            return props.getProperty("git.commit.id.abbrev", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String getBuildVersion() {
        try {
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/build.properties"));
            return props.getProperty("build.version", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
```

### 2.3 Service Discovery Client

```java
// Service Discovery Configuration
@Configuration
@EnableDiscoveryClient
public class ServiceDiscoveryConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        template.setErrorHandler(new ServiceDiscoveryErrorHandler());
        return template;
    }
    
    @Bean
    public DiscoveryClient discoveryClient() {
        return new CompositeDiscoveryClient(
            Arrays.asList(
                new EurekaDiscoveryClient(),
                new ConsulDiscoveryClient()
            )
        );
    }
}

// Service Discovery Service
@Service
public class ServiceDiscoveryService {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    
    public List<ServiceInstance> getAvailableInstances(String serviceName) {
        return discoveryClient.getInstances(serviceName);
    }
    
    public ServiceInstance chooseInstance(String serviceName) {
        return loadBalancerClient.choose(serviceName);
    }
    
    public List<String> getServices() {
        return discoveryClient.getServices();
    }
    
    public ServiceInstance getHealthyInstance(String serviceName) {
        List<ServiceInstance> instances = getAvailableInstances(serviceName);
        
        return instances.stream()
            .filter(this::isHealthy)
            .findFirst()
            .orElse(null);
    }
    
    private boolean isHealthy(ServiceInstance instance) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String healthUrl = String.format("http://%s:%d/actuator/health", 
                instance.getHost(), instance.getPort());
            
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                return "UP".equals(body.get("status"));
            }
            
            return false;
            
        } catch (Exception e) {
            logger.warn("Health check failed for instance {}:{}: {}", 
                instance.getHost(), instance.getPort(), e.getMessage());
            return false;
        }
    }
    
    public Map<String, Object> getServiceMetadata(String serviceName) {
        List<ServiceInstance> instances = getAvailableInstances(serviceName);
        
        return instances.stream()
            .findFirst()
            .map(ServiceInstance::getMetadata)
            .orElse(Collections.emptyMap());
    }
}
```

---

## 3. Health Checking Mechanisms

### 3.1 Comprehensive Health Checks

```java
// Custom Health Indicator
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .withDetail("connection.pool.active", getActiveConnections())
                    .withDetail("connection.pool.idle", getIdleConnections())
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "Connection invalid")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "Connection failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private int getActiveConnections() {
        // Implementation depends on connection pool
        return 0;
    }
    
    private int getIdleConnections() {
        // Implementation depends on connection pool
        return 0;
    }
}

// Dependency Health Indicator
@Component
public class DependencyHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${app.dependencies.customer-service.url}")
    private String customerServiceUrl;
    
    @Value("${app.dependencies.product-service.url}")
    private String productServiceUrl;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Check customer service
        boolean customerServiceHealthy = checkServiceHealth(customerServiceUrl + "/actuator/health");
        builder.withDetail("customer-service", customerServiceHealthy ? "UP" : "DOWN");
        
        // Check product service
        boolean productServiceHealthy = checkServiceHealth(productServiceUrl + "/actuator/health");
        builder.withDetail("product-service", productServiceHealthy ? "UP" : "DOWN");
        
        // Overall health
        if (!customerServiceHealthy || !productServiceHealthy) {
            builder.down();
        }
        
        return builder.build();
    }
    
    private boolean checkServiceHealth(String healthUrl) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                return "UP".equals(body.get("status"));
            }
            
            return false;
            
        } catch (Exception e) {
            logger.warn("Health check failed for URL {}: {}", healthUrl, e.getMessage());
            return false;
        }
    }
}

// Circuit Breaker Health Indicator
@Component
public class CircuitBreakerHealthIndicator implements HealthIndicator {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            
            builder.withDetail(circuitBreaker.getName(), Map.of(
                "state", state.name(),
                "failure-rate", metrics.getFailureRate(),
                "slow-call-rate", metrics.getSlowCallRate(),
                "buffered-calls", metrics.getNumberOfBufferedCalls(),
                "successful-calls", metrics.getNumberOfSuccessfulCalls(),
                "failed-calls", metrics.getNumberOfFailedCalls()
            ));
            
            if (state == CircuitBreaker.State.OPEN) {
                builder.down();
            }
        });
        
        return builder.build();
    }
}

// Composite Health Indicator
@Component
public class ApplicationHealthIndicator implements HealthIndicator {
    
    @Autowired
    private List<HealthIndicator> healthIndicators;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        boolean allHealthy = true;
        
        for (HealthIndicator indicator : healthIndicators) {
            try {
                Health health = indicator.health();
                String indicatorName = indicator.getClass().getSimpleName();
                
                builder.withDetail(indicatorName, health.getDetails());
                
                if (health.getStatus() != Status.UP) {
                    allHealthy = false;
                }
                
            } catch (Exception e) {
                logger.error("Health check failed for indicator {}: {}", 
                    indicator.getClass().getSimpleName(), e.getMessage());
                allHealthy = false;
            }
        }
        
        if (!allHealthy) {
            builder.down();
        }
        
        return builder.build();
    }
}
```

### 3.2 Health Check Endpoint Configuration

```java
// Health Check Configuration
@Configuration
public class HealthCheckConfig {
    
    @Bean
    public HealthEndpoint healthEndpoint() {
        return new HealthEndpoint(healthContributorRegistry(), healthEndpointGroups());
    }
    
    @Bean
    public HealthContributorRegistry healthContributorRegistry() {
        return new DefaultHealthContributorRegistry();
    }
    
    @Bean
    public HealthEndpointGroups healthEndpointGroups() {
        return HealthEndpointGroups.of(
            Map.of("liveness", HealthEndpointGroup.of(
                Set.of("livenessState", "database", "diskSpace"),
                StatusAggregator.getDefault(),
                HttpCodeStatusMapper.DEFAULT,
                SecurityStrategy.WHEN_AUTHORIZED,
                Set.of("never")
            )),
            Map.of("readiness", HealthEndpointGroup.of(
                Set.of("readinessState", "database", "dependencies"),
                StatusAggregator.getDefault(),
                HttpCodeStatusMapper.DEFAULT,
                SecurityStrategy.WHEN_AUTHORIZED,
                Set.of("never")
            ))
        );
    }
}

// Custom Health Check Controller
@RestController
@RequestMapping("/health")
public class HealthCheckController {
    
    @Autowired
    private ApplicationHealthIndicator applicationHealthIndicator;
    
    @Autowired
    private ServiceDiscoveryService serviceDiscoveryService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Health health = applicationHealthIndicator.health();
        
        HttpStatus status = health.getStatus() == Status.UP ? 
            HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("details", health.getDetails());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(status).body(response);
    }
    
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Health health = applicationHealthIndicator.health();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("details", health.getDetails());
        response.put("timestamp", System.currentTimeMillis());
        
        // Add service discovery information
        response.put("discoveredServices", serviceDiscoveryService.getServices());
        
        // Add runtime information
        Runtime runtime = Runtime.getRuntime();
        response.put("memory", Map.of(
            "total", runtime.totalMemory(),
            "free", runtime.freeMemory(),
            "used", runtime.totalMemory() - runtime.freeMemory()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        // Basic liveness check
        Map<String, Object> response = Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        // Check if service is ready to handle requests
        boolean ready = checkReadiness();
        
        Map<String, Object> response = Map.of(
            "status", ready ? "UP" : "DOWN",
            "timestamp", System.currentTimeMillis()
        );
        
        HttpStatus status = ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }
    
    private boolean checkReadiness() {
        // Check database connectivity
        try {
            Health health = applicationHealthIndicator.health();
            return health.getStatus() == Status.UP;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 4. Load Balancing Strategies

### 4.1 Custom Load Balancer Implementation

```java
// Round Robin Load Balancer
@Component
public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public RoundRobinLoadBalancer(String serviceId,
                                 ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
            .getIfAvailable(NoopServiceInstanceListSupplier::new);
        
        return supplier.get(request)
            .next()
            .map(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    return new EmptyResponse();
                }
                
                int index = counter.getAndIncrement() % serviceInstances.size();
                ServiceInstance instance = serviceInstances.get(index);
                
                return new DefaultResponse(instance);
            });
    }
}

// Weighted Round Robin Load Balancer
@Component
public class WeightedRoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final Map<ServiceInstance, Integer> weights = new ConcurrentHashMap<>();
    private final Map<ServiceInstance, Integer> currentWeights = new ConcurrentHashMap<>();
    
    public WeightedRoundRobinLoadBalancer(String serviceId,
                                         ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
            .getIfAvailable(NoopServiceInstanceListSupplier::new);
        
        return supplier.get(request)
            .next()
            .map(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    return new EmptyResponse();
                }
                
                ServiceInstance selectedInstance = selectByWeight(serviceInstances);
                return new DefaultResponse(selectedInstance);
            });
    }
    
    private ServiceInstance selectByWeight(List<ServiceInstance> instances) {
        // Initialize weights if not present
        for (ServiceInstance instance : instances) {
            weights.computeIfAbsent(instance, this::getInstanceWeight);
            currentWeights.computeIfAbsent(instance, k -> 0);
        }
        
        // Find instance with highest current weight
        ServiceInstance selected = null;
        int maxWeight = Integer.MIN_VALUE;
        int totalWeight = 0;
        
        for (ServiceInstance instance : instances) {
            int weight = weights.get(instance);
            int currentWeight = currentWeights.get(instance) + weight;
            
            currentWeights.put(instance, currentWeight);
            totalWeight += weight;
            
            if (currentWeight > maxWeight) {
                maxWeight = currentWeight;
                selected = instance;
            }
        }
        
        // Decrease selected instance's current weight
        if (selected != null) {
            currentWeights.put(selected, currentWeights.get(selected) - totalWeight);
        }
        
        return selected;
    }
    
    private int getInstanceWeight(ServiceInstance instance) {
        // Get weight from metadata or use default
        String weightStr = instance.getMetadata().get("weight");
        try {
            return weightStr != null ? Integer.parseInt(weightStr) : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}

// Least Connection Load Balancer
@Component
public class LeastConnectionLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final Map<ServiceInstance, AtomicInteger> connections = new ConcurrentHashMap<>();
    
    public LeastConnectionLoadBalancer(String serviceId,
                                      ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
            .getIfAvailable(NoopServiceInstanceListSupplier::new);
        
        return supplier.get(request)
            .next()
            .map(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    return new EmptyResponse();
                }
                
                ServiceInstance selectedInstance = selectLeastConnected(serviceInstances);
                
                // Increment connection count
                connections.computeIfAbsent(selectedInstance, k -> new AtomicInteger(0))
                    .incrementAndGet();
                
                return new DefaultResponse(selectedInstance);
            });
    }
    
    private ServiceInstance selectLeastConnected(List<ServiceInstance> instances) {
        ServiceInstance selected = null;
        int minConnections = Integer.MAX_VALUE;
        
        for (ServiceInstance instance : instances) {
            int connectionCount = connections.computeIfAbsent(instance, k -> new AtomicInteger(0)).get();
            
            if (connectionCount < minConnections) {
                minConnections = connectionCount;
                selected = instance;
            }
        }
        
        return selected;
    }
    
    // Method to decrement connection count when request completes
    public void releaseConnection(ServiceInstance instance) {
        AtomicInteger counter = connections.get(instance);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
}
```

### 4.2 Health-Aware Load Balancing

```java
// Health-Aware Load Balancer
@Component
public class HealthAwareLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final WebClient webClient;
    private final Map<ServiceInstance, HealthStatus> healthCache = new ConcurrentHashMap<>();
    
    public HealthAwareLoadBalancer(String serviceId,
                                  ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                  WebClient webClient) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.webClient = webClient;
        
        // Start health check scheduler
        startHealthCheckScheduler();
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
            .getIfAvailable(NoopServiceInstanceListSupplier::new);
        
        return supplier.get(request)
            .next()
            .map(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    return new EmptyResponse();
                }
                
                // Filter healthy instances
                List<ServiceInstance> healthyInstances = serviceInstances.stream()
                    .filter(this::isHealthy)
                    .collect(Collectors.toList());
                
                if (healthyInstances.isEmpty()) {
                    logger.warn("No healthy instances available for service: {}", serviceId);
                    // Return the first available instance as fallback
                    return new DefaultResponse(serviceInstances.get(0));
                }
                
                // Select from healthy instances using weighted round-robin
                ServiceInstance selected = selectWeightedRoundRobin(healthyInstances);
                return new DefaultResponse(selected);
            });
    }
    
    private boolean isHealthy(ServiceInstance instance) {
        HealthStatus status = healthCache.get(instance);
        if (status == null) {
            // First time checking this instance
            checkHealthAsync(instance);
            return true; // Assume healthy initially
        }
        
        return status.isHealthy() && !status.isExpired();
    }
    
    private void checkHealthAsync(ServiceInstance instance) {
        String healthUrl = String.format("http://%s:%d/actuator/health", 
            instance.getHost(), instance.getPort());
        
        webClient.get()
            .uri(healthUrl)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(5))
            .subscribe(
                response -> {
                    boolean healthy = "UP".equals(response.get("status"));
                    healthCache.put(instance, new HealthStatus(healthy, System.currentTimeMillis()));
                },
                error -> {
                    healthCache.put(instance, new HealthStatus(false, System.currentTimeMillis()));
                    logger.warn("Health check failed for instance {}:{}: {}", 
                        instance.getHost(), instance.getPort(), error.getMessage());
                }
            );
    }
    
    private ServiceInstance selectWeightedRoundRobin(List<ServiceInstance> instances) {
        // Simple round-robin for now
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }
    
    private void startHealthCheckScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            healthCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    @Data
    @AllArgsConstructor
    private static class HealthStatus {
        private boolean healthy;
        private long timestamp;
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000; // 1 minute expiry
        }
    }
}
```

---

## 5. Load Balancer Configuration

### 5.1 Spring Cloud LoadBalancer Configuration

```java
// Load Balancer Configuration
@Configuration
@LoadBalancerClient(name = "customer-service", configuration = CustomerServiceLoadBalancerConfig.class)
@LoadBalancerClient(name = "product-service", configuration = ProductServiceLoadBalancerConfig.class)
@LoadBalancerClient(name = "order-service", configuration = OrderServiceLoadBalancerConfig.class)
public class LoadBalancerConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

// Customer Service Load Balancer Configuration
@Configuration
public class CustomerServiceLoadBalancerConfig {
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> customerServiceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinLoadBalancer(loadBalancerClientFactory
            .getLazyProvider(name, ServiceInstanceListSupplier.class), name);
    }
    
    @Bean
    public ServiceInstanceListSupplier customerServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()
            .withHealthChecks()
            .withCaching()
            .build(context);
    }
}

// Product Service Load Balancer Configuration
@Configuration
public class ProductServiceLoadBalancerConfig {
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> productServiceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new WeightedRoundRobinLoadBalancer(name, loadBalancerClientFactory
            .getLazyProvider(name, ServiceInstanceListSupplier.class));
    }
    
    @Bean
    public ServiceInstanceListSupplier productServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()
            .withHealthChecks()
            .withRetryAwareness()
            .build(context);
    }
}

// Dynamic Load Balancer Factory
@Component
public class DynamicLoadBalancerFactory {
    
    private final LoadBalancerClientFactory loadBalancerClientFactory;
    
    public DynamicLoadBalancerFactory(LoadBalancerClientFactory loadBalancerClientFactory) {
        this.loadBalancerClientFactory = loadBalancerClientFactory;
    }
    
    public ReactorLoadBalancer<ServiceInstance> createLoadBalancer(String serviceName, String strategy) {
        ObjectProvider<ServiceInstanceListSupplier> provider = 
            loadBalancerClientFactory.getLazyProvider(serviceName, ServiceInstanceListSupplier.class);
        
        return switch (strategy.toLowerCase()) {
            case "round-robin" -> new RoundRobinLoadBalancer(serviceName, provider);
            case "weighted-round-robin" -> new WeightedRoundRobinLoadBalancer(serviceName, provider);
            case "least-connection" -> new LeastConnectionLoadBalancer(serviceName, provider);
            case "health-aware" -> new HealthAwareLoadBalancer(serviceName, provider, WebClient.builder().build());
            default -> new RandomLoadBalancer(provider, serviceName);
        };
    }
}
```

### 5.2 Ribbon Configuration (Legacy)

```java
// Ribbon Configuration
@Configuration
public class RibbonConfig {
    
    @Bean
    @ConditionalOnProperty(value = "ribbon.enabled", havingValue = "true")
    public IRule ribbonRule() {
        return new WeightedResponseTimeRule();
    }
    
    @Bean
    @ConditionalOnProperty(value = "ribbon.enabled", havingValue = "true")
    public IPing ribbonPing() {
        return new PingUrl();
    }
    
    @Bean
    @ConditionalOnProperty(value = "ribbon.enabled", havingValue = "true")
    public ServerListUpdater ribbonServerListUpdater() {
        return new PollingServerListUpdater();
    }
}

// Custom Ribbon Rule
public class HealthAwareRule extends AbstractLoadBalancerRule {
    
    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        // Initialize with configuration
    }
    
    @Override
    public Server choose(Object key) {
        return choose(getLoadBalancer(), key);
    }
    
    public Server choose(ILoadBalancer lb, Object key) {
        if (lb == null) {
            return null;
        }
        
        List<Server> reachableServers = lb.getReachableServers();
        List<Server> allServers = lb.getAllServers();
        
        if (reachableServers.isEmpty()) {
            return null;
        }
        
        // Filter healthy servers
        List<Server> healthyServers = reachableServers.stream()
            .filter(this::isHealthy)
            .collect(Collectors.toList());
        
        if (healthyServers.isEmpty()) {
            // Fallback to reachable servers
            return reachableServers.get(ThreadLocalRandom.current().nextInt(reachableServers.size()));
        }
        
        // Select server based on load
        return selectByLoad(healthyServers);
    }
    
    private boolean isHealthy(Server server) {
        // Health check logic
        return true;
    }
    
    private Server selectByLoad(List<Server> servers) {
        // Simple round-robin for now
        return servers.get(ThreadLocalRandom.current().nextInt(servers.size()));
    }
}
```

---

## 6. Service Mesh Integration

### 6.1 Istio Integration

```yaml
# Istio Service Entry
apiVersion: networking.istio.io/v1beta1
kind: ServiceEntry
metadata:
  name: customer-service
spec:
  hosts:
  - customer-service.default.svc.cluster.local
  ports:
  - number: 8080
    name: http
    protocol: HTTP
  location: MESH_EXTERNAL
  resolution: DNS
```

```java
// Istio-aware Service Discovery
@Component
public class IstioServiceDiscovery {
    
    @Autowired
    private KubernetesClient kubernetesClient;
    
    public List<ServiceInstance> getIstioServices(String serviceName) {
        List<Service> services = kubernetesClient.services()
            .inNamespace("default")
            .withLabel("app", serviceName)
            .list()
            .getItems();
        
        return services.stream()
            .map(this::toServiceInstance)
            .collect(Collectors.toList());
    }
    
    private ServiceInstance toServiceInstance(Service service) {
        return new DefaultServiceInstance(
            service.getMetadata().getName(),
            service.getMetadata().getName(),
            service.getSpec().getClusterIP(),
            service.getSpec().getPorts().get(0).getPort(),
            false,
            service.getMetadata().getLabels()
        );
    }
    
    public boolean isIstioEnabled() {
        try {
            kubernetesClient.customResources(
                "networking.istio.io/v1beta1",
                "VirtualService"
            ).list();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 7. Monitoring and Observability

### 7.1 Service Discovery Metrics

```java
// Service Discovery Metrics
@Component
public class ServiceDiscoveryMetrics {
    
    private final Counter serviceRegistrationCounter;
    private final Counter serviceDeregistrationCounter;
    private final Gauge registeredServicesGauge;
    private final Timer serviceHealthCheckTimer;
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    public ServiceDiscoveryMetrics(MeterRegistry meterRegistry) {
        this.serviceRegistrationCounter = Counter.builder("service_registration_total")
            .description("Total number of service registrations")
            .register(meterRegistry);
        
        this.serviceDeregistrationCounter = Counter.builder("service_deregistration_total")
            .description("Total number of service deregistrations")
            .register(meterRegistry);
        
        this.registeredServicesGauge = Gauge.builder("registered_services")
            .description("Number of registered services")
            .register(meterRegistry, this, ServiceDiscoveryMetrics::getRegisteredServiceCount);
        
        this.serviceHealthCheckTimer = Timer.builder("service_health_check_duration")
            .description("Service health check duration")
            .register(meterRegistry);
    }
    
    public void recordServiceRegistration(String serviceName) {
        serviceRegistrationCounter.increment(Tags.of("service", serviceName));
    }
    
    public void recordServiceDeregistration(String serviceName) {
        serviceDeregistrationCounter.increment(Tags.of("service", serviceName));
    }
    
    public void recordHealthCheck(String serviceName, boolean healthy, long duration) {
        serviceHealthCheckTimer.record(duration, TimeUnit.MILLISECONDS,
            Tags.of("service", serviceName, "healthy", String.valueOf(healthy)));
    }
    
    private double getRegisteredServiceCount() {
        return discoveryClient.getServices().size();
    }
}

// Load Balancer Metrics
@Component
public class LoadBalancerMetrics {
    
    private final Counter loadBalancerRequestCounter;
    private final Timer loadBalancerRequestTimer;
    private final Gauge activeConnectionsGauge;
    
    private final Map<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();
    
    public LoadBalancerMetrics(MeterRegistry meterRegistry) {
        this.loadBalancerRequestCounter = Counter.builder("load_balancer_requests_total")
            .description("Total number of load balancer requests")
            .register(meterRegistry);
        
        this.loadBalancerRequestTimer = Timer.builder("load_balancer_request_duration")
            .description("Load balancer request duration")
            .register(meterRegistry);
        
        this.activeConnectionsGauge = Gauge.builder("load_balancer_active_connections")
            .description("Number of active connections")
            .register(meterRegistry, this, LoadBalancerMetrics::getTotalActiveConnections);
    }
    
    public void recordRequest(String serviceName, String instanceId, boolean successful, long duration) {
        loadBalancerRequestCounter.increment(Tags.of(
            "service", serviceName,
            "instance", instanceId,
            "successful", String.valueOf(successful)
        ));
        
        loadBalancerRequestTimer.record(duration, TimeUnit.MILLISECONDS,
            Tags.of("service", serviceName, "instance", instanceId));
    }
    
    public void incrementActiveConnections(String instanceId) {
        activeConnections.computeIfAbsent(instanceId, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    public void decrementActiveConnections(String instanceId) {
        AtomicInteger counter = activeConnections.get(instanceId);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
    
    private double getTotalActiveConnections() {
        return activeConnections.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
    }
}
```

---

## 8. Key Takeaways

### Service Discovery Benefits:
✅ **Dynamic Service Location**: Services can find each other automatically  
✅ **Fault Tolerance**: Automatic failover when services go down  
✅ **Load Distribution**: Intelligent request distribution  
✅ **Health Monitoring**: Continuous health checking  
✅ **Scalability**: Easy to add/remove service instances  

### Load Balancing Strategies:
- **Round Robin**: Simple, equal distribution
- **Weighted Round Robin**: Distribute based on server capacity
- **Least Connection**: Route to server with fewest active connections
- **Health-Aware**: Only route to healthy instances
- **Geographic**: Route based on client location

### Best Practices:
1. **Implement Health Checks**: Monitor service health continuously
2. **Use Circuit Breakers**: Prevent cascading failures
3. **Monitor Metrics**: Track service discovery and load balancing metrics
4. **Plan for Failures**: Design for service unavailability
5. **Optimize for Latency**: Choose load balancing strategy based on requirements

---

## 9. Next Steps

Tomorrow we'll explore **Microservices Data Management**, learning:
- Database per service pattern
- Distributed transaction management
- Event sourcing and CQRS
- Data consistency strategies

**Practice Assignment**: Implement a service discovery system with health checks and multiple load balancing strategies.

---

*"Service discovery is the nervous system of microservices - it enables services to find and communicate with each other dynamically."*

*"Good load balancing is invisible when it works well, but critical when services are under stress."*