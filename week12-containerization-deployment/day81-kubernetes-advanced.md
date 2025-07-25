# Day 81: Kubernetes Advanced - Services, Ingress, ConfigMaps

## Learning Objectives
- Master advanced Service types and networking patterns
- Implement Ingress controllers and traffic routing
- Configure advanced ConfigMaps and Secrets management
- Learn Network Policies and security configurations
- Implement service mesh concepts and traffic management

## Advanced Service Types and Networking

### Service Discovery and DNS

```yaml
# day81-examples/networking/service-discovery.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: production
  labels:
    app: backend
    tier: api
spec:
  selector:
    app: backend
  ports:
  - name: http
    port: 80
    targetPort: 8080
    protocol: TCP
  - name: grpc
    port: 9090
    targetPort: 9090
    protocol: TCP
  type: ClusterIP
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 3600

---
# Headless service for StatefulSet
apiVersion: v1
kind: Service
metadata:
  name: database-headless
  namespace: production
spec:
  clusterIP: None  # Headless service
  selector:
    app: database
  ports:
  - name: postgres
    port: 5432
    targetPort: 5432
```

```java
// day81-examples/service-discovery/ServiceDiscoveryExample.java
@RestController
@RequestMapping("/api/discovery")
public class ServiceDiscoveryController {
    
    private final RestTemplate restTemplate;
    private final LoadBalancerClient loadBalancerClient;
    
    public ServiceDiscoveryController(RestTemplate restTemplate, 
                                    LoadBalancerClient loadBalancerClient) {
        this.restTemplate = restTemplate;
        this.loadBalancerClient = loadBalancerClient;
    }
    
    @GetMapping("/services")
    public Map<String, Object> getServiceInfo() {
        return Map.of(
            "backend-service", callBackendService(),
            "database-service", getDatabaseInfo()
        );
    }
    
    private Map<String, Object> callBackendService() {
        try {
            // Using service name for discovery
            String url = "http://backend-service.production.svc.cluster.local/api/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            return Map.of(
                "status", "UP",
                "response", response.getBody()
            );
        } catch (Exception e) {
            return Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            );
        }
    }
    
    private Map<String, Object> getDatabaseInfo() {
        try {
            // DNS resolution example
            InetAddress[] addresses = InetAddress.getAllByName(
                "database-headless.production.svc.cluster.local"
            );
            
            return Map.of(
                "resolved_ips", Arrays.stream(addresses)
                    .map(InetAddress::getHostAddress)
                    .collect(Collectors.toList()),
                "hostname", InetAddress.getLocalHost().getHostName()
            );
        } catch (UnknownHostException e) {
            return Map.of("error", e.getMessage());
        }
    }
}

@Configuration
public class ServiceDiscoveryConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        
        // Add request interceptor for tracing
        template.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-Service-Name", "frontend-service");
            request.getHeaders().add("X-Request-ID", UUID.randomUUID().toString());
            return execution.execute(request, body);
        });
        
        return template;
    }
    
    @Bean
    public LoadBalancerClient loadBalancerClient() {
        return new RoundRobinLoadBalancer();
    }
}
```

### External Services and Endpoints

```yaml
# day81-examples/networking/external-service.yaml
# External service without selector
apiVersion: v1
kind: Service
metadata:
  name: external-database
  namespace: production
spec:
  ports:
  - name: postgres
    port: 5432
    targetPort: 5432
    protocol: TCP
  type: ClusterIP

---
# Manual endpoints for external service
apiVersion: v1
kind: Endpoints
metadata:
  name: external-database
  namespace: production
subsets:
- addresses:
  - ip: 192.168.1.100  # External database IP
  - ip: 192.168.1.101  # Backup database IP
  ports:
  - name: postgres
    port: 5432
    protocol: TCP

---
# ExternalName service
apiVersion: v1
kind: Service
metadata:
  name: external-api
  namespace: production
spec:
  type: ExternalName
  externalName: api.external-service.com
  ports:
  - name: https
    port: 443
    targetPort: 443
    protocol: TCP
```

