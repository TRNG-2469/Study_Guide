# Practice Quiz: Week 5 Friday — Critical Gap Topics

> **Topics Covered:** Spring MVC Web Layer · Global Exception Handling · CORS · Spring Security
> **Format:** Multiple Choice · True/False · Code Prediction · Fill-in-the-Blank
> **Instructions:** Attempt each question before clicking **🔎 Click for Solution**.

---

## Part 1: Spring MVC — The Web Layer

---

### Q1. Which annotation is the correct choice for a REST API controller that should automatically serialize all return values to JSON?

- [ ] A) `@Controller`
- [ ] B) `@RestController`
- [ ] C) `@Service`
- [ ] D) `@RequestMapping`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `@RestController`

**Explanation:** `@RestController` is a convenience annotation combining `@Controller` + `@ResponseBody`. Every method return value is automatically serialized to JSON by Jackson and written to the HTTP response body.

**Why others are wrong:**
- A) `@Controller` by itself treats return values as view names (for HTML templates), not JSON. You would need `@ResponseBody` on every method.
- C) `@Service` marks a business-logic bean — it is not an HTTP handler.
- D) `@RequestMapping` is a method/class-level mapping annotation, not a class stereotype.

</details>

---

### Q2. What does `@RestController` combine?

- [ ] A) `@Controller` + `@RequestMapping`
- [ ] B) `@Service` + `@ResponseBody`
- [ ] C) `@Controller` + `@ResponseBody`
- [ ] D) `@Component` + `@ResponseBody`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) `@Controller` + `@ResponseBody`

**Explanation:** `@RestController` is explicitly defined as a meta-annotation of `@Controller` and `@ResponseBody`. The `@ResponseBody` part is what instructs Spring to write return values directly to the response body as JSON.

**Why others are wrong:**
- A) `@RequestMapping` is a routing annotation, not part of the `@RestController` composition.
- B) `@Service` is a stereotype annotation for the service layer, not related to HTTP handling.
- D) `@Component` is the generic stereotype — `@Controller` is the correct base.

</details>

---

### Q3. **True or False:** `@GetMapping("/products")` and `@RequestMapping(value="/products", method=RequestMethod.GET)` are functionally equivalent.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** `@GetMapping` is a composed meta-annotation that is a shorthand for `@RequestMapping(method = RequestMethod.GET)`. Both produce identical behaviour. The shorthand annotations (`@GetMapping`, `@PostMapping`, etc.) are preferred in modern Spring applications for readability.

</details>

---

### Q4. **Code Prediction:** What HTTP status code and response will this method return when called with `GET /api/products/99` if the product does not exist?

