# Phase 10 — Angular Internals & Reactivity

## Where We Left Off

After Phase 9, the app has full frontend CRUD.  
We have used `ngOnInit()` in two components already without fully explaining it.

In this phase, we look **inside** how Angular works:
- **Lifecycle** — what happens when a component is created and destroyed
- **Signals** — Angular's modern way to track and react to changing data
- **Change Detection** — how Angular knows when to update the screen

---

## Part 1 — Component Lifecycle

Every Angular component goes through a **lifecycle** — from creation to destruction.

Angular provides **lifecycle hooks** — methods you can implement to run code at specific moments.

### The Key Hooks

| Hook | When it runs |
|---|---|
| `ngOnInit()` | Once, after the component is created and inputs are set |
| `ngOnChanges()` | Every time an `@Input()` value changes |
| `ngOnDestroy()` | Just before the component is removed from the DOM |

> The other hooks (`ngAfterViewInit`, `ngDoCheck`, etc.) exist but are rarely needed for beginners.

---

### `ngOnInit` — Already Used

We already used `ngOnInit` in `StudentDetailComponent` and `StudentFormComponent` to read the route parameter and load data.

The rule is simple:
- Use the **constructor** only to inject services
- Use **`ngOnInit`** to do work (fetch data, read routes, setup)

```typescript
// ✅ Correct pattern
constructor(private service: StudentService) {}  // inject only

ngOnInit() {
  this.students = this.service.getStudents();     // do work here
}
```

---

### `ngOnDestroy` — Cleanup

`ngOnDestroy` runs when the component is removed — for example, when navigating away from a page.

Use it to clean up timers, subscriptions, or any resources.

### Add `ngOnDestroy` to `StudentListComponent`

```typescript
import { Component, inject, OnInit, OnDestroy } from '@angular/core';
// ... other imports

export class StudentListComponent implements OnInit, OnDestroy {

  private studentService = inject(StudentService);

  // ... existing properties

  ngOnInit() {
    console.log('StudentListComponent created');
  }

  ngOnDestroy() {
    console.log('StudentListComponent destroyed — navigate to another page to see this');
  }

  // ... existing methods
}
```

Open the browser console and navigate between pages — you will see the log messages fire at the right moments.

---

## Part 2 — Signals

### The Problem Signals Solve

Before Signals (Angular 16 and earlier), Angular detected changes by running **change detection** on the entire component tree after any event.

**Signals** let you declare: *"this piece of data can change, and Angular should update only what depends on it."*

### Three Signal Primitives

| Function | Purpose |
|---|---|
| `signal(value)` | Creates a reactive value |
| `computed(() => ...)` | Derives a value from one or more signals |
| `effect(() => ...)` | Runs a side-effect when a signal changes |

---

### Practical Example — Toast Notification

We will add a **toast notification** to `StudentListComponent` that:
1. Shows a message when a student is deleted
2. Automatically clears after 3 seconds

This demonstrates all three signal primitives in a practical, contained way.

### Update `StudentListComponent`

```typescript
import { Component, inject, OnInit, OnDestroy, signal, computed, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, TitleCasePipe } from '@angular/router';
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

  pageTitle = 'Students';
  searchTerm = '';
  selectedStudent: Student | null = null;

  // --- Signals ---
  toastMessage = signal('');
  hasToast = computed(() => this.toastMessage() !== '');

  constructor() {
    effect(() => {
      if (this.toastMessage()) {
        setTimeout(() => this.toastMessage.set(''), 3000);
      }
    });
  }

  // --- Lifecycle ---
  ngOnInit() {
    console.log('StudentListComponent created');
  }

  ngOnDestroy() {
    console.log('StudentListComponent destroyed');
  }

  // --- Getters ---
  get students(): Student[] {
    return this.studentService.getStudents();
  }

  get filteredStudents(): Student[] {
    if (!this.searchTerm) return this.students;
    return this.students.filter(s =>
      s.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  // --- Event handlers ---
  onStudentSelected(student: Student) {
    this.selectedStudent = student;
  }

  onStudentDeleted(id: number) {
    const student = this.studentService.getStudentById(id);
    this.studentService.deleteStudent(id);
    if (this.selectedStudent?.id === id) {
      this.selectedStudent = null;
    }
    if (student) {
      this.toastMessage.set(`"${student.name}" was deleted.`);
    }
  }

}
```

### What the signals do

| Code | What it does |
|---|---|
| `signal('')` | Creates a reactive string, initially empty |
| `toastMessage()` | **Reads** the signal value (note the `()`) |
| `toastMessage.set('...')` | **Writes** a new value to the signal |
| `computed(() => ...)` | Automatically re-evaluates when `toastMessage` changes |
| `effect(() => ...)` | Runs whenever `toastMessage` changes — sets a 3-second timer |

> `effect()` must be called inside the **constructor**, not in `ngOnInit`.

---

### Show the Toast in the Template

### `student-list.component.html`

Add the toast just below the header:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
    <a class="btn-add" routerLink="/students/new">+ Add Student</a>
  </div>

  @if (hasToast()) {
    <div class="toast">{{ toastMessage() }}</div>
  }

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
</div>
```

> In templates, signals are **read as functions**: `toastMessage()` and `hasToast()`.

### Add toast style to `student-list.component.css`

```css
.toast {
  background-color: #343a40;
  color: white;
  padding: 10px 16px;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 14px;
}
```

---

## Part 3 — Change Detection

Change detection is how Angular decides **when to update the DOM**.

By default, Angular checks every component after **any event** (click, input, timer, HTTP response).

### With Signals

When you use signals, Angular can be smarter — it only updates the parts of the template that **read that specific signal**.

This is why Angular introduced signals: more efficient, more predictable updates.

### Summary

| Approach | How Angular updates |
|---|---|
| Regular properties | Checks all components after every event |
| Signals | Updates only what reads the changed signal |

You don't need to configure anything — just using signals gives you the benefit automatically.

---

## Run the App

```bash
ng serve
```

Test:
- ✅ Open browser console — see `ngOnInit` log when landing on list
- ✅ Navigate to detail → see `ngOnDestroy` log in console
- ✅ Navigate back → see `ngOnInit` log again
- ✅ Delete a student → dark toast appears with the student's name
- ✅ Toast disappears automatically after 3 seconds

---

## Phase 10 Summary

| Concept | What You Learned |
|---|---|
| `ngOnInit` | Run setup code after component is created |
| `ngOnDestroy` | Clean up before component is removed |
| `signal(value)` | Reactive value — read with `()`, write with `.set()` |
| `computed(() => ...)` | Derived value — re-runs when its signals change |
| `effect(() => ...)` | Side effect — re-runs when its signals change |
| Change Detection | Signals make updates more targeted and efficient |

---

## Application State After Phase 10

```
✅ ngOnInit and ngOnDestroy in StudentListComponent (with console logs)
✅ toastMessage signal — shows delete confirmation
✅ hasToast computed — drives @if in the template
✅ effect — auto-clears toast after 3 seconds
```

**Next → Phase 11: Async Programming**
We will learn about async/await and Observables — the foundation needed before connecting to a real HTTP backend.
