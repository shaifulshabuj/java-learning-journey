# Day 60: API Gateway Patterns

## Learning Objectives
- Understand API Gateway architecture and patterns
- Implement centralized API management
- Master request routing and load balancing
- Apply authentication and authorization strategies
- Implement rate limiting and throttling mechanisms

---

## 1. API Gateway Fundamentals

### 1.1 What is an API Gateway?

An API Gateway acts as a single entry point for all client requests to microservices. It provides cross-cutting concerns like authentication, rate limiting, request routing, and monitoring.

```java
// Traditional Direct Service Access (Problems)
@RestController
public class ClientController {
    
    @Autowired
    private CustomerServiceClient customerService;
    
    @Autowired
    private OrderServiceClient orderService;
    
    @Autowired
    private ProductServiceClient productService;
    
    @GetMapping("/customer-orders/{customerId}")
    public ResponseEntity<CustomerOrdersResponse> getCustomerOrders(@PathVariable Long customerId) {
        // Client needs to know about multiple services
        Customer customer = customerService.getCustomer(customerId);
        List<Order> orders = orderService.getOrdersByCustomer(customerId);
        
        // Client handles service orchestration
        List<OrderWithProducts> ordersWithProducts = new ArrayList<>();
        for (Order order : orders) {
            List<Product> products = productService.getProductsByIds(order.getProductIds());
            ordersWithProducts.add(new OrderWithProducts(order, products));
        }
        
        CustomerOrdersResponse response = CustomerOrdersResponse.builder()
            .customer(customer)
            .orders(ordersWithProducts)
            .build();
        
        return ResponseEntity.ok(response);
    }
}

// API Gateway Approach (Benefits)
@RestController
@RequestMapping("/api/v1")
public class ApiGatewayController {
    
    @Autowired
    private CustomerOrchestrationService orchestrationService;
    
    @GetMapping("/customer-orders/{customerId}")
    public ResponseEntity<CustomerOrdersResponse> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestHeader("Authorization") String authHeader) {
        
        // Gateway handles authentication, authorization, and orchestration
        CustomerOrdersResponse response = orchestrationService.getCustomerOrders(customerId);
        return ResponseEntity.ok(response);
    }
}
```

### 1.2 API Gateway Benefits

- **Single Entry Point**: Simplifies client interaction
- **Cross-cutting Concerns**: Authentication, logging, monitoring
- **Protocol Translation**: HTTP to different protocols
- **Request Aggregation**: Combine multiple service calls
- **Load Balancing**: Distribute traffic across service instances

---

## 2. Spring Cloud Gateway Implementation

### 2.1 Gateway Configuration

```java
// Gateway Application
@SpringBootApplication
@EnableEurekaClient
public class ApiGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Customer Service Routes
            .route("customer-service", r -> r
                .path("/api/v1/customers/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway", "spring-cloud-gateway")
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("customer-service")
                        .setFallbackUri("forward:/fallback/customer"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5), 2, true)))
                .uri("lb://customer-service"))
            
            // Order Service Routes
            .route("order-service", r -> r
                .path("/api/v1/orders/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway", "spring-cloud-gateway")
                    .rewritePath("/api/v1/orders/(?<segment>.*)", "/api/v1/orders/${segment}")
                    .circuitBreaker(config -> config
                        .setName("order-service")
                        .setFallbackUri("forward:/fallback/order")))
                .uri("lb://order-service"))
            
            // Product Service Routes
            .route("product-service", r -> r
                .path("/api/v1/products/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway", "spring-cloud-gateway")
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://product-service"))
            
            // Payment Service Routes with Authentication
            .route("payment-service", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway", "spring-cloud-gateway")
                    .filter(jwtAuthenticationFilter()))
                .uri("lb://payment-service"))
            
            .build();
    }
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1); // 10 requests per second, burst of 20
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getRequest()
            .getHeaders()
            .getFirst("X-User-Id")
            .map(Mono::just)
            .orElse(Mono.just("anonymous"));
    }
}

// Gateway Filter Configuration
@Configuration
public class GatewayConfig {
    
    @Bean
    public GlobalFilter customGlobalFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Add correlation ID
            String correlationId = UUID.randomUUID().toString();
            ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-ID", correlationId)
                .build();
            
            // Log request
            logger.info("Gateway request: {} {} - Correlation ID: {}", 
                request.getMethod(), request.getURI(), correlationId);
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    logger.info("Gateway response: {} - Correlation ID: {}", 
                        response.getStatusCode(), correlationId);
                }));
        };
    }
    
    @Bean
    public GatewayFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationGatewayFilterFactory().apply(new JwtAuthenticationGatewayFilterFactory.Config());
    }
}
```

