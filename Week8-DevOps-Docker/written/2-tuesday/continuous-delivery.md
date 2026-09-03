# Continuous Delivery

## Learning Objectives

By the end of this lesson, you will be able to:

- Define Continuous Delivery and distinguish it from Continuous Integration
- Explain the role of the human approval gate in Continuous Delivery
- Distinguish between Continuous Delivery and Continuous Deployment
- Describe environment progression from development through production
- Explain what a versioned release artifact is and why it matters

---

## Why This Matters

Continuous Integration ensures your code always works. Continuous Delivery ensures that working code is always ready to ship. These are different guarantees. A team that has CI without CD has a validated artifact that still requires a manual, error-prone deployment process. CD completes the pipeline: at any moment, a business decision to release is a software deployment that is fast, automated, and low-risk. Understanding CD is essential for working in any organization that ships software to real users.

---

## Picking Up Where CI Left Off

Recall that CI ends with a **verified artifact** — a built and tested deployable package (a JAR file, a Docker image, an npm bundle) that has passed all automated checks. The CI pipeline answers the question: "Is this code correct?"

Continuous Delivery extends the question to: "Is this code ready to deploy to production at any time?"

This is a much higher bar. CI catching unit test failures is necessary but not sufficient. CD requires that:
- The artifact can be deployed to a production-like environment automatically
- The deployment process itself is fully automated and scripted (no manual steps)
- The application behaves correctly in a production-like environment under realistic conditions
- A human decision-maker (product owner, release manager, engineering lead) can trigger a production deployment confidently and at any time — in minutes, not hours

---

## Continuous Delivery: The Definition

**Continuous Delivery (CD)** is the practice of keeping software in a releasable state at all times, with the ability to deploy any verified build to production by triggering a single action — typically a button click or a pipeline approval.

The key characteristics:

1. **Always deployable:** The main branch (or equivalent) is always in a state that could safely go to production. There are no "not yet integrated" features or untested changes lurking.

2. **Automated release pipeline:** Once a build passes CI, it flows automatically through further automated stages (deploy to staging, run integration/E2E tests, performance tests) without human intervention — until the final production approval.

3. **Human approval gate:** Before the production deployment, a human explicitly approves the release. This is the defining feature that separates Continuous Delivery from Continuous Deployment. The gate is a deliberate business decision, not a technical obstacle.

4. **Fast deployment:** Because the process is automated, once approved, the production deployment completes in minutes.

---

## The Human Approval Gate

The production approval gate is not a failure of automation — it is a deliberate design choice for organizations where:

- **Regulatory compliance** requires documented human sign-off before production changes (common in finance, healthcare, and government)
- **Business timing** matters — a retail company may not want to deploy at noon on Black Friday even if the code is perfect
- **Coordination** with other teams is required — the marketing team might need to publish a blog post at the same time a feature launches
- **Risk management** calls for a human to review what is about to be deployed and make an informed judgment

A well-designed CD pipeline makes the approval gate as informed as possible. The approver sees:
- Which commits and stories are included in this release
- Test coverage and quality gate results
- Performance test results compared to the previous release
- Deployment to staging succeeded X hours ago with no issues detected

With this information, the approval is a fast, confident decision — not a tense guessing game.

---

## Continuous Delivery vs. Continuous Deployment

These terms are frequently confused. The difference is precisely the presence or absence of the human approval gate:

| Dimension | Continuous Delivery | Continuous Deployment |
|---|---|---|
| Every passing build is… | …ready to deploy, awaiting approval | …automatically deployed to production |
| Human involvement | Required to trigger production deployment | Not required; humans are notified after deployment |
| Frequency | As often as the business decides | Every commit that passes all checks |
| Best for | Regulated industries, coordinated launches | SaaS products, web apps, high-velocity teams |
| Risk profile | Lower perceived risk; more human control | Requires mature testing and monitoring infrastructure |

Both are valid and both are significantly better than infrequent manual deployments. The choice depends on organizational context, regulatory requirements, and team maturity.

---

## The Release Pipeline vs. The Deployment Pipeline

These are related but distinct concepts:

### The Deployment Pipeline

The deployment pipeline is the automated sequence of stages that a code change travels through from commit to production. It includes:
- CI stages (build, test, analyze)
- Environment deployments (dev, staging, UAT)
- Automated validation at each environment (smoke tests, integration tests, performance tests)
- The approval gate
- Production deployment

The deployment pipeline is a technical artifact — the YAML configuration, Jenkins jobs, or GitHub Actions workflows that implement the automation.

### The Release Pipeline

The release pipeline is the business process that governs when and how software is released to users. It includes the deployment pipeline but also:
- Communication plans (when do we tell users about this feature?)
- Documentation updates (release notes, user guides)
- Marketing coordination
- Support team training
- Rollback plan and criteria

The release pipeline is a cross-functional concern. Product managers, technical writers, support leads, and engineering leads all participate.

---

## Environment Progression

A mature CD pipeline deploys through multiple environments in sequence, each serving a different purpose. Promoting a release from one environment to the next is only allowed if the previous environment's validation passes.

### Development / Integration Environment

**Purpose:** The first environment outside a developer's local machine. Used for continuous integration — every merge to the main branch deploys here automatically.

**Characteristics:**
- Unstable by design (updated on every commit)
- Uses synthetic or anonymized data
- Often runs with reduced capacity (smaller database, fewer replicas)
- Developers debug issues here

**Validation:** Smoke tests (does the application start? does the main happy path work?), integration tests

