# Day 96: GraphQL & Advanced API Design

## Learning Objectives
- Master GraphQL schema design and implementation
- Build efficient data fetching with DataLoaders
- Implement real-time subscriptions and mutations
- Design scalable GraphQL APIs with security
- Integrate GraphQL with modern Java frameworks

## 1. GraphQL Fundamentals with Java

### Spring Boot GraphQL Setup

```java
package com.javajourney.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Controller;

import graphql.scalars.ExtendedScalars;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class GraphQLApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GraphQLApplication.class, args);
    }
    
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.Json)
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(ExtendedScalars.UUID);
    }
}

// GraphQL Schema Definition
/*
schema.graphqls:

scalar DateTime
scalar JSON
scalar UUID

type Query {
    users(first: Int, after: String, filter: UserFilter): UserConnection!
    user(id: ID!): User
    posts(first: Int, after: String, authorId: ID): PostConnection!
    post(id: ID!): Post
    comments(postId: ID!, first: Int, after: String): CommentConnection!
}

type Mutation {
    createUser(input: CreateUserInput!): CreateUserPayload!
    updateUser(id: ID!, input: UpdateUserInput!): UpdateUserPayload!
    deleteUser(id: ID!): DeleteUserPayload!
    
    createPost(input: CreatePostInput!): CreatePostPayload!
    updatePost(id: ID!, input: UpdatePostInput!): UpdatePostPayload!
    deletePost(id: ID!): DeletePostPayload!
    
    addComment(input: AddCommentInput!): AddCommentPayload!
    updateComment(id: ID!, input: UpdateCommentInput!): UpdateCommentPayload!
    deleteComment(id: ID!): DeleteCommentPayload!
}

type Subscription {
    postAdded: Post!
    commentAdded(postId: ID!): Comment!
    userStatusChanged(userId: ID!): UserStatus!
}

type User {
    id: ID!
    username: String!
    email: String!
    profile: UserProfile!
    posts(first: Int, after: String): PostConnection!
    comments(first: Int, after: String): CommentConnection!
    followers(first: Int, after: String): UserConnection!
    following(first: Int, after: String): UserConnection!
    createdAt: DateTime!
    updatedAt: DateTime!
    status: UserStatus!
}

type UserProfile {
    firstName: String!
    lastName: String!
    bio: String
    avatar: String
    location: String
    website: String
    birthDate: DateTime
}

type Post {
    id: ID!
    title: String!
    content: String!
    excerpt: String
    author: User!
    comments(first: Int, after: String): CommentConnection!
    tags: [String!]!
    status: PostStatus!
    publishedAt: DateTime
    createdAt: DateTime!
    updatedAt: DateTime!
    likesCount: Int!
    commentsCount: Int!
    metadata: JSON
}

type Comment {
    id: ID!
    content: String!
    author: User!
    post: Post!
    parent: Comment
    replies(first: Int, after: String): CommentConnection!
    createdAt: DateTime!
    updatedAt: DateTime!
    likesCount: Int!
}

# Connection types for pagination
type UserConnection {
    edges: [UserEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type UserEdge {
    node: User!
    cursor: String!
}

type PostConnection {
    edges: [PostEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type PostEdge {
    node: Post!
    cursor: String!
}

type CommentConnection {
    edges: [CommentEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type CommentEdge {
    node: Comment!
    cursor: String!
}

type PageInfo {
    hasNextPage: Boolean!
    hasPreviousPage: Boolean!
    startCursor: String
    endCursor: String
}

# Input types
input CreateUserInput {
    username: String!
    email: String!
    password: String!
    profile: CreateUserProfileInput!
}

input CreateUserProfileInput {
    firstName: String!
    lastName: String!
    bio: String
    avatar: String
    location: String
    website: String
    birthDate: DateTime
}

input UpdateUserInput {
    username: String
    email: String
    profile: UpdateUserProfileInput
}

input UpdateUserProfileInput {
    firstName: String
    lastName: String
    bio: String
    avatar: String
    location: String
    website: String
    birthDate: DateTime
}

input CreatePostInput {
    title: String!
    content: String!
    excerpt: String
    tags: [String!]
    status: PostStatus = DRAFT
    publishedAt: DateTime
    metadata: JSON
}

input UpdatePostInput {
    title: String
    content: String
    excerpt: String
    tags: [String!]
    status: PostStatus
    publishedAt: DateTime
    metadata: JSON
}

input AddCommentInput {
    postId: ID!
    parentId: ID
    content: String!
}

input UpdateCommentInput {
    content: String!
}

input UserFilter {
    username: String
    email: String
    status: UserStatus
    createdAfter: DateTime
    createdBefore: DateTime
}

# Payload types
type CreateUserPayload {
    user: User
    errors: [Error!]
}

type UpdateUserPayload {
    user: User
    errors: [Error!]
}

type DeleteUserPayload {
    deletedUserId: ID
    errors: [Error!]
}

type CreatePostPayload {
    post: Post
    errors: [Error!]
}

type UpdatePostPayload {
    post: Post
    errors: [Error!]
}

type DeletePostPayload {
    deletedPostId: ID
    errors: [Error!]
}

type AddCommentPayload {
    comment: Comment
    errors: [Error!]
}

type UpdateCommentPayload {
    comment: Comment
    errors: [Error!]
}

type DeleteCommentPayload {
    deletedCommentId: ID
    errors: [Error!]
}

type Error {
    message: String!
    field: String
    code: String!
}

# Enums
enum UserStatus {
    ACTIVE
    INACTIVE
    SUSPENDED
    PENDING_VERIFICATION
}

enum PostStatus {
    DRAFT
    PUBLISHED
    ARCHIVED
    DELETED
}
*/

// Data Models
public record User(
    String id,
    String username,
    String email,
    UserProfile profile,
    UserStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

public record UserProfile(
    String firstName,
    String lastName,
    String bio,
    String avatar,
    String location,
    String website,
    LocalDateTime birthDate
) {}

public record Post(
    String id,
    String title,
    String content,
    String excerpt,
    String authorId,
    List<String> tags,
    PostStatus status,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int likesCount,
    int commentsCount,
    Object metadata
) {}

public record Comment(
    String id,
    String content,
    String authorId,
    String postId,
    String parentId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int likesCount
) {}

public enum UserStatus {
    ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION
}

public enum PostStatus {
    DRAFT, PUBLISHED, ARCHIVED, DELETED
}
```