```java
@GetMapping("/{id}")
public ResponseEntity<Product> getById(@PathVariable Long id) {
    return productService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

- [ ] A) 200 OK with `null` body
- [ ] B) 404 Not Found with no body
- [ ] C) 500 Internal Server Error
- [ ] D) 204 No Content

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) 404 Not Found with no body

**Explanation:** When `findById(99)` returns `Optional.empty()`, the `.orElse(ResponseEntity.notFound().build())` branch executes. `ResponseEntity.notFound().build()` creates a response with status `404 Not Found` and no body.

**Why others are wrong:**
- A) `ResponseEntity.notFound().build()` explicitly sets the status to 404 — not 200.
- C) No exception is thrown; the Optional handles the missing case gracefully.
- D) 204 No Content is used for successful operations with no body to return (e.g., DELETE), not for missing resources.

</details>

---

### Q5. **Fill-in-the-Blank:** The annotation used to bind a query string parameter (e.g., `?category=electronics`) to a method parameter is `@_________`.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `@RequestParam`

**Explanation:** `@RequestParam` binds key=value pairs from the query string to method parameters. It supports `required` and `defaultValue` attributes for optional parameters. Example: `@RequestParam(required = false, defaultValue = "all") String category`.

</details>

---

### Q6. What is the role of the `DispatcherServlet` in Spring MVC?

- [ ] A) It connects to the database and manages transactions
- [ ] B) It acts as a Front Controller, routing all incoming HTTP requests to the correct handler method
- [ ] C) It scans the classpath for `@Component` annotations
- [ ] D) It serializes Java objects to JSON using Jackson

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) It acts as a Front Controller, routing all incoming HTTP requests to the correct handler method

**Explanation:** The `DispatcherServlet` implements the Front Controller design pattern. Every HTTP request arrives at the `DispatcherServlet` first, which delegates to `HandlerMapping` to find the right `@Controller` method, then to `HandlerAdapter` to invoke it.

**Why others are wrong:**
- A) Transaction management is handled by `@Transactional` and the PlatformTransactionManager.
- C) Component scanning is handled by `ApplicationContext` at startup, not `DispatcherServlet` per request.
- D) JSON serialization is performed by `HttpMessageConverter` (specifically Jackson's `MappingJackson2HttpMessageConverter`), not `DispatcherServlet` directly.

</details>

---

### Q7. **Code Prediction:** What HTTP status code does this POST endpoint return on success?

```java
@PostMapping
public ResponseEntity<Product> create(@RequestBody Product product) {
    Product saved = productService.save(product);
    URI location = URI.create("/api/products/" + saved.getId());
    return ResponseEntity.created(location).body(saved);
}
```

- [ ] A) 200 OK
- [ ] B) 201 Created
- [ ] C) 202 Accepted
- [ ] D) 204 No Content

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) 201 Created

**Explanation:** `ResponseEntity.created(location)` produces a `201 Created` response. This is the correct HTTP status for a successful resource creation — it also sets the `Location` header to the URL of the newly created resource.

**Why others are wrong:**
- A) 200 OK is for successful reads or updates that return a body.
- C) 202 Accepted means the request was received but processing is not yet complete (used in async workflows).
- D) 204 No Content is for successful operations with no body, typically DELETE.

</details>

---

### Q8. **True or False:** `@PathVariable` and `@RequestParam` can both be used in the same handler method.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** These annotations bind from different parts of the HTTP request and are completely independent. A single method can use `@PathVariable` to extract the `{id}` from the URL path and `@RequestParam` to extract query parameters — for example, `GET /api/products/42/reviews?rating=5` could use both.

</details>

---

### Q9. Which annotation is placed on a method parameter to deserialize a JSON request body into a Java object?

- [ ] A) `@PathVariable`
- [ ] B) `@RequestParam`
- [ ] C) `@RequestBody`
- [ ] D) `@ResponseBody`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) `@RequestBody`

**Explanation:** `@RequestBody` tells Spring to read the HTTP request body and use Jackson to deserialize it into the specified Java type. It is used for POST, PUT, and PATCH requests that send a JSON payload.

**Why others are wrong:**
- A) `@PathVariable` extracts values from URL path segments like `{id}`.
- B) `@RequestParam` extracts query string or form parameters.
- D) `@ResponseBody` is a method-level annotation on the *response* side — it tells Spring to write the return value to the response body. It is already included in `@RestController`.

</details>

---

### Q10. **Fill-in-the-Blank:** When using `@RequestMapping` at the class level, the value sets the _________ path shared by all handler methods in that controller.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** **base** path (also accepted: "root path" or "prefix path")

**Explanation:** `@RequestMapping("/api/products")` at the class level defines the base URL prefix. Individual method-level mappings like `@GetMapping("/{id}")` append to this base, resulting in `/api/products/{id}`.

</details>

---

### Q11. **Code Prediction:** What is the correct HTTP method and URL that this handler will respond to?

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        orderService.removeItem(orderId, itemId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] A) `GET /api/orders/{orderId}/items/{itemId}`
- [ ] B) `DELETE /api/orders/{orderId}/items/{itemId}`
- [ ] C) `DELETE /api/orders/items`
- [ ] D) `POST /api/orders/{orderId}/items/{itemId}`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `DELETE /api/orders/{orderId}/items/{itemId}`

**Explanation:** The class-level `@RequestMapping("/api/orders")` sets the base path. The method-level `@DeleteMapping("/{orderId}/items/{itemId}")` appends to it. Combined: `DELETE /api/orders/{orderId}/items/{itemId}`. Both `orderId` and `itemId` are extracted via `@PathVariable`.

</details>

---

### Q12. **True or False:** When using `ResponseEntity`, you must always provide a response body.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** `ResponseEntity` supports body-less responses using `.build()`. For example, `ResponseEntity.noContent().build()` returns a `204 No Content` response with no body, and `ResponseEntity.notFound().build()` returns `404 Not Found` with no body. The body is optional.

</details>

---

### Q13. What does Jackson's `MessageConverter` do in the Spring MVC pipeline?

- [ ] A) Routes the request to the correct controller
- [ ] B) Validates `@PathVariable` types
- [ ] C) Serializes Java return values to JSON (and deserializes JSON bodies to Java)
- [ ] D) Manages database transactions

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) Serializes Java return values to JSON (and deserializes JSON bodies to Java)

**Explanation:** After the handler method returns a Java object, the `HandlerAdapter` delegates to an `HttpMessageConverter`. Jackson's `MappingJackson2HttpMessageConverter` serializes the Java object to JSON for the response, and deserializes the JSON request body (for `@RequestBody`) to Java on the inbound side.

</details>

---

### Q14. **Fill-in-the-Blank:** `@PatchMapping` maps HTTP _______ requests, typically used for _______ updates to a resource.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `PATCH` requests, typically used for **partial** updates.

**Explanation:** `PATCH` is used when you want to update only some fields of a resource, unlike `PUT` which replaces the entire resource. `@PatchMapping` is the Spring shorthand for `@RequestMapping(method = RequestMethod.PATCH)`.

</details>

---

## Part 2: Global Exception Handling (`@ControllerAdvice`)

---

### Q15. What annotation marks a class as a global exception handler applied to all controllers?

- [ ] A) `@ExceptionHandler`
- [ ] B) `@ControllerAdvice`
- [ ] C) `@GlobalHandler`
- [ ] D) `@ErrorController`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `@ControllerAdvice`

**Explanation:** `@ControllerAdvice` marks a class as a global component that applies to all `@Controller` (and `@RestController`) classes in the application. Methods inside it annotated with `@ExceptionHandler` intercept exceptions thrown by any controller.

**Why others are wrong:**
- A) `@ExceptionHandler` is a method-level annotation used *inside* a `@ControllerAdvice` class to specify which exception each method handles.
- C) `@GlobalHandler` does not exist in Spring.
- D) `@ErrorController` is an interface for customizing the `/error` fallback endpoint — different from `@ControllerAdvice`.

</details>

---

### Q16. **True or False:** `@RestControllerAdvice` is equivalent to `@ControllerAdvice` + `@ResponseBody`.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** `@RestControllerAdvice` is a convenience meta-annotation combining `@ControllerAdvice` and `@ResponseBody`. The `@ResponseBody` addition ensures that every `@ExceptionHandler` method return value is automatically serialized to JSON — no extra annotation needed on each method.

</details>

---

### Q17. Which annotation is placed on a method *inside* a `@ControllerAdvice` class to specify which exception it handles?

- [ ] A) `@CatchException`
- [ ] B) `@ExceptionMapping`
- [ ] C) `@ExceptionHandler`
- [ ] D) `@HandleError`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) `@ExceptionHandler`

**Explanation:** `@ExceptionHandler(SomeException.class)` on a method inside `@ControllerAdvice` tells Spring: "when `SomeException` is thrown by any controller, invoke this method to produce the HTTP response." The method receives the exception as a parameter.

</details>

---

### Q18. **Code Prediction:** Given this `@RestControllerAdvice`, what HTTP status code is returned when `ProductNotFoundException` is thrown?

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(500, "Unexpected error."));
    }
}
```

