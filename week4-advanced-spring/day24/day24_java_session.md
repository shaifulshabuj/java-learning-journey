# Day 24: Spring Security Basics (August 2, 2025)

**Date:** August 2, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Spring Security fundamentals: authentication, authorization, user management, and security configuration

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand Spring Security architecture and core concepts
- ✅ Configure authentication and authorization mechanisms
- ✅ Implement user management with UserDetailsService
- ✅ Master password encoding and security best practices
- ✅ Create method-level security with annotations
- ✅ Build a complete secure web application

---

## 🔐 Part 1: Spring Security Architecture & Authentication (45 minutes)

### **Understanding Spring Security**

**Spring Security** is a comprehensive security framework for Java applications that provides:
- **Authentication**: Who are you?
- **Authorization**: What are you allowed to do?
- **Protection**: Against common attacks (CSRF, XSS, etc.)
- **Integration**: With Spring Boot and other frameworks

### **Core Security Concepts**

1. **Principal**: The currently authenticated user
2. **Credentials**: Password or other authentication proof
3. **Authority**: Permission to perform specific actions
4. **Role**: Collection of authorities (e.g., ADMIN, USER)
5. **SecurityContext**: Holds security information for current thread

### **Basic Security Configuration**

Create `BasicSecurityConfigDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic Spring Security configuration demonstration
 * Shows fundamental security setup and authentication
 */
@SpringBootApplication
public class BasicSecurityConfigDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Basic Spring Security Demo ===\n");
        SpringApplication.run(BasicSecurityConfigDemo.class, args);
        
        System.out.println("🔐 Security Configuration:");
        System.out.println("   Default users created:");
        System.out.println("     - user/password (USER role)");
        System.out.println("     - admin/admin123 (ADMIN role)");
        System.out.println("   Protected endpoints:");
        System.out.println("     - /api/public/* (accessible to all)");
        System.out.println("     - /api/user/* (USER role required)");
        System.out.println("     - /api/admin/* (ADMIN role required)");
        System.out.println("   Login URL: http://localhost:8080/login");
        System.out.println();
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
            
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN", "USER")
            .build();
            
        return new InMemoryUserDetailsManager(user, admin);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/login", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // Disable for API testing
            
        return http.build();
    }
}

// Test controllers for different security levels
@RestController
class SecurityTestController {
    
    @GetMapping("/api/public/hello")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a public endpoint - no authentication required");
        response.put("timestamp", System.currentTimeMillis());
        response.put("access", "public");
        return response;
    }
    
    @GetMapping("/api/user/profile")
    public Map<String, Object> userEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to user area");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("access", "user");
        return response;
    }
    
    @GetMapping("/api/admin/dashboard")
    public Map<String, Object> adminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to admin dashboard");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("access", "admin");
        return response;
    }
    
    @GetMapping("/api/current-user")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("authenticated", auth.isAuthenticated());
        response.put("principal", auth.getPrincipal().toString());
        return response;
    }
}

// Simple login controller
@Controller
class LoginController {
    
    @GetMapping("/login")
    public String login() {
        return "login"; // Return login.html template
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard"; // Return dashboard.html template
    }
    
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
```

---

## 👤 Part 2: Custom User Management & Password Encoding (45 minutes)

### **Custom UserDetailsService Implementation**

