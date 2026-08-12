# Lab: Build a Spring MVC REST Controller

> **Mode:** A — Code Lab
> **Estimated Time:** 3–4 hours
> **Reference:** `content/Week5-Spring/written/5-friday/spring-mvc-web-layer.md`
> **Demo Reference:** `content/Week5-Spring/demos/5-friday/code/ProductController.java`

---

## Context

You have spent this week wiring up beans (Monday), configuring Spring Boot (Wednesday), and persisting data with JPA (Thursday). Today you connect the HTTP layer.

Your task is to build a complete REST controller for a **Book** resource on top of a provided service stub. By the end of this lab, any REST client (Postman, curl, or a browser) will be able to perform full CRUD operations against your API.

---

## Setup

1. Navigate to `starter_code/`.
2. Open the project in IntelliJ.
3. The project includes:
   - `Book.java` — a JPA entity with `id`, `title`, `author`, and `isbn` fields
   - `BookService.java` — a service interface with all CRUD methods already defined
   - `BookServiceImpl.java` — a stub implementation returning hardcoded data (no database needed for this lab)
   - `BookController.java` — **empty** — this is where all your work goes

> Do **not** modify `Book.java`, `BookService.java`, or `BookServiceImpl.java`.

---

## Core Tasks

### Task 1 — Annotate the Controller (10 min)

Open `BookController.java`. It is currently a plain Java class.

1. Annotate it as a Spring REST controller.
2. Map it to the base path `/api/books`.
3. Inject `BookService` using **constructor injection** (no `@Autowired` on the field).

**Verify:** Run the application. Navigate to `http://localhost:8080/api/books` in a browser. You should see an empty JSON array `[]` (not a 404 or 500).

---

### Task 2 — Implement `GET` Endpoints (20 min)

Implement the following endpoints:

| Method | URL | Returns | Success Status |
|---|---|---|---|
| GET | `/api/books` | All books | 200 OK |
| GET | `/api/books/{id}` | Single book by id | 200 OK / 404 if not found |
| GET | `/api/books/search?author=Tolkien` | Books by author (query param) | 200 OK |

**Requirements:**
- Use `@PathVariable` to bind `{id}`.
- Use `@RequestParam` for the `author` query parameter. Make it **optional** with a default of `""` (empty string — return all books when not provided).
- Use `ResponseEntity<Book>` for the single-book endpoint so you can return `404 Not Found` when the book does not exist.

**Test in Postman:**
```
GET http://localhost:8080/api/books          → 200, list of books
GET http://localhost:8080/api/books/1        → 200, single book
GET http://localhost:8080/api/books/999      → 404, no body
GET http://localhost:8080/api/books/search?author=Tolkien  → 200, filtered list
```

---

### Task 3 — Implement `POST` (20 min)

Implement the create endpoint:

| Method | URL | Request Body | Returns | Success Status |
|---|---|---|---|---|
| POST | `/api/books` | `{"title":"...", "author":"...", "isbn":"..."}` | Created book | 201 Created |

**Requirements:**
- Use `@RequestBody` to deserialize the JSON body into a `Book` object.
- Return `201 Created` using `ResponseEntity.created(location).body(saved)`.
- The `Location` header must be set to `/api/books/{id}` of the newly created book.

**Test in Postman:**
```
POST http://localhost:8080/api/books
Content-Type: application/json

{
  "title": "The Fellowship of the Ring",
  "author": "J.R.R. Tolkien",
  "isbn": "978-0618346257"
}
```
→ Expected: `201 Created` with `Location: /api/books/4` (or the next id) and the book JSON in the body.

---

### Task 4 — Implement `PUT` and `DELETE` (30 min)

| Method | URL | Request Body | Returns | Success Status |
|---|---|---|---|---|
| PUT | `/api/books/{id}` | Full book JSON | Updated book | 200 OK / 404 |
| DELETE | `/api/books/{id}` | — | No body | 204 No Content |

**Requirements:**
- `PUT`: Use both `@PathVariable` and `@RequestBody`. Return `404` if the id does not exist.
- `DELETE`: Return `204 No Content` (not `200 OK` — there is no body to return).

**Test in Postman:**
```
PUT http://localhost:8080/api/books/1
Content-Type: application/json
{ "title": "The Hobbit (Revised)", "author": "Tolkien", "isbn": "978-0547928227" }
→ 200 with updated book

DELETE http://localhost:8080/api/books/1
→ 204, empty body

GET http://localhost:8080/api/books/1
→ 404 (it was deleted)
```

---

### Task 5 — Reflection Questions (20 min)

Answer the following in a file named `REFLECTION.md` in the project root:

1. What would happen if you annotated `BookController` with `@Controller` instead of `@RestController`? How would your GET endpoints need to change to still return JSON?

2. You have both `@PathVariable Long id` and `@RequestBody Book book` on your `PUT` method. The `book` object might also contain an `id` field from the JSON body. Which id should you use to identify the resource to update, and why?

3. In your `DELETE` endpoint, why is `204 No Content` the more appropriate status than `200 OK`? Under what circumstances would a `200 OK` with a body be acceptable for a delete operation?

---

## Definition of Done

- [ ] All 7 HTTP endpoints respond with the correct status codes
- [ ] `GET /api/books/999` returns `404 Not Found` (not `500`)
- [ ] `POST /api/books` returns `201 Created` with a valid `Location` header
- [ ] `DELETE /api/books/1` returns `204 No Content` with no body
- [ ] Constructor injection is used (no `@Autowired` on fields)
- [ ] `REFLECTION.md` contains answers to all 3 questions

---

## Stretch Goal

Add a `PATCH /api/books/{id}` endpoint that accepts a **partial** update — only update the fields that are present in the request body (i.e., if `author` is not in the JSON, don't overwrite it). How does your implementation differ from `PUT`?
