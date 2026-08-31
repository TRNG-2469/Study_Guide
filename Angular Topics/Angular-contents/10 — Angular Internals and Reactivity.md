# Phase 10 — Angular Internals & Reactivity

## What You Will Learn

In previous phases you wrote working Angular code without thinking much about *when* Angular runs it. This phase goes inside the machine:

- **Component Lifecycle** — the sequence of events from component creation to destruction, and the hooks Angular gives you to participate in each stage
- **Signals** — Angular's modern reactive state system that replaces plain class properties for shared, reactive values
- **Change Detection** — how Angular decides when to update the DOM, and how to make your application more efficient

By the end of this phase, `StudentService` will use Signals so that any component consuming the student list reacts automatically when the data changes — without getter polling.

---

## 1. Component Lifecycle

Every Angular component goes through a lifecycle — a sequence of events from creation to destruction. Angular provides **lifecycle hooks** — interface methods you implement in the component class — that are called at each stage.

```
constructor()
  ↓
ngOnChanges()     ← Called when @Input() values change (before ngOnInit on first run)
  ↓
ngOnInit()        ← Component is initialised; @Input() values are ready
  ↓
ngDoCheck()       ← Called every Change Detection cycle (use sparingly)
  ↓
ngAfterContentInit()   ← Projected content (ng-content) is initialised
  ↓
ngAfterContentChecked()
  ↓
ngAfterViewInit()      ← Component's view (template + children) is fully initialised
  ↓
ngAfterViewChecked()
  ↓
  [User interactions, data changes, HTTP responses — lifecycle repeats from ngOnChanges]
  ↓
ngOnDestroy()     ← Component is about to be removed from the DOM
```

### The Hooks You Will Use Most

#### `ngOnInit` — Component Initialisation

```typescript
import { Component, OnInit } from '@angular/core';

export class StudentDetailComponent implements OnInit {
  student: Student | undefined;

  ngOnInit(): void {
    // Safe to read @Input() values and route params here
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.student = this.studentService.getById(id);
  }
}
```

**Use for:** Reading route parameters, fetching initial data, setting up subscriptions.

**Why not use `constructor()`?** The constructor runs before Angular has set `@Input()` values and before route params are available. `ngOnInit` is where the component is "ready". Reserve the constructor for dependency injection only.

#### `ngOnDestroy` — Cleanup

```typescript
import { Component, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';

export class StudentListComponent implements OnDestroy {
  private sub!: Subscription;

  ngOnInit(): void {
    this.sub = someObservable.subscribe(...);
  }

  ngOnDestroy(): void {
    // Prevent memory leaks — unsubscribe when the component is removed
    this.sub.unsubscribe();
  }
}
```

**Use for:** Unsubscribing from Observables, clearing intervals and timeouts, releasing resources. Angular's `AsyncPipe` (Phase 11) and Signals do this automatically — but manually created subscriptions must be cleaned up here.

#### `ngOnChanges` — Reacting to `@Input()` Changes

```typescript
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';

export class StudentCardComponent implements OnChanges {
  @Input() student!: Student;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['student']) {
      const prev = changes['student'].previousValue;
      const curr = changes['student'].currentValue;
      console.log('Student changed from', prev, 'to', curr);
    }
  }
}
```

**Use for:** Reacting to changes in `@Input()` properties — for example, recalculating derived values when a parent passes new data.

#### `ngAfterViewInit` — Access Child Elements

```typescript
import { Component, AfterViewInit, ViewChild, ElementRef } from '@angular/core';

export class SearchComponent implements AfterViewInit {
  @ViewChild('searchInput') searchInput!: ElementRef;

  ngAfterViewInit(): void {
    // The DOM element is now available
    this.searchInput.nativeElement.focus();
  }
}
```

**Use for:** Accessing rendered DOM elements or child component instances. The view is not available in `ngOnInit` — wait for `ngAfterViewInit`.

### Lifecycle Interfaces

Implementing the interface (`implements OnInit`) is optional but recommended — TypeScript will warn you if you misspell `ngOnInit()`, catching bugs at compile time rather than at runtime.

---

## 2. Signals — Modern Reactive State

### The Problem with Plain Properties