### Multi-Port and Named Ports

```yaml
# day81-examples/networking/multi-port-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: multi-port-service
  namespace: production
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-backend-protocol: http
    service.beta.kubernetes.io/aws-load-balancer-ssl-cert: arn:aws:acm:region:account:certificate/cert-id
spec:
  selector:
    app: web-application
  ports:
  - name: http
    port: 80
    targetPort: web-port
    protocol: TCP
  - name: https
    port: 443
    targetPort: web-port
    protocol: TCP
  - name: metrics
    port: 9090
    targetPort: metrics-port
    protocol: TCP
  - name: health
    port: 8081
    targetPort: health-port
    protocol: TCP
  type: LoadBalancer
  loadBalancerSourceRanges:
  - 10.0.0.0/8
  - 172.16.0.0/12

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-application
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: web-application
  template:
    metadata:
      labels:
        app: web-application
    spec:
      containers:
      - name: web-app
        image: web-app:latest
        ports:
        - name: web-port
          containerPort: 8080
          protocol: TCP
        - name: metrics-port
          containerPort: 9090
          protocol: TCP
        - name: health-port
          containerPort: 8081
          protocol: TCP
```

## Ingress Controllers and Traffic Management

### Basic Ingress Configuration

```yaml
# day81-examples/ingress/basic-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: web-ingress
  namespace: production
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - api.example.com
    - www.example.com
    secretName: example-tls
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /api/v1
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 80
      - path: /api/v2
        pathType: Prefix
        backend:
          service:
            name: backend-v2-service
            port:
              number: 80
  - host: www.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
```

### Advanced Ingress with Path-based Routing

```yaml
# day81-examples/ingress/advanced-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: microservices-ingress
  namespace: production
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/configuration-snippet: |
      more_set_headers "X-Frame-Options: DENY";
      more_set_headers "X-Content-Type-Options: nosniff";
      more_set_headers "X-XSS-Protection: 1; mode=block";
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "30"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "30"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "30"
    nginx.ingress.kubernetes.io/use-regex: "true"
spec:
  tls:
  - hosts:
    - api.company.com
    secretName: api-tls-secret
  rules:
  - host: api.company.com
    http:
      paths:
      # User service
      - path: /api/v1/users(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 80
      # Product service
      - path: /api/v1/products(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 80
      # Order service
      - path: /api/v1/orders(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 80
      # Payment service
      - path: /api/v1/payments(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: payment-service
            port:
              number: 80
      # Health checks
      - path: /health(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: health-service
            port:
              number: 80
```

### Ingress with Authentication and Rate Limiting

```yaml
# day81-examples/ingress/auth-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: authenticated-ingress
  namespace: production
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/auth-type: basic
    nginx.ingress.kubernetes.io/auth-secret: basic-auth
    nginx.ingress.kubernetes.io/auth-realm: 'Authentication Required'
    nginx.ingress.kubernetes.io/rate-limit: "10"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/rate-limit-connections: "5"
    nginx.ingress.kubernetes.io/whitelist-source-range: "10.0.0.0/8,172.16.0.0/12"
    nginx.ingress.kubernetes.io/server-snippet: |
      location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
      }
spec:
  tls:
  - hosts:
    - admin.company.com
    secretName: admin-tls-secret
  rules:
  - host: admin.company.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: admin-dashboard
            port:
              number: 80

---
# Basic auth secret
apiVersion: v1
kind: Secret
metadata:
  name: basic-auth
  namespace: production
type: Opaque
data:
  auth: YWRtaW46JGFwcjEkSDY1dnBrTU8kSXp1QTFWc1VWQmdpVXloNDFvclkyLwoK  # admin:secretpassword
```

### Canary Deployments with Ingress

