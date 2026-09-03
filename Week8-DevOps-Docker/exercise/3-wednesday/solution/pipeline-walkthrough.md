# Pipeline Walkthrough — GitLab CI/CD Solution

This document walks through every job in `gitlab-ci-solution.yml`, explains the design decisions, describes what the output looks like, lists common mistakes, and tells you exactly what screenshots to take in the GitLab UI.

---

## How to Read This Document

Each section covers one job. The structure is:

1. **What it does** — the job's purpose in plain language
2. **Why it's configured this way** — the reasoning behind each keyword choice
3. **What you see in GitLab** — what the logs and UI look like when it works
4. **Common mistakes** — what goes wrong and how to fix it

---

## Pipeline Overview

```
Stage: build          Stage: test           Stage: deploy       Stage: notify
─────────────────     ─────────────────     ──────────────      ─────────────
build-app       ─┐    test-app        ─┐    deploy-staging      notify-deploy
print-variables  │    sonar-analysis   │    (manual ▶)
docker-build    ─┘                    ─┘
       ↓ (all must pass)       ↓ (all must pass)       ↓ (human trigger)
```

Jobs in the same stage run in parallel. The next stage starts only after all jobs in the current stage succeed (unless `allow_failure: true`).

---

## Global Configuration

### `stages:`

```yaml
stages:
  - build
  - test
  - deploy
  - notify
```

**What it does:** Declares the four stages and their execution order.

**Why four stages instead of one?** Separation of concerns. Build failures surface before you waste time running tests. Test failures block deployment. Notification only fires after a successful deploy. Each stage represents a quality gate.

**Common mistake:** Adding a job with `stage: integration` when `integration` is not in the `stages` list. GitLab rejects this with: `jobs:your-job:stage is not included in stages`. Fix: add the stage name to the `stages` list.

---

### `variables:` (global)

```yaml
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_TLS_CERTDIR: "/certs"
```

**What it does:** `MAVEN_OPTS` redirects Maven's local repository from the runner's home directory (`~/.m2`) to a path inside the project checkout (`$CI_PROJECT_DIR/.m2`).

**Why does this matter?** GitLab's cache system can only save paths inside `$CI_PROJECT_DIR`. The default Maven repository lives at `~/.m2/repository`, which is outside the project directory and cannot be cached. Moving it into the project directory makes it cacheable.

**`DOCKER_TLS_CERTDIR`** is required by Docker-in-Docker (`dind`) for TLS security. Without it, the Docker daemon refuses connections from the Docker CLI.

---

### `cache:`

```yaml
cache:
  key: "$CI_COMMIT_REF_SLUG"
  paths:
    - .m2/repository
```

**What it does:** Tells GitLab to save the Maven repository after each job and restore it before the next run.

**Why `$CI_COMMIT_REF_SLUG`?** This resolves to the branch name, normalized for use in URLs (slashes replaced, special chars removed). Using the branch name as the cache key gives each branch its own cache bucket. If you used a fixed key (e.g., `"maven-cache"`), all branches would share one cache — a feature branch that installs a snapshot dependency could corrupt main's cache.

**What you see:** In the job log, near the top:
```
Restoring cache
Checking cache for main...
Successfully extracted cache
```
At the bottom:
```
Saving cache for successful job
Created cache
```

**Common mistake:** Defining `MAVEN_OPTS` but forgetting the `cache:` block, or vice versa. Both are required — `MAVEN_OPTS` tells Maven where to write, and `cache:` tells GitLab what to save.

---

## Job: `build-app`

### What it does

Compiles the Spring Boot project and packages a runnable JAR. Stores the JAR as a pipeline artifact.

### Why it's configured this way

**`image: maven:3.9-eclipse-temurin-17`**

This is the official Maven image from Docker Hub, pinned to Maven 3.9 and Java 17 (Eclipse Temurin JDK). Spring Boot 3.x requires Java 17+. Using `eclipse-temurin` (formerly AdoptOpenJDK) is preferred over Oracle JDK for licensing reasons.

**`mvn package -DskipTests`**

`package` compiles the source, runs resources filtering, and produces a JAR. `-DskipTests` skips test execution (but not compilation). Tests belong in the test stage — running them here would duplicate work and slow the pipeline.

**`artifacts.expire_in: 1 day`**

Artifacts consume GitLab storage. Keeping them for 1 day is enough for developers to download the JAR for debugging. Increase to `1 week` if the artifact feeds a deployment pipeline that might run 24+ hours later.

### What you see in GitLab

**Job log (end of output):**
```
[INFO] Building jar: /builds/group/project/target/project3-backend-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
[INFO] Total time: 45.321 s
$ echo "Build complete. Artifact:"
Build complete. Artifact:
$ ls -lh target/*.jar
-rw-r--r-- 1 root root 42M Sep  3 14:22 target/project3-backend-0.0.1-SNAPSHOT.jar
Job succeeded
```

**Pipeline sidebar (right panel when job is selected):**
- A section labeled **Job artifacts** appears
- Click **Browse** to see the file tree
- Click **Download** to get a zip containing the JAR

