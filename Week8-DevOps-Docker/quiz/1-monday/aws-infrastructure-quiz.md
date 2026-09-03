# Weekly Knowledge Check: Week 8 — AWS Infrastructure & Cloud Fundamentals
**Day:** Monday | **Topics:** AWS CLI · EC2 · AMIs · Security Groups · EBS · Auto Scaling · ECS · API Gateway · Containerization

---

## Part 1: Multiple Choice

**Question 1.** When running `aws configure`, which prompt/flag sets the default **output format** for all CLI responses?

A) `--format`
B) `--output`
C) `--response-type`
D) `--mode`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `--output`

**Explanation:** During `aws configure`, the CLI prompts for four values: AWS Access Key ID, AWS Secret Access Key, Default region name, and **Default output format**. The format (json, yaml, text, table) is stored in `~/.aws/config` and can be overridden per-command with `--output`.

- **Why A is wrong:** `--format` is not a valid AWS CLI global option.
- **Why C is wrong:** `--response-type` does not exist in the AWS CLI.
- **Why D is wrong:** `--mode` is not a recognized AWS CLI configuration flag.
</details>

---

**Question 2.** An Amazon Machine Image (AMI) is best described as:

A) A running virtual server instance in AWS
B) A snapshot template containing the OS, application code, and configuration used to launch EC2 instances
C) A managed container image stored in Amazon ECR
D) A virtual network interface attached to an EC2 instance

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) A snapshot template containing the OS, application code, and configuration used to launch EC2 instances

**Explanation:** An AMI is a pre-configured image that includes the root volume snapshot, launch permissions, and block device mapping. It acts as a blueprint — you launch one or more identical EC2 instances from a single AMI, enabling consistent, repeatable deployments.

- **Why A is wrong:** A running virtual server is an EC2 *instance*, not an AMI.
- **Why C is wrong:** Amazon ECR stores Docker/OCI container images, which are a completely different resource type.
- **Why D is wrong:** A virtual network interface is an Elastic Network Interface (ENI), a separate AWS resource.
</details>

---

**Question 3.** You need to SSH into an EC2 instance using a `.pem` key file. Before connecting you run `chmod 400 key.pem`. What is the primary reason this step is required?

A) It grants the EC2 instance permission to accept the key
B) It encrypts the private key file before transmission over the network
C) It restricts the key file to read-only by the owner, satisfying SSH's requirement that private keys not be publicly accessible
D) It converts the key from RSA format to Ed25519 format

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) It restricts the key file to read-only by the owner, satisfying SSH's requirement that private keys not be publicly accessible

**Explanation:** The `ssh` client checks permissions on private key files before use. If the file is readable by group or others, SSH refuses with "WARNING: UNPROTECTED PRIVATE KEY FILE!" — `chmod 400` sets owner-read-only, passing this check.

- **Why A is wrong:** Local file permissions have no effect on the EC2 instance's acceptance logic.
- **Why B is wrong:** `chmod` changes access permissions on disk; it does not encrypt file contents.
- **Why D is wrong:** `chmod` has nothing to do with key algorithms or formats.
</details>

---

**Question 4.** AWS Security Groups are described as "stateful." Which statement best explains what stateful means in this context?

A) Security group rules are automatically replicated across all AWS Regions
B) If you allow inbound traffic on a port, the corresponding return/response traffic is automatically permitted outbound without a separate outbound rule
C) Security groups remember the last 1,000 connections and block any new connections beyond that limit
D) All security group changes take effect only after the EC2 instance is rebooted

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) If you allow inbound traffic on a port, the corresponding return/response traffic is automatically permitted outbound without a separate outbound rule

**Explanation:** Stateful firewalls track connection state. For Security Groups, an allowed inbound TCP connection's response packets are automatically permitted outbound — and vice versa. This contrasts with Network ACLs (NACLs), which are stateless and require explicit rules in both directions.

- **Why A is wrong:** Security groups are VPC-scoped; cross-Region replication is a different concept entirely.
- **Why C is wrong:** AWS Security Groups have no connection count memory limit of this kind.
- **Why D is wrong:** Security group rule changes take effect immediately — no reboot required.
</details>

