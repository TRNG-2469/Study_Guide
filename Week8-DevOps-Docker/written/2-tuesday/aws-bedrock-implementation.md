# AWS Bedrock Implementation in Java

## Learning Objectives

By the end of this lesson, you will be able to:

- Add the correct AWS SDK for Java v2 dependencies to a Maven project
- Use the `BedrockRuntimeClient` to invoke foundation models
- Construct and send requests using the `InvokeModel` and `Converse` APIs
- Parse JSON responses from Claude and other models
- Implement error handling and understand retry strategies
- Describe cost considerations for Bedrock API usage

---

## Why This Matters

Knowing that Bedrock exists is not enough — you need to be able to write production-quality Java code that calls it. This lesson translates concepts into working code. The patterns you learn here apply to any foundation model in Bedrock, and the same AWS SDK patterns apply to other AWS services you will use throughout your career.

---

## Maven Dependencies

Before writing any code, you need the correct dependencies in your `pom.xml`. AWS SDK for Java v2 uses a bill-of-materials (BOM) to manage version compatibility across modules.

```xml
<dependencyManagement>
  <dependencies>
    <!-- BOM: manages versions of all AWS SDK v2 modules consistently -->
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>bom</artifactId>
      <version>2.25.60</version>  <!-- Use the latest stable release -->
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- Core Bedrock Runtime client for invoking models -->
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>bedrockruntime</artifactId>
    <!-- Version managed by BOM above; do not specify here -->
  </dependency>

  <!-- Jackson for JSON serialization/deserialization -->
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.0</version>
  </dependency>

  <!-- SLF4J for logging (good practice in any production code) -->
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.12</version>
  </dependency>
</dependencies>
```

**Important:** The `bedrockruntime` module is what you use for inference (calling models). There is a separate `bedrock` module for management operations (listing models, managing fine-tuning jobs). Do not confuse them.

---

## Authentication and Client Setup

AWS SDK for Java v2 uses the **default credential provider chain**. When your code runs, the SDK searches for credentials in this order:

1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. Java system properties
3. AWS credentials file (`~/.aws/credentials`)
4. IAM role attached to the compute resource (EC2 instance role, ECS task role, Lambda execution role)

In production on AWS, option 4 (IAM roles) is always preferred — no credentials are stored in code or environment variables. For local development, option 3 (credentials file configured via `aws configure`) is standard.

```java
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

public class BedrockClientFactory {

    /**
     * Creates a BedrockRuntimeClient configured for a specific AWS region.
     * The client is thread-safe and expensive to create — create it once
     * and reuse it throughout the application lifecycle.
     *
     * @param region The AWS region where Bedrock is available (e.g., us-east-1)
     * @return A configured BedrockRuntimeClient
     */
    public static BedrockRuntimeClient createClient(Region region) {
        return BedrockRuntimeClient.builder()
                // DefaultCredentialsProvider searches for credentials automatically
                // (env vars → properties → ~/.aws/credentials → IAM role)
                .credentialsProvider(DefaultCredentialsProvider.create())
                // Bedrock is not available in all regions; us-east-1 and us-west-2
                // have the broadest model availability as of 2024
                .region(region)
                .build();
    }
}
```

---

## API 1: InvokeModel

The `InvokeModel` API sends a raw JSON request body to a specific model and returns a raw JSON response body. Every model family has a different request/response JSON schema, so you must construct the JSON according to that model's specification.

### Calling Claude via InvokeModel

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.io.IOException;

public class ClaudeInvokeModelExample {