### Screenshot to take
> **Screenshot 1:** The `build-app` job page showing the green checkmark in the header and the **Job artifacts** panel on the right with the JAR visible. Also capture the job duration shown in the header.

### Common mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Using `mvn install` instead of `mvn package` | Works but pollutes the local repo unnecessarily | Use `mvn package` |
| `artifacts.paths: target/` (directory, no glob) | GitLab uploads the entire target/ directory including class files (~100MB) | Use `target/*.jar` |
| No `expire_in` set | Artifacts never expire and consume storage indefinitely | Always set `expire_in` |

---

## Job: `print-variables`

### What it does

Prints five predefined GitLab CI/CD variables. Runs in parallel with `build-app` (same stage).

### What you see in GitLab

```
=== GitLab Predefined Variables ===
Full commit SHA   : a3f8b2c1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9
Short commit SHA  : a3f8b2c
Pipeline ID       : 1234567
Job name          : print-variables
Project name      : project3-backend
Branch name       : main
Runner description: shared-runners-manager-1234.gitlab.com
GitLab host       : https://gitlab.com
```

### Screenshot to take
> **Screenshot 2:** The `print-variables` job log showing all 5 variable values. Verify that `CI_COMMIT_SHORT_SHA` is the first 7 characters of `CI_COMMIT_SHA`.

---

## Job: `test-app`

### What it does

Runs the full test suite. Publishes JUnit XML reports so GitLab displays pass/fail counts.

### Why it's configured this way

**`artifacts.when: always`**

If tests fail, Maven exits with a non-zero code and the job fails. Without `when: always`, GitLab would not collect the artifacts (because the job "failed"), which means you would never receive the JUnit report. `when: always` ensures the XML is collected regardless of test outcome — which is exactly when you most need the report.

**`reports.junit: target/surefire-reports/TEST-*.xml`**

Maven Surefire writes one XML file per test class. The glob `TEST-*.xml` captures all of them. GitLab parses these XML files using the JUnit XML format and surfaces the results in:
- The job page (test count in the header)
- The pipeline page (test summary tab)
- The merge request widget (pass/fail delta compared to main)

### What you see in GitLab

