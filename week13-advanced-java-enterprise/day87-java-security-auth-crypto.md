# Day 87: Java Security - Authentication, Authorization & Cryptography

## Overview
Today we dive deep into Java security fundamentals, covering authentication mechanisms, authorization frameworks, cryptographic operations, and secure coding practices. We'll explore Spring Security, JWT tokens, OAuth 2.0, encryption/decryption, digital signatures, and security best practices for enterprise applications.

## Learning Objectives
- Implement robust authentication and authorization systems
- Master cryptographic operations in Java
- Understand and implement JWT and OAuth 2.0
- Apply security best practices and prevent common vulnerabilities
- Build secure REST APIs and web applications
- Implement proper session management and CSRF protection

## 1. Authentication Fundamentals

### Authentication Mechanisms
```java
package com.javajourney.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Comprehensive authentication mechanisms and password security
 */
public class AuthenticationMechanisms {
    
    /**
     * Secure password hashing and verification
     */
    public static class PasswordSecurity {
        private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
        private final SecureRandom secureRandom = new SecureRandom();
        
        /**
         * Hash password with salt using BCrypt
         */
        public String hashPassword(String plainPassword) {
            // BCrypt automatically generates salt and handles iterations
            return passwordEncoder.encode(plainPassword);
        }
        
        /**
         * Verify password against hash
         */
        public boolean verifyPassword(String plainPassword, String hashedPassword) {
            return passwordEncoder.matches(plainPassword, hashedPassword);
        }
        
        /**
         * Manual salt generation for custom implementations
         */
        public String generateSalt() {
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        }
        
        /**
         * Custom password hashing with PBKDF2
         */
        public String hashPasswordPBKDF2(String password, String salt, int iterations) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(Base64.getDecoder().decode(salt));
                
                byte[] hash = password.getBytes("UTF-8");
                for (int i = 0; i < iterations; i++) {
                    hash = digest.digest(hash);
                }
                
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw new RuntimeException("Password hashing failed", e);
            }
        }
        
        /**
         * Password strength validation
         */
        public PasswordStrength validatePasswordStrength(String password) {
            if (password == null || password.length() < 8) {
                return PasswordStrength.WEAK;
            }
            
            boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
            boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
            boolean hasDigit = password.chars().anyMatch(Character::isDigit);
            boolean hasSpecial = password.chars()
                .anyMatch(ch -> "!@#$%^&*()-_=+[]{}|;:,.<>?".indexOf(ch) >= 0);
            
            int criteriaCount = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + 
                              (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
            
            if (password.length() >= 12 && criteriaCount >= 3) {
                return PasswordStrength.STRONG;
            } else if (password.length() >= 8 && criteriaCount >= 2) {
                return PasswordStrength.MEDIUM;
            } else {
                return PasswordStrength.WEAK;
            }
        }
    }
    
    public enum PasswordStrength {
        WEAK("Password is too weak"),
        MEDIUM("Password strength is acceptable"),
        STRONG("Password is strong");
        
        private final String description;
        
        PasswordStrength(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Multi-factor authentication implementation
     */
    public static class MultiFactorAuthentication {
        private final SecureRandom random = new SecureRandom();
        
        /**
         * Generate TOTP (Time-based One-Time Password) compatible secret
         */
        public String generateTOTPSecret() {
            byte[] secret = new byte[20]; // 160-bit secret
            random.nextBytes(secret);
            return Base64.getEncoder().encodeToString(secret);
        }
        
        /**
         * Generate SMS/Email verification code
         */
        public String generateVerificationCode(int length) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < length; i++) {
                code.append(random.nextInt(10));
            }
            return code.toString();
        }
        
        /**
         * Verify time-based code with tolerance window
         */
        public boolean verifyTimeBasedCode(String userCode, String expectedCode, 
                                         long timeWindowSeconds) {
            // In real implementation, you'd calculate TOTP based on current time
            // This is a simplified version
            return userCode.equals(expectedCode);
        }
        
        /**
         * Rate limiting for authentication attempts
         */
        public static class AuthenticationRateLimit {
            private final Map<String, AttemptTracker> attempts = new ConcurrentHashMap<>();
            private final int maxAttempts;
            private final long lockoutDurationMs;
            
            public AuthenticationRateLimit(int maxAttempts, long lockoutDurationMs) {
                this.maxAttempts = maxAttempts;
                this.lockoutDurationMs = lockoutDurationMs;
            }
            
            public boolean isAccountLocked(String identifier) {
                AttemptTracker tracker = attempts.get(identifier);
                if (tracker == null) return false;
                
                if (tracker.isLocked() && 
                    System.currentTimeMillis() - tracker.getLockTime() > lockoutDurationMs) {
                    tracker.reset();
                    return false;
                }
                
                return tracker.isLocked();
            }
            
            public void recordFailedAttempt(String identifier) {
                attempts.computeIfAbsent(identifier, k -> new AttemptTracker())
                        .recordFailedAttempt(maxAttempts);
            }
            
            public void recordSuccessfulLogin(String identifier) {
                attempts.remove(identifier);
            }
            
            private static class AttemptTracker {
                private int failedAttempts = 0;
                private long lockTime = 0;
                private boolean locked = false;
                
                void recordFailedAttempt(int maxAttempts) {
                    failedAttempts++;
                    if (failedAttempts >= maxAttempts) {
                        locked = true;
                        lockTime = System.currentTimeMillis();
                    }
                }
                
                void reset() {
                    failedAttempts = 0;
                    locked = false;
                    lockTime = 0;
                }
                
                boolean isLocked() { return locked; }
                long getLockTime() { return lockTime; }
            }
        }
    }
}
```

