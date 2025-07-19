# Day 79: Docker Compose & Multi-Container Applications

## Learning Objectives
- Master Docker Compose for multi-container orchestration
- Learn service definition and configuration management
- Implement networking and volume management in Compose
- Configure development, testing, and production environments
- Build scalable microservices architectures with Compose

## Introduction to Docker Compose

### What is Docker Compose?
Docker Compose is a tool for defining and running multi-container Docker applications. With Compose, you use a YAML file to configure your application's services, networks, and volumes.

```yaml
# Basic docker-compose.yml structure
version: '3.8'

services:
  web:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - database
    environment:
      - SPRING_PROFILES_ACTIVE=production

  database:
    image: postgres:15
    environment:
      - POSTGRES_DB=appdb
      - POSTGRES_USER=appuser
      - POSTGRES_PASSWORD=apppass
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:

networks:
  default:
    driver: bridge
```

### Docker Compose vs Docker Commands

```bash
# Traditional Docker commands
docker network create app-network
docker volume create postgres_data
docker run -d --name postgres --network app-network \
  -e POSTGRES_DB=appdb postgres:15
docker run -d --name app --network app-network \
  -p 8080:8080 spring-app:latest

# Docker Compose equivalent
docker-compose up -d

# Benefits of Docker Compose:
# - Declarative configuration
# - Environment reproducibility
# - Service dependencies
# - Easy scaling and management
```

## Docker Compose File Structure

### Version and Services

```yaml
# day79-examples/basic-compose/docker-compose.yml
version: '3.8'

services:
  # Web application service
  web:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        BUILD_VERSION: "1.0.0"
    image: myapp:latest
    container_name: web-app
    restart: unless-stopped
    ports:
      - "8080:8080"
      - "8443:8443"
    environment:
      - SPRING_PROFILES_ACTIVE=compose
      - DATABASE_URL=jdbc:postgresql://database:5432/appdb
      - REDIS_URL=redis://redis:6379
    depends_on:
      database:
        condition: service_healthy
      redis:
        condition: service_started
    volumes:
      - ./config:/app/config:ro
      - app_logs:/app/logs
    networks:
      - frontend
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  # Database service
  database:
    image: postgres:15-alpine
    container_name: postgres-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
      POSTGRES_INITDB_ARGS: "--auth-host=scram-sha-256"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d:ro
    networks:
      - backend
    secrets:
      - db_password
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d appdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis cache service
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    restart: unless-stopped
    command: redis-server --appendonly yes --requirepass redispass
    volumes:
      - redis_data:/data
      - ./redis.conf:/usr/local/etc/redis/redis.conf:ro
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

# Named volumes
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
  app_logs:
    driver: local

# Custom networks
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true

# Secrets management
secrets:
  db_password:
    file: ./secrets/db_password.txt
```

### Environment-specific Configurations

```yaml
# day79-examples/environment-configs/docker-compose.yml (Base)
version: '3.8'

services:
  web:
    build: .
    ports:
      - "${WEB_PORT:-8080}:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILE:-development}
      - DATABASE_URL=jdbc:postgresql://database:5432/${DB_NAME:-appdb}
    depends_on:
      - database

  database:
    image: postgres:15
    environment:
      POSTGRES_DB: ${DB_NAME:-appdb}
      POSTGRES_USER: ${DB_USER:-appuser}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-apppass}
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

```yaml
# day79-examples/environment-configs/docker-compose.override.yml (Development)
version: '3.8'

services:
  web:
    build:
      target: development
    volumes:
      - .:/app
      - /app/target
    environment:
      - SPRING_DEVTOOLS_RESTART_ENABLED=true
      - LOGGING_LEVEL_ROOT=DEBUG
    command: ["./mvnw", "spring-boot:run"]

  database:
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_LOG_STATEMENT=all
```

```yaml
# day79-examples/environment-configs/docker-compose.prod.yml (Production)
version: '3.8'

