# Amazon EC2: Virtual Servers in the Cloud

## Learning Objectives
- Define Amazon EC2 and explain its role as the primary IaaS compute service in AWS.
- Describe the key components of an EC2 instance: instance types, AMIs, key pairs, security groups, and Elastic IPs.
- Launch, connect to, and stop an EC2 instance using the AWS Management Console and the AWS CLI.
- Choose an appropriate EC2 instance type and purchasing option for a given workload scenario.
- Understand how EC2 underpins other AWS services studied this week (e.g., RDS running on managed EC2 under the hood).

---

## Why This Matters

Every application you have ever used — a web server, a database, a REST API — runs on some piece of computing hardware. Before the cloud era, that hardware lived in a private data center, and it was **your** team's job to rack the servers, cable them, install operating systems, and keep them patched. This capital-heavy model made scaling slow and expensive.

**Amazon EC2 (Elastic Compute Cloud)** changed this by letting you rent virtual servers — called *instances* — by the second, from anywhere in the world, through a browser or an API call. This week's Weekly Epic places you at the transition point between relational databases and cloud infrastructure. EC2 is the backbone of that cloud infrastructure: Amazon RDS, the managed database service we cover later today, actually runs its PostgreSQL or MySQL engine **on** EC2 instances behind the scenes — AWS just manages those instances for you. Understanding EC2 directly gives you the mental model to reason about every managed service that sits on top of it.

As a full-stack Java developer, you will regularly deploy applications to EC2, debug connectivity through security groups, and configure IAM-based access from application code. This is a foundational skill the industry expects you to have on Day 1.

---

## The Concept

### What is Amazon EC2?

Amazon Elastic Compute Cloud (EC2) is a web service that provides resizable **virtual machine (VM) compute capacity** in the cloud. The word *elastic* refers to the ability to increase or decrease capacity within minutes based on demand.

An EC2 instance is a virtual server running inside AWS physical hardware. You choose:
- The **operating system** (via an Amazon Machine Image)
- The **hardware size** (via an instance type)
- The **network and security settings** (via a VPC and security groups)

AWS manages the physical host, the hypervisor layer, and the data center operations. You manage everything above the hypervisor — the OS, your software, and your data. This is the **IaaS model** you read about in the IaaS/PaaS/SaaS reading.

---

### Core EC2 Concepts

#### 1. Amazon Machine Images (AMIs)

An **AMI** is a pre-configured template that defines the software installed on your instance when it boots. Think of it as a *snapshot of a disk* that includes:
- The operating system (e.g., Amazon Linux 2, Ubuntu 22.04, Windows Server 2022)
- Optionally, pre-installed software (e.g., a Java runtime, a web server)
- Storage volume configuration

AMIs are categorized by source:

| AMI Source | Description |
|---|---|
| **AWS-provided** | Official AMIs maintained by AWS (e.g., Amazon Linux 2023) |
| **AWS Marketplace** | Third-party vendor AMIs (e.g., pre-installed database engines) |
| **Community AMIs** | Public AMIs contributed by the AWS community |
| **Custom (My AMIs)** | AMIs you create by snapshotting your own configured instance |

When you launch an instance, you select one AMI. Every instance started from the same AMI begins with an identical software environment.

---

#### 2. Instance Types

EC2 offers a large family of **instance types** optimized for different workloads. Instance types define the number of virtual CPUs (vCPUs), the amount of RAM, and the network/storage throughput characteristics.

The naming convention is: **`[Family][Generation].[Size]`**

Example: `t3.micro`
- `t` → Family = **General Purpose / Burstable**
- `3` → Generation = 3rd generation hardware
- `micro` → Size = smallest vCPU + RAM configuration

**Common Instance Families:**

| Family | Code | Optimized For | Example Use Case |
|---|---|---|---|
| General Purpose | `t`, `m` | Balanced CPU/memory | Web servers, small databases |
| Compute Optimized | `c` | High CPU-to-memory ratio | Batch processing, gaming servers |
| Memory Optimized | `r`, `x` | Large in-memory datasets | In-memory databases, real-time analytics |
| Storage Optimized | `i`, `d` | High sequential disk I/O | Data warehousing, Hadoop |
| Accelerated Computing | `p`, `g` | GPU workloads | Machine learning training, video rendering |

> **Tip for this week:** For lab exercises, the **`t3.micro`** instance (1 vCPU, 1 GB RAM) is free-tier eligible and sufficient for running a basic Java application or PostgreSQL database.

