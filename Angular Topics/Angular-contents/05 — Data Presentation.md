# Phase 5 — Data Presentation

## What You Will Learn

Your Student Management System now renders a dynamic list from real data. But raw data rarely looks good — names need consistent capitalisation, dates need formatting, numbers need commas. In this phase you will learn **Pipes** — Angular's mechanism for transforming displayed values in templates.

By the end of this phase:
- Student names are formatted in title case
- Enrolment dates are displayed in a readable format
- A custom `CourseBadgePipe` transforms a plain course name into a visual tag
- A custom `EnrolmentYearPipe` converts a year number to a human-readable label

---

## 1. What is a Pipe?

A **pipe** transforms a value for display in the template. The value in your TypeScript class stays unchanged — only the rendered output is different.

```html
{{ value | pipeName }}
{{ value | pipeName : argument1 : argument2 }}
```

The `|` character is the pipe operator. You chain a value on the left into a transformation on the right.

### Analogy to Spring

If you have used Spring's `@JsonSerialize` or custom Jackson serialisers, pipes serve a similar purpose — they control *how a value is presented* without changing the stored data. A pipe is purely a display concern.

---

## 2. Built-in Angular Pipes

Angular ships with a set of ready-to-use pipes in `@angular/common`. You import only the ones you need.

### String Pipes

```html
{{ 'hello world' | uppercase }}    <!-- HELLO WORLD -->
{{ 'HELLO WORLD' | lowercase }}    <!-- hello world -->
{{ 'alice johnson' | titlecase }}  <!-- Alice Johnson -->
```

Import: `UpperCasePipe`, `LowerCasePipe`, `TitleCasePipe`

### Number Pipe

```typescript
{{ 1234567.89 | number }}             // 1,234,567.89
{{ 1234567.89 | number:'1.0-0' }}     // 1,234,568
{{ 0.75 | number:'1.2-2' }}           // 0.75
```

The format string is `'minIntegerDigits.minFractionDigits-maxFractionDigits'`.

Import: `DecimalPipe`

### Currency Pipe

```html
{{ 9999.99 | currency }}              <!-- $9,999.99 -->
{{ 9999.99 | currency:'EUR' }}        <!-- €9,999.99 -->
{{ 9999.99 | currency:'GBP':'symbol':'1.0-0' }}  <!-- £10,000 -->
```

Import: `CurrencyPipe`

### Percent Pipe

```html
{{ 0.85 | percent }}       <!-- 85% -->
{{ 0.85 | percent:'1.1' }} <!-- 85.0% -->
```

Import: `PercentPipe`

### Date Pipe

The Date pipe is the most powerful built-in pipe:

```html
{{ dateValue | date }}                     <!-- Aug 29, 2026 -->
{{ dateValue | date:'fullDate' }}          <!-- Saturday, August 29, 2026 -->
{{ dateValue | date:'shortDate' }}         <!-- 8/29/26 -->
{{ dateValue | date:'dd/MM/yyyy' }}        <!-- 29/08/2026 -->
{{ dateValue | date:'MMM yyyy' }}          <!-- Aug 2026 -->
{{ dateValue | date:'h:mm a' }}            <!-- 3:45 PM -->
```

The `dateValue` can be a `Date` object, an ISO string, or a Unix timestamp (milliseconds).

Import: `DatePipe`

### Slice Pipe

```html
<!-- Take first 3 items from an array -->
@for (student of students | slice:0:3; track student.id) { ... }

<!-- Take first 20 characters of a string -->
{{ longText | slice:0:20 }}...
```

Import: `SlicePipe`

### JSON Pipe (Debugging)

Extremely useful during development to inspect an object in the template:

```html
<pre>{{ student | json }}</pre>
```

This renders the object as formatted JSON. Remove it before deploying — it is a debugging tool only.

Import: `JsonPipe`

### AsyncPipe

Used with Observables and Promises — you will use this extensively in Phase 11 and 12 when connecting to your Spring REST APIs. It automatically subscribes and unsubscribes for you.

```html
{{ observable$ | async }}
```

---

## 3. Using Built-in Pipes — Updating the Student Model

First, add an `enrolmentDate` field to the `Student` interface:

### Updated `src/app/models/student.model.ts`

```typescript
export interface Student {
  id: number;
  name: string;
  course: string;
  year: number;
  email: string;
  status: 'active' | 'inactive' | 'graduated';
  enrolmentDate: Date;
}
```

Update the student array in `StudentListComponent` to include dates:

```typescript
students: Student[] = [
  { id: 1, name: 'alice johnson',  course: 'Computer Science', year: 2, email: 'alice@uni.edu',  status: 'active',    enrolmentDate: new Date('2023-09-01') },
  { id: 2, name: 'bob martinez',   course: 'Mathematics',      year: 3, email: 'bob@uni.edu',    status: 'active',    enrolmentDate: new Date('2022-09-01') },
  { id: 3, name: 'carol williams', course: 'Physics',          year: 1, email: 'carol@uni.edu',  status: 'inactive',  enrolmentDate: new Date('2024-09-01') },
  { id: 4, name: 'david chen',     course: 'Computer Science', year: 4, email: 'david@uni.edu',  status: 'graduated', enrolmentDate: new Date('2021-09-01') },
  { id: 5, name: 'emma davis',     course: 'Engineering',      year: 2, email: 'emma@uni.edu',   status: 'active',    enrolmentDate: new Date('2023-09-01') },
];
```

Notice the names are all **lowercase** — you will use the `titlecase` pipe to fix the display without changing the data.

---

## 4. Applying Built-in Pipes in `StudentCardComponent`

### Updated `src/app/student-card/student-card.component.ts`

```typescript
import { Component, Input } from '@angular/core';
import { NgClass, NgStyle, TitleCasePipe, DatePipe } from '@angular/common';
import { Student } from '../models/student.model';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [NgClass, NgStyle, TitleCasePipe, DatePipe],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {
  @Input() student!: Student;
  isSelected = false;

  toggleSelect(): void {
    this.isSelected = !this.isSelected;
  }

  getAvatarColour(): string {
    const colours = ['#1a73e8', '#e8710a', '#1e8e3e', '#d93025', '#7627bb'];
    return colours[this.student.id % colours.length];
  }
}
```

### Updated `src/app/student-card/student-card.component.html`

```html
<div
  class="student-card"
  [ngClass]="{ 'selected': isSelected }"
  (click)="toggleSelect()">

  <div class="card-header">
    <div class="avatar" [ngStyle]="{ 'background-color': getAvatarColour() }">
      {{ student.name[0] | uppercase }}
    </div>
    <div class="student-info">
      <!-- titlecase pipe: 'alice johnson' → 'Alice Johnson' -->
      <h3>{{ student.name | titlecase }}</h3>
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
      <!-- lowercase pipe ensures email is always lowercase -->
      <span class="value email">{{ student.email | lowercase }}</span>
    </div>
    <div class="detail-row">
      <span class="label">Enrolled</span>
      <!-- date pipe: formats the Date object as 'MMM yyyy' -->
      <span class="value">{{ student.enrolmentDate | date:'MMM yyyy' }}</span>
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

Add to `student-card.component.css`:

```css
.email {
  font-size: 12px;
  color: #555;
}
```

---

## 5. Building a Custom Pipe

Built-in pipes cover common formatting needs. For application-specific transformations you build **custom pipes**.

### Creating a Pipe with the CLI

```bash
ng generate pipe pipes/course-badge
ng generate pipe pipes/enrolment-year
```

This creates `src/app/pipes/course-badge.pipe.ts` and `src/app/pipes/enrolment-year.pipe.ts`.

### Anatomy of a Custom Pipe

```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'pipeName',    // The name used in the template: {{ value | pipeName }}
  standalone: true,
})
export class MyPipe implements PipeTransform {
  transform(value: any, ...args: any[]): any {
    // Transform and return the value
    return value;
  }
}
```

The `PipeTransform` interface requires a single `transform()` method. The first parameter is the input value; additional parameters come from template arguments after the colon (`{{ value | pipe : arg1 : arg2 }}`).

---

## 6. `CourseBadgePipe` — Course Name to Colour Tag

This pipe maps a course name to a CSS class suffix so the template can apply course-specific badge colours.

### `src/app/pipes/course-badge.pipe.ts`

```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'courseBadge',
  standalone: true,
})
export class CourseBadgePipe implements PipeTransform {
  private readonly courseColours: Record<string, string> = {
    'Computer Science': 'cs',
    'Mathematics':      'math',
    'Physics':          'physics',
    'Engineering':      'eng',
    'Biology':          'bio',
  };

