# Docker Containers

## Learning Objectives
By the end of this lesson, you will be able to:
- Explain the difference between a Docker image and a container
- Describe the full container lifecycle and its states
- Create containers using `docker create` and run them interactively or in detached mode
- Use container naming for easier management
- Debug running containers with `docker exec`

---

## Why This Matters

Containers are the heart of Docker — they are the actual running processes that serve your application. Understanding the difference between images and containers, and knowing how to control a container's lifecycle, is the skill you will use every single day as a developer or DevOps engineer. Misunderstanding this distinction is one of the most common sources of confusion for Docker beginners.

---

## Image vs. Container: The Core Distinction

| Concept | Image | Container |
|---------|-------|-----------|
| What is it? | Read-only filesystem snapshot | Running instance of an image |
| Mutable? | No — never changes | Yes — has a writable layer |
| Stored where? | Local image cache / registry | Docker daemon (local machine) |
| Created by | `docker build` or `docker pull` | `docker run` or `docker create` |
| Analogy | Cookie cutter / Class definition | Cookie / Object instance |

When you run `docker run nginx`, Docker:
1. Finds the `nginx` image (pulls it if not cached)
2. Creates a new **container** — a writable layer on top of the image
3. Sets up networking and namespaces
4. Starts the `nginx` process inside the container

The image is unchanged. The container is where your application actually lives.

---

## Container Lifecycle States

A container moves through these states during its life:

```
          docker create
               |
               v
           [CREATED]
               |
         docker start
               |
               v
           [RUNNING] <----> [PAUSED]
               |           (docker pause / docker unpause)
               |
     process exits / docker stop
               |
               v
           [EXITED]
               |
          docker rm
               |
               v
           [DELETED]
```

- **CREATED**: Container is configured but the process has not started
- **RUNNING**: Container process is actively executing
- **PAUSED**: Process is suspended (SIGSTOP) — useful for snapshotting
- **EXITED**: Process terminated (either naturally or via `docker stop`)
- **DELETED**: Container is permanently removed from the system

---

## Running Containers Interactively

```bash
# Run a container and attach your terminal to it
# -i: Keep STDIN open (interactive)
# -t: Allocate a pseudo-TTY (terminal emulation)
docker run -it ubuntu:22.04 bash

# Now you are inside the container — a full Ubuntu shell!
root@abc123def456:/# ls /
root@abc123def456:/# apt-get install -y curl
root@abc123def456:/# exit   # container stops when shell exits
```

Interactive mode is useful for:
- Exploring what is inside an image
- Debugging a container environment
- Running one-off administrative commands

---

## Running Containers in Detached Mode

```bash
# -d: Detach — run in the background, print container ID
docker run -d nginx

# Combine detached with port mapping and a name
docker run -d --name my-webserver -p 8080:80 nginx

# Check that it is running
docker ps
# CONTAINER ID   IMAGE   COMMAND                  STATUS         PORTS
# a1b2c3d4e5f6   nginx   "/docker-entrypoint.…"  Up 2 minutes   0.0.0.0:8080->80/tcp

# Visit http://localhost:8080 in your browser — nginx serves the default page
```

Detached mode is used for long-running services: web servers, databases, message queues.

---

## Naming Containers

By default Docker assigns a random name (e.g., `happy_payne`, `boring_tesla`). Always name your containers in development:

```bash
# --name: Give the container a human-readable name
docker run -d --name postgres-dev -e POSTGRES_PASSWORD=secret postgres:16

# Named containers are easier to reference in subsequent commands
docker logs postgres-dev
docker exec -it postgres-dev psql -U postgres
docker stop postgres-dev
docker rm postgres-dev
```

---

## Executing Commands in a Running Container

```bash
# Open an interactive shell in a running container
docker exec -it my-webserver bash

# Run a one-off command without an interactive session
docker exec my-webserver nginx -t     # test nginx config
docker exec postgres-dev pg_dump mydb > backup.sql

# Run as a specific user
docker exec -u root -it mycontainer bash

# Set environment variables for the exec session
docker exec -e DEBUG=true -it myapp bash
```

`docker exec` is your primary debugging tool — it lets you inspect the container filesystem, check processes, and run diagnostic commands without stopping the container.

---

## Separating `docker create` and `docker start`

Sometimes you want to create a container but not start it immediately:

```bash
# Create without starting
docker create --name myapp-staging myapp:v1.0.0

# Inspect it before starting
docker inspect myapp-staging

# Start it when ready
docker start myapp-staging

# Attach to its output
docker attach myapp-staging
```

This is useful in orchestration scripts where containers need to be pre-configured before being started in a specific order.

---

## Container vs. Virtual Machine

| Feature | Container | Virtual Machine |
|---------|-----------|-----------------|
| Startup time | Milliseconds | Minutes |
| Size | MBs | GBs |
| OS | Shares host kernel | Full OS copy |
| Isolation | Process-level | Hardware-level |
| Performance overhead | Near-zero | Significant |
| Use case | Microservices, apps | Full OS environments |

Containers are not VMs. They are isolated **processes** on the host machine that share the Linux kernel. This is why they are so fast and lightweight.

---

## Summary

- An image is the read-only template; a container is the running instance
- Container states: created → running → paused → exited → deleted
- Use `-it` for interactive sessions, `-d` for detached background services
- Always name your containers with `--name` for easier management
- Use `docker exec -it <name> bash` to debug any running container
- Containers share the host kernel — they are not VMs

---

## External Resources

- [What is a Container? (Official)](https://docs.docker.com/get-started/docker-concepts/the-basics/what-is-a-container/)
- [Container Lifecycle Commands](https://docs.docker.com/engine/reference/commandline/container/)
- [Containers vs. Virtual Machines Explained](https://www.docker.com/resources/what-container/)
