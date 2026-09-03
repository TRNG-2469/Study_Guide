# Docker Images

## Learning Objectives
By the end of this lesson, you will be able to:
- Explain the layered anatomy of a Docker image
- Pull images from Docker Hub and inspect their metadata
- List, tag, and remove images using the Docker CLI
- Understand image IDs, digests, and tags
- Optimize image size through layer awareness

---

## Why This Matters

Images are the foundation of everything you deploy with Docker. Understanding how they are structured — as stacked, read-only layers — explains why they are efficient to distribute, how caching works during builds, and why minimizing unnecessary layers matters for production systems. Sloppy image management leads to bloated registries, slow CI pipelines, and wasted disk space.

---

## What Is a Docker Image?

A Docker image is a **read-only template** that contains:
- A base operating system layer (e.g., Alpine Linux, Ubuntu)
- Application runtime (e.g., JRE, Node.js, Python)
- Application code
- Dependencies and configuration

Think of an image as a **snapshot** of a complete file system at a point in time. When you run an image, Docker adds a thin **writable layer** on top — that is your container. The image itself never changes.

### Image vs. Container Analogy

| Concept | Analogy |
|---------|---------|
| Docker Image | Class definition in Java |
| Docker Container | Object (instance) created from that class |
| `docker run` | `new MyClass()` — creates an instance |

You can run many containers from a single image, just as you can create many objects from one class.

---

## Image Layers

Every instruction in a Dockerfile that modifies the filesystem creates a new **layer**. Layers are:
- **Read-only** — once created, a layer never changes
- **Shared** — multiple images can share the same base layers
- **Cached** — Docker reuses unchanged layers from previous builds
- **Stacked** using a union filesystem (overlay2 on Linux)

```
Image: myapp:1.0
  Layer 4: COPY . /app          (your source code, 2 MB)
  Layer 3: RUN npm install      (node_modules, 45 MB)
  Layer 2: COPY package*.json   (manifest, 5 KB)
  Layer 1: FROM node:20-alpine  (base, 50 MB)
  ─────────────────────────────
  Total: ~97 MB
```

If you change only your source code and rebuild, Docker reuses Layers 1-3 from cache and only rebuilds Layer 4. The push to a registry only uploads Layer 4 — saving bandwidth.

---

## Pulling Images

```bash
# Pull the latest version of an image (implicit :latest tag)
docker pull nginx

# Pull a specific version tag
docker pull nginx:1.25-alpine

# Pull by SHA digest (fully pinned — never changes)
docker pull nginx@sha256:a484819eb60211f5299034ac80f6a681b06f89e65866ce91f356ed7c72af059c

# Pull from a private registry
docker pull myregistry.company.com/myteam/myapp:2.1.0
```

---

## Listing Images

```bash
# List all locally cached images
docker images
# or
docker image ls

# Output:
# REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
# nginx        latest    a6bd71f48f68   2 days ago     187MB
# node         20-alpine 3e3c45f80c9d   5 days ago     131MB
# postgres     16        e394a3f65adf   1 week ago     432MB

# Show all images including intermediate layers
docker images -a

# Filter by repository name
docker images nginx

# Show only image IDs (useful for scripting)
docker images -q

# Format output as JSON-like table
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
```

---

## Inspecting Images

```bash
# Full JSON metadata for an image
docker image inspect nginx

# Extract specific fields using Go template syntax
docker image inspect nginx --format "{{.Os}}/{{.Architecture}}"
# Output: linux/amd64

# View the image history (layers and their commands)
docker image history nginx
# IMAGE         CREATED       CREATED BY                                SIZE
# a6bd71f48f68  2 days ago    CMD ["nginx" "-g" "daemon off;"]          0B
# <missing>     2 days ago    EXPOSE map[80/tcp:{}]                     0B
# <missing>     2 days ago    RUN /bin/sh -c ...                        61.1MB
```

The `history` command shows you exactly which Dockerfile instruction created each layer and how large each layer is — invaluable for debugging large image sizes.

---

## Tagging Images

Tags give human-readable names to images. The full image name format is:

```
[registry/][namespace/]repository[:tag]
```

Examples:
```
nginx                          # Docker Hub official image, latest tag
nginx:1.25-alpine              # Docker Hub official, specific version
library/nginx:1.25-alpine      # Same, with explicit namespace
myuser/myapp:v2.1.0            # Docker Hub personal repo
ghcr.io/myorg/myapp:sha-abc123 # GitHub Container Registry
myregistry:5000/myapp:staging  # Private registry with port
```

```bash
# Tag an existing image with a new name
docker tag nginx:latest myuser/my-nginx:1.0

# Tag a locally built image for Docker Hub
docker tag myapp:latest myusername/myapp:v1.2.3

# Tag an image by its ID
docker tag a6bd71f48f68 myapp:stable
```

---

## Removing Images

```bash
# Remove a specific image by name:tag
docker rmi nginx:latest

# Remove by image ID (you can use just the first few characters)
docker rmi a6bd71f4

# Force remove (even if a container is using it)
docker rmi -f nginx:latest

# Remove multiple images at once
docker rmi nginx:1.24 nginx:1.25 postgres:15

# Remove all unused images (not referenced by any container)
docker image prune

# Remove ALL images — including ones referenced by stopped containers
docker image prune -a

# Remove images with a filter (e.g., created more than 24 hours ago)
docker image prune -a --filter "until=24h"
```

> **Caution**: `docker image prune -a` removes all locally cached images. Your next pull will re-download everything from the registry.

---

## Image IDs and Digests

Every image has two identifiers:

**Image ID** — a SHA256 hash of the image configuration:
```
sha256:a6bd71f48f68d31d9a4fb6e3d7c2cdde5d9f16c826cc2b89f55a9e9de929b14a
# Usually shown as short form: a6bd71f48f68
```

**Digest** — a SHA256 hash of the image manifest (stable across pulls):
```
nginx@sha256:a484819eb60211f5299034ac80f6a681b06f89e65866ce91f356ed7c72af059c
```

The digest is the safest way to pin an image — it is immutable even if someone pushes a new image with the same tag name.

```bash
# Show digests alongside image list
docker images --digests
```

---

## Image Size Optimization Tips

| Technique | Impact |
|-----------|--------|
| Use Alpine-based images (`node:20-alpine` vs `node:20`) | 70-80% size reduction |
| Use `slim` variants (`python:3.11-slim`) | 50-60% size reduction |
| Multi-stage builds | Remove build tools from final image |
| Chain `RUN` commands with `&&` | Fewer layers, cleanup in same layer |
| Use `.dockerignore` | Avoid copying unnecessary files |
| Remove package manager caches | `rm -rf /var/lib/apt/lists/*` |

```bash
# Compare sizes
docker images | grep node
# node    20         12345abc   ...   1.1GB
# node    20-slim    67890def   ...   240MB
# node    20-alpine  abcdef12   ...   131MB
```

---

## Summary

- An image is a read-only, layered filesystem template
- Layers are cached, shared, and only re-built when their instruction changes
- Use `docker pull` to fetch, `docker images` to list, `docker image inspect` to examine
- Use `docker rmi` or `docker image prune` to clean up unused images
- Pin image tags or digests in production — never rely on `:latest`
- Smaller base images (Alpine, slim) dramatically reduce attack surface and deploy time

---

## External Resources

- [Docker Images Overview (Official)](https://docs.docker.com/get-started/docker-concepts/the-basics/what-is-an-image/)
- [Understanding Image Layers](https://docs.docker.com/storage/storagedriver/)
- [Docker Hub Official Images](https://hub.docker.com/search?q=&type=image&image_filter=official)
