# Weekly Knowledge Check: Week 8 — Splunk & Observability
**Day:** Friday | **Topics:** Splunk Architecture · Data Inputs · Search · SPL · Commands & Functions · Knowledge Objects · Dashboards & Reports

---

## Part 1: Multiple Choice

**Q1.** Which of the following best describes the difference between Splunk Enterprise and Splunk Cloud?

A) Splunk Enterprise supports real-time search; Splunk Cloud does not  
B) Splunk Enterprise is self-hosted and managed by the customer; Splunk Cloud is managed by Splunk as a SaaS offering  
C) Splunk Cloud can only ingest syslog data; Splunk Enterprise supports all source types  
D) Splunk Enterprise requires a forwarder; Splunk Cloud does not  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** Splunk Enterprise is installed and maintained on customer infrastructure, giving full administrative control. Splunk Cloud offloads infrastructure management to Splunk, operating as a Software-as-a-Service platform.  
- **Why A is wrong:** Both editions support real-time search; the difference is deployment model, not search capability.  
- **Why C is wrong:** Both editions support all source types and data inputs.  
- **Why D is wrong:** Both editions can use forwarders; the forwarder requirement is not edition-specific.  
</details>

---

**Q2.** What is the primary role of the SPL pipe character (`|`) in a Splunk search?

A) It marks the beginning of a new search job  
B) It separates index names from search terms  
C) It passes the output of one command as the input to the next command  
D) It triggers an alert when a threshold is crossed  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** The pipe (`|`) is the core of SPL's command chaining syntax — the result set produced by the left-hand command becomes the input data set for the right-hand command, enabling multi-step transformations.  
- **Why A is wrong:** A new search job is started by running a complete search string, not by a pipe character.  
- **Why B is wrong:** Index names are specified with `index=<name>` syntax, not with a pipe.  
- **Why D is wrong:** Alerts are configured through Knowledge Objects or the `alert` action, not the pipe character.  
</details>

---

**Q3.** What does the `sourcetype` field represent in a Splunk event?

A) The IP address of the host that sent the data  
B) The format or category of the data, used to guide parsing and field extraction  
C) The name of the Splunk index where the event is stored  
D) The user account that triggered the event  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `sourcetype` tells Splunk how to interpret and parse incoming data — for example, `access_combined` signals Apache log format, causing Splunk to apply the correct field extractions automatically.  
- **Why A is wrong:** The originating IP or hostname is captured in the `host` field, not `sourcetype`.  
- **Why C is wrong:** The storage location is identified by the `index` field.  
- **Why D is wrong:** User identity (if present) is captured in event-specific fields extracted from the raw data.  
</details>

---

**Q4.** Which search mode in Splunk returns the fewest fields but delivers the fastest results?

A) Verbose mode  
B) Smart mode  
C) Fast mode  
D) Compact mode  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** Fast mode disables field discovery and event list building for transforming commands, returning only the fields needed to compute results — dramatically improving performance for large data sets.  
- **Why A is wrong:** Verbose mode returns all fields and raw event data, making it the slowest but most complete option.  
- **Why B is wrong:** Smart mode automatically chooses behavior based on whether the search is transforming or not; it balances speed and completeness.  
- **Why D is wrong:** "Compact mode" is not a valid Splunk search mode.  
</details>

---

**Q5.** What is the key functional difference between a Universal Forwarder and a Heavy Forwarder?

A) Universal Forwarders can index data locally; Heavy Forwarders cannot  
B) Heavy Forwarders can parse, filter, and route data before forwarding; Universal Forwarders perform minimal processing  
C) Universal Forwarders require a license; Heavy Forwarders are free  
D) Heavy Forwarders only support Windows; Universal Forwarders are cross-platform  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** Heavy Forwarders run a full Splunk instance and can perform data parsing, filtering, routing, and even local indexing before sending to an indexer. Universal Forwarders are lightweight agents that collect and compress data with minimal CPU and memory overhead.  
- **Why A is wrong:** Universal Forwarders do not index data locally; that capability belongs to the Heavy Forwarder or a local indexer.  
- **Why C is wrong:** Both types consume Splunk license volume based on daily ingestion, not the forwarder type itself.  
- **Why D is wrong:** Both forwarder types are cross-platform and run on Windows, Linux, and macOS.  
</details>

