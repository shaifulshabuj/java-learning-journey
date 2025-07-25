# Day 82: CI/CD with Jenkins & GitHub Actions

## Learning Objectives
- Master Jenkins pipeline creation and configuration
- Implement GitHub Actions workflows for Java applications
- Learn containerized CI/CD with Docker integration
- Configure automated testing and deployment pipelines
- Implement GitOps workflows and security best practices

## Introduction to CI/CD

### CI/CD Fundamentals
Continuous Integration/Continuous Deployment (CI/CD) is a method to frequently deliver apps by introducing automation into the stages of app development.

```yaml
# CI/CD Pipeline Stages
continuous_integration:
  stages:
    - source_code_management: "Git triggers and webhooks"
    - build: "Compile, package, and create artifacts"
    - test: "Unit tests, integration tests, security scans"
    - quality_gates: "Code quality analysis and coverage"
    - artifact_creation: "Docker images, JAR files, documentation"

continuous_deployment:
  stages:
    - deployment_preparation: "Configuration management and secrets"
    - staging_deployment: "Deploy to staging environment"
    - automated_testing: "E2E tests, performance tests, security tests"
    - production_deployment: "Blue-green or canary deployments"
    - monitoring: "Health checks and observability"
```

### Pipeline as Code Benefits

```yaml
# Benefits of Pipeline as Code
version_control:
  - "Pipeline definitions versioned with source code"
  - "Branching and merging strategies for pipelines"
  - "Rollback capabilities for pipeline changes"

reproducibility:
  - "Consistent builds across environments"
  - "Infrastructure as Code integration"
  - "Deterministic dependency management"

collaboration:
  - "Team visibility into deployment processes"
  - "Code review for pipeline changes"
  - "Documentation alongside code"
```

## Jenkins Pipeline Implementation

### Jenkins Setup and Configuration

```groovy
// day82-examples/jenkins/Dockerfile
FROM jenkins/jenkins:lts

USER root

# Install Docker
RUN apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

RUN echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

RUN apt-get update && apt-get install -y docker-ce-cli

# Install kubectl
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
    && chmod +x kubectl \
    && mv kubectl /usr/local/bin/

# Install additional tools
RUN apt-get install -y \
    maven \
    nodejs \
    npm \
    python3 \
    python3-pip

# Install Jenkins plugins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins

# Copy initial configuration
COPY jenkins.yaml /var/jenkins_home/casc_configs/jenkins.yaml
```

```txt
# day82-examples/jenkins/plugins.txt
blueocean:latest
pipeline-stage-view:latest
docker-workflow:latest
kubernetes:latest
git:latest
github:latest
pipeline-github:latest
workflow-aggregator:latest
credentials:latest
configuration-as-code:latest
job-dsl:latest
build-timeout:latest
timestamper:latest
ws-cleanup:latest
ant:latest
gradle:latest
maven-plugin:latest
nodejs:latest
sonar:latest
checkmarx:latest
owasp-dependency-check:latest
```

### Basic Jenkins Pipeline

```groovy
// day82-examples/jenkins/Jenkinsfile.basic
pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-17'
            args '-v /root/.m2:/root/.m2'
        }
    }
    
    environment {
        MAVEN_OPTS = '-Xmx1024m'
        JAVA_HOME = '/usr/local/openjdk-17'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    triggers {
        pollSCM('H/5 * * * *')
        cron('H 2 * * 1-5')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_HASH = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_HASH}"
                }
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    echo "Building with Maven..."
                    mvn clean compile -DskipTests=true
                '''
            }
        }
        
        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh 'mvn test'
                    }
                    post {
                        always {
                            publishTestResults(
                                testResultsPattern: 'target/surefire-reports/*.xml'
                            )
                        }
                    }
                }
                
                stage('Integration Tests') {
                    steps {
                        sh 'mvn verify -Dskip.unit.tests=true'
                    }
                    post {
                        always {
                            publishTestResults(
                                testResultsPattern: 'target/failsafe-reports/*.xml'
                            )
                        }
                    }
                }
            }
        }
        
        stage('Quality Analysis') {
            steps {
                sh '''
                    mvn sonar:sonar \
                        -Dsonar.projectKey=${JOB_NAME} \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_AUTH_TOKEN}
                '''
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests=true'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline succeeded!'
            emailext (
                subject: "Build Success: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build ${env.BUILD_NUMBER} succeeded for ${env.JOB_NAME}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
        failure {
            echo 'Pipeline failed!'
            emailext (
                subject: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build ${env.BUILD_NUMBER} failed for ${env.JOB_NAME}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
```