    // ObjectMapper is thread-safe after configuration; reuse it
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Sends a single-turn prompt to Claude and returns the model's text response.
     *
     * @param client     The initialized BedrockRuntimeClient
     * @param modelId    The Bedrock model ID, e.g. "anthropic.claude-3-haiku-20240307-v1:0"
     * @param userPrompt The user's question or instruction
     * @return The model's text response
     */
    public static String invokeClaudeModel(
            BedrockRuntimeClient client,
            String modelId,
            String userPrompt) throws IOException {

        // Build the request body according to Anthropic's Messages API format.
        // Claude uses the "messages" array format with "role" and "content" fields.
        ObjectNode requestBody = MAPPER.createObjectNode();

        // anthropic_version is required for Claude models in Bedrock
        requestBody.put("anthropic_version", "bedrock-2023-05-31");

        // max_tokens limits the length of Claude's response
        // Set this based on the expected response length for your use case
        requestBody.put("max_tokens", 1024);

        // The messages array holds the conversation turns
        // Each message has a "role" ("user" or "assistant") and "content" (the text)
        var messagesArray = MAPPER.createArrayNode();
        var userMessage = MAPPER.createObjectNode();
        userMessage.put("role", "user");

        var contentArray = MAPPER.createArrayNode();
        var textContent = MAPPER.createObjectNode();
        textContent.put("type", "text");   // Claude supports text and image content types
        textContent.put("text", userPrompt); // The actual text of the user's message
        contentArray.add(textContent);

        userMessage.set("content", contentArray);
        messagesArray.add(userMessage);
        requestBody.set("messages", messagesArray);

        // Optional: Add a system prompt to set Claude's behavior
        // requestBody.put("system", "You are a helpful assistant that responds concisely.");

        // Optional: Adjust temperature (0.0 to 1.0); default is 1.0 for Claude
        // requestBody.put("temperature", 0.5);

        // Convert the request body ObjectNode to a JSON string, then to bytes
        byte[] requestBodyBytes = MAPPER.writeValueAsBytes(requestBody);

        // Build the InvokeModelRequest
        InvokeModelRequest request = InvokeModelRequest.builder()
                // The model ID specifies exactly which model to invoke
                .modelId(modelId)
                // contentType tells Bedrock the format of our request body
                .contentType("application/json")
                // accept tells Bedrock the format we want in the response
                .accept("application/json")
                // SdkBytes wraps raw bytes for the SDK to transmit
                .body(SdkBytes.fromByteArray(requestBodyBytes))
                .build();

        // Make the synchronous API call — this blocks until the model responds
        InvokeModelResponse response = client.invokeModel(request);

        // The response body is also raw JSON bytes
        // Convert bytes to a string, then parse as JSON
        String responseJson = response.body().asUtf8String();
        JsonNode responseNode = MAPPER.readTree(responseJson);

        // Navigate the Claude response structure:
        // { "content": [ { "type": "text", "text": "..." } ], "usage": {...}, ... }
        // The "content" array holds the model's response; index [0] is the first block
        return responseNode
                .path("content")   // Get the "content" array
                .path(0)           // Get the first element
                .path("text")      // Get its "text" field
                .asText();         // Convert the JsonNode to a plain Java String
    }
}
```

### Example Usage

```java
public class Main {
    public static void main(String[] args) throws Exception {
        // Create the client once and reuse it
        BedrockRuntimeClient client = BedrockClientFactory.createClient(
                software.amazon.awssdk.regions.Region.US_EAST_1
        );

        // Claude 3 Haiku is fast and cost-effective for development/testing
        String modelId = "anthropic.claude-3-haiku-20240307-v1:0";

        String answer = ClaudeInvokeModelExample.invokeClaudeModel(
                client,
                modelId,
                "Explain what a context window is in a foundation model, in two sentences."
        );

        System.out.println("Claude says: " + answer);

        // Always close the client when done to release resources
        client.close();
    }
}
```

---

## API 2: Converse

The `Converse` API is a newer, higher-level API introduced by AWS to provide a **unified request/response format** across all Bedrock models. Instead of constructing model-specific JSON, you use typed Java objects. The SDK handles the model-specific translation internally.

This API is strongly recommended for new applications because:
- It is model-agnostic (switching models requires only changing the model ID)
- It handles multi-turn conversation natively
- It is type-safe (compiler catches errors instead of runtime JSON errors)

```java
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

public class ConverseApiExample {

