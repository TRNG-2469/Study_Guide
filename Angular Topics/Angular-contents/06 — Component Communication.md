# Phase 6 — Component Communication

## What You Will Learn

So far, `StudentCardComponent` manages its own selected state (`isSelected`) internally. This creates a problem: the parent (`StudentListComponent`) has no idea which card is selected. In a real application the parent needs to know — so it can show a detail panel, highlight the correct row in a table, or disable the "Edit" button when nothing is selected.

In this phase you will learn the two formal mechanisms Angular provides for sharing data between parent and child components:

| Mechanism | Direction | Decorator |
|---|---|---|
| Property Input | Parent → Child | `@Input()` |
| Event Output | Child → Parent | `@Output()` + `EventEmitter` |

By the end of this phase:
- The parent passes each student object and a `selectedId` flag **down** to the card
- The card emits a `studentSelected` event **up** when clicked
- The parent captures the event and displays a **Student Detail Panel** on the side

---

## 1. The Problem with Internal State

Currently `StudentCardComponent` manages `isSelected` itself:

```typescript
// Inside StudentCardComponent — self-managed state
isSelected = false;

toggleSelect(): void {
  this.isSelected = !this.isSelected;
}
```

**The problem:** Only one student should be selected at a time, but there is nothing stopping multiple cards from all having `isSelected = true` simultaneously. Each card is an isolated island — they cannot talk to each other.

**The solution:** Lift the selected state up to the **parent** (`StudentListComponent`). The parent owns `selectedStudentId`. It tells each card whether it is selected (via `@Input()`), and each card tells the parent when it was clicked (via `@Output()`).

This pattern — **lifting state up** — is fundamental to component-based UI development.

---

## 2. `@Input()` — Passing Data from Parent to Child

`@Input()` marks a component property as receivable from a parent component's template.

### Syntax in the Child Component

```typescript
import { Component, Input } from '@angular/core';

export class ChildComponent {
  @Input() message: string = '';
  @Input() count: number = 0;
}
```

### Syntax in the Parent Template

```html
<app-child [message]="parentMessage" [count]="42"></app-child>
```

Square brackets on the left match the `@Input()` property name. The value on the right is a TypeScript expression evaluated in the **parent's** context.

### Input Rules

- An `@Input()` property flows **one way** — parent to child. Changing it inside the child does not change the parent's value.
- If you need the parent to react to a change, use `@Output()`.
- Mark required inputs with `@Input({ required: true })` (Angular 16+) — Angular throws a compile-time error if the parent forgets to provide it.

```typescript
@Input({ required: true }) student!: Student;
```

The `!` (non-null assertion) tells TypeScript "I know this will be set by the time it is used, even though it starts as undefined."

---

## 3. `@Output()` and `EventEmitter` — Sending Events from Child to Parent

`@Output()` marks a component property as an **event stream** that the parent can listen to. It is always paired with `EventEmitter<T>`, where `T` is the type of data the event carries.

### Syntax in the Child Component

```typescript
import { Component, Output, EventEmitter } from '@angular/core';

export class ChildComponent {
  @Output() buttonClicked = new EventEmitter<string>();

  onClick(): void {
    this.buttonClicked.emit('Hello from child!');
  }
}
```

### Syntax in the Parent Template

```html
<!-- (buttonClicked) is the @Output name; handleClick($event) is the parent method -->
<app-child (buttonClicked)="handleClick($event)"></app-child>
```

Parentheses on the left match the `@Output()` property name — just like event binding on a native DOM element. `$event` holds the value passed to `emit()`.

### Naming Convention

`@Output()` names should be **camelCase verbs** that describe what happened, not what to do:

```typescript
// Good — describes the event
@Output() studentSelected = new EventEmitter<Student>();
@Output() formSubmitted = new EventEmitter<void>();
@Output() itemDeleted = new EventEmitter<number>();

// Avoid — sounds like a command
@Output() onSelect = new EventEmitter<Student>();
```

---

## 4. The Data Flow Pattern

```
StudentListComponent (Parent)
│
│  selectedStudentId: number | null = null
│
│  [student]="student"          → passes Student object down
│  [isSelected]="student.id === selectedStudentId"  → passes boolean down
│  (studentSelected)="onStudentSelected($event)"    → listens for event up
│
└──► StudentCardComponent (Child)
     │
     │  @Input() student: Student
     │  @Input() isSelected: boolean
     │  @Output() studentSelected = new EventEmitter<Student>()
     │
     └──► User clicks card → emit(this.student) → parent receives $event
```

