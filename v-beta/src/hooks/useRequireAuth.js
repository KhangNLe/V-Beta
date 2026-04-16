"use client";

import { onAuthStateChanged } from "firebase/auth";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { auth } from "@/app/firebase";

/**
 * Redirects unauthenticated visitors to `/login`.
 *
 * @param {{ redirectMode?: "push" | "replace" }} [options]
 * @returns {{
 *   user: import("firebase/auth").User | null,
 *   ready: boolean,
 * }}
 */
export function useRequireAuth(options = {}) {
  const { redirectMode = "push" } = options;
  const router = useRouter();
  /** @type {readonly [import("firebase/auth").User | null, (u: import("firebase/auth").User | null) => void]} */
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      if (!currentUser) {
        if (redirectMode === "replace") router.replace("/login");
        else router.push("/login");
      } else {
        setUser(currentUser);
      }
      setReady(true);
    });
    return () => unsubscribe();
  }, [router, redirectMode]);

  return { user, ready };
}
