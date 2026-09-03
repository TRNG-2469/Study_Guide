# API Gateway — Creation and Management

## Learning Objectives

By the end of this lesson, you will be able to:

- Create a REST API in API Gateway using the AWS Console
- Configure resources (URL paths) and HTTP methods
- Integrate an API Gateway method with an EC2-hosted Spring Boot backend
- Deploy an API to a named stage
- Test the deployed API with Postman
- Configure throttling and usage quotas to protect your backend

---

## Why This Matters

You have an EC2 instance running a Spring Boot application. How do clients reach it? You could expose the EC2's public IP directly — but that means managing SSL certificates, rate limiting, authentication, monitoring, and CORS yourself. **API Gateway** handles all of that as a managed service, sitting in front of your backend and providing a professional, scalable, secure HTTP endpoint. This is the final piece of the Week 8 deployment puzzle: infrastructure (EC2) → containers (Docker/ECS) → public access (API Gateway).

---

## What Is API Gateway?

**Amazon API Gateway** is a fully managed service that lets you create, publish, maintain, monitor, and secure APIs at any scale. It acts as the "front door" for your backend services.

```
Client (Browser / Postman / Mobile App)
        │
        │  HTTPS request to API Gateway endpoint
        ▼
  ┌─────────────────────────────────┐
  │        AWS API Gateway          │
  │  ┌─────────────────────────┐   │
  │  │  Auth / Throttling /    │   │
  │  │  CORS / Logging         │   │
  │  └──────────┬──────────────┘   │
  └─────────────┼───────────────────┘
                │  forwards valid requests
                ▼
  ┌─────────────────────────────────┐
  │  EC2 (Spring Boot on port 8080) │
  └─────────────────────────────────┘
```

---

## Step 1 — Create a REST API

### Using the AWS Console

1. Open **API Gateway Console** → Click **Create API**
2. Select **REST API** → Click **Build**
3. Configure:
   - **Protocol:** REST
   - **Create new API:** New API
   - **API name:** `myapp-api`
   - **Description:** `REST API for myapp Spring Boot backend`
   - **Endpoint type:** Regional (recommended for single-region deployments)
4. Click **Create API**

You now have an empty API with no resources or methods.

### Using the AWS CLI

```bash
# Create the API
aws apigateway create-rest-api \
  --name myapp-api \
  --description "REST API for myapp Spring Boot backend" \
  --endpoint-configuration types=REGIONAL \
  --region us-east-1

# The command returns the API ID — save this value
# Example: "id": "abc123def4"
API_ID="abc123def4"
```

---

## Step 2 — Create a Resource (URL Path)

A **resource** is a URL path in your API (e.g., `/users`, `/products`, `/health`).

### Using the Console

1. In the API editor, click **Actions → Create Resource**
2. **Resource Name:** `users`
3. **Resource Path:** `/users`
4. **Enable API Gateway CORS:** Check this box (prevents CORS errors from browser clients)
5. Click **Create Resource**

### Using the CLI

```bash
# Get the root resource ID (the "/" path that exists by default)
ROOT_RESOURCE_ID=$(aws apigateway get-resources \
  --rest-api-id $API_ID \
  --query 'items[?path==`/`].id' \
  --output text)

# Create the /users resource under root
aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $ROOT_RESOURCE_ID \
  --path-part users

# The command returns the new resource ID
RESOURCE_ID="xyz789abc0"
```

---

## Step 3 — Create a Method

A **method** is an HTTP verb (GET, POST, PUT, DELETE) attached to a resource.

### Using the Console

1. Select the `/users` resource in the left panel
2. Click **Actions → Create Method**
3. Select **GET** from the dropdown → Click the checkmark
4. In the method setup dialog:
   - **Integration type:** HTTP
   - **Use HTTP Proxy integration:** unchecked (for full control)
   - **HTTP method:** GET
   - **Endpoint URL:** `http://<your-ec2-public-ip>:8080/users`
5. Click **Save**

### Using the CLI

```bash
# Create the GET method on the /users resource
aws apigateway put-method \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --authorization-type NONE \        # No auth for this demo; use AWS_IAM or Cognito in production
  --no-api-key-required

# Create the integration — forward GET /users to the Spring Boot backend
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --type HTTP \
  --integration-http-method GET \
  --uri "http://54.123.45.67:8080/users"   # Your EC2 public IP and Spring Boot port
```

---

## Step 4 — Configure Method Response and Integration Response

For a proper REST API, API Gateway needs to know what HTTP status codes to return and how to map them.

### Using the Console

After setting up the integration, API Gateway prompts you to configure:
- **Method Response:** What HTTP codes your API exposes (add 200)
- **Integration Response:** How to map backend responses to method responses (default mapping works for most cases)

### Using the CLI

```bash
# Define that the method can return a 200 response
aws apigateway put-method-response \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --status-code 200 \
  --response-models '{"application/json": "Empty"}'

# Map the backend's response to the 200 method response
aws apigateway put-integration-response \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE_ID \
  --http-method GET \
  --status-code 200 \
  --selection-pattern ""
```

---

## Step 5 — Enable CORS

CORS (Cross-Origin Resource Sharing) is required if your API will be called from a browser (React, Angular, etc.) hosted on a different domain.

### Using the Console

1. Select the `/users` resource
2. Click **Actions → Enable CORS**
3. Leave the defaults (allows all origins — refine for production)
4. Click **Enable CORS and replace existing CORS headers**

API Gateway creates an `OPTIONS` method and adds the necessary headers automatically.

### Key CORS Headers API Gateway Adds

```
Access-Control-Allow-Origin: '*'
Access-Control-Allow-Methods: 'GET,POST,PUT,DELETE,OPTIONS'
Access-Control-Allow-Headers: 'Content-Type,Authorization'
```

