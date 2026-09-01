# Phase 5 — Pipes

## Where We Left Off

After Phase 4, the student table shows dynamic data from a TypeScript array. The data is correct but the formatting is plain — names and emails are shown exactly as stored.

In this phase, we will **format** how the data looks using **pipes**.

---

## 1. What is a Pipe?

A pipe **transforms a value** in the template before displaying it.

Syntax:

```html
{{ value | pipeName }}
```

It does NOT change the actual data — only how it is displayed.

Think of it like a Java method that formats output — for example, `String.format()` or `DateTimeFormatter`.

---

## 2. Built-in Pipes

Angular comes with several ready-to-use pipes:

| Pipe | What it does | Example |
|---|---|---|
| `uppercase` | ALL CAPS | `alice` → `ALICE` |
| `lowercase` | all lowercase | `ALICE` → `alice` |
| `titlecase` | First Letter Caps | `alice johnson` → `Alice Johnson` |
| `date` | Formats a date | `2024-01-15` → `Jan 15, 2024` |
| `slice` | Cuts a string/array | `'Hello World' \| slice:0:5` → `Hello` |
| `currency` | Formats as money | `1500` → `$1,500.00` |

---

## 3. Add `enrolledDate` to the Student Model

We'll add a date field so we can demonstrate the `date` pipe.

### `src/app/student.model.ts`

```typescript
export interface Student {
  id: number;
  name: string;
  email: string;
  enrolledDate: string;
}
```

### Update the students array in `student-list.component.ts`

```typescript
students: Student[] = [
  { id: 1, name: 'alice johnson', email: 'alice@example.com', enrolledDate: '2024-01-15' },
  { id: 2, name: 'bob smith',     email: 'bob@example.com',   enrolledDate: '2024-03-22' },
  { id: 3, name: 'charlie brown', email: 'charlie@example.com', enrolledDate: '2024-06-10' },
];
```

> Names are stored in lowercase on purpose — so we can see `titlecase` pipe at work.

---

## 4. Use Built-in Pipes in the Template

### `src/app/student-list/student-list.component.ts`

Import the pipes you need:

```typescript
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgClass, TitleCasePipe, DatePipe } from '@angular/common';
import { Student } from '../student.model';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule, NgClass, TitleCasePipe, DatePipe],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  pageTitle = 'Students';
  searchTerm = '';
  message = '';

  students: Student[] = [
    { id: 1, name: 'alice johnson', email: 'alice@example.com', enrolledDate: '2024-01-15' },
    { id: 2, name: 'bob smith',     email: 'bob@example.com',   enrolledDate: '2024-03-22' },
    { id: 3, name: 'charlie brown', email: 'charlie@example.com', enrolledDate: '2024-06-10' },
  ];

  get filteredStudents(): Student[] {
    if (!this.searchTerm) return this.students;
    return this.students.filter(s =>
      s.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  onAddClick() {
    this.message = 'Add Student feature coming soon!';
  }
}
```

### `src/app/student-list/student-list.component.html`

Update the table to add the new column and pipes:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
    <button (click)="onAddClick()">Add Student</button>
  </div>

  @if (message) {
    <p class="message">{{ message }}</p>
  }

  <div class="search-bar">
    <input type="text" [(ngModel)]="searchTerm" placeholder="Search by name..." />
  </div>

  <p>Showing {{ filteredStudents.length }} of {{ students.length }} students</p>

  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Email</th>
        <th>Enrolled</th>
      </tr>
    </thead>
    <tbody>
      @for (student of filteredStudents; track student.id) {
        <tr [ngClass]="{ 'highlight': student.id === 1 }">
          <td>{{ student.id }}</td>
          <td>{{ student.name | titlecase }}</td>
          <td>{{ student.email | lowercase }}</td>
          <td>{{ student.enrolledDate | date: 'mediumDate' }}</td>
        </tr>
      }
      @if (filteredStudents.length === 0) {
        <tr>
          <td colspan="4">No students found.</td>
        </tr>
      }
    </tbody>
  </table>
</div>
```

### What changed

| Column | Pipe used | Result |
|---|---|---|
| Name | `titlecase` | `alice johnson` → `Alice Johnson` |
| Email | `lowercase` | ensures always lowercase |
| Enrolled | `date: 'mediumDate'` | `2024-01-15` → `Jan 15, 2024` |

---

## 5. Custom Pipe — `initials`

Sometimes built-in pipes are not enough. You can create your own.

We will create an `initials` pipe that takes a full name and returns the person's initials.

Example: `"Alice Johnson"` → `"AJ"`

### Generate the pipe

```bash
ng generate pipe initials
```

This creates `src/app/initials.pipe.ts`.

### `src/app/initials.pipe.ts`

```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'initials',
  standalone: true
})
export class InitialsPipe implements PipeTransform {

  transform(name: string): string {
    return name
      .split(' ')
      .map(word => word[0].toUpperCase())
      .join('');
  }

}
```

### How it works

1. Split the name by spaces → `['alice', 'johnson']`
2. Take the first character of each word → `['a', 'j']`
3. Uppercase and join → `'AJ'`

---

## 6. Use the Custom Pipe

### Import it in `student-list.component.ts`

```typescript
import { InitialsPipe } from '../initials.pipe';

@Component({
  ...
  imports: [FormsModule, NgClass, TitleCasePipe, DatePipe, InitialsPipe],
  ...
})
```

### Add a column to `student-list.component.html`

```html
<thead>
  <tr>
    <th>ID</th>
    <th>Name</th>
    <th>Initials</th>
    <th>Email</th>
    <th>Enrolled</th>
  </tr>
</thead>
<tbody>
  @for (student of filteredStudents; track student.id) {
    <tr [ngClass]="{ 'highlight': student.id === 1 }">
      <td>{{ student.id }}</td>
      <td>{{ student.name | titlecase }}</td>
      <td>{{ student.name | initials }}</td>
      <td>{{ student.email | lowercase }}</td>
      <td>{{ student.enrolledDate | date: 'mediumDate' }}</td>
    </tr>
  }
</tbody>
```

Result:

| ID | Name | Initials | Email | Enrolled |
|---|---|---|---|---|
| 1 | Alice Johnson | AJ | alice@example.com | Jan 15, 2024 |
| 2 | Bob Smith | BS | bob@example.com | Mar 22, 2024 |

---

## 7. Run the App

```bash
ng serve
```

Check:
- ✅ Names display as Title Case
- ✅ Enrolled dates are formatted (e.g. `Jan 15, 2024`)
- ✅ Initials column shows `AJ`, `BS`, `CB`
- ✅ Search still works

---

## Phase 5 Summary

| Concept | What You Learned |
|---|---|
| Pipe | Transforms a value in the template |
| Syntax | `{{ value \| pipeName }}` |
| With argument | `{{ date \| date: 'mediumDate' }}` |
| Built-in pipes | `titlecase`, `lowercase`, `date`, etc. |
| Custom pipe | `@Pipe` decorator + `PipeTransform` interface |
| `ng g pipe` | CLI command to generate a pipe |

---

## Application State After Phase 5

```
✅ Student model updated with enrolledDate
✅ Built-in pipes: titlecase, lowercase, date
✅ Custom initials pipe
✅ Table shows formatted, readable student data
```

**Next → Phase 6: Component Communication**
We will pass data between components using `@Input()` and `@Output()` / `EventEmitter`.
