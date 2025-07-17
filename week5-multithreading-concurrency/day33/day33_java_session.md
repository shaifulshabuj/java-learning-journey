# Day 33: CompletableFuture & Async Programming (August 11, 2025)

**Date:** August 11, 2025  
**Duration:** 3 hours  
**Focus:** Master modern asynchronous programming with CompletableFuture and reactive patterns

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Understand asynchronous programming concepts and benefits
- ✅ Master CompletableFuture creation and basic operations
- ✅ Learn composition and chaining of asynchronous operations
- ✅ Implement error handling and exception management
- ✅ Build reactive programming patterns with async pipelines
- ✅ Create production-ready asynchronous applications

---

## 🚀 Part 1: CompletableFuture Fundamentals (60 minutes)

### **Understanding Asynchronous Programming**

**Asynchronous Programming** allows non-blocking execution:

- **Blocking**: Thread waits for operation to complete
- **Non-blocking**: Thread continues while operation executes elsewhere
- **Callback Hell**: Nested callbacks become hard to manage
- **Future Composition**: Chain operations elegantly

### **CompletableFuture Basics**

Create `CompletableFutureBasics.java`:

```java
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.Random;

/**
 * Comprehensive demonstration of CompletableFuture fundamentals
 */
public class CompletableFutureBasics {
    
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println("=== CompletableFuture Fundamentals ===\n");
        
        // 1. Creating CompletableFutures
        System.out.println("1. Creating CompletableFutures:");
        demonstrateCreation();
        
        // 2. Basic completion and retrieval
        System.out.println("\n2. Basic Completion and Retrieval:");
        demonstrateCompletion();
        
        // 3. Asynchronous execution
        System.out.println("\n3. Asynchronous Execution:");
        demonstrateAsyncExecution();
        
        // 4. Combining results
        System.out.println("\n4. Combining Results:");
        demonstrateCombining();
        
        System.out.println("\n=== CompletableFuture Fundamentals Completed ===");
    }
    
    private static void demonstrateCreation() {
        // 1. Already completed CompletableFuture
        CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("Hello World");
        System.out.println("Completed future result: " + completedFuture.join());
        
        // 2. Manual completion
        CompletableFuture<Integer> manualFuture = new CompletableFuture<>();
        
        // Complete it in another thread
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                manualFuture.complete(42);
            } catch (InterruptedException e) {
                manualFuture.completeExceptionally(e);
            }
        });
        
        System.out.println("Manual future result: " + manualFuture.join());
        
        // 3. Using supplyAsync
        CompletableFuture<String> asyncSupplier = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "Async result from " + Thread.currentThread().getName();
        });
        
        System.out.println("Supply async result: " + asyncSupplier.join());
        
        // 4. Using runAsync (no return value)
        CompletableFuture<Void> asyncRunner = CompletableFuture.runAsync(() -> {
            sleep(300);
            System.out.println("Async task completed on " + Thread.currentThread().getName());
        });
        
        asyncRunner.join(); // Wait for completion
    }
    
    private static void demonstrateCompletion() {
        // Test different completion methods
        CompletableFuture<String> future1 = new CompletableFuture<>();
        CompletableFuture<String> future2 = new CompletableFuture<>();
        CompletableFuture<String> future3 = new CompletableFuture<>();
        
        // Normal completion
        future1.complete("Normal completion");
        
        // Exceptional completion
        future2.completeExceptionally(new RuntimeException("Something went wrong"));
        
        // Check completion status
        System.out.println("Future1 completed: " + future1.isDone());
        System.out.println("Future1 completed exceptionally: " + future1.isCompletedExceptionally());
        System.out.println("Future1 cancelled: " + future1.isCancelled());
        
        System.out.println("Future2 completed: " + future2.isDone());
        System.out.println("Future2 completed exceptionally: " + future2.isCompletedExceptionally());
        
        // Get results
        try {
            System.out.println("Future1 result: " + future1.get());
        } catch (Exception e) {
            System.out.println("Future1 error: " + e.getMessage());
        }
        
        try {
            System.out.println("Future2 result: " + future2.get());
        } catch (Exception e) {
            System.out.println("Future2 error: " + e.getCause().getMessage());
        }
        
        // Timeout demonstration
        CompletableFuture<String> slowFuture = CompletableFuture.supplyAsync(() -> {
            sleep(3000);
            return "Slow result";
        });
        
        try {
            String result = slowFuture.get(1, TimeUnit.SECONDS);
            System.out.println("Slow future result: " + result);
        } catch (TimeoutException e) {
            System.out.println("Slow future timed out");
            slowFuture.cancel(true);
        } catch (Exception e) {
            System.out.println("Slow future error: " + e.getMessage());
        }
    }
    
    private static void demonstrateAsyncExecution() {
        // Custom executor
        ExecutorService customExecutor = Executors.newFixedThreadPool(3);
        
        try {
            // Using default ForkJoinPool
            CompletableFuture<String> defaultPool = CompletableFuture.supplyAsync(() -> {
                System.out.println("Default pool: " + Thread.currentThread().getName());
                return "Default result";
            });
            
            // Using custom executor
            CompletableFuture<String> customPool = CompletableFuture.supplyAsync(() -> {
                System.out.println("Custom pool: " + Thread.currentThread().getName());
                return "Custom result";
            }, customExecutor);
            
            // Chain operations with different executors
            CompletableFuture<String> chainedResult = defaultPool
                .thenApplyAsync(result -> {
                    System.out.println("Chain step 1: " + Thread.currentThread().getName());
                    return result + " -> processed";
                }, customExecutor)
                .thenApplyAsync(result -> {
                    System.out.println("Chain step 2: " + Thread.currentThread().getName());
                    return result + " -> finalized";
                });
            
            // Wait for all to complete
            CompletableFuture.allOf(defaultPool, customPool, chainedResult).join();
            
            System.out.println("Default result: " + defaultPool.join());
            System.out.println("Custom result: " + customPool.join());
            System.out.println("Chained result: " + chainedResult.join());
            
        } finally {
            customExecutor.shutdown();
        }
    }
    
    private static void demonstrateCombining() {
        // Create multiple async operations
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            System.out.println("Future1 completed");
            return 10;
        });
        
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            System.out.println("Future2 completed");
            return 20;
        });
        
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            sleep(800);
            System.out.println("Future3 completed");
            return "Hello";
        });
        
        // Combine two futures
        CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> {
            System.out.println("Combining: " + a + " + " + b);
            return a + b;
        });
        
        // Combine with a different type
        CompletableFuture<String> mixedCombine = future1.thenCombine(future3, (num, str) -> {
            System.out.println("Mixed combining: " + num + " with " + str);
            return str + " " + num;
        });
        
        // Wait for either of two futures
        CompletableFuture<Object> either = future1.applyToEither(future2, result -> {
            System.out.println("First completed with: " + result);
            return "Either result: " + result;
        });
        
        // Wait for all futures
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            future1, future2, future3, combined, mixedCombine, either
        );
        
        allFutures.join();
        
        System.out.println("Combined result: " + combined.join());
        System.out.println("Mixed combine result: " + mixedCombine.join());
        System.out.println("Either result: " + either.join());
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### **Advanced Composition Patterns**

Create `CompletableFutureComposition.java`:

```java
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Random;