### GraphQL Controllers and Resolvers

```java
package com.javajourney.graphql.resolvers;

import com.javajourney.graphql.model.*;
import com.javajourney.graphql.service.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

@Controller
public class GraphQLResolver {
    
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final GraphQLDataLoader dataLoader;
    
    public GraphQLResolver(UserService userService, PostService postService, 
                          CommentService commentService, GraphQLDataLoader dataLoader) {
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
        this.dataLoader = dataLoader;
    }
    
    // Query resolvers
    @QueryMapping
    public CompletableFuture<UserConnection> users(
            @Argument Integer first,
            @Argument String after,
            @Argument UserFilter filter) {
        return userService.getUsers(first, after, filter);
    }
    
    @QueryMapping
    public CompletableFuture<User> user(@Argument String id) {
        return dataLoader.loadUser(id);
    }
    
    @QueryMapping
    public CompletableFuture<PostConnection> posts(
            @Argument Integer first,
            @Argument String after,
            @Argument String authorId) {
        return postService.getPosts(first, after, authorId);
    }
    
    @QueryMapping
    public CompletableFuture<Post> post(@Argument String id) {
        return dataLoader.loadPost(id);
    }
    
    @QueryMapping
    public CompletableFuture<CommentConnection> comments(
            @Argument String postId,
            @Argument Integer first,
            @Argument String after) {
        return commentService.getComments(postId, first, after);
    }
    
    // Mutation resolvers
    @MutationMapping
    public CompletableFuture<CreateUserPayload> createUser(@Argument CreateUserInput input) {
        return userService.createUser(input);
    }
    
    @MutationMapping
    public CompletableFuture<UpdateUserPayload> updateUser(
            @Argument String id,
            @Argument UpdateUserInput input) {
        return userService.updateUser(id, input);
    }
    
    @MutationMapping
    public CompletableFuture<DeleteUserPayload> deleteUser(@Argument String id) {
        return userService.deleteUser(id);
    }
    
    @MutationMapping
    public CompletableFuture<CreatePostPayload> createPost(@Argument CreatePostInput input) {
        return postService.createPost(input);
    }
    
    @MutationMapping
    public CompletableFuture<UpdatePostPayload> updatePost(
            @Argument String id,
            @Argument UpdatePostInput input) {
        return postService.updatePost(id, input);
    }
    
    @MutationMapping
    public CompletableFuture<DeletePostPayload> deletePost(@Argument String id) {
        return postService.deletePost(id);
    }
    
    @MutationMapping
    public CompletableFuture<AddCommentPayload> addComment(@Argument AddCommentInput input) {
        return commentService.addComment(input);
    }
    
    @MutationMapping
    public CompletableFuture<UpdateCommentPayload> updateComment(
            @Argument String id,
            @Argument UpdateCommentInput input) {
        return commentService.updateComment(id, input);
    }
    
    @MutationMapping 
    public CompletableFuture<DeleteCommentPayload> deleteComment(@Argument String id) {
        return commentService.deleteComment(id);
    }
    
    // Field resolvers for complex fields
    @SchemaMapping(typeName = "User", field = "posts")
    public CompletableFuture<PostConnection> userPosts(
            User user,
            @Argument Integer first,
            @Argument String after) {
        return postService.getPostsByAuthor(user.id(), first, after);
    }
    
    @SchemaMapping(typeName = "User", field = "comments")
    public CompletableFuture<CommentConnection> userComments(
            User user,
            @Argument Integer first,
            @Argument String after) {
        return commentService.getCommentsByAuthor(user.id(), first, after);
    }
    
    @SchemaMapping(typeName = "User", field = "followers")
    public CompletableFuture<UserConnection> userFollowers(
            User user,
            @Argument Integer first,
            @Argument String after) {
        return userService.getFollowers(user.id(), first, after);
    }
    
    @SchemaMapping(typeName = "User", field = "following")
    public CompletableFuture<UserConnection> userFollowing(
            User user,
            @Argument Integer first,
            @Argument String after) {
        return userService.getFollowing(user.id(), first, after);
    }
    
    @SchemaMapping(typeName = "Post", field = "author")
    public CompletableFuture<User> postAuthor(Post post) {
        return dataLoader.loadUser(post.authorId());
    }
    
    @SchemaMapping(typeName = "Post", field = "comments")
    public CompletableFuture<CommentConnection> postComments(
            Post post,
            @Argument Integer first,
            @Argument String after) {
        return commentService.getComments(post.id(), first, after);
    }
    
    @SchemaMapping(typeName = "Comment", field = "author")
    public CompletableFuture<User> commentAuthor(Comment comment) {
        return dataLoader.loadUser(comment.authorId());
    }
    
    @SchemaMapping(typeName = "Comment", field = "post")
    public CompletableFuture<Post> commentPost(Comment comment) {
        return dataLoader.loadPost(comment.postId());
    }
    
    @SchemaMapping(typeName = "Comment", field = "parent")
    public CompletableFuture<Comment> commentParent(Comment comment) {
        if (comment.parentId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return dataLoader.loadComment(comment.parentId());
    }
    
    @SchemaMapping(typeName = "Comment", field = "replies")
    public CompletableFuture<CommentConnection> commentReplies(
            Comment comment,
            @Argument Integer first,
            @Argument String after) {
        return commentService.getReplies(comment.id(), first, after);
    }
}
```

