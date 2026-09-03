# EC2 Auto Scaling

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what Auto Scaling is and the problem it solves
- Configure a Launch Template for repeatable instance launches
- Define minimum, maximum, and desired capacity for a scaling group
- Set up the three types of scaling policies
- Understand how CloudWatch alarms trigger scaling events
- Explain how health checks enable automatic instance replacement

---

## Why This Matters

One of the promises of cloud computing is elasticity — the ability to grow and shrink your infrastructure automatically in response to real demand. Without Auto Scaling, you face a painful choice: over-provision (pay for idle capacity 24/7) or under-provision (let your app crash under load). Auto Scaling solves this by continuously matching your server count to your actual traffic. It is the cornerstone of cost-efficient, resilient cloud architectures and directly enables the deployment patterns you will use throughout Week 8.

---

## What Is EC2 Auto Scaling?

**EC2 Auto Scaling** automatically adjusts the number of EC2 instances in a group based on demand, health checks, or a schedule. It works in three directions:

- **Scale out:** Add instances when load increases
- **Scale in:** Remove instances when load decreases
- **Replace:** Automatically replace unhealthy instances

### Analogy

Think of a call center that staffs up during business hours and sends agents home at night. Auto Scaling is the manager that automatically calls in more agents when the phone queue backs up, and sends agents home early when calls slow down — except it happens in seconds rather than hours.

---

## Core Components

```
┌─────────────────────────────────────────────────────┐
│              Auto Scaling Group (ASG)               │
│                                                     │
│  ┌──────────────┐    ┌──────────────────────────┐  │
│  │    Launch    │    │    Scaling Policies       │  │
│  │   Template   │    │  (when to add/remove)     │  │
│  │ (what to     │    └──────────────────────────┘  │
│  │  launch)     │                 ▲                │
│  └──────────────┘                 │                │
│         │                 CloudWatch Alarms         │
│         ▼                         │                │
│   [EC2] [EC2] [EC2]              Metrics           │
│    min=1  desired=2  max=5                          │
└─────────────────────────────────────────────────────┘
```

---

## Launch Templates

A **Launch Template** defines the configuration of the instances that Auto Scaling will launch. It answers the question: "When Auto Scaling needs to add an instance, what should it look like?"

### What a Launch Template Contains

| Setting | Example |
|---|---|
| AMI ID | `ami-0abc123def456789` (your Golden AMI) |
| Instance type | `t3.medium` |
| Key pair | `my-key-pair` |
| Security groups | `sg-0abc123def456789` |
| IAM instance profile | `EC2-SSM-Role` |
| User data script | Bootstrap script to start the app |
| EBS volume config | 20 GiB gp3 root volume |
| Tags | `Environment=production`, `App=myapp` |

### Creating a Launch Template (Console)

1. **EC2 → Launch Templates → Create launch template**
2. Fill in:
   - **Template name:** `myapp-launch-template`
   - **AMI:** Your Golden AMI ID
   - **Instance type:** `t3.medium`
   - **Key pair:** `my-key-pair`
   - **Security groups:** `myapp-sg`
   - **User data:** (optional startup script)
3. Click **Create launch template**

### Creating a Launch Template (CLI)

```bash
aws ec2 create-launch-template \
  --launch-template-name myapp-launch-template \
  --version-description "v1 - initial" \
  --launch-template-data '{
    "ImageId": "ami-0abc123def456789",
    "InstanceType": "t3.medium",
    "KeyName": "my-key-pair",
    "SecurityGroupIds": ["sg-0abc123def456789"],
    "IamInstanceProfile": {
      "Name": "EC2-SSM-Role"
    },
    "UserData": "IyEvYmluL2Jhc2gKamF2YSAtamFyIC9vcHQvbXlhcHAvYXBwLmphciAmCg==",
    "TagSpecifications": [{
      "ResourceType": "instance",
      "Tags": [
        {"Key": "Name", "Value": "myapp-asg-instance"},
        {"Key": "Environment", "Value": "production"}
      ]
    }]
  }'
```

> The `UserData` value is Base64-encoded. Encode your script with: `base64 -w 0 bootstrap.sh`

