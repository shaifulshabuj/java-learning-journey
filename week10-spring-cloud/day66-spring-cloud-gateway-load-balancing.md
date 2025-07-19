# Day 66: Spring Cloud Gateway & Load Balancing

## Learning Objectives
- Master Spring Cloud Gateway as modern API Gateway
- Implement advanced routing and filtering
- Configure load balancing strategies
- Apply rate limiting and security patterns
- Build reactive gateway solutions

---

## 1. Introduction to Spring Cloud Gateway

### 1.1 Gateway vs Zuul Comparison

```java
// Why Spring Cloud Gateway?
@Configuration
public class GatewayOverview {
    
    /*
     * Spring Cloud Gateway Advantages over Zuul:
     * 
     * 1. Built on Spring WebFlux (Reactive)
     *    - Non-blocking I/O
     *    - Better performance under load
     *    - Backpressure handling
     *    
     * 2. Predicates and Filters
     *    - More flexible routing
     *    - Built-in predicate factories
     *    - Customizable filter chains
     *    
     * 3. Better Integration
     *    - Spring Boot 2.x native support
     *    - Spring Cloud LoadBalancer
     *    - Spring Security integration
     *    
     * 4. Performance
     *    - Lower latency
     *    - Higher throughput
     *    - Better resource utilization
     *    
     * 5. Monitoring
     *    - Actuator integration
     *    - Micrometer metrics
     *    - Observability features
     */
}

// Architecture Overview
@Component
public class GatewayArchitecture {
    
    /*
     * Gateway Components:
     * 
     * 1. Route Predicate Factory
     *    - Matches incoming requests
     *    - Built-in predicates (Path, Method, Header, etc.)
     *    - Custom predicates
     *    
     * 2. Gateway Filter Factory
     *    - Modifies requests/responses
     *    - Pre and post filters
     *    - Global and route-specific filters
     *    
     * 3. Load Balancer
     *    - Client-side load balancing
     *    - Multiple algorithms
     *    - Health checking
     *    
     * 4. Route Locator
     *    - Defines routes
     *    - Configuration-based or programmatic
     *    - Dynamic route updates
     */
}
```

### 1.2 Basic Gateway Setup

```java
// Gateway Application
@SpringBootApplication
@EnableEurekaClient
public class GatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

// Basic Gateway Configuration
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Simple route
            .route("user-service", r -> r.path("/users/**")
                .uri("lb://user-service"))
            
            // Route with predicates
            .route("order-service", r -> r.path("/orders/**")
                .and().method("GET", "POST")
                .and().header("X-Request-Type", "api")
                .uri("lb://order-service"))
            
            // Route with filters
            .route("product-service", r -> r.path("/products/**")
                .filters(f -> f.addRequestHeader("X-Gateway", "Spring-Cloud-Gateway")
                            .addResponseHeader("X-Response-Time", "#{T(System).currentTimeMillis()}")
                            .hystrix(config -> config.setName("product-service")
                                                   .setFallbackUri("forward:/fallback/product")))
                .uri("lb://product-service"))
            
            // Route with custom filter
            .route("auth-service", r -> r.path("/auth/**")
                .filters(f -> f.filter(new AuthenticationGatewayFilterFactory().apply(
                    new AuthenticationGatewayFilterFactory.Config())))
                .uri("lb://auth-service"))
            
            .build();
    }
}

// YAML Configuration Alternative
@Configuration
@ConfigurationProperties
public class GatewayYamlConfig {
    
    /*
    spring:
      cloud:
        gateway:
          routes:
            - id: user-service
              uri: lb://user-service
              predicates:
                - Path=/users/**
              filters:
                - AddRequestHeader=X-Request-Source, gateway
                - AddResponseHeader=X-Response-Gateway, spring-cloud-gateway
                
            - id: order-service
              uri: lb://order-service
              predicates:
                - Path=/orders/**
                - Method=GET,POST
                - Header=X-Request-Type, api
              filters:
                - StripPrefix=1
                - AddRequestParameter=source, gateway
                - name: Hystrix
                  args:
                    name: order-service
                    fallbackUri: forward:/fallback/order
                    
            - id: product-service
              uri: lb://product-service
              predicates:
                - Path=/products/**
                - Query=version, v1
              filters:
                - name: RequestRateLimiter
                  args:
                    redis-rate-limiter.replenishRate: 10
                    redis-rate-limiter.burstCapacity: 20
                    key-resolver: "#{@userKeyResolver}"
                    
            - id: websocket-service
              uri: lb:ws://websocket-service
              predicates:
                - Path=/websocket/**
                
          default-filters:
            - AddRequestHeader=X-Gateway-Version, 1.0
            - AddResponseHeader=X-Response-Time, "#{T(System).currentTimeMillis()}"
            
          globalcors:
            cors-configurations:
              '[/**]':
                allowedOrigins: "*"
                allowedMethods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                allowedHeaders: "*"
                
    # Discovery locator for automatic routes
    spring:
      cloud:
        gateway:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true
              predicates:
                - name: Path
                  args:
                    pattern: "'/'+serviceId+'/**'"
              filters:
                - name: StripPrefix
                  args:
                    parts: 1
    */
}
```

