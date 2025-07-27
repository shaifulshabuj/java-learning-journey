# Day 106: Cloud-Native Java - Containerization, Kubernetes & Service Mesh

## Learning Objectives
- Master containerization strategies for Java applications
- Implement cloud-native patterns with Kubernetes orchestration
- Design and deploy service mesh architectures with Istio
- Optimize Java applications for cloud environments
- Build resilient, scalable microservices with cloud-native technologies

## Topics Covered
1. Cloud-Native Application Design Principles
2. Advanced Docker Containerization for Java
3. Kubernetes Deployment and Orchestration
4. Service Mesh Implementation with Istio
5. Cloud-Native Observability and Monitoring
6. Advanced Networking and Traffic Management
7. Production-Ready Cloud Deployment Strategies

## 1. Cloud-Native Application Design Principles

### Twelve-Factor App Implementation

```java
// Configuration management for cloud-native apps
@Configuration
@EnableConfigurationProperties(CloudNativeProperties.class)
public class CloudNativeConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "app.cloud.environment", havingValue = "kubernetes")
    public CloudEnvironmentDetector kubernetesEnvironmentDetector() {
        return new KubernetesEnvironmentDetector();
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.cloud.environment", havingValue = "aws")
    public CloudEnvironmentDetector awsEnvironmentDetector() {
        return new AWSEnvironmentDetector();
    }
    
    @Bean
    public CloudNativeHealthIndicator cloudNativeHealthIndicator(
            CloudEnvironmentDetector environmentDetector) {
        return new CloudNativeHealthIndicator(environmentDetector);
    }
}

@ConfigurationProperties(prefix = "app.cloud")
@Validated
public class CloudNativeProperties {
    
    /**
     * Cloud environment type (kubernetes, aws, gcp, azure)
     */
    @NotBlank
    private String environment = "kubernetes";
    
    /**
     * Application instance ID for tracking
     */
    private String instanceId = UUID.randomUUID().toString();
    
    /**
     * Deployment configuration
     */
    @Valid
    private DeploymentConfig deployment = new DeploymentConfig();
    
    /**
     * Networking configuration
     */
    @Valid
    private NetworkingConfig networking = new NetworkingConfig();
    
    /**
     * Observability configuration
     */
    @Valid
    private ObservabilityConfig observability = new ObservabilityConfig();
    
    public static class DeploymentConfig {
        
        /**
         * Deployment strategy (rolling, blue-green, canary)
         */
        private String strategy = "rolling";
        
        /**
         * Maximum unavailable pods during deployment
         */
        @Min(0)
        private int maxUnavailable = 1;
        
        /**
         * Maximum surge pods during deployment
         */
        @Min(0)
        private int maxSurge = 1;
        
        /**
         * Readiness probe configuration
         */
        private ProbeConfig readinessProbe = new ProbeConfig();
        
        /**
         * Liveness probe configuration
         */
        private ProbeConfig livenessProbe = new ProbeConfig();
        
        // Getters and setters
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        
        public int getMaxUnavailable() { return maxUnavailable; }
        public void setMaxUnavailable(int maxUnavailable) { this.maxUnavailable = maxUnavailable; }
        
        public int getMaxSurge() { return maxSurge; }
        public void setMaxSurge(int maxSurge) { this.maxSurge = maxSurge; }
        
        public ProbeConfig getReadinessProbe() { return readinessProbe; }
        public void setReadinessProbe(ProbeConfig readinessProbe) { this.readinessProbe = readinessProbe; }
        
        public ProbeConfig getLivenessProbe() { return livenessProbe; }
        public void setLivenessProbe(ProbeConfig livenessProbe) { this.livenessProbe = livenessProbe; }
    }
    
    public static class ProbeConfig {
        
        /**
         * Initial delay before first probe
         */
        @Min(0)
        private int initialDelaySeconds = 30;
        
        /**
         * Period between probes
         */
        @Min(1)
        private int periodSeconds = 10;
        
        /**
         * Timeout for each probe
         */
        @Min(1)
        private int timeoutSeconds = 5;
        
        /**
         * Number of consecutive successes required
         */
        @Min(1)
        private int successThreshold = 1;
        
        /**
         * Number of consecutive failures before restart
         */
        @Min(1)
        private int failureThreshold = 3;
        
        // Getters and setters
        public int getInitialDelaySeconds() { return initialDelaySeconds; }
        public void setInitialDelaySeconds(int initialDelaySeconds) { 
            this.initialDelaySeconds = initialDelaySeconds; 
        }
        
        public int getPeriodSeconds() { return periodSeconds; }
        public void setPeriodSeconds(int periodSeconds) { this.periodSeconds = periodSeconds; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        
        public int getSuccessThreshold() { return successThreshold; }
        public void setSuccessThreshold(int successThreshold) { 
            this.successThreshold = successThreshold; 
        }
        
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { 
            this.failureThreshold = failureThreshold; 
        }
    }
    
    public static class NetworkingConfig {
        
        /**
         * Service mesh enabled
         */
        private boolean serviceMeshEnabled = true;
        
        /**
         * TLS mode (STRICT, PERMISSIVE, DISABLED)
         */
        private String tlsMode = "STRICT";
        
        /**
         * Circuit breaker configuration
         */
        private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
        
        /**
         * Retry policy configuration
         */
        private RetryConfig retry = new RetryConfig();
        
        // Getters and setters
        public boolean isServiceMeshEnabled() { return serviceMeshEnabled; }
        public void setServiceMeshEnabled(boolean serviceMeshEnabled) { 
            this.serviceMeshEnabled = serviceMeshEnabled; 
        }
        
        public String getTlsMode() { return tlsMode; }
        public void setTlsMode(String tlsMode) { this.tlsMode = tlsMode; }
        
        public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
        public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { 
            this.circuitBreaker = circuitBreaker; 
        }
        
        public RetryConfig getRetry() { return retry; }
        public void setRetry(RetryConfig retry) { this.retry = retry; }
    }
    
    public static class CircuitBreakerConfig {
        
        /**
         * Consecutive failures threshold
         */
        @Min(1)
        private int consecutiveErrors = 5;
        
        /**
         * Interval for circuit breaker reset
         */
        @Min(1000)
        private long intervalMs = 30000;
        
        /**
         * Base ejection time
         */
        @Min(1000)
        private long baseEjectionTimeMs = 30000;
        
        // Getters and setters
        public int getConsecutiveErrors() { return consecutiveErrors; }
        public void setConsecutiveErrors(int consecutiveErrors) { 
            this.consecutiveErrors = consecutiveErrors; 
        }
        
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
        
        public long getBaseEjectionTimeMs() { return baseEjectionTimeMs; }
        public void setBaseEjectionTimeMs(long baseEjectionTimeMs) { 
            this.baseEjectionTimeMs = baseEjectionTimeMs; 
        }
    }
    
    public static class RetryConfig {
        
        /**
         * Number of retry attempts
         */
        @Min(0)
        private int attempts = 3;
        
        /**
         * Per-try timeout
         */
        @Min(100)
        private long perTryTimeoutMs = 5000;
        
        /**
         * Retry on status codes
         */
        private Set<Integer> retryOn = Set.of(502, 503, 504);
        
        // Getters and setters
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        
        public long getPerTryTimeoutMs() { return perTryTimeoutMs; }
        public void setPerTryTimeoutMs(long perTryTimeoutMs) { 
            this.perTryTimeoutMs = perTryTimeoutMs; 
        }
        
        public Set<Integer> getRetryOn() { return retryOn; }
        public void setRetryOn(Set<Integer> retryOn) { this.retryOn = retryOn; }
    }
    
    public static class ObservabilityConfig {
        
        /**
         * Tracing enabled
         */
        private boolean tracingEnabled = true;
        
        /**
         * Metrics enabled
         */
        private boolean metricsEnabled = true;
        
        /**
         * Logging level
         */
        private String loggingLevel = "INFO";
        
        /**
         * Structured logging enabled
         */
        private boolean structuredLogging = true;
        
        // Getters and setters
        public boolean isTracingEnabled() { return tracingEnabled; }
        public void setTracingEnabled(boolean tracingEnabled) { this.tracingEnabled = tracingEnabled; }
        
        public boolean isMetricsEnabled() { return metricsEnabled; }
        public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }
        
        public String getLoggingLevel() { return loggingLevel; }
        public void setLoggingLevel(String loggingLevel) { this.loggingLevel = loggingLevel; }
        
        public boolean isStructuredLogging() { return structuredLogging; }
        public void setStructuredLogging(boolean structuredLogging) { 
            this.structuredLogging = structuredLogging; 
        }
    }
    
    // Main class getters and setters
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    
    public DeploymentConfig getDeployment() { return deployment; }
    public void setDeployment(DeploymentConfig deployment) { this.deployment = deployment; }
    
    public NetworkingConfig getNetworking() { return networking; }
    public void setNetworking(NetworkingConfig networking) { this.networking = networking; }
    
    public ObservabilityConfig getObservability() { return observability; }
    public void setObservability(ObservabilityConfig observability) { 
        this.observability = observability; 
    }
}

// Cloud environment detection
public interface CloudEnvironmentDetector {
    String getEnvironmentType();
    Map<String, String> getEnvironmentMetadata();
    boolean isRunningInCloud();
    String getInstanceId();
    String getAvailabilityZone();
}

@Component
public class KubernetesEnvironmentDetector implements CloudEnvironmentDetector {
    
    private static final String SERVICE_ACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";
    private static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
    private static final String HOSTNAME_ENV = "HOSTNAME";
    
    private final Logger logger = LoggerFactory.getLogger(KubernetesEnvironmentDetector.class);
    
    @Override
    public String getEnvironmentType() {
        return "kubernetes";
    }
    
    @Override
    public Map<String, String> getEnvironmentMetadata() {
        Map<String, String> metadata = new HashMap<>();
        
        try {
            // Pod information
            metadata.put("pod.name", getPodName());
            metadata.put("pod.namespace", getPodNamespace());
            metadata.put("pod.node", getPodNodeName());
            
            // Service information
            metadata.put("kubernetes.host", System.getenv(KUBERNETES_SERVICE_HOST));
            
            // Cluster information
            metadata.put("cluster.name", getClusterName());
            
        } catch (Exception e) {
            logger.warn("Failed to retrieve Kubernetes metadata", e);
        }
        
        return metadata;
    }
    
    @Override
    public boolean isRunningInCloud() {
        return Files.exists(Paths.get(SERVICE_ACCOUNT_PATH)) ||
               System.getenv(KUBERNETES_SERVICE_HOST) != null;
    }
    
    @Override
    public String getInstanceId() {
        return getPodName();
    }
    
    @Override
    public String getAvailabilityZone() {
        try {
            // Try to get zone from node labels
            return getNodeLabel("topology.kubernetes.io/zone");
        } catch (Exception e) {
            logger.debug("Could not determine availability zone", e);
            return "unknown";
        }
    }
    
    private String getPodName() {
        return System.getenv(HOSTNAME_ENV);
    }
    
    private String getPodNamespace() {
        try {
            Path namespacePath = Paths.get(SERVICE_ACCOUNT_PATH, "namespace");
            if (Files.exists(namespacePath)) {
                return Files.readString(namespacePath).trim();
            }
        } catch (Exception e) {
            logger.debug("Could not read namespace from service account", e);
        }
        return "default";
    }
    
    private String getPodNodeName() {
        // This would typically come from Kubernetes API or environment variable
        return System.getenv("NODE_NAME");
    }
    
    private String getClusterName() {
        // This would typically come from cluster metadata
        return System.getenv("CLUSTER_NAME");
    }
    
    private String getNodeLabel(String labelKey) {
        // This would require Kubernetes API client to get node labels
        return "unknown";
    }
}

// Cloud-native health indicator
@Component
public class CloudNativeHealthIndicator implements HealthIndicator {
    
    private final CloudEnvironmentDetector environmentDetector;
    private final CloudNativeProperties properties;
    
    public CloudNativeHealthIndicator(CloudEnvironmentDetector environmentDetector,
                                    CloudNativeProperties properties) {
        this.environmentDetector = environmentDetector;
        this.properties = properties;
    }
    
    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Environment information
            details.put("environment", environmentDetector.getEnvironmentType());
            details.put("instance-id", environmentDetector.getInstanceId());
            details.put("availability-zone", environmentDetector.getAvailabilityZone());
            details.put("running-in-cloud", environmentDetector.isRunningInCloud());
            
            // Configuration status
            details.put("service-mesh-enabled", properties.getNetworking().isServiceMeshEnabled());
            details.put("tracing-enabled", properties.getObservability().isTracingEnabled());
            details.put("metrics-enabled", properties.getObservability().isMetricsEnabled());
            
            // Environment metadata
            details.putAll(environmentDetector.getEnvironmentMetadata());
            
            return Health.up()
                    .withDetails(details)
                    .build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

// Graceful shutdown configuration
@Component
public class GracefulShutdownConfiguration implements DisposableBean {
    
    private final ApplicationContext applicationContext;
    private final Logger logger = LoggerFactory.getLogger(GracefulShutdownConfiguration.class);
    
    public GracefulShutdownConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        registerShutdownHook();
    }
    
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Starting graceful shutdown...");
            
            try {
                // Give ongoing requests time to complete
                Thread.sleep(5000);
                
                // Stop accepting new requests (handled by Spring Boot)
                logger.info("Stopped accepting new requests");
                
                // Close resources
                if (applicationContext instanceof ConfigurableApplicationContext) {
                    ((ConfigurableApplicationContext) applicationContext).close();
                }
                
                logger.info("Graceful shutdown completed");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Graceful shutdown interrupted");
            } catch (Exception e) {
                logger.error("Error during graceful shutdown", e);
            }
        }));
    }
    
    @Override
    public void destroy() throws Exception {
        logger.info("Application context destroy callback");
    }
}
```

