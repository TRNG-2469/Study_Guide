# Spring MVC — The Web Layer

## Learning Objectives

By the end of this reading you will be able to:

- Explain what the `DispatcherServlet` is and its role in Spring MVC.
- Distinguish between `@Controller` and `@RestController`.
- Map HTTP methods to handler methods using `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, and `@PatchMapping`.
- Extract data from HTTP requests using `@PathVariable`, `@RequestParam`, and `@RequestBody`.
- Return fine-grained HTTP responses using `ResponseEntity<T>`.
- Describe the full Spring MVC request-processing pipeline end-to-end.

---

## Why This Matters

You have spent this week learning how Spring manages objects (IoC/DI), how Spring Boot auto-configures an application, and how Spring Data JPA persists data to PostgreSQL. But none of those layers are useful if there is no HTTP entry point — no way for a browser, mobile app, or Postman to reach your application.

**Spring MVC is that entry point.** It is the HTTP web layer of the Spring framework. Every REST API you build during Project 2, and every backend you will write in a professional Java role, flows through Spring MVC.

In Week 4 you used Javalin to handle HTTP requests. Javalin is lightweight and explicit — you registered every route manually. Spring MVC operates on the same fundamental principle (route → handler method), but integrates deeply with the rest of the Spring ecosystem: dependency injection, validation, transactions, and security all participate automatically.

---

## The Concept

### The `DispatcherServlet` — Spring MVC's Front Controller

Spring MVC implements the **Front Controller** design pattern. Every incoming HTTP request is first received by a single servlet: the **`DispatcherServlet`**. It acts as a traffic controller — inspecting the request and forwarding it to the correct handler method.

Here is what happens in order for every HTTP request:

```
HTTP Request
    │
    ▼
DispatcherServlet
    │
    ├─► HandlerMapping  →  finds the @Controller method matching the URL + HTTP method
    │
    ├─► HandlerAdapter  →  invokes the method, resolves @RequestBody, @PathVariable, etc.
    │
    ├─► Your @Controller / @RestController method runs
    │        │
    │        └─► returns a Java object (or ResponseEntity)
    │
    ├─► MessageConverter  →  serializes the Java object to JSON (via Jackson)
    │
    ▼
HTTP Response (JSON body, status code, headers)
```

With Spring Boot, the `DispatcherServlet` is registered automatically — you do not need to configure it in `web.xml` as you would in a traditional Spring MVC project.

---

### `@Controller` vs `@RestController`

| Annotation | What it does |
|---|---|
| `@Controller` | Marks a class as a Spring MVC controller. By default, return values are treated as **view names** (used with Thymeleaf or JSP templates). |
| `@RestController` | A convenience annotation that combines `@Controller` + `@ResponseBody`. Every method return value is **automatically serialized to JSON** and written into the HTTP response body. |

For REST APIs — which is what you are building — you will **always** use `@RestController`.

```java
// Traditional MVC — returns a view name (HTML template)
@Controller
public class PageController {
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("user", "Alice");
        return "dashboard"; // resolves to templates/dashboard.html
    }
}

// REST API — returns JSON automatically
@RestController
public class ProductController {
    @GetMapping("/api/products")
    public List<Product> getAllProducts() {
        return List.of(new Product(1L, "Laptop", 999.99));
        // Jackson serializes this List to JSON automatically
    }
}
```

---

### Request Mapping Annotations

Spring MVC provides shorthand annotations for each HTTP method. All of them are specializations of `@RequestMapping`.

| Annotation | HTTP Method | Typical Use |
|---|---|---|
| `@GetMapping` | GET | Retrieve resource(s) |
| `@PostMapping` | POST | Create a new resource |
| `@PutMapping` | PUT | Replace a resource entirely |
| `@PatchMapping` | PATCH | Partially update a resource |
| `@DeleteMapping` | DELETE | Delete a resource |

```java
@RestController
@RequestMapping("/api/products")   // base path shared by all methods below
public class ProductController {

    @GetMapping                     // GET /api/products
    public List<Product> getAll() { ... }

    @GetMapping("/{id}")            // GET /api/products/42
    public Product getById(@PathVariable Long id) { ... }

    @PostMapping                    // POST /api/products
    public Product create(@RequestBody Product product) { ... }

    @PutMapping("/{id}")            // PUT /api/products/42
    public Product update(@PathVariable Long id, @RequestBody Product product) { ... }

