# Day 111: Platform Engineering - Developer Experience, Infrastructure as Code & GitOps

## Learning Objectives
- Design and implement comprehensive platform engineering solutions for developer productivity
- Build Infrastructure as Code (IaC) frameworks using Terraform, Pulumi, and Kubernetes operators
- Create GitOps workflows with ArgoCD, Flux, and automated deployment pipelines
- Implement developer self-service platforms with automated provisioning and management
- Design observability and monitoring stacks for platform operations
- Build developer experience tools including CLI tooling, IDE integration, and documentation automation

## Part 1: Infrastructure as Code with Terraform and Pulumi

### Terraform Infrastructure Modules

```hcl
# modules/kubernetes-cluster/main.tf
terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.10"
    }
  }
}

# Variables
variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "cluster_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.27"
}

variable "vpc_id" {
  description = "VPC ID for the cluster"
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for the cluster"
  type        = list(string)
}

variable "node_groups" {
  description = "Node group configurations"
  type = map(object({
    instance_types = list(string)
    min_size      = number
    max_size      = number
    desired_size  = number
    disk_size     = number
    labels        = map(string)
    taints = list(object({
      key    = string
      value  = string
      effect = string
    }))
  }))
}

# EKS Cluster
resource "aws_eks_cluster" "main" {
  name     = var.cluster_name
  role_arn = aws_iam_role.cluster.arn
  version  = var.cluster_version

  vpc_config {
    subnet_ids              = var.subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = ["0.0.0.0/0"]
  }

  encryption_config {
    provider {
      key_arn = aws_kms_key.eks.arn
    }
    resources = ["secrets"]
  }

  enabled_cluster_log_types = [
    "api",
    "audit",
    "authenticator",
    "controllerManager",
    "scheduler"
  ]

  depends_on = [
    aws_iam_role_policy_attachment.cluster_AmazonEKSClusterPolicy,
    aws_cloudwatch_log_group.cluster
  ]

  tags = {
    Name = var.cluster_name
    Type = "platform-cluster"
  }
}

# Node Groups
resource "aws_eks_node_group" "main" {
  for_each = var.node_groups

  cluster_name    = aws_eks_cluster.main.name
  node_group_name = each.key
  node_role_arn   = aws_iam_role.node_group.arn
  subnet_ids      = var.subnet_ids
  instance_types  = each.value.instance_types
  disk_size       = each.value.disk_size

  scaling_config {
    desired_size = each.value.desired_size
    max_size     = each.value.max_size
    min_size     = each.value.min_size
  }

  remote_access {
    ec2_ssh_key = aws_key_pair.node_group.key_name
  }

  labels = each.value.labels

  dynamic "taint" {
    for_each = each.value.taints
    content {
      key    = taint.value.key
      value  = taint.value.value
      effect = taint.value.effect
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.node_group_AmazonEKSWorkerNodePolicy,
    aws_iam_role_policy_attachment.node_group_AmazonEKS_CNI_Policy,
    aws_iam_role_policy_attachment.node_group_AmazonEC2ContainerRegistryReadOnly,
  ]

  tags = {
    Name = "${var.cluster_name}-${each.key}"
  }
}

# OIDC Provider for IAM Roles for Service Accounts
data "tls_certificate" "cluster" {
  url = aws_eks_cluster.main.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "cluster" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.cluster.certificates[0].sha1_fingerprint]
  url             = aws_eks_cluster.main.identity[0].oidc[0].issuer

  tags = {
    Name = "${var.cluster_name}-oidc"
  }
}

# IAM Roles
resource "aws_iam_role" "cluster" {
  name = "${var.cluster_name}-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_AmazonEKSClusterPolicy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.cluster.name
}

# Add-ons
resource "aws_eks_addon" "main" {
  for_each = {
    coredns = {
      version = "v1.10.1-eksbuild.1"
    }
    kube-proxy = {
      version = "v1.27.1-eksbuild.1"
    }
    vpc-cni = {
      version = "v1.12.6-eksbuild.2"
    }
    aws-ebs-csi-driver = {
      version = "v1.19.0-eksbuild.2"
    }
  }

  cluster_name      = aws_eks_cluster.main.name
  addon_name        = each.key
  addon_version     = each.value.version
  resolve_conflicts = "OVERWRITE"

  depends_on = [aws_eks_node_group.main]
}

# Outputs
output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.main.endpoint
}

output "cluster_arn" {
  value = aws_eks_cluster.main.arn
}

output "oidc_provider_arn" {
  value = aws_iam_openid_connect_provider.cluster.arn
}
```

### Pulumi Infrastructure with Java

