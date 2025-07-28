# Day 123: Platform Engineering - Infrastructure as Code, GitOps & Developer Experience

## Learning Objectives
- Build comprehensive Infrastructure as Code (IaC) frameworks with Terraform and Pulumi
- Implement GitOps workflows with ArgoCD and Flux for continuous deployment
- Create developer experience platforms with self-service capabilities
- Design platform engineering solutions for enterprise environments
- Integrate observability, security, and compliance into platform engineering workflows

## 1. Infrastructure as Code (IaC) Framework

### Enterprise Terraform Platform
```java
@Service
@Slf4j
public class TerraformOrchestrator {
    
    @Autowired
    private TerraformExecutor terraformExecutor;
    
    @Autowired
    private StateManagementService stateManagementService;
    
    @Autowired
    private PolicyEngineService policyEngineService;
    
    @Autowired
    private SecretManagementService secretManagementService;
    
    public class TerraformWorkspace {
        private final String workspaceId;
        private final TerraformConfiguration configuration;
        private final Map<String, String> variables;
        private final List<TerraformModule> modules;
        private final ExecutorService executorService;
        
        public TerraformWorkspace(String workspaceId, TerraformConfiguration configuration) {
            this.workspaceId = workspaceId;
            this.configuration = configuration;
            this.variables = new ConcurrentHashMap<>();
            this.modules = new ArrayList<>();
            this.executorService = Executors.newFixedThreadPool(4);
        }
        
        public CompletableFuture<TerraformPlanResult> plan() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Starting Terraform plan for workspace: {}", workspaceId);
                    
                    // Validate configuration
                    ValidationResult validationResult = validateConfiguration();
                    if (!validationResult.isValid()) {
                        throw new TerraformValidationException(
                            "Configuration validation failed: " + validationResult.getErrors()
                        );
                    }
                    
                    // Initialize workspace
                    initializeWorkspace();
                    
                    // Generate variable files
                    generateVariableFiles();
                    
                    // Execute terraform plan
                    TerraformCommandResult planResult = terraformExecutor.executePlan(
                        workspaceId, configuration.getWorkingDirectory()
                    );
                    
                    // Parse plan output
                    TerraformPlan plan = parsePlanOutput(planResult.getOutput());
                    
                    // Apply policy checks
                    PolicyCheckResult policyResult = policyEngineService.checkPlan(plan);
                    if (!policyResult.allPoliciesPassed()) {
                        throw new PolicyViolationException(
                            "Policy violations detected: " + policyResult.getViolations()
                        );
                    }
                    
                    // Estimate costs
                    CostEstimate costEstimate = calculateCostEstimate(plan);
                    
                    return TerraformPlanResult.builder()
                        .workspaceId(workspaceId)
                        .plan(plan)
                        .policyResult(policyResult)
                        .costEstimate(costEstimate)
                        .planOutput(planResult.getOutput())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Terraform plan failed for workspace: {}", workspaceId, e);
                    throw new TerraformException("Plan execution failed", e);
                }
            }, executorService);
        }
        
        public CompletableFuture<TerraformApplyResult> apply(String planId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Starting Terraform apply for workspace: {} with plan: {}", 
                        workspaceId, planId);
                    
                    // Acquire state lock
                    StateLock stateLock = stateManagementService.acquireLock(workspaceId);
                    
                    try {
                        // Pre-apply validations
                        performPreApplyValidations(planId);
                        
                        // Backup current state
                        StateBackup stateBackup = stateManagementService.createBackup(workspaceId);
                        
                        // Execute terraform apply
                        TerraformCommandResult applyResult = terraformExecutor.executeApply(
                            workspaceId, planId, configuration.getWorkingDirectory()
                        );
                        
                        // Parse apply output
                        TerraformState newState = parseApplyOutput(applyResult.getOutput());
                        
                        // Update state store
                        stateManagementService.updateState(workspaceId, newState);
                        
                        // Generate infrastructure documentation
                        generateInfrastructureDocumentation(newState);
                        
                        // Update resource inventory
                        updateResourceInventory(newState);
                        
                        return TerraformApplyResult.builder()
                            .workspaceId(workspaceId)
                            .planId(planId)
                            .state(newState)
                            .applyOutput(applyResult.getOutput())
                            .backupId(stateBackup.getBackupId())
                            .appliedAt(Instant.now())
                            .build();
                            
                    } finally {
                        // Release state lock
                        stateManagementService.releaseLock(stateLock);
                    }
                    
                } catch (Exception e) {
                    log.error("Terraform apply failed for workspace: {}", workspaceId, e);
                    // Attempt rollback
                    attemptRollback(planId, e);
                    throw new TerraformException("Apply execution failed", e);
                }
            }, executorService);
        }
        
        private void generateVariableFiles() throws IOException {
            // Generate terraform.tfvars
            Path tfvarsPath = Paths.get(configuration.getWorkingDirectory(), "terraform.tfvars");
            
            Properties tfvars = new Properties();
            variables.forEach(tfvars::setProperty);
            
            try (FileWriter writer = new FileWriter(tfvarsPath.toFile())) {
                tfvars.store(writer, "Generated by Platform Engineering Service");
            }
            
            // Generate variable definitions
            generateVariableDefinitions();
            
            // Generate provider configurations
            generateProviderConfigurations();
        }
        
        private void generateProviderConfigurations() throws IOException {
            Path providersPath = Paths.get(configuration.getWorkingDirectory(), "providers.tf");
            
            StringBuilder providers = new StringBuilder();
            
            // AWS Provider
            if (configuration.getProviders().contains(TerraformProvider.AWS)) {
                providers.append(generateAWSProviderConfig());
            }
            
            // Azure Provider
            if (configuration.getProviders().contains(TerraformProvider.AZURE)) {
                providers.append(generateAzureProviderConfig());
            }
            
            // GCP Provider
            if (configuration.getProviders().contains(TerraformProvider.GCP)) {
                providers.append(generateGCPProviderConfig());
            }
            
            // Kubernetes Provider
            if (configuration.getProviders().contains(TerraformProvider.KUBERNETES)) {
                providers.append(generateKubernetesProviderConfig());
            }
            
            Files.write(providersPath, providers.toString().getBytes());
        }
        
        private String generateAWSProviderConfig() {
            return """
                terraform {
                  required_providers {
                    aws = {
                      source  = "hashicorp/aws"
                      version = "~> 5.0"
                    }
                  }
                }
                
                provider "aws" {
                  region = var.aws_region
                  
                  default_tags {
                    tags = {
                      Environment   = var.environment
                      Project       = var.project_name
                      ManagedBy     = "terraform"
                      Workspace     = "%s"
                      CostCenter    = var.cost_center
                      Owner         = var.owner_email
                    }
                  }
                }
                
                """.formatted(workspaceId);
        }
    }
    
    @Component
    public static class TerraformModuleRegistry {
        
        @Value("${platform.terraform.module-registry-url}")
        private String moduleRegistryUrl;
        
        @Autowired
        private GitRepositoryService gitRepositoryService;
        
        public class ModuleDefinition {
            private final String name;
            private final String version;
            private final String source;
            private final Map<String, ModuleVariable> variables;
            private final Map<String, ModuleOutput> outputs;
            private final List<String> dependencies;
            
            public ModuleDefinition(String name, String version, String source) {
                this.name = name;
                this.version = version;
                this.source = source;
                this.variables = new HashMap<>();
                this.outputs = new HashMap<>();
                this.dependencies = new ArrayList<>();
            }
            
            public String generateModuleCall(Map<String, Object> variableValues) {
                StringBuilder moduleCall = new StringBuilder();
                
                moduleCall.append("module \"").append(name).append("\" {\n");
                moduleCall.append("  source  = \"").append(source).append("\"\n");
                moduleCall.append("  version = \"").append(version).append("\"\n\n");
                
                // Add variables
                variableValues.forEach((key, value) -> {
                    if (variables.containsKey(key)) {
                        moduleCall.append("  ").append(key).append(" = ");
                        if (value instanceof String) {
                            moduleCall.append("\"").append(value).append("\"");
                        } else {
                            moduleCall.append(value);
                        }
                        moduleCall.append("\n");
                    }
                });
                
                moduleCall.append("}\n");
                
                return moduleCall.toString();
            }
        }
        
        public ModuleDefinition getModule(String moduleName, String version) {
            try {
                // Fetch module definition from registry
                String moduleUrl = String.format("%s/%s/%s", moduleRegistryUrl, moduleName, version);
                ModuleMetadata metadata = fetchModuleMetadata(moduleUrl);
                
                // Clone module source
                String moduleSource = gitRepositoryService.cloneRepository(
                    metadata.getRepositoryUrl(), version
                );
                
                // Parse module configuration
                return parseModuleConfiguration(moduleName, version, moduleSource, metadata);
                
            } catch (Exception e) {
                log.error("Failed to fetch module: {} version: {}", moduleName, version, e);
                throw new ModuleRegistryException("Module fetch failed", e);
            }
        }
        
        private ModuleDefinition parseModuleConfiguration(String name, String version,
                String moduleSource, ModuleMetadata metadata) throws IOException {
            
            ModuleDefinition module = new ModuleDefinition(name, version, moduleSource);
            
            // Parse variables.tf
            Path variablesPath = Paths.get(moduleSource, "variables.tf");
            if (Files.exists(variablesPath)) {
                parseVariablesFile(variablesPath, module);
            }
            
            // Parse outputs.tf
            Path outputsPath = Paths.get(moduleSource, "outputs.tf");
            if (Files.exists(outputsPath)) {
                parseOutputsFile(outputsPath, module);
            }
            
            // Parse dependencies from module metadata
            metadata.getDependencies().forEach(module.getDependencies()::add);
            
            return module;
        }
        
        public List<ModuleDefinition> discoverModules(String pattern) {
            // Discover available modules from registry
            return gitRepositoryService.searchRepositories(pattern).stream()
                .filter(repo -> repo.getTags().contains("terraform-module"))
                .map(repo -> {
                    try {
                        return getModule(repo.getName(), repo.getLatestVersion());
                    } catch (Exception e) {
                        log.warn("Failed to load module: {}", repo.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }
    
    @Component
    public static class PulumiIntegration {
        
        @Autowired
        private PulumiStackService pulumiStackService;
        
        public class PulumiProgram {
            private final String programName;
            private final String language;
            private final Map<String, Object> configuration;
            private final PulumiStack stack;
            
            public PulumiProgram(String programName, String language) {
                this.programName = programName;
                this.language = language;
                this.configuration = new HashMap<>();
                this.stack = new PulumiStack(programName);
            }
            
            public CompletableFuture<PulumiPreviewResult> preview() {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // Generate Pulumi program
                        generatePulumiProgram();
                        
                        // Execute pulumi preview
                        PulumiCommandResult previewResult = pulumiStackService.executePreview(
                            programName, stack.getStackName()
                        );
                        
                        // Parse preview output
                        ResourceChanges changes = parsePreviewOutput(previewResult.getOutput());
                        
                        return PulumiPreviewResult.builder()
                            .programName(programName)
                            .changes(changes)
                            .previewOutput(previewResult.getOutput())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Pulumi preview failed for program: {}", programName, e);
                        throw new PulumiException("Preview execution failed", e);
                    }
                });
            }
            
            public CompletableFuture<PulumiUpResult> up() {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // Execute pulumi up
                        PulumiCommandResult upResult = pulumiStackService.executeUp(
                            programName, stack.getStackName()
                        );
                        
                        // Parse up output
                        StackState newState = parseUpOutput(upResult.getOutput());
                        
                        return PulumiUpResult.builder()
                            .programName(programName)
                            .state(newState)
                            .upOutput(upResult.getOutput())
                            .updatedAt(Instant.now())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Pulumi up failed for program: {}", programName, e);
                        throw new PulumiException("Up execution failed", e);
                    }
                });
            }
            
            private void generatePulumiProgram() throws IOException {
                switch (language.toLowerCase()) {
                    case "typescript":
                        generateTypeScriptProgram();
                        break;
                    case "python":
                        generatePythonProgram();
                        break;
                    case "java":
                        generateJavaProgram();
                        break;
                    case "go":
                        generateGoProgram();
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported language: " + language);
                }
            }
            
            private void generateJavaProgram() throws IOException {
                Path programPath = Paths.get("pulumi-programs", programName);
                Files.createDirectories(programPath);
                
                // Generate Pulumi.yaml
                generatePulumiYaml(programPath);
                
                // Generate pom.xml
                generatePomXml(programPath);
                
                // Generate main program
                generateMainJavaProgram(programPath);
            }
            
            private void generateMainJavaProgram(Path programPath) throws IOException {
                String javaProgram = """
                    package com.platform.pulumi;
                    
                    import com.pulumi.Pulumi;
                    import com.pulumi.aws.ec2.Instance;
                    import com.pulumi.aws.ec2.InstanceArgs;
                    import com.pulumi.aws.ec2.SecurityGroup;
                    import com.pulumi.aws.ec2.SecurityGroupArgs;
                    import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
                    import com.pulumi.aws.s3.Bucket;
                    import com.pulumi.aws.s3.BucketArgs;
                    import com.pulumi.core.Output;
                    
                    public class InfrastructureProgram {
                        public static void main(String[] args) {
                            Pulumi.run(ctx -> {
                                // Create S3 bucket
                                var bucket = new Bucket("platform-bucket", BucketArgs.builder()
                                    .bucket(String.format("platform-%s-bucket", ctx.getProject()))
                                    .versioning(BucketVersioningArgs.builder()
                                        .enabled(true)
                                        .build())
                                    .serverSideEncryptionConfiguration(BucketServerSideEncryptionConfigurationArgs.builder()
                                        .rules(BucketServerSideEncryptionConfigurationRuleArgs.builder()
                                            .applyServerSideEncryptionByDefault(
                                                BucketServerSideEncryptionConfigurationRuleApplyServerSideEncryptionByDefaultArgs.builder()
                                                    .sseAlgorithm("AES256")
                                                    .build())
                                            .build())
                                        .build())
                                    .publicAccessBlock(BucketPublicAccessBlockArgs.builder()
                                        .blockPublicAcls(true)
                                        .blockPublicPolicy(true)
                                        .ignorePublicAcls(true)
                                        .restrictPublicBuckets(true)
                                        .build())
                                    .tags(Map.of(
                                        "Environment", "production",
                                        "ManagedBy", "pulumi"
                                    ))
                                    .build());
                                
                                // Create security group
                                var securityGroup = new SecurityGroup("platform-sg", SecurityGroupArgs.builder()
                                    .description("Platform security group")
                                    .ingress(SecurityGroupIngressArgs.builder()
                                        .protocol("tcp")
                                        .fromPort(80)
                                        .toPort(80)
                                        .cidrBlocks("0.0.0.0/0")
                                        .build())
                                    .ingress(SecurityGroupIngressArgs.builder()
                                        .protocol("tcp")
                                        .fromPort(443)
                                        .toPort(443)
                                        .cidrBlocks("0.0.0.0/0")
                                        .build())
                                    .build());
                                
                                // Create EC2 instance
                                var instance = new Instance("platform-instance", InstanceArgs.builder()
                                    .instanceType("t3.medium")
                                    .ami("ami-0c02fb55956c7d316") // Amazon Linux 2
                                    .vpcSecurityGroupIds(securityGroup.id())
                                    .userData(generateUserData())
                                    .tags(Map.of(
                                        "Name", "Platform Instance",
                                        "Environment", "production"
                                    ))
                                    .build());
                                
                                // Export outputs
                                ctx.export("bucketName", bucket.bucket());
                                ctx.export("instanceId", instance.id());
                                ctx.export("instancePublicIp", instance.publicIp());
                            });
                        }
                        
                        private static String generateUserData() {
                            return "#!/bin/bash\\n" +
                                   "yum update -y\\n" +
                                   "yum install -y docker\\n" +
                                   "service docker start\\n" +
                                   "usermod -a -G docker ec2-user\\n";
                        }
                    }
                    """;
                
                Path javaFile = programPath.resolve("src/main/java/com/platform/pulumi/InfrastructureProgram.java");
                Files.createDirectories(javaFile.getParent());
                Files.write(javaFile, javaProgram.getBytes());
            }
        }
    }
}
```