### Staging / Pre-Production Environment

**Purpose:** A production-equivalent environment for final validation before release. The goal is that "if it works in staging, it will work in production."

**Characteristics:**
- Configuration matches production as closely as possible (same database version, same secrets management, same infrastructure sizing — or a proportional fraction)
- Uses production-like data (anonymized copies of production data or realistic synthetic data)
- Access restricted — developers can view logs and metrics but not modify data directly

**Validation:** Full integration test suite, end-to-end tests, performance/load tests, manual exploratory testing (QA team), UAT (User Acceptance Testing) by business stakeholders

### UAT (User Acceptance Testing) Environment

**Purpose:** In organizations with formal acceptance testing, UAT is a separate environment where business stakeholders validate that the software meets requirements before approving production release.

**Who uses it:** Product owners, business analysts, selected end users (beta testers)

**Validation:** Manual testing against acceptance criteria defined in user stories

### Production

**Purpose:** The live environment serving real users.

**Characteristics:**
- Full capacity and redundancy
- Real data (protected by strict access controls)
- All changes go through the full pipeline before reaching here
- Monitored continuously

**Post-deployment validation:** Automated smoke tests run immediately after deployment, health checks, synthetic monitoring (automated scripts that simulate user journeys and alert if they fail)

---

## Versioned Release Artifacts

A **release artifact** is the deployable output of the build process. In CD, every artifact that passes CI is:

1. **Versioned:** Tagged with a unique, immutable identifier
2. **Stored in an artifact registry:** Published to a repository where it can be retrieved for deployment
3. **Immutable:** The artifact for version 1.2.3 never changes. If you need to fix a bug, you build a new artifact with a new version

### Versioning Strategies

**Semantic Versioning (SemVer):** `MAJOR.MINOR.PATCH` (e.g., `2.4.1`)
- MAJOR: Breaking changes
- MINOR: New features, backward compatible
- PATCH: Bug fixes, backward compatible

Used for: libraries, APIs, mobile apps — any software with external consumers who need to understand compatibility

**Build number / commit SHA:** `1.0.0-build-4721` or `1.0.0-abc123f`
- Unique per build; traces back to the exact commit

Used for: internal services where external consumers do not need to understand compatibility semantics

**Calendar versioning (CalVer):** `2024.06.15.1`
- Encodes the release date

Used for: infrastructure tools, some desktop applications

### Artifact Registries

| Artifact Type | Registry Options |
|---|---|
| Java JARs / Maven artifacts | Sonatype Nexus, JFrog Artifactory, GitHub Packages |
| Docker images | AWS ECR, Docker Hub, GitHub Container Registry, JFrog Artifactory |
| npm packages | npm Registry, GitHub Packages, JFrog Artifactory |
| Python packages | PyPI, JFrog Artifactory |
| Helm charts (Kubernetes) | JFrog Artifactory, AWS ECR (OCI artifacts), ChartMuseum |

### Why Immutable Artifacts Matter

If you rebuild the artifact at deployment time (compiling from source again at each environment), you cannot guarantee that what you tested in staging is exactly what you deployed to production. Build inputs (dependency versions, compiler versions, environment variables) could have changed. Immutable artifacts eliminate this uncertainty: you build once, test that artifact, and deploy that exact same artifact everywhere. What you tested is what users get.

---

## A Complete CD Pipeline: End to End

```
Developer pushes to main branch
              |
              v
[CI Pipeline]
  ├── Compile
  ├── Unit Tests
  ├── Static Analysis (SonarCloud)
  └── Build Docker Image → push to ECR with tag: 2.4.1-build-1547
              |
         (all pass)
              v
[Auto-deploy to Development Environment]
  ├── Deploy Docker image 2.4.1-build-1547
  ├── Run smoke tests
  └── Run integration tests
              |
         (all pass)
              v
[Auto-deploy to Staging Environment]
  ├── Deploy Docker image 2.4.1-build-1547
  ├── Run full E2E test suite
  ├── Run performance tests (compare to baseline)
  └── Notify QA team: "Staging is ready for review"
              |
    (manual testing + approval)
              v
[HUMAN APPROVAL GATE] ← Release Manager approves
  "Release 2.4.1-build-1547 to production"
              |
              v
[Production Deployment]
  ├── Deploy Docker image 2.4.1-build-1547 (same artifact)
  ├── Run smoke tests against production
  ├── Monitor error rates and latency for 15 minutes
  └── Alert on-call engineer if anomalies detected
              |
              v
[Release Complete]
  └── Notify stakeholders: "Version 2.4.1 is live"
```

---

## Summary

| Concept | Key Takeaway |
|---|---|
| Continuous Delivery | Every passing build is production-ready; human approves before deploying |
| vs. CI | CI validates code; CD ensures code is always in a deployable state |
| vs. CD (Deployment) | Delivery has a human gate; Deployment is fully automated to production |
| Approval gate | A deliberate business decision, not a technical bottleneck |
| Environment progression | dev → staging → UAT → prod; each stage validates before promoting |
| Versioned artifacts | Build once, store immutably, deploy the same artifact everywhere |

---

## External Resources

1. **Martin Fowler: Continuous Delivery vs. Continuous Deployment** — https://martinfowler.com/bliki/ContinuousDelivery.html
2. **Continuous Delivery (The Book) — Humble & Farley** — https://continuousdelivery.com/
3. **AWS CodePipeline (CD on AWS)** — https://docs.aws.amazon.com/codepipeline/latest/userguide/welcome.html
