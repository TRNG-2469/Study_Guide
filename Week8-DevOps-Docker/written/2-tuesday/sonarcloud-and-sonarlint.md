# SonarCloud and SonarLint

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what static code analysis is and why it matters
- Distinguish between SonarCloud (CI integration) and SonarLint (IDE plugin)
- Define the five key quality metrics: bugs, vulnerabilities, code smells, coverage, and duplication
- Explain what a quality gate is and how it protects the main branch
- Describe how to integrate SonarCloud into a GitLab CI pipeline

---

## Why This Matters

Writing code that works is necessary but not sufficient. Code must also be secure, maintainable, and consistent in quality across a team. Without objective quality standards, code quality degrades gradually over time — each developer makes small compromises, and the accumulated technical debt eventually makes the codebase expensive and risky to change. SonarCloud and SonarLint automate code quality enforcement, catching problems the moment they are introduced rather than months later when they are expensive to fix. These tools are used in production at thousands of companies and will be part of your daily workflow as a professional developer.

---

## What Is Static Code Analysis?

**Static code analysis** is the process of examining source code without executing it. An analysis tool reads your code, applies a set of rules (also called checks or inspectors), and reports issues it finds. This is distinct from dynamic analysis, which tests code behavior while it runs.

Think of static analysis as a very thorough automated code reviewer that:
- Never gets tired
- Applies the same standards to every line of every file, every time
- Catches entire categories of bugs that humans commonly miss
- Enforces style and complexity rules consistently across the team

Static analysis tools have existed for decades (lint for C, FindBugs for Java), but SonarQube and its cloud-hosted sibling SonarCloud have become the industry standard for enterprise Java development because they integrate deeply with CI/CD pipelines and provide a comprehensive, actionable quality dashboard.

---

## SonarLint: Catch Issues in the IDE

### What It Is

**SonarLint** is an IDE plugin that runs static analysis locally, in real time, as you type code. It catches issues before you even commit — the earlier a bug is found, the cheaper it is to fix.

The analogy: SonarLint is to code quality what spell-check is to writing. It underlines problems as you create them, without requiring you to explicitly invoke a check.

### How to Install SonarLint

SonarLint is available for all major Java IDEs:

**IntelliJ IDEA:**
1. Open File → Settings (or IntelliJ IDEA → Preferences on macOS)
2. Navigate to Plugins
3. Search for "SonarLint"
4. Click Install and restart the IDE

**VS Code:**
1. Open the Extensions panel (Ctrl+Shift+X)
2. Search for "SonarLint"
3. Click Install

**Eclipse:**
1. Go to Help → Eclipse Marketplace
2. Search for "SonarLint"
3. Click Install

### What SonarLint Shows You

Once installed, SonarLint adds inline annotations in your editor:

```java
public class UserService {

    public User findById(String id) {
        // SonarLint underlines this with a warning:
        // "Null pointers should not be dereferenced (java:S2259)"
        // Because getUser() could return null and we immediately call .getName()
        return userRepository.getUser(id).toUser();
        //                              ^^^^^^^^
        //                              SonarLint: Possible NullPointerException here
    }

    public void processUsers(List<User> users) {
        // SonarLint flags this with a code smell:
        // "Collection should not be compared using '==' (java:S4973)"
        if (users == null) {   // ← correct for null check
            return;
        }
        // SonarLint flags the next line:
        // "String literals should not be duplicated (java:S1192)"
        // if the same "ERROR: user" string appears elsewhere in the file
    }
}
```

### Connecting SonarLint to SonarCloud (Connected Mode)

By default, SonarLint uses its own built-in rules. In **Connected Mode**, SonarLint synchronizes with your SonarCloud project, meaning:
- It applies exactly the same rules your CI pipeline uses
- Issues it finds locally are guaranteed to be flagged by the CI pipeline
- Quality profile changes in SonarCloud propagate to everyone's IDE automatically

To connect: In the SonarLint settings panel, add a connection to your SonarCloud organization and bind your local project to a SonarCloud project.

---

## SonarCloud: Continuous Quality in the CI Pipeline

### What It Is

**SonarCloud** is the cloud-hosted, SaaS version of SonarQube. It integrates into your CI pipeline to analyze every pull request and every merge to the main branch, providing:

- A web dashboard showing code quality trends over time
- Pull request decoration (comments on the PR showing what issues were introduced)
- Quality gates that can block merges if quality standards are not met
- Historical analysis so you can see whether quality is improving or degrading

SonarCloud is free for public repositories and has a paid tier for private repositories.