---

**Question 5.** You are provisioning an EBS volume for a high-performance transactional database requiring consistent sub-millisecond latency and up to 64,000 IOPS. Which volume type is correct?

A) gp2 (General Purpose SSD)
B) gp3 (General Purpose SSD)
C) io2 Block Express (Provisioned IOPS SSD)
D) st1 (Throughput Optimized HDD)

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) io2 Block Express (Provisioned IOPS SSD)

**Explanation:** io2 Block Express is designed for the most demanding I/O-intensive workloads — up to 256,000 IOPS, sub-millisecond latency, and 99.999% durability. It is the correct choice for large relational databases (Oracle, SQL Server) requiring consistent high IOPS.

- **Why A is wrong:** gp2 maxes out at 16,000 IOPS and does not offer provisioned, consistent performance.
- **Why B is wrong:** gp3 allows up to 16,000 IOPS — good for many workloads but insufficient for 64,000 IOPS requirements.
- **Why D is wrong:** st1 is a magnetic HDD optimized for high sequential throughput (log processing), not low-latency random I/O.
</details>

---

**Question 6.** In Amazon ECS, what is the key operational difference between the **Fargate** launch type and the **EC2** launch type?

A) Fargate supports Docker containers; EC2 launch type only supports virtual machines
B) Fargate is serverless — AWS manages the underlying infrastructure; with the EC2 launch type you provision and manage the cluster EC2 instances yourself
C) The EC2 launch type automatically scales to zero when idle; Fargate does not
D) Fargate only runs Windows containers; EC2 launch type runs Linux containers

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Fargate is serverless — AWS manages the underlying infrastructure; with the EC2 launch type you provision and manage the cluster EC2 instances yourself

**Explanation:** With Fargate, you define CPU/memory at the task level and AWS provisions, patches, and scales the underlying compute invisibly. With the EC2 launch type, you register EC2 instances into your ECS cluster — more control (instance type, GPU, custom AMIs) but more operational responsibility.

- **Why A is wrong:** Both launch types run Docker containers; the difference is in infrastructure management.
- **Why C is wrong:** EC2 launch type instances continue running (and billing) even when idle unless explicitly terminated.
- **Why D is wrong:** Both Fargate and EC2 launch types support Linux and Windows containers.
</details>

---

**Question 7.** In EC2 Auto Scaling, what is the difference between a **Target Tracking** policy and a **Step Scaling** policy?

A) Target Tracking scales only on CPU; Step Scaling can use any CloudWatch metric
B) Target Tracking automatically adjusts capacity to maintain a specific metric value (e.g., 60% CPU utilization); Step Scaling defines discrete scaling adjustments triggered at specific alarm thresholds
C) Step Scaling is the newer preferred policy; Target Tracking is deprecated
D) Target Tracking requires manual approval to apply scaling actions; Step Scaling is fully automatic

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Target Tracking automatically adjusts capacity to maintain a specific metric value; Step Scaling defines discrete scaling adjustments triggered at specific alarm thresholds

**Explanation:** Target Tracking works like a thermostat — you set a target (e.g., keep average CPU at 50%) and Auto Scaling continuously adjusts instance count to maintain it. Step Scaling fires CloudWatch alarms at defined thresholds and applies specific adjustments (e.g., "add 2 instances if CPU > 70%, add 4 if CPU > 90%").

- **Why A is wrong:** Target Tracking supports many predefined metrics (ALB request count, custom metrics) beyond CPU.
- **Why C is wrong:** Both are currently supported; Target Tracking is actually the recommended modern approach for most use cases.
- **Why D is wrong:** Both policy types are fully automatic; no manual intervention is required.
</details>

---

**Question 8.** Which statement best describes the architectural difference between **Containers** and **Virtual Machines (VMs)**?

A) Containers virtualize hardware; VMs share the host OS kernel
B) Containers share the host OS kernel and isolate processes in user space; VMs emulate full hardware and run a complete guest OS per instance
C) VMs start in milliseconds; containers take minutes to boot because they include a full OS
D) Containers require a hypervisor to run; VMs use a container runtime like Docker

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) Containers share the host OS kernel and isolate processes in user space; VMs emulate full hardware and run a complete guest OS per instance

