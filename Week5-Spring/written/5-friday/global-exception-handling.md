# Global Exception Handling with `@ControllerAdvice`

## Learning Objectives

By the end of this reading you will be able to:

- Explain why a global exception handling strategy is essential in a Spring Boot REST API.
- Use `@ControllerAdvice` and `@ExceptionHandler` to intercept exceptions across all controllers.
- Create a standardized error response structure for API consumers.
- Apply `@ResponseStatus` to custom exception classes.
- Distinguish between handling exceptions locally (in a single controller) and globally.

---

## Why This Matters

In Week 4, when you built your Javalin API, you used `app.exception(SomeException.class, handler)` to catch errors and return meaningful JSON responses instead of raw stack traces. Without that, clients received a cryptic 500 Internal Server Error with an HTML error page.

Spring Boot has the same problem — and a more powerful solution. When an unhandled exception escapes a controller method, Spring Boot's default error mechanism kicks in and returns a generic error page or a partially-helpful JSON body. For a production REST API, this is not acceptable. Your API consumers (Angular frontends, mobile apps, or other microservices) need:

1. A **consistent** error response format — every error looks the same structurally.
2. **Meaningful status codes** — 404 for missing resources, 400 for bad input, 409 for conflicts.
3. **No internal details leaked** — stack traces and database error messages must not reach the client.

`@ControllerAdvice` is Spring's solution. It is a single, centralized class that intercepts exceptions thrown anywhere in your application and converts them into clean, controlled HTTP responses.

---

## The Concept

### The Problem Without Global Handling

Consider this controller method:

```java
@GetMapping("/{id}")
public Product getById(@PathVariable Long id) {
    return productService.findById(id)  // throws ProductNotFoundException if not found
}
```

If `ProductService.findById` throws a `ProductNotFoundException` (a custom exception), and you have no global handler, Spring Boot returns:

```json
{
  "timestamp": "2026-08-12T16:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/products/99"
}
```

The status is wrong (it should be 404), the message is useless, and you've told the client nothing actionable. You would have to add a `try-catch` inside every controller method — which is repetitive and error-prone.

---

### `@ControllerAdvice` — The Centralized Interceptor

`@ControllerAdvice` marks a class as a **global exception handler**. Spring automatically applies it to every `@Controller` and `@RestController` in your application. You never touch the individual controller methods.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    // @ExceptionHandler methods go here
}
```

Inside this class, each method annotated with `@ExceptionHandler` declares which exception type it handles.

---

### `@ExceptionHandler` — Mapping Exceptions to Responses

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                           .getFieldErrors()
                           .stream()
                           .map(e -> e.getField() + ": " + e.getDefaultMessage())
                           .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse(400, message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllOther(Exception ex) {
        ErrorResponse error = new ErrorResponse(500, "An unexpected error occurred.");
        return ResponseEntity.internalServerError().body(error);
    }
}
```

> **Order matters:** Spring applies the most specific `@ExceptionHandler` first. `Exception.class` acts as a catch-all and should always be defined — it prevents raw stack traces from ever reaching the client.

---

### Creating a Standardized Error Response

Define a simple POJO (or a Java record) to represent your error response. Every error your API returns will use this same shape — making it predictable for API consumers.

```java
public class ErrorResponse {
    private int status;
    private String message;
    private Instant timestamp;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = Instant.now();
    }

    // Getters required for Jackson serialization
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
}
```

Using Lombok from Wednesday, this simplifies to:

```java
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp = Instant.now();
}
```

Your API will now return consistent, structured errors:

```json
{
  "status": 404,
  "message": "Product with id 99 was not found.",
  "timestamp": "2026-08-12T16:00:00Z"
}
```

---

### Creating Custom Exception Classes

Domain-specific exceptions make your code readable and allow fine-grained handling:

```java
// Thrown when a requested resource does not exist
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product with id " + id + " was not found.");
    }
}

// Thrown when a business rule is violated
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName) {
        super("Insufficient stock for product: " + productName);
    }
}
```

These are **unchecked exceptions** (`RuntimeException` subclasses). Spring's `@Transactional` rolls back transactions on unchecked exceptions by default — a convenient behavior you get for free.

---

### `@ResponseStatus` — The Shortcut Approach

For simple cases where you always want the same status code, you can annotate the exception class itself with `@ResponseStatus`:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product with id " + id + " was not found.");
    }
}
```

When this exception propagates unhandled, Spring automatically responds with `404 Not Found`. However, this approach provides **less control** over the response body. The `@ControllerAdvice` approach is preferred when you need a consistent `ErrorResponse` structure.

---

### Local vs Global Exception Handling

You can also use `@ExceptionHandler` inside a specific `@RestController` to handle exceptions only within that controller:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLocalNotFound(ProductNotFoundException ex) {
        // This only applies to exceptions from THIS controller
        return ResponseEntity.status(404).body(new ErrorResponse(404, ex.getMessage()));
    }
}
```

| Approach | Scope | When to Use |
|---|---|---|
| `@ControllerAdvice` (global) | All controllers | Default — use for all standard exceptions |
| `@ExceptionHandler` in controller | Single controller | Rarely — only if behavior genuinely differs from global |

In practice, you will define a single `@ControllerAdvice` class and handle all exceptions there.

---

### A Complete Global Exception Handler

Here is a production-ready global exception handler for a typical Spring Boot REST API:

```java
package com.example.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// Ensures the error response is serialized as JSON automatically
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 — Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    // 400 — Bean Validation failures (@Valid on @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(400, "Validation failed: " + details));
    }

    // 400 — Type mismatch (e.g., sending "abc" for a Long @PathVariable)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String msg = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(400, msg));
    }

    // 409 — Business rule conflict
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(BusinessRuleViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage()));
    }

    // 500 — Catch-all (prevents internal details leaking to the client)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        // Log the real exception internally (use a Logger in production)
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(500, "An unexpected error occurred. Please try again later."));
    }
}
```

> **Note:** `@RestControllerAdvice` is a convenience annotation that combines `@ControllerAdvice` and `@ResponseBody`. Use it instead of `@ControllerAdvice` for REST APIs so all error responses are automatically serialized as JSON.

---

### The `@ExceptionHandler` Method Signature

Spring is flexible about what parameters `@ExceptionHandler` methods can accept:

| Parameter Type | What it provides |
|---|---|
| `SomeException ex` | The exception itself (always include this) |
| `HttpServletRequest request` | Request details (URL, headers) |
| `WebRequest request` | Web request abstraction |

You do not need all of these — the exception parameter is almost always sufficient.

---

## Summary

| Concept | Key Point |
|---|---|
| `@ControllerAdvice` | Global exception handler class applied to all controllers |
| `@RestControllerAdvice` | Same as `@ControllerAdvice` + `@ResponseBody` — prefer for REST APIs |
| `@ExceptionHandler(X.class)` | Intercepts exceptions of type X and returns a controlled HTTP response |
| `ErrorResponse` POJO | Standardized structure for all API error responses |
| Custom `RuntimeException` subclasses | Domain-specific exceptions that communicate intent |
| `@ResponseStatus` | Quick status code annotation on exception class — less flexible than `@ControllerAdvice` |
| Catch-all `Exception.class` handler | Prevents internal details (stack traces, DB errors) from reaching the client |

A good global exception handler is invisible to happy-path users but invaluable during errors. It is one of the first things to put in place when starting a new Spring Boot REST API project.

---

## Additional Resources

- [Exception Handling for REST with Spring — Baeldung](https://www.baeldung.com/exception-handling-for-rest-with-spring)
- [Spring @ControllerAdvice — Official Docs](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
- [Error Handling in Spring Boot — Spring Blog](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)
