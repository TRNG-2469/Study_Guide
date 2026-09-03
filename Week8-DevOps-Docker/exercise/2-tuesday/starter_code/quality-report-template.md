# Code Quality Report

> **Template** - Fill in every section with real data from your SonarLint analysis.
> Delete all placeholder text before submitting.

---

## Header

| Field | Value |
|---|---|
| **Project Name** | _e.g., revature-ecommerce-api_ |
| **Repository** | _GitHub URL_ |
| **Date of Analysis** | _YYYY-MM-DD_ |
| **Analyzed by** | _Your full name_ |
| **SonarLint Version** | _Check: IntelliJ -> Plugins -> SonarLint -> version number_ |
| **Java Version** | _e.g., 17.0.11_ |
| **Spring Boot Version** | _e.g., 3.3.4_ |

---

## Section 1 - Executive Summary

_Write one paragraph (4-6 sentences) summarizing the overall quality state of the codebase.
Cover: total number of findings, most concerning category, any patterns you noticed
(e.g., "null handling is consistently weak across service classes"), and your overall
assessment of whether the codebase is ready for a production release._

> **Your summary here:**
>
> [Replace this placeholder with your paragraph]

---

## Section 2 - Finding Summary

### 2a - Counts by Category

| Category | Count | % of Total |
|---|---|---|
| Bug | | |
| Vulnerability | | |
| Code Smell | | |
| Security Hotspot | | |
| **Total** | | 100% |

### 2b - Counts by Severity

| Severity | Count | Action Required |
|---|---|---|
| Blocker | | Must fix before any release |
| Critical | | Fix before production release |
| Major | | Fix within current sprint |
| Minor | | Fix when touching the file |
| Info | | Review and decide |

### 2c - Top Files by Finding Count

| File | Finding Count | Primary Category |
|---|---|---|
| _e.g., ProductService.java_ | | |
| | | |
| | | |

---

## Section 3 - Top 3 Critical Findings

### Finding 3.1 - [Finding Title from SonarLint]

| Field | Value |
|---|---|
| **File** | |
| **Line** | |
| **Category** | Bug / Vulnerability / Code Smell / Security Hotspot |
| **Severity** | Blocker / Critical / Major |
| **SonarLint Rule ID** | _e.g., java:S2259_ |

**Why this is a problem:**
_Explain the risk in plain English. What could go wrong at runtime? What attacker action does this enable? What maintenance burden does this create?_

**Before (problematic code):**
```java
// Paste the original code here
```

**After (fixed code):**
```java
// Paste the fixed code here
```

**Fix verification:** Re-ran SonarLint analysis after fix - finding no longer appears. [ ]

---

### Finding 3.2 - [Finding Title from SonarLint]

| Field | Value |
|---|---|
| **File** | |
| **Line** | |
| **Category** | Bug / Vulnerability / Code Smell / Security Hotspot |
| **Severity** | Blocker / Critical / Major |
| **SonarLint Rule ID** | _e.g., java:S2068_ |

**Why this is a problem:**
_Explain the risk in plain English._

**Before (problematic code):**
```java
// Paste the original code here
```

**After (fixed code):**
```java
// Paste the fixed code here
```

**Fix verification:** Re-ran SonarLint analysis after fix - finding no longer appears. [ ]

---

### Finding 3.3 - [Finding Title from SonarLint]

| Field | Value |
|---|---|
| **File** | |
| **Line** | |
| **Category** | Bug / Vulnerability / Code Smell / Security Hotspot |
| **Severity** | Blocker / Critical / Major |
| **SonarLint Rule ID** | _e.g., java:S138_ |

**Why this is a problem:**
_Explain the risk in plain English._

**Before (problematic code):**
```java
// Paste the original code here
```

**After (fixed code):**
```java
// Paste the fixed code here
```

**Fix verification:** Re-ran SonarLint analysis after fix - finding no longer appears. [ ]

---

## Section 4 - Technical Debt Estimate

SonarLint assigns a remediation time estimate to each finding. Use those estimates to complete this section.

| Category | Estimated Remediation Time |
|---|---|
| All Bugs | _e.g., 2h 30m_ |
| All Vulnerabilities | |
| All Code Smells | |
| All Security Hotspots | |
| **Total Estimated Debt** | |

**Debt rating (circle one):**

- A - Less than 5% of development time to pay off
- B - Between 6% and 10%
- C - Between 11% and 20%
- D - Between 21% and 50%
- E - More than 50%

**Context:** _How does this debt compare to the effort already invested in the project? Is it manageable before Project 3 presentations?_

---

## Section 5 - Recommendations

List at least 3 concrete, actionable recommendations. Each should be specific enough that another developer could act on it without additional clarification.

### Recommendation 1 - [Title]

**Priority:** High / Medium / Low
**Effort:** Hours / Days / Sprint
**Owner:** _Who should do this_

_Describe exactly what should be done, referencing specific files or patterns if possible._

---

### Recommendation 2 - [Title]

**Priority:** High / Medium / Low
**Effort:** Hours / Days / Sprint
**Owner:** _Who should do this_

_Describe exactly what should be done._

---

### Recommendation 3 - [Title]

**Priority:** High / Medium / Low
**Effort:** Hours / Days / Sprint
**Owner:** _Who should do this_

_Describe exactly what should be done._

---

### Additional Recommendations (optional)

_Add more recommendations here if needed._

---

## Sign-off

| Field | Value |
|---|---|
| **Report prepared by** | _Your full name_ |
| **Date** | _YYYY-MM-DD_ |
| **Reviewed by** | _Trainer name (leave blank for trainee submission)_ |
| **Status** | Draft / Final |

_I confirm that all findings in this report are based on actual SonarLint analysis output and that the documented fixes have been verified by re-running SonarLint._

Signature: _________________________ Date: _____________
