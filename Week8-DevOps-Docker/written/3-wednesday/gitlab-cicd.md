# GitLab CI/CD — Full Walkthrough

## Learning Objectives

By the end of this lesson, you will be able to:

- Trace the complete journey of a code push through a GitLab CI/CD pipeline
- Explain how runners pick up jobs, execute them, and report results
- Understand how artifacts flow between stages
- Read and write a complete `.gitlab-ci.yml` for a Spring Boot application covering build, test, and deploy stages

---

## Why This Matters

The previous lessons covered individual components — stages, jobs, runners, variables, artifacts. This lesson connects all of them into a single, coherent picture. Seeing how everything works together end-to-end is what transforms theoretical knowledge into practical skill. After this lesson, you will be able to write a production-quality pipeline from scratch and confidently explain what happens at every step.

---

## The Full CI/CD Journey: Code Push to Deployed Application

Here is the complete sequence of events from the moment a developer runs `git push` to the moment a new version of the application is live:

```
Developer machine          GitLab Server               Runner Machine
-----------------          -------------               --------------

1. git push origin main
        │
        ▼
2. GitLab receives push
   Reads .gitlab-ci.yml
   Creates a Pipeline object
   Creates Job objects (one per job)
        │
        ▼
3. Pipeline starts
   Stage 1 (build) begins
   All build-stage Jobs enter "pending" state
        │
        ▼
4. Runner polls GitLab every 3 seconds:
   "Any jobs available for my tags?"
        │
        ▼
5. GitLab assigns a pending job to the runner
        │
        ▼
6. Runner receives job:
   - Clones the repository at $CI_COMMIT_SHA
   - Pulls the Docker image (if Docker executor)
   - Starts the container
   - Runs before_script commands
   - Runs script commands
   - Runs after_script commands
   - Uploads artifacts to GitLab
   - Reports success or failure
        │
        ▼
7. GitLab marks the job passed/failed
   If all jobs in stage 1 pass → Stage 2 begins
   If any job fails → Pipeline fails, later stages skipped
        │
        ▼
8. Stage 2 (test): Runners pick up test jobs
   Artifacts from Stage 1 are downloaded into the runner
   Tests run, results uploaded
        │
        ▼
9. Stage 3 (deploy): Deployment job runs
   Application is pushed to target environment
   GitLab updates the Environments dashboard
        │
        ▼
10. Pipeline marked "passed" — green checkmark in UI
    Developers notified via email / Slack
    MR shows pipeline status for reviewers
```

---

## Complete Spring Boot Pipeline

The following is a real-world `.gitlab-ci.yml` for a Spring Boot application. Every line is annotated. Read it carefully — it uses everything covered this week.