    /**
     * Uses the Converse API to send a message and receive a response.
     * The Converse API provides a unified interface that works across model families.
     *
     * @param client     The initialized BedrockRuntimeClient
     * @param modelId    The Bedrock model ID
     * @param userPrompt The user's question or instruction
     * @return The model's text response
     */
    public static String converseWithModel(
            BedrockRuntimeClient client,
            String modelId,
            String userPrompt) {

        // Build the user's message using typed SDK objects
        // ContentBlock.fromText() wraps plain text in the appropriate structure
        Message userMessage = Message.builder()
                .role(ConversationRole.USER)  // Enum: USER or ASSISTANT
                .content(ContentBlock.fromText(userPrompt))  // The actual message text
                .build();

        // Build inference configuration (optional but recommended)
        InferenceConfiguration inferenceConfig = InferenceConfiguration.builder()
                .maxTokens(1024)   // Maximum length of the response
                .temperature(0.5F) // 0.0 (deterministic) to 1.0 (creative)
                .topP(0.9F)        // Nucleus sampling threshold
                .build();

        // Build the ConverseRequest with the model ID, messages, and configuration
        ConverseRequest converseRequest = ConverseRequest.builder()
                .modelId(modelId)
                .messages(List.of(userMessage)) // Pass a list of conversation turns
                .inferenceConfig(inferenceConfig)
                // Optional: Set a system prompt to configure the model's behavior
                .system(SystemContentBlock.fromText(
                        "You are a helpful assistant for software developers. " +
                        "Be concise and accurate."
                ))
                .build();

        // Make the API call
        ConverseResponse response = client.converse(converseRequest);

        // Extract the text response using the typed response structure
        // response.output().message() is the assistant's Message object
        // .content() returns the list of ContentBlocks
        // .get(0).text() gets the text from the first content block
        return response.output()
                .message()
                .content()
                .get(0)
                .text();
    }
}
```

### Multi-Turn Conversation with Converse

```java
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MultiTurnChatExample {

    /**
     * Demonstrates a simple interactive chat loop that maintains conversation history.
     * Each turn sends the full conversation history so the model has context.
     */
    public static void runChat(BedrockRuntimeClient client, String modelId) {
        // conversationHistory grows with each turn; both user and assistant messages are added
        List<Message> conversationHistory = new ArrayList<>();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Chat started. Type 'quit' to exit.\n");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("quit")) {
                System.out.println("Chat ended.");
                break;
            }

            // Add the user's message to history
            conversationHistory.add(
                    Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(userInput))
                            .build()
            );

            // Send the full conversation history with every request
            // This is how the model "remembers" previous turns
            ConverseRequest request = ConverseRequest.builder()
                    .modelId(modelId)
                    .messages(conversationHistory) // All previous turns included
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(1024)
                            .build())
                    .build();

            ConverseResponse response = client.converse(request);
            Message assistantMessage = response.output().message();
            String assistantText = assistantMessage.content().get(0).text();

            // Add the assistant's response to history for the next turn
            conversationHistory.add(assistantMessage);

            System.out.println("Assistant: " + assistantText);
            System.out.println();
        }
    }
}
```

---

## Error Handling and Retry Strategies

Production code must handle failures gracefully. Bedrock API calls can fail for several reasons:

```java
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;
import software.amazon.awssdk.core.exception.SdkClientException;

public class ResilientBedrockCaller {

    private static final int MAX_RETRIES = 3;
    // Initial delay in milliseconds; doubles with each retry (exponential backoff)
    private static final long INITIAL_BACKOFF_MS = 500;

    public static String invokeWithRetry(
            BedrockRuntimeClient client,
            ConverseRequest request) throws InterruptedException {

        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                ConverseResponse response = client.converse(request);
                return response.output().message().content().get(0).text();

            } catch (ThrottlingException e) {
                // ThrottlingException: You have exceeded your requests-per-minute quota.
                // This is a transient error — wait and retry.
                System.err.printf("Request throttled (attempt %d/%d). " +
                        "Retrying in %dms...%n", attempt, MAX_RETRIES, backoffMs);
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2; // Exponential backoff: 500ms → 1000ms → 2000ms
                } else {
                    throw new RuntimeException("Max retries exceeded due to throttling", e);
                }

            } catch (ModelNotReadyException e) {
                // ModelNotReadyException: The model is loading (common after a long idle period).
                // Also transient — retry after a brief wait.
                System.err.println("Model not ready, retrying...");
                Thread.sleep(backoffMs);
                backoffMs *= 2;

            } catch (ModelErrorException e) {
                // ModelErrorException: The model itself encountered an internal error.
                // May be transient; worth retrying once or twice.
                System.err.println("Model error: " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(backoffMs);
                } else {
                    throw new RuntimeException("Model error persisted", e);
                }

            } catch (ValidationException e) {
                // ValidationException: Your request was malformed (bad parameters, invalid model ID).
                // This is NOT transient — retrying will not help. Fix the request.
                throw new IllegalArgumentException(
                        "Invalid request parameters: " + e.getMessage(), e);

            } catch (AccessDeniedException e) {
                // AccessDeniedException: Your IAM role lacks permission to invoke this model,
                // OR you have not requested access to this model in the Bedrock console.
                // NOT transient — fix permissions or request model access.
                throw new SecurityException(
                        "Access denied. Check IAM permissions and Bedrock model access: "
                                + e.getMessage(), e);

            } catch (SdkClientException e) {
                // SdkClientException: Network error, timeout, etc. on the client side.
                System.err.println("Network error: " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } else {
                    throw new RuntimeException("Network error persisted", e);
                }
            }
        }

        throw new RuntimeException("All retry attempts exhausted");
    }
}
```

### AWS SDK Built-In Retry Configuration

The AWS SDK for Java v2 has built-in retry logic. You can configure it on the client:

```java
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;

