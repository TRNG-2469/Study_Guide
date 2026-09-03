# AWS API Gateway — Types, Integration Patterns, and CLI Reference

## Learning Objectives

By the end of this lesson, you will be able to:

- Distinguish between REST APIs, HTTP APIs, and WebSocket APIs in API Gateway
- Choose the right API type for a given use case
- Explain the four integration types and when to use each
- Configure stage variables and mapping templates
- Set up CORS correctly for browser-based clients
- Execute the most important AWS CLI commands for managing API Gateway

---

## Why This Matters

The previous lesson walked you through creating a single REST API by hand. This lesson gives you the broader framework: understanding all API types, integration patterns, and the CLI commands you will use to automate API management in scripts and CI/CD pipelines. Knowing the trade-offs between API types prevents costly architectural mistakes and helps you design systems that are both performant and cost-effective.

---

## API Gateway API Types

API Gateway supports three distinct API types, each designed for a different communication pattern.

### 1. REST APIs

The original API Gateway offering. REST APIs provide the richest feature set.

**Features unique to REST APIs:**
- Request/response transformation via mapping templates
- API keys and usage plans
- Per-method throttling
- Caching (API Gateway edge cache)
- Custom domain names with certificates
- Resource policies (IP-based allow/deny)
- WAF (Web Application Firewall) integration

**Pricing:** ~$3.50 per million API calls

**Best for:**
- Public APIs requiring fine-grained access control
- APIs that need request/response transformation
- Enterprise integrations
- APIs fronting non-HTTP backends (e.g., AWS Step Functions, DynamoDB directly)

### 2. HTTP APIs (Recommended for Most New Projects)

HTTP APIs are a newer, simplified version of REST APIs with lower latency and lower cost.

**Advantages over REST APIs:**
- ~60% lower cost (~$1.00 per million calls)
- Lower latency (fewer processing layers)
- Native JWT/OIDC authorizers (simpler than REST API custom authorizers)
- Built-in CORS configuration

**Limitations compared to REST APIs:**
- No request/response transformation (mapping templates not available)
- No API key + usage plan support
- No edge caching
- Fewer integration types

**Best for:**
- Lambda backends
- Simple HTTP proxy to backends (EC2, ECS, ALB)
- APIs with standard JWT authentication (Auth0, Cognito, etc.)
- Cost-sensitive high-volume APIs

### 3. WebSocket APIs

WebSocket APIs maintain a persistent, bidirectional connection between client and server.

**How it works:**
```
Client                    API Gateway               Backend (Lambda / EC2)
  │                           │                           │
  │  WebSocket connect        │                           │
  │──────────────────────────►│  $connect route           │
  │                           │──────────────────────────►│
  │                           │                           │
  │  Send message             │                           │
  │──────────────────────────►│  $default route           │
  │                           │──────────────────────────►│
  │                           │                           │
  │◄──────────────────────────│  Server-initiated push    │
  │                           │◄──────────────────────────│
  │  WebSocket disconnect     │                           │
  │──────────────────────────►│  $disconnect route        │
  │                           │──────────────────────────►│
```

**Best for:**
- Real-time chat applications
- Live dashboards and data feeds
- Multiplayer games
- Collaborative editing tools (like Google Docs)
- Any use case requiring server-to-client push without polling

### API Type Comparison Table

| Feature | REST API | HTTP API | WebSocket API |
|---|---|---|---|
| Protocol | HTTP/HTTPS | HTTP/HTTPS | WebSocket |
| Communication | Request/Response | Request/Response | Bidirectional |
| Cost (per million) | ~$3.50 | ~$1.00 | ~$1.00 + $0.25/million messages |
| Mapping templates | Yes | No | Yes |
| JWT authorizer | Via Lambda | Built-in | No |
| Caching | Yes | No | No |
| Usage plans/API keys | Yes | No | No |
| Best starting point | Complex APIs | New projects | Real-time use cases |

---

## Integration Types

An **integration** defines how API Gateway forwards requests to the backend. There are four integration types.

