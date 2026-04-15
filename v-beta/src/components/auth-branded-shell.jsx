"use client"

import Link from "next/link"
import { useState } from "react"

/**
 * Login/signup layout: logo lockup, hero art, and scoped typography (colors from global shadcn :root tokens).
 */
export function AuthBrandedShell({ children }) {
  const [authBgIndex] = useState(() => Math.floor(Math.random() * 6) + 1)

  return (
    <div className="auth-brand text-foreground grid min-h-svh bg-background lg:grid-cols-2">
      <div className="auth-hero-panel relative order-2 hidden lg:order-1 lg:block">
        <img
          src={`/auth-bg-${authBgIndex}.svg`}
          alt=""
          className="auth-hero-image absolute inset-0 h-full w-full object-cover"
        />
        <div className="auth-hero-scrim pointer-events-none absolute inset-0" aria-hidden />
      </div>
      <div className="order-1 flex flex-col gap-6 p-6 md:p-10 lg:order-2">
        <header className="flex justify-center md:justify-start">
          <Link
            href="/"
            className="auth-header-lockup flex items-center gap-3 transition-opacity hover:opacity-90"
            aria-label="V-Beta home"
          >
            <img
              src="/logo.svg"
              alt=""
              width={160}
              height={48}
              className="h-10 w-auto shrink-0 object-contain object-left"
            />
            <span className="auth-wordmark">V-Beta</span>
          </Link>
        </header>
        <div className="flex flex-1 items-center justify-center">
          <div className="w-full max-w-xs">{children}</div>
        </div>
      </div>
    </div>
  )
}
