# AWS Security Groups — Virtual Firewalls for EC2

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what a security group is and how it controls network traffic
- Distinguish between inbound and outbound rules
- Understand why security groups are stateful and what that means in practice
- Read and write CIDR notation for IP ranges
- Configure security groups for common scenarios: SSH, HTTP/HTTPS, and Spring Boot

---

## Why This Matters

No server should be open to the entire internet by default. Security groups are the first line of defense that controls who can reach your EC2 instances and on which ports. When your SSH connection times out or your Spring Boot app is unreachable from a browser, the security group is almost always the cause. Understanding them deeply saves hours of debugging and is essential for building production-ready systems this week.

---

## What Is a Security Group?

A **security group** acts as a virtual firewall for your EC2 instance. It controls inbound (incoming) and outbound (outgoing) network traffic at the instance level.

### Key Characteristics

| Property | Value |
|---|---|
| **Scope** | Applied at the instance (network interface) level |
| **Type** | Stateful firewall |
| **Default inbound** | All traffic denied (whitelist model) |
| **Default outbound** | All traffic allowed |
| **Multiple per instance** | Yes — up to 5 security groups per instance |
| **Multiple instances per group** | Yes — one security group can protect many instances |

### Analogy

Think of a security group as a **bouncer at a nightclub**:
- The inbound rules are the guest list — only traffic that matches a rule gets in.
- The outbound rules control what guests can take when they leave.
- If you are not on the list (no matching rule), you are turned away — no exception.

---

## Inbound vs. Outbound Rules

### Inbound Rules

Control what traffic can *reach* your instance from the outside world.

Each inbound rule defines:
- **Type/Protocol:** The network protocol (TCP, UDP, ICMP, or All)
- **Port range:** The destination port (e.g., 22 for SSH, 80 for HTTP)
- **Source:** Who is allowed to send traffic — an IP address, a CIDR block, or another security group

### Outbound Rules

Control what traffic your instance can *send* to the outside world.

By default, all outbound traffic is allowed. This means your instance can reach the internet to download packages, call external APIs, etc. You can restrict this for high-security environments, but it is rarely necessary in development.

---

## Stateful Nature — The Critical Detail

Security groups are **stateful**. This means:

> If you allow an inbound connection, the response traffic is automatically allowed — even if there is no matching outbound rule.

### Example: HTTP Request

```
Browser (internet) ─── TCP SYN ──────────────────────► EC2 (port 80)
                                    [Inbound rule: port 80 allowed]
                                    [State table: records this connection]

Browser (internet) ◄── HTTP Response ─────────────────── EC2 (port 80)
                                    [No outbound rule needed — stateful!]
                                    [State table: response is part of allowed connection]
```

This differs from a **stateless** firewall (like AWS Network ACLs), where you must explicitly allow both inbound request AND outbound response.

### Practical Impact

Because security groups are stateful, you typically only need to configure inbound rules. The return traffic takes care of itself.

---

## CIDR Notation

**CIDR (Classless Inter-Domain Routing)** notation specifies a range of IP addresses. It is how security group rules define which IPs are allowed.

### Format

```
<IP address>/<prefix length>
```

The prefix length (the number after `/`) determines how many IP addresses are in the range.

### Common CIDR Ranges

| CIDR | IP Range | Number of Addresses | Use Case |
|---|---|---|---|
| `0.0.0.0/0` | 0.0.0.0 – 255.255.255.255 | All IPv4 addresses | Allow anyone (internet) |
| `::/0` | All IPv6 addresses | All IPv6 addresses | Allow anyone (IPv6) |
| `203.0.113.25/32` | 203.0.113.25 only | 1 (exact IP) | Your specific machine |
| `10.0.0.0/8` | 10.0.0.0 – 10.255.255.255 | 16,777,216 | Entire private VPC range |
| `172.31.0.0/16` | 172.31.0.0 – 172.31.255.255 | 65,536 | Default VPC range |
| `192.168.1.0/24` | 192.168.1.0 – 192.168.1.255 | 256 | Home/office subnet |

### How to Read CIDR

The prefix length tells you how many bits of the address are *fixed*. The remaining bits can vary.

- `/32` — all 32 bits fixed → exactly 1 address
- `/24` — 24 bits fixed → last 8 bits vary → 256 addresses
- `/16` — 16 bits fixed → last 16 bits vary → 65,536 addresses
- `/0` — 0 bits fixed → all bits vary → all addresses

### Finding Your IP for SSH Rules

```bash
# On Linux/macOS — find your current public IP
curl -s https://checkip.amazonaws.com

# Use that IP with /32 for a single-host rule
# Example: 203.0.113.25/32
```

---

## Security Group Rule Components

| Field | Description | Example |
|---|---|---|
| **Type** | Preset protocol+port combinations | SSH, HTTP, HTTPS, Custom TCP |
| **Protocol** | TCP, UDP, ICMP, or All | TCP |
| **Port range** | Single port or range | 22 or 8080-8090 |
| **Source (inbound)** | IP/CIDR or another security group | 0.0.0.0/0 or sg-0abc123 |
| **Destination (outbound)** | IP/CIDR or another security group | 0.0.0.0/0 |
| **Description** | Optional human-readable note | "SSH from office" |