- [ ] A) 500 Internal Server Error (catch-all runs)
- [ ] B) 404 Not Found
- [ ] C) 200 OK
- [ ] D) Whichever handler is defined first

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) 404 Not Found

**Explanation:** Spring applies the *most specific* `@ExceptionHandler` first. `ProductNotFoundException` has a dedicated handler, so it takes priority over the `Exception.class` catch-all. The catch-all only runs when no more specific handler matches.

**Why others are wrong:**
- A) The catch-all does NOT run when a more specific handler exists for the exception type.
- C) The response body contains an `ErrorResponse`, not the product. Status 200 is not set.
- D) Declaration order does not determine which handler runs — specificity does.

</details>

---

### Q19. Why should the catch-all `@ExceptionHandler(Exception.class)` return a generic message instead of `ex.getMessage()`?

- [ ] A) Because `getMessage()` always returns `null`
- [ ] B) To prevent internal implementation details (database errors, stack traces) from being exposed to clients
- [ ] C) Because `ResponseEntity` does not support string bodies
- [ ] D) For performance reasons — string operations are slow

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) To prevent internal implementation details (database errors, stack traces) from being exposed to clients

**Explanation:** Unexpected exceptions may contain sensitive information in their messages — database connection strings, table names, internal class paths, or even partial data. Exposing these in API responses is a security risk (OWASP A02 Cryptographic Failures, information exposure). A generic "An unexpected error occurred" message is safe while still signalling a 500 to the client.

</details>

---

### Q20. **True or False:** You can use `@ExceptionHandler` inside a single `@RestController` to handle exceptions only for that controller.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** `@ExceptionHandler` can be placed on a method directly inside a `@Controller` or `@RestController`. In that case, it only intercepts exceptions thrown by methods in *that specific class*. When placed inside `@ControllerAdvice`, the scope is global (all controllers). The global approach is preferred for consistent error handling.

</details>

---

### Q21. **Fill-in-the-Blank:** A custom exception class should extend `_______Exception` (not the checked alternative) so that Spring's `@Transactional` rolls back automatically and callers do not need `try-catch`.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `RuntimeException`

**Explanation:** Spring's `@Transactional` rolls back transactions when an unchecked exception (subclass of `RuntimeException`) propagates. Checked exceptions (subclasses of `Exception` but not `RuntimeException`) do NOT trigger automatic rollback by default. Additionally, unchecked exceptions don't force the caller to declare `throws` or add `try-catch`.

</details>

---

### Q22. What is the purpose of the `ErrorResponse` POJO in a global exception handler?

- [ ] A) To log exceptions to a file
- [ ] B) To provide a consistent, structured JSON shape for every error response
- [ ] C) To convert Java exceptions to HTTP exceptions automatically
- [ ] D) To retry failed requests

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) To provide a consistent, structured JSON shape for every error response

**Explanation:** Without a standardized `ErrorResponse`, different exceptions might produce different JSON structures, making client-side error handling unpredictable. A single POJO (with fields like `status`, `message`, `timestamp`) ensures every error — 404, 400, 500 — looks the same to the API consumer.

</details>

---

