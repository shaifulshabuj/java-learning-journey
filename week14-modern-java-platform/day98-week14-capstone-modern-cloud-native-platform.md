# Day 98: Week 14 Capstone - Modern Cloud-Native Enterprise Platform

## Project Overview

Build a comprehensive cloud-native enterprise platform that integrates all modern Java technologies from Week 14:
- **Virtual Threads (Project Loom)** for high-concurrency request handling
- **Modern Java Features** (Records, Pattern Matching, Sealed Classes) for type-safe domain modeling
- **GraalVM Native Images** for fast startup and low memory footprint
- **Advanced Observability** with OpenTelemetry for comprehensive monitoring
- **GraphQL APIs** for flexible and efficient data access
- **Machine Learning Integration** for intelligent features and analytics

## System Architecture

```java
package com.javajourney.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Modern Cloud-Native Enterprise Platform
 * 
 * Features:
 * - Virtual threads for high-concurrency
 * - Modern Java language features
 * - GraalVM native image support
 * - Comprehensive observability
 * - GraphQL APIs
 * - ML-powered intelligent features
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class ModernPlatformApplication {
    
    static {
        // Configure system properties for optimal performance
        System.setProperty("spring.threads.virtual.enabled", "true");
        System.setProperty("spring.aot.enabled", "true");
        System.setProperty("spring.native.enabled", "true");
        
        // OpenTelemetry configuration
        System.setProperty("otel.service.name", "modern-platform");
        System.setProperty("otel.service.version", "1.0.0");
        System.setProperty("otel.resource.attributes", 
            "environment=production,team=platform,region=us-east-1");
        
        // GraalVM optimization
        System.setProperty("org.graalvm.nativeimage.imagecode", "runtime");
    }
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ModernPlatformApplication.class);
        
        // Configure for cloud-native deployment
        app.setAdditionalProfiles("cloud-native", "observability", "ml-enabled");
        
        // Set banner mode for clean startup
        app.setBannerMode(org.springframework.boot.Banner.Mode.LOG);
        
        app.run(args);
    }
}
```

## 1. Domain Model with Modern Java Features

### Core Domain Entities

```java
package com.javajourney.platform.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Modern user entity using Java Records with validation
 */
public record User(
    String id,
    String username,
    String email,
    UserProfile profile,
    UserStatus status,
    UserPreferences preferences,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt
) {
    public User {
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(profile, "Profile cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(createdAt, "Created date cannot be null");
        
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
    
    public boolean isRecent() {
        return createdAt.isAfter(LocalDateTime.now().minusDays(30));
    }
    
    public String getDisplayName() {
        return profile.firstName() + " " + profile.lastName();
    }
    
    // Factory methods
    public static User newUser(String username, String email, UserProfile profile) {
        return new User(
            java.util.UUID.randomUUID().toString(),
            username,
            email,
            profile,
            UserStatus.PENDING_VERIFICATION,
            UserPreferences.defaultPreferences(),
            LocalDateTime.now(),
            null
        );
    }
}

public record UserProfile(
    String firstName,
    String lastName,
    String bio,
    String avatar,
    String location,
    String timezone,
    LocalDateTime birthDate
) {
    public UserProfile {
        Objects.requireNonNull(firstName, "First name cannot be null");
        Objects.requireNonNull(lastName, "Last name cannot be null");
    }
    
    public int getAge() {
        if (birthDate == null) return 0;
        return java.time.Period.between(birthDate.toLocalDate(), 
                                      java.time.LocalDate.now()).getYears();
    }
}

public record UserPreferences(
    String theme,
    String language,
    boolean emailNotifications,
    boolean pushNotifications,
    Map<String, Object> customSettings
) {
    public static UserPreferences defaultPreferences() {
        return new UserPreferences(
            "light",
            "en",
            true,
            true,
            Map.of()
        );
    }
}

/**
 * Sealed class for user status with pattern matching support
 */
public sealed interface UserStatus permits ActiveStatus, InactiveStatus, SuspendedStatus, PendingStatus {
    String getDisplayName();
    boolean canLogin();
}

public record ActiveStatus() implements UserStatus {
    @Override
    public String getDisplayName() { return "Active"; }
    
    @Override
    public boolean canLogin() { return true; }
}

public record InactiveStatus(String reason) implements UserStatus {
    @Override
    public String getDisplayName() { return "Inactive"; }
    
    @Override
    public boolean canLogin() { return false; }
}

public record SuspendedStatus(String reason, LocalDateTime until) implements UserStatus {
    @Override
    public String getDisplayName() { return "Suspended"; }
    
    @Override
    public boolean canLogin() { 
        return until != null && LocalDateTime.now().isAfter(until); 
    }
}

public record PendingStatus(String verificationType) implements UserStatus {
    @Override
    public String getDisplayName() { return "Pending Verification"; }
    
    @Override
    public boolean canLogin() { return false; }
}

// Convenience constants
public static final UserStatus ACTIVE = new ActiveStatus();
public static final UserStatus INACTIVE = new InactiveStatus("User deactivated account");
public static final UserStatus PENDING_VERIFICATION = new PendingStatus("email");

/**
 * Project entity with modern Java features
 */
public record Project(
    String id,
    String name,
    String description,
    String ownerId,
    ProjectStatus status,
    ProjectSettings settings,
    List<String> collaboratorIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public Project {
        Objects.requireNonNull(id, "Project ID cannot be null");
        Objects.requireNonNull(name, "Project name cannot be null");
        Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(settings, "Settings cannot be null");
        Objects.requireNonNull(collaboratorIds, "Collaborators cannot be null");
        Objects.requireNonNull(createdAt, "Created date cannot be null");
        Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        
        // Make defensive copy
        collaboratorIds = List.copyOf(collaboratorIds);
        
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name cannot be blank");
        }
    }
    
    public boolean isOwner(String userId) {
        return ownerId.equals(userId);
    }
    
    public boolean isCollaborator(String userId) {
        return collaboratorIds.contains(userId);
    }
    
    public boolean hasAccess(String userId) {
        return isOwner(userId) || isCollaborator(userId);
    }
    
    public int getCollaboratorCount() {
        return collaboratorIds.size();
    }
}

public sealed interface ProjectStatus permits ActiveProject, ArchivedProject, DeletedProject {
    String getDisplayName();
    boolean isActive();
}

public record ActiveProject() implements ProjectStatus {
    @Override
    public String getDisplayName() { return "Active"; }
    
    @Override
    public boolean isActive() { return true; }
}

public record ArchivedProject(LocalDateTime archivedAt, String reason) implements ProjectStatus {
    @Override
    public String getDisplayName() { return "Archived"; }
    
    @Override
    public boolean isActive() { return false; }
}

public record DeletedProject(LocalDateTime deletedAt, String reason) implements ProjectStatus {
    @Override
    public String getDisplayName() { return "Deleted"; }
    
    @Override
    public boolean isActive() { return false; }
}

public record ProjectSettings(
    boolean isPublic,
    boolean allowCollaborators,
    String defaultBranch,
    Map<String, Object> customSettings
) {
    public static ProjectSettings defaultSettings() {
        return new ProjectSettings(
            false,
            true,
            "main",
            Map.of()
        );
    }
}

/**
 * Analytics event for ML processing
 */
public sealed interface AnalyticsEvent permits UserEvent, ProjectEvent, SystemEvent {
    String eventId();
    String userId();
    LocalDateTime timestamp();
    Map<String, Object> properties();
}

public record UserEvent(
    String eventId,
    String userId,
    String eventType,
    LocalDateTime timestamp,
    Map<String, Object> properties
) implements AnalyticsEvent {}

public record ProjectEvent(
    String eventId,
    String userId,
    String projectId,
    String eventType,
    LocalDateTime timestamp,
    Map<String, Object> properties
) implements AnalyticsEvent {}

public record SystemEvent(
    String eventId,
    String userId,
    String component,
    String eventType,
    LocalDateTime timestamp,
    Map<String, Object> properties
) implements AnalyticsEvent {}
```

