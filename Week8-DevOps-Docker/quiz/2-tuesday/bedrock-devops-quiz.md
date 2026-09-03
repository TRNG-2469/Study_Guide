# Weekly Knowledge Check: Week 8 — AWS Bedrock & DevOps/CI/CD
**Day:** Tuesday | **Topics:** AWS Bedrock · Foundation Models · Model Customization · DevOps · Agile · CI · CD · Continuous Deployment · SonarCloud/SonarLint

---

## Part 1: Multiple Choice

**Q1.** What does the `temperature` parameter control in a foundation model inference request?

A. The number of tokens the model can generate  
B. The randomness/creativity of the model's output  
C. The number of candidate responses returned  
D. The latency of the model API call  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** `temperature` controls output randomness — a value near 0 makes responses deterministic and focused, while values near 1 make them more creative and varied.  
- **Why A is wrong:** Token limits are controlled by `maxTokens` (or `max_tokens`), not temperature.  
- **Why C is wrong:** The number of candidate responses is controlled by `n` or `topK` in some APIs, not temperature.  
- **Why D is wrong:** Temperature has no effect on API latency; it only shapes the probability distribution over next tokens.  
</details>

---

**Q2.** Which DORA metric measures the average time it takes to restore service after a production incident?

A. Deployment Frequency  
B. Lead Time for Changes  
C. Change Failure Rate  
D. Mean Time to Recovery (MTTR)  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** D  
**Explanation:** MTTR (Mean Time to Recovery) measures how quickly a team restores service after a production failure — a key indicator of operational resilience.  
- **Why A is wrong:** Deployment Frequency measures how often code is deployed to production.  
- **Why B is wrong:** Lead Time for Changes measures the time from code commit to production deployment.  
- **Why C is wrong:** Change Failure Rate measures the percentage of deployments that cause a production failure.  
</details>

---

**Q3.** What is the key difference between Continuous Delivery and Continuous Deployment?

A. Continuous Delivery requires automated tests; Continuous Deployment does not  
B. Continuous Delivery deploys to production automatically; Continuous Deployment requires manual approval  
C. Continuous Delivery keeps software in a deployable state with a manual production gate; Continuous Deployment automatically releases every passing build to production  
D. Continuous Deployment only applies to cloud environments; Continuous Delivery is environment-agnostic  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** Continuous Delivery ensures the codebase is always in a releasable state but retains a manual approval step before production. Continuous Deployment removes that gate and ships every green build automatically.  
- **Why A is wrong:** Both practices require automated tests; that is not the distinguishing factor.  
- **Why B is wrong:** This reverses the definitions — Continuous Deployment is the fully automated one.  
- **Why D is wrong:** Both practices apply to any environment (on-prem or cloud).  
</details>

---

**Q4.** When is Retrieval-Augmented Generation (RAG) preferred over fine-tuning a foundation model?

A. When you need the model to learn a completely new language style  
B. When you want to reduce the model's token output length  
C. When you need to ground responses in frequently updated or proprietary documents without retraining  
D. When you need to improve the model's mathematical reasoning  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** RAG retrieves relevant context from an external knowledge base at inference time, making it ideal for dynamic or proprietary data that changes often — no retraining required.  
- **Why A is wrong:** Teaching a new style (tone, format) is better handled by fine-tuning on stylistic examples.  
- **Why B is wrong:** Token output length is controlled by `maxTokens`, not the customization method.  
- **Why D is wrong:** Improving core reasoning capabilities typically requires fine-tuning or using a model trained on math datasets.  
</details>

---

**Q5.** Which of the following best describes DORA's four key metrics collectively?

A. Code coverage, defect rate, sprint velocity, and deployment window  
B. Deployment Frequency, Lead Time for Changes, Change Failure Rate, and Mean Time to Recovery  
C. Uptime, throughput, error budget, and release cadence  
D. Pull request count, review turnaround, build duration, and rollback rate  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** DORA's research identified exactly these four metrics as the strongest predictors of software delivery performance and organizational outcomes.  
- **Why A is wrong:** Code coverage and sprint velocity are useful engineering metrics but are not the DORA four.  
- **Why C is wrong:** Uptime and error budgets belong to SRE/SLO frameworks, not the DORA model.  
- **Why D is wrong:** PR count and review turnaround are team health proxies, not the DORA metrics.  
</details>