### Spring Security Configuration
```java
package com.javajourney.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Comprehensive Spring Security configuration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    /**
     * Main security filter chain configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/public/**")
            )
            
            // Session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            
            // Request authorization
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**", "/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            
            // Custom authentication filters
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitingFilter(), JwtAuthenticationFilter.class)
            
            // Exception handling
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            
            // Security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            );
            
        return http.build();
    }
    
    /**
     * JWT Authentication Filter
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
    
    /**
     * Rate limiting filter
     */
    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter();
    }
    
    /**
     * Custom authentication entry point
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("""
                {
                    "error": "Unauthorized",
                    "message": "Authentication required",
                    "timestamp": "%s"
                }
                """.formatted(Instant.now()));
        };
    }
    
    /**
     * Custom access denied handler
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("""
                {
                    "error": "Forbidden", 
                    "message": "Insufficient privileges",
                    "timestamp": "%s"
                }
                """.formatted(Instant.now()));
        };
    }
    
    /**
     * Password encoder configuration
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * Authentication manager configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

## 2. JWT Token Implementation

### JWT Service Implementation
```java
package com.javajourney.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.*;

/**
 * Comprehensive JWT token service with security best practices
 */
@Service
public class JwtTokenService {
    
    private final Key signingKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    private final String issuer;
    
