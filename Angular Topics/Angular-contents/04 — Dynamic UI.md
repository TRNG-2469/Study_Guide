# Phase 4 — Dynamic UI

## What You Will Learn

In Phase 3 you had three hardcoded `<app-student-card>` tags in your template. Real applications work with collections of data — an array of students fetched from a database. In this phase you will:

- Define a proper `Student` TypeScript interface
- Create an array of students in `StudentListComponent`
- Use Angular's modern control flow (`@for`, `@if`, `@else`, `@switch`) to render the list dynamically
- Use attribute directives (`ngClass`, `ngStyle`) to apply styles conditionally
- Understand the legacy structural directives (`*ngFor`, `*ngIf`) you will encounter in older code

By the end, clicking a student card will highlight it and the list will react dynamically to the data.

---

## 1. Defining a TypeScript Interface for Student

Before building the dynamic list, define the shape of a student using a TypeScript **interface**. This gives you type safety across the entire application.

Create a new file: `src/app/models/student.model.ts`

You can create the `models` folder manually or run:

```bash
mkdir src/app/models
```

Then create the file:

### `src/app/models/student.model.ts`

```typescript
export interface Student {
  id: number;
  name: string;
  course: string;
  year: number;
  email: string;
  status: 'active' | 'inactive' | 'graduated';
}
```

**Why an interface?** You already know TypeScript — an interface defines a contract. Every object typed as `Student` must have exactly these properties with these types. Angular's template will benefit from autocomplete and compile-time checking when you work with `Student` objects.

The `status` field uses a **union type** — it can only be one of the three string literals. This prevents typos like `'actve'` from slipping through.

---

## 2. Modern Angular Control Flow

Angular v17 introduced a new **built-in control flow** syntax using `@` blocks directly in templates. These replace the older structural directives and are more readable, better performing, and require no imports.

### `@for` — Looping Over a Collection

```html
@for (item of collection; track item.id) {
  <div>{{ item.name }}</div>
}
```

The `track` expression is **required**. It tells Angular how to uniquely identify each item so it can efficiently update the DOM when the list changes (add, remove, reorder). Always track by a unique ID — never `track $index` unless the list never reorders.

**Loop variables available inside `@for`:**

| Variable | Type | Description |
|---|---|---|
| `$index` | `number` | Current item's position (0-based) |
| `$count` | `number` | Total number of items |
| `$first` | `boolean` | `true` for the first item |
| `$last` | `boolean` | `true` for the last item |
| `$even` | `boolean` | `true` for even-indexed items |
| `$odd` | `boolean` | `true` for odd-indexed items |

```html
@for (student of students; track student.id) {
  <div [class.last-item]="$last">
    {{ $index + 1 }}. {{ student.name }}
  </div>
}
```

#### The `@empty` Block

`@for` supports an `@empty` block that renders when the collection is empty:

```html
@for (student of students; track student.id) {
  <app-student-card></app-student-card>
} @empty {
  <p class="empty-message">No students found.</p>
}
```

### `@if` / `@else if` / `@else` — Conditional Rendering

```html
@if (students.length > 0) {
  <p>Showing {{ students.length }} students</p>
} @else if (isLoading) {
  <p>Loading...</p>
} @else {
  <p>No students found.</p>
}
```

Angular adds and removes the DOM elements entirely — it does not just hide them with CSS. This means conditional content does not take up space or consume resources when hidden.

### `@switch` — Matching One Value Against Cases

```html
@switch (student.status) {
  @case ('active') {
    <span class="badge badge-active">Active</span>
  }
  @case ('inactive') {
    <span class="badge badge-inactive">Inactive</span>
  }
  @case ('graduated') {
    <span class="badge badge-graduated">Graduated</span>
  }
  @default {
    <span class="badge">Unknown</span>
  }
}
```

---

## 3. Attribute Directives

While control flow adds or removes DOM elements, **attribute directives** change the appearance or behaviour of elements that are already in the DOM.

### `[ngClass]` — Conditionally Apply CSS Classes

