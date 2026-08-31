# Phase 11 — Async Programming

## What You Will Learn

In Phase 12 you will connect Angular to your Spring REST API. Every HTTP call is **asynchronous** — the response does not arrive immediately; the browser sends the request and continues running. When the response eventually arrives, Angular needs to know what to do with it.

Angular uses **RxJS Observables** for all HTTP communication. This phase gives you the foundation to work with them confidently.

By the end of this phase you will understand:
- Why async programming matters and how it evolved
- What an Observable is and how it differs from a Promise
- The key RxJS operators you will use every day
- How `AsyncPipe` handles subscriptions in templates
- How `Subject` and `BehaviorSubject` create custom event streams
- How to bridge Observables to Signals with `toSignal()`

---

## 1. Why Async Programming?

Web applications make network requests that take time — milliseconds to seconds. If the browser blocked and waited for every response, the UI would freeze on every HTTP call.

Instead, JavaScript (and therefore Angular) uses an **event loop** — the browser fires off a request and continues processing user interactions. When the response arrives, a callback is queued and executed.

### The Evolution: Callbacks → Promises → Observables

#### Callbacks (Old, Problematic)

```javascript
// Callback — deeply nested, hard to read
getStudents(function(students) {
  getCourses(function(courses) {
    getEnrolments(function(enrolments) {
      // "Callback hell" — hard to read, hard to handle errors
    });
  });
});
```

#### Promises (Better)

```typescript
// Promise — flat chain, one value, not cancellable
fetch('/api/students')
  .then(response => response.json())
  .then(students => console.log(students))
  .catch(error => console.error(error));
```

A Promise represents a **single future value**. It resolves once and is done. You cannot cancel it, and it cannot emit multiple values over time.

#### Observables (Most Powerful)

```typescript
// Observable — lazy, cancellable, multiple values, composable
this.http.get<Student[]>('/api/students')
  .pipe(
    map(students => students.filter(s => s.status === 'active')),
    catchError(err => of([]))
  )
  .subscribe(students => this.students = students);
```

An Observable can:
- Emit **zero, one, or many values** over time
- Be **cancelled** (unsubscribed) at any time
- Have **operators** chained to transform the data stream
- Be **lazy** — nothing happens until something subscribes

---

## 2. Observable Fundamentals

### Creating and Subscribing

```typescript
import { Observable, of, from, interval } from 'rxjs';

// of() — emits a fixed list of values synchronously then completes
const numbers$ = of(1, 2, 3, 4, 5);

numbers$.subscribe({
  next: value => console.log('Value:', value),    // Called for each emitted value
  error: err  => console.error('Error:', err),    // Called if an error occurs
  complete: () => console.log('Done!'),           // Called when the stream ends
});
// Output: Value: 1  Value: 2  Value: 3  Value: 4  Value: 5  Done!

// from() — converts a Promise or array to an Observable
const fromArray$ = from([10, 20, 30]);

// interval() — emits an incrementing number every n milliseconds
const tick$ = interval(1000);   // Emits 0, 1, 2, 3... every second (never completes)
```

> **Convention:** Observable variables are suffixed with `$` (the dollar sign). This is a widely adopted convention — not enforced by Angular but universally recognised.

### The Subscription

Calling `.subscribe()` is what starts the Observable. The return value is a `Subscription` object — hold onto it so you can unsubscribe:

```typescript
const sub = interval(1000).subscribe(n => console.log(n));

// Later, when the component is destroyed:
sub.unsubscribe();   // Stops the interval, prevents memory leaks
```

---

## 3. The `pipe()` Method and Operators

Raw Observables are rarely used directly. You transform them with **operators** inside a `.pipe()` chain:

```typescript
observable$.pipe(
  operator1(),
  operator2(),
  operator3()
).subscribe(...)
```

Each operator receives the stream, transforms it, and passes the result to the next operator. This is analogous to Java Streams: `list.stream().filter(...).map(...).collect(...)`.

### The Operators You Will Use Most

#### `map()` — Transform Each Value

```typescript
import { map } from 'rxjs/operators';

this.http.get<Student[]>('/api/students')
  .pipe(
    map(students => students.filter(s => s.status === 'active'))
  )
  .subscribe(activeStudents => this.students = activeStudents);
```

#### `filter()` — Emit Only Matching Values

```typescript
import { filter } from 'rxjs/operators';

searchInput$.pipe(
  filter(term => term.length >= 3)   // Only process terms with 3+ characters
).subscribe(term => this.search(term));
```

#### `tap()` — Side Effects Without Transforming

```typescript
import { tap } from 'rxjs/operators';

this.http.get<Student[]>('/api/students')
  .pipe(
    tap(students => console.log('Received:', students.length)),  // Log for debugging
    map(students => students.sort((a, b) => a.name.localeCompare(b.name)))
  )
  .subscribe(sorted => this.students = sorted);
```

`tap()` is like a "peek" — it runs a side effect without changing the stream. Common uses: logging, setting loading states.

#### `catchError()` — Handle Errors Gracefully

