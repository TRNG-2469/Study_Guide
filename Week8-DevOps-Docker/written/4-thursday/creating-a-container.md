# Creating a Docker Container

## Learning Objectives
By the end of this lesson, you will be able to:
- Use `docker run` with its most important flags
- Map ports between host and container
- Inject environment variables at container startup
- Mount volumes for data persistence
- Connect containers to networks
- Set restart policies and resource limits

---

## Why This Matters

`docker run` is the most-used Docker command. Its flags control nearly every aspect of how a container behaves: which ports it exposes, what environment it receives, where it stores data, how much CPU/RAM it can use, and what happens if it crashes. Mastering these flags is the difference between a fragile, hard-to-manage container and a well-configured, production-grade one.

---

## The `docker run` Command

```bash
docker run [OPTIONS] IMAGE [COMMAND] [ARG...]
```

At minimum, you need an image name. Every other flag is optional but often essential.

---

## Image Name and Tag

```bash
# Use the latest tag (implicit)
docker run nginx

# Pin a specific version (always do this in production)
docker run nginx:1.25-alpine

# Use an image from a private registry
docker run myregistry.company.com/backend-api:v2.3.1

# Use a locally built image
docker run myapp:latest
```

---

## Detached vs. Interactive Mode

```bash
# -d: Run in background (detached), print container ID
docker run -d nginx

# -it: Interactive + TTY — attach your terminal
docker run -it ubuntu:22.04 bash

# --rm: Automatically delete container when it exits (great for one-off commands)
docker run --rm -it python:3.11 python3
```

---

## Naming Containers

```bash
# --name: Give the container a memorable name
docker run -d --name web-server nginx
docker run -d --name api-backend myapp:v1
docker run -d --name db-primary postgres:16
```

Without `--name`, Docker assigns a random name. Named containers are much easier to reference in logs, exec, stop, and remove commands.

---

## Port Mapping (-p)

Containers have their own network namespace — ports are not automatically accessible from the host. You must explicitly map them.

```bash
# -p hostPort:containerPort
docker run -d -p 8080:80 nginx
# Now: http://localhost:8080 → nginx inside container on port 80

# Map multiple ports
docker run -d -p 8080:80 -p 8443:443 nginx

# Bind to a specific host interface (not all interfaces)
docker run -d -p 127.0.0.1:8080:80 nginx

# Map to a random available host port
docker run -d -p 80 nginx
# Find which port was assigned:
docker port <container-name> 80

# Full example: Spring Boot app
docker run -d --name springapp -p 8080:8080 myorg/springboot-app:v1.0.0
```

---

## Environment Variables (-e)

```bash
# -e: Set a single environment variable
docker run -d -e SPRING_PROFILES_ACTIVE=production myapp

# Set multiple environment variables
docker run -d \
  -e POSTGRES_DB=mydb \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=secret123 \
  postgres:16

# Load env vars from a file (--env-file)
# Create a .env file:
# DB_HOST=localhost
# DB_PORT=5432
# DB_NAME=mydb
docker run -d --env-file .env myapp

# Combine both
docker run -d --env-file .env -e LOG_LEVEL=debug myapp
```

> **Security warning**: Do NOT store secrets in Dockerfiles with `ENV`. Use `--env-file` with a file excluded from version control, or use a secrets manager (HashiCorp Vault, AWS Secrets Manager, Docker Secrets).

---

## Volume Mounts (-v)

```bash
# Bind mount: map a host directory into the container
# -v hostPath:containerPath
docker run -d -v /home/user/data:/app/data myapp

# Named volume (Docker manages the storage location)
docker run -d -v myapp-data:/app/data myapp

# Read-only mount (container cannot write to it)
docker run -d -v /host/config:/app/config:ro myapp

# Mount a single file
docker run -d -v /home/user/app.conf:/etc/myapp/app.conf myapp

# Practical example: PostgreSQL with persistent data
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=secret \
  -v postgres-data:/var/lib/postgresql/data \
  postgres:16
# Data persists even if the container is removed
```

---

## Networking (--network)

```bash
# Connect to the default bridge network (default if not specified)
docker run -d --network bridge nginx

# Connect to a custom user-defined network
docker network create myapp-net
docker run -d --network myapp-net --name web nginx
docker run -d --network myapp-net --name api myapp
# Containers on the same network can reach each other by name: http://web:80

# Use host networking (container shares host network stack — Linux only)
docker run -d --network host nginx

# No networking at all (isolated)
docker run -d --network none myapp
```

---

## Restart Policy (--restart)

```bash
# no: Never restart (default)
docker run -d --restart no myapp

# always: Always restart, even after daemon restart
docker run -d --restart always nginx

# unless-stopped: Restart unless you explicitly stop it
docker run -d --restart unless-stopped nginx

# on-failure: Restart only if exit code is non-zero
docker run -d --restart on-failure myapp

# on-failure with max retries
docker run -d --restart on-failure:5 myapp
```

Use `--restart unless-stopped` or `--restart always` for services that should survive server reboots.

---

## Resource Limits (--memory, --cpus)

Without limits, a misbehaving container can consume all host resources:

```bash
# Limit memory to 512 MB
docker run -d --memory 512m myapp

# Limit to 1.5 CPU cores
docker run -d --cpus 1.5 myapp

# Combined resource limits
docker run -d \
  --name api \
  --memory 256m \
  --memory-swap 256m \
  --cpus 0.5 \
  myapp:v1.0.0

# View resource usage of running containers
docker stats
```

---

## A Complete Real-World Example

Putting it all together — a containerized Java Spring Boot application:

```bash
docker run -d \
  --name springboot-api \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  --env-file .env.production \
  -v /var/log/myapp:/app/logs \
  --network myapp-net \
  --memory 512m \
  --cpus 1.0 \
  myorg/springboot-api:v2.1.0
```

This single command:
- Runs detached with a meaningful name
- Restarts on crash or server reboot
- Exposes port 8080
- Configures the Spring profile and database connection
- Loads secrets from a `.env` file
- Mounts a log directory for persistence
- Joins a private network to communicate with the database
- Caps memory at 512 MB and CPU at 1 core

---

## Summary of Key Flags

| Flag | Purpose | Example |
|------|---------|---------|
| `-d` | Detached mode | `docker run -d nginx` |
| `-it` | Interactive + TTY | `docker run -it ubuntu bash` |
| `--rm` | Auto-remove on exit | `docker run --rm python:3.11 python3` |
| `--name` | Container name | `--name myapp` |
| `-p` | Port mapping | `-p 8080:80` |
| `-e` | Environment variable | `-e NODE_ENV=production` |
| `--env-file` | Load env from file | `--env-file .env` |
| `-v` | Volume mount | `-v data:/app/data` |
| `--network` | Network | `--network myapp-net` |
| `--restart` | Restart policy | `--restart unless-stopped` |
| `--memory` | Memory limit | `--memory 512m` |
| `--cpus` | CPU limit | `--cpus 1.5` |

---

## External Resources

- [`docker run` Reference (Official)](https://docs.docker.com/engine/reference/run/)
- [Docker Networking Overview](https://docs.docker.com/network/)
- [Docker Resource Constraints](https://docs.docker.com/config/containers/resource_constraints/)