## 2. GitOps Implementation

### ArgoCD Integration Platform
```java
@Service
@Slf4j
public class GitOpsOrchestrator {
    
    @Autowired
    private ArgoCDClient argoCDClient;
    
    @Autowired
    private FluxClient fluxClient;
    
    @Autowired
    private GitRepositoryService gitRepositoryService;
    
    @Autowired
    private KubernetesClient kubernetesClient;
    
    public class GitOpsApplication {
        private final String applicationName;
        private final GitOpsConfiguration configuration;
        private final ApplicationSpec applicationSpec;
        private final SyncPolicy syncPolicy;
        
        public GitOpsApplication(String applicationName, GitOpsConfiguration configuration) {
            this.applicationName = applicationName;
            this.configuration = configuration;
            this.applicationSpec = createApplicationSpec();
            this.syncPolicy = createSyncPolicy();
        }
        
        public CompletableFuture<ApplicationDeployResult> deploy() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Deploying GitOps application: {}", applicationName);
                    
                    // Validate Git repository
                    validateGitRepository();
                    
                    // Create ArgoCD application
                    Application application = createArgoCDApplication();
                    
                    // Apply application to ArgoCD
                    Application createdApp = argoCDClient.createApplication(application);
                    
                    // Monitor deployment progress
                    DeploymentProgress progress = monitorDeployment(createdApp);
                    
                    // Verify deployment health
                    HealthStatus healthStatus = verifyApplicationHealth(createdApp);
                    
                    return ApplicationDeployResult.builder()
                        .applicationName(applicationName)
                        .deploymentProgress(progress)
                        .healthStatus(healthStatus)
                        .deployedAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("GitOps application deployment failed: {}", applicationName, e);
                    throw new GitOpsDeploymentException("Deployment failed", e);
                }
            });
        }
        
        private Application createArgoCDApplication() {
            return Application.builder()
                .metadata(ApplicationMetadata.builder()
                    .name(applicationName)
                    .namespace("argocd")
                    .labels(Map.of(
                        "app.kubernetes.io/name", applicationName,
                        "app.kubernetes.io/managed-by", "platform-engineering"
                    ))
                    .build())
                .spec(ApplicationSpec.builder()
                    .project("default")
                    .source(ApplicationSource.builder()
                        .repoURL(configuration.getRepositoryUrl())
                        .targetRevision(configuration.getTargetRevision())
                        .path(configuration.getPath())
                        .helm(HelmSource.builder()
                            .valueFiles(configuration.getValueFiles())
                            .parameters(configuration.getHelmParameters())
                            .build())
                        .build())
                    .destination(ApplicationDestination.builder()
                        .server(configuration.getDestinationServer())
                        .namespace(configuration.getDestinationNamespace())
                        .build())
                    .syncPolicy(SyncPolicy.builder()
                        .automated(AutomatedSyncPolicy.builder()
                            .prune(true)
                            .selfHeal(true)
                            .allowEmpty(false)
                            .build())
                        .syncOptions(Arrays.asList(
                            "Validate=true",
                            "CreateNamespace=true",
                            "PrunePropagationPolicy=foreground"
                        ))
                        .retry(RetryPolicy.builder()
                            .limit(5)
                            .backoff(Backoff.builder()
                                .duration("5s")
                                .factor(2)
                                .maxDuration("3m")
                                .build())
                            .build())
                        .build())
                    .build())
                .build();
        }
        
        private DeploymentProgress monitorDeployment(Application application) {
            DeploymentProgress progress = new DeploymentProgress(applicationName);
            
            try (ApplicationWatcher watcher = argoCDClient.watchApplication(applicationName)) {
                watcher.onUpdate(updatedApp -> {
                    ApplicationStatus status = updatedApp.getStatus();
                    
                    progress.updateStatus(status.getSync().getStatus());
                    progress.updateHealth(status.getHealth().getStatus());
                    
                    // Check for sync completion
                    if (status.getSync().getStatus() == SyncStatus.SYNCED) {
                        if (status.getHealth().getStatus() == HealthStatus.HEALTHY) {
                            progress.markCompleted();
                        } else if (status.getHealth().getStatus() == HealthStatus.DEGRADED) {
                            progress.markDegraded();
                        }
                    }
                    
                    // Handle sync errors
                    if (status.getSync().getStatus() == SyncStatus.OUT_OF_SYNC) {
                        progress.addError("Application out of sync");
                    }
                });
                
                // Wait for deployment completion
                progress.waitForCompletion(Duration.ofMinutes(15));
            }
            
            return progress;
        }
        
        public CompletableFuture<RollbackResult> rollback(String targetRevision) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Rolling back application {} to revision: {}", 
                        applicationName, targetRevision);
                    
                    // Create rollback operation
                    Operation rollbackOp = argoCDClient.rollbackApplication(
                        applicationName, targetRevision
                    );
                    
                    // Monitor rollback progress
                    OperationProgress rollbackProgress = monitorOperation(rollbackOp);
                    
                    return RollbackResult.builder()
                        .applicationName(applicationName)
                        .targetRevision(targetRevision)
                        .rollbackProgress(rollbackProgress)
                        .rolledBackAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Rollback failed for application: {}", applicationName, e);
                    throw new GitOpsRollbackException("Rollback failed", e);
                }
            });
        }
    }
    
    @Component
    public static class FluxIntegration {
        
        @Autowired
        private FluxClient fluxClient;
        
        public class FluxKustomization {
            private final String name;
            private final String namespace;
            private final KustomizationSpec spec;
            
            public FluxKustomization(String name, String namespace, KustomizationSpec spec) {
                this.name = name;
                this.namespace = namespace;
                this.spec = spec;
            }
            
            public CompletableFuture<KustomizationResult> deploy() {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // Create Flux Kustomization
                        Kustomization kustomization = Kustomization.builder()
                            .metadata(ObjectMeta.builder()
                                .name(name)
                                .namespace(namespace)
                                .build())
                            .spec(spec)
                            .build();
                        
                        // Apply to cluster
                        Kustomization createdKustomization = fluxClient.createKustomization(kustomization);
                        
                        // Monitor reconciliation
                        ReconciliationStatus status = monitorReconciliation(createdKustomization);
                        
                        return KustomizationResult.builder()
                            .name(name)
                            .reconciliationStatus(status)
                            .deployedAt(Instant.now())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Flux Kustomization deployment failed: {}", name, e);
                        throw new FluxDeploymentException("Deployment failed", e);
                    }
                });
            }
            
            private ReconciliationStatus monitorReconciliation(Kustomization kustomization) {
                ReconciliationStatus status = new ReconciliationStatus();
                
                try (KustomizationWatcher watcher = fluxClient.watchKustomization(name, namespace)) {
                    watcher.onUpdate(updatedKustomization -> {
                        KustomizationStatus kustomizationStatus = updatedKustomization.getStatus();
                        
                        status.updateLastReconcileTime(kustomizationStatus.getLastReconcileTime());
                        status.updateConditions(kustomizationStatus.getConditions());
                        
                        // Check for ready condition
                        Optional<Condition> readyCondition = kustomizationStatus.getConditions()
                            .stream()
                            .filter(condition -> "Ready".equals(condition.getType()))
                            .findFirst();
                        
                        if (readyCondition.isPresent()) {
                            if ("True".equals(readyCondition.get().getStatus())) {
                                status.markReady();
                            } else {
                                status.markNotReady(readyCondition.get().getMessage());
                            }
                        }
                    });
                    
                    // Wait for reconciliation
                    status.waitForReady(Duration.ofMinutes(10));
                }
                
                return status;
            }
        }
        
        public class FluxHelmRelease {
            private final String name;
            private final String namespace;
            private final HelmReleaseSpec spec;
            
            public FluxHelmRelease(String name, String namespace, HelmReleaseSpec spec) {
                this.name = name;
                this.namespace = namespace;
                this.spec = spec;
            }
            
            public CompletableFuture<HelmReleaseResult> deploy() {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // Create Flux HelmRelease
                        HelmRelease helmRelease = HelmRelease.builder()
                            .metadata(ObjectMeta.builder()
                                .name(name)
                                .namespace(namespace)
                                .build())
                            .spec(spec)
                            .build();
                        
                        // Apply to cluster
                        HelmRelease createdRelease = fluxClient.createHelmRelease(helmRelease);
                        
                        // Monitor Helm deployment
                        HelmDeploymentStatus deploymentStatus = monitorHelmDeployment(createdRelease);
                        
                        return HelmReleaseResult.builder()
                            .name(name)
                            .deploymentStatus(deploymentStatus)
                            .deployedAt(Instant.now())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Flux HelmRelease deployment failed: {}", name, e);
                        throw new FluxHelmException("Helm deployment failed", e);
                    }
                });
            }
        }
    }
    
    @Component
    public static class GitOpsSecurityScanner {
        
        @Autowired
        private PolicyEngineService policyEngineService;
        
        @Autowired
        private VulnerabilityScanner vulnerabilityScanner;
        
        public CompletableFuture<SecurityScanResult> scanManifests(String repositoryUrl, 
                String branch, String path) {
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Clone repository
                    String localPath = gitRepositoryService.cloneRepository(repositoryUrl, branch);
                    
                    // Scan Kubernetes manifests
                    List<Path> manifestFiles = findKubernetesManifests(localPath, path);
                    
                    List<SecurityFinding> findings = new ArrayList<>();
                    
                    // Policy validation
                    for (Path manifestFile : manifestFiles) {
                        PolicyScanResult policyResult = policyEngineService.scanManifest(manifestFile);
                        findings.addAll(policyResult.getFindings());
                    }
                    
                    // Vulnerability scanning
                    for (Path manifestFile : manifestFiles) {
                        VulnerabilityScanResult vulnResult = vulnerabilityScanner.scanManifest(manifestFile);
                        findings.addAll(vulnResult.getVulnerabilities());
                    }
                    
                    // Security best practices check
                    SecurityBestPracticesResult bestPracticesResult = 
                        checkSecurityBestPractices(manifestFiles);
                    findings.addAll(bestPracticesResult.getFindings());
                    
                    return SecurityScanResult.builder()
                        .repositoryUrl(repositoryUrl)
                        .branch(branch)
                        .path(path)
                        .findings(findings)
                        .scannedAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Security scan failed for repository: {}", repositoryUrl, e);
                    throw new SecurityScanException("Security scan failed", e);
                }
            });
        }
        
        private SecurityBestPracticesResult checkSecurityBestPractices(List<Path> manifestFiles) {
            List<SecurityFinding> findings = new ArrayList<>();
            
            for (Path manifestFile : manifestFiles) {
                try {
                    // Parse Kubernetes manifest
                    Map<String, Object> manifest = yamlMapper.readValue(manifestFile.toFile(), Map.class);
                    
                    // Check for security best practices
                    checkRunAsNonRoot(manifest, findings);
                    checkReadOnlyRootFilesystem(manifest, findings);
                    checkResourceLimits(manifest, findings);
                    checkSecurityContext(manifest, findings);
                    checkImagePullPolicy(manifest, findings);
                    checkNetworkPolicies(manifest, findings);
                    
                } catch (Exception e) {
                    log.warn("Failed to parse manifest file: {}", manifestFile, e);
                }
            }
            
            return SecurityBestPracticesResult.builder()
                .findings(findings)
                .build();
        }
        
        private void checkRunAsNonRoot(Map<String, Object> manifest, List<SecurityFinding> findings) {
            String kind = (String) manifest.get("kind");
            
            if (Arrays.asList("Deployment", "StatefulSet", "DaemonSet", "Pod").contains(kind)) {
                Map<String, Object> spec = getNestedMap(manifest, "spec");
                Map<String, Object> template = getNestedMap(spec, "template");
                Map<String, Object> podSpec = getNestedMap(template, "spec");
                
                if (podSpec == null && "Pod".equals(kind)) {
                    podSpec = spec;
                }
                
                if (podSpec != null) {
                    Map<String, Object> securityContext = getNestedMap(podSpec, "securityContext");
                    
                    if (securityContext == null || 
                        !Boolean.TRUE.equals(securityContext.get("runAsNonRoot"))) {
                        
                        findings.add(SecurityFinding.builder()
                            .type(SecurityFindingType.SECURITY_BEST_PRACTICE)
                            .severity(Severity.MEDIUM)
                            .title("Container should run as non-root user")
                            .description("Container is not configured to run as non-root user")
                            .resource(kind + "/" + getMetadataName(manifest))
                            .recommendation("Set securityContext.runAsNonRoot to true")
                            .build());
                    }
                }
            }
        }
        
        private void checkResourceLimits(Map<String, Object> manifest, List<SecurityFinding> findings) {
            String kind = (String) manifest.get("kind");
            
            if (Arrays.asList("Deployment", "StatefulSet", "DaemonSet").contains(kind)) {
                Map<String, Object> spec = getNestedMap(manifest, "spec");
                Map<String, Object> template = getNestedMap(spec, "template");
                Map<String, Object> podSpec = getNestedMap(template, "spec");
                List<Map<String, Object>> containers = getNestedList(podSpec, "containers");
                
                if (containers != null) {
                    for (Map<String, Object> container : containers) {
                        Map<String, Object> resources = getNestedMap(container, "resources");
                        
                        if (resources == null || 
                            !resources.containsKey("limits") || 
                            !resources.containsKey("requests")) {
                            
                            findings.add(SecurityFinding.builder()
                                .type(SecurityFindingType.RESOURCE_MANAGEMENT)
                                .severity(Severity.MEDIUM)
                                .title("Container missing resource limits")
                                .description("Container does not have resource limits configured")
                                .resource(kind + "/" + getMetadataName(manifest))
                                .recommendation("Configure CPU and memory limits and requests")
                                .build());
                        }
                    }
                }
            }
        }
    }
}
```