The parent owns the selected state. The child receives it and reports clicks.

---

## 5. Updating `StudentCardComponent`

### `src/app/student-card/student-card.component.ts`

```typescript
import { Component, Input, Output, EventEmitter } from '@angular/core';
import {
  NgClass, NgStyle,
  TitleCasePipe, DatePipe, LowerCasePipe, UpperCasePipe
} from '@angular/common';
import { Student } from '../models/student.model';
import { CourseBadgePipe } from '../pipes/course-badge.pipe';
import { EnrolmentYearPipe } from '../pipes/enrolment-year.pipe';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [
    NgClass, NgStyle,
    TitleCasePipe, DatePipe, LowerCasePipe, UpperCasePipe,
    CourseBadgePipe, EnrolmentYearPipe
  ],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {
  // INPUT — receives data from parent
  @Input({ required: true }) student!: Student;
  @Input() isSelected: boolean = false;

  // OUTPUT — emits an event carrying the student object
  @Output() studentSelected = new EventEmitter<Student>();

  onCardClick(): void {
    this.studentSelected.emit(this.student);
  }

  getAvatarColour(): string {
    const colours = ['#1a73e8', '#e8710a', '#1e8e3e', '#d93025', '#7627bb'];
    return colours[this.student.id % colours.length];
  }
}
```

**What changed:**
- Removed `isSelected = false` (internal state) — it is now an `@Input()`
- Removed `toggleSelect()` — replaced with `onCardClick()` which emits
- Added `@Output() studentSelected` with `EventEmitter<Student>`
- Added `@Input({ required: true })` to enforce the parent must provide `student`

### `src/app/student-card/student-card.component.html`

```html
<div
  class="student-card"
  [ngClass]="{ 'selected': isSelected }"
  (click)="onCardClick()">

  <div class="card-header">
    <div class="avatar" [ngStyle]="{ 'background-color': getAvatarColour() }">
      {{ student.name[0] | uppercase }}
    </div>
    <div class="student-info">
      <h3>{{ student.name | titlecase }}</h3>
      <span class="student-id">STU-{{ student.id.toString().padStart(3, '0') }}</span>
    </div>
    @if (isSelected) {
      <span class="selected-check">✔</span>
    }
  </div>

  <div class="card-body">
    <div class="detail-row">
      <span class="label">Course</span>
      <span class="value course-badge course-{{ student.course | courseBadge }}">
        {{ student.course }}
      </span>
    </div>
    <div class="detail-row">
      <span class="label">Year</span>
      <span class="value">{{ student.year | enrolmentYear }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Email</span>
      <span class="value email">{{ student.email | lowercase }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Enrolled</span>
      <span class="value">{{ student.enrolmentDate | date:'MMM yyyy' }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Status</span>
      <span class="value">
        @switch (student.status) {
          @case ('active')    { <span class="badge badge-active">● Active</span> }
          @case ('inactive')  { <span class="badge badge-inactive">● Inactive</span> }
          @case ('graduated') { <span class="badge badge-graduated">✓ Graduated</span> }
        }
      </span>
    </div>
  </div>

  <div class="card-footer">
    <button class="btn btn-view" (click)="$event.stopPropagation()">View</button>
    <button class="btn btn-edit" (click)="$event.stopPropagation()">Edit</button>
  </div>

</div>
```

Add to `student-card.component.css`:

```css
.selected-check {
  margin-left: auto;
  color: #1a73e8;
  font-size: 18px;
  font-weight: bold;
}
```

---

## 6. Updating `StudentListComponent`

The parent now owns `selectedStudent` and responds to the child's event.

### `src/app/student-list/student-list.component.ts`

