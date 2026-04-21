"use client";

import { fetchWallSectionProblemsForUser, fetchWallSectionsForUser } from "@/api/wallSections";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { buttons, card, colors, layout, fontFamily } from "@/ui/appTheme";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

export default function WallSectionPage() {
  const router = useRouter();
  const params = useParams();
  const { user, ready } = useRequireAuth({
    requireAuth: false,
    requireEmailVerified: true,
  });
  const [section, setSection] = useState(null);
  const [problems, setProblems] = useState([]);
  const [fetchError, setFetchError] = useState(null);
  const [loading, setLoading] = useState(true);

  const rawWallSectionID = params?.wallSectionID;
  const wallSectionID = useMemo(() => {
    const normalized = Array.isArray(rawWallSectionID) ? rawWallSectionID[0] : rawWallSectionID;
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [rawWallSectionID]);

  useEffect(() => {
    if (!ready) return;
    if (!wallSectionID) {
      setLoading(false);
      setFetchError("Invalid wall section id.");
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        const [sectionsData, problemsData] = await Promise.all([
          fetchWallSectionsForUser(user),
          fetchWallSectionProblemsForUser(user, wallSectionID),
        ]);
        if (cancelled) return;

        const selected = sectionsData.find((item) => item.wallSectionID === wallSectionID) || null;
        setSection(selected);
        setProblems(Array.isArray(problemsData) ? problemsData : []);
        setFetchError(null);
      } catch (err) {
        console.error("Failed to fetch wall section page data:", err);
        if (!cancelled) setFetchError(err instanceof Error ? err.message : "Unknown error");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [ready, user, wallSectionID]);

  const handleViewProblem = (problemId) => {
    router.push(`/wall/${wallSectionID}/problem/${problemId}`);
  };

  const handleBackToSections = () => {
    router.push("/main-page");
  };

  if (!ready) return <PageLoader message="Loading…" />;
  if (loading) return <PageLoader message="Loading wall section…" />;

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>
        <button
          type="button"
          onClick={handleBackToSections}
          style={{ ...buttons.secondary, marginBottom: "16px" }}
        >
          Back
        </button>

        {/* Section Header Card */}
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
            Wall Section: {section?.wallSectionName || `Section ${wallSectionID}`}
          </h1>
          <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55, maxWidth: "65ch" }}>
            {section?.wallSectionInfo || "No section description available."}
          </p>
        </section>

        <h2 style={{ margin: "0 0 16px", fontSize: "1.125rem", fontWeight: 600, color: colors.muted }}>
          Problems
        </h2>

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

        {problems.length === 0 ? (
          <p style={{ margin: 0, color: colors.subtle }}>No problems found for this wall section.</p>
        ) : (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))",
              gap: "20px",
            }}
          >
            {problems.map((problem) => (
              <article
                key={problem.problemId}
                style={{
                  ...card.surface,
                  padding: "20px",
                  display: "flex",
                  flexDirection: "column",
                  gap: "12px",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <h3 style={{ margin: 0, fontSize: "1.125rem", fontWeight: 600, lineHeight: 1.35, color: colors.text }}>
                    {problem.holdColor} {problem.assignedGrade || "V?"}
                  </h3>
                  <span style={{ fontSize: "0.75rem", fontWeight: 500, color: colors.subtle }}>
                    #{problem.problemId}
                  </span>
                </div>

                <p style={{ 
                    margin: 0, 
                    fontSize: "0.875rem", 
                    color: colors.muted, 
                    lineHeight: 1.5,
                    flexGrow: 1 
                }}>
                  {problem.problemInfo || "No problem notes available."}
                </p>

                <button
                  type="button"
                  onClick={() => handleViewProblem(problem.problemId)}
                  style={{ 
                    ...buttons.primary, 
                    marginTop: "4px", 
                    alignSelf: "flex-start" 
                  }}
                >
                  View problem
                </button>
              </article>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}