## 2. DataLoader for Efficient Data Fetching

### DataLoader Implementation

```java
package com.javajourney.graphql.dataloader;

import com.javajourney.graphql.model.*;
import com.javajourney.graphql.repository.*;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@org.springframework.graphql.execution.DataLoaderRegistrar
public class GraphQLDataLoader implements org.springframework.graphql.execution.DataLoaderRegistrar {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    
    public GraphQLDataLoader(UserRepository userRepository, 
                           PostRepository postRepository,
                           CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }
    
    @Override
    public void registerDataLoaders(DataLoaderRegistry registry) {
        // User DataLoader
        DataLoader<String, User> userLoader = DataLoader.newDataLoader(
            userIds -> {
                System.out.println("Batch loading users: " + userIds);
                return userRepository.findByIds(userIds);
            },
            DataLoaderOptions.newOptions()
                .setBatchingEnabled(true)
                .setCachingEnabled(true)
                .setMaxBatchSize(100)
        );
        registry.register("userLoader", userLoader);
        
        // Post DataLoader
        DataLoader<String, Post> postLoader = DataLoader.newDataLoader(
            postIds -> {
                System.out.println("Batch loading posts: " + postIds);
                return postRepository.findByIds(postIds);
            },
            DataLoaderOptions.newOptions()
                .setBatchingEnabled(true)
                .setCachingEnabled(true)
                .setMaxBatchSize(100)
        );
        registry.register("postLoader", postLoader);
        
        // Comment DataLoader
        DataLoader<String, Comment> commentLoader = DataLoader.newDataLoader(
            commentIds -> {
                System.out.println("Batch loading comments: " + commentIds);
                return commentRepository.findByIds(commentIds);
            },
            DataLoaderOptions.newOptions()
                .setBatchingEnabled(true)
                .setCachingEnabled(true)
                .setMaxBatchSize(100)
        );
        registry.register("commentLoader", commentLoader);
        
        // Posts by Author DataLoader
        DataLoader<String, List<Post>> postsByAuthorLoader = DataLoader.newDataLoader(
            authorIds -> {
                System.out.println("Batch loading posts by authors: " + authorIds);
                return postRepository.findByAuthorIds(authorIds);
            },
            DataLoaderOptions.newOptions()
                .setBatchingEnabled(true)
                .setCachingEnabled(true)
                .setMaxBatchSize(50)
        );
        registry.register("postsByAuthorLoader", postsByAuthorLoader);
        
        // Comments by Post DataLoader
        DataLoader<String, List<Comment>> commentsByPostLoader = DataLoader.newDataLoader(
            postIds -> {
                System.out.println("Batch loading comments by posts: " + postIds);
                return commentRepository.findByPostIds(postIds);
            },
            DataLoaderOptions.newOptions()
                .setBatchingEnabled(true)
                .setCachingEnabled(true)
                .setMaxBatchSize(50)
        );
        registry.register("commentsByPostLoader", commentsByPostLoader);
    }
    
    // Convenience methods for accessing DataLoaders
    @org.springframework.graphql.data.method.annotation.ContextValue
    private DataLoaderRegistry dataLoaderRegistry;
    
    public CompletableFuture<User> loadUser(String userId) {
        return dataLoaderRegistry.<String, User>getDataLoader("userLoader").load(userId);
    }
    
    public CompletableFuture<List<User>> loadUsers(List<String> userIds) {
        return dataLoaderRegistry.<String, User>getDataLoader("userLoader").loadMany(userIds);
    }
    
    public CompletableFuture<Post> loadPost(String postId) {
        return dataLoaderRegistry.<String, Post>getDataLoader("postLoader").load(postId);
    }
    
    public CompletableFuture<List<Post>> loadPosts(List<String> postIds) {
        return dataLoaderRegistry.<String, Post>getDataLoader("postLoader").loadMany(postIds);
    }
    
    public CompletableFuture<Comment> loadComment(String commentId) {
        return dataLoaderRegistry.<String, Comment>getDataLoader("commentLoader").load(commentId);
    }
    
    public CompletableFuture<List<Comment>> loadComments(List<String> commentIds) {
        return dataLoaderRegistry.<String, Comment>getDataLoader("commentLoader").loadMany(commentIds);
    }
    
    public CompletableFuture<List<Post>> loadPostsByAuthor(String authorId) {
        return dataLoaderRegistry.<String, List<Post>>getDataLoader("postsByAuthorLoader").load(authorId);
    }
    
    public CompletableFuture<List<Comment>> loadCommentsByPost(String postId) {
        return dataLoaderRegistry.<String, List<Comment>>getDataLoader("commentsByPostLoader").load(postId);
    }
}

// Optimized batch loading service
@Component
public class BatchDataService {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    
    public BatchDataService(UserRepository userRepository,
                           PostRepository postRepository,
                           CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }
    
    // Batch load users with optimized queries
    public CompletableFuture<List<User>> batchLoadUsers(List<String> userIds) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> uniqueIds = userIds.stream().collect(Collectors.toSet());
            List<User> users = userRepository.findAllById(uniqueIds);
            
            // Maintain order of original request
            return userIds.stream()
                .map(id -> users.stream()
                    .filter(user -> user.id().equals(id))
                    .findFirst()
                    .orElse(null))
                .collect(Collectors.toList());
        });
    }
    
    // Batch load posts with author information
    public CompletableFuture<List<Post>> batchLoadPostsWithAuthors(List<String> postIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<Post> posts = postRepository.findAllById(postIds);
            
            // Pre-load authors to avoid N+1 problem
            Set<String> authorIds = posts.stream()
                .map(Post::authorId)
                .collect(Collectors.toSet());
            
            if (!authorIds.isEmpty()) {
                List<User> authors = userRepository.findAllById(authorIds);
                // Authors are now cached for subsequent field resolution
            }
            
            return posts;
        });
    }
    
    // Batch load comments with full context
    public CompletableFuture<List<Comment>> batchLoadCommentsWithContext(List<String> commentIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<Comment> comments = commentRepository.findAllById(commentIds);
            
            // Pre-load related data
            Set<String> authorIds = comments.stream()
                .map(Comment::authorId)
                .collect(Collectors.toSet());
            
            Set<String> postIds = comments.stream()
                .map(Comment::postId)
                .collect(Collectors.toSet());
            
            // Batch load authors and posts
            CompletableFuture<List<User>> authorsTask = batchLoadUsers(List.copyOf(authorIds));
            CompletableFuture<List<Post>> postsTask = batchLoadPostsWithAuthors(List.copyOf(postIds));
            
            // Wait for pre-loading to complete
            CompletableFuture.allOf(authorsTask, postsTask).join();
            
            return comments;
        });
    }
}
```

