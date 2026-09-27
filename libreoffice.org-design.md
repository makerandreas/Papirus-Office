---
version: alpha
name: LibreOffice
description: A clean, community-focused open source system with bright green calls to action, soft contrast, and simple editorial structure.
colors:
  primary: "#18a303"
  secondary: "#525252"
  tertiary: "#1c99e0"
  neutral: "#ffffff"
  surface: "#ffffff"
  on-surface: "#525252"
  background: "#ffffff"
  border: "#e5e7eb"
  muted: "#f3f4f6"
  success: "#18a303"
  link: "#1c99e0"
  shadow: "#224a16"
typography:
  headline-display:
    fontFamily: Lato
    fontSize: 48px
    fontWeight: 900
    lineHeight: 48px
    letterSpacing: 0.4px
  headline-lg:
    fontFamily: Lato
    fontSize: 38px
    fontWeight: 900
    lineHeight: 48px
    letterSpacing: 0.4px
  headline-md:
    fontFamily: Lato
    fontSize: 29px
    fontWeight: 700
    lineHeight: 48px
    letterSpacing: 0.4px
  headline-sm:
    fontFamily: Lato
    fontSize: 23px
    fontWeight: 600
    lineHeight: 28px
    letterSpacing: 0px
  body-lg:
    fontFamily: Lato
    fontSize: 18px
    fontWeight: 400
    lineHeight: 27px
    letterSpacing: 0.4px
  body-md:
    fontFamily: Lato
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
    letterSpacing: 0.2px
  body-sm:
    fontFamily: Lato
    fontSize: 14px
    fontWeight: 400
    lineHeight: 21px
    letterSpacing: 0.2px
  label-lg:
    fontFamily: Lato
    fontSize: 20px
    fontWeight: 900
    lineHeight: 24px
    letterSpacing: 0.2px
  label-md:
    fontFamily: Lato
    fontSize: 16px
    fontWeight: 700
    lineHeight: 20px
    letterSpacing: 0.2px
  label-sm:
    fontFamily: Lato
    fontSize: 12px
    fontWeight: 700
    lineHeight: 16px
    letterSpacing: 0.4px
rounded:
  none: 0px
  sm: 4px
  md: 5px
  lg: 8px
  xl: 12px
  full: 9999px
spacing:
  xs: 8px
  sm: 16px
  md: 28px
  lg: 36px
  xl: 56px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.neutral}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.md}"
    padding: "16px 32px"
    height: "66px"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.sm}"
    padding: "16px 32px"
    height: "66px"
  button-link:
    backgroundColor: "transparent"
    textColor: "{colors.link}"
    typography: "{typography.body-lg}"
    rounded: "{rounded.none}"
    padding: "0px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.lg}"
    padding: "16px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.sm}"
    padding: "12px 16px"
  chip:
    backgroundColor: "{colors.muted}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.full}"
    padding: "8px 12px"
---

# LibreOffice

## Overview
LibreOffice’s visual language is friendly, practical, and community-oriented rather than corporate or luxe. The page uses a lot of open white space, a restrained gray text palette, and a vivid green accent to signal action and optimism. Overall it feels approachable and trustworthy, with strong emphasis on accessibility and clear calls to action.