`ngClass` accepts an object where each key is a CSS class name and each value is a boolean expression:

```html
<!-- Apply 'selected' class when isSelected is true -->
<div [ngClass]="{ 'selected': isSelected, 'highlighted': isHighlighted }">
```

For a single class you can use the simpler shorthand:

```html
<div [class.selected]="isSelected">
```

### `[ngStyle]` — Conditionally Apply Inline Styles

```html
<div [ngStyle]="{ 'background-color': student.colour, 'font-size': fontSize + 'px' }">
```

For a single style:

```html
<div [style.color]="isActive ? 'green' : 'red'">
```

> **Best practice:** Prefer CSS classes over inline styles. Use `[ngStyle]` only when the style value is truly dynamic (e.g. a colour value from the database). Keeping styles in CSS files makes them easier to maintain and override.

Both `ngClass` and `ngStyle` are part of `@angular/common` and must be imported:

```typescript
import { NgClass, NgStyle } from '@angular/common';

@Component({
  imports: [NgClass, NgStyle]
})
```

---

## 4. Legacy Structural Directives — `*ngFor` and `*ngIf`

You will encounter these constantly in older Angular codebases, tutorials, and Stack Overflow answers. They do the same job as `@for` and `@if` but with a different syntax. They are still supported in modern Angular — they are not removed.

### `*ngFor`

```html
<div *ngFor="let student of students; let i = index; trackBy: trackById">
  {{ i + 1 }}. {{ student.name }}
</div>
```

The `trackBy` function must be defined in the TypeScript class:

```typescript
trackById(index: number, student: Student): number {
  return student.id;
}
```

### `*ngIf`

```html
<p *ngIf="students.length > 0; else emptyBlock">
  Showing {{ students.length }} students
</p>

<ng-template #emptyBlock>
  <p>No students found.</p>
</ng-template>
```

### When to Use Which

| Situation | Use |
|---|---|
| Writing new code | `@for`, `@if`, `@switch` (modern) |
| Reading or maintaining existing code | Understand `*ngFor`, `*ngIf` |
| Team mixes old and new code | Both work; avoid mixing in the same component |

This course uses modern syntax throughout.

---

## 5. Updating the Application — Dynamic Student List

### Step 1 — Update `StudentCardComponent` to Accept a Student

Update `src/app/student-card/student-card.component.ts`:

```typescript
import { Component, Input } from '@angular/core';
import { NgClass, NgStyle } from '@angular/common';
import { Student } from '../models/student.model';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [NgClass, NgStyle],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {
  @Input() student!: Student;
  isSelected = false;

  toggleSelect(): void {
    this.isSelected = !this.isSelected;
  }
}
```

> **`@Input()`** is from Phase 6 (Component Communication), but you need it here to pass student data from parent to child. For now, understand that `@Input()` makes a property receivable from a parent component via its template. You will learn it fully in Phase 6.

### Step 2 — Update `StudentCardComponent` Template

Replace `src/app/student-card/student-card.component.html`:

```html
<div
  class="student-card"
  [ngClass]="{ 'selected': isSelected }"
  (click)="toggleSelect()">

  <div class="card-header">
    <div class="avatar" [ngStyle]="{ 'background-color': getAvatarColour() }">
      {{ student.name[0] }}
    </div>
    <div class="student-info">
      <h3>{{ student.name }}</h3>
      <span class="student-id">STU-{{ student.id.toString().padStart(3, '0') }}</span>
    </div>
  </div>

  <div class="card-body">
    <div class="detail-row">
      <span class="label">Course</span>
      <span class="value">{{ student.course }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Year</span>
      <span class="value">Year {{ student.year }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Email</span>
      <span class="value">{{ student.email }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Status</span>
      <span class="value">
        @switch (student.status) {
          @case ('active') {
            <span class="badge badge-active">● Active</span>
          }
          @case ('inactive') {
            <span class="badge badge-inactive">● Inactive</span>
          }
          @case ('graduated') {
            <span class="badge badge-graduated">✓ Graduated</span>
          }
        }
      </span>
    </div>
  </div>

  <div class="card-footer">
    @if (isSelected) {
      <span class="selected-indicator">✔ Selected</span>
    }
    <button class="btn btn-view" (click)="$event.stopPropagation()">View</button>
    <button class="btn btn-edit" (click)="$event.stopPropagation()">Edit</button>
  </div>

</div>
```

