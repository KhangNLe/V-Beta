"use client";

import { fetchWallSectionsForUser } from "@/api/wallSections";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
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
    <main className="min-h-screen bg-zinc-100 px-6 py-7 pb-12 font-sans text-zinc-900">
      <div className="mx-auto max-w-[960px]">
        <header className="mb-6">
          <h1 className="m-0 text-[1.75rem] font-bold">GYM</h1>
        </header>

        {/* Gym Info Card */}
        <section className="relative mb-7 overflow-hidden rounded-xl border border-zinc-200 bg-white py-[22px] pr-[22px] pl-5 shadow-sm">
          <div
            className="pointer-events-none absolute top-0 bottom-0 left-0 w-1 bg-linear-to-b from-blue-600 to-blue-700"
            aria-hidden
          />
          <p className="mb-1.5 text-xs font-semibold tracking-wide text-zinc-500 uppercase">
            Gym info
          </p>
          <p className="mb-3 max-w-[65ch] text-[0.9375rem] leading-[1.55] text-zinc-600">
            A fantastic gym with a variety of climbing walls for all skill levels. Come in and climb!
          </p>
          <div className="flex flex-wrap items-center gap-x-3.5 gap-y-2">
            <span className="text-sm font-medium text-zinc-600">
              <span className="text-zinc-500">Location · </span>
              123 Climbing St, Boulder City
            </span>
          </div>
        </section>

        <h2 className="mb-4 text-lg font-semibold text-zinc-600">Wall sections</h2>

        {fetchError && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 px-3.5 py-3 text-red-700">
            {fetchError}
          </div>
        )}

        {sections.length === 0 ? (
          <p className="m-0 text-zinc-500">No wall sections found.</p>
        ) : (
          <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(280px,1fr))]">
            {sections.map((section) => (
              <article
                key={section.wallSectionID}
                className="flex flex-col gap-2.5 rounded-xl border border-zinc-200 bg-white p-5 shadow-sm"
              >
                {/* Header with Name */}
                <div className="flex items-start justify-between">
                  <h3 className="m-0 flex-1 text-lg font-semibold leading-[1.3] text-zinc-900">
                    {section.wallSectionName}
                  </h3>
                </div>

                {/* Description under the name */}
                <p className="m-0 flex-grow text-sm leading-normal text-zinc-600">
                  {section.wallSectionInfo || "No description available for this section."}
                </p>

                <button
                  type="button"
                  onClick={() => handleSelectSection(section)}
                  className="mt-1.5 cursor-pointer self-start rounded-lg border-0 bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 focus-visible:outline-none"
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