---

## 2. Advanced Routing and Predicates

### 2.1 Custom Predicates

```java
// Custom Predicate Factory
@Component
public class BusinessHoursRoutePredicateFactory 
    extends AbstractRoutePredicateFactory<BusinessHoursRoutePredicateFactory.Config> {
    
    public BusinessHoursRoutePredicateFactory() {
        super(Config.class);
    }
    
    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            LocalTime now = LocalTime.now();
            return !now.isBefore(config.getStartTime()) && 
                   !now.isAfter(config.getEndTime());
        };
    }
    
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("startTime", "endTime");
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private LocalTime startTime = LocalTime.of(9, 0);
        private LocalTime endTime = LocalTime.of(17, 0);
    }
}

// Custom Weight-based Predicate
@Component
public class WeightRoutePredicateFactory 
    extends AbstractRoutePredicateFactory<WeightRoutePredicateFactory.Config> {
    
    public WeightRoutePredicateFactory() {
        super(Config.class);
    }
    
    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            // Use consistent hashing based on request attributes
            String hashKey = extractHashKey(exchange);
            int hash = hashKey.hashCode();
            int bucket = Math.abs(hash % 100);
            return bucket < config.getWeight();
        };
    }
    
    private String extractHashKey(ServerWebExchange exchange) {
        // Hash based on session ID or user ID
        String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-ID");
        if (sessionId != null) {
            return sessionId;
        }
        return exchange.getRequest().getRemoteAddress().getHostString();
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private int weight = 50; // 50% of traffic
    }
}

// Custom IP Range Predicate
@Component
public class IPRangeRoutePredicateFactory 
    extends AbstractRoutePredicateFactory<IPRangeRoutePredicateFactory.Config> {
    
    public IPRangeRoutePredicateFactory() {
        super(Config.class);
    }
    
    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            String clientIP = getClientIP(exchange);
            return isIPInRange(clientIP, config.getAllowedRanges());
        };
    }
    
    private String getClientIP(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return exchange.getRequest().getRemoteAddress().getHostString();
    }
    
    private boolean isIPInRange(String ip, List<String> ranges) {
        return ranges.stream().anyMatch(range -> isIPInCIDR(ip, range));
    }
    
    private boolean isIPInCIDR(String ip, String cidr) {
        try {
            SubnetUtils subnet = new SubnetUtils(cidr);
            return subnet.getInfo().isInRange(ip);
        } catch (Exception e) {
            logger.warn("Invalid CIDR range: {}", cidr);
            return false;
        }
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private List<String> allowedRanges = Arrays.asList("10.0.0.0/8", "192.168.0.0/16");
    }
}

// Dynamic Route Configuration
@RestController
@RequestMapping("/admin/routes")
public class DynamicRouteController {
    
    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @PostMapping
    public Mono<ResponseEntity<String>> createRoute(@RequestBody RouteDefinition route) {
        return routeDefinitionWriter.save(Mono.just(route))
            .then(Mono.fromRunnable(() -> eventPublisher.publishEvent(new RefreshRoutesEvent(this))))
            .then(Mono.just(ResponseEntity.ok("Route created successfully")));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<String>> updateRoute(@PathVariable String id, 
                                                   @RequestBody RouteDefinition route) {
        route.setId(id);
        return routeDefinitionWriter.save(Mono.just(route))
            .then(Mono.fromRunnable(() -> eventPublisher.publishEvent(new RefreshRoutesEvent(this))))
            .then(Mono.just(ResponseEntity.ok("Route updated successfully")));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteRoute(@PathVariable String id) {
        return routeDefinitionWriter.delete(Mono.just(id))
            .then(Mono.fromRunnable(() -> eventPublisher.publishEvent(new RefreshRoutesEvent(this))))
            .then(Mono.just(ResponseEntity.ok("Route deleted successfully")));
    }
}
```