```yaml
# day81-examples/ingress/canary-deployment.yaml
# Production version
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-production
  namespace: production
  annotations:
    kubernetes.io/ingress.class: nginx
spec:
  rules:
  - host: app.company.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: app-service-v1
            port:
              number: 80

---
# Canary version (10% traffic)
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-canary
  namespace: production
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/canary: "true"
    nginx.ingress.kubernetes.io/canary-weight: "10"
    nginx.ingress.kubernetes.io/canary-by-header: "X-Canary"
    nginx.ingress.kubernetes.io/canary-by-header-pattern: "always"
spec:
  rules:
  - host: app.company.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: app-service-v2
            port:
              number: 80
```

## Advanced ConfigMaps and Secrets

### Structured ConfigMaps

```yaml
# day81-examples/config/structured-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: application-config
  namespace: production
data:
  # Application properties
  application.properties: |
    # Server configuration
    server.port=8080
    server.servlet.context-path=/api/v1
    server.compression.enabled=true
    server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    
    # Database configuration
    spring.datasource.hikari.maximum-pool-size=20
    spring.datasource.hikari.minimum-idle=5
    spring.datasource.hikari.connection-timeout=30000
    spring.datasource.hikari.idle-timeout=600000
    spring.datasource.hikari.max-lifetime=1800000
    
    # Cache configuration
    spring.cache.type=redis
    spring.redis.timeout=2000ms
    spring.redis.lettuce.pool.max-active=8
    spring.redis.lettuce.pool.max-idle=8
    spring.redis.lettuce.pool.min-idle=0
    
    # Logging configuration
    logging.level.com.company=INFO
    logging.level.org.springframework.web=DEBUG
    logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n
  
  # Logback configuration
  logback-spring.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
        <springProfile name="kubernetes">
            <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                    <providers>
                        <timestamp/>
                        <logLevel/>
                        <loggerName/>
                        <message/>
                        <mdc/>
                        <arguments/>
                        <stackTrace/>
                    </providers>
                </encoder>
            </appender>
            <root level="INFO">
                <appender-ref ref="STDOUT"/>
            </root>
        </springProfile>
    </configuration>
  
  # Nginx configuration
  nginx.conf: |
    worker_processes auto;
    error_log /var/log/nginx/error.log;
    pid /run/nginx.pid;
    
    events {
        worker_connections 1024;
        use epoll;
        multi_accept on;
    }
    
    http {
        include /etc/nginx/mime.types;
        default_type application/octet-stream;
        
        log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                        '$status $body_bytes_sent "$http_referer" '
                        '"$http_user_agent" "$http_x_forwarded_for"';
        
        access_log /var/log/nginx/access.log main;
        
        sendfile on;
        tcp_nopush on;
        tcp_nodelay on;
        keepalive_timeout 65;
        types_hash_max_size 2048;
        
        gzip on;
        gzip_vary on;
        gzip_min_length 1024;
        gzip_types
            text/plain
            text/css
            text/xml
            text/javascript
            application/javascript
            application/xml+rss
            application/json;
        
        upstream backend {
            server backend-service:80 max_fails=3 fail_timeout=30s;
        }
        
        server {
            listen 80 default_server;
            server_name _;
            
            location /health {
                access_log off;
                return 200 "healthy\n";
            }
            
            location / {
                proxy_pass http://backend;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
            }
        }
    }

---
# Environment-specific overrides
apiVersion: v1
kind: ConfigMap
metadata:
  name: environment-overrides
  namespace: production
data:
  SPRING_PROFILES_ACTIVE: "production,kubernetes"
  LOG_LEVEL: "WARN"
  CACHE_TTL: "7200"
  MAX_CONNECTIONS: "50"
  ENABLE_METRICS: "true"
  METRICS_EXPORT_INTERVAL: "60s"
```

### Secrets with Different Types