```typescript
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

this.http.get<Student[]>('/api/students')
  .pipe(
    catchError(err => {
      console.error('Failed to load students:', err);
      return of([]);    // Return an empty array so the app continues working
    })
  )
  .subscribe(students => this.students = students);
```

`catchError()` intercepts an error in the stream and lets you recover by returning a new Observable. Without it, an unhandled error would terminate the subscription.

#### `switchMap()` — Cancel the Previous, Start a New Request

```typescript
import { switchMap } from 'rxjs/operators';

// When the user types, cancel any in-flight search and start a new one
searchTerm$.pipe(
  switchMap(term => this.http.get<Student[]>(`/api/students/search?q=${term}`))
).subscribe(results => this.students = results);
```

`switchMap()` is essential for search-as-you-type — if the user types "al" then "ali", you do not want the response for "al" to overwrite the response for "ali". `switchMap()` cancels the "al" request when "ali" is typed.

#### `debounceTime()` — Wait for the User to Stop Typing

```typescript
import { debounceTime } from 'rxjs/operators';

searchTerm$.pipe(
  debounceTime(300),          // Wait 300ms after the last keystroke
  switchMap(term => this.http.get<Student[]>(`/api/students/search?q=${term}`))
).subscribe(results => this.students = results);
```

Without `debounceTime`, every keystroke fires an HTTP request. With it, Angular waits until the user pauses typing before sending.

#### `distinctUntilChanged()` — Skip Identical Consecutive Values

```typescript
import { distinctUntilChanged } from 'rxjs/operators';

searchTerm$.pipe(
  debounceTime(300),
  distinctUntilChanged(),    // Don't re-search if the term hasn't changed
  switchMap(term => this.http.get<Student[]>(`/api/students/search?q=${term}`))
).subscribe(...)
```

---

## 4. `AsyncPipe` — Subscribing in Templates

`AsyncPipe` subscribes to an Observable (or Promise) in the template and returns the latest emitted value. When the component is destroyed, it **automatically unsubscribes** — no `ngOnDestroy` cleanup needed.

### Without AsyncPipe

```typescript
// Component
students: Student[] = [];
private sub!: Subscription;

ngOnInit(): void {
  this.sub = this.studentService.getAll$().subscribe(s => this.students = s);
}

ngOnDestroy(): void {
  this.sub.unsubscribe();   // Must remember to do this
}
```

```html
<!-- Template -->
@for (student of students; track student.id) { ... }
```

### With AsyncPipe

```typescript
// Component — much simpler
students$ = this.studentService.getAll$();
```

```html
<!-- Template -->
@for (student of (students$ | async) ?? []; track student.id) { ... }
```

`AsyncPipe` handles the full lifecycle: subscribe on init, unsubscribe on destroy. The `?? []` provides a fallback empty array while the Observable has not emitted yet.

### `async` with `@if` — the Classic Pattern

```html
@if (students$ | async; as students) {
  <!-- 'students' holds the latest emitted value -->
  <p>{{ students.length }} students</p>
  @for (s of students; track s.id) {
    <app-student-card [student]="s"></app-student-card>
  }
} @else {
  <p>Loading...</p>
}
```

The `as students` syntax assigns the unwrapped value to a local variable. The `@else` block shows while the Observable has not yet emitted.

---

## 5. `Subject` and `BehaviorSubject`

An `Observable` is **cold** — it only produces values when subscribed. A `Subject` is both an Observable **and** an Observer — you can push values into it manually, and anything subscribed receives them.

### `Subject`

```typescript
import { Subject } from 'rxjs';

const clicks$ = new Subject<void>();

clicks$.subscribe(() => console.log('Clicked!'));

// Push a value in manually
clicks$.next();   // Logs "Clicked!"
clicks$.next();   // Logs "Clicked!"
```

**Use case:** Custom event buses, coordinating between services.

### `BehaviorSubject` — Subject with a Current Value

```typescript
import { BehaviorSubject } from 'rxjs';

// Requires an initial value
const selectedStudent$ = new BehaviorSubject<Student | null>(null);

// Any subscriber immediately receives the current value
selectedStudent$.subscribe(student => console.log('Current:', student));
// Logs: Current: null

selectedStudent$.next(students[0]);
// Logs: Current: { id: 1, name: 'alice...' }

// Read current value without subscribing
selectedStudent$.getValue();   // { id: 1, name: 'alice...' }
```

**Use case:** Shared state across services, caching the last known value, loading states.

### Adding a Loading State to `StudentService`

Here is a practical use of `BehaviorSubject` — a loading indicator that components can subscribe to:

```typescript
// In StudentService
import { BehaviorSubject } from 'rxjs';

private _loading = new BehaviorSubject<boolean>(false);
readonly loading$ = this._loading.asObservable();

// Called when an HTTP request starts
private setLoading(loading: boolean): void {
  this._loading.next(loading);
}
```

In a component template:

```html
@if (studentService.loading$ | async) {
  <div class="spinner">Loading...</div>
}
```

---

## 6. `toSignal()` — Bridging Observables to Signals

