# Containerization — Concepts, Images, and Layers

## Learning Objectives

By the end of this lesson, you will be able to:

- Define containerization and explain the OS-level mechanisms that make it possible
- Explain what a container image is and how layers work
- Describe why Docker became the dominant container runtime
- Explain how containerization solves environment inconsistency problems
- Understand the relationship between images and running containers

---

## Why This Matters

Containerization is the foundational concept behind everything in the Docker portion of Week 8. Before you write your first Dockerfile, you need to understand what a container *is* at the conceptual level — why it works, what guarantees it provides, and why the industry adopted it. This lesson is the conceptual bridge between EC2 (infrastructure) and Docker (application packaging).

---

## The Problem Containerization Solves

Software has a well-known deployment problem, often expressed as:

> **"It works on my machine."**

A developer builds an application on their laptop with:
- Ubuntu 22.04
- Java 17.0.9
- Specific environment variables
- A particular file system layout

The application is then deployed to a server with:
- Amazon Linux 2023
- Java 21.0.1
- Different environment variables
- A different file system layout

The application behaves differently — or fails entirely. Tracking down these discrepancies wastes days.

Containerization solves this by packaging the **application together with everything it needs to run**: runtime, libraries, configuration, and file system. The result is a portable, self-contained unit that runs identically in every environment.

---

## What Is Containerization?

**Containerization** is a method of OS-level virtualization that packages an application and its dependencies into a standardized unit called a **container**. Unlike a VM, a container does not include a full guest OS — it shares the host machine's Linux kernel while maintaining complete isolation of its file system, processes, users, and network.

### The Two Kernel Features Behind Containers

#### 1. Namespaces — Isolation

Linux namespaces give each container its own isolated view of system resources:

| Namespace | Isolates |
|---|---|
| `pid` | Process IDs — container processes cannot see host processes |
| `net` | Network interfaces, routing tables, ports |
| `mnt` | File system mount points |
| `uts` | Hostname and domain name |
| `ipc` | Inter-process communication (shared memory, semaphores) |
| `user` | User and group IDs |

From inside a container, the process sees only its own processes, its own network interfaces, and its own file system — as if it were the only tenant on the machine.

#### 2. cgroups (Control Groups) — Resource Limits

cgroups allow the kernel to limit, account for, and isolate resource usage:

```bash
# Docker uses cgroups to enforce limits like:
docker run --memory 512m --cpus 1.0 my-image
# This container cannot use more than 512 MB RAM or 1 CPU core
```

Without cgroups, a single runaway container could consume all available memory and crash every other container on the host.

---

## Docker — The Container Runtime

**Docker** is the software platform that builds, ships, and runs containers. While the underlying kernel mechanisms (namespaces, cgroups) existed for years, Docker (released in 2013) made them accessible to developers through:

- A simple `Dockerfile` format for building images
- A `docker` CLI with human-friendly commands
- A public registry (Docker Hub) for sharing images
- A portable runtime that works on Linux, macOS (via a Linux VM), and Windows

### The Docker Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Docker Client                          │
│   docker build / docker run / docker push / docker pull  │
└────────────────────────┬─────────────────────────────────┘
                         │  REST API
                         ▼
┌──────────────────────────────────────────────────────────┐
│                   Docker Daemon (dockerd)                 │
│   Manages images, containers, networks, and volumes       │
└────────┬──────────────────────────┬──────────────────────┘
         │                          │
         ▼                          ▼
┌────────────────┐        ┌──────────────────────┐
│  Docker Images │        │  Running Containers   │
│  (local cache) │        │  (isolated processes) │
└────────────────┘        └──────────────────────┘
         ▲
         │  pull/push
         ▼
┌────────────────┐
│  Container     │
│  Registry      │
│  (Docker Hub,  │
│   ECR, etc.)   │
└────────────────┘
```

### Key Terminology

| Term | Definition |
|---|---|
| **Dockerfile** | A text file with instructions for building a container image |
| **Image** | A static, immutable snapshot of a container's file system and configuration |
| **Container** | A running instance of an image (a live process with its own isolated environment) |
| **Registry** | A server that stores and distributes images (Docker Hub, AWS ECR) |
| **Repository** | A collection of related images, usually different versions of the same app |
| **Tag** | A label on an image, typically a version number (e.g., `myapp:1.2.3`, `nginx:latest`) |

### Image vs Container — The Critical Distinction

| | Image | Container |
|---|---|---|
| **State** | Static, immutable | Dynamic, running |
| **Analogy** | A class definition | An instance of the class |
| **Another analogy** | A recipe | The cooked meal |
| **Can you run it?** | No — must instantiate first | Yes — it is already running |
| **Stored where?** | Registry (remote) or local cache | Local host only |

You can create many containers from a single image — just as you can launch many EC2 instances from a single AMI.

---

## Container Images and Layers

A container image is not a monolithic file. It is composed of **ordered, immutable layers**. Each layer represents a set of file system changes (files added, modified, or deleted).

### How Layers Work

```
Layer 4: COPY app.jar /opt/app/        ← Your application JAR
Layer 3: RUN apt-get install curl      ← Installed curl
Layer 2: RUN apt-get update            ← Package list updated
Layer 1: FROM ubuntu:22.04             ← Base OS layer (official Ubuntu image)
```

Each layer is identified by a SHA256 hash. When Docker builds or pulls an image, it processes layers from bottom to top.

### Dockerfile That Produces These Layers

```dockerfile
# Layer 1: Start from the official Ubuntu 22.04 base image
FROM ubuntu:22.04

