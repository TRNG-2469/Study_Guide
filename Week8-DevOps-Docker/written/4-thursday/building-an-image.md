# Building a Docker Image

## Learning Objectives
By the end of this lesson, you will be able to:
- Use `docker build` to build an image from a Dockerfile
- Understand build context and why it matters
- Use tags, the `-f` flag, and `--no-cache` effectively
- Read Docker build output layer by layer
- Create an optimized multi-stage build for a real application
- Apply cache ordering techniques to speed up iterative builds

---

## Why This Matters

`docker build` is the command that transforms your Dockerfile into a deployable image. Understanding how the build process works — especially layer caching — is the single biggest factor in how fast your CI/CD pipeline runs. A well-structured Dockerfile can build in 10 seconds on a cache hit; a poorly structured one rebuilds from scratch every time and takes 5 minutes.

---

## The `docker build` Command

```bash
docker build [OPTIONS] PATH
```

`PATH` is the **build context** — the directory Docker sends to the daemon. Usually `.` (current directory).

---

## Build Context

When you run `docker build .`, Docker packages everything in `.` (the current directory) and sends it to the Docker daemon as a **tar archive** called the build context.

```bash
# Build using the current directory as context
docker build .

# The daemon can only access files inside the build context
# COPY ../outside-file.txt /app/  <-- This FAILS (outside the context)
```

**Why size matters**: If your project has a `node_modules` folder with 200,000 files, Docker sends all of them to the daemon on every build. This can take 30+ seconds even before a single build step runs.

**Solution**: Use `.dockerignore` to exclude unnecessary files:
```
node_modules
.git
dist
*.log
```

---

## Tagging with `-t`

```bash
# -t: Tag the resulting image with a name:tag
docker build -t myapp .

# Full tag with version
docker build -t myapp:v1.2.0 .

# Tag with Docker Hub username for pushing
docker build -t myusername/myapp:v1.2.0 .

# Apply multiple tags in a single build
docker build -t myapp:v1.2.0 -t myapp:latest .

# Tag with registry URL for a private registry
docker build -t myregistry.company.com/team/myapp:v1.2.0 .
```

---

## Specifying a Dockerfile with `-f`

By default Docker looks for a file named `Dockerfile` in the build context. Use `-f` to specify a different file:

```bash
# Use a Dockerfile in a subdirectory
docker build -f docker/Dockerfile.prod -t myapp:prod .

# Use a Dockerfile for a specific environment
docker build -f Dockerfile.dev -t myapp:dev .

# Useful for projects with multiple Dockerfiles
# Dockerfile            (development)
# Dockerfile.prod       (production multi-stage)
# Dockerfile.test       (test runner)
```

---

## Cache Control

```bash
# --no-cache: Force a complete rebuild, ignoring all cached layers
docker build --no-cache -t myapp:fresh .

# Use --no-cache when:
# - A RUN command fetches external data (e.g., apt-get) that may have updated
# - You suspect a stale cache is hiding a bug
# - Building a release image that must be reproducible

# --pull: Always pull the latest version of the base image
docker build --pull -t myapp .

# Combine for a completely fresh build
docker build --no-cache --pull -t myapp:release .
```

---

## Build Arguments

```bash
# Pass a build argument defined with ARG in the Dockerfile
docker build --build-arg NODE_ENV=production -t myapp .

# Multiple build arguments
docker build \
  --build-arg APP_VERSION=2.1.0 \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  -t myapp:v2.1.0 .
```

---

## Reading Docker Build Output

```bash
docker build -t myapp .
```

Each line of output corresponds to a Dockerfile instruction:

```
[+] Building 23.4s (10/10) FINISHED
 => [internal] load build definition from Dockerfile              0.0s
 => [internal] load .dockerignore                                 0.0s
 => [internal] load metadata for docker.io/library/node:20-alpine 1.2s
 => [1/6] FROM docker.io/library/node:20-alpine@sha256:abc...    0.0s
 => CACHED [2/6] WORKDIR /app                                     0.0s
 => CACHED [3/6] COPY package*.json ./                            0.0s
 => CACHED [4/6] RUN npm ci --only=production                     0.0s
 => [5/6] COPY . .                                               0.3s
 => [6/6] RUN npm run build                                       8.7s
 => exporting to image                                            0.4s
 => => exporting layers                                           0.3s
 => => writing image sha256:def456...                             0.0s
 => => naming to docker.io/library/myapp:latest                   0.0s
```