### 1. Lambda Function Integration

Routes requests to an AWS Lambda function. This is the most common integration.

```
Client ──► API Gateway ──► Lambda Function ──► Response
```

**Two modes:**
- **Lambda Proxy Integration (recommended):** API Gateway passes the entire request (headers, body, path params, query strings) as a JSON event to Lambda. Lambda must return a specific JSON structure.
- **Lambda Non-Proxy Integration:** API Gateway can transform the request before sending to Lambda and transform the response after. Requires mapping templates.

Lambda Proxy event structure (what Lambda receives):

```json
{
  "httpMethod": "GET",
  "path": "/users",
  "pathParameters": null,
  "queryStringParameters": {"page": "1", "limit": "10"},
  "headers": {
    "Content-Type": "application/json",
    "Authorization": "Bearer eyJhb..."
  },
  "body": null,
  "isBase64Encoded": false
}
```

Lambda must return:

```json
{
  "statusCode": 200,
  "headers": {
    "Content-Type": "application/json"
  },
  "body": "[{\"id\": 1, \"name\": \"Alice\"}]"
}
```

### 2. HTTP Integration (Proxy to EC2/ECS/External URL)

Forwards the request to an HTTP endpoint — your EC2 instance, ECS service, load balancer, or any external URL.

**Two modes:**
- **HTTP Proxy Integration (recommended for simple cases):** Passes the request through unchanged; the backend response is returned directly.
- **HTTP Non-Proxy Integration:** Allows request/response transformation via mapping templates.

```bash
# Create an HTTP proxy integration
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --type HTTP_PROXY \
  --integration-http-method GET \
  --uri "http://54.123.45.67:8080/users"
```

### 3. Mock Integration

Returns a response directly from API Gateway without ever contacting a backend. Useful for:
- API development before the backend is ready
- Testing client behavior with predetermined responses
- Returning static error responses

```bash
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --type MOCK \
  --request-templates '{"application/json": "{\"statusCode\": 200}"}'

# Configure the response body
aws apigateway put-integration-response \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --status-code 200 \
  --response-templates '{"application/json": "[{\"id\": 1, \"name\": \"Mock User\"}]"}'
```

### 4. AWS Service Integration

Routes requests directly to AWS services (DynamoDB, SQS, SNS, S3, Step Functions) without writing Lambda code.

```bash
# Example: Write to SQS directly from API Gateway (no Lambda needed)
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method POST \
  --type AWS \
  --integration-http-method POST \
  --uri "arn:aws:apigateway:us-east-1:sqs:path/123456789012/myqueue" \
  --credentials "arn:aws:iam::123456789012:role/APIGatewayToSQSRole" \
  --request-parameters '{"integration.request.header.Content-Type": "'application/x-www-form-urlencoded'"}'
```

---

## Stage Variables

**Stage variables** are key-value pairs associated with a deployment stage. They act like environment variables for your API — letting you configure different backends, feature flags, or settings per stage without changing your API definition.

### Setting Stage Variables

```bash
aws apigateway update-stage \
  --rest-api-id $API_ID \
  --stage-name dev \
  --patch-operations \
    op=replace,path=/variables/backendHost,value=54.123.45.67 \
    op=replace,path=/variables/appPort,value=8080
```

### Referencing Stage Variables in Integration URIs

In the integration URI, reference stage variables with `${stageVariables.variableName}`:

```
http://${stageVariables.backendHost}:${stageVariables.appPort}/users
```

This allows:
- `dev` stage → `http://dev-ec2-ip:8080/users`
- `prod` stage → `http://prod-alb-url:8080/users`

With a single API definition and no code changes between environments.

---

## Mapping Templates

**Mapping templates** transform request/response data between API Gateway and the backend. They are written in **VTL (Velocity Template Language)**.

> Note: Mapping templates are only available in REST APIs (not HTTP APIs).

### When to Use Mapping Templates

- Your backend expects a different request format than what the client sends
- You want to extract specific fields from the request body
- You need to rename fields, add headers, or restructure the response

