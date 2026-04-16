"use client"

import Link from "next/link"
import { useState } from "react"
import { useRouter } from "next/navigation"
import {
  GoogleAuthProvider,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
} from "firebase/auth"

import { cn } from "@/lib/utils"
import { formatLoginAuthError } from "@/lib/format-login-auth-error"
import { syncSessionWithBackend } from "@/lib/sync-backend-session"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { auth } from "@/app/firebase"
import { SiGoogle } from "react-icons/si"

export function LoginForm({ className, ...props }) {
  const router = useRouter()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const googleProvider = new GoogleAuthProvider()

  const handleEmailLogin = async (event) => {
    event.preventDefault()
    setIsLoading(true)
    setError("")

    try {
      await signInWithEmailAndPassword(auth, email, password)
      try {
        await syncSessionWithBackend()
        router.push("/main-page")
      } catch (syncErr) {
        try {
          await signOut(auth)
        } catch {
          // ignore
        }
        throw syncErr
      }
    } catch (err) {
      console.error(err)
      setError(formatLoginAuthError(err))
    } finally {
      setIsLoading(false)
    }
  }

  const handleGoogleLogin = async () => {
    setIsLoading(true)
    setError("")

    try {
      await signInWithPopup(auth, googleProvider)
      try {
        await syncSessionWithBackend()
        router.push("/main-page")
      } catch (syncErr) {
        try {
          await signOut(auth)
        } catch {
          // ignore
        }
        throw syncErr
      }
    } catch (err) {
      console.error(err)
      setError(formatLoginAuthError(err))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <form
      className={cn(
        "flex flex-col gap-6 [&_[data-slot=label]]:font-[var(--font-geist-sans),system-ui,sans-serif] [&_[data-slot=label]]:text-[0.7rem] [&_[data-slot=label]]:font-normal [&_[data-slot=label]]:tracking-[0.12em] [&_[data-slot=label]]:uppercase [&_[data-slot=label]]:text-muted-foreground",
        className
      )}
      onSubmit={handleEmailLogin}
      {...props}
    >
      <FieldGroup>
        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="font-[var(--font-inter),ui-sans-serif,system-ui,sans-serif] text-2xl font-bold tracking-[-0.03em] text-foreground md:text-3xl">
            Login to your account
          </h1>
          <p className="font-[var(--font-geist-sans),system-ui,sans-serif] text-sm font-light text-balance text-muted-foreground">
            Enter your email below to login to your account
          </p>
        </div>
        <Field>
          <FieldLabel htmlFor="email">Email</FieldLabel>
          <Input
            id="email"
            type="email"
            placeholder="m@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </Field>
        <Field>
          <div className="flex items-center">
            <FieldLabel htmlFor="password">Password</FieldLabel>
            <Link
              href="/forgot-password"
              className="ml-auto text-sm underline-offset-4 hover:underline"
            >
              Forgot your password?
            </Link>
          </div>
          <Input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </Field>
        {error ? (
          <FieldDescription className="text-center text-red-600">{error}</FieldDescription>
        ) : null}
        <Field>
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Logging in..." : "Login"}
          </Button>
        </Field>
        <FieldSeparator>Or continue with</FieldSeparator>
        <Field>
          <Button
            variant="outline"
            type="button"
            className="!border-primary !bg-background !text-primary hover:!bg-accent hover:!text-primary"
            onClick={handleGoogleLogin}
            disabled={isLoading}
          >
            <SiGoogle className="size-[.9rem] shrink-0" aria-hidden />
            Login with Google
          </Button>
          <FieldDescription className="text-center">
            Don&apos;t have an account?{" "}
            <Link href="/signup" className="underline underline-offset-4">
              Sign up
            </Link>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  )
}