/**
 * Advanced CompletableFuture composition and chaining patterns
 */
public class CompletableFutureComposition {
    
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println("=== CompletableFuture Advanced Composition ===\n");
        
        // 1. Sequential composition
        System.out.println("1. Sequential Composition:");
        demonstrateSequentialComposition();
        
        // 2. Parallel composition
        System.out.println("\n2. Parallel Composition:");
        demonstrateParallelComposition();
        
        // 3. Complex pipelines
        System.out.println("\n3. Complex Processing Pipelines:");
        demonstrateComplexPipelines();
        
        // 4. Dynamic composition
        System.out.println("\n4. Dynamic Composition:");
        demonstrateDynamicComposition();
        
        System.out.println("\n=== Advanced Composition Completed ===");
    }
    
    private static void demonstrateSequentialComposition() {
        // Chain of dependent operations
        CompletableFuture<String> pipeline = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Step 1: Fetching user data...");
                sleep(500);
                return "user123";
            })
            .thenCompose(userId -> {
                System.out.println("Step 2: Fetching user profile for " + userId);
                return CompletableFuture.supplyAsync(() -> {
                    sleep(800);
                    return "UserProfile{id=" + userId + ", name=John Doe}";
                });
            })
            .thenCompose(profile -> {
                System.out.println("Step 3: Fetching user preferences for " + profile);
                return CompletableFuture.supplyAsync(() -> {
                    sleep(600);
                    return profile + ", preferences={theme=dark, lang=en}";
                });
            })
            .thenApply(fullProfile -> {
                System.out.println("Step 4: Processing complete profile");
                return "Processed: " + fullProfile;
            });
        
        String result = pipeline.join();
        System.out.println("Final result: " + result);
    }
    
    private static void demonstrateParallelComposition() {
        // Multiple independent operations
        CompletableFuture<String> userService = CompletableFuture.supplyAsync(() -> {
            System.out.println("Calling user service...");
            sleep(1000);
            return "UserData";
        });
        
        CompletableFuture<String> productService = CompletableFuture.supplyAsync(() -> {
            System.out.println("Calling product service...");
            sleep(1200);
            return "ProductData";
        });
        
        CompletableFuture<String> orderService = CompletableFuture.supplyAsync(() -> {
            System.out.println("Calling order service...");
            sleep(800);
            return "OrderData";
        });
        
        // Combine all results
        CompletableFuture<String> combinedResult = userService
            .thenCombine(productService, (user, product) -> user + " + " + product)
            .thenCombine(orderService, (combined, order) -> combined + " + " + order);
        
        System.out.println("Combined service result: " + combinedResult.join());
        
        // Alternative: allOf for multiple futures
        CompletableFuture<Void> allServices = CompletableFuture.allOf(
            userService, productService, orderService
        );
        
        CompletableFuture<List<String>> allResults = allServices.thenApply(v ->
            List.of(userService.join(), productService.join(), orderService.join())
        );
        
        System.out.println("All service results: " + allResults.join());
    }
    
    private static void demonstrateComplexPipelines() {
        // Simulate a complex data processing pipeline
        CompletableFuture<ProcessingResult> pipeline = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Stage 1: Data ingestion");
                sleep(300);
                return new RawData("raw input data");
            })
            .thenCompose(rawData -> {
                System.out.println("Stage 2: Data validation");
                return CompletableFuture.supplyAsync(() -> {
                    sleep(200);
                    if (rawData.isValid()) {
                        return new ValidatedData(rawData.content + " [validated]");
                    } else {
                        throw new RuntimeException("Invalid data");
                    }
                });
            })
            .thenCompose(validatedData -> {
                // Parallel processing of validated data
                CompletableFuture<String> transform1 = CompletableFuture.supplyAsync(() -> {
                    System.out.println("Stage 3a: Transform 1");
                    sleep(400);
                    return validatedData.content + " [transform1]";
                });
                
                CompletableFuture<String> transform2 = CompletableFuture.supplyAsync(() -> {
                    System.out.println("Stage 3b: Transform 2");
                    sleep(350);
                    return validatedData.content + " [transform2]";
                });
                
                return transform1.thenCombine(transform2, (t1, t2) -> {
                    System.out.println("Stage 4: Combining transformations");
                    return new TransformedData(t1 + " + " + t2);
                });
            })
            .thenCompose(transformedData -> {
                System.out.println("Stage 5: Final processing");
                return CompletableFuture.supplyAsync(() -> {
                    sleep(250);
                    return new ProcessingResult(transformedData.content + " [processed]");
                });
            });
        
        ProcessingResult result = pipeline.join();
        System.out.println("Pipeline result: " + result.content);
    }
    
    private static void demonstrateDynamicComposition() {
        // Dynamic number of operations based on input
        List<String> inputData = List.of("item1", "item2", "item3", "item4", "item5");
        
        System.out.println("Processing " + inputData.size() + " items dynamically");
        
        // Create futures dynamically
        List<CompletableFuture<String>> futures = inputData.stream()
            .map(item -> CompletableFuture.supplyAsync(() -> {
                System.out.println("Processing: " + item);
                sleep(200 + random.nextInt(500));
                return "Processed: " + item;
            }))
            .collect(Collectors.toList());
        
        // Combine all futures
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        // Collect results
        CompletableFuture<List<String>> allResults = allFutures.thenApply(v ->
            futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
        );
        
        List<String> results = allResults.join();
        System.out.println("Dynamic processing results:");
        results.forEach(result -> System.out.println("  " + result));
        
        // Sequential processing of dynamic operations
        CompletableFuture<String> sequentialPipeline = inputData.stream()
            .reduce(
                CompletableFuture.completedFuture("Start"),
                (future, item) -> future.thenCompose(previous -> 
                    CompletableFuture.supplyAsync(() -> {
                        System.out.println("Sequential processing: " + item + " (after " + previous + ")");
                        sleep(200);
                        return previous + " -> " + item;
                    })
                ),
                (f1, f2) -> f1.thenCombine(f2, (a, b) -> a + " | " + b)
            );
        
        System.out.println("Sequential pipeline result: " + sequentialPipeline.join());
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Supporting classes for pipeline demonstration
class RawData {
    final String content;
    
    RawData(String content) {
        this.content = content;
    }
    
    boolean isValid() {
        return content != null && !content.trim().isEmpty();
    }
}

class ValidatedData {
    final String content;
    
    ValidatedData(String content) {
        this.content = content;
    }
}

class TransformedData {
    final String content;
    
    TransformedData(String content) {
        this.content = content;
    }
}

class ProcessingResult {
    final String content;
    
    ProcessingResult(String content) {
        this.content = content;
    }
}
```

---

## ⚠️ Part 2: Error Handling & Exception Management (60 minutes)

### **CompletableFuture Exception Handling**

Create `CompletableFutureErrorHandling.java`:

```java
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.Random;

/**
 * Comprehensive error handling patterns with CompletableFuture
 */
public class CompletableFutureErrorHandling {
    
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println("=== CompletableFuture Error Handling ===\n");
        
        // 1. Basic exception handling
        System.out.println("1. Basic Exception Handling:");
        demonstrateBasicErrorHandling();
        
        // 2. Recovery patterns
        System.out.println("\n2. Recovery Patterns:");
        demonstrateRecoveryPatterns();
        
        // 3. Exception propagation
        System.out.println("\n3. Exception Propagation:");
        demonstrateExceptionPropagation();
        
        // 4. Timeout and cancellation
        System.out.println("\n4. Timeout and Cancellation:");
        demonstrateTimeoutAndCancellation();
        
        System.out.println("\n=== Error Handling Completed ===");
    }
    
    private static void demonstrateBasicErrorHandling() {
        // Handle exceptions with handle()
        CompletableFuture<String> futureWithError = CompletableFuture
            .supplyAsync(() -> {
                if (random.nextBoolean()) {
                    throw new RuntimeException("Random failure occurred");
                }
                return "Success result";
            })
            .handle((result, throwable) -> {
                if (throwable != null) {
                    System.out.println("Error occurred: " + throwable.getMessage());
                    return "Default result after error";
                } else {
                    System.out.println("Operation succeeded: " + result);
                    return result;
                }
            });
        
        System.out.println("Handle result: " + futureWithError.join());
        
        // Exception handling with exceptionally()
        CompletableFuture<String> futureWithExceptionally = CompletableFuture
            .supplyAsync(() -> {
                throw new IllegalStateException("Simulated error");
            })
            .exceptionally(throwable -> {
                System.out.println("Handled exception: " + throwable.getMessage());
                return "Recovery value";
            });
        
        System.out.println("Exceptionally result: " + futureWithExceptionally.join());
        
        // Complete exceptionally
        CompletableFuture<String> manualError = new CompletableFuture<>();
        manualError.completeExceptionally(new CustomException("Manual error"));
        
        String manualResult = manualError.handle((result, throwable) -> {
            if (throwable != null) {
                return "Handled manual error: " + throwable.getCause().getMessage();
            }
            return result;
        }).join();
        
        System.out.println("Manual error result: " + manualResult);
    }
    
    private static void demonstrateRecoveryPatterns() {
        // Primary service with fallback
        CompletableFuture<String> primaryService = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Calling primary service...");
                if (random.nextFloat() < 0.7) { // 70% failure rate
                    throw new ServiceException("Primary service unavailable");
                }
                return "Primary service result";
            });
        
        CompletableFuture<String> fallbackService = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Calling fallback service...");
                sleep(300); // Slower but more reliable
                return "Fallback service result";
            });
        
        // Fallback pattern
        CompletableFuture<String> serviceWithFallback = primaryService
            .exceptionally(throwable -> {
                System.out.println("Primary failed: " + throwable.getMessage());
                return fallbackService.join();
            });
        
        System.out.println("Service result: " + serviceWithFallback.join());
        
        // Retry pattern
        CompletableFuture<String> retryableFuture = retryOperation(
            () -> {
                System.out.println("Attempting operation...");
                if (random.nextFloat() < 0.6) { // 60% failure rate
                    throw new TransientException("Transient failure");
                }
                return "Retry success";
            },
            3 // max retries
        );
        
        System.out.println("Retry result: " + retryableFuture.join());
        
        // Circuit breaker pattern (simplified)
        CircuitBreaker circuitBreaker = new CircuitBreaker();
        
        for (int i = 1; i <= 8; i++) {
            final int attempt = i;
            CompletableFuture<String> circuitBreakerFuture = CompletableFuture
                .supplyAsync(() -> {
                    if (circuitBreaker.canExecute()) {
                        try {
                            if (random.nextFloat() < 0.8) { // 80% failure rate
                                throw new ServiceException("Service failure #" + attempt);
                            }
                            circuitBreaker.recordSuccess();
                            return "Circuit breaker success #" + attempt;
                        } catch (Exception e) {
                            circuitBreaker.recordFailure();
                            throw e;
                        }
                    } else {
                        return "Circuit breaker open - using cached result";
                    }
                })
                .exceptionally(throwable -> {
                    return "Circuit breaker handled: " + throwable.getMessage();
                });
            
            System.out.println("Attempt " + attempt + ": " + circuitBreakerFuture.join());
        }
    }
    
    private static void demonstrateExceptionPropagation() {
        // Exception propagation through chain
        CompletableFuture<String> chainWithError = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Step 1: Initial processing");
                return "initial";
            })
            .thenApply(result -> {
                System.out.println("Step 2: Processing " + result);
                if (random.nextBoolean()) {
                    throw new ProcessingException("Error in step 2");
                }
                return result + " -> step2";
            })
            .thenApply(result -> {
                System.out.println("Step 3: Processing " + result);
                return result + " -> step3";
            })
            .thenApply(result -> {
                System.out.println("Step 4: Final processing " + result);
                return result + " -> final";
            })
            .exceptionally(throwable -> {
                System.out.println("Chain failed at: " + throwable.getMessage());
                return "Error recovery result";
            });
        
        System.out.println("Chain result: " + chainWithError.join());
        
        // Handle errors at specific stages
        CompletableFuture<String> stageSpecificHandling = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Risky operation 1");
                if (random.nextBoolean()) {
                    throw new ValidationException("Validation failed");
                }
                return "validated";
            })
            .handle((result, throwable) -> {
                if (throwable != null) {
                    System.out.println("Validation error handled: " + throwable.getMessage());
                    return "default_validated";
                }
                return result;
            })
            .thenCompose(result -> {
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("Risky operation 2 with: " + result);
                    if (random.nextBoolean()) {
                        throw new BusinessException("Business logic failed");
                    }
                    return result + " -> processed";
                });
            })
            .exceptionally(throwable -> {
                System.out.println("Business error handled: " + throwable.getMessage());
                return "business_error_fallback";
            });
        
        System.out.println("Stage-specific handling result: " + stageSpecificHandling.join());
    }
    
    private static void demonstrateTimeoutAndCancellation() {
        // Timeout handling
        CompletableFuture<String> slowOperation = CompletableFuture.supplyAsync(() -> {
            System.out.println("Starting slow operation...");
            sleep(5000); // 5 seconds
            return "Slow operation completed";
        });
        
        CompletableFuture<String> timeoutFuture = slowOperation
            .completeOnTimeout("Timeout default value", 2, TimeUnit.SECONDS);
        
        System.out.println("Timeout result: " + timeoutFuture.join());
        
        // Cancellation
        CompletableFuture<String> cancellableFuture = CompletableFuture.supplyAsync(() -> {
            for (int i = 1; i <= 10; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeException("Operation was cancelled");
                }
                System.out.println("Cancellable operation step " + i);
                sleep(500);
            }
            return "Cancellable operation completed";
        });
        
        // Cancel after 2 seconds
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
            System.out.println("Cancelling operation...");
            cancellableFuture.cancel(true);
        });
        
        try {
            String result = cancellableFuture.get();
            System.out.println("Cancellation result: " + result);
        } catch (CancellationException e) {
            System.out.println("Operation was cancelled");
        } catch (Exception e) {
            System.out.println("Other error: " + e.getMessage());
        }
        
        // Ortimeout (Java 9+)
        CompletableFuture<String> anotherSlowOperation = CompletableFuture.supplyAsync(() -> {
            sleep(4000);
            return "Another slow result";
        });
        
        try {
            String result = anotherSlowOperation.orTimeout(1, TimeUnit.SECONDS).join();
            System.out.println("OrTimeout result: " + result);
        } catch (Exception e) {
            System.out.println("OrTimeout failed: " + e.getCause().getClass().getSimpleName());
        }
    }
    
    private static CompletableFuture<String> retryOperation(
            java.util.function.Supplier<String> operation, int maxRetries) {
        
        return CompletableFuture.supplyAsync(() -> {
            Exception lastException = null;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    return operation.get();
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < maxRetries) {
                        System.out.println("Attempt " + attempt + " failed, retrying...");
                        sleep(1000 * attempt); // Exponential backoff
                    }
                }
            }
            
            throw new RuntimeException("All retry attempts failed", lastException);
        });
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Custom exceptions
class CustomException extends Exception {
    public CustomException(String message) {
        super(message);
    }
}

