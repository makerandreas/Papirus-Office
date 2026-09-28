---
version: alpha
name: WPS Office Modern
description: A clean, cloud-first productivity system with bright blue accents, airy spacing, and friendly editorial typography.
colors:
  primary: "#1F69E0"
  secondary: "#2C2F31"
  tertiary: "#EBF1FA"
  neutral: "#FFFFFF"
  surface: "#FEFEFE"
  on-surface: "#2C2F31"
  muted: "#6B7280"
  border: "#EBF1FA"
  success: "#8FD14F"
  error: "#E5484D"
typography:
  headline-display:
    fontFamily: Gabarito
    fontSize: 58px
    fontWeight: 600
    lineHeight: 72.5px
    letterSpacing: 0px
  headline-lg:
    fontFamily: Gabarito
    fontSize: 51px
    fontWeight: 600
    lineHeight: 61px
    letterSpacing: 0px
  headline-md:
    fontFamily: Gabarito
    fontSize: 44px
    fontWeight: 600
    lineHeight: 53px
    letterSpacing: 0px
  headline-sm:
    fontFamily: Inter
    fontSize: 39px
    fontWeight: 600
    lineHeight: 47px
    letterSpacing: 0px
  body-lg:
    fontFamily: Gabarito
    fontSize: 34px
    fontWeight: 600
    lineHeight: 51px
    letterSpacing: 0px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
    letterSpacing: 0px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: 400
    lineHeight: 20px
    letterSpacing: 0px
  label-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: 600
    lineHeight: 24px
    letterSpacing: 0px
  label-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: 600
    lineHeight: 24px
    letterSpacing: 0px
  label-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: 600
    lineHeight: 20px
    letterSpacing: 0px
  caption-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: 400
    lineHeight: 16px
    letterSpacing: 0px
  caption-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: 400
    lineHeight: 14px
    letterSpacing: 0px
rounded:
  none: 0px
  sm: 4px
  md: 8px
  lg: 12px
  xl: 24px
  full: 9999px
spacing:
  xs: 8px
  sm: 16px
  md: 24px
  lg: 40px
  xl: 72px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.neutral}"
    typography: "{typography.label-md}"
    rounded: "{rounded.lg}"
    padding: "20px 48px"
    size: "290px"
    height: "64px"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-md}"
    rounded: "{rounded.md}"
    padding: "20px 48px"
    size: "290px"
    height: "64px"
  button-tertiary:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.none}"
    padding: "0px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.xl}"
    padding: "48px 24px"
  input:
    backgroundColor: "{colors.neutral}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.md}"
    padding: "16px 16px"
  chip:
    backgroundColor: "{colors.tertiary}"
    textColor: "{colors.primary}"
    typography: "{typography.caption-md}"
    rounded: "{rounded.full}"
    padding: "8px 12px"
---

# WPS Office Modern

## Overview
WPS Office feels polished, approachable, and productivity-focused, with a strong emphasis on clarity and trust. The interface is spacious and light, using a bright white canvas, a vivid blue accent, and large, friendly headings to make the product feel modern and accessible. The tone is professional rather than playful, but still energetic thanks to the AI-forward messaging and bold hero treatment.

