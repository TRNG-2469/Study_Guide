# Lab: AI-Powered Features & Code Quality

**Duration:** 3-4 hours
**Mode:** Individual (Implementation)
**Week:** 8 - Tuesday
**Track:** AWS Bedrock (Java) - DevOps / CI/CD Concepts - SonarCloud / SonarLint

---

## Prerequisites

| Requirement | Version / Notes |
|---|---|
| IntelliJ IDEA | 2023.x or later (Community or Ultimate) |
| Java | 17 (confirm with `java -version`) |
| Maven | 3.9.x (confirm with `mvn -v`) |
| AWS Account | Bedrock access enabled (see Setup below) |
| SonarLint Plugin | Installed in IntelliJ (see Part 2 Setup) |
| Project 3 codebase | Cloned and opening cleanly in IntelliJ |

---

## Learning Objectives

By the end of this lab you will be able to:

1. **Call AWS Bedrock from Java** using the AWS SDK v2 `BedrockRuntimeClient` and the Converse API.
2. **Explain and experiment with the temperature parameter** - articulate how it affects output variability and choose an appropriate value for a production use case.
3. **Distinguish the four SonarLint finding categories** (Bug, Vulnerability, Code Smell, Security Hotspot) and apply the correct remediation strategy to each.
4. **Remediate high-severity code quality issues** in a real Spring Boot codebase, verifying each fix with SonarLint re-analysis.
5. **Communicate code quality findings** clearly in a structured quality report suitable for a sprint review or technical retrospective.

---

## Scenario

> *The product team has asked you to add an AI-powered product description generator to the e-commerce API. Meanwhile, QA has flagged that the codebase has technical debt that must be addressed before the next sprint. Your job today: build the AI feature AND clean up the code.*

This is a common real-world situation: new features and quality improvement work happening in parallel. By the end of the day you will have a working Bedrock integration checked into your branch and a documented quality baseline for Project 3.

---

# PART 1 - AWS Bedrock Integration in Java (~2 hours)

---

## Setup

### Step 1 - Add Maven Dependencies

Open your project's `pom.xml` and add the AWS SDK BOM inside `<dependencyManagement>` and the Bedrock Runtime dependency inside `<dependencies>`:

```xml
<!-- Inside <dependencyManagement> -> <dependencies> -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>bom</artifactId>
    <version>2.28.17</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- Inside <dependencies> (no version needed - BOM manages it) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>bedrockruntime</artifactId>
</dependency>

<!-- Also needed if not already present -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sso</artifactId>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ssooidc</artifactId>
</dependency>
```

Run `mvn dependency:resolve` to confirm there are no conflicts.

---

### Step 2 - Configure AWS Credentials

You have two options. Use whichever your AWS setup supports:

**Option A - Environment Variables (recommended for this lab)**

```bash
# Windows (PowerShell)
$env:AWS_ACCESS_KEY_ID     = "AKIA..."
$env:AWS_SECRET_ACCESS_KEY = "your-secret-key"
$env:AWS_DEFAULT_REGION    = "us-east-1"

# macOS / Linux
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_DEFAULT_REGION="us-east-1"
```

Set these in your shell before launching IntelliJ, or configure them in IntelliJ's Run/Debug configuration under **Environment variables**.

**Option B - ~/.aws/credentials file**

```ini
[default]
aws_access_key_id     = AKIA...
aws_secret_access_key = your-secret-key
region                = us-east-1
```

The AWS SDK picks this up automatically. Do not commit this file to version control.

---

### Step 3 - Verify Bedrock Model Access

1. Log in to the AWS Console and navigate to **Amazon Bedrock**.
2. In the left sidebar click **Model access**.
3. Confirm that **Anthropic -> Claude 3.5 Sonnet** shows status **Access granted**.
4. If not, click **Manage model access**, tick Claude 3.5 Sonnet, and submit. Access is usually granted within 1-2 minutes.

> **Note:** Model access is per-region. Make sure the region you enable matches `AWS_DEFAULT_REGION`.

---

## Task 1.1 - Build the Basic Bedrock Client

**File:** `starter_code/BedrockProductService.java`

Open the starter file in IntelliJ. You will implement the `generateProductDescription` method.

**What to implement:**

1. Build a `ConverseRequest` using the Converse API (not `InvokeModel` - the Converse API is the modern, model-agnostic approach).
2. Target model ID: `anthropic.claude-3-5-sonnet-20241022-v2:0`
3. Craft a prompt that instructs Claude to write a **2-3 sentence product description** suitable for an e-commerce site, given a product name and comma-separated features.
4. Extract the response text from the `ConverseResponse`.
5. Return the generated description as a `String`.

**Prompt template suggestion:**

```
You are a professional copywriter for an e-commerce platform.
Write a compelling 2-3 sentence product description for the following product.
The description should highlight key benefits and appeal to online shoppers.

Product name: {productName}
Key features: {features}

Respond with only the product description. Do not include labels or preamble.
```

