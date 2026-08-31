# Phase 11 — Async Programming: Practice Exercises

**Prerequisites:** Completed Phase 11 lesson on RxJS and Async Patterns  
**Application Context:** Student Management System  
**Folder:** `student-management/src/app/`

---

## Exercise 1: Simulated Async Service with Observables

**Objective:** Replace the synchronous signal-based service with Observable-returning methods that simulate network delay using `of()` and `delay()`.

### Steps

**1. Add Observable methods alongside the existing signal methods in `StudentService`:**

```typescript
// student.service.ts — add these imports and methods
import { Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';

// Simulated async GET — returns after 400ms
getAll$(): Observable<Student[]> {
  return of([...this._students()]).pipe(delay(400));
}

getById$(id: number): Observable<Student | undefined> {
  return of(this._students().find(s => s.id === id)).pipe(delay(200));
}

search$(query: string): Observable<Student[]> {
  const q = query.toLowerCase();
  return of(this._students().filter(s => s.name.toLowerCase().includes(q)))
    .pipe(delay(300));
}
```

**2. Build `AsyncStudentListComponent` that subscribes manually:**

```bash
ng g c components/async-student-list
```

```typescript
// async-student-list.component.ts
import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { StudentService } from '../../services/student.service';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-async-student-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './async-student-list.component.html',
})
export class AsyncStudentListComponent implements OnInit, OnDestroy {
  private svc = inject(StudentService);
  private sub!: Subscription;

  students: Student[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    this.loading = true;
    this.sub = this.svc.getAll$().subscribe({
      next:     (data) => { this.students = data; this.loading = false; },
      error:    (err)  => { this.error = 'Failed to load students'; this.loading = false; },
      complete: ()     => console.log('Stream complete'),
    });
  }

  ngOnDestroy(): void {
    // Always unsubscribe to prevent memory leaks
    this.sub.unsubscribe();
  }
}
```

```html
<!-- async-student-list.component.html -->
@if (loading) {
  <div class="loading-pulse">Loading students…</div>
}

@if (error) {
  <p class="error-banner">{{ error }}</p>
}

@if (!loading && !error) {
  @for (student of students; track student.id) {
    <div class="row">{{ student.name }} · {{ student.course }}</div>
  } @empty {
    <p>No students found.</p>
  }
}
```

### What to Verify
- On page load there is a 400 ms delay during which "Loading students…" is visible.
- After the delay the list renders.
- Open the Network tab — no real HTTP request fires; the delay is simulated with `of()`.
- Navigating away and back does not leave dangling subscriptions (check the console).

---

## Exercise 2: Live Search with `debounceTime` and `switchMap`

**Objective:** Wire a search input to an Observable pipeline using `debounceTime`, `distinctUntilChanged`, and `switchMap` to avoid unnecessary calls.

### Steps

**1. Generate `SearchBarComponent`:**

```bash
ng g c components/search-bar
```

**2. Implement the reactive search:**

```typescript
// search-bar.component.ts
import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { StudentService } from '../../services/student.service';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './search-bar.component.html',
})
export class SearchBarComponent implements OnInit, OnDestroy {
  private svc     = inject(StudentService);
  private destroy$ = new Subject<void>();

  searchControl = new FormControl('');
  results: Student[] = [];
  searching = false;

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),           // Wait 300ms after the user stops typing
      distinctUntilChanged(),      // Skip if value didn't actually change
      switchMap(query => {         // Cancel previous in-flight request
        this.searching = true;
        return this.svc.search$(query ?? '');
      }),
      takeUntil(this.destroy$)     // Auto-unsubscribe on destroy
    ).subscribe(data => {
      this.results = data;
      this.searching = false;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

```html
<!-- search-bar.component.html -->
<div class="search-container">
  <input [formControl]="searchControl" placeholder="Search students…" class="search-input" />

  @if (searching) {
    <span class="searching-indicator">Searching…</span>
  }

  @for (student of results; track student.id) {
    <div class="result-item">
      <strong>{{ student.name }}</strong>
      <span>{{ student.course }} · Year {{ student.year }}</span>
    </div>
  } @empty {
    @if (!searching && searchControl.value) {
      <p class="no-results">No students match "{{ searchControl.value }}"</p>
    }
  }
</div>
```

**3. Add styles:**

```css
.search-container { position: relative; max-width: 480px; }
.search-input { width: 100%; padding: 10px 14px; border: 1px solid #ccc; border-radius: 8px; font-size: 1rem; }
.searching-indicator { font-size: .8rem; color: #888; margin: 6px 0; display: block; }
.result-item { display: flex; justify-content: space-between; padding: 10px 14px; border-bottom: 1px solid #eee; }
.result-item strong { color: #1976d2; }
.no-results { color: #888; font-style: italic; padding: 10px; }
```

### What to Verify
- Typing quickly does not fire `search$()` on every keystroke — only after a 300 ms pause.
- Typing the same query twice in a row (backspace + retype) does not fire a duplicate search.
- Results update after the simulated 300 ms delay.
- `takeUntil(this.destroy$)` pattern means no subscription leak when the component unmounts.

---

## Exercise 3: AsyncPipe and BehaviorSubject

**Objective:** Replace manual subscribe/unsubscribe with `AsyncPipe` and use `BehaviorSubject` to expose the student list as a stream.

### Steps

**1. Add a `BehaviorSubject` stream to `StudentService`:**

```typescript
import { BehaviorSubject } from 'rxjs';

// Inside StudentService
private _studentsSubject = new BehaviorSubject<Student[]>(this._students());

// Expose as Observable
readonly students$ = this._studentsSubject.asObservable();

// Call this whenever the signal updates — add to add(), update(), delete()
private syncSubject(): void {
  this._studentsSubject.next([...this._students()]);
}

// Update add(), update(), delete() to call syncSubject() after the signal update
add(data: Omit<Student, 'id'>): void {
  this._students.update(list => [...list, { id: this.nextId++, ...data }]);
  this.syncSubject();
}

delete(id: number): void {
  this._students.update(list => list.filter(s => s.id !== id));
  this.syncSubject();
}
```

**2. Build a component that uses `AsyncPipe` — zero subscriptions in the class:**

```typescript
// async-pipe-demo.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { StudentService } from '../../services/student.service';

@Component({
  selector: 'app-async-pipe-demo',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  template: `
    @if (svc.students$ | async; as students) {
      <p>{{ students.length }} students loaded</p>
      @for (s of students; track s.id) {
        <div class="row">
          {{ s.name }} — {{ s.course }}
          <button (click)="svc.delete(s.id)">×</button>
        </div>
      }
    } @else {
      <p>Loading…</p>
    }
  `,
  styles: [`.row{display:flex;justify-content:space-between;padding:8px 12px;border-bottom:1px solid #eee}`]
})
export class AsyncPipeDemoComponent {
  svc = inject(StudentService);
  // No ngOnInit, no Subscription, no ngOnDestroy needed — AsyncPipe handles it all
}
```

### What to Verify
- The component renders the student list without a single `.subscribe()` call in TypeScript.
- Calling `svc.delete(id)` from the template updates the view instantly because `BehaviorSubject` emits a new value.
- Opening DevTools memory profiler shows no Observable leaks after navigating away.

### Reflection Questions
1. Why does `BehaviorSubject` emit immediately to new subscribers while a plain `Subject` does not?
2. When would you choose `AsyncPipe` over manual subscription?
3. What is the risk of using `switchMap` without `takeUntil` or `AsyncPipe`?