## 3. Developer Experience Platform

### Self-Service Development Platform
```java
@Service
@Slf4j
public class DeveloperExperiencePlatform {
    
    @Autowired
    private ProjectTemplateService projectTemplateService;
    
    @Autowired
    private EnvironmentProvisioningService environmentProvisioningService;
    
    @Autowired
    private CI_CD_PipelineService ciCdPipelineService;
    
    @Autowired
    private DeveloperPortalService developerPortalService;
    
    public class SelfServicePortal {
        
        public CompletableFuture<ProjectCreationResult> createProject(ProjectCreationRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Creating new project: {}", request.getProjectName());
                    
                    // Validate project request
                    validateProjectRequest(request);
                    
                    // Create project from template
                    ProjectTemplate template = projectTemplateService.getTemplate(
                        request.getTemplateId()
                    );
                    
                    Project project = generateProjectFromTemplate(template, request);
                    
                    // Create Git repository
                    GitRepository repository = createGitRepository(project);
                    
                    // Setup development environment
                    DevelopmentEnvironment devEnvironment = setupDevelopmentEnvironment(project);
                    
                    // Create CI/CD pipeline
                    Pipeline pipeline = createCiCdPipeline(project, repository);
                    
                    // Setup monitoring and observability
                    MonitoringStack monitoring = setupMonitoring(project);
                    
                    // Generate project documentation
                    generateProjectDocumentation(project, repository);
                    
                    // Register in developer portal
                    developerPortalService.registerProject(project);
                    
                    return ProjectCreationResult.builder()
                        .projectId(project.getId())
                        .projectName(project.getName())
                        .repositoryUrl(repository.getUrl())
                        .developmentEnvironment(devEnvironment)
                        .pipeline(pipeline)
                        .monitoring(monitoring)
                        .createdAt(Instant.now())
                        .build();
                        
                } catch (Exception e) {
                    log.error("Project creation failed: {}", request.getProjectName(), e);
                    throw new ProjectCreationException("Project creation failed", e);
                }
            });
        }
        
        private Project generateProjectFromTemplate(ProjectTemplate template, 
                ProjectCreationRequest request) {
            
            Project project = Project.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getProjectName())
                .description(request.getDescription())
                .templateId(template.getId())
                .owner(request.getOwner())
                .team(request.getTeam())
                .language(template.getLanguage())
                .framework(template.getFramework())
                .build();
            
            // Apply template customizations
            for (TemplateCustomization customization : request.getCustomizations()) {
                project = applyCustomization(project, customization);
            }
            
            return project;
        }
        
        private GitRepository createGitRepository(Project project) {
            // Create repository structure
            RepositoryStructure structure = RepositoryStructure.builder()
                .addDirectory("src/main/java")
                .addDirectory("src/test/java")
                .addDirectory("src/main/resources")
                .addDirectory(".github/workflows")
                .addDirectory("infrastructure")
                .addDirectory("docs")
                .addFile(".gitignore", generateGitIgnore(project))
                .addFile("README.md", generateReadme(project))
                .addFile("pom.xml", generatePomXml(project))
                .addFile("Dockerfile", generateDockerfile(project))
                .addFile(".github/workflows/ci.yml", generateCiWorkflow(project))
                .build();
            
            // Create repository
            GitRepository repository = gitRepositoryService.createRepository(
                project.getName(), structure
            );
            
            // Setup branch protection rules
            setupBranchProtection(repository);
            
            return repository;
        }
        
        private DevelopmentEnvironment setupDevelopmentEnvironment(Project project) {
            EnvironmentSpec environmentSpec = EnvironmentSpec.builder()
                .name(project.getName() + "-dev")
                .type(EnvironmentType.DEVELOPMENT)
                .resources(ResourceRequirements.builder()
                    .cpu("2000m")
                    .memory("4Gi")
                    .storage("20Gi")
                    .build())
                .services(Arrays.asList(
                    ServiceSpec.builder()
                        .name("application")
                        .image(project.getDockerImage())
                        .ports(Arrays.asList(8080))
                        .build(),
                    ServiceSpec.builder()
                        .name("database")
                        .image("postgres:13")
                        .ports(Arrays.asList(5432))
                        .environment(Map.of(
                            "POSTGRES_DB", project.getName(),
                            "POSTGRES_USER", "developer",
                            "POSTGRES_PASSWORD", generateSecurePassword()
                        ))
                        .build()
                ))
                .build();
            
            return environmentProvisioningService.createEnvironment(environmentSpec);
        }
        
        private Pipeline createCiCdPipeline(Project project, GitRepository repository) {
            PipelineSpec pipelineSpec = PipelineSpec.builder()
                .name(project.getName() + "-pipeline")
                .repository(repository.getUrl())
                .stages(Arrays.asList(
                    createBuildStage(project),
                    createTestStage(project),
                    createSecurityScanStage(project),
                    createDockerBuildStage(project),
                    createDeploymentStage(project)
                ))
                .triggers(Arrays.asList(
                    PipelineTrigger.builder()
                        .type(TriggerType.PUSH)
                        .branches(Arrays.asList("main", "develop"))
                        .build(),
                    PipelineTrigger.builder()
                        .type(TriggerType.PULL_REQUEST)
                        .branches(Arrays.asList("main"))
                        .build()
                ))
                .build();
            
            return ciCdPipelineService.createPipeline(pipelineSpec);
        }
        
        private PipelineStage createBuildStage(Project project) {
            return PipelineStage.builder()
                .name("build")
                .type(StageType.BUILD)
                .steps(Arrays.asList(
                    PipelineStep.builder()
                        .name("checkout")
                        .action("checkout")
                        .build(),
                    PipelineStep.builder()
                        .name("setup-java")
                        .action("setup-java")
                        .parameters(Map.of("java-version", "17"))
                        .build(),
                    PipelineStep.builder()
                        .name("build")
                        .action("run")
                        .parameters(Map.of("run", "./mvnw clean compile"))
                        .build()
                ))
                .build();
        }
        
        private PipelineStage createTestStage(Project project) {
            return PipelineStage.builder()
                .name("test")
                .type(StageType.TEST)
                .steps(Arrays.asList(
                    PipelineStep.builder()
                        .name("unit-tests")
                        .action("run")
                        .parameters(Map.of("run", "./mvnw test"))
                        .build(),
                    PipelineStep.builder()
                        .name("integration-tests")
                        .action("run")
                        .parameters(Map.of("run", "./mvnw verify"))
                        .build(),
                    PipelineStep.builder()
                        .name("test-report")
                        .action("publish-test-results")
                        .parameters(Map.of("test-results", "target/surefire-reports/*.xml"))
                        .build()
                ))
                .build();
        }
        
        private MonitoringStack setupMonitoring(Project project) {
            MonitoringConfiguration config = MonitoringConfiguration.builder()
                .projectName(project.getName())
                .metrics(MetricsConfiguration.builder()
                    .enabled(true)
                    .namespace(project.getName())
                    .labels(Map.of(
                        "project", project.getName(),
                        "team", project.getTeam(),
                        "environment", "development"
                    ))
                    .build())
                .logging(LoggingConfiguration.builder()
                    .enabled(true)
                    .logLevel("INFO")
                    .structured(true)
                    .build())
                .tracing(TracingConfiguration.builder()
                    .enabled(true)
                    .samplingRate(0.1)
                    .build())
                .alerting(AlertingConfiguration.builder()
                    .enabled(true)
                    .contactPoints(Arrays.asList(project.getOwner().getEmail()))
                    .build())
                .build();
            
            return monitoringService.createMonitoringStack(config);
        }
    }
    
    @Component
    public static class DeveloperPortalService {
        
        @Autowired
        private ServiceCatalogService serviceCatalogService;
        
        @Autowired
        private TechDocsService techDocsService;
        
        @Autowired
        private ScaffolderService scaffolderService;
        
        public class BackstageIntegration {
            
            public void registerProject(Project project) {
                // Create catalog-info.yaml
                CatalogEntity catalogEntity = CatalogEntity.builder()
                    .apiVersion("backstage.io/v1alpha1")
                    .kind("Component")
                    .metadata(EntityMetadata.builder()
                        .name(project.getName())
                        .description(project.getDescription())
                        .labels(Map.of(
                            "team", project.getTeam(),
                            "language", project.getLanguage(),
                            "framework", project.getFramework()
                        ))
                        .annotations(Map.of(
                            "github.com/project-slug", project.getRepositoryUrl()
                        ))
                        .build())
                    .spec(ComponentSpec.builder()
                        .type("service")
                        .lifecycle("production")
                        .owner(project.getTeam())
                        .build())
                    .build();
                
                serviceCatalogService.registerEntity(catalogEntity);
                
                // Generate API documentation
                generateApiDocumentation(project);
                
                // Create tech docs
                createTechDocs(project);
            }
            
            private void generateApiDocumentation(Project project) {
                if (project.hasRestApi()) {
                    // Generate OpenAPI specification
                    OpenAPISpec openApiSpec = generateOpenAPISpec(project);
                    
                    CatalogEntity apiEntity = CatalogEntity.builder()
                        .apiVersion("backstage.io/v1alpha1")
                        .kind("API")
                        .metadata(EntityMetadata.builder()
                            .name(project.getName() + "-api")
                            .description("API for " + project.getName())
                            .build())
                        .spec(APISpec.builder()
                            .type("openapi")
                            .lifecycle("production")
                            .owner(project.getTeam())
                            .definition(openApiSpec)
                            .build())
                        .build();
                    
                    serviceCatalogService.registerEntity(apiEntity);
                }
            }
            
            private void createTechDocs(Project project) {
                TechDocsConfiguration techDocsConfig = TechDocsConfiguration.builder()
                    .projectName(project.getName())
                    .repositoryUrl(project.getRepositoryUrl())
                    .docsDir("docs")
                    .mkdocsConfig(generateMkdocsConfig(project))
                    .build();
                
                techDocsService.generateTechDocs(techDocsConfig);
            }
            
            private MkdocsConfig generateMkdocsConfig(Project project) {
                return MkdocsConfig.builder()
                    .siteName(project.getName() + " Documentation")
                    .repoUrl(project.getRepositoryUrl())
                    .nav(Arrays.asList(
                        NavItem.of("Home", "index.md"),
                        NavItem.of("Getting Started", "getting-started.md"),
                        NavItem.of("API Reference", "api-reference.md"),
                        NavItem.of("Architecture", "architecture.md"),
                        NavItem.of("Deployment", "deployment.md")
                    ))
                    .theme(ThemeConfig.builder()
                        .name("material")
                        .palette(PaletteConfig.builder()
                            .primary("blue")
                            .accent("blue")
                            .build())
                        .build())
                    .plugins(Arrays.asList("search", "techdocs-core"))
                    .build();
            }
        }
        
        public class EnvironmentCatalog {
            
            public List<EnvironmentInfo> listAvailableEnvironments(String team) {
                return environmentProvisioningService.getAvailableEnvironments(team)
                    .stream()
                    .map(env -> EnvironmentInfo.builder()
                        .name(env.getName())
                        .type(env.getType())
                        .status(env.getStatus())
                        .resources(env.getResources())
                        .createdAt(env.getCreatedAt())
                        .build())
                    .collect(Collectors.toList());
            }
            
            public CompletableFuture<EnvironmentCreationResult> requestEnvironment(
                    EnvironmentRequest request) {
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // Validate request
                        validateEnvironmentRequest(request);
                        
                        // Check resource quotas
                        ResourceQuota quota = checkResourceQuota(request.getTeam());
                        if (!quota.canAccommodate(request.getResourceRequirements())) {
                            throw new ResourceQuotaExceededException(
                                "Insufficient resource quota for request"
                            );
                        }
                        
                        // Create environment
                        EnvironmentSpec environmentSpec = createEnvironmentSpec(request);
                        DevelopmentEnvironment environment = 
                            environmentProvisioningService.createEnvironment(environmentSpec);
                        
                        // Setup access controls
                        setupEnvironmentAccess(environment, request);
                        
                        // Register in catalog
                        registerEnvironmentInCatalog(environment);
                        
                        return EnvironmentCreationResult.builder()
                            .environmentId(environment.getId())
                            .name(environment.getName())
                            .endpoints(environment.getEndpoints())
                            .credentials(environment.getCredentials())
                            .createdAt(Instant.now())
                            .build();
                            
                    } catch (Exception e) {
                        log.error("Environment creation failed: {}", request.getName(), e);
                        throw new EnvironmentCreationException("Environment creation failed", e);
                    }
                });
            }
        }
    }
    
    @Component
    public static class DeveloperProductivityMetrics {
        
        @Autowired
        private MetricsCollector metricsCollector;
        
        @Autowired
        private AnalyticsService analyticsService;
        
        @Scheduled(fixedRate = 300000) // Every 5 minutes
        public void collectDeveloperMetrics() {
            try {
                // Lead time metrics
                collectLeadTimeMetrics();
                
                // Deployment frequency
                collectDeploymentFrequencyMetrics();
                
                // Mean time to recovery
                collectMTTRMetrics();
                
                // Change failure rate
                collectChangeFailureRateMetrics();
                
                // Developer satisfaction
                collectDeveloperSatisfactionMetrics();
                
            } catch (Exception e) {
                log.error("Failed to collect developer metrics", e);
            }
        }
        
        private void collectLeadTimeMetrics() {
            // Collect lead time from commit to production
            List<Deployment> recentDeployments = deploymentService.getRecentDeployments(
                Duration.ofDays(7)
            );
            
            for (Deployment deployment : recentDeployments) {
                Duration leadTime = Duration.between(
                    deployment.getFirstCommitTime(),
                    deployment.getProductionDeploymentTime()
                );
                
                metricsCollector.recordGauge(
                    "developer.lead_time.seconds",
                    leadTime.getSeconds(),
                    Tags.of(
                        "project", deployment.getProjectName(),
                        "team", deployment.getTeam()
                    )
                );
            }
        }
        
        private void collectDeploymentFrequencyMetrics() {
            // Count deployments per day/week
            Map<String, Long> dailyDeployments = deploymentService.getDeploymentCounts(
                Duration.ofDays(30),
                TimeUnit.DAYS
            );
            
            dailyDeployments.forEach((date, count) -> {
                metricsCollector.recordCounter(
                    "developer.deployment_frequency.daily",
                    count,
                    Tags.of("date", date)
                );
            });
        }
        
        private void collectMTTRMetrics() {
            // Mean Time To Recovery from incidents
            List<Incident> resolvedIncidents = incidentService.getResolvedIncidents(
                Duration.ofDays(30)
            );
            
            for (Incident incident : resolvedIncidents) {
                Duration mttr = Duration.between(
                    incident.getCreatedAt(),
                    incident.getResolvedAt()
                );
                
                metricsCollector.recordGauge(
                    "developer.mttr.minutes",
                    mttr.toMinutes(),
                    Tags.of(
                        "severity", incident.getSeverity().toString(),
                        "team", incident.getOwningTeam()
                    )
                );
            }
        }
        
        public DeveloperProductivityReport generateProductivityReport(String team, 
                Duration period) {
            
            // Collect metrics for the period
            DeveloperMetrics metrics = analyticsService.aggregateMetrics(team, period);
            
            // Calculate DORA metrics
            DORAMetrics doraMetrics = DORAMetrics.builder()
                .deploymentFrequency(metrics.getAverageDeploymentFrequency())
                .leadTimeForChanges(metrics.getAverageLeadTime())
                .changeFailureRate(metrics.getChangeFailureRate())
                .timeToRestore(metrics.getAverageMTTR())
                .build();
            
            // Generate recommendations
            List<ProductivityRecommendation> recommendations = 
                generateProductivityRecommendations(doraMetrics, metrics);
            
            return DeveloperProductivityReport.builder()
                .team(team)
                .period(period)
                .doraMetrics(doraMetrics)
                .developerMetrics(metrics)
                .recommendations(recommendations)
                .generatedAt(Instant.now())
                .build();
        }
        
        private List<ProductivityRecommendation> generateProductivityRecommendations(
                DORAMetrics doraMetrics, DeveloperMetrics metrics) {
            
            List<ProductivityRecommendation> recommendations = new ArrayList<>();
            
            // Lead time recommendations
            if (doraMetrics.getLeadTimeForChanges().toDays() > 7) {
                recommendations.add(ProductivityRecommendation.builder()
                    .category(RecommendationCategory.LEAD_TIME)
                    .priority(Priority.HIGH)
                    .title("Reduce Lead Time")
                    .description("Lead time is above industry average (7 days)")
                    .actions(Arrays.asList(
                        "Implement feature flags for faster releases",
                        "Reduce batch size of changes",
                        "Automate manual testing processes"
                    ))
                    .build());
            }
            
            // Deployment frequency recommendations
            if (doraMetrics.getDeploymentFrequency() < 1.0) { // Less than daily
                recommendations.add(ProductivityRecommendation.builder()
                    .category(RecommendationCategory.DEPLOYMENT_FREQUENCY)
                    .priority(Priority.MEDIUM)
                    .title("Increase Deployment Frequency")
                    .description("Deployment frequency is below daily")
                    .actions(Arrays.asList(
                        "Implement continuous deployment",
                        "Reduce deployment complexity",
                        "Improve automated testing coverage"
                    ))
                    .build());
            }
            
            return recommendations;
        }
    }
}
```