### 2.2 Gateway Filters

```java
// Custom Authentication Filter
@Component
public class AuthenticationGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Skip authentication for public endpoints
            if (isPublicEndpoint(request.getPath().value())) {
                return chain.filter(exchange);
            }
            
            String token = extractToken(request);
            if (token == null) {
                return handleUnauthorized(exchange);
            }
            
            return validateToken(token)
                .flatMap(valid -> {
                    if (valid) {
                        return addUserInfoToRequest(exchange, token, chain);
                    } else {
                        return handleUnauthorized(exchange);
                    }
                })
                .onErrorResume(throwable -> handleUnauthorized(exchange));
        };
    }
    
    private Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
            // Check Redis blacklist
            Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                return false;
            }
            
            // Validate token
            return tokenProvider.validateToken(token);
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    private Mono<Void> addUserInfoToRequest(ServerWebExchange exchange, String token, GatewayFilterChain chain) {
        Long userId = tokenProvider.getUserIdFromToken(token);
        String userRole = tokenProvider.getUserRoleFromToken(token);
        
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header("X-User-Id", userId.toString())
            .header("X-User-Role", userRole)
            .header("X-Auth-Token", token)
            .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
    
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth/") || 
               path.startsWith("/public/") || 
               path.equals("/health") ||
               path.equals("/actuator/health");
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private boolean enabled = true;
        private List<String> publicEndpoints = Arrays.asList("/auth/**", "/public/**");
    }
}

// Rate Limiting Filter
@Component
public class CustomRateLimitingGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<CustomRateLimitingGatewayFilterFactory.Config> {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public CustomRateLimitingGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String key = resolveKey(exchange, config);
            
            return checkRateLimit(key, config)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        return handleRateLimitExceeded(exchange, config);
                    }
                });
        };
    }
    
    private Mono<Boolean> checkRateLimit(String key, Config config) {
        return Mono.fromCallable(() -> {
            String redisKey = "rate_limit:" + key;
            String currentCount = redisTemplate.opsForValue().get(redisKey);
            
            if (currentCount == null) {
                // First request
                redisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(config.getWindowSize()));
                return true;
            }
            
            int count = Integer.parseInt(currentCount);
            if (count < config.getLimit()) {
                redisTemplate.opsForValue().increment(redisKey);
                return true;
            }
            
            return false;
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    private String resolveKey(ServerWebExchange exchange, Config config) {
        switch (config.getKeyResolver()) {
            case "user":
                return exchange.getRequest().getHeaders().getFirst("X-User-Id");
            case "ip":
                return exchange.getRequest().getRemoteAddress().getHostString();
            case "path":
                return exchange.getRequest().getPath().value();
            default:
                return "global";
        }
    }
    
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, Config config) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getLimit()));
        response.getHeaders().add("X-RateLimit-Window", String.valueOf(config.getWindowSize()));
        
        String body = String.format(
            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded: %d requests per %d seconds\"}",
            config.getLimit(), config.getWindowSize()
        );
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private int limit = 100;
        private int windowSize = 60; // seconds
        private String keyResolver = "ip"; // user, ip, path, global
    }
}

// Request/Response Logging Filter
@Component
public class LoggingGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingGatewayFilterFactory.class);
    
    public LoggingGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Log request
            if (config.isLogRequest()) {
                logRequest(request);
            }
            
            // Capture start time
            long startTime = System.currentTimeMillis();
            
            return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    // Log response
                    if (config.isLogResponse()) {
                        long duration = System.currentTimeMillis() - startTime;
                        logResponse(exchange.getResponse(), duration);
                    }
                })
            );
        };
    }
    
    private void logRequest(ServerHttpRequest request) {
        logger.info("Gateway Request: {} {} from {}",
            request.getMethod(),
            request.getURI(),
            request.getRemoteAddress());
        
        if (logger.isDebugEnabled()) {
            request.getHeaders().forEach((name, values) -> 
                logger.debug("Request Header: {}: {}", name, values));
        }
    }
    
    private void logResponse(ServerHttpResponse response, long duration) {
        logger.info("Gateway Response: {} - Duration: {}ms",
            response.getStatusCode(),
            duration);
        
        if (logger.isDebugEnabled()) {
            response.getHeaders().forEach((name, values) -> 
                logger.debug("Response Header: {}: {}", name, values));
        }
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private boolean logRequest = true;
        private boolean logResponse = true;
        private boolean logHeaders = false;
    }
}
```

