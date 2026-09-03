#!/usr/bin/env bash
# =============================================================================
# health-check.sh
# Week 8 Monday — Remote health check for deployed Spring Boot API
#
# Usage:
#   chmod +x health-check.sh
#
#   # Pass EC2 IP as argument:
#   ./health-check.sh 3.92.145.210
#
#   # Or set as environment variable:
#   export EC2_IP=3.92.145.210
#   ./health-check.sh
#
# Exit codes:
#   0 — HTTP 200 received (app is healthy)
#   1 — Non-200 response or connection failed
# =============================================================================

set -euo pipefail

# ── Colour codes ─────────────────────────────────────────────────────────────
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
RESET="\033[0m"
BOLD="\033[1m"

TIMEOUT=10
PORT=8080
ENDPOINT="/health"

# ── Resolve EC2 IP ────────────────────────────────────────────────────────────
# Accept the IP as the first argument, or fall back to the EC2_IP env variable.
if [[ $# -ge 1 ]]; then
    EC2_IP="$1"
elif [[ -n "${EC2_IP:-}" ]]; then
    EC2_IP="${EC2_IP}"
else
    echo -e "${RED}Error:${RESET} No EC2 IP provided."
    echo ""
    echo "Usage:"
    echo "  ./health-check.sh <EC2-PUBLIC-IP>"
    echo "  EC2_IP=<EC2-PUBLIC-IP> ./health-check.sh"
    exit 1
fi

TARGET_URL="http://${EC2_IP}:${PORT}${ENDPOINT}"

echo ""
echo -e "${BOLD}======================================================${RESET}"
echo -e "${BOLD}  Spring Boot Health Check${RESET}"
echo -e "${BOLD}======================================================${RESET}"
echo -e "  Target : ${YELLOW}${TARGET_URL}${RESET}"
echo -e "  Timeout: ${TIMEOUT}s"
echo ""

# ── Run curl ──────────────────────────────────────────────────────────────────
# -s          silent (no progress bar)
# -o          write response body to a temp file
# -w          write HTTP status code to stdout
# --max-time  connection + transfer timeout in seconds
RESPONSE_FILE=$(mktemp)

HTTP_STATUS=$(curl -s     --max-time "$TIMEOUT"     -o "$RESPONSE_FILE"     -w "%{http_code}"     "$TARGET_URL" 2>&1) || CURL_EXIT=$?

RESPONSE_BODY=$(cat "$RESPONSE_FILE")
rm -f "$RESPONSE_FILE"

# ── Evaluate result ───────────────────────────────────────────────────────────
echo -e "  ${BOLD}HTTP Status:${RESET}   $HTTP_STATUS"
echo -e "  ${BOLD}Response Body:${RESET}"
echo ""

if [[ -n "$RESPONSE_BODY" ]]; then
    echo "  $RESPONSE_BODY"
else
    echo -e "  ${YELLOW}(empty response body)${RESET}"
fi

echo ""
echo -e "${BOLD}======================================================${RESET}"

if [[ "$HTTP_STATUS" == "200" ]]; then
    echo -e "  ${GREEN}${BOLD}HEALTHY${RESET} — App is up and responding correctly."
    echo ""
    exit 0
else
    echo -e "  ${RED}${BOLD}UNHEALTHY${RESET} — Expected HTTP 200, got: $HTTP_STATUS"
    echo ""
    echo "  Troubleshooting steps:"
    echo "  1. Confirm the Spring Boot app is running on the EC2 instance (Task 4)"
    echo "  2. Confirm Security Group allows port 8080 from 0.0.0.0/0 (Task 2)"
    echo "  3. Confirm the EC2 instance is in 'running' state"
    echo "  4. Try the URL directly in your browser: $TARGET_URL"
    echo ""
    exit 1
fi