In `StudentService`, `getTotalCount()` returns `this.students.length`. Every time Angular runs Change Detection, it calls this method to check if the value changed. This works, but it is inefficient for large applications and not truly reactive — components do not *know* the data changed, they just get re-checked.

### What is a Signal?

A **Signal** is a reactive value — a wrapper around a value that notifies Angular automatically when it changes. Components reading a signal are tracked; when the signal's value changes, only those components update.

```typescript
import { signal, computed, effect } from '@angular/core';

// Create a signal with an initial value
const count = signal(0);

// Read a signal — call it like a function
console.log(count());    // 0

// Update a signal
count.set(5);            // replace the value
count.update(n => n + 1); // update based on current value

console.log(count());    // 6
```

### `computed()` — Derived Signals

A computed signal derives its value from other signals and updates automatically:

```typescript
const students = signal<Student[]>([]);
const activeCount = computed(() => students().filter(s => s.status === 'active').length);

// When students changes, activeCount is automatically recalculated
students.update(list => [...list, newStudent]);
console.log(activeCount());  // updates automatically
```

### `effect()` — Side Effects

An `effect()` runs a function whenever any signal it reads changes:

```typescript
effect(() => {
  // Runs whenever students() or searchTerm() changes
  console.log('Students updated:', students().length);
});
```

**Use for:** Logging, syncing to localStorage, triggering animations. Do not use for deriving state — use `computed()` for that.

### Signals vs. Plain Properties

| | Plain Property | Signal |
|---|---|---|
| Reading | `this.count` | `this.count()` |
| Writing | `this.count = 5` | `this.count.set(5)` |
| Derived values | `get active()` | `computed(...)` |
| Change notification | Angular polls on every cycle | Angular is notified immediately |
| Template usage | `{{ count }}` | `{{ count() }}` |

---

## 3. Refactoring `StudentService` with Signals

### Updated `src/app/services/student.service.ts`

```typescript
import { Injectable, signal, computed } from '@angular/core';
import { Student } from '../models/student.model';

@Injectable({
  providedIn: 'root'
})
export class StudentService {

  // The students array is now a Signal — not a plain private array
  private _students = signal<Student[]>([
    { id: 1, name: 'alice johnson',  course: 'Computer Science', year: 2, email: 'alice@uni.edu',  status: 'active',    enrolmentDate: new Date('2023-09-01') },
    { id: 2, name: 'bob martinez',   course: 'Mathematics',      year: 3, email: 'bob@uni.edu',    status: 'active',    enrolmentDate: new Date('2022-09-01') },
    { id: 3, name: 'carol williams', course: 'Physics',          year: 1, email: 'carol@uni.edu',  status: 'inactive',  enrolmentDate: new Date('2024-09-01') },
    { id: 4, name: 'david chen',     course: 'Computer Science', year: 4, email: 'david@uni.edu',  status: 'graduated', enrolmentDate: new Date('2021-09-01') },
    { id: 5, name: 'emma davis',     course: 'Engineering',      year: 2, email: 'emma@uni.edu',   status: 'active',    enrolmentDate: new Date('2023-09-01') },
  ]);

  // Expose a read-only computed signal — components can read but not set directly
  readonly students = this._students.asReadonly();

  // Computed signals — automatically update when _students changes
  readonly totalCount  = computed(() => this._students().length);
  readonly activeCount = computed(() => this._students().filter(s => s.status === 'active').length);

  // ─── Read ────────────────────────────────────────────────────────────────

  getAll(): Student[] {
    return this._students();   // Call the signal to read its value
  }

  getById(id: number): Student | undefined {
    return this._students().find(s => s.id === id);
  }

  search(term: string): Student[] {
    if (!term.trim()) return this._students();
    const lower = term.toLowerCase();
    return this._students().filter(s =>
      s.name.toLowerCase().includes(lower) ||
      s.course.toLowerCase().includes(lower)
    );
  }

  // ─── Write ───────────────────────────────────────────────────────────────

  add(student: Omit<Student, 'id'>): Student {
    const newStudent: Student = { ...student, id: this.nextId() };
    // update() receives the current array and returns the new one
    this._students.update(list => [...list, newStudent]);
    return newStudent;
  }

  update(updated: Student): boolean {
    const exists = this._students().some(s => s.id === updated.id);
    if (!exists) return false;
    this._students.update(list =>
      list.map(s => s.id === updated.id ? updated : s)
    );
    return true;
  }

  delete(id: number): boolean {
    const exists = this._students().some(s => s.id === id);
    if (!exists) return false;
    this._students.update(list => list.filter(s => s.id !== id));
    return true;
  }

  // ─── Private ─────────────────────────────────────────────────────────────

  private nextId(): number {
    const list = this._students();
    return list.length > 0 ? Math.max(...list.map(s => s.id)) + 1 : 1;
  }
}
```

