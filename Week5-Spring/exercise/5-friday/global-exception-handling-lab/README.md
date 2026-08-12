# Lab: Add Global Exception Handling

> **Mode:** A — Code Lab
> **Estimated Time:** 2–3 hours
> **Reference:** `content/Week5-Spring/written/5-friday/global-exception-handling.md`
> **Demo Reference:** `content/Week5-Spring/demos/5-friday/code/GlobalExceptionHandler.java`

---

## Context

You have a working Spring Boot REST API (from the Spring MVC lab, or use the starter code here). The API has one serious problem: when something goes wrong, the responses are a mess.

Try hitting `GET /api/books/999` right now in Postman. You will get something like:
```json
{
  "timestamp": "...",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/books/999"
}
```

**Status is wrong** (should be 404). **Message is useless**. **No field details**. Your Angular frontend has no idea what to do with this.

Your job is to add a global exception handling layer so every error from this API returns a consistent, meaningful, correctly-coded JSON response.

---

## Setup

1. Navigate to `starter_code/`.
2. Open the project in IntelliJ.
3. The project is the completed `BookController` from the Spring MVC lab — but **intentionally broken** in two ways:
   - There is **no** `GlobalExceptionHandler` class yet.
   - The service throws real exceptions (not just returning `Optional.empty()`).
4. Run the app and reproduce the broken error responses in Postman before you fix them.

---

## Core Tasks

### Task 1 — Create the `ErrorResponse` POJO (15 min)

Create a new class `ErrorResponse` in the `exception` package with:
- `int status`
- `String message`
- `Instant timestamp` — set automatically in the constructor to `Instant.now()`

Requirements:
- All fields must have getters (Jackson needs them to serialize to JSON).
- No setters needed — this object is immutable after construction.
- Constructor: `ErrorResponse(int status, String message)`

**Expected JSON output shape:**
```json
{
  "status": 404,
  "message": "Book with id 999 was not found.",
  "timestamp": "2026-08-12T16:00:00Z"
}
```

---

### Task 2 — Create Custom Exception Classes (20 min)

Create the following exceptions in the `exception` package:

**`BookNotFoundException`**
- Extends `RuntimeException`
- Constructor: `BookNotFoundException(Long id)` — message: `"Book with id {id} was not found."`

**`DuplicateIsbnException`**
- Extends `RuntimeException`
- Constructor: `DuplicateIsbnException(String isbn)` — message: `"A book with ISBN {isbn} already exists."`

After creating them, update `BookServiceImpl` to **throw** `BookNotFoundException` in `findById` when the book is not found, instead of returning `Optional.empty()`.

> **Why RuntimeException?** Unchecked exceptions do not require `try-catch` at the call site. Spring's `@Transactional` also rolls back automatically on unchecked exceptions.

---

### Task 3 — Create the `GlobalExceptionHandler` (30 min)

Create `GlobalExceptionHandler` in the `exception` package. This class must:

1. Be annotated correctly for global REST API exception handling (check the reading for the right annotation — hint: it's not just `@ControllerAdvice`).

2. Handle **all four** of the following cases with separate `@ExceptionHandler` methods:

| Exception | HTTP Status | When it occurs |
|---|---|---|
| `BookNotFoundException` | 404 Not Found | Book id does not exist |
| `DuplicateIsbnException` | 409 Conflict | Duplicate ISBN on create |
| `MethodArgumentNotValidException` | 400 Bad Request | `@Valid` fails on request body |
| `Exception` (catch-all) | 500 Internal Server Error | Any unhandled exception |

3. Every handler must return a `ResponseEntity<ErrorResponse>`.

4. The catch-all handler must **not** expose the real exception message — return the generic text: `"An unexpected error occurred. Please try again later."`

---

### Task 4 — Test All Error Cases in Postman (30 min)

Create a Postman collection named `Book API — Error Cases` with the following requests. Document the actual response for each:

| Request | Expected Status | Expected `message` |
|---|---|---|
| `GET /api/books/999` | 404 | "Book with id 999 was not found." |
| `POST /api/books` with `{"isbn":"978-0132350884"}` | 409 | "A book with ISBN ... already exists." |
| `POST /api/books` with `{"title":""}` (if @Valid added) | 400 | "Validation failed: title: must not be blank" |
| `GET /api/books/abc` (type mismatch) | 400 | "Invalid value 'abc' for parameter 'id'" |

**Before your `GlobalExceptionHandler` existed:** screenshot or note the broken response.
**After:** screenshot the corrected response. Include both in `REFLECTION.md`.

---

### Task 5 — Reflection Questions (20 min)

Answer in `REFLECTION.md`:

1. What is the difference between `@ControllerAdvice` and `@RestControllerAdvice`? When would you use each?

2. You defined a `BookNotFoundException` handler AND a catch-all `Exception` handler. If `BookNotFoundException` is thrown, which handler runs? Why doesn't the catch-all run instead?

3. The catch-all handler returns a generic "An unexpected error occurred" message rather than the real exception message. Why? Give a concrete example of what could go wrong if you returned `ex.getMessage()` directly for every exception.

---

## Definition of Done

- [ ] `ErrorResponse` POJO with `status`, `message`, `timestamp` fields and getters
- [ ] `BookNotFoundException` and `DuplicateIsbnException` custom exceptions exist
- [ ] `GlobalExceptionHandler` with `@RestControllerAdvice` handles all 4 cases
- [ ] `GET /api/books/999` returns `404` with `ErrorResponse` JSON (not `500`)
- [ ] Catch-all returns generic message, not the real exception text
- [ ] Postman collection `Book API — Error Cases` demonstrates all 4 error scenarios
- [ ] `REFLECTION.md` answers all 3 questions

---

## Stretch Goal

Add a `@ExceptionHandler` for `HttpRequestMethodNotAllowedException`. This is thrown when a client sends a request with an HTTP method the endpoint doesn't support (e.g., `DELETE /api/books/search`). Return a `405 Method Not Allowed` response with a clear message.
