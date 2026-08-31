# Phase 9 — Forms

## What You Will Learn

Forms are the primary way users create and update data in a web application. Angular provides two approaches to forms:

| Approach | Where the form is defined | Best for |
|---|---|---|
| **Template-Driven Forms** | HTML template | Simple forms, quick prototypes |
| **Reactive Forms** | TypeScript class | Complex forms, validation, testing |

This course uses **Reactive Forms** — the approach preferred in professional Angular applications because the form structure and validation logic live in TypeScript, where they are testable, explicit, and type-safe.

By the end of this phase the application will have:
- An **Add Student** page at `/students/new`
- An **Edit Student** page at `/students/:id/edit`
- Full validation: required fields, email format, year range, custom validators
- Error messages that appear only after the user has interacted with a field

---

## 1. Template-Driven vs. Reactive Forms

### Template-Driven Forms (brief overview)

```html
<!-- Template-driven — form structure defined in HTML -->
<form #myForm="ngForm" (ngSubmit)="onSubmit(myForm)">
  <input name="email" ngModel required email>
</form>
```

Angular infers the form model from the template directives. Validation is declared in HTML with attributes. Easy to set up, hard to test, hard to control dynamically.

### Reactive Forms (what you will use)

```typescript
// Reactive — form structure defined in TypeScript
this.form = new FormGroup({
  email: new FormControl('', [Validators.required, Validators.email])
});
```

The form structure is a TypeScript object. Validation rules are code. The template binds to this model — it does not define it. This approach scales well, is fully testable, and gives you fine-grained control.

---

## 2. The Building Blocks of Reactive Forms

### `FormControl` — One Field

A `FormControl` tracks the value and validation state of a single input:

```typescript
const nameControl = new FormControl('', [Validators.required, Validators.minLength(2)]);

nameControl.value;    // Current value
nameControl.valid;    // true if all validators pass
nameControl.invalid;  // true if any validator fails
nameControl.errors;   // Object of error keys: { required: true } or { minlength: { ... } }
nameControl.touched;  // true after the user has focused and left the field
nameControl.dirty;    // true after the user has changed the value
```

### `FormGroup` — A Group of Fields

A `FormGroup` is a collection of `FormControl` instances representing one form:

```typescript
const studentForm = new FormGroup({
  name:   new FormControl('', Validators.required),
  email:  new FormControl('', [Validators.required, Validators.email]),
  course: new FormControl('', Validators.required),
  year:   new FormControl(1,  [Validators.required, Validators.min(1), Validators.max(4)]),
});

studentForm.valid;   // true only if ALL controls are valid
studentForm.value;   // { name: '...', email: '...', course: '...', year: 1 }
```

### `FormBuilder` — Concise Syntax

`FormBuilder` is a service that provides shorthand for creating `FormGroup` and `FormControl` instances:

```typescript
// Without FormBuilder
this.form = new FormGroup({
  name: new FormControl('', Validators.required)
});

// With FormBuilder — identical result, less boilerplate
this.form = this.fb.group({
  name: ['', Validators.required]
  //     ^    ^
  //     |    validators (single or array)
  //     initial value
});
```

The array syntax is `[initialValue, validators, asyncValidators]`.

---

## 3. Built-in Validators

Angular provides a set of ready-made validators in `Validators`:

| Validator | What It Checks |
|---|---|
| `Validators.required` | Field is not empty |
| `Validators.minLength(n)` | Value has at least n characters |
| `Validators.maxLength(n)` | Value has at most n characters |
| `Validators.min(n)` | Numeric value ≥ n |
| `Validators.max(n)` | Numeric value ≤ n |
| `Validators.email` | Valid email format |
| `Validators.pattern(regex)` | Value matches a regular expression |

---

## 4. Custom Validators

A validator is a function that takes an `AbstractControl` and returns either `null` (valid) or an error object (invalid):

```typescript
import { AbstractControl, ValidationErrors } from '@angular/forms';

// No spaces allowed in a field
export function noSpacesValidator(control: AbstractControl): ValidationErrors | null {
  if (control.value && control.value.includes(' ')) {
    return { noSpaces: true };   // Error key used in template
  }
  return null;   // Valid
}
```

Use it like any other validator:

```typescript
name: ['', [Validators.required, noSpacesValidator]]
```

---

## 5. Adding the Form Routes

Add two new routes to `app.routes.ts`:

