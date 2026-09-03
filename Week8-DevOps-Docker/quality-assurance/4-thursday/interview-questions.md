# Interview Questions: Week 8 — Docker Mastery
**Day:** Thursday | **Difficulty Distribution:** 70% Beginner · 25% Intermediate · 5% Advanced

---

## 🟢 Beginner — Foundational Knowledge (Q1–Q14)

### Q1: What problem does Docker solve, and what is the "works on my machine" problem?
**Keywords:** portability, environment parity, containers
**Hint:** Think about what happens when code runs fine locally but fails in staging.
<details>
<summary>Click to Reveal Answer</summary>

Docker solves the classic "works on my machine" problem by packaging an application together with all its dependencies, configuration, and runtime into a single portable unit called a container. Before Docker, differences in OS libraries, language versions, or environment variables between a developer's laptop and a production server would cause unpredictable failures. With Docker, the container runs identically regardless of the underlying host system, guaranteeing environment parity from development through production.
</details>

---

### Q2: What are the three main components of Docker architecture?
**Keywords:** Docker Client, Docker Daemon, Docker Registry
<details>
<summary>Click to Reveal Answer</summary>

The three main components are the Docker Client, the Docker Daemon (dockerd), and a Docker Registry. The Docker Client is the CLI tool (`docker`) that users interact with to issue commands. The Docker Daemon is the background service that actually builds, runs, and manages containers and images. The Registry (such as Docker Hub or Amazon ECR) is the remote store where Docker images are pushed to and pulled from.
</details>

---

### Q3: What is the Docker daemon (dockerd)?
**Keywords:** dockerd, background service, REST API, socket
<details>
<summary>Click to Reveal Answer</summary>

The Docker daemon (`dockerd`) is a persistent background service that listens for Docker API requests — typically over a Unix socket (`/var/run/docker.sock`) or a TCP port — and manages Docker objects such as images, containers, networks, and volumes. It is the engine that actually executes operations; the Docker CLI simply sends requests to the daemon and displays the results. On Linux the daemon runs as a systemd service, and its behavior can be configured via `/etc/docker/daemon.json`.
</details>

---

### Q4: How do you install Docker on a Linux server, and what command verifies a successful installation?
**Keywords:** apt/yum, docker install, docker version, docker run hello-world
<details>
<summary>Click to Reveal Answer</summary>

On Ubuntu/Debian you install Docker Engine by adding Docker's official apt repository, then running `sudo apt-get install docker-ce docker-ce-cli containerd.io`. On RHEL/CentOS you use `sudo yum install docker-ce`. After installation, `docker version` prints the client and server versions confirming the daemon is reachable, and `docker run hello-world` performs an end-to-end test by pulling a tiny image and running it successfully.
</details>

---

### Q5: What is the difference between a Docker image and a Docker container?
**Keywords:** image, container, read-only, writable layer, running instance
<details>
<summary>Click to Reveal Answer</summary>

A Docker image is a read-only, layered template that contains everything needed to run an application — OS libraries, application code, dependencies, and configuration. A Docker container is a live, runnable instance created from an image; it adds a thin writable layer on top of the image's read-only layers for any runtime changes. You can create many containers from a single image, just as you can launch many processes from one executable.
</details>

---

### Q6: What is a Docker layer, and why does layer caching matter for build performance?
**Keywords:** Union filesystem, layer, cache invalidation, build speed
<details>
<summary>Click to Reveal Answer</summary>

Each instruction in a Dockerfile (`RUN`, `COPY`, `ADD`, etc.) produces an immutable filesystem layer that is stacked using a union filesystem (OverlayFS on modern Linux). Docker caches each layer by its content hash; if nothing upstream has changed, Docker reuses the cached layer instead of recomputing it, dramatically speeding up subsequent builds. Because cache is invalidated from the changed layer downward, you should place rarely-changing instructions (e.g., installing OS packages) before frequently-changing ones (e.g., copying application source code).
</details>

---

### Q7: What is the difference between CMD and ENTRYPOINT in a Dockerfile?
**Keywords:** CMD, ENTRYPOINT, default command, override, exec form
<details>
<summary>Click to Reveal Answer</summary>

`ENTRYPOINT` defines the fixed executable that always runs when a container starts and cannot be overridden without the `--entrypoint` flag. `CMD` provides default arguments to the entrypoint (or a default command if no entrypoint is set) that can be easily replaced by appending arguments to `docker run`. Best practice is to set `ENTRYPOINT` to your application binary and use `CMD` for default flags, giving callers flexibility to pass custom arguments without re-specifying the executable.
</details>

---

### Q8: What does `docker run -p 8080:80` mean?
**Keywords:** port mapping, host port, container port, -p flag
<details>
<summary>Click to Reveal Answer</summary>

The `-p 8080:80` flag maps port 8080 on the Docker host to port 80 inside the container. Traffic arriving at `localhost:8080` on the host machine is forwarded by Docker's networking layer to port 80 of the running container. The format is always `HOST_PORT:CONTAINER_PORT`; multiple `-p` flags can be supplied to expose additional ports.
</details>

---