### Q23. **Code Prediction:** What is wrong with this `@ExceptionHandler` method?

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex) {
    return ResponseEntity.ok()
            .body(new ErrorResponse(200, "Validation failed"));
}
```

- [ ] A) `MethodArgumentNotValidException` cannot be handled with `@ExceptionHandler`
- [ ] B) The status code is wrong — validation failures should return `400 Bad Request`, not `200 OK`
- [ ] C) `ErrorResponse` cannot be used as a response body
- [ ] D) `ResponseEntity.ok()` is not a valid method

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) The status code is wrong — validation failures should return `400 Bad Request`, not `200 OK`

**Explanation:** `MethodArgumentNotValidException` is thrown when `@Valid` validation fails. The correct HTTP status is `400 Bad Request`, indicating the client sent invalid data. Using `200 OK` misleads clients into thinking the request succeeded when it actually failed. The correct response should use `ResponseEntity.badRequest()`.

</details>

---

### Q24. **True or False:** `@ControllerAdvice` only intercepts exceptions from classes annotated with `@RestController`, not plain `@Controller` classes.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** `@ControllerAdvice` applies to **both** `@Controller` and `@RestController` classes. It is the base form — `@RestControllerAdvice` adds `@ResponseBody` so return values are serialized to JSON. Both advices cover all controllers.

</details>

---

### Q25. **Fill-in-the-Blank:** The annotation `@ResponseStatus(HttpStatus.NOT_FOUND)` placed directly on a custom exception class makes Spring return a _______ response when that exception propagates unhandled.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `404 Not Found`

**Explanation:** `@ResponseStatus` on an exception class is a quick shortcut to associate a default HTTP status with that exception type. However, it provides less control than `@ControllerAdvice` — you cannot customize the response body format, so `@ControllerAdvice` is preferred when a consistent `ErrorResponse` structure is needed.

</details>

---

## Part 3: CORS

---

### Q26. What does CORS stand for?

- [ ] A) Controller Object Routing System
- [ ] B) Cross-Origin Resource Sharing
- [ ] C) Cached Object Request Standard
- [ ] D) Cross-Origin Response Security

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Cross-Origin Resource Sharing

**Explanation:** CORS is the browser mechanism that controls how JavaScript at one origin can request resources from a different origin. It is the *controlled relaxation* of the browser's Same-Origin Policy.

</details>

---

### Q27. What three components together define an "origin"?

- [ ] A) Domain, path, and query string
- [ ] B) Protocol, host, and port
- [ ] C) IP address, port, and HTTP method
- [ ] D) URL, cookie, and session ID

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Protocol, host, and port

**Explanation:** Two URLs share the same origin only if ALL three match: protocol (`http`/`https`), host (domain or IP), and port number. `http://localhost:4200` and `http://localhost:8080` are different origins because the port differs.

</details>

---

### Q28. **True or False:** A DELETE request from `http://localhost:4200` to `http://localhost:8080/api/products/1` will trigger a CORS preflight `OPTIONS` request.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** `DELETE` is a "non-simple" HTTP method. The browser sends an `OPTIONS` preflight request first to ask the server's permission before sending the actual `DELETE`. Spring Boot handles the preflight response automatically once CORS is configured.

</details>

---

### Q29. **True or False:** Postman enforces the Same-Origin Policy, so if CORS is misconfigured, requests will also fail in Postman.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** CORS is a **browser-only** enforcement mechanism. Postman is a desktop application, not a browser — it does not enforce the Same-Origin Policy. A request that works in Postman can still fail in a browser if CORS headers are missing or incorrect. Always test CORS from an actual browser, not just Postman.

</details>

---

### Q30. Which class do you implement to configure CORS globally for all controllers in Spring Boot?

- [ ] A) `CorsConfigurationSource`
- [ ] B) `WebSecurityConfigurer`
- [ ] C) `WebMvcConfigurer`
- [ ] D) `HandlerInterceptor`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) `WebMvcConfigurer`

**Explanation:** Implementing `WebMvcConfigurer` and overriding `addCorsMappings(CorsRegistry registry)` is the recommended approach for global CORS configuration. You declare a `@Configuration` class that implements `WebMvcConfigurer` — no need to annotate individual controllers with `@CrossOrigin`.

**Why others are wrong:**
- A) `CorsConfigurationSource` is used in the Spring Security CORS integration — different configuration path.
- B) `WebSecurityConfigurer` (and its successor `SecurityFilterChain`) handles authentication/authorization, not CORS per se.
- D) `HandlerInterceptor` intercepts requests before and after handler execution — not intended for CORS headers.

</details>

---

### Q31. **Code Prediction:** What CORS annotation can be applied directly to a single controller method to allow requests from a specific origin?

- [ ] A) `@AllowOrigin`
- [ ] B) `@CorsMapping`
- [ ] C) `@CrossOrigin`
- [ ] D) `@EnableCors`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) `@CrossOrigin`

**Explanation:** `@CrossOrigin` can be placed at the class or method level on a `@RestController` to define CORS rules for that scope. Example: `@CrossOrigin(origins = "http://localhost:4200")`. For production APIs, global configuration via `WebMvcConfigurer` is preferred over annotating individual controllers.

</details>

---

### Q32. Why can you NOT combine `allowedOrigins("*")` with `allowCredentials(true)`?

- [ ] A) It is a syntax error in Spring
- [ ] B) `allowCredentials(true)` requires an `OPTIONS` request first
- [ ] C) Allowing credentials from any origin would let any website make authenticated requests on behalf of your users — a critical security vulnerability
- [ ] D) `"*"` is not a valid value for `allowedOrigins`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) Allowing credentials from any origin would let any website make authenticated requests on behalf of your users — a critical security vulnerability

**Explanation:** `allowCredentials(true)` permits cookies and `Authorization` headers to be sent cross-origin. Combined with `allowedOrigins("*")`, any malicious website could silently make authenticated API calls using the victim's credentials. Spring Boot explicitly rejects this combination at startup with an `IllegalArgumentException`.

</details>

---

### Q33. **Fill-in-the-Blank:** A CORS preflight request uses the HTTP method _______.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `OPTIONS`

**Explanation:** Before sending a "non-simple" cross-origin request (DELETE, PUT, PATCH, or any request with custom headers or a JSON body), the browser sends an `OPTIONS` request to the same URL. The server responds with CORS permission headers, and only then does the browser send the real request. Spring handles this `OPTIONS` response automatically.

