# Day 128: Enterprise Event-Driven Architecture - Event Sourcing, CQRS & Distributed Sagas

## Learning Objectives
- Master advanced event-driven architecture patterns for enterprise-scale systems
- Implement Event Sourcing with CQRS for high-performance, auditable systems
- Build distributed saga orchestration for complex business transactions
- Create event streaming platforms with guaranteed delivery and ordering
- Develop autonomous event processing with intelligent routing and transformation

## 1. Advanced Event Sourcing Architecture

### Enterprise Event Store Implementation
```java
@Component
@Slf4j
public class EnterpriseEventStore {
    
    @Autowired
    private EventStorageEngine storageEngine;
    
    @Autowired
    private EventSerializationManager serializationManager;
    
    @Autowired
    private EventEncryptionService encryptionService;
    
    public class EventStoreOrchestrator {
        private final Map<String, AggregateEventStream> eventStreams;
        private final EventIndexManager indexManager;
        private final SnapshotManager snapshotManager;
        private final ReplicationManager replicationManager;
        private final ConsistencyManager consistencyManager;
        
        public EventStoreOrchestrator() {
            this.eventStreams = new ConcurrentHashMap<>();
            this.indexManager = new EventIndexManager();
            this.snapshotManager = new SnapshotManager();
            this.replicationManager = new ReplicationManager();
            this.consistencyManager = new ConsistencyManager();
        }
        
        public CompletableFuture<EventAppendResult> appendEvents(AppendEventsCommand command) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String streamId = command.getStreamId();
                    List<DomainEvent> events = command.getEvents();
                    long expectedVersion = command.getExpectedVersion();
                    
                    log.info("Appending {} events to stream: {}", events.size(), streamId);
                    
                    // Validate stream consistency
                    StreamConsistencyResult consistencyResult = 
                        consistencyManager.validateStreamConsistency(streamId, expectedVersion);
                    
                    if (!consistencyResult.isConsistent()) {
                        throw new OptimisticConcurrencyException(
                            "Stream version mismatch. Expected: " + expectedVersion + 
                            ", Actual: " + consistencyResult.getCurrentVersion()
                        );
                    }
                    
                    // Prepare events for storage
                    List<EventRecord> eventRecords = prepareEventRecords(streamId, events);
                    
                    // Encrypt sensitive events
                    List<EventRecord> encryptedRecords = encryptEvents(eventRecords);
                    
                    // Append to primary storage
                    EventAppendOperation appendOp = EventAppendOperation.builder()
                        .streamId(streamId)
                        .eventRecords(encryptedRecords)
                        .expectedVersion(expectedVersion)
                        .metadata(EventMetadata.builder()
                            .correlationId(command.getCorrelationId())
                            .causationId(command.getCausationId())
                            .userId(command.getUserId())
                            .timestamp(Instant.now())
                            .build())
                        .build();
                    
                    EventStorageResult storageResult = storageEngine.appendEvents(appendOp);
                    
                    // Update indexes
                    indexManager.updateIndexes(streamId, encryptedRecords);
                    
                    // Replicate to secondary stores
                    CompletableFuture<ReplicationResult> replicationFuture = 
                        replicationManager.replicateEvents(appendOp);
                    
                    // Create snapshot if needed
                    if (shouldCreateSnapshot(streamId, storageResult.getNewVersion())) {
                        CompletableFuture.runAsync(() -> createSnapshot(streamId, storageResult.getNewVersion()));
                    }
                    
                    // Publish domain events for projections
                    publishEventsForProjections(streamId, events, storageResult.getNewVersion());
                    
                    return EventAppendResult.builder()
                        .streamId(streamId)
                        .newVersion(storageResult.getNewVersion())
                        .eventCount(events.size())
                        .storageResult(storageResult)
                        .replicationFuture(replicationFuture)
                        .appendedAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Event append failed: {}", command.getStreamId(), e);
                    throw new EventAppendException("Failed to append events", e);
                }
            });
        }
        
        public CompletableFuture<EventStreamResult> loadEventStream(LoadEventStreamQuery query) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String streamId = query.getStreamId();
                    long fromVersion = query.getFromVersion();
                    long toVersion = query.getToVersion();
                    
                    log.info("Loading event stream: {} from version {} to {}", 
                        streamId, fromVersion, toVersion);
                    
                    // Check if snapshot can be used
                    Optional<AggregateSnapshot> snapshot = 
                        snapshotManager.getLatestSnapshot(streamId, fromVersion);
                    
                    long effectiveFromVersion = snapshot
                        .map(s -> s.getVersion() + 1)
                        .orElse(fromVersion);
                    
                    // Load events from storage
                    EventLoadOperation loadOp = EventLoadOperation.builder()
                        .streamId(streamId)
                        .fromVersion(effectiveFromVersion)
                        .toVersion(toVersion)
                        .includeDeleted(query.isIncludeDeleted())
                        .batchSize(query.getBatchSize())
                        .build();
                    
                    List<EventRecord> eventRecords = storageEngine.loadEvents(loadOp);
                    
                    // Decrypt events
                    List<EventRecord> decryptedRecords = decryptEvents(eventRecords);
                    
                    // Deserialize to domain events
                    List<DomainEvent> domainEvents = deserializeEvents(decryptedRecords);
                    
                    // Apply performance optimizations
                    if (query.isOptimizeForReplay()) {
                        domainEvents = optimizeEventsForReplay(domainEvents);
                    }
                    
                    return EventStreamResult.builder()
                        .streamId(streamId)
                        .fromVersion(fromVersion)
                        .toVersion(toVersion)
                        .snapshot(snapshot)
                        .events(domainEvents)
                        .totalEvents(eventRecords.size())
                        .loadedAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Event stream load failed: {}", query.getStreamId(), e);
                    throw new EventLoadException("Failed to load event stream", e);
                }
            });
        }
        
        private List<EventRecord> prepareEventRecords(String streamId, List<DomainEvent> events) {
            List<EventRecord> records = new ArrayList<>();
            
            for (int i = 0; i < events.size(); i++) {
                DomainEvent event = events.get(i);
                
                // Serialize event data
                SerializedEventData serializedData = serializationManager.serialize(event);
                
                // Create event record
                EventRecord record = EventRecord.builder()
                    .eventId(UUID.randomUUID().toString())
                    .streamId(streamId)
                    .eventType(event.getClass().getSimpleName())
                    .eventData(serializedData.getData())
                    .eventMetadata(serializedData.getMetadata())
                    .eventVersion(determineEventVersion(event))
                    .timestamp(event.getTimestamp())
                    .correlationId(event.getCorrelationId())
                    .causationId(event.getCausationId())
                    .userId(event.getUserId())
                    .build();
                
                records.add(record);
            }
            
            return records;
        }
        
        private List<EventRecord> encryptEvents(List<EventRecord> eventRecords) {
            return eventRecords.stream()
                .map(record -> {
                    if (requiresEncryption(record)) {
                        EncryptedEventData encryptedData = encryptionService.encrypt(
                            record.getEventData(), record.getStreamId()
                        );
                        
                        return record.toBuilder()
                            .eventData(encryptedData.getData())
                            .encryptionMetadata(encryptedData.getMetadata())
                            .encrypted(true)
                            .build();
                    }
                    return record;
                })
                .collect(Collectors.toList());
        }
        
        private boolean shouldCreateSnapshot(String streamId, long currentVersion) {
            // Create snapshot every 100 events
            if (currentVersion % 100 == 0) {
                return true;
            }
            
            // Or if aggregate is getting large
            AggregateEventStream stream = eventStreams.get(streamId);
            if (stream != null && stream.getEventCount() > 1000) {
                return true;
            }
            
            return false;
        }
        
        private void createSnapshot(String streamId, long version) {
            try {
                log.info("Creating snapshot for stream: {} at version: {}", streamId, version);
                
                // Load aggregate from events
                LoadEventStreamQuery query = LoadEventStreamQuery.builder()
                    .streamId(streamId)
                    .fromVersion(0)
                    .toVersion(version)
                    .optimizeForReplay(true)
                    .build();
                
                EventStreamResult streamResult = loadEventStream(query).get();
                
                // Rebuild aggregate state
                AggregateRoot aggregate = rebuildAggregateFromEvents(streamId, streamResult.getEvents());
                
                // Serialize aggregate state
                SerializedAggregateData serializedState = serializationManager.serializeAggregate(aggregate);
                
                // Create snapshot
                AggregateSnapshot snapshot = AggregateSnapshot.builder()
                    .streamId(streamId)
                    .aggregateType(aggregate.getClass().getSimpleName())
                    .version(version)
                    .snapshotData(serializedState.getData())
                    .snapshotMetadata(serializedState.getMetadata())
                    .createdAt(Instant.now())
                    .build();
                
                // Store snapshot
                snapshotManager.storeSnapshot(snapshot);
                
                log.info("Snapshot created successfully: {} at version: {}", streamId, version);
                
            } catch (Exception e) {
                log.error("Snapshot creation failed: {}", streamId, e);
            }
        }
    }
    
    @Component
    public static class CQRSProjectionEngine {
        
        @Autowired
        private ProjectionStorageManager projectionStorage;
        
        @Autowired
        private EventSubscriptionManager subscriptionManager;
        
        @Autowired
        private ProjectionCheckpointManager checkpointManager;
        
        public class ProjectionOrchestrator {
            private final Map<String, ProjectionDefinition> projections;
            private final ProjectionStateManager stateManager;
            private final ProjectionErrorHandler errorHandler;
            private final ProjectionMetricsCollector metricsCollector;
            
            public ProjectionOrchestrator() {
                this.projections = new ConcurrentHashMap<>();
                this.stateManager = new ProjectionStateManager();
                this.errorHandler = new ProjectionErrorHandler();
                this.metricsCollector = new ProjectionMetricsCollector();
            }
            
            public CompletableFuture<ProjectionDeploymentResult> deployProjections(
                    ProjectionDeploymentRequest request) {
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info("Deploying projections: {}", request.getProjectionNames());
                        
                        List<ProjectionDeploymentResult.ProjectionResult> results = new ArrayList<>();
                        
                        for (String projectionName : request.getProjectionNames()) {
                            ProjectionDefinition definition = projections.get(projectionName);
                            if (definition == null) {
                                throw new ProjectionNotFoundException("Projection not found: " + projectionName);
                            }
                            
                            // Deploy individual projection
                            ProjectionResult result = deployProjection(definition);
                            results.add(result);
                        }
                        
                        return ProjectionDeploymentResult.builder()
                            .requestId(request.getRequestId())
                            .projectionResults(results)
                            .deployedAt(Instant.now())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Projection deployment failed", e);
                        throw new ProjectionDeploymentException("Deployment failed", e);
                    }
                });
            }
            
            private ProjectionResult deployProjection(ProjectionDefinition definition) {
                String projectionName = definition.getName();
                
                try {
                    log.info("Deploying projection: {}", projectionName);
                    
                    // Initialize projection state
                    ProjectionState state = stateManager.initializeProjectionState(definition);
                    
                    // Setup event subscription
                    EventSubscription subscription = createEventSubscription(definition);
                    
                    // Create projection processor
                    ProjectionProcessor processor = createProjectionProcessor(definition, state);
                    
                    // Start processing events
                    CompletableFuture<Void> processingFuture = startProjectionProcessing(
                        processor, subscription
                    );
                    
                    // Setup health monitoring
                    ProjectionHealthMonitor healthMonitor = setupHealthMonitoring(definition);
                    
                    return ProjectionResult.builder()
                        .projectionName(projectionName)
                        .state(state)
                        .subscription(subscription)
                        .processor(processor)
                        .processingFuture(processingFuture)
                        .healthMonitor(healthMonitor)
                        .status(ProjectionStatus.RUNNING)
                        .deployedAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Projection deployment failed: {}", projectionName, e);
                    return ProjectionResult.builder()
                        .projectionName(projectionName)
                        .status(ProjectionStatus.FAILED)
                        .error(e.getMessage())
                        .build();
                }
            }
            
            private ProjectionProcessor createProjectionProcessor(
                    ProjectionDefinition definition, ProjectionState state) {
                
                return new ProjectionProcessor() {
                    @Override
                    public CompletableFuture<ProjectionProcessResult> processEvent(DomainEvent event) {
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                Timer.Sample processingTimer = Timer.start();
                                
                                // Check if event is relevant for this projection
                                if (!definition.isEventRelevant(event)) {
                                    return ProjectionProcessResult.skipped();
                                }
                                
                                // Load current projection data
                                Optional<ProjectionData> currentData = 
                                    projectionStorage.loadProjectionData(
                                        definition.getName(), 
                                        extractProjectionKey(event, definition)
                                    );
                                
                                // Apply event to projection
                                ProjectionData updatedData = definition.getProjectionHandler()
                                    .handle(event, currentData.orElse(null));
                                
                                // Save updated projection data
                                if (updatedData != null) {
                                    projectionStorage.saveProjectionData(
                                        definition.getName(),
                                        extractProjectionKey(event, definition),
                                        updatedData
                                    );
                                }
                                
                                // Update checkpoint
                                checkpointManager.updateCheckpoint(
                                    definition.getName(), 
                                    event.getEventId(),
                                    event.getTimestamp()
                                );
                                
                                // Record metrics
                                long processingTime = processingTimer.stop(
                                    Timer.builder("projection.processing.duration")
                                        .tag("projection", definition.getName())
                                        .register(meterRegistry)
                                ).longValue(TimeUnit.MILLISECONDS);
                                
                                metricsCollector.recordEventProcessed(
                                    definition.getName(), 
                                    event.getClass().getSimpleName(),
                                    processingTime
                                );
                                
                                return ProjectionProcessResult.builder()
                                    .eventId(event.getEventId())
                                    .projectionName(definition.getName())
                                    .status(ProcessStatus.SUCCESS)
                                    .processingTime(Duration.ofMillis(processingTime))
                                    .processedAt(Instant.now())
                                    .build();
                                    
                            } catch (Exception e) {
                                log.error("Projection processing failed: {} for event: {}", 
                                    definition.getName(), event.getEventId(), e);
                                
                                // Handle error based on strategy
                                ProjectionErrorResult errorResult = errorHandler.handleError(
                                    definition.getName(), event, e
                                );
                                
                                return ProjectionProcessResult.builder()
                                    .eventId(event.getEventId())
                                    .projectionName(definition.getName())
                                    .status(ProcessStatus.FAILED)
                                    .error(e.getMessage())
                                    .errorResult(errorResult)
                                    .processedAt(Instant.now())
                                    .build();
                            }
                        });
                    }
                };
            }
            
            private CompletableFuture<Void> startProjectionProcessing(
                    ProjectionProcessor processor, EventSubscription subscription) {
                
                return CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Starting projection processing for subscription: {}", 
                            subscription.getSubscriptionId());
                        
                        // Process events from subscription
                        subscription.processEvents(event -> {
                            processor.processEvent(event)
                                .thenAccept(result -> {
                                    if (result.getStatus() == ProcessStatus.FAILED) {
                                        log.warn("Event processing failed: {}", result);
                                    }
                                })
                                .exceptionally(throwable -> {
                                    log.error("Event processing exception", throwable);
                                    return null;
                                });
                        });
                        
                    } catch (Exception e) {
                        log.error("Projection processing failed", e);
                        throw new ProjectionProcessingException("Processing failed", e);
                    }
                });
            }
        }
        
        public class ReadModelQueryEngine {
            private final Map<String, ReadModelDefinition> readModels;
            private final QueryOptimizer queryOptimizer;
            private final CacheManager cacheManager;
            
            public ReadModelQueryEngine() {
                this.readModels = new ConcurrentHashMap<>();
                this.queryOptimizer = new QueryOptimizer();
                this.cacheManager = new CacheManager();
            }
            
            public <T> CompletableFuture<QueryResult<T>> executeQuery(Query<T> query) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Timer.Sample queryTimer = Timer.start();
                        
                        // Optimize query
                        OptimizedQuery<T> optimizedQuery = queryOptimizer.optimize(query);
                        
                        // Check cache first
                        Optional<T> cachedResult = cacheManager.get(optimizedQuery.getCacheKey());
                        if (cachedResult.isPresent()) {
                            return QueryResult.<T>builder()
                                .result(cachedResult.get())
                                .fromCache(true)
                                .executedAt(Instant.now())
                                .build();
                        }
                        
                        // Execute query against read model
                        ReadModelDefinition readModel = readModels.get(optimizedQuery.getReadModelName());
                        if (readModel == null) {
                            throw new ReadModelNotFoundException("Read model not found: " + 
                                optimizedQuery.getReadModelName());
                        }
                        
                        T result = readModel.getQueryHandler().execute(optimizedQuery);
                        
                        // Cache result if appropriate
                        if (optimizedQuery.isCacheable()) {
                            cacheManager.put(
                                optimizedQuery.getCacheKey(), 
                                result, 
                                optimizedQuery.getCacheTtl()
                            );
                        }
                        
                        // Record metrics
                        long queryTime = queryTimer.stop(
                            Timer.builder("readmodel.query.duration")
                                .tag("readmodel", optimizedQuery.getReadModelName())
                                .tag("query", optimizedQuery.getQueryType())
                                .register(meterRegistry)
                        ).longValue(TimeUnit.MILLISECONDS);
                        
                        return QueryResult.<T>builder()
                            .result(result)
                            .fromCache(false)
                            .queryTime(Duration.ofMillis(queryTime))
                            .executedAt(Instant.now())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Query execution failed", e);
                        throw new QueryExecutionException("Query execution failed", e);
                    }
                });
            }
        }
    }
}
```