**Run `main()` to test.** You should see three generated descriptions printed to console.

Checkpoint 1.1: Running `main()` prints a non-empty AI-generated description for each of the 3 sample products.

---

## Task 1.2 - Experiment with Temperature

Temperature controls how deterministic vs. creative the model output is:

- **0.0** - fully deterministic; the model always picks the highest-probability next token. Outputs are nearly identical across runs.
- **0.7** - balanced; introduces variation while keeping outputs coherent and on-topic.
- **1.0** - high creativity; outputs vary significantly run-to-run and may occasionally drift off-topic.

**Instructions:**

Use the same product (e.g., "Wireless Noise-Cancelling Headphones") for all runs.

1. Set temperature to `0.0f`, run `main()` 3 times, copy the first 50 characters of each output.
2. Set temperature to `0.7f`, run 3 times, copy outputs.
3. Set temperature to `1.0f`, run 3 times, copy outputs.

Fill in this observation table (save it as `temperature-observations.md` in this folder):

```
| Temperature | Run 1 (first 50 chars) | Run 2 (first 50 chars) | Run 3 (first 50 chars) | Observation |
|-------------|------------------------|------------------------|------------------------|-------------|
| 0.0         |                        |                        |                        |             |
| 0.7         |                        |                        |                        |             |
| 1.0         |                        |                        |                        |             |
```

**Reflection:** Which temperature would you choose for a production e-commerce feature? Why? Write 2-3 sentences below the table.

Checkpoint 1.2: Table is filled in. Written conclusion explains the production trade-off.

---

## Task 1.3 - Add Error Handling

Production Bedrock calls can fail in predictable ways. Add a try-catch block inside `generateProductDescription` that handles:

| Exception | When it happens | User-friendly message |
|---|---|---|
| `AccessDeniedException` | Model not enabled in this region / IAM permissions missing | "AI description unavailable: model access not configured. Contact your administrator." |
| `ThrottlingException` | Too many requests per second | "AI description temporarily unavailable due to high demand. Please try again in a few seconds." |
| `ValidationException` | Malformed request (bad model ID, empty prompt, etc.) | "Could not generate description: invalid request parameters." |
| `BedrockRuntimeException` | Any other Bedrock-specific error | "AI description service error: {exception message}" |

**How to trigger each error for testing:**

- `AccessDeniedException`: temporarily change the model ID to one you have not enabled.
- `ThrottlingException`: add a unit test that mocks the client to throw this exception.
- `ValidationException`: pass an empty string as the model ID.

Checkpoint 1.3: Each exception prints a distinct, user-friendly message. The happy path still works after adding error handling.

---

## Task 1.4 - Integrate into a Spring Boot Controller (Bonus)

Create `src/main/java/com/revature/controller/ProductAIController.java`:

```java
@RestController
@RequestMapping("/api/products")
public class ProductAIController {

    private final BedrockProductService bedrockService;

    // Constructor injection

    @PostMapping("/describe")
    public ResponseEntity<Map<String, String>> describeProduct(
            @RequestBody ProductDescribeRequest request) {
        // Call bedrockService.generateProductDescription(...)
        // Return { "description": "..." } as JSON
    }
}
```

**Request body:**

```json
{
  "productName": "Ergonomic Office Chair",
  "features": ["lumbar support", "adjustable armrests", "breathable mesh back"]
}
```

**Expected response:**

```json
{
  "description": "Experience all-day comfort with our Ergonomic Office Chair..."
}
```

Test with Postman. Register `BedrockProductService` as a Spring `@Service` bean.

Checkpoint 1.4 (Bonus): `POST /api/products/describe` returns a 200 response with a generated description.

---

# PART 2 - SonarLint Code Quality Analysis (~1.5 hours)

---

## Setup

### Install the SonarLint Plugin

1. In IntelliJ: **File -> Settings -> Plugins** (Windows/Linux) or **IntelliJ IDEA -> Preferences -> Plugins** (macOS).
2. Search for **SonarLint**.
3. Click **Install**, then **Restart IDE** when prompted.
4. After restart: **View -> Tool Windows -> SonarLint** - the SonarLint panel should appear at the bottom.

### Run the Initial Analysis

1. Open your **Project 3** Spring Boot codebase in IntelliJ.
2. In the Project panel, right-click the project root.
3. Select **SonarLint -> Analyze All Files**.
4. Wait for the analysis to complete (30 seconds to 2 minutes).
5. Review findings in the SonarLint panel.

---

## Understanding Finding Categories

| Category | Definition | Example |
|---|---|---|
| **Bug** | Code that is likely incorrect and will cause unexpected behavior at runtime | NullPointerException risk, resource leak, incorrect equals() implementation |
| **Vulnerability** | Code that can be exploited by an attacker to compromise the system | SQL injection, hardcoded secrets, insecure deserialization |
| **Code Smell** | Code that is not wrong but is hard to maintain, understand, or extend | Method too long, duplicate code, magic numbers, unused variables |
| **Security Hotspot** | Code that requires a human security review - may or may not be a real vulnerability | Use of Random (vs SecureRandom), logging of user input |

