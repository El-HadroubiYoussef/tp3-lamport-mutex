#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
[ -d out ] || bash build.sh
exec java -cp out tp3.partieA.LamportExclusion
