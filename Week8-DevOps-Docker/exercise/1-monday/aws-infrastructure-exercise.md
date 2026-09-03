# Lab: Your First AWS Deployment

**Duration:** 3–4 hours
**Mode:** Individual (Hybrid — setup + implementation)
**Week:** 8, Monday — AWS Infrastructure Fundamentals

---

## Prerequisites

Before starting, confirm you have the following ready:

- An AWS account with an IAM user (do NOT use the root account)
- AWS CLI installed on your local machine (`aws --version` should respond)
- The Spring Boot JAR file provided by your instructor (e.g., `product-catalog-api.jar`)
- Postman installed locally

---

## Learning Objectives

By the end of this lab you will be able to:

- Launch and configure an EC2 instance using the AWS Management Console
- Create and apply Security Group rules to control inbound network access
- Connect to a remote Linux server via SSH using a PEM key pair
- Deploy a Spring Boot application onto an EC2 instance and verify it via HTTP
- Navigate Auto Scaling Groups and ECS in the AWS Console and explain their core concepts

---

## Scenario

You have just been hired as a junior DevOps engineer at a startup building a product catalog platform. The backend team has handed you a packaged Spring Boot JAR. Your first task is to deploy this API to AWS so the frontend team can begin integration testing against a live URL. You will provision the compute, lock down network access appropriately, deploy the app, and verify it is reachable — all by the end of today.

---

## Setup — Before You Begin

### 1. Configure the AWS CLI

Run the following command and fill in your IAM user credentials when prompted:

```bash
aws configure
```

You will be asked for:

| Prompt | What to enter |
|---|---|
| AWS Access Key ID | Your IAM access key (AWS Console → IAM → Your user → Security credentials) |
| AWS Secret Access Key | Your IAM secret key (shown only once at creation time) |
| Default region name | `us-east-1` |
| Default output format | `json` |

### 2. Verify Your Configuration

```bash
# Confirm the CLI can reach AWS and recognises your identity
aws sts get-caller-identity
```

Expected output (values will differ):

```json
{
    "UserId": "AIDARXXXXXXXXXXXXXXXX",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/your-iam-username"
}
```

If you see an error, re-run `aws configure` and double-check your keys.

### 3. Locate the Spring Boot JAR

Confirm the instructor-provided JAR is on your machine:

```bash
ls -lh ~/Downloads/product-catalog-api.jar
# Adjust the path if your instructor directed you to save it elsewhere
```

You will SCP this file to EC2 in Task 4.

---

## Core Tasks

---

### Task 1 — Launch an EC2 Instance (via Console)

**Goal:** Have a running Amazon Linux 2023 virtual machine in the cloud.