**What changed:**
- `private students: Student[]` → `private _students = signal<Student[]>([...])`
- `students` is now a **readonly computed signal** exposed publicly — components read it reactively
- `totalCount` and `activeCount` are **computed signals** — they recalculate automatically when `_students` changes
- Write operations use `_students.update(list => ...)` — the pattern for producing a new array from the old one (Angular requires a new reference to detect the change)
- `getAll()` calls `this._students()` — the signal call reads the current value

---

## 4. Reading Signals in Components

### Updated `NavbarComponent`

```typescript
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { StudentService } from '../services/student.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  private studentService = inject(StudentService);

  // Directly expose the computed signal from the service
  totalStudents = this.studentService.totalCount;

  appName = 'Student Management System';
}
```

In the template, call the signal with `()`:

```html
<span class="student-count">{{ totalStudents() }} Students</span>
```

Angular tracks that this template reads `totalStudents()` (which reads `_students`). When a student is added or deleted anywhere in the app, Angular knows to re-render only the parts of the navbar that depend on this signal — not the entire component tree.

### Updated `StudentListComponent`

```typescript
export class StudentListComponent {
  private studentService = inject(StudentService);

  // Signals from the service
  readonly students    = this.studentService.students;      // ReadonlySignal<Student[]>
  readonly activeCount = this.studentService.activeCount;   // Signal<number>

  // UI state — still plain properties (not shared, not reactive globally)
  sectionTitle = 'All Students';
  searchTerm = '';
  showSearch = false;
  selectedStudent: Student | null = null;

  get filteredStudents(): Student[] {
    return this.studentService.search(this.searchTerm);
  }

  onStudentSelected(student: Student): void {
    this.selectedStudent = this.selectedStudent?.id === student.id ? null : student;
  }

  closeDetail(): void { this.selectedStudent = null; }

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) this.searchTerm = '';
  }

  onDeleteStudent(id: number): void {
    this.studentService.delete(id);
    if (this.selectedStudent?.id === id) this.selectedStudent = null;
  }
}
```

In `student-list.component.html`, update signal reads with `()`:

```html
<span class="count-badge">{{ students().length }} total · {{ activeCount() }} active</span>

<!-- The @for loop reads the signal -->
@for (student of filteredStudents; track student.id) {
  <app-student-card
    [student]="student"
    [isSelected]="selectedStudent?.id === student.id"
    (studentSelected)="onStudentSelected($event)">
  </app-student-card>
}
```

---

## 5. Change Detection

Change Detection (CD) is Angular's process of comparing current values against previous values to determine which parts of the DOM need updating.

### Default Change Detection

By default, Angular runs CD for every component after every event (click, keypress, timer, HTTP response). It walks the entire component tree checking every bound expression:

```
User clicks a button
  → Angular runs Change Detection
  → Checks AppComponent bindings
  → Checks NavbarComponent bindings
  → Checks StudentListComponent bindings
  → Checks every StudentCardComponent binding
  → Updates DOM where values changed
```

This is safe but can be slow for large trees.

### `OnPush` Change Detection

With `changeDetection: ChangeDetectionStrategy.OnPush`, Angular checks a component **only when**:
1. An `@Input()` reference changes
2. An event originates from inside the component
3. A Signal the component reads changes
4. You manually trigger it

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-student-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ...
})
export class StudentCardComponent { ... }
```

`StudentCardComponent` is a perfect candidate for `OnPush`:
- It only depends on `@Input() student` and `@Input() isSelected`
- Angular will skip CD for a card unless one of those inputs changes or the card emits an event

This means if you have 100 student cards and update one student, Angular checks only that card — not all 100.

### Signals + OnPush

Signals work seamlessly with `OnPush`. A component with `OnPush` still updates when a signal it reads changes — Angular tracks signal reads regardless of the CD strategy. This is the most efficient combination for modern Angular apps.

---

## 6. `toSignal()` — Converting Observables to Signals

When you start using HTTP in Phase 12, you will work with Observables (RxJS). Angular provides `toSignal()` to bridge between the two worlds:

```typescript
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';

