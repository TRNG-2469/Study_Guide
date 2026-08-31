# Phase 5 — Data Presentation: Practice Exercises

**Prerequisites:** Completed Phase 5 lesson on Pipes  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Formatting the Student Table with Built-in Pipes

**Objective:** Apply `TitleCasePipe`, `DatePipe`, `DecimalPipe`, and `UpperCasePipe` to present student data cleanly.

**Scenario:** The student list needs polished formatting — names in title case, enrolment dates formatted, GPA to two decimal places, and course codes in uppercase.

### Steps

**1. Add date and GPA fields to the student data:**

```typescript
// student-list.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './student-list.component.html',
})
export class StudentListComponent {
  students = [
    { id: 1, name: 'aanya sharma', course: 'cse', gpa: 8.756, enrolledOn: new Date('2022-07-15'), active: true  },
    { id: 2, name: 'rohan mehta',  course: 'ece', gpa: 7.3,   enrolledOn: new Date('2023-01-10'), active: false },
    { id: 3, name: 'priya nair',   course: 'cse', gpa: 9.12,  enrolledOn: new Date('2021-08-02'), active: true  },
    { id: 4, name: 'karan singh',  course: 'mba', gpa: 8.0,   enrolledOn: new Date('2020-07-20'), active: true  },
    { id: 5, name: 'divya pillai', course: 'ece', gpa: 6.889, enrolledOn: new Date('2023-07-18'), active: false },
  ];
}
```

**2. Apply built-in pipes in the template:**

```html
<table class="student-table">
  <thead>
    <tr>
      <th>Name</th>
      <th>Course</th>
      <th>GPA</th>
      <th>Enrolled On</th>
      <th>Status</th>
    </tr>
  </thead>
  <tbody>
    @for (student of students; track student.id) {
      <tr>
        <!-- TitleCase: capitalise each word -->
        <td>{{ student.name | titlecase }}</td>

        <!-- UpperCase: course code always uppercase -->
        <td>{{ student.course | uppercase }}</td>

        <!-- Decimal: exactly 2 decimal places -->
        <td>{{ student.gpa | number:'1.2-2' }}</td>

        <!-- Date: readable format -->
        <td>{{ student.enrolledOn | date:'dd MMM yyyy' }}</td>

        <!-- LowerCase for a simple tag -->
        <td>
          <span [class]="'tag tag-' + (student.active ? 'active' : 'inactive')">
            {{ (student.active ? 'ACTIVE' : 'INACTIVE') | lowercase }}
          </span>
        </td>
      </tr>
    }
  </tbody>
</table>

<!-- Currency pipe example -->
<p>Annual Fee: {{ 75000 | currency:'INR':'symbol':'1.0-0' }}</p>

<!-- Percent pipe example -->
<p>Placement Rate: {{ 0.876 | percent:'1.1-1' }}</p>
```

### What to Verify
- Names display in title case even though the raw data is lowercase.
- GPA shows exactly 2 decimal places (e.g. `8.76`, `9.12`).
- Dates render as `15 Jul 2022`, not as a raw timestamp.
- Course codes are uppercased in the template without modifying the component data.

### Challenge
Add a `| date:'EEEE, MMMM d, y'` format to a tooltip (`[title]` binding) on the date cell so hovering shows the full date name (e.g. "Friday, July 15, 2022").

---

## Exercise 2: Custom `GradeLetterPipe`

**Objective:** Build a pure custom pipe that converts a numeric GPA to a letter grade.

**Scenario:** Each student row should display a letter grade (A+, A, B, C, F) derived from their GPA.

### Steps

**1. Generate the pipe:**

```bash
ng g pipe pipes/grade-letter
```

**2. Implement the transformation:**

```typescript
// pipes/grade-letter.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'gradeLetter', standalone: true })
export class GradeLetterPipe implements PipeTransform {
  transform(gpa: number): string {
    if (gpa >= 9.0) return 'A+';
    if (gpa >= 8.0) return 'A';
    if (gpa >= 7.0) return 'B';
    if (gpa >= 6.0) return 'C';
    return 'F';
  }
}
```

**3. Import and use the pipe:**

```typescript
// student-list.component.ts
imports: [CommonModule, GradeLetterPipe],
```

```html
<!-- In the student table row -->
<td>
  <span [class]="'grade grade-' + (student.gpa | gradeLetter | lowercase)">
    {{ student.gpa | gradeLetter }}
  </span>
</td>
```

**4. Style the grade badges:**

```css
.grade { padding: 2px 10px; border-radius: 10px; font-weight: bold; font-size: .85rem; }
.grade-a\+ { background: #c8e6c9; color: #1b5e20; }
.grade-a  { background: #dcedc8; color: #33691e; }
.grade-b  { background: #fff9c4; color: #f57f17; }
.grade-c  { background: #ffe0b2; color: #e65100; }
.grade-f  { background: #ffcdd2; color: #b71c1c; }
```

### What to Verify
- A GPA of 9.12 shows `A+`; 8.76 shows `A`; 7.3 shows `B`; 6.9 shows `C`.
- The CSS class is built by chaining `gradeLetter` → `lowercase` in the template.
- The pipe is pure — Angular does not recalculate it on every change detection cycle.

### Challenge
Add an optional second argument `showGpa: boolean = false` to the pipe so that `student.gpa | gradeLetter:true` outputs `"A+ (9.12)"` and `student.gpa | gradeLetter` outputs just `"A+"`.

---

## Exercise 3: Custom `TimeAgoPipe` with Impure Behaviour

**Objective:** Build an impure pipe that converts an enrolment date into a human-readable "time ago" string that updates as time passes.

**Scenario:** Instead of a fixed formatted date, each row shows "3 years ago", "1 year ago", "6 months ago", etc.

### Steps

**1. Generate the pipe:**

```bash
ng g pipe pipes/time-ago
```

**2. Implement the impure transformation:**

```typescript
// pipes/time-ago.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'timeAgo', standalone: true, pure: false })
export class TimeAgoPipe implements PipeTransform {
  transform(date: Date | string): string {
    const now  = new Date();
    const past = new Date(date);
    const diffMs   = now.getTime() - past.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays < 1)   return 'today';
    if (diffDays < 7)   return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    if (diffDays < 30)  return `${Math.floor(diffDays / 7)} week(s) ago`;
    if (diffDays < 365) return `${Math.floor(diffDays / 30)} month(s) ago`;
    const years = Math.floor(diffDays / 365);
    return `${years} year${years > 1 ? 's' : ''} ago`;
  }
}
```

**3. Use both pipes together in the template:**

```html
<!-- Show formatted date + time-ago side by side -->
<td>
  {{ student.enrolledOn | date:'dd MMM yyyy' }}
  <small class="time-ago">({{ student.enrolledOn | timeAgo }})</small>
</td>
```

**4. Style the time-ago text:**

```css
.time-ago { color: #888; margin-left: 6px; font-size: .8rem; }
```

### Why `pure: false`?
A pure pipe only re-runs when its input reference changes. Because `new Date()` (the current time) changes every second but the `enrolledOn` value stays the same, we mark the pipe `pure: false` so Angular re-evaluates it on every change detection cycle. In production you would throttle this with an interval; for this exercise, the default CD cycle is sufficient.

### What to Verify
- Students enrolled more than a year ago show `"X year(s) ago"`.
- Students enrolled a few months ago show `"X month(s) ago"`.
- Both the formatted date and the time-ago text appear on the same row.

### Challenge
Chain the `timeAgo` pipe with `uppercase` so the output reads `"3 YEARS AGO"`. Then add a toggle button that switches the column between the formatted date view and the time-ago view.

