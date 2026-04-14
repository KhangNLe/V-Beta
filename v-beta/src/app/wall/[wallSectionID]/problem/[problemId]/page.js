"use client";

import { fetchProblemForUser } from "@/api/wallSections";
import { postCommentForUser } from "@/api/comments";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { buttons, card, colors, layout, fontFamily } from "@/ui/appTheme";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";

/** @param {string | null | undefined} raw */
function formatCommentDate(raw) {
  if (!raw) return "Recently";
  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) return String(raw);
  return parsed.toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

/** @param {string | null | undefined} url */
function inferVideoMimeType(url) {
  const value = (url || "").toLowerCase();
  return value.endsWith(".webm") ? "video/webm" : "video/mp4";
}

export default function ProblemPage() {
  const router = useRouter();
  const params = useParams();
  const { user, ready } = useRequireAuth({ redirectMode: "push" });

  const [problem, setProblem] = useState(null);
  const [fetchError, setFetchError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [commentText, setCommentText] = useState("");
  const [submittingComment, setSubmittingComment] = useState(false);
  const [dropdownIndex, setDropdownIndex] = useState(null);
  const [perceivedGrade, setPerceivedGrade] = useState("VB");
  const [entryMode, setEntryMode] = useState("comment"); // "comment" | "file"
  const [solutionFile, setSolutionFile] = useState(null);

  const handleDeleteComment = async (commentIndex) => {
    // TODO: Implement backend API call to delete comment
    setDropdownIndex(null);
  };

  const handleSuggestPerceivedGrade = async () => {
    if (!perceivedGrade) return;
    // TODO: Implement backend API call to suggest perceived grade
    console.log("Suggesting perceived grade:", perceivedGrade);
    setPerceivedGrade("");
  };

  const rawWallSectionID = params?.wallSectionID;
  const rawProblemId = params?.problemId;

  const wallSectionID = useMemo(() => {
    const normalized = Array.isArray(rawWallSectionID) ? rawWallSectionID[0] : rawWallSectionID;
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [rawWallSectionID]);

  const problemId = useMemo(() => {
    const normalized = Array.isArray(rawProblemId) ? rawProblemId[0] : rawProblemId;
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [rawProblemId]);

  useEffect(() => {
    if (!ready || !user) return;
    if (!wallSectionID || !problemId) {
      setLoading(false);
      setFetchError("Invalid wall section or problem id.");
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        const problemData = await fetchProblemForUser(user, wallSectionID, problemId);
        if (cancelled) return;

        setProblem(problemData);
        setFetchError(null);
      } catch (err) {
        toast.error("Failed to fetch problem data:", err);
        if (!cancelled) setFetchError(err instanceof Error ? err.message : "Unknown error");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [ready, user, wallSectionID, problemId]);

  const handleBackToSection = () => {
    router.push(`/wall/${wallSectionID}`);
  };

  const handlePostComment = async () => {
    const charCount = commentText.length;
    if (!commentText.trim() || charCount > 250) return;
    setSubmittingComment(true);
    try {
      await postCommentForUser(user, problemId, commentText.trim());
      setCommentText("");
      const refreshedProblem = await fetchProblemForUser(user, wallSectionID, problemId);
      setProblem(refreshedProblem);
      toast.success("Comment posted successfully!");
    } catch (err) {
      toast.error("Failed to post comment:", err);
      setFetchError(err instanceof Error ? err.message : "Failed to post comment");
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleUploadSolutionBeta = () => {
    // TODO: Implement backend API call to upload solution beta
    if (!solutionFile) return;
    console.log("Selected beta file:", solutionFile.name, solutionFile.type);
  };

  if (!ready) return <PageLoader message="Loading…" />;
  if (!user) return <PageLoader message="Redirecting…" />;
  if (loading) return <PageLoader message="Loading problem…" />;

  const commentCharCount = commentText.length;
  const commentOverLimit = commentCharCount > 250;
  const canPostComment = !!commentText.trim() && !commentOverLimit && !submittingComment;

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>
        <button
          type="button"
          onClick={handleBackToSection}
          style={{ ...buttons.secondary, marginBottom: "16px" }}
        >
          Back to Wall Section
        </button>

        {fetchError && (
          <div style={{
            color: colors.danger,
            background: colors.dangerBg,
            border: `1px solid ${colors.dangerBorder}`,
            borderRadius: "8px",
            padding: "12px 14px",
            marginBottom: "20px"
          }}>
            {fetchError}
          </div>
        )}

        {problem && (
          <>
            {/* Problem Details Card */}
            <section
              style={{
                ...card.surface,
                position: "relative",
                padding: "22px 22px 22px 20px",
                marginBottom: "28px",
                overflow: "hidden",
                fontFamily,
              }}
            >
              <div style={card.accentBar} aria-hidden />
              <h1 style={{ margin: "0 0 8px", fontSize: "1.75rem", fontWeight: 700, color: colors.text }}>
                {problem.holdColor}
              </h1>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "baseline",
                  gap: "12px",
                  flexWrap: "wrap",
                }}
              >
                <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55 }}>
                  Assigned Grade: {problem.assignedGrade || "V?"}
                </p>
                <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55 }}>
                  Perceived Difficulty: {problem.perceiveGrade.trim() || "N/A"}
                </p>
              </div>
              <p style={{ margin: 0, color: colors.text, lineHeight: 1.55, maxWidth: "65ch" }}>
                Info: {problem.info || "No problem notes available."}
              </p>
            </section>

            {/* Discussion Section */}
            <section style={{ marginBottom: "28px" }}>
              <h2 style={{ margin: "0 0 16px", fontSize: "1.125rem", fontWeight: 600, color: colors.text }}>
                User Discussion
              </h2>

              {/* Comments List */}
              {problem.discussion && problem.discussion.length > 0 ? (
                <div
                  style={{
                    ...card.surface,
                    marginBottom: "24px",
                    overflow: "hidden",
                  }}
                >
                  {problem.discussion.map((comment, index) => (
                    <article
                      key={index}
                      style={{
                        padding: "16px",
                        borderTop: index > 0 ? `1px solid ${colors.border}` : "none",
                      }}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", position: "relative", gap: "12px" }}>
                        <div style={{ flex: 1 }}>
                          <p style={{ margin: "0 0 4px", fontWeight: 600, color: colors.text }}>
                            {comment.username || "Anonymous"}
                          </p>
                        </div>
                        <p style={{ margin: "0 0 8px", fontSize: "0.8rem", color: colors.subtle, whiteSpace: "nowrap" }}>
                          {formatCommentDate(comment.createdDate)}
                        </p>
                        <button
                          type="button"
                          onClick={() => setDropdownIndex(dropdownIndex === index ? null : index)}
                          style={{
                            background: "none",
                            border: "none",
                            fontSize: "1.5rem",
                            cursor: "pointer",
                            color: colors.muted,
                            padding: "0 8px",
                          }}
                        >
                          ⋮
                        </button>
                        {dropdownIndex === index && (
                          <div
                            style={{
                              position: "absolute",
                              top: "100%",
                              right: 0,
                              background: colors.surface,
                              border: `1px solid ${colors.muted}`,
                              borderRadius: "4px",
                              zIndex: 10,
                              minWidth: "120px",
                            }}
                          >
                            <button
                              type="button"
                              onClick={() => handleDeleteComment(index)}
                              style={{
                                width: "100%",
                                padding: "8px 12px",
                                background: "none",
                                border: "none",
                                color: colors.danger,
                                cursor: "pointer",
                                textAlign: "left",
                                fontSize: "0.875rem",
                              }}
                            >
                              Delete Comment
                            </button>
                          </div>
                        )}
                      </div>
                      {comment.comment == null && comment.videoURL ? (
                        <details>
                          <summary
                            style={{
                              color: colors.primary,
                              fontSize: "0.9rem",
                              textDecoration: "underline",
                              cursor: "pointer",
                            }}
                          >
                            Play video
                          </summary>
                          <div style={{ marginTop: "10px" }}>
                            <video
                              controls
                              preload="metadata"
                              style={{
                                width: "100%",
                                maxWidth: "360px",
                                borderRadius: "8px",
                                border: `1px solid ${colors.border}`,
                                background: "#000",
                              }}
                            >
                              <source src={comment.videoURL} type={inferVideoMimeType(comment.videoURL)} />
                              Your browser does not support the video tag.
                            </video>
                          </div>
                        </details>
                      ) : (
                        <p style={{ margin: 0, color: colors.text, lineHeight: 1.5 }}>
                          {comment.comment || ""}
                        </p>
                      )}
                    </article>
                  ))}
                </div>
              ) : (
                <p style={{ color: colors.subtle, marginBottom: "24px" }}>
                  No comments yet. Be the first to discuss this problem!
                </p>
              )}

              {/* Add Comment Form */}
              <div style={{ display: "flex", gap: "16px", alignItems: "flex-start" }}>
                <div
                  style={{
                    ...card.surface,
                    padding: "20px",
                    borderRadius: "8px",
                    marginBottom: "16px",
                    flex: 1,
                  }}
                >
                  <p style={{ margin: "0 0 12px", fontWeight: 600, color: colors.text }}>
                    Add a Comment or Solution Beta
                  </p>
                  <div style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                    <button
                      type="button"
                      onClick={() => setEntryMode("comment")}
                      style={{
                        ...buttons.secondary,
                        ...(entryMode === "comment"
                          ? { border: `1px solid ${colors.primary}`, color: colors.primary, background: "#eff6ff" }
                          : {}),
                      }}
                    >
                      Add Comment
                    </button>
                    <button
                      type="button"
                      onClick={() => setEntryMode("file")}
                      style={{
                        ...buttons.secondary,
                        ...(entryMode === "file"
                          ? { border: `1px solid ${colors.primary}`, color: colors.primary, background: "#eff6ff" }
                          : {}),
                      }}
                    >
                      Submit Beta
                    </button>
                  </div>

                  {entryMode === "comment" ? (
                    <>
                      <textarea
                        value={commentText}
                        onChange={(e) => setCommentText(e.target.value)}
                        placeholder="Write a comment here!"
                        style={{
                          width: "100%",
                          minHeight: "100px",
                          padding: "12px",
                          border: `1px solid ${commentOverLimit ? colors.danger : colors.muted}`,
                          borderRadius: "6px",
                          fontFamily,
                          fontSize: "0.875rem",
                          resize: "vertical",
                          marginBottom: "8px",
                        }}
                      />
                      <p
                        style={{
                          margin: "0 0 12px",
                          fontSize: "0.8rem",
                          color: commentOverLimit ? colors.danger : colors.subtle,
                          textAlign: "right",
                        }}
                      >
                        {commentCharCount}/250
                      </p>
                    </>
                  ) : (
                    <div style={{ marginBottom: "12px" }}>
                      <input
                        type="file"
                        accept="video/mp4,video/webm"
                        onChange={(e) => setSolutionFile(e.target.files?.[0] || null)}
                        style={{ fontFamily, fontSize: "0.875rem" }}
                      />
                      <p style={{ margin: "8px 0 0", fontSize: "0.8rem", color: colors.subtle }}>
                        Allowed file types: .mp4, .webm
                      </p>
                    </div>
                  )}
                  <div style={{ display: "flex", justifyContent: "flex-end" }}>
                    {entryMode === "comment" ? (
                      <button
                        type="button"
                        onClick={handlePostComment}
                        disabled={!canPostComment}
                        style={{
                          ...buttons.primary,
                          opacity: !canPostComment ? 0.6 : 1,
                          cursor: !canPostComment ? "not-allowed" : "pointer",
                        }}
                      >
                        {submittingComment ? "Posting..." : "Post Comment"}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={handleUploadSolutionBeta}
                        disabled={!solutionFile}
                        style={{
                          ...buttons.primary,
                          opacity: !solutionFile ? 0.6 : 1,
                          cursor: !solutionFile ? "not-allowed" : "pointer",
                        }}
                      >
                        Upload Solution Beta
                      </button>
                    )}
                  </div>
                </div>
                {/* Aggregate Perceived Grade Card */}
                <div
                  style={{
                    ...card.surface,
                    padding: "16px",
                    borderRadius: "8px",
                    textAlign: "center",
                    minWidth: "160px",
                    marginBottom: "16px",
                  }}
                >
                  <p style={{ margin: "0 0 12px", fontWeight: 600, color: colors.text }}>
                    Suggest Difficulty
                  </p>
                  <p style={{ margin: "0 0 10px", fontSize: "1.625rem", fontWeight: 700, color: colors.text }}>
                    {perceivedGrade || "N/A"}
                  </p>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px", marginBottom: "10px" }}>
                    <select
                      value={perceivedGrade}
                      onChange={(e) => setPerceivedGrade(e.target.value)}
                      style={{
                        fontSize: "0.75rem",
                        padding: "4px 6px",
                        border: `1px solid ${colors.muted}`,
                        borderRadius: "6px",
                        fontFamily,
                        background: "white",
                        color: colors.text,
                        cursor: "pointer",
                      }}
                    >
                      {["VB","V0","V1","V2","V3","V4","V5","V6","V7","V8",
                        "V9","V10","V11","V12","V13","V14","V15","V16","V17"].map((g) => (
                        <option key={g} value={g}>{g}</option>
                      ))}
                    </select>
                  </div>
                  <button
                    type="button"
                    onClick={handleSuggestPerceivedGrade}
                    style={{ ...buttons.primary, width: "100%", fontSize: "0.75rem" }}
                  >
                    Suggest Grade
                  </button>
                </div>
              </div>
            </section>
          </>
        )}
      </div>
    </main>
  );
}