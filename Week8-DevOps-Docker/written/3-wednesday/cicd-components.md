# GitLab CI/CD Components

## Learning Objectives

By the end of this lesson, you will be able to:

- Describe the structure and purpose of a `.gitlab-ci.yml` file
- Define stages and control job execution order
- Use global variables, `before_script`, and `after_script` to reduce repetition
- Apply `extends` to reuse job configurations
- Use `include` to split a large CI configuration across multiple files

---

## Why This Matters

Every automated pipeline needs a blueprint — a file that tells the system what to do, in what order, and under what conditions. In GitLab CI/CD, that blueprint is the `.gitlab-ci.yml` file. Understanding how to structure this file effectively is the foundation of everything else you will build this week. Without a solid grasp of its components, pipelines become hard to maintain, difficult to debug, and impossible to scale across multiple projects.

---

## What Is `.gitlab-ci.yml`?

The `.gitlab-ci.yml` file is a YAML-formatted configuration file that lives in the root of your Git repository. When you push code to GitLab, the platform reads this file and uses it to create a **pipeline** — a series of automated steps that build, test, and deploy your application.

Think of it like a recipe card. The recipe card (`.gitlab-ci.yml`) describes all the steps a chef (GitLab Runner) must follow to produce the final dish (a deployed application). If the recipe is well-organized, the chef can work efficiently and consistently every time.

---

## Core Concepts

### 1. The `stages` Array

The `stages` keyword defines the top-level phases of your pipeline and the **order** in which they run. All jobs within a stage run in parallel. Stages run sequentially — a later stage only begins if all jobs in the previous stage succeeded.

```yaml
# The stages keyword defines the order of execution.
# Jobs assigned to 'build' will all run first.
# Only if ALL build jobs pass will 'test' jobs start.
# Only if ALL test jobs pass will 'deploy' jobs start.
stages:
  - build
  - test
  - deploy
```

If you do not define `stages`, GitLab uses three default stages: `build`, `test`, and `deploy`.

---

### 2. Global Variables

The `variables` keyword at the top level of the file defines environment variables that are available to **every job** in the pipeline. This avoids repeating the same values across dozens of jobs.

```yaml
# Global variables are inherited by every job in the pipeline.
# They can be referenced using the $VARIABLE_NAME syntax.
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  APP_NAME: "my-spring-boot-app"
  DOCKER_REGISTRY: "registry.example.com"

stages:
  - build
  - test
  - deploy
```

Variables can also be overridden at the job level — a job-level variable with the same name will take precedence over the global one.

---

### 3. `before_script` and `after_script`

These keywords define commands that run **before** or **after** the main `script` block in every job (when defined globally) or in a specific job (when defined locally).

**Global `before_script`** — runs before every job's `script`:

```yaml
# This before_script runs before EVERY job in the pipeline.
# Use it for setup tasks that every job needs, like logging in to a registry.
before_script:
  - echo "Pipeline started. Job name: $CI_JOB_NAME"
  - docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"

stages:
  - build
  - test
```

**`after_script`** — runs after every job, even if the job fails. This makes it ideal for cleanup tasks.

```yaml
# after_script always executes, regardless of whether the job succeeded or failed.
# Use it to clean up resources, send notifications, or log results.
after_script:
  - echo "Job $CI_JOB_NAME finished with status: $CI_JOB_STATUS"
  - docker logout "$CI_REGISTRY"
```

**Job-level override** — you can define `before_script` inside a specific job to replace the global one for that job only:

```yaml
build-job:
  stage: build
  # This before_script replaces the global one for this job only.
  before_script:
    - echo "Custom setup just for the build job"
  script:
    - mvn clean package
```

---

### 4. `extends` — Reusing Job Configurations

As your pipeline grows, you will notice that many jobs share common settings: the same Docker image, the same tags, the same `before_script`. The `extends` keyword lets you define a **template job** (prefixed with a dot `.` so GitLab ignores it as a real job) and reuse its configuration in other jobs.

```yaml
# A "hidden" job — GitLab ignores any job whose name starts with a dot.
# This is used purely as a template for other jobs to extend.
.java-base:
  image: maven:3.9-eclipse-temurin-17   # Use this Docker image for all extending jobs
  tags:
    - docker                             # Route to runners tagged 'docker'
  before_script:
    - java -version                      # Confirm Java version before running
    - mvn --version                      # Confirm Maven version before running
  cache:
    paths:
      - .m2/repository/                  # Cache Maven dependencies between runs

# This job 'extends' the .java-base template.
# It inherits image, tags, before_script, and cache from the template.
build-app:
  extends: .java-base                    # Pull in all settings from .java-base
  stage: build
  script:
    - mvn clean package -DskipTests      # Add only the job-specific commands here

# This job also extends .java-base and adds its own script.
unit-tests:
  extends: .java-base
  stage: test
  script:
    - mvn test                           # Run unit tests
  artifacts:
    reports:
      junit: target/surefire-reports/**/*.xml   # Publish test results to GitLab UI
```