---

#### 3. Key Pairs

To connect securely to a Linux EC2 instance, AWS uses **SSH public-key authentication**. When you launch an instance:
1. You select or create a **key pair**.
2. AWS stores the **public key** on the instance.
3. You download and store the **private key** (`.pem` file) locally.

You then connect using the private key:

```bash
ssh -i /path/to/your-key.pem ec2-user@<public-ip-address>
```

> **Important Security Rule:** Your `.pem` private key file must never be committed to a Git repository or shared. Treat it like a password. Set its file permissions to read-only for your user only (`chmod 400 your-key.pem` on Linux/macOS).

---

#### 4. Security Groups

A **Security Group** acts as a virtual firewall for your EC2 instance. It controls **inbound** and **outbound** network traffic using rules.

Key characteristics:
- Security groups are **stateful**: if you allow an inbound request, the response is automatically allowed outbound.
- Rules are **allow-only**: there is no explicit deny rule; traffic not matching an allow rule is blocked by default.
- Multiple security groups can be attached to one instance.

**Example Security Group Rules for a Web Server:**

| Direction | Protocol | Port | Source/Destination | Purpose |
|---|---|---|---|---|
| Inbound | TCP | 22 | Your IP (e.g., `203.0.113.0/32`) | SSH access for you only |
| Inbound | TCP | 80 | `0.0.0.0/0` (Anywhere) | Public HTTP web traffic |
| Inbound | TCP | 443 | `0.0.0.0/0` (Anywhere) | Public HTTPS web traffic |
| Outbound | All | All | `0.0.0.0/0` (Anywhere) | Allow all outbound traffic |

> Restrict SSH (port 22) to your own IP address only. Opening SSH to `0.0.0.0/0` (all internet) is one of the most common cloud security mistakes.

---

#### 5. Elastic IP Addresses

By default, when you **stop and restart** an EC2 instance, its public IP address changes. For production systems that need a **stable, persistent IP address**, you allocate an **Elastic IP (EIP)**.

- An Elastic IP is a static public IPv4 address you allocate from AWS.
- You **associate** it with a running EC2 instance.
- The IP address remains the same even after the instance is stopped and restarted.

> **Cost Awareness:** AWS charges for Elastic IPs that are allocated but **not associated** with a running instance. Always release Elastic IPs you are no longer using to avoid unexpected charges.

---

#### 6. Instance Lifecycle

An EC2 instance transitions through several states:

```
[Not yet created]
       |
       v
   [Pending]  ← Instance is starting up; AWS is booting the OS
       |
       v
   [Running]  ← Instance is fully operational; you are billed per second here
       |
      / \
     /   \
    v     v
[Stopped] [Terminated]
  |            |
  |            +→ Permanently deleted; instance store volumes are lost
  |
  +→ Can be restarted; EBS-backed root volumes retain data while stopped
```

**Key Distinction:**
- **Stop**: The instance shuts down but its EBS root volume (persistent disk) is retained. You can restart it later. You are **not** charged for compute while stopped (but you are charged for EBS storage).
- **Terminate**: The instance and its ephemeral (instance store) volumes are **permanently deleted**. EBS volumes may or may not be deleted depending on the configuration.

---

#### 7. EC2 Purchasing Options

AWS offers several pricing models for EC2 capacity, depending on how predictable your workload is:

| Option | How It Works | Best For | Discount vs On-Demand |
|---|---|---|---|
| **On-Demand** | Pay per second, no commitment | Development, unpredictable workloads | — (baseline) |
| **Reserved Instances** | Commit to 1 or 3 years | Steady-state production workloads | Up to 72% off |
| **Spot Instances** | Bid on unused AWS capacity; can be interrupted | Batch jobs, fault-tolerant processing | Up to 90% off |
| **Savings Plans** | Flexible hourly commitment for 1–3 years | Mixed workloads with flexible instance types | Up to 66% off |

For this course, you will use **On-Demand** instances via AWS Academy credits.

---

### EC2 and the AWS Shared Responsibility Model

Recall the Shared Responsibility Model from today's reading. EC2 is a perfect example of how responsibilities are divided:

| Responsibility | AWS | You (Customer) |
|---|---|---|
| Physical data center security | ✅ | |
| Hardware maintenance | ✅ | |
| Hypervisor and virtualization | ✅ | |
| Operating system (provided via AMI) | ✅ | ✅ (you must patch it) |
| OS security patches going forward | | ✅ |
| Application installation | | ✅ |
| Security group configuration | | ✅ |
| Data encryption | | ✅ |
| IAM access management | | ✅ |

