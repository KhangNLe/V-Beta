import { APP_ORIGIN } from "@/app/envExports";

/**
 * Firebase console (team checklist):
 * - Authentication: enable Email/Password (and any OAuth providers you use).
 * - Authentication → Templates: customize "Email address verification" and "Password reset".
 * - Authentication → Settings → Authorized domains: include every origin used in `url` below
 *   (e.g. `localhost`, production host). Add `NEXT_PUBLIC_APP_ORIGIN` when the public URL differs
 *   from `window.location.origin` (reverse proxy, preview deploys).
 *
 * @param {string} [continuePath] pathname + query only, e.g. `/verify-email` or `/login`
 * @returns {import("firebase/auth").ActionCodeSettings | undefined}
 */
export function getEmailActionCodeSettings(continuePath = "/verify-email") {
  if (typeof window === "undefined") return undefined;
  const base = (APP_ORIGIN || window.location.origin).replace(/\/$/, "");
  const path = continuePath.startsWith("/") ? continuePath : `/${continuePath}`;
  return {
    url: `${base}${path}`,
    handleCodeInApp: false,
  };
}
