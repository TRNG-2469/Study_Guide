package com.revature.service;

// ============================================================
// BedrockProductService-solution.java  --  SOLUTION (DO NOT SHARE WITH TRAINEES)
// Week 8, Tuesday Lab: AI-Powered Features & Code Quality
// ============================================================
//
// TRAINER NOTES:
//   - Temperature is exposed as a constructor parameter (default 0.7f)
//   - Full error handling with specific AWS exception types
//   - Observation table answers documented in class-level Javadoc
//   - main() produces real output for all 3 sample products
// ============================================================

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

/**
 * SOLUTION: Service class that uses AWS Bedrock (Claude 3.5 Sonnet) to generate
 * AI-powered product descriptions via the Converse API.
 *
 * <h2>Temperature Observation Guide (Task 1.2)</h2>
 *
 * <p>When trainees run the same prompt at different temperatures they should observe:</p>
 *
 * <ul>
 *   <li><strong>temperature = 0.0f</strong> - Outputs are virtually identical across all 3 runs.
 *       The model selects the maximum-probability token at every step, producing a single
 *       "best" description. Minor whitespace differences may appear but word choice will
 *       be the same. Good for: legal copy, disclaimers, anything requiring exact reproducibility.</li>
 *
 *   <li><strong>temperature = 0.7f</strong> - Outputs vary noticeably between runs in word choice
 *       and sentence structure, but all remain coherent and on-topic. This is the Goldilocks zone:
 *       creative enough to feel natural, constrained enough to stay professional.
 *       Good for: product descriptions, customer-facing copy, most production AI features.</li>
 *
 *   <li><strong>temperature = 1.0f</strong> - Outputs vary significantly. Occasional unusual word
 *       choices, metaphors, or structural surprises. Most outputs are still usable but some may
 *       feel off-brand or overly elaborate. Good for: brainstorming, creative writing tools,
 *       generating diverse options for A/B testing.</li>
 * </ul>
 *
 * <h3>Production recommendation answer</h3>
 * <p>For a production e-commerce feature, <strong>0.7f</strong> is the right choice.
 * It produces descriptions that feel authentically written (not robotic) while
 * staying coherent and brand-appropriate. Temperature 0.0 produces copy that sounds
 * repetitive if many products have similar features; temperature 1.0 occasionally
 * produces descriptions that are creative but inconsistent with brand voice.</p>
 */
public class BedrockProductService {

    private static final String MODEL_ID = "anthropic.claude-3-5-sonnet-20241022-v2:0";
    private static final Region AWS_REGION = Region.US_EAST_1;

    /** Default temperature - balanced creativity/consistency for production use. */
    private static final float DEFAULT_TEMPERATURE = 0.7f;

    private final BedrockRuntimeClient client;
    private final float temperature;

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------

    /**
     * Creates the service with the default temperature (0.7f).
     */
    public BedrockProductService() {
        this(DEFAULT_TEMPERATURE);
    }

    /**
     * Creates the service with a custom temperature value.
     *
     * @param temperature value between 0.0 (deterministic) and 1.0 (creative)
     */
    public BedrockProductService(float temperature) {
        if (temperature < 0.0f || temperature > 1.0f) {
            throw new IllegalArgumentException(
                "Temperature must be between 0.0 and 1.0, got: " + temperature);
        }
        this.temperature = temperature;
        this.client = BedrockRuntimeClient.builder()
                .region(AWS_REGION)
                .build();
    }

    // ----------------------------------------------------------------
    // Core method
    // ----------------------------------------------------------------

    /**
     * Calls AWS Bedrock to generate a 2-3 sentence e-commerce product description.
     *
     * @param productName the name of the product
     * @param features    comma-separated list of key product features
     * @return the generated description, or a user-friendly error message on failure
     */
    public String generateProductDescription(String productName, String features) {

        // STEP 1 - Build the prompt
        String promptText = "You are a professional copywriter for an e-commerce platform.\n"
                + "Write a compelling 2-3 sentence product description for the following product.\n"
                + "The description should highlight key benefits and appeal to online shoppers.\n"
                + "Respond with ONLY the product description. Do not include labels or preamble.\n\n"
                + "Product name: " + productName + "\n"
                + "Key features: " + features;

        // STEP 2 - Build the Message (user turn)
        Message userMessage = Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(promptText))
                .build();

        // STEP 3 - Build the ConverseRequest with inference config for temperature
        InferenceConfiguration inferenceConfig = InferenceConfiguration.builder()
                .temperature(temperature)
                .build();

        ConverseRequest request = ConverseRequest.builder()
                .modelId(MODEL_ID)
                .messages(userMessage)
                .inferenceConfig(inferenceConfig)
                .build();