## 4. Platform Engineering Observability

### Comprehensive Monitoring and Alerting
```java
@Service
@Slf4j
public class PlatformObservabilityService {
    
    @Autowired
    private PrometheusClient prometheusClient;
    
    @Autowired
    private GrafanaClient grafanaClient;
    
    @Autowired
    private JaegerClient jaegerClient;
    
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    
    @Component
    public static class InfrastructureMonitoring {
        
        public void setupInfrastructureMonitoring(InfrastructureComponent component) {
            switch (component.getType()) {
                case KUBERNETES_CLUSTER:
                    setupKubernetesMonitoring(component);
                    break;
                case DATABASE:
                    setupDatabaseMonitoring(component);
                    break;
                case MESSAGE_QUEUE:
                    setupMessageQueueMonitoring(component);
                    break;
                case CACHE:
                    setupCacheMonitoring(component);
                    break;
                case LOAD_BALANCER:
                    setupLoadBalancerMonitoring(component);
                    break;
            }
        }
        
        private void setupKubernetesMonitoring(InfrastructureComponent cluster) {
            // Deploy Prometheus operator
            PrometheusOperatorSpec operatorSpec = PrometheusOperatorSpec.builder()
                .namespace("monitoring")
                .version("v0.60.1")
                .resources(ResourceRequirements.builder()
                    .cpuRequest("100m")
                    .cpuLimit("200m")
                    .memoryRequest("128Mi")
                    .memoryLimit("256Mi")
                    .build())
                .build();
            
            kubernetesClient.deploy(operatorSpec);
            
            // Configure Prometheus
            PrometheusSpec prometheusSpec = PrometheusSpec.builder()
                .namespace("monitoring")
                .replicas(2)
                .retention("30d")
                .storageSize("50Gi")
                .serviceMonitorSelector(Map.of(
                    "matchLabels", Map.of("monitoring", "enabled")
                ))
                .ruleSelector(Map.of(
                    "matchLabels", Map.of("prometheus", "platform")
                ))
                .alerting(AlertmanagerConfig.builder()
                    .alertmanagers(Arrays.asList(
                        AlertmanagerEndpoint.builder()
                            .namespace("monitoring")
                            .name("alertmanager")
                            .port("web")
                            .build()
                    ))
                    .build())
                .build();
            
            kubernetesClient.deploy(prometheusSpec);
            
            // Setup Grafana dashboards
            createKubernetesDashboards();
            
            // Configure alerting rules
            createKubernetesAlertingRules();
        }
        
        private void createKubernetesDashboards() {
            // Cluster overview dashboard
            GrafanaDashboard clusterDashboard = GrafanaDashboard.builder()
                .title("Kubernetes Cluster Overview")
                .tags(Arrays.asList("kubernetes", "cluster", "overview"))
                .panels(Arrays.asList(
                    createNodeResourcePanel(),
                    createPodStatusPanel(),
                    createNamespaceResourcePanel(),
                    createClusterEventsPanel()
                ))
                .build();
            
            grafanaClient.createDashboard(clusterDashboard);
            
            // Application performance dashboard
            GrafanaDashboard appDashboard = GrafanaDashboard.builder()
                .title("Application Performance")
                .tags(Arrays.asList("applications", "performance"))
                .panels(Arrays.asList(
                    createRequestRatePanel(),
                    createResponseTimePanel(),
                    createErrorRatePanel(),
                    createThroughputPanel()
                ))
                .build();
            
            grafanaClient.createDashboard(appDashboard);
        }
        
        private GrafanaPanel createNodeResourcePanel() {
            return GrafanaPanel.builder()
                .title("Node Resource Usage")
                .type("stat")
                .targets(Arrays.asList(
                    PrometheusQuery.builder()
                        .expr("(1 - avg(rate(node_cpu_seconds_total{mode=\"idle\"}[5m]))) * 100")
                        .legendFormat("CPU Usage %")
                        .build(),
                    PrometheusQuery.builder()
                        .expr("(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100")
                        .legendFormat("Memory Usage %")
                        .build()
                ))
                .fieldConfig(FieldConfig.builder()
                    .min(0)
                    .max(100)
                    .unit("percent")
                    .thresholds(Thresholds.builder()
                        .steps(Arrays.asList(
                            ThresholdStep.builder().color("green").value(0).build(),
                            ThresholdStep.builder().color("yellow").value(70).build(),
                            ThresholdStep.builder().color("red").value(90).build()
                        ))
                        .build())
                    .build())
                .build();
        }
        
        private void createKubernetesAlertingRules() {
            PrometheusRule alertingRules = PrometheusRule.builder()
                .metadata(ObjectMeta.builder()
                    .name("kubernetes-platform-alerts")
                    .namespace("monitoring")
                    .labels(Map.of("prometheus", "platform"))
                    .build())
                .spec(PrometheusRuleSpec.builder()
                    .groups(Arrays.asList(
                        createNodeAlertsGroup(),
                        createPodAlertsGroup(),
                        createResourceAlertsGroup()
                    ))
                    .build())
                .build();
            
            kubernetesClient.apply(alertingRules);
        }
        
        private PrometheusRuleGroup createNodeAlertsGroup() {
            return PrometheusRuleGroup.builder()
                .name("kubernetes.nodes")
                .rules(Arrays.asList(
                    AlertingRule.builder()
                        .alert("NodeCPUUsageHigh")
                        .expr("(1 - avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m]))) * 100 > 80")
                        .duration("5m")
                        .labels(Map.of("severity", "warning"))
                        .annotations(Map.of(
                            "summary", "High CPU usage on node {{ $labels.instance }}",
                            "description", "Node {{ $labels.instance }} has CPU usage above 80% for more than 5 minutes"
                        ))
                        .build(),
                    AlertingRule.builder()
                        .alert("NodeMemoryUsageHigh")
                        .expr("(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 85")
                        .duration("5m")
                        .labels(Map.of("severity", "warning"))
                        .annotations(Map.of(
                            "summary", "High memory usage on node {{ $labels.instance }}",
                            "description", "Node {{ $labels.instance }} has memory usage above 85% for more than 5 minutes"
                        ))
                        .build(),
                    AlertingRule.builder()
                        .alert("NodeDiskSpaceUsageHigh")
                        .expr("(1 - (node_filesystem_avail_bytes{mountpoint=\"/\"} / node_filesystem_size_bytes{mountpoint=\"/\"})) * 100 > 90")
                        .duration("5m")
                        .labels(Map.of("severity", "critical"))
                        .annotations(Map.of(
                            "summary", "High disk usage on node {{ $labels.instance }}",
                            "description", "Node {{ $labels.instance }} has disk usage above 90% for more than 5 minutes"
                        ))
                        .build()
                ))
                .build();
        }
    }
    
    @Component
    public static class ApplicationPerformanceMonitoring {
        
        @Autowired
        private OpenTelemetryCollector openTelemetryCollector;
        
        public void setupApplicationMonitoring(Application application) {
            // Configure OpenTelemetry
            OpenTelemetryConfiguration otelConfig = OpenTelemetryConfiguration.builder()
                .serviceName(application.getName())
                .serviceVersion(application.getVersion())
                .environment(application.getEnvironment())
                .instrumentations(Arrays.asList(
                    "spring-boot",
                    "jdbc",
                    "http-client",
                    "jvm"
                ))
                .exporters(Arrays.asList(
                    OTelExporter.builder()
                        .type("prometheus")
                        .endpoint("http://prometheus:9090/api/v1/write")
                        .build(),
                    OTelExporter.builder()
                        .type("jaeger")
                        .endpoint("http://jaeger-collector:14268/api/traces")
                        .build()
                ))
                .build();
            
            openTelemetryCollector.configure(otelConfig);
            
            // Setup custom metrics
            setupCustomMetrics(application);
            
            // Configure distributed tracing
            setupDistributedTracing(application);
            
            // Setup logging
            setupStructuredLogging(application);
        }
        
        private void setupCustomMetrics(Application application) {
            // Business metrics
            Counter businessTransactionCounter = Counter.builder("business.transactions.total")
                .description("Total number of business transactions")
                .tag("application", application.getName())
                .tag("type", "business")
                .register(meterRegistry);
            
            Timer businessTransactionTimer = Timer.builder("business.transaction.duration")
                .description("Business transaction duration")
                .tag("application", application.getName())
                .register(meterRegistry);
            
            // Technical metrics
            Gauge activeConnectionsGauge = Gauge.builder("database.connections.active")
                .description("Active database connections")
                .tag("application", application.getName())
                .register(meterRegistry, application, app -> app.getActiveConnections());
            
            Counter errorCounter = Counter.builder("application.errors.total")
                .description("Total application errors")
                .tag("application", application.getName())
                .register(meterRegistry);
        }
        
        private void setupDistributedTracing(Application application) {
            TracingConfiguration tracingConfig = TracingConfiguration.builder()
                .serviceName(application.getName())
                .samplingRate(0.1) // 10% sampling
                .maxSpansPerTrace(1000)
                .spanProcessors(Arrays.asList(
                    SpanProcessor.builder()
                        .type("batch")
                        .exporter("jaeger")
                        .batchSize(512)
                        .exportTimeout(Duration.ofSeconds(30))
                        .build()
                ))
                .build();
            
            openTelemetryCollector.configureTracing(tracingConfig);
        }
        
        private void setupStructuredLogging(Application application) {
            LoggingConfiguration loggingConfig = LoggingConfiguration.builder()
                .format("json")
                .level("INFO")
                .includeTraceId(true)
                .includeSpanId(true)
                .fields(Map.of(
                    "service.name", application.getName(),
                    "service.version", application.getVersion(),
                    "environment", application.getEnvironment()
                ))
                .appenders(Arrays.asList(
                    LogAppender.builder()
                        .type("elasticsearch")
                        .endpoint("http://elasticsearch:9200")
                        .index("application-logs")
                        .build(),
                    LogAppender.builder()
                        .type("console")
                        .build()
                ))
                .build();
            
            loggingService.configure(loggingConfig);
        }
    }
    
    @Component
    public static class PlatformSLOMonitoring {
        
        public void definePlatformSLOs(PlatformComponent component) {
            switch (component.getType()) {
                case API_GATEWAY:
                    defineApiGatewaySLOs(component);
                    break;
                case MICROSERVICE:
                    defineMicroserviceSLOs(component);
                    break;
                case DATABASE:
                    defineDatabaseSLOs(component);
                    break;
                case MESSAGE_QUEUE:
                    defineMessageQueueSLOs(component);
                    break;
            }
        }
        
        private void defineApiGatewaySLOs(PlatformComponent apiGateway) {
            // Availability SLO: 99.9% uptime
            SLO availabilitySLO = SLO.builder()
                .name("api-gateway-availability")
                .description("API Gateway availability SLO")
                .objective(0.999) // 99.9%
                .timeWindow(Duration.ofDays(30))
                .sli(SLI.builder()
                    .type(SLIType.AVAILABILITY)
                    .goodQuery("sum(rate(http_requests_total{job=\"api-gateway\",code!~\"5..\"}[5m]))")
                    .totalQuery("sum(rate(http_requests_total{job=\"api-gateway\"}[5m]))")
                    .build())
                .alerting(SLOAlerting.builder()
                    .burnRateAlerts(Arrays.asList(
                        BurnRateAlert.builder()
                            .severity("critical")
                            .longWindow(Duration.ofHours(1))
                            .shortWindow(Duration.ofMinutes(5))
                            .burnRateThreshold(14.4)
                            .build(),
                        BurnRateAlert.builder()
                            .severity("warning")
                            .longWindow(Duration.ofHours(6))
                            .shortWindow(Duration.ofMinutes(30))
                            .burnRateThreshold(6.0)
                            .build()
                    ))
                    .build())
                .build();
            
            // Latency SLO: 95% of requests < 200ms
            SLO latencySLO = SLO.builder()
                .name("api-gateway-latency")
                .description("API Gateway latency SLO")
                .objective(0.95) // 95%
                .timeWindow(Duration.ofDays(30))
                .sli(SLI.builder()
                    .type(SLIType.LATENCY)
                    .goodQuery("sum(rate(http_request_duration_seconds_bucket{job=\"api-gateway\",le=\"0.2\"}[5m]))")
                    .totalQuery("sum(rate(http_request_duration_seconds_count{job=\"api-gateway\"}[5m]))")
                    .build())
                .build();
            
            sloService.register(availabilitySLO);
            sloService.register(latencySLO);
        }
        
        private void defineMicroserviceSLOs(PlatformComponent microservice) {
            // Request success rate SLO
            SLO successRateSLO = SLO.builder()
                .name(microservice.getName() + "-success-rate")
                .description("Microservice request success rate SLO")
                .objective(0.995) // 99.5%
                .timeWindow(Duration.ofDays(30))
                .sli(SLI.builder()
                    .type(SLIType.SUCCESS_RATE)
                    .goodQuery(String.format(
                        "sum(rate(http_requests_total{job=\"%s\",code!~\"5..\"}[5m]))",
                        microservice.getName()
                    ))
                    .totalQuery(String.format(
                        "sum(rate(http_requests_total{job=\"%s\"}[5m]))",
                        microservice.getName()
                    ))
                    .build())
                .build();
            
            sloService.register(successRateSLO);
        }
    }
}
```