## Colors
- **Primary (#1F69E0):** A saturated WPS blue used for key calls to action, emphasis text, icons, and trust-building interactive states. It carries most of the brand energy and should remain the dominant accent.
- **Secondary (#2C2F31):** A deep charcoal used for headlines, navigation, and body copy. It provides crisp contrast without feeling as harsh as pure black.
- **Tertiary (#EBF1FA):** A pale blue-gray used for soft surfaces, subtle fills, and calm supporting UI backgrounds. It helps create the airy, cloud-like feel visible in the hero and card sections.
- **Neutral (#FFFFFF):** The main page background and default button/text contrast base. The system relies on white space heavily, so this should remain the primary canvas color.
- **Surface (#FEFEFE):** A near-white card surface that separates content from the page without introducing visual weight. It works well for containers, panels, and callout boxes.
- **On-surface (#2C2F31):** The primary readable text color on white and light surfaces. It matches the charcoal seen across headings and supporting copy.
- **Muted (#6B7280):** A quieter gray for secondary descriptions, utility text, and metadata. Use it when content should step back from the main message.
- **Border (#EBF1FA):** A soft divider and outline color for cards, panels, and input boundaries. Borders are subtle and should never dominate the layout.
- **Success (#8FD14F):** A fresh green used sparingly for positive status, confirmation, or trust signals.
- **Error (#E5484D):** A clear alert red reserved for validation and destructive states.

## Typography
Headings use Gabarito for a rounded, contemporary voice that feels friendly and branded. The largest display styles are bold and compact, with no extra letter spacing, which keeps the hero message energetic and legible at large sizes.

Body and utility text shift to Inter for clarity and dense informational reading. The body scale should stay clean and neutral, while labels and buttons use semibold weights to reinforce actions and navigation. Uppercase styling is not a major pattern here; the system relies more on weight, size, and color than on letter-case or tracking.

Suggested roles:
- **headline-display / headline-lg / headline-md:** Main marketing headlines and section leads.
- **headline-sm:** Secondary display headings where a slightly more utility-oriented face is helpful.
- **body-lg:** Hero subheadlines and prominent supporting copy.
- **body-md / body-sm:** Paragraphs, descriptive text, and navigation-supporting copy.
- **label-lg / label-md / label-sm:** Buttons, nav items, tags, and compact UI labels.
- **caption-md / caption-sm:** Microcopy, platform names, and trust-row captions.

## Layout
The page uses a centered, fixed-max-width marketing layout with generous side margins and substantial vertical breathing room. Content is stacked in clear bands: header, hero, feature proof, trust signals, then the next section, with strong section separation created through spacing rather than heavy rules.

Spacing follows a simple rhythm: 8px, 16px, 24px, 40px, and 72px. Smaller steps handle icon/text gaps and inline alignment, while larger steps define section padding, hero separation, and card breathing room. Cards and trust panels use roomy internal padding, with the larger containers feeling almost poster-like rather than dense dashboard-like.

## Elevation & Depth
The system is mostly flat, relying on tonal contrast, thin borders, and whitespace instead of dramatic shadow stacks. When depth is used, it is soft and restrained: subtle card outlines, very light inset shine, and occasional gentle shadowing that supports hierarchy without making the layout feel heavy.

Primary hierarchy comes from size, color, and placement rather than elevation. Blue CTAs and highlighted headings draw attention, while pale bordered containers group trust logos and feature content without competing for focus.

## Shapes
The overall shape language is soft and modern. Interactive elements use moderate radii, with the primary button landing at 12px and cards at a more generous 24px to create a friendly, rounded, SaaS-like feel.

This combination keeps controls approachable while preserving structure. Full-pill shapes can be used for chips and small badges, but most containers should avoid extreme rounding unless they are meant to feel decorative or lightweight.

## Components
### Buttons
- **Primary buttons** should use `button-primary`: filled blue background, white text, 12px-radius feel via `rounded.lg`, and large generous padding. They are the main conversion drivers and should appear visually dominant.
- **Secondary buttons** should use `button-secondary`: transparent background, dark text, and a quiet outlined or neutral presentation for less important actions.
- **Tertiary buttons / text links** should use `button-tertiary`: minimal chrome, no fill, no border emphasis, and compact spacing.
- Button sizing is substantial: the hero CTA is wide, tall, and comfortable to scan, so avoid shrinking primary CTAs below the established rhythm.
- Hover states should deepen blue slightly or add subtle emphasis, but avoid glossy effects or heavy shadows.

### Cards
- Cards should use `card`: near-white surface, soft border, 24px radius, and roomy padding.
- Keep card contents centered or evenly spaced where possible; the layout reads best when cards feel calm and organized.
- Use cards for trust strips, feature highlights, and grouped content blocks, not for dense nested interactions.

### Inputs
- Inputs should be understated, with light borders, white backgrounds, and moderate rounding.
- Focus states should lean on the primary blue rather than adding thick outlines or shadows.
- Maintain generous internal padding so fields match the spacious feel of the marketing pages.

### Navigation
- Top-level navigation is minimal and text-first, with simple spacing and low visual weight.
- The selected or primary action in the header should stand out with blue fill, while secondary actions remain neutral and compact.

### Chips and badges
- Chips should be compact, pill-shaped, and softly tinted using the tertiary palette.
- Use them for product tags, platform markers, or small status indicators, not as primary navigation.

### Lists and icon rows
- Icon grids and product rows should stay aligned, evenly spaced, and lightly labeled beneath each icon.
- Use color sparingly so each app icon can retain its own identity while the page structure stays consistent.

## Do's and Don'ts
- Do keep major layouts centered with generous whitespace and clear section breaks.
- Do use the primary blue for the main CTA and key emphasis, not for every clickable element.
- Do pair large Gabarito headlines with simpler Inter body copy for readability.
- Do preserve the soft-border, low-shadow aesthetic for cards and grouped panels.
- Don't introduce heavy gradients, dark backgrounds, or high-contrast neon accents.
- Don't make buttons overly small or cramped; the system expects roomy touch targets.
- Don't overuse shadows to separate content; rely on spacing, borders, and type hierarchy instead.
- Don't add decorative uppercase tracking-heavy typography unless it is a very small utility label.