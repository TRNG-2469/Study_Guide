# Phase 8 — Navigation: Practice Exercises

**Prerequisites:** Completed Phase 8 lesson on Angular Router  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Set Up the Student Router and Active Link Styling

**Objective:** Configure `app.routes.ts`, add `<router-outlet>`, and style active `routerLink` buttons in the navbar.

### Steps

**1. Define the routes:**

```typescript
// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { HomeComponent }          from './components/home/home.component';
import { StudentListComponent }   from './components/student-list/student-list.component';
import { StudentDetailComponent } from './components/student-detail/student-detail.component';
import { AboutComponent }         from './components/about/about.component';
import { NotFoundComponent }      from './components/not-found/not-found.component';

export const routes: Routes = [
  { path: '',          redirectTo: 'home', pathMatch: 'full' },
  { path: 'home',      component: HomeComponent },
  { path: 'students',  component: StudentListComponent },
  { path: 'students/:id', component: StudentDetailComponent },
  { path: 'about',     component: AboutComponent },
  { path: '**',        component: NotFoundComponent },
];
```

**2. Update `AppComponent` to host the outlet:**

```typescript
// app.component.ts
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar />
    <main class="main-content">
      <router-outlet />
    </main>
  `,
  styles: [`.main-content { max-width: 1100px; margin: 24px auto; padding: 0 16px; }`]
})
export class AppComponent {}
```

**3. Wire up `NavbarComponent` with `routerLink` and `routerLinkActive`:**

```typescript
// navbar.component.ts
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { StudentService } from '../../services/student.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <span class="brand">📚 SMS</span>
      <div class="links">
        <a routerLink="/home"     routerLinkActive="active">Home</a>
        <a routerLink="/students" routerLinkActive="active">Students</a>
        <a routerLink="/about"    routerLinkActive="active">About</a>
      </div>
      <span class="badge">{{ svc.getActiveCount() }} active</span>
    </nav>
  `,
  styles: [`
    .navbar { background: #1976d2; color: #fff; padding: 12px 24px; display: flex; align-items: center; gap: 20px; }
    .brand  { font-weight: 700; font-size: 1.1rem; margin-right: auto; }
    .links a { color: rgba(255,255,255,.8); text-decoration: none; padding: 6px 12px; border-radius: 6px; transition: background .2s; }
    .links a:hover, .links a.active { background: rgba(255,255,255,.2); color: #fff; }
    .badge  { background: #fff; color: #1976d2; padding: 4px 12px; border-radius: 20px; font-weight: bold; font-size: .85rem; }
  `]
})
export class NavbarComponent {
  svc = inject(StudentService);
}
```

### What to Verify
- Navigating to `/home`, `/students`, `/about` renders the correct component inside `<router-outlet>`.
- The active link is visually highlighted by the `.active` class.
- Typing an unknown URL renders `NotFoundComponent`.
- The root path `/` redirects to `/home`.

---

## Exercise 2: Dynamic Route — Student Detail Page

**Objective:** Read a route parameter using `ActivatedRoute` and display the matching student, or redirect if not found.

### Steps

**1. Build `StudentDetailComponent`:**

```typescript
// student-detail.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student.service';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './student-detail.component.html',
})
export class StudentDetailComponent implements OnInit {
  private route   = inject(ActivatedRoute);
  private router  = inject(Router);
  private svc     = inject(StudentService);

  student: Student | undefined;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.student = this.svc.getById(id);

    if (!this.student) {
      // Student not found — navigate to 404
      this.router.navigate(['/not-found']);
    }
  }

  goBack(): void {
    this.router.navigate(['/students']);
  }
}
```

```html
<!-- student-detail.component.html -->
@if (student) {
  <div class="detail-card">
    <button class="back-btn" (click)="goBack()">← Back to List</button>

    <h2>{{ student.name }}</h2>

    <table class="info-table">
      <tr><th>Course</th>  <td>{{ student.course }}</td></tr>
      <tr><th>Year</th>    <td>{{ student.year }}</td></tr>
      <tr><th>GPA</th>     <td>{{ student.gpa | number:'1.2-2' }}</td></tr>
      <tr><th>Status</th>  <td>{{ student.status | titlecase }}</td></tr>
    </table>

    <a [routerLink]="['/students', student.id, 'edit']" class="edit-link">
      ✏️ Edit this Student
    </a>
  </div>
} @else {
  <p>Loading…</p>
}
```

**2. Add navigation links from `StudentListComponent`:**

```html
<!-- Inside the @for loop -->
<a [routerLink]="['/students', student.id]" class="view-link">View →</a>
```

### What to Verify
- Clicking **View →** on a student row navigates to `/students/3` (or whichever id).
- The detail page shows that student's data.
- Manually typing `/students/999` (non-existent id) redirects to `/not-found`.
- The **Back to List** button returns to `/students`.

---

## Exercise 3: Route Guard — Block Access to Non-Existent Students

**Objective:** Write a `CanActivateFn` guard that checks whether the requested student id exists before activating the detail route.

### Steps

**1. Create the guard:**

```bash
ng g guard guards/student-exists
# Choose: CanActivate
```

**2. Implement as a functional guard:**

```typescript
// guards/student-exists.guard.ts
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { StudentService } from '../services/student.service';

export const studentExistsGuard: CanActivateFn = (route, state) => {
  const svc    = inject(StudentService);
  const router = inject(Router);

  const id = Number(route.paramMap.get('id'));
  const exists = !!svc.getById(id);

  if (exists) {
    return true;
  }

  // Redirect to not-found instead of showing a blank page
  return router.createUrlTree(['/not-found']);
};
```

**3. Attach the guard to the dynamic route:**

```typescript
// app.routes.ts
import { studentExistsGuard } from './guards/student-exists.guard';

{ path: 'students/:id', component: StudentDetailComponent, canActivate: [studentExistsGuard] },
```

### What to Verify
- `/students/1` loads the detail page for student with id 1.
- `/students/999` (no matching student) redirects immediately to `/not-found` — the `StudentDetailComponent` never mounts.
- Remove the guard temporarily and verify `/students/999` now shows a broken detail page — this proves the guard's value.

### Challenge
Extend the guard to also check that `id` is a valid positive integer. If the URL segment is not a number (e.g. `/students/abc`), redirect to `/not-found` with a query param `?reason=invalid-id`.

