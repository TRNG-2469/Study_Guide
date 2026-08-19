# Spring REST Controller — Practice Exercises

These three exercises are designed to practice the complete progression:

1. Sample Data
2. GET All
3. GET By ID
4. POST
5. PUT
6. DELETE
7. Service Layer
8. ResponseEntity
9. Custom Exception
10. @ControllerAdvice
11. Generic Exception
12. Validation
13. Lombok

The exercises deliberately use **Employee, Bank Account, and Movie** domains so students transfer the concepts learned from the Student example instead of repeating it.

---

# Exercise 1 — Employee Management REST API

## Scenario

Your company wants a REST API to manage its employees.

Create a Spring Boot application using an **in-memory `List<Employee>`**. Do not use a database.

## Employee Model

```text
Employee
--------
id
name
email
department
designation
salary
```

Example:

```json
{
    "id": 101,
    "name": "Rahul Sharma",
    "email": "rahul@company.com",
    "department": "IT",
    "designation": "Developer",
    "salary": 65000
}
```

## Part A — Sample Data

Create at least **6 employees** with different departments, designations, and salaries.

Suggested data:

```text
101  Rahul Sharma    IT          Developer       65000
102  Priya Singh     HR          Manager         75000
103  Amit Verma      IT          Tester          55000
104  Sneha Patel     Finance     Accountant      60000
105  Arjun Mehta     IT          Manager         90000
106  Neha Gupta      HR          Recruiter       45000
```

## Part B — GET All Employees

Create:

```http
GET /employees
```

Return all employees.

## Part C — GET Employee By ID

Create:

```http
GET /employees/{id}
```

Example:

```http
GET /employees/103
```

Return employee `103`.

For a non-existing employee, initially you may return `null`.

> Do not implement custom exceptions yet.

## Part D — POST Employee

Create:

```http
POST /employees
```

Accept:

```json
{
    "id": 107,
    "name": "Karan Joshi",
    "email": "karan@company.com",
    "department": "IT",
    "designation": "Developer",
    "salary": 70000
}
```

Add the employee to the list.

## Part E — PUT Employee

Create:

```http
PUT /employees/{id}
```

Example:

```http
PUT /employees/103
```

Request body:

```json
{
    "name": "Amit Verma",
    "email": "amit@company.com",
    "department": "IT",
    "designation": "Senior Tester",
    "salary": 70000
}
```

Update the existing employee.

## Part F — DELETE Employee

Create:

```http
DELETE /employees/{id}
```

Example:

```http
DELETE /employees/106
```

Remove the employee from the list.

---

## Employee Search Requirements

### Part G — Find By Email

Create:

```http
GET /employees/email/{email}
```

Example:

```http
GET /employees/email/rahul@company.com
```

Find an employee using **email instead of ID**.

Use:

```java
@PathVariable
```

### Part H — Find By Department

Create:

```http
GET /employees?department=IT
```

Use:

```java
@RequestParam
```

Return all employees in the requested department.

### Part I — Find By Designation

Create:

```http
GET /employees?designation=Manager
```

Return all managers.

### Part J — Salary Range

Create:

```http
GET /employees?minSalary=50000&maxSalary=80000
```

Return employees whose salary is between the two values.

Use `@RequestParam` for both parameters.

---

## Part K — Service Layer

Move all employee business logic into:

```text
EmployeeService
```

Architecture:

```text
EmployeeController
        ↓
EmployeeService
        ↓
List<Employee>
```

### Part L — ResponseEntity

Use appropriate HTTP status codes:

```text
GET existing employee     → 200 OK
GET missing employee      → 404 NOT FOUND
POST employee             → 201 CREATED
PUT existing employee     → 200 OK
DELETE existing employee  → 204 NO CONTENT
```

### Part M — Exception Handling

Create:

```text
EmployeeNotFoundException
```

Use:

```java
@ControllerAdvice
```

to handle it.

Also add a generic:

```java
@ExceptionHandler(Exception.class)
```

for unexpected errors.

### Part N — Validation

Add validation rules:

```text
name        → required
email       → required + valid email
department  → required
designation → required
salary      → greater than 0
```

Use appropriate validation annotations.

Also handle validation errors through `@ControllerAdvice`.

### Part O — Lombok

Finally, replace boilerplate in `Employee` with Lombok.

