# Phase 9 — Forms: Practice Exercises

**Prerequisites:** Completed Phase 9 lesson on Reactive Forms  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Build the Student Registration Form with Validation

**Objective:** Create a reactive form for adding a new student with field-level validation and inline error messages.

### Steps

**1. Generate the form component:**

```bash
ng g c components/student-form
```

**2. Implement the component:**

```typescript
// student-form.component.ts
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { StudentService } from '../../services/student.service';

// Custom validator: no special characters in name
function noSpecialCharsValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value ?? '';
  const hasSpecial = /[^a-zA-Z\s]/.test(value);
  return hasSpecial ? { specialChars: true } : null;
}

@Component({
  selector: 'app-student-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './student-form.component.html',
  styleUrl: './student-form.component.css'
})
export class StudentFormComponent {
  private fb     = inject(FormBuilder);
  private svc    = inject(StudentService);
  private router = inject(Router);

  courses = ['CSE', 'ECE', 'MBA', 'BCA', 'MCA'];

  form = this.fb.group({
    name:   ['', [Validators.required, Validators.minLength(3), noSpecialCharsValidator]],
    email:  ['', [Validators.required, Validators.email]],
    course: ['CSE', Validators.required],
    year:   [1, [Validators.required, Validators.min(1), Validators.max(4)]],
    gpa:    [null, [Validators.min(0), Validators.max(10)]],
  });

  // Convenience getters for clean template access
  get name()   { return this.form.get('name')!;   }
  get email()  { return this.form.get('email')!;  }
  get course() { return this.form.get('course')!; }
  get year()   { return this.form.get('year')!;   }
  get gpa()    { return this.form.get('gpa')!;    }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email, course, year, gpa } = this.form.value;
    this.svc.add({
      name:   name!,
      course: course!,
      year:   year!,
      status: 'active',
      gpa:    gpa ?? 0,
    });
    this.router.navigate(['/students']);
  }

  onReset(): void {
    this.form.reset({ course: 'CSE', year: 1 });
  }
}
```

**3. Build the template with inline error messages:**

```html
<!-- student-form.component.html -->
<div class="form-container">
  <h2>Register New Student</h2>

  <form [formGroup]="form" (ngSubmit)="onSubmit()">

    <!-- Name -->
    <div class="field" [class.invalid]="name.touched && name.invalid">
      <label>Full Name *</label>
      <input type="text" formControlName="name" placeholder="Enter full name" />
      @if (name.touched && name.errors) {
        @if (name.errors['required'])     { <span class="error">Name is required.</span> }
        @if (name.errors['minlength'])    { <span class="error">Name must be at least 3 characters.</span> }
        @if (name.errors['specialChars']) { <span class="error">Name must not contain special characters.</span> }
      }
    </div>

    <!-- Email -->
    <div class="field" [class.invalid]="email.touched && email.invalid">
      <label>Email *</label>
      <input type="email" formControlName="email" placeholder="student@college.edu" />
      @if (email.touched && email.errors) {
        @if (email.errors['required']) { <span class="error">Email is required.</span> }
        @if (email.errors['email'])    { <span class="error">Please enter a valid email address.</span> }
      }
    </div>

    <!-- Course -->
    <div class="field">
      <label>Course *</label>
      <select formControlName="course">
        @for (c of courses; track c) {
          <option [value]="c">{{ c }}</option>
        }
      </select>
    </div>

    <!-- Year -->
    <div class="field" [class.invalid]="year.touched && year.invalid">
      <label>Year *</label>
      <input type="number" formControlName="year" min="1" max="4" />
      @if (year.touched && year.errors) {
        <span class="error">Year must be between 1 and 4.</span>
      }
    </div>

    <!-- GPA (optional) -->
    <div class="field" [class.invalid]="gpa.touched && gpa.invalid">
      <label>GPA <small>(0–10, optional)</small></label>
      <input type="number" formControlName="gpa" min="0" max="10" step="0.01" />
      @if (gpa.touched && gpa.errors) {
        <span class="error">GPA must be between 0 and 10.</span>
      }
    </div>

    <!-- Actions -->
    <div class="actions">
      <button type="submit" [disabled]="form.invalid && form.touched">Register</button>
      <button type="button" (click)="onReset()">Reset</button>
    </div>

  </form>
</div>
```

**4. Add styles:**