### Advanced Jenkins Pipeline with Docker

```groovy
// day82-examples/jenkins/Jenkinsfile.docker
pipeline {
    agent {
        kubernetes {
            yaml """
                apiVersion: v1
                kind: Pod
                spec:
                  containers:
                  - name: maven
                    image: maven:3.8.6-openjdk-17
                    command:
                    - cat
                    tty: true
                    volumeMounts:
                    - name: docker-sock
                      mountPath: /var/run/docker.sock
                  - name: docker
                    image: docker:latest
                    command:
                    - cat
                    tty: true
                    volumeMounts:
                    - name: docker-sock
                      mountPath: /var/run/docker.sock
                  - name: kubectl
                    image: bitnami/kubectl:latest
                    command:
                    - cat
                    tty: true
                  volumes:
                  - name: docker-sock
                    hostPath:
                      path: /var/run/docker.sock
            """
        }
    }
    
    environment {
        DOCKER_REGISTRY = 'your-registry.com'
        IMAGE_NAME = 'spring-boot-app'
        KUBECONFIG = credentials('kubeconfig')
        DOCKER_CREDENTIALS = credentials('docker-registry')
        SONAR_TOKEN = credentials('sonar-token')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()
                    env.GIT_SHORT_COMMIT = env.GIT_COMMIT.take(7)
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_SHORT_COMMIT}"
                    env.FULL_IMAGE_NAME = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }
        
        stage('Build & Test') {
            parallel {
                stage('Maven Build') {
                    steps {
                        container('maven') {
                            sh '''
                                mvn clean compile test package \
                                    -Dmaven.test.failure.ignore=false \
                                    -Dspring.profiles.active=test
                            '''
                        }
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            publishCoverage adapters: [
                                jacocoAdapter('target/site/jacoco/jacoco.xml')
                            ], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
                        }
                    }
                }
                
                stage('Security Scan') {
                    steps {
                        container('maven') {
                            sh '''
                                mvn org.owasp:dependency-check-maven:check \
                                    -DfailBuildOnCVSS=7 \
                                    -DsuppressedVulnerabilitiesFile=suppressions.xml
                            '''
                        }
                    }
                    post {
                        always {
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'target',
                                reportFiles: 'dependency-check-report.html',
                                reportName: 'OWASP Dependency Check Report'
                            ])
                        }
                    }
                }
            }
        }
        
        stage('Code Quality') {
            steps {
                container('maven') {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.projectKey=${JOB_NAME} \
                                -Dsonar.projectVersion=${IMAGE_TAG} \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                container('docker') {
                    sh '''
                        docker build \
                            --build-arg JAR_FILE=target/*.jar \
                            --build-arg BUILD_VERSION=${IMAGE_TAG} \
                            -t ${FULL_IMAGE_NAME} \
                            -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest \
                            .
                    '''
                }
            }
        }
        
        stage('Docker Security Scan') {
            steps {
                container('docker') {
                    sh '''
                        # Install Trivy
                        curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
                        
                        # Scan image
                        trivy image --exit-code 1 --severity HIGH,CRITICAL ${FULL_IMAGE_NAME}
                    '''
                }
            }
        }
        
        stage('Push Image') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    buildingTag()
                }
            }
            steps {
                container('docker') {
                    sh '''
                        echo ${DOCKER_CREDENTIALS_PSW} | docker login ${DOCKER_REGISTRY} -u ${DOCKER_CREDENTIALS_USR} --password-stdin
                        docker push ${FULL_IMAGE_NAME}
                        docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest
                    '''
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                container('kubectl') {
                    sh '''
                        kubectl set image deployment/spring-boot-app \
                            spring-app=${FULL_IMAGE_NAME} \
                            -n staging
                        
                        kubectl rollout status deployment/spring-boot-app -n staging --timeout=300s
                    '''
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    input message: 'Deploy to production?', 
                          parameters: [choice(name: 'DEPLOY', choices: ['yes', 'no'], description: 'Deploy to production?')]
                }
                container('kubectl') {
                    sh '''
                        # Blue-green deployment
                        kubectl set image deployment/spring-boot-app-green \
                            spring-app=${FULL_IMAGE_NAME} \
                            -n production
                        
                        kubectl rollout status deployment/spring-boot-app-green -n production --timeout=300s
                        
                        # Switch traffic
                        kubectl patch service spring-boot-app-service \
                            -n production \
                            -p '{"spec":{"selector":{"version":"green"}}}'
                    '''
                }
            }
        }
    }
    
    post {
        always {
            container('docker') {
                sh 'docker system prune -f'
            }
        }
        success {
            slackSend(
                channel: '#ci-cd',
                color: 'good',
                message: "✅ Pipeline succeeded: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
            )
        }
        failure {
            slackSend(
                channel: '#ci-cd',
                color: 'danger',
                message: "❌ Pipeline failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
            )
        }
    }
}
```

