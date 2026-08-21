# Spring Boot REST CRUD Demo — Incremental Build Guide
### Entity: `Student` (id, name, email, course)

This guide builds **one project incrementally**, stage by stage. At every stage I tell you:
1. What Maven dependency (if any) is newly required
2. What package/file to create or change
3. The full code for that stage
4. The concept being taught

**Base package used throughout:** `com.example.studentapi`

Final package structure (you'll grow into this by Stage 21):

```
com.example.studentapi
 ├── StudentApiApplication.java
 ├── controller/StudentController.java
 ├── controller/AuthController.java
 ├── service/StudentService.java
 ├── service/StudentServiceImpl.java
 ├── repository/StudentRepository.java
 ├── repository/UserRepository.java
 ├── entity/Student.java
 ├── entity/User.java
 ├── dto/ErrorResponse.java
 ├── dto/JwtRequest.java / JwtResponse.java
 ├── exception/StudentNotFoundException.java
 ├── exception/GlobalExceptionHandler.java
 ├── config/SecurityConfig.java
 └── security/jwt/JwtUtil.java, JwtAuthFilter.java
```

---

## Stage 0 — Project Setup

Create a Spring Boot project (via https://start.spring.io) with:
- Group: `com.example`
- Artifact: `student-api`
- Dependency to add now: **Spring Web**

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
    <relativePath/>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

**Concept:** `spring-boot-starter-web` pulls in embedded Tomcat, Spring MVC, and Jackson (for JSON). The `-parent` POM manages versions for you so you don't specify version numbers on every dependency.

---

## Stage 1 — Sample Data

**Package:** `entity`

```java
package com.example.studentapi.entity;

public class Student {
    private int id;
    private String name;
    private String email;
    private String course;

    public Student() {}

    public Student(int id, String name, String email, String course) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.course = course;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
}
```

**Package:** `controller`

```java
package com.example.studentapi.controller;

import com.example.studentapi.entity.Student;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class StudentController {

    private List<Student> students = new ArrayList<>(List.of(
            new Student(1, "Aarav Sharma", "aarav@mail.com", "CSE"),
            new Student(2, "Isha Verma", "isha@mail.com", "ECE"),
            new Student(3, "Rohan Gupta", "rohan@mail.com", "ME")
    ));
}
```

**Concept:** We start with an in-memory `List<Student>` as a stand-in for a database, so students can see CRUD mechanics before dealing with persistence. `@RestController` = `@Controller` + `@ResponseBody`, meaning every method's return value is written directly to the HTTP response body (as JSON, via Jackson) instead of resolving to a view name.

---

## Stage 2 — GET All

```java
import org.springframework.web.bind.annotation.GetMapping;

@GetMapping("/students")
public List<Student> getAllStudents() {
    return students;
}
```

**Concept:** `@GetMapping` maps HTTP GET requests to this method. Returning a `List<Student>` causes Jackson to serialize it into a JSON array automatically. Test with:
```
GET http://localhost:8080/students
```

---

## Stage 3 — GET By ID

```java
import org.springframework.web.bind.annotation.PathVariable;

@GetMapping("/students/{id}")
public Student getStudentById(@PathVariable int id) {
    return students.stream()
            .filter(s -> s.getId() == id)
            .findFirst()
            .orElse(null);
}
```

**Concept:** `{id}` in the path is a URI template variable; `@PathVariable` binds it to the method parameter. Right now a missing student silently returns `null` (empty body) — we'll fix that properly with exceptions in Stage 9-10.

---

## Stage 4 — POST

```java
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@PostMapping("/students")
public Student createStudent(@RequestBody Student student) {
    students.add(student);
    return student;
}
```

**Concept:** `@RequestBody` tells Spring to deserialize the incoming JSON request body into a `Student` object using Jackson. `@PostMapping` = shorthand for `@RequestMapping(method = RequestMethod.POST)`.

Test with a JSON body:
```json
{ "id": 4, "name": "Neha Singh", "email": "neha@mail.com", "course": "IT" }
```

---

## Stage 5 — PUT

```java
import org.springframework.web.bind.annotation.PutMapping;

@PutMapping("/students/{id}")
public Student updateStudent(@PathVariable int id, @RequestBody Student updated) {
    for (Student s : students) {
        if (s.getId() == id) {
            s.setName(updated.getName());
            s.setEmail(updated.getEmail());
            s.setCourse(updated.getCourse());
            return s;
        }
    }
    return null;
}
```

**Concept:** PUT is used for full updates of an existing resource, identified by `id` in the path, with the new representation in the request body.

---

## Stage 6 — DELETE

```java
import org.springframework.web.bind.annotation.DeleteMapping;

@DeleteMapping("/students/{id}")
public String deleteStudent(@PathVariable int id) {
    students.removeIf(s -> s.getId() == id);
    return "Student with id " + id + " deleted";
}
```

**Concept:** `@DeleteMapping` maps HTTP DELETE. At this point the controller has all 5 CRUD operations but is doing **both** HTTP handling and business logic — that's a code smell we fix next.

---

## Stage 7 — Move Logic → Service Layer

**Package:** `service`

```java
package com.example.studentapi.service;

import com.example.studentapi.entity.Student;
import java.util.List;

public interface StudentService {
    List<Student> getAllStudents();
    Student getStudentById(int id);
    Student createStudent(Student student);
    Student updateStudent(int id, Student student);
    void deleteStudent(int id);
}
```

```java
package com.example.studentapi.service;

import com.example.studentapi.entity.Student;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private List<Student> students = new ArrayList<>(List.of(
            new Student(1, "Aarav Sharma", "aarav@mail.com", "CSE"),
            new Student(2, "Isha Verma", "isha@mail.com", "ECE"),
            new Student(3, "Rohan Gupta", "rohan@mail.com", "ME")
    ));

    @Override
    public List<Student> getAllStudents() { return students; }

    @Override
    public Student getStudentById(int id) {
        return students.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    @Override
    public Student createStudent(Student student) {
        students.add(student);
        return student;
    }

    @Override
    public Student updateStudent(int id, Student updated) {
        for (Student s : students) {
            if (s.getId() == id) {
                s.setName(updated.getName());
                s.setEmail(updated.getEmail());
                s.setCourse(updated.getCourse());
                return s;
            }
        }
        return null;
    }

    @Override
    public void deleteStudent(int id) {
        students.removeIf(s -> s.getId() == id);
    }
}
```

**Controller now becomes thin** (constructor injection):

```java
package com.example.studentapi.controller;

import com.example.studentapi.entity.Student;
import com.example.studentapi.service.StudentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/students")
    public List<Student> getAllStudents() { return studentService.getAllStudents(); }

    @GetMapping("/students/{id}")
    public Student getStudentById(@PathVariable int id) { return studentService.getStudentById(id); }

    @PostMapping("/students")
    public Student createStudent(@RequestBody Student student) { return studentService.createStudent(student); }

    @PutMapping("/students/{id}")
    public Student updateStudent(@PathVariable int id, @RequestBody Student student) {
        return studentService.updateStudent(id, student);
    }

    @DeleteMapping("/students/{id}")
    public String deleteStudent(@PathVariable int id) {
        studentService.deleteStudent(id);
        return "Student with id " + id + " deleted";
    }
}
```

**Concept:** This is the classic **3-tier layering** (Controller → Service → Repository/Data). `@Service` marks a Spring-managed bean holding business logic. Coding to an **interface** (`StudentService`) rather than the implementation makes the controller loosely coupled — you can swap implementations (e.g., for testing with a mock) without touching the controller. Constructor injection (no `@Autowired` needed on a single constructor since Spring 4.3+) is the recommended DI style over field injection.

---

## Stage 8 — ResponseEntity

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@GetMapping("/students")
public ResponseEntity<List<Student>> getAllStudents() {
    return ResponseEntity.ok(studentService.getAllStudents());
}

@GetMapping("/students/{id}")
public ResponseEntity<Student> getStudentById(@PathVariable int id) {
    Student student = studentService.getStudentById(id);
    if (student == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(student);
}

@PostMapping("/students")
public ResponseEntity<Student> createStudent(@RequestBody Student student) {
    Student created = studentService.createStudent(student);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}

@PutMapping("/students/{id}")
public ResponseEntity<Student> updateStudent(@PathVariable int id, @RequestBody Student student) {
    Student updated = studentService.updateStudent(id, student);
    if (updated == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(updated);
}

@DeleteMapping("/students/{id}")
public ResponseEntity<Void> deleteStudent(@PathVariable int id) {
    studentService.deleteStudent(id);
    return ResponseEntity.noContent().build();
}
```

**Concept:** `ResponseEntity<T>` gives you full control over the HTTP **status code**, **headers**, and **body**, instead of Spring always returning 200 OK. This is the correct REST practice: 201 Created for POST, 404 Not Found when a resource is missing, 204 No Content for a successful DELETE with no body.

---

## Stage 9 — Custom Exception

**Package:** `exception`

```java
package com.example.studentapi.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(int id) {
        super("Student not found with id: " + id);
    }
}
```

Update the service to throw it instead of returning `null`:

```java
@Override
public Student getStudentById(int id) {
    return students.stream()
            .filter(s -> s.getId() == id)
            .findFirst()
            .orElseThrow(() -> new StudentNotFoundException(id));
}

@Override
public Student updateStudent(int id, Student updated) {
    Student existing = getStudentById(id); // throws if not found
    existing.setName(updated.getName());
    existing.setEmail(updated.getEmail());
    existing.setCourse(updated.getCourse());
    return existing;
}

@Override
public void deleteStudent(int id) {
    Student existing = getStudentById(id); // throws if not found
    students.remove(existing);
}
```

**Concept:** A **custom (checked or unchecked) exception** models a specific business error clearly, instead of returning ambiguous `null`s that force every caller to null-check. `RuntimeException` is unchecked, so it doesn't need `throws` declarations everywhere. Right now, if unhandled, Spring Boot would turn this into a generic 500 error — Stage 10 fixes that.

---

## Stage 10 — @ControllerAdvice (Handle the Custom Exception)

**Package:** `dto`

```java
package com.example.studentapi.dto;

import java.time.LocalDateTime;

public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

**Package:** `exception`

```java
package com.example.studentapi.exception;

import com.example.studentapi.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotFound(StudentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
```

Now the controller can simplify — no more manual null-checks:

```java
@GetMapping("/students/{id}")
public ResponseEntity<Student> getStudentById(@PathVariable int id) {
    return ResponseEntity.ok(studentService.getStudentById(id));
}
```

**Concept:** `@ControllerAdvice` centralizes exception handling for **all** controllers in one place — no repeated try/catch blocks. `@ExceptionHandler` targets a specific exception type. This is the **cross-cutting concern** pattern: exception handling is separated from business logic.

---

## Stage 11 — Generic Exception Handler

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Something went wrong: " + ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

**Concept:** This is a **catch-all safety net** — any unanticipated exception (NPE, array index issues, etc.) is converted into a clean JSON 500 response instead of leaking a stack trace to the client. Spring resolves handlers from **most specific to least specific**, so `StudentNotFoundException` is still caught by its own handler first.

---

## Stage 12 — Validation

**Maven dependency (new):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Update the entity:

```java
package com.example.studentapi.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Student {

    private int id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Course is required")
    @Size(min = 2, max = 30, message = "Course must be between 2 and 30 characters")
    private String course;

    // constructors, getters, setters unchanged
}
```

Controller:

```java
import jakarta.validation.Valid;

@PostMapping("/students")
public ResponseEntity<Student> createStudent(@Valid @RequestBody Student student) {
    return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(student));
}
```

Handle validation failures in `GlobalExceptionHandler`:

```java
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.stream.Collectors;

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
    ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
}
```

**Concept:** `@Valid` triggers Bean Validation (JSR-380) on the incoming `@RequestBody`. If any constraint fails, Spring throws `MethodArgumentNotValidException` **before your controller method body even runs** — which is why we handle it centrally rather than manually inside every method.

---

## Stage 13 — Lombok

**Maven dependency (new):**

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```
*(Also install the Lombok plugin in your IDE, e.g. IntelliJ/Eclipse, and enable annotation processing.)*

Entity becomes:

```java
package com.example.studentapi.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    private int id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Course is required")
    @Size(min = 2, max = 30)
    private String course;
}
```

**Concept:** Lombok uses **annotation processing at compile time** to generate getters, setters, `equals()`, `hashCode()`, `toString()` (`@Data`), and constructors (`@NoArgsConstructor`, `@AllArgsConstructor`), so you stop hand-writing boilerplate. The generated code is added into the `.class` file, not visible in your `.java` source.

---

## Stage 14 — Actuator

**Maven dependency (new):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

`application.properties`:

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

**Concept:** Actuator exposes **production-ready monitoring endpoints** like `/actuator/health`, `/actuator/metrics`, `/actuator/info` without you writing any code — useful for health checks, uptime monitoring, and diagnostics (e.g. by Kubernetes liveness probes or monitoring tools like Prometheus).

---

## Stage 15 — DevTools

**Maven dependency (new):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Concept:** DevTools gives **automatic application restart** whenever you recompile a class (e.g., save a file in your IDE), and disables template caching for faster iteration during development. It automatically deactivates itself in a production build (`optional=true`, `scope=runtime` keeps it out of the final production artifact if packaged correctly, and it self-disables when running from a "packaged" jar).

---

## Stage 16 — Configuration / Profiles

Split configuration by environment:

`application.properties`:
```properties
spring.application.name=student-api
spring.profiles.active=dev
```

`application-dev.properties`:
```properties
server.port=8080
logging.level.com.example.studentapi=DEBUG
```

`application-prod.properties`:
```properties
server.port=80
logging.level.com.example.studentapi=WARN
```

A typed configuration class:

```java
package com.example.studentapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String institutionName;
    private int maxStudentsPerCourse;
}
```

```properties
app.institution-name=Bright Future College
app.max-students-per-course=60
```

**Concept:** `spring.profiles.active` selects which `application-{profile}.properties` overlays the base `application.properties` — letting the **same jar** run with different settings in dev vs. prod without code changes. `@ConfigurationProperties` binds a whole group of related properties into one type-safe Java object (instead of many scattered `@Value("${...}")` injections).

---

## Stage 17 — JPA Entity

**Maven dependency (new):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

Convert the plain POJO into a JPA-managed entity:

```java
package com.example.studentapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Course is required")
    @Size(min = 2, max = 30)
    private String course;
}
```

**Concept:** `@Entity` tells Hibernate (the default JPA provider bundled with `spring-boot-starter-data-jpa`) that this class maps to a database table. `@Id` marks the primary key; `@GeneratedValue(strategy = GenerationType.IDENTITY)` delegates auto-increment to the database (matches PostgreSQL's `SERIAL`/`BIGSERIAL`/identity columns). At this stage nothing runs yet because there's no datasource configured — that comes in Stage 19.

---

## Stage 18 — Repository

**Package:** `repository`

```java
package com.example.studentapi.repository;