**Job log:**
```
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Pipeline page (Tests tab):**
```
24 tests  0 failures  0 skipped  0 errors
```

**Merge request (if applicable):**
```
Test summary: 24 passed
```

### Screenshot to take
> **Screenshot 3:** The pipeline's **Tests** tab showing the test count. If you have a merge request open, also capture the test summary widget on the MR page.

### Common mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| `when: on_success` (the default) | Test reports disappear when tests fail | Change to `when: always` |
| Path `target/surefire-reports/*.xml` (missing `TEST-` prefix) | Works, but also picks up `failsafe-reports` XMLs if present | Use `TEST-*.xml` to be precise |
| Forgetting `reports:` nesting | GitLab treats the XML as a plain artifact, not a test report | Ensure correct nesting: `artifacts.reports.junit` |

---

## Job: `sonar-analysis`

### What it does

Runs SonarCloud static analysis on the `main` and `develop` branches only. `allow_failure: true` prevents Sonar failures from blocking deployment.

### Why `allow_failure: true`?

Code quality findings should be informative, not blocking — at least during initial adoption. Once the team agrees on a quality threshold, remove `allow_failure` to make the pipeline enforce it.

### What you see in GitLab

The job shows an orange warning icon (not red failure) if Sonar fails. The pipeline continues to the deploy stage regardless.

In SonarCloud's dashboard: code coverage, code smells, bugs, and vulnerabilities are displayed per branch.

### Common mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| `SONAR_TOKEN` not masked | Token visible in plain text in logs | Always mask tokens |
| Running on every branch | Wastes Sonar analysis quota | Add `rules:` to restrict to main/develop |

---

## Job: `docker-build`

### What it does

Builds a Docker image from the project's `Dockerfile` and pushes it to the GitLab Container Registry. Tags with both the 7-character commit SHA and `latest`.

### Why two tags?

`latest` is convenient for "give me the newest image" scripts. The commit SHA tag is immutable — it always refers to the exact image built from that commit. Use the SHA tag in deployments so you can always roll back to a known commit.

### What you see in GitLab

**Job log:**
```
$ docker build -t registry.gitlab.com/group/project:a3f8b2c .
Step 1/8 : FROM eclipse-temurin:17-jre
Step 2/8 : WORKDIR /app
...
Successfully built d4e5f6a7b8c9
Successfully tagged registry.gitlab.com/group/project:a3f8b2c
$ docker push registry.gitlab.com/group/project:a3f8b2c
The push refers to repository [registry.gitlab.com/group/project]
a3f8b2c: digest: sha256:... size: 1234
```

**In GitLab UI:** Project → **Packages & Registries → Container Registry** shows the new image tags.

### Screenshot to take
> **Screenshot 4:** The Container Registry page showing both the commit SHA tag and the `latest` tag after a successful `docker-build` job.

### Common mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Missing `services: [docker:24-dind]` | `Cannot connect to the Docker daemon` error | Add the `services:` block |
| `DOCKER_TLS_CERTDIR` not set | TLS handshake failures | Add to global `variables:` |
| No `Dockerfile` at project root | `unable to prepare context: path "." not found` | Create a Dockerfile (see Thursday's Docker exercise) |

---

## Job: `deploy-staging`

### What it does

Deploys the built JAR to the staging environment. Requires a human to click the play button. Tracks the deployment in GitLab Environments.

### Why `when: manual`?

Automatic deployments to staging carry risk — a broken commit automatically becomes a broken environment, which may block QA testing. A manual gate ensures a human has reviewed the pipeline before deploying. For production environments, this gate is mandatory.

### Why `needs: [build-app]` without `test-app`?

`needs:` with `artifacts: true` downloads the JAR from `build-app`. Skipping `test-app` in the needs graph means the deploy job can technically run even if tests fail (if manually triggered). In production, add `test-app` to the `needs:` list to enforce the quality gate.

### What you see in GitLab

**Pipeline view:** The `deploy-staging` job shows a **▶ play button** icon. The pipeline is not blocked — build and test ran normally, and the deploy is just waiting.

**After clicking ▶:**
```
=== Staging Deployment ===
Project    : project3-backend
Commit     : a3f8b2c
Pipeline   : 1234567
Target URL : [MASKED]
Artifact   : target/project3-backend-0.0.1-SNAPSHOT.jar
Deployment complete.
```

**Deployments → Environments:** A row appears for `staging` showing the commit SHA, who triggered it, and when. Clicking the environment name shows deployment history.

### Screenshot to take
> **Screenshot 5:** The pipeline view showing the `deploy-staging` job with the ▶ play button (before triggering).
> **Screenshot 6:** The `deploy-staging` job log after manually triggering — confirm `[MASKED]` appears instead of the URL value.
> **Screenshot 7:** The **Deployments → Environments** page showing the `staging` environment row with the commit SHA and deployment time.

### Common mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| `DEPLOY_TARGET_URL` not masked | Real URL visible in logs | Edit variable → enable Masked |
| Protected variable on unprotected branch | Variable is empty / job fails with no error | Either protect the branch or unprotect the variable |
| Forgetting `artifacts: true` in `needs:` | `target/` directory is empty in the deploy job | Add `artifacts: true` under the `needs:` job entry |
| `environment.url` uses plain text instead of variable | URL shown in UI but not reusable | Use `$DEPLOY_TARGET_URL` |

---

## Job: `notify-deploy`

### What it does

Sends a Slack notification after a successful manual deploy. Only runs on `main` and `develop`.

### What you see in Slack

```
🚀 project3-backend deployed to staging
Commit:   a3f8b2c     Pipeline:  1234567
Branch:   main        Author:    Your Name
```

### Common mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| `SLACK_WEBHOOK_URL` not masked | Webhook URL visible in logs — rotate it immediately | Always mask webhook URLs |
| `curl` not installed in the image | `command not found: curl` | Add `before_script: [apk add --no-cache curl]` |
| Missing `needs: [deploy-staging]` | Notify job runs even when deploy is skipped | Add the `needs:` dependency |

---

## CI Lint Validation

Before any pipeline runs, validate your YAML:

1. Go to **Project → CI/CD → Editor → Validate**
2. A valid pipeline shows all job names with their stage assignments:

```
Pipeline is valid!

Jobs:
  build-app (build)
  print-variables (build)
  test-app (test)
  sonar-analysis (test)
  docker-build (build)
  deploy-staging (deploy)
  notify-deploy (notify)
```

**Common lint errors and their meaning:**

| Error text | Root cause |
|-----------|------------|
| `root config contains unknown keys: artefacts` | Typo — `artefacts` should be `artifacts` |
| `jobs:build-app:stage is not included in stages` | Stage name mismatch — check exact spelling |
| `jobs:deploy-staging:needs job 'build-app' not found` | `build-app` was renamed or the `needs:` entry has a typo |
| `yaml: line 42: mapping values are not allowed here` | Indentation error at line 42 — YAML is whitespace-sensitive |

---

## Screenshot Checklist

Collect all screenshots before submission:

| # | Where to take it | What to capture |
|---|-----------------|----------------|
| 1 | `build-app` job page | Green checkmark + JAR artifact in the right panel |
| 2 | `print-variables` job log | All 5 predefined variable values printed |
| 3 | Pipeline Tests tab | Test count (e.g., "24 tests, 0 failures") |
| 4 | Container Registry page | Both SHA tag and `latest` tag listed |
| 5 | Pipeline view (before deploy trigger) | `deploy-staging` job showing ▶ play button |
| 6 | `deploy-staging` job log | `[MASKED]` where the URL would appear |
| 7 | Deployments → Environments | `staging` row with commit SHA and timestamp |
| 8 | CI/CD Editor → Validate | "Pipeline is valid" message with all jobs listed |

---

*Week 8 — DevOps & Docker | Wednesday: GitLab CI/CD | Solution Walkthrough*
