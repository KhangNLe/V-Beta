"use client";

import { card, colors } from "@/ui/appTheme";

/**
 * @param {{
 *   problems: import("@/types/climbProblem").ClimbProblem[],
 *   onProblemSelect?: (problem: import("@/types/climbProblem").ClimbProblem) => void,
 * }} props
 */
export default function WallSectionProblems({ problems, onProblemSelect }) {
  if (problems.length === 0) {
    return (
      <p style={{ margin: "16px 0 0", fontSize: "0.875rem", color: colors.subtle }}>
        No problems listed for this section yet.
      </p>
    );
  }

  return (
    <div style={{ marginTop: "20px" }}>
      <h2
        style={{
          margin: "0 0 12px",
          fontSize: "1rem",
          fontWeight: 600,
          color: colors.muted,
        }}
      >
        Problems
      </h2>
      <ul
        style={{
          listStyle: "none",
          margin: 0,
          padding: 0,
          display: "flex",
          flexDirection: "column",
          gap: "10px",
        }}
      >
        {problems.map((problem) => (
          <li key={problem.problem_id}>
            <button
              type="button"
              onClick={() => onProblemSelect?.(problem)}
              style={{
                ...card.surface,
                width: "100%",
                textAlign: "left",
                padding: "14px 16px",
                cursor: onProblemSelect ? "pointer" : "default",
                font: "inherit",
                borderWidth: 1,
              }}
            >
              <div style={{ fontWeight: 600, color: colors.text, marginBottom: "4px" }}>{problem.name}</div>
              {problem.grade && (
                <div style={{ fontSize: "0.8125rem", color: colors.subtle, marginBottom: "6px" }}>{problem.grade}</div>
              )}
              {problem.description && (
                <div style={{ fontSize: "0.875rem", color: colors.muted, lineHeight: 1.45 }}>{problem.description}</div>
              )}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
