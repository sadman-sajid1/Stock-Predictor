#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  StockPredictor – build & run helper
#  Usage:
#    ./run.sh                          # interactive demo
#    ./run.sh AAPL 5                   # predict AAPL 5 days ahead
#    ./run.sh --csv AAPL data/AAPL.csv # use real CSV
# ─────────────────────────────────────────────────────────────

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_ROOT/src/main/java"
OUT_DIR="$PROJECT_ROOT/out"
RESOURCES="$PROJECT_ROOT/src/main/resources"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Building Stock Price Predictor..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

mkdir -p "$OUT_DIR"

# Compile all Java sources
find "$SRC_DIR" -name "*.java" | xargs javac -d "$OUT_DIR" -cp "$SRC_DIR"

# Copy resources
cp -r "$RESOURCES/"* "$OUT_DIR/" 2>/dev/null || true

echo "  ✅ Build successful!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Running..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

cd "$OUT_DIR"
java com.stockpredictor.Main "$@"
