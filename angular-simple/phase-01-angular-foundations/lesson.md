# Phase 1 — Angular Foundations

## What We Are Building

We will build a **Student Management System** step by step across all phases.

By the end of this phase, you will have a running Angular app that shows a simple Student Management page.

---

## 1. What is Angular?

Angular is a **frontend framework** built by Google.

It lets you build **Single Page Applications (SPA)** using TypeScript, HTML, and CSS.

You already know:
- Java + Spring → builds the **backend** (REST APIs)
- Angular → builds the **frontend** (the UI in the browser)

They connect through **HTTP requests**, just like you did with Postman.

---

## 2. What is a Single Page Application (SPA)?

In a traditional website:
- Every time you click a link → browser loads a **new HTML page** from the server.

In a SPA:
- The browser loads **one HTML page** once.
- Angular dynamically updates the content **without reloading**.

This makes the app feel fast, like a desktop application.

---

## 3. Modern Angular (v17+)

We are using **Angular 17+**, which introduced:

| Feature | Description |
|---|---|
| Standalone Components | No need for NgModule |
| `@if` / `@for` | New simpler template syntax |
| Signals | New reactivity model |

You will learn each of these as we go. For now, just know we are using **modern Angular**.

---

## 4. Setup

### Prerequisites

Make sure you have these installed:

```bash
node --version    # Should be v18 or higher
npm --version     # Should be v9 or higher
```

### Install Angular CLI

```bash
npm install -g @angular/cli
```

Verify:

```bash
ng version
```

---

## 5. Create the Project

```bash
ng new student-management --standalone --style=css --routing=true --skip-tests
```

| Flag | Meaning |
|---|---|
| `--standalone` | Use modern standalone components (no NgModule) |
| `--style=css` | Use plain CSS |
| `--routing=true` | Enable routing from the start |
| `--skip-tests` | Skip test files (we focus on the app) |

When prompted about SSR → type **No**.

Move into the project folder:

```bash
cd student-management
```

---

## 6. Project Structure

```
student-management/
├── src/
│   ├── app/
│   │   ├── app.component.ts       ← Root component (TypeScript)
│   │   ├── app.component.html     ← Root component (Template)
│   │   ├── app.component.css      ← Root component (Styles)
│   │   └── app.config.ts          ← App configuration
│   ├── index.html                 ← The ONE HTML page loaded by browser
│   └── main.ts                    ← Entry point — bootstraps the app
├── angular.json                   ← Angular CLI configuration
└── package.json                   ← npm dependencies
```

### Key idea

- `index.html` contains `<app-root>` — a custom HTML tag.
- Angular replaces `<app-root>` with the content from `app.component.html`.
- Everything you see in the browser comes from Angular components.

---

## 7. Understand the Root Component

Open `src/app/app.component.ts`:

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'student-management';
}
```

### What this means

| Part | Meaning |
|---|---|
| `@Component` | Decorator — marks this class as an Angular component |
| `selector: 'app-root'` | The custom HTML tag for this component |
| `standalone: true` | Modern Angular — no NgModule needed |
| `templateUrl` | The HTML file for this component |
| `styleUrl` | The CSS file for this component |

This is similar to a Java class with annotations (`@RestController`, `@Service`).

---

## 8. Hello Angular — Our First Page

Let's replace the default content with our Student Management app shell.

### `src/app/app.component.ts`

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  appTitle = 'Student Management System';
}
```

### `src/app/app.component.html`

Replace all existing content with:

```html
<div class="container">
  <h1>{{ appTitle }}</h1>
  <p>Welcome! Use this app to manage students.</p>
</div>
```

`{{ appTitle }}` — this is **interpolation**. Angular reads the `appTitle` property from the TypeScript class and displays it in the HTML. You will learn this fully in Phase 3.

### `src/app/app.component.css`

```css
.container {
  max-width: 900px;
  margin: 40px auto;
  font-family: Arial, sans-serif;
}

h1 {
  color: #333;
  border-bottom: 2px solid #007bff;
  padding-bottom: 10px;
}
```

---

## 9. Run the Application

```bash
ng serve
```

Open your browser at: **http://localhost:4200**

You should see:

```
Student Management System
Welcome! Use this app to manage students.
```

Angular watches your files and automatically refreshes the browser when you save changes.

---

## Phase 1 Summary

| Concept | What You Learned |
|---|---|
| SPA | One page, dynamic content, no full reload |
| Angular CLI | `ng new`, `ng serve` commands |
| Project Structure | `src/app/`, `index.html`, `main.ts` |
| Component | TypeScript class + HTML template + CSS |
| `@Component` | Decorator that defines a component |
| Interpolation `{{ }}` | Display TypeScript data in HTML |

---

## Application State After Phase 1

```
✅ Running Angular app
✅ Root component with app title
✅ Basic page layout
```

**Next → Phase 2: Components and UI**
We will break this page into separate Angular components.
