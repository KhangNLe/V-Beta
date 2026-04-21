"use client";

import Link from "next/link";
import { useState } from "react";
import { sendPasswordResetEmail } from "firebase/auth";

import { auth } from "@/app/firebase";
import { getEmailActionCodeSettings } from "@/lib/authEmailSettings";
import { formatPasswordResetAuthError } from "@/lib/format-login-auth-error";
import { Button } from "@/components/ui/button";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

export function ForgotPasswordForm({ className, ...props }) {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess(false);
    setIsLoading(true);
    const trimmed = email.trim();
    try {
      const settings = getEmailActionCodeSettings("/login");
      await sendPasswordResetEmail(auth, trimmed, settings ?? undefined);
      setSuccess(true);
    } catch (err) {
      console.error(err);
      setError(formatPasswordResetAuthError(err));
    } finally {
      setIsLoading(false);
    }
  };

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
            Reset your password
          </h1>
          <p className="font-[var(--font-geist-sans),system-ui,sans-serif] text-sm font-light text-balance text-muted-foreground">
            Enter your account email. If an account exists, you&apos;ll receive a reset link.
          </p>
        </div>
        <Field>
          <FieldLabel htmlFor="reset-email">Email</FieldLabel>
          <Input
            id="reset-email"
            type="email"
            autoComplete="email"
            placeholder="m@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </Field>
        {success ? (
          <FieldDescription className="text-center text-muted-foreground">
            If an account exists for that email, check your inbox for a password reset link.
          </FieldDescription>
        ) : null}
        {error ? (
          <FieldDescription className="text-center text-red-600">{error}</FieldDescription>
        ) : null}
        <Field>
          <Button type="submit" disabled={isLoading || success}>
            {isLoading ? "Sending…" : "Send reset email"}
          </Button>
          <FieldDescription className="text-center">
            <Link href="/login" className="underline underline-offset-4">
              Back to login
            </Link>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  );
}
