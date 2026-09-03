# Amazon EBS — Elastic Block Store

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what EBS is and how it differs from other AWS storage options
- Choose the appropriate EBS volume type for a given workload
- Attach and detach EBS volumes to EC2 instances
- Create and restore from EBS snapshots
- Enable encryption at rest on EBS volumes
- Select the right storage service (EBS vs EFS vs S3) for a use case

---

## Why This Matters

Your EC2 instance needs persistent storage. The built-in storage on a stopped or terminated instance can disappear. EBS provides the durable, high-performance disk storage that backs your databases, application files, and operating system. Understanding EBS is essential for designing systems that do not lose data — a core requirement of any production architecture you will build this week.

---

## What Is EBS?

**Amazon Elastic Block Store (EBS)** provides persistent block-level storage volumes for use with EC2 instances. Think of an EBS volume as a **hard drive in the cloud** that you can attach to your EC2 instance.

### Key Properties

| Property | Value |
|---|---|
| **Persistence** | Data persists independently of the EC2 instance lifecycle |
| **Availability Zone** | Bound to a single AZ (e.g., us-east-1a) |
| **Attachment** | One EBS volume → one EC2 instance at a time (with exceptions for multi-attach) |
| **Resizable** | Volume size and type can be modified without downtime |
| **Snapshots** | Point-in-time backups stored in S3 |

### Analogy

EBS is like an **external USB hard drive**. You plug it into your computer (attach to EC2), read and write files, and unplug it (detach). The data stays on the drive even when it is not plugged in. You can plug it into a different computer (different EC2 instance) and all your data is still there.

---

## EBS Volume Types

AWS offers several volume types optimized for different performance profiles and cost points.

### SSD-Backed Volumes (Low Latency, Random I/O)

| Type | Name | IOPS | Throughput | Best For |
|---|---|---|---|---|
| **gp3** | General Purpose SSD v3 | Up to 16,000 | Up to 1,000 MB/s | Default choice for most workloads |
| **gp2** | General Purpose SSD v2 | Up to 16,000 (burst) | Up to 250 MB/s | Older default; prefer gp3 |
| **io2 Block Express** | Provisioned IOPS SSD | Up to 256,000 | Up to 4,000 MB/s | Critical databases requiring guaranteed IOPS |
| **io1** | Provisioned IOPS SSD | Up to 64,000 | Up to 1,000 MB/s | High-performance databases |

### HDD-Backed Volumes (High Throughput, Sequential I/O)

| Type | Name | Throughput | Best For |
|---|---|---|---|
| **st1** | Throughput Optimized HDD | Up to 500 MB/s | Big data, log processing, data warehouses |
| **sc1** | Cold HDD | Up to 250 MB/s | Infrequently accessed data, cheapest storage |

### Choosing the Right Volume Type

| Scenario | Recommended Volume |
|---|---|
| OS root volume, general application storage | gp3 |
| MySQL/PostgreSQL database | gp3 (or io2 for high traffic) |
| High-traffic database requiring guaranteed IOPS | io2 |
| Kafka log storage, Hadoop, big data | st1 |
| Archival/cold storage, rarely accessed data | sc1 |

### gp3 vs gp2: Why gp3 is the Modern Default

| | gp2 | gp3 |
|---|---|---|
| IOPS | Tied to volume size (3 IOPS/GB) | Fixed baseline 3,000 IOPS regardless of size |
| Throughput | Up to 250 MB/s | Up to 1,000 MB/s |
| Pricing | Higher | ~20% cheaper |
| IOPS customization | No | Yes (up to 16,000 independently) |

Always choose **gp3** for new volumes unless you have a specific reason to use another type.

---

## Attaching an EBS Volume to an EC2 Instance

### Step 1: Create a New EBS Volume

```bash
aws ec2 create-volume \
  --volume-type gp3 \              # Volume type
  --size 20 \                       # Size in GiB
  --availability-zone us-east-1a \ # MUST match the instance's AZ
  --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=myapp-data}]'

# Returns:
# {
#   "VolumeId": "vol-0abc123def456789",
#   "State": "creating",
#   "AvailabilityZone": "us-east-1a",
#   "Size": 20,
#   "VolumeType": "gp3"
# }
```

