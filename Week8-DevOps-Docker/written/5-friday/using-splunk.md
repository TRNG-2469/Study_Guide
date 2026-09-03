# Using Splunk: Interface Tour and Data Inputs

## Learning Objectives

By the end of this lesson, you will be able to:

- Navigate the core Splunk web interface and its major apps
- Describe the concept of a Splunk index and why data organization matters
- Explain the different methods for getting data into Splunk
- Differentiate between Universal and Heavy Forwarders and know when to use each
- Understand the HTTP Event Collector (HEC) as an application-level integration

---

## Why This Matters

Knowing how to search Splunk is only useful if data is actually flowing into it. Understanding the interface and data input mechanisms is the foundation for everything else — searches, dashboards, and alerts all depend on data arriving correctly and being organized sensibly. This lesson bridges the gap between "Splunk exists" and "Splunk is receiving and organizing my application's logs."

---

## The Splunk Web Interface

Splunk's web interface is organized around the concept of **Apps**. An App is a packaged collection of dashboards, searches, data inputs, and configurations targeted at a specific use case. Think of Apps like browser extensions — they extend Splunk's core functionality without changing the underlying platform.

When you first log in to Splunk, you land on the **Home** screen, which shows your installed Apps and recent activity.

### Core Built-In Apps

#### 1. Search & Reporting App

This is the most important app for developers and analysts. It provides:

- **The Search Bar:** Where you type SPL (Search Processing Language) queries
- **The Time Range Picker:** Controls the time window your search covers
- **The Events Panel:** Displays results of your search
- **The Fields Sidebar:** Shows all fields Splunk has extracted from your results
- **The Visualization Tab:** Turns your results into charts and graphs
- **The Statistics Tab:** Shows aggregated tabular results

You will spend the majority of your time in this app. Every other lesson today uses it.

#### 2. Dashboards App (Dashboard Studio)

The Dashboards app is where you build and view visual dashboards. It has two modes:

- **Classic Dashboards:** XML-based dashboards (Simple XML) — easier to share and version-control, compatible with all Splunk versions
- **Dashboard Studio:** A newer drag-and-drop visual editor with more chart types and layout flexibility

Dashboards aggregate multiple search panels into a single view so stakeholders can monitor system health at a glance.

#### 3. Alerts

Alerts are saved searches that run on a schedule and trigger actions when conditions are met. The Alerts interface lets you:

- View all configured alerts and their status
- See alert trigger history (when each alert fired and what it found)
- Manage alert actions (email, webhook, Slack notification, PagerDuty integration)

#### 4. Reports

Reports are saved searches that run on a schedule and store results or deliver them via email. Unlike alerts (which notify on a condition), reports simply gather and present data on a schedule — e.g., "send the daily error summary to the engineering team every morning at 8 AM."

#### 5. Settings (Admin Area)

The Settings menu is where administrators configure:

- **Data Inputs:** How data enters Splunk (file monitors, forwarders, HEC)
- **Indexes:** Create and manage data repositories
- **Users and Roles:** Access control
- **Lookups:** Reference tables for enriching data
- **Source Types:** Rules for parsing different types of data

---

## The Index: Splunk's Data Organization Unit

Before discussing how data gets into Splunk, it is essential to understand where it goes: the **index**.

### What Is an Index?

An index is Splunk's primary data storage unit. It is analogous to a database in a relational system — a named repository where events are stored. When Splunk receives data, it parses the raw text into discrete events, extracts fields, and stores them in an index.

Key characteristics of an index:

- **Data lives in indexes permanently** (until a retention policy expires it)
- **Indexes are searched by name** — you can restrict a search to one or more indexes
- **Indexes have retention settings** — how many days or gigabytes of data to keep before the oldest data is deleted
- **Indexes have access controls** — certain users or roles may only be able to read specific indexes

### Default Indexes

Splunk ships with several built-in indexes:

| Index Name | Purpose |
|---|---|
| `main` | The default index — data goes here if no index is specified |
| `_internal` | Splunk's own operational logs (search activity, indexer performance, forwarder connections) |
| `_audit` | Audit trail of Splunk user activity |
| `history` | Search history |

### Best Practice: Use Dedicated Indexes Per Application/Team

Rather than dumping all data into `main`, most organizations create dedicated indexes:

```
index=prod_payments       # Payment service logs
index=prod_auth           # Authentication service logs  
index=prod_api_gateway    # API gateway access logs
index=staging_all         # All staging environment logs
index=security_events     # Security-relevant events
```

This organization provides several benefits:

1. **Access control:** The payments team can be restricted to `prod_payments` without seeing other teams' data
2. **Retention flexibility:** Security logs might be retained for 365 days; debug logs for only 30
3. **Search performance:** Restricting searches to a relevant index is dramatically faster than searching `index=*`
4. **Cost management:** In Splunk Cloud, different retention tiers have different costs

### Searching Across Indexes

```spl
-- Search a single index
index=prod_api_gateway

-- Search multiple indexes
index=prod_payments OR index=prod_auth

-- Search all indexes (avoid in production -- very slow and expensive)
index=*
```

---