</details>

---

### Q34. **True or False:** When you configure CORS with `WebMvcConfigurer`, you still need to write an `@ExceptionHandler` for the preflight `OPTIONS` request.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** Spring Boot handles preflight `OPTIONS` requests automatically once CORS is configured via `WebMvcConfigurer`. You do not need to write any `OPTIONS` handler methods or exception handlers for preflight. This is one of the advantages of the framework-level CORS configuration.

</details>

---

### Q35. **Code Prediction:** `http://localhost:4200/app` and `http://localhost:4200` — are these the same origin?

- [ ] A) No — the paths differ
- [ ] B) Yes — protocol, host, and port are identical
- [ ] C) No — one has a path and one doesn't
- [ ] D) Depends on the browser

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Yes — protocol, host, and port are identical

**Explanation:** Origin is defined by **protocol + host + port only** — the path (`/app`) is NOT part of the origin. Both URLs share the same protocol (`http`), host (`localhost`), and port (`4200`). The path is irrelevant for Same-Origin Policy calculations.

</details>

---

### Q36. What is the purpose of the `maxAge` attribute in CORS configuration?

- [ ] A) Sets the maximum age of a user session
- [ ] B) Specifies how long (in seconds) the browser caches the preflight response
- [ ] C) Limits the age of JWT tokens
- [ ] D) Sets a timeout for API requests

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Specifies how long (in seconds) the browser caches the preflight response

**Explanation:** `maxAge(3600)` tells the browser it can cache the preflight `OPTIONS` response for 3600 seconds (1 hour). During this window, the browser skips the preflight for subsequent identical cross-origin requests, reducing latency. The default value for `@CrossOrigin` is `1800` seconds (30 minutes).

</details>

---

### Q37. **True or False:** A simple `GET` request with no custom headers from a cross-origin page will always trigger a preflight `OPTIONS` request.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** A `GET` request with standard headers and no custom headers is considered a "simple" request by the CORS specification. Simple requests are sent directly without a preflight — the browser just includes the `Origin` header. Preflight is only triggered for non-simple requests: `DELETE`, `PUT`, `PATCH`, or any request with custom headers (like `Authorization`) or a JSON body.

</details>

---

### Q38. **Fill-in-the-Blank:** Using `@Profile("dev")` on a `WebMvcConfigurer` class ensures the CORS configuration is only active when the Spring profile named _______ is set.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `dev`

**Explanation:** `@Profile("dev")` is a conditional annotation — the Spring container only registers that `@Configuration` bean when `spring.profiles.active=dev` is set (in `application.properties`, `application.yml`, or as an environment variable). This allows different CORS policies for development and production environments.

</details>

---

## Part 4: Spring Security

---

### Q39. What is the first thing Spring Boot does when `spring-boot-starter-security` is added to the classpath with no configuration?

- [ ] A) Creates a login page at `/login` and blocks all other routes
- [ ] B) Logs a warning and continues with an open API
- [ ] C) Locks down the entire application — every endpoint requires authentication — and generates a random password
- [ ] D) Enables only the `/actuator/health` endpoint

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) Locks down the entire application — every endpoint requires authentication — and generates a random password

**Explanation:** Spring Security's "secure by default" philosophy means adding the dependency immediately locks everything. Spring Boot prints a generated password to the console for the default `user` account. This forces developers to explicitly declare what should be public — rather than accidentally leaving endpoints open.

</details>

---

### Q40. **True or False:** In Spring Security 6 (used by Spring Boot 3), you should extend `WebSecurityConfigurerAdapter` to customize security.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** `WebSecurityConfigurerAdapter` was deprecated in Spring Security 5.7 and **removed** entirely in Spring Security 6 / Spring Boot 3. The correct modern approach is to declare a `SecurityFilterChain` bean in a `@Configuration` class using `HttpSecurity`.

</details>

---

### Q41. What is the correct modern way to configure Spring Security in Spring Boot 3?

- [ ] A) Extend `WebSecurityConfigurerAdapter` and override `configure(HttpSecurity)`
- [ ] B) Declare a `@Bean` method returning a `SecurityFilterChain` that accepts `HttpSecurity`
- [ ] C) Annotate your main class with `@EnableSecurity`
- [ ] D) Create a `security.properties` file

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Declare a `@Bean` method returning a `SecurityFilterChain` that accepts `HttpSecurity`

**Explanation:** The component-based configuration model replaced `WebSecurityConfigurerAdapter`. You declare a `@Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { ... }` inside a `@Configuration @EnableWebSecurity` class. This is the Spring Security 6 standard.

</details>

---

### Q42. **Code Prediction:** What happens when a request to `POST /api/products` is made by a user with the `USER` role using this configuration?

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

- [ ] A) 200 OK — USER role is authenticated
- [ ] B) 401 Unauthorized — USER is not authenticated
- [ ] C) 403 Forbidden — USER is authenticated but not authorized for POST
- [ ] D) 404 Not Found

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) 403 Forbidden — USER is authenticated but not authorized for POST

**Explanation:** The USER is authenticated (Spring Security verifies their identity), so `401 Unauthorized` is not returned. However, `POST /api/**` requires the `ADMIN` role. The USER role doesn't satisfy this rule, so Spring Security returns `403 Forbidden` — "I know who you are, but you don't have permission."

