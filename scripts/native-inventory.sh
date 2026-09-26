#!/usr/bin/env bash
# Native inventory (roadmap PR 15 / AGENTS.md "JNI else GitHub API approach").
#
# Read-only: for every bundled shared object under app/src/main/libs/<abi>/ it
# prints size, hash, ELF class and machine, NEEDED/SONAME entries, the number
# of exported symbols, every JNI entry point (Java_*) and every LibreOfficeKit
# export (lok_*, libreofficekit_*), then checks which of the `external fun`
# declarations in LibreOfficeCore.kt have a matching JNI symbol.
#
# Runs in CI after an LFS checkout; on a clone without git-lfs the files are
# pointers and the script says so instead of failing. Needs binutils
# (readelf, nm) and coreutils.
#
# Usage: scripts/native-inventory.sh [output-file]

set -u

ROOT=$(cd "$(dirname "$0")/.." && pwd)
LIBS="${PAPIRUS_LIBS:-$ROOT/app/src/main/libs}"
OUT="${1:-$ROOT/build/native-inventory.txt}"
JNI_SOURCE="$ROOT/app/src/main/java/com/example/core/jni/LibreOfficeCore.kt"
JNI_PREFIX="Java_com_example_core_jni_LibreOfficeCore_"

mkdir -p "$(dirname "$OUT")"

have() { command -v "$1" >/dev/null 2>&1; }

inventory() {
  echo "# native inventory, $(date -u +%Y-%m-%dT%H:%M:%SZ), commit $(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)"
  echo "# libs: $LIBS"
  for tool in readelf nm sha256sum; do
    have "$tool" || echo "# missing tool: $tool (install binutils/coreutils)"
  done

  if [ ! -d "$LIBS" ]; then
    echo "no app/src/main/libs directory"
    return
  fi

  local so abi name size
  for so in "$LIBS"/*/*.so; do
    [ -e "$so" ] || continue
    abi=$(basename "$(dirname "$so")")
    name=$(basename "$so")
    size=$(stat -c %s "$so" 2>/dev/null || wc -c <"$so")
    echo
    echo "== $abi/$name ($size bytes)"
    if head -c 64 "$so" | grep -q "git-lfs"; then
      echo "   git-lfs pointer only; run with an LFS checkout to inspect the binary"
      continue
    fi
    have sha256sum && echo "   sha256 $(sha256sum "$so" | cut -d' ' -f1)"
    if have readelf; then
      readelf -h "$so" 2>/dev/null | awk -F: '/Class|Machine/ {gsub(/^[ \t]+|[ \t]+$/, "", $2); printf "   %s: %s\n", $1, $2}' | sed 's/^   *\([A-Za-z]*\)/   \1/'
      echo "   SONAME: $(readelf -d "$so" 2>/dev/null | awk '/\(SONAME\)/ {print $5}' | tr -d '[]')"
      echo "   NEEDED:"
      readelf -d "$so" 2>/dev/null | awk '/\(NEEDED\)/ {print "     " $5}' | tr -d '[]'
    fi
    if have nm; then
      local exported
      exported=$(nm -D --defined-only "$so" 2>/dev/null | wc -l | tr -d ' ')
      echo "   exported symbols: $exported"
      echo "   JNI entry points (Java_*):"
      nm -D --defined-only "$so" 2>/dev/null | awk '$3 ~ /^Java_/ {print "     " $3}' | sort | head -400
      echo "   LibreOfficeKit exports (lok_*, libreofficekit_*):"
      nm -D --defined-only "$so" 2>/dev/null | awk '$3 ~ /^(lok_|libreofficekit_)/ {print "     " $3}' | sort | head -100
    fi
  done

  echo
  echo "== JNI seam check: external fun in $(basename "$JNI_SOURCE") vs $JNI_PREFIX* in liblo-native-code.so"
  if [ ! -f "$JNI_SOURCE" ]; then
    echo "   source file not found"
    return
  fi
  local lib
  lib=$(ls "$LIBS"/*/liblo-native-code.so 2>/dev/null | head -1)
  if [ -z "$lib" ] || head -c 64 "$lib" | grep -q "git-lfs" || ! have nm; then
    echo "   binary not inspectable here (LFS pointer or nm missing)"
    return
  fi
  local symbols fn
  symbols=$(nm -D --defined-only "$lib" 2>/dev/null | awk '{print $3}')
  grep -oE 'external fun [A-Za-z0-9_]+' "$JNI_SOURCE" | awk '{print $3}' | sort -u | while read -r fn; do
    if printf '%s\n' "$symbols" | grep -qx "${JNI_PREFIX}${fn}"; then
      echo "   present  ${JNI_PREFIX}${fn}"
    else
      echo "   MISSING  ${JNI_PREFIX}${fn}"
    fi
  done
  echo "   (MISSING means the Kotlin side would throw UnsatisfiedLinkError on first call; the loader already treats that as simulated mode)"
}

inventory | tee "$OUT"
echo
echo "written to $OUT"
