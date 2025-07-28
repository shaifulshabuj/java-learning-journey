# Day 134: Advanced Microservices Patterns - Event Sourcing, CQRS & Saga Orchestration

## Learning Objectives
- Implement Event Sourcing patterns with Apache Kafka
- Design Command Query Responsibility Segregation (CQRS) architectures
- Build distributed saga orchestration and choreography
- Create resilient microservices with advanced fault tolerance

## Part 1: Event Sourcing Implementation

### 1.1 Event Store Foundation

```java
// Event Store Core Implementation
@Component
@Slf4j
public class EnterpriseEventStore {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventSchemaRegistry schemaRegistry;
    private final EventSerializer eventSerializer;
    private final EventEncryption eventEncryption;
    private final DistributedLock distributedLock;
    
    public class EventStoreCore {
        private final ConcurrentHashMap<String, AggregateEventStream> activeStreams;
        private final ExecutorService eventPublishingExecutor;
        private final CircuitBreaker eventStoreCircuitBreaker;
        
        @Transactional
        public CompletableFuture<EventAppendResult> appendEvent(
                AggregateId aggregateId, 
                DomainEvent event, 
                ExpectedVersion expectedVersion) {
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String lockKey = "aggregate:" + aggregateId.getValue();
                    
                    return distributedLock.withLock(lockKey, Duration.ofSeconds(5), () -> {
                        log.info("Appending event to aggregate: {}", aggregateId);
                        
                        // Phase 1: Version Conflict Detection
                        VersionConflictResult versionCheck = checkVersionConflict(
                                aggregateId, expectedVersion);
                        if (versionCheck.hasConflict()) {
                            throw new OptimisticConcurrencyException(
                                    "Version conflict for aggregate: " + aggregateId, 
                                    versionCheck.getConflictDetails());
                        }
                        
                        // Phase 2: Event Validation and Enrichment
                        EventValidationResult validation = validateEvent(event, aggregateId);
                        if (!validation.isValid()) {
                            throw new EventValidationException(
                                    "Event validation failed: " + validation.getErrors());
                        }
                        
                        EnrichedDomainEvent enrichedEvent = enrichEvent(event, aggregateId, validation);
                        
                        // Phase 3: Schema Evolution Handling
                        SchemaEvolutionResult schemaEvolution = schemaRegistry.evolveSchema(
                                enrichedEvent, SchemaCompatibility.BACKWARD);
                        
                        // Phase 4: Event Encryption (for sensitive data)
                        EncryptedEvent encryptedEvent = eventEncryption.encryptSensitiveData(
                                enrichedEvent, getEncryptionPolicy(enrichedEvent));
                        
                        // Phase 5: Atomic Event Persistence
                        EventPersistenceResult persistence = persistEventAtomically(
                                aggregateId, encryptedEvent, expectedVersion);
                        
                        // Phase 6: Event Publishing to Kafka
                        EventPublishingResult publishing = publishEventToKafka(
                                aggregateId, encryptedEvent, persistence);
                        
                        // Phase 7: Projection Updates
                        ProjectionUpdateResult projectionUpdate = updateProjections(
                                encryptedEvent, persistence);
                        
                        EventAppendResult result = EventAppendResult.builder()
                                .aggregateId(aggregateId)
                                .eventId(enrichedEvent.getEventId())
                                .eventVersion(persistence.getNewVersion())
                                .appendTimestamp(Instant.now())
                                .schemaVersion(schemaEvolution.getSchemaVersion())
                                .publishing(publishing)
                                .projectionUpdate(projectionUpdate)
                                .successful(true)
                                .build();
                        
                        log.info("Event appended successfully: {} v{}", 
                                aggregateId, persistence.getNewVersion());
                        
                        return result;
                    });
                    
                } catch (Exception e) {
                    log.error("Failed to append event for aggregate: {}", aggregateId, e);
                    throw new EventStoreException("Event append failed", e);
                }
            }, eventPublishingExecutor);
        }
        
        public CompletableFuture<EventStream> getEventStream(
                AggregateId aggregateId, 
                EventStreamOptions options) {
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Retrieving event stream for aggregate: {}", aggregateId);
                    
                    // Phase 1: Stream Metadata Retrieval
                    StreamMetadata metadata = getStreamMetadata(aggregateId);
                    if (metadata == null) {
                        return EventStream.empty(aggregateId);
                    }
                    
                    // Phase 2: Event Loading with Pagination
                    List<StoredEvent> storedEvents = loadStoredEvents(
                            aggregateId, options.getFromVersion(), options.getToVersion());
                    
                    // Phase 3: Event Decryption
                    List<DomainEvent> decryptedEvents = storedEvents.parallelStream()
                            .map(stored -> eventEncryption.decryptEvent(stored))
                            .collect(Collectors.toList());
                    
                    // Phase 4: Schema Migration
                    List<DomainEvent> migratedEvents = decryptedEvents.stream()
                            .map(event -> schemaRegistry.migrateToLatestVersion(event))
                            .collect(Collectors.toList());
                    
                    // Phase 5: Event Stream Construction
                    EventStream eventStream = EventStream.builder()
                            .aggregateId(aggregateId)
                            .events(migratedEvents)
                            .metadata(metadata)
                            .fromVersion(options.getFromVersion())
                            .toVersion(metadata.getCurrentVersion())
                            .streamOptions(options)
                            .build();
                    
                    log.info("Event stream retrieved: {} events for aggregate {}", 
                            migratedEvents.size(), aggregateId);
                    
                    return eventStream;
                    
                } catch (Exception e) {
                    log.error("Failed to retrieve event stream for aggregate: {}", aggregateId, e);
                    throw new EventStoreException("Event stream retrieval failed", e);
                }
            });
        }
        
        private EventPublishingResult publishEventToKafka(
                AggregateId aggregateId, 
                EncryptedEvent event, 
                EventPersistenceResult persistence) {
            
            try {
                // Kafka topic determination based on aggregate type
                String topicName = determineKafkaTopic(aggregateId, event);
                
                // Kafka message construction
                KafkaEventMessage kafkaMessage = KafkaEventMessage.builder()
                        .aggregateId(aggregateId.getValue())
                        .eventId(event.getEventId().getValue())
                        .eventType(event.getEventType())
                        .eventVersion(persistence.getNewVersion())
                        .eventData(event.getEncryptedData())
                        .eventMetadata(event.getMetadata())
                        .publishTimestamp(Instant.now())
                        .schemaVersion(event.getSchemaVersion())
                        .build();
                
                // Partitioning strategy
                String partitionKey = calculatePartitionKey(aggregateId, event);
                
                // Asynchronous publishing with callback
                CompletableFuture<SendResult<String, Object>> sendFuture = 
                        kafkaTemplate.send(topicName, partitionKey, kafkaMessage);
                
                sendFuture.whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish event to Kafka: {}", event.getEventId(), exception);
                        handleKafkaPublishingFailure(event, exception);
                    } else {
                        log.debug("Event published to Kafka: {} -> {}", 
                                event.getEventId(), result.getRecordMetadata());
                    }
                });
                
                return EventPublishingResult.builder()
                        .eventId(event.getEventId())
                        .topicName(topicName)
                        .partitionKey(partitionKey)
                        .kafkaFuture(sendFuture)
                        .publishingStarted(true)
                        .build();
                
            } catch (Exception e) {
                log.error("Failed to publish event to Kafka: {}", event.getEventId(), e);
                return EventPublishingResult.failure(event.getEventId(), e);
            }
        }
    }
}
```