AWS keeps the lights on and the physical hardware running. You are responsible for keeping the **guest OS** patched, your **application** secure, and your **data** protected.

---

## Code Examples

### Launching an EC2 Instance with the AWS CLI

The AWS CLI allows you to automate instance creation without using the console. The following command launches a `t3.micro` Amazon Linux 2023 instance:

```bash
aws ec2 run-instances \
    --image-id ami-0c02fb55956c7d316 \
    --instance-type t3.micro \
    --key-name MyKeyPair \
    --security-group-ids sg-0a1b2c3d4e5f67890 \
    --subnet-id subnet-0abc123def456789a \
    --count 1 \
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=MyWebServer}]'
```

**Parameter breakdown:**
- `--image-id`: The AMI to boot from (Amazon Linux 2023 in `us-east-1`)
- `--instance-type`: Hardware size
- `--key-name`: The key pair to install for SSH access
- `--security-group-ids`: The firewall rules to apply
- `--tag-specifications`: A descriptive name tag for the instance

---

### Describing Running Instances

After launching, list your running instances to find the public IP:

```bash
aws ec2 describe-instances \
    --filters "Name=instance-state-name,Values=running" \
    --query "Reservations[*].Instances[*].[InstanceId,PublicIpAddress,State.Name,Tags[?Key=='Name'].Value|[0]]" \
    --output table
```

Example output:
```text
-----------------------------------------------------------------------
|                         DescribeInstances                           |
+--------------------+----------------+---------+---------------------+
|  i-0a1b2c3d4e5f678 |  54.198.47.22  | running |  MyWebServer        |
+--------------------+----------------+---------+---------------------+
```

---

### Connecting via SSH

Once the instance is running and its public IP is known:

```bash
# Set correct permissions on the private key (required on Linux/macOS)
chmod 400 MyKeyPair.pem

# Connect to the instance
ssh -i MyKeyPair.pem ec2-user@54.198.47.22

# Once connected, verify the OS version
cat /etc/os-release
```

Expected output inside the instance:
```text
NAME="Amazon Linux"
VERSION="2023"
```

---

### Stopping an Instance When Done

Always stop development instances when not in use to conserve AWS Academy credits:

```bash
# Stop the instance (data on EBS root volume is preserved)
aws ec2 stop-instances --instance-ids i-0a1b2c3d4e5f678

# Verify the instance state
aws ec2 describe-instance-status --instance-ids i-0a1b2c3d4e5f678
```

---

### Adding an Inbound Rule to a Security Group

If you need to open a port (for example, port 8080 for a Java web application):

```bash
aws ec2 authorize-security-group-ingress \
    --group-id sg-0a1b2c3d4e5f67890 \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0
```

> This example opens port 8080 to the entire internet. For production use, restrict the `--cidr` to specific known IP ranges.

---

## Summary

- **Amazon EC2** is the core IaaS compute service in AWS, providing on-demand virtual machines (instances) in the cloud.
- An EC2 instance is defined by its **AMI** (operating system template) and **instance type** (hardware size).
- **Security groups** act as stateful virtual firewalls; always restrict SSH access to known IPs.
- **Key pairs** provide secure SSH authentication; protect your `.pem` private key file at all times.
- **Elastic IPs** provide a stable public IP address that persists across instance stop/start cycles.
- The instance **lifecycle** includes Pending → Running → Stopped/Terminated; you are only billed per second while in the Running state.
- **Purchasing options** (On-Demand, Reserved, Spot) let you optimize costs based on workload predictability.
- EC2 is the foundation on which AWS managed services like **RDS** and, later in the week, database services like **DynamoDB** (serverless) are built or compared against.

> **Looking Ahead:** On Wednesday and Thursday, we will explore Amazon DynamoDB and Amazon Redshift — both of which abstract away the EC2 layer entirely. As you study those managed services, keep the EC2 mental model in mind: understanding what AWS is *hiding* for you is what makes managed services so valuable.

---

## Additional Resources

- [Amazon EC2 Documentation — Getting Started](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html)
- [AWS EC2 Instance Types Explorer](https://aws.amazon.com/ec2/instance-types/)
- [AWS Security Groups for EC2 (Official Guide)](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security-groups.html)
