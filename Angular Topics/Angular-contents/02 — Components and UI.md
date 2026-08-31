# Phase 2 — Components and UI

## What You Will Learn

In Phase 1 you created the Student Management System as a single component. In this phase you will learn what Angular components truly are, how they are structured, and how to break a UI into smaller, reusable pieces.

By the end of this phase your application will have four components working together:

```
AppComponent          ← Root (already exists)
├── NavbarComponent   ← Top navigation bar
├── StudentListComponent  ← Displays the list of students
│   └── StudentCardComponent  ← Displays one student card
```

---

## 1. What is a Component?

A **component** is the fundamental building block of every Angular application. Everything you see on screen is a component.

Think of a webpage as a set of LEGO bricks. Each brick is a component — a self-contained, reusable piece that has:
- Its own **HTML template** — what it looks like
- Its own **CSS styles** — how it is styled
- Its own **TypeScript class** — what data it holds and what it can do

### Components and Spring Boot

You already understand this concept from Spring Boot:

| Spring Boot | Angular | Purpose |
|---|---|---|
| `@RestController` | `@Component` | Marks a class with a special role |
| Method in a Controller | Method in a Component class | Handles logic/actions |
| Thymeleaf template | HTML template file | Defines the view |
| CSS in static resources | Component CSS file | Styles the view |

The difference: Angular components are not just controllers — they are **view + logic bundled together**, each responsible for one piece of the UI.

### Why Break UI into Components?

Imagine building the entire Student Management System in a single HTML file. After a while it becomes thousands of lines long — impossible to navigate or maintain.

Components solve this:
- **Reusability** — write a `StudentCardComponent` once, use it for every student
- **Separation of concerns** — the navbar knows nothing about the student list
- **Maintainability** — update the card design in one file; every card updates everywhere
- **Testability** — test each component in isolation

---

## 2. Anatomy of a Component

Every Angular component consists of three files (and optionally a test file):

```
student-card/
├── student-card.component.ts      ← The class and @Component decorator
├── student-card.component.html    ← The HTML template
└── student-card.component.css     ← The component-scoped styles
```

### The TypeScript File — `*.component.ts`

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-student-card',    // The HTML tag for this component
  standalone: true,                // Self-contained, no NgModule needed
  imports: [],                     // Other components/directives used in template
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {
  // Properties and methods go here
}
```

The `@Component` decorator is metadata — it tells Angular how to treat this class.

#### `selector` — The Component's HTML Tag

The `selector` value becomes the HTML tag you use to place this component anywhere in the application:

```html
<!-- Using the selector in another component's template -->
<app-student-card></app-student-card>
```

Angular sees `<app-student-card>` and replaces it with the full rendered output of `StudentCardComponent`. This is the same mechanism as `<app-root>` in `index.html`.

#### `standalone: true` — Modern Angular

In older Angular applications, components had to be declared in an `NgModule` — a separate registry file. Modern Angular (v17+) uses **standalone components**, which manage their own imports directly inside `@Component`. This is simpler and more explicit — you will see exactly what each component depends on.

#### `imports: []` — Declaring Dependencies

When your template uses another component, a directive, or a pipe, you declare it here. For example, if `StudentListComponent` uses `StudentCardComponent` inside its template, you import it:

```typescript
imports: [StudentCardComponent]
```

Angular uses this list to know which components to resolve when it renders the template.

---

## 3. Component Templates — HTML with Superpowers

The template file (`.component.html`) is standard HTML, but Angular adds several capabilities on top. You have already seen one: **interpolation** (`{{ }}`).

Templates are compiled by Angular — they are not raw HTML files sent to the browser. Angular processes them and produces efficient JavaScript that updates the DOM.

### Template Rules

- A component template must have **one root element** wrapping everything, or use Angular's `<ng-container>` (a non-rendered wrapper). From Angular 17 onwards multiple root elements are also supported, but one root is still the cleanest approach for beginners.
- You can use any HTML tags plus the selectors of imported components.
- Keep templates focused — a template that is hundreds of lines long is a signal to extract a child component.

---

## 4. Component Styles — Scoped CSS

The CSS file (`.component.css`) applies **only** to that component's template. This is called **View Encapsulation**.

This means:

```css
/* In student-card.component.css */
h2 {
  color: #1a73e8;
}
```

This rule affects **only** the `h2` tags inside `StudentCardComponent`. It will not accidentally affect `h2` tags in `NavbarComponent` or anywhere else.

This solves one of the most common problems in large web applications — CSS rules unintentionally overriding each other across the page.

> **Global styles** that should apply everywhere (fonts, resets, body background) go in `src/styles.css` — you already added some there in Phase 1.

---

## 5. Generating Components with the CLI

You could create the three files manually, but Angular CLI generates them for you with the correct structure and filenames:

```bash
ng generate component navbar
ng generate component student-list
ng generate component student-card
```

Or use the shorthand:

```bash
ng g c navbar
ng g c student-list
ng g c student-card
```

Run these three commands inside your `student-management` project folder. The CLI creates a subfolder for each component under `src/app/`:

```
src/app/
├── navbar/
│   ├── navbar.component.ts
│   ├── navbar.component.html
│   ├── navbar.component.css
│   └── navbar.component.spec.ts
├── student-list/
│   ├── student-list.component.ts
│   ├── student-list.component.html
│   ├── student-list.component.css
│   └── student-list.component.spec.ts
├── student-card/
│   ├── student-card.component.ts
│   ├── student-card.component.html
│   ├── student-card.component.css
│   └── student-card.component.spec.ts
├── app.component.ts
├── app.component.html
├── app.component.css
└── app.config.ts
```

The `.spec.ts` files are unit test files — Angular generates them automatically. You can ignore them for now.

---

## 6. Building the NavbarComponent

### `src/app/navbar/navbar.component.ts`

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  appName = 'Student Management System';
}
```

