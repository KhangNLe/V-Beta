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
      // For now, this is a placeholder
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
                Perceived difficulty: {problem.perceiveGrade.trim() || "N/A"}
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
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
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
              <div
                style={{
                  ...card.surface,
                  padding: "20px",
                  borderRadius: "8px",
                  marginBottom: "16px",
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
            </section>
          </>
        )}
      </div>
    </main>
  );
}