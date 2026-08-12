# CORS in Spring Boot

## Learning Objectives

By the end of this reading you will be able to:

- Explain what CORS is and why browsers enforce it.
- Identify the CORS error when it occurs in a browser or Postman.
- Enable CORS for a single endpoint using `@CrossOrigin`.
- Configure CORS globally for all controllers using `WebMvcConfigurer`.
- Understand preflight requests and how Spring handles them.
- Recognize the security trade-offs of permissive vs restrictive CORS policies.

---

## Why This Matters

You have just built a Spring Boot REST API. You have tested every endpoint in Postman — they all work perfectly. You then connect your Angular (or React) frontend running at `http://localhost:4200` to fetch data from your API at `http://localhost:8080`, and immediately see this in the browser console:

```
Access to XMLHttpRequest at 'http://localhost:8080/api/products' from origin
'http://localhost:4200' has been blocked by CORS policy: No
'Access-Control-Allow-Origin' header is present on the requested resource.
```

This is the most common first error new full-stack developers encounter. It is not a bug in your code — it is the browser's **Same-Origin Policy** doing its job. Understanding and properly configuring CORS is a foundational skill for anyone building REST APIs consumed by a web frontend.

In Week 7, when you build your Angular application, your frontend will run on a different origin than your Spring Boot backend. You will need CORS configured correctly before a single API call can succeed.

---

## The Concept

### What Is CORS?

**CORS — Cross-Origin Resource Sharing** — is a browser security mechanism that controls how a web page running at one origin can request resources from a different origin.

An **origin** is the combination of:
- **Protocol** (`http` or `https`)
- **Host** (domain name or IP address)
- **Port** (80, 443, 8080, 4200, etc.)

Two URLs share the same origin only if **all three** components match exactly.

| URL A | URL B | Same Origin? |
|---|---|---|
| `http://localhost:8080` | `http://localhost:8080/api/products` | ✅ Yes (same protocol, host, port) |
| `http://localhost:4200` | `http://localhost:8080` | ❌ No (different port) |
| `https://myapp.com` | `http://myapp.com` | ❌ No (different protocol) |
| `https://api.myapp.com` | `https://myapp.com` | ❌ No (different subdomain) |

Browsers enforce the Same-Origin Policy for **security**: without it, a malicious website could silently make API calls to your bank's website using your logged-in session cookies.

CORS is the **controlled relaxation** of this restriction. It lets your backend tell browsers: "I trust requests from this specific origin."

> **Important:** Postman does **not** enforce CORS. CORS is a browser-only mechanism. This is why your API works fine in Postman but fails in a browser. Never assume "it works in Postman" means "CORS is configured correctly."

---

### How CORS Works — The Browser's Conversation

When a browser makes a cross-origin request, it follows this protocol:

#### Simple Requests (GET, POST with plain text)

1. Browser sends the request with an `Origin` header.
2. Server responds with (or without) `Access-Control-Allow-Origin` header.
3. If the header is present and matches, the browser allows the response through to JavaScript.
4. If the header is absent or doesn't match, the browser **blocks** the response (even though the server processed it).

#### Preflight Requests (PUT, DELETE, PATCH, or requests with JSON body)

For "non-simple" requests, the browser first sends an automatic **OPTIONS** request (the "preflight") to ask the server's permission before sending the real request:

```
Browser → OPTIONS /api/products/1
          Origin: http://localhost:4200
          Access-Control-Request-Method: DELETE
          Access-Control-Request-Headers: Content-Type

Server  → 200 OK
          Access-Control-Allow-Origin: http://localhost:4200
          Access-Control-Allow-Methods: GET, POST, PUT, DELETE
          Access-Control-Allow-Headers: Content-Type
          Access-Control-Max-Age: 3600

Browser → DELETE /api/products/1 (the actual request, now permitted)
```

Spring Boot handles preflight responses automatically once you configure CORS — you do not need to write `OPTIONS` handlers.

---

### Option 1: `@CrossOrigin` on a Single Controller or Method

The simplest approach is to annotate a specific controller class or method:

```java
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")  // allow only this origin
public class ProductController {

    @GetMapping
    public List<Product> getAll() { ... }
}
```

You can also place it on a single method to restrict CORS to that method only:

```java
@GetMapping("/public")
@CrossOrigin(origins = "*")  // allow any origin for this one public endpoint
public List<Product> getPublicProducts() { ... }
```

#### `@CrossOrigin` Attributes

