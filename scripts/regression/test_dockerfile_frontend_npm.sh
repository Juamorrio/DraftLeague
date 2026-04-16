#!/usr/bin/env bash
# Regression test for Dockerfile.frontend.
#
# Context:
#   Commit 2c0771b ("fix: :bug: fixed error in npm install") replaced the
#   `npm install` step in Dockerfile.frontend with `npm ci` and started
#   copying frontend/package-lock.json into the builder stage. The previous
#   `npm install`-based build failed with an `ERESOLVE` peer-dependency
#   conflict (react vs. react-test-renderer transitive peers).
#
# This script reproduces the original failure and verifies that the current
# `npm ci`-based Dockerfile.frontend does not regress.
#
# It is intended to be run from the repository root, with Docker available.
#
# Exit codes:
#   0  both scenarios behaved as expected
#   1  regression: either the old-style build unexpectedly succeeded, or the
#      current Dockerfile.frontend failed to build

set -u
set -o pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

DOCKERFILE_BROKEN="$TMP_DIR/Dockerfile.frontend.npm-install"
DOCKERFILE_FIXED="Dockerfile.frontend"

BROKEN_BUILD_LOG="$TMP_DIR/broken-build.log"
FIXED_BUILD_LOG="$TMP_DIR/fixed-build.log"

log()  { printf '\n[regression] %s\n' "$*"; }
fail() { printf '\n[regression][FAIL] %s\n' "$*" >&2; exit 1; }
pass() { printf '[regression][PASS] %s\n' "$*"; }

if ! command -v docker >/dev/null 2>&1; then
    fail "docker is required to run this regression test"
fi

if [[ ! -f "$DOCKERFILE_FIXED" ]]; then
    fail "expected $DOCKERFILE_FIXED in repo root"
fi

if [[ ! -f "frontend/package.json" || ! -f "frontend/package-lock.json" ]]; then
    fail "expected frontend/package.json and frontend/package-lock.json"
fi

# ---------------------------------------------------------------------------
# 1. Reproduce the original failure: copy only package.json and run
#    `npm install`. This mirrors Dockerfile.frontend at commit 0fac673.
# ---------------------------------------------------------------------------
cat > "$DOCKERFILE_BROKEN" <<'DOCKERFILE'
# Intentionally-broken Dockerfile used by the regression test to reproduce
# the pre-fix behavior of Dockerfile.frontend. Do NOT use in production.
FROM node:20-alpine AS builder
WORKDIR /app
COPY frontend/package.json ./package.json
RUN npm install
DOCKERFILE

log "Building intentionally-broken image with 'npm install' (no lockfile)..."
set +e
docker build \
    --no-cache \
    --pull=false \
    -f "$DOCKERFILE_BROKEN" \
    -t draftleague-frontend-regression-broken \
    . >"$BROKEN_BUILD_LOG" 2>&1
BROKEN_EXIT=$?
set -e

if [[ $BROKEN_EXIT -eq 0 ]]; then
    cat "$BROKEN_BUILD_LOG"
    fail "'npm install' unexpectedly succeeded; the original bug is no longer reproducible. Update this regression test."
fi

if ! grep -q "ERESOLVE" "$BROKEN_BUILD_LOG"; then
    cat "$BROKEN_BUILD_LOG"
    fail "'npm install' failed but not with the expected ERESOLVE peer-dependency error"
fi

pass "Reproduced original failure: 'npm install' exits non-zero with ERESOLVE"

# ---------------------------------------------------------------------------
# 2. Verify that the current Dockerfile.frontend (npm ci + lockfile) builds
#    the builder stage successfully.
# ---------------------------------------------------------------------------
log "Building current Dockerfile.frontend builder stage with 'npm ci'..."
set +e
docker build \
    --pull=false \
    --target builder \
    -f "$DOCKERFILE_FIXED" \
    -t draftleague-frontend-regression-fixed \
    . >"$FIXED_BUILD_LOG" 2>&1
FIXED_EXIT=$?
set -e

if [[ $FIXED_EXIT -ne 0 ]]; then
    cat "$FIXED_BUILD_LOG"
    fail "current Dockerfile.frontend failed to build; the 'npm ci' fix regressed"
fi

if grep -q "ERESOLVE" "$FIXED_BUILD_LOG"; then
    cat "$FIXED_BUILD_LOG"
    fail "current Dockerfile.frontend build logs still contain ERESOLVE errors"
fi

pass "Current Dockerfile.frontend builds successfully with 'npm ci'"

log "Regression test passed: 'npm install' reproduces the original bug and 'npm ci' prevents it."
