# CORS Policy Design Worksheet

> Complete this **before** writing any code (Phase 1).
> Reference: `cors-in-spring-boot.md` and `cors-preflight-flow.mermaid`

---

## Question 1 — Origin Analysis

For each request origin below, determine if it is cross-origin relative to `http://localhost:8080`.
An origin = protocol + host + port. All three must match.

| Request From | Cross-Origin? (Yes/No) | Reason |
|---|---|---|
| `http://localhost:4200` | | |
| `http://localhost:8080/dashboard` | | |
| `https://localhost:8080` | | |
| `https://library-app.com` | | |
| `http://localhost:4200/books` | | |

---

## Question 2 — Preflight Trigger

Which requests will trigger a preflight `OPTIONS` request?
(Hint: simple = GET/POST with no custom headers and no JSON body. Non-simple = anything else.)

| Request | Triggers Preflight? (Yes/No) | Reason |
|---|---|---|
| `GET /api/books` (no custom headers) | | |
| `DELETE /api/books/1` | | |
| `POST /api/books` with `Content-Type: application/json` | | |
| `GET /api/books` with `Authorization: Bearer token` | | |

---

## Question 3 — CORS Policy Matrix

Fill in your recommended settings for each environment.

| Setting | Development | Production |
|---|---|---|
| `allowedOrigins` | | |
| `allowedMethods` | | |
| `allowedHeaders` | | |
| `allowCredentials` | | |
| `maxAge` | | |

---

## Question 4 — The Security Trade-off

> Your teammate suggests using `allowedOrigins("*")` in both environments.
> Write 2–3 sentences explaining the security risk.

**Your answer:**

[Write your response here]

---

## My Design Decisions

> After completing the questions, summarize the key decisions you made and why.

[Write your summary here]
