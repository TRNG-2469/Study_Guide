# Reports, Alerts, and Dashboards in Splunk

## Learning Objectives

By the end of this lesson, you will be able to:

- Save a Splunk search as a scheduled report and configure email delivery
- Configure an alert with threshold conditions and notification actions
- Build a multi-panel dashboard using saved searches as panel data sources
- Choose the correct visualization type for different kinds of data
- Read and understand Simple XML dashboard structure
- Describe the main visualization types available in Splunk dashboards

---

## Why This Matters

Searches answer questions on demand. Reports and dashboards answer questions proactively — they deliver insight to stakeholders who do not know SPL and who should not need to log into Splunk to understand system health. Alerts close the loop on observability: instead of checking Splunk to discover an outage, Splunk tells you when an outage condition is detected.

This lesson brings together everything you have learned today — data inputs, event structure, SPL queries, knowledge objects — into the deliverables that the rest of your organization actually sees: a health dashboard for your application and alerts that wake someone up when something goes wrong.

---

## Reports: Scheduled, Saved Searches

A **report** in Splunk is a saved search that can be scheduled to run automatically and whose results can be delivered via email, stored for later viewing, or used to drive summary indexes.

### Saving a Search as a Report

Any search you run in Search & Reporting can be saved as a report:

1. Run and refine your search until you are satisfied with the output
2. Click **Save As → Report**
3. Fill in the details:
   - **Title:** Descriptive, consistent with your naming convention (e.g., `API Gateway - Error Summary - Daily`)
   - **Description:** What this report shows and who it is for
   - **Time Range:** Keep the picker range or override it (for scheduled reports, the time range is usually set relative, e.g., "Last 24 hours")
   - **Display:** Choose whether to default to Statistics view (for table output) or Visualization (for charts)

4. Click **Save**

### Scheduling a Report