### Jenkins Shared Library

```groovy
// day82-examples/jenkins/shared-library/vars/buildJavaApp.groovy
def call(Map config) {
    pipeline {
        agent any
        
        environment {
            MAVEN_OPTS = config.mavenOpts ?: '-Xmx1024m'
            JAVA_VERSION = config.javaVersion ?: '17'
        }
        
        stages {
            stage('Setup') {
                steps {
                    script {
                        env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                        env.IMAGE_NAME = config.imageName ?: env.JOB_NAME.toLowerCase()
                    }
                }
            }
            
            stage('Build') {
                steps {
                    buildMavenProject(config)
                }
            }
            
            stage('Test') {
                steps {
                    runTests(config)
                }
            }
            
            stage('Quality Gates') {
                steps {
                    runQualityGates(config)
                }
            }
            
            stage('Package') {
                steps {
                    packageApplication(config)
                }
            }
            
            stage('Deploy') {
                when {
                    expression { config.deploy == true }
                }
                steps {
                    deployApplication(config)
                }
            }
        }
    }
}

def buildMavenProject(config) {
    sh """
        mvn clean compile \
            -Djava.version=${JAVA_VERSION} \
            ${config.buildArgs ?: ''}
    """
}

def runTests(config) {
    parallel(
        'Unit Tests': {
            sh 'mvn test'
            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
        },
        'Integration Tests': {
            if (config.integrationTests != false) {
                sh 'mvn verify -Dskip.unit.tests=true'
                publishTestResults testResultsPattern: 'target/failsafe-reports/*.xml'
            }
        }
    )
}

def runQualityGates(config) {
    if (config.sonarQube != false) {
        withSonarQubeEnv('SonarQube') {
            sh """
                mvn sonar:sonar \
                    -Dsonar.projectKey=${env.JOB_NAME} \
                    -Dsonar.projectVersion=${env.BUILD_VERSION}
            """
        }
        
        timeout(time: 5, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
        }
    }
}

def packageApplication(config) {
    sh 'mvn package -DskipTests=true'
    
    if (config.docker != false) {
        sh """
            docker build \
                -t ${env.IMAGE_NAME}:${env.BUILD_VERSION} \
                -t ${env.IMAGE_NAME}:latest \
                .
        """
    }
}

def deployApplication(config) {
    if (config.kubernetes) {
        sh """
            kubectl set image deployment/${config.kubernetes.deployment} \
                ${config.kubernetes.container}=${env.IMAGE_NAME}:${env.BUILD_VERSION} \
                -n ${config.kubernetes.namespace}
        """
    }
}
```

