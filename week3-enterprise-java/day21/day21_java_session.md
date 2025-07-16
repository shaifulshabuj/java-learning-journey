# Day 21: Week 3 Capstone Project - Enterprise Task Management System (July 30, 2025)

**Date:** July 30, 2025  
**Duration:** 3 hours  
**Focus:** Build a complete enterprise application integrating JDBC, Spring Framework, Spring Boot, Spring MVC, and Spring Data JPA

---

## 🎯 Capstone Project Goals

By the end of this capstone, you'll have built:
- ✅ Complete full-stack enterprise web application
- ✅ Database design with complex relationships
- ✅ RESTful APIs with proper HTTP semantics
- ✅ Web interface with forms and validation
- ✅ Authentication and authorization
- ✅ Real-time features and notifications
- ✅ Production-ready deployment configuration

---

## 🏗️ Project Overview: Enterprise Task Management System

### **System Architecture**

**Technology Stack:**
- **Backend**: Spring Boot 2.7+, Spring Data JPA, Spring Security
- **Database**: H2 (development), PostgreSQL (production)
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Build Tool**: Maven
- **Additional**: WebSockets, Actuator, Validation

**System Features:**
1. **User Management**: Registration, authentication, roles
2. **Project Management**: Create projects, assign teams
3. **Task Management**: CRUD operations, status tracking
4. **Real-time Updates**: WebSocket notifications
5. **Reporting**: Dashboard with analytics
6. **File Management**: Document attachments
7. **API**: RESTful services for mobile/integration

### **Database Schema Design**

```sql
-- Users and Authentication
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- Projects and Teams
CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'PLANNING',
    owner_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_members (
    project_id BIGINT REFERENCES projects(id),
    user_id BIGINT REFERENCES users(id),
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id)
);

-- Tasks and Assignments
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    project_id BIGINT REFERENCES projects(id),
    assignee_id BIGINT REFERENCES users(id),
    reporter_id BIGINT REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'TODO',
    priority VARCHAR(10) DEFAULT 'MEDIUM',
    due_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Comments and Activity
CREATE TABLE task_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT REFERENCES tasks(id),
    user_id BIGINT REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- File Attachments
CREATE TABLE attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT REFERENCES tasks(id),
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    uploaded_by BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 💻 Part 1: Application Foundation and Configuration (45 minutes)

### **Main Application Class and Configuration**

Create `TaskManagementApplication.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Enterprise Task Management System - Spring Boot Application
 * 
 * Features:
 * - User authentication and authorization
 * - Project and task management
 * - Real-time notifications
 * - File attachments
 * - Comprehensive reporting
 * - RESTful API
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableWebSocket
@EnableTransactionManagement
@EnableConfigurationProperties
public class TaskManagementApplication {
    
    public static void main(String[] args) {
        System.out.println("=== Enterprise Task Management System ===");
        System.out.println("🚀 Starting application...\n");
        
        SpringApplication app = new SpringApplication(TaskManagementApplication.class);
        
        // Set default properties
        System.setProperty("spring.application.name", "task-management-system");
        System.setProperty("server.port", "8080");
        System.setProperty("spring.profiles.active", "development");
        
        app.run(args);
    }
    
    @PostConstruct
    public void init() {
        // Set default timezone
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
        System.out.println("✅ Application timezone set to UTC");
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

/**
 * Application Configuration
 */
@Configuration
class ApplicationConfig {
    
    @Bean
    public org.springframework.boot.context.properties.ConfigurationProperties taskManagementProperties() {
        return new TaskManagementProperties();
    }
}

/**
 * Application Properties
 */
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "app.task-management")
class TaskManagementProperties {
    
    private String appName = "Task Management System";
    private String version = "1.0.0";
    private int maxFileSize = 10485760; // 10MB
    private String uploadsDirectory = "./uploads";
    private boolean enableNotifications = true;
    private boolean enableEmailNotifications = false;
    
    // Getters and setters
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public int getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(int maxFileSize) { this.maxFileSize = maxFileSize; }
    
    public String getUploadsDirectory() { return uploadsDirectory; }
    public void setUploadsDirectory(String uploadsDirectory) { this.uploadsDirectory = uploadsDirectory; }
    
    public boolean isEnableNotifications() { return enableNotifications; }
    public void setEnableNotifications(boolean enableNotifications) { this.enableNotifications = enableNotifications; }
    
    public boolean isEnableEmailNotifications() { return enableEmailNotifications; }
    public void setEnableEmailNotifications(boolean enableEmailNotifications) { this.enableEmailNotifications = enableEmailNotifications; }
}
```

### **Domain Entities**

Create comprehensive entity classes:

```java
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base entity with auditing capabilities
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

/**
 * User entity with Spring Security integration
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email")
})
class User extends BaseEntity implements UserDetails {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Column(nullable = false)
    private String password;
    
    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
    
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Project> ownedProjects = new ArrayList<>();
    
    @OneToMany(mappedBy = "assignee")
    private List<Task> assignedTasks = new ArrayList<>();
    
    @OneToMany(mappedBy = "reporter")
    private List<Task> reportedTasks = new ArrayList<>();
    
    // Spring Security UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { return enabled; }
    
    // Business methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    
    public List<Project> getOwnedProjects() { return ownedProjects; }
    public void setOwnedProjects(List<Project> ownedProjects) { this.ownedProjects = ownedProjects; }
    
    public List<Task> getAssignedTasks() { return assignedTasks; }
    public void setAssignedTasks(List<Task> assignedTasks) { this.assignedTasks = assignedTasks; }
    
    public List<Task> getReportedTasks() { return reportedTasks; }
    public void setReportedTasks(List<Task> reportedTasks) { this.reportedTasks = reportedTasks; }
}

/**
 * Role entity for authorization
 */
@Entity
@Table(name = "roles")
class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(length = 200)
    private String description;
    
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
    
    public Role() {}
    
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }
}

/**
 * Project entity
 */
@Entity
@Table(name = "projects")
class Project extends BaseEntity {
    
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.PLANNING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectMember> members = new HashSet<>();
    
    // Business methods
    public int getTaskCount() {
        return tasks.size();
    }
    