  transform(course: string): string {
    return this.courseColours[course] ?? 'default';
  }
}
```

The pipe returns a short string key (`'cs'`, `'math'`, etc.) that maps to a CSS class. The template uses it to build the class name.

---

## 7. `EnrolmentYearPipe` — Year Number to Label

This pipe converts a year number (1, 2, 3, 4) into a label like `'1st Year'`, `'2nd Year'`, etc.

### `src/app/pipes/enrolment-year.pipe.ts`

```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'enrolmentYear',
  standalone: true,
})
export class EnrolmentYearPipe implements PipeTransform {
  transform(year: number): string {
    const suffixes: Record<number, string> = {
      1: '1st Year',
      2: '2nd Year',
      3: '3rd Year',
      4: '4th Year',
    };
    return suffixes[year] ?? `Year ${year}`;
  }
}
```

---

## 8. Using Custom Pipes in `StudentCardComponent`

### Updated imports in `student-card.component.ts`

```typescript
import { Component, Input } from '@angular/core';
import { NgClass, NgStyle, TitleCasePipe, DatePipe, LowerCasePipe, UpperCasePipe } from '@angular/common';
import { Student } from '../models/student.model';
import { CourseBadgePipe } from '../pipes/course-badge.pipe';
import { EnrolmentYearPipe } from '../pipes/enrolment-year.pipe';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [NgClass, NgStyle, TitleCasePipe, DatePipe, LowerCasePipe, UpperCasePipe, CourseBadgePipe, EnrolmentYearPipe],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {
  @Input() student!: Student;
  isSelected = false;

  toggleSelect(): void {
    this.isSelected = !this.isSelected;
  }

  getAvatarColour(): string {
    const colours = ['#1a73e8', '#e8710a', '#1e8e3e', '#d93025', '#7627bb'];
    return colours[this.student.id % colours.length];
  }
}
```

### Updated template — applying the custom pipes

In `student-card.component.html`, update the Course and Year rows:

```html
<div class="detail-row">
  <span class="label">Course</span>
  <!-- courseBadge pipe returns a CSS key; use it to build the class name -->
  <span class="value course-badge course-{{ student.course | courseBadge }}">
    {{ student.course }}
  </span>
</div>
<div class="detail-row">
  <span class="label">Year</span>
  <!-- enrolmentYear pipe transforms 2 → '2nd Year' -->
  <span class="value">{{ student.year | enrolmentYear }}</span>
</div>
```

### Add course badge styles to `student-card.component.css`

```css
.course-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.course-cs      { background-color: #e8f0fe; color: #1a73e8; }
.course-math    { background-color: #fef7e0; color: #b06000; }
.course-physics { background-color: #e6f4ea; color: #1e8e3e; }
.course-eng     { background-color: #fce8e6; color: #d93025; }
.course-bio     { background-color: #f3e8fd; color: #7627bb; }
.course-default { background-color: #f1f3f4; color: #444; }
```

---

## 9. Chaining Pipes

You can chain multiple pipes on the same value:

```html
<!-- First titlecase, then slice to first 15 characters -->
{{ student.name | titlecase | slice:0:15 }}

<!-- Uppercase then show only if truthy -->
{{ student.course | uppercase }}
```

Pipes are applied left to right — the output of each becomes the input of the next.

---

## 10. Pure vs. Impure Pipes

By default all Angular pipes are **pure**. Angular only re-runs a pure pipe when its input value *reference* changes — not when the internal contents of an object or array mutate. This makes pipes very efficient.

An **impure** pipe runs on every Change Detection cycle regardless:

```typescript
@Pipe({
  name: 'myPipe',
  pure: false,    // impure — use sparingly
})
```

You will rarely need impure pipes and should avoid them for performance reasons. If you find yourself needing one, it usually signals that the transformation logic belongs in a component property or a service method instead.

---

## 11. The Application So Far

**Pipes applied in `StudentCardComponent`:**

| Value | Pipe | Result |
|---|---|---|
| `'alice johnson'` | `titlecase` | `'Alice Johnson'` |
| `'ALICE@UNI.EDU'` | `lowercase` | `'alice@uni.edu'` |
| `'a'` | `uppercase` | `'A'` (avatar initial) |
| `new Date('2023-09-01')` | `date:'MMM yyyy'` | `'Sep 2023'` |
| `'Computer Science'` | `courseBadge` | `'cs'` → class `course-cs` |
| `2` | `enrolmentYear` | `'2nd Year'` |

**Current state:** Student cards display cleanly formatted, visually rich data. Course names appear as colour-coded badges. Year numbers read as natural English labels. All formatting is done in the template — the TypeScript data is unchanged.

---

## Phase 5 Summary

| Concept | What You Learned |
|---|---|
| Pipe syntax | `{{ value \| pipeName : arg }}` — transforms values for display only |
| `titlecase`, `uppercase`, `lowercase` | String capitalisation pipes |
| `date` | Flexible date formatting with named and custom format strings |
| `number`, `currency`, `percent` | Numeric formatting with precision control |
| `slice` | Substring or sub-array extraction |
| `json` | Debugging tool — renders objects as formatted JSON |
| `async` | Handles Observables/Promises — covered in Phase 11 |
| Custom Pipe | `ng g pipe` + implement `PipeTransform.transform()` |
| Pipe chaining | `{{ val \| pipe1 \| pipe2 }}` — applied left to right |
| Pure vs. Impure | Default (pure) = efficient; impure = runs every cycle, avoid |

---

## What's Next

In **Phase 6 — Component Communication**, you will learn the formal Angular mechanism for passing data between components using `@Input()` and `@Output()` with `EventEmitter`. You will properly set up the parent-child relationship between `StudentListComponent` and `StudentCardComponent` so that:
- The parent passes a student object **down** to each card via `@Input()`
- The card emits a `selected` event **up** to the parent via `@Output()` and `EventEmitter`
- The parent tracks which student is selected and highlights it

