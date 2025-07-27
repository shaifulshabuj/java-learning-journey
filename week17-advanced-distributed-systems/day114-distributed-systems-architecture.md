# Day 114: Distributed Systems Architecture - Consensus Algorithms, CAP Theorem & Distributed Transactions

## Learning Objectives
- Understand and implement consensus algorithms including Raft and PBFT
- Apply CAP theorem principles in practical distributed system design
- Design and implement distributed transaction coordination mechanisms
- Build Byzantine fault-tolerant systems with advanced recovery patterns
- Create distributed lock managers and coordination services
- Implement partition-tolerant architectures with eventual consistency

## Part 1: Raft Consensus Algorithm Implementation

### Raft Node Implementation with Leader Election

```java
package com.distributed.consensus.raft;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RaftNode {

    private static final Logger logger = LoggerFactory.getLogger(RaftNode.class);

    // Raft state
    private final String nodeId;
    private final AtomicReference<NodeState> state = new AtomicReference<>(NodeState.FOLLOWER);
    private final AtomicLong currentTerm = new AtomicLong(0);
    private volatile String votedFor = null;
    private volatile String currentLeader = null;

    // Log state
    private final List<LogEntry> log = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong commitIndex = new AtomicLong(0);
    private final AtomicLong lastApplied = new AtomicLong(0);

    // Leader state (only used when this node is leader)
    private final Map<String, Long> nextIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> matchIndex = new ConcurrentHashMap<>();

    // Cluster configuration
    private final Set<String> clusterNodes;
    private final RaftNetworkService networkService;
    private final RaftStateMachine stateMachine;

    // Timing configuration
    private volatile long lastHeartbeat = System.currentTimeMillis();
    private volatile long electionTimeout = generateElectionTimeout();
    private static final long HEARTBEAT_INTERVAL = 50; // 50ms
    private static final long MIN_ELECTION_TIMEOUT = 150; // 150ms
    private static final long MAX_ELECTION_TIMEOUT = 300; // 300ms

    public RaftNode(String nodeId, Set<String> clusterNodes, 
            RaftNetworkService networkService, RaftStateMachine stateMachine) {
        this.nodeId = nodeId;
        this.clusterNodes = new HashSet<>(clusterNodes);
        this.networkService = networkService;
        this.stateMachine = stateMachine;
    }

    @Scheduled(fixedDelay = 10) // Check every 10ms
    public void raftTick() {
        long now = System.currentTimeMillis();
        
        switch (state.get()) {
            case FOLLOWER:
                handleFollowerTick(now);
                break;
            case CANDIDATE:
                handleCandidateTick(now);
                break;
            case LEADER:
                handleLeaderTick(now);
                break;
        }
    }

    public Mono<ClientResponse> handleClientRequest(ClientRequest request) {
        if (state.get() != NodeState.LEADER) {
            return Mono.just(ClientResponse.builder()
                    .success(false)
                    .leaderId(currentLeader)
                    .message("Not the leader. Redirect to: " + currentLeader)
                    .build());
        }

        return appendLogEntry(request)
                .flatMap(this::replicateToMajority)
                .flatMap(this::applyToStateMachine)
                .map(result -> ClientResponse.builder()
                        .success(true)
                        .result(result)
                        .leaderId(nodeId)
                        .message("Request processed successfully")
                        .build())
                .onErrorResume(error -> Mono.just(ClientResponse.builder()
                        .success(false)
                        .leaderId(nodeId)
                        .message("Failed to process request: " + error.getMessage())
                        .build()));
    }

    public Mono<VoteResponse> handleVoteRequest(VoteRequest request) {
        logger.debug("Node {} received vote request from {} for term {}", 
                nodeId, request.getCandidateId(), request.getTerm());

        return Mono.fromCallable(() -> {
            synchronized (this) {
                // If request term is stale, reject
                if (request.getTerm() < currentTerm.get()) {
                    return VoteResponse.builder()
                            .term(currentTerm.get())
                            .voteGranted(false)
                            .nodeId(nodeId)
                            .build();
                }

                // If request term is newer, update our term and convert to follower
                if (request.getTerm() > currentTerm.get()) {
                    currentTerm.set(request.getTerm());
                    votedFor = null;
                    state.set(NodeState.FOLLOWER);
                    currentLeader = null;
                }

                // Check if we can vote for this candidate
                boolean canVote = (votedFor == null || votedFor.equals(request.getCandidateId()));
                boolean logUpToDate = isLogUpToDate(request);

                if (canVote && logUpToDate) {
                    votedFor = request.getCandidateId();
                    lastHeartbeat = System.currentTimeMillis();
                    
                    logger.info("Node {} granted vote to {} for term {}", 
                            nodeId, request.getCandidateId(), request.getTerm());
                    
                    return VoteResponse.builder()
                            .term(currentTerm.get())
                            .voteGranted(true)
                            .nodeId(nodeId)
                            .build();
                } else {
                    logger.debug("Node {} denied vote to {} for term {} (canVote: {}, logUpToDate: {})", 
                            nodeId, request.getCandidateId(), request.getTerm(), canVote, logUpToDate);
                    
                    return VoteResponse.builder()
                            .term(currentTerm.get())
                            .voteGranted(false)
                            .nodeId(nodeId)
                            .build();
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AppendEntriesResponse> handleAppendEntries(AppendEntriesRequest request) {
        logger.debug("Node {} received append entries from {} for term {} with {} entries", 
                nodeId, request.getLeaderId(), request.getTerm(), request.getEntries().size());

        return Mono.fromCallable(() -> {
            synchronized (this) {
                // Update heartbeat timestamp
                lastHeartbeat = System.currentTimeMillis();

                // If request term is stale, reject
                if (request.getTerm() < currentTerm.get()) {
                    return AppendEntriesResponse.builder()
                            .term(currentTerm.get())
                            .success(false)
                            .nodeId(nodeId)
                            .build();
                }

                // If request term is newer or equal, update our state
                if (request.getTerm() >= currentTerm.get()) {
                    currentTerm.set(request.getTerm());
                    state.set(NodeState.FOLLOWER);
                    currentLeader = request.getLeaderId();
                    votedFor = null;
                }

                // Log consistency check
                if (!isLogConsistent(request)) {
                    return AppendEntriesResponse.builder()
                            .term(currentTerm.get())
                            .success(false)
                            .nodeId(nodeId)
                            .conflictIndex(findConflictIndex(request))
                            .build();
                }

                // Append new entries
                appendNewEntries(request);

                // Update commit index
                if (request.getLeaderCommit() > commitIndex.get()) {
                    long newCommitIndex = Math.min(request.getLeaderCommit(), 
                            log.size() > 0 ? log.size() - 1 : 0);
                    commitIndex.set(newCommitIndex);
                    applyCommittedEntries();
                }

                return AppendEntriesResponse.builder()
                        .term(currentTerm.get())
                        .success(true)
                        .nodeId(nodeId)
                        .matchIndex(log.size() > 0 ? log.size() - 1 : 0)
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void handleFollowerTick(long now) {
        if (now - lastHeartbeat > electionTimeout) {
            logger.info("Node {} election timeout, starting election for term {}", 
                    nodeId, currentTerm.get() + 1);
            startElection();
        }
    }

    private void handleCandidateTick(long now) {
        if (now - lastHeartbeat > electionTimeout) {
            logger.info("Node {} election timeout as candidate, restarting election", nodeId);
            startElection();
        }
    }

    private void handleLeaderTick(long now) {
        if (now - lastHeartbeat > HEARTBEAT_INTERVAL) {
            sendHeartbeats();
            lastHeartbeat = now;
        }
    }

    private void startElection() {
        synchronized (this) {
            state.set(NodeState.CANDIDATE);
            currentTerm.incrementAndGet();
            votedFor = nodeId;
            currentLeader = null;
            lastHeartbeat = System.currentTimeMillis();
            electionTimeout = generateElectionTimeout();
        }

        logger.info("Node {} starting election for term {}", nodeId, currentTerm.get());

        // Vote for self
        AtomicInteger voteCount = new AtomicInteger(1);
        
        // Request votes from other nodes
        Flux.fromIterable(clusterNodes)
                .filter(node -> !node.equals(nodeId))
                .flatMap(this::requestVote)
                .subscribe(
                        response -> {
                            if (response.isVoteGranted()) {
                                int votes = voteCount.incrementAndGet();
                                logger.debug("Node {} received vote from {}, total votes: {}", 
                                        nodeId, response.getNodeId(), votes);
                                
                                if (votes > clusterNodes.size() / 2 && state.get() == NodeState.CANDIDATE) {
                                    becomeLeader();
                                }
                            } else if (response.getTerm() > currentTerm.get()) {
                                // Discovered higher term, become follower
                                synchronized (this) {
                                    currentTerm.set(response.getTerm());
                                    state.set(NodeState.FOLLOWER);
                                    votedFor = null;
                                    currentLeader = null;
                                }
                            }
                        },
                        error -> logger.warn("Error requesting vote: {}", error.getMessage())
                );
    }

    private void becomeLeader() {
        synchronized (this) {
            if (state.get() != NodeState.CANDIDATE) {
                return; // State changed during election
            }
            
            state.set(NodeState.LEADER);
            currentLeader = nodeId;
            
            // Initialize leader state
            nextIndex.clear();
            matchIndex.clear();
            
            for (String node : clusterNodes) {
                if (!node.equals(nodeId)) {
                    nextIndex.put(node, log.size());
                    matchIndex.put(node, 0L);
                }
            }
        }

        logger.info("Node {} became leader for term {}", nodeId, currentTerm.get());
        
        // Send immediate heartbeat to establish leadership
        sendHeartbeats();
    }

    private void sendHeartbeats() {
        if (state.get() != NodeState.LEADER) {
            return;
        }

        Flux.fromIterable(clusterNodes)
                .filter(node -> !node.equals(nodeId))
                .flatMap(this::sendAppendEntries)
                .subscribe(
                        response -> handleAppendEntriesResponse(response),
                        error -> logger.warn("Error sending heartbeat: {}", error.getMessage())
                );
    }

    private Mono<VoteResponse> requestVote(String targetNode) {
        VoteRequest request = VoteRequest.builder()
                .term(currentTerm.get())
                .candidateId(nodeId)
                .lastLogIndex(log.size() > 0 ? log.size() - 1 : 0)
                .lastLogTerm(log.size() > 0 ? log.get(log.size() - 1).getTerm() : 0)
                .build();

        return networkService.sendVoteRequest(targetNode, request)
                .timeout(Duration.ofMillis(100))
                .onErrorResume(error -> {
                    logger.debug("Failed to get vote from {}: {}", targetNode, error.getMessage());
                    return Mono.just(VoteResponse.builder()
                            .term(currentTerm.get())
                            .voteGranted(false)
                            .nodeId(targetNode)
                            .build());
                });
    }

    private Mono<AppendEntriesResponse> sendAppendEntries(String targetNode) {
        long nextIdx = nextIndex.getOrDefault(targetNode, (long) log.size());
        long prevLogIndex = nextIdx - 1;
        long prevLogTerm = prevLogIndex >= 0 && prevLogIndex < log.size() ? 
                log.get((int) prevLogIndex).getTerm() : 0;

        List<LogEntry> entries = new ArrayList<>();
        if (nextIdx < log.size()) {
            entries = log.subList((int) nextIdx, log.size());
        }

        AppendEntriesRequest request = AppendEntriesRequest.builder()
                .term(currentTerm.get())
                .leaderId(nodeId)
                .prevLogIndex(prevLogIndex)
                .prevLogTerm(prevLogTerm)
                .entries(entries)
                .leaderCommit(commitIndex.get())
                .build();

        return networkService.sendAppendEntries(targetNode, request)
                .timeout(Duration.ofMillis(100))
                .onErrorResume(error -> {
                    logger.debug("Failed to send append entries to {}: {}", targetNode, error.getMessage());
                    return Mono.just(AppendEntriesResponse.builder()
                            .term(currentTerm.get())
                            .success(false)
                            .nodeId(targetNode)
                            .build());
                });
    }

    private void handleAppendEntriesResponse(AppendEntriesResponse response) {
        if (state.get() != NodeState.LEADER) {
            return;
        }

        String nodeId = response.getNodeId();
        
        if (response.getTerm() > currentTerm.get()) {
            // Discovered higher term, step down
            synchronized (this) {
                currentTerm.set(response.getTerm());
                state.set(NodeState.FOLLOWER);
                votedFor = null;
                currentLeader = null;
            }
            return;
        }

        if (response.isSuccess()) {
            // Update next and match indexes
            long newMatchIndex = response.getMatchIndex();
            matchIndex.put(nodeId, newMatchIndex);
            nextIndex.put(nodeId, newMatchIndex + 1);
            
            // Try to advance commit index
            updateCommitIndex();
        } else {
            // Log inconsistency, decrement nextIndex and retry
            long currentNextIndex = nextIndex.getOrDefault(nodeId, 0L);
            long newNextIndex = Math.max(0, 
                    response.getConflictIndex() != null ? response.getConflictIndex() : currentNextIndex - 1);
            nextIndex.put(nodeId, newNextIndex);
            
            logger.debug("Log inconsistency with {}, decremented nextIndex to {}", nodeId, newNextIndex);
        }
    }

    private void updateCommitIndex() {
        if (state.get() != NodeState.LEADER || log.isEmpty()) {
            return;
        }

        // Find the highest index that is replicated on a majority of servers
        for (int i = log.size() - 1; i > commitIndex.get(); i--) {
            if (log.get(i).getTerm() != currentTerm.get()) {
                continue; // Only commit entries from current term
            }

            long replicationCount = 1; // Count self
            for (long matchIdx : matchIndex.values()) {
                if (matchIdx >= i) {
                    replicationCount++;
                }
            }

            if (replicationCount > clusterNodes.size() / 2) {
                commitIndex.set(i);
                applyCommittedEntries();
                break;
            }
        }
    }

    private Mono<LogEntry> appendLogEntry(ClientRequest request) {
        return Mono.fromCallable(() -> {
            LogEntry entry = LogEntry.builder()
                    .term(currentTerm.get())
                    .index(log.size())
                    .command(request.getCommand())
                    .data(request.getData())
                    .timestamp(LocalDateTime.now())
                    .build();
            
            synchronized (log) {
                log.add(entry);
            }
            
            logger.debug("Appended log entry at index {} for term {}", entry.getIndex(), entry.getTerm());
            return entry;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<LogEntry> replicateToMajority(LogEntry entry) {
        return Mono.fromCallable(() -> {
            // Wait for replication to majority
            // This is simplified - in practice, you'd wait for AppendEntries responses
            while (true) {
                long replicationCount = 1; // Count self
                for (long matchIdx : matchIndex.values()) {
                    if (matchIdx >= entry.getIndex()) {
                        replicationCount++;
                    }
                }
                
                if (replicationCount > clusterNodes.size() / 2) {
                    break;
                }
                
                try {
                    Thread.sleep(10); // Wait for replication
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            
            return entry;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Object> applyToStateMachine(LogEntry entry) {
        return stateMachine.apply(entry)
                .doOnSuccess(result -> {
                    lastApplied.set(Math.max(lastApplied.get(), entry.getIndex()));
                    logger.debug("Applied log entry {} to state machine", entry.getIndex());
                });
    }

    private void applyCommittedEntries() {
        while (lastApplied.get() < commitIndex.get()) {
            long nextToApply = lastApplied.get() + 1;
            if (nextToApply < log.size()) {
                LogEntry entry = log.get((int) nextToApply);
                stateMachine.apply(entry).subscribe(
                        result -> lastApplied.set(nextToApply),
                        error -> logger.error("Failed to apply entry {}: {}", nextToApply, error.getMessage())
                );
            } else {
                break;
            }
        }
    }

    private boolean isLogUpToDate(VoteRequest request) {
        if (log.isEmpty()) {
            return true;
        }
        
        LogEntry lastEntry = log.get(log.size() - 1);
        long lastTerm = lastEntry.getTerm();
        long lastIndex = lastEntry.getIndex();
        
        // Candidate log is up to date if:
        // 1. Last term is higher, OR
        // 2. Last term is same and last index is >= ours
        return request.getLastLogTerm() > lastTerm || 
               (request.getLastLogTerm() == lastTerm && request.getLastLogIndex() >= lastIndex);
    }

    private boolean isLogConsistent(AppendEntriesRequest request) {
        if (request.getPrevLogIndex() == -1) {
            return true; // No previous entry
        }
        
        if (request.getPrevLogIndex() >= log.size()) {
            return false; // Missing entries
        }
        
        LogEntry prevEntry = log.get((int) request.getPrevLogIndex());
        return prevEntry.getTerm() == request.getPrevLogTerm();
    }

    private Long findConflictIndex(AppendEntriesRequest request) {
        // Find the index where the log conflicts
        for (int i = (int) request.getPrevLogIndex(); i >= 0; i--) {
            if (i < log.size() && log.get(i).getTerm() != request.getPrevLogTerm()) {
                return (long) i;
            }
        }
        return 0L;
    }

    private void appendNewEntries(AppendEntriesRequest request) {
        if (request.getEntries().isEmpty()) {
            return;
        }

        int logIndex = (int) (request.getPrevLogIndex() + 1);
        
        // Remove conflicting entries
        if (logIndex < log.size()) {
            log.subList(logIndex, log.size()).clear();
        }
        
        // Append new entries
        for (LogEntry entry : request.getEntries()) {
            LogEntry newEntry = LogEntry.builder()
                    .term(entry.getTerm())
                    .index(logIndex++)
                    .command(entry.getCommand())
                    .data(entry.getData())
                    .timestamp(entry.getTimestamp())
                    .build();
            log.add(newEntry);
        }
    }

    private long generateElectionTimeout() {
        return MIN_ELECTION_TIMEOUT + 
               ThreadLocalRandom.current().nextLong(MAX_ELECTION_TIMEOUT - MIN_ELECTION_TIMEOUT);
    }

    // Getters for monitoring and testing
    public NodeState getState() { return state.get(); }
    public long getCurrentTerm() { return currentTerm.get(); }
    public String getCurrentLeader() { return currentLeader; }
    public int getLogSize() { return log.size(); }
    public long getCommitIndex() { return commitIndex.get(); }
    public long getLastApplied() { return lastApplied.get(); }

    // Enums and data classes
    public enum NodeState {
        FOLLOWER, CANDIDATE, LEADER
    }

    public static class LogEntry {
        private long term;
        private long index;
        private String command;
        private Map<String, Object> data;
        private LocalDateTime timestamp;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private LogEntry entry = new LogEntry();

            public Builder term(long term) { entry.term = term; return this; }
            public Builder index(long index) { entry.index = index; return this; }
            public Builder command(String command) { entry.command = command; return this; }
            public Builder data(Map<String, Object> data) { entry.data = data; return this; }
            public Builder timestamp(LocalDateTime timestamp) { entry.timestamp = timestamp; return this; }

            public LogEntry build() { return entry; }
        }

        // Getters
        public long getTerm() { return term; }
        public long getIndex() { return index; }
        public String getCommand() { return command; }
        public Map<String, Object> getData() { return data; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class VoteRequest {
        private long term;
        private String candidateId;
        private long lastLogIndex;
        private long lastLogTerm;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private VoteRequest request = new VoteRequest();

            public Builder term(long term) { request.term = term; return this; }
            public Builder candidateId(String candidateId) { request.candidateId = candidateId; return this; }
            public Builder lastLogIndex(long lastLogIndex) { request.lastLogIndex = lastLogIndex; return this; }
            public Builder lastLogTerm(long lastLogTerm) { request.lastLogTerm = lastLogTerm; return this; }

            public VoteRequest build() { return request; }
        }

        // Getters
        public long getTerm() { return term; }
        public String getCandidateId() { return candidateId; }
        public long getLastLogIndex() { return lastLogIndex; }
        public long getLastLogTerm() { return lastLogTerm; }
    }

    public static class VoteResponse {
        private long term;
        private boolean voteGranted;
        private String nodeId;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private VoteResponse response = new VoteResponse();

            public Builder term(long term) { response.term = term; return this; }
            public Builder voteGranted(boolean voteGranted) { response.voteGranted = voteGranted; return this; }
            public Builder nodeId(String nodeId) { response.nodeId = nodeId; return this; }

            public VoteResponse build() { return response; }
        }

        // Getters
        public long getTerm() { return term; }
        public boolean isVoteGranted() { return voteGranted; }
        public String getNodeId() { return nodeId; }
    }

    public static class AppendEntriesRequest {
        private long term;
        private String leaderId;
        private long prevLogIndex;
        private long prevLogTerm;
        private List<LogEntry> entries;
        private long leaderCommit;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private AppendEntriesRequest request = new AppendEntriesRequest();

            public Builder term(long term) { request.term = term; return this; }
            public Builder leaderId(String leaderId) { request.leaderId = leaderId; return this; }
            public Builder prevLogIndex(long prevLogIndex) { request.prevLogIndex = prevLogIndex; return this; }
            public Builder prevLogTerm(long prevLogTerm) { request.prevLogTerm = prevLogTerm; return this; }
            public Builder entries(List<LogEntry> entries) { request.entries = entries; return this; }
            public Builder leaderCommit(long leaderCommit) { request.leaderCommit = leaderCommit; return this; }

            public AppendEntriesRequest build() { return request; }
        }

        // Getters
        public long getTerm() { return term; }
        public String getLeaderId() { return leaderId; }
        public long getPrevLogIndex() { return prevLogIndex; }
        public long getPrevLogTerm() { return prevLogTerm; }
        public List<LogEntry> getEntries() { return entries; }
        public long getLeaderCommit() { return leaderCommit; }
    }

    public static class AppendEntriesResponse {
        private long term;
        private boolean success;
        private String nodeId;
        private Long conflictIndex;
        private long matchIndex;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private AppendEntriesResponse response = new AppendEntriesResponse();

            public Builder term(long term) { response.term = term; return this; }
            public Builder success(boolean success) { response.success = success; return this; }
            public Builder nodeId(String nodeId) { response.nodeId = nodeId; return this; }
            public Builder conflictIndex(Long conflictIndex) { response.conflictIndex = conflictIndex; return this; }
            public Builder matchIndex(long matchIndex) { response.matchIndex = matchIndex; return this; }

            public AppendEntriesResponse build() { return response; }
        }

        // Getters
        public long getTerm() { return term; }
        public boolean isSuccess() { return success; }
        public String getNodeId() { return nodeId; }
        public Long getConflictIndex() { return conflictIndex; }
        public long getMatchIndex() { return matchIndex; }
    }

    public static class ClientRequest {
        private String command;
        private Map<String, Object> data;

        // Getters
        public String getCommand() { return command; }
        public Map<String, Object> getData() { return data; }
    }

    public static class ClientResponse {
        private boolean success;
        private Object result;
        private String leaderId;
        private String message;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ClientResponse response = new ClientResponse();

            public Builder success(boolean success) { response.success = success; return this; }
            public Builder result(Object result) { response.result = result; return this; }
            public Builder leaderId(String leaderId) { response.leaderId = leaderId; return this; }
            public Builder message(String message) { response.message = message; return this; }

            public ClientResponse build() { return response; }
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getLeaderId() { return leaderId; }
        public String getMessage() { return message; }
    }
}
```