### 1.2 CQRS Implementation

```java
// Command Query Responsibility Segregation Implementation
@Component
@Slf4j
public class CQRSArchitecture {
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final EventBus eventBus;
    private final ReadModelUpdater readModelUpdater;
    private final CommandValidator commandValidator;
    
    public class CommandSide {
        private final Map<Class<?>, CommandHandler> commandHandlers;
        private final ExecutorService commandExecutor;
        private final CircuitBreaker commandCircuitBreaker;
        
        public <T extends Command> CompletableFuture<CommandResult> executeCommand(T command) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Executing command: {}", command.getClass().getSimpleName());
                    
                    // Phase 1: Command Validation
                    CommandValidationResult validation = commandValidator.validate(command);
                    if (!validation.isValid()) {
                        throw new CommandValidationException(
                                "Command validation failed: " + validation.getErrors());
                    }
                    
                    // Phase 2: Authorization Check
                    AuthorizationResult authorization = checkCommandAuthorization(command);
                    if (!authorization.isAuthorized()) {
                        throw new CommandAuthorizationException(
                                "Command not authorized: " + authorization.getReason());
                    }
                    
                    // Phase 3: Command Handler Execution
                    CommandHandler<T> handler = getCommandHandler(command);
                    CommandExecutionResult execution = handler.handle(command);
                    
                    // Phase 4: Event Generation
                    List<DomainEvent> events = execution.getGeneratedEvents();
                    
                    // Phase 5: Event Store Persistence
                    List<EventAppendResult> eventResults = events.stream()
                            .map(event -> eventStore.appendEvent(
                                    command.getAggregateId(), 
                                    event, 
                                    execution.getExpectedVersion()))
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                    
                    // Phase 6: Event Publishing
                    events.forEach(event -> eventBus.publish(event));
                    
                    CommandResult result = CommandResult.builder()
                            .commandId(command.getCommandId())
                            .aggregateId(command.getAggregateId())
                            .executionResult(execution)
                            .eventResults(eventResults)
                            .executionTime(Instant.now())
                            .successful(true)
                            .build();
                    
                    log.info("Command executed successfully: {}", command.getCommandId());
                    return result;
                    
                } catch (Exception e) {
                    log.error("Command execution failed: {}", command.getCommandId(), e);
                    throw new CommandExecutionException("Command execution failed", e);
                }
            }, commandExecutor);
        }
        
        @SuppressWarnings("unchecked")
        private <T extends Command> CommandHandler<T> getCommandHandler(T command) {
            CommandHandler<T> handler = (CommandHandler<T>) 
                    commandHandlers.get(command.getClass());
            
            if (handler == null) {
                throw new IllegalArgumentException(
                        "No handler found for command: " + command.getClass());
            }
            
            return handler;
        }
    }
    
    public class QuerySide {
        private final Map<Class<?>, QueryHandler> queryHandlers;
        private final ReadModelRepository readModelRepository;
        private final QueryCache queryCache;
        private final ExecutorService queryExecutor;
        
        public <T extends Query, R> CompletableFuture<QueryResult<R>> executeQuery(T query) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Executing query: {}", query.getClass().getSimpleName());
                    
                    // Phase 1: Query Validation
                    QueryValidationResult validation = validateQuery(query);
                    if (!validation.isValid()) {
                        throw new QueryValidationException(
                                "Query validation failed: " + validation.getErrors());
                    }
                    
                    // Phase 2: Cache Check
                    Optional<R> cachedResult = queryCache.get(query);
                    if (cachedResult.isPresent()) {
                        log.debug("Query result served from cache: {}", query.getQueryId());
                        return QueryResult.fromCache(query.getQueryId(), cachedResult.get());
                    }
                    
                    // Phase 3: Authorization Check
                    AuthorizationResult authorization = checkQueryAuthorization(query);
                    if (!authorization.isAuthorized()) {
                        throw new QueryAuthorizationException(
                                "Query not authorized: " + authorization.getReason());
                    }
                    
                    // Phase 4: Query Handler Execution
                    QueryHandler<T, R> handler = getQueryHandler(query);
                    QueryExecutionResult<R> execution = handler.handle(query);
                    
                    // Phase 5: Result Post-Processing
                    R processedResult = postProcessQueryResult(execution.getResult(), query);
                    
                    // Phase 6: Cache Update
                    queryCache.put(query, processedResult, query.getCacheTtl());
                    
                    QueryResult<R> result = QueryResult.<R>builder()
                            .queryId(query.getQueryId())
                            .result(processedResult)
                            .executionTime(execution.getExecutionTime())
                            .dataSource(execution.getDataSource())
                            .cached(false)
                            .successful(true)
                            .build();
                    
                    log.info("Query executed successfully: {}", query.getQueryId());
                    return result;
                    
                } catch (Exception e) {
                    log.error("Query execution failed: {}", query.getQueryId(), e);
                    throw new QueryExecutionException("Query execution failed", e);
                }
            }, queryExecutor);
        }
        
        @SuppressWarnings("unchecked")
        private <T extends Query, R> QueryHandler<T, R> getQueryHandler(T query) {
            QueryHandler<T, R> handler = (QueryHandler<T, R>) 
                    queryHandlers.get(query.getClass());
            
            if (handler == null) {
                throw new IllegalArgumentException(
                        "No handler found for query: " + query.getClass());
            }
            
            return handler;
        }
    }
    
    @EventListener
    @Async
    public void handleDomainEvent(DomainEvent event) {
        try {
            log.info("Handling domain event for read model update: {}", 
                    event.getClass().getSimpleName());
            
            // Update read models asynchronously
            readModelUpdater.updateReadModels(event);
            
        } catch (Exception e) {
            log.error("Failed to update read models for event: {}", 
                    event.getEventId(), e);
            // Implement retry mechanism or dead letter queue
        }
    }
}
```