class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }
}

class TransientException extends RuntimeException {
    public TransientException(String message) {
        super(message);
    }
}

class ProcessingException extends RuntimeException {
    public ProcessingException(String message) {
        super(message);
    }
}

class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

// Simple circuit breaker implementation
class CircuitBreaker {
    private int failureCount = 0;
    private final int threshold = 3;
    private long lastFailureTime = 0;
    private final long timeout = 5000; // 5 seconds
    
    public boolean canExecute() {
        if (failureCount >= threshold) {
            if (System.currentTimeMillis() - lastFailureTime > timeout) {
                failureCount = 0; // Reset
                return true;
            }
            return false; // Circuit open
        }
        return true;
    }
    
    public void recordSuccess() {
        failureCount = 0;
    }
    
    public void recordFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
    }
}
```

---

## 🏭 Part 3: Real-World Async Applications (60 minutes)

### **Production-Ready Async Service**

Create `AsyncServiceExamples.java`:

```java
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Random;

/**
 * Real-world asynchronous service implementations
 */
public class AsyncServiceExamples {
    
    public static void main(String[] args) {
        System.out.println("=== Real-World Async Service Examples ===\n");
        
        // 1. E-commerce order processing
        System.out.println("1. E-commerce Order Processing:");
        demonstrateOrderProcessing();
        
        // 2. Data aggregation service
        System.out.println("\n2. Data Aggregation Service:");
        demonstrateDataAggregation();
        
        // 3. Notification service
        System.out.println("\n3. Async Notification Service:");
        demonstrateNotificationService();
        
        System.out.println("\n=== Real-World Examples Completed ===");
    }
    
