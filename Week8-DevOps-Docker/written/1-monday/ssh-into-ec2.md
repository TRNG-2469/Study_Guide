# SSH into an EC2 Instance

## Learning Objectives

By the end of this lesson, you will be able to:

- Generate an SSH key pair and understand its two parts
- Set correct file permissions on a .pem key file
- Connect to an EC2 instance using the `ssh -i` command
- Diagnose and resolve the most common SSH connection errors
- Use EC2 Instance Connect as a no-key browser alternative

---

## Why This Matters

Launching an EC2 instance is only the first step. To deploy code, configure software, view logs, or troubleshoot issues, you need to get inside the instance. SSH (Secure Shell) is the standard protocol for doing this securely. Every DevOps engineer and cloud practitioner uses SSH daily. Mastering it — including knowing how to read error messages — is a foundational survival skill for this week and your career.

---

## Background: How SSH Key Authentication Works

SSH uses **asymmetric cryptography** (a public/private key pair):

```
Your laptop                           EC2 Instance
──────────                            ────────────
Private key (.pem)   ─── verifies ─►  Public key (stored in ~/.ssh/authorized_keys)
(NEVER share this)                    (safe to share; useless without private key)
```

When you connect:
1. The EC2 instance presents its public key fingerprint.
2. Your SSH client signs a challenge with your private key.
3. The instance verifies the signature using the stored public key.
4. If it matches, you are authenticated — no password needed.

---

## Step 1: Create or Download a Key Pair

### Option A — Create a Key Pair in the AWS Console

1. Open **EC2 Console → Network & Security → Key Pairs**
2. Click **Create key pair**
3. Fill in:
   - **Name:** `my-key-pair`
   - **Key pair type:** RSA
   - **Private key file format:** `.pem` (for Linux/macOS) or `.ppk` (for PuTTY on Windows)
4. Click **Create key pair**
5. The browser downloads `my-key-pair.pem` automatically — **this is your only chance to download it**

### Option B — Create a Key Pair via the CLI

```bash
# Create the key pair and save the private key directly to a .pem file
aws ec2 create-key-pair \
  --key-name my-key-pair \
  --query 'KeyMaterial' \       # Extract only the key text from the JSON response
  --output text \               # Return plain text, not JSON
  > my-key-pair.pem             # Redirect output to a file
```

### Option C — Use an Existing Local Key Pair

If you already have an SSH key pair on your laptop (`~/.ssh/id_rsa` + `~/.ssh/id_rsa.pub`), you can import your public key into AWS:

```bash
aws ec2 import-key-pair \
  --key-name my-existing-key \
  --public-key-material fileb://~/.ssh/id_rsa.pub
```

---

## Step 2: Set Correct File Permissions

This step is **mandatory** on Linux and macOS. SSH refuses to use a private key file that is readable by other users on the system — it considers such a key compromised.

```bash
# Set the file to be readable only by you (owner)
chmod 400 my-key-pair.pem

# Verify the permissions
ls -l my-key-pair.pem
# Expected: -r-------- 1 youruser youruser 1674 Jun 01 10:00 my-key-pair.pem
# The 'r--------' means: owner=read-only, group=none, other=none
```

### Permission Reference

| chmod value | Meaning | SSH accepts? |
|---|---|---|
| `400` | Owner read-only | Yes |
| `600` | Owner read+write | Yes |
| `644` | Owner r+w; others read | **No** — too permissive |
| `777` | Everyone r+w+execute | **No** — far too permissive |

> On **Windows**, permissions work differently. If using PowerShell, right-click the .pem file → Properties → Security → ensure only your user account has access. Or use WSL (Windows Subsystem for Linux) where `chmod 400` works normally.

---

## Step 3: Find the Instance's Public IP and Username

### Get the Public IP

```bash
aws ec2 describe-instances \
  --instance-ids i-0abcd1234efgh5678 \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text
```

Or in the AWS Console: **EC2 → Instances → select instance → Public IPv4 address**

### Default Usernames by AMI

Each AMI type has a different default username:

| AMI | Default Username |
|---|---|
| Amazon Linux 2 / Amazon Linux 2023 | `ec2-user` |
| Ubuntu | `ubuntu` |
| Debian | `admin` |
| CentOS | `centos` |
| RHEL | `ec2-user` |
| Windows | `Administrator` (RDP, not SSH) |

---

## Step 4: Connect with SSH

```bash
ssh -i /path/to/my-key-pair.pem ec2-user@<PUBLIC-IP-ADDRESS>
```

### Full Command Breakdown

```bash
ssh                              # The SSH client program
  -i /path/to/my-key-pair.pem   # -i = identity file (your private key)
  ec2-user                       # Username on the remote instance
  @                              # Separator between user and host
  54.123.45.67                   # Public IP address of the instance
```

### First Connection Warning

The first time you connect to an instance, SSH asks you to verify the host:

```
The authenticity of host '54.123.45.67 (54.123.45.67)' can't be established.
ECDSA key fingerprint is SHA256:abc123def456...
Are you sure you want to continue connecting (yes/no/[fingerprint])?
```

Type `yes` and press Enter. This adds the instance to `~/.ssh/known_hosts`. Subsequent connections skip this prompt.

### Successful Connection Output

```
Last login: Mon Jun  3 09:15:42 2024 from 203.0.113.0

       __|  __|_  )
       _|  (     /   Amazon Linux 2
      ___|\___|___|

https://aws.amazon.com/amazon-linux-2/
[ec2-user@ip-172-31-42-10 ~]$
```