### Example: Transform a POST Body

Client sends:
```json
{"firstName": "Alice", "lastName": "Smith", "email": "alice@example.com"}
```

Backend expects:
```json
{"name": "Alice Smith", "contactEmail": "alice@example.com"}
```

Mapping template (request transformation):

```velocity
#set($body = $input.path('$'))
{
  "name": "$body.firstName $body.lastName",
  "contactEmail": "$body.email"
}
```

Apply it:

```bash
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method POST \
  --type HTTP \
  --integration-http-method POST \
  --uri "http://54.123.45.67:8080/users" \
  --request-templates '{"application/json": "#set($body = $input.path(\"$\")){\"name\": \"$body.firstName $body.lastName\", \"contactEmail\": \"$body.email\"}"}'
```

### Useful VTL Variables

| Variable | Description |
|---|---|
| `$input.body` | The raw request body as a string |
| `$input.path('$.field')` | Extract a field from the JSON body |
| `$context.requestId` | Unique request ID from API Gateway |
| `$context.identity.sourceIp` | Client's IP address |
| `$stageVariables.variableName` | Stage variable value |
| `$util.escapeJavaScript(str)` | Escape a string for JSON embedding |

---

## CORS Configuration

**CORS (Cross-Origin Resource Sharing)** must be configured when browsers make API calls from a different domain than the API.

### How CORS Works in API Gateway

1. The browser first sends an **OPTIONS preflight request** to check if the API allows cross-origin requests.
2. API Gateway must respond to OPTIONS with the appropriate CORS headers.
3. The browser then sends the actual request.

### Enabling CORS via CLI (REST API)

```bash
# Create the OPTIONS method on the resource
aws apigateway put-method \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method OPTIONS \
  --authorization-type NONE

# Create a MOCK integration for OPTIONS (returns headers directly, no backend call)
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method OPTIONS \
  --type MOCK \
  --request-templates '{"application/json": "{\"statusCode\": 200}"}'

# Configure the 200 response with CORS headers
aws apigateway put-method-response \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method OPTIONS \
  --status-code 200 \
  --response-parameters '{
    "method.response.header.Access-Control-Allow-Headers": false,
    "method.response.header.Access-Control-Allow-Methods": false,
    "method.response.header.Access-Control-Allow-Origin": false
  }'

aws apigateway put-integration-response \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method OPTIONS \
  --status-code 200 \
  --response-parameters '{
    "method.response.header.Access-Control-Allow-Headers": "'"'"'Content-Type,Authorization,X-Api-Key'"'"'",
    "method.response.header.Access-Control-Allow-Methods": "'"'"'GET,POST,PUT,DELETE,OPTIONS'"'"'",
    "method.response.header.Access-Control-Allow-Origin": "'"'"'*'"'"'"
  }'
```

### CORS for HTTP APIs (simpler)

HTTP APIs have built-in CORS configuration:

```bash
aws apigatewayv2 update-api \
  --api-id $API_ID \
  --cors-configuration \
    AllowOrigins='["*"]',\
    AllowMethods='["GET","POST","PUT","DELETE","OPTIONS"]',\
    AllowHeaders='["Content-Type","Authorization"]',\
    MaxAge=300
```

---

## Key AWS CLI Commands for API Gateway

### REST API (apigateway)

```bash
# List all REST APIs
aws apigateway get-rest-apis

# Get all resources (paths) in an API
aws apigateway get-resources --rest-api-id $API_ID

# Get all stages for an API
aws apigateway get-stages --rest-api-id $API_ID

# Get details of a specific stage (including stage variables)
aws apigateway get-stage \
  --rest-api-id $API_ID \
  --stage-name dev

# Get the integration for a specific method
aws apigateway get-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET

# Update throttling on a stage
aws apigateway update-stage \
  --rest-api-id $API_ID \
  --stage-name dev \
  --patch-operations \
    op=replace,path=/defaultRouteSettings/throttlingRateLimit,value=100

# Create a deployment (publish changes to a stage)
aws apigateway create-deployment \
  --rest-api-id $API_ID \
  --stage-name dev

# Delete an API
aws apigateway delete-rest-api --rest-api-id $API_ID

# Export the API definition (OpenAPI/Swagger format)
aws apigateway get-export \
  --rest-api-id $API_ID \
  --stage-name dev \
  --export-type oas30 \
  --output json \
  --accepts application/json \
  api-definition.json
```

