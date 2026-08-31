# Phase 4 — Dynamic UI: Practice Exercises

**Prerequisites:** Completed Phase 4 lesson on Dynamic UI  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Student Status Board with `@for` and `@if`

**Objective:** Practice modern control flow (`@for`, `@if`, `@else`, `@empty`) to render a status board.

**Scenario:** Build a board that groups students by status and highlights empty groups with a friendly message.

### Steps

**1. Update `StudentListComponent` with grouped data:**

```typescript
students = [
  { id: 1, name: 'Aanya Sharma', course: 'CSE', year: 3, status: 'active'   },
  { id: 2, name: 'Rohan Mehta',  course: 'ECE', year: 1, status: 'inactive' },
  { id: 3, name: 'Priya Nair',   course: 'CSE', year: 2, status: 'active'   },
  { id: 4, name: 'Karan Singh',  course: 'MBA', year: 4, status: 'pending'  },
  { id: 5, name: 'Divya Pillai', course: 'ECE', year: 2, status: 'inactive' },
];

get activeStudents()   { return this.students.filter(s => s.status === 'active');   }
get inactiveStudents() { return this.students.filter(s => s.status === 'inactive'); }
get pendingStudents()  { return this.students.filter(s => s.status === 'pending');  }
```

**2. Build the status board template:**

```html
<div class="status-board">

  <!-- Active Column -->
  <div class="column active-col">
    <h3>Active ({{ activeStudents.length }})</h3>
    @for (student of activeStudents; track student.id) {
      <div class="student-chip">
        <span class="dot active-dot"></span>
        {{ student.name }} · {{ student.course }}
      </div>
    } @empty {
      <p class="empty-msg">No active students.</p>
    }
  </div>

  <!-- Inactive Column -->
  <div class="column inactive-col">
    <h3>Inactive ({{ inactiveStudents.length }})</h3>
    @for (student of inactiveStudents; track student.id) {
      <div class="student-chip">
        <span class="dot inactive-dot"></span>
        {{ student.name }} · {{ student.course }}
      </div>
    } @empty {
      <p class="empty-msg">No inactive students.</p>
    }
  </div>

  <!-- Pending Column -->
  <div class="column pending-col">
    <h3>Pending ({{ pendingStudents.length }})</h3>
    @for (student of pendingStudents; track student.id) {
      <div class="student-chip">
        <span class="dot pending-dot"></span>
        {{ student.name }} · {{ student.course }}
      </div>
    } @empty {
      <p class="empty-msg">No pending students.</p>
    }
  </div>

</div>

<!-- Detailed view using @if / @else -->
@if (students.length > 0) {
  <p class="summary">Total students enrolled: {{ students.length }}</p>
} @else {
  <p class="summary warning">No students found. Please add students to the system.</p>
}
```

**3. Add column styles:**

```css
.status-board { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin: 20px 0; }
.column { padding: 16px; border-radius: 8px; min-height: 120px; }
.active-col   { background: #e8f5e9; border-left: 4px solid #4caf50; }
.inactive-col { background: #f5f5f5; border-left: 4px solid #9e9e9e; }
.pending-col  { background: #fff8e1; border-left: 4px solid #ffc107; }
.student-chip { background: #fff; padding: 8px 12px; border-radius: 6px; margin: 6px 0; display: flex; align-items: center; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.active-dot   { background: #4caf50; }
.inactive-dot { background: #9e9e9e; }
.pending-dot  { background: #ffc107; }
.empty-msg { color: #aaa; font-style: italic; font-size: .9rem; }
```

### What to Verify
- Each column shows only the students matching that status.
- `@empty` renders when a column has zero students (try clearing all from one status).
- `@if` / `@else` block shows the summary or the warning correctly.

### Challenge
Add a `searchQuery` two-way binding and filter all three groups simultaneously so the `@empty` block fires when the search yields no matches.

---

## Exercise 2: Course Badge Switcher with `@switch`

**Objective:** Use `@switch` to map a course code to a styled badge without nested `@if` chains.

**Scenario:** Each student card should display a colour-coded department badge derived from their course field.

### Steps

**1. Add a helper method to the component:**

```typescript
getBadgeLabel(course: string): string {
  switch (course) {
    case 'CSE': return 'Computer Science';
    case 'ECE': return 'Electronics';
    case 'MBA': return 'Business Admin';
    case 'BCA': return 'Computer Apps';
    default:    return course;
  }
}
```

**2. Use `@switch` in the template inside each student card:**

```html
@for (student of students; track student.id) {
  <div class="student-card">
    <strong>{{ student.name }}</strong>

    <!-- @switch for colour-coded badge -->
    @switch (student.course) {
      @case ('CSE') {
        <span class="badge badge-cse">💻 Computer Science</span>
      }
      @case ('ECE') {
        <span class="badge badge-ece">⚡ Electronics</span>
      }
      @case ('MBA') {
        <span class="badge badge-mba">📊 Business Admin</span>
      }
      @case ('BCA') {
        <span class="badge badge-bca">🖥️ Computer Apps</span>
      }
      @default {
        <span class="badge badge-default">{{ student.course }}</span>
      }
    }

    <small>Year {{ student.year }}</small>
  </div>
}
```

