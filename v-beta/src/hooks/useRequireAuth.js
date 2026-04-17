"use client";

import { onAuthStateChanged } from "firebase/auth";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { auth } from "@/app/firebase";
import {
  clearStoredAccountSession,
  getStoredAccountSession,
  syncAccountSessionWithBackend,
} from "@/lib/accountSession";

/**
 * Redirects unauthenticated visitors to `/`.
 *
 * @param {{ redirectMode?: "push" | "replace" }} [options]
 * @returns {{
 *   user: import("firebase/auth").User | null,
 *   account: import("@/lib/accountSession").AccountSession | null,
 *   ready: boolean,
 * }}
 */
export function useRequireAuth(options = {}) {
  const { redirectMode = "push" } = options;
  const router = useRouter();
  /** @type {readonly [import("firebase/auth").User | null, (u: import("firebase/auth").User | null) => void]} */
  const [user, setUser] = useState(null);
  const [account, setAccount] = useState(() => getStoredAccountSession());
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (!currentUser) {
        setUser(null);
        setAccount(null);
        clearStoredAccountSession();
        if (redirectMode === "replace") router.replace("/");
        else router.push("/");
      } else {
        setUser(currentUser);
        try {
          const session = await syncAccountSessionWithBackend(currentUser);
          setAccount(session);
        } catch (error) {
          console.error("Failed to sync backend account session:", error);
          setAccount(getStoredAccountSession());
        }
      }
      setReady(true);
    });
    return () => unsubscribe();
  }, [router, redirectMode]);

  return { user, account, ready };
}
