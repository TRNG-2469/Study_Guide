# Phase 10 — Angular Internals and Reactivity: Practice Exercises

**Prerequisites:** Completed Phase 10 lesson on Lifecycle Hooks, Signals, and Change Detection  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Lifecycle Hook Logger

**Objective:** Implement `ngOnInit`, `ngOnChanges`, `ngOnDestroy`, and `ngAfterViewInit` in `StudentCardComponent` to observe when Angular fires each hook.

### Steps

**1. Update `StudentCardComponent` to implement all four hooks:**

```typescript
// student-card.component.ts
import {
  Component, Input, Output, EventEmitter,
  OnInit, OnChanges, OnDestroy, AfterViewInit,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './student-card.component.html',
})
export class StudentCardComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {
  @Input({ required: true }) student!: Student;
  @Input() isSelected = false;
  @Output() studentSelected = new EventEmitter<Student>();

  renderCount = 0;

  ngOnChanges(changes: SimpleChanges): void {
    // Fires BEFORE ngOnInit and every time an @Input changes
    if (changes['student']) {
      const prev = changes['student'].previousValue;
      const curr = changes['student'].currentValue;
      console.log(`[ngOnChanges] student changed:`, prev?.name, '→', curr?.name);
    }
    if (changes['isSelected']) {
      console.log(`[ngOnChanges] isSelected → ${changes['isSelected'].currentValue}`);
    }
  }

  ngOnInit(): void {
    // Fires once after the first ngOnChanges
    console.log(`[ngOnInit] Card initialised for: ${this.student.name}`);
    this.renderCount = 1;
  }

  ngAfterViewInit(): void {
    // Fires after the component's view (and child views) are rendered
    console.log(`[ngAfterViewInit] DOM ready for: ${this.student.name}`);
  }

  ngOnDestroy(): void {
    // Fires just before Angular removes this component from the DOM
    console.log(`[ngOnDestroy] Card destroyed for: ${this.student.name}`);
  }
}
```

**2. Add a counter to the card template:**

```html
<!-- student-card.component.html -->
<div class="card" [class.selected]="isSelected" (click)="studentSelected.emit(student)">
  <h4>{{ student.name }}</h4>
  <p>{{ student.course }} · Year {{ student.year }}</p>
  <small class="render-count">Renders: {{ renderCount }}</small>
</div>
```

**3. Test the lifecycle by toggling visibility in the parent:**

```html
<!-- student-list.component.html — add a toggle -->
<button (click)="showCards = !showCards">
  {{ showCards ? 'Hide Cards' : 'Show Cards' }}
</button>

@if (showCards) {
  @for (student of students; track student.id) {
    <app-student-card [student]="student" [isSelected]="selectedId === student.id"
                      (studentSelected)="selectedId = $event.id" />
  }
}
```

```typescript
showCards = true;
selectedId: number | null = null;
```

### What to Verify
- Open the browser console. Click **Hide Cards** and observe `[ngOnDestroy]` for each card.
- Click **Show Cards** and observe `[ngOnInit]` and `[ngAfterViewInit]` for each card.
- Click a card to select it and observe `[ngOnChanges]` firing on the selected card (isSelected flips).
- Note that `ngOnChanges` fires before `ngOnInit` on first load.

### Reflection Questions
1. Why should you never access the DOM directly in `ngOnInit`? (Hint: view is not yet rendered.)
2. Where is the correct place to start a timer or subscribe to an Observable — `ngOnInit` or `ngAfterViewInit`?

---

## Exercise 2: Refactor `StudentService` to Use Signals

**Objective:** Replace the plain array in `StudentService` with a `signal`, expose readonly and `computed` signals, and observe reactive updates in the template.

### Steps

**1. Refactor `StudentService` with signals:**

