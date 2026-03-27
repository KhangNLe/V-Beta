"use client";

import { signOut, onAuthStateChanged } from "firebase/auth";
import { auth } from "../firebase"; // adjust path if needed
import { useRouter } from "next/navigation";
import { useState, useEffect } from "react";  

export default function MainPage() {
  const router = useRouter();
  const problems = ["V3 - Overhang Start", "V5 - Slab Balance"];
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Listen for authentication state changes and redirect to login if the user is not authenticated
  useEffect(() => {
    // Subscribe to authentication state changes
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      if (!currentUser) {
        router.push("/");
      } else {
        setUser(currentUser);
      }
      setIsLoading(false);
    });

    return () => unsubscribe(); // Clean up the subscription on unmount
  }, []);

  if (isLoading) {
    return <div>Loading...</div>;
  }

  // Logout handler
  const handleLogout = async () => {
    try {
      await signOut(auth);
      router.push("/"); // send user back to login
    } catch (error) {
      console.error("Logout failed", error);
    }
  };

  // Click handler for problem
  const handleProblemClick = (problem) => {
    // Replace spaces with dashes or use a proper ID for routing
    const problemId = problem.replace(/\s+/g, "-").toLowerCase();
    router.push(`/problem/${problemId}`);
  };

  return (
    <main style={{ padding: "24px", fontFamily: "sans-serif" }}>
      <h1>Main Page</h1>

      {/* Logout button */}
      <button
        onClick={handleLogout}
        style={{
          marginBottom: "24px",
          padding: "8px 16px",
          cursor: "pointer",
        }}
      >
        Log out
      </button>

      <p>Bouldering Problems:</p>
      <ul>
        {problems.map((problem) => (
          <li
            key={problem}
            style={{
              marginBottom: "12px",
              cursor: "pointer",
              color: "blue",
              textDecoration: "underline",
            }}
            onClick={() => handleProblemClick(problem)}
          >
            {problem}
          </li>
        ))}
      </ul>
    </main>
  );
}