### Domain Services with Pattern Matching

```java
package com.javajourney.platform.service;

import com.javajourney.platform.domain.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final Tracer tracer;
    private final AnalyticsService analyticsService;
    
    public UserService(UserRepository userRepository, Tracer tracer, AnalyticsService analyticsService) {
        this.userRepository = userRepository;
        this.tracer = tracer;
        this.analyticsService = analyticsService;
    }
    
    /**
     * Process user status change using pattern matching
     */
    public CompletableFuture<User> updateUserStatus(String userId, UserStatus newStatus) {
        Span span = tracer.spanBuilder("user.updateStatus")
            .setAttribute("user.id", userId)
            .setAttribute("new.status", newStatus.getDisplayName())
            .startSpan();
        
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = span.makeCurrent()) {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                
                // Pattern matching for status transitions
                String transitionResult = switch (user.status()) {
                    case ActiveStatus active -> switch (newStatus) {
                        case InactiveStatus inactive -> {
                            span.setAttribute("transition", "active_to_inactive");
                            yield "User deactivated successfully";
                        }
                        case SuspendedStatus suspended -> {
                            span.setAttribute("transition", "active_to_suspended");
                            span.setAttribute("suspension.reason", suspended.reason());
                            yield "User suspended: " + suspended.reason();
                        }
                        case ActiveStatus ignored -> {
                            span.setAttribute("transition", "no_change");
                            yield "User already active";
                        }
                        case PendingStatus pending -> {
                            span.setAttribute("transition", "active_to_pending");
                            yield "User status changed to pending verification";
                        }
                    };
                    
                    case InactiveStatus inactive -> switch (newStatus) {
                        case ActiveStatus active -> {
                            span.setAttribute("transition", "inactive_to_active");
                            yield "User reactivated successfully";
                        }
                        case InactiveStatus ignored -> {
                            span.setAttribute("transition", "no_change");
                            yield "User already inactive";
                        }
                        default -> {
                            span.setAttribute("transition", "invalid");
                            yield "Cannot transition from inactive to " + newStatus.getDisplayName();
                        }
                    };
                    
                    case SuspendedStatus suspended -> switch (newStatus) {
                        case ActiveStatus active when suspended.until() == null || 
                                                    LocalDateTime.now().isAfter(suspended.until()) -> {
                            span.setAttribute("transition", "suspended_to_active");
                            yield "User suspension lifted";
                        }
                        case SuspendedStatus ignored -> {
                            span.setAttribute("transition", "no_change");
                            yield "User already suspended";
                        }
                        default -> {
                            span.setAttribute("transition", "invalid");
                            yield "Cannot change status while suspended";
                        }
                    };
                    
                    case PendingStatus pending -> switch (newStatus) {
                        case ActiveStatus active -> {
                            span.setAttribute("transition", "pending_to_active");
                            span.setAttribute("verification.type", pending.verificationType());
                            yield "User verification completed";
                        }
                        case InactiveStatus inactive -> {
                            span.setAttribute("transition", "pending_to_inactive");
                            yield "User verification failed";
                        }
                        default -> {
                            span.setAttribute("transition", "invalid");
                            yield "Invalid transition from pending status";
                        }
                    };
                };
                
                // Update user with new status
                User updatedUser = new User(
                    user.id(),
                    user.username(),
                    user.email(),
                    user.profile(),
                    newStatus,
                    user.preferences(),
                    user.createdAt(),
                    user.lastLoginAt()
                );
                
                // Save to repository
                User savedUser = userRepository.save(updatedUser);
                
                // Record analytics event
                analyticsService.recordEvent(new UserEvent(
                    java.util.UUID.randomUUID().toString(),
                    userId,
                    "status_changed",
                    LocalDateTime.now(),
                    Map.of(
                        "old_status", user.status().getDisplayName(),
                        "new_status", newStatus.getDisplayName(),
                        "transition_result", transitionResult
                    )
                ));
                
                span.setAttribute("transition.result", transitionResult);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                
                return savedUser;
                
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                throw new RuntimeException("Failed to update user status", e);
            } finally {
                span.end();
            }
        });
    }
    
    /**
     * Authenticate user with pattern matching
     */
    public CompletableFuture<AuthenticationResult> authenticateUser(String username, String password) {
        Span span = tracer.spanBuilder("user.authenticate")
            .setAttribute("username", username)
            .startSpan();
        
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = span.makeCurrent()) {
                User user = userRepository.findByUsername(username)
                    .orElse(null);
                
                if (user == null) {
                    span.setAttribute("auth.result", "user_not_found");
                    return new AuthenticationResult(false, "User not found", null);
                }
                
                // Verify password (simplified)
                boolean passwordValid = verifyPassword(password, user);
                if (!passwordValid) {
                    span.setAttribute("auth.result", "invalid_password");
                    return new AuthenticationResult(false, "Invalid password", null);
                }
                
                // Check user status using pattern matching
                return switch (user.status()) {
                    case ActiveStatus active -> {
                        span.setAttribute("auth.result", "success");
                        
                        // Update last login
                        User updatedUser = new User(
                            user.id(),
                            user.username(),
                            user.email(),
                            user.profile(),
                            user.status(),
                            user.preferences(),
                            user.createdAt(),
                            LocalDateTime.now()
                        );
                        userRepository.save(updatedUser);
                        
                        yield new AuthenticationResult(true, "Authentication successful", updatedUser);
                    };
                    
                    case InactiveStatus inactive -> {
                        span.setAttribute("auth.result", "account_inactive");
                        span.setAttribute("inactive.reason", inactive.reason());
                        yield new AuthenticationResult(false, "Account is inactive: " + inactive.reason(), null);
                    };
                    
                    case SuspendedStatus suspended -> {
                        span.setAttribute("auth.result", "account_suspended");
                        span.setAttribute("suspension.reason", suspended.reason());
                        
                        if (suspended.until() != null && LocalDateTime.now().isAfter(suspended.until())) {
                            // Automatically lift suspension
                            updateUserStatus(user.id(), ACTIVE);
                            yield new AuthenticationResult(true, "Suspension lifted - authentication successful", user);
                        } else {
                            String message = suspended.until() != null 
                                ? "Account suspended until " + suspended.until() + ": " + suspended.reason()
                                : "Account suspended: " + suspended.reason();
                            yield new AuthenticationResult(false, message, null);
                        }
                    };
                    
                    case PendingStatus pending -> {
                        span.setAttribute("auth.result", "verification_pending");
                        span.setAttribute("verification.type", pending.verificationType());
                        yield new AuthenticationResult(false, 
                            "Account verification pending: " + pending.verificationType(), null);
                    };
                };
                
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                throw new RuntimeException("Authentication failed", e);
            } finally {
                span.end();
            }
        });
    }
    
    private boolean verifyPassword(String password, User user) {
        // Simplified password verification
        return password != null && password.length() >= 6;
    }
    
    public record AuthenticationResult(boolean success, String message, User user) {}
}

@Service
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final Tracer tracer;
    private final AnalyticsService analyticsService;
    
    public ProjectService(ProjectRepository projectRepository, Tracer tracer, AnalyticsService analyticsService) {
        this.projectRepository = projectRepository;
        this.tracer = tracer;
        this.analyticsService = analyticsService;
    }
    
    /**
     * Project operations with pattern matching
     */
    public CompletableFuture<OperationResult> performProjectOperation(String projectId, ProjectOperation operation, String userId) {
        Span span = tracer.spanBuilder("project.operation")
            .setAttribute("project.id", projectId)
            .setAttribute("user.id", userId)
            .setAttribute("operation.type", operation.getClass().getSimpleName())
            .startSpan();
        
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = span.makeCurrent()) {
                Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
                
                // Pattern matching for different operations
                return switch (operation) {
                    case ArchiveOperation archiveOp -> {
                        if (!project.isOwner(userId)) {
                            yield new OperationResult(false, "Only project owner can archive", null);
                        }
                        
                        ArchivedProject archivedStatus = new ArchivedProject(
                            LocalDateTime.now(), 
                            archiveOp.reason()
                        );
                        
                        Project updatedProject = new Project(
                            project.id(),
                            project.name(),
                            project.description(),
                            project.ownerId(),
                            archivedStatus,
                            project.settings(),
                            project.collaboratorIds(),
                            project.createdAt(),
                            LocalDateTime.now()
                        );
                        
                        Project saved = projectRepository.save(updatedProject);
                        span.setAttribute("operation.result", "archived");
                        
                        yield new OperationResult(true, "Project archived successfully", saved);
                    };
                    
                    case AddCollaboratorOperation addOp -> {
                        if (!project.isOwner(userId)) {
                            yield new OperationResult(false, "Only project owner can add collaborators", null);
                        }
                        
                        if (project.isCollaborator(addOp.collaboratorId())) {
                            yield new OperationResult(false, "User is already a collaborator", null);
                        }
                        
                        List<String> newCollaborators = new ArrayList<>(project.collaboratorIds());
                        newCollaborators.add(addOp.collaboratorId());
                        
                        Project updatedProject = new Project(
                            project.id(),
                            project.name(),
                            project.description(),
                            project.ownerId(),
                            project.status(),
                            project.settings(),
                            newCollaborators,
                            project.createdAt(),
                            LocalDateTime.now()
                        );
                        
                        Project saved = projectRepository.save(updatedProject);
                        span.setAttribute("operation.result", "collaborator_added");
                        span.setAttribute("collaborator.id", addOp.collaboratorId());
                        
                        yield new OperationResult(true, "Collaborator added successfully", saved);
                    };
                    
                    case UpdateSettingsOperation updateOp -> {
                        if (!project.hasAccess(userId)) {
                            yield new OperationResult(false, "Access denied", null);
                        }
                        
                        Project updatedProject = new Project(
                            project.id(),
                            project.name(),
                            project.description(),
                            project.ownerId(),
                            project.status(),
                            updateOp.newSettings(),
                            project.collaboratorIds(),
                            project.createdAt(),
                            LocalDateTime.now()
                        );
                        
                        Project saved = projectRepository.save(updatedProject);
                        span.setAttribute("operation.result", "settings_updated");
                        
                        yield new OperationResult(true, "Settings updated successfully", saved);
                    };
                };
                
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                throw new RuntimeException("Project operation failed", e);
            } finally {
                span.end();
            }
        });
    }
    
    // Sealed interface for project operations
    public sealed interface ProjectOperation 
        permits ArchiveOperation, AddCollaboratorOperation, UpdateSettingsOperation {}
    
    public record ArchiveOperation(String reason) implements ProjectOperation {}
    
    public record AddCollaboratorOperation(String collaboratorId) implements ProjectOperation {}
    
    public record UpdateSettingsOperation(ProjectSettings newSettings) implements ProjectOperation {}
    
    public record OperationResult(boolean success, String message, Project project) {}
}
```