**Explanation:** Containers use Linux namespaces and cgroups to isolate applications at the process level while sharing the host kernel — making them lightweight and fast to start (milliseconds). VMs use a hypervisor to virtualize hardware; each VM boots its own full OS, providing stronger isolation at the cost of more resource overhead.

- **Why A is wrong:** This reverses the definitions — VMs virtualize hardware; containers share the kernel.
- **Why C is wrong:** Containers start in milliseconds; VMs take longer due to full OS boot time.
- **Why D is wrong:** Containers use a container runtime (Docker, containerd); VMs use a hypervisor (KVM, VMware).
</details>

---

**Question 9.** In AWS API Gateway, which integration type allows API Gateway to call an AWS service (e.g., DynamoDB, SQS) **directly**, without an intermediate Lambda function?

A) HTTP_PROXY
B) MOCK
C) AWS
D) HTTP

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) AWS

**Explanation:** The `AWS` integration type enables API Gateway to call AWS service APIs directly using mapping templates. For example, you can map a REST endpoint directly to DynamoDB `PutItem` or SQS `SendMessage` — no Lambda required, reducing latency and cost.

- **Why A is wrong:** HTTP_PROXY passes requests to an external HTTP endpoint with no transformation; it cannot call AWS service APIs natively.
- **Why B is wrong:** MOCK returns a static predefined response from API Gateway itself — useful for testing with no backend.
- **Why D is wrong:** HTTP integration calls an external HTTP endpoint with request/response mapping, but is not used for native AWS service calls.
</details>

---

**Question 10.** What happens to an EBS volume **by default** when the EC2 instance it is attached to is terminated?

A) The EBS volume is automatically snapshotted to S3, then deleted
B) All attached EBS volumes are always retained regardless of termination
C) The root EBS volume is deleted (DeleteOnTermination=true by default); additional data volumes default to being retained (DeleteOnTermination=false)
D) All attached EBS volumes are deleted immediately upon termination

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** C) The root EBS volume is deleted (DeleteOnTermination=true by default); additional data volumes default to being retained (DeleteOnTermination=false)

**Explanation:** The root volume has `DeleteOnTermination=true` by default — it is deleted when the instance terminates. Additional data volumes you attach have `DeleteOnTermination=false` by default and persist after termination. Both settings are configurable at launch time or while the instance is running.

- **Why A is wrong:** AWS does not automatically snapshot before deletion; you must configure this via AWS Backup or Data Lifecycle Manager.
- **Why B is wrong:** The root volume IS deleted by default unless explicitly changed.
- **Why D is wrong:** Additional data volumes are retained by default, not deleted.
</details>

---

**Question 11.** Which AWS CLI command correctly lists EC2 instances filtered to show only those in the `running` state?

A) `aws ec2 list-instances --state running`
B) `aws ec2 describe-instances --filters Name=instance-state-name,Values=running`
C) `aws ec2 get-instances --query "running"`
D) `aws ec2 show-instances --status running`

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) `aws ec2 describe-instances --filters Name=instance-state-name,Values=running`

**Explanation:** `aws ec2 describe-instances` retrieves EC2 instance details. The `--filters` flag accepts server-side filters like `Name=instance-state-name,Values=running` to narrow results before they are returned — more efficient than fetching all instances and filtering client-side.

- **Why A is wrong:** `list-instances` is not a valid EC2 CLI subcommand.
- **Why C is wrong:** `get-instances` does not exist; `--query` performs client-side JMESPath filtering on results already returned.
- **Why D is wrong:** `show-instances` is not a valid AWS CLI command.
</details>

---

**Question 12.** When you **deploy** an API Gateway REST API to a Stage, what is the primary outcome?

A) It registers the API with Route 53 for automatic DNS resolution
B) It makes the API publicly invocable at a versioned URL such as `https://{id}.execute-api.{region}.amazonaws.com/{stage}`
C) It automatically attaches a WAF Web Application Firewall to the API
D) It enables response caching on all resources and methods by default

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** B) It makes the API publicly invocable at a versioned URL such as `https://{id}.execute-api.{region}.amazonaws.com/{stage}`

