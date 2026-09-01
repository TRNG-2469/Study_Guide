# Phase 7 — Services and Dependency Injection

## Where We Left Off

After Phase 6, the student data (the `students` array) lives inside `StudentListComponent`.

**Problem:** What if another component also needs the student data?  
It would have to duplicate the array — that is bad design.

**Solution:** Move the data into a **Service** — a single place that any component can access.

---

## 1. What is a Service?

A service is a **plain TypeScript class** that holds **shared logic or data**.

Components should focus on the UI. Services handle the data.

| Handles | Where it lives |
|---|---|
| Display logic, user interaction | Component |
| Data, business logic | Service |

You already know this concept from Spring:

| Angular | Spring |
|---|---|
| `@Injectable` Service | `@Service` class |
| Dependency Injection | `@Autowired` |
| `StudentService` | `StudentService` |

Angular's DI system automatically creates one instance of the service and **injects** it wherever you ask for it.

---

## 2. Create StudentService

```bash
ng generate service student
```

This creates `src/app/student.service.ts`.

---

## 3. Implement the Service

### `src/app/student.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { Student } from './student.model';

@Injectable({
  providedIn: 'root'
})
export class StudentService {

  private students: Student[] = [
    { id: 1, name: 'alice johnson', email: 'alice@example.com', enrolledDate: '2024-01-15' },
    { id: 2, name: 'bob smith',     email: 'bob@example.com',   enrolledDate: '2024-03-22' },
    { id: 3, name: 'charlie brown', email: 'charlie@example.com', enrolledDate: '2024-06-10' },
  ];

  getStudents(): Student[] {
    return this.students;
  }

  deleteStudent(id: number): void {
    this.students = this.students.filter(s => s.id !== id);
  }

}
```

### What to notice

| Part | Meaning |
|---|---|
| `@Injectable({ providedIn: 'root' })` | Registers this service with Angular's DI system — available app-wide |
| `private students` | Data is private — components access it only through methods |
| `getStudents()` | Returns the current list |
| `deleteStudent(id)` | Removes a student by id |

> `providedIn: 'root'` means Angular creates **one shared instance** — like a singleton bean in Spring.

---

## 4. Add a Delete Button to StudentCardComponent

### `src/app/student-card/student-card.component.ts`

Add a `deleted` output:

```typescript
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { TitleCasePipe, DatePipe } from '@angular/common';
import { Student } from '../student.model';

@Component({
  selector: 'app-student-card',
  standalone: true,
  imports: [TitleCasePipe, DatePipe],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.css'
})
export class StudentCardComponent {

  @Input() student!: Student;

  @Output() selected = new EventEmitter<Student>();
  @Output() deleted  = new EventEmitter<number>();

  onSelect() {
    this.selected.emit(this.student);
  }

  onDelete() {
    this.deleted.emit(this.student.id);
  }

}
```

### `src/app/student-card/student-card.component.html`

Add a Delete button:

```html
<div class="student-card">
  <div class="card-info">
    <span class="student-id">#{{ student.id }}</span>
    <strong>{{ student.name | titlecase }}</strong>
    <span>{{ student.email }}</span>
    <span class="date">Enrolled: {{ student.enrolledDate | date: 'mediumDate' }}</span>
  </div>
  <div class="card-actions">
    <button class="btn-select" (click)="onSelect()">Select</button>
    <button class="btn-delete" (click)="onDelete()">Delete</button>
  </div>
</div>
```

### Add button styles to `student-card.component.css`

```css
.card-actions {
  display: flex;
  gap: 8px;
}

.btn-select {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-delete {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
}
```

---

## 5. Inject the Service into StudentListComponent

### `src/app/student-list/student-list.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { Student } from '../student.model';
import { StudentService } from '../student.service';
import { StudentCardComponent } from '../student-card/student-card.component';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule, TitleCasePipe, StudentCardComponent],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {

  private studentService = inject(StudentService);

  pageTitle = 'Students';
  searchTerm = '';
  selectedStudent: Student | null = null;

  get students(): Student[] {
    return this.studentService.getStudents();
  }

  get filteredStudents(): Student[] {
    if (!this.searchTerm) return this.students;
    return this.students.filter(s =>
      s.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  onStudentSelected(student: Student) {
    this.selectedStudent = student;
  }

  onStudentDeleted(id: number) {
    this.studentService.deleteStudent(id);
    if (this.selectedStudent?.id === id) {
      this.selectedStudent = null;
    }
  }

}
```

### What changed

| Change | Reason |
|---|---|
| `inject(StudentService)` | Modern Angular DI — inject the service |
| `students` getter calls service | Data no longer lives in the component |
| `onStudentDeleted()` | Delegates to service, clears selection if needed |

> `inject()` is the modern way. The classic way uses the constructor:
> ```typescript
> constructor(private studentService: StudentService) {}
> ```
> Both work. `inject()` is cleaner in modern Angular.

---

## 6. Update the Template

### `src/app/student-list/student-list.component.html`

Add the `(deleted)` event binding:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
  </div>

  <div class="search-bar">
    <input type="text" [(ngModel)]="searchTerm" placeholder="Search by name..." />
  </div>

  <p>Showing {{ filteredStudents.length }} of {{ students.length }} students</p>

  @if (selectedStudent) {
    <div class="selected-banner">
      ✅ Selected: {{ selectedStudent.name | titlecase }}
    </div>
  }

  @for (student of filteredStudents; track student.id) {
    <app-student-card
      [student]="student"
      (selected)="onStudentSelected($event)"
      (deleted)="onStudentDeleted($event)">
    </app-student-card>
  }

  @if (filteredStudents.length === 0) {
    <p>No students found.</p>
  }
</div>
```

Only one new line compared to Phase 6:

```html
(deleted)="onStudentDeleted($event)"
```

---

## 7. Run the App

```bash
ng serve
```

Test:
- ✅ All students still display (now loaded from service)
- ✅ Click **Select** → banner shows selected student
- ✅ Click **Delete** → card disappears from the list
- ✅ Deleting the selected student clears the banner
- ✅ Search still works

---

## Phase 7 Summary

| Concept | What You Learned |
|---|---|
| Service | Class that holds shared data/logic |
| `@Injectable` | Marks a class as injectable |
| `providedIn: 'root'` | One shared instance app-wide |
| `inject()` | Modern way to inject a service |
| Data in service | Components get data through service methods |

---

## Application State After Phase 7

```
✅ StudentService — holds student data, provides getStudents() and deleteStudent()
✅ StudentListComponent — injects service, no longer owns data
✅ StudentCardComponent — emits deleted event with student id
✅ Delete works end-to-end through the service
```

**Next → Phase 8: Navigation**
We will add routing so the app has multiple pages —
a student list page and a student detail page.
