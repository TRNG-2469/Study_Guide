# Weekly Knowledge Check: Week 8 — Docker Mastery
**Day:** Thursday | **Topics:** Docker Architecture · Dockerfile · Images · Containers · Docker Compose · Volumes · Registries · Best Practices

---

## Part 1: Multiple Choice

**1. What problem does Docker primarily solve in software development and deployment?**

A) Replacing virtual machines entirely with a lighter-weight hypervisor
B) "Works on my machine" inconsistency by packaging apps with all their dependencies into portable containers
C) Speeding up application code execution through JIT compilation inside containers
D) Providing a cloud-native replacement for operating system kernels

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** Docker bundles an application and its dependencies (libraries, configs, runtime) into a container image, ensuring the environment is identical across dev, staging, and production — eliminating environment drift.
- **Why A is wrong:** Docker containers share the host OS kernel; they do not introduce a new hypervisor.
- **Why C is wrong:** Docker has no JIT compiler; it does not inherently speed up application code.
- **Why D is wrong:** Containers use the host kernel; Docker does not replace or provide an OS kernel.
</details>

---

**2. Which component of Docker architecture is responsible for actually building, running, and managing containers?**

A) Docker Client (`docker` CLI)
B) Docker Registry
C) Docker Daemon (`dockerd`)
D) Docker Compose

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C
**Explanation:** The Docker Daemon (`dockerd`) is the long-running background service that listens for Docker API requests and manages images, containers, networks, and volumes. The CLI is just a client that sends commands to the daemon.
- **Why A is wrong:** The Docker Client is a CLI tool that translates user commands into REST API calls sent to the daemon.
- **Why B is wrong:** A Registry stores and distributes image layers; it does not run containers.
- **Why D is wrong:** Docker Compose is an orchestration tool that calls the Docker API; it does not manage containers directly.
</details>

---

**3. What is the correct order of the Docker build context flow when you run `docker build .`?**

A) Registry → Daemon → Client → Image
B) Client → Daemon → Build Context → Image Layers
C) Daemon → Client → Dockerfile → Registry
D) Dockerfile → Registry → Daemon → Container

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** The Docker Client sends the build context (files in `.`) and the Dockerfile to the Daemon. The Daemon then processes each Dockerfile instruction, creating image layers that form the final image.
- **Why A is wrong:** The Registry is not involved in a local build unless a base image needs to be pulled.
- **Why C is wrong:** The Daemon does not initiate a build; it receives the request from the Client.
- **Why D is wrong:** The Dockerfile does not push to the Registry; that is a separate `docker push` step.
</details>

---

**4. In a Dockerfile, what is the key difference between `COPY` and `ADD`?**

A) `COPY` supports remote URLs and tar auto-extraction; `ADD` only copies local files
B) `ADD` supports remote URLs and automatic tar extraction; `COPY` only copies local files/directories
C) They are identical; `ADD` is just an alias for `COPY` in newer Docker versions
D) `COPY` runs as root; `ADD` respects the USER instruction

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** `ADD` has two extra features over `COPY`: it can fetch files from remote URLs and it automatically extracts `.tar` archives into the destination. Best practice is to prefer `COPY` unless you specifically need these features.
- **Why A is wrong:** This has `COPY` and `ADD` reversed.
- **Why C is wrong:** They are distinct instructions with different behaviors.
- **Why D is wrong:** Both instructions use the same user context; `USER` affects subsequent `RUN`/`CMD`/`ENTRYPOINT`, not `COPY`/`ADD` themselves.
</details>

---

**5. What does the port mapping `-p 8080:80` mean in `docker run -p 8080:80 nginx`?**

