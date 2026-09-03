# Docker Architecture

## Learning Objectives
By the end of this lesson, you will be able to:
- Describe the three main components of Docker architecture
- Explain the client-server model Docker uses
- Trace the lifecycle of a `docker run` command from CLI to running container
- Understand how the Docker Registry fits into the overall system

---

## Why This Matters

When something goes wrong in Docker — a container fails to start, an image cannot be pulled, a build hangs — knowing the architecture tells you *where* to look. Developers who understand the client-server model can configure remote daemons, set up CI/CD pipelines, and debug networking issues far more effectively than those who treat Docker as a black box.

---

## The Big Picture

Docker uses a **client-server architecture**. Think of it like a restaurant:

| Restaurant Role | Docker Equivalent |
|----------------|-------------------|
| You (customer) | Docker Client (CLI) |
| Waiter taking orders | Docker API (REST) |
| Kitchen preparing food | Docker Daemon (dockerd) |
| Grocery store supplying ingredients | Docker Registry |

---

## Core Components

### 1. Docker Client (`docker`)

The Docker Client is the command-line tool you interact with every day. When you type:

```bash
docker run nginx
```

...the client does **not** run anything itself. It translates your command into an HTTP request and sends it to the Docker Daemon via a REST API.

- Communicates over a Unix socket: `/var/run/docker.sock` (local)
- Can also communicate over TCP for remote daemons
- Multiple clients can talk to one daemon
- `docker` CLI, Docker Desktop GUI, and third-party tools all act as clients

### 2. Docker Daemon (`dockerd`)

The Docker Daemon is a long-running background process that does all the real work:

- **Builds** images from Dockerfiles
- **Pulls** images from registries
- **Creates and runs** containers
- **Manages** volumes, networks, and plugins

The daemon listens for API requests (by default on a Unix socket at `/var/run/docker.sock`) and fulfills them. It can also be configured to listen on a TCP port for remote access.

```bash
# Check that the daemon is running
sudo systemctl status docker

# View daemon logs
sudo journalctl -u docker -f
```

### 3. Docker Registry

A registry is a **storage and distribution system for Docker images**. The default registry is **Docker Hub** (`hub.docker.com`).

- **Public images**: `nginx`, `postgres`, `node`, `python` — free to pull
- **Private repositories**: require authentication
- **Self-hosted options**: Harbor, AWS ECR, GitLab Container Registry

When you run `docker pull nginx`, the daemon contacts the registry and downloads the image layers.

---

## How `docker run nginx` Actually Works

Here is the step-by-step flow when you execute `docker run nginx`:

```
You (Terminal)
    |
    | 1. Type: docker run nginx
    v
Docker Client (CLI)
    |
    | 2. Translates to HTTP POST /containers/create
    |    Sends request to Unix socket /var/run/docker.sock
    v
Docker Daemon (dockerd)
    |
    | 3. Checks local image cache
    |    Image "nginx" not found locally
    |
    | 4. Contacts Docker Hub registry
    v
Docker Registry (hub.docker.com)
    |
    | 5. Returns image manifest + layer URLs
    v
Docker Daemon (dockerd)
    |
    | 6. Downloads and caches image layers
    | 7. Creates container filesystem (overlay2)
    | 8. Sets up networking (bridge network, IP address)
    | 9. Starts container process (nginx master process)
    |
    | 10. Returns container ID to client
    v
Docker Client (CLI)
    |
    | 11. Prints container ID or streams logs
    v
You (Terminal) — see output
```

---

## Architecture Diagram

```
+------------------+        REST API         +----------------------+
|   Docker Client  | ----------------------> |   Docker Daemon      |
|   (docker CLI)   |  /var/run/docker.sock   |   (dockerd)          |
+------------------+   or TCP :2376          |                      |
                                             |  - Image Management  |
                                             |  - Container Runtime |
                                             |  - Network Driver    |
                                             |  - Volume Driver     |
                                             +----------+-----------+
                                                        |
                                                        | Pull/Push
                                                        v
                                             +----------------------+
                                             |   Docker Registry    |
                                             |   (Docker Hub /      |
                                             |    Private Registry) |
                                             +----------------------+
```

---

## Under the Hood: containerd and runc

Modern Docker is layered:

```
dockerd (Docker Daemon)
  └── containerd  (container runtime manager)
        └── runc  (OCI-compliant low-level container runner)
```

- **`dockerd`**: High-level API, image management, Compose, networking
- **`containerd`**: Manages container lifecycle (pull, create, run, stop)
- **`runc`**: Actually calls Linux kernel namespaces and cgroups to isolate the process

You rarely interact with `containerd` or `runc` directly, but knowing they exist helps when reading error messages.

---

## Local vs. Remote Daemon

By default the client connects to the **local** daemon via Unix socket. You can point the client at a **remote** daemon:

```bash
# Connect to a remote Docker daemon over TCP
export DOCKER_HOST=tcp://192.168.1.100:2376

# Or per-command
docker -H tcp://192.168.1.100:2376 ps
```

> **Security Note**: Exposing the daemon over TCP without TLS gives full root access to the host. Always use TLS certificates for remote access in production.

---

## Summary

| Component | Role | Location |
|-----------|------|----------|
| Docker Client | Sends commands via REST API | Your terminal / CI tool |
| Docker Daemon | Executes commands, manages resources | Host OS background process |
| Docker Registry | Stores and serves images | Docker Hub, ECR, self-hosted |
| containerd | Container lifecycle management | Inside dockerd |
| runc | Low-level Linux container creation | Called by containerd |

The client-server model means the daemon can run on a different machine than the client, enabling remote build clusters and CI/CD systems.

---

## External Resources

- [Docker Architecture Overview (Official Docs)](https://docs.docker.com/get-started/docker-overview/)
- [containerd Project](https://containerd.io/)
- [OCI Runtime Specification (runc)](https://github.com/opencontainers/runtime-spec)
