"use client"

import Link from "next/link"
import { useState } from "react"
import { useRouter } from "next/navigation"
import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
} from "firebase/auth"

import { cn } from "@/lib/utils"
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

export function SignupForm({ className, ...props }) {
  const router = useRouter()
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [error, setError] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const googleProvider = new GoogleAuthProvider()

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError("")

    if (password.length < 8) {
      setError("Password must be at least 8 characters long.")
      return
    }
    if (password !== confirmPassword) {
      setError("Passwords do not match.")
      return
    }

    setIsLoading(true)

    try {
      const { user } = await createUserWithEmailAndPassword(auth, email, password)
      try {
        if (name.trim()) {
          await updateProfile(user, { displayName: name.trim() })
        }
        await syncSessionWithBackend(
          name.trim() ? { username: name.trim() } : {}
        )
        router.push("/main-page")
      } catch (afterCreateErr) {
        try {
          await signOut(auth)
        } catch {
          // ignore
        }
        throw afterCreateErr
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Sign up failed.")
    } finally {
      setIsLoading(false)
    }
  }

  const handleGoogleSignup = async () => {
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
      setError(err instanceof Error ? err.message : "Google sign up failed.")
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
      onSubmit={handleSubmit}
      {...props}
    >
      <FieldGroup>
        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="font-[var(--font-inter),ui-sans-serif,system-ui,sans-serif] text-2xl font-bold tracking-[-0.03em] text-foreground md:text-3xl">
            Create your account
          </h1>
          <p className="font-[var(--font-geist-sans),system-ui,sans-serif] text-sm font-light text-balance text-muted-foreground">
            Fill in the form below to create your account
          </p>
        </div>
        <Field>
          <FieldLabel htmlFor="name">Full Name</FieldLabel>
          <Input
            id="name"
            type="text"
            placeholder="John Doe"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </Field>
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
          <FieldDescription>
            We&apos;ll use this to contact you. We will not share your email
            with anyone else.
          </FieldDescription>
        </Field>
        <Field>
          <FieldLabel htmlFor="password">Password</FieldLabel>
          <Input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <FieldDescription>
            Must be at least 8 characters long.
          </FieldDescription>
        </Field>
        <Field>
          <FieldLabel htmlFor="confirm-password">Confirm Password</FieldLabel>
          <Input
            id="confirm-password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
          <FieldDescription>Please confirm your password.</FieldDescription>
        </Field>
        {error ? (
          <FieldDescription className="text-center text-red-600">{error}</FieldDescription>
        ) : null}
        <Field>
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Creating account..." : "Create Account"}
          </Button>
        </Field>
        <FieldSeparator>Or continue with</FieldSeparator>
        <Field>
          <Button
            variant="outline"
            type="button"
            className="!border-primary !bg-background !text-primary hover:!bg-accent hover:!text-primary"
            onClick={handleGoogleSignup}
            disabled={isLoading}
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
              <path
                d="M21.35 11.1H12v2.93h5.36c-.23 1.5-1.74 4.42-5.36 4.42-3.23 0-5.87-2.68-5.87-6s2.64-6 5.87-6c1.84 0 3.07.78 3.77 1.46l2.57-2.49C16.7 3.89 14.57 3 12 3 7.03 3 3 7.03 3 12s4.03 9 9 9c5.19 0 8.63-3.65 8.63-8.8 0-.59-.07-1.04-.15-1.1Z"
                fill="currentColor"
              />
            </svg>
            Sign up with Google
          </Button>
          <FieldDescription className="px-6 text-center">
            Already have an account?{" "}
            <Link href="/login" className="underline underline-offset-4">
              Sign in
            </Link>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  )
}