### 2.2 Custom Gateway Filters

```java
// JWT Authentication Filter
@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {
    
    @Autowired
    private JwtTokenValidator jwtTokenValidator;
    
    public JwtAuthenticationGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract JWT token from Authorization header
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            
            return jwtTokenValidator.validateToken(token)
                .flatMap(claims -> {
                    // Add user information to request headers
                    ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Role", claims.get("role", String.class))
                        .header("X-User-Email", claims.get("email", String.class))
                        .build();
                    
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(ex -> {
                    logger.error("JWT validation failed: {}", ex.getMessage());
                    return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                });
        };
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\": \"%s\", \"message\": \"%s\"}", 
            httpStatus.getReasonPhrase(), message);
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
    
    @Data
    public static class Config {
        private boolean validateToken = true;
        private String secretKey = "default-secret-key";
    }
}

// Rate Limiting Filter
@Component
public class CustomRateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<CustomRateLimitGatewayFilterFactory.Config> {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public CustomRateLimitGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Generate rate limit key
            String key = generateRateLimitKey(request, config);
            
            return checkRateLimit(key, config)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        return onRateLimitExceeded(exchange, config);
                    }
                });
        };
    }
    
    private String generateRateLimitKey(ServerHttpRequest request, Config config) {
        String clientId = request.getHeaders().getFirst("X-Client-ID");
        String userId = request.getHeaders().getFirst("X-User-ID");
        String ip = request.getRemoteAddress().getAddress().getHostAddress();
        
        return switch (config.getKeyType()) {
            case CLIENT -> "rate_limit:client:" + clientId;
            case USER -> "rate_limit:user:" + userId;
            case IP -> "rate_limit:ip:" + ip;
            default -> "rate_limit:global";
        };
    }
    
    private Mono<Boolean> checkRateLimit(String key, Config config) {
        return Mono.fromCallable(() -> {
            String current = redisTemplate.opsForValue().get(key);
            int count = current != null ? Integer.parseInt(current) : 0;
            
            if (count >= config.getLimit()) {
                return false;
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofSeconds(config.getWindowSize()));
            
            return true;
        });
    }
    
    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, Config config) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getLimit()));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + config.getWindowSize() * 1000));
        
        String body = String.format("{\"error\": \"Rate limit exceeded\", \"limit\": %d, \"windowSize\": %d}", 
            config.getLimit(), config.getWindowSize());
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
    
    @Data
    public static class Config {
        private int limit = 100;
        private int windowSize = 60; // seconds
        private KeyType keyType = KeyType.IP;
    }
    
    public enum KeyType {
        CLIENT, USER, IP, GLOBAL
    }
}

// Request Transformation Filter
@Component
public class RequestTransformationGatewayFilterFactory extends AbstractGatewayFilterFactory<RequestTransformationGatewayFilterFactory.Config> {
    
    public RequestTransformationGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Transform request based on configuration
            ServerHttpRequest.Builder requestBuilder = request.mutate();
            
            // Add headers
            config.getAddHeaders().forEach(requestBuilder::header);
            
            // Remove headers
            config.getRemoveHeaders().forEach(header -> 
                requestBuilder.headers(headers -> headers.remove(header)));
            
            // Transform path
            if (config.getPathRewrite() != null) {
                String newPath = request.getURI().getPath().replaceAll(
                    config.getPathRewrite().getPattern(), 
                    config.getPathRewrite().getReplacement());
                requestBuilder.path(newPath);
            }
            
            // Add query parameters
            config.getAddQueryParams().forEach((key, value) -> 
                requestBuilder.queryParam(key, value));
            
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        };
    }
    
    @Data
    public static class Config {
        private Map<String, String> addHeaders = new HashMap<>();
        private List<String> removeHeaders = new ArrayList<>();
        private Map<String, String> addQueryParams = new HashMap<>();
        private PathRewrite pathRewrite;
    }
    
    @Data
    public static class PathRewrite {
        private String pattern;
        private String replacement;
    }
}
```

---

## 3. Request Routing and Load Balancing

### 3.1 Advanced Routing Strategies

