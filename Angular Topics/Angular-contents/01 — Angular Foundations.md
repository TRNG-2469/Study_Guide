# Phase 1 — Angular Foundations

## What You Will Learn

In this phase you will understand what Angular is, why it exists, and how to create your very first Angular application — the **Student Management System** that you will build upon throughout this entire course.

By the end of this phase your application will display a simple "Student Management System" heading in the browser — but more importantly you will understand *every file and folder* Angular created for you and *why* each one exists.

---

## 1. What is Angular?

Angular is a **frontend framework** built by Google. It lets you build complex, interactive web applications using TypeScript.

You already know:
- **Spring Boot** organises your Java backend into Controllers, Services, and Repositories.
- **Angular** does the same for your frontend — it organises your HTML, CSS, and TypeScript into a structured, maintainable application.

Just as Spring Boot gives you conventions and tooling so you are not writing raw Java servlets, Angular gives you conventions and tooling so you are not writing raw DOM manipulation code.

| Spring Boot (Backend) | Angular (Frontend) |
|---|---|
| `@Controller` handles HTTP requests | Component handles user interactions |
| `@Service` holds business logic | Service holds shared logic |
| `@Repository` talks to the database | `HttpClient` talks to REST APIs |
| Maven manages dependencies | npm manages dependencies |
| `spring-boot-starter` scaffolds the project | Angular CLI scaffolds the project |

---

## 2. Single-Page Application (SPA)

### Traditional Multi-Page Applications

In a traditional web application every time the user clicks a link the browser makes a full round-trip to the server:

```
User clicks link
  → Browser sends GET request to server
  → Server processes request
  → Server returns a completely new HTML page
  → Browser re-renders the entire page
```

Every navigation causes a full page reload. The user sees a flash. State is lost between pages.

### Single-Page Application

An SPA loads **one HTML page once**. After that, JavaScript takes over and dynamically swaps content inside that single page without ever reloading the browser:

```
Browser loads index.html once
  → Angular JavaScript loads
  → User clicks link
  → Angular intercepts the click
  → Angular swaps only the content that changed
  → No full page reload
```

**Benefits:**
- Faster navigation after the initial load
- Smooth, app-like user experience
- The server only needs to provide data (JSON from your Spring REST APIs), not full HTML pages
- Frontend and backend are completely decoupled

**This is exactly how modern web applications work** — Gmail, Google Maps, GitHub — they are all SPAs.

---

## 3. Modern Angular — What You Need to Know

Angular has changed significantly over the years. This course teaches **modern Angular (v17+)**.

### Key Modern Features You Will Use

| Feature | What It Means |
|---|---|
| Standalone Components | No need for `NgModule` — components are self-contained |
| `@if` / `@for` | New built-in control flow syntax (replaces `*ngIf`, `*ngFor`) |
| Signals | A modern reactive state system |
| `inject()` function | A cleaner way to use Dependency Injection |

> **Note:** You may find older Angular tutorials online that use `NgModule`, `*ngIf`, and `*ngFor`. Those still work but this course uses the modern equivalents. When you see those in older code you will know what they are.

---

## 4. Setting Up Your Angular Environment

You already have **Node.js** and **npm** installed from your JavaScript/TypeScript work. Angular is installed as a global npm package.

### Step 1 — Verify Node.js and npm

Open a terminal and run:

```bash
node --version
npm --version
```

