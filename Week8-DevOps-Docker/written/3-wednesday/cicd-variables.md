# GitLab CI/CD Variables

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain the difference between predefined and custom CI/CD variables
- Use commonly needed predefined variables in your pipeline jobs
- Define project-level and group-level custom variables in the GitLab UI
- Protect and mask sensitive variables so they are not exposed in logs
- Pass variables between jobs using dotenv artifacts

---

## Why This Matters

Hardcoding values like server addresses, credentials, or version numbers directly into your `.gitlab-ci.yml` is a maintenance nightmare and a security risk. CI/CD variables let you externalize configuration — keeping secrets out of your source code, making pipelines portable across environments, and enabling dynamic behavior based on context (which branch is building, who triggered the pipeline, what time it is). Every professional CI/CD pipeline relies heavily on variables.

---

## What Are CI/CD Variables?

A **CI/CD variable** is a key-value pair that is injected into the environment of every job that runs in your pipeline. Inside a job's `script`, you access variables using the standard shell syntax: `$VARIABLE_NAME` (Linux/macOS) or `%VARIABLE_NAME%` (Windows).

Variables come from several sources, applied in this priority order (highest wins):

1. Trigger variables / scheduled pipeline variables
2. Project-level CI/CD variables (set in GitLab UI)
3. Group-level CI/CD variables
4. Instance-level CI/CD variables
5. Variables in `.gitlab-ci.yml` (`variables:` keyword)
6. GitLab predefined variables

---

## Predefined Variables

GitLab automatically injects dozens of variables into every pipeline. You do not need to define them — they are always available.

### Most Commonly Used Predefined Variables

```yaml
# This job demonstrates the most important predefined variables.
# In real jobs, you use these in script commands, not just echo them.
show-predefined-vars:
  stage: build
  script:
    # --- Pipeline and Job Identity ---
    - echo "Pipeline ID: $CI_PIPELINE_ID"
    # Unique numeric ID for this pipeline run. Example: 12345678

    - echo "Job ID: $CI_JOB_ID"
    # Unique numeric ID for this specific job. Example: 98765432

    - echo "Job Name: $CI_JOB_NAME"
    # The name of this job as defined in .gitlab-ci.yml. Example: show-predefined-vars

    - echo "Job Stage: $CI_JOB_STAGE"
    # The stage this job belongs to. Example: build

    # --- Commit and Branch Information ---
    - echo "Full commit SHA: $CI_COMMIT_SHA"
    # Full 40-character Git commit hash. Example: a1b2c3d4e5f6...

    - echo "Short commit SHA: $CI_COMMIT_SHORT_SHA"
    # First 8 characters of the commit SHA. Example: a1b2c3d4
    # Great for tagging Docker images: my-app:$CI_COMMIT_SHORT_SHA

    - echo "Branch: $CI_COMMIT_BRANCH"
    # Name of the branch being built. Example: feature/login-page

    - echo "Tag: $CI_COMMIT_TAG"
    # Git tag name, if this pipeline was triggered by a tag push. Example: v1.2.3

    - echo "Commit message: $CI_COMMIT_MESSAGE"
    # Full commit message. Useful for checking for skip flags.

    - echo "Commit ref slug: $CI_COMMIT_REF_SLUG"
    # Branch/tag name with special characters replaced by dashes.
    # Example: 'feature/login-page' becomes 'feature-login-page'
    # Use this in filenames and Docker image tags (no slashes allowed).

    # --- Project Information ---
    - echo "Project name: $CI_PROJECT_NAME"
    # The project name. Example: my-spring-app

    - echo "Project path: $CI_PROJECT_PATH"
    # Group and project name. Example: mygroup/my-spring-app

    - echo "Project URL: $CI_PROJECT_URL"
    # Full HTTPS URL to the project. Example: https://gitlab.com/mygroup/my-spring-app

    - echo "Project directory: $CI_PROJECT_DIR"
    # Absolute path where your repository is checked out inside the runner.
    # Example: /builds/mygroup/my-spring-app

    # --- Registry and Token ---
    - echo "Registry: $CI_REGISTRY"
    # GitLab Container Registry URL. Example: registry.gitlab.com

    - echo "Registry image: $CI_REGISTRY_IMAGE"
    # Full image path for this project. Example: registry.gitlab.com/mygroup/my-spring-app

    - echo "Registry user: $CI_REGISTRY_USER"
    # Username for authenticating to the GitLab Container Registry.

    # $CI_REGISTRY_PASSWORD is the password (never echo passwords!)

    - echo "Job token available: yes (value hidden)"
    # $CI_JOB_TOKEN: A short-lived token for this job.
    # Use it to authenticate with the GitLab API, Package Registry, or other GitLab services.

    # --- Pipeline Trigger Source ---
    - echo "Pipeline source: $CI_PIPELINE_SOURCE"
    # How this pipeline was triggered.
    # Values: push, merge_request_event, schedule, web, trigger, api
```

