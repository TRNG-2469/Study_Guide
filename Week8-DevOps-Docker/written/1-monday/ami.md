# Amazon Machine Images (AMIs)

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what an AMI contains and why it matters for deployment consistency
- Create a custom AMI from a running EC2 instance
- Distinguish between an AMI and an EBS snapshot
- Apply the Golden AMI pattern to repeatable deployments
- Share an AMI across AWS accounts

---

## Why This Matters

Week 8's central theme is deploying software reliably at scale. The single biggest enemy of reliable deployment is the phrase "it works on my machine." AMIs eliminate that problem at the infrastructure level. When your application runs on a custom AMI, every new server that launches from it is byte-for-byte identical — same OS, same runtime, same configuration. This week you will use AMIs as the foundation for Auto Scaling groups, so understanding them now is essential.

---

## What Is an AMI?

An **Amazon Machine Image (AMI)** is a template that contains the complete information needed to launch an EC2 instance. Think of it as a **snapshot of an entire computer** — not just the data, but the operating system, installed software, configuration files, and startup scripts.

### Analogy

Imagine you spend two hours setting up a perfect laptop: installing your IDE, configuring your terminal, installing project dependencies, tuning system settings. Now imagine you could clone that laptop's exact state and hand it to 100 teammates, who each get an identical, ready-to-use machine in 60 seconds. That is what an AMI does for cloud servers.

---

## What Does an AMI Contain?

An AMI is composed of several components:

| Component | Description |
|---|---|
| **Root Volume Snapshot** | The EBS snapshot of the boot disk — the OS, file system, and installed software |
| **Launch Permissions** | Which AWS accounts can use this AMI |
| **Block Device Mapping** | Instructions for what volumes to attach at launch (root volume + any additional data volumes) |
| **Virtualization Type** | Either `hvm` (Hardware Virtual Machine — modern, required for current instance types) or `paravirtual` (legacy) |
| **Architecture** | `x86_64` or `arm64` |

### Root Device Types

- **EBS-backed (most common):** The root volume is an EBS volume. The instance can be stopped and restarted. Data on the root volume persists.
- **Instance store-backed (legacy):** The root volume is ephemeral storage on the host. The instance cannot be stopped — only terminated. Data is lost on termination.

---

## AMI Lifecycle

```
Running EC2 Instance
       │
       │  aws ec2 create-image
       ▼
  Custom AMI  ──────────────────────► Launch new EC2 instances
       │
       │  (stored as)
       ▼
  EBS Snapshot(s) in S3 (managed by AWS)
```

---

## Creating a Custom AMI from a Running Instance

### Step 1 — Prepare Your Instance

Before capturing the AMI, ensure the instance is in a clean state:

```bash
# On the EC2 instance, clear logs and temp files (optional but recommended)
sudo rm -rf /var/log/*.log
sudo rm -rf /tmp/*

# Ensure your application is installed and configured correctly
# Example: verify Java is installed
java -version

# Example: verify your Spring Boot JAR is in place
ls /opt/myapp/app.jar
```

### Step 2 — Create the AMI (AWS Console)

1. Open EC2 Console → **Instances**
2. Select your running instance
3. **Actions → Image and templates → Create image**
4. Fill in:
   - **Image name:** `myapp-java17-ubuntu22-v1.0`
   - **Image description:** `Spring Boot app server, Java 17, Ubuntu 22.04, configured June 2024`
   - **No reboot:** Leave unchecked (AWS will briefly reboot the instance to ensure filesystem consistency)
5. Click **Create image**

### Step 3 — Create the AMI (AWS CLI)

```bash
aws ec2 create-image \
  --instance-id i-0abcd1234efgh5678 \
  --name "myapp-java17-ubuntu22-v1.0" \
  --description "Spring Boot app server, Java 17, Ubuntu 22.04" \
  --no-reboot

# The command returns the new AMI ID immediately
# {
#   "ImageId": "ami-0123456789abcdef0"
# }
```

> **Note on `--no-reboot`:** Using `--no-reboot` skips the instance reboot, which means the filesystem is not flushed to disk cleanly. This is fine for read-only data but may cause corruption if the instance was writing data at the time. For production AMIs, allow the reboot.

### Step 4 — Wait for the AMI to Become Available

AMI creation takes 2–10 minutes depending on disk size.

```bash
# Poll until the AMI state is "available"
aws ec2 wait image-available --image-ids ami-0123456789abcdef0

# Or check the current state manually
aws ec2 describe-images \
  --image-ids ami-0123456789abcdef0 \
  --query 'Images[0].State'
```

---

## Launching a New Instance from Your Custom AMI

```bash
aws ec2 run-instances \
  --image-id ami-0123456789abcdef0 \
  --instance-type t3.micro \
  --key-name my-key-pair \
  --security-group-ids sg-0abc123def456789 \
  --subnet-id subnet-0abc123 \
  --count 1 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=myapp-server-2}]'
```

Every instance launched from this AMI starts with your application pre-installed and pre-configured — no manual setup required.