1. Open the [AWS Management Console](https://console.aws.amazon.com) and navigate to **EC2**.
2. Click **Launch instance**.
3. Fill in the configuration:

   | Field | Value |
   |---|---|
   | Name | `week8-product-api` |
   | AMI | Amazon Linux 2023 AMI (free tier eligible) |
   | Instance type | `t3.micro` |
   | Key pair | Click **Create new key pair** |
   | Key pair name | `week8-keypair` |
   | Key pair type | RSA |
   | Private key file format | `.pem` |

4. Click **Create key pair**. The `.pem` file downloads automatically. **Move it to a safe location — you cannot download it again.**

   ```bash
   mv ~/Downloads/week8-keypair.pem ~/.ssh/week8-keypair.pem
   ```

5. Under **Network settings**, click **Edit**, then choose **Create security group**:
   - Security group name: `week8-sg`
   - Description: `Week 8 exercise security group`
   - Leave the default SSH rule for now (you will tighten it in Task 2)

6. Under **Configure storage**, leave the default: **8 GiB gp3**.

7. Click **Launch instance**.

8. On the success screen, click the instance ID link. Wait until the **Instance state** column shows **running** (refresh every 30 seconds).

9. Record your instance's **Public IPv4 address** — you will use it throughout this lab:

   ```
   My EC2 Public IP: ___________________________
   ```

> **Checkpoint 1:** The EC2 dashboard shows your instance with state `running` and a green indicator. Screenshot this for your submission.

---

### Task 2 — Configure Security Group Rules

**Goal:** Allow SSH from only your IP, and allow API traffic on port 8080 from anywhere.

1. In the EC2 console, click **Security Groups** in the left sidebar.
2. Select `week8-sg`.
3. Click the **Inbound rules** tab, then **Edit inbound rules**.
4. Configure exactly two rules:

   | Type | Protocol | Port | Source | Reason |
   |---|---|---|---|---|
   | SSH | TCP | 22 | **My IP** (let AWS fill it in) | Limits shell access to your current IP only — dramatically reduces attack surface |
   | Custom TCP | TCP | 8080 | **Anywhere (0.0.0.0/0)** | Frontend team needs HTTP access from their machines |

5. Click **Save rules**.

**Why does this distinction matter?**

SSH gives full shell access to your server. If port 22 is open to the world (`0.0.0.0/0`), automated bots will attempt thousands of login attempts per hour — this is a real and constant phenomenon. Port 8080 serves HTTP responses only; no credentials are exposed, so broad access is acceptable in a development environment.

> **Checkpoint 2:** The Inbound rules tab for `week8-sg` shows exactly two rules: SSH restricted to your IP, and TCP 8080 open to 0.0.0.0/0.

---

### Task 3 — SSH into the Instance

**Goal:** Open a terminal session on your EC2 instance.

1. Protect your key file — SSH refuses to use a key that is too permissive:

   ```bash
   chmod 400 ~/.ssh/week8-keypair.pem
   ```

2. Connect:

   ```bash
   ssh -i ~/.ssh/week8-keypair.pem ec2-user@<YOUR-EC2-PUBLIC-IP>
   ```

3. When prompted `Are you sure you want to continue connecting?` type `yes`.

**Troubleshooting common SSH errors:**

| Error Message | Likely Cause | Fix |
|---|---|---|
| `Connection timed out` | Port 22 blocked in Security Group, or instance not yet running | Confirm the SSH inbound rule exists in `week8-sg`; wait for instance to fully start |
| `Permission denied (publickey)` | Wrong username or wrong key file | Amazon Linux uses `ec2-user`; confirm `-i` points to the correct `.pem` |
| `UNPROTECTED PRIVATE KEY FILE` | Key file permissions too open | Run `chmod 400 ~/.ssh/week8-keypair.pem` |
| `Host key verification failed` | Stale entry from a different instance at the same IP | Run `ssh-keygen -R <IP>` to clear the old entry |

> **Checkpoint 3:** You see the Amazon Linux welcome banner in your terminal, ending with a prompt like `[ec2-user@ip-172-xx-xx-xx ~]$`.

---

### Task 4 — Install Java and Deploy the Spring Boot App

**Goal:** Get the API running on port 8080 on your EC2 instance.

**Step A — Install Java on the instance (run inside your SSH session):**

```bash
# Update package index
sudo dnf update -y

# Install Amazon Corretto 17 (AWS's OpenJDK distribution)
sudo dnf install java-17-amazon-corretto -y

# Verify
java -version
```

Expected output:

```
openjdk version "17.x.x" ...
OpenJDK Runtime Environment Corretto-17.x.x.x ...
```

**Step B — Copy the JAR from your local machine to EC2 (run in a NEW local terminal, not inside SSH):**

```bash
scp -i ~/.ssh/week8-keypair.pem \
    ~/Downloads/product-catalog-api.jar \
    ec2-user@<YOUR-EC2-PUBLIC-IP>:~/product-catalog-api.jar
```

**Step C — Run the application (back in your SSH session):**

```bash
java -jar ~/product-catalog-api.jar
```

Leave this terminal open. Application logs stream here.

> **Checkpoint 4:** The console output includes a line similar to:
> ```
> Started ProductCatalogApplication in 4.321 seconds
> ```
> or
> ```
> Tomcat started on port(s): 8080 (http)
> ```

---

### Task 5 — Verify via Postman

**Goal:** Confirm the deployed API responds correctly over the public internet.

1. Open Postman on your local machine.
2. Create a new GET request:

   ```
   GET http://<YOUR-EC2-PUBLIC-IP>:8080/health
   ```

3. Click **Send**. Expected response:

   ```json
   { "status": "UP" }
   ```

   Expected HTTP status: `200 OK`

4. Test at least two more endpoints (adjust paths to match the actual API your instructor provided):

   | Method | URL | Expected Status |
   |---|---|---|
   | GET | `http://<EC2-IP>:8080/api/products` | 200 |
   | GET | `http://<EC2-IP>:8080/api/products/1` | 200 |
   | POST | `http://<EC2-IP>:8080/api/products` (with JSON body) | 201 |

5. Save a screenshot of Postman showing the 200 OK from `/health`.

> **Checkpoint 5:** Postman shows a 200 OK from your EC2's public IP on port 8080. Screenshot saved.

---

### Task 6 — Explore Auto Scaling in Console

**Goal:** Understand where Auto Scaling lives in the console and what its key settings mean.

> **Do NOT create an Auto Scaling group — this is a conceptual walkthrough only.**

1. In the EC2 console left sidebar, scroll to **Auto Scaling** → **Auto Scaling Groups**.
2. Click **Create Auto Scaling group** and walk through the wizard:

   | Setting | What It Controls |
   |---|---|
   | Launch template | AMI, instance type, and key pair that new instances will use |
   | Desired capacity | How many instances AWS tries to maintain right now |
   | Minimum capacity | The floor — ASG never goes below this count |
   | Maximum capacity | The ceiling — ASG never exceeds this count |
   | Scaling policies | Rules that trigger scale-out (add) or scale-in (remove) |

3. On the **Configure group size and scaling policies** page, note the three policy types:
   - **Target tracking** — maintain a target metric (e.g., keep CPU at 50%)
   - **Step scaling** — add/remove a specific count of instances at defined thresholds
   - **Scheduled scaling** — scale at a set time (e.g., more capacity every weekday at 9 AM)

4. Click **Cancel** — do not create the group.

> **Checkpoint 6:** Screenshot of the Auto Scaling wizard page showing the Minimum, Desired, and Maximum capacity fields.

---

### Task 7 — Explore ECS in Console

**Goal:** Understand ECS structure and the difference between Fargate and EC2 launch types.

> **Do NOT create a cluster or task definition — cost awareness.**

1. In the AWS Console search bar, type **ECS** and open Elastic Container Service.
2. Click **Clusters** → **Create cluster** and compare the options:

   | Option | What It Means |
   |---|---|
   | AWS Fargate (serverless) | AWS manages the underlying servers; you pay per task per second |
   | Amazon EC2 instances | You manage a pool of EC2s; containers run on those machines |

3. Click **Cancel**.
4. Click **Task Definitions** → **Create new task definition** and observe:
   - A task definition is a blueprint for a container (image URL, CPU, memory, ports, environment variables)
   - Each revision is immutable
   - Services use task definitions to run and maintain a desired count of running tasks

5. Click **Cancel**.

> **Checkpoint 7:** Screenshot of the Task Definition creation page showing the container configuration section.

---

## Bonus Challenges (Optional)

---

### Bonus A — List Running Instances via CLI

```bash
aws ec2 describe-instances \
    --filters "Name=instance-state-name,Values=running" \
    --query "Reservations[*].Instances[*].{ID:InstanceId,Type:InstanceType,IP:PublicIpAddress,State:State.Name}" \
    --output table
```

Your `week8-product-api` instance should appear in the output.

---

### Bonus B — Upload the JAR to S3

```bash
# Create a bucket (names must be globally unique)
aws s3 mb s3://week8-artifacts-yourname-2026

# Upload
aws s3 cp ~/Downloads/product-catalog-api.jar \
    s3://week8-artifacts-yourname-2026/week8/product-catalog-api.jar

# Confirm
aws s3 ls s3://week8-artifacts-yourname-2026/week8/
```

---

### Bonus C — Run the App as a systemd Service

This ensures the app restarts automatically if it crashes or the instance reboots:

```bash
sudo tee /etc/systemd/system/product-catalog.service << 'EOF'
[Unit]
Description=Product Catalog Spring Boot API
After=network.target

[Service]
User=ec2-user
ExecStart=/usr/bin/java -jar /home/ec2-user/product-catalog-api.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable product-catalog
sudo systemctl start product-catalog
sudo systemctl status product-catalog
```

---

## IMPORTANT — Cost Cleanup

> **STOP your EC2 instance when you finish this lab.**

A t3.micro costs roughly $0.01/hour. Left running over a weekend, that accumulates unnecessarily.

**Stop via console:** EC2 → Instances → Select instance → Instance state → **Stop instance**

**Stop via CLI:**

```bash
# Replace with your actual instance ID (from the console or Task 1)
aws ec2 stop-instances --instance-ids i-xxxxxxxxxxxxxxxxx
```

> STOP pauses the instance and preserves its data. TERMINATE deletes it permanently.
> Only terminate when you are certain you no longer need the instance or its storage.

**Other resources to review:**
- S3 buckets created in Bonus B (delete when no longer needed)
- Key pairs — free, no action required
- Security groups — free when not attached to a running instance

---

## Definition of Done

- [ ] EC2 instance `week8-product-api` is running and accessible via SSH
- [ ] Security Group `week8-sg` has SSH (22) restricted to your IP and TCP 8080 open to 0.0.0.0/0
- [ ] Spring Boot app responds to `GET /health` with HTTP 200 and `{"status":"UP"}`
- [ ] Postman screenshot saved showing the 200 response from the EC2 public IP
- [ ] Auto Scaling wizard explored in console (screenshot saved, no group created)
- [ ] ECS Task Definition page explored in console (screenshot saved, no resources created)
- [ ] EC2 instance STOPPED after completing the exercise

---

## Reflection Questions

Answer these in your own words before your next session:

1. **Security Groups vs. Firewalls:** A Security Group in AWS is described as "stateful." What does stateful mean in this context, and how does it differ from a stateless firewall rule?

2. **Auto Scaling Trade-offs:** You set a minimum of 2 and a maximum of 10 in an Auto Scaling group. Under what real-world conditions would you choose a minimum greater than 1, even during off-peak hours?

3. **Fargate vs. EC2 Launch Type:** If you were deploying a containerised app for a startup with unpredictable traffic and a small ops team, which ECS launch type would you choose and why?
