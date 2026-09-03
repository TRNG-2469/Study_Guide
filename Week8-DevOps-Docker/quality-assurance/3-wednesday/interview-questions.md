# Interview Questions: Week 8 — GitLab CI/CD
**Day:** Wednesday | **Difficulty Distribution:** 70% Beginner · 25% Intermediate · 5% Advanced

---

## 🟢 Beginner — Foundational Knowledge (Q1–Q13)

### Q1: What is the purpose of the `stages` keyword in `.gitlab-ci.yml`?
**Keywords:** stages, pipeline order, execution sequence
**Hint:** Think about how GitLab groups and sequences work.
<details>
<summary>Click to Reveal Answer</summary>

The `stages` keyword defines the ordered list of stages in a GitLab CI/CD pipeline. Jobs assigned to the same stage run in parallel, while stages execute sequentially — the next stage only begins after all jobs in the current stage succeed. This gives you control over the high-level execution flow (e.g., `build → test → deploy`). If `stages` is omitted, GitLab uses default stages: `build`, `test`, and `deploy`.

</details>

---

### Q2: What is the difference between `artifacts` and `cache` in a GitLab CI job?
**Keywords:** artifacts, cache, pipeline data, job output
**Hint:** One is for sharing between jobs; the other is for speeding up repeated runs.
<details>
<summary>Click to Reveal Answer</summary>

`artifacts` are files produced by a job that are uploaded to GitLab and can be downloaded or passed to downstream jobs within the same pipeline. `cache` stores directories (like `node_modules` or `.m2`) between pipeline runs to speed up execution by avoiding repeated downloads. Artifacts are pipeline-scoped and explicitly passed between jobs, while cache is runner-scoped and used for performance optimization across runs.

</details>

---

### Q3: What does the `needs` keyword do, and how does it differ from stage ordering?
**Keywords:** needs, DAG, dependencies, stage
**Hint:** Consider directed acyclic graphs in pipeline execution.
<details>
<summary>Click to Reveal Answer</summary>

The `needs` keyword creates a Directed Acyclic Graph (DAG) by allowing a job to start as soon as its specified dependencies finish, regardless of stage ordering. Unlike stage ordering — where all jobs in a stage must complete before the next stage begins — `needs` lets jobs in later stages start early if their specific prerequisites are done. This can dramatically reduce overall pipeline duration by enabling fine-grained parallelism.

</details>

---

### Q4: What is a masked CI/CD variable and when would you use one?
**Keywords:** masked variable, secret, CI/CD variables, security
**Hint:** Think about what appears in job logs.
<details>
<summary>Click to Reveal Answer</summary>

A masked CI/CD variable has its value hidden in job logs — GitLab replaces it with `[MASKED]` whenever it appears in output. You would use masked variables to store sensitive credentials such as API keys, passwords, or tokens that must be available to jobs but should never be exposed in plain text in pipeline logs. Masked variables must meet certain formatting requirements (no whitespace, minimum length) to be eligible for masking.

</details>

---

### Q5: What is the difference between a shared runner and a project runner?
**Keywords:** shared runner, project runner, group runner, runner scope
<details>
<summary>Click to Reveal Answer</summary>

A shared runner is available to all projects within a GitLab instance and is managed by GitLab administrators, making it suitable for general-purpose workloads. A project runner is registered specifically for one project and is only available to that project, offering more control over execution environment and resource allocation. Group runners sit between these two — they are shared across all projects within a specific GitLab group. Project runners are often used when a project has special hardware, network, or security requirements.

</details>

---

### Q6: What predefined variable contains the full commit SHA?
**Keywords:** predefined variables, CI_COMMIT_SHA, commit hash
**Hint:** Look at the `CI_COMMIT_` variable family.
<details>
<summary>Click to Reveal Answer</summary>

The predefined variable `CI_COMMIT_SHA` contains the full 40-character SHA of the commit that triggered the pipeline. GitLab automatically injects this and many other predefined variables into every job without any configuration required. Related variables include `CI_COMMIT_SHORT_SHA` for the first 8 characters and `CI_COMMIT_REF_NAME` for the branch or tag name.

</details>

---

### Q7: What are the four GitLab pipeline types?
**Keywords:** pipeline types, branch pipeline, MR pipeline, scheduled pipeline, parent-child pipeline
<details>
<summary>Click to Reveal Answer</summary>

The four primary GitLab pipeline types are: (1) **Branch pipelines**, triggered by a push to any branch; (2) **Merge Request (MR) pipelines**, triggered by activity on an open merge request and used to validate changes before merging; (3) **Scheduled pipelines**, triggered on a cron-like schedule independent of code pushes; and (4) **Parent-child pipelines**, where a parent pipeline dynamically triggers one or more child pipelines, useful for monorepos or complex multi-service architectures.

