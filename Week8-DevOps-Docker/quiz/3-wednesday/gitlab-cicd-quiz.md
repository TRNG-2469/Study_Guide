# Weekly Knowledge Check: Week 8 — GitLab CI/CD
**Day:** Wednesday | **Topics:** CI/CD Components · Pipeline Types · Jobs · Runners · Variables · GitLab vs Bitbucket · Terraform Integration

---

## Part 1: Multiple Choice

**Question 1.** What does the predefined CI/CD variable `CI_COMMIT_SHA` contain?

A) The short 8-character commit hash  
B) The full 40-character SHA-1 hash of the commit that triggered the pipeline  
C) The SHA of the most recent tag on the branch  
D) The SHA of the merge request target branch HEAD  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `CI_COMMIT_SHA` holds the complete 40-character SHA-1 commit hash of the commit that triggered the pipeline. It is useful for tagging Docker images or referencing exact code versions.  
- **Why A is wrong:** The short hash is stored in `CI_COMMIT_SHORT_SHA`, not `CI_COMMIT_SHA`.  
- **Why C is wrong:** Tag SHAs are referenced via `CI_COMMIT_TAG`, not this variable.  
- **Why D is wrong:** The target branch HEAD is not captured by `CI_COMMIT_SHA`; that would require a separate Git command.  
</details>

---

**Question 2.** Which executor does a Docker executor GitLab Runner use to run each job?

A) A persistent virtual machine shared across all jobs  
B) A fresh Docker container created from the job's `image` for each job  
C) The host machine's shell environment directly  
D) A Kubernetes pod that persists for the runner's lifetime  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** The Docker executor spins up a brand-new container (using the specified `image`) for every job, providing isolation and a clean environment each time.  
- **Why A is wrong:** Persistent VMs are characteristic of the VirtualBox executor, not Docker.  
- **Why C is wrong:** Running directly on the host shell is the behavior of the Shell executor.  
- **Why D is wrong:** Long-lived pods are a Kubernetes executor concept; Docker executor containers are ephemeral per-job.  
</details>

---

**Question 3.** In a `.gitlab-ci.yml` file, what is the purpose of the `before_script` keyword at the global level?

A) It defines which Docker image to use for all jobs  
B) It specifies commands that run before every job's `script` section unless overridden  
C) It sets environment variables available only before the pipeline starts  
D) It lists the stages that must complete before the pipeline is triggered  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** A global `before_script` defines commands injected before every job's main `script`. Individual jobs can override it with their own `before_script`.  
- **Why A is wrong:** The Docker image is set with the `image` keyword, not `before_script`.  
- **Why C is wrong:** Environment variables are configured via `variables`, not `before_script`.  
- **Why D is wrong:** Stage ordering is defined by the `stages` keyword.  
</details>

---

**Question 4.** What does the `include` keyword do in `.gitlab-ci.yml`?

A) Includes external Docker images into the pipeline  
B) Merges one or more external YAML files into the current pipeline configuration  
C) Imports variables from a `.env` file at runtime  
D) Includes output artifacts from a previous pipeline  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `include` lets you split your CI/CD configuration across multiple YAML files (local, remote, or from templates), which GitLab merges into a single effective configuration.  
- **Why A is wrong:** Docker images are specified with `image`, not `include`.  
- **Why C is wrong:** Runtime `.env` variables are loaded via `artifacts: reports: dotenv`.  
- **Why D is wrong:** Artifacts from previous pipelines are fetched with `needs` or `dependencies`, not `include`.  
</details>

---

**Question 5.** When is a GitLab Merge Request (MR) pipeline triggered?

A) Only when a commit is pushed directly to the default branch  
B) When a merge request is created or updated with new commits  
C) On a fixed time schedule regardless of code changes  
D) When a parent pipeline explicitly triggers it using `trigger:`  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** An MR pipeline runs whenever a merge request is created or receives new commits, and it uses `CI_PIPELINE_SOURCE == "merge_request_event"` to identify itself.  
- **Why A is wrong:** Pushes to the default branch create branch pipelines, not MR pipelines.  
- **Why C is wrong:** Time-based runs are scheduled pipelines, triggered by a cron schedule.  
- **Why D is wrong:** Parent-child pipelines use `trigger:` but that is a distinct pipeline type from MR pipelines.  
</details>