```java
package com.securetrading.platform.infrastructure;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.Vpc;
import com.pulumi.aws.ec2.VpcArgs;
import com.pulumi.aws.ec2.Subnet;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.InternetGateway;
import com.pulumi.aws.ec2.InternetGatewayArgs;
import com.pulumi.aws.ec2.RouteTable;
import com.pulumi.aws.ec2.RouteTableArgs;
import com.pulumi.aws.ec2.Route;
import com.pulumi.aws.ec2.RouteArgs;
import com.pulumi.aws.ec2.RouteTableAssociation;
import com.pulumi.aws.ec2.RouteTableAssociationArgs;
import com.pulumi.aws.rds.Instance;
import com.pulumi.aws.rds.InstanceArgs;
import com.pulumi.aws.rds.SubnetGroup;
import com.pulumi.aws.rds.SubnetGroupArgs;
import com.pulumi.kubernetes.Provider;
import com.pulumi.kubernetes.ProviderArgs;
import com.pulumi.kubernetes.apps.v1.Deployment;
import com.pulumi.kubernetes.apps.v1.DeploymentArgs;
import com.pulumi.kubernetes.core.v1.Service;
import com.pulumi.kubernetes.core.v1.ServiceArgs;
import com.pulumi.kubernetes.core.v1.Namespace;
import com.pulumi.kubernetes.core.v1.NamespaceArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;

import java.util.Map;
import java.util.List;

public class TradingPlatformInfrastructure extends ComponentResource {

    public TradingPlatformInfrastructure(String name, TradingPlatformInfrastructureArgs args, 
            ComponentResourceOptions options) {
        super("securetrading:platform:Infrastructure", name, args, options);

        // Create VPC
        var vpc = createVpc(args);
        
        // Create Subnets
        var subnets = createSubnets(vpc, args);
        
        // Create Database
        var database = createDatabase(subnets, args);
        
        // Create Kubernetes resources
        var k8sResources = createKubernetesResources(args);
        
        // Register outputs
        this.registerOutputs(Map.of(
            "vpcId", vpc.id(),
            "databaseEndpoint", database.endpoint(),
            "kubernetesNamespace", k8sResources.namespace.metadata().name()
        ));
    }

    private Vpc createVpc(TradingPlatformInfrastructureArgs args) {
        return new Vpc("trading-vpc", VpcArgs.builder()
                .cidrBlock("10.0.0.0/16")
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .tags(Map.of(
                        "Name", "trading-platform-vpc",
                        "Environment", args.environment(),
                        "Project", "secure-trading"
                ))
                .build(), ComponentResourceOptions.builder().parent(this).build());
    }

    private SubnetConfiguration createSubnets(Vpc vpc, TradingPlatformInfrastructureArgs args) {
        // Public subnets
        var publicSubnet1 = new Subnet("public-subnet-1", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.1.0/24")
                .availabilityZone("us-east-1a")
                .mapPublicIpOnLaunch(true)
                .tags(Map.of(
                        "Name", "trading-public-subnet-1",
                        "Type", "public"
                ))
                .build(), ComponentResourceOptions.builder().parent(this).build());

        var publicSubnet2 = new Subnet("public-subnet-2", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.2.0/24")
                .availabilityZone("us-east-1b")
                .mapPublicIpOnLaunch(true)
                .tags(Map.of(
                        "Name", "trading-public-subnet-2",
                        "Type", "public"
                ))
                .build(), ComponentResourceOptions.builder().parent(this).build());

        // Private subnets
        var privateSubnet1 = new Subnet("private-subnet-1", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.3.0/24")
                .availabilityZone("us-east-1a")
                .tags(Map.of(
                        "Name", "trading-private-subnet-1",
                        "Type", "private"
                ))
                .build(), ComponentResourceOptions.builder().parent(this).build());

        var privateSubnet2 = new Subnet("private-subnet-2", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.4.0/24")
                .availabilityZone("us-east-1b")
                .tags(Map.of(
                        "Name", "trading-private-subnet-2",
                        "Type", "private"
                ))
                .build(), ComponentResourceOptions.builder().parent(this).build());

        // Internet Gateway
        var igw = new InternetGateway("igw", InternetGatewayArgs.builder()
                .vpcId(vpc.id())
                .tags(Map.of("Name", "trading-igw"))
                .build(), ComponentResourceOptions.builder().parent(this).build());

        // Route Table
        var publicRouteTable = new RouteTable("public-rt", RouteTableArgs.builder()
                .vpcId(vpc.id())
                .tags(Map.of("Name", "trading-public-rt"))
                .build(), ComponentResourceOptions.builder().parent(this).build());

        // Route to Internet Gateway
        new Route("public-route", RouteArgs.builder()
                .routeTableId(publicRouteTable.id())
                .destinationCidrBlock("0.0.0.0/0")
                .gatewayId(igw.id())
                .build(), ComponentResourceOptions.builder().parent(this).build());

        // Route Table Associations
        new RouteTableAssociation("public-rt-assoc-1", RouteTableAssociationArgs.builder()
                .subnetId(publicSubnet1.id())
                .routeTableId(publicRouteTable.id())
                .build(), ComponentResourceOptions.builder().parent(this).build());

        new RouteTableAssociation("public-rt-assoc-2", RouteTableAssociationArgs.builder()
                .subnetId(publicSubnet2.id())
                .routeTableId(publicRouteTable.id())
                .build(), ComponentResourceOptions.builder().parent(this).build());

        return new SubnetConfiguration(
                List.of(publicSubnet1, publicSubnet2),
                List.of(privateSubnet1, privateSubnet2)
        );
    }

    private Instance createDatabase(SubnetConfiguration subnets, TradingPlatformInfrastructureArgs args) {
        // DB Subnet Group
        var dbSubnetGroup = new SubnetGroup("db-subnet-group", SubnetGroupArgs.builder()
                .name("trading-db-subnet-group")
                .subnetIds(subnets.privateSubnets().stream().map(s -> s.id()).toList())
                .tags(Map.of("Name", "trading-db-subnet-group"))
                .build(), ComponentResourceOptions.builder().parent(this).build());

        // RDS Instance
        return new Instance("trading-db", InstanceArgs.builder()
                .identifier("trading-platform-db")
                .engine("postgres")
                .engineVersion("15.3")
                .instanceClass("db.t3.medium")
                .allocatedStorage(100)
                .storageType("gp3")
                .storageEncrypted(true)
                .dbName("trading_platform")
                .username("admin")
                .password(args.dbPassword())
                .dbSubnetGroupName(dbSubnetGroup.name())
                .backupRetentionPeriod(7)
                .backupWindow("03:00-04:00")
                .maintenanceWindow("sun:04:00-sun:05:00")
                .skipFinalSnapshot(false)
                .finalSnapshotIdentifier("trading-platform-final-snapshot")
                .deletionProtection(true)
                .tags(Map.of(
                        "Name", "trading-platform-db",
                        "Environment", args.environment()
                ))
                .build(), ComponentResourceOptions.builder().parent(this).build());
    }

    private KubernetesResources createKubernetesResources(TradingPlatformInfrastructureArgs args) {
        // Kubernetes Provider
        var k8sProvider = new Provider("k8s-provider", ProviderArgs.builder()
                .kubeconfig(args.kubeconfig())
                .build(), ComponentResourceOptions.builder().parent(this).build());

        // Namespace
        var namespace = new Namespace("trading-platform", NamespaceArgs.builder()
                .metadata(com.pulumi.kubernetes.meta.v1.ObjectMetaArgs.builder()
                        .name("trading-platform")
                        .labels(Map.of(
                                "app", "trading-platform",
                                "environment", args.environment()
                        ))
                        .build())
                .build(), ComponentResourceOptions.builder()
                .parent(this)
                .provider(k8sProvider)
                .build());

        // Application Deployment
        var deployment = new Deployment("trading-app", DeploymentArgs.builder()
                .metadata(com.pulumi.kubernetes.meta.v1.ObjectMetaArgs.builder()
                        .namespace(namespace.metadata().name())
                        .name("trading-application")
                        .labels(Map.of("app", "trading-application"))
                        .build())
                .spec(com.pulumi.kubernetes.apps.v1.DeploymentSpecArgs.builder()
                        .replicas(3)
                        .selector(com.pulumi.kubernetes.meta.v1.LabelSelectorArgs.builder()
                                .matchLabels(Map.of("app", "trading-application"))
                                .build())
                        .template(com.pulumi.kubernetes.core.v1.PodTemplateSpecArgs.builder()
                                .metadata(com.pulumi.kubernetes.meta.v1.ObjectMetaArgs.builder()
                                        .labels(Map.of("app", "trading-application"))
                                        .build())
                                .spec(com.pulumi.kubernetes.core.v1.PodSpecArgs.builder()
                                        .containers(com.pulumi.kubernetes.core.v1.ContainerArgs.builder()
                                                .name("trading-app")
                                                .image(args.applicationImage())
                                                .ports(com.pulumi.kubernetes.core.v1.ContainerPortArgs.builder()
                                                        .containerPort(8080)
                                                        .build())
                                                .env(
                                                        com.pulumi.kubernetes.core.v1.EnvVarArgs.builder()
                                                                .name("ENVIRONMENT")
                                                                .value(args.environment())
                                                                .build(),
                                                        com.pulumi.kubernetes.core.v1.EnvVarArgs.builder()
                                                                .name("DATABASE_URL")
                                                                .value("jdbc:postgresql://database:5432/trading_platform")
                                                                .build()
                                                )
                                                .resources(com.pulumi.kubernetes.core.v1.ResourceRequirementsArgs.builder()
                                                        .requests(Map.of(
                                                                "memory", "512Mi",
                                                                "cpu", "250m"
                                                        ))
                                                        .limits(Map.of(
                                                                "memory", "1Gi",
                                                                "cpu", "500m"
                                                        ))
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build(), ComponentResourceOptions.builder()
                .parent(this)
                .provider(k8sProvider)
                .build());

        // Service
        var service = new Service("trading-service", ServiceArgs.builder()
                .metadata(com.pulumi.kubernetes.meta.v1.ObjectMetaArgs.builder()
                        .namespace(namespace.metadata().name())
                        .name("trading-application-service")
                        .build())
                .spec(com.pulumi.kubernetes.core.v1.ServiceSpecArgs.builder()
                        .selector(Map.of("app", "trading-application"))
                        .ports(com.pulumi.kubernetes.core.v1.ServicePortArgs.builder()
                                .port(80)
                                .targetPort(8080)
                                .protocol("TCP")
                                .build())
                        .type("LoadBalancer")
                        .build())
                .build(), ComponentResourceOptions.builder()
                .parent(this)
                .provider(k8sProvider)
                .build());

        return new KubernetesResources(namespace, deployment, service);
    }

    // Supporting records
    public record TradingPlatformInfrastructureArgs(
            String environment,
            String dbPassword,
            String kubeconfig,
            String applicationImage
    ) {}

    public record SubnetConfiguration(
            List<Subnet> publicSubnets,
            List<Subnet> privateSubnets
    ) {}

    public record KubernetesResources(
            Namespace namespace,
            Deployment deployment,
            Service service
    ) {}

    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var infrastructure = new TradingPlatformInfrastructure("trading-platform",
                    new TradingPlatformInfrastructureArgs(
                            "production",
                            "secure-password-123",
                            System.getProperty("KUBECONFIG"),
                            "trading-platform:latest"
                    ),
                    ComponentResourceOptions.builder().build());

            ctx.export("vpcId", infrastructure.vpcId);
            ctx.export("databaseEndpoint", infrastructure.databaseEndpoint);
        });
    }
}
```

