# Phase 3 — Data Binding: Practice Exercises

**Prerequisites:** Completed Phase 3 lesson on Data Binding  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Live Student Counter Dashboard

**Objective:** Practice interpolation and property binding by building a statistics header that reacts to component state.

**Scenario:** The student list page needs a header bar that shows real-time counts and disables action buttons when the list is empty.

### Steps

**1. Update `StudentListComponent` with richer state:**

```typescript
// student-list.component.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  pageTitle = 'Student Management System';
  lastUpdated = new Date();
  searchQuery = '';
  showInactive = false;

  students = [
    { id: 1, name: 'Aanya Sharma',   course: 'CSE', year: 3, active: true  },
    { id: 2, name: 'Rohan Mehta',    course: 'ECE', year: 1, active: false },
    { id: 3, name: 'Priya Nair',     course: 'CSE', year: 2, active: true  },
    { id: 4, name: 'Karan Singh',    course: 'MBA', year: 4, active: true  },
    { id: 5, name: 'Divya Pillai',   course: 'ECE', year: 2, active: false },
  ];

  get totalStudents(): number {
    return this.students.length;
  }

  get activeStudents(): number {
    return this.students.filter(s => s.active).length;
  }

  get filteredStudents() {
    return this.students.filter(s => {
      const matchesSearch = s.name.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchesFilter = this.showInactive ? true : s.active;
      return matchesSearch && matchesFilter;
    });
  }

  get hasResults(): boolean {
    return this.filteredStudents.length > 0;
  }

  exportStudents(): void {
    alert(`Exporting ${this.filteredStudents.length} student records…`);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.showInactive = false;
  }
}
```

**2. Build the template using all four binding types:**

```html
<!-- student-list.component.html -->

<!-- Interpolation: page title and live stats -->
<div class="list-header">
  <h2>{{ pageTitle }}</h2>
  <p class="subtitle">
    Showing {{ filteredStudents.length }} of {{ totalStudents }} students
    ({{ activeStudents }} active)
  </p>
  <p class="last-updated">Last updated: {{ lastUpdated }}</p>
</div>

<!-- Two-way binding: search input -->
<div class="search-bar">
  <input
    type="text"
    [(ngModel)]="searchQuery"
    placeholder="Search by name…"
  />

  <!-- Property binding: disable when nothing to clear -->
  <button
    [disabled]="searchQuery === '' && !showInactive"
    (click)="clearSearch()"
  >
    Clear
  </button>
</div>

<!-- Two-way binding: toggle checkbox -->
<label class="toggle-label">
  <input type="checkbox" [(ngModel)]="showInactive" />
  Show inactive students
</label>

<!-- Temporary list output -->
<ul>
  <li *ngFor="let s of filteredStudents">
    {{ s.name }} — {{ s.course }} Year {{ s.year }}
    ({{ s.active ? 'Active' : 'Inactive' }})
  </li>
</ul>

<!-- Event binding + property binding on Export button -->
<button
  [disabled]="!hasResults"
  [title]="hasResults ? 'Export ' + filteredStudents.length + ' records' : 'No records to export'"
  (click)="exportStudents()"
>
  Export
</button>
```

> **Note:** `*ngFor` is used here temporarily — you will replace it with `@for` in Phase 4.

### What to Verify
- Typing in the search box immediately filters the list (two-way binding in action).
- The **Clear** button is disabled when the search field is empty and the toggle is off.
- The **Export** button is disabled when no results match the current filter.
- The subtitle updates live as you type.

### Challenge
Add a `sortOrder: 'asc' | 'desc' = 'asc'` property and a **Sort** button that toggles it. Use property binding on a `[class.sorted-desc]` class to rotate an arrow icon.

---

## Exercise 2: Inline Edit Toggle with Event Binding

**Objective:** Practice event binding and class/style property binding to build an inline editable student row.

**Scenario:** Each student row should have an **Edit** button that reveals an inline text field to rename the student without navigating away.

### Steps

**1. Create `InlineEditComponent`:**

```bash
ng g c components/inline-edit
```