## 2. Advanced Docker Containerization

### Optimized Dockerfile for Java Applications

```dockerfile
# Multi-stage Dockerfile for Java application
FROM maven:3.9.4-openjdk-21 AS build

# Set working directory
WORKDIR /app

# Copy dependency files first for better layer caching
COPY pom.xml .
COPY src/main/resources/application.properties src/main/resources/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Production stage
FROM openjdk:21-jre-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+PrintGCDetails \
               -XX:+PrintGCTimeStamps \
               -Xloggc:/app/logs/gc.log"

# Application configuration
ENV SPRING_PROFILES_ACTIVE=cloud
ENV SERVER_PORT=8080
ENV MANAGEMENT_SERVER_PORT=8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8080 8081

# Entry point with signal handling
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose for Development

```yaml
# docker-compose.yml
version: '3.8'

services:
  trading-service:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/trading
      - SPRING_DATASOURCE_USERNAME=trading
      - SPRING_DATASOURCE_PASSWORD=trading123
      - SPRING_REDIS_HOST=redis
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0
      - MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - trading-network
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 1g
          cpus: '0.5'
        reservations:
          memory: 512m
          cpus: '0.25'

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=trading
      - POSTGRES_USER=trading
      - POSTGRES_PASSWORD=trading123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U trading"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - trading-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - trading-network

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - trading-network

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    healthcheck:
      test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - trading-network

  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"
    networks:
      - trading-network

  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    networks:
      - trading-network

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./docker/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./docker/grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - trading-network

