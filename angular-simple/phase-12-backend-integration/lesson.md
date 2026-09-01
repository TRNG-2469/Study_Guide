# Phase 12 — Backend Integration

## Where We Left Off

After Phase 11, `StudentService` returns `Observable<Student[]>` using `of()` to simulate HTTP.

In this phase, we replace `of()` with real **`HttpClient`** calls to a Spring REST API backed by PostgreSQL.

The components do **not change** — only the service changes.

---

## The Full Stack Picture

```
Browser (Angular)
    ↕ HTTP (JSON)
Spring Boot REST API  (localhost:8080)
    ↕ JPA
PostgreSQL Database
```

---

## Part 1 — Spring Boot Backend

You already know how to build this. Here is what the backend needs.

### Student Entity

```java
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String enrolledDate;

    // getters and setters
}
```

### StudentRepository

```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {}
```

### StudentController

```java
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "http://localhost:4200")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping
    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getById(@PathVariable Long id) {
        return studentRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Student create(@RequestBody Student student) {
        return studentRepository.save(student);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> update(@PathVariable Long id, @RequestBody Student student) {
        if (!studentRepository.existsById(id)) return ResponseEntity.notFound().build();
        student.setId(id);
        return ResponseEntity.ok(studentRepository.save(student));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!studentRepository.existsById(id)) return ResponseEntity.notFound().build();
        studentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/studentdb
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.port=8080
```

> `@CrossOrigin(origins = "http://localhost:4200")` — allows Angular (running on port 4200) to call this API. Without this, the browser blocks the request.

Start the Spring app and verify with Postman:
```
GET  http://localhost:8080/api/students
POST http://localhost:8080/api/students
```

---

## Part 2 — Angular Setup

### Step 1 — Enable HttpClient

### `src/app/app.config.ts`

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
  ]
};
```

`provideHttpClient()` registers Angular's HTTP module — required before using `HttpClient`.

---

### Step 2 — Environment File

Store the API base URL in an environment file so it's easy to change between dev and production.

### `src/environments/environment.ts` ← create this file

```typescript
export const environment = {
  apiUrl: 'http://localhost:8080/api'
};
```

---

## Part 3 — Update StudentService

This is the **only major change** in Phase 12. Replace the entire service.

### `src/app/student.service.ts`

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { Student } from './student.model';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StudentService {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/students`;

  getStudents(): Observable<Student[]> {
    return this.http.get<Student[]>(this.apiUrl).pipe(
      catchError(this.handleError)
    );
  }

  getStudentById(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  addStudent(student: Omit<Student, 'id'>): Observable<Student> {
    return this.http.post<Student>(this.apiUrl, student).pipe(
      catchError(this.handleError)
    );
  }

  updateStudent(id: number, student: Omit<Student, 'id'>): Observable<Student> {
    return this.http.put<Student>(`${this.apiUrl}/${id}`, student).pipe(
      catchError(this.handleError)
    );
  }

  deleteStudent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: any): Observable<never> {
    console.error('API Error:', error);
    return throwError(() => new Error(error.message || 'Something went wrong'));
  }

}
```

### What changed

| Before (Phase 11) | After (Phase 12) |
|---|---|
| `of(this.students)` | `this.http.get<Student[]>(url)` |
| `private students: Student[]` | No local array — data lives in the database |
| Synchronous mutations | Every operation returns `Observable` |
| `delay(300)` simulation | Real HTTP latency |

---

## Part 4 — Update Components

### StudentListComponent

`getStudents()` now returns `Observable` and we already subscribe correctly from Phase 11.

The only change: `deleteStudent()` now also returns an Observable, so we subscribe to it:

```typescript
onStudentDeleted(id: number) {
  this.studentService.deleteStudent(id).subscribe({
    next: () => {
      this.students = this.students.filter(s => s.id !== id);
      if (this.selectedStudent?.id === id) this.selectedStudent = null;
      this.toastMessage.set('Student deleted.');
    },
    error: (err) => {
      this.toastMessage.set('Failed to delete student.');
    }
  });
}
```

---

### StudentDetailComponent

`getStudentById()` now returns `Observable<Student>`. Update to subscribe:

```typescript
ngOnInit() {
  const id = Number(this.route.snapshot.paramMap.get('id'));
  this.studentService.getStudentById(id).subscribe({
    next: (data) => this.student = data,
    error: () => this.router.navigate(['/'])
  });
}
```

Update the property type:

```typescript
student: Student | undefined;
```

---

### StudentFormComponent

`addStudent()` and `updateStudent()` now return Observables. Subscribe on submit:

```typescript
onSubmit() {
  if (this.form.invalid) return;

  const data = this.form.value as Omit<Student, 'id'>;

  if (this.isEditMode && this.studentId !== null) {
    this.studentService.updateStudent(this.studentId, data).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => console.error('Update failed', err)
    });
  } else {
    this.studentService.addStudent(data).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => console.error('Add failed', err)
    });
  }
}
```

---

### studentExistsGuard

The guard used `getStudentById()` synchronously. Now it returns `Observable<Student>`.

Update the guard to handle async:

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StudentService } from './student.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export const studentExistsGuard: CanActivateFn = (route) => {
  const studentService = inject(StudentService);
  const router = inject(Router);
  const id = Number(route.paramMap.get('id'));

  return studentService.getStudentById(id).pipe(
    map(() => true),
    catchError(() => {
      router.navigate(['/']);
      return of(false);
    })
  );
};
```

