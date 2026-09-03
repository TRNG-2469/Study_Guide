# GitLab Runners

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what a GitLab Runner is and how it relates to a pipeline job
- Distinguish between shared, group, and project-scoped runners
- Describe the available runner executors and when to use each
- Register a runner using `gitlab-runner register`
- Configure the Docker executor
- Use runner tags to route jobs to the correct runner

---

## Why This Matters

A pipeline definition in `.gitlab-ci.yml` is just a plan — it describes what should happen, but nothing actually happens until a **runner** picks up the work. Runners are the machines, containers, or virtual environments that physically execute your jobs. Choosing the right runner type and configuring it correctly determines how fast your pipeline runs, how isolated your build environment is, and how much control you have over your build infrastructure. Understanding runners is the bridge between writing CI/CD configuration and having it work reliably in the real world.

---

## What Is a GitLab Runner?

A **GitLab Runner** is an open-source agent program installed on a machine (virtual or physical, on-premise or cloud) that polls GitLab for available jobs, executes them, and reports results back to GitLab.

Think of it like a food delivery driver. GitLab (the restaurant) has orders (jobs) waiting. The runner (driver) checks for new orders, picks one up, completes the delivery (executes the job), and reports back when done. Multiple runners can work simultaneously, just as multiple drivers can deliver different orders at the same time.

The runner agent is a single binary called `gitlab-runner`. It is separate from the GitLab server itself — you can install it on any machine with network access to your GitLab instance.

---

## Runner Scope

GitLab runners are scoped to determine which projects they can serve.

### Shared Runners

**Shared runners** are available to all projects in a GitLab instance. They are managed by GitLab administrators (or by GitLab.com itself for the hosted service). Most teams start with shared runners because no setup is required.

- **Pros:** Zero configuration for project teams, always available, maintained by admins
- **Cons:** Shared with all users (potential queue), less control over environment, may have usage limits on GitLab.com

On GitLab.com, shared runners are provided by GitLab and billed by compute minutes (free tier: 400 minutes/month on the free plan).

### Group Runners

**Group runners** are available to all projects within a specific GitLab group (and its subgroups). They are registered by a group owner and provide a middle ground between shared runners and project-specific runners.

- **Pros:** Dedicated to your organization's projects, not shared with the whole instance
- **Cons:** Requires a group owner to register and maintain them

### Project Runners

**Project runners** (formerly called "specific runners") are available only to a single project. They are registered by a project maintainer.

- **Pros:** Full control — you choose the hardware, OS, and configuration
- **Cons:** You are responsible for all maintenance, scaling, and uptime

---

## Runner Executors

When a runner picks up a job, it needs to know **how** to execute it. The **executor** defines the execution environment.

### Shell Executor

The runner executes job commands directly on the host machine using the system shell (bash, PowerShell, etc.).

- **Pros:** Simple, fast, no Docker required
- **Cons:** Jobs run on the same OS as the host — no isolation, host can be polluted by builds, hard to reproduce exact environments

**Best for:** Simple scripts, machines dedicated to a single project, cases where Docker is unavailable.

### Docker Executor

The runner starts a fresh Docker container for each job using the `image` specified in the job definition. The job runs inside the container, and the container is discarded afterward.

- **Pros:** Excellent isolation — each job starts clean; reproducible environments; easy to change tool versions per job
- **Cons:** Requires Docker to be installed on the runner host; slightly slower startup than shell executor

**Best for:** Most CI/CD workloads. This is the recommended executor for the majority of teams.

### Kubernetes Executor

The runner creates a Kubernetes pod for each job. When the job finishes, the pod is deleted. This executor scales automatically — Kubernetes provisions and deprovisions compute resources on demand.

- **Pros:** Highly scalable, cloud-native, auto-scaling, excellent for high-volume pipelines
- **Cons:** Requires a Kubernetes cluster; more complex to configure

**Best for:** Large engineering organizations with existing Kubernetes infrastructure.

### Other Executors

| Executor | Description |
|---|---|
| `docker-autoscaler` | Spins up cloud VMs on demand, runs Docker executor in each |
| `virtualbox` | Creates VirtualBox VMs for each job (good for macOS/Windows testing) |
| `ssh` | Connects to a remote host via SSH and runs commands there |
| `custom` | Calls your own scripts to prepare, run, and clean up the environment |

---

## Installing `gitlab-runner`

On a Linux host (Ubuntu/Debian):

```bash
# 1. Add the GitLab Runner package repository.
curl -L "https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.deb.sh" | sudo bash

# 2. Install the gitlab-runner package.
sudo apt-get install gitlab-runner

# 3. Verify the installation.
gitlab-runner --version
```

On macOS (using Homebrew):

```bash
brew install gitlab-runner
brew services start gitlab-runner
```

---

## Registering a Runner

Registration is the process of linking a runner agent to a GitLab project, group, or instance. You need a **registration token**, which you find in GitLab under:

- **Shared runner:** Admin Area > CI/CD > Runners > Registration token
- **Group runner:** Group > Settings > CI/CD > Runners > Registration token
- **Project runner:** Project > Settings > CI/CD > Runners > Set up a specific runner

### Interactive Registration

