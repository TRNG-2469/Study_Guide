# Phase 7 — Application Architecture

## What You Will Learn

Right now the student data array lives inside `StudentListComponent`. This works for a single component, but consider what happens in a real application:

- A `NavbarComponent` wants to show the total student count
- A `DashboardComponent` wants to show active vs. inactive statistics
- A `StudentDetailPage` needs to look up one student by ID

Every one of these components would need its own copy of the data, and keeping them in sync would be a maintenance nightmare.

The solution is the same one you already know from Spring Boot — a **Service layer**. You move shared data and business logic into a service. Components inject and use the service. The data lives in one place.

By the end of this phase:
- All student data lives in `StudentService`
- `StudentListComponent` and `NavbarComponent` both inject `StudentService`
- The navbar shows a live student count sourced from the service
- Adding a student in one component is reflected everywhere that uses the service

---

## 1. What is an Angular Service?

An Angular **service** is a TypeScript class decorated with `@Injectable`. Its purpose is to hold shared logic, data, or communication that does not belong inside a single component.

### Spring Boot vs. Angular Services

You already understand this architecture:

| Spring Boot | Angular | Purpose |
|---|---|---|
| `@Service` | `@Injectable` | Marks a class as a service |
| `@Autowired` / constructor injection | `inject()` / constructor | Injects the service |
| Spring Container (IoC) | Angular Injector | Manages service instances |
| Singleton scope (default) | `providedIn: 'root'` | One instance for the whole app |

When you annotate a Spring service with `@Service`, Spring creates one instance and injects it wherever `@Autowired` is used. Angular does exactly the same thing with `@Injectable({ providedIn: 'root' })`.

### What Belongs in a Service?

| Put in a Service | Keep in a Component |
|---|---|
| Data shared between components | UI state (is a panel open?) |
| Business logic | Template event handlers |
| HTTP calls to REST APIs | Display formatting |
| Data transformation | `@Input()` / `@Output()` wiring |

---

## 2. The `@Injectable` Decorator

```typescript
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'   // Register with the root injector — app-wide singleton
})
export class StudentService {
  // logic here
}
```

`providedIn: 'root'` is the modern way to register a service. It tells Angular's Dependency Injection system to create **one instance** of this service for the entire application. Every component that injects `StudentService` gets the **same instance** — so they all see the same data.

---

## 3. Generating a Service with the CLI

```bash
ng generate service services/student
```

Or shorthand:

```bash
ng g s services/student
```

This creates `src/app/services/student.service.ts` and a test file. Angular automatically adds `@Injectable({ providedIn: 'root' })`.

---

## 4. Building `StudentService`

### `src/app/services/student.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { Student } from '../models/student.model';

@Injectable({
  providedIn: 'root'
})
export class StudentService {

  // The single source of truth for student data
  private students: Student[] = [
    { id: 1, name: 'alice johnson',  course: 'Computer Science', year: 2, email: 'alice@uni.edu',  status: 'active',    enrolmentDate: new Date('2023-09-01') },
    { id: 2, name: 'bob martinez',   course: 'Mathematics',      year: 3, email: 'bob@uni.edu',    status: 'active',    enrolmentDate: new Date('2022-09-01') },
    { id: 3, name: 'carol williams', course: 'Physics',          year: 1, email: 'carol@uni.edu',  status: 'inactive',  enrolmentDate: new Date('2024-09-01') },
    { id: 4, name: 'david chen',     course: 'Computer Science', year: 4, email: 'david@uni.edu',  status: 'graduated', enrolmentDate: new Date('2021-09-01') },
    { id: 5, name: 'emma davis',     course: 'Engineering',      year: 2, email: 'emma@uni.edu',   status: 'active',    enrolmentDate: new Date('2023-09-01') },
  ];

  // ─── Read ───────────────────────────────────────────────────────

  getAll(): Student[] {
    return this.students;
  }

  getById(id: number): Student | undefined {
    return this.students.find(s => s.id === id);
  }

  search(term: string): Student[] {
    if (!term.trim()) return this.students;
    const lower = term.toLowerCase();
    return this.students.filter(s =>
      s.name.toLowerCase().includes(lower) ||
      s.course.toLowerCase().includes(lower)
    );
  }

  // ─── Computed ────────────────────────────────────────────────────

  getTotalCount(): number {
    return this.students.length;
  }

  getActiveCount(): number {
    return this.students.filter(s => s.status === 'active').length;
  }

  // ─── Write ───────────────────────────────────────────────────────