- `CACHED` = layer reused from cache (fast!)
- A time like `8.7s` = layer was rebuilt
- If you see many `CACHED` lines, your cache ordering is working correctly

---

## Understanding Layer Caching in Practice

```dockerfile
# BAD: Cache busts at line 3 whenever ANY file changes
FROM node:20-alpine
WORKDIR /app
COPY . .                    # copies everything — busts cache on any change
RUN npm ci                  # re-runs every time ANY file changes (even a .md file)
CMD ["node", "server.js"]
```

```dockerfile
# GOOD: Cache busts at line 5 only when package.json changes
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./       # changes rarely
RUN npm ci                  # cached unless package.json changes
COPY . .                    # busts cache when source changes — but npm ci already ran
CMD ["node", "server.js"]
```

Build time comparison:
- BAD (after any file change): 45 seconds (full npm install every time)
- GOOD (after only source change): 3 seconds (npm install cached)

---

## Multi-Stage Build: Spring Boot + Angular

Here is a real-world multi-stage build combining a Java backend and an Angular frontend:

```dockerfile
# syntax=docker/dockerfile:1

# ===== Stage 1: Build Angular Frontend =====
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build --configuration=production
# Output: dist/myapp/browser/


# ===== Stage 2: Build Spring Boot Backend =====
FROM eclipse-temurin:21-jdk AS backend-builder

WORKDIR /backend

# Cache Maven dependencies separately
COPY backend/pom.xml .
COPY backend/mvnw .
COPY backend/.mvn .mvn/
RUN ./mvnw dependency:go-offline -q

# Build the application
COPY backend/src ./src/
RUN ./mvnw package -DskipTests -q
# Output: target/myapp-1.0.0.jar


# ===== Stage 3: Production Runtime =====
FROM eclipse-temurin:21-jre AS production

# Security: run as non-root
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app appuser
WORKDIR /app

# Copy the built JAR from Stage 2
COPY --from=backend-builder /backend/target/*.jar app.jar

# Copy the Angular build output into the Spring Boot static resources directory
COPY --from=frontend-builder /frontend/dist/myapp/browser/ ./static/

# Ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

Build it:
```bash
docker build -t myorg/fullstack-app:v1.0.0 .
```

The final image contains:
- Java JRE (not JDK — no compiler)
- The JAR file
- Compiled Angular HTML/CSS/JS files
- No Node.js, no Maven, no source code

**Final image size**: ~300MB instead of 1.5GB+ if built naively.

---

## Targeting a Specific Stage

```bash
# Build only through the backend-builder stage (useful for testing the build step)
docker build --target backend-builder -t myapp:build-check .

# Build only the frontend
docker build --target frontend-builder -t myapp:frontend-only .
```

---

## Viewing the Built Image

```bash
# Confirm the image was created
docker images myapp

# View how layers were constructed
docker image history myapp:v1.0.0

# Full metadata
docker image inspect myapp:v1.0.0
```

---

## Summary

| Flag | Purpose |
|------|---------|
| `.` | Build context (current directory) |
| `-t name:tag` | Tag the resulting image |
| `-f Dockerfile.prod` | Use a specific Dockerfile |
| `--no-cache` | Ignore cached layers, full rebuild |
| `--pull` | Always pull latest base image |
| `--build-arg KEY=val` | Pass a build-time argument |
| `--target stage` | Build only up to a specific stage |

Order Dockerfile instructions from rarely-changing to frequently-changing to maximize cache hits. Use multi-stage builds to keep production images lean.

---

## External Resources

- [`docker build` Reference (Official)](https://docs.docker.com/engine/reference/commandline/build/)
- [Dockerfile Best Practices](https://docs.docker.com/build/building/best-practices/)
- [BuildKit Documentation](https://docs.docker.com/build/buildkit/)
