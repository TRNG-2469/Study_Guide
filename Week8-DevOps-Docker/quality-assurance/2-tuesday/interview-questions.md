# Interview Questions: Week 8 — AWS Bedrock & DevOps/CI/CD
**Day:** Tuesday | **Difficulty Distribution:** 70% Beginner · 25% Intermediate · 5% Advanced

---

## 🟢 Beginner — Foundational Knowledge (Q1–Q13)

### Q1: What is AWS Bedrock and what problem does it solve for developers?
**Keywords:** managed service, foundation models, generative AI
**Hint:** Think about what developers would have to do without it.
<details>
<summary>Click to Reveal Answer</summary>

AWS Bedrock is a fully managed service that provides access to high-performing foundation models (FMs) from leading AI companies through a single API. It solves the problem of developers having to build, train, and host their own large language models, which requires massive compute resources, ML expertise, and significant cost. Instead, developers can focus on building applications using pre-trained models without managing any infrastructure.
</details>

---

### Q2: What is a foundation model?
**Keywords:** pre-trained, large-scale, transfer learning
<details>
<summary>Click to Reveal Answer</summary>

A foundation model is a large AI model trained on a vast and diverse dataset that can be adapted to a wide range of downstream tasks through fine-tuning or prompting. These models serve as a "foundation" upon which specialized applications are built. Examples include Claude (Anthropic), Titan (Amazon), Llama (Meta), and Stable Diffusion for image generation.
</details>

---

### Q3: What does the `temperature` parameter control in a language model?
**Keywords:** randomness, creativity, determinism
<details>
<summary>Click to Reveal Answer</summary>

The `temperature` parameter controls the randomness of a model's output. A temperature of 0 makes the model highly deterministic, always choosing the most probable next token, which is useful for factual or code-generation tasks. A higher temperature (e.g., 0.8–1.0) introduces more randomness and creativity, useful for brainstorming or creative writing. Values are typically between 0 and 1.
</details>

---

### Q4: What is the difference between fine-tuning and RAG (Retrieval-Augmented Generation)?
**Keywords:** fine-tuning, RAG, model weights, retrieval
<details>
<summary>Click to Reveal Answer</summary>

Fine-tuning involves updating a model's weights by training it on a domain-specific dataset, making the model itself "learn" new knowledge or behavior permanently. RAG, on the other hand, leaves the model's weights unchanged and instead retrieves relevant documents from an external knowledge base at inference time, injecting that context into the prompt. Fine-tuning is best for style/behavior changes; RAG is best for keeping knowledge current without retraining.
</details>

---

### Q5: What is prompt engineering and how does it differ from fine-tuning?
**Keywords:** prompt, instructions, zero-shot, few-shot
<details>
<summary>Click to Reveal Answer</summary>

Prompt engineering is the practice of crafting input text (prompts) to guide a model's output without modifying any model weights. Techniques include zero-shot prompting, few-shot prompting with examples, and chain-of-thought prompting. Unlike fine-tuning, prompt engineering requires no training data or compute — it works entirely at inference time. It is the fastest and cheapest way to customize model behavior but offers less control than fine-tuning.
</details>

---

### Q6: What are the 4 DORA metrics?
**Keywords:** DORA, deployment frequency, lead time, MTTR, change failure rate
<details>
<summary>Click to Reveal Answer</summary>

The four DORA (DevOps Research and Assessment) metrics are: (1) Deployment Frequency — how often an organization deploys to production; (2) Lead Time for Changes — the time from code commit to production; (3) Mean Time to Restore (MTTR) — how quickly a team recovers from a failure; and (4) Change Failure Rate — the percentage of deployments that cause a failure in production. Together they measure both throughput and stability of a software delivery pipeline.
</details>

---

### Q7: What is the difference between Continuous Delivery and Continuous Deployment?
**Keywords:** manual approval, automated release, pipeline
<details>
<summary>Click to Reveal Answer</summary>

Continuous Delivery means every code change is automatically built, tested, and made ready to release to production, but an actual release to production still requires a manual approval or trigger. Continuous Deployment goes one step further — every change that passes automated tests is automatically released to production without any human intervention. Continuous Delivery is appropriate when business or compliance reasons require a human sign-off before going live.
</details>

