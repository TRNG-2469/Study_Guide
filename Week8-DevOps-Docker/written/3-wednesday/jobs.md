# GitLab CI/CD Jobs

## Learning Objectives

By the end of this lesson, you will be able to:

- Define a complete GitLab CI/CD job with all its key fields
- Use `artifacts` and `cache` to pass files between jobs and speed up pipelines
- Control job execution order with `needs` (DAG pipelines)
- Apply conditional execution with `when` and `rules`
- Configure `retry` and `timeout` for resilience

---

## Why This Matters

Jobs are the atomic unit of work in GitLab CI/CD. Every action your pipeline takes — compiling code, running tests, building a Docker image, deploying to a server — is defined as a job. Understanding how to configure jobs correctly determines how fast, reliable, and maintainable your entire pipeline is. A well-written job is predictable; a poorly written one is a source of mystery failures at 2 AM.

---

## What Is a Job?

A **job** is a named block in `.gitlab-ci.yml` that defines a set of shell commands to run in a specific environment. GitLab assigns each job to a **runner** (a machine that executes it), runs the commands in the `script` block, and reports the result (passed/failed) back to the pipeline.

```yaml
# Minimum viable job — just a name and a script.
hello-world:
  script:
    - echo "I am a GitLab CI/CD job"
```

Every job that does not start with a dot (`.`) and is not a keyword is treated as a real job that will run in the pipeline.

---

## Job Fields

### `stage`

The `stage` keyword assigns a job to a pipeline stage. Jobs in the same stage run in parallel. If you omit `stage`, the job is assigned to the default `test` stage.

```yaml
stages:
  - build
  - test
  - deploy

compile:
  stage: build       # This job belongs to the 'build' stage
  script:
    - mvn clean compile

run-tests:
  stage: test        # This job belongs to the 'test' stage — runs after 'build'
  script:
    - mvn test
```

---

### `image`

The `image` keyword specifies the Docker image the job runs inside. This overrides any global image for this specific job.

```yaml
# Use a specific Maven + JDK image for Java builds.
# Docker Hub images are referenced as 'name:tag'.
build-jar:
  stage: build
  image: maven:3.9-eclipse-temurin-17   # Official Maven image with JDK 17
  script:
    - mvn clean package -DskipTests

# Use a Node.js image for a frontend build.
build-frontend:
  stage: build
  image: node:20-alpine                 # Lightweight Node.js 20 image
  script:
    - npm ci                            # Install dependencies (clean install)
    - npm run build                     # Build the frontend bundle
```

---

### `script`

The `script` keyword contains the list of shell commands the job will execute. Commands run in order — if any command exits with a non-zero code, the job fails and subsequent commands are skipped.

```yaml
test-suite:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - echo "Running unit tests..."
    - mvn test                          # Runs all unit tests
    - echo "Tests complete. Checking coverage..."
    - mvn jacoco:report                 # Generate code coverage report
```

---

### `artifacts`

**Artifacts** are files or directories that GitLab preserves after a job finishes and makes available to downstream jobs and the GitLab UI for download. Without artifacts, every job starts with a clean workspace and has no access to files produced by previous jobs.

```yaml
build-jar:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn clean package -DskipTests
  artifacts:
    # Specify which files or directories to preserve.
    paths:
      - target/*.jar                    # Save the compiled JAR file
      - target/classes/                 # Save compiled class files
    # How long to keep the artifact before GitLab deletes it automatically.
    expire_in: 1 day                    # Options: '30 mins', '1 hour', '1 week', 'never'
    # Name the artifact archive (optional — useful for identification).
    name: "$CI_PROJECT_NAME-$CI_COMMIT_SHORT_SHA"

test-job:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  # By default, GitLab downloads artifacts from all previous stage jobs.
  # The JAR built by 'build-jar' will be available here in the target/ directory.
  script:
    - ls target/                        # Verify the JAR is present
    - mvn test
  artifacts:
    # The 'reports' key tells GitLab to parse these files and display results in the UI.
    reports:
      junit: target/surefire-reports/**/*.xml   # JUnit XML test report
      coverage_report:
        coverage_format: cobertura
        path: target/site/jacoco/jacoco.xml     # Code coverage report
```