  add(student: Omit<Student, 'id'>): Student {
    const newStudent: Student = {
      ...student,
      id: this.nextId()
    };
    this.students.push(newStudent);
    return newStudent;
  }

  update(updated: Student): boolean {
    const index = this.students.findIndex(s => s.id === updated.id);
    if (index === -1) return false;
    this.students[index] = updated;
    return true;
  }

  delete(id: number): boolean {
    const index = this.students.findIndex(s => s.id === id);
    if (index === -1) return false;
    this.students.splice(index, 1);
    return true;
  }

  // ─── Private helpers ─────────────────────────────────────────────

  private nextId(): number {
    return this.students.length > 0
      ? Math.max(...this.students.map(s => s.id)) + 1
      : 1;
  }
}
```

**Design notes:**

- `private students` — the array is private. Components cannot modify it directly; they must use the service's public methods. This is the same encapsulation principle you use with Spring's `@Service` — the repository is private to the service.
- `Omit<Student, 'id'>` — the `add()` method accepts a student without an ID because the service assigns the ID. This uses TypeScript's built-in `Omit` utility type.
- CRUD methods (`getAll`, `getById`, `add`, `update`, `delete`) — the service exposes a complete API. In Phase 12 these will delegate to HTTP calls against your Spring REST API.

---

## 5. Two Ways to Inject a Service

Angular provides two equivalent injection syntaxes. You will see both in real projects.

### Option 1 — `inject()` Function (Modern, Recommended)

```typescript
import { Component } from '@angular/core';
import { inject } from '@angular/core';
import { StudentService } from '../services/student.service';

export class StudentListComponent {
  private studentService = inject(StudentService);
}
```

`inject()` is a function you call at the field level. It looks up the `StudentService` instance from Angular's injector and assigns it. This is the modern Angular style — concise, type-inferred, and works well with signals.

### Option 2 — Constructor Injection (Traditional)

```typescript
import { Component } from '@angular/core';
import { StudentService } from '../services/student.service';

export class StudentListComponent {
  constructor(private studentService: StudentService) {}
}
```

Angular sees `StudentService` as a constructor parameter type and automatically injects the instance. This is the style used in most Angular tutorials and older codebases — it still works perfectly and is equally correct.

**Which should you use?** This course uses `inject()` for new code. When you maintain older code you will encounter the constructor pattern — both are valid.

---

## 6. Updating `StudentListComponent` to Use the Service

### `src/app/student-list/student-list.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe, DatePipe } from '@angular/common';
import { StudentCardComponent } from '../student-card/student-card.component';
import { Student } from '../models/student.model';
import { StudentService } from '../services/student.service';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [StudentCardComponent, FormsModule, TitleCasePipe, DatePipe],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {

  // Inject the service — Angular provides the singleton instance
  private studentService = inject(StudentService);

  // UI state stays in the component
  sectionTitle = 'All Students';
  searchTerm = '';
  showSearch = false;
  selectedStudent: Student | null = null;

  // Data now comes from the service
  get students(): Student[] {
    return this.studentService.getAll();
  }

  get filteredStudents(): Student[] {
    return this.studentService.search(this.searchTerm);
  }

  get activeCount(): number {
    return this.studentService.getActiveCount();
  }

  // Event handlers
  onStudentSelected(student: Student): void {
    this.selectedStudent = this.selectedStudent?.id === student.id ? null : student;
  }

  closeDetail(): void {
    this.selectedStudent = null;
  }

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) this.searchTerm = '';
  }

  // Delegate to service
  onDeleteStudent(id: number): void {
    this.studentService.delete(id);
    if (this.selectedStudent?.id === id) {
      this.selectedStudent = null;
    }
  }
}
```

**What changed:**
- The `students` array is gone from the component
- Getters now delegate to the service: `this.studentService.getAll()`, `.search()`, `.getActiveCount()`
- `onDeleteStudent()` calls `this.studentService.delete()` — the component does not manipulate the array directly
- UI state (`searchTerm`, `showSearch`, `selectedStudent`) stays in the component — it is view-specific

Update the detail panel's Delete button in the template to call `onDeleteStudent()`:

```html
<!-- In the detail-actions div -->
<div class="detail-actions">
  <button class="btn btn-primary">Edit Student</button>
  <button class="btn btn-danger" (click)="onDeleteStudent(selectedStudent!.id)">
    Delete
  </button>
