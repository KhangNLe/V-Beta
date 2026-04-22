"use client";

import { Spinner } from "@/components/ui/spinner";

/** @param {{ message?: string, subStyle?: import("react").CSSProperties }} props */
export default function PageLoader({ message, subStyle }) {
  const caption = typeof message === "string" ? message.trim() : "";

  return (
    <div
      className="flex min-h-[var(--app-layout-main-min-height)] flex-col items-center justify-center gap-3 bg-[var(--app-bg)] p-[var(--app-layout-main-compact-padding)] font-[var(--app-font-family)] text-[var(--app-subtle)]"
      style={subStyle}
    >
      <Spinner className="size-10 text-current" />
      {caption ? (
        <p className="m-0 text-center text-sm">{caption}</p>
      ) : null}
    </div>
  );
}