---

## Step 6 — Deploy the API to a Stage

Changes to an API are not live until you **deploy** them to a **stage**. A stage is a named snapshot of your API (e.g., `dev`, `staging`, `production`).

### Using the Console

1. Click **Actions → Deploy API**
2. **Deployment stage:** [New Stage]
3. **Stage name:** `dev`
4. **Stage description:** `Development environment`
5. Click **Deploy**

API Gateway displays your **Invoke URL**:
```
https://abc123def4.execute-api.us-east-1.amazonaws.com/dev
```

Your `/users` endpoint is now accessible at:
```
https://abc123def4.execute-api.us-east-1.amazonaws.com/dev/users
```

### Using the CLI

```bash
# Deploy the API to a stage named "dev"
aws apigateway create-deployment \
  --rest-api-id $API_ID \
  --stage-name dev \
  --stage-description "Development environment" \
  --description "Initial deployment"

# Construct your endpoint URL:
# https://<api-id>.execute-api.<region>.amazonaws.com/<stage>/<resource>
echo "API URL: https://$API_ID.execute-api.us-east-1.amazonaws.com/dev/users"
```

---

## Step 7 — Test with Postman

1. Open **Postman**
2. Create a new request:
   - **Method:** GET
   - **URL:** `https://abc123def4.execute-api.us-east-1.amazonaws.com/dev/users`
3. Click **Send**
4. You should see the response from your Spring Boot application

### Troubleshooting Common Issues

| Symptom | Likely Cause | Fix |
|---|---|---|
| 502 Bad Gateway | Backend (EC2) is not reachable from API Gateway | Check EC2 security group: allow port 8080 from `0.0.0.0/0` or from API Gateway's IP ranges |
| 403 Forbidden | Method requires API key or auth | Check authorization type; add API key header if required |
| CORS error in browser | OPTIONS method missing or headers incorrect | Re-run "Enable CORS" in console |
| 504 Timeout | Backend is too slow to respond (> 29 second limit) | Optimize the backend; consider async patterns |

---

## Step 8 — Throttling and Quota Settings

API Gateway can protect your backend from being overwhelmed by too many requests.

### Stage-Level Throttling

```bash
aws apigateway update-stage \
  --rest-api-id $API_ID \
  --stage-name dev \
  --patch-operations \
    op=replace,path=/defaultRouteSettings/throttlingBurstLimit,value=100 \
    op=replace,path=/defaultRouteSettings/throttlingRateLimit,value=50
```

| Setting | Meaning |
|---|---|
| **Rate limit** | Maximum steady-state requests per second across all methods |
| **Burst limit** | Maximum concurrent requests (token bucket capacity) |

### Method-Level Throttling

You can override throttling for specific methods (e.g., stricter limits on expensive operations):

1. **Console:** Stage Editor → select a method → set override throttling
2. **CLI:**

```bash
aws apigateway update-stage \
  --rest-api-id $API_ID \
  --stage-name dev \
  --patch-operations \
    op=replace,path=/~1users/GET/throttling/rateLimit,value=10
```

### Usage Plans and API Keys

For APIs exposed to external clients, use **usage plans** and **API keys**:

```bash
# Create a usage plan
aws apigateway create-usage-plan \
  --name myapp-usage-plan \
  --api-stages apiId=$API_ID,stage=dev \
  --throttle burstLimit=200,rateLimit=100 \
  --quota limit=10000,period=MONTH

# Create an API key for a client
aws apigateway create-api-key \
  --name my-client-key \
  --enabled

# Associate the key with the usage plan
aws apigateway create-usage-plan-key \
  --usage-plan-id <plan-id> \
  --key-id <key-id> \
  --key-type API_KEY
```

Clients then pass the key in the `x-api-key` request header:

```bash
curl -H "x-api-key: abc123..." https://abc123def4.execute-api.us-east-1.amazonaws.com/dev/users
```

---

## Managing Multiple Stages

Real-world applications use multiple stages for different environments:

```
                    ┌──────────────────────────────┐
                    │         myapp-api             │
                    └──────────┬───────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
      Stage: dev          Stage: staging      Stage: prod
   /dev/users             /staging/users      /prod/users
   Rate: 50 req/s         Rate: 200 req/s     Rate: 1000 req/s
   Backend: dev EC2       Backend: stage EC2  Backend: prod ALB
```

Use stage variables to configure the backend URL per stage without changing code:

1. In the stage, define variable `backendUrl = http://54.123.45.67:8080`
2. In the integration URI, reference it: `http://${stageVariables.backendUrl}/users`

---

## Updating and Redeploying

After changing resources, methods, or integrations, you must redeploy:

```bash
# Deploy updated API to the dev stage
aws apigateway create-deployment \
  --rest-api-id $API_ID \
  --stage-name dev \
  --description "Added POST /users endpoint"
```

---

## Summary

- API Gateway creates a managed HTTPS endpoint in front of your backend.
- Resources define URL paths; methods define HTTP verbs on those paths.
- Integrations forward requests from API Gateway to your backend (EC2, Lambda, etc.).
- Every change must be deployed to a stage before it is live.
- The Invoke URL format: `https://<api-id>.execute-api.<region>.amazonaws.com/<stage>/<resource>`
- Enable CORS to allow browser clients from different origins to call your API.
- Throttling (rate/burst limits) and usage plans protect your backend from traffic spikes.

---

## External Resources

- [API Gateway Getting Started — AWS Documentation](https://docs.aws.amazon.com/apigateway/latest/developerguide/getting-started.html)
- [API Gateway Stage Variables](https://docs.aws.amazon.com/apigateway/latest/developerguide/aws-api-gateway-stage-variables-reference.html)
- [Throttling Settings in API Gateway](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-request-throttling.html)
