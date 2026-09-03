# SPL Commands and Functions Reference

## Learning Objectives

By the end of this lesson, you will be able to:

- Use the `stats` command with its full range of statistical functions
- Apply `eval` functions for conditional logic, string manipulation, and type conversion
- Extract fields from unstructured text using `rex` (regex extraction)
- Group related events into transactions using `transaction`
- Enrich event data using `lookup` tables
- Identify which SPL function category solves a given analysis problem

---

## Why This Matters

The previous lesson gave you the foundational SPL commands. This lesson deepens that knowledge by exploring the functions available inside those commands. The difference between a beginner and an intermediate Splunk user is often just vocabulary — knowing that `perc95()` exists, or that `rex` can extract fields on the fly, or that `lookup` can join your log data with external reference data. These tools let you answer increasingly sophisticated questions from your production data.

---

## The `stats` Command — Complete Function Reference

The `stats` command aggregates data. Its power comes from the range of functions available inside it.

### Count Functions

```spl
-- count: total number of events
index=prod_api | stats count

-- count with field: number of events where the field is not null
index=prod_api | stats count(errorCode) as events_with_errors

-- distinct_count (dc): number of unique values
-- "How many unique users made requests in the last hour?"
index=prod_api earliest=-1h
| stats dc(userId) as unique_users

-- dc across multiple grouping fields
index=prod_api earliest=-24h
| stats dc(userId) as unique_users by endpoint, level
```

### Statistical Functions

```spl
-- avg: arithmetic mean
index=prod_api
| stats avg(durationMs) as mean_response_ms by endpoint

-- median: middle value (50th percentile)
index=prod_api
| stats median(durationMs) as median_response_ms by endpoint

-- stdev: standard deviation (how spread out values are)
-- High stdev means very inconsistent response times
index=prod_api
| stats avg(durationMs) as mean_ms, stdev(durationMs) as stdev_ms by endpoint

-- percentiles: perc<N> where N is 1-99
-- p95 and p99 are the industry standard for latency SLOs
index=prod_api
| stats avg(durationMs) as avg_ms,
        perc50(durationMs) as p50_ms,
        perc95(durationMs) as p95_ms,
        perc99(durationMs) as p99_ms
  by endpoint

-- max and min: extreme values
index=prod_api
| stats min(durationMs) as fastest_ms, max(durationMs) as slowest_ms by endpoint

-- sum: total
-- "What is the total revenue processed in the last 24 hours?"
index=prod_payments sourcetype=payment_json result=SUCCESS earliest=-24h
| stats sum(amount) as total_revenue
```

### Aggregation Functions for Multi-Value Fields

```spl
-- values: all UNIQUE values of a field, collected into a multivalue field
-- "What error codes appeared on each endpoint?"
index=prod_api level=ERROR
| stats values(errorCode) as error_codes by endpoint

-- list: all values INCLUDING duplicates (can be very large)
index=prod_api level=ERROR
| stats list(message) as all_messages by endpoint

-- first and last: first/last value seen in time order
index=prod_api
| stats first(status) as initial_status, last(status) as final_status by sessionId
```

### Using Multiple Stats Functions Together

```spl
-- Comprehensive API health dashboard data
index=prod_api sourcetype=myapp_json earliest=-1h
| stats count as total_requests,
        sum(eval(if(level="ERROR", 1, 0))) as error_count,
        avg(durationMs) as avg_ms,
        perc95(durationMs) as p95_ms,
        max(durationMs) as max_ms,
        dc(userId) as unique_users
  by endpoint
| eval error_rate_pct = round((error_count / total_requests) * 100, 2)
| sort -error_count
```

---

## The `eval` Command — Complete Function Reference

`eval` computes expressions and creates or modifies fields. It has a large library of built-in functions.

### Conditional Functions

#### `if(condition, true_value, false_value)`

The simplest conditional — like a ternary operator.

```spl
-- Flag slow requests
index=prod_api
| eval is_slow = if(durationMs > 2000, "SLOW", "OK")

-- Convert boolean numeric to readable string
index=prod_api
| eval status_label = if(status >= 200 AND status < 300, "Success", "Non-Success")
```

#### `case(condition1, value1, condition2, value2, ..., true(), default_value)`

Multi-branch conditional — like switch/case or if-else-if. The `true()` at the end is the "else" clause (always evaluates to true, so it catches anything not matched above).

```spl
-- Classify HTTP status codes
index=prod_api sourcetype=access_combined
| eval status_class = case(
    status >= 500, "5xx Server Error",
    status >= 400, "4xx Client Error",
    status >= 300, "3xx Redirect",
    status >= 200, "2xx Success",
    true(),        "Unknown"
  )
| stats count by status_class
```