## Colors
- **Primary (#18A303):** A bright, saturated green used for the brand mark, primary buttons, and subtle illustrative arcs. It carries the energy of open-source contribution and is the strongest visual cue on the page.
- **Secondary / On-surface (#525252):** A medium charcoal used for body text, navigation, and secondary button borders. It keeps the interface calm and highly readable without feeling harsh black.
- **Tertiary / Link (#1C99E0):** A clear blue reserved for links and informational accents. It adds contrast against the green brand color while staying clean and functional.
- **Neutral / Surface / Background (#FFFFFF):** The dominant canvas color. White space is a core part of the aesthetic and gives the page an open, editorial feel.
- **Border (#E5E7EB):** A soft gray used for light outlines around cards and interface containers. It supports structure without creating heavy visual weight.
- **Muted (#F3F4F6):** A pale neutral for low-emphasis surfaces and utility areas when needed. It should stay understated so the accent colors remain prominent.
- **Shadow (#224A16):** A deep green-tinted shadow used sparingly under primary buttons. It reinforces the brand accent and gives just enough depth for clickable affordance.

## Typography
The system is built around Lato, with strong fallback coverage for broad platform compatibility. Headlines use very heavy weights, especially 900, to project confidence and make the main value proposition feel bold and direct. Body text stays lighter and highly readable, with modest letter spacing that keeps the overall rhythm crisp.

Headings are large and compact: `headline-display` and `headline-lg` are used for hero messaging, while `headline-md` and `headline-sm` support section titles and prominent interface labels. Body styles carry the explanatory content and should remain calm, with `body-lg` reserved for lead paragraphs and `body-md`/`body-sm` for denser UI text. Labels and buttons use `label-lg` or `label-md` with strong weight; the site leans on all-caps button text and a slightly expanded feel rather than decorative tracking.

## Layout
The layout is centered and spacious, with a simple top navigation bar and a hero section that leaves substantial breathing room above and around the main message. Content is arranged in a fixed, symmetric composition rather than a dense grid, with large horizontal gaps between navigation links and between primary call-to-action buttons. Vertical rhythm is generous, using an 8px-based spacing system scaled up through 16px, 28px, 36px, and 56px for major section breaks.

Containers should feel broad and airy, with content centered on the page and primary actions aligned in a balanced pair. Padding on cards and controls should be moderate rather than dense, and section spacing should favor openness over compact stacking. This system works best when it preserves clear separation between the header, hero text, actions, and product imagery.

## Elevation & Depth
Depth is subtle and selective. Most of the interface is flat, relying on white space, contrast, and thin borders for structure. Primary buttons gain just enough elevation through the soft green shadow to read as clickable and important, while cards use a light border instead of a pronounced drop shadow.

Avoid heavy layering or glossy effects. The design hierarchy should come from color emphasis, scale, and spacing first, with elevation used only to reinforce interactive affordances.

## Shapes
The shape language is gentle and practical, with small radii rather than rounded, playful forms. Primary buttons use a 5px corner radius, giving them a slightly softened rectangular profile. Cards and surfaces lean into simple geometry with 8px-level rounding for contained elements, maintaining a clean and dependable feel.

Overall, the system favors architectural clarity over decorative curves. Shapes should stay boxy, legible, and modestly softened at the edges.

## Components
Buttons are the most expressive component in the system. `button-primary` is a solid green call to action with white text, bold Lato, 16px by 32px padding, and a 66px height; it should feel prominent and reliable. `button-secondary` is more restrained, with a transparent background, gray text, and a thin border for secondary actions. `button-link` should appear as a plain text link with underline and blue color, used for inline navigation or low-emphasis actions.

Use uppercase styling and heavy weight for prominent action labels, especially in hero areas and top-level tasks. Primary buttons should be large, visually centered, and separated enough to read as a pair rather than a cluster. Secondary buttons should remain visually quieter and should not compete with the primary action.

Cards should stay white with a light gray border, 8px rounding, and 16px padding. They should not use strong shadows or decorative backgrounds. Inputs should feel compact and functional, with modest rounding and a clean border that matches the site’s restrained utility style.

Navigation links should be understated, medium gray, and spaced widely enough to support scanability. Any utility controls, tags, or chips should remain simple and neutral unless they represent a primary action. Iconography should be thin, functional, and used sparingly so the content and CTAs remain dominant.

## Do's and Don'ts
- Do keep the page spacious and centered, with generous breathing room around hero content.
- Do use bright green only for primary emphasis and brand-signaling actions.
- Do keep typography heavy in headlines and clean in body copy.
- Do preserve the soft, minimal elevation style; use shadows sparingly and intentionally.
- Don't introduce dark backgrounds or high-contrast color schemes that fight the open, light feel.
- Don't over-round buttons or cards; the system should stay crisp and practical.
- Don't add busy gradients, ornamental textures, or elaborate depth effects.
- Don't let secondary elements compete with the green primary CTA.