services:
  web:
    build:
      target: production
    restart: always
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - JAVA_OPTS=-Xmx1g -XX:+UseG1GC
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M
          cpus: '0.25'

  database:
    restart: always
    environment:
      - POSTGRES_SHARED_PRELOAD_LIBRARIES=pg_stat_statements
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
```

```bash
# Environment-specific commands
# Development
docker-compose up -d

# Production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Testing
docker-compose -f docker-compose.yml -f docker-compose.test.yml up --abort-on-container-exit
```

## Complete Microservices Example

### E-commerce Application Architecture

```yaml
# day79-examples/ecommerce-microservices/docker-compose.yml
version: '3.8'

services:
  # API Gateway
  api-gateway:
    build: ./api-gateway
    ports:
      - "80:8080"
    environment:
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka
    depends_on:
      - eureka
    networks:
      - frontend
      - backend

  # Service Discovery
  eureka:
    build: ./eureka-server
    ports:
      - "8761:8761"
    environment:
      - EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
      - EUREKA_CLIENT_FETCH_REGISTRY=false
    networks:
      - backend

  # User Service
  user-service:
    build: ./user-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://user-db:5432/userdb
      - SPRING_REDIS_HOST=redis
    depends_on:
      user-db:
        condition: service_healthy
      eureka:
        condition: service_started
    networks:
      - backend
    deploy:
      replicas: 2

  user-db:
    image: postgres:15
    environment:
      POSTGRES_DB: userdb
      POSTGRES_USER: userservice
      POSTGRES_PASSWORD: userpass
    volumes:
      - user_db_data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U userservice -d userdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Product Service
  product-service:
    build: ./product-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://product-db:5432/productdb
      - ELASTICSEARCH_HOST=elasticsearch
    depends_on:
      product-db:
        condition: service_healthy
      elasticsearch:
        condition: service_healthy
    networks:
      - backend
    deploy:
      replicas: 2

  product-db:
    image: postgres:15
    environment:
      POSTGRES_DB: productdb
      POSTGRES_USER: productservice
      POSTGRES_PASSWORD: productpass
    volumes:
      - product_db_data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U productservice -d productdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Order Service
  order-service:
    build: ./order-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://order-db:5432/orderdb
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      order-db:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - backend

  order-db:
    image: postgres:15
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: orderservice
      POSTGRES_PASSWORD: orderpass
    volumes:
      - order_db_data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U orderservice -d orderdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Payment Service
  payment-service:
    build: ./payment-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - kafka
    networks:
      - backend

  # Notification Service
  notification-service:
    build: ./notification-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - SPRING_MAIL_HOST=mailhog
    depends_on:
      - kafka
      - mailhog
    networks:
      - backend

  # Infrastructure Services
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  # Kafka & Zookeeper
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data
    networks:
      - backend

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
    volumes:
      - kafka_data:/var/lib/kafka/data
    networks:
      - backend
    healthcheck:
      test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.8.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Mail Service (for testing)
  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "8025:8025"  # Web interface
    networks:
      - backend

  # Monitoring
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    networks:
      - backend

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources:ro
    networks:
      - backend

volumes:
  user_db_data:
  product_db_data:
  order_db_data:
  redis_data:
  kafka_data:
  zookeeper_data:
  elasticsearch_data:
  prometheus_data:
  grafana_data:

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
```

### Spring Boot Configuration for Compose

```java
// day79-examples/user-service/src/main/java/com/ecommerce/user/config/DockerConfig.java
@Configuration
@Profile("docker")
public class DockerConfiguration {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://user-db:5432/userdb");
        config.setUsername("userservice");
        config.setPassword("userpass");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Connection validation
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("redis", 6379)
        );
        factory.setValidateConnection(true);
        return factory;
    }
    
    @Bean
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true")
    public EurekaClientConfigBean eurekaClientConfig() {
        EurekaClientConfigBean config = new EurekaClientConfigBean();
        config.setServiceUrl(Map.of(
            "defaultZone", "http://eureka:8761/eureka"
        ));
        config.setRegisterWithEureka(true);
        config.setFetchRegistry(true);
        config.setRegistryFetchIntervalSeconds(30);
        config.setInstanceInfoReplicationIntervalSeconds(30);
        return config;
    }
}
```

```properties
# day79-examples/user-service/src/main/resources/application-docker.properties
# Server configuration
server.port=8080
server.servlet.context-path=/api/v1