## Data Inputs: Getting Data Into Splunk

Splunk supports multiple mechanisms for ingesting data. The right choice depends on where your data lives and how it is generated.

### Method 1: File and Directory Monitoring

Splunk can watch a file or directory on the local filesystem and continuously read new content as it is appended.

**How it works:** You configure a monitor input pointing to a file path. Splunk tracks a checkpoint (how far it has read) so that on restart it picks up where it left off, never re-reading the same data.

**Configuration example (in `inputs.conf`):**
```ini
[monitor:///var/log/myapp/application.log]
index = prod_api
sourcetype = myapp_json
```

**Best for:**
- Applications that write to log files on disk
- System logs (`/var/log/syslog`, `/var/log/auth.log`)
- Application server logs (Tomcat, Nginx, Apache)

**Limitations:**
- Only works on the local filesystem — the Splunk instance must be on the same machine as the log file (or a forwarder must be installed)
- Does not work for containerized applications where logs go to stdout/stderr (use HEC or a log driver instead)

### Method 2: HTTP Event Collector (HEC)

HEC allows applications to send events directly to Splunk over HTTP or HTTPS, using a simple REST API with JSON payloads.

**How it works:**

1. An administrator enables HEC in Splunk Settings and creates an HEC token (an API key)
2. Your application sends HTTP POST requests to Splunk's HEC endpoint with the token in the Authorization header
3. Splunk receives the events and indexes them immediately

**HEC Endpoint:**
```
POST https://your-splunk-instance:8088/services/collector/event
Authorization: Splunk <your-hec-token>
Content-Type: application/json
```

**Example payload — sending a single event:**
```json
{
  "time": 1705320000,
  "host": "api-server-prod-01",
  "source": "payment-service",
  "sourcetype": "payment_json",
  "index": "prod_payments",
  "event": {
    "level": "ERROR",
    "message": "Payment processing failed",
    "orderId": "ORD-84729",
    "errorCode": "INSUFFICIENT_FUNDS",
    "userId": "usr_4821",
    "durationMs": 342
  }
}
```

**Example — sending multiple events in one request (batch HEC):**
```json
{"time": 1705320000, "event": {"level": "INFO", "message": "Request received", "requestId": "req-001"}}
{"time": 1705320001, "event": {"level": "INFO", "message": "Processing complete", "requestId": "req-001", "durationMs": 125}}
```

**Sending from a Java application (Spring Boot example):**
```java
@Service
public class SplunkLogger {

    private final RestTemplate restTemplate;
    private final String hecUrl = "https://splunk.company.com:8088/services/collector/event";
    private final String hecToken = "your-hec-token-here";

    public void logEvent(String level, String message, Map<String, Object> fields) {
        // Build the event payload
        Map<String, Object> event = new HashMap<>(fields);
        event.put("level", level);
        event.put("message", message);
        event.put("service", "payment-service");

        Map<String, Object> payload = new HashMap<>();
        payload.put("time", Instant.now().getEpochSecond());
        payload.put("host", System.getenv("HOSTNAME"));
        payload.put("index", "prod_payments");
        payload.put("sourcetype", "payment_json");
        payload.put("event", event);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Splunk " + hecToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Send to Splunk
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        restTemplate.postForObject(hecUrl, request, String.class);
    }
}
```

**Best for:**
- Containerized applications (no filesystem access for forwarders)
- Microservices that want to control their own log format
- Serverless functions (AWS Lambda)
- Applications sending structured JSON events
- Real-time streaming scenarios

**Advantages of HEC:**
- Works from anywhere with network access to Splunk
- Supports structured JSON natively (no log parsing required)
- Low latency — events appear in Splunk within seconds
- Supports batching for efficiency

### Method 3: Splunk Forwarders

A Splunk Forwarder is a lightweight Splunk agent installed on a server, VM, or container. It monitors local files and system metrics, then ships the data to a Splunk indexer over an encrypted TCP connection.

This is the most common data input method for traditional server-based deployments.

---

## Forwarder Architecture: Universal vs Heavy Forwarder

Splunk has two types of forwarders with very different purposes.

### Universal Forwarder (UF)

**What it is:** A minimal, lightweight agent. It has NO search capability, NO parsing capability, and NO user interface. Its only job is to collect data from the local system and forward it to the indexer as efficiently as possible.

**Resource footprint:** Very small — typically less than 1% CPU and 64 MB RAM. Designed to run on every server in your fleet without impacting application performance.

**What it can do:**
- Monitor files and directories
- Tail log files in real time
- Monitor Windows Event Logs (on Windows)
- Monitor network ports (UDP/TCP)
- Collect scripted inputs (run a script and capture its output)

**What it CANNOT do:**
- Parse log events (that happens at the indexer)
- Run SPL searches
- Mask or filter sensitive data before sending
- Transform data (field extractions, regex substitutions)

**Best for:** 99% of deployments. Install a UF on every application server and let the indexer handle parsing.

**Installation size:** ~20-30 MB. Runs as a system service.

### Heavy Forwarder (HF)

**What it is:** A full Splunk instance configured to forward data instead of indexing it locally. Because it is a full instance, it has Splunk's complete parsing and data manipulation capability.