```groovy
// day82-examples/jenkins/shared-library/usage/Jenkinsfile
@Library('java-pipeline-library') _

buildJavaApp([
    javaVersion: '17',
    mavenOpts: '-Xmx2048m',
    imageName: 'my-spring-app',
    integrationTests: true,
    sonarQube: true,
    docker: true,
    deploy: true,
    kubernetes: [
        deployment: 'spring-boot-app',
        container: 'spring-app',
        namespace: 'production'
    ]
])
```

## GitHub Actions Implementation

### Basic GitHub Actions Workflow

```yaml
# day82-examples/github-actions/.github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '17'
  MAVEN_OPTS: '-Xmx1024m'

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      with:
        fetch-depth: 0  # Full history for SonarQube
    
    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
    
    - name: Run tests
      run: |
        mvn clean verify \
          -Dspring.profiles.active=test \
          -Dspring.datasource.url=jdbc:postgresql://localhost:5432/testdb \
          -Dspring.redis.host=localhost
      env:
        POSTGRES_HOST: localhost
        REDIS_HOST: localhost
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
        flags: unittests
        name: codecov-umbrella
    
    - name: SonarQube Scan
      uses: sonarqube-quality-gate-action@master
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      with:
        scanMetadataReportFile: target/sonar/report-task.txt
```

### Advanced GitHub Actions with Docker

```yaml
# day82-examples/github-actions/.github/workflows/cd.yml
name: CD Pipeline

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
      security-events: write
    
    outputs:
      image: ${{ steps.image.outputs.image }}
      digest: ${{ steps.build.outputs.digest }}
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build application
      run: |
        mvn clean package -DskipTests \
          -Dspring.profiles.active=production
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3
    
    - name: Log in to Container Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=semver,pattern={{version}}
          type=semver,pattern={{major}}.{{minor}}
          type=sha,prefix=sha-
    
    - name: Build and push Docker image
      id: build
      uses: docker/build-push-action@v5
      with:
        context: .
        platforms: linux/amd64,linux/arm64
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max
        build-args: |
          BUILD_VERSION=${{ github.sha }}
          BUILD_DATE=${{ github.event.head_commit.timestamp }}
    
    - name: Output image
      id: image
      run: |
        echo "image=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}" >> $GITHUB_OUTPUT
    
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: ${{ steps.image.outputs.image }}
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: Upload Trivy scan results to GitHub Security tab
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'

  deploy-staging:
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: staging
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-west-2
    
    - name: Deploy to EKS
      run: |
        aws eks update-kubeconfig --name staging-cluster --region us-west-2
        
        kubectl set image deployment/spring-boot-app \
          spring-app=${{ needs.build-and-push.outputs.image }} \
          -n staging
        
        kubectl rollout status deployment/spring-boot-app -n staging --timeout=300s
    
    - name: Run smoke tests
      run: |
        # Wait for deployment to be ready
        sleep 30
        
        # Run smoke tests
        curl -f http://staging.example.com/actuator/health
        curl -f http://staging.example.com/api/v1/health
    
    - name: Notify Slack
      if: always()
      uses: 8398a7/action-slack@v3
      with:
        status: ${{ job.status }}
        channel: '#deployments'
        webhook_url: ${{ secrets.SLACK_WEBHOOK }}

  deploy-production:
    needs: [build-and-push, deploy-staging]
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-west-2
    
    - name: Blue-Green Deployment
      run: |
        aws eks update-kubeconfig --name production-cluster --region us-west-2
        
        # Deploy to green environment
        kubectl set image deployment/spring-boot-app-green \
          spring-app=${{ needs.build-and-push.outputs.image }} \
          -n production
        
        kubectl rollout status deployment/spring-boot-app-green -n production --timeout=600s
        
        # Health check
        kubectl exec deployment/spring-boot-app-green -n production -- \
          curl -f http://localhost:8080/actuator/health
        
        # Switch traffic
        kubectl patch service spring-boot-app-service -n production \
          -p '{"spec":{"selector":{"version":"green"}}}'
        
        # Verify traffic switch
        sleep 60
        curl -f https://api.example.com/actuator/health
    
    - name: Create GitHub Release
      if: startsWith(github.ref, 'refs/tags/')
      uses: actions/create-release@v1
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      with:
        tag_name: ${{ github.ref }}
        release_name: Release ${{ github.ref }}
        body: |
          Docker Image: ${{ needs.build-and-push.outputs.image }}
          SHA: ${{ github.sha }}
        draft: false
        prerelease: false
```