    private static void demonstrateOrderProcessing() {
        OrderService orderService = new OrderService();
        
        // Process multiple orders concurrently
        List<String> orderIds = List.of("ORDER-001", "ORDER-002", "ORDER-003");
        
        CompletableFuture<Void> allOrders = CompletableFuture.allOf(
            orderIds.stream()
                .map(orderService::processOrder)
                .toArray(CompletableFuture[]::new)
        );
        
        allOrders.join();
        System.out.println("All orders processed");
    }
    
    private static void demonstrateDataAggregation() {
        DataAggregationService aggregationService = new DataAggregationService();
        
        String userId = "user123";
        CompletableFuture<UserDashboard> dashboard = aggregationService
            .getUserDashboard(userId);
        
        UserDashboard result = dashboard.join();
        System.out.println("Dashboard result: " + result);
    }
    
    private static void demonstrateNotificationService() {
        NotificationService notificationService = new NotificationService();
        
        List<String> userIds = List.of("user1", "user2", "user3", "user4", "user5");
        String message = "Important system update available";
        
        CompletableFuture<NotificationResult> notificationResult = 
            notificationService.sendBulkNotifications(userIds, message);
        
        NotificationResult result = notificationResult.join();
        System.out.println("Notification result: " + result);
    }
}

/**
 * E-commerce order processing service
 */