networks:
  trading-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
  grafana_data:
```

## 3. Kubernetes Deployment and Orchestration

### Kubernetes Manifests

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: trading-system
  labels:
    istio-injection: enabled
    name: trading-system

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trading-service-config
  namespace: trading-system
data:
  application.yml: |
    spring:
      profiles:
        active: kubernetes
      datasource:
        url: jdbc:postgresql://postgres-service:5432/trading
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
      redis:
        host: redis-service
        port: 6379
      kafka:
        bootstrap-servers: kafka-service:9092
    
    management:
      endpoints:
        web:
          exposure:
            include: "*"
      endpoint:
        health:
          probes:
            enabled: true
      tracing:
        sampling:
          probability: 0.1
      zipkin:
        tracing:
          endpoint: http://zipkin-service:9411/api/v2/spans
    
    app:
      cloud:
        environment: kubernetes
        deployment:
          strategy: rolling
          readinessProbe:
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          livenessProbe:
            initialDelaySeconds: 60
            periodSeconds: 20
            timeoutSeconds: 10
            failureThreshold: 3

---
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: trading-service-secrets
  namespace: trading-system
type: Opaque
data:
  DB_USERNAME: dHJhZGluZw==  # trading (base64)
  DB_PASSWORD: dHJhZGluZzEyMw==  # trading123 (base64)

---
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trading-service
  namespace: trading-system
  labels:
    app: trading-service
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: trading-service
      version: v1
  template:
    metadata:
      labels:
        app: trading-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: trading-service-account
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
      - name: trading-service
        image: trading-service:latest
        imagePullPolicy: Always
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        - name: management
          containerPort: 8081
          protocol: TCP
        env:
        - name: SPRING_CONFIG_LOCATION
          value: "classpath:/application.yml,/etc/config/application.yml"
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: trading-service-secrets
              key: DB_USERNAME
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: trading-service-secrets
              key: DB_PASSWORD
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        volumeMounts:
        - name: config-volume
          mountPath: /etc/config
        - name: logs-volume
          mountPath: /app/logs
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: management
          initialDelaySeconds: 60
          periodSeconds: 20
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: management
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /actuator/health/startup
            port: management
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 12
      volumes:
      - name: config-volume
        configMap:
          name: trading-service-config
      - name: logs-volume
        emptyDir: {}
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - trading-service
              topologyKey: kubernetes.io/hostname

---
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: trading-service
  namespace: trading-system
  labels:
    app: trading-service
spec:
  selector:
    app: trading-service
  ports:
  - name: http
    port: 8080
    targetPort: 8080
    protocol: TCP
  - name: management
    port: 8081
    targetPort: 8081
    protocol: TCP
  type: ClusterIP

---
# service-account.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: trading-service-account
  namespace: trading-system

---
# horizontal-pod-autoscaler.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: trading-service-hpa
  namespace: trading-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: trading-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30

---
# pod-disruption-budget.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: trading-service-pdb
  namespace: trading-system
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: trading-service

---
# network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: trading-service-network-policy
  namespace: trading-system
spec:
  podSelector:
    matchLabels:
      app: trading-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
    - namespaceSelector:
        matchLabels:
          name: trading-system
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 8081
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: trading-system
  - to: []
    ports:
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53
```