> **Critical:** The EBS volume must be in the **same Availability Zone** as the EC2 instance you want to attach it to. You cannot attach a volume from us-east-1a to an instance in us-east-1b.

### Step 2: Attach the Volume

```bash
aws ec2 attach-volume \
  --volume-id vol-0abc123def456789 \
  --instance-id i-0abcd1234efgh5678 \
  --device /dev/sdf              # Device name visible inside the instance
```

### Step 3: Format and Mount the Volume (Inside the Instance)

After attaching, SSH into the instance and prepare the volume:

```bash
# List all block devices (find your new volume)
lsblk
# OUTPUT:
# NAME    MAJ:MIN RM  SIZE RO TYPE MOUNTPOINT
# xvda    202:0    0    8G  0 disk
# └─xvda1 202:1    0    8G  0 part /
# xvdf    202:80   0   20G  0 disk     ← Your new volume (no partition, no mount)

# Check if the volume already has a filesystem (new volumes do not)
sudo file -s /dev/xvdf
# OUTPUT: /dev/xvdf: data  ← "data" means no filesystem; safe to format

# Create an ext4 filesystem on the volume
sudo mkfs -t ext4 /dev/xvdf

# Create a mount point directory
sudo mkdir -p /data

# Mount the volume
sudo mount /dev/xvdf /data

# Verify it is mounted
df -h /data
# OUTPUT:
# Filesystem      Size  Used Avail Use% Mounted on
# /dev/xvdf        20G   45M   19G   1% /data
```

### Step 4: Mount Automatically on Reboot

Without this step, the volume will not remount after a reboot:

```bash
# Get the UUID of the volume (more reliable than device name which can change)
sudo blkid /dev/xvdf
# OUTPUT: /dev/xvdf: UUID="abc123-def456-..." TYPE="ext4"

# Add an entry to /etc/fstab
echo 'UUID=abc123-def456-...  /data  ext4  defaults,nofail  0  2' | sudo tee -a /etc/fstab

# Test the fstab entry without rebooting
sudo mount -a
```

---

## Detaching an EBS Volume

Before detaching, unmount the filesystem from inside the instance:

```bash
# Inside the EC2 instance: unmount the volume
sudo umount /data

# From your local machine: detach the volume
aws ec2 detach-volume --volume-id vol-0abc123def456789

# Wait for it to be available
aws ec2 wait volume-available --volume-ids vol-0abc123def456789
```

---

## EBS Snapshots

A **snapshot** is a point-in-time backup of an EBS volume. Snapshots are stored in S3 (managed by AWS) and are incremental — only changed blocks are stored in subsequent snapshots, making them storage-efficient.

### Creating a Snapshot

```bash
# Create a snapshot with a description
aws ec2 create-snapshot \
  --volume-id vol-0abc123def456789 \
  --description "myapp-data backup before v2.0 deployment"

# Returns the snapshot ID: snap-0abc123def456789
```

### Viewing Your Snapshots

```bash
aws ec2 describe-snapshots \
  --owner-ids self \                        # Only your account's snapshots
  --query 'Snapshots[*].[SnapshotId,Description,StartTime,State]' \
  --output table
```

### Restoring from a Snapshot (Creating a New Volume)

```bash
# Create a new EBS volume from an existing snapshot
aws ec2 create-volume \
  --snapshot-id snap-0abc123def456789 \
  --volume-type gp3 \
  --availability-zone us-east-1a

# Attach and mount the new volume as described above
```

### Snapshot Backup Strategy

For production systems, automate regular snapshots:

```bash
# Create a snapshot automatically via Data Lifecycle Manager (DLM)
# In the AWS Console: EC2 → Elastic Block Store → Lifecycle Manager → Create policy
# Set schedule: daily at 2:00 AM, retain last 7 snapshots
```

A simple manual backup loop (for development):

```bash
#!/bin/bash
# backup.sh — run this daily via cron

VOLUME_ID="vol-0abc123def456789"
DATE=$(date +%Y-%m-%d)

aws ec2 create-snapshot \
  --volume-id "$VOLUME_ID" \
  --description "automated-backup-$DATE"

echo "Snapshot created for $DATE"
```