---

## 3. Load Balancing Strategies

### 3.1 Spring Cloud LoadBalancer

```java
// Load Balancer Configuration
@Configuration
public class LoadBalancerConfig {
    
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public ReactorLoadBalancerExchangeFilterFunction lbFunction(ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
        return new ReactorLoadBalancerExchangeFilterFunction(factory);
    }
}

// Custom Load Balancer
@Configuration
public class CustomLoadBalancerConfiguration {
    
    @Bean
    public ServiceInstanceListSupplier customServiceInstanceListSupplier() {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()
            .withHealthChecks()
            .withCaching()
            .build();
    }
    
    @Bean
    public ReactiveLoadBalancer<ServiceInstance> customLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new WeightedResponseTimeLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name
        );
    }
}

// Weighted Response Time Load Balancer
public class WeightedResponseTimeLoadBalancer implements ReactiveLoadBalancer<ServiceInstance> {
    
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final String serviceId;
    private final Map<String, ResponseTimeStats> responseTimeStats = new ConcurrentHashMap<>();
    
    public WeightedResponseTimeLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable();
        
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }
        
        return supplier.get(request)
            .next()
            .map(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    return new EmptyResponse();
                }
                
                ServiceInstance selected = selectByWeight(serviceInstances);
                return new DefaultResponse(selected);
            });
    }
    
    private ServiceInstance selectByWeight(List<ServiceInstance> instances) {
        if (instances.size() == 1) {
            return instances.get(0);
        }
        
        // Calculate weights based on response times
        List<WeightedInstance> weightedInstances = instances.stream()
            .map(instance -> {
                ResponseTimeStats stats = responseTimeStats.getOrDefault(
                    instance.getInstanceId(), 
                    new ResponseTimeStats()
                );
                
                // Lower response time = higher weight
                double weight = calculateWeight(stats);
                return new WeightedInstance(instance, weight);
            })
            .collect(Collectors.toList());
        
        // Weighted random selection
        return selectByWeightedRandom(weightedInstances);
    }
    
    private double calculateWeight(ResponseTimeStats stats) {
        if (stats.getSampleCount() == 0) {
            return 1.0; // Default weight for new instances
        }
        
        // Inverse of average response time
        double avgResponseTime = stats.getAverageResponseTime();
        return 1.0 / (avgResponseTime + 1); // +1 to avoid division by zero
    }
    
    private ServiceInstance selectByWeightedRandom(List<WeightedInstance> weightedInstances) {
        double totalWeight = weightedInstances.stream()
            .mapToDouble(WeightedInstance::getWeight)
            .sum();
        
        double randomValue = Math.random() * totalWeight;
        double cumulativeWeight = 0;
        
        for (WeightedInstance weighted : weightedInstances) {
            cumulativeWeight += weighted.getWeight();
            if (randomValue <= cumulativeWeight) {
                return weighted.getInstance();
            }
        }
        
        // Fallback to first instance
        return weightedInstances.get(0).getInstance();
    }
    
    public void recordResponseTime(String instanceId, long responseTime) {
        responseTimeStats.compute(instanceId, (key, stats) -> {
            if (stats == null) {
                stats = new ResponseTimeStats();
            }
            stats.addSample(responseTime);
            return stats;
        });
    }
}

// Response Time Statistics
@Data
public class ResponseTimeStats {
    private long totalResponseTime = 0;
    private int sampleCount = 0;
    private long maxResponseTime = 0;
    private long minResponseTime = Long.MAX_VALUE;
    private final int maxSamples = 100; // Keep last 100 samples
    
    public void addSample(long responseTime) {
        totalResponseTime += responseTime;
        sampleCount++;
        
        maxResponseTime = Math.max(maxResponseTime, responseTime);
        minResponseTime = Math.min(minResponseTime, responseTime);
        
        // Keep only recent samples
        if (sampleCount > maxSamples) {
            totalResponseTime = totalResponseTime * 9 / 10; // Decay old samples
            sampleCount = maxSamples * 9 / 10;
        }
    }
    
    public double getAverageResponseTime() {
        return sampleCount > 0 ? (double) totalResponseTime / sampleCount : 0;
    }
}

@Data
@AllArgsConstructor
class WeightedInstance {
    private ServiceInstance instance;
    private double weight;
}

// Health Check Integration
@Component
public class HealthCheckServiceInstanceListSupplier 
    extends DelegatingServiceInstanceListSupplier {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    public HealthCheckServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
        super(delegate);
    }
    
    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        return delegate.get(request)
            .flatMap(this::filterHealthyInstances);
    }
    
    private Mono<List<ServiceInstance>> filterHealthyInstances(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return Mono.just(instances);
        }
        
        List<Mono<ServiceInstance>> healthChecks = instances.stream()
            .map(this::checkHealth)
            .collect(Collectors.toList());
        
        return Flux.fromIterable(healthChecks)
            .flatMap(mono -> mono.onErrorReturn(null))
            .collectList()
            .map(results -> results.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }
    
    private Mono<ServiceInstance> checkHealth(ServiceInstance instance) {
        String healthUrl = instance.getUri() + "/actuator/health";
        
        return webClientBuilder.build()
            .get()
            .uri(healthUrl)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> instance)
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn(null);
    }
}
```