**Artifact types for the `reports` key:**

| Report Type | Description |
|---|---|
| `junit` | Test results displayed inline in MR |
| `coverage_report` | Code coverage percentage shown in MR |
| `sast` | Static Application Security Testing results |
| `dotenv` | Environment variables passed to downstream jobs |

---

### `cache`

**Cache** stores directories between pipeline runs to speed up jobs. Unlike artifacts (which pass files between jobs in the same pipeline), cache persists across multiple pipelines for the same branch.

The most common use case is caching dependency downloads (Maven, npm, pip) so they do not need to be re-downloaded on every run.

```yaml
build-with-cache:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  cache:
    # The cache key determines which cache to use.
    # Using the branch name means each branch has its own cache.
    key: "$CI_COMMIT_REF_SLUG"
    paths:
      - .m2/repository/                 # Cache Maven local repository
    # 'pull-push' is the default: download the cache at start, upload at end.
    # 'pull' only downloads. 'push' only uploads.
    policy: pull-push
  script:
    - mvn clean package -Dmaven.repo.local=.m2/repository
  variables:
    # Tell Maven to use the cached local repository inside the project directory.
    MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
```

**Cache vs. Artifacts — key differences:**

| | Cache | Artifacts |
|---|---|---|
| Purpose | Speed up repeated tasks | Pass files between jobs |
| Scope | Across pipelines (same branch) | Within one pipeline |
| Reliability | Best-effort (may be absent) | Guaranteed if job passes |
| Typical content | Dependencies, build tools | Compiled code, reports |

---

### `needs` — DAG (Directed Acyclic Graph) Pipelines

By default, all jobs in a stage wait for all jobs in the previous stage. With `needs`, a job can declare exactly which upstream jobs it depends on and start as soon as those specific jobs finish — even if other jobs in the same stage are still running. This is called a **DAG pipeline**.

```yaml
stages:
  - build
  - test
  - deploy

# Build jobs run in parallel (same stage).
build-backend:
  stage: build
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar

build-frontend:
  stage: build
  script:
    - npm ci && npm run build
  artifacts:
    paths:
      - dist/

# Without 'needs', test-backend would wait for BOTH build jobs.
# With 'needs', it starts as soon as build-backend finishes,
# even if build-frontend is still running.
test-backend:
  stage: test
  needs:
    - job: build-backend              # Only depends on this specific job
      artifacts: true                 # Download artifacts from build-backend
  script:
    - mvn test

test-frontend:
  stage: test
  needs:
    - job: build-frontend
      artifacts: true
  script:
    - npm test

# Deploy only after both test jobs pass.
deploy-app:
  stage: deploy
  needs:
    - test-backend
    - test-frontend
  script:
    - echo "Deploying full application..."
```

---

### `when` — Controlling When a Job Runs

The `when` keyword controls the conditions under which a job executes.

```yaml
# Possible values for 'when':

deploy-staging:
  stage: deploy
  script:
    - ./deploy.sh staging
  when: on_success         # DEFAULT: run only if all previous jobs passed

cleanup-on-failure:
  stage: deploy
  script:
    - ./cleanup.sh
  when: on_failure         # Run only if a previous job FAILED (for cleanup)

always-notify:
  stage: deploy
  script:
    - ./send-notification.sh
  when: always             # Run regardless of previous job results

manual-deploy-prod:
  stage: deploy
  script:
    - ./deploy.sh production
  when: manual             # Requires a human to click "Run" in the GitLab UI
  allow_failure: false     # Blocking: pipeline stays 'pending' until triggered
```

---

### `rules` — Advanced Conditional Execution

`rules` is the modern replacement for `only`/`except`. It provides fine-grained control over when a job runs using a list of conditions evaluated top-to-bottom. The first matching rule determines the job's behavior.