Create `UserAuthenticationDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.CommandLineRunner;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Custom user authentication demonstration
 * Shows how to implement custom UserDetailsService and user management
 */
@SpringBootApplication
public class UserAuthenticationDemo implements CommandLineRunner {
    
    private final CustomUserService userService;
    private final PasswordEncoder passwordEncoder;
    
    public UserAuthenticationDemo(CustomUserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }
    
    public static void main(String[] args) {
        System.out.println("=== User Authentication Demo ===\n");
        SpringApplication.run(UserAuthenticationDemo.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔐 Initializing users...");
        
        // Create default users
        userService.createUser(new CreateUserRequest("admin", "admin@example.com", "admin123", 
            Arrays.asList("ADMIN", "USER")));
        userService.createUser(new CreateUserRequest("user", "user@example.com", "password", 
            Arrays.asList("USER")));
        userService.createUser(new CreateUserRequest("manager", "manager@example.com", "manager123", 
            Arrays.asList("MANAGER", "USER")));
        
        System.out.println("✅ Default users created successfully!");
        System.out.println();
    }
}

// Custom User entity
class CustomUser {
    private String username;
    private String email;
    private String password;
    private List<String> roles;
    private boolean enabled;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private int failedLoginAttempts;
    
    public CustomUser() {
        this.enabled = true;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.createdAt = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.roles = new ArrayList<>();
    }
    
    public CustomUser(String username, String email, String password, List<String> roles) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isAccountNonExpired() { return accountNonExpired; }
    public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }
    
    public boolean isAccountNonLocked() { return accountNonLocked; }
    public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }
    
    public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
    public void setCredentialsNonExpired(boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
}

// Custom UserDetails implementation
class CustomUserDetails implements UserDetails {
    private final CustomUser user;
    
    public CustomUserDetails(CustomUser user) {
        this.user = user;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    @Override
    public String getUsername() {
        return user.getUsername();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return user.isAccountNonExpired();
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return user.isAccountNonLocked();
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return user.isCredentialsNonExpired();
    }
    
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
    
    public CustomUser getUser() {
        return user;
    }
}

// Custom UserDetailsService implementation
@Service
class CustomUserDetailsService implements UserDetailsService {
    
    private final CustomUserService userService;
    
    public CustomUserDetailsService(CustomUserService userService) {
        this.userService = userService;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CustomUser user = userService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        
        System.out.println("Loading user: " + username + " with roles: " + user.getRoles());
        
        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userService.updateUser(user);
        
        return new CustomUserDetails(user);
    }
}

// User service for managing users
@Service
class CustomUserService {
    
    private final Map<String, CustomUser> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    
    public CustomUserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    
    public void createUser(CreateUserRequest request) {
        if (users.containsKey(request.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + request.getUsername());
        }
        
        CustomUser user = new CustomUser(
            request.getUsername(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getRoles()
        );
        
        users.put(request.getUsername(), user);
        System.out.println("Created user: " + request.getUsername() + " with roles: " + request.getRoles());
    }
    
    public CustomUser findByUsername(String username) {
        return users.get(username);
    }
    
    public List<CustomUser> findAll() {
        return new ArrayList<>(users.values());
    }
    
    public void updateUser(CustomUser user) {
        users.put(user.getUsername(), user);
    }
    
    public void deleteUser(String username) {
        users.remove(username);
    }
    
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        CustomUser user = users.get(username);
        if (user == null) {
            return false;
        }
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        users.put(username, user);
        return true;
    }
    
    public void lockUser(String username) {
        CustomUser user = users.get(username);
        if (user != null) {
            user.setAccountNonLocked(false);
            users.put(username, user);
        }
    }
    
    public void unlockUser(String username) {
        CustomUser user = users.get(username);
        if (user != null) {
            user.setAccountNonLocked(true);
            user.setFailedLoginAttempts(0);
            users.put(username, user);
        }
    }
    
    public void incrementFailedLoginAttempts(String username) {
        CustomUser user = users.get(username);
        if (user != null) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            
            // Lock account after 3 failed attempts
            if (user.getFailedLoginAttempts() >= 3) {
                user.setAccountNonLocked(false);
                System.out.println("Account locked for user: " + username);
            }
            
            users.put(username, user);
        }
    }
    
    public void resetFailedLoginAttempts(String username) {
        CustomUser user = users.get(username);
        if (user != null) {
            user.setFailedLoginAttempts(0);
            users.put(username, user);
        }
    }
}

// Request DTOs
class CreateUserRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @Email(message = "Valid email is required")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    private List<String> roles;
    
    public CreateUserRequest() {}
    
    public CreateUserRequest(String username, String email, String password, List<String> roles) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}

class ChangePasswordRequest {
    @NotBlank(message = "Old password is required")
    private String oldPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
    
    // Getters and setters
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}

// User management controller
@RestController
@RequestMapping("/api/users")
class UserManagementController {
    
    private final CustomUserService userService;
    
    public UserManagementController(CustomUserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/register")
    public Map<String, Object> registerUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            userService.createUser(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", request.getUsername());
            response.put("roles", request.getRoles());
            
            return response;
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    @GetMapping
    public List<Map<String, Object>> getAllUsers() {
        return userService.findAll().stream()
            .map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("roles", user.getRoles());
                userInfo.put("enabled", user.isEnabled());
                userInfo.put("accountNonLocked", user.isAccountNonLocked());
                userInfo.put("createdAt", user.getCreatedAt());
                userInfo.put("lastLoginAt", user.getLastLoginAt());
                userInfo.put("failedLoginAttempts", user.getFailedLoginAttempts());
                return userInfo;
            })
            .collect(Collectors.toList());
    }
    
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        boolean success = userService.changePassword(username, request.getOldPassword(), request.getNewPassword());
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("message", "Password changed successfully");
        } else {
            response.put("error", "Invalid old password");
        }
        
        return response;
    }
    
    @PostMapping("/{username}/lock")
    public Map<String, Object> lockUser(@PathVariable String username) {
        userService.lockUser(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User locked successfully");
        response.put("username", username);
        
        return response;
    }
    
    @PostMapping("/{username}/unlock")
    public Map<String, Object> unlockUser(@PathVariable String username) {
        userService.unlockUser(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User unlocked successfully");
        response.put("username", username);
        
        return response;
    }
}

// Security configuration for custom user service
@Configuration
@EnableWebSecurity
class CustomSecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/users/register", "/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasRole("MANAGER")
                .requestMatchers("/api/user/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());
            
        return http.build();
    }
}
```