```typescript
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe, DatePipe } from '@angular/common';
import { StudentCardComponent } from '../student-card/student-card.component';
import { Student } from '../models/student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [StudentCardComponent, FormsModule, TitleCasePipe, DatePipe],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  sectionTitle = 'All Students';
  searchTerm = '';
  showSearch = false;
  selectedStudent: Student | null = null;   // ← lifted state

  students: Student[] = [
    { id: 1, name: 'alice johnson',  course: 'Computer Science', year: 2, email: 'alice@uni.edu',  status: 'active',    enrolmentDate: new Date('2023-09-01') },
    { id: 2, name: 'bob martinez',   course: 'Mathematics',      year: 3, email: 'bob@uni.edu',    status: 'active',    enrolmentDate: new Date('2022-09-01') },
    { id: 3, name: 'carol williams', course: 'Physics',          year: 1, email: 'carol@uni.edu',  status: 'inactive',  enrolmentDate: new Date('2024-09-01') },
    { id: 4, name: 'david chen',     course: 'Computer Science', year: 4, email: 'david@uni.edu',  status: 'graduated', enrolmentDate: new Date('2021-09-01') },
    { id: 5, name: 'emma davis',     course: 'Engineering',      year: 2, email: 'emma@uni.edu',   status: 'active',    enrolmentDate: new Date('2023-09-01') },
  ];

  get filteredStudents(): Student[] {
    if (!this.searchTerm.trim()) return this.students;
    const term = this.searchTerm.toLowerCase();
    return this.students.filter(s =>
      s.name.toLowerCase().includes(term) ||
      s.course.toLowerCase().includes(term)
    );
  }

  get activeCount(): number {
    return this.students.filter(s => s.status === 'active').length;
  }

  // Called when a StudentCardComponent emits studentSelected
  onStudentSelected(student: Student): void {
    // Toggle: clicking the same card again deselects it
    this.selectedStudent = this.selectedStudent?.id === student.id ? null : student;
  }

  closeDetail(): void {
    this.selectedStudent = null;
  }

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) this.searchTerm = '';
  }
}
```

### `src/app/student-list/student-list.component.html`

```html
<section class="student-list-section">

  <!-- Header -->
  <div class="section-header">
    <div class="title-area">
      <h2>{{ sectionTitle }}</h2>
      <span class="count-badge">{{ students.length }} total · {{ activeCount }} active</span>
    </div>
    <div class="action-buttons">
      <button class="btn-search" (click)="toggleSearch()">
        {{ showSearch ? '✕ Close' : '🔍 Search' }}
      </button>
      <button class="btn-add">+ Add Student</button>
    </div>
  </div>

  <!-- Search bar -->
  <div class="search-area" [class.visible]="showSearch">
    <input
      type="text"
      [(ngModel)]="searchTerm"
      placeholder="Search by name or course..."
      class="search-input">
  </div>

  <!-- Results summary -->
  @if (searchTerm) {
    <p class="results-summary">
      @if (filteredStudents.length > 0) {
        Showing {{ filteredStudents.length }} result(s) for "<strong>{{ searchTerm }}</strong>"
      } @else {
        No students match "<strong>{{ searchTerm }}</strong>"
      }
    </p>
  }

  <!-- Main content: grid + detail panel side by side -->
  <div class="content-layout" [class.has-detail]="selectedStudent">

    <!-- Student grid -->
    <div class="student-grid">
      @for (student of filteredStudents; track student.id) {
        <app-student-card
          [student]="student"
          [isSelected]="selectedStudent?.id === student.id"
          (studentSelected)="onStudentSelected($event)">
        </app-student-card>
      } @empty {
        <div class="empty-state">
          <p>🎓 No students found.</p>
        </div>
      }
    </div>

    <!-- Detail panel: only shown when a student is selected -->
    @if (selectedStudent) {
      <aside class="detail-panel">
        <div class="detail-header">
          <h3>Student Detail</h3>
          <button class="btn-close" (click)="closeDetail()">✕</button>
        </div>
        <div class="detail-body">
          <div class="detail-avatar">
            {{ selectedStudent.name[0].toUpperCase() }}
          </div>
          <h2 class="detail-name">{{ selectedStudent.name | titlecase }}</h2>
          <p class="detail-id">STU-{{ selectedStudent.id.toString().padStart(3, '0') }}</p>

          <dl class="detail-list">
            <dt>Course</dt>
            <dd>{{ selectedStudent.course }}</dd>

            <dt>Year</dt>
            <dd>Year {{ selectedStudent.year }}</dd>

            <dt>Email</dt>
            <dd>{{ selectedStudent.email }}</dd>

            <dt>Status</dt>
            <dd class="status-{{ selectedStudent.status }}">
              {{ selectedStudent.status | titlecase }}
            </dd>

            <dt>Enrolled</dt>
            <dd>{{ selectedStudent.enrolmentDate | date:'dd MMMM yyyy' }}</dd>
          </dl>

          <div class="detail-actions">
            <button class="btn btn-primary">Edit Student</button>
            <button class="btn btn-danger">Delete</button>
          </div>
        </div>
      </aside>
    }

  </div>

</section>
```

**Key template bindings explained:**

