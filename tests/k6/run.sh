#!/usr/bin/env sh
set -eu

SCRIPT="${1:-smoke.js}"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

docker run --rm -i \
  -v "${SCRIPT_DIR}:/scripts:ro" \
  -e FB_API_URL="${FB_API_URL:-http://host.docker.internal:9999}" \
  -e FB_MANAGEMENT_URL="${FB_MANAGEMENT_URL:-http://host.docker.internal:8085/fb-management/v1}" \
  -e FB_API_TOKEN="${FB_API_TOKEN:-${FB_TOKEN:-}}" \
  -e FB_MANAGEMENT_TOKEN="${FB_MANAGEMENT_TOKEN:-${FB_TOKEN:-}}" \
  -e FB_ACTIVATION_WAIT_SECONDS="${FB_ACTIVATION_WAIT_SECONDS:-5}" \
  -e FB_CLEANUP="${FB_CLEANUP:-false}" \
  -e FB_PARTY_ID="${FB_PARTY_ID:-k6-load-party}" \
  -e FB_SHOP_ID="${FB_SHOP_ID:-k6-load-shop}" \
  -e FB_TEMPLATE_ID="${FB_TEMPLATE_ID:-k6-load-template}" \
  -e FB_RATE="${FB_RATE:-20}" \
  -e FB_DURATION="${FB_DURATION:-2m}" \
  -e FB_PREALLOCATED_VUS="${FB_PREALLOCATED_VUS:-20}" \
  -e FB_MAX_VUS="${FB_MAX_VUS:-100}" \
  -e FB_INSPECT_RATE="${FB_INSPECT_RATE:-20}" \
  -e FB_INGEST_RATE="${FB_INGEST_RATE:-5}" \
  -e FB_MANAGEMENT_VUS="${FB_MANAGEMENT_VUS:-2}" \
  grafana/k6:latest run "/scripts/${SCRIPT}"

