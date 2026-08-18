export const USER_DELETED_OWN_DISCUSSION = "User deleted their own discussion";
export const ADMIN_FORCED_DELETE_DISCUSSION = "Admin forced delete the discussion";

/**
 * Reason string sent with discussion soft-delete requests.
 * Owner deletes use {@link USER_DELETED_OWN_DISCUSSION}; admin deletes of
 * another user's item use {@link ADMIN_FORCED_DELETE_DISCUSSION}.
 *
 * @param {boolean} isOwner
 * @returns {typeof USER_DELETED_OWN_DISCUSSION | typeof ADMIN_FORCED_DELETE_DISCUSSION}
 */
export function discussionDeletionReason(isOwner) {
  return isOwner ? USER_DELETED_OWN_DISCUSSION : ADMIN_FORCED_DELETE_DISCUSSION;
}