### SonarCloud vs. SonarQube

| Dimension | SonarCloud | SonarQube |
|---|---|---|
| Hosting | Cloud (SaaS, hosted by Sonar) | Self-hosted (your own servers) |
| Setup | Minutes (create account, connect repo) | Hours to days (install, configure, maintain) |
| Cost | Free for open source; subscription for private | Free Community Edition; paid for advanced features |
| Maintenance | None — Sonar maintains the infrastructure | Your team maintains the server, upgrades, backups |
| Best for | Most teams; new projects | Organizations with data sovereignty requirements or air-gapped networks |

---

## The Five Key Quality Metrics

SonarCloud measures code quality across five dimensions. Understanding each one helps you interpret the dashboard and prioritize improvements.

### 1. Bugs

**Definition:** A coding error that will cause incorrect behavior or a crash in certain conditions.

Bugs are issues that SonarCloud is highly confident will cause runtime problems. They are not speculative — the analyzer has determined that the code behaves incorrectly.

**Examples of what SonarCloud detects as bugs:**

```java
// Bug: NullPointerException waiting to happen
public String getUserEmail(Long userId) {
    User user = userRepository.findById(userId); // Returns null if not found
    return user.getEmail(); // NullPointerException if user is null
    // Fix: return user != null ? user.getEmail() : null;
    // Or: return userRepository.findById(userId)
    //          .map(User::getEmail)
    //          .orElse(null);
}

// Bug: Incorrect use of equals() on incompatible types
public boolean isAdminUser(Object userRole) {
    return userRole == "ADMIN"; // Always false! Use .equals() for String comparison
    // Fix: return "ADMIN".equals(userRole);
}

// Bug: Resource leak — InputStream opened but never closed
public void processFile(String path) throws IOException {
    InputStream is = new FileInputStream(path);
    // ... process file ...
    // is.close() is never called — resource leak!
    // Fix: use try-with-resources:
    // try (InputStream is = new FileInputStream(path)) { ... }
}
```

**Priority:** Bugs are the highest priority finding. A new bug introduced in a pull request should block the merge.

### 2. Vulnerabilities

**Definition:** A security weakness in the code that could be exploited by an attacker.

SonarCloud applies OWASP Top 10 and CWE security rules to identify vulnerabilities before they reach production.

**Examples:**

```java
// Vulnerability: SQL Injection
// Attacker input: "'; DROP TABLE users; --"
public List<User> searchUsers(String searchTerm) {
    // NEVER concatenate user input into SQL strings
    String query = "SELECT * FROM users WHERE name = '" + searchTerm + "'";
    return jdbcTemplate.query(query, userRowMapper);
    // Fix: Use parameterized queries:
    // return jdbcTemplate.query(
    //     "SELECT * FROM users WHERE name = ?",
    //     userRowMapper,
    //     searchTerm
    // );
}

// Vulnerability: Hardcoded credentials
public DataSource createDataSource() {
    DataSourceBuilder builder = DataSourceBuilder.create();
    builder.password("mySecretPassword123"); // NEVER hardcode credentials
    // Fix: Load from environment variable or AWS Secrets Manager
    // builder.password(System.getenv("DB_PASSWORD"));
    return builder.build();
}

// Vulnerability: Weak cryptography
import java.security.MessageDigest;

public String hashPassword(String password) {
    MessageDigest md = MessageDigest.getInstance("MD5"); // MD5 is broken for passwords
    byte[] hash = md.digest(password.getBytes());
    // Fix: Use BCrypt, Argon2, or PBKDF2 for password hashing
    // return BCrypt.hashpw(password, BCrypt.gensalt());
}
```

**Priority:** Vulnerabilities should block PR merges and be treated as bugs in terms of urgency.

### 3. Code Smells

**Definition:** Code that is not wrong today but will make the codebase harder to maintain, understand, or extend tomorrow. Code smells are maintainability issues.

Code smells are not bugs — the code may work correctly. But they are warning signs of design problems that accumulate into significant technical debt.

**Examples:**

