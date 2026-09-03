# Using Search in Splunk

## Learning Objectives

By the end of this lesson, you will be able to:

- Navigate the Search & Reporting interface with confidence
- Use the time range picker effectively to scope searches
- Distinguish between keyword search and field-value search syntax
- Choose the appropriate search mode (fast, smart, verbose) for different scenarios
- Read and interpret the event timeline and fields sidebar
- Write basic SPL searches to find relevant events

---

## Why This Matters

Search is the primary interface between you and your data in Splunk. Before you can build dashboards, configure alerts, or perform sophisticated analysis, you need to be able to find the events you care about. This lesson is your foundation — the skill of writing effective searches is what separates a Splunk user who gets answers from one who gets frustrated.

---

## The Search & Reporting App Interface

When you open the Search & Reporting app, you see several distinct components. Understanding each one is the first step to working efficiently.

### The Search Bar

The search bar is the most prominent element — a wide text field at the top of the page. This is where you type SPL (Search Processing Language) queries.

Key behaviors:
- **Auto-complete:** As you type, Splunk suggests field names, values, and SPL commands based on your indexed data
- **Search history:** Use the up/down arrow keys to cycle through your recent searches
- **Search on Enter or click the magnifying glass:** Both submit the search
- **Shift+Enter:** Opens the search in a new window

The search bar is not case-sensitive for most operations (field names, commands, keywords), but field values are case-sensitive by default.

### The Time Range Picker

Located immediately to the right of the search bar, the time range picker controls which time window your search covers. This is one of the most important controls in Splunk because:

1. **All Splunk data is timestamped** — every event has a time associated with it
2. **Time range directly affects search speed** — searching the last 15 minutes is orders of magnitude faster than searching the last 30 days
3. **Choosing the wrong time range is a common reason searches return no results**

#### Time Range Options

**Preset Ranges:**
- Last 15 minutes, Last 60 minutes, Last 4 hours, Last 24 hours
- Today, This week, This month
- Last 7 days, Last 30 days

**Relative ranges:**
- "Earliest -2h" means "from 2 hours ago to now"
- "Earliest -1d@d" means "from the start of yesterday to now"

**Real-time ranges:**
- 30-second window, 1-minute window — searches continuously update as new data arrives
- Useful for live monitoring dashboards
- Expensive — use sparingly and only when truly needed

**Custom (Date Range):**
- Specify exact start and end timestamps
- Useful for investigating a specific incident: "I know the outage was between 2:15 PM and 3:47 PM on January 15th"

#### Best Practices for Time Range

- **Start narrow, expand if needed.** Begin with "Last 15 minutes" or "Last hour" rather than "Last 7 days." If you find events, you can adjust. Starting broad wastes time and resources.
- **When investigating an incident, use a custom range** that brackets the incident window plus a buffer before and after to capture context.
- **Never use "All time" unless absolutely necessary** — it searches every event ever indexed, which can be millions or billions of records.

### The Event Timeline

Below the search bar and time picker, after a search runs, you see a bar chart called the **event timeline** (also called the timeline histogram).

What it shows:
- **X-axis:** Time, divided into buckets based on your search window (if you searched 1 hour, each bar might represent 5 minutes; if you searched 7 days, each bar represents a few hours)
- **Y-axis:** Number of events in that time bucket
- **Color:** By default a single color; can be split by field for comparison

**How to use the timeline:**

- **Identify spikes:** A sudden spike in events often indicates an incident — a spike in error events, for example
- **Click and drag** on the timeline to zoom into a specific time window without re-running the search
- **Click a bar** to filter to just that time bucket
- **The timeline updates in real time** as you modify your search filters

This is one of Splunk's most powerful quick-triage tools. You can instantly see: "The error rate doubled at 2:30 PM" just by looking at the timeline shape.

### The Fields Sidebar

To the left of the event results (or accessible via a sidebar toggle), the **fields sidebar** shows every field Splunk has extracted from your search results.

Fields are divided into two sections:

**Selected Fields (always shown):**
- `host` — the server or container that generated the event
- `source` — the file or input the event came from
- `sourcetype` — the classification of the data format

**Interesting Fields:**
Fields that appear in at least 20% of your result events and have fewer than 100 unique values. Splunk considers these "interesting" because they are likely meaningful for analysis.