    public int getCompletedTaskCount() {
        return (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();
    }
    
    public double getCompletionPercentage() {
        if (tasks.isEmpty()) return 0.0;
        return (double) getCompletedTaskCount() / tasks.size() * 100;
    }
    
    public boolean isOverdue() {
        return endDate != null && endDate.isBefore(LocalDate.now()) && 
               status != ProjectStatus.COMPLETED;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    
    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }
    
    public Set<ProjectMember> getMembers() { return members; }
    public void setMembers(Set<ProjectMember> members) { this.members = members; }
}

/**
 * Project member association entity
 */
@Entity
@Table(name = "project_members")
class ProjectMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role = ProjectRole.MEMBER;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public ProjectRole getRole() { return role; }
    public void setRole(ProjectRole role) { this.role = role; }
    
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}

/**
 * Task entity
 */
@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_task_status", columnList = "status"),
    @Index(name = "idx_task_priority", columnList = "priority"),
    @Index(name = "idx_task_due_date", columnList = "due_date")
})
class Task extends BaseEntity {
    
    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Task title cannot exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskComment> comments = new ArrayList<>();
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();
    
    // Business methods
    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDateTime.now()) && 
               status != TaskStatus.COMPLETED;
    }
    
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }
    
    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public List<TaskComment> getComments() { return comments; }
    public void setComments(List<TaskComment> comments) { this.comments = comments; }
    
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }
}

/**
 * Task comment entity
 */
@Entity
@Table(name = "task_comments")
class TaskComment extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank(message = "Comment content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    // Getters and setters
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

/**
 * File attachment entity
 */
@Entity
@Table(name = "attachments")
class Attachment extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    
    @Column(nullable = false)
    private String filename;
    
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
    
    // Getters and setters
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
}

// Enums
enum ProjectStatus {
    PLANNING("Planning"),
    ACTIVE("Active"),
    ON_HOLD("On Hold"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}

enum ProjectRole {
    OWNER("Owner"),
    MANAGER("Manager"),
    MEMBER("Member"),
    VIEWER("Viewer");
    
    private final String displayName;
    
    ProjectRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}

enum TaskStatus {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
    
    public String getBadgeClass() {
        return switch (this) {
            case TODO -> "bg-secondary";
            case IN_PROGRESS -> "bg-primary";
            case IN_REVIEW -> "bg-warning";
            case COMPLETED -> "bg-success";
            case CANCELLED -> "bg-danger";
        };
    }
}

enum TaskPriority {
    LOW("Low", "bg-secondary"),
    MEDIUM("Medium", "bg-primary"),
    HIGH("High", "bg-warning"),
    URGENT("Urgent", "bg-danger");
    
    private final String displayName;
    private final String badgeClass;
    
    TaskPriority(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }
    
    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
}
```

---

## 🗄️ Part 2: Data Access Layer (45 minutes)

### **Repository Interfaces**

Create comprehensive repository interfaces with custom queries:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Repository with authentication support
 */
@Repository
interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByEnabledTrue();
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<User> searchUsers(@Param("search") String search);
}

/**
 * Role Repository
 */
@Repository
interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByName(String name);
    
    boolean existsByName(String name);
}

/**
 * Project Repository with analytics queries
 */
@Repository
interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    
    List<Project> findByOwner(User owner);
    
    List<Project> findByStatus(ProjectStatus status);
    
    @Query("SELECT p FROM Project p JOIN p.members pm WHERE pm.user = :user")
    List<Project> findByMember(@Param("user") User user);
    
    @Query("SELECT p FROM Project p WHERE p.owner = :user OR EXISTS " +
           "(SELECT pm FROM ProjectMember pm WHERE pm.project = p AND pm.user = :user)")
    List<Project> findByOwnerOrMember(@Param("user") User user);
    
    @Query("SELECT p FROM Project p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Project> searchProjects(@Param("search") String search);
    
    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> getProjectStatusStatistics();
    
    @Query("SELECT p FROM Project p WHERE p.endDate < :date AND p.status != 'COMPLETED'")
    List<Project> findOverdueProjects(@Param("date") java.time.LocalDate date);
    
    @Query("SELECT p FROM Project p ORDER BY p.createdAt DESC")
    List<Project> findRecentProjects(Pageable pageable);
}

/**
 * Project Member Repository
 */
@Repository
interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    
    List<ProjectMember> findByProject(Project project);
    
    List<ProjectMember> findByUser(User user);
    
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
    
    boolean existsByProjectAndUser(Project project, User user);
    
    void deleteByProjectAndUser(Project project, User user);
    
    @Query("SELECT pm.user FROM ProjectMember pm WHERE pm.project = :project AND pm.role = :role")
    List<User> findUsersByProjectAndRole(@Param("project") Project project, @Param("role") ProjectRole role);
}

/**
 * Task Repository with comprehensive queries
 */
@Repository
interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    
    List<Task> findByProject(Project project);
    
    List<Task> findByAssignee(User assignee);
    
    List<Task> findByReporter(User reporter);
    
    List<Task> findByStatus(TaskStatus status);
    
    List<Task> findByPriority(TaskPriority priority);
    
    @Query("SELECT t FROM Task t WHERE t.assignee = :user AND t.status != 'COMPLETED'")
    List<Task> findActiveTasksByAssignee(@Param("user") User user);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate < :date AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasks(@Param("date") LocalDateTime date);
    
    @Query("SELECT t FROM Task t WHERE t.project = :project AND t.status = :status")
    List<Task> findByProjectAndStatus(@Param("project") Project project, @Param("status") TaskStatus status);
    
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.project = :project GROUP BY t.status")
    List<Object[]> getTaskStatusStatistics(@Param("project") Project project);
    
    @Query("SELECT t.priority, COUNT(t) FROM Task t WHERE t.project = :project GROUP BY t.priority")
    List<Object[]> getTaskPriorityStatistics(@Param("project") Project project);
    
    @Query("SELECT DATE(t.createdAt), COUNT(t) FROM Task t " +
           "WHERE t.createdAt >= :startDate AND t.createdAt <= :endDate " +
           "GROUP BY DATE(t.createdAt) ORDER BY DATE(t.createdAt)")
    List<Object[]> getTaskCreationTrend(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Task t WHERE " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Task> searchTasks(@Param("search") String search);
    
    // Dashboard queries
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee = :user")
    Long countByAssignee(@Param("user") User user);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee = :user AND t.status = 'COMPLETED'")
    Long countCompletedByAssignee(@Param("user") User user);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee = :user AND t.dueDate < :date AND t.status != 'COMPLETED'")
    Long countOverdueByAssignee(@Param("user") User user, @Param("date") LocalDateTime date);
    
    @Modifying
    @Query("UPDATE Task t SET t.status = :status WHERE t.id = :taskId")
    int updateTaskStatus(@Param("taskId") Long taskId, @Param("status") TaskStatus status);
}

/**
 * Task Comment Repository
 */
@Repository
interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    
    List<TaskComment> findByTask(Task task);
    
    List<TaskComment> findByUser(User user);
    
    @Query("SELECT tc FROM TaskComment tc WHERE tc.task = :task ORDER BY tc.createdAt ASC")
    List<TaskComment> findByTaskOrderByCreatedAt(@Param("task") Task task);
    
    Long countByTask(Task task);
}

/**
 * Attachment Repository
 */
@Repository
interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    
    List<Attachment> findByTask(Task task);
    
    List<Attachment> findByUploadedBy(User user);
    
    @Query("SELECT SUM(a.fileSize) FROM Attachment a WHERE a.uploadedBy = :user")
    Long getTotalFileSizeByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.task.project = :project")
    Long countByProject(@Param("project") Project project);
}
```

### **Service Layer Implementation**

Create comprehensive service classes:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User Service with Spring Security integration
 */
@Service
@Transactional
class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) 
            throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    public User registerUser(UserRegistrationRequest request) {
        // Validate unique constraints
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        
        // Assign default role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default USER role not found"));
        user.getRoles().add(userRole);
        
        return userRepository.save(user);
    }
    
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        
        return userRepository.save(user);
    }
    
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    public List<User> searchUsers(String search) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findByEnabledTrue();
        }
        return userRepository.searchUsers(search.trim());
    }
    
    public List<User> getAllActiveUsers() {
        return userRepository.findByEnabledTrue();
    }
    
    public void initializeDefaultRoles() {
        if (!roleRepository.existsByName("ADMIN")) {
            roleRepository.save(new Role("ADMIN", "System Administrator"));
        }
        if (!roleRepository.existsByName("MANAGER")) {
            roleRepository.save(new Role("MANAGER", "Project Manager"));
        }
        if (!roleRepository.existsByName("USER")) {
            roleRepository.save(new Role("USER", "Regular User"));
        }
    }
}

