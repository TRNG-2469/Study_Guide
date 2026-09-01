# Phase 11 — Async Programming

## Where We Left Off

After Phase 10, the app has full CRUD and reactive state with signals.

Right now, all data is **instantly available** — the student array lives in memory.

In Phase 12, we connect to a real **Spring REST backend**. Data will come over HTTP, which means it takes time — the app must wait for it without freezing.

This phase prepares you for that.

---

## 1. The Async Problem

When you call a REST API:

```
Angular → HTTP request → ... wait 200ms ... → Spring → response → Angular
```

If Angular waits **synchronously**, the browser freezes until the response arrives.

The solution: make the call **asynchronously** — let Angular continue, and handle the result when it arrives.

---

## 2. Promises and async/await (Quick Recap)

You already know Promises from JavaScript. Here is the pattern:

```typescript
// Returns a Promise that resolves after 1 second
function fetchData(): Promise<string> {
  return new Promise(resolve => {
    setTimeout(() => resolve('Hello'), 1000);
  });
}

// Consume it with async/await
async function load() {
  const result = await fetchData();
  console.log(result); // 'Hello' — printed after 1 second
}
```

`async/await` makes asynchronous code look like synchronous code. You know this.

---

## 3. What is an Observable?

Angular's `HttpClient` does **not** return a Promise. It returns an **Observable**.

An Observable is similar to a Promise but more powerful:

| | Promise | Observable |
|---|---|---|
| Emits values | Once | Once **or** multiple times |
| Cancellable | ❌ No | ✅ Yes |
| Operators | Limited | Rich (`map`, `filter`, `catchError`, etc.) |
| Used by | Fetch, native JS | Angular HttpClient, events |

Think of an Observable like a **stream** — it can deliver one value (like an HTTP response) or many values over time (like WebSocket messages).

For HTTP calls, a `Observable<Student[]>` behaves almost exactly like `Promise<Student[]>` — it delivers the data once when the response arrives.

---

## 4. How to Consume an Observable — `subscribe()`

You **subscribe** to an Observable to receive its value:

```typescript
someObservable.subscribe({
  next: (data) => { /* handle data */ },
  error: (err) => { /* handle error */ },
});
```

- `next` — called when data arrives
- `error` — called if something goes wrong

---

## 5. Simulate Async in StudentService

We will update `StudentService` to return an **Observable** instead of a plain array.

This directly mirrors what `HttpClient.get()` will return in Phase 12 — the components won't need to change when we switch to real HTTP.

We use `of()` from RxJS — it wraps a value in an Observable that emits immediately.

### `src/app/student.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Student } from './student.model';

@Injectable({
  providedIn: 'root'
})
export class StudentService {

  private nextId = 4;

  private students: Student[] = [
    { id: 1, name: 'alice johnson', email: 'alice@example.com', enrolledDate: '2024-01-15' },
    { id: 2, name: 'bob smith',     email: 'bob@example.com',   enrolledDate: '2024-03-22' },
    { id: 3, name: 'charlie brown', email: 'charlie@example.com', enrolledDate: '2024-06-10' },
  ];

  getStudents(): Observable<Student[]> {
    return of(this.students).pipe(delay(300));
  }

  getStudentById(id: number): Student | undefined {
    return this.students.find(s => s.id === id);
  }

  addStudent(data: Omit<Student, 'id'>): void {
    this.students.push({ id: this.nextId++, ...data });
  }

  updateStudent(id: number, data: Omit<Student, 'id'>): void {
    const index = this.students.findIndex(s => s.id === id);
    if (index !== -1) {
      this.students[index] = { id, ...data };
    }
  }