export class StudentService {
  private http = inject(HttpClient);

  // In Phase 12 — an Observable from HTTP converted to a Signal
  readonly students = toSignal(
    this.http.get<Student[]>('/api/students'),
    { initialValue: [] }
  );
}
```

You do not need this yet — but knowing it exists prepares you for Phase 12 where HTTP returns Observables and you will want them as Signals.

---

## 7. Lifecycle + Signals Together — A Practical Pattern

Here is how lifecycle hooks and signals combine in a real component:

```typescript
@Component({ ... })
export class StudentListComponent implements OnInit, OnDestroy {
  private studentService = inject(StudentService);

  // Signals — reactive
  readonly students = this.studentService.students;

  // Local signal for search
  searchTerm = signal('');

  // Computed — derived from signals
  readonly filteredStudents = computed(() => {
    const term = this.searchTerm().toLowerCase();
    return this.students().filter(s =>
      s.name.toLowerCase().includes(term) ||
      s.course.toLowerCase().includes(term)
    );
  });

  // Effect — side effect when search changes
  private logEffect = effect(() => {
    console.log('Filtering by:', this.searchTerm());
  });

  ngOnInit(): void {
    // Runs once after component initialisation
    console.log('StudentListComponent ready');
  }

  ngOnDestroy(): void {
    // Clean up manual subscriptions here (not signals — they clean up themselves)
  }
}
```

In the template:

```html
<input [(ngModel)]="searchTerm" ... />   <!-- won't work — ngModel needs a plain property -->

<!-- For a signal-backed input, use event binding -->
<input [value]="searchTerm()" (input)="searchTerm.set($any($event.target).value)">

<!-- Or keep searchTerm as a plain property and use computed() in the service -->
```

> **Practical tip for now:** Keep simple local UI state like `searchTerm` as a plain property and use `[(ngModel)]`. Promote it to a Signal when you need the computed/effect capabilities or when it is shared across components.

---

## 8. The Application So Far

```
StudentService
  _students = signal<Student[]>([...])     ← single source of truth (reactive)
  students  = _students.asReadonly()       ← exposed to components
  totalCount  = computed(...)              ← auto-updates when _students changes
  activeCount = computed(...)              ← auto-updates when _students changes

NavbarComponent
  totalStudents = studentService.totalCount   ← reads computed signal
  Template: {{ totalStudents() }}             ← re-renders only when signal changes

StudentListComponent
  students    = studentService.students       ← reads signal
  activeCount = studentService.activeCount    ← reads computed signal
  Template: {{ students().length }}           ← re-renders only when signal changes
```

**Current state:** The application is fully reactive. Any mutation through `StudentService` (add, update, delete) automatically propagates to all components reading its signals, with minimal DOM updates.

---

## Phase 10 Summary

| Concept | What You Learned |
|---|---|
| `ngOnInit` | First safe place to read `@Input()` values and route params |
| `ngOnDestroy` | Clean up subscriptions and resources |
| `ngOnChanges` | React to `@Input()` property changes |
| `ngAfterViewInit` | Access rendered DOM elements and child components |
| `signal(value)` | Creates a reactive value wrapper |
| `signal.set(v)` | Replaces the signal's value |
| `signal.update(fn)` | Updates based on current value |
| `signal.asReadonly()` | Exposes a signal without the ability to set it |
| `computed(() => ...)` | A derived signal that recalculates when its dependencies change |
| `effect(() => ...)` | Runs a side effect when any signal it reads changes |
| `ChangeDetectionStrategy.OnPush` | Component only checks when inputs change, events fire, or signals update |
| `toSignal()` | Converts an Observable to a Signal (preview for Phase 12) |

---

## What's Next

In **Phase 11 — Async Programming**, you will learn the fundamentals of asynchronous programming in Angular. You will understand why HTTP calls return **Observables** (not plain values), how to work with RxJS basics, and how the `AsyncPipe` simplifies subscribing in templates — preparing you to replace the in-memory `StudentService` with real HTTP calls to your Spring REST API in Phase 12.

