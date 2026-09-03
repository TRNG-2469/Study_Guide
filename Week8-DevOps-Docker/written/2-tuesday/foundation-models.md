# Foundation Models

## Learning Objectives

By the end of this lesson, you will be able to:

- Define what a foundation model is and why the term "foundation" is used
- Explain the difference between inference and fine-tuning
- Describe what a context window is and why its size matters
- Explain temperature and sampling parameters and how they affect output
- Compare the major model families available in AWS Bedrock

---

## Why This Matters

Before you can use AWS Bedrock effectively, you need to understand the thing you are actually calling: the foundation model. Understanding how these models work — even at a conceptual level — helps you write better prompts, choose the right model for the right task, tune parameters for your use case, and debug unexpected outputs. This is the foundational literacy that separates a developer who uses AI tools from one who genuinely engineers with them.

---

## What Is a Foundation Model?

A **foundation model** is a large-scale machine learning model trained on a vast, diverse dataset — typically hundreds of billions of words of text drawn from the internet, books, code repositories, scientific papers, and more. The training process teaches the model statistical patterns in language: which words tend to follow other words, how concepts relate, how arguments are structured, how code behaves.

The word "foundation" is deliberate. These models serve as a **base** upon which many different applications can be built, much like how a building's foundation supports many different kinds of structures on top of it. A single foundation model can be used for customer service, legal analysis, code generation, translation, and creative writing — all without any retraining, simply by changing the prompt.

### Scale Is What Makes Them Different

Earlier machine learning models were trained on small, specific datasets to perform one task (classify spam email, detect a face in a photo). Foundation models are trained on general data at a scale that produces **emergent capabilities** — abilities that were not explicitly programmed and were not predictable from smaller models. These include:

- Multi-step reasoning ("If A implies B, and B implies C, what does A imply?")
- In-context learning (adapting behavior based on examples given in the prompt)
- Code generation from natural language descriptions
- Translating between languages the model was not explicitly trained on

This generality is what makes foundation models so powerful as a platform for application development.

---

## Inference vs. Fine-Tuning

These two terms describe different ways of using a foundation model.

### Inference

**Inference** is the act of sending a prompt to a trained model and receiving a response. This is what happens every time you call the Bedrock API. The model's internal parameters (its "knowledge," encoded as billions of numerical weights) do not change. You are simply using the model as-is.

