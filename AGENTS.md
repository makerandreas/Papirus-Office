# Project Conventions & Specification References

## Reference Material (`/sources`)
All document format specifications, standards, and schema definitions placed in `/sources` serve as the authoritative standard for document parsing, serializing, package handling, and rendering:
- **ODF v1.2 / v1.3 Standards**:
  - `Part 1: OpenDocument Schema` (elements, styles, XML schema rules)
  - `Part 2/3: Packages` (ZIP container, `mimetype`, `META-INF/manifest.xml`, encryption, signatures)
- Whenever implementing or modifying parsers, serializers, or document processors in `com.makerandreas.papirusoffice`:
  1. Consult the relevant specification files in `/sources`.
  2. Adhere strictly to the normative rules (e.g., exact namespace definitions, element ordering, MIME header constraints, non-destructive package preservation).

## Test Fixtures (`/tests`)
Files in `/tests` (e.g., `.odt`, `.docx`) are reference test files for regression testing and compatibility verification across LibreOffice, Microsoft Word, and Papirus Office.
