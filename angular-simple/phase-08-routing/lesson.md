# Phase 8 — Navigation

## Where We Left Off

After Phase 7, the app is a single page. All components are stacked on one screen.

In this phase, we give the app **multiple pages** using Angular's **Router**:

| URL | Page |
|---|---|
| `/` | Student list |
| `/students/:id` | Student detail |

---

## 1. How Angular Routing Works

In a SPA, the browser **never loads a new HTML page**.  
Instead, Angular swaps components in and out of a placeholder called `<router-outlet>`.

The flow:
1. User clicks a link → URL changes
2. Angular reads the new URL
3. Finds a matching **route** → loads the mapped component into `<router-outlet>`

---

## 2. Define Routes

Angular CLI already created `src/app/app.routes.ts` when we used `--routing=true`.

### `src/app/app.routes.ts`

```typescript
import { Routes } from '@angular/router';
import { StudentListComponent } from './student-list/student-list.component';
import { StudentDetailComponent } from './student-detail/student-detail.component';
import { studentExistsGuard } from './student-exists.guard';

export const routes: Routes = [
  { path: '',          component: StudentListComponent },
  { path: 'students/:id', component: StudentDetailComponent, canActivate: [studentExistsGuard] },
];
```

| Part | Meaning |
|---|---|
| `path: ''` | Matches `/` — the home page |
| `path: 'students/:id'` | Matches `/students/1`, `/students/2`, etc. |
| `:id` | A **route parameter** — a dynamic part of the URL |
| `canActivate` | A **route guard** — checks before loading the component |

---

## 3. Update AppComponent

`AppComponent` no longer hosts `StudentListComponent` directly.  
It now holds the `<router-outlet>` and a simple navigation bar.

### `src/app/app.component.ts`

```typescript
import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { HeaderComponent } from './header/header.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, HeaderComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {}
```

### `src/app/app.component.html`

```html
<app-header></app-header>

<nav class="main-nav">
  <a routerLink="/">Students</a>
</nav>

<router-outlet></router-outlet>
```

- `routerLink="/"` — Angular's way to navigate without a full page reload (instead of `href`)
- `<router-outlet>` — Angular loads the matched component here

### `src/app/app.component.css`

```css
.main-nav {
  background: #f8f9fa;
  padding: 10px 30px;
  border-bottom: 1px solid #ddd;
}

.main-nav a {
  color: #007bff;
  text-decoration: none;
  font-weight: 500;
}
```

---

## 4. Create StudentDetailComponent

```bash
ng g c student-detail
```

### `src/app/student-detail/student-detail.component.ts`

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TitleCasePipe, DatePipe } from '@angular/common';
import { Student } from '../student.model';
import { StudentService } from '../student.service';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [TitleCasePipe, DatePipe, RouterLink],
  templateUrl: './student-detail.component.html',
  styleUrl: './student-detail.component.css'
})
export class StudentDetailComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private studentService = inject(StudentService);

  student: Student | undefined;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.student = this.studentService.getStudentById(id);
  }

}
```

### What's happening

| Code | Meaning |
|---|---|
| `inject(ActivatedRoute)` | Access the current route info |
| `this.route.snapshot.paramMap.get('id')` | Read the `:id` from the URL |
| `Number(...)` | Convert string `"1"` to number `1` |
| `ngOnInit()` | Lifecycle hook — runs once after component loads (covered fully in Phase 10) |

---

### `src/app/student-detail/student-detail.component.html`

```html
<div class="detail-container">
  @if (student) {
    <h2>{{ student.name | titlecase }}</h2>
    <table class="detail-table">
      <tr>
        <th>ID</th>
        <td>{{ student.id }}</td>
      </tr>
      <tr>
        <th>Name</th>
        <td>{{ student.name | titlecase }}</td>
      </tr>
      <tr>
        <th>Email</th>
        <td>{{ student.email }}</td>
      </tr>
      <tr>
        <th>Enrolled</th>
        <td>{{ student.enrolledDate | date: 'mediumDate' }}</td>
      </tr>
    </table>
    <a routerLink="/">← Back to List</a>
  }
