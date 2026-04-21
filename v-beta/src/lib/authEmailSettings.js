import { APP_ORIGIN } from "@/app/envExports";

/**
 * 
 * This function is used to get the email action code settings for the Firebase Authentication.
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