## 2. High-Performance Virtual Thread Controllers

### GraphQL API with Virtual Threads

```java
package com.javajourney.platform.api;

import com.javajourney.platform.domain.*;
import com.javajourney.platform.service.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;

/**
 * GraphQL Controller using Virtual Threads for high concurrency
 */
@Controller
public class PlatformGraphQLController {
    
    private final UserService userService;
    private final ProjectService projectService;
    private final AnalyticsService analyticsService;
    private final MLInsightsService mlInsightsService;
    
    public PlatformGraphQLController(UserService userService, ProjectService projectService,
                                   AnalyticsService analyticsService, MLInsightsService mlInsightsService) {
        this.userService = userService;
        this.projectService = projectService;
        this.analyticsService = analyticsService;
        this.mlInsightsService = mlInsightsService;
    }
    
    // Query resolvers using virtual threads
    @QueryMapping
    public CompletableFuture<User> user(@Argument String id) {
        return CompletableFuture.supplyAsync(() -> {
            return userService.findById(id).orElse(null);
        });
    }
    
    @QueryMapping
    public CompletableFuture<UserConnection> users(
            @Argument Integer first,
            @Argument String after,
            @Argument UserFilter filter) {
        
        // Use structured concurrency for parallel data fetching
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Parallel tasks
                var usersTask = scope.fork(() -> userService.getUsers(first, after, filter));
                var totalCountTask = scope.fork(() -> userService.getUserCount(filter));
                var analyticsTask = scope.fork(() -> analyticsService.getUserAnalytics(filter));
                
                // Wait for all tasks
                scope.join();
                scope.throwIfFailed();
                
                // Combine results
                List<User> users = usersTask.get();
                Long totalCount = totalCountTask.get();
                UserAnalytics analytics = analyticsTask.get();
                
                return new UserConnection(
                    users.stream().map(user -> new UserEdge(user, encodeCursor(user.id()))).toList(),
                    new PageInfo(
                        users.size() == first,
                        after != null,
                        users.isEmpty() ? null : encodeCursor(users.get(0).id()),
                        users.isEmpty() ? null : encodeCursor(users.get(users.size() - 1).id())
                    ),
                    totalCount.intValue(),
                    analytics
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch users", e);
            }
        });
    }
    
    @QueryMapping
    public CompletableFuture<Project> project(@Argument String id) {
        return CompletableFuture.supplyAsync(() -> {
            return projectService.findById(id).orElse(null);
        });
    }
    
    @QueryMapping
    public CompletableFuture<DashboardData> dashboard(@Argument String userId) {
        // Complex dashboard data fetching with structured concurrency
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Parallel dashboard data fetching
                var userTask = scope.fork(() -> userService.findById(userId));
                var projectsTask = scope.fork(() -> projectService.getUserProjects(userId));
                var recentActivityTask = scope.fork(() -> analyticsService.getRecentActivity(userId));
                var insightsTask = scope.fork(() -> mlInsightsService.getUserInsights(userId));
                var recommendationsTask = scope.fork(() -> mlInsightsService.getRecommendations(userId));
                
                // Wait for all tasks
                scope.join();
                scope.throwIfFailed();
                
                // Combine results
                User user = userTask.get().orElse(null);
                List<Project> projects = projectsTask.get();
                List<ActivityEvent> recentActivity = recentActivityTask.get();
                UserInsights insights = insightsTask.get();
                List<Recommendation> recommendations = recommendationsTask.get();
                
                return new DashboardData(
                    user,
                    projects,
                    recentActivity,
                    insights,
                    recommendations,
                    LocalDateTime.now()
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch dashboard data", e);
            }
        });
    }
    
    // Mutation resolvers
    @MutationMapping
    public CompletableFuture<CreateUserPayload> createUser(@Argument CreateUserInput input) {
        return userService.createUser(input)
            .thenApply(user -> new CreateUserPayload(user, List.of()))
            .exceptionally(throwable -> new CreateUserPayload(null, 
                List.of(new GraphQLError("USER_CREATION_FAILED", throwable.getMessage()))));
    }
    
    @MutationMapping
    public CompletableFuture<UpdateUserStatusPayload> updateUserStatus(
            @Argument String userId,
            @Argument UserStatusInput statusInput) {
        
        return userService.updateUserStatus(userId, convertToUserStatus(statusInput))
            .thenApply(user -> new UpdateUserStatusPayload(user, List.of()))
            .exceptionally(throwable -> new UpdateUserStatusPayload(null,
                List.of(new GraphQLError("STATUS_UPDATE_FAILED", throwable.getMessage()))));
    }
    
    @MutationMapping
    public CompletableFuture<CreateProjectPayload> createProject(@Argument CreateProjectInput input) {
        return projectService.createProject(input)
            .thenApply(project -> new CreateProjectPayload(project, List.of()))
            .exceptionally(throwable -> new CreateProjectPayload(null,
                List.of(new GraphQLError("PROJECT_CREATION_FAILED", throwable.getMessage()))));
    }
    
    // Field resolvers with virtual threads
    @SchemaMapping(typeName = "User", field = "projects")
    public CompletableFuture<List<Project>> userProjects(User user, @Argument Integer first) {
        return CompletableFuture.supplyAsync(() -> {
            return projectService.getUserProjects(user.id(), first);
        });
    }
    
    @SchemaMapping(typeName = "User", field = "insights")
    public CompletableFuture<UserInsights> userInsights(User user) {
        return CompletableFuture.supplyAsync(() -> {
            return mlInsightsService.getUserInsights(user.id());
        });
    }
    
    @SchemaMapping(typeName = "Project", field = "owner")
    public CompletableFuture<User> projectOwner(Project project) {
        return CompletableFuture.supplyAsync(() -> {
            return userService.findById(project.ownerId()).orElse(null);
        });
    }
    
    @SchemaMapping(typeName = "Project", field = "collaborators")
    public CompletableFuture<List<User>> projectCollaborators(Project project) {
        return CompletableFuture.supplyAsync(() -> {
            // Use structured concurrency for parallel user fetching
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                List<StructuredTaskScope.Subtask<User>> tasks = project.collaboratorIds().stream()
                    .map(id -> scope.fork(() -> userService.findById(id).orElse(null)))
                    .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                return tasks.stream()
                    .map(task -> {
                        try {
                            return task.get();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch collaborators", e);
            }
        });
    }
    
    // Real-time subscriptions
    @SubscriptionMapping
    public Flux<User> userStatusChanged(@Argument String userId) {
        return analyticsService.getUserStatusChangeStream(userId);
    }
    
    @SubscriptionMapping
    public Flux<ProjectEvent> projectUpdated(@Argument String projectId) {
        return analyticsService.getProjectEventStream(projectId);
    }
    
    @SubscriptionMapping
    public Flux<SystemNotification> systemNotifications(@Argument String userId) {
        return analyticsService.getSystemNotificationStream(userId);
    }
    
    // Helper methods
    private UserStatus convertToUserStatus(UserStatusInput input) {
        return switch (input.type().toUpperCase()) {
            case "ACTIVE" -> ACTIVE;
            case "INACTIVE" -> new InactiveStatus(input.reason());
            case "SUSPENDED" -> new SuspendedStatus(input.reason(), input.until());
            case "PENDING" -> new PendingStatus(input.verificationType());
            default -> throw new IllegalArgumentException("Invalid status type: " + input.type());
        };
    }
    
    private String encodeCursor(String id) {
        return java.util.Base64.getEncoder().encodeToString(id.getBytes());
    }
    
    // GraphQL data classes
    public record UserConnection(
        List<UserEdge> edges,
        PageInfo pageInfo,
        int totalCount,
        UserAnalytics analytics
    ) {}
    
    public record UserEdge(User node, String cursor) {}
    
    public record PageInfo(
        boolean hasNextPage,
        boolean hasPreviousPage,
        String startCursor,
        String endCursor
    ) {}
    
    public record DashboardData(
        User user,
        List<Project> projects,
        List<ActivityEvent> recentActivity,
        UserInsights insights,
        List<Recommendation> recommendations,
        LocalDateTime timestamp
    ) {}
    
    public record CreateUserPayload(User user, List<GraphQLError> errors) {}
    public record UpdateUserStatusPayload(User user, List<GraphQLError> errors) {}
    public record CreateProjectPayload(Project project, List<GraphQLError> errors) {}
    
    public record GraphQLError(String code, String message) {}
    
    // Input types
    public record CreateUserInput(
        String username,
        String email,
        UserProfileInput profile
    ) {}
    
    public record UserProfileInput(
        String firstName,
        String lastName,
        String bio,
        String avatar,
        String location,
        String timezone
    ) {}
    
    public record UserStatusInput(
        String type,
        String reason,
        LocalDateTime until,
        String verificationType
    ) {}
    
    public record CreateProjectInput(
        String name,
        String description,
        ProjectSettingsInput settings
    ) {}
    
    public record ProjectSettingsInput(
        boolean isPublic,
        boolean allowCollaborators,
        String defaultBranch
    ) {}
    
    public record UserFilter(
        String username,
        String email,
        String status,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
    ) {}
}
```