## 4. Service Mesh Implementation with Istio

### Istio Configuration

```yaml
# istio-gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: trading-gateway
  namespace: trading-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - trading.example.com
    tls:
      httpsRedirect: true
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: trading-tls-secret
    hosts:
    - trading.example.com

---
# virtual-service.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: trading-service-vs
  namespace: trading-system
spec:
  hosts:
  - trading.example.com
  gateways:
  - trading-gateway
  http:
  - match:
    - uri:
        prefix: /api/v1/trading
    route:
    - destination:
        host: trading-service
        port:
          number: 8080
        subset: v1
      weight: 90
    - destination:
        host: trading-service
        port:
          number: 8080
        subset: v2
      weight: 10
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s
    retries:
      attempts: 3
      perTryTimeout: 10s
      retryOn: 5xx,reset,connect-failure,refused-stream
    timeout: 30s

---
# destination-rule.yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: trading-service-dr
  namespace: trading-system
spec:
  host: trading-service
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
    connectionPool:
      tcp:
        maxConnections: 100
        connectTimeout: 30s
        tcpKeepalive:
          time: 7200s
          interval: 75s
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 100
        maxRequestsPerConnection: 10
        maxRetries: 3
        consecutiveGatewayErrors: 5
        interval: 30s
        baseEjectionTime: 30s
        maxEjectionPercent: 50
        minHealthPercent: 50
    circuitBreaker:
      consecutiveGatewayErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
      minHealthPercent: 50
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2

---
# peer-authentication.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: trading-service-pa
  namespace: trading-system
spec:
  selector:
    matchLabels:
      app: trading-service
  mtls:
    mode: STRICT

---
# authorization-policy.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: trading-service-authz
  namespace: trading-system
spec:
  selector:
    matchLabels:
      app: trading-service
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/trading-system/sa/trading-client"]
    to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
        paths: ["/api/v1/trading/*"]
  - from:
    - source:
        namespaces: ["istio-system"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/actuator/health", "/actuator/ready"]

---
# service-monitor.yaml
apiVersion: networking.istio.io/v1beta1
kind: ServiceMonitor
metadata:
  name: trading-service-monitor
  namespace: trading-system
spec:
  selector:
    matchLabels:
      app: trading-service
  endpoints:
  - port: management
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

### Observability Configuration

```yaml
# telemetry.yaml
apiVersion: telemetry.istio.io/v1alpha1
kind: Telemetry
metadata:
  name: trading-service-telemetry
  namespace: trading-system