### 3.2 Sticky Sessions and Session Affinity

```java
// Session Affinity Load Balancer
public class SessionAffinityLoadBalancer implements ReactiveLoadBalancer<ServiceInstance> {
    
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final String serviceId;
    private final Map<String, String> sessionToInstance = new ConcurrentHashMap<>();
    private final LoadBalancer roundRobinLoadBalancer;
    
    public SessionAffinityLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
        this.roundRobinLoadBalancer = new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, serviceId);
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        String sessionId = extractSessionId(request);
        
        if (sessionId == null) {
            // No session, use round-robin
            return roundRobinLoadBalancer.choose(request);
        }
        
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }
        
        return supplier.get(request)
            .next()
            .map(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    return new EmptyResponse();
                }
                
                // Check if session has an assigned instance
                String assignedInstanceId = sessionToInstance.get(sessionId);
                if (assignedInstanceId != null) {
                    ServiceInstance assignedInstance = serviceInstances.stream()
                        .filter(instance -> instance.getInstanceId().equals(assignedInstanceId))
                        .findFirst()
                        .orElse(null);
                    
                    if (assignedInstance != null) {
                        return new DefaultResponse(assignedInstance);
                    } else {
                        // Assigned instance is no longer available
                        sessionToInstance.remove(sessionId);
                    }
                }
                
                // Assign new instance
                ServiceInstance newInstance = selectNewInstance(serviceInstances);
                sessionToInstance.put(sessionId, newInstance.getInstanceId());
                
                return new DefaultResponse(newInstance);
            });
    }
    
    private String extractSessionId(Request request) {
        if (request.getContext() instanceof ServerWebExchange) {
            ServerWebExchange exchange = (ServerWebExchange) request.getContext();
            return exchange.getRequest().getHeaders().getFirst("X-Session-ID");
        }
        return null;
    }
    
    private ServiceInstance selectNewInstance(List<ServiceInstance> instances) {
        // Simple round-robin for new sessions
        return instances.get(new Random().nextInt(instances.size()));
    }
}

// Session Affinity Gateway Filter
@Component
public class SessionAffinityGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<SessionAffinityGatewayFilterFactory.Config> {
    
    public SessionAffinityGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String sessionId = extractSessionId(request);
            
            if (sessionId == null) {
                // Generate new session ID
                sessionId = UUID.randomUUID().toString();
                
                // Add session ID to request
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Session-ID", sessionId)
                    .build();
                
                exchange = exchange.mutate().request(modifiedRequest).build();
            }
            
            return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    // Add session ID to response
                    ServerHttpResponse response = exchange.getResponse();
                    if (config.isSetCookie()) {
                        response.getHeaders().add("Set-Cookie", 
                            "SESSIONID=" + sessionId + "; Path=/; HttpOnly");
                    }
                    response.getHeaders().add("X-Session-ID", sessionId);
                })
            );
        };
    }
    
    private String extractSessionId(ServerHttpRequest request) {
        // Try header first
        String sessionId = request.getHeaders().getFirst("X-Session-ID");
        if (sessionId != null) {
            return sessionId;
        }
        
        // Try cookie
        List<HttpCookie> cookies = request.getCookies().get("SESSIONID");
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.get(0).getValue();
        }
        
        return null;
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private boolean setCookie = true;
        private String cookieName = "SESSIONID";
        private int cookieMaxAge = 3600; // 1 hour
    }
}
```