---

## Encryption at Rest

EBS volumes can be encrypted at rest using AWS-managed or customer-managed KMS keys. Encryption protects data if the physical hardware is ever compromised.

### Enable Encryption on a New Volume

```bash
aws ec2 create-volume \
  --volume-type gp3 \
  --size 20 \
  --availability-zone us-east-1a \
  --encrypted \                          # Enable encryption
  --kms-key-id alias/aws/ebs             # Use the AWS-managed EBS key (default)
```

### Encrypt an Existing Unencrypted Volume

You cannot directly encrypt an existing volume. The process is:

```
Unencrypted Volume
       │
       │  1. Create snapshot
       ▼
Unencrypted Snapshot
       │
       │  2. Copy snapshot with encryption enabled
       ▼
Encrypted Snapshot
       │
       │  3. Create new volume from encrypted snapshot
       ▼
Encrypted Volume  ──► Attach to EC2 instance
```

```bash
# Step 1: Snapshot the existing volume
aws ec2 create-snapshot \
  --volume-id vol-0abc123def456789 \
  --description "pre-encryption snapshot"

# Step 2: Copy the snapshot with encryption
aws ec2 copy-snapshot \
  --source-region us-east-1 \
  --source-snapshot-id snap-0abc123 \
  --encrypted \
  --description "encrypted copy"

# Step 3: Create a new volume from the encrypted snapshot
aws ec2 create-volume \
  --snapshot-id snap-0xyz789 \
  --volume-type gp3 \
  --availability-zone us-east-1a
```

### Enable Default Encryption for All New Volumes

```bash
# All new EBS volumes in the region will be encrypted by default
aws ec2 enable-ebs-encryption-by-default
```

---

## EBS vs EFS vs S3 — Storage Service Comparison

This is one of the most common architecture decision points for AWS beginners.

| Feature | EBS | EFS | S3 |
|---|---|---|---|
| **Type** | Block storage | File storage (NFS) | Object storage |
| **Access** | One EC2 instance at a time | Many EC2 instances simultaneously | Any client via HTTP |
| **Use with** | EC2 only | EC2, Lambda, ECS | Anything |
| **Protocol** | Block device (mounted as disk) | NFSv4 | REST API / SDK |
| **Latency** | Sub-millisecond | Low (slightly higher than EBS) | Milliseconds to seconds |
| **Scalability** | Fixed size (resize manually) | Automatically grows/shrinks | Virtually unlimited |
| **Persistence** | Tied to AZ | Multi-AZ | Multi-AZ / Multi-Region |
| **Price** | ~$0.08/GB/month (gp3) | ~$0.30/GB/month | ~$0.023/GB/month |
| **Best for** | OS root volumes, databases | Shared config files, CMS media | Backups, static files, logs, images |

### Decision Guide

```
Need disk for EC2 OS or database?
  → EBS

Multiple EC2 instances need to share the same files?
  → EFS

Storing files that are accessed via HTTP (images, video, static assets)?
  → S3

Backing up data infrequently?
  → S3 (or S3 Glacier for archival)
```

---

## Summary

- EBS provides persistent block storage volumes that outlive EC2 instances.
- gp3 is the default and most cost-effective SSD volume type; io2 is for high-IOPS databases.
- Volumes must be in the same AZ as the instance they attach to.
- After attaching, format the volume and add it to `/etc/fstab` for persistence across reboots.
- Snapshots are incremental backups stored in S3; use them to restore or create new volumes.
- Encrypt new volumes with `--encrypted`; encrypting existing volumes requires a snapshot copy workflow.
- EBS = single-instance disk; EFS = shared file system; S3 = object/file store via HTTP.

---

## External Resources

- [Amazon EBS Volume Types](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-volume-types.html)
- [EBS Snapshots Documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSSnapshots.html)
- [Comparing EBS, EFS, and S3](https://aws.amazon.com/blogs/storage/confused-by-aws-storage-options-s3-ebs-efs-explained/)
