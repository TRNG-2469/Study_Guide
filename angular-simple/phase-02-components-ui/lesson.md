# Phase 2 — Components and UI

## Where We Left Off

After Phase 1, we have a single `AppComponent` that shows:

```
Student Management System
Welcome! Use this app to manage students.
```

Everything is in one component. That works for now, but real apps split the UI into **smaller, reusable pieces** called **components**.

---

## 1. What is a Component?

A component is a **self-contained piece of UI** — its own HTML, CSS, and TypeScript.

Think of it like a **Java class** — it has its own responsibility, and you can reuse it anywhere.

In our app, we will create:

| Component | Responsibility |
|---|---|
| `HeaderComponent` | Shows the app title/navbar |
| `StudentListComponent` | Shows the list of students |

`AppComponent` will just **host** these two components.

---

## 2. Generate Components with Angular CLI

Angular CLI can generate components for you:

```bash
ng generate component header
ng generate component student-list
```

Short form:

```bash
ng g c header
ng g c student-list
```

This creates a folder for each component inside `src/app/`:

```
src/app/
├── header/
│   ├── header.component.ts
│   ├── header.component.html
│   └── header.component.css
├── student-list/
│   ├── student-list.component.ts
│   ├── student-list.component.html
│   └── student-list.component.css
└── app.component.ts
```

---

## 3. HeaderComponent

### `src/app/header/header.component.ts`

No changes needed. Angular CLI generates this automatically:

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {}
```

Notice `selector: 'app-header'` — this is the custom HTML tag you use to place this component.

### `src/app/header/header.component.html`

```html
<header class="app-header">
  <h1>Student Management System</h1>
  <p>Manage your students easily</p>
</header>
```

### `src/app/header/header.component.css`

```css
.app-header {
  background-color: #007bff;
  color: white;
  padding: 20px 30px;
}

.app-header h1 {
  margin: 0;
  font-size: 24px;
}

.app-header p {
  margin: 4px 0 0;
  font-size: 14px;
  opacity: 0.85;
}
```

---

## 4. StudentListComponent

### `src/app/student-list/student-list.component.ts`

No changes needed. Angular CLI generates:

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {}
```

### `src/app/student-list/student-list.component.html`

For now, just static HTML — no dynamic data yet (that comes in Phase 3 and 4):

```html
<div class="student-list">
  <h2>Students</h2>
  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Email</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>1</td>
        <td>Alice</td>
        <td>alice@example.com</td>
      </tr>
      <tr>
        <td>2</td>
        <td>Bob</td>
        <td>bob@example.com</td>
      </tr>
    </tbody>
  </table>
</div>
```

### `src/app/student-list/student-list.component.css`

```css
.student-list {
  padding: 20px 30px;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

th, td {
  border: 1px solid #ddd;
  padding: 10px;
  text-align: left;
}

th {
  background-color: #f0f0f0;
}
```

---

## 5. Use Components in AppComponent

Now we **combine** the two components inside `AppComponent`.

### `src/app/app.component.ts`

Import and add both components to `imports`:

```typescript
import { Component } from '@angular/core';
import { HeaderComponent } from './header/header.component';
import { StudentListComponent } from './student-list/student-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [HeaderComponent, StudentListComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {}
```

> **Note:** In standalone Angular, you import components directly — no NgModule required.

### `src/app/app.component.html`

Replace all content with:

```html
<app-header></app-header>
<app-student-list></app-student-list>
```

That's it. Angular replaces each custom tag with that component's template.

### `src/app/app.component.css`

You can clear this file or leave it empty.

---

## 6. Run the App

```bash
ng serve
```

Open **http://localhost:4200**

You should see:
- A **blue header** with the app title
- A **student table** with two static rows

---

## Phase 2 Summary

| Concept | What You Learned |
|---|---|
| Component | Self-contained UI unit (TS + HTML + CSS) |
| `ng g c` | CLI command to generate a component |
| `selector` | Custom HTML tag for a component |
| `imports` array | How standalone components include other components |
| Composing UI | `AppComponent` hosts `HeaderComponent` + `StudentListComponent` |

---

## Application State After Phase 2

```
✅ HeaderComponent — app title and subtitle
✅ StudentListComponent — static student table
✅ AppComponent — composes the two components
```

**Next → Phase 3: Data Binding**
We will replace the static table rows with real TypeScript data using interpolation, property binding, event binding, and two-way binding.