---

## Min, Max, and Desired Capacity

Every Auto Scaling Group (ASG) has three capacity settings:

| Setting | Meaning | Example |
|---|---|---|
| **Minimum** | ASG will never have fewer than this many instances | `1` |
| **Desired** | ASG targets this count under normal conditions | `2` |
| **Maximum** | ASG will never exceed this many instances | `10` |

```
Instances: 1 ──────────── 2 ──────────── 10
           ▲              ▲              ▲
        Minimum        Desired         Maximum
      (always keep   (normal state)  (never exceed)
       at least 1)
```

### Practical Rules

- **Minimum ≥ 1** for production: ensures there is always at least one instance serving traffic even if scaling logic has a bug.
- **Desired** starts at the minimum or your estimated normal load.
- **Maximum** caps your spending and prevents runaway scaling from a bug.

### Creating an Auto Scaling Group (CLI)

```bash
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name myapp-asg \
  --launch-template "LaunchTemplateName=myapp-launch-template,Version=\$Latest" \
  --min-size 1 \
  --max-size 10 \
  --desired-capacity 2 \
  --vpc-zone-identifier "subnet-0abc123,subnet-0def456" \  # Subnets across multiple AZs
  --health-check-type ELB \                                # Use load balancer health checks
  --health-check-grace-period 300 \                        # Wait 300s before checking new instances
  --tags Key=Name,Value=myapp-asg-instance,PropagateAtLaunch=true
```

---

## Scaling Policies

Scaling policies define *when* and *by how much* to scale. There are three types.

### 1. Target Tracking Scaling (Recommended for Most Cases)

Target tracking is the simplest and most intelligent policy. You specify a target value for a metric, and AWS automatically adjusts capacity to keep the metric at that target.

**Analogy:** Like cruise control in a car — you set the target speed, the car adjusts the throttle automatically.

```bash
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name myapp-asg \
  --policy-name myapp-cpu-target-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ASGAverageCPUUtilization"
    },
    "TargetValue": 60.0,
    "DisableScaleIn": false
  }'
```

This policy keeps average CPU utilization at 60%. If CPU exceeds 60%, AWS adds instances. If CPU drops significantly below 60%, AWS removes instances.

**Common Target Tracking Metrics:**

| Metric | Target Value | Meaning |
|---|---|---|
| `ASGAverageCPUUtilization` | 60.0 | Keep average CPU at 60% |
| `ALBRequestCountPerTarget` | 1000 | Keep 1000 requests/instance/minute |
| `ASGAverageNetworkIn` | 10000000 | Keep avg network in at 10 MB/s |

### 2. Step Scaling

Step scaling lets you define different scaling amounts for different alarm thresholds. Useful when you want aggressive scale-out but conservative scale-in.

```bash
# First, create a CloudWatch alarm
aws cloudwatch put-metric-alarm \
  --alarm-name myapp-high-cpu \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 60 \                    # Evaluate every 60 seconds
  --threshold 75 \                 # Trigger when CPU > 75%
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \         # Alarm triggers after 2 consecutive periods
  --dimensions Name=AutoScalingGroupName,Value=myapp-asg \
  --alarm-actions arn:aws:autoscaling:us-east-1:123456789012:scalingPolicy:...

# Then, create the step scaling policy
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name myapp-asg \
  --policy-name myapp-step-scale-out \
  --policy-type StepScaling \
  --adjustment-type ChangeInCapacity \
  --step-adjustments '[
    {
      "MetricIntervalLowerBound": 0,
      "MetricIntervalUpperBound": 20,
      "ScalingAdjustment": 1
    },
    {
      "MetricIntervalLowerBound": 20,
      "ScalingAdjustment": 3
    }
  ]'
```

This policy:
- Adds 1 instance when CPU is 75–95%
- Adds 3 instances when CPU is above 95%

### 3. Scheduled Scaling

Scheduled scaling adjusts capacity at specific times. Ideal for predictable traffic patterns.

