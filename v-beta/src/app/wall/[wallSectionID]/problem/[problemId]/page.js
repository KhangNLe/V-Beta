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
              Problem #{problem.problemId}: {problem.holdColor} {problem.assignedGrade || "V?"}
            </h1>
            <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55, maxWidth: "65ch" }}>
              {problem.problemInfo || "No problem description available."}
            </p>
            {/* Add more fields if available, e.g., date created, setter, etc. */}
          </section>
        )}
      </div>
    </main>
  );
}