### GitHub Actions Reusable Workflows

```yaml
# day82-examples/github-actions/.github/workflows/java-ci.yml
name: Reusable Java CI

on:
  workflow_call:
    inputs:
      java-version:
        required: false
        type: string
        default: '17'
      maven-args:
        required: false
        type: string
        default: ''
      run-sonar:
        required: false
        type: boolean
        default: true
    secrets:
      SONAR_TOKEN:
        required: false

jobs:
  ci:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
    - uses: actions/checkout@v4
      with:
        fetch-depth: 0
    
    - name: Set up JDK ${{ inputs.java-version }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ inputs.java-version }}
        distribution: 'temurin'
        cache: maven
    
    - name: Run tests
      run: |
        mvn clean verify ${{ inputs.maven-args }} \
          -Dspring.profiles.active=test \
          -Dspring.datasource.url=jdbc:postgresql://localhost:5432/testdb
    
    - name: Publish test results
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Test Results
        path: target/surefire-reports/*.xml
        reporter: java-junit
    
    - name: SonarQube analysis
      if: inputs.run-sonar && secrets.SONAR_TOKEN != ''
      run: |
        mvn sonar:sonar \
          -Dsonar.projectKey=${{ github.repository_owner }}_${{ github.event.repository.name }} \
          -Dsonar.organization=${{ github.repository_owner }} \
          -Dsonar.host.url=https://sonarcloud.io \
          -Dsonar.login=${{ secrets.SONAR_TOKEN }}
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

```yaml
# day82-examples/github-actions/.github/workflows/use-reusable.yml
name: Use Reusable Workflow

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  ci:
    uses: ./.github/workflows/java-ci.yml
    with:
      java-version: '17'
      maven-args: '-Dmaven.compiler.release=17'
      run-sonar: true
    secrets:
      SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

## Advanced CI/CD Patterns

### Multi-Environment Deployment

```yaml
# day82-examples/advanced-patterns/.github/workflows/multi-env.yml
name: Multi-Environment Deployment

on:
  push:
    branches: [ main, develop, 'release/*' ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image: ${{ steps.image.outputs.image }}
      version: ${{ steps.version.outputs.version }}
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Generate version
      id: version
      run: |
        if [[ ${{ github.ref }} == refs/heads/main ]]; then
          echo "version=production-$(date +%Y%m%d)-${{ github.sha }}" >> $GITHUB_OUTPUT
        elif [[ ${{ github.ref }} == refs/heads/develop ]]; then
          echo "version=staging-$(date +%Y%m%d)-${{ github.sha }}" >> $GITHUB_OUTPUT
        else
          echo "version=feature-$(date +%Y%m%d)-${{ github.sha }}" >> $GITHUB_OUTPUT
        fi
    
    - name: Build and push
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.version.outputs.version }}
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
        cache-from: type=gha
        cache-to: type=gha,mode=max
    
    - name: Output image
      id: image
      run: |
        echo "image=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.version.outputs.version }}" >> $GITHUB_OUTPUT

  deploy-dev:
    needs: build
    runs-on: ubuntu-latest
    environment: development
    if: github.ref == 'refs/heads/develop'
    
    steps:
    - name: Deploy to Development
      run: |
        echo "Deploying ${{ needs.build.outputs.image }} to development"
        # Deployment logic here

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment: staging
    if: github.ref == 'refs/heads/develop'
    
    steps:
    - name: Deploy to Staging
      run: |
        echo "Deploying ${{ needs.build.outputs.image }} to staging"
        # Deployment logic here

  deploy-production:
    needs: [build, deploy-staging]
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Deploy to Production
      run: |
        echo "Deploying ${{ needs.build.outputs.image }} to production"
        # Blue-green deployment logic here
```