# Database configuration
spring.datasource.url=jdbc:postgresql://user-db:5432/userdb
spring.datasource.username=userservice
spring.datasource.password=userpass
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Redis configuration
spring.redis.host=redis
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=0

# Eureka configuration
eureka.client.service-url.defaultZone=http://eureka:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true
eureka.instance.hostname=user-service

# Kafka configuration
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.consumer.group-id=user-service
spring.kafka.consumer.auto-offset-reset=earliest

# Actuator configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true

# Logging configuration
logging.level.com.ecommerce.user=INFO
logging.level.org.springframework.web=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

## Docker Compose Commands and Workflow

### Basic Commands

```bash
# Start services
docker-compose up
docker-compose up -d  # Detached mode
docker-compose up --build  # Rebuild images
docker-compose up service1 service2  # Start specific services

# Stop services
docker-compose down
docker-compose down --volumes  # Remove volumes
docker-compose down --rmi all  # Remove images
docker-compose stop service1  # Stop specific service

# View services
docker-compose ps
docker-compose ps --services  # List service names
docker-compose top  # Show running processes

# Logs
docker-compose logs
docker-compose logs -f  # Follow logs
docker-compose logs service1  # Specific service logs
docker-compose logs --tail=100 service1

# Execute commands
docker-compose exec service1 bash
docker-compose exec web curl http://localhost:8080/health
docker-compose run --rm web bash  # One-time command

# Scaling services
docker-compose up --scale web=3
docker-compose up --scale worker=5

# Configuration validation
docker-compose config
docker-compose config --services
docker-compose config --volumes
```

### Development Workflow

```bash
# Development setup script
#!/bin/bash
# day79-examples/scripts/dev-setup.sh

set -e

echo "Starting development environment..."

# Pull latest images
docker-compose pull

# Build services
docker-compose build --no-cache

# Start infrastructure services first
docker-compose up -d postgres redis

# Wait for databases to be ready
echo "Waiting for databases..."
docker-compose exec -T postgres pg_isready -U appuser -d appdb
docker-compose exec -T redis redis-cli ping

# Start application services
docker-compose up -d web

# Show status
docker-compose ps

# Follow logs
docker-compose logs -f web
```

### Production Deployment

```bash
# Production deployment script
#!/bin/bash
# day79-examples/scripts/prod-deploy.sh

set -e

echo "Deploying to production..."

# Use production compose file
export COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml

# Pull latest images
docker-compose pull

# Start services with rolling update
docker-compose up -d --no-deps --scale web=3 web

# Wait for health checks
echo "Waiting for health checks..."
for i in {1..30}; do
  if docker-compose exec -T web curl -f http://localhost:8080/actuator/health; then
    echo "Health check passed"
    break
  fi
  echo "Waiting for health check... ($i/30)"
  sleep 10
done

# Update other services
docker-compose up -d

echo "Deployment completed"
```

## Advanced Compose Features

### Service Dependencies and Health Checks

```yaml
# day79-examples/advanced-features/docker-compose.yml
version: '3.8'

services:
  web:
    build: .
    depends_on:
      database:
        condition: service_healthy
      redis:
        condition: service_started
      migration:
        condition: service_completed_successfully
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  database:
    image: postgres:15
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: apppass
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d appdb"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  migration:
    build:
      context: .
      dockerfile: Dockerfile.migration
    depends_on:
      database:
        condition: service_healthy
    environment:
      DATABASE_URL: jdbc:postgresql://database:5432/appdb
    volumes:
      - ./migrations:/migrations:ro

volumes:
  postgres_data:
```