```yaml
# day81-examples/config/comprehensive-secrets.yaml
# Generic secrets
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: production
type: Opaque
data:
  database-password: c3VwZXJzZWNyZXRwYXNzd29yZA==
  jwt-secret: eW91ci1qd3Qtc2VjcmV0LWtleS12ZXJ5LWxvbmctYW5kLXNlY3VyZQ==
  encryption-key: MzJieXRlLWVuY3J5cHRpb24ta2V5LWZvci1hZXM=
  api-key: eW91ci1leHRlcm5hbC1hcGkta2V5LXZhbHVl

---
# TLS secrets
apiVersion: v1
kind: Secret
metadata:
  name: tls-certificates
  namespace: production
type: kubernetes.io/tls
data:
  tls.crt: |
    LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0t...
  tls.key: |
    LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0t...

---
# Docker registry secrets
apiVersion: v1
kind: Secret
metadata:
  name: private-registry
  namespace: production
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: |
    eyJhdXRocyI6eyJwcml2YXRlLXJlZ2lzdHJ5LmNvbXBhbnkuY29tIjp7InVzZXJuYW1lIjoiZGVwbG95ZXIiLCJwYXNzd29yZCI6InNlY3VyZXBhc3N3b3JkIiwiZW1haWwiOiJkZXBsb3llckBjb21wYW55LmNvbSIsImF1dGgiOiJaR1Z3Ykc5NVpYSTZjMlZqZFhKbGNHRnpjM2R2Y21RPSJ9fX0=

---
# Service account token (for external services)
apiVersion: v1
kind: Secret
metadata:
  name: external-service-token
  namespace: production
  annotations:
    kubernetes.io/service-account.name: "external-service-account"
type: kubernetes.io/service-account-token
data:
  token: ZXlKaGJHY2lPaUpTVXpJMU5pSXNJbXRwWkNJNkls...

---
# SSH secrets for Git operations
apiVersion: v1
kind: Secret
metadata:
  name: git-ssh-secret
  namespace: production
type: kubernetes.io/ssh-auth
data:
  ssh-privatekey: |
    LS0tLS1CRUdJTiBPUEVOU1NIIFBSSVZBVEUgS0VZLS0tLS0...
```

### Advanced Secret Management

```java
// day81-examples/config/SecretManager.java
@Component
public class SecretManager {
    
    private final Environment environment;
    private final ApplicationContext applicationContext;
    
    public SecretManager(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
    }
    
    @PostConstruct
    public void loadSecrets() {
        loadSecretsFromFiles();
        loadSecretsFromEnvironment();
        configureSecurityProperties();
    }
    
    private void loadSecretsFromFiles() {
        // Load secrets mounted as files
        Path secretsPath = Paths.get("/app/secrets");
        if (Files.exists(secretsPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(secretsPath)) {
                for (Path secretFile : stream) {
                    String secretName = secretFile.getFileName().toString();
                    String secretValue = Files.readString(secretFile).trim();
                    System.setProperty("secret." + secretName, secretValue);
                }
            } catch (IOException e) {
                log.error("Failed to load secrets from files", e);
            }
        }
    }
    
    private void loadSecretsFromEnvironment() {
        // Load secrets from environment variables
        Map<String, String> secrets = environment.getProperties().entrySet().stream()
            .filter(entry -> entry.getKey().toString().startsWith("SECRET_"))
            .collect(Collectors.toMap(
                entry -> entry.getKey().toString().toLowerCase().replace("secret_", "secret."),
                entry -> entry.getValue().toString()
            ));
        
        secrets.forEach(System::setProperty);
    }
    
    private void configureSecurityProperties() {
        // Configure security-related properties
        if (hasSecret("jwt-secret")) {
            System.setProperty("jwt.secret", getSecret("jwt-secret"));
        }
        
        if (hasSecret("encryption-key")) {
            System.setProperty("app.encryption.key", getSecret("encryption-key"));
        }
        
        if (hasSecret("database-password")) {
            System.setProperty("spring.datasource.password", getSecret("database-password"));
        }
    }
    
    public String getSecret(String name) {
        // Try multiple sources
        String secret = System.getProperty("secret." + name);
        if (secret == null) {
            secret = environment.getProperty("SECRET_" + name.toUpperCase().replace("-", "_"));
        }
        return secret;
    }
    
    public boolean hasSecret(String name) {
        return getSecret(name) != null;
    }
    
    @EventListener
    public void handleSecretRotation(SecretRotationEvent event) {
        log.info("Rotating secret: {}", event.getSecretName());
        
        // Refresh specific components that use the rotated secret
        switch (event.getSecretName()) {
            case "database-password":
                refreshDataSource();
                break;
            case "jwt-secret":
                refreshJwtConfiguration();
                break;
            case "api-key":
                refreshExternalApiClients();
                break;
        }
    }
    
    private void refreshDataSource() {
        // Refresh database connections
        HikariDataSource dataSource = applicationContext.getBean(HikariDataSource.class);
        dataSource.getHikariConfigMXBean().setPassword(getSecret("database-password"));
    }
    
    private void refreshJwtConfiguration() {
        // Refresh JWT configuration
        // Implementation depends on JWT library used
    }
    
    private void refreshExternalApiClients() {
        // Refresh API clients with new credentials
        // Implementation depends on HTTP client used
    }
}

@Configuration
@EnableConfigurationProperties(SecretConfiguration.class)
public class SecretConfiguration {
    
    @ConfigurationProperties(prefix = "app.secrets")
    @Data
    public static class SecretProperties {
        private String jwtSecret;
        private String encryptionKey;
        private String databasePassword;
        private String apiKey;
        private Duration rotationInterval = Duration.ofHours(24);
        private Boolean autoRotate = false;
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.secrets.auto-rotate", havingValue = "true")
    public SecretRotationScheduler secretRotationScheduler() {
        return new SecretRotationScheduler();
    }
}
```

