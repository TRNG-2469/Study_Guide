# Phase 3 — Data Binding

## What You Will Learn

Data binding is the mechanism Angular uses to keep your TypeScript class and your HTML template **in sync**. Instead of manually querying the DOM and setting values (the way you would in plain JavaScript), Angular handles the synchronisation automatically.

There are four forms of data binding. By the end of this phase you will have used all of them to make the Student Management System interactive:

| Syntax | Direction | Name |
|---|---|---|
| `{{ expression }}` | Class → Template | Interpolation |
| `[property]="expression"` | Class → Template | Property Binding |
| `(event)="handler()"` | Template → Class | Event Binding |
| `[(ngModel)]="property"` | Class ↔ Template | Two-Way Binding |

---

## 1. Interpolation — Displaying Data

You have already used interpolation in Phases 1 and 2. This section deepens your understanding.

### How It Works

Angular evaluates the expression inside `{{ }}` and inserts the result as text into the DOM. The expression can be any valid TypeScript expression:

```html
<!-- Simple property -->
<h2>{{ sectionTitle }}</h2>

<!-- Arithmetic -->
<p>Total students: {{ studentCount * 1 }}</p>

<!-- String method -->
<p>{{ studentName.toUpperCase() }}</p>

<!-- Ternary expression -->
<p>{{ isActive ? 'Active' : 'Inactive' }}</p>

<!-- Array access -->
<div>{{ studentName[0] }}</div>
```

### What Interpolation Cannot Do

Interpolation renders **text only**. It cannot set HTML element properties (for that, use Property Binding). It also cannot execute statements like assignments (`=`) or `new`.

---

## 2. Property Binding — Setting Element Properties

Property Binding sets an HTML element's **DOM property** from a TypeScript expression. The syntax uses square brackets:

```html
[domProperty]="typescriptExpression"
```

### Interpolation vs. Property Binding

Consider this button that should be disabled when there are no students:

```html
<!-- Interpolation — this does NOT work for boolean properties -->
<button disabled="{{ hasNoStudents }}">Add</button>

<!-- Property Binding — this DOES work -->
<button [disabled]="hasNoStudents">Add</button>
```

Why? Because `disabled="{{ hasNoStudents }}"` sets the HTML **attribute** to the string `"true"` or `"false"` — and any non-empty string attribute is treated as `disabled=true` by the browser. Property Binding sets the **DOM property** directly to a real boolean.

### Common Property Binding Examples

```html
<!-- Set the src of an image -->
<img [src]="student.photoUrl" [alt]="student.name">

<!-- Add or remove a CSS class conditionally -->
<div [class.active]="isSelected">...</div>

<!-- Set inline style -->
<div [style.background-color]="student.colour">...</div>

<!-- Control disabled state of a button -->
<button [disabled]="isLoading">Save</button>

<!-- Set a custom attribute -->
<input [placeholder]="searchHint">
```

### The Distinction: Attribute vs. Property

HTML **attributes** are what you write in HTML source (like `href`, `class`, `disabled`). DOM **properties** are the live JavaScript objects on the element. They start with the same values but can diverge. Angular Property Binding targets the **DOM property**, which is what the browser actually uses at runtime.

For the rare case where you need to bind to an HTML attribute directly (e.g. `aria-*`, `colspan`), prefix with `attr.`:

```html
<td [attr.colspan]="columnSpan">...</td>
<button [attr.aria-label]="buttonLabel">...</button>
```

---

## 3. Event Binding — Responding to User Actions

Event Binding listens for a DOM event and calls a method on the TypeScript class when it fires. The syntax uses parentheses:

```html
(domEvent)="typescriptMethod()"
```

This is analogous to JavaScript's `addEventListener`, but declared directly in the template and automatically cleaned up by Angular.

### Common Events

```html
<!-- Button click -->
<button (click)="addStudent()">Add Student</button>

<!-- Input change -->
<input (input)="onSearch($event)">

<!-- Form submission -->
<form (submit)="onFormSubmit($event)">

<!-- Mouse events -->
<div (mouseenter)="highlight()" (mouseleave)="unhighlight()">

<!-- Keyboard events -->
<input (keyup.enter)="search()">
```

