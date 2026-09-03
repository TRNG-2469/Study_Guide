# GitLab Pipeline Types

## Learning Objectives

By the end of this lesson, you will be able to:

- Identify and explain the different types of GitLab pipelines
- Understand when each pipeline type is triggered
- Read the GitLab pipeline visualization UI
- Configure downstream pipelines and cross-project triggers

---

## Why This Matters

Not every code change needs the same automated response. A developer pushing a quick fix to a feature branch should not trigger a production deployment. A nightly build should run on a schedule, not on every commit. GitLab gives you multiple pipeline types so you can match the right level of automation to the right event. Knowing which type to use — and when — is the difference between a CI/CD system that helps your team and one that slows it down.

---

## Core Concept: What Is a Pipeline?

A **pipeline** is a collection of jobs organized into stages that GitLab CI/CD runs automatically in response to an event (a push, a merge request, a schedule, etc.). Every pipeline has:

- A **trigger** — the event that starts it
- A **status** — running, passed, failed, or canceled
- One or more **stages** containing one or more **jobs**

Think of a pipeline like an airport security checkpoint. Every passenger (code change) must pass through a series of checkpoints (stages: ID check, X-ray, boarding gate scan) in order. If any checkpoint fails, the passenger does not board. If all checkpoints pass, the passenger reaches the destination (production).

---

## Pipeline Types

### 1. Branch Pipelines

A **branch pipeline** is triggered when a developer pushes commits to a branch. This is the most common pipeline type.

**When it runs:** Every `git push` to any branch (unless restricted by `rules` or `only`/`except`).

**Common use:** Run tests and builds on every commit so developers get immediate feedback.

```yaml
# This job runs on EVERY branch pipeline by default.
# No special configuration is needed — branch pipelines are the default.
build-app:
  stage: build
  script:
    - mvn clean package

# Restrict a job to run ONLY on the main branch using 'rules'.
deploy-production:
  stage: deploy
  script:
    - ./deploy.sh production
  rules:
    # $CI_COMMIT_BRANCH is a predefined variable set by GitLab.
    # This rule says: only run this job when the branch is 'main'.
    - if: '$CI_COMMIT_BRANCH == "main"'
```

---

### 2. Merge Request Pipelines

A **merge request (MR) pipeline** runs in the context of a merge request — it tests what the code will look like *after* the merge, before the actual merge happens. This gives reviewers confidence that merging will not break anything.

**When it runs:** When a merge request is opened or when new commits are pushed to the source branch of an open MR.

**Key difference from branch pipelines:** MR pipelines have access to the merge result (the merged code), not just the source branch.

```yaml
# To enable merge request pipelines, use rules with $CI_PIPELINE_SOURCE.
# $CI_PIPELINE_SOURCE == "merge_request_event" is true for MR pipelines.

stages:
  - test
  - build

# This job runs ONLY during merge request pipelines.
mr-test:
  stage: test
  script:
    - mvn test
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'

# This job runs on branch pipelines (push to any branch) but NOT MR pipelines.
branch-test:
  stage: test
  script:
    - mvn test
  rules:
    - if: '$CI_PIPELINE_SOURCE == "push"'
```

**Best practice — avoid duplicate pipelines:** When you use both branch and MR pipelines, GitLab may run two pipelines for the same commit. Use the following pattern to prevent this:

```yaml
# This pattern ensures a job runs in an MR pipeline OR on the main branch,
# but NOT as a redundant branch pipeline when an MR already exists.
test-job:
  stage: test
  script:
    - mvn test
  rules:
    # Run if this is a merge request pipeline
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    # OR run if pushing directly to main (no open MR)
    - if: '$CI_COMMIT_BRANCH == "main"'
```

---

### 3. Scheduled Pipelines

A **scheduled pipeline** runs automatically at a specific time, like a cron job. It is not triggered by a code push — it runs on a timer regardless of recent activity.

**When it runs:** On a user-defined schedule (e.g., every night at 2 AM, every Monday morning).

**Common use cases:**
- Nightly full test suite (slow tests skipped during normal pushes)
- Weekly dependency vulnerability scans
- Automated database backups

**How to configure a schedule:**
1. In your GitLab project, go to **Build > Pipeline schedules**
2. Click **New schedule**
3. Set a description, interval (cron syntax), target branch, and any custom variables
4. Save

GitLab sets the `$CI_PIPELINE_SOURCE` variable to `"schedule"` for scheduled runs. You can use this in your `rules` to run certain jobs only on a schedule:

```yaml
# This job runs ONLY during scheduled pipelines (not on push or MR events).
# Use it for long-running or resource-intensive jobs you don't want on every commit.
nightly-full-test-suite:
  stage: test
  script:
    - mvn verify -P full-test-suite   # Run ALL tests, including slow integration tests
  rules:
    - if: '$CI_PIPELINE_SOURCE == "schedule"'

# This job runs on regular pushes but SKIPS the scheduled pipeline.
quick-unit-tests:
  stage: test
  script:
    - mvn test
  rules:
    - if: '$CI_PIPELINE_SOURCE != "schedule"'
```

You can also pass **custom variables** to a scheduled pipeline. In the schedule configuration, add a variable like `NIGHTLY=true`. Then reference it in your jobs:

```yaml
dependency-scan:
  stage: test
  script:
    - mvn dependency-check:check
  rules:
    # Only run this expensive scan when the NIGHTLY variable is set to "true"
    - if: '$NIGHTLY == "true"'
```

---

### 4. Parent-Child Pipelines

As a project grows, a single `.gitlab-ci.yml` can become enormous and slow. **Parent-child pipelines** allow you to split your pipeline into a parent pipeline that triggers one or more child pipelines, each with their own YAML configuration.

**When to use:** Monorepos (a single repository containing multiple services), or any project where different parts of the codebase need independent pipeline logic.

**How it works:**
1. The parent pipeline detects which part of the codebase changed
2. It triggers the relevant child pipelines using the `trigger` keyword
3. Each child runs its own jobs independently

```yaml
# ---- Parent .gitlab-ci.yml ----

stages:
  - triggers   # This stage launches child pipelines

# Trigger a child pipeline for the backend service.
# The child pipeline is defined in backend/.gitlab-ci.yml.
trigger-backend:
  stage: triggers
  trigger:
    include: backend/.gitlab-ci.yml   # Path to the child pipeline configuration
    strategy: depend                  # Make the parent wait for the child to finish
  rules:
    # Only trigger the backend child pipeline if backend files changed.
    - changes:
        - backend/**/*
        - backend/.gitlab-ci.yml

# Trigger a child pipeline for the frontend service.
trigger-frontend:
  stage: triggers
  trigger:
    include: frontend/.gitlab-ci.yml
    strategy: depend
  rules:
    - changes:
        - frontend/**/*
        - frontend/.gitlab-ci.yml
```

```yaml
# ---- backend/.gitlab-ci.yml (Child pipeline) ----
# This is a fully independent pipeline configuration.
# It has its own stages, variables, and jobs.

stages:
  - build
  - test

build-backend:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd backend && mvn clean package

test-backend:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd backend && mvn test
```

The `strategy: depend` setting makes the parent pipeline wait for all child pipelines to finish before marking itself as passed or failed. Without it, the parent treats the trigger job as immediately successful.

---

### 5. Downstream Pipelines and Cross-Project Triggers

Beyond parent-child pipelines (which live in the same repository), GitLab supports **cross-project pipelines** — triggering a pipeline in a completely different GitLab project.

**When to use:** When one project's deployment depends on a shared library being built, or when a microservice deployment should trigger integration tests in a separate test repository.

```yaml
# ---- In Project A (.gitlab-ci.yml) ----

# After Project A is built and tested, trigger a pipeline in Project B.
trigger-integration-tests:
  stage: deploy
  trigger:
    # Reference the OTHER project using its full GitLab path.
    project: 'company-group/integration-test-repo'
    branch: main                  # Which branch of Project B to run
    strategy: depend              # Wait for Project B's pipeline to finish
  variables:
    # Pass information to the downstream pipeline using variables.
    UPSTREAM_PROJECT: "$CI_PROJECT_NAME"
    UPSTREAM_COMMIT: "$CI_COMMIT_SHA"
```

The downstream project receives the custom variables and can use them in its own jobs:

```yaml
# ---- In Project B (.gitlab-ci.yml) ----

run-integration-tests:
  stage: test
  script:
    # Use the variable passed from Project A
    - echo "Running tests triggered by $UPSTREAM_PROJECT at commit $UPSTREAM_COMMIT"
    - ./run-integration-tests.sh
```

---

## Pipeline Visualization in the GitLab UI

GitLab provides a visual pipeline graph that makes it easy to understand the status and flow of a pipeline. To access it:

1. Go to your project in GitLab
2. Click **Build > Pipelines**
3. Click on any pipeline to open its detail view

The pipeline graph shows:
- **Stages** as columns (left to right)
- **Jobs** as clickable boxes within each stage
- **Status icons** — green checkmark (passed), red X (failed), spinning circle (running), gray circle (pending)
- **Job logs** — click any job box to see its real-time or historical output

**Reading job status colors:**
| Color | Status | Meaning |
|---|---|---|
| Green | Passed | Job completed successfully |
| Red | Failed | Job exited with a non-zero code |
| Blue | Running | Job is currently executing |
| Gray | Pending | Waiting for a runner to pick it up |
| Orange | Manual | Requires a human to click "Run" |
| Yellow | Canceled | Stopped before completion |

---

## Summary

| Pipeline Type | Trigger | Common Use |
|---|---|---|
| Branch pipeline | `git push` to any branch | Build and test on every commit |
| Merge request pipeline | Open/update an MR | Test merge result before merging |
| Scheduled pipeline | Cron schedule | Nightly builds, weekly scans |
| Parent-child pipeline | `trigger` job in parent | Monorepos, independent sub-pipelines |
| Cross-project pipeline | `trigger` with `project:` | Downstream integration tests |

---

## External Resources

- [GitLab pipeline types overview](https://docs.gitlab.com/ee/ci/pipelines/)
- [Merge request pipelines](https://docs.gitlab.com/ee/ci/pipelines/merge_request_pipelines.html)
- [Parent-child pipelines](https://docs.gitlab.com/ee/ci/pipelines/parent_child_pipelines.html)