**Explanation:** In API Gateway, resources and methods are not accessible until deployed to a Stage. A Stage (dev, staging, prod) represents a versioned snapshot of your API and creates the invocation URL. Multiple stages allow separate environments with different throttling, caching, and stage variable settings from the same API definition.

- **Why A is wrong:** Route 53 registration is a separate step requiring a custom domain and API mapping.
- **Why C is wrong:** WAF association must be configured explicitly via AWS WAF; it is not automatic on deployment.
- **Why D is wrong:** Stage-level caching is disabled by default and must be explicitly enabled per resource/method.
</details>

---

## Part 2: True/False

**Question 13.** True or False: An EC2 Security Group can be assigned to multiple EC2 instances within the same VPC.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** A single Security Group can be attached to multiple EC2 instances (and other VPC resources such as RDS databases and Lambda functions) within the same VPC. This is a core design feature — define rules once and apply them consistently across all instances sharing the same access pattern, reducing configuration drift and management overhead.
</details>

---

**Question 14.** True or False: Each Docker container runs its own separate OS kernel, which is why containers provide complete isolation from the host system.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** Containers do NOT run separate OS kernels. They share the host OS kernel and use Linux **namespaces** (for process, network, and filesystem isolation) and **cgroups** (for resource limits) to achieve isolation at the process level. This is precisely what makes containers lightweight compared to VMs. A kernel-level vulnerability can potentially affect all containers on the same host — containers provide process isolation, not kernel isolation.
</details>

---

**Question 15.** True or False: In EC2 Auto Scaling, the `MinSize` and `MaxSize` settings guarantee the instance count never falls below the minimum or exceeds the maximum.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** An Auto Scaling Group enforces hard boundaries: it will never terminate instances below `MinSize` (to preserve availability) and will never launch instances beyond `MaxSize` (to control cost). The `DesiredCapacity` floats between these bounds in response to scaling policies, scheduled actions, or manual changes.
</details>

---

**Question 16.** True or False: AWS API Gateway supports both REST APIs and WebSocket APIs, enabling both request-response and full-duplex persistent connection patterns.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** True

**Explanation:** API Gateway offers three API types: **REST API**, **HTTP API** (a lighter, cheaper REST variant), and **WebSocket API**. REST/HTTP APIs follow the traditional request-response model. WebSocket APIs maintain persistent connections between client and server, enabling real-time bidirectional communication — ideal for chat applications, live dashboards, and multiplayer games.
</details>

---

**Question 17.** True or False: A standard EBS volume can be simultaneously attached to multiple EC2 instances by default, enabling shared block storage across a cluster.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** False

**Explanation:** By default, a standard EBS volume can only be attached to **one** EC2 instance at a time. AWS does offer **EBS Multi-Attach** for io1/io2 volumes — allowing attachment to up to 16 Nitro-based instances in the same Availability Zone — but this requires explicit configuration and application-level coordination to manage concurrent writes safely. It is not the default behavior.
</details>

---

## Part 3: Fill in the Blank

**Question 18.** The AWS CLI command to retrieve details about your EC2 instances is `aws ec2 _______`. *(Fill in the exact subcommand)*

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `describe-instances`

**Explanation:** `aws ec2 describe-instances` is the standard command for inspecting EC2 instances. Without filters it returns all instances in the region. Add `--filters Name=instance-state-name,Values=running` to narrow to running instances, or `--instance-ids i-xxxxxxxxx` to target a specific instance. This command is a foundation of EC2 scripting and automation.
</details>

---

**Question 19.** In a Dockerfile, the `_______` instruction sets the default executable that runs when the container starts and **cannot** be easily overridden by arguments passed to `docker run` — whereas `CMD` provides default arguments that *can* be replaced at runtime.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** `ENTRYPOINT`

