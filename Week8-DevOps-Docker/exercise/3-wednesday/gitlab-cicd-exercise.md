# Lab: Build Your First GitLab CI/CD Pipeline

**Duration:** 3–4 hours
**Mode:** Individual (Implementation)
**Week:** 8 — DevOps & Docker | **Day:** Wednesday

---

## Prerequisites

Before starting, confirm all of the following:

- [ ] GitLab account with access to your Project 3 repository
- [ ] Project 3 Spring Boot backend pushed to a GitLab repo (not just GitHub)
- [ ] GitLab shared runners enabled: **Settings → CI/CD → Runners → Shared runners: Enabled**
- [ ] Project builds locally: `mvn package -DskipTests` completes without errors
- [ ] You have at least **Developer** role in the GitLab project (needed to trigger pipelines)

---

## Learning Objectives

By the end of this lab you will be able to:

1. Author a complete `.gitlab-ci.yml` file using stages, jobs, image, script, artifacts, and cache keywords
2. Explain how GitLab pipeline stages control job execution order and parallelism
3. Create masked and protected CI/CD variables and verify they are hidden in job logs
4. Configure artifact storage and JUnit test report parsing so GitLab surfaces test results in the UI
5. Add a manual-trigger deploy job linked to a named environment using `when: manual`

---

## Scenario

The engineering team has decided to automate the build-and-test process for Project 3. Your mission: write a complete CI/CD pipeline that automatically builds and tests the Spring Boot backend on every commit, and provides a manual-trigger deploy stage for the staging environment.

Every push to the repo should trigger build + test automatically. The deploy stage should require a human to click a button — no accidental deployments.

---

## Setup — Before You Begin

### Step 1 — Verify Shared Runners

1. Open your GitLab project in the browser
2. Go to **Settings → CI/CD → Runners**
3. Expand the **Runners** section
4. Confirm you see a green circle next to at least one shared runner
5. If no runners appear, ask your instructor — without a runner, no jobs will execute

### Step 2 — Confirm Local Build

```bash
cd /path/to/your/project3-backend
mvn package -DskipTests
ls target/*.jar
```

You should see a JAR file in `target/`. Fix any local build errors before writing CI/CD — a broken local build always produces a broken pipeline.

### Step 3 — Note Your Repository URL

Copy your GitLab project URL (e.g., `https://gitlab.com/your-username/project3-backend`).

### Step 4 — Confirm Your Role

Go to **Project → Members** and verify your role is **Developer** or higher.

---

## Core Tasks

---

### Task 1 — Create Your First `.gitlab-ci.yml`

The `.gitlab-ci.yml` file lives at the **root** of the repository (same level as `pom.xml`). GitLab detects it automatically on every push.

**Steps:**

1. At the root of your local Project 3 clone, create the file:

```bash
touch .gitlab-ci.yml
```

2. Add only the stages declaration:

```yaml
stages:
  - build
  - test
  - deploy
```

3. Commit and push:

```bash
git add .gitlab-ci.yml
git commit -m "ci: add initial pipeline skeleton"
git push origin main
```

4. Open GitLab → **CI/CD → Pipelines**

A pipeline appears. It will be failed or blocked (no jobs yet) — that is expected. You are confirming GitLab detected your file.

**✅ Checkpoint 1:** A pipeline entry appears in the GitLab CI/CD → Pipelines list.

---

### Task 2 — Write the Build Job

The build job compiles the project, packages a JAR, and stores it as a pipeline artifact so later jobs and humans can download it.

Add the following to `.gitlab-ci.yml` and fill in every `# TODO`:

```yaml
build-app:
  stage: build
  image: # TODO: which Maven+Java image from Docker Hub?
         # Hint: search Docker Hub for "maven"
         # Answer: maven:3.9-eclipse-temurin-17
  script:
    - # TODO: Maven command to compile and package, skipping tests
      # Hint: mvn package ...
  artifacts:
    paths:
      - # TODO: where does Maven put the JAR?
        # Hint: Maven always outputs to target/
    expire_in: # TODO: how long to keep this artifact?
               # Example: 1 day
```

**TODO answers:**

| TODO | Fill in |
|------|---------|
| image | `maven:3.9-eclipse-temurin-17` |
| script | `mvn package -DskipTests` |
| artifacts paths | `target/*.jar` |
| expire_in | `1 day` |

Push and watch the job run in GitLab → click `build-app` → read the live log. After it passes, click **Browse** or **Download** on the right panel to see the stored JAR.

**✅ Checkpoint 2:** `build-app` passes with a green checkmark. A JAR artifact is visible and downloadable in the pipeline UI.

---