spec:
  metrics:
  - providers:
    - name: prometheus
  - overrides:
    - match:
        metric: ALL_METRICS
      tagOverrides:
        request_id:
          operation: UPSERT
          value: "%{REQUEST_ID}"
        trader_id:
          operation: UPSERT
          value: "%{REQ(X-Trader-ID)}"
  tracing:
  - providers:
    - name: jaeger
  accessLogging:
  - providers:
    - name: otel

---
# request-authentication.yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: trading-service-jwt
  namespace: trading-system
spec:
  selector:
    matchLabels:
      app: trading-service
  jwtRules:
  - issuer: "https://auth.trading.example.com"
    jwksUri: "https://auth.trading.example.com/.well-known/jwks.json"
    audiences:
    - "trading-api"
    forwardOriginalToken: true

---
# envoy-filter.yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: trading-service-lua
  namespace: trading-system
spec:
  workloadSelector:
    labels:
      app: trading-service
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
        name: envoy.filters.http.lua
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.http.lua.v3.Lua
          inline_code: |
            function envoy_on_request(request_handle)
              -- Add correlation ID if not present
              local correlation_id = request_handle:headers():get("x-correlation-id")
              if not correlation_id then
                correlation_id = request_handle:headers():get("x-request-id")
                if correlation_id then
                  request_handle:headers():add("x-correlation-id", correlation_id)
                end
              end
              
              -- Add timestamp header
              request_handle:headers():add("x-request-timestamp", 
                                         os.date("!%Y-%m-%dT%H:%M:%SZ"))
            end
            
            function envoy_on_response(response_handle)
              -- Add response time header
              response_handle:headers():add("x-response-time", 
                                          os.clock() * 1000)
            end
