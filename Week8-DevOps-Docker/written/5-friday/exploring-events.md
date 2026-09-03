# Exploring Events in Splunk

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what a Splunk event is and how it is created from raw data
- Describe the default metadata fields (`host`, `source`, `sourcetype`) and their role
- Understand how Splunk extracts timestamps from raw log data
- Distinguish between automatic and manual field extraction
- Use the event detail panel to inspect individual events
- Explain event sampling and when it is used
- Interpret the interesting fields sidebar to discover data structure

---

## Why This Matters

Searches return lists of events. But to write meaningful searches, dashboards, and alerts, you need to understand what an event actually is, what fields it contains, and where those fields come from. This lesson takes you inside the event — the fundamental unit of data in Splunk — and gives you the knowledge to work with your data's structure rather than just its raw text.

---

## What Is a Splunk Event?

An event is a **single record** in Splunk. It is the atomic unit of data — the equivalent of one row in a database table.

In practice, an event often corresponds to:
- One line in a log file
- One HTTP request in an access log
- One JSON object sent via HEC
- One Windows Event Log entry
- One network connection record

Here is an example of a raw log line from a Spring Boot application:

```
2024-01-15 14:23:01.847  INFO 12345 --- [nio-8080-exec-3] c.company.api.PaymentController : Payment processed successfully | orderId=ORD-84729 | userId=usr_4821 | amount=99.99 | durationMs=342
```

When Splunk ingests this line, it creates a single event containing:
- The raw text of the log line
- Extracted metadata fields (timestamp, host, source, sourcetype)
- Any additional fields Splunk can automatically or manually extract (orderId, userId, amount, durationMs)

### Multi-Line Events

Not all events are single lines. Some log formats span multiple lines — Java stack traces are the classic example:

```
2024-01-15 14:23:05.123 ERROR 12345 --- [nio-8080-exec-7] c.company.api.PaymentController : Payment failed
java.sql.SQLException: Connection timed out
    at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:213)
    at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:163)
    at com.company.api.repository.PaymentRepository.save(PaymentRepository.java:87)
    at com.company.api.service.PaymentService.processPayment(PaymentService.java:142)
    at com.company.api.controller.PaymentController.processPayment(PaymentController.java:67)
```

Splunk must recognize that this is ONE event (one log entry with its stack trace), not six separate events. Splunk handles this through **event breaking rules** — configured in `props.conf` for the relevant sourcetype. Common strategies:
- **Line breaking:** Each newline is a new event (appropriate for single-line logs)
- **Regex-based breaking:** A new event begins when a line matches a pattern (e.g., a line starting with a timestamp)
- **Timestamp-based breaking:** A new event begins when Splunk detects a new timestamp

---

## Default Metadata Fields

Every Splunk event has four default fields automatically populated at index time. These fields are always present — you do not need to extract them from the log content.

### `_time` — The Timestamp

**What it is:** The Unix timestamp representing when the event occurred.

**Where it comes from:** Splunk attempts to automatically extract the timestamp from the event content. It recognizes dozens of common timestamp formats including:
- `2024-01-15 14:23:01.847` (ISO-8601 style)
- `15/Jan/2024:14:23:01 +0000` (Apache Combined Log Format)
- `Jan 15 14:23:01` (syslog format)
- `1705320181` (Unix epoch)

**What happens when Splunk cannot find a timestamp?** It uses the time the event was indexed (when it arrived at the Splunk indexer). This is called the "index time" and is noted as a potential issue because events can arrive seconds or even minutes after they occurred — so the indexed time may be slightly later than the actual event time.

**Why `_time` matters:** All time-based filtering, the event timeline, time charts, and trend analysis rely on `_time` being correct. Incorrect timestamps break nearly every form of time-based analysis.

**Searching by time:**
```spl
-- These are equivalent ways to limit by time:
index=prod_api earliest=-1h latest=now
index=prod_api earliest="01/15/2024:14:00:00" latest="01/15/2024:15:00:00"
```

### `host` — The Source Machine

**What it is:** The hostname or IP address of the machine that generated the event.