---

**Q6.** What happens when a SonarCloud quality gate fails in a GitLab CI/CD pipeline (assuming proper integration)?

A. The pipeline continues but sends an email alert  
B. SonarCloud automatically reverts the last commit  
C. The pipeline stage fails, blocking the merge or deployment  
D. SonarCloud opens a JIRA ticket automatically  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** When a quality gate fails, SonarCloud returns a non-zero exit code to the CI runner, causing the pipeline stage to fail and preventing a merge or downstream deployment.  
- **Why A is wrong:** The pipeline does not continue — the failing stage blocks downstream jobs.  
- **Why B is wrong:** SonarCloud is a static analysis tool; it has no ability to perform git operations like reverting commits.  
- **Why D is wrong:** Automatic JIRA ticket creation is not a built-in SonarCloud behavior; it requires a separate integration.  
</details>

---

**Q7.** In the context of AWS Bedrock, which statement about Foundation Models (FMs) is correct?

A. Foundation models are trained from scratch by each AWS customer on their own data  
B. Foundation models are pre-trained large models provided by third-party providers accessible via Bedrock's unified API  
C. Foundation models can only generate text and cannot handle image inputs  
D. AWS Bedrock only provides Amazon's own Titan models  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** AWS Bedrock offers foundation models from providers such as Anthropic, Meta, Cohere, Stability AI, and Amazon Titan through a single managed API — no training infrastructure needed.  
- **Why A is wrong:** Customers do not train FMs from scratch through Bedrock; they access pre-trained models.  
- **Why C is wrong:** Several Bedrock FMs (e.g., Claude 3, Stable Diffusion) support multimodal inputs including images.  
- **Why D is wrong:** Bedrock hosts models from multiple providers, not only Amazon Titan.  
</details>

---

**Q8.** Which trunk-based development principle most directly supports Continuous Integration?

A. Using long-lived feature branches to isolate work  
B. Committing small, frequent changes directly to the main branch (trunk)  
C. Running integration tests only before a quarterly release  
D. Requiring manual code freeze periods before each merge  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** Trunk-based development's core rule — small, frequent commits to a single main branch — ensures the build is always integration-tested and conflicts are caught immediately, which is exactly what CI requires.  
- **Why A is wrong:** Long-lived branches delay integration and create merge conflicts, which is the opposite of CI goals.  
- **Why C is wrong:** Running tests only quarterly defeats the purpose of continuous integration.  
- **Why D is wrong:** Manual code freezes introduce process gates that slow delivery and contradict CI principles.  
</details>

---

**Q9.** Which of the following model customization approaches modifies the actual weights of a foundation model?

A. Prompt Engineering  
B. Retrieval-Augmented Generation (RAG)  
C. Fine-Tuning  
D. System Prompt Injection  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** Fine-tuning updates the model's internal parameters (weights) by training on a domain-specific dataset, permanently adjusting its behavior for that domain.  
- **Why A is wrong:** Prompt engineering shapes responses through input crafting without touching model weights.  
- **Why B is wrong:** RAG retrieves external context at inference time; no weights are changed.  
- **Why D is wrong:** System prompt injection is a form of prompt engineering — it influences behavior via input, not weight updates.  
</details>

---

**Q10.** How does DevOps relate to Agile methodology?

A. DevOps replaces Agile — teams must choose one or the other  
B. DevOps extends Agile principles beyond development into operations and delivery, bridging the Dev-Ops gap  
C. Agile is a subset of DevOps focusing only on sprint planning  
D. DevOps requires the Waterfall model for release management  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** DevOps takes Agile's iterative, collaborative principles and extends them to include operations, infrastructure, and delivery — enabling end-to-end fast, reliable software delivery.  
- **Why A is wrong:** DevOps and Agile are complementary and are frequently used together.  
- **Why C is wrong:** Agile is a complete software development philosophy, not a subset of DevOps.  
- **Why D is wrong:** DevOps is philosophically opposed to Waterfall's sequential, batch-release model.  
</details>

---

