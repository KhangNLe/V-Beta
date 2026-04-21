import { VerifyEmailForm } from "@/components/ui/verify-email-form";
import { AuthBrandedShell } from "@/components/auth-branded-shell";

export default function VerifyEmailPage() {
  return (
    <AuthBrandedShell>
      <VerifyEmailForm />
    </AuthBrandedShell>
  );
}
