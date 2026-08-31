# Phase 8 — Navigation

## What You Will Learn

The Student Management System currently has one view — the student list. A real application needs multiple pages: a dashboard, the student list, a student detail page, and an about page. Users should be able to bookmark URLs, use the browser back button, and share links — all the behaviours of a real web application.

Angular's **Router** makes this possible inside a Single-Page Application. No full page reloads happen — Angular intercepts navigation and swaps only the content area.

By the end of this phase the application will have:

| URL | Page |
|---|---|
| `/` or `/home` | Dashboard (overview stats) |
| `/students` | Student list |
| `/students/:id` | Student detail (URL-based) |
| `/about` | About page |
| `**` (any unknown URL) | 404 Not Found page |

Route guards will protect the detail page from being accessed with an invalid ID.

---

## 1. How Angular Routing Works

When a user navigates to `/students/3`:

```
Browser URL changes to /students/3
  → Angular Router intercepts (no HTTP request to server)
  → Router matches the URL against the route configuration
  → Router finds: { path: 'students/:id', component: StudentDetailComponent }
  → Router renders StudentDetailComponent inside <router-outlet>
  → StudentDetailComponent reads :id = '3' from the URL
  → Component fetches student with id=3 from StudentService
```

The `<router-outlet>` tag is a placeholder in your template. The router replaces its content with whichever component matches the current URL.

---

## 2. Setting Up Routes

Routes are configured in `src/app/app.routes.ts` — already created by the CLI in Phase 1:

### Step 1 — Generate the Page Components

```bash
ng g c pages/home
ng g c pages/student-detail
ng g c pages/about
ng g c pages/not-found
```

This creates four components under `src/app/pages/`.

### Step 2 — Define the Routes

### `src/app/app.routes.ts`

```typescript
import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { StudentListComponent } from './student-list/student-list.component';
import { StudentDetailComponent } from './pages/student-detail/student-detail.component';
import { AboutComponent } from './pages/about/about.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { studentExistsGuard } from './guards/student-exists.guard';

export const routes: Routes = [
  // Redirect root path to /home
  { path: '',        redirectTo: 'home', pathMatch: 'full' },

  // Static pages
  { path: 'home',    component: HomeComponent },
  { path: 'students', component: StudentListComponent },
  { path: 'about',   component: AboutComponent },

  // Dynamic route — :id is a route parameter
  {
    path: 'students/:id',
    component: StudentDetailComponent,
    canActivate: [studentExistsGuard]    // Route guard (Step 5)
  },

  // Wildcard — must be last
  { path: '**',      component: NotFoundComponent },
];
```

**Route order matters.** Angular matches routes top to bottom and stops at the first match. The wildcard `**` must always be last, or it will catch every URL.

### Step 3 — Provide the Router

`app.config.ts` was generated with `provideRouter(routes)` already — nothing to change here:

```typescript
// src/app/app.config.ts  (already correct from Phase 1)
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes)
  ]
};
```

---

## 3. `<router-outlet>` — The Content Area

The router renders the matched component wherever you place `<router-outlet>` in a template. Update `AppComponent` to use it:

### `src/app/app.component.ts`

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {}
```

### `src/app/app.component.html`

```html
<app-navbar></app-navbar>

<main class="app-main">
  <router-outlet></router-outlet>
</main>
```

Now Angular renders whichever page component matches the URL inside `<main>`. The navbar stays fixed at the top across all pages.

---

## 4. `routerLink` — Navigating Between Pages

Replace the placeholder `href="#"` links in the navbar with Angular's `routerLink` directive. Unlike a plain `href`, `routerLink` tells the Angular Router to navigate without a page reload.

### Updated `src/app/navbar/navbar.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { StudentService } from '../services/student.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  private studentService = inject(StudentService);
  appName = 'Student Management System';

  get totalStudents(): number {
    return this.studentService.getTotalCount();
  }
}
```

### Updated `src/app/navbar/navbar.component.html`

```html
<nav class="navbar">
  <div class="navbar-brand">
    <a routerLink="/home" class="brand-link">
      <span class="brand-icon">🎓</span>
      <span class="brand-name">{{ appName }}</span>
    </a>
  </div>
  <div class="navbar-center">
    <span class="student-count">{{ totalStudents }} Students</span>
  </div>
  <div class="navbar-links">
    <a routerLink="/home"     routerLinkActive="active">Home</a>
    <a routerLink="/students" routerLinkActive="active">Students</a>
    <a routerLink="/about"    routerLinkActive="active">About</a>
  </div>
</nav>
```

Add to `navbar.component.css`:

```css
.brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  color: white;
  text-decoration: none;
}

