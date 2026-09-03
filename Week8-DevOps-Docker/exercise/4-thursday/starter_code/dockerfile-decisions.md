# Dockerfile Decisions — Documentation Template

**Name:**
**Date:**
**Project:** Spring Boot Application — Week 8 Docker Mastery Challenge

---

## Instruction Decision Table

Complete every row. Write in the "Why You Chose This" column in your own words —
explain the reasoning, not just a restatement of what the instruction does.

| Instruction | Your Implementation | Why You Chose This |
|---|---|---|
| FROM (builder stage) | | |
| FROM (runtime stage) | | |
| WORKDIR (builder) | | |
| WORKDIR (runtime) | | |
| COPY pom.xml + dependency cache | | |
| RUN mvn package | | |
| RUN adduser / addgroup | | |
| COPY --from=builder --chown | | |
| ENV JAVA_OPTS | | |
| EXPOSE | | |
| HEALTHCHECK | | |
| USER | | |
| ENTRYPOINT | | |

---

## .dockerignore Decisions

List each pattern you added to `.dockerignore` and explain why it must be excluded:

| Pattern | Why Excluded |
|---|---|
| target/ | |
| .git/ | |
| *.log | |
| .idea/ | |
| *.iml | |
| (add more rows as needed) | |

---

## Image Size Analysis

Record your image sizes at each stage of optimization:

| Stage | Base Image | Image Size | Notes |
|---|---|---|---|
| Naive single-stage (if you tried it) | | | |
| Multi-stage with JDK runtime | | | |
| Multi-stage with JRE Alpine runtime | | | |

**Target:** Final image must be under 300MB.

---

## Security Findings

Results from `docker scout cves` or manual image inspection:

| Finding | Severity | CVE ID (if known) | Remediation |
|---|---|---|---|
| | | | |
| | | | |

If no vulnerabilities were found, write "No critical or high CVEs found in scan on [date]."

---

## Reflection

**Biggest challenge I faced:**

*(Write 2–3 sentences describing what took the most time or caused the most confusion.)*

**What I would do differently next time:**

*(Write 2–3 sentences. Consider: dependency caching, base image choice, layer ordering.)*

**Reflection Question 1 — Alpine vs Debian tradeoff:**

*(Answer in 3–5 sentences.)*

**Reflection Question 2 — Dependency cache invalidation:**

*(Answer in 3–5 sentences.)*

**Reflection Question 3 — UseContainerSupport JVM flag:**

*(Answer in 3–5 sentences.)*

**Questions I still have:**

1.
2.
3.

---

## Submission

- [ ] All table rows completed
- [ ] Image size recorded and confirmed under 300MB
- [ ] Security findings documented
- [ ] Reflection questions answered
- [ ] Docker Hub URL: `https://hub.docker.com/r/<your-username>/week8-springboot`

---

*Week 8 — Thursday | Docker Mastery Day | Solo Challenge*