---

**Question 6.** What is the key difference between `artifacts: paths` and `cache: paths`?

A) `artifacts` are stored in the container; `cache` is uploaded to an S3 bucket  
B) `artifacts` are passed between jobs and downloaded by GitLab; `cache` speeds up subsequent pipelines by persisting files between runs but is not guaranteed  
C) `artifacts` expire immediately after a job; `cache` persists forever  
D) `artifacts` are only available to jobs in the same stage; `cache` is available across all stages  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** Artifacts are uploaded to GitLab after a job and explicitly passed to downstream jobs; they are reliable and versioned. Cache is a runner-local optimization (e.g., node_modules) that may or may not be available depending on which runner picks up the job.  
- **Why A is wrong:** Both artifacts and cache are uploaded to storage, not kept solely in the container.  
- **Why C is wrong:** Artifacts have configurable expiry (`artifacts: expire_in`), not immediate expiry.  
- **Why D is wrong:** Artifacts can be shared across stages via `dependencies` or `needs`; they are not stage-scoped.  
</details>

---

**Question 7.** What restriction applies to the value of a masked CI/CD variable in GitLab?

A) It must be fewer than 64 characters long  
B) It can only contain alphanumeric characters and underscores  
C) It must consist only of printable ASCII characters with no whitespace or special characters from a restricted set  
D) It must be defined at the project level, not the group level  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** GitLab masked variables must consist of printable, Base64-safe characters with no whitespace; this allows the runner to reliably redact the value from logs wherever it appears.  
- **Why A is wrong:** There is no strict 64-character length limit on masked variable values.  
- **Why B is wrong:** Base64 characters (including `+`, `/`, `=`) are allowed; the restriction is about whitespace and certain control characters.  
- **Why D is wrong:** Masked variables can be defined at the project, group, or instance level.  
</details>

---

**Question 8.** In a GitLab vs Bitbucket comparison, which feature is unique to GitLab CI/CD and not natively available in Bitbucket Pipelines?

A) YAML-based pipeline definition files  
B) Support for Docker-based build environments  
C) Built-in parent-child and multi-project pipeline orchestration  
D) Branch-based pipeline triggers  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** GitLab natively supports parent-child pipelines and multi-project pipeline triggers via the `trigger:` keyword, enabling complex pipeline hierarchies. Bitbucket Pipelines lacks this level of native pipeline orchestration.  
- **Why A is wrong:** Both GitLab and Bitbucket use YAML files (`gitlab-ci.yml` and `bitbucket-pipelines.yml`).  
- **Why B is wrong:** Both platforms support Docker-based build environments.  
- **Why D is wrong:** Both platforms trigger pipelines based on branch pushes.  
</details>

---

**Question 9.** What does the GitLab Terraform HTTP backend store in GitLab?

A) Terraform provider binaries and module registry metadata  
B) The Terraform state file, securely persisted in GitLab's managed state storage  
C) CI/CD pipeline logs generated during `terraform apply`  
D) Environment variables used by the `terraform` CLI commands  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** GitLab's Terraform HTTP backend stores and locks the Terraform state file within GitLab itself, eliminating the need for an external S3 bucket or other backend storage.  
- **Why A is wrong:** Provider binaries are fetched from the Terraform registry, not stored in GitLab's backend.  
- **Why C is wrong:** Pipeline logs are stored in GitLab job logs, not in the Terraform HTTP backend.  
- **Why D is wrong:** CI/CD variables are managed separately in GitLab's variable settings, not in the Terraform backend.  
</details>

---

**Question 10.** Which runner type is available to all projects within a GitLab instance and is not tied to any specific project or group?

A) Project runner  
B) Group runner  
C) Shared runner  
D) Dedicated runner  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** Shared runners are configured at the GitLab instance level and are available to all projects unless disabled. They are managed by administrators and handle jobs across the entire platform.  
- **Why A is wrong:** Project runners are registered to a specific project and only process that project's jobs.  
- **Why B is wrong:** Group runners are available only to projects within a specific group hierarchy.  
- **Why D is wrong:** "Dedicated runner" is not a standard GitLab runner classification.  
</details>

---

**Question 11.** What happens when you use `extends:` in a GitLab CI/CD job?