After saving, click **Edit → Edit Schedule** (or from the Reports list, click the report's Edit menu):

**Enable Schedule:** Toggle on to enable scheduled execution.

**Schedule Type:** Choose from presets or Cron:
- **Run every hour / day / week / month** (simplified presets)
- **Cron Expression** — gives full control

### Cron Scheduling in Splunk

Cron expressions follow the standard five-field format:

```
┌───── minute (0-59)
│  ┌───── hour (0-23)
│  │  ┌───── day of month (1-31)
│  │  │  ┌───── month (1-12)
│  │  │  │  ┌───── day of week (0-7, 0 and 7 = Sunday)
│  │  │  │  │
*  *  *  *  *
```

**Common cron expressions for reports:**

| Schedule | Cron Expression | Description |
|---|---|---|
| Every 5 minutes | `*/5 * * * *` | Continuous monitoring check |
| Every hour | `0 * * * *` | Hourly summary |
| Daily at 8 AM | `0 8 * * *` | Morning team briefing |
| Weekdays at 8 AM | `0 8 * * 1-5` | Business days only |
| Monday at 9 AM | `0 9 * * 1` | Weekly review |
| 1st of month, midnight | `0 0 1 * *` | Monthly executive report |
| Daily at 2:30 AM | `30 2 * * *` | Off-hours heavy report |

**Example:** A daily API error summary sent to the engineering team every weekday morning:
```
Cron: 0 8 * * 1-5
Time range: Last 24 hours
```

### Configuring Email Delivery

In the report schedule settings, under **Schedule Actions → Send Email:**

- **To:** Comma-separated list of email addresses
- **Subject:** e.g., `[Splunk] Daily API Error Summary - {trigger_time}`
- **Message:** Optional body text
- **Include Results:** Choose to include inline results table in the email body, attach as CSV, attach as PDF, or attach as inline chart image
- **Send on empty results:** Usually disabled for error reports (no news is good news)

**Useful email variables in subject/body:**
- `{trigger_time}` — when the report ran
- `{app}` — the Splunk app
- `{name}` — the report name
- `{results_count}` — number of results returned

---

## Alerts: Triggered Notifications on Conditions

An **alert** is a saved search that runs on a schedule (or in real time) and triggers one or more **actions** when specified conditions are met. Unlike reports (which always deliver results), alerts only fire when a threshold is crossed.

### Alert vs. Report — When to Use Each

| Use Case | Choose |
|---|---|
| "Send me the error list every morning" | Report |
| "Notify me when error rate exceeds 5%" | Alert |
| "Weekly executive dashboard" | Report |
| "Page on-call if the service goes down" | Alert |
| "Archive metrics daily" | Report |
| "Slack message when deployment fails" | Alert |

### Creating an Alert

1. Run the search that detects the condition you care about
2. Click **Save As → Alert**
3. Configure:

**Alert Type:**
- **Scheduled:** Runs at set intervals (cron). After each run, checks whether the results meet your trigger condition.
- **Real-time:** Continuously searches incoming data. Triggers on each individual event that matches. Use carefully — real-time alerts are resource-intensive.

**Trigger Conditions (for Scheduled alerts):**

| Condition | Description | Example Use |
|---|---|---|
| **Number of results** | Triggers if result count is `>`, `<`, `=`, `>=`, `<=` a threshold | Alert when error count > 100 in the last hour |
| **Number of hosts** | Triggers based on count of unique hosts in results | Alert when errors appear on more than 3 hosts |
| **Number of sources** | Similar to hosts but for sources | |
| **Custom condition** | Evaluate an SPL expression on the results | Alert when avg_ms > 2000 (average response > 2s) |

**Throttling:** Prevent alert fatigue by throttling — after an alert fires, suppress it for a set period (e.g., do not re-alert for 1 hour even if the condition persists).

### Alert Actions

**Send Email:**
- Same configuration as scheduled reports
- Subject often includes: `[ALERT] Payment Error Rate Exceeded Threshold`

**Webhook:**
Sends an HTTP POST request to any URL when the alert fires. Used to integrate with:
- **Slack/Teams:** Post a message to an engineering channel
- **PagerDuty:** Create an incident and page on-call
- **Jira:** Automatically create a bug ticket
- **Custom scripts:** Trigger automated remediation

Webhook payload is configurable JSON:
```json
{
  "alert_name": "$name$",
  "trigger_time": "$trigger_time$",
  "results_count": "$result.count$",
  "search_url": "$results_link$"
}
```

**Run a Script:**
Execute a shell script or Python script on the Splunk server when the alert fires. Used for automated remediation (restart a service, clear a cache, notify a monitoring system).

**Add to Triggered Alerts:**
Logs the alert trigger to Splunk's own alert log — viewable in the Alerts app. Always enable this for audit trails.

### Complete Alert Example: High Error Rate

**Goal:** Alert the team when the payment service error rate exceeds 5% over the last 15 minutes.

**Search:**
```spl
index=prod_payments sourcetype=payment_json earliest=-15m
| stats count as total,
        sum(eval(if(level="ERROR", 1, 0))) as errors
  by index
| eval error_rate_pct = (errors / total) * 100
| where error_rate_pct > 5
```

**Alert settings:**
- Type: Scheduled
- Cron: `*/5 * * * *` (every 5 minutes)
- Trigger: Number of results is greater than 0 (i.e., any result means the condition was met)
- Throttle: 30 minutes (don't re-alert within 30 minutes)
- Actions: Send email to `eng-oncall@company.com` + Webhook to Slack channel

---

## Building Dashboards

A Splunk **dashboard** is a page containing one or more **panels**. Each panel displays the results of a search as a visualization (chart, table, single value, etc.). Dashboards give teams a real-time view of system health without requiring SPL knowledge.

### Dashboard Creation Workflow

**Option 1: Build from searches**
1. Run a search
2. Click **Save As → Dashboard Panel**
3. Choose an existing dashboard or create a new one
4. Select the visualization type
5. Repeat for each panel

**Option 2: Create dashboard first, add panels**
1. Go to **Dashboards → Create New Dashboard**
2. Add panels one at a time by clicking **Add Panel → New from Search**
3. Enter the SPL query for each panel
4. Select visualization type

**Option 3: Edit Simple XML directly**
For fine-grained control, click **Edit → Edit Source** to edit the underlying XML.

### Dashboard Input Controls

Dashboards support **input tokens** — dynamic variables that viewers can change via dropdowns, text fields, or time pickers, causing all panels to update.

Common inputs:
- **Time range picker:** Let viewers choose the analysis window
- **Dropdown:** Select a specific host, environment, or application
- **Text field:** Enter a userId or orderId to investigate
- **Radio button:** Switch between views (e.g., by host vs. by endpoint)

**Example: A time range token**

In Simple XML, a time range input looks like:
```xml
<input type="time" token="time_tok">
  <label>Time Range</label>
  <default>
    <earliest>-24h@h</earliest>
    <latest>now</latest>
  </default>
</input>
```

And panels reference it:
```xml
<query>index=prod_api level=ERROR earliest=$time_tok.earliest$ latest=$time_tok.latest$</query>
```

When the viewer changes the time picker, `$time_tok.earliest$` and `$time_tok.latest$` update, and all panels re-execute their searches with the new time range.

---

## Visualization Types

Choosing the right visualization is as important as writing the right query. The wrong chart type can obscure the very pattern you are trying to show.

### Timechart

**What it shows:** Values over time — how a metric changes as time progresses.

**Best for:** Trends, patterns, comparisons of rates over time.

**SPL command:** `| timechart`

```spl
-- Error count over time, one line per log level
index=prod_api earliest=-24h
| timechart span=1h count by level

-- Average response time trend with upper bound
index=prod_api earliest=-24h
| timechart span=15m avg(durationMs) as avg_ms, perc95(durationMs) as p95_ms
```

In the visualization, each field (`avg_ms`, `p95_ms`) becomes a separate line on the line chart. The x-axis is always time.

### Bar Chart / Column Chart

**What it shows:** Comparison of values across discrete categories.

**Best for:** Ranking — "which endpoint has the most errors," "which host is slowest."

**SPL command:** `| stats` or `| top` followed by the chart visualization tab.

```spl
-- Error count per endpoint -- perfect for a bar chart
index=prod_api level=ERROR earliest=-24h
| stats count as error_count by endpoint
| sort -error_count
| head 10
```

### Single Value (Scoreboard)

**What it shows:** A single number, optionally with a label, trend indicator, and color threshold (green/yellow/red).

**Best for:** KPIs and status indicators on monitoring dashboards — "Total Errors Today," "Current Error Rate," "Uptime."

```spl
-- Total error count in last hour
index=prod_api level=ERROR earliest=-1h
| stats count as total_errors

-- With a trend: compare to previous hour
index=prod_api level=ERROR earliest=-2h latest=-1h
| stats count as previous_hour_errors
| appendcols [
    search index=prod_api level=ERROR earliest=-1h
    | stats count as current_hour_errors
  ]
| eval trend = if(current_hour_errors > previous_hour_errors, "increasing", "decreasing")
```

**Single value color thresholds:** In the visualization editor, you can set thresholds:
- `< 10` errors: green background
- `10-50` errors: yellow
- `> 50` errors: red

This gives instant traffic-light status to non-technical stakeholders.

### Table

**What it shows:** Structured tabular data — multiple fields and rows.

**Best for:** Detailed lists for investigation — recent errors, affected users, slow endpoints with multiple attributes.

```spl
-- Recent errors table for dashboard
index=prod_api level=ERROR earliest=-1h
| sort -_time
| head 20
| table _time, host, endpoint, userId, errorCode, message, durationMs
```

### Area Chart

**What it shows:** Like a timechart/line chart but with the area under each line filled in. Useful for showing volume over time, especially stacked areas showing composition.

```spl
-- Request volume by status class over time -- stacked area shows composition
index=prod_api sourcetype=access_combined earliest=-24h
| eval status_class = case(status >= 500, "5xx", status >= 400, "4xx", true(), "2xx/3xx")
| timechart span=1h count by status_class
```

### Pie / Donut Chart

**What it shows:** Proportional breakdown of a whole — each slice is a percentage.

**Best for:** Small number of categories (2-5). Becomes unreadable with many slices.

```spl
-- Distribution of log levels
index=prod_api earliest=-24h
| stats count by level
```

**Caution:** Pie charts are often misused. If you have more than 5 categories or the question is "how have proportions changed over time," a stacked bar or area chart is better.

### Choropleth Map

**What it shows:** Geographic distribution of a metric, where regions are colored by value intensity.

**Best for:** Geographic analysis — request volume by country, error rates by region, latency by data center location.

**Requires:** A field containing geographic identifiers (country code, US state abbreviation, or coordinates). Splunk includes built-in geographic lookup tables.

```spl
-- Request volume by country (requires country field in data)
index=prod_api sourcetype=access_combined earliest=-24h
| iplocation clientip       -- enriches with country, region, city from IP
| stats count as requests by Country
| geom geo_countries featureIdField=Country
```

---

## Simple XML Dashboard Structure

Every Splunk Classic Dashboard is stored as Simple XML. Understanding the structure lets you version-control your dashboards (store in Git), share them between Splunk instances, and make precise edits that the GUI does not expose.

### Overall Structure

```xml
<dashboard version="1.1">

  <!-- Dashboard title shown in the browser tab and header -->
  <label>Payment Service Health Dashboard</label>
  <description>Real-time monitoring for the Payment API -- error rates, latency, and throughput</description>

  <!-- Input controls that create tokens usable in panel queries -->
  <fieldset submitButton="false" autoRun="true">
    <input type="time" token="time_tok" searchWhenChanged="true">
      <label>Time Range</label>
      <default>
        <earliest>-1h</earliest>
        <latest>now</latest>
      </default>
    </input>
    <input type="dropdown" token="env_tok" searchWhenChanged="true">
      <label>Environment</label>
      <choice value="prod_payments">Production</choice>
      <choice value="staging_payments">Staging</choice>
      <default>prod_payments</default>
    </input>
  </fieldset>

  <!-- A row groups panels horizontally -->
  <row>
    <!-- Single value panel: total error count -->
    <panel>
      <title>Errors in Period</title>
      <single>
        <search>
          <query>index=$env_tok$ level=ERROR | stats count as total_errors</query>
          <earliest>$time_tok.earliest$</earliest>
          <latest>$time_tok.latest$</latest>
        </search>
        <option name="colorBy">value</option>
        <option name="colorMode">background</option>
        <option name="rangeColors">["0x65A637","0xF7BC38","0xD93F3C"]</option>
        <option name="rangeValues">[10,50]</option>
        <option name="underLabel">Total Errors</option>
      </single>
    </panel>

    <!-- Single value panel: unique affected users -->
    <panel>
      <title>Affected Users</title>
      <single>
        <search>
          <query>index=$env_tok$ level=ERROR | stats dc(userId) as affected_users</query>
          <earliest>$time_tok.earliest$</earliest>
          <latest>$time_tok.latest$</latest>
        </search>
        <option name="underLabel">Unique Users Impacted</option>
      </single>
    </panel>
  </row>

  <!-- Second row: a timechart panel -->
  <row>
    <panel>
      <title>Error Rate Over Time</title>
      <chart>
        <search>
          <query>
            index=$env_tok$ earliest=$time_tok.earliest$ latest=$time_tok.latest$
            | timechart span=5m
                sum(eval(if(level="ERROR", 1, 0))) as errors,
                count as total
            | eval error_rate_pct = round((errors / total) * 100, 2)
            | fields _time, error_rate_pct
          </query>
          <earliest>$time_tok.earliest$</earliest>
          <latest>$time_tok.latest$</latest>
        </search>
        <option name="charting.chart">line</option>
        <option name="charting.axisTitleX.text">Time</option>
        <option name="charting.axisTitleY.text">Error Rate (%)</option>
        <option name="charting.chart.nullValueMode">connect</option>
      </chart>
    </panel>
  </row>

  <!-- Third row: a table panel showing recent errors -->
  <row>
    <panel>
      <title>Recent Errors (Last 20)</title>
      <table>
        <search>
          <query>
            index=$env_tok$ level=ERROR
            | sort -_time
            | head 20
            | table _time, host, endpoint, userId, errorCode, durationMs, message
            | rename durationMs as "Duration (ms)"
          </query>
          <earliest>$time_tok.earliest$</earliest>
          <latest>$time_tok.latest$</latest>
        </search>
        <option name="wrap">true</option>
        <option name="rowNumbers">true</option>
        <option name="dataOverlayMode">none</option>
      </table>
    </panel>
  </row>

  <!-- Fourth row: a bar chart of errors by endpoint -->
  <row>
    <panel>
      <title>Top Endpoints by Error Count</title>
      <chart>
        <search>
          <query>
            index=$env_tok$ level=ERROR
            | stats count as error_count by endpoint
            | sort -error_count
            | head 10
          </query>
          <earliest>$time_tok.earliest$</earliest>
          <latest>$time_tok.latest$</latest>
        </search>
        <option name="charting.chart">bar</option>
        <option name="charting.axisTitleX.text">Endpoint</option>
        <option name="charting.axisTitleY.text">Error Count</option>
        <option name="charting.legend.placement">none</option>
      </chart>
    </panel>

    <!-- A pie chart panel in the same row -->
    <panel>
      <title>Errors by Error Code</title>
      <chart>
        <search>
          <query>
            index=$env_tok$ level=ERROR
            | stats count by errorCode
            | sort -count
            | head 8
          </query>
          <earliest>$time_tok.earliest$</earliest>
          <latest>$time_tok.latest$</latest>
        </search>
        <option name="charting.chart">pie</option>
        <option name="charting.legend.placement">right</option>
      </chart>
    </panel>
  </row>

</dashboard>
```

### Key Simple XML Elements Reference

| XML Element | Purpose |
|---|---|
| `<dashboard>` | Root element, `version="1.1"` for Classic dashboards |
| `<label>` | Dashboard title |
| `<description>` | Dashboard subtitle/description |
| `<fieldset>` | Container for input controls |
| `<input type="time">` | Time range picker |
| `<input type="dropdown">` | Dropdown menu |
| `<input type="text">` | Free-text input |
| `<row>` | Horizontal grouping of panels |
| `<panel>` | A single visualization unit |
| `<title>` | Panel title |
| `<single>` | Single value visualization |
| `<chart>` | Chart visualization (type set via `charting.chart` option) |
| `<table>` | Table visualization |
| `<search>` | Search definition for a panel |
| `<query>` | The SPL query string |
| `<earliest>` / `<latest>` | Time range for the panel's search |
| `<option>` | Visualization setting (chart type, axis labels, colors) |

### Chart Type Values for `charting.chart`

| Option Value | Chart Type |
|---|---|
| `line` | Line chart |
| `area` | Area chart |
| `bar` | Horizontal bar chart |
| `column` | Vertical column/bar chart |
| `pie` | Pie chart |
| `scatter` | Scatter plot |
| `bubble` | Bubble chart |
| `fillerGauge` | Gauge (fill style) |
| `radialGauge` | Gauge (radial style) |
| `markerGauge` | Gauge (marker style) |

---

## Dashboard Best Practices

### Design for Your Audience

**Operations/On-call dashboards:** Prioritize single-value tiles with color thresholds at the top. The engineer arriving during an incident needs to know IMMEDIATELY what is broken. Put time series charts below for context.

**Executive/Management dashboards:** Aggregate KPIs, trend lines, fewer panels. Use business language in labels, not technical field names.

**Developer/Debug dashboards:** Detailed tables, raw event counts, fine-grained time breakdowns. Include drill-down links to pre-built searches.

### Naming and Organization

- Use consistent naming: `<team> - <function> - <audience>` (e.g., `Payments - Error Analysis - Engineering`)
- Add descriptions to dashboards explaining their purpose and data sources
- Group related dashboards in a dedicated Splunk app

### Performance Considerations

- **Avoid `index=*` in dashboard panels** — always specify the index
- **Set reasonable time ranges as defaults** — a dashboard defaulting to "Last 7 days" on a high-volume index will be slow for everyone who opens it
- **Use saved searches as panel sources** — if the same search powers multiple panels or multiple dashboards, define it once as a saved search with its own acceleration
- **Enable report acceleration** for heavy searches — Splunk pre-computes results and stores a summary index, making the dashboard panel load in seconds instead of minutes

---

## Summary

Reports deliver scheduled search results to stakeholders via email. Alerts detect threshold conditions and trigger actions — email, webhook, script — to notify teams proactively. Dashboards combine multiple panels into a unified view, with input controls that let viewers customize their perspective.

Simple XML is the underlying representation of Classic Dashboards — learning to read and write it gives you version control, portability, and fine-grained control over every dashboard element.

Together, reports, alerts, and dashboards complete the observability story for your applications. You have data flowing into Splunk from your AWS-deployed, Docker-containerized applications. You have SPL queries that extract meaningful signals from that data. And now you have the tools to deliver those signals — automatically, visually, and proactively — to everyone who needs them.

This is what it means to operate software professionally in a modern cloud environment.

---

## External Resources

- [Splunk Docs: Create and Edit Dashboards](https://docs.splunk.com/Documentation/Splunk/latest/Viz/CreateDashboards)
- [Splunk Docs: Simple XML Reference](https://docs.splunk.com/Documentation/Splunk/latest/Viz/PanelreferenceforSimplifiedXML)
- [Splunk Alerting Manual](https://docs.splunk.com/Documentation/Splunk/latest/Alert/Aboutalerts)
