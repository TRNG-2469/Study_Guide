# Interview Questions: Week 8 — Splunk & Observability
**Day:** Friday | **Difficulty Distribution:** 70% Beginner · 25% Intermediate · 5% Advanced

---

## 🟢 Beginner — Foundational Knowledge (Q1–Q13)

### Q1: What is Splunk and what type of data does it analyze?
**Keywords:** Splunk, machine data, log analysis
<details>
<summary>Click to Reveal Answer</summary>

Splunk is a platform for searching, monitoring, and analyzing machine-generated data in real time. It ingests data from sources such as application logs, server metrics, network traffic, and event streams. Splunk indexes this data so it can be searched and visualized quickly. It is commonly used for IT operations, security monitoring, and application observability.
</details>

---

### Q2: What is the difference between Splunk Enterprise and Splunk Cloud?
**Keywords:** Enterprise, Cloud, deployment, SaaS
<details>
<summary>Click to Reveal Answer</summary>

Splunk Enterprise is a self-managed deployment installed on your own infrastructure, giving you full control over hardware, storage, and configuration. Splunk Cloud is a fully managed SaaS offering hosted by Splunk, where infrastructure maintenance and upgrades are handled automatically. Enterprise suits organizations with strict data-residency or customization requirements, while Cloud reduces operational overhead. Both share the same core search and SPL capabilities.
</details>

---

### Q3: What is the HTTP Event Collector (HEC) and how is it used?
**Keywords:** HEC, HTTP, token, JSON
<details>
<summary>Click to Reveal Answer</summary>

The HTTP Event Collector (HEC) is a Splunk feature that accepts event data over HTTP or HTTPS using a token-based authentication mechanism. Applications send JSON-formatted log payloads to a HEC endpoint, and Splunk indexes them without requiring a forwarder agent. HEC is commonly used to ingest data directly from applications such as Spring Boot services configured with a Logback HEC appender. It supports high-throughput batch sending and real-time streaming.
</details>

---

### Q4: What is the difference between a Universal Forwarder and a Heavy Forwarder?
**Keywords:** Universal Forwarder, Heavy Forwarder, parsing, routing
<details>
<summary>Click to Reveal Answer</summary>

A Universal Forwarder is a lightweight agent that collects and forwards raw log data to an indexer with minimal resource consumption; it performs no parsing or indexing locally. A Heavy Forwarder is a full Splunk instance that can parse, filter, route, and even index data before forwarding, making it suitable for preprocessing at the edge. Universal Forwarders are preferred when low overhead is required, while Heavy Forwarders are used when transformation or routing logic is needed near the data source.
</details>

---

### Q5: What are the three default metadata fields present on every Splunk event?
**Keywords:** host, source, sourcetype
<details>
<summary>Click to Reveal Answer</summary>

Every Splunk event automatically has three metadata fields: `host` (the hostname or IP of the machine that generated the event), `source` (the file path, script, or network input that produced the data), and `sourcetype` (a label that describes the format of the data and controls how Splunk parses it). These fields are indexed alongside the raw event text and can always be used in search filters without any additional configuration.
</details>

---

### Q6: What is the SPL pipe character (`|`) used for?
**Keywords:** pipe, SPL, command chaining, results
<details>
<summary>Click to Reveal Answer</summary>

The pipe character (`|`) in SPL passes the output of one command as the input to the next command, allowing you to chain transformations sequentially. For example, `search error | stats count by host | sort -count` first filters events containing "error", then counts them per host, then sorts descending. Each command in the pipeline operates on the result set produced by the previous command. This design mirrors Unix pipe semantics and is fundamental to writing SPL queries.
</details>

---

### Q7: What is the difference between fast, smart, and verbose search modes in Splunk?
**Keywords:** search mode, fast, smart, verbose, field discovery
<details>
<summary>Click to Reveal Answer</summary>

Fast mode prioritizes performance by returning only the fields explicitly referenced in the search, skipping automatic field discovery and event rendering details. Verbose mode returns all fields and event data, performing full field extraction, which gives the richest results but at higher cost. Smart mode is the default and balances the two: it behaves like fast mode for transforming searches (those ending in stats-type commands) and like verbose mode for raw event searches. Choosing the right mode can significantly affect query performance on large data sets.
</details>

---

### Q8: What does the `stats count by status` SPL command return?
**Keywords:** stats, count, group by, aggregation
<details>
<summary>Click to Reveal Answer</summary>

`stats count by status` returns a table with one row per unique value of the `status` field, along with a `count` column showing how many events share that value. It is equivalent to a SQL `SELECT status, COUNT(*) GROUP BY status`. This command is useful for quickly summarizing distributions, such as how many HTTP responses had each status code. The result is a transformed table, not raw events.
</details>

---