### The `$event` Object

When Angular calls your method it can pass the native DOM event object as `$event`. For example, reading what the user typed in an input:

```typescript
onSearch(event: Event): void {
  const input = event.target as HTMLInputElement;
  this.searchTerm = input.value;
}
```

For simpler cases you will use Two-Way Binding with `ngModel` instead.

---

## 4. Two-Way Binding — Keeping Data and UI in Sync

Two-Way Binding combines Property Binding and Event Binding in a single syntax called **banana in a box** (`[()]`):

```html
[(ngModel)]="propertyName"
```

It is called "banana in a box" because parentheses `()` are the banana and square brackets `[]` are the box.

**What it does:**
- Sets the input's value from the TypeScript property (Property Binding direction)
- Updates the TypeScript property whenever the user changes the input (Event Binding direction)

Without Two-Way Binding you would need to write both manually:

```html
<!-- Without Two-Way Binding — verbose -->
<input [value]="searchTerm" (input)="searchTerm = $event.target.value">

<!-- With Two-Way Binding — concise -->
<input [(ngModel)]="searchTerm">
```

### Importing FormsModule

`ngModel` is part of `FormsModule`. To use it you must import it in your component:

```typescript
import { FormsModule } from '@angular/forms';

@Component({
  ...
  imports: [FormsModule],
  ...
})
```

---

## 5. Updating the Application — Making It Interactive

You will now update `StudentListComponent` to demonstrate all four binding types.

### Updated `src/app/student-list/student-list.component.ts`

```typescript
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StudentCardComponent } from '../student-card/student-card.component';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [StudentCardComponent, FormsModule],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  sectionTitle = 'All Students';
  studentCount = 3;
  searchTerm = '';
  showSearch = false;

  // Event binding handler — toggles the search bar visibility
  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) {
      this.searchTerm = '';  // Clear search when hiding
    }
  }

  // Event binding handler — called on Add Student button click
  onAddStudent(): void {
    this.studentCount++;
    alert(`Student count updated to ${this.studentCount}`);
  }
}
```

### Updated `src/app/student-list/student-list.component.html`

```html
<section class="student-list-section">

  <!-- Section header with event binding on both buttons -->
  <div class="section-header">
    <div class="title-area">
      <h2>{{ sectionTitle }}</h2>
      <!-- Interpolation: shows the live student count -->
      <span class="count-badge">{{ studentCount }} students</span>
    </div>
    <div class="action-buttons">
      <!-- Event binding: calls toggleSearch() on click -->
      <button class="btn-search" (click)="toggleSearch()">
        {{ showSearch ? '✕ Close' : '🔍 Search' }}
      </button>
      <!-- Property binding: disabled when studentCount >= 5 -->
      <button
        class="btn-add"
        [disabled]="studentCount >= 5"
        [title]="studentCount >= 5 ? 'Maximum students reached' : 'Add a new student'"
        (click)="onAddStudent()">
        + Add Student
      </button>
    </div>
  </div>

  <!-- Search bar: Two-Way Binding with ngModel -->
  <div class="search-area" [class.visible]="showSearch">
    <input
      type="text"
      [(ngModel)]="searchTerm"
      placeholder="Search students by name..."
      class="search-input">
    <!-- Interpolation: shows live search term feedback -->
    <p class="search-feedback" *ngIf="searchTerm">
      Searching for: <strong>{{ searchTerm }}</strong>
    </p>
  </div>

  <!-- Student grid -->
  <div class="student-grid">
    <app-student-card></app-student-card>
    <app-student-card></app-student-card>
    <app-student-card></app-student-card>
  </div>

</section>
```

> **Note:** You will notice `*ngIf` in the template above. This is a structural directive — covered in depth in Phase 4. For now, know that `*ngIf="searchTerm"` shows the paragraph only when `searchTerm` is not empty.

### Updated `src/app/student-list/student-list.component.css`