**Q11.** In the AWS Bedrock Java SDK v2 Converse API, which class is used to build the synchronous runtime client?

A. `AmazonBedrockClient.builder()`  
B. `BedrockClient.create()`  
C. `BedrockRuntimeClient.builder()`  
D. `BedrockRuntimeAsyncClient.standard()`  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C  
**Explanation:** `BedrockRuntimeClient.builder()` is the correct SDK v2 builder for the synchronous Bedrock Runtime client used to call the Converse API.  
- **Why A is wrong:** `AmazonBedrockClient` is the SDK v1 (AWS SDK for Java 1.x) naming convention.  
- **Why B is wrong:** `BedrockClient` targets the Bedrock control-plane (model management), not inference calls.  
- **Why D is wrong:** `BedrockRuntimeAsyncClient` is the async variant; `.standard()` is not a valid SDK v2 builder method.  
</details>

---

**Q12.** What does SonarLint primarily provide that SonarCloud does not?

A. Repository-wide aggregate code quality reports  
B. Real-time, in-IDE code analysis and fix suggestions as you type  
C. Automated pull request decoration in GitHub and GitLab  
D. Quality gate enforcement in CI pipelines  

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B  
**Explanation:** SonarLint is an IDE plugin that gives developers instant feedback on code issues as they write, acting like a spell-checker for code quality — before a commit ever happens.  
- **Why A is wrong:** Aggregate project-wide reports are a SonarCloud (server-side) feature.  
- **Why C is wrong:** PR decoration (annotations on diffs) is a SonarCloud feature tied to CI integration.  
- **Why D is wrong:** Quality gate enforcement in pipelines is exclusively a SonarCloud server-side capability.  
</details>

---

## Part 2: True/False

**Q13.** SonarLint runs analysis inside the developer's IDE, while SonarCloud runs analysis as part of a CI pipeline.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** TRUE  
**Explanation:** SonarLint is an IDE plugin (VS Code, IntelliJ, Eclipse) providing real-time local analysis. SonarCloud is a cloud-hosted service triggered by CI events (push, pull request) to analyze the full codebase.  
</details>

---

**Q14.** In Continuous Integration, a broken build should be allowed to remain in a failing state overnight so developers can fix it the next morning.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** FALSE  
**Explanation:** A core CI principle is that a broken build must be treated as the team's highest priority and fixed immediately (or rolled back). Leaving it broken blocks everyone working against the same branch.  
</details>

---

**Q15.** RAG (Retrieval-Augmented Generation) requires retraining the foundation model every time the knowledge base is updated.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** FALSE  
**Explanation:** RAG retrieves updated documents from an external vector store at inference time. The model itself is never retrained — only the knowledge base index needs to be updated, making RAG far cheaper and faster to keep current than fine-tuning.  
</details>

---

**Q16.** AWS Bedrock is a managed service, meaning customers do not need to provision or manage the underlying GPU infrastructure to run foundation models.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** TRUE  
**Explanation:** AWS Bedrock is fully managed — AWS handles all infrastructure, scaling, and availability. Customers interact purely through APIs and are billed per token, with no server management required.  
</details>

---

**Q17.** GitFlow's use of long-lived `develop` and `release` branches makes it easier to achieve true Continuous Integration compared to trunk-based development.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** FALSE  
**Explanation:** GitFlow's long-lived branches mean code stays isolated for extended periods, increasing merge complexity and delaying integration feedback. Trunk-based development — with short-lived or no feature branches — is the model that best supports true CI.  
</details>

---

## Part 3: Fill in the Blank

**Q18.** The AWS Bedrock Java SDK v2 API designed for multi-turn conversations, which automatically manages the conversation history, is called the __________ API.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** **Converse**  
**Explanation:** The `Converse` API (called via `BedrockRuntimeClient.converse()`) is Bedrock's unified multi-turn conversation interface. It accepts a list of `Message` objects representing conversation history and works across different model providers with a consistent request/response schema.  
</details>

---

**Q19.** In DevOps, the practice of automatically deploying every code change that passes all automated tests directly to production — with no manual approval gate — is called Continuous __________.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** **Deployment**  
**Explanation:** Continuous Deployment is the highest level of automation in the CI/CD pipeline. Unlike Continuous Delivery (which stops at keeping software releasable), Continuous Deployment removes the human gate and releases every green build to production automatically.  
</details>