        // STEPS 4 & 5 - Call Bedrock and extract the response text
        try {
            ConverseResponse response = client.converse(request);
            return response.output().message().content().get(0).text();

        } catch (AccessDeniedException e) {
            // Model not enabled in this region, or IAM permissions missing
            System.err.println("[BedrockProductService] AccessDeniedException: " + e.getMessage());
            return "AI description unavailable: model access not configured. "
                    + "Contact your administrator.";

        } catch (ThrottlingException e) {
            // Too many requests - back off and retry
            System.err.println("[BedrockProductService] ThrottlingException: " + e.getMessage());
            return "AI description temporarily unavailable due to high demand. "
                    + "Please try again in a few seconds.";

        } catch (ValidationException e) {
            // Bad request - malformed model ID, empty prompt, etc.
            System.err.println("[BedrockProductService] ValidationException: " + e.getMessage());
            return "Could not generate description: invalid request parameters.";

        } catch (BedrockRuntimeException e) {
            // Catch-all for other Bedrock errors
            System.err.println("[BedrockProductService] BedrockRuntimeException: " + e.getMessage());
            return "AI description service error: " + e.getMessage();
        }
    }

    // ----------------------------------------------------------------
    // Shutdown
    // ----------------------------------------------------------------

    /**
     * Closes the Bedrock client. In Spring Boot, annotate with @PreDestroy.
     */
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    // ----------------------------------------------------------------
    // Main method - produces output for all 3 sample products
    // ----------------------------------------------------------------

    /**
     * Smoke-test main. Runs all 3 sample products at temperature 0.7f (default),
     * then demonstrates the temperature range by running the first product at
     * 0.0f and 1.0f for comparison.
     *
     * <p>Sample output (actual AI output will vary):</p>
     * <pre>
     * === AWS Bedrock Product Description Generator ===
     *
     * [temp=0.70] Product: Wireless Noise-Cancelling Headphones
     * Generated: Experience crystal-clear audio with our Wireless Noise-Cancelling
     *            Headphones, featuring 40 hours of battery life and advanced ANC
     *            technology. Fold them flat for easy travel with USB-C charging
     *            ready when you are.
     * ---
     * [temp=0.70] Product: Stainless Steel Water Bottle
     * Generated: Stay hydrated in style with our 32 oz Stainless Steel Water Bottle,
     *            engineered with double-wall vacuum insulation to keep drinks ice-cold
     *            for 24 hours. The BPA-free, leak-proof lid means you can toss it in
     *            your bag with confidence.
     * ---
     * [temp=0.70] Product: Ergonomic Mesh Office Chair
     * Generated: Upgrade your workspace with our Ergonomic Mesh Office Chair,
     *            designed to support your body through long work sessions with
     *            adjustable lumbar support and breathable mesh back. A full 360-degree
     *            swivel and weight capacity up to 300 lbs make it a durable, flexible
     *            choice for any desk setup.
     * ---
     *
     * === Temperature Comparison for: Wireless Noise-Cancelling Headphones ===
     *
     * [temp=0.00] Run 1: Experience crystal-clear audio with our Wireless Noise-Cancelli...
     * [temp=0.00] Run 2: Experience crystal-clear audio with our Wireless Noise-Cancelli...
     * [temp=0.00] Run 3: Experience crystal-clear audio with our Wireless Noise-Cancelli...
     *   -> Observation: Nearly identical outputs - deterministic.
     *
     * [temp=0.70] Run 1: Immerse yourself in flawless sound with our Wireless Noise-Canc...
     * [temp=0.70] Run 2: Experience crystal-clear audio with our Wireless Noise-Cancelli...
     * [temp=0.70] Run 3: Enjoy premium audio quality with our advanced Wireless Headphon...
     *   -> Observation: Varied word choice, all coherent and on-brand.
     *
     * [temp=1.00] Run 1: Dive into a world of pure, uninterrupted audio bliss with our h...
     * [temp=1.00] Run 2: Transform your listening experience with our premium Wireless No...
     * [temp=1.00] Run 3: Say goodbye to distracting background noise and hello to your pe...
     *   -> Observation: Creative and varied, occasionally more elaborate or dramatic.
     * </pre>
     */
    public static void main(String[] args) {

        // ---- Part 1: All 3 products at default temperature ----
        BedrockProductService service = new BedrockProductService();

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
            String name = product[0];
            String features = product[1];
            System.out.printf("[temp=%.2f] Product: %s%n", service.temperature, name);
            System.out.println("Generated: " + service.generateProductDescription(name, features));
            System.out.println("---");
        }

        service.shutdown();

        // ---- Part 2: Temperature comparison ----
        System.out.println();
        System.out.println("=== Temperature Comparison for: Wireless Noise-Cancelling Headphones ===");
        System.out.println();

        String testProduct = "Wireless Noise-Cancelling Headphones";
        String testFeatures = "40-hour battery life, active noise cancellation, foldable design, USB-C charging";
        float[] temperatures = {0.0f, 0.7f, 1.0f};

        for (float temp : temperatures) {
            BedrockProductService tempService = new BedrockProductService(temp);
            for (int run = 1; run <= 3; run++) {
                String output = tempService.generateProductDescription(testProduct, testFeatures);
                String preview = output.length() > 60 ? output.substring(0, 60) + "..." : output;
                System.out.printf("[temp=%.2f] Run %d: %s%n", temp, run, preview);
            }
            String obs = temp == 0.0f
                    ? "Nearly identical outputs - deterministic."
                    : temp == 0.7f
                    ? "Varied word choice, all coherent and on-brand."
                    : "Creative and varied, occasionally more elaborate or dramatic.";
            System.out.println("  -> Observation: " + obs);
            System.out.println();
            tempService.shutdown();
        }

        System.out.println("Done.");
    }
}
