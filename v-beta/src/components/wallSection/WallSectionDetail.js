"use client";

import { buttons, card, colors, fontFamily } from "@/ui/appTheme";

import { useWallSection } from "./useWallSection";
import WallSectionProblems from "./WallSectionProblems";

const FALLBACK_DESCRIPTION = "No description has been added for this section yet.";

/**
 * @param {{
 *   section: import("@/types/wallSection").WallSection,
 *   user: import("firebase/auth").User,
 *   onClose: () => void,
 *   onProblemSelect?: (problem: import("@/types/climbProblem").ClimbProblem) => void,
 * }} props
 */
export default function WallSectionDetail({ section, user, onClose, onProblemSelect }) {
  const { problems, loading, error } = useWallSection(user, section.wall_section_id, {
    initialSection: section,
  });

  const description = section.info?.trim() || FALLBACK_DESCRIPTION;

  return (
    <article
      style={{
        ...card.surface,
        padding: "24px",
        fontFamily,
      }}
    >
      <div style={{ display: "flex", flexWrap: "wrap", alignItems: "flex-start", gap: "12px", marginBottom: "8px" }}>
        <p
          style={{
            margin: 0,
            fontSize: "0.75rem",
            fontWeight: 600,
            letterSpacing: "0.04em",
            textTransform: "uppercase",
            color: colors.subtle,
            flex: "1 1 auto",
          }}
        >
          Wall section
        </p>
        <button type="button" onClick={onClose} style={{ ...buttons.secondary, flex: "0 0 auto" }}>
          Back
        </button>
      </div>

      <h1 style={{ margin: "0 0 10px", fontSize: "1.75rem", fontWeight: 700 }}>{section.wall_section_name}</h1>

      <p style={{ margin: "0 0 12px", fontSize: "0.9375rem", color: colors.muted, lineHeight: 1.55 }}>{description}</p>

      <p style={{ margin: "12px 0 0", fontSize: "0.875rem", color: colors.subtle }}>
        Section number: <strong style={{ color: colors.zinc600 }}>{section.wall_section_id}</strong>
      </p>

      {error && (
        <p style={{ margin: "16px 0 0", fontSize: "0.875rem", color: colors.danger }}>{error}</p>
      )}

      {loading ? (
        <p style={{ margin: "20px 0 0", fontSize: "0.875rem", color: colors.subtle }}>Loading problems…</p>
      ) : (
        <WallSectionProblems problems={problems} onProblemSelect={onProblemSelect} />
      )}
    </article>
  );
}
