---
version: alpha
name: Material Modern
description: An airy, expressive Material system with soft surfaces, rounded geometry, and a confident purple accent.
colors:
  primary: "#6442D6"
  secondary: "#EAE4FF"
  tertiary: "#F3D9F2"
  neutral: "#FEFBFF"
  surface: "#FFFFFF"
  on-surface: "#1C1B1D"
  muted: "#5D5A62"
  border: "#E5E7EB"
  error: "#B3261E"
  accent-weak: "#EFE7FF"
typography:
  headline-display:
    fontFamily: "Google Sans"
    fontSize: "96px"
    fontWeight: 475
    lineHeight: "96px"
    letterSpacing: "0px"
  headline-lg:
    fontFamily: "Google Sans"
    fontSize: "61px"
    fontWeight: 475
    lineHeight: "64px"
    letterSpacing: "0px"
  headline-md:
    fontFamily: "Google Sans Text"
    fontSize: "39px"
    fontWeight: 475
    lineHeight: "47px"
    letterSpacing: "0px"
  headline-sm:
    fontFamily: "Google Sans Text"
    fontSize: "25px"
    fontWeight: 475
    lineHeight: "30px"
    letterSpacing: "0px"
  body-lg:
    fontFamily: "Google Sans Text"
    fontSize: "18px"
    fontWeight: 400
    lineHeight: "28px"
    letterSpacing: "0px"
  body-md:
    fontFamily: "Google Sans Text"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    letterSpacing: "0px"
  body-sm:
    fontFamily: "Google Sans Text"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0px"
  label-lg:
    fontFamily: "Google Sans Text"
    fontSize: "24px"
    fontWeight: 475
    lineHeight: "28px"
    letterSpacing: "0px"
  label-md:
    fontFamily: "Google Sans Text"
    fontSize: "16px"
    fontWeight: 475
    lineHeight: "24px"
    letterSpacing: "0px"
  label-sm:
    fontFamily: "Google Sans Text"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: "16px"
    letterSpacing: "0.02em"
  button-lg:
    fontFamily: "Google Sans"
    fontSize: "24px"
    fontWeight: 475
    lineHeight: "28px"
    letterSpacing: "0px"
  button-md:
    fontFamily: "Google Sans Text"
    fontSize: "18px"
    fontWeight: 475
    lineHeight: "24px"
    letterSpacing: "0px"
  caption:
    fontFamily: "Google Sans Text"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
    letterSpacing: "0.04em"
rounded:
  none: 0px
  sm: 4px
  md: 8px
  lg: 20px
  xl: 48px
  full: 9999px
spacing:
  xs: 8px
  sm: 20px
  md: 48px
  lg: 64px
  xl: 96px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface}"
    typography: "{typography.button-lg}"
    rounded: "{rounded.full}"
    padding: "28px 48px"
    height: "80px"
  button-primary-hover:
    backgroundColor: "{colors.on-surface}"
    textColor: "{colors.surface}"
    rounded: "{rounded.full}"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface}"
    typography: "{typography.button-lg}"
    rounded: "{rounded.full}"
    padding: "28px 48px"
    height: "80px"
  button-tertiary:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    typography: "{typography.button-md}"
    rounded: "{rounded.none}"
    padding: "0px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.sm}"
    padding: "16px"
  card-soft:
    backgroundColor: "{colors.neutral}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.xl}"
    padding: "24px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.full}"
    padding: "12px 16px"
  chip:
    backgroundColor: "{colors.accent-weak}"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "8px 12px"
---

# Material Modern

## Overview
This system feels distinctly Material: approachable, polished, and product-focused, with a light editorial warmth rather than strict enterprise minimalism. The interface is spacious and airy, using large hero typography, soft lavender surfaces, and rounded controls to make the experience feel friendly and current. It is designed for a broad consumer and developer audience, balancing clarity, delight, and strong visual hierarchy.