```typescript
import { StudentFormComponent } from './pages/student-form/student-form.component';

export const routes: Routes = [
  { path: '',               redirectTo: 'home', pathMatch: 'full' },
  { path: 'home',           component: HomeComponent },
  { path: 'students',       component: StudentListComponent },
  { path: 'students/new',   component: StudentFormComponent },          // Add
  { path: 'students/:id',   component: StudentDetailComponent, canActivate: [studentExistsGuard] },
  { path: 'students/:id/edit', component: StudentFormComponent },       // Edit
  { path: 'about',          component: AboutComponent },
  { path: '**',             component: NotFoundComponent },
];
```

> **Important:** `students/new` must come **before** `students/:id` in the routes array. Otherwise Angular would match `/students/new` as `:id = 'new'` and try to look up student with ID `NaN`.

Generate the form component:

```bash
ng g c pages/student-form
```

---

## 6. Building `StudentFormComponent`

### `src/app/pages/student-form/student-form.component.ts`

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { StudentService } from '../../services/student.service';
import { Student } from '../../models/student.model';

// ─── Custom Validator ───────────────────────────────────────────────────────
function noSpecialCharsValidator(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value ?? '';
  const hasSpecial = /[^a-zA-Z\s\-']/.test(value);
  return hasSpecial ? { noSpecialChars: true } : null;
}

@Component({
  selector: 'app-student-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './student-form.component.html',
  styleUrl: './student-form.component.css'
})
export class StudentFormComponent implements OnInit {
  private fb      = inject(FormBuilder);
  private route   = inject(ActivatedRoute);
  private router  = inject(Router);
  private studentService = inject(StudentService);

  form!: FormGroup;
  isEditMode = false;
  editStudentId: number | null = null;
  pageTitle = 'Add Student';
  submitLabel = 'Add Student';

  readonly courses = [
    'Computer Science',
    'Mathematics',
    'Physics',
    'Engineering',
    'Biology',
  ];

  readonly statuses: Student['status'][] = ['active', 'inactive', 'graduated'];

  ngOnInit(): void {
    this.buildForm();
    this.detectEditMode();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      name:   ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60), noSpecialCharsValidator]],
      email:  ['', [Validators.required, Validators.email]],
      course: ['', Validators.required],
      year:   [1,  [Validators.required, Validators.min(1), Validators.max(4)]],
      status: ['active', Validators.required],
    });
  }

  private detectEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.editStudentId = Number(id);
      this.pageTitle = 'Edit Student';
      this.submitLabel = 'Save Changes';

      const student = this.studentService.getById(this.editStudentId);
      if (student) {
        // Populate the form with existing values
        this.form.patchValue({
          name:   student.name,
          email:  student.email,
          course: student.course,
          year:   student.year,
          status: student.status,
        });
      }
    }
  }

  // ─── Convenience getters — used in the template ──────────────────────────
  get name()   { return this.form.get('name')!; }
  get email()  { return this.form.get('email')!; }
  get course() { return this.form.get('course')!; }
  get year()   { return this.form.get('year')!; }
  get status() { return this.form.get('status')!; }

  // ─── Submission ───────────────────────────────────────────────────────────
  onSubmit(): void {
    if (this.form.invalid) {
      // Mark every control as touched to show all errors
      this.form.markAllAsTouched();
      return;
    }

    if (this.isEditMode && this.editStudentId !== null) {
      const existing = this.studentService.getById(this.editStudentId)!;
      const updated: Student = {
        ...existing,
        ...this.form.value,
      };
      this.studentService.update(updated);
      this.router.navigate(['/students', this.editStudentId]);
    } else {
      const added = this.studentService.add({
        ...this.form.value,
        enrolmentDate: new Date(),
      });
      this.router.navigate(['/students', added.id]);
    }
  }

  onCancel(): void {
    this.router.navigate(['/students']);
  }
}
```

**Key points:**

- `buildForm()` — separates form construction from route detection; keeps `ngOnInit` readable
- `detectEditMode()` — checks for `:id` in the URL; if present it is Edit mode, and `patchValue()` pre-fills the form
- `patchValue()` — sets specific fields without touching others (vs. `setValue()` which sets all fields and throws if any is missing)
- Convenience getters (`get name()`, etc.) — avoid calling `this.form.get('name')!` repeatedly in the template; the `!` asserts the control exists
- `form.markAllAsTouched()` — ensures all error messages are shown when the user clicks Submit without filling the form

---

## 7. `StudentFormComponent` Template

### `src/app/pages/student-form/student-form.component.html`

```html
<div class="form-page">

  <div class="form-header">
    <a routerLink="/students" class="btn-back">← Students</a>
    <h1>{{ pageTitle }}</h1>
  </div>

  <!-- [formGroup] binds the template to the TypeScript FormGroup -->
  <form class="student-form" [formGroup]="form" (ngSubmit)="onSubmit()">

    <!-- Name field -->
    <div class="form-group" [class.error]="name.invalid && name.touched">
      <label for="name">Full Name <span class="required">*</span></label>
      <input
        id="name"
        type="text"
        formControlName="name"
        placeholder="e.g. Alice Johnson">

      <!-- Error messages — shown only after the field is touched -->
      @if (name.touched && name.errors) {
        <div class="error-messages">
          @if (name.errors['required'])      { <span>Name is required.</span> }
          @if (name.errors['minlength'])     { <span>Name must be at least 2 characters.</span> }
          @if (name.errors['maxlength'])     { <span>Name must be 60 characters or fewer.</span> }
          @if (name.errors['noSpecialChars']){ <span>Name may only contain letters, spaces, hyphens, and apostrophes.</span> }
        </div>
      }
    </div>

    <!-- Email field -->
    <div class="form-group" [class.error]="email.invalid && email.touched">
      <label for="email">Email Address <span class="required">*</span></label>
      <input
        id="email"
        type="email"
        formControlName="email"
        placeholder="e.g. alice@uni.edu">

      @if (email.touched && email.errors) {
        <div class="error-messages">
          @if (email.errors['required']) { <span>Email is required.</span> }
          @if (email.errors['email'])    { <span>Please enter a valid email address.</span> }
        </div>
      }
    </div>

    <!-- Course field -->
    <div class="form-group" [class.error]="course.invalid && course.touched">
      <label for="course">Course <span class="required">*</span></label>
      <select id="course" formControlName="course">
        <option value="">— Select a course —</option>
        @for (c of courses; track c) {
          <option [value]="c">{{ c }}</option>
        }
      </select>

      @if (course.touched && course.errors) {
        <div class="error-messages">
          @if (course.errors['required']) { <span>Please select a course.</span> }
        </div>
      }
    </div>

    <!-- Year field -->
    <div class="form-group" [class.error]="year.invalid && year.touched">
      <label for="year">Year of Study <span class="required">*</span></label>
      <input
        id="year"
        type="number"
        formControlName="year"
        min="1"
        max="4">

      @if (year.touched && year.errors) {
        <div class="error-messages">
          @if (year.errors['required']) { <span>Year is required.</span> }
          @if (year.errors['min'])      { <span>Year must be at least 1.</span> }
          @if (year.errors['max'])      { <span>Year cannot exceed 4.</span> }
        </div>
      }
    </div>

    <!-- Status field -->
    <div class="form-group">
      <label>Status <span class="required">*</span></label>
      <div class="radio-group">
        @for (s of statuses; track s) {
          <label class="radio-label">
            <input type="radio" formControlName="status" [value]="s">
            {{ s | titlecase }}
          </label>
        }
      </div>
    </div>

    <!-- Form actions -->
    <div class="form-actions">
      <button type="button" class="btn btn-secondary" (click)="onCancel()">
        Cancel
      </button>
      <button
        type="submit"
        class="btn btn-primary"
        [disabled]="form.invalid && form.touched">
        {{ submitLabel }}
      </button>
    </div>

    <!-- Form-level status indicator (useful during development) -->
    @if (form.touched) {
      <p class="form-status" [class.valid]="form.valid" [class.invalid]="form.invalid">
        Form is {{ form.valid ? '✔ valid' : '✗ invalid' }}
      </p>
    }

  </form>