```java
// Weighted Routing Configuration
@Configuration
public class RoutingConfig {
    
    @Bean
    public RouteLocator weightedRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // A/B Testing Routes
            .route("product-service-v1", r -> r
                .path("/api/v1/products/**")
                .and()
                .header("X-Version", "v1")
                .uri("lb://product-service-v1"))
            
            .route("product-service-v2", r -> r
                .path("/api/v1/products/**")
                .and()
                .header("X-Version", "v2")
                .uri("lb://product-service-v2"))
            
            // Canary Deployment (90% v1, 10% v2)
            .route("product-service-canary", r -> r
                .path("/api/v1/products/**")
                .and()
                .weight("product-group", 90)
                .uri("lb://product-service-v1"))
            
            .route("product-service-canary-new", r -> r
                .path("/api/v1/products/**")
                .and()
                .weight("product-group", 10)
                .uri("lb://product-service-v2"))
            
            // Geographic Routing
            .route("customer-service-us", r -> r
                .path("/api/v1/customers/**")
                .and()
                .header("X-Region", "US")
                .uri("lb://customer-service-us"))
            
            .route("customer-service-eu", r -> r
                .path("/api/v1/customers/**")
                .and()
                .header("X-Region", "EU")
                .uri("lb://customer-service-eu"))
            
            .build();
    }
}

// Custom Route Predicate
@Component
public class CustomRoutePredicateFactory extends AbstractRoutePredicateFactory<CustomRoutePredicateFactory.Config> {
    
    public CustomRoutePredicateFactory() {
        super(Config.class);
    }
    
    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Custom business logic for routing
            String userTier = request.getHeaders().getFirst("X-User-Tier");
            String requestType = request.getHeaders().getFirst("X-Request-Type");
            
            // Route premium users to premium service
            if ("PREMIUM".equals(userTier) && "SEARCH".equals(requestType)) {
                return true;
            }
            
            // Route based on request load
            if ("BULK".equals(requestType)) {
                return exchange.getRequest().getQueryParams().containsKey("batch");
            }
            
            return false;
        };
    }
    
    @Data
    public static class Config {
        private String userTier;
        private String requestType;
    }
}

// Load Balancer Configuration
@Configuration
public class LoadBalancerConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> customerServiceLoadBalancer(
            Environment environment, LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), name);
    }
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> orderServiceLoadBalancer(
            Environment environment, LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinLoadBalancer(loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), name);
    }
}
```

### 3.2 Health-based Load Balancing

```java
// Health Check Load Balancer
@Component
public class HealthCheckLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final WebClient webClient;
    
    public HealthCheckLoadBalancer(String serviceId, 
                                  ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                  WebClient webClient) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.webClient = webClient;
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        
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
                    return new EmptyResponse();
                }
                
                // Select instance based on load
                ServiceInstance selectedInstance = selectInstanceByLoad(healthyInstances);
                return new DefaultResponse(selectedInstance);
            });
    }
    
    private boolean isHealthy(ServiceInstance serviceInstance) {
        try {
            String healthUrl = "http://" + serviceInstance.getHost() + ":" + serviceInstance.getPort() + "/actuator/health";
            
            String response = webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(2))
                .block();
            
            return response != null && response.contains("\"status\":\"UP\"");
            
        } catch (Exception e) {
            logger.warn("Health check failed for instance {}:{}: {}", 
                serviceInstance.getHost(), serviceInstance.getPort(), e.getMessage());
            return false;
        }
    }
    
    private ServiceInstance selectInstanceByLoad(List<ServiceInstance> instances) {
        // Simple round-robin for now
        // In production, you might check actual load metrics
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }
}
```

---

## 4. Authentication and Authorization

### 4.1 JWT Token Validation

```java
// JWT Token Validator
@Component
public class JwtTokenValidator {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Mono<Claims> validateToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                
                // Additional validation
                if (claims.getExpiration().before(new Date())) {
                    throw new JwtException("Token expired");
                }
                
                return claims;
                
            } catch (Exception e) {
                throw new JwtException("Invalid token: " + e.getMessage());
            }
        });
    }
    
    public Mono<Boolean> hasRole(String token, String requiredRole) {
        return validateToken(token)
            .map(claims -> {
                String roles = claims.get("roles", String.class);
                return roles != null && roles.contains(requiredRole);
            })
            .onErrorReturn(false);
    }
    
    public Mono<Boolean> hasPermission(String token, String requiredPermission) {
        return validateToken(token)
            .map(claims -> {
                String permissions = claims.get("permissions", String.class);
                return permissions != null && permissions.contains(requiredPermission);
            })
            .onErrorReturn(false);
    }
}

// Role-based Authorization Filter
@Component
public class RoleBasedAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<RoleBasedAuthorizationGatewayFilterFactory.Config> {
    
    @Autowired
    private JwtTokenValidator jwtTokenValidator;
    
    public RoleBasedAuthorizationGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            
            return jwtTokenValidator.hasRole(token, config.getRequiredRole())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return onError(exchange, "Insufficient permissions", HttpStatus.FORBIDDEN);
                    }
                    return chain.filter(exchange);
                });
        };
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\": \"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
    
    @Data
    public static class Config {
        private String requiredRole;
    }
}
```