## 2. Distributed Saga Orchestration

### Advanced Saga Orchestration Engine
```java
@Component
@Slf4j
public class DistributedSagaOrchestrator {
    
    @Autowired
    private SagaStateManager stateManager;
    
    @Autowired
    private SagaDefinitionRegistry sagaRegistry;
    
    @Autowired
    private CompensationManager compensationManager;
    
    public class SagaExecutionEngine {
        private final Map<String, RunningInstance> runningSagas;
        private final SagaStepExecutor stepExecutor;
        private final SagaTimeoutManager timeoutManager;
        private final SagaMetricsCollector metricsCollector;
        
        public SagaExecutionEngine() {
            this.runningSagas = new ConcurrentHashMap<>();
            this.stepExecutor = new SagaStepExecutor();
            this.timeoutManager = new SagaTimeoutManager();
            this.metricsCollector = new SagaMetricsCollector();
        }
        
        public CompletableFuture<SagaExecutionResult> startSaga(StartSagaCommand command) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String sagaId = command.getSagaId();
                    String sagaType = command.getSagaType();
                    
                    log.info("Starting saga: {} of type: {}", sagaId, sagaType);
                    
                    // Load saga definition
                    SagaDefinition definition = sagaRegistry.getSagaDefinition(sagaType);
                    if (definition == null) {
                        throw new SagaDefinitionNotFoundException("Saga definition not found: " + sagaType);
                    }
                    
                    // Create saga instance
                    SagaInstance instance = SagaInstance.builder()
                        .sagaId(sagaId)
                        .sagaType(sagaType)
                        .definition(definition)
                        .status(SagaStatus.RUNNING)
                        .currentStep(0)
                        .sagaData(command.getSagaData())
                        .startedAt(Instant.now())
                        .metadata(SagaMetadata.builder()
                            .correlationId(command.getCorrelationId())
                            .initiator(command.getInitiator())
                            .priority(command.getPriority())
                            .build())
                        .build();
                    
                    // Save initial state
                    stateManager.saveSagaState(instance);
                    
                    // Create running instance
                    RunningInstance runningInstance = new RunningInstance(instance);
                    runningSagas.put(sagaId, runningInstance);
                    
                    // Start executing saga steps
                    CompletableFuture<SagaExecutionResult> executionFuture = 
                        executeSagaSteps(runningInstance);
                    
                    // Setup timeout handling
                    timeoutManager.scheduleTimeout(sagaId, definition.getTimeout());
                    
                    return executionFuture.get();
                    
                } catch (Exception e) {
                    log.error("Saga start failed: {}", command.getSagaId(), e);
                    throw new SagaExecutionException("Failed to start saga", e);
                }
            });
        }
        
        private CompletableFuture<SagaExecutionResult> executeSagaSteps(RunningInstance instance) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    SagaInstance sagaInstance = instance.getSagaInstance();
                    SagaDefinition definition = sagaInstance.getDefinition();
                    List<SagaStep> steps = definition.getSteps();
                    
                    log.info("Executing saga steps for: {}", sagaInstance.getSagaId());
                    
                    List<SagaStepResult> stepResults = new ArrayList<>();
                    
                    for (int i = sagaInstance.getCurrentStep(); i < steps.size(); i++) {
                        SagaStep step = steps.get(i);
                        
                        try {
                            log.info("Executing saga step: {} for saga: {}", 
                                step.getName(), sagaInstance.getSagaId());
                            
                            // Update current step
                            sagaInstance.setCurrentStep(i);
                            stateManager.saveSagaState(sagaInstance);
                            
                            // Execute step
                            SagaStepResult stepResult = executeStep(sagaInstance, step);
                            stepResults.add(stepResult);
                            
                            if (stepResult.getStatus() == StepStatus.FAILED) {
                                log.error("Saga step failed: {} for saga: {}", 
                                    step.getName(), sagaInstance.getSagaId());
                                
                                // Start compensation
                                CompensationResult compensationResult = 
                                    startCompensation(sagaInstance, stepResults);
                                
                                return SagaExecutionResult.builder()
                                    .sagaId(sagaInstance.getSagaId())
                                    .status(SagaStatus.COMPENSATED)
                                    .stepResults(stepResults)
                                    .compensationResult(compensationResult)
                                    .completedAt(Instant.now())
                                    .build();
                            }
                            
                            // Handle step completion
                            handleStepCompletion(sagaInstance, step, stepResult);
                            
                        } catch (Exception e) {
                            log.error("Saga step execution error: {} for saga: {}", 
                                step.getName(), sagaInstance.getSagaId(), e);
                            
                            SagaStepResult errorResult = SagaStepResult.builder()
                                .stepName(step.getName())
                                .status(StepStatus.FAILED)
                                .error(e.getMessage())
                                .executedAt(Instant.now())
                                .build();
                            
                            stepResults.add(errorResult);
                            
                            // Start compensation
                            CompensationResult compensationResult = 
                                startCompensation(sagaInstance, stepResults);
                            
                            return SagaExecutionResult.builder()
                                .sagaId(sagaInstance.getSagaId())
                                .status(SagaStatus.FAILED)
                                .stepResults(stepResults)
                                .compensationResult(compensationResult)
                                .error(e.getMessage())
                                .completedAt(Instant.now())
                                .build();
                        }
                    }
                    
                    // All steps completed successfully
                    sagaInstance.setStatus(SagaStatus.COMPLETED);
                    sagaInstance.setCompletedAt(Instant.now());
                    stateManager.saveSagaState(sagaInstance);
                    
                    // Cleanup
                    runningSagas.remove(sagaInstance.getSagaId());
                    timeoutManager.cancelTimeout(sagaInstance.getSagaId());
                    
                    // Record metrics
                    metricsCollector.recordSagaCompletion(
                        sagaInstance.getSagaType(),
                        Duration.between(sagaInstance.getStartedAt(), Instant.now()),
                        SagaStatus.COMPLETED
                    );
                    
                    return SagaExecutionResult.builder()
                        .sagaId(sagaInstance.getSagaId())
                        .status(SagaStatus.COMPLETED)
                        .stepResults(stepResults)
                        .completedAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Saga execution failed", e);
                    throw new SagaExecutionException("Saga execution failed", e);
                }
            });
        }
        
        private SagaStepResult executeStep(SagaInstance sagaInstance, SagaStep step) {
            try {
                Timer.Sample stepTimer = Timer.start();
                
                // Prepare step context
                SagaStepContext context = SagaStepContext.builder()
                    .sagaId(sagaInstance.getSagaId())
                    .sagaType(sagaInstance.getSagaType())
                    .stepName(step.getName())
                    .sagaData(sagaInstance.getSagaData())
                    .correlationId(sagaInstance.getMetadata().getCorrelationId())
                    .build();
                
                // Execute step action
                StepExecutionResult executionResult = stepExecutor.executeStep(step, context);
                
                // Update saga data if provided
                if (executionResult.getUpdatedSagaData() != null) {
                    sagaInstance.setSagaData(executionResult.getUpdatedSagaData());
                }
                
                // Record step completion time
                long stepDuration = stepTimer.stop(
                    Timer.builder("saga.step.duration")
                        .tag("saga_type", sagaInstance.getSagaType())
                        .tag("step", step.getName())
                        .register(meterRegistry)
                ).longValue(TimeUnit.MILLISECONDS);
                
                return SagaStepResult.builder()
                    .stepName(step.getName())
                    .status(executionResult.isSuccess() ? StepStatus.COMPLETED : StepStatus.FAILED)
                    .result(executionResult.getResult())
                    .error(executionResult.getError())
                    .executionTime(Duration.ofMillis(stepDuration))
                    .executedAt(Instant.now())
                    .build();
                    
            } catch (Exception e) {
                log.error("Step execution failed: {} for saga: {}", 
                    step.getName(), sagaInstance.getSagaId(), e);
                
                return SagaStepResult.builder()
                    .stepName(step.getName())
                    .status(StepStatus.FAILED)
                    .error(e.getMessage())
                    .executedAt(Instant.now())
                    .build();
            }
        }
        
        private CompensationResult startCompensation(SagaInstance sagaInstance, 
                List<SagaStepResult> completedSteps) {
            
            log.info("Starting compensation for saga: {}", sagaInstance.getSagaId());
            
            try {
                sagaInstance.setStatus(SagaStatus.COMPENSATING);
                stateManager.saveSagaState(sagaInstance);
                
                List<CompensationStepResult> compensationResults = new ArrayList<>();
                
                // Compensate in reverse order
                List<SagaStepResult> successfulSteps = completedSteps.stream()
                    .filter(result -> result.getStatus() == StepStatus.COMPLETED)
                    .collect(Collectors.toList());
                
                Collections.reverse(successfulSteps);
                
                for (SagaStepResult stepResult : successfulSteps) {
                    try {
                        // Find corresponding step definition
                        SagaStep step = sagaInstance.getDefinition().getSteps().stream()
                            .filter(s -> s.getName().equals(stepResult.getStepName()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Step not found: " + 
                                stepResult.getStepName()));
                        
                        // Execute compensation
                        CompensationStepResult compensationResult = 
                            executeCompensation(sagaInstance, step, stepResult);
                        
                        compensationResults.add(compensationResult);
                        
                        if (compensationResult.getStatus() == CompensationStatus.FAILED) {
                            log.error("Compensation failed for step: {} in saga: {}", 
                                step.getName(), sagaInstance.getSagaId());
                        }
                        
                    } catch (Exception e) {
                        log.error("Compensation execution error for step: {} in saga: {}", 
                            stepResult.getStepName(), sagaInstance.getSagaId(), e);
                        
                        compensationResults.add(CompensationStepResult.builder()
                            .stepName(stepResult.getStepName())
                            .status(CompensationStatus.FAILED)
                            .error(e.getMessage())
                            .executedAt(Instant.now())
                            .build());
                    }
                }
                
                // Update saga status
                boolean allCompensationSuccessful = compensationResults.stream()
                    .allMatch(result -> result.getStatus() == CompensationStatus.COMPLETED);
                
                sagaInstance.setStatus(allCompensationSuccessful ? 
                    SagaStatus.COMPENSATED : SagaStatus.COMPENSATION_FAILED);
                sagaInstance.setCompletedAt(Instant.now());
                stateManager.saveSagaState(sagaInstance);
                
                // Cleanup
                runningSagas.remove(sagaInstance.getSagaId());
                timeoutManager.cancelTimeout(sagaInstance.getSagaId());
                
                // Record metrics
                metricsCollector.recordSagaCompletion(
                    sagaInstance.getSagaType(),
                    Duration.between(sagaInstance.getStartedAt(), Instant.now()),
                    sagaInstance.getStatus()
                );
                
                return CompensationResult.builder()
                    .sagaId(sagaInstance.getSagaId())
                    .compensationSteps(compensationResults)
                    .allSuccessful(allCompensationSuccessful)
                    .completedAt(Instant.now())
                    .build();
                    
            } catch (Exception e) {
                log.error("Compensation failed for saga: {}", sagaInstance.getSagaId(), e);
                throw new CompensationException("Compensation failed", e);
            }
        }
        
        private CompensationStepResult executeCompensation(SagaInstance sagaInstance, 
                SagaStep step, SagaStepResult originalResult) {
            
            try {
                log.info("Executing compensation for step: {} in saga: {}", 
                    step.getName(), sagaInstance.getSagaId());
                
                Timer.Sample compensationTimer = Timer.start();
                
                // Prepare compensation context
                CompensationContext context = CompensationContext.builder()
                    .sagaId(sagaInstance.getSagaId())
                    .sagaType(sagaInstance.getSagaType())
                    .stepName(step.getName())
                    .originalResult(originalResult.getResult())
                    .sagaData(sagaInstance.getSagaData())
                    .correlationId(sagaInstance.getMetadata().getCorrelationId())
                    .build();
                
                // Execute compensation action
                CompensationExecutionResult result = compensationManager.executeCompensation(
                    step, context
                );
                
                // Record compensation time
                long compensationDuration = compensationTimer.stop(
                    Timer.builder("saga.compensation.duration")
                        .tag("saga_type", sagaInstance.getSagaType())
                        .tag("step", step.getName())
                        .register(meterRegistry)
                ).longValue(TimeUnit.MILLISECONDS);
                
                return CompensationStepResult.builder()
                    .stepName(step.getName())
                    .status(result.isSuccess() ? 
                        CompensationStatus.COMPLETED : CompensationStatus.FAILED)
                    .result(result.getResult())
                    .error(result.getError())
                    .executionTime(Duration.ofMillis(compensationDuration))
                    .executedAt(Instant.now())
                    .build();
                    
            } catch (Exception e) {
                log.error("Compensation execution failed for step: {} in saga: {}", 
                    step.getName(), sagaInstance.getSagaId(), e);
                
                return CompensationStepResult.builder()
                    .stepName(step.getName())
                    .status(CompensationStatus.FAILED)
                    .error(e.getMessage())
                    .executedAt(Instant.now())
                    .build();
            }
        }
    }
    
    @Service
    public static class SagaDefinitionBuilder {
        
        public static class OrderProcessingSagaDefinition {
            
            public static SagaDefinition create() {
                return SagaDefinition.builder()
                    .name("OrderProcessingSaga")
                    .description("Handles complete order processing with payment and inventory")
                    .timeout(Duration.ofMinutes(30))
                    .steps(Arrays.asList(
                        // Step 1: Validate Order
                        SagaStep.builder()
                            .name("ValidateOrder")
                            .description("Validate order details and customer information")
                            .action(new ValidateOrderAction())
                            .compensation(new ValidateOrderCompensation())
                            .timeout(Duration.ofSeconds(30))
                            .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(3)
                                .backoffStrategy(BackoffStrategy.EXPONENTIAL)
                                .baseDelay(Duration.ofSeconds(1))
                                .build())
                            .build(),
                        
                        // Step 2: Reserve Inventory
                        SagaStep.builder()
                            .name("ReserveInventory")
                            .description("Reserve required items in inventory")
                            .action(new ReserveInventoryAction())
                            .compensation(new ReleaseInventoryCompensation())
                            .timeout(Duration.ofSeconds(45))
                            .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(5)
                                .backoffStrategy(BackoffStrategy.LINEAR)
                                .baseDelay(Duration.ofSeconds(2))
                                .build())
                            .build(),
                        
                        // Step 3: Process Payment
                        SagaStep.builder()
                            .name("ProcessPayment")
                            .description("Process customer payment")
                            .action(new ProcessPaymentAction())
                            .compensation(new RefundPaymentCompensation())
                            .timeout(Duration.ofMinutes(2))
                            .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(3)
                                .backoffStrategy(BackoffStrategy.EXPONENTIAL)
                                .baseDelay(Duration.ofSeconds(5))
                                .build())
                            .build(),
                        
                        // Step 4: Update Customer Points
                        SagaStep.builder()
                            .name("UpdateCustomerPoints")
                            .description("Award loyalty points to customer")
                            .action(new UpdateCustomerPointsAction())
                            .compensation(new RevertCustomerPointsCompensation())
                            .timeout(Duration.ofSeconds(30))
                            .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(3)
                                .backoffStrategy(BackoffStrategy.FIXED)
                                .baseDelay(Duration.ofSeconds(1))
                                .build())
                            .build(),
                        
                        // Step 5: Create Shipment
                        SagaStep.builder()
                            .name("CreateShipment")
                            .description("Create shipment for order fulfillment")
                            .action(new CreateShipmentAction())
                            .compensation(new CancelShipmentCompensation())
                            .timeout(Duration.ofMinutes(1))
                            .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(3)
                                .backoffStrategy(BackoffStrategy.EXPONENTIAL)
                                .baseDelay(Duration.ofSeconds(2))
                                .build())
                            .build(),
                        
                        // Step 6: Send Confirmation
                        SagaStep.builder()
                            .name("SendConfirmation")
                            .description("Send order confirmation to customer")
                            .action(new SendConfirmationAction())
                            .compensation(new SendCancellationNotificationCompensation())
                            .timeout(Duration.ofSeconds(30))
                            .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(5)
                                .backoffStrategy(BackoffStrategy.LINEAR)
                                .baseDelay(Duration.ofSeconds(1))
                                .build())
                            .build()
                    ))
                    .build();
            }
        }
        
        // Sample action implementations
        public static class ValidateOrderAction implements SagaStepAction {
            @Override
            public StepExecutionResult execute(SagaStepContext context) {
                try {
                    // Extract order data
                    OrderData orderData = (OrderData) context.getSagaData().get("order");
                    
                    // Validate order details
                    if (orderData.getItems().isEmpty()) {
                        return StepExecutionResult.failure("Order has no items");
                    }
                    
                    if (orderData.getCustomerId() == null) {
                        return StepExecutionResult.failure("Customer ID is required");
                    }
                    
                    // Additional validation logic...
                    
                    // Update saga data with validation results
                    Map<String, Object> updatedData = new HashMap<>(context.getSagaData());
                    updatedData.put("validationResult", ValidationResult.builder()
                        .valid(true)
                        .validatedAt(Instant.now())
                        .build());
                    
                    return StepExecutionResult.success(updatedData);
                    
                } catch (Exception e) {
                    return StepExecutionResult.failure("Order validation failed: " + e.getMessage());
                }
            }
        }
        
        public static class ReserveInventoryAction implements SagaStepAction {
            @Override
            public StepExecutionResult execute(SagaStepContext context) {
                try {
                    OrderData orderData = (OrderData) context.getSagaData().get("order");
                    
                    List<InventoryReservation> reservations = new ArrayList<>();
                    
                    for (OrderItem item : orderData.getItems()) {
                        // Call inventory service to reserve items
                        InventoryReservation reservation = inventoryService.reserveItem(
                            item.getProductId(), 
                            item.getQuantity(),
                            context.getCorrelationId()
                        );
                        
                        if (!reservation.isSuccessful()) {
                            // Rollback previous reservations
                            rollbackReservations(reservations);
                            return StepExecutionResult.failure(
                                "Failed to reserve inventory for product: " + item.getProductId()
                            );
                        }
                        
                        reservations.add(reservation);
                    }
                    
                    // Update saga data with reservation details
                    Map<String, Object> updatedData = new HashMap<>(context.getSagaData());
                    updatedData.put("inventoryReservations", reservations);
                    
                    return StepExecutionResult.success(updatedData);
                    
                } catch (Exception e) {
                    return StepExecutionResult.failure("Inventory reservation failed: " + e.getMessage());
                }
            }
            
            private void rollbackReservations(List<InventoryReservation> reservations) {
                for (InventoryReservation reservation : reservations) {
                    try {
                        inventoryService.releaseReservation(reservation.getReservationId());
                    } catch (Exception e) {
                        log.error("Failed to rollback inventory reservation: {}", 
                            reservation.getReservationId(), e);
                    }
                }
            }
        }
    }
}
```

