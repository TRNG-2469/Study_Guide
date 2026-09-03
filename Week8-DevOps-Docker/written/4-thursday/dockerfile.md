# Dockerfile

## Learning Objectives
By the end of this lesson, you will be able to:
- Write a valid Dockerfile from scratch
- Explain the purpose of every common Dockerfile instruction
- Use layer caching strategically to speed up builds
- Create multi-stage builds to produce lean production images
- Write a proper `.dockerignore` file

---

## Why This Matters

A Dockerfile is the blueprint for your application's container image. Every time you deploy your app — to staging, production, a colleague's machine, or a CI/CD pipeline — Docker uses this file to reproduce an identical environment. Mastering Dockerfiles means your app will "just work" everywhere, eliminating the classic "works on my machine" problem forever.

---

## What Is a Dockerfile?

A Dockerfile is a plain text file (no extension) containing a series of instructions. Docker reads these instructions top-to-bottom and executes each one to build an image layer by layer.

Think of a Dockerfile like a recipe:
- The base image (`FROM`) is your starting ingredients
- Each `RUN`, `COPY`, and `ADD` instruction is a preparation step
- The final image is the finished dish, ready to serve

---

## Basic Dockerfile Structure

```dockerfile
# Start from an official base image
FROM node:20-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy dependency manifest first (for layer caching)
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy the rest of the application source code
COPY . .

# Document which port the app listens on
EXPOSE 3000

# Define the command to start the application
CMD ["node", "server.js"]
```

---

## Core Instructions Explained

### `FROM` — Choose Your Base Image

Every Dockerfile must start with `FROM`. It sets the base image all subsequent instructions build upon.

```dockerfile
# Use an official image from Docker Hub
FROM ubuntu:22.04

# Use a language-specific slim variant (smaller size)
FROM python:3.11-slim

# Use Alpine Linux for the smallest possible base (~5MB)
FROM node:20-alpine

# Start from absolute scratch (for fully static binaries)
FROM scratch
```

**Best practice**: Always pin a specific tag (e.g., `node:20-alpine`) rather than `node:latest`. The `latest` tag changes without warning and can break your builds.

---

### `WORKDIR` — Set the Working Directory

```dockerfile
WORKDIR /app
```

Sets the working directory for subsequent `RUN`, `COPY`, `ADD`, `CMD`, and `ENTRYPOINT` instructions. If the directory does not exist, Docker creates it automatically.

**Why use it?** Without `WORKDIR`, files end up scattered in `/` (the root), making the container hard to debug.

---

### `COPY` — Copy Files Into the Image

```dockerfile
# Copy a single file
COPY server.js /app/server.js

# Copy everything in the current directory to /app
COPY . /app

# Copy using WORKDIR as the destination (. means WORKDIR)
COPY package*.json ./
```

`COPY` is the preferred way to get files from your build context (your local directory) into the image.

---

### `ADD` — Like COPY, With Superpowers

```dockerfile
# ADD can extract tar archives automatically
ADD source.tar.gz /app/

# ADD can fetch files from URLs (not recommended — use curl/wget instead)
ADD https://example.com/file.tar.gz /tmp/
```

**Rule of thumb**: Use `COPY` unless you specifically need `ADD`'s tar-extraction feature. `COPY` is more explicit and predictable.

---

### `RUN` — Execute Commands During Build

```dockerfile
# Shell form (runs via /bin/sh -c)
RUN apt-get update && apt-get install -y curl

# Exec form (no shell, more explicit)
RUN ["apt-get", "install", "-y", "curl"]

# Chain commands with && to minimize layers
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       curl \
       git \
       vim \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
```

Each `RUN` instruction creates a new image layer. Chain related commands with `&&` and clean up in the same layer to keep image size small.

---

### `ENV` — Set Environment Variables

```dockerfile
# Set a single variable
ENV NODE_ENV=production

# Set multiple variables (modern syntax)
ENV PORT=3000 \
    DB_HOST=localhost \
    LOG_LEVEL=info
```

Environment variables set with `ENV` persist into running containers. They can be overridden at runtime with `docker run -e PORT=8080`.

---

### `ARG` — Build-Time Arguments

```dockerfile
# Declare a build argument with an optional default
ARG APP_VERSION=1.0.0
ARG BUILD_DATE

# Use it in subsequent instructions
LABEL version=$APP_VERSION
RUN echo "Building version $APP_VERSION"
```

`ARG` variables only exist during the build — they are NOT available in running containers. Pass them at build time:

```bash
docker build --build-arg APP_VERSION=2.1.0 .
```

---

### `EXPOSE` — Document the Port

```dockerfile
EXPOSE 8080
EXPOSE 8080/udp
```

