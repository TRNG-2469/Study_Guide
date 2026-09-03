# Installing Docker

## Learning Objectives
By the end of this lesson, you will be able to:
- Install Docker Desktop on Windows, macOS, and Linux
- Understand the WSL2 backend requirement for Windows
- Verify a successful Docker installation
- Distinguish between Docker Desktop and Docker Engine
- Resolve common installation issues

---

## Why This Matters

A properly installed Docker environment is the foundation for everything else in this program. Installation issues — especially on Windows — are the number-one cause of wasted time on Day 1. Understanding what Docker Desktop actually installs (and why WSL2 is required on Windows) helps you debug problems confidently instead of reinstalling blindly.

---

## Docker Desktop vs. Docker Engine

| Feature | Docker Desktop | Docker Engine |
|---------|---------------|---------------|
| Platform | Windows, macOS, Linux | Linux only |
| GUI | Yes | No |
| Includes Docker Compose | Yes | No (install separately) |
| WSL2 / VM required | Yes (Windows/Mac) | No (runs natively) |
| Best for | Developer workstations | Servers, CI/CD |
| License | Free for personal/small biz | Apache 2.0 (free) |

**Use Docker Desktop for development on Windows or Mac. Use Docker Engine on Linux servers.**

---

## Installing on Windows

### Prerequisites

1. **Windows 10 version 1903+** or **Windows 11**
2. **WSL2 enabled** (Windows Subsystem for Linux, version 2)
3. Hardware virtualization enabled in BIOS

### Step 1: Enable WSL2

```powershell
# Run in PowerShell as Administrator
wsl --install

# This installs WSL2 + Ubuntu by default
# Restart your computer after this step
```

If WSL is already installed, upgrade it:

```powershell
wsl --set-default-version 2
wsl --update
```

### Step 2: Download Docker Desktop

1. Visit [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)
2. Click **Download for Windows**
3. Run the installer (`Docker Desktop Installer.exe`)
4. During installation, ensure **Use WSL 2 instead of Hyper-V** is checked
5. Click **OK** and wait for installation to complete
6. Restart your computer

### Step 3: Start Docker Desktop

After restart, Docker Desktop launches automatically. Wait for the Docker icon in the system tray to show a green "Running" state (this may take 30-60 seconds on first launch).

### Why WSL2?

Docker containers use Linux kernel features (namespaces, cgroups). On Windows, Docker runs a lightweight Linux VM using WSL2 as the backend. WSL2 is:
- Faster than the old Hyper-V backend
- Better file system performance
- Required for Docker Desktop on Windows 10/11

---

## Installing on macOS

### Prerequisites

- macOS 12 (Monterey) or newer
- Apple Silicon (M1/M2/M3) or Intel Mac

### Steps

1. Visit [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)
2. Choose **Mac with Apple Silicon** or **Mac with Intel Chip**
3. Open the downloaded `.dmg` file
4. Drag **Docker** to your **Applications** folder
5. Open Docker from Applications
6. Accept the Terms of Service
7. Wait for Docker to start (whale icon in menu bar)

> **Apple Silicon Note**: Docker Desktop uses Rosetta 2 or native ARM images. Most official Docker Hub images now provide `linux/arm64` variants, so you rarely hit compatibility issues.

---

## Installing Docker Engine on Linux (Ubuntu/Debian)

On Linux servers and WSL2 Ubuntu, you install **Docker Engine** (no GUI needed).

```bash
# Step 1: Remove old versions
sudo apt-get remove docker docker-engine docker.io containerd runc

# Step 2: Set up the repository
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg |   sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Step 3: Install Docker Engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Step 4: Start and enable the service
sudo systemctl start docker
sudo systemctl enable docker

# Step 5: Run Docker as non-root (optional but recommended)
sudo usermod -aG docker $USER
newgrp docker
```

---

## Verifying Your Installation

Run these commands to confirm Docker is working correctly:

```bash
# Check Docker client and server versions
docker version
# Expected output:
# Client: Docker Engine - Community
#  Version: 26.x.x
# Server: Docker Desktop 4.x.x (or Docker Engine on Linux)
#  Engine:
#   Version: 26.x.x

# Check system-wide info (daemon status, storage driver, etc.)
docker info
# Shows: containers running, images cached, storage driver, OS, architecture

# Run the official hello-world image to confirm everything works end-to-end
docker run hello-world
# Expected: "Hello from Docker!" message
```

If `docker version` shows both Client and Server info, Docker is correctly installed. If you only see Client info, the daemon is not running.

---

## Common Installation Issues

### Issue: "Cannot connect to the Docker daemon"
```
Error response from daemon: Cannot connect to the Docker daemon at
unix:///var/run/docker.sock. Is the docker daemon running?
```
**Fix (Linux)**: `sudo systemctl start docker`
**Fix (Windows/Mac)**: Open Docker Desktop from the Start Menu or Applications folder and wait for it to fully start.

### Issue: "permission denied while trying to connect to Docker daemon socket"
**Fix (Linux)**: Add your user to the docker group:
```bash
sudo usermod -aG docker $USER
# Log out and back in, or run:
newgrp docker
```

### Issue: WSL2 kernel update required (Windows)
Docker Desktop shows a message asking you to install the WSL2 kernel update.
**Fix**: Download and install the update from: https://aka.ms/wsl2kernel
Then restart Docker Desktop.

### Issue: Virtualization not enabled (Windows)
**Fix**: Restart computer, enter BIOS (usually F2 or Delete key), find "Virtualization Technology" or "Intel VT-x" and enable it.

### Issue: Docker Desktop slow to start on Mac
This is normal on first launch. Docker Desktop starts a Linux VM. Subsequent starts are faster. If it hangs for more than 5 minutes, try: Docker Desktop menu > Troubleshoot > Restart.

---

## Checking Docker Desktop Settings

Once installed, explore these important Docker Desktop settings:

- **Resources**: Adjust CPU cores, RAM, and disk space allocated to Docker
- **WSL Integration** (Windows): Choose which WSL2 distros can use Docker
- **Kubernetes**: Enable the built-in single-node Kubernetes cluster
- **Docker Scout**: Image vulnerability scanning

---

## Summary

| Platform | Method | Key Requirement |
|----------|--------|-----------------|
| Windows | Docker Desktop | WSL2 backend |
| macOS | Docker Desktop | No special requirements |
| Linux Server | Docker Engine | Kernel 3.10+ |
| Linux Desktop | Docker Desktop or Engine | Either works |

Always verify with `docker version`, `docker info`, and `docker run hello-world` before proceeding to build anything.

---

## External Resources

- [Install Docker Desktop on Windows (Official)](https://docs.docker.com/desktop/install/windows-install/)
- [Install Docker Desktop on Mac (Official)](https://docs.docker.com/desktop/install/mac-install/)
- [Install Docker Engine on Ubuntu (Official)](https://docs.docker.com/engine/install/ubuntu/)
