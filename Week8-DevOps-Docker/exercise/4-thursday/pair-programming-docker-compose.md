# Pair Lab: Dockerize the Full Stack

**Duration:** ~2 hours
**Mode:** Pair Programming (Driver/Navigator — rotate every 25 minutes)

**Prerequisites:**
- Docker Desktop installed and running on both machines
- Both partners cloned from the same repository
- Java 17+ and Maven installed

**Roles:**
- **Partner A** — Spring Boot service owner
- **Partner B** — Database service owner

---

## Learning Objectives

By the end of this lab, both partners will be able to:

- Write a  that wires multiple services into a single deployable stack
- Configure inter-service dependencies using  with health check conditions
- Use named volumes to persist database data across container restarts
- Define custom Docker networks to isolate and connect services
- Add  instructions so Docker knows when a service is truly ready

---

## Scenario

Your team has been manually deploying Spring Boot + PostgreSQL on separate VMs. A new
requirement has arrived: **every developer must be able to spin up the entire stack with
ONE command.** Your pair's mission is to write the  that makes
 bring up Spring Boot, PostgreSQL, and Nginx — fully wired together,
persistent, and health-checked.

---

## Driver/Navigator Protocol

| Role | Responsibility |
|------|---------------|
| **Driver** | Types all code. Speaks their thinking aloud. |
| **Navigator** | Reviews every line. Looks up docs. Catches typos and logic errors. |

**Rules:**
- Rotate roles every 25 minutes — set a timer before starting each phase.
- Neither partner types while it is not their turn to drive.
- Both partners must understand and be able to explain every line before moving to the next phase.
- If you disagree on an approach, spend 60 seconds debating, then go with the navigator's call.

---

## Phase 1 — Partner A Drives: Spring Boot Service (25 min)

**Driver:** Partner A | **Navigator:** Partner B

Partner A writes the  service block inside :



### Partner A's Tasks

1. **Fill in the environment variables.**
   -  — hostname must equal the service name 
   -  and  — coordinate exact values with Partner B

2. **Add a healthcheck for the Spring Boot app.** Spring Boot Actuator exposes :
   

3. **Write a ** in the project root meeting these requirements:
   - Multi-stage:  stage compiles with Maven;  stage runs the JAR
   -  for builder;  for runtime
   - Non-root user (, UID 1001) in the runtime stage
   - Copy only the compiled JAR from the builder stage

> **Navigator (Partner B):** Watch for hardcoded passwords,  image tags, and missing .

### Partner A Checkpoint



Expected: build completes with no errors. Image appears in .

---

## Phase 2 — Partner B Drives: PostgreSQL Service (25 min)

**Driver:** Partner B | **Navigator:** Partner A

Partner B writes the  service block in the same :



### Partner B's Tasks

1. **Fill in all TODO items** — environment variables, volume mapping, and healthcheck.

2. **Declare the named volume** at the bottom of the file:
   

3. **Declare the  network** at the bottom of the file:
   

> **Navigator (Partner A):** Confirm POSTGRES_USER and POSTGRES_PASSWORD exactly match
> SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD. A mismatch here is the
> most common connection error in this lab.

### Partner B Checkpoint



Wait 30 seconds, then in a second terminal:



Expected: the  service shows . If it shows , wait another 15 seconds.

---

## Phase 3 — Together: Merge and Add Nginx (20 min)

Both partners review each other's sections, then add the Nginx reverse proxy together.

### Review Checklist (Navigator reads aloud, Driver confirms)

- [ ]  environment  uses hostname , not 
- [ ]  has 
- [ ]  healthcheck uses the same username as 
- [ ] Named volume  is declared under top-level 
- [ ]  network is declared under top-level 

### Add the Nginx Service



### Write  Together

Create  in the project root:



Uncomment and fill in the 4 proxy headers together before moving on.

### Together Checkpoint



Expected: all three services start. Nginx and app show ; db shows .

---

## Phase 4 — Integration Testing (15 min)

Both partners run each test together, agree on the expected output, and only advance when the test passes.



### Integration Checkpoint

All 5 commands must produce expected output before proceeding to Phase 5.

---

## Phase 5 — Persistence Test (10 min)

This test proves your named volume is working correctly.



### Why Data Persists

When you run  without , Docker removes containers but
**leaves named volumes intact on the host filesystem**. When PostgreSQL starts again it
mounts the same  volume and finds its data files exactly where it left them.

**What  does:** Removes the named volumes too, permanently
deleting all database data. Use only in CI or when you want a completely clean slate.
Never use it in production.

### Persistence Checkpoint

Your test record from Step 1 must appear in the Step 5 API response.

---

## Pair Retrospective (10 min)

Both partners write answers to these questions in a new file  in the project root.
Each partner writes their own perspective first, then read each other's answers.



---

## Definition of Done (Pair)

- [ ]  brings up all 3 services with no errors
- [ ]  returns HTTP 200 with 
- [ ] Data persists across  + 
- [ ]  is completed with both partners' answers
- [ ] Both partners can explain every line in the final  without notes

---

*Week 8 — Thursday | Docker Mastery Day | Pair Programming Lab*