---

### Q8: What is the fail-fast principle in Continuous Integration?
**Keywords:** early feedback, pipeline, build failure
<details>
<summary>Click to Reveal Answer</summary>

The fail-fast principle in CI means that a pipeline should detect and report failures as early as possible, stopping subsequent stages the moment a critical check fails. For example, if unit tests fail, the pipeline should not proceed to integration tests or deployment. This reduces wasted compute, gives developers rapid feedback, and prevents broken code from propagating further down the pipeline.
</details>

---

### Q9: What does SonarLint do that SonarCloud cannot, and vice versa?
**Keywords:** IDE, real-time, cloud, quality gate
<details>
<summary>Click to Reveal Answer</summary>

SonarLint is an IDE plugin that provides real-time, local static analysis as you write code — it flags issues immediately without a commit or build, like a spell-checker for code quality. SonarCloud is a cloud-hosted service that performs a full analysis of an entire codebase or pull request in a CI/CD pipeline, enforces quality gates, tracks trends over time, and provides dashboards for the whole team. SonarLint cannot enforce quality gates; SonarCloud cannot analyze code before it is committed.
</details>

---

### Q10: What is a quality gate in SonarCloud?
**Keywords:** pass/fail, metrics threshold, merge block
<details>
<summary>Click to Reveal Answer</summary>

A quality gate in SonarCloud is a set of conditions (thresholds) that a codebase must meet before a build is considered acceptable. Typical conditions include: new code coverage must be >= 80%, no new blocker bugs, duplicated lines on new code < 3%, and security rating of A. If any condition fails, the quality gate fails and SonarCloud can block a pull request or notify the team, acting as an automated compliance check.
</details>

---

### Q11: What is DevSecOps?
**Keywords:** security, shift-left, pipeline integration
<details>
<summary>Click to Reveal Answer</summary>

DevSecOps is the practice of integrating security practices and tooling directly into the DevOps pipeline, rather than treating security as a separate phase done at the end. The idea is to "shift security left" so that vulnerabilities are caught early in development — through tools like SAST (static analysis), DAST (dynamic analysis), dependency scanning, and secrets detection — making security a shared responsibility across Dev, Sec, and Ops teams.
</details>

---

### Q12: What is trunk-based development?
**Keywords:** main branch, short-lived branches, feature flags
<details>
<summary>Click to Reveal Answer</summary>

Trunk-based development is a version control strategy where all developers integrate their code into a single shared branch (the "trunk" or `main`) at least once per day, keeping branches extremely short-lived (hours, not weeks). It avoids long-running feature branches and the painful merge conflicts they cause. To hide incomplete features in production, teams use feature flags. It is a key enabler of true Continuous Integration.
</details>

---

### Q13: What is the DevOps lifecycle? Name all 8 phases.
**Keywords:** plan, code, build, test, release, deploy, operate, monitor
<details>
<summary>Click to Reveal Answer</summary>

The DevOps lifecycle consists of 8 continuous phases: (1) Plan — define requirements and sprints; (2) Code — write and version-control source code; (3) Build — compile and package the application; (4) Test — run automated tests; (5) Release — prepare artifacts for deployment; (6) Deploy — push to the target environment; (7) Operate — manage infrastructure and configuration; (8) Monitor — observe performance and collect feedback to feed back into planning.
</details>

---

## 🟡 Intermediate — Application & Scenario (Q14–Q17)

### Q14: Your Bedrock API call is throwing a `ThrottlingException`. How do you handle this in Java using the AWS SDK v2?
**Keywords:** exponential backoff, retry policy, RetryMode, SdkClientException
<details>
<summary>Click to Reveal Answer</summary>

The AWS SDK v2 has built-in retry logic that can be configured on the client. You should configure a retry policy using `RetryMode.ADAPTIVE` or `RetryMode.STANDARD` on the `BedrockRuntimeClient` builder, which implements exponential backoff with jitter automatically. For finer control, implement a custom `RetryPolicy` or wrap calls in a try-catch for `ThrottlingException` and use `Thread.sleep` with an exponential delay. Additionally, request a service quota increase in the AWS console if throttling is persistent.
</details>