**Where it comes from:**
- For Universal Forwarder: automatically set to the OS hostname of the server where the forwarder is installed
- For HEC: specified in the JSON payload (the `host` field), or defaults to the client's IP if not specified
- For file monitoring: the hostname of the machine Splunk is installed on

**Why `host` matters:** In a multi-server deployment, `host` is how you identify which specific machine generated an event. When debugging an issue that only affects one server in a cluster, filtering by `host` is essential.

```spl
-- Find errors only on a specific host
index=prod_api host=prod-api-03 level=ERROR

-- Find errors on any prod-api host (wildcard)
index=prod_api host=prod-api-* level=ERROR

-- Compare error counts across hosts
index=prod_api level=ERROR | stats count by host
```

### `source` — The Data Origin

**What it is:** The specific file path, network port, or input name that the event came from.

**Where it comes from:**
- For file monitoring: the full file path (e.g., `/var/log/myapp/application.log`)
- For HEC: the `source` field in the JSON payload, or the input name if not specified
- For network ports: the port specification (e.g., `udp:514`)

**Why `source` matters:** On a server with multiple log files (application log, access log, error log, GC log), `source` tells you which specific file an event came from. If your application writes different log types to different files, `source` is how you distinguish them.

```spl
-- Events from a specific log file
index=prod_api source="/var/log/myapp/application.log"

-- Events from all GC logs across hosts
index=prod_api source="*gc.log"
```

### `sourcetype` — The Data Format Classification

**What it is:** Splunk's classification of what kind of data the event is. The sourcetype determines which parsing rules (timestamp recognition, line breaking, field extraction) Splunk applies to the data.

**Where it comes from:**
- Manually set in the forwarder configuration, HEC payload, or file monitor configuration
- Automatically detected by Splunk if not specified (based on filename patterns and content sampling)

**Why `sourcetype` matters:** It is the single most important piece of metadata for search performance and field extraction accuracy. Always specify sourcetype explicitly in production configurations.

**Common built-in sourcetypes:**
| Sourcetype | Used For |
|---|---|
| `access_combined` | Apache/Nginx HTTP access logs |
| `syslog` | Linux system logs |
| `linux_messages_syslog` | Linux `/var/log/messages` |
| `wineventsecurity` | Windows Security Event Log |
| `json` | Generic JSON data |
| `csv` | Comma-separated values |

**Custom sourcetypes** (you create these):
```
myapp_json          # Your Java app's JSON-formatted logs
payment_service     # Payment service logs with custom format
docker_container    # Container stdout/stderr
```

```spl
-- Best practice: always specify sourcetype
index=prod_payments sourcetype=payment_json level=ERROR

-- Compare volume across sourcetypes
index=prod_api | stats count by sourcetype
```

---

## Automatic Field Extraction

Splunk automatically extracts fields from events based on the sourcetype's configuration. For common, well-known log formats, Splunk ships with pre-built extraction rules.

### JSON Automatic Extraction

For JSON-formatted data, Splunk automatically extracts every key-value pair as a field. This is the biggest reason to use structured JSON logging in your applications — zero configuration needed.

**Raw JSON event:**
```json
{"timestamp":"2024-01-15T14:23:01.847Z","level":"ERROR","service":"payment-service","message":"Payment failed","orderId":"ORD-84729","userId":"usr_4821","errorCode":"DB_TIMEOUT","durationMs":5021}
```

**Automatically extracted fields:**
- `timestamp` = `2024-01-15T14:23:01.847Z`
- `level` = `ERROR`
- `service` = `payment-service`
- `message` = `Payment failed`
- `orderId` = `ORD-84729`
- `userId` = `usr_4821`
- `errorCode` = `DB_TIMEOUT`
- `durationMs` = `5021`

You can immediately search these fields with no configuration:
```spl
index=prod_payments sourcetype=payment_json errorCode=DB_TIMEOUT
index=prod_payments sourcetype=payment_json durationMs>5000
```

### Key-Value Pair Automatic Extraction

For unstructured logs that use consistent `key=value` formatting, Splunk's built-in KV extraction often works automatically:

**Raw event:**
```
2024-01-15 14:23:01 INFO orderId=ORD-84729 userId=usr_4821 amount=99.99 status=SUCCESS durationMs=342
```

Splunk automatically extracts: `orderId`, `userId`, `amount`, `status`, `durationMs` as fields.

### Access Log Automatic Extraction

The `access_combined` sourcetype automatically extracts:
- `clientip` — the requester's IP address
- `method` — HTTP method (GET, POST, etc.)
- `uri` — the requested URI path
- `status` — HTTP status code
- `bytes` — response size in bytes
- `referrer` — the referring URL
- `useragent` — the client's User-Agent string

```spl
-- Find all POST requests returning 500 errors
index=prod_api sourcetype=access_combined method=POST status=500

-- Find slow requests
index=prod_api sourcetype=access_combined | where bytes > 1000000
```

---

## Manual Field Extraction

When Splunk's automatic extraction does not capture a field you need — because your log format is custom or non-standard — you can create manual field extractions.

### The Field Extractor UI

Splunk provides a point-and-click Field Extractor:

1. Find an example event in your search results
2. Click the event to expand it
3. Click **Event Actions → Extract Fields**
4. Highlight a value in the raw event text
5. Name the field
6. Splunk generates a regex and shows which other events in your results match

This generates a **field extraction** saved as a **knowledge object** (covered in a later lesson), which applies to all future searches of that sourcetype.

### Regex-Based Manual Extraction (props.conf / transforms.conf)

For production deployments, field extractions are defined in configuration files rather than the UI. This approach is version-controllable and reproducible.

**Example:** Your application logs look like this:
```
[2024-01-15 14:23:01] PAYMENT | customer=cust_4821 | amount=99.99 | currency=USD | result=SUCCESS
```

To extract `customer`, `amount`, `currency`, and `result`, you would define a regex:

```ini
# In props.conf - apply this extraction to sourcetype "myapp_payment"
[myapp_payment]
EXTRACT-payment_fields = \| customer=(?P<customer>[^\s|]+) \| amount=(?P<amount>[\d.]+) \| currency=(?P<currency>[A-Z]+) \| result=(?P<result>[A-Z]+)
```

After this configuration is applied, every event of that sourcetype automatically has those fields extracted.

---

## The Event Detail Panel

When you click on any event in Splunk's search results, it expands into the **event detail panel**. This is one of the most useful views for understanding your data.

The event detail panel shows:

### Raw Event Text
The complete, unmodified raw text of the event as it was indexed. This is exactly what Splunk received from your log source.

### Extracted Fields Table
Every field Splunk has extracted from this event, in a two-column table:

| Field | Value |
|---|---|
| `_time` | 1/15/2024 2:23:01.847 PM |
| `host` | prod-api-03 |
| `source` | /var/log/myapp/application.log |
| `sourcetype` | myapp_json |
| `level` | ERROR |
| `message` | Payment processing failed |
| `orderId` | ORD-84729 |
| `durationMs` | 5021 |

### Interactive Field Actions
Clicking any field value in the detail panel gives you quick options:
- **Add to search** — appends `fieldname=value` to your current search (find more events like this)
- **Exclude from search** — appends `fieldname!=value` to exclude these events
- **New search** — starts a new search with just this field-value

This interactive workflow lets you drill down into specific subsets of data without typing complex queries manually.

---

## Event Sampling for Large Datasets

When a search returns an extremely large number of events (hundreds of thousands or millions), Splunk may automatically apply **event sampling** — it shows you a representative subset of the results rather than processing every event.

### Why Event Sampling Exists

Full processing of millions of events takes time and computational resources. For initial exploration, you often want a quick answer about the shape of your data, not an exhaustive count. Sampling gives you a statistically representative view quickly.

### How to Recognize Sampling

When sampling is active, you will see a yellow banner or indicator in the search interface stating something like:
> "Showing sampled results (1:100) — results may not be complete"

A ratio of 1:100 means Splunk processed approximately 1% of matching events.

### Disabling Sampling