**2. Implement the component:**

```typescript
// inline-edit.component.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface EditableStudent {
  id: number;
  name: string;
  course: string;
  year: number;
  editMode: boolean;
  draftName: string;
}

@Component({
  selector: 'app-inline-edit',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './inline-edit.component.html',
  styleUrl: './inline-edit.component.css'
})
export class InlineEditComponent {
  students: EditableStudent[] = [
    { id: 1, name: 'Aanya Sharma', course: 'CSE', year: 3, editMode: false, draftName: '' },
    { id: 2, name: 'Rohan Mehta',  course: 'ECE', year: 1, editMode: false, draftName: '' },
    { id: 3, name: 'Priya Nair',   course: 'CSE', year: 2, editMode: false, draftName: '' },
  ];

  startEdit(student: EditableStudent): void {
    // Close any other open edit
    this.students.forEach(s => { if (s !== student) s.editMode = false; });
    student.draftName = student.name;
    student.editMode = true;
  }

  saveEdit(student: EditableStudent): void {
    const trimmed = student.draftName.trim();
    if (trimmed.length > 0) {
      student.name = trimmed;
    }
    student.editMode = false;
  }

  cancelEdit(student: EditableStudent): void {
    student.editMode = false;
  }
}
```

**3. Build the template:**

```html
<!-- inline-edit.component.html -->
<h3>Student List (Inline Edit)</h3>

<table class="student-table">
  <thead>
    <tr>
      <th>#</th>
      <th>Name</th>
      <th>Course</th>
      <th>Year</th>
      <th>Actions</th>
    </tr>
  </thead>
  <tbody>
    <tr *ngFor="let student of students"
        [class.editing]="student.editMode">

      <td>{{ student.id }}</td>

      <!-- Show input OR display text based on editMode -->
      <td>
        <span *ngIf="!student.editMode">{{ student.name }}</span>
        <input
          *ngIf="student.editMode"
          type="text"
          [(ngModel)]="student.draftName"
          (keyup.enter)="saveEdit(student)"
          (keyup.escape)="cancelEdit(student)"
        />
      </td>

      <td>{{ student.course }}</td>
      <td>{{ student.year }}</td>

      <td class="actions">
        <!-- Event binding: edit/save/cancel -->
        <button *ngIf="!student.editMode"
                (click)="startEdit(student)">
          Edit
        </button>

        <ng-container *ngIf="student.editMode">
          <button (click)="saveEdit(student)"
                  [disabled]="student.draftName.trim().length === 0">
            Save
          </button>
          <button (click)="cancelEdit(student)">Cancel</button>
        </ng-container>
      </td>
    </tr>
  </tbody>
</table>
```

**4. Add styles for the editing state:**

```css
/* inline-edit.component.css */
.student-table { width: 100%; border-collapse: collapse; }
.student-table th, .student-table td { padding: 8px 12px; border: 1px solid #ddd; }
.student-table tr.editing { background: #fffbe6; }
.actions { display: flex; gap: 6px; }
input[type="text"] { padding: 4px 8px; border: 1px solid #aaa; border-radius: 4px; }
```

### What to Verify
- Clicking **Edit** opens the inline input pre-filled with the current name.
- Pressing **Enter** saves; pressing **Escape** cancels.
- Clicking **Save** with an empty field is disabled (property binding on `[disabled]`).
- Only one row can be in edit mode at a time.

### Challenge
Add a **Delete** button that removes the student from the array using `(click)` event binding and `Array.splice()`.

---

## Exercise 3: Real-Time Form Preview with Two-Way Binding

**Objective:** Use two-way binding (`[(ngModel)]`) to build a "live preview" card that mirrors a student registration form in real time.

**Scenario:** When a trainer fills in a new student form, a preview card on the right should update with every keystroke — no submit button needed.

### Steps

**1. Create `StudentPreviewComponent`:**

```bash
ng g c components/student-preview
```

**2. Implement the component:**