.navbar-links a.active {
  text-decoration: underline;
  opacity: 1;
  font-weight: 600;
}
```

**`routerLinkActive="active"`** adds the CSS class `active` to the link whose route matches the current URL. This gives visual feedback about which page is open — without any JavaScript logic in the component.

---

## 5. Route Parameters — The Student Detail Page

The URL `/students/3` contains a **route parameter** — the student ID. The `StudentDetailComponent` reads this parameter and fetches the matching student.

### `src/app/pages/student-detail/student-detail.component.ts`

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TitleCasePipe, DatePipe } from '@angular/common';
import { StudentService } from '../../services/student.service';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [TitleCasePipe, DatePipe, RouterLink],
  templateUrl: './student-detail.component.html',
  styleUrl: './student-detail.component.css'
})
export class StudentDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);   // Gives access to current route info
  private router = inject(Router);          // For programmatic navigation
  private studentService = inject(StudentService);

  student: Student | undefined;

  ngOnInit(): void {
    // Read the :id parameter from the URL — always a string
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);
    this.student = this.studentService.getById(id);
  }

  goBack(): void {
    this.router.navigate(['/students']);
  }

  onDelete(): void {
    if (this.student && confirm(`Delete ${this.student.name}?`)) {
      this.studentService.delete(this.student.id);
      this.router.navigate(['/students']);  // Navigate away after deletion
    }
  }
}
```

#### `ActivatedRoute` — Reading Route Information

`ActivatedRoute` is a service Angular provides that gives information about the currently active route:

| Property/Method | What It Gives You |
|---|---|
| `snapshot.paramMap.get('id')` | The value of `:id` in the URL as a string |
| `snapshot.queryParamMap.get('tab')` | A query parameter like `?tab=profile` |
| `snapshot.url` | The URL segments as an array |
| `params` | An Observable of params — use when the component may reuse without destroy |

`snapshot` gives a point-in-time read. For most navigation patterns it is sufficient. Use the Observable `params` only when the URL parameter can change while the same component stays mounted (e.g. next/previous navigation within a detail page).

#### `Router` — Programmatic Navigation

```typescript
this.router.navigate(['/students']);            // Go to /students
this.router.navigate(['/students', student.id]); // Go to /students/3
this.router.navigate(['..'], { relativeTo: this.route }); // Go up one level
```

### `src/app/pages/student-detail/student-detail.component.html`

```html
<div class="detail-page">

  <div class="page-header">
    <button class="btn-back" (click)="goBack()">← Back to Students</button>
  </div>

  @if (student) {
    <div class="detail-card">
      <div class="detail-hero">
        <div class="hero-avatar">{{ student.name[0].toUpperCase() }}</div>
        <div class="hero-info">
          <h1>{{ student.name | titlecase }}</h1>
          <p class="student-id">STU-{{ student.id.toString().padStart(3, '0') }}</p>
          <span class="status-pill status-{{ student.status }}">
            {{ student.status | titlecase }}
          </span>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-item">
          <label>Course</label>
          <span>{{ student.course }}</span>
        </div>
        <div class="detail-item">
          <label>Year of Study</label>
          <span>Year {{ student.year }}</span>
        </div>
        <div class="detail-item">
          <label>Email Address</label>
          <span>{{ student.email }}</span>
        </div>
        <div class="detail-item">
          <label>Enrolment Date</label>
          <span>{{ student.enrolmentDate | date:'dd MMMM yyyy' }}</span>
        </div>
      </div>

      <div class="detail-actions">
        <button class="btn btn-primary">Edit Student</button>
        <button class="btn btn-danger" (click)="onDelete()">Delete Student</button>
      </div>
    </div>
  } @else {
    <div class="not-found">
      <p>Student not found.</p>
      <a routerLink="/students">Return to Students</a>
    </div>
  }

</div>
```

### `src/app/pages/student-detail/student-detail.component.css`

```css
.detail-page {
  max-width: 700px;
  margin: 30px auto;
  padding: 0 20px;
}

.page-header {
  margin-bottom: 20px;
}

.btn-back {
  background: none;
  border: none;
  color: #1a73e8;
  font-size: 15px;
  cursor: pointer;
  padding: 0;
  font-weight: 500;
}

.btn-back:hover { text-decoration: underline; }

.detail-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  overflow: hidden;
}

.detail-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 30px;
  background: linear-gradient(135deg, #1a73e8, #0d47a1);
  color: white;
}

.hero-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: bold;
  flex-shrink: 0;
}

.hero-info h1 { margin: 0 0 6px; font-size: 26px; }

.student-id {
  margin: 0 0 10px;
  opacity: 0.8;
  font-size: 14px;
}

.status-pill {
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255,255,255,0.2);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: #f0f0f0;
  border-top: 1px solid #f0f0f0;
}

.detail-item {
  background: white;
  padding: 20px 24px;
}

.detail-item label {
  display: block;
  font-size: 11px;
  text-transform: uppercase;
  color: #999;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.detail-item span {
  font-size: 16px;
  color: #1a1a2e;
  font-weight: 500;
}

.detail-actions {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
}

.btn { padding: 10px 24px; border: none; border-radius: 6px; cursor: pointer; font-size: 15px; font-weight: 500; }
.btn-primary { background: #1a73e8; color: white; }
.btn-primary:hover { background: #1557b0; }
.btn-danger { background: #fce8e6; color: #d93025; }
.btn-danger:hover { background: #f5c6c2; }

.not-found { text-align: center; padding: 60px; color: #888; }
.not-found a { color: #1a73e8; }
```