A) The container listens on port 8080 and maps it to host port 80
B) The host listens on port 8080 and forwards traffic to container port 80
C) Both the host and container expose port 8080, with 80 as a fallback
D) Port 80 on the host is aliased to port 8080 inside the container network

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** The `-p` flag syntax is `HOST_PORT:CONTAINER_PORT`. So `-p 8080:80` means: bind host port 8080 to container port 80. Requests hitting `localhost:8080` on the host are forwarded to port 80 inside the container.
- **Why A is wrong:** This reverses the host/container roles in the mapping.
- **Why C is wrong:** There is no fallback concept; the format is strictly host:container.
- **Why D is wrong:** Port 80 on the host is not involved; the host port is 8080.
</details>

---

**6. What happens when you pass additional arguments to `docker run myimage arg1` and the Dockerfile has both `ENTRYPOINT ["python", "app.py"]` and `CMD ["--help"]`?**

A) Both `CMD` and the extra argument are appended: `python app.py --help arg1`
B) `arg1` replaces `CMD`, so the container runs: `python app.py arg1`
C) `arg1` replaces `ENTRYPOINT`, so the container runs: `arg1 --help`
D) The container fails because you cannot combine `ENTRYPOINT` and extra arguments

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** In exec form, `ENTRYPOINT` is fixed and `CMD` provides default arguments. Any arguments passed via `docker run` override `CMD` entirely. So `docker run myimage arg1` runs `python app.py arg1`.
- **Why A is wrong:** `CMD` is fully replaced by the runtime argument, not appended.
- **Why C is wrong:** Runtime arguments override `CMD`, not `ENTRYPOINT`.
- **Why D is wrong:** This is perfectly valid Docker behavior; `ENTRYPOINT` + runtime args is a common pattern.
</details>

---

**7. Which Docker image registry is tightly integrated with AWS IAM for authentication?**

A) Docker Hub
B) GitHub Container Registry (GHCR)
C) Amazon Elastic Container Registry (ECR)
D) GitLab Container Registry

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C
**Explanation:** Amazon ECR uses AWS IAM roles and policies for access control. Authentication requires generating a temporary login token via the AWS CLI, unlike Docker Hub which uses username/password or access tokens.
- **Why A is wrong:** Docker Hub uses its own username/password or Docker access tokens, not IAM.
- **Why B is wrong:** GHCR uses GitHub Personal Access Tokens, not AWS IAM.
- **Why D is wrong:** GitLab Registry uses GitLab CI/CD tokens or deploy tokens.
</details>

---

**8. In Docker Compose, what must a dependency service define for `condition: service_healthy` to work in `depends_on`?**

A) A `restart: always` policy
B) A `HEALTHCHECK` instruction in its Dockerfile or a `healthcheck` block in `docker-compose.yml`
C) An exposed port that Docker can probe
D) A `volumes` mount so Docker can read health log files

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** `condition: service_healthy` requires the dependency service to have a health check defined — either via `HEALTHCHECK` in its Dockerfile or a `healthcheck:` block in Compose. Without a health check, the service can never become "healthy" and the dependent service will never start.
- **Why A is wrong:** `restart: always` controls restart behavior, not health status.
- **Why C is wrong:** An exposed port alone does not constitute a health check.
- **Why D is wrong:** Volumes are not involved in health checking.
</details>

---

**9. What is a named volume in Docker, and who manages it?**

A) A host directory path you specify; managed by the developer
B) A Docker-managed storage area with a name you assign; Docker handles the actual storage location
C) A temporary in-memory filesystem that is destroyed when the container stops
D) An NFS share mounted from an external server into the container

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** Named volumes (e.g., `docker volume create mydata`) are managed entirely by Docker. Docker decides where to store the data on the host (typically under `/var/lib/docker/volumes/`). They persist across container restarts and removals.
- **Why A is wrong:** That describes a bind mount, where the host path is user-specified.
- **Why C is wrong:** That describes a `tmpfs` mount — an in-memory, ephemeral filesystem.
- **Why D is wrong:** Named volumes are local by default; NFS would require a volume plugin.
</details>

---

