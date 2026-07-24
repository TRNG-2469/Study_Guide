# Maven: Managing Your Java Project with a Build Tool

## Learning Objectives
- Explain what a build tool is and why Java projects need one.
- Describe Maven's role in dependency management, project structure, and the build lifecycle.
- Read and understand the key sections of a `pom.xml` file.
- Add a third-party library to a project by declaring a dependency in `pom.xml`.
- Execute common Maven lifecycle goals (`clean`, `compile`, `test`, `package`).
- Create a Maven project from scratch using IntelliJ IDEA.

---

## Why This Matters

Think about everything you have built during Week 1. Every file lived inside a directory you organised manually, and every class you wrote was compiled from the command line or via IntelliJ's internal build. That workflow is fine for small, self-contained programs — but it breaks down the moment a real-world project requires:

- **External libraries** (e.g., a JSON parser, a database driver, a testing framework like JUnit).
- **Multiple developers** who each need to set up the project identically on their own machines.
- **Automated pipelines** that compile, test, and package the application without human intervention.

Copying JAR files by hand, configuring class-paths manually, and hoping your colleague did exactly the same steps is a recipe for "it works on my machine" bugs.

**Maven** is Apache's answer to this. It gives every Java project a standardised, declarative build configuration in a single file (`pom.xml`). When Maven manages your project, any developer (or CI/CD pipeline — you will encounter these in Week 8) can clone the repository, run `mvn package`, and have a fully compiled, tested, deployable JAR in seconds.

By the end of this program you will see Maven referenced in Spring Boot (Week 5), Docker multi-stage builds (Week 8), and GitLab CI pipelines (Week 8). Understanding it now, during Java fundamentals week, makes every subsequent week easier.

---

## The Concept

### What Is a Build Tool?

A **build tool** automates the repetitive steps of transforming source code into a deployable artefact:

| Step | Without a Build Tool | With Maven |
|---|---|---|
| Download libraries | Manually download JARs, copy to `/lib` | Declare in `pom.xml`; Maven downloads automatically |
| Compile | Run `javac` manually or rely on IDE | `mvn compile` |
| Run tests | Execute test classes one by one | `mvn test` |
| Package | Manually create JAR/WAR | `mvn package` |
| Share config | Hope teammates copy the same JARs | Check in `pom.xml`; Maven resolves everything |

Maven stores downloaded libraries in a local cache called the **local repository** (typically `~/.m2/repository`). Once a library is downloaded it is reused across all your Maven projects on the same machine — no duplicate downloads.

---

### Maven's Standard Directory Layout

One of Maven's biggest contributions is its *convention over configuration* approach. Every Maven project follows the same directory structure by default:

```
my-project/
├── pom.xml                  ← The entire project configuration lives here
└── src/
    ├── main/
    │   └── java/            ← Your production source code (.java files)
    │       └── com/
    │           └── example/
    │               └── App.java
    └── test/
        └── java/            ← Your test source code (JUnit tests)
            └── com/
                └── example/
                    └── AppTest.java
```

Maven automatically knows where to look for sources, tests, and resources based on this layout — no configuration required. IntelliJ understands this structure too; it colour-codes `src/main/java` as a "Sources Root" and `src/test/java` as a "Test Sources Root".

---

### The `pom.xml` — Project Object Model

The **POM** (Project Object Model) is an XML file that is the heart of every Maven project. It tells Maven everything it needs to know about the project. Let's walk through the anatomy of a minimal but realistic `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <!-- POM schema version — always 4.0.0 for Maven 2/3 projects -->
    <modelVersion>4.0.0</modelVersion>

    <!-- ─── Project Coordinates ─────────────────────────────────────────── -->
    <!-- GAV (Group, Artifact, Version) uniquely identifies this project    -->
    <groupId>com.example</groupId>         <!-- Reverse-domain company ID   -->
    <artifactId>week1-demo</artifactId>    <!-- Project/module name         -->
    <version>1.0-SNAPSHOT</version>        <!-- SNAPSHOT = work in progress -->
    <packaging>jar</packaging>             <!-- Output type: jar, war, pom  -->

    <!-- ─── Properties ──────────────────────────────────────────────────── -->
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- ─── Dependencies ─────────────────────────────────────────────────  -->
    <dependencies>

        <!-- JUnit 5 — the standard Java unit testing framework -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>   <!-- Only available during testing; not in the final JAR -->
        </dependency>

    </dependencies>

    <!-- ─── Build ────────────────────────────────────────────────────────  -->
    <build>
        <plugins>
            <!-- Required to run JUnit 5 tests with `mvn test` -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>

