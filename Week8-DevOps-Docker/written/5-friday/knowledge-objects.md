# Splunk Knowledge Objects

## Learning Objectives

By the end of this lesson, you will be able to:

- Define what a Splunk knowledge object is and why it matters
- Describe the six main types of knowledge objects: saved searches, field extractions, lookups, event types, tags, and field aliases
- Explain how field aliases normalize data across multiple sourcetypes
- Describe the Common Information Model (CIM) and its benefits for cross-data correlation
- Apply knowledge objects to make searches simpler, more consistent, and reusable

---

## Why This Matters

As you write more SPL queries, you will notice repetition. The same `rex` command to extract an exception class. The same `lookup` to enrich with region data. The same filter for error events. Copying and pasting these fragments into every new search is error-prone and hard to maintain — when the field name changes, you have to update every search.

Splunk Knowledge Objects solve this problem. They are reusable, saved configurations that apply automatically to your searches. Instead of writing the same regex in every query, you define it once as a field extraction, and Splunk applies it automatically whenever you search that sourcetype. This lesson teaches you how to build and use these building blocks — the foundation of production-grade Splunk deployments.

---

## What Are Knowledge Objects?

A knowledge object is any saved configuration in Splunk that adds structure, context, or meaning to your data. Knowledge objects are:

- **Reusable:** Defined once, available in every search, dashboard, and alert
- **Shared:** Can be shared with specific users, roles, or all users of an app
- **Persistent:** Stored in Splunk's configuration system, not inside individual search queries
- **Cumulative:** Multiple knowledge objects layer on top of each other to progressively enrich your data

Think of knowledge objects as the difference between a SQL database with no schema (raw text files) and one with tables, views, stored procedures, and foreign keys. Knowledge objects are Splunk's schema layer.

The six primary types of knowledge objects are:

1. Saved Searches (and Reports)
2. Field Extractions
3. Lookups (Lookup Table Files + Lookup Definitions + Automatic Lookups)
4. Event Types
5. Tags
6. Field Aliases

---

## 1. Saved Searches and Reports

A **saved search** is a stored SPL query, complete with a time range, that you can re-run on demand or on a schedule.

**When to save a search:**
- Any query you run more than twice
- Queries that serve as the basis for dashboard panels
- Queries used by alerts
- Queries you want to share with teammates

**Creating a saved search:**
1. Write and run your search in Search & Reporting
2. Click **Save As → Report**
3. Give it a meaningful name (e.g., `Payment Errors - Last 24h by Endpoint`)
4. Set the default time range
5. Optionally set permissions (private, specific roles, or all users in the app)

**Naming conventions matter.** In large organizations, a Splunk instance can have hundreds of saved searches. A consistent naming convention like `<team>_<domain>_<description>` makes them findable:
```
payments_errors_daily_summary
auth_failed_logins_per_hour
api_slow_requests_p95
```

**Saved searches as scheduled reports:**
In the next lesson you will configure scheduling and email delivery for saved searches. For now, understand that any saved search can be scheduled to run automatically.

---

## 2. Field Extractions

Field extractions define how Splunk pulls structured fields out of raw event text. They are applied automatically at search time whenever the matching sourcetype is queried.

### Why They Matter

Without field extractions, your events are just raw text — searchable by keyword but not by field. With field extractions, `durationMs=5021` in your logs becomes a numeric field you can compare (`durationMs > 2000`), aggregate (`avg(durationMs)`), and sort on.

### Types of Field Extractions

**Delimiter-based (for structured formats):**
Data delimited by a consistent character (CSV, pipe-separated, tab-separated). You specify the delimiter and field names.

Example — pipe-delimited log:
```
2024-01-15 14:23:01 | ERROR | payment-service | ORD-84729 | DB_TIMEOUT | 5021
```

Field extraction configuration:
```ini
[myapp_pipe_logs]
DELIMS = "|"
FIELDS = timestamp, level, service, orderId, errorCode, durationMs
```

**Regex-based (for unstructured or semi-structured formats):**
A regular expression with named capture groups extracts fields from events that do not follow a simple delimiter pattern.

Example — mixed-format application log:
```
2024-01-15 14:23:01 ERROR PaymentService - Processing failed for order=ORD-84729 after 5021ms
```