## Network Policies and Security

### Basic Network Policies

```yaml
# day81-examples/security/network-policies.yaml
# Default deny all ingress traffic
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
  namespace: production
spec:
  podSelector: {}
  policyTypes:
  - Ingress

---
# Allow ingress from specific namespaces
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-frontend-ingress
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: backend-service
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: frontend
    - podSelector:
        matchLabels:
          app: frontend-service
    ports:
    - protocol: TCP
      port: 8080

---
# Allow egress to specific services
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-database-egress
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: application
  policyTypes:
  - Egress
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
  # Allow DNS resolution
  - to: []
    ports:
    - protocol: UDP
      port: 53
    - protocol: TCP
      port: 53
```

### Comprehensive Security Policies

```yaml
# day81-examples/security/comprehensive-network-policy.yaml
# Microservices network segmentation
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: microservices-segmentation
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: microservice
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # Allow ingress from API gateway
  - from:
    - podSelector:
        matchLabels:
          app: api-gateway
    ports:
    - protocol: TCP
      port: 8080
  # Allow ingress from same tier (inter-service communication)
  - from:
    - podSelector:
        matchLabels:
          tier: microservice
    ports:
    - protocol: TCP
      port: 8080
  # Allow ingress from monitoring
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 8081  # Management port
  egress:
  # Allow egress to databases
  - to:
    - podSelector:
        matchLabels:
          tier: database
    ports:
    - protocol: TCP
      port: 5432
    - protocol: TCP
      port: 3306
  # Allow egress to cache
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
  # Allow egress to message brokers
  - to:
    - podSelector:
        matchLabels:
          app: kafka
    ports:
    - protocol: TCP
      port: 9092
  # Allow DNS and external APIs
  - to: []
    ports:
    - protocol: UDP
      port: 53
    - protocol: TCP
      port: 443
    - protocol: TCP
      port: 80

---
# Database isolation policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: database-isolation
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: database
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # Only allow access from applications
  - from:
    - podSelector:
        matchLabels:
          tier: microservice
    - podSelector:
        matchLabels:
          tier: application
    ports:
    - protocol: TCP
      port: 5432
    - protocol: TCP
      port: 3306
  # Allow monitoring
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 9187  # PostgreSQL exporter
  egress:
  # Allow DNS resolution only
  - to: []
    ports:
    - protocol: UDP
      port: 53
```