| Binding | Type | Purpose |
|---|---|---|
| `[student]="student"` | `@Input` | Passes the loop item into the card |
| `[isSelected]="selectedStudent?.id === student.id"` | `@Input` | Passes a boolean — true only for the currently selected card |
| `(studentSelected)="onStudentSelected($event)"` | `@Output` | Parent listens for the event; `$event` is the `Student` object emitted |

### Add layout styles to `student-list.component.css`

```css
/* Add to existing CSS */

.content-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  transition: grid-template-columns 0.3s ease;
}

.content-layout.has-detail {
  grid-template-columns: 1fr 320px;
}

/* Detail panel */
.detail-panel {
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  align-self: start;
  position: sticky;
  top: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background-color: #1a73e8;
  color: white;
}

.detail-header h3 {
  margin: 0;
  font-size: 16px;
}

.btn-close {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  padding: 0 4px;
  opacity: 0.8;
}

.btn-close:hover { opacity: 1; }

.detail-body {
  padding: 20px;
  text-align: center;
}

.detail-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background-color: #1a73e8;
  color: white;
  font-size: 28px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.detail-name {
  margin: 0 0 4px;
  font-size: 20px;
  color: #1a1a2e;
}

.detail-id {
  margin: 0 0 20px;
  font-size: 13px;
  color: #888;
}

.detail-list {
  text-align: left;
  margin: 0 0 20px;
}

.detail-list dt {
  font-size: 11px;
  text-transform: uppercase;
  color: #999;
  letter-spacing: 0.5px;
  margin-top: 12px;
}

.detail-list dd {
  margin: 3px 0 0;
  font-size: 14px;
  color: #1a1a2e;
  font-weight: 500;
}

.status-active    { color: #1e8e3e; }
.status-inactive  { color: #d93025; }
.status-graduated { color: #1a73e8; }

.detail-actions {
  display: flex;
  gap: 8px;
}

.btn-primary {
  flex: 1;
  padding: 9px;
  background-color: #1a73e8;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}

.btn-danger {
  padding: 9px 14px;
  background-color: #fce8e6;
  color: #d93025;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}

.btn-primary:hover { background-color: #1557b0; }
.btn-danger:hover  { background-color: #f5c6c2; }
```

---

## 7. Seeing Component Communication in Action

Open **http://localhost:4200**:

1. **Click a student card** — the card gains a blue border and a checkmark. A detail panel slides in on the right.
2. **Click a different card** — the previous card deselects, the new one highlights, and the detail panel updates.
3. **Click the same card again** — it deselects and the panel closes.
4. **Click ✕ in the panel** — the panel closes, the card deselects.

The data flows in a clean, traceable loop:
```
User clicks card
  → StudentCardComponent.onCardClick()
  → studentSelected.emit(this.student)
  → StudentListComponent.onStudentSelected($event)
  → selectedStudent = student
  → [isSelected]="selectedStudent?.id === student.id" re-evaluated for all cards
  → Only the matching card gets isSelected = true
```

---

## 8. The Application So Far

```
StudentListComponent (owns selectedStudent state)
│
├── [student]="student"              → passes Student data down
├── [isSelected]="id === selected"   → passes selection state down
├── (studentSelected)="handler()"    → receives click events up
│
└──► StudentCardComponent (stateless — receives everything from parent)
     ├── @Input() student: Student
     ├── @Input() isSelected: boolean
     └── @Output() studentSelected: EventEmitter<Student>
```

**Current state:** A fully interactive student list with a slide-in detail panel. State flows correctly — owned by the parent, rendered by the child, reported back through events.

---

## Phase 6 Summary

| Concept | What You Learned |
|---|---|
| `@Input()` | Marks a property as receivable from a parent's template via `[propName]` |
| `@Input({ required: true })` | Compile-time enforcement that the parent must provide this value |
| `@Output()` | Marks a property as an event stream the parent can listen to via `(eventName)` |
| `EventEmitter<T>` | The event stream — call `.emit(value)` to fire the event |
| `$event` | In the parent template — holds the value passed to `.emit()` |
| Lifting state up | Move shared state to the lowest common parent; children receive it as inputs |
| `selectedStudent?.id` | Optional chaining — safe access when `selectedStudent` may be `null` |
| Toggle pattern | `this.selected = this.selected?.id === id ? null : item` |

---

## What's Next

In **Phase 7 — Application Architecture**, you will learn **Angular Services** and **Dependency Injection**. Currently the student data lives inside `StudentListComponent`. In Phase 7 you will move it into a `StudentService` — a singleton that any component in the application can inject and use. This is the same architectural separation you know from Spring's `@Service` layer.