---

### Q15: When would you choose RAG over fine-tuning for an AWS Bedrock use case?
**Keywords:** knowledge currency, cost, latency, domain data
<details>
<summary>Click to Reveal Answer</summary>

Choose RAG when your knowledge base changes frequently (e.g., internal wikis, product documentation, support tickets) since you can update the vector store without retraining. RAG is also preferred when you have a limited labeled dataset insufficient for effective fine-tuning, when cost and time are constraints (no training job needed), or when you need source attribution (RAG can cite retrieved documents). Fine-tuning is better when you need the model to adopt a specific tone, format, or behavior that cannot be achieved through prompting alone.
</details>

---

### Q16: SonarCloud blocks a merge request due to a failed quality gate. What are your options as a developer?
**Keywords:** remediate, override, false positive, quality gate policy
<details>
<summary>Click to Reveal Answer</summary>

Your first option is to fix the flagged issues — address the new bugs, vulnerabilities, or coverage gaps that caused the gate to fail, then push a new commit to trigger a re-analysis. If an issue is a confirmed false positive, you can mark it as such in the SonarCloud UI with justification, which removes it from the gate calculation. A project admin can also temporarily adjust the quality gate thresholds, though this is a governance decision not to be taken lightly. Bypassing the gate entirely (e.g., forcing the merge) should only happen with explicit team agreement and a follow-up ticket to address the issues.
</details>

---

### Q17: How does a sprint in Agile align with CI/CD pipeline cadence?
**Keywords:** sprint, iteration, release cycle, pipeline frequency
<details>
<summary>Click to Reveal Answer</summary>

An Agile sprint (typically 1–2 weeks) defines the business rhythm of delivering a potentially shippable increment, while CI/CD operates at a much higher frequency — developers integrate code multiple times per day and the pipeline runs on every commit. The sprint cadence governs when stakeholders review working software (sprint review) and which features are prioritized, whereas the CI/CD pipeline ensures code is always in a deployable state throughout the sprint. In a mature setup, the team can deploy to production at any point during a sprint, decoupling release decisions from technical readiness.
</details>

---

## 🔴 Advanced — Deep Dive & System Design (Q18)

### Q18: Your team's deployment frequency is once per quarter. Using DORA metrics, describe the full pipeline transformation — from branch strategy through CI/CD configuration to quality gate setup — required to reach elite performer status (multiple deploys per day) without sacrificing stability.
**Keywords:** DORA elite, trunk-based development, feature flags, automated testing pyramid, quality gates, MTTR, change failure rate, blue-green deployment
<details>
<summary>Click to Reveal Answer</summary>

Reaching elite DORA status from quarterly deployments requires a multi-layer transformation. First, adopt trunk-based development — eliminate long-lived feature branches in favour of short-lived branches (< 1 day) merged to `main`, using feature flags (e.g., AWS AppConfig or LaunchDarkly) to gate incomplete features in production. Second, restructure the CI pipeline around the testing pyramid: fast unit tests (< 5 min) must gate every commit; integration and contract tests run on merge to main; heavy E2E suites run asynchronously or on a nightly cadence to avoid blocking the fast path. Third, configure SonarCloud quality gates on the "New Code" definition only — blocking on new critical issues and coverage drops without penalising legacy debt, preventing gate fatigue that leads to bypasses. Fourth, implement progressive delivery for CD: blue-green or canary deployments on AWS (e.g., CodeDeploy with traffic shifting) let you roll back in minutes if error rate or latency spikes, directly improving MTTR. Fifth, instrument production with real-time observability (CloudWatch metrics, structured logs, alarms) so the on-call engineer can detect and restore within one hour, keeping MTTR low even as deploy frequency rises. Finally, track Change Failure Rate in a lightweight incident log — a CFR above 15% signals the test suite or review process needs strengthening before increasing frequency further. The combined effect moves all four DORA metrics into the elite band: deploy frequency daily+, lead time < one hour, MTTR < one hour, CFR < 5%.
</details>

---
