"use client";

import { colors, fontFamily, layout } from "@/ui/appTheme";

/** @param {{ message?: string, subStyle?: import("react").CSSProperties }} props */
export default function PageLoader({ message = "Loading…", subStyle }) {
  return (
    <div
      style={{
        ...layout.mainCompact,
        color: colors.subtle,
        ...subStyle,
      }}
    >
      {message}
    </div>
  );
}
