# Project Conventions & Specification References

## Reference Material 
### `/sources`
All document format specifications, standards, and schema definitions placed in `/sources` serve as the authoritative standard for document parsing, serializing, package handling, and rendering:
- **ODF v1.4 Standards**:
  - `Part 1: Introduction` (architecture, conformance, namespaces, references)
  - `Part 2: Packages` (ZIP container, `mimetype`, `META-INF/manifest.xml`, encryption, signatures)
  - `Part 3: OpenDocument Schema` (elements, styles, XML schema rules for text, spreadsheets, presentations)
  - `Part 4: Recalculated Formula (OpenFormula) Format` (OpenFormula expressions, syntax, evaluators)
- Whenever implementing or modifying parsers, serializers, or document processors in `com.makerandreas.papirusoffice`:
  1. Consult the relevant specification files in `/sources`.
  2. Adhere strictly to the normative rules (e.g., exact namespace definitions, element ordering, MIME header constraints, non-destructive package preservation).

### `/app/src/libs`
There are subdirectories for each architecture. Make sure to consult these subdirectories and its necessary `so` libraries if needed.

### `sdk-examples`
When necessary, consult all SDK examples in `/sdk-references` directory.

## Test Fixtures (`/tests`)
Files in `/tests` (e.g., `.odt`, `.docx`) are reference test files for regression testing and compatibility verification across LibreOffice, Microsoft Word, and Papirus Office.

## Color Scheme & Module Accent Conventions
Default static accent colors for Android 11 and below (devices without dynamic color / Material You support):
- **Papirus (Base)**: `#2563EB` (Primary Suite Blue)
- **Inky**: `#0F9D58` (Word Processing Green)
- **Cellina**: `#16A3B7` (Spreadsheets Cyan/Teal)
- **Slidia**: `#F59E0B` (Presentations Amber/Orange)
- **Pagella**: `#D93025` (PDF Viewer Red)

## Strings for localization
When necessary, translate all strings to`en_US` and add to `strings.xml`

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
Before starting, ask the user when antislop applies: during the work, or after it is done.
<!-- antislop:end -->