```

## 5. Advanced Kubernetes Operators

### Custom Resource Definition

```java
// Kubernetes operator implementation in Java
@Component
public class TradingServiceOperator {
    
    private final KubernetesClient kubernetesClient;
    private final Logger logger = LoggerFactory.getLogger(TradingServiceOperator.class);
    
    public TradingServiceOperator(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        startWatching();
    }
    
    private void startWatching() {
        // Watch for TradingService custom resources
        kubernetesClient.customResources(TradingService.class, TradingServiceList.class)
                .inAnyNamespace()
                .watch(new Watcher<TradingService>() {
                    @Override
                    public void eventReceived(Action action, TradingService resource) {
                        handleTradingServiceEvent(action, resource);
                    }
                    
                    @Override
                    public void onClose(WatcherException cause) {
                        logger.error("TradingService watcher closed", cause);
                        // Restart watching
                        startWatching();
                    }
                });
    }
    
    private void handleTradingServiceEvent(Watcher.Action action, TradingService tradingService) {
        logger.info("TradingService event: {} for {}", action, tradingService.getMetadata().getName());
        
        switch (action) {
            case ADDED -> handleTradingServiceAdded(tradingService);
            case MODIFIED -> handleTradingServiceModified(tradingService);
            case DELETED -> handleTradingServiceDeleted(tradingService);
            default -> logger.warn("Unknown action: {}", action);
        }
    }
    