## 3. Machine Learning Intelligence Layer

### ML-Powered Insights and Recommendations

```java
package com.javajourney.platform.ml;

import com.javajourney.platform.domain.*;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;

@Service
public class MLInsightsService {
    
    private final MLModelRegistry modelRegistry;
    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final ProjectService projectService;
    
    public MLInsightsService(MLModelRegistry modelRegistry, AnalyticsService analyticsService,
                           UserService userService, ProjectService projectService) {
        this.modelRegistry = modelRegistry;
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.projectService = projectService;
    }
    
    /**
     * Generate comprehensive user insights using ML models
     */
    public CompletableFuture<UserInsights> getUserInsights(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Parallel ML inference tasks
                var churnTask = scope.fork(() -> predictUserChurn(userId));
                var engagementTask = scope.fork(() -> analyzeUserEngagement(userId));
                var preferencesTask = scope.fork(() -> predictUserPreferences(userId));
                var behaviorTask = scope.fork(() -> analyzeBehaviorPatterns(userId));
                var riskTask = scope.fork(() -> assessSecurityRisk(userId));
                
                scope.join();
                scope.throwIfFailed();
                
                // Combine ML insights
                ChurnPrediction churnPrediction = churnTask.get();
                EngagementAnalysis engagement = engagementTask.get();
                PreferencesPrediction preferences = preferencesTask.get();
                BehaviorAnalysis behavior = behaviorTask.get();
                SecurityRiskAssessment risk = riskTask.get();
                
                return new UserInsights(
                    userId,
                    churnPrediction,
                    engagement,
                    preferences,
                    behavior,
                    risk,
                    generateActionableInsights(churnPrediction, engagement, behavior),
                    LocalDateTime.now()
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate user insights", e);
            }
        });
    }
    
    /**
     * Predict user churn probability
     */
    private ChurnPrediction predictUserChurn(String userId) {
        try {
            User user = userService.findById(userId).orElseThrow();
            List<AnalyticsEvent> userEvents = analyticsService.getUserEvents(userId, Duration.ofDays(30));
            
            // Prepare features for churn model
            Map<String, Object> features = prepareChurnFeatures(user, userEvents);
            MLModelRegistry.ModelInput input = new MLModelRegistry.ModelInput(features);
            
            // Get churn prediction
            MLModelRegistry.MLModel churnModel = modelRegistry.getModel("user_churn_prediction")
                .orElseThrow(() -> new RuntimeException("Churn model not available"));
            
            MLModelRegistry.PredictionResult result = churnModel.predict(input);
            
            boolean willChurn = "churn".equals(result.prediction());
            double churnProbability = result.probabilities().getOrDefault("churn", 0.0);
            
            String riskLevel = switch (churnProbability) {
                case double p when p > 0.8 -> "HIGH";
                case double p when p > 0.5 -> "MEDIUM";
                default -> "LOW";
            };
            
            List<String> factors = identifyChurnFactors(features, result);
            List<String> interventions = recommendChurnInterventions(willChurn, churnProbability, user);
            
            return new ChurnPrediction(
                willChurn,
                churnProbability,
                riskLevel,
                factors,
                interventions,
                result.confidence()
            );
            
        } catch (Exception e) {
            // Return default prediction on error
            return new ChurnPrediction(false, 0.0, "UNKNOWN", List.of(), List.of(), 0.0);
        }
    }
    
    /**
     * Analyze user engagement patterns
     */
    private EngagementAnalysis analyzeUserEngagement(String userId) {
        try {
            List<AnalyticsEvent> events = analyticsService.getUserEvents(userId, Duration.ofDays(7));
            
            // Calculate engagement metrics
            int dailyActivenessDays = calculateActivenessDays(events);
            double sessionDuration = calculateAverageSessionDuration(events);
            int featureUsage = calculateFeatureUsageScore(events);
            double socialInteraction = calculateSocialInteractionScore(userId, events);
            
            // Overall engagement score
            double engagementScore = (dailyActivenessDays * 0.3) + 
                                  (Math.min(sessionDuration / 60, 5) * 0.25) +
                                  (Math.min(featureUsage / 10.0, 1) * 0.25) +
                                  (socialInteraction * 0.2);
            
            String engagementLevel = switch (engagementScore) {
                case double s when s > 4.0 -> "VERY_HIGH";
                case double s when s > 3.0 -> "HIGH";
                case double s when s > 2.0 -> "MEDIUM";
                case double s when s > 1.0 -> "LOW";
                default -> "VERY_LOW";
            };
            
            List<String> engagementTrends = identifyEngagementTrends(events);
            List<String> improvementAreas = identifyImprovementAreas(engagementScore, events);
            
            return new EngagementAnalysis(
                engagementScore,
                engagementLevel,
                dailyActivenessDays,
                sessionDuration,
                featureUsage,
                socialInteraction,
                engagementTrends,
                improvementAreas
            );
            
        } catch (Exception e) {
            return new EngagementAnalysis(0.0, "UNKNOWN", 0, 0.0, 0, 0.0, List.of(), List.of());
        }
    }
    
    /**
     * Predict user preferences using collaborative filtering
     */
    private PreferencesPrediction predictUserPreferences(String userId) {
        try {
            // Get user behavior data
            List<AnalyticsEvent> events = analyticsService.getUserEvents(userId, Duration.ofDays(30));
            User user = userService.findById(userId).orElseThrow();
            
            // Find similar users
            List<String> similarUsers = findSimilarUsers(userId, events);
            
            // Predict preferences based on similar users
            Map<String, Double> preferenceScores = calculatePreferenceScores(userId, similarUsers);
            
            // Extract top preferences
            List<UserPreference> topPreferences = preferenceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(entry -> new UserPreference(entry.getKey(), entry.getValue()))
                .toList();
            
            // Generate recommendations based on preferences
            List<String> recommendations = generatePreferenceBasedRecommendations(topPreferences, user);
            
            return new PreferencesPrediction(
                topPreferences,
                recommendations,
                similarUsers.size(),
                calculatePredictionConfidence(preferenceScores)
            );
            
        } catch (Exception e) {
            return new PreferencesPrediction(List.of(), List.of(), 0, 0.0);
        }
    }
    
    /**
     * Analyze user behavior patterns using time series analysis
     */
    private BehaviorAnalysis analyzeBehaviorPatterns(String userId) {
        try {
            List<AnalyticsEvent> events = analyticsService.getUserEvents(userId, Duration.ofDays(30));
            
            // Analyze temporal patterns
            Map<Integer, Integer> hourlyActivity = analyzeHourlyActivity(events);
            Map<DayOfWeek, Integer> dailyActivity = analyzeDailyActivity(events);
            
            // Identify peak activity times
            int peakHour = hourlyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(12);
            
            DayOfWeek peakDay = dailyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DayOfWeek.MONDAY);
            
            // Analyze usage patterns
            List<String> usagePatterns = identifyUsagePatterns(events);
            List<String> anomalies = detectBehaviorAnomalies(events);
            
            // Predict future behavior
            Map<String, Double> futureBehaviorPredictions = predictFutureBehavior(events);
            
            return new BehaviorAnalysis(
                hourlyActivity,
                dailyActivity,
                peakHour,
                peakDay,
                usagePatterns,
                anomalies,
                futureBehaviorPredictions
            );
            
        } catch (Exception e) {
            return new BehaviorAnalysis(Map.of(), Map.of(), 12, DayOfWeek.MONDAY, List.of(), List.of(), Map.of());
        }
    }
    
    /**
     * Assess security risk using anomaly detection
     */
    private SecurityRiskAssessment assessSecurityRisk(String userId) {
        try {
            List<AnalyticsEvent> events = analyticsService.getUserEvents(userId, Duration.ofDays(7));
            
            // Prepare features for security model
            Map<String, Object> features = prepareSecurityFeatures(userId, events);
            MLModelRegistry.ModelInput input = new MLModelRegistry.ModelInput(features);
            
            // Get risk assessment
            MLModelRegistry.MLModel securityModel = modelRegistry.getModel("security_risk_assessment")
                .orElseThrow(() -> new RuntimeException("Security model not available"));
            
            MLModelRegistry.PredictionResult result = securityModel.predict(input);
            
            boolean isHighRisk = "high_risk".equals(result.prediction());
            double riskScore = result.probabilities().getOrDefault("high_risk", 0.0);
            
            String riskLevel = switch (riskScore) {
                case double r when r > 0.8 -> "CRITICAL";
                case double r when r > 0.6 -> "HIGH";
                case double r when r > 0.4 -> "MEDIUM";
                default -> "LOW";
            };
            
            List<String> riskFactors = identifyRiskFactors(features, result);
            List<String> recommendations = generateSecurityRecommendations(riskLevel, riskFactors);
            
            return new SecurityRiskAssessment(
                isHighRisk,
                riskScore,
                riskLevel,
                riskFactors,
                recommendations,
                result.confidence()
            );
            
        } catch (Exception e) {
            return new SecurityRiskAssessment(false, 0.0, "UNKNOWN", List.of(), List.of(), 0.0);
        }
    }
    
    /**
     * Generate intelligent recommendations
     */
    public CompletableFuture<List<Recommendation>> getRecommendations(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Parallel recommendation generation
                var contentTask = scope.fork(() -> generateContentRecommendations(userId));
                var socialTask = scope.fork(() -> generateSocialRecommendations(userId));
                var featureTask = scope.fork(() -> generateFeatureRecommendations(userId));
                var learningTask = scope.fork(() -> generateLearningRecommendations(userId));
                
                scope.join();
                scope.throwIfFailed();
                
                // Combine and rank recommendations
                List<Recommendation> allRecommendations = new ArrayList<>();
                allRecommendations.addAll(contentTask.get());
                allRecommendations.addAll(socialTask.get());
                allRecommendations.addAll(featureTask.get());
                allRecommendations.addAll(learningTask.get());
                
                return allRecommendations.stream()
                    .sorted((r1, r2) -> Double.compare(r2.relevanceScore(), r1.relevanceScore()))
                    .limit(10)
                    .toList();
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate recommendations", e);
            }
        });
    }
    
    // Helper methods (implementations would be more complex in real scenarios)
    private Map<String, Object> prepareChurnFeatures(User user, List<AnalyticsEvent> events) {
        return Map.of(
            "days_since_last_login", java.time.temporal.ChronoUnit.DAYS.between(user.lastLoginAt(), LocalDateTime.now()),
            "total_sessions", events.size(),
            "avg_session_duration", calculateAverageSessionDuration(events),
            "feature_usage_count", calculateFeatureUsageScore(events),
            "days_since_registration", java.time.temporal.ChronoUnit.DAYS.between(user.createdAt(), LocalDateTime.now())
        );
    }
    
    private List<String> identifyChurnFactors(Map<String, Object> features, MLModelRegistry.PredictionResult result) {
        // Analyze feature importance from model result
        return List.of("Low recent activity", "Limited feature usage");
    }
    
    private List<String> recommendChurnInterventions(boolean willChurn, double probability, User user) {
        if (!willChurn) return List.of("Continue current engagement");
        
        List<String> interventions = new ArrayList<>();
        interventions.add("Send personalized re-engagement email");
        
        if (probability > 0.8) {
            interventions.add("Offer premium feature trial");
            interventions.add("Schedule retention call");
        }
        
        return interventions;
    }
    
    // Additional helper methods would be implemented here...
    private int calculateActivenessDays(List<AnalyticsEvent> events) { return 5; }
    private double calculateAverageSessionDuration(List<AnalyticsEvent> events) { return 30.5; }
    private int calculateFeatureUsageScore(List<AnalyticsEvent> events) { return 8; }
    private double calculateSocialInteractionScore(String userId, List<AnalyticsEvent> events) { return 0.7; }
    private List<String> identifyEngagementTrends(List<AnalyticsEvent> events) { return List.of("Increasing mobile usage"); }
    private List<String> identifyImprovementAreas(double score, List<AnalyticsEvent> events) { return List.of("Social features"); }
    private List<String> findSimilarUsers(String userId, List<AnalyticsEvent> events) { return List.of("user1", "user2"); }
    private Map<String, Double> calculatePreferenceScores(String userId, List<String> similarUsers) { return Map.of("feature1", 0.8); }
    private List<String> generatePreferenceBasedRecommendations(List<UserPreference> preferences, User user) { return List.of("Try feature X"); }
    private double calculatePredictionConfidence(Map<String, Double> scores) { return 0.85; }
    private Map<Integer, Integer> analyzeHourlyActivity(List<AnalyticsEvent> events) { return Map.of(14, 10); }
    private Map<DayOfWeek, Integer> analyzeDailyActivity(List<AnalyticsEvent> events) { return Map.of(DayOfWeek.MONDAY, 5); }
    private List<String> identifyUsagePatterns(List<AnalyticsEvent> events) { return List.of("Morning heavy usage"); }
    private List<String> detectBehaviorAnomalies(List<AnalyticsEvent> events) { return List.of(); }
    private Map<String, Double> predictFutureBehavior(List<AnalyticsEvent> events) { return Map.of("next_login", 0.9); }
    private Map<String, Object> prepareSecurityFeatures(String userId, List<AnalyticsEvent> events) { return Map.of("login_locations", 1); }
    private List<String> identifyRiskFactors(Map<String, Object> features, MLModelRegistry.PredictionResult result) { return List.of(); }
    private List<String> generateSecurityRecommendations(String riskLevel, List<String> factors) { return List.of("Enable 2FA"); }
    private List<Recommendation> generateContentRecommendations(String userId) { return List.of(); }
    private List<Recommendation> generateSocialRecommendations(String userId) { return List.of(); }
    private List<Recommendation> generateFeatureRecommendations(String userId) { return List.of(); }
    private List<Recommendation> generateLearningRecommendations(String userId) { return List.of(); }
    private List<String> generateActionableInsights(ChurnPrediction churn, EngagementAnalysis engagement, BehaviorAnalysis behavior) {
        return List.of("User shows strong engagement", "Consider advanced features");
    }
    
    // Data classes for ML insights
    public record UserInsights(
        String userId,
        ChurnPrediction churnPrediction,
        EngagementAnalysis engagement,
        PreferencesPrediction preferences,
        BehaviorAnalysis behavior,
        SecurityRiskAssessment security,
        List<String> actionableInsights,
        LocalDateTime generatedAt
    ) {}
    
    public record ChurnPrediction(
        boolean willChurn,
        double churnProbability,
        String riskLevel,
        List<String> factors,
        List<String> interventions,
        double confidence
    ) {}
    
    public record EngagementAnalysis(
        double engagementScore,
        String engagementLevel,
        int dailyActivenessDays,
        double averageSessionDuration,
        int featureUsageScore,
        double socialInteractionScore,
        List<String> trends,
        List<String> improvementAreas
    ) {}
    
    public record PreferencesPrediction(
        List<UserPreference> topPreferences,
        List<String> recommendations,
        int similarUsersCount,
        double confidence
    ) {}
    
    public record UserPreference(String preference, double score) {}
    
    public record BehaviorAnalysis(
        Map<Integer, Integer> hourlyActivity,
        Map<DayOfWeek, Integer> dailyActivity,
        int peakHour,
        DayOfWeek peakDay,
        List<String> usagePatterns,
        List<String> anomalies,
        Map<String, Double> futurePredictions
    ) {}
    
    public record SecurityRiskAssessment(
        boolean isHighRisk,
        double riskScore,
        String riskLevel,
        List<String> riskFactors,
        List<String> recommendations,
        double confidence
    ) {}
    
    public record Recommendation(
        String id,
        String type,
        String title,
        String description,
        double relevanceScore,
        Map<String, Object> metadata
    ) {}
}
```