## 3. Event Streaming Platform

### Advanced Event Streaming Infrastructure
```java
@Component
@Slf4j
public class EventStreamingPlatform {
    
    @Autowired
    private StreamProcessingEngine processingEngine;
    
    @Autowired
    private EventRoutingManager routingManager;
    
    @Autowired
    private StreamTopologyManager topologyManager;
    
    public class StreamOrchestrator {
        private final Map<String, StreamDefinition> streams;
        private final StreamStateManager stateManager;
        private final StreamMetricsCollector metricsCollector;
        private final DeadLetterQueueManager dlqManager;
        
        public StreamOrchestrator() {
            this.streams = new ConcurrentHashMap<>();
            this.stateManager = new StreamStateManager();
            this.metricsCollector = new StreamMetricsCollector();
            this.dlqManager = new DeadLetterQueueManager();
        }
        
        public CompletableFuture<StreamDeploymentResult> deployEventStreams(
                StreamDeploymentRequest request) {
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Deploying event streams: {}", request.getStreamNames());
                    
                    List<StreamDeploymentResult.StreamResult> results = new ArrayList<>();
                    
                    for (String streamName : request.getStreamNames()) {
                        StreamDefinition definition = streams.get(streamName);
                        if (definition == null) {
                            throw new StreamDefinitionNotFoundException("Stream not found: " + streamName);
                        }
                        
                        // Deploy individual stream
                        StreamResult result = deployStream(definition);
                        results.add(result);
                    }
                    
                    return StreamDeploymentResult.builder()
                        .requestId(request.getRequestId())
                        .streamResults(results)
                        .deployedAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Stream deployment failed", e);
                    throw new StreamDeploymentException("Deployment failed", e);
                }
            });
        }
        
        private StreamResult deployStream(StreamDefinition definition) {
            String streamName = definition.getName();
            
            try {
                log.info("Deploying stream: {}", streamName);
                
                // Create stream topology
                StreamTopology topology = createStreamTopology(definition);
                
                // Configure stream processing
                StreamProcessor processor = configureStreamProcessor(definition, topology);
                
                // Setup stream monitoring
                StreamMonitor monitor = setupStreamMonitoring(definition);
                
                // Start stream processing
                CompletableFuture<Void> processingFuture = startStreamProcessing(processor);
                
                return StreamResult.builder()
                    .streamName(streamName)
                    .topology(topology)
                    .processor(processor)
                    .monitor(monitor)
                    .processingFuture(processingFuture)
                    .status(StreamStatus.RUNNING)
                    .deployedAt(Instant.now())
                    .build();
                    
            } catch (Exception e) {
                log.error("Stream deployment failed: {}", streamName, e);
                return StreamResult.builder()
                    .streamName(streamName)
                    .status(StreamStatus.FAILED)
                    .error(e.getMessage())
                    .build();
            }
        }
        
        private StreamTopology createStreamTopology(StreamDefinition definition) {
            StreamsBuilder builder = new StreamsBuilder();
            
            // Create source stream
            KStream<String, Object> sourceStream = builder.stream(
                definition.getSourceTopic(),
                Consumed.with(Serdes.String(), createSerde(definition.getValueType()))
                    .withTimestampExtractor(new EventTimestampExtractor())
            );
            
            // Apply stream transformations
            KStream<String, Object> transformedStream = sourceStream;
            
            for (StreamTransformation transformation : definition.getTransformations()) {
                transformedStream = applyTransformation(transformedStream, transformation);
            }
            
            // Apply filtering if configured
            if (definition.getFilterPredicate() != null) {
                transformedStream = transformedStream.filter(definition.getFilterPredicate());
            }
            
            // Apply windowing if configured
            if (definition.getWindowConfiguration() != null) {
                transformedStream = applyWindowing(transformedStream, definition.getWindowConfiguration());
            }
            
            // Route to output topics
            for (StreamOutput output : definition.getOutputs()) {
                KStream<String, Object> outputStream = transformedStream;
                
                // Apply output-specific transformations
                for (StreamTransformation transformation : output.getTransformations()) {
                    outputStream = applyTransformation(outputStream, transformation);
                }
                
                // Send to output topic
                outputStream.to(
                    output.getTopicName(),
                    Produced.with(Serdes.String(), createSerde(output.getValueType()))
                );
            }
            
            return builder.build();
        }
        
        private KStream<String, Object> applyTransformation(
                KStream<String, Object> stream, StreamTransformation transformation) {
            
            switch (transformation.getType()) {
                case MAP:
                    return stream.map((key, value) -> {
                        Object transformedValue = transformation.getMapper().apply(value);
                        return KeyValue.pair(key, transformedValue);
                    });
                
                case FLAT_MAP:
                    return stream.flatMap((key, value) -> {
                        List<KeyValue<String, Object>> results = 
                            transformation.getFlatMapper().apply(key, value);
                        return results;
                    });
                
                case FILTER:
                    return stream.filter(transformation.getPredicate());
                
                case MAP_VALUES:
                    return stream.mapValues(transformation.getValueMapper());
                
                case ENRICH:
                    return enrichStream(stream, transformation.getEnrichmentConfig());
                
                case AGGREGATE:
                    return aggregateStream(stream, transformation.getAggregationConfig());
                
                case JOIN:
                    return joinStream(stream, transformation.getJoinConfig());
                
                default:
                    throw new UnsupportedTransformationException(
                        "Unsupported transformation type: " + transformation.getType()
                    );
            }
        }
        
        private KStream<String, Object> enrichStream(KStream<String, Object> stream, 
                EnrichmentConfiguration config) {
            
            // Load enrichment data as KTable
            KTable<String, Object> enrichmentTable = createEnrichmentTable(config);
            
            // Perform stream-table join for enrichment
            return stream.leftJoin(
                enrichmentTable,
                (streamValue, enrichmentValue) -> {
                    if (enrichmentValue != null) {
                        return config.getEnrichmentFunction().apply(streamValue, enrichmentValue);
                    }
                    return streamValue;
                },
                Joined.with(Serdes.String(), createSerde(config.getStreamValueType()), 
                           createSerde(config.getEnrichmentValueType()))
            );
        }
        
        private KStream<String, Object> applyWindowing(KStream<String, Object> stream, 
                WindowConfiguration windowConfig) {
            
            switch (windowConfig.getType()) {
                case TUMBLING:
                    return stream.groupByKey()
                        .windowedBy(TimeWindows.of(windowConfig.getSize()))
                        .aggregate(
                            windowConfig.getInitializer(),
                            windowConfig.getAggregator(),
                            createSerde(windowConfig.getValueType())
                        )
                        .toStream()
                        .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value));
                
                case HOPPING:
                    return stream.groupByKey()
                        .windowedBy(TimeWindows.of(windowConfig.getSize())
                            .advanceBy(windowConfig.getAdvanceBy()))
                        .aggregate(
                            windowConfig.getInitializer(),
                            windowConfig.getAggregator(),
                            createSerde(windowConfig.getValueType())
                        )
                        .toStream()
                        .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value));
                
                case SESSION:
                    return stream.groupByKey()
                        .windowedBy(SessionWindows.with(windowConfig.getInactivityGap()))
                        .aggregate(
                            windowConfig.getInitializer(),
                            windowConfig.getAggregator(),
                            windowConfig.getSessionMerger(),
                            createSerde(windowConfig.getValueType())
                        )
                        .toStream()
                        .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value));
                
                default:
                    throw new UnsupportedWindowTypeException(
                        "Unsupported window type: " + windowConfig.getType()
                    );
            }
        }
    }
    
    @Service
    public static class IntelligentEventRouter {
        
        @Autowired
        private RoutingRuleEngine ruleEngine;
        
        @Autowired
        private ContentBasedRouter contentRouter;
        
        @Autowired
        private LoadBalancingRouter loadBalancer;
        
        public class RoutingOrchestrator {
            private final Map<String, RoutingConfiguration> routingConfigs;
            private final RoutingMetricsCollector metricsCollector;
            private final RoutingStateManager stateManager;
            
            public RoutingOrchestrator() {
                this.routingConfigs = new ConcurrentHashMap<>();
                this.metricsCollector = new RoutingMetricsCollector();
                this.stateManager = new RoutingStateManager();
            }
            
            public CompletableFuture<RoutingResult> routeEvent(EventRoutingRequest request) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Timer.Sample routingTimer = Timer.start();
                        
                        DomainEvent event = request.getEvent();
                        String eventType = event.getClass().getSimpleName();
                        
                        log.debug("Routing event: {} of type: {}", event.getEventId(), eventType);
                        
                        // Get routing configuration
                        RoutingConfiguration config = routingConfigs.get(eventType);
                        if (config == null) {
                            // Use default routing
                            config = getDefaultRoutingConfiguration();
                        }
                        
                        // Apply routing rules
                        List<RoutingDestination> destinations = 
                            determineDestinations(event, config);
                        
                        if (destinations.isEmpty()) {
                            log.warn("No routing destinations found for event: {}", event.getEventId());
                            return RoutingResult.builder()
                                .eventId(event.getEventId())
                                .destinations(Collections.emptyList())
                                .status(RoutingStatus.NO_DESTINATIONS)
                                .routedAt(Instant.now())
                                .build();
                        }
                        
                        // Route to destinations
                        List<DestinationResult> destinationResults = new ArrayList<>();
                        
                        for (RoutingDestination destination : destinations) {
                            try {
                                DestinationResult result = routeToDestination(event, destination);
                                destinationResults.add(result);
                            } catch (Exception e) {
                                log.error("Routing to destination failed: {}", destination.getName(), e);
                                destinationResults.add(DestinationResult.builder()
                                    .destinationName(destination.getName())
                                    .status(DestinationStatus.FAILED)
                                    .error(e.getMessage())
                                    .build());
                            }
                        }
                        
                        // Handle routing failures
                        List<DestinationResult> failedResults = destinationResults.stream()
                            .filter(result -> result.getStatus() == DestinationStatus.FAILED)
                            .collect(Collectors.toList());
                        
                        if (!failedResults.isEmpty()) {
                            handleRoutingFailures(event, failedResults, config);
                        }
                        
                        // Record metrics
                        long routingTime = routingTimer.stop(
                            Timer.builder("event.routing.duration")
                                .tag("event_type", eventType)
                                .register(meterRegistry)
                        ).longValue(TimeUnit.MILLISECONDS);
                        
                        metricsCollector.recordEventRouted(
                            eventType, 
                            destinations.size(),
                            failedResults.size(),
                            routingTime
                        );
                        
                        return RoutingResult.builder()
                            .eventId(event.getEventId())
                            .destinations(destinations)
                            .destinationResults(destinationResults)
                            .status(failedResults.isEmpty() ? 
                                RoutingStatus.SUCCESS : RoutingStatus.PARTIAL_FAILURE)
                            .routingTime(Duration.ofMillis(routingTime))
                            .routedAt(Instant.now())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Event routing failed", e);
                        throw new EventRoutingException("Routing failed", e);
                    }
                });
            }
            
            private List<RoutingDestination> determineDestinations(DomainEvent event, 
                    RoutingConfiguration config) {
                
                List<RoutingDestination> destinations = new ArrayList<>();
                
                // Apply content-based routing rules
                for (ContentBasedRule rule : config.getContentBasedRules()) {
                    if (rule.matches(event)) {
                        destinations.addAll(rule.getDestinations());
                    }
                }
                
                // Apply header-based routing rules
                for (HeaderBasedRule rule : config.getHeaderBasedRules()) {
                    if (rule.matches(event.getHeaders())) {
                        destinations.addAll(rule.getDestinations());
                    }
                }
                
                // Apply topic-based routing
                if (config.getTopicBasedRouting() != null) {
                    String targetTopic = config.getTopicBasedRouting().determineTarget(event);
                    if (targetTopic != null) {
                        destinations.add(RoutingDestination.builder()
                            .name(targetTopic)
                            .type(DestinationType.TOPIC)
                            .address(targetTopic)
                            .build());
                    }
                }
                
                // Apply load balancing if multiple destinations
                if (destinations.size() > 1 && config.getLoadBalancingStrategy() != null) {
                    destinations = loadBalancer.selectDestinations(
                        destinations, 
                        config.getLoadBalancingStrategy(),
                        event
                    );
                }
                
                return destinations.stream().distinct().collect(Collectors.toList());
            }
            
            private DestinationResult routeToDestination(DomainEvent event, 
                    RoutingDestination destination) {
                
                try {
                    Timer.Sample destinationTimer = Timer.start();
                    
                    switch (destination.getType()) {
                        case TOPIC:
                            return routeToTopic(event, destination);
                        
                        case QUEUE:
                            return routeToQueue(event, destination);
                        
                        case HTTP_ENDPOINT:
                            return routeToHttpEndpoint(event, destination);
                        
                        case WEBSOCKET:
                            return routeToWebSocket(event, destination);
                        
                        case DATABASE:
                            return routeToDatabase(event, destination);
                        
                        default:
                            throw new UnsupportedDestinationTypeException(
                                "Unsupported destination type: " + destination.getType()
                            );
                    }
                    
                } catch (Exception e) {
                    log.error("Routing to destination failed: {}", destination.getName(), e);
                    return DestinationResult.builder()
                        .destinationName(destination.getName())
                        .status(DestinationStatus.FAILED)
                        .error(e.getMessage())
                        .build();
                }
            }
            
            private void handleRoutingFailures(DomainEvent event, List<DestinationResult> failures, 
                    RoutingConfiguration config) {
                
                for (DestinationResult failure : failures) {
                    // Send to dead letter queue if configured
                    if (config.getDeadLetterQueueConfig() != null) {
                        try {
                            dlqManager.sendToDeadLetterQueue(
                                event, 
                                failure.getDestinationName(),
                                failure.getError(),
                                config.getDeadLetterQueueConfig()
                            );
                        } catch (Exception e) {
                            log.error("Failed to send event to dead letter queue", e);
                        }
                    }
                    
                    // Apply retry strategy if configured
                    if (config.getRetryStrategy() != null) {
                        scheduleRetry(event, failure.getDestinationName(), config.getRetryStrategy());
                    }
                    
                    // Send alert if configured
                    if (config.getAlertingConfig() != null) {
                        sendRoutingFailureAlert(event, failure, config.getAlertingConfig());
                    }
                }
            }
        }
    }
}
```