### HTTP API (apigatewayv2)

HTTP APIs use a different CLI command namespace: `apigatewayv2`.

```bash
# Create an HTTP API
aws apigatewayv2 create-api \
  --name myapp-http-api \
  --protocol-type HTTP \
  --target "http://54.123.45.67:8080"   # Simple catch-all proxy

# List HTTP APIs
aws apigatewayv2 get-apis

# Create a route on an HTTP API
aws apigatewayv2 create-route \
  --api-id $HTTP_API_ID \
  --route-key "GET /users"

# Create an integration for the route
aws apigatewayv2 create-integration \
  --api-id $HTTP_API_ID \
  --integration-type HTTP_PROXY \
  --integration-method GET \
  --integration-uri "http://54.123.45.67:8080/users" \
  --payload-format-version "1.0"

# Deploy the HTTP API
aws apigatewayv2 create-deployment \
  --api-id $HTTP_API_ID \
  --stage-name dev

# Get the HTTP API endpoint URL
aws apigatewayv2 get-api \
  --api-id $HTTP_API_ID \
  --query 'ApiEndpoint'
```

---

## Viewing API Gateway Logs and Metrics

### Enable Access Logging

```bash
# Create a CloudWatch log group for API access logs
aws logs create-log-group --log-group-name /apigateway/myapp-api

# Enable logging on the stage
aws apigateway update-stage \
  --rest-api-id $API_ID \
  --stage-name dev \
  --patch-operations \
    op=replace,path=/accessLogSettings/destinationArn,value=arn:aws:logs:us-east-1:123456789012:log-group:/apigateway/myapp-api \
    op=replace,path=/accessLogSettings/format,value='{"requestId":"$context.requestId","ip":"$context.identity.sourceIp","method":"$context.httpMethod","path":"$context.path","status":"$context.status","responseTime":"$context.responseLatency"}'
```

### Key Metrics in CloudWatch

| Metric | Description |
|---|---|
| `Count` | Total number of API calls |
| `4XXError` | Client errors (400-499) |
| `5XXError` | Server/backend errors (500-599) |
| `Latency` | Total time from request receipt to response send |
| `IntegrationLatency` | Time API Gateway spent waiting for the backend |
| `CacheHitCount` | Requests served from cache (REST API with caching enabled) |
| `CacheMissCount` | Requests that bypassed cache and went to the backend |

---

## Summary

| Topic | Key Point |
|---|---|
| REST API | Full-featured; best for complex APIs, request transformation, usage plans |
| HTTP API | Simpler and cheaper; best for most new projects with Lambda or HTTP backends |
| WebSocket API | Bidirectional, persistent connections; best for real-time applications |
| Lambda integration | Proxy passes full event; non-proxy allows transformation |
| HTTP integration | Proxy passes request through; non-proxy allows transformation via VTL |
| Mock integration | Returns hardcoded responses; no backend call |
| AWS service integration | Direct backend calls to DynamoDB, SQS, etc. |
| Stage variables | Per-stage key-value config (like environment variables for APIs) |
| Mapping templates | VTL-based request/response transformation (REST API only) |
| CORS | Required for browser clients; OPTIONS method returns Allow headers |
| Throttling | Rate limit + burst limit protect the backend |

---

## External Resources

- [Choose Between REST and HTTP APIs](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-vs-rest.html)
- [API Gateway Mapping Template Reference (VTL)](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-mapping-template-reference.html)
- [API Gateway CLI Reference (apigateway)](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/apigateway/index.html)
