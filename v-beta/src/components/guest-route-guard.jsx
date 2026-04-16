"use client"

import { onAuthStateChanged } from "firebase/auth"
import { useRouter } from "next/navigation"
import { useEffect, useState } from "react"

import { auth } from "@/app/firebase"
import PageLoader from "@/components/ui/PageLoader"

/**
 * Wrap the full guest route (e.g. branded shell + form). Renders children only when there is no Firebase user;
 * otherwise replaces the route with /main-page. Shows a full-page loader until auth is resolved.
 */
export function GuestRouteGuard({ children }) {
  const router = useRouter()
  const [authResolved, setAuthResolved] = useState(false)
  const [allowGuest, setAllowGuest] = useState(false)

  useEffect(() => {
    if (auth.currentUser) {
      router.replace("/main-page")
      setAllowGuest(false)
      setAuthResolved(true)
    }

    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setAuthResolved(true)
      if (currentUser) {
        router.replace("/main-page")
        setAllowGuest(false)
        return
      }
      setAllowGuest(true)
    })
    return () => unsubscribe()
  }, [router])

  if (!authResolved || !allowGuest) {
    return <PageLoader />
  }

  return children
}
