# Day 108: Advanced Security & Authentication - OAuth2, JWT, Zero Trust Architecture

## Learning Objectives
- Master OAuth2 and OpenID Connect authentication flows
- Implement JWT with advanced security patterns and proper validation
- Design Zero Trust Architecture with adaptive authentication
- Create secure microservices communication patterns
- Build comprehensive security monitoring and threat detection
- Implement RBAC, ABAC, and fine-grained authorization systems

## Part 1: OAuth2 & OpenID Connect Implementation

### OAuth2 Resource Server with JWT

```java
package com.securetrading.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret:defaultSecretKeyThatShouldBeChangedInProduction}")
    private String jwtSecret;

    @Value("${jwt.issuer:secure-trading-platform}")
    private String jwtIssuer;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**", "/public/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/trader/**").hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "TRADER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new SecurityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256"))
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        authenticationConverter.setPrincipalClaimName("sub");
        return authenticationConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("https://*.securetrading.com", "http://localhost:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### Advanced JWT Service with Security Features

```java
package com.securetrading.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);
    
    private final SecretKey secretKey;
    private final String issuer;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    
    // Token blacklist for revoked tokens
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    public JwtTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-expiration:900000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public TokenPair generateTokenPair(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(accessTokenExpirationMs, ChronoUnit.MILLIS);
        Instant refreshExpiry = now.plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
        
        // Generate unique token ID for tracking
        String tokenId = UUID.randomUUID().toString();
        
        // Access Token
        String accessToken = Jwts.builder()
                .setId(tokenId)
                .setSubject(userPrincipal.getUsername())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(accessExpiry))
                .claim("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("userId", userPrincipal.getId())
                .claim("email", userPrincipal.getEmail())
                .claim("type", "access")
                .claim("sessionId", generateSessionId())
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        // Refresh Token
        String refreshToken = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userPrincipal.getUsername())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(refreshExpiry))
                .claim("type", "refresh")
                .claim("accessTokenId", tokenId)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        logger.info("Generated token pair for user: {} with session: {}", 
                userPrincipal.getUsername(), tokenId);

        return new TokenPair(accessToken, refreshToken, accessExpiry, refreshExpiry);
    }

    public Claims validateToken(String token) {
        if (blacklistedTokens.contains(extractTokenId(token))) {
            throw new JwtException("Token has been revoked");
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            throw new JwtException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
            throw new JwtException("Malformed token", e);
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid signature", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public String refreshAccessToken(String refreshToken) {
        Claims claims = validateToken(refreshToken);
        
        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtException("Invalid token type for refresh");
        }

        // Blacklist the old access token
        String oldAccessTokenId = claims.get("accessTokenId", String.class);
        if (oldAccessTokenId != null) {
            blacklistedTokens.add(oldAccessTokenId);
        }

        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationMs, ChronoUnit.MILLIS);
        String newTokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(newTokenId)
                .setSubject(claims.getSubject())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .claim("type", "access")
                .claim("sessionId", generateSessionId())
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public void revokeToken(String token) {
        String tokenId = extractTokenId(token);
        blacklistedTokens.add(tokenId);
        logger.info("Token revoked: {}", tokenId);
    }

    private String extractTokenId(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getId();
        } catch (Exception e) {
            logger.warn("Failed to extract token ID: {}", e.getMessage());
            return null;
        }
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final Instant accessTokenExpiry;
        private final Instant refreshTokenExpiry;

        public TokenPair(String accessToken, String refreshToken, 
                        Instant accessTokenExpiry, Instant refreshTokenExpiry) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessTokenExpiry = accessTokenExpiry;
            this.refreshTokenExpiry = refreshTokenExpiry;
        }

        // Getters
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public Instant getAccessTokenExpiry() { return accessTokenExpiry; }
        public Instant getRefreshTokenExpiry() { return refreshTokenExpiry; }
    }
}
```

## Part 2: Zero Trust Architecture Implementation

### Adaptive Authentication Service

```java
package com.securetrading.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdaptiveAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveAuthenticationService.class);

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private DeviceFingerprintService deviceFingerprintService;

    @Autowired
    private GeoLocationService geoLocationService;

    private final Map<String, UserBehaviorProfile> userProfiles = new ConcurrentHashMap<>();

    public AuthenticationResult authenticateWithAdaptiveRisk(AuthenticationRequest request) {
        logger.info("Starting adaptive authentication for user: {}", request.getUsername());

        // 1. Basic credential validation
        boolean credentialsValid = validateCredentials(request);
        if (!credentialsValid) {
            return AuthenticationResult.failed("Invalid credentials", RiskLevel.HIGH);
        }

        // 2. Risk assessment
        RiskScore riskScore = calculateRiskScore(request);
        
        // 3. Device fingerprinting
        DeviceFingerprint deviceFingerprint = deviceFingerprintService.generateFingerprint(request);
        
        // 4. Behavior analysis
        BehaviorAnalysis behaviorAnalysis = analyzeBehavior(request);

        // 5. Determine authentication requirements based on risk
        AuthenticationRequirements requirements = determineRequirements(riskScore, behaviorAnalysis);

        // 6. Update user behavior profile
        updateUserProfile(request.getUsername(), request, riskScore);

        logger.info("Authentication completed for user: {} with risk level: {}", 
                request.getUsername(), riskScore.getLevel());

        return new AuthenticationResult(
                credentialsValid,
                riskScore,
                requirements,
                deviceFingerprint,
                behaviorAnalysis
        );
    }

    private RiskScore calculateRiskScore(AuthenticationRequest request) {
        RiskScoreBuilder builder = new RiskScoreBuilder();

        // Geographic risk
        GeoLocation location = geoLocationService.getLocation(request.getIpAddress());
        builder.addGeographicRisk(location, request.getUsername());

        // Time-based risk
        builder.addTimeBasedRisk(LocalDateTime.now(), request.getUsername());

        // Device risk
        builder.addDeviceRisk(request.getDeviceInfo());

        // Network risk
        builder.addNetworkRisk(request.getIpAddress(), request.getUserAgent());

        // Velocity risk (multiple attempts)
        builder.addVelocityRisk(request.getUsername(), request.getIpAddress());

        return builder.build();
    }

    private BehaviorAnalysis analyzeBehavior(AuthenticationRequest request) {
        UserBehaviorProfile profile = userProfiles.get(request.getUsername());
        
        if (profile == null) {
            return new BehaviorAnalysis(BehaviorRisk.UNKNOWN, "New user - establishing baseline");
        }

        List<BehaviorAnomaly> anomalies = new ArrayList<>();

        // Check for unusual login times
        if (!profile.isTypicalLoginTime(LocalDateTime.now())) {
            anomalies.add(new BehaviorAnomaly("unusual_time", "Login outside typical hours", 0.3));
        }

        // Check for unusual locations
        GeoLocation currentLocation = geoLocationService.getLocation(request.getIpAddress());
        if (!profile.isTypicalLocation(currentLocation)) {
            double distance = profile.calculateDistanceFromUsualLocations(currentLocation);
            if (distance > 1000) { // More than 1000km from usual locations
                anomalies.add(new BehaviorAnomaly("unusual_location", 
                        "Login from unusual geographic location", Math.min(distance / 5000, 1.0)));
            }
        }

        // Check for unusual devices
        if (!profile.isKnownDevice(request.getDeviceInfo())) {
            anomalies.add(new BehaviorAnomaly("new_device", "Login from new device", 0.4));
        }

        // Calculate overall behavior risk
        double totalAnomalyScore = anomalies.stream()
                .mapToDouble(BehaviorAnomaly::getScore)
                .sum();

        BehaviorRisk behaviorRisk;
        if (totalAnomalyScore < 0.3) {
            behaviorRisk = BehaviorRisk.LOW;
        } else if (totalAnomalyScore < 0.7) {
            behaviorRisk = BehaviorRisk.MEDIUM;
        } else {
            behaviorRisk = BehaviorRisk.HIGH;
        }

        return new BehaviorAnalysis(behaviorRisk, anomalies);
    }

    private AuthenticationRequirements determineRequirements(RiskScore riskScore, BehaviorAnalysis behaviorAnalysis) {
        List<AuthenticationFactor> requiredFactors = new ArrayList<>();
        requiredFactors.add(AuthenticationFactor.PASSWORD); // Always required

        // Determine additional factors based on risk
        if (riskScore.getScore() > 0.3 || behaviorAnalysis.getRisk() == BehaviorRisk.MEDIUM) {
            requiredFactors.add(AuthenticationFactor.SMS_OTP);
        }

        if (riskScore.getScore() > 0.6 || behaviorAnalysis.getRisk() == BehaviorRisk.HIGH) {
            requiredFactors.add(AuthenticationFactor.AUTHENTICATOR_APP);
        }

        if (riskScore.getScore() > 0.8) {
            requiredFactors.add(AuthenticationFactor.BIOMETRIC);
        }

        // Determine session restrictions
        SessionRestrictions restrictions = new SessionRestrictions();
        if (riskScore.getScore() > 0.5) {
            restrictions.setShortSession(true);
            restrictions.setRequireReauthentication(true);
            restrictions.setLimitedPrivileges(true);
        }

        return new AuthenticationRequirements(requiredFactors, restrictions);
    }

    private void updateUserProfile(String username, AuthenticationRequest request, RiskScore riskScore) {
        UserBehaviorProfile profile = userProfiles.computeIfAbsent(username, 
                k -> new UserBehaviorProfile(username));
        
        profile.recordLogin(
                LocalDateTime.now(),
                geoLocationService.getLocation(request.getIpAddress()),
                request.getDeviceInfo(),
                riskScore
        );
    }

    private boolean validateCredentials(AuthenticationRequest request) {
        // Implementation would integrate with your user service
        // This is a placeholder
        return true;
    }

    // Supporting classes
    public static class RiskScoreBuilder {
        private double totalScore = 0.0;
        private List<RiskFactor> factors = new ArrayList<>();

        public RiskScoreBuilder addGeographicRisk(GeoLocation location, String username) {
            // Implementation for geographic risk calculation
            return this;
        }

        public RiskScoreBuilder addTimeBasedRisk(LocalDateTime loginTime, String username) {
            // Implementation for time-based risk calculation
            return this;
        }

        public RiskScoreBuilder addDeviceRisk(String deviceInfo) {
            // Implementation for device risk calculation
            return this;
        }

        public RiskScoreBuilder addNetworkRisk(String ipAddress, String userAgent) {
            // Implementation for network risk calculation
            return this;
        }

        public RiskScoreBuilder addVelocityRisk(String username, String ipAddress) {
            // Implementation for velocity risk calculation
            return this;
        }

        public RiskScore build() {
            RiskLevel level;
            if (totalScore < 0.3) {
                level = RiskLevel.LOW;
            } else if (totalScore < 0.7) {
                level = RiskLevel.MEDIUM;
            } else {
                level = RiskLevel.HIGH;
            }

            return new RiskScore(totalScore, level, factors);
        }
    }

    // Enums and data classes
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    public enum BehaviorRisk { LOW, MEDIUM, HIGH, UNKNOWN }
    public enum AuthenticationFactor { PASSWORD, SMS_OTP, AUTHENTICATOR_APP, BIOMETRIC, HARDWARE_TOKEN }

    public static class AuthenticationRequest {
        private String username;
        private String password;
        private String ipAddress;
        private String userAgent;
        private String deviceInfo;

        // Constructors, getters, setters
        public AuthenticationRequest(String username, String password, String ipAddress, 
                String userAgent, String deviceInfo) {
            this.username = username;
            this.password = password;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.deviceInfo = deviceInfo;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
        public String getDeviceInfo() { return deviceInfo; }
    }

    public static class AuthenticationResult {
        private final boolean successful;
        private final RiskScore riskScore;
        private final AuthenticationRequirements requirements;
        private final DeviceFingerprint deviceFingerprint;
        private final BehaviorAnalysis behaviorAnalysis;

        public AuthenticationResult(boolean successful, RiskScore riskScore, 
                AuthenticationRequirements requirements, DeviceFingerprint deviceFingerprint,
                BehaviorAnalysis behaviorAnalysis) {
            this.successful = successful;
            this.riskScore = riskScore;
            this.requirements = requirements;
            this.deviceFingerprint = deviceFingerprint;
            this.behaviorAnalysis = behaviorAnalysis;
        }

        public static AuthenticationResult failed(String reason, RiskLevel riskLevel) {
            return new AuthenticationResult(false, 
                    new RiskScore(1.0, riskLevel, List.of()), null, null, null);
        }

        // Getters
        public boolean isSuccessful() { return successful; }
        public RiskScore getRiskScore() { return riskScore; }
        public AuthenticationRequirements getRequirements() { return requirements; }
        public DeviceFingerprint getDeviceFingerprint() { return deviceFingerprint; }
        public BehaviorAnalysis getBehaviorAnalysis() { return behaviorAnalysis; }
    }

    // Additional supporting classes would be implemented here
    public static class RiskScore {
        private final double score;
        private final RiskLevel level;
        private final List<RiskFactor> factors;

        public RiskScore(double score, RiskLevel level, List<RiskFactor> factors) {
            this.score = score;
            this.level = level;
            this.factors = factors;
        }

        public double getScore() { return score; }
        public RiskLevel getLevel() { return level; }
        public List<RiskFactor> getFactors() { return factors; }
    }

    public static class RiskFactor {
        private final String type;
        private final String description;
        private final double impact;

        public RiskFactor(String type, String description, double impact) {
            this.type = type;
            this.description = description;
            this.impact = impact;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public double getImpact() { return impact; }
    }

    public static class AuthenticationRequirements {
        private final List<AuthenticationFactor> requiredFactors;
        private final SessionRestrictions restrictions;

        public AuthenticationRequirements(List<AuthenticationFactor> requiredFactors, 
                SessionRestrictions restrictions) {
            this.requiredFactors = requiredFactors;
            this.restrictions = restrictions;
        }

        public List<AuthenticationFactor> getRequiredFactors() { return requiredFactors; }
        public SessionRestrictions getRestrictions() { return restrictions; }
    }

    public static class SessionRestrictions {
        private boolean shortSession;
        private boolean requireReauthentication;
        private boolean limitedPrivileges;

        public boolean isShortSession() { return shortSession; }
        public void setShortSession(boolean shortSession) { this.shortSession = shortSession; }
        public boolean isRequireReauthentication() { return requireReauthentication; }
        public void setRequireReauthentication(boolean requireReauthentication) { 
            this.requireReauthentication = requireReauthentication; 
        }
        public boolean isLimitedPrivileges() { return limitedPrivileges; }
        public void setLimitedPrivileges(boolean limitedPrivileges) { 
            this.limitedPrivileges = limitedPrivileges; 
        }
    }
}
```

## Part 3: Microservices Security Patterns

### Secure Service-to-Service Communication

```java
package com.securetrading.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class ServiceToServiceAuthenticator implements ClientHttpRequestInterceptor {

    @Value("${service.auth.secret}")
    private String serviceSecret;

    @Value("${service.name}")
    private String serviceName;

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String AUTH_HEADER = "X-Service-Auth";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String NONCE_HEADER = "X-Nonce";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // Add authentication headers
        addAuthHeaders(request, body);
        
        return execution.execute(request, body);
    }

    private void addAuthHeaders(HttpRequest request, byte[] body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        
        // Create signature payload
        String payload = createSignaturePayload(request, body, timestamp, nonce);
        
        // Generate HMAC signature
        String signature = generateHmacSignature(payload);
        
        // Add headers
        HttpHeaders headers = request.getHeaders();
        headers.add(AUTH_HEADER, String.format("%s:%s", serviceName, signature));
        headers.add(TIMESTAMP_HEADER, timestamp);
        headers.add(NONCE_HEADER, nonce);
        headers.add("X-Service-Name", serviceName);
    }

    private String createSignaturePayload(HttpRequest request, byte[] body, 
            String timestamp, String nonce) {
        StringBuilder payload = new StringBuilder();
        payload.append(request.getMethod().name()).append("\n");
        payload.append(request.getURI().getPath()).append("\n");
        payload.append(request.getURI().getQuery() != null ? request.getURI().getQuery() : "").append("\n");
        payload.append(timestamp).append("\n");
        payload.append(nonce).append("\n");
        
        if (body != null && body.length > 0) {
            payload.append(Base64.getEncoder().encodeToString(body));
        }
        
        return payload.toString();
    }

    private String generateHmacSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    serviceSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    public static RestTemplate createSecureRestTemplate(ServiceToServiceAuthenticator authenticator) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(authenticator);
        return restTemplate;
    }
}
```

### Security Headers Filter

```java
package com.securetrading.security.filter;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
            FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Security headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("Permissions-Policy", 
                "camera=(), microphone=(), geolocation=(), payment=()");
        
        // HSTS for HTTPS
        if (httpRequest.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload");
        }

        // CSP for modern browsers
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https:; " +
                "connect-src 'self' https://api.securetrading.com; " +
                "frame-ancestors 'none'");

        chain.doFilter(request, response);
    }
}
```

## Part 4: Advanced Authorization Patterns

### Role-Based and Attribute-Based Access Control

```java
package com.securetrading.security.authorization;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;

