# Model Customization

## Learning Objectives

By the end of this lesson, you will be able to:

- Distinguish between prompt engineering, RAG, and fine-tuning as customization strategies
- Explain when each approach is appropriate
- Describe the difference between continued pre-training and instruction fine-tuning
- Outline the data requirements for fine-tuning
- Describe the high-level fine-tuning workflow in AWS Bedrock

---

## Why This Matters

A foundation model out of the box is powerful but generic. Real business applications need AI that behaves in specific, reliable ways — answering questions using your company's proprietary knowledge, writing in your brand's voice, or extracting data in a specific format every time. Model customization is how you bridge the gap between a generic model and one that fits your use case precisely. Knowing which customization strategy to reach for first saves enormous time and cost.

---

## The Customization Spectrum

Think of foundation model customization as a spectrum from lightest to heaviest:

```
Lighter ←————————————————————————————————————→ Heavier

Prompt Engineering → RAG → Fine-Tuning → Continued Pre-Training
(minutes)           (days)  (days/weeks)    (weeks/months)
(free)              (low $) (moderate $$)   (high $$$)
```

You should always start at the lightest option that meets your needs. Each step to the right increases cost, complexity, and time significantly.

---

## Strategy 1: Prompt Engineering

### What It Is

Prompt engineering is the practice of crafting the input text (the prompt) you send to the model to elicit better, more consistent, or more structured responses — without changing the model itself.

This is not a workaround. It is a legitimate engineering discipline with real techniques:

- **System prompts:** Instructions placed before the conversation that set the model's role, constraints, and output format
- **Few-shot examples:** Including 2–5 examples of ideal input/output pairs in the prompt so the model learns by demonstration
- **Chain-of-thought prompting:** Instructing the model to "think step by step" before answering, which dramatically improves reasoning accuracy
- **Output format specification:** Telling the model exactly what format to produce ("respond only with valid JSON using this schema: ...")
- **Role assignment:** "You are a senior Java developer reviewing code for security vulnerabilities..."

### When to Use Prompt Engineering

Use prompt engineering:
- As your **first and default approach** — it is free, fast, and reversible
- When the task is well-defined and examples can be included inline
- When you need to iterate quickly
- When you are prototyping or exploring what is possible

### When Prompt Engineering Is Not Enough

Prompt engineering alone may not suffice when:
- The model consistently lacks knowledge of your proprietary domain (it was never trained on it)
- Your prompts are becoming extremely long and hitting context limits
- You need perfect consistency across thousands of outputs (format, tone, style)
- Latency is critical and shorter prompts are required

---

## Strategy 2: Retrieval-Augmented Generation (RAG)

### What It Is

RAG is a pattern that gives the model access to your data at inference time by retrieving relevant content and including it in the prompt dynamically. The model's weights are NOT changed — you are simply providing better context.

The RAG pipeline:

```
1. User asks a question
         |
         v
2. The question is converted to a vector embedding
         |
         v
3. A vector database is searched for similar content
         |
         v
4. The most relevant documents/chunks are retrieved
         |
         v
5. Retrieved content + user question → sent to the model as a prompt
         |
         v
6. Model answers based on the retrieved content (not just its training data)
```

### When to Use RAG

Use RAG when:
- The model needs access to **your proprietary, internal, or frequently updated information** (product manuals, internal policies, customer records)
- You need answers **grounded in specific source documents** (the model can cite its source)
- Your data changes frequently and you cannot retrain on every change
- You want to avoid hallucination by grounding responses in retrieved facts

### RAG in AWS Bedrock

AWS offers **Bedrock Knowledge Bases**, a managed service that handles the RAG pipeline automatically. You point it at documents stored in S3, and it handles:
- Chunking the documents into segments
- Generating embeddings using Titan Embeddings
- Storing embeddings in a managed vector store (Amazon OpenSearch Serverless or Aurora)
- Retrieving relevant chunks at query time

This removes most of the engineering complexity from building a RAG system.

### When RAG Is Not Enough

RAG does not help when:
- The model's fundamental reasoning or language capabilities need improvement for your domain
- You need the model to generate in a highly specific style that cannot be achieved by prompt examples
- Response latency from retrieving + generating is too high for your use case

---

## Strategy 3: Fine-Tuning

### What It Is

Fine-tuning takes a pre-trained foundation model and continues the training process on a smaller, curated dataset specific to your domain or task. Unlike inference and RAG, fine-tuning **permanently updates the model's weights** to encode new patterns.

The result is a **custom model** — a version of the base model that has been adapted. In Bedrock, your fine-tuned model is stored privately in your AWS account and is not shared with other customers or used to update the base model.

