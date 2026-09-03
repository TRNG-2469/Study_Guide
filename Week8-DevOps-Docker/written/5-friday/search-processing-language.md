# Search Processing Language (SPL) Fundamentals

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain the pipe (`|`) concept and how SPL chains commands
- Use the core SPL commands: `search`, `table`, `stats`, `eval`, `where`, `sort`, `head`, `tail`, and `dedup`
- Rename fields using the `as` keyword
- Recognize and apply common SPL patterns for application log analysis
- Read and explain multi-command SPL queries line by line

---

## Why This Matters

SPL is Splunk's query language — the tool that transforms raw log data into answers. Knowing how to click around the Splunk UI gets you started, but writing SPL queries is what lets you answer complex operational questions: "What are the top 10 slowest API endpoints over the past week?" "Which users triggered the most failed login attempts today?" "Is the error rate higher on server A than server B?" SPL is the language of observability analysis, and by the end of this lesson you will be able to write it fluently for common developer use cases.

---

## The Pipe Concept — The Heart of SPL

If you have used Unix command-line tools, you already understand the core concept of SPL. In Unix, the pipe (`|`) passes the output of one command as the input to the next:

```bash
cat /var/log/app.log | grep ERROR | sort | uniq -c
```

SPL works identically. A search starts with an initial search that fetches events from the index, then pipes those events through a series of commands that transform, filter, or aggregate them:

```spl
index=prod_api level=ERROR
| stats count by endpoint
| sort -count
| head 10
```

Read this query aloud:
1. "Get all ERROR-level events from prod_api..."
2. "...count them, grouped by endpoint..."
3. "...sort descending by count..."
4. "...keep only the top 10 rows."

Each `|` passes the entire dataset to the next command. Commands do not fetch from disk — they operate on the in-memory dataset passed to them by the previous step.

### Two Categories of SPL Commands

**Streaming commands** — operate on one event at a time and pass each event through immediately. Examples: `where`, `eval`, `rename`, `rex`. These can handle any volume of data.

**Transforming commands** — consume ALL events from the previous step and produce a NEW, summarized dataset. Examples: `stats`, `chart`, `timechart`, `top`. After a transforming command, you are no longer working with individual events — you are working with summary rows.

This distinction matters because after a `stats` command, you cannot reference original event fields that were not in your `stats` clause — they have been summarized away.

---

## The Initial Search (Before the First Pipe)

The part of an SPL query before the first `|` is the **initial search**. It retrieves events from Splunk's indexes using keyword and field-value search syntax.

Best practices for initial searches:

1. **Always specify `index=`** — restricts the search to relevant data
2. **Specify `sourcetype=`** when you know it — further narrows scope
3. **Use the most specific field-value filters you can** — the narrower the initial search, the fewer events are passed to downstream commands, and the faster everything runs
4. **Use time range from the picker OR `earliest`/`latest` in the query** — but not both (they can conflict)

```spl
-- Good: specific index, sourcetype, and field
index=prod_payments sourcetype=payment_json level=ERROR

-- Less good: broad search that returns everything
*
```

---

## Core SPL Commands

### `search`

**Purpose:** Filters events passing through the pipeline. Equivalent to adding conditions to your initial search, but used mid-pipeline after other transformations.

**Syntax:** `| search field=value` or `| search keyword`

```spl
index=prod_api
| search level=ERROR
```

This is equivalent to:
```spl
index=prod_api level=ERROR
```

The `search` command mid-pipeline is most useful after a `stats` or other transforming command where you want to filter the summary rows:

```spl
index=prod_api level=ERROR
| stats count by endpoint
| search count > 100    -- keep only endpoints with more than 100 errors
```

---

### `table`

**Purpose:** Selects which fields to display, and in what order, as a structured table. Drops all other fields from the output.

**Syntax:** `| table field1, field2, field3, ...`

```spl
-- Show only the time, host, and error message from ERROR events
index=prod_api level=ERROR
| table _time, host, endpoint, message, durationMs
```

**Why use `table`?**
- Raw events have many fields — `table` focuses the view on what matters
- Makes output readable and shareable (especially for reports)
- After a `stats` command, `table` controls column order

```spl
-- Show error summary with clean column order
index=prod_api level=ERROR earliest=-24h
| stats count as error_count, avg(durationMs) as avg_duration by endpoint
| table endpoint, error_count, avg_duration
| sort -error_count
```

---

### `stats`

