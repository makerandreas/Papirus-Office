# Plan 11 — Hybrid Experience Design and Design-System Alignment

**Date:** 2026-09-25
**Status:** Documentation alignment is in this change; product implementation is split into later PRs.
**User context:** Plan 3B is complete. The checked-out history is shallow, so the completion statement is based on the user's confirmation and the existing Plan 3B implementation record, not an attempt to reconstruct missing Git history.
**Scope:** Reconcile `DESIGN.md` and related product docs to the user's design direction, analyze the prior anti-slop plans, connect the design to the Writer Guide and real ODT/DOCX fixtures, then define a safe PR sequence. No app runtime behavior or product UI implementation is changed; the nightly release copy is clarified as a standing feature summary rather than a commit changelog.

---

## 1. Decision summary

Papirus Office is intended to be a Material 3 Expressive, Android-first office suite based on LibreOffice technologies/API where available, first-class for ODF and designed for practical OOXML compatibility. The user-facing UI combines bounded patterns from multiple mobile office apps rather than cloning a single suite. LibreOffice supplies office concepts and format behavior; it is not a mandate to reproduce a desktop window on a phone.

The design-source map is now binding:

| Surface | Reference |
|---|---|
| Start Screen, every tab | Google Workspace apps |
| Editor dialogs in all modules | Google Workspace apps |
| Welcome and Create New Documents | WPS Office |
| Options, Crash Logs, About | Android system settings; About is vertically paged, TikTok-like only in its page-to-page navigation/transition |
| Editor screens | M365 Copilot mobile office |
| Standard Bottom Sheet command decks | Microsoft Office 365 for Inky/Cellina/Slidia; SoftMaker FlexiPDF for Pagella |
| General document concepts/layout | LibreOffice, adapted to Android and touch |

Material 3 Expressive, Papirus identity, accessibility, adaptive behavior and honest implementation status apply to every row. These references guide interaction hierarchy and task flow, not third-party branding, assets or pixel-for-pixel copies.

### Theme choices

- **Android 12+ default:** Android's system dynamic scheme (`dynamicLightColorScheme` / `dynamicDarkColorScheme`). The OS palette can derive from wallpaper; Papirus consumes the system scheme in this mode.
- **Android 11 and earlier:** Papirus static light/dark schemes, using the established Papirus/module accents as seeds and identifiers.
- **Optional custom colors:** Papirus generates a semantic tonal scheme from a user-selected custom seed/accent.
- **Optional wallpaper palette:** Papirus derives its own semantic scheme from system wallpaper colors independently of Android's Material system scheme. `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` is available from API 27; use it as a color input, not as a ready-made Material scheme. If unavailable/null (for example, a live wallpaper), offer a SAF-selected image or a clear Papirus-static fallback.
- A clear theme-mode preference may expose the modes supported by the device; mode limitations and fallbacks must be explicit.

### Visual assets

- Material Symbols Rounded is the default icon family.
- `app/src/main/share/config/images_colibre.zip` is a candidate optional Colibre icon set. Inventory content, attribution and license before implementation or redistribution.
- Google Sans is the UI typeface target. It is already present in the repository and `Type.kt` attempts to load it, but previous user testing reported hiccups and a Roboto fallback. Treat Roboto/system sans-serif as the reliable fallback until the Google Sans rendering path passes device, weight, accessibility-scale and performance checks.
- UI font selection is separate from ODF/OOXML document-family preservation and the Plan 5/7/8 document-font pipeline.

---

## 2. What the previous PRs and audits establish

The `anti-slop/` history is an implementation and evidence ledger, not a clean-slate design brief:

1. **Audits 001–002 (Sep 7–9):** established copy/emoji/contrast/touch-target anti-patterns, semantic icons, Material tokens, and CI hygiene. The design update must preserve these quality gates.
2. **Audit 003 (Sep 22):** hands-on Realme C3 review; Chapter 1 lifecycle, editing and undo paths had meaningful progress, while device UI, pagination, fonts and fidelity still needed evidence. It set the principle that a desktop feature needs an honest, usable mobile equivalent.
3. **Audit 004 (Sep 24):** Writer style-resolution improvements landed in PR C; it explicitly left real font loading and Font Style UI for later. It also documented that a passing unit test is not a device rendering check.
4. **Audit 005 (Sep 24):** post-PR-C Sample-6 and UI/input triage. It found specific chrome/input defects and major pagination, image and ODF/DOCX-structure gaps.
5. **Audit 006 (Sep 24):** screenshot and compliance evidence, and a warning against docs claiming more than the code proves. It flags, among other things, the stub-sized bundled `.so` files and simulated engine path, therefore ``LibreOffice-based`` is the strategic architecture and not proof that native LOKit renders current documents.
6. **Plans 1–3:** created the evidence registry, screenshot backlog and mechanical compliance work. Plan 3A/3B records contain implementation results; Plan 3C was scoped as a documentation-accuracy change.
7. **Plans 4–9 / roadmap v2:** define the Writer fidelity critical path: measuring/pagination, image handling, ODF structures, DOCX styles/fields/numbering/tables/sections, then round-trip integrity. They correctly treat Sample-6 as a cross-format stress fixture and defer save claims until tested.
8. **Plan 10:** parked the document-font engine and broad design-language review pending fidelity work. This Plan 11 expands and separates that scope: UI design-system/theming/icon work is not the same dependency as document-font metrics. Preserve Plan 10's document-font thread; reconcile its former UI thread against this plan before implementation.

### Evidence/claim rule

A resource in `res/font`, a ZIP in the repository, a composable name, a roadmap bullet, or an available native library is not proof that the feature is usable. Product docs should label each claim as a design target, implemented behavior, partially implemented behavior or verified behavior, and cite tests/device evidence where relevant. Avoid claims of full OOXML/ODF coverage, M365 Copilot functionality or active native LibreOffice API rendering unless verified.

---

## 3. Writer Guide Chapter 1 comparison

