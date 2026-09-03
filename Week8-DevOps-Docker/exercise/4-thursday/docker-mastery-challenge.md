# Solo Challenge: Production-Grade Dockerfile

**Duration:** ~2 hours
**Mode:** Individual
**Prerequisites:** Docker Desktop, a Spring Boot project with Maven, Docker Hub account

## Learning Objectives

By the end of this challenge, you will be able to:

- Implement a multi-stage Docker build that separates compilation from the runtime image
- Apply all production Dockerfile best practices: pinned tags, non-root user, `.dockerignore`, and exec-form entrypoints
- Use `HEALTHCHECK` so the Docker daemon can monitor your container's application-level readiness
- Measure and reduce image size using an Alpine-based JRE runtime stage
- Push a versioned image to Docker Hub and verify it is publicly pullable

---

## The Challenge

Write a Dockerfile for the Spring Boot application that meets Revature's production standards.
No shortcuts — every best practice must be applied and verifiable.

---

## Requirements Checklist (implement ALL 10)

- [ ] Multi-stage build — a `builder` stage and a separate `runtime` stage
- [ ] Pinned base image tags — no `latest` anywhere in the file
- [ ] Non-root user — create `appuser` with UID 1001 and run the app as that user
- [ ] `.dockerignore` — excludes `target/`, `.git/`, `*.log`, `.idea/`, `*.iml`
- [ ] `HEALTHCHECK` instruction — uses `wget` to check `/actuator/health`
- [ ] `COPY --chown` — files are owned by `appuser`, not root
- [ ] `ENTRYPOINT` in exec form — `["java", ...]`, NOT `java ...` shell form
- [ ] `ENV JAVA_OPTS` — set JVM flags for container-aware memory limits
- [ ] Image size under 300MB — use an Alpine-based JRE in the runtime stage
- [ ] No secrets — no passwords, tokens, or credentials in any `ENV` or `ARG`

---

## Step 1 — Write the `.dockerignore`

Create a `.dockerignore` file in the project root. Each exclusion keeps build context small
and prevents secrets or build artifacts from leaking into the image.

```
# Compiled output — the builder stage recompiles from source
target/

# Version control metadata — never needed inside an image
.git/
.gitignore

# Log files — generated at runtime, not part of the image
*.log

# IDE project files — not needed to run the application
.idea/
*.iml
*.class

# Local environment files — may contain secrets
.env
*.env
```

**Why this matters:** Docker sends the entire build context to the daemon before reading the
Dockerfile. Without `.dockerignore`, a large `target/` directory adds seconds to every build
and inflates the image if accidentally COPYed.

---

## Step 2 — Write the Multi-Stage Dockerfile

Implement each instruction listed below. Use this as your checklist — tick each off as you write it.

**Builder stage checklist:**
- [ ] `FROM eclipse-temurin:17-jdk-alpine AS builder` — pinned JDK tag, named stage
- [ ] `WORKDIR /build` — isolates build files from the root filesystem
- [ ] `COPY pom.xml .` — copy the POM first to cache dependency downloads
- [ ] `RUN mvn dependency:go-offline -B` — pre-download dependencies (cache layer)
- [ ] `COPY src ./src` — copy source only after deps are cached
- [ ] `RUN mvn package -DskipTests -B` — compile and package the JAR

**Runtime stage checklist:**
- [ ] `FROM eclipse-temurin:17-jre-alpine AS runtime` — JRE only, no compiler tools
- [ ] `RUN addgroup -S appgroup && adduser -S appuser -G appgroup -u 1001` — non-root user
- [ ] `WORKDIR /app` — working directory for the application
- [ ] `COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar` — copy only the JAR, correct ownership
- [ ] `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"` — JVM container awareness
- [ ] `EXPOSE 8080` — documents the port (does not publish it)
- [ ] `HEALTHCHECK` with `wget`, 30s interval, 10s timeout, 5 retries, 60s start_period
- [ ] `USER appuser` — switch to non-root before ENTRYPOINT
- [ ] `ENTRYPOINT ["java", "-jar", "/app/app.jar"]` — exec form, no shell wrapper