import com.example.studentapi.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    // JpaRepository already gives: findAll(), findById(), save(), deleteById(), existsById()...
}
```

Rewire the service to use the repository instead of the in-memory `List`:

```java
package com.example.studentapi.service;

import com.example.studentapi.entity.Student;
import com.example.studentapi.exception.StudentNotFoundException;
import com.example.studentapi.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Student getStudentById(int id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }

    @Override
    public Student createStudent(Student student) {
        return studentRepository.save(student);
    }

    @Override
    public Student updateStudent(int id, Student updated) {
        Student existing = getStudentById(id);
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setCourse(updated.getCourse());
        return studentRepository.save(existing);
    }

    @Override
    public void deleteStudent(int id) {
        Student existing = getStudentById(id);
        studentRepository.deleteById(existing.getId());
    }
}
```

**Concept:** `JpaRepository<Student, Integer>` (entity type, primary-key type) gives you a full CRUD API **without writing any implementation** — Spring Data JPA generates it at runtime via a dynamic proxy. `@Repository` marks it as a Spring bean and enables automatic translation of database exceptions into Spring's `DataAccessException` hierarchy.

---

## Stage 19 — PostgreSQL

**Maven dependency (new):**

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

`application-dev.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/student_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Create the database first (one-time, via `psql` or a GUI like pgAdmin):

