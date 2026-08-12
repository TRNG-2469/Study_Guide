# Access Control Matrix

> Complete this **before** writing any code (Phase 1).
> Reference: `introduction-to-spring-security.md` and `security-filter-chain.mermaid`

---

## Question 1 — Endpoint Access Matrix

Mark each cell with ✅ (allowed) or ❌ (denied). The `Public` column means no authentication required.

| HTTP Method | URL | MEMBER | LIBRARIAN | ADMIN | Public |
|---|---|---|---|---|---|
| GET | `/api/books` | | | | |
| GET | `/api/books/{id}` | | | | |
| GET | `/api/books/search` | | | | |
| POST | `/api/books` | | | | |
| PUT | `/api/books/{id}` | | | | |
| DELETE | `/api/books/{id}` | | | | |
| GET | `/actuator/health` | | | | |

---

## Question 2 — Password Storage

> Your teammate suggests MD5 hashing. What is your response?

**What is wrong with MD5 for password storage:**

[Write your answer here]

**What should be used instead, and why:**

[Write your answer here]

---

## Question 3 — Session Strategy

> Should this API use stateful or stateless session management? Why?

**My recommendation:**

[Write your answer here]

**Reason:**

[Write your answer here]

---

## My Implementation Plan

> After completing the questions, outline the `.authorizeHttpRequests` rules you plan to write.

```java
// Example structure — fill in the actual rules:
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health").permitAll()
    // TODO: Add your rules here
    .anyRequest().authenticated()
)
```