BedrockRuntimeClient client = BedrockRuntimeClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.US_EAST_1)
        .overrideConfiguration(config -> config
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(3)  // Retry up to 3 times automatically
                        .build()))
        .build();
```

For most cases, the SDK's default retry policy (which handles throttling automatically) is sufficient.

---

## Cost Considerations

Every token processed costs money. Here are practical guidelines:

### Track Token Usage

```java
// The Converse API returns token usage in the response metadata
ConverseResponse response = client.converse(request);

// Usage contains inputTokens (what you sent) and outputTokens (what was generated)
TokenUsage usage = response.usage();
System.out.printf("Input tokens: %d | Output tokens: %d | Total: %d%n",
        usage.inputTokens(),
        usage.outputTokens(),
        usage.totalTokens());
```

### Cost Reduction Strategies

1. **Choose the right model tier:** Claude 3 Haiku costs roughly 60x less per token than Claude 3 Opus. Use Haiku for simpler tasks and Opus only when maximum capability is genuinely required.

2. **Minimize prompt length:** Every token in your system prompt and conversation history costs money on every call. Keep system prompts concise.

3. **Set appropriate `maxTokens`:** Do not set this to an arbitrarily large value. If responses are typically 200 tokens, setting maxTokens to 4096 does not cost you extra (you pay for output tokens actually generated), but setting it correctly avoids rare runaway responses.

4. **Cache repeated context:** If many requests share the same long system prompt or document context, explore prompt caching (available for Claude models in Bedrock) — repeated input tokens in a cached prefix are charged at a reduced rate.

5. **Set up AWS Cost Alerts:** In the AWS Billing console, create a budget alert for Bedrock spending. During development, even a small runaway script can generate unexpected charges.

---

## Complete Working Example

```java
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

/**
 * A complete, minimal example of calling AWS Bedrock from Java.
 * Prerequisites:
 *   1. AWS credentials configured (aws configure or IAM role)
 *   2. Model access granted in Bedrock console (us-east-1 region)
 *   3. Maven dependencies added (see top of lesson)
 */
public class BedrockQuickstart {

    public static void main(String[] args) {
        // 1. Create the client (create once, reuse for all calls)
        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();

        // 2. Define which model to use
        // Claude 3 Haiku: fast, cheap, great for development and simple tasks
        String modelId = "anthropic.claude-3-haiku-20240307-v1:0";

        // 3. Build the request
        ConverseRequest request = ConverseRequest.builder()
                .modelId(modelId)
                .messages(List.of(
                        Message.builder()
                                .role(ConversationRole.USER)
                                .content(ContentBlock.fromText(
                                        "In one paragraph, explain why DevOps matters " +
                                        "for software delivery teams."
                                ))
                                .build()
                ))
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(512)
                        .temperature(0.5F)
                        .build())
                .build();

        // 4. Invoke the model and print the response
        ConverseResponse response = client.converse(request);
        String answer = response.output().message().content().get(0).text();

        System.out.println("Response:");
        System.out.println(answer);
        System.out.printf("%nTokens used — Input: %d | Output: %d%n",
                response.usage().inputTokens(),
                response.usage().outputTokens());

        // 5. Close the client to release resources
        client.close();
    }
}
```

---

## Summary

| Topic | Key Takeaway |
|---|---|
| Maven setup | Use the AWS SDK BOM to manage version compatibility; add `bedrockruntime` module |
| Authentication | Use `DefaultCredentialsProvider`; prefer IAM roles in production |
| `InvokeModel` API | Raw JSON request/response; model-specific schema; more flexible but more complex |
| `Converse` API | Typed Java objects; model-agnostic; recommended for new applications |
| Error handling | Retry on `ThrottlingException` and `ModelNotReadyException`; do not retry `ValidationException` |
| Cost management | Track token usage; choose the right model tier; set up billing alerts |

---

## External Resources

1. **AWS SDK for Java v2 Developer Guide** — https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html
2. **AWS Bedrock Runtime API Reference** — https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html
3. **Anthropic Claude Messages API Reference** — https://docs.anthropic.com/en/api/messages