### Useful Predefined Variables Quick Reference

| Variable | Example Value | Use Case |
|---|---|---|
| `$CI_COMMIT_SHA` | `a1b2c3d4e5f6...` | Full Git commit hash |
| `$CI_COMMIT_SHORT_SHA` | `a1b2c3d4` | Short hash for image tags |
| `$CI_COMMIT_BRANCH` | `main` | Current branch name |
| `$CI_COMMIT_TAG` | `v1.2.3` | Git tag (release pipelines) |
| `$CI_COMMIT_REF_SLUG` | `feature-login-page` | Safe name for filenames/tags |
| `$CI_PIPELINE_ID` | `12345678` | Unique pipeline identifier |
| `$CI_JOB_TOKEN` | `(hidden)` | API auth within a job |
| `$CI_PROJECT_NAME` | `my-app` | Project name |
| `$CI_REGISTRY_IMAGE` | `registry.gitlab.com/group/app` | Container registry image path |
| `$CI_PIPELINE_SOURCE` | `push` | What triggered the pipeline |
| `$CI_ENVIRONMENT_NAME` | `staging` | Deployment environment name |

---

## Custom Variables — Defined in `.gitlab-ci.yml`

You can define variables directly in your pipeline configuration. These are suitable for non-sensitive values that do not change between environments.

```yaml
# Global variables — available to all jobs.
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  APP_PORT: "8080"
  JAVA_VERSION: "17"

stages:
  - build
  - test

build-job:
  stage: build
  image: maven:3.9-eclipse-temurin-$JAVA_VERSION   # Reference global variable in image name
  variables:
    # Job-level variables override global variables for this job only.
    MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository -Xmx1g"
  script:
    - echo "Building with Java $JAVA_VERSION on port $APP_PORT"
    - mvn clean package
```

---

## Project-Level and Group-Level Variables (GitLab UI)

For values that should not live in your source code — especially secrets like API keys, passwords, and tokens — define variables in the GitLab UI. These are stored encrypted and injected into pipelines at runtime.

### Setting a Project-Level Variable

1. Go to your project in GitLab
2. Navigate to **Settings > CI/CD**
3. Expand the **Variables** section
4. Click **Add variable**
5. Fill in:
   - **Key:** e.g., `DEPLOY_SERVER_IP`
   - **Value:** e.g., `192.168.1.100`
   - **Type:** Variable (text) or File (writes value to a temp file and passes the path)
   - **Environment scope:** All, or a specific environment like `production`
   - **Protect variable:** Checked = only available in protected branches/tags
   - **Mask variable:** Checked = value is hidden in job logs

Once saved, the variable is automatically available to all jobs in that project.

### Setting a Group-Level Variable

Group-level variables are defined at **Group > Settings > CI/CD > Variables** and are inherited by all projects in the group. This is ideal for organization-wide settings like:
- Container registry credentials shared across microservices
- Slack webhook URLs for notifications
- Shared deployment keys

---

## Protected Variables

A **protected variable** is only available in pipelines running on **protected branches** (typically `main`, `master`, `release/*`) or **protected tags**. Jobs on unprotected branches (feature branches) cannot access protected variables.

This is a security measure: a developer working on a feature branch cannot access production database credentials even if they write a malicious job that tries to print them.

```yaml
# This job runs on all branches.
# On unprotected branches, PROD_DB_PASSWORD will be empty/undefined.
# On protected branches (main), PROD_DB_PASSWORD will have its real value.
deploy-to-prod:
  stage: deploy
  script:
    - echo "Connecting to production database..."
    # PROD_DB_PASSWORD is a protected variable — only available on main
    - ./db-migrate.sh --password "$PROD_DB_PASSWORD"
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
```

---

## Masked Variables

A **masked variable** has its value replaced with `[MASKED]` in job logs, even if the job accidentally prints it. This prevents secrets from appearing in logs that teammates or auditors might view.