```css
/* student-form.component.css */
.form-container { max-width: 540px; margin: 32px auto; background: #fff; padding: 28px; border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,.08); }
.field { display: flex; flex-direction: column; gap: 4px; margin-bottom: 18px; }
label { font-weight: 600; font-size: .88rem; color: #444; }
input, select { padding: 9px 12px; border: 1px solid #ccc; border-radius: 6px; font-size: 1rem; transition: border-color .2s; }
input:focus, select:focus { outline: none; border-color: #1976d2; }
.field.invalid input, .field.invalid select { border-color: #e53935; }
.error { font-size: .8rem; color: #e53935; }
.actions { display: flex; gap: 12px; margin-top: 8px; }
button[type="submit"] { flex: 1; background: #1976d2; color: #fff; padding: 10px; border: none; border-radius: 6px; font-size: 1rem; cursor: pointer; }
button[type="submit"]:disabled { opacity: .5; cursor: not-allowed; }
button[type="button"] { padding: 10px 20px; border: 1px solid #ccc; border-radius: 6px; background: #fff; cursor: pointer; }
```

### What to Verify
- Clicking **Register** without filling in any field triggers `markAllAsTouched()` and shows all error messages.
- Typing a name with numbers (e.g. "Priya123") triggers the custom `specialChars` error.
- A valid form submission calls `svc.add()` and navigates to `/students`.
- **Reset** clears the form and restores `course = 'CSE'` and `year = 1`.

---

## Exercise 2: Edit Mode — Pre-populate the Form with `patchValue`

**Objective:** Reuse `StudentFormComponent` for editing an existing student by detecting whether an `id` param is in the route.

### Steps

**1. Update the component to handle edit mode:**

```typescript
import { ActivatedRoute } from '@angular/router';

isEditMode = false;
private editId: number | null = null;

constructor() {}

ngOnInit(): void {
  const id = Number(this.route.snapshot.paramMap.get('id'));
  if (id) {
    const student = this.svc.getById(id);
    if (student) {
      this.isEditMode = true;
      this.editId = id;
      this.form.patchValue({
        name:   student.name,
        email:  '',           // email not stored in model — leave blank
        course: student.course,
        year:   student.year,
        gpa:    student.gpa,
      });
    }
  }
}

onSubmit(): void {
  if (this.form.invalid) { this.form.markAllAsTouched(); return; }
  const { name, course, year, gpa } = this.form.value;
  if (this.isEditMode && this.editId !== null) {
    this.svc.update(this.editId, { name: name!, course: course!, year: year!, gpa: gpa ?? 0 });
  } else {
    this.svc.add({ name: name!, course: course!, year: year!, status: 'active', gpa: gpa ?? 0 });
  }
  this.router.navigate(['/students']);
}
```

**2. Update the template heading and button label:**

```html
<h2>{{ isEditMode ? 'Edit Student' : 'Register New Student' }}</h2>
<!-- ... -->
<button type="submit">{{ isEditMode ? 'Save Changes' : 'Register' }}</button>
```

**3. Add routes:**

```typescript
// app.routes.ts
{ path: 'students/new',        component: StudentFormComponent },
{ path: 'students/:id/edit',   component: StudentFormComponent },
// NOTE: 'students/new' MUST come before 'students/:id' to avoid 'new' being treated as an id
```

### What to Verify
- Navigating to `/students/new` shows an empty form with heading "Register New Student".
- Navigating to `/students/1/edit` shows the form pre-filled with student 1's data.
- Saving the edit calls `svc.update()` and the list reflects the change.

---

## Exercise 3: Cross-Field Validator — GPA Requires Year

**Objective:** Write a form-level (cross-field) validator that requires GPA to be filled if Year 3 or 4 is selected.

### Steps

**1. Create the cross-field validator:**

```typescript
function gpaRequiredForSeniors(group: AbstractControl): ValidationErrors | null {
  const year = group.get('year')?.value;
  const gpa  = group.get('gpa')?.value;
  if ((year === 3 || year === 4) && (gpa === null || gpa === '')) {
    return { gpaRequired: true };
  }
  return null;
}
```

**2. Attach it to the form group:**

```typescript
form = this.fb.group({
  // ... same fields as before
}, { validators: gpaRequiredForSeniors });
```

**3. Display the group-level error:**

```html
<!-- Below all fields, before the action buttons -->
@if (form.errors?.['gpaRequired'] && form.touched) {
  <p class="error-banner">
    ⚠️ GPA is required for Year 3 and Year 4 students.
  </p>
}
```

### What to Verify
- Selecting Year 3 or 4 and leaving GPA empty shows the cross-field error on submit.
- Filling in GPA removes the error immediately.
- Year 1 or 2 students can submit without a GPA.