```typescript
// student-preview.component.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-student-preview',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './student-preview.component.html',
  styleUrl: './student-preview.component.css'
})
export class StudentPreviewComponent {
  // Form model — two-way bound to inputs
  name    = '';
  email   = '';
  course  = 'CSE';
  year    = 1;
  active  = true;

  courses = ['CSE', 'ECE', 'MBA', 'BCA', 'MCA'];

  get isFormReady(): boolean {
    return this.name.trim().length > 0 && this.email.includes('@');
  }

  reset(): void {
    this.name   = '';
    this.email  = '';
    this.course = 'CSE';
    this.year   = 1;
    this.active = true;
  }
}
```

**3. Build the two-panel template:**

```html
<!-- student-preview.component.html -->
<div class="preview-layout">

  <!-- LEFT: Form -->
  <div class="form-panel">
    <h3>New Student Registration</h3>

    <div class="field">
      <label>Full Name</label>
      <input type="text" [(ngModel)]="name" placeholder="Enter full name" />
    </div>

    <div class="field">
      <label>Email</label>
      <input type="email" [(ngModel)]="email" placeholder="student@college.edu" />
    </div>

    <div class="field">
      <label>Course</label>
      <select [(ngModel)]="course">
        <option *ngFor="let c of courses" [value]="c">{{ c }}</option>
      </select>
    </div>

    <div class="field">
      <label>Year ({{ year }})</label>
      <input type="range" min="1" max="4" [(ngModel)]="year" />
    </div>

    <div class="field checkbox-field">
      <label>
        <input type="checkbox" [(ngModel)]="active" />
        Active Student
      </label>
    </div>

    <button (click)="reset()" [disabled]="!isFormReady">Reset</button>
  </div>

  <!-- RIGHT: Live Preview Card -->
  <div class="preview-panel">
    <h3>Live Preview</h3>

    <div class="student-card" [class.inactive]="!active">
      <div class="card-header" [style.background-color]="active ? '#4caf50' : '#9e9e9e'">
        <span class="status-dot"></span>
        {{ active ? 'Active' : 'Inactive' }}
      </div>

      <div class="card-body">
        <p class="student-name">{{ name || '— name —' }}</p>
        <p class="student-email">{{ email || '— email —' }}</p>
        <p class="student-meta">
          {{ course }} · Year {{ year }}
        </p>
      </div>

      <div class="card-footer">
        <small [style.color]="isFormReady ? 'green' : 'gray'">
          {{ isFormReady ? '✓ Ready to submit' : 'Fill in name and email to continue' }}
        </small>
      </div>
    </div>
  </div>
</div>
```

**4. Style the two-panel layout:**

```css
/* student-preview.component.css */
.preview-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; padding: 24px; }
.form-panel, .preview-panel { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.field { display: flex; flex-direction: column; margin-bottom: 14px; gap: 4px; }
label { font-size: .85rem; font-weight: 600; color: #555; }
input, select { padding: 8px 10px; border: 1px solid #ccc; border-radius: 6px; font-size: 1rem; }
.checkbox-field { flex-direction: row; align-items: center; gap: 8px; }
.student-card { border-radius: 10px; overflow: hidden; border: 1px solid #ddd; }
.card-header { padding: 10px 16px; color: #fff; font-weight: bold; display: flex; align-items: center; gap: 8px; }
.card-body { padding: 16px; }
.card-footer { padding: 8px 16px; background: #f5f5f5; }
.student-name { font-size: 1.2rem; font-weight: 700; margin: 0 0 4px; }
.student-email { color: #666; margin: 0 0 8px; font-size: .9rem; }
.student-meta { font-size: .85rem; color: #888; }
.inactive .card-body { opacity: .6; }
```

### What to Verify
- The preview card updates with every keystroke in the name and email fields.
- The range slider moves the year display in real time.
- The card header colour switches immediately when the checkbox is toggled.
- The "Ready to submit" indicator turns green only when both name and email are filled.

### Challenge
Add a `phoneNumber` field with a `[(ngModel)]` binding and validate it as exactly 10 digits. Use property binding on `[style.border-color]` of the input to show red for invalid and green for valid.