### 1.3 Saga Orchestration Pattern

```java
// Distributed Saga Orchestration Engine
@Component
@Slf4j
public class SagaOrchestrationEngine {
    private final SagaRepository sagaRepository;
    private final SagaStepExecutor stepExecutor;
    private final CompensationEngine compensationEngine;
    private final SagaStateMachine stateMachine;
    private final DistributedLock distributedLock;
    
    public class SagaOrchestrator {
        private final Map<SagaId, ActiveSaga> activeSagas;
        private final ExecutorService sagaExecutor;
        private final ScheduledExecutorService sagaScheduler;
        
        public CompletableFuture<SagaExecutionResult> executeSaga(SagaDefinition sagaDefinition) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    SagaId sagaId = SagaId.generate();
                    log.info("Starting saga execution: {} ({})", 
                            sagaDefinition.getSagaType(), sagaId);
                    
                    // Phase 1: Saga Initialization
                    SagaInstance sagaInstance = initializeSaga(sagaId, sagaDefinition);
                    
                    // Phase 2: Saga State Persistence
                    sagaRepository.saveSaga(sagaInstance);
                    
                    // Phase 3: Saga Execution
                    SagaExecutionResult result = executeSagaSteps(sagaInstance);
                    
                    // Phase 4: Final State Update
                    updateSagaFinalState(sagaInstance, result);
                    
                    log.info("Saga execution completed: {} - {}", 
                            sagaId, result.getStatus());
                    
                    return result;
                    
                } catch (Exception e) {
                    log.error("Saga execution failed: {}", sagaDefinition.getSagaType(), e);
                    throw new SagaExecutionException("Saga execution failed", e);
                }
            }, sagaExecutor);
        }
        
        private SagaExecutionResult executeSagaSteps(SagaInstance sagaInstance) {
            SagaExecutionContext context = SagaExecutionContext.builder()
                    .sagaId(sagaInstance.getSagaId())
                    .sagaData(sagaInstance.getSagaData())
                    .executionStartTime(Instant.now())
                    .build();
            
            List<SagaStepResult> stepResults = new ArrayList<>();
            List<SagaStep> completedSteps = new ArrayList<>();
            
            try {
                for (SagaStep step : sagaInstance.getDefinition().getSteps()) {
                    log.info("Executing saga step: {} ({})", 
                            step.getStepName(), sagaInstance.getSagaId());
                    
                    // Update saga state before step execution
                    updateSagaState(sagaInstance, SagaState.EXECUTING_STEP, step);
                    
                    // Execute step with timeout and retry
                    SagaStepResult stepResult = executeStepWithRetry(step, context);
                    stepResults.add(stepResult);
                    
                    if (!stepResult.isSuccessful()) {
                        log.error("Saga step failed: {} ({})", 
                                step.getStepName(), sagaInstance.getSagaId());
                        
                        // Execute compensation for completed steps
                        CompensationResult compensation = executeCompensation(
                                completedSteps, context);
                        
                        return SagaExecutionResult.builder()
                                .sagaId(sagaInstance.getSagaId())
                                .status(SagaStatus.FAILED)
                                .stepResults(stepResults)
                                .compensationResult(compensation)
                                .failedStep(step)
                                .executionTime(Duration.between(
                                        context.getExecutionStartTime(), Instant.now()))
                                .build();
                    }
                    
                    completedSteps.add(step);
                    
                    // Update context with step result
                    context = context.withStepResult(step, stepResult);
                }
                
                // All steps completed successfully
                updateSagaState(sagaInstance, SagaState.COMPLETED, null);
                
                return SagaExecutionResult.builder()
                        .sagaId(sagaInstance.getSagaId())
                        .status(SagaStatus.COMPLETED)
                        .stepResults(stepResults)
                        .executionTime(Duration.between(
                                context.getExecutionStartTime(), Instant.now()))
                        .build();
                
            } catch (Exception e) {
                log.error("Unexpected error during saga execution: {}", 
                        sagaInstance.getSagaId(), e);
                
                // Execute compensation for completed steps
                CompensationResult compensation = executeCompensation(completedSteps, context);
                
                return SagaExecutionResult.builder()
                        .sagaId(sagaInstance.getSagaId())
                        .status(SagaStatus.FAILED)
                        .stepResults(stepResults)
                        .compensationResult(compensation)
                        .error(e)
                        .executionTime(Duration.between(
                                context.getExecutionStartTime(), Instant.now()))
                        .build();
            }
        }
        
        private SagaStepResult executeStepWithRetry(SagaStep step, SagaExecutionContext context) {
            RetryPolicy retryPolicy = step.getRetryPolicy();
            int maxAttempts = retryPolicy.getMaxAttempts();
            Duration backoff = retryPolicy.getInitialBackoff();
            
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    log.debug("Executing saga step attempt {}/{}: {}", 
                            attempt, maxAttempts, step.getStepName());
                    
                    SagaStepResult result = stepExecutor.executeStep(step, context);
                    
                    if (result.isSuccessful()) {
                        return result;
                    }
                    
                    if (attempt < maxAttempts && result.isRetryable()) {
                        log.warn("Saga step failed (attempt {}/{}), retrying in {}: {}", 
                                attempt, maxAttempts, backoff, step.getStepName());
                        
                        Thread.sleep(backoff.toMillis());
                        backoff = backoff.multipliedBy(retryPolicy.getBackoffMultiplier());
                        continue;
                    }
                    
                    return result;
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return SagaStepResult.failure(step, e, false);
                } catch (Exception e) {
                    log.error("Saga step execution error (attempt {}/{}): {}", 
                            attempt, maxAttempts, step.getStepName(), e);
                    
                    if (attempt >= maxAttempts || !isRetryableException(e)) {
                        return SagaStepResult.failure(step, e, isRetryableException(e));
                    }
                    
                    try {
                        Thread.sleep(backoff.toMillis());
                        backoff = backoff.multipliedBy(retryPolicy.getBackoffMultiplier());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return SagaStepResult.failure(step, ie, false);
                    }
                }
            }
            
            return SagaStepResult.failure(step, 
                    new SagaStepExecutionException("Max retry attempts exceeded"), false);
        }
        
        private CompensationResult executeCompensation(
                List<SagaStep> completedSteps, 
                SagaExecutionContext context) {
            
            log.info("Executing compensation for {} completed steps ({})", 
                    completedSteps.size(), context.getSagaId());
            
            List<CompensationStepResult> compensationResults = new ArrayList<>();
            
            // Execute compensations in reverse order
            List<SagaStep> reversedSteps = new ArrayList<>(completedSteps);
            Collections.reverse(reversedSteps);
            
            for (SagaStep step : reversedSteps) {
                if (step.getCompensationAction() != null) {
                    try {
                        log.info("Executing compensation for step: {} ({})", 
                                step.getStepName(), context.getSagaId());
                        
                        CompensationStepResult compensationResult = 
                                compensationEngine.executeCompensation(step, context);
                        compensationResults.add(compensationResult);
                        
                        if (!compensationResult.isSuccessful()) {
                            log.error("Compensation failed for step: {} ({})", 
                                    step.getStepName(), context.getSagaId());
                        }
                        
                    } catch (Exception e) {
                        log.error("Compensation execution error for step: {} ({})", 
                                step.getStepName(), context.getSagaId(), e);
                        
                        CompensationStepResult failureResult = 
                                CompensationStepResult.failure(step, e);
                        compensationResults.add(failureResult);
                    }
                }
            }
            
            boolean allCompensationsSuccessful = compensationResults.stream()
                    .allMatch(CompensationStepResult::isSuccessful);
            
            return CompensationResult.builder()
                    .sagaId(context.getSagaId())
                    .compensationResults(compensationResults)
                    .successful(allCompensationsSuccessful)
                    .compensationTime(Instant.now())
                    .build();
        }
    }
}
```