```bash
# Run the registration command. GitLab will prompt you for each setting.
sudo gitlab-runner register
```

You will be asked:

```
Enter the GitLab instance URL (for example, https://gitlab.com/):
https://gitlab.com/

Enter the registration token:
<paste your token here>

Enter a description for the runner:
my-docker-runner

Enter tags for the runner (comma-separated):
docker,java,linux

Enter optional maintenance note for the runner:
(leave blank)

Enter an executor: shell, docker, docker-autoscaler, ...:
docker

Enter the default Docker image (for example, ruby:2.7):
maven:3.9-eclipse-temurin-17
```

### Non-Interactive Registration (for automation/scripts)

```bash
sudo gitlab-runner register \
  --non-interactive \
  --url "https://gitlab.com/" \
  --registration-token "YOUR_REGISTRATION_TOKEN" \
  --description "docker-runner-01" \
  --tag-list "docker,java,linux" \
  --executor "docker" \
  --docker-image "maven:3.9-eclipse-temurin-17" \
  --docker-privileged              # Required for Docker-in-Docker builds
```

After registration, the runner configuration is stored in `/etc/gitlab-runner/config.toml` (Linux) or `~/.gitlab-runner/config.toml` (macOS/user install).

---

## Configuring the Docker Executor

The runner configuration file (`config.toml`) contains detailed settings for each registered runner. Here is an example configuration for a Docker executor runner:

```toml
# /etc/gitlab-runner/config.toml

# Global settings — apply to all runners on this host.
concurrent = 4          # Allow up to 4 jobs to run simultaneously on this host
check_interval = 0      # How often (seconds) to poll GitLab for new jobs (0 = default 3s)
log_level = "info"      # Logging verbosity: debug, info, warn, error

[[runners]]
  name = "docker-runner-01"              # Human-readable name shown in GitLab UI
  url = "https://gitlab.com/"
  token = "RUNNER_TOKEN_FROM_REGISTRATION"

  executor = "docker"                    # Use the Docker executor

  [runners.docker]
    image = "maven:3.9-eclipse-temurin-17"  # Default image if job doesn't specify one
    privileged = false                       # Set true only if jobs need Docker-in-Docker
    disable_entrypoint_overwrite = false
    oom_kill_disable = false
    disable_cache = false

    # Mount volumes into every job container.
    # Cache the Docker socket so jobs can use the host Docker daemon.
    volumes = ["/cache", "/var/run/docker.sock:/var/run/docker.sock"]

    # How long (seconds) to wait for the container to start.
    wait_for_services_timeout = 30

    # Pull policy: always pull fresh image, or use cached if available.
    pull_policy = ["if-not-present"]     # Options: always, if-not-present, never

  [runners.cache]
    Type = "local"                       # Store cache on the runner's local disk
    Shared = false
```

After editing `config.toml`, restart the runner:

```bash
sudo gitlab-runner restart
```

---

## Runner Tags

**Tags** are labels you assign to a runner during registration. In your `.gitlab-ci.yml`, jobs can specify which tags a runner must have to pick them up. This is how you route specific jobs to specific infrastructure.

For example, if you have both a lightweight runner for quick scripts and a powerful runner with GPU access for ML jobs:

```yaml
# This job requires a runner tagged 'docker'.
# Only runners with the 'docker' tag will pick it up.
build-jar:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  tags:
    - docker
  script:
    - mvn clean package

# This job requires a runner tagged 'gpu' — a specialized machine.
train-model:
  stage: train
  tags:
    - gpu
    - linux
  script:
    - python train.py --epochs 50

# A job with no 'tags' key will be picked up by any runner
# that has 'Run untagged jobs' enabled.
simple-lint:
  stage: test
  script:
    - echo "Running linter..."
```

**Best practice:** Always tag your runners and specify tags in jobs. This prevents jobs from being accidentally picked up by the wrong runner type (e.g., a Windows runner picking up a Linux shell script job).

---

## Verifying Runner Status

Check which runners are available to a project:
**Project > Settings > CI/CD > Runners** (scroll down)

You will see:
- Runner name and description
- Last contacted time (green dot = online, gray = offline)
- Tags
- Whether it is paused or active

To list runners from the command line on the runner host:

```bash
sudo gitlab-runner list
# Output:
# Runtime platform arch=amd64 os=linux pid=12345
# Listing configured runners          ConfigFile=/etc/gitlab-runner/config.toml
# docker-runner-01                    Executor=docker Token=abc123 URL=https://gitlab.com/
```

---

## Summary

| Concept | Description |
|---|---|
| Shared runner | Available to all projects on the instance |
| Group runner | Available to all projects in a group |
| Project runner | Available to one specific project |
| Shell executor | Runs commands directly on the host OS |
| Docker executor | Runs commands inside a fresh Docker container |
| Kubernetes executor | Creates a Kubernetes pod for each job |
| Registration | Links a runner agent to GitLab using a token |
| Tags | Labels used to route jobs to specific runners |

---

## External Resources

- [GitLab Runner installation guide](https://docs.gitlab.com/runner/install/)
- [Registering runners](https://docs.gitlab.com/runner/register/)
- [Runner executors reference](https://docs.gitlab.com/runner/executors/)