When a job uses `extends`, GitLab performs a **deep merge** of the two configurations. The job's own settings take priority, so you can override individual fields from the template without losing the rest.

---

### 5. `include` — Splitting Configuration Across Multiple Files

Large projects may have pipelines with dozens of jobs. Putting everything in one `.gitlab-ci.yml` file makes it hard to read and maintain. The `include` keyword lets you pull in configuration from other YAML files — either local files in your repository or remote URLs.

**Local include** — reference another file in the same repository:

```yaml
# Main .gitlab-ci.yml — acts as the entry point for the entire pipeline.
stages:
  - build
  - test
  - deploy

# Pull in job definitions from separate files.
# GitLab merges these files together at pipeline creation time.
include:
  - local: '/ci/build-jobs.yml'      # Jobs related to compiling and packaging
  - local: '/ci/test-jobs.yml'       # Jobs related to running tests
  - local: '/ci/deploy-jobs.yml'     # Jobs related to deploying
```

**Remote include** — pull in a shared template from another project or a URL:

```yaml
include:
  # Include a template maintained by another team in a different GitLab project.
  - project: 'company/shared-ci-templates'
    ref: main                            # Use the 'main' branch of that project
    file: '/templates/docker-build.yml'  # Path to the template file

  # Include a file from a public URL (must be HTTPS).
  - remote: 'https://gitlab.com/gitlab-org/gitlab/-/raw/master/lib/gitlab/ci/templates/Docker.gitlab-ci.yml'
```

**Example split file — `/ci/build-jobs.yml`:**

```yaml
# This file contains only build-stage jobs.
# It is included by the main .gitlab-ci.yml.

build-docker-image:
  stage: build
  image: docker:24.0
  services:
    - docker:24.0-dind              # Docker-in-Docker service for building images
  script:
    - docker build -t $DOCKER_REGISTRY/$APP_NAME:$CI_COMMIT_SHORT_SHA .
    - docker push $DOCKER_REGISTRY/$APP_NAME:$CI_COMMIT_SHORT_SHA
```

---

## Putting It All Together

Here is a complete, annotated `.gitlab-ci.yml` that uses every concept covered above:

```yaml
# ============================================================
# .gitlab-ci.yml — Complete example using all core components
# ============================================================

# Define the order of pipeline stages.
# Jobs run stage by stage; within a stage, jobs run in parallel.
stages:
  - build
  - test
  - deploy

# Global variables available to every job.
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  IMAGE_TAG: "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA"

# Runs before EVERY job's script block.
before_script:
  - echo "Starting job: $CI_JOB_NAME on branch: $CI_COMMIT_REF_NAME"

# Runs after EVERY job, even on failure.
after_script:
  - echo "Finished job: $CI_JOB_NAME — Status: $CI_JOB_STATUS"

# ---- TEMPLATE (hidden job, not executed directly) ----
.java-base:
  image: maven:3.9-eclipse-temurin-17
  tags:
    - docker
  cache:
    key: "$CI_PROJECT_ID-maven"
    paths:
      - .m2/repository/

# ---- INCLUDED FILES ----
# Additional job definitions live in the /ci directory.
include:
  - local: '/ci/security-scan.yml'

# ---- BUILD STAGE ----
build-jar:
  extends: .java-base           # Inherit image, tags, and cache from template
  stage: build
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar            # Pass the compiled JAR to downstream jobs
    expire_in: 1 hour           # Automatically delete artifacts after 1 hour

# ---- TEST STAGE ----
unit-tests:
  extends: .java-base
  stage: test
  script:
    - mvn test
  artifacts:
    reports:
      junit: target/surefire-reports/**/*.xml

# ---- DEPLOY STAGE ----
deploy-staging:
  stage: deploy
  image: alpine:latest
  script:
    - echo "Deploying $IMAGE_TAG to staging..."
    - ./scripts/deploy.sh staging   # Call a deployment script
  environment:
    name: staging
    url: https://staging.example.com
  only:
    - main                          # Only deploy when pushing to the main branch
```

---

## Summary

| Component | Purpose |
|---|---|
| `stages` | Defines pipeline phases and their execution order |
| `variables` | Sets environment variables available to all jobs |
| `before_script` | Commands that run before every job's main script |
| `after_script` | Commands that always run after every job (even on failure) |
| `extends` | Reuses settings from a template job to avoid duplication |
| `include` | Pulls in job definitions from other YAML files |

---

## External Resources

- [GitLab CI/CD YAML reference (official docs)](https://docs.gitlab.com/ee/ci/yaml/)
- [GitLab CI/CD tutorial for beginners](https://docs.gitlab.com/ee/ci/quick_start/)
- [Using `extends` to reuse configuration](https://docs.gitlab.com/ee/ci/yaml/yaml_optimization.html)
