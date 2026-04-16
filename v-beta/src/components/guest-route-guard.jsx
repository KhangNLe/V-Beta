"use client"

import { onAuthStateChanged } from "firebase/auth"
import { useRouter } from "next/navigation"
import { useEffect, useState } from "react"

import { auth } from "@/app/firebase"

/**
 * Renders children only when there is no Firebase user; otherwise replaces the route with /main-page.
 */
export function GuestRouteGuard({ children }) {
  const router = useRouter()
  const [allowGuest, setAllowGuest] = useState(false)

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      if (currentUser) {
        router.replace("/main-page")
        setAllowGuest(false)
        return
      }
      setAllowGuest(true)
    })
    return () => unsubscribe()
  }, [router])

  if (!allowGuest) {
    return null
  }

  return children
}