/**
 * Project Service with comprehensive project management
 */
@Service
@Transactional
class ProjectService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private ProjectMemberRepository projectMemberRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Project createProject(ProjectCreateRequest request, User owner) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(ProjectStatus.PLANNING);
        project.setOwner(owner);
        
        Project savedProject = projectRepository.save(project);
        
        // Add owner as project member with OWNER role
        addProjectMember(savedProject, owner, ProjectRole.OWNER);
        
        return savedProject;
    }
    
    public Project updateProject(Long projectId, ProjectUpdateRequest request, User user) {
        Project project = findById(projectId);
        
        // Check permissions
        if (!canEditProject(project, user)) {
            throw new SecurityException("User does not have permission to edit this project");
        }
        
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(request.getStatus());
        
        return projectRepository.save(project);
    }
    
    public void deleteProject(Long projectId, User user) {
        Project project = findById(projectId);
        
        if (!project.getOwner().equals(user) && !user.hasRole("ADMIN")) {
            throw new SecurityException("Only project owner or admin can delete the project");
        }
        
        projectRepository.delete(project);
    }
    
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));
    }
    
    public List<Project> findUserProjects(User user) {
        return projectRepository.findByOwnerOrMember(user);
    }
    
    public List<Project> searchProjects(String search) {
        if (search == null || search.trim().isEmpty()) {
            return projectRepository.findAll();
        }
        return projectRepository.searchProjects(search.trim());
    }
    
    public Page<Project> findProjects(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }
    
    public void addProjectMember(Project project, User user, ProjectRole role) {
        if (projectMemberRepository.existsByProjectAndUser(project, user)) {
            throw new IllegalArgumentException("User is already a member of this project");
        }
        
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        
        projectMemberRepository.save(member);
    }
    
    public void removeProjectMember(Project project, User user) {
        if (project.getOwner().equals(user)) {
            throw new IllegalArgumentException("Cannot remove project owner from the project");
        }
        
        projectMemberRepository.deleteByProjectAndUser(project, user);
    }
    
    public List<User> getProjectMembers(Project project) {
        return projectMemberRepository.findByProject(project)
                .stream()
                .map(ProjectMember::getUser)
                .collect(Collectors.toList());
    }
    
    public boolean canEditProject(Project project, User user) {
        return project.getOwner().equals(user) || 
               user.hasRole("ADMIN") ||
               isProjectManager(project, user);
    }
    
    public boolean isProjectMember(Project project, User user) {
        return projectMemberRepository.existsByProjectAndUser(project, user);
    }
    
    public boolean isProjectManager(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user)
                .map(member -> member.getRole() == ProjectRole.MANAGER)
                .orElse(false);
    }
    
    public ProjectStatistics getProjectStatistics(Project project) {
        List<Object[]> statusStats = projectRepository.getProjectStatusStatistics();
        // Implementation for detailed project statistics
        return new ProjectStatistics(project, statusStats);
    }
    
    public List<Project> getRecentProjects(int limit) {
        return projectRepository.findRecentProjects(PageRequest.of(0, limit));
    }
    
    public List<Project> getOverdueProjects() {
        return projectRepository.findOverdueProjects(LocalDate.now());
    }
}

/**
 * Task Service with comprehensive task management
 */
