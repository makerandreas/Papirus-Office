#!/usr/bin/env python3
"""Builds the pull-request comment the CI test job posts after running.

The branch is worked from a sandbox that can reach api.github.com but not
the Actions log and artifact storage (AGENTS.md, "GitHub API approach"),
so the things a developer normally reads in the log are mirrored into one
comment: Gradle compile errors, failing tests with their messages, the
plan 5 element dump (`Plan5ElementDumpTest` system-out) and the native
inventory. Everything is best-effort; a missing input just leaves its
section out. Output goes to stdout as Markdown.

Usage: ci-dump-comment.py <test-results-dir> <gradle-log> <native-inventory>
"""
import glob
import html
import os
import re
import sys
import xml.etree.ElementTree as ET

COMMENT_LIMIT = 60000
DUMP_CLASS = "com.example.Plan5ElementDumpTest"


def read(path):
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            return f.read()
    except OSError:
        return ""


def details(summary, body, lang=""):
    return f"<details><summary>{summary}</summary>\n\n```{lang}\n{body.rstrip()}\n```\n\n</details>\n"


def gradle_section(log):
    if not log:
        return "", False
    lines = log.splitlines()
    errors = [l for l in lines if re.match(r"^e: ", l) or "error:" in l.lower() and ".kt" in l]
    failed_tasks = [l for l in lines if l.startswith("> Task") and l.rstrip().endswith("FAILED")]
    ok = any("BUILD SUCCESSFUL" in l for l in lines)
    parts = []
    if errors:
        parts.append(details(f"Kotlin compile errors ({len(errors)})", "\n".join(errors[:200])))
    if failed_tasks:
        parts.append("Failed tasks: " + ", ".join(t.replace("> Task ", "") for t in failed_tasks) + "\n")
    if not ok:
        # The "FAILURE:" block near the end names the cause when tests did not run at all.
        idx = max((i for i, l in enumerate(lines) if l.startswith("FAILURE:")), default=None)
        if idx is not None:
            parts.append(details("Gradle failure block", "\n".join(lines[idx:idx + 60])))
        elif not errors:
            parts.append(details("Gradle log tail", "\n".join(lines[-80:])))
    return "".join(parts), ok


def test_sections(results_dir):
    rows = []
    failures = []
    dump = ""
    total = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for path in sorted(glob.glob(os.path.join(results_dir, "TEST-*.xml"))):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        name = root.get("name", os.path.basename(path))
        counts = {k: int(root.get(k, "0") or 0) for k in total}
        for k in total:
            total[k] += counts[k]
        rows.append((name.replace("com.example.", ""), counts))
        for case in root.iter("testcase"):
            for tag in ("failure", "error"):
                node = case.find(tag)
                if node is not None:
                    message = node.get("message") or ""
                    text = (node.text or "").strip()
                    body = message if message else text
                    if text and text not in body:
                        body = body + "\n" + text
                    failures.append((name.replace("com.example.", ""), case.get("name", "?"), html.unescape(body)[:4000]))
        if name == DUMP_CLASS:
            out = root.find("system-out")
            if out is not None and out.text:
                dump = out.text
    return rows, total, failures, dump


def main():
    results_dir = sys.argv[1] if len(sys.argv) > 1 else "app/build/test-results/testDebugUnitTest"
    gradle_log = read(sys.argv[2]) if len(sys.argv) > 2 else ""
    inventory = read(sys.argv[3]) if len(sys.argv) > 3 else ""

    run_id = os.environ.get("GITHUB_RUN_ID", "?")
    sha = os.environ.get("GITHUB_SHA", "?")[:7]
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    run_url = f"https://github.com/{repo}/actions/runs/{run_id}" if repo else ""

    gradle_md, gradle_ok = gradle_section(gradle_log)
    rows, total, failures, dump = test_sections(results_dir)

    head = [f"### CI report for `{sha}` ([run {run_id}]({run_url}))\n"]
    if rows:
        verdict = "all tests passed" if total["failures"] == 0 and total["errors"] == 0 else "there are failing tests"
        head.append(
            f"Unit tests: {total['tests']} run, {total['failures']} failed, {total['errors']} errors, "
            f"{total['skipped']} skipped; {verdict}.\n"
        )
    elif gradle_log:
        head.append("No test results were written" + ("; the Gradle log says why below.\n" if not gradle_ok else ".\n"))
    else:
        head.append("No test results and no Gradle log found.\n")

    sections = []
    if gradle_md:
        sections.append(gradle_md)
    if failures:
        body = "\n\n".join(f"{cls}.{case}\n{msg}" for cls, case, msg in failures)
        sections.append(details(f"Failing tests ({len(failures)})", body))
    if rows:
        table = ["| class | tests | failed | errors | skipped |", "|---|---|---|---|---|"]
        for name, c in rows:
            table.append(f"| {name} | {c['tests']} | {c['failures']} | {c['errors']} | {c['skipped']} |")
        sections.append(details("Per-class results", "\n".join(table), lang=""))
    if dump:
        sections.append(details("Plan 5 element dump (Plan5ElementDumpTest system-out)", dump))
    if inventory:
        # Keep the header, liblo-native-code entries and the seam check; the NSS chain is noise here.
        keep = []
        current_keep = True
        for line in inventory.splitlines():
            if line.startswith("== "):
                current_keep = "liblo-native-code" in line or "JNI seam" in line
            elif line.startswith("# "):
                current_keep = True
            if current_keep:
                keep.append(line)
        sections.append(details("Native inventory (liblo-native-code and JNI seam; full file in the artifact)", "\n".join(keep)))

    text = "".join(head) + "\n" + "\n".join(sections)
    if len(text) > COMMENT_LIMIT:
        # Drop from the end (inventory, dump) until it fits; say so.
        note = "\n_(comment truncated to fit; the full output is in the unit-test-reports artifact)_\n"
        text = text[: COMMENT_LIMIT - len(note) - 20]
        # Close any open code fence / details so the rest of the page renders.
        if text.count("```") % 2 == 1:
            text += "\n```\n"
        if text.count("<details>") > text.count("</details>"):
            text += "\n</details>\n"
        text += note
    sys.stdout.write(text)


if __name__ == "__main__":
    main()
