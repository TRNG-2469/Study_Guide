# Introduction to Spring Security

## Learning Objectives

By the end of this reading you will be able to:

- Explain the purpose of Spring Security and what it protects against.
- Describe the Security Filter Chain and its role in processing HTTP requests.
- Configure a basic `SecurityFilterChain` bean using `HttpSecurity`.
- Define which routes are public and which require authentication.
- Encode passwords with `BCryptPasswordEncoder`.
- Set up in-memory user details for learning and testing purposes.
- Understand what JWT-based authentication looks like at a high level (covered fully in a future week).

---

## Why This Matters

Every REST API you build will eventually need to answer two fundamental security questions:

1. **Authentication** — *Who are you?* (Are you a registered user?)
2. **Authorization** — *What are you allowed to do?* (Can this user access this resource?)

Without security, your API is fully open: anyone who discovers the URL can read, modify, or delete your data. In Week 4 you addressed this in Javalin using a manual `before()` handler that checked a header token. Spring Security provides a far more powerful, configurable, and battle-tested framework for the same goal.

Spring Security is also directly related to the OWASP vulnerabilities you have studied this week:
- **A01 Broken Access Control** (Tuesday) — Spring Security enforces authorization rules consistently.
- **A02 Cryptographic Failures** (Thursday) — Spring Security provides `BCryptPasswordEncoder` so passwords are never stored in plaintext.

This reading gives you the **foundational knowledge** to understand Spring Security. Authentication strategies — particularly JWT-based stateless authentication — will be covered in full detail when we return to security topics in a future week.

---

## The Concept

### What Is Spring Security?

Spring Security is a powerful, customizable security framework for Java applications. It operates primarily through a **chain of Servlet Filters** — components that intercept every HTTP request before it reaches your controllers.

When you add `spring-boot-starter-security` to your project, Spring Boot automatically activates this chain. Without any configuration, Spring Security **locks down the entire application** — every endpoint requires authentication. This "secure by default" philosophy is intentional: it forces you to explicitly declare what is public.

---

### The Security Filter Chain

The Security Filter Chain is a sequence of filters, each performing one security responsibility:

```
HTTP Request
    │
    ▼
SecurityFilterChain
    │
    ├─► UsernamePasswordAuthenticationFilter (handles login form)
    ├─► BearerTokenAuthenticationFilter (handles JWT tokens — future topic)
    ├─► ExceptionTranslationFilter (converts auth exceptions to 401/403 responses)
    ├─► FilterSecurityInterceptor (checks authorization rules)
    │
    ▼
DispatcherServlet → Your @RestController
```

When any filter in the chain rejects a request (unauthenticated, or unauthorized), the request never reaches your controller. Your business logic is never exposed to unauthenticated access.

---

### Adding Spring Security to Your Project

Add the starter dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

As soon as this is on the classpath, Spring Boot:
1. Secures all endpoints — every request requires authentication.
2. Generates a random password at startup (printed to the console) for the default user named `user`.
3. Provides a basic login form at `/login`.

You will see in your application logs:

```
Using generated security password: 3a8e4f2b-5c91-4d2a-b7f3-1e6a9d0c8b12
```

This is enough to get started but is obviously not production behaviour. You will replace this with your own configuration immediately.

---

### Configuring `SecurityFilterChain`

In modern Spring Security (6.x, which Spring Boot 3.x uses), you configure security by declaring a `SecurityFilterChain` bean in a `@Configuration` class. The old approach of extending `WebSecurityConfigurerAdapter` is **deprecated and removed** — do not use it.

```java
package com.example.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST APIs (stateless — no browser sessions)
            .csrf(csrf -> csrf.disable())

            // Define authorization rules
            .authorizeHttpRequests(auth -> auth
                // Permit public endpoints — no authentication needed
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Use stateless sessions — no server-side session storage
            // (Required for JWT-based authentication)
            .sessionManagement(session ->
                session.sessionCreationPolicy(STATELESS)
            );

        return http.build();
    }
}
```

#### Key `HttpSecurity` Configuration Points

| Configuration | What it does |
|---|---|
| `.csrf(csrf -> csrf.disable())` | Disables CSRF protection — appropriate for stateless REST APIs (not for session-based apps) |
| `.authorizeHttpRequests(...)` | Defines which endpoints require authentication |
| `.requestMatchers("/path/**").permitAll()` | Makes a URL pattern fully public |
| `.anyRequest().authenticated()` | All other routes require an authenticated user |
| `.sessionManagement(STATELESS)` | Tells Spring Security not to create or use HTTP sessions |

> **Why disable CSRF for REST APIs?** CSRF (Cross-Site Request Forgery) attacks exploit browser session cookies. A stateless REST API using JWT tokens is not vulnerable to CSRF, so the protection is unnecessary overhead. If your API uses session-based authentication, keep CSRF enabled.