### Q9: What does the `rex` command do in SPL?
**Keywords:** rex, regex, field extraction, named capture group
<details>
<summary>Click to Reveal Answer</summary>

The `rex` command extracts new fields from existing field values using regular expressions with named capture groups. For example, `rex field=_raw "user=(?P<username>\w+)"` creates a new field called `username` populated by whatever matches the capture group. It can also be used in sed mode to replace or transform field values. `rex` is particularly useful for parsing unstructured log lines where fields have not been extracted by the sourcetype configuration.
</details>

---

### Q10: What is a knowledge object in Splunk?
**Keywords:** knowledge object, saved search, field extraction, reusability
<details>
<summary>Click to Reveal Answer</summary>

A knowledge object is any reusable configuration that enriches or organizes Splunk data, such as saved searches, field extractions, lookups, event types, tags, field aliases, and calculated fields. Knowledge objects are created once and applied automatically at search time, making searches shorter and more consistent. They can be shared across users and apps within a Splunk instance. Properly managed knowledge objects are the foundation of a scalable and maintainable Splunk environment.
</details>

---

### Q11: What does a field alias do in Splunk, and does it change the raw data?
**Keywords:** field alias, normalization, CIM, raw data
<details>
<summary>Click to Reveal Answer</summary>

A field alias creates an alternate name for an existing extracted field, allowing you to reference different source field names using a single consistent name in searches. For example, aliasing both `user_id` and `userId` to `user` lets a single search work across both data sources. Field aliases do not modify or duplicate the raw data stored in the index; they are applied at search time as a metadata mapping. This makes them a zero-cost normalization technique for CIM compliance.
</details>

---

### Q12: What is a `transaction` in SPL and what does it group?
**Keywords:** transaction, session, grouping, startswith, endswith
<details>
<summary>Click to Reveal Answer</summary>

The `transaction` command groups related events into a single event based on shared field values, such as a session ID or user identifier, optionally bounded by start and end conditions using `startswith` and `endswith`. It calculates fields like `duration` (time between first and last event) and `eventcount` automatically. Transactions are useful for correlating multi-step workflows such as login-to-logout sequences or request-response pairs. They are more flexible than `stats` but less efficient at scale, so `stats` is preferred when only aggregated metrics are needed.
</details>

---

### Q13: What is the CIM (Common Information Model) and why is it useful?
**Keywords:** CIM, normalization, data model, field naming
<details>
<summary>Click to Reveal Answer</summary>

The Common Information Model (CIM) is a Splunk framework that defines standard field names and data models for common categories of data such as network traffic, authentication events, and web activity. By normalizing different data sources to the same CIM field names (for example, always using `src` for source IP regardless of the original field name), searches and dashboards built against CIM work consistently across all compliant data. CIM compliance is achieved through field aliases, extractions, and tags applied via knowledge objects. It is particularly important for Splunk apps like Enterprise Security that rely on standardized field names.
</details>

---

## 🟡 Intermediate — Application & Scenario (Q14–Q17)

### Q14: Your Spring Boot application is sending logs to Splunk HEC but no events appear in the index. What are the first three things you check?
**Keywords:** HEC, token, index, troubleshooting, 400/403
**Hint:** Think about authentication, routing, and network connectivity.
<details>
<summary>Click to Reveal Answer</summary>

First, verify the HEC token is valid and enabled in Splunk Settings, and confirm the application's configured token matches exactly — a 403 response from HEC indicates an invalid token. Second, check that the target index specified in the HEC token configuration (or in the payload's `index` field) exists and that the token has permission to write to it; a 400 error with "unknown index" is the typical symptom. Third, confirm network connectivity and TLS trust between the application and the HEC endpoint — firewalls, self-signed certificate rejection, or a wrong port (default is 8088) are common causes of silent failures. If all three check out, inspect Splunk's `_internal` index for HEC-related errors using `index=_internal sourcetype=splunkd component=HttpInputDataHandler`.
</details>

---

### Q15: You need to find all HTTP 500 errors from the last 24 hours grouped by endpoint, sorted by frequency descending. Write the SPL query.
**Keywords:** stats, count, sort, status, uri_path
**Hint:** Combine a filter, aggregation, and sort in a single pipeline.
<details>
<summary>Click to Reveal Answer</summary>

```
index=app_logs earliest=-24h status=500
| stats count by uri_path
| sort -count
```
This query filters events from the last 24 hours where `status` equals 500, then uses `stats count by uri_path` to group and count occurrences per endpoint, and finally `sort -count` orders the results with the highest frequency first. If the endpoint field has a different name in your data (such as `endpoint` or `request_uri`), substitute accordingly. Adding `| head 20` at the end limits output to the top 20 results for readability.
</details>

---

