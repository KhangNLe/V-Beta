import {
  buildShortUploadFileName,
  SOLUTION_VIDEO_ACCEPT,
  toPlaybackStorageRefs,
  videoExtensionFromFile,
  videoUploadContentType,
  waitForPublicVideo,
} from "./videoUpload";

describe("videoUpload helpers", () => {
  it("maps mp4, webm, and iPhone MOV files", () => {
    expect(videoExtensionFromFile({ name: "clip.MP4", type: "" })).toBe("mp4");
    expect(videoExtensionFromFile({ name: "clip.webm", type: "video/webm" })).toBe("webm");
    expect(videoExtensionFromFile({ name: "IMG_1.MOV", type: "" })).toBe("mov");
    expect(videoExtensionFromFile({ name: "clip", type: "video/quicktime" })).toBe("mov");
    expect(videoUploadContentType({ name: "IMG_1.MOV", type: "" })).toBe("video/quicktime");
    expect(buildShortUploadFileName("IMG_99.MOV", 22)).toBe("beta_22.mov");
    expect(SOLUTION_VIDEO_ACCEPT).toContain(".mov");
  });

  it("rewrites MOV storage refs to the converted MP4", () => {
    expect(
      toPlaybackStorageRefs(
        "wall/problem/uuid-beta_22.mov",
        "https://storage.googleapis.com/b/uuid-beta_22.mov",
      ),
    ).toEqual({
      objectName: "wall/problem/uuid-beta_22.mp4",
      publicURL: "https://storage.googleapis.com/b/uuid-beta_22.mp4",
    });
    expect(toPlaybackStorageRefs("a.mp4", "https://cdn/a.mp4")).toEqual({
      objectName: "a.mp4",
      publicURL: "https://cdn/a.mp4",
    });
  });

  it("resolves when HEAD succeeds and times out when it does not", async () => {
    const ok = jest.fn().mockResolvedValue({ ok: true });
    await expect(
      waitForPublicVideo("https://cdn/a.mp4", { fetchImpl: ok, timeoutMs: 50, intervalMs: 10 }),
    ).resolves.toBeUndefined();

    const fail = jest.fn().mockResolvedValue({ ok: false, status: 404 });
    await expect(
      waitForPublicVideo("https://cdn/a.mp4", { fetchImpl: fail, timeoutMs: 20, intervalMs: 10 }),
    ).rejects.toThrow(/conversion to MP4 timed out/);
  });
});
