---
version: alpha
name: Google Search Light
description: A minimal, highly spacious search-first system with a crisp blue accent and understated surfaces.
colors:
  primary: "#0b57d0"
  secondary: "#3c4043"
  tertiary: "#f8f9fa"
  neutral: "#ffffff"
  surface: "#ffffff"
  on-surface: "#1f1f1f"
  border: "#e5e7eb"
  muted: "#5f6368"
  error: "#d93025"
typography:
  headline-display:
    fontFamily: Roboto
    fontSize: 32px
    fontWeight: 700
    lineHeight: 38px
    letterSpacing: 0px
  headline-lg:
    fontFamily: Roboto
    fontSize: 24px
    fontWeight: 700
    lineHeight: 29px
    letterSpacing: 0px
  headline-md:
    fontFamily: Roboto
    fontSize: 20px
    fontWeight: 600
    lineHeight: 24px
    letterSpacing: 0px
  headline-sm:
    fontFamily: Roboto
    fontSize: 18px
    fontWeight: 600
    lineHeight: 22px
    letterSpacing: 0px
  body-lg:
    fontFamily: Roboto
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
    letterSpacing: 0px
  body-md:
    fontFamily: Roboto
    fontSize: 14px
    fontWeight: 400
    lineHeight: 20px
    letterSpacing: 0px
  body-sm:
    fontFamily: Roboto
    fontSize: 12px
    fontWeight: 400
    lineHeight: 16px
    letterSpacing: 0px
  label-lg:
    fontFamily: Google Sans
    fontSize: 16px
    fontWeight: 500
    lineHeight: 20px
    letterSpacing: 0px
  label-md:
    fontFamily: Google Sans
    fontSize: 14px
    fontWeight: 500
    lineHeight: 18px
    letterSpacing: 0px
  label-sm:
    fontFamily: Google Sans
    fontSize: 12px
    fontWeight: 500
    lineHeight: 16px
    letterSpacing: 0px
  utility-md:
    fontFamily: Roboto
    fontSize: 14px
    fontWeight: 400
    lineHeight: 14px
    letterSpacing: 0px
rounded:
  none: 0px
  sm: 4px
  md: 8px
  lg: 20px
  xl: 100px
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 20px
  xl: 24px
  xxl: 40px
  page: 32px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.neutral}"
    typography: "{typography.label-md}"
    rounded: "{rounded.xl}"
    padding: "10px 12px"
    height: "40px"
  button-secondary:
    backgroundColor: "{colors.tertiary}"
    textColor: "{colors.secondary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.xl}"
    padding: "10px 12px"
    height: "40px"
  button-link:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.none}"
    padding: "0px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.md}"
    padding: "16px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-lg}"
    rounded: "{rounded.xl}"
    padding: "12px 16px"
  chip:
    backgroundColor: "{colors.tertiary}"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.xl}"
    padding: "8px 12px"
# Google Search Light

## Overview
This interface is extremely minimal, calm, and utility-first. It feels familiar and trustworthy, with a spacious center-weighted composition that puts search interaction ahead of visual decoration. The tone is professional but friendly, using bright white space, soft gray surfaces, and a single strong blue accent to guide action.