---

## 6. Navigating to the Detail Page from the List

Update `StudentListComponent`'s detail panel to link to the detail page instead of showing inline.

In `student-list.component.html`, update the "View" logic in the detail panel actions:

```html
<!-- Import RouterLink in student-list.component.ts first -->
<div class="detail-actions">
  <a [routerLink]="['/students', selectedStudent!.id]" class="btn btn-primary">
    Full Detail
  </a>
  <button class="btn btn-danger" (click)="onDeleteStudent(selectedStudent!.id)">
    Delete
  </button>
</div>
```

Add `RouterLink` to `StudentListComponent` imports:

```typescript
import { RouterLink } from '@angular/router';

@Component({
  imports: [StudentCardComponent, FormsModule, TitleCasePipe, DatePipe, RouterLink],
  ...
})
```

`[routerLink]="['/students', selectedStudent!.id]"` is **array syntax** for building a route — Angular joins the segments: `/students/1`. This is the recommended approach for dynamic segments.

---

## 7. Route Guard — Protecting the Detail Page

A **route guard** prevents navigation to a route under certain conditions. `studentExistsGuard` should stop users from reaching `/students/999` when student 999 does not exist.

### Generate the Guard

```bash
ng generate guard guards/student-exists
```

Choose **CanActivate** when prompted.

### `src/app/guards/student-exists.guard.ts`

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StudentService } from '../services/student.service';

export const studentExistsGuard: CanActivateFn = (route) => {
  const studentService = inject(StudentService);
  const router = inject(Router);

  const id = Number(route.paramMap.get('id'));
  const exists = !!studentService.getById(id);

  if (exists) {
    return true;              // Allow navigation
  }

  // Redirect to not-found page
  return router.createUrlTree(['/not-found']);
};
```

A `CanActivateFn` is a function (not a class) — this is the modern Angular guard style. It returns:
- `true` — allow the navigation
- `false` — block the navigation
- A `UrlTree` — block and redirect to a different URL

`inject()` works inside guard functions too — Angular's DI is available anywhere inside an injection context.

---

## 8. Building the Remaining Pages

### `HomeComponent` — Dashboard

### `src/app/pages/home/home.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StudentService } from '../../services/student.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  private studentService = inject(StudentService);

  get totalStudents(): number  { return this.studentService.getTotalCount(); }
  get activeStudents(): number { return this.studentService.getActiveCount(); }
  get graduatedStudents(): number {
    return this.studentService.getAll().filter(s => s.status === 'graduated').length;
  }
}
```

### `src/app/pages/home/home.component.html`

```html
<div class="home-page">
  <div class="welcome-banner">
    <h1>Welcome to Student Management</h1>
    <p>Manage and track all your students in one place.</p>
    <a routerLink="/students" class="btn-cta">View All Students →</a>
  </div>

  <div class="stats-grid">
    <div class="stat-card">
      <div class="stat-number">{{ totalStudents }}</div>
      <div class="stat-label">Total Students</div>
    </div>
    <div class="stat-card stat-green">
      <div class="stat-number">{{ activeStudents }}</div>
      <div class="stat-label">Active</div>
    </div>
    <div class="stat-card stat-blue">
      <div class="stat-number">{{ graduatedStudents }}</div>
      <div class="stat-label">Graduated</div>
    </div>
    <div class="stat-card stat-red">
      <div class="stat-number">{{ totalStudents - activeStudents - graduatedStudents }}</div>
      <div class="stat-label">Inactive</div>
    </div>
  </div>
</div>
```

### `src/app/pages/home/home.component.css`

```css
.home-page { max-width: 900px; margin: 40px auto; padding: 0 20px; }

