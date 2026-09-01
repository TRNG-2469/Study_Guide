# Phase 9 — Forms

## Where We Left Off

After Phase 8, the app can navigate between a list page and a detail page.

We can **delete** students but cannot **add** or **edit** them.

In this phase, we add a form that handles **both Add and Edit** in one component.

---

## 1. Two Types of Forms in Angular

| Type | How it works |
|---|---|
| Template-driven | Form logic in the HTML using `ngModel` |
| **Reactive** | Form logic in TypeScript — more control, easier to validate |

We use **Reactive Forms**. The form structure and validation rules are defined in TypeScript, and the HTML just binds to them.

---

## 2. New Routes

We need two new routes in `app.routes.ts`:

| URL | Purpose |
|---|---|
| `/students/new` | Add a new student |
| `/students/:id/edit` | Edit an existing student |

### `src/app/app.routes.ts`

```typescript
import { Routes } from '@angular/router';
import { StudentListComponent } from './student-list/student-list.component';
import { StudentDetailComponent } from './student-detail/student-detail.component';
import { StudentFormComponent } from './student-form/student-form.component';
import { studentExistsGuard } from './student-exists.guard';

export const routes: Routes = [
  { path: '',                    component: StudentListComponent },
  { path: 'students/new',        component: StudentFormComponent },
  { path: 'students/:id',        component: StudentDetailComponent, canActivate: [studentExistsGuard] },
  { path: 'students/:id/edit',   component: StudentFormComponent,   canActivate: [studentExistsGuard] },
];
```

> **Important:** `students/new` must come **before** `students/:id`. Otherwise Angular matches `new` as an id.

---

## 3. Update StudentService

Add `addStudent()` and `updateStudent()`:

### `src/app/student.service.ts`

```typescript
import { Injectable } from '@angular/core';
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

  getStudents(): Student[] {
    return this.students;
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

> `Omit<Student, 'id'>` means "a Student object without the id field" — the service assigns the id.

---

## 4. Create StudentFormComponent

```bash
ng g c student-form
```

### `src/app/student-form/student-form.component.ts`

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { StudentService } from '../student.service';

@Component({
  selector: 'app-student-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './student-form.component.html',
  styleUrl: './student-form.component.css'
})
export class StudentFormComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private studentService = inject(StudentService);

  isEditMode = false;
  studentId: number | null = null;

  form = new FormGroup({
    name:         new FormControl('', [Validators.required, Validators.minLength(2)]),
    email:        new FormControl('', [Validators.required, Validators.email]),
    enrolledDate: new FormControl('', [Validators.required]),
  });

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.studentId = Number(id);
      const student = this.studentService.getStudentById(this.studentId);
      if (student) {
        this.form.setValue({
          name:         student.name,
          email:        student.email,
          enrolledDate: student.enrolledDate,
        });
      }
    }
  }

  onSubmit() {
    if (this.form.invalid) return;

    const data = this.form.value as { name: string; email: string; enrolledDate: string };

    if (this.isEditMode && this.studentId !== null) {
      this.studentService.updateStudent(this.studentId, data);
    } else {
      this.studentService.addStudent(data);
    }

    this.router.navigate(['/']);
  }

}
```

### How it works

| Part | Meaning |
|---|---|
| `FormGroup` | Container for the whole form |
| `FormControl('')` | A single field with an initial value |
| `Validators.required` | Field must not be empty |
| `Validators.email` | Must be a valid email format |
| `Validators.minLength(2)` | Must be at least 2 characters |
| `form.setValue(...)` | Pre-fills the form (edit mode) |
| `form.invalid` | True if any validation fails |
| `this.router.navigate(['/'])` | Redirect to list after save |

---

## 5. Form Template

### `src/app/student-form/student-form.component.html`