**Why others are wrong:**
- A) Authentication alone is not sufficient — authorization (the right role) is also required.
- B) 401 means the user is not authenticated at all (no credentials, or invalid credentials).
- D) 404 is for missing resources, not authorization failures.

</details>

---

### Q43. Why is BCrypt recommended for password hashing over MD5 or SHA-1?

- [ ] A) BCrypt is faster, making login quicker
- [ ] B) BCrypt is reversible so you can show users their password if they forget it
- [ ] C) BCrypt automatically salts passwords and has a configurable work factor, making brute-force and rainbow table attacks impractical
- [ ] D) BCrypt is the only algorithm supported by Spring Security

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) BCrypt automatically salts passwords and has a configurable work factor, making brute-force and rainbow table attacks impractical

**Explanation:** BCrypt generates a unique random salt per password, so two identical plaintext passwords produce different hashes. The configurable work factor (rounds) means the hashing time can be increased as hardware improves. MD5 and SHA-1 are fast and unsalted — trivially reversible with precomputed rainbow tables (OWASP A02).

**Why others are wrong:**
- A) BCrypt is intentionally *slower* than MD5/SHA-1 — that is a feature, not a bug.
- B) BCrypt is one-way — you cannot reverse it. Forgotten passwords require a reset, not a lookup.
- D) Spring Security supports multiple `PasswordEncoder` implementations, but BCrypt is the recommended one.

</details>

---

### Q44. **Fill-in-the-Blank:** The method `passwordEncoder._______(rawPassword, encodedHash)` returns `true` if the raw password matches the stored hash.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `matches`

**Explanation:** `passwordEncoder.matches("mypassword", "$2a$10$...")` runs the BCrypt algorithm on the raw password with the stored salt (extracted from the hash) and compares the result. It returns `true` if they match. This is the correct way to verify a login — never decrypt or compare plaintext.

</details>

---

### Q45. What is the difference between Authentication and Authorization?

- [ ] A) Authentication checks what you can do; Authorization checks who you are
- [ ] B) Authentication checks who you are; Authorization checks what you are allowed to do
- [ ] C) They are synonyms in Spring Security
- [ ] D) Authentication is for REST APIs; Authorization is for web pages

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Authentication checks who you are; Authorization checks what you are allowed to do

**Explanation:** Authentication = identity verification ("Is this really Alice?"). Authorization = permission check ("Is Alice allowed to DELETE this resource?"). Spring Security handles both: authentication filters verify credentials, authorization filters check `hasRole()`/`hasAnyRole()` rules. You cannot authorize without first authenticating.

</details>

---

### Q46. **True or False:** For a stateless REST API using JWT tokens, you should configure Spring Security to use `SessionCreationPolicy.STATELESS`.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** `STATELESS` tells Spring Security never to create or use an HTTP session (`HttpSession`). Since JWT-based APIs send a token on every request (not a session cookie), there is no need for server-side session storage. This makes the API horizontally scalable — any server instance can handle any request.

</details>

---

### Q47. What does `requestMatchers("/actuator/health").permitAll()` do?

- [ ] A) Requires the `ADMIN` role to access `/actuator/health`
- [ ] B) Disables the `/actuator/health` endpoint completely
- [ ] C) Allows any request (authenticated or not) to access `/actuator/health`
- [ ] D) Logs all requests to `/actuator/health`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) Allows any request (authenticated or not) to access `/actuator/health`

**Explanation:** `.permitAll()` means no authentication or authorization check is performed for that URL pattern. This is used for public endpoints like health checks, login, and registration. The request bypasses Spring Security's authentication filter entirely.

</details>

---

### Q48. **Code Prediction:** In this code, what is the value of `hash1 == hash2`?

```java
PasswordEncoder encoder = new BCryptPasswordEncoder();
String hash1 = encoder.encode("password123");
String hash2 = encoder.encode("password123");
```

- [ ] A) `true` — same input always produces same hash
- [ ] B) `false` — BCrypt generates a different salt each time, producing a different hash
- [ ] C) Depends on the BCrypt strength setting
- [ ] D) `null` — `encode()` returns null for duplicate inputs

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `false` — BCrypt generates a different salt each time, producing a different hash

**Explanation:** BCrypt automatically generates a new random salt each time `encode()` is called. Two calls with the same input produce two different hashes. However, `encoder.matches("password123", hash1)` returns `true` because `matches()` extracts the salt from the stored hash and re-runs the algorithm for comparison.

</details>

---

### Q49. Why is CSRF protection typically disabled for stateless REST APIs?

- [ ] A) CSRF protection breaks JSON serialization
- [ ] B) CSRF attacks exploit browser session cookies — stateless APIs using tokens are not vulnerable to CSRF
- [ ] C) Spring Security does not support CSRF for REST APIs
- [ ] D) Disabling CSRF improves performance by 50%

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) CSRF attacks exploit browser session cookies — stateless APIs using tokens are not vulnerable to CSRF

**Explanation:** CSRF (Cross-Site Request Forgery) tricks browsers into silently sending session-cookie-based requests to a victim site. Stateless APIs don't use session cookies — they use tokens (like JWT) in the `Authorization` header, which browsers never send automatically. Without the attack vector, CSRF protection is unnecessary overhead.

</details>

---

