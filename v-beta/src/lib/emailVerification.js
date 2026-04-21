/**
 * Email/password accounts must verify inbox via Firebase link before we treat email as confirmed.
 *
 * @param {import("firebase/auth").User | null} user
 * @returns {boolean}
 */
export function needsPasswordProviderEmailVerification(user) {
  if (!user || user.emailVerified) return false;
  return user.providerData.some((p) => p.providerId === "password");
}