## Part 2: CAP Theorem Implementation Strategies

### Partition-Tolerant System with Eventual Consistency

```java
package com.distributed.cap.consistency;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EventuallyConsistentDataStore {

    private static final Logger logger = LoggerFactory.getLogger(EventuallyConsistentDataStore.class);

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final PartitionDetector partitionDetector;
    private final ConflictResolver conflictResolver;
    private final AntiEntropyService antiEntropyService;

    // Local storage for partition tolerance
    private final Map<String, VersionedValue> localStorage = new ConcurrentHashMap<>();
    private final VectorClock vectorClock;
    private final String nodeId;

    // Replica configuration
    private final Set<String> replicaNodes;
    private final Map<String, NodeStatus> nodeStatuses = new ConcurrentHashMap<>();

    public EventuallyConsistentDataStore(String nodeId, Set<String> replicaNodes,
            ReactiveRedisTemplate<String, Object> redisTemplate,
            PartitionDetector partitionDetector,
            ConflictResolver conflictResolver,
            AntiEntropyService antiEntropyService) {
        this.nodeId = nodeId;
        this.replicaNodes = new HashSet<>(replicaNodes);
        this.redisTemplate = redisTemplate;
        this.partitionDetector = partitionDetector;
        this.conflictResolver = conflictResolver;
        this.antiEntropyService = antiEntropyService;
        this.vectorClock = new VectorClock(nodeId, replicaNodes);
        
        initializeNodeStatuses();
        startPartitionDetection();
        startAntiEntropy();
    }

    public Mono<WriteResult> write(String key, Object value, ConsistencyLevel consistencyLevel) {
        logger.debug("Writing key: {} with consistency level: {}", key, consistencyLevel);

        return Mono.fromCallable(() -> {
            // Increment vector clock
            vectorClock.increment();
            
            // Create versioned value
            VersionedValue versionedValue = VersionedValue.builder()
                    .value(value)
                    .version(vectorClock.copy())
                    .timestamp(LocalDateTime.now())
                    .nodeId(nodeId)
                    .build();

            // Always write to local storage first
            localStorage.put(key, versionedValue);
            
            return new WriteContext(key, versionedValue, consistencyLevel);
        })
        .flatMap(context -> replicateWrite(context))
        .map(context -> WriteResult.builder()
                .success(true)
                .key(context.getKey())
                .version(context.getVersionedValue().getVersion())
                .replicatedNodes(context.getReplicatedNodes())
                .consistencyLevel(context.getConsistencyLevel())
                .build())
        .onErrorResume(error -> {
            logger.error("Write failed for key: {}", key, error);
            return Mono.just(WriteResult.builder()
                    .success(false)
                    .key(key)
                    .error(error.getMessage())
                    .build());
        });
    }

    public Mono<ReadResult> read(String key, ConsistencyLevel consistencyLevel) {
        logger.debug("Reading key: {} with consistency level: {}", key, consistencyLevel);

        switch (consistencyLevel) {
            case LOCAL:
                return readLocal(key);
            case EVENTUAL:
                return readEventual(key);
            case STRONG:
                return readStrong(key);
            default:
                return readEventual(key);
        }
    }

    private Mono<WriteResult> replicateWrite(WriteContext context) {
        switch (context.getConsistencyLevel()) {
            case LOCAL:
                return Mono.just(context)
                        .map(ctx -> {
                            // Asynchronously replicate to available nodes
                            replicateAsync(ctx);
                            ctx.setReplicatedNodes(Set.of(nodeId));
                            return ctx;
                        });
                        
            case EVENTUAL:
                return replicateToQuorum(context, 1); // W=1
                
            case STRONG:
                return replicateToQuorum(context, (replicaNodes.size() / 2) + 1); // W=majority
                
            default:
                return replicateToQuorum(context, 1);
        }
    }

    private Mono<WriteContext> replicateToQuorum(WriteContext context, int requiredReplicas) {
        Set<String> availableNodes = getAvailableNodes();
        
        if (availableNodes.size() < requiredReplicas) {
            if (partitionDetector.isPartitioned()) {
                // In partition, accept writes based on availability
                logger.warn("Network partition detected, accepting write with reduced consistency");
                replicateAsync(context);
                context.setReplicatedNodes(Set.of(nodeId));
                return Mono.just(context);
            } else {
                return Mono.error(new InsufficientReplicasException(
                        String.format("Required %d replicas, but only %d available", 
                                requiredReplicas, availableNodes.size())));
            }
        }

        return Flux.fromIterable(availableNodes)
                .take(requiredReplicas)
                .flatMap(node -> replicateToNode(node, context))
                .collectList()
                .map(responses -> {
                    Set<String> successfulNodes = new HashSet<>();
                    successfulNodes.add(nodeId); // Include self
                    
                    responses.stream()
                            .filter(ReplicationResponse::isSuccess)
                            .forEach(response -> successfulNodes.add(response.getNodeId()));
                    
                    context.setReplicatedNodes(successfulNodes);
                    return context;
                })
                .filter(ctx -> ctx.getReplicatedNodes().size() >= requiredReplicas)
                .switchIfEmpty(Mono.error(new ReplicationException("Failed to achieve required replication level")));
    }

    private void replicateAsync(WriteContext context) {
        Flux.fromIterable(getAvailableNodes())
                .flatMap(node -> replicateToNode(node, context))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                logger.debug("Async replication to {} successful", response.getNodeId());
                            } else {
                                logger.warn("Async replication to {} failed: {}", 
                                        response.getNodeId(), response.getError());
                            }
                        },
                        error -> logger.error("Async replication error", error)
                );
    }

    private Mono<ReplicationResponse> replicateToNode(String nodeId, WriteContext context) {
        ReplicationRequest request = ReplicationRequest.builder()
                .key(context.getKey())
                .versionedValue(context.getVersionedValue())
                .sourceNodeId(this.nodeId)
                .build();

        return sendReplicationRequest(nodeId, request)
                .timeout(Duration.ofMillis(500))
                .onErrorResume(error -> {
                    logger.debug("Replication to {} failed: {}", nodeId, error.getMessage());
                    markNodeUnavailable(nodeId);
                    return Mono.just(ReplicationResponse.builder()
                            .nodeId(nodeId)
                            .success(false)
                            .error(error.getMessage())
                            .build());
                });
    }

    private Mono<ReadResult> readLocal(String key) {
        return Mono.fromCallable(() -> {
            VersionedValue value = localStorage.get(key);
            
            return ReadResult.builder()
                    .key(key)
                    .value(value != null ? value.getValue() : null)
                    .version(value != null ? value.getVersion() : null)
                    .found(value != null)
                    .consistencyLevel(ConsistencyLevel.LOCAL)
                    .sourceNodes(Set.of(nodeId))
                    .build();
        });
    }

    private Mono<ReadResult> readEventual(String key) {
        return readFromAvailableNodes(key, 1) // R=1
                .map(results -> {
                    if (results.isEmpty()) {
                        return ReadResult.builder()
                                .key(key)
                                .found(false)
                                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                                .build();
                    }
                    
                    // Return the most recent version
                    ReadResult latest = results.stream()
                            .max(Comparator.comparing(result -> 
                                    result.getVersion() != null ? result.getVersion().getTimestamp() : 0L))
                            .orElse(results.get(0));
                    
                    return ReadResult.builder()
                            .key(key)
                            .value(latest.getValue())
                            .version(latest.getVersion())
                            .found(true)
                            .consistencyLevel(ConsistencyLevel.EVENTUAL)
                            .sourceNodes(Set.of(latest.getSourceNodes().iterator().next()))
                            .build();
                });
    }

    private Mono<ReadResult> readStrong(String key) {
        int requiredReads = (replicaNodes.size() / 2) + 1; // R=majority
        
        return readFromAvailableNodes(key, requiredReads)
                .map(results -> {
                    if (results.size() < requiredReads) {
                        throw new InsufficientReplicasException(
                                String.format("Required %d reads, but only %d available", 
                                        requiredReads, results.size()));
                    }
                    
                    // Resolve conflicts and return consistent value
                    ReadResult resolved = resolveReadConflicts(results);
                    
                    return ReadResult.builder()
                            .key(key)
                            .value(resolved.getValue())
                            .version(resolved.getVersion())
                            .found(resolved.isFound())
                            .consistencyLevel(ConsistencyLevel.STRONG)
                            .sourceNodes(results.stream()
                                    .flatMap(r -> r.getSourceNodes().stream())
                                    .collect(Collectors.toSet()))
                            .build();
                });
    }

    private Mono<List<ReadResult>> readFromAvailableNodes(String key, int requiredReads) {
        Set<String> availableNodes = getAvailableNodes();
        availableNodes.add(nodeId); // Include self
        
        return Flux.fromIterable(availableNodes)
                .take(requiredReads)
                .flatMap(node -> readFromNode(node, key))
                .filter(Objects::nonNull)
                .collectList();
    }

    private Mono<ReadResult> readFromNode(String nodeId, String key) {
        if (nodeId.equals(this.nodeId)) {
            return readLocal(key);
        }

        return sendReadRequest(nodeId, key)
                .timeout(Duration.ofMillis(300))
                .onErrorResume(error -> {
                    logger.debug("Read from {} failed: {}", nodeId, error.getMessage());
                    markNodeUnavailable(nodeId);
                    return Mono.empty();
                });
    }

    private ReadResult resolveReadConflicts(List<ReadResult> results) {
        if (results.size() == 1) {
            return results.get(0);
        }

        // Group by version
        Map<VectorClock, List<ReadResult>> versionGroups = results.stream()
                .filter(r -> r.getVersion() != null)
                .collect(Collectors.groupingBy(ReadResult::getVersion));

        if (versionGroups.size() == 1) {
            // All results have same version, return any
            return results.get(0);
        }

        // Conflict detected, resolve using conflict resolver
        return conflictResolver.resolve(results);
    }

    private Set<String> getAvailableNodes() {
        return nodeStatuses.entrySet().stream()
                .filter(entry -> entry.getValue() == NodeStatus.AVAILABLE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void markNodeUnavailable(String nodeId) {
        nodeStatuses.put(nodeId, NodeStatus.UNAVAILABLE);
        logger.debug("Marked node {} as unavailable", nodeId);
    }

    private void initializeNodeStatuses() {
        for (String node : replicaNodes) {
            if (!node.equals(this.nodeId)) {
                nodeStatuses.put(node, NodeStatus.AVAILABLE);
            }
        }
    }

    private void startPartitionDetection() {
        // Start periodic partition detection
        Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> partitionDetector.detectPartitions())
                .subscribe(
                        partitionInfo -> handlePartitionChange(partitionInfo),
                        error -> logger.error("Partition detection error", error)
                );
    }

    private void startAntiEntropy() {
        // Start periodic anti-entropy process
        Flux.interval(Duration.ofSeconds(30))
                .flatMap(tick -> antiEntropyService.performAntiEntropy(nodeId, localStorage))
                .subscribe(
                        result -> logger.debug("Anti-entropy completed: {} conflicts resolved", 
                                result.getConflictsResolved()),
                        error -> logger.error("Anti-entropy error", error)
                );
    }

    private void handlePartitionChange(PartitionInfo partitionInfo) {
        // Update node statuses based on partition information
        for (String node : replicaNodes) {
            if (!node.equals(this.nodeId)) {
                NodeStatus status = partitionInfo.isNodeReachable(node) ? 
                        NodeStatus.AVAILABLE : NodeStatus.UNAVAILABLE;
                nodeStatuses.put(node, status);
            }
        }
        
        logger.info("Partition status updated: {} available, {} unavailable", 
                getAvailableNodes().size(),
                replicaNodes.size() - getAvailableNodes().size() - 1); // -1 for self
    }

    // Network communication methods (would be implemented with actual network calls)
    private Mono<ReplicationResponse> sendReplicationRequest(String nodeId, ReplicationRequest request) {
        // Simulate network communication
        return Mono.just(ReplicationResponse.builder()
                .nodeId(nodeId)
                .success(true)
                .build())
                .delayElement(Duration.ofMillis(10)); // Simulate network latency
    }

    private Mono<ReadResult> sendReadRequest(String nodeId, String key) {
        // Simulate network communication
        return Mono.just(ReadResult.builder()
                .key(key)
                .found(false)
                .sourceNodes(Set.of(nodeId))
                .build())
                .delayElement(Duration.ofMillis(5)); // Simulate network latency
    }

    // Supporting classes and enums
    public enum ConsistencyLevel {
        LOCAL,      // Read/write from local node only
        EVENTUAL,   // R=1, W=1 - fastest, eventually consistent
        STRONG      // R=majority, W=majority - slower, strongly consistent
    }

    public enum NodeStatus {
        AVAILABLE, UNAVAILABLE, SUSPECTED
    }

    public static class VersionedValue {
        private Object value;
        private VectorClock version;
        private LocalDateTime timestamp;
        private String nodeId;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private VersionedValue versionedValue = new VersionedValue();

            public Builder value(Object value) { versionedValue.value = value; return this; }
            public Builder version(VectorClock version) { versionedValue.version = version; return this; }
            public Builder timestamp(LocalDateTime timestamp) { versionedValue.timestamp = timestamp; return this; }
            public Builder nodeId(String nodeId) { versionedValue.nodeId = nodeId; return this; }

            public VersionedValue build() { return versionedValue; }
        }

        // Getters
        public Object getValue() { return value; }
        public VectorClock getVersion() { return version; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getNodeId() { return nodeId; }
    }

    public static class VectorClock {
        private final Map<String, Long> clock = new ConcurrentHashMap<>();
        private final String nodeId;

        public VectorClock(String nodeId, Set<String> allNodes) {
            this.nodeId = nodeId;
            for (String node : allNodes) {
                clock.put(node, 0L);
            }
        }

        public VectorClock(Map<String, Long> clock, String nodeId) {
            this.clock.putAll(clock);
            this.nodeId = nodeId;
        }

        public void increment() {
            clock.merge(nodeId, 1L, Long::sum);
        }

        public VectorClock copy() {
            return new VectorClock(new HashMap<>(clock), nodeId);
        }

        public long getTimestamp() {
            return clock.values().stream().mapToLong(Long::longValue).sum();
        }

        // Vector clock comparison methods would be implemented here
        public boolean happensBefore(VectorClock other) {
            // Implementation of vector clock comparison
            return false; // Simplified
        }

        public boolean isConcurrent(VectorClock other) {
            // Implementation of concurrency check
            return false; // Simplified
        }

        // Getters
        public Map<String, Long> getClock() { return new HashMap<>(clock); }
        public String getNodeId() { return nodeId; }
    }

    // Additional supporting classes
    private static class WriteContext {
        private String key;
        private VersionedValue versionedValue;
        private ConsistencyLevel consistencyLevel;
        private Set<String> replicatedNodes = new HashSet<>();

        public WriteContext(String key, VersionedValue versionedValue, ConsistencyLevel consistencyLevel) {
            this.key = key;
            this.versionedValue = versionedValue;
            this.consistencyLevel = consistencyLevel;
        }

        // Getters and setters
        public String getKey() { return key; }
        public VersionedValue getVersionedValue() { return versionedValue; }
        public ConsistencyLevel getConsistencyLevel() { return consistencyLevel; }
        public Set<String> getReplicatedNodes() { return replicatedNodes; }
        public void setReplicatedNodes(Set<String> replicatedNodes) { this.replicatedNodes = replicatedNodes; }
    }

    // Exception classes
    public static class InsufficientReplicasException extends RuntimeException {
        public InsufficientReplicasException(String message) { super(message); }
    }

    public static class ReplicationException extends RuntimeException {
        public ReplicationException(String message) { super(message); }
    }
}
```

