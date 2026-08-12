# Challenge: Configure and Debug CORS

> **Mode:** C — Hybrid (Design first, then implement)
> **Estimated Time:** 2.5–3 hours
> **Reference:** `content/Week5-Spring/written/5-friday/cors-in-spring-boot.md`
> **Demo Reference:** `content/Week5-Spring/demos/5-friday/code/WebConfig.java`
> **Diagram Reference:** `content/Week5-Spring/demos/5-friday/diagrams/cors-preflight-flow.mermaid`

---

## Context

You have a working Spring Boot API running on port `8080`. In Week 7 you will build an Angular frontend that will run on port `4200` and call this API. Before that connection can work, CORS must be configured correctly.

But there is a subtlety: different environments need different CORS policies. The development environment is permissive (you trust `localhost:4200`). The production environment is strict (only your deployed domain is allowed). A misconfigured policy in either direction causes real problems.

---

## Phase 1: Design (45 min)

**Complete the CORS Policy Design worksheet in `templates/cors-policy-design.md` before writing any code.**

### Scenario

Your API will be deployed with two environments:

| Environment | Frontend URL | API URL |
|---|---|---|
| Development | `http://localhost:4200` | `http://localhost:8080` |
| Production | `https://library-app.com` | `https://api.library-app.com` |

### Design Questions (fill in the template)

**Question 1 — Origin Analysis**

For each request below, determine if it is cross-origin relative to `http://localhost:8080`. Explain your reasoning.

| Request From | Cross-Origin? | Reason |
|---|---|---|
| `http://localhost:4200` | ? | |
| `http://localhost:8080/dashboard` | ? | |
| `https://localhost:8080` | ? | |
| `https://library-app.com` | ? | |
| `http://localhost:4200/books` | ? | |

**Question 2 — Preflight Trigger**

Which of the following requests will trigger a preflight `OPTIONS` request? Explain why or why not for each.

| Request | Triggers Preflight? | Reason |
|---|---|---|
| `GET /api/books` (no custom headers) | ? | |
| `DELETE /api/books/1` | ? | |
| `POST /api/books` with `Content-Type: application/json` | ? | |
| `GET /api/books` with `Authorization: Bearer token` | ? | |

**Question 3 — Policy Matrix**

Fill in the recommended CORS policy for each environment:

| Setting | Development | Production |
|---|---|---|
| `allowedOrigins` | ? | ? |
| `allowedMethods` | ? | ? |
| `allowedHeaders` | ? | ? |
| `allowCredentials` | ? | ? |
| `maxAge` | ? | ? |

**Question 4 — The Security Trade-off**

Your teammate suggests using `allowedOrigins("*")` in both environments to "keep it simple." Write 2–3 sentences explaining why this is acceptable for one environment but a security risk for the other. What specific attack does explicit origin restriction prevent?

---

## Phase 2: Implementation (60 min)

Now implement the CORS policy you designed.

### Task 1 — Global CORS Configuration (20 min)

Create `WebConfig.java` in the `config` package implementing `WebMvcConfigurer`. Apply your **development** CORS policy from your design worksheet.

Requirements:
- Use `addCorsMappings` (not `@CrossOrigin` on controllers).
- Apply the policy to `/api/**` only — not to `/actuator/**`.
- Do **not** use `allowedOrigins("*")`.

### Task 2 — Profile-Based CORS (25 min)

Your `WebConfig` currently hard-codes the dev origins. Make it environment-aware using Spring Profiles.

1. Split into two `@Configuration` classes:
   - `DevWebConfig` — annotated with `@Profile("dev")`, permissive policy for `localhost:4200`
   - `ProdWebConfig` — annotated with `@Profile("prod")`, strict policy for `https://library-app.com`

2. Add to `application.yml`:
   ```yaml
   spring:
     profiles:
       active: dev
   ```

3. Verify the active profile is logged at startup:
   ```
   The following 1 profile is active: "dev"
   ```

### Task 3 — The Live CORS Test (15 min)

Open your browser's Developer Tools (F12) → Console tab. With the app running:

**Test A — from the correct origin:**
```javascript
// Simulate a request from localhost:4200 (use a browser extension or DevTools override)
fetch('http://localhost:8080/api/books')
  .then(r => r.json())
  .then(console.log)
```
→ Expected: book data in the console.

**Test B — from a blocked origin:**
Temporarily change `allowedOrigins` in `DevWebConfig` to `"https://someothersite.com"`. Restart. Run the same `fetch()`.
→ Expected: CORS error in console.

Document both results in `REFLECTION.md` with console screenshots or copy-pasted error text.

---

## Phase 3: Debug Scenario (30 min)

Your teammate has pushed a broken `WebConfig`. Open `starter_code/BrokenWebConfig.java`. It contains **3 CORS bugs**.

1. Identify all 3 bugs — describe each in `REFLECTION.md`.
2. Explain what symptom each bug would cause (CORS error? startup failure? wrong status code?).
3. Fix them. Rename the fixed file `FixedWebConfig.java`.

---

## Definition of Done

- [ ] `templates/cors-policy-design.md` completed with all 4 design questions answered
- [ ] `WebConfig.java` (or `DevWebConfig` + `ProdWebConfig`) implements the designed policy
- [ ] CORS correctly blocks requests from unlisted origins
- [ ] CORS correctly allows requests from `http://localhost:4200`
- [ ] `@Profile("dev")` and `@Profile("prod")` split is implemented
- [ ] `REFLECTION.md` includes live test results (Test A and Test B) with console output
- [ ] All 3 bugs in `BrokenWebConfig.java` are identified, explained, and fixed

---

## Stretch Goal

Add a custom `CorsFilter` bean (using `FilterRegistrationBean<CorsFilter>`) as an alternative to `WebMvcConfigurer`. Research why this approach is needed when Spring Security is present — document your findings in `REFLECTION.md`.