---

**Q20.** SonarCloud uses a concept called a __________ __________ to define a set of conditions (e.g., coverage thresholds, bug limits) that must all pass before code is considered ready to merge or release.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** **Quality Gate**  
**Explanation:** A Quality Gate is a configurable set of conditions in SonarCloud (e.g., "new code coverage >= 80%", "zero new critical bugs"). If any condition fails, the gate fails, and the CI pipeline can be configured to block the merge or deployment.  
</details>

---

## Part 4: Code Prediction

**Q21.** Given the following Java snippet using the AWS Bedrock SDK v2:

```java
BedrockRuntimeClient client = BedrockRuntimeClient.builder()
    .region(Region.US_EAST_1)
    .build();

ConverseRequest request = ConverseRequest.builder()
    .modelId("anthropic.claude-3-sonnet-20240229-v1:0")
    .messages(Message.builder()
        .role(ConversationRole.USER)
        .content(ContentBlock.fromText("Explain DevOps in one sentence."))
        .build())
    .build();

try {
    ConverseResponse response = client.converse(request);
    System.out.println(response.output().message().content().get(0).text());
} catch (ThrottlingException e) {
    System.out.println("Rate limit hit: " + e.getMessage());
} catch (BedrockRuntimeException e) {
    System.out.println("Bedrock error: " + e.getMessage());
}
```

What exception type specifically handles the case where AWS Bedrock throttles your request due to too many API calls per second, and what does the catch block print in that scenario?

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `ThrottlingException` is caught. The program prints: `Rate limit hit: ` followed by the exception's detail message from AWS.  
**Explanation:** AWS SDK v2 throws `ThrottlingException` (a subtype of `BedrockRuntimeException`) when the request rate exceeds the model's provisioned throughput. Because `ThrottlingException` is listed first in the catch chain, it is matched before the more general `BedrockRuntimeException` handler, and the first print statement executes. The second catch block only runs for non-throttling Bedrock errors.  
- **Why `BedrockRuntimeException` alone is insufficient:** It would catch throttling, but since `ThrottlingException` appears first (more specific before more general), the more specific block always wins for throttling scenarios.  
- **Key SDK pattern:** Always catch specific AWS exception subtypes before their parent classes to handle different failure modes with appropriate retry/backoff logic.  
</details>

---

**Q22.** Consider this partial Java method that calls the Bedrock Converse API:

```java
public static String chat(BedrockRuntimeClient client, String userMessage) {
    ConverseRequest req = ConverseRequest.builder()
        .modelId("amazon.titan-text-express-v1")
        .messages(
            Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(userMessage))
                .build()
        )
        .inferenceConfig(InferenceConfiguration.builder()
            .maxTokens(5)
            .temperature(0.0f)
            .build())
        .build();

    ConverseResponse res = client.converse(req);
    return res.output().message().content().get(0).text();
}

// Called as:
System.out.println(chat(client, "What is 2 + 2?"));
```

Given `maxTokens(5)` and `temperature(0.0f)`, what is the most likely output, and what risk does this configuration introduce?

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** The output is likely a very short fragment such as `4` or `The answer is` — possibly truncated mid-sentence — because `maxTokens(5)` caps the response at 5 tokens. The risk is **response truncation**: the model cannot complete a coherent answer within the token budget.  
**Explanation:** Setting `temperature(0.0f)` makes the model fully deterministic (highest-probability next token always chosen), which is appropriate for factual questions. However, `maxTokens(5)` severely limits output. For a simple arithmetic question the model may return "4" (1 token) or begin "The answer is 4" and be cut off. Crucially, the API call succeeds with no exception thrown — truncation is silent. The caller receives an incomplete string with no indication that the model had more to say.  
- **Why this is not an exception:** The SDK does not throw an error when `maxTokens` is reached; it simply stops generation at that boundary and returns what was produced.  
- **Practical fix:** Set `maxTokens` to a value appropriate for the expected response length (e.g., 256–1024 for conversational answers) and validate response completeness via `stopReason` in the response metadata.  
</details>
