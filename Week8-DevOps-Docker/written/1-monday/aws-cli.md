# AWS CLI — Installation, Configuration, and Essential Commands

## Learning Objectives

By the end of this lesson, you will be able to:

- Install the AWS CLI on macOS, Windows, and Linux
- Configure the CLI with credentials using `aws configure`
- Use named profiles to manage multiple AWS accounts
- Execute common CLI commands for S3, EC2, and ECS
- Choose and interpret the correct output format for your use case

---

## Why This Matters

This week's epic is the operational inflection point of your training: you move from *building* software to *deploying and monitoring* it at cloud scale. Before you can automate deployments, spin up servers, or manage containers, you need a reliable, scriptable interface to AWS. The **AWS CLI** is that interface. Every DevOps workflow you encounter — CI/CD pipelines, infrastructure-as-code, scheduled automation — relies on the CLI or tools built on top of it. Mastering it now means every subsequent topic this week becomes dramatically easier.

---

## What Is the AWS CLI?

The **AWS Command Line Interface (CLI)** is an open-source tool that lets you interact with AWS services directly from your terminal. Instead of clicking through the AWS Management Console web UI, you type commands. This matters for three reasons:

1. **Automation** — Commands can be scripted and embedded in pipelines.
2. **Speed** — Experienced engineers operate 10–50× faster in the CLI than in the console.
3. **Repeatability** — A script runs identically every time; human clicks do not.

---

## Installation

### macOS

```bash
# Using the official installer package
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /

# Verify the installation
aws --version
# Expected output: aws-cli/2.x.x Python/3.x.x ...
```

### Windows

Download and run the MSI installer from:
`https://awscli.amazonaws.com/AWSCLIV2.msi`

Then open **Command Prompt** or **PowerShell** and verify:

```powershell
aws --version
```

### Linux (Amazon Linux 2 / Ubuntu / Debian)

```bash
# Download the zip package
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"

# Unzip it
unzip awscliv2.zip

# Run the install script (requires sudo)
sudo ./aws/install

# Verify
aws --version
```

---

## Configuration — `aws configure`

Before you can make any API calls, the CLI needs to know *who you are* (credentials) and *where* you are operating (region, output format).

Run the following command and answer the prompts:

```bash
aws configure
```

You will be asked for four values:

```
AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
Default region name [None]: us-east-1
Default output format [None]: json
```

### Where Do These Values Come From?

| Value | Source |
|---|---|
| Access Key ID | IAM Console → Your user → Security credentials → Create access key |
| Secret Access Key | Shown **once** at creation time — copy it immediately |
| Region | AWS region where your resources live, e.g. `us-east-1` |
| Output format | Your preference: `json`, `table`, or `text` |

### What Gets Stored?

`aws configure` writes two files:

```
~/.aws/credentials   # Contains your secret keys (never commit this to Git)
~/.aws/config        # Contains region, output format, and other settings
```

**Important:** These files contain sensitive credentials. They should never be committed to a Git repository. Add `~/.aws/` to your `.gitignore` if you ever work from a checked-out repo on your local machine.

---

## Named Profiles

Real-world engineers juggle multiple AWS accounts: personal, development, staging, production. Named profiles let you switch between them without reconfiguring.

### Creating a Named Profile

```bash
# This creates a profile called "dev"
aws configure --profile dev
```

### Using a Named Profile

```bash
# List S3 buckets in your "dev" account
aws s3 ls --profile dev

# Or set it as the environment default for the current shell session
export AWS_PROFILE=dev
aws s3 ls   # Now automatically uses "dev" credentials
```

### Viewing All Profiles

```bash
cat ~/.aws/credentials
```

Example contents:

```ini
[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

[dev]
aws_access_key_id = AKIAI44QH8DHBEXAMPLE
aws_secret_access_key = je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY
```

---

## Output Formats

The CLI can return data in three formats. Choose based on how you plan to use the output.

### `json` (default)

Machine-readable. Best when piping output to tools like `jq` for further processing.

```bash
aws ec2 describe-instances --output json
```

```json
{
  "Reservations": [
    {
      "Instances": [
        {
          "InstanceId": "i-0abcd1234efgh5678",
          "InstanceType": "t3.micro",
          "State": { "Name": "running" }
        }
      ]
    }
  ]
}
```

