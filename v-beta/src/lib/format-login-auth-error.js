const GENERIC = "Login failed. Please try again."

const MESSAGES = {
  "auth/invalid-credential": "Incorrect email or password.",
  "auth/invalid-email": "Enter a valid email address.",
  "auth/user-disabled": "This account has been disabled.",
  "auth/too-many-requests": "Too many attempts. Try again later.",
  "auth/network-request-failed": "Network error. Check your connection.",
  "auth/popup-closed-by-user": "Sign-in was cancelled.",
  "auth/popup-blocked": "Pop-up was blocked. Allow pop-ups for this site and try again.",
  "auth/cancelled-popup-request": "Sign-in was cancelled.",
  "auth/account-exists-with-different-credential":
    "An account already exists with this email using a different sign-in method.",
}

/**
 * User-facing login error copy. Never returns Firebase's raw `err.message`.
 * @param {unknown} err
 * @returns {string}
 */
export function formatLoginAuthError(err) {
  if (!err || typeof err !== "object" || !("code" in err)) {
    return GENERIC
  }
  const { code } = err
  if (typeof code !== "string") {
    return GENERIC
  }
  if (Object.prototype.hasOwnProperty.call(MESSAGES, code)) {
    return MESSAGES[code]
  }
  if (code.startsWith("auth/")) {
    return GENERIC
  }
  return GENERIC
}