```sql
CREATE DATABASE student_db;
```

**Concept:** `spring.datasource.*` configures the JDBC connection pool (HikariCP, bundled by default) that Spring Boot auto-configures into a `DataSource` bean. `spring.jpa.hibernate.ddl-auto=update` tells Hibernate to auto-create/alter tables from your `@Entity` classes to match — convenient for demos, but in real production systems teams typically use `validate` (or `none`) plus a migration tool like **Flyway** or **Liquibase** for controlled schema changes.

---

## Stage 20 — Spring Security

**Maven dependency (new):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### 20a. Authentication (concept)
Authentication answers "**who are you?**" — verifying identity (e.g., via username/password). Adding the starter alone auto-secures every endpoint with HTTP Basic and a generated password logged at startup — that's Spring Security's safe-by-default behavior.

### 20b. In-Memory Users

```java
package com.example.studentapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class UserConfig {

    @Bean
    public UserDetailsService userDetailsService(org.springframework.security.crypto.password.PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build(),
                User.withUsername("user").password(encoder.encode("user123")).roles("USER").build()
        );
    }
}
```

### 20c. Roles & 20d. Authorization

```java
package com.example.studentapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/students/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/students/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/students/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/students/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(org.springframework.security.config.Customizer.withDefaults());

        return http.build();
    }
}
```