**Clicking a field** in the sidebar opens a panel showing:
- Top values for that field and their counts/percentages
- A "Select fields" option to add it to your Selected Fields list (pins it to every event display)
- Quick links to search for specific values

This is extremely useful for exploration: if you do not know what field contains the HTTP status code, you can browse the interesting fields sidebar to discover it.

### Raw vs Table View

Search results can be displayed in two main views:

**Raw Events View:**
Shows each event as it was ingested — the raw log line — with extracted fields expandable below it. This is the default view and is best for:
- Reading the actual content of log messages
- Understanding the full context of an event
- Debugging field extraction issues

**Table/Statistics View:**
Shows structured tabular output, populated by SPL commands that produce aggregated results (like `stats` or `table`). Best for:
- Viewing aggregated counts, averages, or sums
- Comparing values across groups
- Building the data for charts

You switch between these views using the tabs at the top of the results panel: **Events**, **Patterns**, **Statistics**, **Visualization**.

---

## Search Types: Keyword vs Field-Value

### Keyword Search

The simplest form of Splunk search is a free-text keyword search — you type a word or phrase and Splunk returns all events containing that text anywhere in the raw event.

```spl
error
```
Returns all events in the selected time range that contain the word "error" anywhere in the raw text.

```spl
"connection refused"
```
Returns events containing the exact phrase "connection refused" (use quotes for multi-word phrases).

```spl
NullPointerException
```
Returns events containing "NullPointerException" — useful for finding Java exceptions.

```spl
payment failed
```
Returns events containing BOTH "payment" AND "failed" anywhere in the event (implicit AND between terms).

```spl
timeout OR refused
```
Returns events containing either "timeout" or "refused."

```spl
error NOT debug
```
Returns events containing "error" but NOT "debug" — useful when your logs are verbose and you want to exclude noise.

**When keyword search is appropriate:**
- Quick exploratory searches when you do not know the field structure
- Searching for specific error messages or stack trace snippets
- Initial triage when an incident is reported

**Limitations of keyword search:**
- Searches ALL text in the event — can return false positives if your search term appears in unexpected places
- Less precise than field-value search
- Cannot express conditions like "response time greater than 500ms"

### Field-Value Search

Field-value searches use the `field=value` syntax to search specific extracted fields. This is more precise and generally faster than keyword search.

```spl
status=500
```
Returns events where the extracted field `status` has the value `500`.

```spl
level=ERROR
```
Returns events where the `level` field equals `ERROR`.

```spl
host=prod-api-01
```
Returns events from the specific host named "prod-api-01".

```spl
index=prod_payments sourcetype=payment_json level=ERROR
```
Searches the `prod_payments` index for events with sourcetype `payment_json` where level is ERROR. **Always specify index and sourcetype early in your search** — it dramatically reduces the data Splunk needs to scan.

```spl
status!=200
```
The `!=` operator means "not equal to" — returns events where status is anything other than 200.

```spl
durationMs>1000
```
Numeric comparison — returns events where `durationMs` is greater than 1000.

```spl
durationMs>=500 durationMs<=2000
```
Range query — events where durationMs is between 500 and 2000 ms.

```spl
message="*payment*"
```
Wildcard search — the `*` matches any sequence of characters. Returns events where the message field contains "payment" anywhere.

```spl
userId=usr_*
```
Wildcard at end — returns events where userId starts with "usr_".

---

## Combining Keyword and Field-Value Search

You can freely mix keyword and field-value search in the same query:

```spl
index=prod_payments level=ERROR "database connection"
```
Search the prod_payments index for ERROR-level events that also contain the phrase "database connection" anywhere in the raw text.

```spl
index=prod_api host=prod-api-* status=500 timeout
```
Search prod_api index for 500 errors from any host matching "prod-api-*" that also mention "timeout."

The general SPL search syntax follows this pattern:
```
index=<index_name> [field=value ...] [keywords] [| commands]
```

---

## Search Modes

Splunk offers three search modes that trade off thoroughness for speed. You select them from the dropdown next to the search button.

### Fast Mode

**What it does:** Splunk returns only the minimum data needed. It focuses on returning events quickly and skips field extraction for fields not explicitly referenced in your search.