## Pod Security and RBAC

### Pod Security Standards

```yaml
# day81-examples/security/pod-security.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: secure-production
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted

---
# Secure deployment with security context
apiVersion: apps/v1
kind: Deployment
metadata:
  name: secure-application
  namespace: secure-production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: secure-app
  template:
    metadata:
      labels:
        app: secure-app
    spec:
      serviceAccountName: secure-app-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        runAsGroup: 3000
        fsGroup: 2000
        seccompProfile:
          type: RuntimeDefault
      containers:
      - name: app
        image: secure-app:latest
        ports:
        - containerPort: 8080
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          runAsNonRoot: true
          runAsUser: 1001
          capabilities:
            drop:
            - ALL
            add:
            - NET_BIND_SERVICE
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        volumeMounts:
        - name: tmp-volume
          mountPath: /tmp
        - name: var-cache
          mountPath: /var/cache
        - name: var-run
          mountPath: /var/run
      volumes:
      - name: tmp-volume
        emptyDir: {}
      - name: var-cache
        emptyDir: {}
      - name: var-run
        emptyDir: {}
      automountServiceAccountToken: false
```

### RBAC Configuration

```yaml
# day81-examples/security/rbac.yaml
# Service Account
apiVersion: v1
kind: ServiceAccount
metadata:
  name: application-sa
  namespace: production
automountServiceAccountToken: true

---
# Role for application namespace
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: production
  name: application-role
rules:
# ConfigMap access
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "list", "watch"]
  resourceNames: ["app-config", "environment-config"]
# Secret access (limited)
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get"]
  resourceNames: ["app-secrets"]
# Pod information access
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list"]
# Service discovery
- apiGroups: [""]
  resources: ["services", "endpoints"]
  verbs: ["get", "list", "watch"]
# Events (for debugging)
- apiGroups: [""]
  resources: ["events"]
  verbs: ["create"]

---
# ClusterRole for cross-namespace access
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: service-discovery-role
rules:
# Service discovery across namespaces
- apiGroups: [""]
  resources: ["services"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["endpoints"]
  verbs: ["get", "list", "watch"]
# Node information for scheduling
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list"]

---
# RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: application-rolebinding
  namespace: production
subjects:
- kind: ServiceAccount
  name: application-sa
  namespace: production
roleRef:
  kind: Role
  name: application-role
  apiGroup: rbac.authorization.k8s.io

---
# ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: service-discovery-binding
subjects:
- kind: ServiceAccount
  name: application-sa
  namespace: production
roleRef:
  kind: ClusterRole
  name: service-discovery-role
  apiGroup: rbac.authorization.k8s.io

---
# Monitoring service account with limited permissions
apiVersion: v1
kind: ServiceAccount
metadata:
  name: monitoring-sa
  namespace: monitoring

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring-role
rules:
- apiGroups: [""]
  resources: ["nodes", "nodes/metrics", "pods", "services", "endpoints"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch"]
- nonResourceURLs: ["/metrics", "/metrics/cadvisor"]
  verbs: ["get"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: monitoring-binding
subjects:
- kind: ServiceAccount
  name: monitoring-sa
  namespace: monitoring
roleRef:
  kind: ClusterRole
  name: monitoring-role
  apiGroup: rbac.authorization.k8s.io
```

## Service Mesh Fundamentals

### Istio Basic Configuration