class OrderService {
    private final Random random = new Random();
    
    public CompletableFuture<OrderResult> processOrder(String orderId) {
        return validateOrder(orderId)
            .thenCompose(this::checkInventory)
            .thenCompose(this::processPayment)
            .thenCompose(this::reserveItems)
            .thenCompose(this::scheduleShipping)
            .thenApply(order -> {
                System.out.println("Order " + orderId + " completed successfully");
                return new OrderResult(orderId, "COMPLETED");
            })
            .exceptionally(throwable -> {
                System.out.println("Order " + orderId + " failed: " + throwable.getMessage());
                return new OrderResult(orderId, "FAILED");
            });
    }
    
    private CompletableFuture<Order> validateOrder(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Validating order: " + orderId);
            sleep(200);
            
            if (random.nextFloat() < 0.1) { // 10% validation failure
                throw new OrderValidationException("Invalid order data");
            }
            
            return new Order(orderId, "VALIDATED");
        });
    }
    
    private CompletableFuture<Order> checkInventory(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Checking inventory for: " + order.orderId);
            sleep(300);
            
            if (random.nextFloat() < 0.15) { // 15% inventory issue
                throw new InventoryException("Insufficient inventory");
            }
            
            order.status = "INVENTORY_CHECKED";
            return order;
        });
    }
    
    private CompletableFuture<Order> processPayment(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Processing payment for: " + order.orderId);
            sleep(500);
            
            if (random.nextFloat() < 0.05) { // 5% payment failure
                throw new PaymentException("Payment processing failed");
            }
            
            order.status = "PAYMENT_PROCESSED";
            return order;
        });
    }
    
    private CompletableFuture<Order> reserveItems(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Reserving items for: " + order.orderId);
            sleep(250);
            
            order.status = "ITEMS_RESERVED";
            return order;
        });
    }
    
    private CompletableFuture<Order> scheduleShipping(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Scheduling shipping for: " + order.orderId);
            sleep(400);
            
            order.status = "SHIPPING_SCHEDULED";
            return order;
        });
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Data aggregation service for user dashboard
 */