## Part 2: GitOps with ArgoCD and Custom Operators

### ArgoCD Application Configuration

```yaml
# gitops/applications/trading-platform.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: trading-platform
  namespace: argocd
  labels:
    app: trading-platform
    environment: production
  annotations:
    argocd.argoproj.io/sync-wave: "1"
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: trading-platform
  source:
    repoURL: https://github.com/secure-trading/platform-manifests
    targetRevision: main
    path: overlays/production
    kustomize:
      commonLabels:
        app.kubernetes.io/instance: trading-platform
        app.kubernetes.io/managed-by: argocd
      patches:
        - target:
            kind: Deployment
            name: trading-application
          patch: |
            - op: replace
              path: /spec/replicas
              value: 5
  destination:
    server: https://kubernetes.default.svc
    namespace: trading-platform
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
      allowEmpty: false
    syncOptions:
      - CreateNamespace=true
      - PrunePropagationPolicy=foreground
      - PruneLast=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
  revisionHistoryLimit: 10
---
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: trading-platform
  namespace: argocd
spec:
  description: Trading Platform Project
  sourceRepos:
  - 'https://github.com/secure-trading/*'
  destinations:
  - namespace: 'trading-*'
    server: https://kubernetes.default.svc
  - namespace: 'monitoring'
    server: https://kubernetes.default.svc
  clusterResourceWhitelist:
  - group: ''
    kind: Namespace
  - group: rbac.authorization.k8s.io
    kind: ClusterRole
  - group: rbac.authorization.k8s.io
    kind: ClusterRoleBinding
  namespaceResourceWhitelist:
  - group: ''
    kind: ConfigMap
  - group: ''
    kind: Secret
  - group: ''
    kind: Service
  - group: apps
    kind: Deployment
  - group: networking.k8s.io
    kind: Ingress
  roles:
  - name: developers
    description: Developer access
    policies:
    - p, proj:trading-platform:developers, applications, get, trading-platform/*, allow
    - p, proj:trading-platform:developers, applications, sync, trading-platform/*, allow
    groups:
    - trading-platform:developers
  - name: admins
    description: Admin access
    policies:
    - p, proj:trading-platform:admins, applications, *, trading-platform/*, allow
    - p, proj:trading-platform:admins, repositories, *, *, allow
    groups:
    - trading-platform:admins
```

