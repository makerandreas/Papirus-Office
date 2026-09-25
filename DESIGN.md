---
version: 2.0
name: Papirus Office — Hybrid Material 3 Expressive
status: Product design direction; implementation is staged separately
source:
  design_system: https://m3.material.io/
  material_symbols: https://fonts.google.com/icons
  writer_reference: https://documentation.libreoffice.org/assets/Uploads/Documentation/en/WG7.2/WG72-WriterGuide.pdf
  product: Papirus Office
---

# Design Direction — Papirus Office

## 1. Product intent

Papirus Office is an Android-first, open-source office suite intended to be a credible alternative to proprietary mobile office products, including Microsoft 365 Copilot. It is built around LibreOffice technologies and APIs where available, treats ODF as a first-class format, and aims to preserve practical OOXML compatibility. This is a product direction, not a claim that every native LibreOffice API or every format feature is implemented today. See `PROJECT_CONTEXT.md` and the PR plans under `anti-slop/` for implementation status and evidence.

The experience is a deliberate hybrid: Material 3 Expressive supplies the design system and accessibility baseline; selected office apps inform task-specific interaction patterns; LibreOffice Writer and the other LibreOffice modules inform document concepts and capabilities. References are inspiration, not pixel-for-pixel targets. Papirus keeps its own identity, adapts desktop concepts to touch, and does not reuse third-party logos, proprietary assets, or branded artwork.

## 2. Design principles and dials

- **ENERGY 2**: balanced, purposeful, professional.
- **RHYTHM 1**: consistent spacing, navigation and document controls.
- **MOTION 3**: expressive but brief, interruptible motion with reduced-motion support.
- Keep editing controls close to the document while preventing desktop-density layouts from being forced onto phone screens.
- Prefer clear state, semantic icons, accessible labels and honest unavailable states over decorative treatment or dead controls.
- Use responsive layouts for compact phones, foldables and tablets. Preserve a touch-first path without excluding keyboard, mouse, stylus or accessibility-service input.

## 3. Hybrid experience map

| Surface | Design reference | Papirus adaptation |
|---|---|---|
| Start Screen, all tabs | Google Workspace apps | Familiar, focused document browsing and tab navigation; Papirus module identity and Android navigation remain authoritative. |
| Editor dialogs, all modules | Google Workspace apps | Consistent, task-focused editor dialogs and sheets; use Papirus theme tokens, accessible dismissal and clear commit/cancel behavior. |
| Welcome Screen and Create New Documents | WPS Office | Friendly entry and template-first creation flow, simplified for Android and integrated with Papirus navigation. |
| Papirus Office Options, Crash Logs, About | Android system settings | Native-feeling preference rows and diagnostic information. About is a set of vertically paged sections with a restrained, TikTok-like vertical page-to-page gesture/transition; it is not an endless social feed. Provide explicit navigation, screen-reader semantics and a non-gesture route. |
| Editor screens | M365 Copilot mobile office | Context-aware, document-first editing chrome and compact command access, adapted to the Papirus engine and Android rather than copied. |
| Standard Bottom Sheet / Ribbon | Microsoft Office 365 for Inky, Cellina and Slidia; SoftMaker FlexiPDF for Pagella | A shared Papirus bottom-sheet host with module-specific decks, spacing and actions. The content and interaction model are specified in `CONCEPT.md`; unsupported commands stay visibly unavailable until implemented. |
| General document interactions | LibreOffice Writer, Calc, Impress and PDF concepts | Preserve familiar office concepts such as document navigation, styles, selection and format fidelity, but redesign desktop-only controls for mobile, foldable and tablet layouts. |

The app-to-app references are bounded by surface; do not transfer an entire visual system from any one reference. Material 3 Expressive tokens, accessibility and Papirus branding remain consistent across the product.

## 4. Color and personalization

All UI chrome uses semantic theme roles (`primary`, `onPrimary`, container, surface, outline, error, and their dark-theme equivalents). Module accents identify product areas, but do not bypass contrast-safe scheme generation or replace semantic colors on arbitrary controls. Document content and pinned paper/canvas colors are not application chrome and retain their document-specific color behavior.

### Theme modes

