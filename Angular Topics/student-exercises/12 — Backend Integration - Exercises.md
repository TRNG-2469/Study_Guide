# Phase 12 — Backend Integration: Practice Exercises

**Prerequisites:** Completed Phase 12 lesson on HttpClient and Spring Boot  
**Application Context:** Student Management System  
**Spring Boot API:** `http://localhost:8080/api/students`  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Full CRUD with `HttpClient`

**Objective:** Rewrite `StudentService` to fetch real data from the Spring Boot REST API using `HttpClient`, and wire every operation to the UI.

### Steps

**1. Ensure `provideHttpClient` is registered in `app.config.ts`:**

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
  ],
};
```

**2. Configure the API base URL in the environment file:**

```typescript
// src/environments/environment.development.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
};
```

**3. Rewrite `StudentService` with `HttpClient`:**

```typescript
// student.service.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError, finalize } from 'rxjs/operators';
import { environment } from '../../environments/environment.development';
import { Student } from '../models/student.model';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/students`;

  // Local signal cache — kept in sync after every HTTP mutation
  private _students = signal<Student[]>([]);
  readonly students  = this._students.asReadonly();
  readonly totalCount = computed(() => this._students().length);
  readonly activeCount = computed(() => this._students().filter(s => s.status === 'active').length);

  // Loading flag shown in the UI
  readonly loading = signal(false);

  /** Load all students from the API into the local cache */
  loadAll(): void {
    this.loading.set(true);
    this.http.get<Student[]>(this.base).pipe(
      tap(data => this._students.set(data)),
      catchError(this.handleError),
      finalize(() => this.loading.set(false)),
    ).subscribe();
  }

  /** Load with a search query */
  search(query: string): void {
    const params = new HttpParams().set('name', query);
    this.loading.set(true);
    this.http.get<Student[]>(this.base, { params }).pipe(
      tap(data => this._students.set(data)),
      catchError(this.handleError),
      finalize(() => this.loading.set(false)),
    ).subscribe();
  }

  /** Add a student — returns Observable so the caller knows when it's done */
  add$(student: Omit<Student, 'id'>): Observable<Student> {
    return this.http.post<Student>(this.base, student).pipe(
      tap(created => this._students.update(list => [...list, created])),
      catchError(this.handleError),
    );
  }

  /** Update a student */
  update$(id: number, changes: Partial<Student>): Observable<Student> {
    return this.http.put<Student>(`${this.base}/${id}`, changes).pipe(
      tap(updated => this._students.update(list =>
        list.map(s => s.id === id ? updated : s)
      )),
      catchError(this.handleError),
    );
  }

  /** Delete a student */
  delete$(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`).pipe(
      tap(() => this._students.update(list => list.filter(s => s.id !== id))),
      catchError(this.handleError),
    );
  }

  private handleError(err: HttpErrorResponse): Observable<never> {
    let message = 'An unexpected error occurred.';
    if (err.status === 0)   message = 'Cannot connect to the server.';
    if (err.status === 404) message = 'Student not found.';
    if (err.status === 409) message = 'A student with this data already exists.';
    if (err.status >= 500)  message = 'Server error. Please try again later.';
    console.error('[StudentService]', err);
    return throwError(() => new Error(message));
  }
}
```

**4. Wire `loadAll()` in `StudentListComponent`:**

```typescript
ngOnInit(): void {
  this.svc.loadAll();
}
```

```html
<!-- student-list.component.html -->
@if (svc.loading()) {
  <div class="loading-bar">Fetching students from server…</div>
}

@for (student of svc.students(); track student.id) {
  <div class="row">
    {{ student.name }} · {{ student.course }}
    <button (click)="delete(student.id)">Remove</button>
  </div>
} @empty {
  @if (!svc.loading()) {
    <p>No students found.</p>
  }
}
```

```typescript
delete(id: number): void {
  this.svc.delete$(id).subscribe({
    error: (err) => alert(err.message),
  });
}
```

### What to Verify
- The list loads data from your running Spring Boot application.
- Adding a student via the form persists to the database and the card appears immediately (optimistic update via `tap`).
- Deleting a student removes it from both the UI and the database.
- Stopping Spring Boot and reloading shows the "Cannot connect to the server" error message.

---

## Exercise 2: HTTP Interceptor for Loading and Error Handling

**Objective:** Write an `HttpInterceptorFn` that automatically sets a loading flag and formats error responses globally.

### Steps

**1. Create the interceptor:**

```bash
ng g interceptor interceptors/http-logger
```

**2. Implement the interceptor:**

```typescript
// interceptors/http-logger.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { tap } from 'rxjs/operators';