    // Blacklisted tokens store (in production, use Redis or database)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    public JwtTokenService(@Value("${jwt.secret}") String secret,
                          @Value("${jwt.access-token-validity:900000}") long accessTokenValidityMs,
                          @Value("${jwt.refresh-token-validity:2592000000}") long refreshTokenValidityMs,
                          @Value("${jwt.issuer:java-journey}") String issuer) {
        
        // Ensure secret is at least 256 bits for HS256
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
        this.issuer = issuer;
    }
    
    /**
     * Generate access token with user details and roles
     */
    public String generateAccessToken(String username, Collection<String> roles, 
                                    Map<String, Object> additionalClaims) {
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityMs);
        
        JwtBuilder builder = Jwts.builder()
            .setSubject(username)
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "access")
            .claim("roles", roles)
            .claim("jti", UUID.randomUUID().toString()) // JWT ID for revocation
            .signWith(signingKey, SignatureAlgorithm.HS256);
        
        // Add additional claims
        if (additionalClaims != null) {
            additionalClaims.forEach(builder::claim);
        }
        
        return builder.compact();
    }
    
    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityMs);
        
        return Jwts.builder()
            .setSubject(username)
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "refresh")
            .claim("jti", UUID.randomUUID().toString())
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Validate and parse JWT token
     */
    public Optional<Claims> validateToken(String token) {
        try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                return Optional.empty();
            }
            
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
                
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            // Token is invalid
            return Optional.empty();
        }
    }
    
    /**
     * Extract username from token
     */
    public Optional<String> getUsernameFromToken(String token) {
        return validateToken(token).map(Claims::getSubject);
    }
    
    /**
     * Extract roles from token
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getRolesFromToken(String token) {
        return validateToken(token)
            .map(claims -> (Collection<String>) claims.get("roles"))
            .orElse(Collections.emptyList());
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return validateToken(token)
            .map(Claims::getExpiration)
            .map(expiration -> expiration.before(new Date()))
            .orElse(true);
    }
    
    /**
     * Blacklist a token (for logout/revocation)
     */
    public void blacklistToken(String token) {
        // Extract JTI (JWT ID) for efficient storage
        validateToken(token)
            .map(claims -> claims.get("jti", String.class))
            .ifPresent(blacklistedTokens::add);
    }
    
    /**
     * Check if token is blacklisted
     */
    private boolean isTokenBlacklisted(String token) {
        return validateToken(token)
            .map(claims -> claims.get("jti", String.class))
            .map(blacklistedTokens::contains)
            .orElse(false);
    }
    
    /**
     * Refresh access token using refresh token
     */
    public Optional<TokenPair> refreshTokens(String refreshToken) {
        return validateToken(refreshToken)
            .filter(claims -> "refresh".equals(claims.get("type")))
            .map(claims -> {
                String username = claims.getSubject();
                
                // In real implementation, fetch user roles from database
                Collection<String> roles = getUserRolesFromDatabase(username);
                
                String newAccessToken = generateAccessToken(username, roles, null);
                String newRefreshToken = generateRefreshToken(username);
                
                // Blacklist old refresh token
                blacklistToken(refreshToken);
                
                return new TokenPair(newAccessToken, newRefreshToken);
            });
    }
    
    /**
     * Token pair for access and refresh tokens
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        
        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
    
    // Placeholder - in real implementation, fetch from database
    private Collection<String> getUserRolesFromDatabase(String username) {
        // Mock implementation
        return List.of("ROLE_USER");
    }
}
```

### JWT Authentication Filter
```java
package com.javajourney.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT Authentication Filter for processing JWT tokens
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenService jwtTokenService;
    
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractTokenFromRequest(request);
        
        if (token != null && jwtTokenService.validateToken(token).isPresent()) {
            String username = jwtTokenService.getUsernameFromToken(token).orElse(null);
            Collection<String> roles = jwtTokenService.getRolesFromToken(token);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create authentication object
                Collection<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Also check for token in cookies as fallback
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                .filter(cookie -> "jwt-token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        }
        
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip JWT processing for public endpoints
        return path.startsWith("/api/public/") || 
               path.startsWith("/auth/") ||
               path.equals("/health") ||
               path.equals("/metrics");
    }
}
```

## 3. OAuth 2.0 Implementation

### OAuth 2.0 Authorization Server
```java
package com.javajourney.security.oauth;

import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

/**
 * OAuth 2.0 Authorization Server implementation
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig {
    
    /**
     * OAuth 2.0 client registration repository
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient webClient = RegisteredClient.withId("web-client")
            .clientId("web-app")
            .clientSecret("{bcrypt}$2a$10$...") // BCrypt encoded secret
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/web-app")
            .scope("read")
            .scope("write")
            .scope("admin")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .requireProofKey(true) // PKCE
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(15))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .reuseRefreshTokens(false)
                .build())
            .build();
        
        RegisteredClient mobileClient = RegisteredClient.withId("mobile-client")
            .clientId("mobile-app")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // Public client
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("com.javajourney.mobile://oauth2/callback")
            .scope("read")
            .scope("write")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true) // PKCE required for public clients
                .build())
            .build();
        
        return new InMemoryRegisteredClientRepository(webClient, mobileClient);
    }
    
    /**
     * Authorization server settings
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:9000")
            .authorizationEndpoint("/oauth2/authorize")
            .tokenEndpoint("/oauth2/token")
            .tokenRevocationEndpoint("/oauth2/revoke")
            .tokenIntrospectionEndpoint("/oauth2/introspect")
            .oidcUserInfoEndpoint("/oauth2/userinfo")
            .build();
    }
    
    /**
     * JWT customizer for access tokens
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return (context) -> {
            if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
                Authentication principal = context.getPrincipal();
                Set<String> authorities = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
                
                context.getClaims()
                    .claim("authorities", authorities)
                    .claim("username", principal.getName())
                    .claim("client_id", context.getRegisteredClient().getClientId());
            }
        };
    }
}
```

### OAuth 2.0 Resource Server
```java
package com.javajourney.security.oauth;

/**
 * OAuth 2.0 Resource Server configuration
 */
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {
    
    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**")
                    .hasAuthority("SCOPE_read")
                .requestMatchers(HttpMethod.POST, "/api/products/**")
                    .hasAuthority("SCOPE_write")
                .requestMatchers("/api/admin/**")
                    .hasAuthority("SCOPE_admin")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    .jwtDecoder(jwtDecoder())
                )
            );
            
        return http.build();
    }
    
    /**
     * JWT decoder configuration
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // For development, use local validation
        String jwkSetUri = "http://localhost:9000/oauth2/jwks";
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
            .cache(Duration.ofMinutes(5))
            .build();
            
        // Set up validation rules
        jwtDecoder.setJwtValidator(jwtValidator());
        return jwtDecoder;
    }
    
    /**
     * JWT validator with custom rules
     */
    @Bean
    public Jwt.Validator<Jwt> jwtValidator() {
        List<Oauth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new JwtIssuerValidator("http://localhost:9000"));
        validators.add(new JwtAudienceValidator("java-journey-api"));
        
        return new DelegatingOAuth2TokenValidator<>(validators);
    }
    
    /**
     * Convert JWT to Spring Security authentication
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");
        
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        jwtConverter.setPrincipalClaimName("username");
        
        return jwtConverter;
    }
}
```

## 4. Cryptography in Java

### Symmetric and Asymmetric Encryption
```java
package com.javajourney.security.crypto;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Comprehensive cryptography utilities for Java applications
 */
