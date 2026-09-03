# Lab: Building Observability for the Spring Boot API

**Duration:** 3-4 hours
**Mode:** Individual (Hybrid — configuration + implementation + analysis)
**Prerequisites:**
- Splunk free trial account (cloud.splunk.com) OR Splunk Enterprise running locally via Docker
- Project 3 Spring Boot API application (source code accessible)
- Postman installed and configured with Project 3 request collection
- Java 17+, Maven 3.8+ installed locally

**Learning Objectives:**
- Configure an HTTP Event Collector (HEC) token in Splunk and validate it via curl
- Integrate Splunk Logback appender into a Spring Boot application for structured JSON log shipping
- Write SPL queries to isolate 5xx errors, compute request rates, and analyze response time distributions
- Save a recurring scheduled report with email delivery
- Build a multi-panel Splunk dashboard with auto-refresh and color-coded thresholds
- Configure a threshold-based alert that triggers and delivers an email notification

---

## Scenario

The ops team has deployed the Project 3 API to production (on the EC2 instance from Monday).
Now they need visibility into: how many errors occur per minute, which endpoints are slowest,
and an alert when the error rate gets too high. Your job: instrument the app with Splunk and
build the monitoring dashboard.

---

## PART 1 — Configure HTTP Event Collector in Splunk (~20 min)

The HTTP Event Collector (HEC) is Splunk's mechanism for receiving log events pushed from
applications over HTTPS. Before the Spring Boot app can send logs, Splunk needs a token that
authenticates those incoming events.

### Step 1.1 — Create an HEC Token

1. Log in to your Splunk instance (Splunk Cloud or local Docker).
2. Navigate to: **Settings → Data Inputs → HTTP Event Collector → New Token**
3. Fill in the wizard:
   - **Name:** 
   - **Source type:**   <- important: logs will arrive as JSON
   - **Default Index:** 
4. Click **Submit** on the final screen.
5. **Copy the generated token value immediately** — it is shown only once.
   Store it somewhere safe (e.g., a local  file — never commit it to git).

> **Why _json?** Selecting  as the sourcetype tells Splunk to parse the incoming
> payload as JSON and index each top-level key as a separate field. This makes your
> , , and  fields searchable without any further extraction.

### Step 1.2 — Note Your HEC Endpoint

The endpoint URL depends on where Splunk is running:

- **Splunk Cloud:**
  
- **Local Docker (Splunk Enterprise):**
  

Test that the token works before touching any Java code:



**Expected response:**


If you see , the token was copied incorrectly.
If the curl command hangs, the HEC port (8088) may be blocked by a firewall or security group rule.

> **Checkpoint 1:**  returns  with your actual token.

---

## PART 2 — Configure Spring Boot Logback for HEC (~30 min)

### Step 2.1 — Add the Maven Dependency

Open your Project 3  and add the following inside :



Run  to confirm Maven can download both artifacts.

### Step 2.2 — Configure logback-spring.xml

A starter template is provided at .
Copy it into your project at:


Open the file and fill in every  comment:

1. Set  — read from the environment variable 
2. Set  — read from the environment variable 
3. Set  to 
4. Set  to 
5. Set  to 
6. Set  to  (flush after every 10 events during development)

### Step 2.3 — Add Structured Logging to the Spring Boot App

Structured logging means attaching key-value pairs to every log statement so Splunk can
index them as individual fields rather than raw strings.

**Add a request-tracing filter** (create  in your filters package):



**Log structured events in your service layer:**



### Step 2.4 — Start the App and Generate Events

Set the environment variables, then start:



Using Postman, make at least 10 API calls — mix of:
- Valid GETs that succeed (status 200)
- POSTs with invalid request bodies (status 400)
- Requests to non-existent endpoints (status 404)
- At least 2 requests that trigger a 500 (force an exception or remove a required bean temporarily)

Then verify in Splunk:

1. Go to **Search and Reporting**
2. Run: 
3. You should see your request events appearing in the event list.

> **Checkpoint 2:** Events are visible in Splunk with .
> Click one event and confirm , , and  appear
> in the field sidebar on the left.

---

## PART 3 — Search for 5xx Errors (~20 min)

Run each search in Splunk's Search and Reporting app. Record the output after each.

**Search 1 — All ERROR-level log lines in the last hour:**

Record the event count and paste the first 3 rows here:


**Search 2 — Only HTTP 5xx errors:**

Record the event count and paste the first 3 rows here:


**Search 3 — Error count grouped by endpoint:**

Record all rows returned (this is a summary — should be short):


> **Checkpoint 3:** At least 2 of the 3 searches return results. If all return zero events,
> revisit Part 2 — the Logback appender may not be flushing. Check the Spring Boot console
> for any  error messages.

---

## PART 4 — Write SPL to Compute Requests Per Minute (~30 min)

In this part you write SPL queries from scratch. The starter file
 provides the first pipe command of each query —
you supply the rest.

### Task 4.1 — Basic Request Rate

Write SPL that shows the number of requests per minute over the last hour.

Hint: use . Your final query should start with a base search
and pipe into timechart.

Paste your final SPL here:


Describe the shape of the timechart output (was there a spike when you ran Postman?):


### Task 4.2 — Error Rate Percentage

Write SPL that shows what percentage of all requests resulted in an error
(level=ERROR or status_code>=500). Include your formula logic as a comment.



Paste the percentage your query returned:


### Task 4.3 — Response Time Distribution

Write SPL using the  field to compute the following per endpoint:
average, p50, p95, p99, and maximum response times.



Paste the result table here:


---

## PART 5 — Save as Scheduled Report (~20 min)

### Task 5.1 — Save the Error Rate Search

1. Run the error rate percentage query from Task 4.2.
2. Click **Save As → Report**.
3. Configure:
   - **Name:** 
   - **Description:** 
   - **Time Range:** Last 60 minutes
4. On the next screen, click **Schedule**.
5. Set schedule to **Every 5 minutes** (for this lab — in production this would be hourly or daily).
6. Under **Actions**, enable **Send Email** and enter your email address.
7. Save.

> **Why 5 minutes for this lab?** In production you would schedule reports daily or hourly
> to avoid alert fatigue and control compute costs. 5 minutes here lets you verify
> delivery before the lab session ends.

### Task 5.2 — Verify the Scheduled Report

1. Navigate to **Reports** in the top navigation bar.
2. Find  in the list.
3. Click **Run Now** in the Actions column.
4. Confirm the report results appear without error.

> **Checkpoint 5:** The report appears in the Reports list and "Run Now" produces a results
> page. Record the report URL here: 

---

## PART 6 — Build a 3-Panel Dashboard (~40 min)

### Task 6.1 — Create the Dashboard

1. Navigate to **Dashboards → Create New Dashboard**.
2. Fill in:
   - **Title:** 
   - **Description:** 
   - **Permissions:** Private (for now)
3. Choose **Classic Dashboard** (Simple XML) for maximum compatibility.

### Task 6.2 — Panel 1: Error Rate Over Time

1. Click **Add Panel → New → Line Chart**.
2. Enter the SPL:

3. Set panel title: 
4. Click **Apply**.

### Task 6.3 — Panel 2: Top Error Messages

1. Click **Add Panel → New → Statistics Table**.
2. Enter the SPL:

3. Set panel title: 
4. Click **Apply**.

### Task 6.4 — Panel 3: Average Response Time (Single Value)

1. Click **Add Panel → New → Single Value**.
2. Enter the SPL:

3. Set panel title: 
4. Click the **Format** tab and add color thresholds:
   - Green: 
   - Yellow: 
   - Red: 
5. Click **Apply**.

### Task 6.5 — Enable Auto-Refresh

1. Click **Edit Dashboard** (top right).
2. Set **Refresh** to **Every 5 minutes**.
3. Click **Save**.

> **Checkpoint 6:** All 3 panels display data and the dashboard header shows the
> auto-refresh countdown. Take a screenshot of your completed dashboard and attach it
> to your submission.

---

## PART 7 — Configure an Alert (~20 min)

### Task 7.1 — Create a Threshold Alert

1. Navigate to **Alerts → Create Alert**.
2. Fill in:
   - **Name:** 
   - **Description:** 
3. Paste the SPL:

4. **Alert Type:** Scheduled
5. **Schedule:** Every 5 minutes
6. **Trigger Condition:** Number of Results > 0
   (The WHERE clause already filters so results only exist when count exceeds 10)
7. **Actions → Add Actions → Send Email:**
   - To: your email address
   - Subject: 
   - Message: 
8. Save.

### Task 7.2 — Test the Alert

Generate more than 10 errors rapidly so the alert fires on the next evaluation:

Request 1 sent
Request 2 sent
Request 3 sent
Request 4 sent
Request 5 sent
Request 6 sent
Request 7 sent
Request 8 sent
Request 9 sent
Request 10 sent
Request 11 sent
Request 12 sent
Request 13 sent
Request 14 sent
Request 15 sent

Wait up to 5 minutes for the next alert evaluation window, then:

1. Navigate to **Alerts → Triggered Alerts**.
2. Confirm  appears in the list.
3. Check your email for the alert notification.

> **Checkpoint 7:** The alert appears in Triggered Alerts. Record the trigger timestamp: 

---

## Definition of Done

Before submitting, confirm every item below:

- [ ] HEC token created and curl test returns 
- [ ] Spring Boot app starts with HEC environment variables set and sends structured JSON logs
- [ ] Events visible in Splunk with 
- [ ] , , and  appear as indexed fields in event detail
- [ ] All three error searches in Part 3 return results
- [ ] SPL queries for requests/min, error rate percentage, and response time written and pasted above
- [ ] Scheduled report saved, appears in Reports list, and "Run Now" succeeds
- [ ] 3-panel dashboard built with all panels showing data and auto-refresh enabled
- [ ] Alert configured, triggered at least once, and email received
- [ ] Dashboard screenshot attached to submission

---

## Reflection Questions

Answer each question in 3-5 sentences.

**Q1 — Deployment visibility:**
During a bad deployment where a broken container was pushed to production, what would a
Splunk dashboard have shown in the minutes immediately after deployment? Which panel would
have lit up first — the error rate line chart or the avg response time single value?
Explain your reasoning based on how the two metrics are computed.

**Q2 — Alert tuning:**
The alert you built triggers when  in 5 minutes. In a real production
system serving 10,000 requests per minute, why is a raw count threshold insufficient?
What metric would you use instead, and how would you write that SPL?

**Q3 — Structured vs. unstructured logs:**
You added MDC fields and structured key-value arguments to emit JSON. Before this change,
what would have been different about the Splunk searches you wrote? Which Part 3 or Part 4
query would have been impossible or significantly harder with plain-text logs?

**Q4 — Observability in Project 3:**
Looking at Project 3 grading criteria, which two dashboard panels from Part 6 give the
most evidence that your API meets its non-functional requirements for performance and
reliability? How would you frame those panels in a demo to stakeholders who are not
engineers?