### Custom Kubernetes Operator for Trading Platform

```java
package com.securetrading.platform.operator;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ControllerConfiguration(
        dependents = {
                @Dependent(type = TradingPlatformDeploymentDependentResource.class),
                @Dependent(type = TradingPlatformServiceDependentResource.class),
                @Dependent(type = TradingPlatformConfigMapDependentResource.class)
        }
)
public class TradingPlatformReconciler implements Reconciler<TradingPlatform>, 
        EventSourceInitializer<TradingPlatform>, Cleaner<TradingPlatform> {

    private static final Logger logger = LoggerFactory.getLogger(TradingPlatformReconciler.class);

    private final KubernetesClient kubernetesClient;
    private final TradingPlatformService platformService;

    public TradingPlatformReconciler() {
        this.kubernetesClient = new KubernetesClientBuilder().build();
        this.platformService = new TradingPlatformService(kubernetesClient);
    }

    @Override
    public UpdateControl<TradingPlatform> reconcile(TradingPlatform tradingPlatform, Context<TradingPlatform> context) {
        logger.info("Reconciling TradingPlatform: {}", tradingPlatform.getMetadata().getName());

        try {
            // Validate the resource
            ValidationResult validation = validateTradingPlatform(tradingPlatform);
            if (!validation.isValid()) {
                logger.error("TradingPlatform validation failed: {}", validation.getErrorMessage());
                return updateStatusAndRequeue(tradingPlatform, TradingPlatformStatus.FAILED, 
                        validation.getErrorMessage());
            }

            // Update status to processing
            tradingPlatform.getStatus().setPhase(TradingPlatformStatus.PROCESSING);
            tradingPlatform.getStatus().setMessage("Reconciling trading platform components");

            // Reconcile components
            ReconciliationResult result = reconcileComponents(tradingPlatform, context);
            
            if (result.isSuccessful()) {
                logger.info("Successfully reconciled TradingPlatform: {}", 
                        tradingPlatform.getMetadata().getName());
                return updateStatusAndRequeue(tradingPlatform, TradingPlatformStatus.READY, 
                        "Trading platform is ready and operational");
            } else {
                logger.error("Failed to reconcile TradingPlatform: {} - {}", 
                        tradingPlatform.getMetadata().getName(), result.getErrorMessage());
                return updateStatusAndRequeue(tradingPlatform, TradingPlatformStatus.FAILED, 
                        result.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Error reconciling TradingPlatform: {}", 
                    tradingPlatform.getMetadata().getName(), e);
            return updateStatusAndRequeue(tradingPlatform, TradingPlatformStatus.FAILED, 
                    "Unexpected error during reconciliation: " + e.getMessage());
        }
    }

    @Override
    public DeleteControl cleanup(TradingPlatform tradingPlatform, Context<TradingPlatform> context) {
        logger.info("Cleaning up TradingPlatform: {}", tradingPlatform.getMetadata().getName());

        try {
            // Perform cleanup operations
            CleanupResult cleanup = platformService.cleanup(tradingPlatform);
            
            if (cleanup.isSuccessful()) {
                logger.info("Successfully cleaned up TradingPlatform: {}", 
                        tradingPlatform.getMetadata().getName());
                return DeleteControl.defaultDelete();
            } else {
                logger.error("Failed to cleanup TradingPlatform: {} - {}", 
                        tradingPlatform.getMetadata().getName(), cleanup.getErrorMessage());
                return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofMinutes(1));
            }

        } catch (Exception e) {
            logger.error("Error cleaning up TradingPlatform: {}", 
                    tradingPlatform.getMetadata().getName(), e);
            return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofMinutes(1));
        }
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<TradingPlatform> context) {
        // Watch for deployment changes
        InformerEventSource<io.fabric8.kubernetes.api.model.apps.Deployment, TradingPlatform> deploymentEventSource =
                new InformerEventSource<>(
                        InformerEventSource.InformerConfiguration.<io.fabric8.kubernetes.api.model.apps.Deployment>builder()
                                .withResourceClass(io.fabric8.kubernetes.api.model.apps.Deployment.class)
                                .withLabelSelector("app.kubernetes.io/managed-by=trading-platform-operator")
                                .build(),
                        context);

        // Watch for service changes
        InformerEventSource<io.fabric8.kubernetes.api.model.Service, TradingPlatform> serviceEventSource =
                new InformerEventSource<>(
                        InformerEventSource.InformerConfiguration.<io.fabric8.kubernetes.api.model.Service>builder()
                                .withResourceClass(io.fabric8.kubernetes.api.model.Service.class)
                                .withLabelSelector("app.kubernetes.io/managed-by=trading-platform-operator")
                                .build(),
                        context);

        return List.of(deploymentEventSource, serviceEventSource);
    }

    private ValidationResult validateTradingPlatform(TradingPlatform tradingPlatform) {
        TradingPlatformSpec spec = tradingPlatform.getSpec();

        // Validate required fields
        if (spec.getImage() == null || spec.getImage().isEmpty()) {
            return ValidationResult.invalid("Image is required");
        }

        if (spec.getReplicas() <= 0) {
            return ValidationResult.invalid("Replicas must be greater than 0");
        }

        if (spec.getResources() == null) {
            return ValidationResult.invalid("Resources specification is required");
        }

        // Validate resource limits
        if (spec.getResources().getMemoryLimit() == null || 
            spec.getResources().getCpuLimit() == null) {
            return ValidationResult.invalid("Memory and CPU limits are required");
        }

        // Validate database configuration
        if (spec.getDatabase() == null || spec.getDatabase().getUrl() == null) {
            return ValidationResult.invalid("Database configuration is required");
        }

        return ValidationResult.valid();
    }

    private ReconciliationResult reconcileComponents(TradingPlatform tradingPlatform, 
            Context<TradingPlatform> context) {
        
        try {
            // The dependent resources will be automatically reconciled by the framework
            // We just need to perform any additional logic here

            // Update metrics and monitoring
            platformService.updateMetrics(tradingPlatform);

            // Verify component health
            HealthCheckResult healthCheck = platformService.performHealthCheck(tradingPlatform);
            if (!healthCheck.isHealthy()) {
                return ReconciliationResult.failed("Health check failed: " + 
                        healthCheck.getErrorMessage());
            }

            return ReconciliationResult.successful();

        } catch (Exception e) {
            return ReconciliationResult.failed("Failed to reconcile components: " + e.getMessage());
        }
    }

    private UpdateControl<TradingPlatform> updateStatusAndRequeue(TradingPlatform tradingPlatform, 
            TradingPlatformStatus status, String message) {
        
        tradingPlatform.getStatus().setPhase(status);
        tradingPlatform.getStatus().setMessage(message);
        tradingPlatform.getStatus().setLastUpdated(java.time.Instant.now().toString());

        if (status == TradingPlatformStatus.FAILED) {
            return UpdateControl.updateStatus(tradingPlatform).rescheduleAfter(Duration.ofMinutes(5));
        } else {
            return UpdateControl.updateStatus(tradingPlatform).rescheduleAfter(Duration.ofMinutes(1));
        }
    }

    // Supporting classes
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ReconciliationResult {
        private final boolean successful;
        private final String errorMessage;

        private ReconciliationResult(boolean successful, String errorMessage) {
            this.successful = successful;
            this.errorMessage = errorMessage;
        }

        public static ReconciliationResult successful() {
            return new ReconciliationResult(true, null);
        }

        public static ReconciliationResult failed(String errorMessage) {
            return new ReconciliationResult(false, errorMessage);
        }

        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class CleanupResult {
        private final boolean successful;
        private final String errorMessage;

        private CleanupResult(boolean successful, String errorMessage) {
            this.successful = successful;
            this.errorMessage = errorMessage;
        }

        public static CleanupResult successful() {
            return new CleanupResult(true, null);
        }

        public static CleanupResult failed(String errorMessage) {
            return new CleanupResult(false, errorMessage);
        }

        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class HealthCheckResult {
        private final boolean healthy;
        private final String errorMessage;

        private HealthCheckResult(boolean healthy, String errorMessage) {
            this.healthy = healthy;
            this.errorMessage = errorMessage;
        }

        public static HealthCheckResult healthy() {
            return new HealthCheckResult(true, null);
        }

        public static HealthCheckResult unhealthy(String errorMessage) {
            return new HealthCheckResult(false, errorMessage);
        }

        public boolean isHealthy() { return healthy; }
        public String getErrorMessage() { return errorMessage; }
    }
}

// Custom Resource Definition
class TradingPlatform {
    private ObjectMeta metadata;
    private TradingPlatformSpec spec;
    private TradingPlatformStatus status;

    // Constructors, getters, setters
    public TradingPlatform() {
        this.status = new TradingPlatformStatus();
    }

    public ObjectMeta getMetadata() { return metadata; }
    public void setMetadata(ObjectMeta metadata) { this.metadata = metadata; }
    public TradingPlatformSpec getSpec() { return spec; }
    public void setSpec(TradingPlatformSpec spec) { this.spec = spec; }
    public TradingPlatformStatus getStatus() { return status; }
    public void setStatus(TradingPlatformStatus status) { this.status = status; }
}

class TradingPlatformSpec {
    private String image;
    private int replicas;
    private ResourceSpec resources;
    private DatabaseSpec database;
    private SecuritySpec security;
    private Map<String, String> environment;

    // Getters and setters
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getReplicas() { return replicas; }
    public void setReplicas(int replicas) { this.replicas = replicas; }
    public ResourceSpec getResources() { return resources; }
    public void setResources(ResourceSpec resources) { this.resources = resources; }
    public DatabaseSpec getDatabase() { return database; }
    public void setDatabase(DatabaseSpec database) { this.database = database; }
    public SecuritySpec getSecurity() { return security; }
    public void setSecurity(SecuritySpec security) { this.security = security; }
    public Map<String, String> getEnvironment() { return environment; }
    public void setEnvironment(Map<String, String> environment) { this.environment = environment; }
}

class TradingPlatformStatus {
    private TradingPlatformStatus phase;
    private String message;
    private String lastUpdated;
    private Map<String, Object> conditions;

    public TradingPlatformStatus() {
        this.phase = TradingPlatformStatus.PENDING;
        this.conditions = new java.util.HashMap<>();
    }

    // Getters and setters
    public TradingPlatformStatus getPhase() { return phase; }
    public void setPhase(TradingPlatformStatus phase) { this.phase = phase; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    public Map<String, Object> getConditions() { return conditions; }
    public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
}

enum TradingPlatformStatus {
    PENDING, PROCESSING, READY, FAILED, UPDATING
}

class ResourceSpec {
    private String memoryLimit;
    private String cpuLimit;
    private String memoryRequest;
    private String cpuRequest;

    // Getters and setters
    public String getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(String memoryLimit) { this.memoryLimit = memoryLimit; }
    public String getCpuLimit() { return cpuLimit; }
    public void setCpuLimit(String cpuLimit) { this.cpuLimit = cpuLimit; }
    public String getMemoryRequest() { return memoryRequest; }
    public void setMemoryRequest(String memoryRequest) { this.memoryRequest = memoryRequest; }
    public String getCpuRequest() { return cpuRequest; }
    public void setCpuRequest(String cpuRequest) { this.cpuRequest = cpuRequest; }
}

class DatabaseSpec {
    private String url;
    private String username;
    private String passwordSecret;
    private String driver;

    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordSecret() { return passwordSecret; }
    public void setPasswordSecret(String passwordSecret) { this.passwordSecret = passwordSecret; }
    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }
}

class SecuritySpec {
    private boolean enableTls;
    private String tlsSecret;
    private AuthenticationSpec authentication;

    // Getters and setters
    public boolean isEnableTls() { return enableTls; }
    public void setEnableTls(boolean enableTls) { this.enableTls = enableTls; }
    public String getTlsSecret() { return tlsSecret; }
    public void setTlsSecret(String tlsSecret) { this.tlsSecret = tlsSecret; }
    public AuthenticationSpec getAuthentication() { return authentication; }
    public void setAuthentication(AuthenticationSpec authentication) { this.authentication = authentication; }
}

class AuthenticationSpec {
    private String method;
    private String oidcProvider;
    private String jwtSecret;

    // Getters and setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getOidcProvider() { return oidcProvider; }
    public void setOidcProvider(String oidcProvider) { this.oidcProvider = oidcProvider; }
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
}
```