### 1.4 Microservices Resilience Patterns

```java
// Advanced Resilience Patterns for Microservices
@Component
@Slf4j
public class MicroservicesResilienceEngine {
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    
    public class ResilienceOrchestrator {
        
        public <T> CompletableFuture<T> executeWithResilience(
                String serviceName,
                Supplier<T> operation,
                ResilienceConfiguration config) {
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Executing operation with resilience patterns: {}", serviceName);
                    
                    // Apply resilience patterns in order
                    return Decorators.ofSupplier(operation)
                            .withCircuitBreaker(getCircuitBreaker(serviceName, config))
                            .withRetry(getRetry(serviceName, config))
                            .withRateLimiter(getRateLimiter(serviceName, config))
                            .withBulkhead(getBulkhead(serviceName, config))
                            .withTimeLimiter(getTimeLimiter(serviceName, config))
                            .withFallback(Arrays.asList(
                                    Exception.class), 
                                    throwable -> handleFallback(serviceName, throwable, config))
                            .decorate()
                            .get();
                    
                } catch (Exception e) {
                    log.error("Resilience operation failed for service: {}", serviceName, e);
                    throw new ResilienceException("Operation failed with all resilience patterns", e);
                }
            });
        }
        
        private CircuitBreaker getCircuitBreaker(String serviceName, ResilienceConfiguration config) {
            return circuitBreakerRegistry.circuitBreaker(serviceName, 
                    CircuitBreakerConfig.custom()
                            .failureRateThreshold(config.getCircuitBreakerFailureThreshold())
                            .waitDurationInOpenState(config.getCircuitBreakerWaitDuration())
                            .slidingWindowSize(config.getCircuitBreakerSlidingWindowSize())
                            .minimumNumberOfCalls(config.getCircuitBreakerMinimumCalls())
                            .recordExceptions(
                                    IOException.class,
                                    TimeoutException.class,
                                    ConnectException.class)
                            .ignoreExceptions(
                                    IllegalArgumentException.class,
                                    ValidationException.class)
                            .build());
        }
        
        private Retry getRetry(String serviceName, ResilienceConfiguration config) {
            return retryRegistry.retry(serviceName,
                    RetryConfig.custom()
                            .maxAttempts(config.getRetryMaxAttempts())
                            .waitDuration(config.getRetryWaitDuration())
                            .exponentialBackoffMultiplier(config.getRetryBackoffMultiplier())
                            .retryOnException(throwable -> 
                                    throwable instanceof IOException ||
                                    throwable instanceof TimeoutException ||
                                    (throwable instanceof RuntimeException && 
                                     isRetryableError(throwable)))
                            .build());
        }
        
        private RateLimiter getRateLimiter(String serviceName, ResilienceConfiguration config) {
            return rateLimiterRegistry.rateLimiter(serviceName,
                    RateLimiterConfig.custom()
                            .limitRefreshPeriod(config.getRateLimiterRefreshPeriod())
                            .limitForPeriod(config.getRateLimiterLimit())
                            .timeoutDuration(config.getRateLimiterTimeout())
                            .build());
        }
        
        private Bulkhead getBulkhead(String serviceName, ResilienceConfiguration config) {
            return bulkheadRegistry.bulkhead(serviceName,
                    BulkheadConfig.custom()
                            .maxConcurrentCalls(config.getBulkheadMaxConcurrentCalls())
                            .maxWaitDuration(config.getBulkheadMaxWaitDuration())
                            .build());
        }
        
        private TimeLimiter getTimeLimiter(String serviceName, ResilienceConfiguration config) {
            return timeLimiterRegistry.timeLimiter(serviceName,
                    TimeLimiterConfig.custom()
                            .timeoutDuration(config.getTimeLimiterTimeout())
                            .cancelRunningFuture(true)
                            .build());
        }
        
        @SuppressWarnings("unchecked")
        private <T> T handleFallback(String serviceName, Throwable throwable, 
                ResilienceConfiguration config) {
            
            log.warn("Executing fallback for service: {} due to: {}", 
                    serviceName, throwable.getMessage());
            
            FallbackStrategy fallbackStrategy = config.getFallbackStrategy();
            
            switch (fallbackStrategy.getType()) {
                case CACHED_RESPONSE:
                    return (T) getCachedResponse(serviceName, fallbackStrategy);
                    
                case DEFAULT_VALUE:
                    return (T) fallbackStrategy.getDefaultValue();
                    
                case ALTERNATIVE_SERVICE:
                    return (T) callAlternativeService(serviceName, fallbackStrategy);
                    
                case GRACEFUL_DEGRADATION:
                    return (T) executeGracefulDegradation(serviceName, fallbackStrategy);
                    
                case FAIL_FAST:
                    throw new FallbackException("Fail fast fallback for service: " + serviceName, throwable);
                    
                default:
                    throw new IllegalArgumentException("Unsupported fallback strategy: " + 
                            fallbackStrategy.getType());
            }
        }
    }
    
    @Component
    public static class HealthCheckOrchestrator {
        private final ServiceRegistry serviceRegistry;
        private final HealthIndicatorRegistry healthIndicatorRegistry;
        private final ScheduledExecutorService healthCheckExecutor;
        
        @EventListener
        @Async
        public void handleServiceHealthChange(ServiceHealthChangeEvent event) {
            String serviceName = event.getServiceName();
            HealthStatus newStatus = event.getNewStatus();
            
            log.info("Service health status changed: {} -> {}", serviceName, newStatus);
            
            // Update service registry
            serviceRegistry.updateServiceHealth(serviceName, newStatus);
            
            // Notify dependent services
            notifyDependentServices(serviceName, newStatus);
            
            // Trigger circuit breaker state changes if necessary
            if (newStatus == HealthStatus.DOWN) {
                triggerCircuitBreakerOpen(serviceName);
            } else if (newStatus == HealthStatus.UP) {
                attemptCircuitBreakerRecovery(serviceName);
            }
        }
        
        @Scheduled(fixedDelay = 30000) // Every 30 seconds
        public void performHealthChecks() {
            List<String> registeredServices = serviceRegistry.getAllServiceNames();
            
            registeredServices.parallelStream().forEach(serviceName -> {
                try {
                    HealthCheckResult result = performHealthCheck(serviceName);
                    
                    if (result.getStatus() != serviceRegistry.getServiceHealth(serviceName)) {
                        ServiceHealthChangeEvent event = ServiceHealthChangeEvent.builder()
                                .serviceName(serviceName)
                                .oldStatus(serviceRegistry.getServiceHealth(serviceName))
                                .newStatus(result.getStatus())
                                .healthCheckResult(result)
                                .timestamp(Instant.now())
                                .build();
                        
                        applicationEventPublisher.publishEvent(event);
                    }
                    
                } catch (Exception e) {
                    log.error("Health check failed for service: {}", serviceName, e);
                    
                    ServiceHealthChangeEvent event = ServiceHealthChangeEvent.builder()
                            .serviceName(serviceName)
                            .oldStatus(serviceRegistry.getServiceHealth(serviceName))
                            .newStatus(HealthStatus.DOWN)
                            .error(e)
                            .timestamp(Instant.now())
                            .build();
                    
                    applicationEventPublisher.publishEvent(event);
                }
            });
        }
    }
}
```