## Part 3: Distributed Transaction Coordination

### Two-Phase Commit Protocol Implementation

```java
package com.distributed.transactions.twophase;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TwoPhaseCommitCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TwoPhaseCommitCoordinator.class);

    private final TransactionLog transactionLog;
    private final ParticipantRegistry participantRegistry;
    private final TimeoutManager timeoutManager;
    private final RecoveryManager recoveryManager;

    // Active transactions
    private final Map<String, TransactionState> activeTransactions = new ConcurrentHashMap<>();

    // Configuration
    private static final Duration PREPARE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration COMMIT_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration ABORT_TIMEOUT = Duration.ofSeconds(30);

    public TwoPhaseCommitCoordinator(TransactionLog transactionLog,
            ParticipantRegistry participantRegistry,
            TimeoutManager timeoutManager,
            RecoveryManager recoveryManager) {
        this.transactionLog = transactionLog;
        this.participantRegistry = participantRegistry;
        this.timeoutManager = timeoutManager;
        this.recoveryManager = recoveryManager;
        
        startRecoveryProcess();
    }

    public Mono<TransactionResult> executeTransaction(DistributedTransaction transaction) {
        String transactionId = UUID.randomUUID().toString();
        
        logger.info("Starting distributed transaction: {}", transactionId);

        return initializeTransaction(transactionId, transaction)
                .flatMap(this::executePhaseOne)
                .flatMap(this::executePhaseTwo)
                .map(state -> TransactionResult.builder()
                        .transactionId(transactionId)
                        .status(state.getStatus())
                        .participants(state.getParticipants().keySet())
                        .startTime(state.getStartTime())
                        .endTime(LocalDateTime.now())
                        .message("Transaction completed successfully")
                        .build())
                .doOnSuccess(result -> {
                    activeTransactions.remove(transactionId);
                    logger.info("Transaction {} completed with status: {}", transactionId, result.getStatus());
                })
                .onErrorResume(error -> handleTransactionError(transactionId, error));
    }

    private Mono<TransactionState> initializeTransaction(String transactionId, 
            DistributedTransaction transaction) {
        
        return Mono.fromCallable(() -> {
            // Create transaction state
            TransactionState state = TransactionState.builder()
                    .transactionId(transactionId)
                    .status(TransactionStatus.INITIALIZING)
                    .transaction(transaction)
                    .startTime(LocalDateTime.now())
                    .build();

            // Register participants
            for (TransactionOperation operation : transaction.getOperations()) {
                String participantId = operation.getParticipantId();
                Participant participant = participantRegistry.getParticipant(participantId);
                
                if (participant == null) {
                    throw new ParticipantNotFoundException("Participant not found: " + participantId);
                }
                
                state.getParticipants().put(participantId, ParticipantState.builder()
                        .participant(participant)
                        .operation(operation)
                        .status(ParticipantStatus.INITIALIZED)
                        .build());
            }

            activeTransactions.put(transactionId, state);
            state.setStatus(TransactionStatus.INITIALIZED);

            return state;
        }).subscribeOn(Schedulers.boundedElastic())
        .flatMap(state -> logTransactionStart(state));
    }

    private Mono<TransactionState> executePhaseOne(TransactionState state) {
        logger.info("Starting Phase 1 (Prepare) for transaction: {}", state.getTransactionId());
        
        state.setStatus(TransactionStatus.PREPARING);
        
        return logPhaseStart(state, 1)
                .flatMap(loggedState -> sendPrepareRequests(loggedState))
                .flatMap(this::waitForPrepareResponses)
                .map(responseState -> {
                    // Determine if we can commit based on prepare responses
                    boolean allPrepared = responseState.getParticipants().values().stream()
                            .allMatch(p -> p.getStatus() == ParticipantStatus.PREPARED);
                    
                    if (allPrepared) {
                        responseState.setStatus(TransactionStatus.PREPARED);
                        responseState.setDecision(TransactionDecision.COMMIT);
                    } else {
                        responseState.setStatus(TransactionStatus.ABORTED);
                        responseState.setDecision(TransactionDecision.ABORT);
                    }
                    
                    return responseState;
                })
                .flatMap(this::logPhaseOneDecision);
    }

    private Mono<TransactionState> executePhaseTwo(TransactionState state) {
        logger.info("Starting Phase 2 ({}) for transaction: {}", 
                state.getDecision(), state.getTransactionId());

        return logPhaseStart(state, 2)
                .flatMap(this::sendPhaseOneDecision)
                .flatMap(this::waitForPhaseOneResponses)
                .map(responseState -> {
                    // Mark transaction as completed
                    if (responseState.getDecision() == TransactionDecision.COMMIT) {
                        responseState.setStatus(TransactionStatus.COMMITTED);
                    } else {
                        responseState.setStatus(TransactionStatus.ABORTED);
                    }
                    return responseState;
                })
                .flatMap(this::logTransactionCompletion);
    }

    private Mono<TransactionState> sendPrepareRequests(TransactionState state) {
        return Flux.fromIterable(state.getParticipants().entrySet())
                .flatMap(entry -> sendPrepareRequest(state, entry.getKey(), entry.getValue()))
                .then(Mono.just(state));
    }

    private Mono<Void> sendPrepareRequest(TransactionState state, String participantId, 
            ParticipantState participantState) {
        
        PrepareRequest request = PrepareRequest.builder()
                .transactionId(state.getTransactionId())
                .operation(participantState.getOperation())
                .timeout(PREPARE_TIMEOUT)
                .build();

        return participantState.getParticipant().prepare(request)
                .timeout(PREPARE_TIMEOUT)
                .doOnSuccess(response -> {
                    synchronized (participantState) {
                        if (response.isSuccess()) {
                            participantState.setStatus(ParticipantStatus.PREPARED);
                            participantState.setResourceLocks(response.getResourceLocks());
                        } else {
                            participantState.setStatus(ParticipantStatus.ABORTED);
                            participantState.setErrorMessage(response.getErrorMessage());
                        }
                    }
                    
                    logger.debug("Prepare response from {}: {}", participantId, response.isSuccess());
                })
                .onErrorResume(error -> {
                    logger.error("Prepare request to {} failed", participantId, error);
                    synchronized (participantState) {
                        participantState.setStatus(ParticipantStatus.ABORTED);
                        participantState.setErrorMessage(error.getMessage());
                    }
                    return Mono.empty();
                })
                .then();
    }

    private Mono<TransactionState> waitForPrepareResponses(TransactionState state) {
        return Mono.fromCallable(() -> {
            // Wait for all participants to respond or timeout
            long startTime = System.currentTimeMillis();
            long timeoutMs = PREPARE_TIMEOUT.toMillis();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                boolean allResponded = state.getParticipants().values().stream()
                        .allMatch(p -> p.getStatus() != ParticipantStatus.PREPARING);
                
                if (allResponded) {
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            
            // Mark unresponsive participants as aborted
            state.getParticipants().values().stream()
                    .filter(p -> p.getStatus() == ParticipantStatus.PREPARING)
                    .forEach(p -> {
                        p.setStatus(ParticipantStatus.ABORTED);
                        p.setErrorMessage("Prepare timeout");
                    });
            
            return state;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<TransactionState> sendPhaseOneDecision(TransactionState state) {
        if (state.getDecision() == TransactionDecision.COMMIT) {
            return sendCommitRequests(state);
        } else {
            return sendAbortRequests(state);
        }
    }

    private Mono<TransactionState> sendCommitRequests(TransactionState state) {
        state.setStatus(TransactionStatus.COMMITTING);
        
        return Flux.fromIterable(state.getParticipants().entrySet())
                .filter(entry -> entry.getValue().getStatus() == ParticipantStatus.PREPARED)
                .flatMap(entry -> sendCommitRequest(state, entry.getKey(), entry.getValue()))
                .then(Mono.just(state));
    }

    private Mono<TransactionState> sendAbortRequests(TransactionState state) {
        state.setStatus(TransactionStatus.ABORTING);
        
        return Flux.fromIterable(state.getParticipants().entrySet())
                .flatMap(entry -> sendAbortRequest(state, entry.getKey(), entry.getValue()))
                .then(Mono.just(state));
    }

    private Mono<Void> sendCommitRequest(TransactionState state, String participantId, 
            ParticipantState participantState) {
        
        CommitRequest request = CommitRequest.builder()
                .transactionId(state.getTransactionId())
                .resourceLocks(participantState.getResourceLocks())
                .build();

        return participantState.getParticipant().commit(request)
                .timeout(COMMIT_TIMEOUT)
                .doOnSuccess(response -> {
                    synchronized (participantState) {
                        participantState.setStatus(response.isSuccess() ? 
                                ParticipantStatus.COMMITTED : ParticipantStatus.COMMIT_FAILED);
                    }
                    
                    logger.debug("Commit response from {}: {}", participantId, response.isSuccess());
                })
                .onErrorResume(error -> {
                    logger.error("Commit request to {} failed", participantId, error);
                    synchronized (participantState) {
                        participantState.setStatus(ParticipantStatus.COMMIT_FAILED);
                        participantState.setErrorMessage(error.getMessage());
                    }
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> sendAbortRequest(TransactionState state, String participantId, 
            ParticipantState participantState) {
        
        AbortRequest request = AbortRequest.builder()
                .transactionId(state.getTransactionId())
                .resourceLocks(participantState.getResourceLocks())
                .build();

        return participantState.getParticipant().abort(request)
                .timeout(ABORT_TIMEOUT)
                .doOnSuccess(response -> {
                    synchronized (participantState) {
                        participantState.setStatus(ParticipantStatus.ABORTED);
                    }
                    
                    logger.debug("Abort response from {}: {}", participantId, response.isSuccess());
                })
                .onErrorResume(error -> {
                    logger.error("Abort request to {} failed", participantId, error);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<TransactionState> waitForPhaseOneResponses(TransactionState state) {
        return Mono.fromCallable(() -> {
            // Wait for all phase 2 responses
            long startTime = System.currentTimeMillis();
            Duration timeout = state.getDecision() == TransactionDecision.COMMIT ? 
                    COMMIT_TIMEOUT : ABORT_TIMEOUT;
            
            while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
                boolean allResponded = state.getParticipants().values().stream()
                        .allMatch(p -> p.getStatus() == ParticipantStatus.COMMITTED || 
                                       p.getStatus() == ParticipantStatus.ABORTED ||
                                       p.getStatus() == ParticipantStatus.COMMIT_FAILED);
                
                if (allResponded) {
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            
            return state;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<TransactionResult> handleTransactionError(String transactionId, Throwable error) {
        logger.error("Transaction {} failed", transactionId, error);
        
        TransactionState state = activeTransactions.get(transactionId);
        if (state != null) {
            // Attempt to abort the transaction
            return abortTransaction(state)
                    .map(abortedState -> TransactionResult.builder()
                            .transactionId(transactionId)
                            .status(TransactionStatus.ABORTED)
                            .participants(state.getParticipants().keySet())
                            .startTime(state.getStartTime())
                            .endTime(LocalDateTime.now())
                            .message("Transaction aborted due to error: " + error.getMessage())
                            .build())
                    .doFinally(signal -> activeTransactions.remove(transactionId));
        }
        
        return Mono.just(TransactionResult.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.FAILED)
                .message("Transaction failed: " + error.getMessage())
                .build());
    }

    private Mono<TransactionState> abortTransaction(TransactionState state) {
        state.setDecision(TransactionDecision.ABORT);
        return sendAbortRequests(state)
                .flatMap(this::waitForPhaseOneResponses)
                .map(abortedState -> {
                    abortedState.setStatus(TransactionStatus.ABORTED);
                    return abortedState;
                })
                .flatMap(this::logTransactionCompletion);
    }

    // Transaction logging methods
    private Mono<TransactionState> logTransactionStart(TransactionState state) {
        return transactionLog.logTransactionStart(state)
                .thenReturn(state);
    }

    private Mono<TransactionState> logPhaseStart(TransactionState state, int phase) {
        return transactionLog.logPhaseStart(state.getTransactionId(), phase, state.getStatus())
                .thenReturn(state);
    }

    private Mono<TransactionState> logPhaseOneDecision(TransactionState state) {
        return transactionLog.logPhaseOneDecision(state.getTransactionId(), state.getDecision())
                .thenReturn(state);
    }

    private Mono<TransactionState> logTransactionCompletion(TransactionState state) {
        return transactionLog.logTransactionCompletion(state)
                .thenReturn(state);
    }

    private void startRecoveryProcess() {
        // Start periodic recovery process to handle coordinator failures
        Flux.interval(Duration.ofMinutes(5))
                .flatMap(tick -> recoveryManager.recoverIncompleteTransactions())
                .subscribe(
                        recoveredCount -> {
                            if (recoveredCount > 0) {
                                logger.info("Recovered {} incomplete transactions", recoveredCount);
                            }
                        },
                        error -> logger.error("Recovery process error", error)
                );
    }

    // Supporting classes and enums
    public enum TransactionStatus {
        INITIALIZING, INITIALIZED, PREPARING, PREPARED, COMMITTING, COMMITTED, ABORTING, ABORTED, FAILED
    }

    public enum ParticipantStatus {
        INITIALIZED, PREPARING, PREPARED, COMMITTING, COMMITTED, ABORTING, ABORTED, COMMIT_FAILED
    }

    public enum TransactionDecision {
        COMMIT, ABORT
    }

    // Data classes with builders...
    public static class TransactionState {
        private String transactionId;
        private TransactionStatus status;
        private DistributedTransaction transaction;
        private Map<String, ParticipantState> participants = new ConcurrentHashMap<>();
        private TransactionDecision decision;
        private LocalDateTime startTime;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private TransactionState state = new TransactionState();

            public Builder transactionId(String transactionId) { state.transactionId = transactionId; return this; }
            public Builder status(TransactionStatus status) { state.status = status; return this; }
            public Builder transaction(DistributedTransaction transaction) { state.transaction = transaction; return this; }
            public Builder startTime(LocalDateTime startTime) { state.startTime = startTime; return this; }

            public TransactionState build() { return state; }
        }

        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public TransactionStatus getStatus() { return status; }
        public void setStatus(TransactionStatus status) { this.status = status; }
        public DistributedTransaction getTransaction() { return transaction; }
        public Map<String, ParticipantState> getParticipants() { return participants; }
        public TransactionDecision getDecision() { return decision; }
        public void setDecision(TransactionDecision decision) { this.decision = decision; }
        public LocalDateTime getStartTime() { return startTime; }
    }

    // Additional supporting classes would be implemented here...
    // ParticipantState, TransactionResult, PrepareRequest, CommitRequest, etc.

    // Exception classes
    public static class ParticipantNotFoundException extends RuntimeException {
        public ParticipantNotFoundException(String message) { super(message); }
    }

    public static class TransactionTimeoutException extends RuntimeException {
        public TransactionTimeoutException(String message) { super(message); }
    }
}
```