You need Node.js 18.19 or higher. If the versions are below that, download the latest LTS version from [nodejs.org](https://nodejs.org).

### Step 2 — Install the Angular CLI

```bash
npm install -g @angular/cli
```

The `-g` flag installs it **globally** — available in any terminal, in any folder, just like how `mvn` is available anywhere after Maven is installed.

### Step 3 — Verify the Installation

```bash
ng version
```

You should see output listing the Angular CLI version and your Node/npm versions. If you see this, you are ready.

---

## 5. Angular CLI — Your Project Toolbox

`ng` is the Angular CLI command. Think of it as the Angular equivalent of the Spring Initializr and Maven combined — it scaffolds projects, generates files, runs a dev server, and builds for production.

### The Commands You Will Use Most

| Command | Purpose |
|---|---|
| `ng new project-name` | Create a new Angular project |
| `ng serve` | Start the development server (like `mvn spring-boot:run`) |
| `ng generate component name` | Generate a new component |
| `ng generate service name` | Generate a new service |
| `ng build` | Build for production |

You will encounter all of these over the next phases. For now, the two you need are `ng new` and `ng serve`.

---

## 6. Creating the Student Management Application

### Create the Project

In your terminal, navigate to where you want to create the project and run:

```bash
ng new student-management
```

The CLI will ask you a few questions. Answer as follows:

```
? Which stylesheet format would you like to use?
  ❯ CSS          ← choose this

? Do you want to enable Server-Side Rendering (SSR)?
  No             ← choose No
```

Angular will then create the project folder and install all npm dependencies. This takes a minute or two — it is downloading your frontend dependencies, the same way Maven downloads Spring Boot's JARs.

### Open the Project

```bash
cd student-management
```

Open this folder in VS Code:

```bash
code .
```

---

## 7. Angular Project Structure — Every File Explained

When you open the project you will see:

```
student-management/
├── src/
│   ├── app/
│   │   ├── app.component.ts        ← Root component (TypeScript)
│   │   ├── app.component.html      ← Root component template (HTML)
│   │   ├── app.component.css       ← Root component styles
│   │   ├── app.component.spec.ts   ← Root component unit test
│   │   └── app.config.ts           ← Application configuration
│   ├── index.html                  ← The ONE HTML file (the "single page")
│   ├── main.ts                     ← Application entry point
│   └── styles.css                  ← Global styles
├── public/
│   └── favicon.ico
├── angular.json                    ← Angular CLI configuration
├── package.json                    ← npm dependencies
├── tsconfig.json                   ← TypeScript configuration
└── tsconfig.app.json
```

### Understanding the Key Files

#### `index.html` — The Single Page

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>StudentManagement</title>
  <base href="/">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="icon" type="image/x-icon" href="favicon.ico">
</head>
<body>
  <app-root></app-root>   <!-- Angular replaces this with your application -->
</body>
</html>
```

This is the **only HTML file** that is ever sent to the browser. The `<app-root>` tag is where Angular renders your entire application. Angular replaces this tag dynamically with your component tree.

#### `main.ts` — The Entry Point

```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
```

This is Angular's startup file — similar to Spring Boot's `main()` method with `SpringApplication.run()`. It tells Angular: *"Start the application using `AppComponent` as the root."*

#### `app.config.ts` — Application Configuration

```typescript
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

This is where application-wide configuration lives — similar to Spring Boot's `application.properties` or `@Configuration` classes. You will add more providers here later (for example, `provideHttpClient()` when you connect to your Spring REST APIs).

#### `app.component.ts` — The Root Component

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

Notice the `@Component` decorator — this is Angular's equivalent of Spring's `@Controller` or `@Service`. It marks a TypeScript class as an Angular component and provides metadata:

| Property | Meaning |
|---|---|
| `selector` | The HTML tag name that renders this component (`<app-root>`) |
| `standalone: true` | This component manages its own dependencies — no NgModule needed |
| `imports` | Other components/modules this component uses |
| `templateUrl` | The HTML template file for this component |
| `styleUrl` | The CSS file for this component |

The class itself can hold properties and methods. `title = 'student-management'` is a component property — you will learn how to display it in the template using data binding in Phase 3.

---

## 8. Running the Application

Start the development server:

```bash
ng serve
```

You will see output like:

```
  ✔ Browser application bundle generation complete.

Application bundle generation complete. Watching for file changes...

Local:   http://localhost:4200/
```

Open your browser and go to **http://localhost:4200**.

You will see Angular's default welcome page. That is the generated content inside `app.component.html`. In the next step you will replace it with your Student Management application.

> **Hot Reload:** Just like Spring DevTools, `ng serve` watches your files. Every time you save a file the browser automatically refreshes. You do not need to restart the server.

---

## 9. Hello Angular — Building the Student Management Home Page

Now customise the application to become the Student Management System.

### Update `app.component.ts`

Open `src/app/app.component.ts` and update it:

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

**What changed:** Renamed `title` to `appTitle` and set a meaningful value. This property will be used in the template.

### Update `app.component.html`

Delete everything in `src/app/app.component.html` and replace it with:

```html
<div class="app-container">
  <header class="app-header">
    <h1>{{ appTitle }}</h1>
    <p>Manage your students efficiently</p>
  </header>

  <main>
    <p>Welcome to the Student Management System.</p>
    <p>Use the navigation to manage students.</p>
  </main>
</div>
```

**What is `{{ appTitle }}`?** This is **interpolation** — Angular's simplest form of data binding. It reads the `appTitle` property from your TypeScript component class and renders its value in the HTML. You will learn data binding in depth in Phase 3.

### Update `app.component.css`

Open `src/app/app.component.css` and add:

```css
.app-container {
  font-family: Arial, sans-serif;
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.app-header {
  background-color: #1a73e8;
  color: white;
  padding: 20px 30px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.app-header h1 {
  margin: 0 0 5px 0;
  font-size: 28px;
}

.app-header p {
  margin: 0;
  opacity: 0.85;
}

main {
  padding: 10px 0;
  color: #333;
}
```

### Update `src/styles.css`

Open `src/styles.css` (the global stylesheet) and add a browser reset:

```css
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  background-color: #f5f7fa;
}
```

Save all files. The browser at **http://localhost:4200** will automatically update and show your Student Management System header.

---

## 10. Understanding What Just Happened

Let us trace the full flow from browser request to rendered page:

```
Browser requests http://localhost:4200
  → Angular dev server returns index.html
  → Browser parses index.html and finds <app-root>
  → Browser loads main.ts (compiled JavaScript)
  → main.ts calls bootstrapApplication(AppComponent)
  → Angular finds <app-root> in the DOM
  → Angular renders AppComponent's template (app.component.html) inside <app-root>
  → {{ appTitle }} is replaced with "Student Management System"
  → User sees the styled page
```

This is the **Angular bootstrap process** — the sequence of events that turns your TypeScript + HTML into what the user sees.

---

## 11. The Application So Far

```
student-management/
└── src/
    └── app/
        ├── app.component.ts      ← Root component with appTitle property
        ├── app.component.html    ← Header and welcome message
        └── app.component.css     ← Blue header, clean layout
```

**Current state:** A single-page application that renders a styled Student Management System header. The application is running at `http://localhost:4200`.

---

## Phase 1 Summary

| Concept | What You Learned |
|---|---|
| Angular | A TypeScript frontend framework by Google, structured like Spring Boot |
| SPA | One HTML page; JavaScript swaps content without full page reloads |
| Modern Angular | Standalone components, new control flow, no NgModule required |
| Node.js + npm | Runtime and package manager for Angular (like JDK + Maven) |
| Angular CLI (`ng`) | Scaffolds projects, generates files, runs dev server |
| `index.html` | The single HTML page; `<app-root>` is where Angular renders |
| `main.ts` | The entry point — like `SpringApplication.run()` |
| `app.config.ts` | Application-wide configuration — providers and routing |
| `@Component` | Decorator that turns a TypeScript class into an Angular component |
| `ng serve` | Development server with hot reload at `http://localhost:4200` |
| Interpolation `{{ }}` | Renders a TypeScript property value in HTML |

---

## What's Next

In **Phase 2 — Components and UI**, you will break the Student Management System's UI into smaller, reusable Angular components — a `NavbarComponent`, a `StudentListComponent`, and a `StudentCardComponent` — using the same `@Component` decorator you learned here.