```spl
-- Classify response time tiers for SLO tracking
index=prod_api
| eval slo_tier = case(
    durationMs < 100,  "Excellent (<100ms)",
    durationMs < 500,  "Good (<500ms)",
    durationMs < 2000, "Acceptable (<2s)",
    durationMs < 5000, "Degraded (<5s)",
    true(),            "Critical (>5s)"
  )
| stats count by slo_tier
| sort slo_tier
```

#### `coalesce(field1, field2, ...)`

Returns the first non-null value from the list. Useful when the same concept is stored in different fields across sourcetypes.

```spl
-- Some events have "userId", others have "user_id" -- normalize them
index=prod_api
| eval canonical_user_id = coalesce(userId, user_id, uid, "unknown")
```

### Math Functions

```spl
-- round(value, precision): round to N decimal places
| eval avg_rounded = round(avg_ms, 2)

-- floor and ceil
| eval duration_seconds_floor = floor(durationMs / 1000)
| eval duration_seconds_ceil  = ceil(durationMs / 1000)

-- abs: absolute value
| eval deviation = abs(actual - expected)

-- pow: exponentiation
| eval squared = pow(value, 2)

-- log: natural log; log(value, base) for other bases
| eval log_duration = log(durationMs, 10)
```

### String Functions

```spl
-- len: string length
| eval message_length = len(message)
| where len(userId) > 0    -- filter out empty userId strings

-- lower and upper: case conversion
| eval level_lower = lower(level)
| eval endpoint_upper = upper(endpoint)

-- substr(string, start, length): extract substring
-- (Splunk substr uses 1-based indexing, length is optional)
| eval first_3_chars = substr(errorCode, 1, 3)

-- replace(string, regex, replacement): regex-based replacement
| eval sanitized_message = replace(message, "\d{16}", "CARD_REDACTED")

-- trim, ltrim, rtrim: remove whitespace (or specific characters)
| eval clean_value = trim(raw_value)

-- split(string, delimiter): split into multivalue field
| eval tags_list = split(tags_csv, ",")

-- mvcount: count elements in a multivalue field
| eval tag_count = mvcount(tags_list)

-- mvindex(mvfield, index): get one element from multivalue (0-based)
| eval first_tag = mvindex(tags_list, 0)

-- mvjoin: join multivalue field back to string
| eval tags_display = mvjoin(tags_list, " | ")
```

### Type Conversion Functions

```spl
-- tonumber: convert string to number
| eval amount_num = tonumber(amount_str)
| eval amount_num = tonumber(hex_value, 16)   -- with base (hex in this case)

-- tostring: convert number to string
| eval status_str = tostring(status_code)
| eval formatted = tostring(large_number, "commas")  -- "1,234,567"
| eval duration_str = tostring(durationMs) + "ms"

-- strptime: parse a string into a Unix timestamp
| eval event_epoch = strptime(timestamp_str, "%Y-%m-%dT%H:%M:%S")

-- strftime: format a Unix timestamp into a string
| eval readable_time = strftime(_time, "%Y-%m-%d %H:%M:%S")
| eval date_only = strftime(_time, "%Y-%m-%d")
| eval hour_of_day = strftime(_time, "%H")
```

### Validation Functions

```spl
-- isnull(field): true if field does not exist or has no value
| where isnull(errorCode)          -- events without an error code

-- isnotnull(field): true if field exists and has a value
| where isnotnull(userId)

-- isnum(field): true if field value is numeric
| where isnum(durationMs)

-- isstr(field): true if field value is a string
| eval type_check = if(isstr(amount), "STRING - potential issue", "OK")

-- match(string, regex): true if string matches regex
| where match(endpoint, "^/api/v[12]/")  -- only v1 or v2 API endpoints
| where match(message, "(?i)timeout")    -- case-insensitive timeout search
```

---

## The `rex` Command — Regex Field Extraction

`rex` applies a regular expression to an event field and extracts named capture groups as new fields. It is the on-the-fly version of field extraction — you do not need to create a permanent knowledge object; you just include `rex` in your search.

**Syntax:** `| rex field=<source_field> "<regex_with_named_groups>"`

Named capture groups use Python regex syntax: `(?P<field_name>pattern)`

### Example 1: Extract fields from an unstructured log

**Raw event `_raw`:**
```
2024-01-15 14:23:01 [PAYMENT] Processing order=ORD-84729 customer=cust_4821 amount=99.99 currency=USD
```

```spl
index=prod_payments
| rex field=_raw "order=(?P<orderId>[A-Z0-9-]+) customer=(?P<customerId>[a-z0-9_]+) amount=(?P<amount>[\d.]+)"
| table _time, orderId, customerId, amount
```

After this `rex`, every event in the results has three new fields: `orderId`, `customerId`, and `amount`, extracted from the raw text.

### Example 2: Extract HTTP path segments