Reference: [LibreOffice Writer Guide 7.2, Chapter 1: Introducing Writer](https://documentation.libreoffice.org/assets/Uploads/Documentation/en/WG7.2/WG72-WriterGuide.pdf). The chapter is a functional baseline, not a demand to reproduce a desktop screen.

| Writer Guide Chapter 1 area | Mobile Papirus treatment / existing evidence |
|---|---|
| Main window: title/menu bars, toolbars, work area, status bar, sidebar, rulers | Map to adaptive editor chrome, Toolbar Hub, Standard Bottom Sheet, canvas and unified status information. Keep only high-value functions visible at phone width; use expanded decks/panes at larger widths. Rulers and page geometry controls need an explicit mobile route. |
| Context (right-click) menus | FCT and contextual command surfaces must be reachable from touch as well as mouse/keyboard; do not make a right-click-only function. |
| Dialogs | A consistent Google Workspace-inspired editor-dialog pattern across modules, with clear Apply/Cancel/back semantics and accessibility-safe full-page or sheet layouts. |
| Document views | Viewer/editor modes and zoom need reliable fit behavior, a single status-bar page count, and preserved document position. Audit-005/006 identified regressions to verify. |
| Create from blank/template | WPS-inspired Welcome and Create New flow, preserving the project template library and document-format choice. |
| Open, save, reload, close | Existing `docs/InkyC1Checklist.md` lifecycle tests are the acceptance baseline. Saving is not "compatible" until reopened output is compared, including structural content. |
| Moving through a document | Navigator, Go To Page, Navigate By, headings, tables, images, hyperlinks and reminders. Plans 7/8 own parser-backed categories; no fake populated states. |
| Undo/redo and status feedback | The checklist exercises typing, selection and multiple undo; keep status feedback understandable without relying only on transient toasts. |

`docs/InkyC1Checklist.md` extends this comparison with input, selection, session restore and stress tests. It is not all Chapter 1 content, but it is the project's practical Writer acceptance suite. The checklist still requires real device runs at its resume points.

---

## 4. Sample corpus analysis: ODT and DOCX

The six matched files under `tests/inky/` were inspected as ZIP packages and their main XML parts were counted. The following inventory is a structural guide; it is not a fidelity score. Counts describe literal XML elements, not expanded repeated rows/cells or rendered page counts.

| Pair | ODT structure observed | DOCX structure observed | Why the pair matters |
|---|---|---|---|
| Sample-1 | 183 paragraphs, 28 headings, 3 tables, 70 list containers / 84 list items, 6 frames/images, 8 soft page breaks | 211 paragraphs, 3 tables, 6 drawings, 9 field instructions / 27 field markers | Dense legal/research text, lists, tables, image/media and sequence fields. Tests preservation of varied body structure. |
| Sample-2 | 308 paragraphs, 30 headings, 2 tables, 41 lists / 103 items, 15 links, 1 TOC, 11 frames / 10 images, 10 soft page breaks | 351 paragraphs, 2 tables, 15 hyperlinks, 14 field instructions / 42 field markers, 22 last-rendered breaks, 5 section properties, 12 drawings | Long research paper with TOC/index, hyperlinks, media, fields, page-break hints and multiple sections. Strong TOC/field/pagination case. |
| Sample-3 | 170 paragraphs, one table (8 rows / 40 cells); no ODT heading/list/link/image structures in the inspected body | 169 paragraphs, one table (8 rows / 40 cells), 421 runs; no links, fields or drawings | Control fixture for paragraph/run/style and table behavior without list/TOC/image complexity. Regression rule: structural PRs add no invented structure and lose no existing text. |
| Sample-4 | 112 paragraphs, 7 headings, 15 lists / 18 items, 23 links, 1 TOC, one image/frame, 2 soft page breaks | 116 paragraphs, 23 hyperlinks, 1 field instruction / 3 field markers, 6 section properties, 1 drawing | Hyperlink-heavy paper and TOC cross-check, with media and per-section DOCX geometry. |
| Sample-5 | 218 paragraphs, 123 lists / 186 items, 14 links, 1 TOC, 2 frames / 1 image, 2 soft page breaks; heading-like content uses paragraph auto-styles rather than `text:h` | 217 paragraphs, 14 hyperlinks, 1 field instruction / 3 field markers, 3 last-rendered breaks, 5 section properties, 1 drawing | Existing pagination and style-resolution control; catches nested/multilevel list behavior and protects the known paragraph-style hierarchy. |
| Sample-6 | 322 paragraphs, 45 headings, 115 lists / 180 items, 45 links, 1 TOC, 1 table (6 rows / 30 cells), 3 frames/images | 371 paragraphs, 45 hyperlinks/bookmark targets, 1 field instruction / 3 field markers, 1 table (6 rows / 30 cells), 5 section properties, 3 drawings | Highest-value convergence fixture: numbered headings/list styles, TOC links, images, tables, five DOCX sections, page-numbering and ODT/DOCX parity. |

### Sample-driven testing rules

- Run every relevant parser/renderer/save assertion against **all six ODT/DOCX pairs**, not only the visually convenient Sample-5 or Sample-6.
- Use Sample-3 as a negative/control fixture for styles, text, run spans and tables.
- Use Sample-6 as the end-to-end convergence fixture, not as the only fixture.
- Assert XML/package structure as well as visible output: ODT package/manifest/links/styles and DOCX parts/relationships/content types/fields/sections matter even when the first screen looks plausible.
- Keep ODF interpretation grounded in the checked-in ODF 1.4 Parts 1–4 under `docs/html`. Use ECMA-376 as normative for OOXML; the [Open XML SDK documentation](https://learn.microsoft.com/en-us/office/open-xml/open-xml-sdk) is a useful package/part model and API reference, not a replacement for ECMA-376.

---

## 5. PR sequence and work packages

Plan 3B is treated as complete. The next documentation PR is the existing Plan 3C intent, expanded to align the hybrid design direction. **PR 15–22 remain reserved for the established Plans 5–9 roadmap** in `plan-2026-09-24-remaining-pr-roadmap-v2.md`; this design plan does not renumber or silently replace those fidelity PRs.

| Order | Scope | Exit gate |
|---|---|---|
| **PR 14 / Plan 3C: design contract and docs accuracy** *(this documentation change)* | Align `DESIGN.md`, `CONCEPT.md`, `AGENTS.md` and `PROJECT_CONTEXT.md`; add this plan and link it from the master index/Plan 10. Keep design target distinct from code status. | No conflicting palette/font/reference/layout claims across these docs; ODF/OOXML and LOKit status claims are qualified. |
| **After PR 14: UI inventory and evidence gate** | Read Compose screens and theme code against the design map. Capture current-vs-target matrix for home/start tabs, welcome/create, options, crash logs, about, editor overlays/ribbon; identify typography, color, insets, adaptive and behavior gaps. | Every screen has an owner, current state, target source, strings/accessibility requirements and test surface. No feature promises inferred from visual references. |
| **Theme work package** | Implement/version-gate system dynamic colors on API 31+, Papirus static schemes on API 30 and below, optional user custom scheme, and explicit user-selected-wallpaper extraction independent of system dynamic color. Store preference safely; define fallback and light/dark behavior. | Automated scheme-selection tests plus device matrix for API 30/31+; contrast validation across module surfaces and supported modes. |
| **Typography and icon work package** | Validate Google Sans resources/weights and typography mapping, keep Roboto fallback; verify scaling/performance. Inventory Material Symbols Rounded coverage and Colibre ZIP provenance/license; add a selectable icon pack only if rights and accessibility mapping are clear. | Screenshot and text-scale evidence; no missing-font crash; icons have semantic labels; license/provenance recorded before bundling assets. |
| **Home-entry surfaces work package** | Apply WPS-inspired Welcome/Create New and Google Workspace-inspired Start Screen tab patterns on shared theme. Preserve real SAF/template behavior; Google Drive remains an honest placeholder unless separately implemented. | Compact 320 dp, medium and expanded-width checks; navigation, empty states, strings, TalkBack and theme evidence. |
| **Settings and About work package** | Android-settings-inspired preference and diagnostic hierarchy; About's paged sections have explicit controls/indicators and an accessible alternative to vertical swiping. Keep diagnostics technical and copy anti-slop compliant. | Back navigation, screen-reader order, reduced motion, text scaling, large screen and persistence checks. |
| **Editor surfaces work package** | Normalize editor dialogs/sheets across all modules using the Google Workspace-inspired task hierarchy and M3 components. Apply M365-inspired editor hierarchy without importing proprietary functionality; align Toolbar Hub, status bar, viewer/editor controls and general LibreOffice-derived concepts to touch. | Writer Guide/checklist mapping complete; Inky device gate plus representative Calc/Impress/PDF smoke tests. |
| **Module ribbon work packages** | Implement Office-365-inspired decks for Inky/Cellina/Slidia and FlexiPDF-inspired Pagella. Split by module/capability; no control may only toast or mutate document state without serializer support. | Each command has a real enabled action or explicit unavailable state; ODF/OOXML round-trip tests for document edits. |

### Dependency and ordering notes

- The documentation contract is PR 14. The existing PR 15–22 document-fidelity roadmap remains the numbered critical path; design implementation packages should receive actual PR numbers only after that schedule is re-confirmed.
- Theme and app-shell work can proceed alongside ODF/DOCX parser work only where they touch separate UI/theme paths. Share anti-slop gates and resolve source conflicts before merge.
- UI Google Sans work is not a substitute for Plan 5/7/8 document-font metrics and should not block a shell redesign. Conversely, do not change saved document font names to make the UI font path work.
- Ribbon controls that edit content depend on their model/writer capability, so follow Plans 6–9 or coordinate capability by capability. Design specs may be written early; implementation cannot claim persistence until round-trip evidence passes.
- Do not postpone theme/screen architecture solely for Plan 10's document-font work. Keep Plan 10 A1–A5 (document rendering/substitution and metrics) separate from UI typography/icon foundations. Re-scope the old Plan 10 Thread B/B1–B6 against this Plan 11 before implementation to avoid competing design roadmaps.

---

## 6. Product-wide acceptance gates

Every UI PR must report:

1. **Viewport:** 320 dp compact width and one medium/expanded width; no overlap, clipped action or unusable bottom sheet.
2. **Android version/palette:** API 30 and API 31+ where applicable; light/dark; system/static/custom/wallpaper modes supported on that platform.
3. **Typography/accessibility:** user font scaling (at least 1.0, 1.3 and 2.0), TalkBack focus/order/labels, keyboard/mouse navigation where relevant, minimum 48 dp targets, contrast and reduced-motion behavior.
4. **Honesty:** implemented, partially implemented, disabled or placeholder state is accurate; no silent or fake command behavior.
5. **Evidence:** screenshots, test names, device/build identifiers and the known exclusions. A source grep or successful unit test alone is not a visual Delivery Gate.

---

## 7. References consulted

- [Material 3 Expressive](https://m3.material.io/get-started), [Android color guidance](https://developer.android.com/design/ui/mobile/guides/styles/color), [Compose Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3), [WallpaperManager](https://developer.android.com/reference/android/app/WallpaperManager), and the [bottom sheet guidance](https://m3.material.io/components/bottom-sheets/overview). M3 recommends semantic theme roles and describes standard sheets as supplementary/dismissible; Papirus's persistent command-deck use is an intentional adaptation that requires validation.
- [Material Symbols](https://fonts.google.com/icons).
- [Google Workspace apps' Material 3 redesign](https://www.theverge.com/2023/2/23/23612000/google-docs-workspace-redesign-chips-drive-sheets-slides-material-you) is a secondary visual-history reference; the user's chosen Google Workspace interaction pattern is the product decision.
- [Microsoft 365 Copilot app on Android](https://support.microsoft.com/en-us/microsoft-365-copilot/install-and-set-up-the-microsoft-365-copilot-app-on-an-android) and [Copilot in Word on Android](https://support.microsoft.com/en-us/word/copilot/copilot-in-word-on-mobile-devices) provide public task-flow references, not a feature-parity commitment.
- [WPS Office Android create-document walkthrough](https://www.wps.com/blog/how-to-create-a-word-document-on-android-ultimate-guide/) for blank/template creation patterns.
- [SoftMaker FlexiPDF manual](https://www.softmaker.net/down/flexipdf2025_en.pdf) documents a desktop ribbon. FlexiPDF is not treated as a proven Android UI reference; borrow only the user's specified PDF command grouping/ribbon concept and redesign for Papirus's mobile layout.
- [LibreOffice Writer Guide 7.2, Chapter 1](https://documentation.libreoffice.org/assets/Uploads/Documentation/en/WG7.2/WG72-WriterGuide.pdf).
- [Open XML SDK documentation — Microsoft Learn](https://learn.microsoft.com/en-us/office/open-xml/open-xml-sdk) and [ECMA-376 / OOXML standards](https://ecma-international.org/publications-and-standards/standards/ecma-376/).
- ODF 1.4 specifications under `docs/html`.
- Project evidence: `anti-slop/audit-001` through `audit-006`, `anti-slop/plan-01` through `plan-10`, `docs/InkyC1Checklist.md`, `CONCEPT.md`, `AGENTS.md`, `PROJECT_CONTEXT.md`, and all 12 ODT/DOCX fixtures in `tests/inky/`.