```bash
# Scale up every weekday morning at 8:00 AM UTC
aws autoscaling put-scheduled-update-group-action \
  --auto-scaling-group-name myapp-asg \
  --scheduled-action-name scale-up-morning \
  --recurrence "0 8 * * 1-5" \      # Cron: 8am Monday-Friday
  --desired-capacity 5 \
  --min-size 3

# Scale down every weekday evening at 6:00 PM UTC
aws autoscaling put-scheduled-update-group-action \
  --auto-scaling-group-name myapp-asg \
  --scheduled-action-name scale-down-evening \
  --recurrence "0 18 * * 1-5" \     # Cron: 6pm Monday-Friday
  --desired-capacity 2 \
  --min-size 1
```

---

## CloudWatch Alarms

**CloudWatch Alarms** watch metrics and trigger actions (like scaling) when thresholds are crossed.

### Alarm States

| State | Meaning |
|---|---|
| `OK` | Metric is within the threshold |
| `ALARM` | Metric has breached the threshold |
| `INSUFFICIENT_DATA` | Not enough data points yet |

### Common Metrics for Scaling

| Metric | Namespace | When to Scale Out |
|---|---|---|
| `CPUUtilization` | `AWS/EC2` | > 70% for 2 minutes |
| `RequestCountPerTarget` | `AWS/ApplicationELB` | > 1000/minute/instance |
| `MemoryUtilization` | `CWAgent` (custom) | > 80% |
| `ActiveConnectionCount` | `AWS/ApplicationELB` | > 5000 |

### Viewing Alarm Status

```bash
aws cloudwatch describe-alarms \
  --alarm-names myapp-high-cpu \
  --query 'MetricAlarms[0].{State:StateValue,Reason:StateReason}'
```

---

## Health Checks and Automatic Replacement

One of Auto Scaling's most powerful features is **automatic instance replacement**. If an instance fails a health check, Auto Scaling terminates it and launches a replacement.

### Health Check Types

| Type | What It Checks |
|---|---|
| **EC2 (default)** | Instance status checks (hardware/hypervisor level) |
| **ELB** | Whether the load balancer reports the instance as healthy |

### Health Check Grace Period

When a new instance launches, it needs time to boot and start the application before health checks run. The **grace period** (default 300 seconds) pauses health checking for new instances.

```
Instance launches
       │
       │  [300 second grace period — no health checks]
       │
       ▼
Health checks begin
  Pass → Instance stays in service
  Fail → Instance terminated; replacement launched
```

### Viewing ASG Activity

```bash
# See recent scaling activities
aws autoscaling describe-scaling-activities \
  --auto-scaling-group-name myapp-asg \
  --max-items 10 \
  --query 'Activities[*].{Time:StartTime,Status:StatusCode,Description:Description}' \
  --output table
```

---

## Cooldown Periods

After a scaling event, Auto Scaling waits a **cooldown period** (default 300 seconds) before evaluating alarms again. This prevents rapid repeated scaling triggered by the same spike.

```
Scale out event (add 2 instances)
       │
       │  [300 second cooldown — ignore alarms]
       │
       ▼
Resume monitoring → evaluate alarms normally
```

---

## Summary

- Auto Scaling matches EC2 instance count to real demand, eliminating over/under-provisioning.
- Launch Templates define what gets launched: AMI, instance type, security groups, user data.
- Min/Desired/Max capacity set the bounds; Auto Scaling keeps instance count within these limits.
- Target Tracking is the recommended policy — set a target metric value; AWS adjusts automatically.
- Step Scaling gives fine-grained control for different threshold levels.
- Scheduled Scaling handles predictable patterns (business hours, weekly cycles).
- CloudWatch Alarms watch metrics and trigger scaling actions.
- Health checks enable automatic replacement of failed instances.

---

## External Resources

- [EC2 Auto Scaling User Guide](https://docs.aws.amazon.com/autoscaling/ec2/userguide/what-is-amazon-ec2-auto-scaling.html)
- [Scaling Policy Types Explained](https://docs.aws.amazon.com/autoscaling/ec2/userguide/as-scaling-simple-step.html)
- [CloudWatch Metrics for EC2 Auto Scaling](https://docs.aws.amazon.com/autoscaling/ec2/userguide/ec2-auto-scaling-cloudwatch-monitoring.html)