### Q9: What is the difference between a named volume and a bind mount in Docker?
**Keywords:** named volume, bind mount, /var/lib/docker/volumes, host path, persistence
<details>
<summary>Click to Reveal Answer</summary>

A named volume is managed entirely by Docker — stored under `/var/lib/docker/volumes/` — making it portable, easy to back up, and independent of the host directory structure. A bind mount maps a specific directory or file from the host filesystem directly into the container, giving full control over the exact host path. Named volumes are recommended for persistent production data (e.g., databases), while bind mounts are preferred for development workflows where you want live code changes reflected inside the container instantly.
</details>

---

### Q10: What does `docker compose down` do compared to `docker compose stop`?
**Keywords:** compose down, compose stop, remove containers, networks, volumes
<details>
<summary>Click to Reveal Answer</summary>

`docker compose stop` gracefully stops running containers but leaves them (and their networks) intact so they can be restarted quickly with `docker compose start`. `docker compose down` goes further: it stops all containers AND removes them along with the networks defined in the Compose file, effectively tearing down the entire environment. Adding `--volumes` to `docker compose down` also deletes named volumes, wiping persistent data — use this flag with caution.
</details>

---

### Q11: What is a multi-stage Dockerfile build and why is it used?
**Keywords:** multi-stage, FROM AS, build stage, final image, image size
<details>
<summary>Click to Reveal Answer</summary>

A multi-stage build uses multiple `FROM` instructions in a single Dockerfile, each defining a separate stage with its own base image. You compile or build the application in an early "builder" stage (which can include compilers, SDKs, and build tools), then copy only the compiled artifacts into a lean final stage based on a minimal runtime image. This eliminates build-time dependencies from the shipped image, often reducing size by hundreds of megabytes and shrinking the attack surface significantly.
</details>

---

### Q12: What does the HEALTHCHECK instruction do in a Dockerfile?
**Keywords:** HEALTHCHECK, health status, CMD, interval, retries
<details>
<summary>Click to Reveal Answer</summary>

`HEALTHCHECK` instructs Docker to periodically run a command inside the container to determine whether the application is functioning correctly. Docker marks the container as `healthy`, `unhealthy`, or `starting` based on the command's exit code (0 = healthy, 1 = unhealthy). Docker Compose and orchestrators like ECS use this status to delay dependent services from starting and to replace failed containers automatically, making it essential for production reliability.
</details>

---

### Q13: What is the difference between COPY and ADD in a Dockerfile?
**Keywords:** COPY, ADD, tar extraction, URL fetch, best practice
<details>
<summary>Click to Reveal Answer</summary>

`COPY` simply copies files or directories from the build context into the image filesystem — straightforward and predictable. `ADD` does everything `COPY` does but also auto-extracts local `.tar` archives and can fetch files from remote URLs. The Docker best-practice recommendation is to use `COPY` by default because its behavior is explicit and easier to reason about; reserve `ADD` only when you specifically need its auto-extraction feature, and never use it to fetch remote files (use `RUN curl` or a separate stage instead).
</details>

---

### Q14: Why should you never use the `latest` tag in a production Dockerfile?
**Keywords:** latest tag, immutability, reproducible builds, pinned version
<details>
<summary>Click to Reveal Answer</summary>

The `latest` tag is a mutable pointer — it changes every time a new image is pushed, meaning two builds from the same Dockerfile at different points in time may pull entirely different base images. This breaks reproducibility: a security update or breaking change in the upstream image can silently alter your container's behavior or introduce incompatibilities. Production Dockerfiles must pin to specific, immutable digest-based tags (e.g., `openjdk:21.0.3-jre-slim`) so every build is byte-for-byte predictable and auditable.
</details>

---

## 🟡 Intermediate — Application & Scenario (Q15–Q19)

### Q15: Your Spring Boot container exits immediately after starting. How do you diagnose this using Docker CLI commands?
**Keywords:** docker logs, docker ps -a, exit code, docker inspect, stderr
<details>
<summary>Click to Reveal Answer</summary>

Start by running `docker ps -a` to confirm the container exited and note its container ID and exit code. Then run `docker logs <container_id>` to read the application's stdout/stderr output, which usually reveals the root cause (missing environment variable, port conflict, application startup exception). If the logs are insufficient, use `docker inspect <container_id>` to examine the full container state, including environment variables, mounts, and the exact command that was run. For a Spring Boot app specifically, look for a `BeanCreationException` or "Port already in use" error in the logs.
</details>

---

### Q16: A new developer can't pull from your private ECR registry in CI. What's the most likely cause and how do you fix it?
**Keywords:** ECR authentication, aws ecr get-login-password, IAM role, docker login, 401 Unauthorized
<details>
<summary>Click to Reveal Answer</summary>

The most likely cause is that the CI runner is not authenticated to ECR. ECR uses short-lived tokens (valid 12 hours) obtained by running `aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account>.dkr.ecr.<region>.amazonaws.com`. In CI, this requires the runner to have an IAM role or access key with `ecr:GetAuthorizationToken` and `ecr:BatchGetImage` permissions. Fix it by adding an authentication step before any `docker pull`/`docker push` commands in the pipeline, and ensure the CI environment has the correct IAM credentials or instance role attached.
</details>