**Note:** `$event.stopPropagation()` prevents the button click from also triggering the card's `(click)="toggleSelect()"`. Without it, clicking Edit would both open the edit flow and toggle the selected state.

Add the `getAvatarColour()` method to `StudentCardComponent`:

```typescript
getAvatarColour(): string {
  const colours = ['#1a73e8', '#e8710a', '#1e8e3e', '#d93025', '#7627bb'];
  return colours[this.student.id % colours.length];
}
```

This cycles through five colours based on the student ID, giving each avatar a consistent colour.

### Step 3 — Update `StudentCardComponent` CSS

Add these new rules to `src/app/student-card/student-card.component.css`:

```css
/* Add to existing CSS */

.student-card {
  cursor: pointer;
}

.student-card.selected {
  border: 2px solid #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.15);
}

.badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.badge-active {
  background-color: #e6f4ea;
  color: #1e8e3e;
}

.badge-inactive {
  background-color: #fce8e6;
  color: #d93025;
}

.badge-graduated {
  background-color: #e8f0fe;
  color: #1a73e8;
}

.selected-indicator {
  font-size: 13px;
  color: #1a73e8;
  font-weight: 500;
  flex: 1;
}

.card-footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background-color: #fafafa;
  border-top: 1px solid #f0f0f0;
  align-items: center;
}
```

### Step 4 — Update `StudentListComponent`

Replace `src/app/student-list/student-list.component.ts`:

```typescript
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StudentCardComponent } from '../student-card/student-card.component';
import { Student } from '../models/student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [StudentCardComponent, FormsModule],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  sectionTitle = 'All Students';
  searchTerm = '';
  showSearch = false;

  students: Student[] = [
    { id: 1, name: 'Alice Johnson',  course: 'Computer Science', year: 2, email: 'alice@uni.edu',   status: 'active'    },
    { id: 2, name: 'Bob Martinez',   course: 'Mathematics',      year: 3, email: 'bob@uni.edu',     status: 'active'    },
    { id: 3, name: 'Carol Williams', course: 'Physics',          year: 1, email: 'carol@uni.edu',   status: 'inactive'  },
    { id: 4, name: 'David Chen',     course: 'Computer Science', year: 4, email: 'david@uni.edu',   status: 'graduated' },
    { id: 5, name: 'Emma Davis',     course: 'Engineering',      year: 2, email: 'emma@uni.edu',    status: 'active'    },
  ];

  get filteredStudents(): Student[] {
    if (!this.searchTerm.trim()) {
      return this.students;
    }
    const term = this.searchTerm.toLowerCase();
    return this.students.filter(s =>
      s.name.toLowerCase().includes(term) ||
      s.course.toLowerCase().includes(term)
    );
  }

  get activeCount(): number {
    return this.students.filter(s => s.status === 'active').length;
  }

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) {
      this.searchTerm = '';
    }
  }
}
```

**What is a `get` property?** A TypeScript getter looks like a method but is accessed like a property in the template — `{{ filteredStudents.length }}` not `{{ filteredStudents().length }}`. Angular recalculates it automatically whenever Change Detection runs.

### Step 5 — Update `StudentListComponent` Template

Replace `src/app/student-list/student-list.component.html`:

