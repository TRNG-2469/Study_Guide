package com.revature.service;

// ============================================================
// BedrockProductService.java  --  STARTER CODE
// Week 8, Tuesday Lab: AI-Powered Features & Code Quality
// ============================================================
//
// MAVEN DEPENDENCIES REQUIRED in pom.xml:
//
//   <!-- In <dependencyManagement><dependencies> -->
//   <dependency>
//       <groupId>software.amazon.awssdk</groupId>
//       <artifactId>bom</artifactId>
//       <version>2.28.17</version>
//       <type>pom</type>
//       <scope>import</scope>
//   </dependency>
//
//   <!-- In <dependencies> -->
//   <dependency>
//       <groupId>software.amazon.awssdk</groupId>
//       <artifactId>bedrockruntime</artifactId>
//   </dependency>
//   <dependency>
//       <groupId>software.amazon.awssdk</groupId>
//       <artifactId>sso</artifactId>
//   </dependency>
//   <dependency>
//       <groupId>software.amazon.awssdk</groupId>
//       <artifactId>ssooidc</artifactId>
//   </dependency>
//
// ============================================================

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;

/**
 * Service class that uses AWS Bedrock to generate AI-powered product descriptions.
 *
 * <p>This class demonstrates how to integrate Amazon Bedrock's Converse API
 * into a Java application using the AWS SDK v2.</p>
 *
 * <p>AWS credentials are loaded automatically from the environment:
 * either environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY,
 * AWS_DEFAULT_REGION) or the ~/.aws/credentials file.</p>
 */
public class BedrockProductService {

    // The model ID for Claude 3.5 Sonnet on Amazon Bedrock.
    // This must exactly match the model ID in the AWS Bedrock console.
    private static final String MODEL_ID = "anthropic.claude-3-5-sonnet-20241022-v2:0";

    // The AWS region where your Bedrock model access is enabled.
    // Change this if you enabled model access in a different region.
    private static final Region AWS_REGION = Region.US_EAST_1;

    // The BedrockRuntimeClient handles all communication with the Bedrock service.
    // It is thread-safe and should be shared / reused -- do not create a new
    // client for every request.
    private final BedrockRuntimeClient client;

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    /**
     * Creates a BedrockProductService with a default BedrockRuntimeClient.
     *
     * <p>The client uses the DefaultCredentialsProvider, which checks (in order):
     * 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     * 2. Java system properties
     * 3. ~/.aws/credentials file
     * 4. IAM role attached to the EC2/ECS/Lambda instance</p>
     */
    public BedrockProductService() {
        // TODO (Task 1.1): Initialize the BedrockRuntimeClient.
        //
        // Use the builder pattern:
        //   this.client = BedrockRuntimeClient.builder()
        //       .region(AWS_REGION)
        //       .build();
        //
        // The SDK automatically picks up credentials from environment variables
        // or ~/.aws/credentials -- no explicit credential configuration needed
        // as long as your environment is set up correctly.

        this.client = null; // Replace this line with the real implementation
    }

    // ----------------------------------------------------------------
    // Core method -- implement this in Task 1.1
    // ----------------------------------------------------------------

    /**
     * Calls AWS Bedrock (Claude 3.5 Sonnet) to generate a product description.
     *
     * @param productName the name of the product (e.g., "Wireless Headphones")
     * @param features    a comma-separated list of key features
     *                    (e.g., "noise cancellation, 30-hour battery, foldable design")
     * @return a 2-3 sentence product description suitable for an e-commerce site,
     *         or an error message if the call fails
     */
    public String generateProductDescription(String productName, String features) {
        // TODO (Task 1.1): Implement this method using the Converse API.
        //
        // STEP 1 - Build the prompt text.
        //   Craft a system-style instruction followed by the product details.
        //   Example prompt structure:
        //
        //     "You are a professional copywriter for an e-commerce platform.
        //      Write a compelling 2-3 sentence product description for the
        //      following product. Highlight key benefits. Appeal to online shoppers.
        //      Respond with ONLY the description - no labels, no preamble.
        //
        //      Product name: " + productName + "
        //      Key features: " + features
        //
        // STEP 2 - Build a Message object containing the prompt as a ContentBlock.
        //   Message userMessage = Message.builder()
        //       .role(ConversationRole.USER)
        //       .content(ContentBlock.fromText(promptText))
        //       .build();
        //
        // STEP 3 - Build a ConverseRequest.
        //   ConverseRequest request = ConverseRequest.builder()
        //       .modelId(MODEL_ID)
        //       .messages(userMessage)
        //       .build();
        //
        //   For Task 1.2, you will also set temperature via inferenceConfig:
        //   .inferenceConfig(cfg -> cfg.temperature(0.7f))
        //
        // STEP 4 - Call the Bedrock service.
        //   ConverseResponse response = client.converse(request);
        //
        // STEP 5 - Extract and return the text from the response.
        //   The response structure is:
        //   response.output().message().content().get(0).text()

        // TODO (Task 1.3): Wrap Steps 4-5 in a try-catch that handles:
        //   - software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException
        //   - software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException
        //   - software.amazon.awssdk.services.bedrockruntime.model.ValidationException
        //   - BedrockRuntimeException (catch-all for other Bedrock errors)
        // Return a user-friendly error message string for each case.

        return "TODO: implement generateProductDescription";
    }

    // ----------------------------------------------------------------
    // Shutdown
    // ----------------------------------------------------------------

    /**
     * Closes the Bedrock client and releases underlying HTTP resources.
     * Call this when the service is no longer needed (e.g., application shutdown).
     */
    public void shutdown() {
        // TODO: Call client.close() here.
        // In a Spring Boot app this would be annotated with @PreDestroy.
        if (client != null) {
            client.close();
        }
    }

    // ----------------------------------------------------------------
    // Main method -- use this to test your implementation
    // ----------------------------------------------------------------

    /**
     * Quick smoke-test. Run this class directly to verify your Bedrock
     * integration works before wiring it into the Spring Boot app.
     *
     * Expected output: three non-empty product descriptions printed to console.
     */
    public static void main(String[] args) {

        BedrockProductService service = new BedrockProductService();

        // Sample products to test with
        String[][] products = {
            {
                "Wireless Noise-Cancelling Headphones",
                "40-hour battery life, active noise cancellation, foldable design, USB-C charging"
            },
            {
                "Stainless Steel Water Bottle",
                "32 oz capacity, double-wall vacuum insulation, keeps drinks cold 24 hours, BPA-free, leak-proof lid"
            },
            {
                "Ergonomic Mesh Office Chair",
                "lumbar support, adjustable armrests, breathable mesh back, 360-degree swivel, weight capacity 300 lbs"
            }
        };

        System.out.println("=== AWS Bedrock Product Description Generator ===");
        System.out.println();

        for (String[] product : products) {
            String productName = product[0];
            String features    = product[1];

            System.out.println("Product: " + productName);
            System.out.println("Features: " + features);
            System.out.println("Generated description:");

            String description = service.generateProductDescription(productName, features);
            System.out.println(description);
            System.out.println("---");
        }

        service.shutdown();
        System.out.println("Done.");
    }
}
