# Phase 3 — Data Binding

## Where We Left Off

After Phase 2, we have two components:
- `HeaderComponent` — static title
- `StudentListComponent` — **hardcoded** HTML table rows

In this phase, we replace the hardcoded values with **TypeScript data** using the four types of data binding.

> **Note:** We still show students as static rows for now.
> In Phase 4, we will loop through the array using `@for`.

---

## The Four Types of Data Binding

| Type | Syntax | Direction |
|---|---|---|
| Interpolation | `{{ value }}` | TypeScript → HTML |
| Property Binding | `[property]="value"` | TypeScript → HTML |
| Event Binding | `(event)="method()"` | HTML → TypeScript |
| Two-Way Binding | `[(ngModel)]="value"` | Both directions |

---

## 1. Interpolation — `{{ }}`

Displays a TypeScript value in the HTML template.

### Update `student-list.component.ts`

Add two properties:

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  pageTitle = 'Students';
  totalStudents = 2;
}
```

### Update `student-list.component.html`

Replace the `<h2>` and add the count:

```html
<div class="student-list">
  <h2>{{ pageTitle }}</h2>
  <p>Total: {{ totalStudents }}</p>

  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Email</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>1</td>
        <td>Alice</td>
        <td>alice@example.com</td>
      </tr>
      <tr>
        <td>2</td>
        <td>Bob</td>
        <td>bob@example.com</td>
      </tr>
    </tbody>
  </table>
</div>
```

`{{ pageTitle }}` and `{{ totalStudents }}` are read from the TypeScript class and inserted into the HTML.

---

## 2. Property Binding — `[property]`

Binds a **TypeScript value** to an **HTML attribute or DOM property**.

Syntax: `[attribute]="typescriptExpression"`

### Update `student-list.component.ts`

Add a property to control the button:

```typescript
export class StudentListComponent {
  pageTitle = 'Students';
  totalStudents = 2;
  isAddDisabled = false;
}
```

### Update `student-list.component.html`

Add a button with property binding:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
    <button [disabled]="isAddDisabled">Add Student</button>
  </div>
  <p>Total: {{ totalStudents }}</p>

  <table>
    <!-- same table as before -->
  </table>
</div>
```

`[disabled]="isAddDisabled"` — Angular reads `isAddDisabled` from the class and sets the button's `disabled` property.

If you change `isAddDisabled = true`, the button becomes disabled automatically.

> **Difference from interpolation:**
> - `{{ value }}` — inserts text into the HTML
> - `[attr]="value"` — sets an HTML property/attribute

---

## 3. Event Binding — `(event)`

Listens for a user action (like a click) and calls a TypeScript method.

Syntax: `(event)="methodName()"`

### Update `student-list.component.ts`

Add a method:

```typescript
export class StudentListComponent {
  pageTitle = 'Students';
  totalStudents = 2;
  isAddDisabled = false;
  message = '';

  onAddClick() {
    this.message = 'Add Student feature coming soon!';
  }
}
```

### Update `student-list.component.html`

Bind the click event and show the message:

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
    <button [disabled]="isAddDisabled" (click)="onAddClick()">Add Student</button>
  </div>
  <p>Total: {{ totalStudents }}</p>
  <p>{{ message }}</p>

  <table>
    <!-- same table as before -->
  </table>
</div>
```

When the user clicks the button, Angular calls `onAddClick()`, which sets `message`. Angular then updates `{{ message }}` in the HTML automatically.

---

## 4. Two-Way Binding — `[(ngModel)]`

Keeps a TypeScript property and an HTML input **in sync** — changes in the input update TypeScript, and changes in TypeScript update the input.

This is used for **forms and search inputs**.

### Step 1 — Import FormsModule

`ngModel` requires `FormsModule`. Import it in the component:

```typescript
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.css'
})
export class StudentListComponent {
  pageTitle = 'Students';
  totalStudents = 2;
  isAddDisabled = false;
  message = '';
  searchTerm = '';

  onAddClick() {
    this.message = 'Add Student feature coming soon!';
  }
}
```

### Step 2 — Add the search input

```html
<div class="student-list">
  <div class="list-header">
    <h2>{{ pageTitle }}</h2>
    <button [disabled]="isAddDisabled" (click)="onAddClick()">Add Student</button>
  </div>
  <p>Total: {{ totalStudents }}</p>
  <p>{{ message }}</p>

  <div class="search-bar">
    <input type="text" [(ngModel)]="searchTerm" placeholder="Search students..." />
    <p>You typed: {{ searchTerm }}</p>
  </div>

  <table>
    <!-- same table as before -->
  </table>
</div>
```

As you type in the input, `{{ searchTerm }}` updates **instantly** — no button click needed.

> In Phase 4, we will use `searchTerm` to actually filter the student list.

---

## 5. Add a Little CSS

### `student-list.component.css` — add these rules

```css
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 4px;
}

button:disabled {
  background-color: #aaa;
  cursor: not-allowed;
}

.search-bar {
  margin: 16px 0;
}

.search-bar input {
  padding: 8px;
  width: 300px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
```

---

## 6. Run the App

```bash
ng serve
```

Test each binding type:
- ✅ `pageTitle` and `totalStudents` display via interpolation
- ✅ Button is enabled/disabled based on `isAddDisabled`
- ✅ Clicking button shows the message
- ✅ Typing in the search box updates `{{ searchTerm }}` live

---

## Phase 3 Summary

| Binding | Syntax | Example |
|---|---|---|
| Interpolation | `{{ value }}` | `{{ pageTitle }}` |
| Property | `[attr]="value"` | `[disabled]="isAddDisabled"` |
| Event | `(event)="method()"` | `(click)="onAddClick()"` |
| Two-Way | `[(ngModel)]="value"` | `[(ngModel)]="searchTerm"` |

---

## Application State After Phase 3

```
✅ HeaderComponent — unchanged
✅ StudentListComponent:
    - pageTitle via interpolation
    - totalStudents count
    - Add button with property + event binding
    - Search input with two-way binding
```

**Next → Phase 4: Dynamic UI**
We will use `@for` to loop through a student array and display rows dynamically,
and `@if` to show/hide content based on conditions.