```yaml
# ===========================================================
# .gitlab-ci.yml — Spring Boot Application
# Stages: build → test → package → deploy
# ===========================================================

# ---- Stage Order ----
# Stages are executed left to right.
# All jobs in a stage run in parallel.
# A stage starts only when all jobs in the previous stage pass.
stages:
  - build        # Compile the application
  - test         # Run unit and integration tests
  - package      # Build and push Docker image
  - deploy       # Deploy to the target environment

# ---- Global Variables ----
# Available to every job. Override at job level if needed.
variables:
  # Maven settings: use a project-local directory for the Maven repository.
  # This directory is then cached between pipeline runs.
  MAVEN_OPTS: >-
    -Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository
    -Dmaven.test.failure.ignore=false
  MAVEN_CLI_OPTS: "--batch-mode --no-transfer-progress"

  # Docker image name: uses the GitLab Container Registry for this project.
  # $CI_REGISTRY_IMAGE is predefined: e.g., registry.gitlab.com/mygroup/my-app
  # $CI_COMMIT_SHORT_SHA gives a unique tag per commit: e.g., a1b2c3d4
  DOCKER_IMAGE: "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA"
  DOCKER_IMAGE_LATEST: "$CI_REGISTRY_IMAGE:latest"

# ---- Global Before Script ----
# Runs before EVERY job in the pipeline.
before_script:
  - echo "================================================================"
  - echo "Job: $CI_JOB_NAME | Stage: $CI_JOB_STAGE"
  - echo "Branch: $CI_COMMIT_BRANCH | Commit: $CI_COMMIT_SHORT_SHA"
  - echo "Pipeline: $CI_PIPELINE_ID | Triggered by: $CI_PIPELINE_SOURCE"
  - echo "================================================================"

# ---- Reusable Template for Maven Jobs ----
# A "hidden" job (starts with dot) — GitLab does not run it directly.
# Other jobs use 'extends' to inherit these settings.
.maven-base:
  image: maven:3.9-eclipse-temurin-17    # Use Maven 3.9 with Java 17 (LTS)
  tags:
    - docker                             # Route only to runners with the 'docker' tag
  cache:
    # Cache key per project per branch — each branch has its own Maven cache.
    key: "$CI_PROJECT_ID-maven-$CI_COMMIT_REF_SLUG"
    paths:
      - .m2/repository/                  # The local Maven repository directory
    policy: pull-push                    # Download cache at start, upload any changes at end

# ===========================================================
# STAGE 1: BUILD
# Compile the source code and produce a JAR artifact.
# ===========================================================

compile:
  extends: .maven-base                   # Inherit image, tags, and cache from template
  stage: build
  script:
    # Compile only — skip tests in this stage (tests run in the next stage).
    # $MAVEN_CLI_OPTS applies global Maven flags (batch mode, quiet transfer).
    - mvn $MAVEN_CLI_OPTS compile
    - echo "Compilation successful. Class files located in target/classes/"
  artifacts:
    # Save the compiled class files so test jobs don't need to recompile.
    paths:
      - target/classes/
      - target/generated-sources/
    expire_in: 1 hour                    # Discard after 1 hour — tests will run by then

# ===========================================================
# STAGE 2: TEST
# Run unit tests and generate coverage reports.
# Two jobs run in parallel: unit tests and static analysis.
# ===========================================================

unit-tests:
  extends: .maven-base
  stage: test
  # 'needs' makes this job start as soon as 'compile' finishes,
  # without waiting for other build-stage jobs (DAG behavior).
  needs:
    - job: compile
      artifacts: true                    # Download artifacts (compiled classes) from 'compile'
  script:
    - mvn $MAVEN_CLI_OPTS test           # Run all JUnit tests
    - echo "Unit tests complete. See artifacts for reports."
  artifacts:
    when: always                         # Preserve reports even if tests fail
    paths:
      - target/surefire-reports/         # JUnit XML reports (also parsed by 'reports' below)
      - target/site/jacoco/              # JaCoCo HTML coverage report
    reports:
      # GitLab parses this XML and shows test results inline in Merge Requests.
      junit: target/surefire-reports/**/*.xml
      # GitLab parses this and shows a coverage percentage in the pipeline and MR.
      coverage_report:
        coverage_format: cobertura
        path: target/site/jacoco/jacoco.xml
    expire_in: 1 week
  # Extract the coverage percentage from Maven output for GitLab's coverage tracking.
  # GitLab uses the regex to find the percentage in the job log.
  coverage: '/Total.*?([0-9]{1,3})%/'
  retry:
    max: 1                               # Retry once on failure (handles flaky infrastructure)
    when:
      - runner_system_failure

# Run a static analysis check in parallel with unit tests.
# Jobs in the same stage run simultaneously — this saves time.
code-quality:
  extends: .maven-base
  stage: test
  needs:
    - job: compile
      artifacts: true
  script:
    # Run SpotBugs static analysis (configured as a Maven plugin in pom.xml).
    - mvn $MAVEN_CLI_OPTS spotbugs:check
  artifacts:
    paths:
      - target/spotbugsXml.xml
    reports:
      # GitLab will display code quality findings inline in Merge Requests.
      codequality: target/spotbugsXml.xml
    expire_in: 1 week
  allow_failure: true                    # Don't fail the pipeline for code quality issues (yet)

# ===========================================================
# STAGE 3: PACKAGE
# Build a production JAR, create a Docker image, push to registry.
# ===========================================================

build-jar:
  extends: .maven-base
  stage: package
  needs:
    - job: unit-tests                    # Only package if tests passed
  script:
    # Package with all tests skipped — tests already ran in stage 2.
    - mvn $MAVEN_CLI_OPTS package -DskipTests
    - echo "JAR built: $(ls target/*.jar)"
  artifacts:
    name: "spring-boot-app-$CI_COMMIT_SHORT_SHA"
    paths:
      - target/*.jar                     # The final executable JAR
    expire_in: 1 week

build-docker:
  stage: package
  image: docker:24.0                     # Use the official Docker CLI image
  services:
    # Docker-in-Docker (dind) service: allows building Docker images inside a container.
    - name: docker:24.0-dind
      alias: docker
  tags:
    - docker
  needs:
    - job: build-jar
      artifacts: true                    # Download the JAR artifact
  variables:
    # Required for Docker-in-Docker communication.
    DOCKER_HOST: tcp://docker:2376
    DOCKER_TLS_CERTDIR: "/certs"
    DOCKER_TLS_VERIFY: 1
    DOCKER_CERT_PATH: "$DOCKER_TLS_CERTDIR/client"
  before_script:
    # Authenticate to the GitLab Container Registry.
    # CI_REGISTRY_USER and CI_REGISTRY_PASSWORD are predefined — no manual setup needed.
    - docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"
  script:
    # Build the Docker image.
    # The Dockerfile should be in the root of the repository.
    - |
      docker build \
        --label "git.commit=$CI_COMMIT_SHA" \
        --label "git.branch=$CI_COMMIT_BRANCH" \
        --label "pipeline.id=$CI_PIPELINE_ID" \
        -t "$DOCKER_IMAGE" \
        -t "$DOCKER_IMAGE_LATEST" \
        .
    # Push both the versioned tag and 'latest'.
    - docker push "$DOCKER_IMAGE"
    - docker push "$DOCKER_IMAGE_LATEST"
    - echo "Image pushed: $DOCKER_IMAGE"
  after_script:
    - docker logout "$CI_REGISTRY"       # Always log out, even if the job failed
  rules:
    # Only build the Docker image for commits to main or release branches.
    # Feature branch commits just need build + test, not packaging.
    - if: '$CI_COMMIT_BRANCH == "main"'
    - if: '$CI_COMMIT_BRANCH =~ /^release\//'
    - if: '$CI_COMMIT_TAG'               # Also run for Git tags (release versions)

# ===========================================================
# STAGE 4: DEPLOY
# Deploy the Docker image to staging (auto) and production (manual).
# ===========================================================

deploy-staging:
  stage: deploy
  image: alpine:latest
  tags:
    - docker
  needs:
    - job: build-docker
  before_script:
    # Install SSH client and kubectl for deployment (lightweight Alpine image).
    - apk add --no-cache openssh-client curl
    # Configure SSH key from a protected/masked CI variable.
    # DEPLOY_SSH_KEY is set in GitLab UI: Settings > CI/CD > Variables.
    - mkdir -p ~/.ssh
    - echo "$DEPLOY_SSH_KEY" > ~/.ssh/id_ed25519
    - chmod 600 ~/.ssh/id_ed25519
    - ssh-keyscan -H "$STAGING_SERVER_IP" >> ~/.ssh/known_hosts
  script:
    - echo "Deploying $DOCKER_IMAGE to staging..."
    - |
      ssh deploy@$STAGING_SERVER_IP "
        docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY &&
        docker pull $DOCKER_IMAGE &&
        docker stop spring-app || true &&
        docker rm spring-app || true &&
        docker run -d \
          --name spring-app \
          --restart unless-stopped \
          -p 8080:8080 \
          -e SPRING_PROFILES_ACTIVE=staging \
          $DOCKER_IMAGE
      "
    - echo "Staging deployment complete."
  # Declare an 'environment' so GitLab tracks deployments in the Environments dashboard.
  environment:
    name: staging
    url: https://staging.myapp.example.com
    # GitLab will prompt to deploy to this URL and track deployment history.
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'

# Production deploy is MANUAL — a human must click "Run" in the GitLab UI.
# This is a common safety gate before touching production.
deploy-production:
  stage: deploy
  image: alpine:latest
  tags:
    - docker
  needs:
    - job: deploy-staging              # Production requires staging to have succeeded
  before_script:
    - apk add --no-cache openssh-client
    - mkdir -p ~/.ssh
    - echo "$PROD_DEPLOY_SSH_KEY" > ~/.ssh/id_ed25519
    - chmod 600 ~/.ssh/id_ed25519
    - ssh-keyscan -H "$PROD_SERVER_IP" >> ~/.ssh/known_hosts
  script:
    - echo "Deploying $DOCKER_IMAGE to PRODUCTION..."
    - |
      ssh deploy@$PROD_SERVER_IP "
        docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY &&
        docker pull $DOCKER_IMAGE &&
        docker stop spring-app || true &&
        docker rm spring-app || true &&
        docker run -d \
          --name spring-app \
          --restart unless-stopped \
          -p 8080:8080 \
          -e SPRING_PROFILES_ACTIVE=production \
          $DOCKER_IMAGE
      "
    - echo "Production deployment complete."
  environment:
    name: production
    url: https://myapp.example.com
  when: manual                         # Requires a human to trigger — never auto-deploys to prod
  allow_failure: false                 # Pipeline stays in 'blocked' state until triggered
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
```