---

## 4. Circuit Breaker Integration

### 4.1 Resilience4j Circuit Breaker

```java
// Circuit Breaker Configuration
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
    }
}

// Circuit Breaker Gateway Filter
@Component
public class CircuitBreakerGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<CircuitBreakerGatewayFilterFactory.Config> {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    public CircuitBreakerGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
                config.getName(), 
                config.getCircuitBreakerConfig()
            );
            
            return chain.filter(exchange)
                .transformDeferred(publisher -> 
                    circuitBreaker.transformPublisher(publisher)
                )
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException) {
                        return handleCircuitBreakerOpen(exchange, config);
                    }
                    return handleError(exchange, throwable, config);
                });
        };
    }
    
    private Mono<Void> handleCircuitBreakerOpen(ServerWebExchange exchange, Config config) {
        if (config.getFallbackUri() != null) {
            return routeToFallback(exchange, config.getFallbackUri());
        }
        
        return returnCircuitBreakerResponse(exchange);
    }
    
    private Mono<Void> handleError(ServerWebExchange exchange, Throwable throwable, Config config) {
        if (config.getFallbackUri() != null) {
            return routeToFallback(exchange, config.getFallbackUri());
        }
        
        return returnErrorResponse(exchange, throwable);
    }
    
    private Mono<Void> routeToFallback(ServerWebExchange exchange, String fallbackUri) {
        ServerHttpRequest request = exchange.getRequest().mutate()
            .path(fallbackUri)
            .build();
        
        return exchange.mutate().request(request).build()
            .getResponse()
            .writeWith(Mono.empty());
    }
    
    private Mono<Void> returnCircuitBreakerResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\":\"Circuit Breaker Open\",\"message\":\"Service temporarily unavailable\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    private Mono<Void> returnErrorResponse(ServerWebExchange exchange, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format(
            "{\"error\":\"Service Error\",\"message\":\"%s\"}", 
            throwable.getMessage()
        );
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private String name = "default";
        private String fallbackUri;
        private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.ofDefaults();
    }
}

// Circuit Breaker Metrics
@Component
public class CircuitBreakerMetrics {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @PostConstruct
    public void init() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            // Register metrics
            Gauge.builder("circuit.breaker.state")
                .tag("name", circuitBreaker.getName())
                .register(meterRegistry, circuitBreaker, cb -> cb.getState().ordinal());
            
            // Event metrics
            circuitBreaker.getEventPublisher().onStateTransition(event -> {
                meterRegistry.counter("circuit.breaker.state.transition",
                    "name", circuitBreaker.getName(),
                    "from", event.getStateTransition().getFromState().name(),
                    "to", event.getStateTransition().getToState().name()
                ).increment();
            });
            
            circuitBreaker.getEventPublisher().onCallNotPermitted(event -> {
                meterRegistry.counter("circuit.breaker.call.not.permitted",
                    "name", circuitBreaker.getName()
                ).increment();
            });
        });
    }
}
```

---

## 5. Security and CORS

### 5.1 Gateway Security Configuration