</div>
```

### `src/app/student-detail/student-detail.component.css`

```css
.detail-container {
  padding: 30px;
  max-width: 500px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  margin: 20px 0;
}

.detail-table th, .detail-table td {
  border: 1px solid #ddd;
  padding: 10px;
  text-align: left;
}

.detail-table th {
  background: #f0f0f0;
  width: 120px;
}

a {
  color: #007bff;
  text-decoration: none;
}
```

---

## 5. Add `getStudentById` to StudentService

### `src/app/student.service.ts`

Add one method:

```typescript
getStudentById(id: number): Student | undefined {
  return this.students.find(s => s.id === id);
}
```

---

## 6. Route Guard — `studentExistsGuard`

A **route guard** runs before a route is activated. If it returns `false`, navigation is blocked.

We will create a guard that checks whether the student id in the URL actually exists. If not, redirect to the list.

```bash
ng generate guard student-exists
```

When prompted, choose **CanActivate**.

### `src/app/student-exists.guard.ts`

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StudentService } from './student.service';

export const studentExistsGuard: CanActivateFn = (route) => {
  const studentService = inject(StudentService);
  const router = inject(Router);

  const id = Number(route.paramMap.get('id'));
  const exists = !!studentService.getStudentById(id);

  if (!exists) {
    router.navigate(['/']);
    return false;
  }

  return true;
};
```

If `/students/99` is visited and student 99 doesn't exist → redirect to `/`.

---

## 7. Add "View" Navigation to StudentCardComponent

### `src/app/student-card/student-card.component.ts`

Import `RouterLink`:

```typescript
import { RouterLink } from '@angular/router';

@Component({
  ...
  imports: [TitleCasePipe, DatePipe, RouterLink],
  ...
})
```

### `src/app/student-card/student-card.component.html`

Add a View link:

```html
<div class="student-card">
  <div class="card-info">
    <span class="student-id">#{{ student.id }}</span>
    <strong>{{ student.name | titlecase }}</strong>
    <span>{{ student.email }}</span>
    <span class="date">Enrolled: {{ student.enrolledDate | date: 'mediumDate' }}</span>
  </div>
  <div class="card-actions">
    <a class="btn-view" [routerLink]="['/students', student.id]">View</a>
    <button class="btn-delete" (click)="onDelete()">Delete</button>
  </div>
</div>
```

`[routerLink]="['/students', student.id]"` — builds the URL dynamically, e.g. `/students/1`.

Also add the style for the link:

```css
.btn-view {
  background-color: #28a745;
  color: white;
  text-decoration: none;
  padding: 6px 14px;
  border-radius: 4px;
}
```

---

## 8. Run the App

```bash
ng serve
```

Test:
- ✅ `/` → shows student list with cards
- ✅ Click **View** → navigates to `/students/1` — shows detail page
- ✅ Click **← Back to List** → returns to `/`
- ✅ Visit `/students/99` in the browser → redirected back to `/` by the guard

---

## Phase 8 Summary

| Concept | What You Learned |
|---|---|
| `Routes` array | Maps URL paths to components |
| `<router-outlet>` | Where Angular renders the matched component |
| `routerLink` | Navigate without page reload |
| `[routerLink]` | Dynamic URL with variable |
| Route parameter `:id` | Variable part of the URL |
| `ActivatedRoute` | Read route parameters inside a component |
| Route Guard | Block/redirect navigation based on a condition |

---

## Application State After Phase 8

```
✅ app.routes.ts — two routes defined
✅ AppComponent — hosts router-outlet and nav link
✅ StudentDetailComponent — reads :id, loads student from service
✅ studentExistsGuard — blocks invalid student ids
✅ StudentCardComponent — View link navigates to detail page
✅ StudentService — getStudentById() added
```

**Next → Phase 9: Forms**
We will add an Add Student form and an Edit Student form using Reactive Forms with validation.