### `src/app/navbar/navbar.component.html`

```html
<nav class="navbar">
  <div class="navbar-brand">
    <span class="brand-icon">🎓</span>
    <span class="brand-name">{{ appName }}</span>
  </div>
  <div class="navbar-links">
    <a href="#">Home</a>
    <a href="#">Students</a>
    <a href="#">About</a>
  </div>
</nav>
```

> **Note:** The `href="#"` links are placeholders. In Phase 8 you will replace them with Angular Router links that navigate between real pages.

### `src/app/navbar/navbar.component.css`

```css
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #1a73e8;
  color: white;
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: bold;
}

.brand-icon {
  font-size: 24px;
}

.navbar-links {
  display: flex;
  gap: 24px;
}

.navbar-links a {
  color: white;
  text-decoration: none;
  font-size: 15px;
  opacity: 0.9;
  transition: opacity 0.2s;
}

.navbar-links a:hover {
  opacity: 1;
  text-decoration: underline;
}
```

---

## 7. Building the StudentCardComponent

### `src/app/student-card/student-card.component.ts`

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {
  studentName = 'Alice Johnson';
  studentId = 'STU-001';
  course = 'Computer Science';
  year = 2;
}
```

For now this component has hardcoded data. In Phase 6 (Component Communication) you will learn how to pass different student data into this component so it becomes truly reusable.

### `src/app/student-card/student-card.component.html`

```html
<div class="student-card">
  <div class="card-header">
    <div class="avatar">{{ studentName[0] }}</div>
    <div class="student-info">
      <h3>{{ studentName }}</h3>
      <span class="student-id">{{ studentId }}</span>
    </div>
  </div>
  <div class="card-body">
    <div class="detail-row">
      <span class="label">Course</span>
      <span class="value">{{ course }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Year</span>
      <span class="value">Year {{ year }}</span>
    </div>
  </div>
  <div class="card-footer">
    <button class="btn btn-view">View</button>
    <button class="btn btn-edit">Edit</button>
  </div>
</div>
```

**What is `{{ studentName[0] }}`?** Interpolation evaluates any valid TypeScript expression. `studentName[0]` gets the first character of the string — used here as a simple avatar initial. So `'Alice Johnson'[0]` renders as `'A'`.

### `src/app/student-card/student-card.component.css`

```css
.student-card {
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.student-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background-color: #f0f4ff;
  border-bottom: 1px solid #e0e7ff;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #1a73e8;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  flex-shrink: 0;
}

.student-info h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #1a1a2e;
}

.student-id {
  font-size: 12px;
  color: #666;
  background-color: #e0e7ff;
  padding: 2px 8px;
  border-radius: 12px;
}