class DataAggregationService {
    
    public CompletableFuture<UserDashboard> getUserDashboard(String userId) {
        // Fetch data from multiple services in parallel
        CompletableFuture<UserProfile> profileFuture = getUserProfile(userId);
        CompletableFuture<List<Order>> ordersFuture = getUserOrders(userId);
        CompletableFuture<List<Recommendation>> recommendationsFuture = getUserRecommendations(userId);
        CompletableFuture<AccountSummary> accountFuture = getAccountSummary(userId);
        
        // Combine all data
        return CompletableFuture.allOf(profileFuture, ordersFuture, recommendationsFuture, accountFuture)
            .thenApply(v -> {
                UserProfile profile = profileFuture.join();
                List<Order> orders = ordersFuture.join();
                List<Recommendation> recommendations = recommendationsFuture.join();
                AccountSummary account = accountFuture.join();
                
                return new UserDashboard(profile, orders, recommendations, account);
            })
            .exceptionally(throwable -> {
                System.out.println("Dashboard aggregation failed: " + throwable.getMessage());
                // Return partial data or default dashboard
                return new UserDashboard(null, List.of(), List.of(), null);
            });
    }
    
    private CompletableFuture<UserProfile> getUserProfile(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching user profile for: " + userId);
            sleep(300);
            return new UserProfile(userId, "John Doe");
        });
    }
    
    private CompletableFuture<List<Order>> getUserOrders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching orders for: " + userId);
            sleep(500);
            return List.of(
                new Order("ORDER-1", "DELIVERED"),
                new Order("ORDER-2", "SHIPPED")
            );
        });
    }
    
    private CompletableFuture<List<Recommendation>> getUserRecommendations(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Generating recommendations for: " + userId);
            sleep(800);
            return List.of(
                new Recommendation("Product A"),
                new Recommendation("Product B")
            );
        });
    }
    
    private CompletableFuture<AccountSummary> getAccountSummary(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching account summary for: " + userId);
            sleep(400);
            return new AccountSummary(userId, 1250.50);
        });
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Bulk notification service
 */
