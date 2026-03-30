"use client";

import { fetchWallSectionsForUser } from "@/api/wallSections";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { wallSectionPath } from "@/lib/wallSectionUrl";
import { buttons, card, colors, fontFamily, layout } from "@/ui/appTheme";
import { signOut } from "firebase/auth";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";

import { auth } from "../firebase";

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

  if (!ready) {
    return <PageLoader message="Loading…" />;
  }

  if (!user) {
    return <PageLoader message="Redirecting…" />;
  }

  const handleLogout = async () => {
    try {
      await signOut(auth);
      router.push("/");
    } catch (err) {
      console.error("Logout failed", err);
      toast.error("Logout failed");
    }
  };

  const handleSelectSection = (section) => {
    router.push(wallSectionPath(section));
  };

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>
        <header
          style={{
            display: "flex",
            flexWrap: "wrap",
            alignItems: "flex-start",
            justifyContent: "space-between",
            gap: "16px",
            marginBottom: "24px",
          }}
        >
          <div style={{ flex: "1 1 280px", minWidth: 0 }}>
            <h1 style={{ margin: "0 0 8px", fontSize: "1.75rem", fontWeight: 700, fontFamily }}>
              Bouldering Project - Minneapolis
            </h1>
          </div>
          <button type="button" onClick={handleLogout} style={{ ...buttons.secondary, flex: "0 0 auto" }}>
            Log out
          </button>
        </header>

        <section
          style={{
            position: "relative",
            marginBottom: "28px",
            padding: "22px 22px 22px 20px",
            borderRadius: "12px",
            border: `1px solid ${colors.border}`,
            background: "#fff",
            boxShadow: card.surface.boxShadow,
            overflow: "hidden",
            fontFamily,
          }}
          aria-labelledby="gym-info-heading"
        >
          <div style={card.accentBar} aria-hidden />
          <p
            style={{
              margin: "0 0 6px",
              fontSize: "0.75rem",
              fontWeight: 600,
              letterSpacing: "0.04em",
              textTransform: "uppercase",
              color: colors.subtle,
            }}
          >
            Gym info
          </p>
          <p
            id="gym-info-heading"
            style={{
              margin: "0 0 12px",
              fontSize: "0.9375rem",
              lineHeight: 1.55,
              color: colors.muted,
              maxWidth: "65ch",
            }}
          >
            Bouldering Project - Minneapolis is the ultimate playground for climbers of all levels! With
            walls that challenge your strength, agility, and creativity, plus cozy spaces to train, relax,
            and connect, it's more than a gym - it's a climbing community where every problem is an
            adventure.
          </p>
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              alignItems: "center",
              gap: "8px 14px",
            }}
          >
            <span style={{ fontSize: "0.875rem", color: colors.zinc600, fontWeight: 500 }}>
              <span style={{ color: colors.subtle, fontWeight: 500 }}>Location · </span>
              1433 West River Rd N, Minneapolis, MN 55411, USA
            </span>
            <span style={{ color: colors.borderHairline }} aria-hidden>
              ·
            </span>
            <span style={{ fontSize: "0.8125rem", color: colors.subtle }}>
              Indoor bouldering &amp; rope walls
            </span>
          </div>
        </section>

        <h2
          style={{
            margin: "0 0 16px",
            fontSize: "1.125rem",
            fontWeight: 600,
            color: colors.muted,
          }}
        >
          Wall sections
        </h2>

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

        {sections.length === 0 ? (
          <p style={{ margin: 0, color: colors.subtle }}>No wall sections found.</p>
        ) : (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))",
              gap: "20px",
            }}
          >
            {sections.map((section) => (
              <article
                key={section.wall_section_id}
                style={{
                  ...card.surface,
                  padding: "20px",
                  display: "flex",
                  flexDirection: "column",
                  gap: "12px",
                }}
              >
                <h3
                  style={{
                    margin: 0,
                    fontSize: "1.125rem",
                    fontWeight: 600,
                    lineHeight: 1.35,
                    color: colors.text,
                  }}
                >
                  {section.wall_section_name}
                </h3>
                <p style={{ margin: 0, fontSize: "0.875rem", color: colors.subtle }}>
                  Section number: <strong style={{ color: colors.zinc600 }}>{section.wall_section_id}</strong>
                </p>
                <button
                  type="button"
                  onClick={() => handleSelectSection(section)}
                  style={{ ...buttons.primary, marginTop: "4px", alignSelf: "flex-start" }}
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
