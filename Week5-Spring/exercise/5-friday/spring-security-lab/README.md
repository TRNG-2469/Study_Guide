# Challenge: Secure a Spring Boot API with Spring Security

> **Mode:** C — Hybrid (Design access rules first, then implement)
> **Estimated Time:** 3–4 hours
> **Reference:** `content/Week5-Spring/written/5-friday/introduction-to-spring-security.md`
> **Demo Reference:** `content/Week5-Spring/demos/5-friday/code/SecurityConfig.java`
> **Diagram Reference:** `content/Week5-Spring/demos/5-friday/diagrams/security-filter-chain.mermaid`

---

## Context

You have a working Spring Boot Book API. Right now it is completely open — anyone with the URL can read, modify, or delete every record. Before this API goes to production, it must be secured.

A library has three types of users:

| Role | Permissions |
|---|---|
| **MEMBER** | Read books (`GET`) |
| **LIBRARIAN** | Read + Create + Update books (`GET`, `POST`, `PUT`, `PATCH`) |
| **ADMIN** | Full access including delete (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) |

Your job: design the access control matrix, then implement it with Spring Security.

---

## Phase 1: Design the Access Control Matrix (30 min)

**Complete the Access Control worksheet in `templates/access-control-matrix.md` before writing any code.**

### Design Questions

**Question 1 — Endpoint Inventory**

List all 7 endpoints your `BookController` exposes. For each, specify which role(s) should have access:

| HTTP Method | URL | MEMBER | LIBRARIAN | ADMIN | Public (no auth) |
|---|---|---|---|---|---|
| GET | `/api/books` | | | | |
| GET | `/api/books/{id}` | | | | |
| GET | `/api/books/search` | | | | |
| POST | `/api/books` | | | | |
| PUT | `/api/books/{id}` | | | | |
| DELETE | `/api/books/{id}` | | | | |
| GET | `/actuator/health` | | | | |

**Question 2 — Password Storage**

You need to store user passwords. Your teammate suggests MD5 hashing because "it's fast." Respond to this suggestion — what is wrong with MD5 for password storage, and what should be used instead? (Reference `owasp-cryptographic-failures.md` from Thursday.)

**Question 3 — Session Strategy**

Your API will eventually be consumed by Angular (Week 7) using JWT tokens. Should you configure Spring Security to use stateful (server-side session) or stateless session management? Explain your reasoning.

---

## Phase 2: Implementation (90 min)

### Task 1 — Add the Dependency (5 min)

Add `spring-boot-starter-security` to `pom.xml`. Restart the app.

**Observe:** Hit any endpoint in Postman — what HTTP status do you get? What does this tell you about Spring Security's default behaviour?

Document in `REFLECTION.md`: "Before any configuration, Spring Security defaults to: ____"

---

### Task 2 — Configure the `PasswordEncoder` (10 min)

Create `SecurityConfig.java` in the `config` package.

Add a single `@Bean` method that returns a `BCryptPasswordEncoder`.

**Verify BCrypt in a scratch test:**
```java
PasswordEncoder encoder = new BCryptPasswordEncoder();
System.out.println(encoder.encode("password123"));  // run this twice
// Both outputs should be different (different salt each time)
System.out.println(encoder.matches("password123", /* paste one hash */));  // true
System.out.println(encoder.matches("wrongpass",   /* paste one hash */));  // false
```
Record both hashes and both match results in `REFLECTION.md`.

---

### Task 3 — Create In-Memory Users (20 min)

Add a `UserDetailsService` `@Bean` to `SecurityConfig` that creates three users — one per role:

| Username | Password (plaintext) | Role |
|---|---|---|
| `alice` | `member123` | `MEMBER` |
| `bob` | `librarian123` | `LIBRARIAN` |
| `carol` | `admin123` | `ADMIN` |

Requirements:
- Passwords must be **encoded with your `PasswordEncoder`** — never stored as plaintext.
- Use `InMemoryUserDetailsManager`.
- Use `User.builder()` to construct each user.

---

### Task 4 — Implement the `SecurityFilterChain` (40 min)

Add a `SecurityFilterChain` `@Bean` to `SecurityConfig`. Implement the access control rules from your **Phase 1 design matrix**.

Requirements:
- Disable CSRF (stateless REST API).
- `/actuator/health` must be public (no authentication required).
- Implement your role-based rules using `.requestMatchers(HttpMethod.X, "/path/**").hasRole("Y")`.
- All other requests require authentication.
- Session management must be `STATELESS`.
- Enable HTTP Basic Auth (`httpBasic(Customizer.withDefaults())`) for testing in Postman.

---

### Task 5 — Verify All Access Rules in Postman (25 min)

Test every combination using Postman's **Basic Auth** tab (Username + Password):

| Request | Auth | Expected Status | Reason |
|---|---|---|---|
| GET `/api/books` | alice / member123 | 200 | MEMBER can read |
| GET `/api/books` | no auth | 401 | Not authenticated |
| POST `/api/books` | alice / member123 | 403 | MEMBER cannot create |
| POST `/api/books` | bob / librarian123 | 201 | LIBRARIAN can create |
| DELETE `/api/books/1` | bob / librarian123 | 403 | LIBRARIAN cannot delete |
| DELETE `/api/books/1` | carol / admin123 | 204 | ADMIN can delete |
| GET `/actuator/health` | no auth | 200 | Public endpoint |

Document each actual result in `REFLECTION.md`. For any result that differs from expected, investigate and fix.

---

## Phase 3: Reflection and Analysis (20 min)

Answer in `REFLECTION.md`:

1. **The Filter Chain:** Draw or describe what happens step-by-step when `alice` sends `POST /api/books`. Trace from the HTTP request arriving at the app to the 403 response being returned. Reference the `security-filter-chain.mermaid` diagram.

2. **Secure by Default:** When you first added `spring-boot-starter-security` with no configuration, all endpoints returned `401`. Explain why this "secure by default" philosophy is better than "open by default." What would the risk be if Spring Security required you to explicitly lock down every endpoint?

3. **HTTP Basic vs JWT:** This exercise uses HTTP Basic Authentication (credentials sent with every request). Describe one disadvantage of HTTP Basic Auth that JWT tokens solve. (We will implement JWT in a future lesson — this is a forward-thinking question.)

---

## Definition of Done

- [ ] `templates/access-control-matrix.md` completed with all design questions answered
- [ ] `SecurityConfig` has `PasswordEncoder`, `UserDetailsService`, and `SecurityFilterChain` beans
- [ ] Passwords are BCrypt-encoded (not plaintext in `UserDetailsService`)
- [ ] CSRF is disabled
- [ ] Session management is `STATELESS`
- [ ] `/actuator/health` returns 200 with no authentication
- [ ] All 7 Postman access-control tests produce the expected status codes
- [ ] `REFLECTION.md` answers all 3 Phase 3 questions and includes Postman test results

---

## Stretch Goal

Add a fourth user `dave` with role `GUEST` who can only read the book list (`GET /api/books`) — not individual books (`GET /api/books/{id}`). Implement and test this restriction. Hint: you will need an additional `.requestMatchers` rule.