### GitOps with ArgoCD

```yaml
# day82-examples/gitops/application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: spring-boot-app
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/company/spring-boot-app-config
    targetRevision: HEAD
    path: k8s/overlays/production
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
      allowEmpty: false
    syncOptions:
    - CreateNamespace=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
  revisionHistoryLimit: 10
```

```yaml
# day82-examples/gitops/.github/workflows/gitops.yml
name: GitOps Update

on:
  workflow_run:
    workflows: ["CD Pipeline"]
    types:
      - completed

jobs:
  update-manifests:
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    
    steps:
    - name: Checkout config repo
      uses: actions/checkout@v4
      with:
        repository: company/spring-boot-app-config
        token: ${{ secrets.GITOPS_TOKEN }}
    
    - name: Update image tag
      run: |
        NEW_TAG="${{ github.sha }}"
        
        # Update kustomization files
        find k8s/overlays -name "kustomization.yaml" -exec \
          sed -i "s|newTag: .*|newTag: ${NEW_TAG}|g" {} \;
        
        # Update Helm values
        find helm -name "values*.yaml" -exec \
          sed -i "s|tag: .*|tag: ${NEW_TAG}|g" {} \;
    
    - name: Commit and push
      run: |
        git config --local user.email "action@github.com"
        git config --local user.name "GitHub Action"
        git add .
        git commit -m "Update image tag to ${{ github.sha }}" || exit 0
        git push
```

## Security and Best Practices

### Security Scanning Integration

```yaml
# day82-examples/security/.github/workflows/security.yml
name: Security Scanning

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Monday

jobs:
  sast:
    name: Static Application Security Testing
    runs-on: ubuntu-latest
    permissions:
      security-events: write
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Run CodeQL Analysis
      uses: github/codeql-action/init@v2
      with:
        languages: java
        queries: security-and-quality
    
    - name: Autobuild
      uses: github/codeql-action/autobuild@v2
    
    - name: Perform CodeQL Analysis
      uses: github/codeql-action/analyze@v2
    
    - name: Run Semgrep
      uses: returntocorp/semgrep-action@v1
      with:
        config: >-
          p/security-audit
          p/secrets
          p/owasp-top-ten
        generateSarif: "1"
    
    - name: Upload Semgrep results to GitHub
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: semgrep.sarif

  dependency-check:
    name: Dependency Vulnerability Scan
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Run OWASP Dependency Check
      run: |
        mvn org.owasp:dependency-check-maven:check \
          -DfailBuildOnCVSS=7 \
          -Dformats=HTML,JSON,SARIF
    
    - name: Upload results to GitHub Security
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: target/dependency-check-report.sarif
    
    - name: Upload dependency check report
      uses: actions/upload-artifact@v3
      with:
        name: dependency-check-report
        path: target/dependency-check-report.html

  container-scan:
    name: Container Security Scan
    runs-on: ubuntu-latest
    needs: build
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Build Docker image
      run: docker build -t test-image .
    
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: 'test-image'
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'
    
    - name: Run Snyk Container test
      uses: snyk/actions/docker@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        image: test-image
        args: --severity-threshold=high --file=Dockerfile
```

### Secrets Management