## 3. Real-time Subscriptions

### WebSocket Subscription Implementation

```java
package com.javajourney.graphql.subscription;

import com.javajourney.graphql.model.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Controller
public class GraphQLSubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    public GraphQLSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }
    
    @SubscriptionMapping
    public Flux<Post> postAdded() {
        return subscriptionService.getPostAddedStream();
    }
    
    @SubscriptionMapping
    public Flux<Comment> commentAdded(@Argument String postId) {
        return subscriptionService.getCommentAddedStream(postId);
    }
    
    @SubscriptionMapping
    public Flux<UserStatus> userStatusChanged(@Argument String userId) {
        return subscriptionService.getUserStatusStream(userId);
    }
}

@Component
public class SubscriptionService {
    
    // Sinks for different subscription types
    private final Sinks.Many<Post> postAddedSink;
    private final Map<String, Sinks.Many<Comment>> commentSinksByPost;
    private final Map<String, Sinks.Many<UserStatus>> userStatusSinks;
    
    // Subscription management
    private final Map<String, SubscriptionInfo> activeSubscriptions;
    
    public SubscriptionService() {
        this.postAddedSink = Sinks.many().multicast().onBackpressureBuffer();
        this.commentSinksByPost = new ConcurrentHashMap<>();
        this.userStatusSinks = new ConcurrentHashMap<>();
        this.activeSubscriptions = new ConcurrentHashMap<>();
        
        // Start cleanup task
        startCleanupTask();
    }
    
    // Post subscriptions
    public Flux<Post> getPostAddedStream() {
        return postAddedSink.asFlux()
            .doOnSubscribe(subscription -> {
                String subscriptionId = generateSubscriptionId();
                activeSubscriptions.put(subscriptionId, 
                    new SubscriptionInfo(subscriptionId, "POST_ADDED", null, Instant.now()));
                System.out.println("New post subscription: " + subscriptionId);
            })
            .doOnCancel(() -> System.out.println("Post subscription cancelled"))
            .doOnError(error -> System.err.println("Post subscription error: " + error.getMessage()));
    }
    
    public void publishPostAdded(Post post) {
        System.out.println("Publishing new post: " + post.id());
        postAddedSink.tryEmitNext(post);
    }
    
    // Comment subscriptions
    public Flux<Comment> getCommentAddedStream(String postId) {
        return commentSinksByPost
            .computeIfAbsent(postId, key -> Sinks.many().multicast().onBackpressureBuffer())
            .asFlux()
            .doOnSubscribe(subscription -> {
                String subscriptionId = generateSubscriptionId();
                activeSubscriptions.put(subscriptionId, 
                    new SubscriptionInfo(subscriptionId, "COMMENT_ADDED", postId, Instant.now()));
                System.out.println("New comment subscription for post " + postId + ": " + subscriptionId);
            })
            .doOnCancel(() -> System.out.println("Comment subscription cancelled for post: " + postId))
            .doOnError(error -> System.err.println("Comment subscription error: " + error.getMessage()));
    }
    
    public void publishCommentAdded(Comment comment) {
        System.out.println("Publishing new comment for post: " + comment.postId());
        Sinks.Many<Comment> sink = commentSinksByPost.get(comment.postId());
        if (sink != null) {
            sink.tryEmitNext(comment);
        }
    }
    
    // User status subscriptions
    public Flux<UserStatus> getUserStatusStream(String userId) {
        return userStatusSinks
            .computeIfAbsent(userId, key -> Sinks.many().multicast().onBackpressureBuffer())
            .asFlux()
            .doOnSubscribe(subscription -> {
                String subscriptionId = generateSubscriptionId();
                activeSubscriptions.put(subscriptionId, 
                    new SubscriptionInfo(subscriptionId, "USER_STATUS", userId, Instant.now()));
                System.out.println("New user status subscription for user " + userId + ": " + subscriptionId);
            })
            .doOnCancel(() -> System.out.println("User status subscription cancelled for user: " + userId))
            .doOnError(error -> System.err.println("User status subscription error: " + error.getMessage()));
    }
    
    public void publishUserStatusChanged(String userId, UserStatus status) {
        System.out.println("Publishing status change for user " + userId + ": " + status);
        Sinks.Many<UserStatus> sink = userStatusSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(status);
        }
    }
    
    // Subscription management
    public Map<String, SubscriptionInfo> getActiveSubscriptions() {
        return Map.copyOf(activeSubscriptions);
    }
    
    public void cancelSubscription(String subscriptionId) {
        SubscriptionInfo info = activeSubscriptions.remove(subscriptionId);
        if (info != null) {
            System.out.println("Cancelled subscription: " + subscriptionId);
        }
    }
    
    private String generateSubscriptionId() {
        return "sub_" + System.nanoTime();
    }
    
    private void startCleanupTask() {
        // Clean up old subscriptions every 5 minutes
        Flux.interval(Duration.ofMinutes(5))
            .doOnNext(tick -> cleanupOldSubscriptions())
            .subscribe();
    }
    
    private void cleanupOldSubscriptions() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        
        activeSubscriptions.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getValue().createdAt().isBefore(cutoff);
            if (shouldRemove) {
                System.out.println("Cleaning up old subscription: " + entry.getKey());
            }
            return shouldRemove;
        });
        
        // Clean up empty sinks
        commentSinksByPost.entrySet().removeIf(entry -> 
            entry.getValue().currentSubscriberCount() == 0);
        
        userStatusSinks.entrySet().removeIf(entry -> 
            entry.getValue().currentSubscriberCount() == 0);
    }
    
    // Data classes
    public record SubscriptionInfo(
        String id,
        String type,
        String targetId,
        java.time.Instant createdAt
    ) {}
}

// WebSocket configuration for subscriptions
@Configuration
@EnableWebSocket
public class GraphQLWebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new GraphQLWebSocketHandler(), "/graphql-ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}

@Component
public class GraphQLWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> activeSessions;
    
    public GraphQLWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        System.out.println("WebSocket connection established: " + sessionId);
        
        // Send connection acknowledgment
        Map<String, Object> ack = Map.of(
            "type", "connection_ack",
            "payload", Map.of("message", "Connection established")
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        System.out.println("WebSocket connection closed: " + sessionId + ", status: " + status);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");
            
            switch (type) {
                case "start" -> handleSubscriptionStart(session, payload);
                case "stop" -> handleSubscriptionStop(session, payload);
                case "connection_terminate" -> session.close();
                default -> System.out.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error handling WebSocket message: " + e.getMessage());
            Map<String, Object> error = Map.of(
                "type", "error",
                "payload", Map.of("message", e.getMessage())
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        }
    }
    
    private void handleSubscriptionStart(WebSocketSession session, Map<String, Object> payload) {
        String id = (String) payload.get("id");
        @SuppressWarnings("unchecked")
        Map<String, Object> subscriptionPayload = (Map<String, Object>) payload.get("payload");
        
        System.out.println("Starting subscription: " + id + " for session: " + session.getId());
        
        // In a real implementation, you would:
        // 1. Parse the GraphQL subscription query
        // 2. Execute the subscription
        // 3. Stream results back to the client
        
        // For demo purposes, send acknowledgment
        try {
            Map<String, Object> ack = Map.of(
                "type", "data",
                "id", id,
                "payload", Map.of("data", Map.of("message", "Subscription started"))
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
        } catch (Exception e) {
            System.err.println("Error sending subscription start acknowledgment: " + e.getMessage());
        }
    }
    
    private void handleSubscriptionStop(WebSocketSession session, Map<String, Object> payload) {
        String id = (String) payload.get("id");
        System.out.println("Stopping subscription: " + id + " for session: " + session.getId());
        
        // Send completion message
        try {
            Map<String, Object> complete = Map.of(
                "type", "complete",
                "id", id
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(complete)));
        } catch (Exception e) {
            System.err.println("Error sending subscription stop acknowledgment: " + e.getMessage());
        }
    }
    
    public void broadcastToAll(Object data) {
        String message;
        try {
            message = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            System.err.println("Error serializing broadcast data: " + e.getMessage());
            return;
        }
        
        activeSessions.values().parallelStream().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting to session " + session.getId() + ": " + e.getMessage());
            }
        });
    }
}
```