### Q16: A stakeholder wants a dashboard that auto-refreshes every 5 minutes showing error rate over time, top error messages, and average response time. What panels and visualization types would you use?
**Keywords:** dashboard, Simple XML, timechart, auto-refresh, visualization
**Hint:** Match each metric to the most appropriate chart type.
<details>
<summary>Click to Reveal Answer</summary>

For error rate over time, use a `timechart` panel with a line or area chart — `timechart span=1m count(eval(status>=500)) as errors, count as total` gives a rate when combined with `eval`. For top error messages, use a `stats count by message | sort -count | head 10` search displayed as a bar chart or statistical table. For average response time, use a single-value panel driven by `stats avg(responseTime) as avg_ms`. In Simple XML, set the `auto_refresh` attribute on the `<dashboard>` element to `300` (seconds) to enable the 5-minute refresh. Each panel should have a meaningful title and appropriate time range tokens so stakeholders can drill into specific windows.
</details>

---

### Q17: You have logs from two different applications with different field names for the same concept — one uses `user_id` and another uses `userId`. How do you normalize them for a single search?
**Keywords:** field alias, eval, coalesce, normalization, CIM
**Hint:** Consider both search-time and index-time solutions.
<details>
<summary>Click to Reveal Answer</summary>

The cleanest approach is to create a field alias knowledge object for each sourcetype that maps its native field name to a shared canonical name such as `user` — this requires no SPL changes and applies automatically at search time across all searches. Alternatively, within a single SPL query you can use `eval user=coalesce(user_id, userId)` to create a unified field from whichever source field is populated. For long-term normalization at scale, mapping both fields to a CIM-standard field name (such as `user` in the Authentication data model) ensures all downstream dashboards and reports work uniformly. Index-time field extraction changes should generally be avoided because they require re-indexing existing data.
</details>

---

## 🔴 Advanced — Deep Dive & System Design (Q18)

### Q18: Your production Spring Boot application is experiencing intermittent performance degradation. Using Splunk, describe your complete observability strategy: how you configure Logback/HEC to capture structured JSON logs with MDC fields (traceId, userId, endpoint, responseTime), the SPL queries you would write to detect anomalies (p95 response time spike, error rate increase, specific user impact), the knowledge objects you would create to make this repeatable, and how you would configure a real-time alert that pages on-call when error rate exceeds 5% over a 5-minute window.
**Keywords:** MDC, structured logging, p95, perc95, eval, alert, real-time, knowledge objects, HEC
**Hint:** Walk through instrumentation → ingestion → detection → alerting as four distinct layers.
<details>
<summary>Click to Reveal Answer</summary>

**Instrumentation Layer:** Configure Logback with a `LogstashEncoder` or custom JSON layout so every log line is a flat JSON object. Populate SLF4J MDC at the servlet filter or Spring interceptor level with `traceId`, `userId`, `endpoint`, and `responseTime` (in milliseconds) so these fields appear in every log record. Send logs to Splunk via the `splunk-library-javalogging` HEC appender, targeting a dedicated `app_prod` index with a `json` sourcetype.

**Ingestion and Field Extraction:** Because logs are structured JSON, Splunk's `_json` sourcetype (or a custom sourcetype with `KV_MODE=json`) extracts all MDC fields automatically at index time. Create field aliases to map `responseTime` to the CIM field `duration` for compatibility with standard reports.

**Anomaly Detection SPL:**

- P95 response time: `index=app_prod sourcetype=app_json | timechart span=1m perc95(responseTime) as p95_ms` — a spike above a baseline threshold (e.g., 2x normal p95) signals degradation.
- Error rate: `index=app_prod | timechart span=1m count(eval(level="ERROR")) as errors, count as total | eval error_pct=round(errors/total*100,2)` — values exceeding 5% trigger concern.
- User impact: `index=app_prod level=ERROR | stats count by userId | sort -count` identifies which users are disproportionately affected, helping distinguish a broad outage from a single-account issue.

**Knowledge Objects:** Create a saved search for each SPL query above, store them in a `prod_observability` app, and build calculated fields for `error_pct` so it appears automatically in all searches. Define an event type `slow_request` for events where `responseTime > 2000` to simplify filtering. Create a lookup table mapping `endpoint` to `team_owner` so alerts route to the correct squad.

**Real-Time Alert:** In Splunk, create a real-time alert with a 5-minute rolling window on the error-rate saved search. Set the trigger condition to `error_pct > 5` with a suppress window of 15 minutes to avoid alert storms. Configure the alert action to call a webhook that posts to PagerDuty or OpsGenie, including the `endpoint`, current `error_pct`, and a link to the pre-built dashboard. Set the alert to run as a scheduled search every 1 minute with `earliest=-5m latest=now` if true real-time alerting latency is a concern.
</details>

---