---

## Step 3 — Build and Verify

Run each command and confirm the expected output before moving to the next step.

```bash
# Build the image with a specific version tag
docker build -t myapp:v1.0.0 .

# Check image size — must be under 300MB
docker images myapp
# Look at the SIZE column in the output

# Run the container
docker run -d --name myapp-test -p 8080:8080 myapp:v1.0.0

# Wait for the health check start_period (60 seconds), then inspect health status
sleep 65
docker inspect --format='{{.State.Health.Status}}' myapp-test
# Expected output: healthy

# Verify the process runs as a non-root user
docker exec myapp-test whoami
# Expected output: appuser

# Verify exec-form entrypoint (no shell process wrapping Java)
docker exec myapp-test ps aux
# Java should be PID 1, not wrapped in /bin/sh

# Test the application directly
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Clean up
docker stop myapp-test && docker rm myapp-test
```

---

## Step 4 — Security Scan

```bash
# Option A: Docker Scout (if available in your Docker Desktop)
docker scout cves myapp:v1.0.0

# Option B: Inspect the image layers for unexpected content
docker image inspect myapp:v1.0.0 | python3 -m json.tool | grep -A5 "Env"
# Confirm no passwords or tokens appear in the Env array

# Option C: Check what files are in the image
docker run --rm myapp:v1.0.0 find /app -type f
# Should contain only app.jar — no source code, no .git, no pom.xml
```

Document your findings in `starter_code/dockerfile-decisions.md` under the security section.
If Scout finds CVEs, note their severity and whether a newer base image tag resolves them.

---

## Step 5 — Push to Docker Hub

```bash
# Log in to Docker Hub
docker login

# Tag with your Docker Hub username and a semantic version
docker tag myapp:v1.0.0 <your-dockerhub-username>/week8-springboot:v1.0.0

# Push to your personal repository
docker push <your-dockerhub-username>/week8-springboot:v1.0.0

# Verify the push by pulling it back (proves it is publicly accessible)
docker pull <your-dockerhub-username>/week8-springboot:v1.0.0
```

Open Docker Hub in a browser and confirm the repository and tag appear in your dashboard.
Copy the repository URL (format: `https://hub.docker.com/r/<username>/week8-springboot`)
and save it — this is what you submit.

---

## Step 6 — Document Your Decisions

Open `starter_code/dockerfile-decisions.md` and complete every row in the table.
Write in your own words — explain the *why*, not just the *what*.

---

## Definition of Done (Solo)

- [ ] All 10 requirements checklist items implemented and verified
- [ ] `docker images myapp` shows SIZE under 300MB
- [ ] `docker exec <container> whoami` outputs `appuser`
- [ ] `docker inspect` shows health status `healthy` within 90 seconds of startup
- [ ] Image tagged and pushed to personal Docker Hub repository
- [ ] `starter_code/dockerfile-decisions.md` completed with your reasoning
- [ ] Docker Hub repository URL recorded and ready to submit

---

## Reflection Questions

Answer these in your `dockerfile-decisions.md` under the "Reflection" section:

1. You used `eclipse-temurin:17-jre-alpine` in the runtime stage. What is the tradeoff
   between Alpine-based images (smaller, fewer tools) and Debian-based images
   (larger, better debuggability)? When would you choose each?

2. The `COPY pom.xml` + `RUN mvn dependency:go-offline` pattern creates a cache layer
   for dependencies. Under what circumstances does this cache layer get invalidated,
   and what is the performance cost when that happens?

3. You set `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"`.
   What problem does `+UseContainerSupport` solve, and what happens to a JVM
   running in a container without it?

---

*Week 8 — Thursday | Docker Mastery Day | Solo Challenge*