**URI field value:** `/api/v2/users/usr_4821/orders/ORD-84729`

```spl
index=prod_api sourcetype=access_combined
| rex field=uri "/api/v\d+/(?P<resource>[^/]+)/(?P<resourceId>[^/]+)"
| stats count by resource
```

Extracts `resource=users` and `resourceId=usr_4821` from the URI.

### Example 3: Extract stack trace exception type

**Raw event containing a Java stack trace:**
```
ERROR - Payment failed
java.sql.SQLException: Connection timed out after 5000ms
    at com.zaxxer.hikari...
```

```spl
index=prod_api level=ERROR
| rex field=_raw "(?P<exception_class>[A-Za-z.]+Exception)[:\s](?P<exception_message>[^\n]+)"
| stats count by exception_class, exception_message
| sort -count
```

### Example 4: Repeated `rex` for multiple fields

```spl
index=prod_api
| rex field=_raw "userId=(?P<userId>[a-z0-9_]+)"
| rex field=_raw "orderId=(?P<orderId>[A-Z0-9-]+)"
| rex field=_raw "durationMs=(?P<durationMs>\d+)"
| eval durationMs = tonumber(durationMs)
| where durationMs > 1000
```

Note: Fields extracted by `rex` are strings by default — use `tonumber()` for numeric comparisons.

---

## The `transaction` Command

`transaction` groups multiple related events into a single transaction event. It is used when a business process spans multiple log entries and you want to analyze the complete process as one unit.

**Syntax:** `| transaction field1, [field2, ...] [startswith=<condition>] [endswith=<condition>] [maxspan=<time>] [maxevents=<N>]`

**Key options:**

| Option | Description | Example |
|---|---|---|
| `field` | Group events sharing these field values | `transaction sessionId` |
| `startswith` | Event that begins a transaction | `startswith="login"` |
| `endswith` | Event that ends a transaction | `endswith="logout"` |
| `maxspan` | Maximum time a transaction can span | `maxspan=30m` |
| `maxpause` | Max time between events in a transaction | `maxpause=5m` |
| `maxevents` | Max events per transaction | `maxevents=10` |

After `transaction`, each result event represents one complete transaction with new fields:
- `duration` — time in seconds from first to last event in the transaction
- `eventcount` — number of events in the transaction
- `_time` — timestamp of the FIRST event
- `_raw` — concatenation of all raw events in the transaction

### Example 1: Track a checkout flow

```spl
-- Find all events for each user session, measure how long checkout takes
index=prod_ecommerce sourcetype=ecommerce_json earliest=-24h
| transaction sessionId maxspan=1h
| where eventcount >= 3          -- sessions with at least 3 events
| stats avg(duration) as avg_session_seconds,
        avg(eventcount) as avg_events_per_session
```

### Example 2: Measure database query transaction duration

```spl
-- Group DB events by query ID to measure total query time including retries
index=prod_api sourcetype=db_audit earliest=-1h
| transaction queryId startswith="QUERY_START" endswith="QUERY_END" maxspan=60s
| where duration > 5             -- transactions taking more than 5 seconds
| stats count as slow_queries, avg(duration) as avg_seconds
```

### Example 3: Detect failed login sequences

```spl
index=prod_auth sourcetype=auth_json earliest=-1h
| transaction userId maxspan=5m maxevents=10
| where match(_raw, "login_failed.*login_failed.*login_failed")  -- 3+ failures
| table _time, userId, duration, eventcount
```

**Important:** `transaction` can be slow and memory-intensive on large datasets. For pure counting or timing analysis, prefer `stats` with `first()`, `last()`, or time-based expressions when possible. Use `transaction` when you genuinely need the grouped event view.

---

## The `lookup` Command — Enriching Events with Reference Data

`lookup` joins your Splunk events with an external reference table (a CSV file or database table) to add contextual information not present in the raw logs.

**Syntax:** `| lookup <lookup_name> <input_field> [AS <alias>] [OUTPUT <output_field> [AS <alias>]]`

### Why Use Lookups?

Your application logs contain IDs (userId, productId, regionCode) but not the human-readable names associated with them. Lookups let you join log data with reference data to answer questions like: "Which product category had the most errors?" when your logs only contain `productId`.

### Setting Up a CSV Lookup

1. Create a CSV file with a key column and descriptive columns:

**`user_regions.csv`:**
```csv
userId,region,tier,sales_rep
usr_4821,Northeast,Enterprise,Sarah Johnson
usr_2299,Southwest,SMB,Mike Chen
usr_9103,Midwest,Enterprise,Sarah Johnson
```

2. Upload it in Splunk: **Settings → Lookups → Lookup Table Files → Add New**
3. Define the lookup: **Settings → Lookups → Lookup Definitions → Add New**
   - Name: `user_regions`
   - File: `user_regions.csv`
   - Key fields: `userId`

