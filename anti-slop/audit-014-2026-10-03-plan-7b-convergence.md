# Audit 014 (2026-10-03): Plan 7B canonical semantic model and ODT importer convergence

**Baseline:** `main` `c475f138ec56193b20d9594ca4d58e272533a074` (Plan 12A and Plan 7A complete), branch `arena/01a100a2-papirus-office`.
**Scope:** Plan 7B only: the shared semantic boundary, one ODT package/import pipeline, compatibility-parser delegation, explicit save capability, and regression gates. Authored-index/section behavior, table layout, and font alias calibration remain later plans.
**Method:** `antislop` DURING mode; code-path tracing from all ODT entry points; raw ZIP/XML inventory of the six `tests/inky` ODT fixtures; ODF 1.4 Part 3 schema checks; LibreOffice and Collabora architecture review; Open XML SDK typed-model review; and GitHub Actions for compilation/tests because this sandbox has no JDK.

## 1. Binding four-plan split

The former combined Plan 7B package is superseded by four plans and exactly four PRs:

| Plan | Ownership | Explicit non-goals |
|---|---|---|
| **7B** | Canonical semantic model and authoritative ODT package/import path; legacy facade delegation; diagnostics seam; fail-closed structural save capability | No authored index/section parsing or UI; no table measurement/rendering; no font alias calibration |
| **7C** | Authored index snapshots, named section ranges, Navigator rows/jumps, and status context | No table layout or font calibration |
| **7D** | Table grid/style import, intrinsic measurement, row pagination, rendering, and cell hit-testing | No font alias calibration |
| **7E** | ODF font-face declarations/aliases, resolver integration, and final pagination calibration | No writer regeneration; structural writing remains Plan 9 |

The dependency order is **7B -> 7C -> 7D -> 7E -> 8A -> 8B -> 9**. Forecast slots are 7B `#27`, 7C `#28`, 7D `#29`, 7E `#30`, 8A `#31`, 8B `#32`, Plan 9 `#33`, Plan 10 `#34-#35`, and Plan 11 `#36-#40`. Plan IDs are authoritative; PR numbers are forecasts, not reservations.

## 2. The duplicated importer defect

Before 7B, production/runtime ODT open used `SvXMLImport`, while public and serializer-facing `writer.OdtDocumentParser` contained 741 lines of separate XML and ZIP semantics. The two paths disagreed on styles, list resolution, run formatting, package state, media, metadata, and failure handling. `SvXMLImport` itself accepted Android `Context` only to call `DocumentParsingLogger`, preventing a pure shared importer.

7B replaces that split with:

1. `OdtImportPipeline`, the single ZIP reader and semantic orchestrator.
2. `SvXMLImport`, still the authoritative element/style importer, now consuming `OdfImportDiagnostics` rather than storing Android `Context`.
3. `AndroidOdfImportDiagnostics`, the runtime adapter to `DocumentParsingLogger`, plus `SilentOdfImportDiagnostics` for pure callers.
4. Runtime `OfficeDocumentParser` routing ODT only through `OdtImportPipeline`; ODS and ODP remain on their existing paths.
5. `writer.OdtDocumentParser` reduced to a compatibility facade over the pipeline, with no XML parser or independent semantic methods.

The pipeline preserves the full package-entry map, exact original XML parts, extracted-media mappings, document styles, bookmarks, metadata/statistics, failure state, and source-feature provenance. An ODT with no `content.xml` remains a valid blank template package; an empty/invalid package fails explicitly.

## 3. Canonical semantic boundary

`DocumentSemantics.kt` introduces format-neutral value types without implementing later-plan behavior:

- half-open `BodyElementRange` sidecars for authored indexes and named sections;
- `DocumentIndexRange`/`DocumentIndexEntry` and `DocumentSectionRange` identity/provenance;
- table column width, row, cell occupancy/span/repeat, box, border, padding, and vertical-alignment values;
- `OfficeFontFace` declaration metadata;
- `OdtSourceFeatures` for save-capability provenance.

Both `OfficeParsedDocument` and `OfficeDocument` carry the sidecars. Parsed-to-canonical and canonical-to-parsed adapters preserve metadata, styles, bookmarks, runs, images, page breaks, sidecars, and the full table value model. The richer table and font fields are storage seams only: 7B does not parse table grids or font declarations into them.

The range sidecar design is deliberate. Index and section ranges point into the normal body flow rather than nesting duplicate body trees. This lets 7C add identity and navigation without forcing `LayoutEngine` to learn a second content topology.

## 4. Save capability closes the known lossy path

Unmodified imported ODTs still reuse the exact original `content.xml` and preserved package entries. Modified documents previously regenerated a simplified body even when that discarded authored indexes, sections, or source table structure.

`OdtDocumentWriter.saveCapability(document)` now reports `OdtSaveCapability(isSupported, blockingFeatures)`. `write()` enforces the same decision and throws `UnsupportedOdtStructureException` before writing bytes when a modified document contains any of the unsupported structural classes recorded by `OdtSourceFeatures` or by canonical sidecars/table values. This is conservative by design. Plan 9 owns structural regeneration; 7B only prevents silent loss.

## 5. Regression gates

`Plan7bSemanticImportTest` adds these gates:

1. runtime parser and compatibility facade produce the same normalized semantic result for all six ODT fixtures;
2. title/author/statistics and the reference page count survive `meta.xml` import;
3. source-feature inventory is derived from the package once;
4. canonical index/section sidecars, font-face storage, and rich table values survive the adapter;
5. malformed ZIP input fails through a pure diagnostics implementation without Android `Context`;
6. unmodified save preserves exact `content.xml`, while modified unsupported structure is reported and refused;
7. a source-architecture assertion prevents `writer.OdtDocumentParser` from regrowing an XML parser.

Existing ODT list, hyperlink, heading-run, bookmark, package-preservation, writer round-trip, and structured-save suites remain part of the full CI gate.

## 6. Deferred work, stated positively

- **7C** populates `authoredIndexes` and `namedSectionRanges`, renders authored snapshots, and feeds Navigator/status surfaces.
- **7D** populates table columns/occupancy/style fields and makes them load-bearing in layout, rendering, pagination, and hit-testing.
- **7E** populates `fontFaces`, resolves aliases through `FontRegistry`, then remeasures and tightens pagination with evidence.
- **Plan 9** serializes structural edits. Until then, the save capability remains closed for structures the current writer cannot reproduce.

## 7. Verification record

Local static checks: `git diff --check`, constructor/call-site searches, and raw fixture ZIP/XML inventories. The local JDK is unavailable, as recorded by `AGENTS.md`; compilation and tests therefore run through GitHub Actions. Final run IDs, counts, and any remediation belong here and in the PR body once CI completes.
