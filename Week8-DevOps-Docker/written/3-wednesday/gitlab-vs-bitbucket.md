# GitLab vs Bitbucket — Platform Comparison

## Learning Objectives

By the end of this lesson, you will be able to:

- Compare GitLab and Bitbucket across their major feature categories
- Describe the CI/CD approach native to each platform
- Explain differences in pricing, integrations, and hosted runner offerings
- Apply a structured decision framework to recommend the right platform for a given team

---

## Why This Matters

As a developer or DevOps engineer, you will often join teams that have already chosen a platform, or you will be asked to help evaluate options. Understanding the differences between GitLab and Bitbucket allows you to contribute meaningfully to those conversations, avoid platform-specific surprises, and migrate knowledge from one tool to another when switching employers or clients.

---

## Background: What Are These Platforms?

Both GitLab and Bitbucket are **Git repository hosting platforms** — they store your code, manage branches and merge/pull requests, and provide tooling for code review. But they differ significantly in their built-in feature sets, ecosystem integrations, and philosophy.

**GitLab** is a single, self-contained DevOps platform. Its philosophy is "everything in one place" — source code, CI/CD, container registry, package registry, security scanning, project management, and infrastructure management all live under one roof.

**Bitbucket** (by Atlassian) is a Git host deeply integrated with the Atlassian ecosystem — Jira, Confluence, and Bamboo. It excels in environments already using those tools. Bitbucket Pipelines provides basic built-in CI/CD, while complex pipelines are often delegated to Bamboo (Atlassian's dedicated CI server).

---

## Feature Comparison

### Source Code Management (Core Git Features)

| Feature | GitLab | Bitbucket |
|---|---|---|
| Repository hosting | Yes | Yes |
| Merge requests (GitLab) / Pull requests (Bitbucket) | Yes | Yes |
| Code review inline comments | Yes | Yes |
| Branch protection rules | Yes | Yes |
| Required reviewers / approvals | Yes (with approval rules) | Yes (with branch permissions) |
| Fork workflow | Yes | Yes |
| Code owners (`CODEOWNERS`) | Yes | Yes |
| Repository mirroring | Yes (push and pull) | Yes (mirror only) |
| Repository size limit (cloud) | 10 GB (free) | 2 GB (free) |
| Private repos on free plan | Unlimited | Unlimited (up to 5 users) |

---

### CI/CD Capabilities

This is where the platforms diverge most significantly.

| Feature | GitLab CI/CD | Bitbucket Pipelines |
|---|---|---|
| Configuration file | `.gitlab-ci.yml` | `bitbucket-pipelines.yml` |
| Native CI/CD | Yes — built-in, no plugin needed | Yes — basic pipelines built-in |
| Pipeline visualization graph | Yes (rich, interactive) | Basic (linear view) |
| Parallel jobs | Yes | Yes |
| DAG pipelines (needs keyword) | Yes — full DAG support | Limited — no direct equivalent |
| Parent-child pipelines | Yes | No |
| Cross-project triggers | Yes | No |
| Scheduled pipelines | Yes | Yes |
| Manual approval gates | Yes (`when: manual`) | Yes (deployment gates) |
| Environment tracking | Yes (Environments dashboard) | Limited |
| Auto DevOps | Yes (zero-config pipelines) | No |
| Advanced CI/CD for enterprise | Same tool, more features | Requires Bamboo (separate product) |

**Bitbucket Pipelines configuration example** — for comparison with `.gitlab-ci.yml`:

```yaml
# bitbucket-pipelines.yml

# Pipelines are triggered by push, pull request, or schedule.
image: maven:3.9-eclipse-temurin-17    # Default image for all steps

pipelines:
  # 'default' runs on every branch that doesn't match a more specific rule.
  default:
    - step:
        name: Build and Test
        caches:
          - maven                       # Built-in cache shortcut for Maven
        script:
          - mvn clean test              # Commands to run
        artifacts:
          - target/**                   # Files to pass to the next step

    - step:
        name: Package
        script:
          - mvn package -DskipTests

  # Branch-specific pipelines — run different steps per branch.
  branches:
    main:
      - step:
          name: Deploy to Staging
          deployment: staging           # Marks this as a deployment step
          script:
            - ./deploy.sh staging

      - step:
          name: Deploy to Production
          deployment: production
          trigger: manual               # Requires manual trigger (like GitLab's when: manual)
          script:
            - ./deploy.sh production

  # Pull request pipelines — run when a PR is opened.
  pull-requests:
    '**':                               # Match any branch name
      - step:
          name: PR Validation
          script:
            - mvn test
```

Note how Bitbucket Pipelines is simpler but less powerful. It lacks DAG pipelines, cross-project triggers, and deep artifact reporting. For teams that outgrow it, Atlassian recommends **Bamboo** (a separate CI/CD product).

---

### Security Scanning

GitLab has a significant advantage here — its security scanning features are built directly into the platform.

| Security Feature | GitLab | Bitbucket |
|---|---|---|
| SAST (Static Analysis Security Testing) | Built-in (free tier includes basic) | Via third-party integrations |
| DAST (Dynamic Application Security Testing) | Built-in (Ultimate tier) | Via third-party integrations |
| Dependency scanning | Built-in | Via Snyk or similar integrations |
| Secret detection | Built-in | Via third-party or manual setup |
| Container scanning | Built-in | Via third-party |
| Security dashboard | Yes — unified view | No native dashboard |
| License compliance scanning | Built-in (Ultimate) | No native feature |

**GitLab SAST example** — adding security scanning is a single line:

```yaml
# In your .gitlab-ci.yml, include GitLab's managed SAST template.
# This automatically adds security scanning jobs to your pipeline.
include:
  - template: Security/SAST.gitlab-ci.yml         # Static analysis for source code
  - template: Security/Secret-Detection.gitlab-ci.yml  # Find leaked secrets in commits
  - template: Security/Dependency-Scanning.gitlab-ci.yml  # Check for vulnerable dependencies

stages:
  - test
  - build
  - deploy
  # Security jobs are automatically added to the 'test' stage by the templates above.
```

---

### Package and Container Registry

| Feature | GitLab | Bitbucket |
|---|---|---|
| Container Registry | Yes — built-in | Yes — via Atlassian Container Registry (limited) |
| Maven Package Registry | Yes | No native option |
| npm Package Registry | Yes | No native option |
| PyPI Package Registry | Yes | No native option |
| NuGet Package Registry | Yes | No native option |
| Generic package registry | Yes | No |
| Registry integrated with CI auth | Yes — `$CI_JOB_TOKEN` auth | Manual credential setup |

GitLab's container registry is deeply integrated with CI. The predefined variables `$CI_REGISTRY`, `$CI_REGISTRY_USER`, `$CI_REGISTRY_PASSWORD`, and `$CI_REGISTRY_IMAGE` make pushing and pulling images trivial with no manual configuration.

---

### Hosted Runner Offerings

| Feature | GitLab.com (SaaS) | Bitbucket Cloud |
|---|---|---|
| Hosted runners included | Yes | Yes |
| Free compute minutes | 400 min/month (free tier) | 50 min/month (free tier) |
| OS options | Linux x86, Linux ARM, macOS, Windows | Linux only (free tier) |
| GPU runners | Available (add-on) | Not available |
| Custom runner registration | Yes | Yes |
| Runner autoscaling | Yes (with Docker Machine or autoscaler) | Limited |
| Self-hosted (on-premise) runners | Yes — open source runner agent | Yes — Bitbucket Runner |

**GitLab.com compute minutes pricing (approximate as of 2024):**
- Free: 400 min/month
- Premium: 10,000 min/month
- Ultimate: 50,000 min/month
- Additional minutes: purchasable separately

---

### Project Management and Integrations

| Feature | GitLab | Bitbucket |
|---|---|---|
| Built-in issue tracker | Yes | Basic (limited) |
| Agile boards (kanban/scrum) | Yes — built-in | Requires Jira |
| Roadmaps | Yes (Premium+) | Requires Jira |
| Jira integration | Yes (bi-directional) | Native — Jira is same ecosystem |
| Confluence integration | Basic | Native — Confluence is same ecosystem |
| Slack integration | Yes | Yes |
| Webhooks | Yes | Yes |
| API access | Yes (comprehensive REST + GraphQL) | Yes (REST) |
| VS Code extension | Yes | Yes |

---

### Pricing Tiers

**GitLab (cloud — GitLab.com):**

| Tier | Price (per user/month) | Key Features |
|---|---|---|
| Free | $0 | Unlimited private repos, 400 CI min, 5 GB storage |
| Premium | ~$29 | Code owners, merge request approvals, 10,000 CI min |
| Ultimate | ~$99 | Security dashboards, DAST, compliance, 50,000 CI min |

**Bitbucket (cloud — Atlassian):**

| Tier | Price (per user/month) | Key Features |
|---|---|---|
| Free | $0 | Up to 5 users, 50 CI min, 1 GB LFS |
| Standard | ~$3 | Unlimited users, 2,500 CI min, branch permissions |
| Premium | ~$6 | Unlimited CI min, advanced permissions, enforced MR checks |

Note: Bitbucket's pricing is lower per user, but large teams often need Jira (separate cost) and Bamboo (additional cost) to match GitLab's feature set, which can make the total cost similar or higher.

---

## Decision Guide

Use this framework to decide which platform fits a given team:

### Choose GitLab when:

- Your team wants a **single platform** for the entire DevOps lifecycle (code, CI/CD, security, registry, monitoring)
- You need **advanced CI/CD** — DAG pipelines, cross-project triggers, parent-child pipelines, Auto DevOps
- **Security and compliance** are priorities — GitLab's built-in SAST, DAST, and dependency scanning reduce the need for third-party tools
- You need a **container or package registry** tightly integrated with CI authentication
- You are building on **Kubernetes** — GitLab's Kubernetes integration and runner autoscaling are mature
- You are **self-hosting** — GitLab CE (Community Edition) is free and open source with a comprehensive feature set

### Choose Bitbucket when:

- Your organization is **already invested in the Atlassian ecosystem** — Jira is the primary project tracker, Confluence is the wiki, and Bamboo (or Jenkins) handles complex CI/CD
- **Team size is small** (under 5 users) and you need a free, simple Git host with basic pipelines
- **Jira integration** is non-negotiable and must be native (not via webhook/plugin)
- The team's CI/CD complexity is **low** — straightforward build-test-deploy with no need for DAG or cross-project pipelines
- Your organization uses **Atlassian Cloud** and wants a unified billing and SSO experience

### Hybrid Approach

Many large organizations use Bitbucket for source control (to stay in the Atlassian ecosystem for Jira) while routing CI/CD through Jenkins, Bamboo, or even GitLab CI (with a GitLab mirror). This is common but adds operational complexity.

---

## Summary Comparison Table

| Category | GitLab | Bitbucket |
|---|---|---|
| Platform philosophy | All-in-one DevOps | Git host + Atlassian ecosystem |
| CI/CD power | High (native, feature-rich) | Moderate (Pipelines) or High (with Bamboo) |
| Security scanning | Built-in | Third-party required |
| Package registry | Comprehensive | Limited |
| Jira integration | Good (via integration) | Native (same company) |
| Free CI minutes | 400/month | 50/month |
| Self-hosting | Yes (open source CE) | Yes (Bitbucket Data Center, paid) |
| Best for | Greenfield DevOps teams | Atlassian-first organizations |

---

## External Resources

- [GitLab feature comparison page](https://about.gitlab.com/competition/bitbucket/)
- [Bitbucket Pipelines documentation](https://support.atlassian.com/bitbucket-cloud/docs/get-started-with-bitbucket-pipelines/)
- [GitLab vs Bitbucket — Atlassian's own comparison](https://www.atlassian.com/git/tutorials/bitbucket-vs-github-vs-gitlab)