## 4. Comprehensive Observability

### OpenTelemetry Integration

```java
package com.javajourney.platform.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ObservabilityConfiguration {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }
    
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("modern-platform", "1.0.0");
    }
    
    @Bean
    public Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter("modern-platform", "1.0.0");
    }
    
    @Bean
    public PlatformMetrics platformMetrics(Meter meter) {
        return new PlatformMetrics(meter);
    }
    
    @Bean
    public DistributedTracing distributedTracing(Tracer tracer) {
        return new DistributedTracing(tracer);
    }
}

@Component
public class PlatformMetrics {
    
    private final Meter meter;
    
    // Counters
    private final LongCounter userRegistrations;
    private final LongCounter userLogins; 
    private final LongCounter projectCreations;
    private final LongCounter mlInferences;
    private final LongCounter graphqlQueries;
    private final LongCounter graphqlMutations;
    
    // Gauges
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong activeProjects = new AtomicLong(0);
    private final AtomicLong mlModelCount = new AtomicLong(0);
    
    // Attribute keys
    private static final AttributeKey<String> USER_STATUS = AttributeKey.stringKey("user.status");
    private static final AttributeKey<String> PROJECT_TYPE = AttributeKey.stringKey("project.type");
    private static final AttributeKey<String> ML_MODEL = AttributeKey.stringKey("ml.model");
    private static final AttributeKey<String> GRAPHQL_OPERATION = AttributeKey.stringKey("graphql.operation");
    
    public PlatformMetrics(Meter meter) {
        this.meter = meter;
        
        // Initialize counters
        this.userRegistrations = meter
            .counterBuilder("platform_user_registrations_total")
            .setDescription("Total number of user registrations")
            .build();
            
        this.userLogins = meter
            .counterBuilder("platform_user_logins_total")
            .setDescription("Total number of user logins")
            .build();
            
        this.projectCreations = meter
            .counterBuilder("platform_project_creations_total")
            .setDescription("Total number of project creations")
            .build();
            
        this.mlInferences = meter
            .counterBuilder("platform_ml_inferences_total")
            .setDescription("Total number of ML model inferences")
            .build();
            
        this.graphqlQueries = meter
            .counterBuilder("platform_graphql_queries_total")
            .setDescription("Total number of GraphQL queries")
            .build();
            
        this.graphqlMutations = meter
            .counterBuilder("platform_graphql_mutations_total")
            .setDescription("Total number of GraphQL mutations")
            .build();
    }
    
    @PostConstruct
    public void initializeGauges() {
        // Active users gauge
        meter.gaugeBuilder("platform_active_users")
            .setDescription("Number of currently active users")
            .ofLongs()
            .buildWithCallback(measurement -> measurement.record(activeUsers.get()));
            
        // Active projects gauge
        meter.gaugeBuilder("platform_active_projects")
            .setDescription("Number of currently active projects")
            .ofLongs()
            .buildWithCallback(measurement -> measurement.record(activeProjects.get()));
            
        // ML models gauge
        meter.gaugeBuilder("platform_ml_models")
            .setDescription("Number of loaded ML models")
            .ofLongs()
            .buildWithCallback(measurement -> measurement.record(mlModelCount.get()));
            
        // JVM metrics
        meter.gaugeBuilder("platform_jvm_memory_used")
            .setDescription("JVM memory usage in bytes")
            .ofLongs()
            .buildWithCallback(measurement -> {
                Runtime runtime = Runtime.getRuntime();
                long used = runtime.totalMemory() - runtime.freeMemory();
                measurement.record(used);
            });
    }
    
    // Metric recording methods
    public void recordUserRegistration(String userStatus) {
        userRegistrations.add(1, Attributes.of(USER_STATUS, userStatus));
    }
    
    public void recordUserLogin(String userStatus) {
        userLogins.add(1, Attributes.of(USER_STATUS, userStatus));
    }
    
    public void recordProjectCreation(String projectType) {
        projectCreations.add(1, Attributes.of(PROJECT_TYPE, projectType));
    }
    
    public void recordMLInference(String modelName, long durationMs) {
        mlInferences.add(1, Attributes.of(ML_MODEL, modelName));
        
        // Record duration histogram
        meter.histogramBuilder("platform_ml_inference_duration")
            .setDescription("ML inference duration in milliseconds")
            .ofLongs()
            .build()
            .record(durationMs, Attributes.of(ML_MODEL, modelName));
    }
    
    public void recordGraphQLOperation(String operationType, String operationName, long durationMs) {
        if ("query".equals(operationType)) {
            graphqlQueries.add(1, Attributes.of(GRAPHQL_OPERATION, operationName));
        } else if ("mutation".equals(operationType)) {
            graphqlMutations.add(1, Attributes.of(GRAPHQL_OPERATION, operationName));
        }
        
        // Record duration
        meter.histogramBuilder("platform_graphql_duration")
            .setDescription("GraphQL operation duration in milliseconds")
            .ofLongs()
            .build()
            .record(durationMs, Attributes.of(
                AttributeKey.stringKey("operation.type"), operationType,
                GRAPHQL_OPERATION, operationName
            ));
    }
    
    // Gauge updates
    public void updateActiveUsers(long count) {
        activeUsers.set(count);
    }
    
    public void updateActiveProjects(long count) {
        activeProjects.set(count);
    }
    
    public void updateMLModelCount(long count) {
        mlModelCount.set(count);
    }
}

@Component
public class DistributedTracing {
    
    private final Tracer tracer;
    
    public DistributedTracing(Tracer tracer) {
        this.tracer = tracer;
    }
    
    /**
     * Create a span for user operations
     */
    public Span createUserOperationSpan(String operation, String userId) {
        return tracer.spanBuilder("user." + operation)
            .setAttribute("user.id", userId)
            .setAttribute("operation.type", "user")
            .startSpan();
    }
    
    /**
     * Create a span for project operations
     */
    public Span createProjectOperationSpan(String operation, String projectId, String userId) {
        return tracer.spanBuilder("project." + operation)
            .setAttribute("project.id", projectId)
            .setAttribute("user.id", userId)
            .setAttribute("operation.type", "project")
            .startSpan();
    }
    
    /**
     * Create a span for ML operations
     */
    public Span createMLOperationSpan(String operation, String modelId, String userId) {
        return tracer.spanBuilder("ml." + operation)
            .setAttribute("ml.model.id", modelId)
            .setAttribute("user.id", userId)
            .setAttribute("operation.type", "ml")
            .startSpan();
    }
    
    /**
     * Create a span for GraphQL operations
     */
    public Span createGraphQLOperationSpan(String operationType, String operationName) {
        return tracer.spanBuilder("graphql." + operationType)
            .setAttribute("graphql.operation.type", operationType)
            .setAttribute("graphql.operation.name", operationName)
            .setAttribute("operation.type", "graphql")
            .startSpan();
    }
}

@Component
public class PlatformHealthIndicator implements HealthIndicator {
    
    private final MLModelRegistry modelRegistry;
    private final PlatformMetrics platformMetrics;
    
    public PlatformHealthIndicator(MLModelRegistry modelRegistry, PlatformMetrics platformMetrics) {
        this.modelRegistry = modelRegistry;
        this.platformMetrics = platformMetrics;
    }
    
    @Override
    public Health health() {
        try {
            // Check ML models health
            Set<String> availableModels = modelRegistry.getAvailableModels();
            long healthyModels = availableModels.stream()
                .mapToLong(modelId -> {
                    try {
                        MLModelRegistry.MLModel model = modelRegistry.getModel(modelId).orElse(null);
                        return model != null && model.isLoaded() ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
            
            // Check system resources
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;
            
            // Check if critical thresholds are exceeded
            boolean isHealthy = healthyModels > 0 && memoryUsage < 0.9;
            
            Health.Builder builder = isHealthy ? Health.up() : Health.down();
            
            return builder
                .withDetail("ml_models_total", availableModels.size())
                .withDetail("ml_models_healthy", healthyModels)
                .withDetail("memory_usage_percent", String.format("%.1f", memoryUsage * 100))
                .withDetail("available_processors", runtime.availableProcessors())
                .withDetail("max_memory_mb", runtime.maxMemory() / (1024 * 1024))
                .withDetail("timestamp", LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", LocalDateTime.now())
                .build();
        }
    }
}
```