### Task 3 — Write the Test Job

The test job runs after build. It executes `mvn test` and publishes JUnit XML reports — GitLab parses these to display pass/fail counts in the pipeline and merge request widgets.

```yaml
test-app:
  stage: test
  image: # TODO: same Maven+Java image as build-app
  script:
    - # TODO: Maven command that runs tests (no -DskipTests!)
  artifacts:
    when: always   # collect even if tests fail
    reports:
      junit:
        - # TODO: where does Maven write JUnit XML?
          # Hint: target/surefire-reports/TEST-*.xml
```

After pushing:

1. Watch `build-app` then `test-app` run sequentially
2. Click the `test-app` log — look for `Tests run:` lines from Maven Surefire
3. Open or create a merge request — look for the **Test summary** widget

**✅ Checkpoint 3:** `test-app` passes. GitLab shows a test count (e.g., "12 tests, 0 failures").

---

### Task 4 — Add a Maven Dependency Cache

**The problem:** Maven re-downloads all dependencies on every run — 50–100 MB, adding 2–4 minutes per pipeline.

**The fix:** Cache the Maven local repository between runs.

Add these two blocks at the **top level** of `.gitlab-ci.yml` (not inside any job):

```yaml
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  key: "$CI_COMMIT_REF_SLUG"
  paths:
    - .m2/repository
```

**What each piece does:**

- `MAVEN_OPTS` redirects Maven's local repo from `~/.m2` into the project directory, which GitLab can cache
- `cache.key: "$CI_COMMIT_REF_SLUG"` uses the branch name — each branch gets its own cache
- `cache.paths` tells GitLab what to save and restore between runs

After pushing, trigger the pipeline twice (push a trivial change like a comment). Compare the `build-app` job duration between run 1 and run 2.

**✅ Checkpoint 4:** The second run is faster. The build log shows `Restoring cache` at the top and `Saving cache` at the bottom.

---

### Task 5 — Create a Masked CI/CD Variable

Never hardcode passwords, tokens, or server URLs in `.gitlab-ci.yml` — the file is visible to everyone with repo access.

**Steps:**

1. Go to **Project → Settings → CI/CD → Variables → Add variable**
2. Fill in:
   - **Key:** `DEPLOY_TARGET_URL`
   - **Value:** `https://staging.example.com`
   - **Masked:** ✅
   - **Protected:** ✅
3. Click **Add variable**

**Test the masking** by adding a temporary job:

```yaml
test-masking:
  stage: build
  image: alpine:latest
  script:
    - echo "Deploy URL = $DEPLOY_TARGET_URL"
```

Push and open the job log. You should see:

```
$ echo "Deploy URL = $DEPLOY_TARGET_URL"
Deploy URL = [MASKED]
```

After confirming, remove the `test-masking` job — it was only for verification.

> **Note:** GitLab only masks values that are at least 8 characters. Shorter values are not masked.

**✅ Checkpoint 5:** The job log shows `[MASKED]` instead of the actual URL value.

---

### Task 6 — Write the Manual Deploy Job

The deploy job must not run automatically. It waits for a human to click a play button — this is `when: manual`.

```yaml
deploy-staging:
  stage: deploy
  when: manual
  environment:
    name: staging
    url: $DEPLOY_TARGET_URL
  needs:
    - job: build-app
      artifacts: true
  script:
    - echo "Deploying to $DEPLOY_TARGET_URL"
    - echo "Artifact: $(ls target/*.jar)"
    - echo "Commit: $CI_COMMIT_SHORT_SHA"
    - echo "Pipeline: $CI_PIPELINE_ID"
    # Real scenario: SSH to server, copy JAR, restart service
```

**Keyword breakdown:**

| Keyword | Purpose |
|---------|---------|
| `when: manual` | Requires a human click — does not auto-start |
| `environment` | Links to a GitLab Environment for deployment tracking |
| `needs` | Declares dependency on `build-app` and its artifacts |
| `artifacts: true` | Downloads the JAR from `build-app` before running scripts |

After pushing:

1. `build-app` and `test-app` run automatically
2. `deploy-staging` shows a **▶ play button** (waiting, not failed)
3. Click the play button to manually trigger
4. After it runs: **Project → Deployments → Environments** — `staging` should appear

**✅ Checkpoint 6:** Deploy job shows a play button, not auto-run. Clicking it succeeds. `staging` environment appears under Deployments.

---

### Task 7 — Use Predefined Variables

GitLab provides dozens of built-in variables describing the current pipeline, commit, and project. Add this job to print five of them:

```yaml
print-variables:
  stage: build
  image: alpine:latest
  script:
    - echo "Full commit SHA   : $CI_COMMIT_SHA"
    - echo "Short commit SHA  : $CI_COMMIT_SHORT_SHA"
    - echo "Pipeline ID       : $CI_PIPELINE_ID"
    - echo "Job name          : $CI_JOB_NAME"
    - echo "Project name      : $CI_PROJECT_NAME"
```

**Reference:**

| Variable | Example | Common Use |
|----------|---------|------------|
| `$CI_COMMIT_SHA` | `a3f8b2c1...` (40 chars) | Tagging Docker images with exact commit |
| `$CI_COMMIT_SHORT_SHA` | `a3f8b2c` (7 chars) | Human-readable version labels |
| `$CI_PIPELINE_ID` | `1234567` | Tracing which pipeline produced an artifact |
| `$CI_JOB_NAME` | `print-variables` | Conditional script logic |
| `$CI_PROJECT_NAME` | `project3-backend` | Naming artifacts or containers |

**✅ Checkpoint 7:** All five values appear in the job log with descriptive labels.

---

### Task 8 — Validate with CI Lint

Before declaring success, validate your complete YAML with GitLab's built-in linter.

**Steps:**

1. In GitLab, go to **Project → CI/CD → Editor**
   (or navigate to `https://gitlab.com/<your-project>/-/ci/lint`)
2. Your current `.gitlab-ci.yml` is shown — click **Validate**
3. Read any warnings or errors and fix them in your local file
4. Re-push and re-validate until clean

**Common lint errors:**

| Error | Cause | Fix |
|-------|-------|-----|
| `unknown keyword` | Typo (e.g., `artefacts`) | Check spelling |
| `stage not included in stages` | Job's `stage` doesn't match the `stages` list | Match exactly |
| `needs job not found` | `needs` references a non-existent job name | Check job name |
| `mapping values not allowed here` | Indentation error | Fix YAML indentation |

**✅ Checkpoint 8:** CI Lint shows **"Pipeline is valid"** with all jobs listed and no errors.

---

## Bonus Challenges

### Bonus A — SonarCloud Integration

1. Sign up at [sonarcloud.io](https://sonarcloud.io) and link your GitLab account
2. Create a project in SonarCloud, copy the `SONAR_TOKEN`
3. Add `SONAR_TOKEN` as a masked variable in GitLab
4. Add:

```yaml
sonar-analysis:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn verify sonar:sonar
        -Dsonar.projectKey=your-project-key
        -Dsonar.host.url=https://sonarcloud.io
        -Dsonar.login=$SONAR_TOKEN
  allow_failure: true
```

### Bonus B — Branch-Specific Rules

Make the deploy job appear **only on `main`**, not feature branches:

```yaml
deploy-staging:
  stage: deploy
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
      when: manual
    - when: never
  script:
    - echo "Deploying $CI_COMMIT_SHORT_SHA to staging"
```

Push to a feature branch and confirm the deploy job does not appear. Merge to main and confirm it does.

### Bonus C — Docker Image Build and Push

Use GitLab's built-in Container Registry (requires a `Dockerfile` at project root):

```yaml
docker-build:
  stage: build
  image: docker:24
  services:
    - docker:24-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
```

---

## Definition of Done

- [ ] `.gitlab-ci.yml` passes CI Lint with no errors
- [ ] `build-app` passes and produces a downloadable JAR artifact
- [ ] `test-app` passes and shows test count in GitLab UI
- [ ] Cache reduces pipeline duration on second run (compare job times)
- [ ] Masked variable `DEPLOY_TARGET_URL` shows as `[MASKED]` in logs
- [ ] `deploy-staging` requires manual trigger — shows play button, not auto-run
- [ ] `staging` environment appears under Deployments → Environments
- [ ] Predefined variables job prints all 5 values correctly
- [ ] Screenshots of each passing stage saved locally
- [ ] Final `.gitlab-ci.yml` copied into the `solution/` folder

---

## Reflection Questions

Answer in your notes or the class discussion thread:

1. **Stages vs. Jobs:** A pipeline has `stages: [build, test, deploy]` and three jobs all assigned to the `test` stage. How does GitLab schedule those three jobs — sequentially or in parallel? What would you change if you needed them to run sequentially?

2. **Masked Variables:** A teammate complains their masked variable value is visible in plain text in the job log. What are two possible reasons this could happen, and how would you diagnose each one?

3. **`needs:` vs. Stage Order:** The `deploy-staging` job uses `needs: [build-app]` and skips waiting for `test-app`. In a production pipeline, would you keep this or change it? Justify your answer with a real-world consequence.

---

*Week 8 — DevOps & Docker | Wednesday: GitLab CI/CD*