### Configuration Management with Secrets

```yaml
# day79-examples/secrets-config/docker-compose.yml
version: '3.8'

services:
  web:
    build: .
    environment:
      - DATABASE_URL=jdbc:postgresql://database:5432/appdb
      - DATABASE_USER_FILE=/run/secrets/db_user
      - DATABASE_PASSWORD_FILE=/run/secrets/db_password
      - JWT_SECRET_FILE=/run/secrets/jwt_secret
    secrets:
      - db_user
      - db_password
      - jwt_secret
    configs:
      - source: app_config
        target: /app/config/application.properties
        mode: 0444

  database:
    image: postgres:15
    environment:
      - POSTGRES_DB=appdb
      - POSTGRES_USER_FILE=/run/secrets/db_user
      - POSTGRES_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - db_user
      - db_password

secrets:
  db_user:
    file: ./secrets/db_user.txt
  db_password:
    file: ./secrets/db_password.txt
  jwt_secret:
    external: true
    name: jwt_secret_v1

configs:
  app_config:
    file: ./config/application.properties
```

```java
// day79-examples/secrets-config/SecretConfigurationLoader.java
@Component
public class SecretConfigurationLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        // Load secrets from files
        loadSecretFromFile(environment, "DATABASE_USER_FILE", "spring.datasource.username");
        loadSecretFromFile(environment, "DATABASE_PASSWORD_FILE", "spring.datasource.password");
        loadSecretFromFile(environment, "JWT_SECRET_FILE", "app.jwt.secret");
    }
    
    private void loadSecretFromFile(ConfigurableEnvironment environment, 
                                  String fileEnvVar, String propertyName) {
        String secretFile = environment.getProperty(fileEnvVar);
        if (secretFile != null) {
            try {
                String secret = Files.readString(Paths.get(secretFile)).trim();
                System.setProperty(propertyName, secret);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load secret from " + secretFile, e);
            }
        }
    }
}
```

### Load Balancing and Scaling

```yaml
# day79-examples/load-balancing/docker-compose.yml
version: '3.8'

services:
  # Load Balancer
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - web
    networks:
      - frontend

  # Scalable web service
  web:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DATABASE_URL=jdbc:postgresql://database:5432/appdb
    depends_on:
      database:
        condition: service_healthy
    networks:
      - frontend
      - backend
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M
          cpus: '0.25'
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
        window: 120s

  database:
    image: postgres:15
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: apppass
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d appdb"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true
```

```nginx
# day79-examples/load-balancing/nginx/nginx.conf
upstream web_servers {
    least_conn;
    server web_1:8080 max_fails=3 fail_timeout=30s;
    server web_2:8080 max_fails=3 fail_timeout=30s;
    server web_3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name localhost;

    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }

    # Main application
    location / {
        proxy_pass http://web_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Health check configuration
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 10s;
    }
}
```

## Testing with Docker Compose

### Integration Testing Setup

```yaml
# day79-examples/testing/docker-compose.test.yml
version: '3.8'

services:
  # Test database
  test-db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: testuser
      POSTGRES_PASSWORD: testpass
    volumes:
      - ./test-data:/docker-entrypoint-initdb.d:ro
    tmpfs:
      - /var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U testuser -d testdb"]
      interval: 5s
      timeout: 3s
      retries: 5

  # Test Redis
  test-redis:
    image: redis:7-alpine
    tmpfs:
      - /data

  # Application under test
  app-under-test:
    build:
      context: .
      target: test
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - DATABASE_URL=jdbc:postgresql://test-db:5432/testdb
      - REDIS_URL=redis://test-redis:6379
    depends_on:
      test-db:
        condition: service_healthy
      test-redis:
        condition: service_started
    volumes:
      - ./target/test-results:/app/test-results
    command: ["./mvnw", "test"]

  # Integration test runner
  integration-tests:
    build:
      context: .
      dockerfile: Dockerfile.test
    environment:
      - APP_URL=http://app-under-test:8080
      - DATABASE_URL=jdbc:postgresql://test-db:5432/testdb
    depends_on:
      app-under-test:
        condition: service_healthy
    volumes:
      - ./target/test-results:/test-results
    command: ["./mvnw", "verify", "-Dtest.groups=integration"]
```