export const httpLoggerInterceptor: HttpInterceptorFn = (req, next) => {
  const start = performance.now();

  console.log(`[HTTP] → ${req.method} ${req.url}`);

  return next(req).pipe(
    tap(event => {
      // HttpResponse arrives here on success
      const elapsed = Math.round(performance.now() - start);
      console.log(`[HTTP] ← ${req.url} (${elapsed}ms)`);
    }),
    catchError((err: HttpErrorResponse) => {
      console.error(`[HTTP Error] ${req.method} ${req.url} → ${err.status} ${err.statusText}`);
      return throwError(() => err);
    }),
  );
};
```

**3. Register the interceptor:**

```typescript
// app.config.ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { httpLoggerInterceptor } from './interceptors/http-logger.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([httpLoggerInterceptor])),
  ],
};
```

### What to Verify
- Open the browser console. Every HTTP request logs its method and URL.
- Every response logs the URL and elapsed time in milliseconds.
- A 404 or 500 response logs the status code in the error line.
- The interceptor requires zero changes in any component or service — it works transparently.

### Challenge
Extend the interceptor to add an `Authorization` header to every outgoing request:

```typescript
const authReq = req.clone({
  setHeaders: { Authorization: `Bearer demo-token` }
});
return next(authReq).pipe(/* ... */);
```

Verify the header appears in DevTools → Network → Request Headers.

---

## Exercise 3: End-to-End CRUD Flow Verification

**Objective:** Trace a complete create → read → update → delete cycle through the Angular front end and Spring Boot back end, and verify each step in both the UI and the database.

### Checklist

Run through each step with your Spring Boot server running and a PostgreSQL database connected.

**Step 1 — Read:**
- [ ] Open `/students` in the browser.
- [ ] Confirm the student list loads from `GET /api/students`.
- [ ] Check the Network tab — status 200, JSON array returned.

**Step 2 — Create:**
- [ ] Navigate to `/students/new`.
- [ ] Fill in the form and submit.
- [ ] Confirm a `POST /api/students` request fires in the Network tab (status 201).
- [ ] Return to `/students` and confirm the new student appears.
- [ ] Query your database: `SELECT * FROM students ORDER BY id DESC LIMIT 1;`

**Step 3 — Update:**
- [ ] Click **Edit** on any student.
- [ ] Change the name and save.
- [ ] Confirm a `PUT /api/students/:id` request fires (status 200).
- [ ] Refresh the page — the updated name persists.
- [ ] Query the database to verify the change.

**Step 4 — Delete:**
- [ ] Click **Remove** on a student.
- [ ] Confirm a `DELETE /api/students/:id` request fires (status 204).
- [ ] Confirm the card disappears from the UI immediately.
- [ ] Query the database — the row is gone.

**Step 5 — Error Handling:**
- [ ] Stop the Spring Boot server.
- [ ] Reload the Angular app — confirm the "Cannot connect to the server" message appears.
- [ ] Restart the server — confirm the list loads again without a page refresh (use `loadAll()` on a retry button).

### Bonus: Add a Retry Button

```typescript
// In StudentListComponent
retryLoad(): void {
  this.svc.loadAll();
}
```

```html
@if (!svc.loading() && svc.students().length === 0) {
  <div class="retry-panel">
    <p>Could not load students.</p>
    <button (click)="retryLoad()">Retry</button>
  </div>
}
```

### Reflection Questions
1. Why does the Angular app cache the student list in a signal rather than re-fetching on every navigation?
2. What would break if you used `providedIn: 'root'` for a service but forgot to call `provideHttpClient()` in `app.config.ts`?
3. How does `tap()` differ from `map()` in an RxJS pipeline?
4. What is the advantage of `finalize()` over putting `loading.set(false)` in both the `next` and `error` callbacks?