  deleteStudent(id: number): void {
    this.students = this.students.filter(s => s.id !== id);
  }

}
```

### What changed

| Change | Meaning |
|---|---|
| `of(this.students)` | Wraps the array in an Observable |
| `.pipe(delay(300))` | Simulates a 300ms network delay |
| Return type `Observable<Student[]>` | Matches what `HttpClient.get<Student[]>()` will return |

> `getStudentById`, `addStudent`, `updateStudent`, and `deleteStudent` stay synchronous for now — they will become HTTP calls in Phase 12.

---

## 6. Update StudentListComponent to Subscribe

The `students` getter no longer works because `getStudents()` now returns an Observable, not an array.

We use `ngOnInit` to subscribe and store the result.

### `src/app/student-list/student-list.component.ts`

```typescript
import { Component, inject, OnInit, OnDestroy, signal, computed, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, TitleCasePipe } from '@angular/router';
import { Subscription } from 'rxjs';
import { Student } from '../student.model';
import { StudentService } from '../student.service';
import { StudentCardComponent } from '../student-card/student-card.component';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule, TitleCasePipe, StudentCardComponent, RouterLink],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent implements OnInit, OnDestroy {

  private studentService = inject(StudentService);
  private subscription!: Subscription;

  pageTitle = 'Students';
  searchTerm = '';
  selectedStudent: Student | null = null;
  students: Student[] = [];
  isLoading = true;

  toastMessage = signal('');
  hasToast = computed(() => this.toastMessage() !== '');

  constructor() {
    effect(() => {
      if (this.toastMessage()) {
        setTimeout(() => this.toastMessage.set(''), 3000);
      }
    });
  }

  ngOnInit() {
    this.subscription = this.studentService.getStudents().subscribe({
      next: (data) => {
        this.students = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load students', err);
        this.isLoading = false;
      }
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get filteredStudents(): Student[] {
    if (!this.searchTerm) return this.students;
    return this.students.filter(s =>
      s.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  onStudentSelected(student: Student) {
    this.selectedStudent = student;
  }

  onStudentDeleted(id: number) {
    const student = this.studentService.getStudentById(id);
    this.studentService.deleteStudent(id);
    this.students = this.studentService.getStudents()
      ? this.students.filter(s => s.id !== id)
      : [];
    if (this.selectedStudent?.id === id) {
      this.selectedStudent = null;
    }
    if (student) {
      this.toastMessage.set(`"${student.name}" was deleted.`);
    }
  }

}
```

### Key points

| Code | Purpose |
|---|---|
| `this.subscription = ...subscribe(...)` | Store the subscription reference |
| `next: (data) => { this.students = data }` | Handle the data when Observable emits |
| `isLoading = true/false` | Show a loading state while waiting |
| `this.subscription.unsubscribe()` | Clean up in `ngOnDestroy` to prevent memory leaks |

> **Why unsubscribe?** If you navigate away while an Observable is still active, it keeps running in the background. `unsubscribe()` in `ngOnDestroy` stops it. This is the cleanup pattern.

---

## 7. Show Loading State in the Template

### `student-list.component.html`

Add a loading indicator:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
    <a class="btn-add" routerLink="/students/new">+ Add Student</a>
  </div>

  @if (hasToast()) {
    <div class="toast">{{ toastMessage() }}</div>
  }

  @if (isLoading) {
    <p>Loading students...</p>
  } @else {

    <div class="search-bar">
      <input type="text" [(ngModel)]="searchTerm" placeholder="Search by name..." />
    </div>

    <p>Showing {{ filteredStudents.length }} of {{ students.length }} students</p>

    @if (selectedStudent) {
      <div class="selected-banner">
        ✅ Selected: {{ selectedStudent.name | titlecase }}
      </div>
    }

    @for (student of filteredStudents; track student.id) {
      <app-student-card
        [student]="student"
        (selected)="onStudentSelected($event)"
        (deleted)="onStudentDeleted($event)">
      </app-student-card>
    }

    @if (filteredStudents.length === 0) {
      <p>No students found.</p>
    }

  }
</div>
```

---

## 8. The `async` Pipe — Preview

Angular has a built-in pipe that subscribes to an Observable **in the template**, so you don't need to manually subscribe and unsubscribe in the component:

```html
<!-- Template subscribes automatically -->
@for (student of students$ | async; track student.id) { ... }
```

Where `students$` is an `Observable<Student[]>` (by convention, `$` suffix = Observable).

The `async` pipe:
- Subscribes when the component is created
- Unsubscribes automatically when the component is destroyed
- No need for `ngOnDestroy`

We will use the `async` pipe in Phase 12 when working with real HTTP calls.

---

## Run the App

```bash
ng serve
```

Test:
- ✅ A brief "Loading students..." message appears for 300ms
- ✅ Students then render
- ✅ Delete still works with toast notification

---

## Phase 11 Summary

| Concept | What You Learned |
|---|---|
| async problem | HTTP takes time — can't block the browser |
| Promise / async/await | You already know this — quick recap |
| Observable | Like a Promise but cancellable and streamable |
| `subscribe()` | How to receive Observable values |
| `of()` + `delay()` | Simulate an async data source |
| `Subscription` + `unsubscribe()` | Prevent memory leaks in `ngOnDestroy` |
| `async` pipe | Template-based subscription — preview for Phase 12 |

---

## Application State After Phase 11

```
✅ StudentService.getStudents() returns Observable<Student[]>
✅ StudentListComponent subscribes in ngOnInit
✅ Proper unsubscribe in ngOnDestroy
✅ isLoading flag — shows loading state
✅ App behaves exactly as it will with real HTTP
```

**Next → Phase 12: Backend Integration**
We replace `of(this.students)` with real `HttpClient.get()` calls to the Spring REST API
and add error handling and loading states.