@Service
@Transactional
class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskCommentRepository commentRepository;
    
    @Autowired
    private AttachmentRepository attachmentRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    public Task createTask(TaskCreateRequest request, User reporter) {
        Project project = projectService.findById(request.getProjectId());
        
        // Check if user can create tasks in this project
        if (!projectService.isProjectMember(project, reporter)) {
            throw new SecurityException("User is not a member of this project");
        }
        
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setProject(project);
        task.setReporter(reporter);
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.TODO);
        
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            
            // Check if assignee is project member
            if (!projectService.isProjectMember(project, assignee)) {
                throw new IllegalArgumentException("Assignee must be a project member");
            }
            
            task.setAssignee(assignee);
        }
        
        Task savedTask = taskRepository.save(task);
        
        // Send notification
        if (savedTask.getAssignee() != null) {
            notificationService.sendTaskAssignedNotification(savedTask);
        }
        
        return savedTask;
    }
    
    public Task updateTask(Long taskId, TaskUpdateRequest request, User user) {
        Task task = findById(taskId);
        
        if (!canEditTask(task, user)) {
            throw new SecurityException("User does not have permission to edit this task");
        }
        
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        
        // Handle assignee change
        if (request.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            
            if (!newAssignee.equals(task.getAssignee())) {
                task.setAssignee(newAssignee);
                notificationService.sendTaskReassignedNotification(task);
            }
        } else {
            task.setAssignee(null);
        }
        
        return taskRepository.save(task);
    }
    
    public Task updateTaskStatus(Long taskId, TaskStatus status, User user) {
        Task task = findById(taskId);
        
        if (!canEditTask(task, user)) {
            throw new SecurityException("User does not have permission to update this task");
        }
        
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(status);
        
        if (status == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
            notificationService.sendTaskCompletedNotification(task);
        }
        
        Task savedTask = taskRepository.save(task);
        
        // Log status change as comment
        addSystemComment(savedTask, user, "Status changed from " + oldStatus.getDisplayName() + 
                        " to " + status.getDisplayName());
        
        return savedTask;
    }
    
    public void deleteTask(Long taskId, User user) {
        Task task = findById(taskId);
        
        if (!canDeleteTask(task, user)) {
            throw new SecurityException("User does not have permission to delete this task");
        }
        
        taskRepository.delete(task);
    }
    
    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
    }
    
    public List<Task> findUserTasks(User user) {
        return taskRepository.findByAssignee(user);
    }
    
    public List<Task> findProjectTasks(Project project) {
        return taskRepository.findByProject(project);
    }
    
    public List<Task> searchTasks(String search) {
        if (search == null || search.trim().isEmpty()) {
            return taskRepository.findAll();
        }
        return taskRepository.searchTasks(search.trim());
    }
    
    public Page<Task> findTasks(TaskSearchCriteria criteria, Pageable pageable) {
        Specification<Task> spec = buildTaskSpecification(criteria);
        return taskRepository.findAll(spec, pageable);
    }
    
    public TaskComment addComment(Long taskId, String content, User user) {
        Task task = findById(taskId);
        
        if (!projectService.isProjectMember(task.getProject(), user)) {
            throw new SecurityException("User is not a member of this project");
        }
        
        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setContent(content);
        
        return commentRepository.save(comment);
    }
    
    private void addSystemComment(Task task, User user, String content) {
        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setContent("[System] " + content);
        commentRepository.save(comment);
    }
    
    public List<Task> getOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDateTime.now());
    }
    
    public List<Task> getUserOverdueTasks(User user) {
        return taskRepository.findOverdueTasks(LocalDateTime.now())
                .stream()
                .filter(task -> user.equals(task.getAssignee()))
                .collect(Collectors.toList());
    }
    
    public TaskStatistics getUserTaskStatistics(User user) {
        Long totalTasks = taskRepository.countByAssignee(user);
        Long completedTasks = taskRepository.countCompletedByAssignee(user);
        Long overdueTasks = taskRepository.countOverdueByAssignee(user, LocalDateTime.now());
        
        return new TaskStatistics(totalTasks, completedTasks, overdueTasks);
    }
    
    private boolean canEditTask(Task task, User user) {
        return task.getReporter().equals(user) ||
               task.getAssignee() != null && task.getAssignee().equals(user) ||
               projectService.canEditProject(task.getProject(), user);
    }
    
    private boolean canDeleteTask(Task task, User user) {
        return task.getReporter().equals(user) ||
               projectService.canEditProject(task.getProject(), user) ||
               user.hasRole("ADMIN");
    }
    
    private Specification<Task> buildTaskSpecification(TaskSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (criteria.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), criteria.getProjectId()));
            }
            
            if (criteria.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), criteria.getAssigneeId()));
            }
            
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            
            if (criteria.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), criteria.getPriority()));
            }
            
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                String searchPattern = "%" + criteria.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern)
                ));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    @Autowired
    private UserRepository userRepository;
}

/**
 * File Management Service
 */
@Service
@Transactional
class FileService {
    
    @Autowired
    private AttachmentRepository attachmentRepository;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskManagementProperties properties;
    
    public Attachment uploadFile(Long taskId, MultipartFile file, User user) throws IOException {
        Task task = taskService.findById(taskId);
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }
        
        // Create uploads directory if it doesn't exist
        Path uploadsDir = Paths.get(properties.getUploadsDirectory());
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadsDir.resolve(uniqueFilename);
        
        // Save file
        Files.copy(file.getInputStream(), filePath);
        
        // Create attachment record
        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setFilename(originalFilename);
        attachment.setFilePath(filePath.toString());
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setUploadedBy(user);
        
        return attachmentRepository.save(attachment);
    }
    
    public void deleteFile(Long attachmentId, User user) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        
        // Check permissions
        if (!attachment.getUploadedBy().equals(user) && 
            !taskService.canEditTask(attachment.getTask(), user)) {
            throw new SecurityException("User does not have permission to delete this file");
        }
        
        // Delete physical file
        Path filePath = Paths.get(attachment.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        
        // Delete attachment record
        attachmentRepository.delete(attachment);
    }
    
    public Attachment findById(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }
    
    public List<Attachment> getTaskAttachments(Task task) {
        return attachmentRepository.findByTask(task);
    }
}

/**
 * Notification Service
 */
@Service
class NotificationService {
    
    @Autowired
    private TaskManagementProperties properties;
    
    // WebSocket support would be implemented here
    // For this demo, we'll use simple logging
    
    public void sendTaskAssignedNotification(Task task) {
        if (!properties.isEnableNotifications()) return;
        
        String message = String.format("Task '%s' has been assigned to you in project '%s'",
                task.getTitle(), task.getProject().getName());
        
        System.out.println("📧 Notification to " + task.getAssignee().getEmail() + ": " + message);
        
        // In a real application, this would send email, push notification, or WebSocket message
    }
    
    public void sendTaskReassignedNotification(Task task) {
        if (!properties.isEnableNotifications()) return;
        
        String message = String.format("Task '%s' has been reassigned to you", task.getTitle());
        
        System.out.println("📧 Notification to " + task.getAssignee().getEmail() + ": " + message);
    }
    
    public void sendTaskCompletedNotification(Task task) {
        if (!properties.isEnableNotifications()) return;
        
        String message = String.format("Task '%s' has been completed", task.getTitle());
        
        System.out.println("📧 Notification to " + task.getReporter().getEmail() + ": " + message);
    }
}

// DTOs and Request/Response classes
class UserRegistrationRequest {
    @NotBlank private String username;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 6) private String password;
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}

class UserUpdateRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank @Email private String email;
    
    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class ProjectCreateRequest {
    @NotBlank private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}

class ProjectUpdateRequest extends ProjectCreateRequest {
    private ProjectStatus status;
    
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
}

