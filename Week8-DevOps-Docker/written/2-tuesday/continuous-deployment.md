# Continuous Deployment

## Learning Objectives

By the end of this lesson, you will be able to:

- Define Continuous Deployment and explain how it differs from Continuous Delivery
- Explain the role of feature flags in enabling safe continuous deployment
- Describe blue-green deployments and their benefits
- Explain canary releases and how traffic splitting works
- Describe automated rollback strategies

---

## Why This Matters

Continuous Deployment represents the highest maturity level of the CI/CD spectrum. When every passing build automatically goes to production without human intervention, the speed of feedback from real users becomes a competitive advantage. Companies like Amazon, Netflix, and Google deploy to production thousands of times per day using these practices. Understanding these patterns prepares you to work in elite engineering organizations and to design systems that can safely evolve at high velocity.

---

## Continuous Deployment Defined

**Continuous Deployment** is the practice where every code change that passes all automated checks is automatically deployed to production — no human approval required, no manual step before users see it.

This sounds alarming at first. "Automatically deploying to production without anyone pressing a button?" But consider: if your automated test suite is comprehensive and your deployment is fully automated, what exactly is the human approval adding? If the answer is "a chance to catch what the tests missed," then the real solution is better tests — not a manual gate. The manual gate is a symptom of insufficient confidence in the automated process.

Continuous Deployment forces teams to invest deeply in:
- Comprehensive automated testing (unit, integration, E2E, performance)
- Fast and reliable monitoring with automated alerting
- Safe deployment patterns (feature flags, blue-green, canary) so that any bad deployment can be detected and reversed quickly
- A culture of small, incremental changes rather than large batches

---

## The Prerequisite: Small, Safe Changes

Continuous Deployment is only viable when individual changes are small and self-contained. A change that deploys one new API endpoint is low-risk — if something goes wrong, it is obvious what caused it, and reverting is trivial. A change that deploys six months of accumulated features simultaneously is high-risk — debugging failures is hard, and rolling back means losing all six months of work.

This creates a virtuous cycle:
- Small changes are safer to deploy automatically
- Automatic deployment encourages small, frequent changes
- Small, frequent changes make the system easier to understand and debug
- A system that is easier to understand and debug produces better code

---

## Feature Flags

### The Problem They Solve

Continuous Deployment requires that every commit to main can safely go to production immediately. But what about a new feature that is only half-built? Or a feature that is built but not yet announced publicly?

Without feature flags, the team would need to keep the feature on a long-lived branch until it is complete — exactly the kind of long-lived branching that creates integration problems. With feature flags, incomplete or unreleased features ship to production in a disabled state.

### What Is a Feature Flag?

A **feature flag** (also called a feature toggle, feature switch, or feature gate) is a conditional in your code that enables or disables functionality at runtime, without redeploying.

```java
// Basic feature flag using a configuration property
@Service
public class PaymentService {

    // This value is loaded from configuration (environment variable, database, or
    // a feature flag service like LaunchDarkly or AWS AppConfig)
    @Value("${features.newCheckoutFlow.enabled:false}")
    private boolean newCheckoutFlowEnabled;

    public PaymentResult processPayment(PaymentRequest request) {
        if (newCheckoutFlowEnabled) {
            // New checkout implementation (in development; not yet public)
            return processWithNewCheckoutFlow(request);
        } else {
            // Existing, proven checkout implementation
            return processWithLegacyCheckoutFlow(request);
        }
    }
}
```

When `features.newCheckoutFlow.enabled=false` (the default), all users get the legacy flow. Development continues to ship to production with the flag off. When the feature is ready and the business decides to launch it, a configuration change — not a code deployment — flips the flag to `true`.

### Types of Feature Flags

**Release flags:** Control whether a new feature is visible to users. Used to decouple deployment (code goes to production) from release (users see the feature). This is the most common type.

**Experiment flags (A/B testing):** Route a percentage of users to a new experience for performance testing. "Show the new recommendation algorithm to 10% of users and measure their purchase conversion rate."

**Operational flags (kill switches):** Quickly disable a feature in production if it is causing performance problems, without a redeployment. "The new search feature is causing database overload — flip the flag off immediately."

**Permission flags:** Enable features for specific users or user groups. "Only show the beta dashboard to users in the BETA_TESTERS group."

### Feature Flag Best Practices

