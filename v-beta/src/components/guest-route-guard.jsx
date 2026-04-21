"use client"

import { onAuthStateChanged } from "firebase/auth"
import { usePathname, useRouter } from "next/navigation"
import { useEffect, useState } from "react"

import { auth } from "@/app/firebase"
import { getStoredAccountSession } from "@/lib/accountSession"
import PageLoader from "@/components/ui/PageLoader"

function isLoginSignupOrForgotPath(pathname) {
  return pathname === "/login" || pathname === "/signup" || pathname === "/forgot-password"
}

/**
 * Wrap the full guest route (e.g. branded shell + form).
 * Redirects to /main-page when Firebase has a user and a matching backend session is already in storage.
 * On /login, /signup, and /forgot-password, if the user exists but session is not stored yet, keeps showing
 * children so the form can run sync and navigate (avoids racing signup/login POST with an immediate replace).
 */
export function GuestRouteGuard({ children }) {
  const router = useRouter()
  const pathname = usePathname() ?? ""
  const [authResolved, setAuthResolved] = useState(false)
  const [allowGuest, setAllowGuest] = useState(false)

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setAuthResolved(true)
      if (!currentUser) {
        setAllowGuest(true)
        return
      }

      const session = getStoredAccountSession()
      const sessionReady =
        session != null && session.firebaseUid === currentUser.uid

      if (sessionReady) {
        router.replace("/main-page")
        setAllowGuest(false)
        return
      }

      if (isLoginSignupOrForgotPath(pathname)) {
        setAllowGuest(true)
        return
      }

      router.replace("/main-page")
      setAllowGuest(false)
    })
    return () => unsubscribe()
  }, [router, pathname])

  if (!authResolved || !allowGuest) {
    return <PageLoader />
  }

  return children
}