**Trade-off:** Faster searches, but the fields sidebar will be empty or incomplete. Event details may not show all extracted fields.

**When to use it:**
- When you only care about the raw event text, not specific fields
- When searching large time windows and performance is critical
- When running searches that do not reference specific fields

**Example scenario:** You want to quickly find whether any event in the last 30 days mentions "OutOfMemoryError." You do not need field data — just existence of the event. Fast mode is appropriate.

### Smart Mode (Default)

**What it does:** Splunk automatically chooses the level of field extraction based on what your search requires. If your search is a simple keyword search, it behaves like Fast mode. If your search uses SPL commands that require field data (like `stats` or `table`), it performs full field extraction.

**Trade-off:** Balances speed and functionality intelligently for most use cases.

**When to use it:** The default for most day-to-day searching. Leave it here unless you have a specific reason to change.

### Verbose Mode

**What it does:** Full field extraction for every event returned. Every extractable field is populated, and the fields sidebar shows the complete picture of all fields present in your data.

**Trade-off:** Slowest mode. Significantly more processing required, especially for large result sets.

**When to use it:**
- When you are exploring a new data source and want to understand all available fields
- When debugging field extraction configurations
- When your search is small (narrow time range, specific host, few results) and you want complete field information

---

## Practical Search Examples

### Example 1: Find all errors in the last hour

```spl
index=prod_api level=ERROR earliest=-1h
```

Read this as: In the prod_api index, find events where level equals ERROR, from the last hour.

### Example 2: Find 500 errors from a specific service

```spl
index=prod_api sourcetype=access_combined status=500 earliest=-24h
```

Apache/Nginx access logs often use the sourcetype `access_combined`. This finds all 500 errors from the last 24 hours.

### Example 3: Find slow API responses

```spl
index=prod_api durationMs>2000 earliest=-1h
```

Find requests that took longer than 2 seconds (2000 milliseconds).

### Example 4: Find login failures for a specific user

```spl
index=prod_auth event_type=login_failed userId=usr_4821
```

If your auth service emits structured JSON logs with these fields, this query finds all login failures for that specific user.

### Example 5: Find container restarts

```spl
index=prod_containers "container exited" OR "OOMKilled"
```

Docker events in your log stream may include these phrases when containers restart unexpectedly.

### Example 6: Combining time, index, and keyword

```spl
index=prod_payments earliest="01/15/2024:14:00:00" latest="01/15/2024:16:00:00" "payment failed" status=ERROR
```

Investigate a specific two-hour incident window. Note the timestamp format: `MM/DD/YYYY:HH:MM:SS`.

---

## The Search History and Saved Searches

### Search History

Every search you run is saved in your personal search history (accessible from the Search app). You can:
- Click any past search to re-run it
- Use the up/down arrows in the search bar to cycle through recent searches
- Filter history by keyword to find a specific past search

### Saving Searches

If you write a search you will use repeatedly, save it:
1. Run the search
2. Click **Save As → Report** or **Save As → Alert**
3. Give it a name and optional description
4. Set the default time range

Saved searches can be shared with teammates, scheduled to run automatically, or used as the basis for dashboard panels.

---

## Summary

Splunk's search interface is built around the search bar, time range picker, event timeline, and fields sidebar working together. The time range picker is critical — always start with a narrow window and expand only as needed.

Keyword search is the fastest way to find events containing specific text. Field-value search (`field=value`) is more precise and performs better at scale. Combine them freely, and always specify `index=` at the start of your searches.

Search modes let you trade thoroughness for speed — Smart mode is the right default, Fast mode helps for large exploratory searches, and Verbose mode is essential when learning a new data source.

The skills from this lesson form the foundation for everything that follows: SPL commands, knowledge objects, dashboards, and alerts all start with a search.

---

## External Resources

- [Splunk Docs: Search Tutorial](https://docs.splunk.com/Documentation/Splunk/latest/SearchTutorial/WelcometotheSearchTutorial)
- [Splunk Search Reference: Search Syntax](https://docs.splunk.com/Documentation/Splunk/latest/Search/Aboutsearchlanguage)
- [Splunk Quick Reference Card (PDF)](https://www.splunk.com/pdfs/solution-guides/splunk-quick-reference-guide.pdf)