</details>

---

### Q8: What is a Docker executor runner and what does it provide over a shell executor?
**Keywords:** Docker executor, shell executor, runner, isolation, container
<details>
<summary>Click to Reveal Answer</summary>

A Docker executor runner runs each CI job inside a fresh Docker container, providing strong isolation between jobs and a clean environment for every run. Unlike a shell executor — which runs jobs directly on the host machine using the host's installed tools and state — the Docker executor allows you to specify a custom Docker image per job, ensuring reproducible builds and eliminating "it works on my machine" issues. Docker executors also prevent jobs from polluting the host system or each other.

</details>

---

### Q9: What does `when: manual` do in a job definition?
**Keywords:** when, manual, trigger, job control, pipeline
<details>
<summary>Click to Reveal Answer</summary>

Setting `when: manual` on a job prevents it from running automatically as part of the pipeline flow. Instead, the job is displayed in the GitLab UI with a play button, and a user must manually click it to trigger execution. This is commonly used for deployment jobs (e.g., deploying to production) where human approval is required before proceeding. The pipeline is not blocked by a manual job — it simply waits until the job is triggered or skipped.

</details>

---

### Q10: What is the GitLab Terraform HTTP backend used for?
**Keywords:** Terraform, HTTP backend, state management, GitLab, remote state
<details>
<summary>Click to Reveal Answer</summary>

The GitLab Terraform HTTP backend is a built-in remote backend for storing Terraform state files within GitLab itself, without needing an external service like AWS S3 or HashiCorp Cloud. It uses GitLab's API to read and write state, supports state locking to prevent concurrent modifications, and ties state to a GitLab project for access control and auditability. It is configured using the `CI_API_V4_URL`, `CI_PROJECT_ID`, and `CI_JOB_TOKEN` variables, making it easy to integrate into GitLab CI pipelines.

</details>

---

### Q11: What does `extends` do in a `.gitlab-ci.yml`?
**Keywords:** extends, job template, reuse, DRY, inheritance
<details>
<summary>Click to Reveal Answer</summary>

