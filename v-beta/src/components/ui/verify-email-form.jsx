"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { reload, sendEmailVerification, signOut } from "firebase/auth";
import { toast } from "react-toastify";

import { auth } from "@/app/firebase";
import { clearStoredAccountSession } from "@/lib/accountSession";
import { getEmailActionCodeSettings } from "@/lib/authEmailSettings";
import { formatEmailVerificationError } from "@/lib/format-login-auth-error";
import { needsPasswordProviderEmailVerification } from "@/lib/emailVerification";
import { syncAccountSessionWithBackend } from "@/lib/accountSession";
import { Button } from "@/components/ui/button";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import PageLoader from "@/components/ui/PageLoader";
import { cn } from "@/lib/utils";

export function VerifyEmailForm({ className, ...props }) {
  const router = useRouter();
  const { user, ready } = useRequireAuth({ redirectMode: "replace", requireEmailVerified: false });
  const [resendLoading, setResendLoading] = useState(false);
  const [checkLoading, setCheckLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!ready) return;
    if (!user) {
      router.replace("/login");
      return;
    }
    if (!needsPasswordProviderEmailVerification(user)) {
      router.replace("/main-page");
    }
  }, [ready, user, router]);

  const handleResend = useCallback(async () => {
    if (!user || resendLoading) return;
    setError("");
    setResendLoading(true);
    try {
      const settings = getEmailActionCodeSettings("/verify-email");
      await sendEmailVerification(user, settings ?? undefined);
      toast.success("Verification email sent.");
    } catch (err) {
      console.error(err);
      setError(formatEmailVerificationError(err));
    } finally {
      setResendLoading(false);
    }
  }, [user, resendLoading]);

  const handleVerifiedClick = useCallback(async () => {
    if (!user || checkLoading) return;
    setError("");
    setCheckLoading(true);
    try {
      await reload(user);
      const refreshed = auth.currentUser;
      if (refreshed?.emailVerified) {
        await syncAccountSessionWithBackend(refreshed);
        router.replace("/main-page");
        return;
      }
      setError("Email not verified yet. Check your inbox and try again.");
    } catch (err) {
      console.error(err);
      setError(formatEmailVerificationError(err));
    } finally {
      setCheckLoading(false);
    }
  }, [user, checkLoading, router]);

  const handleBackToLogin = useCallback(async () => {
    try {
      await signOut(auth);
    } catch (err) {
      console.error("Failed to sign out before redirecting to login:", err);
    } finally {
      clearStoredAccountSession();
      router.replace("/login");
    }
  }, [router]);

  if (!ready || !user || !needsPasswordProviderEmailVerification(user)) {
    return <PageLoader message="Loading…" />;
  }

  return (
    <div
      className={cn(
        "flex flex-col gap-6 [&_[data-slot=label]]:font-[var(--font-geist-sans),system-ui,sans-serif] [&_[data-slot=label]]:text-[0.7rem] [&_[data-slot=label]]:font-normal [&_[data-slot=label]]:tracking-[0.12em] [&_[data-slot=label]]:uppercase [&_[data-slot=label]]:text-muted-foreground",
        className
      )}
      {...props}
    >
      <FieldGroup>
        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="font-[var(--font-inter),ui-sans-serif,system-ui,sans-serif] text-2xl font-bold tracking-[-0.03em] text-foreground md:text-3xl">
            Verify your email
          </h1>
          <p className="font-[var(--font-geist-sans),system-ui,sans-serif] text-sm font-light text-balance text-muted-foreground">
            We sent a link to <span className="font-medium text-foreground">{user.email}</span>. Open it
            to confirm your address, then return here.
          </p>
        </div>
        <Field className="gap-3">
          <FieldLabel className="sr-only">Actions</FieldLabel>
          <Button type="button" variant="outline" disabled={resendLoading} onClick={handleResend}>
            {resendLoading ? "Sending…" : "Resend verification email"}
          </Button>
          <Button type="button" disabled={checkLoading} onClick={handleVerifiedClick}>
            {checkLoading ? "Checking…" : "I’ve verified — continue"}
          </Button>
          {error ? (
            <FieldDescription className="text-center text-red-600">{error}</FieldDescription>
          ) : null}
          <FieldDescription className="text-center">
            <Link
              href="/login"
              className="underline underline-offset-4"
              onClick={(event) => {
                event.preventDefault();
                handleBackToLogin();
              }}
            >
              Back to login
            </Link>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </div>
  );
}
