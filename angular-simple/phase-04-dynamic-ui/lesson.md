# Phase 4 — Dynamic UI

## Where We Left Off

After Phase 3:
- The student table still has **hardcoded HTML rows**
- We have a `searchTerm` from two-way binding but it doesn't filter anything yet

In this phase, we will:
1. Define a `Student` type
2. Store students in a TypeScript array
3. Use `@for` to render rows **dynamically**
4. Use `@if` to show/hide content based on conditions
5. Actually filter the list using `searchTerm`

---

## 1. Define a Student Type

Create a file for the Student type:

### `src/app/student.model.ts` ← new file

```typescript
export interface Student {
  id: number;
  name: string;
  email: string;
}
```

> An `interface` in TypeScript is like a Java interface — it defines the shape of an object.
> We use it here to give our student data a clear, typed structure.

---

## 2. Update StudentListComponent

### `src/app/student-list/student-list.component.ts`

Replace the full file:

```typescript
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Student } from '../student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  pageTitle = 'Students';
  searchTerm = '';
  message = '';

  students: Student[] = [
    { id: 1, name: 'Alice', email: 'alice@example.com' },
    { id: 2, name: 'Bob', email: 'bob@example.com' },
    { id: 3, name: 'Charlie', email: 'charlie@example.com' },
  ];

  get filteredStudents(): Student[] {
    if (!this.searchTerm) return this.students;
    return this.students.filter(s =>
      s.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  onAddClick() {
    this.message = 'Add Student feature coming soon!';
  }
}
```

### What changed

| Change | Reason |
|---|---|
| Added `Student` import | Use the typed interface |
| Added `students` array | Real data instead of hardcoded HTML |
| Added `filteredStudents` getter | Filters by `searchTerm` from Phase 3 |
| Removed `isAddDisabled` | Not needed anymore, simplifying |

---

## 3. Modern Control Flow — `@for` and `@if`

Angular 17+ introduced built-in control flow directly in templates.

### `@for` — Loop through a collection

```html
@for (item of list; track item.id) {
  <p>{{ item.name }}</p>
}
```

- `track item.id` — tells Angular how to identify each item (required, for performance)

### `@if` — Conditionally show content

```html
@if (condition) {
  <p>Shown when true</p>
} @else {
  <p>Shown when false</p>
}
```

---

## 4. Update the Template

### `src/app/student-list/student-list.component.html`

Replace all content:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
    <button (click)="onAddClick()">Add Student</button>
  </div>

  @if (message) {
    <p class="message">{{ message }}</p>
  }

  <div class="search-bar">
    <input type="text" [(ngModel)]="searchTerm" placeholder="Search by name..." />
  </div>

  <p>Showing {{ filteredStudents.length }} of {{ students.length }} students</p>

  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Email</th>
      </tr>
    </thead>
    <tbody>
      @for (student of filteredStudents; track student.id) {
        <tr>
          <td>{{ student.id }}</td>
          <td>{{ student.name }}</td>
          <td>{{ student.email }}</td>
        </tr>
      }
      @if (filteredStudents.length === 0) {
        <tr>
          <td colspan="3">No students found.</td>
        </tr>
      }
    </tbody>
  </table>
</div>
```

### What to notice

- `@for` loops through `filteredStudents` and creates a `<tr>` for each one
- `@if (message)` — the message only shows **after** the button is clicked
- `@if (filteredStudents.length === 0)` — shows a "no results" row when search finds nothing
- `{{ student.id }}`, `{{ student.name }}` — interpolation inside the loop

---

## 5. Attribute Directives

Attribute directives **change the appearance** of an element based on a condition.

The most common one is `[ngClass]`.

### Example — highlight a row

Add this to the `<tr>` inside `@for`:

```html
@for (student of filteredStudents; track student.id) {
  <tr [ngClass]="{ 'highlight': student.id === 1 }">
    <td>{{ student.id }}</td>
    <td>{{ student.name }}</td>
    <td>{{ student.email }}</td>
  </tr>
}
```

And import `NgClass` in the component:

```typescript
import { NgClass } from '@angular/common';

@Component({
  ...
  imports: [FormsModule, NgClass],
  ...
})
```

Add the CSS:

```css
.highlight {
  background-color: #fff3cd;
}
```

`[ngClass]` adds the `highlight` class only when the condition is true.

> We will use this more meaningfully in Phase 6 when we select a student.

---

## 6. Legacy Structural Directives (For Reference)

Older Angular code uses `*ngFor` and `*ngIf` instead of `@for` / `@if`.

You will see these in older projects and tutorials:

```html
<!-- Old syntax -->
<tr *ngFor="let student of students">
  <td>{{ student.name }}</td>
</tr>

<p *ngIf="students.length === 0">No students found.</p>
```

They do the same thing. We use the **modern `@for` / `@if`** syntax going forward.

---

## 7. Run the App

```bash
ng serve
```

Test these:
- ✅ All 3 students appear in the table
- ✅ Type `"ali"` in the search box → only Alice appears
- ✅ Type something that matches nobody → "No students found."
- ✅ Click Add Student → message appears
- ✅ Alice's row has a yellow highlight (from `[ngClass]`)

---

## Phase 4 Summary

| Concept | Syntax | What it does |
|---|---|---|
| `@for` | `@for (item of list; track item.id)` | Loop through array |
| `@if` | `@if (condition)` | Show/hide content |
| `@else` | `} @else {` | Fallback content |
| `[ngClass]` | `[ngClass]="{ 'class': condition }"` | Add CSS class conditionally |
| Legacy | `*ngFor`, `*ngIf` | Old syntax, same purpose |

---

## Application State After Phase 4

```
✅ Student type defined (student.model.ts)
✅ students array in StudentListComponent
✅ @for — rows rendered dynamically
✅ @if — message shown only after click
✅ @if — "No students found" when search has no results
✅ filteredStudents getter — search actually works
✅ [ngClass] — highlighted row
```

**Next → Phase 5: Pipes**
We will format the displayed data (e.g., uppercase names, formatted dates) using Angular's built-in pipes and then create a simple custom pipe.
