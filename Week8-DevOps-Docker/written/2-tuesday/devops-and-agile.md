# DevOps and Agile

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain the relationship between Agile and DevOps
- Describe how they differ in scope and focus
- Show how sprint cadence aligns with CI/CD pipelines
- Define DevSecOps and explain why security is embedded in the pipeline
- Recognize how Agile and DevOps together create a complete software delivery system

---

## Why This Matters

Many organizations adopt Agile for their development process and then wonder why software still takes months to reach users. Others invest heavily in DevOps tooling without organizing their teams or planning processes effectively. Agile and DevOps are complementary — not competing — frameworks, and understanding how they work together explains the full picture of modern software delivery. As a developer, you will work inside both frameworks simultaneously every day.

---

## Agile: The Development Philosophy

Agile is a philosophy for how software development teams plan, organize, and execute their work. It emerged from the **Agile Manifesto** (2001), which was a reaction to the failures of "waterfall" development — a sequential process where requirements were defined, then designed, then built, then tested, then deployed, often over a period of years, only for the result to miss what users actually needed.

The four core values of the Agile Manifesto:
1. **Individuals and interactions** over processes and tools
2. **Working software** over comprehensive documentation
3. **Customer collaboration** over contract negotiation
4. **Responding to change** over following a plan

### What Agile Covers

Agile frameworks (Scrum, Kanban, SAFe) govern:
- How requirements are gathered and prioritized (Product Backlog, User Stories)
- How work is organized into time-boxed iterations (Sprints, typically 1–2 weeks)
- How teams communicate (Daily Standups, Sprint Reviews, Retrospectives)
- How scope is managed (only the highest-priority items are worked on first)
- How feedback is collected from stakeholders (Sprint Demos)

### What Agile Does NOT Cover

Agile is largely silent on what happens after a developer commits their code. It does not specify:
- How code is built and tested automatically
- How it is deployed to servers
- How the running application is monitored
- How incidents are handled in production

This gap is where DevOps lives.

---

## DevOps: The End-to-End Delivery Philosophy

DevOps extends the scope of responsibility beyond the development team's "done" to the application's full lifecycle in production. If Agile answers "how do we build the right thing efficiently?", DevOps answers "how do we get what we built to users reliably and continuously?"

DevOps covers:
- Continuous Integration (automated build and test on every commit)
- Continuous Delivery/Deployment (automated pipeline from code to production)
- Infrastructure as Code (reproducible, version-controlled infrastructure)
- Monitoring and Observability (understanding production behavior)
- Incident Management (detecting and recovering from failures quickly)

---

## How Agile and DevOps Fit Together

Think of Agile and DevOps as two gears that mesh:

```
AGILE                               DEVOPS
------                              ------
Product Vision                          |
    |                                   |
Backlog Refinement                      |
    |                                   |
Sprint Planning                         |
    |                                   |
Daily Development ──── commit ──→ [CI Pipeline: Build + Test]
    |                                       |
Sprint Review ◄────── feedback ────── [Staging Deploy]
    |                                       |
Retrospective                    [Production Deploy]
    |                                       |
Next Sprint Planning ◄── monitoring data ── [Monitor]
```

The developer completes a user story during a sprint. When they push their code, the DevOps pipeline immediately validates it (build, test, scan). If the pipeline passes, the code moves toward production without manual handoffs. By the Sprint Review, the story is already live in a staging or production environment — not "done in development but waiting for the ops team to deploy it."

### Sprint Cadence and CI/CD Alignment

In a well-functioning organization, the sprint is not the deployment cadence. Deployments happen continuously — potentially dozens of times per sprint — every time a developer pushes a change that passes all automated checks. The sprint cadence governs:
- When the team plans and commits to new work
- When stakeholders review progress
- When the team reflects and adjusts

The deployment cadence is governed by CI/CD: code ships when it is ready, not when the sprint ends.

This is a crucial difference from waterfall and even early Agile implementations where "done" meant "ready for a release at the end of the sprint."

---

## Agile vs. DevOps: Side-by-Side

| Dimension | Agile | DevOps |
|---|---|---|
| Primary focus | How teams develop software | How software reaches and runs in production |
| Scope | Planning through code completion | Code completion through production monitoring |
| Team | Development team | Development + Operations + Security + QA |
| Cadence | Sprint (1–2 week iterations) | Continuous (every commit triggers automation) |
| Artifact | Working software at end of sprint | Deployed, running software in production |
| Feedback source | Product Owner, stakeholders, users | Monitoring, alerts, user analytics |
| Key practices | Scrum/Kanban, User Stories, Retrospectives | CI/CD, IaC, Observability, On-call |

---

## DevSecOps: Embedding Security in the Pipeline

### The Traditional Security Problem

In traditional organizations, security review was a gate at the end of the development process. The sequence was:

```
Build → Test → [Wait months] → Security Audit → [Fail/Pass] → Deploy
```

This created several problems:
- Security issues discovered late are enormously expensive to fix (the code is already written, tested, and integrated)
- Security teams became bottlenecks, blocking releases
- Developers had no visibility into security standards while writing code
- The adversarial relationship between Development and Security mirrored the Dev/Ops divide

### DevSecOps: Security as a First-Class Citizen

**DevSecOps** (sometimes called "Secure DevOps" or "Security in the Pipeline") extends the DevOps philosophy to include Security teams and security practices throughout the entire software lifecycle — not just at the end.

The principle: **"Shift security left"** — move security checks earlier in the development process, ideally to the moment code is written.