The `extends` keyword allows a job to inherit configuration from another job or a hidden job template (prefixed with `.`), promoting the DRY (Don't Repeat Yourself) principle. The inheriting job merges the parent's configuration with its own, with the child's values taking precedence in case of conflicts. This is commonly used to define reusable base configurations for common patterns like Docker login steps, shared `before_script` blocks, or common `rules`, and then extend them across multiple jobs.

</details>

---

### Q12: What is `CI_JOB_TOKEN` used for?
**Keywords:** CI_JOB_TOKEN, authentication, GitLab API, registry, predefined variable
<details>
<summary>Click to Reveal Answer</summary>

`CI_JOB_TOKEN` is a short-lived, automatically generated token that GitLab injects into every CI job for authentication purposes. It can be used to authenticate against the GitLab API, pull from or push to the GitLab Container Registry, access the Package Registry, and interact with the Terraform state HTTP backend — all without needing to store a personal access token as a CI variable. The token is scoped to the current job and expires when the job finishes, making it more secure than long-lived credentials.

</details>

---

### Q13: What does `include` allow you to do in a pipeline configuration?
**Keywords:** include, modular pipeline, reuse, template, remote config
**Hint:** Think about splitting a large `.gitlab-ci.yml` into smaller pieces.
<details>
<summary>Click to Reveal Answer</summary>

The `include` keyword allows you to split your pipeline configuration across multiple YAML files and import them into your main `.gitlab-ci.yml`. You can include files from the same repository (`local`), from other GitLab projects (`project`), from a remote URL (`remote`), or from GitLab's built-in template library (`template`). This promotes modularity and reuse — for example, a shared team could maintain common job templates in a central repository that all projects include, rather than duplicating configuration everywhere.

</details>

---

## 🟡 Intermediate — Application & Scenario (Q14–Q17)

### Q14: A job in your pipeline needs a secret API key. How do you store and inject it safely using GitLab CI variables?
**Keywords:** CI/CD variables, masked variable, protected variable, environment variable, secrets management
<details>
<summary>Click to Reveal Answer</summary>

Store the API key in **Settings → CI/CD → Variables** with the **Masked** flag enabled so it never appears in job logs, and optionally set it as **Protected** so it is only injected into jobs running on protected branches or tags. In the job, reference it as a standard environment variable (e.g., `$API_KEY`). For additional security, consider using a secrets manager (like HashiCorp Vault) via the `secrets` keyword in GitLab CI to pull credentials dynamically at job runtime rather than storing them statically in GitLab. Never hardcode secrets in `.gitlab-ci.yml` or pass them as plain job arguments.

</details>

---

### Q15: Your pipeline has a deploy job that should only run on the `main` branch, not on feature branches. How do you configure this with `rules`?
**Keywords:** rules, branch filtering, CI_COMMIT_BRANCH, conditional execution, deploy
<details>
<summary>Click to Reveal Answer</summary>

Use the `rules` keyword with an `if` clause checking the predefined variable `CI_COMMIT_BRANCH`. For example: `rules: - if: '$CI_COMMIT_BRANCH == "main"'`. This ensures the deploy job is only added to the pipeline when the pipeline runs on the `main` branch. You can add `when: never` as a fallback to explicitly exclude it from all other pipelines. The `rules` keyword is more powerful than the older `only/except` syntax, supporting complex conditions, variable expressions, and `changes` file path filters.

</details>

---

### Q16: The build stage takes 8 minutes and the test stage depends on it. How do you use `needs` and `artifacts` to minimize overall pipeline time?
**Keywords:** needs, artifacts, DAG, pipeline optimization, parallel jobs
<details>
<summary>Click to Reveal Answer</summary>

Configure the build job to publish its compiled output as `artifacts`, then use `needs: [build-job]` on each test job to declare an explicit dependency. With `needs`, the test jobs can start the moment the build job finishes — without waiting for all other jobs in the build stage to complete. By combining `needs` with `artifacts: paths`, the test jobs automatically download only the build outputs they require. This DAG approach can turn a linear 8-min build + N-min test wait into a much shorter wall-clock time when multiple tests run in parallel immediately after the build artifact is available.

</details>

---

### Q17: Your Terraform `plan` job generates a plan output that needs to be reviewed before `apply`. How do you implement this pattern in GitLab CI?
**Keywords:** Terraform plan, apply, manual approval, artifacts, when: manual
<details>
<summary>Click to Reveal Answer</summary>

Configure the `terraform plan` job to save its output using `terraform plan -out=tfplan` and publish `tfplan` as an artifact. Then define a `terraform apply` job that uses `needs` to depend on the plan job, downloads the plan artifact, and is set to `when: manual` so it requires a human to click "Play" in the GitLab UI before executing. This creates a gated workflow: the plan output is visible in the artifacts for review, and no infrastructure changes are applied until an authorized team member approves. You can further restrict the apply job to protected branches and specific user roles for additional governance.

</details>

---

## 🔴 Advanced — Deep Dive & System Design (Q18)

### Q18: You are migrating a monolithic Jenkins pipeline to GitLab CI for a microservices project with 12 services. Describe how you would structure the pipeline using parent-child pipelines, shared runner tags, reusable job templates with `extends`, and how you'd manage environment-specific secrets across dev/staging/prod without duplicating variable definitions.
**Keywords:** parent-child pipelines, extends, runner tags, environment-specific variables, monorepo, microservices, secrets management, pipeline architecture
<details>
<summary>Click to Reveal Answer</summary>

**Pipeline Structure:** Create a root `.gitlab-ci.yml` that acts as the parent pipeline. Use `trigger` jobs with `include` and `rules: changes` to detect which service directories changed, then dynamically trigger child pipelines only for affected services — avoiding rebuilding all 12 services on every commit. Each service gets its own `ci/<service>/.gitlab-ci.yml` inheriting shared templates via `include: project`.

**Reusable Templates:** Define hidden jobs (`.docker-build`, `.deploy-template`, `.security-scan`) in a shared templates repository. Each service's pipeline uses `extends` to inherit these templates and overrides only service-specific values (image name, deploy target). This keeps hundreds of lines of duplicated YAML consolidated into a single source of truth.

**Runner Tags:** Register specialized runners with tags (e.g., `docker-build`, `k8s-deploy`, `gpu-test`) and use `tags` in job definitions to route jobs to appropriate infrastructure. Shared runners handle lightweight jobs; tagged runners handle resource-intensive or environment-specific workloads (e.g., a `prod-deploy` tagged runner with locked-down network access).

**Environment-Specific Secrets:** Use GitLab's **environment-scoped CI/CD variables** — define `DB_PASSWORD`, `API_KEY`, etc. once per environment scope (`dev/*`, `staging/*`, `production/*`) rather than per-service. Deploy jobs reference the `environment` keyword, and GitLab automatically injects the matching scoped variables. For highly sensitive secrets, integrate HashiCorp Vault via the `secrets` keyword so credentials are fetched dynamically at job runtime and never stored statically in GitLab — this also enables automatic rotation without pipeline changes.

</details>

---
