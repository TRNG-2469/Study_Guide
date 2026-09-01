# Phase 6 — Component Communication

## Where We Left Off

After Phase 5, `StudentListComponent` does everything:
- Holds the student data
- Loops through students
- Displays each student row

As the app grows, it is better to **extract** the display of a single student into its own component — a `StudentCardComponent`.

This raises a question: **how do components share data?**

That's what this phase is about.

---

## The Two Directions

| Direction | Decorator | How |
|---|---|---|
| Parent → Child | `@Input()` | Parent passes data down via property binding |
| Child → Parent | `@Output()` + `EventEmitter` | Child fires an event that parent listens to |

---

## The Plan for This Phase

- Create `StudentCardComponent` — displays **one** student
- `StudentListComponent` (parent) passes each student to it using `@Input()`
- `StudentCardComponent` has a **Select** button
- When clicked, it emits an event using `@Output()`
- `StudentListComponent` receives the event and shows the selected student's name

---

## 1. Create StudentCardComponent

```bash
ng g c student-card
```

---

## 2. StudentCardComponent — Receives Data with `@Input()`

### `src/app/student-card/student-card.component.ts`

```typescript
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { TitleCasePipe, DatePipe } from '@angular/common';
import { Student } from '../student.model';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [TitleCasePipe, DatePipe],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {

  @Input() student!: Student;

  @Output() selected = new EventEmitter<Student>();

  onSelect() {
    this.selected.emit(this.student);
  }

}
```

### What's happening

| Code | Meaning |
|---|---|
| `@Input() student!: Student` | Accepts a `Student` object from the parent |
| `@Output() selected` | Declares an event this component can fire |
| `new EventEmitter<Student>()` | An event that carries a `Student` payload |
| `this.selected.emit(this.student)` | Fires the event when button is clicked |

> The `!` after `student` tells TypeScript: "I know this will be set by the parent, trust me."

This is like a Java callback — the child notifies the parent something happened.

---

### `src/app/student-card/student-card.component.html`

```html
<div class="student-card">
  <div class="card-info">
    <span class="student-id">#{{ student.id }}</span>
    <strong>{{ student.name | titlecase }}</strong>
    <span>{{ student.email }}</span>
    <span class="date">Enrolled: {{ student.enrolledDate | date: 'mediumDate' }}</span>
  </div>
  <button (click)="onSelect()">Select</button>
</div>
```

### `src/app/student-card/student-card.component.css`

```css
.student-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #fff;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.student-id {
  font-size: 12px;
  color: #888;
}

.date {
  font-size: 12px;
  color: #888;
}

button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
}
```

---

## 3. Update StudentListComponent — Pass Data with `@Input()`

### `src/app/student-list/student-list.component.ts`

```typescript
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Student } from '../student.model';
import { StudentCardComponent } from '../student-card/student-card.component';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule, StudentCardComponent],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  pageTitle = 'Students';
  searchTerm = '';
  selectedStudent: Student | null = null;

  students: Student[] = [
    { id: 1, name: 'alice johnson', email: 'alice@example.com', enrolledDate: '2024-01-15' },
    { id: 2, name: 'bob smith',     email: 'bob@example.com',   enrolledDate: '2024-03-22' },
    { id: 3, name: 'charlie brown', email: 'charlie@example.com', enrolledDate: '2024-06-10' },
  ];

  get filteredStudents(): Student[] {
    if (!this.searchTerm) return this.students;
    return this.students.filter(s =>
      s.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  onStudentSelected(student: Student) {
    this.selectedStudent = student;
  }
}
```

### What changed

- Replaced the table and pipes imports with `StudentCardComponent`
- Added `selectedStudent` — stores whichever student was selected
- Added `onStudentSelected()` — called when a card fires its event

---

### `src/app/student-list/student-list.component.html`

Replace all content:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
  </div>

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
      (selected)="onStudentSelected($event)">
    </app-student-card>
  }

  @if (filteredStudents.length === 0) {
    <p>No students found.</p>
  }
</div>
```

### The two key lines

```html
[student]="student"
```
→ Property binding — parent passes the `student` object **into** the child via `@Input()`

```html
(selected)="onStudentSelected($event)"
```
→ Event binding — parent listens to the child's `@Output()` event; `$event` is the emitted `Student`

---

### `src/app/student-list/student-list.component.css`

```css
.student-list {
  padding: 20px 30px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin: 16px 0;
}

.search-bar input {
  padding: 8px;
  width: 300px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.selected-banner {
  background-color: #d4edda;
  border: 1px solid #28a745;
  color: #155724;
  padding: 10px 16px;
  border-radius: 4px;
  margin-bottom: 12px;
}
```

---

## 4. Add `TitleCasePipe` to StudentListComponent

The selected banner uses `titlecase`. Import it:

```typescript
import { TitleCasePipe } from '@angular/common';

@Component({
  ...
  imports: [FormsModule, StudentCardComponent, TitleCasePipe],
  ...
})
```

---

## 5. Run the App

```bash
ng serve
```

Test:
- ✅ Three student cards appear
- ✅ Search still filters cards
- ✅ Click **Select** on any card → green banner shows that student's name
- ✅ Clicking a different card updates the banner

---

## Phase 6 Summary

| Concept | Decorator | Syntax |
|---|---|---|
| Parent → Child | `@Input()` | `[student]="student"` |
| Child → Parent | `@Output()` + `EventEmitter` | `(selected)="onStudentSelected($event)"` |
| Emit event | — | `this.selected.emit(value)` |

---

## Application State After Phase 6

```
✅ StudentCardComponent — displays one student, has Select button
✅ @Input() student — receives student data from parent
✅ @Output() selected — fires event when Select is clicked
✅ StudentListComponent — passes each student to StudentCardComponent
✅ onStudentSelected() — receives the selected student and shows banner
```

**Next → Phase 7: Services and Dependency Injection**
We will move the student data out of `StudentListComponent` into a dedicated `StudentService`,
so it can be shared across multiple components.