### Security Gates in a DevSecOps Pipeline

A DevSecOps pipeline includes automated security checks at every stage:

```
Developer writes code
        |
        v
[IDE Plugin: SonarLint]  ← Catches issues as code is typed
        |
Developer commits to Git
        |
        v
[Pre-commit hooks: secret scanning] ← Prevents credentials from being committed
        |
CI Pipeline triggered
        |
        v
[SAST: Static Application Security Testing]
  e.g., SonarCloud, Checkmarx, Semgrep
  Scans source code for security vulnerabilities
        |
        v
[Dependency Scanning: SCA (Software Composition Analysis)]
  e.g., Snyk, OWASP Dependency-Check
  Identifies known CVEs in third-party libraries
        |
        v
[Container Scanning]
  e.g., Trivy, AWS ECR scanning
  Scans Docker images for OS and package vulnerabilities
        |
        v
[DAST: Dynamic Application Security Testing]
  e.g., OWASP ZAP
  Tests running application for vulnerabilities (SQL injection, XSS, etc.)
        |
        v
Deploy (if all gates pass)
        |
        v
[Runtime Security Monitoring]
  e.g., AWS GuardDuty, Falco
  Monitors running systems for anomalous behavior
```

### Key DevSecOps Practices

**1. Secrets Management**
Never commit secrets (API keys, passwords, database credentials) to version control. Use:
- Environment variables injected at runtime
- AWS Secrets Manager or HashiCorp Vault for centralized secret storage
- Pre-commit hooks to detect accidentally committed secrets (tools: git-secrets, truffleHog)

**2. Least Privilege IAM Policies**
Every application should run with the minimum permissions required for its function. A service that only reads from an S3 bucket should have read-only S3 access — never full Administrator access.

**3. Dependency Updates**
Keep third-party libraries current. Tools like Dependabot (GitHub) or Renovate Bot automatically create pull requests to update dependencies when new versions are released, keeping your supply chain secure.

**4. Security as Code**
Security policies, firewall rules, and access control lists are defined in code (Terraform, CloudFormation) and stored in version control — auditable, reviewable, and reproducible.

### The Three Ways of DevOps (Gene Kim's Framework)

Gene Kim, one of the founders of the DevOps movement, describes DevOps through "The Three Ways":

**The First Way: Flow**
Optimize the flow of work from Development through Operations to the customer. Reduce handoffs, eliminate waste, automate repetitive work. The goal is fast, smooth delivery.

**The Second Way: Feedback**
Create fast, amplified feedback loops so problems are detected and corrected immediately. Monitoring, automated testing, and sprint reviews are all feedback mechanisms.

**The Third Way: Continual Learning**
Foster a culture of experimentation and learning from failure. Blameless post-mortems, knowledge sharing, and dedicating time to improvement rather than just feature delivery.

DevSecOps adds: **embed security within The First Way** — security must flow with the work, not be a separate gate that stops flow.

---

## A Practical Example: One User Story's Journey

Let's trace a single user story from planning to production in an Agile + DevOps + DevSecOps organization:

1. **Sprint Planning (Agile):** The team commits to implementing a user profile edit feature.

2. **Development (Agile):** A developer writes the Java controller, service, and repository layers, along with unit and integration tests.

3. **Commit (DevOps/DevSecOps):** The developer pushes to a feature branch. Pre-commit hooks scan for secrets. The CI pipeline triggers automatically.

4. **CI Pipeline (DevOps/DevSecOps):**
   - Maven builds the JAR
   - JUnit runs 847 unit and integration tests (all pass)
   - SonarCloud scans for bugs, vulnerabilities, and code smells — flags a potential SQL injection in a search parameter
   - The developer fixes the vulnerability
   - CI re-runs and passes

5. **Code Review (Agile):** A peer reviewer approves the pull request after reviewing the code and seeing the CI pipeline pass.

6. **Merge and Deploy to Staging (DevOps):** The PR is merged to main. The CD pipeline automatically deploys to the staging environment.

7. **Sprint Review (Agile):** The Product Owner demos the feature on the staging environment. Feedback: "Can we also show the last-modified date?" — added to the backlog.

8. **Production Deployment (DevOps):** The team lead approves the staging build for production. CD pipeline deploys automatically.

9. **Monitoring (DevOps):** CloudWatch dashboards show the feature is used 200 times in the first hour. No errors. Latency is within SLA.

10. **Next Sprint (Agile):** The "last-modified date" feedback becomes a story in the next sprint backlog.

The whole journey from code commit to production took under two hours. In a pre-DevOps organization, the same change might take two weeks to schedule through the Operations change management process.

---

## Summary

| Concept | Key Takeaway |
|---|---|
| Agile scope | Planning through code completion; team ceremonies and iteration |
| DevOps scope | Code completion through production monitoring; automation and operations |
| How they fit | Agile governs how teams work; DevOps governs how software reaches users |
| Sprint vs. CI/CD cadence | Sprints structure planning; deployments happen continuously |
| DevSecOps | Security embedded at every pipeline stage; "shift left" security |
| The Three Ways | Flow, Feedback, and Continual Learning — the philosophical foundation |

---

## External Resources

1. **Agile Manifesto** — https://agilemanifesto.org/
2. **DevSecOps — OWASP Overview** — https://owasp.org/www-project-devsecops-guideline/
3. **The Three Ways: The Principles Underpinning DevOps (Gene Kim)** — https://itrevolution.com/articles/the-three-ways-principles-underpinning-devops/