**Rules for masking to work:**
- The value must be at least 8 characters long
- The value must not contain whitespace or newlines
- The value must contain only printable ASCII characters

```yaml
push-docker-image:
  stage: build
  script:
    # CI_REGISTRY_PASSWORD is a masked variable.
    # Even though it is used here, its value will appear as [MASKED] in logs.
    - docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"
    - docker push "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA"
```

Example log output with masking:
```
$ docker login -u "gitlab-ci-token" -p "[MASKED]" "registry.gitlab.com"
Login Succeeded
```

---

## Passing Variables Between Jobs with Dotenv Artifacts

By default, variables set in one job are not visible to other jobs. To pass a dynamically computed value from one job to another, use a **dotenv artifact**.

A dotenv file is a plain text file where each line is `KEY=VALUE`. GitLab reads this file after the job completes and injects the variables into downstream jobs.

```yaml
stages:
  - prepare
  - build
  - deploy

# Job 1: Compute a version string and export it for later jobs.
generate-version:
  stage: prepare
  script:
    # Compute a semantic version using the pipeline ID and short commit hash.
    - VERSION="1.0.$CI_PIPELINE_ID-$CI_COMMIT_SHORT_SHA"
    - echo "Computed version: $VERSION"

    # Write the variable to a dotenv file.
    # The file name can be anything, but 'build.env' is conventional.
    - echo "APP_VERSION=$VERSION" >> build.env
    - echo "BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> build.env
  artifacts:
    # Declare the dotenv file as a 'dotenv' report artifact.
    # GitLab will parse this file and inject its variables into downstream jobs.
    reports:
      dotenv: build.env

# Job 2: Uses variables from Job 1.
# Because 'generate-version' is in a previous stage, its dotenv artifact
# is automatically available here.
build-docker:
  stage: build
  needs:
    - job: generate-version
      artifacts: true       # Explicitly request the artifacts (including dotenv)
  script:
    # APP_VERSION and BUILD_DATE are now available as environment variables!
    - echo "Building version $APP_VERSION (built at $BUILD_DATE)"
    - docker build \
        --label "version=$APP_VERSION" \
        --label "build-date=$BUILD_DATE" \
        -t "$CI_REGISTRY_IMAGE:$APP_VERSION" .
    - docker push "$CI_REGISTRY_IMAGE:$APP_VERSION"

# Job 3: Also receives the dotenv variables.
deploy-with-version:
  stage: deploy
  needs:
    - job: generate-version
      artifacts: true
    - job: build-docker
  script:
    - echo "Deploying version $APP_VERSION"
    - kubectl set image deployment/my-app app="$CI_REGISTRY_IMAGE:$APP_VERSION"
```

---

## Variable Scoping by Environment

Variables can be scoped to a specific **environment** so that the same variable key maps to different values in different environments. This is configured in the GitLab UI when adding a variable by setting the **Environment scope**.

Example setup in the UI:

| Key | Value | Environment Scope |
|---|---|---|
| `API_BASE_URL` | `https://api.dev.example.com` | `development` |
| `API_BASE_URL` | `https://api.staging.example.com` | `staging` |
| `API_BASE_URL` | `https://api.example.com` | `production` |

In your pipeline, when a job runs in the `staging` environment, `$API_BASE_URL` will automatically resolve to `https://api.staging.example.com`:

```yaml
deploy-staging:
  stage: deploy
  script:
    - echo "Deploying to $API_BASE_URL"   # Resolves to staging URL automatically
  environment:
    name: staging
```

---

## Summary

| Variable Type | Where Defined | Use Case |
|---|---|---|
| Predefined | Automatically by GitLab | Commit SHA, branch name, registry URL |
| `.gitlab-ci.yml` variables | In the YAML file | Non-sensitive config values |
| Project-level (UI) | Project Settings > CI/CD | Project-specific secrets |
| Group-level (UI) | Group Settings > CI/CD | Shared org-wide secrets |
| Protected | UI (checkbox) | Restrict secrets to protected branches |
| Masked | UI (checkbox) | Hide values in job logs |
| Dotenv artifact | Job produces a dotenv file | Dynamic values computed at runtime |

---

## External Resources

- [Predefined CI/CD variables reference](https://docs.gitlab.com/ee/ci/variables/predefined_variables.html)
- [Using CI/CD variables](https://docs.gitlab.com/ee/ci/variables/)
- [Passing variables between jobs (dotenv)](https://docs.gitlab.com/ee/ci/variables/#pass-an-environment-variable-to-another-job)