class TaskCreateRequest {
    @NotBlank private String title;
    private String description;
    @NotNull private Long projectId;
    private Long assigneeId;
    private TaskPriority priority = TaskPriority.MEDIUM;
    private LocalDateTime dueDate;
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}

class TaskUpdateRequest {
    @NotBlank private String title;
    private String description;
    private Long assigneeId;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}

class TaskSearchCriteria {
    private Long projectId;
    private Long assigneeId;
    private TaskStatus status;
    private TaskPriority priority;
    private String search;
    
    // Getters and setters
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }
}

// Statistics classes
class ProjectStatistics {
    private Project project;
    private Map<ProjectStatus, Long> statusCounts;
    
    public ProjectStatistics(Project project, List<Object[]> statusStats) {
        this.project = project;
        this.statusCounts = statusStats.stream()
                .collect(Collectors.toMap(
                    stat -> (ProjectStatus) stat[0],
                    stat -> (Long) stat[1]
                ));
    }
    
    public Project getProject() { return project; }
    public Map<ProjectStatus, Long> getStatusCounts() { return statusCounts; }
}

class TaskStatistics {
    private Long totalTasks;
    private Long completedTasks;
    private Long overdueTasks;
    
    public TaskStatistics(Long totalTasks, Long completedTasks, Long overdueTasks) {
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.overdueTasks = overdueTasks;
    }
    
    public Long getTotalTasks() { return totalTasks; }
    public Long getCompletedTasks() { return completedTasks; }
    public Long getOverdueTasks() { return overdueTasks; }
    
    public double getCompletionPercentage() {
        if (totalTasks == 0) return 0.0;
        return (double) completedTasks / totalTasks * 100;
    }
}
```

---

## 🔐 Part 3: Security Configuration (30 minutes)

### **Spring Security Implementation**

Create comprehensive security configuration:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security Configuration
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(requests -> requests
                // Public endpoints
                .antMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**", 
                           "/webjars/**", "/h2-console/**").permitAll()
                
                // API endpoints
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/**").authenticated()
                
                // Admin endpoints
                .antMatchers("/admin/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(86400) // 24 hours
                .key("uniqueAndSecret")
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .csrf(csrf -> csrf
                .ignoringAntMatchers("/api/**", "/h2-console/**")
            )
            .headers(headers -> headers
                .frameOptions().sameOrigin() // For H2 console
            );
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder);
    }
    
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }
}

/**
 * Custom authentication success handler
 */
class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      org.springframework.security.core.Authentication authentication)
            throws IOException, ServletException {
        
        // Get user authorities
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_MANAGER"));
        
        // Redirect based on role
        if (isAdmin) {
            setDefaultTargetUrl("/admin/dashboard");
        } else if (isManager) {
            setDefaultTargetUrl("/manager/dashboard");
        } else {
            setDefaultTargetUrl("/dashboard");
        }
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

/**
 * Data initialization service
 */
@org.springframework.stereotype.Service
class DataInitializationService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskService taskService;
    
    @org.springframework.boot.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initializeData() {
        System.out.println("🔧 Initializing application data...");
        
        try {
            // Initialize roles
            userService.initializeDefaultRoles();
            
            // Create admin user if it doesn't exist
            createAdminUser();
            
            // Create sample data
            createSampleData();
            
            System.out.println("✅ Application data initialized successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing data: " + e.getMessage());
        }
    }
    
    private void createAdminUser() {
        try {
            userService.findByUsername("admin");
        } catch (RuntimeException e) {
            // Admin user doesn't exist, create it
            UserRegistrationRequest adminRequest = new UserRegistrationRequest();
            adminRequest.setUsername("admin");
            adminRequest.setEmail("admin@taskmanagement.com");
            adminRequest.setPassword("admin123");
            adminRequest.setFirstName("System");
            adminRequest.setLastName("Administrator");
            
            User admin = userService.registerUser(adminRequest);
            
            // Add admin role
            admin.getRoles().clear();
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
            
            System.out.println("✅ Admin user created: admin/admin123");
        }
    }
    
    private void createSampleData() {
        try {
            // Create sample users
            User manager = createSampleUser("manager", "manager@example.com", "John", "Manager", "MANAGER");
            User user1 = createSampleUser("user1", "user1@example.com", "Alice", "Developer", "USER");
            User user2 = createSampleUser("user2", "user2@example.com", "Bob", "Designer", "USER");
            
            // Create sample project
            ProjectCreateRequest projectRequest = new ProjectCreateRequest();
            projectRequest.setName("Task Management System Development");
            projectRequest.setDescription("Development of the enterprise task management system");
            projectRequest.setStartDate(LocalDate.now().minusDays(30));
            projectRequest.setEndDate(LocalDate.now().plusDays(60));
            
            Project project = projectService.createProject(projectRequest, manager);
            
            // Add project members
            projectService.addProjectMember(project, user1, ProjectRole.MEMBER);
            projectService.addProjectMember(project, user2, ProjectRole.MEMBER);
            
            // Create sample tasks
            createSampleTask("Setup development environment", 
                           "Configure development tools and environments", 
                           project, user1, manager, TaskPriority.HIGH, TaskStatus.COMPLETED);
            
            createSampleTask("Design user interface mockups", 
                           "Create wireframes and mockups for the application", 
                           project, user2, manager, TaskPriority.MEDIUM, TaskStatus.COMPLETED);
            
            createSampleTask("Implement user authentication", 
                           "Develop login, registration, and security features", 
                           project, user1, manager, TaskPriority.HIGH, TaskStatus.IN_PROGRESS);
            
            createSampleTask("Create project management features", 
                           "Implement project creation and management functionality", 
                           project, user1, manager, TaskPriority.MEDIUM, TaskStatus.TODO);
            
            createSampleTask("Develop task management system", 
                           "Build task creation, assignment, and tracking features", 
                           project, user2, manager, TaskPriority.HIGH, TaskStatus.TODO);
            
            System.out.println("✅ Sample data created successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error creating sample data: " + e.getMessage());
        }
    }
    
    private User createSampleUser(String username, String email, String firstName, String lastName, String roleName) {
        try {
            return userService.findByUsername(username);
        } catch (RuntimeException e) {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setUsername(username);
            request.setEmail(email);
            request.setPassword("password123");
            request.setFirstName(firstName);
            request.setLastName(lastName);
            
            User user = userService.registerUser(request);
            
            // Add specific role if needed
            if (!roleName.equals("USER")) {
                Role role = roleRepository.findByName(roleName).orElseThrow();
                user.getRoles().add(role);
                userRepository.save(user);
            }
            
            return user;
        }
    }
    
    private void createSampleTask(String title, String description, Project project, 
                                User assignee, User reporter, TaskPriority priority, TaskStatus status) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setProjectId(project.getId());
        request.setAssigneeId(assignee.getId());
        request.setPriority(priority);
        
        Task task = taskService.createTask(request, reporter);
        task.setStatus(status);
        if (status == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now().minusDays((long) (Math.random() * 10)));
        }
        taskRepository.save(task);
    }
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TaskRepository taskRepository;
}
```

