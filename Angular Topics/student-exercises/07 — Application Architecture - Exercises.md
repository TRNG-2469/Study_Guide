# Phase 7 — Application Architecture: Practice Exercises

**Prerequisites:** Completed Phase 7 lesson on Services and Dependency Injection  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Inject `StudentService` Using Both Patterns

**Objective:** Practice both `inject()` and constructor injection so you understand when each is appropriate.

**Scenario:** Two sibling components — `StudentListComponent` and `NavbarComponent` — both consume the same `StudentService` singleton. One uses `inject()`, the other uses constructor injection.

### Steps

**1. Verify `StudentService` is provided at root:**

```typescript
// src/app/services/student.service.ts
import { Injectable } from '@angular/core';
import { Student } from '../models/student.model';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private students: Student[] = [
    { id: 1, name: 'Aanya Sharma', course: 'CSE', year: 3, status: 'active',   gpa: 8.76 },
    { id: 2, name: 'Rohan Mehta',  course: 'ECE', year: 1, status: 'inactive', gpa: 7.30 },
    { id: 3, name: 'Priya Nair',   course: 'CSE', year: 2, status: 'active',   gpa: 9.12 },
    { id: 4, name: 'Karan Singh',  course: 'MBA', year: 4, status: 'pending',  gpa: 8.00 },
    { id: 5, name: 'Divya Pillai', course: 'ECE', year: 2, status: 'inactive', gpa: 6.89 },
  ];
  private nextId = 6;

  getAll(): Student[]                        { return [...this.students]; }
  getById(id: number): Student | undefined   { return this.students.find(s => s.id === id); }
  getActiveCount(): number                   { return this.students.filter(s => s.status === 'active').length; }

  add(data: Omit<Student, 'id'>): Student {
    const student: Student = { id: this.nextId++, ...data };
    this.students.push(student);
    return student;
  }

  update(id: number, changes: Partial<Omit<Student, 'id'>>): boolean {
    const idx = this.students.findIndex(s => s.id === id);
    if (idx === -1) return false;
    this.students[idx] = { ...this.students[idx], ...changes };
    return true;
  }

  delete(id: number): boolean {
    const before = this.students.length;
    this.students = this.students.filter(s => s.id !== id);
    return this.students.length < before;
  }
}
```

**2. Use `inject()` in `StudentListComponent`:**

```typescript
// student-list.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student.service';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './student-list.component.html',
})
export class StudentListComponent implements OnInit {
  // Modern injection pattern — no constructor needed
  private studentService = inject(StudentService);

  students: Student[] = [];

  ngOnInit(): void {
    this.students = this.studentService.getAll();
  }

  delete(id: number): void {
    this.studentService.delete(id);
    this.students = this.studentService.getAll();
  }
}
```

**3. Use constructor injection in `NavbarComponent`:**

```typescript
// navbar.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="navbar">
      <span class="brand">SMS</span>
      <span class="badge">{{ activeCount }} active</span>
    </nav>
  `,
  styles: [`
    .navbar { background: #1976d2; color: #fff; padding: 12px 24px; display: flex; justify-content: space-between; align-items: center; }
    .badge  { background: #fff; color: #1976d2; padding: 4px 12px; border-radius: 20px; font-weight: bold; }
  `]
})
export class NavbarComponent implements OnInit {
  activeCount = 0;

  // Classic constructor injection
  constructor(private studentService: StudentService) {}

  ngOnInit(): void {
    this.activeCount = this.studentService.getActiveCount();
  }
}
```

### What to Verify
- Both components display data from the **same service instance** (not two separate copies).
- Deleting a student in `StudentListComponent` and re-initialising the navbar shows the updated count.
- Console log `inject(StudentService) === constructor(StudentService)` — Angular resolves to the same object.

### Reflection Questions
1. When would you prefer `inject()` over constructor injection?
2. What happens if you accidentally write `new StudentService()` inside a component instead of injecting it?

---

## Exercise 2: CRUD Roundtrip Through the Service

**Objective:** Exercise all four service methods (getAll, add, update, delete) from the template.

### Steps

**1. Build a mini CRUD UI in `StudentListComponent`:**

```html
<!-- student-list.component.html -->
<app-navbar />