### Q50. **Fill-in-the-Blank:** The `InMemoryUserDetailsManager` is used for _______ and _______ only — real applications load users from a database using a custom `UserDetailsService`.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** **learning** (development) and **testing** only

**Explanation:** `InMemoryUserDetailsManager` stores users in application memory. Restarting the app loses all users. It has no persistence and cannot scale. For production, you implement a `UserDetailsService` that calls your `UserRepository` to load users from a database (PostgreSQL in Project 2).

</details>

---

### Q51. **True or False:** Spring Security's Security Filter Chain runs before the `DispatcherServlet`, meaning an unauthorized request never reaches your `@RestController`.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** Spring Security operates as a servlet filter chain — it intercepts every HTTP request at the Servlet container level, before `DispatcherServlet` (and therefore before any `@Controller` or `@RestController` method). An unauthorized request is rejected at the filter layer with `401` or `403` — your business logic is never invoked.

</details>

---

### Q52. Which Spring Security annotation enables web security processing on a `@Configuration` class?

- [ ] A) `@EnableSecurity`
- [ ] B) `@EnableWebSecurity`
- [ ] C) `@SecurityConfig`
- [ ] D) `@ActivateSecurity`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `@EnableWebSecurity`

**Explanation:** `@EnableWebSecurity` is placed alongside `@Configuration` on your security configuration class. It activates Spring Security's web security support and applies the `SecurityFilterChain` bean you define. Without it, your `SecurityFilterChain` bean may not be picked up correctly.

</details>

---

## Part 5: Cross-Topic Integration

---

### Q53. **Scenario:** A trainee calls `GET http://localhost:8080/api/books` from their Angular app at `http://localhost:4200`. They get a CORS error. They add Spring Security to the project. Which of the following is the **most likely** cause if the CORS error persists after adding `WebMvcConfigurer` CORS config?

- [ ] A) The `@GetMapping` annotation is missing
- [ ] B) Spring Security's filter chain intercepts the request and strips CORS headers before the response reaches the browser
- [ ] C) `application.yml` doesn't have the `cors.enabled=true` property
- [ ] D) Postman is blocking the request

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Spring Security's filter chain intercepts the request and strips CORS headers before the response reaches the browser

**Explanation:** When Spring Security is present, its filter chain runs *before* Spring MVC's `CorsFilter`. If security rejects the request with `401`, the CORS headers never get added to the response, and the browser sees a CORS error instead of an auth error. The fix is to configure CORS within Spring Security's `HttpSecurity` as well.

</details>

---

### Q54. **Scenario:** Your `POST /api/products` endpoint has `@Valid @RequestBody Product product`. A client sends `{"name": "", "price": -10}`. Which exception is thrown, and which HTTP status should the `GlobalExceptionHandler` return?

- [ ] A) `ResourceNotFoundException` → 404
- [ ] B) `MethodArgumentNotValidException` → 400
- [ ] C) `HttpMessageNotReadableException` → 400
- [ ] D) `IllegalArgumentException` → 500

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `MethodArgumentNotValidException` → 400

**Explanation:** When `@Valid` is present and the request body fails Bean Validation constraints (e.g., `@NotBlank` on `name`, `@Min(0)` on `price`), Spring automatically throws `MethodArgumentNotValidException`. Your `GlobalExceptionHandler` should map this to `400 Bad Request` and extract the field-level error details from `ex.getBindingResult().getFieldErrors()`.

</details>

---

### Q55. **True or False:** The `DispatcherServlet`, `SecurityFilterChain`, and `@RestControllerAdvice` all operate at the same level in the Spring request processing pipeline.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** They operate at different levels. The `SecurityFilterChain` (servlet filter level) runs first — before `DispatcherServlet`. The `DispatcherServlet` runs next — routing the request to a `@RestController` method. `@RestControllerAdvice` intercepts exceptions that escape controller methods — it runs last, inside the MVC layer. The order matters: security → dispatching → business logic → exception handling.

</details>

---

### Q56. **Scenario:** You want `GET /api/books` to return `200 OK` to unauthenticated users, but `POST /api/books` to require the `ADMIN` role. Which configuration correctly achieves this?

```java
// Option A
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/books").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/books").hasRole("ADMIN")
    .anyRequest().authenticated()
)

// Option B
.authorizeHttpRequests(auth -> auth
    .anyRequest().authenticated()
    .requestMatchers(HttpMethod.GET, "/api/books").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/books").hasRole("ADMIN")
)
```

- [ ] A) Option A
- [ ] B) Option B
- [ ] C) Both are equivalent
- [ ] D) Neither — you must use `@CrossOrigin` for this

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** A) Option A

**Explanation:** Spring Security evaluates `authorizeHttpRequests` rules **top to bottom, first match wins**. In Option A, specific rules for GET and POST come first, followed by the catch-all. In Option B, `.anyRequest().authenticated()` would match first for every request — the specific rules below it would never be reached. Always put specific rules before the catch-all.

</details>

---

### Q57. **Fill-in-the-Blank:** The annotation `@CrossOrigin` without any attributes defaults to `allowedOrigins = "_______"`, which permits requests from any origin.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `"*"` (a wildcard / asterisk)

**Explanation:** By default, `@CrossOrigin` with no attributes sets `allowedOrigins = "*"`, allowing requests from any origin. This is acceptable for fully public, read-only APIs but is a security risk for authenticated APIs. Always specify explicit origins in production.