</div>
```

---

## 7. Updating `NavbarComponent` to Use the Service

Now demonstrate why the service matters: the navbar can show the live student count without the parent passing it down.

### `src/app/navbar/navbar.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { StudentService } from '../services/student.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  private studentService = inject(StudentService);

  appName = 'Student Management System';

  get totalStudents(): number {
    return this.studentService.getTotalCount();
  }
}
```

### Updated `src/app/navbar/navbar.component.html`

```html
<nav class="navbar">
  <div class="navbar-brand">
    <span class="brand-icon">🎓</span>
    <span class="brand-name">{{ appName }}</span>
  </div>
  <div class="navbar-center">
    <span class="student-count">
      {{ totalStudents }} Students
    </span>
  </div>
  <div class="navbar-links">
    <a href="#">Home</a>
    <a href="#">Students</a>
    <a href="#">About</a>
  </div>
</nav>
```

Add to `navbar.component.css`:

```css
.navbar-center {
  flex: 1;
  text-align: center;
}

.student-count {
  font-size: 14px;
  background-color: rgba(255,255,255,0.2);
  padding: 4px 14px;
  border-radius: 20px;
}
```

**Now delete a student** using the Delete button in the detail panel. Watch the navbar's count drop immediately. Both `NavbarComponent` and `StudentListComponent` injected the **same** `StudentService` instance — when one mutates the data, both components reflect the change automatically on the next Change Detection cycle.

---

## 8. The Dependency Injection System

Angular's DI system works through a hierarchy of **injectors**. When a component requests a service, Angular walks up the injector tree until it finds a provider:

```
Root Injector (created by bootstrapApplication)
│  ← provides StudentService (providedIn: 'root')
│
├── AppComponent Injector
│   ├── NavbarComponent Injector    → asks for StudentService → found at Root → same instance
│   └── StudentListComponent Injector → asks for StudentService → found at Root → same instance
│       └── StudentCardComponent Injector
```

Because `StudentService` is provided at the root, every component in the tree gets the same single instance. This is equivalent to Spring's default singleton scope.

### Providing a Service at Component Level

You can limit a service's scope by providing it directly in a component:

```typescript
@Component({
  providers: [StudentService]   // Creates a NEW instance scoped to this component
})
export class StudentListComponent { ... }
```

This creates a new, isolated instance only for `StudentListComponent` and its children. You will rarely need this for data services, but it is useful for services that hold form state or wizard state that should reset when the component is destroyed.

---

## 9. Service Architecture Pattern

```
Component Layer              Service Layer              Data Layer (Phase 12)
─────────────────────        ─────────────────────      ─────────────────────
NavbarComponent              StudentService             (later) Spring REST API
  inject(StudentService) ──► getAll()                   POST /api/students
  getTotalCount()            getById()                  GET  /api/students
                             search()                   PUT  /api/students/:id
StudentListComponent         add()                      DELETE /api/students/:id
  inject(StudentService) ──► update()
  search()                   delete()
  delete()
```

In Phase 12, the service methods that currently manipulate an in-memory array will be replaced with `HttpClient` calls to your Spring REST API. The components will not need to change — they will still call the same `getAll()`, `add()`, `delete()` methods on the service.

---

## 10. The Application So Far

```
Angular Injector (Root)
└── StudentService (singleton) ← one instance, owns the data array
    │
    ├── NavbarComponent.totalStudents
    └── StudentListComponent.students / filteredStudents / activeCount / onDelete
```

**Current state:** Student data lives in exactly one place — `StudentService`. The navbar and the list both consume it. Deleting a student through the list is immediately reflected in the navbar count. The architecture mirrors the Spring `@Service` / `@Controller` separation you already know.

---

## Phase 7 Summary

| Concept | What You Learned |
|---|---|
| `@Injectable({ providedIn: 'root' })` | Creates an app-wide singleton service |
| `inject(ServiceClass)` | Modern DI — injects a service at the field level |
| Constructor injection | Traditional DI — injects via constructor parameter type |
| Service responsibility | Data + business logic; not UI state or event handling |
| Singleton behaviour | All injecting components share the same instance and the same data |
| `Omit<T, 'id'>` | TypeScript utility — a type without specific keys |
| Component-level providers | Scopes a service to one component subtree (rarely needed for data) |
| Architecture preview | Service methods will be replaced with HTTP calls in Phase 12 |

---

## What's Next

In **Phase 8 — Navigation**, you will configure Angular's **Router** to give the application multiple pages. You will create separate routes for the student list, a student detail page, and an about page — replacing the placeholder `href="#"` links in the navbar with real `routerLink` directives. You will also learn how to pass a student ID in the URL and retrieve it with route parameters.

