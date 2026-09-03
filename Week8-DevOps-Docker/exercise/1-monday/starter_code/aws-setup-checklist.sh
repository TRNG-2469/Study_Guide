#!/usr/bin/env bash
# =============================================================================
# aws-setup-checklist.sh
# Week 8 Monday — Pre-flight environment check
#
# Usage:
#   chmod +x aws-setup-checklist.sh
#   ./aws-setup-checklist.sh
#
# Runs a series of checks to confirm your local environment is ready for the
# AWS Infrastructure lab. Prints a coloured PASS/FAIL summary at the end.
# =============================================================================

set -euo pipefail

# ── Colour codes ─────────────────────────────────────────────────────────────
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
RESET="\033[0m"
BOLD="\033[1m"

pass() { echo -e "  ${GREEN}✅  PASS${RESET}  $1"; }
fail() { echo -e "  ${RED}❌  FAIL${RESET}  $1"; }
info() { echo -e "  ${YELLOW}ℹ️   INFO${RESET}  $1"; }

PASS_COUNT=0
FAIL_COUNT=0

record_pass() { PASS_COUNT=$((PASS_COUNT + 1)); pass "$1"; }
record_fail() { FAIL_COUNT=$((FAIL_COUNT + 1)); fail "$1"; }

echo ""
echo -e "${BOLD}======================================================${RESET}"
echo -e "${BOLD}  Week 8 — AWS Lab Environment Checklist${RESET}"
echo -e "${BOLD}======================================================${RESET}"
echo ""

# ── Check 1: AWS CLI installed ────────────────────────────────────────────────
echo -e "${BOLD}[1/5] AWS CLI installation${RESET}"
if command -v aws &>/dev/null; then
    AWS_VER=$(aws --version 2>&1 | head -1)
    record_pass "AWS CLI found: $AWS_VER"
else
    record_fail "AWS CLI not found. Install from: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"
fi

echo ""

# ── Check 2: AWS credentials configured ──────────────────────────────────────
echo -e "${BOLD}[2/5] AWS credentials (aws sts get-caller-identity)${RESET}"
if command -v aws &>/dev/null; then
    IDENTITY=$(aws sts get-caller-identity 2>&1)
    if echo "$IDENTITY" | grep -q '"UserId"'; then
        ACCOUNT=$(echo "$IDENTITY" | grep '"Account"' | sed 's/.*: "//;s/".*//')
        ARN=$(echo "$IDENTITY" | grep '"Arn"' | sed 's/.*: "//;s/".*//')
        record_pass "Credentials valid. Account: $ACCOUNT"
        info "ARN: $ARN"
    else
        record_fail "Credentials check failed. Run: aws configure"
        info "Error: $IDENTITY"
    fi
else
    record_fail "Skipped — AWS CLI not installed"
fi

echo ""

# ── Check 3: Java installed locally ──────────────────────────────────────────
echo -e "${BOLD}[3/5] Java installation (local machine)${RESET}"
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1)
    record_pass "Java found: $JAVA_VER"
else
    record_fail "Java not found locally. Install from: https://adoptium.net"
    info "Note: Java on your EC2 instance is installed separately in Task 4."
fi

echo ""

# ── Check 4: PEM key file exists ──────────────────────────────────────────────
echo -e "${BOLD}[4/5] PEM key file${RESET}"
echo -n "  Enter the full path to your week8-keypair.pem file (or press Enter to skip): "
read -r PEM_PATH

if [[ -z "$PEM_PATH" ]]; then
    info "Skipped PEM check — enter the path when you have the file."
elif [[ -f "$PEM_PATH" ]]; then
    PERMS=$(stat -c "%a" "$PEM_PATH" 2>/dev/null || stat -f "%A" "$PEM_PATH" 2>/dev/null || echo "unknown")
    if [[ "$PERMS" == "400" || "$PERMS" == "0400" ]]; then
        record_pass "PEM file found with correct permissions (400): $PEM_PATH"
    else
        record_fail "PEM file found but permissions are $PERMS — run: chmod 400 $PEM_PATH"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
else
    record_fail "PEM file not found at: $PEM_PATH"
fi

echo ""

# ── Check 5: SSH connectivity to EC2 ─────────────────────────────────────────
echo -e "${BOLD}[5/5] SSH connectivity to EC2 instance${RESET}"
echo -n "  Enter your EC2 Public IP address (or press Enter to skip): "
read -r EC2_IP

if [[ -z "$EC2_IP" ]]; then
    info "Skipped SSH check — run this script again once your instance is running."
elif [[ -z "${PEM_PATH:-}" || ! -f "${PEM_PATH:-}" ]]; then
    info "Skipped SSH check — valid PEM path required (check 4 must pass first)."
else
    echo "  Testing SSH connection to ec2-user@$EC2_IP (10-second timeout)..."
    SSH_RESULT=$(ssh -i "$PEM_PATH"         -o ConnectTimeout=10         -o StrictHostKeyChecking=no         -o BatchMode=yes         "ec2-user@$EC2_IP"         "echo SSH_OK" 2>&1)

    if echo "$SSH_RESULT" | grep -q "SSH_OK"; then
        record_pass "SSH connection successful to ec2-user@$EC2_IP"
    else
        record_fail "SSH connection failed to $EC2_IP"
        info "Error: $SSH_RESULT"
        info "Troubleshooting: confirm Security Group allows port 22 from your IP"
    fi
fi

echo ""

# ── Summary ───────────────────────────────────────────────────────────────────
echo -e "${BOLD}======================================================${RESET}"
echo -e "${BOLD}  Summary${RESET}"
echo -e "${BOLD}======================================================${RESET}"
echo -e "  ${GREEN}Passed:${RESET} $PASS_COUNT"
echo -e "  ${RED}Failed:${RESET} $FAIL_COUNT"
echo ""

if [[ $FAIL_COUNT -eq 0 ]]; then
    echo -e "  ${GREEN}${BOLD}All checks passed — you are ready for the lab!${RESET}"
else
    echo -e "  ${RED}${BOLD}Fix the failing checks above before starting Task 1.${RESET}"
fi

echo ""