```java
// day82-examples/security/SecretsManager.java
@Component
@Slf4j
public class SecretsManager {
    
    private final Environment environment;
    private final VaultTemplate vaultTemplate;
    
    public SecretsManager(Environment environment, 
                         @Autowired(required = false) VaultTemplate vaultTemplate) {
        this.environment = environment;
        this.vaultTemplate = vaultTemplate;
    }
    
    @PostConstruct
    public void initializeSecrets() {
        if (isKubernetesEnvironment()) {
            loadKubernetesSecrets();
        } else if (isVaultEnabled()) {
            loadVaultSecrets();
        } else {
            loadEnvironmentSecrets();
        }
    }
    
    private boolean isKubernetesEnvironment() {
        return Files.exists(Paths.get("/var/run/secrets/kubernetes.io"));
    }
    
    private boolean isVaultEnabled() {
        return vaultTemplate != null && 
               environment.getProperty("spring.cloud.vault.enabled", Boolean.class, false);
    }
    
    private void loadKubernetesSecrets() {
        Path secretsPath = Paths.get("/app/secrets");
        if (Files.exists(secretsPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(secretsPath)) {
                for (Path secretFile : stream) {
                    String secretName = secretFile.getFileName().toString();
                    String secretValue = Files.readString(secretFile).trim();
                    
                    // Set as system property with prefix
                    System.setProperty("secret." + secretName, secretValue);
                    log.info("Loaded Kubernetes secret: {}", secretName);
                }
            } catch (IOException e) {
                log.error("Failed to load Kubernetes secrets", e);
                throw new RuntimeException("Secret loading failed", e);
            }
        }
    }
    
    private void loadVaultSecrets() {
        try {
            String secretPath = environment.getProperty("app.vault.secret-path", "secret/myapp");
            VaultResponseSupport<?> response = vaultTemplate.read(secretPath);
            
            if (response != null && response.getData() != null) {
                Map<String, Object> secrets = response.getData();
                secrets.forEach((key, value) -> {
                    System.setProperty("secret." + key, value.toString());
                    log.info("Loaded Vault secret: {}", key);
                });
            }
        } catch (Exception e) {
            log.error("Failed to load Vault secrets", e);
            throw new RuntimeException("Vault secret loading failed", e);
        }
    }
    
    private void loadEnvironmentSecrets() {
        // Load secrets from environment variables
        Map<String, String> env = System.getenv();
        env.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("SECRET_"))
            .forEach(entry -> {
                String secretName = entry.getKey()
                    .toLowerCase()
                    .replace("secret_", "secret.");
                System.setProperty(secretName, entry.getValue());
                log.info("Loaded environment secret: {}", secretName);
            });
    }
    
    public String getSecret(String name) {
        String secret = System.getProperty("secret." + name);
        if (secret == null) {
            secret = environment.getProperty("SECRET_" + name.toUpperCase().replace("-", "_"));
        }
        return secret;
    }
    
    public String getRequiredSecret(String name) {
        String secret = getSecret(name);
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("Required secret not found: " + name);
        }
        return secret;
    }
}

@Configuration
@EnableConfigurationProperties(SecretsProperties.class)
public class SecretsConfiguration {
    
    @ConfigurationProperties(prefix = "app.secrets")
    @Data
    public static class SecretsProperties {
        private boolean enabled = true;
        private String provider = "kubernetes"; // kubernetes, vault, environment
        private String vaultPath = "secret/myapp";
        private Duration refreshInterval = Duration.ofMinutes(30);
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.secrets.enabled", havingValue = "true")
    public SecretsRefreshScheduler secretsRefreshScheduler(SecretsProperties properties) {
        return new SecretsRefreshScheduler(properties.getRefreshInterval());
    }
}
```

### Pipeline Monitoring and Observability