</project>
```

#### Key POM Sections Explained

| Section | Purpose |
|---|---|
| `<groupId>` | Identifies your organisation (e.g., `com.revature`, `com.google`). Follows Java's reverse-domain convention. |
| `<artifactId>` | The unique name of this specific module or application. |
| `<version>` | The version of your project. `SNAPSHOT` signals a development version; a release would be `1.0.0`. |
| `<properties>` | Reusable key-value pairs (like variables). Setting `maven.compiler.source` to `17` tells Maven to compile with Java 17 syntax. |
| `<dependencies>` | The list of external libraries your project needs. Maven downloads them automatically from Maven Central. |
| `<build><plugins>` | Plugins that extend Maven's capabilities (e.g., `maven-surefire-plugin` for running JUnit 5 tests). |

---

### Dependency Management: Searching Maven Central

Every `<dependency>` is identified by its **GAV coordinates**: `groupId`, `artifactId`, and `version`. You find these on [Maven Central](https://search.maven.org/) — a public repository hosting hundreds of thousands of Java libraries.

**Example: Adding the Google Gson library (a JSON parser)**

1. Go to [https://search.maven.org/](https://search.maven.org/) and search for `gson`.
2. Click the `com.google.code.gson » gson` result.
3. Copy the XML snippet provided:

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

4. Paste it inside `<dependencies>` in your `pom.xml`.
5. In IntelliJ, click the **"Load Maven Changes"** elephant icon (or press `Ctrl+Shift+O`). Maven downloads the library and it immediately becomes available in your source code.

#### Dependency Scopes

The `<scope>` element controls when a dependency is available:

| Scope | Available At | Included in Final JAR? | Typical Use |
|---|---|---|---|
| `compile` *(default)* | Compile + runtime | Yes | Most libraries (e.g., Gson) |
| `test` | Test compilation + execution only | No | JUnit, Mockito |
| `provided` | Compile only | No | Servlet API (server provides it at runtime) |
| `runtime` | Runtime only | Yes | JDBC drivers |

---

### The Maven Build Lifecycle

Maven defines a set of **lifecycle phases** executed in a fixed order. Running a later phase automatically executes all earlier phases first.

```
validate → compile → test → package → verify → install → deploy
```

| Phase | What Happens |
|---|---|
| `validate` | Checks that the project is structurally correct and `pom.xml` is valid. |
| `compile` | Compiles `src/main/java` into `.class` files in `target/classes`. |
| `test` | Runs unit tests found in `src/test/java`. Build **fails** if any test fails. |
| `package` | Bundles compiled classes into a JAR (or WAR) in the `target/` directory. |
| `install` | Copies the packaged JAR into your **local Maven repository** (`~/.m2`). |
| `deploy` | Uploads the JAR to a **remote repository** (e.g., Nexus, Artifactory). |

**Common Goals You Will Use Daily:**

```bash
mvn clean          # Deletes the target/ directory (removes old build artefacts)
mvn compile        # Compiles only — fast, good for catching syntax errors
mvn test           # Compiles + runs all unit tests
mvn package        # Compiles + tests + packages into a JAR
mvn clean package  # Always clean before packaging to avoid stale class files
```

> **Tip:** In IntelliJ, you can run these goals from the **Maven tool window** (View → Tool Windows → Maven). Expand your project → Lifecycle, then double-click any phase.

---

## Code Example

### Creating a Maven Project in IntelliJ

1. **File → New → Project**
2. Select **Maven Archetype** (or simply **Maven** if using a newer IntelliJ)
3. Set:
   - **Name**: `week1-maven-demo`
   - **GroupId**: `com.example`
   - **ArtifactId**: `week1-maven-demo`
4. Click **Create**

IntelliJ generates the standard directory structure and a starter `pom.xml` automatically.

---

### Sample: Project with JUnit 5

**`pom.xml`** (minimum working configuration):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>week1-maven-demo</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

**`src/main/java/com/example/Calculator.java`**:
```java
package com.example;

public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Cannot divide by zero.");
        }
        return a / b;
    }
}
```

**`src/test/java/com/example/CalculatorTest.java`**:
```java
package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    private final Calculator calc = new Calculator();

    @Test
    void addReturnsSumOfTwoNumbers() {
        assertEquals(7, calc.add(3, 4));
    }

    @Test
    void divideByZeroThrowsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> calc.divide(10, 0));
    }
}
```

Run `mvn test` from the terminal (or double-click **test** in IntelliJ's Maven window). You will see output like:

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Summary
- **Maven** is a build automation tool that standardises project structure, manages library dependencies, and automates compilation, testing, and packaging.
- Every Maven project is described by a `pom.xml` file containing **GAV coordinates**, **properties**, **dependencies**, and **build plugins**.
- Maven downloads libraries from **Maven Central** and caches them in `~/.m2/repository`.
- **Dependency scopes** (`compile`, `test`, `provided`, `runtime`) control when a library is available and whether it is bundled into the final JAR.
- The **Maven Build Lifecycle** (`validate → compile → test → package → install → deploy`) ensures a consistent, repeatable build process.
- You will encounter Maven again in **Week 5 (Spring Boot)** and **Week 8 (DevOps / GitLab CI)** — understanding it now gives you a strong foundation.

---

## Additional Resources
- [Maven Getting Started Guide — Apache Maven Official Docs](https://maven.apache.org/guides/getting-started/index.html)
- [Maven in 5 Minutes — Apache Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
- [Introduction to the POM — Apache Maven](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)