```dockerfile
# day79-examples/testing/Dockerfile.test
FROM maven:3.8.6-openjdk-17-slim

WORKDIR /app

# Copy test dependencies
COPY pom.xml .
RUN mvn dependency:resolve-sources

# Copy test source
COPY src/test ./src/test
COPY src/main ./src/main

# Install test tools
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Test execution script
COPY test-scripts/run-integration-tests.sh /app/run-tests.sh
RUN chmod +x /app/run-tests.sh

ENTRYPOINT ["/app/run-tests.sh"]
```

### Test Execution Script

```bash
#!/bin/bash
# day79-examples/testing/test-scripts/run-integration-tests.sh

set -e

echo "Starting integration tests..."

# Wait for application to be ready
echo "Waiting for application..."
for i in {1..30}; do
  if curl -f http://app-under-test:8080/actuator/health; then
    echo "Application is ready"
    break
  fi
  echo "Waiting for application... ($i/30)"
  sleep 5
done

# Run integration tests
echo "Running integration tests..."
mvn verify -Dtest.groups=integration -Dapp.url=http://app-under-test:8080

# Generate test reports
echo "Generating test reports..."
mvn surefire-report:report-only
mvn surefire-report:failsafe-report-only

# Copy results
cp -r target/site /test-results/
cp target/surefire-reports/* /test-results/ 2>/dev/null || true
cp target/failsafe-reports/* /test-results/ 2>/dev/null || true

echo "Integration tests completed"
```

```java
// day79-examples/testing/IntegrationTestConfiguration.java
@TestConfiguration
@Profile("test")
public class IntegrationTestConfiguration {
    
    @Bean
    @Primary
    public DataSource testDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://test-db:5432/testdb");
        config.setUsername("testuser");
        config.setPassword("testpass");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }
    
    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("test-redis", 6379)
        );
    }
    
    @Bean
    public TestRestTemplate testRestTemplate() {
        return new TestRestTemplate();
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
public class ApplicationIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Value("${app.url:http://localhost:8080}")
    private String baseUrl;
    
    @Test
    @Order(1)
    public void healthCheckShouldReturnOk() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", Map.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
    
    @Test
    @Order(2)
    public void databaseConnectionShouldWork() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health/db", Map.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
```

## Environment Configuration

### Environment Variables and .env Files

```bash
# day79-examples/environment/.env
# Application Configuration
SPRING_PROFILES_ACTIVE=production
APP_VERSION=1.0.0
LOG_LEVEL=INFO

# Database Configuration
DB_HOST=database
DB_PORT=5432
DB_NAME=appdb
DB_USER=appuser
DB_PASSWORD=securepassword

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=redispassword

# External Services
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ELASTICSEARCH_HOST=elasticsearch:9200

# Security
JWT_SECRET=your-jwt-secret-key
ENCRYPTION_KEY=your-encryption-key

# Monitoring
METRICS_ENABLED=true
TRACING_ENABLED=true

# Resource Limits
JAVA_OPTS=-Xmx1g -XX:+UseG1GC
MAX_CONNECTIONS=100
```

```yaml
# day79-examples/environment/docker-compose.yml
version: '3.8'

services:
  web:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
      - APP_VERSION=${APP_VERSION}
      - DATABASE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
      - DATABASE_USER=${DB_USER}
      - DATABASE_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=${REDIS_HOST}
      - REDIS_PORT=${REDIS_PORT}
      - KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}
      - JAVA_OPTS=${JAVA_OPTS}
    env_file:
      - .env
      - .env.local  # Override for local development
```

### Multi-Environment Configuration

