# Docker Hub

## Learning Objectives
By the end of this lesson, you will be able to:
- Describe Docker Hub and its role in the Docker ecosystem
- Distinguish between Official Images, Verified Publisher images, and community images
- Push and pull images using the Docker Hub workflow
- Understand image tagging strategies for repositories
- Work with automated builds and be aware of rate limits

---

## Why This Matters

Docker Hub is the default registry Docker pulls from. Every time you write `FROM node:20-alpine` or `FROM postgres:16`, Docker fetches that image from Docker Hub. Understanding how to use Docker Hub — authenticate, push your own images, and choose trustworthy base images — is a daily DevOps skill. Rate limits also directly impact CI/CD pipelines, so knowing how to manage them prevents broken builds.

---

## What Is Docker Hub?

Docker Hub is the world's largest container registry, maintained by Docker Inc. It serves as:

1. **The default registry** — `docker pull nginx` implicitly means `docker pull docker.io/library/nginx`
2. **A public image marketplace** — millions of community images
3. **A private image host** — store proprietary application images
4. **An automated build trigger** — rebuild images when GitHub/GitLab code changes

URL: [https://hub.docker.com](https://hub.docker.com)

---

## Image Categories on Docker Hub

### Official Images
- Maintained by Docker Inc. in partnership with upstream software teams
- Examples: `nginx`, `postgres`, `redis`, `node`, `python`, `ubuntu`
- Undergo security scanning and follow Dockerfile best practices
- Shown with an "Official Image" badge
- No namespace prefix: `docker pull postgres` (not `docker/postgres`)

### Verified Publisher Images
- Maintained by ISVs (Independent Software Vendors) like Microsoft, Elastic, HashiCorp
- Examples: `mcr.microsoft.com/dotnet/aspnet`, `elastic/elasticsearch`
- Verified by Docker — the publisher is a real company
- May have separate registries (MCR for Microsoft)

### Community Images
- Uploaded by individual users or organizations
- Format: `username/image-name:tag`
- Examples: `bitnami/wordpress`, `linuxserver/plex`
- **Use with caution** — review the Dockerfile and star count before using in production

---

## Creating a Docker Hub Account and Repository

1. Sign up at [https://hub.docker.com](https://hub.docker.com)
2. Click **Create Repository**
3. Choose a name (e.g., `myapp`)
4. Set visibility: **Public** (free, unlimited) or **Private** (1 free, then paid)
5. Click **Create**

Your repository URL will be: `hub.docker.com/r/yourusername/myapp`

---

## Authentication

```bash
# Login to Docker Hub (prompts for username and password)
docker login

# Login with credentials inline (useful for scripts — prefer access tokens)
docker login -u myusername -p mypassword

# Login with an Access Token (more secure than password)
# Generate at: hub.docker.com > Account Settings > Security > Access Tokens
docker login -u myusername --password-stdin <<< "dckr_pat_xxxxx"

# Logout (removes stored credentials)
docker logout
```

> **Security tip**: Use Docker Hub Access Tokens instead of your password. Tokens can be scoped to read-only or read/write, and revoked independently without changing your password.

---

## Pushing an Image to Docker Hub

The full workflow: build, tag, push.

```bash
# Step 1: Build your image
docker build -t myapp .

# Step 2: Tag it with your Docker Hub username and repository
# Format: username/repository:tag
docker tag myapp myusername/myapp:v1.0.0

# Also tag as latest (optional but conventional)
docker tag myapp myusername/myapp:latest

# Step 3: Push both tags
docker push myusername/myapp:v1.0.0
docker push myusername/myapp:latest
```

During the push, Docker only uploads layers that do not already exist in the registry. If your base image (`node:20-alpine`) is already on Docker Hub, those layers are skipped — only your application layers are uploaded.

---

## Pulling an Image from Docker Hub

```bash
# Pull using the default (latest) tag
docker pull myusername/myapp

# Pull a specific version tag
docker pull myusername/myapp:v1.0.0

# Pull by SHA digest (immutable — never changes even if tag is updated)
docker pull myusername/myapp@sha256:abc123def456...

# Pull from Docker Hub explicitly (same as without the prefix)
docker pull docker.io/myusername/myapp:v1.0.0
```

---

## Tagging Strategy

A consistent tagging strategy prevents confusion in teams and CI/CD systems:

| Tag Pattern | Example | Use Case |
|-------------|---------|----------|
| Semantic version | `v1.2.3` | Stable releases |
| Major.minor | `v1.2` | Alias to latest patch |
| `latest` | `latest` | Convenience alias (use carefully) |
| Git SHA | `sha-abc1234` | Exact commit traceability |
| Branch name | `main`, `develop` | Auto-builds from branches |
| Environment | `staging`, `production` | Deployment-specific images |

**Recommended CI/CD tagging**:
```bash
# Tag with git commit SHA for traceability
docker tag myapp myusername/myapp:$(git rev-parse --short HEAD)

# Also tag the branch
docker tag myapp myusername/myapp:$(git rev-parse --abbrev-ref HEAD)
```

---

## Automated Builds (Linked Repositories)

Docker Hub can automatically build a new image whenever you push code to GitHub or GitLab:

1. Link your Docker Hub account to GitHub/GitLab in Account Settings
2. Create a repository and enable **Automated Builds**
3. Configure build rules: which branch/tag triggers which Docker image tag
4. Every `git push` to `main` can automatically build and push `myapp:latest`

> **Note**: Free automated builds are limited on Docker Hub. Most teams use GitHub Actions or GitLab CI to build and push images instead — it gives more control and is often faster.

---

## Rate Limits

Docker Hub imposes **pull rate limits** to prevent abuse:

| Account Type | Pull Rate Limit |
|-------------|----------------|
| Anonymous (unauthenticated) | 100 pulls per 6 hours per IP |
| Free account (authenticated) | 200 pulls per 6 hours |
| Pro/Team account | Unlimited |

**Impact on CI/CD**: If your CI/CD system (GitHub Actions, Jenkins, GitLab CI) runs many parallel builds from the same IP address without authentication, it will hit the anonymous rate limit quickly and builds will fail with:

```
Error response from daemon: toomanyrequests: You have reached your pull rate limit.
```

**Solutions**:
1. Authenticate your CI runner: `docker login -u $DOCKER_USER -p $DOCKER_TOKEN`
2. Use a Docker Hub paid plan
3. Mirror images to your private registry (ECR, GitLab Registry)
4. Use a registry cache proxy

---

## Security Scanning

Docker Hub scans Official Images for known CVEs (Common Vulnerabilities and Exposures):

- View scan results on the image page under the **Tags** tab
- Look for images with 0 critical/high vulnerabilities
- Re-scan on push for paid accounts

For your own images, Docker Hub offers **Docker Scout** — a more comprehensive security analysis tool.

---

## Summary

| Task | Command |
|------|---------|
| Login | `docker login` |
| Build and tag | `docker build -t user/repo:tag .` |
| Push image | `docker push user/repo:tag` |
| Pull image | `docker pull user/repo:tag` |
| Logout | `docker logout` |
| List local images | `docker images` |

Docker Hub is the starting point for most Docker workflows, but production teams often migrate to private registries (AWS ECR, GitLab Registry, Harbor) for better security, control, and rate limit elimination.

---

## External Resources

- [Docker Hub Overview (Official)](https://docs.docker.com/docker-hub/)
- [Docker Hub Access Tokens](https://docs.docker.com/docker-hub/access-tokens/)
- [Docker Hub Rate Limiting](https://docs.docker.com/docker-hub/download-rate-limit/)