You are now inside the EC2 instance. The prompt `[ec2-user@ip-172-31-42-10 ~]$` shows:
- `ec2-user` — your username
- `ip-172-31-42-10` — the instance's private IP as its hostname
- `~` — your current directory (home directory)

---

## Common Connection Errors and Solutions

### Error 1: Connection Timed Out

```
ssh: connect to host 54.123.45.67 port 22: Connection timed out
```

**Cause:** The security group attached to the instance does not have an inbound rule allowing port 22.

**Fix:**
1. Go to **EC2 → Security Groups**
2. Select the security group attached to your instance
3. **Inbound rules → Edit inbound rules → Add rule**
4. Type: `SSH`, Protocol: `TCP`, Port: `22`, Source: `My IP` (or `0.0.0.0/0` for any IP)
5. Save rules, then retry

```bash
# Verify via CLI
aws ec2 describe-security-groups \
  --group-ids sg-0abc123def456789 \
  --query 'SecurityGroups[0].IpPermissions'
```

### Error 2: Permission Denied (publickey)

```
ec2-user@54.123.45.67: Permission denied (publickey).
```

**Possible Causes and Fixes:**

| Cause | Fix |
|---|---|
| Wrong .pem file (key mismatch) | Verify the key pair name matches what was used at instance launch |
| Wrong username | Check the default username for your AMI type (see table above) |
| .pem permissions too open | Run `chmod 400 my-key-pair.pem` |
| Connecting to wrong IP | Confirm the Public IP in the EC2 console |

### Error 3: WARNING: UNPROTECTED PRIVATE KEY FILE

```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@         WARNING: UNPROTECTED PRIVATE KEY FILE!          @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
Permissions 0644 for 'my-key-pair.pem' are too open.
```

**Fix:** `chmod 400 my-key-pair.pem`

### Error 4: Host Key Verification Failed

```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
```

**Cause:** The IP address was previously used by a different instance, and its fingerprint is stored in `~/.ssh/known_hosts`.

**Fix:**

```bash
# Remove the old fingerprint for this IP
ssh-keygen -R 54.123.45.67

# Then reconnect and accept the new fingerprint
ssh -i my-key-pair.pem ec2-user@54.123.45.67
```

### Error 5: No Route to Host

```
ssh: connect to host 54.123.45.67 port 22: No route to host
```

**Cause:** Instance has no public IP, or it is in a private subnet with no internet gateway.

**Fix:** Ensure the instance has a public IP (or Elastic IP), is in a public subnet, and the route table has a route to an Internet Gateway.

---

## EC2 Instance Connect — No Key Required

**EC2 Instance Connect** is a browser-based SSH terminal built into the AWS Console. It does not require a .pem file.

### How It Works

AWS temporarily injects a one-time public SSH key into the instance's `authorized_keys`. The browser terminal uses the corresponding private key to authenticate. The temporary key expires after 60 seconds.

### Using EC2 Instance Connect

1. Open **EC2 Console → Instances**
2. Select your running instance
3. Click **Connect** (top right)
4. Select **EC2 Instance Connect** tab
5. Verify the username (e.g., `ec2-user`)
6. Click **Connect**

A terminal opens in your browser — no local configuration needed.

### Limitations

- Requires the instance to have a public IP address (or use VPC endpoint)
- Port 22 must be open in the security group from AWS IP ranges (for the console method)
- Only works with Amazon Linux 2, Amazon Linux 2023, and Ubuntu (not all AMIs)

---

## SSH Config File for Convenience

Typing the full `ssh -i my-key-pair.pem ec2-user@54.123.45.67` command every time is tedious. Create an SSH config file to simplify this:

```bash
# Edit (or create) ~/.ssh/config
nano ~/.ssh/config
```

Add an entry:

```
Host myserver
    HostName 54.123.45.67
    User ec2-user
    IdentityFile ~/keys/my-key-pair.pem
```

Now connect with simply:

```bash
ssh myserver
```

---

## Copying Files to/from an EC2 Instance

```bash
# Copy a file FROM your laptop TO the instance
scp -i my-key-pair.pem ./app.jar ec2-user@54.123.45.67:/opt/myapp/

# Copy a file FROM the instance TO your laptop
scp -i my-key-pair.pem ec2-user@54.123.45.67:/var/log/app.log ./app.log

# Copy an entire directory (recursive)
scp -i my-key-pair.pem -r ./config/ ec2-user@54.123.45.67:/opt/myapp/config/
```

---

## Summary

- SSH key pairs consist of a public key (on the instance) and a private key (.pem on your laptop).
- Always run `chmod 400 my-key-pair.pem` before using a key file.
- Connect with: `ssh -i /path/to/key.pem username@public-ip`
- Default usernames: `ec2-user` (Amazon Linux), `ubuntu` (Ubuntu).
- Connection timeout → check security group inbound rules for port 22.
- Permission denied → verify correct key file, username, and `chmod 400`.
- EC2 Instance Connect provides a browser-based terminal with no key file required.

---

## External Resources

- [Connect to Your Linux Instance Using SSH](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/connect-linux-inst-ssh.html)
- [EC2 Instance Connect Documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Connect-using-EC2-Instance-Connect.html)
- [SSH Config File Guide (ssh_config manpage)](https://linux.die.net/man/5/ssh_config)
