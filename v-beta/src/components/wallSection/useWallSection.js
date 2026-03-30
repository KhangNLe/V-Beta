"use client";

import { fetchWallSectionProblemsForUser, fetchWallSectionsForUser } from "@/api/wallSections";
import { useEffect, useState } from "react";

/**
 * Loads wall section row and problems. Pass `initialSection` when it is already known
 * (e.g. from the URL page) to avoid an extra list fetch.
 *
 * @param {import("firebase/auth").User | null} user
 * @param {number | null} sectionId
 * @param {{ initialSection?: import("@/types/wallSection").WallSection | null }} [options]
 */
export function useWallSection(user, sectionId, options = {}) {
  const { initialSection = null } = options;

  const [section, setSection] = useState(
    initialSection != null && Number(initialSection.wall_section_id) === Number(sectionId)
      ? initialSection
      : null,
  );
  const [problems, setProblems] = useState(/** @type {import("@/types/climbProblem").ClimbProblem[]} */ ([]));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(/** @type {string | null} */ (null));

  const seedId = initialSection?.wall_section_id;

  useEffect(() => {
    if (!user || sectionId == null || Number.isNaN(Number(sectionId))) {
      setLoading(false);
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        let resolved =
          initialSection != null && Number(initialSection.wall_section_id) === Number(sectionId)
            ? initialSection
            : null;

        if (!resolved) {
          const list = await fetchWallSectionsForUser(user);
          resolved = list.find((s) => Number(s.wall_section_id) === Number(sectionId)) ?? null;
        }

        if (cancelled) return;

        if (!resolved) {
          setSection(null);
          setProblems([]);
          setLoading(false);
          return;
        }

        setSection(resolved);
        const probs = await fetchWallSectionProblemsForUser(user, Number(sectionId));
        if (!cancelled) {
          setProblems(probs);
          setLoading(false);
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : "Something went wrong");
          setLoading(false);
        }
      }
    };

    run();
    return () => {
      cancelled = true;
    };
  }, [user, sectionId, seedId]);

  return { section, problems, loading, error };
}