---

**Q6.** What does the `rex` command do in SPL?

A) Removes duplicate events from a result set  
B) Extracts fields from raw event data using a regular expression  
C) Renames an existing field to a new name  
D) Sorts results by a regular expression pattern  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `rex` applies a Perl-compatible regular expression to a field (defaulting to `_raw`) and uses named capture groups to create new fields on the fly — for example, `rex field=_raw "user=(?P<username>\w+)"` creates a `username` field.  
- **Why A is wrong:** Removing duplicates is the job of the `dedup` command.  
- **Why C is wrong:** Renaming a field is done with the `rename` command.  
- **Why D is wrong:** Sorting is done with the `sort` command; regular expressions are not used to define sort order.  
</details>

---

**Q7.** What does the `transaction` command group together?

A) Events that share the same sourcetype within a fixed time window  
B) Individual SPL commands into a reusable macro  
C) Related events into a single result based on shared field values and optional time constraints  
D) Dashboard panels into a single report  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** `transaction` correlates multiple events that share one or more field values (such as `session_id`) and optionally constrains grouping by `maxspan`, `maxpause`, or start/end patterns — useful for reconstructing sessions or multi-step workflows.  
- **Why A is wrong:** Sharing a sourcetype alone does not define a transaction; a common field value is required.  
- **Why B is wrong:** Reusable SPL snippets are created with macros, not the `transaction` command.  
- **Why D is wrong:** Dashboard panel grouping is a UI concept unrelated to the `transaction` SPL command.  
</details>

---

**Q8.** What is the primary benefit of the Common Information Model (CIM) in Splunk?

A) It compresses raw event data to reduce storage costs  
B) It normalizes data from different sources into a standard field-naming schema, enabling cross-source searches  
C) It automatically detects and blocks security threats in real time  
D) It replaces the need for field extractions by reading vendor documentation  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** CIM defines standardized data models and field names (e.g., `src_ip`, `dest_ip`, `action`) so that data from firewalls, endpoints, and cloud services can be searched and correlated with a single SPL query, regardless of source format.  
- **Why A is wrong:** CIM is about semantic normalization, not compression; storage management uses different Splunk features.  
- **Why C is wrong:** Threat detection is an application-level capability (e.g., Splunk ES); CIM is the data normalization layer beneath it.  
- **Why D is wrong:** CIM requires field extractions and data model mappings; it does not eliminate that need.  
</details>

---

**Q9.** When configuring a scheduled report in Splunk, which of the following alert notification formats is supported?

A) SMS text message sent directly from the Splunk server  
B) Email with an attached CSV or PDF of the results  
C) Automated phone call to an on-call engineer  
D) Push notification to a mobile browser without any add-on  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** Splunk's built-in alert actions support sending email notifications that can include a rendered PDF or inline/attached CSV of the report results, making it easy to distribute scheduled findings.  
- **Why A is wrong:** Native SMS delivery is not built into Splunk; it requires a third-party add-on or webhook integration.  
- **Why C is wrong:** Automated phone calls require external integrations (e.g., PagerDuty); they are not a native Splunk alert action.  
- **Why D is wrong:** Push notifications to mobile browsers require the Splunk Mobile app or a third-party alerting add-on.  
</details>

---

**Q10.** In Splunk, what is a Saved Search?

A) A search that is stored in the index alongside raw events  
B) A named, reusable SPL query that can be scheduled, shared, and used as the basis for alerts or reports  
C) A caching layer that stores recent search results for 24 hours  
D) A read-only snapshot of a dashboard panel  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** A Saved Search is a Knowledge Object — it stores an SPL query with its time range and settings so it can be re-run on demand, scheduled to run automatically, used to power alerts, or referenced inside reports and dashboards.  
- **Why A is wrong:** Saved Searches are stored in Splunk's configuration files (`savedsearches.conf`), not in data indexes.  
- **Why C is wrong:** Splunk does cache job results, but that is a separate mechanism from Saved Searches, which are query definitions rather than result caches.  
- **Why D is wrong:** Dashboard panels reference searches dynamically; a Saved Search is the query definition, not a frozen snapshot.  
</details>