`toSignal()` converts an Observable into a Signal. The Signal holds the latest emitted value and triggers Angular's reactive update mechanism when it changes.

```typescript
import { toSignal } from '@angular/core/rxjs-interop';
import { inject } from '@angular/core';

export class StudentListComponent {
  private studentService = inject(StudentService);

  // In Phase 12, studentService.getAll$() returns an Observable<Student[]>
  // toSignal converts it to a Signal<Student[] | undefined>
  readonly students = toSignal(this.studentService.getAll$(), { initialValue: [] as Student[] });
}
```

`{ initialValue: [] }` provides the value the signal holds before the Observable emits — prevents `undefined` in the template.

After this conversion, the template uses `students()` just as it does with any other signal — no `async` pipe, no subscription management.

### When to Use What

| Scenario | Approach |
|---|---|
| Observable in a template | `AsyncPipe` — simple, declarative |
| Observable read in TypeScript | `.subscribe()` + `ngOnDestroy` cleanup |
| Observable used with computed signals | `toSignal()` |
| Observable from HttpClient in a service | `toSignal()` in service, expose as Signal |

---

## 7. Preparing the Application — A Simulated Async Service

To demonstrate async patterns before connecting to a real backend, update `StudentService` to return Observables from its read methods, simulating an HTTP delay:

```typescript
import { Injectable, signal, computed } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import { Student } from '../models/student.model';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private _students = signal<Student[]>([...]);  // same as Phase 10

  readonly students    = this._students.asReadonly();
  readonly totalCount  = computed(() => this._students().length);
  readonly activeCount = computed(() => this._students().filter(s => s.status === 'active').length);

  // Observable versions — will be replaced with HttpClient in Phase 12
  getAll$(): Observable<Student[]> {
    return of(this._students()).pipe(delay(200));   // Simulate 200ms network delay
  }

  getById$(id: number): Observable<Student | undefined> {
    return of(this._students().find(s => s.id === id)).pipe(delay(200));
  }

  search$(term: string): Observable<Student[]> {
    if (!term.trim()) return of(this._students()).pipe(delay(100));
    const lower = term.toLowerCase();
    return of(
      this._students().filter(s =>
        s.name.toLowerCase().includes(lower) ||
        s.course.toLowerCase().includes(lower)
      )
    ).pipe(delay(100));
  }

  // Synchronous methods remain for direct use
  getAll(): Student[] { return this._students(); }
  getById(id: number): Student | undefined { return this._students().find(s => s.id === id); }

  add(student: Omit<Student, 'id'>): Student { ... }
  update(updated: Student): boolean { ... }
  delete(id: number): boolean { ... }
}
```

`of(value).pipe(delay(200))` creates an Observable that emits the value after a 200ms delay — exactly how a real HTTP call behaves. In Phase 12, `of(...).pipe(delay(...))` is replaced with `this.http.get<Student[]>('/api/students')`.

---

## 8. The Application So Far

```
StudentService
  ├── _students (Signal)               ← reactive in-memory store
  ├── getAll$()  (Observable)          ← async read (will become HTTP)
  ├── getById$() (Observable)          ← async read (will become HTTP)
  └── search$()  (Observable)          ← async read (will become HTTP)

Components can now:
  ├── Use AsyncPipe: students$ | async
  ├── Use toSignal(): const s = toSignal(service.getAll$())
  └── Subscribe manually + clean up in ngOnDestroy
```

**Current state:** The service exposes both synchronous (signal-based) and asynchronous (Observable-based) APIs. The application is ready to replace the simulated delay with real `HttpClient` calls in Phase 12.

---

## Phase 11 Summary

| Concept | What You Learned |
|---|---|
| Observable | A lazy, cancellable stream of zero or more values over time |
| `subscribe()` | Starts an Observable; returns a `Subscription` to unsubscribe later |
| `$` convention | Observable variables are named with a `$` suffix |
| `of()`, `from()`, `interval()` | RxJS creation functions |
| `pipe()` | Chains operators to transform a stream |
| `map()` | Transforms each emitted value |
| `filter()` | Passes only values that match a predicate |
| `tap()` | Side effects without transforming the stream |
| `catchError()` | Handles errors and recovers with a new Observable |
| `switchMap()` | Cancels in-flight inner Observable when a new value arrives |
| `debounceTime()` | Waits for a pause before emitting |
| `distinctUntilChanged()` | Skips re-emission of the same value |
| `AsyncPipe` | Subscribes in template; auto-unsubscribes on destroy |
| `Subject` | Observable you push values into manually |
| `BehaviorSubject` | Subject with a current value; new subscribers get the latest immediately |
| `toSignal()` | Converts an Observable to a Signal |

---

## What's Next

In **Phase 12 — Backend Integration**, you will replace the simulated Observable methods in `StudentService` with real `HttpClient` calls to your Spring Boot REST API. You will configure `provideHttpClient()`, make GET/POST/PUT/DELETE requests, handle HTTP errors properly, and display loading states — completing the full stack Student Management System: Angular + Spring Boot + PostgreSQL.

