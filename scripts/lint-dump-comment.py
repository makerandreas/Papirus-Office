#!/usr/bin/env python3
"""Builds the pull-request comment the CI lint job posts after running.

`app/build.gradle.kts` sets `lint { abortOnError = false }`, so a lint error
does not fail the job and nothing in the run status says one happened. The
report itself only reaches the `lint-reports` artifact, which the sandbox this
repository is worked from cannot open (AGENTS.md, "Reading the CI report").
This mirrors the errors into a comment, the same way `ci-dump-comment.py`
mirrors the compiler output.

Reads the lint XML report first because it carries the issue id, severity and
location as attributes; falls back to the text report when the XML is missing.

Environment the workflow supplies: `GITHUB_RUN_ID`, `GITHUB_REPOSITORY`,
`GITHUB_SHA` and, on pull-request runs, `HEAD_SHA`, `HEAD_REF`, `BASE_SHA` and
`BASE_REF`. `HEAD_SHA` is the pull-request head commit and is what the comment
header names.

Usage: lint-dump-comment.py <lint-results.xml> <lint-results.txt>
"""
import html
import os
import re
import sys
import xml.etree.ElementTree as ET

COMMENT_LIMIT = 60000
MAX_ERRORS_LISTED = 200


def read(path):
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            return f.read()
    except OSError:
        return ""


def details(summary, body, lang=""):
    return f"<details><summary>{summary}</summary>\n\n```{lang}\n{body.rstrip()}\n```\n\n</details>\n"


def repo_relative(path):
    """Lint writes paths absolute from the runner or relative to the module.

    A reader in a sandbox has the checkout at some other root, so the
    repo-relative `app/src/...` form is the one that can be opened or grepped.
    """
    if not path:
        return ""
    path = path.replace("\\", "/")
    marker = "/app/"
    idx = path.rfind(marker)
    if idx >= 0:
        return path[idx + 1:]
    # Module-relative, which is how AGP writes it once the report is not
    # rooted at the runner's checkout.
    if re.match(r"^(src|build|libs)/", path):
        return "app/" + path
    return path


def location_text(location):
    file_name = repo_relative(location.get("file", ""))
    line = location.get("line", "")
    column = location.get("column", "")
    where = file_name or "?"
    if line:
        where += f":{line}"
        if column:
            where += f":{column}"
    return where


def from_xml(text):
    """Returns (errors, warnings, by_id, tool) or None when the XML is unusable.

    errors and warnings are lists of `path:line [Id] message`; by_id maps an
    issue id to (severity, count, summary).
    """
    try:
        root = ET.fromstring(text)
    except ET.ParseError:
        return None
    if root.tag != "issues":
        return None
    errors, warnings, by_id = [], [], {}
    for issue in root.findall("issue"):
        issue_id = issue.get("id", "?")
        severity = issue.get("severity", "?")
        message = html.unescape(issue.get("message", "")).strip()
        summary = html.unescape(issue.get("summary", "")).strip()
        locations = issue.findall("location")
        where = location_text(locations[0]) if locations else "?"
        line = f"{where} [{issue_id}] {message}"
        if severity.lower() in ("error", "fatal"):
            errors.append(line)
        else:
            warnings.append(line)
        entry = by_id.setdefault(issue_id, [severity, 0, summary])
        entry[1] += 1
    by = root.get("by", "").strip()
    tool = by or "lint"
    variant = root.get("variant", "")
    if variant and variant not in tool:
        tool += f" ({variant})"
    return errors, warnings, by_id, tool