**Concept (Roles/Authorization):** Authentication proves identity; **authorization** decides "**what are you allowed to do?**". `hasRole("ADMIN")` checks for a granted authority `ROLE_ADMIN`. `authorizeHttpRequests` matchers are evaluated **top-to-bottom, first match wins**, so order matters — always put the most specific rule first.

### 20e. Password Encoding

```java
@Bean
public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
}
```

**Concept:** Passwords must **never** be stored in plaintext. `BCryptPasswordEncoder` applies a one-way, salted hashing algorithm — even you (the developer/DBA) cannot reverse it back to the plaintext password.

### 20f. Database Authentication

Entity:

```java
package com.example.studentapi.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "app_users")
@Data
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;
    private String password;
    private String role; // e.g. "ADMIN" or "USER"
}
```

Repository:

```java
package com.example.studentapi.repository;

import com.example.studentapi.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByUsername(String username);
}
```

Custom `UserDetailsService` (replaces the in-memory one from 20b):

```java
package com.example.studentapi.security;

import com.example.studentapi.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DbUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(appUser.getRole())
                .build();
    }
}
```

**Concept:** Instead of hardcoded users, Spring Security now delegates authentication to your `UserDetailsService` implementation, which looks up credentials from PostgreSQL. Passwords are stored **already BCrypt-hashed** in the `app_users` table (hash them once when creating the user, e.g., via a `CommandLineRunner` or an admin endpoint).

