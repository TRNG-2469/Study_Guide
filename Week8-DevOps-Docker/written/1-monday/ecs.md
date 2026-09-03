# Amazon ECS — Elastic Container Service

## Learning Objectives

By the end of this lesson, you will be able to:

- Explain what ECS is and the problem it solves beyond just running containers
- Define the core ECS components: clusters, task definitions, tasks, and services
- Compare the Fargate and EC2 launch types and choose appropriately
- Understand how ECS integrates with ECR for private image storage
- Explain when to choose ECS over EKS (Kubernetes)

---

## Why This Matters

You now know how to build Docker images and run containers with `docker run`. But in production, you do not just run one container on one server — you run dozens of containers across multiple servers, need to restart failed containers automatically, perform rolling deployments without downtime, and scale the number of containers based on traffic. ECS is the AWS service that manages all of this. It is the bridge between the Docker concepts you are learning and production-grade container deployments on AWS.

---

## What Is ECS?

**Amazon Elastic Container Service (ECS)** is a fully managed container orchestration service. "Orchestration" means it handles:

- **Scheduling:** Deciding which server (EC2 instance) to run each container on
- **Lifecycle management:** Starting, stopping, and restarting containers
- **Scaling:** Adding or removing containers based on load
- **Health monitoring:** Replacing failed containers automatically
- **Deployments:** Rolling out new versions without downtime

### Analogy

If Docker is the shipping container, ECS is the container port — the operation that decides which ship (EC2 instance) carries which container, handles loading/unloading, replaces damaged containers, and manages the entire logistics operation.

---

## Core ECS Components

```
┌──────────────────────────────────────────────────────────────┐
│                        ECS Cluster                           │
│  (a logical grouping of compute capacity)                    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    ECS Service                       │    │
│  │  (maintains N running copies of a task definition)  │    │
│  │                                                      │    │
│  │   ┌────────────┐  ┌────────────┐  ┌────────────┐   │    │
│  │   │   Task 1   │  │   Task 2   │  │   Task 3   │   │    │
│  │   │ ┌────────┐ │  │ ┌────────┐ │  │ ┌────────┐ │   │    │
│  │   │ │Container│ │  │ │Container│ │  │ │Container│ │   │    │
│  │   │ │(App)   │ │  │ │(App)   │ │  │ │(App)   │ │   │    │
│  │   │ └────────┘ │  │ └────────┘ │  │ └────────┘ │   │    │
│  │   └────────────┘  └────────────┘  └────────────┘   │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

### 1. Cluster

A **cluster** is a logical grouping of infrastructure where your containers run. The cluster manages:
- The pool of compute capacity (EC2 instances or Fargate capacity)
- Networking (VPC and subnets)
- IAM permissions
- CloudWatch logging

Creating a cluster:

```bash
aws ecs create-cluster \
  --cluster-name myapp-cluster \
  --capacity-providers FARGATE FARGATE_SPOT \
  --tags key=Environment,value=production