**Explanation:** `ENTRYPOINT` defines the process that always runs when the container starts. Arguments passed to `docker run` are appended to the `ENTRYPOINT` command rather than replacing it (only the `--entrypoint` flag can override it). `CMD` provides default arguments to `ENTRYPOINT`, or acts as the default command if no `ENTRYPOINT` is defined. In ECS Task Definitions, these map to the `entryPoint` and `command` fields — understanding the distinction is critical for correctly configuring container startup behavior.
</details>

---

**Question 20.** When configuring EC2 Auto Scaling, you define a _______ that specifies the AMI ID, instance type, key pair, security groups, and user data script that Auto Scaling uses when launching new instances.

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** **Launch Template** (previously: Launch Configuration — now deprecated in favor of Launch Templates)

**Explanation:** A **Launch Template** is the versioned blueprint Auto Scaling uses to spin up new EC2 instances. It encapsulates all instance configuration: AMI ID, instance type, key pair, security groups, IAM instance profile, user data, and storage settings. Launch Templates supersede Launch Configurations by supporting versioning, mixed instance policies (combining On-Demand and Spot), and a broader set of EC2 features like T3 Unlimited and Capacity Reservations.
</details>

---

## Part 4: Code Prediction

**Question 21.** Examine the following AWS CLI command:

```bash
aws ec2 run-instances \
  --image-id ami-0abcdef1234567890 \
  --instance-type t2.micro \
  --key-name MyKeyPair \
  --security-group-ids sg-0123456789abcdef0 \
  --count 3 \
  --region us-east-1
```

**What will be the effect of running this command?** How many instances are launched, and what determines their placement within us-east-1?

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** This command launches **3** `t2.micro` EC2 instances using the specified AMI and key pair in region `us-east-1`. Because no `--subnet-id` is specified, AWS places them in the **default VPC's default subnet**, and the Availability Zone is chosen automatically by AWS.

**Explanation of each flag:**

- `--image-id`: Specifies which AMI to use as the root image — the OS and pre-installed software.
- `--instance-type t2.micro`: Defines the hardware profile (1 vCPU, 1 GiB RAM for t2.micro).
- `--key-name MyKeyPair`: Associates a key pair so you can SSH in with the matching `.pem` file.
- `--security-group-ids`: Attaches the specified Security Group to control inbound/outbound traffic.
- `--count 3`: Launches exactly 3 identical instances in one API call. Also accepts `min:max` format (e.g., `2:5`) for Spot requests where partial fulfillment is acceptable.
- `--region us-east-1`: Targets the N. Virginia region.

The command returns a JSON response containing instance IDs, private IPs, and initial state (`pending`) for all 3 instances.
</details>

---

**Question 22.** A developer runs the following command:

```bash
docker run -d \
  --name web-app \
  -p 8080:80 \
  -e APP_ENV=production \
  nginx:1.25
```

After the command completes, they navigate to `http://localhost:8080` in a browser. **What will they see, and what does each flag contribute to this outcome?**

<details>
<summary><b>🔎 Click for Solution</b></summary>

**Correct Answer:** The developer will see the **default Nginx welcome page** ("Welcome to nginx!"). The container is running in the background, serving HTTP traffic on the host's port 8080.

**Explanation of each flag:**

- **`-d`** (detached): Runs the container in the background, immediately returning the container ID to the terminal.
- **`--name web-app`**: Assigns a human-readable name instead of Docker's auto-generated random name — enables `docker stop web-app`, `docker logs web-app`, etc.
- **`-p 8080:80`** (port mapping — format is `host:container`): Maps port 8080 on the **host machine** to port 80 **inside the container**. Nginx listens on port 80 inside; without this flag the container would be unreachable from outside.
- **`-e APP_ENV=production`**: Injects an environment variable into the container. For stock Nginx this has no visible effect, but a custom application image would read `APP_ENV` at runtime to switch behavior.
- **`nginx:1.25`**: The image name and pinned tag. Docker pulls from Docker Hub if not cached locally. Pinning `:1.25` (rather than `:latest`) ensures reproducible builds.

**ECS connection:** These flags map directly to ECS Task Definition fields: `portMappings` (containerPort: 80, hostPort: 8080), `environment` (key: APP_ENV, value: production), and `image` (nginx:1.25) — the same concepts apply, expressed as JSON rather than CLI flags.
</details>
