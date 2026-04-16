"use client"

import Link from "next/link"

/**
 * Login/signup layout: logo lockup, hero art, and typography.
 */
export function AuthBrandedShell({ children }) {
  return (
    <div className="grid min-h-svh bg-background text-foreground lg:grid-cols-2">
      <div className="relative order-2 hidden bg-foreground lg:order-1 lg:block dark:bg-card">
        <img
          src="/auth-bg.svg"
          alt=""
          className="absolute inset-0 h-full w-full object-cover opacity-[0.88]"
        />
        <div
          className="pointer-events-none absolute inset-0 bg-[linear-gradient(135deg,color-mix(in_srgb,var(--foreground)_58%,transparent)_0%,color-mix(in_srgb,var(--foreground)_38%,transparent)_100%)] dark:bg-[linear-gradient(135deg,color-mix(in_srgb,var(--background)_58%,transparent)_0%,color-mix(in_srgb,var(--background)_38%,transparent)_100%)]"
          aria-hidden
        />
      </div>
      <div className="order-1 flex flex-col gap-6 p-6 md:p-10 lg:order-2">
        <header className="flex justify-center md:justify-start">
          <Link
            href="/"
            className="flex items-center gap-0 transition-opacity hover:opacity-90"
            aria-label="V-Beta Landing Page"
          >
            <img
              src="/logo.svg"
              alt=""
              width={120}
              height={48}
              className="h-12 w-auto shrink-0 -mr-2 object-contain object-left"
            />
            <span className="font-[var(--font-inter),ui-sans-serif,system-ui,sans-serif] text-[1.25rem] leading-[1.1] font-bold tracking-[-0.03em] text-primary">
              V-Beta
            </span>
          </Link>
        </header>
        <div className="flex flex-1 items-center justify-center">
          <div className="w-full max-w-xs">{children}</div>
        </div>
      </div>
    </div>
  )
}