You can disable sampling in two ways:
1. **Change the sample ratio** using the sampling control in the search interface (set to "No sampling")
2. **Reduce your time window** so the result set is small enough that sampling is not triggered

### Impact on SPL Commands

**Important:** Event sampling only affects the display of raw events. Transforming commands (`stats`, `chart`, `timechart`) are NOT affected by event sampling — they always process all matching events to produce accurate aggregated results. Sampling is only a concern when you are reviewing raw event lists.

---

## The Interesting Fields Sidebar

The fields sidebar on the left side of the Search interface (after a search runs) is divided into two sections:

### Selected Fields
Always displayed with every event. The defaults are `host`, `source`, and `sourcetype`. You can add any field here by clicking it in the sidebar and selecting "Yes" under "Add to all events."

### Interesting Fields
Fields that appear in at least 20% of your result events AND have fewer than 100 unique values.

The "20% and <100 unique values" heuristic is designed to surface fields that are meaningful for analysis — present often enough to be significant, but not so unique (like a UUID) that they are not useful for grouping or filtering.

#### Reading the Interesting Fields Display

For each interesting field, you see:
- **Field name** (clickable)
- **An icon:** `a` for string/text fields, `#` for numeric fields
- **Top values shown inline**

Clicking a field name opens a detail panel showing:
- Top 10 values by count
- Each value's percentage of the total results
- Quick links to search for or exclude specific values

**Example:** If `level` is an interesting field and you click it, you might see:
```
level (interesting field - in 100% of events)
  INFO     72%    (7,200 events)
  WARN     18%    (1,800 events)
  ERROR     8%      (800 events)
  DEBUG     2%      (200 events)
```

This instantly tells you the distribution of log levels in your data without writing any query.

#### Fields NOT Appearing in the Sidebar

Fields with very high cardinality (many unique values) appear only when you search for them explicitly, not in the interesting fields sidebar. Examples:
- `userId` (millions of unique users — too many unique values)
- `requestId` (every request has a unique ID)
- `timestamp` (every event has a different timestamp value)
- `_raw` (the complete raw text of the event)

These fields are still searchable and extractable — they just do not appear as "interesting."

---

## Best Practices for Event Exploration

**1. Start with default fields.** When exploring new data, first understand the distribution of `host`, `source`, and `sourcetype`. Run:
```spl
index=prod_api | stats count by host, sourcetype | sort -count
```

**2. Use the interesting fields sidebar before writing complex queries.** Clicking around in the sidebar often reveals field names and value distributions faster than trying to guess them.

**3. Expand individual events to understand structure.** When working with a new data source, expand 5-10 events and read the extracted fields table to understand the full schema.

**4. Verify timestamp accuracy.** If `_time` does not match your expectations, your searches may miss events in the expected time window.

**5. Use structured logging (JSON) to maximize automatic field extraction.** Every key in a JSON log line becomes a searchable Splunk field with zero configuration. This investment in your application's logging format pays dividends in Splunk usability.

---

## Summary

A Splunk event is a single parsed record derived from raw machine data. Every event has four automatically populated metadata fields: `_time` (when it happened), `host` (where it came from), `source` (which specific file or input), and `sourcetype` (what kind of data it is).

Splunk extracts fields automatically from structured formats (JSON, key-value pairs, access logs) and allows custom field extractions via regex for non-standard formats. The event detail panel gives you interactive access to every field in a single event. The interesting fields sidebar provides a statistical overview of field distributions across your entire result set.

Understanding event structure is what allows you to move from keyword searches to precise field-value searches and, ultimately, to the sophisticated SPL queries covered in the next lesson.

---

## External Resources

- [Splunk Docs: How Timestamp Recognition Works](https://docs.splunk.com/Documentation/Splunk/latest/Data/HowSplunkextractstimestamps)
- [Splunk Docs: About Default Fields](https://docs.splunk.com/Documentation/Splunk/latest/Data/Aboutdefaultfields)
- [Splunk Docs: Create Field Extractions with the Field Extractor](https://docs.splunk.com/Documentation/Splunk/latest/Knowledge/ExtractfieldsinteractivelywithIFX)