Regex extraction:
```ini
[myapp_text_logs]
EXTRACT-payment_fields = Processing failed for order=(?P<orderId>[A-Z0-9-]+) after (?P<durationMs>\d+)ms
```

### Creating Field Extractions in the UI

Splunk's **Field Extractor** tool makes this visual:

1. Find an example event in your search results
2. Click **Event Actions → Extract Fields**
3. In the IFX (Interactive Field Extractor), choose **Regular Expression** or **Delimiter** mode
4. For regex mode: click on a value in the raw event text to highlight it
5. Name the field
6. The tool generates a regex and shows validation across all result events
7. Save — the extraction is stored as a knowledge object

### Scope of Field Extractions

Field extractions are scoped to a **sourcetype** (most common), a **source**, or a **host**. The scope determines when the extraction applies. Sourcetype-scoped extractions are the standard choice because sourcetype represents the data format.

---

## 3. Lookups

Lookups were introduced in the previous lesson as a command (`| lookup`). As a knowledge object, a lookup is a stored reference table that can be applied automatically without being typed in every search.

### Three-Part Lookup System

**Lookup Table File:** The actual data file — typically a CSV, though Splunk also supports database-backed lookups (KV Store lookups and external lookups via Python scripts).

**Lookup Definition:** A named configuration that maps the lookup table file to input and output fields. It tells Splunk: "When you see this lookup name in a query, use this file, match on this field, and return these fields."

**Automatic Lookup:** Configures Splunk to apply a lookup automatically whenever a specific sourcetype, source, or host is searched — no `| lookup` command needed in the search.

### Creating an Automatic Lookup

After creating the lookup table file and definition:

1. Go to **Settings → Lookups → Automatic Lookups → Add New**
2. Name the automatic lookup
3. Select the lookup definition
4. Set the scope: sourcetype, source, or host
5. Map the input field (the key in your events) to the lookup key
6. Set output fields

After saving, every search against that sourcetype automatically has the lookup's fields available:

```spl
-- Without automatic lookup, you would write:
index=prod_payments sourcetype=payment_json level=ERROR
| lookup customer_data customerId OUTPUT customer_name, region, tier

-- With automatic lookup configured, this is equivalent:
index=prod_payments sourcetype=payment_json level=ERROR
-- customer_name, region, and tier are now available automatically
| table _time, customerId, customer_name, region, tier, errorCode
```

### KV Store Lookups

The KV Store (Key-Value Store) is a built-in NoSQL database in Splunk for storing lookup data that needs to be updated frequently by Splunk itself (dashboards, apps, or searches). Unlike CSV lookups (which require file replacement to update), KV Store lookups can be updated via REST API or Splunk's `outputlookup` command.

```spl
-- Write current error counts to a KV Store lookup for use in a dashboard
index=prod_api level=ERROR earliest=-1h
| stats count as error_count by endpoint
| outputlookup endpoint_error_counts     -- writes to a KV Store or CSV lookup
```

---

## 4. Event Types

An **event type** is a saved search filter that categorizes events. It gives a meaningful label to events matching specific criteria, letting you refer to a set of events by name rather than by the search expression that finds them.

### Why Event Types Exist

Imagine you define "a critical payment error" as:
```spl
index=prod_payments level=ERROR errorCode IN ("DB_TIMEOUT", "PAYMENT_GATEWAY_DOWN", "FRAUD_BLOCK")
```

Without an event type, you paste this everywhere. With an event type named `critical_payment_error`, you search:
```spl
eventtype=critical_payment_error
```

### Creating an Event Type

1. Run a search that finds the events you want to classify
2. Click **Save As → Event Type**
3. Give it a name and optionally a priority (1-10, lower = higher priority) and color for the Splunk UI
4. Save

**Example event types for a payments team:**
- `payment_success` — `index=prod_payments result=SUCCESS`
- `payment_failure` — `index=prod_payments result=FAILURE`
- `payment_fraud_flag` — `index=prod_payments errorCode=FRAUD_BLOCK`
- `payment_slow` — `index=prod_payments durationMs>5000`

**Using event types in searches:**
```spl
-- Find slow payments that also failed
eventtype=payment_slow eventtype=payment_failure

-- Count events by event type
index=prod_payments
| stats count by eventtype
```