---

## 🔑 Part 3: Password Encoding & Security Best Practices (30 minutes)

### **Password Encoding Strategies**

Create `PasswordEncodingDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Password encoding strategies demonstration
 * Shows different password encoding techniques and security considerations
 */
@SpringBootApplication
public class PasswordEncodingDemo implements CommandLineRunner {
    
    public static void main(String[] args) {
        System.out.println("=== Password Encoding Demo ===\n");
        SpringApplication.run(PasswordEncodingDemo.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔐 Password Encoding Demonstration\n");
        
        // Test different password encoders
        testPasswordEncoders();
        
        // Test password strength
        testPasswordStrength();
        
        // Test password policies
        testPasswordPolicies();
        
        // Test secure password generation
        testSecurePasswordGeneration();
        
        System.out.println("\n✅ Password encoding demo completed!\n");
    }
    
    private void testPasswordEncoders() {
        System.out.println("1. Testing Password Encoders:");
        
        String plainPassword = "mySecurePassword123!";
        
        // BCrypt - Most common and recommended
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        String bcryptHash = bcrypt.encode(plainPassword);
        System.out.println("   BCrypt Hash: " + bcryptHash);
        System.out.println("   BCrypt Matches: " + bcrypt.matches(plainPassword, bcryptHash));
        
        // SCrypt - Memory-hard function
        SCryptPasswordEncoder scrypt = new SCryptPasswordEncoder();
        String scryptHash = scrypt.encode(plainPassword);
        System.out.println("   SCrypt Hash: " + scryptHash);
        System.out.println("   SCrypt Matches: " + scrypt.matches(plainPassword, scryptHash));
        
        // Argon2 - Latest and most secure
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder();
        String argon2Hash = argon2.encode(plainPassword);
        System.out.println("   Argon2 Hash: " + argon2Hash);
        System.out.println("   Argon2 Matches: " + argon2.matches(plainPassword, argon2Hash));
        
        // PBKDF2 - Standards-based
        Pbkdf2PasswordEncoder pbkdf2 = new Pbkdf2PasswordEncoder();
        String pbkdf2Hash = pbkdf2.encode(plainPassword);
        System.out.println("   PBKDF2 Hash: " + pbkdf2Hash);
        System.out.println("   PBKDF2 Matches: " + pbkdf2.matches(plainPassword, pbkdf2Hash));
        
        System.out.println();
    }
    
    private void testPasswordStrength() {
        System.out.println("2. Testing Password Strength:");
        
        String[] testPasswords = {
            "123456",
            "password",
            "Password123",
            "MyStr0ng@Pass!",
            "Sup3r$ecur3P@ssw0rd!2023"
        };
        
        PasswordStrengthChecker checker = new PasswordStrengthChecker();
        
        for (String password : testPasswords) {
            PasswordStrength strength = checker.checkStrength(password);
            System.out.println(String.format("   Password: %-25s | Strength: %-10s | Score: %d/100",
                password, strength.getLevel(), strength.getScore()));
        }
        
        System.out.println();
    }
    
    private void testPasswordPolicies() {
        System.out.println("3. Testing Password Policies:");
        
        PasswordPolicy policy = new PasswordPolicy();
        
        String[] testPasswords = {
            "short",
            "nouppercase123!",
            "NOLOWERCASE123!",
            "NoNumbers!",
            "NoSpecialChars123",
            "ValidPass123!"
        };
        
        for (String password : testPasswords) {
            PolicyViolation violation = policy.validate(password);
            System.out.println(String.format("   Password: %-20s | Valid: %-5s | Violations: %s",
                password, violation.isValid(), violation.getViolations()));
        }
        
        System.out.println();
    }
    
    private void testSecurePasswordGeneration() {
        System.out.println("4. Testing Secure Password Generation:");
        
        SecurePasswordGenerator generator = new SecurePasswordGenerator();
        
        for (int i = 0; i < 5; i++) {
            String password = generator.generatePassword();
            System.out.println("   Generated Password: " + password);
        }
        
        System.out.println();
    }
}

// Password strength checker
class PasswordStrengthChecker {
    
    public PasswordStrength checkStrength(String password) {
        int score = 0;
        String level = "WEAK";
        
        // Length check
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 25;
        
        // Character variety checks
        if (password.matches(".*[a-z].*")) score += 10;
        if (password.matches(".*[A-Z].*")) score += 10;
        if (password.matches(".*[0-9].*")) score += 10;
        if (password.matches(".*[^a-zA-Z0-9].*")) score += 20;
        
        // Determine strength level
        if (score >= 80) level = "VERY_STRONG";
        else if (score >= 60) level = "STRONG";
        else if (score >= 40) level = "MODERATE";
        else if (score >= 20) level = "WEAK";
        else level = "VERY_WEAK";
        
        return new PasswordStrength(level, score);
    }
}

class PasswordStrength {
    private final String level;
    private final int score;
    
    public PasswordStrength(String level, int score) {
        this.level = level;
        this.score = score;
    }
    
    public String getLevel() { return level; }
    public int getScore() { return score; }
}

// Password policy validator
class PasswordPolicy {
    
    public PolicyViolation validate(String password) {
        PolicyViolation violation = new PolicyViolation();
        
        if (password.length() < 8) {
            violation.addViolation("Password must be at least 8 characters long");
        }
        
        if (!password.matches(".*[a-z].*")) {
            violation.addViolation("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            violation.addViolation("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[0-9].*")) {
            violation.addViolation("Password must contain at least one number");
        }
        
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            violation.addViolation("Password must contain at least one special character");
        }
        
        return violation;
    }
}

class PolicyViolation {
    private final java.util.List<String> violations = new java.util.ArrayList<>();
    
    public void addViolation(String violation) {
        violations.add(violation);
    }
    
    public boolean isValid() {
        return violations.isEmpty();
    }
    
    public java.util.List<String> getViolations() {
        return violations;
    }
}

// Secure password generator
class SecurePasswordGenerator {
    
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARS = LOWERCASE + UPPERCASE + NUMBERS + SPECIAL_CHARS;
    
    private final SecureRandom random = new SecureRandom();
    
    public String generatePassword() {
        return generatePassword(12);
    }
    
    public String generatePassword(int length) {
        StringBuilder password = new StringBuilder(length);
        
        // Ensure at least one character from each category
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        
        // Fill remaining positions with random characters
        for (int i = 4; i < length; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }
        
        // Shuffle the password to avoid predictable patterns
        return shuffleString(password.toString());
    }
    
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int randomIndex = random.nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[randomIndex];
            chars[randomIndex] = temp;
        }
        return new String(chars);
    }
}

// Delegating password encoder configuration
@Bean
public PasswordEncoder passwordEncoder() {
    String idForEncode = "bcrypt";
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    
    encoders.put(idForEncode, new BCryptPasswordEncoder());
    encoders.put("scrypt", new SCryptPasswordEncoder());
    encoders.put("argon2", new Argon2PasswordEncoder());
    encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
    
    return new DelegatingPasswordEncoder(idForEncode, encoders);
}
```