**10. Why is using the `latest` tag for production Docker images considered a bad practice?**

A) The `latest` tag is reserved by Docker Hub and cannot be used for private repositories
B) `latest` does not guarantee you get the most recent image; it is just a tag that can point to any version, causing unpredictable deployments
C) Docker automatically deletes images tagged `latest` after 30 days
D) The `latest` tag increases image pull time because Docker must check all available tags first

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** `latest` is just a conventional tag name — it is not automatically updated by Docker. A push can overwrite it with any image version. In production, using immutable version tags (e.g., `myapp:1.4.2`) ensures reproducible, auditable deployments.
- **Why A is wrong:** `latest` can be used freely in private and public repositories.
- **Why C is wrong:** Docker Hub does not automatically delete images tagged `latest`.
- **Why D is wrong:** Docker pulls a specific manifest; it does not scan all tags to resolve `latest`.
</details>

---

**11. What does the `docker exec -it container_name bash` command's `-it` flag combination do?**

A) `-i` runs in idle mode; `-t` runs in test mode for CI pipelines
B) `-i` keeps STDIN open (interactive); `-t` allocates a pseudo-TTY — together they give you an interactive terminal session inside the running container
C) `-i` sets the image name; `-t` sets the container tag
D) `-i` isolates the network namespace; `-t` enables tracing for debugging

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** `-i` (`--interactive`) keeps STDIN open so you can type commands. `-t` (`--tty`) allocates a pseudo-terminal, making the session behave like a real terminal with proper formatting. Together they enable an interactive shell session inside a running container.
- **Why A is wrong:** There is no "idle" or "test" mode in Docker's `-i` or `-t` flags.
- **Why C is wrong:** Image and tag are specified separately in `docker run`; `exec` does not use `-i`/`-t` for that purpose.
- **Why D is wrong:** Network isolation is handled at container creation, not via `exec` flags.
</details>

---

**12. In a multi-stage Dockerfile, what is copied from the builder stage to the final stage?**

A) All filesystem layers from every previous stage
B) Only what is explicitly copied using `COPY --from=<stage>` instructions
C) Environment variables and build arguments defined in the builder stage
D) The entire `/app` directory is automatically transferred between stages

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** Multi-stage builds let you select specific artifacts (compiled binaries, static files) to copy from earlier stages with `COPY --from=builder /app/dist /app`. Only those explicitly copied files end up in the final image — build tools, source code, and intermediate files are discarded.
- **Why A is wrong:** Each stage starts from its own base image; layers are not automatically inherited.
- **Why C is wrong:** `ENV` and `ARG` values do not carry over automatically; they must be re-declared if needed.
- **Why D is wrong:** No directory is automatically transferred; every file must be explicitly copied.
</details>

---

**13. What is the purpose of the `HEALTHCHECK` instruction in a Dockerfile?**

A) It validates the Dockerfile syntax before building the image
B) It defines a command Docker runs periodically inside the container to determine if the application is functioning correctly
C) It checks that all `COPY` source files exist on the build host before proceeding
D) It monitors container CPU and memory usage and restarts the container if thresholds are exceeded

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** `HEALTHCHECK` tells Docker how to test whether the container's application is still working. Docker runs the command at specified intervals and marks the container as `healthy`, `unhealthy`, or `starting` based on the exit code (0 = healthy, 1 = unhealthy).
- **Why A is wrong:** Dockerfile syntax validation happens during `docker build`, not via `HEALTHCHECK`.
- **Why C is wrong:** `HEALTHCHECK` runs inside a live container, not during build-time file validation.
- **Why D is wrong:** Resource thresholds and automatic restarts are configured via Docker run flags or orchestrators, not `HEALTHCHECK`.
</details>

---

**14. Which Docker Compose network configuration correctly defines a bridge network with a custom subnet?**