### Two Types of Fine-Tuning

#### Continued Pre-Training (Domain Adaptation)

Continued pre-training exposes the model to a large body of **unlabeled domain text** using the same self-supervised training objective as the original pre-training (predicting the next token). It teaches the model the language, vocabulary, and concepts of your domain without teaching it a specific behavior.

**Example:** A pharmaceutical company has access to 50 million pages of clinical trial reports. Continued pre-training on this corpus teaches the model the specialized vocabulary and sentence patterns of clinical research. After this, the model understands terms like "randomized controlled trial," "adverse event," and "pharmacokinetics" far better than the base model.

**Data requirements:**
- Large volume: hundreds of millions to billions of tokens recommended
- Unlabeled: just raw text, no annotations needed
- Domain-representative: should reflect the actual text the model will encounter

**When to use:**
- Your domain uses highly specialized terminology
- You have massive amounts of domain text available
- You are willing to invest significant compute and time

#### Instruction Fine-Tuning (Task Adaptation)

Instruction fine-tuning trains the model on **labeled input/output pairs** that demonstrate the exact behavior you want. Each training example is a (prompt, ideal response) pair.

**Example:** A legal tech company creates 10,000 examples, each consisting of a contract clause (the prompt) and a JSON object listing the clause type, key obligations, and any risk flags (the ideal response). After instruction fine-tuning, the model reliably extracts this structured information from new clauses.

**Data requirements:**
- Minimum viable: 100–500 high-quality examples (results will be limited)
- Good results: 1,000–10,000 examples
- Excellent results: 10,000+ examples
- Must be high quality: errors in training data teach the model to make the same errors
- Format: typically JSONL files with "prompt" and "completion" fields

**When to use:**
- You need highly consistent output format or style
- The base model frequently makes errors on your specific task that prompt engineering cannot fix
- You have sufficient labeled examples

### The Fine-Tuning Workflow in AWS Bedrock

1. **Prepare your dataset:** Create a JSONL file with prompt/completion pairs, stored in S3
2. **Choose a base model:** Not all Bedrock models support fine-tuning; check the AWS documentation
3. **Configure a training job:** In the Bedrock console or via API, specify the base model, S3 location of training data, hyperparameters (number of epochs, learning rate), and the S3 output location
4. **Run the training job:** AWS provisions the compute, runs training, and saves the custom model to your account. This typically takes minutes to hours depending on dataset size
5. **Evaluate the custom model:** Test it against a held-out evaluation dataset
6. **Deploy for inference:** Provision throughput (purchase model units) and invoke your custom model via the same Bedrock API using your custom model ARN

### Fine-Tuning Costs

Fine-tuning in Bedrock has two cost components:
- **Training cost:** Charged per token processed during training (can be significant for large datasets)
- **Inference cost:** Custom models require provisioned throughput (a committed capacity purchase), not the on-demand pricing of base models. This means fine-tuned models have a higher fixed cost per hour.

---

## Decision Framework: Which Strategy to Choose?

Work through these questions in order:

```
Q1: Does the model need access to your private/proprietary data to answer correctly?
    YES → Start with RAG
    NO  → Continue to Q2

Q2: Are you getting poor results with prompt engineering alone?
    NO  → Stick with prompt engineering; optimize your prompts
    YES → Continue to Q3

Q3: Is the problem that the model lacks domain vocabulary / knowledge?
    YES → Consider continued pre-training (if you have large unlabeled data)
         OR RAG (if the data is more document-retrieval-oriented)
    NO  → Continue to Q4

Q4: Is the problem inconsistent behavior, wrong format, or task-specific errors?
    YES → Consider instruction fine-tuning (if you have 1,000+ labeled examples)
    NO  → Re-examine your prompt engineering approach
```

---

## Summary

| Strategy | Changes Model? | Data Needed | Cost | Best For |
|---|---|---|---|---|
| Prompt Engineering | No | None | Free | Default starting point; most use cases |
| RAG | No | Documents in S3/vector DB | Low-moderate | Proprietary knowledge, grounded answers |
| Instruction Fine-Tuning | Yes | 1K–10K+ labeled pairs | Moderate-high | Consistent format/behavior, task-specific accuracy |
| Continued Pre-Training | Yes | Millions+ of domain tokens | High | Deep domain vocabulary adaptation |

---

## External Resources

1. **AWS Bedrock Fine-Tuning Documentation** — https://docs.aws.amazon.com/bedrock/latest/userguide/custom-models.html
2. **AWS Bedrock Knowledge Bases (Managed RAG)** — https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html
3. **Prompt Engineering Guide** — https://www.promptingguide.ai/
