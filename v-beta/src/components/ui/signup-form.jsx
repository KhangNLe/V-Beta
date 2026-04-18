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
import { formatSignupAuthError } from "@/lib/format-login-auth-error"
import { syncAccountSessionWithBackend } from "@/lib/accountSession"
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

export function SignupForm({ className, ...props }) {
  const router = useRouter()
  const [username, setUsername] = useState("")
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
        if (username.trim()) {
          await updateProfile(user, { displayName: username.trim() })
        }
        await syncAccountSessionWithBackend(
          auth.currentUser,
          username.trim() ? { username: username.trim() } : {}
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
      console.error(err)
      setError(formatSignupAuthError(err))
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
        await syncAccountSessionWithBackend(auth.currentUser)
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
      setError(formatSignupAuthError(err))
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
          <FieldLabel htmlFor="username">Username</FieldLabel>
          <Input
            id="username"
            type="text"
            placeholder="John"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
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
            <SiGoogle className="size-[.9rem] shrink-0" aria-hidden />
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