@Service("securityService")
public class SecurityService {

    private final Map<String, Set<String>> rolePermissions = new HashMap<>();
    private final Map<String, UserAttributes> userAttributes = new HashMap<>();

    public SecurityService() {
        initializeRolePermissions();
    }

    private void initializeRolePermissions() {
        rolePermissions.put("ADMIN", Set.of(
                "user:read", "user:write", "user:delete",
                "trading:read", "trading:write", "trading:execute",
                "system:admin", "audit:read"
        ));
        
        rolePermissions.put("TRADER", Set.of(
                "trading:read", "trading:write", "trading:execute",
                "portfolio:read", "portfolio:write",
                "market:read"
        ));
        
        rolePermissions.put("ANALYST", Set.of(
                "trading:read", "portfolio:read", "market:read",
                "reports:generate", "analytics:read"
        ));
        
        rolePermissions.put("USER", Set.of(
                "portfolio:read", "market:read", "profile:read", "profile:write"
        ));
    }

    @PreAuthorize("hasRole('ADMIN') or @securityService.hasPermission(authentication, 'user:read')")
    public boolean canReadUser(String targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        
        // Admin can read all users
        if (hasRole(auth, "ADMIN")) {
            return true;
        }
        
        // Users can read their own profile
        if (principal.getId().equals(targetUserId)) {
            return true;
        }
        
        // Traders can read users in their team
        if (hasRole(auth, "TRADER")) {
            return isInSameTeam(principal.getId(), targetUserId);
        }
        
        return false;
    }

    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        
        // Check role-based permissions
        boolean hasRolePermission = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String role = authority.getAuthority().replace("ROLE_", "");
                    Set<String> permissions = rolePermissions.get(role);
                    return permissions != null && permissions.contains(permission);
                });

        if (hasRolePermission) {
            return true;
        }

        // Check attribute-based permissions
        return evaluateAttributeBasedPermission(principal, permission);
    }

    private boolean evaluateAttributeBasedPermission(UserPrincipal principal, String permission) {
        UserAttributes attributes = userAttributes.get(principal.getId());
        if (attributes == null) {
            return false;
        }

        // Time-based access control
        if (permission.startsWith("trading:")) {
            LocalTime now = LocalTime.now();
            if (now.isBefore(attributes.getTradingHoursStart()) || 
                now.isAfter(attributes.getTradingHoursEnd())) {
                return false;
            }
        }

        // Location-based access control
        if (permission.contains("admin") && !attributes.isInAllowedLocation()) {
            return false;
        }

        // Risk-based access control
        if (permission.equals("trading:execute")) {
            return attributes.getRiskLevel().ordinal() <= RiskLevel.MEDIUM.ordinal();
        }

        // Department-based access control
        if (permission.equals("reports:generate")) {
            return attributes.getDepartments().contains("ANALYTICS") || 
                   attributes.getDepartments().contains("MANAGEMENT");
        }

        return false;
    }

    public boolean canExecuteTrade(String userId, double amount) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        
        if (!hasPermission(auth, "trading:execute")) {
            return false;
        }

        UserAttributes attributes = userAttributes.get(principal.getId());
        if (attributes == null) {
            return false;
        }

        // Check trading limits
        return amount <= attributes.getTradingLimit();
    }

    public boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private boolean isInSameTeam(String userId1, String userId2) {
        UserAttributes attr1 = userAttributes.get(userId1);
        UserAttributes attr2 = userAttributes.get(userId2);
        
        if (attr1 == null || attr2 == null) {
            return false;
        }
        
        return attr1.getTeamId().equals(attr2.getTeamId());
    }

    // Supporting classes
    public static class UserAttributes {
        private String teamId;
        private Set<String> departments;
        private RiskLevel riskLevel;
        private double tradingLimit;
        private LocalTime tradingHoursStart;
        private LocalTime tradingHoursEnd;
        private boolean inAllowedLocation;

        // Constructors, getters, setters
        public UserAttributes(String teamId, Set<String> departments, RiskLevel riskLevel,
                double tradingLimit, LocalTime tradingHoursStart, LocalTime tradingHoursEnd,
                boolean inAllowedLocation) {
            this.teamId = teamId;
            this.departments = departments;
            this.riskLevel = riskLevel;
            this.tradingLimit = tradingLimit;
            this.tradingHoursStart = tradingHoursStart;
            this.tradingHoursEnd = tradingHoursEnd;
            this.inAllowedLocation = inAllowedLocation;
        }

        public String getTeamId() { return teamId; }
        public Set<String> getDepartments() { return departments; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public double getTradingLimit() { return tradingLimit; }
        public LocalTime getTradingHoursStart() { return tradingHoursStart; }
        public LocalTime getTradingHoursEnd() { return tradingHoursEnd; }
        public boolean isInAllowedLocation() { return inAllowedLocation; }
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
```

## Part 5: Security Monitoring and Threat Detection

### Security Event Monitoring

```java
package com.securetrading.security.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SecurityEventMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityEventMonitor.class);
    
    @Autowired
    private ThreatDetectionService threatDetectionService;
    
    @Autowired
    private SecurityAlertsService alertsService;
    
    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastFailedAttempt = new ConcurrentHashMap<>();

    @EventListener
    public void handleAuthenticationSuccess(AbstractAuthenticationEvent event) {
        String username = event.getAuthentication().getName();
        String clientInfo = extractClientInfo(event);
        
        logger.info("Successful authentication for user: {} from {}", username, clientInfo);
        
        // Reset failed attempts on successful login
        failedAttempts.remove(username);
        lastFailedAttempt.remove(username);
        
        // Log security event
        SecurityEvent securityEvent = new SecurityEvent(
                SecurityEventType.AUTHENTICATION_SUCCESS,
                username,
                clientInfo,
                LocalDateTime.now(),
                "User successfully authenticated"
        );
        
        threatDetectionService.analyzeSecurityEvent(securityEvent);
    }

    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String clientInfo = extractClientInfo(event);
        String reason = event.getException().getMessage();
        
        logger.warn("Failed authentication for user: {} from {} - Reason: {}", 
                username, clientInfo, reason);
        
        // Track failed attempts
        int attempts = failedAttempts.computeIfAbsent(username, k -> new AtomicInteger(0))
                .incrementAndGet();
        lastFailedAttempt.put(username, LocalDateTime.now());
        
        // Check for brute force attacks
        if (attempts >= 5) {
            SecurityThreat threat = new SecurityThreat(
                    ThreatType.BRUTE_FORCE,
                    ThreatSeverity.HIGH,
                    username,
                    clientInfo,
                    String.format("Multiple failed login attempts: %d", attempts)
            );
            
            threatDetectionService.handleThreat(threat);
            alertsService.sendSecurityAlert(threat);
        }
        
        // Log security event
        SecurityEvent securityEvent = new SecurityEvent(
                SecurityEventType.AUTHENTICATION_FAILURE,
                username,
                clientInfo,
                LocalDateTime.now(),
                reason
        );
        
        threatDetectionService.analyzeSecurityEvent(securityEvent);
    }

    @EventListener
    public void handleAuthorizationDenied(AuthorizationDeniedEvent event) {
        String username = event.getAuthentication().getName();
        String resource = event.getAuthorizationDecision().toString();
        
        logger.warn("Authorization denied for user: {} accessing resource: {}", 
                username, resource);
        
        SecurityEvent securityEvent = new SecurityEvent(
                SecurityEventType.AUTHORIZATION_DENIED,
                username,
                extractClientInfo(event.getAuthentication()),
                LocalDateTime.now(),
                "Access denied to resource: " + resource
        );
        
        threatDetectionService.analyzeSecurityEvent(securityEvent);
    }

    private String extractClientInfo(Object event) {
        // Extract client IP, user agent, etc. from the event
        // This would be implemented based on your specific requirements
        return "client-info-placeholder";
    }

    // Supporting classes
    public enum SecurityEventType {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_DENIED,
        SUSPICIOUS_ACTIVITY,
        TOKEN_REVOKED,
        PASSWORD_CHANGED,
        PRIVILEGE_ESCALATION_ATTEMPT
    }

    public enum ThreatType {
        BRUTE_FORCE,
        CREDENTIAL_STUFFING,
        PRIVILEGE_ESCALATION,
        DATA_EXFILTRATION,
        SUSPICIOUS_LOGIN_PATTERN,
        TOKEN_MANIPULATION,
        API_ABUSE
    }

    public enum ThreatSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public static class SecurityEvent {
        private final SecurityEventType type;
        private final String username;
        private final String clientInfo;
        private final LocalDateTime timestamp;
        private final String description;

        public SecurityEvent(SecurityEventType type, String username, String clientInfo,
                LocalDateTime timestamp, String description) {
            this.type = type;
            this.username = username;
            this.clientInfo = clientInfo;
            this.timestamp = timestamp;
            this.description = description;
        }

        // Getters
        public SecurityEventType getType() { return type; }
        public String getUsername() { return username; }
        public String getClientInfo() { return clientInfo; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
    }

    public static class SecurityThreat {
        private final ThreatType type;
        private final ThreatSeverity severity;
        private final String username;
        private final String source;
        private final String description;
        private final LocalDateTime detectedAt;

        public SecurityThreat(ThreatType type, ThreatSeverity severity, String username,
                String source, String description) {
            this.type = type;
            this.severity = severity;
            this.username = username;
            this.source = source;
            this.description = description;
            this.detectedAt = LocalDateTime.now();
        }

        // Getters
        public ThreatType getType() { return type; }
        public ThreatSeverity getSeverity() { return severity; }
        public String getUsername() { return username; }
        public String getSource() { return source; }
        public String getDescription() { return description; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }
}
```

## Practical Exercises

### Exercise 1: OAuth2 Client Implementation
Create an OAuth2 client that integrates with Google, GitHub, or another provider using Spring Security OAuth2.

### Exercise 2: JWT Security Enhancement
Implement JWT token rotation, secure storage, and XSS protection mechanisms.

### Exercise 3: Zero Trust Network Design
Design a microservices architecture that implements zero trust principles with mutual TLS and service mesh security.

### Exercise 4: Advanced RBAC System
Build a flexible RBAC system that supports hierarchical roles, dynamic permissions, and contextual access control.

### Exercise 5: Security Monitoring Dashboard
Create a real-time security monitoring system that detects and responds to security threats automatically.

## Performance Considerations

### Security Optimization Strategies
- **Token Caching**: Implement Redis-based token validation caching
- **Async Security Processing**: Use async methods for security logging and monitoring
- **Connection Pooling**: Optimize database connections for user and role lookups
- **Rate Limiting**: Implement intelligent rate limiting to prevent abuse

### Monitoring and Metrics
- **Authentication Metrics**: Track authentication success/failure rates
- **Authorization Metrics**: Monitor access control decisions and performance
- **Security Event Metrics**: Real-time security event processing and alerting
- **Threat Detection Metrics**: ML-based anomaly detection and response times

## Security Best Practices
- Always use HTTPS in production environments
- Implement proper session management and timeout policies
- Use secure random number generation for tokens and nonces
- Regular security audits and penetration testing
- Keep security libraries and frameworks updated
- Implement defense in depth strategies
- Use secure coding practices and static code analysis
- Implement comprehensive logging and monitoring
- Regular security training for development teams

## Summary

Day 108 covered advanced security and authentication patterns essential for modern enterprise Java applications:

1. **OAuth2 & JWT Implementation**: Comprehensive token-based authentication with security best practices
2. **Zero Trust Architecture**: Adaptive authentication with risk-based access control
3. **Microservices Security**: Service-to-service authentication and secure communication patterns
4. **Advanced Authorization**: RBAC and ABAC implementations with contextual access control
5. **Security Monitoring**: Real-time threat detection and automated security response

These patterns provide the foundation for building secure, scalable applications that can adapt to evolving security threats while maintaining excellent user experience and system performance.