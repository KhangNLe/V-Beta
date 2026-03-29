"use client";

import { signOut, onAuthStateChanged } from "firebase/auth";
import { auth } from "../firebase"; // adjust path to firebase.js
import { useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import { toast } from "react-toastify";
import { API_BASE_URL } from "../envExports"; // points to 8080

/** Set to false when the backend GET /api/wall-sections is live. */
const USE_MOCK_WALL_SECTIONS = true;

const MOCK_WALL_SECTIONS = [
  { wall_section_id: 1, wall_section_name: "Overhang" },
  { wall_section_id: 2, wall_section_name: "Slab Balance" },
];

export default function MainPage() {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [wallSections, setWallSections] = useState([]);
  const [fetchError, setFetchError] = useState(null);

  // Firebase auth check
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      if (!currentUser) router.push("/"); // redirect if not logged in
      else setUser(currentUser);
      setIsLoading(false);
    });
    return () => unsubscribe();
  }, []);

  // Fetch wall sections from real backend
  useEffect(() => {
    if (!user) return;

    const fetchWallSections = async () => {
      if (USE_MOCK_WALL_SECTIONS) {
        // TEMP MOCK — flip USE_MOCK_WALL_SECTIONS to false for real API
        setWallSections(MOCK_WALL_SECTIONS);
        setFetchError(null);
        return;
      }

      try {
        const idToken = await user.getIdToken();
        const response = await fetch(`${API_BASE_URL}/api/wall-sections`, {
          headers: {
            Authorization: `Bearer ${idToken}`,
          },
        });
        if (!response.ok) throw new Error(`Failed to fetch: ${response.status}`);
        const data = await response.json();
        setWallSections(data);
        setFetchError(null);
      } catch (err) {
        console.error("Fetch wall sections failed:", err);
        setFetchError(err.message);
      }
    };

    fetchWallSections();
  }, [user]);

  if (isLoading) return <div>Loading...</div>;

  const handleLogout = async () => {
    try {
      await signOut(auth);
      router.push("/");
    } catch (err) {
      console.error("Logout failed", err);
      toast.error("Logout failed");
    }
  };

  const handleWallSectionClick = (section) => {
    const nameSlug = encodeURIComponent(section.wall_section_name.replace(/\s+/g, "-"));
    router.push(`/wall/${nameSlug}/${section.wall_section_id}`);
  };

  return (
    <main style={{ padding: "24px", fontFamily: "sans-serif" }}>
      <h1>Main Page</h1>

      <button
        onClick={handleLogout}
        style={{ marginBottom: "24px", padding: "8px 16px", cursor: "pointer" }}
      >
        Log out
      </button>

      <h2>Wall Sections:</h2>

      {fetchError && (
        <div style={{ color: "red", marginBottom: "12px" }}>Error: {fetchError}</div>
      )}

      {wallSections.length === 0 ? (
        <p>No wall sections found.</p>
      ) : (
        <ul>
          {wallSections.map((section) => (
            <li
              key={section.wall_section_id}
              style={{
                marginBottom: "12px",
                cursor: "pointer",
                color: "blue",
                textDecoration: "underline",
              }}
              onClick={() => handleWallSectionClick(section)}
            >
              {section.wall_section_name}
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}