A) The job inherits and merges configuration from another named job or hidden job template  
B) The job runs after the referenced job completes, creating an implicit dependency  
C) The job's `script` section is replaced entirely by the referenced job's script  
D) The job is exported as a reusable component to other projects  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** A  
**Explanation:** `extends:` allows a job to inherit configuration keys from a template job (often a hidden job prefixed with `.`). Keys are deep-merged, with the extending job's values taking precedence.  
- **Why B is wrong:** Implicit ordering is handled by `stages` and `needs`, not `extends`.  
- **Why C is wrong:** The `script` key is merged, not replaced wholesale; the extending job can override individual keys.  
- **Why D is wrong:** Exporting jobs to other projects is done via `include:` with a remote reference, not `extends`.  
</details>

---

**Question 12.** During a full GitLab CI/CD walkthrough, which sequence correctly reflects the default pipeline execution flow?

A) Runner picks up job → GitLab evaluates `rules` → job script executes → artifacts uploaded  
B) GitLab evaluates `rules` → pipeline is created → stages execute in order → runners pick up jobs within each stage  
C) Pipeline is created → all jobs run simultaneously regardless of stage → artifacts are collected  
D) Runner registers with GitLab → `before_script` runs globally → stages are determined → jobs execute  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** GitLab first evaluates `rules`/`only`/`except` to determine which jobs to include, creates the pipeline, then executes stages sequentially, with each stage's jobs dispatched to available runners in parallel.  
- **Why A is wrong:** Rule evaluation happens before pipeline creation and runner pickup, not after.  
- **Why C is wrong:** Jobs within a stage can run in parallel, but stages themselves execute sequentially by default.  
- **Why D is wrong:** Runner registration is a one-time admin setup step, not part of individual pipeline execution flow.  
</details>

---

## Part 2: True/False

**Question 13.** True or False: In a parent-child pipeline setup, child pipelines always run in the same GitLab project as the parent pipeline.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False  
**Explanation:** Child pipelines triggered via `trigger: include:` run in the same project by default, but multi-project pipelines (triggered via `trigger: project:`) run in a *different* downstream project. The two terms are distinct — parent-child pipelines are same-project, while multi-project pipelines cross project boundaries.  
</details>

---

**Question 14.** True or False: A job with `when: manual` in GitLab CI/CD will block the entire pipeline from proceeding to the next stage until a user manually triggers it.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False  
**Explanation:** By default, a `when: manual` job is optional and does *not* block subsequent stages. To make it blocking, you must also set `allow_failure: false`. Without that setting, the pipeline continues to the next stage regardless of whether the manual job is triggered.  
</details>

---

**Question 15.** True or False: GitLab Shared Runners on GitLab.com use the Docker executor by default.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True  
**Explanation:** GitLab.com's shared runners are configured with the Docker executor by default, running each job in an isolated container. This ensures clean environments and supports the `image:` keyword in job definitions.  
</details>

---

**Question 16.** True or False: The `cache:` keyword in GitLab CI/CD guarantees that cached files will always be available to every job that declares the same cache key.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False  
**Explanation:** Cache availability is not guaranteed. If a different runner picks up a job and has not previously populated the cache for that key, the cache will be a miss. Caches are a best-effort performance optimization, not a reliable data-passing mechanism — that role belongs to `artifacts`.  
</details>

---

**Question 17.** True or False: A GitLab CI/CD variable defined at the group level is automatically available to all pipelines in projects that belong to that group.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True  
**Explanation:** Group-level CI/CD variables are inherited by all projects within the group (and subgroups), making them available to pipelines without needing to redefine them per project. Project-level variables with the same name will override group-level ones.  
</details>

---

## Part 3: Fill in the Blank

**Question 18.** The `________` keyword in a GitLab CI/CD job allows it to start as soon as its listed dependency jobs finish, without waiting for all other jobs in the previous stage to complete.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `needs`  
**Explanation:** `needs:` creates a DAG (Directed Acyclic Graph) dependency, letting a job run as soon as specific named jobs finish rather than waiting for the entire stage. This can significantly speed up pipelines by enabling out-of-order job execution.  
</details>

---

