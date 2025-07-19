# Day 52: Structural Patterns - Adapter & Decorator

## Learning Goals

By the end of this session, you will:
- Master the Adapter pattern for interface compatibility and legacy integration
- Understand the Decorator pattern for dynamic behavior enhancement
- Implement wrapper classes and composition over inheritance
- Build flexible systems that can adapt to changing requirements
- Create extensible architectures using structural patterns
- Apply real-world scenarios for payment gateways, data processing, and UI components

## Table of Contents
1. [Adapter Pattern](#adapter-pattern)
2. [Decorator Pattern](#decorator-pattern)
3. [Advanced Adapter Implementations](#advanced-adapter)
4. [Advanced Decorator Implementations](#advanced-decorator)
5. [Hands-On Practice](#hands-on-practice)
6. [Challenges](#challenges)
7. [Summary](#summary)

---

## Adapter Pattern

### Problem and Solution

**Problem**: You need to use an existing class with an incompatible interface, or integrate with third-party libraries that don't match your system's interface.

**Solution**: Create an adapter class that wraps the existing class and translates interface calls to make it compatible with your system.

### Basic Adapter Implementation

```java
// MediaPlayerAdapter.java - Basic Adapter Pattern implementation
package com.javajourney.patterns.adapter;

/**
 * Media Player Adapter Pattern Implementation
 * Demonstrates making incompatible interfaces work together
 */

// Target interface that client expects
interface MediaPlayer {
    void play(String filename);
    void stop();
    void pause();
    String[] getSupportedFormats();
}

// Legacy audio player with incompatible interface
class LegacyAudioPlayer {
    public void playSound(String file) {
        if (file.endsWith(".mp3")) {
            System.out.println("Playing MP3 file: " + file);
        } else {
            System.out.println("Unsupported format for LegacyAudioPlayer: " + file);
        }
    }
    
    public void stopSound() {
        System.out.println("LegacyAudioPlayer stopped");
    }
    
    public String[] getAudioFormats() {
        return new String[]{"mp3"};
    }
}

// Third-party video player with different interface
class ThirdPartyVideoPlayer {
    private boolean isPlaying = false;
    
    public boolean startVideo(String videoFile) {
        if (videoFile.endsWith(".mp4") || videoFile.endsWith(".avi")) {
            isPlaying = true;
            System.out.println("ThirdPartyVideoPlayer starting: " + videoFile);
            return true;
        }
        System.out.println("Unsupported video format: " + videoFile);
        return false;
    }
    
    public void haltVideo() {
        isPlaying = false;
        System.out.println("ThirdPartyVideoPlayer halted");
    }
    
    public void pauseVideo() {
        if (isPlaying) {
            System.out.println("ThirdPartyVideoPlayer paused");
        }
    }
    
    public String[] getSupportedVideoTypes() {
        return new String[]{"mp4", "avi"};
    }
}

// Adapter for legacy audio player
class AudioPlayerAdapter implements MediaPlayer {
    private LegacyAudioPlayer legacyPlayer;
    
    public AudioPlayerAdapter(LegacyAudioPlayer legacyPlayer) {
        this.legacyPlayer = legacyPlayer;
    }
    
    @Override
    public void play(String filename) {
        legacyPlayer.playSound(filename);
    }
    
    @Override
    public void stop() {
        legacyPlayer.stopSound();
    }
    
    @Override
    public void pause() {
        // Legacy player doesn't support pause, so we stop instead
        System.out.println("Legacy player doesn't support pause, stopping instead");
        legacyPlayer.stopSound();
    }
    
    @Override
    public String[] getSupportedFormats() {
        return legacyPlayer.getAudioFormats();
    }
}

// Adapter for third-party video player
class VideoPlayerAdapter implements MediaPlayer {
    private ThirdPartyVideoPlayer videoPlayer;
    
    public VideoPlayerAdapter(ThirdPartyVideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }
    
    @Override
    public void play(String filename) {
        boolean success = videoPlayer.startVideo(filename);
        if (!success) {
            System.out.println("Failed to play video: " + filename);
        }
    }
    
    @Override
    public void stop() {
        videoPlayer.haltVideo();
    }
    
    @Override
    public void pause() {
        videoPlayer.pauseVideo();
    }
    
    @Override
    public String[] getSupportedFormats() {
        return videoPlayer.getSupportedVideoTypes();
    }
}

// Universal media player that uses adapters
class UniversalMediaPlayer implements MediaPlayer {
    private MediaPlayer audioAdapter;
    private MediaPlayer videoAdapter;
    private MediaPlayer currentPlayer;
    
    public UniversalMediaPlayer() {
        this.audioAdapter = new AudioPlayerAdapter(new LegacyAudioPlayer());
        this.videoAdapter = new VideoPlayerAdapter(new ThirdPartyVideoPlayer());
    }
    
    @Override
    public void play(String filename) {
        String extension = getFileExtension(filename);
        
        switch (extension.toLowerCase()) {
            case "mp3" -> {
                currentPlayer = audioAdapter;
                audioAdapter.play(filename);
            }
            case "mp4", "avi" -> {
                currentPlayer = videoAdapter;
                videoAdapter.play(filename);
            }
            default -> System.out.println("Unsupported file format: " + extension);
        }
    }
    
    @Override
    public void stop() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer = null;
        }
    }
    
    @Override
    public void pause() {
        if (currentPlayer != null) {
            currentPlayer.pause();
        }
    }
    
    @Override
    public String[] getSupportedFormats() {
        String[] audioFormats = audioAdapter.getSupportedFormats();
        String[] videoFormats = videoAdapter.getSupportedFormats();
        
        String[] allFormats = new String[audioFormats.length + videoFormats.length];
        System.arraycopy(audioFormats, 0, allFormats, 0, audioFormats.length);
        System.arraycopy(videoFormats, 0, allFormats, audioFormats.length, videoFormats.length);
        
        return allFormats;
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot + 1) : "";
    }
}
```

### Payment Gateway Adapter

```java
// PaymentGatewayAdapter.java - Real-world adapter for payment systems
package com.javajourney.patterns.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Payment Gateway Adapter Implementation
 * Demonstrates adapting different payment providers to a common interface
 */

// Common payment interface for our system
interface PaymentProcessor {
    PaymentResult processPayment(PaymentRequest request);
    boolean refundPayment(String transactionId, BigDecimal amount);
    PaymentStatus getPaymentStatus(String transactionId);
    boolean isAvailable();
    String getProviderName();
}

// Payment request/response classes
class PaymentRequest {
    private BigDecimal amount;
    private String currency;
    private String customerEmail;
    private String description;
    private Map<String, String> metadata;
    
    public PaymentRequest(BigDecimal amount, String currency, String customerEmail, String description) {
        this.amount = amount;
        this.currency = currency;
        this.customerEmail = customerEmail;
        this.description = description;
        this.metadata = new HashMap<>();
    }
    
    // Getters and setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getMetadata() { return metadata; }
    public void addMetadata(String key, String value) { this.metadata.put(key, value); }
}

class PaymentResult {
    private boolean success;
    private String transactionId;
    private String errorMessage;
    private LocalDateTime timestamp;
    private BigDecimal actualAmount;
    private BigDecimal fees;
    
    public PaymentResult(boolean success, String transactionId) {
        this.success = success;
        this.transactionId = transactionId;
        this.timestamp = LocalDateTime.now();
    }
    
    public PaymentResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
}

enum PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
}

// Legacy payment provider (different interface)
class LegacyPayPalAPI {
    public String makePayment(double amount, String email, String note) {
        // Simulate PayPal payment processing
        if (amount > 0 && email != null && email.contains("@")) {
            String transactionId = "PAYPAL_" + System.currentTimeMillis();
            System.out.println("PayPal payment processed: $" + amount + " to " + email);
            return transactionId;
        }
        return null;
    }
    
    public boolean cancelTransaction(String transactionId) {
        System.out.println("PayPal transaction cancelled: " + transactionId);
        return true;
    }
    
    public String getTransactionStatus(String transactionId) {
        return "COMPLETED"; // Simplified for demo
    }
    
    public boolean isServiceOnline() {
        return true; // Simulate service availability
    }
}

// Modern payment provider (different interface)
class ModernStripeAPI {
    public StripePaymentResponse charge(StripeChargeRequest request) {
        // Simulate Stripe payment processing
        if (request.isValid()) {
            String chargeId = "STRIPE_" + System.currentTimeMillis();
            System.out.println("Stripe payment processed: " + request.getAmountCents() + 
                             " cents for " + request.getCustomerEmail());
            
            StripePaymentResponse response = new StripePaymentResponse();
            response.setId(chargeId);
            response.setStatus("succeeded");
            response.setAmountCents(request.getAmountCents());
            response.setFeeCents((long)(request.getAmountCents() * 0.029 + 30)); // 2.9% + 30¢
            return response;
        }
        
        StripePaymentResponse response = new StripePaymentResponse();
        response.setStatus("failed");
        response.setErrorMessage("Invalid payment request");
        return response;
    }
    
    public StripeRefundResponse refund(String chargeId, Long amountCents) {
        System.out.println("Stripe refund processed for charge: " + chargeId);
        StripeRefundResponse response = new StripeRefundResponse();
        response.setRefundId("REFUND_" + System.currentTimeMillis());
        response.setStatus("succeeded");
        response.setAmountCents(amountCents);
        return response;
    }
    
    public StripePaymentResponse retrieveCharge(String chargeId) {
        StripePaymentResponse response = new StripePaymentResponse();
        response.setId(chargeId);
        response.setStatus("succeeded");
        return response;
    }
    
    public boolean checkHealth() {
        return true; // Simulate health check
    }
}

// Stripe API classes
class StripeChargeRequest {
    private Long amountCents;
    private String currency;
    private String customerEmail;
    private String description;
    
    public StripeChargeRequest(Long amountCents, String currency, String customerEmail, String description) {
        this.amountCents = amountCents;
        this.currency = currency;
        this.customerEmail = customerEmail;
        this.description = description;
    }
    
    public boolean isValid() {
        return amountCents != null && amountCents > 0 && 
               customerEmail != null && customerEmail.contains("@");
    }
    
    // Getters
    public Long getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getCustomerEmail() { return customerEmail; }
    public String getDescription() { return description; }
}

class StripePaymentResponse {
    private String id;
    private String status;
    private Long amountCents;
    private Long feeCents;
    private String errorMessage;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
    public Long getFeeCents() { return feeCents; }
    public void setFeeCents(Long feeCents) { this.feeCents = feeCents; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

class StripeRefundResponse {
    private String refundId;
    private String status;
    private Long amountCents;
    
    // Getters and setters
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
}

// PayPal Adapter
class PayPalAdapter implements PaymentProcessor {
    private LegacyPayPalAPI paypalAPI;
    
    public PayPalAdapter(LegacyPayPalAPI paypalAPI) {
        this.paypalAPI = paypalAPI;
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            // Convert our request to PayPal format
            double amount = request.getAmount().doubleValue();
            String transactionId = paypalAPI.makePayment(amount, request.getCustomerEmail(), 
                                                        request.getDescription());
            
            if (transactionId != null) {
                PaymentResult result = new PaymentResult(true, transactionId);
                result.setActualAmount(request.getAmount());
                result.setFees(request.getAmount().multiply(new BigDecimal("0.034"))); // 3.4% + $0.30
                return result;
            } else {
                return new PaymentResult(false, "PayPal payment failed");
            }
        } catch (Exception e) {
            return new PaymentResult(false, "PayPal error: " + e.getMessage());
        }
    }
    
    @Override
    public boolean refundPayment(String transactionId, BigDecimal amount) {
        return paypalAPI.cancelTransaction(transactionId);
    }
    
    @Override
    public PaymentStatus getPaymentStatus(String transactionId) {
        String status = paypalAPI.getTransactionStatus(transactionId);
        return switch (status) {
            case "COMPLETED" -> PaymentStatus.COMPLETED;
            case "PENDING" -> PaymentStatus.PENDING;
            case "FAILED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
    
    @Override
    public boolean isAvailable() {
        return paypalAPI.isServiceOnline();
    }
    
    @Override
    public String getProviderName() {
        return "PayPal";
    }
}

// Stripe Adapter
class StripeAdapter implements PaymentProcessor {
    private ModernStripeAPI stripeAPI;
    
    public StripeAdapter(ModernStripeAPI stripeAPI) {
        this.stripeAPI = stripeAPI;
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            // Convert our request to Stripe format
            Long amountCents = request.getAmount().multiply(new BigDecimal("100")).longValue();
            StripeChargeRequest stripeRequest = new StripeChargeRequest(
                amountCents, 
                request.getCurrency(), 
                request.getCustomerEmail(), 
                request.getDescription()
            );
            
            StripePaymentResponse response = stripeAPI.charge(stripeRequest);
            
            if ("succeeded".equals(response.getStatus())) {
                PaymentResult result = new PaymentResult(true, response.getId());
                result.setActualAmount(new BigDecimal(response.getAmountCents()).divide(new BigDecimal("100")));
                result.setFees(new BigDecimal(response.getFeeCents()).divide(new BigDecimal("100")));
                return result;
            } else {
                return new PaymentResult(false, response.getErrorMessage());
            }
        } catch (Exception e) {
            return new PaymentResult(false, "Stripe error: " + e.getMessage());
        }
    }
    
    @Override
    public boolean refundPayment(String transactionId, BigDecimal amount) {
        try {
            Long amountCents = amount.multiply(new BigDecimal("100")).longValue();
            StripeRefundResponse response = stripeAPI.refund(transactionId, amountCents);
            return "succeeded".equals(response.getStatus());
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public PaymentStatus getPaymentStatus(String transactionId) {
        try {
            StripePaymentResponse response = stripeAPI.retrieveCharge(transactionId);
            return switch (response.getStatus()) {
                case "succeeded" -> PaymentStatus.COMPLETED;
                case "pending" -> PaymentStatus.PENDING;
                case "failed" -> PaymentStatus.FAILED;
                default -> PaymentStatus.PENDING;
            };
        } catch (Exception e) {
            return PaymentStatus.FAILED;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return stripeAPI.checkHealth();
    }
    
    @Override
    public String getProviderName() {
        return "Stripe";
    }
}

// Payment service that uses adapters
class UnifiedPaymentService {
    private Map<String, PaymentProcessor> processors;
    private String defaultProcessor;
    
    public UnifiedPaymentService() {
        this.processors = new HashMap<>();
        
        // Register payment processors through adapters
        processors.put("paypal", new PayPalAdapter(new LegacyPayPalAPI()));
        processors.put("stripe", new StripeAdapter(new ModernStripeAPI()));
        
        this.defaultProcessor = "stripe";
    }
    
    public PaymentResult processPayment(PaymentRequest request, String providerName) {
        PaymentProcessor processor = processors.get(providerName.toLowerCase());
        if (processor == null) {
            return new PaymentResult(false, "Unknown payment provider: " + providerName);
        }
        
        if (!processor.isAvailable()) {
            return new PaymentResult(false, processor.getProviderName() + " is currently unavailable");
        }
        
        return processor.processPayment(request);
    }
    
    public PaymentResult processPayment(PaymentRequest request) {
        return processPayment(request, defaultProcessor);
    }
    
    public PaymentResult processPaymentWithFallback(PaymentRequest request) {
        // Try default processor first
        PaymentResult result = processPayment(request, defaultProcessor);
        
        // If failed, try other available processors
        if (!result.isSuccess()) {
            for (String providerName : processors.keySet()) {
                if (!providerName.equals(defaultProcessor)) {
                    PaymentProcessor processor = processors.get(providerName);
                    if (processor.isAvailable()) {
                        System.out.println("Falling back to " + processor.getProviderName());
                        result = processor.processPayment(request);
                        if (result.isSuccess()) {
                            break;
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    public void addPaymentProcessor(String name, PaymentProcessor processor) {
        processors.put(name.toLowerCase(), processor);
    }
    
    public void setDefaultProcessor(String processorName) {
        if (processors.containsKey(processorName.toLowerCase())) {
            this.defaultProcessor = processorName.toLowerCase();
        }
    }
    
    public String[] getAvailableProcessors() {
        return processors.keySet().toArray(new String[0]);
    }
}
```

---

## Decorator Pattern

### Problem and Solution

**Problem**: You need to add behavior to objects dynamically without altering their structure or creating numerous subclasses.

**Solution**: Create wrapper classes that add new functionality while maintaining the same interface as the original object.

### Basic Decorator Implementation

```java
// CoffeeDecorator.java - Basic Decorator Pattern implementation
package com.javajourney.patterns.decorator;

import java.math.BigDecimal;

/**
 * Coffee Shop Decorator Pattern Implementation
 * Demonstrates dynamic behavior enhancement through composition
 */

// Component interface
interface Coffee {
    String getDescription();
    BigDecimal getCost();
    int getCalories();
    String getSize();
}

// Concrete component - base coffee
class SimpleCoffee implements Coffee {
    private String size;
    
    public SimpleCoffee(String size) {
        this.size = size;
    }
    
    @Override
    public String getDescription() {
        return size + " Simple Coffee";
    }
    
    @Override
    public BigDecimal getCost() {
        return switch (size.toLowerCase()) {
            case "small" -> new BigDecimal("2.00");
            case "medium" -> new BigDecimal("2.50");
            case "large" -> new BigDecimal("3.00");
            default -> new BigDecimal("2.50");
        };
    }
    
    @Override
    public int getCalories() {
        return switch (size.toLowerCase()) {
            case "small" -> 5;
            case "medium" -> 8;
            case "large" -> 12;
            default -> 8;
        };
    }
    
    @Override
    public String getSize() {
        return size;
    }
}

// Base decorator class
abstract class CoffeeDecorator implements Coffee {
    protected Coffee coffee;
    
    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription();
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost();
    }
    
    @Override
    public int getCalories() {
        return coffee.getCalories();
    }
    
    @Override
    public String getSize() {
        return coffee.getSize();
    }
}

// Concrete decorators
class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Milk";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("0.50"));
    }
    
    @Override
    public int getCalories() {
        return coffee.getCalories() + 20;
    }
}

class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Sugar";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("0.25"));
    }
    
    @Override
    public int getCalories() {
        return coffee.getCalories() + 16;
    }
}

class WhipCreamDecorator extends CoffeeDecorator {
    public WhipCreamDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Whip Cream";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("0.75"));
    }
    
    @Override
    public int getCalories() {
        return coffee.getCalories() + 50;
    }
}

class VanillaSyrupDecorator extends CoffeeDecorator {
    public VanillaSyrupDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Vanilla Syrup";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("0.60"));
    }
    
    @Override
    public int getCalories() {
        return coffee.getCalories() + 30;
    }
}

class CaramelSyrupDecorator extends CoffeeDecorator {
    public CaramelSyrupDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Caramel Syrup";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("0.60"));
    }
    
    @Override
    public int getCalories() {
        return coffee.getCalories() + 35;
    }
}

class ExtraShotDecorator extends CoffeeDecorator {
    public ExtraShotDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Extra Shot";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("1.00"));
    }
    
    @Override
    public int getCalories() {
        return coffee.getCalories() + 2;
    }
}

// Coffee shop class to demonstrate usage
class CoffeeShop {
    public Coffee createCustomCoffee(String size, String... addOns) {
        Coffee coffee = new SimpleCoffee(size);
        
        for (String addOn : addOns) {
            coffee = switch (addOn.toLowerCase()) {
                case "milk" -> new MilkDecorator(coffee);
                case "sugar" -> new SugarDecorator(coffee);
                case "whip", "whipcream" -> new WhipCreamDecorator(coffee);
                case "vanilla" -> new VanillaSyrupDecorator(coffee);
                case "caramel" -> new CaramelSyrupDecorator(coffee);
                case "extrashot", "shot" -> new ExtraShotDecorator(coffee);
                default -> coffee; // Ignore unknown add-ons
            };
        }
        
        return coffee;
    }
    
    public void printReceipt(Coffee coffee) {
        System.out.println("=== Coffee Shop Receipt ===");
        System.out.println("Order: " + coffee.getDescription());
        System.out.println("Cost: $" + coffee.getCost());
        System.out.println("Calories: " + coffee.getCalories());
        System.out.println("Size: " + coffee.getSize());
        System.out.println("==========================");
    }
}
```

### Advanced Decorator: Data Processing Pipeline

```java
// DataProcessingDecorator.java - Advanced Decorator for data processing
package com.javajourney.patterns.decorator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data Processing Pipeline Decorator Implementation
 * Demonstrates building flexible data processing pipelines using decorator pattern
 */

// Component interface for data processing
interface DataProcessor<T> {
    List<T> process(List<T> data);
    String getProcessingInfo();
    long getProcessingTime();
}

// Base data processor
class BasicDataProcessor<T> implements DataProcessor<T> {
    private long processingTime;
    
    @Override
    public List<T> process(List<T> data) {
        long startTime = System.nanoTime();
        
        // Basic processing - just return a copy of the data
        List<T> result = new ArrayList<>(data);
        
        processingTime = System.nanoTime() - startTime;
        return result;
    }
    
    @Override
    public String getProcessingInfo() {
        return "Basic Data Processing";
    }
    
    @Override
    public long getProcessingTime() {
        return processingTime;
    }
}

// Base decorator
abstract class DataProcessorDecorator<T> implements DataProcessor<T> {
    protected DataProcessor<T> processor;
    protected long decoratorProcessingTime;
    
    public DataProcessorDecorator(DataProcessor<T> processor) {
        this.processor = processor;
    }
    
    @Override
    public List<T> process(List<T> data) {
        return processor.process(data);
    }
    
    @Override
    public String getProcessingInfo() {
        return processor.getProcessingInfo();
    }
    
    @Override
    public long getProcessingTime() {
        return processor.getProcessingTime() + decoratorProcessingTime;
    }
}

// Validation decorator
class ValidationDecorator<T> extends DataProcessorDecorator<T> {
    private Function<T, Boolean> validator;
    private String validationName;
    
    public ValidationDecorator(DataProcessor<T> processor, Function<T, Boolean> validator, String validationName) {
        super(processor);
        this.validator = validator;
        this.validationName = validationName;
    }
    
    @Override
    public List<T> process(List<T> data) {
        long startTime = System.nanoTime();
        
        // First process with wrapped processor
        List<T> processedData = processor.process(data);
        
        // Then apply validation
        List<T> validData = processedData.stream()
            .filter(validator::apply)
            .collect(Collectors.toList());
        
        decoratorProcessingTime = System.nanoTime() - startTime - processor.getProcessingTime();
        
        int filteredCount = processedData.size() - validData.size();
        if (filteredCount > 0) {
            System.out.println("Validation (" + validationName + ") filtered out " + filteredCount + " items");
        }
        
        return validData;
    }
    
    @Override
    public String getProcessingInfo() {
        return processor.getProcessingInfo() + " → Validation(" + validationName + ")";
    }
}

// Transformation decorator
class TransformationDecorator<T> extends DataProcessorDecorator<T> {
    private Function<T, T> transformer;
    private String transformationName;
    
    public TransformationDecorator(DataProcessor<T> processor, Function<T, T> transformer, String transformationName) {
        super(processor);
        this.transformer = transformer;
        this.transformationName = transformationName;
    }
    
    @Override
    public List<T> process(List<T> data) {
        long startTime = System.nanoTime();
        
        // First process with wrapped processor
        List<T> processedData = processor.process(data);
        
        // Then apply transformation
        List<T> transformedData = processedData.stream()
            .map(transformer)
            .collect(Collectors.toList());
        
        decoratorProcessingTime = System.nanoTime() - startTime - processor.getProcessingTime();
        
        return transformedData;
    }
    
    @Override
    public String getProcessingInfo() {
        return processor.getProcessingInfo() + " → Transform(" + transformationName + ")";
    }
}

// Sorting decorator
class SortingDecorator<T> extends DataProcessorDecorator<T> {
    private Comparator<T> comparator;
    private String sortingName;
    
    public SortingDecorator(DataProcessor<T> processor, Comparator<T> comparator, String sortingName) {
        super(processor);
        this.comparator = comparator;
        this.sortingName = sortingName;
    }
    
    @Override
    public List<T> process(List<T> data) {
        long startTime = System.nanoTime();
        
        // First process with wrapped processor
        List<T> processedData = processor.process(data);
        
        // Then apply sorting
        List<T> sortedData = processedData.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
        
        decoratorProcessingTime = System.nanoTime() - startTime - processor.getProcessingTime();
        
        return sortedData;
    }
    
    @Override
    public String getProcessingInfo() {
        return processor.getProcessingInfo() + " → Sort(" + sortingName + ")";
    }
}

// Caching decorator
class CachingDecorator<T> extends DataProcessorDecorator<T> {
    private Map<Integer, List<T>> cache;
    private int maxCacheSize;
    
    public CachingDecorator(DataProcessor<T> processor, int maxCacheSize) {
        super(processor);
        this.cache = new LinkedHashMap<Integer, List<T>>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, List<T>> eldest) {
                return size() > maxCacheSize;
            }
        };
        this.maxCacheSize = maxCacheSize;
    }
    
    @Override
    public List<T> process(List<T> data) {
        long startTime = System.nanoTime();
        
        int dataHash = data.hashCode();
        
        // Check cache first
        List<T> cachedResult = cache.get(dataHash);
        if (cachedResult != null) {
            decoratorProcessingTime = System.nanoTime() - startTime;
            System.out.println("Cache hit! Returning cached result.");
            return new ArrayList<>(cachedResult);
        }
        
        // Process with wrapped processor
        List<T> result = processor.process(data);
        
        // Cache the result
        cache.put(dataHash, new ArrayList<>(result));
        
        decoratorProcessingTime = System.nanoTime() - startTime - processor.getProcessingTime();
        
        return result;
    }
    
    @Override
    public String getProcessingInfo() {
        return processor.getProcessingInfo() + " → Cache(size:" + cache.size() + "/" + maxCacheSize + ")";
    }
    
    public void clearCache() {
        cache.clear();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
}

// Logging decorator
class LoggingDecorator<T> extends DataProcessorDecorator<T> {
    private String logPrefix;
    
    public LoggingDecorator(DataProcessor<T> processor, String logPrefix) {
        super(processor);
        this.logPrefix = logPrefix;
    }
    
    @Override
    public List<T> process(List<T> data) {
        long startTime = System.nanoTime();
        
        System.out.println(logPrefix + " - Processing started with " + data.size() + " items");
        
        List<T> result = processor.process(data);
        
        decoratorProcessingTime = System.nanoTime() - startTime - processor.getProcessingTime();
        
        System.out.println(logPrefix + " - Processing completed with " + result.size() + " items");
        System.out.println(logPrefix + " - Total processing time: " + (getProcessingTime() / 1_000_000.0) + " ms");
        
        return result;
    }
    
    @Override
    public String getProcessingInfo() {
        return processor.getProcessingInfo() + " → Log(" + logPrefix + ")";
    }
}

// Performance monitoring decorator
class PerformanceMonitoringDecorator<T> extends DataProcessorDecorator<T> {
    private List<Long> processingTimes;
    private List<Integer> dataSizes;
    
    public PerformanceMonitoringDecorator(DataProcessor<T> processor) {
        super(processor);
        this.processingTimes = new ArrayList<>();
        this.dataSizes = new ArrayList<>();
    }
    
    @Override
    public List<T> process(List<T> data) {
        long startTime = System.nanoTime();
        
        List<T> result = processor.process(data);
        
        long totalTime = System.nanoTime() - startTime;
        decoratorProcessingTime = totalTime - processor.getProcessingTime();
        
        // Record metrics
        processingTimes.add(totalTime);
        dataSizes.add(data.size());
        
        return result;
    }
    
    @Override
    public String getProcessingInfo() {
        return processor.getProcessingInfo() + " → PerfMonitor(runs:" + processingTimes.size() + ")";
    }
    
    public void printStatistics() {
        if (processingTimes.isEmpty()) {
            System.out.println("No processing statistics available");
            return;
        }
        
        double avgTime = processingTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        long minTime = processingTimes.stream().mapToLong(Long::longValue).min().orElse(0L) / 1_000_000L;
        long maxTime = processingTimes.stream().mapToLong(Long::longValue).max().orElse(0L) / 1_000_000L;
        
        int avgDataSize = (int) dataSizes.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int minDataSize = dataSizes.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxDataSize = dataSizes.stream().mapToInt(Integer::intValue).max().orElse(0);
        
        System.out.println("=== Performance Statistics ===");
        System.out.println("Total runs: " + processingTimes.size());
        System.out.println("Processing time - Avg: " + String.format("%.2f", avgTime) + "ms, " +
                          "Min: " + minTime + "ms, Max: " + maxTime + "ms");
        System.out.println("Data size - Avg: " + avgDataSize + ", Min: " + minDataSize + ", Max: " + maxDataSize);
        System.out.println("==============================");
    }
    
    public void resetStatistics() {
        processingTimes.clear();
        dataSizes.clear();
    }
}

// Pipeline builder for easy configuration
class DataProcessingPipelineBuilder<T> {
    private DataProcessor<T> processor;
    
    public DataProcessingPipelineBuilder() {
        this.processor = new BasicDataProcessor<>();
    }
    
    public DataProcessingPipelineBuilder<T> validate(Function<T, Boolean> validator, String name) {
        processor = new ValidationDecorator<>(processor, validator, name);
        return this;
    }
    
    public DataProcessingPipelineBuilder<T> transform(Function<T, T> transformer, String name) {
        processor = new TransformationDecorator<>(processor, transformer, name);
        return this;
    }
    
    public DataProcessingPipelineBuilder<T> sort(Comparator<T> comparator, String name) {
        processor = new SortingDecorator<>(processor, comparator, name);
        return this;
    }
    
    public DataProcessingPipelineBuilder<T> cache(int maxSize) {
        processor = new CachingDecorator<>(processor, maxSize);
        return this;
    }
    
    public DataProcessingPipelineBuilder<T> log(String prefix) {
        processor = new LoggingDecorator<>(processor, prefix);
        return this;
    }
    
    public DataProcessingPipelineBuilder<T> monitor() {
        processor = new PerformanceMonitoringDecorator<>(processor);
        return this;
    }
    
    public DataProcessor<T> build() {
        return processor;
    }
}
```

---

## Hands-On Practice

### Practice 1: File Processing Decorators

```java
// FileProcessingDecorator.java - Practice with file processing decorators
package com.javajourney.patterns.practice;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * File Processing Decorator Practice
 * Build flexible file processing pipelines using decorator pattern
 */

// Component interface
interface FileProcessor {
    void processFile(String inputPath, String outputPath) throws IOException;
    String getProcessingDescription();
    Map<String, Object> getStatistics();
}

// Base file processor
class BasicFileProcessor implements FileProcessor {
    private Map<String, Object> statistics;
    
    public BasicFileProcessor() {
        this.statistics = new HashMap<>();
    }
    
    @Override
    public void processFile(String inputPath, String outputPath) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // Basic file copy
        Files.copy(Paths.get(inputPath), Paths.get(outputPath), StandardCopyOption.REPLACE_EXISTING);
        
        long processingTime = System.currentTimeMillis() - startTime;
        statistics.put("processingTime", processingTime);
        statistics.put("inputSize", Files.size(Paths.get(inputPath)));
        statistics.put("outputSize", Files.size(Paths.get(outputPath)));
    }
    
    @Override
    public String getProcessingDescription() {
        return "Basic File Copy";
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        return new HashMap<>(statistics);
    }
}

// Base decorator
abstract class FileProcessorDecorator implements FileProcessor {
    protected FileProcessor processor;
    protected Map<String, Object> decoratorStats;
    
    public FileProcessorDecorator(FileProcessor processor) {
        this.processor = processor;
        this.decoratorStats = new HashMap<>();
    }
    
    @Override
    public String getProcessingDescription() {
        return processor.getProcessingDescription();
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> allStats = new HashMap<>(processor.getStatistics());
        allStats.putAll(decoratorStats);
        return allStats;
    }
}

// Compression decorator
class CompressionDecorator extends FileProcessorDecorator {
    
    public CompressionDecorator(FileProcessor processor) {
        super(processor);
    }
    
    @Override
    public void processFile(String inputPath, String outputPath) throws IOException {
        // Create temporary file for intermediate processing
        String tempPath = outputPath + ".temp";
        
        // Process with wrapped processor first
        processor.processFile(inputPath, tempPath);
        
        // Then compress
        long startTime = System.currentTimeMillis();
        compressFile(tempPath, outputPath);
        long compressionTime = System.currentTimeMillis() - startTime;
        
        // Cleanup
        Files.deleteIfExists(Paths.get(tempPath));
        
        // Update statistics
        decoratorStats.put("compressionTime", compressionTime);
        decoratorStats.put("originalSize", Files.size(Paths.get(inputPath)));
        decoratorStats.put("compressedSize", Files.size(Paths.get(outputPath)));
        
        long originalSize = (Long) decoratorStats.get("originalSize");
        long compressedSize = (Long) decoratorStats.get("compressedSize");
        double compressionRatio = (double) compressedSize / originalSize;
        decoratorStats.put("compressionRatio", compressionRatio);
    }
    
    private void compressFile(String inputPath, String outputPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputPath);
             FileOutputStream fos = new FileOutputStream(outputPath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
        }
    }
    
    @Override
    public String getProcessingDescription() {
        return processor.getProcessingDescription() + " → GZIP Compression";
    }
}

// Encryption decorator (simplified)
class EncryptionDecorator extends FileProcessorDecorator {
    private String encryptionKey;
    
    public EncryptionDecorator(FileProcessor processor, String encryptionKey) {
        super(processor);
        this.encryptionKey = encryptionKey;
    }
    
    @Override
    public void processFile(String inputPath, String outputPath) throws IOException {
        String tempPath = outputPath + ".temp";
        
        processor.processFile(inputPath, tempPath);
        
        long startTime = System.currentTimeMillis();
        encryptFile(tempPath, outputPath);
        long encryptionTime = System.currentTimeMillis() - startTime;
        
        Files.deleteIfExists(Paths.get(tempPath));
        
        decoratorStats.put("encryptionTime", encryptionTime);
        decoratorStats.put("encryptionKey", encryptionKey.hashCode()); // Don't store actual key
    }
    
    private void encryptFile(String inputPath, String outputPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputPath);
             FileOutputStream fos = new FileOutputStream(outputPath)) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                // Simple XOR encryption (for demo purposes only)
                for (int i = 0; i < len; i++) {
                    buffer[i] ^= encryptionKey.hashCode() & 0xFF;
                }
                fos.write(buffer, 0, len);
            }
        }
    }
    
    @Override
    public String getProcessingDescription() {
        return processor.getProcessingDescription() + " → XOR Encryption";
    }
}

// Validation decorator
class FileValidationDecorator extends FileProcessorDecorator {
    
    public FileValidationDecorator(FileProcessor processor) {
        super(processor);
    }
    
    @Override
    public void processFile(String inputPath, String outputPath) throws IOException {
        // Validate input file
        validateInputFile(inputPath);
        
        processor.processFile(inputPath, outputPath);
        
        // Validate output file
        validateOutputFile(outputPath);
        
        decoratorStats.put("inputValidation", "passed");
        decoratorStats.put("outputValidation", "passed");
    }
    
    private void validateInputFile(String inputPath) throws IOException {
        Path path = Paths.get(inputPath);
        
        if (!Files.exists(path)) {
            throw new IOException("Input file does not exist: " + inputPath);
        }
        
        if (!Files.isReadable(path)) {
            throw new IOException("Input file is not readable: " + inputPath);
        }
        
        if (Files.size(path) == 0) {
            throw new IOException("Input file is empty: " + inputPath);
        }
        
        if (Files.size(path) > 100 * 1024 * 1024) { // 100MB limit
            throw new IOException("Input file too large: " + inputPath);
        }
    }
    
    private void validateOutputFile(String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        
        if (!Files.exists(path)) {
            throw new IOException("Output file was not created: " + outputPath);
        }
        
        if (Files.size(path) == 0) {
            throw new IOException("Output file is empty: " + outputPath);
        }
    }
    
    @Override
    public String getProcessingDescription() {
        return processor.getProcessingDescription() + " → File Validation";
    }
}

// Logging decorator
class FileLoggingDecorator extends FileProcessorDecorator {
    private String loggerName;
    
    public FileLoggingDecorator(FileProcessor processor, String loggerName) {
        super(processor);
        this.loggerName = loggerName;
    }
    
    @Override
    public void processFile(String inputPath, String outputPath) throws IOException {
        System.out.println("[" + loggerName + "] Starting file processing: " + inputPath + " → " + outputPath);
        
        long startTime = System.currentTimeMillis();
        
        try {
            processor.processFile(inputPath, outputPath);
            
            long processingTime = System.currentTimeMillis() - startTime;
            System.out.println("[" + loggerName + "] File processing completed successfully in " + processingTime + "ms");
            
            decoratorStats.put("loggedProcessingTime", processingTime);
            
        } catch (IOException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            System.err.println("[" + loggerName + "] File processing failed after " + processingTime + "ms: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public String getProcessingDescription() {
        return processor.getProcessingDescription() + " → Logging(" + loggerName + ")";
    }
}

// Builder for file processing pipeline
class FileProcessingPipelineBuilder {
    private FileProcessor processor;
    
    public FileProcessingPipelineBuilder() {
        this.processor = new BasicFileProcessor();
    }
    
    public FileProcessingPipelineBuilder compress() {
        processor = new CompressionDecorator(processor);
        return this;
    }
    
    public FileProcessingPipelineBuilder encrypt(String key) {
        processor = new EncryptionDecorator(processor, key);
        return this;
    }
    
    public FileProcessingPipelineBuilder validate() {
        processor = new FileValidationDecorator(processor);
        return this;
    }
    
    public FileProcessingPipelineBuilder log(String loggerName) {
        processor = new FileLoggingDecorator(processor, loggerName);
        return this;
    }
    
    public FileProcessor build() {
        return processor;
    }
}
```

### Practice 2: Comprehensive Demo Application

```java
// AdapterDecoratorDemo.java - Comprehensive demonstration
package com.javajourney.patterns;

import java.math.BigDecimal;
import java.util.*;

/**
 * Comprehensive Demonstration of Adapter and Decorator Patterns
 */
public class AdapterDecoratorDemo {
    
    public static void main(String[] args) {
        demonstrateMediaPlayerAdapter();
        System.out.println();
        
        demonstratePaymentGatewayAdapter();
        System.out.println();
        
        demonstrateCoffeeDecorator();
        System.out.println();
        
        demonstrateDataProcessingDecorator();
        System.out.println();
        
        demonstrateFileProcessingDecorator();
    }
    
    private static void demonstrateMediaPlayerAdapter() {
        System.out.println("=== Media Player Adapter Demo ===");
        
        UniversalMediaPlayer player = new UniversalMediaPlayer();
        
        System.out.println("Supported formats: " + Arrays.toString(player.getSupportedFormats()));
        
        // Play different media types
        System.out.println("\nPlaying different media files:");
        player.play("song.mp3");
        player.pause();
        
        player.play("movie.mp4");
        player.pause();
        
        player.play("video.avi");
        player.stop();
        
        // Try unsupported format
        player.play("document.pdf");
    }
    
    private static void demonstratePaymentGatewayAdapter() {
        System.out.println("=== Payment Gateway Adapter Demo ===");
        
        UnifiedPaymentService paymentService = new UnifiedPaymentService();
        
        // Test payment with different providers
        PaymentRequest request = new PaymentRequest(
            new BigDecimal("99.99"), 
            "USD", 
            "customer@example.com", 
            "Product Purchase"
        );
        
        System.out.println("Available processors: " + Arrays.toString(paymentService.getAvailableProcessors()));
        
        // Process with Stripe
        System.out.println("\nProcessing with Stripe:");
        PaymentResult stripeResult = paymentService.processPayment(request, "stripe");
        printPaymentResult(stripeResult);
        
        // Process with PayPal
        System.out.println("\nProcessing with PayPal:");
        PaymentResult paypalResult = paymentService.processPayment(request, "paypal");
        printPaymentResult(paypalResult);
        
        // Test fallback functionality
        System.out.println("\nTesting fallback functionality:");
        PaymentResult fallbackResult = paymentService.processPaymentWithFallback(request);
        printPaymentResult(fallbackResult);
    }
    
    private static void demonstrateCoffeeDecorator() {
        System.out.println("=== Coffee Decorator Demo ===");
        
        CoffeeShop shop = new CoffeeShop();
        
        // Create different coffee combinations
        Coffee simpleCoffee = shop.createCustomCoffee("medium");
        shop.printReceipt(simpleCoffee);
        
        Coffee fancyCoffee = shop.createCustomCoffee("large", "milk", "sugar", "vanilla", "whip");
        shop.printReceipt(fancyCoffee);
        
        Coffee espressoShot = shop.createCustomCoffee("small", "extrashot", "extrashot");
        shop.printReceipt(espressoShot);
        
        Coffee caramelMacchiato = shop.createCustomCoffee("medium", "milk", "caramel", "extrashot");
        shop.printReceipt(caramelMacchiato);
    }
    
    private static void demonstrateDataProcessingDecorator() {
        System.out.println("=== Data Processing Decorator Demo ===");
        
        // Create sample data
        List<String> data = Arrays.asList(
            "apple", "banana", "cherry", "", "date", "elderberry", 
            "fig", null, "grape", "honeydew", "a", "bb"
        );
        
        System.out.println("Original data: " + data);
        
        // Build processing pipeline
        DataProcessor<String> pipeline = new DataProcessingPipelineBuilder<String>()
            .log("DataProcessor")
            .validate(Objects::nonNull, "NotNull")
            .validate(s -> !s.isEmpty(), "NotEmpty")
            .validate(s -> s.length() > 2, "MinLength")
            .transform(String::toLowerCase, "Lowercase")
            .transform(s -> s.substring(0, 1).toUpperCase() + s.substring(1), "Capitalize")
            .sort(String::compareTo, "Alphabetical")
            .cache(10)
            .monitor()
            .build();
        
        System.out.println("\nPipeline: " + pipeline.getProcessingInfo());
        
        // Process data multiple times to test caching
        for (int i = 1; i <= 3; i++) {
            System.out.println("\n--- Run " + i + " ---");
            List<String> result = pipeline.process(data);
            System.out.println("Result: " + result);
        }
        
        // Print performance statistics
        if (pipeline instanceof PerformanceMonitoringDecorator) {
            ((PerformanceMonitoringDecorator<String>) pipeline).printStatistics();
        }
    }
    
    private static void demonstrateFileProcessingDecorator() {
        System.out.println("=== File Processing Decorator Demo ===");
        
        try {
            // Create sample input file
            String inputFile = "sample.txt";
            String outputFile = "processed_sample.txt.gz";
            
            Files.write(Paths.get(inputFile), 
                       "This is a sample file for demonstrating file processing decorators.\n".repeat(100).getBytes());
            
            // Build file processing pipeline
            FileProcessor pipeline = new FileProcessingPipelineBuilder()
                .log("FileProcessor")
                .validate()
                .compress()
                .build();
            
            System.out.println("Pipeline: " + pipeline.getProcessingDescription());
            
            // Process file
            pipeline.processFile(inputFile, outputFile);
            
            // Print statistics
            Map<String, Object> stats = pipeline.getStatistics();
            System.out.println("\nProcessing Statistics:");
            stats.forEach((key, value) -> System.out.println(key + ": " + value));
            
            // Cleanup
            Files.deleteIfExists(Paths.get(inputFile));
            Files.deleteIfExists(Paths.get(outputFile));
            
        } catch (Exception e) {
            System.err.println("File processing demo failed: " + e.getMessage());
        }
    }
    
    private static void printPaymentResult(PaymentResult result) {
        System.out.println("Success: " + result.isSuccess());
        if (result.isSuccess()) {
            System.out.println("Transaction ID: " + result.getTransactionId());
            System.out.println("Amount: $" + result.getActualAmount());
            System.out.println("Fees: $" + result.getFees());
        } else {
            System.out.println("Error: " + result.getErrorMessage());
        }
        System.out.println("Timestamp: " + result.getTimestamp());
    }
}
```

---

## Challenges

### Challenge 1: Database Connection Adapter
Create adapters for different database drivers (MySQL, PostgreSQL, MongoDB) that provide a unified interface for CRUD operations.

### Challenge 2: Notification Decorator Chain
Build a decorator system for notifications that can add features like retry logic, rate limiting, formatting, and delivery confirmation.

### Challenge 3: Web Service Response Decorator
Design decorators for web service responses that can add caching, compression, encryption, and transformation capabilities.

---

## Summary

### Key Takeaways

1. **Adapter Pattern Benefits**:
   - Enables integration with incompatible interfaces
   - Allows reuse of existing legacy code
   - Provides clean separation between client and implementation
   - Facilitates third-party library integration

2. **Decorator Pattern Benefits**:
   - Adds behavior dynamically without modifying original classes
   - Follows open/closed principle (open for extension, closed for modification)
   - Enables flexible composition of features
   - Avoids explosion of subclasses

3. **Implementation Best Practices**:
   - Use composition over inheritance in decorators
   - Ensure decorators maintain the same interface as components
   - Consider performance implications of decorator chains
   - Implement proper error handling and resource cleanup

4. **Real-World Applications**:
   - Payment gateway integration (Adapter)
   - Data processing pipelines (Decorator)
   - File I/O operations with multiple transformations (Decorator)
   - Legacy system modernization (Adapter)

### Performance Checklist
- [ ] Adapter objects properly delegate method calls
- [ ] Decorator chains don't create excessive object overhead
- [ ] Resource cleanup is handled properly in decorators
- [ ] Error propagation works correctly through decorator chains
- [ ] Performance monitoring is in place for complex pipelines

### Quality Gates Checklist
- [ ] All adapters maintain interface contracts
- [ ] Decorators preserve component behavior while adding features
- [ ] Error handling is comprehensive and consistent
- [ ] Memory leaks are prevented in decorator chains
- [ ] Thread safety is maintained where required

### Tomorrow's Preview
Tomorrow we'll explore **Structural Patterns - Facade & Composite** where we'll learn to simplify complex subsystems and work with tree-like object structures.

---

*Day 52 Complete! You've mastered Adapter and Decorator patterns for creating flexible, extensible systems. You can now integrate incompatible interfaces and add dynamic behavior to objects without modifying their structure. Tomorrow we'll continue with more structural patterns that help organize complex systems.*