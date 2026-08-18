import {
  ADMIN_FORCED_DELETE_DISCUSSION,
  USER_DELETED_OWN_DISCUSSION,
  discussionDeletionReason,
} from "@/lib/discussionDeletion";

describe("discussionDeletionReason", () => {
  it("uses the owner reason when the current user owns the discussion", () => {
    expect(discussionDeletionReason(true)).toBe("User deleted their own discussion");
    expect(discussionDeletionReason(true)).toBe(USER_DELETED_OWN_DISCUSSION);
  });

  it("uses the admin reason when an admin deletes another user's discussion", () => {
    expect(discussionDeletionReason(false)).toBe("Admin forced delete the discussion");
    expect(discussionDeletionReason(false)).toBe(ADMIN_FORCED_DELETE_DISCUSSION);
  });
});