- **Clean up old flags:** A flag for a feature that has been fully launched for 3 months is now dead code. Remove it. Accumulated flags become a maintenance burden and a source of confusion.
- **Use a proper feature flag service for production:** Hardcoding flags in configuration files works for development but does not support runtime changes. Tools like AWS AppConfig, LaunchDarkly, or Unleash allow changing flag values instantly without redeployment.
- **Test both states:** CI should test with the flag both on and off to catch regressions in either path.

---

## Blue-Green Deployments

### The Concept

A **blue-green deployment** maintains two identical production environments, called "blue" and "green." At any given time, one environment is live (serving production traffic) and the other is idle (standing by for the next deployment).

```
Current state: BLUE is live (v1.2.3)
               GREEN is idle

Step 1: Deploy new version v1.3.0 to GREEN
        (GREEN is not yet serving traffic; users unaffected)

Step 2: Run smoke tests and validation against GREEN
        (Verify v1.3.0 works correctly)

Step 3: Switch the load balancer to route traffic to GREEN
        (GREEN becomes live; v1.3.0 now serves all users)
        (BLUE is now idle but still running v1.2.3)

Step 4: Monitor GREEN (v1.3.0) for several minutes
        If healthy: BLUE can be terminated or kept as rollback
        If unhealthy: Switch load balancer back to BLUE instantly
```

### Benefits

**Zero downtime:** Traffic switches happen at the load balancer level in milliseconds. Users experience no downtime during the deployment.

**Instant rollback:** If the new version (GREEN) has a problem, reverting means switching the load balancer back to BLUE — a single configuration change that takes effect in seconds. Compare this to a traditional rollback that requires re-running a deployment process.

**Production environment validation:** You can validate v1.3.0 in the exact production environment before it receives real traffic. This catches environment-specific issues that staging may have missed.

**Enables database schema validation:** Deploy the new code to GREEN, run it against a read-only clone of the production database, validate queries work correctly, then cut over.

### Implementation on AWS

Blue-green deployments on AWS are commonly implemented with:
- **AWS Elastic Load Balancer (ALB):** Weighted target groups control traffic distribution
- **AWS CodeDeploy:** Has built-in blue-green deployment support for ECS, Lambda, and EC2
- **AWS ECS with Application Load Balancer:** Create two ECS services (blue and green) and use listener rules to control which service receives traffic

### Limitations

Blue-green deployments require maintaining two full production environments simultaneously, which doubles infrastructure cost during the deployment window. For large applications, this can be significant. This is why canary deployments are often preferred for very large-scale systems.

---

## Canary Releases

### The Concept

A **canary release** (named after the historical practice of miners bringing canary birds into coal mines — if the canary died, there was dangerous gas) gradually shifts traffic from the old version to the new version, monitoring for problems at each step.

```
100% → v1.2.3 (stable)

Step 1: Deploy v1.3.0 alongside v1.2.3
         5% → v1.3.0 (canary)
        95% → v1.2.3 (stable)

Monitor 15 minutes: error rate, latency, user complaints
Is everything healthy? → continue

Step 2: Increase canary traffic
        25% → v1.3.0
        75% → v1.2.3

Monitor 30 minutes
Is everything healthy? → continue

Step 3: 50/50 split
        Monitor 30 minutes

Step 4: Majority on new version
        90% → v1.3.0
        10% → v1.2.3

Monitor 30 minutes

Step 5: Full rollout
       100% → v1.3.0
              v1.2.3 terminated
```

### Why Canary Beats Blue-Green for Scale

Canary deployments limit the **blast radius** of a bad deployment. If v1.3.0 has a critical bug, only 5% of users are affected before it is detected and rolled back. With a blue-green deployment, the switch is all-or-nothing — every user immediately gets the new version.

For a service with 10 million active users, a 5% canary means 500,000 users are affected by a bad deployment before detection. That is still a lot of affected users, but dramatically better than 10 million.

### Automated Canary Analysis

Modern canary releases use automated metric comparison to decide whether to proceed or roll back:

```
Canary Metrics (v1.3.0, 5% traffic)    Baseline Metrics (v1.2.3, 95% traffic)
─────────────────────────────────────  ─────────────────────────────────────
Error rate: 0.03%                      Error rate: 0.02%    ✓ ACCEPTABLE DEVIATION
P99 latency: 145ms                     P99 latency: 132ms   ⚠ ELEVATED (threshold: 150ms)
Success rate: 99.97%                   Success rate: 99.98% ✓ ACCEPTABLE DEVIATION

Analysis result: PROCEED to 10% canary
```