---

## 🛡️ Part 4: Method-Level Security (30 minutes)

### **Method Security Annotations**

Create `MethodSecurityDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Method-level security demonstration
 * Shows how to secure methods with annotations
 */
@SpringBootApplication
public class MethodSecurityDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Method Security Demo ===\n");
        SpringApplication.run(MethodSecurityDemo.class, args);
        
        System.out.println("🛡️ Method Security Features:");
        System.out.println("   @PreAuthorize - Check permissions before method execution");
        System.out.println("   @PostAuthorize - Check permissions after method execution");
        System.out.println("   @PreFilter - Filter input parameters");
        System.out.println("   @PostFilter - Filter return values");
        System.out.println("   @Secured - Role-based access control");
        System.out.println("   @RolesAllowed - JSR-250 annotations");
        System.out.println();
    }
}

@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,     // Enable @PreAuthorize and @PostAuthorize
    securedEnabled = true,     // Enable @Secured
    jsr250Enabled = true       // Enable @RolesAllowed
)
class MethodSecurityConfig {
}

// Document entity for demonstration
class Document {
    private String id;
    private String title;
    private String content;
    private String owner;
    private String department;
    private LocalDateTime createdAt;
    private boolean confidential;
    
    public Document() {}
    
    public Document(String id, String title, String content, String owner, String department, boolean confidential) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.owner = owner;
        this.department = department;
        this.confidential = confidential;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public boolean isConfidential() { return confidential; }
    public void setConfidential(boolean confidential) { this.confidential = confidential; }
}

// Service with method-level security
@Service
class DocumentService {
    
    private final Map<String, Document> documents = new HashMap<>();
    
    public DocumentService() {
        // Initialize sample documents
        documents.put("1", new Document("1", "Public Document", "Public content", "admin", "PUBLIC", false));
        documents.put("2", new Document("2", "HR Document", "HR content", "manager", "HR", true));
        documents.put("3", new Document("3", "Finance Report", "Finance content", "admin", "FINANCE", true));
        documents.put("4", new Document("4", "IT Policy", "IT content", "user", "IT", false));
    }
    
    // Only authenticated users can access
    @PreAuthorize("isAuthenticated()")
    public List<Document> findAllDocuments() {
        System.out.println("Finding all documents for authenticated user");
        return new ArrayList<>(documents.values());
    }
    
    // Only users with ADMIN role can access
    @PreAuthorize("hasRole('ADMIN')")
    public List<Document> findAllDocumentsAdmin() {
        System.out.println("Admin accessing all documents");
        return new ArrayList<>(documents.values());
    }
    
    // User can access documents they own or if they have ADMIN role
    @PreAuthorize("hasRole('ADMIN') or #owner == authentication.name")
    public List<Document> findDocumentsByOwner(String owner) {
        System.out.println("Finding documents for owner: " + owner);
        return documents.values().stream()
            .filter(doc -> doc.getOwner().equals(owner))
            .collect(Collectors.toList());
    }
    
    // Check access after method execution - user can access only non-confidential documents
    // unless they're admin or the owner
    @PostAuthorize("hasRole('ADMIN') or returnObject.owner == authentication.name or !returnObject.confidential")
    public Document findDocumentById(String id) {
        System.out.println("Finding document by ID: " + id);
        return documents.get(id);
    }
    
    // Filter input list - only process documents user owns or is admin
    @PreFilter("hasRole('ADMIN') or filterObject.owner == authentication.name")
    public List<Document> updateDocuments(List<Document> documentsToUpdate) {
        System.out.println("Updating " + documentsToUpdate.size() + " documents");
        
        for (Document doc : documentsToUpdate) {
            documents.put(doc.getId(), doc);
        }
        
        return documentsToUpdate;
    }
    
    // Filter return list - only return documents user can access
    @PostFilter("hasRole('ADMIN') or filterObject.owner == authentication.name or !filterObject.confidential")
    public List<Document> searchDocuments(String query) {
        System.out.println("Searching documents for: " + query);
        
        return documents.values().stream()
            .filter(doc -> doc.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                          doc.getContent().toLowerCase().contains(query.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    // Complex authorization logic
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and #document.department == 'HR') or #document.owner == authentication.name")
    public Document createDocument(Document document) {
        System.out.println("Creating document: " + document.getTitle());
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        document.setOwner(username);
        document.setCreatedAt(LocalDateTime.now());
        
        documents.put(document.getId(), document);
        return document;
    }
    
    // Multiple conditions with SpEL
    @PreAuthorize("hasRole('ADMIN') or (#id != null and @documentService.isDocumentOwner(#id, authentication.name))")
    public void deleteDocument(String id) {
        System.out.println("Deleting document: " + id);
        documents.remove(id);
    }
    
    // Custom security method
    public boolean isDocumentOwner(String documentId, String username) {
        Document doc = documents.get(documentId);
        return doc != null && doc.getOwner().equals(username);
    }
    
    // Department-based access control
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or @documentService.isSameDepartment(#id, authentication.name)")
    public Document getDocumentWithDepartmentAccess(String id) {
        System.out.println("Getting document with department access: " + id);
        return documents.get(id);
    }
    
    public boolean isSameDepartment(String documentId, String username) {
        Document doc = documents.get(documentId);
        if (doc == null) return false;
        
        // In real application, you'd check user's department from database
        // For demo, assume user department based on roles
        return true; // Simplified for demo
    }
}

// Controller to test method security
@RestController
@RequestMapping("/api/documents")
class DocumentController {
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentService.findAllDocuments();
    }
    
    @GetMapping("/admin")
    public List<Document> getAllDocumentsAdmin() {
        return documentService.findAllDocumentsAdmin();
    }
    
    @GetMapping("/owner/{owner}")
    public List<Document> getDocumentsByOwner(@PathVariable String owner) {
        return documentService.findDocumentsByOwner(owner);
    }
    
    @GetMapping("/{id}")
    public Document getDocumentById(@PathVariable String id) {
        return documentService.findDocumentById(id);
    }
    
    @GetMapping("/search")
    public List<Document> searchDocuments(@RequestParam String query) {
        return documentService.searchDocuments(query);
    }
    
    @PostMapping
    public Document createDocument(@RequestBody Document document) {
        return documentService.createDocument(document);
    }
    
    @DeleteMapping("/{id}")
    public void deleteDocument(@PathVariable String id) {
        documentService.deleteDocument(id);
    }
    
    @GetMapping("/{id}/department")
    public Document getDocumentWithDepartmentAccess(@PathVariable String id) {
        return documentService.getDocumentWithDepartmentAccess(id);
    }
    
    @PutMapping("/batch")
    public List<Document> updateDocuments(@RequestBody List<Document> documents) {
        return documentService.updateDocuments(documents);
    }
}

// Security expression methods
@Service("securityService")
class SecurityService {
    
    public boolean canAccessDocument(String documentId, String username) {
        // Custom security logic
        return true;
    }
    
    public boolean isBusinessHours() {
        int hour = LocalDateTime.now().getHour();
        return hour >= 9 && hour <= 17;
    }
    
    public boolean hasPermission(String permission) {
        // Check if user has specific permission
        return true;
    }
}

// Advanced security expressions
@RestController
@RequestMapping("/api/advanced")
class AdvancedSecurityController {
    
    // Time-based access control
    @PreAuthorize("@securityService.isBusinessHours()")
    @GetMapping("/business-hours")
    public Map<String, Object> getBusinessHoursData() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This endpoint is only available during business hours");
        response.put("currentTime", LocalDateTime.now());
        return response;
    }
    
    // IP-based access control (would require custom implementation)
    @PreAuthorize("hasRole('ADMIN') or hasIpAddress('192.168.1.0/24')")
    @GetMapping("/internal")
    public Map<String, Object> getInternalData() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Internal data accessible from trusted network");
        return response;
    }
    
    // Multiple role requirements
    @PreAuthorize("hasRole('ADMIN') and hasRole('MANAGER')")
    @GetMapping("/admin-manager")
    public Map<String, Object> getAdminManagerData() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Data for users with both ADMIN and MANAGER roles");
        return response;
    }
    
    // Custom permission check
    @PreAuthorize("@securityService.hasPermission('READ_SENSITIVE_DATA')")
    @GetMapping("/sensitive")
    public Map<String, Object> getSensitiveData() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensitive data accessible with special permission");
        return response;
    }
}
```