A) `networks: mynet: driver: overlay subnet: 172.20.0.0/16`
B) `networks: mynet: driver: bridge ipam: config: - subnet: 172.20.0.0/16`
C) `networks: mynet: ip_range: 172.20.0.0/16 type: internal`
D) `networks: mynet: address: 172.20.0.0 mask: /16 mode: bridge`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** In Docker Compose, custom subnet configuration goes under the `ipam` (IP Address Management) block with a `config` list containing `subnet`. The `bridge` driver is the correct local driver for custom subnets.
- **Why A is wrong:** `subnet` is not a direct key under the network driver config; it must be under `ipam.config`.
- **Why C is wrong:** `ip_range` and `type: internal` are not valid Compose network keys in this form.
- **Why D is wrong:** `address`, `mask`, and `mode` are not valid Docker Compose network configuration keys.
</details>

---

## Part 2: True/False

**15. True or False: In a multi-stage Docker build, ALL intermediate stages are included in the final pushed image, increasing its size.**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False
**Explanation:** Only the final stage (or a stage explicitly targeted with `--target`) is included in the pushed image. Intermediate stages are used only during the build process and are discarded — this is the whole point of multi-stage builds: keeping the final image lean.
</details>

---

**16. True or False: A `.dockerignore` file works like a `.gitignore` file — it uses the same pattern syntax and tells Docker which files to exclude from the build context sent to the daemon.**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True
**Explanation:** `.dockerignore` uses the same glob pattern syntax as `.gitignore`. Patterns listed in `.dockerignore` are excluded from the build context that the Docker Client sends to the daemon, reducing build time and preventing sensitive files (e.g., `.env`, `node_modules`) from being accidentally included in images.
</details>

---

**17. True or False: `docker run --restart=always` means the container will automatically restart even after the Docker daemon itself is restarted (e.g., after a host reboot).**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True
**Explanation:** The `--restart=always` policy persists across daemon restarts. When the Docker daemon starts (e.g., after a system reboot), it restarts all containers that have this policy. Use `--restart=unless-stopped` if you want containers that were manually stopped to remain stopped after a daemon restart.
</details>

---

**18. True or False: Docker volumes declared under the `volumes:` key in a `docker-compose.yml` file are automatically deleted when you run `docker-compose down`.**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False
**Explanation:** By default, `docker-compose down` stops and removes containers and networks but preserves named volumes to protect persistent data. To also remove volumes, you must explicitly add the `-v` flag: `docker-compose down -v`.
</details>

---

**19. True or False: The `ENV` instruction in a Dockerfile sets environment variables that are available both during the image build process AND at container runtime.**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True
**Explanation:** `ENV` variables persist into the running container's environment. This contrasts with `ARG`, which is only available during the build phase and is not present in the final container environment. Use `ARG` for build-time secrets and `ENV` for runtime configuration.
</details>

---

## Part 3: Fill in the Blank

**20. To run a container in the background (detached mode) so that it does not block your terminal, use `docker run _____ nginx`.**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `-d`
**Explanation:** The `-d` (or `--detach`) flag runs the container in the background and prints the container ID. Without it, the container's output streams to your terminal and `Ctrl+C` stops the container.
</details>

---

**21. The AWS CLI command used to authenticate Docker with Amazon ECR so you can push and pull private images is:**
`aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin _____.dkr.ecr.<region>.amazonaws.com`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `<your-aws-account-id>` (your 12-digit AWS Account ID)
**Explanation:** The ECR registry URL format is `<account-id>.dkr.ecr.<region>.amazonaws.com`. The `get-login-password` command generates a short-lived token that is piped directly into `docker login`, avoiding the need to store credentials in shell history.
</details>

---

**22. In a Dockerfile, to set the working directory for all subsequent `RUN`, `CMD`, `ENTRYPOINT`, `COPY`, and `ADD` instructions, you use the _____ instruction.**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `WORKDIR`
**Explanation:** `WORKDIR /app` sets the default directory for subsequent instructions and for the container's shell when you `exec` into it. If the directory does not exist, Docker creates it automatically. Using `WORKDIR` is preferred over `RUN cd /app` because it is cleaner, explicit, and creates an audit trail in the image metadata.
</details>