```typescript
// student.service.ts
import { Injectable, signal, computed } from '@angular/core';
import { Student } from '../models/student.model';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private _students = signal<Student[]>([
    { id: 1, name: 'Aanya Sharma', course: 'CSE', year: 3, status: 'active',   gpa: 8.76 },
    { id: 2, name: 'Rohan Mehta',  course: 'ECE', year: 1, status: 'inactive', gpa: 7.30 },
    { id: 3, name: 'Priya Nair',   course: 'CSE', year: 2, status: 'active',   gpa: 9.12 },
    { id: 4, name: 'Karan Singh',  course: 'MBA', year: 4, status: 'pending',  gpa: 8.00 },
    { id: 5, name: 'Divya Pillai', course: 'ECE', year: 2, status: 'inactive', gpa: 6.89 },
  ]);
  private nextId = 6;

  // Public read-only view — consumers cannot call .set() directly
  readonly students = this._students.asReadonly();

  // Derived signals — auto-update when _students changes
  readonly totalCount  = computed(() => this._students().length);
  readonly activeCount = computed(() => this._students().filter(s => s.status === 'active').length);
  readonly averageGpa  = computed(() => {
    const list = this._students();
    if (list.length === 0) return 0;
    return list.reduce((sum, s) => sum + s.gpa, 0) / list.length;
  });

  getById(id: number): Student | undefined {
    return this._students().find(s => s.id === id);
  }

  add(data: Omit<Student, 'id'>): void {
    this._students.update(list => [...list, { id: this.nextId++, ...data }]);
  }

  update(id: number, changes: Partial<Omit<Student, 'id'>>): void {
    this._students.update(list =>
      list.map(s => s.id === id ? { ...s, ...changes } : s)
    );
  }

  delete(id: number): void {
    this._students.update(list => list.filter(s => s.id !== id));
  }
}
```

**2. Consume signals in `StudentListComponent` (no `ngOnInit` needed):**

```typescript
// student-list.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student.service';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './student-list.component.html',
})
export class StudentListComponent {
  svc = inject(StudentService);

  // No local copy needed — template reads directly from the signal
  deleteStudent(id: number): void { this.svc.delete(id); }
}
```

```html
<!-- student-list.component.html -->
<p>Total: {{ svc.totalCount() }} · Active: {{ svc.activeCount() }} · Avg GPA: {{ svc.averageGpa() | number:'1.2-2' }}</p>

@for (student of svc.students(); track student.id) {
  <div class="row">
    {{ student.name }} · {{ student.course }}
    <button (click)="deleteStudent(student.id)">Remove</button>
  </div>
}
```

**3. Consume the same signals in `NavbarComponent` — they auto-sync:**

```html
<span class="badge">{{ svc.activeCount() }} active</span>
```

### What to Verify
- Deleting a student instantly updates the count in the navbar without any manual refresh call.
- `totalCount`, `activeCount`, and `averageGpa` all recalculate automatically.
- You no longer call `getAll()` or `refresh()` anywhere — the signal is the source of truth.

---

## Exercise 3: `effect()` — Log Every State Change

**Objective:** Use `effect()` to run a side effect whenever the students signal changes, simulating an audit log.

### Steps

**1. Add an `effect` inside `StudentListComponent`:**

```typescript
import { Component, inject, effect } from '@angular/core';
import { StudentService } from '../../services/student.service';

export class StudentListComponent {
  svc = inject(StudentService);

  auditLog: string[] = [];

  constructor() {
    // effect() runs once immediately, then re-runs whenever svc.students() changes
    effect(() => {
      const count = this.svc.totalCount();
      const entry = `[${new Date().toLocaleTimeString()}] Student count changed to ${count}`;
      // Note: read the signal inside effect — this establishes the dependency
      this.auditLog = [...this.auditLog, entry];
      console.log(entry);
    });
  }

  deleteStudent(id: number): void { this.svc.delete(id); }
}
```

**2. Display the audit log in the template:**

```html
<h3>Audit Log</h3>
<ul class="audit-log">
  @for (entry of auditLog; track $index) {
    <li>{{ entry }}</li>
  }
</ul>
```

### What to Verify
- On page load the effect fires once and logs the initial count.
- Each deletion adds a new timestamped entry to the audit log.
- The log grows with each change — `effect()` never fires when no signal it reads has changed.

### Challenge
Add a `computed()` signal called `topStudent` that returns the student with the highest GPA. Display their name in the navbar as "🏆 Top: {{ svc.topStudent()?.name }}".