    private void handleTradingServiceAdded(TradingService tradingService) {
        try {
            // Create deployment
            Deployment deployment = createDeployment(tradingService);
            kubernetesClient.apps().deployments()
                    .inNamespace(tradingService.getMetadata().getNamespace())
                    .createOrReplace(deployment);
            
            // Create service
            Service service = createService(tradingService);
            kubernetesClient.services()
                    .inNamespace(tradingService.getMetadata().getNamespace())
                    .createOrReplace(service);
            
            // Create HPA
            HorizontalPodAutoscaler hpa = createHPA(tradingService);
            kubernetesClient.autoscaling().v2().horizontalPodAutoscalers()
                    .inNamespace(tradingService.getMetadata().getNamespace())
                    .createOrReplace(hpa);
            
            // Update status
            updateTradingServiceStatus(tradingService, "Ready", "TradingService deployed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to handle TradingService added event", e);
            updateTradingServiceStatus(tradingService, "Failed", e.getMessage());
        }
    }
    
    private void handleTradingServiceModified(TradingService tradingService) {
        // Handle updates to TradingService spec
        handleTradingServiceAdded(tradingService); // For simplicity, treat as add
    }
    
    private void handleTradingServiceDeleted(TradingService tradingService) {
        String namespace = tradingService.getMetadata().getNamespace();
        String name = tradingService.getMetadata().getName();
        
        // Delete related resources
        kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .delete();
        
        kubernetesClient.services()
                .inNamespace(namespace)
                .withName(name)
                .delete();
        
        kubernetesClient.autoscaling().v2().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .withName(name + "-hpa")
                .delete();
        
        logger.info("Cleaned up resources for TradingService: {}", name);
    }
    
    private Deployment createDeployment(TradingService tradingService) {
        return new DeploymentBuilder()
                .withNewMetadata()
                    .withName(tradingService.getMetadata().getName())
                    .withNamespace(tradingService.getMetadata().getNamespace())
                    .addToLabels("app", tradingService.getMetadata().getName())
                    .addToLabels("managed-by", "trading-service-operator")
                .endMetadata()
                .withNewSpec()
                    .withReplicas(tradingService.getSpec().getReplicas())
                    .withNewSelector()
                        .addToMatchLabels("app", tradingService.getMetadata().getName())
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", tradingService.getMetadata().getName())
                            .addToLabels("version", tradingService.getSpec().getVersion())
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("trading-service")
                                .withImage(tradingService.getSpec().getImage())
                                .addNewPort()
                                    .withName("http")
                                    .withContainerPort(8080)
                                .endPort()
                                .withNewResources()
                                    .addToRequests("memory", new Quantity("512Mi"))
                                    .addToRequests("cpu", new Quantity("250m"))
                                    .addToLimits("memory", new Quantity("1Gi"))
                                    .addToLimits("cpu", new Quantity("500m"))
                                .endResources()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }
    
    private Service createService(TradingService tradingService) {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(tradingService.getMetadata().getName())
                    .withNamespace(tradingService.getMetadata().getNamespace())
                    .addToLabels("app", tradingService.getMetadata().getName())
                .endMetadata()
                .withNewSpec()
                    .addToSelector("app", tradingService.getMetadata().getName())
                    .addNewPort()
                        .withName("http")
                        .withPort(8080)
                        .withTargetPort(new IntOrString(8080))
                    .endPort()
                .endSpec()
                .build();
    }
    