### 4.2 OAuth2 Integration

```java
// OAuth2 Resource Server Configuration
@Configuration
@EnableWebFluxSecurity
public class OAuth2Config {
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/api/v1/public/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .pathMatchers("/api/v1/customers/**").hasRole("USER")
                .pathMatchers("/api/v1/orders/**").hasRole("USER")
                .pathMatchers("/api/v1/payments/**").hasRole("USER")
                .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }
    
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
            .withJwkSetUri("https://auth-server.example.com/.well-known/jwks.json")
            .build();
        
        decoder.setJwtValidator(jwtValidator());
        return decoder;
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
    public Validator<Jwt> jwtValidator() {
        List<Validator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new JwtIssuerValidator("https://auth-server.example.com"));
        validators.add(new JwtAudienceValidator("gateway-audience"));
        
        return new DelegatingValidator<>(validators);
    }
}
```

---

## 5. Rate Limiting and Throttling

### 5.1 Redis-based Rate Limiting

```java
// Redis Rate Limiter Configuration
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(
            10,  // replenishRate: tokens per second
            20,  // burstCapacity: maximum tokens
            1    // requestedTokens: tokens per request
        );
    }
    
    @Bean
    public RedisRateLimiter premiumRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }
    
    @Bean
    public RedisRateLimiter bulkRateLimiter() {
        return new RedisRateLimiter(2, 5, 5); // 5 tokens per request for bulk operations
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }
    
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            return Mono.just(apiKey != null ? apiKey : "no-api-key");
        };
    }
    
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }
}

// Adaptive Rate Limiter
@Component
public class AdaptiveRateLimiter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public Mono<Boolean> isAllowed(String key, String tier, String operation) {
        RateLimitConfig config = getRateLimitConfig(tier, operation);
        
        return checkRateLimit(key, config)
            .doOnNext(allowed -> {
                // Record metrics
                meterRegistry.counter("rate_limit_requests",
                    "key", key,
                    "tier", tier,
                    "operation", operation,
                    "allowed", String.valueOf(allowed))
                    .increment();
            });
    }
    
    private RateLimitConfig getRateLimitConfig(String tier, String operation) {
        return switch (tier) {
            case "PREMIUM" -> switch (operation) {
                case "READ" -> new RateLimitConfig(100, 200, 60);
                case "WRITE" -> new RateLimitConfig(50, 100, 60);
                case "BULK" -> new RateLimitConfig(10, 20, 60);
                default -> new RateLimitConfig(50, 100, 60);
            };
            case "STANDARD" -> switch (operation) {
                case "READ" -> new RateLimitConfig(50, 100, 60);
                case "WRITE" -> new RateLimitConfig(20, 40, 60);
                case "BULK" -> new RateLimitConfig(5, 10, 60);
                default -> new RateLimitConfig(20, 40, 60);
            };
            default -> new RateLimitConfig(10, 20, 60);
        };
    }
    
    private Mono<Boolean> checkRateLimit(String key, RateLimitConfig config) {
        return Mono.fromCallable(() -> {
            String redisKey = "rate_limit:" + key;
            String current = redisTemplate.opsForValue().get(redisKey);
            
            long now = System.currentTimeMillis();
            long windowStart = now - (config.getWindowSizeSeconds() * 1000);
            
            // Clean old entries
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
            
            // Count current requests
            long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, now);
            
            if (currentCount >= config.getLimit()) {
                return false;
            }
            
            // Add current request
            redisTemplate.opsForZSet().add(redisKey, UUID.randomUUID().toString(), now);
            redisTemplate.expire(redisKey, Duration.ofSeconds(config.getWindowSizeSeconds()));
            
            return true;
        });
    }
    
    @Data
    @AllArgsConstructor
    private static class RateLimitConfig {
        private int limit;
        private int burstLimit;
        private int windowSizeSeconds;
    }
}
```