**Purpose:** The most important transforming command. Calculates aggregate statistics — counts, averages, sums, min, max — optionally grouped by one or more fields.

**Syntax:** `| stats function(field) [as alias] [by grouping_field1, grouping_field2]`

**Core stats functions:**

| Function | Description | Example |
|---|---|---|
| `count` | Number of events | `stats count` |
| `count(field)` | Number of events where field exists | `stats count(errorCode)` |
| `distinct_count(field)` or `dc(field)` | Number of unique values | `stats dc(userId)` |
| `avg(field)` | Arithmetic mean | `stats avg(durationMs)` |
| `max(field)` | Maximum value | `stats max(durationMs)` |
| `min(field)` | Minimum value | `stats min(durationMs)` |
| `sum(field)` | Sum of values | `stats sum(amount)` |
| `stdev(field)` | Standard deviation | `stats stdev(durationMs)` |
| `perc95(field)` | 95th percentile | `stats perc95(durationMs)` |
| `values(field)` | All unique values as a list | `stats values(errorCode)` |
| `list(field)` | All values (including duplicates) | `stats list(message)` |
| `first(field)` | First value seen | `stats first(status)` |
| `last(field)` | Last value seen | `stats last(status)` |

**Examples:**

```spl
-- Count total events in the index
index=prod_api
| stats count

-- Count events by log level
index=prod_api
| stats count by level

-- Multiple stats in one command
index=prod_api sourcetype=payment_json
| stats count as total_requests,
        avg(durationMs) as avg_ms,
        max(durationMs) as max_ms,
        perc95(durationMs) as p95_ms
  by endpoint

-- Count distinct users who hit errors
index=prod_api level=ERROR
| stats dc(userId) as affected_users by endpoint

-- Count errors per host and endpoint
index=prod_api level=ERROR
| stats count by host, endpoint
| sort -count
```

---

### `eval`

**Purpose:** Creates new fields or modifies existing fields by evaluating expressions. Unlike `stats`, `eval` is a streaming command — it adds a field to each individual event.

**Syntax:** `| eval new_field = expression`

**Arithmetic:**
```spl
-- Convert milliseconds to seconds
index=prod_api
| eval duration_seconds = durationMs / 1000
```

**String operations:**
```spl
-- Concatenate fields
index=prod_api
| eval host_endpoint = host + " | " + endpoint
```

**Conditional logic with `if()`:**
```spl
-- Classify response times
index=prod_api
| eval performance = if(durationMs < 200, "fast",
                     if(durationMs < 1000, "acceptable", "slow"))
```

**`case()` for multiple conditions (like a switch/case):**
```spl
-- Map HTTP status codes to categories
index=prod_api sourcetype=access_combined
| eval status_category = case(
    status >= 500, "server_error",
    status >= 400, "client_error",
    status >= 300, "redirect",
    status >= 200, "success",
    true(), "unknown"
  )
```

**Math functions:**
```spl
-- Round to 2 decimal places
| eval avg_rounded = round(avg(durationMs), 2)

-- Absolute value
| eval abs_diff = abs(expected - actual)
```

**Type conversion:**
```spl
-- Convert string to number
| eval amount_num = tonumber(amount_string)

-- Convert to string
| eval status_str = tostring(status)
```

**Chaining multiple `eval` commands:**
```spl
index=prod_api
| eval duration_seconds = durationMs / 1000
| eval is_slow = if(duration_seconds > 2, "YES", "NO")
| eval display_label = endpoint + " (" + is_slow + ")"
```

---

### `where`

**Purpose:** Filters events using an expression. Similar to `search` but uses eval-style expressions, making it more powerful for numeric comparisons, functions, and complex conditions.

**Syntax:** `| where expression`

```spl
-- Filter events slower than 2 seconds
index=prod_api
| where durationMs > 2000

-- Filter using string comparison (case-sensitive)
index=prod_api
| where level = "ERROR"

-- Multiple conditions
index=prod_api
| where durationMs > 1000 AND status >= 400

-- Using functions
index=prod_api
| where isnotnull(userId) AND len(userId) > 0

-- After stats -- filter summary rows
index=prod_api
| stats avg(durationMs) as avg_ms by endpoint
| where avg_ms > 500
```

**`where` vs `search`:**
- `| search level=ERROR` — simple field-value match, faster for basic string equality
- `| where level = "ERROR"` — eval-style expression, same result but slightly different syntax
- `| where durationMs > 2000` — numeric comparison, MUST use `where` (not `search`)
- `| where match(message, "(?i)timeout")` — regex matching, requires `where`

