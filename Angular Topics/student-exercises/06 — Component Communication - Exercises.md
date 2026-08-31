# Phase 6 — Component Communication: Practice Exercises

**Prerequisites:** Completed Phase 6 lesson on Component Communication  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Selectable Student Cards with `@Input` and `@Output`

**Objective:** Wire `@Input()` and `@Output()` between a parent list and a child card so selection state lifts upward.

**Scenario:** `StudentListComponent` owns an array of students and a `selectedStudent`. Each `StudentCardComponent` receives its student via `@Input` and emits a selection event via `@Output`.

### Steps

**1. Define the Student model (if not already present):**

```typescript
// src/app/models/student.model.ts
export interface Student {
  id: number;
  name: string;
  course: string;
  year: number;
  status: 'active' | 'inactive' | 'pending';
  gpa: number;
}
```

**2. Build `StudentCardComponent`:**

```typescript
// student-card.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {
  @Input({ required: true }) student!: Student;
  @Input() isSelected: boolean = false;

  @Output() studentSelected = new EventEmitter<Student>();
  @Output() studentDeselected = new EventEmitter<Student>();

  toggleSelect(): void {
    if (this.isSelected) {
      this.studentDeselected.emit(this.student);
    } else {
      this.studentSelected.emit(this.student);
    }
  }
}
```

```html
<!-- student-card.component.html -->
<div class="card" [class.selected]="isSelected" (click)="toggleSelect()">
  <div class="card-top">
    <h4>{{ student.name }}</h4>
    <span class="course-badge">{{ student.course }}</span>
  </div>
  <p>Year {{ student.year }} · GPA {{ student.gpa | number:'1.2-2' }}</p>
  <span [class]="'status status-' + student.status">{{ student.status | titlecase }}</span>
  @if (isSelected) {
    <div class="selected-banner">✓ Selected</div>
  }
</div>
```

```css
/* student-card.component.css */
.card { border: 2px solid #ddd; border-radius: 10px; padding: 14px; cursor: pointer; transition: border-color .2s, box-shadow .2s; }
.card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.1); }
.card.selected { border-color: #1976d2; background: #e3f2fd; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.course-badge { background: #e0e0e0; padding: 2px 8px; border-radius: 10px; font-size: .8rem; }
.selected-banner { margin-top: 8px; color: #1565c0; font-weight: bold; font-size: .85rem; }
.status { font-size: .78rem; padding: 2px 8px; border-radius: 8px; }
.status-active   { background: #c8e6c9; color: #1b5e20; }
.status-inactive { background: #eee;    color: #555;    }
.status-pending  { background: #fff9c4; color: #f57f17; }
```

**3. Build `StudentListComponent` (parent):**

```typescript
// student-list.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentCardComponent } from '../student-card/student-card.component';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [CommonModule, StudentCardComponent],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  students: Student[] = [
    { id: 1, name: 'Aanya Sharma', course: 'CSE', year: 3, status: 'active',   gpa: 8.76 },
    { id: 2, name: 'Rohan Mehta',  course: 'ECE', year: 1, status: 'inactive', gpa: 7.30 },
    { id: 3, name: 'Priya Nair',   course: 'CSE', year: 2, status: 'active',   gpa: 9.12 },
    { id: 4, name: 'Karan Singh',  course: 'MBA', year: 4, status: 'pending',  gpa: 8.00 },
    { id: 5, name: 'Divya Pillai', course: 'ECE', year: 2, status: 'inactive', gpa: 6.89 },
  ];

  selectedStudent: Student | null = null;

  onStudentSelected(student: Student): void   { this.selectedStudent = student; }
  onStudentDeselected(student: Student): void { this.selectedStudent = null; }

  get averageGpa(): number {
    const sum = this.students.reduce((acc, s) => acc + s.gpa, 0);
    return sum / this.students.length;
  }
}
```

```html
<!-- student-list.component.html -->
<h2>Students</h2>
<p>Avg GPA: {{ averageGpa | number:'1.2-2' }}</p>

<div class="grid">
  @for (student of students; track student.id) {
    <app-student-card
      [student]="student"
      [isSelected]="selectedStudent?.id === student.id"
      (studentSelected)="onStudentSelected($event)"
      (studentDeselected)="onStudentDeselected($event)"
    />
  }
</div>

@if (selectedStudent) {
  <aside class="detail-panel">
    <h3>Selected: {{ selectedStudent.name }}</h3>
    <p>Course: {{ selectedStudent.course }} · Year {{ selectedStudent.year }}</p>
    <p>GPA: {{ selectedStudent.gpa | number:'1.2-2' }}</p>
    <button (click)="selectedStudent = null">Deselect</button>
  </aside>
}
```