```java
// Code smell: Method is too long (> 30 lines is SonarCloud's default threshold)
// Long methods are hard to understand, test, and modify
public void processOrder(Order order) {
    // 150 lines of complex logic...
    // Fix: Extract into smaller, well-named methods
}

// Code smell: Too many parameters (> 7 parameters)
// Hard to understand, easy to mix up argument order
public Invoice createInvoice(
    String customerId, String productId, int quantity,
    BigDecimal unitPrice, String currency, String shippingAddress,
    String billingAddress, LocalDate dueDate, boolean taxExempt) {
    // Fix: Introduce a parameter object (CreateInvoiceRequest class)
}

// Code smell: Duplicated code
// If the same 10-line block appears in 3 places, a bug fix must be applied 3 times
// Fix: Extract into a shared method

// Code smell: Cognitive complexity too high
// Deeply nested conditionals are hard to reason about
public String classify(int score) {
    if (score > 0) {
        if (score > 50) {
            if (score > 80) {
                if (score > 95) {
                    return "A+";
                } else {
                    return "A";
                }
            } else {
                return "B";
            }
        } else {
            return "C";
        }
    } else {
        return "F";
    }
    // Fix: Use guard clauses and early returns
}
```

**Priority:** Code smells are tracked as **technical debt** — SonarCloud estimates how long it would take to fix them. The goal is to prevent debt accumulation, not necessarily to fix all existing smells immediately.

### 4. Code Coverage

**Definition:** The percentage of production code lines that are executed by automated tests.

Coverage is measured by running your test suite with a code coverage tool (JaCoCo for Java) and reporting which lines were and were not executed.

```
Coverage Example:
  Total lines of production code: 2,400
  Lines executed by tests: 1,872
  Coverage: 1,872 / 2,400 = 78%
```

**What coverage tells you:** Lines not covered by tests are lines that could break without any test catching it. 0% coverage on a class means that class has never been tested by the automated suite.

**What coverage does NOT tell you:** High coverage does not mean good tests. A test that exercises every line but makes no assertions (no `assertEquals`, no `assertThat`) counts as 100% covered but tests nothing.

**Industry benchmarks:**
- Below 60%: Concerning — significant areas of untested code
- 60–80%: Acceptable for legacy systems with technical debt
- 80–90%: Good for most production systems
- Above 90%: Excellent — typically requires TDD discipline

**Configuring JaCoCo for SonarCloud in Maven:**

```xml
<!-- pom.xml: Add JaCoCo plugin to generate coverage reports -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <!-- Prepare coverage agent before tests run -->
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <!-- Generate the report after tests complete -->
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 5. Duplication

**Definition:** The percentage of lines of code that are copy-pasted duplicates of other lines in the codebase.

SonarCloud detects blocks of code (typically 10+ lines) that appear multiple times. Duplicated code is a problem because:
- Bug fixes must be applied in every copy (and developers forget)
- Understanding the system requires reading the same logic multiple times
- It indicates poor abstraction — logic that should be a shared function

**Target:** Below 3% duplication is considered good. Many mature codebases maintain below 1%.

---

## Quality Gates

### What Is a Quality Gate?

A **quality gate** is a set of conditions that a project must meet before a pull request can be merged or a release can proceed. If any condition fails, the gate "fails" and the CI pipeline reports a failure status on the PR — blocking the merge until the issues are resolved.

Quality gates make code quality standards automatic and enforceable, not suggestions.

### The Default SonarCloud Quality Gate

SonarCloud's default "Sonar way" quality gate checks the **new code** introduced in a pull request (not the entire codebase). This is important — it prevents a team from being blocked by existing technical debt while still preventing new debt from being added.

Default conditions for new code:
- **No new bugs:** 0 bugs introduced in this PR
- **No new vulnerabilities:** 0 security vulnerabilities introduced
- **New code coverage ≥ 80%:** At least 80% of the new lines added must be tested
- **New code duplication ≤ 3%:** No more than 3% of new lines are duplicated

### Customizing Quality Gates

Teams can define custom quality gates to match their standards:

```
Custom Quality Gate: "Revature Production Standard"

Conditions:
  - New Bugs: 0 (BLOCKER)
  - New Vulnerabilities: 0 (BLOCKER)
  - New Security Hotspots Reviewed: 100% (BLOCKER)
  - New Code Coverage: ≥ 75% (BLOCKER)
  - New Duplicated Lines: ≤ 5% (WARNING)
  - New Code Smells: ≤ 10 (WARNING)
```

BLOCKER conditions fail the quality gate (blocking the merge). WARNING conditions appear in the report but do not block.

---

## Integrating SonarCloud into GitLab CI

### Prerequisites

1. Create a SonarCloud account at https://sonarcloud.io (free for public repos)
2. Create an organization and project in SonarCloud
3. Generate a SonarCloud token (User → My Account → Security → Generate Token)
4. Add the token as a GitLab CI/CD variable: Settings → CI/CD → Variables → Add `SONAR_TOKEN`

### GitLab CI Configuration

```yaml
# .gitlab-ci.yml