**3. Style the badges:**

```css
.badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: .78rem; font-weight: 600; }
.badge-cse  { background: #e3f2fd; color: #1565c0; }
.badge-ece  { background: #fff3e0; color: #e65100; }
.badge-mba  { background: #f3e5f5; color: #6a1b9a; }
.badge-bca  { background: #e8f5e9; color: #2e7d32; }
.badge-default { background: #eeeeee; color: #555; }
.student-card { padding: 12px 16px; border: 1px solid #ddd; border-radius: 8px; margin: 8px 0; display: flex; align-items: center; gap: 12px; }
```

### What to Verify
- Each student displays the correct colour badge for their course.
- Adding a student with an unknown course falls through to `@default`.
- No nested `@if` is used — only `@switch` / `@case` / `@default`.

### Challenge
Add a course filter dropdown. Bind it with `[(ngModel)]` and filter the `@for` loop so only students of the selected course are shown. Display "All Courses" as the default option.

---

## Exercise 3: Dynamic Row Styling with `NgClass` and `NgStyle`

**Objective:** Apply `NgClass` for status-based row classes and `NgStyle` for year-based progress bar widths.

**Scenario:** The student table should highlight rows by status using CSS classes, and show a visual year-progress bar using inline styles.

### Steps

**1. Extend the student data with a score field:**

```typescript
students = [
  { id: 1, name: 'Aanya Sharma', course: 'CSE', year: 3, status: 'active',   score: 88 },
  { id: 2, name: 'Rohan Mehta',  course: 'ECE', year: 1, status: 'inactive', score: 54 },
  { id: 3, name: 'Priya Nair',   course: 'CSE', year: 2, status: 'active',   score: 76 },
  { id: 4, name: 'Karan Singh',  course: 'MBA', year: 4, status: 'pending',  score: 91 },
  { id: 5, name: 'Divya Pillai', course: 'ECE', year: 2, status: 'inactive', score: 62 },
];

yearPercent(year: number): number {
  return (year / 4) * 100;
}
```

**2. Build the styled table template:**

```html
<table class="students-table">
  <thead>
    <tr>
      <th>Name</th><th>Course</th><th>Year Progress</th><th>Score</th><th>Status</th>
    </tr>
  </thead>
  <tbody>
    @for (student of students; track student.id) {
      <tr [ngClass]="{
            'row-active':   student.status === 'active',
            'row-inactive': student.status === 'inactive',
            'row-pending':  student.status === 'pending'
          }">

        <td>{{ student.name }}</td>
        <td>{{ student.course }}</td>

        <!-- NgStyle: dynamic progress bar -->
        <td>
          <div class="progress-track">
            <div class="progress-fill"
                 [ngStyle]="{
                   'width': yearPercent(student.year) + '%',
                   'background-color': student.year === 4 ? '#4caf50' : '#2196f3'
                 }">
            </div>
          </div>
          <small>Year {{ student.year }} of 4</small>
        </td>

        <!-- NgStyle: score colour -->
        <td [ngStyle]="{ 'color': student.score >= 75 ? '#2e7d32' : '#c62828',
                         'font-weight': 'bold' }">
          {{ student.score }}%
        </td>

        <td>
          <span [ngClass]="'status-badge status-' + student.status">
            {{ student.status | titlecase }}
          </span>
        </td>
      </tr>
    }
  </tbody>
</table>
```

**3. Add the table CSS:**

```css
.students-table { width: 100%; border-collapse: collapse; }
.students-table th, .students-table td { padding: 10px 14px; border-bottom: 1px solid #eee; }
.row-active   { background: #f1f8f1; }
.row-inactive { background: #fafafa; color: #999; }
.row-pending  { background: #fffde7; }
.progress-track { background: #e0e0e0; border-radius: 4px; height: 8px; width: 120px; margin-bottom: 2px; }
.progress-fill  { height: 8px; border-radius: 4px; transition: width .3s ease; }
.status-badge   { padding: 2px 10px; border-radius: 10px; font-size: .8rem; font-weight: 600; }
.status-active   { background: #c8e6c9; color: #1b5e20; }
.status-inactive { background: #eeeeee; color: #616161; }
.status-pending  { background: #fff9c4; color: #f57f17; }
```

### What to Verify
- Active rows have a light green background; inactive rows are grey; pending rows are yellow.
- Year 4 students show a green progress bar; others show blue.
- Scores ≥ 75 appear in dark green; below 75 appear in dark red.

### Challenge
Add a toggle button that switches between showing all students and only active ones, keeping all the `NgClass` / `NgStyle` bindings intact.