```html
<section class="student-list-section">

  <!-- Header -->
  <div class="section-header">
    <div class="title-area">
      <h2>{{ sectionTitle }}</h2>
      <span class="count-badge">{{ students.length }} total · {{ activeCount }} active</span>
    </div>
    <div class="action-buttons">
      <button class="btn-search" (click)="toggleSearch()">
        {{ showSearch ? '✕ Close' : '🔍 Search' }}
      </button>
      <button class="btn-add">+ Add Student</button>
    </div>
  </div>

  <!-- Search bar -->
  <div class="search-area" [class.visible]="showSearch">
    <input
      type="text"
      [(ngModel)]="searchTerm"
      placeholder="Search by name or course..."
      class="search-input">
  </div>

  <!-- Results summary -->
  @if (searchTerm) {
    <p class="results-summary">
      @if (filteredStudents.length > 0) {
        Showing {{ filteredStudents.length }} result(s) for "<strong>{{ searchTerm }}</strong>"
      } @else {
        No students match "<strong>{{ searchTerm }}</strong>"
      }
    </p>
  }

  <!-- Student grid -->
  <div class="student-grid">
    @for (student of filteredStudents; track student.id) {
      <app-student-card [student]="student"></app-student-card>
    } @empty {
      <div class="empty-state">
        <p>🎓 No students found.</p>
        <p>Add some students or clear your search.</p>
      </div>
    }
  </div>

</section>
```

**Key points in this template:**
- `@for (student of filteredStudents; track student.id)` — loops over the filtered array
- `[student]="student"` — passes each student object into `StudentCardComponent` via `@Input()`
- `@empty` — shows a message when the filtered list is empty
- Nested `@if` / `@else` inside the results summary

Add to `src/app/student-list/student-list.component.css`:

```css
/* Add to existing CSS */

.results-summary {
  margin: 0 0 16px;
  font-size: 14px;
  color: #555;
}

.empty-state {
  grid-column: 1 / -1;   /* spans all grid columns */
  text-align: center;
  padding: 48px 20px;
  color: #888;
}

.empty-state p {
  margin: 4px 0;
  font-size: 16px;
}
```

---

## 6. Seeing the Dynamic List in Action

Open **http://localhost:4200**. You should see:

1. **Five student cards** in a responsive grid, each showing real data
2. **Coloured avatars** — each student gets a consistent colour based on their ID
3. **Status badges** — Active (green), Inactive (red), Graduated (blue), driven by `@switch`
4. **Click a card** — it highlights with a blue border (ngClass applying the `selected` class)
5. **Open the search bar and type** — the grid filters live as you type, the results summary updates
6. **Search for something with no match** — the `@empty` block displays

---

## 7. The Application So Far

```
Student array (TypeScript)
  → filteredStudents getter (filtered by searchTerm)
    → @for loop in template
      → <app-student-card [student]="student"> for each item
        → @switch renders the correct status badge
        → [ngClass] applies 'selected' on click
        → [ngStyle] applies avatar colour
```

**Current state:** A fully dynamic student list. The UI responds to data — loop, filter, conditional badges, selection highlighting — all driven by TypeScript values.

---

## Phase 4 Summary

| Concept | Syntax | What It Does |
|---|---|---|
| `@for` | `@for (item of list; track item.id) { }` | Loop over a collection |
| `@empty` | `} @empty { }` | Renders when the `@for` collection is empty |
| `@if` / `@else` | `@if (cond) { } @else { }` | Conditionally add/remove DOM elements |
| `@switch` | `@switch (val) { @case ('x') { } }` | Match one value against multiple cases |
| `[ngClass]` | `[ngClass]="{ 'cls': bool }"` | Add/remove CSS classes conditionally |
| `[class.name]` | `[class.selected]="isSelected"` | Shorthand for a single class |
| `[ngStyle]` | `[ngStyle]="{ 'color': val }"` | Apply inline styles conditionally |
| `*ngFor` / `*ngIf` | Legacy equivalents | Still work; common in older code |
| TypeScript `get` | `get filtered(): T[] { }` | Computed property — used like a field in templates |

---

## What's Next

In **Phase 5 — Data Presentation**, you will learn about Angular **Pipes** — a way to transform displayed values directly in the template. You will format student names to title case, display dates, format numbers, and build a custom `CourseBadgePipe` that transforms a course string into a coloured tag.