class NotificationService {
    private final ExecutorService notificationExecutor = Executors.newFixedThreadPool(5);
    
    public CompletableFuture<NotificationResult> sendBulkNotifications(
            List<String> userIds, String message) {
        
        System.out.println("Sending notifications to " + userIds.size() + " users");
        
        // Send notifications in parallel with batching
        List<CompletableFuture<Boolean>> notificationFutures = userIds.stream()
            .map(userId -> sendNotification(userId, message))
            .collect(Collectors.toList());
        
        // Aggregate results
        CompletableFuture<Void> allNotifications = CompletableFuture.allOf(
            notificationFutures.toArray(new CompletableFuture[0])
        );
        
        return allNotifications.thenApply(v -> {
            long successCount = notificationFutures.stream()
                .mapToLong(future -> future.join() ? 1 : 0)
                .sum();
            
            long failureCount = userIds.size() - successCount;
            
            return new NotificationResult((int) successCount, (int) failureCount);
        });
    }
    
    private CompletableFuture<Boolean> sendNotification(String userId, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Sending notification to: " + userId);
                
                // Simulate different notification channels
                String channel = selectNotificationChannel(userId);
                
                // Simulate network call
                sleep(100 + new Random().nextInt(200));
                
                // Simulate occasional failures
                if (new Random().nextFloat() < 0.1) { // 10% failure rate
                    throw new NotificationException("Failed to send to " + userId);
                }
                
                System.out.println("Notification sent to " + userId + " via " + channel);
                return true;
                
            } catch (Exception e) {
                System.out.println("Notification failed for " + userId + ": " + e.getMessage());
                return false;
            }
        }, notificationExecutor);
    }
    
    private String selectNotificationChannel(String userId) {
        // Simulate channel selection logic
        String[] channels = {"EMAIL", "SMS", "PUSH"};
        return channels[userId.hashCode() % channels.length];
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void shutdown() {
        notificationExecutor.shutdown();
    }
}