### Using the Lookup

```spl
-- Enrich error events with user region information
index=prod_api level=ERROR earliest=-24h
| lookup user_regions userId OUTPUT region, tier, sales_rep
| stats count as error_count by region, tier
| sort -error_count
```

```spl
-- Find which sales reps have the most affected users
index=prod_api level=ERROR earliest=-24h
| dedup userId
| lookup user_regions userId OUTPUT sales_rep
| stats count as affected_users by sales_rep
| sort -affected_users
```

### The `inputlookup` Command

`inputlookup` reads an entire lookup table as search results (without joining to events):

```spl
-- See all contents of a lookup table
| inputlookup user_regions.csv

-- Filter the lookup table
| inputlookup user_regions.csv where tier="Enterprise"
```

Useful for verifying lookup contents or using a lookup table as the starting point for analysis.

### Automatic Lookups

You can configure Splunk to apply a lookup automatically whenever a specific sourcetype is searched — you never have to type `| lookup` in your queries. Configured under **Settings → Lookups → Automatic Lookups**.

---

## SPL Function Categories Overview

SPL's eval functions are organized into categories. Knowing the categories helps you find the right function when solving a new problem.

| Category | Purpose | Example Functions |
|---|---|---|
| **Mathematical** | Arithmetic, rounding, logarithms | `round()`, `floor()`, `ceil()`, `abs()`, `pow()`, `log()`, `sqrt()` |
| **Statistical** | Aggregation in `stats` | `avg()`, `count()`, `dc()`, `max()`, `min()`, `sum()`, `perc<N>()`, `stdev()` |
| **String** | Text manipulation | `len()`, `lower()`, `upper()`, `substr()`, `replace()`, `trim()`, `split()` |
| **Conversion** | Type and format conversion | `tonumber()`, `tostring()`, `strptime()`, `strftime()` |
| **Conditional** | Branching logic | `if()`, `case()`, `coalesce()`, `nullif()` |
| **Multivalue** | Work with multi-value fields | `mvcount()`, `mvindex()`, `mvappend()`, `mvjoin()`, `mvfilter()` |
| **Validation** | Check field types and existence | `isnull()`, `isnotnull()`, `isnum()`, `isstr()`, `match()` |
| **Cryptographic** | Hashing | `md5()`, `sha1()`, `sha256()` |
| **Date/Time** | Time math and formatting | `strptime()`, `strftime()`, `now()`, `relative_time()` |

### Practical Category Examples

**Date/Time — Working with time arithmetically:**
```spl
-- Events within the last 2 hours of each event's own timestamp
-- (useful in transaction analysis)
index=prod_api
| eval two_hours_ago = relative_time(_time, "-2h")
| where _time > two_hours_ago

-- Add a "day of week" field for traffic pattern analysis
index=prod_api
| eval day_of_week = strftime(_time, "%A")   -- Monday, Tuesday, etc.
| stats avg(durationMs) as avg_ms by day_of_week
```

**Multivalue — Working with fields that have multiple values:**
```spl
-- A user's event types over a session (multivalue field from stats values())
index=prod_auth
| stats values(event_type) as event_types by userId, sessionId
| eval event_count = mvcount(event_types)
| eval had_error = if(mvfind(event_types, "error") >= 0, "YES", "NO")
| where had_error = "YES"
```

**Cryptographic — Anonymize sensitive data in reports:**
```spl
-- Hash userId for sharing data without exposing PII
index=prod_api
| eval userId_hashed = sha256(userId)
| stats count by userId_hashed, endpoint
```

---

## Summary

This lesson expanded your SPL vocabulary across five key areas:

- **`stats` functions:** From basic `count` and `avg` to `perc95`, `dc`, and `stdev` for statistical depth
- **`eval` functions:** Conditional logic with `if` and `case`; string manipulation; type conversion; datetime formatting
- **`rex`:** On-the-fly regex field extraction from unstructured text — no permanent configuration needed
- **`transaction`:** Grouping related events into a single record for end-to-end process analysis
- **`lookup`:** Enriching events with reference data to add business context to operational logs

Together, these tools let you move from basic "find me errors" queries to sophisticated "calculate the p95 response time per endpoint per region, enriched with customer tier data" analysis. The function category table in this lesson is a reference you will return to — bookmark it.

---

## External Resources

- [Splunk Docs: Stats Command and Functions](https://docs.splunk.com/Documentation/Splunk/latest/SearchReference/Stats)
- [Splunk Docs: Eval Functions Complete Reference](https://docs.splunk.com/Documentation/Splunk/latest/SearchReference/CommonEvalFunctions)
- [Splunk Docs: Rex Command](https://docs.splunk.com/Documentation/Splunk/latest/SearchReference/Rex)
