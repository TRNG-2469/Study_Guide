# Amazon EC2 — Elastic Compute Cloud Fundamentals

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what EC2 is and where it fits in AWS
- Choose an appropriate instance type for a given workload
- Launch an EC2 instance using the console and CLI
- Understand the complete instance lifecycle
- Explain EC2's billing model and how to avoid surprise charges
- Use EC2 Instance Connect as a browser-based SSH alternative

---

## Why This Matters

EC2 is the backbone of AWS compute. Nearly every architecture you encounter will include EC2 instances — directly as application servers, or indirectly as the nodes behind ECS clusters and Auto Scaling groups. Understanding EC2 fundamentals is the prerequisite for everything else this week: SSH access, security groups, EBS volumes, auto scaling, and container deployments all build on top of EC2.

---

## What Is EC2?

**Amazon Elastic Compute Cloud (EC2)** provides resizable virtual servers in the cloud. Each server is called an **instance**. You choose the operating system, CPU, RAM, storage, and networking — and AWS runs the hardware underneath.

### Analogy

EC2 is like renting a computer from a data center. You specify what kind of computer you want (CPU cores, RAM, disk), AWS provisions it in minutes, and you pay only for the time it is running. When you no longer need it, you return it (terminate it) and stop paying.

---

## Instance Types

AWS organizes instance types into **families** based on workload characteristics, and **sizes** within each family.

### Instance Type Naming Convention

```
t3.medium
│ │  └── Size (nano, micro, small, medium, large, xlarge, 2xlarge, ...)
│ └───── Generation (1, 2, 3, 4, ...)
└─────── Family (t, m, c, r, g, ...)
```

### Common Instance Families

| Family | Optimized For | Use Case | Example |
|---|---|---|---|
| **t** (Burstable) | Cost | Dev/test, low-traffic web | `t3.micro`, `t3.small` |
| **m** (General Purpose) | Balance of CPU/RAM | App servers, microservices | `m5.large`, `m5.xlarge` |
| **c** (Compute) | CPU | High-traffic APIs, batch processing | `c5.2xlarge` |
| **r** (Memory) | RAM | Databases, caches, in-memory analytics | `r6i.4xlarge` |
| **g** (GPU) | GPU acceleration | Machine learning, video encoding | `g4dn.xlarge` |
| **i** (Storage) | High I/O NVMe | NoSQL, data warehouses | `i3.large` |
| **p** (Accelerated) | GPU for ML training | Deep learning | `p3.2xlarge` |

### T-Family Burst Credits (Important for Beginners)

The `t` family (t2, t3, t3a) uses a **CPU credit** model:
- When idle, instances accumulate CPU credits.
- When load spikes, instances spend credits to burst above their baseline CPU.
- When credits run out, CPU is throttled to baseline (e.g., 10–20% of one vCPU).

This makes t-class instances excellent for development workloads but dangerous for sustained high-CPU production loads.

### Choosing the Right Instance Type

| Scenario | Recommended |
|---|---|
| Learning / development | `t3.micro` (Free Tier eligible) |
| Spring Boot app, low traffic | `t3.small` or `t3.medium` |
| Spring Boot app, moderate production traffic | `m5.large` |
| CPU-bound processing | `c5.large` |
| Database server | `r5.large` |

---

## Key Pairs

When you launch an EC2 instance, you associate a **key pair** with it. This key pair is used for SSH authentication.

- **Public key:** AWS stores this on the instance in `~/.ssh/authorized_keys`
- **Private key (.pem file):** You download this once and keep it secret

> If you lose your .pem file, you cannot connect to the instance via SSH. There is no "reset password" equivalent for key-pair authentication. Store .pem files securely.

### Creating a Key Pair (CLI)

```bash
# Create a key pair and save the private key to a file
aws ec2 create-key-pair \
  --key-name my-key-pair \
  --query 'KeyMaterial' \
  --output text > my-key-pair.pem

# Restrict permissions so SSH will accept it
chmod 400 my-key-pair.pem
```

---

## Launching an EC2 Instance

### Using the AWS Console

1. Navigate to **EC2 → Instances → Launch instances**
2. **Name:** Give your instance a descriptive name (e.g., `myapp-server-1`)
3. **AMI:** Select an AMI — for beginners, choose **Amazon Linux 2023** or **Ubuntu 22.04 LTS**
4. **Instance type:** Start with `t3.micro` (Free Tier eligible)
5. **Key pair:** Select an existing key pair or create a new one
6. **Network settings:** Choose a VPC and subnet; configure security groups (covered in the next lesson)
7. **Storage:** Default 8 GiB gp3 root volume is fine for most exercises
8. **Advanced → User data (optional):** A shell script that runs on first boot
9. Click **Launch instance**

### Using the AWS CLI

```bash
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \        # Amazon Linux 2 AMI ID (region-specific)
  --instance-type t3.micro \                 # Instance type
  --key-name my-key-pair \                   # Key pair name (not the .pem file)
  --security-group-ids sg-0abc123def456789 \ # Security group ID
  --subnet-id subnet-0abc123def456789 \      # Subnet ID
  --count 1 \                                # Number of instances to launch
  --tag-specifications \
    'ResourceType=instance,Tags=[{Key=Name,Value=myapp-server-1},{Key=Environment,Value=dev}]'
```

### User Data — Bootstrap Script

User data is a shell script that runs automatically on first boot. It is ideal for installing software, pulling code, or starting services.