---

### `sort`

**Purpose:** Sorts results by one or more fields. Ascending by default; prefix with `-` for descending.

**Syntax:** `| sort [limit] [-]field1, [-]field2, ...`

```spl
-- Sort by count descending (most errors first)
index=prod_api level=ERROR
| stats count by endpoint
| sort -count

-- Sort by time ascending (oldest first)
index=prod_api
| sort _time

-- Sort by multiple fields
index=prod_api
| stats count, avg(durationMs) as avg_ms by host, endpoint
| sort host, -avg_ms    -- sort by host ascending, then avg_ms descending within each host

-- Limit results during sort (equivalent to | head 10 after sort)
index=prod_api level=ERROR
| stats count by endpoint
| sort 10 -count        -- return only top 10
```

---

### `head` and `tail`

**Purpose:** Return the first N or last N results from the pipeline.

**Syntax:** `| head [N]` or `| tail [N]` (default N = 10)

```spl
-- First 20 events
index=prod_api level=ERROR
| head 20

-- Last 5 results after sorting
index=prod_api level=ERROR
| stats count by endpoint
| sort -count
| head 5       -- top 5 endpoints by error count

-- Most recent 10 events
index=prod_api
| sort -_time
| head 10
```

`head` is extremely common after `sort` to implement "top N" patterns. `tail` is useful for finding the lowest N values.

---

### `dedup`

**Purpose:** Removes duplicate events based on specified fields. Keeps only the first occurrence of each unique combination of field values.

**Syntax:** `| dedup [N] field1, field2, ...`

```spl
-- Keep only one event per unique userId
index=prod_auth level=ERROR
| dedup userId

-- Keep only one event per unique orderId-errorCode combination
index=prod_payments
| dedup orderId, errorCode

-- Keep the most recent 3 events per userId (dedup with count)
index=prod_auth
| sort -_time
| dedup 3 userId        -- keeps up to 3 events per userId
```

**Common use case:** Finding which users experienced an error, without counting how many times each experienced it:
```spl
-- Without dedup: "usr_4821 had 47 errors" (counted multiple times)
-- With dedup: "usr_4821 experienced at least one error" (listed once)
index=prod_api level=ERROR
| dedup userId
| table _time, userId, endpoint, message
```

---

### Field Renaming with `as`

The `as` keyword is used within `stats` and `eval` (and some other commands) to give a field a new name in the output.

```spl
-- Rename count to "error_count" for clarity
index=prod_api level=ERROR
| stats count as error_count by endpoint

-- Rename multiple stats results
index=prod_api
| stats count as total_requests,
        avg(durationMs) as avg_response_ms,
        max(durationMs) as max_response_ms,
        dc(userId) as unique_users
  by endpoint
```

You can also use the standalone `rename` command:
```spl
| rename durationMs as "Duration (ms)", endpoint as "API Endpoint"
```

Renaming with spaces requires quotes around the new name. This is useful for clean report output.

---

## Common SPL Patterns for Application Log Analysis

These complete, annotated examples cover scenarios every developer encounters in production monitoring.

### Pattern 1: Error Rate Analysis

**Goal:** Find the error rate (percentage of requests that are errors) per API endpoint over the last 24 hours.

```spl
index=prod_api sourcetype=myapp_json earliest=-24h
| stats count as total,                           -- count all events
        sum(eval(if(level="ERROR", 1, 0))) as errors   -- count only ERRORs
  by endpoint
| eval error_rate_pct = round((errors / total) * 100, 2)   -- calculate percentage
| sort -error_rate_pct                            -- worst endpoints first
| table endpoint, total, errors, error_rate_pct  -- clean output columns
```

### Pattern 2: Top N Slowest Requests

**Goal:** Find the 10 slowest individual requests in the last hour.

```spl
index=prod_api sourcetype=myapp_json earliest=-1h
| sort -durationMs                  -- sort by duration, slowest first
| head 10                           -- keep only 10
| table _time, host, endpoint, userId, durationMs, status, message
```

### Pattern 3: Exception Frequency Analysis

**Goal:** Count how many times each unique exception type appeared in the last 7 days.

```spl
index=prod_api sourcetype=myapp_json level=ERROR earliest=-7d
| stats count as occurrences,
        dc(host) as affected_hosts,
        last(_time) as most_recent
  by exceptionType
| sort -occurrences
| table exceptionType, occurrences, affected_hosts, most_recent
```