## 5. Native Image Configuration

### GraalVM Native Configuration

```java
package com.javajourney.platform.native_config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Native image configuration for the modern platform
 */
@Configuration
@ImportRuntimeHints(PlatformNativeConfiguration.PlatformRuntimeHints.class)
public class PlatformNativeConfiguration {
    
    static class PlatformRuntimeHints implements RuntimeHintsRegistrar {
        
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register domain model classes
            registerDomainModels(hints);
            
            // Register ML model classes
            registerMLModels(hints);
            
            // Register GraphQL types
            registerGraphQLTypes(hints);
            
            // Register observability classes
            registerObservabilityClasses(hints);
            
            // Register resources
            registerResources(hints);
            
            // Register serialization
            registerSerialization(hints);
        }
        
        private void registerDomainModels(RuntimeHints hints) {
            // User domain
            hints.reflection()
                .registerType(com.javajourney.platform.domain.User.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
                .registerType(com.javajourney.platform.domain.UserProfile.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
                .registerType(com.javajourney.platform.domain.UserPreferences.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS));
            
            // Project domain
            hints.reflection()
                .registerType(com.javajourney.platform.domain.Project.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
                .registerType(com.javajourney.platform.domain.ProjectSettings.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS));
            
            // Status interfaces and implementations
            hints.reflection()
                .registerType(com.javajourney.platform.domain.ActiveStatus.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(com.javajourney.platform.domain.InactiveStatus.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(com.javajourney.platform.domain.SuspendedStatus.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(com.javajourney.platform.domain.PendingStatus.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS));
        }
        
        private void registerMLModels(RuntimeHints hints) {
            // ML registry and model classes
            hints.reflection()
                .registerType(com.javajourney.platform.ml.MLModelRegistry.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(com.javajourney.platform.ml.MLModelRegistry.ModelInput.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
                .registerType(com.javajourney.platform.ml.MLModelRegistry.PredictionResult.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS));
            
            // ML insights classes
            hints.reflection()
                .registerType(com.javajourney.platform.ml.MLInsightsService.UserInsights.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
                .registerType(com.javajourney.platform.ml.MLInsightsService.ChurnPrediction.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
                .registerType(com.javajourney.platform.ml.MLInsightsService.EngagementAnalysis.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS));
        }
        
        private void registerGraphQLTypes(RuntimeHints hints) {
            // GraphQL API classes
            hints.reflection()
                .registerType(com.javajourney.platform.api.PlatformGraphQLController.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(com.javajourney.platform.api.PlatformGraphQLController.UserConnection.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS))
                .registerType(com.javajourney.platform.api.PlatformGraphQLController.DashboardData.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                               org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS));
        }
        
        private void registerObservabilityClasses(RuntimeHints hints) {
            // OpenTelemetry classes
            hints.reflection()
                .registerType(com.javajourney.platform.observability.PlatformMetrics.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS))
                .registerType(com.javajourney.platform.observability.DistributedTracing.class, hint -> hint
                    .withMembers(org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS));
        }
        
        private void registerResources(RuntimeHints hints) {
            // Configuration files
            hints.resources()
                .registerPattern("application*.yml")
                .registerPattern("application*.yaml")
                .registerPattern("application*.properties")
                .registerPattern("logback*.xml")
                .registerPattern("graphql/*.graphqls")
                .registerPattern("static/**")
                .registerPattern("templates/**");
                
            // ML model files
            hints.resources()
                .registerPattern("models/**")
                .registerPattern("ml-config/**");
        }
        
        private void registerSerialization(RuntimeHints hints) {
            // Domain objects for JSON serialization
            hints.serialization()
                .registerType(com.javajourney.platform.domain.User.class)
                .registerType(com.javajourney.platform.domain.Project.class)
                .registerType(com.javajourney.platform.ml.MLInsightsService.UserInsights.class);
        }
    }
}
```