### 5.2 Circuit Breaker Integration

```java
// Circuit Breaker with Rate Limiting
@Component
public class ResilientRateLimitFilter implements GatewayFilter {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private AdaptiveRateLimiter rateLimiter;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        String serviceName = extractServiceName(request);
        String userTier = request.getHeaders().getFirst("X-User-Tier");
        String userId = request.getHeaders().getFirst("X-User-ID");
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        
        return rateLimiter.isAllowed(userId, userTier, "API_CALL")
            .flatMap(allowed -> {
                if (!allowed) {
                    return onRateLimitExceeded(exchange);
                }
                
                return executeWithCircuitBreaker(circuitBreaker, exchange, chain);
            });
    }
    
    private Mono<Void> executeWithCircuitBreaker(CircuitBreaker circuitBreaker, 
                                                ServerWebExchange exchange, 
                                                GatewayFilterChain chain) {
        return Mono.fromCallable(() -> circuitBreaker.executeSupplier(() -> {
            return chain.filter(exchange);
        }))
        .flatMap(result -> result)
        .onErrorResume(CircuitBreakerOpenException.class, ex -> 
            onCircuitBreakerOpen(exchange))
        .onErrorResume(CallNotPermittedException.class, ex -> 
            onCircuitBreakerOpen(exchange));
    }
    
    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\": \"Rate limit exceeded\", \"retryAfter\": 60}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
    
    private Mono<Void> onCircuitBreakerOpen(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\": \"Service temporarily unavailable\", \"retryAfter\": 30}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
    
    private String extractServiceName(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (path.startsWith("/api/v1/customers")) return "customer-service";
        if (path.startsWith("/api/v1/orders")) return "order-service";
        if (path.startsWith("/api/v1/products")) return "product-service";
        if (path.startsWith("/api/v1/payments")) return "payment-service";
        return "unknown-service";
    }
}
```

---

## 6. Monitoring and Observability

### 6.1 Metrics Collection

```java
// Gateway Metrics Configuration
@Configuration
public class MetricsConfig {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public GlobalFilter metricsGlobalFilter(MeterRegistry meterRegistry) {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            String path = exchange.getRequest().getURI().getPath();
            String method = exchange.getRequest().getMethodValue();
            
            return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    HttpStatus status = exchange.getResponse().getStatusCode();
                    
                    Timer.Sample sample = Timer.start(meterRegistry);
                    sample.stop(Timer.builder("gateway.request.duration")
                        .description("Gateway request duration")
                        .tag("method", method)
                        .tag("path", path)
                        .tag("status", status != null ? status.toString() : "unknown")
                        .register(meterRegistry));
                    
                    Counter.builder("gateway.requests.total")
                        .description("Total gateway requests")
                        .tag("method", method)
                        .tag("path", path)
                        .tag("status", status != null ? status.toString() : "unknown")
                        .register(meterRegistry)
                        .increment();
                });
        };
    }
}

// Custom Metrics
@Component
public class GatewayMetrics {
    
    private final Counter requestCounter;
    private final Timer requestTimer;
    private final Gauge activeConnections;
    private final Counter errorCounter;
    
    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("gateway_requests_total")
            .description("Total number of requests processed by gateway")
            .register(meterRegistry);
        
        this.requestTimer = Timer.builder("gateway_request_duration_seconds")
            .description("Request processing time")
            .register(meterRegistry);
        
        this.activeConnections = Gauge.builder("gateway_active_connections")
            .description("Number of active connections")
            .register(meterRegistry, this, GatewayMetrics::getActiveConnectionCount);
        
        this.errorCounter = Counter.builder("gateway_errors_total")
            .description("Total number of errors")
            .register(meterRegistry);
    }
    
    public void recordRequest(String method, String path, String status, long duration) {
        requestCounter.increment(
            Tags.of(
                Tag.of("method", method),
                Tag.of("path", path),
                Tag.of("status", status)
            )
        );
        
        requestTimer.record(duration, TimeUnit.MILLISECONDS,
            Tags.of(
                Tag.of("method", method),
                Tag.of("path", path),
                Tag.of("status", status)
            )
        );
    }
    
    public void recordError(String errorType, String service) {
        errorCounter.increment(
            Tags.of(
                Tag.of("error_type", errorType),
                Tag.of("service", service)
            )
        );
    }
    
    private double getActiveConnectionCount() {
        // Implementation depends on your connection tracking mechanism
        return 0.0;
    }
}
```

