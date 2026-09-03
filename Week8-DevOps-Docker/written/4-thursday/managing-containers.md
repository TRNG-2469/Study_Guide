# Managing Docker Containers

## Learning Objectives
By the end of this lesson, you will be able to:
- List running and stopped containers with `docker ps`
- Stop, start, restart, and remove containers
- Stream and filter container logs with `docker logs`
- Inspect container metadata with `docker inspect`
- Monitor live resource usage with `docker stats`
- Debug running containers with `docker exec`

---

## Why This Matters

Building and starting a container is only half the job. Knowing how to observe it, debug it, and safely stop or remove it is what separates a competent Docker user from someone who just runs `docker run` and hopes for the best. These commands are your day-to-day toolkit for maintaining a healthy containerized environment.

---

## Listing Containers — `docker ps`

```bash
# Show only running containers
docker ps

# Output columns:
# CONTAINER ID   IMAGE     COMMAND                  CREATED       STATUS        PORTS                  NAMES
# a1b2c3d4e5f6   nginx     "/docker-entrypoint.…"  2 hours ago   Up 2 hours    0.0.0.0:8080->80/tcp   web-server

# Show ALL containers — running AND stopped
docker ps -a

# Show only container IDs (useful for scripting)
docker ps -q

# Show all container IDs including stopped
docker ps -aq

# Filter by status
docker ps --filter "status=exited"
docker ps --filter "status=running"

# Filter by name
docker ps --filter "name=web"

# Filter by ancestor image
docker ps --filter "ancestor=nginx"

# Custom format
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

---

## Stopping Containers — `docker stop` and `docker kill`

```bash
# Graceful stop: sends SIGTERM, waits 10 seconds, then SIGKILL
docker stop web-server

# Stop with a custom timeout (seconds to wait before SIGKILL)
docker stop --time 30 web-server

# Immediately force kill (SIGKILL — no graceful shutdown)
docker kill web-server

# Stop multiple containers at once
docker stop web-server api-backend db-primary

# Stop all running containers
docker stop $(docker ps -q)
```

**SIGTERM vs SIGKILL**: `docker stop` gives the application a chance to finish in-flight requests and clean up resources (graceful shutdown). `docker kill` is immediate — use it only when `docker stop` hangs.

---

## Starting and Restarting Containers

```bash
# Start a stopped container (preserves its configuration)
docker start web-server

# Restart a running or stopped container
docker restart web-server

# Restart with a timeout (wait N seconds before SIGKILL)
docker restart --time 15 web-server

# Attach to a started container's output
docker start -a web-server
```

---

## Removing Containers — `docker rm`

```bash
# Remove a stopped container
docker rm web-server

# Force remove a running container (stop + remove in one step)
docker rm -f web-server

# Remove multiple containers
docker rm container1 container2 container3

# Remove all stopped containers
docker container prune

# Remove all stopped containers without confirmation prompt
docker container prune -f

# Remove all containers (running and stopped) — use carefully!
docker rm -f $(docker ps -aq)
```

> **Important**: Removing a container is permanent. Any data written to the container filesystem (not in a volume) is lost. Always use volumes for data you care about.

---

## Viewing Container Logs — `docker logs`

```bash
# Print all logs since container started
docker logs web-server

# Follow (stream) logs in real time
docker logs -f web-server
docker logs --follow web-server

# Show only the last N lines
docker logs --tail 100 web-server

# Show logs with timestamps
docker logs --timestamps web-server

# Combine: follow last 50 lines with timestamps
docker logs -f --tail 50 --timestamps web-server

# Show logs since a specific time
docker logs --since 2024-01-15T10:00:00 web-server

# Show logs from the last 30 minutes
docker logs --since 30m web-server
```

Logs from `docker logs` come from the container's **stdout and stderr**. This is why well-written containerized apps log to stdout rather than to files — Docker captures and manages them automatically.

---

## Inspecting Containers — `docker inspect`

`docker inspect` returns the full JSON metadata for a container: its configuration, networking, mounts, state, and more.

```bash
# Full JSON output for a container
docker inspect web-server

# Extract a specific field using Go template syntax
# Get the container IP address
docker inspect -f "{{.NetworkSettings.IPAddress}}" web-server