---

## 🌐 Part 4: Web Controllers and REST APIs (45 minutes)

### **Web Controllers Implementation**

Create comprehensive web controllers:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

/**
 * Main Dashboard Controller
 */
@Controller
class DashboardController {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {
        // User's projects
        List<Project> userProjects = projectService.findUserProjects(currentUser);
        
        // User's tasks
        List<Task> userTasks = taskService.findUserTasks(currentUser);
        List<Task> overdueTasks = taskService.getUserOverdueTasks(currentUser);
        
        // Statistics
        TaskStatistics taskStats = taskService.getUserTaskStatistics(currentUser);
        
        // Recent projects
        List<Project> recentProjects = projectService.getRecentProjects(5);
        
        model.addAttribute("title", "Dashboard");
        model.addAttribute("userProjects", userProjects);
        model.addAttribute("userTasks", userTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("taskStats", taskStats);
        model.addAttribute("recentProjects", recentProjects);
        
        return "dashboard/index";
    }
    
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        // System-wide statistics
        List<User> allUsers = userService.getAllActiveUsers();
        List<Project> allProjects = projectService.getRecentProjects(10);
        List<Project> overdueProjects = projectService.getOverdueProjects();
        List<Task> overdueTasks = taskService.getOverdueTasks();
        
        model.addAttribute("title", "Admin Dashboard");
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalProjects", allProjects.size());
        model.addAttribute("overdueProjects", overdueProjects);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("recentProjects", allProjects);
        
        return "admin/dashboard";
    }
}

/**
 * Authentication Controller
 */
@Controller
class AuthController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                           @RequestParam(required = false) String logout,
                           Model model) {
        
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password");
        }
        
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully");
        }
        
        model.addAttribute("title", "Login");
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("title", "Register");
        model.addAttribute("userRegistration", new UserRegistrationRequest());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userRegistration") UserRegistrationRequest request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Register");
            return "auth/register";
        }
        
        try {
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Registration successful! Please log in with your credentials.");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("title", "Register");
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
}

/**
 * Project Management Controller
 */