**Event types vs. saved searches:** A saved search is executed and returns results. An event type is a filter applied to the current search — it adds a label to matching events. Event types compose (an event can match multiple event types simultaneously).

---

## 5. Tags

**Tags** are labels you assign to specific field-value pairs. Once tagged, you can search for events using the tag rather than the original field-value expression.

### The Tag Concept

Suppose your infrastructure spans three environments:
- Production servers are tagged with: `host=prod-api-01`, `host=prod-api-02`, `host=prod-api-03`
- Staging servers: `host=staging-api-01`, `host=staging-api-02`

Without tags, to search production servers you write:
```spl
host=prod-api-01 OR host=prod-api-02 OR host=prod-api-03
```

With tags, you tag all three host values with `tag::host=production`. Then:
```spl
tag::host=production
-- or more commonly:
tag=production
```

### Creating Tags

1. Run a search and find an event with the field-value pair you want to tag
2. In the event detail panel, find the field
3. Click the field value's **Actions** dropdown → **Edit Tags**
4. Type a tag name and save

Or via Settings → Tags → All tag definitions.

### Tagging Multiple Values

```spl
-- You can tag field-value pairs in bulk via the configuration UI
-- For example, tagging all error-level events:
-- Tag: level=ERROR → "error", "needs_attention"
-- Tag: level=WARN → "warning"
-- Tag: level=INFO → "informational"
```

Then:
```spl
-- Find everything tagged as needing attention
tag=needs_attention

-- This matches all events where level=ERROR (since you tagged that field-value pair)
```

### Tags for Normalization Across Sourcetypes

Tags shine when different sourcetypes use different field values for the same concept. Firewall logs might use `action=DROP`, while proxy logs use `action=BLOCK`, while IDS logs use `action=DENY`. Tag all three with `tag::action=blocked`:

```spl
-- Without tags: verbose and brittle
(sourcetype=firewall action=DROP) OR (sourcetype=proxy action=BLOCK) OR (sourcetype=ids action=DENY)

-- With tags: clean and maintainable
tag::action=blocked
```

---

## 6. Field Aliases

A **field alias** creates an alternate name for an existing field. When you alias field `A` to `B`, searching for `B=value` returns events where `A=value`.

### The Problem Field Aliases Solve

Different data sources often use different field names for the same concept:
- Your Java app logs use `userId`
- Your Nginx access logs use `user_id`
- Your authentication service uses `uid`
- Your CRM lookup uses `customer_id`

Without aliases, writing a search across all these sources requires:
```spl
(userId=usr_4821 OR user_id=usr_4821 OR uid=usr_4821)
```

With field aliases, you alias `user_id`, `uid`, and `customer_id` all to `userId` on their respective sourcetypes. Now every search uses `userId=usr_4821` regardless of which sourcetype the data came from.

### Creating Field Aliases

1. Go to **Settings → Fields → Field Aliases → Add New**
2. Select the app scope
3. Set the scope type and value (typically sourcetype = `nginx_access_combined`)
4. Specify existing field name and alias (new) field name:
   - Existing field name: `user_id`
   - Field aliases (new name): `userId`
5. Save

### Field Aliases Do Not Rename Fields

An important nuance: field aliases create an **additional** name for a field, not a replacement. The original field name still works. Both `user_id=usr_4821` and `userId=usr_4821` return the same events after an alias is configured.

---

## The Common Information Model (CIM)

The Common Information Model is Splunk's standard data schema — a set of field names and event type definitions that normalize data from different sources into a consistent structure.

### Why CIM Exists

Splunk can receive data from thousands of different sources: AWS CloudTrail, Okta, Nginx, PostgreSQL, Windows Event Logs, Palo Alto firewalls, and your custom Java application. Each source uses its own field names and values. Without normalization:

- AWS CloudTrail calls the user `userIdentity.arn`
- Okta calls the user `actor.alternateId`
- Windows calls the user `SubjectUserName`
- Nginx calls the user `remote_user`

To correlate "which user's activities span all these systems," you need all four to share a common field name. CIM defines that common field name as `user`.

### CIM Data Models

CIM is organized into **data models** — groups of normalized fields and event types for specific data domains:

| Data Model | Covers | Key Normalized Fields |
|---|---|---|
| **Authentication** | Login/logout events, access control | `user`, `app`, `action` (`success`/`failure`), `src` (source IP) |
| **Network Traffic** | Firewall logs, network flows | `src_ip`, `dest_ip`, `src_port`, `dest_port`, `bytes_in`, `bytes_out`, `action` |
| **Web** | HTTP access logs | `url`, `http_method`, `status`, `bytes`, `response_time` |
| **Endpoint** | Host-level events, process activity | `user`, `process`, `process_name`, `dest`, `file_name` |
| **Alerts** | Security alerts from any source | `severity`, `signature`, `category`, `dest` |
| **Databases** | Database activity | `app`, `user`, `query`, `response_time`, `object` |
| **Application State** | Application status, errors | `app`, `status`, `message`, `severity` |

### CIM in Practice — The Splunk Add-on Model

Splunk partners and the community have built **Splunk Add-ons** (also called Technology Add-ons or TAs) for virtually every common data source. Each add-on:

1. Provides pre-built field extractions for that source
2. Creates field aliases mapping source-specific field names to CIM field names
3. Defines event types and tags that categorize events per CIM standards

**Example:** The **Splunk Add-on for AWS** normalizes CloudTrail logs:
- Maps `userIdentity.arn` → alias to `user` (CIM field)
- Maps `eventTime` → properly formatted `_time`
- Creates event type `aws_cloudtrail_authentication` tagged as `authentication`

After installing the add-on, CloudTrail events are queryable using CIM field names alongside any other authentication data source.

### Searching CIM-Normalized Data

The real power: once all your data sources are CIM-normalized, you can write searches that span all of them using consistent field names:

```spl
-- Authentication failures across ALL authentication sources
-- (Windows AD, AWS IAM, Okta, SSH, your app -- all in one query)
tag=authentication action=failure earliest=-24h
| stats count as failed_attempts by user, src
| where failed_attempts > 10
| sort -failed_attempts
```

Without CIM, this query would be impossible without explicitly naming every sourcetype and field variant. With CIM, it just works.

### The `tstats` Command and CIM Data Models

When you use CIM data models and accelerate them, you can use `tstats` — a dramatically faster version of `stats` that operates on pre-built data model summaries rather than raw events:

```spl
-- Using tstats on an accelerated CIM data model -- extremely fast
| tstats count as failed_auth
    FROM datamodel=Authentication.Authentication
    WHERE Authentication.action=failure earliest=-24h
    BY Authentication.user, Authentication.src
| rename Authentication.user as user, Authentication.src as source_ip
| sort -failed_auth
```

This can return results in seconds on billions of events that would take minutes with a standard `stats` search.

---

## Knowledge Object Permissions and Sharing

Knowledge objects have a three-tier permission model:

| Permission Level | Who Can Use It |
|---|---|
| **Private** | Only the user who created it |
| **App** | All users of the specific Splunk app (e.g., Search & Reporting) |
| **Global** | All users across all apps in the Splunk instance |

**Best practice:** Create knowledge objects privately during development/testing. Share at the App level for team use. Promote to Global only when universally applicable.

---

## Summary

Knowledge objects are Splunk's schema and reusability layer. They transform a collection of ad-hoc searches into a governed, maintainable observability platform.

- **Saved searches** store reusable queries
- **Field extractions** automatically parse structure from raw event text
- **Lookups** enrich events with reference data from external tables
- **Event types** categorize events with meaningful labels
- **Tags** abstract field-value pairs behind semantic labels, enabling cross-sourcetype searches
- **Field aliases** normalize inconsistent field names across data sources

The Common Information Model extends these concepts to the enterprise scale, providing a standard vocabulary that makes data from thousands of different systems comparable and correlatable. CIM is the foundation of Splunk's security and IT analytics capabilities, and it demonstrates how knowledge objects compose into something much greater than the sum of their parts.

---

## External Resources

- [Splunk Docs: Knowledge Objects Overview](https://docs.splunk.com/Documentation/Splunk/latest/Knowledge/Whataresplunkknowledgeobjects)
- [Splunk Common Information Model (CIM) Documentation](https://docs.splunk.com/Documentation/CIM/latest/User/Overview)
- [Splunk Add-ons on Splunkbase](https://splunkbase.splunk.com/apps/#/product/splunk/type/addon)