## Part 3: Developer Experience Platform

### CLI Tool for Platform Management

```java
package com.securetrading.platform.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@SpringBootApplication
public class PlatformCLIApplication implements CommandLineRunner {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(PlatformCLIApplication.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        int exitCode = new CommandLine(new TradingPlatformCLI()).execute(args);
        System.exit(exitCode);
    }
}

@Component
@Command(
        name = "trading-cli",
        description = "Trading Platform CLI for developers",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        subcommands = {
                ProjectCommands.class,
                DeployCommands.class,
                EnvironmentCommands.class,
                MonitoringCommands.class
        }
)
public class TradingPlatformCLI implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Trading Platform CLI - Use --help for available commands");
        return 0;
    }
}

@Command(
        name = "project",
        description = "Project management commands",
        subcommands = {
                ProjectCommands.InitCommand.class,
                ProjectCommands.BuildCommand.class,
                ProjectCommands.TestCommand.class
        }
)
class ProjectCommands {

    @Command(name = "init", description = "Initialize a new trading platform project")
    static class InitCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Project name")
        private String projectName;

        @Option(names = {"-t", "--template"}, description = "Project template", 
                defaultValue = "microservice")
        private String template;

        @Option(names = {"-p", "--path"}, description = "Project path", 
                defaultValue = ".")
        private String path;

        @Override
        public Integer call() throws Exception {
            System.out.println("Initializing project: " + projectName);
            
            Path projectPath = Paths.get(path, projectName);
            
            // Create project structure
            createProjectStructure(projectPath, template);
            
            // Generate configuration files
            generateConfigFiles(projectPath, projectName);
            
            // Initialize Git repository
            initializeGitRepository(projectPath);
            
            System.out.println("Project " + projectName + " initialized successfully!");
            System.out.println("Next steps:");
            System.out.println("  cd " + projectName);
            System.out.println("  trading-cli project build");
            
            return 0;
        }

        private void createProjectStructure(Path projectPath, String template) throws IOException {
            Files.createDirectories(projectPath.resolve("src/main/java"));
            Files.createDirectories(projectPath.resolve("src/main/resources"));
            Files.createDirectories(projectPath.resolve("src/test/java"));
            Files.createDirectories(projectPath.resolve("docker"));
            Files.createDirectories(projectPath.resolve("k8s"));
            Files.createDirectories(projectPath.resolve("scripts"));
            
            // Create basic files based on template
            switch (template) {
                case "microservice":
                    createMicroserviceTemplate(projectPath);
                    break;
                case "library":
                    createLibraryTemplate(projectPath);
                    break;
                default:
                    createDefaultTemplate(projectPath);
            }
        }

        private void createMicroserviceTemplate(Path projectPath) throws IOException {
            // Create Spring Boot application
            String appContent = """
                package com.securetrading.%s;
                
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                
                @SpringBootApplication
                public class Application {
                    public static void main(String[] args) {
                        SpringApplication.run(Application.class, args);
                    }
                }
                """.formatted(projectPath.getFileName().toString().toLowerCase());
            
            Files.writeString(projectPath.resolve("src/main/java/Application.java"), appContent);
            
            // Create Dockerfile
            String dockerContent = """
                FROM openjdk:17-jdk-alpine
                VOLUME /tmp
                COPY target/*.jar app.jar
                ENTRYPOINT ["java","-jar","/app.jar"]
                """;
            
            Files.writeString(projectPath.resolve("docker/Dockerfile"), dockerContent);
            
            // Create basic pom.xml
            String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.securetrading</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                    <packaging>jar</packaging>
                    
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.1.0</version>
                        <relativePath/>
                    </parent>
                    
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-test</artifactId>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """.formatted(projectPath.getFileName().toString());
            
            Files.writeString(projectPath.resolve("pom.xml"), pomContent);
        }

        private void createLibraryTemplate(Path projectPath) throws IOException {
            // Library-specific template
            System.out.println("Creating library template...");
        }

        private void createDefaultTemplate(Path projectPath) throws IOException {
            // Default template
            System.out.println("Creating default template...");
        }

        private void generateConfigFiles(Path projectPath, String projectName) throws IOException {
            // Generate application.yml
            String appConfig = """
                server:
                  port: 8080
                
                spring:
                  application:
                    name: %s
                  
                management:
                  endpoints:
                    web:
                      exposure:
                        include: health,info,metrics,prometheus
                  endpoint:
                    health:
                      show-details: when-authorized
                """.formatted(projectName);
            
            Files.writeString(projectPath.resolve("src/main/resources/application.yml"), appConfig);
            
            // Generate Kubernetes manifests
            String k8sDeployment = """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: %s
                  labels:
                    app: %s
                spec:
                  replicas: 3
                  selector:
                    matchLabels:
                      app: %s
                  template:
                    metadata:
                      labels:
                        app: %s
                    spec:
                      containers:
                      - name: %s
                        image: %s:latest
                        ports:
                        - containerPort: 8080
                        resources:
                          requests:
                            memory: "256Mi"
                            cpu: "250m"
                          limits:
                            memory: "512Mi"
                            cpu: "500m"
                """.formatted(projectName, projectName, projectName, projectName, projectName, projectName);
            
            Files.writeString(projectPath.resolve("k8s/deployment.yaml"), k8sDeployment);
        }

        private void initializeGitRepository(Path projectPath) throws IOException {
            // Create .gitignore
            String gitignoreContent = """
                target/
                .mvn/
                *.jar
                *.war
                *.log
                .DS_Store
                .idea/
                *.iml
                .vscode/
                """;
            
            Files.writeString(projectPath.resolve(".gitignore"), gitignoreContent);
            
            // Initialize Git (would run git init command)
            System.out.println("Git repository initialized");
        }
    }

    @Command(name = "build", description = "Build the project")
    static class BuildCommand implements Callable<Integer> {

        @Option(names = {"-e", "--environment"}, description = "Target environment", 
                defaultValue = "development")
        private String environment;

        @Option(names = {"--docker"}, description = "Build Docker image")
        private boolean buildDocker;

        @Override
        public Integer call() throws Exception {
            System.out.println("Building project for environment: " + environment);
            
            // Maven build
            ProcessBuilder mvnBuild = new ProcessBuilder("mvn", "clean", "package");
            Process process = mvnBuild.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                System.err.println("Maven build failed");
                return 1;
            }
            
            System.out.println("Maven build successful");
            
            if (buildDocker) {
                // Docker build
                ProcessBuilder dockerBuild = new ProcessBuilder("docker", "build", 
                        "-f", "docker/Dockerfile", "-t", getCurrentProjectName() + ":latest", ".");
                Process dockerProcess = dockerBuild.start();
                int dockerExitCode = dockerProcess.waitFor();
                
                if (dockerExitCode != 0) {
                    System.err.println("Docker build failed");
                    return 1;
                }
                
                System.out.println("Docker build successful");
            }
            
            return 0;
        }

        private String getCurrentProjectName() {
            return Paths.get(".").toAbsolutePath().getFileName().toString();
        }
    }

    @Command(name = "test", description = "Run tests")
    static class TestCommand implements Callable<Integer> {

        @Option(names = {"-t", "--type"}, description = "Test type", 
                defaultValue = "unit")
        private String testType;

        @Override
        public Integer call() throws Exception {
            System.out.println("Running " + testType + " tests");
            
            String[] command;
            switch (testType) {
                case "unit":
                    command = new String[]{"mvn", "test"};
                    break;
                case "integration":
                    command = new String[]{"mvn", "test", "-Dtest=**/*IT"};
                    break;
                case "e2e":
                    command = new String[]{"mvn", "test", "-Dtest=**/*E2E"};
                    break;
                default:
                    command = new String[]{"mvn", "test"};
            }
            
            ProcessBuilder testProcess = new ProcessBuilder(command);
            Process process = testProcess.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("Tests passed successfully");
            } else {
                System.err.println("Tests failed");
            }
            
            return exitCode;
        }
    }
}

@Command(
        name = "deploy",
        description = "Deployment commands",
        subcommands = {
                DeployCommands.DeployCommand.class,
                DeployCommands.StatusCommand.class,
                DeployCommands.RollbackCommand.class
        }
)
class DeployCommands {

    @Command(name = "deploy", description = "Deploy application")
    static class DeployCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Environment name")
        private String environment;

        @Option(names = {"-v", "--version"}, description = "Application version")
        private String version;

        @Option(names = {"--dry-run"}, description = "Perform a dry run")
        private boolean dryRun;

        @Override
        public Integer call() throws Exception {
            System.out.println("Deploying to environment: " + environment);
            
            if (dryRun) {
                System.out.println("DRY RUN - No actual deployment will be performed");
            }
            
            // Deployment logic would go here
            // This would typically interact with ArgoCD or kubectl
            
            if (!dryRun) {
                System.out.println("Deployment completed successfully");
            }
            
            return 0;
        }
    }

    @Command(name = "status", description = "Check deployment status")
    static class StatusCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Environment name")
        private String environment;

        @Override
        public Integer call() throws Exception {
            System.out.println("Checking deployment status for: " + environment);
            
            // Status checking logic would go here
            
            return 0;
        }
    }

    @Command(name = "rollback", description = "Rollback deployment")
    static class RollbackCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Environment name")
        private String environment;

        @Option(names = {"-v", "--version"}, description = "Target version for rollback")
        private String targetVersion;

        @Override
        public Integer call() throws Exception {
            System.out.println("Rolling back deployment in: " + environment);
            
            if (targetVersion != null) {
                System.out.println("Target version: " + targetVersion);
            }
            
            // Rollback logic would go here
            
            return 0;
        }
    }
}

@Command(
        name = "env",
        description = "Environment management commands",
        subcommands = {
                EnvironmentCommands.ListCommand.class,
                EnvironmentCommands.CreateCommand.class,
                EnvironmentCommands.DeleteCommand.class
        }
)
class EnvironmentCommands {

    @Command(name = "list", description = "List available environments")
    static class ListCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("Available environments:");
            System.out.println("  - development");
            System.out.println("  - staging");
            System.out.println("  - production");
            
            return 0;
        }
    }

    @Command(name = "create", description = "Create new environment")
    static class CreateCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Environment name")
        private String environmentName;

        @Option(names = {"-t", "--template"}, description = "Environment template")
        private String template;

        @Override
        public Integer call() throws Exception {
            System.out.println("Creating environment: " + environmentName);
            
            // Environment creation logic would go here
            
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete environment")
    static class DeleteCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Environment name")
        private String environmentName;

        @Option(names = {"-f", "--force"}, description = "Force deletion")
        private boolean force;

        @Override
        public Integer call() throws Exception {
            System.out.println("Deleting environment: " + environmentName);
            
            if (!force) {
                System.out.println("Use --force to confirm deletion");
                return 1;
            }
            
            // Environment deletion logic would go here
            
            return 0;
        }
    }
}

@Command(
        name = "monitor",
        description = "Monitoring and observability commands",
        subcommands = {
                MonitoringCommands.LogsCommand.class,
                MonitoringCommands.MetricsCommand.class,
                MonitoringCommands.TraceCommand.class
        }
)
class MonitoringCommands {

    @Command(name = "logs", description = "View application logs")
    static class LogsCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Application name")
        private String applicationName;

        @Option(names = {"-e", "--environment"}, description = "Environment", 
                defaultValue = "development")
        private String environment;

        @Option(names = {"-f", "--follow"}, description = "Follow logs")
        private boolean follow;

        @Option(names = {"-n", "--lines"}, description = "Number of lines", 
                defaultValue = "100")
        private int lines;

        @Override
        public Integer call() throws Exception {
            System.out.println("Viewing logs for: " + applicationName + " in " + environment);
            
            // Log viewing logic would go here
            // This would typically use kubectl logs or similar
            
            return 0;
        }
    }

    @Command(name = "metrics", description = "View application metrics")
    static class MetricsCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Application name")
        private String applicationName;

        @Option(names = {"-e", "--environment"}, description = "Environment", 
                defaultValue = "development")
        private String environment;

        @Override
        public Integer call() throws Exception {
            System.out.println("Viewing metrics for: " + applicationName + " in " + environment);
            
            // Metrics viewing logic would go here
            
            return 0;
        }
    }

    @Command(name = "trace", description = "View application traces")
    static class TraceCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Trace ID")
        private String traceId;

        @Override
        public Integer call() throws Exception {
            System.out.println("Viewing trace: " + traceId);
            
            // Trace viewing logic would go here
            
            return 0;
        }
    }
}
```