---

## Part 5 — Error Handling in the Template

Add an `errorMessage` property to `StudentListComponent`:

```typescript
errorMessage = '';
```

Update `ngOnInit` subscribe:

```typescript
ngOnInit() {
  this.subscription = this.studentService.getStudents().subscribe({
    next: (data) => {
      this.students = data;
      this.isLoading = false;
    },
    error: (err) => {
      this.errorMessage = 'Could not load students. Is the server running?';
      this.isLoading = false;
    }
  });
}
```

Add to template:

```html
@if (isLoading) {
  <p>Loading students...</p>
} @else if (errorMessage) {
  <p class="error-message">{{ errorMessage }}</p>
} @else {
  <!-- student cards -->
}
```

---

## Part 6 — Run the Full Stack

### Start PostgreSQL

Make sure your PostgreSQL server is running and `studentdb` database exists.

### Start Spring Boot

```bash
./mvnw spring-boot:run
```

Verify: `GET http://localhost:8080/api/students` returns JSON.

### Start Angular

```bash
ng serve
```

### Test Full CRUD

| Action | What happens |
|---|---|
| Load list | `GET /api/students` |
| Click View | `GET /api/students/1` |
| Click + Add Student, submit | `POST /api/students` |
| Click Edit, submit | `PUT /api/students/1` |
| Click Delete | `DELETE /api/students/1` |

Open **Network tab** in browser DevTools to see the real HTTP requests.

---

## Phase 12 Summary

| Concept | What You Learned |
|---|---|
| `provideHttpClient()` | Register HttpClient in the app |
| `HttpClient` | Make HTTP GET, POST, PUT, DELETE requests |
| `environment.ts` | Store config (API URL) separately from code |
| `catchError` | Handle errors in an Observable pipe |
| `@CrossOrigin` | Allow Angular to call the Spring API |
| Async guard | Route guard that waits for an Observable |

---

## 🎯 Complete Full Stack Application

```
✅ Angular Frontend
    - Components, routing, forms, services
    - Reactive forms with validation
    - Signals and lifecycle hooks
    ↕ HTTP (JSON)
✅ Spring Boot REST API
    - @RestController, @CrossOrigin
    - Full CRUD endpoints
    ↕ JPA
✅ PostgreSQL Database
    - Persistent student records
```

### What the students built — phase by phase

| Phase | What was added |
|---|---|
| 1 | Angular project, root component |
| 2 | Header and StudentList components |
| 3 | Data binding (interpolation, events, two-way) |
| 4 | @for, @if, dynamic student list |
| 5 | Pipes — titlecase, date, custom initials |
| 6 | @Input / @Output — StudentCard component |
| 7 | StudentService — centralized data and delete |
| 8 | Routing — list page, detail page, route guard |
| 9 | Reactive forms — add and edit student |
| 10 | Lifecycle hooks, Signals, toast notification |
| 11 | Observables, subscribe, loading state |
| 12 | HttpClient — real Spring + PostgreSQL backend |
