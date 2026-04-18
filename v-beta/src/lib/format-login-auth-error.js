const LOGIN_GENERIC = "Login failed. Please try again."
const SIGNUP_GENERIC = "Sign up failed. Please try again."

const MESSAGES = {
  "auth/invalid-credential": "Incorrect email or password.",
  "auth/invalid-email": "Enter a valid email address.",
  "auth/user-disabled": "This account has been disabled.",
  "auth/too-many-requests": "Too many attempts. Try again later.",
  "auth/network-request-failed": "Network error. Check your connection.",
  "auth/popup-closed-by-user": "Login was cancelled.",
  "auth/popup-blocked": "Pop-up was blocked. Allow pop-ups for this site and try again.",
  "auth/cancelled-popup-request": "Login was cancelled.",
  "auth/account-exists-with-different-credential":
    "An account already exists with this email using a different Login method.",
  "auth/email-already-in-use": "An account already exists with this email.",
  "auth/weak-password": "Password is too weak. Try a longer mix of letters and numbers.",
}

/** Sign-up flows only: wording that would read wrong if shared with login. */
const SIGNUP_MESSAGE_OVERRIDES = {
  "auth/popup-closed-by-user": "Sign up was cancelled.",
  "auth/cancelled-popup-request": "Sign up was cancelled.",
}

/**
 * @param {unknown} err
 * @param {string} generic
 * @returns {string}
 */
function formatAuthError(err, generic) {
  if (!err || typeof err !== "object" || !("code" in err)) {
    return generic
  }
  const { code } = err
  if (typeof code !== "string") {
    return generic
  }
  if (Object.prototype.hasOwnProperty.call(MESSAGES, code)) {
    return MESSAGES[code]
  }
  if (code.startsWith("auth/")) {
    return generic
  }
  return generic
}

/**
 * User-facing login error copy. Never returns Firebase's raw `err.message`.
 * @param {unknown} err
 * @returns {string}
 */
export function formatLoginAuthError(err) {
  return formatAuthError(err, LOGIN_GENERIC)
}

/**
 * User-facing sign-up error copy. Never returns Firebase's raw `err.message`.
 * @param {unknown} err
 * @returns {string}
 */
export function formatSignupAuthError(err) {
  if (err && typeof err === "object" && "code" in err) {
    const { code } = err
    if (typeof code === "string" && Object.prototype.hasOwnProperty.call(SIGNUP_MESSAGE_OVERRIDES, code)) {
      return SIGNUP_MESSAGE_OVERRIDES[code]
    }
  }
  return formatAuthError(err, SIGNUP_GENERIC)
}