---

## Part 4: Code Prediction

**23. Study this Dockerfile snippet and predict what command actually executes when the container starts:**

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY app.py .
ENTRYPOINT ["python", "app.py"]
CMD ["--port", "8080"]
```

You run: `docker run myapp --port 9090`

**What command runs inside the container?**

A) `python app.py --port 8080 --port 9090`
B) `python app.py --port 9090`
C) `python app.py`
D) `--port 9090`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B — `python app.py --port 9090`
**Explanation:** `ENTRYPOINT` in exec form is fixed (`python app.py`). `CMD` in exec form provides default arguments (`--port 8080`), but any arguments supplied at `docker run` time **completely replace** `CMD`. So `--port 9090` replaces `--port 8080`, giving the final command `python app.py --port 9090`.
- **Why A is wrong:** Docker does not append runtime args to `CMD`; it replaces `CMD` entirely.
- **Why C is wrong:** Some argument is always passed; `CMD` is replaced, not dropped entirely.
- **Why D is wrong:** `ENTRYPOINT` is always prepended; the runtime arg alone does not run independently.
</details>

---

**24. Examine this `docker run` command and predict the container's behavior after the Docker daemon is restarted:**

```bash
docker run \
  --name webserver \
  --restart=always \
  -d \
  -p 443:443 \
  -v /etc/ssl/certs:/certs:ro \
  nginx:1.25
```

The host runs `sudo systemctl restart docker`. What happens to the `webserver` container?

A) The container is deleted because the daemon restart clears all state
B) The container stays stopped until manually started with `docker start webserver`
C) The container automatically restarts; the `/etc/ssl/certs` host directory is mounted read-only at `/certs` inside the container
D) The container restarts but the volume mount is lost because mounts reset on daemon restart

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C
**Explanation:** `--restart=always` causes Docker to restart the container whenever the daemon starts, including after host reboots. The bind mount (`-v /etc/ssl/certs:/certs:ro`) is re-established as configured — the `:ro` flag makes it read-only inside the container. Container configuration (including mounts) persists in the daemon's state database.
- **Why A is wrong:** The daemon persists container configuration; a restart does not delete containers.
- **Why B is wrong:** `--restart=always` causes automatic restart even after a manual stop; use `--restart=unless-stopped` to preserve a manual stop across daemon restarts.
- **Why D is wrong:** Volume and bind mount configurations are stored persistently and re-attached on container restart.
</details>

---

**25. Analyze this multi-stage Dockerfile and predict what is included in the final pushed image:**

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY src/ ./src/
RUN npm run build

# Stage 2: Production
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

After `docker build -t myapp:prod .` and `docker push myapp:prod`, what is included in the pushed image?

A) Both stages: node:20-alpine layers + nginx:alpine layers + all build artifacts
B) Only the nginx:alpine base layers + the compiled `/app/dist` files copied from the builder — no Node.js, npm, or source code
C) The nginx:alpine layers + all node_modules and source files from the builder stage
D) Only the `/app/dist` files with no base OS or nginx layers

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B
**Explanation:** Multi-stage builds produce a final image that only includes the last stage. The pushed image contains nginx:alpine layers plus the compiled `dist/` files copied from the builder. Node.js, npm, `node_modules`, `package.json`, and all source files from the builder stage are completely excluded — this is the primary benefit of multi-stage builds for production images.
- **Why A is wrong:** Intermediate stages are discarded; their layers are not in the pushed image.
- **Why C is wrong:** `node_modules` and source files exist only in the builder stage and are never copied to the final stage.
- **Why D is wrong:** The base image layers (nginx:alpine) are always included; Docker images always have a base.
</details>