```bash
# day79-examples/multi-env/scripts/deploy.sh
#!/bin/bash

ENVIRONMENT=${1:-development}

case $ENVIRONMENT in
  development)
    COMPOSE_FILES="-f docker-compose.yml -f docker-compose.dev.yml"
    ENV_FILE=".env.development"
    ;;
  staging)
    COMPOSE_FILES="-f docker-compose.yml -f docker-compose.staging.yml"
    ENV_FILE=".env.staging"
    ;;
  production)
    COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"
    ENV_FILE=".env.production"
    ;;
  *)
    echo "Unknown environment: $ENVIRONMENT"
    exit 1
    ;;
esac

echo "Deploying to $ENVIRONMENT environment..."

# Load environment variables
export $(grep -v '^#' $ENV_FILE | xargs)

# Deploy services
docker-compose $COMPOSE_FILES up -d

echo "Deployment to $ENVIRONMENT completed"
```

## Monitoring and Observability

### Comprehensive Monitoring Stack

```yaml
# day79-examples/monitoring/docker-compose.monitoring.yml
version: '3.8'

services:
  # Application
  web:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
      - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
    networks:
      - app-network
      - monitoring

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./monitoring/rules:/etc/prometheus/rules:ro
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
    networks:
      - monitoring

  # Grafana
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
      - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards:ro
    networks:
      - monitoring

  # Alertmanager
  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    networks:
      - monitoring

  # Node Exporter
  node-exporter:
    image: prom/node-exporter:latest
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.ignored-mount-points=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - monitoring

  # cAdvisor
  cadvisor:
    image: gcr.io/cadvisor/cadvisor:latest
    ports:
      - "8080:8080"
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
      - /dev/disk/:/dev/disk:ro
    privileged: true
    devices:
      - /dev/kmsg
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:

networks:
  app-network:
    external: true
  monitoring:
    driver: bridge
```

```yaml
# day79-examples/monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['web:8080']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
```

## Summary

### Key Concepts Covered
- **Docker Compose Fundamentals**: Service definition, networking, and volumes
- **Multi-Container Orchestration**: Dependencies, health checks, and scaling
- **Environment Management**: Configuration for development, testing, and production
- **Microservices Architecture**: Complete e-commerce example with service discovery
- **Testing Strategies**: Integration testing with containerized environments
- **Monitoring and Observability**: Prometheus, Grafana, and alerting setup

### Docker Compose Commands Cheat Sheet

```bash
# Basic operations
docker-compose up -d
docker-compose down --volumes
docker-compose ps
docker-compose logs -f service

# Building and scaling
docker-compose build --no-cache
docker-compose up --scale web=3
docker-compose restart service

# Configuration
docker-compose config
docker-compose -f file1.yml -f file2.yml up
docker-compose --env-file .env.prod up

# Debugging
docker-compose exec service bash
docker-compose run --rm service command
docker-compose top
```

### Best Practices
- **Environment Separation**: Use different compose files for different environments
- **Health Checks**: Implement proper health checks for service dependencies
- **Resource Limits**: Set appropriate memory and CPU limits
- **Security**: Use secrets management and non-root users
- **Monitoring**: Include observability from the start
- **Testing**: Automate integration testing with compose

### Next Steps
- Day 80: Kubernetes Fundamentals - Container Orchestration
- Learn production-grade container orchestration
- Implement service mesh and advanced networking
- Master cloud-native deployment patterns

---

## Exercises

### Exercise 1: Multi-Service Application
Create a complete multi-service application with API gateway, database, cache, and message queue.

### Exercise 2: Environment Configuration
Implement proper environment separation with different compose files for dev, staging, and production.

### Exercise 3: Integration Testing
Set up automated integration testing pipeline using Docker Compose.

### Exercise 4: Monitoring Setup
Implement comprehensive monitoring with Prometheus, Grafana, and alerting.

### Exercise 5: Service Scaling
Configure load balancing and horizontal scaling for a web application.