---

## Matching Dockerfile

The pipeline above expects a `Dockerfile` in the repository root. Here is a production-ready multi-stage Dockerfile for a Spring Boot application:

```dockerfile
# Stage 1: Build (only needed at build time — not included in final image)
# Using multi-stage builds keeps the final image small and secure.
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy dependency descriptor first — Docker caches this layer if pom.xml hasn't changed.
COPY pom.xml .
RUN mvn dependency:go-offline -q
# Copy source and build.
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime image
# Use a minimal JRE image — no build tools, no Maven, smaller attack surface.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy only the JAR from the builder stage.
COPY --from=builder /app/target/*.jar app.jar
# Expose the application port.
EXPOSE 8080
# Run the application.
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## What to Watch in the GitLab UI

After pushing code, go to **Build > Pipelines** in your project:

1. You will see a new pipeline row with status "running"
2. Click the pipeline to see the stage graph
3. Click any job box to see its real-time log output
4. After unit-tests completes, check **Build > Artifacts** to download test reports
5. Go to **Operate > Environments** to see deployment history for staging and production
6. In an open Merge Request, the pipeline status appears at the bottom with a link to test results

---

## Summary

The complete pipeline flow:
1. **Developer pushes** → GitLab reads `.gitlab-ci.yml` → Pipeline created
2. **compile** (build stage) → Produces class files → Saved as artifact
3. **unit-tests + code-quality** (test stage, parallel) → Consumes compile artifact → Produces reports
4. **build-jar + build-docker** (package stage) → Produces JAR and Docker image → Pushes to registry
5. **deploy-staging** (deploy stage, auto on main) → Pulls image → Runs on staging server
6. **deploy-production** (deploy stage, manual) → Human approves → Runs on production server

---

## External Resources

- [GitLab CI/CD quick start](https://docs.gitlab.com/ee/ci/quick_start/)
- [GitLab Container Registry](https://docs.gitlab.com/ee/user/packages/container_registry/)
- [GitLab Environments and deployments](https://docs.gitlab.com/ee/ci/environments/)
