---
version: alpha
name: Microsoft 365 Light
description: A clean enterprise landing-page system with soft lavender-blue atmosphere, bold blue CTAs, and restrained Microsoft-style typography.
colors:
  primary: "#464FEB"
  secondary: "#FFFFFF"
  tertiary: "#000000"
  neutral: "#E5E7EB"
  surface: "#FFF4ED"
  on-surface: "#000000"
  background: "#FFF4ED"
  text: "#000000"
  accent: "#464FEB"
  border: "#E5E7EB"
  subtle: "#DDE4FF"
  error: "#D83B01"
typography:
  headline-display:
    fontFamily: "Segoe UI"
    fontSize: "50px"
    fontWeight: 400
    lineHeight: "65px"
    letterSpacing: "0px"
  headline-lg:
    fontFamily: "Segoe UI"
    fontSize: "39px"
    fontWeight: 400
    lineHeight: "47px"
    letterSpacing: "0px"
  headline-md:
    fontFamily: "Segoe UI"
    fontSize: "30px"
    fontWeight: 400
    lineHeight: "44px"
    letterSpacing: "0px"
  headline-sm:
    fontFamily: "Segoe UI"
    fontSize: "23px"
    fontWeight: 400
    lineHeight: "28px"
    letterSpacing: "0px"
  body-lg:
    fontFamily: "Segoe UI"
    fontSize: "18px"
    fontWeight: 400
    lineHeight: "26px"
    letterSpacing: "0px"
  body-md:
    fontFamily: "Segoe UI"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    letterSpacing: "0px"
  body-sm:
    fontFamily: "Segoe UI"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0px"
  label-lg:
    fontFamily: "Segoe UI"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: "20px"
    letterSpacing: "0px"
  label-md:
    fontFamily: "Segoe UI"
    fontSize: "14px"
    fontWeight: 600
    lineHeight: "18px"
    letterSpacing: "0px"
  label-sm:
    fontFamily: "Segoe UI"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: "16px"
    letterSpacing: "0px"
  link-md:
    fontFamily: "Segoe UI"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0px"
  nav-md:
    fontFamily: "Segoe UI"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "18px"
    letterSpacing: "0px"
rounded:
  none: 0px
  sm: 4px
  md: 8px
  lg: 12px
  xl: 16px
  full: 9999px
spacing:
  xs: 6px
  sm: 16px
  md: 30px
  lg: 58px
  xl: 172px
  gutter: 24px
  section: 48px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.secondary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.sm}"
    padding: "12px 12px"
    height: "40px"
    width: "200px"
  button-secondary:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.primary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.sm}"
    padding: "12px 12px"
    height: "40px"
    width: "200px"
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
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.sm}"
    padding: "12px 12px"
  chip:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.primary}"
    rounded: "{rounded.full}"
    padding: "6px 10px"
---

# Microsoft 365 Light

## Overview
This is a polished, enterprise-first marketing system with a bright, airy feel and a strong Microsoft product cadence. The tone is professional and reassuring rather than playful, using generous whitespace, soft pastel surfaces, and a single vivid blue accent to guide attention. It feels spacious and modern, with centered hero content and clear call-to-action hierarchy.

## Colors
- **Primary (#464FEB):** A saturated Microsoft blue used for the main call-to-action, links, and interactive emphasis. It carries the brand energy and is the strongest color in the system.
- **Secondary (#FFFFFF):** Pure white used for buttons, top navigation, and contrast against the soft background. It keeps the interface crisp and legible.
- **Tertiary (#000000):** Deep black used for the most important text and high-contrast UI elements. It provides a neutral, authoritative anchor.
- **Neutral (#E5E7EB):** A light gray border tone used for subtle separation and low-emphasis outlines. It supports structure without adding visual weight.
- **Surface (#FFF4ED):** A warm, very light cream-lavender surface that defines the page atmosphere. It softens the layout and keeps the landing page feeling approachable.
- **On-surface (#000000):** Primary text color on light surfaces. Use it for body copy, headings, and utility text where maximum readability is needed.
- **Background (#FFF4ED):** The main page background, matching the soft overall canvas. It helps large empty areas feel intentional rather than blank.
- **Accent (#464FEB):** Mirrors the primary blue and should be used for interactive highlights, key links, and brand moments.
- **Border (#E5E7EB):** Standard divider and card border color for restrained framing.
- **Subtle (#DDE4FF):** A soft cool tint useful for gentle hover states, backplates, or decorative UI layers.
- **Error (#D83B01):** Reserved for destructive states and validation messaging; it should remain rare in this otherwise calm palette.

## Typography
Segoe UI is the defining typeface, with a clean system-native appearance that reinforces Microsoft’s platform identity. Headings are light in weight visually but structurally large, relying on size and spacing more than heavy bolding; the source uses 50px, 39px, 30px, and 23px tiers for clear hierarchy. Body text sits at 18px or 16px with comfortable line heights, while labels and buttons use semibold weights to improve scannability. Letter spacing is neutral and uppercase styling is not a core convention in the observed UI.

## Layout
The page uses a centered, fixed-max-width hero composition with generous side margins and a very large vertical rhythm. Content is stacked in a simple single-column flow: brand header, hero copy, button row, then a wide visual showcase image. Spacing is intentionally expansive, with `xs` at 6px for micro gaps and `xl` at 172px for major separation; sections should favor 30px to 58px spacing steps to preserve the roomy Microsoft feel.

## Elevation & Depth
Depth is mostly minimal and relies on contrast, borders, and large image framing rather than strong shadows. The interface is largely flat, with only subtle shadowing in supporting imagery and soft separation lines in the navigation. Cards and controls should avoid dramatic elevation; instead, use clean outlines, tonal layering, and spacing to establish hierarchy.

## Shapes
The shape language is restrained and slightly rounded, with 4px radii on primary controls and 8px on cards. This creates a pragmatic, enterprise-friendly feel without looking sharp or industrial. Use full rounding only for small chips or avatar-style elements where a softer pill is appropriate.

## Components
Buttons are the strongest interactive element in the system. `button-primary` uses the blue fill, white text, 4px radius, and a compact 40px height with 12px vertical padding; it should feel decisive and prominent. `button-secondary` inverts that treatment with a white fill and blue border/text, matching the outline CTA seen beside the primary action. `button-link` is text-only and understated, used for tertiary actions like signup prompts or nav utilities.

Cards should use `card` styling: white or warm-surface backgrounds, 8px corners, a 1px neutral border, and 16px padding. Keep cards visually quiet so the content inside does the work; avoid shadows unless a particular module truly needs separation.

Inputs should feel consistent with the button language: white background, 4px radius, and compact padding. Borders should remain light and functional, with a focus state that leans on the primary blue rather than heavy glow effects.

Chips, tags, and small badges should be simple and pill-like, using `chip` with full rounding, modest horizontal padding, and blue text on white or lightly tinted backgrounds. Navigation items should remain text-first and lightweight, with dropdown indicators and no filled backgrounds by default.

## Do's and Don'ts
- Do keep the visual hierarchy simple: one bold primary CTA, one outline secondary CTA, and one low-emphasis text link.
- Do preserve the spacious hero layout and avoid crowding the center column.
- Do use Segoe UI consistently for headings, body copy, buttons, and navigation.
- Do keep borders and corners subtle; 4px to 8px is the right range for most elements.
- Don't introduce heavy shadows, dark surfaces, or dense card grids that fight the airy landing-page feel.
- Don't replace the blue accent with multiple competing accent colors.
- Don't over-style labels or navigation with excessive uppercase, tracking, or decorative treatments.
- Don't let secondary actions visually overpower the primary button.