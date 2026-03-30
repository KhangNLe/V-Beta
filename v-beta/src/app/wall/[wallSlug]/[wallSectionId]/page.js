"use client";

import { fetchWallSectionsForUser } from "@/api/wallSections";
import PageLoader from "@/components/ui/PageLoader";
import WallSectionDetail from "@/components/wallSection/WallSectionDetail";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { wallSectionPath, wallSectionSlugFromName } from "@/lib/wallSectionUrl";
import { buttons, colors, layout } from "@/ui/appTheme";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function WallSectionPage() {
  const router = useRouter();
  const params = useParams();
  const wallSlug = typeof params?.wallSlug === "string" ? params.wallSlug : "";
  const wallSectionIdParam =
    typeof params?.wallSectionId === "string" ? params.wallSectionId : "";

  const { user, ready: authReady } = useRequireAuth({ redirectMode: "replace" });

  const [section, setSection] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [resolved, setResolved] = useState(false);

  const sectionIdNum = Number.parseInt(wallSectionIdParam, 10);

  useEffect(() => {
    if (!authReady || !user) return;

    if (wallSectionIdParam === "" || Number.isNaN(sectionIdNum)) {
      setNotFound(true);
      setSection(null);
      setResolved(true);
      return;
    }

    let cancelled = false;

    const run = async () => {
      setResolved(false);
      setLoadError(null);
      setNotFound(false);
      try {
        const list = await fetchWallSectionsForUser(user);
        if (cancelled) return;

        const found = list.find((s) => Number(s.wall_section_id) === sectionIdNum);

        if (!found) {
          setNotFound(true);
          setSection(null);
          setResolved(true);
          return;
        }

        const canonicalSlug = wallSectionSlugFromName(found.wall_section_name);
        if (wallSlug !== canonicalSlug) {
          router.replace(wallSectionPath(found));
          return;
        }

        setSection(found);
        setNotFound(false);
        setResolved(true);
      } catch (err) {
        if (!cancelled) {
          console.error(err);
          setLoadError(err instanceof Error ? err.message : "Something went wrong");
          setResolved(true);
        }
      }
    };

    run();
    return () => {
      cancelled = true;
    };
  }, [authReady, user, wallSlug, wallSectionIdParam, sectionIdNum, router]);

  if (!authReady || (authReady && !user)) {
    return <PageLoader />;
  }

  if (!resolved && !loadError) {
    return <PageLoader message="Loading section…" subStyle={{ color: colors.muted }} />;
  }

  if (loadError) {
    return (
      <div style={layout.mainCompact}>
        <div style={layout.maxWidth560}>
          <p style={{ color: colors.danger, marginBottom: "16px" }}>{loadError}</p>
          <Link
            href="/main-page"
            style={{ color: colors.primary, fontWeight: 600, textDecoration: "none" }}
          >
            ← Back to wall sections
          </Link>
        </div>
      </div>
    );
  }

  if (resolved && (notFound || !section)) {
    return (
      <div style={{ ...layout.mainCompact, color: colors.text }}>
        <div style={layout.maxWidth560}>
          <h1 style={{ fontSize: "1.5rem", marginBottom: "8px" }}>Section not found</h1>
          <p style={{ color: colors.muted, marginBottom: "20px" }}>
            This wall section does not exist or the link is invalid.
          </p>
          <Link
            href="/main-page"
            style={{ ...buttons.primary, display: "inline-block", textDecoration: "none" }}
          >
            Back to main page
          </Link>
        </div>
      </div>
    );
  }

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth640}>
        <Link
          href="/main-page"
          style={{
            display: "inline-block",
            marginBottom: "20px",
            fontSize: "0.875rem",
            fontWeight: 600,
            color: colors.primary,
            textDecoration: "none",
          }}
        >
          ← Wall sections
        </Link>

        <WallSectionDetail
          key={section.wall_section_id}
          section={section}
          user={user}
          onClose={() => router.push("/main-page")}
        />
      </div>
    </main>
  );
}