---

## AMI vs EBS Snapshot — Key Distinction

This is a commonly confused pair. Here is the precise difference:

| | AMI | EBS Snapshot |
|---|---|---|
| **What it is** | A launch template for an entire EC2 instance | A point-in-time backup of a single EBS volume |
| **Contains** | One or more volume snapshots + launch metadata | Just the volume data |
| **Used to** | Launch new EC2 instances | Restore or create a new EBS volume |
| **Relationship** | An AMI *references* one or more EBS snapshots | A snapshot is the raw storage behind an AMI |
| **Can launch an instance?** | Yes | No — must first create an AMI or attach as a data volume |

**Key insight:** When you create an AMI, AWS automatically creates EBS snapshots of every volume attached to the source instance. The AMI is a pointer to those snapshots plus metadata. If you deregister (delete) an AMI, the underlying snapshots are **not** automatically deleted — you must delete them separately to stop incurring storage costs.

---

## Sharing AMIs

### Share with a Specific AWS Account

```bash
aws ec2 modify-image-attribute \
  --image-id ami-0123456789abcdef0 \
  --launch-permission "Add=[{UserId=123456789012}]"
```

### Make an AMI Public

```bash
aws ec2 modify-image-attribute \
  --image-id ami-0123456789abcdef0 \
  --launch-permission "Add=[{Group=all}]"
```

> **Warning:** Making an AMI public exposes it to the entire internet. Only do this intentionally and ensure it contains no secrets, passwords, SSH keys, or application-specific credentials.

### Copy an AMI to Another Region

AMIs are region-specific. To use an AMI in a different region:

```bash
aws ec2 copy-image \
  --source-image-id ami-0123456789abcdef0 \
  --source-region us-east-1 \
  --region us-west-2 \
  --name "myapp-java17-ubuntu22-v1.0-uswest2"
```

---

## The Golden AMI Pattern

The **Golden AMI** (also called a hardened AMI or baked AMI) is an industry best practice for large-scale deployments.

### The Problem It Solves

Without a Golden AMI, the common approach is to launch a bare OS instance and run a configuration script (or Ansible playbook) on startup to install software. This approach has problems:

- Startup time is slow (may take 5–10 minutes for a fully provisioned instance)
- External package registries (apt, yum, npm) may be unavailable during an outage
- Inconsistencies creep in over time as packages receive minor version updates

### The Golden AMI Approach

```
Base OS AMI (official Ubuntu/Amazon Linux)
         │
         │  Install: runtime (Java, Node), app server config, security patches
         │  Configure: logging agents, monitoring agents, startup scripts
         │  Harden: remove unnecessary packages, restrict SSH access
         ▼
    Golden AMI  ─────────────────────────────────► Auto Scaling Group
         │                                          (launches 0-to-N instances
         │                                           in ~60 seconds each)
         │
         │  New version of app / new patches?
         ▼
    Golden AMI v2 ────────────────────────────────► Update Auto Scaling Group
```

### Golden AMI Benefits

| Benefit | Explanation |
|---|---|
| **Fast scaling** | Instances launch in 60–90 seconds because software is pre-installed |
| **Consistency** | All instances are byte-for-byte identical — no configuration drift |
| **Reliability** | No dependency on external package registries at launch time |
| **Security** | Security patches are baked in; no window where an unpatched instance runs |
| **Auditability** | Each AMI version is immutable and traceable |

### Naming Convention for Golden AMIs

Use a structured naming convention to track versions:

```
{app-name}-{os}-{runtime}-{version}-{date}

Examples:
  myapp-ubuntu22-java17-v1.2.3-20240601
  myapp-amazon-linux2-java21-v2.0.0-20240901
```

---

## Deregistering (Deleting) an AMI

```bash
# Step 1: Deregister the AMI
aws ec2 deregister-image --image-id ami-0123456789abcdef0

# Step 2: Find and delete the associated snapshot(s)
aws ec2 describe-snapshots \
  --filters "Name=description,Values=*ami-0123456789abcdef0*" \
  --query 'Snapshots[*].SnapshotId' \
  --output text

# Delete each snapshot
aws ec2 delete-snapshot --snapshot-id snap-0abc123def456789
```

---

## Summary

- An AMI is a complete launch template: OS + software + configuration + block device mapping.
- Creating a custom AMI captures a running instance's state as reusable template.
- AMIs reference EBS snapshots; deleting an AMI does not delete its snapshots.
- AMIs can be shared with specific accounts or made public.
- AMIs are region-specific; copy them explicitly to use in other regions.
- The Golden AMI pattern pre-bakes all software and config to enable fast, consistent, reliable deployments at scale.

---

## External Resources

- [AWS AMI Documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html)
- [Golden AMI Pipeline — AWS Blog](https://aws.amazon.com/blogs/awsmarketplace/announcing-the-golden-ami-pipeline/)
- [EC2 Image Builder (AMI automation)](https://docs.aws.amazon.com/imagebuilder/latest/userguide/what-is-image-builder.html)