stages:
  - build
  - test
  - analyze

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  # SONAR_TOKEN is set as a protected CI/CD variable in GitLab
  # Never hardcode it here — it is a secret

cache:
  paths:
    - .m2/repository

# Run tests and collect JaCoCo coverage data
unit-tests:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    # 'verify' runs compile → test → package, which includes JaCoCo report generation
    - mvn verify -B
  artifacts:
    paths:
      # Save the JaCoCo report so the sonar job can read it
      - target/site/jacoco/jacoco.xml
      - target/surefire-reports/
    reports:
      # Publish JUnit results to GitLab's test reports UI
      junit: target/surefire-reports/*.xml
    expire_in: 1 hour

# SonarCloud analysis — runs after tests so coverage data is available
sonarcloud-analysis:
  stage: analyze
  image: maven:3.9-eclipse-temurin-21
  dependencies:
    - unit-tests  # Download artifacts from the unit-tests job (coverage report)
  script:
    - mvn sonar:sonar -B
      -Dsonar.projectKey=my-org_my-project
      -Dsonar.organization=my-org
      -Dsonar.host.url=https://sonarcloud.io
      -Dsonar.login=$SONAR_TOKEN
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
      -Dsonar.qualitygate.wait=true
  # sonar.qualitygate.wait=true makes the CI job wait for SonarCloud to finish
  # analysis and return the quality gate result. The job fails if the gate fails.
  only:
    - main
    - merge_requests
```

### Maven Plugin Configuration

Add the SonarQube Maven plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.11.0.3922</version>
</plugin>
```

### What Happens in a Pull Request

When a developer opens a merge request in GitLab:

1. GitLab CI triggers the pipeline on the MR branch
2. The `unit-tests` job runs and generates coverage data
3. The `sonarcloud-analysis` job runs and sends results to SonarCloud
4. SonarCloud analyzes only the **new code** in this MR against the quality gate
5. SonarCloud posts a comment on the MR showing:
   - Issues found in this MR (bugs, vulnerabilities, code smells)
   - Coverage of new code
   - Quality gate status (PASSED or FAILED)
6. GitLab shows the SonarCloud status check on the MR — if it failed, the "Merge" button is disabled (when branch protection rules are configured)

---

## Reading the SonarCloud Dashboard

The SonarCloud project dashboard shows:

```
Overall Code Health
┌────────────┬───────────────┬──────────────┬──────────────┬─────────────┐
│   Bugs     │ Vulnerabilities│ Code Smells  │  Coverage   │ Duplication │
│    3        │      0        │    47        │    82.3%    │    1.2%     │
│  (D rating) │  (A rating)   │  (C rating)  │  (B rating) │  (A rating) │
└────────────┴───────────────┴──────────────┴──────────────┴─────────────┘

Quality Gate: ✅ PASSED (new code meets all conditions)

Reliability Rating:  A = 0 bugs, B = 1 minor bug, C = 1 major bug, D = 1 critical bug
Security Rating:     A = 0 vulnerabilities, ... F = 1+ critical vulnerability
Maintainability:     A = < 5% technical debt ratio, ... E = > 50% technical debt ratio
```

The **new code** tab (showing only recently added code) is the most actionable view during active development. The **overall code** view shows the accumulated health of the entire codebase.

---

## Summary

| Concept | Key Takeaway |
|---|---|
| Static analysis | Examines code without running it; catches bugs, vulnerabilities, and smells automatically |
| SonarLint | IDE plugin; catches issues as you type; works offline; supports connected mode |
| SonarCloud | Cloud-hosted CI integration; analyzes every PR; tracks trends over time |
| Bugs | Code errors that cause incorrect behavior; highest priority |
| Vulnerabilities | Security weaknesses exploitable by attackers; block merges immediately |
| Code smells | Maintainability issues; accumulate as technical debt; track and prevent |
| Coverage | Percentage of lines exercised by tests; target 80%+ for new code |
| Duplication | Repeated code blocks; target below 3%; indicates poor abstraction |
| Quality gate | Set of conditions code must meet; failed gate blocks PR merge |
| GitLab CI integration | Add `sonar:sonar` goal to pipeline; use `SONAR_TOKEN` secret; `qualitygate.wait=true` |

---

## External Resources

1. **SonarCloud Official Documentation** — https://docs.sonarcloud.io/
2. **SonarLint IDE Plugin Download** — https://www.sonarsource.com/products/sonarlint/
3. **OWASP Top 10 (the security vulnerabilities SonarCloud checks against)** — https://owasp.org/www-project-top-ten/
