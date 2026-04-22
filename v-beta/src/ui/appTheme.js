/** Shared layout + color tokens for app-shell pages (main, wall detail, etc.). */

export const fontFamily = "var(--app-font-family)";

export const colors = {
  bg: "var(--app-bg)",
  text: "var(--app-text)",
  muted: "var(--app-muted)",
  subtle: "var(--app-subtle)",
  zinc600: "var(--app-zinc-600)",
  border: "var(--app-border)",
  borderHairline: "var(--app-border-hairline)",
  primary: "var(--app-primary)",
  primaryDark: "var(--app-primary-dark)",
  accentSoft: "var(--app-accent-soft)",
  surfaceAlt: "var(--app-muted-surface)",
  danger: "var(--app-danger)",
  dangerBg: "var(--app-danger-bg)",
  dangerBorder: "var(--app-danger-border)",
};

export const layout = {
  /** Full-page column shell */
  main: {
    minHeight: "var(--app-layout-main-min-height)",
    padding: "var(--app-layout-main-padding)",
    fontFamily,
    background: colors.bg,
    color: colors.text,
  },
  /** Padding-only shell (loading / error states) */
  mainCompact: {
    minHeight: "var(--app-layout-main-min-height)",
    padding: "var(--app-layout-main-compact-padding)",
    fontFamily,
    background: colors.bg,
  },
  maxWidth960: { maxWidth: "var(--app-max-width-960)", margin: "0 auto" },
  maxWidth640: { maxWidth: "var(--app-max-width-640)", margin: "0 auto" },
  maxWidth560: { maxWidth: "var(--app-max-width-560)", margin: "0 auto" },
};

export const card = {
  surface: {
    background: "var(--app-card-surface-bg)",
    borderRadius: "var(--app-card-radius)",
    border: "1px solid var(--app-border)",
    boxShadow: "var(--app-card-shadow)",
  },
  accentBar: {
    position: "absolute",
    left: 0,
    top: 0,
    bottom: 0,
    width: "var(--app-card-accent-bar-width)",
    background:
      "linear-gradient(180deg, var(--app-primary) 0%, var(--app-primary-dark) 100%)",
  },
};

export const buttons = {
  secondary: {
    padding: "var(--app-btn-secondary-padding)",
    cursor: "pointer",
    borderRadius: "var(--app-btn-secondary-radius)",
    border: "1px solid var(--app-border-hairline)",
    background: "var(--app-btn-secondary-bg)",
    color: colors.zinc600,
    fontSize: "var(--app-btn-secondary-font-size)",
    fontWeight: "var(--app-btn-secondary-font-weight)",
  },
  primary: {
    padding: "var(--app-btn-primary-padding)",
    cursor: "pointer",
    borderRadius: "var(--app-btn-primary-radius)",
    border: "none",
    background: colors.primary,
    color: "var(--app-btn-primary-fg)",
    fontSize: "var(--app-btn-primary-font-size)",
    fontWeight: "var(--app-btn-primary-font-weight)",
  },
};