```bash
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.micro \
  --key-name my-key-pair \
  --user-data file://bootstrap.sh
```

`bootstrap.sh` example:

```bash
#!/bin/bash
# This script runs as root on first boot

# Update packages
yum update -y

# Install Java 17
yum install -y java-17-amazon-corretto

# Create application directory
mkdir -p /opt/myapp

# Download application JAR from S3
aws s3 cp s3://my-bucket/app.jar /opt/myapp/app.jar

# Start the application
java -jar /opt/myapp/app.jar &
```

---

## Instance Lifecycle

Understanding the lifecycle is critical for cost management and operational awareness.

```
         ┌──────────────────────────────────────────┐
         │              INSTANCE LIFECYCLE           │
         └──────────────────────────────────────────┘

  [Launch] ──────────────────────────────► [Pending]
                                               │
                                               │ (boot completes)
                                               ▼
                          ┌───────────── [Running] ──────────────┐
                          │                  │                   │
                    stop-instances      reboot-instances    terminate-instances
                          │                  │                   │
                          ▼                  ▼                   ▼
                      [Stopping]         [Running]           [Shutting-down]
                          │             (stays running)           │
                          ▼                                       ▼
                      [Stopped] ────────────────────────► [Terminated]
                          │          terminate-instances      (permanent)
                          │
                    start-instances
                          │
                          ▼
                      [Pending]
                          │
                          ▼
                      [Running]
```

### Lifecycle State Definitions

| State | Billing | Description |
|---|---|---|
| **Pending** | No charge | Instance is booting; not yet usable |
| **Running** | Charged | Instance is operational; you are billed per second |
| **Stopping** | No charge | Transitioning to stopped state |
| **Stopped** | No charge for compute; EBS volumes still billed | Instance is off; can be restarted |
| **Shutting-down** | No charge | Transitioning to terminated |
| **Terminated** | No charge | Permanent deletion; cannot be restarted |

### Critical Distinction: Stop vs. Terminate

| | Stop | Terminate |
|---|---|---|
| **Effect** | Instance shuts down; EBS root volume preserved | Instance and root volume are deleted |
| **Reversible?** | Yes — can be started again | No — permanent |
| **EBS data** | Preserved | Deleted (unless volume has "Delete on Termination" = false) |
| **Instance store data** | Lost | Lost |
| **IP address** | Public IP released; private IP retained | Both released |
| **Billing** | Compute stops; storage continues | Everything stops |

> **Warning for beginners:** "Terminate" means permanent deletion in AWS. It does NOT mean "end the SSH session." Always double-check before terminating an instance.

---

## Billing Model

EC2 billing is calculated per second (minimum 60 seconds) for most instance types.

### On-Demand Pricing (default)

- Pay for exactly what you use, no commitment
- Highest per-hour rate
- Best for: development, unpredictable workloads, short-term experiments

### Savings Plans and Reserved Instances

- Commit to 1 or 3 years of usage in exchange for 30–70% discount
- Best for: stable production workloads with predictable load

### Spot Instances

- Bid on spare AWS capacity at up to 90% discount
- AWS can reclaim the instance with 2 minutes notice
- Best for: batch processing, CI/CD runners, fault-tolerant workloads

### Free Tier

New AWS accounts include 750 hours/month of `t2.micro` or `t3.micro` for 12 months. One `t3.micro` running 24/7 = 720 hours/month — safely within the free tier.

---

## EC2 Instance Connect — Browser-Based SSH

**EC2 Instance Connect** lets you SSH into an instance directly from the AWS Console without a .pem file. It temporarily pushes a one-time public key to the instance and opens a terminal in your browser.

### Requirements

- Instance must be running Amazon Linux 2, Amazon Linux 2023, or Ubuntu 20.04+
- Instance must have a public IP address
- The EC2 Instance Connect endpoint must be reachable (security group must allow port 22 from AWS IP ranges — or use the VPC endpoint)

### Using EC2 Instance Connect

1. Open **EC2 Console → Instances**
2. Select your running instance
3. Click **Connect**
4. Select the **EC2 Instance Connect** tab
5. Click **Connect**

A browser terminal opens. No .pem file required.

### EC2 Instance Connect via CLI

```bash
# Send a one-time SSH public key to the instance
aws ec2-instance-connect send-ssh-public-key \
  --instance-id i-0abcd1234efgh5678 \
  --instance-os-user ec2-user \
  --ssh-public-key file://~/.ssh/id_rsa.pub \
  --availability-zone us-east-1a

# Then SSH normally — the key is valid for 60 seconds
ssh -i ~/.ssh/id_rsa ec2-user@<public-ip>
```

---

## Summary

- EC2 provides resizable virtual servers (instances) in the cloud.
- Instance families are optimized for specific workloads: t (burst/dev), m (general), c (compute), r (memory).
- Key pairs authenticate SSH; the private .pem file must be kept secure.
- The instance lifecycle: Pending → Running → Stopped/Terminated.
- "Terminate" is irreversible; "Stop" preserves the EBS root volume.
- Billing is per-second while running; stopped instances only pay for EBS storage.
- EC2 Instance Connect provides browser-based SSH — no .pem file needed.

---

## External Resources

- [EC2 Instance Types](https://aws.amazon.com/ec2/instance-types/)
- [EC2 User Guide — Getting Started](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html)
- [EC2 Instance Connect Documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Connect-using-EC2-Instance-Connect.html)