| Attribute | Default | Description |
|---|---|---|
| `origins` | `*` (all origins) | Allowed origins |
| `methods` | All methods | Allowed HTTP methods |
| `allowedHeaders` | All headers | Allowed request headers |
| `maxAge` | `1800` (30 min) | How long preflight results are cached (seconds) |

> **Warning:** `@CrossOrigin` without any attributes defaults to `origins = "*"` — allowing **any** origin. This is acceptable for fully public APIs (like a public read-only data endpoint) but is a security risk for authenticated APIs. Always specify your actual allowed origins in production.

---

### Option 2: Global CORS Configuration with `WebMvcConfigurer`

For most applications, you want CORS configured **globally** for all controllers rather than annotating each one. Implement `WebMvcConfigurer` and override `addCorsMappings`:

```java
package com.example.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")              // Apply to all /api/* routes
                .allowedOrigins(
                    "http://localhost:4200",        // Angular dev server
                    "https://myapp.com"             // Production frontend
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")                // Allow all request headers
                .allowCredentials(true)             // Allow cookies / auth headers
                .maxAge(3600);                      // Cache preflight for 1 hour
    }
}
```

This is the **recommended approach** for production Spring Boot APIs. It keeps all CORS rules in one place and removes the need to annotate individual controllers.

---

### `allowCredentials` — Cookies and Authorization Headers

When your API uses session cookies or HTTP authentication headers (like `Authorization: Bearer <token>`), you must set:

```java
.allowCredentials(true)
```

However, there is a critical constraint: **you cannot use `allowCredentials(true)` together with `allowedOrigins("*")`**. This would be a massive security hole — any website could make authenticated requests on behalf of your users. Spring Boot will throw an exception if you attempt this combination.

```java
// ❌ INVALID — Spring Boot will reject this at startup
registry.addMapping("/**")
        .allowedOrigins("*")
        .allowCredentials(true);

// ✅ CORRECT — always specify explicit origins with allowCredentials
registry.addMapping("/**")
        .allowedOrigins("http://localhost:4200", "https://myapp.com")
        .allowCredentials(true);
```

---

### CORS in Development vs Production

A common pattern is to use Spring Profiles (from Wednesday) to apply permissive CORS in development and strict CORS in production:

```java
@Configuration
@Profile("dev")  // Only active when spring.profiles.active=dev
public class DevWebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200", "http://localhost:3000")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}

@Configuration
@Profile("prod")  // Only active when spring.profiles.active=prod
public class ProdWebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://myapp.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

### CORS and Spring Security

If your project uses Spring Security (introduced in today's reading), be aware that Spring Security has its own CORS integration. Improper ordering can cause CORS headers to be stripped before reaching the browser, making it appear as a CORS error when the real problem is an authentication failure.

We will cover the correct integration of CORS with Spring Security in the Spring Security reading.

---

### Diagnosing CORS Issues — A Quick Checklist

When you see a CORS error in the browser console:

1. **Check the exact error message** — it specifies which header or method was blocked.
2. **Confirm CORS config is loaded** — add a breakpoint or log in `addCorsMappings`.
3. **Verify the origin matches exactly** — `http://localhost:4200` ≠ `http://localhost:4200/` (trailing slash).
4. **Check for Spring Security interference** — if using Spring Security, ensure CORS is configured there too.
5. **Do not test with Postman and call it "fixed"** — always verify from the actual browser.

---

## Summary

| Concept | Key Point |
|---|---|
| Same-Origin Policy | Browser blocks JS from reading responses from a different origin |
| CORS | Controlled relaxation of the Same-Origin Policy via HTTP headers |
| Preflight (`OPTIONS`) | Browser checks permission before sending PUT/DELETE/JSON requests |
| `@CrossOrigin` | Quick per-controller/per-method CORS annotation |
| `WebMvcConfigurer.addCorsMappings` | Global CORS configuration — the recommended approach |
| `allowedOrigins("*")` | Permits any origin — fine for public APIs, dangerous for authenticated ones |
| `allowCredentials(true)` | Required for cookie/token authentication; incompatible with `origins("*")` |
| Profiles for CORS | Use `@Profile("dev")` / `@Profile("prod")` to apply different policies per environment |

CORS is not optional — it is the first configuration step before your Angular frontend can talk to your Spring Boot backend. Configure it globally from the start of every project.

---

## Additional Resources

- [CORS with Spring — Baeldung](https://www.baeldung.com/spring-cors)
- [Cross-Origin Resource Sharing (CORS) — MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS)
- [Spring MVC CORS Configuration — Official Docs](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)
