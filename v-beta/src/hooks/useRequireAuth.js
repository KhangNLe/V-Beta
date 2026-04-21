'use client';

import { onAuthStateChanged } from "firebase/auth";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { auth } from '@/app/firebase';
import {
  clearStoredAccountSession,
  getStoredAccountSession,
  syncAccountSessionWithBackend,
} from "@/lib/accountSession";
import { needsPasswordProviderEmailVerification } from "@/lib/emailVerification";

/**
 * Redirects unauthenticated visitors to `/login` unless `allowGuest` is true.
 * `requireAuth: false` is treated as `allowGuest: true` for backwards compatibility.
 * `requireEmailVerified: true` sends email/password unverified users to `/verify-email`.
 *
 * @param {{
 *   skip?: boolean,
 *   redirectMode?: "push" | "replace",
 *   allowGuest?: boolean,
 *   requireAuth?: boolean,
 *   requireEmailVerified?: boolean,
 * }} [options]
 */
export function useRequireAuth(options = {}) {
  const {
    skip = false,
    redirectMode = "push",
    allowGuest: allowGuestOpt,
    requireAuth,
    requireEmailVerified = false,
  } = options;
  const allowGuest = allowGuestOpt ?? requireAuth === false;
  const router = useRouter();
  const pathname = usePathname() ?? "";
  const [user, setUser] = useState(null);
  const [account, setAccount] = useState(() => getStoredAccountSession());
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (skip) return undefined;

    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (!currentUser) {
        setUser(null);
        setAccount(null);
        clearStoredAccountSession();
        if (!allowGuest) {
          if (redirectMode === "replace") router.replace("/login");
          else router.push("/login");
        }
        setReady(true);
        return;
      }

      setUser(currentUser);
      try {
        const session = await syncAccountSessionWithBackend(currentUser);
        setAccount(session);
      } catch (error) {
        console.error("Failed to sync backend account session:", error);
        setAccount(getStoredAccountSession());
      }
      setUser(currentUser);

      try {
        const session = await syncAccountSessionWithBackend(currentUser);
        setAccount(session);
      } catch (error) {
        console.error('Failed to sync backend account session:', error);
        setAccount(getStoredAccountSession());
      }

      setReady(true);
    });

    return () => unsubscribe();
  }, [router, redirectMode, skip, allowGuest]);

  useEffect(() => {
    if (skip || !ready || !user) return;
    if (!requireEmailVerified || !needsPasswordProviderEmailVerification(user)) return;
    if (pathname === "/verify-email") return;
    router.replace("/verify-email");
  }, [skip, ready, user, requireEmailVerified, pathname, router]);

  return { user, account, ready };
}
