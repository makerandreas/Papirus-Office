---
version: 1.0
name: Papirus Office — Material 3 Expressive
description: |
  Papirus Office delivers PC-level office suite capabilities ergonomically designed
  for smartphones, foldables, and tablets by adopting Material 3 Expressive design
  principles. The system balances high-density document productivity with generous
  touch ergonomics, fluid transitions, and clear module-specific identity
  (Base Suite Blue #2563EB, Inky Green #0F9D58, Cellina Teal #16A3B7, Slidia Amber #F59E0B,
  Pagella Red #D93025), complemented by dynamic Material You theming on Android 12+.
source:
  url: "https://m3.material.io"
  spec: "Material 3 Expressive"
  product: "Papirus Office"
dials:
  energy: 2 # Balanced — Modern, functional, purposeful presence
  rhythm: 1 # Uniform — Predictable, structured, document & suite grid consistency
  motion: 3 # Bold / Expressive — Material 3 Expressive choreographies, responsive feedback, fluid transitions
modules:
  papirus:
    name: "Base / Papirus Suite"
    primary: "#2563EB"
  inky:
    name: "Inky (Word Processing)"
    primary: "#0F9D58"
  cellina:
    name: "Cellina (Spreadsheets)"
    primary: "#16A3B7"
  slidia:
    name: "Slidia (Presentations)"
    primary: "#F59E0B"
  pagella:
    name: "Pagella (PDF Viewer)"
    primary: "#D93025"
colors:
  primary: "#2563EB" # Base / Papirus Suite Blue (default static accent)
  canvas: "#FEFBFF"
  ink: "#1C1B1D"
  body: "#4D4256"
  hairline: "#E8E0E8"
  accent-1: "#E0E7FF"
  neutral-1: "#F2F2F2"
  neutral-2: "#303030"
typography:
  display-xxl:
    fontFamily: "Google Sans"
    fontSize: 96px
    fontWeight: 475
    lineHeight: 1
    letterSpacing: 0px
  display-xl:
    fontFamily: "Google Sans"
    fontSize: 60px
    fontWeight: 475
    lineHeight: 1.08
    letterSpacing: 0px
  display-lg:
    fontFamily: "Google Sans"
    fontSize: 57px
    fontWeight: 475
    lineHeight: 1.12
    letterSpacing: 0px
  display-md:
    fontFamily: "Google Symbols"
    fontSize: 40px
    fontWeight: 400
    lineHeight: 1
    letterSpacing: 0px
  heading-md:
    fontFamily: "Google Sans"
    fontSize: 28px
    fontWeight: 475
    lineHeight: 1.29
    letterSpacing: 0px
  heading-sm:
    fontFamily: "Google Sans Text"
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.43
    letterSpacing: 0px
  body-xl:
    fontFamily: "Google Sans"
    fontSize: 24px
    fontWeight: 475
    lineHeight: 1.33
    letterSpacing: 0px
  body-lg:
    fontFamily: "Google Sans Text"
    fontSize: 22px
    fontWeight: 400
    lineHeight: 1.36
    letterSpacing: 0px
  body-md:
    fontFamily: "Google Sans Text"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0px
  body-md-strong:
    fontFamily: "Google Sans Text"
    fontSize: 16px
    fontWeight: 500
    lineHeight: 1.5
    letterSpacing: 0px
  body-md-2:
    fontFamily: "Google Sans Text"
    fontSize: 16px
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: 0px
  body-sm:
    fontFamily: "Google Sans Text"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.43
    letterSpacing: 0px
  button-xl:
    fontFamily: "Google Symbols"
    fontSize: 20px
    fontWeight: 400
    lineHeight: 1
    letterSpacing: 0px
  button-lg:
    fontFamily: "Google Sans"
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.43
    letterSpacing: 0.1px
  button-md:
    fontFamily: "Google Sans"
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.33
    letterSpacing: 0.1px
  caption-sm:
    fontFamily: "Google Sans Text"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.33
    letterSpacing: 0.1px
  caption-xs:
    fontFamily: "Google Sans Text"
    fontSize: 11px
    fontWeight: 500
    lineHeight: 1.45
    letterSpacing: 0.1px
rounded:
  none: 0px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  full: 9999px
spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 20px
  xl: 24px
  xxl: 40px
  xxxl: 56px
  section: 64px
  band: 72px
borderWidths:
  thin: 1px
shadows:
  sm: "rgba(0, 0, 0, 0.3) 0px 1px 2px 0px, rgba(0, 0, 0, 0.15) 0px 1px 3px 1px"
elevationStrategy: tonal-and-single-tier
themes:
  derived: dark
  light:
    bg: "#FEFBFF"
    surface: "#F6F3F7"
    surfaceRaised: "#EDEAEE"
    text: "#1C1B1D"
    textMuted: "#4D4256"
    border: "#E8E0E8"
    accent: "#2563EB"
    accentFg: "#FFFFFF"
    focusRing: "#2563EB"
    elevation: tonal-surface
  dark:
    bg: "#0D0B13"
    surface: "#1C1A21"
    surfaceRaised: "#28262D"
    text: "#F9F7FD"
    textMuted: "#9F9DA4"
    border: "#343239"
    accent: "#93B7FF"
    accentFg: "#002B75"
    focusRing: "#93B7FF"
    elevation: "border+surface"
components:
  button-primary:
    typography: "{typography.body-md-strong}"
    textColor: "rgb(255, 255, 255)"
    height: 48px
    padding: "0px 24px"
    rounded: "{rounded.full}"
    backgroundColor: "{colors.primary}"
  button-filled:
    typography: "{typography.body-md-strong}"
    textColor: "{colors.body}"
    height: 48px
    padding: "0px 16px"
    rounded: "{rounded.md}"
    backgroundColor: "{colors.accent-1}"
  button-icon:
    textColor: "{colors.ink}"
    height: 48px
    width: 48px
    rounded: "{rounded.full}"
  card:
    typography: "{typography.body-md}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
---

# Design Direction — Papirus Office

## Dials
Dial: ENERGY 2 / RHYTHM 1 / MOTION 3

- **ENERGY**: 2 (Balanced — Modern, functional, purposeful presence)
- **RHYTHM**: 1 (Uniform — Predictable, structured, document & suite grid consistency)
- **MOTION**: 3 (Bold / Expressive — Material 3 Expressive choreographies, responsive feedback, fluid transitions)

---

## Identity & Product Essence
- **Target**: A PC-Level office suite that runs in a UI-friendly interface for smartphones and tablets.
- **Core Essence**: It delivers PC-class office suite capabilities and features, ergonomically packaged to avoid the constraints of a cramped mobile UI, by adopting the *Material 3 Expressive* design principles, which are optimized for smartphones and tablets.

---

## Personality & Character
- **Personality**: *Modern office suite*.
- **Karakter**: Utilitarian, structured, precise, professional, productive, and clean, free from excessive ornamentation that distracts from the document's work-related focus.

---

## Palette & Theming (Material 3 Expressive)

Papirus Office leverages a cohesive dual-tier color architecture that provides clear visual hierarchy and instant suite module recognition.

### Android 11 and below (Static Palette)
Uses a static color palette based on Material 3 Expressive, referencing the module accent colors defined in `AGENTS.md`:
- **Base / Papirus Suite**: `#2563EB` (Primary Suite Blue)
- **Inky (Word Processing)**: `#0F9D58` (Green)
- **Cellina (Spreadsheets)**: `#16A3B7` (Cyan/Teal)
- **Slidia (Presentations)**: `#F59E0B` (Amber/Orange)
- **Pagella (PDF Viewer)**: `#D93025` (Red)

### Android 12 and above (Dynamic Palette)
- Uses a dynamic color palette (*Material You Dynamic Color Scheme*) in accordance with Material 3 Expressive standards (`dynamicLightColorScheme` / `dynamicDarkColorScheme`).
- Users have the option to disable dynamic colors and continue using the module's static color palette.

### Neutral Scale & Surfaces
- **Canvas** (`#FEFBFF`): Clean, crisp background surface for screens, toolbars, and dashboard canvases.
- **Ink** (`#1C1B1D`): Headings, file names, active labels, and primary body copy.
- **Body / Muted** (`#4D4256`): Metadata, timestamps, file sizes, and secondary descriptions.
- **Hairline / Border** (`#E8E0E8`): 1px borders, subtle separators, and card outlines.
- **Surface Tonal Fills** (`#F6F3F7` / `#EDEAEE`): Subtle surface tiers for cards, bottom sheets, navigation bars, and dialogue containers.

---

## Typography

### UI Interface
- Uses the **Google Sans** font family:
  - `Google Sans` (Display, Headings, Primary Action Buttons)
  - `Google Sans Text` (Body copy, Subtitles, Metadata, Navigation items)
  - `Google Sans Medium` / `Google Sans Bold` / `Google Sans Flex` (Weighted emphasis and variable axis scaling)

### Document Content Separation
- **CRITICAL**: UI typography does not affect the fonts, rendering, or content of documents created, edited, or opened within the application. Document viewports and rendering engines maintain their own document-native typography (ODF fonts, embedded styles, and OpenFormula text).

### Typography Hierarchy Scale

| Role | Font | Size | Weight | Line Height | Letter Spacing | UI Usage |
|---|---|---|---|---|---|---|
| Display XL | Google Sans | 60px | 475 | 64px | 0px | Splash screens, hero greetings |
| Display LG | Google Sans | 40px | 475 | 48px | 0px | Large modal titles, major empty state headers |
| Heading MD | Google Sans | 24px | 500 | 32px | 0px | Screen headers, top app bar titles, section titles |
| Heading SM | Google Sans Text | 18px | 500 | 24px | 0px | Card titles, dialog headers, sheet headers |
| Body MD | Google Sans Text | 16px | 400 | 24px | 0px | File list items, setting descriptions, primary body |
| Body MD Strong | Google Sans Text | 16px | 500 | 24px | 0px | Selected tab labels, file names in lists, emphasis |
| Body SM | Google Sans Text | 14px | 400 | 20px | 0px | Secondary metadata, dates, file sizes, subtitles |
| Caption XS | Google Sans Text | 11px | 500 | 16px | 0.1px | Badge labels, module tags, category pills |
| Button Primary | Google Sans | 14px–16px | 500 | 24px | 0.1px | Primary CTAs, FAB labels, save/create buttons |
| Button Secondary | Google Sans Text | 14px | 500 | 20px | 0.1px | Filter chips, toolbar actions, dialog actions |

---

## Component Stylings (Material 3 Expressive)

### 1. Buttons & Interactive Controls
- **Primary Action (FAB & CTAs)**:
  - Shape: `{rounded.full}` (pill shape) or `{rounded.lg}` (24px).
  - Background: Active Module Accent (or Dynamic Primary on Android 12+).
  - Text: Contrast White (`#FFFFFF`) or onPrimary.
  - Height / Touch Target: Minimum `48dp` (accessible standard).
- **Filter Chips & Module Filter Bar**:
  - Pill shape with subtle border or tonal container background.
  - Active state reflects the specific module color (e.g., Inky Green when filtering Inky documents).
- **Icon Buttons & Toolbar Actions**:
  - Minimum touch target: `48dp x 48dp`.
  - Icon centered with ripple indication and active state fill or tint.

### 2. Toolbars & Office Ergonomics (Mobile vs. Tablet)
- **Toolbar Hub (Mobile Quick Bar)**:
  - Docked directly above the virtual keyboard (or viewport bottom in edit mode).
  - Horizontally scrollable standard formatting items + persistent trailing actions: `\t` (Insert Tab), Soft Keyboard toggle, and Ribbon button.
  - Contextual adaptation: Adapts dynamically based on selected objects (text, table, shape, cell range).
  - Overflow handling: Persistent drop-down or switcher button if toolbars exceed threshold.
- **Simplified Ribbon Bar (Mobile Deck)**:
  - 40% height bottom sheet deck inspired by modern mobile office suites (M365 Copilot / Word / Excel).
  - Header with `Ribbon options (inverted triangle)` on left and `Undo | Redo | Close` on right.
  - Dropdown menu allows switching between 8-18 contextual ribbon categories (File, Home, Insert, Layout, Formulas, Data, Review, Transitions, Animations, Slide Show, View, Drawing, Table, Picture, Chart).
- **Ribbon Full View (Tablet & Foldables)**:
  - Full desktop-grade ribbon tab bar along the top.
  - Collapsible/minimizable by double-tapping a tab or clicking `^ Hide ribbon`.
  - Multi-pane layouts (sidebars for PivotTable in Cellina, Animation Timelines in Slidia, Document Navigator).
- **Floating Contextual Toolbar (FCT)**:
  - Anchored dynamically above/below text, cells, or objects with auto-flip boundary checks.
  - Rightmost item opens expanded options (`...` / More options).
  - Cell multi-selection feedback (Sum, Average, Count snackbar) in Calc.

### 3. Cards & File Tiles
- **List & Grid Cards**:
  - Shape: Rounded corners (`{rounded.md}` — `16px`) to ensure touchable, modern M3 aesthetics.
  - Background: Surface container (`#F6F3F7`) or subtle outlined border (`1px` `#E8E0E8`).
  - Active / Pressed State: Material 3 ripple with slight tonal elevation.

### 3. Decorative Shapes & Empty States
- Empty state backgrounds utilize official **Material 3 Expressive Shapes**:
  - **All / Recents Base**: 12-sided Rosette Flower (`MaterialShapes.Rosette(12)`).
  - **Inky (Word Processing)**: Bun shape (`MaterialShapes.Bun`).
  - **Cellina (Spreadsheets)**: 4-Leaf Clover (`MaterialShapes.Clover4Leaf`).
  - **Slidia (Presentations)**: Sunny / Starburst Expressive polygon.
  - **Pagella (PDF)**: Scalloped shield / Diamond expressive polygon.

---

## Layout Principles & Responsive Behavior

### Spacing System (8dp Grid)
- `{spacing.xxs}` = `4dp` — Micro gaps, badge insets, icon-to-label gaps
- `{spacing.xs}` = `8dp` — Standard component padding, list item vertical gaps
- `{spacing.sm}` = `12dp` — Content separation within cards and dialogs
- `{spacing.md}` = `16dp` — Screen edge padding, standard card padding
- `{spacing.lg}` = `20dp` — Toolbar horizontal padding, card grid gaps
- `{spacing.xl}` = `24dp` — Section separators, header margins
- `{spacing.xxl}` = `40dp` — Empty state vertical spacing
- `{spacing.section}` = `64dp` — Hero section boundaries and sheet expanses

### Adaptive & Canonical Layouts
- **Compact (Handheld Phones, < 600dp)**:
  - Vertical list orientation, bottom navigation bar, floating action buttons (FAB).
  - Edge-to-edge support with proper `WindowInsets` padding.
- **Medium & Expanded (Tablets & Foldables, ≥ 600dp)**:
  - Canonical **List-Detail** or **Supporting Pane** layout.
  - Replaces bottom navigation with a **Navigation Rail** or permanent drawer.
  - Multi-column file grids (2–4 columns) with maximum container widths (`widthIn(max = 600dp)` on single-pane content) to prevent uncomfortable stretching.

---

## Depth & Elevation
- **Tonal Elevation First**: In alignment with Material Design 3, elevation is achieved predominantly through tonal surface shifts (color tinting) rather than heavy drop shadows.
- **Single-Tier Shadow**: Where physical elevation is functional (e.g., FABs, dropdown menus, modals), a subtle single-tier shadow (`rgba(0, 0, 0, 0.15)`) is applied without layering multiple elevation tiers.

---

## Do's and Don'ts

### Do
- **Preserve Module Accent Integrity**: Consistently color-code Inky (Green `#0F9D58`), Cellina (Teal `#16A3B7`), Slidia (Amber `#F59E0B`), Pagella (Red `#D93025`), and Base Papirus (Blue `#2563EB`).
- **Strictly Respect 48dp Minimum Touch Targets**: Ensure every icon button, list item, and action pill can be easily tapped on mobile devices.
- **Use Edge-to-Edge Padding**: Always handle `WindowInsets` properly around status bars and navigation bars using `Scaffold` and `Modifier.windowInsetsPadding`.
- **Maintain UI vs. Document Independence**: Keep Google Sans for UI controls and never force it onto parsed document contents or sheets.

### Don't
- **Do not introduce arbitrary unbranded colors**: Stick strictly to the defined module accents and M3 dynamic color schemes.
- **Do not crowd document controls**: Use bottom sheets, collapsible toolbars, and contextual ribbon bars to prevent mobile screen clutter.
- **Do not use heavy skeuomorphic shadows or gradients**: Rely on flat color blocking, tonal surface containers, and crisp 1px hairline dividers.