    private HorizontalPodAutoscaler createHPA(TradingService tradingService) {
        return new HorizontalPodAutoscalerBuilder()
                .withNewMetadata()
                    .withName(tradingService.getMetadata().getName() + "-hpa")
                    .withNamespace(tradingService.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                    .withNewScaleTargetRef()
                        .withApiVersion("apps/v1")
                        .withKind("Deployment")
                        .withName(tradingService.getMetadata().getName())
                    .endScaleTargetRef()
                    .withMinReplicas(tradingService.getSpec().getMinReplicas())
                    .withMaxReplicas(tradingService.getSpec().getMaxReplicas())
                    .addNewMetric()
                        .withType("Resource")
                        .withNewResource()
                            .withName("cpu")
                            .withNewTarget()
                                .withType("Utilization")
                                .withAverageUtilization(70)
                            .endTarget()
                        .endResource()
                    .endMetric()
                .endSpec()
                .build();
    }
    
    private void updateTradingServiceStatus(TradingService tradingService, String phase, String message) {
        try {
            TradingServiceStatus status = new TradingServiceStatus();
            status.setPhase(phase);
            status.setMessage(message);
            status.setLastUpdated(OffsetDateTime.now().toString());
            
            tradingService.setStatus(status);
            
            kubernetesClient.customResources(TradingService.class, TradingServiceList.class)
                    .inNamespace(tradingService.getMetadata().getNamespace())
                    .withName(tradingService.getMetadata().getName())
                    .updateStatus(tradingService);
                    
        } catch (Exception e) {
            logger.error("Failed to update TradingService status", e);
        }
    }
}

// Custom Resource classes
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec", "status"})
public class TradingService extends CustomResource<TradingServiceSpec, TradingServiceStatus> {
    
    @Override
    protected TradingServiceSpec initSpec() {
        return new TradingServiceSpec();
    }
    
    @Override
    protected TradingServiceStatus initStatus() {
        return new TradingServiceStatus();
    }
}

public class TradingServiceSpec {
    private String image;
    private String version;
    private Integer replicas;
    private Integer minReplicas;
    private Integer maxReplicas;
    private Map<String, String> environment;
    
    // Getters and setters
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public Integer getReplicas() { return replicas; }
    public void setReplicas(Integer replicas) { this.replicas = replicas; }
    
    public Integer getMinReplicas() { return minReplicas; }
    public void setMinReplicas(Integer minReplicas) { this.minReplicas = minReplicas; }
    
    public Integer getMaxReplicas() { return maxReplicas; }
    public void setMaxReplicas(Integer maxReplicas) { this.maxReplicas = maxReplicas; }
    
    public Map<String, String> getEnvironment() { return environment; }
    public void setEnvironment(Map<String, String> environment) { this.environment = environment; }
}

public class TradingServiceStatus {
    private String phase;
    private String message;
    private String lastUpdated;
    
    // Getters and setters
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}

public class TradingServiceList extends CustomResourceList<TradingService> {
}
```

### Custom Resource Definition YAML

```yaml
# trading-service-crd.yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: tradingservices.trading.example.com
spec:
  group: trading.example.com
  versions:
  - name: v1
    served: true
    storage: true
    schema:
      openAPIV3Schema:
        type: object
        properties:
          spec:
            type: object
            properties:
              image:
                type: string
                description: "Container image for the trading service"
              version:
                type: string
                description: "Version of the trading service"
              replicas:
                type: integer
                minimum: 1
                maximum: 100
                description: "Number of replicas"
              minReplicas:
                type: integer
                minimum: 1
                description: "Minimum number of replicas for HPA"
              maxReplicas:
                type: integer
                minimum: 1
                description: "Maximum number of replicas for HPA"
              environment:
                type: object
                additionalProperties:
                  type: string
                description: "Environment variables"
            required:
            - image
            - version
            - replicas
          status:
            type: object
            properties:
              phase:
                type: string
                enum: ["Pending", "Ready", "Failed"]
              message:
                type: string
              lastUpdated:
                type: string
                format: date-time
    subresources:
      status: {}
  scope: Namespaced
  names:
    plural: tradingservices
    singular: tradingservice
    kind: TradingService
    shortNames:
    - ts
```

## Key Takeaways

1. **Cloud-Native Design**: Implement twelve-factor app principles with proper configuration management
2. **Container Optimization**: Use multi-stage builds and optimize for cloud environments
3. **Kubernetes Orchestration**: Leverage advanced Kubernetes features for scalability and resilience
4. **Service Mesh**: Implement comprehensive traffic management and security with Istio
5. **Observability**: Build in monitoring, tracing, and logging from the ground up
6. **Custom Operators**: Extend Kubernetes with domain-specific automation
7. **Production Readiness**: Implement proper health checks, resource management, and deployment strategies

This comprehensive cloud-native implementation provides the foundation for building scalable, resilient microservices that can handle enterprise-grade workloads in production Kubernetes environments.