public class CryptographyUtils {
    
    /**
     * Symmetric encryption using AES
     */
    public static class SymmetricCrypto {
        private static final String AES_ALGORITHM = "AES";
        private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
        private static final int GCM_IV_LENGTH = 12;
        private static final int GCM_TAG_LENGTH = 16;
        
        /**
         * Generate AES key
         */
        public static SecretKey generateAESKey(int keySize) throws NoSuchAlgorithmException {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(keySize);
            return keyGenerator.generateKey();
        }
        
        /**
         * Encrypt data using AES-GCM
         */
        public static EncryptionResult encryptAESGCM(String plaintext, SecretKey key) 
                throws Exception {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            
            // Generate random IV
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            return new EncryptionResult(
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(iv)
            );
        }
        
        /**
         * Decrypt data using AES-GCM
         */
        public static String decryptAESGCM(String encryptedData, String ivString, SecretKey key) 
                throws Exception {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            
            byte[] iv = Base64.getDecoder().decode(ivString);
            byte[] ciphertext = Base64.getDecoder().decode(encryptedData);
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");
        }
        
        /**
         * Key derivation using PBKDF2
         */
        public static SecretKey deriveKeyFromPassword(String password, byte[] salt, 
                                                    int iterations, int keyLength) throws Exception {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec);
        }
    }
    
    /**
     * Asymmetric encryption using RSA
     */
    public static class AsymmetricCrypto {
        private static final String RSA_ALGORITHM = "RSA";
        private static final String RSA_TRANSFORMATION = "RSA/OAEP/SHA-256";
        
        /**
         * Generate RSA key pair
         */
        public static KeyPair generateRSAKeyPair(int keySize) throws NoSuchAlgorithmException {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(keySize);
            return keyGen.generateKeyPair();
        }
        
        /**
         * Encrypt using RSA public key
         */
        public static String encryptRSA(String plaintext, PublicKey publicKey) throws Exception {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(ciphertext);
        }
        
        /**
         * Decrypt using RSA private key
         */
        public static String decryptRSA(String encryptedData, PrivateKey privateKey) throws Exception {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            
            byte[] ciphertext = Base64.getDecoder().decode(encryptedData);
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");
        }
        
        /**
         * Export key to PEM format
         */
        public static String exportKeyToPEM(Key key, String keyType) {
            byte[] encoded = key.getEncoded();
            String base64Key = Base64.getEncoder().encodeToString(encoded);
            
            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN ").append(keyType).append("-----\n");
            
            // Add line breaks every 64 characters
            for (int i = 0; i < base64Key.length(); i += 64) {
                pem.append(base64Key, i, Math.min(i + 64, base64Key.length()));
                pem.append("\n");
            }
            
            pem.append("-----END ").append(keyType).append("-----");
            return pem.toString();
        }
    }
    
    /**
     * Digital signatures
     */
    public static class DigitalSignature {
        private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
        
        /**
         * Sign data using private key
         */
        public static String signData(String data, PrivateKey privateKey) throws Exception {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes("UTF-8"));
            
            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        }
        
        /**
         * Verify signature using public key
         */
        public static boolean verifySignature(String data, String signatureString, 
                                            PublicKey publicKey) throws Exception {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes("UTF-8"));
            
            byte[] signatureBytes = Base64.getDecoder().decode(signatureString);
            return signature.verify(signatureBytes);
        }
    }
    
    /**
     * Hashing utilities
     */
    public static class HashUtils {
        
        /**
         * Generate SHA-256 hash
         */
        public static String sha256Hash(String input) throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        }
        
        /**
         * Generate HMAC-SHA256
         */
        public static String hmacSHA256(String data, SecretKey key) throws Exception {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] hmac = mac.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hmac);
        }
        
        /**
         * Secure random salt generation
         */
        public static byte[] generateSalt(int length) {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[length];
            random.nextBytes(salt);
            return salt;
        }
    }
    
    /**
     * Encryption result container
     */
    public static class EncryptionResult {
        private final String encryptedData;
        private final String iv;
        
        public EncryptionResult(String encryptedData, String iv) {
            this.encryptedData = encryptedData;
            this.iv = iv;
        }
        
        public String getEncryptedData() { return encryptedData; }
        public String getIv() { return iv; }
    }
}
```

## 5. Security Best Practices

### Input Validation and Sanitization
```java
package com.javajourney.security.validation;