```yaml
# day81-examples/service-mesh/istio-config.yaml
# Enable Istio injection for namespace
apiVersion: v1
kind: Namespace
metadata:
  name: istio-system
  labels:
    istio-injection: enabled

---
# Gateway configuration
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: application-gateway
  namespace: production
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - api.company.com
    tls:
      httpsRedirect: true
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: api-tls-secret
    hosts:
    - api.company.com

---
# Virtual Service for traffic routing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: application-vs
  namespace: production
spec:
  hosts:
  - api.company.com
  gateways:
  - application-gateway
  http:
  - match:
    - uri:
        prefix: /api/v1/users
    route:
    - destination:
        host: user-service
        port:
          number: 80
      weight: 90
    - destination:
        host: user-service-v2
        port:
          number: 80
      weight: 10
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
  - match:
    - uri:
        prefix: /api/v1/products
    route:
    - destination:
        host: product-service
        port:
          number: 80
    timeout: 10s

---
# Destination Rule for load balancing
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: user-service-dr
  namespace: production
spec:
  host: user-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
    loadBalancer:
      simple: LEAST_CONN
    outlierDetection:
      consecutiveErrors: 3
      interval: 30s
      baseEjectionTime: 30s
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
    trafficPolicy:
      connectionPool:
        tcp:
          maxConnections: 50
```

### Traffic Management

```yaml
# day81-examples/service-mesh/traffic-management.yaml
# Circuit breaker configuration
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: circuit-breaker
  namespace: production
spec:
  host: external-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 10
      http:
        http1MaxPendingRequests: 10
        maxRequestsPerConnection: 2
        consecutiveGatewayErrors: 5
        interval: 10s
        baseEjectionTime: 30s
    outlierDetection:
      consecutiveErrors: 3
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50

---
# Rate limiting
apiVersion: networking.istio.io/v1beta1
kind: EnvoyFilter
metadata:
  name: rate-limit-filter
  namespace: production
spec:
  workloadSelector:
    labels:
      app: api-gateway
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.local_ratelimit
        typed_config:
          "@type": type.googleapis.com/udpa.type.v1.TypedStruct
          type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
          value:
            stat_prefix: rate_limiter
            token_bucket:
              max_tokens: 100
              tokens_per_fill: 100
              fill_interval: 60s
            filter_enabled:
              runtime_key: rate_limit_enabled
              default_value:
                numerator: 100
                denominator: HUNDRED
            filter_enforced:
              runtime_key: rate_limit_enforced
              default_value:
                numerator: 100
                denominator: HUNDRED

---
# Security policy
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: production
spec:
  mtls:
    mode: STRICT

---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: api-access-policy
  namespace: production
spec:
  selector:
    matchLabels:
      app: api-service
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/frontend/sa/frontend-sa"]
  - to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/v1/*"]
  - when:
    - key: custom_header
      values: ["allowed-value"]
```

## Practical Implementation Example

### Complete E-commerce Microservices Setup

```yaml
# day81-examples/complete-example/ecommerce-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce
  labels:
    istio-injection: enabled
    pod-security.kubernetes.io/enforce: restricted

---
# day81-examples/complete-example/gateway-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
  namespace: ecommerce
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/rate-limit: "1000"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - api.ecommerce.com
    - admin.ecommerce.com
    secretName: ecommerce-tls
  rules:
  - host: api.ecommerce.com
    http:
      paths:
      - path: /api/v1/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 80
      - path: /api/v1/users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 80
      - path: /api/v1/products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 80
      - path: /api/v1/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 80
      - path: /api/v1/payments
        pathType: Prefix
        backend:
          service:
            name: payment-service
            port:
              number: 80
  - host: admin.ecommerce.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: admin-dashboard
            port:
              number: 80

---
# day81-examples/complete-example/network-policies.yaml
# Default deny policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny
  namespace: ecommerce
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress

---
# Microservices communication policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: microservices-communication
  namespace: ecommerce
spec:
  podSelector:
    matchLabels:
      tier: microservice
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
    - podSelector:
        matchLabels:
          tier: microservice
    - podSelector:
        matchLabels:
          app: api-gateway
  egress:
  - to:
    - podSelector:
        matchLabels:
          tier: database
  - to:
    - podSelector:
        matchLabels:
          app: redis
  - to:
    - podSelector:
        matchLabels:
          app: kafka
  - to: []
    ports:
    - protocol: UDP
      port: 53
    - protocol: TCP
      port: 443
```

