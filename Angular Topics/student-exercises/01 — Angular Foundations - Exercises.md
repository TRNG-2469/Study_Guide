# Exercises — Phase 1: Angular Foundations

---

## Exercise 1 — Personalise the Application Shell

**Goal:** Practice editing component properties and understanding how interpolation renders them.

### Tasks

1. Open `src/app/app.component.ts` and add two new properties:
   ```typescript
   appTitle = 'Student Management System';
   appVersion = '1.0.0';
   appAuthor = 'Your Name';
   ```

2. Update `src/app/app.component.html` to display all three properties:
   ```html
   <header class="app-header">
     <h1>{{ appTitle }}</h1>
     <p>Version {{ appVersion }} · Built by {{ appAuthor }}</p>
   </header>
   ```

3. Style the header so the version line is smaller and lighter than the title.

### Expected Result
The page shows a two-line header: the app title on one line and the version and author on a second, smaller line.

### Challenge
Add a `currentYear` property that uses `new Date().getFullYear()` and display it in the footer as `© 2026 Your Name`. Observe how Angular evaluates the TypeScript expression.

---

## Exercise 2 — Explore the Project Structure

**Goal:** Deepen your understanding of every file Angular generated.

### Tasks

1. Open each of the following files and write a one-line comment at the top explaining what the file does:
   - `src/main.ts`
   - `src/app/app.config.ts`
   - `src/app/app.component.ts`
   - `src/index.html`
   - `angular.json`
   - `tsconfig.json`

2. In `src/index.html`, temporarily change `<app-root></app-root>` to `<app-root>Loading…</app-root>`. Observe what the browser shows. Then revert the change. Explain in a comment why the text `Loading…` never appears.

3. In `src/app/app.component.ts`, rename `appTitle` to `heading`. Update the template to use `{{ heading }}`. Verify the browser updates automatically without restarting the server.

### Questions to Answer (write in a comment block at the top of `app.component.ts`)
- What would happen if you removed `standalone: true` from `@Component`?
- Why does Angular use `selector: 'app-root'` instead of a plain `div`?

---

## Exercise 3 — Create a "Library Management System" Starter

**Goal:** Practice using the Angular CLI to scaffold a new project from scratch.

### Tasks

1. In a new terminal (outside the `student-management` folder), run:
   ```bash
   ng new library-management
   ```
   Choose CSS and answer No to SSR.

2. Navigate into the project and run `ng serve`. Confirm it opens at `http://localhost:4201` (Angular picks the next available port if 4200 is in use).

3. In `src/app/app.component.ts`, replace the generated content with:
   ```typescript
   export class AppComponent {
     systemName = 'Library Management System';
     totalBooks = 0;
     isOpen = true;
   }
   ```

4. In `src/app/app.component.html`, display all three properties and use a ternary expression in interpolation to show `'Open'` or `'Closed'` based on `isOpen`:
   ```html
   <h1>{{ systemName }}</h1>
   <p>Books in catalogue: {{ totalBooks }}</p>
   <p>Status: {{ isOpen ? 'Open' : 'Closed' }}</p>
   ```

### Expected Result
A second Angular app running in parallel showing the library system heading, book count, and open/closed status — all driven by TypeScript properties.