</div>
```

**Template directives explained:**

| Directive | Purpose |
|---|---|
| `[formGroup]="form"` | Binds the `<form>` element to the TypeScript `FormGroup` |
| `formControlName="name"` | Binds an input to a named `FormControl` inside the group |
| `(ngSubmit)="onSubmit()"` | Calls `onSubmit()` when the form is submitted |
| `[value]="c"` on `<option>` | Sets the option's value to a TypeScript expression (not a string) |

---

## 8. `StudentFormComponent` CSS

### `src/app/pages/student-form/student-form.component.css`

```css
.form-page {
  max-width: 600px;
  margin: 30px auto;
  padding: 0 20px;
}

.form-header {
  margin-bottom: 24px;
}

.btn-back {
  color: #1a73e8;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.btn-back:hover { text-decoration: underline; }

.form-header h1 {
  margin: 10px 0 0;
  color: #1a1a2e;
  font-size: 24px;
}

.student-form {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  padding: 32px;
}

.form-group {
  margin-bottom: 22px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #444;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.required { color: #d93025; }

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
  background: white;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26,115,232,0.1);
}

.form-group.error input,
.form-group.error select {
  border-color: #d93025;
}

.error-messages {
  margin-top: 6px;
}

.error-messages span {
  display: block;
  font-size: 12px;
  color: #d93025;
  margin-top: 3px;
}