---

### Q17: Your PostgreSQL container loses all data when restarted. How do you fix this, and what volume type would you use?
**Keywords:** named volume, data persistence, postgres data directory, docker run -v, compose volumes
<details>
<summary>Click to Reveal Answer</summary>

PostgreSQL stores its data in `/var/lib/postgresql/data` inside the container; without a volume, this directory lives only in the container's writable layer and is discarded on removal. Fix it by attaching a named volume to that path — either `docker run -v pgdata:/var/lib/postgresql/data postgres` or declaring it in Compose with a `volumes:` block. A named volume is the correct choice here because Docker manages its lifecycle independently of the container, it survives `docker compose down` (without `--volumes`), and it can be backed up or migrated easily without knowing the host path.
</details>

---

### Q18: Your Docker image is 1.2 GB. Name 4 concrete steps you would take to reduce its size.
**Keywords:** multi-stage build, alpine base, .dockerignore, layer consolidation, apt-get clean
<details>
<summary>Click to Reveal Answer</summary>

First, switch to a multi-stage build so build-time tools (JDK, Maven, npm) never appear in the final image — only the compiled artifact does. Second, choose a minimal base image for the runtime stage, such as `eclipse-temurin:21-jre-alpine` instead of a full JDK or Ubuntu image. Third, add a `.dockerignore` file to exclude `target/`, `node_modules/`, `.git/`, and test files from the build context so they are never copied in. Fourth, consolidate `RUN` instructions and clean up package manager caches in the same layer (e.g., `apt-get install ... && rm -rf /var/lib/apt/lists/*`) so the cache files do not persist in the image.
</details>

---

### Q19: How do you pass different database credentials to the same Docker image in dev vs prod without rebuilding the image?
**Keywords:** environment variables, --env-file, Docker secrets, compose env_file, 12-factor app
<details>
<summary>Click to Reveal Answer</summary>

Never bake credentials into the image — follow the 12-factor app principle by reading all configuration from environment variables at runtime. For local development, use a `.env` file referenced by Compose (`env_file: .env.dev`) or pass variables with `docker run -e DB_PASSWORD=...`. For production, inject secrets securely: ECS task definitions support referencing AWS Secrets Manager or Parameter Store values that are injected as environment variables at container startup. This way, the exact same immutable image artifact is deployed to every environment, with only the runtime environment differing.
</details>

---

## 🔴 Advanced — Deep Dive & System Design (Q20)

### Q20: You are containerizing a Spring Boot microservices application with 5 services. Walk through your complete Docker strategy: Dockerfile design for each service (multi-stage, non-root, HEALTHCHECK, pinned tags), Docker Compose configuration for local development (networking, volume strategy, health-check ordering with depends_on), and how the same images are promoted from local → CI registry → ECR → production ECS without rebuilding.
**Keywords:** multi-stage build, non-root user, HEALTHCHECK, image promotion, ECR, ECS task definition, Docker Compose depends_on, service_healthy, immutable tags, CI/CD pipeline
<details>
<summary>Click to Reveal Answer</summary>

**Dockerfile Design:** Each microservice uses a two-stage Dockerfile. Stage 1 (`FROM maven:3.9.6-eclipse-temurin-21 AS builder`) compiles the JAR with `mvn package -DskipTests`. Stage 2 (`FROM eclipse-temurin:21.0.3-jre-alpine`) copies only the JAR, creates a dedicated non-root user (`RUN addgroup -S appgroup && adduser -S appuser -G appgroup`), switches to that user (`USER appuser`), sets a `HEALTHCHECK` (`HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1`), and uses `ENTRYPOINT ["java","-jar","app.jar"]`. All base image tags are pinned to exact patch versions (never `latest`).

**Docker Compose (Local Dev):** A single `compose.yml` defines all 5 services plus PostgreSQL and any message broker. Each service declares `healthcheck:` mirroring the Dockerfile check, and dependent services use `depends_on: db: condition: service_healthy` so Spring Boot never starts before the database is accepting connections. A shared custom bridge network (`networks: backend:`) isolates services from the default bridge. Persistent data uses named volumes (`pgdata:/var/lib/postgresql/data`); source code for hot-reload services uses bind mounts. Environment-specific variables come from `.env.dev` via `env_file`.

**Image Promotion Strategy:** In CI (GitHub Actions/GitLab CI), the pipeline builds once and tags the image with the Git commit SHA (e.g., `myapp-orders:abc1234`). This immutable SHA tag is pushed to a CI-internal registry (GitLab Registry or GHCR) for integration testing. On merge to main, the same pre-tested image (identified by SHA) is re-tagged with a semantic version and pushed to Amazon ECR — no rebuild, no code change, the exact tested artifact is promoted. The ECS task definition references the ECR image URI with the immutable SHA or semantic version tag; ECS performs a rolling deployment pulling the new tag. Environment-specific secrets (DB passwords, API keys) are stored in AWS Secrets Manager and injected at container startup via the ECS task definition's `secrets:` block, keeping them out of the image and out of source control entirely.
</details>

---