## Colors
- **Primary (#6442D6):** A vivid material purple used for the main call to action, selected states, and key emphasis. It carries the strongest brand recognition in the system.
- **Secondary (#EAE4FF):** A pale lavender used for subtle highlights, supporting containers, and gentle contrast against the white canvas.
- **Tertiary (#F3D9F2):** A soft pink-lilac accent that adds warmth and keeps the palette expressive without feeling loud.
- **Neutral (#FEFBFF):** The main background tint, slightly warmer than pure white, creating a soft paper-like stage for content.
- **Surface (#FFFFFF):** The cleanest surface color for cards, panels, and inputs where maximum legibility is needed.
- **On-surface (#1C1B1D):** A near-black text color that gives headlines and UI labels strong contrast while avoiding harsh pure black.
- **Muted (#5D5A62):** A restrained gray-violet for supporting copy, metadata, and secondary labels.
- **Border (#E5E7EB):** A light neutral border for cards and structural separation when shadow is intentionally minimized.
- **Error (#B3261E):** A clear semantic red reserved for validation states and destructive feedback.
- **Accent-weak (#EFE7FF):** A very light purple used for chips, selected pills, and soft emphasis backgrounds.

## Typography
Typography is led by Google Sans and Google Sans Text, which keeps the interface modern, rounded, and highly readable. Large display headings use the Google Sans family with a 475 weight to create a distinctive but not overly heavy presence; this same measured weight appears across most headings, giving the system a calm, composed voice. Body text relies on Google Sans Text at 16px/24px for strong readability, while labels and buttons use the same family at larger sizes to preserve consistency in a product-forward interface.

Headlines should remain clean and unforced, with no decorative casing or dramatic tracking. The visible source favors sentence case and generous scale jumps rather than uppercase labels, so avoid shouting with all-caps UI unless absolutely necessary. Small captions can use slightly increased letter spacing for utility-style metadata, but most text should stay neutral and legible.

## Layout
The layout is expansive and highly padded, with large content blocks separated by generous whitespace. Cards and panels appear in a loose grid rather than a dense modular matrix, which gives the page a showcase feel and lets hero imagery breathe. The spacing rhythm follows clear jumps at 8px, 20px, 48px, 64px, and 96px, so components should feel either compact or distinctly open rather than mildly spaced.

Primary containers favor wide horizontal alignment and rounded rectangles, often with large internal padding and minimal edge clutter. Section composition should prioritize strong left alignment, broad gutters, and comfortable vertical separation between headline, body copy, and CTA. Use the larger spacing tokens for page-level section breaks and the smaller tokens for icon-text groups, button internals, and list items.

## Elevation & Depth
The system is intentionally flat and relies more on color layering and soft border contrast than on shadow depth. Cards typically sit on pale or white surfaces with subtle borders, while inner content blocks use tonal variation to create separation. When depth is needed, prefer gentle contrast, large rounded containers, and nested surface colors over heavy shadows.

This makes the interface feel clean and contemporary, while still allowing featured areas to stand out through size and color. Shadows should be used sparingly, if at all, and never compete with the content hierarchy. The overall effect is light, soft, and materially grounded rather than physically raised.

## Shapes
The shape language is soft and approachable, with a strong preference for rounded rectangles and pill forms. Interactive controls lean toward `rounded.full`, while cards and panels use smaller but still friendly radii to keep the interface from feeling rigid. The result is a system that feels polished and consumer-friendly, not sharp or technical.

Avoid angular corners on major interactive elements. Even media containers and hero cards should preserve the rounded, tactile language so the experience remains cohesive. Small utility elements can use `rounded.sm`, but the dominant shape memory should be curvature and softness.

## Components
Buttons are the most expressive component in the system. `button-primary` uses the purple brand color, white text, large text sizing, and pill rounding with substantial padding to create a strong invitation to act. `button-primary-hover` can deepen to the dark on-surface tone for stronger contrast, while `button-secondary` should remain transparent with an outline or tonal separation and the same generous pill geometry. `button-tertiary` is reserved for low-emphasis actions and should read like a quiet text button, not a mini primary button.

Cards should feel spacious and editorial, with either `card` for neutral white surfaces or `card-soft` for tinted feature blocks. Keep card borders subtle and rely on internal padding and image composition to create structure. Do not overload cards with multiple competing shadows or heavy dividers.

Inputs should use the `input` style: white background, rounded full geometry, medium body typography, and comfortable horizontal padding. They should look calm and accessible, with clear text contrast and enough height to feel touch-friendly. Chips should use `chip` with a soft lavender fill and compact pill shaping for tags, filters, and selected states.

For lists and metadata clusters, use compact spacing, muted text, and clear alignment over decorative separators. Icons should feel simple and geometric, matching the rounded, utility-first mood of the rest of the system. Tooltips, selection states, and small badges should stay tonal and lightweight rather than saturated or shadow-heavy.

## Do's and Don'ts
- Do use the purple primary color for the highest-priority action only.
- Do keep layouts spacious, with generous section padding and strong alignment.
- Do use Google Sans for headlines and Google Sans Text for body and UI labels.
- Do prefer soft surfaces, subtle borders, and rounded corners over heavy shadow effects.
- Don't introduce sharp corners on major buttons, cards, or inputs.
- Don't use many saturated colors at once; keep the palette focused on lavender, purple, and neutral tones.
- Don't shrink typography too aggressively; the system depends on clear scale and breathing room.
- Don't make secondary actions look more important than the primary CTA.