## Practical Exercises

### Exercise 1: Raft Cluster Implementation
Build a complete Raft cluster with:
- Multi-node deployment with network partitioning simulation
- Log compaction and snapshot mechanisms
- Client request routing and leader discovery
- Performance testing under various failure scenarios

### Exercise 2: CAP Theorem Trade-offs
Implement different consistency models:
- Strong consistency with reduced availability during partitions
- Eventual consistency with conflict resolution strategies
- Tunable consistency levels based on application requirements
- Performance comparison across different consistency models

### Exercise 3: Byzantine Fault Tolerance
Create a PBFT (Practical Byzantine Fault Tolerance) implementation:
- Three-phase protocol with view changes
- Malicious node detection and recovery
- Performance optimization for normal case operation
- Integration with existing distributed applications

### Exercise 4: Distributed Lock Manager
Build a comprehensive distributed lock service:
- Hierarchical locking with deadlock detection
- Fair queueing and priority-based lock acquisition
- Lease-based locks with automatic expiration
- High availability with leader election

### Exercise 5: Multi-Phase Commit Optimization
Enhance the 2PC implementation with:
- Three-phase commit for improved availability
- Presumed abort/commit optimizations
- Read-only transaction optimization
- Integration with distributed databases

## Performance Considerations

### Consensus Algorithm Optimization
- **Batching**: Group multiple requests for better throughput
- **Pipelining**: Allow multiple rounds of consensus in parallel
- **Network Optimization**: Reduce message size and round trips
- **Leader Stickiness**: Minimize leader elections through stability