```yaml
deploy-production:
  stage: deploy
  script:
    - ./deploy.sh production
  rules:
    # Rule 1: If pushing to main AND the commit message contains '[skip deploy]',
    # do not run this job at all.
    - if: '$CI_COMMIT_BRANCH == "main" && $CI_COMMIT_MESSAGE =~ /\[skip deploy\]/'
      when: never

    # Rule 2: If this is a merge request pipeline, run but only after manual approval.
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
      when: manual

    # Rule 3: If pushing to main, run automatically.
    - if: '$CI_COMMIT_BRANCH == "main"'
      when: on_success

    # Rule 4 (catch-all): For everything else, don't run this job.
    - when: never

# Run a job ONLY when specific files have changed (useful for monorepos).
build-backend:
  stage: build
  script:
    - mvn clean package
  rules:
    - changes:
        - backend/**/*         # Only run if any file in the backend/ directory changed
        - pom.xml
```

---

### `retry` — Handling Flaky Jobs

Some jobs fail not because of a bug but because of transient issues: network timeouts, temporary resource unavailability, or infrastructure hiccups. The `retry` keyword tells GitLab to automatically retry a failed job.

```yaml
integration-tests:
  stage: test
  script:
    - ./run-integration-tests.sh
  retry:
    max: 2                     # Retry up to 2 times (3 total attempts)
    when:
      - runner_system_failure  # Retry if the runner itself crashed
      - stuck_or_timeout_failure  # Retry if the job got stuck
      - script_failure         # Retry on any script error (use carefully)
```

**Common `retry.when` values:**
- `runner_system_failure` — runner crashed or disconnected
- `stuck_or_timeout_failure` — job exceeded timeout
- `api_failure` — GitLab API returned an error
- `script_failure` — the script itself returned a non-zero exit code

---

### `timeout` — Preventing Runaway Jobs

By default, GitLab allows jobs to run for up to 1 hour. Use `timeout` to set a stricter limit for jobs that should complete quickly.

```yaml
unit-tests:
  stage: test
  script:
    - mvn test
  timeout: 10 minutes          # Fail the job if it takes longer than 10 minutes

long-integration-test:
  stage: test
  script:
    - ./run-full-integration-suite.sh
  timeout: 2 hours             # Allow more time for comprehensive tests
```

---

## Complete Job Example

```yaml
# A fully configured job that uses all major fields.
build-and-package:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  tags:
    - docker                           # Route this job to runners with the 'docker' tag
  variables:
    MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  before_script:
    - java -version                    # Verify environment before running
  script:
    - mvn clean package -DskipTests
    - echo "Build complete: $(ls target/*.jar)"
  after_script:
    - echo "Build job finished with status: $CI_JOB_STATUS"
  artifacts:
    name: "app-$CI_COMMIT_SHORT_SHA"
    paths:
      - target/*.jar
    expire_in: 2 hours
  cache:
    key: "$CI_PROJECT_ID-maven-$CI_COMMIT_REF_SLUG"
    paths:
      - .m2/repository/
  retry:
    max: 1
    when:
      - runner_system_failure
  timeout: 15 minutes
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == "main"'
```

---

## Summary

| Field | Purpose |
|---|---|
| `stage` | Assigns the job to a pipeline stage |
| `image` | Docker image the job runs inside |
| `script` | Shell commands the job executes |
| `artifacts` | Files preserved after the job for downstream use |
| `cache` | Directories cached between pipeline runs |
| `needs` | Declares specific job dependencies (DAG) |
| `when` | Simple condition: on_success, on_failure, always, manual |
| `rules` | Advanced conditional logic with if/changes/when |
| `retry` | Auto-retry on failure with optional condition filter |
| `timeout` | Maximum allowed run time for the job |

---

## External Resources

- [GitLab CI/CD job keywords reference](https://docs.gitlab.com/ee/ci/yaml/#job-keywords)
- [Artifacts in GitLab CI/CD](https://docs.gitlab.com/ee/ci/jobs/job_artifacts.html)
- [Directed Acyclic Graph (DAG) pipelines](https://docs.gitlab.com/ee/ci/directed_acyclic_graph/)