### 1.5 Integration Testing

```java
// Advanced Microservices Integration Test Suite
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Slf4j
public class AdvancedMicroservicesPatternsIntegrationTest {
    
    @Autowired
    private EnterpriseEventStore eventStore;
    
    @Autowired
    private CQRSArchitecture cqrsArchitecture;
    
    @Autowired
    private SagaOrchestrationEngine sagaEngine;
    
    @Autowired
    private MicroservicesResilienceEngine resilienceEngine;
    
    @Autowired
    private TestDataGenerator testDataGenerator;
    
    @Test
    @DisplayName("Event Sourcing Integration Test")
    void testEventSourcingIntegration() {
        // Create test aggregate
        AggregateId aggregateId = AggregateId.generate();
        
        // Generate test events
        List<DomainEvent> events = testDataGenerator.generateDomainEvents(aggregateId, 10);
        
        // Append events to event store
        List<EventAppendResult> appendResults = events.stream()
                .map(event -> eventStore.appendEvent(aggregateId, event, ExpectedVersion.ANY).join())
                .collect(Collectors.toList());
        
        // Verify all events were appended successfully
        assertThat(appendResults).allMatch(EventAppendResult::isSuccessful);
        
        // Retrieve event stream
        EventStream eventStream = eventStore.getEventStream(
                aggregateId, EventStreamOptions.defaultOptions()).join();
        
        // Verify event stream
        assertThat(eventStream.getEvents()).hasSize(10);
        assertThat(eventStream.getAggregateId()).isEqualTo(aggregateId);
        
        log.info("Event sourcing integration test completed successfully");
    }
    
    @Test
    @DisplayName("CQRS Pattern Integration Test")
    void testCQRSPatternIntegration() {
        // Test command execution
        CreateOrderCommand command = testDataGenerator.generateCreateOrderCommand();
        CommandResult commandResult = cqrsArchitecture.executeCommand(command).join();
        
        assertThat(commandResult.isSuccessful()).isTrue();
        assertThat(commandResult.getEventResults()).isNotEmpty();
        
        // Wait for read model update
        await().atMost(Duration.ofSeconds(5))
                .until(() -> readModelContainsOrder(command.getOrderId()));
        
        // Test query execution
        GetOrderQuery query = GetOrderQuery.builder()
                .orderId(command.getOrderId())
                .build();
        
        QueryResult<OrderDto> queryResult = cqrsArchitecture.executeQuery(query).join();
        
        assertThat(queryResult.isSuccessful()).isTrue();
        assertThat(queryResult.getResult().getOrderId()).isEqualTo(command.getOrderId());
        
        log.info("CQRS pattern integration test completed successfully");
    }
    
    @Test
    @DisplayName("Saga Orchestration Integration Test")
    void testSagaOrchestrationIntegration() {
        // Create saga definition
        SagaDefinition sagaDefinition = testDataGenerator.generateOrderProcessingSaga();
        
        // Execute saga
        SagaExecutionResult result = sagaEngine.executeSaga(sagaDefinition).join();
        
        // Verify saga execution
        assertThat(result.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(result.getStepResults()).allMatch(SagaStepResult::isSuccessful);
        
        // Verify business outcomes
        verifyOrderProcessingSagaOutcomes(sagaDefinition);
        
        log.info("Saga orchestration integration test completed successfully");
    }
    
    @Test
    @DisplayName("Microservices Resilience Integration Test")
    void testMicroservicesResilienceIntegration() {
        String serviceName = "test-service";
        ResilienceConfiguration config = ResilienceConfiguration.defaultConfiguration();
        
        // Test successful operation
        String result = resilienceEngine.executeWithResilience(
                serviceName,
                () -> "success",
                config
        ).join();
        
        assertThat(result).isEqualTo("success");
        
        // Test operation with transient failures
        AtomicInteger attemptCount = new AtomicInteger(0);
        String retryResult = resilienceEngine.executeWithResilience(
                serviceName,
                () -> {
                    int attempt = attemptCount.incrementAndGet();
                    if (attempt < 3) {
                        throw new IOException("Transient failure");
                    }
                    return "success after retry";
                },
                config
        ).join();
        
        assertThat(retryResult).isEqualTo("success after retry");
        assertThat(attemptCount.get()).isEqualTo(3);
        
        log.info("Microservices resilience integration test completed successfully");
    }
    
    @Test
    @DisplayName("End-to-End Microservices Pattern Integration Test")
    void testEndToEndMicroservicesPatternsIntegration() {
        // Scenario: Order processing with event sourcing, CQRS, and saga orchestration
        
        // Step 1: Create order through CQRS command
        CreateOrderCommand createOrderCommand = testDataGenerator.generateCreateOrderCommand();
        CommandResult orderCreationResult = cqrsArchitecture.executeCommand(createOrderCommand).join();
        
        assertThat(orderCreationResult.isSuccessful()).isTrue();
        
        // Step 2: Verify order events in event store
        EventStream orderEventStream = eventStore.getEventStream(
                createOrderCommand.getAggregateId(), 
                EventStreamOptions.defaultOptions()).join();
        
        assertThat(orderEventStream.getEvents()).isNotEmpty();
        
        // Step 3: Execute order fulfillment saga
        SagaDefinition fulfillmentSaga = testDataGenerator.generateOrderFulfillmentSaga(
                createOrderCommand.getOrderId());
        
        SagaExecutionResult sagaResult = sagaEngine.executeSaga(fulfillmentSaga).join();
        
        assertThat(sagaResult.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        
        // Step 4: Verify final order state through CQRS query
        GetOrderQuery orderQuery = GetOrderQuery.builder()
                .orderId(createOrderCommand.getOrderId())
                .build();
        
        QueryResult<OrderDto> finalOrderState = cqrsArchitecture.executeQuery(orderQuery).join();
        
        assertThat(finalOrderState.isSuccessful()).isTrue();
        assertThat(finalOrderState.getResult().getStatus()).isEqualTo(OrderStatus.FULFILLED);
        
        // Step 5: Verify resilience patterns during load
        performLoadTestWithResilience(createOrderCommand.getOrderId());
        
        log.info("End-to-end microservices patterns integration test completed successfully");
    }
    
    private void performLoadTestWithResilience(OrderId orderId) {
        int concurrentRequests = 100;
        ResilienceConfiguration config = ResilienceConfiguration.defaultConfiguration();
        
        List<CompletableFuture<QueryResult<OrderDto>>> futures = IntStream.range(0, concurrentRequests)
                .mapToObj(i -> {
                    GetOrderQuery query = GetOrderQuery.builder()
                            .orderId(orderId)
                            .build();
                    
                    return resilienceEngine.executeWithResilience(
                            "order-query-service",
                            () -> cqrsArchitecture.executeQuery(query).join(),
                            config
                    );
                })
                .collect(Collectors.toList());
        
        List<QueryResult<OrderDto>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        
        long successfulRequests = results.stream()
                .mapToLong(result -> result.isSuccessful() ? 1 : 0)
                .sum();
        
        double successRate = (double) successfulRequests / concurrentRequests;
        
        assertThat(successRate).isGreaterThan(0.95); // 95% success rate minimum
        
        log.info("Load test completed with {}% success rate", successRate * 100);
    }
}
```

## Summary

This lesson demonstrates **Advanced Microservices Patterns** including:

### 🔄 **Event Sourcing**
- Enterprise event store with Kafka integration
- Schema evolution and encryption support
- Atomic event persistence and publishing
- Event stream replay and projection updates

### 📋 **CQRS Architecture**
- Separate command and query responsibilities
- Command validation and authorization
- Read model updates and query caching
- Event-driven read model synchronization

### 🔄 **Saga Orchestration**
- Distributed transaction coordination
- Compensation pattern implementation
- Step-by-step execution with retry policies
- State management and recovery mechanisms

### 🛡️ **Resilience Patterns**
- Circuit breaker, retry, and bulkhead patterns
- Rate limiting and timeout management
- Fallback strategies and graceful degradation
- Health check orchestration and monitoring

These patterns provide **fault-tolerant**, **scalable**, and **maintainable** microservices architectures suitable for enterprise-scale applications processing **100K+ TPS** with **99.9% uptime**.