Use appropriate annotations such as:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
```

---

# Exercise 2 — Bank Account REST API

## Scenario

Create a REST API for managing bank accounts.

Use an in-memory list.

## Account Model

```text
Account
-------
id
accountNumber
customerName
customerEmail
accountType
branch
balance
```

`accountType` can be:

```text
SAVINGS
CURRENT
```

## Part A — Sample Data

Create at least 6 accounts.

Suggested data:

```text
1  ACC1001  Rahul Sharma   SAVINGS  Bhopal   50000
2  ACC1002  Priya Singh    CURRENT  Indore   125000
3  ACC1003  Amit Verma     SAVINGS  Bhopal   75000
4  ACC1004  Sneha Patel    CURRENT  Delhi    200000
5  ACC1005  Arjun Mehta    SAVINGS  Mumbai   35000
6  ACC1006  Neha Gupta     SAVINGS  Bhopal   90000
```

## Part B — CRUD

Implement:

```http
GET     /accounts
GET     /accounts/{id}
POST    /accounts
PUT     /accounts/{id}
DELETE  /accounts/{id}
```

---

## Account Search Requirements

### Part C — Find By Account Number

Create:

```http
GET /accounts/number/{accountNumber}
```

Example:

```http
GET /accounts/number/ACC1003
```

Use:

```java
@PathVariable
```

### Part D — Find By Customer Name

Create:

```http
GET /accounts?customerName=Rahul
```

Use:

```java
@RequestParam
```

The search should be case-insensitive.

### Part E — Find By Branch

Create:

```http
GET /accounts?branch=Bhopal
```

### Part F — Find By Account Type

Create:

```http
GET /accounts?accountType=SAVINGS
```

### Part G — Find Accounts By Balance Range

Create:

```http
GET /accounts?minBalance=50000&maxBalance=150000
```

Return accounts where:

```text
balance >= minBalance
AND
balance <= maxBalance
```

---

## Business Operations

### Part H — Deposit

Create:

```http
POST /accounts/{id}/deposit?amount=5000
```

Example:

```http
POST /accounts/3/deposit?amount=5000
```

The account balance should increase by `5000`.

### Part I — Withdraw

Create:

```http
POST /accounts/{id}/withdraw?amount=10000
```

The account balance should decrease by `10000`.

### Part J — Insufficient Balance

If a customer tries:

```http
POST /accounts/3/withdraw?amount=1000000
```

throw:

```text
InsufficientBalanceException
```

Return:

```text
HTTP 400 BAD REQUEST
```

with an appropriate message.

### Part K — Invalid Amount

If:

```http
POST /accounts/3/deposit?amount=-5000
```

the operation should not be allowed.

Create an appropriate exception or validation approach.

### Part L — Service Layer

Create:

```text
AccountService
```

The controller should not contain balance calculation logic.

For example, this should not be in the controller:

```java
account.setBalance(account.getBalance() + amount);
```

It belongs in the service.

### Part M — Exception Handling

Create:

```text
AccountNotFoundException
InsufficientBalanceException
```

Handle them using:

```java
@ControllerAdvice
```

Also implement a generic exception handler.

### Part N — Validation

Apply validation to account creation/update:

```text
accountNumber → required
customerName  → required
customerEmail → required + valid email
accountType   → required
branch        → required
balance       → 0 or greater
```

### Part O — Lombok

Convert the `Account` class to use Lombok.

---

# Exercise 3 — Movie Management REST API

This is the **final and most comprehensive exercise**.

## Scenario

You are developing an API for a movie streaming platform.

## Movie Model

```text
Movie
-----
id
title
director
genre
language
rating
releaseYear
duration
```

Example:

```json
{
    "id": 1,
    "title": "Inception",
    "director": "Christopher Nolan",
    "genre": "Sci-Fi",
    "language": "English",
    "rating": 8.8,
    "releaseYear": 2010,
    "duration": 148
}
```

## Part A — Sample Data

Create at least **8 movies**.

Make sure your data contains:

- multiple genres
- multiple directors
- multiple languages
- different ratings
- different release years

This is important because students need enough data to test their search APIs.

## Part B — CRUD

Implement:

```http
GET     /movies
GET     /movies/{id}
POST    /movies
PUT     /movies/{id}
DELETE  /movies/{id}
```

---

## Search Requirements

### Part C — Find By Title

Create:

```http
GET /movies/title/{title}
```

Example:

```http
GET /movies/title/Inception
```

Use:

```java
@PathVariable
```

### Part D — Search By Director

Create:

```http
GET /movies?director=Christopher%20Nolan
```

Use:

```java
@RequestParam
```

### Part E — Search By Genre

Create:

```http
GET /movies?genre=Sci-Fi
```

### Part F — Search By Language

Create:

```http
GET /movies?language=English
```

### Part G — Search By Release Year

Create:

```http
GET /movies?releaseYear=2010
```

### Part H — Minimum Rating

Create:

```http
GET /movies?minRating=8
```

Return movies where:

```text
rating >= 8
```

### Part I — Release Year Range

Create:

```http
GET /movies?fromYear=2000&toYear=2020
```

Return movies released between those years.

### Part J — Combined Search

Support:

```http
GET /movies?genre=Sci-Fi&language=English&minRating=8
```

The result must satisfy all three conditions:

```text
genre = Sci-Fi
AND
language = English
AND
rating >= 8
```

### Advanced Challenge

Support:

```http
GET /movies?genre=Sci-Fi&director=Christopher%20Nolan&minRating=8&fromYear=2000&toYear=2020
```

Students need to decide how to implement the filtering logic in the **service layer**.

---

## Part K — Service Layer

Create:

```text
MovieService
```

The controller should primarily deal with:

```text
HTTP request
     ↓