---

## Stage 21 — JWT Authentication

**Maven dependency (new):**

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

### 21a. JWT Concepts
A JWT (JSON Web Token) is a self-contained, digitally-signed token made of three Base64Url-encoded parts: `header.payload.signature`. The **payload (claims)** carries data like username and roles; the **signature** (HMAC or RSA) lets the server verify the token wasn't tampered with, **without needing server-side session storage** — this is what makes JWT auth *stateless*, ideal for REST APIs.

### 21b/21c. Login Endpoint & Generate JWT

**Package:** `dto`

```java
package com.example.studentapi.dto;
public class JwtRequest {
    private String username;
    private String password;
    // getters/setters
}
```
```java
package com.example.studentapi.dto;
public class JwtResponse {
    private String token;
    public JwtResponse(String token) { this.token = token; }
    public String getToken() { return token; }
}
```

**Package:** `security.jwt`

```java
package com.example.studentapi.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    // In real projects, load this secret from application.properties / an env var, never hardcode it.
    private final SecretKey key = Keys.hmacShaKeyFor(
            "this-is-a-very-long-demo-secret-key-please-change-me-in-real-apps".getBytes());

    private final long expirationMs = 3600000; // 1 hour

    public String generateToken(UserDetails userDetails) {
        String roles = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRoles(String token) {
        return parseClaims(token).get("roles", String.class);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
```

**Package:** `controller`

```java
package com.example.studentapi.controller;

import com.example.studentapi.dto.JwtRequest;
import com.example.studentapi.dto.JwtResponse;
import com.example.studentapi.security.jwt.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                           UserDetailsService userDetailsService,
                           JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public JwtResponse login(@RequestBody JwtRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        var userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        return new JwtResponse(token);
    }
}
```

**Concept:** `POST /api/auth/login` is the one endpoint that must stay **publicly accessible** (no token required yet, since the client doesn't have one). `AuthenticationManager.authenticate(...)` runs the credentials through your `UserDetailsService` + `PasswordEncoder` (from Stage 20) and throws `BadCredentialsException` on failure (handle it with `@ExceptionHandler` for a clean 401 response). On success, `JwtUtil` issues a signed token back to the client.

### 21d. Bearer Token
The client stores the returned token and sends it on every subsequent request in the `Authorization` header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjoi...
```

### 21e/21f. Validate JWT & Extract Roles (Filter)

```java
package com.example.studentapi.security.jwt;

import com.example.studentapi.security.DbUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final DbUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, DbUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()); // roles extracted here via authorities
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
```

### 21g. Secure REST API (wire the filter into Security Config)

```java
package com.example.studentapi.config;

import com.example.studentapi.security.jwt.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/students/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/students/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/students/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**Concept:** `SessionCreationPolicy.STATELESS` disables `HttpSession` entirely — the server keeps **no memory** of who's logged in between requests; every request must carry its own proof of identity (the JWT). `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` inserts our custom filter into Spring Security's filter chain **before** the standard form-login filter, so it runs on every incoming request, reads the `Authorization` header, validates the token, extracts the username + roles, and populates the `SecurityContextHolder` — which is what `hasRole("ADMIN")` checks against downstream.

**End-to-end flow to demo to students:**
1. `POST /api/auth/login` with username/password → get back a JWT
2. Copy the JWT into `Authorization: Bearer <token>` header
3. Call `GET /students` (works, any authenticated user) vs. `DELETE /students/1` (works only if role = ADMIN, else 403 Forbidden)

---

## Suggested Teaching Flow Per Stage
For each stage in class: (1) state the *problem* with the previous stage, (2) add the dependency if any, (3) write the code together, (4) run and test in Postman/curl, (5) name the concept explicitly before moving on. This keeps the "why" attached to each increment rather than presenting Spring features in isolation.