# Layer 2: Update the package list
RUN apt-get update

# Layer 3: Install curl
RUN apt-get install -y curl

# Layer 4: Copy your compiled JAR into the image
COPY target/app.jar /opt/app/app.jar

# Declare the command to run when the container starts
CMD ["java", "-jar", "/opt/app/app.jar"]
```

### Layer Caching — Why Layer Order Matters

Docker caches each layer. When you rebuild an image, Docker reuses cached layers for any instruction that has not changed — and only rebuilds from the first changed instruction downward.

```
Build 1 (no cache):
  Layer 1 (FROM ubuntu:22.04)    → BUILT (2 seconds)
  Layer 2 (apt-get update)       → BUILT (15 seconds)
  Layer 3 (apt-get install curl) → BUILT (10 seconds)
  Layer 4 (COPY app.jar)         → BUILT (0.1 seconds)
  Total: ~27 seconds

Build 2 (only app.jar changed):
  Layer 1 (FROM ubuntu:22.04)    → CACHED (instant)
  Layer 2 (apt-get update)       → CACHED (instant)
  Layer 3 (apt-get install curl) → CACHED (instant)
  Layer 4 (COPY app.jar)         → REBUILT (0.1 seconds)
  Total: ~0.5 seconds
```

This is why you should structure Dockerfiles with **infrequently changing instructions first** (dependencies, OS packages) and **frequently changing instructions last** (your application code).

### Layer Sharing Across Images

If two images share a base layer (e.g., both start with `FROM ubuntu:22.04`), Docker stores that layer only once on disk. This dramatically reduces storage and download time:

```
Image A: ubuntu:22.04 + Java + App-v1
Image B: ubuntu:22.04 + Java + App-v2

Disk storage:
  ubuntu:22.04 layer  → stored once (shared)
  Java layer          → stored once (shared)
  App-v1 layer        → stored once
  App-v2 layer        → stored once
  Total: 4 layers instead of 6
```

---

## The Container File System

When a container starts, Docker creates a **union file system** that merges all image layers into a single coherent view. The layers themselves are read-only. Docker adds one writable layer on top for any changes the container makes while running:

```
┌─────────────────────────────────┐
│  Writable Container Layer       │ ← Runtime changes (files written during execution)
├─────────────────────────────────┤
│  Image Layer 4: COPY app.jar    │ ← Read-only
├─────────────────────────────────┤
│  Image Layer 3: RUN apt install │ ← Read-only
├─────────────────────────────────┤
│  Image Layer 2: RUN apt update  │ ← Read-only
├─────────────────────────────────┤
│  Image Layer 1: ubuntu:22.04    │ ← Read-only
└─────────────────────────────────┘
```

**Key implication:** When a container is deleted, its writable layer is deleted too. Data written inside a running container is **ephemeral** by default. This is why containers are designed to be stateless, and persistent data is stored outside the container using **volumes** (covered later this week).

---

## Why Containerization Enables Consistent Deployments

The container image bundles:
- The OS user-space (library files, shared objects, system binaries)
- The runtime (JVM, Node.js, Python interpreter)
- The application code
- Environment variables and configuration
- The startup command

Everything except the Linux kernel is inside the image. The result:

```
Developer laptop ──────────────────────► CI/CD pipeline ──────────────────────► Production EC2
  docker run myapp:1.2.3                  docker run myapp:1.2.3                 docker run myapp:1.2.3
  Result: identical behavior              Result: identical behavior              Result: identical behavior
```

The environment no longer matters. The image *is* the environment.

---

## A Minimal Spring Boot Container Image

Here is a complete, commented Dockerfile for a Spring Boot application:

```dockerfile
# Start from the official Eclipse Temurin Java 17 JRE image
# Using a JRE (not JDK) for a smaller image — we only need to run, not compile
FROM eclipse-temurin:17-jre-jammy

# Set the working directory inside the container
WORKDIR /opt/app

# Copy the compiled JAR from the Maven/Gradle build output
# The JAR must be built locally first: mvn package -DskipTests
COPY target/myapp-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 to document that the app listens here
# (This is documentation only — you still need to publish the port with -p)
EXPOSE 8080

# The command to run when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
# Build the image and tag it
docker build -t myapp:1.0 .

# Run a container from the image
docker run -p 8080:8080 myapp:1.0

# Your Spring Boot app is now running at http://localhost:8080
```

---

## Summary

- Containerization packages an application with all its dependencies into a portable, isolated unit.
- Containers use Linux namespaces (isolation) and cgroups (resource limits) — not a separate OS kernel.
- Docker is the toolchain that makes containers accessible: Dockerfile → image → running container.
- Images are immutable and layered; containers are running instances with a writable layer on top.
- Layer caching makes rebuilds fast — put stable instructions early, changing instructions late.
- Containers are stateless by default; persistent data must be stored in volumes or external services.
- The container image guarantees identical behavior across every environment from dev to production.

---

## External Resources

- [Docker Official Getting Started Guide](https://docs.docker.com/get-started/)
- [Linux Containers: cgroups and Namespaces](https://www.linuxjournal.com/content/everything-you-need-know-about-linux-containers-part-i-linux-control-groups-and-process)
- [Dockerfile Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