---

### Password Encoding with `BCryptPasswordEncoder`

Passwords must **never** be stored in plaintext in a database. Spring Security provides the `PasswordEncoder` abstraction, with `BCryptPasswordEncoder` as the recommended implementation.

BCrypt is a strong, adaptive hashing algorithm that:
- Automatically salts passwords (preventing rainbow table attacks).
- Has a configurable work factor (cost) that can be increased as hardware gets faster.
- Is one-way — you cannot "decrypt" a BCrypt hash back to the original password.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Declare the PasswordEncoder as a Spring bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // default strength = 10
    }

    // ... securityFilterChain
}
```

To encode a password (when creating a user):

```java
@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public User register(String username, String rawPassword) {
        String encoded = passwordEncoder.encode(rawPassword);
        // stored: "$2a$10$EBlZqNotypKk6Vc5.../..." — never the raw password
        return userRepository.save(new User(username, encoded));
    }
}
```

To verify a password (during login):

```java
// rawPassword = "mySecret123"
// storedHash  = "$2a$10$EBlZqNotypKk..."
boolean matches = passwordEncoder.matches(rawPassword, storedHash);
// true if the raw password hashes to match the stored hash
```

---

### In-Memory User Details (For Learning)

During development and learning, you can define users in memory without a database. This is useful for testing your security configuration quickly:

```java
@Bean
public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    UserDetails admin = User.builder()
            .username("admin")
            .password(encoder.encode("password123"))
            .roles("ADMIN")
            .build();

    UserDetails trainee = User.builder()
            .username("trainee")
            .password(encoder.encode("trainee123"))
            .roles("USER")
            .build();

    return new InMemoryUserDetailsManager(admin, trainee);
}
```

> **Important:** In-memory user details are for **learning and testing only**. Real applications load users from a database via a custom `UserDetailsService` that queries your `UserRepository`. We will implement database-backed authentication in a future lesson.

---

### Role-Based Authorization

Once users have roles, you can restrict access to specific endpoints based on those roles:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("USER", "ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

This configuration means:
- Anyone can hit `/api/auth/**` (login/register endpoints).
- Any authenticated user (USER or ADMIN) can read products.
- Only ADMIN users can create or delete products.

---

### What Comes Next — JWT Authentication

The configuration above uses **HTTP Basic Authentication** by default — the browser pops up a login dialog, and credentials are sent with every request in a Base64-encoded header. This is appropriate for learning the security fundamentals but is not suitable for modern REST APIs consumed by SPAs (Single Page Applications like Angular).

Modern REST APIs use **JWT (JSON Web Tokens)** for stateless authentication:
1. The client sends credentials to a `/login` endpoint.
2. The server validates them and returns a signed JWT token.
3. The client includes the token in every subsequent request: `Authorization: Bearer <token>`.
4. Spring Security validates the token on every request — no session storage needed.

We will implement full JWT-based authentication in a dedicated future lesson. The `SecurityFilterChain` you have learned to configure today is the **exact same starting point** for JWT configuration — you will add a JWT filter to the chain.

---

### A Complete Security Configuration (Learning Baseline)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().hasRole("ADMIN")
            )
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .httpBasic(Customizer.withDefaults());  // Enable HTTP Basic Auth for testing

        return http.build();
    }
}
```

---

## Summary

| Concept | Key Point |
|---|---|
| Spring Security | A servlet-filter-based security framework that intercepts every HTTP request |
| Security Filter Chain | Ordered sequence of filters for authentication and authorization |
| `SecurityFilterChain` bean | The modern (Spring Security 6) way to configure security — replaces `WebSecurityConfigurerAdapter` |
| `.csrf(csrf -> csrf.disable())` | Disable CSRF for stateless REST APIs |
| `.authorizeHttpRequests(...)` | Define which routes are public (`.permitAll()`) and which require auth (`.authenticated()`) |
| `BCryptPasswordEncoder` | The standard password hasher in Spring Security — always use it, never store plaintext passwords |
| `InMemoryUserDetailsManager` | In-memory user store for learning/testing — replace with database-backed service in production |
| Role-based authorization | Restrict endpoints by role using `.hasRole()` / `.hasAnyRole()` |
| JWT authentication | Stateless token-based auth — the production standard, covered in full in a future lesson |

Spring Security is a large topic. Today you have the conceptual foundation: the filter chain, how to configure which routes are protected, and why password hashing matters. Building on this foundation, you will implement real authentication with database-backed users and JWT tokens in a future dedicated security week.

---

## Additional Resources

- [Spring Security Reference Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Security Architecture — Spring Blog](https://spring.io/guides/topicals/spring-security-architecture/)
- [BCrypt Password Encoding — Baeldung](https://www.baeldung.com/spring-security-registration-password-encoding-bcrypt)
