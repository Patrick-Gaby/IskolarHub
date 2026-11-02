/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Green = opportunity/success, amber = approaching deadline, red = urgent/ineligible reasons.
        primary: { 50: "#f0fdf4", 100: "#dcfce7", 600: "#15803d", 700: "#166534", 800: "#14532d" },
        warn: { 100: "#fef3c7", 700: "#92400e" },
        danger: { 100: "#fee2e2", 700: "#991b1b" },
      },
      // Centralised motion tokens (values live in styles/design-tokens.css).
      transitionDuration: {
        fast: "var(--motion-fast)",
        standard: "var(--motion-standard)",
        emphasis: "var(--motion-emphasis)",
        layout: "var(--motion-layout)",
      },
      transitionTimingFunction: {
        enter: "var(--ease-enter)",
        exit: "var(--ease-exit)",
        standard: "var(--ease-standard)",
      },
    },
  },
  plugins: [],
};