.welcome-banner {
  background: linear-gradient(135deg, #1a73e8, #0d47a1);
  color: white;
  padding: 40px;
  border-radius: 12px;
  margin-bottom: 30px;
  text-align: center;
}

.welcome-banner h1 { margin: 0 0 10px; font-size: 28px; }
.welcome-banner p  { margin: 0 0 20px; opacity: 0.85; font-size: 16px; }

.btn-cta {
  display: inline-block;
  background: white;
  color: #1a73e8;
  padding: 10px 28px;
  border-radius: 6px;
  text-decoration: none;
  font-weight: 600;
  font-size: 15px;
}

.btn-cta:hover { background: #f0f0f0; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border-top: 4px solid #1a73e8;
}

.stat-green { border-top-color: #1e8e3e; }
.stat-blue  { border-top-color: #1a73e8; }
.stat-red   { border-top-color: #d93025; }

.stat-number { font-size: 36px; font-weight: bold; color: #1a1a2e; }
.stat-label  { font-size: 13px; color: #888; margin-top: 4px; }
```

### `AboutComponent`

### `src/app/pages/about/about.component.html`

```html
<div class="about-page">
  <h1>About This Application</h1>
  <p>
    The Student Management System is a full-stack application built with
    <strong>Angular</strong> on the frontend and <strong>Spring Boot</strong>
    with <strong>PostgreSQL</strong> on the backend.
  </p>
  <h2>Technology Stack</h2>
  <ul>
    <li>Angular 17+ (Standalone Components, Modern Control Flow)</li>
    <li>TypeScript</li>
    <li>Spring Boot 3</li>
    <li>Spring Data JPA</li>
    <li>PostgreSQL</li>
  </ul>
</div>
```

### `src/app/pages/about/about.component.css`

```css
.about-page { max-width: 700px; margin: 40px auto; padding: 0 20px; }
.about-page h1 { color: #1a1a2e; }
.about-page h2 { color: #1a73e8; margin-top: 30px; }
.about-page li { margin-bottom: 8px; line-height: 1.6; }
```

### `NotFoundComponent`

### `src/app/pages/not-found/not-found.component.html`

```html
<div class="not-found-page">
  <div class="error-code">404</div>
  <h1>Page Not Found</h1>
  <p>The page you are looking for does not exist.</p>
  <a routerLink="/home" class="btn-home">Go Home</a>
</div>
```

### `src/app/pages/not-found/not-found.component.css`

```css
.not-found-page {
  text-align: center;
  padding: 80px 20px;
  color: #555;
}

.error-code {
  font-size: 96px;
  font-weight: bold;
  color: #e0e0e0;
  line-height: 1;
  margin-bottom: 10px;
}

.not-found-page h1 { font-size: 28px; color: #1a1a2e; margin: 0 0 10px; }
.not-found-page p  { font-size: 16px; margin: 0 0 24px; }

.btn-home {
  display: inline-block;
  background: #1a73e8;
  color: white;
  padding: 10px 28px;
  border-radius: 6px;
  text-decoration: none;
  font-weight: 500;
}
```

---

## 9. The Complete Route Map

```
/                → redirect to /home
/home            → HomeComponent      (dashboard with stats)
/students        → StudentListComponent  (card grid + detail panel)
/students/:id    → StudentDetailComponent  (full detail page)
                    ↳ guarded by studentExistsGuard
/about           → AboutComponent
/**              → NotFoundComponent  (404)
```

Open the application and test every route:
- Click **Students** in the navbar → student list loads
- Click **Full Detail** on a student card → navigates to `/students/1`, full detail page
- Click **← Back to Students** → programmatic navigation returns to the list
- Type `/students/999` in the browser bar → guard redirects to `/not-found`
- Click **Home** → dashboard with live stats from `StudentService`
- Browser back button works through the full history

---

## Phase 8 Summary

| Concept | What You Learned |
|---|---|
| `Routes` array | Declares URL-to-component mappings; order matters; `**` last |
| `redirectTo` | Redirects one path to another |
| `<router-outlet>` | Placeholder where the router renders the matched component |
| `RouterOutlet` | Must be imported to use `<router-outlet>` |
| `routerLink` | Directive for client-side navigation; no page reload |
| `[routerLink]="['/path', param]"` | Array syntax for dynamic URL segments |
| `routerLinkActive` | Adds a CSS class when the route is active |
| `ActivatedRoute` | Service to read route parameters, query params, and URL data |
| `snapshot.paramMap.get('id')` | Reads `:id` from the URL as a string |
| `Router.navigate()` | Programmatic navigation from TypeScript |
| `CanActivateFn` | Modern functional route guard — returns true/false/UrlTree |
| `router.createUrlTree()` | Redirects from inside a guard |

---

## What's Next

In **Phase 9 — Forms**, you will build the Add Student and Edit Student forms using Angular's **Reactive Forms** — a code-driven, type-safe form approach. You will define the form structure in TypeScript, validate inputs (required fields, email format, year range), display validation error messages, and wire the form's submit to `StudentService.add()` and `StudentService.update()`.