import java.util.regex.Pattern;

/**
 * Input validation and sanitization utilities
 */
public class InputValidation {
    
    // Common validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$");
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=", 
        Pattern.CASE_INSENSITIVE);
    
    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate phone number
     */
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Check for SQL injection patterns
     */
    public static boolean containsSQLInjection(String input) {
        return input != null && SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    /**
     * Check for XSS patterns
     */
    public static boolean containsXSS(String input) {
        return input != null && XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * Sanitize string for HTML output
     */
    public static String sanitizeForHTML(String input) {
        if (input == null) return null;
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }
    
    /**
     * Validate and sanitize user input
     */
    public static ValidationResult validateAndSanitize(String input, ValidationType type) {
        if (input == null || input.trim().isEmpty()) {
            return new ValidationResult(false, "Input cannot be empty", null);
        }
        
        // Check for malicious patterns
        if (containsSQLInjection(input)) {
            return new ValidationResult(false, "Input contains potentially malicious content", null);
        }
        
        if (containsXSS(input)) {
            return new ValidationResult(false, "Input contains potentially malicious script", null);
        }
        
        // Type-specific validation
        boolean isValid = switch (type) {
            case EMAIL -> isValidEmail(input);
            case PHONE -> isValidPhoneNumber(input);
            case ALPHANUMERIC -> input.matches("^[a-zA-Z0-9]+$");
            case TEXT -> true; // Basic validation passed
        };
        
        if (!isValid) {
            return new ValidationResult(false, "Input format is invalid", null);
        }
        
        // Sanitize the input
        String sanitized = sanitizeForHTML(input.trim());
        return new ValidationResult(true, "Valid input", sanitized);
    }
    
    public enum ValidationType {
        EMAIL, PHONE, ALPHANUMERIC, TEXT
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String sanitizedInput;
        
        public ValidationResult(boolean valid, String message, String sanitizedInput) {
            this.valid = valid;
            this.message = message;
            this.sanitizedInput = sanitizedInput;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getSanitizedInput() { return sanitizedInput; }
    }
}
```

## 6. Practical Security Implementation

### Secure REST API Example
```java
package com.javajourney.security.api;

/**
 * Secure REST API implementation with comprehensive security measures
 */
@RestController
@RequestMapping("/api/secure")
@Validated
public class SecureApiController {
    
    private final JwtTokenService jwtTokenService;
    private final InputValidation inputValidation;
    private final AuditService auditService;
    
    /**
     * Secure login endpoint with rate limiting
     */
    @PostMapping("/login")
    @RateLimited(maxRequests = 5, windowMinutes = 15) // Custom rate limiting annotation
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, 
                                             HttpServletRequest httpRequest) {
        
        // Validate input
        ValidationResult validation = inputValidation.validateAndSanitize(
            request.getUsername(), ValidationType.ALPHANUMERIC);
        
        if (!validation.isValid()) {
            auditService.logSecurityEvent("INVALID_LOGIN_ATTEMPT", 
                request.getUsername(), httpRequest.getRemoteAddr());
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, null, "Invalid input"));
        }
        
        try {
            // Authenticate user (implementation depends on your user store)
            UserDetails user = authenticateUser(validation.getSanitizedInput(), 
                                              request.getPassword());
            
            // Generate tokens
            Collection<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
            
            String accessToken = jwtTokenService.generateAccessToken(
                user.getUsername(), roles, null);
            String refreshToken = jwtTokenService.generateRefreshToken(user.getUsername());
            
            // Log successful login
            auditService.logSecurityEvent("SUCCESSFUL_LOGIN", 
                user.getUsername(), httpRequest.getRemoteAddr());
            
            return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, "Success"));
            
        } catch (AuthenticationException e) {
            // Log failed attempt
            auditService.logSecurityEvent("FAILED_LOGIN_ATTEMPT", 
                request.getUsername(), httpRequest.getRemoteAddr());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(null, null, "Authentication failed"));
        }
    }
    
    /**
     * Secure data endpoint with role-based access
     */
    @GetMapping("/data")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SecureData>> getSecureData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        // Validate pagination parameters
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }
        
        // Get user-specific data
        String username = authentication.getName();
        List<SecureData> data = secureDataService.getUserData(username, page, size);
        
        // Log data access
        auditService.logDataAccess("SECURE_DATA_ACCESS", username, data.size());
        
        return ResponseEntity.ok(data);
    }
    
    /**
     * File upload with security validation
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        // Validate file
        FileValidationResult validation = validateFile(file);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest()
                .body(new UploadResponse(false, validation.getErrorMessage()));
        }
        
        try {
            // Process file securely
            String filename = secureFileService.saveFile(file, authentication.getName());
            
            auditService.logFileUpload("FILE_UPLOAD", authentication.getName(), 
                                     file.getOriginalFilename(), file.getSize());
            
            return ResponseEntity.ok(new UploadResponse(true, "File uploaded: " + filename));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UploadResponse(false, "Upload failed"));
        }
    }
    
    private FileValidationResult validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            return new FileValidationResult(false, "File too large");
        }
        
        // Check file type
        String contentType = file.getContentType();
        if (!isAllowedContentType(contentType)) {
            return new FileValidationResult(false, "Invalid file type");
        }
        
        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..") || 
            filename.contains("/") || filename.contains("\\")) {
            return new FileValidationResult(false, "Invalid filename");
        }
        
        return new FileValidationResult(true, "Valid file");
    }
    
    private boolean isAllowedContentType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("application/pdf") ||
            contentType.equals("text/plain")
        );
    }
}
```

## Practice Exercises

### Exercise 1: Implement Multi-Factor Authentication
Create a complete MFA system with TOTP support and backup codes.

### Exercise 2: Build OAuth 2.0 Client
Implement an OAuth 2.0 client that integrates with Google or GitHub.

### Exercise 3: Secure File Storage
Create a secure file storage system with encryption at rest and access controls.

### Exercise 4: Security Audit System
Build a comprehensive security audit system that logs and analyzes security events.

## Key Takeaways

1. **Defense in Depth**: Implement multiple layers of security controls
2. **Input Validation**: Always validate and sanitize user input
3. **Cryptography**: Use established algorithms and libraries, never roll your own crypto
4. **Token Security**: Implement proper token lifecycle management and validation
5. **Audit Logging**: Log all security-relevant events for monitoring and compliance
6. **Least Privilege**: Grant minimum necessary permissions to users and applications

## Next Steps
Tomorrow we'll explore Advanced Database Patterns, covering connection pooling, transaction management, and database optimization techniques that complement the security measures we've established today.