```css
.student-list-section {
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-area h2 {
  margin: 0;
  color: #1a1a2e;
  font-size: 22px;
}

.count-badge {
  background-color: #e8f0fe;
  color: #1a73e8;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.btn-search {
  background-color: white;
  color: #1a73e8;
  border: 1px solid #1a73e8;
  padding: 9px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-search:hover {
  background-color: #e8f0fe;
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

.btn-add:hover:not(:disabled) {
  background-color: #1557b0;
}

.btn-add:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.search-area {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease, padding 0.3s ease;
}

.search-area.visible {
  max-height: 100px;
  padding-bottom: 16px;
}

.search-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}

.search-feedback {
  margin: 8px 0 0;
  font-size: 13px;
  color: #555;
}

.student-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
```

---

## 6. Observing All Four Bindings in Action

Open your browser at **http://localhost:4200** and interact with the application:

### Interpolation
The heading shows `All Students` and the badge shows `3 students`. Both are driven from TypeScript properties — change the `studentCount` value in your TypeScript class and watch the badge update instantly on save.

### Event Binding
- Click **🔍 Search** — the search bar slides down. The button label changes to **✕ Close** (driven by the same `showSearch` boolean via interpolation in the button text).
- Click **+ Add Student** — the `onAddStudent()` method runs, increments `studentCount`, and shows an alert.

### Property Binding
- Click **+ Add Student** until `studentCount` reaches 5. The button becomes visually disabled — Angular has set `[disabled]="true"` on the DOM element. Hover over it to see the tooltip change from `'Add a new student'` to `'Maximum students reached'`.

### Two-Way Binding
- Click **🔍 Search** to open the search bar. Start typing in the input. Watch `Searching for: <term>` appear instantly below the input — the `searchTerm` property updates on every keystroke, which immediately re-renders the interpolation expression `{{ searchTerm }}`.

This is the core power of data binding: the template and the TypeScript class are always in sync, with Angular handling all the DOM updates automatically.

---

## 7. Data Binding Under the Hood

Angular's data binding works through **Change Detection**. After every event (a click, a keystroke, a timer, an HTTP response) Angular checks all bound expressions in the component tree and updates the DOM wherever a value has changed.

```
User types in input
  → ngModel emits an event
  → Angular's Change Detection runs
  → Checks {{ searchTerm }} — value changed? Yes → updates DOM
  → Checks [disabled]="studentCount >= 5" — value changed? No → no DOM update
  → Checks every other binding in the component
```

This is why you never need to call `document.getElementById` or `element.textContent = ...` in Angular. Angular does all DOM manipulation for you, and it does it efficiently.

---

## 8. The Application So Far

```
StudentListComponent
  ├── appTitle (Interpolation)            → "All Students"
  ├── studentCount (Interpolation)        → "3 students" badge
  ├── showSearch (Property + Event)       → search bar toggle
  ├── searchTerm (Two-Way + Interpolation)→ live search feedback
  └── onAddStudent() (Event)              → increments count
```

**Current state:** The Student Management System has a reactive UI. The search bar toggles, the Add button disables at the limit, and the search input provides live feedback — all without touching the DOM manually.

---

## Phase 3 Summary

| Binding Type | Syntax | Direction | Use Case |
|---|---|---|---|
| Interpolation | `{{ expr }}` | Class → Template | Display text values |
| Property Binding | `[prop]="expr"` | Class → Template | Set element properties, attributes, classes, styles |
| Event Binding | `(event)="fn()"` | Template → Class | Respond to user interactions |
| Two-Way Binding | `[(ngModel)]="prop"` | Class ↔ Template | Form inputs that stay in sync |

**Key rules:**
- `FormsModule` must be imported to use `[(ngModel)]`
- Use `[attr.name]` for HTML attributes (not DOM properties) like `aria-*` and `colspan`
- `$event` gives you the native DOM event in an event handler
- Angular's Change Detection keeps the DOM in sync automatically after every event

---

## What's Next

In **Phase 4 — Dynamic UI**, you will replace the three hardcoded `<app-student-card>` tags with a real TypeScript array of students and render them dynamically using Angular's modern control flow syntax:
- `@for` — loop over a collection and render a component for each item
- `@if` / `@else` — show or hide elements based on conditions
- Attribute Directives — `[class.active]`, `[ngClass]`, `[ngStyle]`