## 4. Security and Authorization

### GraphQL Security Implementation

```java
package com.javajourney.graphql.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlConfigurer;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class GraphQLSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/graphql", "/graphiql/**", "/graphql-ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            .build();
    }
    
    @Bean
    public WebGraphQlConfigurer graphQLConfigurer() {
        return (builder, transport) -> {
            builder.queryComplexity(100)
                  .queryDepth(10);
        };
    }
    
    @Bean
    public Instrumentation graphQLInstrumentation() {
        return new ChainedInstrumentation(List.of(
            new MaxQueryComplexityInstrumentation(100),
            new MaxQueryDepthInstrumentation(15),
            new SecurityInstrumentation(),
            new PerformanceInstrumentation()
        ));
    }
}

// Custom security instrumentation
public class SecurityInstrumentation extends SimpleInstrumentation {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityInstrumentation.class);
    
    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(
            InstrumentationExecutionParameters parameters) {
        
        // Log all GraphQL operations for security monitoring
        String query = parameters.getQuery();
        String operationName = parameters.getOperationName();
        Map<String, Object> variables = parameters.getVariables();
        
        logger.info("GraphQL operation: {} - Query: {} - Variables: {}", 
            operationName, sanitizeQuery(query), sanitizeVariables(variables));
        
        return super.beginExecution(parameters);
    }
    
    @Override
    public DataFetcher<?> instrumentDataFetcher(
            DataFetcher<?> dataFetcher, 
            InstrumentationFieldFetchParameters parameters) {
        
        return environment -> {
            // Check rate limiting
            if (!checkRateLimit(environment)) {
                throw new GraphQLException("Rate limit exceeded");
            }
            
            // Check field-level permissions
            if (!checkFieldPermissions(environment)) {
                throw new GraphQLException("Access denied to field: " + 
                    environment.getField().getName());
            }
            
            return dataFetcher.get(environment);
        };
    }
    
    private boolean checkRateLimit(DataFetchingEnvironment environment) {
        // Implement rate limiting logic
        String userId = getCurrentUserId(environment);
        String fieldName = environment.getField().getName();
        
        // Simple rate limiting - 100 requests per minute per user per field
        String key = userId + ":" + fieldName;
        // In production, use Redis or similar for distributed rate limiting
        
        return true; // Simplified for demo
    }
    
    private boolean checkFieldPermissions(DataFetchingEnvironment environment) {
        String fieldName = environment.getField().getName();
        String userId = getCurrentUserId(environment);
        
        // Check if user has permission to access this field
        switch (fieldName) {
            case "email" -> {
                // Only allow users to see their own email or if they're admin
                return isCurrentUser(userId, environment) || isAdmin(userId);
            }
            case "deleteUser", "deletePost", "deleteComment" -> {
                // Only admins can delete
                return isAdmin(userId);
            }
            default -> {
                return true; // Allow by default
            }
        }
    }
    
    private String getCurrentUserId(DataFetchingEnvironment environment) {
        // Extract user ID from JWT token in GraphQL context
        return environment.getGraphQlContext().get("userId");
    }
    
    private boolean isCurrentUser(String userId, DataFetchingEnvironment environment) {
        // Check if the requested resource belongs to the current user
        Object source = environment.getSource();
        if (source instanceof User user) {
            return user.id().equals(userId);
        }
        return false;
    }
    
    private boolean isAdmin(String userId) {
        // Check if user has admin role
        // In production, query user roles from database or JWT claims
        return false; // Simplified for demo
    }
    
    private String sanitizeQuery(String query) {
        // Remove sensitive information from query for logging
        return query.replaceAll("password\\s*:\\s*\"[^\"]*\"", "password: \"***\"")
                   .replaceAll("token\\s*:\\s*\"[^\"]*\"", "token: \"***\"");
    }
    
    private Map<String, Object> sanitizeVariables(Map<String, Object> variables) {
        // Remove sensitive information from variables for logging
        Map<String, Object> sanitized = new HashMap<>(variables);
        sanitized.replaceAll((key, value) -> {
            if (key.toLowerCase().contains("password") || 
                key.toLowerCase().contains("token") ||
                key.toLowerCase().contains("secret")) {
                return "***";
            }
            return value;
        });
        return sanitized;
    }
}

// Performance monitoring instrumentation
public class PerformanceInstrumentation extends SimpleInstrumentation {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceInstrumentation.class);
    
    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(
            InstrumentationExecutionParameters parameters) {
        
        long startTime = System.currentTimeMillis();
        
        return new InstrumentationContext<ExecutionResult>() {
            @Override
            public void onCompleted(ExecutionResult result, Throwable t) {
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("GraphQL execution completed in {}ms - Operation: {} - Errors: {}", 
                    duration, 
                    parameters.getOperationName(),
                    result.getErrors().size());
                
                if (duration > 5000) {
                    logger.warn("Slow GraphQL query detected: {}ms - Operation: {}", 
                        duration, parameters.getOperationName());
                }
            }
        };
    }
    
    @Override
    public DataFetcher<?> instrumentDataFetcher(
            DataFetcher<?> dataFetcher, 
            InstrumentationFieldFetchParameters parameters) {
        
        return environment -> {
            long startTime = System.nanoTime();
            
            try {
                Object result = dataFetcher.get(environment);
                
                long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
                
                if (duration > 1000) {
                    logger.warn("Slow field resolution: {} took {}ms", 
                        environment.getField().getName(), duration);
                }
                
                return result;
            } catch (Exception e) {
                long duration = (System.nanoTime() - startTime) / 1_000_000;
                logger.error("Field resolution error: {} failed after {}ms - Error: {}", 
                    environment.getField().getName(), duration, e.getMessage());
                throw e;
            }
        };
    }
}

// Authorization annotations for resolvers
@Component
public class SecureGraphQLResolver {
    
    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<User> user(@Argument String id) {
        // Implementation
        return CompletableFuture.completedFuture(null);
    }
    
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<UserConnection> adminUsers(@Argument Integer first) {
        // Implementation for admin-only user listing
        return CompletableFuture.completedFuture(null);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('USER') and #input.authorId == authentication.name")
    public CompletableFuture<CreatePostPayload> createPost(@Argument CreatePostInput input) {
        // Users can only create posts for themselves
        return CompletableFuture.completedFuture(null);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN') or @postService.isPostOwner(#id, authentication.name)")
    public CompletableFuture<DeletePostPayload> deletePost(@Argument String id) {
        // Admins can delete any post, users can only delete their own
        return CompletableFuture.completedFuture(null);
    }
}
```

## Practice Exercises

### Exercise 1: Complete GraphQL API

Build a complete GraphQL API for a social media platform with users, posts, comments, and real-time features.

### Exercise 2: DataLoader Optimization

Implement advanced DataLoader patterns to eliminate N+1 queries in complex GraphQL schemas.

### Exercise 3: Real-time Subscriptions

Create a real-time chat application using GraphQL subscriptions with proper authentication and authorization.

### Exercise 4: GraphQL Federation

Implement GraphQL federation to combine multiple services into a unified schema.

## Key Takeaways

1. **Efficient Data Fetching**: DataLoaders solve the N+1 query problem and enable efficient batch loading
2. **Real-time Capabilities**: GraphQL subscriptions provide powerful real-time functionality
3. **Security First**: Implement query complexity analysis, rate limiting, and field-level authorization
4. **Performance Monitoring**: Use instrumentation to monitor query performance and identify bottlenecks
5. **Type Safety**: Strong schema definition enables better development experience and runtime safety

GraphQL provides a powerful alternative to REST APIs, offering flexible data fetching, real-time capabilities, and strong type safety for modern applications.