def from_text(text):
    """Fallback for the human-readable report: `path:line: Severity: message [Id]`."""
    errors, warnings, by_id = [], [], {}
    pattern = re.compile(r"^(?P<where>[^\s].*?:\d+(?::\d+)?):\s*(?P<sev>Error|Warning|Information|Ignore):\s*(?P<msg>.*?)\s*\[(?P<id>[A-Za-z0-9]+)\]\s*$")
    for raw in text.splitlines():
        match = pattern.match(raw.strip())
        if not match:
            continue
        issue_id = match.group("id")
        severity = match.group("sev")
        where = repo_relative(match.group("where"))
        line = f"{where} [{issue_id}] {match.group('msg')}"
        if severity == "Error":
            errors.append(line)
        else:
            warnings.append(line)
        entry = by_id.setdefault(issue_id, [severity, 0, ""])
        entry[1] += 1
    if not errors and not warnings:
        return None
    return errors, warnings, by_id, "lint"


def main():
    xml_text = read(sys.argv[1]) if len(sys.argv) > 1 else ""
    txt_text = read(sys.argv[2]) if len(sys.argv) > 2 else ""

    parsed = from_xml(xml_text) if xml_text else None
    if parsed is None:
        parsed = from_text(txt_text) if txt_text else None

    run_id = os.environ.get("GITHUB_RUN_ID", "?")
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    run_url = f"https://github.com/{repo}/actions/runs/{run_id}" if repo else ""
    head_sha = (os.environ.get("HEAD_SHA") or os.environ.get("GITHUB_SHA") or "?")[:7]
    head_ref = os.environ.get("HEAD_REF", "")
    base_sha = (os.environ.get("BASE_SHA") or "")[:7]
    base_ref = os.environ.get("BASE_REF", "")

    head = [f"### Lint report for `{head_sha}` ([run {run_id}]({run_url}))\n"]
    if head_ref:
        provenance = f"Head `{head_sha}` on `{head_ref}`"
        if base_sha:
            provenance += f", merged onto `{base_ref or 'base'}` at `{base_sha}` for this run"
        head.append(provenance + ".\n")

    if parsed is None:
        head.append(
            "No lint report was written. `./gradlew :app:lintDebug` produced neither "
            "`app/build/reports/lint-results-debug.xml` nor the `.txt` report, so this "
            "run says nothing about lint.\n"
        )
        sys.stdout.write("".join(head))
        return

    errors, warnings, by_id, tool = parsed
    if errors:
        verdict = (
            "`abortOnError = false`, so this job stays green with errors present; "
            "the errors below are real and unfixed."
        )
    else:
        verdict = (
            "`abortOnError = false`, so a lint error would not fail this job; "
            "this run reported none."
        )
    head.append(
        f"{tool}: {len(errors)} errors, {len(warnings)} warnings across "
        f"{len(by_id)} issue ids. {verdict}\n"
    )

    sections = []
    if errors:
        body = "\n".join(errors[:MAX_ERRORS_LISTED])
        label = f"Lint errors ({len(errors)}"
        if len(errors) > MAX_ERRORS_LISTED:
            label += f", first {MAX_ERRORS_LISTED} shown"
        sections.append(details(label + ")", body))
    if by_id:
        rows = ["| issue id | severity | count | summary |", "|---|---|---|---|"]
        for issue_id, (severity, count, summary) in sorted(by_id.items(), key=lambda kv: (-kv[1][1], kv[0])):
            rows.append(f"| {issue_id} | {severity} | {count} | {summary or '(no summary in the text report)'} |")
        sections.append(details(f"Issue ids ({len(by_id)})", "\n".join(rows)))
    if warnings:
        body = "\n".join(warnings[:MAX_ERRORS_LISTED])
        label = f"Lint warnings ({len(warnings)}"
        if len(warnings) > MAX_ERRORS_LISTED:
            label += f", first {MAX_ERRORS_LISTED} shown"
        sections.append(details(label + ")", body))

    text = "".join(head) + "\n" + "\n".join(sections)
    if len(text) > COMMENT_LIMIT:
        note = "\n_(comment truncated to fit; the full report is in the lint-reports artifact)_\n"
        text = text[: COMMENT_LIMIT - len(note) - 20]
        if text.count("```") % 2 == 1:
            text += "\n```\n"
        if text.count("<details>") > text.count("</details>"):
            text += "\n</details>\n"
        text += note
    sys.stdout.write(text)


if __name__ == "__main__":
    main()