</details>

---

### Q58. **Code Prediction:** A request to `DELETE /api/products/5` is made by a user with role `USER` on this security config. What is the response, and from which Spring layer is it generated?

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

- [ ] A) `204 No Content` — from `ProductController.delete()`
- [ ] B) `403 Forbidden` — from Spring Security's `AuthorizationFilter`
- [ ] C) `401 Unauthorized` — from `GlobalExceptionHandler`
- [ ] D) `404 Not Found` — from `GlobalExceptionHandler`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `403 Forbidden` — from Spring Security's `AuthorizationFilter`

**Explanation:** The `USER` role does not satisfy `.hasRole("ADMIN")` for DELETE requests. Spring Security's `AuthorizationFilter` (inside the Security Filter Chain) rejects the request with `403 Forbidden` before it ever reaches `ProductController` or `GlobalExceptionHandler`. The `GlobalExceptionHandler` only handles exceptions from controller methods — not Security Filter Chain rejections.

</details>

---

### Q59. **True or False:** Adding `@Valid` to a `@RequestBody` parameter is sufficient to return a useful error message — no `GlobalExceptionHandler` is needed.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** `@Valid` triggers Bean Validation and throws `MethodArgumentNotValidException` when constraints are violated. Without a `GlobalExceptionHandler`, Spring Boot returns a generic, unformatted error response. A `@RestControllerAdvice` with `@ExceptionHandler(MethodArgumentNotValidException.class)` is needed to extract field-level error details and return a clean `400 Bad Request` with a structured `ErrorResponse` body.

</details>

---

### Q60. **Scenario:** You are building a library management API. Rank the following in the correct order of execution for an authenticated `POST /api/books` request:

1. `@Transactional` method in `BookService` saves the book
2. Spring Security's `AuthorizationFilter` checks the user's role
3. `BookController.create()` is invoked by `DispatcherServlet`
4. Jackson serializes the `Book` response to JSON
5. `GlobalExceptionHandler` (not invoked — no exception)

- [ ] A) 1 → 2 → 3 → 4 → 5
- [ ] B) 2 → 3 → 1 → 4 → 5
- [ ] C) 3 → 2 → 1 → 5 → 4
- [ ] D) 5 → 2 → 3 → 1 → 4

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) 2 → 3 → 1 → 4 → 5

**Explanation:** The correct Spring execution order:
1. **Security Filter Chain (AuthorizationFilter)** checks the user's role — before DispatcherServlet.
2. **DispatcherServlet** routes to `BookController.create()`.
3. **`BookController.create()`** calls the service method.
4. **`@Transactional` `BookService`** saves to the database.
5. **Jackson** serializes the returned `Book` to JSON.
6. **`GlobalExceptionHandler`** is NOT invoked (no exception occurred).

</details>

---

### Q61. **Fill-in-the-Blank:** In a Spring Boot REST API, the recommended annotation for a global exception handler that automatically serializes error objects to JSON is `@_______ControllerAdvice`.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `@RestControllerAdvice`

**Explanation:** `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. The `@ResponseBody` addition means every return value from every `@ExceptionHandler` method is automatically serialized to JSON — the same way `@RestController` works for regular controller methods.

</details>

---

### Q62. **True or False:** A `GET` request with the header `Authorization: Bearer eyJhbGciOi...` will trigger a CORS preflight request.

- [ ] True
- [ ] False

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** Even though the HTTP method is `GET` (normally "simple"), adding a custom header like `Authorization` makes the request "non-simple" by the CORS specification. The `Authorization` header is not in the set of CORS-safe headers, so the browser will send a preflight `OPTIONS` request first to check if the server permits `Authorization` in cross-origin requests.

</details>

---

### Q63. **Scenario:** Your team wants to return `404` when a product is not found, but also keep controller methods clean (no `Optional` handling in the controller). Which combination achieves this cleanly?

- [ ] A) Annotate `ProductController` with `@ResponseStatus(NOT_FOUND)` on every method
- [ ] B) Throw a custom `ProductNotFoundException` in the service layer; catch it in `GlobalExceptionHandler` and return `404`
- [ ] C) Return `null` from the service — Spring converts `null` to `404` automatically
- [ ] D) Use `ResponseEntity.notFound()` in every service method

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Throw a custom `ProductNotFoundException` in the service layer; catch it in `GlobalExceptionHandler` and return `404`

**Explanation:** The service throws a domain-specific `ProductNotFoundException` (unchecked). The controller doesn't need try-catch or `Optional` — it stays clean. `GlobalExceptionHandler` intercepts the exception globally and returns a standardized `404 ErrorResponse`. This is the separation of concerns pattern.

**Why others are wrong:**
- A) `@ResponseStatus` on the controller affects the entire controller, not specific "not found" cases, and doesn't allow custom error bodies.
- C) Returning `null` from a `@RestController` method produces a `200 OK` with an empty body, not a `404`.
- D) `ResponseEntity` belongs in the controller layer, not the service layer.

</details>

---

*End of Quiz — 63 Questions*

> **Score Guide:**
> - **55–63** 🏆 Excellent — ready for Project 2
> - **43–54** ✅ Good — review the sections you missed
> - **30–42** 📖 Developing — re-read the written content and retry
> - **< 30** 🔄 Needs review — work through the exercises before retrying