    @DeleteMapping("/{id}")         // DELETE /api/products/42
    public void delete(@PathVariable Long id) { ... }
}
```

> **Tip:** Place `@RequestMapping("/api/products")` at the **class level** to define a base path shared by all handler methods. Individual methods then specify only the sub-path.

---

### Extracting Data from the Request

#### `@PathVariable` — URL Path Segments

Used to bind a URI template variable (the `{id}` placeholder) to a method parameter.

```java
// URL: GET /api/products/42
@GetMapping("/{id}")
public Product getById(@PathVariable Long id) {
    // id = 42
    return productService.findById(id);
}
```

If the variable name in the URL matches the parameter name exactly, Spring maps them automatically. If they differ, specify the name explicitly:

```java
@GetMapping("/{productId}")
public Product getById(@PathVariable("productId") Long id) { ... }
```

#### `@RequestParam` — Query String Parameters

Used to bind query parameters (the key=value pairs after `?` in a URL).

```java
// URL: GET /api/products?category=electronics&minPrice=500
@GetMapping
public List<Product> search(
        @RequestParam String category,
        @RequestParam(required = false, defaultValue = "0") double minPrice) {
    return productService.search(category, minPrice);
}
```

- `required = false` makes the parameter optional.
- `defaultValue` provides a fallback when the parameter is absent.

#### `@RequestBody` — JSON Request Body

Used to deserialize the HTTP request body (JSON) into a Java object. Jackson performs the conversion automatically.

```java
// POST /api/products
// Request body: {"name": "Mouse", "price": 29.99}
@PostMapping
public Product create(@RequestBody Product product) {
    // product.getName() = "Mouse", product.getPrice() = 29.99
    return productService.save(product);
}
```

> **Important:** `@RequestBody` is used for POST, PUT, and PATCH requests that send a JSON body. Do not use it for GET or DELETE requests.

---

### `ResponseEntity<T>` — Full HTTP Response Control

When you return a plain object from a handler method, Spring automatically wraps it in a `200 OK` response. This is fine for simple cases, but often you need control over:

- The **HTTP status code** (201 Created, 204 No Content, 404 Not Found, etc.)
- **Response headers** (Location header after creating a resource)
- A **conditional body** (no body for 204, an error body for 404)

`ResponseEntity<T>` gives you that control:

```java
@PostMapping
public ResponseEntity<Product> create(@RequestBody Product product) {
    Product saved = productService.save(product);
    URI location = URI.create("/api/products/" + saved.getId());
    return ResponseEntity
            .created(location)          // 201 Created + Location header
            .body(saved);               // response body = serialized Product JSON
}

@GetMapping("/{id}")
public ResponseEntity<Product> getById(@PathVariable Long id) {
    return productService.findById(id)
            .map(ResponseEntity::ok)                        // 200 OK with body
            .orElse(ResponseEntity.notFound().build());     // 404 Not Found, no body
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();   // 204 No Content, no body
}
```

#### `ResponseEntity` Builder Quick Reference

| Builder Method | Status Code | Body |
|---|---|---|
| `ResponseEntity.ok(body)` | 200 OK | Yes |
| `ResponseEntity.created(uri).body(body)` | 201 Created | Yes |
| `ResponseEntity.noContent().build()` | 204 No Content | No |
| `ResponseEntity.badRequest().body(error)` | 400 Bad Request | Optional |
| `ResponseEntity.notFound().build()` | 404 Not Found | No |
| `ResponseEntity.status(HttpStatus.X).body(b)` | Any status | Yes |

---

### A Complete Controller Example

Putting it all together — a complete, production-style controller:

```java
package com.example.api.controller;

import com.example.api.model.Product;
import com.example.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // Constructor injection — no @Autowired needed in Spring Boot
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        Product saved = productService.save(product);
        URI location = URI.create("/api/products/" + saved.getId());
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @Valid @RequestBody Product product) {
        return productService.update(id, product)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

Notice that:
- The `ProductService` is injected via constructor injection (the preferred approach from Monday).
- `@Valid` triggers Bean Validation on the request body (from Thursday's lesson).
- `ResponseEntity` is used for every method that can return a 404 or needs a specific status.

---

### The `@RequestMapping` Annotation Directly

Before the shorthand annotations existed, all mappings used `@RequestMapping` with a `method` attribute:

```java
@RequestMapping(value = "/api/products", method = RequestMethod.GET)
public List<Product> getAll() { ... }
```

You may encounter this in older codebases. The shorthand annotations (`@GetMapping`, etc.) are preferred in modern Spring applications.

---

## Summary

| Concept | Key Point |
|---|---|
| `DispatcherServlet` | Front Controller — all HTTP requests flow through it |
| `@RestController` | `@Controller` + `@ResponseBody`; returns JSON automatically |
| `@GetMapping` / `@PostMapping` etc. | Map HTTP methods to handler methods |
| `@PathVariable` | Binds URL path segment (`/products/{id}`) to method parameter |
| `@RequestParam` | Binds query string parameter (`?category=X`) to method parameter |
| `@RequestBody` | Deserializes JSON request body into a Java object (via Jackson) |
| `ResponseEntity<T>` | Returns an HTTP response with full control over status code, headers, and body |
| `@RequestMapping` (class level) | Defines the base URL prefix for all methods in a controller |

Spring MVC transforms your service layer into a network-accessible API. Every HTTP request flows through the `DispatcherServlet`, is routed to the right method, and your Java return value is converted to JSON — all handled by the framework with zero boilerplate.

---

## Additional Resources

- [Spring MVC — Official Reference Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Building a RESTful Web Service — Spring Guides](https://spring.io/guides/gs/rest-service/)
- [ResponseEntity — Baeldung Deep Dive](https://www.baeldung.com/spring-response-entity)