1. **System dynamic (Android 12 / API 31 and above)**: default to the Android-provided Material dynamic light/dark scheme. On supported devices this reflects the system's wallpaper-derived palette. Papirus reads the system color scheme; it does not independently claim to reproduce Android's algorithm.
2. **Papirus static**: on Android 11 and below, use the Papirus light/dark schemes and module accents listed below. This is also an available user choice on newer Android versions.
3. **Custom**: allow a user-selected Papirus seed/accent scheme, with generated tonal roles and validated text/icon contrast.
4. **Wallpaper palette**: offer an opt-in Papirus palette derived from the device's system wallpaper colors, independently of the Material system color scheme. On API 27+, use `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` as the wallpaper-color input, then generate Papirus semantic tones; do not consume `dynamicLightColorScheme`/`dynamicDarkColorScheme` as the source for this mode. The app's minimum SDK is lower, so unsupported/null cases need the selected-image or static fallback. If the API returns no colors or is unavailable, offer a user-selected wallpaper image through the Storage Access Framework or fall back clearly to the Papirus static scheme. Do not request broad storage access.

Users can switch between available modes. A mode that cannot be provided on the current OS/device must be explained and disabled or offered with a suitable fallback, not silently misrepresented. Respect system light/dark preference unless the user selects an explicit theme mode.

### Stable module accents

| Area | Accent |
|---|---|
| Papirus suite | `#2563EB` |
| Inky (Writer) | `#0F9D58` |
| Cellina (Calc) | `#16A3B7` |
| Slidia (Impress) | `#F59E0B` |
| Pagella (PDF) | `#D93025` |

These are brand seeds/identifiers for the static Papirus theme. Dynamic and custom schemes may derive different rendered tones while retaining module identification where appropriate. Never apply these raw values where a semantic `MaterialTheme.colorScheme` role is required.

## 5. Typography and document-font separation

- **Target UI family:** Google Sans, using the appropriate Google Sans text/display styles where those bundled faces are available and legally distributable. Google Sans Code may be used for diagnostic or code-like content.
- **Reliability fallback:** Roboto / Android sans-serif. The repository includes Google Sans font resources and the theme currently attempts to load them; previous integration tests reported rendering hiccups. Keep Roboto as a deliberate fallback until on-device rendering, font weights, accessibility scaling and performance pass. Do not claim the target is fully validated based on resource presence alone.
- UI typography is independent of document typography. Opening or editing an ODT/DOCX must preserve the document's family/style identity; substitutions used for rendering belong to the document-font engine and must not change the saved family name.
- Define the UI type scale in Android `sp` with line height, weight, letter spacing, scalable text and component mapping. The table in this file is a design target, not evidence of the values currently used by every composable. Reconcile it with `ui/theme/Type.kt` during the implementation audit.

| Role | Target size | Typical use |
|---|---:|---|
| Display | 40–57 sp | Large welcome or empty-state headings, sparingly |
| Headline | 24–32 sp | Screen or section headings |
| Title | 14–22 sp | App bars, cards, dialogs and sheets |
| Body | 14–16 sp | Document lists, settings and explanatory copy |
| Label | 11–14 sp | Buttons, tabs, metadata and compact controls |

Follow Material typography roles and user font scaling; do not set text sizes in physical pixels. The final scale must be validated against real devices and `Type.kt`, then treated as the implementation source of truth.

## 6. Icons and visual assets

- **Default icon family:** Material Symbols Rounded. Select icons by meaning and maintain consistent optical size, weight, fill and accessibility descriptions.
- **Optional icon set:** Colibre is a candidate alternate icon theme; the repository contains `app/src/main/share/config/images_colibre.zip`. Inventory its contents and license/provenance before wiring it into the app or redistributing extracted assets. Do not mix icon families casually within one screen.
- Logos, empty-state illustrations and document page previews are distinct from action icons. Keep contrast and meaning understandable without color alone; avoid decorative emoji as interface iconography.

## 7. Layout and component rules

- Use Android adaptive layout patterns: compact bottom navigation where appropriate, and navigation rail, supporting pane or list-detail patterns when width permits. Do not enforce a desktop menu/ribbon on narrow screens.
- Maintain at least **48 dp** interactive targets, including compact glyph controls. A small visible icon can sit inside a larger semantic/touch target.
- Use theme tokens for application chrome, clear focus/pressed/disabled states, adequate text contrast, edge-to-edge insets and predictable back behavior.
- The Standard Bottom Sheet is a touch-oriented command deck, not a literal desktop ribbon resized to 40% of every screen. It may expand/scroll as needed, keeps essential actions reachable, and must account for keyboard, display size and accessibility settings.
- Dialogs and sheets explain consequences, provide clear primary/secondary actions and distinguish Apply/Save from Cancel/Back. Use consistent editor-dialog patterns across modules.
- Motion communicates state and navigation; honor Android reduced-motion settings and never make a gesture the only way to navigate.
- Do not make mockups or static badges appear actionable. Clearly mark features not yet implemented.

