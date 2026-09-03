# Solution Notes — Lab: Your First AWS Deployment

**For instructor use only — do not distribute to trainees before the lab.**

---

## Checkpoint Expected Outputs

### Checkpoint 1 — EC2 Instance Running

The EC2 Instances list should show:

| Field | Expected Value |
|---|---|
| Name | `week8-product-api` |
| Instance state | `Running` (green circle) |
| Instance type | `t3.micro` |
| AMI | Amazon Linux 2023 |
| Public IPv4 address | A valid public IP (e.g., `3.92.145.210`) |

---

### Checkpoint 2 — Security Group Rules

The Inbound rules tab for `week8-sg` should show exactly:

| Type | Protocol | Port range | Source |
|---|---|---|---|
| SSH | TCP | 22 | `<trainee-home-ip>/32` |
| Custom TCP | TCP | 8080 | `0.0.0.0/0` |

Common mistake: trainees leave SSH open to `0.0.0.0/0` (the default). Prompt them to check the source column.

---

### Checkpoint 3 — SSH Success

After a successful `ssh -i ~/.ssh/week8-keypair.pem ec2-user@<IP>` the trainee should see:

```
   ,     #_
   ~\_  ####_
  ~~  \_#####\
  ~~     \###|
  ~~       \#/ ___
   ~~       V~' '->
    ~~~         /
      ~~._.   _/
         _/ _/
       _/m/'
Last login: ...
[ec2-user@ip-172-xx-xx-xx ~]$
```

---

### Checkpoint 4 — Application Started

Expected Spring Boot startup log lines (look for these in order):

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
...
Started ProductCatalogApplication in 4.123 seconds (process running for 4.8)
```

or, for Tomcat-embedded apps:

```
Tomcat started on port(s): 8080 (http) with context path ''
```

---

### Checkpoint 5 — Postman Health Response

Exact expected Postman output:

| Field | Value |
|---|---|
| HTTP Status | `200 OK` |
| Content-Type | `application/json` |
| Body | `{"status":"UP"}` |

Spring Boot Actuator format (if actuator is on the classpath):

```json
{
    "status": "UP"
}
```

Custom health endpoint format (if instructor wrote a simple controller):

```json
{ "status": "UP" }
```

Both are acceptable. If a trainee sees `404`, the endpoint path may differ — check with the instructor what path was implemented.

---

### Checkpoint 6 — Auto Scaling Wizard

The trainee should screenshot a page with these three numeric fields visible:

- Desired capacity (default: 1)
- Minimum capacity (default: 1)
- Maximum capacity (default: 1)

On newer console versions, these may appear as a "Group size" section. Either layout is correct.

---

### Checkpoint 7 — ECS Task Definition Page

The trainee should screenshot the **Container** section of the task definition form, which includes:

- Container name field
- Image URI field (e.g., `123456789.dkr.ecr.us-east-1.amazonaws.com/my-app:latest`)
- Port mappings (container port, protocol)
- Resource limits (CPU, Memory)

---

## Exact Command Templates (Filled In)

### SSH Command

```bash
ssh -i ~/.ssh/week8-keypair.pem ec2-user@3.92.145.210
```

Replace `3.92.145.210` with the trainee's actual public IP.

### SCP Command

```bash
scp -i ~/.ssh/week8-keypair.pem \
    ~/Downloads/product-catalog-api.jar \
    ec2-user@3.92.145.210:~/product-catalog-api.jar
```

The destination path `~/product-catalog-api.jar` expands to `/home/ec2-user/product-catalog-api.jar` on the instance.

---

## Common Trainee Mistakes and Diagnoses

### Mistake 1 — SSH times out immediately

**Symptom:** `ssh: connect to host 3.92.145.210 port 22: Connection timed out`

**Diagnosis:**
1. Security Group SSH rule source is not set to the trainee's current IP.
2. Trainee is on a VPN or a network that has a different public IP than what was configured.
3. Instance is not yet in `running` state.

**Fix:** Ask the trainee to go to the Security Group, edit the SSH rule, and click "My IP" again — their IP may have changed if they are on DHCP or a VPN.

---

### Mistake 2 — Permission denied (publickey)

**Symptom:** `ec2-user@3.92.145.210: Permission denied (publickey).`

**Diagnosis:**
1. Wrong username — trainees sometimes try `ubuntu` or `root`.
2. Wrong key file path in the `-i` flag.
3. Key was not associated with the instance at launch (rare — would mean they chose "Proceed without a key pair").

**Fix:** Confirm the command uses `ec2-user` and that `-i` points to the exact `.pem` downloaded at launch. If they cannot recover, they will need to terminate the instance and launch a new one with the correct key pair.

---

### Mistake 3 — App starts but Postman gets "connection refused"

**Symptom:** Postman shows `Error: connect ECONNREFUSED` or curl hangs.

**Diagnosis:**
1. Security Group does not have the port 8080 inbound rule (forgot Task 2, or saved to the wrong SG).
2. App is listening on a different port (check the JAR's `application.properties`).
3. Trainee is hitting the private IP instead of the public IP.

**Fix:** Check the Security Group inbound rules first. Then confirm the app logs show `Tomcat started on port(s): 8080`. If the app uses a different port, update the Security Group rule and Postman URL.

---

### Mistake 4 — Trainee locked out of SSH (wrong SG rule)

**Scenario:** Trainee accidentally deleted the SSH rule or set the source to an incorrect CIDR, and can no longer SSH in. The app may still be running on 8080 from the previous session.

**Recovery steps (via AWS Console):**
1. Navigate to EC2 → Security Groups → `week8-sg` → Inbound rules → Edit inbound rules.
2. Add (or restore) the SSH rule: Type = SSH, Source = My IP.
3. Save rules. Try SSH again.

No instance reboot is required — Security Group changes take effect immediately.

**If the trainee accidentally deleted the Security Group from the instance:**
1. EC2 → Instances → Select instance → Actions → Security → Change security groups.
2. Re-attach `week8-sg` or create a new SG with the correct rules.

---

### Mistake 5 — App dies when SSH session closes

**Symptom:** Postman worked during the lab, but the next day (or after closing the terminal) the app is gone.

**Cause:** `java -jar` runs in the foreground of the SSH session. When the session ends, the process is killed.

**Fix (quick):** Run with `nohup`:

```bash
nohup java -jar ~/product-catalog-api.jar > ~/app.log 2>&1 &
```

**Fix (proper):** Direct the trainee to Bonus C — configure the app as a `systemd` service. This is the correct production approach.

---

## Notes on Cost Management

- t3.micro is within the AWS Free Tier for the first 12 months of a new account (750 hours/month).
- Trainees should STOP (not terminate) instances between sessions to preserve their configuration.
- EBS volumes for stopped instances still incur a small charge (~$0.10/GB/month for gp3). An 8 GiB volume costs roughly $0.80/month — negligible for the week.
- Warn trainees who create S3 buckets in Bonus B to empty and delete the bucket at the end of the week to avoid storage charges.