### Pattern 4: User Impact Analysis

**Goal:** For a specific error code, find how many unique users were affected in the last 24 hours.

```spl
index=prod_api sourcetype=myapp_json errorCode=DB_TIMEOUT earliest=-24h
| dedup userId                       -- one entry per user
| stats count as affected_users      -- count unique users
| appendcols [                       -- add total user count for context
    search index=prod_api sourcetype=myapp_json earliest=-24h
    | dedup userId
    | stats count as total_active_users
  ]
| eval impact_pct = round((affected_users / total_active_users) * 100, 2)
```

### Pattern 5: Host Comparison

**Goal:** Compare error rates across all production hosts.

```spl
index=prod_api sourcetype=myapp_json earliest=-1h
| stats count as total_requests,
        sum(eval(if(level="ERROR", 1, 0))) as error_count
  by host
| eval error_rate_pct = round((error_count / total_requests) * 100, 2)
| sort -error_rate_pct
```

### Pattern 6: Response Time Trend

**Goal:** Show average response time over the last 6 hours, bucketed by 15-minute intervals.

```spl
index=prod_api sourcetype=myapp_json earliest=-6h
| bin _time span=15m                 -- group events into 15-minute buckets
| stats avg(durationMs) as avg_ms,
        perc95(durationMs) as p95_ms,
        count as request_count
  by _time
| sort _time
```

### Pattern 7: Error Pattern After Deployment

**Goal:** Compare error counts before and after a deployment that happened at 2:00 PM today.

```spl
index=prod_api sourcetype=myapp_json level=ERROR
  earliest="01/15/2024:12:00:00" latest="01/15/2024:16:00:00"
| eval period = if(_time < strptime("01/15/2024 14:00:00", "%m/%d/%Y %H:%M:%S"),
                   "pre-deploy", "post-deploy")
| stats count as error_count by period
```

### Pattern 8: Find Users Who Had Specific Sequence of Events

**Goal:** Find all users who logged in successfully but then got an authorization error (potential privilege escalation or misconfiguration issue).

```spl
index=prod_auth sourcetype=auth_json earliest=-1h
  (event_type=login_success OR event_type=authorization_failed)
| stats values(event_type) as event_types by userId, sessionId
| where mvfind(event_types, "login_success") >= 0
    AND mvfind(event_types, "authorization_failed") >= 0
| table userId, sessionId, event_types
```

---

## SPL Query Structure Summary

A complete SPL query follows this general structure:

```spl
-- 1. Initial Search: fetch events from indexes
index=<name> [sourcetype=<type>] [field=value ...] [keywords] [time range]

-- 2. Optional filtering: narrow down events
| where <expression>
| search <field>=<value>

-- 3. Optional transformation: create new fields
| eval <new_field> = <expression>

-- 4. Optional deduplication
| dedup <field>

-- 5. Aggregation: summarize the data
| stats <function>(<field>) as <alias> [by <field>]

-- 6. Post-aggregation filtering
| where <expression on aggregated fields>

-- 7. Presentation
| sort [-]<field>
| head <N>
| table <field1>, <field2>, ...
| rename <field> as "<display name>"
```

Not every query needs every step. Start with what you need and add complexity only when the simpler approach does not answer your question.

---

## Summary

SPL's pipe-based architecture mirrors Unix command chaining — each command receives the output of the previous command and passes its own output downstream. The initial search fetches events; subsequent commands filter, transform, aggregate, and present them.

The core commands — `search`, `table`, `stats`, `eval`, `where`, `sort`, `head`, `tail`, `dedup` — cover the vast majority of operational analysis tasks. The `as` keyword for renaming keeps output readable. The common patterns in this lesson are templates you can adapt immediately for your own applications.

SPL mastery comes from practice — the next lesson dives deeper into specific commands and functions to extend your vocabulary further.

---

## External Resources

- [Splunk SPL Quick Reference Guide](https://www.splunk.com/pdfs/solution-guides/splunk-quick-reference-guide.pdf)
- [Splunk Docs: Search Command Reference](https://docs.splunk.com/Documentation/Splunk/latest/SearchReference/WhatsInThisManual)
- [Splunk Fundamentals 1 Free Training (eLearning)](https://www.splunk.com/en_us/training/free-courses/splunk-fundamentals-1.html)