**Resource footprint:** Significantly larger — requires 1+ GB RAM and meaningful CPU. You would NOT install this on every server.

**What it can do (in addition to everything UF does):**
- Parse and transform events before forwarding
- Filter out unwanted events (reducing data volume before it reaches the indexer)
- Mask or anonymize sensitive data (credit card numbers, SSNs) at the point of collection
- Route data to different indexes based on content
- Apply timestamp recognition to ambiguously formatted logs
- Aggregate multiple sources before forwarding

**Best for:**
- Compliance scenarios requiring PII masking before data leaves a network segment
- High-volume deployments where filtering at the forwarder reduces indexer load
- Data routing — sending certain log types to different indexes or even different Splunk deployments
- Collecting data from sources that cannot have a UF installed (network devices using syslog, databases)

### Forwarder Deployment Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    PRODUCTION NETWORK                         │
│                                                              │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐       │
│  │ App Server 1 │   │ App Server 2 │   │ App Server 3 │      │
│  │    UF       │   │    UF       │   │    UF       │       │
│  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘       │
│         │                 │                 │               │
│         └─────────────────┼─────────────────┘               │
│                           │                                  │
│                           ▼                                  │
│                  ┌─────────────────┐                        │
│                  │ Heavy Forwarder  │  ← PII masking,        │
│                  │ (optional tier) │    filtering, routing   │
│                  └────────┬────────┘                        │
└───────────────────────────┼──────────────────────────────────┘
                            │  (encrypted TCP, port 9997)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  SPLUNK INDEXER CLUSTER                       │
│                 (may be Splunk Cloud)                        │
└─────────────────────────────────────────────────────────────┘
```

### Forwarder Data Flow in a Docker/Container Environment

Containers present a challenge for traditional forwarder-based architectures. Containers are ephemeral — they start and stop frequently, and you typically do not install agents inside containers (it violates the single-responsibility principle and increases image size).

Common patterns for containerized log collection:

**Pattern 1: Sidecar Container**
A UF runs as a sidecar container in the same pod (Kubernetes) or task definition (ECS), sharing a volume mount with the application container. The UF monitors the shared log volume.

**Pattern 2: Host-Level Forwarder**
A UF is installed on the EC2 host (not inside containers). Docker configures containers to write logs to the host filesystem (the `json-file` log driver), and the UF monitors those host-level log files.

**Pattern 3: Docker Log Driver**
Splunk provides an official Docker log driver that sends container stdout/stderr directly to HEC. Configure in `docker-compose.yml` or ECS task definition:

```yaml
# docker-compose.yml example
services:
  myapp:
    image: myapp:latest
    logging:
      driver: splunk
      options:
        splunk-token: "your-hec-token"
        splunk-url: "https://splunk.company.com:8088"
        splunk-index: "prod_containers"
        splunk-sourcetype: "docker:myapp"
        splunk-insecureskipverify: "false"
```

**Pattern 4: Log Aggregator (Fluentd/Logstash)**
A separate log aggregation service (Fluentd, Logstash, or AWS Kinesis Data Firehose) collects logs from Docker, enriches them, and forwards them to Splunk via HEC. Common in Kubernetes deployments.

---

## Setting Up a Data Input in Splunk Web (Admin View)

Here is the workflow for adding a new data input through the Splunk UI:

1. Navigate to **Settings → Data Inputs**
2. Select the input type (Files & Directories, HTTP Event Collector, TCP, UDP, etc.)
3. Configure the input source (file path, port number, etc.)
4. Set the **Source Type** — either select an existing one or create a new one
5. Select the **Index** where data should be stored
6. Review and save

For HEC specifically:

1. Navigate to **Settings → Data Inputs → HTTP Event Collector**
2. Click **New Token**
3. Name the token (e.g., "payment-service-prod")
4. Set the allowed indexes (restrict to `prod_payments` for principle of least privilege)
5. Copy the generated token — this is your API key for HEC calls

---

## Summary

Splunk's web interface is organized around Apps, with Search & Reporting being the primary workspace for developers. Data is organized into Indexes, which should be planned around application boundaries, team ownership, and retention requirements.

Data reaches Splunk through three main mechanisms: file/directory monitoring (traditional server logs), HTTP Event Collector (applications, containers, serverless), and Splunk Forwarders (agents on servers). The Universal Forwarder is the right choice for server-based collection; the Heavy Forwarder adds data transformation capability at the cost of significantly higher resource usage.

Container environments require adapted architectures — sidecar forwarders, host-level forwarders, or the Splunk Docker log driver are all valid approaches depending on your infrastructure.

---

## External Resources

- [Splunk Docs: Getting Data In Overview](https://docs.splunk.com/Documentation/Splunk/latest/Data/WhatSplunkcanmonitor)
- [Splunk HEC Documentation](https://docs.splunk.com/Documentation/Splunk/latest/Data/UsetheHTTPEventCollector)
- [Splunk Universal Forwarder Manual](https://docs.splunk.com/Documentation/Forwarder/latest/Forwarder/Abouttheuniversalforwarder)