# Get the container status
docker inspect -f "{{.State.Status}}" web-server

# Get all mount points
docker inspect -f "{{json .Mounts}}" web-server | python3 -m json.tool

# Get environment variables
docker inspect -f "{{.Config.Env}}" web-server

# Get the restart policy
docker inspect -f "{{.HostConfig.RestartPolicy.Name}}" web-server

# Inspect multiple containers at once
docker inspect web-server api-backend
```

---

## Monitoring Resource Usage — `docker stats`

```bash
# Live resource usage for all running containers
docker stats

# Output:
# CONTAINER ID   NAME         CPU %     MEM USAGE / LIMIT     MEM %   NET I/O         BLOCK I/O
# a1b2c3d4e5f6   web-server   0.01%     12.4MiB / 7.67GiB     0.16%   648B / 0B       0B / 0B
# b2c3d4e5f6a7   api-backend  2.34%     245MiB / 7.67GiB      3.12%   1.2MB / 890KB   18MB / 4MB

# Monitor specific containers
docker stats web-server api-backend

# Show a single snapshot (no streaming)
docker stats --no-stream

# Custom format
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

---

## Executing Commands — `docker exec`

`docker exec` runs a command inside a **running** container. It is your primary debugging tool.

```bash
# Open an interactive bash shell
docker exec -it web-server bash

# Open a shell in a container that only has sh (e.g., Alpine)
docker exec -it myapp sh

# Run a single command and exit
docker exec web-server cat /etc/nginx/nginx.conf

# Check running processes inside the container
docker exec web-server ps aux

# Check disk usage inside the container
docker exec web-server df -h

# Run as root (useful when container runs as non-root)
docker exec -u root -it myapp bash

# Set environment variables for the exec session
docker exec -e DEBUG=verbose -it myapp bash

# Connect to a PostgreSQL database inside a container
docker exec -it postgres-dev psql -U postgres -d mydb
```

---

## Pausing and Unpausing Containers

```bash
# Pause a running container (sends SIGSTOP to all processes)
docker pause web-server

# Unpause — resume the paused container
docker unpause web-server
```

Pausing is useful for taking consistent filesystem snapshots or saving CPU during debugging, without stopping the container.

---

## Renaming a Container

```bash
# Rename a container (can be done while running)
docker rename old-name new-name
```

---

## Copying Files To/From Containers

```bash
# Copy a file from host to container
docker cp /host/path/file.txt web-server:/app/file.txt

# Copy a file from container to host
docker cp web-server:/app/logs/error.log ./error.log

# Copy a directory
docker cp web-server:/app/config ./config-backup
```

---

## Common Workflows

### Debugging a Crashed Container
```bash
# See if it crashed and what its exit code was
docker ps -a --filter "name=myapp"
# STATUS: Exited (1) 5 minutes ago

# Check the logs for the crash reason
docker logs myapp

# Restart it to try again
docker restart myapp

# Or start fresh
docker rm myapp
docker run -d --name myapp myapp:v1.0.0
```

### Cleaning Up All Stopped Containers
```bash
docker container prune -f
```

### Getting a Container IP for Direct Connections
```bash
docker inspect -f "{{.NetworkSettings.IPAddress}}" myapp
```

---

## Summary

| Task | Command |
|------|---------|
| List running | `docker ps` |
| List all | `docker ps -a` |
| Stop gracefully | `docker stop <name>` |
| Force stop | `docker kill <name>` |
| Start stopped | `docker start <name>` |
| Restart | `docker restart <name>` |
| Remove stopped | `docker rm <name>` |
| Remove running | `docker rm -f <name>` |
| Stream logs | `docker logs -f <name>` |
| Full metadata | `docker inspect <name>` |
| Resource usage | `docker stats` |
| Shell access | `docker exec -it <name> bash` |
| Copy files | `docker cp src dest` |

---

## External Resources

- [docker ps Reference](https://docs.docker.com/engine/reference/commandline/ps/)
- [docker logs Reference](https://docs.docker.com/engine/reference/commandline/logs/)
- [docker exec Reference](https://docs.docker.com/engine/reference/commandline/exec/)