**Question 19.** In a `.gitlab-ci.yml` file, a job name prefixed with `________` (a dot/period) is treated as a hidden job — it is not executed but can be used as a reusable template with `extends:`.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `.` (a period/dot)  
**Explanation:** Any job whose name begins with a `.` (e.g., `.base-job`) is hidden and skipped during pipeline execution. Hidden jobs serve as reusable configuration templates consumed by other jobs via `extends:`.  
</details>

---

**Question 20.** To pass environment variables from one job to downstream jobs using a file, you use `artifacts: reports: ________`, which reads a `.env` formatted file and exposes its key-value pairs as CI/CD variables in subsequent jobs.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `dotenv`  
**Explanation:** `artifacts: reports: dotenv: variables.env` causes GitLab to parse the uploaded `.env` file and inject its key-value pairs as environment variables into downstream jobs. This is the standard pattern for dynamically passing values between jobs.  
</details>

---

## Part 4: Code Prediction

**Question 21.** Given the following `.gitlab-ci.yml` snippet, what will the pipeline do when a developer pushes directly to the `main` branch (not via a merge request)?

```yaml
stages:
  - build
  - test
  - deploy

build-job:
  stage: build
  script:
    - echo "Building..."

test-job:
  stage: test
  needs: [build-job]
  script:
    - echo "Testing..."

deploy-job:
  stage: deploy
  script:
    - echo "Deploying..."
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
      when: always
    - when: never
```

A) All three jobs run: build, test, then deploy — in that order  
B) Only `build-job` and `test-job` run; `deploy-job` is skipped because the push is not a merge request event  
C) Only `deploy-job` runs because the `rules` condition matches a direct push  
D) The pipeline fails because `needs` cannot reference a job in a previous stage  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** The `deploy-job` has a `rules` block that only allows it to run when `CI_PIPELINE_SOURCE == "merge_request_event"`. A direct push to `main` sets the source to `"push"`, so the first rule does not match and the fallback `when: never` excludes `deploy-job` entirely. `build-job` and `test-job` have no restricting rules, so they run normally. Also, `needs: [build-job]` is valid and tells `test-job` to start as soon as `build-job` finishes, not when the full build stage completes.  
- **Why A is wrong:** `deploy-job` is excluded by the `rules` evaluation — it does not run on a plain push.  
- **Why C is wrong:** The `if` condition explicitly checks for `merge_request_event`, which a direct push does not satisfy.  
- **Why D is wrong:** `needs:` can reference any previously completed job; cross-stage references are a primary use case.  
</details>

---

**Question 22.** Examine this GitLab CI/CD YAML snippet. What is the precise behavior of `integration-test` relative to the other jobs in the pipeline?

```yaml
stages:
  - build
  - test
  - report

compile:
  stage: build
  script:
    - make compile
  artifacts:
    paths:
      - build/

unit-test:
  stage: test
  script:
    - make unit-test

integration-test:
  stage: test
  needs:
    - compile
  script:
    - make integration-test
  retry:
    max: 2
    when:
      - runner_system_failure
      - script_failure
  timeout: 10 minutes
```

A) `integration-test` waits for both `compile` AND `unit-test` to finish before starting, then retries up to 2 times on any failure  
B) `integration-test` starts as soon as `compile` finishes (not waiting for `unit-test`), retries up to 2 times only on runner system failure or script failure, and is cancelled if it runs longer than 10 minutes  
C) `integration-test` runs in parallel with `compile` because `needs` disables stage ordering entirely  
D) `integration-test` retries indefinitely because `max: 2` means 2 additional retries after the first attempt, with no timeout enforced  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `needs: [compile]` enables DAG behavior — `integration-test` starts immediately after `compile` completes, without waiting for `unit-test` (which is in the same stage). The `retry` block limits automatic retries to a maximum of 2 attempts and only triggers on the listed failure types. The `timeout: 10 minutes` setting cancels the job if it exceeds that duration, overriding the project or runner-level default.  
- **Why A is wrong:** `needs:` specifically bypasses the wait-for-full-stage behavior; `integration-test` does NOT wait for `unit-test`.  
- **Why C is wrong:** `needs` does not cause a job to skip its named dependencies; it still waits for `compile` — it just avoids waiting for `unit-test`.  
- **Why D is wrong:** `max: 2` means at most 2 retry attempts (3 total runs maximum), and `timeout` is fully enforced by GitLab.  
</details>