---

**Q11.** Which SPL command would you use to keep only the top 10 results from a search?

A) `limit 10`  
B) `head 10`  
C) `first 10`  
D) `take 10`  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `head <N>` returns the first N results from the current result set, equivalent to SQL's `LIMIT` clause applied to the top of an ordered list. `tail <N>` performs the complementary operation from the bottom.  
- **Why A is wrong:** `limit` is not a standalone SPL command; result limits in commands like `top` are specified as an argument (e.g., `top limit=10`).  
- **Why C is wrong:** `first` is not a valid SPL command.  
- **Why D is wrong:** `take` is not a valid SPL command.  
</details>

---

**Q12.** In Splunk's field extraction Knowledge Object, what is a field alias used for?

A) To permanently rename a field in the raw event data  
B) To assign an alternative name to an existing extracted field without modifying the data  
C) To create a calculated field using an eval expression  
D) To map a field value to a human-readable label stored in a lookup  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** A field alias creates an additional name that points to an existing field, allowing different source types that use different field names for the same concept to be queried uniformly — the underlying raw data and original field are untouched.  
- **Why A is wrong:** Field aliases do not alter raw event data or remove the original field name; both names remain valid.  
- **Why C is wrong:** Calculated fields using eval expressions are created with the "Calculated Fields" Knowledge Object, not field aliases.  
- **Why D is wrong:** Mapping field values to human-readable labels is the purpose of lookup tables, not field aliases.  
</details>

---

## Part 2: True/False

**Q13.** True or False: In Splunk, the `eval` command can create new fields or overwrite existing fields in the result set.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True  
**Explanation:** `eval` computes an expression and assigns the result to a field name. If the field does not yet exist, it is created; if it already exists, its value is replaced for each result row — making `eval` one of the most versatile SPL commands for data enrichment.  
- **Why False is wrong:** Both behaviors (create and overwrite) are well-documented and commonly used; for example, `eval status_label = if(status==200,"OK","Error")` creates `status_label` from scratch.  
</details>

---

**Q14.** True or False: Field aliases in Splunk change the underlying raw event data stored in the index.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False  
**Explanation:** Field aliases are a search-time Knowledge Object — they apply an alternative name to a field only when a search runs. The raw event bytes stored in the index are never modified; Splunk's architecture separates raw storage from search-time enrichment.  
- **Why True is wrong:** Splunk's raw data is immutable after indexing. Knowledge Objects like aliases, extractions, and lookups enrich data at search time only.  
</details>

---

**Q15.** True or False: A Simple XML dashboard in Splunk auto-refreshes its panels only when the `refresh` attribute is explicitly configured in the XML.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True  
**Explanation:** By default, a Simple XML dashboard loads its search results once when the page opens and does not poll again. To enable periodic refresh, developers must add a `refresh` attribute (in seconds) to the `<dashboard>` element or individual `<search>` blocks.  
- **Why False is wrong:** Without the `refresh` attribute, panels display the results from the initial page load and remain static until the user manually reloads the page or triggers the search again.  
</details>

---

**Q16.** True or False: The `dedup` command in SPL removes events that contain duplicate values in one or more specified fields, keeping only the first occurrence by default.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True  
**Explanation:** `dedup <field>` scans the result set and removes subsequent rows where the specified field value has already been seen, retaining the first occurrence. You can specify multiple fields to deduplicate on a composite key.  
- **Why False is wrong:** This is the documented default behavior. The `keepevents` and `keepempty` options can modify behavior, but the base command keeps the first occurrence and discards later duplicates.  
</details>

---

**Q17.** True or False: In Splunk, the `host` field is automatically extracted from every ingested event and always reflects the DNS hostname of the machine that generated the data.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False  
**Explanation:** While `host` is a default field assigned at index time, its value comes from the forwarder or input configuration — it can be set to an IP address, a custom string, or overridden in `inputs.conf`. It reflects what was configured, not necessarily a DNS-resolved hostname.  
- **Why True is wrong:** Splunk does not perform DNS lookups to populate `host` by default; the value is whatever the sending forwarder or data input reports, which could be an IP or a manually configured name.  
</details>

