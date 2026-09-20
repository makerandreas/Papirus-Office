
> **Status: Phase 6 implemented.** This document records the memory model and
> the untrusted-input guards added to the parsers, and the remaining targets.

## 1. Threat model

Office documents are **ZIP archives from arbitrary sources**. An untrusted file
can:

1. **Zip-bomb** the app: small compressed input -> gigabytes of XML (OOM).
2. **Many-entries** bomb: 100k+ tiny entries (CPU/memory per entry).
3. **Oversized single entry**: one 5 GB `content.xml` (OOM).
4. **Sparse spreadsheet**: absurd column refs / repeated-cell counts that
   explode the parsed model (2^N-ish growth).

All parser paths now enforce hard caps (below) instead of reading unbounded.

## 2. Enforced caps (`data/util/ZipSafe.kt`)

| Constant | Value | Enforced by |
|----------|-------|-------------|
| `MAX_ZIP_ENTRY_BYTES` | 48 MB | `InputStream.readCappedBytes()` — every ZIP entry read |
| `MAX_ZIP_ENTRIES` | 10,000 | `ZipInputStream.nextEntryBudgeted()` — every scan pass |
| `MAX_DOCUMENT_BYTES` | 256 MB | `ZipScanBudget` total-bytes cap per scan pass; SAF/asset reads |
| `MAX_IMAGE_BYTES` | 50 MB | embedded image extraction |
| `MAX_IMAGE_COUNT` | 200 | embedded image extraction |
| `MAX_DOWNLOAD_BYTES` | 100 MB | template/font downloads |
| `MAX_FONT_BYTES` | 30 MB | font downloads |
| `MAX_XLSX_ROWS` | 200,000 | XLSX parsed model (per sheet) |
| `MAX_XLSX_COLUMN_INDEX` | 16,383 (XFD) | XLSX sparse-cell fill loop |

**Rule:** parsers must call `readCappedBytes()` / `copyCappedTo()` /
`nextEntryBudgeted()` — never the unbounded `InputStream.readBytes()` /
`copyTo()`. A `ZipScanBudget` is created per scan pass and threaded through
entry reads and `BudgetedInputStream` (for streamed XmlPullParser input).

## 3. Two-pass streaming (XLSX / PPTX)

The old parser buffered *every* worksheet/slide XML into `packageEntries`
(peak memory = sum of all sheet XMLs). Phase 6 splits parsing into passes:

```
Pass 1 (metadata, small):   sharedStrings.xml, workbook.xml, rels  -> memory
                            inventory of worksheet/slide entry names
Pass 2 (stream, per sheet): for each sheet/slide, open the ZIP entry and
                            stream it directly into XmlPullParser via
                            BudgetedInputStream — no full-XML ByteArray kept
```

Peak memory is now bounded by **one worksheet at a time**, not the workbook.
The parsed *model* is separately bounded by `MAX_XLSX_ROWS`.

### 3.1 Why per-sheet streaming matters

A workbook with 10 sheets x 20 MB XML:
- Before: 200 MB resident simultaneously.
- After: ~20 MB (one sheet stream) + the final row model (capped).

## 4. ODP (Impress) import — pages & shapes

`SvXMLImport`/`SvXMLImportContext` now handle:

- `<draw:page>` -> `OdfSlidePageContext`: emits a `PageBreak` between slides and
  a `SlideTitle` heading (from `draw:name`).
- Drawing containers (`<draw:g>`, `<draw:custom-shape>`, `<draw:text-box>`)
  -> `OdfDrawingContainerContext`, so text inside grouped shapes is recovered.
- `OdfFrameContext` also descends into text/shapes/paragraphs (not just images).
- Page count is derived from slide breaks for ODP documents.

## 5. Spreadsheet (Cellina) table import

- **Repeat expansion**: `table:number-rows-repeated` / `number-columns-repeated`
  are expanded (row copy per repeat; capped to 64 rows / 256 cols non-empty,
  16 empty) to prevent exponential aliasing while keeping tables correct.
- **Table names** (`table:name`) are propagated onto `OfficeDocumentElement.Table`
  and surfaced in plain text as `=== Sheet: <name> ===`.
- **Cell values**: numeric/boolean/date cells fall back to `office:value*`
  attributes when the XML text is empty, so numbers aren't dropped.

## 6. DOCX/ODT (Inky) import

- Heading/style classification regexes are **precompiled** (hoisted to companion
  constants) instead of recompiled per style/paragraph (CPU).
- `OdtDocumentParser` reads entries via `readCappedBytes()`.
- `OfficeDocumentModel` search/replace/cursor methods return **safe no-op stubs**
  instead of throwing `NotImplementedError`, so Find & Replace degrades
  gracefully on the parsed model.

## 7. Undo race fix (PendingTypingBuffer)

`modules/inky/state/PendingTypingBuffer` buffers rapid keystrokes and flushes to
the document + undo stack as coalesced steps, fixing the Ctrl+Z "one undo
deletes a whole line" race:

- Typed characters accumulate in the buffer instead of one undo entry per key.
- On flush/undo boundary, the buffer is committed as **one** undo step.
- `performUndo` / `performRedo` / `performUndoTo` / `performRedoTo` flush the
  buffer first, so the undo stack is never mid-keystroke.

## 8. Log rotation

- `runtime.log` (PapirusLogger) and `crash.log` are rotated: when they exceed
  512 KB they are truncated to their last 256 KB (keeping the file parseable).
  Append-only files can no longer grow unbounded on long-lived installs.

## 9. Remaining / future targets

- [ ] Native LOKit engine memory profiling (see `docs/LOKIT_INTEGRATION.md` §7).
- [ ] Consider `ZipFile` (random access) instead of `ZipInputStream` for the
      common small-document path once streaming is validated (fewer rescans).
- [ ] Add a "malicious document" test fixture (small zip-bomb) to CI that
      asserts the caps throw cleanly without OOM.
- [ ] Cap plain-text materialization (`plainTextBuilder`) for pathological docs.