---

## 📋 Practice Exercises

### Exercise 1: User Registration System
Create a complete user registration and management system:
- User registration with email verification
- Password reset functionality
- Account activation/deactivation
- User profile management
- Role-based access control

### Exercise 2: JWT Authentication
Implement JWT-based authentication:
- JWT token generation on login
- Token validation middleware
- Token refresh mechanism
- Logout with token blacklisting
- Token expiration handling

### Exercise 3: OAuth2 Integration
Integrate with OAuth2 providers:
- Google OAuth2 login
- GitHub OAuth2 login
- Custom OAuth2 authorization server
- Resource server configuration
- Scope-based access control

---

## 🎯 Key Takeaways

### Spring Security Core Concepts:
1. **Authentication**: Verify user identity
2. **Authorization**: Check user permissions
3. **Principal**: Currently authenticated user
4. **Authorities**: User permissions and roles
5. **SecurityContext**: Thread-local security information

### Password Security Best Practices:
1. **Use strong encoders**: BCrypt, SCrypt, or Argon2
2. **Implement password policies**: Length, complexity, history
3. **Account lockout**: Prevent brute force attacks
4. **Regular password rotation**: Encourage periodic changes
5. **Secure password storage**: Never store plain text passwords

### Method Security Features:
1. **@PreAuthorize**: Check before method execution
2. **@PostAuthorize**: Check after method execution
3. **@PreFilter**: Filter input parameters
4. **@PostFilter**: Filter return values
5. **SpEL expressions**: Flexible authorization logic

### Security Configuration:
1. **SecurityFilterChain**: Modern Spring Security configuration
2. **HttpSecurity**: Configure web security
3. **AuthenticationManager**: Handle authentication
4. **UserDetailsService**: Load user details
5. **PasswordEncoder**: Encode and verify passwords

---

## 🚀 Next Steps

Tomorrow, we'll explore **Frontend Integration** (Day 25), where you'll learn:
- Advanced Thymeleaf features
- AJAX integration with Spring
- Form handling and validation
- WebSocket for real-time features
- Static resource management

Your security knowledge will be essential for building secure, user-friendly web applications! 🌐