```java
// day81-examples/complete-example/ServiceMeshConfiguration.java
@Configuration
@EnableWebSecurity
public class ServiceMeshConfiguration {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        
        // Add service mesh headers
        template.getInterceptors().add((request, body, execution) -> {
            // Tracing headers for Istio
            request.getHeaders().add("x-request-id", UUID.randomUUID().toString());
            request.getHeaders().add("x-b3-traceid", getCurrentTraceId());
            request.getHeaders().add("x-b3-spanid", getCurrentSpanId());
            
            // Service identification
            request.getHeaders().add("x-service-name", "user-service");
            request.getHeaders().add("x-service-version", "v1.0.0");
            
            return execution.execute(request, body);
        });
        
        return template;
    }
    
    @Bean
    public ServiceMeshHealthIndicator serviceMeshHealthIndicator() {
        return new ServiceMeshHealthIndicator();
    }
    
    private String getCurrentTraceId() {
        // Implementation depends on tracing library (Jaeger, Zipkin, etc.)
        return MDC.get("traceId");
    }
    
    private String getCurrentSpanId() {
        return MDC.get("spanId");
    }
}

@Component
public class ServiceMeshHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check Istio sidecar health
            boolean sidecarHealthy = checkSidecarHealth();
            
            // Check service mesh connectivity
            boolean meshConnectivity = checkMeshConnectivity();
            
            if (sidecarHealthy && meshConnectivity) {
                return Health.up()
                    .withDetail("sidecar", "healthy")
                    .withDetail("mesh", "connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("sidecar", sidecarHealthy ? "healthy" : "unhealthy")
                    .withDetail("mesh", meshConnectivity ? "connected" : "disconnected")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private boolean checkSidecarHealth() {
        try {
            // Check Envoy admin interface
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:15000/ready", String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkMeshConnectivity() {
        try {
            // Check connectivity to other services through mesh
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://product-service.ecommerce.svc.cluster.local/health", String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
```

## Summary

### Key Concepts Covered
- **Advanced Services**: Multi-port services, external services, and service discovery patterns
- **Ingress Management**: Path-based routing, TLS termination, and canary deployments
- **Configuration Management**: Structured ConfigMaps, secret rotation, and security best practices
- **Network Security**: Network policies, pod security standards, and RBAC
- **Service Mesh**: Istio fundamentals, traffic management, and security policies

### Best Practices Implemented
- **Security First**: Network policies, pod security contexts, and RBAC
- **Configuration Management**: Externalized configuration with proper secret handling
- **Traffic Management**: Load balancing, circuit breakers, and canary deployments
- **Observability**: Health checks, metrics, and distributed tracing
- **High Availability**: Anti-affinity rules and disruption budgets

### kubectl Advanced Commands

```bash
# Network troubleshooting
kubectl get networkpolicies
kubectl describe networkpolicy <policy-name>
kubectl get endpoints <service-name>

# Ingress management
kubectl get ingress -o wide
kubectl describe ingress <ingress-name>
kubectl get certificaterequests

# Configuration debugging
kubectl get configmaps -o yaml
kubectl get secrets -o yaml
kubectl create configmap from-env-file

# RBAC verification
kubectl auth can-i get pods --as=system:serviceaccount:default:my-sa
kubectl get rolebindings -o wide
kubectl describe clusterrole <role-name>
```

### Next Steps
- Day 82: CI/CD with Jenkins & GitHub Actions
- Learn automated deployment pipelines
- Implement GitOps workflows
- Master container registry management

---

## Exercises

### Exercise 1: Advanced Ingress
Configure path-based routing with TLS termination and rate limiting for a microservices application.

### Exercise 2: Network Security
Implement comprehensive network policies for microservices isolation and security.

### Exercise 3: Configuration Management
Set up dynamic configuration management with secret rotation capabilities.

### Exercise 4: Service Mesh
Deploy Istio service mesh with traffic management and security policies.

### Exercise 5: Complete Integration
Build a production-ready microservices deployment with all advanced features.