```java
// Security Configuration
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveAuthenticationManager authenticationManager,
            ServerAuthenticationEntryPoint authenticationEntryPoint) {
        
        return http
            .csrf().disable()
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/auth/**", "/public/**").permitAll()
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .pathMatchers("/users/**").hasAnyRole("USER", "ADMIN")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .authenticationManager(authenticationManager)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .build();
    }
    
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return ReactiveJwtDecoders.fromIssuerLocation("https://your-auth-server.com");
    }
    
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<String> roles = jwt.getClaimAsStringList("roles");
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
    
    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("Content-Type", "application/json");
            
            String body = "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
            
            return response.writeWith(Mono.just(buffer));
        };
    }
}

// CORS Configuration
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}

// JWT Token Filter
@Component
public class JwtTokenGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<JwtTokenGatewayFilterFactory.Config> {
    
    @Autowired
    private ReactiveJwtDecoder jwtDecoder;
    
    public JwtTokenGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractToken(exchange.getRequest());
            
            if (token == null) {
                return chain.filter(exchange);
            }
            
            return jwtDecoder.decode(token)
                .map(jwt -> {
                    // Add JWT claims to request headers
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-JWT-SUB", jwt.getSubject())
                        .header("X-JWT-ISS", jwt.getIssuer().toString())
                        .header("X-JWT-AUD", String.join(",", jwt.getAudience()))
                        .header("X-JWT-EXP", jwt.getExpiresAt().toString())
                        .build();
                    
                    return exchange.mutate().request(modifiedRequest).build();
                })
                .flatMap(chain::filter)
                .onErrorResume(throwable -> {
                    logger.error("JWT validation failed", throwable);
                    return handleJwtError(exchange);
                });
        };
    }
    
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private Mono<Void> handleJwtError(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\":\"Invalid JWT\",\"message\":\"JWT validation failed\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private boolean validateExpiration = true;
        private boolean validateIssuer = true;
        private boolean validateAudience = false;
    }
}
```

---

## 6. Monitoring and Observability

### 6.1 Gateway Metrics

```java
// Gateway Metrics Configuration
@Configuration
public class GatewayMetricsConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public GatewayMetrics gatewayMetrics(MeterRegistry meterRegistry) {
        return new GatewayMetrics(meterRegistry);
    }
}

@Component
public class GatewayMetrics {
    
    private final Counter requestCounter;
    private final Timer requestTimer;
    private final Gauge activeConnections;
    private final Counter errorCounter;
    
    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("gateway.requests")
            .description("Total number of requests")
            .register(meterRegistry);
        
        this.requestTimer = Timer.builder("gateway.request.duration")
            .description("Request processing duration")
            .register(meterRegistry);
        
        this.activeConnections = Gauge.builder("gateway.connections.active")
            .description("Number of active connections")
            .register(meterRegistry, this, GatewayMetrics::getActiveConnections);
        
        this.errorCounter = Counter.builder("gateway.errors")
            .description("Number of errors")
            .register(meterRegistry);
    }
    
    public void recordRequest(String route, String method, String status) {
        requestCounter.increment(Tags.of(
            "route", route,
            "method", method,
            "status", status
        ));
    }
    
    public Timer.Sample startTimer() {
        return Timer.start();
    }
    
    public void recordRequestDuration(Timer.Sample sample, String route, String method) {
        sample.stop(Timer.builder("gateway.request.duration")
            .tag("route", route)
            .tag("method", method)
            .register(requestTimer.getMeter().getId().getMeterRegistry()));
    }
    
    public void recordError(String route, String errorType) {
        errorCounter.increment(Tags.of(
            "route", route,
            "error.type", errorType
        ));
    }
    
    private double getActiveConnections() {
        // Implementation to get active connection count
        return 0.0;
    }
}

// Metrics Gateway Filter
@Component
public class MetricsGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<MetricsGatewayFilterFactory.Config> {
    
    @Autowired
    private GatewayMetrics metrics;
    
    public MetricsGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Timer.Sample sample = metrics.startTimer();
            
            ServerHttpRequest request = exchange.getRequest();
            String route = getRouteName(exchange);
            String method = request.getMethod().name();
            
            return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    String status = String.valueOf(exchange.getResponse().getStatusCode().value());
                    metrics.recordRequest(route, method, status);
                    metrics.recordRequestDuration(sample, route, method);
                })
                .doOnError(throwable -> {
                    metrics.recordError(route, throwable.getClass().getSimpleName());
                    metrics.recordRequestDuration(sample, route, method);
                });
        };
    }
    
    private String getRouteName(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "unknown";
    }
    
    @Data
    @NoArgsConstructor
    public static class Config {
        private boolean enabled = true;
    }
}

// Health Check Endpoint
@RestController
@RequestMapping("/actuator")
public class GatewayHealthController {
    
    @Autowired
    private GatewayMetrics metrics;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            
            // Circuit breaker health
            Map<String, Object> circuitBreakers = new HashMap<>();
            circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
                Map<String, Object> cbHealth = new HashMap<>();
                cbHealth.put("state", cb.getState().name());
                cbHealth.put("failureRate", cb.getMetrics().getFailureRate());
                cbHealth.put("callsCount", cb.getMetrics().getNumberOfSuccessfulCalls() + 
                                         cb.getMetrics().getNumberOfFailedCalls());
                circuitBreakers.put(cb.getName(), cbHealth);
            });
            health.put("circuitBreakers", circuitBreakers);
            
            return ResponseEntity.ok(health);
        });
    }
    
    @GetMapping("/metrics")
    public Mono<ResponseEntity<Map<String, Object>>> metrics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> metricsData = new HashMap<>();
            metricsData.put("timestamp", System.currentTimeMillis());
            
            // Add custom metrics
            metricsData.put("activeConnections", metrics.getActiveConnections());
            
            return ResponseEntity.ok(metricsData);
        });
    }
}
```