## Key Learning Outcomes

After completing Day 128, you will have mastered:

1. **Advanced Event Sourcing Mastery**
   - Enterprise-grade event store implementation with encryption and replication
   - Sophisticated snapshot management and consistency guarantees
   - Advanced event serialization and versioning strategies
   - High-performance event replay and projection optimization

2. **CQRS Excellence with Projections**
   - Intelligent projection orchestration with automatic recovery
   - Advanced read model optimization and caching strategies
   - Multi-tenant projection management and isolation
   - Real-time projection updates with guaranteed consistency

3. **Distributed Saga Orchestration**
   - Complex saga workflow design with conditional branching
   - Advanced compensation strategies and failure handling
   - Saga state management with persistence and recovery
   - Performance optimization and timeout handling

4. **Event Streaming Platform Engineering**
   - Sophisticated stream processing topologies with Kafka Streams
   - Intelligent event routing with content-based and load-balanced strategies
   - Advanced windowing and aggregation techniques
   - Dead letter queue management and error recovery

5. **Enterprise Integration Patterns**
   - Event-driven microservices communication patterns
   - Advanced event transformation and enrichment pipelines
   - Multi-protocol event routing (HTTP, WebSocket, Message Queues)
   - Production monitoring and observability for event-driven systems

This comprehensive foundation enables you to architect and implement the most sophisticated event-driven systems used by Fortune 500 companies.

## Additional Resources
- Event Sourcing and CQRS Patterns Guide
- Apache Kafka Streams Advanced Patterns
- Distributed Saga Implementation Strategies
- Event-Driven Architecture Best Practices
- Enterprise Event Streaming Platform Design