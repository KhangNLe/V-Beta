"use client";

import { fetchWallSectionsForUser } from "@/api/wallSections";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { buttons, card, colors, fontFamily, layout } from "@/ui/appTheme";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function MainPage() {
  const router = useRouter();
  const { user, ready } = useRequireAuth({ redirectMode: "push" });
  const [sections, setSections] = useState([]);
  const [fetchError, setFetchError] = useState(null);

  useEffect(() => {
    if (!user) return;

    let cancelled = false;
    (async () => {
      try {
        const data = await fetchWallSectionsForUser(user);
        if (!cancelled) {
          setSections(data);
          setFetchError(null);
        }
      } catch (err) {
        console.error("Fetch wall sections failed:", err);
        if (!cancelled) {
          setFetchError(err instanceof Error ? err.message : "Unknown error");
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [user]);

  if (!ready) return <PageLoader message="Loading…" />;
  if (!user) return <PageLoader message="Redirecting…" />;

  const handleSelectSection = (section) => {
    router.push(`/wall/${section.wallSectionID}`);
  };

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>
        <header style={{ marginBottom: "24px" }}>
          <h1 style={{ margin: 0, fontSize: "1.75rem", fontWeight: 700, fontFamily }}>
            GYM
          </h1>
        </header>

        {/* Gym Info Card */}
        <section
          style={{
            ...card.surface,
            position: "relative",
            marginBottom: "28px",
            padding: "22px 22px 22px 20px",
            overflow: "hidden",
            fontFamily,
          }}
        >
          <div style={card.accentBar} aria-hidden />
          <p style={{ margin: "0 0 6px", fontSize: "0.75rem", fontWeight: 600, letterSpacing: "0.04em", textTransform: "uppercase", color: colors.subtle }}>
            Gym info
          </p>
          <p style={{ margin: "0 0 12px", fontSize: "0.9375rem", lineHeight: 1.55, color: colors.muted, maxWidth: "65ch" }}>
            A fantastic gym with a variety of climbing walls for all skill levels. Come in and climb!
          </p>
          <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "8px 14px" }}>
            <span style={{ fontSize: "0.875rem", color: colors.zinc600, fontWeight: 500 }}>
              <span style={{ color: colors.subtle }}>Location · </span>
              123 Climbing St, Boulder City
            </span>
          </div>
        </section>

        <h2 style={{ margin: "0 0 16px", fontSize: "1.125rem", fontWeight: 600, color: colors.muted }}>
          Wall sections
        </h2>

        {fetchError && (
          <div style={{ color: colors.danger, background: colors.dangerBg, border: `1px solid ${colors.dangerBorder}`, borderRadius: "8px", padding: "12px 14px", marginBottom: "20px" }}>
            {fetchError}
          </div>
        )}

        {sections.length === 0 ? (
          <p style={{ margin: 0, color: colors.subtle }}>No wall sections found.</p>
        ) : (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
              gap: "20px",
            }}
          >
            {sections.map((section) => (
              <article
                key={section.wallSectionID}
                style={{
                  ...card.surface,
                  padding: "20px",
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                }}
              >
                {/* Header with Name */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <h3 style={{ margin: 0, fontSize: "1.125rem", fontWeight: 600, lineHeight: 1.3, color: colors.text, flex: 1 }}>
                    {section.wallSectionName}
                  </h3>
                </div>

                {/* Description under the name */}
                <p style={{ 
                  margin: 0, 
                  fontSize: "0.875rem", 
                  color: colors.muted, 
                  lineHeight: 1.5, 
                  flexGrow: 1 
                }}>
                  {section.wallSectionInfo || "No description available for this section."}
                </p>

                <button
                  type="button"
                  onClick={() => handleSelectSection(section)}
                  style={{ ...buttons.primary, marginTop: "6px", alignSelf: "flex-start" }}
                >
                  View section
                </button>
              </article>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}