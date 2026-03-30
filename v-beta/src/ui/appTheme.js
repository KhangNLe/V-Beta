/** Shared layout + color tokens for app-shell pages (main, wall detail, etc.). */

export const fontFamily =
  'system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", sans-serif';

export const colors = {
  bg: "#f4f4f5",
  text: "#18181b",
  muted: "#52525b",
  subtle: "#71717a",
  zinc600: "#3f3f46",
  border: "#e4e4e7",
  borderHairline: "#d4d4d8",
  primary: "#2563eb",
  primaryDark: "#1d4ed8",
  danger: "#b91c1c",
  dangerBg: "#fef2f2",
  dangerBorder: "#fecaca",
};

export const layout = {
  /** Full-page column shell */
  main: {
    minHeight: "100vh",
    padding: "28px 24px 48px",
    fontFamily,
    background: colors.bg,
    color: colors.text,
  },
  /** Padding-only shell (loading / error states) */
  mainCompact: {
    minHeight: "100vh",
    padding: "28px 24px",
    fontFamily,
    background: colors.bg,
  },
  maxWidth960: { maxWidth: "960px", margin: "0 auto" },
  maxWidth640: { maxWidth: "640px", margin: "0 auto" },
  maxWidth560: { maxWidth: "560px", margin: "0 auto" },
};

export const card = {
  surface: {
    background: "#fff",
    borderRadius: "12px",
    border: `1px solid ${colors.border}`,
    boxShadow: "0 1px 2px rgba(0, 0, 0, 0.05)",
  },
  accentBar: {
    position: "absolute",
    left: 0,
    top: 0,
    bottom: 0,
    width: "4px",
    background: `linear-gradient(180deg, ${colors.primary} 0%, ${colors.primaryDark} 100%)`,
  },
};

export const buttons = {
  secondary: {
    padding: "8px 14px",
    cursor: "pointer",
    borderRadius: "8px",
    border: `1px solid ${colors.borderHairline}`,
    background: "#fff",
    color: colors.zinc600,
    fontSize: "0.875rem",
    fontWeight: 500,
  },
  primary: {
    padding: "10px 16px",
    cursor: "pointer",
    borderRadius: "8px",
    border: "none",
    background: colors.primary,
    color: "#fff",
    fontSize: "0.875rem",
    fontWeight: 600,
  },
};