```yaml
# day82-examples/monitoring/.github/workflows/pipeline-metrics.yml
name: Pipeline Metrics

on:
  workflow_run:
    workflows: ["CI Pipeline", "CD Pipeline"]
    types: [completed]

jobs:
  collect-metrics:
    runs-on: ubuntu-latest
    
    steps:
    - name: Collect pipeline metrics
      run: |
        # Calculate pipeline duration
        START_TIME="${{ github.event.workflow_run.created_at }}"
        END_TIME="${{ github.event.workflow_run.updated_at }}"
        
        START_TIMESTAMP=$(date -d "$START_TIME" +%s)
        END_TIMESTAMP=$(date -d "$END_TIME" +%s)
        DURATION=$((END_TIMESTAMP - START_TIMESTAMP))
        
        # Send metrics to monitoring system
        curl -X POST "${{ secrets.METRICS_ENDPOINT }}" \
          -H "Authorization: Bearer ${{ secrets.METRICS_TOKEN }}" \
          -H "Content-Type: application/json" \
          -d '{
            "pipeline_name": "${{ github.event.workflow_run.name }}",
            "repository": "${{ github.repository }}",
            "branch": "${{ github.event.workflow_run.head_branch }}",
            "status": "${{ github.event.workflow_run.conclusion }}",
            "duration": '${DURATION}',
            "commit_sha": "${{ github.event.workflow_run.head_sha }}",
            "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
          }'
    
    - name: Update deployment frequency metrics
      if: github.event.workflow_run.name == 'CD Pipeline' && github.event.workflow_run.conclusion == 'success'
      run: |
        # Track deployment frequency
        curl -X POST "${{ secrets.METRICS_ENDPOINT }}/deployments" \
          -H "Authorization: Bearer ${{ secrets.METRICS_TOKEN }}" \
          -H "Content-Type: application/json" \
          -d '{
            "repository": "${{ github.repository }}",
            "environment": "production",
            "commit_sha": "${{ github.event.workflow_run.head_sha }}",
            "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
          }'
```

## Summary

### Key Concepts Covered
- **Jenkins Pipelines**: Declarative and scripted pipelines with Docker integration
- **GitHub Actions**: Workflow automation with advanced features
- **Security Integration**: SAST, DAST, dependency scanning, and container security
- **GitOps Workflows**: Configuration management and automated deployments
- **Multi-Environment Deployments**: Staging, production, and blue-green strategies

### CI/CD Best Practices Implemented
- **Pipeline as Code**: Version-controlled pipeline definitions
- **Automated Testing**: Unit, integration, and security testing
- **Quality Gates**: Code quality and security thresholds
- **Container Security**: Image scanning and vulnerability management
- **Secrets Management**: Secure handling of sensitive data
- **Monitoring**: Pipeline metrics and observability

### Jenkins vs GitHub Actions Comparison

| Feature | Jenkins | GitHub Actions |
|---------|---------|----------------|
| **Hosting** | Self-hosted or cloud | GitHub-hosted or self-hosted |
| **Cost** | Free (self-hosted) | Free tier + usage-based |
| **Flexibility** | Highly customizable | Good customization |
| **Ecosystem** | Extensive plugin ecosystem | Growing marketplace |
| **Learning Curve** | Steeper | Gentler |
| **Integration** | Universal | GitHub-native |

### Next Steps
- Day 83: Monitoring & Observability - Prometheus, Grafana, ELK
- Learn comprehensive monitoring strategies
- Implement observability for microservices
- Master alerting and incident response

---

## Exercises

### Exercise 1: Jenkins Pipeline
Create a comprehensive Jenkins pipeline with testing, security scanning, and deployment stages.

### Exercise 2: GitHub Actions Workflow
Implement a complete GitHub Actions workflow with multi-environment deployment.

### Exercise 3: Security Integration
Add security scanning (SAST, dependency check, container scan) to your CI/CD pipeline.

### Exercise 4: GitOps Implementation
Set up GitOps workflow with ArgoCD for automated deployments.

### Exercise 5: Pipeline Monitoring
Implement comprehensive monitoring and metrics collection for your CI/CD pipelines.