**Severity levels:** Blocker -> Critical -> Major -> Minor -> Info

---

## Task 2.1 - Categorize ALL Findings

Fill in the table below for every finding SonarLint reports on your Project 3 codebase. If there are more than 20, include all Blockers and Criticals, then a representative sample of Majors.

Save as `sonar-findings.md` in this folder.

```
| # | File (short name) | Line | Category | Severity | Description (what SonarLint says) |
|---|-------------------|------|----------|----------|-----------------------------------|
| 1 |                   |      |          |          |                                   |
| 2 |                   |      |          |          |                                   |
| 3 |                   |      |          |          |                                   |
| 4 |                   |      |          |          |                                   |
| 5 |                   |      |          |          |                                   |
```

**Tally summary (fill in):**

| Category | Count |
|---|---|
| Bug | |
| Vulnerability | |
| Code Smell | |
| Security Hotspot | |
| **Total** | |

Checkpoint 2.1: Table contains at least 5 rows (or all findings if fewer than 5). Tally is complete.

---

## Task 2.2 - Fix at Least 3 High-Severity Issues

For each issue you fix, document it using this structure:

---

### Fix #1 - [Issue Title]

**File:** `path/to/File.java`
**Line:** XX
**Category:** Bug / Vulnerability / Code Smell
**Severity:** Blocker / Critical / Major

**Why it is a problem:**
[One or two sentences explaining the risk or maintenance burden]

**Before:**
```java
// Paste the problematic code snippet here
```

**After:**
```java
// Paste the fixed code snippet here
```

**Verification:** Re-ran SonarLint - finding no longer appears.

---

Repeat for Fix #2 and Fix #3.

### Common Issues in Spring Boot Projects - What to Look For

**Bug Example - Potential NullPointerException**
```java
// BEFORE (Bug - if findById returns empty Optional, get() throws NoSuchElementException)
Product product = productRepository.findById(id).get();
return product.getName();

// AFTER
Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
return product.getName();
```

**Vulnerability Example - SQL String Concatenation**
```java
// BEFORE (Vulnerability - SQL injection risk)
String query = "SELECT * FROM products WHERE name = '" + name + "'";
jdbcTemplate.query(query, ...);

// AFTER - use parameterized query
String query = "SELECT * FROM products WHERE name = ?";
jdbcTemplate.query(query, new Object[]{name}, ...);
```

**Code Smell Example - Magic Number**
```java
// BEFORE (Code Smell - magic number)
if (description.length() > 500) {
    throw new IllegalArgumentException("Too long");
}

// AFTER
private static final int MAX_DESCRIPTION_LENGTH = 500;

if (description.length() > MAX_DESCRIPTION_LENGTH) {
    throw new IllegalArgumentException("Description exceeds maximum length of "
            + MAX_DESCRIPTION_LENGTH + " characters.");
}
```

Checkpoint 2.2: 3 fixes documented with before/after code. SonarLint re-analysis confirms each is resolved.

---

## Task 2.3 - Write a Quality Report

Use the template at `starter_code/quality-report-template.md`.

Fill in every section with real data from your Project 3 analysis. The report should be something you could present at a sprint retrospective.

Save the completed report as `quality-report-completed.md` in this folder.

Checkpoint 2.3: All 5 sections of the template are filled in with real data.

---

## Definition of Done

- [ ] `BedrockProductService.generateProductDescription()` is fully implemented and returns descriptions for at least 3 different products
- [ ] `temperature-observations.md` is filled in with 9 outputs (3 temperatures x 3 runs) and includes a written production recommendation
- [ ] Error handling covers at least `AccessDeniedException` and `ThrottlingException` with distinct messages
- [ ] Bonus: `POST /api/products/describe` endpoint is implemented and tested in Postman
- [ ] `sonar-findings.md` contains all SonarLint findings from Project 3, with complete tally
- [ ] At least 3 high-severity fixes are documented with before/after code and re-analysis confirmation
- [ ] `quality-report-completed.md` is fully written with real Project 3 data

---

## Reflection Questions

Answer these in a `reflection.md` file in this folder:

1. **Temperature and determinism:** You experimented with temperature 0.0, 0.7, and 1.0. Imagine you are building a Bedrock feature that generates *legal disclaimers* for financial products. What temperature would you choose, and why? How does your answer change if the feature is generating *ad copy* for a social media campaign?

2. **Error handling philosophy:** The Bedrock client can throw `ThrottlingException` when the service is under load. In a real Spring Boot API, what patterns could you use to handle throttling gracefully without returning a 500 error to the end user? Name at least two strategies and explain the trade-offs.

3. **Technical debt prioritization:** SonarLint reported findings across four categories. If you only had time to fix issues in one category before a production release, which category would you prioritize - Bug, Vulnerability, Code Smell, or Security Hotspot? Justify your choice and explain what you would do with the unfixed categories in the meantime.