If any metric exceeds a defined threshold relative to the baseline (e.g., error rate triples, P99 latency increases by more than 20%), the canary is automatically halted and rolled back.

Tools that support automated canary analysis:
- **AWS CodeDeploy with CloudWatch alarms:** Define alarms that trigger automatic rollback
- **Argo Rollouts (Kubernetes):** Sophisticated progressive delivery with metric-based analysis
- **Spinnaker:** Multi-cloud CD platform with built-in canary analysis
- **Flagger (Kubernetes):** GitOps-based progressive delivery with metric analysis

---

## Automated Rollback

### Why It Matters

In Continuous Deployment, when a bad deployment goes out, you want it detected and reversed with minimal human intervention — especially outside business hours. Manual rollbacks require an on-call engineer to be paged, understand the situation, and execute commands. Automated rollbacks detect the problem and execute the rollback in seconds.

### Health Check-Based Rollback

The most common rollback trigger: deployment fails if the new version's health checks do not pass within a timeout.

```yaml
# AWS ECS Task Definition - deployment configuration
deploymentConfiguration:
  minimumHealthyPercent: 100  # Keep old tasks running until new ones are healthy
  maximumPercent: 200         # Allow up to 2x tasks during deployment
  
# Health check definition
healthCheck:
  command: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
  interval: 10      # Check every 10 seconds
  timeout: 5        # Fail if no response within 5 seconds
  retries: 3        # Fail the health check after 3 consecutive failures
  startPeriod: 30   # Give the container 30 seconds to start before health checks begin
```

If the new task fails its health check 3 consecutive times, ECS stops the deployment and keeps the old tasks running. No human intervention required.

### Metric-Based Automated Rollback

More sophisticated: monitor business and technical metrics after deployment and roll back if they deviate from baseline.

```
Post-deployment monitoring window: 15 minutes

Metric alerts (CloudWatch Alarms):
  - Error rate > 1%: ALARM → trigger CodeDeploy rollback
  - P99 latency > 2000ms: ALARM → trigger CodeDeploy rollback
  - Failed transactions > 10/minute: ALARM → trigger CodeDeploy rollback

If any alarm fires within the monitoring window:
  1. CodeDeploy detects the CloudWatch alarm state
  2. Initiates automatic rollback to previous deployment
  3. Sends notification to on-call Slack channel:
     "Deployment auto-rolled back due to elevated error rate alarm"
```

### Rollback vs. Roll Forward

There is debate in the DevOps community between "rollback" (revert to the previous version) and "roll forward" (fix the bug and deploy a new version). Arguments for roll forward:
- In a true CD environment, a new version can be deployed in minutes anyway
- Rollbacks can be complicated if they involve database schema changes
- The underlying bug still exists in the codebase and must be fixed regardless

In practice, teams use both: automated rollback for immediate production stability, followed by a roll-forward fix when the engineer wakes up to address the root cause.

---

## Database Migrations in Continuous Deployment

Database schema changes are the hardest part of continuous deployment. A deployment can be rolled back instantly, but a database schema change (adding a column, renaming a table) is not easily reversible, and the old code may not work with the new schema.

The solution: **expand-contract migrations** (also called the "make-before-break" pattern):

```
Phase 1 (Expand): Add the new column; old code ignores it, new code uses it
Phase 2 (Migrate): Background job copies data from old column to new column  
Phase 3 (Contract): Remove the old column once all services are on new code
```

Each phase is a separate deployment. No deployment is breaking — old and new code can run simultaneously at any point. This is essential for zero-downtime continuous deployment.

---

## Summary

| Concept | Key Takeaway |
|---|---|
| Continuous Deployment | Every passing build automatically deploys to production; no human gate |
| vs. Continuous Delivery | Delivery has a human approval; Deployment is fully automated |
| Feature flags | Decouple deployment from release; ship incomplete features safely |
| Blue-green deployment | Two identical environments; traffic switches instantly; rollback in seconds |
| Canary release | Gradual traffic shift; limits blast radius; enables metric-based rollback |
| Automated rollback | Health checks and metric alarms trigger automatic revert; no human required |
| DB migrations | Expand-contract pattern enables zero-downtime schema changes |

---

## External Resources

1. **Martin Fowler: Feature Toggles** — https://martinfowler.com/articles/feature-toggles.html
2. **AWS CodeDeploy Blue-Green Deployments** — https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-steps-ecs.html
3. **Argo Rollouts: Progressive Delivery for Kubernetes** — https://argoproj.github.io/rollouts/
