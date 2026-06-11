# Flocko — Landing Page

## Overview

**Flocko** is a food delivery landing page built with pure HTML and CSS. It features a modern, clean UI with a sticky navbar, animated hero section, order status cards, statistics, and a restaurant grid — all without any external JavaScript or CSS frameworks.

---

## Tech Stack

| Layer  | Technology                        |
|--------|-----------------------------------|
| Markup | HTML5                             |
| Style  | CSS3 (Custom Properties, Grid, Flexbox, Keyframe Animations) |
| Fonts  | Google Fonts — **Fraunces** (headings) & **DM Sans** (body)  |

---

## Design Tokens (CSS Custom Properties)

```css
--primary : #22C55E   /* Green — brand color */
--accent  : #16A34A   /* Darker green — hover / emphasis */
--bg      : #F8FAFC   /* Page background */
--surface : #FFFFFF   /* Card / nav background */
--text    : #1F2937   /* Primary text */
--muted   : #6B7280   /* Secondary text */
--border  : #E5E7EB   /* Borders & dividers */
--error   : #EF4444   /* Error state */
--warning : #F59E0B   /* Ratings / warnings */
```

---

## Page Structure

### 1. Navigation (`<nav>`)

- **Logo** — "Flocko" rendered with the `Fraunces` serif font; "cko" uses the accent color.
- **Links** — Restaurants · Orders · Offers.
- **CTA Buttons** — *Log in* (outline) and *Sign up* (primary).
- Sticky at the top (`position: sticky; top: 0`).

### 2. Hero Section

Split into two columns via CSS Grid:

| Left — Content | Right — Visual |
|----------------|----------------|
| Tag badge ("Now live in your city") | Three **order status cards** (Delivered, Preparing, Placed) |
| Heading with highlighted keyword | Stats row: 1K+ Restaurants · 30m Avg Delivery · 10K Happy Users |
| Subtitle paragraph | |
| "Order now" button + delivery time note | |

Both columns use a `fadeUp` entrance animation.

### 3. Popular Restaurants Grid

A 3-column CSS Grid showcasing restaurant cards:

| Restaurant    | Rating | Delivery Time | Price for Two |
|---------------|--------|---------------|---------------|
| Green Kitchen | ★ 4.8  | 25–35 min     | ₹150          |
| Pizza House   | ★ 4.5  | 30–40 min     | ₹300          |
| Noodle Bar    | ★ 4.6  | 20–30 min     | ₹200          |

Each card has a colored thumbnail, hover lift effect, and meta info row.

### 4. Footer

Simple centered copyright line:  
`© 2026 Flocko · Built for learning, designed with care.`

---

## Key CSS Techniques

| Technique | Where Used |
|-----------|-----------|
| **CSS Custom Properties** | Theming across the entire page |
| **CSS Grid** | Hero two-column layout, stats row, restaurant grid |
| **Flexbox** | Nav, order cards, hero actions, meta rows |
| **`@keyframes fadeUp`** | Hero content & visual entrance animation |
| **`position: sticky`** | Navbar stays fixed on scroll |
| **Pseudo-element (`::before`)** | Green dot indicator inside the hero tag badge |
| **Hover transitions** | Buttons, nav links, restaurant cards |

---

## Component Reference

### Buttons

| Class         | Style              |
|---------------|--------------------|
| `.btn-primary`| Green filled       |
| `.btn-outline`| Transparent border |

### Badges

| Class          | Color   | Use Case   |
|----------------|---------|------------|
| `.badge-green` | Green   | Delivered  |
| `.badge-amber` | Amber   | Preparing  |
| `.badge-blue`  | Blue    | Placed     |

### Food Icons

| Class          | Background |
|----------------|------------|
| `.food-icon.green` | `#DCFCE7` |
| `.food-icon.amber` | `#FEF3C7` |
| `.food-icon.red`   | `#FEE2E2` |

---

## Source Code

The full HTML file is self-contained — all styles are embedded in a `<style>` block inside `<head>`. No external CSS or JS files are required.

```
flocko-landing-page.html   <!-- single-file page -->
```

---

## Preview

> Open the HTML file directly in any modern browser — no build step needed.

---

*Last updated: March 5, 2026*