```html
<div class="form-container">
  <h2>{{ isEditMode ? 'Edit Student' : 'Add Student' }}</h2>

  <form [formGroup]="form" (ngSubmit)="onSubmit()">

    <div class="form-group">
      <label>Name</label>
      <input type="text" formControlName="name" placeholder="Full name" />
      @if (form.controls.name.invalid && form.controls.name.touched) {
        <span class="error">
          @if (form.controls.name.errors?.['required']) { Name is required. }
          @if (form.controls.name.errors?.['minlength']) { Name must be at least 2 characters. }
        </span>
      }
    </div>

    <div class="form-group">
      <label>Email</label>
      <input type="email" formControlName="email" placeholder="Email address" />
      @if (form.controls.email.invalid && form.controls.email.touched) {
        <span class="error">
          @if (form.controls.email.errors?.['required']) { Email is required. }
          @if (form.controls.email.errors?.['email']) { Enter a valid email. }
        </span>
      }
    </div>

    <div class="form-group">
      <label>Enrolled Date</label>
      <input type="date" formControlName="enrolledDate" />
      @if (form.controls.enrolledDate.invalid && form.controls.enrolledDate.touched) {
        <span class="error">Date is required.</span>
      }
    </div>

    <div class="form-actions">
      <button type="submit" [disabled]="form.invalid">
        {{ isEditMode ? 'Update' : 'Add' }}
      </button>
      <a routerLink="/">Cancel</a>
    </div>

  </form>
</div>
```

> Import `RouterLink` in the component's `imports` array for the Cancel link.

### Key template bindings

| Syntax | Meaning |
|---|---|
| `[formGroup]="form"` | Connects the form element to the `FormGroup` |
| `formControlName="name"` | Connects an input to a `FormControl` |
| `(ngSubmit)="onSubmit()"` | Calls `onSubmit()` when form is submitted |
| `.touched` | True after user has focused and left the field |
| `.errors?.['required']` | Checks for a specific validation error |

---

## 6. Form Styles

### `src/app/student-form/student-form.component.css`

```css
.form-container {
  padding: 30px;
  max-width: 480px;
}

.form-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
}

.form-group label {
  font-weight: 600;
  margin-bottom: 4px;
}

.form-group input {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
}

.error {
  color: #dc3545;
  font-size: 12px;
  margin-top: 4px;
}

.form-actions {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-top: 20px;
}

button[type="submit"] {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  cursor: pointer;
}

button[disabled] {
  background-color: #aaa;
  cursor: not-allowed;
}

a {
  color: #666;
  text-decoration: none;
}
```

---

## 7. Add Navigation Links

### Add Student button in `student-list.component.html`

Add a link to the list header:

```html
<div class="list-header">
  <h2>{{ pageTitle }}</h2>
  <a class="btn-add" routerLink="/students/new">+ Add Student</a>
</div>
```

Import `RouterLink` in `StudentListComponent`.

### Edit button in `student-detail.component.html`

```html
<div class="detail-actions">
  <a [routerLink]="['/students', student!.id, 'edit']">Edit</a>
  <a routerLink="/">← Back to List</a>
</div>
```

---

## 8. Run the App

```bash
ng serve
```

Test:
- ✅ Click **+ Add Student** → form opens (blank)
- ✅ Submit empty form → validation errors appear
- ✅ Fill in and submit → new student appears in the list
- ✅ Click **View** on a student → detail page
- ✅ Click **Edit** → form opens pre-filled
- ✅ Change name → click Update → detail page shows updated name

---

## Phase 9 Summary

| Concept | What You Learned |
|---|---|
| `FormGroup` | Container that holds all form controls |
| `FormControl` | Represents one input field |
| `Validators` | Built-in validation rules |
| `[formGroup]` | Binds the HTML form to the FormGroup |
| `formControlName` | Binds an input to a FormControl |
| `.touched` | Whether user has interacted with the field |
| `.errors` | Object containing which validations failed |
| Edit mode | Same form, pre-filled via `form.setValue()` |

---

## Application State After Phase 9

```
✅ StudentFormComponent — one form for both Add and Edit
✅ Reactive form with 3 fields and validation
✅ Add route: /students/new
✅ Edit route: /students/:id/edit
✅ StudentService: addStudent(), updateStudent() added
✅ Full CRUD on the frontend (Create, Read, Update, Delete)
```

**Next → Phase 10: Angular Internals & Reactivity**
We will look at the component lifecycle (`ngOnInit`, `ngOnDestroy`),
Angular Signals, and how change detection works.