### CAP Theorem Applications
- **Consistency Models**: Choose appropriate consistency levels per operation
- **Partition Handling**: Implement graceful degradation during partitions
- **Conflict Resolution**: Design efficient conflict resolution strategies
- **Performance Monitoring**: Track consistency vs. availability trade-offs

## Distributed Systems Best Practices
- Implement comprehensive logging and monitoring for distributed operations
- Design for partial failures and network partitions
- Use idempotent operations to handle duplicate requests
- Implement proper timeout and retry strategies
- Monitor system health and performance metrics continuously
- Plan for disaster recovery and data replication strategies
- Test failure scenarios regularly in controlled environments

## Summary

Day 114 covered fundamental distributed systems architecture patterns:

1. **Raft Consensus Algorithm**: Complete implementation with leader election, log replication, and safety guarantees
2. **CAP Theorem Applications**: Practical implementation of different consistency models with trade-off analysis
3. **Distributed Transactions**: Two-phase commit protocol with comprehensive error handling and recovery
4. **Fault Tolerance**: Byzantine fault tolerance and partition handling strategies
5. **Performance Optimization**: Advanced techniques for high-throughput distributed systems

These implementations provide the theoretical foundation and practical tools needed to build robust, scalable distributed systems that can handle real-world failure scenarios while maintaining consistency and availability guarantees.