.card-body {
  padding: 16px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.detail-row:last-child {
  border-bottom: none;
}

.label {
  color: #666;
}

.value {
  color: #1a1a2e;
  font-weight: 500;
}

.card-footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background-color: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.btn {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-view {
  background-color: #1a73e8;
  color: white;
}

.btn-view:hover {
  background-color: #1557b0;
}

.btn-edit {
  background-color: #e8f0fe;
  color: #1a73e8;
}

.btn-edit:hover {
  background-color: #d2e3fc;
}
```

---

## 8. Building the StudentListComponent

### `src/app/student-list/student-list.component.ts`

```typescript
import { Component } from '@angular/core';
import { StudentCardComponent } from '../student-card/student-card.component';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [StudentCardComponent],   // ← Import so the template can use <app-student-card>
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  sectionTitle = 'All Students';
}
```

**Key point:** `StudentListComponent` imports `StudentCardComponent` inside `@Component`. This is how Angular knows that `<app-student-card>` inside the template refers to `StudentCardComponent`. Without this import, Angular would not recognise the tag and would throw an error.

This is analogous to Java imports — you must explicitly declare what you are using.

### `src/app/student-list/student-list.component.html`

```html
<section class="student-list-section">
  <div class="section-header">
    <h2>{{ sectionTitle }}</h2>
    <button class="btn-add">+ Add Student</button>
  </div>

  <div class="student-grid">
    <!-- Hardcoded cards for now — Phase 4 will replace these with @for -->
    <app-student-card></app-student-card>
    <app-student-card></app-student-card>
    <app-student-card></app-student-card>
  </div>
</section>
```

Three `<app-student-card>` tags render three identical cards for now. In Phase 4 (Dynamic UI) you will loop over a real array of students and display each one with its own data.

### `src/app/student-list/student-list.component.css`

```css
.student-list-section {
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  color: #1a1a2e;
  font-size: 22px;
}

.btn-add {
  background-color: #1a73e8;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-add:hover {
  background-color: #1557b0;
}

.student-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
```

---

## 9. Wiring Everything into AppComponent

Now update the root `AppComponent` to use the two top-level components:

### `src/app/app.component.ts`

```typescript
import { Component } from '@angular/core';
import { NavbarComponent } from './navbar/navbar.component';
import { StudentListComponent } from './student-list/student-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NavbarComponent, StudentListComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  // AppComponent is now purely a layout shell
}
```

### `src/app/app.component.html`

Replace the entire file content with:

```html
<app-navbar></app-navbar>

<main class="app-main">
  <app-student-list></app-student-list>
</main>
```

### `src/app/app.component.css`

```css
.app-main {
  max-width: 1100px;
  margin: 0 auto;
}
```

Save all files. Your browser at **http://localhost:4200** should now show:
- A blue navigation bar at the top with the app name and links
- A grid of three student cards below it

---

## 10. How Angular Renders the Component Tree

When Angular renders your application it walks a **component tree**:

```
bootstrapApplication(AppComponent)
  → Renders AppComponent template
    → Finds <app-navbar>
      → Renders NavbarComponent template
    → Finds <app-student-list>
      → Renders StudentListComponent template
        → Finds <app-student-card> (×3)
          → Renders StudentCardComponent template (×3)
```

Each component renders independently. Angular assembles the final page by composing all of them together. This is called the **component tree** and is fundamental to how Angular works.

---

## 11. View Encapsulation in Practice

Open browser DevTools (F12) and inspect one of the student card elements. You will notice Angular has added a custom attribute to every element in the component, for example:

```html
<div class="student-card" _nghost-ng-c123456789>
  <div class="card-header" _ngcontent-ng-c123456789>
    ...
  </div>
</div>
```

Angular modifies the CSS rules to include this attribute:

```css
/* What you wrote */
.student-card { ... }

/* What Angular compiles it to */
.student-card[_ngcontent-ng-c123456789] { ... }
```

This is how Angular **scopes CSS** to its component — by appending a unique attribute selector. It happens automatically; you never need to write these attributes yourself. The result is that your component's CSS rules are guaranteed not to leak out and affect other components.

---

## 12. The Application So Far

```
src/app/
├── navbar/                    ← Top navigation bar
├── student-list/              ← Lists student cards in a grid
├── student-card/              ← One student's card (name, ID, course, year)
├── app.component.ts           ← Root layout shell
├── app.component.html         ← <app-navbar> + <app-student-list>
└── app.config.ts
```

**Current state:** A component-based Student Management System showing a navbar and a grid of student cards, all with scoped CSS and no global style conflicts.

---

## Phase 2 Summary

| Concept | What You Learned |
|---|---|
| Component | A self-contained UI unit: TypeScript class + HTML template + CSS |
| `@Component` decorator | Marks a class as a component; provides `selector`, `templateUrl`, `styleUrl` |
| `selector` | The HTML tag used to place a component inside another component's template |
| `standalone: true` | Modern Angular — no NgModule needed |
| `imports: []` | Declares which components/directives are used in this component's template |
| `ng generate component` | CLI command to scaffold a component's three files |
| View Encapsulation | Component CSS is automatically scoped — cannot leak to other components |
| Component tree | Angular renders by walking the tree of nested components |
| Template expressions | `{{ expression }}` evaluates any valid TypeScript — not just property names |

---

## What's Next

In **Phase 3 — Data Binding**, you will learn all four ways Angular connects TypeScript data to the HTML template:
- **Interpolation** `{{ }}` — already used, explored deeper
- **Property Binding** `[property]` — set an element's attribute from TypeScript
- **Event Binding** `(event)` — respond to user actions like button clicks
- **Two-Way Binding** `[(ngModel)]` — keep a form input and a TypeScript property in sync

You will make the student cards interactive and add a live student count display to the list.