### What to Verify
- Clicking a card highlights it and populates the detail panel.
- Clicking the same card again or pressing **Deselect** clears the selection.
- Only one card can be selected at a time.

---

## Exercise 2: Delete Confirmation with `@Output`

**Objective:** Emit a delete request from the child card up to the parent, which then confirms and removes the student.

### Steps

**1. Add a delete output to `StudentCardComponent`:**

```typescript
@Output() deleteRequested = new EventEmitter<Student>();

onDelete(event: MouseEvent): void {
  event.stopPropagation(); // prevent card selection toggle
  this.deleteRequested.emit(this.student);
}
```

```html
<!-- Inside student-card.component.html -->
<button class="delete-btn" (click)="onDelete($event)">🗑 Remove</button>
```

**2. Handle deletion in `StudentListComponent`:**

```typescript
onDeleteRequested(student: Student): void {
  const confirmed = confirm(`Remove ${student.name} from the list?`);
  if (confirmed) {
    this.students = this.students.filter(s => s.id !== student.id);
    if (this.selectedStudent?.id === student.id) {
      this.selectedStudent = null;
    }
  }
}
```

```html
<!-- In student-list.component.html, add the output binding -->
<app-student-card
  [student]="student"
  [isSelected]="selectedStudent?.id === student.id"
  (studentSelected)="onStudentSelected($event)"
  (studentDeselected)="onStudentDeselected($event)"
  (deleteRequested)="onDeleteRequested($event)"
/>
```

### What to Verify
- Clicking **Remove** does not trigger card selection.
- After confirmation the card disappears from the grid.
- If the deleted student was selected, the detail panel closes.

---

## Exercise 3: Stats Bar as a Presentational Component

**Objective:** Build a pure presentational child component that receives summary data via `@Input` and has no logic of its own.

**Scenario:** A `StatsBarComponent` sits above the student grid and displays total, active, and average GPA — all passed in from the parent.

### Steps

**1. Generate `StatsBarComponent`:**

```bash
ng g c components/stats-bar
```

**2. Define the component (inputs only, no state):**

```typescript
// stats-bar.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stats-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stats-bar">
      <div class="stat">
        <span class="stat-value">{{ total }}</span>
        <span class="stat-label">Total Students</span>
      </div>
      <div class="stat">
        <span class="stat-value">{{ active }}</span>
        <span class="stat-label">Active</span>
      </div>
      <div class="stat">
        <span class="stat-value">{{ avgGpa | number:'1.2-2' }}</span>
        <span class="stat-label">Avg GPA</span>
      </div>
      <div class="stat">
        <span class="stat-value">{{ total - active }}</span>
        <span class="stat-label">Inactive / Pending</span>
      </div>
    </div>
  `,
  styles: [`
    .stats-bar { display: flex; gap: 16px; background: #1976d2; padding: 16px 24px; border-radius: 10px; margin-bottom: 20px; }
    .stat { background: rgba(255,255,255,.15); padding: 12px 20px; border-radius: 8px; text-align: center; flex: 1; }
    .stat-value { display: block; font-size: 1.8rem; font-weight: 700; color: #fff; }
    .stat-label { font-size: .78rem; color: rgba(255,255,255,.8); text-transform: uppercase; letter-spacing: .05em; }
  `]
})
export class StatsBarComponent {
  @Input({ required: true }) total!: number;
  @Input({ required: true }) active!: number;
  @Input({ required: true }) avgGpa!: number;
}
```

**3. Use it in the parent:**

```html
<!-- student-list.component.html -->
<app-stats-bar
  [total]="students.length"
  [active]="students.filter(s => s.status === 'active').length"
  [avgGpa]="averageGpa"
/>
```

### What to Verify
- Deleting a student instantly updates the stats bar totals.
- `StatsBarComponent` has no methods, no local data — it is purely a display component.
- Passing `required: true` inputs means Angular will warn if a parent forgets to bind them.