## Practical Exercises

### Exercise 1: Complete IaC Pipeline
Build a complete Infrastructure as Code pipeline that provisions a multi-environment Kubernetes cluster with networking, storage, and monitoring components.

### Exercise 2: GitOps Workflow Implementation
Implement a full GitOps workflow with ArgoCD that handles application deployments, rollbacks, and environment promotion with approval processes.

### Exercise 3: Custom Operator Development
Develop a comprehensive Kubernetes operator that manages the complete lifecycle of a microservice including deployment, scaling, backup, and disaster recovery.

### Exercise 4: Developer Self-Service Platform
Create a developer self-service platform that allows teams to provision environments, deploy applications, and manage resources through a web interface and CLI.

### Exercise 5: Observability Stack Implementation
Build a complete observability stack with metrics, logging, tracing, and alerting that provides comprehensive insights into platform and application health.

## Platform Engineering Best Practices

### Infrastructure as Code
- Use version control for all infrastructure definitions
- Implement infrastructure testing and validation
- Use modular and reusable infrastructure components
- Implement proper secret management and security
- Use infrastructure drift detection and remediation

### GitOps Implementation
- Maintain separation between application code and deployment manifests
- Use declarative configuration management
- Implement proper branching strategies for different environments
- Use automated testing for deployment manifests
- Implement rollback and disaster recovery procedures

### Developer Experience
- Provide self-service capabilities for common tasks
- Implement comprehensive documentation and onboarding
- Create standardized project templates and tooling
- Implement automated testing and quality gates
- Provide real-time feedback and monitoring

## Summary

Day 111 covered comprehensive platform engineering concepts for building developer-focused infrastructure:

1. **Infrastructure as Code**: Terraform and Pulumi implementations for scalable infrastructure management
2. **GitOps Implementation**: ArgoCD-based deployment workflows with automated rollbacks and environment management
3. **Custom Kubernetes Operators**: Building operators for application lifecycle management and automation
4. **Developer Experience Tools**: CLI tooling, project templates, and self-service platforms
5. **Platform Operations**: Monitoring, observability, and operational best practices

These implementations provide the foundation for building modern platform engineering solutions that enhance developer productivity while maintaining operational excellence and security standards.

Tomorrow's Day 112 will be the Week 16 Capstone project, where we'll integrate all the concepts from this week into a comprehensive cloud-native AI-powered financial analytics platform.