### `table`

Human-readable. Formats data as an ASCII table. Best when you are reading output interactively.

```bash
aws ec2 describe-instances --output table
```

```
----------------------------------------------
|           DescribeInstances                |
+--------------------------------------------+
| InstanceId          | i-0abcd1234efgh5678  |
| InstanceType        | t3.micro             |
| State               | running              |
+--------------------------------------------+
```

### `text`

Tab-separated values. Best for piping into shell scripts or `grep`/`awk`.

```bash
aws ec2 describe-instances \
  --query 'Reservations[*].Instances[*].[InstanceId,State.Name]' \
  --output text
```

```
i-0abcd1234efgh5678    running
```

---

## The `--query` Flag

The `--query` flag uses **JMESPath** syntax to filter and reshape JSON output on the server side. This avoids piping megabytes of JSON through `jq`.

```bash
# Get only instance IDs and their state names
aws ec2 describe-instances \
  --query 'Reservations[*].Instances[*].[InstanceId,State.Name]' \
  --output table
```

---

## Common Commands

### S3 — Simple Storage Service

```bash
# List all buckets in your account
aws s3 ls

# List objects inside a specific bucket
aws s3 ls s3://my-bucket-name/

# Copy a local file to S3
aws s3 cp ./report.pdf s3://my-bucket-name/reports/report.pdf

# Copy a file from S3 to local
aws s3 cp s3://my-bucket-name/reports/report.pdf ./report.pdf

# Sync an entire local folder to S3 (only uploads changed files)
aws s3 sync ./build/ s3://my-bucket-name/

# Delete an object
aws s3 rm s3://my-bucket-name/reports/old-report.pdf
```

### EC2 — Elastic Compute Cloud

```bash
# List all instances in the default region
aws ec2 describe-instances

# List only running instances (using a filter)
aws ec2 describe-instances \
  --filters "Name=instance-state-name,Values=running"

# Start a stopped instance
aws ec2 start-instances --instance-ids i-0abcd1234efgh5678

# Stop a running instance
aws ec2 stop-instances --instance-ids i-0abcd1234efgh5678

# Reboot an instance
aws ec2 reboot-instances --instance-ids i-0abcd1234efgh5678

# Describe security groups
aws ec2 describe-security-groups
```

### ECS — Elastic Container Service

```bash
# List all ECS clusters
aws ecs list-clusters

# List services in a cluster
aws ecs list-services --cluster my-cluster

# Run a one-off task (e.g., a database migration)
aws ecs run-task \
  --cluster my-cluster \
  --task-definition my-task-def:3 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-abc123],securityGroups=[sg-xyz789],assignPublicIp=ENABLED}"

# Describe a running task
aws ecs describe-tasks \
  --cluster my-cluster \
  --tasks arn:aws:ecs:us-east-1:123456789012:task/my-cluster/abc123def456
```

---

## Useful Global Flags

| Flag | Purpose | Example |
|---|---|---|
| `--region` | Override the default region for this command | `--region us-west-2` |
| `--profile` | Use a named profile | `--profile production` |
| `--output` | Change output format | `--output table` |
| `--query` | Filter output using JMESPath | `--query 'Instances[0].InstanceId'` |
| `--dry-run` | Simulate the command without executing (EC2 only) | `--dry-run` |
| `--no-cli-pager` | Disable the pager (useful in scripts) | `--no-cli-pager` |

---

## Summary

- The AWS CLI bridges your terminal to every AWS service.
- `aws configure` stores credentials in `~/.aws/credentials` — never commit these.
- Named profiles (`--profile`) let you manage multiple accounts cleanly.
- Output formats: `json` for machines, `table` for humans, `text` for scripts.
- The `--query` flag filters data using JMESPath, reducing output noise.
- Core service commands follow the pattern: `aws <service> <action> [options]`.

---

## External Resources

- [AWS CLI Official Documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html)
- [JMESPath Query Tutorial](https://jmespath.org/tutorial.html)
- [AWS CLI Command Reference (full index)](https://awscli.amazonaws.com/v2/documentation/api/latest/index.html)