<div class="container">
  <h2>Students ({{ students.length }})</h2>

  <!-- Quick Add Form -->
  <form class="quick-add" (ngSubmit)="quickAdd()">
    <input [(ngModel)]="newName"   name="name"   placeholder="Name"   required />
    <input [(ngModel)]="newCourse" name="course" placeholder="Course" required />
    <select [(ngModel)]="newYear"  name="year">
      <option [value]="1">Year 1</option>
      <option [value]="2">Year 2</option>
      <option [value]="3">Year 3</option>
      <option [value]="4">Year 4</option>
    </select>
    <button type="submit">Add Student</button>
  </form>

  <!-- Student Rows -->
  @for (student of students; track student.id) {
    <div class="student-row">
      @if (editingId === student.id) {
        <!-- Inline edit mode -->
        <input [(ngModel)]="editName" placeholder="Name" />
        <button (click)="saveEdit(student.id)">Save</button>
        <button (click)="cancelEdit()">Cancel</button>
      } @else {
        <span>{{ student.name }} · {{ student.course }} · Year {{ student.year }}</span>
        <button (click)="startEdit(student)">Edit</button>
        <button (click)="deleteStudent(student.id)">Delete</button>
      }
    </div>
  }
</div>
```

**2. Add the component logic:**

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { StudentService } from '../../services/student.service';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './student-list.component.html',
})
export class StudentListComponent implements OnInit {
  private svc = inject(StudentService);

  students: Student[] = [];

  // Quick-add state
  newName   = '';
  newCourse = 'CSE';
  newYear   = 1;

  // Inline edit state
  editingId: number | null = null;
  editName  = '';

  ngOnInit(): void { this.refresh(); }

  refresh(): void { this.students = this.svc.getAll(); }

  quickAdd(): void {
    if (!this.newName.trim()) return;
    this.svc.add({ name: this.newName.trim(), course: this.newCourse, year: this.newYear, status: 'active', gpa: 0 });
    this.newName = '';
    this.refresh();
  }

  startEdit(student: Student): void {
    this.editingId = student.id;
    this.editName  = student.name;
  }

  saveEdit(id: number): void {
    this.svc.update(id, { name: this.editName.trim() });
    this.editingId = null;
    this.refresh();
  }

  cancelEdit(): void { this.editingId = null; }

  deleteStudent(id: number): void {
    this.svc.delete(id);
    this.refresh();
  }
}
```

### What to Verify
- Adding a student appends them to the list immediately.
- Editing a name and saving updates it in place.
- Deleting removes the row.
- The navbar badge does NOT auto-update yet (that requires signals — covered in Phase 10). Note this limitation.

---

## Exercise 3: `CourseFilterService` — A Second Service

**Objective:** Create a second service that holds shared UI state (current course filter) and inject it into two components.

### Steps

**1. Generate the service:**

```bash
ng g s services/course-filter
```

**2. Implement it:**

```typescript
// course-filter.service.ts
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CourseFilterService {
  private _selected: string = 'ALL';

  get selected(): string        { return this._selected; }
  set selected(value: string)   { this._selected = value; }

  readonly options = ['ALL', 'CSE', 'ECE', 'MBA', 'BCA', 'MCA'];

  matches(course: string): boolean {
    return this._selected === 'ALL' || this._selected === course;
  }
}
```

**3. Add a filter bar component that writes to the service:**

```typescript
// filter-bar.component.ts
import { Component, inject, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CourseFilterService } from '../../services/course-filter.service';

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="filter-bar">
      @for (option of filter.options; track option) {
        <button [class.active]="filter.selected === option"
                (click)="select(option)">{{ option }}</button>
      }
    </div>
  `,
  styles: [`.filter-bar{display:flex;gap:8px;margin:12px 0}button{padding:6px 14px;border:1px solid #ddd;border-radius:20px;cursor:pointer}button.active{background:#1976d2;color:#fff;border-color:#1976d2}`]
})
export class FilterBarComponent {
  filter = inject(CourseFilterService);
  @Output() changed = new EventEmitter<string>();
  select(option: string): void { this.filter.selected = option; this.changed.emit(option); }
}
```

**4. Read from the service in `StudentListComponent`:**

```typescript
private filterSvc = inject(CourseFilterService);

get filteredStudents(): Student[] {
  return this.students.filter(s => this.filterSvc.matches(s.course));
}
```

### What to Verify
- Clicking a filter button immediately narrows the student list.
- Selecting **ALL** shows every student.
- Both the filter bar and the list share the same service instance — no prop drilling needed.