### 6.2 Distributed Tracing

```java
// Tracing Configuration
@Configuration
public class TracingConfig {
    
    @Bean
    public GlobalFilter tracingGlobalFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract or generate trace ID
            String traceId = request.getHeaders().getFirst("X-Trace-ID");
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
            }
            
            // Extract or generate span ID
            String spanId = UUID.randomUUID().toString();
            String parentSpanId = request.getHeaders().getFirst("X-Parent-Span-ID");
            
            // Add tracing headers to request
            ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Trace-ID", traceId)
                .header("X-Span-ID", spanId)
                .header("X-Parent-Span-ID", parentSpanId)
                .build();
            
            // Create span
            Span span = Span.current()
                .toBuilder()
                .setSpanKind(SpanKind.SERVER)
                .setName("gateway-request")
                .setAttribute("http.method", request.getMethodValue())
                .setAttribute("http.url", request.getURI().toString())
                .setAttribute("component", "api-gateway")
                .build();
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> {
                    HttpStatus status = exchange.getResponse().getStatusCode();
                    span.setAttribute("http.status_code", status != null ? status.value() : 0);
                    
                    if (status != null && status.is4xxClientError()) {
                        span.setStatus(StatusCode.ERROR, "Client error");
                    } else if (status != null && status.is5xxServerError()) {
                        span.setStatus(StatusCode.ERROR, "Server error");
                    } else {
                        span.setStatus(StatusCode.OK);
                    }
                    
                    span.end();
                });
        };
    }
}
```

---

## 7. Fallback Mechanisms

### 7.1 Service Fallback Implementation

```java
// Fallback Controller
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    
    @GetMapping("/customer")
    public ResponseEntity<CustomerResponse> customerFallback() {
        CustomerResponse fallbackResponse = CustomerResponse.builder()
            .id(-1L)
            .email("fallback@example.com")
            .firstName("Service")
            .lastName("Unavailable")
            .status("UNKNOWN")
            .build();
        
        return ResponseEntity.ok()
            .header("X-Fallback", "true")
            .body(fallbackResponse);
    }
    
    @GetMapping("/order")
    public ResponseEntity<ErrorResponse> orderFallback() {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(503)
            .error("Service Unavailable")
            .message("Order service is temporarily unavailable")
            .path("/api/v1/orders")
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Fallback", "true")
            .body(error);
    }
    
    @GetMapping("/product")
    public ResponseEntity<List<ProductResponse>> productFallback() {
        List<ProductResponse> fallbackProducts = Arrays.asList(
            ProductResponse.builder()
                .id(-1L)
                .name("Service Unavailable")
                .description("Product service is temporarily unavailable")
                .price(BigDecimal.ZERO)
                .status("UNAVAILABLE")
                .build()
        );
        
        return ResponseEntity.ok()
            .header("X-Fallback", "true")
            .body(fallbackProducts);
    }
}
```

---

## 8. Key Takeaways

### API Gateway Benefits:
✅ **Centralized Management**: Single point for cross-cutting concerns  
✅ **Security**: Centralized authentication and authorization  
✅ **Monitoring**: Unified logging and metrics collection  
✅ **Routing**: Intelligent request routing and load balancing  
✅ **Resilience**: Circuit breakers and fallback mechanisms  

### Gateway Patterns:
- **Backend for Frontend (BFF)**: Tailored APIs for different clients
- **API Composition**: Aggregate multiple service calls
- **Protocol Translation**: Convert between different protocols
- **Request/Response Transformation**: Modify requests and responses

### Best Practices:
1. **Keep Gateway Lightweight**: Avoid business logic in gateway
2. **Implement Fallbacks**: Provide graceful degradation
3. **Monitor Everything**: Track performance and health
4. **Use Circuit Breakers**: Prevent cascading failures
5. **Implement Rate Limiting**: Protect backend services

---

## 9. Next Steps

Tomorrow we'll explore **Service Discovery & Load Balancing**, learning:
- Service registry patterns
- Health check mechanisms  
- Dynamic load balancing strategies
- Service mesh integration

**Practice Assignment**: Implement an API Gateway with authentication, rate limiting, and fallback mechanisms for a multi-service application.

---

*"An API Gateway is like a smart receptionist for your microservices - it knows where everything is, who's allowed to access what, and how to handle problems gracefully."*

*"The key to a successful API Gateway is balancing centralization of concerns with maintaining service autonomy."*