Inference is:
- Instant (responses typically arrive in under a second to a few seconds)
- Cheap (you pay only for the tokens processed)
- Flexible (you change the model's behavior entirely by changing your prompt)
- Stateless (the model has no memory between API calls unless you include prior conversation in your prompt)

**The vast majority of Bedrock applications use only inference.** Prompt engineering — the practice of crafting effective prompts — is a powerful enough tool that most business use cases do not require going further.

### Fine-Tuning

**Fine-tuning** takes a pre-trained foundation model and continues training it on a smaller, task-specific dataset. The model's weights are updated to improve performance on a particular domain or style.

Think of the analogy of hiring a newly graduated lawyer versus a specialist. The new graduate (the foundation model) has broad general knowledge of law. If you want a specialist in maritime law, you could put them through several additional months of specialized training — that is fine-tuning.

Fine-tuning is appropriate when:
- The domain uses specialized vocabulary or formats the base model handles poorly
- You need consistent output style (e.g., your brand always writes in a specific voice)
- You have thousands of high-quality labeled examples
- Prompt engineering alone cannot achieve acceptable accuracy

Fine-tuning is NOT appropriate when:
- You simply want to give the model access to new information (use RAG instead)
- You have fewer than a few hundred training examples
- You need rapid iteration (fine-tuning jobs take time and cost money)

AWS Bedrock supports fine-tuning for select model families. We will cover the workflow in the next lesson.

---

## Context Windows

Every foundation model has a **context window** — the maximum amount of text it can process in a single interaction, measured in tokens.

Everything inside the context window at the time of a request is "visible" to the model:
- Your system instructions (e.g., "You are a helpful assistant...")
- The conversation history (prior turns)
- Any documents or data you have included
- The current user message

Anything outside the context window is invisible. The model has no memory of it.

### Token Counting

A **token** is the basic unit of text that models process. Tokens are not exactly words or characters — they are fragments determined by the model's tokenizer. As a rough rule:
- 1 token ≈ 4 characters of English text
- 100 tokens ≈ 75 English words
- 1,000 tokens ≈ 750 words ≈ 1.5 pages of text

### Why Context Window Size Matters

| Context Window Size | What Becomes Possible |
|---|---|
| 4,096 tokens (~3,000 words) | Short documents, brief conversations |
| 16,000 tokens (~12,000 words) | Medium reports, multi-turn conversations |
| 100,000 tokens (~75,000 words) | Entire codebases, book chapters, lengthy legal contracts |
| 200,000 tokens (~150,000 words) | Entire books, large document sets |

Claude 3 models in Bedrock support up to 200,000 token context windows, making them suitable for tasks involving very large documents. Amazon Titan Text models have smaller windows, typically 8,000 tokens.

### The Context Window Is Not a Database

A critical misconception: the context window is not permanent storage. Every API call is independent. If you want the model to "remember" a previous conversation, you must include that conversation in the new request's context. This is why chat applications maintain conversation history and re-send it with each message.

---

## Temperature and Sampling Parameters

Foundation models generate text probabilistically. At each step, the model calculates a probability distribution over the entire vocabulary — essentially a ranked list of "what word is most likely to come next given everything so far." Sampling parameters control how the model selects from that distribution.

### Temperature

**Temperature** controls the randomness of the model's output. It is typically a value between 0 and 1 (some models allow higher).

- **Temperature = 0:** The model always picks the highest-probability next token. Output is deterministic and highly consistent. Two calls with identical prompts produce identical responses.
- **Temperature = 0.5:** Moderate randomness. The model sometimes picks lower-probability tokens, producing more varied responses while still being coherent.
- **Temperature = 1.0:** High randomness. The model samples more liberally from the probability distribution. Output is creative but can become incoherent or unpredictable.

**Rule of thumb:**
- Factual retrieval, data extraction, classification → low temperature (0 to 0.2)
- General question answering, summarization → medium temperature (0.3 to 0.7)
- Creative writing, brainstorming, marketing copy → higher temperature (0.7 to 1.0)

### Top-P (Nucleus Sampling)

**Top-P** (also called nucleus sampling) is an alternative to temperature that restricts the model to sampling from the smallest set of tokens whose cumulative probability exceeds P.

- **Top-P = 1.0:** No restriction; the model can sample from any token
- **Top-P = 0.9:** The model samples from the top 90% of probability mass, cutting off rare low-probability words
- **Top-P = 0.5:** The model is restricted to very high-confidence choices

Top-P and temperature are often used together. AWS Bedrock exposes both parameters for models that support them.

### Top-K

**Top-K** restricts sampling to the K most probable next tokens.

- **Top-K = 1:** Always picks the single most probable token (equivalent to temperature = 0)
- **Top-K = 50:** Samples from the 50 most probable tokens

### Max Tokens (Max New Tokens)

This parameter limits the length of the model's response. Setting it prevents runaway costs and ensures responses are appropriately concise. If the model reaches the token limit mid-sentence, it stops — so set this value generously enough for the expected response length.

---

## Comparing Model Families in Bedrock

### Side-by-Side Comparison

| Dimension | Claude 3 (Anthropic) | Titan Text (Amazon) | Llama 3 (Meta) | Mistral |
|---|---|---|---|---|
| Context window | Up to 200K tokens | Up to 8K tokens | Up to 128K tokens | Up to 32K tokens |
| Strengths | Reasoning, safety, long documents, nuanced instruction | AWS-native integration, embeddings | Cost efficiency, open weights | Speed, efficiency, European data residency |
| Best for | Customer-facing chat, complex analysis | RAG pipelines (embeddings), AWS-centric apps | High-volume cost-sensitive use cases | Fast inference, EU compliance needs |
| Fine-tuning in Bedrock | Yes (select variants) | Yes | Yes (select variants) | Limited |
| Relative cost | Higher for Opus, competitive for Haiku | Moderate | Lower | Lower |

### When to Choose Claude

Choose Claude when:
- The task requires nuanced multi-step reasoning
- Safety and refusal of harmful content is critical (Claude is trained with Constitutional AI)
- You are processing very long documents (200K context)
- The quality bar is high and cost is secondary

### When to Choose Titan

Choose Titan when:
- You need text embeddings for a RAG pipeline (Titan Embeddings is excellent for this)
- Your architecture is deeply AWS-native and you want minimal cross-vendor complexity
- You are building on AWS Knowledge Bases (a managed RAG service that uses Titan natively)

### When to Choose Llama

Choose Llama when:
- Cost is a primary constraint and volume is high
- You prefer open-weight models for transparency or auditability
- You want the option to eventually self-host the same model weights

### When to Choose Mistral

Choose Mistral when:
- Latency is critical (Mistral models are fast)
- You are in the EU and have data residency preferences
- Cost efficiency is important and task complexity is moderate

---

## Summary

| Concept | Key Takeaway |
|---|---|
| Foundation model | Large-scale pre-trained model usable for many tasks without retraining |
| Inference | Sending a prompt and receiving a response; model weights unchanged |
| Fine-tuning | Continued training on domain-specific data; updates model weights |
| Context window | Maximum text the model can process at once; measured in tokens |
| Temperature | Controls output randomness; 0 = deterministic, 1 = creative |
| Top-P / Top-K | Additional sampling constraints for controlling output distribution |
| Model families | Claude (reasoning/safety), Titan (AWS-native), Llama (open/cost), Mistral (fast/EU) |

---

## External Resources

1. **Hugging Face: What are Foundation Models?** — https://huggingface.co/blog/foundation-models
2. **Anthropic Claude Model Overview** — https://www.anthropic.com/claude
3. **AWS Bedrock Supported Foundation Models** — https://docs.aws.amazon.com/bedrock/latest/userguide/models-supported.html