`EXPOSE` does **not** actually publish the port. It is documentation — telling users of the image which port the application listens on. Actual port publishing happens with `docker run -p 8080:8080`.

---

### `CMD` — Default Container Command

```dockerfile
# Exec form (preferred)
CMD ["node", "server.js"]

# Shell form
CMD node server.js
```

`CMD` defines the **default command** that runs when a container starts. It can be overridden on the command line:

```bash
docker run myapp node --version   # overrides CMD
```

---

### `ENTRYPOINT` — Fixed Container Command

```dockerfile
ENTRYPOINT ["python", "app.py"]
```

`ENTRYPOINT` defines a command that **cannot be overridden** (without `--entrypoint`). Any arguments passed to `docker run` are appended to the ENTRYPOINT command.

**CMD + ENTRYPOINT Together**:
```dockerfile
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
# Result: nginx -g "daemon off;"
# Override just the args: docker run myimage -c /custom/nginx.conf
```

---

## Layer Caching Strategy

Docker caches the result of each instruction. If a layer's instruction and all previous layers are unchanged, Docker reuses the cached layer instead of re-running it. This dramatically speeds up subsequent builds.

**Slow build (cache-unfriendly):**
```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY . .                    # copies everything — any file change busts cache
RUN npm install             # reinstalls all packages every time any file changes
```

**Fast build (cache-friendly):**
```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./       # only changes when package.json changes
RUN npm install             # cached unless package.json changed
COPY . .                    # now copy source — cache bust here only
```

**Key rule**: Put instructions that change frequently (source code) **after** instructions that change rarely (installing dependencies).

---

## Multi-Stage Builds

Multi-stage builds let you use one image to compile/build your application and a different, smaller image for the final artifact. This is the gold standard for production images.

### Example: Spring Boot Application

```dockerfile
# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy Maven/Gradle wrapper and dependencies first
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q

# Copy source and build the JAR
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre AS runtime

# Create a non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Result**: The final image contains only the JRE and the JAR file — not Maven, source code, or build tools. Typically 200-300MB instead of 600MB+.

---

## `.dockerignore` File

Just like `.gitignore` tells Git what to ignore, `.dockerignore` tells Docker what to exclude from the build context:

```
# .dockerignore

# Version control
.git
.gitignore

# Dependencies (they get installed inside the container)
node_modules
vendor/

# Build outputs
target/
dist/
build/

# IDE files
.idea/
.vscode/
*.iml

# Logs and temp files
*.log
*.tmp
.DS_Store

# Test files (not needed in production image)
**/*test*
**/*spec*
```

**Why it matters**: Without `.dockerignore`, Docker sends your entire project directory (including `node_modules` with potentially millions of files) to the daemon on every build. This can make builds take minutes instead of seconds.

---

## Complete Example: Python Flask App

```dockerfile
# syntax=docker/dockerfile:1

# Stage 1: Builder
FROM python:3.11-slim AS builder

WORKDIR /build

# Install build dependencies
RUN pip install --upgrade pip

# Copy and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --target=/build/deps -r requirements.txt

# Stage 2: Production
FROM python:3.11-slim AS production

# Security: create non-root user
RUN useradd --create-home --shell /bin/bash appuser

WORKDIR /app

# Copy installed packages from builder
COPY --from=builder /build/deps /usr/local/lib/python3.11/site-packages/

# Copy application source
COPY --chown=appuser:appuser src/ ./src/

# Set environment variables
ENV FLASK_APP=src/app.py \
    FLASK_ENV=production \
    PORT=5000

# Switch to non-root user
USER appuser

EXPOSE 5000

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:5000/health')"

CMD ["python", "-m", "flask", "run", "--host=0.0.0.0", "--port=5000"]
```

---

## Summary

| Instruction | Purpose | Changes Frequently? |
|-------------|---------|-------------------|
| `FROM` | Base image | Rarely |
| `WORKDIR` | Working directory | Rarely |
| `COPY package*.json` | Dependency manifest | Sometimes |
| `RUN npm install` | Install dependencies | When manifest changes |
| `COPY . .` | Application source | Often |
| `ENV` | Runtime environment variables | Rarely |
| `EXPOSE` | Port documentation | Rarely |
| `CMD` | Default start command | Rarely |
| `ENTRYPOINT` | Fixed executable | Rarely |

Order your instructions from least-frequently-changed to most-frequently-changed to maximize Docker layer cache hits.

---

## External Resources

- [Dockerfile Reference (Official)](https://docs.docker.com/engine/reference/builder/)
- [Docker Build Best Practices](https://docs.docker.com/build/building/best-practices/)
- [Multi-stage Builds (Official Guide)](https://docs.docker.com/build/building/multi-stage/)