---

## Part 3: Fill in the Blank

**Q18.** The Splunk data input method that allows applications to send events directly over HTTP or HTTPS using a token-based authentication mechanism is called the __________.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** HTTP Event Collector (HEC)  
**Explanation:** HEC provides a REST endpoint that accepts JSON-formatted events authenticated with a pre-generated token. It is widely used by applications, containers, and cloud services to stream data directly into Splunk without a forwarder.  
- **Why alternatives are wrong:** Syslog inputs use UDP/TCP port 514 without token auth. File monitoring inputs (`monitor://`) watch local files. Neither matches the HTTP token-based description.  
</details>

---

**Q19.** To search for events in Splunk where the field `status` equals `500`, the correct SPL field-value syntax is __________.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `status=500`  
**Explanation:** SPL field-value searches use the `field=value` syntax. For numeric equality, no quotes are required; for string values with spaces, use quotes: `status="internal error"`. This is more efficient than keyword search because Splunk uses the TSIDX index to resolve field-value pairs directly.  
- **Why alternatives are wrong:** `status==500` (double equals) is valid only inside `eval` or `where` expressions. `status:500` is Elasticsearch syntax, not SPL. `WHERE status=500` requires the `where` command prefix.  
</details>

---

**Q20.** In SPL, the command used to extract fields from a field's value using named regular expression capture groups is __________.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `rex`  
**Explanation:** The `rex` command applies a PCRE regular expression and creates new fields from named capture groups using the `(?P<fieldname>pattern)` syntax. Example: `rex field=_raw "user=(?P<username>\w+)"` creates a `username` field.  
- **Why alternatives are wrong:** `extract` is used for automatic field extraction via `props.conf` transforms. `erex` uses example-based extraction and is a different command. `regex` filters events by pattern but does not create fields.  
</details>

---

## Part 4: Code Prediction

**Q21.** Examine the following SPL query:

```spl
index=app_logs
| stats avg(response_time) as avg_ms by endpoint
| sort -avg_ms
```

What does this query return?

A) A list of all raw events sorted by response time in ascending order  
B) The average response time per endpoint, with the slowest (highest average) endpoints listed first  
C) The total count of requests per endpoint, sorted alphabetically  
D) A table of the top 10 endpoints by average response time  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `stats avg(response_time) as avg_ms by endpoint` aggregates all events in `app_logs`, computing the mean `response_time` for each unique `endpoint` value and labeling it `avg_ms`. `sort -avg_ms` then orders results descending (the `-` prefix means descending), so the endpoint with the highest average latency appears first — ideal for identifying slow endpoints.  
- **Why A is wrong:** `stats` transforms raw events into aggregated rows; raw events are no longer visible in the output.  
- **Why C is wrong:** `avg()` computes a mean, not a count; `count` would be used for request counts.  
- **Why D is wrong:** No `head` or `limit` argument is present, so all endpoints are returned, not just the top 10.  
</details>

---

**Q22.** Examine the following SPL query:

```spl
index=web
| top limit=5 uri
```

What does this query return?

A) The 5 most recently accessed URIs with their timestamps  
B) The 5 URIs with the highest number of occurrences, along with their event count and percentage of total events  
C) The 5 URIs with the longest average response time  
D) A random sample of 5 URI values from the result set  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** The `top` command finds the most common values of a field. With `limit=5`, it returns the 5 most frequently occurring `uri` values from the `web` index, automatically adding a `count` column (number of occurrences) and a `percent` column (share of total events) — no additional `stats` command is needed.  
- **Why A is wrong:** `top` ranks by frequency, not recency; timestamps are not part of the output.  
- **Why C is wrong:** Average response time requires `stats avg(response_time) by uri`; `top` only counts occurrences.  
- **Why D is wrong:** Random sampling uses the `sample` command; `top` is deterministic, always selecting the highest-frequency values.  
</details>