## 5. Practical Implementation Exercise

### Complete Platform Engineering Solution
```java
@RestController
@RequestMapping("/api/v1/platform")
@Slf4j
public class PlatformEngineeringController {
    
    @Autowired
    private TerraformOrchestrator terraformOrchestrator;
    
    @Autowired
    private GitOpsOrchestrator gitOpsOrchestrator;
    
    @Autowired
    private DeveloperExperiencePlatform developerExperiencePlatform;
    
    @PostMapping("/infrastructure/plan")
    public CompletableFuture<ResponseEntity<TerraformPlanResult>> planInfrastructure(
            @RequestBody @Valid InfrastructurePlanRequest request) {
        
        TerraformWorkspace workspace = terraformOrchestrator.createWorkspace(
            request.getWorkspaceId(), request.getConfiguration()
        );
        
        return workspace.plan()
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(throwable -> {
                log.error("Infrastructure planning failed", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TerraformPlanResult.error(throwable.getMessage()));
            });
    }
    
    @PostMapping("/infrastructure/apply")
    public CompletableFuture<ResponseEntity<TerraformApplyResult>> applyInfrastructure(
            @PathVariable String workspaceId,
            @RequestBody @Valid InfrastructureApplyRequest request) {
        
        TerraformWorkspace workspace = terraformOrchestrator.getWorkspace(workspaceId);
        
        return workspace.apply(request.getPlanId())
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(throwable -> {
                log.error("Infrastructure apply failed", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TerraformApplyResult.error(throwable.getMessage()));
            });
    }
    
    @PostMapping("/applications")
    public CompletableFuture<ResponseEntity<ApplicationDeployResult>> deployApplication(
            @RequestBody @Valid ApplicationDeployRequest request) {
        
        GitOpsApplication application = gitOpsOrchestrator.createApplication(
            request.getApplicationName(), request.getConfiguration()
        );
        
        return application.deploy()
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(throwable -> {
                log.error("Application deployment failed", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApplicationDeployResult.error(throwable.getMessage()));
            });
    }
    
    @PostMapping("/projects")
    public CompletableFuture<ResponseEntity<ProjectCreationResult>> createProject(
            @RequestBody @Valid ProjectCreationRequest request) {
        
        return developerExperiencePlatform.getSelfServicePortal().createProject(request)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(throwable -> {
                log.error("Project creation failed", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProjectCreationResult.error(throwable.getMessage()));
            });
    }
    
    @GetMapping("/metrics/productivity/{team}")
    public ResponseEntity<DeveloperProductivityReport> getProductivityMetrics(
            @PathVariable String team,
            @RequestParam(defaultValue = "30") int days) {
        
        try {
            DeveloperProductivityReport report = developerExperiencePlatform
                .getDeveloperProductivityMetrics()
                .generateProductivityReport(team, Duration.ofDays(days));
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            log.error("Failed to generate productivity report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

## Key Learning Outcomes

After completing Day 123, you will understand:

1. **Infrastructure as Code (IaC) Framework**
   - Terraform orchestration with workspace management and state handling
   - Pulumi integration for multi-language infrastructure programming
   - Policy validation and cost estimation for infrastructure changes
   - Module registry and reusable infrastructure components

2. **GitOps Implementation**
   - ArgoCD integration for continuous deployment and application lifecycle
   - Flux implementation with Kustomization and Helm releases
   - Security scanning and policy validation for Kubernetes manifests
   - Automated rollback and disaster recovery workflows

3. **Developer Experience Platform**
   - Self-service project creation with templates and automation
   - Environment provisioning with resource quotas and access controls
   - Backstage integration for service catalog and technical documentation
   - CI/CD pipeline automation with testing and security scanning

4. **Platform Engineering Observability**
   - Comprehensive monitoring with Prometheus, Grafana, and Jaeger
   - SLO/SLI definitions with burn rate alerting and error budgets
   - Developer productivity metrics and DORA metrics collection
   - Custom dashboards and alerting for platform components

5. **Enterprise Integration**
   - Production-ready platform APIs with proper error handling
   - Security and compliance integration throughout the platform
   - Scalable architecture supporting multiple teams and projects
   - Cost optimization and resource management capabilities

## Next Steps
- Day 124: Global Scale Systems - Multi-Region Architecture, Edge Computing & CDN Integration
- Day 125: Future Technologies - WebAssembly, Edge AI & Next-Generation Computing
- Continue building enterprise-grade platforms with global scale capabilities

## Additional Resources
- Terraform Provider Documentation
- ArgoCD and Flux GitOps Documentation
- Backstage Developer Portal Framework
- Prometheus and Grafana Monitoring Stack
- OpenTelemetry Observability Standards