---

## Common Security Group Configurations

### 1. SSH Access (Port 22)

```
Inbound Rule:
  Type:     SSH
  Protocol: TCP
  Port:     22
  Source:   <your-ip>/32    ← RECOMMENDED: restrict to your IP
            or 0.0.0.0/0    ← Open to anyone — only for temporary debugging
```

> **Security Best Practice:** Never leave port 22 open to `0.0.0.0/0` in production. Automated bots constantly scan the internet for open port 22 and attempt brute-force logins. Use `<your-ip>/32` or, better, use EC2 Instance Connect / AWS Systems Manager Session Manager which require no open port 22 at all.

### 2. HTTP Web Traffic (Port 80)

```
Inbound Rule:
  Type:     HTTP
  Protocol: TCP
  Port:     80
  Source:   0.0.0.0/0    ← Allow anyone (public website)
```

### 3. HTTPS Web Traffic (Port 443)

```
Inbound Rule:
  Type:     HTTPS
  Protocol: TCP
  Port:     443
  Source:   0.0.0.0/0
```

### 4. Spring Boot Application (Port 8080)

```
Inbound Rule:
  Type:     Custom TCP
  Protocol: TCP
  Port:     8080
  Source:   0.0.0.0/0    ← For public APIs
            or <your-ip>/32  ← For development/testing
```

### 5. All Traffic Within a VPC (Internal Services)

When EC2 instances need to communicate with each other (e.g., app server talking to a database), use a security group as the source instead of an IP range:

```
Inbound Rule (on the database server's security group):
  Type:     Custom TCP
  Protocol: TCP
  Port:     5432    (PostgreSQL)
  Source:   sg-0abc123def456789   ← The app server's security group

This means: "Allow port 5432 connections from any instance that belongs to the app server security group"
```

This approach is more flexible than IP-based rules because it works even as instances scale up/down and their IPs change.

---

## Creating a Security Group via CLI

```bash
# Step 1: Create the security group
aws ec2 create-security-group \
  --group-name myapp-sg \
  --description "Security group for myapp Spring Boot server" \
  --vpc-id vpc-0abc123def456789

# The command returns the new security group ID:
# { "GroupId": "sg-0123456789abcdef0" }

# Step 2: Add an inbound rule for SSH (port 22) from your IP only
aws ec2 authorize-security-group-ingress \
  --group-id sg-0123456789abcdef0 \
  --protocol tcp \
  --port 22 \
  --cidr 203.0.113.25/32

# Step 3: Add an inbound rule for HTTP (port 80) from anyone
aws ec2 authorize-security-group-ingress \
  --group-id sg-0123456789abcdef0 \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Step 4: Add an inbound rule for Spring Boot (port 8080)
aws ec2 authorize-security-group-ingress \
  --group-id sg-0123456789abcdef0 \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# View all rules on the security group
aws ec2 describe-security-groups \
  --group-ids sg-0123456789abcdef0
```

---

## Removing a Security Group Rule

```bash
# Remove the SSH rule (revoke = remove inbound permission)
aws ec2 revoke-security-group-ingress \
  --group-id sg-0123456789abcdef0 \
  --protocol tcp \
  --port 22 \
  --cidr 203.0.113.25/32
```

---

## Security Groups vs. Network ACLs

Security groups are not the only network control mechanism in AWS. Network ACLs (NACLs) operate at the subnet level and are stateless. Here is a quick comparison:

| Feature | Security Group | Network ACL |
|---|---|---|
| **Scope** | Instance level | Subnet level |
| **Stateful?** | Yes | No (must allow return traffic) |
| **Rules** | Allow only (whitelist) | Allow and Deny |
| **Evaluation** | All rules evaluated | Rules evaluated in order (numbered) |
| **Default** | All inbound denied; all outbound allowed | All traffic allowed (default NACL) |
| **Use for** | Instance-level control | Subnet-level broad controls |

For most applications, security groups alone are sufficient. NACLs are used for additional defense-in-depth in highly regulated environments.

---

## Summary

- Security groups are stateful virtual firewalls applied at the instance level.
- Inbound rules are deny-all by default — you must explicitly allow traffic.
- Outbound rules allow all by default — you rarely need to modify them.
- Stateful means return traffic for allowed connections is automatically permitted.
- CIDR notation specifies IP ranges: `/32` = single IP, `/0` = all IPs.
- Common ports: 22 (SSH), 80 (HTTP), 443 (HTTPS), 8080 (Spring Boot).
- Use another security group as a source to allow traffic between AWS resources.
- Never open port 22 to `0.0.0.0/0` in production.

---

## External Resources

- [Security Groups for EC2 — AWS Documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security-groups.html)
- [CIDR Notation Explained (Visual Guide)](https://cidr.xyz/)
- [AWS Network ACLs vs Security Groups](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-network-acls.html)
