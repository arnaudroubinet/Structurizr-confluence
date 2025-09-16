#!/usr/bin/env bash
set -euo pipefail

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR="${DIR%/scripts}"

cd "$ROOT_DIR"
exec mvn -B --no-transfer-progress clean install