---

## 7. Testing Gateway Configuration

### 7.1 Integration Tests

```java
// Gateway Integration Test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=test-service",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8080",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**"
})
class GatewayIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Test
    void testBasicRouting() {
        String url = "http://localhost:" + port + "/test/hello";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void testAuthenticationFilter() {
        String url = "http://localhost:" + port + "/protected/resource";
        
        // Without token
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // With token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

// Filter Unit Test
@ExtendWith(MockitoExtension.class)
class AuthenticationGatewayFilterTest {
    
    @Mock
    private JwtTokenProvider tokenProvider;
    
    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private GatewayFilterChain chain;
    
    @Mock
    private ServerHttpRequest request;
    
    @Mock
    private ServerHttpResponse response;
    
    @InjectMocks
    private AuthenticationGatewayFilterFactory filterFactory;
    
    @Test
    void testValidToken() {
        // Setup
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getPath()).thenReturn(PathContainer.parsePath("/protected/resource"));
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn(123L);
        when(tokenProvider.getUserRoleFromToken("valid-token")).thenReturn("USER");
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // Test
        GatewayFilter filter = filterFactory.apply(new AuthenticationGatewayFilterFactory.Config());
        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();
        
        // Verify
        verify(chain).filter(any());
    }
    
    @Test
    void testInvalidToken() {
        // Setup
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getPath()).thenReturn(PathContainer.parsePath("/protected/resource"));
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.getHeaders()).thenReturn(new HttpHeaders());
        when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(response.writeWith(any())).thenReturn(Mono.empty());
        
        // Test
        GatewayFilter filter = filterFactory.apply(new AuthenticationGatewayFilterFactory.Config());
        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();
        
        // Verify
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }
}
```

---

## 8. Summary and Best Practices

### Key Concepts Mastered

1. **Spring Cloud Gateway**
   - Reactive, non-blocking I/O
   - Flexible routing with predicates
   - Filter chain architecture
   - Built-in and custom filters

2. **Load Balancing**
   - Spring Cloud LoadBalancer
   - Custom load balancing strategies
   - Health checking integration
   - Session affinity

3. **Circuit Breaker Integration**
   - Resilience4j integration
   - Fallback mechanisms
   - Metrics and monitoring

4. **Security**
   - JWT token validation
   - OAuth2 resource server
   - CORS configuration
   - Custom authentication filters

### Best Practices

✅ **Use reactive programming patterns**  
✅ **Implement proper error handling**  
✅ **Configure circuit breakers for resilience**  
✅ **Monitor gateway metrics**  
✅ **Implement proper authentication/authorization**  
✅ **Use health checks for load balancing**  
✅ **Configure appropriate timeouts**  

### Common Pitfalls to Avoid

❌ **Blocking operations in filters**  
❌ **Not handling backpressure properly**  
❌ **Missing fallback mechanisms**  
❌ **Inadequate error handling**  
❌ **Poor security configuration**  

### Next Steps

Tomorrow, we'll explore Spring Cloud OpenFeign for declarative HTTP clients and advanced RestTemplate configurations for microservice communication.

---

**Practice Assignment**: Build a Spring Cloud Gateway with custom filters for authentication, rate limiting, and logging. Implement multiple load balancing strategies and integrate circuit breakers with fallback mechanisms.

---

*"Spring Cloud Gateway represents the evolution of API gateways - reactive, performant, and built for cloud-native architectures."*