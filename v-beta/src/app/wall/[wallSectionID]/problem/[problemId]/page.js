"use client";

import { fetchProblemForUser } from "@/api/wallSections";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { buttons, card, colors, layout, fontFamily } from "@/ui/appTheme";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

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
  const [perceivedGrade, setPerceivedGrade] = useState("");

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
        console.error("Failed to fetch problem data:", err);
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
    if (!commentText.trim()) return;
    setSubmittingComment(true);
    try {
      // TODO: Implement backend API call to post comment
      setCommentText("");
    } catch (err) {
      console.error("Failed to post comment:", err);
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleUploadSolutionBeta = () => {
    // TODO: Implement backend API call to upload solution beta
  };

  if (!ready) return <PageLoader message="Loading…" />;
  if (!user) return <PageLoader message="Redirecting…" />;
  if (loading) return <PageLoader message="Loading problem…" />;

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
                Problem #{problem.problemId}
              </h1>
              <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55, maxWidth: "65ch" }}>
                Hold Color: {problem.holdColor || "N/A"}
              </p>
              <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55, maxWidth: "65ch" }}>
                Assigned Grade: {problem.assignedGrade || "V?"}
              </p>
              <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55, maxWidth: "65ch" }}>
                Perceived Difficulty: {problem.perceiveGrade.trim() || "N/A"}
              </p>
            </section>

            {/* Discussion Section */}
            <section style={{ marginBottom: "28px" }}>
              <h2 style={{ margin: "0 0 16px", fontSize: "1.125rem", fontWeight: 600, color: colors.text }}>
                User Discussion
              </h2>

              {/* Comments List */}
              {problem.discussion && problem.discussion.length > 0 ? (
                <div style={{ marginBottom: "24px" }}>
                  {problem.discussion.map((comment, index) => (
                    <article
                      key={index}
                      style={{
                        ...card.surface,
                        padding: "16px",
                        marginBottom: "12px",
                        borderRadius: "8px",
                      }}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", position: "relative" }}>
                        <div style={{ flex: 1 }}>
                          <p style={{ margin: "0 0 4px", fontWeight: 600, color: colors.text }}>
                            {comment.username || "Anonymous"}
                          </p>
                          <p style={{ margin: "0 0 8px", fontSize: "0.875rem", color: colors.subtle }}>
                            {comment.createdDate || "Recently"}
                          </p>
                        </div>
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
                      <p style={{ margin: 0, color: colors.text, lineHeight: 1.5 }}>
                        {comment.comment || ""}
                      </p>
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
                  <textarea
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    placeholder="Write a comment here!"
                    style={{
                      width: "100%",
                      minHeight: "100px",
                      padding: "12px",
                      border: `1px solid ${colors.muted}`,
                      borderRadius: "6px",
                      fontFamily,
                      fontSize: "0.875rem",
                      resize: "vertical",
                      marginBottom: "12px",
                    }}
                  />
                  <div style={{ display: "flex", gap: "12px" }}>
                    <button
                      type="button"
                      onClick={handlePostComment}
                      disabled={submittingComment || !commentText.trim()}
                      style={{
                        ...buttons.primary,
                        opacity: submittingComment || !commentText.trim() ? 0.6 : 1,
                        cursor: submittingComment || !commentText.trim() ? "not-allowed" : "pointer",
                      }}
                    >
                      {submittingComment ? "Posting..." : "Post Comment"}
                    </button>
                    <button
                      type="button"
                      onClick={handleUploadSolutionBeta}
                      style={{
                        ...buttons.secondary,
                      }}
                    >
                      Upload Solution Beta
                    </button>
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
                    Aggregate Perceived Difficulty
                  </p>
                  <p style={{ margin: "0 0 10px", fontSize: "1.625rem", fontWeight: 700, color: colors.text }}>
                    {problem.aggregatePerceivedGrade ?? "N/A"}
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