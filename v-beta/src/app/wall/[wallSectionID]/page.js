"use client";

import { fetchWallSectionProblemsForUser, fetchWallSectionsForUser } from "@/api/wallSections";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { buttons, card, colors, layout } from "@/ui/appTheme";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

export default function WallSectionPage() {
  const router = useRouter();
  const params = useParams();
  const { user, ready } = useRequireAuth({ redirectMode: "push" });

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
    if (!ready || !user) return;

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
        if (!cancelled) {
          setFetchError(err instanceof Error ? err.message : "Unknown error");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [ready, user, wallSectionID]);

  if (!ready) {
    return <PageLoader message="Loading…" />;
  }

  if (!user) {
    return <PageLoader message="Redirecting…" />;
  }

  if (loading) {
    return <PageLoader message="Loading wall section…" />;
  }

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
          <button type="button" onClick={() => router.push("/main-page")} style={buttons.secondary}>
            Back to wall sections
          </button>
        </div>

        {fetchError && (
          <div
            style={{
              color: colors.danger,
              background: colors.dangerBg,
              border: `1px solid ${colors.dangerBorder}`,
              borderRadius: "8px",
              padding: "12px 14px",
              marginBottom: "20px",
              fontSize: "0.875rem",
            }}
          >
            {fetchError}
          </div>
        )}

        <section
          style={{
            ...card.surface,
            padding: "20px",
            marginBottom: "20px",
          }}
        >
          <h1 style={{ margin: "0 0 8px", fontSize: "1.5rem", color: colors.text }}>
            {section?.wallSectionName || `Wall section ${rawWallSectionID || ""}`}
          </h1>
          <p style={{ margin: "0 0 8px", fontSize: "0.875rem", color: colors.subtle }}>
            Section number: <strong style={{ color: colors.zinc600 }}>{wallSectionID ?? "Unknown"}</strong>
          </p>
          {section?.wallSectionInfo ? (
            <p style={{ margin: 0, color: colors.muted, lineHeight: 1.5 }}>{section.wallSectionInfo}</p>
          ) : (
            <p style={{ margin: 0, color: colors.subtle }}>No section description available.</p>
          )}
        </section>

        <section>
          <h2 style={{ margin: "0 0 14px", fontSize: "1.125rem", color: colors.muted }}>Problems</h2>
          {problems.length === 0 ? (
            <p style={{ margin: 0, color: colors.subtle }}>No problems found for this wall section.</p>
          ) : (
            <div style={{ display: "grid", gap: "14px" }}>
              {problems.map((problem) => (
                <article key={problem.problemId} style={{ ...card.surface, padding: "16px" }}>
                  <p style={{ margin: "0 0 6px", color: colors.subtle, fontSize: "0.8125rem" }}>
                    Problem ID: <strong style={{ color: colors.zinc600 }}>{problem.problemId}</strong>
                  </p>
                  <p style={{ margin: "0 0 6px", color: colors.text, fontWeight: 600 }}>
                    Grade: {problem.assignedGrade || "Unassigned"}
                  </p>
                  <p style={{ margin: "0 0 6px", color: colors.muted }}>
                    Hold color: {problem.holdColor || "Unknown"}
                  </p>
                  <p style={{ margin: 0, color: colors.subtle }}>
                    {problem.problemInfo || "No problem notes available."}
                  </p>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