request parameters
     ↓
service call
     ↓
HTTP response
```

The filtering logic belongs in the service.

## Part L — ResponseEntity

Use appropriate responses:

```text
200 OK
201 CREATED
204 NO CONTENT
404 NOT FOUND
400 BAD REQUEST
```

## Part M — Custom Exception

Create:

```text
MovieNotFoundException
```

If:

```http
GET /movies/999
```

return:

```text
404 NOT FOUND
```

## Part N — Generic Exception

Use:

```java
@ControllerAdvice
```

with:

```java
@ExceptionHandler(Exception.class)
```

Return:

```text
500 INTERNAL SERVER ERROR
```

## Part O — Validation

Use appropriate validation rules:

```text
title       → required
director    → required
genre       → required
language    → required
rating      → between 0 and 10
releaseYear → reasonable year range
duration    → greater than 0
```

For example:

```json
{
    "title": "",
    "director": "",
    "rating": 15,
    "duration": -20
}
```

should fail validation.

## Part P — Lombok

Finally convert the model to Lombok.

Students should be able to explain what:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
```

are doing before they use them.

---

# What Students Should Learn Across the Three Exercises

By the end of the three exercises, students should be comfortable choosing between the following.

## 1. Path Variable

Use it when the value is part of the resource path.

```http
GET /employees/101
```

```java
@PathVariable int id
```

It can also be a field other than `id`:

```http
GET /employees/email/rahul@company.com
```

```java
@PathVariable String email
```

---

## 2. Request Parameter

Use it for searching, filtering, sorting, or optional criteria.

```http
GET /employees?department=IT
```

```java
@RequestParam String department
```

Multiple parameters:

```http
GET /employees?minSalary=50000&maxSalary=80000
```

```java
@RequestParam double minSalary
@RequestParam double maxSalary
```

---

## 3. Request Body

Use it when creating or updating an object.

```http
POST /employees
```

```json
{
    "name": "Rahul",
    "email": "rahul@company.com"
}
```

```java
@RequestBody Employee employee
```

---

# Difficulty Progression

| Concept | Employee | Bank | Movie |
|---|:---:|:---:|:---:|
| Sample data | ✓ | ✓ | ✓ |
| GET all | ✓ | ✓ | ✓ |
| GET by ID | ✓ | ✓ | ✓ |
| POST | ✓ | ✓ | ✓ |
| PUT | ✓ | ✓ | ✓ |
| DELETE | ✓ | ✓ | ✓ |
| Service | ✓ | ✓ | ✓ |
| ResponseEntity | ✓ | ✓ | ✓ |
| Custom exception | ✓ | ✓ | ✓ |
| `@ControllerAdvice` | ✓ | ✓ | ✓ |
| Generic exception | ✓ | ✓ | ✓ |
| Validation | ✓ | ✓ | ✓ |
| Lombok | ✓ | ✓ | ✓ |
| Find by non-ID field | Email | Account number | Title |
| `@RequestParam` | ✓ | ✓ | ✓ |
| Multiple request params | Salary range | Balance range | Year/rating |
| Combined filtering | Basic | Moderate | Advanced |
| Business logic | — | **✓** | — |

## Recommended Teaching Order

```text
Student Example
      ↓
Employee Exercise
      ↓
Bank Account Exercise
      ↓
Movie Exercise
```

The **Employee** exercise checks that students can transfer the basic CRUD pattern.

The **Bank Account** exercise adds real business logic such as deposit, withdrawal, and insufficient balance.

The **Movie** exercise acts as the final assessment because students must combine CRUD, non-ID searches, multiple `@RequestParam` values, filtering, validation, exceptions, and service-layer logic.
