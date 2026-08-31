# Exercises — Phase 2: Components and UI

---

## Exercise 1 — Build a `FooterComponent`

**Goal:** Practice generating a component with the CLI, writing a template, scoping CSS, and using it in `AppComponent`.

### Tasks

1. Generate the component:
   ```bash
   ng g c footer
   ```

2. Add the following properties to `footer.component.ts`:
   ```typescript
   appName = 'Student Management System';
   currentYear = new Date().getFullYear();
   version = '1.0.0';
   ```

3. Write `footer.component.html`:
   ```html
   <footer class="app-footer">
     <p>© {{ currentYear }} {{ appName }} · v{{ version }}</p>
     <nav class="footer-links">
       <a href="#">Privacy</a>
       <a href="#">Help</a>
       <a href="#">Contact</a>
     </nav>
   </footer>
   ```

4. Style `footer.component.css` with a dark background, white text, and flex layout.

5. Add `FooterComponent` to `AppComponent`'s `imports` array and place `<app-footer>` at the bottom of `app.component.html`.

### Expected Result
A styled footer appears at the bottom of every page with the copyright line and three links.

### Challenge
Open DevTools and inspect the footer elements. Find the `_nghost` and `_ngcontent` attributes Angular adds. Explain in your own words why those attributes exist.

---

## Exercise 2 — Create a `StatsBarComponent`

**Goal:** Practice building a component that displays summary statistics, and understand how each component has its own isolated scope.

### Tasks

1. Generate the component:
   ```bash
   ng g c stats-bar
   ```

2. Add four properties to `stats-bar.component.ts`:
   ```typescript
   totalStudents = 5;
   activeStudents = 3;
   inactiveStudents = 1;
   graduatedStudents = 1;
   ```

3. Write `stats-bar.component.html` as a horizontal bar of four stat tiles, each showing a label and number.

4. Style it so the four tiles sit side by side in a flex row, each with a coloured top border (blue for total, green for active, red for inactive, grey for graduated).

5. Place `<app-stats-bar>` in `app.component.html` between the navbar and the main content.

### Expected Result
A horizontal stats bar appears below the navbar, showing four coloured tiles with student counts.

### Challenge
What happens if you write `.app-main { color: red; }` inside `stats-bar.component.css` — does it affect the `<main>` element defined in `app.component.html`? Test it and explain the result.

---

## Exercise 3 — Refactor: Extract a `PageHeaderComponent`

**Goal:** Practice identifying repeating UI patterns and extracting them into a reusable component.

### Tasks

1. Notice that `StudentListComponent` and `StudentDetailComponent` (from Phase 8) both show a page title and a back-link. Extract this into a shared component:
   ```bash
   ng g c shared/page-header
   ```

2. Add two properties to `page-header.component.ts`:
   ```typescript
   title = '';
   showBackLink = false;
   backLabel = '← Back';
   ```

3. Write `page-header.component.html`:
   ```html
   <div class="page-header">
     @if (showBackLink) {
       <a href="#" class="back-link">{{ backLabel }}</a>
     }
     <h1>{{ title }}</h1>
   </div>
   ```

4. Use `<app-page-header>` in at least two places in the application, setting the `title` property directly in the template for now (e.g. `<app-page-header title="All Students">`). Property binding with `@Input()` is covered in Phase 6 — for now hardcoding the attribute is fine.

### Expected Result
Both pages show a consistent header. Updating the styles in `page-header.component.css` immediately changes both pages.