### Spacing, shape and adaptive layout

- Use a 4 dp base increment for micro-spacing and an 8 dp primary rhythm; common values are 4, 8, 12, 16, 24, 32 and 48 dp. Let content hierarchy determine the gap rather than repeating one value everywhere.
- Use a small semantic shape scale from modest control corners through larger cards/sheets to pill/circle only where the component calls for it. Prefer Material 3 shape tokens and avoid making every component a pill.
- Prefer tonal surface roles and restrained elevation. Shadows should clarify a real layer, not decorate every card.
- Use Android window size classes as guidance: compact below 600 dp, medium from 600–839 dp and expanded at 840 dp or above. A navigation bar, rail, supporting pane or multi-pane editor is chosen by task and available space, not merely by device label.
- Keep document-page/paper color distinct from application chrome. Pinned white/black page surfaces are document surfaces, not exceptions that justify arbitrary chrome colors.

### Component decisions and known audit deltas

| Component family | Direction | Known delta to verify in implementation |
|---|---|---|
| Bottom/status bar | One authoritative page/zoom/status surface; keep status values optically aligned and protected from edge-to-edge/system insets. | Audit-005/006 documented duplicate per-page counters, an Edit FAB overlapping the bar and a counter that shifts off center. The code fix belongs to the owning UI plan, not this documentation update. |
| FAB role | Use a FAB only for the screen's most common or important action. Creation is the likely primary action on Start/Create surfaces; in an editor, a mode switch should be a toolbar/status affordance unless research confirms it is the primary action. Never allow overlap with the canvas or status bar. | Audit-005/006 found the Edit FAB overlapping status-bar actions. Re-evaluate its role and placement against current task flows before implementation. |
| Standard Bottom Sheet | Use a reachable command deck that can scroll/expand; treat 40% as an initial phone composition, not an exact-height invariant. | M3 describes standard sheets as supplementary and dismissible. Papirus intentionally adapts the sheet into a primary office command deck; preserve access to the document, make dismissal/expansion behavior clear, and verify keyboard/insets, long command sets, larger text and expanded-width layouts. |
| Editor dialogs | Shared Google Workspace-inspired task structure with Papirus/M3 styling; use a dialog, sheet or full page according to task complexity, with explicit back/commit/cancel behavior. | The previous concept prescribed mostly fixed full-page forms. Inventory existing dialogs and record any component-specific deviation before consolidation. |

Material 3 references: [Android color guidance](https://developer.android.com/design/ui/mobile/guides/styles/color), [dynamic Compose color](https://developer.android.com/develop/ui/compose/designsystems/material3), [navigation bars](https://m3.material.io/components/navigation-bar/overview), [FABs](https://m3.material.io/components/floating-action-button/overview), [bottom sheets](https://m3.material.io/components/bottom-sheets/overview), and [dialogs](https://m3.material.io/components/dialogs). M3 describes bottom sheets as supplementary content with standard and modal variants; Papirus's command-deck use is an explicit product adaptation. References inform the target; they do not replace measuring Papirus's behavior at compact and expanded widths.

## 8. Standards and product boundaries

- **ODF 1.4**: use the normative files in `docs/html` for package, schema and formula behavior.
- **OOXML**: use ECMA-376 as normative; Microsoft's Open XML SDK documentation is a useful package/part model and API guide, not a substitute for the standard. Existing links are collected in `CONCEPT.md`.
- **Writer interaction reference**: LibreOffice Writer Guide, Chapter 1, is a functional comparison for the window, navigation, document lifecycle, dialogs, views, creation, opening and saving. Mobile equivalents are documented in `anti-slop/plan-11-hybrid-experience-design.md`.
- Inspiration from M365 Copilot, Google Workspace, WPS Office and FlexiPDF should be expressed as task-level patterns only. Do not imply Papirus has M365 Copilot features, Google Workspace integrations, or native LibreOfficeKit execution unless those are independently implemented and verified.

## 9. Acceptance gates for design changes

Before a design change is considered complete, record:

1. Which surface and source pattern it addresses, and why the Android adaptation is suitable.
2. Behavior at compact width (including 320 dp), medium/expanded width and large text scaling.
3. Contrast, touch-target, focus, screen-reader and reduced-motion checks.
4. Theme behavior for light/dark and each supported palette mode, with Android-version limitations stated.
5. Whether typography/icons are actually loaded in the rendered UI, not just present as assets.
6. Evidence (screenshots or automated checks) and any known exclusions or future implementation work.