## Practice Exercises

### Exercise 1: Complete Platform Implementation

Implement all remaining components including repositories, additional services, and security configuration.

### Exercise 2: Advanced GraphQL Features

Add advanced GraphQL features like subscriptions, federation, and custom directives.

### Exercise 3: ML Model Pipeline

Create a complete ML pipeline from training to deployment with A/B testing capabilities.

### Exercise 4: Production Deployment

Deploy the platform using Docker, Kubernetes, and implement comprehensive monitoring.

## Key Takeaways

1. **Modern Java Integration**: Successfully combined virtual threads, records, pattern matching, and sealed classes for a type-safe, high-performance system
2. **Cloud-Native Architecture**: Built with observability, scalability, and resilience as core principles
3. **ML-Powered Intelligence**: Integrated machine learning seamlessly into business workflows for intelligent features
4. **GraphQL Excellence**: Implemented efficient, flexible APIs with proper batching and real-time capabilities
5. **Production Ready**: Configured for GraalVM native images with comprehensive monitoring and health checks

This capstone project demonstrates the power of modern Java technologies working together to create enterprise-grade, cloud-native applications that are both performant and maintainable.

## Next Steps

This concludes Week 14 of the Java Learning Journey. The modern cloud-native enterprise platform showcases the cutting-edge capabilities of the Java ecosystem and prepares you for building next-generation applications with the latest technologies and best practices.