```

### 2. Task Definition

A **task definition** is a blueprint for a task — analogous to a Docker Compose file or a Kubernetes Pod spec. It defines:

- Which container image(s) to run
- CPU and memory requirements
- Port mappings
- Environment variables
- Volume mounts
- IAM task role (what AWS services the container can access)
- Logging configuration

```json
{
  "family": "myapp-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123456789012:role/myapp-task-role",
  "containerDefinitions": [
    {
      "name": "myapp",
      "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp:1.2.3",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "production"},
        {"name": "DB_HOST", "value": "mydb.abc123.us-east-1.rds.amazonaws.com"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/myapp",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

Register the task definition:

```bash
aws ecs register-task-definition \
  --cli-input-json file://task-definition.json
```

### 3. Task

A **task** is a running instance of a task definition — the actual containers running on the cluster. Tasks can be:
- **Long-running:** Web servers, APIs (managed by a Service)
- **One-off:** Database migrations, batch jobs (run manually or on a schedule)

Running a one-off task:

```bash
aws ecs run-task \
  --cluster myapp-cluster \
  --task-definition myapp-task:3 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={
    subnets=[subnet-0abc123,subnet-0def456],
    securityGroups=[sg-0abc123def456789],
    assignPublicIp=ENABLED
  }"
```

### 4. Service

A **service** ensures that a specified number of task instances are always running. If a task fails, the service replaces it automatically.

```bash
aws ecs create-service \
  --cluster myapp-cluster \
  --service-name myapp-service \
  --task-definition myapp-task:3 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={
    subnets=[subnet-0abc123,subnet-0def456],
    securityGroups=[sg-0abc123def456789],
    assignPublicIp=ENABLED
  }" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=myapp,containerPort=8080"
```

Services also handle **rolling deployments**: when you update the task definition (e.g., new image version), the service replaces old tasks with new ones incrementally, keeping a percentage of tasks running throughout.

---

## Launch Types: Fargate vs EC2

ECS supports two launch types that determine where your containers actually run.

### Fargate (Serverless Containers)

With Fargate, AWS manages the underlying EC2 instances. You only define your container requirements (CPU, memory, image), and AWS provisions appropriate infrastructure invisibly.

```
You define:                AWS manages:
┌──────────────┐           ┌──────────────────────────────┐
│ Task Def     │           │ EC2 instance selection       │
│ cpu: 512     │ ────────► │ Instance provisioning        │
│ memory: 1024 │           │ OS patching                  │
│ image: ...   │           │ Container placement           │
└──────────────┘           │ Scaling infrastructure       │
                           └──────────────────────────────┘
```

**Fargate advantages:**
- No EC2 instance management — no patching, no capacity planning
- Pay only for CPU and memory consumed by your tasks (per-second billing)
- Scales from zero tasks to hundreds instantly

**Fargate disadvantages:**
- Higher per-unit cost than EC2 at large scale
- Slightly slower task startup (cold start of underlying micro-VM)
- No access to the underlying host

### EC2 Launch Type

With the EC2 launch type, you manage a pool of EC2 instances that form your cluster. ECS schedules containers onto these instances.

```
You manage:                        ECS manages:
┌───────────────────────┐          ┌──────────────────────────┐
│ EC2 instances in      │          │ Container scheduling     │
│ cluster               │ ───────► │ Task placement           │
│ Auto Scaling group    │          │ Health monitoring        │
│ OS + Docker runtime   │          │ Rolling deployments      │
└───────────────────────┘          └──────────────────────────┘
```

**EC2 advantages:**
- Lower cost at scale (especially with Reserved Instances or Spot Instances)
- Full access to the underlying host (useful for GPU workloads, high-bandwidth networking)
- Consistent compute capacity (no cold starts)

**EC2 disadvantages:**
- Must manage EC2 instances (patching, capacity planning)
- Requires ECS Container Agent on every instance

### Choosing Between Fargate and EC2

| Scenario | Recommended |
|---|---|
| Getting started with ECS | Fargate |
| Variable traffic with unpredictable peaks | Fargate |
| Cost-optimized at large, stable scale | EC2 (Reserved Instances) |
| GPU-required workloads | EC2 |
| Dev/test environments | Fargate |
| >50 tasks running continuously | EC2 (cost comparison needed) |

---

## ECR — Elastic Container Registry

**Amazon ECR (Elastic Container Registry)** is AWS's private Docker image registry. It integrates directly with ECS and IAM.

### Why Use ECR Instead of Docker Hub?

| Feature | Docker Hub (public) | ECR (private) |
|---|---|---|
| Privacy | Public by default | Private by default |
| AWS IAM integration | No | Yes — IAM policies control access |
| ECS integration | Manual credential setup | Seamless — ECS pulls automatically |
| Vulnerability scanning | Limited | Built-in with AWS Inspector |
| Region | Global CDN | Regional (same region as ECS = fast pulls) |

### Pushing an Image to ECR

```bash
# Step 1: Create a repository in ECR
aws ecr create-repository \
  --repository-name myapp \
  --region us-east-1

# Step 2: Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 \
  | docker login \
    --username AWS \
    --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Step 3: Tag your local image with the ECR repository URI
docker tag myapp:1.2.3 \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp:1.2.3

# Step 4: Push the image
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp:1.2.3
```

In the task definition, reference the ECR image URI:

```json
"image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp:1.2.3"
```

---

## ECS vs EKS — Choosing the Right Orchestrator

Both ECS and EKS (Elastic Kubernetes Service) run containers on AWS. Choosing between them is a common architecture decision.

| Feature | ECS | EKS (Kubernetes) |
|---|---|---|
| **Complexity** | Low — AWS-native, simpler API | High — Kubernetes has a steep learning curve |
| **Learning curve** | Days | Weeks to months |
| **Portability** | AWS-only | Portable — same config runs on any cloud |
| **Ecosystem** | AWS tools (CloudWatch, ALB, IAM) | Vast open-source ecosystem (Helm, Istio, etc.) |
| **Cost** | No cluster fee for Fargate | $0.10/hour per cluster + node costs |
| **Managed control plane** | Yes (fully managed) | Yes (EKS manages Kubernetes masters) |
| **Multi-cloud strategy** | No | Yes (same Kubernetes manifest runs on GKE, AKS) |
| **Best for** | AWS-first teams, simpler microservices | Large orgs, multi-cloud, complex orchestration needs |

**Recommendation for this course:** Use ECS. It provides the same operational benefits as Kubernetes at a fraction of the learning overhead.

---

## Monitoring ECS

```bash
# List running services in a cluster
aws ecs list-services --cluster myapp-cluster

# Describe a service (see running task count, deployments)
aws ecs describe-services \
  --cluster myapp-cluster \
  --services myapp-service

# List tasks in a service
aws ecs list-tasks \
  --cluster myapp-cluster \
  --service-name myapp-service

# Describe a specific task (see container status, IP address)
aws ecs describe-tasks \
  --cluster myapp-cluster \
  --tasks arn:aws:ecs:us-east-1:123456789012:task/myapp-cluster/abc123

# View logs in CloudWatch
aws logs get-log-events \
  --log-group-name /ecs/myapp \
  --log-stream-name ecs/myapp/abc123def456
```

---

## Summary

- ECS orchestrates containers: scheduling, health monitoring, scaling, and deployments.
- Core concepts: Cluster (infrastructure grouping), Task Definition (blueprint), Task (running instance), Service (keeps N tasks running).
- Fargate = serverless; AWS manages EC2. Best for most teams starting with ECS.
- EC2 launch type = you manage the instances. Best for cost optimization at scale.
- ECR stores private Docker images with seamless ECS and IAM integration.
- Choose ECS over EKS for simpler setups and AWS-only deployments; EKS for multi-cloud and complex Kubernetes ecosystem needs.

---

## External Resources

- [Amazon ECS Developer Guide](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html)
- [ECS Fargate vs EC2 Launch Type](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/launch_types.html)
- [Amazon ECR User Guide](https://docs.aws.amazon.com/AmazonECR/latest/userguide/what-is-ecr.html)