@Controller
@RequestMapping("/projects")
class ProjectController {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listProjects(@AuthenticationPrincipal User currentUser,
                              @RequestParam(required = false) String search,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        
        List<Project> projects;
        if (search != null && !search.trim().isEmpty()) {
            projects = projectService.searchProjects(search);
        } else {
            projects = projectService.findUserProjects(currentUser);
        }
        
        model.addAttribute("title", "Projects");
        model.addAttribute("projects", projects);
        model.addAttribute("search", search);
        
        return "projects/list";
    }
    
    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             Model model) {
        
        Project project = projectService.findById(id);
        
        // Check if user has access to this project
        if (!projectService.isProjectMember(project, currentUser) && !currentUser.hasRole("ADMIN")) {
            throw new SecurityException("Access denied");
        }
        
        List<Task> projectTasks = taskService.findProjectTasks(project);
        List<User> projectMembers = projectService.getProjectMembers(project);
        boolean canEdit = projectService.canEditProject(project, currentUser);
        
        model.addAttribute("title", project.getName());
        model.addAttribute("project", project);
        model.addAttribute("tasks", projectTasks);
        model.addAttribute("members", projectMembers);
        model.addAttribute("canEdit", canEdit);
        
        return "projects/view";
    }
    
    @GetMapping("/new")
    public String newProject(Model model) {
        model.addAttribute("title", "Create Project");
        model.addAttribute("projectCreate", new ProjectCreateRequest());
        return "projects/form";
    }
    
    @PostMapping
    public String createProject(@Valid @ModelAttribute("projectCreate") ProjectCreateRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User currentUser,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Create Project");
            return "projects/form";
        }
        
        try {
            Project project = projectService.createProject(request, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Project '" + project.getName() + "' created successfully!");
            return "redirect:/projects/" + project.getId();
            
        } catch (Exception e) {
            model.addAttribute("title", "Create Project");
            model.addAttribute("errorMessage", e.getMessage());
            return "projects/form";
        }
    }
    
    @GetMapping("/{id}/edit")
    public String editProject(@PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             Model model) {
        
        Project project = projectService.findById(id);
        
        if (!projectService.canEditProject(project, currentUser)) {
            throw new SecurityException("Access denied");
        }
        
        ProjectUpdateRequest updateRequest = new ProjectUpdateRequest();
        updateRequest.setName(project.getName());
        updateRequest.setDescription(project.getDescription());
        updateRequest.setStartDate(project.getStartDate());
        updateRequest.setEndDate(project.getEndDate());
        updateRequest.setStatus(project.getStatus());
        
        model.addAttribute("title", "Edit Project");
        model.addAttribute("project", project);
        model.addAttribute("projectUpdate", updateRequest);
        model.addAttribute("statuses", ProjectStatus.values());
        
        return "projects/edit";
    }
    
    @PostMapping("/{id}")
    public String updateProject(@PathVariable Long id,
                               @Valid @ModelAttribute("projectUpdate") ProjectUpdateRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User currentUser,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            Project project = projectService.findById(id);
            model.addAttribute("title", "Edit Project");
            model.addAttribute("project", project);
            model.addAttribute("statuses", ProjectStatus.values());
            return "projects/edit";
        }
        
        try {
            Project project = projectService.updateProject(id, request, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Project '" + project.getName() + "' updated successfully!");
            return "redirect:/projects/" + id;
            
        } catch (Exception e) {
            Project project = projectService.findById(id);
            model.addAttribute("title", "Edit Project");
            model.addAttribute("project", project);
            model.addAttribute("statuses", ProjectStatus.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "projects/edit";
        }
    }
    
    @PostMapping("/{id}/members")
    public String addProjectMember(@PathVariable Long id,
                                  @RequestParam Long userId,
                                  @RequestParam ProjectRole role,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttributes) {
        
        try {
            Project project = projectService.findById(id);
            User user = userService.findById(userId);
            
            if (!projectService.canEditProject(project, currentUser)) {
                throw new SecurityException("Access denied");
            }
            
            projectService.addProjectMember(project, user, role);
            redirectAttributes.addFlashAttribute("successMessage", 
                user.getFullName() + " added to project successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/projects/" + id;
    }
}

/**
 * Task Management Controller
 */
@Controller
@RequestMapping("/tasks")
class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FileService fileService;
    
    @GetMapping
    public String listTasks(@AuthenticationPrincipal User currentUser,
                           @RequestParam(required = false) Long projectId,
                           @RequestParam(required = false) TaskStatus status,
                           @RequestParam(required = false) TaskPriority priority,
                           @RequestParam(required = false) String search,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        criteria.setProjectId(projectId);
        criteria.setStatus(status);
        criteria.setPriority(priority);
        criteria.setSearch(search);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Task> tasks = taskService.findTasks(criteria, pageable);
        
        // Get user's projects for filter
        List<Project> userProjects = projectService.findUserProjects(currentUser);
        
        model.addAttribute("title", "Tasks");
        model.addAttribute("tasks", tasks);
        model.addAttribute("userProjects", userProjects);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("criteria", criteria);
        
        return "tasks/list";
    }
    
    @GetMapping("/{id}")
    public String viewTask(@PathVariable Long id,
                          @AuthenticationPrincipal User currentUser,
                          Model model) {
        
        Task task = taskService.findById(id);
        
        // Check access
        if (!projectService.isProjectMember(task.getProject(), currentUser) && !currentUser.hasRole("ADMIN")) {
            throw new SecurityException("Access denied");
        }
        
        List<TaskComment> comments = task.getComments();
        List<Attachment> attachments = fileService.getTaskAttachments(task);
        boolean canEdit = taskService.canEditTask(task, currentUser);
        
        model.addAttribute("title", task.getTitle());
        model.addAttribute("task", task);
        model.addAttribute("comments", comments);
        model.addAttribute("attachments", attachments);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("newComment", "");
        model.addAttribute("statuses", TaskStatus.values());
        
        return "tasks/view";
    }
    
    @GetMapping("/new")
    public String newTask(@RequestParam(required = false) Long projectId,
                         @AuthenticationPrincipal User currentUser,
                         Model model) {
        
        List<Project> userProjects = projectService.findUserProjects(currentUser);
        List<User> users = userService.getAllActiveUsers();
        
        TaskCreateRequest taskCreate = new TaskCreateRequest();
        if (projectId != null) {
            taskCreate.setProjectId(projectId);
        }
        
        model.addAttribute("title", "Create Task");
        model.addAttribute("taskCreate", taskCreate);
        model.addAttribute("projects", userProjects);
        model.addAttribute("users", users);
        model.addAttribute("priorities", TaskPriority.values());
        
        return "tasks/form";
    }
    
    @PostMapping
    public String createTask(@Valid @ModelAttribute("taskCreate") TaskCreateRequest request,
                            BindingResult bindingResult,
                            @AuthenticationPrincipal User currentUser,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            List<Project> userProjects = projectService.findUserProjects(currentUser);
            List<User> users = userService.getAllActiveUsers();
            
            model.addAttribute("title", "Create Task");
            model.addAttribute("projects", userProjects);
            model.addAttribute("users", users);
            model.addAttribute("priorities", TaskPriority.values());
            return "tasks/form";
        }
        
        try {
            Task task = taskService.createTask(request, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Task '" + task.getTitle() + "' created successfully!");
            return "redirect:/tasks/" + task.getId();
            
        } catch (Exception e) {
            List<Project> userProjects = projectService.findUserProjects(currentUser);
            List<User> users = userService.getAllActiveUsers();
            
            model.addAttribute("title", "Create Task");
            model.addAttribute("projects", userProjects);
            model.addAttribute("users", users);
            model.addAttribute("priorities", TaskPriority.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "tasks/form";
        }
    }
    
    @PostMapping("/{id}/status")
    public String updateTaskStatus(@PathVariable Long id,
                                  @RequestParam TaskStatus status,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttributes) {
        
        try {
            taskService.updateTaskStatus(id, status, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Task status updated successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/tasks/" + id;
    }
    
    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable Long id,
                            @RequestParam String content,
                            @AuthenticationPrincipal User currentUser,
                            RedirectAttributes redirectAttributes) {
        
        try {
            if (content.trim().isEmpty()) {
                throw new IllegalArgumentException("Comment cannot be empty");
            }
            
            taskService.addComment(id, content.trim(), currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Comment added successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/tasks/" + id;
    }
    
    @PostMapping("/{id}/attachments")
    public String uploadAttachment(@PathVariable Long id,
                                  @RequestParam("file") MultipartFile file,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttributes) {
        
        try {
            fileService.uploadFile(id, file, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/tasks/" + id;
    }
}

/**
 * User Management Controller
 */
@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listUsers(@RequestParam(required = false) String search, Model model) {
        List<User> users = userService.searchUsers(search);
        
        model.addAttribute("title", "User Management");
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        
        return "admin/users";
    }
    
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        
        model.addAttribute("title", user.getFullName());
        model.addAttribute("user", user);
        
        return "admin/user-details";
    }
}
```

### **REST API Controllers**

Create REST API controllers for mobile and integration:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Projects
 */
@RestController
@RequestMapping("/api/projects")
class ProjectApiController {
    
    @Autowired
    private ProjectService projectService;
    
    @GetMapping
    public ResponseEntity<List<Project>> getUserProjects(@AuthenticationPrincipal User currentUser) {
        List<Project> projects = projectService.findUserProjects(currentUser);
        return ResponseEntity.ok(projects);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id,
                                             @AuthenticationPrincipal User currentUser) {
        try {
            Project project = projectService.findById(id);
            
            if (!projectService.isProjectMember(project, currentUser) && !currentUser.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(project);
            
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody ProjectCreateRequest request,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            Project project = projectService.createProject(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(project);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id,
                                                @Valid @RequestBody ProjectUpdateRequest request,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            Project project = projectService.updateProject(id, request, currentUser);
            return ResponseEntity.ok(project);
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id,
                                             @AuthenticationPrincipal User currentUser) {
        try {
            projectService.deleteProject(id, currentUser);
            return ResponseEntity.noContent().build();
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/statistics")
    public ResponseEntity<ProjectStatistics> getProjectStatistics(@PathVariable Long id,
                                                                 @AuthenticationPrincipal User currentUser) {
        try {
            Project project = projectService.findById(id);
            
            if (!projectService.isProjectMember(project, currentUser) && !currentUser.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            ProjectStatistics stats = projectService.getProjectStatistics(project);
            return ResponseEntity.ok(stats);
            
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

/**
 * REST API for Tasks
 */
@RestController
@RequestMapping("/api/tasks")
class TaskApiController {
    
    @Autowired
    private TaskService taskService;
    
    @GetMapping
    public ResponseEntity<Page<Task>> getTasks(@RequestParam(required = false) Long projectId,
                                              @RequestParam(required = false) TaskStatus status,
                                              @RequestParam(required = false) TaskPriority priority,
                                              @RequestParam(required = false) String search,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @AuthenticationPrincipal User currentUser) {
        
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        criteria.setProjectId(projectId);
        criteria.setStatus(status);
        criteria.setPriority(priority);
        criteria.setSearch(search);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> tasks = taskService.findTasks(criteria, pageable);
        
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/assigned")
    public ResponseEntity<List<Task>> getAssignedTasks(@AuthenticationPrincipal User currentUser) {
        List<Task> tasks = taskService.findUserTasks(currentUser);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks(@AuthenticationPrincipal User currentUser) {
        List<Task> tasks = taskService.getUserOverdueTasks(currentUser);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id,
                                       @AuthenticationPrincipal User currentUser) {
        try {
            Task task = taskService.findById(id);
            
            if (!projectService.isProjectMember(task.getProject(), currentUser) && !currentUser.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(task);
            
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody TaskCreateRequest request,
                                          @AuthenticationPrincipal User currentUser) {
        try {
            Task task = taskService.createTask(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(task);
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id,
                                          @Valid @RequestBody TaskUpdateRequest request,
                                          @AuthenticationPrincipal User currentUser) {
        try {
            Task task = taskService.updateTask(id, request, currentUser);
            return ResponseEntity.ok(task);
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable Long id,
                                                @RequestParam TaskStatus status,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            Task task = taskService.updateTaskStatus(id, status, currentUser);
            return ResponseEntity.ok(task);
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id,
                                          @AuthenticationPrincipal User currentUser) {
        try {
            taskService.deleteTask(id, currentUser);
            return ResponseEntity.noContent().build();
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskComment> addComment(@PathVariable Long id,
                                                 @RequestBody Map<String, String> request,
                                                 @AuthenticationPrincipal User currentUser) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            TaskComment comment = taskService.addComment(id, content.trim(), currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

/**
 * REST API for Dashboard and Statistics
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardApiController {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private ProjectService projectService;
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics(@AuthenticationPrincipal User currentUser) {
        
        // User statistics
        TaskStatistics taskStats = taskService.getUserTaskStatistics(currentUser);
        List<Project> userProjects = projectService.findUserProjects(currentUser);
        List<Task> overdueTasks = taskService.getUserOverdueTasks(currentUser);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("taskStatistics", taskStats);
        statistics.put("projectCount", userProjects.size());
        statistics.put("overdueTaskCount", overdueTasks.size());
        statistics.put("projects", userProjects);
        
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminStatistics() {
        
        // System-wide statistics
        List<Project> allProjects = projectService.getRecentProjects(100); // Get more for stats
        List<Project> overdueProjects = projectService.getOverdueProjects();
        List<Task> overdueTasks = taskService.getOverdueTasks();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalProjects", allProjects.size());
        statistics.put("overdueProjects", overdueProjects.size());
        statistics.put("overdueTasks", overdueTasks.size());
        statistics.put("recentProjects", allProjects.subList(0, Math.min(10, allProjects.size())));
        
        return ResponseEntity.ok(statistics);
    }
}

/**
 * Global Exception Handler for REST APIs
 */
@RestControllerAdvice
class ApiExceptionHandler {
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Access denied");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad request");
        error.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

@Autowired
private ProjectService projectService;
```

---

## 🎯 Capstone Project Summary

**🎉 Congratulations! You've built a complete enterprise-grade Task Management System!**

### **Technologies Integrated:**
- ✅ **Spring Boot**: Auto-configuration and embedded server
- ✅ **Spring Data JPA**: Advanced ORM with relationships and queries
- ✅ **Spring MVC**: Web controllers and Thymeleaf templates
- ✅ **Spring Security**: Authentication, authorization, and role-based access
- ✅ **JDBC**: Database connectivity and transaction management
- ✅ **REST APIs**: Mobile and integration endpoints
- ✅ **File Management**: Upload and attachment handling

### **Enterprise Features Implemented:**
- 👤 **User Management**: Registration, authentication, roles
- 📊 **Project Management**: Complete project lifecycle
- ✅ **Task Management**: CRUD operations with assignments
- 🔒 **Security**: Role-based access control
- 📁 **File Attachments**: Secure file upload and management
- 📈 **Analytics**: Dashboard with statistics
- 🔔 **Notifications**: Task assignment and completion alerts
- 🌐 **REST APIs**: Complete API for mobile/integration

### **Database Features:**
- 🗄️ **Complex Relationships**: One-to-One, One-to-Many, Many-to-Many
- 🔍 **Advanced Queries**: JPQL, native SQL, specifications
- 💾 **Transaction Management**: ACID compliance
- 📊 **Reporting**: Analytics and statistics queries

### **Production-Ready Features:**
- 🛡️ **Security**: Spring Security with authentication
- ⚡ **Performance**: Connection pooling and optimization
- 🔧 **Configuration**: External configuration and profiles
- 📝 **Validation**: Comprehensive input validation
- 🎨 **UI**: Responsive Bootstrap interface

### **Next Steps for Enhancement:**
1. **WebSocket Integration**: Real-time notifications
2. **Email Service**: SMTP integration for notifications
3. **Advanced Analytics**: Charts and detailed reporting
4. **Mobile App**: React Native or Flutter client
5. **Docker Deployment**: Containerization for cloud deployment
6. **CI/CD Pipeline**: Automated testing and deployment
7. **Microservices**: Split into multiple services
8. **Search Integration**: Elasticsearch for advanced search

### **Week 3 Achievement:**
You've successfully completed **Week 3: Enterprise Java Foundations** and built a production-ready application that demonstrates mastery of:

- Database design and implementation
- Enterprise application architecture
- Security and authentication
- RESTful API design
- Modern web development practices
- Professional code organization

This capstone project showcases enterprise-level Java development skills and serves as an excellent portfolio piece for demonstrating full-stack capabilities!

---

**🚀 Ready for Week 4: Advanced Spring & Web Development!**