.radio-group {
  display: flex;
  gap: 20px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: normal;
  text-transform: none;
  letter-spacing: 0;
  cursor: pointer;
  color: #333;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-primary {
  background: #1a73e8;
  color: white;
}

.btn-primary:hover:not(:disabled) { background: #1557b0; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }

.btn-secondary {
  background: #f1f3f4;
  color: #444;
}

.btn-secondary:hover { background: #e0e0e0; }

.form-status {
  margin-top: 16px;
  font-size: 13px;
  text-align: right;
}

.form-status.valid   { color: #1e8e3e; }
.form-status.invalid { color: #d93025; }
```

---

## 9. Wiring the Add/Edit Buttons

### In `StudentListComponent` — Add button

Update the Add Student button in `student-list.component.html`:

```html
<a routerLink="/students/new" class="btn-add">+ Add Student</a>
```

Change it from a `<button>` to an `<a>` tag with `routerLink`. Import `RouterLink` in `StudentListComponent` if not already done.

### In `StudentDetailComponent` — Edit button

```html
<a [routerLink]="['/students', student!.id, 'edit']" class="btn btn-primary">
  Edit Student
</a>
```

---

## 10. Validation UX — When to Show Errors

A good form shows errors only when the user has had a chance to fill the field — not immediately on page load. The pattern used in this lesson:

```html
@if (name.touched && name.errors) { ... }
```

- `touched` becomes `true` after the user has focused **and then left** a field (blur event)
- Errors are shown only for fields the user has already interacted with
- On submit, `markAllAsTouched()` forces all errors to appear at once so the user can see everything that needs fixing

This gives a professional UX: clean on first load, progressively reveals issues as you fill the form, and validates everything on submit.

---

## 11. The Application So Far

```
/students/new      → StudentFormComponent (Add mode)
  - Empty form
  - On submit: StudentService.add() → navigate to new student's detail page

/students/:id/edit → StudentFormComponent (Edit mode)
  - Form pre-filled via patchValue()
  - On submit: StudentService.update() → navigate back to detail page
```

**Current state:** Full Create and Update functionality. The student list, detail page, and form all share data through `StudentService`. Validation runs on blur and on submit. Error messages are specific and appear only after user interaction.

---

## Phase 9 Summary

| Concept | What You Learned |
|---|---|
| Reactive Forms | Form model defined in TypeScript — explicit, testable, scalable |
| `FormGroup` | A group of `FormControl` instances representing one form |
| `FormControl` | Tracks value, validity, touched, dirty state for one field |
| `FormBuilder.group()` | Concise syntax: `[initialValue, validators]` |
| Built-in `Validators` | `required`, `email`, `minLength`, `min`, `max`, `pattern` |
| Custom validator | A function `(control) => errors | null` |
| `[formGroup]` | Binds a `<form>` element to a TypeScript `FormGroup` |
| `formControlName` | Binds an `<input>` to a named control in the group |
| `patchValue()` | Pre-fills specific fields — used for Edit mode |
| `markAllAsTouched()` | Forces all error messages to appear on submit |
| `touched` / `dirty` | Controls when validation feedback appears |
| `form.invalid` | True when any control fails validation |

---

## What's Next

In **Phase 10 — Angular Internals & Reactivity**, you will learn the **Component Lifecycle** (how Angular creates, updates, and destroys components), **Signals** (Angular's modern reactive state primitive that replaces plain properties for fine-grained updates), and **Change Detection** (how Angular decides when to update the DOM). You will use signals to make the student count in the navbar update without the manual getter polling pattern.