// Supporting data classes
class Order {
    String orderId;
    String status;
    
    Order(String orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "Order{id='" + orderId + "', status='" + status + "'}";
    }
}

class OrderResult {
    String orderId;
    String status;
    
    OrderResult(String orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "OrderResult{id='" + orderId + "', status='" + status + "'}";
    }
}

class UserProfile {
    String userId;
    String name;
    
    UserProfile(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}

class Recommendation {
    String product;
    
    Recommendation(String product) {
        this.product = product;
    }
}

class AccountSummary {
    String userId;
    double balance;
    
    AccountSummary(String userId, double balance) {
        this.userId = userId;
        this.balance = balance;
    }
}

class UserDashboard {
    UserProfile profile;
    List<Order> orders;
    List<Recommendation> recommendations;
    AccountSummary account;
    
    UserDashboard(UserProfile profile, List<Order> orders, 
                  List<Recommendation> recommendations, AccountSummary account) {
        this.profile = profile;
        this.orders = orders;
        this.recommendations = recommendations;
        this.account = account;
    }
    
    @Override
    public String toString() {
        return "UserDashboard{orders=" + orders.size() + 
               ", recommendations=" + recommendations.size() + "}";
    }
}

class NotificationResult {
    int successCount;
    int failureCount;
    
    NotificationResult(int successCount, int failureCount) {
        this.successCount = successCount;
        this.failureCount = failureCount;
    }
    
    @Override
    public String toString() {
        return "NotificationResult{success=" + successCount + 
               ", failures=" + failureCount + "}";
    }
}

// Custom exceptions
class OrderValidationException extends RuntimeException {
    public OrderValidationException(String message) {
        super(message);
    }
}

class InventoryException extends RuntimeException {
    public InventoryException(String message) {
        super(message);
    }
}

class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}

class NotificationException extends RuntimeException {
    public NotificationException(String message) {
        super(message);
    }
}
```

---

## 🏆 Day 33 Summary

### **Key Concepts Mastered:**

✅ **CompletableFuture Fundamentals:**

- Asynchronous execution patterns
- Creation methods (supplyAsync, runAsync, completedFuture)
- Result retrieval and timeout handling
- Custom executor integration

✅ **Advanced Composition:**

- Sequential composition with thenCompose()
- Parallel composition with thenCombine()
- Complex processing pipelines
- Dynamic operation chaining

✅ **Error Handling Mastery:**

- Exception handling with handle() and exceptionally()
- Recovery patterns and fallback strategies
- Circuit breaker implementation
- Timeout and cancellation management

✅ **Real-World Applications:**

- E-commerce order processing pipelines
- Data aggregation services
- Bulk notification systems
- Production-ready async patterns

✅ **Best Practices:**

- Proper exception propagation
- Resource management with custom executors
- Performance optimization techniques
- Monitoring and error recovery

### **Production Applications:**

- Microservices communication
- API gateway implementations
- Background job processing
- Real-time data processing
- Event-driven architectures

### **Next Steps:**

Tomorrow we'll explore **Performance & Best Practices** - learning how to optimize concurrent applications, measure performance, and avoid common pitfalls.

You're now equipped to build sophisticated asynchronous applications with modern reactive patterns! 🚀⚡