## Colors
- **Primary (#0b57d0):** A vivid Google blue used for the main sign-in action and key emphasis. It should signal interactivity and the highest priority call to action.
- **Secondary (#3c4043):** A restrained charcoal used for secondary text and controls. It keeps the page readable without competing with the logo or primary action.
- **Tertiary (#f8f9fa):** A very light cool gray used for secondary buttons and subtle interactive surfaces. It provides separation from pure white while remaining nearly neutral.
- **Neutral (#ffffff):** The dominant page background and card base. The system relies on this bright neutral to create a clean, open, low-friction feel.
- **Surface (#ffffff):** Matches the neutral background and is used for fields and cards that need to sit quietly in the layout.
- **On-surface (#1f1f1f):** The main body text color for icons, labels, and navigation copy. It provides strong contrast without appearing harsh black.
- **Border (#e5e7eb):** A soft light-gray border used to define inputs and cards. It adds structure through outline rather than heavy shadow.
- **Muted (#5f6368):** A softer text tone for less prominent navigation and footer links. It supports hierarchy while staying readable.
- **Error (#d93025):** Reserved for system feedback states; it should remain rare and highly noticeable.

## Typography
Roboto is the primary reading face, supporting the familiar product feel across navigation, footer links, and general text. Google Sans is used for button labels and more branded UI moments, giving actions a slightly friendlier, more polished presence.

Headlines are bold and compact, with `headline-display` and `headline-lg` reserved for the most prominent content and `headline-md`/`headline-sm` for smaller emphasis. Body text stays plain and highly legible, typically in `body-md` or `body-lg`, with no decorative tracking or uppercase treatment.

Labels and button text should use `label-md` or `label-sm` in Google Sans for a clean UI rhythm. Letter spacing remains neutral at `0px`, and the system avoids all-caps styling so the interface feels approachable rather than ceremonial.

## Layout & Spacing
The page is fluid and center-weighted rather than constrained by a visible content column. Large amounts of whitespace frame the logo, search field, and action buttons, making the middle of the screen the clear focal point.

Spacing is based on a modest scale: `4px`, `8px`, `16px`, `20px`, and `24px`, with larger page-level breathing room where needed. Controls should generally use compact internal padding, while the surrounding layout should stay expansive and airy.

Cards, inputs, and button groups should remain horizontally balanced and lightly separated, not densely packed. Footer and header regions use small edge padding and minimal visual weight so the center of the page stays dominant.

## Elevation & Depth
Depth is very subtle and mostly achieved through soft shadows and thin borders rather than layered surfaces. The search bar uses a gentle shadow and a light outline to lift it from the page without feeling heavy.

Buttons and cards stay mostly flat, with the secondary visual separation coming from tonal contrast: white against near-white gray. There is no dramatic elevation language, no strong ambient shadow, and no complex layering.

## Shapes
The system uses soft, highly rounded geometry. Primary actions, search fields, and chips lean into pill shapes with `rounded.xl`, while cards use a gentler `rounded.md` to stay orderly and quiet.

This shape language makes the interface feel friendly, modern, and low-stress. Rectangular sharp corners are rare and mostly reserved for plain links or zero-padding utility elements.

## Components
**Buttons:** Use `button-primary` for the strongest call to action, with blue fill, white text, 10px/12px padding, 40px height, and pill rounding. Use `button-secondary` for quiet actions like search utilities; it should be light gray with dark text and the same compact height. Use `button-link` for text-only navigation, with no fill, no border, and no rounded container.

Button labels should be short, medium-weight, and vertically centered. Hover states should preserve the same hierarchy: primary remains blue, secondary can slightly deepen or tint within the gray range, and link buttons should stay visually understated.

**Search/Input:** The input shell is the most important component after the logo. It should be wide, white, pill-shaped, and bordered softly rather than boxed aggressively, with enough internal padding for icons at both ends.

Inside the field, icon treatments should remain monochrome and restrained. The search control should feel like a single unified bar, even when multiple icons or shortcuts appear inside it.

**Cards:** Use `card` for content containers that need light separation from the page. Keep borders thin, background white, shadows absent or minimal, and padding at 16px so the container feels clean rather than dense.

**Chips:** Use chip-like pills for compact, inline actions or modes. They should borrow the same light-gray background as secondary buttons, with small typography and rounded pill geometry.

**Navigation and Footer Links:** These should stay text-first, small, and low-emphasis. Links can be plain `button-link` behavior or equivalent body text with muted color, avoiding button styling unless a real action needs emphasis.

## Do's and Don'ts
- Do keep the center of the page visually dominant with generous whitespace around the search area.
- Do use blue sparingly for the most important action only.
- Do favor pill shapes and soft rounded geometry for interactive controls.
- Do use light gray borders and tonal surfaces instead of heavy shadows.
- Don't introduce dark backgrounds or saturated secondary colors.
- Don't over-style links or footer text; they should remain quiet and functional.
- Don't use sharp corners on primary controls unless the component is explicitly text-only.
- Don't clutter the layout with dense cards, stacked panels, or decorative framing.